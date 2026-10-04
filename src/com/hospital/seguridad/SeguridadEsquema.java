package com.hospital.seguridad;

import com.hospital.db.AppPaths;
import com.hospital.db.Db;
import com.hospital.model.Usuario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Esquema de seguridad del sistema de acceso: crea la tabla "usuario" (que el
 * proyecto documenta en database/schema.sql pero que Db.java no creaba) y las
 * tablas nuevas de auditoria, y siembra los cuatro roles con las cuentas de
 * demostración.
 *
 * No se modifica ninguna tabla existente del sistema (paciente, consulta,
 * enfermedad, operacion): solo se anaden tablas, y la tabla de usuarios se crea
 * exactamente con las columnas ya documentadas en database/schema.sql, de modo
 * que ese archivo siga siendo la referencia valida.
 *
 * Compatibilidad: si la base de datos ya tenia usuarios con la contrasena en
 * texto plano (base creada por una version anterior), se conserva su
 * contrasena y se migra automaticamente a hash PBKDF2 en el primer arranque.
 */
public final class SeguridadEsquema {

    private static volatile boolean inicializada;

    private SeguridadEsquema() {}

    public static synchronized void init() {
        if (inicializada) return;
        inicializada = true;

        // Aviso en consola: credenciales de fabrica.
        Path nota = AppPaths.resolveDir("data").resolve("CREDENCIALES-DEMOSTRACION.txt");
        try {
            Path dir = AppPaths.resolveDir("data");
            Files.createDirectories(dir);
            if (!Files.exists(nota)) {
                StringBuilder contenido = new StringBuilder(
                        "Cuentas de demostracion creadas por el sistema de seguridad.\n"
                                + "CAMBIA ESTAS CONTRASENAS ANTES DE USAR EL SISTEMA CON DATOS REALES.\n\n"
                                + String.format("%-12s %-16s %s%n", "Usuario", "Contrasena", "Nombre")
                                + "------------------------------------------------------------\n");
                for (String[] u : USUARIOS_DEMO) {
                    contenido.append(String.format("%-12s %-16s %s%n", u[0], u[1], u[2]));
                }
                Files.writeString(nota, contenido.toString());
                System.out.println("Usuarios de demostracion creados. Ver: " + nota);
            }
        } catch (Exception e) {
            System.out.println("Aviso: no se pudo escribir el archivo de credenciales (" + e.getMessage() + ")");
        }

        try (Connection conn = Db.getConnection(); Statement st = conn.createStatement()) {
            for (String ddl : DDL) st.execute(ddl);
            sembrarUsuarios(conn);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo inicializar el esquema de seguridad.", e);
        }
    }

    private static final String[] DDL = {
        // Tabla de usuarios: identica a la documentada en database/schema.sql.
        "CREATE TABLE IF NOT EXISTS usuario (" +
        "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "  username VARCHAR(50) NOT NULL," +
        "  password VARCHAR(255) NOT NULL," +
        "  nombre_completo VARCHAR(150) NOT NULL," +
        "  rol VARCHAR(30) NOT NULL," +
        "  activo BOOLEAN NOT NULL DEFAULT TRUE," +
        "  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
        "  CONSTRAINT uq_usuario_username UNIQUE (username)" +
        ")",

        // Bitacora de seguridad: accesos, operaciones y denegaciones.
        "CREATE TABLE IF NOT EXISTS auditoria_acceso (" +
        "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "  fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
        "  usuario_id BIGINT," +
        "  usuario VARCHAR(50)," +
        "  rol VARCHAR(30)," +
        "  ip VARCHAR(60)," +
        "  metodo VARCHAR(6)," +
        "  ruta VARCHAR(200)," +
        "  accion VARCHAR(40)," +
        "  resultado VARCHAR(20)," +
        "  detalle VARCHAR(400)" +
        ")",

        "CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria_acceso(fecha)",
        "CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON auditoria_acceso(usuario, fecha)"
    };

    /** Cuentas de demostracion: usuario, contrasena inicial, nombre, rol. */
    static final String[][] USUARIOS_DEMO = {
            {"admin",     "Admin2026", "Elena Ramirez Rojas",  "ADMIN_GENERAL"},
            {"doctor",    "Doctor2026", "Carlos Mendoza Rios",  "DOCTOR"},
            {"enfermera", "Enfermera2026", "Ana Torres Vasquez", "ENFERMERA"},
            {"digitador", "Digitador2026", "Luis Huaman Vega",   "DIGITADOR"}
    };

    private static void sembrarUsuarios(Connection conn) throws SQLException {
        int existentes;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuario")) {
            rs.next();
            existentes = rs.getInt(1);
        }

        if (existentes > 0) {
            int migradas = migrarPasswordsEnTextoPlano(conn);
            if (existentes < USUARIOS_DEMO.length) {
                crearFaltantes(conn);
            }
            if (migradas > 0) {
                System.out.println("Se migraron a hash PBKDF2 " + migradas + " contrasena(s) antigua(s).");
            }
            return;
        }

        for (String[] u : USUARIOS_DEMO) {
            crearUsuario(conn, u[0], Passwords.hash(u[1]), u[2], u[3]);
        }
        System.out.println("Se crearon los " + USUARIOS_DEMO.length + " usuarios de los cuatro roles.");
    }

    private static void crearFaltantes(Connection conn) throws SQLException {
        for (String[] u : USUARIOS_DEMO) {
            if (existeUsuario(conn, u[0])) continue;
            crearUsuario(conn, u[0], Passwords.hash(u[1]), u[2], u[3]);
        }
    }

    /**
     * Convierte a PBKDF2 las contrasenas guardadas en texto plano por versiones
     * anteriores del proyecto, sin cambiar el valor de la contrasena.
     */
    private static int migrarPasswordsEnTextoPlano(Connection conn) throws SQLException {
        int total = 0;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, password FROM usuario WHERE password NOT LIKE 'pbkdf2-sha256$%'")) {
            ps.setFetchSize(50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String plano = rs.getString("password");
                    if (Passwords.esHashValido(plano)) continue;
                    try (PreparedStatement upd = conn.prepareStatement("UPDATE usuario SET password = ? WHERE id = ?")) {
                        upd.setString(1, Passwords.hash(plano));
                        upd.setLong(2, id);
                        upd.executeUpdate();
                        total++;
                    }
                }
            }
        }
        return total;
    }

    private static boolean existeUsuario(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM usuario WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static void crearUsuario(Connection conn, String username, String passwordHash, String nombre, String rol)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO usuario (username, password, nombre_completo, rol, activo) VALUES (?, ?, ?, ?, TRUE)")) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, nombre);
            ps.setString(4, rol);
            ps.executeUpdate();
        }
    }

    /** Registra un acceso en la bitacora; nunca debe lanzar excepciones al llamador. */
    public static void auditar(Long usuarioId, String username, String rol, String ip,
                               String metodo, String ruta, String accion, String resultado, String detalle) {
        Auditoria.registrar(usuarioId, username, rol, ip, metodo, ruta, accion, resultado, detalle);
    }

    /** Utility compartido: convierte un Usuario leido de la BD. */
    public static Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getBoolean("activo"));
        return u;
    }
}