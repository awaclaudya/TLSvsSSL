# SSL Example (Server & Client)

This small project demonstrates a simple SSL/TLS server (`SSLServer.java`) and a client (`SSLClientExample.java`). 

The server uses a JKS keystore (`server.keystore`) containing the server private key and certificate. 

The client in this example uses a permissive TrustManager that accepts any certificate (for testing only).

Files:
- `SSLServer.java` — simple SSL server that listens on port 1234 and echoes received lines.
- `SSLClientExample.java` — simple SSL client that connects to localhost:1234 and sends a single message.
- `setup_keystores.sh` — helper script to create `server.keystore` for local testing.
- `run.sh` — helper script to compile and run the server and client.

Warning: These instructions are for local testing only. Do NOT use a permissive TrustManager or self-signed keys in production.

Requirements
- Java 8+ (javac/java)
- keytool (part of the JDK)

Quick start

1. Make the helper scripts executable:

```bash
chmod +x setup_keystores.sh run.sh
```

2. Generate the server keystore (creates `server.keystore`):

```bash
./setup_keystores.sh
```

3. Compile and run the server and client (the script starts the server in background, then runs the client):

```bash
./run.sh
```

What the scripts do
- `setup_keystores.sh` uses `keytool` to create a self-signed certificate and store it in `server.keystore` (password: `password`).
- `run.sh` compiles the Java sources and starts the server in the background. It then runs the client which connects and sends a message.

Notes
- `server.keystore` is created with alias `server` and password `password` to match the code in `SSLServer.java`.
- If you prefer a more secure setup, create a CA, sign server certificates, and configure proper truststores on the client side.
