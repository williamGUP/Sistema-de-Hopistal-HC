package com.hospital.web;

import com.hospital.db.EnfermedadDao;
import com.hospital.db.PacienteDao;
import com.hospital.model.Enfermedad;
import com.hospital.model.Paciente;
import com.hospital.template.TemplateEngine;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import static com.hospital.web.PacienteController.enc;

public class EnfermedadController {

    private final EnfermedadDao enfermedadDao = new EnfermedadDao();
    private final PacienteDao pacienteDao = new PacienteDao();
    private final TemplateEngine engine;

    public EnfermedadController(TemplateEngine engine) {
        this.engine = engine;
    }

    public void register(Router router) {
        router.get("/pacientes/{id}/enfermedades/nueva", this::form);
        router.post("/pacientes/{id}/enfermedades/nueva", this::guardar);
        router.post("/enfermedades/{id}/eliminar", this::eliminar);
    }

    private void form(RequestContext ctx) throws Exception {
        long pacienteId = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(pacienteId);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }

        Map<String, Object> data = Views.baseData("Nueva enfermedad / antecedente", "pacientes", ctx);
        data.put("paciente", Views.pacienteResumen(op.get()));
        data.put("nombre", "");
        data.put("fechaDiagnostico", "");
        data.put("observaciones", "");
        data.put("estadoOptionsHtml", Catalogos.optionsHtml(Catalogos.ESTADO_ENFERMEDAD, "ACTIVA"));
        ctx.html(200, engine.render("enfermedad-form", data));
    }

    private void guardar(RequestContext ctx) throws Exception {
        long pacienteId = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(pacienteId);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }

        String nombre = ctx.param("nombre", "").trim();
        String estado = ctx.param("estado", "ACTIVA");

        if (nombre.isEmpty() || !Catalogos.isValid(Catalogos.ESTADO_ENFERMEDAD, estado)) {
            Map<String, Object> data = Views.baseData("Nueva enfermedad / antecedente", "pacientes", ctx);
            data.put("paciente", Views.pacienteResumen(op.get()));
            data.put("flashError", "Indica el nombre de la enfermedad y un estado válido.");
            data.put("nombre", nombre);
            data.put("fechaDiagnostico", ctx.param("fechaDiagnostico", ""));
            data.put("observaciones", ctx.param("observaciones", ""));
            data.put("estadoOptionsHtml", Catalogos.optionsHtml(Catalogos.ESTADO_ENFERMEDAD, estado));
            ctx.html(200, engine.render("enfermedad-form", data));
            return;
        }

        Enfermedad e = new Enfermedad();
        e.setPacienteId(pacienteId);
        e.setNombre(nombre);
        String fecha = ctx.param("fechaDiagnostico", "");
        if (!fecha.isBlank()) {
            try { e.setFechaDiagnostico(LocalDate.parse(fecha)); } catch (DateTimeParseException ignored) {}
        }
        e.setEstado(estado);
        e.setObservaciones(ctx.param("observaciones", "").trim());
        enfermedadDao.crear(e);

        ctx.redirect("/pacientes/" + pacienteId + "?ok=" + enc("Enfermedad registrada correctamente."));
    }

    private void eliminar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Optional<Enfermedad> e = enfermedadDao.buscarPorId(id);
        if (e.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Registro no encontrado.")); return; }
        long pacienteId = e.get().getPacienteId();
        enfermedadDao.eliminar(id);
        ctx.redirect("/pacientes/" + pacienteId + "?ok=" + enc("Enfermedad eliminada."));
    }
}
