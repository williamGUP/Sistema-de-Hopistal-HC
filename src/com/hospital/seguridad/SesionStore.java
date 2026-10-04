package com.hospital.seguridad;

import com.hospital.model.Usuario;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacen de sesiones en memoria del servidor.
 *
 * Decisiones de seguridad:
 *  - El identificador de sesion es un token aleatorio de 256 bits (no contiene
 *    datos del usuario), por lo que no se puede adivinar ni manipular.
 *  - Cada inicio de sesion genera un token nuevo: si alguien fija una sesion
 *    antes del login, esa sesion queda huerfana y nunca se reutiliza
 *    (prevencion de fijacion de sesion).
 *  - Caducidad por inactividad y caducidad maxima absoluta.
 *  - Al cerrar sesion se invalidan TODAS las sesiones de ese usuario, de forma
 *    que cambiar la contrasena o desactivar la cuenta corta el acceso en
 *    cualquier otro navegador.
 */
public final class SesionStore {

    private static final SesionStore INSTANCE = new SesionStore();

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    private SesionStore() {
    }

    public static SesionStore getInstance() {
        return INSTANCE;
    }

    public Sesion crear(Usuario usuario, String ip, String navegador) {
        Instant ahora = Instant.now();
        Sesion sesion = new Sesion(Sesion.nuevoToken(), usuario, Sesion.nuevoToken(), ahora, ip, navegador);
        cerrarSesionesAnteriores(usuario);
        sesiones.put(sesion.token(), sesion);
        return sesion;
    }

    /** Devuelve la sesion vigente o null; si esta caducada, la elimina del almacen. */
    public Sesion obtener(String token) {
        if (token == null || token.isBlank()) return null;
        Sesion s = sesiones.get(token);
        if (s == null) return null;
        Instant ahora = Instant.now();
        if (s.expirada(ahora)) {
            sesiones.remove(token);
            return null;
        }
        s.tocar(ahora);
        return s;
    }

    public void destruir(String token) {
        if (token != null) sesiones.remove(token);
    }

    /** Cierra todas las sesiones del usuario indicado (cambio de contrasena o baja). */
    public int cerrarSesionesAnteriores(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) return 0;
        long id = usuario.getId();
        int cerradas = 0;
        for (Map.Entry<String, Sesion> e : sesiones.entrySet()) {
            if (e.getValue().idUsuario() == id) {
                sesiones.remove(e.getKey());
                cerradas++;
            }
        }
        return cerradas;
    }

    public long contar() {
        return sesiones.size();
    }

    /** Sesiones activas de un usuario, para la pantalla "Mi cuenta". */
    public List<Sesion> sesionesDe(long idUsuario) {
        List<Sesion> propias = new ArrayList<>();
        Instant ahora = Instant.now();
        for (Sesion s : sesiones.values()) {
            if (s.idUsuario() == idUsuario && !s.expirada(ahora)) propias.add(s);
        }
        propias.sort(Comparator.comparing(Sesion::ultimaActividad).reversed());
        return propias;
    }

    /** Limpieza periodica de sesiones caducadas (llamada al arrancar el servidor). */
    public int purgar() {
        Instant ahora = Instant.now();
        int antes = sesiones.size();
        sesiones.entrySet().removeIf(e -> e.getValue().expirada(ahora));
        return antes - sesiones.size();
    }
}