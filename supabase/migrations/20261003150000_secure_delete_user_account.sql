-- Secure account deletion against deleting another user's account.
-- Prepared for review on NagpurePUlse-feature-v2; not applied to live Supabase.
-- Removes dependent content in FK-safe order before deleting the auth identity.
create or replace function public.delete_user_account(target_user_id uuid)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, auth
as $function$
begin
    if auth.uid() is null or auth.uid() is distinct from target_user_id then
        raise exception 'Not authorized to delete this account'
            using errcode = '42501';
    end if;

    -- Remove notifications and queued work tied to posts authored by this user.
    delete from public.notifications
    where user_id = target_user_id
       or related_post_id in (
            select id from public.posts where user_id = target_user_id
       );

    delete from public.notification_queue
    where post_id in (
        select id from public.posts where user_id = target_user_id
    );

    -- Remove chat data involving the account so profile/conversation foreign keys
    -- cannot block deletion. This intentionally deletes those conversations' history.
    delete from public.notifications
    where related_conversation_id in (
        select id
        from public.conversations
        where participant_one = target_user_id
           or participant_two = target_user_id
    );

    delete from public.message_hidden_for_users
    where user_id = target_user_id
       or message_id in (
            select m.id
            from public.messages m
            join public.conversations c on c.id = m.conversation_id
            where c.participant_one = target_user_id
               or c.participant_two = target_user_id
       );

    delete from public.conversation_hidden_for_users
    where user_id = target_user_id
       or conversation_id in (
            select id
            from public.conversations
            where participant_one = target_user_id
               or participant_two = target_user_id
       );

    delete from public.messages
    where conversation_id in (
        select id
        from public.conversations
        where participant_one = target_user_id
           or participant_two = target_user_id
    );

    delete from public.conversations
    where participant_one = target_user_id
       or participant_two = target_user_id;

    -- Remove comments on the user's posts and descendants of comments they wrote.
    -- Descendant replies are included to avoid violating comments.parent_id.
    with recursive comments_to_remove(id) as (
        select c.id
        from public.comments c
        where c.user_id = target_user_id
           or c.post_id in (
                select p.id from public.posts p where p.user_id = target_user_id
           )
        union
        select child.id
        from public.comments child
        join comments_to_remove parent on child.parent_id = parent.id
    )
    delete from public.comment_likes
    where user_id = target_user_id
       or comment_id in (select id from comments_to_remove);

    with recursive comments_to_remove(id) as (
        select c.id
        from public.comments c
        where c.user_id = target_user_id
           or c.post_id in (
                select p.id from public.posts p where p.user_id = target_user_id
           )
        union
        select child.id
        from public.comments child
        join comments_to_remove parent on child.parent_id = parent.id
    )
    delete from public.comment_reports
    where reported_by = target_user_id
       or comment_id in (select id from comments_to_remove);

    with recursive comments_to_remove(id) as (
        select c.id
        from public.comments c
        where c.user_id = target_user_id
           or c.post_id in (
                select p.id from public.posts p where p.user_id = target_user_id
           )
        union
        select child.id
        from public.comments child
        join comments_to_remove parent on child.parent_id = parent.id
    )
    delete from public.notifications
    where related_comment_id in (select id from comments_to_remove);

    with recursive comments_to_remove(id) as (
        select c.id
        from public.comments c
        where c.user_id = target_user_id
           or c.post_id in (
                select p.id from public.posts p where p.user_id = target_user_id
           )
        union
        select child.id
        from public.comments child
        join comments_to_remove parent on child.parent_id = parent.id
    )
    delete from public.comments
    where id in (select id from comments_to_remove);

    -- Reports about the user's posts and reports submitted by the user.
    delete from public.post_reports
    where reported_by = target_user_id
       or post_id in (
            select id from public.posts where user_id = target_user_id
       );

    delete from public.votes
    where user_id = target_user_id
       or post_id in (
            select id from public.posts where user_id = target_user_id
       );

    delete from public.saved_posts
    where user_id = target_user_id
       or post_id in (
            select id from public.posts where user_id = target_user_id
       );
    delete from public.notification_queue
    where post_id in (select id from public.posts where user_id = target_user_id);

    delete from public.posts where user_id = target_user_id;

    -- Remove account-owned metadata and references that otherwise prevent profile deletion.
    delete from public.badges where user_id = target_user_id;
    delete from public.user_seed_registry where profile_id = target_user_id;
    delete from public.admin_roles
    where user_id = target_user_id or granted_by = target_user_id;
    delete from public.user_suspensions where user_id = target_user_id;
    update public.user_suspensions
    set suspended_by = null
    where suspended_by = target_user_id;

    delete from public.device_tokens where user_id = target_user_id;
    delete from public.user_preferences where user_id = target_user_id;

    -- Preserve moderation audit rows but detach the deleted account as actor.
    update public.admin_actions set admin_id = null where admin_id = target_user_id;

    delete from public.profiles where id = target_user_id;
    delete from auth.users where id = target_user_id;
end;
$function$;

revoke all on function public.delete_user_account(uuid) from public;
revoke all on function public.delete_user_account(uuid) from anon;
grant execute on function public.delete_user_account(uuid) to authenticated;
