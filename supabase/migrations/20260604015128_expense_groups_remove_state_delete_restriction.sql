-- The DELETE policy incorrectly blocked deletion of finalized expense groups.
-- A group member should be able to delete any expense group regardless of state.
DROP POLICY "expense_groups: member delete open" ON public.expense_groups;

CREATE POLICY "expense_groups: member delete"
  ON public.expense_groups FOR DELETE
  TO authenticated
  USING (public.is_group_member(group_id));
