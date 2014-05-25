CREATE TABLE rss_articles
(
  source_url text NOT NULL,
  link text NOT NULL,
  "timestamp" bigint NOT NULL,
  title text NOT NULL,
  description text NOT NULL,
  text text,
  CONSTRAINT rss_articles_pkey PRIMARY KEY (source_url, link),
  CONSTRAINT rss_articles_source_url_fkey FOREIGN KEY (source_url)
    REFERENCES rss_feeds (source_url) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);