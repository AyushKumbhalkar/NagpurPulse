-- Prevent visually identical usernames that differ only by letter casing.
-- Abort safely if existing data has case-insensitive collisions; resolve those
-- accounts explicitly before retrying this migration.
do $$
begin
    if exists (
        select 1
        from public.profiles
        where username is not null
        group by lower(username)
        having count(*) > 1
    ) then
        raise exception 'Cannot add case-insensitive username index: duplicate usernames differ only by case. Resolve conflicts before retrying.';
    end if;
end;
$$;

create unique index if not exists profiles_username_lower_unique_idx
    on public.profiles (lower(username));
