package com.hospital.web;

import com.hospital.template.TemplateEngine;

/** Listas de valores fijos usados en los formularios (selects), con validación server-side. */
public final class Catalogos {
    private Catalogos() {}

    public static final String[][] SEXO = {
        {"MASCULINO", "Masculino"}, {"FEMENINO", "Femenino"}, {"OTRO", "Otro"}
    };
    public static final String[][] GRUPO_SANGUINEO = {
        {"", "No registrado"}, {"O+", "O+"}, {"O-", "O-"}, {"A+", "A+"}, {"A-", "A-"},
        {"B+", "B+"}, {"B-", "B-"}, {"AB+", "AB+"}, {"AB-", "AB-"}
    };
    public static final String[][] ESTADO_ENFERMEDAD = {
        {"ACTIVA", "Activa"}, {"CONTROLADA", "Controlada"}, {"CURADA", "Curada"}
    };
    public static final String[][] RESULTADO_OPERACION = {
        {"EXITOSA", "Exitosa"}, {"CON_COMPLICACIONES", "Con complicaciones"}, {"FALLIDA", "Fallida"}
    };

    public static String optionsHtml(String[][] pairs, String selected) {
        String sel = selected == null ? "" : selected;
        StringBuilder sb = new StringBuilder();
        for (String[] pair : pairs) {
            sb.append("<option value=\"").append(TemplateEngine.escapeHtml(pair[0])).append('"');
            if (pair[0].equals(sel)) sb.append(" selected");
            sb.append('>').append(TemplateEngine.escapeHtml(pair[1])).append("</option>");
        }
        return sb.toString();
    }

    public static boolean isValid(String[][] pairs, String value) {
        for (String[] pair : pairs) if (pair[0].equals(value)) return true;
        return false;
    }

    public static String labelFor(String[][] pairs, String value) {
        for (String[] pair : pairs) if (pair[0].equals(value)) return pair[1];
        return value == null ? "" : value;
    }
}
