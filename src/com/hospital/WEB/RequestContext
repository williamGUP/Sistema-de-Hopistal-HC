package com.hospital.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Envoltorio sobre HttpExchange: parámetros de ruta/query/formulario y helpers de respuesta. */
public class RequestContext {
    public final HttpExchange exchange;
    public final Map<String, String> pathParams = new HashMap<>();
    public final Map<String, String> queryParams = new HashMap<>();
    public final Map<String, String> formParams = new HashMap<>();

    public RequestContext(HttpExchange exchange) {
        this.exchange = exchange;
    }

    void parseQuery() {
        parseUrlEncoded(exchange.getRequestURI().getRawQuery(), queryParams);
    }

    void parseForm() throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        parseUrlEncoded(new String(body, StandardCharsets.UTF_8), formParams);
    }

    private void parseUrlEncoded(String s, Map<String, String> target) {
        if (s == null || s.isEmpty()) return;
        for (String pair : s.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String k = eq >= 0 ? pair.substring(0, eq) : pair;
            String v = eq >= 0 ? pair.substring(eq + 1) : "";
            target.put(decode(k), decode(v));
        }
    }

    private String decode(String s) {
        // application/x-www-form-urlencoded: '+' representa un espacio y %XX son
        // bytes UTF-8 codificados; URLDecoder.decode ya maneja ambos casos correctamente.
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    /** Busca primero en el formulario (POST), luego en la query string (GET), luego en la ruta. */
    public String param(String name) {
        if (formParams.containsKey(name)) return formParams.get(name);
        if (queryParams.containsKey(name)) return queryParams.get(name);
        return pathParams.get(name);
    }

    public String param(String name, String def) {
        String v = param(name);
        return (v == null || v.isBlank()) ? def : v;
    }

    public long pathLong(String name) {
        return Long.parseLong(pathParams.get(name));
    }

    public void redirect(String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    public void html(int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
