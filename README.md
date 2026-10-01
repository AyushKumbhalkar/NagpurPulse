# 🏙️ Nagpur Pulse

> **Nagpur ki awaaz. Ab ek jagah.**  
> A hyperlocal community forum app for Nagpur city, India.

Built with **Kotlin + Jetpack Compose + Supabase**.

---

## 📱 Screens

| Screen | Description |
|--------|-------------|
| Onboarding | Welcome screen with city illustration |
| Sign Up / Login | Supabase Auth with email & password |
| Area Selection | Pick up to 3 Nagpur neighbourhoods |
| Home Feed | Infinite-scroll posts sorted by Top / New / Hot |
| Explore | Category grid + search + trending cards |
| Thread Detail | Full post view with comments |
| Create Thread | Post with category, area tag, anonymous mode |
| Live Alerts | Real-time Supabase Realtime feed |
| Notifications | Activity feed with read/unread states |
| Profile | Karma, badges, tabs for threads & comments |

---

## 🚀 Quick Start

### 1. Create a Supabase Project

1. Go to [https://app.supabase.com](https://app.supabase.com) and create a free project.
2. Once created, go to **Settings → API**.
3. Copy your **Project URL** and **anon public key**.

---

### 2. Set Up the Database

Open the **SQL Editor** in your Supabase dashboard and run the following:

```sql
-- PROFILES
create table profiles (
  id uuid references auth.users primary key,
  username text unique not null,
  tagline text,
  avatar_url text,
  areas text[] default '{}',
  karma integer default 0,
  created_at timestamptz default now()
);

-- POSTS
create table posts (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references profiles(id),
  title text not null,
  body text,
  category text not null check (
    category in ('food','nightlife','jobs','college','rants',
                 'neighborhoods','lost_found','events','traffic','alerts')
  ),
  area_tag text,
  is_anonymous boolean default false,
  upvotes integer default 0,
  downvotes integer default 0,
  comment_count integer default 0,
  view_count integer default 0,
  image_url text,
  is_alert boolean default false,
  alert_severity text check (alert_severity in ('critical','high','medium','low')),
  created_at timestamptz default now()
);

-- COMMENTS
create table comments (
  id uuid default gen_random_uuid() primary key,
  post_id uuid references posts(id) on delete cascade,
  user_id uuid references profiles(id),
  parent_id uuid references comments(id),
  body text not null,
  upvotes integer default 0,
  is_anonymous boolean default false,
  created_at timestamptz default now()
);

-- VOTES
create table votes (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references profiles(id),
  post_id uuid references posts(id) on delete cascade,
  vote_type text check (vote_type in ('up','down')),
  unique(user_id, post_id)
);

-- NOTIFICATIONS
create table notifications (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references profiles(id),
  type text not null,
  title text not null,
  body text,
  is_read boolean default false,
  related_post_id uuid references posts(id),
  created_at timestamptz default now()
);

-- BADGES
create table badges (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references profiles(id),
  badge_type text not null,
  earned_at timestamptz default now()
);
```

---

### 3. Enable Realtime

In Supabase dashboard → **Database → Replication**, enable the `posts` table for Realtime.

---

### 4. Seed Sample Data

```sql
-- First create a test user via Auth → Users → Invite User
-- Then note their UUID and run:

insert into profiles (id, username, tagline, areas, karma)
values
  ('YOUR-USER-UUID', 'NagpurFoodie', 'Foodie by heart 🍜', '{Dharampeth,Sadar}', 420);

-- Sample posts
insert into posts (user_id, title, body, category, area_tag, upvotes, comment_count, is_alert)
values
  ('YOUR-USER-UUID', 'Best momos in Nagpur — the definitive list 🥟', 'Tried them all so you don''t have to. Dharampeth market wins hands down.', 'food', 'Dharampeth', 234, 47, false),
  ('YOUR-USER-UUID', 'Chai tapri near VNIT that hits different at 2 AM', 'You know the one. Outside the south gate. ₹8 chai, priceless views.', 'food', 'VNIT Area', 189, 31, false),
  ('YOUR-USER-UUID', 'Wardha Road traffic NIGHTMARE right now 🚨', 'Massive jam near Bajaj Nagar flyover. Accident reported. Avoid.', 'traffic', 'Wardha Road', 312, 28, true),
  ('YOUR-USER-UUID', 'Power cut in Manish Nagar — 4th time this week ⚡', 'MSEDCL helpline not responding. Anyone else?', 'alerts', 'Manish Nagar', 156, 42, true),
  ('YOUR-USER-UUID', 'VNIT Techfest 2024 dates announced 🎓', 'March 15-17. Registrations open now. This year theme is AI & Robotics.', 'college', 'VNIT Area', 445, 67, false),
  ('YOUR-USER-UUID', 'YCCE placements 2024 — worst season ever?', 'Only 60% placed so far. Last year was 85%. What happened?', 'college', 'Wardha Road', 278, 89, false),
  ('YOUR-USER-UUID', 'Auto drivers in Nagpur are absolutely shameless', 'Asked for ₹150 for a 3 km ride. Meter shows ₹40. Enough is enough.', 'rants', 'Sitabuldi', 567, 134, false),
  ('YOUR-USER-UUID', 'Every road in Trimurti Nagar is a pothole with tarmac around it', 'My Activa alignment is gone. Who do we complain to?', 'rants', 'Trimurti Nagar', 891, 203, false),
  ('YOUR-USER-UUID', 'Nagpur Food Festival coming to Futala Lake this weekend 🎉', 'Over 50 stalls, live music, 7 PM onwards. Entry free!', 'events', 'Pratap Nagar', 334, 56, false),
  ('YOUR-USER-UUID', 'Rooftop bars in Nagpur — does Nightlife even exist here? 🌙', 'Genuinely asking. Every place closes by 11 PM.', 'nightlife', 'Sadar', 445, 88, false),
  ('YOUR-USER-UUID', 'Lost: Black wallet near Sitabuldi market 🔍', 'Lost yesterday evening around 6 PM near the main crossing. Has ID cards. Please contact if found. Reward: ₹500.', 'lost_found', 'Sitabuldi', 67, 12, false),
  ('YOUR-USER-UUID', 'Hiring: Android Developer at Nagpur startup 💼', 'Early stage startup, good comp, work from Dharampeth office. 1-3 yrs exp. DM me.', 'jobs', 'Dharampeth', 145, 34, false),
  ('YOUR-USER-UUID', 'Sitabuldi checkpost — heavy police presence 🚔', 'Multiple vehicles being stopped. Carry documents.', 'traffic', 'Sitabuldi', 89, 15, true),
  ('YOUR-USER-UUID', 'Best gym under ₹1000/month in Nagpur?', 'Moved to Bajaj Nagar recently. Looking for good gym. Any recommendations?', 'neighborhoods', 'Bajaj Nagar', 123, 45, false),
  ('YOUR-USER-UUID', 'Overrated cafes in Nagpur — an honest review', 'Some of these Instagram-famous places charge Delhi prices for Nagpur service. Thread below.', 'food', 'Civil Lines', 678, 156, false);
```

---

### 5. Configure Row Level Security (RLS)

```sql
-- Enable RLS
alter table profiles enable row level security;
alter table posts enable row level security;
alter table comments enable row level security;
alter table votes enable row level security;
alter table notifications enable row level security;
alter table badges enable row level security;

-- Profiles: public read, owner write
create policy "Public profiles are viewable by everyone" on profiles for select using (true);
create policy "Users can update own profile" on profiles for update using (auth.uid() = id);

-- Posts: public read, auth write
create policy "Posts are viewable by everyone" on posts for select using (true);
create policy "Authenticated users can insert posts" on posts for insert with check (auth.uid() = user_id);

-- Comments: public read, auth write
create policy "Comments are viewable by everyone" on comments for select using (true);
create policy "Authenticated users can insert comments" on comments for insert with check (auth.uid() = user_id);

-- Votes: auth only
create policy "Users can manage their own votes" on votes for all using (auth.uid() = user_id);

-- Notifications: owner only
create policy "Users can view own notifications" on notifications for select using (auth.uid() = user_id);
create policy "Users can update own notifications" on notifications for update using (auth.uid() = user_id);

-- Badges: public read
create policy "Badges are viewable by everyone" on badges for select using (true);
```

---

### 6. Add Credentials to the App

Edit `local.properties` in the project root:

```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
SUPABASE_URL=https://YOUR_PROJECT_ID.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_KEY_HERE
```

> ⚠️ `local.properties` is git-ignored. Never commit your credentials.

---

### 7. Run the App

1. Open the project in **Android Studio Hedgehog** or newer.
2. Let Gradle sync complete.
3. Select a device (API 26+ emulator or physical device).
4. Click **▶ Run**.

---

## 🏗️ Architecture

```
MVVM + Jetpack Compose + Hilt
├── UI Layer        →  @Composable screens + ViewModels
├── Domain Layer    →  Repositories (single source of truth)
└── Data Layer      →  Supabase SDK (Auth, Postgrest, Realtime, Storage)
```

### State Management
All ViewModels expose a single `StateFlow<UiState>` collected with `collectAsState()` in Compose.

### Navigation
Single `NavHost` in `NavGraph.kt`. Bottom nav handled by `BottomNavBar.kt` composable.

---

## 🎨 Design Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `BackgroundDark` | `#0D0D0D` | Screen backgrounds |
| `CardDark` | `#1A1A1A` | Cards, surfaces |
| `OrangePrimary` | `#FF6B00` | CTAs, accents, selected states |
| `RedAlert` | `#FF3B30` | Alert cards, emergency |
| `TextPrimary` | `#FFFFFF` | Main text |
| `TextSecondary` | `#9CA3AF` | Subtitles, metadata |

---

## 📦 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Navigation | Navigation Compose |
| Backend | Supabase |
| Auth | Supabase Auth |
| Database | Supabase PostgreSQL |
| Realtime | Supabase Realtime |
| Storage | Supabase Storage |
| HTTP Client | Ktor (Android) |
| Images | Coil |
| DI | Hilt |
| Async | Coroutines + Flow |

---

## 🐛 Troubleshooting

**Build fails with `SUPABASE_URL` not found**  
→ Make sure `local.properties` exists and has both keys filled in.

**Posts not loading**  
→ Check RLS policies are applied. Try disabling RLS temporarily to test.

**Auth signup fails**  
→ Enable Email provider in Supabase Dashboard → Authentication → Providers.

**Realtime not working**  
→ In Supabase → Database → Replication, toggle on the `posts` table.

---

## 📄 License

MIT — build something great for Nagpur! 🧡
