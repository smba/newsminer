package newsminer.rss;

import newsminer.util.DatabaseUtils;
import newsminer.util.TextUtils;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
 * Clusters articles that cover the same topic.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-06-27
 */
public class ArticleClusterer implements Observer { //TODO Observable
  //constants
  /** function used to determine similarity */
  private static final SimType SIM_FUNC  = SimType.PEARSON_CORRELATION;
  /** threshold value used in clustering */
  private static final String  THRESHOLD = "0.27";
  
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
    if (o instanceof RSSCrawler) {
      //Start clustering.
      System.out.println("Clustering articles.");
      final long startTimestamp = System.currentTimeMillis();
      
      //Cluster.
      final int clusters;
      try {
        clusters = cluster();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      
      //Finish clustering.
      final long finishTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimestamp);
      System.out.printf("Finished clustering articles into %d clusters in %s second%s.\n",
          clusters, finishTimeSeconds, finishTimeSeconds == 1 ? "" : "s");
    }
  }

  /**
   * Clusters the articles and stores the result.
   * @return the amount of clusters found
   * @throws IOException when the articles could not be loaded or the clusters could not be stored
   */
  public int cluster() throws IOException {
    //Get the matrix.
    final Matrix       matrix      = new AtomicGrowingSparseMatrix();
    final Set<String>  tagUniverse = new LinkedHashSet<>(); //column headers
          List<String> links       = new LinkedList<>();    //row headers
    try (final PreparedStatement selectArticles = DatabaseUtils.getConnection().prepareStatement(
        "SELECT * FROM rss_articles")) {
      //Get the articles.
      final ResultSet rs = selectArticles.executeQuery();
      
      //Add each article as one row.
      int i = 0;
      while (rs.next()) {
        //Get the text.
        final String text = rs.getString("text");
        final String link = rs.getString("link");
        
        //Get the text's tag distribution.
        final Map<String, Integer> tagDistribution = TextUtils.getTagDistribution(text);
        
        //Add all new tags to the tag universe.
        tagUniverse.addAll(tagDistribution.keySet());
        
        //Get the vector for this tag distribution and add it as a row to the matrix.
        matrix.setRow(i, getVector(tagUniverse, tagDistribution));
        links.add(link);
        i++;
      }
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
    links = new ArrayList<>(links); //faster lookup
    
    //Cluster.
    final Clustering         clustering = new HierarchicalAgglomerativeClustering();
    final List<Set<Integer>> clusters   = clustering.cluster(matrix, clusteringProperties).clusters();
    
    //Remove the old clusters.
    try (final PreparedStatement deleteClusters = DatabaseUtils.getConnection().prepareStatement(
        "DELETE FROM rss_article_clusters")) {
      deleteClusters.executeUpdate();
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
    
    //Store the clusters.
    int id = 0;
    try (final PreparedStatement insertCluster = DatabaseUtils.getConnection().prepareStatement(
        "INSERT INTO rss_article_clusters(id) VALUES (?)");
        final PreparedStatement updateArticleCluster = DatabaseUtils.getConnection().prepareStatement(
            "UPDATE rss_articles SET cluster_id = ? WHERE link = ?");) {
      for (Set<Integer> cluster : clusters) {
        //Store a new cluster.
        id++;
        insertCluster.setInt(1, id);
        insertCluster.addBatch();
        
        //Store the cluster elements with the new cluster.
        if (cluster.size() > 1) {
          for (int index : cluster) {
            final String link = links.get(index);
            updateArticleCluster.setInt   (1, id);
            updateArticleCluster.setString(2, link);
            updateArticleCluster.addBatch();
          }
        }
      }
      insertCluster.executeBatch();
      updateArticleCluster.executeBatch();
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
    return clusters.size();
  }
  
  /**
   * Returns a vector with the occurrence count of the tag where it is found in the universe.
   * @param  tagUniverse set of all tags
   * @param  tagDistribution all tags and their occurrence count
   * @return a vector with the occurrence count of the tag where it is found in the universe
   */
  private static DoubleVector getVector(Set<String> tagUniverse, Map<String, Integer> tagDistribution) {
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