-- Allow a USER-role member to delete their own group_members row (Leave Group).
-- ADMINs are excluded: they must delete the group instead.
create policy "group_members: user delete own"
  on public.group_members for delete
  to authenticated
  using (user_id = auth.uid() and role = 'USER');
