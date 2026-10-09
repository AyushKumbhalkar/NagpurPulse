-- Messaging performance indexes
--
-- Supports the newest-first, offset-paged chat history query
--   select ... from messages where conversation_id = $1 order by created_at desc range ...
-- and the per-user lookups added by 20261009102503_messaging_upgrade.sql.
-- Safe to run more than once.

create index if not exists messages_conversation_created_idx
    on public.messages (conversation_id, created_at desc);

create index if not exists conversation_prefs_user_idx
    on public.conversation_prefs (user_id);

create index if not exists message_reports_reported_user_idx
    on public.message_reports (reported_user_id, created_at desc);
