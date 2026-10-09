# Account 1 — Backend / DB / Security (Supabase)

**Setup once:** connect the Supabase connector to your **staging** project (clone of prod schema). Ask Claude
to use it read-only first (list tables, policies, functions, advisors). Apply SQL only as migration files
that you review and commit; do not let any chat run write statements against production.
**Budget:** ~16 chats. Order: DB-01 → DB-04 first (security), then DB-07, DB-08, rest.
**Prompt pattern:** [START prompt + AI_CONTEXT] + the task prompt below. Attach nothing unless listed — use the connector.

## DB-01 · RLS audit (2 chats)
```
TASK DB-01. Using the Supabase connector (staging), list every table in schema public with RLS status and every
policy. For each table produce: who can SELECT/INSERT/UPDATE/DELETE as anon, authenticated, owner, admin.
Flag any table with RLS off, any "using (true)" write policy, any policy that trusts client-supplied user_id
without auth.uid(), and columns a user could update that should be server-only (is_admin, karma, counts,
is_verified, is_pinned, is_locked, is_read of others). Output: (1) a table of findings ranked
CRITICAL/HIGH/MED, (2) ONE new migration file supabase/migrations/<timestamp>_rls_audit_fixes.sql with fixes.
```
Chat 2: `Now write a SQL test script (two users A and B using set local role / request.jwt.claims) that proves B cannot read/modify A's private rows and cannot escalate privileges. Output as supabase/tests/rls_two_user.sql.`

## DB-02 · Storage (1 chat)
```
TASK DB-02. Review all storage buckets and storage.objects policies (avatars, post images, anything else).
Check: public vs private, path convention enforces owner folder (auth.uid()), file size limit, allowed mime
types, who can delete/overwrite, orphan cleanup when a post/account is deleted. Output a migration with fixes
and a short list of app-side changes needed (file names only).
```

## DB-03 · Auth hardening (1 chat)
```
TASK DB-03. From the Supabase auth settings (connector or my pasted screenshot values) and AuthRepository.kt
(attached), review: email confirmation enforced server-side, password min length/complexity, leaked-password
protection, rate limits for sign-in/OTP/reset, session lifetime, redirect URLs allowlist, Google OAuth config,
JWT expiry, deactivated-account handling. Output: checklist of settings to change in the dashboard (exact
names) + any code patch needed.
```

## DB-04 · Functions & grants (1 chat)
```
TASK DB-04. List all functions in public/private schemas. For each: SECURITY DEFINER or INVOKER, fixed
search_path, who has EXECUTE (anon/authenticated/public), and whether it validates auth.uid(). Flag admin RPCs
callable by non-admins, definer functions without search_path, and functions that accept a user_id parameter.
Output a migration that revokes/locks down, plus a table of intended callers.
```

## DB-05 · Edge functions (1 chat)
Attach: `supabase/functions/send-push/index.ts`, `supabase/functions/send-push-notification/index.ts`.
```
TASK DB-05. Two push functions exist; docs/operations/legacy-send-push-function-review.md describes one as
legacy. Decide which to keep, verify webhook authentication (secret/JWT), input validation, FCM error handling
(invalid tokens cleaned from device_tokens), batching for many tokens, idempotency, and logging without PII.
Output a patch deleting/merging as needed and a deploy checklist (secrets to set, in order).
```

## DB-06 · Abuse protection (2 chats)
```
TASK DB-06. Add server-side protection against spam and abuse: CHECK constraints (title/body/username length,
allowed categories), triggers or RPC wrappers that rate-limit posts/comments/messages/reports per user
(e.g. N per minute/hour), block posting when account is deactivated or banned, and sanitize usernames. Keep the
current client working: list any client changes required. Output one migration + a list of client error codes.
```

## DB-07 · Counters, karma, unread count (2 chats) — findings F1, F2
Attach: `PostRepository.kt` (addComment, updateKarma, voting parts), `NotificationRepository.kt`.
```
TASK DB-07. Move comment_count, upvote/downvote totals and karma to database triggers so the client cannot
forge them and races disappear. Replace the client-side increments in PostRepository (addComment etc.) and make
getUnreadCount use a count-only query/RPC instead of decoding all rows. Backfill existing counts. Output:
migration (triggers + backfill) and a Kotlin patch removing client-side increments.
```

## DB-08 · Moderation backend (2 chats)
Attach: admin/*ViewModel and the repo methods for reports if any.
```
TASK DB-08. Check if tables exist for reports, blocks (user blocks user) and admin action logs. If missing,
design them: columns, RLS, RPCs (report_post, report_comment, report_user, block_user, unblock_user,
admin_hide_content, admin_ban_user) and make blocked users' content hidden in feeds/comments/messages via
policy or view. Add an admin audit log written by the RPCs. Output migration + Kotlin repository patch.
Google Play UGC policy requires report + block + a way to act on reports.
```

## DB-09 · Indexes & performance (1 chat)
```
TASK DB-09. Run the performance advisors via the connector and inspect slow/seq-scan queries for the feed,
comments by post, notifications by user, messages by conversation. Propose indexes (names, columns, partial
where useful), remove unused/duplicate indexes, add missing FK indexes. Output a migration; estimate the impact.
```

## DB-10 · Deletion, export, retention (1 chat)
```
TASK DB-10. Audit delete_user_account and deactivation: does it remove profile, posts/comments (or anonymize),
messages, votes, saved posts, notifications, device tokens, storage files, consents? What stays and why?
Add data export RPC (JSON of the user's own data) and retention rules for notification_analytics and old
notifications (e.g. purge > 90 days). Output migration(s) and a user-facing explanation paragraph for the policy.
```

## DB-11 · Environments & backups (1 chat)
```
TASK DB-11. Write docs/operations/environments.md: staging vs production Supabase projects, how migrations
are promoted (CLI commands), secrets list per environment (names only), backup/PITR settings to enable, how to
restore, monitoring/alerts to set (errors, auth failures, DB CPU, storage), and a rollback procedure.
```

## DB-12 · Notification server logic (1 chat)
Attach: latest notification migrations, `docs/database-actions/2026-10-02-server-authoritative-notifications.md`.
```
TASK DB-12. The new Notifications screen can render type 'milestone' (e.g. post reached 10/50/100/500
upvotes, first comment) and later a posting streak. Implement milestone notifications as a trigger on votes/
comments that inserts a notification once per threshold per post (idempotent), respecting notif prefs.
Design (do not build yet) a streak table. Output migration + a note of exact title/body strings used.
```

### Typical load for this account
Security block (DB-01…DB-04, DB-10): 6 chats · Data integrity (DB-07, DB-08, DB-06): 6 chats · Rest: 4 chats.
