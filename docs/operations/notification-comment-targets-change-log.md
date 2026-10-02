# Production Change Log — Comment-Level Notification Destinations

- Project: `eazkmfzegxmdkbowohiy`
- Repository branch: `more-updates-on-nagpur-pulse`
- Migration file: `supabase/migrations/20261002160000_notification_comment_targets.sql`
- Purpose: allow in-app and push notifications caused by comments/replies/mentions/comment-likes to identify the exact comment.
- Change risk: moderate. Adds a nullable column and index, and replaces three notification functions. Existing notification rows remain valid because the new column is nullable.

## Before state (read from production before applying)

1. `public.notifications` had `related_post_id uuid` and `related_conversation_id uuid`, but no `related_comment_id`.
2. `public.create_notification_event(uuid,text,text,text,uuid,uuid,uuid)` inserted notification rows without a comment target.
3. `public.notify_comment_insert()` created comment/reply/mention notifications but passed no comment ID.
4. `public.notify_comment_like_insert()` created comment-like notifications but passed no comment ID.
5. Triggers already existed:
   - `notifications_from_comment_insert` on `public.comments` (AFTER INSERT)
   - `notifications_from_comment_like_insert` on `public.comment_likes` (AFTER INSERT)
   - `on_new_notification` on `public.notifications` (AFTER INSERT), dispatching push notifications.
6. The production `send-push-notification` Edge Function was version 11 and did not select or send a comment ID in FCM data.

## Changes made by the SQL migration

1. Add nullable `public.notifications.related_comment_id uuid`.
2. Add partial index `notifications_related_comment_id_idx` for non-null comment IDs.
3. Replace the old 7-argument `create_notification_event` function with an 8-argument function whose last argument `p_comment_id uuid DEFAULT NULL`. Existing seven-argument callers remain compatible.
4. Update `notify_comment_insert()` to save `NEW.id` for replies, post comments, and mentions.
5. Update `notify_comment_like_insert()` to save `NEW.comment_id`.
6. No notification rows are deleted or rewritten by this migration. Existing rows will have NULL in the new column.

## Changes required in the Edge Function

The branch's `supabase/functions/send-push-notification/index.ts`:
- selects `related_comment_id` from the database;
- includes it in duplicate-echo matching;
- sends `comment_id` in FCM data;
- maps `comment_like` to `notif_upvotes`.

Deploy this function only after the SQL migration succeeds, otherwise its SELECT would reference a column that does not yet exist.

## Verification checklist

- [ ] Migration appears in Supabase migration history.
- [ ] `public.notifications.related_comment_id` exists as nullable UUID.
- [ ] Partial index exists.
- [ ] The 8-argument `create_notification_event` and both updated trigger functions exist.
- [ ] `send-push-notification` Edge Function deployed with `related_comment_id` query and `comment_id` FCM data.
- [ ] Test one reply/comment/mention/comment-like end-to-end on a test account and confirm tapping the notification opens the intended post/comment.
- [ ] Confirm existing message and post notifications still route as before.

## Rollback plan

**Do not run this rollback casually.** It removes the new column and all comment-target values collected after deployment. Existing notification rows and their post/conversation destinations remain; exact comment destinations will be lost. The rollback below restores the production function bodies captured immediately before this migration. If the functions have been changed by another migration since then, reconcile those changes before rolling back.

Run the following SQL only if rollback is needed:

