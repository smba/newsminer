CREATE TABLE rss_article_clusters
(
  id integer NOT NULL,
  locations text[] NOT NULL DEFAULT '{}'::text[],
  organizations text[] NOT NULL DEFAULT '{}'::text[],
  persons text[] NOT NULL DEFAULT '{}'::text[],
  CONSTRAINT rss_article_clusters_pkey PRIMARY KEY (id)
);