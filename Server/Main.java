package Server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class Main {
    private static final int CHUNK_SIZE = 16;
    private static final String DB_URL = "jdbc:sqlite:world.db";
    private static final int WORLD_RADIUS_CHUNKS = 512; // 1024x1024 chunks total (~1M)
    private static final int HTTP_PORT = 8001;
    private static final int WS_PORT = 8002;

    // Async Progress Tracking
    private static volatile boolean worldReady = false; // Renamed from isWorldReady as requested

    // STatic reference for broadcasting
    private static GameWebSocketServer serverInstance;

    // WebSocket Server Inner Class
    public static class GameWebSocketServer extends WebSocketServer {
        public GameWebSocketServer(int port) {
            super(new InetSocketAddress(port));
            serverInstance = this;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            if (!worldReady) {
                conn.close(1013, "Server is generating world... please wait.");
                return;
            }
            System.out.println("New connection: " + conn.getRemoteSocketAddress());

            // Send initial world dimension/metadata if needed
            // conn.send("{\"type\":\"config\",\"worldSize\":" + (WORLD_RADIUS_CHUNKS * 2) +
            // "}");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            try {
                // System.out.println("Received: " + message); // Debug log

                // Simple generic JSON parser since we don't have a library
                // We look for "type":"value"
                String type = extractJsonString(message, "type");

                if ("pos".equals(type)) {
                    // Broadcast to others
                    broadcast(message);
                } else if ("getChunk".equals(type)) {
                    // Handle Chunk Request: {"type":"getChunk", "key":"0,0", "x":0, "y":0}
                    String key = extractJsonString(message, "key");
                    if (key == null)
                        return;

                    // key is "x,y", split it
                    String[] parts = key.split(",");
                    int cx = Integer.parseInt(parts[0]);
                    int cy = Integer.parseInt(parts[1]);

                    String chunkData = getOrGenerateChunk(cx, cy);

                    // Send back: {"type":"chunk", "key":"...", "data":"..."}
                    // We construct JSON manually
                    String response = "{\"type\":\"chunk\", \"key\":\"" + key + "\", \"data\":\"" + chunkData + "\"}";
                    conn.send(response);
                } else if ("save".equals(type)) {
                    // Handle Save: {"type":"save", "player":{...}, "chunks":[...]}
                    try {
                        int count = saveDataInternal(message);
                        conn.send("{\"type\":\"saveAck\", \"count\":" + count + "}");
                    } catch (Exception e) {
                        e.printStackTrace();
                        conn.send("{\"type\":\"error\", \"message\":\"Save failed\"}");
                    }
                } else if ("setBlock".equals(type)) {
                    // Handle Block Update: {"type":"setBlock", "x":10, "y":20, "val":1}
                    String xStr = extractJsonString(message, "x");
                    String yStr = extractJsonString(message, "y");
                    String valStr = extractJsonString(message, "val");

                    if (xStr != null && yStr != null && valStr != null) {
                        try {
                            int gx = Integer.parseInt(xStr); // Global X
                            int gy = Integer.parseInt(yStr); // Global Y
                            int val = Integer.parseInt(valStr);

                            // Broadcast Immediately for responsiveness
                            broadcast(message);

                            // Persist to DB (ASync or blocking? Blocking for safety now)
                            // Refactored to a helper method
                            setBlock(gx, gy, val);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // Legacy support or unknown
                    // broadcast(message);
                }
            } catch (Exception e) {
                System.err.println("Error processing message: " + message);
                e.printStackTrace();
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            ex.printStackTrace();
        }

        @Override
        public void onStart() {
            System.out.println("WebSocket Server started on port: " + getPort());
        }
    }

    public static void main(String[] args) {
        // Initialize Database
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found. Ensure dbdriver.jar is in classpath.");
            e.printStackTrace();
            return;
        }

        // Start WebSocket Server
        GameWebSocketServer wsServer = new GameWebSocketServer(WS_PORT);
        wsServer.start();

        // Initialize Random Seed
        initWorldGen(12345);

        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement()) {

            // Enable WAL mode for better concurrency
            stmt.execute("PRAGMA journal_mode=WAL;");

            // Create Table
            String sql = "CREATE TABLE IF NOT EXISTS chunks (" +
                    "id TEXT PRIMARY KEY," +
                    "data TEXT NOT NULL" +
                    ")";
            stmt.execute(sql);

            // Create Metadata Table (Player Position, Seed, etc.)
            stmt.execute("CREATE TABLE IF NOT EXISTS metadata (key TEXT PRIMARY KEY, value TEXT)");

            // World Loading Check
            int count = getWorldChunkCount(conn);

            if (count == 0) {
                System.out.println("No world data found. Generating new world...");
                // FORCE SYNC GENERATION (Blocking)
                preGenerateWorld(DB_URL);
            } else {
                System.out.println("World loaded from database: " + count + " chunks");
                worldReady = true;
            }

            System.out.println("Database: Ready");
        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            return;
        }

        // Start HTTP Server for Static Files
        try {
            startHttpServer();
        } catch (IOException e) {
            System.err.println("Failed to start HTTP Server: " + e.getMessage());
            return;
        }

        // Keep Server Alive
        try {
            System.out.println("");
            System.out.println("========================================");
            System.out.println("  tyls.io Server Ready!");
            System.out.println("  HTTP:      http://localhost:" + HTTP_PORT);
            System.out.println("  WebSocket: ws://localhost:" + WS_PORT);
            System.out.println("  Game:      http://localhost:" + HTTP_PORT + "/Frontend/index.html");
            System.out.println("========================================");
            System.out.println("  Press Ctrl+C to stop.");

            System.out.println("  Press Ctrl+C to stop.");

            // SERVER TICK LOOP (20 TPS)
            long lastTime = System.nanoTime();
            final double ns = 1000000000.0 / 20.0;
            double delta = 0;
            long timer = System.currentTimeMillis();
            int updates = 0;
            // int frames = 0; // Removed unused variable

            while (true) {
                long now = System.nanoTime();
                delta += (now - lastTime) / ns;
                lastTime = now;

                while (delta >= 1) {
                    // Update Game Logic Here (if any)
                    // world.tick();
                    updates++;
                    delta--;
                }

                // Sleep to prevent CPU hogging
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (System.currentTimeMillis() - timer > 1000) {
                    timer += 1000;
                    System.out.println("TPS: " + updates);
                    updates = 0;
                }
            }

        } catch (Exception e) { // General Exception
            e.printStackTrace();
        }
    }

    // --- HTTP Static File Server ---
    private static void startHttpServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);

        // MIME type mapping
        final Map<String, String> MIME_TYPES = new HashMap<>();
        MIME_TYPES.put(".html", "text/html");
        MIME_TYPES.put(".js", "application/javascript");
        MIME_TYPES.put(".css", "text/css");
        MIME_TYPES.put(".json", "application/json");
        MIME_TYPES.put(".png", "image/png");
        MIME_TYPES.put(".jpg", "image/jpeg");
        MIME_TYPES.put(".gif", "image/gif");
        MIME_TYPES.put(".svg", "image/svg+xml");
        MIME_TYPES.put(".ttf", "font/ttf");
        MIME_TYPES.put(".woff", "font/woff");
        MIME_TYPES.put(".woff2", "font/woff2");
        MIME_TYPES.put(".wasm", "application/wasm");
        MIME_TYPES.put(".mp3", "audio/mpeg");
        MIME_TYPES.put(".ogg", "audio/ogg");
        MIME_TYPES.put(".wav", "audio/wav");
        MIME_TYPES.put(".ico", "image/x-icon");

        // Project root directory (where the server is launched from)
        final Path ROOT = Paths.get(".").toAbsolutePath().normalize();

        httpServer.createContext("/", exchange -> {
            String method = exchange.getRequestMethod();

            // Handle CORS preflight
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            String path = exchange.getRequestURI().getPath();

            // Default to index
            if (path.equals("/")) {
                path = "/Frontend/index.html";
            }

            // Security: Block path traversal
            if (path.contains("..")) {
                sendError(exchange, 403, "Forbidden");
                return;
            }

            // Security: Block access to server source and lib
            String lowerPath = path.toLowerCase();
            if (lowerPath.startsWith("/server/") || lowerPath.startsWith("/lib/") ||
                    lowerPath.startsWith("/.git")) {
                sendError(exchange, 403, "Forbidden");
                return;
            }

            // Resolve file
            Path filePath = ROOT.resolve(path.substring(1)).normalize();

            // Ensure resolved path is still within project root
            if (!filePath.startsWith(ROOT)) {
                sendError(exchange, 403, "Forbidden");
                return;
            }

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendError(exchange, 404, "Not Found: " + path);
                return;
            }

            // Determine MIME type
            String fileName = filePath.getFileName().toString();
            String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
            String mimeType = MIME_TYPES.getOrDefault(ext.toLowerCase(), "application/octet-stream");

            // Send response
            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().add("Content-Type", mimeType);
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache"); // Dev-friendly
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        });

        httpServer.setExecutor(null); // Default executor
        httpServer.start();
        System.out.println("HTTP Server started on port: " + HTTP_PORT);
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] resp = message.getBytes();
        exchange.sendResponseHeaders(code, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private static int getWorldChunkCount(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT count(*) FROM chunks")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static void preGenerateWorld(String dbUrl) {
        System.out.println("Beginning World Generation (" + (WORLD_RADIUS_CHUNKS * 2) + "x" + (WORLD_RADIUS_CHUNKS * 2)
                + " chunks)... Background Thread Started.");

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // WAL Mode on new connection
            try (Statement s = conn.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL;");
            }

            // ... Generation Logic ...
            long startTime = System.currentTimeMillis();

            String sql = "INSERT INTO chunks(id, data) VALUES(?, ?)";

            try {
                conn.setAutoCommit(false); // Begin Transaction

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    int total = (WORLD_RADIUS_CHUNKS * 2) * (WORLD_RADIUS_CHUNKS * 2);
                    int current = 0;

                    initWorldGen(12345); // Seed

                    for (int cy = -WORLD_RADIUS_CHUNKS; cy < WORLD_RADIUS_CHUNKS; cy++) {
                        for (int cx = -WORLD_RADIUS_CHUNKS; cx < WORLD_RADIUS_CHUNKS; cx++) {

                            String key = cx + "," + cy;
                            // Generate using Perlin
                            int[][] chunk = generateChunkPerlin(cx, cy);
                            String dataStr = chunkToString(chunk);

                            pstmt.setString(1, key);
                            pstmt.setString(2, dataStr);
                            pstmt.addBatch();

                            current++;

                            // Execute batch every 10,000 chunks
                            if (current % 10000 == 0) {
                                pstmt.executeBatch();
                                conn.commit();

                                // Console Progress
                                int p = (int) ((long) current * 100 / total);
                                System.out.print(
                                        "\rProgress: " + p + "% (" + current + "/" + total + ")");
                            }
                        }
                    }

                    // Final batch
                    pstmt.executeBatch();
                    conn.commit();
                    System.out.println("\rProgress: 100% - Done!");
                    worldReady = true;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Failed to generate world: " + e.getMessage());
            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("World Generation Complete in " + duration + "s");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Helpers (RESTORED) ---

    // Extracted logic for reuse in WebSocket
    private static int saveDataInternal(String body) throws SQLException {
        int savedCount = 0;

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            // 1. Save Player Position
            // Expected JSON: ... "player":{"x":123,"y":456} ...
            int playerIdx = body.indexOf("\"player\":");
            if (playerIdx != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT OR REPLACE INTO metadata(key, value) VALUES(?, ?)")) {

                    // "x":123.45, "y":...
                    String xStr = extractJsonValue(body, "\"x\":", playerIdx).trim();
                    String yStr = extractJsonValue(body, "\"y\":", playerIdx).trim();

                    pstmt.setString(1, "player_x");
                    pstmt.setString(2, xStr);
                    pstmt.addBatch();

                    pstmt.setString(1, "player_y");
                    pstmt.setString(2, yStr);
                    pstmt.addBatch();

                    pstmt.executeBatch();
                }
            }

            // 2. Save Chunks
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO chunks(id, data) VALUES(?, ?)")) {

                // Find all chunk entries
                int idx = body.indexOf("\"chunks\""); // Start searching after "chunks"
                if (idx == -1)
                    idx = 0;

                while ((idx = body.indexOf("\"key\":", idx)) != -1) {
                    // Extract key
                    String key = extractJsonString(body, "key", idx);

                    // We need to advance idx manually to find data relative to this key
                    // Let's look for "data" AFTER the key
                    int keyPosInStr = body.indexOf(key, idx); // Approximate position

                    int dataIdx = body.indexOf("\"data\":", keyPosInStr);
                    String data = extractJsonString(body, "data", dataIdx - 1); // allow some slack?

                    int keyStart = body.indexOf("\"", idx + 6) + 1;
                    int keyEnd = body.indexOf("\"", keyStart);

                    // re-parsing using the indices logic:
                    key = body.substring(keyStart, keyEnd);

                    int dIdx = body.indexOf("\"data\":", keyEnd);
                    int dStart = body.indexOf("\"", dIdx + 7) + 1;
                    int dEnd = body.indexOf("\"", dStart);
                    data = body.substring(dStart, dEnd);

                    pstmt.setString(1, key);
                    pstmt.setString(2, data);
                    pstmt.addBatch();
                    savedCount++;

                    idx = dEnd;
                }

                pstmt.executeBatch();
                conn.commit();
            }
        }
        return savedCount;
    }

    private static String extractJsonValue(String json, String key, int startIdx) {
        int k = json.indexOf(key, startIdx);
        if (k == -1)
            return "0";
        int valStart = k + key.length();
        int valEnd = valStart;
        while (valEnd < json.length()) {
            char c = json.charAt(valEnd);
            if (c == ',' || c == '}' || c == ']')
                break;
            valEnd++;
        }
        return json.substring(valStart, valEnd);
    }

    // New Helper for extracting string values "key":"value"
    private static String extractJsonString(String json, String key) {
        return extractJsonString(json, key, 0);
    }

    private static String extractJsonString(String json, String key, int startIdx) {
        // Look for "key":"
        String search = "\"" + key + "\":";
        int k = json.indexOf(search, startIdx);
        if (k == -1)
            return null;

        int valStart = json.indexOf("\"", k + search.length()) + 1;
        int valEnd = json.indexOf("\"", valStart);

        if (valStart == 0 || valEnd == -1)
            return null; // Not found

        return json.substring(valStart, valEnd);
    }

    // --- SQLite Logic ---

    private static String getOrGenerateChunk(int cx, int cy) {
        String key = cx + "," + cy;

        // 1. Try Select
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement("SELECT data FROM chunks WHERE id = ?")) {

            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("data");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. Generate (Fallback for Out of Bounds)
        initWorldGen(12345); // Ensure initialized if falling back
        int[][] chunk = generateChunkPerlin(cx, cy);
        String dataStr = chunkToString(chunk);

        // 3. Insert
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO chunks(id, data) VALUES(?, ?)")) {

            pstmt.setString(1, key);
            pstmt.setString(2, dataStr);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            // Duplicate key race condition possible
        }

        return dataStr;
    }

    // Helper to update a block in the database
    private static void setBlock(int gx, int gy, int val) {
        // Calculate Chunk ID
        int cx = Math.floorDiv(gx, CHUNK_SIZE);
        int cy = Math.floorDiv(gy, CHUNK_SIZE);
        String key = cx + "," + cy;

        // Calculate Local Coordinates (0-15)
        int lx = (gx % CHUNK_SIZE + CHUNK_SIZE) % CHUNK_SIZE;
        int ly = (gy % CHUNK_SIZE + CHUNK_SIZE) % CHUNK_SIZE;
        int idx = ly * CHUNK_SIZE + lx;

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sqlSelect = "SELECT data FROM chunks WHERE id = ?";
            String currentData = null;

            try (PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {
                pstmt.setString(1, key);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    currentData = rs.getString("data");
                }
            }

            if (currentData != null) {
                // Remove brackets if present (backward compatibility check)
                if (currentData.startsWith("[")) {
                    currentData = currentData.substring(1, currentData.length() - 1);
                }

                String[] parts = currentData.split(",");
                if (idx >= 0 && idx < parts.length) {
                    parts[idx] = String.valueOf(val);

                    // Reconstruct
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    for (int i = 0; i < parts.length; i++) {
                        sb.append(parts[i]);
                        if (i < parts.length - 1)
                            sb.append(",");
                    }
                    sb.append("]");
                    String newData = sb.toString();

                    // Save back
                    String sqlUpdate = "UPDATE chunks SET data = ? WHERE id = ?";
                    try (PreparedStatement pstmtUpd = conn.prepareStatement(sqlUpdate)) {
                        pstmtUpd.setString(1, newData);
                        pstmtUpd.setString(2, key); // Fixed typo from pstmtUp
                        pstmtUpd.executeUpdate();
                    }

                    // Broadcast Update to all clients
                    // {"type":"block", "x":1, "y":2, "val":3}
                    String updateMsg = String.format("{\"type\":\"block\",\"x\":%d,\"y\":%d,\"val\":%d}", gx, gy, val);
                    broadcast(updateMsg); // Helper method needed or access inner class
                }
            }

        } catch (SQLException e) {
            System.err.println("SetBlock DB Error: " + e.getMessage());
        }
    }

    // Static helper to broadcast
    public static void broadcast(String msg) {
        // This is a bit hacky because GameWebSocketServer is an inner class instance.
        // But we need to access the helper.
        // Ideally, we should have a static reference to the server instance.
        if (serverInstance != null) {
            serverInstance.broadcast(msg);
        }
    }

    private static int[][] generateChunkPerlin(int cx, int cy) {
        int[][] chunk = new int[CHUNK_SIZE][CHUNK_SIZE];

        // Safe spawn zone in center
        double distFromCenter = Math.sqrt(cx * cx + cy * cy);
        if (distFromCenter < 2) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    chunk[y][x] = 1; // Dirt (Safe Spawn - User Request: ID 1 is Dirt)
                }
            }
            return chunk;
        }

        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                // User Request: Density based on Perlin Noise (Low = Dense)
                // 0=Water, 1=Dirt, 2=Grass, 3=Sand

                // Generate a single noise value for "density"
                // Using previously tuned frequency (0.04) for visible variation in LOD
                double val = getNoise((cx * CHUNK_SIZE + x) * 0.04, (cy * CHUNK_SIZE + y) * 0.04);

                int blockID;
                if (val < -0.3) {
                    blockID = 0; // Low Perlin = Water (Densest)
                } else if (val < 0.1) {
                    blockID = 1; // Dirt
                } else if (val < 0.4) {
                    blockID = 2; // Grass
                } else {
                    blockID = 3; // High Perlin = Sand (Least Dense)
                }
                chunk[y][x] = blockID;
            }
        }
        return chunk;
    }

    // Convert 2D int array to JSON-like Array String "[1,0,2,...]"
    private static String chunkToString(int[][] chunk) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int r = 0; r < chunk.length; r++) {
            for (int c = 0; c < chunk[r].length; c++) {
                sb.append(chunk[r][c]);
                if (r != chunk.length - 1 || c != chunk[r].length - 1) {
                    sb.append(",");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    // --- Perlin Noise Logic (Embedded) ---
    private static final int[] P = new int[512];

    private static void initWorldGen(int seed) {
        int[] permutation = new int[256];
        Random r = new Random(seed);

        for (int i = 0; i < 256; i++)
            permutation[i] = i;
        // Shuffle
        for (int i = 0; i < 256; i++) {
            int j = r.nextInt(256);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }

        for (int i = 0; i < 512; i++) {
            P[i] = permutation[i % 256];
        }
    }

    private static double getNoise(double x, double y) {
        // Find unit cube that contains point
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        // Find relative x,y of point in cube
        x -= Math.floor(x);
        y -= Math.floor(y);

        // Compute fade curves for x,y
        double u = fade(x);
        double v = fade(y);

        // Hash coordinates of the 4 cube corners
        int A = P[X] + Y, AA = P[A], AB = P[A + 1];
        int B = P[X + 1] + Y, BA = P[B], BB = P[B + 1];

        // Hash coordinates (fixed for static context)
        // Note: P is static now.
        int aaa = P[AA];
        int aba = P[AB];
        int baa = P[BA];
        int bba = P[BB];

        // Add blended results from 4 corners
        return lerp(v, lerp(u, grad(aaa, x, y), grad(baa, x - 1, y)),
                lerp(u, grad(aba, x, y - 1), grad(bba, x - 1, y - 1)));
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        int h = hash & 15;
        @SuppressWarnings("unused")
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : 0);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
