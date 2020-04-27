<?
#Always use this example if you are not using compression
#Also use this example if you are using compression, and expecting a high volume of messages

$username = 'XXXXXXXXXX';
$apikey = 'XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX';
$compression = 0;


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
    // compress, gzip, deflate
    $initcmd .= " compression deflate";
}
fwrite($fp, "$initcmd\n");

// Apply a decompression filter, if requested.
if ($compression && !stream_filter_append($fp, 'zlib.inflate', STREAM_FILTER_READ)) {
   echo "Error appending filter.\n";
   exit(1);
}

// Main loop, reading lines of JSON from the server.
$i = 1;
while (($buffer = fgets($fp)) !== false) {
    echo "LINE $i\n";
    
    $data = json_decode($buffer);
    if (json_last_error() !== JSON_ERROR_NONE) {
        echo "Error: invalid json.\n";
        echo $buffer;
        break;
    }
    print_r($data);

    if ($i++ >= 10) {
        break;
    }
}
fclose($fp);
echo "All done.\n";

?>