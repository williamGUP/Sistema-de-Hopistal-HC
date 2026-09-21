package com.hospital.web;

import com.hospital.template.TemplateEngine;

/**
 * Módulo de listas estáticas para la generación de selectores HTML y su validación backend.
 */
public final class Catalogos {

    // Constructor privado para evitar la instanciación
    private Catalogos() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    // Opciones para formularios
    public static final String[][] SEXO = {
        {"MASCULINO", "Masculino"},
        {"FEMENINO", "Femenino"},
        {"OTRO", "Otro"}
    };

    public static final String[][] GRUPO_SANGUINEO = {
        {"", "No registrado"},
        {"O+", "O+"},
        {"O-", "O-"},
        {"A+", "A+"},
        {"A-", "A-"},
        {"B+", "B+"},
        {"B-", "B-"},
        {"AB+", "AB+"},
        {"AB-", "AB-"}
    };

    public static final String[][] ESTADO_ENFERMEDAD = {
        {"ACTIVA", "Activa"},
        {"CONTROLADA", "Controlada"},
        {"CURADA", "Curada"}
    };

    public static final String[][] RESULTADO_OPERACION = {
        {"EXITOSA", "Exitosa"},
        {"CON_COMPLICACIONES", "Con complicaciones"},
        {"FALLIDA", "Fallida"}
    };

    /**
     * Construye las etiquetas <option> para un <select> HTML resaltando la opción seleccionada.
     */
    public static String generarOpcionesHtml(final String[][] catalogo, final String valorSeleccionado) {
        StringBuilder htmlOptions = new StringBuilder();
        
        for (String[] opcion : catalogo) {
            String clave = opcion[0];
            String texto = opcion[1];

            htmlOptions.append("<option value=\"")
                       .append(TemplateEngine.escapeHtml(clave))
                       .append("\"");

            if (clave.equals(valorSeleccionado)) {
                htmlOptions.append(" selected");
            }

            htmlOptions.append(">")
                       .append(TemplateEngine.escapeHtml(texto))
                       .append("</option>");
        }

        return htmlOptions.toString();
    }

    /**
     * Comprueba si un valor enviado desde el formulario existe dentro del catálogo permitido.
     */
    public static boolean esValido(final String[][] catalogo, final String valor) {
        if (valor == null) {
            return false;
        }
        for (String[] opcion : catalogo) {
            if (opcion[0].equals(valor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retorna el texto legible asociado a una clave del catálogo.
     */
    public static String obtenerEtiqueta(final String[][] catalogo, final String clave) {
        for (String[] opcion : catalogo) {
            if (opcion[0].equals(clave)) {
                return opcion[1];
            }
        }
        return clave != null ? clave : "";
    }
}
