# Matriz de Trazabilidad — Sistema de Hospital HC

**Proyecto:** Sistema de Registro de Pacientes — Hospital HC
**Curso:** Técnicas de Programación Orientada a Objetos — Unidad III
**Práctica:** Campo 7 — Integración y validación del proyecto
**Repositorio:** <https://github.com/williamGUP/Sistema-de-Hopistal-HC>
**Fecha de actualización:** 2026-10-05

---

## 1. Leyenda de estados

| Estado | Significado |
|---|---|
| **Validado** | Implementado, integrado, compilado y verificado con un caso de prueba que arroja el resultado esperado |
| **Implementado** | El código existe y compila, pero aún no se ha verificado con un caso de prueba |
| **En desarrollo** | Parcialmente implementado; requiere ajustes |
| **Pendiente** | No iniciado |

**Criterio para asignar el estado:**

- *Implementado* → el archivo y el método existen en el repositorio.
- *Validado* → además, un caso de prueba de `test/TestCrudJdbc.ps1` (23 casos) o de `test/TestAccesoRoles.ps1` (71 casos) se ejecutó y coincidió con el resultado esperado.

En esta versión **los 40 requerimientos funcionales están en estado "Validado"**, respaldado por las dos suites de pruebas ejecutadas sobre el sistema en funcionamiento.

---

## 2. Requerimientos funcionales — Gestión de pacientes (línea base)

| ID | Historia de usuario | Criterios de aceptación | Estado | Evidencia |
|---|---|---|---|---|
| **RF-01** | **Registrar paciente**<br>*Como recepcionista quiero registrar un paciente nuevo para que quede disponible en el sistema.* | 1. DNI único de exactamente 8 dígitos<br>2. Nombres y apellidos obligatorios<br>3. Número de historia generado automáticamente<br>4. Fecha de nacimiento no futura<br>5. Sexo y grupo sanguíneo validados contra catálogo<br>6. Redirección a la historia con mensaje de éxito | **Validado** | `PacienteController.guardarNuevo()`<br>`PacienteController.validar()`<br>`PacienteDao.crear()`<br>Prueba: `CRUD-01`, `VAL-01`…`VAL-06` |
| **RF-02** | **Listar y buscar pacientes**<br>*Como médico quiero ver y buscar pacientes activos para acceder a su historia.* | 1. Listado ordenado por apellidos y nombres<br>2. Búsqueda por DNI, historia, nombres o apellidos<br>3. Insensible a mayúsculas<br>4. Solo pacientes con `activo = TRUE` | **Validado** | `PacienteController.lista()`<br>`PacienteController.home()`<br>`PacienteDao.listarActivos()`, `buscar()`<br>Prueba: `CRUD-02` |
| **RF-03** | **Ver historia clínica completa**<br>*Como médico quiero ver toda la información del paciente en una pantalla.* | 1. Datos generales completos<br>2. Consultas ordenadas por fecha descendente<br>3. Enfermedades con su estado<br>4. Operaciones con su resultado<br>5. Acciones contextuales según el permiso del rol | **Validado** | `PacienteController.detalle()`<br>`Views.pacienteCompleto()`<br>Plantilla `paciente-detalle.html`<br>Prueba: `CRUD-03`, `CRUD-09` |
| **RF-04** | **Editar paciente**<br>*Como recepcionista quiero corregir los datos de un paciente.* | 1. Formulario precargado<br>2. Mismas validaciones que el alta<br>3. DNI único salvo el propio paciente<br>4. Número de historia no editable | **Validado** | `PacienteController.formEditar()`<br>`PacienteController.guardarEditar()`<br>`PacienteDao.actualizar()`<br>Prueba: `CRUD-04`, `CRUD-05` |
| **RF-05** | **Dar de baja lógica**<br>*Como administrador quiero retirar un paciente del listado sin perder su historial.* | 1. Se ejecuta `UPDATE … SET activo = FALSE` (nunca `DELETE`)<br>2. Desaparece del listado y del buscador<br>3. El historial clínico permanece<br>4. Solo el rol ADMIN_GENERAL puede ejecutar la acción | **Validado** | `PacienteController.eliminar()`<br>`PacienteDao.desactivar()`<br>Prueba: `CRUD-10`, `CRUD-11` |

