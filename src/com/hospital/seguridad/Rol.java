package com.hospital.seguridad;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Los cuatro roles del sistema Hospital HC.
 *
 * El control de acceso es de tipo RBAC (Role-Based Access Control): cada rol
 * tiene associated un conjunto de {@link Permiso} y el filtro de seguridad
 * (ver {@link FiltroSeguridad}) valida cada peticion HTTP contra ese conjunto.
 *
 * Matriz de permisos (resumen; la version completa esta en docs/MATRIZ_PERMISOS.md):
 *
 * <pre>
 *                       ADMIN  DIGITADOR  ENFERMERA  DOCTOR
 *   Pacientes: ver         x        x         x         x
 *              crear       x        x         x         x
 *              editar      x        x         x         x
 *              eliminar    x        -         -         -
 *   Consultas: ver         x        x         x         x
 *              crear       x        -         x         x
 *              eliminar    x        -         x         x
 *   Enfermedades (C/E/E)  x        -         x         x
 *   Operaciones: ver       x        x         x         x
 *              crear       x        -         -         x
 *              eliminar    x        -         -         x
 *   Usuarios (CRUD)        x        -         -         -
 *   Bitacora de auditoria  x        -         -         -
 * </pre>
 */
public enum Rol {

    ADMIN_GENERAL("ADMIN_GENERAL", "Administrador general",
            "Control total del sistema: pacientes, informacion clinica, usuarios y bitacora.") {
        @Override public Set<Permiso> permisos() { return TODOS; }
    },

    DIGITADOR("DIGITADOR", "Digitador",
            "Captura y depuracion de datos de pacientes. Solo lectura de la informacion clinica.") {
        @Override public Set<Permiso> permisos() {
            return EnumSet.of(
                    Permiso.PACIENTE_VER, Permiso.PACIENTE_CREAR, Permiso.PACIENTE_EDITAR,
                    Permiso.CONSULTA_VER,
                    Permiso.ENFERMEDAD_VER,
                    Permiso.OPERACION_VER);
        }
    },

    ENFERMERA("ENFERMERA", "Enfermeria",
            "Seguimiento clinico del paciente: consultas, enfermedades y antecedentes. Sin operaciones quirurgicas.") {
        @Override public Set<Permiso> permisos() {
            return EnumSet.of(
                    Permiso.PACIENTE_VER, Permiso.PACIENTE_CREAR, Permiso.PACIENTE_EDITAR,
                    Permiso.CONSULTA_VER, Permiso.CONSULTA_CREAR, Permiso.CONSULTA_ELIMINAR,
                    Permiso.ENFERMEDAD_VER, Permiso.ENFERMEDAD_CREAR, Permiso.ENFERMEDAD_ELIMINAR,
                    Permiso.OPERACION_VER);
        }
    },

    DOCTOR("DOCTOR", "Medico",
            "Historia clinica completa: consultas, diagnosticos, enfermedades y operaciones quirurgicas.") {
        @Override public Set<Permiso> permisos() {
            return EnumSet.of(
                    Permiso.PACIENTE_VER, Permiso.PACIENTE_CREAR, Permiso.PACIENTE_EDITAR,
                    Permiso.CONSULTA_VER, Permiso.CONSULTA_CREAR, Permiso.CONSULTA_ELIMINAR,
                    Permiso.ENFERMEDAD_VER, Permiso.ENFERMEDAD_CREAR, Permiso.ENFERMEDAD_ELIMINAR,
                    Permiso.OPERACION_VER, Permiso.OPERACION_CREAR, Permiso.OPERACION_ELIMINAR);
        }
    };

    private static final Set<Permiso> TODOS = Collections.unmodifiableSet(EnumSet.allOf(Permiso.class));

    private final String clave;
    private final String etiqueta;
    private final String descripcion;

    Rol(String clave, String etiqueta, String descripcion) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    /** Conjunto de permisos concedidos a este rol. */
    public abstract Set<Permiso> permisos();

    /** Codigo persistido en la columna "rol" de la tabla usuario. */
    public String clave() { return clave; }

    /** Nombre legible para la interfaz. */
    public String etiqueta() { return etiqueta; }

    /** Descripcion del alcance del rol, mostrada en el login y en la matriz de permisos. */
    public String descripcion() { return descripcion; }

    public boolean concede(Permiso permiso) {
        return permiso != null && permisos().contains(permiso);
    }

    public boolean esAdmin() { return this == ADMIN_GENERAL; }

    /** Resuelve un rol a partir del codigo guardado en la base de datos (tolera mayusculas/espacios). */
    public static Rol desdeClave(String clave) {
        if (clave == null) return null;
        String k = clave.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        for (Rol r : values()) {
            if (r.clave.equals(k)) return r;
        }
        // Alias historicos / nombres alternativos que podrian venir de la base de datos.
        switch (k) {
            case "ADMIN": case "ADMINISTRADOR": case "ADMIN_GENERAL": return ADMIN_GENERAL;
            case "DIGITADOR": case "GENERAL": case "USUARIO_GENERAL": return DIGITADOR;
            case "ENFERMERA": case "ENFERMERIA": case "ENFERMERO": return ENFERMERA;
            case "DOCTOR": case "MEDICO": return DOCTOR;
            default: return null;
        }
    }

    /** Alias tolerante para bases de datos que usen nombres en minusculas. */
    public static Rol desdeNombreOClave(String valor) {
        Rol r = desdeClave(valor);
        return r != null ? r : null;
    }

    /** Mapa etiqueta -> rol, para poblar los <select> de la administracion de usuarios. */
    public static Map<String, String> opciones() {
        Map<String, String> m = new LinkedHashMap<>();
        for (Rol r : values()) m.put(r.clave, r.etiqueta);
        return m;
    }

    public static List<Rol> todos() {
        return List.of(values());
    }
}