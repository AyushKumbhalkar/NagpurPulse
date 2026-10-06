-- Move the account activity helper out of the public API schema.
create schema if not exists private;
create or replace function private.is_account_active(target_user_id uuid)
returns boolean language sql stable security definer set search_path=pg_catalog,public
as $$ select coalesce((select not p.is_deactivated from public.profiles p where p.id=target_user_id),false) $$;
revoke all on function private.is_account_active(uuid) from public,anon,authenticated;
drop policy if exists "Active accounts only - post visibility" on public.posts;
create policy "Active accounts only - post visibility" on public.posts as restrictive for select to anon,authenticated using(private.is_account_active(user_id));
drop policy if exists "Active accounts only - post insert" on public.posts;
create policy "Active accounts only - post insert" on public.posts as restrictive for insert to authenticated with check(auth.uid()=user_id and private.is_account_active(auth.uid()));
drop policy if exists "Active accounts only - post update" on public.posts;
create policy "Active accounts only - post update" on public.posts as restrictive for update to authenticated using(private.is_account_active(auth.uid())) with check(auth.uid()=user_id and private.is_account_active(auth.uid()));
drop policy if exists "Active accounts only - post delete" on public.posts;
create policy "Active accounts only - post delete" on public.posts as restrictive for delete to authenticated using(auth.uid()=user_id and private.is_account_active(auth.uid()));
drop policy if exists "Active accounts only - comment visibility" on public.comments;
create policy "Active accounts only - comment visibility" on public.comments as restrictive for select to anon,authenticated using(private.is_account_active(user_id));
drop policy if exists "Active accounts only - comment insert" on public.comments;
create policy "Active accounts only - comment insert" on public.comments as restrictive for insert to authenticated with check(auth.uid()=user_id and private.is_account_active(auth.uid()));
drop policy if exists "Active accounts only - comment update" on public.comments;
create policy "Active accounts only - comment update" on public.comments as restrictive for update to authenticated using(private.is_account_active(auth.uid())) with check(auth.uid()=user_id and private.is_account_active(auth.uid()));
drop policy if exists "Active accounts only - comment delete" on public.comments;
create policy "Active accounts only - comment delete" on public.comments as restrictive for delete to authenticated using(auth.uid()=user_id and private.is_account_active(auth.uid()));
drop policy if exists "Active accounts only - profile visibility" on public.profiles;
create policy "Active accounts only - profile visibility" on public.profiles as restrictive for select to anon,authenticated using(id=auth.uid() or is_deactivated=false);
drop function if exists public.is_account_active(uuid);