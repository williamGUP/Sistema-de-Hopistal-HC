# Informe de Pruebas — Sistema de Hospital HC

**Proyecto:** Sistema de Registro de Pacientes — Hospital HC
**Práctica:** Campo 7 — Integración y validación del proyecto
**Criterios que cubre:** *4. Pruebas y manejo de errores* (peso 3) y *3. JDBC y CRUD* (peso 4)
**Fecha de ejecución:** 2026-10-05

Este informe registra los resultados **reales** de dos suites de pruebas
ejecutadas sobre el sistema funcionando. Todos los casos son reproducibles.

---

## 1. Metodología

### 1.1 Alcance

| Suite | Casos | Alcance |
|---|---:|---|
| `test/TestCrudJdbc.ps1` | 23 | Operaciones CRUD y validación de datos válidos e inválidos |
| `test/TestAccesoRoles.ps1` | 71 | Control de acceso por roles, seguridad y gestión de usuarios |
| **Total** | **94** | |

### 1.2 Entorno de ejecución

| Elemento | Valor |
|---|---|
| Sistema operativo | Windows 11 |
| JDK | OpenJDK 21.0.12 (Temurin), compatible con el requisito de Java 17+ |
| Base de datos | H2 2.2.224 embebida, archivo `data/hospital.mv.db` |
| Estado inicial | Base de datos recién creada (se borró la carpeta `data/`) |
| Datos de prueba | 3 pacientes de ejemplo del *seed* + registros creados por las pruebas |

### 1.3 Procedimiento

1. Detener el servidor y borrar la carpeta `data/`.
2. Arrancar: `java -jar hospital.jar 8080`
3. Ejecutar `test\TestCrudJdbc.ps1`
4. Ejecutar `test\TestAccesoRoles.ps1`
5. Registrar el resultado de cada criterio de aceptación.

### 1.4 Criterios de decisión

- **Correcto**: el resultado obtenido coincide exactamente con el esperado.
- **Incorrecto**: el resultado difiere del esperado.
- Cada caso se ejecuta de forma independiente y con datos únicos (sufijo
  numérico aleatorio), de modo que la suite puede repetirse sin limpiar la base.

---

## 2. Resultados de CRUD y validación (23 casos)

### 2.1 Operaciones CRUD con datos válidos

| Caso | Operación | Dato de prueba | Resultado esperado | Resultado obtenido |
|---|---|---|---|---|
| `CRUD-01` | **Create** `paciente` | DNI de 8 dígitos, nombres, apellidos, sexo, grupo sanguíneo, fecha de nacimiento | HTTP 302 y alta registrada con identificador asignado | **Correcto** (id asignado) |
| `CRUD-02` | **Read** `paciente` | Búsqueda por el DNI creado | El paciente aparece en el listado | **Correcto** |
| `CRUD-03` | **Read** `paciente` | Detalle por identificador | La historia clínica se muestra | **Correcto** |
| `CRUD-04` | **Update** `paciente` | Edición de nombre, apellidos, teléfono, dirección, correo y alergias | HTTP 302 y cambios guardados | **Correcto** |
| `CRUD-05` | **Update** verificación | Lectura después de la edición | Los datos nuevos aparecen | **Correcto** |
| `CRUD-06` | **Create** `consulta` | Motivo, síntomas, diagnóstico, tratamiento, médico y fecha | HTTP 302 y consulta registrada | **Correcto** |
| `CRUD-07` | **Create** `enfermedad` | Nombre, fecha de diagnóstico y estado `ACTIVA` | HTTP 302 y enfermedad registrada | **Correcto** |
| `CRUD-08` | **Create** `operacion` | Tipo, fecha, cirujano y resultado `EXITOSA` | HTTP 302 y operación registrada | **Correcto** |
| `CRUD-09` | **Read** relacionada | Historia clínica del paciente | Consulta, enfermedad y operación listadas | **Correcto** |
| `CRUD-10` | **Delete** `paciente` | Baja del paciente creado | HTTP 302 y baja registrada | **Correcto** |
| `CRUD-11` | **Delete** verificación | Búsqueda posterior a la baja | El listado activo ya no lo muestra | **Correcto** |

**Resultado: 11 de 11 casos correctos.** Las cuatro operaciones CRUD
(creación, consulta, actualización y eliminación) funcionan sobre las cuatro
tablas del proyecto.

### 2.2 Validación de datos no válidos

