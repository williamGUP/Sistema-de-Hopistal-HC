package com.hospital.db;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexion a la base de datos del proyecto
 */
public final class Db {

    private static String jdbcUrl;

    private Db() {}

    public static synchronized void init() {
        if (jdbcUrl != null) return;
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se encontró el driver de H2 en el classpath (lib/h2-2.2.224.jar).", e);
        }
        Path dataDir = AppPaths.resolveDir("data");
        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear la carpeta de datos: " + dataDir, e);
        }
        Path dbFile = dataDir.resolve("hospital").toAbsolutePath();
        jdbcUrl = "jdbc:h2:file:" + dbFile + ";AUTO_SERVER=TRUE";

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String ddl : SCHEMA_STATEMENTS) {
                st.execute(ddl);
            }
            seedIfEmpty(conn);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo inicializar la base de datos en " + dbFile, e);
        }
        System.out.println("Base de datos lista en: " + dbFile + ".mv.db");
    }

    public static Connection getConnection() throws SQLException {
        if (jdbcUrl == null) throw new IllegalStateException("Db.init() no fue llamado todavía");
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    private static final String[] SCHEMA_STATEMENTS = {
        "CREATE TABLE IF NOT EXISTS paciente (" +
        "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "  dni VARCHAR(8) NOT NULL," +
        "  numero_historia VARCHAR(20) NOT NULL," +
        "  nombres VARCHAR(100) NOT NULL," +
        "  apellidos VARCHAR(100) NOT NULL," +
        "  fecha_nacimiento DATE," +
        "  sexo VARCHAR(15)," +
        "  telefono VARCHAR(20)," +
        "  direccion VARCHAR(200)," +
        "  email VARCHAR(120)," +
        "  grupo_sanguineo VARCHAR(5)," +
        "  alergias VARCHAR(500)," +
        "  activo BOOLEAN NOT NULL DEFAULT TRUE," +
        "  fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
        "  CONSTRAINT uq_paciente_dni UNIQUE (dni)," +
        "  CONSTRAINT uq_paciente_numero_historia UNIQUE (numero_historia)" +
        ")",

        "CREATE TABLE IF NOT EXISTS consulta (" +
        "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "  paciente_id BIGINT NOT NULL," +
        "  fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
        "  medico VARCHAR(100)," +
        "  motivo VARCHAR(200)," +
        "  sintomas VARCHAR(1000)," +
        "  diagnostico VARCHAR(1000)," +
        "  tratamiento VARCHAR(1000)," +
        "  observaciones VARCHAR(1000)," +
        "  CONSTRAINT fk_consulta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id)" +
        ")",

        "CREATE TABLE IF NOT EXISTS enfermedad (" +
        "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "  paciente_id BIGINT NOT NULL," +
        "  nombre VARCHAR(150) NOT NULL," +
        "  fecha_diagnostico DATE," +
        "  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'," +
        "  observaciones VARCHAR(1000)," +
        "  CONSTRAINT fk_enfermedad_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id)" +
        ")",

        "CREATE TABLE IF NOT EXISTS operacion (" +
        "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
        "  paciente_id BIGINT NOT NULL," +
        "  fecha DATE NOT NULL," +
        "  tipo_operacion VARCHAR(150) NOT NULL," +
        "  cirujano VARCHAR(100)," +
        "  resultado VARCHAR(30)," +
        "  observaciones VARCHAR(1000)," +
        "  CONSTRAINT fk_operacion_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id)" +
        ")",

        "CREATE INDEX IF NOT EXISTS idx_paciente_apellidos ON paciente(apellidos)",
        "CREATE INDEX IF NOT EXISTS idx_consulta_paciente ON consulta(paciente_id)",
        "CREATE INDEX IF NOT EXISTS idx_enfermedad_paciente ON enfermedad(paciente_id)",
        "CREATE INDEX IF NOT EXISTS idx_operacion_paciente ON operacion(paciente_id)"
    };

    /** Si la base de datos está recién creada (sin pacientes), inserta un par de pacientes de ejemplo. */
    private static void seedIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM paciente")) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        try (Statement st = conn.createStatement()) {
            st.execute(
                "INSERT INTO paciente (dni, numero_historia, nombres, apellidos, fecha_nacimiento, sexo, telefono, direccion, email, grupo_sanguineo, alergias) VALUES " +
                "('45678912','HC-000001','Maria Fernanda','Torres Quispe','1990-04-12','FEMENINO','987654321','Av. Los Alamos 123, Lima','maria.torres@example.com','O+','Penicilina')");
            st.execute(
                "INSERT INTO paciente (dni, numero_historia, nombres, apellidos, fecha_nacimiento, sexo, telefono, direccion, email, grupo_sanguineo, alergias) VALUES " +
                "('71234567','HC-000002','Jose Luis','Ramirez Soto','1978-11-02','MASCULINO','912345678','Jr. Las Flores 456, Lima','jose.ramirez@example.com','A-','Ninguna conocida')");
            st.execute(
                "INSERT INTO paciente (dni, numero_historia, nombres, apellidos, fecha_nacimiento, sexo, telefono, direccion, email, grupo_sanguineo, alergias) VALUES " +
                "('80112233','HC-000003','Ana Lucia','Gomez Diaz','2005-07-20','FEMENINO','998877665','Calle Real 789, Lima','','B+','')");

            st.execute(
                "INSERT INTO consulta (paciente_id, fecha, medico, motivo, sintomas, diagnostico, tratamiento, observaciones) VALUES " +
                "(1, '2026-06-10 09:30:00', 'Dr. Carlos Mendoza', 'Dolor abdominal', 'Dolor en fosa iliaca derecha, nauseas', 'Sospecha de apendicitis', 'Referida a cirugia para evaluacion', 'Se deriva de emergencia')");
            st.execute(
                "INSERT INTO consulta (paciente_id, fecha, medico, motivo, sintomas, diagnostico, tratamiento, observaciones) VALUES " +
                "(1, '2026-07-02 11:00:00', 'Dra. Lucia Fernandez', 'Control post-operatorio', 'Sin dolor, herida cicatrizando bien', 'Evolucion favorable post apendicectomia', 'Continuar reposo relativo 1 semana mas', '')");
            st.execute(
                "INSERT INTO consulta (paciente_id, fecha, medico, motivo, sintomas, diagnostico, tratamiento, observaciones) VALUES " +
                "(2, '2026-05-15 16:20:00', 'Dr. Carlos Mendoza', 'Control de rutina - diabetes', 'Sed excesiva, fatiga leve', 'Diabetes tipo 2 en tratamiento', 'Continuar metformina 850mg, dieta baja en azucares', 'Glucosa en ayunas: 145 mg/dL')");

            st.execute(
                "INSERT INTO enfermedad (paciente_id, nombre, fecha_diagnostico, estado, observaciones) VALUES " +
                "(1, 'Apendicitis aguda', '2026-06-10', 'CURADA', 'Resuelta mediante apendicectomia')");
            st.execute(
                "INSERT INTO enfermedad (paciente_id, nombre, fecha_diagnostico, estado, observaciones) VALUES " +
                "(2, 'Diabetes mellitus tipo 2', '2019-03-01', 'CONTROLADA', 'Control con metformina y dieta')");
            st.execute(
                "INSERT INTO enfermedad (paciente_id, nombre, fecha_diagnostico, estado, observaciones) VALUES " +
                "(2, 'Hipertension arterial', '2021-08-15', 'ACTIVA', 'En evaluacion, aun sin tratamiento farmacologico')");

            st.execute(
                "INSERT INTO operacion (paciente_id, fecha, tipo_operacion, cirujano, resultado, observaciones) VALUES " +
                "(1, '2026-06-11', 'Apendicectomia laparoscopica', 'Dr. Roberto Salazar', 'EXITOSA', 'Sin complicaciones, alta a las 48 horas')");
        }
        System.out.println("Se insertaron pacientes de ejemplo (primera ejecucion).");
    }
}