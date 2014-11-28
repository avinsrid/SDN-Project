//package project.sdn.app;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONObject;
//import javax.json.JsonObject;
//import org.opendaylight.controller.switchmanager.Switch;
public class PortDiscovery {

	public static JSONObject getTopology(String user, String password, String baseURL) {

    	StringBuffer result = new StringBuffer();
    	try {

        	if (!baseURL.contains("http")) {
        	    baseURL = "http://" + baseURL;
        	}

        	// Create URL = base URL 
        	URL url = new URL(baseURL);

        	// Create authentication string and encode it to Base64
        	String authStr = user + ":" + password;
        	String encodedAuthStr = Base64.encodeBase64String(authStr
                .getBytes());

        	// Create Http connection
        	HttpURLConnection connection = (HttpURLConnection) url
                .openConnection();

        	// Set connection properties
        	connection.setRequestMethod("GET");
        	connection.setRequestProperty("Authorization", "Basic " + encodedAuthStr);
        	connection.setRequestProperty("Accept", "application/json");

        	// Get the response from connection's inputStream
        	InputStream content = (InputStream) connection.getInputStream();
        	BufferedReader in = new BufferedReader(new InputStreamReader(content));
        	String line = "";
        	while ((line = in.readLine()) != null) {
            	result.append(line);
        	}

        	JSONObject topology = new JSONObject(result.toString());
        	return topology;
    	} 
    	catch (Exception e) {
        e.printStackTrace();
    	}

    return null;
	}

	// This is to test the REST API pull from parent controller, along with JSON parsing
	public static void main(String args[]) {

		// Call the getTopology function with username password and URL	
		JSONObject topology = getTopology("admin","admin","http://localhost:8080/controller/nb/v2//topology/default");
	}
}
