# Account 5 — QA, Integration, Release

**Role:** gatekeeper. Nothing reaches `main` until this account has reviewed it. Also keeps the shared
memory tidy. **Budget:** ~12 chats, many of them short (reviews).

## Branch model
- `main` = releasable. `ai/integration` = where all accepted patches land. `ai/<TASK-ID>` = one per task.
- Merge order: task branch → `ai/integration` (after REVIEW) → `main` (after QA-07 checklist passes).

## QA-03 · Review a patch (use for EVERY patch — ~1 short chat each, or batch 2–3 small ones)
```
TASK QA-03. Review the attached patch for <TASK-ID>. Context version info is in AI_CONTEXT. Check: compiles
(imports, API versions), behavior regressions, security (RLS, secrets, logging, exported components),
Play policy impact, performance, accessibility, and conflicts with these other open patches: <list IDs>.
Output: BLOCKERS / SHOULD-FIX / NITS, then a corrected diff for BLOCKERS only, then a 6-line manual test script.
```

## QA-01 · Test infrastructure (2 chats)
Attach: `app/build.gradle.kts`, `NotificationsViewModel` (in NotificationsScreen.kt), 2 repositories.
```
TASK QA-01. There are no tests. Add: JUnit5/JUnit4 + MockK + Turbine + kotlinx-coroutines-test (versions
compatible with Kotlin 2.0), a MainDispatcherRule, fakes for repositories, and 6 meaningful ViewModel tests:
notifications (mark read optimistic + failure rollback, delete + undo, grouping), auth state routing, create
post validation. Output patch + how to run (./gradlew testDebugUnitTest).
```

## QA-02 · Critical-path instrumented tests (2 chats)
```
TASK QA-02. Write Compose UI/instrumented tests (androidx.compose.ui:ui-test-junit4 + Hilt test setup) for:
login happy/error path (fake backend), create post validation, vote button toggle, notification swipe actions.
Use test tags added minimally to production composables. Output patch + CI-friendly run command.
```

## QA-04 · Merge / build-fix session (as needed)
```
TASK QA-04. I merged patches <IDs> into ai/integration and got these errors/conflicts: <paste>. Resolve with
the smallest changes, keep each task's intent, output one patch on top of ai/integration and explain each
conflict in one line.
```

## QA-05 · Two-user security smoke test (1 chat)
```
TASK QA-05. Using DB-01's findings, write a repeatable script (SQL via psql or HTTP via curl with two user JWTs)
that tries: reading another user's messages/notifications/device tokens, editing another user's post, voting twice,
forging counts/karma, calling admin RPCs as normal user, uploading to someone else's storage path, deleting others'
comments. Expected result for each = denied. Output the script + results table template.
```

## QA-06 · CI (1 chat)
```
TASK QA-06. Add .github/workflows/android.yml: on PR → set up JDK 17, Gradle cache, assembleDebug, lintDebug,
testDebugUnitTest, upload lint report; secrets are NOT required for debug builds (use placeholders for
SUPABASE_URL/ANON_KEY via env). Add a second workflow for tagged release → bundleRelease (signing via secrets).
```

## QA-07 · Regression & device matrix (1 chat)
```
TASK QA-07. Produce docs/ai/QA_CHECKLIST.md: smoke test (15 minutes) and full regression (60 minutes) covering
every screen/journey in this app; device/OS matrix (Android 8, 10, 12, 13, 14/15; small phone, large phone,
low RAM); network matrix (offline, 2G/slow, flaky); interrupts (call, rotation, background/kill, app update);
two-device realtime tests (messages, notifications). Include pass/fail columns.
```

## QA-08 · Release notes & versioning (1 chat)
```
TASK QA-08. From these merged HANDOFF_LOG entries and git log: <paste>, write the Play 'What's new' text (≤500
chars, per-language en/hi/mr), internal changelog, and the versionCode/versionName bump patch.
```

## QA-09 · Context compression (every ~10 tasks)
Use the CONTEXT-COMPRESS prompt from `00-session-templates.md` with AI_CONTEXT.md + last 10 HANDOFF_LOG entries.

## Weekly routine (20 minutes, no AI needed)
1. Update TASK_BOARD statuses. 2. Check `ai/integration` builds and smoke checklist passes.
3. Pick next 3 tasks per account so nobody waits for the 5-hour reset to decide what to do.
