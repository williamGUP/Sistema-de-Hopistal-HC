package com.hospital.db;

import com.hospital.model.Consulta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsultaDao {

    public long crear(Consulta c) throws SQLException {
        String sql = "INSERT INTO consulta (paciente_id, fecha, medico, motivo, sintomas, diagnostico, tratamiento, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, c.getPacienteId());
            ps.setTimestamp(2, Timestamp.valueOf(c.getFecha()));
            ps.setString(3, c.getMedico());
            ps.setString(4, c.getMotivo());
            ps.setString(5, c.getSintomas());
            ps.setString(6, c.getDiagnostico());
            ps.setString(7, c.getTratamiento());
            ps.setString(8, c.getObservaciones());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public Optional<Consulta> buscarPorId(long id) throws SQLException {
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM consulta WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Consulta> listarPorPaciente(long pacienteId) throws SQLException {
        List<Consulta> out = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM consulta WHERE paciente_id = ? ORDER BY fecha DESC, id DESC")) {
            ps.setLong(1, pacienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public void eliminar(long id) throws SQLException {
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM consulta WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Consulta map(ResultSet rs) throws SQLException {
        Consulta c = new Consulta();
        c.setId(rs.getLong("id"));
        c.setPacienteId(rs.getLong("paciente_id"));
        Timestamp f = rs.getTimestamp("fecha");
        c.setFecha(f == null ? null : f.toLocalDateTime());
        c.setMedico(rs.getString("medico"));
        c.setMotivo(rs.getString("motivo"));
        c.setSintomas(rs.getString("sintomas"));
        c.setDiagnostico(rs.getString("diagnostico"));
        c.setTratamiento(rs.getString("tratamiento"));
        c.setObservaciones(rs.getString("observaciones"));
        return c;
    }
}
