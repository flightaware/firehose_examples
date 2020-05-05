package com.flightaware.firehose.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import com.google.gson.Gson;

class FlightObject {

    //define here all fields of interest from the received messages
    String id;
    String type;
    String ident;
    String orig;
    String dest;
    String aat; // actual arrival time
    String adt; // actual departure time
    String reg;

    @Override
    public String toString() {
        return "FlightObject{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", ident='" + ident + '\'' +
                ", orig='" + orig + '\'' +
                ", dest='" + dest + '\'' +
                ", aat='" + aat + '\'' +
                ", adt='" + adt + '\'' +
                ", reg='" + reg + '\'' +
                '}';
    }
}

class Flight {
    FlightObject departure;
    FlightObject arrival;

    @Override
    public String toString() {
        String id = "";
        String ident = "";
        String orig = "";
        String dest = "";
        String adt = "";
        String aat = "";

        // Handle all cases where null can be present
        if (departure != null) {
            orig = departure.orig;
            adt = departure.adt;
            ident = departure.ident;
            id = departure.id;
        }

        if (arrival != null) {
            ident = arrival.ident;
            dest = arrival.dest;
            aat = arrival.aat;
            id = arrival.id;
        }

        if (id == null) {
            throw new RuntimeException("id cannot be null " + departure.toString() + " " + arrival.toString());
        }
        if (ident == null) ident = "";
        if (orig == null) orig = "";
        if (dest == null) dest = "";
        if (adt == null) adt = "";
        if (aat == null) aat = "";

        return String.format("%s,%s,%s,%s,%s,%s",
                id, orig, dest, ident, adt, aat);
    }
}

/*
 * Aggregate all of the departure and arrival messages into a single object.
 * Join the messages based on the id key.
 *
 * Since we join the feed in the past, we will have arrivals without departures.
 */
public class Example3 {

    // Our flight board state is saved in a HashMap in RAM
    // The key is the id field, the flight id FA generates.
    private static final Map<String, Flight> flights = new HashMap();

    // GZIP the data on the network wire
    private static final boolean useCompression = false;

    //
    // Either pass the username and API access token on the command line or
    // set properties
    //
    // You can run with maven using the target verify to run the program.
    // Fill in your username and API key
    //
    // mvn -Dfirehose.username=##### -Dfirehose.password=###### verify
    //
    public static void main(String[] args) {
        String machineName = "firehose.flightaware.com";
        if (args.length != 2) {
            RunClient(machineName, System.getProperty("firehose.username"), System.getProperty("firehose.password"));
        } else {
            RunClient(machineName, args[0], args[1]);
        }
        System.out.println(" Thank you for using FlightAware ... bye now");
    }

    /*
     * Print out the hash map as CSV
     */
    private static void PrintBoard() {
        long now = System.currentTimeMillis();
        now = TimeUnit.SECONDS.convert(now, TimeUnit.MILLISECONDS);
        for (Flight flight : flights.values()) {
            System.out.println(now + "," + flight.toString());
        }
    }

    private static void RunClient(String machineName, String username, String password) {
        // Request data from 3 days ago
        // It can take awhile to catch up, but this insures we get all the departures
        // 3 days is a bit conservative
        long startTime = System.currentTimeMillis();
        startTime = TimeUnit.SECONDS.convert(startTime, TimeUnit.MILLISECONDS);
        startTime = startTime - TimeUnit.DAYS.toSeconds(3L);

        // Initiate data stream
        String initiation_command = String.format("pitr " + startTime + " username %s password %s", username, password);
        try {
            SSLSocket ssl_socket;
            ssl_socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(machineName, 1501);
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
            Gson gson = new Gson();
            String message;
            long start = System.currentTimeMillis();
            while ((message = reader.readLine()) != null) {
                // Parse the JSON with Googles GSON
                // Any missing fields will be null
                FlightObject flight = gson.fromJson(message, FlightObject.class);
                // Filter by message type
                if (flight.type.equals("arrival") || flight.type.equals("departure")) {
                    // Get last value from map
                    Flight f = flights.get(flight.id);
                    if (f == null) {
                        // Add a value to map
                        f = new Flight();
                        flights.put(flight.id, f);
                    }
                    // Update either the arrival or departure
                    if (flight.type.equals("arrival")) {
                            f.arrival = flight;
                        } else {
                            f.departure = flight;
                        }
                        // Every 30 seconds print out the collection
                        long now = System.currentTimeMillis();
                        if (now - start > TimeUnit.SECONDS.toMillis(30)) {
                        start = now;
                        PrintBoard();
                    }
                }
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
