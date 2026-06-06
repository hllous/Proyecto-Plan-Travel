-- Denormalise group_id onto item_assignments so that:
--   1. The Realtime channel can filter server-side by group_id instead of
--      receiving every assignment change and relying solely on RLS.
--   2. The RLS SELECT policy becomes a direct is_group_member(group_id) call
--      instead of a JOIN through expense_items on every broadcasted event.
--   3. fetchAssignments can query by group_id in one round-trip instead of
--      fetching expense_items first and then filtering by item_id list.

-- 1. Add the column (nullable while we backfill).
ALTER TABLE public.item_assignments
  ADD COLUMN group_id uuid REFERENCES public.travel_groups(id) ON DELETE CASCADE;

-- 2. Backfill from expense_items — every existing assignment row gets the
--    group_id of the expense item it belongs to.
UPDATE public.item_assignments ia
SET group_id = ei.group_id
FROM public.expense_items ei
WHERE ia.item_id = ei.id;

-- 3. Enforce NOT NULL now that all rows are populated.
ALTER TABLE public.item_assignments
  ALTER COLUMN group_id SET NOT NULL;

-- 4. Index so the Realtime filter and direct queries are efficient.
CREATE INDEX idx_item_assignments_group_id
  ON public.item_assignments (group_id);

-- 5. Replace the complex JOIN-based SELECT policy with a direct membership check.
DROP POLICY "item_assignments: member read" ON public.item_assignments;

CREATE POLICY "item_assignments: member read"
  ON public.item_assignments
  FOR SELECT
  USING (is_group_member(group_id));
