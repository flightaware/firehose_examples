import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SSL_Client_json_simple {

    // substitute your own username and password
    private static String initiation_command = "live username XXXXXXXX password XXXXXXXXXXXXXXXXXXXX";
    private static final boolean useCompression = false;
    private SSLSocket ssl_socket;

    public static void main(String[] args) {
        String machineName = "firehose.flightaware.com";
        RunClient(machineName);
        System.out.println(" Thank you for using FlightAware ... bye now");
    }

    private static void RunClient(String machineName) {
        System.out.println(" Running Client");
        try {
            SSLSocket ssl_socket;
            ssl_socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(machineName, 1501);
            // enable certifcate validation:
            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            sslParams.setProtocols(new String[] {"TLSv1.2"});
            ssl_socket.setSSLParameters(sslParams);

            if (useCompression) {
                initiation_command += " compression gzip";
            }

            initiation_command += "\n";
            
            //send your initiation command
            OutputStreamWriter writer = new OutputStreamWriter(ssl_socket.getOutputStream(), "UTF8");
            writer.write(initiation_command);
            writer.flush();

            InputStream inputStream = ssl_socket.getInputStream();
            if (useCompression) {
                inputStream = new java.util.zip.GZIPInputStream(inputStream);
            }

            // read messages from FlightAware
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String message = null;
            int limit = 5; //limit number messages for testing
            while (limit > 0 && (message = reader.readLine()) != null) {
                System.out.println("msg: " + message + "\n");
                parse_json(message);
                limit--;
            }

            //done, close everything
            writer.close();
            reader.close();
            inputStream.close();
            ssl_socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void parse_json(String message) {
        //using JSON.simple: Java toolkit for JSON
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;
        try {
            // parse message from json to JSONObject
            jsonObject = (JSONObject) jsonParser.parse(message);

            // retrieve values of interest associated with the keys
            // for alternative ways to retrieve data checks http://www.json.org/javadoc/org/json/JSONObject.html
            String type = (String) jsonObject.get("type");
            String ident = (String) jsonObject.get("ident");
            String air_ground = (String) jsonObject.get("air_ground");
            String alt = (String) jsonObject.get("alt");
            String clock = (String) jsonObject.get("clock");
            String id = (String) jsonObject.get("id");
            String gs = (String) jsonObject.get("gs");
            String heading = (String) jsonObject.get("heading");
            String lat = (String) jsonObject.get("lat");
            String lon = (String) jsonObject.get("lon");
            String reg = (String) jsonObject.get("reg");
            String squawk = (String) jsonObject.get("squawk");
            String updateType = (String) jsonObject.get("updateType");
            // if any field is missing, for eg if "squawk" is missing then squawk value will be null!

            // print values from above
            System.out.println("--------- Parsing Results --------");
            System.out.println(String.format(" %-10s %-10s\n "
                    + "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
                    + "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
                    + "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
                    + "%-10s %-10s\n %-10s %-10s\n %-10s %-10s ",
                    "type", type,
                    "ident", ident,
                    "airground", air_ground,
                    "alt", alt,
                    "clock", clock,
                    "id", id,
                    "gs", gs,
                    "heading", heading,
                    "lat", lat,
                    "lon", lon,
                    "reg", reg,
                    "squawk", squawk,
                    "updateType", updateType
            ));
            System.out.println("---------------------------------\n");

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
