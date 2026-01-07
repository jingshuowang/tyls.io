package Server;

import java.io.*;
import java.net.*;

public class Main {
    private static final int CHUNK_SIZE = 16;
    // Cache for preloaded chunks
    private static final java.util.Map<String, String> chunkCache = new java.util.HashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Tyls.io");

        // Preload chunks (User Request: 2^20 Chunks)
        System.out.println("Loading World...");
        preloadChunks(512);
        System.out.println(chunkCache.size() + "/1048576");

        try (ServerSocket server = new ServerSocket(25565)) {
            while (true) {
                Socket client = server.accept();
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    // Handles Client Requests: HTTP GET
    private static void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStream out = socket.getOutputStream()) {
            String line = in.readLine();
            if (line == null)
                return;

            // Log Request
            // System.out.println("Req: " + line);

            // Parse: GET /chunk?x=5&y=10 HTTP/1.1
            if (line.startsWith("GET /chunk")) {
                // Extract params
                int xIndex = line.indexOf("x=");
                int yIndex = line.indexOf("y=");
                if (xIndex != -1 && yIndex != -1) {
                    int endX = line.indexOf("&", xIndex);
                    int endY = line.indexOf(" ", yIndex);
                    if (endX != -1 && endY != -1) {
                        String xStr = line.substring(xIndex + 2, endX);
                        String yStr = line.substring(yIndex + 2, endY);

                        try {
                            int cx = Integer.parseInt(xStr);
                            int cy = Integer.parseInt(yStr);

                            String data = getOrGenerateChunk(cx, cy);
                            sendHttpResponse(out, 200, data);
                            return;
                        } catch (Exception e) {
                        }
                    }
                }
                sendHttpResponse(out, 400, "Bad Request");
            } else if (line.startsWith("OPTIONS")) {
                // Preflight for Private Network Access
                sendHttpResponse(out, 200, "");
            } else if (line.startsWith("GET /status")) {
                sendHttpResponse(out, 200, "{\"chunks\": \"Infinite\", \"status\": \"Online\"}");
            } else {
                sendHttpResponse(out, 404, "Not Found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendHttpResponse(OutputStream out, int code, String body) throws IOException {
        String status = code == 200 ? "OK" : "Error";
        String response = "HTTP/1.1 " + code + " " + status + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, OPTIONS\r\n" + // Allow Methods
                "Access-Control-Allow-Private-Network: true\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Access-Control-Max-Age: 86400\r\n" + // Cache Preflight for 24h
                "Connection: close\r\n" + // Force Socket Close (Fixes Hangs)
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes());
    }

    private static String getOrGenerateChunk(int cx, int cy) {
        String key = cx + "," + cy;
        if (chunkCache.containsKey(key)) {
            return chunkCache.get(key);
        }
        // Fallback for outside preloaded area
        return generateChunkData(cx, cy);
    }

    private static void preloadChunks(int radius) {
        // EXACTLY 2^20 chunks = 1024 * 1024
        // Loop from -512 to 511 (Total 1024)
        for (int y = -radius; y < radius; y++) {
            for (int x = -radius; x < radius; x++) {
                String data = generateChunkData(x, y);
                chunkCache.put(x + "," + y, data);
            }
        }
    }

    // Generate Chunk Data (0s and 1s) using simple Noise
    private static String generateChunkData(int cx, int cy) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                double worldX = cx * CHUNK_SIZE + x;
                double worldY = cy * CHUNK_SIZE + y;
                // Simple Perlin-ish Noise
                double noise = Math.sin(worldX * 0.1) * Math.cos(worldY * 0.1)
                        + Math.sin(worldX * 0.03 + worldY * 0.03) * 0.5;

                int tile = noise > 0.2 ? 1 : 0; // 1=Grass, 0=Dirt
                sb.append(tile);
            }
        }
        return sb.toString();
    }

}
