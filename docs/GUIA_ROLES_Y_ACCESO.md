# Guia de acceso y roles — Hospital HC

Como entrar al sistema, que puede hacer cada rol y que medidas de seguridad
protegen las historias clinicas.

## 1. Entrar por primera vez

1. Abre el sistema en el navegador: `http://localhost:8080`
2. Cualquier direccion te lleva a la **pantalla de acceso**: sin sesion no se
   muestra ningun dato del paciente.
3. En la pantalla hay cuatro tarjetas de **cuentas de demostracion**. Pulsa
   una para rellenar el usuario y la contrasena, o escribelos a mano.

| Usuario | Contrasena | Rol | Uso principal |
|---|---|---|---|
| `admin` | `Admin2026` | Administrador general | Todo, incluidos usuarios y bitacora |
| `doctor` | `Doctor2026` | Medico | Historia clinica completa, con operaciones |
| `enfermera` | `Enfermera2026` | Enfermeria | Consultas y enfermedades |
| `digitador` | `Digitador2026` | Digitador | Captura de pacientes; la clinica solo se lee |

> **Cambia estas contrasenas antes de usar el sistema con historias clinicas
> reales.** Mientras alguna cuenta conserve su clave de fabrica, el sistema
> muestra un aviso en rojo en cada pagina.

Estas cuentas tambien quedan escritas en `data/CREDENCIALES-DEMOSTRACION.txt`.

## 2. Que ve cada rol en la pantalla

El menu superior muestra siempre tu nombre, tus iniciales y tu rol. El menu
desplegable lleva a **Mi cuenta**, y solo el administrador general ve ademas
**Usuarios del sistema** y **Bitacora de seguridad**.

La matriz completa de permisos esta en
[`MATRIZ_PERMISOS.md`](MATRIZ_PERMISOS.md). En resumen:

| Accion | Admin | Digitador | Enfermera | Medico |
|---|:---:|:---:|:---:|:---:|
| Ver pacientes y busar | Si | Si | Si | Si |
| Registrar y editar pacientes | Si | Si | Si | Si |
| **Dar de baja** un paciente | **Si** | No | No | No |
| Registrar consultas | Si | No | Si | Si |
| Registrar enfermedades | Si | No | Si | Si |
| **Registrar operaciones** | **Si** | No | **No** | Si |
| Administrar usuarios | Si | No | No | No |
| Ver la bitacora de seguridad | Si | No | No | No |

Si un rol intenta una accion que no le corresponde, el sistema responde con una
pantalla **"Tu rol no tiene acceso a esta accion"** que explica que permiso
falta, muestra que roles si lo tienen y deja cambiar de usuario. **El boton
tampoco aparece**: la interfaz y la seguridad nunca se contradicen, porque ambas
leen la misma tabla de permisos.

## 3. Mi cuenta

`Mi cuenta` (en el menu del avatar) muestra:

- **Identidad**: nombre, usuario y rol asignado.
- **Esta sesion**: direccion IP de origen, navegador y minutos restantes antes
  del cierre automatico.
- **Sesiones abiertas de mi cuenta**: si ves una sesion que no reconoces,
  cambia tu contrasena: eso cerrara las demas al instante.
- **Mis permisos por modulo**: el detalle de lo que tu rol puede o no puede
  hacer.
- **Cambiar mi contrasena**: pide la actual, la nueva y su repeticion, con un
  medidor de fuerza en vivo.

### Reglas de la contrasena

- Minimo 8 caracteres, con letras y numeros.
- No puede contener espacios, ni tu nombre de usuario, ni tu nombre completo.
- No puede ser igual a la contrasena actual.

Al cambiarla se cierran automaticamente todas tus otras sesiones.

## 4. Administrar usuarios (solo administrador general)

En **Usuarios del sistema** puedes crear cuentas, cambiar el rol, restablecer
la contrasena y desactivar cuentas.

El sistema te protege de tres errores:

- **No se puede desactivar ni degradar al unico administrador general activo.**
- **Un administrador no puede desactivar su propia cuenta.**
- **Al desactivar a alguien o cambiarle la contrasena, sus sesiones abiertas se
  cierran en el acto.**

En la misma pantalla tienes, al pie, una tabla resumen de lo que puede hacer
cada rol.

## 5. Bitacora de seguridad (solo administrador general)

Cada evento relevante queda registrado con fecha, usuario, rol, IP, accion y
resultado:

