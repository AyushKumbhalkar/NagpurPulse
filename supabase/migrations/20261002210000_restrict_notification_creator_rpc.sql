-- Prevent direct client invocation of the SECURITY DEFINER notification creator.
-- Notification rows should be created by trusted database triggers, not arbitrary
-- anonymous/authenticated RPC calls. Trigger functions execute with their owner's
-- privileges and can still invoke this helper.
revoke all on function public.create_notification_event(uuid, text, text, text, uuid, uuid, uuid, uuid)
  from public, anon, authenticated;
grant execute on function public.create_notification_event(uuid, text, text, text, uuid, uuid, uuid, uuid)
  to service_role;

comment on function public.create_notification_event(uuid, text, text, text, uuid, uuid, uuid, uuid) is
  'Internal notification helper. Direct anon/authenticated execution is revoked; database triggers create notification events.';
