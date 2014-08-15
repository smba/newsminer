package newsminer;

import java.io.IOException;

import newsminer.rss.ArticleClusterer;
import newsminer.rss.RSSCrawler;
import newsminer.util.DatabaseUtils;

/**
 * Coordinates the News Miner components.
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 */
public abstract class NewsMiner {
  /**
   * Launches the application.
   * @param args unused
   */
  public static void main(String[] args) {
    //Initialize the components.
    System.out.println("Initializing.");
    DatabaseUtils.getConnectionPool();
    final RSSCrawler       rssCrawler;
    final ArticleClusterer articleClusterer;
    try {
      rssCrawler       = new RSSCrawler();
      articleClusterer = new ArticleClusterer();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    
    //Add observers.
    rssCrawler.addObserver(articleClusterer);
    
    //Start the components.
    System.out.println("Starting.");
    new Thread(rssCrawler, RSSCrawler.class.getSimpleName()).start();
  }
}