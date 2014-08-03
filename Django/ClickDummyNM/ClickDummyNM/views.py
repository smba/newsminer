# -*- coding: utf-8 -*-
from ClickDummyNM.models import RssArticleClusters, RssFeeds, RssArticles, EntityLocations, EntityOrganizations, EntityPersons
from django.http import HttpResponse
from django.shortcuts import render, render_to_response
from django.template import RequestContext
from warnings import catch_warnings

import operator

# from django.template import Template, Context
# from django.template.loader import get_template
global context_dict
context_dict = {
                'dossier':'/dossier/',
                'what':'/what/',
                'impressum':'/impressum/',
                'home':'/'
                }


def index(request):
    context = RequestContext(request)
    clusters = RssArticleClusters.objects.raw("WITH timel AS (SELECT max(timestamp) AS latest_timestamp " 
                                              +"FROM rss_article_clusters) "
                                              +",stats AS ("
                                              +"SELECT avg(array_length(articles, 1)) AS average "
                                              +"FROM rss_article_clusters, timel "
                                              +"WHERE rss_article_clusters.timestamp = timel.latest_timestamp) "
                                              +"SELECT id, articles FROM rss_article_clusters, stats "
                                              +"WHERE array_length(entity_locations, 1) < 30 AND "
                                              +"array_length(articles, 1) > 2 "
                                              +"ORDER BY score DESC")
    clusterDatas = []
    for cluster in clusters:
        clusterData = {}
        clusterData['id'] = cluster.id
        clusterData['centroid'] = RssArticles.objects.filter(link=cluster.articles[0])[0] #cluster centroid
        clusterDatas.append(clusterData)
    specific_context_dict = {
                             'clusterDatas' : clusterDatas,
                             }
    print clusterDatas
    index_context_dict = dict(context_dict.items() + specific_context_dict.items())
    return render_to_response('index.html', index_context_dict, context)

"""
View for the main page (index), which presents all recent 
topics including title and stuff.
"""
def index2(request):
    context = RequestContext(request)
    average_articles = RssArticleClusters.objects.all().aggregate(Avg(''))
    clusters = RssArticleClusters.objects.filter()
    """
    Gets the clusters, which hold more than zero articles 
    and returns their distribution.
    @param param: k most frequent entities 
    @return: dictionary with cluster id and amount of articles included AND
            dictionary with cluster is as key and the most frequent entities (not exceeding k)

    """
    def getClusters(k):
        # At first, determine clusters, which hold more than zero articles
        articles = RssArticles.objects.all()
        distribution = {}
        for article in articles:
            if article.cluster_id == None:
                continue
            elif article.cluster_id not in distribution.keys():
                distribution[article.cluster_id] = 1
            elif article.cluster_id in distribution.keys():
                distribution[article.cluster_id] += 1
        
        # At second, choose a appropriate title for each cluster,
        # based on the most frequent entities occuring.
        
        # Determine the frequency of each entity
        entitiesInCluster = {}
        for article in articles:
            if article.cluster_id not in entitiesInCluster.keys():
                entitiesInCluster[article.cluster_id] = {}
            '''    
            for location in article.entity_locations:
                if location not in entitiesInCluster[article.cluster_id].keys():
                    entitiesInCluster[article.cluster_id][location] = 1
                else:
                    entitiesInCluster[article.cluster_id][location] += 1
            '''
            for organization in article.entity_organizations:
                if organization not in entitiesInCluster[article.cluster_id].keys():
                    entitiesInCluster[article.cluster_id][organization] = 1
                else:
                    entitiesInCluster[article.cluster_id][organization] += 1
            for person in article.entity_persons:
                if person not in entitiesInCluster[article.cluster_id].keys():
                    entitiesInCluster[article.cluster_id][person] = 1
                else:
                    entitiesInCluster[article.cluster_id][person] += 1

        # Sort the frequency dicts and sort them descending by the second element, 
        # finally fetch the first x elements, not exeeding k (x <= k).
        for ckey in entitiesInCluster.keys():
            entitiesInCluster[ckey] = sorted(entitiesInCluster[ckey].iteritems(), key=operator.itemgetter(1), reverse=True)[:k]
        for ckey in entitiesInCluster.keys():
            temp = entitiesInCluster[ckey]
            entitiesInCluster[ckey] = []
            for t in temp:
                entitiesInCluster[ckey].append(t[0])
        print entitiesInCluster[124]
        return (distribution, entitiesInCluster)
    
    temp = getClusters(4)
    distribution = temp[0]  # {cluster_id : a#articles}
    topEntities = temp[1]  # [...]
    
    # content
    specific_context_dict = {
                             'content_text' : "Welcome to News Miner+",
                             'distribution' : distribution,
                             'topEntities' : topEntities
                             }
    
    index_context_dict = dict(context_dict.items() + specific_context_dict.items())            
    return render_to_response('index.html', index_context_dict, context)

