import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Random;

public class SSLClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3000;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java SSLClient <file_size_in_mb> <output_file> <iteration>");
            System.out.println("Example: java SSLClient 10 results.csv 1");
            System.exit(1);
        }

        int fileSizeMB = Integer.parseInt(args[0]);
        String outputFile = args[1];
        int iteration = Integer.parseInt(args[2]);

        System.out.println("Starting client with SSL 3.0");
        System.out.println("File size: " + fileSizeMB + " MB");

        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSLv3");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_HOST, SERVER_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // *** SYNCHRONIZED CONFIGURATION ***
                // Use a list of common SSLv3 cipher suites.
                String[] enabledCipherSuites = {
                        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                        "SSL_RSA_WITH_RC4_128_SHA",
                        "SSL_RSA_WITH_RC4_128_MD5"
                };

                socket.setEnabledProtocols(new String[] { "SSLv3" });
                socket.setEnabledCipherSuites(enabledCipherSuites);

                System.out.println("Client enabled protocols: " + java.util.Arrays.toString(socket.getEnabledProtocols()));
                System.out.println("Client enabled cipher suites: " + java.util.Arrays.toString(socket.getEnabledCipherSuites()));

                socket.startHandshake();

                System.out.println("Negotiated protocol: " + socket.getSession().getProtocol());
                System.out.println("Negotiated cipher suite: " + socket.getSession().getCipherSuite());

                long fileSizeBytes = (long) fileSizeMB * 1024 * 1024;
                out.println("SIZE:" + fileSizeBytes);

                String response = in.readLine();
                if (!response.equals("READY")) {
                    System.out.println("Server not ready: " + response);
                    return;
                }

                byte[] buffer = new byte[8192];
                new Random().nextBytes(buffer);

                long startTime = System.currentTimeMillis();
                long totalBytesSent = 0;
                OutputStream os = socket.getOutputStream();
                while (totalBytesSent < fileSizeBytes) {
                    int bytesToSend = (int) Math.min(buffer.length, fileSizeBytes - totalBytesSent);
                    os.write(buffer, 0, bytesToSend);
                    totalBytesSent += bytesToSend;
                }
                os.flush();

                response = in.readLine();
                if (response.startsWith("RECEIVED:")) {
                    String[] parts = response.split(":");
                    long bytesReceived = Long.parseLong(parts[1]);
                    long duration = Long.parseLong(parts[2]);
                    double throughputMBps = (bytesReceived / (1024.0 * 1024.0)) / (duration / 1000.0);
                    System.out.println("Server received " + bytesReceived + " bytes in " + duration + "ms");
                    System.out.println("Throughput: " + String.format("%.2f", throughputMBps) + " MB/s");
                    try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true))) {
                        writer.println("SSLv3," + fileSizeMB + "," + duration + "," + throughputMBps + "," + iteration);
                    }
                }

                out.println("QUIT");
            }
        } catch (Exception e) {
            System.err.println("SSL Client Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}