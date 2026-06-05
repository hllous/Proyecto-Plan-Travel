create table public.itinerary_events (
  id                    uuid        primary key default gen_random_uuid(),
  group_id              uuid        not null references public.travel_groups(id) on delete cascade,
  name                  text        not null,
  date                  date        not null,
  time_of_day           time without time zone,
  description           text,
  place_id              text,
  created_by_member_id  uuid        not null references public.group_members(id) on delete cascade,
  created_at            timestamptz not null default now()
);

alter table public.itinerary_events enable row level security;

create policy "itinerary_events: members read"
  on public.itinerary_events for select
  to authenticated
  using (public.is_group_member(group_id));

create policy "itinerary_events: members insert"
  on public.itinerary_events for insert
  to authenticated
  with check (public.is_group_member(group_id));

create policy "itinerary_events: members update"
  on public.itinerary_events for update
  to authenticated
  using (public.is_group_member(group_id))
  with check (public.is_group_member(group_id));

create policy "itinerary_events: members delete"
  on public.itinerary_events for delete
  to authenticated
  using (public.is_group_member(group_id));

alter table public.itinerary_events replica identity full;

alter publication supabase_realtime add table public.itinerary_events;
