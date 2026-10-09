# Account 3 — Play Store & Compliance

**Setup:** turn on web search in this account. Google's policies change often, so every task below tells
Claude to **check the current official Google Play / Android pages** and quote the date it checked.
Never trust a number (API level, testing rules) from memory — verify first.
**Budget:** ~14 chats. Order: PS-01, PS-02 first (they change code), then PS-03…PS-06, PS-07, rest.

## PS-01 · SDK & dependency upgrade plan (1 chat)
Attach: `app/build.gradle.kts`, `gradle/libs.versions.toml`.
```
TASK PS-01. Search the official Android/Google Play docs for the CURRENT target API level requirement for new
apps and updates (state the date and URL you used). Our app: compileSdk/targetSdk 34, AGP 8.3.2, Kotlin 2.0.0,
Compose BOM 2025.01.01. Produce: required targetSdk, compatible AGP/Gradle/Kotlin/Compose versions, a list of
behavior changes between 34 and the target that can break our app (notifications permission, edge-to-edge,
foreground services, photo picker, predictive back, etc.), and ONE patch bumping versions. Mark risky items.
```

## PS-02 · Permissions audit (1 chat)
Attach: `AndroidManifest.xml`, `data/location/LocationHelper.kt`, upload code.
```
TASK PS-02. Manifest requests INTERNET, ACCESS_NETWORK_STATE, READ_EXTERNAL_STORAGE(≤32), READ_MEDIA_IMAGES,
POST_NOTIFICATIONS, VIBRATE, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION. For each: is it truly needed, where is
it used in code, and what does current Play policy say (photo/video permissions policy, location). Replace
READ_MEDIA_IMAGES with the system photo picker if possible; prefer coarse-only location unless fine is justified.
Output a patch (manifest + runtime request flows with rationale dialogs) and the exact Play Console declaration text.
```

## PS-03 · Data Safety worksheet (1 chat)
Attach: models (`data/model/*`), repositories list, `docs/SIGNUP_LAUNCH_CHECKLIST.md`.
```
TASK PS-03. From the code, list every piece of user data we collect/store/transmit: email, username, profile
photo, posts/comments, messages, approximate/precise location, FCM token, device info, analytics (aggregate
notification analytics), crash data (if added), purposes, whether shared with third parties (Supabase, Firebase/
Google), encryption in transit/at rest, whether users can request deletion. Output the answers for each Data
Safety form section as a table I can copy into Play Console, and list any mismatch with the privacy policy.
```

## PS-04 · Privacy policy & terms (1 chat)
Attach: `legal-drafts/privacy.html`, `legal-drafts/terms.html`, PS-03 output.
```
TASK PS-04. Review the privacy policy and terms against what the app really does (PS-03 table) and against
Play requirements (public URL, name of developer/entity, contact email, data retention, deletion, children,
UGC rules, location use, third parties) and India's DPDP Act essentials (consent, grievance contact, purpose).
Output: gaps list + corrected HTML files as patch. State clearly that this is not legal advice and the founder
should have a lawyer review before launch.
```

## PS-05 · UGC policy compliance (1 chat)
Attach: output of DB-08, thread/report UI files.
```
TASK PS-05. Google Play's user-generated-content policy expects: terms that forbid objectionable content,
in-app report, block, and moderation with timely action. Check the app against the CURRENT policy text (search it).
List what exists vs missing in the UI (report on post/comment/user/message, block user, hidden content),
content filters, minimum age statement, and an emergency-content disclaimer for Alerts. Output a patch for
missing UI plumbing (coordinate with Account 2 files) and a moderation SOP (who reviews, response time).
```

## PS-06 · Account deletion (1 chat)
```
TASK PS-06. Play requires apps that allow account creation to offer in-app account deletion AND a web link
for deletion requests (verify current policy). Check Settings for the delete flow, the SQL delete_user_account,
confirmation + re-auth, what data is removed/kept. Output: UI patch if missing, and a simple web page text
(HTML) for the deletion-request URL with the contact path.
```

## PS-07 · Signing, AAB, R8 (2 chats)
Attach: `app/build.gradle.kts`, `proguard-rules.pro`.
```
TASK PS-07. Prepare release build: Play App Signing + upload keystore steps (never ask me to paste the keystore),
signingConfigs reading from local.properties/env, AAB output, versionCode/versionName strategy, R8 keep rules
for kotlinx.serialization, Ktor, supabase-kt, Hilt, Coil, Firebase, and resource shrinking. Produce a patch and
a 'release-build smoke test' checklist, because minified builds often crash on reflection/serialization.
```
Chat 2: `Here is the R8 crash/ClassNotFound log from my release build: <paste>. Fix the keep rules only.`

## PS-08 · Store listing (1 chat)
```
TASK PS-08. Create the store listing for NagpurPulse: app name (≤30 chars), short description (≤80), full
description (≤4000, Nagpur-focused, honest, no keyword stuffing), graphics checklist (icon 512, feature graphic
1024x500, phone screenshots count/sizes), screenshot captions, category, tags, contact email, and the
content-rating questionnaire answers (UGC, location, chat). Verify current size limits via search.
```

## PS-09 · Testing tracks (1 chat)
```
TASK PS-09. Search the CURRENT Google Play requirements for new personal developer accounts (closed testing
duration/tester count, if any) and organization accounts. Produce a day-by-day plan: internal testing → closed
testing (how to recruit testers in Nagpur: colleges, communities), feedback collection form, crash thresholds to
proceed, and staged rollout percentages for production.
```

## PS-10 · Firebase / FCM / Crashlytics (1 chat)
```
TASK PS-10. Verify Firebase setup for release: google-services.json matches applicationId and release SHA-1/
SHA-256 (list steps), FCM channel setup, notification icon/color, Android 13 permission flow, token refresh,
Crashlytics + Analytics decisions (privacy impact on Data Safety). Output a patch + dashboard checklist.
```

## PS-11 · Alerts & sensitive content (1 chat)
```
TASK PS-11. The app has community Alerts (possibly emergencies, crime, missing people). Review against Play policies
on emergency/health/misleading claims and user safety. Output: disclaimer copy, report-misinformation flow,
verified-reporter rules, takedown timing, and wording that avoids implying official government service.
```

## PS-12 · India compliance (1 chat)
```
TASK PS-12. Review consent screens (user_consents table, LegalConsent.kt) and signup for DPDP-style requirements:
clear purpose, withdraw consent, minors, data deletion, grievance officer name/email in app and policy.
Output patch for in-app text/links + a one-page compliance checklist. Not legal advice.
```

## PS-13 · Pre-launch report & final checklist (1 chat)
```
TASK PS-13. Here is the Play pre-launch report / Android vitals / policy warnings: <paste>. Triage by severity,
map each to a task (UI/PF/DB), and produce the final go/no-go checklist for production release.
```
