package newsminer.rss;

import newsminer.util.DatabaseUtils;
import newsminer.util.TextUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;

/**
 * Clusters articles that cover same topics.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-06-10
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
   * Clusters the articles and stores the result.
   */
  public void cluster() {
    try (final PreparedStatement ps = DatabaseUtils.getConnection().prepareStatement( //Get the articles.
        "SELECT * FROM rss_articles")) {
      final ResultSet articlesRS = ps.executeQuery();
      
      //Build the matrix.
      final Set<String> tagUniverse = new LinkedHashSet<String>(); //column headers
      final Matrix      matrix      = new GrowingSparseMatrix();
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
        
        //Get the text's tag distribution.
        final Map<String, Integer> tagDistribution = TextUtils.getTagDistribution(text);
        
        //Add all new tags to the tag universe.
        tagUniverse.addAll(tagDistribution.keySet());
        
        //Build the vector for this tag distribution and add it as a row to the matrix.
        matrix.setRow(i, buildVector(tagUniverse, tagDistribution));
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
  private static DoubleVector buildVector(Set<String> tagUniverse, Map<String, Integer> tagDistribution) {
    //Build the vector.
    final double[] array = new double[tagUniverse.size()];
    int i = 0;
    for (String tag : tagUniverse) {
      final Integer count = tagDistribution.get(tag);
      array[i] = count != null ? count : 0.0;
      i++;
    }
    final DoubleVector vector = new CompactSparseVector(array);
    
    //Normalize the vector.
    return new ScaledDoubleVector(vector, 1.0/vector.magnitude());
  }
}