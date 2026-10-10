-- NagpurPulse posting-incentive algorithm
--
-- Read-only functions that help a quiet, new app get people posting:
--
--   get_post_prompts(limit)        personalised "what to post about" ideas
--   get_reply_opportunities(limit) posts that still need a first reply
--   get_posting_momentum()         streak, goals, social proof for the caller
--   get_posting_nudge()            decides IF / WHAT to nudge the caller with
--
-- Nothing here writes or changes any row. All four are SECURITY DEFINER because
-- clients cannot read posts / comments directly (RLS hardening); every personal
-- signal is keyed off auth.uid(), so a caller only ever sees their own activity.
-- Times use Asia/Kolkata (IST).

-- ───────────────────────────────────────────────────────────────────────────
-- 1. Personalised post prompts
-- ───────────────────────────────────────────────────────────────────────────
create or replace function public.get_post_prompts(p_limit integer default 5)
returns table (
  prompt_key text,
  category   text,
  title      text,
  hint       text,
  effort     text,
  reason     text,
  area_tag   text,
  score      double precision
)
language plpgsql
stable
security definer
set search_path = public, pg_catalog
as $$
declare
  v_uid         uuid    := auth.uid();
  v_limit       integer := least(greatest(coalesce(p_limit, 5), 1), 10);
  v_ist         timestamp := timezone('Asia/Kolkata', now());
  v_hour        integer := extract(hour from v_ist)::integer;
  v_weekend     boolean := extract(isodow from v_ist) in (6, 7);
  v_day         text    := to_char(v_ist, 'YYYY-MM-DD');
  v_area        text    := 'Nagpur';
  v_total_posts integer := 0;
  v_aff         jsonb   := '{}'::jsonb;
