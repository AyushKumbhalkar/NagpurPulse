-- Enable Supabase Realtime for comments so thread screens can receive
-- INSERT/UPDATE/DELETE events and refresh comments without manual reloads.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_publication_tables
         WHERE pubname = 'supabase_realtime'
           AND schemaname = 'public'
           AND tablename = 'comments'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.comments;
    END IF;
END;
$$;
