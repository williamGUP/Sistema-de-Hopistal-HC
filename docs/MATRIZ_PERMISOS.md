# Matriz de permisos por rol — Hospital HC

Control de acceso de tipo **RBAC** (Role-Based Access Control). Cada rol tiene
asignado un conjunto de permisos y cada ruta del sistema exige un permiso
concreto: si un rol no lo tiene, la peticion se rechaza con **403** aunque el
usuario escriba la direccion a mano en el navegador.

- Definicion de permisos: `src/com/hospital/seguridad/Permiso.java`
- Permisos de cada rol: `src/com/hospital/seguridad/Rol.java`
- Ruta -> permiso: `src/com/hospital/seguridad/PoliticaPermisos.java`
- Comprobacion en cada peticion: `src/com/hospital/seguridad/FiltroSeguridad.java`

## Resumen de los cuatro roles

| Rol | Alcance | Puede crear | Puede eliminar | Administracion |
|---|---|---|---|---|
| **ADMIN_GENERAL** — Administrador general | Control total del sistema | Todo | Todo | Usuarios y bitacora |
| **DIGITADOR** — Digitador | Captura y depuracion de datos de pacientes; la informacion clinica solo se lee | Pacientes | Nada | No |
| **ENFERMERA** — Enfermeria | Seguimiento clinico: consultas y enfermedades | Pacientes, consultas, enfermedades | Consultas y enfermedades | No |
| **DOCTOR** — Medico | Historia clinica completa, incluidas las operaciones | Todos los modulos clinicos | Consultas, enfermedades y operaciones | No |

## Matriz completa

| Modulo | Permiso | ADMIN_GENERAL | DIGITADOR | ENFERMERA | DOCTOR |
|---|---|:---:|:---:|:---:|:---:|
| Pacientes | Ver listado e historia clinica | Si | Si | Si | Si |
| Pacientes | Registrar | Si | Si | Si | Si |
| Pacientes | Editar | Si | Si | Si | Si |
| Pacientes | Dar de baja | **Si** | No | No | No |
| Consultas | Ver | Si | Si | Si | Si |
| Consultas | Registrar | Si | **No** | Si | Si |
| Consultas | Eliminar | Si | **No** | Si | Si |
| Enfermedades | Ver | Si | Si | Si | Si |
| Enfermedades | Registrar | Si | **No** | Si | Si |
| Enfermedades | Eliminar | Si | **No** | Si | Si |
| Operaciones | Ver | Si | Si | Si | Si |
| Operaciones | Registrar | Si | **No** | **No** | Si |
| Operaciones | Eliminar | Si | **No** | **No** | Si |
| Usuarios | Ver | Si | No | No | No |
| Usuarios | Crear | Si | No | No | No |
| Usuarios | Editar (rol, estado, restablecer clave) | Si | No | No | No |
| Usuarios | Desactivar | Si | No | No | No |
| Auditoria | Ver bitacora | Si | No | No | No |

Resumen en numeros: **ADMIN_GENERAL 18 permisos · DIGITADOR 6 · ENFERMERA 10 ·
DOCTOR 12** (sobre 18 permisos posibles).

## Rutas del sistema y permiso exigido

| Metodo | Ruta | Permiso |
|---|---|---|
| GET, POST | `/login` | *publica* |
| GET | `/` | *iniciar sesion* |
| GET | `/pacientes` | `paciente.ver` |
| GET, POST | `/pacientes/nuevo` | `paciente.crear` |
| GET | `/pacientes/{id}` | `paciente.ver` |
| GET, POST | `/pacientes/{id}/editar` | `paciente.editar` |
| POST | `/pacientes/{id}/eliminar` | `paciente.eliminar` |
| GET, POST | `/pacientes/{id}/consultas/nueva` | `consulta.crear` |
| POST | `/consultas/{id}/eliminar` | `consulta.eliminar` |
| GET, POST | `/pacientes/{id}/enfermedades/nueva` | `enfermedad.crear` |
| POST | `/enfermedades/{id}/eliminar` | `enfermedad.eliminar` |
| GET, POST | `/pacientes/{id}/operaciones/nueva` | `operacion.crear` |
| POST | `/operaciones/{id}/eliminar` | `operacion.eliminar` |
| GET, POST | `/logout` | *iniciar sesion* |
| GET, POST | `/mi-cuenta` | *iniciar sesion* |
| GET | `/usuarios` | `usuario.ver` |
| GET, POST | `/usuarios/nuevo` | `usuario.crear` |
| GET, POST | `/usuarios/{id}/editar` | `usuario.editar` |
| POST | `/usuarios/{id}/eliminar` | `usuario.eliminar` |
| GET | `/auditoria` | `auditoria.ver` |
| GET | `/static/*` | *publica* (hojas de estilo e imagenes) |

## Decisiones de diseno que explican la matriz

1. **Dar de baja a un paciente es exclusivo del administrador general.** Es una
   accion que afecta al historico completo de una persona, asi que no se
   delegua. Los demas roles pueden corregir datos, pero no borrarlos.
   La baja es *logica* (`activo = FALSE`): la historia clinica nunca se borra.
2. **El digitador no escribe nada clinico.** Su trabajo es capturar y depurar
   datos de pacientes con precision; anadir diagnosticos o cirugias excede su
   funcion, asi que solo puede leer esa seccion.
3. **La enfermera no registra operaciones.** Registrar una operacion exige el
   Cirujano responsable, un dato que pertenece al medico.
4. **La enfermeria puede eliminar consultas y enfermedades** porque son
   registros de seguimiento que se corrigen con frecuencia (un error de tipeo al
   tomar los signos, por ejemplo). El medico tiene el mismo control.
5. **Solo el administrador general gestiona usuarios y consulta la bitacora.**
   Si un rol pudiera crear cuentas ni ver los accesos, el control de acceso
   seria ineffective.

## Salvaguardas adicionales del administrador

- No se puede **desactivar ni degradar** al unico administrador general activo
  (el sistema se quedaria sin nadie que lo administre).
- Un administrador **no puede desactivar su propia cuenta**.
- **Cambiar la contrasena o desactivar a un usuario cierra todas sus
  sesiones abiertas** en el acto.
- El **nombre de usuario no se puede editar** una vez creado, para que la
  bitacora siga siendo coherente.