package com.hospital.db;

import com.hospital.model.Enfermedad;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnfermedadDao {

    public long crear(Enfermedad e) throws SQLException {
        String sql = "INSERT INTO enfermedad (paciente_id, nombre, fecha_diagnostico, estado, observaciones) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, e.getPacienteId());
            ps.setString(2, e.getNombre());
            if (e.getFechaDiagnostico() == null) ps.setNull(3, Types.DATE);
            else ps.setDate(3, Date.valueOf(e.getFechaDiagnostico()));
            ps.setString(4, e.getEstado());
            ps.setString(5, e.getObservaciones());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public Optional<Enfermedad> buscarPorId(long id) throws SQLException {
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM enfermedad WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Enfermedad> listarPorPaciente(long pacienteId) throws SQLException {
        List<Enfermedad> out = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM enfermedad WHERE paciente_id = ? ORDER BY fecha_diagnostico DESC, id DESC")) {
            ps.setLong(1, pacienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public void eliminar(long id) throws SQLException {
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM enfermedad WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Enfermedad map(ResultSet rs) throws SQLException {
        Enfermedad e = new Enfermedad();
        e.setId(rs.getLong("id"));
        e.setPacienteId(rs.getLong("paciente_id"));
        e.setNombre(rs.getString("nombre"));
        Date fd = rs.getDate("fecha_diagnostico");
        e.setFechaDiagnostico(fd == null ? null : fd.toLocalDate());
        e.setEstado(rs.getString("estado"));
        e.setObservaciones(rs.getString("observaciones"));
        return e;
    }
}
