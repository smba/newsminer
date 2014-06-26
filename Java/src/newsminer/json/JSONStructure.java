package newsminer.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Represents a JSON structure.
 * This can be a {@link JSONObject} or a {@link JSONArray}.
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
 * @see     JSONObject
 * @see     JSONArray
 */
public abstract class JSONStructure extends JSONValue {
  //Serializable constants
	/** Serial Version UID */
  private static final long serialVersionUID = 6569799656970109772L;
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isPrimitive()
   */
  @Override
  public boolean isPrimitive() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see newsminer.json.JSONValue#isStructure()
   */
  @Override
  public boolean isStructure() {
    return true;
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