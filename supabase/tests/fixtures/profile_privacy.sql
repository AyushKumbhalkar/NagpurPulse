-- Minimal Supabase authorization fixture. Use ONLY a fresh disposable database.
-- Fail before making changes if this looks like an application database.
do $fixture$
begin
    if exists (select 1 from pg_tables where schemaname = 'public')
       or exists (select 1 from pg_namespace where nspname = 'auth') then
        raise exception 'Profile privacy tests require a fresh disposable database';
    end if;
end;
$fixture$;

-- Roles belong to the cluster, so a fresh test database can reuse those left
-- by an earlier run. Never overwrite their attributes or accept an RLS bypass.
do $roles$
begin
    if not exists (select 1 from pg_roles where rolname = 'anon') then
        create role anon nologin;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'authenticated') then
        create role authenticated nologin;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'service_role') then
        create role service_role nologin bypassrls;
    end if;
    if exists (
        select 1 from pg_roles
        where rolname in ('anon', 'authenticated') and (rolsuper or rolbypassrls)
    ) then
        raise exception 'Test anon/authenticated roles must not bypass RLS';
    end if;
end;
$roles$;
create schema auth;
create function auth.uid() returns uuid language sql stable as
    $$ select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid $$;
grant usage on schema auth to anon, authenticated;
grant execute on function auth.uid() to anon, authenticated;

create table public.profiles (
    id uuid primary key,
    hide_profile boolean default false,
    hide_posts boolean default false,
    hide_comments boolean default false,
    is_deactivated boolean not null default false,
    is_verified boolean not null default false
);
create table public.admin_roles (user_id uuid primary key, role text not null);
create table public.posts (
    id uuid primary key,
    user_id uuid,
    is_anonymous boolean not null default false
);
create table public.comments (
    id uuid primary key,
    user_id uuid,
    post_id uuid references public.posts(id),
    is_anonymous boolean not null default false
);
alter table public.profiles enable row level security;
alter table public.posts enable row level security;
alter table public.comments enable row level security;
alter table public.admin_roles enable row level security;
create policy "Public profiles viewable by everyone" on public.profiles
    for select using (true);
create policy "Posts viewable by everyone" on public.posts
    for select using (true);
create policy "Comments viewable by everyone" on public.comments
    for select using (true);
create policy "Users can update own profile" on public.profiles
    for update to authenticated using (id = auth.uid()) with check (id = auth.uid());
create policy "Admin role read own" on public.admin_roles
    for select to authenticated using (user_id = auth.uid());
grant select on public.profiles, public.posts, public.comments to anon, authenticated;
grant select on public.admin_roles to authenticated;
grant update (hide_profile, hide_posts, hide_comments) on public.profiles to authenticated;
grant select on public.profiles, public.posts, public.comments to service_role;

-- Exercise the actual existing role and account-activity helpers/policies.
\ir ../../migrations/20261006171000_scope_admin_role_helpers_to_self.sql
\ir ../../migrations/20261006164426_move_account_activity_helper_private.sql
\ir ../../migrations/20261006172000_restore_policy_helper_execute.sql
