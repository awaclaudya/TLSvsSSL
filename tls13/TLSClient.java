import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Random;

public class TLSClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 3001;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java TLSClient <file_size_in_mb> <output_file> <iteration>");
            System.out.println("Example: java TLSClient 10 results.csv 1");
            System.exit(1);
        }
        
        int fileSizeMB = Integer.parseInt(args[0]);
        String outputFile = args[1];
        int iteration = Integer.parseInt(args[2]);
        
        System.out.println("Starting client with TLS 1.3");
        System.out.println("File size: " + fileSizeMB + " MB");
        
        try {
            // Create a TrustManager that trusts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };

            // Initialize an SSLContext with the "trust all" TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            
            try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_HOST, SERVER_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 // *** FIX: Changed clientSocket to socket on the line below ***
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                
                // Enable ONLY TLS 1.3
                socket.setEnabledProtocols(new String[] { "TLSv1.3" });
                
                System.out.println("Enabled protocols: " + Arrays.toString(socket.getEnabledProtocols()));
                
                socket.startHandshake();
                
                System.out.println("Negotiated protocol: " + socket.getSession().getProtocol());
                System.out.println("Cipher suite: " + socket.getSession().getCipherSuite());
                
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
                    System.out.println("Throughput: " + throughputMBps + " MB/s");
                    
                    try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true))) {
                        writer.println("TLSv1.3," + fileSizeMB + "," + duration + "," + throughputMBps + "," + iteration);
                    }
                }
                
                out.println("QUIT");
            }
        } catch (Exception e) {
            System.err.println("TLS Client Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}