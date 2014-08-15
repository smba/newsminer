package newsminer.json;

/**
 * A {@link JSONPrimitive} representing the JSON null literal (<code>null</code>).
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 * @see     JSONPrimitive
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
  
  @Override
  public boolean isBoolean() {
    return false;
  }
  
  @Override
  public boolean isNull() {
    return true;
  }
  
  @Override
  public boolean isNumber() {
    return false;
  }
  
  @Override
  public boolean isString() {
    return false;
  }
  
  @Override
  public <T> T castTo(Class<T> clazz) {
    return null;
  }
  
  @Override
  public String toString() {
    return STRING_NULL;
  }
}