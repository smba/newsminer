from django.template import Library
from django.utils.encoding import smart_str, smart_unicode
import unicodedata
register = Library()

@register.filter
def escape(text):
    #return text.decode('unicode-escape')
    return unicodedata.normalize('NFKD', text).encode('ascii', 'ignore')
