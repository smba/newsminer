# -*- coding: utf-8 -*-
from newsminer2.models import RssArticleClusters, RssFeeds, RssArticles, EntityLocations, EntityOrganizations, EntityPersons, RssArticleClustersEntityRssArticles
from django.http import HttpResponse, Http404
from django.shortcuts import render, render_to_response
from django.template import RequestContext
from warnings import catch_warnings
from django.db import connection
from django.shortcuts import redirect
from django.utils.dateformat import DateFormat
from django.db.models import Max, Min #
import operator
import datetime
import time

# from django.template import Template, Context
# from django.template.loader import get_template
global context_dict
context_dict = {
                'dossier':'/dossier/',
                'archive':'/archive/',
                'impressum':'/impressum/',
                'home':'/'
                }


def index(request, year, month, day):
    context = RequestContext(request)
    
    
    
    date = (int(year),int(month),int(day))
    timestamps = dateToTimestamp(date)

    clusters = RssArticleClusters.objects.raw("SELECT id,timestamp FROM rss_article_clusters "
                                              +"WHERE rss_article_clusters.timestamp BETWEEN " + str(timestamps[0]*1000) + " AND " + str(timestamps[1]*1000) + " "
                                              +"ORDER BY score DESC, common_entities DESC "
                                              +"LIMIT 10")

    latest = RssArticleClusters.objects.all().aggregate(Max('timestamp'))['timestamp__max']
    time_navigation = {}
    if str(date) == str(timestampToDate(int(str(latest)[:-3]))):
        time_navigation['newer'] = None
    else:
        time_navigation['newer'] = tomorrow(date)
    
    oldest = RssArticleClusters.objects.all().aggregate(Min('timestamp'))['timestamp__min']
    #print str(timestampToDate(int(str(oldest)[:-3])))
    if str(date) == str(timestampToDate(int(str(oldest)[:-3]))):
        time_navigation['older'] = None
    else:
        time_navigation['older'] = yesterday(date)

    
    clusterDatas = []
    cursor = connection.cursor()
    
    
    cl = []
    for cluster in clusters:
        append = True
        
        cursor.execute("select count(*) from rss_article_clusters_rss_articles "
                        +"join rss_article_clusters on rss_article_clusters.id = rss_article_clusters_rss_articles.id "
                        +"where rss_article_clusters_rss_articles.id = " + str(cluster.id))
        row = cursor.fetchone()[0]
        if row < 2:
            append = False
            continue
        
        for type in ["locations", "organizations", "persons"]:
            cursor.execute("select count(*) from rss_article_clusters "
                           +"join rss_article_clusters_entity_"+type+" "
                           +"on rss_article_clusters.id = rss_article_clusters_entity_"+type+".id "
                           +"where rss_article_clusters.id = " + str(cluster.id))
            row = cursor.fetchone()[0]
            if row == 0:
                append = False
                break
        if append:
            cl.append(cluster)
    clusters = cl
    
    
    for cluster in clusters:
        clusterData = {}
        clusterData['id'] = cluster.id
        cursor.execute("SELECT (rss_articles.link, source_url, rss_articles.timestamp, title, description, text) FROM rss_article_clusters "
                        +"JOIN rss_article_clusters_rss_articles "
                        +"ON rss_article_clusters.id=rss_article_clusters_rss_articles.id "
                        +"JOIN rss_articles ON rss_articles.link = rss_article_clusters_rss_articles.link "
                        +"WHERE rss_article_clusters.id = " + str(cluster.id) + " "
                        +"ORDER BY rss_article_clusters_rss_articles DESC "
                        +"LIMIT 1")
        try:
            total_rows = cursor.fetchone()[0][1:-1]
        except:
            continue
        rowstring = total_rows 
        total_rows = total_rows.split(",")
        centroid = {}
        centroid['link'] = total_rows[0]
        centroid['source_url'] = total_rows[1]
        
        date = datetime.datetime.fromtimestamp(int(total_rows[2][:-3]))
        df = DateFormat(date)    
        centroid['timestamp'] = df.format('jS F Y')
        
        title_start = rowstring.find('"')
        title_end = rowstring.find('"', title_start+1)
        centroid['title'] = rowstring[title_start+1:title_end]
        clusterData['centroid'] = centroid
        
        #get cluster persons
        persons = []
        cursor.execute("select entity_persons.image from rss_article_clusters "
                       +"join rss_article_clusters_entity_persons on rss_article_clusters_entity_persons.id = rss_article_clusters.id "
                       +"join entity_persons on rss_article_clusters_entity_persons.name = entity_persons.name "
                       +"where rss_article_clusters.id = " + str(cluster.id) + " and entity_persons.image <>''"
                       +"order by entity_persons.popularity DESC limit 4")
        images_row = cursor.fetchall()
        images = []
        for row in images_row:
            images.append(row[0])
        clusterData["images"] = images
        clusterData["score"] = cluster.score
        clusterDatas.append(clusterData)
    
    if len(clusterDatas) == 0:
        raise Http404
    
    meta_timestamp = datetime.date(int(year), int(month), int(day))
    dformat = DateFormat(meta_timestamp)
    meta_timestamp = dformat.format('jS F Y')
    
    specific_context_dict = {
                             'clusterDatas' : clusterDatas,
                             'time_navigation':time_navigation,
                             'meta_timestamp':meta_timestamp
                             }
    #print time_navigation
    index_context_dict = dict(context_dict.items() + specific_context_dict.items())
    return render_to_response('index.html', index_context_dict, context)


