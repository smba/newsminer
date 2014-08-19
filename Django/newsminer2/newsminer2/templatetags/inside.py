from django.template import Library

register = Library()

@register.filter(name='in')
def inside(value, arg):
    return value in arg