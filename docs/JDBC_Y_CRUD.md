# Integración JDBC y Operaciones CRUD — Sistema de Hospital HC

**Proyecto:** Sistema de Registro de Pacientes — Hospital HC
**Práctica:** Campo 7 — Integración y validación del proyecto
**Criterio que cubre:** *3. JDBC y operaciones CRUD* (peso 4 sobre 20)

Este documento describe, con el código real del repositorio, cómo se conecta
Java con la base de datos mediante JDBC y cómo se implementan las cuatro
operaciones CRUD sobre las tablas del proyecto.

---

## 1. Parámetros de conexión JDBC

La conexión se establece en un único punto de la aplicación:
`src/com/hospital/db/Db.java`.

### 1.1 Parámetros

| Parámetro | Valor | Ubicación |
|---|---|---|
| **Driver** | `org.h2.Driver` | `Db.java` línea 27 |
| **Tipo de base de datos** | H2 2.2.224 (embebida, motor relacional) | `lib/h2-2.2.224.jar` |
| **URL de conexión** | `jdbc:h2:file:<carpeta>/data/hospital;AUTO_SERVER=TRUE` | `Db.java` línea 38 |
| **Usuario** | `sa` | `Db.java` línea 53 |
| **Contraseña** | *(vacía, por defecto del modo embebido)* | `Db.java` línea 53 |
| **Tipo de conexión** | `AUTO_SERVER=TRUE` — permite abrir la base desde varias herramientas | `Db.java` línea 38 |
| **Puerto HTTP de la aplicación** | 8080 por defecto (configurable) | `Main.resolvePort()` |

### 1.2 Ruta absoluta de la base de datos

La ruta no está fijada en el código: se resuelve en tiempo de ejecución para
que la aplicación funcione sin importar desde qué carpeta se ejecute.

```java
// src/com/hospital/db/Db.java
Path dataDir = AppPaths.resolveDir("data");        // ./data junto al .jar
Path dbFile  = dataDir.resolve("hospital").toAbsolutePath();
jdbcUrl = "jdbc:h2:file:" + dbFile + ";AUTO_SERVER=TRUE";
```

El archivo generado es `data/hospital.mv.db`.

### 1.3 Apertura de la conexión

```java
// src/com/hospital/db/Db.java, línea 51
public static Connection getConnection() throws SQLException {
    if (jdbcUrl == null) throw new IllegalStateException("Db.init() no fue llamado todavia");
    return DriverManager.getConnection(jdbcUrl, "sa", "");
}
```

### 1.4 Carga explícita del driver

```java
// src/com/hospital/db/Db.java, línea 27
Class.forName("org.h2.Driver");
```

Si el driver no está en el classpath, la aplicación lanza un mensaje claro:

> `No se encontro el driver de H2 en el classpath (lib/h2-2.2.224.jar).`

Esto cubre el criterio **"controlar errores de conexión y mostrar mensajes
comprensibles"** del enunciado.

### 1.5 El jar y el classpath

El manifiesto del proyecto (`manifest.mf`) declara la ruta del driver, de modo
que `java -jar hospital.jar` funciona sin configurar nada:

```
Manifest-Version: 1.0
Main-Class: com.hospital.Main
Class-Path: lib/h2-2.2.224.jar
```

---

## 2. Creación del esquema

El DDL está embebido en `Db.java` (array `SCHEMA_STATEMENTS`) y se ejecuta con
`CREATE TABLE IF NOT EXISTS`, por lo que es seguro en cada arranque.

```java
// src/com/hospital/db/Db.java, línea 40
try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
    for (String ddl : SCHEMA_STATEMENTS) {
        st.execute(ddl);
    }
    seedIfEmpty(conn);
}
```

### 2.1 Tablas del proyecto