'''
Generic view for the dossiers.
@param param: cluster_id which will be used as link
'''
def dossier(request, cluster_id):
    cursor = connection.cursor()
    context = RequestContext(request)
    
    articles = []
    cluster = RssArticleClusters.objects.filter(id=cluster_id)[0]
    cursor.execute("SELECT rss_articles.link, source_url, timestamp, title, description, text "
                  +"FROM rss_articles JOIN rss_article_clusters_rss_articles "
                  +"ON rss_article_clusters_rss_articles.link = rss_articles.link WHERE id = " + str(cluster_id) + " ORDER BY score DESC")
    total_rows = cursor.fetchall()
    i= 0
    for row in total_rows:
        article = {}
        article['link'] = row[0]
        article['source_url'] = row[1]
        
        date = datetime.datetime.fromtimestamp(int(str(row[2])[:-3]))
        df = DateFormat(date)
        article['timestamp'] = df.format('jS F Y') #H:i
        
        article['title'] = row[3]
        article['description'] = row[4]
        article['text'] = row[5]
        
        #determine locations
        cursor.execute('SELECT name FROM rss_articles JOIN rss_articles_entity_locations '
                        +'ON rss_articles.link = rss_articles_entity_locations.link '
                        +'WHERE rss_articles.link = \'' + article['link'].replace("'","''") + '\'')
        c = []
        for loc in cursor.fetchall():
            c.append(loc[0])
        article['entity_locations'] = c
        
        #determine organizations
        cursor.execute("SELECT name FROM rss_articles JOIN rss_articles_entity_organizations "
                        +"ON rss_articles.link = rss_articles_entity_organizations.link "
                        +'WHERE rss_articles.link = \'' + article['link'].replace("'","''") + '\'')
        c = []
        for loc in cursor.fetchall():
            c.append(loc[0])
        article['entity_organizations'] = c
        
        #determine persons
        cursor.execute("SELECT name FROM rss_articles JOIN rss_articles_entity_persons "
                        +"ON rss_articles.link = rss_articles_entity_persons.link "
                        +'WHERE rss_articles.link = \'' + article['link'].replace("'","''") + '\'')
        c = []
        for loc in cursor.fetchall():
            c.append(loc[0])
        article['entity_persons'] = c
        articles.append(article)
    articlesCopy = []
    for i in range(len(articles)):
        temp = RssFeeds.objects.filter(source_url=articles[i]['source_url'])
        articleDict = articles[i]
        articleDict['newspaper'] = temp[0].name
        articleDict['country'] = temp[0].country
        articlesCopy.append(articleDict)
    '''
    Gets the locations and estimates the map center
    '''
    def getLocations(m):
        locations_bag = {}
        for article in articles:
            for location in article['entity_locations']:
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
        locations = {}
        for article in articles:
            for person in article['entity_persons']:
                if person not in persons.keys():
                    persons[person] = 1
                else:
                    persons[person] += 1
            for organization in article['entity_organizations']:
                if organization not in organizations.keys():
                    organizations[organization] = 1
                else:
                    organizations[organization] += 1

        entitiesDict = {}
        for organization in organizations.keys():
            o = EntityOrganizations.objects.filter(name=organization)[0].__dict__
            if o['description'] != None:
                entitiesDict[organization] = o
                entitiesDict[organization]['type'] = 'organization'
            else:
                del organizations[organization]
        for person in persons.keys():
            p = EntityPersons.objects.filter(name=person)[0].__dict__
            if p['image'] != None and p['description'] != None:
                entitiesDict[person] = p
                entitiesDict[person]['type'] = 'person'
            else:
                del persons[person]
            
        topPersons = sorted(persons.iteritems(), key=operator.itemgetter(1), reverse=True)[:k]
        allEntities = dict(organizations.items() + persons.items())
        topK = sorted(allEntities.iteritems(), key=operator.itemgetter(1), reverse=True)[:k]
        temp = []
        for topk in topK:
            temp.append(topk[0])
        return temp, entitiesDict, topPersons
    
    temp = getEntities(5)
    topK = temp[0]
    allEntities = temp[1]

    topPersons = temp[2]
    
    def getPersonDistribution():
        personlist = topPersons
        person_dist = {}
        for article in articles:
            person_dist[article['link']] = []
        for article in articles:
            for person in personlist:
                i = article['text'].count(person[0])
                person_dist[article['link']].append(i)
        return person_dist
    person_dist = getPersonDistribution() 
    persons = person_dist.keys()
    
    specific_context_dict = {
                    'dossier_title': "Welcome to Dossier No " + str(cluster_id),
                    'article':"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Option congue nihil imperdiet doming id quod mazim placerat facer",
                    'widgets': ["WIKI-Bild", "WIKI-Box", "Google-Maps"],
                    'articles': articles,
                    'locations': locations,
                    'map_center':map_center,
                    'location_match':location_match,
                    'topK': topK,
                    'entities':allEntities,
                    'entitiesKeys':allEntities.keys(),
                    'persons':persons,
                    'topPersons':topPersons,
                    'person_dist':person_dist,
                    }
    dossier_context_dict = dict(context_dict.items() + specific_context_dict.items())
    return render_to_response('dossier.html', dossier_context_dict, context)

