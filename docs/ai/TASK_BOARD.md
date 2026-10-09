# Task Board
Status values: TODO · DOING · PARTIAL · DONE · BLOCKED. Update after every chat.
"Chats" = estimated number of fresh chats (≈ 6–10 messages each).

## Account 1 — Backend / DB / Security  (Supabase connector, staging project)
| ID | Task | Files to attach / action | Chats | Status |
|---|---|---|---|---|
| DB-01 | RLS audit, every table, 2-user attack tests | list policies via connector | 2 | TODO |
| DB-02 | Storage buckets: policies, size/mime limits, cleanup | storage policies + upload code | 1 | TODO |
| DB-03 | Auth hardening: email verify, passwords, OAuth, rate limits | auth settings + AuthRepository | 1 | TODO |
| DB-04 | SECURITY DEFINER / grants / search_path audit | function list via connector | 1 | TODO |
| DB-05 | Edge functions: consolidate push, verify auth | supabase/functions/* | 1 | TODO |
| DB-06 | Abuse protection: server validation + rate limits | posts/comments/messages SQL | 2 | TODO |
| DB-07 | Move counters/karma to triggers (F1, F2) | PostRepository, NotificationRepository | 2 | TODO |
| DB-08 | Moderation backend: reports, blocks, admin audit log | admin screens + repos | 2 | TODO |
| DB-09 | Indexes & query plans (use advisors) | connector advisors | 1 | TODO |
| DB-10 | Account deletion completeness + data export + retention | delete_user_account SQL | 1 | TODO |
| DB-11 | Staging/prod split, backups/PITR, migration workflow | docs | 1 | TODO |
| DB-12 | Notification server logic: milestone, streak data | notification migrations | 1 | TODO |

## Account 2 — UI / UX (one screen per chat, in user-journey order)
| ID | Screen | Files to attach | Chats | Status |
|---|---|---|---|---|
| UI-00 | Design-system audit (tokens, spacing, type scale, light theme) | theme/*, DesignSystem.kt, AnimationUtils.kt | 1 | TODO |
| UI-01 | Splash | splash/SplashScreen.kt | 1 | TODO |
| UI-02 | Welcome + Login | auth/WelcomeContent, LoginScreen, LoginContent, LoginLayout | 1 | TODO |
| UI-03 | Signup + email verification + forgot password | auth/Signup*, ForgotPasswordDialog | 1 | TODO |
| UI-04 | Onboarding (area, identity, username, photo) | onboarding/* | 2 | TODO |
| UI-05 | Home feed + post cards | home/HomeScreen, PostCard, FeedCardVariants | 2 | TODO |
| UI-06 | Explore + category posts | explore/* | 1 | TODO |
| UI-07 | Create post | thread/CreateThread* | 2 | TODO |
| UI-08 | Thread detail + comments | thread/ThreadDetailScreen, CommentCard | 2 | TODO |
| UI-09 | Alerts | alerts/AlertsScreen, AlertCard | 1 | TODO |
| UI-10 | Messages list + chat | messages/* | 2 | TODO |
| UI-11 | Profile + public profile + user search | profile/*, search/* | 2 | TODO |
| UI-12 | Settings (all sub-screens) | settings/* | 2 | TODO |
| UI-13 | Notifications (apply premium patch, polish) | notifications/NotificationsScreen | 1 | TODO |
| UI-14 | Admin panel (low priority) | admin/* | 1 | TODO |
| UI-15 | Accessibility pass (TalkBack, 48dp, contrast, font scale) | per screen list | 1 | TODO |
| UI-16 | Empty/error/loading states consistency | all screens | 1 | TODO |
| UI-17 | Localization (Hindi/Marathi), strings.xml extraction | res/values, locales_config | 1 | TODO |

## Account 3 — Play Store & Compliance
| ID | Task | Files / inputs | Chats | Status |
|---|---|---|---|---|
| PS-01 | SDK upgrade plan (target/compile SDK, AGP, deps) | build.gradle.kts, libs.versions.toml | 1 | TODO |
| PS-02 | Permissions audit (location, media, notifications) | AndroidManifest, LocationHelper | 1 | TODO |
| PS-03 | Data Safety form worksheet from real code | repos + models | 1 | TODO |
| PS-04 | Privacy policy & Terms review/finalize | legal-drafts/*.html | 1 | TODO |
| PS-05 | UGC policy: report/block/moderation, age rules | admin + thread screens | 1 | TODO |
| PS-06 | Account deletion: in-app + web URL | settings + delete SQL | 1 | TODO |
| PS-07 | Signing, AAB, R8/ProGuard rules, versioning | app/build.gradle.kts, proguard | 2 | TODO |
| PS-08 | Store listing: text, graphics list, content rating | — | 1 | TODO |
| PS-09 | Testing-track plan (internal → closed → production) | Play Console | 1 | TODO |
| PS-10 | Firebase/FCM/Crashlytics setup check | google-services, FCM docs | 1 | TODO |
| PS-11 | Emergency/alerts disclaimer + sensitive-content review | AlertsScreen | 1 | TODO |
| PS-12 | India compliance (DPDP consent, grievance contact) | consent flow | 1 | TODO |
| PS-13 | Pre-launch report triage + final checklist | Play pre-launch output | 1 | TODO |

## Account 4 — Performance, Scale, Reliability
| ID | Task | Files / inputs | Chats | Status |
|---|---|---|---|---|
| PF-01 | Baseline measurements (startup, jank) + baseline profile | app module | 1 | TODO |
| PF-02 | Pagination everywhere (feed, explore, comments, messages) | repos + screens | 2 | TODO |
| PF-03 | Image pipeline: compress/resize before upload, Coil cache | upload code, Coil setup | 1 | TODO |
| PF-04 | Network resilience: timeouts, retry/backoff, offline cache | SupabaseClient, repos | 2 | TODO |
| PF-05 | Realtime lifecycle & channel limits | notification/message realtime | 1 | TODO |
| PF-06 | Recomposition/stability audit of top 3 screens | Home, Thread, Messages | 2 | TODO |
| PF-07 | Serialization safety (ignoreUnknownKeys, nullables) | data/model/* | 1 | TODO |
| PF-08 | Logging cleanup (F3) + Crashlytics/observability | all | 1 | TODO |
| PF-09 | Memory/leak + lifecycle review | ViewModels | 1 | TODO |
| PF-10 | APK/AAB size & startup (R8, resources, ABI) | gradle | 1 | TODO |

## Account 5 — QA / Integration / Release
| ID | Task | Inputs | Chats | Status |
|---|---|---|---|---|
| QA-01 | Test infrastructure + first ViewModel tests | build.gradle, 3 ViewModels | 2 | TODO |
| QA-02 | Critical-path instrumented tests (login, post, comment, vote) | UI tests | 2 | TODO |
| QA-03 | Review every patch from accounts 1–4 (REVIEW prompt) | patches | ongoing | TODO |
| QA-04 | Merge/conflict + build-fix sessions | errors | ongoing | TODO |
| QA-05 | Two-user security smoke test script (RLS) | SQL/HTTP script | 1 | TODO |
| QA-06 | CI: GitHub Actions (build, lint, tests) | .github/workflows | 1 | TODO |
| QA-07 | Device/network test matrix + regression checklist | docs | 1 | TODO |
| QA-08 | Release notes, changelog, version bump | git log | 1 | TODO |
| QA-09 | Context compression (every ~10 tasks) | AI_CONTEXT + HANDOFF_LOG | 1 | TODO |

## Parallelism rules
- Accounts 1 and 2 can run at the same time (SQL/repos vs screens), except where a UI task needs a new RPC:
  then Account 1 finishes the DB task first and records the RPC name in HANDOFF_LOG.
- Account 3 and 4 tasks that edit `build.gradle.kts` or `AndroidManifest.xml` (PS-01, PS-02, PS-07, PF-10) must run one at a time.
- Account 5 never starts QA-03 on a patch until its handoff entry exists.

## Suggested order (≈ 8 weeks at 2–3 chats per day)
1. Week 1: DB-01…DB-04, PS-01, PS-02, UI-00, QA-01 · 2: DB-05…DB-08, UI-01…UI-04, PS-03…PS-06
3. Week 3–4: UI-05…UI-13, DB-09…DB-12, PF-01…PF-05 · 5: PF-06…PF-10, UI-14…UI-17, QA-02…QA-07
6. Week 6: PS-07…PS-13, closed testing release · 7–8: fix feedback, QA-08, production release
