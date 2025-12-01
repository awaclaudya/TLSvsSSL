#!/bin/bash

# --- Configuration ---
FILE_SIZES_MB=(1 5 10 1024) # 1MB, 5MB, 10MB, 1GB
ITERATIONS=3
OUTPUT_CSV="tls13_results.csv"
SERVER_PORT=1235
SERVER_CLASS="TLSServer"
CLIENT_CLASS="TLSClient"
# --- End Configuration ---
touch $OUTPUT_CSV
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
rm *.class
# Find a Java version that supports TLS 1.3 (Java 11+)
# It will first look for Java 17, then 11, then any other version.
JAVA=$(find ~/.sdkman/candidates/java -name "java" -type f 2>/dev/null | grep -E "17\.|21\." | head -1)
if [ -z "$JAVA" ]; then
    echo "Java 11 or 17 not found via SDKMAN. Trying system default..."
    JAVA=$(which java)
    if [ -z "$JAVA" ]; then
        echo "No suitable Java version found. Please install Java 11 or higher for TLS 1.3 support."
        exit 1
    fi
fi

JAVAC_CMD="${JAVA%/java}/javac"
if [ ! -f "$JAVAC_CMD" ]; then
  JAVAC_CMD="javac"
fi

echo "Using Java: $JAVA"
echo "Using Java compiler at: $JAVAC_CMD"

# --- Setup ---
echo "Setting up TLS 1.3 test environment..."
rm -f *.class server.log server.jks "$SCRIPT_DIR/$OUTPUT_CSV"

# Generate a keystore for the server if it doesn't exist
echo "Generating server keystore..."
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -validity 365 -keystore server.jks -storepass changeit -keypass changeit -dname "CN=localhost, OU=Test, O=Test, L=Test, S=Test, C=US" -noprompt

# Compile Java sources
echo "Compiling Java sources..."
 $JAVAC_CMD *.java

# Create CSV header

# --- Start Server ---
echo "Starting TLS 1.3 server..."
 $JAVA -cp . $SERVER_CLASS > server.log 2>&1 &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"

# Wait for server to start and check if it's alive
sleep 5
if ! kill -0 $SERVER_PID > /dev/null 2>&1; then
    echo "---------------------------------------------------------"
    echo "FATAL: Server process (PID $SERVER_PID) failed to start."
    echo "---------------------------------------------------------"
    echo "Server Log Contents:"
    cat server.log
    exit 1
fi
echo "Server is running successfully."

 $JAVA -cp . $CLIENT_CLASS 1000 me 1

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
     $JAVA -cp . $CLIENT_CLASS "$size" "$SCRIPT_DIR/$OUTPUT_CSV" "$i"
    
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