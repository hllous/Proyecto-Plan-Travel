CREATE TABLE public.expense_groups (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id uuid NOT NULL REFERENCES public.travel_groups(id) ON DELETE CASCADE,
  name text NOT NULL,
  state text NOT NULL DEFAULT 'open' CHECK (state IN ('open', 'finalized')),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION public.enforce_expense_group_state_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF OLD.state = 'finalized' AND NEW.state = 'open' THEN
    RAISE EXCEPTION 'Cannot revert expense_group state from finalized to open';
  END IF;
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_expense_groups_state_transition
  BEFORE UPDATE ON public.expense_groups
  FOR EACH ROW
  EXECUTE FUNCTION public.enforce_expense_group_state_transition();

ALTER TABLE public.expense_groups ENABLE ROW LEVEL SECURITY;

CREATE POLICY "expense_groups: member read"
  ON public.expense_groups FOR SELECT
  TO authenticated
  USING (is_group_member(group_id));

CREATE POLICY "expense_groups: member insert"
  ON public.expense_groups FOR INSERT
  TO authenticated
  WITH CHECK (is_group_member(group_id));

CREATE POLICY "expense_groups: member delete open"
  ON public.expense_groups FOR DELETE
  TO authenticated
  USING (is_group_member(group_id) AND state = 'open');

CREATE POLICY "expense_groups: admin update"
  ON public.expense_groups FOR UPDATE
  TO authenticated
  USING (is_group_admin(group_id));

ALTER TABLE public.expense_groups REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE public.expense_groups;
