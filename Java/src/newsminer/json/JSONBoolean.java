package newsminer.json;

/**
 * A {@link JSONPrimitive} representing either of the JSON boolean literals (<code>true</code> and <code>false</code>).
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 * @see     JSONPrimitive
 */
public class JSONBoolean extends JSONPrimitive {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 6848916851595210610L;
  
  //constants
  /** a generic instance of the true literal */
  public static final JSONBoolean TRUE         = new JSONBoolean(true);
  /** a generic instance of the false literal */
  public static final JSONBoolean FALSE        = new JSONBoolean(false);
  /** String for the true literal */
  public static final String      STRING_TRUE  = "true";
  /** String for the false literal */
  public static final String      STRING_FALSE = "false";
  
  //attributes
  /** wrapped value */
  private final boolean wrap;
  
  /**
   * Constructs a new instance of this class.
   */
  private JSONBoolean(boolean value) {
    this.wrap = value;
  }
  
  /**
   * Returns the appropriate instance.
   * @param  value value of this
   * @return the appropriate instance
   * @see    TRUE
   * @see    FALSE
   */
  public static JSONBoolean getInstance(boolean value) {
    return value ? TRUE : FALSE;
  }
  
  /**
   * Returns the wrapped value.
   * @return the wrapped value
   */
  public boolean booleanValue() {
    return wrap;
  }
  
  @Override
  public boolean isBoolean() {
    return true;
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
    return false;
  }
  
  @Override
  public <T> T castTo(Class<T> clazz) throws ClassCastException {
    if (clazz == Boolean.class) {
      return clazz.cast(wrap);
    }
    return super.castTo(clazz);
  }
  
  @Override
  public String toString() {
    return wrap ? STRING_TRUE : STRING_FALSE;
  }
}