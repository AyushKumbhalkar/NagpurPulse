-- Profile screen: "Top X%" karma rank, overall and inside the caller's first area.
--
-- Returns aggregate percentages for the CALLING user only (auth.uid()). It never
-- returns other users' ids, names, or karma values, so it is safe to expose to
-- authenticated users. Anonymous callers get no rows.
--
-- The app treats this RPC as optional: if it is missing or fails, the rank row on
-- the profile screen is simply hidden.

create or replace function public.get_my_karma_rank()
returns table (
  rank_percent      integer,
  total_users       integer,
  area              text,
  area_rank_percent integer,
  area_users        integer
)
language plpgsql
stable
security definer
set search_path = pg_catalog, public
as $$
declare
  v_uid         uuid := auth.uid();
  v_karma       integer;
  v_area        text;
  v_total       integer;
  v_ahead       integer;
  v_area_total  integer;
  v_area_ahead  integer;
begin
  if v_uid is null then
    return;
  end if;

  select coalesce(p.karma, 0), p.areas[1]
    into v_karma, v_area
    from public.profiles p
   where p.id = v_uid;

  if not found then
    return;
  end if;

  select (count(*))::integer,
         (count(*) filter (where coalesce(p.karma, 0) > v_karma))::integer
    into v_total, v_ahead
    from public.profiles p
   where coalesce(p.is_deactivated, false) = false;

  if v_area is not null and length(v_area) > 0 then
    select (count(*))::integer,
           (count(*) filter (where coalesce(p.karma, 0) > v_karma))::integer
      into v_area_total, v_area_ahead
      from public.profiles p
     where coalesce(p.is_deactivated, false) = false
       and p.areas @> array[v_area];
  end if;

  rank_percent      := greatest(1, ceil(100.0 * (v_ahead + 1) / greatest(v_total, 1))::integer);
  total_users       := v_total;
  area              := v_area;
  area_rank_percent := case
                         when v_area_total is null then null
                         else greatest(1, ceil(100.0 * (v_area_ahead + 1) / greatest(v_area_total, 1))::integer)
                       end;
  area_users        := v_area_total;
  return next;
end;
$$;

revoke all on function public.get_my_karma_rank() from public, anon;
grant execute on function public.get_my_karma_rank() to authenticated;
