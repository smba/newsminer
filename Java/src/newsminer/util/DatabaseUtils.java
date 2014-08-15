package newsminer.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

/**
 * Provides utility methods for interacting with the database.
 * 
 * @author  Timo Guenther
 * @version 2014-08-15
 */
public abstract class DatabaseUtils {
  //constants
  /** path to the properties file for connecting to the database */
  private static final String CONNECTION_PROPERTIES_FILE_PATH = "conf/database_connection.properties";
  
  //attributes
  /** connection pool for the database */
  private static BoneCP connectionPool;
  
  /**
   * Returns the connection pool.
   * @return the connection pool
   */
  public synchronized static BoneCP getConnectionPool() {
    if (connectionPool == null) {
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
      final BoneCPConfig conf;
      try {
        conf = new BoneCPConfig(properties);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      
      //Create the connection pool.
      try {
        connectionPool = new BoneCP(conf);
      } catch (SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }
    return connectionPool;
  }
}