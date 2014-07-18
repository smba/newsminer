CREATE TABLE rss_article_clusters
(
  id serial NOT NULL,
  articles integer[] NOT NULL DEFAULT '{}'::integer[],
  locations text[] NOT NULL DEFAULT '{}'::text[],
  organizations text[] NOT NULL DEFAULT '{}'::text[],
  persons text[] NOT NULL DEFAULT '{}'::text[],
  CONSTRAINT rss_article_clusters_pkey PRIMARY KEY (id)
);