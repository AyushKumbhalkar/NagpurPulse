-- Add comment-level notification destinations without breaking existing rows.
alter table public.notifications
    add column if not exists related_comment_id uuid;

create index if not exists notifications_related_comment_id_idx
    on public.notifications (related_comment_id)
    where related_comment_id is not null;

-- Replace the legacy seven-argument function so existing callers can continue
-- using seven arguments while comment-aware callers can provide p_comment_id.
drop function if exists public.create_notification_event(uuid, text, text, text, uuid, uuid, uuid);

create function public.create_notification_event(
    p_user_id uuid,
    p_type text,
    p_title text,
    p_body text,
    p_post_id uuid default null,
    p_conversation_id uuid default null,
    p_sender_id uuid default null,
    p_comment_id uuid default null
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
    related_post_id, related_comment_id, related_conversation_id,
    sender_username, sender_avatar_url
  )
  values (
    p_user_id, p_type, p_title, p_body, false,
    p_post_id, p_comment_id, p_conversation_id, sender_name, sender_avatar
  );
end;
$function$;

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
        left(new.body, 120), new.post_id, null, new.user_id, new.id
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
      left(coalesce(post_title, ''), 120), new.post_id, null, new.user_id, new.id
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
        left(new.body, 120), new.post_id, null, new.user_id, new.id
      );
    end if;
  end loop;
  return new;
end;
$function$;

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

    select username into liker_name
      from public.profiles p
     where p.id = new.user_id;

    perform public.create_notification_event(
        comment_owner,
        'comment_like',
        coalesce(liker_name, 'Someone') || ' liked your comment',
        left(coalesce(comment_body, ''), 120),
        comment_post_id,
        null,
        new.user_id,
        new.comment_id
    );

    return new;
end;
$function$;
