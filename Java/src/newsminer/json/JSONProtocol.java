package newsminer.json;

/**
 * Provides constants and utility methods for the JSON protocol as defined in RFC 4627.
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
 */
public abstract class JSONProtocol {
  //character constants
  /** structural character for opening arrays */
  public static final char CHAR_BEGIN_ARRAY     = '[';
  /** structural character for opening objects */
  public static final char CHAR_BEGIN_OBJECT    = '{';
  /** structural character for closing arrays */
  public static final char CHAR_END_ARRAY       = ']';
  /** structural character for closing objects */
  public static final char CHAR_END_OBJECT      = '}';
  /** structural character for separating a key from its value */
  public static final char CHAR_NAME_SEPARATOR  = ':';
  /** structural character for separating a value from another value */
  public static final char CHAR_VALUE_SEPARATOR = ',';
  /** character to determine the start or end of a string */
  public static final char CHAR_QUOTATION_MARK  = '"';
  /** character for escaping control characters within a string */
  public static final char CHAR_ESCAPE          = '\\';
  
  /**
   * Returns true if the given character is a whitespace character.
   * @param  ch the character in question
   * @return true if the given character is a whitespace character
   */
  public static boolean isWhitespace(char ch) {
    switch (ch) {
      case 0x20: //space
      case 0x09: //horizontal tab
      case 0x0A: //line feed or new line
      case 0x0D: //carriage return
        return true;
      default:
        return false;
    }
  }
  
  /**
   * Returns true if the given character is an ASCII control character.
   * @param  ch the character in question
   * @return true if the given character is an ASCII control character
   */
  public static boolean isControlChar(char ch) {
    return ch < 0x20;
  }
  
  /**
   * Returns true if the given character is beyond the safe range.
   * @param  ch the character in question
   * @return true if the given character is beyond the safe range
   */
  public static boolean isBeyondUnicode(char ch) {
    return ch > 0x10FFFF;
  }
  
  /**
   * Returns true if the given character needs to be escaped.
   * @param  ch the character in question
   * @return true if the given character needs to be escaped
   */
  public static boolean isUnsafe(char ch) {
    return isControlChar(ch) ||
        ch == CHAR_QUOTATION_MARK ||
        ch == CHAR_ESCAPE ||
        isBeyondUnicode(ch);
  }
}