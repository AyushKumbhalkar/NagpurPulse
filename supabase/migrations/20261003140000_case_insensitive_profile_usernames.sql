-- Prevent visually identical usernames that differ only by letter casing.
-- Existing profile data was checked for case-insensitive duplicates before adding this index.
create unique index if not exists profiles_username_lower_unique_idx
    on public.profiles (lower(username));

-- Gender is only needed transiently to choose onboarding avatars. The profiles
-- table is publicly readable, so clear previously stored values rather than
-- exposing a field the onboarding UI promises not to display.
update public.profiles set gender = null where gender is not null;
