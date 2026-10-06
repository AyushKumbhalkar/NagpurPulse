revoke execute on function public.is_admin(uuid) from public,anon;
grant execute on function public.is_admin(uuid) to authenticated;
revoke execute on function public.is_super_admin(uuid) from public,anon;
grant execute on function public.is_super_admin(uuid) to authenticated;