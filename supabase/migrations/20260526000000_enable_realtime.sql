-- Enable Supabase Realtime for tables that need live cross-device updates.
--
-- Intentionally NOT setting REPLICA IDENTITY FULL on group_members:
-- without full identity, DELETE events carry only the primary key (no group_id).
-- Supabase's RLS filter cannot evaluate the membership policy without group_id,
-- so it fails-open and broadcasts the DELETE to all connected clients.
-- This ensures the kicked member's device receives the event and transitions
-- to the no-group state, which is exactly the desired behavior.

alter publication supabase_realtime add table public.group_members;
alter publication supabase_realtime add table public.travel_groups;
alter publication supabase_realtime add table public.invite_tokens;