| Resultado | Significado |
|---|---|
| `CORRECTO` | Acceso concedido u operacion realizada |
| `FALLIDO` | Credenciales incorrectas en un intento de acceso |
| `DENEGADO` | Peticion bloqueada: sin permiso, sin sesion o con formulario no confiable |
| `CIERRE` | Cierre de sesion voluntary |

Se puede filtrar por usuario y por tipo de resultado. Es la forma de detectar
un uso indebido: intentos fallidos repetidos, o accesos a modulos que la
persona no deberia usar.

## 6. Medidas de seguridad implementadas

| Medida | Como funciona |
|---|---|
| Contrasenas cifradas | PBKDF2-HMAC-SHA256, 120 000 iteraciones y sal propia por usuario. Nunca se guarda el texto plano. |
| Cookie de sesion segura | `HttpOnly` (invisible a JavaScript), `SameSite=Strict`, `Path=/`, `Secure` si la conexion es HTTPS, con identificador aleatorio de 256 bits. |
| Caducidad de sesion | 30 minutos sin actividad y 12 horas de duracion maxima. |
| Sin fijacion de sesion | Cada inicio de sesion genera un token nuevo y cierra las anteriores de esa cuenta. |
| Bloqueo por fuerza bruta | 5 intentos fallidos bloquean la cuenta 15 minutos; ademas 20 intentos desde la misma IP bloquean el equipo. |
| Sin revelar usuarios | Usuario inexistente, contrasena incorrecta y cuenta desactivada devuelven el mismo mensaje. |
| Proteccion CSRF | Toda peticion `POST` debe venir del propio origen (`Origin`/`Referer`) y las sesiones usan `SameSite=Strict`. |
| Cabeceras de seguridad | CSP sin estilos ni scripts en linea, `X-Frame-Options: DENY` (no se puede incrustar en otra pagina), `nosniff`, `Referrer-Policy: same-origin`. |
| Sin cache en el navegador | Las paginas con datos clinicos se envian con `Cache-Control: no-store`. |
| Revision continua | Cada peticion relee el usuario de la base de datos: si fue desactivado o le cambiaron el rol, pierde el acceso al instante. |
| Bitacora de seguridad | Todo acceso, operacion y denegacion queda registrado. |
| Politica de contrasenas | Validacion en el servidor al crear usuarios y al cambiar claves. |

## 7. Cambiar las contrasenas de demostracion

1. Entra como `admin` / `Admin2026`.
2. Abre **Usuarios del sistema**.
3. Pulsa **Editar** en la cuenta que quieras cambiar.
4. Escribe la nueva contrasena y guarda. Si la dejas vacia, se conserva la
   anterior.
5. Haz lo mismo con tu propia cuenta desde **Mi cuenta**.

Para cambiar la contrasena de `admin` puedes usar cualquier rol: al cambiar una
contrasena solo se cierran las sesiones del usuario EDITADO, no las tuyas.

## 8. Comprobar que la seguridad funciona

Con el servidor arrancado en el puerto 8099:

```
java -jar hospital.jar 8099
powershell -ExecutionPolicy Bypass -File test\TestAccesoRoles.ps1
```

Son **71 comprobaciones automaticas**: proteccion de rutas sin sesion, permisos
de los cuatro roles, cabeceras de seguridad, CSRF, bloqueo por intentos
fallidos, cierre de sesion, cambio de contrasena, administracion de usuarios y
registro en la bitacora. Las pruebas usan cuentas desechables, pero conviene
empezar con una base de datos limpia (borra la carpeta `data/`).

## 9. Limitaciones conocidas

- La sesion vive **en memoria**: si se apaga el servidor, todos deben volver a
  entrar. Es coherente con un sistema de un solo servidor sin dependencias
  externas, pero en un despliegue real habria que guardarlas en la base de datos
  o en un almacen compartido.
- El bloqueo por intentos fallidos es **por cuenta y por IP**, con ventana de 15
  minutos. Un atacante podria dejar sin acceso a una cuenta concreta durante
  esa ventana; para un despliegue real conviene anadir retardo progresivo o
  captcha.
- El transporte es **HTTP sin cifrar** en la configuracion por defecto. En una
  red real hay que servirlo por HTTPS: de lo contrario la cookie pierde su
  atributo `Secure` y las contrasenas viajan en claro por la red.
- Las cuentas de demostracion son conocidas: por eso el sistema avisa mientras
  las conserve.