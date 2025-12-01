import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class SSLServer {
    private static final int PORT = 3000;
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

            // Use the "trust all" TrustManager as before
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };

            // Initialize the SSLContext with the KeyManagers and TrustManagers
            SSLContext sslContext = SSLContext.getInstance("SSLv3");
            sslContext.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());

            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();

            try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT)) {

                // Use the synchronized list of cipher suites
                String[] enabledCipherSuites = {
                        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                        "SSL_RSA_WITH_RC4_128_SHA",
                        "SSL_RSA_WITH_RC4_128_MD5"
                };

                serverSocket.setEnabledProtocols(new String[] { "SSLv3" });
                serverSocket.setEnabledCipherSuites(enabledCipherSuites);

                System.out.println("Server enabled protocols: " + Arrays.toString(serverSocket.getEnabledProtocols()));
                System.out.println("Server enabled cipher suites: " + Arrays.toString(serverSocket.getEnabledCipherSuites()));
                System.out.println("Server started with keystore. Waiting for client...");

                while (true) {
                    try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
                        System.out.println("Client connected.");
                        System.out.println("Negotiated protocol: " + clientSocket.getSession().getProtocol());
                        System.out.println("Negotiated cipher suite: " + clientSocket.getSession().getCipherSuite());

                        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                             InputStream rawIn = clientSocket.getInputStream()) {

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
                                    while (totalBytes < fileSize && (bytesRead = rawIn.read(buffer)) != -1) {
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
            System.err.println("SSL Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}