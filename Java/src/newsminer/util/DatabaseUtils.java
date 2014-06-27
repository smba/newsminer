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
 * @version 2014-06-27
 */
public abstract class DatabaseUtils {
  //constants
  /** path to the properties file for connecting to the database */
  private static final String CONNECTION_PROPERTIES_FILE_PATH = "conf/database_connection.properties";
  /** path to the properties file for connecting to the SSH tunnel */
  //TODO private static final String SSH_PROPERTIES_FILE_PATH        = "conf/ssh.properties";
  /** default SSH port */
  //TODO private static final int    SSH_PORT                        = 22;
  
  //attributes
  /** connection to the database */
  private static Connection connection;

  /**
   * Returns the established database connection.
   * @return the established database connection
   */
  public synchronized static Connection getConnection() {
    //Check if the connection has been established yet.
    if (connection == null) {
      throw new IllegalStateException();
    }
    
    //Return the connection.
    return connection;
  }
  
  /**
   * Returns the established database connection using the given SSH credentials.
   * @param  sshUser user name for the SSH tunnel
   * @param  sshPass password for the SSH tunnel
   * @return the established database connection
   * @throws SQLException
   */
  public synchronized static Connection getConnection(String sshUser, String sshPass) throws IOException {
    //Establish the connection if it is not already established.
    if (connection == null) {
      //Check for the PostgreSQL JDBC driver.
      try {
        Class.forName("org.postgresql.Driver");
      } catch (ClassNotFoundException cnfe) { //not installed
        throw new IOException(cnfe);
      }
      
      //Get the database connection properties.
      final Properties properties = new Properties();
      try (final InputStream in = new FileInputStream(CONNECTION_PROPERTIES_FILE_PATH)) {
        properties.load(in);
      }
      
      //Access the SSH tunnel.
      /** TODO fix
      try {
        final Session session = new JSch().getSession(
            properties.getProperty("user"),
            properties.getProperty("host"),
            SSH_PORT);
        session.setPassword(properties.getProperty("password"));
        session.setConfig(FileUtils.getProperties(SSH_PROPERTIES_FILE_PATH));
        session.connect();
      } catch (JSchException jsche) {
        throw new IOException(jsche);
      }
      */
      
      //Establish the database connection.
      final String url = String.format("jdbc:postgresql://localhost:%s/%s",
          properties.getProperty("port"), properties.getProperty("name"));
      try {
        connection = DriverManager.getConnection(url, properties);
      } catch (SQLException ioe) {
        throw new IOException(ioe);
      }
    }
    
    //Return the connection.
    return connection;
  }
}