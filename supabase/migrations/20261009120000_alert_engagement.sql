-- Alert engagement: severity-based expiry, "Resolved", and neighbour confirmations.
-- Apply this BEFORE shipping the app build that contains the new Alerts UI.

-- 1. Columns -----------------------------------------------------------------
alter table public.posts add column if not exists expires_at    timestamptz;
alter table public.posts add column if not exists resolved_at   timestamptz;
alter table public.posts add column if not exists confirm_count integer not null default 0;

-- The new alert types (power, water, safety, weather) must be accepted as post categories.
-- SUPABASE_SCHEMA.sql shipped a short allow-list; the app already uses many more categories,
-- so category validation lives in the client. Dropping is a no-op if it is already gone.
alter table public.posts drop constraint if exists posts_category_check;

create index if not exists idx_posts_alert_active
  on public.posts (created_at desc)
  where is_alert and resolved_at is null;

-- 2. Default expiry by severity (clients never send expires_at) ----------------
create or replace function public.set_alert_expiry()
returns trigger
language plpgsql
as $$
begin
  if new.is_alert and new.expires_at is null then
    new.expires_at := now() + case new.alert_severity
      when 'critical' then interval '12 hours'
      when 'high'     then interval '8 hours'
      when 'low'      then interval '3 hours'
      else                 interval '6 hours'
    end;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_set_alert_expiry on public.posts;
create trigger trg_set_alert_expiry
  before insert on public.posts
  for each row execute function public.set_alert_expiry();

-- 3. Confirmations -----------------------------------------------------------
create table if not exists public.alert_confirmations (
  post_id    uuid not null references public.posts(id)    on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

alter table public.alert_confirmations enable row level security;

drop policy if exists "Users read own alert confirmations" on public.alert_confirmations;
create policy "Users read own alert confirmations"
  on public.alert_confirmations for select to authenticated
  using (auth.uid() = user_id);
-- No insert/update/delete policies: rows are only written by confirm_alert().

-- Owners can update their own posts (RLS), so stop them inflating confirm_count directly.
create or replace function public.protect_alert_confirm_count()
returns trigger
language plpgsql
as $$
begin
  if new.confirm_count is distinct from old.confirm_count
     and coalesce(current_setting('app.alert_rpc', true), '') <> '1' then
    new.confirm_count := old.confirm_count;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_protect_alert_confirm_count on public.posts;
create trigger trg_protect_alert_confirm_count
  before update on public.posts
  for each row execute function public.protect_alert_confirm_count();

-- 4. RPCs --------------------------------------------------------------------
create or replace function public.confirm_alert(p_post_id uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid   uuid := auth.uid();
  v_owner uuid;
  v_count integer;
begin
  if v_uid is null then
    raise exception 'Not authenticated';
  end if;

  select user_id into v_owner
  from public.posts
  where id = p_post_id and is_alert and resolved_at is null
    and (expires_at is null or expires_at > now());

  if not found then
    raise exception 'Alert is not active';
  end if;
  if v_owner = v_uid then
    raise exception 'You cannot confirm your own alert';
  end if;

  insert into public.alert_confirmations (post_id, user_id)
  values (p_post_id, v_uid)
  on conflict do nothing;

  if found then
    perform set_config('app.alert_rpc', '1', true);
    update public.posts set confirm_count = confirm_count + 1
    where id = p_post_id
    returning confirm_count into v_count;
  else
    select confirm_count into v_count from public.posts where id = p_post_id;
  end if;

  return v_count;
end;
$$;

create or replace function public.resolve_alert(p_post_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Not authenticated';
  end if;

  update public.posts
  set resolved_at = now()
  where id = p_post_id and user_id = auth.uid() and is_alert and resolved_at is null;

  if not found then
    raise exception 'Alert not found or already resolved';
  end if;
end;
$$;

revoke all on function public.confirm_alert(uuid) from public, anon;
revoke all on function public.resolve_alert(uuid) from public, anon;
grant execute on function public.confirm_alert(uuid) to authenticated;
grant execute on function public.resolve_alert(uuid) to authenticated;
