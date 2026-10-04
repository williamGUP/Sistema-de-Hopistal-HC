package com.hospital.seguridad;

import com.hospital.model.Usuario;
import com.hospital.template.TemplateEngine;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Motor de plantillas que, antes de renderizar, inyecta en todas las paginas las
 * variables de la sesion actual: quien esta conectado, con que rol, que
 * permisos tiene y el token CSRF de su sesion.
 *
 * Se extiende el motor original en lugar de duplicarlo, de modo que las
 * plantillas existentes ({{pageTitle}}, {{#flashOk}}...) siguen funcionando
 * igual y las nuevas solo tienen que usar {{usuarioActual}}, {{rolEtiqueta}},
 * {{puedeUsuarios}}, {{csrfToken}}, etc.
 *
 * Como no hay que tocar ni Views ni los controladores, la seguridad queda
 * aplicada de forma homogenea en todas las pantallas del sistema.
 */
public final class MotorConSesion extends TemplateEngine {

    public MotorConSesion(Path templatesDir, boolean cacheEnabled) {
        super(templatesDir, cacheEnabled);
    }

    @Override
    public String render(String templateName, Map<String, Object> data) {
        Map<String, Object> contexto = data == null ? new HashMap<>() : data;
        inyectar(contexto);
        return super.render(templateName, contexto);
    }

    /** Anade al contexto de la plantilla los datos de seguridad de la peticion en curso. */
    static void inyectar(Map<String, Object> data) {
        Usuario u = ContextoActual.usuario();
        Rol rol = ContextoActual.rol();

        data.put("haySesion", u != null);
        data.put("usuarioActual", u == null ? "" : u.getNombreCompleto());
        data.put("usuarioId", u == null || u.getId() == null ? "" : String.valueOf(u.getId()));
        data.put("usernameActual", u == null ? "" : u.getUsername());
        data.put("inicialesUsuario", u == null ? "" : ContextoActual.iniciales());
        data.put("rolActual", rol == null ? "" : rol.clave());
        data.put("rolEtiqueta", rol == null ? "" : rol.etiqueta());
        data.put("rolDescripcion", rol == null ? "" : rol.descripcion());
        data.put("rolCss", rol == null ? "rol-SIN_ROL" : "rol-" + rol.clave());

        // Banderas de permisos que las plantillas usan para mostrar u ocultar
        // acciones. Se calculan aqui para que la UI no pueda contradecir a la
        // politica de seguridad.
        data.put("puedePacientes", ContextoActual.puede(Permiso.PACIENTE_VER));
        data.put("puedeCrearPaciente", ContextoActual.puede(Permiso.PACIENTE_CREAR));
        data.put("puedeEditarPaciente", ContextoActual.puede(Permiso.PACIENTE_EDITAR));
        data.put("puedeEliminarPaciente", ContextoActual.puede(Permiso.PACIENTE_ELIMINAR));
        data.put("puedeCrearConsulta", ContextoActual.puede(Permiso.CONSULTA_CREAR));
        data.put("puedeEliminarConsulta", ContextoActual.puede(Permiso.CONSULTA_ELIMINAR));
        data.put("puedeCrearEnfermedad", ContextoActual.puede(Permiso.ENFERMEDAD_CREAR));
        data.put("puedeEliminarEnfermedad", ContextoActual.puede(Permiso.ENFERMEDAD_ELIMINAR));
        data.put("puedeCrearOperacion", ContextoActual.puede(Permiso.OPERACION_CREAR));
        data.put("puedeEliminarOperacion", ContextoActual.puede(Permiso.OPERACION_ELIMINAR));
        data.put("puedeUsuarios", ContextoActual.puede(Permiso.USUARIO_VER));
        data.put("puedeCrearUsuario", ContextoActual.puede(Permiso.USUARIO_CREAR));
        data.put("puedeEditarUsuario", ContextoActual.puede(Permiso.USUARIO_EDITAR));
        data.put("puedeEliminarUsuario", ContextoActual.puede(Permiso.USUARIO_ELIMINAR));
        data.put("puedeAuditar", ContextoActual.puede(Permiso.AUDITORIA_VER));

        // Token CSRF de la sesion (los formularios nuevos lo incluyen como
        // campo oculto; ademas el filtro valida el origen de cada POST).
        Sesion sesion = ContextoActual.sesion();
        data.put("csrfToken", sesion == null ? "" : sesion.tokenCsrf());
        data.put("sesionMinutosRestantes", sesion == null ? 0 : sesion.minutosRestantes(java.time.Instant.now()));

        // Aviso de contrasena de fabrica sin cambiar.
        data.put("avisoContrasenaDemo", false);
        if (u != null && u.getId() != null) {
            try {
                data.put("avisoContrasenaDemo", UsuariosDao.usaContrasenaDeDemo(u.getId()));
            } catch (Exception ignored) {
                // Si falla la comprobacion, se oculta el aviso.
            }
        }
    }
}