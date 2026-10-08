-- ════════════════════════════════════════════════════════════════════════════
-- Comments overhaul
--  1. Anonymous comments stay anonymous: raw `comments` rows are no longer
--     readable by everyone (they contain user_id). The app reads comments
--     through SECURITY DEFINER RPCs that mask the author of anonymous rows.
--  2. Anonymous aliases are generated on the server, stable per user per post.
--  3. comment_count and karma are maintained by triggers.
--  4. Admins can edit any comment SILENTLY: edited_at is not touched, so no
--     "edited" label is shown. Owner edits still show "Edited". Admin edits are
--     written to admin_actions.
--  5. Atomic like toggle, deduplicated reports, author delete, rate limiting,
--     length limit, parent/post integrity, indexes, schema alignment.
-- NOTE: older app builds read `comments` directly and will only see the
-- signed-in user's own comments after this migration. Ship it with the app release.
-- ════════════════════════════════════════════════════════════════════════════

create schema if not exists private;

-- 0. Schema alignment (these columns existed only in production)
alter table public.comments
    add column if not exists is_deleted        boolean     not null default false,
    add column if not exists deleted_by_author boolean     not null default false,
    add column if not exists edited_at         timestamptz,
    add column if not exists edited_by_admin   boolean     not null default false,
    add column if not exists anon_alias        text;

-- 1. Integrity + indexes
alter table public.comments drop constraint if exists comments_body_length_chk;
alter table public.comments
    add constraint comments_body_length_chk
    check (char_length(body) between 1 and 2000) not valid;

create index if not exists comments_post_parent_created_idx on public.comments (post_id, parent_id, created_at);
create index if not exists comments_post_top_idx on public.comments (post_id, upvotes desc, created_at) where parent_id is null;
create index if not exists comments_parent_id_idx on public.comments (parent_id);
create index if not exists comments_user_created_idx on public.comments (user_id, created_at desc);

delete from public.comment_reports a using public.comment_reports b
 where a.comment_id = b.comment_id and a.reported_by = b.reported_by and a.ctid > b.ctid;
create unique index if not exists comment_reports_unique_reporter_idx
    on public.comment_reports (comment_id, reported_by);

-- 2. Server-side anonymous aliases
create table if not exists private.anon_aliases (
    post_id    uuid not null references public.posts(id)    on delete cascade,
    user_id    uuid not null references public.profiles(id) on delete cascade,
    alias      text not null,
    created_at timestamptz not null default now(),
    primary key (post_id, user_id),
    unique (post_id, alias)
);
alter table private.anon_aliases enable row level security;
revoke all on private.anon_aliases from public, anon, authenticated;

create or replace function private.get_anon_alias(p_post_id uuid, p_user_id uuid)
returns text language plpgsql security definer set search_path = pg_catalog, public
as $$
declare
    v_alias text; v_attempts integer := 0;
    v_adjectives text[] := array['Brave','Swift','Silent','Wild','Calm','Mystic','Clever','Bold',
        'Fierce','Gentle','Sneaky','Happy','Dark','Cosmic','Chill','Sharp','Fuzzy','Lucky','Quirky',
        'Sunny','Shadow','Nimble','Ancient','Cozy'];
    v_animals text[] := array['Tiger','Wolf','Eagle','Fox','Bear','Panda','Hawk','Lion','Owl','Deer',
        'Lynx','Crow','Seal','Raven','Mink','Viper','Cobra','Gecko','Otter','Bison','Moose','Crane',
        'Finch','Manta','Dingo','Hyena','Lemur','Tapir','Quail','Ibis'];
