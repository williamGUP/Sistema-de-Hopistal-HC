package com.hospital.seguridad;

import com.hospital.model.Usuario;
import com.hospital.template.TemplateEngine;
import com.hospital.web.RequestContext;
import com.hospital.web.Router;
import com.hospital.web.Views;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador de acceso: inicio y cierre de sesion y gestion de la cuenta
 * propia. La plantilla de acceso se genera aqui porque no comparte el menu del
 * sistema (aun no hay sesion) y porque necesita informacion propia del
 * formulario de entrada.
 */
public final class ControladorAcceso {

    private final TemplateEngine motor;
    private final SesionStore sesiones = SesionStore.getInstance();

    public ControladorAcceso(TemplateEngine motor) {
        this.motor = motor;
    }

    public void register(Router router) {
        router.get("/login", this::formulario);
        router.post("/login", this::entrar);
        router.get("/logout", this::confirmarSalida);
        router.post("/logout", this::salir);
        router.get("/mi-cuenta", this::miCuenta);
        router.post("/mi-cuenta", this::cambiarContrasena);
    }

    // ---------------- Pantalla de acceso ----------------

    private void formulario(RequestContext ctx) throws Exception {
        Sesion sesion = ContextoActual.sesion();
        if (sesion != null) {
            ctx.redirect("/");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "Iniciar sesion");
        data.put("error", "");
        data.put("exito", "");
        data.put("username", "");
        data.put("motivo", ctx.param("motivo", ""));
        data.put("siguiente", siguienteSeguro(ctx.param("siguiente", "")));
        data.put("sugerenciaUsuario", "");
        data.put("cuentasDemo", cuentasDemo());
        data.put("hayCuentasDemo", true);

        if ("sesion-requerida".equals(data.get("motivo"))) {
            data.put("aviso", "Tu sesion seCerro o nunca inicio. Ingresa de nuevo para continuar.");
        }
        if ("sesion-cerrada".equals(data.get("motivo"))) {
            data.put("exito", "Cerraste sesion de forma segura. Gracias por usar Hospital HC.");
        }
        if ("contrasena-cambiada".equals(data.get("motivo"))) {
            data.put("exito", "Tu contrasena se actualizo correctamente.");
        }

        ctx.html(200, motor.render("acceso", data));
    }

