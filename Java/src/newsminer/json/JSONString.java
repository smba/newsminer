package newsminer.json;

import java.util.Locale;

/**
 * A {@link JSONPrimitive} representing a JSON string.
 * Contains any amount of characters enclosed in quotation marks.
 * 
 * @author  Timo Guenther
 * @version 2014-08-22
 * @see     JSONPrimitive
 */
public class JSONString extends JSONPrimitive {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 6468295594866255962L;
  
  //attributes
  /** wrapped value */
  private final String wrap;
  
  /**
   * Constructs a new, empty instance of this class.
   */
  public JSONString() {
    this.wrap = new String();
  }
  
  /**
   * Constructs a new instance of this class from the given String.
   * Note that this will escape the String to conform to the JSON standard.
   * @param val String to wrap
   * @see   escape(String)
   */
  public JSONString(String val) {
    this(val, true);
  }
  
  /**
   * Constructs a new instance of this class from the given String.
   * @param val String to wrap
   * @param escape true to escape the String
   * @see   escape(String)
   */
  public JSONString(String val, boolean escape) {
    if (escape) {
      val = escape(val);
    }
    this.wrap = val;
  }
  
  /**
   * Returns this as unescaped String without quotation marks.
   * @return this as unescaped String without quotation marks
   * @see    unescape(String)
   */
  public String getString() {
    return getString(true);
  }
  
  /**
   * Returns this as String without quotation marks.
   * @param  unescape true to unescape the String
   * @return this as String without quotation marks
   * @see    unescape(String)
   */
  public String getString(boolean unescape) {
    if (unescape) {
      return unescape(wrap);
    }
    return wrap;
  }
  
  @Override
  public boolean isBoolean() {
    return false;
  }
  
  @Override
  public boolean isNull() {
    return false;
  }
  
  @Override
  public boolean isNumber() {
    return false;
  }
  
  @Override
  public boolean isString() {
    return true;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    return hashCode() == obj.hashCode();
  }
  
  @Override
  public int hashCode() {
    return wrap.hashCode();
  }
  
  @Override
  public <T> T castTo(Class<T> clazz) throws ClassCastException {
    if (clazz == String.class) {
      return clazz.cast(getString());
    }
    if (clazz == Boolean.class) {
      return clazz.cast(Boolean.parseBoolean(getString()));
    }
    if (clazz == Byte.class) {
      return clazz.cast(Byte.parseByte(getString()));
    }
    if (clazz == Short.class) {
      return clazz.cast(Short.parseShort(getString()));
    }
    if (clazz == Integer.class) {
      return clazz.cast(Integer.parseInt(getString()));
    }
    if (clazz == Long.class) {
      return clazz.cast(Long.parseLong(getString()));
    }
    if (clazz == Float.class) {
      return clazz.cast(Float.parseFloat(getString()));
    }
    if (clazz == Double.class) {
      return clazz.cast(Double.parseDouble(getString()));
    }
    return super.castTo(clazz);
  }
  
  @Override
  public String toString() {
    return JSONProtocol.CHAR_QUOTATION_MARK + wrap + JSONProtocol.CHAR_QUOTATION_MARK;
  }
  
  /**
   * Returns the given String escaped according to RFC 4627.
   * @param  s String to escape
   * @return the given String escaped according to RFC 4627
   */
  public static String escape(String s) {
    final StringBuilder sb = new StringBuilder();
    for (char ch : s.toCharArray()) {
      if (JSONProtocol.isControlChar(ch) || JSONProtocol.isBeyondUnicode(ch)) { //Unicode escape
        sb.append(JSONProtocol.CHAR_ESCAPE);
        sb.append('u');
        sb.append(String.format((Locale) null, "%04x", (int) ch)); //TODO handle chars in (0xFFFF,0x10FFFF]
      } else if (ch == JSONProtocol.CHAR_QUOTATION_MARK || ch == JSONProtocol.CHAR_ESCAPE) { //short escape
        sb.append(JSONProtocol.CHAR_ESCAPE);
        sb.append(ch);
      } else { //safe
        sb.append(ch);
      }
    }
    return sb.toString();
  }
  
  /**
   * Returns the given String cleaned of unsafe JSON characters.
   * @param  line String to clean
   * @return the given String cleaned of unsafe JSON characters
   */
  public static String unescape(String s) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length();) {
      char ch = s.charAt(i);
      if (ch == JSONProtocol.CHAR_ESCAPE) { //escaped
        i++;
        ch = s.charAt(i);
        if (ch == JSONProtocol.CHAR_QUOTATION_MARK || ch == JSONProtocol.CHAR_ESCAPE || ch == '/') { //short escape
          sb.append(ch);
          i++;
        } else if (ch == 'u') { //Unicode escape
          i++;
          final String hex;
          try {
            hex = s.substring(i, i + 4); //TODO check for surrogate pairs
          } catch (IndexOutOfBoundsException ioobe) { //unfinished Unicode escape
            sb.append(JSONProtocol.CHAR_ESCAPE);
            sb.append('u');
            continue; //Ignore the escape sequence.
          }
          final char unicode;
          try {
            unicode = (char) Integer.parseInt(hex, 16);
          } catch (NumberFormatException nfe) { //corrupt Unicode escape
            sb.append(JSONProtocol.CHAR_ESCAPE);
            sb.append('u');
            continue; //Ignore the escape sequence.
          }
          sb.append(unicode);
          i += 4;
        } else if (ch == 'n') { //new line
          sb.append('\n');
          i++;
        } else { //wrongly placed escape character
          sb.append(JSONProtocol.CHAR_ESCAPE);
          continue; //Ignore the escape sequence.
        }
      } else { //not escaped
        sb.append(ch);
        i++;
      }
    }
    return sb.toString();
  }
}