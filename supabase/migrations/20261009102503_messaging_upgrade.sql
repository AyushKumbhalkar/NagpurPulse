-- Messaging upgrade
--
-- Adds: user blocking (enforced server-side), message reports, per-user mute / pin,
-- reply-to, "edited" timestamp, emoji reactions, "last message sender" for inbox
-- previews, and a single atomic send_message() RPC (replaces the app's previous
-- insert + read + update round-trips and fixes the unread-counter race).
--
-- Safe to run more than once. Apply in the Supabase SQL Editor (or `supabase db push`).
-- The Android app degrades gracefully if this has not been applied yet, but blocking,
-- reactions, replies, mute/pin and "You:" previews only work after it is.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. New columns
-- ─────────────────────────────────────────────────────────────────────────────
alter table public.messages
    add column if not exists reply_to_id uuid references public.messages(id) on delete set null,
    add column if not exists edited_at   timestamptz,
    add column if not exists reactions   jsonb not null default '{}'::jsonb;

alter table public.conversations
    add column if not exists last_message_sender uuid;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Blocking
-- ─────────────────────────────────────────────────────────────────────────────
create table if not exists public.user_blocks (
    blocker_id uuid not null references auth.users(id) on delete cascade,
    blocked_id uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (blocker_id, blocked_id),
    check (blocker_id <> blocked_id)
);

alter table public.user_blocks enable row level security;

drop policy if exists "Users manage own blocks" on public.user_blocks;
create policy "Users manage own blocks"
on public.user_blocks for all
using (auth.uid() = blocker_id)
with check (auth.uid() = blocker_id);

-- Reject any message between two users when either has blocked the other.
-- SECURITY DEFINER so it can see the *other* person's block row without exposing it
-- to clients. The error text is generic on purpose (the blocked user is not told why).
create or replace function public.enforce_message_block()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    p1 uuid;
    p2 uuid;
begin
    select participant_one, participant_two
      into p1, p2
      from public.conversations
     where id = new.conversation_id;

    if p1 is null then
        return new;
    end if;

    if exists (
        select 1
          from public.user_blocks b
         where (b.blocker_id = p1 and b.blocked_id = p2)
            or (b.blocker_id = p2 and b.blocked_id = p1)
    ) then
        raise exception 'message_blocked';
    end if;

    return new;
end;
$$;

drop trigger if exists messages_enforce_block on public.messages;
create trigger messages_enforce_block
before insert on public.messages
for each row execute function public.enforce_message_block();

revoke all on function public.enforce_message_block() from public, anon, authenticated;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Reports
-- ─────────────────────────────────────────────────────────────────────────────
create table if not exists public.message_reports (
    id               uuid primary key default gen_random_uuid(),
    reporter_id      uuid not null references auth.users(id) on delete cascade,
    reported_user_id uuid references auth.users(id) on delete set null,
    conversation_id  uuid references public.conversations(id) on delete set null,
    message_id       uuid references public.messages(id) on delete set null,
    reason           text not null check (char_length(reason) between 1 and 80),
    details          text check (details is null or char_length(details) <= 1000),
    created_at       timestamptz not null default now()
);

alter table public.message_reports enable row level security;

drop policy if exists "Users file own message reports" on public.message_reports;
create policy "Users file own message reports"
on public.message_reports for insert
with check (auth.uid() = reporter_id);

