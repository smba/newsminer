CREATE TABLE rss_articles
(
  id serial NOT NULL,
  link text NOT NULL,
  feed_id integer NOT NULL,
  "timestamp" bigint NOT NULL,
  title text NOT NULL,
  description text NOT NULL,
  text text,
  entity_locations text[] NOT NULL DEFAULT '{}'::text[],
  entity_organizations text[] NOT NULL DEFAULT '{}'::text[],
  entity_persons text[] NOT NULL DEFAULT '{}'::text[],
  CONSTRAINT rss_articles_pkey PRIMARY KEY (id),
  CONSTRAINT rss_articles_feed_id_fkey FOREIGN KEY (feed_id)
    REFERENCES rss_feeds (id) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);