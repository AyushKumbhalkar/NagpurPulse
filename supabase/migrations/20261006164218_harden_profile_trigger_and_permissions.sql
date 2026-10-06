-- Production-applied security hardening: profile ownership and column-level UPDATE grants.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = pg_catalog, public
as $function$
declare requested_name text; candidate_name text;
begin
 requested_name := coalesce(nullif(trim(new.raw_user_meta_data->>'full_name'), ''),nullif(trim(new.raw_user_meta_data->>'name'), ''),nullif(split_part(coalesce(new.email,''),'@',1), ''),'new_user');
 candidate_name := regexp_replace(lower(requested_name),'[^a-z0-9_]','_','g');
 candidate_name := regexp_replace(candidate_name,'^_+|_+$','','g');
 if candidate_name='' then candidate_name:='new_user'; end if;
 if candidate_name !~ '^[a-z]' then candidate_name:='u'||candidate_name; end if;
 candidate_name := left(candidate_name,14)||'_'||substr(replace(new.id::text,'-',''),1,8);
 insert into public.profiles(id,username,avatar_url) values(new.id,candidate_name,null);
 return new;
end;
$function$;
revoke all on function public.handle_new_user() from public, anon, authenticated;
drop policy if exists "Own profile update" on public.profiles;
drop policy if exists "Users can update own profile" on public.profiles;
create policy "Users can update own profile" on public.profiles for update to authenticated using(auth.uid()=id) with check(auth.uid()=id);
drop policy if exists "Public profiles" on public.profiles;
drop policy if exists "Public profiles are viewable by everyone" on public.profiles;
create policy "Public profiles are viewable by everyone" on public.profiles for select to anon, authenticated using(true);
revoke update on table public.profiles from public, anon, authenticated;
grant update(username,tagline,avatar_url,areas,hide_comments,hide_posts,hide_profile,allow_dms,show_online_status,incognito_mode,hide_from_search,location,website,cover_url,push_enabled,comment_replies,mention_alerts,message_notifs,upvote_notifs,bio,banner_url,display_name,onboarding_completed) on public.profiles to authenticated;