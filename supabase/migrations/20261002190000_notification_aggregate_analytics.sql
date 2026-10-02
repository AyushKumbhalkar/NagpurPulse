-- Privacy-conscious notification analytics.
-- Store only daily aggregate counters; never persist user IDs, device IDs,
-- notification IDs, post IDs, comment IDs, message text, or FCM tokens.
create table if not exists public.notification_analytics_daily (
    event_date date not null default (timezone('utc', now())::date),
    event_type text not null,
    notification_type text not null default 'all',
    event_count bigint not null default 0 check (event_count >= 0),
    updated_at timestamptz not null default now(),
    constraint notification_analytics_daily_pk primary key (event_date, event_type, notification_type),
    constraint notification_analytics_event_type_check check (
      event_type in ('inbox_open', 'notification_open', 'push_delivered', 'push_opt_out')
    ),
    constraint notification_analytics_notification_type_check check (
      notification_type in (
        'all', 'comment', 'reply', 'mention', 'upvote', 'like', 'comment_like',
        'message', 'alert', 'emergency', 'badge', 'trending', 'community',
        'digest', 'admin_warning', 'admin_suspension', 'admin_ban', 'general'
      )
    )
);

alter table public.notification_analytics_daily enable row level security;
revoke all on public.notification_analytics_daily from anon, authenticated;
grant select, insert, update on public.notification_analytics_daily to service_role;

create or replace function public.record_notification_analytics(
    p_event_type text,
    p_notification_type text default 'all'
)
returns void
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $function$
declare
  normalized_type text := lower(coalesce(nullif(trim(p_notification_type), ''), 'all'));
  request_role text := coalesce(current_setting('request.jwt.claim.role', true), '');
begin
  -- App users must be authenticated. Trusted service-role calls from the
  -- push function are also allowed. No user identity is stored in the table.
  if auth.uid() is null and request_role <> 'service_role' then
    raise exception 'authentication required';
  end if;

  if p_event_type not in ('inbox_open', 'notification_open', 'push_delivered', 'push_opt_out') then
    raise exception 'unsupported notification analytics event';
  end if;

  if normalized_type not in (
    'all', 'comment', 'reply', 'mention', 'upvote', 'like', 'comment_like',
    'message', 'alert', 'emergency', 'badge', 'trending', 'community',
    'digest', 'admin_warning', 'admin_suspension', 'admin_ban', 'general'
  ) then
    normalized_type := 'general';
  end if;

  insert into public.notification_analytics_daily (event_date, event_type, notification_type, event_count, updated_at)
  values (timezone('utc', now())::date, p_event_type, normalized_type, 1, now())
  on conflict (event_date, event_type, notification_type)
  do update set event_count = public.notification_analytics_daily.event_count + 1,
                updated_at = now();
end;
$function$;

revoke all on function public.record_notification_analytics(text, text) from public, anon;
grant execute on function public.record_notification_analytics(text, text) to authenticated, service_role;

comment on table public.notification_analytics_daily is
  'Privacy-conscious daily notification counters only; no user, device, content, or notification identifiers.';
comment on function public.record_notification_analytics(text, text) is
  'Increments allow-listed daily notification analytics counters without storing actor identity.';
