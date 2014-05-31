package newsminer.rss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import newsminer.util.TextUtils;
import edu.ucla.sspace.similarity.CosineSimilarity;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.common.Similarity.SimType;

/**
 * @author Stefan Muehlbauer
 * @author Timo Guenther
 * 
 */
public class ArticleClusterer {
  private static final Object MIN_CLUSTER_SIMILARITY_PROPERTY = 5.3;

  public ArticleClusterer() {
    
  }
  
  public void foo() {
    List<String> texts = new ArrayList<String>();
    texts.add("old old very very old old mac");
    texts.add("old donald");
    texts.add("old has");
    texts.add("old farm");
    
    Set<String> tagUniverse = buildUniverse(texts);
    
    edu.ucla.sspace.matrix.Matrix mat = new edu.ucla.sspace.matrix.ArrayMatrix(
        texts.size(), tagUniverse.size());
    int i = 0;
    for (String text : texts) {
      Set<String> tags = new HashSet<String>();
      CompactSparseVector v = buildVector(tagUniverse, text);
      // System.out.println(v.toString());
      mat.setRow(i, v);
      System.out.println(mat.getRowVector(i));
      i++;
    }
    
    edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering clusterer = new edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering();
    
    Properties prop = new Properties();
    prop.put("edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.simFunc", edu.ucla.sspace.common.Similarity.SimType.COSINE);
    prop.put("edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.clusterThreshold", "0.6");
    clusterer.cluster(mat, prop); //TODO
  }
  
  /**
   * builds universe
   * 
   * @param texts
   * @return
   */
  private Set<String> buildUniverse(List<String> texts) {
    Set<String> universe = new LinkedHashSet<String>();
    for (String text : texts) {
      universe.addAll(TextUtils.getWordsDistribution(text).keySet());
    }
    return universe;
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
  private CompactSparseVector buildVector(Set<String> universe, String text) {
    final double[] r = new double[universe.size()];
    int i = 0;
    Map<String, Integer> distribution = TextUtils.getWordsDistribution(text);
    for (String word : universe) {
      r[i] = distribution.keySet().contains(word) ? distribution.get(word) : 0.0;
      i++;
    }
    CompactSparseVector raw = new CompactSparseVector(r);
    
    //normalisieren
    double[] r_ = r;
    for (int j = 0; j < r.length; j++) {
      r_[j] = r[j] / raw.magnitude();
    }
    return new CompactSparseVector(r_);
  }  
}