    private void entrar(RequestContext ctx) throws Exception {
        String ip = Cookies.ip(ctx.exchange);
        String username = ctx.param("username", "").trim();
        String password = ctx.param("password", "");
        String siguiente = siguienteSeguro(ctx.param("siguiente", ""));

        Map<String, Object> data = baseAcceso(username, siguiente);
        data.put("error", "");
        data.put("exito", "");
        data.put("motivo", "");
        data.put("sugerenciaUsuario", "");

        if (username.isEmpty() || password.isEmpty()) {
            falloAcceso(ctx, data, ip, username, "Escribe tu usuario y tu contrasena.");
            return;
        }

        // Bloqueo por fuerza bruta: se cuentan por cuenta y tambien por IP, para
        // que ni un atacante automatizado ni un tercero puedan dejar sin acceso
        // a un usuario legitimo.
        int fallidos = Auditoria.intentosFallidos(username, ip);
        int fallidosIp = Auditoria.intentosFallidosPorIp(ip);

        if (fallidos >= Auditoria.MAX_INTENTOS) {
            long minutos = Auditoria.minutosParaDesbloquear(username, ip);
            Auditoria.registrar(null, username, null, ip, "POST", "/login", "BLOQUEO", Auditoria.DENEGADO,
                    "Cuenta bloqueada por intentos fallidos");
            data.put("error", "Esta cuenta quedo bloqueada temporalmente por intentos fallidos. "
                    + "Intenta de nuevo en " + Math.max(1, minutos)
                    + (minutos == 1 ? " minuto." : " minutos."));
            ctx.html(429, motor.render("acceso", data));
            return;
        }

        if (fallidosIp >= Auditoria.MAX_INTENTOS_POR_IP) {
            Auditoria.registrar(null, username, null, ip, "POST", "/login", "BLOQUEO", Auditoria.DENEGADO,
                    "IP bloqueada por demasiados intentos fallidos");
            data.put("error", "Desde este equipo se hicieron demasiados intentos de acceso. "
                    + "Espera " + Math.max(1, Auditoria.minutosParaDesbloquear(username, ip))
                    + " minutos antes de volver a intentar.");
            ctx.html(429, motor.render("acceso", data));
            return;
        }

        Optional<Usuario> encontrado = UsuariosDao.porUsername(username);
        boolean credencialesOk = encontrado.isPresent()
                && encontrado.get().isActivo()
                && Passwords.verificar(password, encontrado.get().getPassword());

        if (!credencialesOk) {
            // Mismo mensaje para usuario inexistente, contrasena incorrecta y
            // cuenta desactivada: no se revela que cuentas existen.
            Auditoria.registrar(encontrado.map(u -> u.getId()).orElse(null), username,
                    encontrado.map(Usuario::getRol).orElse(null), ip, "POST", "/login",
                    "LOGIN", Auditoria.FALLIDO,
                    encontrado.isPresent() && !encontrado.get().isActivo()
                            ? "Cuenta desactivada" : "Credenciales incorrectas");
            int restantes = Math.max(0, Auditoria.MAX_INTENTOS - (fallidos + 1));
            data.put("error", "Usuario o contrasena incorrectos."
                    + (restantes > 0 && restantes <= 3 ? " Te quedan " + restantes + " intento(s)." : ""));
            data.put("sugerenciaUsuario", existeComoSugerencia(username) ? username : "");
            ctx.html(401, motor.render("acceso", data));
            return;
        }

        Usuario usuario = encontrado.get();

        // Contrasena antigua en texto plano: se migra a hash en este momento.
        if (!Passwords.esHashValido(usuario.getPassword())) {
            UsuariosDao.actualizarContrasena(usuario.getId(), Passwords.hash(password));
            usuario.setPassword(Passwords.hash(password));
        }

        Sesion sesion = sesiones.crear(usuario, ip, Cookies.navegador(ctx.exchange));
        Cookies.emitirCookieSesion(ctx.exchange, sesion.token(), (int) Sesion.DURACION_MAXIMA.toSeconds());
        Auditoria.registrar(usuario.getId(), usuario.getUsername(), usuario.getRol(), ip, "POST", "/login",
                "LOGIN", Auditoria.OK, "Inicio de sesion correcto");

        System.out.println("Acceso concedido a " + usuario.getUsername() + " (" + usuario.getRol()
                + ") desde " + ip + " a las " + Instant.now());

        ctx.redirect(siguiente.isEmpty() ? "/" : siguiente);
    }

    /** Muestra el login otra vez con el error y registra el intento en la bitacora. */
    private void falloAcceso(RequestContext ctx, Map<String, Object> data, String ip,
                             String username, String mensaje) throws Exception {
        Auditoria.registrar(null, username, null, ip, "POST", "/login", "LOGIN", Auditoria.FALLIDO, mensaje);
        data.put("error", mensaje);
        ctx.html(401, motor.render("acceso", data));
    }

