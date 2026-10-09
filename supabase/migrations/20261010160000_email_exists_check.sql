-- Pre-signup email check: lets the app say "this email is already registered" as soon as the
-- user taps Next on the email step, instead of after they have typed a password.
--
-- Only CONFIRMED accounts count: a half-finished signup (never entered the OTP) must still be
-- able to continue, because Supabase simply re-sends the confirmation code for those.
--
-- Security note: like every "email taken?" check this reveals whether an address is registered.
-- This RPC intentionally returns only a boolean and accepts no other input.
create or replace function public.email_exists(p_email text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from auth.users u
    where lower(u.email) = lower(btrim(p_email))
      and u.email_confirmed_at is not null
      and u.deleted_at is null
  );
$$;

revoke all on function public.email_exists(text) from public;
grant execute on function public.email_exists(text) to anon, authenticated;
