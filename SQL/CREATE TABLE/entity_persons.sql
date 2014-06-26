CREATE TABLE entity_persons
(
  name text NOT NULL,
  description text,
  date_of_birth date,
  notable_for text,
  place_of_birth text,
  image text,
  CONSTRAINT entity_persons_pkey PRIMARY KEY (name)
);