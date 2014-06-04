from django.conf.urls import patterns, include, url
from ClickDummyNM import views
from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'ClickDummyNM.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),
    url(r'^$', views.index, name = 'index'),
    url(r'^dossier', views.dossier, name = 'dossier'),
    url(r'^map', views.map, name = 'dossier'),
    #url(r'^admin/', include(admin.site.urls)),
)
