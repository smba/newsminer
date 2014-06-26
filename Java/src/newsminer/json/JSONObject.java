package newsminer.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;

/**
 * Represents a JSON object.
 * Contains any amount of {@link JSONEntry} instances.
 * Note that the keys do not have to be unique.
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
 * @see     JSONEntry
 */
public class JSONObject extends JSONStructure implements Iterable<JSONEntry> {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 7657555477771030189L;
  
  //attributes
  /** list of all entries */
  private final List<JSONEntry> entries;
  
  /**
   * Constructs a new instance of this class.
   */
  public JSONObject() {
    this.entries = new ArrayList<JSONEntry>();
  }
  
  /**
   * Returns the amount of elements.
   * @return the amount of elements
   */
  public int size() {
    return entries.size();
  }
  
  /**
   * Returns true if this contains no elements.
   * @return true if this contains no elements
   */
  public boolean isEmpty() {
    return entries.isEmpty();
  }
  
  /**
   * Returns true if this contains the given value.
   * @param  value value to be found
   * @return true if this contains the given value
   */
  public boolean contains(JSONValue value) {
    if (indexOf(value) < 0) {
      return false;
    }
    return true;
  }
  
  /**
   * Returns true if this contains the given key.
   * @param  key key to be found
   * @return true if this contains the given key
   */
  public boolean containsKey(String key) {
    return containsKey(new JSONString(key));
  }
  
  /**
   * Returns true if this contains the given key.
   * @param  key key to be found
   * @return true if this contains the given key
   */
  public boolean containsKey(JSONString key) {
    if (indexOfKey(key) < 0) {
      return false;
    }
    return true;
  }
  
  /**
   * Returns the index of the given value or -1 if it does not exist.
   * @param  value value to be found
   * @return the index of the given value or -1 if it does not exist
   */
  public int indexOf(JSONValue value) {
    for (int i = 0; i < size(); i++) {
      final JSONValue currentValue = entries.get(i).getValue();
      if (currentValue.equals(value)) {
        return i;
      }
    }
    return -1;
  }
  
  /**
   * Returns the index of the given key or -1 if it does not exist.
   * @param  key key to be found
   * @return the index of the given key or -1 if it does not exist
   */
  public int indexOfKey(String key) {
    return indexOfKey(new JSONString(key));
  }
  
  /**
   * Returns the index of the given key or -1 if it does not exist.
   * @param  key key to be found
   * @return the index of the given key or -1 if it does not exist
   */
  public int indexOfKey(JSONString key) {
    for (int i = 0; i < size(); i++) {
      final JSONString currentKey = entries.get(i).getKey();
      if (currentKey.equals(key)) {
        return i;
      }
    }
    return -1;
  }
  
  /**
   * Returns the value with the given key or null if it does not exist.
   * @param  key key to be found
   * @return the value with the given key or null if it does not exist
   */
  public JSONValue get(String key) {
    return get(new JSONString(key));
  }
  
  /**
   * Returns the value with the given key or null if it does not exist.
   * @param  key key to be found
   * @return the value with the given key or null if it does not exist
   */
  public JSONValue get(JSONString key) {
    final int index = indexOfKey(key);
    if (index < 0) {
      return null;
    }
    return entries.get(index).getValue();
  }
  
  /**
   * Returns the value with the given key casted to the given type.
   * @param  key key to be found
   * @param  clazz class of the type to be casted to
   * @return the value with the given key casted to the given type
   * @throws ClassCastException if the object is not assignable to the given type
   * @throws MissingResourceException if the value with the given key does not exist
   */
  public <U> U get(String key, Class<U> clazz) throws ClassCastException, MissingResourceException {
    return get(new JSONString(key), clazz);
  }
  
  /**
   * Returns the value with the given key casted to the given type.
   * @param  key key to be found
   * @param  clazz class of the type to be casted to
   * @return the value with the given key casted to the given type
   * @throws ClassCastException if the object is not assignable to the given type
   * @throws MissingResourceException if the value with the given key does not exist
   */
  public <U> U get(JSONString key, Class<U> clazz) throws ClassCastException, MissingResourceException {
    final JSONValue value = get(key);
    if (value == null) {
      throw new MissingResourceException("Key not found: " + key.getString(), clazz.toString(), key.getString());
    }
    return value.castTo(clazz);
  }
  
