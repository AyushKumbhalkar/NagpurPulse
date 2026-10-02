-- Allow notifications to link directly to the comment or reply that caused them.
-- Nullable and additive: existing notifications remain valid.
alter table public.notifications
    add column if not exists related_comment_id uuid;

create index if not exists notifications_related_comment_id_idx
    on public.notifications (related_comment_id)
    where related_comment_id is not null;
