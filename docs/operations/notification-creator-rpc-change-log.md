# Production Change Log — Restrict Notification Creator RPC

- Project: `eazkmfzegxmdkbowohiy`
- Branch: `more-updates-on-nagpur-pulse`
- Migration: `supabase/migrations/20261002210000_restrict_notification_creator_rpc.sql`

## Before

The function `public.create_notification_event(uuid,text,text,text,uuid,uuid,uuid,uuid)` was a SECURITY DEFINER helper but retained default PUBLIC execution privileges. A direct anonymous or authenticated RPC call could invoke it with caller-supplied recipient, type, title, body, and related IDs. This bypassed the intended database-trigger-only creation path.

## Change applied

- Revoked function execution from `PUBLIC`, `anon`, and `authenticated`.
- Granted direct execution only to `service_role`.
- Added a database comment documenting that the function is an internal helper.
- Trigger functions `notify_comment_insert` and `notify_comment_like_insert` remain SECURITY DEFINER and can invoke the helper as its owner. This preserves trigger-created comment, reply, mention, and comment-like notifications.
- No notification rows, preferences, tokens, or analytics counters were deleted or modified by this migration.

## Verification

Run the privilege query below and confirm `anon_can_execute=false`, `authenticated_can_execute=false`, and `service_role_can_execute=true`. Also test comment, reply, mention, and comment-like triggers end-to-end before public release.

```sql
select p.proname,
       has_function_privilege('anon', p.oid, 'execute') as anon_can_execute,
       has_function_privilege('authenticated', p.oid, 'execute') as authenticated_can_execute,
       has_function_privilege('service_role', p.oid, 'execute') as service_role_can_execute
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname = 'create_notification_event';
```

## Rollback

Rollback restores the prior direct execution grants and therefore reopens the risk described above. Do not run this casually.

```sql
grant execute on function public.create_notification_event(uuid, text, text, text, uuid, uuid, uuid, uuid)
  to public, anon, authenticated, service_role;
```

Prefer keeping the restriction. If a trusted server workflow requires direct invocation, grant it only to that trusted database role rather than to clients.

## Deployment record

- Applied to production on 2026-10-02.
- Android build and trigger-level end-to-end tests remain pending.
