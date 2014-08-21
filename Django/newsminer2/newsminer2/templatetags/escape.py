from django.template import Library

register = Library()

@register.filter
def escape(text):
    return text.decode('unicode-escape')
