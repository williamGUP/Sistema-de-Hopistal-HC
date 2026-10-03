package com.hospital.web;

import com.hospital.db.OperacionDao;
import com.hospital.db.PacienteDao;
import com.hospital.model.Operacion;
import com.hospital.model.Paciente;
import com.hospital.template.TemplateEngine;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import static com.hospital.web.PacienteController.enc;

public class OperacionController {

    private final OperacionDao operacionDao = new OperacionDao();
    private final PacienteDao pacienteDao = new PacienteDao();
    private final TemplateEngine engine;

    public OperacionController(TemplateEngine engine) {
        this.engine = engine;
    }

    public void register(Router router) {
        router.get("/pacientes/{id}/operaciones/nueva", this::form);
        router.post("/pacientes/{id}/operaciones/nueva", this::guardar);
        router.post("/operaciones/{id}/eliminar", this::eliminar);
    }

    private void form(RequestContext ctx) throws Exception {
        long pacienteId = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(pacienteId);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }

        Map<String, Object> data = Views.baseData("Nueva operación", "pacientes", ctx);
        data.put("paciente", Views.pacienteResumen(op.get()));
        data.put("tipoOperacion", "");
        data.put("cirujano", "");
        data.put("fecha", "");
        data.put("observaciones", "");
        data.put("resultadoOptionsHtml", Catalogos.optionsHtml(Catalogos.RESULTADO_OPERACION, "EXITOSA"));
        ctx.html(200, engine.render("operacion-form", data));
    }

    private void guardar(RequestContext ctx) throws Exception {
        long pacienteId = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(pacienteId);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }

        String tipoOperacion = ctx.param("tipoOperacion", "").trim();
        String resultado = ctx.param("resultado", "EXITOSA");
        String fechaStr = ctx.param("fecha", "").trim();

        boolean fechaValida = true;
        LocalDate fecha = null;
        if (!fechaStr.isBlank()) {
            try { fecha = LocalDate.parse(fechaStr); } catch (DateTimeParseException e) { fechaValida = false; }
        }

        if (tipoOperacion.isEmpty() || fechaStr.isEmpty() || !fechaValida || !Catalogos.isValid(Catalogos.RESULTADO_OPERACION, resultado)) {
            Map<String, Object> data = Views.baseData("Nueva operación", "pacientes", ctx);
            data.put("paciente", Views.pacienteResumen(op.get()));
            data.put("flashError", "Indica el tipo de operación, una fecha válida y un resultado.");
            data.put("tipoOperacion", tipoOperacion);
            data.put("cirujano", ctx.param("cirujano", ""));
            data.put("fecha", fechaStr);
            data.put("observaciones", ctx.param("observaciones", ""));
            data.put("resultadoOptionsHtml", Catalogos.optionsHtml(Catalogos.RESULTADO_OPERACION, resultado));
            ctx.html(200, engine.render("operacion-form", data));
            return;
        }

        Operacion o = new Operacion();
        o.setPacienteId(pacienteId);
        o.setTipoOperacion(tipoOperacion);
        o.setCirujano(ctx.param("cirujano", "").trim());
        o.setFecha(fecha);
        o.setResultado(resultado);
        o.setObservaciones(ctx.param("observaciones", "").trim());
        operacionDao.crear(o);

        ctx.redirect("/pacientes/" + pacienteId + "?ok=" + enc("Operación registrada correctamente."));
    }

    private void eliminar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Optional<Operacion> o = operacionDao.buscarPorId(id);
        if (o.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Registro no encontrado.")); return; }
        long pacienteId = o.get().getPacienteId();
        operacionDao.eliminar(id);
        ctx.redirect("/pacientes/" + pacienteId + "?ok=" + enc("Operación eliminada."));
    }
}
