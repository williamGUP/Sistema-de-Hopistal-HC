package com.hospital.web;

import com.hospital.db.ConsultaDao;
import com.hospital.db.PacienteDao;
import com.hospital.model.Consulta;
import com.hospital.model.Paciente;
import com.hospital.template.TemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.hospital.web.PacienteController.enc;

public class ConsultaController {

    private final ConsultaDao consultaDao;
    private final PacienteDao pacienteDao;
    private final TemplateEngine templateEngine;

    public ConsultaController(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.consultaDao = new ConsultaDao();
        this.pacienteDao = new PacienteDao();
    }

    public void registrarRutas(Router router) {
        router.get("/pacientes/{id}/consultas/nueva", this::mostrarFormularioNuevaConsulta);
        router.post("/pacientes/{id}/consultas/nueva", this::procesarGuardado);
        router.post("/consultas/{id}/eliminar", this::eliminarConsulta);
    }

    private void mostrarFormularioNuevaConsulta(RequestContext context) throws Exception {
        long idPaciente = context.pathLong("id");
        Optional<Paciente> pacienteOpt = pacienteDao.buscarPorId(idPaciente);

        if (pacienteOpt.isEmpty()) {
            context.redirect("/pacientes?error=" + enc("Paciente no encontrado."));
            return;
        }

        Map<String, Object> modelo = Views.baseData("Nueva consulta", "pacientes", context);
        modelo.put("paciente", Views.pacienteResumen(pacienteOpt.get()));
        
        // Inicialización de campos vacíos para el formulario
        String[] campos = {"motivo", "medico", "sintomas", "diagnostico", "tratamiento", "observaciones"};
        for (String campo : campos) {
            modelo.put(campo, "");
        }

        context.html(200, templateEngine.render("consulta-form", modelo));
    }

    private void procesarGuardado(RequestContext context) throws Exception {
        long idPaciente = context.pathLong("id");
        Optional<Paciente> pacienteOpt = pacienteDao.buscarPorId(idPaciente);

        if (pacienteOpt.isEmpty()) {
            context.redirect("/pacientes?error=" + enc("Paciente no encontrado."));
            return;
        }

        String motivo = context.param("motivo", "").trim();
        String sintomas = context.param("sintomas", "").trim();
        String diagnostico = context.param("diagnostico", "").trim();

        if (motivo.isEmpty() || sintomas.isEmpty() || diagnostico.isEmpty()) {
            Map<String, Object> modelo = Views.baseData("Nueva consulta", "pacientes", context);
            modelo.put("paciente", Views.pacienteResumen(pacienteOpt.get()));
            modelo.put("flashError", "Completa al menos motivo, síntomas y diagnóstico.");
            modelo.put("medico", context.param("medico", "").trim());
            modelo.put("motivo", motivo);
            modelo.put("sintomas", sintomas);
            modelo.put("diagnostico", diagnostico);
            modelo.put("tratamiento", context.param("tratamiento", "").trim());
            modelo.put("observaciones", context.param("observaciones", "").trim());

            context.html(200, templateEngine.render("consulta-form", modelo));
            return;
        }

        Consulta nuevaConsulta = new Consulta();
        nuevaConsulta.setPacienteId(idPaciente);

        String entradaFecha = context.param("fecha", "");
        LocalDateTime fechaConsulta;
        try {
            fechaConsulta = entradaFecha.isBlank() ? LocalDateTime.now() : LocalDateTime.parse(entradaFecha);
        } catch (DateTimeParseException e) {
            fechaConsulta = LocalDateTime.now();
        }

        nuevaConsulta.setFecha(fechaConsulta);
        nuevaConsulta.setMedico(context.param("medico", "").trim());
        nuevaConsulta.setMotivo(motivo);
        nuevaConsulta.setSintomas(sintomas);
        nuevaConsulta.setDiagnostico(diagnostico);
        nuevaConsulta.setTratamiento(context.param("tratamiento", "").trim());
        nuevaConsulta.setObservaciones(context.param("observaciones", "").trim());

        consultaDao.crear(nuevaConsulta);

        context.redirect("/pacientes/" + idPaciente + "?ok=" + enc("Consulta registrada correctamente."));
    }

    private void eliminarConsulta(RequestContext context) throws Exception {
        long idConsulta = context.pathLong("id");
        Optional<Consulta> consultaOpt = consultaDao.buscarPorId(idConsulta);

        if (consultaOpt.isEmpty()) {
            context.redirect("/pacientes?error=" + enc("Consulta no encontrada."));
            return;
        }

        long idPaciente = consultaOpt.get().getPacienteId();
        consultaDao.eliminar(idConsulta);

        context.redirect("/pacientes/" + idPaciente + "?ok=" + enc("Consulta eliminada."));
    }
}
