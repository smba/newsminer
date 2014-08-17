CREATE TABLE rss_articles_entity_organizations
(
  link text NOT NULL,
  name text NOT NULL,
  CONSTRAINT rss_articles_entity_organizations_pkey PRIMARY KEY (link, name),
  CONSTRAINT rss_articles_entity_organizations_link_fkey FOREIGN KEY (link)
    REFERENCES rss_articles (link) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT rss_articles_entity_organizations_name_fkey FOREIGN KEY (name)
    REFERENCES entity_organizations (name) MATCH SIMPLE
    ON UPDATE CASCADE ON DELETE CASCADE
);