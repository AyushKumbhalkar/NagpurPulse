-- Keep a single push delivery path for notifications.
-- The webhook-backed send-push-notification function is the canonical sender.
DROP TRIGGER IF EXISTS on_notification_insert ON public.notifications;
DROP FUNCTION IF EXISTS public.notify_user_on_insert();

-- Publish notification INSERT events so open inboxes refresh in real time.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'notifications'
  ) THEN
    EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications';
  END IF;
END
$$;
