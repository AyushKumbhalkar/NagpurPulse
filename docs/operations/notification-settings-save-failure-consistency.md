# Notification Settings Save-Failure Consistency

## Status
Implemented on `more-updates-on-nagpur-pulse`. This change has not been merged into `master`.

## Problem before the change
Notification settings were updated optimistically in the UI. If the Supabase save failed, the UI restored the affected switch and showed an error, but scheduled notification workers could remain in the schedule produced by the failed optimistic value. For example, turning off master push could cancel scheduled workers; a failed save could restore the switch to enabled without restoring those workers.

## Change made
In `app/src/main/java/com/nagpurpulse/ui/screens/settings/NotifSettingsScreen.kt`, the save-failure path now calls `ScheduledPushManager.reschedule(appContext)` after restoring the switch when the failed field is one of:
- `notif_push`
- `notif_digest`
- `notif_trending`
- `notif_community`
- `notif_alerts_summary`

The rescheduler reads the existing local preference cache and reconciles WorkManager with the restored setting. This complements the repository behavior in `UserPreferencesRepository.saveNotifPref`, which updates `NotifPrefsHelper` only after the Supabase write succeeds.

## Expected behavior after the change
- Successful save: UI and local preference cache reflect the saved value; scheduled workers are reconciled.
- Failed save: affected UI switch reverts, an error snackbar is shown, local preference cache remains unchanged, and scheduled workers are reconciled against that prior cached value.
- Analytics opt-out events remain recorded only after a successful save.

## Rollback
1. Revert commit `3c543fbe6b7b0d0c6a0eb2968a9a681bdbe9d023` on this feature branch, or remove only the added failure-path rescheduling block in `NotifSettingsScreen.kt`.
2. Keep the earlier save-failure UI/cache consistency fixes unless intentionally rolling back that behavior too.
3. No database migration or Edge Function redeployment is required for this Kotlin-only change.
4. Rebuild the Android app locally and test failed saves for master push and each scheduled category before considering a merge.

## Verification status
Repository change committed. Android build and device-level failure-path testing are still pending. Do not treat this change as end-to-end verified until those tests pass.
