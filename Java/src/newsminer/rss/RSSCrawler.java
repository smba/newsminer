package newsminer.rss;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.LinkedHashMap;
import java.util.Observable;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.TypeConstraintException;

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
import newsminer.json.JSONObject;
import newsminer.json.JSONParser;
import newsminer.util.DatabaseUtils;

/***
 * Crawls RSS feeds and stores their articles in the database.
 * The URLs to the feeds are taken from the database.
 * Notifies the observers ({@link ArticleClusterer} once all feeds have been crawled.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-08-16
 */
public class RSSCrawler extends Observable implements Runnable {
  //constants
  /** the interval at which the feeds are crawled */
  private static final long   TIMESTEP       = TimeUnit.HOURS.toMillis(1);
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
  /** keeps track of all normalized names */
  private static final Map<String, Map<String, String>> normalizedNamesByTypeAndName = new LinkedHashMap<>();
  static {
    for (final String type : new String[] {"location", "organization", "person"}) {
      normalizedNamesByTypeAndName.put(type, new LinkedHashMap<String, String>());
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
    //Get the classifier.
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
    
    //Prevent the error message "Could not find fetcher.properties on classpath".
    boolean fetcherPropertiesExists = false;
    final String[] classpaths = System.getProperty("java.class.path").split(File.pathSeparator);
    for (String classpath : classpaths) {
      if (new File(classpath, "fetcher.properties").exists()) {
        fetcherPropertiesExists = true;
      }
    }
    if (!fetcherPropertiesExists) {
      new File(classpaths[0], "fetcher.properties").createNewFile();
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
    try (final Connection        con = DatabaseUtils.getConnectionPool().getConnection();
         final PreparedStatement ps  = con.prepareStatement(
             "SELECT source_url FROM rss_feeds")) {
      //Retrieve the feed URLs.
      final ResultSet         rs = ps.executeQuery();
      
      //Crawl each feed.
      while (rs.next()) {
        final String feedURL = rs.getString("source_url");
        final URL    url;
        try {
          url = new URL(feedURL);
        } catch (MalformedURLException murle) {
          throw new IOException(murle);
        }
        try {
          read(url);
        } catch (IOException ioe) {
          System.err.println(ioe.getMessage());
          continue;
        }
      }
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
  }
  
  /**
   * Reads the feed at the given URL.
   * @param  feedURL URL to the feed 
   * @throws IOException when any of the articles could not be loaded or stored
   */
  public void read(URL feedURL) throws IOException {
    //Get the feed.
    final SyndFeed feed      = getFeed(feedURL);
    final String   sourceURL = feedURL.toString();
    
    //Read the articles.
    try (final Connection        con           = DatabaseUtils.getConnectionPool().getConnection();
         final PreparedStatement insertArticle = con.prepareStatement(
             "INSERT INTO rss_articles VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      for (Object articleObject : feed.getEntries()) {
        //Get the article.
        final SyndEntry article = (SyndEntry) articleObject;
        
        //Get the relevant variables.
        final String link        = article.getLink();
        final String source_url  = sourceURL;
        final long   timestamp   = flushTimestamp;
        final String title       = article.getTitle();
        final String description = article.getDescription().getValue();
        final String text;
        try {
          final URL linkURL = new URL(link);
          text = getText(linkURL);
        } catch (MalformedURLException murle) {
          System.err.printf("Invalid article URL received from feed with the URL %s: %s\n",
              feedURL, murle.getMessage());
          continue;
        }
        
        //Get the entity names.
        final Map<String, Set<String>> entityNamesByType = getEntityNamesByType(text);
        
        //Get the entities.
        final Map<String, Set<String>> normalizedEntityNamesByType = new TreeMap<>();
        for (final Entry<String, Set<String>> entityNamesAndType : entityNamesByType.entrySet()) {
          final String      type                  = entityNamesAndType.getKey();
          final Set<String> entityNames           = entityNamesAndType.getValue();
          final Set<String> normalizedEntityNames = new LinkedHashSet<>();
          for (final String entityName : entityNames) {
            final String normalizedEntityName = getNormalizedEntityName(type, entityName);
            normalizedEntityNames.add(normalizedEntityName);
          }
        }
        
        //Update the database table.
        insertArticle.setString(1, link);
        insertArticle.setString(2, source_url);
        insertArticle.setLong  (3, timestamp);
        insertArticle.setString(4, title);
        insertArticle.setString(5, description);
        insertArticle.setString(6, text);
        for (Entry<String, Set<String>> normalizedEntityNamesAndType : normalizedEntityNamesByType.entrySet()) {
          final String      type               = normalizedEntityNamesAndType.getKey();
          final Set<String> namedEntities      = normalizedEntityNamesAndType.getValue();
          final Array       namedEntitiesArray = con.createArrayOf("text", namedEntities.toArray());
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
   * Returns the feed at the given URL.
   * @param  feedURL URL to the feed
   * @return the feed at the given URL
   * @throws IOException if the feed could not be loaded
   */
  private SyndFeed getFeed(URL feedURL) throws IOException {
    //Get the feed.
    final SyndFeed feed;
    try {
      feed = new HttpURLFeedFetcher(HashMapFeedInfoCache.getInstance()).retrieveFeed(feedURL);
    } catch (FeedException fe) {
      throw new IOException(fe);
    } catch (FetcherException fe) {
      throw new IOException(fe);
    }
    return feed;
  }
  
  /**
   * Returns the full text of the article at the given URL.
   * @param  articleURL URL to the article to check
   * @return the full text of the article at the given URL
   * @throws IOException when the text could not be loaded
   */
  private String getText(URL articleURL) throws IOException {
    final String text;
    try {
      text = ArticleExtractor.getInstance().getText(articleURL);
    } catch (BoilerpipeProcessingException bpe) {
      throw new IOException(bpe);
    }
    return text;
  }
  
  /**
   * Returns the names of entities (value) by their type (key).
   * @param  text text to extract named entities from
   * @return the names of entities (value) by their type (key)
   */
  private Map<String, Set<String>> getEntityNamesByType(String text) {
    final Map<String, Set<String>>               entityNamesByType = new TreeMap<>();
    final List<Triple<String, Integer, Integer>> characterOffsets  = classifier.classifyToCharacterOffsets(text);
    for (Triple<String, Integer, Integer> characterOffset : characterOffsets) {
      final String type       = characterOffset.first.toLowerCase();
      final String entityName = text.substring(characterOffset.second, characterOffset.third);
      Set<String> entityNames = entityNamesByType.get(type);
      if (entityNames == null) {
        entityNames = new LinkedHashSet<>();
      }
      entityNames.add(entityName);
      entityNamesByType.put(type, entityNames);
    }
    return entityNamesByType;
  }
  
  /**
   * Returns the normalized name or null if it could not be found.
   * Also looks up and stores entity information along the way.
   * @param  type type of the entity
   * @param  searchName name to use in the search for the entity
   * @return the normalized name or null if it could not be found
   * @throws IOException when interaction with the database failed
   * @throws TypeConstraintException if the type is invalid
   */
  public String getNormalizedEntityName(String type, String searchName) throws IOException, TypeConstraintException {
    //Check the type.
    final Map<String, String> normalizedNameByName = normalizedNamesByTypeAndName.get(type);
    if (normalizedNameByName == null) {
      throw new TypeConstraintException("Invalid entity type: " + type);
    }
    
    //Check if this name was already searched before.
    String normalizedName = normalizedNameByName.get(searchName);
    if (normalizedName != null) {
      return normalizedName;
    }
    
    //Check if the search name is already in the database.
    try (final Connection        con = DatabaseUtils.getConnectionPool().getConnection();
         final PreparedStatement ps  = con.prepareStatement(
             "SELECT CASE "
             + "WHEN EXISTS (SELECT * FROM entity_" + type + "s WHERE name = ?) "
             + "THEN TRUE ELSE FALSE "
             + "END")) {
      ps.setString(1, searchName);
      final ResultSet rs = ps.executeQuery();
      rs.next();
      if (rs.getBoolean(1)) { //already in the database
        return searchName;
      }
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
    
    //Normalize the name using a Freebase search.
    final HttpRequestFactory httpRequestFactory = new NetHttpTransport().createRequestFactory();
    final GenericUrl         searchURL          = new GenericUrl("https://www.googleapis.com/freebase/v1/search");
    searchURL.put("key",    GOOGLE_API_KEY);
    searchURL.put("query",  searchName);
    searchURL.put("filter", "(all type:" + type + ")");
    searchURL.put("limit",  1);
    final HttpResponse searchHTTPResponse = httpRequestFactory.buildGetRequest(searchURL).execute();
    final JSONObject   searchResponseObject;
    try {
      searchResponseObject = (JSONObject) JSONParser.parse(searchHTTPResponse.parseAsString());
    } catch (ParseException pe) {
      throw new IOException(pe);
    }
    final JSONObject searchResultObject = searchResponseObject.get(JSONObject.class, null,
          new Object[] {"result", 0});
    if (searchResultObject == null) { //no matching result found
      return null;
    }
    final String mid        = searchResultObject.get("mid",   String.class);
    final String name       = searchResultObject.get("name",  String.class);
    final double popularity = searchResultObject.get("score", Double.class);
    if (name.isEmpty()) {
      return null;
    }
    
    //Remember this search name and its normalized name.
    normalizedNameByName.put(searchName, name);
    
    //Check if the normalized name is already in the database.
    if (!searchName.equals(name)) {
      normalizedNameByName.put(name, name);
      try (final Connection        con = DatabaseUtils.getConnectionPool().getConnection();
           final PreparedStatement ps  = con.prepareStatement(
              "SELECT CASE "
              + "WHEN EXISTS (SELECT * FROM entity_" + type + "s WHERE name = ?) "
              + "THEN TRUE ELSE FALSE "
              + "END")) {
        ps.setString(1, name);
        final ResultSet rs = ps.executeQuery();
        rs.next();
        if (rs.getBoolean(1)) { //already in the database
          return name;
        }
      } catch (SQLException sqle) {
        throw new IOException(sqle);
      }
    }
    
    //Get entity information from Freebase.
    final GenericUrl topicURL = new GenericUrl("https://www.googleapis.com/freebase/v1/topic" + mid);
    topicURL.put("key",    GOOGLE_API_KEY);
    topicURL.put("filter", "suggest");
    final HttpResponse topicHTTPResponse = httpRequestFactory.buildGetRequest(topicURL).execute();
    final JSONObject topicResponseObject;
    try {
      topicResponseObject = (JSONObject) JSONParser.parse(topicHTTPResponse.parseAsString());
    } catch (ParseException pe) {
      throw new IOException(pe);
    }
    final JSONObject topicResultObject = topicResponseObject.get("property", JSONObject.class);
    final String entityDescription = topicResultObject.get(String.class, null,
          new Object[] {"/common/topic/article", "values", 0, "property", "/common/document/text", "values", 0, "value"});
    
    //Store the entity information in the database.
    try (final Connection        con = DatabaseUtils.getConnectionPool().getConnection()) {
      PreparedStatement ps = null;
      switch (type) {
        case "location":
          ps = con.prepareStatement("INSERT INTO entity_locations VALUES (?, ?, ?, ?, ?)");
          final LatLng geoCoordinates = getGeoCoordinates(name);
          if (geoCoordinates == null) {
            return null;
          }
          final double latitude  = geoCoordinates.getLat().doubleValue();
          final double longitude = geoCoordinates.getLng().doubleValue();
          ps.setString(1, name);
          ps.setString(2, entityDescription);
          ps.setDouble(3, popularity);
          ps.setDouble(4, latitude);
          ps.setDouble(5, longitude);
          ps.executeUpdate();
          break;
        case "organization":
          ps = con.prepareStatement("INSERT INTO entity_organizations VALUES (?, ?, ?)");
          ps.setString(1, name);
          ps.setString(2, entityDescription);
          ps.setDouble(3, popularity);
          ps.executeUpdate();
          break;
        case "person":
          ps = con.prepareStatement("INSERT INTO entity_persons VALUES (?, ?, ?, ?, ?, ?, ?)");
          final String image          = topicResultObject.get(String.class, null, new Object[] {"/common/topic/image",           "values", 0, "id"});
          final String notable_for    = topicResultObject.get(String.class, null, new Object[] {"/common/topic/notable_for",     "values", 0, "text"});
          final String date_of_birth  = topicResultObject.get(String.class, null, new Object[] {"/people/person/date_of_birth",  "values", 0, "text"});
          final String place_of_birth = topicResultObject.get(String.class, null, new Object[] {"/people/person/place_of_birth", "values", 0, "text"});
          ps.setString(1, name);
          ps.setString(2, entityDescription);
          ps.setDouble(3, popularity);
          ps.setString(4, image);
          ps.setString(5, notable_for);
          ps.setString(6, date_of_birth);
          ps.setString(7, place_of_birth);
          ps.executeUpdate();
          break;
      }
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
    
    //Return the normalized name.
    return name;
  }
  
  /**
   * Returns the geographical coordinates for the given location.
   * @param  location location as String, e.g. Braunschweig, Entenhausen, etc.
   * @return latitude and longitude; null if no result was found
   */
  private LatLng getGeoCoordinates(String location) {
    final Geocoder             geocoder         = new Geocoder();
    final GeocoderRequest      geocoderRequest  = new GeocoderRequestBuilder().setAddress(location).getGeocoderRequest();
    final GeocodeResponse      geocoderResponse = geocoder.geocode(geocoderRequest);
    final List<GeocoderResult> results          = geocoderResponse.getResults();
    return results.isEmpty() ? null : results.get(0).getGeometry().getLocation();
  }
}