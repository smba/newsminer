CREATE TABLE rss_article_clusters_entity_organizations
(
  id integer NOT NULL,
  name text NOT NULL,
  score double precision NOT NULL,
  CONSTRAINT rss_article_clusters_entity_organizations_pkey PRIMARY KEY (id, name),
  CONSTRAINT rss_article_clusters_entity_organizations_id_fkey FOREIGN KEY (id)
    REFERENCES rss_article_clusters (id) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT rss_article_clusters_entity_organizations_name_fkey FOREIGN KEY (name)
    REFERENCES entity_organizations (name) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);