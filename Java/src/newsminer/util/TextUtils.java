package newsminer.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
   * Returns all the words in the given text. Stemmed. Stopworded
   * Uses the default language.
   * @param  text text to be split
   * @return all the tags (words) in the given text
   * @see    getTags(String, Locale)
   */
  @Deprecated
  public static Set<String> getTags(String text) {
    return getTags(text, Locale.getDefault());
  }
  
  /**
   * Returns all the tags (words) in the given text.
   * @param  text text to be split
   * @param  locale language for the word iterator
   * @return all the tags (words) in the given text
   */
  @Deprecated
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
  
  /**
   * count words in token array
   * 
   * @param word
   * @param tokens
   * @return
   */
  public static int countWord(String word, String[] tokens) {
    int count = 0;
    for (String token : tokens) {
      if (word.toLowerCase().compareTo(token.toLowerCase()) == 0) {
        count++;
      }
    }
    return count;
  }
  
  /**
   * Returns all occurence of all contained words, stopworded, stemmed
   *  and lowercased.
   * @param text
   * @return
   */
  public static Map<String, Integer> getWordsDistribution(String text) {
    String[] tokens = text.replace(","," ").replace(".", " ").split(" ");
    Map<String, Integer> distribution = new HashMap<String, Integer>();
    for (String token : tokens) {
      token = STEMMER.stem(token.toLowerCase());
      if (!STOPWORDS.contains(token)) {
        //word is valid
        if (!distribution.containsKey(token)) {
          distribution.put(token, 1);
        } else {
          distribution.put(token, distribution.get(token) + 1);
        }
      }
    }
    return distribution;
  }
}