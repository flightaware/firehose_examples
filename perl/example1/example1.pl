#!/usr/local/bin/perl

use strict;
use IO::Socket::SSL;
use JSON::PP;
use IO::Uncompress::Inflate qw($InflateError);
use Data::Dumper;

my $username = 'XXXXXXXXXX';
my $apikey = 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX';
my $compression = 0;


# Open the TLS socket connection to FlightAware.
my $sock = IO::Socket::SSL->new('firehose-test.flightaware.com:1501') or die $!;
print "Connected!\n";

# Send the initiation command to the uncompressed socket.
my $initcmd = "live version 7.0 user $username password $apikey events \"flightplan position\"";
if ($compression) {
    $initcmd .= " compression compress";
}
binmode $sock;
print $sock "$initcmd\n";

# Activate compression, if requested.
my $zsock;
if ($compression) {
    $zsock = new IO::Uncompress::Inflate $sock
	or die "IO::Uncompress::Inflate failed: $InflateError\n";
} else {
    $zsock = $sock;
}

# Main loop, reading lines of JSON from the server.
my $i = 1;
while (my $line = $zsock->getline()) {
    #print "LINE $i\n";
    #print "LINE $i: ", $line, "\n";

    my $data = eval { decode_json $line };
    die "Failed to decode JSON: $line" if !defined($data) || $@;

    print "LINE $i\n" . Dumper($data);
    
    last if ($i++ >= 10);
}
close $sock;

print "All done.\n";
