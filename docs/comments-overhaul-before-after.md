# Comments overhaul — before and after

## Scope and provenance
- Branch: `Ayush_onboarding-update2`.
- Source: user-provided `comments-overhaul(1).patch` (same bytes as earlier `comments-overhaul.patch`).
- Patch commit: `a8ebed6e17d3153e41d95c1032ad66fb45eea441`.
- Exact patch applied without edits: **12 files**, consisting of eight Kotlin files, three string-resource files (English, Hindi, Marathi), and `supabase/migrations/20261008120000_comments_overhaul.sql`.
- Connected database: Supabase project `NagpuPulse`, project ref `eazkmfzegxmdkbowohiy`.
- The SQL migration was executed **unchanged** through Supabase and returned `success: true`. This document is documentation only: no additional fixes have been applied.

## Before → requested change

| Area | Before | After (patch) |
| --- | --- | --- |
| Comment loading | Kotlin repository read comment rows directly | New `get_thread_comments`, `get_user_comments`, and `get_user_comment_count` RPCs, root pagination (20 per page) and replies |
| Anonymous identity | Raw comments contained author `user_id` and `anon_alias` | Server-managed private alias mapping, RPC projection masking anonymous author identity |
| Writing comments | Direct database comment operations in app | New `add_comment`, `edit_comment`, `delete_comment` and `admin_remove_comment` RPCs |
| Moderation | Existing admin repository actions and policies | New admin comment-edit path, recording old/new body in `admin_actions.reason`; admin edits do not set the public edited timestamp |
| Comment likes and reports | Existing comment likes table/triggers and report records | RPC-based like toggle, comment-report deduplication, reason classification |
| Comment data integrity | Existing schema and counters | New trigger-managed comment count and karma on inserts, added body length check (1–2000), indexes, and parent validation |
| Realtime | `public.comments` in `supabase_realtime` publication | `public.comment_events` added to publication and `comments` removed |
| UI | Previous comment cards/thread presentation | Updated cards, composer, reply navigation, sort, loading, badges, snackbars and optimistic behavior |
| Localization | Existing strings | Additional English, Hindi and Marathi comment strings |
| Documentation | No record of this deployment | This before/after file |

## Database state observed before migration
- `public.comments`: **304** rows.
- `public.comment_reports`: **15** rows; zero duplicate reporter/comment pairs.
- `public.posts`: **122** rows.
- `public.comment_likes`: **3** rows.
- `supabase_realtime` published `public.comments`.
- Public SELECT policies on `comments` included **`Comments are viewable by everyone`** and **`Public comments`**. The migration instead drops a policy named `Comments viewable by everyone` (not an exact match).
- Existing owner update, admin update and two permissive authenticated INSERT policies were present.

## Database state observed after migration
- Supabase returned `success: true` for the submitted SQL.
- Counts remain **304** comments, **15** reports, **122** posts, **3** likes.
- New tables `public.comment_events` and `private.anon_aliases` exist.
- `supabase_realtime` now publishes `comment_events` rather than `comments`.
- Comment RPC functions `add_comment`, `edit_comment`, `delete_comment`, `get_thread_comments`, `get_user_comments`, `report_comment`, and `toggle_comment_like` exist.
- **Important: privacy is not secured by the migration as written.** Both permissive public SELECT policies, `Comments are viewable by everyone` and `Public comments`, remain. New owner/admin SELECT policies were added, but do not override existing permissive policies.
- Two permissive authenticated INSERT policies also remain.
- This verifies that SQL ran; it **does not** verify anonymous privacy, RPC behavior, Android compilation or app compatibility.

## Deployment caveats (not independently fixed)
1. **Unresolved privacy risk:** Anonymous comments can still be exposed via direct table SELECT owing to the remaining public SELECT policies. The migration's stated owner/admin-only raw access guarantee is not satisfied.
2. **Existing app versions:** Removing `comments` from Realtime and switching app reads to RPCs can disrupt older clients.
3. **No app build/run:** Android SDK/Gradle execution was not available in this session, so Kotlin compilation and device testing are unverified.
4. **No additional schema edits:** The SQL was executed exactly as attached. Fixing outstanding RLS policies would be a separate change and requires separate authorization.

## File inventory
- `app/src/main/java/com/nagpurpulse/data/model/Comment.kt`
- `app/src/main/java/com/nagpurpulse/data/repository/AdminRepository.kt`
- `app/src/main/java/com/nagpurpulse/data/repository/PostRepository.kt`
- `app/src/main/java/com/nagpurpulse/data/repository/ProfileRepository.kt`
- `app/src/main/java/com/nagpurpulse/ui/components/CommentCard.kt`
- `app/src/main/java/com/nagpurpulse/ui/components/CommentThread.kt`
- `app/src/main/java/com/nagpurpulse/ui/components/CommentUtils.kt`
- `app/src/main/java/com/nagpurpulse/ui/screens/thread/ThreadDetailScreen.kt`
- `app/src/main/res/values-hi/strings.xml`
- `app/src/main/res/values-mr/strings.xml`
- `app/src/main/res/values/strings.xml`
- `supabase/migrations/20261008120000_comments_overhaul.sql`
