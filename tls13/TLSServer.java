import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class TLSServer {
    private static final int PORT = 3001;
    private static final String KEYSTORE_FILE = "server.jks";
    private static final String KEYSTORE_PASSWORD = "changeit";

    public static void main(String[] args) {
        try {
            // Load the keystore that contains the server's certificate
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD.toCharArray());

            // Create a KeyManagerFactory and initialize it with the keystore
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

            // Initialize the SSLContext with the KeyManagers and default TrustManagers
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();

            try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT)) {
                // Enable ONLY TLS 1.3
                serverSocket.setEnabledProtocols(new String[] { "TLSv1.3" });

                System.out.println("Server started on port " + PORT);
                System.out.println("Enabled protocols: " + Arrays.toString(serverSocket.getEnabledProtocols()));
                System.out.println("Waiting for client...");

                while (true) {
                    try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
                        System.out.println("Client connected");
                        System.out.println("Negotiated protocol: " + clientSocket.getSession().getProtocol());
                        System.out.println("Cipher suite: " + clientSocket.getSession().getCipherSuite());

                        // Communicate with client
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                             InputStream rawInput = clientSocket.getInputStream()) {

                            String message;
                            while ((message = in.readLine()) != null) {
                                if (message.startsWith("SIZE:")) {
                                    String sizeStr = message.substring(5);
                                    long fileSize = Long.parseLong(sizeStr);
                                    System.out.println("Client sending file of size: " + fileSize + " bytes");
                                    out.println("READY");

                                    long startTime = System.currentTimeMillis();
                                    long totalBytes = 0;

                                    byte[] buffer = new byte[8192];
                                    int bytesRead;
                                    while (totalBytes < fileSize && (bytesRead = rawInput.read(buffer)) != -1) {
                                        totalBytes += bytesRead;
                                    }

                                    long endTime = System.currentTimeMillis();
                                    long duration = endTime - startTime;

                                    out.println("RECEIVED:" + totalBytes + ":" + duration);
                                    System.out.println("Received " + totalBytes + " bytes in " + duration + "ms");
                                } else if (message.equals("QUIT")) {
                                    System.out.println("Client requested to quit.");
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error handling client: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("TLS Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}