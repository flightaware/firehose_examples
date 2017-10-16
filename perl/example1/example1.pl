#!/usr/local/bin/perl

use strict;
use IO::Socket::SSL;
use JSON::PP;
use Compress::Zlib;
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
    $zsock = inflateInit()
    or die "Could not initiate inflate\n";
} else {
    $zsock = $sock;
}

# Main loop, reading lines of JSON from the server.
my $i = 1;
my $buffer = "";
while (1) {
    #print "LINE $i\n";
    #print "LINE $i: ", $line, "\n";

    my $line = "";
    my $data_available = 1;

    if ($compression) {
        if (index($buffer, "\n") == -1) {
            $data_available = read($sock, $line, 8192);
            (my $output, my $status) = $zsock->inflate($line);
            $line = $output;

            $buffer = $buffer . $line;
        }

        my $rawline = "";
        my $newline_index = index($buffer, "\n");

        if ($newline_index != -1) {
            $rawline = substr($buffer, 0, $newline_index);
            $buffer = substr($buffer, $newline_index + 1);
        }

        $line = $rawline

    } else {
        $line = $sock->getline();
        if(!defined($line)) {
            $data_available = 0;
        }
    }

    if ($line ne "") {
        my $data = eval { decode_json $line };
        die "Failed to decode JSON: $line" if !defined($data) || $@;

        print "LINE $i\n" . Dumper($data);

        last if ($i++ >= 1000);
    } 

    last if (!$data_available);
    
}
close $sock;

print "All done.\n";
