package newsminer.rss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.matrix.Matrix;

/**
 * @author Stefan Muehlbauer
 * @author Timo Guenther
 * 
 */
public class ArticleClusterer {
  private static ArticleClusterer instance = new ArticleClusterer();
  
  private ArticleClusterer() {
    
  }
  public static ArticleClusterer getInstance() {
    return instance;
  }
  
  /**
   * Builds word universe from all texts given
   * @param texts
   * @return
   */
  private List<String> buildUniverse(List<String> texts) {
    Set<String> universeSet = new HashSet<String>();
    for (String text : texts) {
      String[] textSplit = text.replace(".", " ").replace(","," ").split(" ");
      for (String word : textSplit) {
        universeSet.add(word.toLowerCase());
      }
    }
    
    List<String> universe = new ArrayList<String>();
    for (String word : universeSet) {
      universe.add(word);
    }
    return universe;
  }
  
  /**
   * Builds double[] vector from given universe and text
   * @param universe
   * @param text
   * @return
   */
  private double[] buildVector(List<String> universe, String text) {
    double[] values = new double[universe.size()];
    String[] tokens = text.split(" ");
    for (int i = 0; i < universe.size(); i++) {
      values[i] = countWord(universe.get(i), tokens);
    }
    return values;
  }
  
  /**
   * Counts the occurencies of given word in token array.
   * @param word
   * @param tokens
   * @return
   */
  private int countWord(String word, String[] tokens) {
    int count = 0;
    for (String token : tokens) {
      if (token.compareTo(word) == 0) {
        count++;
      }
    }
    return count;
  }
  
  /**
   * Normalizes an vector
   * @param values
   * @return
   */
  private double[] normalize(double[] values) {
    double len = 0.0;
    for (double value : values) {
      len += Math.pow(value, 2);
    }
    len = Math.pow(len, 0.5);
    double[] out = values;
    for (int i = 0; i < values.length; i++) {
      out[i] = values[i] / len;
    }
    return out;
  }
}
