CREATE TABLE rss_articles
(
  link text NOT NULL,
  source_url integer NOT NULL,
  "timestamp" bigint NOT NULL,
  title text NOT NULL,
  description text NOT NULL,
  text text,
  entity_locations text[] NOT NULL DEFAULT '{}'::text[],
  entity_organizations text[] NOT NULL DEFAULT '{}'::text[],
  entity_persons text[] NOT NULL DEFAULT '{}'::text[],
  CONSTRAINT rss_articles_pkey PRIMARY KEY (link),
  CONSTRAINT rss_articles_source_url_fkey FOREIGN KEY (source_url)
    REFERENCES rss_feeds (source_url) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);