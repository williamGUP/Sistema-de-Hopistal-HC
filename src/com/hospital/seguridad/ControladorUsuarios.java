package com.hospital.seguridad;

import com.hospital.model.Usuario;
import com.hospital.template.TemplateEngine;
import com.hospital.web.RequestContext;
import com.hospital.web.Router;
import com.hospital.web.Views;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Administracion de usuarios: alta, edicion de rol, reactivacion y baja.
 * Solo el rol ADMIN_GENERAL tiene permisos de este modulo (ver PoliticaPermisos),
 * y aun asi el filtro vuelve a comprobar el permiso en cada peticion.
 *
 * Protecciones adicionales:
 *  - No se puede desactivar ni degradar al ultimo administrador general activo.
 *  - Un administrador no puede desactivar su propia cuenta (evita el bloqueo
 *    del sistema por accidente).
 *  - Las contrasenas nunca se guardan en claro: se derivan con PBKDF2.
 */
public final class ControladorUsuarios {

    private final TemplateEngine motor;

    public ControladorUsuarios(TemplateEngine motor) {
        this.motor = motor;
    }

    public void register(Router router) {
        router.get("/usuarios", this::listar);
        router.get("/usuarios/nuevo", this::formularioNuevo);
        router.post("/usuarios/nuevo", this::crear);
        router.get("/usuarios/{id}/editar", this::formularioEditar);
        router.post("/usuarios/{id}/editar", this::actualizar);
        router.post("/usuarios/{id}/eliminar", this::eliminar);
    }

    // ---------------- Listado ----------------

    private void listar(RequestContext ctx) throws Exception {
        Map<String, Object> data = Views.baseData("Usuarios", "usuarios", ctx);
        MotorConSesion.inyectar(data);

        String filtro = ctx.param("q", "").trim();
        data.put("q", filtro);

        List<Map<String, Object>> filas = new ArrayList<>();
        long activos = 0;
        for (Usuario u : UsuariosDao.listar()) {
            if (u.isActivo()) activos++;
            if (!filtro.isEmpty() && !coincide(u, filtro)) continue;
            filas.add(fila(u));
        }
        data.put("usuarios", filas);
        data.put("totalUsuarios", filas.size());
        data.put("totalActivos", activos);
        data.put("hayFiltro", !filtro.isEmpty());
        ctx.html(200, motor.render("usuarios-lista", data));
    }

    private boolean coincide(Usuario u, String filtro) {
        String f = filtro.toLowerCase();
        return (u.getUsername() != null && u.getUsername().toLowerCase().contains(f))
                || (u.getNombreCompleto() != null && u.getNombreCompleto().toLowerCase().contains(f))
                || (u.getRol() != null && u.getRol().toLowerCase().contains(f));
    }

    private Map<String, Object> fila(Usuario u) {
        Rol rol = Rol.desdeClave(u.getRol());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nombreCompleto", u.getNombreCompleto());
        m.put("rol", rol == null ? (u.getRol() == null ? "" : u.getRol()) : rol.clave());
        m.put("rolEtiqueta", rol == null ? "Sin rol" : rol.etiqueta());
        m.put("rolDescripcion", rol == null ? "" : rol.descripcion());
        m.put("rolCss", rol == null ? "rol-SIN_ROL" : "rol-" + rol.clave());
        m.put("activo", u.isActivo());
        m.put("numeroPermisos", rol == null ? 0 : rol.permisos().size());
        m.put("iniciales", ContextoActual.iniciales(u.getNombreCompleto()));
        return m;
    }

    // ---------------- Alta ----------------

    private void formularioNuevo(RequestContext ctx) throws Exception {
        Map<String, Object> data = baseFormulario(Views.baseData("Nuevo usuario", "usuarios", ctx), Rol.DIGITADOR.clave());
        data.put("esEdicion", false);
        data.put("titulo", "Registrar usuario");
        data.put("idUsuario", "");
        data.put("username", "");
        data.put("usernameEditable", true);
        data.put("nombreCompleto", "");
        data.put("password", "");
        data.put("passwordEditable", true);
        data.put("rolActual", Rol.DIGITADOR.clave());
        data.put("formAction", "/usuarios/nuevo");
        data.put("ayudaContrasena", "Minimo 8 caracteres, con letras y numeros.");
        ctx.html(200, motor.render("usuario-form", data));
    }

