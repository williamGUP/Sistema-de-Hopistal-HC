# Sistema de Hospital HC

Aplicación web para registrar y consultar pacientes de un hospital: datos generales, DNI, número de historia clínica, consultas médicas, enfermedades / antecedentes y operaciones.

El sistema está escrito en **Java puro, sin frameworks externos**, y guarda todo en una base de datos embebida **H2**, así que no requiere instalar ni configurar nada aparte de tener Java instalado.

Desde esta versión incorpora **acceso con usuario y contraseña y control de permisos por rol**, de modo que cada persona del hospital solo ve y modifica lo que su función le permite.

---

## Funcionalidades

- **Registro y consulta de pacientes** con DNI, número de historia clínica, datos de contacto, grupo sanguíneo y alergias.
- **Historia clínica** por paciente: consultas médicas, enfermedades/antecedentes y operaciones quirúrgicas.
- **Búsqueda** por DNI, número de historia, nombres o apellidos.
- **Acceso con usuario y contraseña** para cuatro roles con permisos distintos.
- **Bitácora de seguridad**: registro de accesos, cierres de sesión, intentos fallidos y operaciones denegadas.
- **Gestión de usuarios** desde la propia aplicación (solo el administrador general).
- **Sin dependencias de terceros**: todo el código y el driver de base de datos viajan en el paquete.

---

## Roles y permisos

| | Administrador general | Digitador | Enfermería | Médico |
|---|:---:|:---:|:---:|:---:|
| Ver pacientes e historias clínicas | Si | Si | Si | Si |
| Registrar y editar pacientes | Si | Si | Si | Si |
| **Dar de baja** a un paciente | **Si** | No | No | No |
| Ver consultas médicas | Si | Si | Si | Si |
| **Registrar y eliminar** consultas | Si | **No** | Si | Si |
| **Registrar y eliminar** enfermedades | Si | **No** | Si | Si |
| Ver operaciones | Si | Si | Si | Si |
| **Registrar y eliminar** operaciones | Si | **No** | **No** | Si |
| Administrar usuarios | Si | No | No | No |
| Ver la bitácora de seguridad | Si | No | No | No |

Resumen: **Administrador general 18 permisos · Médico 12 · Enfermería 10 · Digitador 6** (sobre 18 posibles).

La matriz completa, con la ruta del sistema que exige cada permiso, está en [`docs/MATRIZ_PERMISOS.md`](docs/MATRIZ_PERMISOS.md).

---

## Acceso al sistema

Al abrir la aplicación sin sesión iniciada, cualquier dirección redirige a la pantalla de acceso: **sin iniciar sesión no se muestra ningún dato de paciente**.

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin2026` | Administrador general |
| `doctor` | `Doctor2026` | Médico |
| `enfermera` | `Enfermera2026` | Enfermería |
| `digitador` | `Digitador2026` | Digitador |

Estas cuentas se crean solas en el primer arranque y son **de demostración**. Mientras alguna conserve su contraseña de fábrica, el sistema muestra un aviso en cada pantalla. **Cámbialas antes de usar el sistema con historias clínicas reales** (en *Mi cuenta* para la propia, o en *Usuarios del sistema* para las demás).

---

## Requisitos

- **Java 17 o superior** (JDK o JRE). Comprueba tu versión con:

  ```
  java -version
  ```

  Si no tienes Java instalado, descárgalo de <https://adoptium.net/> (elige la versión "LTS" más reciente para tu sistema).

No hace falta Maven, ni Node, ni un servidor de base de datos aparte, ni Apache ni Tomcat. Todo viene incluido.

---

## Puesta en marcha

1. Descomprime el paquete en cualquier carpeta.
2. Abre una terminal (o el Símbolo del sistema en Windows) dentro de esa carpeta, donde está `hospital.jar`.
3. Ejecuta:

   ```
   java -jar hospital.jar
   ```

4. Abre `http://localhost:8080/` en el navegador e inicia sesión con una de las cuentas de demostración.

Para detener el servidor, vuelve a la terminal y presiona `Ctrl+C`.

### Cambiar el puerto

Por defecto usa el **8080**. Puedes cambiarlo de tres formas:

