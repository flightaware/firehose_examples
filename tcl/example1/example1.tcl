#!/usr/bin/env tclsh

package require tls
package require yajltcl
package require zlib

set username "XXXXXXXXXXXXXXXXX"
set apikey "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"

set hostname "firehose.flightaware.com"
set compression 0



# Open the TLS socket connection to FlightAware.
set sock [tls::socket -tls1 1 $hostname 1501]
puts "Connected!"

# Send the initiation command to the server.
set initcmd "live version 7.0 user $username password $apikey events {flightplan position}"
if {$compression} {
	append initcmd " compression deflate"
}

puts $sock "$initcmd"
flush $sock

# Activate compression, if requested.
if {$compression} {
	zlib push inflate $sock
}

# Main loop, reading lines of JSON from the server.
set linecount 1
while {$linecount < 10000} {
	if {[catch {set getResult [gets $sock line]} catchResult] == 1} {
		puts "Failed to get line from socket: $catchResult"
		break
	} elseif {$getResult < 0} {
		puts "Reached end of socket."
		break
	}

	puts "Line #$linecount"
	array unset data
	if {[catch {
		array set data [::yajl::json2dict $line]
	} catchResult] == 1} {
		puts "Failed to parse message: $line"
		break
	}
	parray data
	puts "========"

	incr linecount
}
close $sock

puts ""
