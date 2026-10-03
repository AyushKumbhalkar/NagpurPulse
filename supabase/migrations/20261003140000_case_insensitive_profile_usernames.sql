-- Prevent visually identical usernames that differ only by letter casing.
-- Existing profile data was checked for case-insensitive duplicates before adding this index.
create unique index if not exists profiles_username_lower_unique_idx
    on public.profiles (lower(username));

