import { createClient } from "npm:@supabase/supabase-js@2";
import { JWT } from "npm:google-auth-library@9";

function jsonResponse(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
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
  if (!projectId || !email || !privateKey) throw new Error("Firebase credentials are not configured");
  const client = new JWT({ email, key: privateKey, scopes: ["https://www.googleapis.com/auth/firebase.messaging"] });
  const result = await client.authorize();
  if (!result.access_token) throw new Error("Firebase access token was not returned");
  return { accessToken: result.access_token, projectId };
}
Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);
  try {
    const payload = await req.json();
    const mode = payload?.mode === "broadcast" ? "broadcast" : "single";
    const userId = typeof payload?.userId === "string" ? payload.userId : null;
    const excludeUserId = typeof payload?.excludeUserId === "string" ? payload.excludeUserId : null;
    const title = typeof payload?.title === "string" ? payload.title.trim() : "";
    const body = typeof payload?.body === "string" ? payload.body : "";
    const postId = typeof payload?.postId === "string" ? payload.postId : "";
    const notificationId = typeof payload?.notificationId === "string" ? payload.notificationId : "";
    const type = typeof payload?.type === "string" ? payload.type : "community";
    if (!title) return jsonResponse({ error: "title is required" }, 400);
    if (mode === "single" && !userId) return jsonResponse({ error: "userId is required" }, 400);

    const url = Deno.env.get("SUPABASE_URL");
    const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!url || !serviceRole) throw new Error("Supabase server credentials are not configured");
    const supabase = createClient(url, serviceRole, { auth: { persistSession: false, autoRefreshToken: false } });
    const { accessToken, projectId } = await getFirebaseAccessToken();

    let query = supabase.from("device_tokens").select("user_id,fcm_token");
    if (mode === "broadcast") {
      if (excludeUserId) query = query.neq("user_id", excludeUserId);
    } else {
      query = query.eq("user_id", userId);
    }
    const { data: tokens, error: tokenError } = await query;
    if (tokenError) throw tokenError;
    if (!tokens || tokens.length === 0) return jsonResponse({ sent: 0, failed: 0, message: "No recipient tokens found" });

    const userIds = [...new Set(tokens.map((item: { user_id: string }) => item.user_id))];
    const { data: preferenceRows, error: preferenceError } = await supabase
      .from("user_preferences")
      .select("user_id,notif_push,notif_replies,notif_mentions,notif_messages,notif_upvotes,notif_digest,notif_trending,notif_community,notif_alerts_summary")
      .in("user_id", userIds);
    if (preferenceError) throw preferenceError;
    const preferences = new Map((preferenceRows || []).map((row: Record<string, unknown>) => [row.user_id, row]));
    const preferenceField = preferenceForType(type);
    const eligible = tokens.filter((item: { user_id: string }) => {
      const prefs = preferences.get(item.user_id) as Record<string, unknown> | undefined;
      if (prefs?.notif_push === false) return false;
      if (preferenceField && prefs?.[preferenceField] === false) return false;
      return true;
    });

    const sends = await Promise.all(eligible.map(async (item: { user_id: string; fcm_token: string }) => {
      try {
        const response = await fetch("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send", {
          method: "POST",
          headers: { Authorization: "Bearer " + accessToken, "Content-Type": "application/json" },
          body: JSON.stringify({
            message: {
              token: item.fcm_token,
              notification: { title, body },
              data: { notification_id: notificationId, post_id: postId, type },
              android: { priority: "HIGH" },
            },
          }),
        });
        const responseBody = await response.json().catch(() => ({}));
        if (response.ok) return { sent: 1, failed: 0 };
        const unregistered = responseBody?.error?.details?.some((detail: { errorCode?: string }) => detail.errorCode === "UNREGISTERED");
        if (unregistered) {
          await supabase.from("device_tokens").delete().eq("user_id", item.user_id).eq("fcm_token", item.fcm_token);
        }
        return { sent: 0, failed: 1 };
      } catch {
        return { sent: 0, failed: 1 };
      }
    }));
    const totals = sends.reduce((sum, result) => ({ sent: sum.sent + result.sent, failed: sum.failed + result.failed }), { sent: 0, failed: 0 });
    return jsonResponse({ ...totals, scanned: tokens.length, eligible: eligible.length, mode });
  } catch (error) {
    console.error("send-push failed", error);
    return jsonResponse({ error: error instanceof Error ? error.message : "Unexpected push error" }, 500);
  }
});
