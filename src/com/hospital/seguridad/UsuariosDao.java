package com.hospital.seguridad;

import com.hospital.db.Db;
import com.hospital.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla usuario para el subsistema de seguridad (autenticacion y
 * administracion de cuentas). Vive en el paquete "seguridad" para no depender
 * del DAO minimo que ya existia en el proyecto.
 */
public final class UsuariosDao {

    private static final String COLUMNAS = "id, username, password, nombre_completo, rol, activo";

    private UsuariosDao() {}

    /** Busca un usuario por nombre, aunque este inactivo (el login filtra por activo). */
    public static Optional<Usuario> porUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM usuario WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username == null ? "" : username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(SeguridadEsquema.mapear(rs)) : Optional.empty();
            }
        }
    }

    public static Optional<Usuario> porId(long id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM usuario WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(SeguridadEsquema.mapear(rs)) : Optional.empty();
            }
        }
    }

    public static List<Usuario> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM usuario ORDER BY activo DESC, nombre_completo";
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(SeguridadEsquema.mapear(rs));
        }
        return lista;
    }

    /** Username ya usado por otro usuario, o null si esta libre. */
    public static Optional<Usuario> existeUsername(String username, long exceptoId) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM usuario WHERE LOWER(username) = LOWER(?) AND id <> ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username == null ? "" : username.trim());
            ps.setLong(2, exceptoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(SeguridadEsquema.mapear(rs)) : Optional.empty();
            }
        }
    }

    public static long crear(String username, String passwordHash, String nombre, Rol rol) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            SeguridadEsquema.crearUsuario(conn, username, passwordHash, nombre, rol.clave());
        }
        return porUsername(username).map(Usuario::getId).orElse(-1L);
    }

    public static void actualizarPerfil(long id, String nombre, Rol rol) throws SQLException {
        String sql = "UPDATE usuario SET nombre_completo = ?, rol = ? WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, rol.clave());
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public static void actualizarContrasena(long id, String passwordHash) throws SQLException {
        String sql = "UPDATE usuario SET password = ? WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public static void actualizarActivo(long id, boolean activo) throws SQLException {
        String sql = "UPDATE usuario SET activo = ? WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** Numero de administradores generales activos (protege contra quedarse sin admin). */
    public static long contarAdminsActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE rol = 'ADMIN_GENERAL' AND activo = TRUE";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /**
     * Indica si el usuario sigue usando la contrasena de fabrica.
     * Permite avisar en la interfaz para que se cambien las claves de prueba.
     */
    public static boolean usaContrasenaDeDemo(long id) throws SQLException {
        Optional<Usuario> u = porId(id);
        if (u.isEmpty()) return false;
        for (String[] demo : SeguridadEsquema.USUARIOS_DEMO) {
            if (!u.get().getUsername().equalsIgnoreCase(demo[0])) continue;
            if (Passwords.verificar(demo[1], u.get().getPassword())) return true;
        }
        return false;
    }
}