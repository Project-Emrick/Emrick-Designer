package org.emrick.project;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;

public class RSSIFileUploaderHandler implements HttpHandler {
     final Path pathToStoreData;

    public RSSIFileUploaderHandler(String pathToStoreData) {
        this.pathToStoreData = Paths.get(pathToStoreData);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            return;
        }

        // Step 1: Read incoming data into a string
        String body;
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            body = reader.lines().reduce("", (acc, line) -> acc + line + "\n").trim();
        }

        // Step 2: Parse lines and extract Board ID
        String[] lines = body.split("\n");
        if (lines.length == 0 || !lines[0].startsWith("Board ID: ")) {
            System.out.println("Missing or invalid Board ID line.");
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().close();
            return;
        }

        String boardId = lines[0].substring("Board ID: ".length()).trim();
        String filename = "Board_" + boardId + ".txt";

        // Step 3: Prepare directory and file path
        Path saveDir = pathToStoreData.resolve("raw_data");
        Files.createDirectories(saveDir);  // Create raw_data/ folder if it doesn't exist
        Path savePath = saveDir.resolve(filename);

        // Step 4: Write each data line (packet#, rssi) to the file
        try (BufferedWriter writer = Files.newBufferedWriter(savePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (int i = 1; i < lines.length; i++) {
                writer.write(lines[i]);
                writer.newLine();
            }
        }

        // Step 5: Logging
        System.out.println("Received data from Board ID: " + boardId);
        for (int i = 1; i < lines.length; i++) {
            System.out.println("  → " + lines[i]);
        }

        // Step 6: Send confirmation response
        String response = "Saved data to " + savePath.toString();
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
