# -*- coding: utf-8 -*-
from django.shortcuts import render
from django.http import HttpResponse
from django.template import RequestContext
from django.shortcuts import render_to_response
from ClickDummyNM.models import Locations, RssFeeds, RssArticles

#from django.template import Template, Context
#from django.template.loader import get_template

def index(request):
    context = RequestContext(request)
    #one_entry = Locations.objects.filter(name='Hallo')
    #print one_entry
    a = RssArticles.objects.all()
    for article in a:
        print article.entity_locations
    context_dict = {'content_text': "Welcome to NewsMiner+", 
                    'dossier':"/dossier/"}
    return render_to_response('index.html', context_dict, context)

def dossier(request):
    context = RequestContext(request)
    context_dict = {'dossier_title': "Welcome to NewsMiner+", 
                    'article_text':"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Option congue nihil imperdiet doming id quod mazim placerat facer",
                    'widget_1': "WIKI-Bild",
                    'widget_2': "WIKI-Box",
                    'widget_3': "Google-Maps",
                    }
    return render_to_response('dossier.html', context_dict, context)

def map(request):
    context = RequestContext(request)
    #empty context_dict
    context_dict = {
                    'locations': [{
                                   "name": 'Braunschweig',
                                   "latlng": (52.16, 10,31)
                                   },
                                  {
                                   "name": 'København',
                                   "latlng": (55.41, 12.35)
                                   },
                                  {
                                   "name": 'Gøteborg',
                                   "latlng": (57.42, 11.57)
                                   },
                                  {
                                   "name": "Basel",
                                   "latlng": (47.34, 07.36)
                                   }
                                  ]
                    }
    #center berechnen
    i = 0.
    lat = 0.0
    lng = 0.0
    for location in context_dict['locations']:
        lat  += location["latlng"][0]
        lng  += location["latlng"][1]
        i += 1
    context_dict["map_center"] = (lat/i, lng/i)
    

    return render_to_response('map.html', context_dict, context)