'''
Generic view for the dossiers.
@param param: cluster_id which will be used as link
'''
def dossier(request, cluster_id):
    context = RequestContext(request)
    
    articles = []
    cluster = RssArticleClusters.objects.filter(id=cluster_id)[0]
    for link in cluster.articles:
        articles.append(RssArticles.objects.filter(link=link)[0])
    articlesCopy = []
    for i in range(len(articles)):
        temp = RssFeeds.objects.filter(source_url=articles[i].source_url.source_url)
        articleDict = articles[i].__dict__
        articleDict['newspaper'] = temp[0].name
        articlesCopy.append(articleDict)
    '''
    Gets the locations and estimates the map center
    '''
    def getLocations(m):
        locations_bag = {}
        for article in articles:
            for location in article.entity_locations:
                if not location in locations_bag.keys():
                    locations_bag[location] = 1
                else:
                    locations_bag[location] += 1
        
        loc_set = set()
        for location in locations_bag.keys():
            if not locations_bag[location] < m:
                loc_set.add(location)
        
        locations = []
        for location in loc_set:
            try:
                match = EntityLocations.objects.filter(name=location)[0]
                locations.append({"name":location, "latlng": (match.latitude, match.longitude)})
            except IndexError:
                continue
            
        # center berechnen
        distances = []
        for i in range(len(locations)):
            distances.append(range(len(locations)))
        for i in range(len(locations)):
            for j in range(len(locations)):
                if i != j:
                    x1 = locations[i]["latlng"][0]
                    x2 = locations[j]["latlng"][0]
                    y1 = locations[j]["latlng"][1]
                    y2 = locations[j]["latlng"][1]
                    distances[i][j] = ((x1 - x2) ** 2 + (y1 - y2) ** 2) ** 0.5
                else:
                    distances[i][j] = 0.0
                    
        min = sum(distances[0])
        min_i = 0
        for i in range(len(distances)):
            if sum(distances[i]) < min:
                min = sum(distances[i])
                min_i = i
        map_center = (locations[min_i]["latlng"][0], locations[min_i]["latlng"][1])
        return (locations, map_center)
    
    locations = getLocations(1)[0]
    map_center = getLocations(1)[1]
    
    location_match = {}
    for location in locations:
        location_match[location["name"]] = {'lat':location['latlng'][0], 'lng':location['latlng'][1]}
    # for article in articles:
    #    article.description = article.description.split(" ")
    #    article.text = article.text.split(" ")
   
    '''
    Gets the Top k entities for presentation and the rest for selection.
    '''
    def getEntities(k):
        persons = {}
        organizations = {}
        for article in articles:
            for person in article.entity_persons:
                if person not in persons.keys():
                    persons[person] = 1
                else:
                    persons[person] += 1
            for organization in article.entity_organizations:
                if organization not in organizations.keys():
                    organizations[organization] = 1
                else:
                    organizations[organization] += 1
        entitiesDict = {}
        for organization in organizations.keys():
            entitiesDict[organization] = EntityOrganizations.objects.filter(name=organization)[0].__dict__
            entitiesDict[organization]['type'] = 'organization'
        for person in persons.keys():
            entitiesDict[person] = EntityPersons.objects.filter(name=person)[0].__dict__
            entitiesDict[person]['type'] = 'person'
            
        allEntities = dict(organizations.items() + persons.items())
        topK = sorted(allEntities.iteritems(), key=operator.itemgetter(1), reverse=True)[:k]
        temp = []
        for topk in topK:
            temp.append(topk[0])
        return temp, entitiesDict
    
    temp = getEntities(3)
    topK = temp[0]
    allEntities = temp[1]
                 
    specific_context_dict = {
                    'dossier_title': "Welcome to Dossier No " + str(cluster_id),
                    'article_text':"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Option congue nihil imperdiet doming id quod mazim placerat facer",
                    'widgets': ["WIKI-Bild", "WIKI-Box", "Google-Maps"],
                    'articles': articles,
                    'locations': locations,
                    'map_center':map_center,
                    'location_match':location_match,
                    'topK': topK,
                    'entities':allEntities,
                    'entitiesKeys':allEntities.keys()
                    }
    print allEntities.keys()
    dossier_context_dict = dict(context_dict.items() + specific_context_dict.items())
    return render_to_response('dossier.html', dossier_context_dict, context)


'''
View for the impressum.
'''
def impressum(request):
    context = RequestContext(request) 
    return render_to_response('impressum.html', context)

'''
View for the what page
'''
def what(request):
    context = RequestContext(request)
    return render_to_response('what.html', context)
