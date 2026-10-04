-- Temporary account deactivation with an explicit reactivation-on-next-login path.
-- Prepared on nagpurpulse-updates-v5. Review and apply to Supabase before shipping the app.
begin;

alter table public.profiles
    add column if not exists is_deactivated boolean not null default false,
    add column if not exists deactivated_at timestamptz;

create or replace function public.deactivate_user_account()
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth
as $function$
begin
    if auth.uid() is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    update public.profiles
       set is_deactivated = true,
           deactivated_at = now()
     where id = auth.uid();

    if not found then
        raise exception 'Profile not found' using errcode = 'P0002';
    end if;
end;
$function$;

create or replace function public.reactivate_user_account()
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth
as $function$
begin
    if auth.uid() is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    update public.profiles
       set is_deactivated = false,
           deactivated_at = null
     where id = auth.uid();

    if not found then
        raise exception 'Profile not found' using errcode = 'P0002';
    end if;
end;
$function$;

-- A deactivated profile is hidden from public profile reads and authored content.
-- RESTRICTIVE policies are ANDed with existing permissive policies, so they do not
-- accidentally grant access that the app's existing policies did not grant.
create or replace function public.is_account_active(target_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = pg_catalog, public
as $function$
    select coalesce(
        (select not p.is_deactivated from public.profiles p where p.id = target_user_id),
        false
    );
$function$;

revoke all on function public.is_account_active(uuid) from public;
grant execute on function public.is_account_active(uuid) to anon, authenticated;

drop policy if exists "Active accounts only - profile visibility" on public.profiles;
create policy "Active accounts only - profile visibility"
    on public.profiles as restrictive
    for select
    to anon, authenticated
    using (id = auth.uid() or is_deactivated = false);

drop policy if exists "Active accounts only - post visibility" on public.posts;
create policy "Active accounts only - post visibility"
    on public.posts as restrictive
    for select
    to anon, authenticated
    using (public.is_account_active(user_id));

drop policy if exists "Active accounts only - post insert" on public.posts;
create policy "Active accounts only - post insert"
    on public.posts as restrictive
    for insert
    to authenticated
    with check (auth.uid() = user_id and public.is_account_active(auth.uid()));

drop policy if exists "Active accounts only - post update" on public.posts;
create policy "Active accounts only - post update"
    on public.posts as restrictive
    for update
    to authenticated
    using (public.is_account_active(auth.uid()))
    with check (auth.uid() = user_id and public.is_account_active(auth.uid()));

drop policy if exists "Active accounts only - post delete" on public.posts;
create policy "Active accounts only - post delete"
    on public.posts as restrictive
    for delete
    to authenticated
    using (auth.uid() = user_id and public.is_account_active(auth.uid()));

drop policy if exists "Active accounts only - comment visibility" on public.comments;
create policy "Active accounts only - comment visibility"
    on public.comments as restrictive
    for select
    to anon, authenticated
    using (public.is_account_active(user_id));

drop policy if exists "Active accounts only - comment insert" on public.comments;
create policy "Active accounts only - comment insert"
    on public.comments as restrictive
    for insert
    to authenticated
    with check (auth.uid() = user_id and public.is_account_active(auth.uid()));

drop policy if exists "Active accounts only - comment update" on public.comments;
create policy "Active accounts only - comment update"
    on public.comments as restrictive
    for update
    to authenticated
    using (public.is_account_active(auth.uid()))
    with check (auth.uid() = user_id and public.is_account_active(auth.uid()));

drop policy if exists "Active accounts only - comment delete" on public.comments;
create policy "Active accounts only - comment delete"
    on public.comments as restrictive
    for delete
    to authenticated
    using (auth.uid() = user_id and public.is_account_active(auth.uid()));

revoke all on function public.deactivate_user_account() from public, anon;
revoke all on function public.reactivate_user_account() from public, anon;
grant execute on function public.deactivate_user_account() to authenticated;
grant execute on function public.reactivate_user_account() to authenticated;

commit;
