package com.hospital.db;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {
    private AppPaths() {}
    public static Path appHome() {
        try {
            File f = new File(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() ? f.getParentFile().toPath() : f.toPath();
        } catch (URISyntaxException | NullPointerException e) {
            return Path.of(".").toAbsolutePath().normalize();
        }
    }

    public static Path resolveDir(String name) {
        Path besideJar = appHome().resolve(name);
        if (Files.isDirectory(besideJar)) return besideJar;
        Path cwd = Path.of(name).toAbsolutePath().normalize();
        if (Files.isDirectory(cwd)) return cwd;
        return besideJar;
    }
}