    /** Dice si el usuario existe, para poder sugerir el nombre correcto sin filtrar nada sensible. */
    private boolean existeComoSugerencia(String username) {
        try {
            return UsuariosDao.porUsername(username).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Solo se permite redirigir a rutas internas: evita el redireccionamiento
     * abierto (un enlace de phishing que devuelve al usuario a otra web tras
     * iniciar sesion).
     */
    static String siguienteSeguro(String valor) {
        if (valor == null || valor.isBlank()) return "";
        String v = valor.trim();
        if (!v.startsWith("/") || v.startsWith("//") || v.contains("\\") || v.contains(":")) return "";
        if (v.startsWith("/login") || v.startsWith("/logout")) return "";
        return v;
    }

    // ---------------- Cierre de sesion ----------------

    private void confirmarSalida(RequestContext ctx) throws Exception {
        Map<String, Object> data = new HashMap<>();
        MotorConSesion.inyectar(data);
        data.put("pageTitle", "Cerrar sesion");
        ctx.html(200, motor.render("salir", data));
    }

    private void salir(RequestContext ctx) throws Exception {
        Sesion sesion = ContextoActual.sesion();
        if (sesion != null) {
            String ip = Cookies.ip(ctx.exchange);
            Usuario u = sesion.usuario();
            sesiones.destruir(sesion.token());
            Cookies.borrarCookieSesion(ctx.exchange);
            Auditoria.registrar(u.getId(), u.getUsername(), u.getRol(), ip, "POST", "/logout",
                    "LOGOUT", Auditoria.CERRADA, "Cierre de sesion");
            System.out.println("Cierre de sesion de " + u.getUsername() + " desde " + ip);
        }
        ContextoActual.limpiar();
        ctx.redirect("/login?motivo=sesion-cerrada");
    }

    // ---------------- Mi cuenta ----------------

    private void miCuenta(RequestContext ctx) throws Exception {
        Map<String, Object> data = datosMiCuenta(ctx);
        data.put("error", "");
        ctx.html(200, motor.render("mi-cuenta", data));
    }

    private void cambiarContrasena(RequestContext ctx) throws Exception {
        Sesion sesion = ContextoActual.sesion();
        Usuario u = sesion.usuario();
        String actual = ctx.param("passwordActual", "");
        String nueva = ctx.param("passwordNueva", "");
        String repetir = ctx.param("passwordRepetir", "");

        Map<String, Object> data = datosMiCuenta(ctx);

        if (!Passwords.verificar(actual, u.getPassword())) {
            data.put("flashError", "La contrasena actual no es correcta.");
            ctx.html(401, motor.render("mi-cuenta", data));
            return;
        }
        if (!nueva.equals(repetir)) {
            data.put("flashError", "La contrasena nueva y su repeticion no coinciden.");
            ctx.html(400, motor.render("mi-cuenta", data));
            return;
        }
        String problema = Passwords.validar(nueva, u.getUsername(), u.getNombreCompleto());
        if (problema != null) {
            data.put("flashError", problema);
            ctx.html(400, motor.render("mi-cuenta", data));
            return;
        }
        if (Passwords.verificar(nueva, u.getPassword())) {
            data.put("flashError", "La contrasena nueva debe ser distinta de la actual.");
            ctx.html(400, motor.render("mi-cuenta", data));
            return;
        }

        UsuariosDao.actualizarContrasena(u.getId(), Passwords.hash(nueva));

        // Al cambiar la contrasena se invalidan las sesiones ya abiertas: si
        // alguien se colara en el equipo, pierde el acceso de inmediato.
        String ip = Cookies.ip(ctx.exchange);
        sesiones.cerrarSesionesAnteriores(u);
        Sesion nuevaSesion = sesiones.crear(u, ip, Cookies.navegador(ctx.exchange));
        Cookies.emitirCookieSesion(ctx.exchange, nuevaSesion.token(), (int) Sesion.DURACION_MAXIMA.toSeconds());
        ContextoActual.establecer(nuevaSesion);

        Auditoria.registrar(u.getId(), u.getUsername(), u.getRol(), ip, "POST", "/mi-cuenta",
                "CAMBIO_CONTRASENA", Auditoria.OK, "Cambio de contrasena propio");

        ctx.redirect("/mi-cuenta?ok=" + enc("Contrasena actualizada. Se cerraron tus otras sesiones."));
    }

    /** Datos comunes a las dos pantallas de "Mi cuenta". */
    private Map<String, Object> datosMiCuenta(RequestContext ctx) {
        Sesion sesion = ContextoActual.sesion();
        Usuario u = sesion.usuario();
        Rol rol = Rol.desdeClave(u.getRol());

        Map<String, Object> data = Views.baseData("Mi cuenta", "cuenta", ctx);
        MotorConSesion.inyectar(data);
        data.put("nombreCompleto", u.getNombreCompleto());
        data.put("username", u.getUsername());
        data.put("rolEtiquetaCuenta", rol == null ? "" : rol.etiqueta());
        data.put("rolDescripcionCuenta", rol == null ? "" : rol.descripcion());
        data.put("permisoActual", datosPermisos(rol));
        data.put("sesionesActivas", datosSesiones(sesion));
        data.put("sesionMinutos", sesion.minutosRestantes(Instant.now()));
        data.put("sesionMaximaMinutos", Sesion.INACTIVIDAD_MAXIMA.toMinutes());
        data.put("desdeIP", sesion.ip());
        data.put("desdeNavegador", Cookies.recortar(sesion.navegador(), 80));
        return data;
    }

    // ---------------- Datos para las plantillas ----------------

    private Map<String, Object> baseAcceso(String username, String siguiente) {
        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "Iniciar sesion");
        data.put("username", username);
        data.put("siguiente", siguiente);
        data.put("cuentasDemo", cuentasDemo());
        data.put("hayCuentasDemo", true);
        return data;
    }

    /** Tarjetas de las cuentas de demostracion para poder probar los cuatro roles. */
    private List<Map<String, Object>> cuentasDemo() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (String[] u : SeguridadEsquema.USUARIOS_DEMO) {
            Rol r = Rol.desdeClave(u[3]);
            Map<String, Object> m = new HashMap<>();
            m.put("username", u[0]);
            m.put("password", u[1]);
            m.put("nombre", u[2]);
            m.put("rol", r.etiqueta());
            m.put("descripcion", r.descripcion());
            m.put("rolCss", "rol-" + r.clave());
            lista.add(m);
        }
        return lista;
    }

