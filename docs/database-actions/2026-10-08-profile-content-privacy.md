# Profile and authored-content privacy

Implemented on `Ayush-new-onboarding`. This document describes a prepared
migration, not confirmation that it has been applied to live Supabase.

## Privacy contract

These flags are access restrictions on the canonical database rows, rather than
instructions for Android to hide already-fetched data:

| Flag | Effect for other users and guests |
| --- | --- |
| `hide_profile` | Cannot read the profile, its authored posts, or its authored comments. |
| `hide_posts` | Cannot read that user's posts through feeds, alerts, search, ID lookups, joins, or direct PostgREST queries. The profile and comments on accessible posts remain readable unless independently hidden. |
| `hide_comments` | Cannot read that user's comments, including in threads and comment-history queries. The profile and posts remain readable unless independently hidden. |

- Authors can retrieve their own profile/content regardless of these three flags.
  Existing account-deactivation policies still apply: deactivated owners can
  retrieve their profile for reactivation, but cannot retrieve inactive content.
- Trusted moderators recognized by the existing `public.is_admin(auth.uid())`
  helper are exempt from these privacy flags only where existing permissions
  already allow a read. Ordinary users cannot obtain this exception through
  profile verification status or a supplied user ID. Deactivation restrictions
  still apply to moderators.
- Comments from other authors also require a readable parent post. A comment
  author may still read their own comment in their history under a hidden post;
  this does not grant access to the post or other users' comments.
- Anonymous posts/comments follow the same rules. Their raw API rows contain
  author IDs, so the former client-side anonymous exception would leave a privacy
  bypass. This intentionally makes anonymous content private too when its owner
  enables these flags.
- Missing author profiles and null privacy flags fail closed for other readers.
  Turning a flag off restores reads allowed by the other existing policies.
- `hide_from_search` remains a discovery preference, not a new access restriction
  in this migration. `allow_dms`, `show_online_status`, and `incognito_mode` are
  outside this change. Their server enforcement needs separate work.

## Implementation and deployment

Apply `supabase/migrations/20261008170000_enforce_profile_content_privacy.sql`
through the normal Supabase migration workflow. Fetching this branch in Android
Studio or building the APK does **not** apply the database migration.

Prerequisites: the profile privacy columns, existing account-deactivation
policies/helpers, and the scoped `public.is_admin(uuid)` helper from previous
migrations must exist. Use the trusted Supabase migration role (`postgres`, with
RLS bypass). The function owner must retain that privilege; otherwise querying
profiles through the helper can recurse into RLS. Keep `private` out of the API's
exposed schemas. No app table is dropped or rewritten by this migration.

The migration enables RLS and adds SELECT-only **restrictive** policies to
`profiles`, `posts`, and `comments`. PostgreSQL ANDs these with the existing
permissive policies, so an old `USING (true)` public-read policy cannot undo them.
The helper reads privacy flags as the trusted database role without recursively
invoking profile RLS, returns only a visibility boolean, and derives the requester
from `auth.uid()`. There is no new public RPC or UI change. Existing grants,
owner-write policies, and activity restrictions are retained.

RLS does not recall previously downloaded data, cached images, or previously
delivered notification summaries. Public Storage images are not made private by
this migration. Database owners, service-role access, and SECURITY DEFINER jobs
can bypass RLS; their notification/export payloads must be audited separately
before claiming privacy across every backend delivery channel. Service-role
credentials must never be used by the Android app.

## Validation

`supabase/tests/profile_privacy.sql` creates a minimal Supabase authorization
fixture in a **fresh disposable PostgreSQL database only**. It deliberately
refuses a database with existing public application tables or an `auth` schema.
Never run this fixture against a live project. It loads the branch's actual admin
and activity helpers, applies the new migration twice, then switches between
`anon`, `authenticated`, and a trusted `service_role` for real RLS assertions.

Run it locally with an isolated container:

```bash
docker run --detach --rm --network none --name profile-privacy-test \
  -e POSTGRES_HOST_AUTH_METHOD=trust postgres:16-alpine
# Wait for pg_isready to report that the server accepts connections.
docker exec profile-privacy-test pg_isready -U postgres
docker exec profile-privacy-test createdb -U postgres profile_privacy
docker cp supabase profile-privacy-test:/tmp/privacy-supabase
docker exec profile-privacy-test psql -U postgres -d profile_privacy \
  -f /tmp/privacy-supabase/tests/profile_privacy.sql
docker stop profile-privacy-test
```

The GitHub `Profile privacy RLS` workflow runs these checks for relevant branch
pushes and pull requests. Coverage includes normal/anonymous content, profile
and content flags independently, direct ID reads, joins, hidden parent posts,
owner reads and privacy writes, attempted cross-owner changes, trusted moderator
access, extra permissive policies, null/missing profiles, deactivation, and
trusted backend access. Existing Android filtering remains defense in depth.

After applying the migration to live Supabase, validate with separate owner,
viewer, moderator, and guest sessions using their normal API roles. Check direct
table reads, normal app feeds, profile saves, comments, and realtime subscriptions.
An established client can retain previously received rows; hiding content does
not automatically clear those clients' caches.

## Rollback

Drop only the three `Profile privacy - ... reads` policies introduced here, then
drop `private.profile_content_is_visible(uuid, text)`. Keep the existing RLS,
grants, ownership, and activity policies. Rollback reopens the pre-existing privacy
gap and should be used only after assessing that consequence.
