-- Canonical profile/content privacy applies to direct PostgREST reads as well
-- as the Android app. Restrictive policies are ANDed with every permissive
-- policy, including legacy/public-read policies. Keep existing ownership and
-- account-deactivation restrictions in place.
begin;

alter table public.profiles enable row level security;
alter table public.posts enable row level security;
alter table public.comments enable row level security;

create schema if not exists private;

-- Read privacy flags without recursively invoking profiles RLS. This function
-- must be owned by the trusted migration role (postgres in Supabase), which
-- can bypass RLS. The private schema must NOT be exposed through the API.
-- Only a boolean is returned; the caller's identity comes from auth.uid().
create or replace function private.profile_content_is_visible(
    author_id uuid,
    content_kind text
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select content_kind in ('post', 'comment') and exists (
        select 1
        from public.profiles p
        where p.id = author_id
          and (
              p.id = (select auth.uid())
              or (select public.is_admin(auth.uid()))
              or (
                  not coalesce(p.hide_profile, true)
                  and case content_kind
                      when 'post' then not coalesce(p.hide_posts, true)
                      when 'comment' then not coalesce(p.hide_comments, true)
                      else false
                  end
              )
          )
    );
$function$;

revoke all on function private.profile_content_is_visible(uuid, text) from public;
grant execute on function private.profile_content_is_visible(uuid, text)
    to anon, authenticated;

drop policy if exists "Profile privacy - profile reads" on public.profiles;
create policy "Profile privacy - profile reads"
    on public.profiles as restrictive
    for select to anon, authenticated
    using (
        id = (select auth.uid())
        or (select public.is_admin(auth.uid()))
        or not coalesce(hide_profile, true)
    );

drop policy if exists "Profile privacy - post reads" on public.posts;
create policy "Profile privacy - post reads"
    on public.posts as restrictive
    for select to anon, authenticated
    using (private.profile_content_is_visible(user_id, 'post'));

drop policy if exists "Profile privacy - comment reads" on public.comments;
create policy "Profile privacy - comment reads"
    on public.comments as restrictive
    for select to anon, authenticated
    using (
        private.profile_content_is_visible(user_id, 'comment')
        and (
            -- Authors can still retrieve their own comments from their history.
            user_id = (select auth.uid())
            or exists (
                -- Invoker RLS on posts also protects comments whose author is
                -- public but whose parent post belongs to a private author.
                select 1 from public.posts p where p.id = comments.post_id
            )
        )
    );

comment on function private.profile_content_is_visible(uuid, text) is
    'RLS privacy check for authored posts/comments; owner and trusted moderator exceptions, no anonymous-content exemption. Existing activity policies still apply.';

commit;
