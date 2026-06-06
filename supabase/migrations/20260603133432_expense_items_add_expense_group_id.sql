ALTER TABLE public.expense_items
  ADD COLUMN expense_group_id uuid REFERENCES public.expense_groups(id) ON DELETE CASCADE;

DROP POLICY IF EXISTS "expense_items: member read" ON public.expense_items;
DROP POLICY IF EXISTS "expense_items: member insert" ON public.expense_items;
DROP POLICY IF EXISTS "expense_items: member delete" ON public.expense_items;
DROP POLICY IF EXISTS "expense_items: admin update" ON public.expense_items;

CREATE POLICY "expense_items: member read"
  ON public.expense_items FOR SELECT
  TO authenticated
  USING (is_group_member(group_id));

CREATE POLICY "expense_items: member insert"
  ON public.expense_items FOR INSERT
  TO authenticated
  WITH CHECK (
    expense_group_id IS NOT NULL AND
    is_group_member(
      (SELECT eg.group_id FROM public.expense_groups eg WHERE eg.id = expense_group_id)
    )
  );

CREATE POLICY "expense_items: member delete"
  ON public.expense_items FOR DELETE
  TO authenticated
  USING (is_group_member(group_id));

CREATE POLICY "expense_items: admin update"
  ON public.expense_items FOR UPDATE
  TO authenticated
  USING (is_group_admin(group_id));
