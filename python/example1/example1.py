#!/usr/bin/env python

import json, socket, ssl, sys, time, zlib


username = "XXXXXXXXXX"
apikey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
compression = None        # set to "deflate", "decompress", or "gzip" to enable compression
servername = "firehose.flightaware.com"


class InflateStream:
   "A wrapper for a socket carrying compressed data that does streaming decompression"

   def __init__(self, sock, mode):
      self.sock = sock
      self._buf = bytearray()
      self._eof = False
      if mode == 'deflate':     # no header, raw deflate stream
         self._z = zlib.decompressobj(-zlib.MAX_WBITS)
      elif mode == 'compress':  # zlib header
         self._z = zlib.decompressobj(zlib.MAX_WBITS)
      elif mode == 'gzip':      # gzip header
         self._z = zlib.decompressobj(16 | zlib.MAX_WBITS)
      else:
         raise ValueError('unrecognized compression mode')

   def _fill(self):
      rawdata = self.sock.recv(8192)
      if len(rawdata) == 0:
         self._buf += self._z.flush()
         self._eof = True
      else:
         self._buf += self._z.decompress(rawdata)

   def readline(self):
      newline = self._buf.find(b'\n')
      while newline < 0 and not self._eof:
         self._fill()
         newline = self._buf.find(b'\n')

      if newline >= 0:
         rawline = self._buf[:newline+1]
         del self._buf[:newline+1]
         return rawline.decode('ascii')

      # EOF
      return ''


# function to parse JSON data:
def parse_json( str ):
   try:
       # parse all data into dictionary decoded:
       decoded = json.loads(str)
       print(decoded)

       # compute the latency of this message:
       clocknow = time.time()
       diff = clocknow - int(decoded['pitr'])
       print("diff = {0:.2f} s\n".format(diff))
   except (ValueError, KeyError, TypeError):
       print("JSON format error: ", sys.exc_info()[0])
       print(str)
       #print(traceback.format_exc())
   return;

# Create socket
sock = socket.socket(socket.AF_INET)
# Create a SSL context with the recommended security settings for client sockets, including automatic certificate verification
context = ssl.create_default_context()
# the folowing line requires Python 3.7+ and OpenSSL 1.1.0g+ to specify minimum_version
context.minimum_version = ssl.TLSVersion.TLSv1_2

ssl_sock = context.wrap_socket(sock, server_hostname = servername)
print("Connecting...")
ssl_sock.connect((servername, 1501))
print("Connection succeeded")

# build the initiation command:
initiation_command = "live username {} password {}".format(username, apikey)
if compression is not None:
   initiation_command += " compression " + compression

# send initialization command to server:
initiation_command += "\n"
if sys.version_info[0] >= 3:
    ssl_sock.write(bytes(initiation_command, 'UTF-8'))
else:
    ssl_sock.write(initiation_command)

# return a file object associated with the socket
if compression is not None:
   file = InflateStream(sock = ssl_sock, mode = compression)
else:
   file = ssl_sock.makefile('r')

# use "while True" for no limit in messages received
count = 10
while (count > 0):
   try :
      # read line from file:
      inline = file.readline()
      if inline == '':
         # EOF
         break

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

