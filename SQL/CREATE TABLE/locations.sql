CREATE TABLE locations
(
  name text NOT NULL,
  latitude double precision NOT NULL,
  longitude double precision NOT NULL,
  CONSTRAINT locations_pkey PRIMARY KEY (name)
);