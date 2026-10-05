package com.hospital;

import com.hospital.db.AppPaths;
import com.hospital.db.Db;
import com.hospital.seguridad.ControladorAcceso;
import com.hospital.seguridad.ControladorAuditoria;
import com.hospital.seguridad.ControladorUsuarios;
import com.hospital.seguridad.FiltroSeguridad;
import com.hospital.seguridad.MotorConSesion;
import com.hospital.seguridad.SeguridadEsquema;
import com.hospital.seguridad.SesionStore;
import com.hospital.template.TemplateEngine;
import com.hospital.web.ConsultaController;
import com.hospital.web.EnfermedadController;
import com.hospital.web.OperacionController;
import com.hospital.web.PacienteController;
import com.hospital.web.Router;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Punto de entrada de la aplicación. Arranca la base de datos, el motor de
 * plantillas y el servidor HTTP embebido (sin frameworks ni dependencias
 * externas aparte del driver de H2).
 *
 * Uso:
 *   java -jar hospital.jar [puerto]
 *
 * Si no se indica puerto, usa 8080. También puede fijarse con la variable
 * de entorno HOSPITAL_PORT o la propiedad de sistema -Dport=NNNN.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("== Sistema de Registro de Pacientes - Hospital ==");

        Db.init();
        SeguridadEsquema.init();

        Path templatesDir = AppPaths.resolveDir("templates");
        Path staticDir = AppPaths.resolveDir("static");
        // MotorConSesion extiende el motor original e inyecta en cada pagina los
        // datos del usuario conectado (rol, permisos y token CSRF).
        TemplateEngine engine = new MotorConSesion(templatesDir, true);

        Router router = new Router(staticDir);
        new ControladorAcceso(engine).register(router);
        new ControladorUsuarios(engine).register(router);
        new ControladorAuditoria(engine).register(router);
        new PacienteController(engine).register(router);
        new ConsultaController(engine).register(router);
        new EnfermedadController(engine).register(router);
        new OperacionController(engine).register(router);

        int port = resolvePort(args);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // El filtro de seguridad envuelve al router: exige sesion, valida el
        // origen de los formularios y comprueba el permiso de cada ruta.
        server.createContext("/", new FiltroSeguridad(router, engine));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        SesionStore.getInstance().purgar();

        System.out.println("Acceso con usuario y contrasena. Roles: ADMIN_GENERAL, DIGITADOR, ENFERMERA, DOCTOR.");
        System.out.println("Servidor iniciado. Abre tu navegador en: http://localhost:" + port + "/");
        System.out.println("Plantillas: " + templatesDir);
        System.out.println("Estáticos:  " + staticDir);
        System.out.println("Presiona Ctrl+C para detener.");
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            try { return Integer.parseInt(args[0].trim()); } catch (NumberFormatException ignored) {}
        }
        String envPort = System.getenv("HOSPITAL_PORT");
        if (envPort != null && !envPort.isBlank()) {
            try { return Integer.parseInt(envPort.trim()); } catch (NumberFormatException ignored) {}
        }
        String sysPort = System.getProperty("port");
        if (sysPort != null && !sysPort.isBlank()) {
            try { return Integer.parseInt(sysPort.trim()); } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }
}
