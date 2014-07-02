# -*- coding: utf-8 -*-
from ClickDummyNM.models import RssArticleClusters, RssArticles, EntityLocations
from django.http import HttpResponse
from django.shortcuts import render, render_to_response
from django.template import RequestContext
from warnings import catch_warnings

#from django.template import Template, Context
#from django.template.loader import get_template

def index(request):
    context = RequestContext(request)
    
    #Determine clusters, which hold more than k articles
    articles = RssArticles.objects.all()
    dist = {}
    for article in articles:
        if article.cluster_id == None:
            continue
        elif article.cluster_id not in dist.keys():
            dist[article.cluster_id] = 1
        elif article.cluster_id in dist.keys():
            dist[article.cluster_id] += 1
    print dist
    context_dict = {'content_text': "Welcome to NewsMiner+", 
                    'dossier':"/dossier/",
                    'impressum': '/impressum/',
                    'what':'/what/',
                    'dist':dist}
    return render_to_response('index.html', context_dict, context)

def dossier(request, offset):
    context = RequestContext(request)
    
    articles = RssArticles.objects.filter(cluster_id=offset)
    locations_set = set()
    for article in articles:
        for location in article.entity_locations:
            locations_set.add(location)
    locations = []
    for location in locations_set:
        try:
            match = EntityLocations.objects.filter(name=location)[0]
            locations.append({"name":location, "latlng": (match.latitude, match.longitude)})
        except IndexError:
            continue
        
    
    context_dict = {'dossier_title': "Welcome to Dossier No " + str(offset), 
                    'article_text':"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Option congue nihil imperdiet doming id quod mazim placerat facer",
                    'widget_1': "WIKI-Bild",
                    'widget_2': "WIKI-Box",
                    'widget_3': "Google-Maps",
                    'articles': articles,
                    'home'    : '/',
                    'impressum' : '/impressum/',
                    'what'      : '/what/',
                    'locations': locations,
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

def impressum(request):
    context = RequestContext(request)
    #empty context_dict
    context_dict = {'home':'/',
                    'what':'/what/'}
    return render_to_response('impressum.html', context_dict, context)

def what(request):
    context = RequestContext(request)
    #empty context_dict
    context_dict = {'home':'/',
                    'impressum': '/impressum/'}
    return render_to_response('what.html', context_dict, context)
