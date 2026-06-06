ALTER TABLE public.profiles
ADD COLUMN IF NOT EXISTS mp_alias text;
