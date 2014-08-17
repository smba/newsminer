CREATE TABLE rss_article_clusters
(
  id serial NOT NULL,
  "timestamp" bigint NOT NULL,
  score double precision NOT NULL,
  common_entities double precision NOT NULL, 
  CONSTRAINT rss_article_clusters_pkey PRIMARY KEY (id)
);
