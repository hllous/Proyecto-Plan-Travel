# One Travel Group per User (hard database constraint)

Each User may belong to at most one Travel Group at a time. This is enforced as a hard constraint at the database level (unique index on `group_members.user_id`), not merely hidden in the UI.

Multi-group membership was considered — the app previously shipped a multi-group list UI and `SelectedGroupHolder` to track the active group. Both are being removed. The Destinations tab, Expense tab, and all group-scoped screens assume exactly one group per user; a group selector would add navigation complexity with no product benefit at current scale.

The trade-off: a User who wants to plan a second trip must leave their current group first, or create a second account. This is acceptable for MVP. Multi-group support can be reintroduced in a future version by removing the unique constraint and restoring a group-selection layer.
