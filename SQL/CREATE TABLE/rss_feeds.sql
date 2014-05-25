CREATE TABLE rss_feeds
(
  source_url text NOT NULL,
  name text NOT NULL,
  CONSTRAINT rss_feeds_pkey PRIMARY KEY (source_url)
);