package com.hospital.db;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resuelve dónde viven las carpetas de recursos (templates, static, data)
 * relativas a la instalación de la app, sin importar desde qué directorio
 * de trabajo se haya lanzado "java -jar hospital.jar". Esto evita el típico
 * problema de "funciona si lo corro desde aquí pero no desde allá".
 */
public final class AppPaths {
    private AppPaths() {}

    /** Carpeta donde está el .jar en ejecución (o build/classes en modo desarrollo). */
    public static Path appHome() {
        try {
            File f = new File(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // Si corre desde un .jar, f es el archivo .jar -> su carpeta contenedora.
            // Si corre desde clases sueltas (modo desarrollo), f ya es esa carpeta.
            return f.isFile() ? f.getParentFile().toPath() : f.toPath();
        } catch (URISyntaxException | NullPointerException e) {
            return Path.of(".").toAbsolutePath().normalize();
        }
    }

    /**
     * Busca una carpeta de recursos por nombre: primero junto al .jar (caso normal
     * de distribución), y si no existe ahí, en el directorio de trabajo actual
     * (caso típico al ejecutar en modo desarrollo desde la raíz del proyecto).
     * Si no existe en ningún lado, devuelve la ruta junto al .jar de todas formas
     * (útil para carpetas como "data" que se crean solas la primera vez).
     */
    public static Path resolveDir(String name) {
        Path besideJar = appHome().resolve(name);
        if (Files.isDirectory(besideJar)) return besideJar;
        Path cwd = Path.of(name).toAbsolutePath().normalize();
        if (Files.isDirectory(cwd)) return cwd;
        return besideJar;
    }
}
