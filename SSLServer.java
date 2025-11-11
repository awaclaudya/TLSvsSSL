
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.KeyManagerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;

public class SSLServer {
	private static final int PORT = 1234;

	public static void main(String[] args) {
		try {
			// 1. Load keystore with server private key and certificate
			char[] password = "password".toCharArray();

			KeyStore keyStore = KeyStore.getInstance("JKS");
			try (FileInputStream fis = new FileInputStream("server.keystore")) {
				keyStore.load(fis, password);
			}

			// 2. Create KeyManagerFactory
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keyStore, password);

			// 3. Create SSL/TLS context
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), null, null);

			// 4. Server socket factory
			SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();

			// 5. Create SSL server socket
			try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT)) {
				System.out.println("Server started. Waiting for client...");

				// 6. Accept client connections
				try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
					System.out.println("Client connected.");

					// 7. Communicate with client
					try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

						String message;
						while ((message = in.readLine()) != null) {
							System.out.println("Received from client: " + message);
							out.println("Server received: " + message);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
