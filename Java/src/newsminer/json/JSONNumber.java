package newsminer.json;

import java.math.BigDecimal;

/**
 * A {@link JSONPrimitive} representing a JSON number.
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 * @see     JSONPrimitive
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
   * @throws NumberFormatException if the number is not formatted properly
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
    return true;
  }
  
  @Override
  public boolean isString() {
    return false;
  }
  
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
  
  @Override
  public String toString() {
    return wrap.toString();
  }
}