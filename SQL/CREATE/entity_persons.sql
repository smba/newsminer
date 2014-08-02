CREATE TABLE entity_persons
(
  name text NOT NULL,
  description text,
  popularity double precision NOT NULL,
  image text,
  notable_for text,
  date_of_birth text,
  place_of_birth text,
  CONSTRAINT entity_persons_pkey PRIMARY KEY (name)
);