begin
  if v_uid is not null then
    select coalesce(nullif(pr.areas[1], ''), 'Nagpur') into v_area
      from public.profiles pr where pr.id = v_uid;
    v_area := coalesce(v_area, 'Nagpur');

    select count(*) into v_total_posts from public.posts where user_id = v_uid;

    with ev as (
      select lower(coalesce(nullif(p.category, ''), 'general')) as cat, 1.0 as w
        from public.votes v join public.posts p on p.id = v.post_id
       where v.user_id = v_uid and v.vote_type = 'up'
         and v.created_at > now() - interval '30 days'
      union all
      select lower(coalesce(nullif(p.category, ''), 'general')), 2.0
        from public.comments c join public.posts p on p.id = c.post_id
       where c.user_id = v_uid and not coalesce(c.is_deleted, false)
         and c.created_at > now() - interval '30 days'
      union all
      select lower(coalesce(nullif(p.category, ''), 'general')), 1.5
        from public.saved_posts s join public.posts p on p.id = s.post_id
       where s.user_id = v_uid and s.created_at > now() - interval '30 days'
    ), agg as (select cat, sum(w) as w from ev group by cat)
    select coalesce(jsonb_object_agg(cat, w / (select sum(w) from agg)), '{}'::jsonb)
      into v_aff from agg;
  end if;

  return query
  with t(key, cat, h1, h2, weekend_only, eff, title_tpl, hint_txt) as (values
    ('intro',        'community',     null::int, null::int, false, 'quick',  'Hi Nagpur! I''m new here. Here''s what I love about {area}', 'Say hello and tell people what you love about your area.'),
    ('commute_am',   'traffic',       6,  10, false, 'quick',  'How''s the traffic in {area} this morning?',              'Roads, jams, diversions. One line is enough.'),
    ('weather_now',  'weather',       6,  20, false, 'quick',  'What''s the weather like in {area} right now?',             'Rain, heat, wind: tell people what to expect.'),
    ('lunch_spot',   'food',          11, 15, false, 'quick',  'Best lunch under ₹150 in {area}?',                      'Name the place and your must-order dish.'),
    ('chai_evening', 'food',          16, 21, false, 'quick',  'Best chai or snack spot in {area} this evening?',         'Where do you go and what do you order?'),
    ('late_eats',    'nightlife',     21, 24, false, 'quick',  'Anything still open late in {area}?',                     'Food, chai, pharmacies: help the night owls.'),
    ('weekend',      'events',        null, null, true,  'normal', 'Weekend plans in Nagpur? Share an event or hangout',     'Markets, treks, meetups, movies: anything goes.'),
    ('hidden_gem',   'community',     null, null, false, 'normal', 'A hidden gem in {area} most people miss',                'A place, shop or view. Why is it special?'),
    ('rant',         'community',     null, null, false, 'quick',  'What annoys you most about {area}?',                        'Keep it honest and specific. Others probably agree.'),
    ('civic_fix',    'civic_issues',  null, null, false, 'normal', 'One thing in {area} the city should fix',                'Potholes, lights, garbage: be specific so it can be acted on.'),
    ('health_ask',   'health',        null, null, false, 'normal', 'Good doctor, clinic or gym in {area}?',                'Share your experience to help a neighbour.'),
    ('study_spot',   'education',     null, null, false, 'normal', 'Best place to study or take coaching in {area}?',      'Library, cafe, classes: what worked for you?'),
    ('sports_play',  'sports',        null, null, false, 'quick',  'Where do you play cricket, football or badminton in {area}?', 'Ground, court, timings: help people find a game.'),
    ('shopping_find','shopping',      null, null, false, 'normal', 'Your best shopping find in Nagpur so far',               'A shop, a bargain, a market lane worth knowing.'),
    ('jobs_ask',     'jobs',          null, null, false, 'normal', 'Hiring or looking for work in Nagpur?',                  'Share an opening or what you are looking for.'),
    ('tech_build',   'technology',    null, null, false, 'normal', 'Developers and makers of Nagpur: what are you building?', 'Projects, meetups, tools: connect with local builders.'),
    ('watching',     'entertainment', null, null, false, 'quick',  'What are you watching or listening to this week?',          'Movies, series, music: swap recommendations.'),
    ('safety_tip',   'safety',        null, null, false, 'normal', 'A safety tip for {area} everyone should know',           'Lighting, scams, routes: keep neighbours safe.')
  ),
  supply as (
    select lower(coalesce(nullif(p.category, ''), 'general')) as cat, count(*) as n
      from public.posts p
     where p.created_at > now() - interval '72 hours'
     group by 1
  ),
  mine as (
    select lower(coalesce(nullif(p.category, ''), 'general')) as cat, count(*) as n
      from public.posts p
     where v_uid is not null and p.user_id = v_uid
       and p.created_at > now() - interval '24 hours'
     group by 1
  ),
  scored as (
    select t.key, t.cat, t.eff, t.title_tpl, t.hint_txt,
           (   1.0
             -- right moment of the day
             + case when t.h1 is not null then 1.5 else 0.0 end
             + case when t.weekend_only then 1.5 else 0.0 end
             -- Nagpur is quiet in this category: help fill the gap
             + 2.0 * (1.0 / (1 + coalesce(s.n, 0)))
             -- the user's own interests
             + 1.5 * least(2.0 * coalesce((v_aff ->> t.cat)::float8, 0), 1.0)
             -- lower the barrier for people who have never posted
             + case when v_total_posts = 0 and t.eff = 'quick' then 1.5 else 0.0 end
             + case when t.key = 'intro' then 3.0 else 0.0 end
             -- do not repeat a category they just posted in
             - case when coalesce(m.n, 0) > 0 then 1.5 else 0.0 end
             -- stable daily rotation so the suggestions feel fresh
             + 0.6 * ((abs(hashtextextended(t.key || coalesce(v_uid::text, 'guest') || v_day, 0)) % 1000)::float8 / 1000.0)
           ) as sc,
           case
             when t.key = 'intro'                                       then 'first_post'
             when v_total_posts = 0 and t.eff = 'quick'                 then 'easy_start'
             when t.h1 is not null                                      then 'right_now'
             when t.weekend_only                                        then 'this_weekend'
             when coalesce(s.n, 0) = 0                                  then 'nobody_posted_yet'
             when coalesce((v_aff ->> t.cat)::float8, 0) >= 0.2         then 'your_interest'
             else 'fresh_idea'
           end as why
      from t
      left join supply s on s.cat = t.cat
      left join mine   m on m.cat = t.cat
     where (t.h1 is null or (v_hour >= t.h1 and v_hour < t.h2))
       and (not t.weekend_only or v_weekend)
       -- the intro idea is only for people who have not posted yet
       and (t.key <> 'intro' or v_total_posts = 0)
  ),
  best_per_cat as (
    select sc.*, row_number() over (partition by sc.cat order by sc.sc desc) as rn
      from scored sc
  )
  select b.key,
         b.cat,
         replace(b.title_tpl, '{area}', v_area),
         b.hint_txt,
         b.eff,
         b.why,
         v_area,
         b.sc::double precision
    from best_per_cat b
   where b.rn = 1
   order by b.sc desc, b.key
   limit v_limit;
