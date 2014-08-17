CREATE TABLE rss_articles
(
  link text NOT NULL,
  source_url text NOT NULL,
  "timestamp" bigint NOT NULL,
  title text NOT NULL,
  description text NOT NULL,
  text text NOT NULL,
  CONSTRAINT rss_articles_pkey PRIMARY KEY (link),
  CONSTRAINT rss_articles_source_url_fkey FOREIGN KEY (source_url)
    REFERENCES rss_feeds (source_url) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);