```sql
begin;

-- Restore the original seven-argument event creator.
drop function if exists public.create_notification_event(uuid, text, text, text, uuid, uuid, uuid, uuid);
create function public.create_notification_event(
  p_user_id uuid,
  p_type text,
  p_title text,
  p_body text,
  p_post_id uuid default null,
  p_conversation_id uuid default null,
  p_sender_id uuid default null
)
returns void
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $function$
declare
  sender_name text;
  sender_avatar text;
begin
  if p_user_id is null or p_type is null or p_title is null then
    return;
  end if;
  if p_sender_id is not null and p_sender_id = p_user_id then
    return;
  end if;
  if p_sender_id is not null then
    select username, avatar_url into sender_name, sender_avatar
      from public.profiles where id = p_sender_id;
  end if;
  insert into public.notifications (
    user_id, type, title, body, is_read,
    related_post_id, related_conversation_id,
    sender_username, sender_avatar_url
  )
  values (
    p_user_id, p_type, p_title, p_body, false,
    p_post_id, p_conversation_id, sender_name, sender_avatar
  );
end;
$function$;

-- Restore original comment/reply/mention notification function.
create or replace function public.notify_comment_insert()
returns trigger
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $function$
declare
  post_owner uuid;
  post_title text;
  recipient_id uuid;
  mention_match text[];
  seen_mentions text[] := array[]::text[];
  mentioned_user_id uuid;
begin
  if new.user_id is null or coalesce(new.is_anonymous, false) then
    return new;
  end if;

  select p.user_id, p.title into post_owner, post_title
    from public.posts p where p.id = new.post_id;

  if new.parent_id is not null then
    select c.user_id into recipient_id
      from public.comments c where c.id = new.parent_id;
    if recipient_id is not null
       and recipient_id <> new.user_id
       and coalesce((
         select up.notif_replies from public.user_preferences up
          where up.user_id = recipient_id
       ), true) then
      perform public.create_notification_event(
        recipient_id, 'reply',
        coalesce((select username from public.profiles where id = new.user_id), 'Someone') || ' replied to your comment',
        left(new.body, 120), new.post_id, null, new.user_id
      );
    end if;
  elsif post_owner is not null
        and post_owner <> new.user_id
        and coalesce((
          select up.notif_replies from public.user_preferences up
           where up.user_id = post_owner
        ), true) then
    perform public.create_notification_event(
      post_owner, 'comment',
      coalesce((select username from public.profiles where id = new.user_id), 'Someone') || ' commented on your post',
      left(coalesce(post_title, ''), 120), new.post_id, null, new.user_id
    );
  end if;

  for mention_match in
    select regexp_matches(coalesce(new.body, ''), '@([A-Za-z0-9_]+)', 'g')
  loop
    if lower(mention_match[1]) = any(seen_mentions) then
      continue;
    end if;
    seen_mentions := array_append(seen_mentions, lower(mention_match[1]));
    select p.id into mentioned_user_id
      from public.profiles p
     where lower(p.username) = lower(mention_match[1])
     limit 1;
    if mentioned_user_id is not null
       and mentioned_user_id <> new.user_id
       and coalesce((
         select up.notif_mentions from public.user_preferences up
          where up.user_id = mentioned_user_id
       ), true) then
      perform public.create_notification_event(
        mentioned_user_id, 'mention',
        '@' || coalesce((select username from public.profiles where id = new.user_id), 'Someone') || ' mentioned you',
        left(new.body, 120), new.post_id, null, new.user_id
      );
    end if;
  end loop;
  return new;
end;
$function$;

-- Restore original comment-like notification function.
create or replace function public.notify_comment_like_insert()
returns trigger
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $function$
declare
    comment_owner uuid;
    comment_post_id uuid;
    comment_body text;
    liker_name text;
begin
    select c.user_id, c.post_id, c.body
      into comment_owner, comment_post_id, comment_body
      from public.comments c
     where c.id = new.comment_id;

    if comment_owner is null or comment_owner = new.user_id then
        return new;
    end if;

    if not coalesce((
        select up.notif_upvotes
          from public.user_preferences up
         where up.user_id = comment_owner
    ), true) then
        return new;
    end if;

    select p.username into liker_name
      from public.profiles p
     where p.id = new.user_id;

    perform public.create_notification_event(
        comment_owner,
        'comment_like',
        coalesce(liker_name, 'Someone') || ' liked your comment',
        left(coalesce(comment_body, ''), 120),
        comment_post_id,
        null,
        new.user_id
    );

    return new;
end;
$function$;

drop index if exists public.notifications_related_comment_id_idx;
alter table public.notifications drop column if exists related_comment_id;

commit;
```

## Rollback limitations

- The SQL rollback does not redeploy an older Edge Function. If the new Edge Function is live, roll it back to the prior version or deploy a compatible version that no longer selects `related_comment_id` before dropping the column.
- Supabase migration history should be reconciled with the rollback using the team's normal migration process; do not manually delete migration-history rows without a deliberate decision.
- This document records the observed pre-change state, not a substitute for a fresh backup. Take a current database backup before any rollback.
