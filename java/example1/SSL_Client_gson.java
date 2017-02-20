import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.*;
import com.google.gson.Gson;

public class SSL_Client_gson {

    // substitute your own username and password
    private static String initiation_command = "live username XXXXXXXX password XXXXXXXXXXXXXXXXXXXX";
    private SSLSocket ssl_socket;
    private static final boolean useCompression = false;

    public static void main(String[] args) {
        String machineName = "firehose.flightaware.com";
        RunClient(machineName);
        System.out.println(" Thank you for using FlightAware ... bye now");
    }

    private class FlightObject {

        //define here all fields of interest from the received messages
        public String type;
        public String ident;
        public String air_ground;
        public String alt;
        public String clock;
        public String id;
        public String gs;
        public String heading;
        public String lat;
        public String lon;
        public String reg;
        public String squawk;
        public String updateType;

        @Override
        public String toString() {
            String result;
            //if any field is missing in the received message,
            //for eg if "squawk" is missing then squawk value will be null!
            //format as a table left justified, 10 chars min width
            result = String.format("%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
                    + "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
                    + "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
                    + "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
                    + "%-10s %-10s\n",
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
            );
            return result;
        }
    }

    public static void RunClient(String machineName) {
        System.out.println(" Running Client");
        try {
            SSLSocket ssl_socket;
            ssl_socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(machineName, 1501);
            // enable certifcate validation:
            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
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
            Gson gson = new Gson();
            String message = null;
            int limit = 10; //limit number messages for testing
            while (limit > 0 && (message = reader.readLine()) != null) {
                System.out.println("msg: " + message);
                //parse message with gson
                System.out.printf("---------------- Parsing ---------------------\n");
                FlightObject flight = gson.fromJson(message, FlightObject.class);
                System.out.println(flight);
                System.out.println("---------------------------------------------\n");
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
}
