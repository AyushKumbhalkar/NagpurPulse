# User Preferences Fetch Error Handling

## Status
Implemented on `more-updates-on-nagpur-pulse`; not merged to `master`.

## Previous behavior
`UserPreferencesRepository.getPreferences()` used `decodeSingle` and caught every exception by returning a default `UserPreferences` object. This made network, permission, schema, and decoding failures indistinguishable from a user who had no preferences row. In notification settings, the default object could then be synced into SharedPreferences, replacing the intended offline-cache fallback.

## Change
The repository now fetches a list:
- If the query succeeds and returns no row, it returns first-run defaults for the current user.
- If a row exists, it returns the first row.
- If the query fails for a real reason, it returns `Result.failure(e)`.

This allows `loadAndSyncNotifPrefs()` to return null on fetch failure, so `NotifSettingsViewModel` uses the existing SharedPreferences fallback instead of overwriting it with defaults.

## Scope and compatibility
Other preference getters already use `getOrNull()` and retain their existing per-setting defaults when the query fails. This change does not alter the database schema and requires no Edge Function redeployment.

## Rollback
Revert commit `5cd629e6dc2c1a3ca70bd44cd7b362a332dbe76d` on the feature branch, or restore the previous `getPreferences()` implementation. No database rollback is needed.

## Verification still required
Build the Android app locally. Test both a user with a saved preference row and a first-run user with no row. Simulate an offline/network failure and confirm notification settings fall back to SharedPreferences without replacing the cached values. Do not merge until these checks pass.
