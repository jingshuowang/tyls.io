package Server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class Main {
    private static final int CHUNK_SIZE = 16;
    private static final String DB_URL = "jdbc:sqlite:world.db";
    private static final int WORLD_RADIUS_CHUNKS = 512; // 1024x1024 chunks total (~1M)

    // Async Progress Tracking
    private static volatile boolean isWorldReady = false;

    // WebSocket Server Inner Class
    public static class GameWebSocketServer extends WebSocketServer {
        public GameWebSocketServer(int port) {
            super(new InetSocketAddress(port));
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            System.out.println("New connection: " + conn.getRemoteSocketAddress());
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
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
            } else {
                // Legacy support or unknown
                // broadcast(message);
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
        GameWebSocketServer wsServer = new GameWebSocketServer(25566);
        wsServer.start();

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
                isWorldReady = true;

            }

            System.out.println("Database: Ready");
        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            return;
        }

        // Start HTTP Server
        try (ServerSocket server = new ServerSocket(25565)) {
            System.out.println("");
            System.out.println("========================================");
            System.out.println("  Server is running!");
            System.out.println("========================================");
            System.out.println("  Frontend URL: http://localhost:25565");
            System.out.println("========================================");
            System.out.println("");
            while (true) {
                Socket client = server.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                    isWorldReady = true;

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

    private static void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStream out = socket.getOutputStream()) {
            String line = in.readLine();
            if (line == null)
                return;

            // Read headers and get content length
            int contentLength = 0;
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.substring(15).trim());
                }
            }

            if (line.startsWith("OPTIONS")) {
                sendHeaders(out, 204, "text/plain", 0);
            } else if (line.startsWith("POST /save")) {
                System.out.println("Handling Save. Expecting " + contentLength + " bytes.");
                // Read body
                char[] bodyChars = new char[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = in.read(bodyChars, totalRead, contentLength - totalRead);
                    if (read == -1)
                        break; // End of stream
                    totalRead += read;
                }
                String body = new String(bodyChars);
                handleSaveRequest(body, out);
            } else if (line.startsWith("GET /chunk")) {
                if (!isWorldReady) {
                    sendResponse(out, 503, "World Generating", "text/plain");
                    return;
                }
                handleChunkRequest(line, out);
            } else if (line.startsWith("GET /player")) {
                handlePlayerRequest(out);
            } else if (line.startsWith("GET /status")) {
                // Always Online if we get here (Server blocks until ready)
                sendResponse(out, 200, "{\"status\":\"Online\"}", "application/json");
            } else {
                // STATIC FILE SERVING
                String path = line.split(" ")[1];
                if (path.equals("/"))
                    path = "/Frontend/index.html";

                if (path.contains("..")) {
                    sendResponse(out, 403, "Forbidden", "text/plain");
                    return;
                }

                String localPath = path.substring(1).replace("/", File.separator);
                File file = new File(localPath);

                if (file.exists() && !file.isDirectory()) {
                    String contentType = guessContentType(path);
                    sendFile(out, 200, file, contentType);
                } else {
                    sendResponse(out, 404, "Not Found", "text/plain");
                }
            }
        } catch (IOException e) {
            // Check for broken pipe or connection reset which is common
        }
    }

    private static void handleSaveRequest(String body, OutputStream out) throws IOException {
        try {
            int savedCount = saveDataInternal(body);
            System.out.println("Saved " + savedCount + " chunks + player data");
            sendResponse(out, 200, "{\"saved\":" + savedCount + "}", "application/json");

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(out, 500, "{\"error\":\"Save failed\"}", "application/json");
        }
    }

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
                    // Actually extractJsonString looks forward from startIdx.
                    // Let's use the robust manual parsing from before but wrapped?

                    // Reverting to the robust loop manually because extractJsonString
                    // might be too simple for nested structures if not careful.
                    // But actually, let's just copy the robust loop logic back here
                    // but cleaner if possible.
                    // For now, I will preserve the original logic structure which was working.

                    int keyStart = body.indexOf("\"", idx + 6) + 1;
                    int keyEnd = body.indexOf("\"", keyStart);
                    // String key = body.substring(keyStart, keyEnd); // Already have this from
                    // extract?
                    // Let's just stick to the indices logic which was proven (mostly).

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

    private static void handleChunkRequest(String request, OutputStream out) throws IOException {
        try {
            int xStart = request.indexOf("x=") + 2;
            int xEnd = request.indexOf("&", xStart);
            int yStart = request.indexOf("y=") + 2;
            int yEnd = request.indexOf(" ", yStart);

            int cx = Integer.parseInt(request.substring(xStart, xEnd));
            int cy = Integer.parseInt(request.substring(yStart, yEnd));

            String chunkData = getOrGenerateChunk(cx, cy);

            // ESCAPE raw newlines for JSON safety (Fixes Black Block / Invalid JSON)
            chunkData = chunkData.replace("\n", "\\n");

            // Send as JSON String Literal "1010..."
            sendResponse(out, 200, "\"" + chunkData + "\"", "application/json");
        } catch (Exception e) {
            // e.printStackTrace();
            sendResponse(out, 400, "Bad Request", "text/plain");
        }
    }

    private static void handlePlayerRequest(OutputStream out) throws IOException {
        double x = 0;
        double y = 0;
        boolean found = false;
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn
                        .prepareStatement("SELECT key, value FROM metadata WHERE key IN ('player_x', 'player_y')")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                if (rs.getString("key").equals("player_x"))
                    x = Double.parseDouble(rs.getString("value"));
                if (rs.getString("key").equals("player_y"))
                    y = Double.parseDouble(rs.getString("value"));
                found = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Return JSON
        if (found) {
            sendResponse(out, 200, "{\"x\":" + x + ",\"y\":" + y + "}", "application/json");
        } else {
            sendResponse(out, 404, "{\"error\":\"No saved player\"}", "application/json");
        }
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
        // If pre-gen is done, this mostly handles "infinite" beyond the border or
        // missing chunks
        // Reuse the same noise visual, but we might want to cache it?
        // For now, doing it on the fly for missing chunks is fine.
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

    private static int[][] generateChunkPerlin(int cx, int cy) {
        int[][] chunk = new int[CHUNK_SIZE][CHUNK_SIZE];

        // Safe spawn zone in center
        double distFromCenter = Math.sqrt(cx * cx + cy * cy);
        if (distFromCenter < 2) {
            return chunk; // All 0 (dirt only, safe spawn)
        }

        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                double worldX = cx * CHUNK_SIZE + x;
                double worldY = cy * CHUNK_SIZE + y;

                // Multi-octave Perlin for more natural terrain
                // Large scale features (continents/islands)
                double large = getNoise(worldX * 0.02, worldY * 0.02) * 1.0;
                // Medium scale features (hills/valleys)
                double medium = getNoise(worldX * 0.05, worldY * 0.05) * 0.5;
                // Small details
                double small = getNoise(worldX * 0.1, worldY * 0.1) * 0.25;

                double val = large + medium + small;

                // Threshold - grass if above 0.1
                chunk[y][x] = val > 0.1 ? 1 : 0;
            }
        }
        return chunk;
    }

    // Convert 2D int array to flat "101010..." String (256 chars, no newlines)
    private static String chunkToString(int[][] chunk) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < chunk.length; r++) {
            for (int c = 0; c < chunk[r].length; c++) {
                sb.append(chunk[r][c]);
            }
        }
        return sb.toString();
    }

    // ... (Helpers) ...

    private static String guessContentType(String path) {
        if (path.endsWith(".html"))
            return "text/html";
        if (path.endsWith(".js"))
            return "application/javascript";
        if (path.endsWith(".css"))
            return "text/css";
        if (path.endsWith(".png"))
            return "image/png";
        if (path.endsWith(".jpg"))
            return "image/jpeg";
        if (path.endsWith(".json"))
            return "application/json";
        if (path.endsWith(".ttf"))
            return "font/ttf";
        if (path.endsWith(".wasm"))
            return "application/wasm";
        if (path.endsWith(".db") || path.endsWith(".sqlite"))
            return "application/x-sqlite3";
        return "application/octet-stream";
    }

    private static void sendFile(OutputStream out, int code, File file, String contentType) throws IOException {
        long length = file.length();
        sendHeaders(out, code, contentType, length);
        Files.copy(file.toPath(), out);
        out.flush();
    }

    private static void sendResponse(OutputStream out, int code, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes();
        sendHeaders(out, code, contentType, bytes.length);
        out.write(bytes);
        out.flush();
    }

    private static void sendHeaders(OutputStream out, int code, String contentType, long length) throws IOException {
        String status = (code == 200 || code == 204) ? "OK" : "Error";
        String headers = "HTTP/1.1 " + code + " " + status + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Access-Control-Allow-Private-Network: true\r\n" +
                "Connection: close\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "\r\n";
        out.write(headers.getBytes());
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
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : 0);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
