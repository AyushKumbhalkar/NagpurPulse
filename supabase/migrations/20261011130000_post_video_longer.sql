-- Longer videos (up to 5 minutes). The app transcodes to ~720p / ~850 kbps before upload,
-- so a 5 minute clip lands around 30-35 MB. Raise the bucket cap to 50 MB (the Supabase free-plan ceiling).
update storage.buckets
set file_size_limit = 52428800
where id = 'post-videos';
