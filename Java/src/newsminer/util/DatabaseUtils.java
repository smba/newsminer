package newsminer.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Interacts with the database.
 * 
 * @author  Timo Guenther
 * @version 2014-07-01
 */
public abstract class DatabaseUtils {
  //constants
  /** path to the properties file for connecting to the database */
  private static final String CONNECTION_PROPERTIES_FILE_PATH = "conf/database_connection.properties";
  
  //attributes
  /** connection to the database */
  private static Connection connection;

  /**
   * Returns the established database connection.
   * @return the established database connection
   */
  public synchronized static Connection getConnection() {
    //Establish the connection if necessary.
    if (connection == null) {
      //Check for the PostgreSQL JDBC driver.
      try {
        Class.forName("org.postgresql.Driver");
      } catch (ClassNotFoundException cnfe) { //not installed
        throw new RuntimeException(cnfe);
      }
      
      //Get the database connection properties.
      final Properties properties = new Properties();
      try (final InputStream in = new FileInputStream(CONNECTION_PROPERTIES_FILE_PATH)) {
        properties.load(in);
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      
      //Establish the database connection.
      final String url = String.format("jdbc:postgresql://%s:%s/%s",
          properties.getProperty("host"),
          properties.getProperty("port"),
          properties.getProperty("name"));
      try {
        connection = DriverManager.getConnection(url, properties);
      } catch (SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }
    
    //Return the connection.
    return connection;
  }
}