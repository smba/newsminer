from django.conf.urls import patterns, include, url
from ClickDummyNM import views
from django.contrib import admin
#from dh5bp.urls import urlpatterns as dh5bp_urls
#admin.autodiscover()

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'ClickDummyNM.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),
    url(r'^$', views.index, name = 'index'),
    url(r'^dossier/(\d+)/$', views.dossier, name = 'dossier'),
    url(r'^what', views.what, name = 'what'),
    #url(r'^dossier', views.dossier, name = 'dossier'),
    url(r'^map', views.map, name = 'dossier'),
    url(r'^impressum', views.impressum, name = 'impressum'),
    #url(r'^admin/', include(admin.site.urls)),
)

#urlpatterns += dh5bp_urls