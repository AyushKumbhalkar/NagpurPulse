-- NagpurPulse feed ranking algorithm
--
-- Additive only: creates indexes and one RPC. It does NOT read-modify any post,
-- comment, vote or profile row, so it is safe to apply while the data is still
-- test / seed content.
--
-- get_feed(p_mode, p_category, p_limit, p_offset)
--   p_mode = 'foryou' (default) | 'hot' | 'top' | 'new'
--
-- 'foryou'  personalised, freshness-decayed, built for a low-content new app
-- 'hot'     same velocity score, but identical for everyone (no personalisation)
-- 'top'     net votes, then comments, then newest (deterministic tie-breaks)
-- 'new'     newest first
--
-- Reads from public.posts_public so anonymous-author masking and deactivated
-- account filtering stay exactly as they are today.
--
-- SECURITY DEFINER because direct SELECT on public.posts / comments is revoked
-- for clients (see the RLS hardening migrations). The function only ever uses
-- auth.uid() for personal signals (the caller's own votes, comments, saves and
-- profile areas), so it cannot be used to read anyone else's activity. EXECUTE
-- is revoked from PUBLIC and granted to anon + authenticated only.

create index if not exists posts_created_at_idx
  on public.posts (created_at desc);
create index if not exists posts_user_created_idx
  on public.posts (user_id, created_at desc);
create index if not exists posts_category_created_idx
  on public.posts (lower(category), created_at desc);

create or replace function public.get_feed(
  p_mode     text    default 'foryou',
  p_category text    default null,
  p_limit    integer default 20,
  p_offset   integer default 0
)
returns setof public.posts_public
language plpgsql
stable
security definer
set search_path = public, pg_catalog
as $$
declare
  v_uid      uuid    := auth.uid();
  v_mode     text    := lower(coalesce(nullif(btrim(p_mode), ''), 'foryou'));
  v_limit    integer := least(greatest(coalesce(p_limit, 20), 1), 50);
  v_offset   integer := greatest(coalesce(p_offset, 0), 0);
  v_cat      text    := lower(nullif(btrim(p_category), ''));
  v_areas    text[]  := '{}';
  v_aff      jsonb   := '{}'::jsonb;
  v_day      text    := to_char(timezone('Asia/Kolkata', now()), 'YYYY-MM-DD');
  v_personal boolean;