```
java -jar hospital.jar 9090
```

```
HOSPITAL_PORT=9090 java -jar hospital.jar
```

```
java -Dport=9090 -jar hospital.jar
```

---

## Dónde se guardan los datos

La primera vez que arranca, el sistema crea una carpeta `data/` junto al `hospital.jar` con la base de datos (`hospital.mv.db`) y el archivo `CREDENCIALES-DEMOSTRACION.txt`.

- **Respaldar**: copia la carpeta `data/`.
- **Empezar de cero**: borra la carpeta `data/` y arranca de nuevo (se recrea con 3 pacientes de ejemplo).

---

## Medidas de seguridad implementadas

| Medida | Cómo funciona |
|---|---|
| Contraseñas cifradas | PBKDF2-HMAC-SHA256, 120 000 iteraciones y sal propia por usuario. Nunca se guarda el texto plano. |
| Cookie de sesión segura | `HttpOnly` (invisible a JavaScript), `SameSite=Strict`, `Path=/`, `Secure` si la conexión es HTTPS, con identificador aleatorio de 256 bits. |
| Caducidad de sesión | 30 minutos sin actividad y 12 horas de duración máxima. |
| Sin fijación de sesión | Cada inicio de sesión genera un token nuevo y cierra las anteriores de esa cuenta. |
| Bloqueo por fuerza bruta | 5 intentos fallidos bloquean la cuenta 15 minutos; además 20 intentos desde la misma IP bloquean el equipo. |
| Sin revelar cuentas | Usuario inexistente, contraseña incorrecta y cuenta desactivada devuelven el mismo mensaje. |
| Protección CSRF | Toda petición `POST` debe venir del propio origen y las sesiones usan `SameSite=Strict`. |
| Cabeceras de seguridad | CSP sin estilos ni scripts en línea, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: same-origin`. |
| Sin caché en el navegador | Las páginas con datos clínicos se envían con `Cache-Control: no-store`. |
| Revisión continua | Cada petición relee el usuario de la base de datos: si fue desactivado o le cambiaron el rol, pierde el acceso al instante. |
| Bitácora | Todo acceso, operación y denegación queda registrado con usuario, rol, IP y resultado. |

---

## Compilar desde el código fuente

```
compilar.bat
```

Equivale a:

```
javac -encoding UTF-8 -d build/classes -cp lib/h2-2.2.224.jar @build/fuentes.txt
cd build/classes
jar cfm ../hospital.jar ../manifest.mf com
```

En Linux o macOS:

```
./compilar.ps1     # requiere PowerShell 7
```

> **Nota:** el script excluye tres archivos heredados que venía en el paquete original con un BOM UTF-8 que `javac` rechaza y que nunca se utilizaban. El detalle está en [`docs/NOTA_ARCHIVOS_HEREDADOS.md`](docs/NOTA_ARCHIVOS_HEREDADOS.md).

---

## Estructura del proyecto

```
hospital/
├── src/com/hospital/
│   ├── Main.java              Punto de entrada: arranca base de datos, plantillas y servidor
│   ├── db/                    Acceso a datos (Db, PacienteDao, ConsultaDao, ...)
│   ├── model/                 Clases de datos (Paciente, Consulta, Operacion, Usuario)
│   ├── template/              Motor de plantillas HTML propio
│   ├── web/                   Controladores y enrutador HTTP
│   └── seguridad/             Acceso por roles: Permiso, Rol, PoliticaPermisos,
│                             Passwords, Sesion, FiltroSeguridad, Auditoria, ...
├── templates/                 Plantillas HTML de las pantallas
├── static/                    Hojas de estilo (CSS), íconos y scripts
├── database/                  Esquema de la base de datos (referencia)
├── docs/                      Manuales, matriz de permisos y diagramas
├── test/                      Pruebas del motor de plantillas y de acceso por roles
├── lib/                       Driver H2
├── manifest.mf                Manifiesto del jar
├── compilar.bat / compilar.ps1  Compilación del proyecto
└── hospital.jar               Aplicación ejecutable
```

---

## Pruebas

Con el servidor arrancado:

```
java -jar hospital.jar 8080
powershell -ExecutionPolicy Bypass -File test\TestCrudJdbc.ps1
powershell -ExecutionPolicy Bypass -File test\TestAccesoRoles.ps1
```

Son **94 comprobaciones automáticas**:

| Suite | Casos | Qué verifica |
|---|---:|---|
| `test/TestCrudJdbc.ps1` | 23 | Operaciones CRUD sobre las cuatro tablas, validación de datos no válidos y control de errores de conexión |
| `test/TestAccesoRoles.ps1` | 71 | Protección de rutas, permisos de los cuatro roles, cabeceras de seguridad, CSRF, bloqueo por fuerza bruta, cierre de sesión, cambio de contraseña y administración de usuarios |

El detalle de cada caso, con los datos usados y el resultado obtenido, está en [`docs/INFORME_DE_PRUEBAS.md`](docs/INFORME_DE_PRUEBAS.md).

---

## Documentación

- **Matriz de trazabilidad**: [`docs/MATRIZ_RASTREABILIDAD.md`](docs/MATRIZ_RASTREABILIDAD.md) — los 40 requerimientos funcionales y 10 no funcionales con historia de usuario, criterios de aceptación, estado y evidencia.
- **Integración JDBC y CRUD**: [`docs/JDBC_Y_CRUD.md`](docs/JDBC_Y_CRUD.md) — parámetros de conexión, sentencias SQL de las cuatro operaciones y buenas prácticas aplicadas.
- **Informe de pruebas**: [`docs/INFORME_DE_PRUEBAS.md`](docs/INFORME_DE_PRUEBAS.md) — metodología, 94 casos con resultados reales y defectos encontrados.
- **Guía de acceso y roles**: [`docs/GUIA_ROLES_Y_ACCESO.md`](docs/GUIA_ROLES_Y_ACCESO.md) — cómo entrar, qué puede hacer cada rol, cómo cambiar contraseñas y qué medidas protegen las historias clínicas.
- **Matriz de permisos**: [`docs/MATRIZ_PERMISOS.md`](docs/MATRIZ_PERMISOS.md) — qué acción CRUD puede hacer cada rol sobre cada módulo, y qué ruta exige qué permiso.
- **Tabla de aportes individuales**: [`APORTES_INDIVIDUALES.md`](APORTES_INDIVIDUALES.md) — distribución del trabajo con evidencia del historial de Git.
- **Manual de usuario**: [`docs/MANUAL_USUARIO.md`](docs/MANUAL_USUARIO.md) — cómo registrar pacientes, buscar historias clínicas y agregar consultas, enfermedades y operaciones.
- **Diccionario de datos**: [`docs/DICCIONARIO_DATOS.md`](docs/DICCIONARIO_DATOS.md) — cada tabla y campo de la base de datos.
- **Esquemas SQL**: [`database/schema.sql`](database/schema.sql) y [`database/seguridad.sql`](database/seguridad.sql).
- **Flujograma del proceso clínico**: [`docs/diagramas/flujograma.png`](docs/diagramas/flujograma.png)
- **Diagrama entidad-relación**: [`docs/diagramas/entidad-relacion.png`](docs/diagramas/entidad-relacion.png)

---

## Limitaciones conocidas

- **Los datos clínicos no están cifrados en el disco.** El archivo `data/hospital.mv.db` contiene la información de los pacientes en texto plano dentro del formato propio de H2. Las *contraseñas de usuario* sí están cifradas (PBKDF2), pero los datos clínicos no. Protege el acceso a esa carpeta como lo harías con cualquier historia clínica en papel.
- **El transporte es HTTP sin cifrar por defecto.** Si vas a usarlo en una red real, sirve el sistema por HTTPS: de lo contrario las contraseñas viajan en claro y la cookie de sesión no lleva el atributo `Secure`.
- **Las sesiones viven en memoria.** Si se apaga el servidor, todos deben volver a entrar.
- **El formato de DNI validado es el peruano (8 dígitos).** Si tu país usa otro documento de identidad, se ajusta en una sola línea del código (`PacienteController.DNI_PATTERN`).
- **Un solo servidor, una sola base de datos.** Cada instalación es independiente; si necesitas varias sedes, instala una copia por sede.