package newsminer.json;

import java.util.AbstractMap.SimpleEntry;

/**
 * Represents an entry in a {@link JSONObject}.
 * This maps a {@link JSONString} (key) to a {@link JSONValue} (value).
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 * @see     JSONObject
 * @see     JSONString
 * @see     JSONValue
 */
public class JSONEntry extends SimpleEntry<JSONString, JSONValue> {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 4336733009782316630L;
  
  /**
   * Constructs a new instance of this class.
   * @param key the key represented by this entry
   * @param value the value represented by this entry
   */
  public JSONEntry(String key, JSONValue value) {
    this(new JSONString(key), value);
  }
  
  /**
   * Constructs a new instance of this class.
   * @param key the key represented by this entry
   * @param value the value represented by this entry
   */
  public JSONEntry(JSONString key, JSONValue value) {
    super(key, value);
  }
  
  @Override
  public String toString() {
    return toString(-1);
  }
  
  /**
   * Returns the textual representation of this as a String with or without added indent.
   * @param  indent amount of spaces added per block level;
   *                0 for newlines without indent;
   *                any negative value for no newlines
   * @return this as String
   */
  public String toString(int indent) {
    return appendTo(new StringBuilder(), indent, 0).toString();
  }
  
  /**
   * Adds the indent to the given StringBuilder.
   * @param  sb StringBuilder to append to
   * @param  indent amount of spaces added per block level;
   *                0 for newlines without indent;
   *                any negative value for no newlines
   * @param  level how many times the indent gets added for every line
   * @return this appended to the given StringBuilder
   */
  StringBuilder appendTo(StringBuilder sb, int indent, int level) {
    getKey().appendTo(sb, indent, level);
    sb.append(JSONProtocol.CHAR_NAME_SEPARATOR);
    if (indent >= 0) {
      sb.append(' ');
    }
    getValue().appendTo(sb, indent, level);
    return sb;
  }
}