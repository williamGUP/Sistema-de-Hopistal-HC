package com.hospital.web;

import com.hospital.db.ConsultaDao;
import com.hospital.db.PacienteDao;
import com.hospital.model.Consulta;
import com.hospital.model.Paciente;
import com.hospital.template.TemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import static com.hospital.web.PacienteController.enc;

public class ConsultaController {

    private final ConsultaDao consultaDao = new ConsultaDao();
    private final PacienteDao pacienteDao = new PacienteDao();
    private final TemplateEngine engine;

    public ConsultaController(TemplateEngine engine) {
        this.engine = engine;
    }

    public void register(Router router) {
        router.get("/pacientes/{id}/consultas/nueva", this::form);
        router.post("/pacientes/{id}/consultas/nueva", this::guardar);
        router.post("/consultas/{id}/eliminar", this::eliminar);
    }

    private void form(RequestContext ctx) throws Exception {
        long pacienteId = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(pacienteId);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }

        Map<String, Object> data = Views.baseData("Nueva consulta", "pacientes", ctx);
        data.put("paciente", Views.pacienteResumen(op.get()));
        data.put("motivo", "");
        data.put("medico", "");
        data.put("sintomas", "");
        data.put("diagnostico", "");
        data.put("tratamiento", "");
        data.put("observaciones", "");
        ctx.html(200, engine.render("consulta-form", data));
    }

    private void guardar(RequestContext ctx) throws Exception {
        long pacienteId = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(pacienteId);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }

        String motivo = ctx.param("motivo", "").trim();
        String sintomas = ctx.param("sintomas", "").trim();
        String diagnostico = ctx.param("diagnostico", "").trim();

        if (motivo.isEmpty() || sintomas.isEmpty() || diagnostico.isEmpty()) {
            Map<String, Object> data = Views.baseData("Nueva consulta", "pacientes", ctx);
            data.put("paciente", Views.pacienteResumen(op.get()));
            data.put("flashError", "Completa al menos motivo, síntomas y diagnóstico.");
            data.put("medico", ctx.param("medico", ""));
            data.put("motivo", motivo);
            data.put("sintomas", sintomas);
            data.put("diagnostico", diagnostico);
            data.put("tratamiento", ctx.param("tratamiento", ""));
            data.put("observaciones", ctx.param("observaciones", ""));
            ctx.html(200, engine.render("consulta-form", data));
            return;
        }

        Consulta c = new Consulta();
        c.setPacienteId(pacienteId);
        String fechaStr = ctx.param("fecha", "");
        LocalDateTime fecha;
        try {
            fecha = fechaStr.isBlank() ? LocalDateTime.now() : LocalDateTime.parse(fechaStr);
        } catch (DateTimeParseException e) {
            fecha = LocalDateTime.now();
        }
        c.setFecha(fecha);
        c.setMedico(ctx.param("medico", "").trim());
        c.setMotivo(motivo);
        c.setSintomas(sintomas);
        c.setDiagnostico(diagnostico);
        c.setTratamiento(ctx.param("tratamiento", "").trim());
        c.setObservaciones(ctx.param("observaciones", "").trim());
        consultaDao.crear(c);

        ctx.redirect("/pacientes/" + pacienteId + "?ok=" + enc("Consulta registrada correctamente."));
    }

    private void eliminar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Optional<Consulta> c = consultaDao.buscarPorId(id);
        if (c.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Consulta no encontrada.")); return; }
        long pacienteId = c.get().getPacienteId();
        consultaDao.eliminar(id);
        ctx.redirect("/pacientes/" + pacienteId + "?ok=" + enc("Consulta eliminada."));
    }
}
