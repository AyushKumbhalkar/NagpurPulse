# New Features Setup Guide
## Messages (DM) + Anonymous Comments with Persistent Aliases

---

## Part 1 — Supabase: New Tables

Run all of the following in **Supabase Dashboard → SQL Editor**.

---

### 1.1 — `conversations` table

```sql
CREATE TABLE conversations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_one   UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    participant_two   UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    last_message      TEXT DEFAULT '',
    last_message_at   TIMESTAMPTZ DEFAULT NOW(),
    unread_count_one  INT DEFAULT 0,
    unread_count_two  INT DEFAULT 0,
    created_at        TIMESTAMPTZ DEFAULT NOW(),

    -- Prevent duplicate conversations between same two users
    CONSTRAINT unique_conversation UNIQUE (
        LEAST(participant_one::TEXT, participant_two::TEXT),
        GREATEST(participant_one::TEXT, participant_two::TEXT)
    )
);

-- Index for fast user-conversation lookup
CREATE INDEX idx_conversations_p1 ON conversations(participant_one);
CREATE INDEX idx_conversations_p2 ON conversations(participant_two);
CREATE INDEX idx_conversations_time ON conversations(last_message_at DESC);
```

---

### 1.2 — `messages` table

```sql
CREATE TABLE messages (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id   UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content           TEXT NOT NULL DEFAULT '',
    message_type      TEXT DEFAULT 'text',   -- text | image | voice
    is_read           BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Index for fast message retrieval per conversation
CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at ASC);
CREATE INDEX idx_messages_sender ON messages(sender_id);
```

---

### 1.3 — Add `anon_alias` column to `comments` table

```sql
-- Add the column to your existing comments table
ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS anon_alias TEXT;

-- Index to find all comments by same anon alias in a thread
CREATE INDEX idx_comments_anon_alias ON comments(post_id, anon_alias)
    WHERE anon_alias IS NOT NULL;
```

---

## Part 2 — Row Level Security (RLS)

### 2.1 — Conversations RLS

```sql
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;

-- Users can only see conversations they are part of
CREATE POLICY "Users see own conversations"
    ON conversations FOR SELECT
    USING (
        auth.uid() = participant_one OR
        auth.uid() = participant_two
    );

-- Users can only create conversations they are part of
CREATE POLICY "Users create own conversations"
    ON conversations FOR INSERT
    TO authenticated
    WITH CHECK (
        auth.uid() = participant_one OR
        auth.uid() = participant_two
    );

-- Users can only update conversations they are part of
-- (for updating last_message, unread_count)
CREATE POLICY "Users update own conversations"
    ON conversations FOR UPDATE
    USING (
        auth.uid() = participant_one OR
        auth.uid() = participant_two
    );
```

### 2.2 — Messages RLS

```sql
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

-- Users can only read messages in their conversations
CREATE POLICY "Users read own messages"
    ON messages FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (c.participant_one = auth.uid() OR c.participant_two = auth.uid())
        )
    );

-- Only the sender can insert messages
CREATE POLICY "Users send messages"
    ON messages FOR INSERT
    TO authenticated
    WITH CHECK (
        sender_id = auth.uid() AND
        EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = conversation_id
            AND (c.participant_one = auth.uid() OR c.participant_two = auth.uid())
        )
    );

-- Only the sender can soft-delete (update content to blank)
CREATE POLICY "Sender can delete own message"
    ON messages FOR UPDATE
    USING (sender_id = auth.uid());
```

---

## Part 3 — Realtime (for live messages)

Enable Realtime on both new tables:

1. Go to **Supabase Dashboard → Database → Replication**
2. Enable Realtime for `messages` table
3. Enable Realtime for `conversations` table

Or run:

```sql
-- Enable realtime for messages
ALTER PUBLICATION supabase_realtime ADD TABLE messages;
ALTER PUBLICATION supabase_realtime ADD TABLE conversations;
```

---

## Part 4 — Profiles: Make username searchable

Ensure your `profiles` table has an index on `username` for fast search:

```sql
-- Case-insensitive index for username search (ilike queries)
CREATE INDEX IF NOT EXISTS idx_profiles_username_search
    ON profiles USING gin(username gin_trgm_ops);

-- Enable the pg_trgm extension if not already enabled
-- (Supabase Dashboard → Database → Extensions → search "trgm" → Enable)
```

If you prefer a simpler index (without trigrams):

```sql
CREATE INDEX IF NOT EXISTS idx_profiles_username
    ON profiles(lower(username));
```

---

## Part 5 — How Anonymous Comment Aliases Work

No extra table needed! Here's the design:

```
User "abc123" comments anonymously on post "xyz789"
  → App computes: generateAnonAlias("xyz789", "abc123")
  → Always produces: "Anon_Swift_Tiger_42"   (deterministic hash)
  → Stores in comments.anon_alias = "Anon_Swift_Tiger_42"
  → Stores in comments.is_anonymous = true
  → The user_id is still stored (for moderation) but hidden from UI

Same user comments again on same post:
  → Same hash → same alias "Anon_Swift_Tiger_42"
  → Other users see it's the same person (same alias)
  → But they don't know WHO it is

Same user comments on a DIFFERENT post:
  → Different hash → different alias "Anon_Calm_Bear_7"
  → No link between their identities across threads
```

The alias is purely **per-thread identity** — consistent within one post,
completely separate in any other post.

---

## Part 6 — Verify Everything Works

After running the SQL above, test in your app:

1. **DMs**: Go to Messages tab → tap Search icon → search a username → tap Message → should open a chat
2. **Send a message**: Type in chat → tap Send → message appears immediately (optimistic) then confirms
3. **Realtime**: Open the same chat on two devices — messages should appear instantly
4. **Anonymous comment**: Open any thread → tap 🕵️ toggle in comment bar → submit comment → should show "Anon_[Adjective]_[Animal]_[N]" with orange "anon" badge
5. **Consistent alias**: Comment again anonymously on the SAME thread → same alias appears
6. **User search**: Messages tab → Search → type 2+ chars → matching usernames appear

---

## Part 7 — Optional: Block Users

To let users block each other from messaging, add:

```sql
CREATE TABLE blocked_users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id  UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    blocked_id  UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id)
);

ALTER TABLE blocked_users ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own blocks" ON blocked_users FOR ALL USING (auth.uid() = blocker_id);
```

Then update the `messages` RLS policy to also check this table.

---

## Summary — What Was Added to the App

| Feature | Files Changed |
|---------|--------------|
| DM conversations list | `MessagesScreen.kt` (new) |
| Chat / real-time messaging | `ChatScreen.kt` (new) |
| User search by username | `UserSearchScreen.kt` (new) |
| Message data model | `Message.kt` (new) |
| Message repository (Supabase) | `MessageRepository.kt` (new) |
| Anon alias model + generator | `AnonAlias.kt` (new) |
| Anon comment toggle in threads | `ThreadDetailScreen.kt` (updated) |
| Anon alias badge in comments | `CommentCard.kt` (updated) |
| addComment with anonAlias param | `PostRepository.kt` (updated) |
| Messages tab in bottom nav | `BottomNavBar.kt` (updated) |
| All routes wired | `NavGraph.kt` (updated) |
| DI registration | `AppModule.kt` (updated) |
