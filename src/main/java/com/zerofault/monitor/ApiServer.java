package com.zerofault.monitor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ApiServer {

    private final MetricsStore store;
    private HttpServer server;

    public ApiServer(MetricsStore store) {
        this.store = store;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/metrics/latest", ex -> sendJson(ex, latestJson()));
        server.createContext("/metrics/history", ex -> sendJson(ex, historyJson(ex.getRequestURI())));
        server.createContext("/alerts", ex -> sendJson(ex, alertsJson()));
        server.createContext("/metrics/prometheus", ex -> sendText(ex, prometheusText()));

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(2));
        server.start();

        System.out.println("API server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --------------------------
    // JSON ENDPOINTS
    // --------------------------

    private String latestJson() {
        MetricsRecord r = store.getLatestOrNull();
        if (r == null) return "{}";

        return "{"
                + "\"timestamp\":\"" + r.timestamp() + "\","
                + "\"cpuNow\":" + r.cpuNow() + ","
                + "\"cpuAvg60\":" + r.cpuAvg60() + ","
                + "\"ramNow\":" + r.ramNow() + ","
                + "\"ramAvg60\":" + r.ramAvg60() + ","
                + "\"downKBps\":" + r.downKBps() + ","
                + "\"upKBps\":" + r.upKBps()
                + "}";
    }

    private String historyJson(URI uri) {
        int seconds = 60;

        String query = uri.getQuery();
        if (query != null) {
            for (String part : query.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && kv[0].equals("seconds")) {
                    try {
                        seconds = Math.max(1, Integer.parseInt(kv[1]));
                    } catch (Exception ignored) {}
                }
            }
        }

        List<MetricsRecord> list = store.getLatestN(seconds);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"count\":").append(list.size()).append(",\"items\":[");

        for (int i = 0; i < list.size(); i++) {
            MetricsRecord r = list.get(i);
            sb.append("{")
                    .append("\"timestamp\":\"").append(r.timestamp()).append("\",")
                    .append("\"cpuNow\":").append(r.cpuNow()).append(",")
                    .append("\"ramNow\":").append(r.ramNow()).append(",")
                    .append("\"downKBps\":").append(r.downKBps()).append(",")
                    .append("\"upKBps\":").append(r.upKBps())
                    .append("}");

            if (i < list.size() - 1) sb.append(",");
        }

        sb.append("]}");
        return sb.toString();
    }

    private String alertsJson() {
        List<String> alerts = store.getAlerts();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"count\":").append(alerts.size()).append(",\"items\":[");

        for (int i = 0; i < alerts.size(); i++) {
            sb.append("\"").append(escape(alerts.get(i))).append("\"");
            if (i < alerts.size() - 1) sb.append(",");
        }

        sb.append("]}");
        return sb.toString();
    }

    // --------------------------
    // PROMETHEUS ENDPOINT
    // --------------------------

    private String prometheusText() {
        MetricsRecord r = store.getLatestOrNull();
        if (r == null) return "# no data\n";

        return ""
                + "zerofault_cpu_now " + r.cpuNow() + "\n"
                + "zerofault_cpu_avg60 " + r.cpuAvg60() + "\n"
                + "zerofault_ram_now " + r.ramNow() + "\n"
                + "zerofault_ram_avg60 " + r.ramAvg60() + "\n"
                + "zerofault_net_down_kbps " + r.downKBps() + "\n"
                + "zerofault_net_up_kbps " + r.upKBps() + "\n";
    }

    // --------------------------
    // RESPONSE HELPERS
    // --------------------------

    private static void sendJson(HttpExchange ex, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange ex, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

