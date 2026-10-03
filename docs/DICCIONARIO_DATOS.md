# Diccionario de datos — Hospital HC

Base de datos: **H2** (embebida, en modo archivo). El esquema completo en
SQL está en [`database/schema.sql`](../database/schema.sql); este
documento explica el significado de cada tabla y campo.

Ver también el diagrama entidad-relación: [`diagramas/entidad-relacion.png`](diagramas/entidad-relacion.png)

## Resumen de tablas

| Tabla        | Descripción                                              |
|--------------|-----------------------------------------------------------|
| `paciente`   | Datos generales de cada paciente registrado.               |
| `consulta`   | Consultas médicas de un paciente (motivo, síntomas, diagnóstico, tratamiento). |
| `enfermedad` | Enfermedades y antecedentes médicos de un paciente.         |
| `operacion`  | Operaciones/intervenciones quirúrgicas de un paciente.      |

Las tablas `consulta`, `enfermedad` y `operacion` se relacionan con
`paciente` mediante `paciente_id` (relación uno a muchos: un paciente
puede tener muchas consultas, muchas enfermedades y muchas operaciones).

---

## Tabla `paciente`

| Campo              | Tipo          | Obligatorio | Descripción |
|--------------------|---------------|:-----------:|-------------|
| `id`               | BIGINT        | Sí (autogenerado) | Identificador interno, autoincremental. Clave primaria. |
| `dni`               | VARCHAR(8)    | Sí | Documento Nacional de Identidad (formato peruano: 8 dígitos numéricos). Único: no puede haber dos pacientes con el mismo DNI. |
| `numero_historia`   | VARCHAR(20)   | Sí (autogenerado) | Número de historia clínica, formato `HC-NNNNNN` (por ejemplo `HC-000001`). Se genera automáticamente al crear el paciente, a partir de su `id`. Único. |
| `nombres`           | VARCHAR(100)  | Sí | Nombres del paciente. |
| `apellidos`         | VARCHAR(100)  | Sí | Apellidos del paciente. |
| `fecha_nacimiento`  | DATE          | No | Fecha de nacimiento. Se usa para calcular la edad mostrada en pantalla. No puede ser una fecha futura. |
| `sexo`              | VARCHAR(15)   | No | Uno de: `MASCULINO`, `FEMENINO`, `OTRO`. |
| `telefono`          | VARCHAR(20)   | No | Teléfono de contacto. |
| `direccion`         | VARCHAR(200)  | No | Dirección de domicilio. |
| `email`             | VARCHAR(120)  | No | Correo electrónico de contacto. |
| `grupo_sanguineo`   | VARCHAR(5)    | No | Uno de: `O+`, `O-`, `A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, o vacío si no se conoce. |
| `alergias`          | VARCHAR(500)  | No | Alergias conocidas, en texto libre. |
| `activo`            | BOOLEAN       | Sí (por defecto `TRUE`) | `FALSE` cuando el paciente fue "dado de baja" desde el sistema. Es una **baja lógica**: el registro y todo su historial permanecen en la base de datos, solo se oculta del listado de activos. |
| `fecha_registro`    | TIMESTAMP     | Sí (automático) | Fecha y hora en que se registró al paciente en el sistema. |

**Índices**: `idx_paciente_apellidos` (búsquedas por apellido).

## Tabla `consulta`

Representa una visita/consulta médica de un paciente.

| Campo           | Tipo          | Obligatorio | Descripción |
|-----------------|---------------|:-----------:|-------------|
| `id`            | BIGINT        | Sí (autogenerado) | Identificador interno. Clave primaria. |
| `paciente_id`   | BIGINT        | Sí | Referencia al paciente (`paciente.id`). |
| `fecha`         | TIMESTAMP     | Sí | Fecha y hora de la consulta. Si no se indica al registrarla, se usa la fecha/hora actual. |
| `medico`        | VARCHAR(100)  | No | Nombre del médico que atendió. |
| `motivo`        | VARCHAR(200)  | Sí | Motivo de la consulta. |
| `sintomas`      | VARCHAR(1000) | Sí | Síntomas presentados por el paciente. |
| `diagnostico`   | VARCHAR(1000) | Sí | Diagnóstico del médico. |
| `tratamiento`   | VARCHAR(1000) | No | Tratamiento indicado. |
| `observaciones` | VARCHAR(1000) | No | Notas adicionales. |

**Índices**: `idx_consulta_paciente` (listar consultas de un paciente).

## Tabla `enfermedad`

Representa una enfermedad diagnosticada o un antecedente médico del
paciente (crónico o pasado).

| Campo               | Tipo          | Obligatorio | Descripción |
|---------------------|---------------|:-----------:|-------------|
| `id`                | BIGINT        | Sí (autogenerado) | Identificador interno. Clave primaria. |
| `paciente_id`       | BIGINT        | Sí | Referencia al paciente (`paciente.id`). |
| `nombre`            | VARCHAR(150)  | Sí | Nombre de la enfermedad o antecedente (por ejemplo "Diabetes mellitus tipo 2"). |
| `fecha_diagnostico` | DATE          | No | Fecha en que fue diagnosticada. |
| `estado`            | VARCHAR(20)   | Sí (por defecto `ACTIVA`) | Uno de: `ACTIVA`, `CONTROLADA`, `CURADA`. |
| `observaciones`     | VARCHAR(1000) | No | Notas adicionales. |

**Índices**: `idx_enfermedad_paciente` (listar enfermedades de un paciente).

## Tabla `operacion`

Representa una intervención quirúrgica realizada al paciente.

| Campo            | Tipo          | Obligatorio | Descripción |
|------------------|---------------|:-----------:|-------------|
| `id`             | BIGINT        | Sí (autogenerado) | Identificador interno. Clave primaria. |
| `paciente_id`    | BIGINT        | Sí | Referencia al paciente (`paciente.id`). |
| `fecha`          | DATE          | Sí | Fecha de la operación. |
| `tipo_operacion` | VARCHAR(150)  | Sí | Tipo de operación realizada (por ejemplo "Apendicectomía laparoscópica"). |
| `cirujano`       | VARCHAR(100)  | No | Nombre del cirujano responsable. |
| `resultado`      | VARCHAR(30)   | No | Uno de: `EXITOSA`, `CON_COMPLICACIONES`, `FALLIDA`. |
| `observaciones`  | VARCHAR(1000) | No | Notas adicionales. |

**Índices**: `idx_operacion_paciente` (listar operaciones de un paciente).

---

## Notas de diseño

- **Ningún registro médico se borra físicamente por accidente**: dar de
  baja a un paciente es una baja lógica (`activo = FALSE`), no un `DELETE`.
  Sí es posible eliminar una consulta, enfermedad u operación puntual mal
  registrada desde su propia historia clínica (esto sí es un `DELETE`
  físico de ese registro puntual, no del paciente).
- **El número de historia clínica se genera del lado del servidor** a
  partir del `id` autoincremental de la tabla `paciente` (`HC-` + el `id`
  con 6 dígitos y ceros a la izquierda), para garantizar que nunca se
  repita y que no dependa de que el usuario lo escriba bien.
- **Toda validación de datos ocurre también en el servidor** (no solo en
  el formulario HTML), así que aunque alguien intente enviar datos
  inválidos directamente (sin pasar por el formulario), el sistema los
  rechaza igual.

## Migrar a otro motor de base de datos

Este sistema usa H2 por ser embebido y no requerir instalación aparte,
pero el esquema en `database/schema.sql` usa SQL estándar (tipos
`BIGINT`, `VARCHAR`, `DATE`, `TIMESTAMP`, `BOOLEAN`, claves foráneas) que
funciona sin cambios o con cambios mínimos en MySQL, PostgreSQL o SQL
Server. Para migrar:

1. Ejecuta `database/schema.sql` (ajustando `AUTO_INCREMENT` al
   equivalente del motor destino, por ejemplo `SERIAL` en PostgreSQL o
   `AUTO_INCREMENT` en MySQL, que ya usa esa misma sintaxis).
2. Cambia la URL de conexión y el driver en `src/com/hospital/db/Db.java`
   (el resto del código usa JDBC estándar mediante `PreparedStatement`, y
   no depende de sintaxis específica de H2).
3. Reemplaza el jar `lib/h2-2.2.224.jar` por el driver JDBC del motor
   elegido, y ajusta el `Class-Path` del manifiesto al empaquetar.
