create table public.destinations (
  id                  uuid        primary key default gen_random_uuid(),
  source              text        not null check (source in ('geonames', 'google')),
  source_id           text        not null,
  name                text        not null,
  normalized_name     text        not null,
  province            text        not null,
  region              text        not null check (region in ('Patagonia', 'Cuyo', 'Noroeste', 'Litoral', 'Buenos Aires', 'Córdoba')),
  country_code        text        not null default 'AR',
  lat                 double precision not null,
  lng                 double precision not null,
  population          integer     not null default 0 check (population >= 0),
  google_place_id     text,
  google_photo_url    text,
  wikipedia_title     text,
  wikipedia_photo_url text,
  display_photo_url   text,
  is_active           boolean     not null default true,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  unique (source, source_id)
);

create index destinations_region_normalized_name_idx
  on public.destinations (region, normalized_name);

create index destinations_google_place_id_idx
  on public.destinations (google_place_id);

create or replace function public.set_destinations_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger set_destinations_updated_at
before update on public.destinations
for each row
execute function public.set_destinations_updated_at();

grant select, insert, update on public.destinations to authenticated;

alter table public.destinations enable row level security;

create policy "destinations: authenticated read active"
  on public.destinations for select
  to authenticated
  using (is_active);

create policy "destinations: authenticated insert"
  on public.destinations for insert
  to authenticated
  with check ((select auth.uid()) is not null);

create policy "destinations: authenticated update"
  on public.destinations for update
  to authenticated
  using ((select auth.uid()) is not null)
  with check ((select auth.uid()) is not null);
