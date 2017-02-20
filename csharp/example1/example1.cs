using System;
using System.Threading;
using System.Collections;
using System.Net;
using System.Net.Security;
using System.Net.Sockets;
using System.Security.Authentication;
using System.Text;
using System.Security.Cryptography.X509Certificates;
using System.IO;
using System.IO.Compression;
using Newtonsoft.Json;

namespace SSLClient
{

    public class FlightObject
    {

        public String type;
        public String ident;
        public String air_ground;
        public String alt;
        public String clock;
        public String id;
        public String gs;
        public String heading;
        public String lat;
        public String lon;
        public String reg;
        public String squawk;
        public String updateType;


        public String toString()
        {
            String result;
            // format result into 2 columns, left justify data, min 10 chars col space
            result = String.Format(" {0,-10} {1,-10}\n {2,-10} {3,-10}\n {4,-10} {5,-10}\n" +
                                   " {6,-10} {7,-10}\n {8,-10} {9,-10}\n {10,-10} {11,-10}\n" +
                                   " {12,-10} {13,-10}\n {14,-10} {15,-10}\n {16,-10} {17,-10}\n" +
                                   " {18,-10} {19,-10}\n {20,-10} {21,-10}\n {22,-10} {23,-10}\n" +
                                   " {24,-10} {25,-10}\n",
                                   "type", type,
                                   "ident", ident,
                                   "airground", air_ground,
                                   "alt", alt,
                                   "clock", clock,
                                   "id", id,
                                   "gs", gs,
                                   "heading", heading,
                                   "lat", lat,
                                   "lon", lon,
                                   "reg", reg,
                                   "squawk", squawk,
                                   "updateType", updateType
                                  );
            return result;
        }
    }


    public class SSLClient
    {
        public static String username = "XXXXXXXX";
        public static String apikey = "XXXXXXXXXXXXXXXXXXXX";
        public static Boolean useCompression = true;
        public static String initiation_command = "live username " + username + " password " + apikey +
                (useCompression ? " compression deflate" : "") + "\n";

        // The following method is invoked by the RemoteCertificateValidationDelegate
        // prevent communication with unauthenticated server
        public static bool ValidateServerCertificate(
            object sender,
            X509Certificate certificate,
            X509Chain chain,
            SslPolicyErrors sslPolicyErrors)
        {
            if (sslPolicyErrors == SslPolicyErrors.None)
            {
                // authenticated
                return true;
            }

            Console.WriteLine("Certificate error: {0}", sslPolicyErrors);
            // Do not allow this client to communicate with unauthenticated servers.
            return false;
        }

        public static void RunClient(string machineName, string serverName)
        {
            // Create a TCP/IP client socket.
            TcpClient client = new TcpClient(machineName, 1501);

            // Create ssl stream to read data
            SslStream sslStream = new SslStream(
                client.GetStream(),
                true,
                new RemoteCertificateValidationCallback(ValidateServerCertificate),
                null);
            try
            {
                // server name must match name on the server certificate.
                sslStream.AuthenticateAsClient(serverName);
                Console.WriteLine("sslStream AuthenticateAsClient completed.");
            }
            catch (AuthenticationException e)
            {
                Console.WriteLine("Exception: {0}", e.Message);
                if (e.InnerException != null)
                {
                    Console.WriteLine("Inner exception: {0}", e.InnerException.Message);
                }
                Console.WriteLine("Authentication failed - closing the connection.");
                client.Close();
                return;
            }

            // Send initiation command to the server.
            // Encode to a byte array.
            byte[] messsage = Encoding.UTF8.GetBytes(initiation_command + "\n");
            sslStream.Write(messsage);
            sslStream.Flush();

            //read from server, print to console:
            StreamReader sr;
            if (useCompression)
            {
                sr = new StreamReader(new DeflateStream(sslStream, CompressionMode.Decompress));
            }
            else
            {
                sr = new StreamReader(sslStream);
            }
            int limit = 1000;
            while (limit > 0)
            {
                string line = sr.ReadLine();
                Console.WriteLine(" Received: " + line);
                parse(line);
                limit--;
            }

            // Close the client connection.
            sr.Close();
            client.Close();
            Console.WriteLine("Client closed.");
        }

        public static void parse(string mes)
        {
            //parse with JSON.NET
            FlightObject flight = JsonConvert.DeserializeObject<FlightObject>(mes);
            Console.WriteLine(" --------------- Message ------------------ \n");
            Console.WriteLine(flight.toString());
            Console.WriteLine(" ------------------------------------------ \n");
        }

        public static int Main(string[] args)
        {

            // machineName is the host running the server application.
            String machineName = "firehose.flightaware.com";
            String serverCertificateName = machineName;

            //connect, read data
            SSLClient.RunClient(machineName, serverCertificateName);

            Console.WriteLine(" Hit Enter to end ...");
            Console.Read();
            return 0;
        }
    }
}
