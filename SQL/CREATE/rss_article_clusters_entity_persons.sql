CREATE TABLE rss_article_clusters_entity_persons
(
  id integer NOT NULL,
  name text NOT NULL,
  score double precision NOT NULL,
  CONSTRAINT rss_article_clusters_entity_persons_pkey PRIMARY KEY (id, name),
  CONSTRAINT rss_article_clusters_entity_persons_id_fkey FOREIGN KEY (id)
    REFERENCES rss_article_clusters (id) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT rss_article_clusters_entity_persons_name_fkey FOREIGN KEY (name)
    REFERENCES entity_persons (name) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);