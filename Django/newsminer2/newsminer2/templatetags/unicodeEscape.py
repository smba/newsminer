from django.template import Library

register = Library()

@register.filter(name='escape')
def inside(text):
    return text.decode('unicode-escape')
