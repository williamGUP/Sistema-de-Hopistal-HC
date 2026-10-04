package com.hospital.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash y verificacion de contrasenas usando PBKDF2-HMAC-SHA256 (algoritmo
 * incluido en la propia JDK, por lo que el proyecto sigue sin dependencias
 * externas).
 *
 * Formato almacenado en la columna "password" de la tabla usuario:
 *
 *   pbkdf2-sha256$<iteraciones>$<sal base64>$<hash base64>
 *
 * Cada usuario tiene su propia sal aleatoria de 16 bytes, de modo que dos
 * contrasenas iguales nunca producen el mismo hash. La verificacion se hace con
 * comparacion en tiempo constante para no filtrar informacion por temporizacion.
 *
 * Por retrocompatibilidad, {@link #esHashValido(String)} permite detectar
 * contrasenas guardadas en texto plano (base de datos antiguas) y migrarlas
 * de forma transparente al primer inicio de sesion exitoso.
 */
public final class Passwords {

    private static final String PREFIJO = "pbkdf2-sha256";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int ITERACIONES_MINIMAS = 60_000;
    private static final int LARGO_SAL = 16;
    private static final int LARGO_HASH = 32;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Passwords() {}

    /** Genera un hash nuevo para la contrasena en claro. */
    public static String hash(String passwordClaro) {
        byte[] sal = new byte[LARGO_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(passwordClaro, sal, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Compara la contrasena en claro contra el valor almacenado. Acepta tanto
     * hashes PBKDF2 como valores antiguos en texto plano (para poder migrarlos).
     */
    public static boolean verificar(String passwordClaro, String almacenado) {
        if (passwordClaro == null || almacenado == null || almacenado.isEmpty()) return false;

        if (!esHashValido(almacenado)) {
            // Base de datos antigua: la contrasena estaba guardada en texto plano.
            return comparacionConstante(passwordClaro, almacenado);
        }

        String[] partes = almacenado.split("\\$");
        if (partes.length != 4) return false;
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < ITERACIONES_MINIMAS) iteraciones = ITERACIONES_MINIMAS;
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] obtenido = derivar(passwordClaro, sal, iteraciones);
            return MessageDigest.isEqual(esperado, obtenido);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Indica si el valor almacenado es un hash PBKDF2 (y por tanto no texto plano). */
    public static boolean esHashValido(String almacenado) {
        return almacenado != null && almacenado.startsWith(PREFIJO + "$") && almacenado.split("\\$").length == 4;
    }

    /** Compara dos cadenas en tiempo constante, para no filtrar informacion por temporizacion. */
    public static boolean comparacionConstante(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reglas minimas de contrasena para el alta y el cambio de clave.
     * Devuelve un mensaje con el problema, o null si la contrasena es valida.
     */
    public static String validar(String password, String username, String nombreCompleto) {
        if (password == null || password.isEmpty()) {
            return "Escribe una contrasena.";
        }
        if (password.length() < 8) {
            return "La contrasena debe tener al menos 8 caracteres.";
        }
        if (password.length() > 72) {
            return "La contrasena no puede superar los 72 caracteres.";
        }
        String minus = password.toLowerCase();
        boolean tieneLetra = minus.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = minus.chars().anyMatch(Character::isDigit);
        if (!tieneLetra || !tieneNumero) {
            return "La contrasena debe combinar letras y numeros.";
        }
        if (minus.contains(" ")) {
            return "La contrasena no puede contener espacios.";
        }
        if (username != null && !username.isBlank() && minus.contains(username.toLowerCase())) {
            return "La contrasena no puede contener tu nombre de usuario.";
        }
        if (nombreCompleto != null) {
            for (String parte : nombreCompleto.toLowerCase().split("\\s+")) {
                if (parte.length() >= 4 && minus.contains(parte)) {
                    return "La contrasena no puede contener tu nombre completo.";
                }
            }
        }
        return null;
    }

    private static byte[] derivar(String password, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, iteraciones, LARGO_HASH * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo derivar la contrasena (PBKDF2 no disponible).", e);
        }
    }
}