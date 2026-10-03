-- The profiles table is publicly readable. Gender is only needed transiently
-- to tailor onboarding avatar suggestions, so clear previously stored values.
-- The updated app no longer writes the onboarding gender to public profiles.
update public.profiles
set gender = null
where gender is not null;
