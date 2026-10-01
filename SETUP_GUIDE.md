# NagpurPulse — Complete Production Setup Guide

This guide covers **every third-party service** the app uses, exactly what you need to
configure, and what is already handled automatically by the code.

---

## Services Used

| Service | Used For | Status |
|---------|----------|--------|
| **Supabase** | Database, Auth, Storage, Realtime | Already configured by you |
| **Firebase** | Push notifications (FCM) | Needs `google-services.json` |

---

## Part 1 — Supabase (you said tables are ready ✅)

### 1.1 Add your keys to `local.properties`

Open (or create) the file `local.properties` in the **root** of the project (next to `settings.gradle.kts`).

```properties
# local.properties  — never commit this file to git
SUPABASE_URL=https://xxxxxxxxxxxxxxxxxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Where to find these values:
- Go to **Supabase Dashboard → Project Settings → API**
- Copy **Project URL** → `SUPABASE_URL`
- Copy **anon / public key** → `SUPABASE_ANON_KEY`

### 1.2 Storage bucket for post images

The app uploads post images to a bucket called `post-images`. Create it if not done:

1. Go to **Supabase Dashboard → Storage → New bucket**
2. Name: `post-images`
3. Set as **Public** (so images load without auth)
4. Under **Policies**, add an INSERT policy so authenticated users can upload:

```sql
-- Allow authenticated users to upload
CREATE POLICY "Authenticated users can upload post images"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'post-images');

-- Allow anyone to read
CREATE POLICY "Public read post images"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'post-images');
```

### 1.3 Tables you already have (reference)

| Table | Key columns |
|-------|------------|
| `profiles` | `id`, `username`, `tagline`, `avatar_url`, `areas[]`, `karma`, `created_at` |
| `posts` | `id`, `user_id`, `title`, `body`, `category`, `area_tag`, `is_anonymous`, `upvotes`, `downvotes`, `comment_count`, `view_count`, `image_url`, `is_alert`, `alert_severity`, `created_at` |
| `comments` | `id`, `post_id`, `user_id`, `parent_id`, `body`, `upvotes`, `is_anonymous`, `created_at` |
| `votes` | `id`, `user_id`, `post_id`, `vote_type` (`up`/`down`), `created_at` |
| `saved_posts` | `id`, `user_id`, `post_id`, `created_at` |
| `badges` | `id`, `user_id`, `badge_type`, `earned_at` |
| `notifications` | `id`, `user_id`, `type`, `title`, `body`, `is_read`, `related_post_id`, `created_at` |
| `device_tokens` | `id`, `user_id`, `fcm_token`, `updated_at` |

### 1.4 Row Level Security (recommended)

Run these in Supabase **SQL Editor**:

```sql
-- Profiles: anyone can read, only owner can update
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public profiles" ON profiles FOR SELECT USING (true);
CREATE POLICY "Own profile update" ON profiles FOR UPDATE USING (auth.uid() = id);

-- Posts: anyone can read, only owner can delete
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public posts" ON posts FOR SELECT USING (true);
CREATE POLICY "Authenticated insert" ON posts FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Own post delete" ON posts FOR DELETE USING (auth.uid() = user_id);
CREATE POLICY "Vote update" ON posts FOR UPDATE TO authenticated USING (true);

-- Comments
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public comments" ON comments FOR SELECT USING (true);
CREATE POLICY "Authenticated comment insert" ON comments FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

-- Votes
ALTER TABLE votes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own votes" ON votes FOR ALL USING (auth.uid() = user_id);

-- Saved posts
ALTER TABLE saved_posts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own saved" ON saved_posts FOR ALL USING (auth.uid() = user_id);

-- Notifications
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own notifications" ON notifications FOR ALL USING (auth.uid() = user_id);

-- Device tokens
ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own device token" ON device_tokens FOR ALL USING (auth.uid() = user_id);
```

### 1.5 Realtime (for live alerts)

1. Go to **Supabase Dashboard → Database → Replication**
2. Enable **Realtime** for the `posts` table
3. The app already subscribes to `INSERT` events on `posts` for the Alerts screen

---

## Part 2 — Firebase (Push Notifications)

### 2.1 Create Firebase project

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add project** → name it `NagpurPulse`
3. Disable Google Analytics (optional, saves setup time)
4. Click **Create project**

### 2.2 Add Android app to Firebase

1. In Firebase Console, click the **Android icon** (Add app)
2. Android package name: `com.nagpurpulse`
3. App nickname: `NagpurPulse`
4. SHA-1: run this in Android Studio terminal:
   ```bash
   ./gradlew signingReport
   ```
   Copy the **SHA-1** from `debug` variant and paste it
5. Click **Register app**
6. **Download `google-services.json`**
7. Place it at: `app/google-services.json` (same folder as `app/build.gradle.kts`)

### 2.3 Verify Firebase in build files

`build.gradle.kts` (root) already has:
```kotlin
id("com.google.gms.google-services") version "4.4.0" apply false
```

`app/build.gradle.kts` already has:
```kotlin
id("com.google.gms.google-services")
// and
implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
implementation("com.google.firebase:firebase-messaging-ktx")
```

### 2.4 Set up FCM → Supabase bridge (send push from backend)

When a user posts a comment, the app inserts a row into `notifications`. To also send
a **push notification**, create this Supabase Edge Function:

**Create file:** `supabase/functions/send-push/index.ts`

```typescript
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const FIREBASE_SERVER_KEY = Deno.env.get("FIREBASE_SERVER_KEY")!

