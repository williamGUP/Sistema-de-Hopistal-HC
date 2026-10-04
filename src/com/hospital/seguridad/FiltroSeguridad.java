package com.hospital.seguridad;

import com.hospital.model.Usuario;
import com.hospital.template.TemplateEngine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro de seguridad que envuelve al Router. Se ejecuta ANTES que cualquier
 * controlador y es el unico punto donde se decide si una peticion puede
 * atenderse. Responsabilidades:
 *
 *  1. Anadir cabeceras de seguridad a todas las respuestas.
 *  2. Resolver la sesion a partir de la cookie HttpOnly HOSPITAL_SID.
 *  3. Redirigir a /login las peticiones que requieren sesion y no la tienen.
 *  4. Validar el origen de las peticiones POST (proteccion CSRF).
 *  5. Comprobar el permiso exigido por la ruta segun {@link PoliticaPermisos}.
 *  6. Registrar en la bitacora los accesos, las escrituras y las denegaciones.
 *
 * Se implementa como un HttpHandler aparte (y no dentro del Router) para
 * proteger todas las rutas existentes sin modificar ni una linea del codigo
 * previo del proyecto.
 */
public final class FiltroSeguridad implements HttpHandler {

    private static final String PLANTILLA_ERROR = "acceso-restringido";

    private final HttpHandler siguiente;
    private final TemplateEngine motor;

    public FiltroSeguridad(HttpHandler siguiente, TemplateEngine motor) {
        this.siguiente = siguiente;
        this.motor = motor;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod();
        String ruta = exchange.getRequestURI().getPath();
        boolean esEstatico = ruta.startsWith("/static/");

        aplicarCabeceras(exchange, esEstatico);

        // Las hojas de estilo e imagenes son publicas: sin ellas la pantalla de
        // acceso se veria sin diseno.
        if (esEstatico) {
            try {
                siguiente.handle(exchange);
            } finally {
                ContextoActual.limpiar();
            }
            return;
        }

        String ip = Cookies.ip(exchange);
        String token = Cookies.leer(exchange, Cookies.COOKIE_SESION);
        Sesion sesion = SesionStore.getInstance().obtener(token);
        ContextoActual.establecer(sesion);

        try {
            // Sesion caducada o cookie antigua: se limpia en el navegador.
            if (sesion == null && token != null && !token.isEmpty()) {
                Cookies.borrarCookieSesion(exchange);
            }

            PoliticaPermisos.Regla regla = PoliticaPermisos.regla(metodo, ruta);

            // 1) Rutas publicas: /login y friends.
            if (regla != null && regla.publica()) {
                siguiente.handle(exchange);
                return;
            }

            // 2) Sin sesion: al login.
            if (sesion == null) {
                Auditoria.registrar(null, null, null, ip, metodo, ruta, "ACCESO", Auditoria.DENEGADO,
                        "Peticion sin sesion iniciada");
                redirigirALogin(exchange, metodo, ruta);
                return;
            }

            // El usuario pudo ser desactivado o cambiar de rol con la sesion
            // abierta, por eso se relee siempre desde la base de datos.
            Usuario usuario = refrescar(sesion.usuario());
            if (usuario == null) {
                SesionStore.getInstance().destruir(sesion.token());
                Cookies.borrarCookieSesion(exchange);
                ContextoActual.limpiar();
                Auditoria.registrar(null, sesion.usuario() == null ? null : sesion.usuario().getUsername(),
                        null, ip, metodo, ruta, "ACCESO", Auditoria.DENEGADO,
                        "Cuenta desactivada durante la sesion");
                redirigirALogin(exchange, metodo, ruta);
                return;
            }
            sesion.actualizarUsuario(usuario);

            // 3) Origen de confianza en los formularios (CSRF).
            if ("POST".equalsIgnoreCase(metodo) && !"/login".equals(ruta)) {
                String problema = Cookies.validarOrigen(exchange);
                if (problema != null) {
                    Auditoria.registrar(usuario.getId(), usuario.getUsername(), usuario.getRol(), ip,
                            metodo, ruta, "CSRF", Auditoria.DENEGADO, "Origen no confiable: " + problema);
                    responderError(exchange, 403, usuario, ruta, "csrf",
                            "No pudimos verificar esta peticion",
                            "Por seguridad el sistema solo acepta formularios enviados desde esta misma pagina. "
                                    + "Vuelve a cargar la pantalla e intentalo de nuevo; si el problema continua, "
                                    + "cierra sesion y vuelve a entrar.");
                    return;
                }
            }

            // 4) Permiso exigido por la ruta.
            Permiso exigido = regla == null ? null : regla.permiso();
            if (exigido != null && !ContextoActual.puede(exigido)) {
                Auditoria.registrar(usuario.getId(), usuario.getUsername(), usuario.getRol(), ip,
                        metodo, ruta, exigido.clave(), Auditoria.DENEGADO,
                        "Rol sin el permiso requerido");
                responderError(exchange, 403, usuario, ruta, "permiso",
                        "Tu rol no tiene acceso a esta accion",
                        "Esta pantalla forma parte del modulo que exige el permiso "
                                + exigencia(exigido) + ", que no forma parte de tu rol.");
                return;
            }

            // 5) Peticion permitida: la atiende el router.
            // Las escrituras quedan registradas en la bitacora. Las rutas sin
            // permiso propio (logout, mi cuenta) las registra su controlador
            // con una accion mas precisa, asi que aqui se evitan duplicados.
            siguiente.handle(exchange);
            if (exigido != null && !"GET".equalsIgnoreCase(metodo) && !"HEAD".equalsIgnoreCase(metodo)) {
                Auditoria.registrar(usuario.getId(), usuario.getUsername(), usuario.getRol(), ip,
                        metodo, ruta, exigido.clave(), Auditoria.OK, null);
            }
        } finally {
            ContextoActual.limpiar();
        }
    }