begin
    select a.alias into v_alias from private.anon_aliases a
     where a.post_id = p_post_id and a.user_id = p_user_id;
    if v_alias is not null then return v_alias; end if;

    -- keep the identity people already saw in threads created before this migration
    select c.anon_alias into v_alias from public.comments c
     where c.post_id = p_post_id and c.user_id = p_user_id and c.is_anonymous and c.anon_alias is not null
     order by c.created_at limit 1;
    if v_alias is not null then
        insert into private.anon_aliases (post_id, user_id, alias) values (p_post_id, p_user_id, v_alias)
        on conflict do nothing;
        select a.alias into v_alias from private.anon_aliases a
         where a.post_id = p_post_id and a.user_id = p_user_id;
        if v_alias is not null then return v_alias; end if;
    end if;

    loop
        v_attempts := v_attempts + 1;
        if v_attempts > 20 then
            v_alias := 'Anon_' || substr(md5(random()::text || clock_timestamp()::text), 1, 10);
        else
            v_alias := 'Anon_' || v_adjectives[1 + floor(random() * array_length(v_adjectives, 1))::integer]
                || '_' || v_animals[1 + floor(random() * array_length(v_animals, 1))::integer]
                || '_' || (1 + floor(random() * 99))::integer;
        end if;
        begin
            insert into private.anon_aliases (post_id, user_id, alias) values (p_post_id, p_user_id, v_alias);
            return v_alias;
        exception when unique_violation then
            select a.alias into v_alias from private.anon_aliases a
             where a.post_id = p_post_id and a.user_id = p_user_id;
            if v_alias is not null then return v_alias; end if;
        end;
    end loop;
end;
$$;
revoke all on function private.get_anon_alias(uuid, uuid) from public, anon, authenticated;

-- 3. Insert guard: never trust client-controlled columns
create or replace function private.comments_before_insert()
returns trigger language plpgsql security definer set search_path = pg_catalog, public
as $$
declare v_parent_post uuid; v_parent_deleted boolean;
begin
    new.body := btrim(new.body);
    new.is_anonymous := coalesce(new.is_anonymous, false);
    if new.is_anonymous then new.anon_alias := private.get_anon_alias(new.post_id, new.user_id);
    else new.anon_alias := null; end if;
    new.upvotes := 0; new.is_deleted := false; new.deleted_by_author := false;
    new.edited_at := null; new.edited_by_admin := false;

    if new.parent_id is not null then
        select c.post_id, coalesce(c.is_deleted, false) into v_parent_post, v_parent_deleted
          from public.comments c where c.id = new.parent_id;
        if v_parent_post is null or v_parent_post <> new.post_id then
            raise exception 'Invalid parent comment' using errcode = '22023';
        end if;
        if v_parent_deleted then
            raise exception 'You cannot reply to a removed comment' using errcode = '22023';
        end if;
    end if;
    return new;
end;
$$;
revoke all on function private.comments_before_insert() from public, anon, authenticated;
drop trigger if exists comments_before_insert on public.comments;
create trigger comments_before_insert before insert on public.comments
for each row execute function private.comments_before_insert();

-- 4. Counters maintained by the database
create or replace function private.comments_sync_counts()
returns trigger language plpgsql security definer set search_path = pg_catalog, public
as $$
begin
    if tg_op = 'INSERT' then
        if not coalesce(new.is_deleted, false) then
            update public.posts set comment_count = coalesce(comment_count, 0) + 1 where id = new.post_id;
            update public.profiles set karma = coalesce(karma, 0) + 1 where id = new.user_id;
        end if;
    elsif tg_op = 'DELETE' then
        if not coalesce(old.is_deleted, false) then
            update public.posts set comment_count = greatest(coalesce(comment_count, 0) - 1, 0) where id = old.post_id;
        end if;
    elsif tg_op = 'UPDATE' then
        if coalesce(old.is_deleted, false) <> coalesce(new.is_deleted, false) then
            update public.posts set comment_count = greatest(coalesce(comment_count, 0)
                + case when coalesce(new.is_deleted, false) then -1 else 1 end, 0)
             where id = new.post_id;
        end if;
    end if;
    return null;
end;
$$;
revoke all on function private.comments_sync_counts() from public, anon, authenticated;
drop trigger if exists comments_sync_counts_ins on public.comments;
create trigger comments_sync_counts_ins after insert on public.comments for each row execute function private.comments_sync_counts();
drop trigger if exists comments_sync_counts_del on public.comments;
create trigger comments_sync_counts_del after delete on public.comments for each row execute function private.comments_sync_counts();
drop trigger if exists comments_sync_counts_upd on public.comments;
create trigger comments_sync_counts_upd after update of is_deleted on public.comments for each row execute function private.comments_sync_counts();

update public.posts p set comment_count = coalesce((
    select count(*) from public.comments c where c.post_id = p.id and not coalesce(c.is_deleted, false)), 0);

