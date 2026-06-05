create table public.poll_candidates (
  id                  uuid        primary key default gen_random_uuid(),
  poll_id             uuid        not null references public.group_polls(id) on delete cascade,
  place_id            text        not null,
  name                text        not null,
  photo_url           text        not null,
  added_by_member_id  uuid        not null references public.group_members(id) on delete cascade,
  created_at          timestamptz not null default now(),
  unique (poll_id, place_id)
);

alter table public.poll_candidates enable row level security;

create policy "poll_candidates: members read"
  on public.poll_candidates for select
  to authenticated
  using (
    exists (
      select 1 from public.group_polls gp
      join public.group_members gm on gm.group_id = gp.group_id
      where gp.id = poll_id
        and gm.user_id = (select auth.uid())
    )
  );

create policy "poll_candidates: members insert"
  on public.poll_candidates for insert
  to authenticated
  with check (
    exists (
      select 1 from public.group_polls gp
      join public.group_members gm on gm.group_id = gp.group_id
      where gp.id = poll_id
        and gm.user_id = (select auth.uid())
    )
  );

alter table public.poll_candidates replica identity full;

alter publication supabase_realtime add table public.poll_candidates;
