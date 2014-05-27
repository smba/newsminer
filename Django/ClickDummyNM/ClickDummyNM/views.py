from django.http import HttpResponse
#from django.template import Template, Context
#from django.template.loader import get_template

def index(request):
    return HttpResponse("<b>Hello Index!</b><br><a href='/dossier'>Dossier</a>")

def dossier(request):
    return HttpResponse("<b>Hello Dossier!</b><br><a href='/'>Home</a>")

