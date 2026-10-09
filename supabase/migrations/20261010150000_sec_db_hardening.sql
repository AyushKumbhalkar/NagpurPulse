-- DB-01 / master audit: SEC-001, DB-002, DB-004, DB-005, DB-008.
-- Apply to a Supabase branch/staging first. App changes in the same patch MUST ship with it
-- (reads move from public.posts to public.posts_public; alerts realtime moves to alert_events).
-- DB-003 (profiles PII) is intentionally NOT here: profiles are read with select=* by guests,
-- so column revokes need a profiles_public view + app change first.

-- ── SEC-001a: comments. The app already reads through masking RPCs; two legacy "true" SELECT
-- policies (not dropped by the comments overhaul, different names) still expose comments.user_id.
drop policy if exists "Comments are viewable by everyone" on public.comments;
drop policy if exists "Public comments" on public.comments;
-- Remaining SELECT paths: owner, admin, restrictive active-account policy, masking RPCs.

-- ── SEC-001b: posts. Mask user_id of anonymous posts.
-- NOTE: the audit's security_invoker view cannot work once user_id is revoked (invoker needs the
-- column privilege). This view runs as its owner and re-applies the only row rule that matters
-- ("Active accounts only - post visibility"). user_id is text: '' when hidden, so the Kotlin
-- Post model (non-null String) and eq("user_id", ...) filters keep working without leaking.
create or replace view public.posts_public with (security_invoker = false) as
select
  p.id,
  case when not coalesce(p.is_anonymous, false)
         or p.user_id = auth.uid()
         or coalesce(public.is_admin(auth.uid()), false)
       then p.user_id::text else '' end as user_id,
  p.title, p.body, p.category, p.area_tag, p.is_anonymous,
  p.upvotes, p.downvotes, p.comment_count, p.view_count, p.image_url,
  p.is_alert, p.alert_severity, p.created_at, p.is_pinned, p.is_locked,
  p.post_type, p.edited_at, p.edited_by_admin, p.expires_at, p.resolved_at, p.confirm_count
from public.posts p
where private.is_account_active(p.user_id);

revoke all on public.posts_public from public, anon, authenticated;
grant select on public.posts_public to anon, authenticated;

-- A column revoke is a no-op while a table-level SELECT grant exists, so replace it.
revoke select on public.posts from anon, authenticated;
grant select (id, title, body, category, area_tag, is_anonymous, upvotes, downvotes, comment_count,
              view_count, image_url, is_alert, alert_severity, created_at, is_pinned, is_locked,
              post_type, edited_at, edited_by_admin, expires_at, resolved_at, confirm_count)
  on public.posts to anon, authenticated;
-- Rollback: grant select on public.posts to anon, authenticated;

-- ── SEC-001c: realtime on posts broadcasts full rows (incl. user_id). Replace with a content-free
-- event table, same pattern as comment_events.
create table if not exists public.alert_events (
  id         bigint generated always as identity primary key,
  post_id    uuid not null references public.posts(id) on delete cascade,
  created_at timestamptz not null default now()
);
alter table public.alert_events enable row level security;
drop policy if exists "Anyone can read alert events" on public.alert_events;
create policy "Anyone can read alert events" on public.alert_events for select to anon, authenticated using (true);
revoke all on public.alert_events from public, anon, authenticated;
grant select on public.alert_events to anon, authenticated;

create or replace function private.emit_alert_event()
returns trigger language plpgsql security definer set search_path = pg_catalog, public as $$
begin
  insert into public.alert_events(post_id) values (new.id);
  return null;
end $$;
revoke all on function private.emit_alert_event() from public, anon, authenticated;
drop trigger if exists trg_emit_alert_event on public.posts;
create trigger trg_emit_alert_event after insert on public.posts
  for each row when (new.is_alert) execute function private.emit_alert_event();

do $$ begin
  if exists (select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename='posts') then
    alter publication supabase_realtime drop table public.posts;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename='alert_events') then
    alter publication supabase_realtime add table public.alert_events;
  end if;
end $$;

-- ── DB-002: owners could write counters / moderation flags on their own posts.
create or replace function public.protect_post_privileged_columns()
returns trigger language plpgsql security invoker set search_path = pg_catalog, public as $$
begin
  if current_user in ('authenticated', 'anon')
     and not coalesce(public.is_admin(auth.uid()), false) then
    if new.upvotes         is distinct from old.upvotes
    or new.downvotes       is distinct from old.downvotes
    or new.comment_count   is distinct from old.comment_count
    or new.is_pinned       is distinct from old.is_pinned
    or new.is_locked       is distinct from old.is_locked
    or new.created_at      is distinct from old.created_at
    or coalesce(new.edited_by_admin, false) is distinct from coalesce(old.edited_by_admin, false)
    or new.user_id         is distinct from old.user_id then
      raise exception 'protected post columns' using errcode = '42501';
    end if;
  end if;
  return new;
