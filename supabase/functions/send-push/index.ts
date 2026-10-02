import { createClient } from "npm:@supabase/supabase-js@2";
import { JWT } from "npm:google-auth-library@9";

type DeviceToken = { user_id: string; fcm_token: string };
type Preferences = Record<string, unknown> & { user_id: string };

function jsonResponse(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), {
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
    case "like": return "notif_upvotes";
    case "trending": return "notif_trending";
    case "community": return "notif_community";
    case "digest": return "notif_digest";
    case "alerts_summary": return "notif_alerts_summary";
    default: return null;
  }
}

async function getFirebaseAccessToken() {
  const projectId = Deno.env.get("FIREBASE_PROJECT_ID");
  const email = Deno.env.get("FIREBASE_CLIENT_EMAIL");
  const privateKey = Deno.env.get("FIREBASE_PRIVATE_KEY")?.replace(/\\n/g, "\n");
  if (!projectId || !email || !privateKey) {
    throw new Error("Firebase credentials are not configured");
  }
  const client = new JWT({
    email,
    key: privateKey,
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  const result = await client.authorize();
  if (!result.access_token) throw new Error("Firebase access token was not returned");
  return { accessToken: result.access_token, projectId };
}

async function deliverBatch(
  supabase: any,
  recipients: DeviceToken[],
  title: string,
  body: string,
  postId: string,
  commentId: string,
  conversationId: string,
  notificationId: string,
  type: string,
  accessToken: string,
  projectId: string,
) {
  if (recipients.length === 0) return { sent: 0, failed: 0, eligible: 0 };

  const userIds = [...new Set(recipients.map((item) => item.user_id))];
  const { data: preferenceRows, error: preferenceError } = await supabase
    .from("user_preferences")
    .select("user_id,notif_push,notif_replies,notif_mentions,notif_messages,notif_upvotes,notif_digest,notif_trending,notif_community,notif_alerts_summary")
    .in("user_id", userIds);
  if (preferenceError) throw preferenceError;

  const preferenceByUser = new Map<string, Preferences>();
  for (const row of (preferenceRows || []) as Preferences[]) {
    preferenceByUser.set(row.user_id, row);
  }

  const preferenceField = preferenceForType(type);
  const eligible = recipients.filter((item) => {
    const prefs = preferenceByUser.get(item.user_id);
    if (prefs?.notif_push === false) return false;
    if (preferenceField && prefs?.[preferenceField] === false) return false;
    return true;
  });

  const outcomes = await Promise.all(eligible.map(async (item) => {
    try {
      const response = await fetch(
        "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send",
        {
          method: "POST",
          headers: {
            Authorization: "Bearer " + accessToken,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token: item.fcm_token,
              notification: { title, body },
              data: {
                notification_id: notificationId,
                post_id: postId,
                comment_id: commentId,
                conversation_id: conversationId,
                type,
              },
              android: { priority: "HIGH" },
            },
          }),
        },
      );
      const responseBody = await response.json().catch(() => ({}));
      if (response.ok) return { sent: 1, failed: 0 };

      const unregistered = responseBody?.error?.details?.some(
        (detail: { errorCode?: string }) => detail.errorCode === "UNREGISTERED",
      );
      if (unregistered) {
        await supabase.from("device_tokens").delete()
          .eq("user_id", item.user_id)
          .eq("fcm_token", item.fcm_token);
      }
      return { sent: 0, failed: 1 };
    } catch {
      return { sent: 0, failed: 1 };
    }
  }));

  return outcomes.reduce(
    (sum, outcome) => ({
      sent: sum.sent + outcome.sent,
      failed: sum.failed + outcome.failed,
      eligible: sum.eligible + outcome.sent + outcome.failed,
    }),
    { sent: 0, failed: 0, eligible: 0 },
  );
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  // This function is server-to-server only. The caller must use the Supabase
  // service-role JWT; the gateway also verifies JWTs before this handler runs.
  const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!serviceRole || req.headers.get("Authorization") !== "Bearer " + serviceRole) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }

  try {
    const payload = await req.json();
    const mode = payload?.mode === "broadcast" ? "broadcast" : "single";
    const userId = typeof payload?.userId === "string" ? payload.userId : null;
    const excludeUserId = typeof payload?.excludeUserId === "string" ? payload.excludeUserId : null;
    const title = typeof payload?.title === "string" ? payload.title.trim() : "";
    const body = typeof payload?.body === "string" ? payload.body : "";
    const postId = typeof payload?.postId === "string" ? payload.postId : "";
    const commentId = typeof payload?.commentId === "string" ? payload.commentId : "";
    const conversationId = typeof payload?.conversationId === "string" ? payload.conversationId : "";
    const notificationId = typeof payload?.notificationId === "string" ? payload.notificationId : "";
    const type = typeof payload?.type === "string" ? payload.type : "community";

    if (!title) return jsonResponse({ error: "title is required" }, 400);
    if (mode === "single" && !userId) {
      return jsonResponse({ error: "userId is required for single-recipient mode" }, 400);
    }

    const url = Deno.env.get("SUPABASE_URL");
    if (!url) throw new Error("SUPABASE_URL is not configured");
    const supabase = createClient(url, serviceRole, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { accessToken, projectId } = await getFirebaseAccessToken();

    let sent = 0;
    let failed = 0;
    let scanned = 0;
    let eligible = 0;
    let lastUserId: string | null = null;
    const pageSize = 400;

    // Keyset pagination avoids PostgREST's default 1,000-row cap and avoids
    // skipping rows if an invalid token is removed while a broadcast is running.
    for (;;) {
      let query = supabase
        .from("device_tokens")
        .select("user_id,fcm_token")
        .order("user_id", { ascending: true })
        .limit(pageSize);

      if (mode === "single") {
        query = query.eq("user_id", userId);
      } else {
        if (excludeUserId) query = query.neq("user_id", excludeUserId);
        if (lastUserId) query = query.gt("user_id", lastUserId);
      }

      const { data, error } = await query;
      if (error) throw error;
      const recipients = (data || []) as DeviceToken[];
      if (recipients.length === 0) break;

      scanned += recipients.length;
      lastUserId = recipients[recipients.length - 1].user_id;
      const result = await deliverBatch(
        supabase, recipients, title, body, postId, commentId, conversationId, notificationId, type, accessToken, projectId,
      );
      sent += result.sent;
      failed += result.failed;
      eligible += result.eligible;

      if (mode === "single" || recipients.length < pageSize) break;
    }

    return jsonResponse({ sent, failed, scanned, eligible, mode });
  } catch (error) {
    console.error("send-push failed", error);
    return jsonResponse(
      { error: error instanceof Error ? error.message : "Unexpected push error" },
      500,
    );
  }
});
