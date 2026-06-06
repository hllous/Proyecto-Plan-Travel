create policy "group_polls: admin delete"
  on public.group_polls for delete
  to authenticated
  using (public.is_group_admin(group_id));
