CREATE TABLE entity_organizations
(
  name text NOT NULL,
  description text,
  popularity double precision NOT NULL,
  CONSTRAINT entity_organizations_pkey PRIMARY KEY (name)
);