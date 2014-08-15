package newsminer.json;

import java.text.ParseException;

/**
 * Provides methods for parsing JSON input according to RFC 4627.
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 */
public abstract class JSONParser {
  /**
   * Stores a JSON value and the index at which it ends.
   * 
   * @author  Timo Guenther
   * @version 2013-06-12
   */
  private static class ValueAndEndIndex {
    //attributes
    /** a JSON value */
    private final JSONValue value;
    /** the index at which the JSON value ends */
    private final int       endIndex;
    
    /**
     * Constructs a new instance of this class.
     * @param value
     * @param endIndex
     */
    public ValueAndEndIndex(JSONValue value, int endIndex) {
      this.value    = value;
      this.endIndex = endIndex;
    }
    
    /**
     * Returns the value.
     * @return the value
     */
    public JSONValue getValue() {
      return value;
    }
    
    /**
     * Returns the end index.
     * @return the end index
     */
    public int getEndIndex() {
      return endIndex;
    }
  }
  
  /**
   * Gets the JSON structure from the given String.
   * @param  line source String
   * @return a JSON structure
   * @throws ParseException if the given String does not contain a structure in JSON format
   */
  public static JSONStructure parse(String line) throws ParseException {
    if (line.length() == 0) {
      throw new ParseException("Expected non-empty input", 0);
    }
    int currentIndex = indexOfNonWhitespace(line, 0);
    if (currentIndex < 0) {
      throw new ParseException("Expected non-whitespace input", line.length());
    }
    final ValueAndEndIndex valueAndEndIndex = parseValue(line, currentIndex);
    final JSONValue        value            = valueAndEndIndex.getValue();
    currentIndex = indexOfNonWhitespace(line, valueAndEndIndex.getEndIndex());
    if (currentIndex >= 0) {
      throw new ParseException("Expected end of input instead of '" + line.charAt(currentIndex) + "' at index " + currentIndex, currentIndex);
    }
    if (value instanceof JSONObject) {
      return (JSONObject) value;
    }
    if (value instanceof JSONArray) {
      return (JSONArray) value;
    }
    throw new ParseException("Expected " + JSONObject.class.getSimpleName() + " or " + JSONArray.class.getSimpleName() + " instead of " + value.getClass().getSimpleName(), 0);
  }
  
