package newsminer.json;

import javax.xml.bind.TypeConstraintException;

/**
 * A {@link JSONValue} representing a JSON primitive.
 * This can be a {@link JSONNumber}, {@link JSONString}, {@link JSONBoolean} or {@link JSONNull}.
 * These types differ from a {@link JSONStructure} in that they do not contain other JSONValue instances.
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 * @see     JSONValue
 * @see     JSONNumber
 * @see     JSONString
 * @see     JSONBoolean
 * @see     JSONNull
 * @see     JSONStructure
 */
public abstract class JSONPrimitive extends JSONValue {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 5813110611935038816L;
  
  @Override
  protected JSONValue get(int index, Object... jsonPath)
      throws TypeConstraintException, IndexOutOfBoundsException {
    if (index == jsonPath.length) {
      return this;
    }
    throw new TypeConstraintException("Expected JSON structure but got this instance of " + getClass() + " instead: " + this);
  }
  
  @Override
  protected <U> U get(int index, Class<U> clazz, Object... jsonPath)
      throws TypeConstraintException, ClassCastException, IndexOutOfBoundsException {
    if (index == jsonPath.length) {
      return castTo(clazz);
    }
    throw new TypeConstraintException("Expected JSON structure but got this instance of " + getClass() + " instead: " + this);
  }
  
  @Override
  protected <U> U get(int index, Class<U> clazz, U defaultValue, Object... jsonPath)
      throws TypeConstraintException, ClassCastException {
    if (index == jsonPath.length) {
      final U castValue = castTo(clazz);
      return castValue == null ? defaultValue : castValue;
    }
    throw new TypeConstraintException("Expected JSON structure but got this instance of " + getClass() + " instead: " + this);
  }
  
  @Override
  public boolean isArray() {
    return false;
  }
  
  @Override
  public boolean isObject() {
    return false;
  }
  
  @Override
  public boolean isPrimitive() {
    return true;
  }
  
  @Override
  public boolean isStructure() {
    return false;
  }
  
  @Override
  StringBuilder appendTo(StringBuilder sb, int indent, int level) {
    return sb.append(toString());
  }
}