-- Security hardening for permissive content/notification policies.
-- Prepared on nagpurpulse-updates-v5 for review. NOT applied to live Supabase.
--
-- IMPORTANT:
-- 1. Review the deployed policy inventory before applying this migration.
-- 2. This intentionally removes client-side counter updates and direct client
--    notification insertion. Route those operations through narrowly scoped,
--    authenticated RPCs or trusted Edge Functions before production rollout.
-- 3. Existing admin post-edit flows may need a dedicated admin-authorized RPC;
--    do not weaken these policies to preserve a client-side admin check.

begin;

-- POSTS: remove the broad policy that could OR with the owner policy and allow
-- updates to arbitrary rows. Ensure owner updates have both USING and WITH CHECK.
drop policy if exists "System can update post counters" on public.posts;
drop policy if exists "Auth users can update own posts" on public.posts;
drop policy if exists "Users can update own posts" on public.posts;

create policy "Owners can update their own posts"
    on public.posts
    for update
    to authenticated
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

-- COMMENTS: remove the broad counter policy. A user may update only their own
-- comments; vote counters should be changed only by a trusted database function.
drop policy if exists "System can update comment upvotes" on public.comments;
drop policy if exists "Auth users can update own comments" on public.comments;
drop policy if exists "Users can update own comments" on public.comments;

create policy "Owners can update their own comments"
    on public.comments
    for update
    to authenticated
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

-- NOTIFICATIONS: notification rows must be produced by trusted backend flows.
-- Remove unrestricted client insertion and revoke direct INSERT from app roles.
drop policy if exists "System can insert notifications" on public.notifications;
drop policy if exists "System can insert notification rows" on public.notifications;
revoke insert on table public.notifications from public;
revoke insert on table public.notifications from anon;
revoke insert on table public.notifications from authenticated;

-- Keep notification ownership checks explicit for reads and updates.
drop policy if exists "Users can view own notifications" on public.notifications;
create policy "Users can view own notifications"
    on public.notifications
    for select
    to authenticated
    using (auth.uid() = user_id);

drop policy if exists "Users can update own notifications" on public.notifications;
create policy "Users can update own notifications"
    on public.notifications
    for update
    to authenticated
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

-- Explicitly limit client notification deletes to the signed-in user's rows.
drop policy if exists "Users can delete own notifications" on public.notifications;
create policy "Users can delete own notifications"
    on public.notifications
    for delete
    to authenticated
    using (auth.uid() = user_id);

commit;
