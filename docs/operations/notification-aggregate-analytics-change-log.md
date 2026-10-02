# Production Change Log — Notification Aggregate Analytics

- Project: `eazkmfzegxmdkbowohiy`
- Repository branch: `more-updates-on-nagpur-pulse`
- Migration: `supabase/migrations/20261002190000_notification_aggregate_analytics.sql`
- Purpose: measure notification inbox opens, notification taps, successful push sends, and (once wired to preference changes) push opt-outs without storing user-level analytics.
- Data minimization: counters are keyed only by UTC day, event type, and notification type. No user ID, notification ID, post/comment ID, conversation ID, device ID, FCM token, message body, or IP address is stored.

## Before state

1. There was no dedicated aggregate notification analytics table.
2. The Android client did not send inbox-open or notification-open aggregate events.
3. The push function did not count successful FCM sends in an aggregate table.
4. Notification delivery remained operationally observable through existing function logs, but not as a privacy-minimized daily counter.

## Changes

1. Create `public.notification_analytics_daily` with primary key `(event_date, event_type, notification_type)`, a non-negative `event_count`, and `updated_at`.
2. Enable RLS and revoke table access from `anon` and `authenticated`. Only the trusted `service_role` gets direct table privileges.
3. Add `public.record_notification_analytics(text,text)` with a strict event/type allow-list. It accepts authenticated app calls and trusted service-role calls, increments a daily aggregate, and stores no actor identity.
4. Android inbox opening and notification tapping increment best-effort counters. Analytics failures are logged at debug level and must not block the inbox or navigation.
5. After FCM accepts a push, the Edge Function increments `push_delivered`. A failed analytics counter does not turn a successful push into a failed delivery.
6. The schema includes `push_opt_out` as an allowed event, but preference-change instrumentation is not yet connected; do not treat opt-out counts as complete until that is wired and verified.

## Rollout order

1. Apply the SQL migration first.
2. Deploy the updated `send-push-notification` Edge Function.
3. Build and run the Android app from `more-updates-on-nagpur-pulse`.
4. Open Notifications and tap an item; verify the corresponding UTC-day counters increment, with no user-level identifiers in the table.
5. Trigger a test push and verify `push_delivered` increments only when FCM returns success.

## Rollback

This feature is additive. If rollback is required, first deploy a compatible older Edge Function and older Android build (or confirm clients will ignore the RPC). Then run:

```sql
begin;
drop function if exists public.record_notification_analytics(text, text);
drop table if exists public.notification_analytics_daily;
commit;
```

This removes all aggregate counters collected since rollout. It does not modify notifications, user preferences, device tokens, or FCM behavior. Reconcile Supabase migration history through the normal migration workflow; do not manually delete migration-history records without a deliberate decision.

## Verification checklist

- [ ] Migration appears in Supabase migration history.
- [ ] Table exists with RLS enabled and no anon/authenticated table grants.
- [ ] RPC accepts authenticated and service-role calls and rejects anonymous calls.
- [ ] Inbox open and notification tap counters increment.
- [ ] Successful push sends increment `push_delivered`; failed sends do not.
- [ ] No identifiers or notification content are stored in the analytics table.
- [ ] Preference-change opt-out instrumentation remains a follow-up item.
- [ ] Android build and end-to-end tests completed locally.