-- 5. Realtime without leaking rows: threads listen to a tiny public events table
create table if not exists public.comment_events (
    id bigint generated always as identity primary key,
    post_id uuid not null,
    kind text not null,
    created_at timestamptz not null default now()
);
create index if not exists comment_events_post_created_idx on public.comment_events (post_id, created_at desc);
alter table public.comment_events enable row level security;
drop policy if exists "Anyone can read comment events" on public.comment_events;
create policy "Anyone can read comment events" on public.comment_events for select to anon, authenticated using (true);
revoke all on public.comment_events from public, anon, authenticated;
grant select on public.comment_events to anon, authenticated;

create or replace function private.comments_emit_event()
returns trigger language plpgsql security definer set search_path = pg_catalog, public
as $$
declare v_post uuid := case when tg_op = 'DELETE' then old.post_id else new.post_id end;
begin
    insert into public.comment_events (post_id, kind) values (v_post, lower(tg_op));
    delete from public.comment_events where post_id = v_post and created_at < now() - interval '15 minutes';
    return null;
end;
$$;
revoke all on function private.comments_emit_event() from public, anon, authenticated;
drop trigger if exists comments_emit_event_ins on public.comments;
create trigger comments_emit_event_ins after insert on public.comments for each row execute function private.comments_emit_event();
drop trigger if exists comments_emit_event_del on public.comments;
create trigger comments_emit_event_del after delete on public.comments for each row execute function private.comments_emit_event();
-- likes only touch upvotes, so they no longer wake every viewer
drop trigger if exists comments_emit_event_upd on public.comments;
create trigger comments_emit_event_upd after update of body, is_deleted on public.comments for each row execute function private.comments_emit_event();

do $$
begin
    if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime'
                    and schemaname = 'public' and tablename = 'comment_events') then
        alter publication supabase_realtime add table public.comment_events;
    end if;
    if exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime'
                and schemaname = 'public' and tablename = 'comments') then
        alter publication supabase_realtime drop table public.comments;
    end if;
end;
$$;

-- 6. RLS: raw rows readable by owner + admins only; writes go through RPCs
drop policy if exists "Comments viewable by everyone"       on public.comments;
drop policy if exists "Owners can read their own comments"  on public.comments;
drop policy if exists "Admins can read all comments"        on public.comments;
create policy "Owners can read their own comments" on public.comments for select to authenticated using (auth.uid() = user_id);
create policy "Admins can read all comments" on public.comments for select to authenticated using (public.is_admin(auth.uid()));
drop policy if exists "Owners can update their own comments" on public.comments;
drop policy if exists "Admins can update comments"           on public.comments;

-- 7. Projection used by every read RPC
drop function if exists public.get_thread_comments(uuid, text, integer, integer);
drop function if exists public.get_user_comments(uuid, integer, integer);
drop function if exists public.get_user_comment_count(uuid);
drop function if exists public.add_comment(uuid, text, boolean, uuid);
drop function if exists public.edit_comment(uuid, text);
drop function if exists public.delete_comment(uuid);
drop function if exists public.toggle_comment_like(uuid);
drop function if exists public.report_comment(uuid, text);
drop function if exists public.admin_remove_comment(uuid);
drop function if exists private.thread_rows(uuid[], uuid, bigint);
drop type if exists public.thread_comment;

create type public.thread_comment as (
    id uuid, post_id uuid, parent_id uuid,
    user_id uuid,            -- NULL for anonymous comments
    body text,               -- '' for removed comments
    is_deleted boolean, deleted_by_author boolean, upvotes integer,
    is_anonymous boolean, anon_alias text, created_at timestamptz,
    edited_at timestamptz,   -- only set by the author's own edits
    username text, avatar_url text,   -- NULL for anonymous comments
    is_post_author boolean,  -- never true for anonymous comments
    is_mine boolean, liked_by_me boolean, total_roots bigint
);

