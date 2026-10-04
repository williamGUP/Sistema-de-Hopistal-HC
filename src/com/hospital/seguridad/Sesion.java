package com.hospital.seguridad;

import com.hospital.model.Usuario;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Sesion iniciada: token opaco, usuario, token CSRF y metadatos de auditoria
 * (IP, navegador, caducidad).
 */
public final class Sesion {

    /** Tiempo maximo sin actividad antes de cerrar la sesion. */
    public static final Duration INACTIVIDAD_MAXIMA = Duration.ofMinutes(30);

    /** Tiempo maximo total de la sesion, aunque el usuario no deje de usarla. */
    public static final Duration DURACION_MAXIMA = Duration.ofHours(12);

    private final String token;
    private final Usuario usuario;
    private final String tokenCsrf;
    private final Instant creada;
    private final String ip;
    private final String navegador;
    private Instant ultimaActividad;

    Sesion(String token, Usuario usuario, String tokenCsrf, Instant creada, String ip, String navegador) {
        this.token = token;
        this.usuario = usuario;
        this.tokenCsrf = tokenCsrf;
        this.creada = creada;
        this.ip = ip;
        this.navegador = navegador;
        this.ultimaActividad = creada;
    }

    public String token() { return token; }

    public Usuario usuario() { return usuario; }

    public String tokenCsrf() { return tokenCsrf; }

    public Instant creada() { return creada; }

    public Instant ultimaActividad() { return ultimaActividad; }

    public String ip() { return ip; }

    public String navegador() { return navegador; }

    public long idUsuario() {
        return usuario == null || usuario.getId() == null ? -1L : usuario.getId();
    }

    public Rol rol() {
        return usuario == null ? null : Rol.desdeClave(usuario.getRol());
    }

    /** Sesion caducada por inactividad o por superar la duracion maxima. */
    public boolean expirada(Instant ahora) {
        if (ultimaActividad.plus(INACTIVIDAD_MAXIMA).isBefore(ahora)) return true;
        return creada.plus(DURACION_MAXIMA).isBefore(ahora);
    }

    /** Minutos restantes de inactividad, para avisar al usuario antes de que expire. */
    public long minutosRestantes(Instant ahora) {
        long segundos = Duration.between(ahora, ultimaActividad.plus(INACTIVIDAD_MAXIMA)).getSeconds();
        return Math.max(0, segundos / 60);
    }

    public void tocar(Instant ahora) {
        this.ultimaActividad = ahora;
    }

    /**
     * Refresca la copia del usuario guardada en la sesion. El rol, el nombre o
     * el estado pueden haber cambiado en la base de datos mientras el usuario
     * tenia la sesion abierta.
     */
    void actualizarUsuario(Usuario actualizado) {
        if (actualizado == null || usuario == null) return;
        usuario.setUsername(actualizado.getUsername());
        usuario.setNombreCompleto(actualizado.getNombreCompleto());
        usuario.setRol(actualizado.getRol());
        usuario.setActivo(actualizado.isActivo());
    }

    /** Genera un token opaco criptograficamente aleatorio (256 bits). */
    static String nuevoToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}