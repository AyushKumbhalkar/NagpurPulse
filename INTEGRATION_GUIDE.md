# NagpurPulse — Admin Panel Integration Guide

## What's included

| File | Action |
|------|--------|
| `ADMIN_SCHEMA.sql` | Run once in Supabase SQL Editor |
| `AdminModels.kt` | New file → `data/model/` |
| `AdminRepository.kt` | New file → `data/repository/` |
| `AdminViewModel.kt` | New file → `ui/screens/admin/` |
| `AdminPanelScreen.kt` | New file → `ui/screens/admin/` |
| `Post.kt` | **Merge** — add 2 new fields to your existing Post.kt |
| `AppModule.kt` | **Merge** — add one `@Provides` block for AdminRepository |
| `SettingsScreen.kt` | **Replace** your existing SettingsScreen.kt |
| `NavGraph.kt` | **Replace** your existing NavGraph.kt |

---

## Step 1 — Run the SQL in Supabase

1. Open your Supabase project → SQL Editor → New Query
2. Paste the entire `ADMIN_SCHEMA.sql` and click **Run**
3. Grant yourself admin access (replace with YOUR actual UUID from
   Supabase → Authentication → Users):

```sql
insert into admin_roles (user_id, role, granted_by)
values ('YOUR-UUID-HERE', 'super_admin', 'YOUR-UUID-HERE');
```

---

## Step 2 — Add new fields to Post.kt

Open `data/model/Post.kt` and add these two lines inside the data class:

```kotlin
@SerialName("is_pinned") val isPinned: Boolean = false,
@SerialName("is_locked") val isLocked: Boolean = false,
```

---

## Step 3 — Add new files

Create the directory `ui/screens/admin/` and copy in:
- `AdminViewModel.kt`
- `AdminPanelScreen.kt`

Copy to their existing directories:
- `data/model/AdminModels.kt`
- `data/repository/AdminRepository.kt`

---

## Step 4 — Update AppModule.kt

In your existing `di/AppModule.kt`, add one `@Provides` block anywhere
alongside the other repository providers:

```kotlin
@Provides @Singleton
fun provideAdminRepository(client: SupabaseClient): AdminRepository =
    AdminRepository(client)
```

Also add the import at the top:
```kotlin
import com.nagpurpulse.data.repository.AdminRepository
```

---

## Step 5 — Update SettingsViewModel

In your existing `SettingsViewModel`, add the `AdminRepository` parameter
and the `checkAdminRole()` call.  The easiest approach is to **replace**
`SettingsScreen.kt` with the version provided, which already has both
integrated. The UI is identical to what you had — the only additions are:

- `AdminRepository` injected into the ViewModel
- `isAdmin` and `adminRole` in `SettingsUiState`
- `checkAdminRole()` called in `init {}`
- `AdminPanelSettingsCard` rendered at the top of the settings list
  (only visible when `isAdmin == true`)

---

## Step 6 — Replace NavGraph.kt

Replace your existing `NavGraph.kt` with the provided version.
The only changes are:

```kotlin
// 1. New import at top
import com.nagpurpulse.ui.screens.admin.AdminPanelScreen

// 2. New route in sealed class
object AdminPanel : Screen("admin_panel")

// 3. New composable at bottom of NavHost
composable(
    route             = Screen.AdminPanel.route,
    enterTransition   = { sheetEnter(this) },
    exitTransition    = { sheetExit(this) },
    popExitTransition = { sheetExit(this) }
) {
    AdminPanelScreen(navController = navController)
}
```

---

## How the admin check works

```
App start
  └── SettingsViewModel.init()
        └── AdminRepository.isAdmin()
              └── Queries admin_roles WHERE user_id = current_uid
                    ├── Row exists  →  shows "Admin Panel" card in Settings
                    └── No row      →  nothing shown (invisible to regular users)
```

The check is performed **every time** Settings is opened, so granting/revoking
access in Supabase takes effect on the next Settings screen open — no rebuild
needed.

---

## Admin Panel screens

| Tab | What it shows |
|-----|--------------|
| **Dashboard** | Live stats, weekly activity chart, pending queue preview |
| **Queue** | All pending reports (posts + comments), filterable. Delete / Ignore / Warn actions |
| **Posts** | All posts with pin, lock, delete, search and sort |
| **Users** | Username search → full user card with Warn / Suspend 7 / Suspend 30 / Permanent Ban |
| **Logs** | Audit trail of every admin action, filterable by type |

---

## Granting admin access to other users

```sql
-- Make someone a moderator
insert into admin_roles (user_id, role, granted_by)
values ('THEIR-UUID', 'moderator', 'YOUR-UUID');

-- Revoke access
delete from admin_roles where user_id = 'THEIR-UUID';
```

Roles: `super_admin` | `admin` | `moderator`
(The role label shown in the UI badge changes automatically.)

---

## How user suspension works

When you Warn / Suspend / Ban a user:
1. A row is written to `user_suspensions`
2. A push notification is inserted into `notifications` for that user
3. An audit entry is written to `admin_actions`

To enforce the suspension in your app, add a check in your post/comment
submission flow:

```kotlin
// In PostRepository or wherever you create posts:
val suspension = supabase.postgrest["user_suspensions"]
    .select { filter { eq("user_id", currentUserId) } }
    .decodeList<UserSuspension>()
    .firstOrNull()

if (suspension != null) {
    if (suspension.isPermanent) throw Exception("Your account has been banned.")
    val until = Instant.parse(suspension.suspendedUntil ?: "")
    if (Instant.now().isBefore(until)) throw Exception("Your account is suspended until $until.")
}
```

---

## Theming

The Admin Panel uses your existing `OrangePrimary`, `RedAlert`, `BlueInfo`,
`GreenSuccess`, `YellowWarn`, `PurpleNight` colour tokens from `Color.kt`.
No new colours were added.

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Admin card not showing in Settings | Check `admin_roles` table has your `user_id`. Run the `isAdmin()` check manually. |
| Stats all show 0 | RLS on `admin_roles` — make sure your policies allow the authenticated user to SELECT their own row. |
| Delete post failing | Check Supabase RLS on `posts` table — admin needs DELETE permission or use a service role edge function. |
| Suspend user not persisting | Check the `user_suspensions` upsert RLS policy. |