create or replace function private.thread_rows(p_ids uuid[], p_viewer uuid, p_total_roots bigint default 0)
returns setof public.thread_comment language sql stable security definer set search_path = pg_catalog, public
as $$
    select
        c.id, c.post_id, c.parent_id,
        case when coalesce(c.is_anonymous, false) then null else c.user_id end,
        (case when coalesce(c.is_deleted, false) then '' else c.body end)::text,
        coalesce(c.is_deleted, false),
        coalesce(c.deleted_by_author, false),
        coalesce(c.upvotes, 0)::integer,
        coalesce(c.is_anonymous, false),
        (case when coalesce(c.is_anonymous, false) then c.anon_alias else null end)::text,
        c.created_at,
        c.edited_at,
        (case when coalesce(c.is_anonymous, false) then null else pr.username end)::text,
        (case when coalesce(c.is_anonymous, false) then null else pr.avatar_url end)::text,
        (not coalesce(c.is_anonymous, false) and c.user_id = p.user_id),
        (p_viewer is not null and c.user_id = p_viewer),
        (p_viewer is not null and exists (select 1 from public.comment_likes l
             where l.comment_id = c.id and l.user_id = p_viewer)),
        p_total_roots::bigint
    from public.comments c
    join public.posts p on p.id = c.post_id
    left join public.profiles pr on pr.id = c.user_id
    where c.id = any (p_ids)
$$;
revoke all on function private.thread_rows(uuid[], uuid, bigint) from public, anon, authenticated;

-- 8. Read RPCs. Top-level comments are paginated; every descendant is included.
create or replace function public.get_thread_comments(
    p_post_id uuid, p_sort text default 'top', p_limit integer default 20, p_offset integer default 0)
returns setof public.thread_comment language plpgsql stable security definer set search_path = pg_catalog, public
as $$
declare v_viewer uuid := auth.uid(); v_total bigint; v_root_ids uuid[];
begin
    select count(*) into v_total from public.comments c
     where c.post_id = p_post_id and c.parent_id is null and private.is_account_active(c.user_id);

    select array_agg(s.id) into v_root_ids from (
        select c.id from public.comments c
         where c.post_id = p_post_id and c.parent_id is null and private.is_account_active(c.user_id)
         order by
            (case when p_sort = 'top' then coalesce(c.upvotes, 0) end) desc nulls last,
            (case when p_sort = 'old' then extract(epoch from c.created_at) end) asc nulls last,
            (case when p_sort = 'new' then extract(epoch from c.created_at) end) desc nulls last,
            c.created_at asc, c.id
         limit greatest(least(coalesce(p_limit, 20), 50), 1)
        offset greatest(coalesce(p_offset, 0), 0)
    ) s;

    return query
        with recursive tree as (
            select c.id from public.comments c where c.id = any (v_root_ids)
            union
            select ch.id from public.comments ch join tree t on ch.parent_id = t.id
             where private.is_account_active(ch.user_id)
        )
        select * from private.thread_rows((select array_agg(id) from tree), v_viewer, v_total);
end;
$$;

-- Anonymous comments are only ever visible to their own author on profiles.
create or replace function public.get_user_comments(p_user_id uuid, p_limit integer default 50, p_offset integer default 0)
returns setof public.thread_comment language plpgsql stable security definer set search_path = pg_catalog, public
as $$
declare v_viewer uuid := auth.uid(); v_ids uuid[];
begin
    if p_user_id is distinct from v_viewer then
        if not exists (select 1 from public.profiles p where p.id = p_user_id
               and not coalesce(p.hide_profile, false) and not coalesce(p.hide_comments, false)
               and not coalesce(p.is_deactivated, false)) then
            return;
        end if;
    end if;
    select array_agg(s.id) into v_ids from (
        select c.id from public.comments c
         where c.user_id = p_user_id
           and (p_user_id is not distinct from v_viewer or not coalesce(c.is_anonymous, false))
           and not coalesce(c.is_deleted, false) and private.is_account_active(c.user_id)
         order by c.created_at desc
         limit greatest(least(coalesce(p_limit, 50), 100), 1)
        offset greatest(coalesce(p_offset, 0), 0)
    ) s;
    return query select * from private.thread_rows(v_ids, v_viewer, 0) order by created_at desc;
end;
$$;

