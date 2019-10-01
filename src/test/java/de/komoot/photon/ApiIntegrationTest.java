package de.komoot.photon;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static spark.Spark.*;

/**
 * These test connect photon to an already running ES node (setup in ESBaseTester) so that we can directly test the API
 */
public class ApiIntegrationTest extends ESBaseTester {
    private static final int LISTEN_PORT = 30234;

    @After
    public void shutdown() {
        stop();
        awaitStop();
        deleteIndex();
    }

    /**
     * Test that the Access-Control-Allow-Origin header is not set
     */
    @Test
    public void testNoCors() throws Exception {
        App.main(new String[]{"-cluster", clusterName, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
        assertNull(connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to *
     */
    @Test
    public void testCorsAny() throws Exception {
        App.main(new String[]{"-cluster", clusterName, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                "-cors-any"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
        assertEquals("*", connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to a specific domain
     */
    @Test
    public void testCorsOriginIsSetToSpecificDomain() throws Exception {
        App.main(new String[]{"-cluster", clusterName, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1",
                "-cors-origin", "www.poole.ch"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin").openConnection();
        assertEquals("www.poole.ch", connection.getHeaderField("Access-Control-Allow-Origin"));
    }

    @Test
    public void testSearchForBerlin() throws Exception {
        App.main(new String[]{"-cluster", clusterName, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1").openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("way", properties.getString("osm_type"));
        assertEquals("tourism", properties.getString("osm_key"));
        assertEquals("attraction", properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }

    /**
     * Search with location bias (this should give the last generated object which is roughly 2km away from the first)
     */
    @Test
    public void testApiWithLocationBias() throws Exception {
        App.main(new String[]{"-cluster", clusterName, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/api?q=berlin&limit=1&lat=52.54714&lon=13.39026")
                .openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("way", properties.getString("osm_type"));
        assertEquals("railway", properties.getString("osm_key"));
        assertEquals("station", properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }

    /**
     * Reverse geocode test
     */
    @Test
    public void testApiReverse() throws Exception {
        App.main(new String[]{"-cluster", clusterName, "-listen-port", Integer.toString(LISTEN_PORT), "-transport-addresses", "127.0.0.1"});
        awaitInitialization();
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port() + "/reverse/?lon=13.38886&lat=52.51704").openConnection();
        JSONObject json = new JSONObject(
                new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining("\n")));
        JSONArray features = json.getJSONArray("features");
        assertEquals(1, features.length());
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("way", properties.getString("osm_type"));
        assertEquals("tourism", properties.getString("osm_key"));
        assertEquals("attraction", properties.getString("osm_value"));
        assertEquals("berlin", properties.getString("name"));
    }
}
