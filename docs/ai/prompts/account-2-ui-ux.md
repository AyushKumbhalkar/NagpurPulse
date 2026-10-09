# Account 2 — UI / UX polish (screen by screen)

**Budget:** ~24 chats (about 1–2 per screen). **Rule:** one screen per chat; attach only that screen's files + theme.
**Every UI prompt = START + AI_CONTEXT + the prompt below + the file pack** (`tools/ai-pack.sh UI-05 <files>`).
Always include `ui/theme/Color.kt` and `ui/components/AnimationUtils.kt` the first time in a chat.
**Design goal:** premium, fast, psychologically rewarding — progress, social proof, instant feedback —
without dark patterns (no fake urgency, no guilt copy).

## Universal UI prompt (use for UI-01 … UI-14, replace SCREEN)
```
TASK <ID>: polish the SCREEN screen. Step 1: list in ≤10 bullets what is weak (hierarchy, spacing, touch
targets <48dp, contrast, missing loading/empty/error states, motion, copy, accessibility). Step 2: implement
the improvements as ONE patch using existing theme tokens and helpers (pressScale, rememberHaptic, shimmerEffect,
EmptyState). Include: skeleton loading, empty state with a CTA, error snackbar, haptics on key actions,
contentDescription on icons/images, small-screen (<360dp) layout. Do not change navigation routes or
ViewModel public APIs unless required; if required, list them first. Max 5 risk bullets.
```

## Per-screen add-ons (append one line to the universal prompt)
- **UI-00 Design system:** `Audit theme tokens and propose a type scale (Title/Body/Caption), spacing scale (4/8/12/16/24), corner radii, elevation/borders, and a light-theme contrast check. Output tokens as additions to Color.kt/Type.kt and a short usage guide docs/ai/DESIGN_RULES.md. Do not restyle screens yet.`
- **UI-01 Splash:** `Goal: perceived speed. Instant brand animation ≤1.2s, no blank flash, correct handling of system splash API on Android 12+, route decision (logged-in/onboarding/lock) without visible jank.`
- **UI-02 Welcome + Login:** `Goal: trust and low friction. Clear primary action, Google sign-in prominent, inline validation, keyboard handling (IME actions), password visibility, loading state on buttons, friendly error copy, links to Terms/Privacy.`
- **UI-03 Signup + verification + forgot password:** `Goal: finish signup. Show progress, resend-email cooldown, deep-link return from email, clear 'check your inbox' state, consent checkbox wording that matches the Terms/Privacy.`
- **UI-04 Onboarding:** `Goal: first-session activation. Make each step skippable only where allowed, show 'Step X of 4', pick-area search with Nagpur localities, username availability live check, photo picker with crop/preview, end with a 'first post' nudge and a welcome moment (subtle confetti/haptic).`
- **UI-05 Home:** `Goal: scan-ability and engagement. Card hierarchy (author, area tag, time, title, preview, actions), image aspect handling, vote animation with haptics, sticky category chips, pull-to-refresh, 'new posts' pill, skeletons, pagination footer, empty state. Keep scroll performance high (stable keys, no heavy modifiers).`
- **UI-06 Explore:** `Goal: discovery. Trending section, category grid with counts, search entry, local-area filter, empty states.`
- **UI-07 Create post:** `Goal: lower effort, higher quality. Category and area selection UX, character counters, draft autosave (in-memory + restore on return), image attach with progress, anonymous toggle explained, clear validation, success moment after posting.`
- **UI-08 Thread detail:** `Goal: conversation flow. Nested replies indentation limit, sticky reply composer, vote/like feedback, highlight target comment from deep link, 'jump to latest', report/share menu, long-comment collapse, loading skeletons.`
- **UI-09 Alerts:** `Goal: clarity and trust. Severity color/icon system, time-ago prominence, 'verified' marker, filter by severity, empty state, clear disclaimer that it is community-reported, not an official emergency service.`
- **UI-10 Messages/Chat:** `Goal: familiar messaging UX. Unread badges, last-message preview, swipe actions, chat bubbles grouping by time, delivery states, composer with IME handling, long-press actions, empty state with 'find people' CTA.`
- **UI-11 Profile + Public profile + User search:** `Goal: identity and progress. Karma/badges/streak progress display, post/comment tabs with pagination, follow/message/report/block actions, edit profile entry, shimmer states.`
- **UI-12 Settings:** `Goal: trust and control. Group settings logically, switch rows with descriptions, account/privacy/notifications/display, delete-account and logout confirmations, app version, links to policy/support.`
- **UI-13 Notifications:** `Apply docs patch notifications-premium-update.patch first (already prepared). Then: verify it builds, polish spacing/animations, wire any new server types from DB-12 (milestone), and check empty/loading/error states on slow network.`
- **UI-14 Admin (optional):** `Goal: safe and efficient moderation. Clear destructive-action confirmations, audit info shown, bulk actions, filters. Do not change permissions logic.`

## UI-15 · Accessibility pass (1 chat)
```
TASK UI-15. For the screens listed (paste names), produce a TalkBack/accessibility checklist and patch:
contentDescription, focus order, minimum 48dp targets, contrast ≥ 4.5:1 for body text, font scale up to 200%
without clipping, no information by color alone, semantic roles for custom clickables.
```

## UI-16 · State consistency (1 chat)
```
TASK UI-16. Create/standardize shared composables in ui/components: ErrorState(retry), LoadingList(skeleton),
EmptyState usage rules, InlineBanner (offline/info). Then patch the 3 screens I list to use them. Output a
migration guide table: screen → components to adopt.
```

## UI-17 · Localization (1 chat)
```
TASK UI-17. Extract hard-coded user-visible strings from the files I attach into strings.xml (en) and create
hi and mr translations. Respect plurals (e.g. '1 reply'/'3 replies'). Output patch + list of strings that need
a native speaker review. locales_config.xml already exists.
```

### Suggested loop per screen (≈ 6–8 messages)
1. START+context+prompt+files → 2. read Claude's weak-points list, reply "go" or adjust → 3. patch → 4. build →
5. screenshot description or errors back (BUILD-FIX) → 6. END prompt → log it.