serve(async (req) => {
  const { userId, title, body, postId } = await req.json()

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  )

  // Get FCM token for user
  const { data: tokens } = await supabase
    .from("device_tokens")
    .select("fcm_token")
    .eq("user_id", userId)

  if (!tokens || tokens.length === 0) {
    return new Response(JSON.stringify({ sent: 0 }), { status: 200 })
  }

  // Send to each device
  const sends = tokens.map(({ fcm_token }) =>
    fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: {
        "Authorization": `key=${FIREBASE_SERVER_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        to: fcm_token,
        notification: { title, body },
        data: { post_id: postId ?? "", type: "comment" },
      }),
    })
  )

  await Promise.allSettled(sends)
  return new Response(JSON.stringify({ sent: tokens.length }), { status: 200 })
})
```

**Deploy it:**
```bash
supabase functions deploy send-push
```

**Set secrets:**
```bash
supabase secrets set FIREBASE_SERVER_KEY=your_firebase_server_key_here
```

Where to get Firebase Server Key:
- Firebase Console → Project Settings → Cloud Messaging → **Server key**

**Create a database trigger** to call this function whenever a notification row is inserted:

```sql
-- In Supabase SQL Editor
CREATE OR REPLACE FUNCTION notify_user_on_insert()
RETURNS trigger AS $$
BEGIN
  PERFORM net.http_post(
    url    := current_setting('app.supabase_url') || '/functions/v1/send-push',
    headers := json_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer ' || current_setting('app.service_role_key')
    )::jsonb,
    body   := json_build_object(
      'userId', NEW.user_id,
      'title',  NEW.title,
      'body',   NEW.body,
      'postId', NEW.related_post_id
    )::jsonb
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_notification_insert
  AFTER INSERT ON notifications
  FOR EACH ROW EXECUTE FUNCTION notify_user_on_insert();
```

> **Note:** This requires the `pg_net` extension. Enable it in
> Supabase Dashboard → Database → Extensions → search `pg_net` → Enable.

---

## Part 3 — Build & Run

### 3.1 Sync and build

```bash
# In Android Studio: File → Sync Project with Gradle Files
# Or from terminal:
./gradlew assembleDebug
```

### 3.2 Run on device / emulator

```bash
./gradlew installDebug
```

### 3.3 Check Supabase connection

In Logcat, filter by `SUPABASE` — you should see no error logs. If you see
`401 Unauthorized`, double-check your `SUPABASE_ANON_KEY` in `local.properties`.

---

## Part 4 — Google Sign-In (optional, not yet wired)

The Login/Signup screens show a **Google** button. To make it functional:

1. In Firebase Console → Authentication → Sign-in method → Enable **Google**
2. Add your SHA-1 fingerprint (same as Part 2.2 step 4)
3. In Supabase Dashboard → Authentication → Providers → Enable **Google**
4. Copy the **Callback URL** from Supabase
5. In Google Cloud Console → OAuth consent screen → add that callback URL
6. Paste **Client ID** and **Client Secret** from Google into Supabase Google provider settings

Wire up in `AuthScreens.kt`:
```kotlin
SocialButton("Google", "G", onClick = {
    // Launch Google Sign-In intent here
    // See: https://supabase.com/docs/guides/auth/social-login/auth-google
})
```

---

## Part 5 — Production Checklist

Before releasing to Play Store:

- [ ] `local.properties` is in `.gitignore` (never commit keys)
- [ ] Enable **ProGuard/R8** in `build.gradle.kts`: `isMinifyEnabled = true`
- [ ] Replace `android:usesCleartextTraffic="true"` with HTTPS-only in prod
- [ ] Set up proper **SSL pinning** for Supabase requests
- [ ] Change Supabase RLS policies from permissive to strict (see Part 1.4)
- [ ] Upload your own app icon to replace the placeholder
- [ ] Test FCM push on a real device (not emulator)
- [ ] Set `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Configure Play Store signing key and update SHA-1 in Firebase

---

## Quick Reference — Key Files

| File | Purpose |
|------|---------|
| `local.properties` | Supabase URL + key (you create this, never commit) |
| `app/google-services.json` | Firebase config (download from Firebase Console) |
| `data/remote/SupabaseClient.kt` | Supabase client initialization |
| `data/repository/AuthRepository.kt` | Login, signup, signout |
| `data/repository/PostRepository.kt` | Posts CRUD, votes, comments, image upload |
| `data/repository/NotificationRepository.kt` | FCM token save, notifications |
| `notifications/NagpurPulseFcmService.kt` | Handles incoming push messages |
| `di/AppModule.kt` | Hilt dependency injection wiring |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `SUPABASE_URL not set` | Add `SUPABASE_URL=...` to `local.properties` |
| Images not uploading | Create `post-images` bucket (see 1.2) |
| Push notifications not received | Check `google-services.json` is at `app/` level |
| Login gives `Invalid login credentials` | User doesn't exist — sign up first |
| `postgrest error 406` | Check that `select()` is chained after `insert()` |
| Realtime not working | Enable Realtime for `posts` table in Supabase Dashboard |
