create table public.poll_votes (
  id           uuid        primary key default gen_random_uuid(),
  candidate_id uuid        not null references public.poll_candidates(id) on delete cascade,
  member_id    uuid        not null references public.group_members(id) on delete cascade,
  created_at   timestamptz not null default now(),
  unique (candidate_id, member_id)
);

alter table public.poll_votes enable row level security;

create policy "poll_votes: members read"
  on public.poll_votes for select
  to authenticated
  using (
    exists (
      select 1 from public.poll_candidates pc
      join public.group_polls gp on gp.id = pc.poll_id
      join public.group_members gm on gm.group_id = gp.group_id
      where pc.id = candidate_id
        and gm.user_id = (select auth.uid())
    )
  );

create policy "poll_votes: members insert"
  on public.poll_votes for insert
  to authenticated
  with check (
    exists (
      select 1 from public.poll_candidates pc
      join public.group_polls gp on gp.id = pc.poll_id
      join public.group_members gm on gm.group_id = gp.group_id
      where pc.id = candidate_id
        and gm.user_id = (select auth.uid())
    )
  );

create policy "poll_votes: members delete own"
  on public.poll_votes for delete
  to authenticated
  using (
    member_id in (
      select id from public.group_members
      where user_id = (select auth.uid())
    )
  );

alter table public.poll_votes replica identity full;

alter publication supabase_realtime add table public.poll_votes;
