# Signup launch checklist (steps only you can do)

## 1. Database: run the consent table (A1)
1. Supabase Dashboard -> SQL Editor.
2. Paste `supabase/migrations/20261007120000_create_user_consents.sql` and run it.
3. Test: create a new account, verify the OTP, then open Table Editor -> `user_consents`. One row should appear.
4. When you change the Terms or Privacy Policy, update `LegalVersions` in `AuthRepository.kt`.

## 2. Email delivery and spam protection (A2)
| Step | Where |
|---|---|
| Set up your own SMTP sender (Brevo, Resend, Amazon SES, etc.) | Supabase -> Authentication -> Emails -> SMTP settings |
| Raise the email rate limit after SMTP is set | Supabase -> Authentication -> Rate limits |
| Customise the signup OTP email text (add your brand, say the code expires) | Supabase -> Authentication -> Emails -> Templates |
| Optional but recommended: turn on CAPTCHA protection | Supabase -> Authentication -> Attack protection. Needs a code change (a CAPTCHA widget), ask for it when you are ready. |
Why: the built-in Supabase email sender has a very low hourly limit and is meant for testing. Check the current limit in your dashboard.

## 3. Legal pages (A4)
1. Edit the DRAFTS in `legal-drafts/` (replace every [PLACEHOLDER]) and have a lawyer review them.
2. Publish them at the two URLs used in `LegalConsent.kt`:
   - https://www.nagpurpulse.in/legal/terms.html
   - https://www.nagpurpulse.in/legal/privacy.html
3. Open both links in a private browser window on your phone. They must load without login.
4. Use the same Privacy URL in Google Play Console (Policy -> App content -> Privacy policy).
5. Fill in Play Console "Data safety" honestly: email, user content, location (if used), device ID/push token, analytics, crash logs.

## 4. Crash reporting (C5)
- After the first release build runs on a phone, open Firebase Console -> Crashlytics to confirm it is receiving data.
- Add "crash and diagnostic data" to the Privacy Policy (the draft already does).

## 5. Release build test (A3)
- R8 is on for release and the project had no `proguard-rules.pro` before. Build a release APK/AAB and test: sign up, OTP, Google sign-in, posting, push notifications.
- If something works in debug but breaks in release, send me the error text and I will add the missing keep rules.

## 6. Hindi and Marathi test (C2, not code)
Switch the language chip to Hindi, then Marathi, and set the phone font size to the largest. Check:
- [ ] Create Account button text is not cut off
- [ ] Three benefit chips at the bottom are not cut off
- [ ] Both dialogs (verify email, email already used) scroll and show all text
- [ ] Error messages under each field are fully visible
- [ ] Language chip does not cover the logo on a small phone
Have a Hindi and a Marathi speaker read every screen once.

## 7. TalkBack test (C1)
Turn on TalkBack and go through signup: each field should be announced with its hint ("Email address") and, when wrong, its error.