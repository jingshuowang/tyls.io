package Server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Random;

public class Main {
    private static final int CHUNK_SIZE = 16;
    private static final String DB_URL = "jdbc:sqlite:world.db";
    private static final int WORLD_RADIUS_CHUNKS = 512; // 1024x1024 chunks total (~1M)

    // Async Progress Tracking
    private static volatile boolean isWorldReady = false;
    private static volatile int genProgress = 0;

    public static void main(String[] args) {
        // Initialize Database
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found. Ensure dbdriver.jar is in classpath.");
            e.printStackTrace();
            return;
        }

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
                genProgress = 100;
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
                    genProgress = 100;

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
            int savedCount = 0;

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                conn.setAutoCommit(false);

                // 1. Save Player Position
                // Expected JSON: {"player":{"x":123,"y":456}, "chunks":[...]}
                int playerIdx = body.indexOf("\"player\":");
                if (playerIdx != -1) {
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT OR REPLACE INTO metadata(key, value) VALUES(?, ?)")) {

                        // Simple parse for x and y
                        // Look for "x":123
                        int xStart = body.indexOf("\"x\":", playerIdx) + 4;
                        int xEnd = body.indexOf(",", xStart);
                        // Handle case where it might be "x": 123 (space)
                        while (body.charAt(xEnd) == ' ' || body.charAt(xEnd) == '}')
                            xEnd--; // cleanup logic?
                        // Simpler: Just parse until non-digit/dot? JSON is strict.
                        // Let's rely on comma or closing brace
                        int yStart = body.indexOf("\"y\":", playerIdx) + 4;
                        int yEnd = body.indexOf("}", yStart);

                        // Re-eval parsing strategy:
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
                        int keyStart = body.indexOf("\"", idx + 6) + 1;
                        int keyEnd = body.indexOf("\"", keyStart);
                        String key = body.substring(keyStart, keyEnd);

                        // Extract data
                        int dataIdx = body.indexOf("\"data\":", keyEnd);
                        int dataStart = body.indexOf("\"", dataIdx + 7) + 1;
                        int dataEnd = body.indexOf("\"", dataStart);
                        String data = body.substring(dataStart, dataEnd);

                        // Unescape \\n to \n for storage - REMOVED to keep consistency with PreGen
                        // data = data.replace("\\n", "\n");

                        pstmt.setString(1, key);
                        pstmt.setString(2, data);
                        pstmt.addBatch();
                        savedCount++;

                        idx = dataEnd;
                    }

                    pstmt.executeBatch();
                    conn.commit();
                }
            }

            System.out.println("Saved " + savedCount + " chunks + player data");
            sendResponse(out, 200, "{\"saved\":" + savedCount + "}", "application/json");

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(out, 500, "{\"error\":\"Save failed\"}", "application/json");
        }
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

        // Use user requested "Spiral" for spawn?
        // Actually, let's just use Perlin for everything, but maybe clear the center?
        double distFromCenter = Math.sqrt(cx * cx + cy * cy);
        if (distFromCenter < 2) {
            // Safe Spawn (Empty)
            return chunk; // All 0
        }

        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                double worldX = cx * CHUNK_SIZE + x;
                double worldY = cy * CHUNK_SIZE + y;

                // Scale controls zoom. 0.05 is "zoomed in", 0.1 is "noisy"
                // Using different scales for "octaves"
                double val = getNoise(worldX * 0.05, worldY * 0.05) * 1.0 +
                        getNoise(worldX * 0.01, worldY * 0.01) * 2.0;

                // Threshold
                chunk[y][x] = val > 0.2 ? 1 : 0;
            }
        }
        return chunk;
    }

    // Convert 2D int array to "1010\n0101" String
    private static String chunkToString(int[][] chunk) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < chunk.length; r++) {
            for (int c = 0; c < chunk[r].length; c++) {
                sb.append(chunk[r][c]);
            }
            if (r < chunk.length - 1)
                sb.append("\\n"); // Use literal \n for JSON string safety
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
