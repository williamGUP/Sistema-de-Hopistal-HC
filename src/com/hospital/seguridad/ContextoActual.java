package com.hospital.seguridad;

import com.hospital.model.Usuario;

/**
 * Usuario de la peticion en curso. Se guarda en un ThreadLocal porque el
 * servidor HTTP embebido atiende cada peticion en un hilo del pool y el
 * contexto debe estar disponible durante todo el ciclo request/response
 * (filtro de seguridad, controladores y plantillas).
 *
 * El filtro de seguridad (FiltroSeguridad) lo establish ANTES de invocar al
 * router y lo limpia SIEMPRE en un bloque finally, de modo que un hilo nunca
 * arrastra la identidad de una peticion anterior.
 */
public final class ContextoActual {

    private static final ThreadLocal<Usuario> ACTUAL = new ThreadLocal<>();
    private static final ThreadLocal<Sesion> SESION = new ThreadLocal<>();

    private ContextoActual() {}

    /** Fija la sesion (y con ella el usuario) para la peticion en curso. */
    public static void establecer(Sesion sesion) {
        if (sesion == null) {
            ACTUAL.remove();
            SESION.remove();
            return;
        }
        SESION.set(sesion);
        ACTUAL.set(sesion.usuario());
    }

    /** Fija solo el usuario, sin sesion (util para pruebas). */
    public static void establecer(Usuario usuario) {
        ACTUAL.set(usuario);
    }

    /** Sesion activa de la peticion en curso, o null. */
    public static Sesion sesion() {
        return SESION.get();
    }

    public static Usuario usuario() {
        return ACTUAL.get();
    }

    public static boolean haySesion() {
        return ACTUAL.get() != null;
    }

    /** Rol del usuario en curso, o null si no hay sesion iniciada. */
    public static Rol rol() {
        Usuario u = ACTUAL.get();
        return u == null ? null : Rol.desdeClave(u.getRol());
    }

    /** Indica si el usuario en curso tiene el permiso indicado. */
    public static boolean puede(Permiso permiso) {
        Rol r = rol();
        return r != null && r.concede(permiso);
    }

    public static boolean puede(Permiso... permisos) {
        Rol r = rol();
        if (r == null) return false;
        for (Permiso p : permisos) {
            if (r.concede(p)) return true;
        }
        return false;
    }

    /** Iniciales para el avatar del menú de usuario (p.ej. "Dra. Ana Torres" -> "AT"). */
    public static String iniciales() {
        Usuario u = ACTUAL.get();
        return u == null ? "" : iniciales(u.getNombreCompleto());
    }

    public static String iniciales(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) return "?";
        String[] partes = nombreCompleto.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p.length() >= 3 && Character.isUpperCase(p.charAt(0)) && esTitulo(p)) continue; // Dra., Lic., Sr.
            if (sb.length() >= 2) break;
            sb.append(Character.toUpperCase(p.charAt(0)));
        }
        if (sb.length() == 0) {
            sb.append(Character.toUpperCase(partes[0].charAt(0)));
        }
        return sb.toString();
    }

    private static boolean esTitulo(String palabra) {
        return palabra.endsWith(".");
    }

    public static void limpiar() {
        ACTUAL.remove();
        SESION.remove();
    }
}