create or replace function public.get_user_comment_count(p_user_id uuid)
returns bigint language plpgsql stable security definer set search_path = pg_catalog, public
as $$
declare v_viewer uuid := auth.uid(); v_count bigint;
begin
    if p_user_id is distinct from v_viewer then
        if not exists (select 1 from public.profiles p where p.id = p_user_id
               and not coalesce(p.hide_profile, false) and not coalesce(p.hide_comments, false)
               and not coalesce(p.is_deactivated, false)) then
            return 0;
        end if;
    end if;
    select count(*) into v_count from public.comments c
     where c.user_id = p_user_id
       and (p_user_id is not distinct from v_viewer or not coalesce(c.is_anonymous, false))
       and not coalesce(c.is_deleted, false);
    return v_count;
end;
$$;

-- 9. Write RPCs
create or replace function public.add_comment(
    p_post_id uuid, p_body text, p_is_anonymous boolean default false, p_parent_id uuid default null)
returns setof public.thread_comment language plpgsql security definer set search_path = pg_catalog, public
as $$
declare v_uid uuid := auth.uid(); v_body text := btrim(coalesce(p_body, '')); v_id uuid;
begin
    if v_uid is null then raise exception 'You must be signed in to comment' using errcode = '28000'; end if;
    if not private.is_account_active(v_uid) then raise exception 'Your account is deactivated' using errcode = '42501'; end if;
    if char_length(v_body) = 0 then raise exception 'Comment cannot be empty' using errcode = '22023'; end if;
    if char_length(v_body) > 2000 then raise exception 'Comment is too long (max 2000 characters)' using errcode = '22023'; end if;
    if not exists (select 1 from public.posts p where p.id = p_post_id and private.is_account_active(p.user_id)) then
        raise exception 'Post not found' using errcode = '22023';
    end if;
    if (select count(*) from public.comments c where c.user_id = v_uid and c.created_at > now() - interval '1 minute') >= 10 then
        raise exception 'You are commenting too fast. Please wait a moment.' using errcode = '54000';
    end if;

    insert into public.comments (post_id, user_id, parent_id, body, is_anonymous)
    values (p_post_id, v_uid, p_parent_id, v_body, coalesce(p_is_anonymous, false))
    returning id into v_id;

    return query select * from private.thread_rows(array[v_id], v_uid, 0);
end;
$$;

-- Owner edits show "Edited". Admin edits are intentionally silent.
create or replace function public.edit_comment(p_comment_id uuid, p_body text)
returns setof public.thread_comment language plpgsql security definer set search_path = pg_catalog, public
as $$
declare v_uid uuid := auth.uid(); v_body text := btrim(coalesce(p_body, ''));
        v_c public.comments; v_is_owner boolean; v_is_admin boolean;
begin
    if v_uid is null then raise exception 'You must be signed in' using errcode = '28000'; end if;
    select * into v_c from public.comments where id = p_comment_id;
    if not found then raise exception 'Comment not found' using errcode = '22023'; end if;

    v_is_owner := (v_c.user_id = v_uid);
    v_is_admin := public.is_admin(v_uid);
    if not v_is_owner and not v_is_admin then raise exception 'You are not allowed to edit this comment' using errcode = '42501'; end if;
    if coalesce(v_c.is_deleted, false) then raise exception 'This comment was removed' using errcode = '22023'; end if;
    if char_length(v_body) = 0 then raise exception 'Comment cannot be empty' using errcode = '22023'; end if;
    if char_length(v_body) > 2000 then raise exception 'Comment is too long (max 2000 characters)' using errcode = '22023'; end if;

    if v_body <> btrim(v_c.body) then
        if v_is_owner then
            update public.comments set body = v_body, edited_at = now() where id = p_comment_id;
        else
            -- moderator edit: edited_at untouched => nothing shown to users
            update public.comments set body = v_body, edited_by_admin = true where id = p_comment_id;
            insert into public.admin_actions (admin_id, action_type, target_type, target_id, reason)
            values (v_uid, 'edit_comment', 'comment', p_comment_id,
                    jsonb_build_object('old_body', v_c.body, 'new_body', v_body)::text);
        end if;
    end if;
    return query select * from private.thread_rows(array[p_comment_id], v_uid, 0);
end;
$$;

create or replace function public.delete_comment(p_comment_id uuid)
returns void language plpgsql security definer set search_path = pg_catalog, public
as $$
declare v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'You must be signed in' using errcode = '28000'; end if;
    update public.comments set is_deleted = true, deleted_by_author = true, body = '[deleted]'
     where id = p_comment_id and user_id = v_uid and not coalesce(is_deleted, false);
    if not found then raise exception 'Comment not found' using errcode = '22023'; end if;
