-- Tighten aggregate analytics: clients may report client-observable events,
-- but only the trusted service role may report successful push delivery.
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
  if auth.uid() is null and request_role <> 'service_role' then
    raise exception 'authentication required';
  end if;

  if p_event_type not in ('inbox_open', 'notification_open', 'push_delivered', 'push_opt_out') then
    raise exception 'unsupported notification analytics event';
  end if;

  if p_event_type = 'push_delivered' and request_role <> 'service_role' then
    raise exception 'push delivery analytics requires service role';
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

comment on function public.record_notification_analytics(text, text) is
  'Increments allow-listed daily notification analytics counters; push delivery events require service role.';