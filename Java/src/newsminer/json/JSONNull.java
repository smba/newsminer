package newsminer.json;

/**
 * Represents the JSON null literal (<code>null</code>).
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
 */
public class JSONNull extends JSONPrimitive {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 8934048968682440422L;
  
  //constants
  /** a generic instance of this class */
  public static final JSONNull NULL        = new JSONNull();
  /** String for the null literal */
  public static final String   STRING_NULL = "null";
  
  /**
   * Constructs a new instance of this class.
   */
  private JSONNull() {
    //Do nothing.
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
    return true;
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
   * @see newsminer.json.JSONValue#isStructure()
   */
  @Override
  public boolean isStructure() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isString()
   */
  @Override
  public boolean isString() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#castTo(java.lang.Class)
   */
  @Override
  public <T> T castTo(Class<T> clazz) {
    return null;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#toString()
   */
  @Override
  public String toString() {
    return STRING_NULL;
  }
}