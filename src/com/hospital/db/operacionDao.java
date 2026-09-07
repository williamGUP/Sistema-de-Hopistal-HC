package com.hospital.db;

import com.hospital.model.Operacion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OperacionDao {

    public long crear(Operacion o) throws SQLException {
        String sql = "INSERT INTO operacion (paciente_id, fecha, tipo_operacion, cirujano, resultado, observaciones) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, o.getPacienteId());
            ps.setDate(2, Date.valueOf(o.getFecha()));
            ps.setString(3, o.getTipoOperacion());
            ps.setString(4, o.getCirujano());
            ps.setString(5, o.getResultado());
            ps.setString(6, o.getObservaciones());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public Optional<Operacion> buscarPorId(long id) throws SQLException {
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM operacion WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Operacion> listarPorPaciente(long pacienteId) throws SQLException {
        List<Operacion> out = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM operacion WHERE paciente_id = ? ORDER BY fecha DESC, id DESC")) {
            ps.setLong(1, pacienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public void eliminar(long id) throws SQLException {
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM operacion WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Operacion map(ResultSet rs) throws SQLException {
        Operacion o = new Operacion();
        o.setId(rs.getLong("id"));
        o.setPacienteId(rs.getLong("paciente_id"));
        Date f = rs.getDate("fecha");
        o.setFecha(f == null ? null : f.toLocalDate());
        o.setTipoOperacion(rs.getString("tipo_operacion"));
        o.setCirujano(rs.getString("cirujano"));
        o.setResultado(rs.getString("resultado"));
        o.setObservaciones(rs.getString("observaciones"));
        return o;
    }
}
