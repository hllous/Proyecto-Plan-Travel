alter table public.travel_groups
  add column trip_destination_place_id text,
  add column trip_destination_name text,
  add column trip_destination_lat double precision,
  add column trip_destination_lng double precision;