end;
$$;

-- ───────────────────────────────────────────────────────────────────────────
-- 2. Reply opportunities: "be the first to reply"
--    Commenting is the easiest first step; it also gives other posters the
--    response that keeps them coming back.
-- ───────────────────────────────────────────────────────────────────────────
create or replace function public.get_reply_opportunities(p_limit integer default 10)
returns setof public.posts_public
language plpgsql
stable
security definer
set search_path = public, pg_catalog
as $$
declare
  v_uid   uuid    := auth.uid();
  v_limit integer := least(greatest(coalesce(p_limit, 10), 1), 30);
  v_areas text[]  := '{}';
  v_aff   jsonb   := '{}'::jsonb;
begin
  if v_uid is not null then
    select coalesce(array(select lower(a) from unnest(pr.areas) a), '{}')
      into v_areas from public.profiles pr where pr.id = v_uid;
    v_areas := coalesce(v_areas, '{}');

    with ev as (
      select lower(coalesce(nullif(p.category, ''), 'general')) as cat, 1.0 as w
        from public.votes v join public.posts p on p.id = v.post_id
       where v.user_id = v_uid and v.vote_type = 'up'
         and v.created_at > now() - interval '30 days'
      union all
      select lower(coalesce(nullif(p.category, ''), 'general')), 2.0
        from public.comments c join public.posts p on p.id = c.post_id
       where c.user_id = v_uid and not coalesce(c.is_deleted, false)
         and c.created_at > now() - interval '30 days'
      union all
      select lower(coalesce(nullif(p.category, ''), 'general')), 2.0
        from public.posts p
       where p.user_id = v_uid and p.created_at > now() - interval '30 days'
    ), agg as (select cat, sum(w) as w from ev group by cat)
    select coalesce(jsonb_object_agg(cat, w / (select sum(w) from agg)), '{}'::jsonb)
      into v_aff from agg;
  end if;

  return query
  with pool as (
    select pp as post, pp.id, pp.created_at,
           greatest(extract(epoch from (now() - pp.created_at)) / 3600.0, 0)::float8 as age_h,
           coalesce(pp.comment_count, 0) as c,
           pp.area_tag,
           pp.user_id as uid,
           lower(coalesce(nullif(pp.category, ''), 'general')) as cat,
           (coalesce(pp.is_alert, false) and pp.resolved_at is null
              and (pp.expires_at is null or pp.expires_at > now())) as alert_live
      from public.posts_public pp
     where coalesce(pp.comment_count, 0) <= 1
       and pp.created_at > now() - interval '72 hours'
       and not coalesce(pp.is_locked, false)
       and (v_uid is null or pp.user_id <> v_uid::text)
       and not (coalesce(pp.is_alert, false)
                and (pp.resolved_at is not null
                     or (pp.expires_at is not null and pp.expires_at < now())))
       and not exists (select 1 from public.user_blocks b
                        where b.blocker_id = v_uid and b.blocked_id::text = pp.user_id)
       and not exists (select 1 from public.comments c
                        where c.post_id = pp.id and c.user_id = v_uid
                          and not coalesce(c.is_deleted, false))
  ),
  authors as (
    select user_id::text as uid, count(*) as n from public.posts group by user_id
  ),
  scored as (
    select p.post, p.created_at, p.id,
           exp(- p.age_h / 18.0)
           * case when p.c = 0 then 1.2 else 1.0 end
           -- first-time authors most need a welcome
           * case when p.uid <> '' and coalesce(a.n, 0) <= 2 then 1.3 else 1.0 end
           * case when p.area_tag is not null and lower(p.area_tag) = any (v_areas)
                  then 1.3 else 1.0 end
           * (1.0 + 0.4 * least(2.0 * coalesce((v_aff ->> p.cat)::float8, 0), 1.0))
           * case when p.alert_live then 1.5 else 1.0 end
           as s
      from pool p
      left join authors a on a.uid = p.uid
  )
  select (sc.post).*
    from scored sc
   order by sc.s desc, sc.created_at desc, sc.id desc
   limit v_limit;
