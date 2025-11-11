#!/usr/bin/env bash
set -euo pipefail

BASEDIR="$(cd "$(dirname "$0")" && pwd)"
cd "$BASEDIR"

JAVAC=$(command -v javac)
JAVA=$(command -v java)

if [ -z "$JAVAC" ] || [ -z "$JAVA" ]; then
  echo "javac/java not found in PATH. Please install JDK and try again."
  exit 1
fi

echo "Compiling Java sources..."
$JAVAC *.java

echo "Starting SSLServer in background..."
# Start server in background; redirect output to server.log
$JAVA -cp . SSLServer > server.log 2>&1 &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"

# Give the server a moment to start
sleep 1

echo "Running SSLClientExample..."
$JAVA -cp . SSLClientExample

echo "Client finished. Stopping server (PID $SERVER_PID)..."
kill $SERVER_PID || true
wait $SERVER_PID 2>/dev/null || true

echo "Done. Server log is in server.log"
