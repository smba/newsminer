CREATE TABLE rss_feeds
(
  id serial NOT NULL,
  source_url text NOT NULL,
  name text NOT NULL,
  country character(2) NOT NULL,
  CONSTRAINT rss_feeds_pkey PRIMARY KEY (id)
);