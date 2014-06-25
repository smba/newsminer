CREATE TABLE entity_locations
(
  name text NOT NULL,
  latitude double precision NOT NULL,
  longitude double precision NOT NULL,
  CONSTRAINT entity_locations_pkey PRIMARY KEY (name)
);