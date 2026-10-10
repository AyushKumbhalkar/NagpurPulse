-- Video posts: image_url keeps holding the cover/thumbnail so every existing image code path still works.
alter table public.posts
  add column if not exists media_type text not null default 'image',
  add column if not exists video_url text,
  add column if not exists video_duration_ms integer;

alter table public.posts drop constraint if exists posts_media_type_check;
alter table public.posts add constraint posts_media_type_check check (media_type in ('image','video'));

-- Same view as 20261010150000_sec_db_hardening.sql, new columns appended at the end.
create or replace view public.posts_public with (security_invoker = false) as
select
  p.id,
  case when not coalesce(p.is_anonymous, false)
         or p.user_id = auth.uid()
         or coalesce(public.is_admin(auth.uid()), false)
       then p.user_id::text else '' end as user_id,
  p.title, p.body, p.category, p.area_tag, p.is_anonymous,
  p.upvotes, p.downvotes, p.comment_count, p.view_count, p.image_url,
  p.is_alert, p.alert_severity, p.created_at, p.is_pinned, p.is_locked,
  p.post_type, p.edited_at, p.edited_by_admin, p.expires_at, p.resolved_at, p.confirm_count,
  p.media_type, p.video_url, p.video_duration_ms
from public.posts p
where private.is_account_active(p.user_id);

grant select on public.posts_public to anon, authenticated;
grant select (media_type, video_url, video_duration_ms) on public.posts to anon, authenticated;

-- Video bucket: public read, 30 MB cap, owner-scoped uploads (mirrors post-images).
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('post-videos', 'post-videos', true, 31457280,
        array['video/mp4','video/webm','video/3gpp','video/quicktime']::text[])
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Authenticated post video uploads" on storage.objects;
create policy "Authenticated post video uploads"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'post-videos'
  and split_part(name, '/', 1) = auth.uid()::text
  and storage.extension(name) in ('mp4','webm','3gp','mov')
);

drop policy if exists "Users delete own post videos" on storage.objects;
create policy "Users delete own post videos"
on storage.objects for delete to authenticated
using (bucket_id = 'post-videos' and split_part(name, '/', 1) = auth.uid()::text);
