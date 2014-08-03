package newsminer.rss;

import newsminer.util.DatabaseUtils;
import newsminer.util.TextUtils;

import java.io.IOException;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.SortedSet;
import java.util.TreeSet;
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
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-08-02
 */
public class ArticleClusterer implements Observer { //TODO Observable
  //constants
  /** function used to determine similarity */
  private static final SimType SIMILARITY_TYPE  = SimType.PEARSON_CORRELATION;
  /** threshold value used in clustering */
  private static final String  THRESHOLD        = "0.03";
  
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
    final Clustering         clustering         = new HierarchicalAgglomerativeClustering();
    final Assignments        clusterAssignments = clustering.cluster(matrix, clusteringProperties);
    final List<Set<Integer>> clusters           = clusterAssignments.clusters(); //TODO order clusters
    final DoubleVector[]     clusterCentroids   = clusterAssignments.getSparseCentroids();
    
    //Store the clusters.
    try (final PreparedStatement insertCluster = DatabaseUtils.getConnection().prepareStatement(
        "INSERT INTO rss_article_clusters(timestamp, articles) VALUES (?, ?, ?, ?, ?, ?)")) {
      
      final PreparedStatement getLocationsScore = DatabaseUtils.getConnection().prepareStatement("SELECT * FROM entity_locations WHERE name = ?");
      final PreparedStatement getOrganizationsScore = DatabaseUtils.getConnection().prepareStatement("SELECT * FROM entity_organizations WHERE name = ?");
      final PreparedStatement getPersonsScore = DatabaseUtils.getConnection().prepareStatement("SELECT * FROM entity_persons WHERE name = ?");
      final long timestamp = System.currentTimeMillis();
      int clusterIndex = -1;
      for (final Set<Integer> cluster : clusters) {
        clusterIndex++;
        final int clusterSize = cluster.size();
        if (clusterSize > 1) {
          //Get each vector's score, that is the similarity to the cluster centroid.
          final DoubleVector         clusterCentroid = clusterCentroids[clusterIndex];
          final Map<Integer, Double> clusterScores   = new LinkedHashMap<>(clusterSize);
          for (final int articleIndex : cluster) {
            final double score = Math.abs(Similarity.getSimilarity(SIMILARITY_TYPE, clusterCentroid, matrix.getRowVector(articleIndex)));
            clusterScores.put(articleIndex, score);
          }
          
          //Sort the cluster by descending score.
          final SortedSet<Integer> clusterSorted = new TreeSet<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
              return clusterScores.get(o2).compareTo(clusterScores.get(o1));
            }
          });
          clusterSorted.addAll(clusterScores.keySet());
          
          //Store the cluster.
          final List<String> articles = new ArrayList<>(clusterSize);
          for (final int articleIndex : clusterSorted) {
            articles.add(links.get(articleIndex));
          }
                    
          final Array articlesArray = DatabaseUtils.getConnection().createArrayOf("text", articles.toArray());
          
          //Score the cluster itself
          final PreparedStatement getArticles = DatabaseUtils.getConnection().prepareStatement("SELECT * FROM rss_articles WHERE link = ANY(?)");
          getArticles.setArray(1, articlesArray);
          ResultSet articleResultSet = getArticles.executeQuery();
          Set<String> clusterLocations = new TreeSet<String>();
          Set<String> clusterOrganizations = new TreeSet<String>();
          Set<String> clusterPersons = new TreeSet<String>();
          List<List<String>> articleLocationsList = new LinkedList<List<String>>();
          List<List<String>> articleOrganizationsList = new LinkedList<List<String>>();
          List<List<String>> articlePersonsList = new LinkedList<List<String>>();
          while (articleResultSet.next()) {
            String[] entityLocations = (String[])articleResultSet.getArray("entity_locations").getArray();
            articleLocationsList.add((List<String>) Arrays.asList(entityLocations));
            clusterLocations.addAll(Arrays.asList(entityLocations));
            
            String[] entityOrganizations = (String[])articleResultSet.getArray("entity_organizations").getArray();
            articleOrganizationsList.add((List<String>) Arrays.asList(entityOrganizations));
            clusterOrganizations.addAll(Arrays.asList(entityOrganizations));
            
            String[] entityPersons = (String[])articleResultSet.getArray("entity_persons").getArray();
            articlePersonsList.add((List<String>) Arrays.asList(entityPersons));
            clusterPersons.addAll(Arrays.asList(entityPersons));
          }
          
          //calculate score for each type of entities
          double locationsScore = 0.0;
          for (String clusterLocation : clusterLocations) {
            int occurence = 0;
            for (List articleLocations : articleLocationsList) {
              if (articleLocations.contains(clusterLocation)) {
                occurence++;
              }
            }
            ///get score of location
            getLocationsScore.setString(1, clusterLocation);
            ResultSet popularitySet = getLocationsScore.executeQuery();
            double popularity = 0.0;
            while (popularitySet.next()) {
              popularity = popularitySet.getDouble("popularity");
              break;
            }
            locationsScore += popularity * 1.0/clusterLocations.size() * (double)occurence/articleLocationsList.size();
          }
          locationsScore /= clusterLocations.size();
          
          double organizationsScore = 0.0;
          for (String clusterOrganization : clusterOrganizations) {
            int occurence = 0;
            for (List articleOrganizations : articleOrganizationsList) {
              if (articleOrganizations.contains(clusterOrganization)) {
                occurence++;
              }
            }
            ///get score of location
            getOrganizationsScore.setString(1, clusterOrganization);
            ResultSet popularitySet = getOrganizationsScore.executeQuery();
            double popularity = 0.0;
            while (popularitySet.next()) {
              popularity = popularitySet.getDouble("popularity");
              break;
            }
            organizationsScore += popularity * 1.0/clusterOrganizations.size() * (double)occurence/articleOrganizationsList.size();
          }
          organizationsScore /= clusterOrganizations.size();
          
          double personsScore = 0.0;
          for (String clusterPerson : clusterPersons) {
            int occurence = 0;
            for (List articlePersons : articlePersonsList) {
              if (articlePersons.contains(clusterPerson)) {
                occurence++;
              }
            }
            ///get score of location
            getPersonsScore.setString(1, clusterPerson);
            ResultSet popularitySet = getPersonsScore.executeQuery();
            double popularity = 0.0;
            while (popularitySet.next()) {
              popularity = popularitySet.getDouble("popularity");
              break;
            }
            personsScore += popularity * 1.0/clusterPersons.size() * (double)occurence/articlePersonsList.size();
          }
          personsScore /= clusterPersons.size();
          
          final double score = (locationsScore*clusterLocations.size() + organizationsScore*clusterOrganizations.size() + personsScore*clusterPersons.size())/(clusterLocations.size()+clusterOrganizations.size()+clusterPersons.size());
          System.out.println(score);
          insertCluster.setLong (1, timestamp);
          insertCluster.setArray(2, articlesArray);
          
          final Array clusterLocationsArray = DatabaseUtils.getConnection().createArrayOf("text", clusterLocations.toArray());
          insertCluster.setArray(3, clusterLocationsArray);
          
          final Array clusterOrganizationsArray = DatabaseUtils.getConnection().createArrayOf("text", clusterOrganizations.toArray());
          insertCluster.setArray(4, clusterOrganizationsArray);
          
          final Array clusterPersonsArray = DatabaseUtils.getConnection().createArrayOf("text", clusterOrganizations.toArray());
          insertCluster.setArray(5, clusterPersonsArray);
          
          insertCluster.setDouble(6, score);
          
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