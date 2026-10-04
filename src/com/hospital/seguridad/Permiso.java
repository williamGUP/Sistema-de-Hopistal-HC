package com.hospital.seguridad;

/**
 * Permisos atomicos del sistema. Cada permiso corresponde a una accion CRUD
 * sobre un recurso concreto (paciente, consulta, enfermedad, operacion, usuario)
 * mas la consulta de la bitacora de auditoria.
 *
 * Esta enumeracion es la fuente de verdad del modelo de permisos: los roles
 * (ver {@link Rol}) conceden un conjunto de estos permisos y
 * {@link PoliticaPermisos} los asocia a las rutas HTTP del sistema.
 */
public enum Permiso {

    PACIENTE_VER("paciente.ver", "paciente", "ver", "Ver",
            "Consultar el listado de pacientes y abrir su historia clinica."),

    PACIENTE_CREAR("paciente.crear", "paciente", "crear", "Registrar",
            "Registrar pacientes nuevos y asignar su numero de historia clinica."),

    PACIENTE_EDITAR("paciente.editar", "paciente", "editar", "Editar",
            "Modificar los datos generales de un paciente."),

    PACIENTE_ELIMINAR("paciente.eliminar", "paciente", "eliminar", "Dar de baja",
            "Dar de baja un paciente (baja logica, no borra la informacion clinica)."),

    CONSULTA_VER("consulta.ver", "consulta", "ver", "Ver",
            "Leer las consultas medicas registradas en la historia clinica."),

    CONSULTA_CREAR("consulta.crear", "consulta", "crear", "Registrar",
            "Registrar consultas medicas: motivo, sintomas, diagnostico y tratamiento."),

    CONSULTA_ELIMINAR("consulta.eliminar", "consulta", "eliminar", "Eliminar",
            "Eliminar una consulta medica del historial."),

    ENFERMEDAD_VER("enfermedad.ver", "enfermedad", "ver", "Ver",
            "Leer las enfermedades y antecedentes de un paciente."),

    ENFERMEDAD_CREAR("enfermedad.crear", "enfermedad", "crear", "Registrar",
            "Registrar enfermedades y antecedentes clinicos."),

    ENFERMEDAD_ELIMINAR("enfermedad.eliminar", "enfermedad", "eliminar", "Eliminar",
            "Eliminar un antecedente de enfermedad."),

    OPERACION_VER("operacion.ver", "operacion", "ver", "Ver",
            "Leer las operaciones quirurgicas registradas."),

    OPERACION_CREAR("operacion.crear", "operacion", "crear", "Registrar",
            "Registrar operaciones quirurgicas, cirujano y resultado."),

    OPERACION_ELIMINAR("operacion.eliminar", "operacion", "eliminar", "Eliminar",
            "Eliminar el registro de una operacion."),

    USUARIO_VER("usuario.ver", "usuario", "ver", "Ver",
            "Ver el listado de usuarios del sistema y sus roles."),

    USUARIO_CREAR("usuario.crear", "usuario", "crear", "Crear",
            "Crear usuarios y asignarles un rol."),

    USUARIO_EDITAR("usuario.editar", "usuario", "editar", "Editar",
            "Cambiar el rol, activar o desactivar usuarios y restablecer su contrasena."),

    USUARIO_ELIMINAR("usuario.eliminar", "usuario", "eliminar", "Desactivar",
            "Desactivar el acceso de un usuario al sistema."),

    AUDITORIA_VER("auditoria.ver", "auditoria", "ver", "Ver",
            "Consultar la bitacora de accesos, operaciones y intentos fallidos.");

    private final String clave;
    private final String familia;
    private final String accion;
    private final String etiqueta;
    private final String descripcion;

    Permiso(String clave, String familia, String accion, String etiqueta, String descripcion) {
        this.clave = clave;
        this.familia = familia;
        this.accion = accion;
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    /** Identificador estable para base de datos, logs y documentacion (p.ej. "paciente.crear"). */
    public String clave() { return clave; }

    /** Recurso al que pertenece el permiso: "paciente", "consulta", "usuario"... */
    public String familia() { return familia; }

    /** Tipo de operacion CRUD: "ver", "crear", "editar" o "eliminar". */
    public String accion() { return accion; }

    /** Texto corto para la interfaz ("Registrar", "Ver"...). */
    public String etiqueta() { return etiqueta; }

    /** Descripcion funcional, para la matriz de permisos de la documentacion. */
    public String descripcion() { return descripcion; }

    /** Nombre del recurso en mayusculas, usado como etiqueta de agrupacion. */
    public String familiaLegible() {
        switch (familia) {
            case "paciente":   return "Pacientes";
            case "consulta":   return "Consultas medicas";
            case "enfermedad": return "Enfermedades";
            case "operacion":  return "Operaciones";
            case "usuario":    return "Usuarios";
            case "auditoria":  return "Auditoria";
            default:           return familia;
        }
    }

    public static Permiso porClave(String clave) {
        if (clave == null) return null;
        for (Permiso p : values()) {
            if (p.clave.equalsIgnoreCase(clave)) return p;
        }
        return null;
    }
}