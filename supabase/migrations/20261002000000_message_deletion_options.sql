-- Per-user hiding for conversations and individual messages.
-- Apply this migration in the Supabase SQL Editor before testing the new options.
create table if not exists public.conversation_hidden_for_users (
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (conversation_id, user_id)
);

create table if not exists public.message_hidden_for_users (
    message_id uuid not null references public.messages(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (message_id, user_id)
);

alter table public.conversation_hidden_for_users enable row level security;
alter table public.message_hidden_for_users enable row level security;

drop policy if exists "Users manage own hidden conversations" on public.conversation_hidden_for_users;
create policy "Users manage own hidden conversations"
on public.conversation_hidden_for_users for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists "Users manage own hidden messages" on public.message_hidden_for_users;
create policy "Users manage own hidden messages"
on public.message_hidden_for_users for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);
