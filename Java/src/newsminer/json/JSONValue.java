package newsminer.json;

import java.io.Serializable;

/**
 * Represents a JSON value.
 * This can be a {@link JSONPrimitive} or a {@link JSONStructure}.
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
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