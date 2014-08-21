from django.template import Library

register = Library()

@register.filter(name='escape')
def escape(text):
    return text.decode('unicode-escape')
