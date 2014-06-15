package newsminer.rss;

import java.sql.SQLException;

import newsminer.util.DatabaseUtils;

public class ArticleClustererTester {
  public static void main(String[] args) throws SQLException {
    DatabaseUtils.getConnection("foo", "bar");
    ArticleClusterer a = new ArticleClusterer();
    a.cluster();
  }
}
