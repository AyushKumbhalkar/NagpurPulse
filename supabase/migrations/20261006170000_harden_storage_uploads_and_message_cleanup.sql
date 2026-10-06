-- Harden Supabase Storage uploads: authenticated-only, user-scoped object paths, and image-only size limits.
drop policy if exists "Allow authenticated uploads 1hys5dx_0" on storage.objects;
drop policy if exists "Avatar uploads" on storage.objects;
drop policy if exists "Avatar updates" on storage.objects;

create policy "Authenticated post image uploads"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'post-images'
  and split_part(name, '/', 1) = auth.uid()::text
  and storage.extension(name) in ('jpg','jpeg','png','webp')
);

create policy "Authenticated avatar uploads"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'profile-images'
  and split_part(name, '/', 1) in ('avatars','banners')
  and split_part(split_part(name, '/', 2), '.', 1) = auth.uid()::text
  and storage.extension(name) in ('jpg','jpeg','png','webp')
);

create policy "Users update own avatar"
on storage.objects for update to authenticated
using (
  bucket_id = 'profile-images'
  and split_part(name, '/', 1) in ('avatars','banners')
  and split_part(split_part(name, '/', 2), '.', 1) = auth.uid()::text
)
with check (
  bucket_id = 'profile-images'
  and split_part(name, '/', 1) in ('avatars','banners')
  and split_part(split_part(name, '/', 2), '.', 1) = auth.uid()::text
);

create policy "Users delete own avatar"
on storage.objects for delete to authenticated
using (
  bucket_id = 'profile-images'
  and split_part(name, '/', 1) in ('avatars','banners')
  and split_part(split_part(name, '/', 2), '.', 1) = auth.uid()::text
);

update storage.buckets
set file_size_limit = 10485760,
    allowed_mime_types = array['image/jpeg','image/png','image/webp']::text[]
where id in ('post-images','profile-images');

create or replace function public.delete_conversation_for_both(p_conversation_id uuid)
returns void language plpgsql security definer
set search_path = 'public'
as $function$
declare v_message_ids uuid[];
begin
  if auth.uid() is null then raise exception 'Not authenticated'; end if;
  if not exists (
    select 1 from public.conversations c
    where c.id = p_conversation_id
      and auth.uid() in (c.participant_one, c.participant_two)
  ) then
    raise exception 'Conversation not found or you are not a participant';
  end if;
  select coalesce(array_agg(m.id), '{}') into v_message_ids
  from public.messages m where m.conversation_id = p_conversation_id;
  if cardinality(v_message_ids) > 0 then
    delete from public.message_hidden_for_users where message_id = any(v_message_ids);
  end if;
  delete from public.messages where conversation_id = p_conversation_id;
  delete from public.conversation_hidden_for_users where conversation_id = p_conversation_id;
  delete from public.conversations where id = p_conversation_id;
end;
$function$;