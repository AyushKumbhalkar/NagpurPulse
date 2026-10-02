# Atomic Voting and Selected/Trending Post Notifications

Branch: `more-updates-on-nagpur-pulse`

## Production migrations
- `20261003120000_atomic_post_voting.sql` — applied as Supabase migration `atomic_post_voting`.
- `20261003130000_selected_trending_post_notifications.sql` — applied as Supabase migration `selected_trending_post_notifications`.

## Atomic voting
- Added `public.vote_post_atomic(p_post_id uuid, p_vote_type text)`.
- Requires an authenticated Supabase user; uses `auth.uid()` as the voter identity and does not trust a caller-supplied user ID.
- Locks the post row, reads/locks the user's existing vote, then inserts/deletes/changes the vote, adjusts counters, and adjusts the post owner's karma in the same transaction.
- Grants execute to `authenticated` and `service_role`; denies `anon` and `PUBLIC`.
- Android `PostRepository.votePost` now invokes this RPC instead of issuing separate reads/writes with stale absolute counter values.

## Selected/trending post notifications
- Ordinary posts do not trigger fan-out notifications.
- When a post is created already pinned/selected or with `post_type = 'trending'`, or transitions into one of those states, opted-in recipients are notified.
- Recipients must have both `notif_push` and `notif_trending` enabled (NULL/missing preferences default to enabled for compatibility); the post author is excluded.
- Existing selected posts do not send duplicates on unrelated edits.
- Uses the existing notification insert/webhook pipeline for FCM delivery.

## Verification
- Production verification confirms both post triggers exist.
- Production function privileges confirm `vote_post_atomic` is executable by authenticated/service_role and not anon; notification trigger function is not directly executable by anon/authenticated.
- No live vote was created/deleted for verification, to avoid changing user data.
- Android Studio build and real two-account/device test remain required.

## Rollback cautions
- Rolling back the Android RPC call before removing the SQL function is safe, but restores the previous non-atomic behavior.
- Remove selected/trending post triggers before dropping their function.
- Do not drop the atomic vote function while a released Android build still calls it.
- Existing vote rows/counters are not backfilled or reconciled by these migrations.
