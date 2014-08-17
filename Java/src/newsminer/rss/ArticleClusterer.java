package newsminer.rss;

import newsminer.util.DatabaseUtils;
import newsminer.util.TextUtils;

import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.lang.Math;

import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.matrix.AtomicGrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;

/**
 * Clusters articles that cover the same topic.
 * Gets called when the {@link RSSCrawler} instance has finished crawling.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-08-17
 */
public class ArticleClusterer implements Observer {
  //constants
  /** function used to determine similarity */
  private static final SimType SIMILARITY_TYPE = SimType.PEARSON_CORRELATION;
  /** threshold value used in clustering */
  private static final String  THRESHOLD       = "0.04";
  /** the span of time of one clustering */
  private static final long    TIMEFRAME       = TimeUnit.HOURS.toMillis(24);
  
  //attributes
   /** properties for the clustering */
  private final Properties clusteringProperties;
  
  /**
   * Constructs a new instance of this class.
   */
  public ArticleClusterer() {
    clusteringProperties = new Properties();
    clusteringProperties.put("edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.simFunc",          SIMILARITY_TYPE);
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
   * Clusters the articles within the current time-frame and stores the result.
   * @return the amount of clusters found
   * @throws IOException when the articles could not be loaded or the clusters could not be stored
   */
  public int cluster() throws IOException {
    //Get the matrix.
    final Matrix       matrix      = new AtomicGrowingSparseMatrix();
    final Set<String>  tagUniverse = new LinkedHashSet<>(); //column headers
          List<String> links       = new LinkedList<>();    //row headers
    try (final Connection        con = DatabaseUtils.getConnectionPool().getConnection();
         final PreparedStatement ps  = con.prepareStatement(
             "SELECT link, text FROM rss_articles "
             + "WHERE timestamp > (SELECT MAX(timestamp) FROM rss_articles) - ?")) {
      //Get the newest articles.
      ps.setLong(1, TIMEFRAME);
      final ResultSet rs = ps.executeQuery();
      
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
    final Clustering         clustering         = new HierarchicalAgglomerativeClustering();
    final Assignments        clusterAssignments = clustering.cluster(matrix, clusteringProperties);
    final List<Set<Integer>> clusters           = clusterAssignments.clusters();
    final DoubleVector[]     clusterCentroids   = clusterAssignments.getSparseCentroids();
    
    //Store the clusters.
    final long timestamp = System.currentTimeMillis();
    int clusterIndex = -1;
    for (final Set<Integer> cluster : clusters) {
      clusterIndex++;
      final int clusterSize = cluster.size();
      if (clusterSize > 1) {
        //Get each article's score, that is the corresponding vector's similarity to the cluster centroid.
        final DoubleVector        clusterCentroid = clusterCentroids[clusterIndex];
        final Map<String, Double> articleScores   = new LinkedHashMap<>(clusterSize);
        for (final int articleIndex : cluster) {
          final String link  = links.get(articleIndex);
          final double score = Math.abs(Similarity.getSimilarity(SIMILARITY_TYPE, clusterCentroid, matrix.getRowVector(articleIndex)));
          articleScores.put(link, score);
        }
        
        //Sort the cluster's articles by descending score.
        final Map<String, Double> articleScoresSorted = new TreeMap<>(new Comparator<String>() {
          @Override
          public int compare(String o1, String o2) {
            return articleScores.get(o2).compareTo(articleScores.get(o1));
          }
        });
        articleScoresSorted.putAll(articleScores);
        
        //Get the common entities, that is the entities shared by the cluster and its articles, as well as their scores.
        final Map<String, Map<String, Double>> entityScoresByNameByType = new TreeMap<>();
        try (final Connection con = DatabaseUtils.getConnectionPool().getConnection()) {
          final Array articlesArray = con.createArrayOf("text", articleScores.keySet().toArray());
          for (final String type : new String[] {"location", "organization", "person"}) {
            final PreparedStatement ps = con.prepareStatement(
                "SELECT e.name, e.popularity FROM "
                + "("
                  + "("
                    + "SELECT name "
                    + "FROM rss_articles_entity_" + type + "s "
                    + "WHERE link = ANY(?) "
                  + ") AS a "
                  + "NATURAL JOIN "
                  + "entity_" + type + "s AS e"
                + ")");
            ps.setArray(1, articlesArray);
            final ResultSet           rs                 = ps.executeQuery();
            final Map<String, Double> entityScoresByName = new LinkedHashMap<>();
            while (rs.next()) {
              final String name        = rs.getString("name");
              final double popularity  = rs.getDouble("popularity");
              final double entityScore = popularity; //TODO use better relevance measure than popularity
              entityScoresByName.put(name, entityScore);
            }
            if (!entityScoresByName.isEmpty()) {
              entityScoresByNameByType.put(type, entityScoresByName);
            }
          }
       } catch (SQLException sqle) {
         throw new IOException(sqle);
       }
        
        //Get the common entity count.
        int commonEntities = 0;
        for (Entry<String, Map<String, Double>> entityScoresByNameAndType : entityScoresByNameByType.entrySet()) {
          final Set<String> entityNames = entityScoresByNameAndType.getValue().keySet();
          commonEntities += entityNames.size();
        }
        final double common_entities = Math.log(commonEntities);
        
        //Get the cluster's score.
        final double score = Math.log(Math.pow(articleScores.keySet().size(), 2));
        
        //Store the cluster in the database.
        final int id;
        try (final Connection        con = DatabaseUtils.getConnectionPool().getConnection();
             final PreparedStatement ps  = con.prepareStatement(
                 "INSERT INTO rss_article_clusters VALUES (DEFAULT, ?, ?, ?) "
                 + "RETURNING id")) {
          ps.setLong  (1, timestamp);
          ps.setDouble(2, score);
          ps.setDouble(3, common_entities);
          final ResultSet rs = ps.executeQuery();
          rs.next();
          id = rs.getInt(1);
        } catch (SQLException sqle) {
          throw new IOException(sqle);
        }
        
        //Store the cluster's articles in the database.
        try (final Connection        con = DatabaseUtils.getConnectionPool().getConnection();
             final PreparedStatement ps  = con.prepareStatement(
                 "INSERT INTO rss_article_clusters_rss_articles VALUES (?, ?, ?)")) {
          for (final Entry<String, Double> linkAndScore : articleScoresSorted.entrySet()) {
            final String link         = linkAndScore.getKey();
            final double articleScore = linkAndScore.getValue();
            ps.setInt   (1, id);
            ps.setString(2, link);
            ps.setDouble(3, articleScore);
            ps.addBatch();
          }
          ps.executeBatch();
        } catch (SQLException sqle) {
          throw new IOException(sqle);
        }
        
        //Store the cluster's entities in the database.
        for (final Entry<String, Map<String, Double>> entityScoresByNameAndType : entityScoresByNameByType.entrySet()) {
          final String              type               = entityScoresByNameAndType.getKey();
          final Map<String, Double> entityScoresByName = entityScoresByNameAndType.getValue();
          try (final Connection        con = DatabaseUtils.getConnectionPool().getConnection();
               final PreparedStatement ps  = con.prepareStatement(
                   "INSERT INTO rss_article_clusters_entity_" + type + "s VALUES (?, ?, ?)")) {
            for (final Entry<String, Double> entityScoreAndName : entityScoresByName.entrySet()) {
              final String name        = entityScoreAndName.getKey();
              final double entityScore = entityScoreAndName.getValue();
              ps.setInt   (1, id);
              ps.setString(2, name);
              ps.setDouble(3, entityScore);
              ps.addBatch();
            }
            ps.executeBatch();
          } catch (SQLException sqle) {
            throw new IOException(sqle);
          }
        }
      }
    }
    return clusters.size();
  }
  
  /**
   * Returns a vector with the occurrence count of the key where it is found in the universe.
   * @param  universe set of all possible keys
   * @param  distribution a subset of the universe mapped to each key's occurrence count
   * @return a vector with the occurrence count of the key where it is found in the universe
   */
  private static <K> DoubleVector getVector(Set<K> universe, Map<K, Integer> distribution) {
    //Build the vector.
    final double[] array = new double[universe.size()];
    int i = 0;
    for (K key : universe) {
      final Integer count = distribution.get(key);
      array[i] = count != null ? count : 0.0;
      i++;
    }
    final DoubleVector vector = new CompactSparseVector(array);
    
    //Normalize the vector.
    return new ScaledDoubleVector(vector, 1.0/vector.magnitude());
  }
}