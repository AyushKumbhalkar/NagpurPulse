-- ═══════════════════════════════════════════════════════════════════
-- NAGPUR PULSE — Full Supabase Schema
-- Run this entire file in Supabase SQL Editor → Run
-- ═══════════════════════════════════════════════════════════════════

-- ── Core tables ────────────────────────────────────────────────────

create table if not exists profiles (
  id          uuid references auth.users primary key,
  username    text unique not null,
  tagline     text,
  avatar_url  text,
  areas       text[] default '{}',
  karma       integer default 0,
  created_at  timestamptz default now()
);

create table if not exists posts (
  id             uuid default gen_random_uuid() primary key,
  user_id        uuid references profiles(id) on delete cascade,
  title          text not null,
  body           text,
  category       text not null check (category in (
                   'food','nightlife','jobs','college','rants',
                   'neighborhoods','lost_found','events','traffic','alerts')),
  area_tag       text,
  is_anonymous   boolean default false,
  upvotes        integer default 0,
  downvotes      integer default 0,
  comment_count  integer default 0,
  view_count     integer default 0,
  image_url      text,
  is_alert       boolean default false,
  alert_severity text check (alert_severity in ('critical','high','medium','low')),
  created_at     timestamptz default now()
);

create table if not exists comments (
  id           uuid default gen_random_uuid() primary key,
  post_id      uuid references posts(id) on delete cascade,
  user_id      uuid references profiles(id) on delete cascade,
  parent_id    uuid references comments(id),
  body         text not null,
  upvotes      integer default 0,
  is_anonymous boolean default false,
  created_at   timestamptz default now()
);

create table if not exists votes (
  id         uuid default gen_random_uuid() primary key,
  user_id    uuid references profiles(id) on delete cascade,
  post_id    uuid references posts(id) on delete cascade,
  vote_type  text check (vote_type in ('up','down')),
  unique(user_id, post_id)
);

create table if not exists notifications (
  id               uuid default gen_random_uuid() primary key,
  user_id          uuid references profiles(id) on delete cascade,
  type             text not null,
  title            text not null,
  body             text,
  is_read          boolean default false,
  related_post_id  uuid references posts(id) on delete set null,
  created_at       timestamptz default now()
);

create table if not exists badges (
  id          uuid default gen_random_uuid() primary key,
  user_id     uuid references profiles(id) on delete cascade,
  badge_type  text not null,
  earned_at   timestamptz default now(),
  unique(user_id, badge_type)
);

-- ── NEW: Saved posts ────────────────────────────────────────────────
create table if not exists saved_posts (
  id         uuid default gen_random_uuid() primary key,
  user_id    uuid references profiles(id) on delete cascade,
  post_id    uuid references posts(id) on delete cascade,
  created_at timestamptz default now(),
  unique(user_id, post_id)
);

-- ── NEW: FCM device tokens ─────────────────────────────────────────
create table if not exists device_tokens (
  id         uuid default gen_random_uuid() primary key,
  user_id    uuid references profiles(id) on delete cascade unique,
  fcm_token  text not null,
  updated_at timestamptz default now()
);

-- ── Indexes for performance ────────────────────────────────────────
create index if not exists idx_posts_user_id    on posts(user_id);
create index if not exists idx_posts_category   on posts(category);
create index if not exists idx_posts_is_alert   on posts(is_alert);
create index if not exists idx_posts_created_at on posts(created_at desc);
create index if not exists idx_comments_post_id on comments(post_id);
create index if not exists idx_votes_post_id    on votes(post_id);
create index if not exists idx_notif_user_id    on notifications(user_id);
create index if not exists idx_notif_is_read    on notifications(is_read);
create index if not exists idx_saved_user_id    on saved_posts(user_id);

-- ── Enable Realtime on posts ───────────────────────────────────────
-- In Dashboard → Database → Replication, also toggle 'posts' ON

-- ── Row Level Security ─────────────────────────────────────────────
alter table profiles      enable row level security;
alter table posts         enable row level security;
alter table comments      enable row level security;
alter table votes         enable row level security;
alter table notifications enable row level security;
alter table badges        enable row level security;
alter table saved_posts   enable row level security;
alter table device_tokens enable row level security;

-- Profiles
create policy "Public profiles viewable by everyone"
  on profiles for select using (true);
create policy "Users can insert own profile"
  on profiles for insert with check (auth.uid() = id);
create policy "Users can update own profile"
  on profiles for update using (auth.uid() = id);

-- Posts
create policy "Posts viewable by everyone"
  on posts for select using (true);
create policy "Auth users can insert posts"
  on posts for insert with check (auth.uid() = user_id);
create policy "Auth users can update own posts"
  on posts for update using (auth.uid() = user_id);
create policy "System can update post counters"
  on posts for update using (true);   -- allows comment_count/upvote increments

-- Comments
create policy "Comments viewable by everyone"
  on comments for select using (true);
create policy "Auth users can insert comments"
  on comments for insert with check (auth.uid() = user_id);
create policy "System can update comment upvotes"
  on comments for update using (true);

-- Votes
create policy "Users can manage own votes"
  on votes for all using (auth.uid() = user_id);

-- Notifications
create policy "Users can view own notifications"
  on notifications for select using (auth.uid() = user_id);
create policy "System can insert notifications"
  on notifications for insert with check (true);
create policy "Users can update own notifications"
  on notifications for update using (auth.uid() = user_id);

-- Badges
create policy "Badges viewable by everyone"
  on badges for select using (true);
create policy "System can insert badges"
  on badges for insert with check (true);

-- Saved posts
create policy "Users can manage own saved posts"
  on saved_posts for all using (auth.uid() = user_id);

-- Device tokens
create policy "Users can manage own device tokens"
  on device_tokens for all using (auth.uid() = user_id);

-- ── Seed data (optional) ───────────────────────────────────────────
-- First create a user via Auth → Users → Add User
-- Then get their UUID and run:
--
-- insert into profiles (id, username, tagline, areas, karma) values
--   ('YOUR-UUID-HERE', 'NagpurFoodie', 'Nagpur ke swaad ka expert 🍜', '{Dharampeth,Sadar}', 420);
--
-- insert into posts (user_id, title, body, category, area_tag, upvotes, comment_count) values
--   ('YOUR-UUID-HERE', 'Best momos in Nagpur — the definitive list', 'Dharampeth market wins.', 'food', 'Dharampeth', 234, 47),
--   ('YOUR-UUID-HERE', 'Wardha Road NIGHTMARE right now 🚨', 'Massive jam near Bajaj Nagar.', 'traffic', 'Wardha Road', 312, 28),
--   ('YOUR-UUID-HERE', 'Auto drivers price gouging again 😤', 'Rs 150 for 3km. Enough.', 'rants', 'Sitabuldi', 567, 134),
--   ('YOUR-UUID-HERE', 'Power cut in Manish Nagar — 4th time this week ⚡', 'MSEDCL not responding.', 'alerts', 'Manish Nagar', 156, 42);