    /** Lista de permisos del rol, agrupada por recurso, para la pantalla "Mi cuenta". */
    private List<Map<String, Object>> datosPermisos(Rol rol) {
        List<Map<String, Object>> lista = new ArrayList<>();
        if (rol == null) return lista;
        String familiaActual = "";
        List<String> acciones = new ArrayList<>();
        for (Permiso p : Permiso.values()) {
            boolean concedido = rol.concede(p);
            if (!p.familia().equals(familiaActual)) {
                if (!acciones.isEmpty()) {
                    lista.add(familia(familiaActual, acciones));
                }
                familiaActual = p.familia();
                acciones = new ArrayList<>();
            }
            acciones.add("<span class=\"" + (concedido ? "acc-si" : "acc-no") + "\">"
                    + (concedido ? "Si" : "No") + "</span> " + Cookies.escapar(p.etiqueta()));
        }
        if (!acciones.isEmpty()) lista.add(familia(familiaActual, acciones));
        return lista;
    }

    private Map<String, Object> familia(String clave, List<String> acciones) {
        Map<String, Object> m = new HashMap<>();
        m.put("familia", Permiso.porClave(clave + ".ver") != null
                ? Permiso.porClave(clave + ".ver").familiaLegible() : capitalizar(clave));
        m.put("acciones", String.join("<br>", acciones));
        return m;
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Sesiones abiertas del usuario, para poder detectar accesos ajenos. */
    private List<Map<String, Object>> datosSesiones(Sesion actual) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Sesion s : sesiones.sesionesDe(actual.idUsuario())) {
            Map<String, Object> m = new HashMap<>();
            m.put("esActual", s.token().equals(actual.token()));
            m.put("navegador", Cookies.recortar(s.navegador(), 60));
            m.put("ip", s.ip());
            m.put("inicio", s.creada().atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            m.put("minutos", s.minutosRestantes(Instant.now()));
            lista.add(m);
        }
        return lista;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}