  /**
   * Returns the value with the given key casted to the given type or the default value if it does not exist or is null.
   * @param  key key to be found
   * @param  clazz class of the type to be casted to
   * @param  defaultValue default value
   * @return the value with the given key casted to the given type or the default value if it does not exist or is null
   * @throws ClassCastException if the object is not assignable to the given type
   */
  public <U> U get(String key, Class<U> clazz, U defaultValue) throws ClassCastException {
    return get(new JSONString(key), clazz, defaultValue);
  }
  
  /**
   * Returns the value with the given key casted to the given type or the default value if it does not exist or is null.
   * @param  key key to be found
   * @param  clazz class of the type to be casted to
   * @param  defaultValue default value
   * @return the value with the given key casted to the given type or the default value if it does not exist or is null
   * @throws ClassCastException if the object is not assignable to the given type
   */
  public <U> U get(JSONString key, Class<U> clazz, U defaultValue) throws ClassCastException {
    final JSONValue value = get(key);
    if (value == null) {
      return defaultValue;
    }
    final U castValue = value.castTo(clazz);
    return castValue == null ? defaultValue : castValue;
  }
  
  /**
   * Adds the given key and value pair to this.
   * @param  key key to be added
   * @param  value value to be added
   * @return this for chaining
   */
  public JSONObject add(String key, JSONValue value) {
    return add(new JSONString(key), value);
  }
  
  /**
   * Adds the given key and value pair to this.
   * @param  key key to be added
   * @param  value value to be added
   * @return this for chaining
   */
  public JSONObject add(JSONString key, JSONValue value) {
    if (key == null || value == null) {
      return this;
    }
    entries.add(new JSONEntry(key, value));
    return this;
  }
  
  /**
   * Sets the value of the pair with the given key to the given value
   * @param  key key to be found
   * @param  value value to be changed to
   * @return this for chaining
   */
  public JSONObject set(String key, JSONValue value) {
    return set(new JSONString(key), value);
  }
  
  /**
   * Sets the value of the pair with the given key to the given value
   * @param  key key to be found
   * @param  value value to be changed to
   * @return this for chaining
   */
  public JSONObject set(JSONString key, JSONValue value) {
    final int index = indexOfKey(key);
    if (index < 0) {
      return this;
    }
    entries.get(index).setValue(value);
    return this;
  }
  
  /**
   * Removes the given key from this
   * @param  key key to be removed
   * @return this for chaining
   */
  public JSONObject remove(String key) {
    return remove(new JSONString(key));
  }
  
  /**
   * Removes the pair with the given key from this
   * @param  key key of the pair to be removed
   * @return this for chaining
   */
  public JSONObject remove(JSONString key) {
    final int index = indexOfKey(key);
    if (index < 0) {
      return this;
    }
    entries.remove(index);
    return this;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<JSONEntry> iterator() {
    return entries.iterator();
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isArray()
   */
  @Override
  public boolean isArray() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isBoolean()
   */
  @Override
  public boolean isBoolean() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isNull()
   */
  @Override
  public boolean isNull() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isNumber()
   */
  @Override
  public boolean isNumber() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isObject()
   */
  @Override
  public boolean isObject() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isString()
   */
  @Override
  public boolean isString() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return toString(-1);
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONStructure#toString(int)
   */
  @Override
  public String toString(int indent) {
    return appendTo(new StringBuilder(), indent, 0).toString();
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONStructure#appendTo(java.lang.StringBuilder, int, int)
   */
  @Override
  public StringBuilder appendTo(StringBuilder sb, int indent, int level) {
    sb.append(JSONProtocol.CHAR_BEGIN_OBJECT);
    if (!isEmpty()) {
      level++;
      for (Iterator<JSONEntry> entryIterator = entries.iterator();;) {
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
    sb.append(JSONProtocol.CHAR_END_OBJECT);
    return sb;
  }
}