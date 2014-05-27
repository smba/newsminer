from django.shortcuts import render
from django.http import HttpResponse
from django.template import RequestContext
from django.shortcuts import render_to_response

#from django.template import Template, Context
#from django.template.loader import get_template

def index(request):
    context = RequestContext(request)
    context_dict = {'content_text': "Welcome to NewsMiner+", 
                    'dossier':"/dossier/"}
    return render_to_response('index.html', context_dict, context)

def dossier(request):
    context = RequestContext(request)
    context_dict = {'dossier_title': "Welcome to NewsMiner+", 
                    'article_text':"/dossier/",
                    'widget_1': "WIKI-Bild",
                    'widget_2': "WIKI-Box",
                    'widget_3': "Google-Maps",
                    }
    return render_to_response('index.html', context_dict, context)
    return HttpResponse("<b>Hello Dossier!</b><br><a href='/'>Home</a>")