  /**
   * Parses the JSON value at this exact offset from the given String and returns it together with the index at which it ends.
   * @param  line source String
   * @param  offset index at which this value starts
   * @return a JSON value and the index at which it ends
   * @throws ParseException if the given String does not contain a value in JSON format
   */
  private static ValueAndEndIndex parseValue(String line, int offset) throws ParseException {
    //Find the first non-whitespace character to determine what type of object this value is.
    final int  startIndex = indexOfNonWhitespace(line, offset);
    if (startIndex < 0) {
      throw new ParseException("Expected " + JSONValue.class.getSimpleName() + " instead of end of input", line.length());
    }
    final char startChar = line.charAt(startIndex);
    int  currentIndex = startIndex;
    
    //Check if this value is an object.
    if (startChar == JSONProtocol.CHAR_BEGIN_OBJECT) {
      currentIndex = indexOfNonWhitespace(line, currentIndex + 1);
      if (currentIndex < 0) {
        throw new ParseException("Expected " + JSONValue.class.getSimpleName() + " or object end instead of end of input", line.length());
      }
      final JSONObject jsonObject = new JSONObject();
      while (true) { //Look for entries or the object end.
        //Check if the object ends here.
        if (line.charAt(currentIndex) == JSONProtocol.CHAR_END_OBJECT) {
          currentIndex++;
          return new ValueAndEndIndex(jsonObject, currentIndex);
        }
        
        //Since the object does not end here, get the entry's key.
        final ValueAndEndIndex keyAndEndIndex = parseValue(line, currentIndex);
        final JSONValue        key            = keyAndEndIndex.getValue();
        if (!(key instanceof JSONString)) {
          throw new ParseException("Expected " + JSONString.class.getSimpleName() + " instead of " + key.getClass().getSimpleName() + " as key at index " + currentIndex, currentIndex);
        }
        currentIndex = keyAndEndIndex.getEndIndex(); //Continue where the recursion left off.
        
        //Look for the name separator.
        currentIndex = indexOfNonWhitespace(line, currentIndex);
        if (currentIndex < 0) {
          throw new ParseException("Expected name separator instead of end of input", line.length());
        }
        if (line.charAt(currentIndex) != JSONProtocol.CHAR_NAME_SEPARATOR) {
          throw new ParseException("Expected name separator instead of '" + line.charAt(currentIndex) + "' at index " + currentIndex, currentIndex);
        }
        currentIndex++;
        
        //Get the entry's value.
        final ValueAndEndIndex valueAndEndIndex = parseValue(line, currentIndex);
        final JSONValue        value            = valueAndEndIndex.getValue();
        currentIndex = valueAndEndIndex.getEndIndex(); //Continue where the recursion left off.
        
        //Look for the value separator.
        currentIndex = indexOfNonWhitespace(line, currentIndex);
        if (currentIndex < 0) {
          throw new ParseException("Expected value separator or object end instead of end of input", line.length());
        }
        if (line.charAt(currentIndex) == JSONProtocol.CHAR_VALUE_SEPARATOR) {
          currentIndex++;
          jsonObject.add((JSONString) key, value);
          continue;
        }
        if (line.charAt(currentIndex) == JSONProtocol.CHAR_END_OBJECT) {
          jsonObject.add((JSONString) key, value);
          continue;
        }
        throw new ParseException("Expected value separator or object end instead of '" + line.charAt(currentIndex) + "' at index " + currentIndex, currentIndex);
      }
    }
    
    //Check if this value is an array.
    if (startChar == JSONProtocol.CHAR_BEGIN_ARRAY) {
      currentIndex = indexOfNonWhitespace(line, currentIndex + 1);
      if (currentIndex < 0) {
        throw new ParseException("Expected value or array end instead of end of input", line.length());
      }
      final JSONArray jsonArray = new JSONArray();
      while (true) { //Look for values or the array end.
        //Check if the array ends here.
        if (line.charAt(currentIndex) == JSONProtocol.CHAR_END_ARRAY) {
          currentIndex++;
          return new ValueAndEndIndex(jsonArray, currentIndex);
        }
        
        //Since the array does not end here, get the value.
        final ValueAndEndIndex valueAndEndIndex = parseValue(line, currentIndex);
        final JSONValue        value            = valueAndEndIndex.getValue();
        currentIndex = valueAndEndIndex.getEndIndex(); //Continue where the recursion left off.
        
        //Look for the value separator.
        currentIndex = indexOfNonWhitespace(line, currentIndex);
        if (currentIndex < 0) {
          throw new ParseException("Expected value separator or array end instead of end of input", line.length());
        }
        if (line.charAt(currentIndex) == JSONProtocol.CHAR_VALUE_SEPARATOR) {
          currentIndex++;
          jsonArray.add((JSONValue) value);
          continue;
        }
        if (line.charAt(currentIndex) == JSONProtocol.CHAR_END_ARRAY) {
          jsonArray.add((JSONValue) value);
          continue;
        }
        throw new ParseException("Expected value separator or array end instead of '" + line.charAt(currentIndex) + "' at index " + currentIndex, currentIndex);
      }
    }
    
    //Check if this value is a string.
    if (startChar == JSONProtocol.CHAR_QUOTATION_MARK) {
      currentIndex++;
      final StringBuilder stringBuilder = new StringBuilder();
      while (true) { //Look for the string end.
        if (currentIndex >= line.length()) {
          throw new ParseException("Expected string end instead of end of input", currentIndex);
        }
        if (line.charAt(currentIndex) == JSONProtocol.CHAR_ESCAPE) { //If the next character is escaped, do not check if it is a control character.
          stringBuilder.append(line.charAt(currentIndex));
          currentIndex++;
          if (currentIndex >= line.length()) {
            throw new ParseException("Expected string end instead of end of input during escape", currentIndex);
          }
        } else if (line.charAt(currentIndex) == JSONProtocol.CHAR_QUOTATION_MARK) { //string end
          currentIndex++;
          return new ValueAndEndIndex(new JSONString(stringBuilder.toString()), currentIndex);
        }
        stringBuilder.append(line.charAt(currentIndex));
        currentIndex++;
      }
    }
    
    //Check if this value is numeric.
    if (Character.isDigit(startChar) || startChar == '-') {
      final StringBuilder numberBuilder = new StringBuilder();
      while (true) { //Look for the numeric end.
        if (currentIndex >= line.length()) {
          throw new ParseException("Expected numeral end instead of end of input", currentIndex);
        }
        if (!(Character.isDigit(line.charAt(currentIndex)) ||
            line.charAt(currentIndex) == '.' ||
            line.charAt(currentIndex) == '-' ||
            line.charAt(currentIndex) == '+' ||
            line.charAt(currentIndex) == 'E' ||
            line.charAt(currentIndex) == 'e')) { //numeric end
          final String numericString = numberBuilder.toString();
          final JSONNumber jsonNumber;
          try {
            jsonNumber = new JSONNumber(numericString);
          } catch (NumberFormatException nfe) {
            throw new ParseException(numericString + " is not a valid numeral", startIndex);
          }
          return new ValueAndEndIndex(jsonNumber, currentIndex);
        }
        numberBuilder.append(line.charAt(currentIndex));
        currentIndex++;
      }
    }
    
    //Check if this value is the true literal.
    if (line.startsWith(JSONBoolean.STRING_TRUE, currentIndex)) {
      return new ValueAndEndIndex(JSONBoolean.TRUE, currentIndex + JSONBoolean.STRING_TRUE.length());
    }
    
    //Check if this value is the false literal.
    if (line.startsWith(JSONBoolean.STRING_FALSE, currentIndex)) {
      return new ValueAndEndIndex(JSONBoolean.FALSE, currentIndex + JSONBoolean.STRING_FALSE.length());
    }
    
    //Check if this value is the null literal.
    if (line.startsWith(JSONNull.STRING_NULL, currentIndex)) {
      return new ValueAndEndIndex(JSONNull.NULL, currentIndex + JSONNull.STRING_NULL.length());
    }
    
    //Disallow this value to be anything other than one of the JSON values above.
    throw new ParseException("Expected " + JSONValue.class.getSimpleName() + " instead of '" + startChar + "' at index " + currentIndex, currentIndex);
  }
  
  /**
   * Returns the index within the given String of the first occurrence of a non-whitespace character, starting the search at the specified index.
   * @param  line source String
   * @param  fromIndex the index to start the search from
   * @return first index of a non-whitespace character or -1 if it does not occur
   */
  private static int indexOfNonWhitespace(String line, int fromIndex) {
    int currentIndex = fromIndex;
    while (currentIndex < line.length()) {
      if (!(JSONProtocol.isWhitespace(line.charAt(currentIndex)))) {
        return currentIndex;
      }
      currentIndex++;
    }
    return -1;
  }
}