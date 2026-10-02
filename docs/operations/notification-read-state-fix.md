# Notification Read-State Consistency Fix

- Branch: `more-updates-on-nagpur-pulse`
- File: `app/src/main/java/com/nagpurpulse/ui/screens/notifications/NotificationsScreen.kt`
- Commit: `c8a2b67387f616a3d4a57007f0f994e699d36d93`

## Before

When marking a grouped reaction notification as read, the ViewModel sent one request per underlying notification. If one request failed after others succeeded, the UI left the entire group unread locally. The server could therefore contain a mix of read and unread rows while the current screen displayed all as unread until a later refresh.

## Change

The ViewModel now collects IDs whose `markOneRead` request succeeded and updates only those local notification rows. If any request fails, it keeps the remaining items unread and displays an error. It also compares against distinct requested IDs when deciding whether the operation was partial.

## Impact

- No database or Edge Function change.
- No notification rows are deleted.
- Successful server-side updates are reflected immediately.
- Failed updates remain eligible for retry.
- Network/build behavior has not been locally tested yet.

## Rollback

Revert commit `c8a2b67387f616a3d4a57007f0f994e699d36d93` on the working branch to restore the previous all-or-nothing local UI behavior.
