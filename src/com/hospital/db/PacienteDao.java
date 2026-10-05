package com.hospital.db;

import com.hospital.model.Paciente;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PacienteDao {

    /** Inserta el paciente y le asigna un número de historia clínica correlativo (HC-000001, HC-000002, ...). */
    public long crear(Paciente p) throws SQLException {
        String sql = "INSERT INTO paciente (dni, numero_historia, nombres, apellidos, fecha_nacimiento, sexo, telefono, direccion, email, grupo_sanguineo, alergias) " +
                     "VALUES (?, '', ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getDni());
            ps.setString(2, p.getNombres());
            ps.setString(3, p.getApellidos());
            setDateOrNull(ps, 4, p.getFechaNacimiento());
            ps.setString(5, p.getSexo());
            ps.setString(6, p.getTelefono());
            ps.setString(7, p.getDireccion());
            ps.setString(8, p.getEmail());
            ps.setString(9, p.getGrupoSanguineo());
            ps.setString(10, p.getAlergias());
            ps.executeUpdate();
            long id;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getLong(1);
            }
            String numeroHistoria = "HC-" + String.format("%06d", id);
            try (PreparedStatement up = conn.prepareStatement("UPDATE paciente SET numero_historia = ? WHERE id = ?")) {
                up.setString(1, numeroHistoria);
                up.setLong(2, id);
                up.executeUpdate();
            }
            return id;
        }
    }

    public void actualizar(Paciente p) throws SQLException {
        String sql = "UPDATE paciente SET dni=?, nombres=?, apellidos=?, fecha_nacimiento=?, sexo=?, telefono=?, direccion=?, email=?, grupo_sanguineo=?, alergias=? WHERE id=?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getDni());
            ps.setString(2, p.getNombres());
            ps.setString(3, p.getApellidos());
            setDateOrNull(ps, 4, p.getFechaNacimiento());
            ps.setString(5, p.getSexo());
            ps.setString(6, p.getTelefono());
            ps.setString(7, p.getDireccion());
            ps.setString(8, p.getEmail());
            ps.setString(9, p.getGrupoSanguineo());
            ps.setString(10, p.getAlergias());
            ps.setLong(11, p.getId());
            ps.executeUpdate();
        }
    }

    public void desactivar(long id) throws SQLException {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE paciente SET activo = FALSE WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Paciente> buscarPorId(long id) throws SQLException {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM paciente WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Paciente> buscarPorDni(String dni) throws SQLException {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM paciente WHERE dni = ?")) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Paciente> listarActivos() throws SQLException {
        List<Paciente> out = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM paciente WHERE activo = TRUE ORDER BY apellidos, nombres")) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<Paciente> ultimosRegistrados(int limite) throws SQLException {
        List<Paciente> out = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM paciente WHERE activo = TRUE ORDER BY fecha_registro DESC, id DESC LIMIT ?")) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public int contarActivos() throws SQLException {
        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM paciente WHERE activo = TRUE")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Busca por DNI exacto, número de historia exacto, o coincidencia parcial en nombres/apellidos. */
    public List<Paciente> buscar(String q) throws SQLException {
        List<Paciente> out = new ArrayList<>();
        String like = "%" + q.toLowerCase() + "%";
        String sql = "SELECT * FROM paciente WHERE activo = TRUE AND (" +
                     "LOWER(dni) = LOWER(?) OR LOWER(numero_historia) = LOWER(?) " +
                     "OR LOWER(nombres) LIKE ? OR LOWER(apellidos) LIKE ? " +
                     "OR LOWER(nombres || ' ' || apellidos) LIKE ?" +
                     ") ORDER BY apellidos, nombres";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private void setDateOrNull(PreparedStatement ps, int idx, LocalDate date) throws SQLException {
        if (date == null) ps.setNull(idx, Types.DATE);
        else ps.setDate(idx, Date.valueOf(date));
    }

    private Paciente map(ResultSet rs) throws SQLException {
        Paciente p = new Paciente();
        p.setId(rs.getLong("id"));
        p.setDni(rs.getString("dni"));
        p.setNumeroHistoria(rs.getString("numero_historia"));
        p.setNombres(rs.getString("nombres"));
        p.setApellidos(rs.getString("apellidos"));
        Date fn = rs.getDate("fecha_nacimiento");
        p.setFechaNacimiento(fn == null ? null : fn.toLocalDate());
        p.setSexo(rs.getString("sexo"));
        p.setTelefono(rs.getString("telefono"));
        p.setDireccion(rs.getString("direccion"));
        p.setEmail(rs.getString("email"));
        p.setGrupoSanguineo(rs.getString("grupo_sanguineo"));
        p.setAlergias(rs.getString("alergias"));
        p.setActivo(rs.getBoolean("activo"));
        Timestamp fr = rs.getTimestamp("fecha_registro");
        p.setFechaRegistro(fr == null ? null : fr.toLocalDateTime());
        return p;
    }
}
