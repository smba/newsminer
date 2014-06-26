package newsminer.json;

/**
 * Represents a JSON primitive.
 * This can be a {@link JSONNumber}, {@link JSONString}, {@link JSONBoolean} or {@link JSONNull}.
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
 * @see     JSONNumber
 * @see     JSONString
 * @see     JSONBoolean
 * @see     JSONNull
 */
public abstract class JSONPrimitive extends JSONValue {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 5813110611935038816L;

  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isPrimitive()
   */
  @Override
  public boolean isPrimitive() {
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
   * @see newsminer.json.JSONValue#appendTo(java.lang.StringBuilder, int, int)
   */
  @Override
  StringBuilder appendTo(StringBuilder sb, int indent, int level) {
    return sb.append(toString());
  }
}