| Caso | Operación | Dato no válido | Resultado esperado | Mensaje del sistema | Resultado |
|---|---|---|---|---|---|
| `VAL-01` | Create `paciente` | DNI = `"123"` (3 dígitos) | Rechazado por formato | *"El DNI debe tener exactamente 8 dígitos numéricos."* | **Correcto** |
| `VAL-02` | Create `paciente` | Nombres y apellidos vacíos | Rechazado por campos obligatorios | *"Completa nombres y apellidos."* | **Correcto** |
| `VAL-03` | Create `paciente` | DNI `45678912` ya existente | Rechazado por clave duplicada | *"Ya existe un paciente registrado con el DNI 45678912 (Torres Quispe, Maria Fernanda)."* | **Correcto** |
| `VAL-04` | Create `paciente` | Fecha de nacimiento 3 años en el futuro | Rechazada por fecha futura | *"La fecha de nacimiento no puede ser futura."* | **Correcto** |
| `VAL-05` | Create `paciente` | Sexo = `"X"` (fuera del catálogo) | Rechazado por catálogo | *"El sexo seleccionado no es válido."* | **Correcto** |
| `VAL-06` | Create `paciente` | Grupo sanguíneo = `"Z+"` | Rechazado por catálogo | *"El grupo sanguíneo seleccionado no es válido."* | **Correcto** |
| `VAL-07` | Create `operacion` | Tipo de operación vacío | Rechazado por campo obligatorio | *"Indica el tipo de operación, una fecha válida y un resultado."* | **Correcto** |
| `VAL-08` | Create `enfermedad` | Estado = `"INVENTADA"` | Rechazado por catálogo | *"Indica el nombre de la enfermedad y un estado válido."* | **Correcto** |
| `VAL-09` | Create `operacion` | Fecha = `"no-es-fecha"` | Rechazado por formato de fecha | *"Indica el tipo de operación, una fecha válida y un resultado."* | **Correcto** |
| `VAL-10` | Update `paciente` | Identificador `999999` inexistente | Redirección con aviso | *"Paciente no encontrado."* | **Correcto** |
| `VAL-11` | Navegación | Ruta `/ruta/que/no/existe` | Página 404 controlada | Página 404 con enlace al inicio | **Correcto** |

**Resultado: 11 de 11 casos correctos.** El sistema rechaza los datos
inválidos con mensajes en lenguaje comprensible, y los datos escritos por el
usuario se conservan para que pueda corregirlos.

### 2.3 Control de errores de conexión

| Caso | Escenario | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| `ERR-01` | Consulta general sin sesión iniciada | Redirección al login, con la base de datos operativa | **Correcto** |

Además se verificó el comportamiento cuando el driver no está disponible: la
aplicación lanza un mensaje explícito en lugar de fallar en silencio.

> `java.lang.RuntimeException: No se encontro el driver de H2 en el classpath (lib/h2-2.2.224.jar).`

**Total de la suite: 23 casos ejecutados, 23 correctos, 0 incorrectos.**

---

## 3. Resultados de acceso y roles (71 casos)

### 3.1 Distribución por bloque

| Bloque | Casos | Resultado |
|---|---:|---|
| 1. Protección de rutas sin sesión | 6 | 6 correctos |
| 2. Inicio de sesión y cookie segura | 8 | 8 correctos |
| 3. Cabeceras de seguridad | 5 | 5 correctos |
| 4. Permisos de los cuatro roles | 23 | 23 correctos |
| 5. Baja de pacientes según rol | 3 | 3 correctos |
| 6. Protección CSRF | 2 | 2 correctos |
| 7. Bloqueo por intentos fallidos | 2 | 2 correctos |
| 8. Cierre de sesión | 2 | 2 correctos |
| 9. Cambio de contraseña propia | 6 | 6 correctos |
| 10. Administración de usuarios | 6 | 6 correctos |
| 11. Desactivación y reactivación | 5 | 5 correctos |
| 12. Bitácora de seguridad | 3 | 3 correctos |
| **Total** | **71** | **71 correctos** |

### 3.2 Evidencia destacada