    private void crear(RequestContext ctx) throws Exception {
        String username = normalizarUsuario(ctx.param("username", ""));
        String nombre = ctx.param("nombreCompleto", "").trim().replaceAll("\\s+", " ");
        String password = ctx.param("password", "");
        Rol rol = Rol.desdeClave(ctx.param("rol", ""));

        Map<String, Object> data = baseFormulario(new HashMap<>(),
                rol == null ? Rol.DIGITADOR.clave() : rol.clave());
        data.put("esEdicion", false);
        data.put("titulo", "Registrar usuario");
        data.put("idUsuario", "");
        data.put("username", username);
        data.put("usernameEditable", true);
        data.put("nombreCompleto", nombre);
        data.put("rolActual", rol == null ? Rol.DIGITADOR.clave() : rol.clave());
        data.put("formAction", "/usuarios/nuevo");
        data.put("ayudaContrasena", "Minimo 8 caracteres, con letras y numeros.");

        String error = validarUsuario(username, nombre, password, rol, 0);
        if (error != null) {
            data.put("flashError", error);
            ctx.html(400, motor.render("usuario-form", data));
            return;
        }

        UsuariosDao.crear(username, Passwords.hash(password), nombre, rol);
        SesionStore.getInstance().purgar();
        Auditoria.registrar(idActual(), usuarioActual(), rolActual(), Cookies.ip(ctx.exchange),
                "POST", "/usuarios/nuevo", "usuario.crear", Auditoria.OK,
                "Alta de usuario " + username + " con rol " + rol.clave());

        ctx.redirect("/usuarios?ok=" + enc("Usuario " + username + " creado con el rol " + rol.etiqueta() + "."));
    }

    // ---------------- Edicion ----------------

