package newsminer.rss;

import newsminer.util.DatabaseUtils;
import newsminer.util.TextUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.vector.CompactSparseVector;

/**
 * Clusters articles that cover same topics.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-06-04
 */
public class ArticleClusterer { //TODO Observer, Observable
  //constants
  /** function used to determine similarity */
  private static final SimType SIM_FUNC  = SimType.COSINE;
  /** threshold value used in clustering */
  private static final String  THRESHOLD = "1.0";
  
  //attributes
   /** properties for the clustering */
  private final Properties clusteringProperties;
  
  /**
   * Constructs a new instance of this class.
   */
  public ArticleClusterer() {
    clusteringProperties = new Properties();
    clusteringProperties.put("edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.simFunc",          SIM_FUNC);
    clusteringProperties.put("edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.clusterThreshold", THRESHOLD);
  }
  
  /**
   * Clusters the articles.
   */
  protected void flush() {
    try (final PreparedStatement ps = DatabaseUtils.getConnection().prepareStatement( //Get the articles.
        "SELECT * FROM rss_articles", //TODO time
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY)) {
      final ResultSet articlesRS = ps.executeQuery();
      
      //Get all tags in all articles.
      final Set<String> tagUniverse = new TreeSet<String>();
      int articleCount = 0;
      while (articlesRS.next()) {
        //Get the text.
        final String text;
        try {
          text = articlesRS.getString("text");
        } catch (SQLException sqle) {
          sqle.printStackTrace();
          continue;
        }
        
        //Get the tags.
        tagUniverse.addAll(TextUtils.getTags(text)); //TODO language
        articleCount++;
      }
      articlesRS.beforeFirst();
      
      //Build the matrix.
      final Matrix matrix = new ArrayMatrix(articleCount, tagUniverse.size());
      int i = 0;
      while (articlesRS.next()) {
        //Get the text.
        final String text;
        try {
          text = articlesRS.getString("text");
        } catch (SQLException sqle) {
          sqle.printStackTrace();
          continue;
        }
        
        //Add the vector.
        matrix.setRow(i, buildVector(tagUniverse, TextUtils.getTagDistribution(text)));
        i++;
      }
      
      //Cluster.
      final Clustering clustering = new HierarchicalAgglomerativeClustering();
      clustering.cluster(matrix, clusteringProperties);
      
      //Store the clustering.
      //TODO
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }
  
  /**
   * Returns a vector with the occurrence count of the tag where it is found in the universe.
   * @param  tagUniverse set of all tags
   * @param  tagDistribution all tags and their occurrence count
   * @return a vector with the occurrence count of the tag where it is found in the universe
   */
  private static CompactSparseVector buildVector(Set<String> tagUniverse, Map<String, Integer> tagDistribution) {
    //Build the vector.
    final double[] r = new double[tagUniverse.size()];
    int i = 0;
    for (String tag : tagUniverse) {
      final Integer count = tagDistribution.get(tag);
      r[i] = count != null ? count : 0.0;
      i++;
    }
    final CompactSparseVector raw = new CompactSparseVector(r);
    
    //Normalize the vector.
    final double magnitude = raw.magnitude();
    for (int j = 0; j < r.length; j++) {
      r[j] /= magnitude;
    }
    return new CompactSparseVector(r);
  }
}