package newsminer.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;

/**
 * 
 * @author Stefan Muehlbauer
 * This class provides the geocooding methods.
 *
 */
public class GeoUtils {
  
  /**
   * @param loc location as String, e.g. Braunschweig, Entenhausen etc.
   * @return double-Vector with latitude and longitude values.
   */
  private static double[] geocodeCalc(String loc) {
    final Geocoder geocoder = new Geocoder();
    GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(loc).getGeocoderRequest();
    GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);
    List<GeocoderResult> results = geocoderResponse.getResults();
    float latitude = results.get(0).getGeometry().getLocation().getLat().floatValue();
    float longitude = results.get(0).getGeometry().getLocation().getLng().floatValue();
    return new double[]{latitude, longitude};
  }
  
  /**
   * Cached method to reduce API calls, @see geocodeCalc.
   * @param loc location as String, e.g. Braunschweig, Entenhausen etc.
   * @return double-Vector with latitude and longitude values.
   */
  public double[] geocode(String loc) throws SQLException {
    //Check if the location is known
    final PreparedStatement ps = DatabaseUtils.getConnection().prepareStatement(
        "SELECT * FROM locations WHERE name = ?");
    ps.setString(1, loc);
    final ResultSet locRS = ps.executeQuery();
    locRS.beforeFirst();
    if (locRS.next()) {
      return new double[]{locRS.getDouble("latitude"), locRS.getDouble("longitude")};
    } else {
      double[] latlong = geocodeCalc(loc);
      final PreparedStatement insertS = DatabaseUtils.getConnection().prepareStatement(
          "INSERT INTO locations VALUES (?, ?, ?)");
      insertS.setString(1, loc);
      insertS.setDouble(2, latlong[0]);
      insertS.setDouble(3, latlong[1]);
      insertS.executeUpdate();
      return latlong;
    }
  }
}
