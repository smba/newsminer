package newsminer.rss;

/**
 * @author Stefan Muehlbauer
 * @author Timo Guenther
 *
 */
public class ArticleClusterer {
  private static final ArticleClusterer instance = new ArticleClusterer();
  private ArticleClusterer() {
    //
  }
  public static ArticleClusterer getInstance() {
    return instance;
  }
  
  public void loadArticles() {
    /*
     * retrieving from the database TODO
     */
  }
  
  private static void calculateSimilarities() {
    
  }
  
  public static void calculateClusters() {
    
  }
}
