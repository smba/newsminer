package newsminer;

import java.io.Console;
import java.io.IOException;
import java.sql.SQLException;

import newsminer.rss.RSSCrawler;
import newsminer.util.DatabaseUtils;

/**
 * Coordinates the News Miner components.
 * 
 * @author  Timo Guenther
 * @version 2014-05-25
 */
public abstract class NewsMiner {
  /**
   * Launches the application.
   * Will ask for SSH credentials if not already provided as arguments.
   * @param args 0: SSH user name (optional);
   *             1: SSH password (optional)
   */
  public static void main(String[] args) {
    //Get the SSH credentials.
    final String sshUser, sshPassword;
    if (args.length >= 2) {
      sshUser     = args[0];
      sshPassword = args[1];
    } else {
      final Console console = System.console();
      if (console == null) {
        throw new IllegalArgumentException("Missing SSH credentials");
      }
      System.out.println("SSH user name?");
      sshUser     = new String(console.readPassword());
      System.out.println("SSH password?");
      sshPassword = new String(console.readPassword());
    }
    
    //Connect to the database.
    try {
      DatabaseUtils.getConnection(sshUser, sshPassword);
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    }
    
    //Initialize the components.
    System.out.println("Initializing.");
    //TODO final TwitterCrawler    twitterCrawler;
    //TODO final TwitterAggregator twitterAggregator;
    final RSSCrawler        rssCrawler;
    //TODO final RSSFilter         rssFilter;
    try {
      //TODO twitterCrawler    = new TwitterCrawler();
      //TODO twitterAggregator = new TwitterAggregator();
      rssCrawler        = new RSSCrawler();
      //TODO rssFilter         = new RSSFilter();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    
    //Add observers.
    //TODO twitterCrawler.addObserver(twitterAggregator);
    //TODO twitterAggregator.addObserver(rssFilter);
    
    //Start the components.
    System.out.println("Starting.");
    //TODO new Thread(twitterAggregator, TwitterAggregator.class.getSimpleName()).start();
    //TODOnew Thread(twitterCrawler, TwitterCrawler.class.getSimpleName()).start();
    new Thread(rssCrawler, RSSCrawler.class.getSimpleName()).start();
  }
}