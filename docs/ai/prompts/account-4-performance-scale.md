# Account 4 — Performance, Scale, Reliability

**Goal:** the app stays fast on low-end Android phones and does not melt when thousands of users join.
**Budget:** ~13 chats. Needs measured data: run the app, use Android Studio Profiler / Logcat, and paste numbers.
**Rule:** measure first, change second. Every task ends with a before/after metric.

## PF-01 · Baseline (1 chat)
```
TASK PF-01. Create a measurement plan for cold start time, Home scroll jank, Thread open time, memory after 5
minutes of use, and APK/AAB size. Provide exact steps (Android Studio profiler, adb shell am start -W, Macrobenchmark
module skeleton) and a patch adding a baseline-profile/macrobenchmark setup. I will paste results afterwards.
```
Chat 2 (same chat later): `My numbers: <paste>. Rank the top 5 bottlenecks and what to fix first.`

## PF-02 · Pagination everywhere (2 chats)
Attach: `PostRepository.kt` (getPosts, comments), Home/Explore/Thread ViewModels, messages repo.
```
TASK PF-02. Implement keyset (cursor) pagination — created_at + id — for: feed, category feed, explore,
comments, messages, notifications (already has limit/Load more). Page size 20–30, preload next page near the end,
dedupe by id, keep scroll position, loading footer, stop at end. Output repository + ViewModel + minimal UI
patch per feature. Do one feature per message to stay within budget.
```

## PF-03 · Image pipeline (1 chat)
Attach: image upload code, Coil setup, ProfilePictureScreen/CreateThread utils.
```
TASK PF-03. Before upload: downscale (max 1600px long edge), compress (JPEG/WebP ~80%), strip EXIF location,
reject huge files, show progress, retry. For display: Coil memory/disk cache sizes, placeholder, correct
sizing so full-size images are never decoded for thumbnails. Suggest Supabase image transformation URLs for
thumbnails if available on our plan. Output a patch.
```

## PF-04 · Network resilience (2 chats)
Attach: `data/remote/SupabaseClient.kt`, 2 repositories.
```
TASK PF-04. Add consistent error handling: timeouts (connect/request), retry with exponential backoff for
idempotent reads, no retry for non-idempotent writes unless idempotency key, user-friendly error mapping
(offline, 401 session expired → re-login, 429 rate limited), and a lightweight offline cache for the feed
(Room or DataStore, choose the simplest that works). Output a patch and the error-mapping table.
```

## PF-05 · Realtime lifecycle (1 chat)
Attach: notification + message realtime code.
```
TASK PF-05. Audit realtime channels: created once per screen/user, unsubscribed on logout/ViewModel clear,
no duplicate subscriptions, reconnect behavior, and cost/limits per user (concurrent connections on our Supabase plan).
Replace 'reload everything on every event' with incremental updates where safe. Output a patch.
```

## PF-06 · Recomposition & scroll performance (2 chats)
Attach: one screen per chat (Home, then ThreadDetail, then Messages).
```
TASK PF-06. Audit this screen for unnecessary recomposition: unstable lambdas/classes, lists without keys,
heavy work in composition (sorting/grouping without remember), nested scrolling, large images, per-item
animations, derivedStateOf misuse. Output a ranked list and a patch with measurable wins; do not change visuals.
```

## PF-07 · Serialization safety (1 chat)
Attach: `data/model/*`, Json configuration.
```
TASK PF-07. Make decoding robust against server changes: Json { ignoreUnknownKeys = true; coerceInputValues = true },
defaults for nullable/new columns, enum-like strings handled safely, no crash if a row is malformed.
Output a patch and a list of models at risk.
```

## PF-08 · Logging & observability (1 chat) — finding F3
```
TASK PF-08. Strip or gate verbose logs (auth session checks, anon alias, tokens, emails, post bodies) behind
BuildConfig.DEBUG across the codebase; add a tiny Logger wrapper. Add Crashlytics (or Sentry) with non-fatal
reporting for repository failures, user id hashed only. Output a patch + the privacy note for the Data Safety form.
```

## PF-09 · Memory & lifecycle (1 chat)
```
TASK PF-09. Review ViewModels, flows and coroutine scopes for leaks: collect with lifecycle awareness
(collectAsStateWithLifecycle), GlobalScope usage, never-cancelled jobs, bitmaps held, large lists in state.
Output findings + patch.
```

## PF-10 · Size & startup (1 chat)
```
TASK PF-10. Reduce AAB size and startup: R8 full mode, shrink resources, remove unused dependencies/icons
(material-icons-extended is large — replace with only needed vector drawables if it saves meaningfully),
lazy init of Firebase/WorkManager, App Startup. Output a patch + expected savings and risks.
```

### Load test note (optional, with Account 1)
Ask Account 1 for `DB-09` first. For 5k–50k users the first limits you will meet are: realtime connections,
feed query without index, image bandwidth, and push fan-out. Track these four.
