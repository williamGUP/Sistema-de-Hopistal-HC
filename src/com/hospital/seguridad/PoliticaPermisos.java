package com.hospital.seguridad;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tabla que asocia cada ruta HTTP del sistema con el permiso que se necesita
 * para acceder a ella. Es la fuente de verdad del control de acceso:
 *
 *  - El filtro de seguridad la consulta en cada peticion ({@link #requerido}).
 *  - Las plantillas la consultan para mostrar u ocultar botones y enlaces
 *    ({@link #permisoDeRuta}).
 *
 * De este modo la interfaz y la seguridad nunca se contradicen: si un boton
 * aparece, es porque el permiso existe; y si el permiso no existe, la ruta
 * esta bloqueada aunque alguien la escriba a mano en la barra del navegador.
 */
public final class PoliticaPermisos {

    /** Una regla de la politica. */
    public static final class Regla {
        private final String metodo;
        private final Pattern patron;
        private final String descripcion;
        private final Permiso permiso;      // null = ruta publica (no requiere sesion)
        private final boolean soloSesion;    // true = requiere iniciar sesion pero ningun permiso concreto

        Regla(String metodo, String ruta, String descripcion, Permiso permiso, boolean soloSesion) {
            this.metodo = metodo;
            this.patron = compilar(ruta);
            this.descripcion = descripcion;
            this.permiso = permiso;
            this.soloSesion = soloSesion;
        }

        public String descripcion() { return descripcion; }
        public Permiso permiso() { return permiso; }
        public boolean publica() { return permiso == null && !soloSesion; }
        public boolean requiereSesion() { return !publica(); }
    }

    private static final List<Regla> REGLAS = new ArrayList<>();

    private PoliticaPermisos() {}

    private static void registrar(String metodo, String ruta, String descripcion, Permiso permiso) {
        REGLAS.add(new Regla(metodo, ruta, descripcion, permiso, false));
    }

    private static void publica(String metodo, String ruta, String descripcion) {
        REGLAS.add(new Regla(metodo, ruta, descripcion, null, false));
    }

    private static void soloSesion(String metodo, String ruta, String descripcion) {
        REGLAS.add(new Regla(metodo, ruta, descripcion, null, true));
    }

    static {
        // ---- Rutas publicas: se pueden pedir sin iniciar sesion ----
        publica("GET", "/login", "Pantalla de inicio de sesion");
        publica("POST", "/login", "Envio del formulario de acceso");

        // ---- Pagina de inicio y paciente: lectura y escritura ----
        soloSesion("GET", "/", "Portada del sistema");
        registrar("GET", "/pacientes", "Listado y busqueda de pacientes", Permiso.PACIENTE_VER);
        registrar("GET", "/pacientes/nuevo", "Formulario de nuevo paciente", Permiso.PACIENTE_CREAR);
        registrar("POST", "/pacientes/nuevo", "Alta de paciente", Permiso.PACIENTE_CREAR);
        registrar("GET", "/pacientes/{id}", "Historia clinica del paciente", Permiso.PACIENTE_VER);
        registrar("GET", "/pacientes/{id}/editar", "Formulario de edicion de paciente", Permiso.PACIENTE_EDITAR);
        registrar("POST", "/pacientes/{id}/editar", "Actualizacion de datos del paciente", Permiso.PACIENTE_EDITAR);
        registrar("POST", "/pacientes/{id}/eliminar", "Baja de paciente", Permiso.PACIENTE_ELIMINAR);

        // ---- Consultas medicas ----
        registrar("GET", "/pacientes/{id}/consultas/nueva", "Formulario de nueva consulta", Permiso.CONSULTA_CREAR);
        registrar("POST", "/pacientes/{id}/consultas/nueva", "Registro de consulta medica", Permiso.CONSULTA_CREAR);
        registrar("POST", "/consultas/{id}/eliminar", "Eliminacion de consulta medica", Permiso.CONSULTA_ELIMINAR);

        // ---- Enfermedades y antecedentes ----
        registrar("GET", "/pacientes/{id}/enfermedades/nueva", "Formulario de nueva enfermedad", Permiso.ENFERMEDAD_CREAR);
        registrar("POST", "/pacientes/{id}/enfermedades/nueva", "Registro de enfermedad", Permiso.ENFERMEDAD_CREAR);
        registrar("POST", "/enfermedades/{id}/eliminar", "Eliminacion de enfermedad", Permiso.ENFERMEDAD_ELIMINAR);

        // ---- Operaciones quirurgicas ----
        registrar("GET", "/pacientes/{id}/operaciones/nueva", "Formulario de nueva operacion", Permiso.OPERACION_CREAR);
        registrar("POST", "/pacientes/{id}/operaciones/nueva", "Registro de operacion", Permiso.OPERACION_CREAR);
        registrar("POST", "/operaciones/{id}/eliminar", "Eliminacion de operacion", Permiso.OPERACION_ELIMINAR);

        // ---- Sesion y cuenta propia ----
        soloSesion("GET", "/logout", "Confirmacion de cierre de sesion");
        soloSesion("POST", "/logout", "Cierre de sesion");
        soloSesion("GET", "/mi-cuenta", "Datos de la cuenta propia");
        soloSesion("POST", "/mi-cuenta", "Cambio de contrasena propia");

        // ---- Administracion de usuarios (solo administrador general) ----
        registrar("GET", "/usuarios", "Listado de usuarios", Permiso.USUARIO_VER);
        registrar("GET", "/usuarios/nuevo", "Formulario de nuevo usuario", Permiso.USUARIO_CREAR);
        registrar("POST", "/usuarios/nuevo", "Alta de usuario", Permiso.USUARIO_CREAR);
        registrar("GET", "/usuarios/{id}/editar", "Formulario de edicion de usuario", Permiso.USUARIO_EDITAR);
        registrar("POST", "/usuarios/{id}/editar", "Edicion de usuario (rol, estado, contrasena)", Permiso.USUARIO_EDITAR);
        registrar("POST", "/usuarios/{id}/eliminar", "Desactivacion de usuario", Permiso.USUARIO_ELIMINAR);

        // ---- Bitacora de auditoria (solo administrador general) ----
        registrar("GET", "/auditoria", "Bitacora de accesos y operaciones", Permiso.AUDITORIA_VER);
    }

    /**
     * Devuelve el permiso exigido por una peticion, o null si la ruta es
     * publica o solo requiere sesion. Si la ruta no aparece en la politica,
     * devuelve null igualmente (quien la controle internamente seguira siendo
     * el Router); las rutas de esta aplicacion estan todas declaradas arriba.
     */
    public static Permiso requerido(String metodo, String ruta) {
        Regla r = buscar(metodo, ruta);
        return r == null ? null : r.permiso();
    }

    /** La regla que aplica a una ruta, o null si ninguna coincide. */
    public static Regla regla(String metodo, String ruta) {
        return buscar(metodo, ruta);
    }

    public static boolean esPublica(String metodo, String ruta) {
        Regla r = buscar(metodo, ruta);
        return r != null && r.publica();
    }

    private static Regla buscar(String metodo, String ruta) {
        if (metodo == null || ruta == null) return null;
        for (Regla r : REGLAS) {
            if (!r.metodo.equalsIgnoreCase(metodo)) continue;
            if (r.patron.matcher(ruta).matches()) return r;
        }
        return null;
    }

    /** Permiso asociado a una ruta GET (lo usan las plantillas para pintar botones). */
    public static Permiso permisoDeRuta(String ruta) {
        return requerido("GET", ruta);
    }

    /** Todas las reglas, para la pantalla de documentacion de permisos. */
    public static List<Regla> reglas() {
        return List.copyOf(REGLAS);
    }

    /** Convierte "/pacientes/{id}/editar" en la expresion regular que hace coincidir "/pacientes/7/editar". */
    private static Pattern compilar(String ruta) {
        StringBuilder sb = new StringBuilder("^");
        for (String segmento : ruta.split("/")) {
            if (segmento.isEmpty()) continue;
            sb.append('/');
            if (segmento.startsWith("{") && segmento.endsWith("}")) {
                String nombre = segmento.substring(1, segmento.length() - 1);
                // "id" solo admite numeros: evita que /pacientes/nuevo se
                // confunda con un identificador al evaluar las reglas.
                if (nombre.endsWith("id") || nombre.equals("id")) {
                    sb.append("(\\d+)");
                } else {
                    sb.append("([^/]+)");
                }
            } else {
                sb.append(Pattern.quote(segmento));
            }
        }
        if (sb.length() == 1) sb.append('/');
        sb.append("/?$");
        return Pattern.compile(sb.toString());
    }
}