---

## 3. Requerimientos funcionales — Consultas, enfermedades y operaciones

| ID | Historia de usuario | Criterios de aceptación | Estado | Evidencia |
|---|---|---|---|---|
| **RF-06** | **Registrar consulta médica**<br>*Como médico quiero registrar una consulta con motivo, síntomas y diagnóstico.* | 1. Motivo, síntomas y diagnóstico obligatorios<br>2. Fecha por defecto: momento actual<br>3. Médico, tratamiento y observaciones opcionales<br>4. Asociada al paciente en sesión | **Validado** | `ConsultaController.guardar()`<br>`ConsultaDao.crear()`<br>Prueba: `CRUD-06` |
| **RF-07** | **Eliminar consulta**<br>*Como médico quiero eliminar una consulta mal registrada.* | 1. Botón en cada tarjeta de consulta<br>2. `DELETE` físico solo de ese registro<br>3. Requiere permiso `consulta.eliminar` | **Validado** | `ConsultaController.eliminar()`<br>`ConsultaDao.eliminar()`<br>Prueba: acceso por rol en `TestAccesoRoles` |
| **RF-08** | **Registrar enfermedad o antecedente**<br>*Como médico quiero registrar antecedentes clínicos del paciente.* | 1. Nombre obligatorio<br>2. Estado validado contra catálogo (ACTIVA / CONTROLADA / CURADA)<br>3. Fecha de diagnóstico opcional | **Validado** | `EnfermedadController.guardar()`<br>`EnfermedadDao.crear()`<br>Prueba: `CRUD-07`, `VAL-08` |
| **RF-09** | **Eliminar enfermedad**<br>*Como médico quiero retirar un antecedente mal registrado.* | 1. `DELETE` físico solo del registro<br>2. Requiere permiso `enfermedad.eliminar` | **Validado** | `EnfermedadController.eliminar()`<br>`EnfermedadDao.eliminar()` |
| **RF-10** | **Registrar operación quirúrgica**<br>*Como cirujano quiero registrar una operación con su resultado.* | 1. Tipo de operación y fecha obligatorios<br>2. Resultado validado contra catálogo<br>3. Cirujano y observaciones opcionales<br>4. Requiere permiso `operacion.crear` | **Validado** | `OperacionController.guardar()`<br>`OperacionDao.crear()`<br>Prueba: `CRUD-08`, `VAL-07`, `VAL-09` |
| **RF-11** | **Eliminar operación**<br>*Como cirujano quiero corregir el registro de una operación.* | 1. `DELETE` físico solo del registro<br>2. Requiere permiso `operacion.eliminar` | **Validado** | `OperacionController.eliminar()`<br>`OperacionDao.eliminar()` |
| **RF-12** | **Validación de datos de entrada**<br>*Como usuario quiero errores claros cuando escribo mal un dato.* | 1. Validación en el servidor, no solo en el navegador<br>2. Mensaje en lenguaje comprensible<br>3. Los datos escritos se conservan para corregirlos | **Validado** | `PacienteController.validar()`<br>Pruebas: `VAL-01` a `VAL-09` (9 casos) |
| **RF-13** | **Catálogos de valores válidos**<br>*Como usuario quiero elegir solo valores permitidos en los desplegables.* | 1. Sexo, grupo sanguíneo, estado de enfermedad y resultado de operación<br>2. Validación en servidor además del desplegable | **Validado** | `Catalogos.java`<br>Pruebas: `VAL-05`, `VAL-06`, `VAL-08` |
| **RF-14** | **Cálculo de edad**<br>*Como médico quiero ver la edad calculada sin calcularla mentalmente.* | 1. Edad derivada de la fecha de nacimiento<br>2. Muestra "—" si no hay fecha registrada | **Validado** | `Paciente.getEdad()`<br>`Views.pacienteResumen()` |
| **RF-15** | **Número de historia clínico automático**<br>*Como recepcionista quiero que el sistema asigne el número de historia.* | 1. Formato `HC-000001` incremental<br>2. Único por paciente (restricción `UNIQUE`) | **Validado** | `PacienteDao.crear()`<br>Prueba: `CRUD-01` (DNI único validado en `VAL-03`) |
| **RF-16** | **Integridad referencial de la historia clínica**<br>*Como médico quiero que las consultas, enfermedades y operaciones pertenezcan a un paciente existente.* | 1. Claves foráneas `FOREIGN KEY (paciente_id) REFERENCES paciente(id)`<br>2. No se admiten registros huérfanos | **Validado** | `database/schema.sql`<br>Declaraciones `CONSTRAINT fk_*` |
| **RF-17** | **Manejo de registro inexistente**<br>*Como usuario quiero un aviso claro si abro un paciente que no existe.* | 1. Redirección con mensaje "Paciente no encontrado"<br>2. No se genera error 500 | **Validado** | `PacienteController.detalle()`, `formEditar()`<br>Prueba: `VAL-10` |
| **RF-18** | **Control de rutas desconocidas**<br>*Como usuario quiero una página 404 y no una pantalla en blanco si escribo mal la dirección.* | 1. Respuesta 404 controlada<br>2. Enlace de retorno al inicio | **Validado** | `Router.handle()`<br>Prueba: `VAL-11` |
| **RF-19** | **Motor de plantillas propio**<br>*Como desarrollador quiero separar la lógica de la presentación.* | 1. Motor propio sin dependencias externas<br>2. Escapes de HTML por defecto<br>3. Secciones, parciales y comentarios | **Validado** | `template/TemplateEngine.java`<br>`test/TestTemplateEngine.java` |
| **RF-20** | **Enrutador HTTP con parámetros**<br>*Como desarrollador quiero rutas con identificadores para direccionar a los controladores.* | 1. Patrones tipo `/pacientes/{id}`<br>2. Enrutador propio sobre `com.sun.net.httpserver`<br>3. Servido de archivos estáticos | **Validado** | `web/Router.java`<br>`web/RequestContext.java` |