drop policy if exists "Users read own message reports" on public.message_reports;
create policy "Users read own message reports"
on public.message_reports for select
using (auth.uid() = reporter_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Per-user conversation preferences (mute / pin)
-- ─────────────────────────────────────────────────────────────────────────────
create table if not exists public.conversation_prefs (
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    user_id         uuid not null references auth.users(id) on delete cascade,
    is_muted        boolean not null default false,
    is_pinned       boolean not null default false,
    updated_at      timestamptz not null default now(),
    primary key (conversation_id, user_id)
);

alter table public.conversation_prefs enable row level security;

drop policy if exists "Users manage own conversation prefs" on public.conversation_prefs;
create policy "Users manage own conversation prefs"
on public.conversation_prefs for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. Atomic send
-- ─────────────────────────────────────────────────────────────────────────────
-- One round-trip: insert the message, update the conversation preview and bump the
-- OTHER participant's unread counter in a single UPDATE (no read-modify-write race).
-- SECURITY INVOKER: the caller's RLS still applies. The block trigger runs on insert.
create or replace function public.send_message(
    p_conversation_id uuid,
    p_content         text,
    p_reply_to        uuid default null
)
returns public.messages
language plpgsql
security invoker
set search_path = public
as $$
declare
    me  uuid := auth.uid();
    msg public.messages;
begin
    if me is null then
        raise exception 'not_authenticated';
    end if;

    if p_content is null or char_length(btrim(p_content)) = 0 then
        raise exception 'empty_message';
    end if;

    if char_length(p_content) > 4000 then
        raise exception 'message_too_long';
    end if;

    if not exists (
        select 1
          from public.conversations c
         where c.id = p_conversation_id
           and (c.participant_one = me or c.participant_two = me)
    ) then
        raise exception 'conversation_not_found';
    end if;

    insert into public.messages (conversation_id, sender_id, content, message_type, is_read, reply_to_id)
    values (p_conversation_id, me, p_content, 'text', false, p_reply_to)
    returning * into msg;

    update public.conversations c
       set last_message        = p_content,
           last_message_at     = msg.created_at,
           last_message_sender = me,
           unread_count_one    = case when c.participant_one = me then c.unread_count_one else c.unread_count_one + 1 end,
           unread_count_two    = case when c.participant_two = me then c.unread_count_two else c.unread_count_two + 1 end
     where c.id = p_conversation_id;

    return msg;
end;
$$;

revoke all on function public.send_message(uuid, text, uuid) from public, anon;
grant execute on function public.send_message(uuid, text, uuid) to authenticated;

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. Reactions (one per user per message; tapping the same emoji again removes it)
-- ─────────────────────────────────────────────────────────────────────────────
-- Stored on the message row, so the existing realtime UPDATE subscription delivers
-- reaction changes to both people with no extra channel.
create or replace function public.toggle_message_reaction(
    p_message_id uuid,
    p_emoji      text
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
    me        uuid := auth.uid();
    cur       jsonb;
    had_same  boolean;
    cleaned   jsonb;
    result    jsonb;
begin
    if me is null then
        raise exception 'not_authenticated';
    end if;

    if p_emoji is null or char_length(p_emoji) = 0 or char_length(p_emoji) > 8 then
        raise exception 'invalid_reaction';
    end if;

    -- Lock the row and make sure the caller is a participant of its conversation.
    select m.reactions
      into cur
      from public.messages m
      join public.conversations c on c.id = m.conversation_id
     where m.id = p_message_id
       and (c.participant_one = me or c.participant_two = me)
       for update of m;

    if not found then
        raise exception 'not_allowed';
    end if;

    cur := coalesce(cur, '{}'::jsonb);
    had_same := coalesce(cur -> p_emoji, '[]'::jsonb) ? me::text;

    -- Remove this user from every emoji, dropping emojis that end up empty.
    select coalesce(jsonb_object_agg(t.k, t.v), '{}'::jsonb)
      into cleaned
      from (
          select e.key as k,
                 (select coalesce(jsonb_agg(u.val), '[]'::jsonb)
                    from jsonb_array_elements_text(e.value) as u(val)
                   where u.val <> me::text) as v
            from jsonb_each(cur) as e
      ) t
     where jsonb_array_length(t.v) > 0;

    if had_same then
        result := cleaned;
    else
        result := jsonb_set(
            cleaned,
            array[p_emoji],
            coalesce(cleaned -> p_emoji, '[]'::jsonb) || to_jsonb(me::text),
            true
        );
    end if;

    update public.messages set reactions = result where id = p_message_id;

    return result;
end;
$$;

revoke all on function public.toggle_message_reaction(uuid, text) from public, anon;
grant execute on function public.toggle_message_reaction(uuid, text) to authenticated;

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. Muted chats must not create notifications / pushes
-- ─────────────────────────────────────────────────────────────────────────────
-- Same function as 20261002150000_server_authoritative_notifications.sql, plus one
-- extra check: skip the notification when the recipient has muted this conversation.
create or replace function public.notify_message_insert()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
declare
    participant_one_id uuid;
    participant_two_id uuid;
    recipient_id uuid;
    sender_name text;
begin
    if new.sender_id is null or new.conversation_id is null then
        return new;
    end if;

    select c.participant_one, c.participant_two
      into participant_one_id, participant_two_id
      from public.conversations c
     where c.id = new.conversation_id;

    if participant_one_id is null or participant_two_id is null then
        return new;
    end if;

    if new.sender_id = participant_one_id then
        recipient_id := participant_two_id;
    elsif new.sender_id = participant_two_id then
        recipient_id := participant_one_id;
    else
        -- Do not create notifications for messages from non-participants.
        return new;
    end if;

    if not coalesce((
        select up.notif_messages from public.user_preferences up
         where up.user_id = recipient_id
    ), true) then
        return new;
    end if;

    -- NEW: respect the recipient's per-chat mute.
    if coalesce((
        select cp.is_muted from public.conversation_prefs cp
         where cp.conversation_id = new.conversation_id
           and cp.user_id = recipient_id
    ), false) then
        return new;
    end if;

    select username into sender_name from public.profiles where id = new.sender_id;
    perform public.create_notification_event(
        recipient_id, 'message',
        coalesce(sender_name, 'Someone') || ' sent you a message',
        left(coalesce(new.content, ''), 120), null, new.conversation_id, new.sender_id
    );

    return new;
end;
$$;

revoke all on function public.notify_message_insert() from public, anon, authenticated;
