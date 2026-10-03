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

    private final EnfermedadDao enfermedadDao;
    private final PacienteDao pacienteDao;
    private final TemplateEngine templateEngine;

    public EnfermedadController(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.enfermedadDao = new EnfermedadDao();
        this.pacienteDao = new PacienteDao();
    }

    public void registrarRutas(Router router) {
        router.get("/pacientes/{id}/enfermedades/nueva", this::mostrarFormularioNuevaEnfermedad);
        router.post("/pacientes/{id}/enfermedades/nueva", this::procesarGuardado);
        router.post("/enfermedades/{id}/eliminar", this::eliminarEnfermedad);
    }

    private void mostrarFormularioNuevaEnfermedad(RequestContext context) throws Exception {
        long idPaciente = context.pathLong("id");
        Optional<Paciente> pacienteOpt = pacienteDao.buscarPorId(idPaciente);

        if (pacienteOpt.isEmpty()) {
            context.redirect("/pacientes?error=" + enc("Paciente no encontrado."));
            return;
        }

        Map<String, Object> modelo = Views.baseData("Nueva enfermedad / antecedente", "pacientes", context);
        modelo.put("paciente", Views.pacienteResumen(pacienteOpt.get()));
        modelo.put("nombre", "");
        modelo.put("fechaDiagnostico", "");
        modelo.put("observaciones", "");
        modelo.put("estadoOptionsHtml", Catalogos.generarOpcionesHtml(Catalogos.ESTADO_ENFERMEDAD, "ACTIVA"));

        context.html(200, templateEngine.render("enfermedad-form", modelo));
    }

    private void procesarGuardado(RequestContext context) throws Exception {
        long idPaciente = context.pathLong("id");
        Optional<Paciente> pacienteOpt = pacienteDao.buscarPorId(idPaciente);

        if (pacienteOpt.isEmpty()) {
            context.redirect("/pacientes?error=" + enc("Paciente no encontrado."));
            return;
        }

        String nombre = context.param("nombre", "").trim();
        String estado = context.param("estado", "ACTIVA");

        if (nombre.isEmpty() || !Catalogos.esValido(Catalogos.ESTADO_ENFERMEDAD, estado)) {
            Map<String, Object> modelo = Views.baseData("Nueva enfermedad / antecedente", "pacientes", context);
            modelo.put("paciente", Views.pacienteResumen(pacienteOpt.get()));
            modelo.put("flashError", "Indica el nombre de la enfermedad y un estado válido.");
            modelo.put("nombre", nombre);
            modelo.put("fechaDiagnostico", context.param("fechaDiagnostico", ""));
            modelo.put("observaciones", context.param("observaciones", ""));
            modelo.put("estadoOptionsHtml", Catalogos.generarOpcionesHtml(Catalogos.ESTADO_ENFERMEDAD, estado));

            context.html(200, templateEngine.render("enfermedad-form", modelo));
            return;
        }

        Enfermedad nuevaEnfermedad = new Enfermedad();
        nuevaEnfermedad.setPacienteId(idPaciente);
        nuevaEnfermedad.setNombre(nombre);

        String entradaFecha = context.param("fechaDiagnostico", "");
        if (!entradaFecha.isBlank()) {
            try {
                nuevaEnfermedad.setFechaDiagnostico(LocalDate.parse(entradaFecha));
            } catch (DateTimeParseException ignored) {
                // Si el formato es inválido, se omite el seteo de la fecha
            }
        }

        nuevaEnfermedad.setEstado(estado);
        nuevaEnfermedad.setObservaciones(context.param("observaciones", "").trim());

        enfermedadDao.crear(nuevaEnfermedad);

        context.redirect("/pacientes/" + idPaciente + "?ok=" + enc("Enfermedad registrada correctamente."));
    }

    private void eliminarEnfermedad(RequestContext context) throws Exception {
        long idEnfermedad = context.pathLong("id");
        Optional<Enfermedad> enfermedadOpt = enfermedadDao.buscarPorId(idEnfermedad);

        if (enfermedadOpt.isEmpty()) {
            context.redirect("/pacientes?error=" + enc("Registro no encontrado."));
            return;
        }

        long idPaciente = enfermedadOpt.get().getPacienteId();
        enfermedadDao.eliminar(idEnfermedad);

        context.redirect("/pacientes/" + idPaciente + "?ok=" + enc("Enfermedad eliminada."));
    }
}