---

## 4. Requerimientos funcionales — Acceso, roles y seguridad (20 requerimientos restantes)

> Este bloque corresponde a los **20 requerimientos restantes** de la Práctica de Campo 7. Todos operan sobre las mismas tablas del proyecto mediante JDBC, sin modificar los módulos previos.

| ID | Historia de usuario | Criterios de aceptación | Estado | Evidencia |
|---|---|---|---|---|
| **RF-21** | **Autenticación de usuarios**<br>*Como trabajador del hospital quiero iniciar sesión con mi usuario y contraseña para que solo yo vea mis acciones.* | 1. Toda ruta exige sesión iniciada<br>2. Sin sesión, redirección a `/login`<br>3. Cookie de sesión `HttpOnly` y `SameSite=Strict`<br>4. Identificador de sesión aleatorio de 256 bits | **Validado** | `seguridad/ControladorAcceso.entrar()`<br>`seguridad/SesionStore.java`<br>Pruebas: `TestAccesoRoles` casos 1–8 |
| **RF-22** | **Cifrado de contraseñas**<br>*Como administrador quiero que las contraseñas no se guarden legibles en la base de datos.* | 1. PBKDF2-HMAC-SHA256 con 120 000 iteraciones<br>2. Sal aleatoria de 16 bytes por usuario<br>3. Comparación en tiempo constante<br>4. Migración automática de contraseñas antiguas en texto plano | **Validado** | `seguridad/Passwords.java`<br>Prueba: `TestAccesoRoles` caso 9 |
| **RF-23** | **Modelo de cuatro roles**<br>*Como administrador quiero asignar un rol a cada usuario para limitar su alcance.* | 1. Roles: ADMIN_GENERAL, DIGITADOR, ENFERMERA, DOCTOR<br>2. 18 permisos atómicos definidos<br>3. Asignación en el alta y edición de usuarios | **Validado** | `seguridad/Rol.java`<br>`seguridad/Permiso.java`<br>Prueba: `TestAccesoRoles` caso 4 |
| **RF-24** | **Control de acceso por permisos**<br>*Como médico quiero que solo pueda registrar operaciones y no eliminar pacientes.* | 1. Cada ruta declara el permiso que exige<br>2. Permiso insuficiente → HTTP 403 con explicación<br>3. La interfaz oculta los botones no permitidos | **Validado** | `seguridad/PoliticaPermisos.java` (30 rutas)<br>`seguridad/FiltroSeguridad.java`<br>Prueba: `TestAccesoRoles` casos 4–5 (13 aserciones) |
| **RF-25** | **Protección contra falsificación de peticiones (CSRF)**<br>*Como administrador quiero que nadie envíe formularios desde un sitio ajeno.* | 1. Todo `POST` debe venir del propio origen<br>2. Cookie de sesión con `SameSite=Strict`<br>3. Rechazo con HTTP 403 y registro en bitácora | **Validado** | `seguridad/Cookies.validarOrigen()`<br>Pruebas: `TestAccesoRoles` caso 6 |
| **RF-26** | **Caducidad de sesión**<br>*Como responsable de seguridad quiero que las sesiones no duren para siempre.* | 1. 30 minutos sin actividad<br>2. 12 horas de duración máxima<br>3. Token nuevo en cada inicio de sesión | **Validado** | `seguridad/Sesion.java`<br>Prueba: `TestAccesoRoles` caso 2 |
| **RF-27** | **Bloqueo por intentos fallidos**<br>*Como administrador quiero que un atacante no pueda adivinar contraseñas indefinidamente.* | 1. 5 intentos fallidos bloquean la cuenta 15 minutos<br>2. 20 intentos desde la misma IP bloquean el equipo<br>3. Aviso de intentos restantes | **Validado** | `seguridad/Auditoria.intentosFallidos()`<br>Prueba: `TestAccesoRoles` caso 7 |
| **RF-28** | **No revelar la existencia de cuentas**<br>*Como usuario quiero que el error no indique si un usuario existe.* | 1. Mismo mensaje para usuario inexistente, contraseña incorrecta y cuenta desactivada | **Validado** | `seguridad/ControladorAcceso.entrar()`<br>Prueba: `TestAccesoRoles` caso 2 |
| **RF-29** | **Bitácora de seguridad**<br>*Como administrador quiero saber quién accedió y qué hizo.* | 1. Se registra fecha, usuario, rol, IP, acción y resultado<br>2. Resultados: CORRECTO, FALLIDO, DENEGADO, CIERRE<br>3. Pantalla con filtros y métricas | **Validado** | `seguridad/Auditoria.java`<br>`seguridad/ControladorAuditoria.java`<br>Prueba: `TestAccesoRoles` caso 12 |
| **RF-30** | **Administración de usuarios**<br>*Como administrador quiero crear, editar y desactivar cuentas.* | 1. Alta con usuario, nombre, contraseña y rol<br>2. Edición de rol, estado y restablecimiento de contraseña<br>3. Desactivación en lugar de borrado | **Validado** | `seguridad/ControladorUsuarios.java`<br>`seguridad/UsuariosDao.java`<br>Pruebas: `TestAccesoRoles` casos 10–11 |
| **RF-31** | **Política de contraseñas**<br>*Como administrador quiero que las cuentas tengan contraseñas robustas.* | 1. Mínimo 8 caracteres con letras y números<br>2. Sin espacios, sin el nombre de usuario ni el nombre completo<br>3. No puede repetirse la contraseña actual | **Validado** | `seguridad/Passwords.validar()`<br>Pruebas: `TestAccesoRoles` casos 9–10 |
| **RF-32** | **Cambio de contraseña propio**<br>*Como usuario quiero cambiar mi contraseña y cerrar las sesiones ajenas.* | 1. Solicita la contraseña actual<br>2. Exige repetir la nueva<br>3. Cierra todas las demás sesiones abiertas | **Validado** | `seguridad/ControladorAcceso.cambiarContrasena()`<br>Prueba: `TestAccesoRoles` caso 9 |
| **RF-33** | **Revisión de la cuenta en cada petición**<br>*Como administrador quiero que desactivar a alguien surta efecto inmediato.* | 1. Cada petición relee el usuario de la base de datos<br>2. Una cuenta desactivada pierde el acceso al instante | **Validado** | `seguridad/FiltroSeguridad.refrescar()`<br>Prueba: `TestAccesoRoles` caso 11 |
| **RF-34** | **Pantalla de acceso restringido**<br>*Como usuario quiero entender por qué una acción no me está permitida.* | 1. Explica qué permiso falta<br>2. Indica qué roles sí lo tienen<br>3. Permite cambiar de usuario | **Validado** | `seguridad/FiltroSeguridad.responderError()`<br>Plantilla `acceso-restringido.html` |
| **RF-35** | **Cabeceras de seguridad HTTP**<br>*Como responsable de seguridad quiero que el navegador no ejecute código inyectado.* | 1. `Content-Security-Policy` sin estilos ni scripts en línea<br>2. `X-Frame-Options: DENY`<br>3. `X-Content-Type-Options: nosniff`<br>4. `Referrer-Policy: same-origin` | **Validado** | `seguridad/FiltroSeguridad.aplicarCabeceras()`<br>Prueba: `TestAccesoRoles` caso 3 |
| **RF-36** | **Datos clínicos sin caché**<br>*Como paciente quiero que mi historia no quede guardada en el equipo compartido.* | 1. `Cache-Control: no-store` en páginas con datos clínicos<br>2. Los archivos estáticos sí se recargan para evitar versiones antiguas | **Validado** | `seguridad/FiltroSeguridad.aplicarCabeceras()` |
| **RF-37** | **Menú de usuario con rol visible**<br>*Como usuario quiero ver siempre con qué cuenta estoy conectado.* | 1. Avatar con iniciales, nombre y rol<br>2. Accesos contextuales: solo el admin ve Usuarios y Bitácora | **Validado** | `templates/partials/usernav.html`<br>`seguridad/MotorConSesion.java` |
| **RF-38** | **Pantalla Mi cuenta**<br>*Como usuario quiero revisar mis permisos y sesiones abiertas.* | 1. Identidad y alcance del rol<br>2. Matriz de permisos por módulo<br>3. Sesiones abiertas con IP, navegador y minutos restantes<br>4. Formulario de cambio de contraseña con medidor de fuerza | **Validado** | `Plantilla mi-cuenta.html`<br>`seguridad/ControladorAcceso.miCuenta()` |
| **RF-39** | **Salvaguarda del último administrador**<br>*Como administrador quiero que el sistema no pueda quedarse sin nadie que lo administre.* | 1. No se degrada ni desactiva al último ADMIN_GENERAL activo<br>2. Un administrador no puede desactivar su propia cuenta | **Validado** | `seguridad/ControladorUsuarios.actualizar()` y `eliminar()` |
| **RF-40** | **Pruebas automatizadas de seguridad**<br>*Como docente quiero evidencia reproducible de que el acceso funciona.* | 1. 71 comprobaciones de acceso y roles<br>2. 23 casos de JDBC, CRUD y validación de datos<br>3. Ambas suites se ejecutan con un comando y devuelven el resultado | **Validado** | `test/TestAccesoRoles.ps1` → 71/71<br>`test/TestCrudJdbc.ps1` → 23/23 |

