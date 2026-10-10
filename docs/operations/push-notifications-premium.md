# Premium push notifications

## Deploy order
1. Apply `supabase/migrations/20261011120000_push_engagement_and_device_tokens.sql`.
   It dedupes `device_tokens` (keeps the newest row per token), adds the unique `fcm_token`
   index the app already relies on, and creates `register_device_token` and
   `get_engagement_snapshot`.
2. Deploy the edge function: `supabase functions deploy send-push-notification`.
   It now sends DATA-ONLY messages (title/body included, so older app builds still work).
3. Ship the Android build.

## What changed
- Tray notifications: monochrome status icon, per-type accent + vector glyph, sender avatar
  with type badge, grouped summaries, inline Reply / Mark as read, MessagingStyle for chats,
  cover image for trending posts. No emoji anywhere (system titles are stripped server- and
  client-side; user content is left untouched).
- Upvote / like bursts collapse into one live-updating notification.
- Scheduled updates use real numbers only; nothing is sent when there is nothing to say.
- Streak-saver / comeback nudge (`ReengagementWorker`, 20:30 local): never if the app was
  opened today, max 3 per absence (gaps 20h, 72h, 7d), obeys pause / quiet hours / the
  Community toggle, and has its own "For you" channel the user can mute.
- Tapping a push marks it read.
- Settings -> Notifications -> Preview sends sample notifications.

## Manual test checklist
- Background and force-quit the app, then trigger a comment: tray entry must be styled.
- Two upvotes on one post: one entry, "A and B upvoted your post", no second sound.
- Reply from the shade to a chat message and to a comment.
- Sign in as account B on a phone previously used by account A: token must move to B.
