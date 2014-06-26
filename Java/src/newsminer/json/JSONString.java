package newsminer.json;

import java.util.Locale;

/**
 * Represents a JSON string.
 * Contains any amount of characters enclosed in quotation marks.
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
 */
public class JSONString extends JSONPrimitive {
	//Serializable constants
	/** Serial Version UID */
  private static final long serialVersionUID = 6468295594866255962L;
  
  //attributes
  /** wrapped value */
  private final String wrap;
  
	/**
	 * Constructs a new instance of this class.
	 */
	public JSONString() {
    this.wrap = new String();
	}
	
	/**
	 * Constructs a new instance of this class from the given String.
   * Note that the source String will not be escaped by default and as such does not conform to the JSON standard.
   * It is advisable to call {@link escape(String)} before this constructor.
	 * @param value
	 * @see   escape(String)
	 */
	public JSONString(String val) {
    this.wrap = val;
	}
	
	/**
	 * Returns this as String without quotation marks.
	 * Note that this will not unescape the source String if it has been escaped earlier.
	 * This may also be the case when this instance was retrieved through the {@link JSONParser}.
	 * @return this as String without quotation marks
	 * @see    unescape(String)
	 */
	public String getString() {
	  return wrap;
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
		return false;
	}
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isString()
   */
  @Override
  public boolean isString() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isStructure()
   */
  @Override
  public boolean isStructure() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    return hashCode() == obj.hashCode();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return wrap.hashCode();
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#cast(java.lang.Class)
   */
  @Override
  public <T> T castTo(Class<T> clazz) throws ClassCastException {
    if (clazz == String.class) {
      return clazz.cast(wrap);
    }
    if (clazz == Boolean.class) {
      return clazz.cast(Boolean.parseBoolean(wrap));
    }
    if (clazz == Byte.class) {
      return clazz.cast(Byte.parseByte(wrap));
    }
    if (clazz == Short.class) {
      return clazz.cast(Short.parseShort(wrap));
    }
    if (clazz == Integer.class) {
      return clazz.cast(Integer.parseInt(wrap));
    }
    if (clazz == Long.class) {
      return clazz.cast(Long.parseLong(wrap));
    }
    if (clazz == Float.class) {
      return clazz.cast(Float.parseFloat(wrap));
    }
    if (clazz == Double.class) {
      return clazz.cast(Double.parseDouble(wrap));
    }
    return super.castTo(clazz);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
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