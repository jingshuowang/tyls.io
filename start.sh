#!/bin/bash
# Tyls.io Server Launcher (macOS)

echo "========================================="
echo "  Tyls.io Server Launcher"
echo "========================================="

# Navigate to project root (where this script lives)
cd "$(dirname "$0")"

# Kill any existing server processes on ports 8001/8002
echo "Checking for old server processes..."
lsof -ti :8001 | xargs kill -9 2>/dev/null && echo "Killed old process on port 8001." || true
lsof -ti :8002 | xargs kill -9 2>/dev/null && echo "Killed old process on port 8002." || true

# Compile
echo ""
echo "Compiling Server..."
javac -cp ".:lib/sqlite-jdbc-3.42.0.0.jar:lib/Java-WebSocket-1.5.4.jar:lib/slf4j-api-1.7.36.jar" Server/Main.java

if [ $? -ne 0 ]; then
    echo "❌ Compilation FAILED!"
    exit 1
fi
echo "✅ Compilation Success."

# Run (auto-open browser after a short delay)
echo ""
echo "Starting Server..."
(sleep 2 && open "http://localhost:8001/Frontend/index.html") &
java -cp ".:lib/sqlite-jdbc-3.42.0.0.jar:lib/Java-WebSocket-1.5.4.jar:lib/slf4j-api-1.7.36.jar:lib/slf4j-simple-1.7.36.jar" Server.Main
