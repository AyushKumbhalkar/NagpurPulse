-- RLS policies invoke this SECURITY DEFINER helper while evaluating post/comment/profile visibility.
-- The helper only returns whether the supplied profile is active and does not expose profile data.
grant execute on function private.is_account_active(uuid) to anon, authenticated;