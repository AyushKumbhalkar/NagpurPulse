-- Allow one FCM token per installation rather than one token per account.
-- Prepared on NagpurePUlse-feature-v2 for review only; NOT applied to live Supabase.
-- Apply before deploying the updated AuthRepository and send-push function.
--
-- Fail safely if existing data has the same token attached to multiple accounts;
-- reconcile those rows manually before retrying instead of silently deleting data.
do $migration$
begin
    if exists (
        select 1
        from public.device_tokens
        group by fcm_token
        having count(*) > 1
    ) then
        raise exception 'Duplicate FCM tokens exist in public.device_tokens; reconcile them before applying this migration';
    end if;
end;
$migration$;

alter table public.device_tokens
    drop constraint if exists device_tokens_user_id_key;

create unique index if not exists device_tokens_fcm_token_uidx
    on public.device_tokens (fcm_token);

create index if not exists device_tokens_user_id_idx
    on public.device_tokens (user_id);
