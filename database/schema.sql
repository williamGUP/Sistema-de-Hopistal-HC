-- ============================================================================
-- Sistema de Registro de Pacientes — esquema de base de datos (H2)
-- ----------------------------------------------------------------------------
-- Este script lo ejecuta la propia aplicación automáticamente al arrancar
-- (usa CREATE TABLE IF NOT EXISTS, así que es seguro tenerlo también aquí
-- como referencia/documentación). Si quieres crear la base de datos a mano
-- en otro motor (MySQL, PostgreSQL), este script es tu punto de partida;
-- ver docs/MANUAL.md, sección "Migrar a otro motor de base de datos".
-- ============================================================================

CREATE TABLE IF NOT EXISTS paciente (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  dni               VARCHAR(8)   NOT NULL,
  numero_historia   VARCHAR(20)  NOT NULL,
  nombres           VARCHAR(100) NOT NULL,
  apellidos         VARCHAR(100) NOT NULL,
  fecha_nacimiento  DATE,
  sexo              VARCHAR(15),
  telefono          VARCHAR(20),
  direccion         VARCHAR(200),
  email             VARCHAR(120),
  grupo_sanguineo   VARCHAR(5),
  alergias          VARCHAR(500),
  activo            BOOLEAN      NOT NULL DEFAULT TRUE,
  fecha_registro    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_paciente_dni UNIQUE (dni),
  CONSTRAINT uq_paciente_numero_historia UNIQUE (numero_historia)
);

CREATE TABLE IF NOT EXISTS consulta (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  paciente_id    BIGINT NOT NULL,
  fecha          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  medico         VARCHAR(100),
  motivo         VARCHAR(200),
  sintomas       VARCHAR(1000),
  diagnostico    VARCHAR(1000),
  tratamiento    VARCHAR(1000),
  observaciones  VARCHAR(1000),
  CONSTRAINT fk_consulta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id)
);

CREATE TABLE IF NOT EXISTS enfermedad (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  paciente_id        BIGINT NOT NULL,
  nombre             VARCHAR(150) NOT NULL,
  fecha_diagnostico  DATE,
  estado             VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
  observaciones      VARCHAR(1000),
  CONSTRAINT fk_enfermedad_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id)
);

CREATE TABLE IF NOT EXISTS operacion (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  paciente_id     BIGINT NOT NULL,
  fecha           DATE NOT NULL,
  tipo_operacion  VARCHAR(150) NOT NULL,
  cirujano        VARCHAR(100),
  resultado       VARCHAR(30),
  observaciones   VARCHAR(1000),
  CONSTRAINT fk_operacion_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id)
);

CREATE INDEX IF NOT EXISTS idx_paciente_apellidos ON paciente(apellidos);
CREATE INDEX IF NOT EXISTS idx_consulta_paciente ON consulta(paciente_id);
CREATE INDEX IF NOT EXISTS idx_enfermedad_paciente ON enfermedad(paciente_id);
CREATE INDEX IF NOT EXISTS idx_operacion_paciente ON operacion(paciente_id);
