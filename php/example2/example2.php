#!/usr/local/bin/php
<?php
#Only use this example if you are running live, expecting a low volume of input and using compression
#Note: If you are using the "range" command with compression, the script will not end on its own.  

# pkg install php56 php56-openssl php56-zlib php56-json


$username = 'XXXXXXXXXX';
$apikey = 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX';
$compression = 1;

class InflateStream {
    private $buffer = "";
    private $sock;

    public function __construct($sock) {
        $this->sock = $sock;
        if (!stream_filter_append($this->sock, 'zlib.inflate', STREAM_FILTER_READ)) {
            echo "Error appending filter.\n";
            exit(1);
        }

        stream_set_blocking($this->sock, 0);
    }

    public function readline() {
        $newline = strpos($this->buffer, "\n");
        while (($newline == false)) {
            $socket_stream = fgets($this->sock);

            if($socket_stream !== false) {
                $this->buffer .= $socket_stream;
                $newline = strpos($this->buffer, "\n");
            }
        }

        $rawline = substr($this->buffer, 0, $newline + 1);
        $this->buffer = substr($this->buffer, $newline + 1);
        return $rawline;
    }
}

// Open the TLS socket connection to FlightAware.
$fp = fsockopen("tcp://firehose.flightaware.com", 1501, $errno, $errstr, 30);
if (!$fp) {
    echo "Error connecting ($errno): $errstr\n";
    exit(1);
}
if (!stream_socket_enable_crypto($fp, true, STREAM_CRYPTO_METHOD_TLSv1_2_CLIENT)) {
   echo "Error negotiating TLS\n";
   fclose($fp);
   exit(1);
}
echo "Connected!\n";

// Send the initiation command to the uncompressed socket.
$initcmd = "live version 7.0 user $username password $apikey events \"flightplan position\"";
if ($compression) {
    $initcmd .= " compression deflate";
}
fwrite($fp, "$initcmd\n");

// Apply a decompression filter, if requested.
if ($compression) {
    $inflate = new InflateStream($fp);
}



// Main loop, reading lines of JSON from the server.
$i = 1;

while ($i < 10000) {

    if ($compression) {
        $buffer = $inflate->readline();
    } else {
        $buffer = fgets($fp);
    }

    if ($buffer == false) {
        break;
    }

    echo "LINE $i\n";

    $data = json_decode($buffer);
    echo $buffer;
    if (json_last_error() !== JSON_ERROR_NONE) {
        echo "Error: invalid json.\n";
        echo $buffer;
        break;
    }

    $i += 1;
}
fclose($fp);
echo "All done.\n";

?>
