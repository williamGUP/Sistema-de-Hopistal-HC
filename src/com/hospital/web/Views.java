package com.hospital.web;

import com.hospital.model.Consulta;
import com.hospital.model.Enfermedad;
import com.hospital.model.Operacion;
import com.hospital.model.Paciente;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/** Helpers para construir los mapas de datos que se pasan a las plantillas. */
public final class Views {
    private Views() {}

    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Datos comunes a todas las páginas: título, qué ítem del menú resaltar, y mensajes flash. */
    public static Map<String, Object> baseData(String pageTitle, String activeSection, RequestContext ctx) {
        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", pageTitle);
        data.put("activeInicio", "inicio".equals(activeSection));
        data.put("activePacientes", "pacientes".equals(activeSection));
        data.put("activeNuevo", "nuevo".equals(activeSection));
        String ok = ctx.param("ok");
        String error = ctx.param("error");
        if (ok != null && !ok.isBlank()) data.put("flashOk", ok);
        if (error != null && !error.isBlank()) data.put("flashError", error);
        return data;
    }

    public static Map<String, Object> pacienteResumen(Paciente p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("nombreCompleto", p.getNombreCompleto());
        m.put("dni", p.getDni());
        m.put("numeroHistoria", p.getNumeroHistoria());
        m.put("edad", p.getEdad() < 0 ? "—" : String.valueOf(p.getEdad()));
        m.put("sexo", Catalogos.labelFor(Catalogos.SEXO, p.getSexo()));
        return m;
    }

    public static Map<String, Object> consultaResumen(Consulta c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("fecha", c.getFecha() == null ? "" : c.getFecha().format(FECHA_HORA));
        m.put("medico", nvl(c.getMedico()));
        m.put("motivo", nvl(c.getMotivo()));
        m.put("sintomas", nvl(c.getSintomas()));
        m.put("diagnostico", nvl(c.getDiagnostico()));
        m.put("tratamiento", nvl(c.getTratamiento()));
        m.put("observaciones", nvl(c.getObservaciones()));
        return m;
    }

    public static Map<String, Object> enfermedadResumen(Enfermedad e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("nombre", nvl(e.getNombre()));
        m.put("fechaDiagnostico", e.getFechaDiagnostico() == null ? "" : e.getFechaDiagnostico().format(FECHA));
        m.put("estado", Catalogos.labelFor(Catalogos.ESTADO_ENFERMEDAD, e.getEstado()));
        m.put("estadoRaw", nvl(e.getEstado()));
        m.put("observaciones", nvl(e.getObservaciones()));
        return m;
    }

    public static Map<String, Object> operacionResumen(Operacion o) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", o.getId());
        m.put("fecha", o.getFecha() == null ? "" : o.getFecha().format(FECHA));
        m.put("tipoOperacion", nvl(o.getTipoOperacion()));
        m.put("cirujano", nvl(o.getCirujano()));
        m.put("resultado", Catalogos.labelFor(Catalogos.RESULTADO_OPERACION, o.getResultado()));
        m.put("resultadoRaw", nvl(o.getResultado()));
        m.put("observaciones", nvl(o.getObservaciones()));
        return m;
    }

    static String nvl(String s) {
        return s == null ? "" : s;
    }
}
