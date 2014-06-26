CREATE TABLE entity_locations
(
  name text NOT NULL,
  description text,
  latitude double precision,
  longitude double precision,
  CONSTRAINT entity_locations_pkey PRIMARY KEY (name)
);