-- Enforce one Travel Group per user at the database level.
-- A user may belong to at most one group at a time (ADR-0008).
alter table public.group_members
  add constraint group_members_user_id_unique unique (user_id);
