\set ON_ERROR_STOP on
\ir fixtures/profile_privacy.sql
\ir ../migrations/20261008170000_enforce_profile_content_privacy.sql
-- Repeat deployment to verify policy/function replacement is safe.
\ir ../migrations/20261008170000_enforce_profile_content_privacy.sql

begin;
insert into public.profiles (id, hide_profile, is_deactivated) values
    ('11111111-1111-1111-1111-111111111111', false, false),
    ('22222222-2222-2222-2222-222222222222', false, false),
    ('33333333-3333-3333-3333-333333333333', false, false),
    ('44444444-4444-4444-4444-444444444444', false, false),
    ('55555555-5555-5555-5555-555555555555', false, true),
    ('66666666-6666-6666-6666-666666666666', null, false);
insert into public.admin_roles values
    ('33333333-3333-3333-3333-333333333333', 'admin');
insert into public.posts (id, user_id, is_anonymous) values
    ('00000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', false),
    ('00000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', true),
    ('00000000-0000-0000-0000-000000000003', '44444444-4444-4444-4444-444444444444', false),
    ('00000000-0000-0000-0000-000000000004', '55555555-5555-5555-5555-555555555555', false),
    ('00000000-0000-0000-0000-000000000005', '66666666-6666-6666-6666-666666666666', false),
    ('00000000-0000-0000-0000-000000000006', '77777777-7777-7777-7777-777777777777', false);
insert into public.comments (id, user_id, post_id, is_anonymous) values
    ('00000000-0000-0000-0000-000000000011', '11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000003', false),
    ('00000000-0000-0000-0000-000000000012', '11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000003', true),
    ('00000000-0000-0000-0000-000000000013', '44444444-4444-4444-4444-444444444444', '00000000-0000-0000-0000-000000000001', false),
    ('00000000-0000-0000-0000-000000000014', '44444444-4444-4444-4444-444444444444', '00000000-0000-0000-0000-000000000003', false),
    ('00000000-0000-0000-0000-000000000015', '22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000001', false),
    ('00000000-0000-0000-0000-000000000016', '11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000004', false);

create function pg_temp.assert_rows(query text, expected bigint, label text)
returns void language plpgsql as $assert$
declare actual bigint;
begin
    execute 'select count(*) from (' || query || ') actual_rows' into actual;
    if actual <> expected then
        raise exception '%: expected % rows, got %', label, expected, actual;
    end if;
    raise notice 'PASS: %', label;
end;
$assert$;

set local role anon;
select set_config('request.jwt.claim.sub', '', true);
select pg_temp.assert_rows('select * from public.profiles', 4, 'guest sees public active profiles only');
select pg_temp.assert_rows('select * from public.posts', 3, 'guest sees public normal and anonymous posts');
select pg_temp.assert_rows('select * from public.comments', 5, 'guest cannot read comments under inactive posts');
reset role;

update public.profiles set hide_profile = true where id = '11111111-1111-1111-1111-111111111111';
-- Add extra permissive policies: they must NOT override restrictive privacy.
create policy "Legacy allow all profiles" on public.profiles for select using (true);
create policy "Legacy allow all posts" on public.posts for select using (true);
create policy "Legacy allow all comments" on public.comments for select using (true);
set local role anon;
select pg_temp.assert_rows('select * from public.profiles where id = ''11111111-1111-1111-1111-111111111111''', 0, 'guest direct hidden-profile lookup denied');
select pg_temp.assert_rows('select * from public.posts where user_id = ''11111111-1111-1111-1111-111111111111''', 0, 'guest cannot retrieve hidden author posts, including anonymous');
select pg_temp.assert_rows('select * from public.comments', 1, 'guest cannot retrieve hidden author comments or comments under hidden posts');
select pg_temp.assert_rows('select c.* from public.comments c join public.posts p on p.id = c.post_id', 1, 'joins also enforce privacy');

set local role authenticated;
select set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', true);
select pg_temp.assert_rows('select * from public.profiles where id = ''11111111-1111-1111-1111-111111111111''', 0, 'another signed-in user cannot read hidden profile');
select pg_temp.assert_rows('select * from public.posts where id = ''00000000-0000-0000-0000-000000000001''', 0, 'direct hidden-post ID lookup denied');
select pg_temp.assert_rows('select * from public.comments where user_id = ''11111111-1111-1111-1111-111111111111''', 0, 'another signed-in user cannot read hidden author comments');
select pg_temp.assert_rows('select * from public.comments where id = ''00000000-0000-0000-0000-000000000015''', 1, 'comment author retains own history under a hidden post');

