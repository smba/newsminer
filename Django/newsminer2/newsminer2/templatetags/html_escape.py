from django.template import Library
import HTMLParser

register = Library()

@register.filter
def html_escape(text):
    h = HTMLParser.HTMLParser()
    return h.unescape(text)
