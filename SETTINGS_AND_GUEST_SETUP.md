# Settings, Guest Mode & Push Notifications Setup

---

## Part 1 — New Supabase Columns (run in SQL Editor)

### 1.1 — Add privacy columns to `profiles` table

```sql
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS hide_comments      BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hide_posts         BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hide_profile       BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS allow_dms          BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS show_online_status BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS incognito_mode     BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hide_from_search   BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS location           TEXT,
    ADD COLUMN IF NOT EXISTS website            TEXT,
    ADD COLUMN IF NOT EXISTS cover_url          TEXT,
    ADD COLUMN IF NOT EXISTS avatar_url         TEXT;
```

### 1.2 — Ensure `notifications` table has required columns

```sql
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS title          TEXT,
    ADD COLUMN IF NOT EXISTS body           TEXT,
    ADD COLUMN IF NOT EXISTS type           TEXT DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS is_read        BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS related_post_id UUID REFERENCES posts(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS created_at     TIMESTAMPTZ DEFAULT NOW();
```

### 1.3 — Notification RLS

```sql
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Own notifications"
    ON notifications FOR ALL
    USING (auth.uid() = user_id);
```

---

## Part 2 — Push Notifications (WorkManager)

Push notifications are scheduled via WorkManager — **no extra setup needed** in
Supabase. The app uses 4 workers:

| Worker | Time | Content |
|--------|------|---------|
| `MorningDigestWorker` | 8:00 AM | Top trending posts |
| `AfternoonTrendingWorker` | 1:00 PM | Hottest post right now |
| `EveningCommunityWorker` | 6:00 PM | Community engagement prompt |
| `NightAlertsSummaryWorker` | 10:00 PM | Active alert count |

Workers are registered in `NagpurPulseApp.onCreate()` via `ScheduledPushManager.schedule(context)`.

### To add WorkManager to `build.gradle.kts`:

```kotlin
// In app/build.gradle.kts dependencies block
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.hilt:hilt-work:1.2.0")
kapt("androidx.hilt:hilt-compiler:1.2.0")
```

### To show notification icon — replace this line in `ScheduledPushManager.kt`:

```kotlin
.setSmallIcon(android.R.drawable.ic_dialog_info)
// Replace with your actual drawable:
.setSmallIcon(R.drawable.ic_notification)
```

Add `ic_notification.xml` to `res/drawable/` — a simple white waveform icon works.

---

## Part 3 — Guest Mode

Guest mode is entirely client-side. When a user taps **"Continue as Guest"** on
the Onboarding screen:

1. `authRepository.enterGuestMode()` is called — sets an in-memory `_isGuest = true` flag
2. User is navigated to `HomeScreen`
3. Every write action (vote, comment, save, post) checks `authRepository.canWrite()`
4. If `canWrite()` returns false, `GuestLoginPrompt` dialog is shown

### Wiring write guards in your screens

Use this pattern wherever a write action happens:

```kotlin
// In any ViewModel that does writes
@HiltViewModel
class ExampleViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel() {

    // Check before ANY write
    fun upvote(postId: String) {
        if (!authRepository.canWrite()) {
            _uiState.value = _uiState.value.copy(showGuestPrompt = true)
            return
        }
        // proceed with Supabase call...
    }
}

// In the Composable
if (uiState.showGuestPrompt) {
    GuestLoginPrompt(
        visible      = true,
        onDismiss    = { viewModel.dismissGuestPrompt() },
        onLoginClick = { navController.navigate(Screen.Login.route) }
    )
}
```

The `GuestLoginPrompt` composable is in `ui/components/GuestGuard.kt`.

---

## Part 4 — Settings Screens Overview

| Screen | Route | What it does |
|--------|-------|-------------|
| `SettingsScreen` | `/settings` | Main settings hub |
| `AccountProfileScreen` | `/settings/account_profile` | Edit display name, bio, location, avatar, privacy toggles |
| `PrivacySettingsScreen` | `/settings/privacy` | Granular visibility controls |
| `IncognitoSettingsScreen` | `/settings/incognito` | Incognito mode, avatar, location hiding |
| `NotifSettingsScreen` | `/settings/notifications` | Push, replies, mentions, DM toggles |
| `SecuritySettingsScreen` | `/settings/security` | Change password, 2FA, delete account |

All toggle changes are **immediately saved to Supabase** via `authRepository.updatePrivacySettings()`.

Profile field edits are saved when the user taps **Save** in `AccountProfileScreen`.
