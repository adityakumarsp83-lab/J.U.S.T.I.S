package com.justis.web;

import com.justis.analytics.BNSSEligibility;
import com.justis.analytics.CourtStatistics;
import com.justis.ds.CitationGraph.CitationAlert;
import com.justis.model.Case;
import com.justis.model.CitationEdge;
import com.justis.model.CivilCase;
import com.justis.model.CriminalCase;
import com.justis.model.WritCase;
import com.justis.service.DataWrangler;
import com.justis.service.DataWrangler.StatuteRecord;
import com.justis.service.SchedulerService;
import com.justis.service.SchedulerService.ScheduledSlot;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in High-Performance HTTP Server for J.U.S.T.I.S.
 *
 * <p>Serves the modern Glassmorphism Web Interface and exposes REST API endpoints
 * powered directly by the custom MaxHeap, Citation DAG, Trie, and Inverted Index.
 *
 * <p>Zero external web frameworks required (Pure JDK 17+ {@code com.sun.net.httpserver}).
 */
public class WebServer {

    private static final int DEFAULT_PORT = 8080;
    private static SchedulerService scheduler;
    private static String webRootDir;

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        // Locate web root directory
        File checkDir = new File("web");
        if (!checkDir.exists()) {
            checkDir = new File("J.U.S.T.I.S-main/web");
        }
        webRootDir = checkDir.getAbsolutePath();