end;
$$;

-- ───────────────────────────────────────────────────────────────────────────
-- 3. Posting momentum: streak, goals and social proof for the caller
-- ───────────────────────────────────────────────────────────────────────────
create or replace function public.get_posting_momentum()
returns table (
  posts_total          integer,
  posts_7d             integer,
  comments_7d          integer,
  streak_days          integer,
  active_today         boolean,
  last_post_at         timestamptz,
  replies_received_7d  integer,
  upvotes_received_14d integer,
  posts_today          integer,
  people_posted_today  integer,
  next_goal            text,
  goal_progress        integer,
  goal_target          integer
)
language plpgsql
stable
security definer
set search_path = public, pg_catalog
as $$
declare
  v_uid     uuid := auth.uid();
  v_today   date := (timezone('Asia/Kolkata', now()))::date;
  v_start   timestamptz := (timezone('Asia/Kolkata', now()))::date::timestamp at time zone 'Asia/Kolkata';
  v_total   integer; v_p7 integer; v_c7 integer; v_streak integer := 0;
  v_active  boolean := false; v_last timestamptz;
  v_replies integer; v_ups integer; v_pt integer; v_people integer;
  v_goal text; v_prog integer; v_target integer;
begin
  if v_uid is null then
    return;
  end if;

  select count(*), count(*) filter (where created_at > now() - interval '7 days'), max(created_at)
    into v_total, v_p7, v_last
    from public.posts where user_id = v_uid;

  select count(*) into v_c7 from public.comments
   where user_id = v_uid and not coalesce(is_deleted, false)
     and created_at > now() - interval '7 days';

  -- Streak: consecutive IST days with a post OR a comment. Counts if the run
  -- reaches today or yesterday, so one quiet day never feels like a hard reset.
  with days as (
    select distinct (timezone('Asia/Kolkata', a.ts))::date as d
      from (
        select created_at as ts from public.posts where user_id = v_uid
        union all
        select created_at from public.comments
         where user_id = v_uid and not coalesce(is_deleted, false)
      ) a
     where a.ts > now() - interval '120 days'
  ), grp as (
    select d, d - (row_number() over (order by d))::integer as g from days
  ), runs as (
    select g, count(*)::integer as n, max(d) as last_d from grp group by g
  )
  select coalesce(max(n) filter (where last_d >= v_today - 1), 0),
         coalesce(bool_or(last_d = v_today), false)
    into v_streak, v_active
    from runs;

  select count(*) into v_replies
    from public.comments c join public.posts p on p.id = c.post_id
   where p.user_id = v_uid and c.user_id <> v_uid
     and not coalesce(c.is_deleted, false)
     and c.created_at > now() - interval '7 days';

  select coalesce(sum(upvotes), 0) into v_ups
    from public.posts
   where user_id = v_uid and created_at > now() - interval '14 days';

  select count(*), count(distinct user_id) into v_pt, v_people
    from public.posts where created_at >= v_start;

  if v_total = 0 then
    v_goal := 'first_post';   v_prog := 0;        v_target := 1;
  elsif v_total < 3 then
    v_goal := 'three_posts';  v_prog := v_total;  v_target := 3;
  elsif v_streak < 7 then
    v_goal := 'seven_day_streak'; v_prog := v_streak; v_target := 7;
  else
    v_goal := 'ten_posts_week';   v_prog := least(v_p7, 10); v_target := 10;
  end if;

  return query select v_total, v_p7, v_c7, v_streak, v_active, v_last,
                      v_replies, v_ups::integer, v_pt, v_people, v_goal, v_prog, v_target;
end;
$$;