end;
$$;

create or replace function public.admin_remove_comment(p_comment_id uuid)
returns void language plpgsql security definer set search_path = pg_catalog, public
as $$
declare v_uid uuid := auth.uid();
begin
    if v_uid is null or not public.is_admin(v_uid) then raise exception 'Admins only' using errcode = '42501'; end if;
    update public.comments set is_deleted = true, deleted_by_author = false, body = '[Removed by moderator]'
     where id = p_comment_id;
    update public.comment_reports set status = 'resolved' where comment_id = p_comment_id;
end;
$$;

create or replace function public.toggle_comment_like(p_comment_id uuid)
returns table (is_liked boolean, like_count integer)
language plpgsql security definer set search_path = pg_catalog, public
as $$
declare v_uid uuid := auth.uid(); v_rows integer;
begin
    if v_uid is null then raise exception 'You must be signed in' using errcode = '28000'; end if;
    if not exists (select 1 from public.comments c where c.id = p_comment_id and not coalesce(c.is_deleted, false)) then
        raise exception 'Comment not found' using errcode = '22023';
    end if;
    insert into public.comment_likes (comment_id, user_id) values (p_comment_id, v_uid)
    on conflict (comment_id, user_id) do nothing;
    get diagnostics v_rows = row_count;
    if v_rows = 1 then is_liked := true;
    else
        delete from public.comment_likes where comment_id = p_comment_id and user_id = v_uid;
        is_liked := false;
    end if;
    select coalesce(c.upvotes, 0) into like_count from public.comments c where c.id = p_comment_id;
    return next;
end;
$$;

create or replace function public.report_comment(p_comment_id uuid, p_reason text default 'other')
returns void language plpgsql security definer set search_path = pg_catalog, public
as $$
declare v_uid uuid := auth.uid(); v_owner uuid; v_reason text := lower(coalesce(p_reason, 'other'));
begin
    if v_uid is null then raise exception 'You must be signed in' using errcode = '28000'; end if;
    select c.user_id into v_owner from public.comments c where c.id = p_comment_id;
    if not found then raise exception 'Comment not found' using errcode = '22023'; end if;
    if v_owner = v_uid then raise exception 'You cannot report your own comment' using errcode = '22023'; end if;
    if v_reason not in ('spam','harassment','misinformation','inappropriate','other') then v_reason := 'other'; end if;
    insert into public.comment_reports (comment_id, reported_by, reason, status)
    values (p_comment_id, v_uid, v_reason, 'pending')
    on conflict (comment_id, reported_by) do nothing;
end;
$$;

-- 10. Grants
revoke all on function public.get_thread_comments(uuid, text, integer, integer) from public, anon, authenticated;
revoke all on function public.get_user_comments(uuid, integer, integer) from public, anon, authenticated;
revoke all on function public.get_user_comment_count(uuid) from public, anon, authenticated;
revoke all on function public.add_comment(uuid, text, boolean, uuid) from public, anon, authenticated;
revoke all on function public.edit_comment(uuid, text) from public, anon, authenticated;
revoke all on function public.delete_comment(uuid) from public, anon, authenticated;
revoke all on function public.admin_remove_comment(uuid) from public, anon, authenticated;
revoke all on function public.toggle_comment_like(uuid) from public, anon, authenticated;
revoke all on function public.report_comment(uuid, text) from public, anon, authenticated;

grant execute on function public.get_thread_comments(uuid, text, integer, integer) to anon, authenticated;
grant execute on function public.get_user_comments(uuid, integer, integer) to anon, authenticated;
grant execute on function public.get_user_comment_count(uuid) to anon, authenticated;
grant execute on function public.add_comment(uuid, text, boolean, uuid) to authenticated;
grant execute on function public.edit_comment(uuid, text) to authenticated;
grant execute on function public.delete_comment(uuid) to authenticated;
grant execute on function public.admin_remove_comment(uuid) to authenticated;
grant execute on function public.toggle_comment_like(uuid) to authenticated;
grant execute on function public.report_comment(uuid, text) to authenticated;
