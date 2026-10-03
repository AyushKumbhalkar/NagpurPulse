-- Harden profile creation and profile row permissions.
-- Prepared on NagpurePUlse-feature-v2 for review only. Not applied to live Supabase.
--
-- Apply 20261003140000_case_insensitive_profile_usernames.sql before this migration.

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $function$
declare
    requested_name text;
    candidate_name text;
begin
    requested_name := coalesce(
        nullif(trim(new.raw_user_meta_data->>'full_name'), ''),
        nullif(trim(new.raw_user_meta_data->>'name'), ''),
        nullif(split_part(coalesce(new.email, ''), '@', 1), ''),
        'new_user'
    );

    candidate_name := regexp_replace(lower(requested_name), '[^a-z0-9_]', '_', 'g');
    candidate_name := regexp_replace(candidate_name, '^_+|_+$', '', 'g');

    if candidate_name = '' then
        candidate_name := 'new_user';
    end if;

    -- Keep the provisional name valid for onboarding's username rules and
    -- make concurrent signups collision-resistant without relying on a query.
    if candidate_name !~ '^[a-z]' then
        candidate_name := 'u' || candidate_name;
    end if;
    candidate_name := left(candidate_name, 14)
        || '_'
        || substr(replace(new.id::text, '-', ''), 1, 8);

    -- Keep avatar_url empty so a Google provider avatar cannot accidentally
    -- make hasCompletedOnboarding() treat a new account as fully onboarded.
    insert into public.profiles (id, username, avatar_url)
    values (new.id, candidate_name, null);

    return new;
end;
$function$;

revoke all on function public.handle_new_user() from public;
revoke all on function public.handle_new_user() from anon;
revoke all on function public.handle_new_user() from authenticated;

-- RLS policies alone do not prevent updates to sensitive columns when a user can
-- issue arbitrary PostgREST updates. Grant client UPDATE only on explicitly
-- editable columns; omit id, karma, created_at, gender, and is_verified.
drop policy if exists "Own profile update" on public.profiles;
drop policy if exists "Users can update own profile" on public.profiles;
create policy "Users can update own profile"
    on public.profiles
    for update
    to authenticated
    using (auth.uid() = id)
    with check (auth.uid() = id);

drop policy if exists "Public profiles" on public.profiles;
drop policy if exists "Public profiles are viewable by everyone" on public.profiles;
create policy "Public profiles are viewable by everyone"
    on public.profiles
    for select
    to anon, authenticated
    using (true);

revoke update on table public.profiles from public;
revoke update on table public.profiles from anon;
revoke update on table public.profiles from authenticated;

grant update (
    username,
    tagline,
    avatar_url,
    areas,
    hide_comments,
    hide_posts,
    hide_profile,
    allow_dms,
    show_online_status,
    incognito_mode,
    hide_from_search,
    location,
    website,
    cover_url,
    push_enabled,
    comment_replies,
    mention_alerts,
    message_notifs,
    upvote_notifs,
    bio,
    banner_url,
    display_name,
    onboarding_completed
) on public.profiles to authenticated;
