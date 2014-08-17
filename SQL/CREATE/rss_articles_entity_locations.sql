CREATE TABLE rss_articles_entity_locations
(
  link text NOT NULL,
  name text NOT NULL,
  CONSTRAINT rss_articles_entity_locations_pkey PRIMARY KEY (link, name),
  CONSTRAINT rss_articles_entity_locations_link_fkey FOREIGN KEY (link)
    REFERENCES rss_articles (link) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT rss_articles_entity_locations_name_fkey FOREIGN KEY (name)
    REFERENCES entity_locations (name) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);