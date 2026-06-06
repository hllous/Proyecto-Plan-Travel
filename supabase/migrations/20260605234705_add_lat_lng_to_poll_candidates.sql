ALTER TABLE public.poll_candidates
ADD COLUMN lat double precision NOT NULL DEFAULT 0.0,
ADD COLUMN lng double precision NOT NULL DEFAULT 0.0;
