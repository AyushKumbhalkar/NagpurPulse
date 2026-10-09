# NagpurPulse — AI Context (paste this FIRST in every chat)
_Last updated: 2026-10-10 · Keep under 150 lines · Update via HANDOFF_LOG after each chat_

## Product
Hyperlocal community app for Nagpur ("Reddit for Nagpur"): posts, comments, alerts, messages, notifications.
Solo founder (Ayush). Goal: production-ready for many users and Google Play review.

## Stack (verified from repo)
- Android, Kotlin 2.0, Jetpack Compose (BOM 2025.01.01), Material3, Hilt, Navigation Compose, Coil.
- Supabase (supabase-kt 3.0.0: auth, postgrest, realtime, storage). Firebase Cloud Messaging.
- appId `com.nagpurpulse`, minSdk 26, compileSdk/targetSdk 34, versionCode 1, R8 minify ON in release.
- Backend in `supabase/migrations` (SQL) and `supabase/functions` (`send-push`, `send-push-notification`).
- No unit/instrumented tests exist yet.

## Code map
- `ui/screens/<feature>/` — splash, auth, onboarding, home, explore, thread, alerts, messages, profile,
  settings, notifications, admin, search.
- `ui/components/` — PostCard, FeedCardVariants, AlertCard, CommentCard, DesignSystem (EmptyState…),
  AnimationUtils (pressScale, shimmerEffect, StaggeredItem), HapticUtils (rememberHaptic).
- `ui/theme/Color.kt` — tokens: OrangePrimary, Surface, SurfaceAlt, Background, PrimaryText, SecondaryText,
  TertiaryText, Divider, GreenSuccess, RedAlert, YellowWarn, BlueInfo (+ AMOLED dark & light themes).
- `ui/navigation/NavGraph.kt` — `Screen` sealed class + all routes.
- `data/repository/*Repository.kt`, `data/model/*`, `data/remote/SupabaseClient.kt`.
- ViewModels live next to their screens (some inside the screen file).

## Conventions (must follow)
1. Analyze existing code first; match naming, theme tokens and helpers. Do not invent new design tokens.
2. Reuse `pressScale`, `rememberHaptic`, `shimmerEffect`, `EmptyState`.
3. Deliverable = ONE unified-diff patch (`git apply` ready). Minimal explanation, max 5 risk bullets.
4. Do not touch files outside the task's file list without asking.
5. Server is authoritative for counts/karma/notifications (see below).
6. SQL changes = new timestamped migration file only; never edit old migrations.

## Backend facts
- Tables seen in migrations: profiles, posts, comments, comment_likes, votes, saved_posts, badges,
  notifications, device_tokens, notification_analytics_daily, user_consents,
  message/conversation hidden tables (+ conversations/messages).
- Done in migrations: server-authoritative notifications, atomic voting, hardened RLS (several rounds),
  account deactivation + secure delete_user_account, revoked anon execute on admin/message RPCs,
  storage upload hardening, consent table, multi-device FCM tokens.
- Admin = `admin_roles` table; UI routing check only, RLS/RPC must stay authoritative.

## Known findings (verify before fixing)
- F1: `PostRepository.addComment` bumps `comment_count` and karma from the client → race/forgeable; move to DB trigger.
- F2: `NotificationRepository.getUnreadCount` downloads rows to count → use count query.
- F3: Debug logs print auth/anon state (`SESSION_CHECK`, `ANON_DB`) → strip in release.
- F4: Two push edge functions exist (`send-push`, `send-push-notification`); one is legacy → consolidate.
- F5: Permissions include FINE+COARSE location and READ_MEDIA_IMAGES → justify, or reduce (photo picker).
- F6: targetSdk 34 → must be raised to Google's current requirement before release.
- F7: No tests, no CI.

## Done
- Notifications screen premium rewrite (patch `notifications-premium-update.patch`): swipe actions, grouping,
  quick reply, weekly summary, pull-to-refresh, pagination, undo-delete.
  Needs server support later: `milestone` notification type, streak data (task DB-12).

## In progress (owner)
- (none yet — fill in from TASK_BOARD)

## Decisions log (append only, newest last)
- 2026-10-10: Work split into 5 account lanes; repo docs are the shared memory.
- 2026-10-10: Supabase connector only on Account 1, staging project only.

## Open questions for the founder
- Is there a `reports`/`blocks` table? (needed for UGC policy) — Account 1 to confirm in DB-08.
- Target audience age gate (13+/18+)? — Account 3 to decide with founder in PS-05.
