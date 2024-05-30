package org.emrick.project;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GetHandler implements HttpHandler {
    private String pkt;
    public GetHandler(String pkt) {
        this.pkt = pkt;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        parseQuery(query, parameters);
        String response = "";
        String[] pkts = pkt.split("\n");
        for (int i = 0; i < pkts.length; i++) {
            if (pkts[i].contains("Strip_id: " + parameters.get("id") + ",")) {
                response += pkts[i] + "\n";
            }
        }
        exchange.sendResponseHeaders(200,response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.flush();
        os.close();
    }

    public void parseQuery(String query, Map<String, Object> parameters) {
        String[] paramsAndValues = query.split("&");
        for (String p : paramsAndValues) {
            String key = p.split("=")[0];
            String value = p.split("=")[1];
            parameters.put(key,value);
        }
    }
}
