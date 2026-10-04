package com.hospital.seguridad;

import com.hospital.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bitacora de seguridad: deja constancia de cada inicio de sesion, cierre,
 * operacion sensible, intento fallido y peticion denegada por permisos.
 *
 * Sirve para tres cosas:
 *  1. Investigar accesos indebidos (requisito habitual en sistemas de salud).
 *  2. Limitar los ataques de fuerza bruta al login (contando intentos fallidos).
 *  3. Mostrar al administrador general que ha pasado por el sistema.
 */
public final class Auditoria {

    public static final String OK = "CORRECTO";
    public static final String FALLIDO = "FALLIDO";
    public static final String DENEGADO = "DENEGADO";
    public static final String CERRADA = "CIERRE";

    /** Maximo de intentos fallidos de una misma cuenta antes de bloquearla. */
    public static final int MAX_INTENTOS = 5;

    /** Maximo de intentos fallidos desde una misma IP antes de bloquearla (frente a ataques automatizados). */
    public static final int MAX_INTENTOS_POR_IP = 20;

    /** Ventana de tiempo (minutos) en la que se cuentan los intentos fallidos. */
    public static final int VENTANA_MINUTOS = 15;

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Auditoria() {}

    public static void registrar(Long usuarioId, String username, String rol, String ip,
                                 String metodo, String ruta, String accion, String resultado, String detalle) {
        String sql = "INSERT INTO auditoria_acceso "
                + "(fecha, usuario_id, usuario, rol, ip, metodo, ruta, accion, resultado, detalle) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            if (usuarioId == null) ps.setNull(2, java.sql.Types.BIGINT);
            else ps.setLong(2, usuarioId);
            ps.setString(3, abreviar(username, 50));
            ps.setString(4, abreviar(rol, 30));
            ps.setString(5, abreviar(ip, 60));
            ps.setString(6, abreviar(metodo, 6));
            ps.setString(7, abreviar(ruta, 200));
            ps.setString(8, abreviar(accion, 40));
            ps.setString(9, resultado);
            ps.setString(10, abreviar(detalle, 400));
            ps.executeUpdate();
        } catch (SQLException e) {
            // La auditoria jamas debe impedir el trabajo del usuario.
            System.err.println("No se pudo registrar el evento de auditoria: " + e.getMessage());
        }
    }

    /**
     * Cuenta los intentos fallidos recientes de un usuario desde una IP.
     * Devuelve el numero de intentos (0 si no hay ninguno).
     */
    public static int intentosFallidos(String username, String ip) {
        String sql = "SELECT COUNT(*) FROM auditoria_acceso "
                + "WHERE resultado = ? AND LOWER(COALESCE(usuario, '')) = LOWER(?) "
                + "AND COALESCE(ip, '') = ? AND fecha > ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, FALLIDO);
            ps.setString(2, username == null ? "" : username.trim());
            ps.setString(3, ip == null ? "" : ip);
            ps.setTimestamp(4, Timestamp.from(Instant.now().minus(VENTANA_MINUTOS, ChronoUnit.MINUTES)));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Intentos fallidos recientes de una misma cuenta desde una IP. Evita que un
     * atacante bloquee una cuenta legitima probando contrasenas a nombre de otro
     * usuario (denegacion de servicio sobre el login).
     */
    public static int intentosFallidosPorIp(String ip) {
        String sql = "SELECT COUNT(*) FROM auditoria_acceso "
                + "WHERE resultado = ? AND COALESCE(ip, '') = ? AND fecha > ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, FALLIDO);
            ps.setString(2, ip == null ? "" : ip);
            ps.setTimestamp(3, Timestamp.from(Instant.now().minus(VENTANA_MINUTOS, ChronoUnit.MINUTES)));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /** Minutos que faltan para que se levante el bloqueo por intentos fallidos. */
    public static long minutosParaDesbloquear(String username, String ip) {
        String sql = "SELECT MAX(fecha) FROM auditoria_acceso "
                + "WHERE resultado = ? AND LOWER(COALESCE(usuario, '')) = LOWER(?) "
                + "AND COALESCE(ip, '') = ? AND fecha > ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, FALLIDO);
            ps.setString(2, username == null ? "" : username.trim());
            ps.setString(3, ip == null ? "" : ip);
            ps.setTimestamp(4, Timestamp.from(Instant.now().minus(VENTANA_MINUTOS, ChronoUnit.MINUTES)));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                Timestamp ultima = rs.getTimestamp(1);
                if (ultima == null) return 0;
                long faltan = VENTANA_MINUTOS - ChronoUnit.MINUTES.between(ultima.toInstant(), Instant.now());
                return Math.max(0, faltan);
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /** Ultimas entradas de la bitacora, de la mas reciente a la mas antigua. */
    public static List<Map<String, Object>> listar(int limite, String filtroUsuario, String filtroResultado) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.fecha, COALESCE(a.usuario, '') usuario, COALESCE(u.nombre_completo, '') nombre, "
                        + "COALESCE(a.rol, '') rol, COALESCE(a.ip, '') ip, COALESCE(a.metodo, '') metodo, "
                        + "COALESCE(a.ruta, '') ruta, COALESCE(a.accion, '') accion, "
                        + "COALESCE(a.resultado, '') resultado, COALESCE(a.detalle, '') detalle "
                        + "FROM auditoria_acceso a LEFT JOIN usuario u ON u.id = a.usuario_id WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (filtroUsuario != null && !filtroUsuario.isBlank()) {
            sql.append(" AND LOWER(COALESCE(a.usuario, '')) LIKE ?");
            params.add("%" + filtroUsuario.trim().toLowerCase() + "%");
        }
        if (filtroResultado != null && !filtroResultado.isBlank()) {
            sql.append(" AND a.resultado = ?");
            params.add(filtroResultado.trim().toUpperCase());
        }
        sql.append(" ORDER BY a.fecha DESC, a.id DESC LIMIT ?");
        params.add(Math.max(1, Math.min(limite, 500)));

        List<Map<String, Object>> filas = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> f = new LinkedHashMap<>();
                    Timestamp fecha = rs.getTimestamp("fecha");
                    f.put("fecha", fecha == null ? "" : FECHA_HORA.format(fecha.toLocalDateTime()));
                    String usuario = rs.getString("usuario");
                    String nombre = rs.getString("nombre");
                    f.put("usuario", usuario);
                    f.put("nombre", nombre == null || nombre.isEmpty() ? "—" : nombre);
                    String rolClave = rs.getString("rol");
                    Rol r = Rol.desdeClave(rolClave);
                    f.put("rol", rolClave);
                    f.put("rolEtiqueta", r == null ? (rolClave.isEmpty() ? "—" : rolClave) : r.etiqueta());
                    f.put("rolCss", r == null ? "rol-SIN_ROL" : "rol-" + r.clave());
                    f.put("ip", rs.getString("ip"));
                    f.put("metodo", rs.getString("metodo"));
                    f.put("ruta", Cookies.recortar(rs.getString("ruta"), 60));
                    f.put("accion", rs.getString("accion"));
                    String resultado = rs.getString("resultado");
                    f.put("resultado", resultado);
                    f.put("detalle", Cookies.recortar(rs.getString("detalle"), 120));
                    boolean malo = Auditoria.DENEGADO.equals(resultado) || Auditoria.FALLIDO.equals(resultado);
                    f.put("claseResultado", malo ? "is-bad" : Auditoria.CERRADA.equals(resultado) ? "is-muted" : "is-ok");
                    filas.add(f);
                }
            }
        } catch (SQLException e) {
            System.err.println("No se pudo leer la bitacora: " + e.getMessage());
        }
        return filas;
    }

    /** Resumen para las tarjetas de estadisticas de la pantalla de auditoria. */
    public static long contar(String resultado) {
        String sql = "SELECT COUNT(*) FROM auditoria_acceso WHERE resultado = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resultado);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public static long contarEventos() {
        return contar(OK) + contar(FALLIDO) + contar(DENEGADO) + contar(CERRADA);
    }

    private static String abreviar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}