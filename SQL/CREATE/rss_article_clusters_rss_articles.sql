CREATE TABLE rss_article_clusters_rss_articles
(
  id integer NOT NULL,
  link text NOT NULL,
  score double precision NOT NULL,
  CONSTRAINT rss_article_clusters_rss_articles_pkey PRIMARY KEY (id, link),
  CONSTRAINT rss_article_clusters_rss_articles_id_fkey FOREIGN KEY (id)
    REFERENCES rss_article_clusters (id) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT rss_article_clusters_rss_articles_link_fkey FOREIGN KEY (link)
    REFERENCES rss_articles (link) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);