from __future__ import unicode_literals

from django.db import models

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
    class Meta:
        managed = False
        db_table = 'rss_articles'

