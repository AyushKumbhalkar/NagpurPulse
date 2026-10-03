-- Secure account deletion against deleting another user's account.
-- This migration is prepared for review only; it has NOT been applied to Supabase.
create or replace function public.delete_user_account(target_user_id uuid)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth
as $function$
begin
    if auth.uid() is null or auth.uid() is distinct from target_user_id then
        raise exception 'Not authorized to delete this account'
            using errcode = '42501';
    end if;

    delete from public.notifications
    where related_post_id in (
        select id from public.posts where user_id = target_user_id
    );

    delete from public.notifications where user_id = target_user_id;
    delete from public.comments where user_id = target_user_id;
    delete from public.saved_posts where user_id = target_user_id;
    delete from public.votes where user_id = target_user_id;
    delete from public.device_tokens where user_id = target_user_id;
    delete from public.user_preferences where user_id = target_user_id;
    delete from public.posts where user_id = target_user_id;
    delete from public.profiles where id = target_user_id;
    delete from auth.users where id = target_user_id;
end;
$function$;

revoke all on function public.delete_user_account(uuid) from public;
revoke all on function public.delete_user_account(uuid) from anon;
grant execute on function public.delete_user_account(uuid) to authenticated;
