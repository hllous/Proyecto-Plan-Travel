ALTER TABLE public.expense_groups
ADD COLUMN IF NOT EXISTS category text;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'expense_groups_category_check'
  ) THEN
    ALTER TABLE public.expense_groups
    ADD CONSTRAINT expense_groups_category_check
    CHECK (
      category IS NULL
      OR category IN ('comida', 'transporte', 'alojamiento', 'entretenimiento', 'otros')
    );
  END IF;
END $$;
