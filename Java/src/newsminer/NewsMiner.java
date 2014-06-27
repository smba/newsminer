package newsminer;

import java.io.Console;
import java.io.IOException;

import newsminer.rss.ArticleClusterer;
import newsminer.rss.RSSCrawler;
import newsminer.util.DatabaseUtils;

/**
 * Coordinates the News Miner components.
 * 
 * @author  Timo Guenther
 * @version 2014-06-27
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
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    
    //Initialize the components.
    System.out.println("Initializing.");
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