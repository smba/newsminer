package newsminer.json;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.TypeConstraintException;

/**
 * A {@link JSONStructure} representing a JSON array.
 * Contains any amount of {@link JSONValue} instances.
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 * @see     JSONStructure
 */
public class JSONArray extends JSONStructure implements Iterable<JSONValue> {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 6966570264744252369L;
  
  //attributes
  /** wrapped value */
  private final List<JSONValue> wrap;
  
  /**
   * Constructs a new instance of this class.
   */
  public JSONArray() {
    this.wrap = new LinkedList<>();
  }
  
  /**
   * @see java.util.List#size()
   */
  public int size() {
    return wrap.size();
  }
  
  /**
   * @see java.util.List#isEmpty()
   */
  public boolean isEmpty() {
    return wrap.isEmpty();
  }
  
  /**
   * @see java.util.List#contains(java.lang.Object)
   */
  public boolean contains(Object o) {
    return wrap.contains(o);
  }
  
  /**
   * @see java.util.List#iterator()
   */
  public Iterator<JSONValue> iterator() {
    return wrap.iterator();
  }
  
  /**
   * @see java.util.List#toArray()
   */
  public JSONValue[] toArray() {
    return (JSONValue[]) wrap.toArray();
  }
  
  /**
   * @see java.util.List#toArray(T[])
   */
  public <T> T[] toArray(T[] a) {
    return wrap.toArray(a);
  }
  
  /**
   * @see java.util.List#add(java.lang.Object)
   */
  public JSONArray add(JSONValue e) {
    wrap.add(e);
    return this;
  }
  
  /**
   * @see java.util.List#remove(java.lang.Object)
   */
  public JSONArray remove(Object o) {
    wrap.remove(o);
    return this;
  }
  
  /**
   * @see java.util.List#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection<?> c) {
    return wrap.containsAll(c);
  }
  
  /**
   * @see java.util.List#addAll(java.util.Collection)
   */
  public JSONArray addAll(Collection<? extends JSONValue> c) {
    wrap.addAll(c);
    return this;
  }
  
  /**
   * @see java.util.List#addAll(int, java.util.Collection)
   */
  public JSONArray addAll(int index, Collection<? extends JSONValue> c) {
    wrap.addAll(index, c);
    return this;
  }
  
  /**
   * @see java.util.List#removeAll(java.util.Collection)
   */
  public JSONArray removeAll(Collection<?> c) {
    wrap.removeAll(c);
    return this;
  }
  
  /**
   * @see java.util.List#retainAll(java.util.Collection)
   */
  public JSONArray retainAll(Collection<?> c) {
    wrap.retainAll(c);
    return this;
  }
  
  /**
   * @see java.util.List#clear()
   */
  public JSONArray clear() {
    wrap.clear();
    return this;
  }
  
  /**
   * Returns the item at the given index.
   * @param  index position of the item
   * @return the item at the given index
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public JSONValue get(int index) throws IndexOutOfBoundsException {
    return wrap.get(index);
  }
  
  @Override
  protected JSONValue get(int index, Object... jsonPath)
      throws TypeConstraintException, IndexOutOfBoundsException {
    final Object key = jsonPath[index];
    if (key instanceof Integer) {
      if (index == jsonPath.length - 1) {
        return get((int) key);
      }
      final JSONValue value = get((int) key);
      return value.get(++index, jsonPath);
    }
    throw new TypeConstraintException("Expected integer as key for JSON array but got this instance of " + key.getClass() + " instead: " + key);
  }
  
  /**
   * Returns the item at the given index casted to the given type.
   * @param  index position of the item
   * @param  clazz class of the type to be casted to
   * @return the item at the given index casted to the given type
   * @throws ClassCastException if the object is not assignable to the given type
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public <T> T get(int index, Class<T> clazz) throws ClassCastException, IndexOutOfBoundsException {
    return get(index).castTo(clazz);
  }
  
  @Override
  protected <U> U get(int index, Class<U> clazz, Object... jsonPath)
      throws TypeConstraintException, ClassCastException, IndexOutOfBoundsException {
    final Object key = jsonPath[index];
    if (key instanceof Integer) {
      if (index == jsonPath.length - 1) {
        return get((int) key, clazz);
      }
      final JSONValue value = get((int) key);
      return value.get(++index, clazz, jsonPath);
    }
    throw new TypeConstraintException("Expected integer as key for JSON array but got this instance of " + key.getClass() + " instead: " + key);
  }
  
  /**
   * Returns the item at the given index casted to the given type or the default value if it is null or the index is out of range.
   * @param  index position of the item
   * @param  clazz class of the type to be casted to
   * @param  defaultValue default value
   * @return the item at the given index casted to the given type or the default value if it is null or the index is out of range
   * @throws ClassCastException if the object is not assignable to the given type
   */
  public <U> U get(int index, Class<U> clazz, U defaultValue) throws ClassCastException {
    if (index < 0 || index >= wrap.size()) {
      return defaultValue;
    }
    final U castValue = get(index).castTo(clazz);
    return castValue == null ? defaultValue : castValue;
  }
  
