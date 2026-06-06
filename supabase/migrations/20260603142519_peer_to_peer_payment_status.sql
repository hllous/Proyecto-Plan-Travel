CREATE TABLE public.peer_to_peer_payment_status (
  from_member_id UUID NOT NULL REFERENCES public.group_members(id) ON DELETE CASCADE,
  to_member_id UUID NOT NULL REFERENCES public.group_members(id) ON DELETE CASCADE,
  expense_group_id UUID NOT NULL REFERENCES public.expense_groups(id) ON DELETE CASCADE,
  debtor_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  creditor_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (from_member_id, to_member_id, expense_group_id)
);

ALTER TABLE public.peer_to_peer_payment_status ENABLE ROW LEVEL SECURITY;

CREATE POLICY "involved_read" ON public.peer_to_peer_payment_status
  FOR SELECT TO authenticated
  USING (
    from_member_id IN (SELECT id FROM public.group_members WHERE user_id = auth.uid())
    OR to_member_id IN (SELECT id FROM public.group_members WHERE user_id = auth.uid())
  );

CREATE POLICY "involved_insert" ON public.peer_to_peer_payment_status
  FOR INSERT TO authenticated
  WITH CHECK (
    from_member_id IN (SELECT id FROM public.group_members WHERE user_id = auth.uid())
    OR to_member_id IN (SELECT id FROM public.group_members WHERE user_id = auth.uid())
  );

CREATE POLICY "debtor_update" ON public.peer_to_peer_payment_status
  FOR UPDATE TO authenticated
  USING (from_member_id IN (SELECT id FROM public.group_members WHERE user_id = auth.uid()));

CREATE POLICY "creditor_update" ON public.peer_to_peer_payment_status
  FOR UPDATE TO authenticated
  USING (to_member_id IN (SELECT id FROM public.group_members WHERE user_id = auth.uid()));
