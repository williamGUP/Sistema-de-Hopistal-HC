package com.hospital.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Enrutador HTTP minimalista con soporte de parámetros de ruta tipo /pacientes/{id}. */
public class Router implements HttpHandler {

    @FunctionalInterface
    public interface Handler {
        void handle(RequestContext ctx) throws Exception;
    }

    private static class Route {
        String method;
        Pattern pattern;
        List<String> paramNames;
        Handler handler;
    }

    private final List<Route> routes = new ArrayList<>();
    private final Path staticDir;

    public Router(Path staticDir) {
        this.staticDir = staticDir;
    }

    public void get(String path, Handler h) { add("GET", path, h); }
    public void post(String path, Handler h) { add("POST", path, h); }

    private void add(String method, String path, Handler h) {
        List<String> names = new ArrayList<>();
        StringBuilder regex = new StringBuilder();
        for (String segment : path.split("/")) {
            if (segment.isEmpty()) continue;
            regex.append('/');
            if (segment.startsWith("{") && segment.endsWith("}")) {
                names.add(segment.substring(1, segment.length() - 1));
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(segment));
            }
        }
        if (regex.length() == 0) regex.append('/');
        Route r = new Route();
        r.method = method;
        r.pattern = Pattern.compile("^" + regex + "/?$");
        r.paramNames = names;
        r.handler = h;
        routes.add(r);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (path.startsWith("/static/")) {
                serveStatic(exchange, path);
                return;
            }

            for (Route r : routes) {
                if (!r.method.equals(method)) continue;
                Matcher m = r.pattern.matcher(path);
                if (!m.matches()) continue;

                RequestContext ctx = new RequestContext(exchange);
                for (int i = 0; i < r.paramNames.size(); i++) {
                    ctx.pathParams.put(r.paramNames.get(i), m.group(i + 1));
                }
                ctx.parseQuery();
                if ("POST".equals(method)) ctx.parseForm();

                try {
                    r.handler.handle(ctx);
                } catch (Exception e) {
                    e.printStackTrace();
                    ctx.html(500, errorPage(e));
                }
                return;
            }

            new RequestContext(exchange).html(404,
                "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><title>404</title>" +
                "<link rel=\"stylesheet\" href=\"/static/css/estilo.css\"></head><body><main class=\"container\">" +
                "<h1>404 — Página no encontrada</h1><p><a href=\"/\">Volver al inicio</a></p></main></body></html>");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            exchange.close();
        }
    }

    private void serveStatic(HttpExchange exchange, String path) throws IOException {
        String rel = path.substring("/static/".length());
        Path file = staticDir.resolve(rel).normalize();
        if (!file.startsWith(staticDir) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", guessContentType(file.toString()));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String guessContentType(String name) {
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private String errorPage(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString()
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><title>Error</title>" +
               "<link rel=\"stylesheet\" href=\"/static/css/estilo.css\"></head><body><main class=\"container\">" +
               "<h1>Ocurrió un error interno</h1><p><a href=\"/\">Volver al inicio</a></p>" +
               "<pre style=\"white-space:pre-wrap;background:#fff;border:1px solid #ddd;padding:12px;border-radius:8px;font-size:12px;\">" +
               trace + "</pre></main></body></html>";
    }
}