end $$;
drop trigger if exists trg_protect_post_privileged on public.posts;
create trigger trg_protect_post_privileged before update on public.posts
  for each row execute function public.protect_post_privileged_columns();
-- Counter writers (vote_post_atomic, comments_sync_counts, confirm/resolve_alert) are SECURITY
-- DEFINER, so current_user is the owner role and they pass.
-- Rollback: drop trigger trg_protect_post_privileged on public.posts;

-- ── DB-004: least privilege (RLS stays the gate; these grants were never needed).
revoke truncate, references, trigger on all tables in schema public from anon, authenticated;
revoke insert, update, delete on all tables in schema public from anon;
alter default privileges in schema public revoke truncate, references, trigger on tables from anon, authenticated;
alter default privileges in schema public revoke insert, update, delete on tables from anon;

-- ── DB-008: mutable search_path.
alter function public.set_alert_expiry()            set search_path = pg_catalog, public;
alter function public.protect_alert_confirm_count() set search_path = pg_catalog, public;

-- ── DB-005: self-votes no longer move karma (body = 20261003120000 version + one guard).
CREATE OR REPLACE FUNCTION public.vote_post_atomic(
  p_post_id uuid,
  p_vote_type text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, auth
AS $$
DECLARE
  v_user_id uuid := auth.uid();
  v_owner_id uuid;
  v_old_vote text;
  v_has_vote boolean := false;
  v_up_delta integer := 0;
  v_down_delta integer := 0;
  v_karma_delta integer := 0;
  v_current_up integer;
  v_current_down integer;
BEGIN
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'Authentication required to vote' USING ERRCODE = '28000';
  END IF;
  IF p_vote_type NOT IN ('up', 'down') THEN
    RAISE EXCEPTION 'Invalid vote type: %', p_vote_type USING ERRCODE = '22023';
  END IF;

  -- Serialize votes on the same post so counters cannot be overwritten by stale reads.
  SELECT p.user_id, COALESCE(p.upvotes, 0), COALESCE(p.downvotes, 0)
    INTO v_owner_id, v_current_up, v_current_down
    FROM public.posts p
   WHERE p.id = p_post_id
   FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Post not found' USING ERRCODE = 'P0002';
  END IF;

  SELECT v.vote_type INTO v_old_vote
    FROM public.votes v
   WHERE v.user_id = v_user_id AND v.post_id = p_post_id
   FOR UPDATE;
  v_has_vote := FOUND;

  IF NOT v_has_vote THEN
    INSERT INTO public.votes(user_id, post_id, vote_type)
    VALUES (v_user_id, p_post_id, p_vote_type);
    v_up_delta := CASE WHEN p_vote_type = 'up' THEN 1 ELSE 0 END;
    v_down_delta := CASE WHEN p_vote_type = 'down' THEN 1 ELSE 0 END;
    v_karma_delta := CASE WHEN p_vote_type = 'up' THEN 1 ELSE -1 END;
  ELSIF v_old_vote = p_vote_type THEN
    DELETE FROM public.votes WHERE user_id = v_user_id AND post_id = p_post_id;
    v_up_delta := CASE WHEN v_old_vote = 'up' THEN -1 ELSE 0 END;
    v_down_delta := CASE WHEN v_old_vote = 'down' THEN -1 ELSE 0 END;
    v_karma_delta := CASE WHEN v_old_vote = 'up' THEN -1 ELSE 1 END;
  ELSE
    UPDATE public.votes SET vote_type = p_vote_type
     WHERE user_id = v_user_id AND post_id = p_post_id;
    v_up_delta := (CASE WHEN p_vote_type = 'up' THEN 1 ELSE 0 END)
                - (CASE WHEN v_old_vote = 'up' THEN 1 ELSE 0 END);
    v_down_delta := (CASE WHEN p_vote_type = 'down' THEN 1 ELSE 0 END)
                  - (CASE WHEN v_old_vote = 'down' THEN 1 ELSE 0 END);
    v_karma_delta := (CASE WHEN p_vote_type = 'up' THEN 1 ELSE -1 END)
                   - (CASE WHEN v_old_vote = 'up' THEN 1 ELSE -1 END);
  END IF;

  UPDATE public.posts
     SET upvotes = GREATEST(0, v_current_up + v_up_delta),
         downvotes = GREATEST(0, v_current_down + v_down_delta)
   WHERE id = p_post_id;

  -- DB-005: no karma from voting on your own post.
  IF v_owner_id = v_user_id THEN v_karma_delta := 0; END IF;

  IF v_owner_id IS NOT NULL AND v_karma_delta <> 0 THEN
    UPDATE public.profiles
       SET karma = GREATEST(0, COALESCE(karma, 0) + v_karma_delta)
     WHERE id = v_owner_id;
  END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.vote_post_atomic(uuid, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.vote_post_atomic(uuid, text) TO authenticated, service_role;
