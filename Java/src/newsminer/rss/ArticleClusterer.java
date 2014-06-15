package newsminer.rss;

import newsminer.util.DatabaseUtils;
import newsminer.util.TextUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.matrix.AtomicGrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;

/**
 * Clusters articles that cover same topics.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-06-15
 */
public class ArticleClusterer implements Observer { //TODO Observable
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
  
  @Override
  public void update(Observable o, Object arg) {
    System.out.println("ArticleClusterer.update");
    if (o instanceof RSSCrawler) {
      //Start clustering.
      System.out.println("Clustering articles.");
      final long startTimestamp = System.currentTimeMillis();
      
      //Cluster.
      cluster();
      
      //Finish clustering.
      final long finishTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimestamp);
      System.out.println("Finished clustering articles in " + finishTimeSeconds + " second" + (finishTimeSeconds == 1 ? "" : "s") + ".");
    }
  }

  /**
   * Clusters the articles and stores the result.
   */
  public synchronized void cluster() {
    try (final PreparedStatement ps = DatabaseUtils.getConnection().prepareStatement( //Get the articles.
        "SELECT * FROM rss_articles")) {
      final ResultSet articlesRS = ps.executeQuery();
      
      //Build the matrix.
      final Set<String>  tagUniverse = new LinkedHashSet<>(); //column headers
      final List<String> links       = new LinkedList<>();    //row headers
      final Matrix       matrix      = new AtomicGrowingSparseMatrix();
      int i = 0;
      while (articlesRS.next()) {
        //Get the text.
        final String text;
        try {
          text = articlesRS.getString("text");
          links.add(articlesRS.getString("link"));
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
      System.out.println(matrix.rows() + " by " + matrix.columns() + " matrix built.");
      
      //Cluster.
      final Clustering         clustering  = new HierarchicalAgglomerativeClustering();
      final List<Set<Integer>> clusters    = clustering.cluster(matrix, clusteringProperties).clusters();
      System.out.println("Clusters found: " + clusters.size());
      
      //Store the clusters.
      for (Set<Integer> cluster : clusters) {
        //Store a new cluster.
        //TODO
        
        //Store the cluster elements with the new cluster.
        for (int index : cluster) {
          final String link = links.get(index);
          //TODO store cluster element
        }
      }
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