    private void formularioEditar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Usuario u = UsuariosDao.porId(id).orElse(null);
        if (u == null) {
            ctx.redirect("/usuarios?error=" + enc("El usuario no existe."));
            return;
        }
        Rol rol = Rol.desdeClave(u.getRol());
        Map<String, Object> data = baseFormulario(Views.baseData("Editar usuario", "usuarios", ctx),
                Rol.desdeClave(u.getRol()) == null ? "" : Rol.desdeClave(u.getRol()).clave());
        data.put("esEdicion", true);
        data.put("titulo", "Editar usuario");
        data.put("idUsuario", u.getId());
        data.put("username", u.getUsername());
        data.put("usernameEditable", false);
        data.put("nombreCompleto", u.getNombreCompleto());
        data.put("password", "");
        data.put("passwordEditable", true);
        data.put("rolActual", rol == null ? "" : rol.clave());
        data.put("activo", u.isActivo());
        data.put("formAction", "/usuarios/" + u.getId() + "/editar");
        data.put("ayudaContrasena", "Dejalo vacio para conservar la contrasena actual.");
        ctx.html(200, motor.render("usuario-form", data));
    }

    private void actualizar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Usuario existente = UsuariosDao.porId(id).orElse(null);
        if (existente == null) {
            ctx.redirect("/usuarios?error=" + enc("El usuario no existe."));
            return;
        }

        String nombre = ctx.param("nombreCompleto", "").trim().replaceAll("\\s+", " ");
        Rol rol = Rol.desdeClave(ctx.param("rol", ""));
        String password = ctx.param("password", "");
        boolean activo = "1".equals(ctx.param("activo", "")) || "on".equals(ctx.param("activo", ""));

        Map<String, Object> data = baseFormulario(new HashMap<>(),
                rol == null ? "" : rol.clave());
        data.put("esEdicion", true);
        data.put("titulo", "Editar usuario");
        data.put("idUsuario", id);
        data.put("username", existente.getUsername());
        data.put("usernameEditable", false);
        data.put("nombreCompleto", nombre);
        data.put("activo", activo);
        data.put("rolActual", rol == null ? "" : rol.clave());
        data.put("formAction", "/usuarios/" + id + "/editar");
        data.put("ayudaContrasena", "Dejalo vacio para conservar la contrasena actual.");

        if (nombre.isEmpty()) {
            data.put("flashError", "Escribe el nombre completo del usuario.");
            ctx.html(400, motor.render("usuario-form", data));
            return;
        }
        if (rol == null) {
            data.put("flashError", "Selecciona un rol valido.");
            ctx.html(400, motor.render("usuario-form", data));
            return;
        }
        if (!password.isEmpty()) {
            String problema = Passwords.validar(password, existente.getUsername(), nombre);
            if (problema != null) {
                data.put("flashError", problema);
                ctx.html(400, motor.render("usuario-form", data));
                return;
            }
        }

        // Salvaguarda: el sistema no puede quedarse sin administrador general.
        boolean pierdeAdmin = Rol.desdeClave(existente.getRol()) == Rol.ADMIN_GENERAL
                && (rol != Rol.ADMIN_GENERAL || !activo);
        if (pierdeAdmin && UsuariosDao.contarAdminsActivos() <= 1) {
            data.put("flashError", "No puedes quitar el rol ni desactivar al unico administrador general activo.");
            ctx.html(400, motor.render("usuario-form", data));
            return;
        }

        UsuariosDao.actualizarPerfil(id, nombre, rol);
        UsuariosDao.actualizarActivo(id, activo);
        if (!password.isEmpty()) {
            UsuariosDao.actualizarContrasena(id, Passwords.hash(password));
            // Contrasena nueva = sesiones abiertas del usuario invalidadas.
            SesionStore.getInstance().cerrarSesionesAnteriores(existente);
        }
        if (!activo) {
            SesionStore.getInstance().cerrarSesionesAnteriores(existente);
        }

        Auditoria.registrar(idActual(), usuarioActual(), rolActual(), Cookies.ip(ctx.exchange),
                "POST", "/usuarios/" + id + "/editar", "usuario.editar", Auditoria.OK,
                "Edicion de " + existente.getUsername() + ": rol " + rol.clave()
                        + (activo ? "" : ", desactivado") + (password.isEmpty() ? "" : ", contrasena restablecida"));

        String mensaje = "Usuario actualizado.";
        if (!activo) mensaje = "Usuario desactivado. Sus sesiones abiertas se cerraron.";
        else if (!password.isEmpty()) mensaje = "Usuario actualizado y contrasena restablecida.";
        ctx.redirect("/usuarios?ok=" + enc(mensaje));
    }

    // ---------------- Baja ----------------

    private void eliminar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Usuario u = UsuariosDao.porId(id).orElse(null);
        if (u == null) {
            ctx.redirect("/usuarios?error=" + enc("El usuario no existe."));
            return;
        }
        if (id == idActual()) {
            ctx.redirect("/usuarios?error=" + enc("No puedes desactivar tu propia cuenta."));
            return;
        }
        if (Rol.desdeClave(u.getRol()) == Rol.ADMIN_GENERAL && UsuariosDao.contarAdminsActivos() <= 1) {
            ctx.redirect("/usuarios?error=" + enc("No puedes desactivar al unico administrador general activo."));
            return;
        }

        UsuariosDao.actualizarActivo(id, false);
        SesionStore.getInstance().cerrarSesionesAnteriores(u);
        Auditoria.registrar(idActual(), usuarioActual(), rolActual(), Cookies.ip(ctx.exchange),
                "POST", "/usuarios/" + id + "/eliminar", "usuario.eliminar", Auditoria.OK,
                "Desactivacion de " + u.getUsername());

        ctx.redirect("/usuarios?ok=" + enc("Usuario " + u.getUsername() + " desactivado."));
    }

    // ---------------- Helpers ----------------

    private String validarUsuario(String username, String nombre, String password, Rol rol, long exceptoId)
            throws Exception {
        if (username.length() < 3 || username.length() > 50) {
            return "El usuario debe tener entre 3 y 50 caracteres.";
        }
        if (!username.matches("[A-Za-z0-9._-]+")) {
            return "El usuario solo puede llevar letras, numeros, punto, guion y guion bajo.";
        }
        if (nombre.isEmpty() || nombre.length() > 150) {
            return "Escribe el nombre completo (maximo 150 caracteres).";
        }
        if (rol == null) {
            return "Selecciona un rol valido.";
        }
        String problema = Passwords.validar(password, username, nombre);
        if (problema != null) {
            return problema;
        }
        if (UsuariosDao.existeUsername(username, exceptoId).isPresent()) {
            return "El usuario " + username + " ya existe. Elige otro nombre.";
        }
        return null;
    }

    private String normalizarUsuario(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private Map<String, Object> baseFormulario(Map<String, Object> data, String rolActual) {
        MotorConSesion.inyectar(data);
        data.put("error", "");

        List<Map<String, Object>> opciones = new ArrayList<>();
        for (Rol r : Rol.values()) {
            Map<String, Object> opcion = new LinkedHashMap<>();
            opcion.put("clave", r.clave());
            opcion.put("etiqueta", r.etiqueta());
            opcion.put("descripcion", r.descripcion());
            opcion.put("seleccionada", r.clave().equals(rolActual));
            opciones.add(opcion);
        }
        data.put("rolesOpciones", opciones);
        return data;
    }

    private static Long idActual() {
        return ContextoActual.usuario() == null ? null : ContextoActual.usuario().getId();
    }

    private static String usuarioActual() {
        return ContextoActual.usuario() == null ? null : ContextoActual.usuario().getUsername();
    }

    private static String rolActual() {
        return ContextoActual.usuario() == null ? null : ContextoActual.usuario().getRol();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}