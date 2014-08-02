CREATE TABLE entity_locations
(
  name text NOT NULL,
  description text,
  popularity double precision NOT NULL,
  latitude double precision,
  longitude double precision,
  CONSTRAINT entity_locations_pkey PRIMARY KEY (name)
);