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

- **Comment likes are not implemented.** The schema has `comments.upvotes` as a counter, but the authoritative per-user comment-like write path was not established. Do not claim comment-like notifications work until that path is found and a separate migration is reviewed.
- Creating a post does not generate a notification in this migration.
- The upvote-milestone existence check can race if concurrent votes reach the same threshold. A dedicated unique event key/constraint would be a future hardening step.
- Anonymous comment notifications are intentionally suppressed to protect author privacy.
- Legacy app builds that still attempt direct INSERTs into `public.notifications` may receive insert failures because permissive INSERT policies were removed. Roll out the matching app version and check logs before broad release.
- Push delivery still depends on the existing `on_new_notification` trigger, Vault webhook secret, and `send-push-notification` Edge Function. This action verified the database trigger remains present, but did not send a real FCM test.
- No synthetic user activity was inserted during verification; live notification delivery has not yet been end-to-end tested after deployment.

## Rollback / follow-up

- Rollback performed: **No**.
- Next: inspect the comment-like write path, review the milestone race, then perform a controlled end-to-end test with two test accounts. Do not re-enable broad client INSERT policies as a workaround.
