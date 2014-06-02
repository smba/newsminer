package newsminer.rss;

import newsminer.util.TextUtils;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.vector.CompactSparseVector;

/**
 * @author Stefan Muehlbauer
 * @author Timo Guenther
 * 
 */
public class ArticleClusterer {

  //Instance regarding to the Singleton Pattern
  private final static ArticleClusterer instance = new ArticleClusterer();
  
  //simFunc DEFAULT
  private final SimType simFunc = edu.ucla.sspace.common.Similarity.SimType.COSINE;
  
  //threshold default
  private String threshold = "1.0";
  
  //word universe
  private List<String> universe;
  
  //texts
  private String[] texts;
  
  //matrix 
  private edu.ucla.sspace.matrix.Matrix matrix;
  
  //clusterer
  private edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering clusterer;
  
  private ArticleClusterer() {
    //
  }
  
  public static ArticleClusterer getInstance() {
    return instance;
  }
  
  public void fetchArticles() {
    /*
     * TODO Database usage
     */
  }
  
  public void storeClusters() {
    /*
     * TODO Database usage
     */
  }
  
  public void buildClusters() {
    clusterer = new edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering();
    
    this.fetchArticles();
    this.buildMatrix();
    Properties properties = new Properties();
    properties.put("edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.simFunc", this.simFunc);
    properties.put("edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.clusterThreshold", this.threshold);
    
    clusterer.cluster(this.matrix, properties);
    
    this.storeClusters();
    //TODO
  }
  
  /**
   * Builds the required matrix
   */
  private void buildMatrix() {
    this.buildUniverse();
    matrix = new edu.ucla.sspace.matrix.ArrayMatrix(this.texts.length, universe.size());
    int i = 0;
    for (String text : this.texts) {
      matrix.setRow(i, buildVector(text));
      i++;
    }
  }
  
  /**
   * builds the universe
   * 
   * @param texts
   * @return 
   * @return
   */
  private void buildUniverse() {
    Set<String> universeSet = new LinkedHashSet<String>();
    for (String text : this.texts) {
      universeSet.addAll(TextUtils.getWordDistribution(text).keySet());
    }
    List<String> _universe = new LinkedList<String>();
    for (String word : universeSet) {
      _universe.add(word);
    }
    this.universe = _universe;
  }
  
  /**
   * Returns a vector with a 1 where the tag is found in the universe
   * 
   * @param tagUniverse
   *          set of all tags
   * @param tags
   *          tags within the tag universe
   * @return a vector with a 1 where the tag is found in the universe
   */
  private CompactSparseVector buildVector(String text) {
    final double[] r = new double[this.universe.size()];
    int i = 0;
    Map<String, Integer> distribution = TextUtils.getWordDistribution(text);
    for (String word : this.universe) {
      r[i] = distribution.keySet().contains(word) ? distribution.get(word) : 0.0;
      i++;
    }
    CompactSparseVector raw = new CompactSparseVector(r);
    
    //normalizing
    double[] r_ = r;
    for (int j = 0; j < r.length; j++) {
      r_[j] = r[j] / raw.magnitude();
    }
    return new CompactSparseVector(r_);
  }  
  
  /**
   * Get the current threshold
   * @return
   */
  public double getThreshold() {
    return Double.parseDouble(threshold);
  }
  
  /**
   * Set the threshold
   * @param threshold
   */
  public void setThreshold(double threshold) {
    this.threshold = String.valueOf(threshold);
  }
}
