#!/usr/bin/env python

import json, socket, ssl, sys, time


username = "XXXXXXXXXX"
apikey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
compression = 0
servername = "firehose.flightaware.com"



# function to parse JSON data:
def parse_json( str ):
   try:
       # parse all data into dictionary decoded:
       decoded = json.loads(str)
       clocknow = time.time()
       diff = clocknow - int(decoded['pitr'])
       print "diff = {0:.2f} s".format(diff)
   except (ValueError, KeyError, TypeError):
       print("JSON format error: ", sys.exc_info()[0])
       print(str)
       #print(traceback.format_exc())
   return;

# Create socket
sock = socket.socket(socket.AF_INET)
# Create a SSL context with the recommended security settings for client sockets, including automatic certificate verification
context = ssl.create_default_context()
# Alternatively, a customized context could be created
#context = ssl.SSLContext(ssl.PROTOCOL_SSLv23)
#context.verify_mode = ssl.CERT_REQUIRED
#context.check_hostname = True
# Load a set of default CA certificates from default locations
#context.load_default_certs()

ssl_sock = context.wrap_socket(sock, server_hostname = servername)
print("Connecting...")
ssl_sock.connect((servername, 1501))
print("Connection succeeded")

# build the initiation command:
initiation_command = "live username {} password {}".format(username, apikey)
if compression:
   initiation_command += " compression gzip"

# send initialization command to server:
initiation_command += "\n"
if sys.version_info[0] >= 3:
    ssl_sock.write(bytes(initiation_command, 'UTF-8'))
else:
    ssl_sock.write(initiation_command)

# return a file object associated with the socket
if compression:
   if sys.version_info[0] >= 3:
      from gzip import GzipFile
      file = gzip.GzipFile(fileobj = ssl_sock.makefile('rb'), mode = 'r')
   else:
      # compression mode on Python 2 requires GzipStream handler from:
      # https://fedorahosted.org/spacewalk/wiki/Projects/python-gzipstream
      from gzipstream import GzipStream
      file = GzipStream(ssl_sock.makefile('r'), 'r')
else:
   file = ssl_sock.makefile('r')

# use "while True" for no limit in messages received
count = 10
while (count > 0):
   try :
      # read line from file:
      if sys.version_info[0] >= 3 and compression:
         inline = file.readline().decode('utf-8')
      else:
         inline = file.readline()

      # print(inline)

      # parse the line
      parse_json(inline)
      count = count - 1
   except socket.error as e:
      print('Connection fail', e)
      print(traceback.format_exc())

# wait for user input to end
# input("\n Press Enter to exit...");
# close the SSLSocket, will also close the underlying socket
ssl_sock.close()

