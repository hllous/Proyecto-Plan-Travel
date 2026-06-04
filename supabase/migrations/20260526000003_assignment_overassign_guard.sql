-- Bug D: prevent over-assignment at the DB level.
-- The app validates locally before upserting, but two concurrent users can
-- both pass local validation and push the total assigned quantity above
-- the item's quantity. This trigger enforces the invariant atomically.

create or replace function public.check_assignment_quantity()
returns trigger
language plpgsql
as $$
declare
  item_quantity int;
  total_assigned int;
begin
  select quantity into item_quantity
  from public.expense_items
  where id = NEW.item_id;

  if item_quantity is null then
    raise exception 'Expense item % not found', NEW.item_id;
  end if;

  select coalesce(sum(quantity), 0) into total_assigned
  from public.item_assignments
  where item_id = NEW.item_id
    and member_id != NEW.member_id;

  if (total_assigned + NEW.quantity) > item_quantity then
    raise exception 'Over-assignment: item % has quantity %, already % assigned to others',
      NEW.item_id, item_quantity, total_assigned
      using errcode = 'check_violation';
  end if;

  return NEW;
end;
$$;

create trigger assignment_quantity_guard
  before insert or update on public.item_assignments
  for each row execute function public.check_assignment_quantity();
