# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from django.db import models
from dbarray import TextArrayField
from django.contrib.postgres.fields import ArrayField
#from django.contrib.postgres import 

"""
@author     : Rebecca Finster, Stefan Muehlbauer
@see        : pip django-dbarray
"""


class RssFeeds(models.Model):
    source_url = models.TextField(primary_key=True)
    name = models.TextField()
    class Meta:
        managed = False
        db_table = 'rss_feeds'


class RssArticles(models.Model):
    source_url = models.ForeignKey('RssFeeds', db_column='source_url')
    link = models.TextField(primary_key=True)
    timestamp = models.BigIntegerField()
    title = models.TextField()
    description = models.TextField()
    text = models.TextField(blank=True)
    
    entity_locations = ArrayField(models.TextField())
    entity_organizations = ArrayField(models.TextField())
    entity_persons = ArrayField(models.TextField())
    cluster_id = models.IntegerField(blank=True)# models.ForeignKey('RssArticleClusters', blank)
    class Meta:
        managed = False
        db_table = 'rss_articles'
    
class RssArticleClusters(models.Model):
    class Meta:
        managed = False
        db_table = 'rss_article_clusters'
    id = models.IntegerField(primary_key = True)
    
    locations = ArrayField(models.TextField())
    organizations = ArrayField(models.TextField())
    persons = ArrayField(models.TextField())
    
class Locations(models.Model):
    class Meta:
        managed = False
        db_table = 'locations'
    name = models.CharField(max_length=100, primary_key = True)
    latitude = models.FloatField()
    longitude = models.FloatField()
    
class EntityPersons(models.Model):
    class Meta:
        managed = False
        db_table = 'entity_persons'
    name = models.TextField(primary_key=True)
    description = models.TextField()
    image = models.TextField()
    notable_for = models.TextField()
    date_of_birth = models.TextField()
    place_of_birth = models.TextField()
    
class EntityOrganizations(models.Model):
    name = models.TextField(primary_key=True)
    description = models.TextField()
    class Meta:
        managed = False
        db_table = 'entity_organizations'

class EntityLocations(models.Model):
    name = models.TextField(primary_key = True)
    description = models.TextField()
    latitude = models.FloatField()
    longitude = models.FloatField()
    class Meta:
        managed = False
        db_table = 'entity_locations' 