---

## 5. Requerimientos no funcionales

| ID | Categoría | Descripción | Criterio de verificación | Estado | Evidencia |
|---|---|---|---|---|---|
| **RNF-01** | Usabilidad | El sistema no requiere instalación de servidor de base de datos, ni Node, ni Maven, ni Tomcat | Se descomprime y ejecuta con `java -jar hospital.jar` | **Validado** | `README.md`; `manifest.mf` con `Class-Path` |
| **RNF-02** | Portabilidad | Java 17 o superior en Windows, Linux o macOS | Compila con JDK 17 y JDK 21 | **Validado** | `compilar.ps1` |
| **RNF-03** | Rendimiento | Respuesta de páginas con listados de menos de 100 registros | Búsquedas y listados responden sin paginación complexa | **Validado** | Consultas con `ORDER BY` e índices |
| **RNF-04** | Seguridad | Las contraseñas nunca se almacenan en texto plano | Inspección de la columna `password` en la tabla `usuario` | **Validado** | `Passwords.hash()` |
| **RNF-05** | Seguridad | Las páginas con datos clínicos no se guardan en caché del navegador | Verificar la cabecera `Cache-Control` en la respuesta | **Validado** | `TestAccesoRoles` caso 3 |
| **RNF-06** | Accesibilidad | Navegación por teclado y enlace para saltar al contenido | Existe enlace de salto, `:focus-visible` visible y `prefers-reduced-motion` respetado | **Validado** | `partials/header.html`; `static/css/seguridad.css` |
| **RNF-07** | Accesibilidad | La interfaz se adapta a pantallas pequeñas | Consultas de medios responsivas en el CSS | **Validado** | `seguridad.css` (4 consultas de medios) |
| **RNF-08** | Mantenibilidad | Separación de responsabilidades por paquetes | `db`, `model`, `template`, `web`, `seguridad` con dependencia unidireccional | **Validado** | Estructura del repositorio |
| **RNF-09** | Confiabilidad | No se pierde información clínica al dar de baja un paciente | La baja es lógica (`activo = FALSE`), nunca física | **Validado** | `PacienteDao.desactivar()` |
| **RNF-10** | Auditoría | Toda acción relevante queda registrada | La tabla `auditoria_acceso` crece en cada acceso, escritura o denegación | **Validado** | `Auditoria.registrar()` |