#testview
def testview(request, a, b, c):
    date = (int(a),int(b),int(c))
    timestamps = dateToTimestamp(date)
    html = "<html><body>It is now between " + str(timestamps[0]) + " and " +  str(timestamps[1]) + "</body></html>"
    return HttpResponse(html)


def index_redirect(request):
    latest = RssArticleClusters.objects.all().aggregate(Max('timestamp'))['timestamp__max']
    return redirect('/%s/%s/%s' % timestampToDate(int(str(latest)[:-3])))

'''
View for the impressum.
'''
def impressum(request):
    context = RequestContext(request) 
    return render_to_response('impressum.html', context)

'''
View for the archive page
'''
def archive(request):
    context = RequestContext(request)
    
    clusters = RssArticleClusters.objects.all().order_by('-timestamp')
    dateStrings = []
    dateLinks  = []
    for cluster in clusters:
        timestamp = cluster.timestamp
        date = timestampToDate(int(str(timestamp)[:-3]))
        
        dateLink = str(date[0]) +"/"+ str(date[1]) +"/"+ str(date[2])
        dateString = str(date[2])+"."+str(date[1]) +"."+ str(date[0])
        if dateString not in dateStrings:
            dateStrings.append(dateString)
            dateLinks.append(dateLink)
    dossierlist = []
    
    for i in range(len(dateStrings)):
        d = {}
        d["link"] = dateLinks[i]
        d["string"] = dateStrings[i]
        dossierlist.append(d)
        
    context_dict = {
                    'dossierlist':dossierlist,
                    }
    
    return render_to_response('archive.html', context_dict, context)

def dateToTimestamp(date):
    start = datetime.date(date[0], date[1], date[2])
    return (int(time.mktime(start.timetuple())), int(time.mktime(start.timetuple()))+24*60*60)

def timestampToDate(timestamp):
    date = datetime.datetime.fromtimestamp(timestamp)
    year = date.year
    month = date.month
    day = date.day
    return (year,month,day)

def yesterday(date):
    timestamps = dateToTimestamp(date)
    return timestampToDate(timestamps[0]-24*60*60)
def tomorrow(date):
    timestamps = dateToTimestamp(date)
    return timestampToDate(timestamps[0]+24*60*60)