-- Bug A: expense_items INSERT and DELETE were restricted to admins only,
-- but any group member should be able to manage expense items.

drop policy "expense_items: admin insert" on public.expense_items;
drop policy "expense_items: admin delete" on public.expense_items;

create policy "expense_items: member insert"
  on public.expense_items for insert
  to authenticated
  with check (public.is_group_member(group_id));

create policy "expense_items: member delete"
  on public.expense_items for delete
  to authenticated
  using (public.is_group_member(group_id));
