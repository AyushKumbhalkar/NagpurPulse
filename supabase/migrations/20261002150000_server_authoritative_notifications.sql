-- Server-authoritative notification events for NagpurPulse.
-- This migration is committed to the feature branch only; it must be reviewed
-- and tested on a Supabase development branch before production deployment.
--
-- Authenticated clients may read/update their own notification rows, but cannot
-- forge notifications for other users. Trusted SECURITY DEFINER triggers create
-- notifications from the underlying comment, vote, and message events.

CREATE OR REPLACE FUNCTION public.create_notification_event(
  p_user_id uuid,
  p_type text,
  p_title text,
  p_body text,
  p_post_id uuid DEFAULT NULL,
  p_conversation_id uuid DEFAULT NULL,
  p_sender_id uuid DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
  sender_name text;
  sender_avatar text;
BEGIN
  IF p_user_id IS NULL OR p_type IS NULL OR p_title IS NULL THEN
    RETURN;
  END IF;

  -- Prevent self-notifications and resolve sender presentation data on the server.
  IF p_sender_id IS NOT NULL AND p_sender_id = p_user_id THEN
    RETURN;
  END IF;

  IF p_sender_id IS NOT NULL THEN
    SELECT username, avatar_url
      INTO sender_name, sender_avatar
      FROM public.profiles
     WHERE id = p_sender_id;
  END IF;

  INSERT INTO public.notifications (
    user_id, type, title, body, is_read,
    related_post_id, related_conversation_id,
    sender_username, sender_avatar_url
  )
  VALUES (
    p_user_id, p_type, p_title, p_body, false,
    p_post_id, p_conversation_id, sender_name, sender_avatar
  );
END;
$$;

REVOKE ALL ON FUNCTION public.create_notification_event(uuid, text, text, text, uuid, uuid, uuid) FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.notify_comment_insert()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
  post_owner uuid;
  post_title text;
  recipient_id uuid;
  recipient_name text;
  mention_match text[];
  seen_mentions text[] := ARRAY[]::text[];
  mentioned_user_id uuid;
BEGIN
  -- Preserve the existing privacy behavior: anonymous comments do not reveal
  -- the author through notification sender fields or trigger notifications.
  IF NEW.user_id IS NULL OR COALESCE(NEW.is_anonymous, false) THEN
    RETURN NEW;
  END IF;

  SELECT p.user_id, p.title
    INTO post_owner, post_title
    FROM public.posts p
   WHERE p.id = NEW.post_id;

  IF NEW.parent_id IS NOT NULL THEN
    SELECT c.user_id
      INTO recipient_id
      FROM public.comments c
     WHERE c.id = NEW.parent_id;
    recipient_name := 'replied to your comment';
    IF recipient_id IS NOT NULL
       AND recipient_id <> NEW.user_id
       AND COALESCE((
         SELECT up.notif_replies FROM public.user_preferences up
          WHERE up.user_id = recipient_id
       ), true) THEN
      PERFORM public.create_notification_event(
        recipient_id, 'reply',
        COALESCE((SELECT username FROM public.profiles WHERE id = NEW.user_id), 'Someone') || ' replied to your comment',
        left(NEW.body, 120), NEW.post_id, NULL, NEW.user_id
      );
    END IF;
  ELSIF post_owner IS NOT NULL
        AND post_owner <> NEW.user_id
        AND COALESCE((
          SELECT up.notif_replies FROM public.user_preferences up
           WHERE up.user_id = post_owner
        ), true) THEN
    PERFORM public.create_notification_event(
      post_owner, 'comment',
      COALESCE((SELECT username FROM public.profiles WHERE id = NEW.user_id), 'Someone') || ' commented on your post',
      left(COALESCE(post_title, ''), 120), NEW.post_id, NULL, NEW.user_id
    );
  END IF;

  -- Mentions are resolved by username on the server, so clients cannot forge
  -- sender metadata or choose arbitrary notification recipients.
  FOR mention_match IN
    SELECT regexp_matches(COALESCE(NEW.body, ''), '@([A-Za-z0-9_]+)', 'g')
  LOOP
    IF lower(mention_match[1]) = ANY(seen_mentions) THEN
      CONTINUE;
    END IF;
    seen_mentions := array_append(seen_mentions, lower(mention_match[1]));

    SELECT p.id INTO mentioned_user_id
      FROM public.profiles p
     WHERE lower(p.username) = lower(mention_match[1])
     LIMIT 1;

    IF mentioned_user_id IS NOT NULL
       AND mentioned_user_id <> NEW.user_id
       AND COALESCE((
         SELECT up.notif_mentions FROM public.user_preferences up
          WHERE up.user_id = mentioned_user_id
       ), true) THEN
      PERFORM public.create_notification_event(
        mentioned_user_id, 'mention',
        '@' || COALESCE((SELECT username FROM public.profiles WHERE id = NEW.user_id), 'Someone') || ' mentioned you',
        left(NEW.body, 120), NEW.post_id, NULL, NEW.user_id
      );
    END IF;
  END LOOP;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS notifications_from_comment_insert ON public.comments;
CREATE TRIGGER notifications_from_comment_insert
AFTER INSERT ON public.comments
FOR EACH ROW EXECUTE FUNCTION public.notify_comment_insert();

CREATE OR REPLACE FUNCTION public.notify_post_upvote()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
  owner_id uuid;
  post_title text;
  voter_name text;
  upvote_count integer;
  milestone_title text;
BEGIN
  IF NEW.vote_type IS DISTINCT FROM 'up' OR (TG_OP = 'UPDATE' AND OLD.vote_type = 'up') THEN
    RETURN NEW;
  END IF;

  SELECT p.user_id, p.title INTO owner_id, post_title
    FROM public.posts p WHERE p.id = NEW.post_id;

  IF owner_id IS NULL OR owner_id = NEW.user_id THEN
    RETURN NEW;
  END IF;

  IF NOT COALESCE((
    SELECT up.notif_upvotes FROM public.user_preferences up
     WHERE up.user_id = owner_id
  ), true) THEN
    RETURN NEW;
  END IF;

  SELECT username INTO voter_name FROM public.profiles WHERE id = NEW.user_id;
  PERFORM public.create_notification_event(
    owner_id, 'upvote',
    COALESCE(voter_name, 'Someone') || ' upvoted your post',
    left(COALESCE(post_title, ''), 120), NEW.post_id, NULL, NEW.user_id
  );

  -- Use the actual vote rows rather than the client-maintained posts.upvotes
  -- counter, which must not be trusted for security-sensitive side effects.
  SELECT count(*)::integer INTO upvote_count
    FROM public.votes v
   WHERE v.post_id = NEW.post_id
     AND v.vote_type = 'up';

  IF upvote_count IN (10, 50, 100, 500, 1000) THEN
    milestone_title := 'Your post reached ' || upvote_count || ' upvotes! 🎉';
    IF NOT EXISTS (
      SELECT 1 FROM public.notifications n
       WHERE n.user_id = owner_id
         AND n.related_post_id = NEW.post_id
         AND n.type = 'upvote'
         AND n.title = milestone_title
    ) THEN
      PERFORM public.create_notification_event(
        owner_id, 'upvote', milestone_title,
        left(COALESCE(post_title, ''), 120), NEW.post_id, NULL, NULL
      );
    END IF;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS notifications_from_post_upvote ON public.votes;
DROP TRIGGER IF EXISTS notifications_from_post_upvote_insert ON public.votes;
DROP TRIGGER IF EXISTS notifications_from_post_upvote_update ON public.votes;

-- Separate event triggers avoid PostgreSQL's UPDATE OF / multi-event syntax
-- restrictions and keep vote-type changes covered.
CREATE TRIGGER notifications_from_post_upvote_insert
AFTER INSERT ON public.votes
FOR EACH ROW EXECUTE FUNCTION public.notify_post_upvote();

CREATE TRIGGER notifications_from_post_upvote_update
AFTER UPDATE OF vote_type ON public.votes
FOR EACH ROW EXECUTE FUNCTION public.notify_post_upvote();

CREATE OR REPLACE FUNCTION public.notify_message_insert()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
  participant_one_id uuid;
  participant_two_id uuid;
  recipient_id uuid;
  sender_name text;
BEGIN
  IF NEW.sender_id IS NULL OR NEW.conversation_id IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT c.participant_one, c.participant_two
    INTO participant_one_id, participant_two_id
    FROM public.conversations c
   WHERE c.id = NEW.conversation_id;

  IF participant_one_id IS NULL OR participant_two_id IS NULL THEN
    RETURN NEW;
  END IF;

  IF NEW.sender_id = participant_one_id THEN
    recipient_id := participant_two_id;
  ELSIF NEW.sender_id = participant_two_id THEN
    recipient_id := participant_one_id;
  ELSE
    -- Do not create notifications for messages from non-participants.
    RETURN NEW;
  END IF;

  IF NOT COALESCE((
    SELECT up.notif_messages FROM public.user_preferences up
     WHERE up.user_id = recipient_id
  ), true) THEN
    RETURN NEW;
  END IF;

  SELECT username INTO sender_name FROM public.profiles WHERE id = NEW.sender_id;
  PERFORM public.create_notification_event(
    recipient_id, 'message',
    COALESCE(sender_name, 'Someone') || ' sent you a message',
    left(COALESCE(NEW.content, ''), 120), NULL, NEW.conversation_id, NEW.sender_id
  );

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS notifications_from_message_insert ON public.messages;
CREATE TRIGGER notifications_from_message_insert
AFTER INSERT ON public.messages
FOR EACH ROW EXECUTE FUNCTION public.notify_message_insert();

REVOKE ALL ON FUNCTION public.notify_comment_insert() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.notify_post_upvote() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.notify_message_insert() FROM PUBLIC, anon, authenticated;

-- Remove every permissive INSERT policy on notifications. Trigger functions
-- execute as their owner; clients must not write arbitrary notification rows.
DO $$
DECLARE policy_row record;
BEGIN
  FOR policy_row IN
    SELECT policyname
      FROM pg_policies
     WHERE schemaname = 'public'
       AND tablename = 'notifications'
       AND cmd IN ('INSERT', 'ALL')
  LOOP
    EXECUTE format('DROP POLICY IF EXISTS %I ON public.notifications', policy_row.policyname);
  END LOOP;
END
$$;

-- Keep recipient-only SELECT and UPDATE policies; remove duplicate policies
-- only if they are broad ALL policies (handled above).
