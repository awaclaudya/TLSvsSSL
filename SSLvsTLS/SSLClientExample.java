
import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SSLClientExample {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    public static void main(String[] args) {
        try {
            // 1. Trust Manager que aceita qualquer certificado
            TrustManager[] trustManagers = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                            // Intencionalmente vazio: aceita qualquer cliente
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                            // Intencionalmente vazio: aceita qualquer servidor
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            // 2. C r i a o do contexto SSL/TLS
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            // 3. F brica de sockets SSL
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            // 4. C r i a o e uso do socket SSL
            try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_HOST, SERVER_PORT);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                // 5. Envio de mensagem
                out.println("Hello, Server!");
                // 6. Leitura da resposta
                String response = in.readLine();
                System.out.println("Received from server: " + response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
