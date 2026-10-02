# Legacy `send-push` Function Review

## Status
Reviewed on 2026-10-02 against branch `more-updates-on-nagpur-pulse` and the live Supabase project `eazkmfzegxmdkbowohiy`. No production change or Edge Function redeployment was made as part of this review.

## Finding: not the active notification-insert delivery path
The live `public.notifications` table has one non-internal trigger:
`on_new_notification AFTER INSERT ... EXECUTE FUNCTION dispatch_notification_push()`.

The checked-in migration `20261001212301_fix_notification_push_pipeline.sql` explicitly drops the legacy `on_notification_insert` trigger and `public.notify_user_on_insert()`, documenting `send-push-notification` as the canonical sender. The live `dispatch_notification_push()` function posts to:
`https://eazkmfzegxmdkbowohiy.supabase.co/functions/v1/send-push-notification`.

Therefore, the separate `send-push` function is deployed and ACTIVE in Supabase (version 15), but it is **not wired to the live database notification-insert trigger**. Repository code search did not find a direct app invocation in the indexed results. This review does not rule out an external/manual caller or a caller not indexed by GitHub search, so do not delete or disable it without checking operational callers/logs.

## Pagination defect found in the separate function
Broadcast mode paginates `device_tokens` ordered only by `user_id`, then advances using `.gt("user_id", lastUserId)`. Since `device_tokens` has an independent UUID `id` and does not guarantee one token per user, if a 400-row page ends in the middle of multiple tokens belonging to the same user, the next page skips every remaining token for that user. Invalid-token deletion during a broadcast can also shift offset-free keyset boundaries, although keyset pagination avoids the usual offset-shift issue for rows before the cursor.

Live inspection showed 2 token rows for 2 distinct users at review time, so the duplicate-token boundary condition was not present in the current data sample. That small snapshot does not prove it cannot occur later.

## Decision
- Do not deploy changes to `send-push` as part of the current Android testing step because it is not the live notification-insert sender.
- If the function is retained for future broadcast use, fix pagination with a stable unique composite cursor (for example `user_id` plus token-row `id` or `fcm_token`) and test page boundaries where a user has multiple devices.
- Keep `send-push-notification` as the canonical live sender until a deliberate architecture change is reviewed and tested.

## Android testing handoff
No Android build or device tests have been run by this review. Proceed with the local Android Studio build and the notification test checklist before any merge to `master`.
