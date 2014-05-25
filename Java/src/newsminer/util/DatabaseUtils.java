package newsminer.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Interacts with the database.
 * 
 * @author  Timo Guenther
 * @version 2014-05-25
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
    //Establish a connection if it is not already established.
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
  public synchronized static Connection getConnection(String sshUser, String sshPass) throws SQLException {
    //Establish the connection if it is not already established.
    if (connection == null) {
      //Check for the PostgreSQL JDBC driver.
      try {
        Class.forName("org.postgresql.Driver");
      } catch (ClassNotFoundException cnfe) { //not installed
        throw new SQLException(cnfe);
      }
      
      //Get the database connection properties.
      final Properties properties;
      try {
        properties = FileUtils.getProperties(CONNECTION_PROPERTIES_FILE_PATH);
      } catch (IOException ioe) {
        throw new SQLException(ioe);
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
      } catch (IOException ioe) {
        throw new SQLException(ioe);
      } catch (JSchException jsche) {
        throw new SQLException(jsche);
      }
      */
      
      //Establish the database connection.
      final String url = "jdbc:postgresql://localhost:" + properties.getProperty("port") + "/" + properties.getProperty("name");
      connection = DriverManager.getConnection(url, properties);
    }
    
    //Return the connection.
    return connection;
  }
}