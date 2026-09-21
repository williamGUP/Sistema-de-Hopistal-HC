package com.hospital;

import com.hospital.db.AppPaths;
import com.hospital.db.Db;
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
 * Punto de entrada principal de la aplicación médica.
 * Se encarga de inicializar la base de datos, configurar el motor de plantillas
 * y desplegar el servidor HTTP nativo.
 */
public class Main {

    private static final int PUERTO_POR_DEFECTO = 8080;
    private static final int HILOS_POOL = 8;

    public static void main(String[] args) throws Exception {
        System.out.println(">>> Iniciando Sistema Hospitalario <<<");

        // Inicialización de la persistencia
        Db.init();

        // Configuración de rutas y plantillas
        Path rutaPlantillas = AppPaths.resolveDir("templates");
        Path rutaEstaticos = AppPaths.resolveDir("static");
        TemplateEngine motorPlantillas = new TemplateEngine(rutaPlantillas, true);

        Router enrutador = new Router(rutaEstaticos);
        
        // Registro de controladores web
        new PacienteController(motorPlantillas).registrarRutas(enrutador);
        new ConsultaController(motorPlantillas).registrarRutas(enrutador);
        new EnfermedadController(motorPlantillas).registrarRutas(enrutador);
        new OperacionController(motorPlantillas).registrarRutas(enrutador);

        // Configuración y puesta en marcha del servidor HTTP
        int puerto = obtenerPuertoServidor(args);
        HttpServer servidorHttp = HttpServer.create(new InetSocketAddress(puerto), 0);
        
        servidorHttp.createContext("/", enrutador);
        servidorHttp.setExecutor(Executors.newFixedThreadPool(HILOS_POOL));
        servidorHttp.start();

        System.out.println("\nServidor activo en: http://localhost:" + puerto + "/");
        System.out.println("Directorio de plantillas: " + rutaPlantillas);
        System.out.println("Directorio estático:   " + rutaEstaticos);
        System.out.println("Presione Ctrl+C para finalizar la ejecución.");
    }

    /**
     * Determina el puerto de escucha evaluando argumentos de línea de comandos,
     * variables de entorno y propiedades de sistema.
     */
    private static int obtenerPuertoServidor(String[] args) {
        // 1. Argumentos de entrada
        if (args.length > 0) {
            Integer puertoArg = parsearEntero(args[0]);
            if (puertoArg != null) {
                return puertoArg;
            }
        }

        // 2. Variable de entorno
        String puertoEnv = System.getenv("HOSPITAL_PORT");
        if (puertoEnv != null && !puertoEnv.isBlank()) {
            Integer puertoParsed = parsearEntero(puertoEnv);
            if (puertoParsed != null) {
                return puertoParsed;
            }
        }

        // 3. Propiedad del sistema (-Dport)
        String puertoSys = System.getProperty("port");
        if (puertoSys != null && !puertoSys.isBlank()) {
            Integer puertoParsed = parsearEntero(puertoSys);
            if (puertoParsed != null) {
                return puertoParsed;
            }
        }

        return PUERTO_POR_DEFECTO;
    }

    private static Integer parsearEntero(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
