-- Notify only opted-in users when a post is explicitly selected/pinned
-- or marked as trending. Ordinary new posts do not fan out notifications.
CREATE OR REPLACE FUNCTION public.notify_selected_trending_post()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
  recipient_id uuid;
BEGIN
  IF NOT (COALESCE(NEW.is_pinned, false) OR NEW.post_type = 'trending') THEN
    RETURN NEW;
  END IF;

  -- Avoid repeat fan-out if an already-selected post is edited.
  IF TG_OP = 'UPDATE'
     AND (COALESCE(OLD.is_pinned, false) OR OLD.post_type = 'trending') THEN
    RETURN NEW;
  END IF;

  FOR recipient_id IN
    SELECT p.id
      FROM public.profiles p
      LEFT JOIN public.user_preferences pref ON pref.user_id = p.id
     WHERE p.id <> NEW.user_id
       AND COALESCE(pref.notif_push, true)
       AND COALESCE(pref.notif_trending, true)
  LOOP
    PERFORM public.create_notification_event(
      recipient_id,
      'trending',
      'A post was selected for NagpurPulse',
      LEFT(COALESCE(NEW.title, 'A post is trending'), 120),
      NEW.id,
      NULL,
      NEW.user_id,
      NULL
    );
  END LOOP;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS notify_selected_trending_post_insert ON public.posts;
DROP TRIGGER IF EXISTS notify_selected_trending_post_update ON public.posts;

CREATE TRIGGER notify_selected_trending_post_insert
AFTER INSERT ON public.posts
FOR EACH ROW
WHEN (COALESCE(NEW.is_pinned, false) OR NEW.post_type = 'trending')
EXECUTE FUNCTION public.notify_selected_trending_post();

CREATE TRIGGER notify_selected_trending_post_update
AFTER UPDATE OF is_pinned, post_type ON public.posts
FOR EACH ROW
WHEN (COALESCE(NEW.is_pinned, false) OR NEW.post_type = 'trending')
EXECUTE FUNCTION public.notify_selected_trending_post();

REVOKE ALL ON FUNCTION public.notify_selected_trending_post() FROM PUBLIC, anon, authenticated;
