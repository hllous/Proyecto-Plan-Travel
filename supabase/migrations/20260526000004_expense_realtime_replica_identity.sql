-- Bug fix: DELETE events from expense_items and item_assignments were silently
-- dropped by Supabase Realtime because RLS evaluated the OLD row using only
-- the primary key (DEFAULT replica identity). Without group_id in the OLD row,
-- is_group_member() cannot be evaluated, so the event is not broadcast.
-- REPLICA IDENTITY FULL stores all column values in the OLD record.

alter table public.expense_items    replica identity full;
alter table public.item_assignments replica identity full;

-- Ensure both tables are in the Realtime publication (idempotent via DO block).
do $$
begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime' and tablename = 'expense_items'
  ) then
    alter publication supabase_realtime add table public.expense_items;
  end if;

  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime' and tablename = 'item_assignments'
  ) then
    alter publication supabase_realtime add table public.item_assignments;
  end if;
end $$;
