# Notification Deep-Link Crash Follow-up

Branch: `more-updates-on-nagpur-pulse`

## Confirmed crash
Device Logcat showed:

`IllegalArgumentException: Navigation destination ... uri=android-app://androidx.navigation/chat/ cannot be found`

The generated route was `chat/`, which means the conversation ID was empty when the notification deep-link collector attempted navigation. The trace pointed to `MainActivity.kt` in the conversation notification collector.

## Fix
- Cold-start and warm-start intent handling now ignores blank `conversation_id` values.
- The collector checks `isNullOrBlank()` before constructing a Chat route and clears malformed pending values instead of navigating to `chat/`.
- Valid conversation IDs still wait for Splash to leave the active destination before navigation.

## Verification needed
Build and install locally, then test valid message and comment notifications both from a fully closed app and a backgrounded app. If the comment notification still crashes, capture the new `FATAL EXCEPTION` block: the supplied trace specifically identified the invalid Chat route, not a Thread route.

## Remaining findings
- The app inserts new posts into `notification_queue`, but production had no `pg_cron` jobs registered and no posts-table insert trigger that creates new-post notifications. Queueing alone does not send alerts.
- Vote state/counters still need a separate fix. `votePost` reads post counters and writes absolute values from that snapshot, which can overwrite concurrent updates. No database changes were made as part of this follow-up.
