# UUID Primary Keys Across All Tables

All database tables use `uuid` primary keys instead of `bigserial`. Supabase Auth assigns UUIDs to users in `auth.users`, so any table referencing a User is inherently UUID-based. Extending UUIDs to all other tables (groups, members, expense items, assignments, invite tokens) avoids a mixed-type schema where some foreign keys are `uuid` and others are `bigint`, which would be awkward at the relational level and would force a second type migration when the backend is wired in feature #10.

## Considered Options

- **`bigserial` for non-user tables, `uuid` for user-linked tables:** Rejected because the schema had not been created yet (no migration cost) and the mixed types would produce confusing FK relationships and require a second migration pass during #10.