  @Override
  protected <U> U get(int index, Class<U> clazz, U defaultValue, Object... jsonPath)
      throws IllegalArgumentException, TypeConstraintException, ClassCastException {
    final Object key = jsonPath[index];
    if (key instanceof Integer) {
      if (index == jsonPath.length - 1) {
        return get((int) key, clazz, defaultValue);
      }
      final JSONValue value = get((int) key, JSONValue.class, (JSONValue) null);
      if (value == null) {
        return defaultValue;
      }
      return value.get(++index, clazz, defaultValue, jsonPath);
    }
    throw new TypeConstraintException("Expected integer as key for JSON array but got this instance of " + key.getClass() + " instead: " + key);
  }
  
  /**
   * @see java.util.List#set(int, java.lang.Object)
   */
  public JSONArray set(int index, JSONValue element) {
    wrap.set(index, element);
    return this;
  }
  
  /**
   * @see java.util.List#add(int, java.lang.Object)
   */
  public JSONArray add(int index, JSONValue element) {
    wrap.add(index, element);
    return this;
  }
  
  /**
   * @see java.util.List#remove(int)
   */
  public JSONArray remove(int index) {
    wrap.remove(index);
    return this;
  }
  
  /**
   * @see java.util.List#indexOf(java.lang.Object)
   */
  public int indexOf(Object o) {
    return wrap.indexOf(o);
  }
  
  /**
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */
  public int lastIndexOf(Object o) {
    return wrap.lastIndexOf(o);
  }
  
  /**
   * @see java.util.List#listIterator()
   */
  public ListIterator<JSONValue> listIterator() {
    return wrap.listIterator();
  }
  
  /**
   * @see java.util.List#listIterator(int)
   */
  public ListIterator<JSONValue> listIterator(int index) {
    return wrap.listIterator(index);
  }
  
  /**
   * @see java.util.List#subList(int, int)
   */
  public List<JSONValue> subList(int fromIndex, int toIndex) {
    return wrap.subList(fromIndex, toIndex);
  }
  
  @Override
  public boolean isArray() {
    return true;
  }
  
  @Override
  public boolean isObject() {
    return false;
  }
  
  @Override
  public String toString() {
    return toString(-1);
  }
  
  @Override
  public String toString(int indent) {
    return appendTo(new StringBuilder(), indent, 0).toString();
  }
  
  @Override
  public StringBuilder appendTo(StringBuilder sb, int indent, int level) {
    sb.append(JSONProtocol.CHAR_BEGIN_ARRAY);
    if (!isEmpty()) {
      level++;
      for (Iterator<JSONValue> entryIterator = wrap.iterator();;) {
        addIndent(sb, indent, level);
        entryIterator.next().appendTo(sb, indent, level);
        if (entryIterator.hasNext()) {
          sb.append(JSONProtocol.CHAR_VALUE_SEPARATOR);
          continue;
        }
        break;
      }
      level--;
      addIndent(sb, indent, level);
    }
    sb.append(JSONProtocol.CHAR_END_ARRAY);
    return sb;
  }
}