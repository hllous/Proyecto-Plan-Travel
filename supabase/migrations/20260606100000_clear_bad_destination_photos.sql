-- Clear fallback tokens stored by destination_photo_region_fallbacks migration.
-- These destinations were never actually fetched; the app will re-fetch them with
-- the corrected Wikipedia geo-validation logic.
UPDATE public.destinations
SET display_photo_url = NULL,
    updated_at        = now()
WHERE is_active = true
  AND display_photo_url LIKE 'fallback://%';

-- Clear SVG thumbnails (location maps, country flags, illustrated diagrams).
-- These are technically images but visually useless as destination photos.
UPDATE public.destinations
SET display_photo_url  = NULL,
    wikipedia_photo_url = NULL,
    wikipedia_title    = NULL,
    updated_at         = now()
WHERE is_active = true
  AND (
    display_photo_url LIKE '%.svg.png'
    OR display_photo_url LIKE '%.svg'
  );

-- Clear known wrong-country / wrong-topic Wikipedia photos that slipped through
-- the direct-name lookup without coordinate validation.
UPDATE public.destinations
SET display_photo_url  = NULL,
    wikipedia_photo_url = NULL,
    wikipedia_title    = NULL,
    updated_at         = now()
WHERE is_active = true
  AND (
    -- Lima (Buenos Aires) got Lima, Peru
    display_photo_url LIKE '%Lima2017%'
    -- Goya (Corrientes) got Francisco de Goya the Spanish painter
    OR display_photo_url LIKE '%Portaña%'
    OR display_photo_url LIKE '%Francisco_de_Goya%'
    -- Montecarlo (Misiones) got Monaco's Monte Carlo
    OR display_photo_url LIKE '%Monte_carlo_ville%'
    -- La Paz destinations got La Paz, Bolivia
    OR display_photo_url LIKE '%centro_de_La_Paz%'
    -- Reconquista (Santa Fe) got a painting of the Reconquista of Spain
    OR display_photo_url LIKE '%Rendici%n_de_Granada%'
    -- Corralito (Córdoba) got Argentina's 2001 financial crisis photo
    OR display_photo_url LIKE '%De_la_R%a_con_Cavallo%'
    -- Roldán (Santa Fe) got a ship photo from Bremen, Germany
    OR display_photo_url LIKE '%Bremer_Roland%'
    -- Los Laureles (Entre Ríos) got a photo of the laurel plant
    OR display_photo_url LIKE '%Laurus_nobilis%'
    -- Mártires (Misiones) got a painting of the martyrdom of Saint Andrew
    OR display_photo_url LIKE '%martirio_de_san%'
  );
