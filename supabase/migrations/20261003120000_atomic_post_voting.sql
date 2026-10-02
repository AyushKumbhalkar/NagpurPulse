-- Atomic, authenticated post voting. All vote-row, counter, and owner-karma
-- changes occur in one PostgreSQL transaction (the function invocation).
CREATE OR REPLACE FUNCTION public.vote_post_atomic(
  p_post_id uuid,
  p_vote_type text
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, auth
AS $$
DECLARE
  v_user_id uuid := auth.uid();
  v_owner_id uuid;
  v_old_vote text;
  v_has_vote boolean := false;
  v_up_delta integer := 0;
  v_down_delta integer := 0;
  v_karma_delta integer := 0;
  v_current_up integer;
  v_current_down integer;
BEGIN
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'Authentication required to vote' USING ERRCODE = '28000';
  END IF;
  IF p_vote_type NOT IN ('up', 'down') THEN
    RAISE EXCEPTION 'Invalid vote type: %', p_vote_type USING ERRCODE = '22023';
  END IF;

  -- Serialize votes on the same post so counters cannot be overwritten by stale reads.
  SELECT p.user_id, COALESCE(p.upvotes, 0), COALESCE(p.downvotes, 0)
    INTO v_owner_id, v_current_up, v_current_down
    FROM public.posts p
   WHERE p.id = p_post_id
   FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Post not found' USING ERRCODE = 'P0002';
  END IF;

  SELECT v.vote_type INTO v_old_vote
    FROM public.votes v
   WHERE v.user_id = v_user_id AND v.post_id = p_post_id
   FOR UPDATE;
  v_has_vote := FOUND;

  IF NOT v_has_vote THEN
    INSERT INTO public.votes(user_id, post_id, vote_type)
    VALUES (v_user_id, p_post_id, p_vote_type);
    v_up_delta := CASE WHEN p_vote_type = 'up' THEN 1 ELSE 0 END;
    v_down_delta := CASE WHEN p_vote_type = 'down' THEN 1 ELSE 0 END;
    v_karma_delta := CASE WHEN p_vote_type = 'up' THEN 1 ELSE -1 END;
  ELSIF v_old_vote = p_vote_type THEN
    DELETE FROM public.votes WHERE user_id = v_user_id AND post_id = p_post_id;
    v_up_delta := CASE WHEN v_old_vote = 'up' THEN -1 ELSE 0 END;
    v_down_delta := CASE WHEN v_old_vote = 'down' THEN -1 ELSE 0 END;
    v_karma_delta := CASE WHEN v_old_vote = 'up' THEN -1 ELSE 1 END;
  ELSE
    UPDATE public.votes SET vote_type = p_vote_type
     WHERE user_id = v_user_id AND post_id = p_post_id;
    v_up_delta := (CASE WHEN p_vote_type = 'up' THEN 1 ELSE 0 END)
                - (CASE WHEN v_old_vote = 'up' THEN 1 ELSE 0 END);
    v_down_delta := (CASE WHEN p_vote_type = 'down' THEN 1 ELSE 0 END)
                  - (CASE WHEN v_old_vote = 'down' THEN 1 ELSE 0 END);
    v_karma_delta := (CASE WHEN p_vote_type = 'up' THEN 1 ELSE -1 END)
                   - (CASE WHEN v_old_vote = 'up' THEN 1 ELSE -1 END);
  END IF;

  UPDATE public.posts
     SET upvotes = GREATEST(0, v_current_up + v_up_delta),
         downvotes = GREATEST(0, v_current_down + v_down_delta)
   WHERE id = p_post_id;

  IF v_owner_id IS NOT NULL AND v_karma_delta <> 0 THEN
    UPDATE public.profiles
       SET karma = GREATEST(0, COALESCE(karma, 0) + v_karma_delta)
     WHERE id = v_owner_id;
  END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.vote_post_atomic(uuid, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.vote_post_atomic(uuid, text) TO authenticated, service_role;
