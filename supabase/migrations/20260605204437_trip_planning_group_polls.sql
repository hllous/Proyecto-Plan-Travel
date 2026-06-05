create table public.group_polls (
  id              uuid        primary key default gen_random_uuid(),
  group_id        uuid        not null references public.travel_groups(id) on delete cascade,
  type            text        not null check (type in ('destination', 'activity')),
  state           text        not null default 'open' check (state in ('open', 'closed')),
  expires_at      timestamptz,
  winner_place_id text,
  created_at      timestamptz not null default now()
);

-- enforce one active poll per group at DB level
create unique index group_polls_one_active_per_group
  on public.group_polls (group_id)
  where (state = 'open');

alter table public.group_polls enable row level security;

create policy "group_polls: members read"
  on public.group_polls for select
  to authenticated
  using (public.is_group_member(group_id));

create policy "group_polls: admin insert"
  on public.group_polls for insert
  to authenticated
  with check (public.is_group_admin(group_id));

create policy "group_polls: admin update"
  on public.group_polls for update
  to authenticated
  using (public.is_group_admin(group_id))
  with check (public.is_group_admin(group_id));

alter table public.group_polls replica identity full;

alter publication supabase_realtime add table public.group_polls;
