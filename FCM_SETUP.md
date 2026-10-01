# 🔔 Firebase Push Notifications Setup

## Step 1 — Create a Firebase project

1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add project** → name it `NagpurPulse`
3. Disable Google Analytics (optional) → **Create project**

## Step 2 — Register your Android app

1. On the Firebase dashboard click the **Android** icon
2. Package name: `com.nagpurpulse`
3. App nickname: `Nagpur Pulse`
4. Click **Register app**
5. Download **`google-services.json`**
6. Place it at: `NagpurPulse/app/google-services.json`

## Step 3 — Enable Cloud Messaging

Firebase Console → your project → **Build → Cloud Messaging** → it's auto-enabled.

## Step 4 — Get your Server Key (for Supabase Edge Functions)

Firebase Console → Project settings ⚙️ → **Cloud Messaging** tab  
Copy the **Server key** — you'll need this in Supabase.

## Step 5 — Supabase Edge Function for sending pushes

In your Supabase dashboard → **Edge Functions** → **New Function** → name it `send-push-notification`

Paste this code:

```typescript
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const FCM_SERVER_KEY = Deno.env.get("FCM_SERVER_KEY") ?? ""

serve(async (req) => {
  const { user_id, title, body, post_id, type } = await req.json()

  // Get FCM token from Supabase
  const supabaseUrl = Deno.env.get("SUPABASE_URL")!
  const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

  const tokenRes = await fetch(`${supabaseUrl}/rest/v1/device_tokens?user_id=eq.${user_id}`, {
    headers: { "apikey": supabaseKey, "Authorization": `Bearer ${supabaseKey}` }
  })
  const tokens = await tokenRes.json()
  if (!tokens.length) return new Response("no token", { status: 200 })

  const fcmToken = tokens[0].fcm_token

  // Send via FCM
  const fcmRes = await fetch("https://fcm.googleapis.com/fcm/send", {
    method: "POST",
    headers: {
      "Authorization": `key=${FCM_SERVER_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      to: fcmToken,
      notification: { title, body },
      data: { type, post_id: post_id ?? "" }
    })
  })

  const result = await fcmRes.json()
  return new Response(JSON.stringify(result), { status: 200 })
})
```

Set environment variable in Supabase → Edge Functions → `FCM_SERVER_KEY` = your Firebase server key.

## Step 6 — Supabase Database Webhook (auto-trigger push on new notification row)

Supabase Dashboard → **Database → Webhooks** → **Create webhook**:

- **Name**: `on_new_notification`
- **Table**: `notifications`
- **Events**: `INSERT`
- **HTTP method**: `POST`
- **URL**: `https://YOUR_PROJECT.supabase.co/functions/v1/send-push-notification`
- **HTTP headers**: `Authorization: Bearer YOUR_ANON_KEY`

This means: whenever a row is inserted into `notifications`, the Edge Function fires and sends the push automatically. 🎉

## What triggers notifications automatically in the app

| Action | Who gets notified | Type |
|--------|------------------|------|
| Someone comments on your post | Post author | `comment` |
| Your post hits 10/50/100/500/1000 upvotes | Post author | `upvote` |
| New alert in your area | All users with that area | (via manual broadcast) |
| New badge earned | Badge earner | `badge` |

## Testing

Use Firebase Console → **Cloud Messaging** → **Send test message**  
Enter a device FCM token (printed in Logcat on first app launch as `FCM Token: xxxxx`)
