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

revoke all on function public.deactivate_user_account() from public, anon;
revoke all on function public.reactivate_user_account() from public, anon;
grant execute on function public.deactivate_user_account() to authenticated;
grant execute on function public.reactivate_user_account() to authenticated;

commit;
