Example3

Read the FlightAware Firehose stream and aggregate the departure and arrival messages.  Collect the departures and
arrivals in a hashmap.
Every 30 seconds print the state of the hashmap as CSV to standard out.

This program can be compiled and run with Apache Maven.  It can also be imported into IntelliJ as a Maven project.

This program uses Googles JSON parser for de-serializing JSON into Java objects.