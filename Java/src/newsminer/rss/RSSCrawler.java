package newsminer.rss;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Observable;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.runner.manipulation.Sortable;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.LatLng;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.sun.syndication.io.FeedException;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.Triple;
import newsminer.json.JSONArray;
import newsminer.json.JSONObject;
import newsminer.json.JSONParser;
import newsminer.util.DatabaseUtils;

/***
 * Crawls RSS feeds and stores their articles in the database.
 * The URLs to the feeds are taken from the database.
 * Notifies the observers once all feeds have been crawled.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-08-02
 */
public class RSSCrawler extends Observable implements Runnable {
  //constants
  /** the interval at which the feeds are crawled */
  private static final long   TIMESTEP              = TimeUnit.HOURS.toMillis(1);
  /** the developer key used for Google API requests */
  private static final String GOOGLE_API_KEY;
  static {
    try (final InputStream in = new FileInputStream("conf/google_api.properties")) {
      final Properties properties = new Properties();
      properties.load(in);
      GOOGLE_API_KEY = properties.getProperty("key");
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  
  //attributes
  /** the classifier to use */
  private final AbstractSequenceClassifier<CoreLabel> classifier;
  
  //variables
  /** the time at which the next flush occurs */
  private long flushTimestamp;
  
  /**
   * Constructs a new instance of this class.
   * @throws IOException when the classifier could not be instantiated
   */
  public RSSCrawler() throws IOException {
    try {
      final Properties properties = new Properties();
      try (final InputStream in = new FileInputStream("conf/classifier.properties")) {
        properties.load(in);
      }
      classifier = CRFClassifier.getClassifier(properties.getProperty("loadPath"));
    } catch (ClassCastException cce) {
      throw new IOException(cce);
    } catch (ClassNotFoundException cnfe) {
      throw new IOException (cnfe);
    }
  }
  
  /**
   * Crawls the RSS feeds every time-step.
   * @see crawl()
   */
  @Override
  public void run() {
    flushTimestamp = System.currentTimeMillis();
    while (!Thread.currentThread().isInterrupted()) {
      //Start flushing.
      System.out.println("Crawling RSS feeds.");
      final long startTimestamp = System.currentTimeMillis();
      
      //Crawl the RSS feeds.
      try {
        crawl();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      
      //Finish flushing.
      final long finishTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimestamp);
      System.out.printf("Finished crawling RSS feeds in %s second%s.\n",
          finishTimeSeconds, finishTimeSeconds == 1 ? "" : "s");
      
      //Notify the observers.
      setChanged();
      notifyObservers();
      
      //Sleep until the next time-step.
      flushTimestamp += TIMESTEP;
      try {
        Thread.sleep(Math.max(0, flushTimestamp - System.currentTimeMillis()));
      } catch (InterruptedException ie) {
        break;
      }
    }
  }
  
  /**
   * Retrieves the feed URLs, accesses their streams, and stores the data.
   * @throws IOException
   */
  public void crawl() throws IOException {
    try (final PreparedStatement ps = DatabaseUtils.getConnection().prepareStatement(
        "SELECT source_url FROM rss_feeds")) {
      //Retrieve the feed URLs.
      final ResultSet rs = ps.executeQuery();
      
      //Crawl the feeds.
      while (rs.next()) {
        final String source_url = rs.getString("source_url");
        read(source_url);
      }
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    } catch (java.lang.NullPointerException nulp) {
      System.err.println("Could not read rss feed");
    }
  }
  
  /**
   * Reads the feed at the given URL.
   * @param  sourceURL URL to the feed 
   * @throws IOException when any of the articles could not be loaded or stored
   */
  public void read(String sourceURL) throws IOException {
    System.out.println(sourceURL);
    //Check the URL.
    final URL feedURL;
    try {
      feedURL = new URL(sourceURL);
    } catch (MalformedURLException murle) {
      throw new IOException(murle);
    }
    
    //Read the feed.
    final SyndFeed feed;
    try {
      feed = new HttpURLFeedFetcher(HashMapFeedInfoCache.getInstance()).retrieveFeed(feedURL);
    } catch (FeedException fe) {
      throw new IOException(fe);
    } catch (FetcherException fe) {
      throw new IOException(fe);
    }
    final long beginTimestamp = System.currentTimeMillis();
    //Read the articles.
    try (
        
        final PreparedStatement insertArticle = DatabaseUtils.getConnection().prepareStatement(
        "INSERT INTO rss_articles VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      for (Object articleObject : feed.getEntries()) {
        //Get the article.
        final SyndEntry article = (SyndEntry) articleObject;
        
        //Get the relevant variables.
        final String link        = article.getLink();
        final String source_url  = sourceURL;
        final long   timestamp   = article.getPublishedDate().getTime();
        final String title       = article.getTitle();
        final String description = article.getDescription().getValue();
        
        //Get the full text.
        final URL linkURL;
        try {
          linkURL = new URL(link);
        } catch (MalformedURLException murle) {
          System.err.printf("Invalid article URL received from feed with the URL %s (%s).\n",
              feedURL, murle.getMessage());
          continue;
        }
        final String text;
        try {
          text = ArticleExtractor.getInstance().getText(linkURL);
        } catch (BoilerpipeProcessingException bpe) {
          throw new IOException(bpe);
        }
        
        //Extract the entities.
        final Map<String, Set<String>>               namedEntityTypes = new TreeMap<>();
        final List<Triple<String, Integer, Integer>> characterOffsets = classifier.classifyToCharacterOffsets(text);
        for (Triple<String, Integer, Integer> characterOffset : characterOffsets) {
          final String type        = characterOffset.first.toLowerCase();
          final String namedEntity = text.substring(characterOffset.second, characterOffset.third);
          Set<String> namedEntities = namedEntityTypes.get(type);
          if (namedEntities == null) {
            namedEntities = new LinkedHashSet<>();
          }
          namedEntities.add(namedEntity);
          namedEntityTypes.put(type, namedEntities);
        }
        
        //Store the entities.
        final Map<String, Set<String>> normalizedNamedEntityTypes = new TreeMap<>();
        final HttpRequestFactory       httpRequestFactory         = new NetHttpTransport().createRequestFactory();
        final GenericUrl               searchURL                  = new GenericUrl("https://www.googleapis.com/freebase/v1/search");
        for (Entry<String, Set<String>> namedEntityType : namedEntityTypes.entrySet()) {
          final String type = namedEntityType.getKey();
          PreparedStatement insertEntity = null;
          try {
            switch (type) {
              case "location":
                insertEntity = DatabaseUtils.getConnection().prepareStatement(
                    "INSERT INTO entity_locations VALUES (?, ?, ?, ?, ?)");
                break;
              case "organization":
                insertEntity = DatabaseUtils.getConnection().prepareStatement(
                    "INSERT INTO entity_organizations VALUES (?, ?, ?)");
                break;
              case "person":
                insertEntity = DatabaseUtils.getConnection().prepareStatement(
                    "INSERT INTO entity_persons VALUES (?, ?, ?, ?, ?, ?, ?)");
                break;
              default:
                continue;
            }
            final Set<String> namedEntities           = namedEntityType.getValue();
            final Set<String> normalizedNamedEntities = new LinkedHashSet<>();
            for (String name : namedEntities) {
              //Search Freebase.
              System.out.println(name);
              searchURL.put("key",    GOOGLE_API_KEY);
              searchURL.put("query",  name);
              searchURL.put("filter", "(all type:" + type + ")");
              searchURL.put("limit",  1);
              final HttpResponse searchHTTPResponse;
              try {
                searchHTTPResponse = httpRequestFactory.buildGetRequest(searchURL).execute();
              } catch (IOException ioe) { //occasional timeout
                System.err.printf("Failed getting HTTP response for %s: %s\n",
                    searchURL, ioe.getMessage());
                continue;
              }
              final JSONObject searchResponseObject;
              try {
                searchResponseObject = (JSONObject) JSONParser.parse(searchHTTPResponse.parseAsString());
              } catch (ParseException pe) {
                throw new IOException(pe);
              }
              final JSONObject searchResultObject;
              try {
                searchResultObject = searchResponseObject
                    .get("result", JSONArray.class)
                    .get(0,        JSONObject.class);
              } catch (IndexOutOfBoundsException ioobe) { //no matching result found
                continue;
              }
              //System.out.println(searchResultObject.toString(2)); //TODO
              final String topicID    = searchResultObject.get("mid",   String.class);
                           name       = searchResultObject.get("name",  String.class);
              final double popularity = searchResultObject.get("score", Double.class);
              if (name.isEmpty()) {
                continue;
              }
              if (!normalizedNamedEntities.add(name)) { //already checked
                continue;
              }
              
              //Update the database if necessary.
              try (final PreparedStatement selectEntity = DatabaseUtils.getConnection().prepareStatement(
                  "SELECT * FROM entity_" + type + "s WHERE name = ?")) {
                selectEntity.setString(1, name);
                final ResultSet rs = selectEntity.executeQuery();
                if (!rs.next()) { //entity not yet in the database
                  //Check Freebase.
                  final GenericUrl topicURL = new GenericUrl("https://www.googleapis.com/freebase/v1/topic" + topicID);
                  topicURL.put("key",    GOOGLE_API_KEY);
                  topicURL.put("filter", "suggest");
                  final HttpResponse topicHTTPResponse;
                  try {
                    topicHTTPResponse = httpRequestFactory.buildGetRequest(topicURL).execute();
                  } catch (IOException ioe) { //occasional timeout
                    System.err.printf("Failed getting HTTP response for %s: %s\n",
                        topicURL, ioe.getMessage());
                    continue;
                  }
                  final JSONObject topicResponseObject;
                  try {
                    topicResponseObject = (JSONObject) JSONParser.parse(topicHTTPResponse.parseAsString());
                  } catch (ParseException pe) {
                    throw new IOException(pe);
                  }
                  final JSONObject topicResultObject = topicResponseObject.get("property", JSONObject.class);
                  String entityDescription;
                  try {
                    entityDescription = topicResultObject
                        .get("/common/topic/article", JSONObject.class)
                        .get("values",                JSONArray.class)
                        .get(0,                       JSONObject.class)
                        .get("property",              JSONObject.class)
                        .get("/common/document/text", JSONObject.class)
                        .get("values",                JSONArray.class)
                        .get(0,                       JSONObject.class)
                        .get("value",                 String.class);
                  } catch (MissingResourceException mre) {
                    entityDescription = null;
                  } catch (IndexOutOfBoundsException ioobe) {
                    entityDescription = null;
                  }
                  
                  //Store the result.
                  switch (type) {
                    case "location":
                      final LatLng geoCoordinates = getGeoCoordinates(name);
                      if (geoCoordinates != null) {
                        insertEntity.setString(1, name);
                        insertEntity.setString(2, entityDescription);
                        insertEntity.setDouble(3, popularity);
                        insertEntity.setDouble(4, geoCoordinates.getLat().doubleValue());
                        insertEntity.setDouble(5, geoCoordinates.getLng().doubleValue());
                        insertEntity.addBatch();
                      }
                      break;
                    case "organization":
                      insertEntity.setString(1, name);
                      insertEntity.setString(2, entityDescription);
                      insertEntity.setDouble(3, popularity);
                      insertEntity.addBatch();
                      break;
                    case "person":
                      String image;
                      try {
                        image = topicResultObject
                            .get("/common/topic/image", JSONObject.class)
                            .get("values",              JSONArray.class)
                            .get(0,                     JSONObject.class)
                            .get("id",                  String.class);
                      } catch (MissingResourceException mre) {
                        image = null;
                      } catch (IndexOutOfBoundsException ioobe) {
                        image = null;
                      }
                      String notable_for;
                      try {
                        notable_for = topicResultObject
                            .get("/common/topic/notable_for", JSONObject.class)
                            .get("values",                    JSONArray.class)
                            .get(0,                           JSONObject.class)
                            .get("text",                      String.class);
                      } catch (MissingResourceException mre) {
                        notable_for = null;
                      } catch (IndexOutOfBoundsException ioobe) {
                        notable_for = null;
                      }
                      String date_of_birth;
                      try {
                        date_of_birth = topicResultObject
                            .get("/people/person/date_of_birth", JSONObject.class)
                            .get("values",                       JSONArray.class)
                            .get(0,                              JSONObject.class)
                            .get("text",                         String.class);
                      } catch (MissingResourceException mre) {
                        date_of_birth = null;
                      } catch (IndexOutOfBoundsException ioobe) {
                        date_of_birth = null;
                      }
                      String place_of_birth;
                      try {
                        place_of_birth = topicResultObject
                            .get("/people/person/place_of_birth", JSONObject.class)
                            .get("values",                        JSONArray.class)
                            .get(0,                               JSONObject.class)
                            .get("text",                          String.class);
                      } catch (MissingResourceException mre) {
                        place_of_birth = null;
                      } catch (IndexOutOfBoundsException ioobe) {
                        place_of_birth = null;
                      }
                      insertEntity.setString(1, name);
                      insertEntity.setString(2, entityDescription);
                      insertEntity.setDouble(3, popularity);
                      insertEntity.setString(4, image);
                      insertEntity.setString(5, notable_for);
                      insertEntity.setString(6, date_of_birth);
                      insertEntity.setString(7, place_of_birth);
                      insertEntity.addBatch();
                      break;
                  }
                }
              }
            }
            insertEntity.executeBatch();
            normalizedNamedEntityTypes.put(type, normalizedNamedEntities);
          } finally {
            if (insertEntity != null) {
              insertEntity.close();
            }
          }
        }
        
        //Update the database table.
        insertArticle.setString(1, link);
        insertArticle.setString(2, source_url);
        insertArticle.setLong  (3, beginTimestamp);
        insertArticle.setString(4, title);
        insertArticle.setString(5, description);
        insertArticle.setString(6, text);
        for (Entry<String, Set<String>> normalizedNamedEntityType : normalizedNamedEntityTypes.entrySet()) {
          final String      type               = normalizedNamedEntityType.getKey();
          final Set<String> namedEntities      = normalizedNamedEntityType.getValue();
          final Array       namedEntitiesArray = DatabaseUtils.getConnection().createArrayOf("text", namedEntities.toArray());
          int i = -1;
          switch (type) {
            case "location":
              i = 7;
              break;
            case "organization":
              i = 8;
              break;
            case "person":
              i = 9;
              break;
          }
          insertArticle.setArray(i, namedEntitiesArray);
        }
        insertArticle.addBatch();
      }
      try {
        insertArticle.executeBatch();
      } catch (SQLException sqle) {
        SQLException next;
        while ((next = sqle.getNextException()) != null) {
          sqle = next;
          final String sqlState = sqle.getSQLState();
          if (sqlState.equals("23505")) { //article exists already
            continue;
          } else {
            throw sqle;
          }
        }
      }
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
  }
  
  /**
   * Returns the geographical coordinates for the given location.
   * @param  location location as String, e.g. Braunschweig, Entenhausen, etc.
   * @return latitude and longitude; null if no result was found
   */
  private static LatLng getGeoCoordinates(String location) {
    final Geocoder             geocoder         = new Geocoder();
    final GeocoderRequest      geocoderRequest  = new GeocoderRequestBuilder().setAddress(location).getGeocoderRequest();
    final GeocodeResponse      geocoderResponse = geocoder.geocode(geocoderRequest);
    final List<GeocoderResult> results          = geocoderResponse.getResults();
    return results.isEmpty() ? null : results.get(0).getGeometry().getLocation();
  }
}