-- Push notifications: premium copy, personalisation data, and reliable multi-device tokens.
--
--  1. device_tokens: one row per INSTALLATION (unique fcm_token) instead of one per account.
--     The Android app already upserts with onConflict = 'fcm_token'; without this unique
--     index that upsert fails, so new tokens never save. Supersedes the review-only
--     20261003160000_support_multiple_device_tokens.sql and is safe to run either way.
--  2. register_device_token(): lets a token move between accounts on the same phone
--     (RLS hides the previous owner's row from a plain upsert).
--  3. get_engagement_snapshot(): aggregate numbers for the signed-in user only, used to
--     personalise the scheduled notifications. Returns no other user's data.
--  4. Notification triggers: emoji-free system titles (the app now draws vector icons).

-- ── 1. device_tokens ─────────────────────────────────────────────────────────
-- Keep only the most recently updated row for any duplicated token.
delete from public.device_tokens a
using public.device_tokens b
where a.fcm_token = b.fcm_token
  and (coalesce(a.updated_at, a.created_at, 'epoch'::timestamptz), a.id)
    < (coalesce(b.updated_at, b.created_at, 'epoch'::timestamptz), b.id);

alter table public.device_tokens
    drop constraint if exists device_tokens_user_id_key;

create unique index if not exists device_tokens_fcm_token_uidx
    on public.device_tokens (fcm_token);

create index if not exists device_tokens_user_id_idx
    on public.device_tokens (user_id);

-- ── 2. register_device_token ─────────────────────────────────────────────────
create or replace function public.register_device_token(p_token text)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;
  if p_token is null or length(btrim(p_token)) < 20 or length(p_token) > 4096 then
    raise exception 'Invalid device token' using errcode = '22023';
  end if;

  insert into public.device_tokens (user_id, fcm_token, updated_at)
  values (v_uid, btrim(p_token), now())
  on conflict (fcm_token)
  do update set user_id = excluded.user_id, updated_at = now();
end;
$$;

revoke all on function public.register_device_token(text) from public, anon;
grant execute on function public.register_device_token(text) to authenticated;

-- ── 3. get_engagement_snapshot ───────────────────────────────────────────────
create or replace function public.get_engagement_snapshot(
  p_since timestamptz default (now() - interval '24 hours')
)
returns jsonb
language plpgsql
stable
security definer
set search_path = pg_catalog, public
as $$
declare
  v_uid      uuid := auth.uid();
  v_since    timestamptz := greatest(coalesce(p_since, now() - interval '24 hours'), now() - interval '7 days');
  v_username text;
  v_unread   integer;
  v_up       integer;
  v_cm       integer;
  v_new      integer;
  v_alerts   integer;
  v_top      record;
begin
  if v_uid is null then
    return null;
  end if;

  select username into v_username from public.profiles where id = v_uid;

  select count(*)::integer into v_unread
    from public.notifications n
   where n.user_id = v_uid and coalesce(n.is_read, false) = false;

  select count(*)::integer into v_up
    from public.votes v
    join public.posts p on p.id = v.post_id
   where p.user_id = v_uid
     and v.vote_type = 'up'
     and v.user_id is distinct from v_uid
     and v.created_at >= v_since;

  select count(*)::integer into v_cm
    from public.comments c
    join public.posts p on p.id = c.post_id
   where p.user_id = v_uid
     and c.user_id is distinct from v_uid
     and coalesce(c.is_deleted, false) = false
     and c.created_at >= v_since;

  select count(*)::integer into v_new
    from public.posts p
   where p.created_at >= v_since
     and p.user_id is distinct from v_uid
     and private.is_account_active(p.user_id);

  select count(*)::integer into v_alerts
    from public.posts p
   where coalesce(p.is_alert, false)
     and p.resolved_at is null
     and (p.expires_at is null or p.expires_at > now())
     and private.is_account_active(p.user_id);

  -- Best non-alert post of the last 24 hours (comments weigh more than upvotes).
  select p.id, p.title, coalesce(p.upvotes, 0) as upvotes, coalesce(p.comment_count, 0) as comments
    into v_top
    from public.posts p
   where p.created_at >= now() - interval '24 hours'
     and coalesce(p.is_alert, false) = false
     and private.is_account_active(p.user_id)
   order by (coalesce(p.upvotes, 0) * 2 + coalesce(p.comment_count, 0) * 3) desc, p.created_at desc
   limit 1;

  return jsonb_build_object(
    'username',         v_username,
    'unread_count',     coalesce(v_unread, 0),
    'my_new_upvotes',   coalesce(v_up, 0),
    'my_new_comments',  coalesce(v_cm, 0),
    'new_posts',        coalesce(v_new, 0),
    'active_alerts',    coalesce(v_alerts, 0),
    'top_post_id',      v_top.id,
    'top_post_title',   v_top.title,
    'top_post_upvotes', coalesce(v_top.upvotes, 0),
    'top_post_comments',coalesce(v_top.comments, 0)
  );
end;
$$;

revoke all on function public.get_engagement_snapshot(timestamptz) from public, anon;
grant execute on function public.get_engagement_snapshot(timestamptz) to authenticated;

-- ── 4. Emoji-free system copy ────────────────────────────────────────────────
create or replace function public.notify_post_upvote()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
  owner_id        uuid;
  post_title      text;
  voter_name      text;
  upvote_count    integer;
  milestone_title text;
begin
  if new.vote_type is distinct from 'up' or (tg_op = 'UPDATE' and old.vote_type = 'up') then
    return new;
  end if;
  select p.user_id, p.title into owner_id, post_title
    from public.posts p where p.id = new.post_id;
  if owner_id is null or owner_id = new.user_id then
    return new;
  end if;
  if not coalesce((
    select up.notif_upvotes from public.user_preferences up where up.user_id = owner_id
  ), true) then
    return new;
  end if;
  select username into voter_name from public.profiles where id = new.user_id;
  perform public.create_notification_event(
    owner_id, 'upvote',
    coalesce(voter_name, 'Someone') || ' upvoted your post',
    left(coalesce(post_title, ''), 120), new.post_id, null, new.user_id
  );
  select count(*)::integer into upvote_count
    from public.votes v where v.post_id = new.post_id and v.vote_type = 'up';
  if upvote_count in (10, 50, 100, 500, 1000) then
    milestone_title := 'Your post reached ' || upvote_count || ' upvotes';
    -- Also match the legacy emoji title so already-celebrated milestones never repeat.
    if not exists (
      select 1 from public.notifications n
       where n.user_id = owner_id and n.related_post_id = new.post_id
         and n.type = 'upvote'
         and n.title in (milestone_title, milestone_title || '! ' || chr(127881))
    ) then
      perform public.create_notification_event(
        owner_id, 'upvote', milestone_title,
        left(coalesce(post_title, ''), 120), new.post_id, null, null
      );
    end if;
  end if;
  return new;
end;
$$;

create or replace function public.notify_selected_trending_post()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
  recipient_id uuid;
begin
  if not (coalesce(new.is_pinned, false) or new.post_type = 'trending') then
    return new;
  end if;

  if tg_op = 'UPDATE'
     and (coalesce(old.is_pinned, false) or old.post_type = 'trending') then
    return new;
  end if;

  for recipient_id in
    select p.id
      from public.profiles p
      left join public.user_preferences pref on pref.user_id = p.id
     where p.id <> new.user_id
       and coalesce(pref.notif_push, true)
       and coalesce(pref.notif_trending, true)
  loop
    perform public.create_notification_event(
      recipient_id,
      'trending',
      'Trending now in Nagpur',
      left(coalesce(new.title, 'A post is trending'), 120),
      new.id,
      null,
      new.user_id,
      null
    );
  end loop;

  return new;
end;
$$;

-- Clean the emoji out of milestone rows already in the inbox (does not re-send anything:
-- the push webhook fires on INSERT only).
update public.notifications
   set title = btrim(replace(title, chr(127881), ''))
 where title like '%' || chr(127881) || '%';

update public.notifications
   set title = regexp_replace(title, '!\s*$', '')
 where type = 'upvote' and title like 'Your post reached % upvotes!';
