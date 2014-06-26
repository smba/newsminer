package newsminer.json;

/**
 * Represents either of the JSON boolean literals (<code>true</code> and <code>false</code>).
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
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
		return true;
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
   * @see newsminer.json.JSONValue#cast(java.lang.Class)
   */
  @Override
  public <T> T castTo(Class<T> clazz) throws ClassCastException {
    if (clazz == Boolean.class) {
      return clazz.cast(wrap);
    }
    return super.castTo(clazz);
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#toString()
   */
  @Override
  public String toString() {
    return wrap ? STRING_TRUE : STRING_FALSE;
  }
}