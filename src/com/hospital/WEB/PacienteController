package com.hospital.web;

import com.hospital.db.ConsultaDao;
import com.hospital.db.EnfermedadDao;
import com.hospital.db.OperacionDao;
import com.hospital.db.PacienteDao;
import com.hospital.model.Paciente;
import com.hospital.template.TemplateEngine;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class PacienteController {

    /** DNI peruano: 8 dígitos. Si tu país usa otro formato de documento, ajusta este patrón. */
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");

    private final PacienteDao pacienteDao = new PacienteDao();
    private final ConsultaDao consultaDao = new ConsultaDao();
    private final EnfermedadDao enfermedadDao = new EnfermedadDao();
    private final OperacionDao operacionDao = new OperacionDao();
    private final TemplateEngine engine;

    public PacienteController(TemplateEngine engine) {
        this.engine = engine;
    }

    public void register(Router router) {
        router.get("/", this::home);
        router.get("/pacientes", this::lista);
        router.get("/pacientes/nuevo", this::formNuevo);
        router.post("/pacientes/nuevo", this::guardarNuevo);
        router.get("/pacientes/{id}", this::detalle);
        router.get("/pacientes/{id}/editar", this::formEditar);
        router.post("/pacientes/{id}/editar", this::guardarEditar);
        router.post("/pacientes/{id}/eliminar", this::eliminar);
    }

    private void home(RequestContext ctx) throws Exception {
        Map<String, Object> data = Views.baseData("Inicio", "inicio", ctx);
        data.put("totalPacientes", pacienteDao.contarActivos());

        List<Map<String, Object>> ultimos = new ArrayList<>();
        for (Paciente p : pacienteDao.ultimosRegistrados(5)) ultimos.add(Views.pacienteResumen(p));
        data.put("ultimosPacientes", ultimos);

        String q = ctx.param("q");
        if (q != null && !q.isBlank()) {
            data.put("buscoAlgo", true);
            data.put("q", q);
            List<Map<String, Object>> resultados = new ArrayList<>();
            for (Paciente p : pacienteDao.buscar(q.trim())) resultados.add(Views.pacienteResumen(p));
            data.put("resultados", resultados);
        } else {
            data.put("q", "");
        }
        ctx.html(200, engine.render("index", data));
    }

    private void lista(RequestContext ctx) throws Exception {
        Map<String, Object> data = Views.baseData("Pacientes", "pacientes", ctx);
        String q = ctx.param("q", "");
        data.put("q", q);
        List<Paciente> pacientes = q.isBlank() ? pacienteDao.listarActivos() : pacienteDao.buscar(q.trim());
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Paciente p : pacientes) filas.add(Views.pacienteResumen(p));
        data.put("pacientes", filas);
        ctx.html(200, engine.render("pacientes-lista", data));
    }

    private void formNuevo(RequestContext ctx) throws Exception {
        Map<String, Object> data = Views.baseData("Nuevo paciente", "nuevo", ctx);
        data.put("esEdicion", false);
        data.put("formAction", "/pacientes/nuevo");
        data.put("sexoOptionsHtml", Catalogos.optionsHtml(Catalogos.SEXO, null));
        data.put("grupoSanguineoOptionsHtml", Catalogos.optionsHtml(Catalogos.GRUPO_SANGUINEO, null));
        ctx.html(200, engine.render("paciente-form", data));
    }

    private void guardarNuevo(RequestContext ctx) throws Exception {
        Paciente p = new Paciente();
        leerCamposFormulario(ctx, p);

        String error = validar(p, null);
        if (error != null) {
            reRenderFormConError(ctx, p, false, "/pacientes/nuevo", error);
            return;
        }
        long id = pacienteDao.crear(p);
        ctx.redirect("/pacientes/" + id + "?ok=" + enc("Paciente registrado correctamente."));
    }

    private void formEditar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(id);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }
        Paciente p = op.get();
        Map<String, Object> data = Views.baseData("Editar paciente", "pacientes", ctx);
        data.put("esEdicion", true);
        data.put("formAction", "/pacientes/" + id + "/editar");
        volcarPacienteEnFormulario(data, p);
        ctx.html(200, engine.render("paciente-form", data));
    }

    private void guardarEditar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(id);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }
        Paciente p = op.get();
        leerCamposFormulario(ctx, p);
        p.setId(id);

        String error = validar(p, id);
        if (error != null) {
            reRenderFormConError(ctx, p, true, "/pacientes/" + id + "/editar", error);
            return;
        }
        pacienteDao.actualizar(p);
        ctx.redirect("/pacientes/" + id + "?ok=" + enc("Datos actualizados correctamente."));
    }

    private void eliminar(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        pacienteDao.desactivar(id);
        ctx.redirect("/pacientes?ok=" + enc("Paciente eliminado del listado activo."));
    }

    private void detalle(RequestContext ctx) throws Exception {
        long id = ctx.pathLong("id");
        Optional<Paciente> op = pacienteDao.buscarPorId(id);
        if (op.isEmpty()) { ctx.redirect("/pacientes?error=" + enc("Paciente no encontrado.")); return; }
        Paciente p = op.get();

        Map<String, Object> data = Views.baseData(p.getNombreCompleto(), "pacientes", ctx);
        data.put("paciente", pacienteCompleto(p));

        List<Map<String, Object>> consultas = new ArrayList<>();
        for (var c : consultaDao.listarPorPaciente(id)) consultas.add(Views.consultaResumen(c));
        data.put("consultas", consultas);

        List<Map<String, Object>> enfermedades = new ArrayList<>();
        for (var e : enfermedadDao.listarPorPaciente(id)) enfermedades.add(Views.enfermedadResumen(e));
        data.put("enfermedades", enfermedades);

        List<Map<String, Object>> operaciones = new ArrayList<>();
        for (var o : operacionDao.listarPorPaciente(id)) operaciones.add(Views.operacionResumen(o));
        data.put("operaciones", operaciones);

        ctx.html(200, engine.render("paciente-detalle", data));
    }

    // ---------- helpers ----------

    private void leerCamposFormulario(RequestContext ctx, Paciente p) {
        p.setDni(ctx.param("dni", "").trim());
        p.setNombres(ctx.param("nombres", "").trim());
        p.setApellidos(ctx.param("apellidos", "").trim());
        p.setSexo(ctx.param("sexo", ""));
        p.setTelefono(ctx.param("telefono", "").trim());
        p.setDireccion(ctx.param("direccion", "").trim());
        p.setEmail(ctx.param("email", "").trim());
        p.setGrupoSanguineo(ctx.param("grupoSanguineo", ""));
        p.setAlergias(ctx.param("alergias", "").trim());
        String fn = ctx.param("fechaNacimiento", "");
        if (!fn.isBlank()) {
            try {
                p.setFechaNacimiento(LocalDate.parse(fn));
            } catch (DateTimeParseException ignored) {
                p.setFechaNacimiento(null);
            }
        }
    }

    /** Devuelve un mensaje de error si algo no es válido, o null si todo está bien. */
    private String validar(Paciente p, Long idActual) throws Exception {
        if (p.getNombres().isEmpty() || p.getApellidos().isEmpty()) {
            return "Completa nombres y apellidos.";
        }
        if (!DNI_PATTERN.matcher(p.getDni()).matches()) {
            return "El DNI debe tener exactamente 8 dígitos numéricos.";
        }
        if (!p.getSexo().isEmpty() && !Catalogos.isValid(Catalogos.SEXO, p.getSexo())) {
            return "El sexo seleccionado no es válido.";
        }
        if (!p.getGrupoSanguineo().isEmpty() && !Catalogos.isValid(Catalogos.GRUPO_SANGUINEO, p.getGrupoSanguineo())) {
            return "El grupo sanguíneo seleccionado no es válido.";
        }
        if (p.getFechaNacimiento() != null && p.getFechaNacimiento().isAfter(LocalDate.now())) {
            return "La fecha de nacimiento no puede ser futura.";
        }
        Optional<Paciente> existente = pacienteDao.buscarPorDni(p.getDni());
        if (existente.isPresent() && (idActual == null || !existente.get().getId().equals(idActual))) {
            return "Ya existe un paciente registrado con el DNI " + p.getDni() + " (" + existente.get().getNombreCompleto() + ").";
        }
        return null;
    }

    private void reRenderFormConError(RequestContext ctx, Paciente p, boolean esEdicion, String formAction, String error) throws Exception {
        Map<String, Object> data = Views.baseData(esEdicion ? "Editar paciente" : "Nuevo paciente", esEdicion ? "pacientes" : "nuevo", ctx);
        data.put("esEdicion", esEdicion);
        data.put("formAction", formAction);
        data.put("flashError", error);
        volcarPacienteEnFormulario(data, p);
        ctx.html(200, engine.render("paciente-form", data));
    }

    private void volcarPacienteEnFormulario(Map<String, Object> data, Paciente p) {
        data.put("dni", Views.nvl(p.getDni()));
        data.put("numeroHistoria", Views.nvl(p.getNumeroHistoria()));
        data.put("nombres", Views.nvl(p.getNombres()));
        data.put("apellidos", Views.nvl(p.getApellidos()));
        data.put("fechaNacimiento", p.getFechaNacimiento() == null ? "" : p.getFechaNacimiento().toString());
        data.put("telefono", Views.nvl(p.getTelefono()));
        data.put("direccion", Views.nvl(p.getDireccion()));
        data.put("email", Views.nvl(p.getEmail()));
        data.put("alergias", Views.nvl(p.getAlergias()));
        data.put("sexoOptionsHtml", Catalogos.optionsHtml(Catalogos.SEXO, p.getSexo()));
        data.put("grupoSanguineoOptionsHtml", Catalogos.optionsHtml(Catalogos.GRUPO_SANGUINEO, p.getGrupoSanguineo()));
    }

    private Map<String, Object> pacienteCompleto(Paciente p) {
        Map<String, Object> m = Views.pacienteResumen(p);
        m.put("fechaNacimiento", p.getFechaNacimiento() == null ? "No registrada" : p.getFechaNacimiento().format(Views.FECHA));
        m.put("telefono", p.getTelefono() == null || p.getTelefono().isEmpty() ? "No registrado" : p.getTelefono());
        m.put("direccion", p.getDireccion() == null || p.getDireccion().isEmpty() ? "No registrada" : p.getDireccion());
        m.put("email", p.getEmail() == null || p.getEmail().isEmpty() ? "No registrado" : p.getEmail());
        m.put("grupoSanguineo", p.getGrupoSanguineo() == null || p.getGrupoSanguineo().isEmpty() ? "No registrado" : p.getGrupoSanguineo());
        m.put("alergias", p.getAlergias() == null || p.getAlergias().isEmpty() ? "Ninguna registrada" : p.getAlergias());
        m.put("fechaRegistro", p.getFechaRegistro() == null ? "" : p.getFechaRegistro().format(Views.FECHA_HORA));
        return m;
    }

    static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
