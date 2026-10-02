# Notification Popup, Vote State, and Deep-Link Fixes

Branch: `more-updates-on-nagpur-pulse`

## Changes
- Creates notification channels at application startup, rather than waiting for the first in-app FCM callback.
- Adds new high-importance channel IDs for social/comment/vote notifications and general FCM notification-payload fallback. New IDs are necessary because Android preserves the importance selected for an existing channel.
- Routes non-alert social notifications to the high-importance social channel and uses high notification priority for social and message notifications.
- Vote repository no longer interprets a failed existing-vote query as an empty vote list. `getUserVote` returns failure when Supabase cannot be queried.
- Home vote refresh preserves its last known value on a transient lookup error and logs the failure. If vote saving fails, it logs the error and refreshes authoritative posts/votes.
- Notification deep-link collectors wait until the navigation graph has moved past Splash before navigating to a thread or conversation, avoiding navigation against an unready graph during cold start.

## Important limitations
- Android can still suppress heads-up popups if notification permission is denied, the user has disabled popups for the channel, Do Not Disturb is active, or device/OEM settings restrict interruptions. Test channel settings on the device.
- The precise comment/chat crash is not confirmed without Logcat. These changes address the identified cold-start navigation race; if a crash remains, capture `FATAL EXCEPTION` and its `Caused by` lines.
- Vote counter consistency still depends on the database's vote constraints/triggers and concurrent writes. Verify that one vote row per user/post is enforced and test on two accounts.
- No local Android build or device tests were run by this change. Do not merge to `master` until local build and device tests pass.

## Test checklist
1. Build and install from this branch.
2. Confirm notifications are enabled in Android app settings and enable pop-ups for the new channels.
3. Test comment, reply, upvote, and message notifications with app foregrounded and backgrounded.
4. Tap comment and message notifications with app backgrounded and fully closed.
5. Upvote a post from Phone B, verify Phone A receives a notification, and verify the vote remains after refresh and after at least two minutes.
6. Inspect Logcat if a crash or vote-save error remains.
