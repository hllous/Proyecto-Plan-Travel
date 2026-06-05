ALTER TABLE public.expense_groups
ADD COLUMN IF NOT EXISTS pinned_at timestamptz;
