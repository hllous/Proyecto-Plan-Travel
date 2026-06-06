select
  count(*) as total_destinations,
  count(*) filter (where display_photo_url is not null) as with_display_photo,
  count(*) filter (where google_photo_url is not null or wikipedia_photo_url is not null) as with_remote_photo,
  count(*) filter (where display_photo_url like 'fallback://region/%') as with_region_fallback,
  count(*) filter (where display_photo_url is null) as still_missing
from public.destinations
where is_active = true;

select
  region,
  count(*) as total_destinations,
  count(*) filter (where google_photo_url is not null or wikipedia_photo_url is not null) as with_remote_photo,
  count(*) filter (where display_photo_url like 'fallback://region/%') as with_region_fallback,
  count(*) filter (where display_photo_url is null) as still_missing
from public.destinations
where is_active = true
group by region
order by region;
