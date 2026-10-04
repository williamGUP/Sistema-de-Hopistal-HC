package com.hospital.seguridad;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utilidades de bajo nivel para trabajar con cookies y con el origen de la
 * peticion directamente sobre el HttpExchange.
 *
 * Se implementan aqui (y no en RequestContext) para no tener que modificar
 * ninguna clase existente del proyecto: basta con el campo publico
 * {@code exchange}, que RequestContext ya expone.
 */
public final class Cookies {

    public static final String COOKIE_SESION = "HOSPITAL_SID";

    private Cookies() {}

    // ---------------- Cookies ----------------

    /** Devuelve el valor de una cookie, o null si no viene. */
    public static String leer(HttpExchange exchange, String nombre) {
        Headers headers = exchange.getRequestHeaders();
        List<String> lineas = headers.get("Cookie");
        if (lineas == null) return null;
        for (String linea : lineas) {
            for (String par : linea.split(";")) {
                int eq = par.indexOf('=');
                if (eq <= 0) continue;
                String clave = par.substring(0, eq).trim();
                if (!clave.equals(nombre)) continue;
                String valor = par.substring(eq + 1).trim();
                if (valor.startsWith("\"") && valor.endsWith("\"") && valor.length() > 1) {
                    valor = valor.substring(1, valor.length() - 1);
                }
                return URLDecoder.decode(valor, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Emite una cookie de sesion:
     * HttpOnly (inaccesible desde JavaScript, evita robo por XSS),
     * SameSite=Strict (bloquea el envio en peticiones iniciadas desde otro
     * sitio), Path=/ y Secure cuando la peticion llego por HTTPS.
     */
    public static void emitirCookieSesion(HttpExchange exchange, String token, int maxAgeSegundos) {
        exchange.getResponseHeaders().add("Set-Cookie", construir(
                COOKIE_SESION, token, maxAgeSegundos, requestSeguro(exchange)));
    }

    /** Invalida la cookie de sesion en el navegador (al cerrar sesion). */
    public static void borrarCookieSesion(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Set-Cookie", construir(COOKIE_SESION, "", 0, false));
    }

    private static String construir(String nombre, String valor, int maxAgeSegundos, boolean seguro) {
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append('=').append(valor.isEmpty() ? "" : valor);
        sb.append("; Path=/");
        sb.append("; Max-Age=").append(Math.max(0, maxAgeSegundos));
        sb.append("; HttpOnly");
        sb.append("; SameSite=Strict");
        if (seguro) sb.append("; Secure");
        return sb.toString();
    }

    public static boolean requestSeguro(HttpExchange exchange) {
        String proto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
        if (proto != null && proto.toLowerCase(Locale.ROOT).contains("https")) return true;
        return "https".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("X-Forwarded-Ssl"));
    }

    // ---------------- Origen / CSRF ----------------

    /**
     * Indica si el cuerpo del formulario debe considerarse de confianza.
     *
     * En una aplicacion HTML clasica (formularios que hacen POST al mismo
     * origen) la proteccion CSRF mas robusta y sin depender de JavaScript es
     * exigir que la peticion venga del propio origen: los navegadores siempre
     * adjuntan la cabecera {@code Origin} (y, si no, {@code Referer}) en las
     * peticiones POST. Si el sitio atacante intenta enviar el formulario desde
     * su propio dominio, el origen no coincidira y la peticion se rechaza.
     *
     * @return null si todo correcto, o el motivo del rechazo.
     */
    public static String validarOrigen(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();

        String origen = headers.getFirst("Origin");
        if (origen != null && !origen.isBlank() && !"null".equalsIgnoreCase(origen)) {
            return coincideOrigen(exchange, origen) ? null : "origen";
        }

        String referer = headers.getFirst("Referer");
        if (referer != null && !referer.isBlank()) {
            return coincideOrigen(exchange, origenDe(referer)) ? null : "referer";
        }

        // Sin Origin ni Referer no se puede validar el origen: se rechaza.
        return "sin-origen";
    }

    private static boolean coincideOrigen(HttpExchange exchange, String origen) {
        String propio = hostPropio(exchange);
        String otro = origenDe(origen);
        // Solo se comparan esquema, host y puerto normalizados; el esquema se
        // acepta en http/https para que funcione tambien detras de un proxy.
        return propio.equalsIgnoreCase(otro);
    }

    private static String origenDe(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url.trim());
            String esquema = uri.getScheme() == null ? "http" : uri.getScheme().toLowerCase(Locale.ROOT);
            int puerto = uri.getPort();
            if (puerto == -1) {
                puerto = "https".equals(esquema) ? 443 : 80;
            }
            return esquema + "://" + uri.getHost() + ":" + puerto;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String hostPropio(HttpExchange exchange) {
        String esquema = Cookies.requestSeguro(exchange) ? "https" : "http";
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (host == null || host.isBlank()) {
            host = exchange.getLocalAddress().getHostString() + ":" + exchange.getLocalAddress().getPort();
        }
        return esquema + "://" + host.toLowerCase(Locale.ROOT);
    }

    // ---------------- Varios ----------------

    /** Direccion IP del cliente (sirve para la bitacora y el bloqueo por intentos). */
    public static String ip(HttpExchange exchange) {
        String reenviada = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        String ip;
        if (reenviada != null && !reenviada.isBlank()) {
            ip = reenviada.split(",")[0].trim();
        } else if (exchange.getRemoteAddress() == null) {
            ip = "desconocida";
        } else {
            ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        }
        // La loopback IPv6 se muestra como IPv4 para que la bitacora sea legible.
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) ip = "127.0.0.1";
        return ip;
    }

    /**
     * Nombre legible del navegador y del sistema operativo, para la lista de
     * sesiones activas (leer un User-Agent crudo no dice nada util al usuario).
     * Si no se reconoce, se recorta el valor original.
     */
    public static String navegador(HttpExchange exchange) {
        String ua = exchange.getRequestHeaders().getFirst("User-Agent");
        return navegadorDesdeUserAgent(ua);
    }

    public static String navegadorDesdeUserAgent(String ua) {
        if (ua == null || ua.isBlank()) return "Navegador desconocido";

        String navegador = "Navegador";
        if (ua.contains("Edg/") || ua.contains("Edge/")) navegador = "Microsoft Edge";
        else if (ua.contains("OPR/") || ua.contains("Opera")) navegador = "Opera";
        else if (ua.contains("Chrome/")) navegador = "Google Chrome";
        else if (ua.contains("Firefox/")) navegador = "Mozilla Firefox";
        else if (ua.contains("Safari/") && !ua.contains("Chrome")) navegador = "Safari";

        String sistema = "Sistema desconocido";
        if (ua.contains("Windows")) sistema = "Windows";
        else if (ua.contains("Mac OS X") || ua.contains("Macintosh")) sistema = "macOS";
        else if (ua.contains("Android")) sistema = "Android";
        else if (ua.contains("iPhone") || ua.contains("iPad")) sistema = "iOS";
        else if (ua.contains("Linux")) sistema = "Linux";

        String uaLimpio = navegador.equals("Navegador") ? recortar(ua, 40) : navegador;
        return uaLimpio + " · " + sistema;
    }

    public static String recortar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    /** Escapa texto para insertarlo en HTML dentro de un atributo. */
    public static String escapar(String texto) {
        return com.hospital.template.TemplateEngine.escapeHtml(texto);
    }
}