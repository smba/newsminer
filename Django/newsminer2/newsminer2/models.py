# -*- coding: utf-8 -*-

"""
@author: Stefan Muehlbauer
Models for accessing the Postgres Database Scheme,
"""

from __future__ import unicode_literals
from django.db import models

"""
Wraps locations.
"""
class EntityLocations(models.Model):
    name        = models.TextField(primary_key = True)
    description = models.TextField(blank=True)
    popularity  = models.FloatField()
    latitude    = models.FloatField(blank=True)
    longitude   = models.FloatField(blank=True)
    
    class Meta:
        managed  = False
        db_table = 'entity_locations'

"""
Wraps organizations.
"""  
class EntityOrganizations(models.Model):
    name        = models.TextField(primary_key=True)
    description = models.TextField(blank=True)
    popularity  = models.FloatField()
    
    class Meta:
        managed  = False
        db_table = 'entity_organizations'

"""
Basic RSS feed class.
"""
class RssFeeds(models.Model):
    source_url = models.TextField(primary_key=True)
    name       = models.TextField()
    country    = models.CharField(max_length=2)
    
    class Meta:
        managed  = False
        db_table = 'rss_feeds'

"""
Wraps persons.
"""
class EntityPersons(models.Model):
    name           = models.TextField(primary_key=True)
    description    = models.TextField(blank=True)
    popularity     = models.FloatField()
    image          = models.TextField(blank=True)
    notable_for    = models.TextField(blank=True)
    date_of_birth  = models.TextField(blank=True)
    place_of_birth = models.TextField(blank=True)
    
    class Meta:
        managed  = False
        db_table = 'entity_persons'

"""
Basic RSS article class.
"""
class RssArticles(models.Model):
    link        = models.TextField(primary_key=True)
    source_url  = models.ForeignKey('RssFeeds', db_column='source_url')
    timestamp   = models.BigIntegerField()
    title       = models.TextField()
    description = models.TextField()
    text        = models.TextField()
    
    class Meta:
        managed  = False
        db_table = 'rss_articles'

"""
Relation between RssArticles and EntityLocations.
"""
class RssArticlesEntityLocations(models.Model):
    link = models.ForeignKey('RssArticles', db_column='link', primary_key=True)
    name = models.ForeignKey('EntityLocations', db_column='name')

    class Meta:
        managed  = False
        db_table = 'rss_articles_entity_locations'

"""
Relation between RssArticles and EntityOrganizations.
"""
class RssArticlesEntityOrganizations(models.Model):
    link = models.ForeignKey('RssArticles', db_column='link', primary_key=True)
    name = models.ForeignKey('EntityOrganizations', db_column='name')
    
    class Meta:
        managed  = False
        db_table = 'rss_articles_entity_organizations'

"""
Relation between RssArticles and EntityPersons.
"""
class RssArticlesEntityPersons(models.Model):
    link = models.ForeignKey('RssArticles', db_column='link', primary_key=True)
    name = models.ForeignKey('EntityPersons', db_column='name')
    
    class Meta:
        managed  = False
        db_table = 'rss_articles_entity_persons'

"""
Basic article cluster class.
"""
class RssArticleClusters(models.Model):
    id              = models.IntegerField(primary_key = True)
    score           = models.FloatField()
    common_entities = models.FloatField()
    timestamp   = models.BigIntegerField()
    class Meta:
        managed  = False
        db_table = 'rss_article_clusters'

"""
Relation between RssArticleClusters and RssArticles.
"""
class RssArticleClustersEntityRssArticles(models.Model):
    id    = models.ForeignKey('RssArticleClusters', db_column='id', primary_key=True)
    link  = models.ForeignKey('RssArticles', db_column='link')
    score = models.FloatField()
    #key = CompositeField(('link', 'name'))
    #key.both.primary_key = True
    class Meta:
        managed  = False
        db_table = 'rss_article_clusters_rss_articles'

"""
Relation between RssArticleClusters and EntityLocations.
"""
class RssArticleClustersEntityLocations(models.Model):
    id    = models.ForeignKey('RssArticleClusters', db_column='id', primary_key=True)
    name  = models.ForeignKey('EntityLocations', db_column='name')
    score = models.FloatField()
    
    class Meta:
        managed  = False
        db_table = 'rss_article_clusters_entity_locations'

"""
Relation between RssArticleClusters and EntityOrganizations.
"""
class RssArticleClustersEntityOrganizations(models.Model):
    id    = models.ForeignKey('RssArticleClusters', db_column='id', primary_key=True)
    name  = models.ForeignKey('EntityOrganizations', db_column='name')
    score = models.FloatField()
    
    class Meta:
        managed  = False
        db_table = 'rss_article_clusters_entity_organizations'

"""
Relation between RssArticleClusters and EntityPersons.
"""
class RssArticleClustersEntityPersons(models.Model):
    id    = models.ForeignKey('RssArticleClusters', db_column='id', primary_key=True)
    name  = models.ForeignKey('EntityPersons', db_column='name')
    score = models.FloatField()
    
    class Meta:
        managed  = False
        db_table = 'rss_article_clusters_entity_persons'