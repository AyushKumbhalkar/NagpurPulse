ALTER TABLE public.notifications
  ADD COLUMN IF NOT EXISTS related_conversation_id uuid
  REFERENCES public.conversations(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS notifications_related_conversation_id_idx
  ON public.notifications (related_conversation_id)
  WHERE related_conversation_id IS NOT NULL;
