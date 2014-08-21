from django.conf.urls import patterns, include, url
from newsminer2 import views
from django.contrib import admin
from django.views.generic import RedirectView
#from dh5bp.urls import urlpatterns as dh5bp_urls
#admin.autodiscover()

urlpatterns = patterns('',
    # Examples:
    # url(r'^$',               'newsminer2.views.home', name='home'),
    # url(r'^blog/',           include('blog.urls')),
    #url(r'^$',               views.index,   name = 'index'),
    url(r'^dossier/(\d+)/$', views.dossier, name = 'dossier'),
    url(r'^archive',            views.archive,    name = 'archive'),
    #url(r'^dossier',         views.dossier, name = 'dossier'),
    url(r'^impressum',       views.impressum, name = 'impressum'),
    url(r'^(\d+)/(\d+)/(\d+)$',     views.index, name = 'index'),
    url(r'^$', views.index_redirect, name = "index"),
)

#urlpatterns += dh5bp_urls