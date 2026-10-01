-- Authenticate database-triggered push requests with a random secret stored in Vault.
-- The Edge Function validates this secret through a service-role-only RPC.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM vault.secrets WHERE name = 'notification_webhook_secret'
  ) THEN
    PERFORM vault.create_secret(
      gen_random_uuid()::text || gen_random_uuid()::text,
      'notification_webhook_secret',
      'Authenticates database-triggered notification push requests',
      NULL
    );
  END IF;
END
$$;

CREATE OR REPLACE FUNCTION public.validate_notification_webhook_secret(p_candidate text)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = pg_catalog
AS $$
  SELECT p_candidate IS NOT NULL AND EXISTS (
    SELECT 1
    FROM vault.decrypted_secrets
    WHERE name = 'notification_webhook_secret'
      AND decrypted_secret = p_candidate
  );
$$;

REVOKE ALL ON FUNCTION public.validate_notification_webhook_secret(text) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.validate_notification_webhook_secret(text) FROM anon, authenticated;
GRANT EXECUTE ON FUNCTION public.validate_notification_webhook_secret(text) TO service_role;

CREATE OR REPLACE FUNCTION public.dispatch_notification_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, vault, net
AS $$
DECLARE
  webhook_secret text;
BEGIN
  SELECT decrypted_secret
    INTO webhook_secret
    FROM vault.decrypted_secrets
   WHERE name = 'notification_webhook_secret'
   LIMIT 1;

  IF webhook_secret IS NULL THEN
    RAISE WARNING 'Notification webhook secret is missing; push was not dispatched';
    RETURN NEW;
  END IF;

  PERFORM net.http_post(
    url := 'https://eazkmfzegxmdkbowohiy.supabase.co/functions/v1/send-push-notification',
    body := jsonb_build_object(
      'type', 'INSERT',
      'schema', 'public',
      'table', 'notifications',
      'record', to_jsonb(NEW)
    ),
    params := '{}'::jsonb,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-notification-webhook-secret', webhook_secret
    ),
    timeout_milliseconds := 5000
  );

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_new_notification ON public.notifications;
CREATE TRIGGER on_new_notification
AFTER INSERT ON public.notifications
FOR EACH ROW
EXECUTE FUNCTION public.dispatch_notification_push();

REVOKE ALL ON FUNCTION public.dispatch_notification_push() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.dispatch_notification_push() FROM anon, authenticated;
