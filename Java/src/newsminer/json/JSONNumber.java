package newsminer.json;

import java.math.BigDecimal;

/**
 * Represents a JSON number.
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
 */
public class JSONNumber extends JSONPrimitive {
	//Serializable constants
	/** Serial Version UID */
  private static final long serialVersionUID = 4864685923820466659L;
  
  //attributes
  /** wrapped value */
  private final BigDecimal wrap;
  
  /**
   * Constructs a new instance of this class from the given String.
   * @param  val source String
   * @throws NumberFormatException
   */
  public JSONNumber(String val) throws NumberFormatException {
    this.wrap = new BigDecimal(val);
  }
  
  /**
   * Constructs a new instance of this class from the given BigDecimal.
   * @param val source double value
   */
  public JSONNumber(BigDecimal val) {
    this.wrap = val;
  }
  
  /**
   * Constructs a new instance of this class from the given integer value.
   * @param val source integer value
   */
  public JSONNumber(int val) {
    this.wrap = new BigDecimal(val);
  }
  
  /**
   * Constructs a new instance of this class from the given long value.
   * @param val source long value
   */
  public JSONNumber(long val) {
    this.wrap = new BigDecimal(val);
  }
  
  /**
   * Constructs a new instance of this class from the given double value.
   * @param val source double value
   */
  public JSONNumber(double val) {
    this.wrap = new BigDecimal(val);
  }
  
  /**
   * Returns the value.
   * @return the value
   */
  public BigDecimal getValue() {
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
		return true;
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
    if (clazz == Byte.class) {
      return clazz.cast(wrap.byteValue());
    }
    if (clazz == Short.class) {
      return clazz.cast(wrap.shortValue());
    }
    if (clazz == Integer.class) {
      return clazz.cast(wrap.intValue());
    }
    if (clazz == Long.class) {
      return clazz.cast(wrap.longValue());
    }
    if (clazz == Float.class) {
      return clazz.cast(wrap.floatValue());
    }
    if (clazz == Double.class) {
      return clazz.cast(wrap.doubleValue());
    }
    return super.castTo(clazz);
  }
	
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return wrap.toString();
  }
}