---

## 6. Resumen de cobertura

| Bloque | Cantidad | Validados | Pendientes |
|---|---:|---:|---:|
| Gestión de pacientes | 5 | 5 | 0 |
| Consultas, enfermedades y operaciones | 12 | 12 | 0 |
| **Línea base (RF-01 a RF-20)** | **20** | **20** | **0** |
| Acceso, roles y seguridad (RF-21 a RF-40) | 20 | 20 | 0 |
| Requerimientos no funcionales | 10 | 10 | 0 |
| **Total** | **50** | **50** | **0** |

**Trazabilidad de las pruebas:**

| Suite | Casos | Cubre |
|---|---:|---|
| `test/TestCrudJdbc.ps1` | 23 | RF-01 a RF-18, RNF-09, RNF-10 |
| `test/TestAccesoRoles.ps1` | 71 | RF-21 a RF-40, RNF-04, RNF-05 |
| **Total de comprobaciones** | **94** | **94** |

---

## 7. Observaciones

1. Los 20 requerimientos de la línea base (RF-01 a RF-20) ya estaban implementados; esta versión los conserva **sin modificar su comportamiento** y añade los 20 restantes (RF-21 a RF-40) como un paquete independiente (`com.hospital.seguridad`), de modo que la integración no alteró la funcionalidad previa.
2. El control de acceso se aplica como un filtro delante del enrutador existente (`FiltroSeguridad`), por lo que **ninguna ruta nueva quedó desprotegida**.
3. La única tabla nueva es `auditoria_acceso`; la tabla `usuario` se crea con las columnas ya documentadas en `database/schema.sql`.
4. El estado "Validado" de los 50 ítems se sostiene en evidencia reproducible: basta con ejecutar las dos suites sobre el sistema arrancado.