        // Initialize backend domain models & data structures
        List<Case> cases = DataWrangler.loadCases("data/cases_master.csv");
        List<StatuteRecord> statutes = DataWrangler.loadStatutes("data/statutes_catalog.csv");
        List<CitationEdge> citations = DataWrangler.loadCitations("data/citations_dag.csv");
        scheduler = new SchedulerService(cases, citations, statutes);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Contexts
        server.createContext("/api/cases", new CasesHandler());
        server.createContext("/api/causelist", new CauseListHandler());
        server.createContext("/api/citations", new CitationsHandler());
        server.createContext("/api/citation-health", new CitationHealthHandler());
        server.createContext("/api/statutes", new StatutesHandler());
        server.createContext("/api/statute-autocomplete", new StatuteAutocompleteHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/undertrial", new UndertrialHandler());

        // Static File Serving
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║        ⚖️  J.U.S.T.I.S. WEB SERVER RUNNING SUCCESSFULLY        ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  URL:        http://localhost:%-32d ║%n", port);
        System.out.printf("║  Static Dir: %-49s ║%n", webRootDir);
        System.out.println("║  Status:     REST API & Web UI Online                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }

    // ─── HTTP Static Handler ──────────────────────────────────────

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File(webRootDir, path.startsWith("/") ? path.substring(1) : path);
            if (!file.exists() || file.isDirectory()) {
                file = new File(webRootDir, "index.html");
            }

            if (!file.exists()) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found");
                return;
            }

            String contentType = getMimeType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                }
            }
        }
    }

    // ─── API Handlers ─────────────────────────────────────────────

    static class CasesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("[");
            List<Case> cases = scheduler.getAllCases();
            for (int i = 0; i < cases.size(); i++) {
                if (i > 0) json.append(",");
                json.append(serializeCase(cases.get(i)));
            }
            json.append("]");
            sendJsonResponse(exchange, json.toString());
        }
    }

    static class CauseListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            int slots = 10;
            if (query.containsKey("slots")) {
                try { slots = Integer.parseInt(query.get("slots")); } catch (NumberFormatException ignored) {}
            }

            List<ScheduledSlot> list = scheduler.generateCauseList(slots);
            StringBuilder json = new StringBuilder("{\"slots\":").append(slots).append(",\"items\":[");
            for (int i = 0; i < list.size(); i++) {
                ScheduledSlot s = list.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                    .append("\"tier\":\"").append(escapeJson(s.getTier())).append("\",")
                    .append("\"priorityScore\":").append(String.format("%.2f", s.getPriorityScore())).append(",")
                    .append("\"justification\":\"").append(escapeJson(s.getJustification())).append("\",")
                    .append("\"case\":").append(serializeCase(s.getScheduledCase()))
                    .append("}");
            }
            json.append("]}");
            sendJsonResponse(exchange, json.toString());
        }
    }

    static class CitationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("{");
            json.append("\"edges\":[");
            List<CitationEdge> edges = scheduler.getAllEdges();
            for (int i = 0; i < edges.size(); i++) {
                CitationEdge e = edges.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                    .append("\"source\":").append(e.getSourceCaseId()).append(",")
                    .append("\"target\":").append(e.getTargetCaseId()).append(",")
                    .append("\"relation\":\"").append(e.getRelationType()).append("\"")
                    .append("}");
            }
            json.append("],\"overruledNodes\":[");
            List<Integer> overruled = new ArrayList<>(scheduler.getCitationGraph().getOverruledCases());
            for (int i = 0; i < overruled.size(); i++) {
                if (i > 0) json.append(",");
                json.append(overruled.get(i));
            }
            json.append("]}");
            sendJsonResponse(exchange, json.toString());
        }
    }

    static class CitationHealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            int id = 102;
            if (query.containsKey("id")) {
                try { id = Integer.parseInt(query.get("id")); } catch (NumberFormatException ignored) {}
            }

            List<CitationAlert> alerts = scheduler.checkPrecedentHealth(id);
            StringBuilder json = new StringBuilder("{\"caseId\":").append(id).append(",\"alerts\":[");
            for (int i = 0; i < alerts.size(); i++) {
                CitationAlert a = alerts.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                    .append("\"citingId\":").append(a.getCitingCaseId()).append(",")
                    .append("\"problematicId\":").append(a.getProblematicCaseId()).append(",")
                    .append("\"severity\":\"").append(a.getSeverity()).append("\",")
                    .append("\"chain\":\"").append(escapeJson(a.getChain())).append("\"")
                    .append("}");
            }
            json.append("]}");
            sendJsonResponse(exchange, json.toString());
        }
    }

    static class StatutesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("[");
            List<StatuteRecord> list = scheduler.getAllStatutes();
            for (int i = 0; i < list.size(); i++) {
                StatuteRecord r = list.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                    .append("\"code\":\"").append(escapeJson(r.getStatuteCode())).append("\",")
                    .append("\"actName\":\"").append(escapeJson(r.getActName())).append("\",")
                    .append("\"description\":\"").append(escapeJson(r.getDescription())).append("\",")
                    .append("\"maxSentenceYears\":").append(r.getMaxSentenceYears())
                    .append("}");
            }
            json.append("]");
            sendJsonResponse(exchange, json.toString());
        }
    }

    static class StatuteAutocompleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String prefix = query.getOrDefault("prefix", "");
            List<String> results = scheduler.autocompleteStatute(prefix);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escapeJson(results.get(i))).append("\"");
            }
            json.append("]");
            sendJsonResponse(exchange, json.toString());
        }
    }

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getQuery());
            String q = query.getOrDefault("q", "").trim();
            List<Case> results;
            if (q.isEmpty()) {
                results = scheduler.getAllCases();
            } else {
                String[] tokens = q.split("\\s+");
                if (tokens.length == 1) {
                    results = scheduler.searchCasesByKeyword(tokens[0]);
                } else {
                    results = scheduler.searchCasesByKeywords(tokens[0], tokens[1]);
                }
            }

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                if (i > 0) json.append(",");
                json.append(serializeCase(results.get(i)));
            }
            json.append("]");
            sendJsonResponse(exchange, json.toString());
        }
    }

    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Case> cases = scheduler.getAllCases();
            double median = CourtStatistics.medianCaseAge(cases);
            double mean = CourtStatistics.meanCaseAge(cases);
            double stdDev = CourtStatistics.standardDeviationCaseAge(cases);

            int criminalCount = 0, civilCount = 0, writCount = 0;
            int starvingCount = 0;
            for (Case c : cases) {
                switch (c.getCaseType()) {
                    case CRIMINAL -> criminalCount++;
                    case CIVIL -> civilCount++;
                    case WRIT -> writCount++;
                }
                if (c.isStarving()) starvingCount++;
            }

            String json = String.format("{\"totalCases\":%d,\"meanAge\":%.2f,\"medianAge\":%.2f,\"stdDev\":%.2f," +
                    "\"starvingCount\":%d,\"criminalCount\":%d,\"civilCount\":%d,\"writCount\":%d}",
                    cases.size(), mean, median, stdDev, starvingCount, criminalCount, civilCount, writCount);
            sendJsonResponse(exchange, json);
        }
    }

    static class UndertrialHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<BNSSEligibility.EligibilityResult> results = scheduler.getUndertrialEligibilityReport();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                BNSSEligibility.EligibilityResult r = results.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                    .append("\"caseId\":").append(r.getCaseId()).append(",")
                    .append("\"title\":\"").append(escapeJson(r.getTitle())).append("\",")
                    .append("\"statuteCode\":\"").append(escapeJson(r.getStatuteCode())).append("\",")
                    .append("\"custodyDays\":").append(r.getCustodyDays()).append(",")
                    .append("\"maxSentenceYears\":").append(r.getMaxSentenceYears()).append(",")
                    .append("\"thresholdDays\":").append(r.getThresholdDays()).append(",")
                    .append("\"status\":\"").append(r.getStatus()).append("\",")
                    .append("\"daysRemaining\":").append(r.getDaysRemaining())
                    .append("}");
            }
            json.append("]");
            sendJsonResponse(exchange, json.toString());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private static String serializeCase(Case c) {
        int custody = 0;
        double dispute = 0.0;
        if (c instanceof CriminalCase cc) custody = cc.getCustodyDays();
        else if (c instanceof WritCase wc) custody = wc.getCustodyDays();
        else if (c instanceof CivilCase civ) dispute = civ.getDisputeValueINR();

        StringBuilder citations = new StringBuilder("[");
        String[] cited = c.getCitedCaseIds();
        if (cited != null) {
            for (int i = 0; i < cited.length; i++) {
                if (i > 0) citations.append(",");
                citations.append("\"").append(escapeJson(cited[i])).append("\"");
            }
        }
        citations.append("]");

        return "{" +
                "\"caseId\":" + c.getCaseId() + "," +
                "\"caseType\":\"" + c.getCaseType() + "\"," +
                "\"title\":\"" + escapeJson(c.getTitle()) + "\"," +
                "\"filingYear\":" + c.getFilingYear() + "," +
                "\"caseAgeDays\":" + c.getCaseAgeDays() + "," +
                "\"urgencyFlag\":" + c.getUrgencyFlag() + "," +
                "\"statuteCode\":\"" + escapeJson(c.getStatuteCode()) + "\"," +
                "\"priorityScore\":" + String.format("%.2f", c.calculatePriorityScore()) + "," +
                "\"status\":\"" + c.getStatus() + "\"," +
                "\"isStarving\":" + c.isStarving() + "," +
                "\"custodyDays\":" + custody + "," +
                "\"disputeValueINR\":" + dispute + "," +
                "\"citedCaseIds\":" + citations +
                "}";
    }

    private static void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
    }

    private static void sendResponse(HttpExchange exchange, int status, String mime, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                           URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static String getMimeType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