| Tabla | Campos principales | Clave foránea |
|---|---|---|
| `paciente` | `id`, `dni`, `numero_historia`, `nombres`, `apellidos`, `fecha_nacimiento`, `sexo`, `telefono`, `direccion`, `email`, `grupo_sanguineo`, `alergias`, `activo`, `fecha_registro` | — |
| `consulta` | `id`, `paciente_id`, `fecha`, `medico`, `motivo`, `sintomas`, `diagnostico`, `tratamiento`, `observaciones` | `fk_consulta_paciente` |
| `enfermedad` | `id`, `paciente_id`, `nombre`, `fecha_diagnostico`, `estado`, `observaciones` | `fk_enfermedad_paciente` |
| `operacion` | `id`, `paciente_id`, `fecha`, `tipo_operacion`, `cirujano`, `resultado`, `observaciones` | `fk_operacion_paciente` |
| `usuario` | `id`, `username`, `password`, `nombre_completo`, `rol`, `activo`, `fecha_creacion` | — |
| `auditoria_acceso` | `id`, `fecha`, `usuario_id`, `usuario`, `rol`, `ip`, `metodo`, `ruta`, `accion`, `resultado`, `detalle` | — |

### 2.2 Restricciones de integridad

```sql
CONSTRAINT uq_paciente_dni UNIQUE (dni)
CONSTRAINT uq_paciente_numero_historia UNIQUE (numero_historia)
CONSTRAINT fk_consulta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id)
CONSTRAINT uq_usuario_username UNIQUE (username)
```

Las restricciones `UNIQUE` y `FOREIGN KEY` son las que permiten que la base
rechace por sí misma datos inconsistentes, aunque la aplicación falle.

### 2.3 Índices

```sql
CREATE INDEX IF NOT EXISTS idx_paciente_apellidos  ON paciente(apellidos)
CREATE INDEX IF NOT EXISTS idx_consulta_paciente  ON consulta(paciente_id)
CREATE INDEX IF NOT EXISTS idx_enfermedad_paciente ON enfermedad(paciente_id)
CREATE INDEX IF NOT EXISTS idx_operacion_paciente ON operacion(paciente_id)
```

---

## 3. Operaciones CRUD por tabla

### 3.1 Tabla `paciente`

| Operación | Método | Sentencia SQL | Resultado verificado |
|---|---|---|---|
| **Create** | `PacienteDao.crear()` | `INSERT INTO paciente (dni, numero_historia, …) VALUES (?, ?, …, ?)` | Prueba `CRUD-01` |
| **Read** | `PacienteDao.listarActivos()` | `SELECT * FROM paciente WHERE activo = TRUE ORDER BY apellidos, nombres` | Prueba `CRUD-02` |
| **Read** | `PacienteDao.buscar(q)` | `SELECT * FROM paciente WHERE activo = TRUE AND (… LIKE ? …)` | Prueba `CRUD-02` |
| **Read** | `PacienteDao.buscarPorId(id)` | `SELECT * FROM paciente WHERE id = ?` | Prueba `CRUD-03` |
| **Update** | `PacienteDao.actualizar()` | `UPDATE paciente SET … WHERE id = ?` | Pruebas `CRUD-04`, `CRUD-05` |
| **Delete** | `PacienteDao.desactivar(id)` | `UPDATE paciente SET activo = FALSE WHERE id = ?` | Pruebas `CRUD-10`, `CRUD-11` |

> **Decisión de diseño:** la eliminación de un paciente es **lógica**, no física.
> Una historia clínica nunca se borra: se marca `activo = FALSE` y deja de
> aparecer en los listados, pero su historial queda intacto. Esto satisface el
> requisito de no perder información clínica.

### 3.2 Tabla `consulta`

| Operación | Método | Sentencia SQL | Prueba |
|---|---|---|---|
| **Create** | `ConsultaDao.crear()` | `INSERT INTO consulta (paciente_id, fecha, medico, motivo, sintomas, diagnostico, tratamiento, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?)` | `CRUD-06` |
| **Read** | `ConsultaDao.listarPorPaciente()` | `SELECT * FROM consulta WHERE paciente_id = ? ORDER BY fecha DESC, id DESC` | `CRUD-09` |
| **Delete** | `ConsultaDao.eliminar()` | `DELETE FROM consulta WHERE id = ?` | Acceso por rol |

### 3.3 Tabla `enfermedad`

