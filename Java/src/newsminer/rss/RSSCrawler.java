package newsminer.rss;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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
import newsminer.util.DatabaseUtils;

/***
 * Crawls RSS feeds and stores their articles in the database.
 * The URLs to the feeds are taken from the database.
 * Notifies the observers once all feeds have been crawled.
 * 
 * @author  Stefan Muehlbauer
 * @author  Timo Guenther
 * @version 2014-06-15
 */
public class RSSCrawler extends Observable implements Runnable {
  //constants
  /** the interval at which the feeds are crawled */
  private static final long   TIMESTEP              = TimeUnit.HOURS.toMillis(1);
  /** the path to the classifier */
  private static final String SERIALIZED_CLASSIFIER = "classifiers/english.all.3class.distsim.crf.ser.gz";
  
  //PreparedStatement attributes
  /** statement for inserting RSS articles */
  private final PreparedStatement insertArticle;
  
  //attributes
  /** the classifier to use */
  private final AbstractSequenceClassifier<CoreLabel> classifier;
  
  //variables
  /** the time at which the next flush occurs */
  private long flushTimestamp;
  
  /**
   * Constructs a new instance of this class.
   * @throws IOException 
   */
  public RSSCrawler() throws IOException {
    //Prepare the statements.
    try {
      insertArticle = DatabaseUtils.getConnection().prepareStatement(
          "INSERT INTO rss_articles VALUES (?, ?, ?, ?, ?, ?)");
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
    
    //Get the classifier.
    try {
      classifier = CRFClassifier.getClassifier(SERIALIZED_CLASSIFIER);
    } catch (ClassCastException cce) {
      throw new IOException(cce);
    } catch (ClassNotFoundException cnfe) {
      throw new IOException (cnfe);
    }
  }
  
  /**
   * Crawls the RSS feeds every time-step.
   * @see flush()
   */
  @Override
  public void run() {
    flushTimestamp = System.currentTimeMillis();
    while (!Thread.currentThread().isInterrupted()) {
      //Crawl the RSS feeds.
      flush();
      
      //Sleep until the next time-step.
      flushTimestamp += TIMESTEP;
      try {
        Thread.sleep(Math.max(0, flushTimestamp - System.currentTimeMillis()));
      } catch (InterruptedException e) {
        break;
      }
    }
  }
  
  /**
   * Retrieves the feed URLs, accesses their streams, and stores the data.
   */
  private synchronized void flush() {
    //Start flushing.
    System.out.println("Crawling RSS feeds.");
    final long startTimestamp = System.currentTimeMillis();
    
    //Retrieve the feed URLs and crawl every one of them.
    try (final PreparedStatement ps = DatabaseUtils.getConnection().prepareStatement(
        "SELECT source_url FROM rss_feeds")) { 
      final ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        //Get the current feed URL.
        final String source_url;
        try {
          source_url = rs.getString("source_url");
        } catch (SQLException sqle) {
          sqle.printStackTrace();
          continue;
        }
        
        //Read the feed.
        read(source_url);
      }
      
      //Notify the observers.
      notifyObservers();
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
    
    //Finish flushing.
    final long finishTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimestamp);
    System.out.println("Finished crawling RSS feeds in " + finishTimeSeconds + " second" + (finishTimeSeconds == 1 ? "" : "s") + ".");
  }
  
  /**
   * Reads the feed at the given URL.
   * @param sourceURL URL to the feed 
   */
  protected void read(String sourceURL) {
    //Check the URL.
    final URL feedURL;
    try {
      feedURL = new URL(sourceURL);
    } catch (MalformedURLException murle) {
      murle.printStackTrace();
      return;
    }
    
    //Read the feed.
    final SyndFeed feed;
    try {
      feed = new HttpURLFeedFetcher(HashMapFeedInfoCache.getInstance()).retrieveFeed(feedURL);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    } catch (FeedException fe) {
      fe.printStackTrace();
      return;
    } catch (FetcherException fe) {
      throw new RuntimeException(fe);
    }
    
    //Read the articles.
    for (Object articleObject : feed.getEntries()) {
      //Get the article.
      if (!(articleObject instanceof SyndEntry)) {
        continue;
      }
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
        murle.printStackTrace();
        continue;
      }
      final String text;
      try {
        text = ArticleExtractor.getInstance().getText(linkURL);
      } catch (BoilerpipeProcessingException bpe) {
        bpe.printStackTrace();
        continue;
      }
      
      //Extract the entities.
      final Map<String, Set<String>>               namedEntityTypes = new TreeMap<>();
      final List<Triple<String, Integer, Integer>> characterOffsets = classifier.classifyToCharacterOffsets(text);
      for (Triple<String, Integer, Integer> characterOffset : characterOffsets) {
        final String type        = characterOffset.first;
        final String namedEntity = text.substring(characterOffset.second, characterOffset.third);
        Set<String> namedEntities = namedEntityTypes.get(type);
        if (namedEntities == null) {
          namedEntities = new LinkedHashSet<>();
        }
        namedEntities.add(namedEntity);
        namedEntityTypes.put(type, namedEntities);
      }
      
      //Store the entities.
      for (Entry<String, Set<String>> namedEntityType : namedEntityTypes.entrySet()) {
        final String      type          = namedEntityType.getKey();
        final Set<String> namedEntities = namedEntityType.getValue();
        for (String namedEntity : namedEntities) {
          //TODO
          System.out.println(type + ": " + namedEntity);
        }
      }
      
      //Update the database table.
      try {
        insertArticle.setString(1, link);
        insertArticle.setString(2, source_url);
        insertArticle.setLong  (3, timestamp);
        insertArticle.setString(4, title);
        insertArticle.setString(5, description);
        insertArticle.setString(6, text);
        insertArticle.addBatch();
      } catch (SQLException sqle) {
        System.err.println(sqle.getMessage());
      }
    }
    try {
      insertArticle.executeBatch();
    } catch (SQLException sqle) {
      System.err.println(sqle.getMessage());
    }
  }
}