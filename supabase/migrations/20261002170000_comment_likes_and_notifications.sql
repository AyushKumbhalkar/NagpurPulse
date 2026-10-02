-- Per-user comment likes with server-maintained counts and authoritative notifications.
-- Applied to production only after the matching app changes are committed to a feature branch.

CREATE TABLE IF NOT EXISTS public.comment_likes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id uuid NOT NULL REFERENCES public.comments(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT comment_likes_comment_user_unique UNIQUE (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS comment_likes_user_id_idx
    ON public.comment_likes(user_id);

ALTER TABLE public.comment_likes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own comment likes" ON public.comment_likes;
CREATE POLICY "Users can view own comment likes"
    ON public.comment_likes FOR SELECT TO authenticated
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can create own comment likes" ON public.comment_likes;
CREATE POLICY "Users can create own comment likes"
    ON public.comment_likes FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own comment likes" ON public.comment_likes;
CREATE POLICY "Users can delete own comment likes"
    ON public.comment_likes FOR DELETE TO authenticated
    USING (auth.uid() = user_id);

GRANT SELECT, INSERT, DELETE ON public.comment_likes TO authenticated;

CREATE OR REPLACE FUNCTION public.maintain_comment_like_count()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.comments
           SET upvotes = COALESCE(upvotes, 0) + 1
         WHERE id = NEW.comment_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.comments
           SET upvotes = GREATEST(COALESCE(upvotes, 0) - 1, 0)
         WHERE id = OLD.comment_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$;

REVOKE ALL ON FUNCTION public.maintain_comment_like_count() FROM PUBLIC, anon, authenticated;

DROP TRIGGER IF EXISTS maintain_comment_like_count_insert ON public.comment_likes;
CREATE TRIGGER maintain_comment_like_count_insert
AFTER INSERT ON public.comment_likes
FOR EACH ROW EXECUTE FUNCTION public.maintain_comment_like_count();

DROP TRIGGER IF EXISTS maintain_comment_like_count_delete ON public.comment_likes;
CREATE TRIGGER maintain_comment_like_count_delete
AFTER DELETE ON public.comment_likes
FOR EACH ROW EXECUTE FUNCTION public.maintain_comment_like_count();

CREATE OR REPLACE FUNCTION public.notify_comment_like_insert()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    comment_owner uuid;
    comment_post_id uuid;
    comment_body text;
    liker_name text;
BEGIN
    SELECT c.user_id, c.post_id, c.body
      INTO comment_owner, comment_post_id, comment_body
      FROM public.comments c
     WHERE c.id = NEW.comment_id;

    IF comment_owner IS NULL OR comment_owner = NEW.user_id THEN
        RETURN NEW;
    END IF;

    IF NOT COALESCE((
        SELECT up.notif_upvotes
          FROM public.user_preferences up
         WHERE up.user_id = comment_owner
    ), true) THEN
        RETURN NEW;
    END IF;

    SELECT p.username INTO liker_name
      FROM public.profiles p
     WHERE p.id = NEW.user_id;

    PERFORM public.create_notification_event(
        comment_owner,
        'comment_like',
        COALESCE(liker_name, 'Someone') || ' liked your comment',
        left(COALESCE(comment_body, ''), 120),
        comment_post_id,
        NULL,
        NEW.user_id
    );

    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.notify_comment_like_insert() FROM PUBLIC, anon, authenticated;

DROP TRIGGER IF EXISTS notifications_from_comment_like_insert ON public.comment_likes;
CREATE TRIGGER notifications_from_comment_like_insert
AFTER INSERT ON public.comment_likes
FOR EACH ROW EXECUTE FUNCTION public.notify_comment_like_insert();
