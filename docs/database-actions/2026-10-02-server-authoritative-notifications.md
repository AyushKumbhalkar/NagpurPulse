# Database Actions Log — NagpurPulse Notifications

This branch records database changes made for the server-authoritative notification system. It is separate from `master`.

- Repository: `AyushKumbhalkar/NagpurPulse`
- Database project ref: `eazkmfzegxmdkbowohiy`
- Related app branch: `feature/reddit-notifications-v2`
- Related migration file: `supabase/migrations/20261002150000_server_authoritative_notifications.sql`
- Log status: **APPLIED TO PRODUCTION; post-deployment schema verification passed.**

## Safety / scope

- Production changes are recorded below with the Supabase migration name/version and verified outcomes.
- Never record service-role keys, webhook secrets, access tokens, or user private data here.
- Do not merge this branch into `master` as a substitute for applying migrations.
- Rollback must be a separately reviewed migration; do not blindly re-enable client-created notifications because that permits clients to forge notification rows.

## Migration: server-authoritative notification events

Source: `supabase/migrations/20261002150000_server_authoritative_notifications.sql`

### Actions applied to the live database

1. **Added** `public.create_notification_event(...)`, a restricted `SECURITY DEFINER` helper. It inserts notification rows and resolves sender username/avatar server-side.
2. **Added** `public.notify_comment_insert()` and the `notifications_from_comment_insert` AFTER INSERT trigger on `public.comments`:
   - Notifies a post owner when another non-anonymous user comments.
   - Notifies a parent comment author when another non-anonymous user replies.
   - Resolves @username mentions to profile IDs and honors notification preferences.
   - Skips anonymous comments to avoid exposing the author through notification metadata.
3. **Added** `public.notify_post_upvote()` and two triggers on `public.votes`:
   - `notifications_from_post_upvote_insert` (AFTER INSERT)
   - `notifications_from_post_upvote_update` (AFTER UPDATE OF vote_type)
   - Notifies a post owner of another user's upvote, honors upvote preferences, and checks actual vote rows for milestones at 10, 50, 100, 500, and 1,000.
4. **Added** `public.notify_message_insert()` and the `notifications_from_message_insert` AFTER INSERT trigger on `public.messages`. It only notifies the other participant when the sender belongs to the conversation and honors message preferences.
5. **Restricted function execution** by revoking direct execution of the helper/trigger functions from `PUBLIC`, `anon`, and `authenticated`.
6. **Removed permissive notification write policies** whose command was INSERT or ALL. Post-deployment verification found these remaining notification policies:
   - `Users can view own notifications` [SELECT]
   - `Users read own notifications` [SELECT]
   - `Users can update own notifications` [UPDATE]
   - `Users update own notifications` [UPDATE]
7. **Preserved** the existing `on_new_notification` trigger on `public.notifications`, which dispatches to the existing push-notification pipeline.

### Deployment record — 2026-10-02

- Status: **APPLIED TO PRODUCTION**
- Supabase migration name: `server_authoritative_notifications`
- Supabase migration version reported after application: `20261002110559`
- Apply operation returned: success.
- Post-deployment verification: all four new event triggers and the existing push-dispatch trigger were present.
- Post-deployment RLS verification: only recipient-scoped SELECT/UPDATE notification policies remained; INSERT/ALL policies were absent.
- No Supabase development branch was created.
- No app code was merged into `master` by this database operation.

## Known limitations and rollout risks

- Comment likes are now implemented in a separate migration and app feature branch; see the next section. The production deployment is schema-verified, but app compilation and end-to-end FCM delivery still require testing.
- Creating a post does not generate a notification in this migration.
- The upvote-milestone existence check can race if concurrent votes reach the same threshold. A dedicated unique event key/constraint would be a future hardening step.
- Anonymous comment notifications are intentionally suppressed to protect author privacy.
- Legacy app builds that still attempt direct INSERTs into `public.notifications` may receive insert failures because permissive INSERT policies were removed. Roll out the matching app version and check logs before broad release.
- Push delivery still depends on the existing `on_new_notification` trigger, Vault webhook secret, and `send-push-notification` Edge Function. This action verified the database trigger remains present, but did not send a real FCM test.
- No synthetic user activity was inserted during verification; live notification delivery has not yet been end-to-end tested after deployment.

## Rollback / follow-up

- Rollback performed: **No**.
- Next: inspect the comment-like write path, review the milestone race, then perform a controlled end-to-end test with two test accounts. Do not re-enable broad client INSERT policies as a workaround.


## Migration: per-user comment likes and notifications

Source: `supabase/migrations/20261002170000_comment_likes_and_notifications.sql` on `feature/comment-like-notifications`.

### Actions applied to the live database

1. **Created** `public.comment_likes` with foreign keys to comments/auth users and a unique constraint on `(comment_id, user_id)` to prevent duplicate likes by the same user.
2. **Enabled RLS** and added authenticated-user-scoped SELECT, INSERT, and DELETE policies. Granted only the table operations required by the app.
3. **Added** `maintain_comment_like_count()` and insert/delete triggers to increment/decrement `comments.upvotes` in the database instead of relying on a client-side read-modify-write counter.
4. **Added** `notify_comment_like_insert()` and `notifications_from_comment_like_insert` to notify a comment author when another user likes the comment. It skips self-notifications and honors the recipient's existing `notif_upvotes` preference.
5. **Restricted** execution of both trigger functions from `PUBLIC`, `anon`, and `authenticated`. Triggers run as their owner and call the previously deployed restricted `create_notification_event(...)` helper.

### Deployment record — 2026-10-02

- Status: **APPLIED TO PRODUCTION; schema/RLS/trigger verification passed.**
- Supabase migration name: `comment_likes_and_notifications`
- Supabase migration version confirmed by migration history: `20261002111134`
- Apply operation returned: success.
- Verification found the three expected policies: own likes SELECT, INSERT, DELETE.
- Verification found the three expected triggers on `public.comment_likes`: count maintenance on INSERT, count maintenance on DELETE, and notification on INSERT.
- No test likes or notifications were inserted during verification.
- No app code was merged into `master`. App changes are isolated on `feature/comment-like-notifications`.

### Remaining validation / limitations

- The Android app has not yet been built locally or tested on devices in this session.
- End-to-end FCM delivery has not yet been verified.
- The client toggle is protected from duplicate rows by the unique constraint, but simultaneous requests can still race; if a duplicate insert is rejected, the UI should refresh/reconcile state.
- Existing historical `comments.upvotes` values are preserved; they are not backfilled into `comment_likes`, since the original per-user identities are unavailable.
- Rollback performed: **No**.


## Migration: enable Realtime for comments

Source: `supabase/migrations/20261002180000_enable_comment_realtime.sql` on `feature/comment-like-notifications`.

### Actions applied to the live database

1. Added `public.comments` to the `supabase_realtime` publication, using an idempotent migration guard.
2. This publication membership is required for the app's new thread subscription to receive comment INSERT/UPDATE/DELETE events. The thread then reloads the canonical comments and post count.
3. Added pull-to-refresh to Thread Detail as a manual fallback.

### Deployment record — 2026-10-02

- Status: **APPLIED TO PRODUCTION; publication membership verified.**
- Supabase migration name: `enable_comment_realtime`
- Supabase migration version: `20261002112333`
- Apply operation returned: success.
- Verification query confirmed `public.comments` is now in `supabase_realtime`.
- No app changes merged into `master`; all app work remains on `feature/comment-like-notifications`.
- Android local build and device testing are still pending.
