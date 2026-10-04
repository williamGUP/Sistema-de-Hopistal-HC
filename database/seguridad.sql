-- ============================================================================
-- Hospital HC — esquema de seguridad (H2)
-- ----------------------------------------------------------------------------
-- Este archivo es la REFERENCIA DOCUMENTAL del subsistema de acceso y roles.
-- La aplicacion ejecuta exactamente estas sentencias al arrancar (estan
-- embebidas en src/com/hospital/seguridad/SeguridadEsquema.java, igual que
-- schema.sql esta embebido en src/com/hospital/db/Db.java), por lo que no
-- hace falta ejecutar este archivo a mano.
--
-- IMPORTANTE: no se modifica ninguna tabla existente del sistema. La tabla
-- "usuario" ya venia documentada en database/schema.sql (pero Db.java nunca la
-- creaba); aqui se crea con exactamente las mismas columnas para que ambos
-- archivos sigan siendo validos. La unica tabla nueva es "auditoria_acceso".
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Cuentas de usuario
-- ----------------------------------------------------------------------------
-- "password" almacena el hash PBKDF2-HMAC-SHA256 con el formato:
--     pbkdf2-sha256$<iteraciones>$<sal base64>$<hash base64>
-- Nunca se guarda la contrasena en texto plano.
CREATE TABLE IF NOT EXISTS usuario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  nombre_completo VARCHAR(150) NOT NULL,
  rol VARCHAR(30) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_usuario_username UNIQUE (username)
);

-- ----------------------------------------------------------------------------
-- 2. Bitacora de seguridad
-- ----------------------------------------------------------------------------
-- Un registro por evento relevante:
--   ACCESO           peticion sin sesion
--   LOGIN / LOGOUT   intento de acceso y cierre de sesion
--   CSRF             formulario con origen no confiable
--   <permiso.clave>  operacion permitida o denegada (p.ej. "paciente.crear")
--   BLOQUEO          bloqueo por intentos fallidos
--   CAMBIO_CONTRASENA  cambio de clave del propio usuario
--
-- resultado: CORRECTO | FALLIDO | DENEGADO | CIERRE
CREATE TABLE IF NOT EXISTS auditoria_acceso (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  usuario_id BIGINT,
  usuario VARCHAR(50),
  rol VARCHAR(30),
  ip VARCHAR(60),
  metodo VARCHAR(6),
  ruta VARCHAR(200),
  accion VARCHAR(40),
  resultado VARCHAR(20),
  detalle VARCHAR(400)
);

CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria_acceso(fecha);
CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON auditoria_acceso(usuario, fecha);

-- ----------------------------------------------------------------------------
-- 3. Los cuatro roles (columna usuario.rol)
-- ----------------------------------------------------------------------------
--   ADMIN_GENERAL  control total, incluidos usuarios y bitacora
--   DIGITADOR      captura de datos de pacientes, solo lectura clinica
--   ENFERMERA      consultas y enfermedades, sin operaciones
--   DOCTOR         historia clinica completa, incluidas operaciones
--
-- La matriz completa de permisos por rol esta en docs/MATRIZ_PERMISOS.md
-- y se implementa en src/com/hospital/seguridad/{Rol,Permiso,PoliticaPermisos}.java

-- ----------------------------------------------------------------------------
-- 4. Cuentas iniciales
-- ----------------------------------------------------------------------------
-- Las crea la propia aplicacion en el primer arranque (SecurityEsquema), con la
-- contrasena ya hasheada. Son cuentas de DEMOSTRACION: cambialas antes de usar
-- el sistema con historias clinicas reales. La aplicacion muestra un aviso en
-- pantalla mientras alguna cuenta conserve su clave de fabrica.
--
--   usuario     contrasena inicial   nombre            rol
--   admin       Admin2026            Elena Ramirez     ADMIN_GENERAL
--   doctor      Doctor2026           Carlos Mendoza    DOCTOR
--   enfermera   Enfermera2026        Ana Torres        ENFERMERA
--   digitador   Digitador2026        Luis Huaman       DIGITADOR
--
-- Tambien se escribe el archivo data/CREDENCIALES-DEMOSTRACION.txt con estas
-- cuentas para consulta rapida.
--
-- Si una base de datos antigua trae usuarios con la contrasena en texto plano,
-- el sistema la migra a PBKDF2 de forma transparente en el siguiente arranque,
-- sin cambiar el valor de la contrasena.