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
    String adt; // actual depature time
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

        if (id == null) throw new RuntimeException("id cannot be null " + departure.toString() + " " + arrival.toString());
        if (ident == null) ident = "";
        if (orig == null) orig = "";
        if (dest == null) dest = "";
        if (adt == null) adt = "";
        if (aat == null) aat = "";

        return String.format("%s,%s,%s,%s,%s,%s",
                id, orig, dest, ident, adt, aat);
    }
}

public class Example3 {

    private static final Map<String, Flight> flights = new HashMap();

    private static final boolean useCompression = false;

    public static void main(String[] args) {
        String machineName = "firehose.flightaware.com";
        if (args.length != 2) {
            RunClient(machineName, System.getProperty("firehose.username"), System.getProperty("firehose.password"));
        } else {
            RunClient(machineName, args[0], args[1]);
        }
        System.out.println(" Thank you for using FlightAware ... bye now");
    }

    private static void PrintBoard() {
        long now = System.currentTimeMillis();
        now = TimeUnit.SECONDS.convert(now, TimeUnit.MILLISECONDS);
        for (Flight flight : flights.values()) {
            System.out.println(now + "," + flight.toString());
        }
    }

    private static void RunClient(String machineName, String username, String password) {
        // Request 3 days ago
        long startTime = System.currentTimeMillis();
        startTime = TimeUnit.SECONDS.convert(startTime, TimeUnit.MILLISECONDS);
        startTime = startTime - TimeUnit.DAYS.toSeconds(3L);
        // Initiate data stream
        String initiation_command = String.format("pitr " + startTime + " username %s password %s", username, password);
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
            String message;
            long start = System.currentTimeMillis();
            while ((message = reader.readLine()) != null) {
                FlightObject flight = gson.fromJson(message, FlightObject.class);
                if (flight.type.equals("arrival") || flight.type.equals("departure")) {
	                Flight f = flights.get(flight.id);
	                if (f == null) {
	                    f = new Flight();
                        flights.put(flight.id, f);
                    }
                    // Update either the arrival or departure
                    if (flight.type.equals("arrival")) {
	                    f.arrival = flight;
	                } else {
	                    f.departure = flight;
	                }
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