select set_config('request.jwt.claim.sub', '11111111-1111-1111-1111-111111111111', true);
select pg_temp.assert_rows('select * from public.profiles where id = auth.uid()', 1, 'owner can read hidden profile without recursive RLS');
select pg_temp.assert_rows('select * from public.posts where user_id = auth.uid()', 2, 'owner can read normal and anonymous own posts');
select pg_temp.assert_rows('select * from public.comments where user_id = auth.uid()', 3, 'owner retains all own comments');
update public.profiles set hide_profile = false, hide_posts = true where id = auth.uid();
select pg_temp.assert_rows('select * from public.profiles where hide_posts and id = auth.uid()', 1, 'owner can change saved privacy flags');

select set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', true);
select pg_temp.assert_rows('select * from public.profiles where id = ''11111111-1111-1111-1111-111111111111''', 1, 'hide_posts leaves public profile readable');
select pg_temp.assert_rows('select * from public.posts where user_id = ''11111111-1111-1111-1111-111111111111''', 0, 'hide_posts prevents normal and anonymous post reads');
select pg_temp.assert_rows('select * from public.comments where user_id = ''11111111-1111-1111-1111-111111111111''', 2, 'hide_posts leaves comments on accessible posts readable');
select pg_temp.assert_rows('select * from public.comments where id = ''00000000-0000-0000-0000-000000000013''', 0, 'public author cannot leak a hidden parent post through comments');
-- Cross-account privacy writes must still fail to affect a row.
update public.profiles set hide_posts = false where id = '11111111-1111-1111-1111-111111111111';
select pg_temp.assert_rows('select * from public.posts where user_id = ''11111111-1111-1111-1111-111111111111''', 0, 'other users cannot switch privacy off');

reset role;
update public.profiles set hide_posts = false, hide_comments = true where id = '11111111-1111-1111-1111-111111111111';
set local role authenticated;
select pg_temp.assert_rows('select * from public.posts where user_id = ''11111111-1111-1111-1111-111111111111''', 2, 'hide_comments does not hide posts');
select pg_temp.assert_rows('select * from public.comments where user_id = ''11111111-1111-1111-1111-111111111111''', 0, 'hide_comments prevents normal and anonymous comment reads');
select pg_temp.assert_rows('select * from public.comments where id = ''00000000-0000-0000-0000-000000000014''', 1, 'unrelated public content remains readable');

reset role;
update public.profiles set hide_profile = true, hide_posts = true where id = '11111111-1111-1111-1111-111111111111';
update public.profiles set is_verified = true where id = '22222222-2222-2222-2222-222222222222';
set local role authenticated;
select pg_temp.assert_rows('select * from public.posts where user_id = ''11111111-1111-1111-1111-111111111111''', 0, 'verified profile is not an administrator');
select set_config('request.jwt.claim.sub', '33333333-3333-3333-3333-333333333333', true);
select pg_temp.assert_rows('select * from public.profiles where id = ''11111111-1111-1111-1111-111111111111''', 1, 'trusted administrator retains moderation profile access');
select pg_temp.assert_rows('select * from public.posts where user_id = ''11111111-1111-1111-1111-111111111111''', 2, 'trusted administrator retains moderation post access');
select pg_temp.assert_rows('select * from public.comments where user_id = ''11111111-1111-1111-1111-111111111111''', 2, 'trusted administrator retains moderation comment access');
select pg_temp.assert_rows('select * from public.posts where user_id = ''55555555-5555-5555-5555-555555555555''', 0, 'privacy exception does not weaken account-deactivation policy');

select set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', true);
select pg_temp.assert_rows('select * from public.posts where user_id = ''66666666-6666-6666-6666-666666666666''', 0, 'unknown/null privacy flag fails closed');
select pg_temp.assert_rows('select * from public.posts where user_id = ''77777777-7777-7777-7777-777777777777''', 0, 'missing profile fails closed');
select set_config('request.jwt.claim.sub', '55555555-5555-5555-5555-555555555555', true);
select pg_temp.assert_rows('select * from public.profiles where id = auth.uid()', 1, 'deactivated owner retains profile access for reactivation');
select pg_temp.assert_rows('select * from public.posts where user_id = auth.uid()', 0, 'deactivated owner still subject to existing activity restriction');

set local role service_role;
select pg_temp.assert_rows('select * from public.posts', 6, 'trusted backend retains explicit BYPASSRLS access');
reset role;
update public.profiles set hide_profile = false, hide_posts = false, hide_comments = false
    where id = '11111111-1111-1111-1111-111111111111';
set local role anon;
select set_config('request.jwt.claim.sub', '', true);
select pg_temp.assert_rows('select * from public.profiles where id = ''11111111-1111-1111-1111-111111111111''', 1, 'turning privacy off restores public profile access');
select pg_temp.assert_rows('select * from public.posts where user_id = ''11111111-1111-1111-1111-111111111111''', 2, 'turning privacy off restores public post access');
select pg_temp.assert_rows('select * from public.comments', 5, 'turning privacy off restores public comments but keeps inactive-parent restriction');
reset role;
rollback;
\echo Profile privacy regression checks passed.
