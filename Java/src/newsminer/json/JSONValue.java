package newsminer.json;

import java.io.Serializable;

import javax.xml.bind.TypeConstraintException;

/**
 * Represents a JSON value.
 * This is the root of the JSON type hierarchy.
 * This can be either a {@link JSONPrimitive} or a {@link JSONStructure}.
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 * @see     JSONPrimitive
 * @see     JSONStructure
 */
public abstract class JSONValue implements Serializable {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 2367738441118742087L;
  
  /**
   * Returns true if this is a {@link JSONArray}.
   * @return true if this is a {@link JSONArray}
   * @see    JSONArray
   */
  public abstract boolean isArray();
  
  /**
   * Returns true if this is a {@link JSONBoolean}.
   * @return true if this is a {@link JSONBoolean}
   * @see    JSONBoolean
   */
  public abstract boolean isBoolean();
  
  /**
   * Returns true if this is a {@link JSONNull}.
   * @return true if this is a {@link JSONNull}
   * @see    JSONNull
   */
  public abstract boolean isNull();
  
  /**
   * Returns true if this is a {@link JSONNumber}.
   * @return true if this is a {@link JSONNumber}
   * @see    JSONNumber
   */
  public abstract boolean isNumber();
  
  /**
   * Returns true if this is a {@link JSONObject}.
   * @return true if this is a {@link JSONObject}
   * @see    JSONObject
   */
  public abstract boolean isObject();
  
  /**
   * Returns true if this is a {@link JSONPrimitive}.
   * @return true if this is a {@link JSONPrimitive}
   * @see    JSONPrimitive
   */
  public abstract boolean isPrimitive();
  
  /**
   * Returns true if this is a {@link JSONString}.
   * @return true if this is a {@link JSONString}
   * @see    JSONString
   */
  public abstract boolean isString();
  
  /**
   * Returns true if this is a {@link JSONStructure}.
   * @return true if this is a {@link JSONStructure}
   * @see    JSONStructure
   */
  public abstract boolean isStructure();
  
  /**
   * Returns the last node of the given JSON path or null if it or any of its parents does not exist.
   * @param  index the current index
   * @param  jsonPath a series of keys and indexes to be traversed
   * @return the last node of the given JSON path or null if it or any of its parents does not exist
   * @throws TypeConstraintException if any node other than the last one is a primitive value;
   *                                 if any node other than the last one is a {@link JSONArray} but the corresponding path key is a String or {@link JSONString};
   *                                 if any node other than the last one is a {@link JSONObject} but the corresponding path key is an integer
   * @throws IndexOutOfBoundsException if any index is out of range
   */
  protected abstract JSONValue get(int index, Object... jsonPath)
      throws TypeConstraintException, IndexOutOfBoundsException;
  
  /**
   * Returns the last node of the given JSON path casted to the given type or null if it or any of its parents does not exist.
   * @param  index the current index
   * @param  clazz class of the type to be casted to
   * @param  jsonPath a series of keys and indexes to be traversed
   * @return the last node of the given JSON path casted to the given type or null if it or any of its parents does not exist
   * @throws TypeConstraintException if any node other than the last one is a primitive value;
   *                                 if any node other than the last one is a {@link JSONArray} but the corresponding path key is a String or {@link JSONString};
   *                                 if any node other than the last one is a {@link JSONObject} but the corresponding path key is an integer
   * @throws ClassCastException if the object is not assignable to the given type;
   * @throws IndexOutOfBoundsException if any index is out of range
   */
  protected abstract <U> U get(int index, Class<U> clazz, Object... jsonPath)
      throws TypeConstraintException, ClassCastException, IndexOutOfBoundsException;
  
  /**
   * Returns the the last node of the given JSON path casted to the given type or the default value if it or any of its parents does not exist or is null or any index is out of range.
   * @param  index the current index
   * @param  clazz class of the type to be casted to
   * @param  defaultValue default value
   * @param  jsonPath a series of keys and indexes to be traversed
   * @return the the last node of the given JSON path casted to the given type or the default value if it or any of its parents does not exist or is null or any index is out of range
   * @throws TypeConstraintException if any node other than the last one is a primitive value;
   *                                 if any node other than the last one is a {@link JSONArray} but the corresponding path key is a String or {@link JSONString};
   *                                 if any node other than the last one is a {@link JSONObject} but the corresponding path key is an integer
   * @throws ClassCastException if the object is not assignable to the given type;
   */
  protected abstract <U> U get(int index, Class<U> clazz, U defaultValue, Object... jsonPath)
      throws TypeConstraintException, ClassCastException;
  
  /**
   * Returns this casted to the given type.
   * @param  clazz class of the type to be casted to
   * @return this casted to the given type
   * @throws ClassCastException if this is not assignable to the given type
   */
  public <T> T castTo(Class<T> clazz) throws ClassCastException {
    return clazz.cast(this);
  }
  
  /**
   * Appends the textual representation of this as a String with or without added indent to the given StringBuilder.
   * @param  sb StringBuilder to append to
   * @param  indent amount of spaces added per block level;
   *                0 for newlines without indent;
   *                any negative value for no newlines
   * @param  level how many times the indent gets added for every line
   * @return this appended to the given StringBuilder
   */
  abstract StringBuilder appendTo(StringBuilder sb, int indent, int level);
  
  /**
   * Adds the indent to the given StringBuilder.
   * @param  sb StringBuilder to append to
   * @param  indent amount of spaces added per block level;
   *                0 for newlines without indent;
   *                any negative value for no newlines
   * @param  level how many times the indent gets added for every line
   * @return StringBuilder with added indent
   */
  static StringBuilder addIndent(StringBuilder sb, int indent, int level) {
    if (indent >= 0) {
      sb.append(System.lineSeparator());
      if (level > 0) {
        for (int left = indent*level; left > 0; left--) {
          sb.append(' ');
        }
      }
    }
    return sb;
  }
}