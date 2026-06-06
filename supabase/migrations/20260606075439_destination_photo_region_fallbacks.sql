update public.destinations
set display_photo_url = case region
  when 'Patagonia' then 'fallback://region/patagonia'
  when 'Cuyo' then 'fallback://region/cuyo'
  when 'Noroeste' then 'fallback://region/noroeste'
  when 'Litoral' then 'fallback://region/litoral'
  when 'Buenos Aires' then 'fallback://region/buenos_aires'
  when 'Córdoba' then 'fallback://region/cordoba'
  else 'fallback://region/argentina'
end,
updated_at = now()
where is_active = true
  and display_photo_url is null;
