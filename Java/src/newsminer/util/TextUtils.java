package newsminer.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import edu.ucla.sspace.text.EnglishStemmer;
import edu.ucla.sspace.text.Stemmer;

/**
 * Provides methods for handling text.
 * 
 * @author  Timo Guenther
 * @version 2014-04-22
 */
public abstract class TextUtils {
  //constants
  /** used for stemming words */
  private static final Stemmer     STEMMER           = new EnglishStemmer();
  /** path to the file for the tags to be emitted */
  private static final String      PATH_STOPWORDS    = "stopwords.txt";
  /** tags to be emitted */
  private static final Set<String> STOPWORDS         = new TreeSet<String>();
  static {
    try (final BufferedReader br = new BufferedReader(new FileReader(PATH_STOPWORDS))) {
      String line;
      while ((line = br.readLine()) != null) {
        STOPWORDS.add(line);
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  
  /**
   * Returns all the tags (words) in the given text.
   * Uses the default language.
   * @param  text text to be split
   * @return all the tags (words) in the given text
   * @see    getTags(String, Locale)
   */
  public static Set<String> getTags(String text) {
    return getTags(text, Locale.getDefault());
  }
  
  /**
   * Returns all the tags (words) in the given text.
   * @param  text text to be split
   * @param  locale language for the word iterator
   * @return all the tags (words) in the given text
   */
  public static Set<String> getTags(String text, Locale locale) {
    final Set<String>   tags         = new TreeSet<String>();
    final BreakIterator wordIterator = BreakIterator.getWordInstance(locale);
    wordIterator.setText(text);
    int start = wordIterator.first();
    int end   = wordIterator.next();
    while (end != BreakIterator.DONE) {
      String tag = text.substring(start, end);
      if (Character.isLetterOrDigit(tag.charAt(0))) {
        tag = STEMMER.stem(tag.toLowerCase());
        if (!STOPWORDS.contains(tag)) {
          tags.add(tag);
        }
      }
      start = end;
      end   = wordIterator.next();
    }
    return tags;
  }
}