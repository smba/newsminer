package newsminer.rss;

import newsminer.util.DatabaseUtils;
import newsminer.util.TextUtils;

import java.io.IOException;
import java.sql.Array;
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
import java.lang.Math;

import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.similarity.PearsonCorrelation;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.AtomicGrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.vector.AbstractVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.Vector;

/**
 * Clusters articles that cover the same topic.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-07-18
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
        "SELECT link, text FROM rss_articles")) {
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
    final List<Set<Integer>> clusters   = clustering.cluster(matrix, clusteringProperties).clusters(); //TODO order clusters
    
    final PearsonCorrelation pearson = new edu.ucla.sspace.similarity.PearsonCorrelation();
    
    //Store the clusters.
    try (final PreparedStatement insertCluster = DatabaseUtils.getConnection().prepareStatement(
        "INSERT INTO rss_article_clusters(timestamp, articles) VALUES (?, ?)")) {
      final long timestamp = System.currentTimeMillis();
      for (Set<Integer> cluster : clusters) {
        if (cluster.size() > 1) {
          final List<String> articles = new LinkedList<>(); 
          
          //build the similarity matrix
          ArrayMatrix similarityMatrix = new ArrayMatrix(cluster.size(), cluster.size());
          List<Integer> clusterList = new ArrayList<Integer>(cluster); 
          
          for (int i = 0; i < cluster.size(); i++) {
            for (int j = i; j < cluster.size(); j++) {
              if (i == j) {
                continue;
              } else {
                DoubleVector a = matrix.getRowVector(clusterList.get(i));
                DoubleVector b = matrix.getRowVector(clusterList.get(j));
                similarityMatrix.set(i, j, pearsonDistance(pearson.sim(a, b)));
                similarityMatrix.set(i, j, pearsonDistance(pearson.sim(b, a)));
              }
            }
          }
          
          //get vector with minimum sum
          double minimum = Double.MAX_VALUE;
          int min_id = -1;
          for (int i = 0; i < similarityMatrix.rows(); i++) {
            if (vectorSum(similarityMatrix.getRowVector(i)) < minimum) {
              minimum = vectorSum(similarityMatrix.getRowVector(i));
              min_id = i;
            }
          }
          
          int centroid = clusterList.get(min_id);
          articles.add(links.get(centroid));
          
          for (int index : clusterList) {
            if (index == centroid) {
              continue;
            } else {
              final String link = links.get(index);
              articles.add(link); 
            }
          }
          //TODO centroid review
          
          final Array articlesArray = DatabaseUtils.getConnection().createArrayOf("text", articles.toArray());
          insertCluster.setLong (1, timestamp);
          insertCluster.setArray(2, articlesArray);
          insertCluster.addBatch();
        }
      }
      insertCluster.executeBatch();
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
  
  private double pearsonDistance(double correlation) {
    return (correlation < 0) ? Math.abs(correlation) : 1 - correlation;
  }
  private double vectorSum(Vector v) {
    double sum = 0.0;
    for (int i = 0; i < v.length(); i++) {
      sum += (Double)v.getValue(i);
    }
    return sum;
    
  }
  
}