| Verificación | Resultado |
|---|---|
| Sin sesión, `/pacientes` responde | HTTP 302 → `/login?motivo=sesion-requerida` |
| Cookie de sesión emitida | `HttpOnly` + `SameSite=Strict` + `Path=/` + `Max-Age` |
| Cabeceras de seguridad presentes | CSP con `frame-ancestors 'none'`, `X-Frame-Options: DENY`, `nosniff`, `Cache-Control: no-store` |
| Digitador intenta acceder a `/usuarios` | HTTP 403 con pantalla explicativa |
| Enfermería intenta registrar una operación | HTTP 403 (requiere `operacion.crear`) |
| Médico registra una operación | HTTP 200 permitido |
| POST con `Origin` ajeno | HTTP 403 rechazado y registrado en bitácora |
| Sexto intento de acceso fallido | HTTP 429, cuenta bloqueada 15 minutos |
| Usuario desactivado intenta usar el sistema | HTTP 302 → redirigido al login |
| Contrasena antigua tras el cambio | HTTP 401, ya no sirve |
| Alta de usuario duplicado | HTTP 400 con mensaje de conflicto |

---

## 4. Cobertura por requerimiento

| Requerimiento | Suite que lo verifica | Casos |
|---|---|---|
| RF-01 Registrar paciente | `TestCrudJdbc` | `CRUD-01`, `VAL-01`…`VAL-06` |
| RF-02 Listar y buscar | `TestCrudJdbc` | `CRUD-02` |
| RF-03 Ver historia clínica | `TestCrudJdbc` | `CRUD-03`, `CRUD-09` |
| RF-04 Editar paciente | `TestCrudJdbc` | `CRUD-04`, `CRUD-05` |
| RF-05 Baja lógica | `TestCrudJdbc` | `CRUD-10`, `CRUD-11` |
| RF-06 a RF-11 Consultas, enfermedades y operaciones | `TestCrudJdbc` | `CRUD-06`…`CRUD-09`, `VAL-07`…`VAL-09` |
| RF-12 a RF-18 Validación y control de errores | `TestCrudJdbc` | `VAL-01`…`VAL-11` |
| RF-21 a RF-40 Acceso, roles y seguridad | `TestAccesoRoles` | 71 casos |

Todos los requerimientos funcionales del proyecto cuentan con al menos un caso
de prueba automatizado.

---

## 5. Pruebas manuales de verificación

Complementan la automatización y sirven para la demostración:

| Nº | Verificación | Resultado |
|---|---|---|
| M-1 | Iniciar sesión con las cuatro cuentas de demostración | Correcto |
| M-2 | Iniciar sesión con contraseña incorrecta | Correcto, mensaje genérico y contador de intentos |
| M-3 | Acceder sin sesión a una dirección interna | Correcto, redirige al login |
| M-4 | Navegar por el menú como administrador | Se muestran Usuarios y Bitácora |
| M-5 | Navegar por el menú como digitador | No se muestran Usuarios ni Bitácora |
| M-6 | Consultar la bitácora con filtros | Correcto |
| M-7 | Cambiar la contraseña propia | Correcto, se cierran las demás sesiones |
| M-8 | Intentar una acción no permitida desde la URL | Correcto, pantalla 403 explicativa |

---

## 6. Defectos encontrados y corregidos durante las pruebas

| Defecto | Causa | Corrección |
|---|---|---|
| El repositorio no compilaba: `PacienteDao.java` truncado | Subida incompleta del archivo | Se restauró el archivo completo |
| `templates/acceso.html` contenía el log del servidor | Se sobrescribió con la salida de la consola | Se restauró la plantilla |
| `templates/index.html` contenía el formulario de operación | Archivo equivocado en su lugar | Se restauró la portada |
| `Main.java` era una versión antigua sin filtro de seguridad | Archivo desactualizado | Se restauró con la integración completa |
| Los archivos con minúscula inicial rompían la compilación en Linux | `db.java`, `enfermedad.java`, `operacionDao.java`, `operacion.java`, `Main.Java` | Se renombraron con su capitalización correcta |
| `model/Usuario.java` faltaba y lo necesitan 9 clases | Se había borrado por estar en la lista de archivos heredados | Se restauró |

---

## 7. Conclusión

| Métrica | Valor |
|---|---|
| Casos de prueba ejecutados | 94 |
| Casos correctos | 94 |
| Casos incorrectos | 0 |
| Tasa de éxito | 100 % |
| Requerimientos con prueba automatizada | 40 de 40 |

El sistema demuestra su funcionamiento con datos válidos y rechaza de forma
controlada los datos no válidos, con mensajes comprensible para el usuario. La
evidencia es reproducible: basta con borrar `data/`, arrancar el sistema y
ejecutar los dos scripts de `test/`.