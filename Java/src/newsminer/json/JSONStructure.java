package newsminer.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import javax.xml.bind.TypeConstraintException;

/**
 * A {@link JSONValue} representing a JSON structure.
 * This can be either a {@link JSONObject} or a {@link JSONArray}.
 * Only these types may be at the root of any JSON document.
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 * @see     JSONValue
 * @see     JSONObject
 * @see     JSONArray
 */
public abstract class JSONStructure extends JSONValue {
  //Serializable constants
  /** Serial Version UID */
  private static final long serialVersionUID = 6569799656970109772L;
  
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
  public boolean isObject() {
    return false;
  }
  
  @Override
  public boolean isPrimitive() {
    return false;
  }
  
  @Override
  public boolean isString() {
    return false;
  }
  
  @Override
  public boolean isStructure() {
    return true;
  }
  
  /**
   * Returns the last node of the given JSON path or null if it or any of its parents does not exist.
   * @param  jsonPath a series of keys and indexes to be traversed
   * @return the last node of the given JSON path or null if it or any of its parents does not exist
   * @throws TypeConstraintException if any node other than the last one is a primitive value;
   *                                 if any node other than the last one is a {@link JSONArray} but the corresponding path key is a String or {@link JSONString};
   *                                 if any node other than the last one is a {@link JSONObject} but the corresponding path key is an integer
   * @throws IndexOutOfBoundsException if any index is out of range
   */
  public JSONValue get(Object... jsonPath)
      throws TypeConstraintException, IndexOutOfBoundsException {
    if (jsonPath.length == 0) {
      return this;
    }
    return get(0, jsonPath);
  }
  
  /**
   * Returns the last node of the given JSON path casted to the given type or null if it or any of its parents does not exist.
   * @param  clazz class of the type to be casted to
   * @param  jsonPath a series of keys and indexes to be traversed
   * @return the last node of the given JSON path casted to the given type or null if it or any of its parents does not exist
   * @throws TypeConstraintException if any node other than the last one is a primitive value;
   *                                 if any node other than the last one is a {@link JSONArray} but the corresponding path key is a String or {@link JSONString};
   *                                 if any node other than the last one is a {@link JSONObject} but the corresponding path key is an integer
   * @throws ClassCastException if the object is not assignable to the given type;
   * @throws IndexOutOfBoundsException if any index is out of range
   */
  public <U> U get(Class<U> clazz, Object... jsonPath)
      throws TypeConstraintException, ClassCastException, IndexOutOfBoundsException {
    if (jsonPath.length == 0) {
      return castTo(clazz);
    }
    return get(0, clazz, jsonPath);
  }
  
  /**
   * Returns the the last node of the given JSON path casted to the given type or the default value if it or any of its parents does not exist or is null or any index is out of range.
   * @param  clazz class of the type to be casted to
   * @param  defaultValue default value
   * @param  jsonPath a series of keys and indexes to be traversed
   * @return the the last node of the given JSON path casted to the given type or the default value if it or any of its parents does not exist or is null or any index is out of range
   * @throws TypeConstraintException if any node other than the last one is a primitive value;
   *                                 if any node other than the last one is a {@link JSONArray} but the corresponding path key is a String or {@link JSONString};
   *                                 if any node other than the last one is a {@link JSONObject} but the corresponding path key is an integer
   * @throws ClassCastException if the object is not assignable to the given type
   */
  public <U> U get(Class<U> clazz, U defaultValue, Object... jsonPath)
      throws TypeConstraintException, ClassCastException {
    if (jsonPath.length == 0) {
      final U castValue = castTo(clazz);
      return castValue == null ? defaultValue : castValue;
    }
    return get(0, clazz, defaultValue, jsonPath);
  }
  
  /**
   * Writes this to the given file.
   * @param  file file to be written to
   * @throws IOException
   */
  public void writeToFile(File file) throws IOException {
    writeToFile(file, -1);
  }
  
  /**
   * Writes this to the given file with or without added indent.
   * @param  file file to be written to
   * @param  indent amount of spaces added per block level;
   *                0 for newlines without indent;
   *                any negative value for no newlines
   * @throws IOException
   */
  public void writeToFile(File file, int indent) throws IOException {
    final File parentFile = file.getParentFile();
    if (parentFile != null) {
      parentFile.mkdirs();
    }
    file.createNewFile();
    try (final OutputStream out = new FileOutputStream(file)) {
      writeToStream(out, indent);
    }
  }
  
  /**
   * Writes this to the given OutputStream.
   * @param  out OutputStream to be written to
   * @throws IOException
   */
  public void writeToStream(OutputStream out) throws IOException {
    writeToStream(out, -1);
  }

  /**
   * Writes this to the given OutputStream with or without added indent.
   * @param  out OutputStream to be written to
   * @param  indent amount of spaces added per block level;
   *                0 for newlines without indent;
   *                any negative value for no newlines
   * @throws IOException
   */
  public void writeToStream(OutputStream out, int indent) throws IOException {
    final Writer writer = new PrintWriter(out);
    writer.write(toString(indent));
  }
  
  /**
   * Returns the textual representation of this as a String with or without added indent.
   * @param  indent amount of spaces added per block level;
   *                0 for newlines without indent;
   *                any negative value for no newlines
   * @return this as String
   */
  public String toString(int indent) {
    return appendTo(new StringBuilder(), indent, 0).toString();
  }
}