| Operación | Método | Sentencia SQL | Prueba |
|---|---|---|---|
| **Create** | `EnfermedadDao.crear()` | `INSERT INTO enfermedad (paciente_id, nombre, fecha_diagnostico, estado, observaciones) VALUES (?, ?, ?, ?, ?)` | `CRUD-07` |
| **Read** | `EnfermedadDao.listarPorPaciente()` | `SELECT * FROM enfermedad WHERE paciente_id = ? ORDER BY fecha_diagnostico DESC, id DESC` | `CRUD-09` |
| **Delete** | `EnfermedadDao.eliminar()` | `DELETE FROM enfermedad WHERE id = ?` | Acceso por rol |

### 3.4 Tabla `operacion`

| Operación | Método | Sentencia SQL | Prueba |
|---|---|---|---|
| **Create** | `OperacionDao.crear()` | `INSERT INTO operacion (paciente_id, fecha, tipo_operacion, cirujano, resultado, observaciones) VALUES (?, ?, ?, ?, ?, ?)` | `CRUD-08` |
| **Read** | `OperacionDao.listarPorPaciente()` | `SELECT * FROM operacion WHERE paciente_id = ? ORDER BY fecha DESC, id DESC` | `CRUD-09` |
| **Delete** | `OperacionDao.eliminar()` | `DELETE FROM operacion WHERE id = ?` | Acceso por rol |

### 3.5 Tablas del subsistema de seguridad

| Tabla | Create | Read | Update | Delete |
|---|---|---|---|---|
| `usuario` | `UsuariosDao.crear()` | `porUsername()`, `porId()`, `listar()` | `actualizarPerfil()`, `actualizarContrasena()`, `actualizarActivo()` | Baja lógica con `actualizarActivo(id, false)` |
| `auditoria_acceso` | `Auditoria.registrar()` | `Auditoria.listar()`, `contar()` | — (es bitácora de solo registro) | — |

---

## 4. Buenas prácticas de JDBC aplicadas

### 4.1 Consultas preparadas

Ninguna consulta concatena valores del usuario. Todas usan `PreparedStatement`:

```java
// PacienteDao.buscarPorDni
String sql = "SELECT * FROM paciente WHERE dni = ?";
try (Connection conn = Db.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, dni);
    ResultSet rs = ps.executeQuery();
}
```

Esto elimina la **inyección SQL** como categoría de riesgo, porque el motor
separa el código de la consulta de los datos que recibe.

### 4.2 Gestión de recursos con try-with-resources

```java
try (Connection conn = Db.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // ... uso
}
// conn, ps y rs se cierran automáticamente, aunque haya excepción
```

### 4.3 Une conexión por operación

Cada método DAO abre y cierra su propia conexión. No hay estado compartido
entre hilos, lo que hace seguro el uso del servidor con un pool de 8 hilos.

### 4.4 Errores propagados y gestionados

- Los DAOs lanzan `SQLException`.
- Los controladores capturan la excepción y muestran un mensaje comprensible.
- `Auditoria.registrar()` nunca propaga un error: una falla en la bitácora no
  debe impedir el trabajo del usuario.

---

## 5. Cómo reproducir la evidencia

Con el sistema detenido y la carpeta `data/` borrada:

```
java -jar hospital.jar 8080
powershell -ExecutionPolicy Bypass -File test\TestCrudJdbc.ps1
```

Resultado obtenido:

```
Casos ejecutados:  23
Casos correctos:   23
Casos fallidos:    0

CRUD Y VALIDACION: TODO CORRECTO
```

Los 23 casos cubren: 11 de CRUD con datos válidos (alta, lectura, edición,
alta en las tres tablas relacionadas y baja lógica) y 11 de validación con
datos no válidos, más un caso de control de errores de conexión. El detalle
caso por caso está en [`INFORME_DE_PRUEBAS.md`](INFORME_DE_PRUEBAS.md).

Para inspeccionar la base con una herramienta externa, gracias al parámetro
`AUTO_SERVER=TRUE` basta con apuntar cualquier cliente JDBC a:

```
jdbc:h2:file:<ruta>/data/hospital;AUTO_SERVER=TRUE
usuario: sa   contrasena: (vacía)
```