    /** Relee el usuario desde la base de datos; null si fue eliminado o desactivado. */
    private Usuario refrescar(Usuario enMemoria) {
        if (enMemoria == null || enMemoria.getId() == null) return null;
        try {
            return UsuariosDao.porId(enMemoria.getId()).filter(Usuario::isActivo).orElse(null);
        } catch (Exception e) {
            // Si la base de datos falla no se le cierra la sesion al usuario.
            return enMemoria.isActivo() ? enMemoria : null;
        }
    }

    private static String exigencia(Permiso p) {
        return p == null ? "requerido" : "\"" + p.clave() + "\"";
    }

    // ---------------- Respuestas ----------------

    private void redirigirALogin(HttpExchange exchange, String metodo, String ruta) throws IOException {
        if ("GET".equalsIgnoreCase(metodo) || "HEAD".equalsIgnoreCase(metodo)) {
            StringBuilder destino = new StringBuilder("/login?motivo=sesion-requerida");
            if (!"/".equals(ruta)) {
                destino.append("&siguiente=").append(URLEncoder.encode(ruta, StandardCharsets.UTF_8));
            }
            exchange.getResponseHeaders().set("Location", destino.toString());
            exchange.sendResponseHeaders(302, -1);
        } else {
            exchange.sendResponseHeaders(401, -1);
        }
    }

    /** Renderiza la pantalla de acceso restringido con el contexto de seguridad del usuario. */
    private void responderError(HttpExchange exchange, int estado, Usuario usuario, String ruta,
                                String motivo, String titulo, String mensaje) throws IOException {
        Map<String, Object> data = new HashMap<>();
        MotorConSesion.inyectar(data);
        Rol rol = Rol.desdeClave(usuario == null ? null : usuario.getRol());

        data.put("pageTitle", titulo);
        data.put("motivo", motivo);
        data.put("tituloAviso", titulo);
        data.put("mensajeAviso", mensaje);
        data.put("rutaIntentada", ruta);
        data.put("rolEtiquetaUsuario", rol == null ? "" : rol.etiqueta());
        data.put("rolDescripcionUsuario", rol == null ? "" : rol.descripcion());
        data.put("rolesSugeridos", rolesSugeridos(motivo, rol));

        byte[] bytes = motor.render(PLANTILLA_ERROR, data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(estado, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Para quien si tiene acceso: el propio admin puede consultar la bitacora. */
    private String rolesSugeridos(String motivo, Rol rolActual) {
        if (!"permiso".equals(motivo)) return "";
        if (rolActual != null && rolActual.esAdmin()) return "";
        StringBuilder sb = new StringBuilder();
        for (Rol r : Rol.values()) {
            if (r == rolActual) continue;
            sb.append("<li><strong>").append(Cookies.escapar(r.etiqueta()))
              .append("</strong><span>").append(Cookies.escapar(r.descripcion()))
              .append("</span></li>");
        }
        return sb.toString();
    }

    // ---------------- Cabeceras de seguridad ----------------

    /**
     * Cabeceras aplicadas a todas las respuestas: proteccion frente a inyeccion
     * de scripts (CSP), robo de credenciales en formularios incrustados
     * (frame-ancestors), sniffing de tipos (nosniff), filtracion de URLs
     * (Referrer-Policy) y cacheo en disco de paginas con datos clinicos
     * (no-store), algo que un ordenador compartido no debe permitir.
     */
    private void aplicarCabeceras(HttpExchange exchange, boolean esEstatico) {
        var h = exchange.getResponseHeaders();
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Referrer-Policy", "same-origin");
        h.set("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; "
                        + "form-action 'self'; frame-ancestors 'none'; base-uri 'self'; object-src 'none'");
        h.set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        if (!esEstatico) {
            h.set("Cache-Control", "no-store, no-cache, must-revalidate");
            h.set("Pragma", "no-cache");
            h.set("Expires", "0");
        } else {
            // Las hojas de estilo y scripts se releen siempre: evita que el
            // navegador siga mostrando una version antigua del CSS tras una
            // actualizacion del sistema.
            h.set("Cache-Control", "no-cache");
        }
    }
}