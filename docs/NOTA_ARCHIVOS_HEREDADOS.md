# Archivos heredados que no se compilan

En el ZIP original venían cuatro archivos sueltos de un intento anterior de
autenticación que **nunca estuvo conectado al sistema**:

| Archivo | Problema |
|---|---|
| `src/com/hospital/auth/AuthManager.java` | Empieza con un BOM UTF-8 que `javac` rechaza |
| `src/com/hospital/db/UsuarioDao.java` | Empieza con un BOM UTF-8 que `javac` rechaza |
| `src/com/hospital/web/AuthController.java` | Empieza con un BOM UTF-8 y además llama a métodos de sesión que `RequestContext` no tiene |
| `src/com/hospital/model/Usuario.java` | Compila, pero ninguna clase lo usaba |

Ninguno estaba en `hospital.jar`, ninguno estaba en `build/classes` y `Main`
no registraba ninguno: es decir, **el código fuente del ZIP original no
compilaba tal cual**.

## Qué se hizo

Para respetar la regla de no tocar el código existente, esos archivos se
dejaron **exactamente como estaban**. Su funcionalidad está reimplementada por
completo en el paquete nuevo `com.hospital.seguridad`:

| Antes (heredado, no compilaba) | Ahora (funciona) |
|---|---|
| `auth/AuthManager.java` | `seguridad/SesionStore.java` + `seguridad/Sesion.java` |
| `db/UsuarioDao.java` | `seguridad/UsuariosDao.java` |
| `web/AuthController.java` | `seguridad/ControladorAcceso.java` |
| contraseñas en texto plano | `seguridad/Passwords.java` (PBKDF2-HMAC-SHA256) |
| sin control de roles | `seguridad/Rol.java` + `Permiso.java` + `PoliticaPermisos.java` |
| sin bitácora | `seguridad/Auditoria.java` |

El script de compilación (`compilar.bat` / `compilar.ps1`) excluye los tres
archivos rotos del empaquetado, así que `hospital.jar` se genera bien.

## Si quieres limpiarlos

Borra los tres archivos (el cuarto, `Usuario.java`, sí compila y no molesta) y
quedaría el proyecto sin restos del intento anterior:

```
src/com/hospital/auth/       (carpeta completa)
src/com/hospital/db/UsuarioDao.java
src/com/hospital/web/AuthController.java
```

No hay que tocar nada más: ninguna clase del sistema los referencia.