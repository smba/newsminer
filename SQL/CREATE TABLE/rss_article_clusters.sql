CREATE TABLE rss_article_clusters
(
  id serial NOT NULL,
  articles text[] NOT NULL DEFAULT '{}'::text[],
  entity_locations text[] NOT NULL DEFAULT '{}'::text[],
  entity_organizations text[] NOT NULL DEFAULT '{}'::text[],
  entity_persons text[] NOT NULL DEFAULT '{}'::text[],
  CONSTRAINT rss_article_clusters_pkey PRIMARY KEY (id)
);