-- ───────────────────────────────────────────────────────────────────────────
-- 4. Nudge decider: IF and WHAT to nudge this user with, right now.
--    The app / a scheduler asks; this never sends anything by itself.
--    Respects the user's push + community notification preferences and IST
--    quiet hours (08:00-22:00 only).
-- ───────────────────────────────────────────────────────────────────────────
create or replace function public.get_posting_nudge()
returns table (
  should_nudge boolean,
  kind         text,
  title        text,
  body         text,
  category     text,
  area_tag     text
)
language plpgsql
stable
security definer
set search_path = public, pg_catalog
as $$
declare
  v_uid      uuid := auth.uid();
  v_hour     integer := extract(hour from timezone('Asia/Kolkata', now()))::integer;
  v_ok       boolean := true;
  v_created  timestamptz;
  v_last_act timestamptz;
  v_waiting  integer := 0;
  m          record;
  pr         record;
  v_kind     text := 'none';
  v_title    text; v_body text;
begin
  if v_uid is null then
    return query select false, 'none', null::text, null::text, null::text, null::text;
    return;
  end if;

  select coalesce(up.notif_push, true) and coalesce(up.notif_community, true)
    into v_ok from public.user_preferences up where up.user_id = v_uid;
  v_ok := coalesce(v_ok, true);

  select created_at into v_created from public.profiles where id = v_uid;

  select max(ts) into v_last_act from (
    select created_at as ts from public.posts where user_id = v_uid
    union all
    select created_at from public.comments
     where user_id = v_uid and not coalesce(is_deleted, false)
  ) a;

  -- People replied to my post in the last 24h and I have not answered since.
  select count(*) into v_waiting
    from public.comments c join public.posts p on p.id = c.post_id
   where p.user_id = v_uid and c.user_id <> v_uid
     and not coalesce(c.is_deleted, false)
     and c.created_at > now() - interval '24 hours'
     and not exists (select 1 from public.comments mine
                      where mine.post_id = c.post_id and mine.user_id = v_uid
                        and mine.created_at > c.created_at
                        and not coalesce(mine.is_deleted, false));

  select * into m  from public.get_posting_momentum();
  select * into pr from public.get_post_prompts(1);

  if v_waiting > 0 then
    v_kind  := 'reply_back';
    v_title := case when v_waiting = 1 then 'Someone replied to your post'
                    else v_waiting || ' people replied to your post' end;
    v_body  := 'Jump back into the conversation.';
  elsif coalesce(m.posts_total, 0) = 0
        and v_created is not null and v_created < now() - interval '1 day' then
    v_kind  := 'first_post';
    v_title := 'Your first post on NagpurPulse';
    v_body  := coalesce(pr.title, 'Say hello to your neighbours.');
  elsif coalesce(m.streak_days, 0) >= 3 and not coalesce(m.active_today, false) then
    v_kind  := 'streak';
    v_title := 'Keep your ' || m.streak_days || '-day streak going';
    v_body  := coalesce(pr.title, 'Share a quick thought or reply to a post.');
  elsif coalesce(m.posts_total, 0) > 0
        and (v_last_act is null or v_last_act < now() - interval '3 days') then
    v_kind  := 'comeback';
    v_title := 'Nagpur misses your voice';
    v_body  := coalesce(pr.title, 'See what your neighbours are talking about.');
  end if;

  return query select
    (v_ok and v_hour >= 8 and v_hour < 22 and v_kind <> 'none'),
    case when v_ok and v_hour >= 8 and v_hour < 22 then v_kind else 'none' end,
    v_title, v_body, pr.category, pr.area_tag;
end;
$$;

-- ── Permissions ─────────────────────────────────────────────────────────────
revoke all on function public.get_post_prompts(integer)        from public;
revoke all on function public.get_reply_opportunities(integer) from public;
revoke all on function public.get_posting_momentum()           from public;
revoke all on function public.get_posting_nudge()              from public;

-- Guests may browse ideas and reply opportunities (they hit the login prompt on action).
grant execute on function public.get_post_prompts(integer)        to anon, authenticated;
grant execute on function public.get_reply_opportunities(integer) to anon, authenticated;
-- Personal functions: signed-in users only.
-- Supabase also grants EXECUTE to anon directly, so revoking from PUBLIC is not enough.
revoke all on function public.get_posting_momentum() from anon;
revoke all on function public.get_posting_nudge()    from anon;
grant execute on function public.get_posting_momentum() to authenticated;
grant execute on function public.get_posting_nudge()    to authenticated;
