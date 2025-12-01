#!/bin/bash

FILE_SIZES_MB=(1 5 10 1024) # 1MB, 5MB, 10MB, 1GB
ITERATIONS=3
OUTPUT_CSV="ssl_results.csv"

touch $OUTPUT_CSV

# Determine the script's directory to find the 'ssl3' folder reliably
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
SSL_DIR="$SCRIPT_DIR/ssl3"

JAVA8=$(find ~/.sdkman/candidates/java -name "java" -type f 2>/dev/null | grep -E "8\.|jdk8" | head -1)

rm -f *.class server.log server.jks
if [ -z "$JAVA8" ]; then
  echo "Java 8 not found"
  exit 1
fi

echo "Using Java: $JAVA8"

# Get javac command
JAVAC_CMD="${JAVA8%/java}/javac"
if [ ! -f "$JAVAC_CMD" ]; then
  JAVAC_CMD="javac"
fi

echo "Using Java compiler at: $JAVAC_CMD"

# Create a custom java.security file to enable SSLv3
echo "jdk.tls.disabledAlgorithms=" > java.security

# Generate a keystore for the server if it doesn't exist
echo "Generating server keystore..."
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -validity 365 -keystore server.jks -storepass changeit -keypass changeit -dname "CN=localhost, OU=Test, O=Test, L=Test, S=Test, C=US" -noprompt

# Compile Java sources
echo "Compiling Java sources..."
 $JAVAC_CMD *.java

echo "Starting SSL 3.0 server..."
 $JAVA8 -Djava.security.properties=$(pwd)/java.security -cp . SSLServer > server.log 2>&1 &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"

sleep 3

if ! kill -0 $SERVER_PID > /dev/null 2>&1; then
    echo "---------------------------------------------------------"
    echo "FATAL: Server process (PID $SERVER_PID) has terminated."
    echo "---------------------------------------------------------"
    echo "Server Log Contents:"
    cat server.log
    exit 1
fi

echo "Server is running. Starting SSL 3.0 client..."
# --- Run Tests ---
echo "Starting test loop..."
TOTAL_TESTS=$((${#FILE_SIZES_MB[@]} * ITERATIONS))
CURRENT_TEST=0

for size in "${FILE_SIZES_MB[@]}"; do
  for (( i=1; i<=ITERATIONS; i++ )); do
    ((CURRENT_TEST++))
    echo "---------------------------------------------------------"
    echo "Running test $CURRENT_TEST of $TOTAL_TESTS: Size=${size}MB, Iteration=$i"
    echo "---------------------------------------------------------"

    # Run the client and capture its output
    $JAVA8 -Djava.security.properties=$(pwd)/java.security -cp . SSLClient "$size" "$SCRIPT_DIR/$OUTPUT_CSV" "$i"

    echo "Test run complete. Waiting 2 seconds before next run..."
    sleep 2
  done
done

# --- Cleanup ---
echo "All tests finished. Stopping server (PID $SERVER_PID)..."
kill $SERVER_PID 2>/dev/null || true
wait $SERVER_PID 2>/dev/null || true

echo "---------------------------------------------------------"
echo "All tests complete!"
echo "Master results file located at: $SCRIPT_DIR/$OUTPUT_CSV"
echo "---------------------------------------------------------"

echo "Test complete!"