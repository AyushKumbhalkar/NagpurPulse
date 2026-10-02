import { createClient } from "npm:@supabase/supabase-js@2";
import { JWT } from "npm:google-auth-library@9";

type NotificationRow = {
  id: string;
  user_id: string;
  type: string;
  title: string;
  body: string | null;
  related_post_id: string | null;
  related_comment_id: string | null;
  related_conversation_id: string | null;
  sender_username: string | null;
  sender_avatar_url: string | null;
  created_at: string;
};

function jsonResponse(payload: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function preferenceForType(type: string): string | null {
  switch (type) {
    case "comment":
    case "reply": return "notif_replies";
    case "mention": return "notif_mentions";
    case "message": return "notif_messages";
    case "upvote":
    case "like":
    case "comment_like": return "notif_upvotes";
    case "trending": return "notif_trending";
    case "community": return "notif_community";
    case "digest": return "notif_digest";
    case "alerts_summary": return "notif_alerts_summary";
    default: return null;
  }
}

async function getFirebaseAccessToken(): Promise<{ token: string; projectId: string }> {
  const projectId = Deno.env.get("FIREBASE_PROJECT_ID");
  const clientEmail = Deno.env.get("FIREBASE_CLIENT_EMAIL");
  const privateKey = Deno.env.get("FIREBASE_PRIVATE_KEY")?.replace(/\\n/g, "\n");
  if (!projectId || !clientEmail || !privateKey) {
    throw new Error("Firebase Cloud Messaging credentials are not configured");
  }

  const jwtClient = new JWT({
    email: clientEmail,
    key: privateKey,
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  const credentials = await jwtClient.authorize();
  if (!credentials.access_token) throw new Error("Firebase access token could not be obtained");
  return { token: credentials.access_token, projectId };
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!supabaseUrl || !serviceRole) throw new Error("Supabase server credentials are not configured");

    const supabase = createClient(supabaseUrl, serviceRole, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    // The database trigger supplies a random secret stored in Supabase Vault.
    // Do not trust the public anon key or webhook body as authentication.
    const webhookSecret = req.headers.get("x-notification-webhook-secret");
    if (!webhookSecret) return jsonResponse({ error: "Unauthorized" }, 401);
    const { data: webhookAuthorized, error: authError } = await supabase.rpc(
      "validate_notification_webhook_secret",
      { p_candidate: webhookSecret },
    );
    if (authError || webhookAuthorized !== true) {
      console.error("Notification webhook authentication failed", authError?.message || "invalid secret");
      return jsonResponse({ error: "Unauthorized" }, 401);
    }

    const payload = await req.json();
    if (
      payload?.type !== "INSERT" ||
      payload?.schema !== "public" ||
      payload?.table !== "notifications" ||
      typeof payload?.record?.id !== "string"
    ) {
      return jsonResponse({ error: "Expected a notifications INSERT webhook" }, 400);
    }

    // Resolve all notification fields from the database, not from webhook input.
    const { data: notification, error: notificationError } = await supabase
      .from("notifications")
      .select("id,user_id,type,title,body,related_post_id,related_comment_id,related_conversation_id,sender_username,sender_avatar_url,created_at")
      .eq("id", payload.record.id)
      .maybeSingle();

    if (notificationError) throw notificationError;
    if (!notification) {
      return jsonResponse({ sent: false, skipped: true, reason: "notification_not_found" }, 404);
    }

    if (!notification.user_id) {
      return jsonResponse({ sent: false, skipped: true, reason: "notification_has_no_recipient" });
    }

    // Older app builds wrote a second notification row when receiving an FCM push.
    // Those legacy echo rows lack sender profile fields. Only apply the short
    // duplicate guard to such rows; do not suppress two legitimate, identical
    // notifications created by current clients.
    if (!notification.sender_username && !notification.sender_avatar_url) {
      const cutoff = new Date(new Date(notification.created_at).getTime() - 120_000).toISOString();
    let duplicateQuery = supabase
      .from("notifications")
      .select("id")
      .eq("user_id", notification.user_id)
      .eq("type", notification.type)
      .eq("title", notification.title)
      .lt("created_at", notification.created_at)
      .gte("created_at", cutoff)
      .limit(1);
    if (notification.body === null) {
      duplicateQuery = duplicateQuery.is("body", null);
    } else {
      duplicateQuery = duplicateQuery.eq("body", notification.body);
    }
    if (notification.related_post_id) {
      duplicateQuery = duplicateQuery.eq("related_post_id", notification.related_post_id);
    } else {
      duplicateQuery = duplicateQuery.is("related_post_id", null);
    }
    if (notification.related_comment_id) {
      duplicateQuery = duplicateQuery.eq("related_comment_id", notification.related_comment_id);
    } else {
      duplicateQuery = duplicateQuery.is("related_comment_id", null);
    }
    if (notification.related_conversation_id) {
      duplicateQuery = duplicateQuery.eq("related_conversation_id", notification.related_conversation_id);
    } else {
      duplicateQuery = duplicateQuery.is("related_conversation_id", null);
    }
    duplicateQuery = duplicateQuery.or("sender_username.not.is.null,sender_avatar_url.not.is.null");
    const { data: priorNotifications, error: duplicateError } = await duplicateQuery;
    if (duplicateError) throw duplicateError;
    if (priorNotifications && priorNotifications.length > 0) {
      return jsonResponse({ sent: false, skipped: true, reason: "duplicate_notification_echo" });
    }
    }

    const { data: preferences, error: preferencesError } = await supabase
      .from("user_preferences")
      .select("notif_push,notif_replies,notif_mentions,notif_messages,notif_upvotes,notif_digest,notif_trending,notif_community,notif_alerts_summary")
      .eq("user_id", notification.user_id)
      .maybeSingle();

    if (preferencesError) throw preferencesError;
    const preferenceField = preferenceForType(String(notification.type || "general"));
    if (
      preferences?.notif_push === false ||
      (preferenceField && preferences?.[preferenceField] === false)
    ) {
      return jsonResponse({ sent: false, skipped: true, reason: "notification_preference_disabled" });
    }

    const { data: device, error: deviceError } = await supabase
      .from("device_tokens")
      .select("fcm_token")
      .eq("user_id", notification.user_id)
      .maybeSingle();

    if (deviceError) throw deviceError;
    if (!device?.fcm_token) {
      return jsonResponse({ sent: false, skipped: true, reason: "no_device_token" });
    }

    const { token: accessToken, projectId } = await getFirebaseAccessToken();
    const data = {
      notification_id: String(notification.id),
      type: String(notification.type || "general"),
      post_id: notification.related_post_id ? String(notification.related_post_id) : "",
      comment_id: notification.related_comment_id ? String(notification.related_comment_id) : "",
      conversation_id: notification.related_conversation_id ? String(notification.related_conversation_id) : "",
      sender_username: notification.sender_username || "",
      sender_avatar_url: notification.sender_avatar_url || "",
    };

    const fcmResponse = await fetch(
      "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send",
      {
        method: "POST",
        headers: {
          Authorization: "Bearer " + accessToken,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: device.fcm_token,
            notification: {
              title: notification.title,
              body: notification.body || "",
            },
            data,
            android: { priority: "HIGH" },
          },
        }),
      },
    );

    const fcmResult = await fcmResponse.json().catch(() => ({}));
    if (!fcmResponse.ok) {
      const fcmError = fcmResult?.error?.message || "FCM delivery failed";
      console.error("FCM delivery failed", {
        notificationId: notification.id,
        status: fcmResponse.status,
        error: fcmError,
      });
      return jsonResponse({ sent: false, error: fcmError }, 502);
    }

    // Best-effort aggregate counter only. Never store recipient or notification IDs.
    const { error: analyticsError } = await supabase.rpc("record_notification_analytics", {
      p_event_type: "push_delivered",
      p_notification_type: String(notification.type || "general").toLowerCase(),
    });
    if (analyticsError) {
      console.error("Notification analytics counter failed", analyticsError.message);
    }

    return jsonResponse({ sent: true, notificationId: notification.id, message: fcmResult });
  } catch (error) {
    console.error("send-push-notification failed", error);
    return jsonResponse(
      { error: error instanceof Error ? error.message : "Unexpected notification error" },
      500,
    );
  }
});
