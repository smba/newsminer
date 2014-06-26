package newsminer.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * @version 2014-06-04
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
  public static Set<String> getTags(String text) {
    return getTags(text, Locale.ENGLISH);
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
  
  /**
   * Returns the occurrence count of every tag (word) in the given text.
   * The words will be stemmed and will not contain stopwords.
   * @param  text text containing words
   * @return the occurrence count of every tag (word) in the given text
   */
  public static Map<String, Integer> getTagDistribution(String text) {
    return getTagDistribution(text, Locale.ENGLISH);
  }
  
  /**
   * Returns the occurrence count of every tag (word) in the given text.
   * The words will be stemmed and will not contain stopwords.
   * @param  text text containing words
   * @param  locale language of the word iterator
   * @return the occurrence count of every tag (word) in the given text
   */
  public static Map<String, Integer> getTagDistribution(String text, Locale locale) {
    final Map<String, Integer> distribution = new LinkedHashMap<String, Integer>();
    final BreakIterator        wordIterator = BreakIterator.getWordInstance(locale);
    wordIterator.setText(text);
    int start = wordIterator.first();
    int end   = wordIterator.next();
    while (end != BreakIterator.DONE) {
      String tag = text.substring(start, end);
      if (Character.isLetterOrDigit(tag.charAt(0))) {
        tag = STEMMER.stem(tag.toLowerCase());
        if (!STOPWORDS.contains(tag)) {
          final Integer count = distribution.get(tag);
          distribution.put(tag, count == null ? 1 : count + 1);
        }
      }
      start = end;
      end   = wordIterator.next();
    }
    return distribution;
  }
  
  /**
   * Extracts all tagged tokens in text by given tag.
   * @param text
   * @param tag
   * @return
   */
  private static Set<String> extractXMLTags(String text, String tag) {
    int i = 0;
    int b, start, end;
    String startTag = "<" + tag + ">";
    String endTag = "</" + tag + ">";
    Set<String> subs = new LinkedHashSet<String>();
    List<Integer> indizes = new ArrayList<Integer>(); 
    while ((text.indexOf(startTag, i) != -1) || (text.indexOf(endTag, i) != -1)) {
      start = text.indexOf(startTag, i);
      end = text.indexOf(endTag, i);
      b = (start < end) && (start != -1) ? start : end; 
      indizes.add(b);
      i = b + 1;
    }
    for (int j = 0; j < indizes.size(); j+=2) {
      subs.add(text.substring(indizes.get(j)+startTag.length(), indizes.get(j+1)));
    }
    return subs;
  }
  
  /**
   * Extracts all tagged tokens in text by given tags.
   * @param text
   * @param tags
   * @return
   */
  public static Map<String, Set<String>> extractXMLTags(String text, String[] tags) {
    Map<String, Set<String>> out = new HashMap<String, Set<String>>();
    Set<String> words;
    for (String tag : tags) {
      words = extractXMLTags(text, tag);
      System.err.println(words);
      out.put(tag, words);
    }
    return out;
  }
}