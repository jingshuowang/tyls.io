package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_SEED = 12345;
    private static final String SAVE_DIR = "world_data";

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Tyler.io Java Server...");

        // Ensure save directory
        new File(SAVE_DIR).mkdirs();

        // SIMULATE DATA LOADING (User Request: "Take a while, render trillions")
        // We will pre-generate a "Server Region"
        System.out.println("Pre-loading World Data (This might take a moment)...");
        // Radius 1000 = 2000x2000 chunks = 4 Million Chunks
        // This simulates a "Massive World" load.
        generateWorldRegion(-1000, -1000, 1000, 1000);
        System.out.println("World Generation Complete. Server Ready on Port 25565.");

        ServerSocket server = new ServerSocket(25565);
        while (true) {
            Socket client = server.accept();
            new Thread(() -> handleClient(client)).start();
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
            } else if (line.startsWith("GET /status")) {
                sendHttpResponse(out, 200, "{\"chunks\": 4000000, \"status\": \"Online\"}");
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
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes());
    }

    private static String getOrGenerateChunk(int cx, int cy) {
        File file = new File(SAVE_DIR, cx + "_" + cy + ".dat");
        if (file.exists()) {
            return readChunk(file);
        } else {
            String data = generateChunkData(cx, cy);
            writeChunk(file, data);
            return data;
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

    // "Pre-load" simulation
    private static void generateWorldRegion(int minX, int minY, int maxX, int maxY) {
        int total = (maxX - minX + 1) * (maxY - minY + 1);
        int current = 0;
        long startTime = System.currentTimeMillis();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                getOrGenerateChunk(x, y);
                current++;
                if (current % 100 == 0) {
                    System.out.printf("\rGenerating Chunks: %d / %d", current, total);
                }
            }
        }
        System.out.println();
    }

    private static void writeChunk(File file, String data) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readChunk(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine();
        } catch (IOException e) {
            return "";
        }
    }
}