begin
  if v_mode not in ('foryou', 'hot', 'top', 'new') then
    v_mode := 'foryou';
  end if;
  v_personal := (v_mode = 'foryou');

  -- ── Newest first ───────────────────────────────────────────────────────────
  if v_mode = 'new' then
    return query
      select pp.*
        from public.posts_public pp
       where (v_cat is null or lower(pp.category) = v_cat)
         and not exists (select 1 from public.user_blocks b
                          where b.blocker_id = v_uid and b.blocked_id::text = pp.user_id)
       order by pp.created_at desc, pp.id desc
       limit v_limit offset v_offset;
    return;
  end if;

  -- ── Top: net votes, then discussion, then recency (stable tie-breaks) ─────
  if v_mode = 'top' then
    return query
      select pp.*
        from public.posts_public pp
       where (v_cat is null or lower(pp.category) = v_cat)
         and not exists (select 1 from public.user_blocks b
                          where b.blocker_id = v_uid and b.blocked_id::text = pp.user_id)
       order by (coalesce(pp.upvotes, 0) - coalesce(pp.downvotes, 0)) desc,
                coalesce(pp.comment_count, 0) desc,
                pp.created_at desc,
                pp.id desc
       limit v_limit offset v_offset;
    return;
  end if;

  -- ── Personalisation inputs (For You, signed-in users only) ────────────────
  if v_personal and v_uid is not null then
    select coalesce(array(select lower(a) from unnest(pr.areas) a), '{}')
      into v_areas
      from public.profiles pr
     where pr.id = v_uid;
    v_areas := coalesce(v_areas, '{}');

    -- Category interests from the user's last 30 days: upvotes, comments,
    -- own posts and saves. Result: category -> share of total interest (0..1).
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
      union all
      select lower(coalesce(nullif(p.category, ''), 'general')), 1.5
        from public.saved_posts s join public.posts p on p.id = s.post_id
       where s.user_id = v_uid and s.created_at > now() - interval '30 days'
    ), agg as (
      select cat, sum(w) as w from ev group by cat
    )
    select coalesce(jsonb_object_agg(cat, w / (select sum(w) from agg)), '{}'::jsonb)
      into v_aff
      from agg;
  end if;

  -- ── For You / Hot scoring ─────────────────────────────────────────────────
  return query
  with pool as (
    -- Candidate pool: newest 600 visible posts. Keeps the query cheap as the
    -- app grows; older posts would have decayed to the floor anyway.
    select pp as post,
           pp.id,
           pp.user_id                                              as uid,
           case when pp.user_id = '' then 'anon:' || pp.id::text
                else pp.user_id end                                as author_key,
           greatest(extract(epoch from (now() - pp.created_at)) / 3600.0, 0)::float8 as age_h,
           coalesce(pp.comment_count, 0)                           as c,
           coalesce(pp.upvotes, 0)                                 as u,
           coalesce(pp.downvotes, 0)                               as d,
           coalesce(pp.confirm_count, 0)                           as cf,
           coalesce(pp.view_count, 0)                              as vw,
           pp.area_tag,
           pp.created_at,
           lower(coalesce(nullif(pp.category, ''), 'general'))     as cat,
           coalesce(pp.is_pinned, false)                           as pinned,
           coalesce(pp.is_locked, false)                           as locked,
           coalesce(pp.is_alert, false)                            as is_alert,
           pp.alert_severity,
           (coalesce(pp.is_alert, false) and pp.resolved_at is null
              and (pp.expires_at is null or pp.expires_at > now())) as alert_live,
           (pp.image_url is not null or pp.video_url is not null)  as has_media
      from public.posts_public pp
     where (v_cat is null or lower(pp.category) = v_cat)
       and not exists (select 1 from public.user_blocks b
                        where b.blocker_id = v_uid and b.blocked_id::text = pp.user_id)
     order by pp.created_at desc
     limit 600
  ),
  authors as (
    select user_id::text as uid, count(*) as n
      from public.posts
     group by user_id
  ),
  scored as (
    select p.post, p.author_key, p.created_at, p.id,
           (
             -- engagement "weight", decayed by age (Hacker-News style gravity).
             -- comments count most: replies are what keep a thread alive.
             (1 + greatest(3.0 * p.c + 2.0 * p.u - 2.0 * p.d + 2.0 * p.cf + 0.1 * p.vw, 0))
               / power(p.age_h + 2.0, 1.35)
           )
           -- Cold start: fresh posts with no replies get an early window of
           -- visibility (2.2x at 0h, ~1.4x at 10h, gone by ~36h) so every post
           -- has a fair chance of getting its first reply.
           * case when p.c = 0 and not p.alert_live and p.age_h < 36
                  then 1.0 + 1.2 * exp(-p.age_h / 10.0) else 1.0 end
           -- Encourage new voices: authors with up to 2 lifetime posts.
           * case when p.uid <> '' and coalesce(a.n, 0) <= 2 and p.age_h < 72
                  then 1.25 else 1.0 end
           -- Rising: quick traction in the first 12 hours.
           * case when p.age_h < 12
                   and (3.0 * p.c + 2.0 * p.u - 2.0 * p.d + 2.0 * p.cf) >= 5
                  then 1.2 else 1.0 end
           -- Rich media is more eye-catching.
           * case when p.has_media then 1.08 else 1.0 end
           -- Live alerts are time-critical; resolved / expired alerts sink.
           * case when p.alert_live then
                    case p.alert_severity
                      when 'critical' then 3.0 when 'high' then 2.2
                      when 'medium'   then 1.5 else 1.2 end
                  when p.is_alert then 0.35
                  else 1.0 end
           -- Admin-pinned posts get a strong (not absolute) lift.
           * case when p.pinned then 1.8 else 1.0 end
           * case when p.locked then 0.85 else 1.0 end
           -- ── Personal signals (For You only) ──
           * case when v_personal and p.area_tag is not null
                   and lower(p.area_tag) = any (v_areas)
                  then 1.3 else 1.0 end
           * case when v_personal
                  then 1.0 + 0.5 * least(2.0 * coalesce((v_aff ->> p.cat)::float8, 0), 1.0)
                  else 1.0 end
           -- Tiny per-user, per-day jitter (+-5%): the feed feels fresh each day
           -- and ties break fairly, but it stays stable while paginating.
           * case when v_personal
                  then 1.0 + 0.10 * (
                         ((abs(hashtextextended(p.id::text || coalesce(v_uid::text, 'guest') || v_day, 0)) % 1000)::float8 / 1000.0)
                         - 0.5)
                  else 1.0 end
           as s
      from pool p
      left join authors a on a.uid = p.uid
  ),
  ranked as (
    select sc.*,
           row_number() over (partition by sc.author_key order by sc.s desc) as rn
      from scored sc
  )
  select (r.post).*
    from ranked r
   -- Author diversity: an author's 2nd post counts 60%, 3rd 36%, ...
   order by r.s * power(0.6, r.rn - 1) desc, r.created_at desc, r.id desc
   limit v_limit offset v_offset;
end;
$$;

revoke all on function public.get_feed(text, text, integer, integer) from public;
grant execute on function public.get_feed(text, text, integer, integer) to anon, authenticated;

comment on function public.get_feed(text, text, integer, integer) is
  'Ranked feed. Modes: foryou (personalised), hot, top, new. Reads posts_public.';
