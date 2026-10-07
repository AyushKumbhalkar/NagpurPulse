-- Proof that a user accepted the Terms of Service and Privacy Policy at sign-up.
-- The app inserts one row per user per (terms_version, privacy_version) after the
-- account is verified. Bump LegalVersions in AuthRepository.kt when the documents change.

create table if not exists public.user_consents (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references auth.users (id) on delete cascade,
    terms_version   text not null,
    privacy_version text not null,
    source          text not null default 'email_signup',  -- email_signup | google_signup
    app_version     text,
    accepted_at     timestamptz not null default now()
);

create unique index if not exists user_consents_user_versions_key
    on public.user_consents (user_id, terms_version, privacy_version);

alter table public.user_consents enable row level security;

-- A signed-in user may record and read only their own consent. No updates or deletes
-- from the app: consent history must stay intact.
drop policy if exists "user_consents_insert_own" on public.user_consents;
create policy "user_consents_insert_own" on public.user_consents
    for insert to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists "user_consents_select_own" on public.user_consents;
create policy "user_consents_select_own" on public.user_consents
    for select to authenticated