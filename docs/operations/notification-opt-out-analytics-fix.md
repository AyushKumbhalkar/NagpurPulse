# Notification Opt-Out Analytics Accuracy Fix

- Branch: `more-updates-on-nagpur-pulse`
- Implementation commit: `b5cd4a82112c48e2fe0521b9596787dc3714afd1`

## Before
The notification settings ViewModel ignored the result from saving a preference and recorded `push_opt_out` whenever a toggle was turned off, including failed Supabase saves.

## After
The ViewModel records the aggregate opt-out event only when `saveNotifPref` returns success and the value is being switched off.

## Scope and limitations
- Android settings ViewModel only; no SQL, production database, or Edge Function changes.
- This fixes analytics counting, not preference-save failure UX. The repository currently mirrors the selected value to SharedPreferences before the remote save, so a failed save can still leave local and remote preference state temporarily different.
- Android build and runtime behavior have not yet been tested.

## Rollback
Revert implementation commit `b5cd4a82112c48e2fe0521b9596787dc3714afd1`.
