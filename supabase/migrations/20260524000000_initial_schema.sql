-- ─── tables ──────────────────────────────────────────────────────────────────

create table public.profiles (
  id           uuid        primary key references auth.users on delete cascade,
  display_name text        not null,
  phone        text,
  avatar_url   text,
  created_at   timestamptz not null default now()
);

create table public.travel_groups (
  id            uuid        primary key default gen_random_uuid(),
  name          text        not null,
  admin_user_id uuid        not null references public.profiles(id) on delete cascade,
  created_at    timestamptz not null default now()
);

create table public.group_members (
  id         uuid        primary key default gen_random_uuid(),
  group_id   uuid        not null references public.travel_groups(id) on delete cascade,
  user_id    uuid        not null references public.profiles(id) on delete cascade,
  role       text        not null check (role in ('ADMIN', 'USER')),
  created_at timestamptz not null default now(),
  unique (group_id, user_id)
);

create table public.invite_tokens (
  code       text        primary key,
  group_id   uuid        not null references public.travel_groups(id) on delete cascade,
  expires_at timestamptz not null,
  created_at timestamptz not null default now()
);

create table public.expense_items (
  id                uuid        primary key default gen_random_uuid(),
  group_id          uuid        not null references public.travel_groups(id) on delete cascade,
  name              text        not null,
  total_price_cents bigint      not null check (total_price_cents >= 0),
  quantity          int         not null check (quantity > 0),
  created_at        timestamptz not null default now()
);

create table public.item_assignments (
  item_id   uuid not null references public.expense_items(id) on delete cascade,
  member_id uuid not null references public.group_members(id) on delete cascade,
  quantity  int  not null check (quantity > 0),
  primary key (item_id, member_id)
);

-- ─── enable RLS ──────────────────────────────────────────────────────────────

alter table public.profiles        enable row level security;
alter table public.travel_groups   enable row level security;
alter table public.group_members   enable row level security;
alter table public.invite_tokens   enable row level security;
alter table public.expense_items   enable row level security;
alter table public.item_assignments enable row level security;

-- ─── helper functions ────────────────────────────────────────────────────────
-- Defined after tables so Postgres can validate the function bodies.

create or replace function public.is_group_member(p_group_id uuid)
returns boolean
language sql
security definer
stable
as $$
  select exists (
    select 1 from public.group_members
    where group_id = p_group_id
      and user_id = auth.uid()
  );
$$;

create or replace function public.is_group_admin(p_group_id uuid)
returns boolean
language sql
security definer
stable
as $$
  select exists (
    select 1 from public.group_members
    where group_id = p_group_id
      and user_id = auth.uid()
      and role = 'ADMIN'
  );
$$;

-- ─── policies: profiles ──────────────────────────────────────────────────────

create policy "profiles: authenticated read"
  on public.profiles for select
  to authenticated
  using (true);

create policy "profiles: insert own"
  on public.profiles for insert
  to authenticated
  with check (id = auth.uid());

create policy "profiles: update own"
  on public.profiles for update
  to authenticated
  using (id = auth.uid());

-- ─── policies: travel_groups ─────────────────────────────────────────────────

create policy "travel_groups: member read"
  on public.travel_groups for select
  to authenticated
  using (public.is_group_member(id));

create policy "travel_groups: authenticated insert"
  on public.travel_groups for insert
  to authenticated
  with check (admin_user_id = auth.uid());

create policy "travel_groups: admin update"
  on public.travel_groups for update
  to authenticated
  using (public.is_group_admin(id));

create policy "travel_groups: admin delete"
  on public.travel_groups for delete
  to authenticated
  using (public.is_group_admin(id));

-- ─── policies: group_members ─────────────────────────────────────────────────

create policy "group_members: member read"
  on public.group_members for select
  to authenticated
  using (public.is_group_member(group_id));

create policy "group_members: insert own"
  on public.group_members for insert
  to authenticated
  with check (user_id = auth.uid());

create policy "group_members: admin delete any"
  on public.group_members for delete
  to authenticated
  using (public.is_group_admin(group_id));

-- ─── policies: invite_tokens ─────────────────────────────────────────────────

create policy "invite_tokens: public read"
  on public.invite_tokens for select
  using (true);

create policy "invite_tokens: admin insert"
  on public.invite_tokens for insert
  to authenticated
  with check (public.is_group_admin(group_id));

create policy "invite_tokens: admin delete"
  on public.invite_tokens for delete
  to authenticated
  using (public.is_group_admin(group_id));

-- ─── policies: expense_items ─────────────────────────────────────────────────

create policy "expense_items: member read"
  on public.expense_items for select
  to authenticated
  using (public.is_group_member(group_id));

create policy "expense_items: admin insert"
  on public.expense_items for insert
  to authenticated
  with check (public.is_group_admin(group_id));

create policy "expense_items: admin update"
  on public.expense_items for update
  to authenticated
  using (public.is_group_admin(group_id));

create policy "expense_items: admin delete"
  on public.expense_items for delete
  to authenticated
  using (public.is_group_admin(group_id));

-- ─── policies: item_assignments ──────────────────────────────────────────────

create policy "item_assignments: member read"
  on public.item_assignments for select
  to authenticated
  using (
    exists (
      select 1 from public.expense_items ei
      where ei.id = item_id
        and public.is_group_member(ei.group_id)
    )
  );

create policy "item_assignments: member insert own"
  on public.item_assignments for insert
  to authenticated
  with check (
    exists (
      select 1 from public.group_members gm
      where gm.id = member_id
        and gm.user_id = auth.uid()
    )
  );

create policy "item_assignments: member update own"
  on public.item_assignments for update
  to authenticated
  using (
    exists (
      select 1 from public.group_members gm
      where gm.id = member_id
        and gm.user_id = auth.uid()
    )
  );

create policy "item_assignments: member delete own"
  on public.item_assignments for delete
  to authenticated
  using (
    exists (
      select 1 from public.group_members gm
      where gm.id = member_id
        and gm.user_id = auth.uid()
    )
  );
