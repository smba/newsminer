package newsminer.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Provides methods for handling files.
 * 
 * @author  Timo Guenther
 * @version 2014-04-11
 */
public abstract class FileUtils {
  /**
   * Returns a properties file.
   * @param  path path to the file
   * @return a properties file
   * @throws IOException
   */
  public static Properties getProperties(String path) throws IOException {
    try (FileInputStream fis = new FileInputStream(path)) {
      final Properties properties = new Properties();
      properties.load(fis);
      return properties;
    }
  }
}