create or replace function public.is_admin(uid uuid)
returns boolean language sql security definer
set search_path = 'public'
as $function$
  select auth.uid() is not null
     and uid = auth.uid()
     and exists (
       select 1 from public.admin_roles
       where user_id = auth.uid()
     );
$function$;

create or replace function public.is_super_admin(uid uuid)
returns boolean language sql security definer
set search_path = 'public'
as $function$
  select auth.uid() is not null
     and uid = auth.uid()
     and exists (
       select 1 from public.admin_roles
       where user_id = auth.uid()
         and role = 'super_admin'
     );
$function$;