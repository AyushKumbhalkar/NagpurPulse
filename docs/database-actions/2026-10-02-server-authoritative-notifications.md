# Database Actions Log — NagpurPulse Notifications

This branch records database changes made for the server-authoritative notification system. It is intentionally separate from `master`.

- Repository: `AyushKumbhalkar/NagpurPulse`
- Database project ref: `eazkmfzegxmdkbowohiy`
- Related app branch: `feature/reddit-notifications-v2`
- Related migration file: `supabase/migrations/20261002150000_server_authoritative_notifications.sql`
- Status: **Deployment log started; update this file immediately after each live database action.**

## Safety / scope

- Changes in this log target the live Supabase project only when explicitly marked **APPLIED TO PRODUCTION**.
- Never record service-role keys, webhook secrets, access tokens, or user private data here.
- Do not merge this branch into `master` as a substitute for applying migrations.
- A migration is not considered deployed until Supabase confirms it and the migration list / schema are checked afterward.
- Rollback must be a separately reviewed migration; do not blindly re-enable client-created notifications because that permits clients to forge notifications.

## Planned migration: server-authoritative notification events

Source: `supabase/migrations/20261002150000_server_authoritative_notifications.sql`

Intended additions:
1. Add `public.create_notification_event(...)`, a restricted `SECURITY DEFINER` helper that inserts notifications and resolves sender username/avatar server-side.
2. Add `public.notify_comment_insert()` and an AFTER INSERT trigger on `public.comments`:
   - Notify a post owner when another non-anonymous user comments.
   - Notify the parent comment author when another non-anonymous user replies.
   - Resolve @username mentions to profile IDs and honor the corresponding notification preferences.
   - Anonymous comments are skipped to avoid exposing the author through notification metadata.
3. Add `public.notify_post_upvote()` and INSERT / vote-type UPDATE triggers on `public.votes`:
   - Notify the post owner of another user's upvote.
   - Check actual vote rows for post-upvote milestones (10, 50, 100, 500, 1,000).
   - Honor the recipient's upvote-notification preference.
4. Add `public.notify_message_insert()` and an AFTER INSERT trigger on `public.messages`:
   - Notify the other conversation participant only when the sender is a participant.
   - Honor the recipient's message-notification preference.
5. Revoke direct execution of trigger/helper functions from `PUBLIC`, `anon`, and `authenticated`.
6. Drop permissive `INSERT` and `ALL` RLS policies on `public.notifications`; retain the separate recipient-scoped SELECT and UPDATE policies.

## Known limitations / things not included

- Comment likes are **not implemented** by this migration. The current schema review found `comments.upvotes` as a counter but did not establish the authoritative per-user comment-like event source. Do not claim comment-like notifications work until the write path is identified and separately implemented.
- Creating a post does not generate a notification in this migration.
- The milestone check can race if multiple votes arrive at the exact threshold concurrently; consider a dedicated milestone/event uniqueness constraint in a future migration.
- Anonymous comment notifications are intentionally suppressed; revisit only if the product can preserve anonymity in all notification surfaces.
- Notification push delivery still depends on the existing `on_new_notification` trigger and `send-push-notification` Edge Function configuration.

## Deployment record

### 2026-10-02 — initial review

- Confirmed the production project has the existing `on_new_notification` push-dispatch trigger.
- Before this migration, production had no event notification triggers on `comments`, `votes`, or `messages`.
- The new migration was present on `feature/reddit-notifications-v2`; it had not yet been applied to production at the time this log was started.
- The previous broad notification INSERT policies included permissive policies that allow authenticated clients to create notification rows. Removing them is a security hardening step, but legacy app builds that still attempt direct notification inserts may receive insert failures until the app changes are rolled out.

### Live deployment result

- Status: **PENDING**
- Migration name: `server_authoritative_notifications`
- Applied at: pending
- Supabase migration confirmation: pending
- Post-deployment trigger / policy verification: pending
- Rollback performed: no
