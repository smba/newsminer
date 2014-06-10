# This is an auto-generated Django model module.
# You'll have to do the following manually to clean this up:
#   * Rearrange models' order
#   * Make sure each model has one field with primary_key=True
#   * Remove `managed = False` lines if you wish to allow Django to create and delete the table
# Feel free to rename the models, but don't rename db_table values or field names.
#
# Also note: You'll have to insert the output of 'django-admin.py sqlcustom [appname]'
# into your database.
from __future__ import unicode_literals

from django.db import models

class RssArticles(models.Model):
    source_url = models.ForeignKey('RssFeeds', db_column='source_url')
    link = models.TextField()
    timestamp = models.BigIntegerField()
    title = models.TextField()
    description = models.TextField()
    text = models.TextField(blank=True)
    class Meta:
        managed = False
        db_table = 'rss_articles'

class RssFeeds(models.Model):
    source_url = models.TextField(primary_key=True)
    name = models.TextField()
    class Meta:
        managed = False
        db_table = 'rss_feeds'

