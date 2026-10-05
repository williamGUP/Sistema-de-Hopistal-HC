# Tabla de Aportes Individuales — Sistema de Hospital HC

**Proyecto:** Sistema de Registro de Pacientes — Hospital HC
**Curso:** Técnicas de Programación Orientada a Objetos — Unidad III
**Práctica:** Campo 7 — Integración y validación del proyecto
**Repositorio:** <https://github.com/williamGUP/Sistema-de-Hopistal-HC>
**Fecha de actualización:** 2026-10-05

---

## 1. Integrantes del equipo

| # | Usuario de GitHub | Rol principal |
|---|---|---|
| 1 | **williamGUP** | Líder técnico · Backend · Persistencia JDBC · Arquitectura |
| 2 | **RegueraMcLovin** | Controladores web · Modelos · Documentación · Compilación |
| 3 | **hugpers423-sketch** | Interfaz y plantillas · Control de acceso y seguridad · Pruebas |
| 4 | **jfarronan9-alt** | Modelos de datos (enfermedad) |
| 5 | **yosminureta** | Modelos de datos (operación) |

---

## 2. Evidencia objetiva: commits por autor

Los datos se obtuvieron del historial real del repositorio con:

```bash
git log --all --pretty=format:"%h|%an|%ad|%s" --date=short
```

| Usuario | Commits | Periodo de actividad | Primer commit | Último commit |
|---|---:|---|---|---|
| hugpers423-sketch | 38 | 14 sep – 5 oct 2026 | `c740674` Añade clase `Views` | `3064ece` Merge PR #2 |
| RegueraMcLovin | 34 | 14 sep – 4 oct 2026 | `0741a73` Clase `Paciente` | `8fbddaf` Script `compilar.ps1` |
| williamGUP | 24 | 13 sep – 4 oct 2026 | `a4f850f` Merge inicial | `e6b2251` Mejoras de seguridad |
| jfarronan9-alt | 3 | 14 sep 2026 | `5cf58f3` Crea `Enfermedad.java` | `bc4aa57` Crea `enfermedad.java` |
| yosminureta | 1 | 14 sep 2026 | `1388b24` Crea `operacion.java` | — |
| **Total** | **100** | **13 sep – 5 oct 2026** | | |

> La participación se repartió de forma equilibrada entre los tres
> integrantes principales (38 / 34 / 24 commits), sin que ninguno concentre la
> totalidad del trabajo.

---

## 3. Distribución del trabajo por módulo

| Módulo / Archivo | williamGUP | RegueraMcLovin | hugpers423-sketch |
|---|:---:|:---:|:---:|
| **Arquitectura y arranque** | | | |
| `Main.java` | ✅ Principal | | ✅ Integración de seguridad |
| `Router.java`, `RequestContext.java` | ✅ Principal | | |
| **Persistencia JDBC** | | | |
| `db/Db.java` (conexión, esquema, *seed*) | ✅ Principal | | |
| `db/AppPaths.java` | ✅ Principal | | |
| `db/PacienteDao.java` (CRUD) | ✅ Principal | | |
| `db/ConsultaDao.java` (CRUD) | ✅ Principal | | |
| `db/EnfermedadDao.java` (CRUD) | ✅ Principal | | |
| `db/OperacionDao.java` (CRUD) | ✅ Principal | | |
| **Modelos** | | | |
| `model/Paciente.java` | | ✅ Principal | |
| `model/Consulta.java` | | ✅ Principal | |
| `model/Enfermedad.java` | | | ✅ `jfarronan9-alt` |
| `model/Operacion.java` | | | ✅ `yosminureta` |
| `model/Usuario.java` | ✅ Principal | | |
| **Controladores web** | | | |
| `web/PacienteController.java` | | ✅ Principal | |
| `web/ConsultaController.java` | | ✅ Principal | |
| `web/EnfermedadController.java` | | ✅ Principal | |
| `web/OperacionController.java` | | ✅ Principal | |
| `web/Catalogos.java`, `web/Views.java` | | ✅ Principal | |
| **Motor de plantillas** | | | |
| `template/TemplateEngine.java` | ✅ Principal | | ✅ Pruebas |
| **Interfaz y plantillas HTML** | | | |
| `templates/` (formularios y listados) | | | ✅ Principal |
| `templates/partials/header.html`, `footer.html` | | | ✅ Principal |
| `static/css/estilo.css` | ✅ Principal | | |
| `static/css/seguridad.css` | | | ✅ Principal |
| `static/js/seguridad.js` | | | ✅ Principal |
| `static/img/favicon.svg` | | ✅ Principal | |
| **Acceso, roles y seguridad** | | | |
| `seguridad/Permiso.java`, `Rol.java` | | | ✅ Principal |
| `seguridad/PoliticaPermisos.java` | | | ✅ Principal |
| `seguridad/Passwords.java` | | | ✅ Principal |
| `seguridad/FiltroSeguridad.java` | ✅ Revisión | | ✅ Principal |
| `seguridad/Controlador*.java` | | | ✅ Principal |
| `seguridad/Auditoria.java`, `UsuariosDao.java` | | | ✅ Principal |
| **Compilación y despliegue** | | | |
| `compilar.bat`, `compilar.ps1` | | ✅ Principal | |
| `manifest.mf` | | ✅ Principal | |
| `database/seguridad.sql` | | ✅ Principal | |
| **Pruebas** | | | |
| `test/TestTemplateEngine.java` | | | ✅ Principal |
| `test/TestAccesoRoles.ps1` (71 casos) | | | ✅ Principal |
| `test/TestCrudJdbc.ps1` (23 casos) | | | ✅ Principal |
| **Documentación** | | | |
| `docs/MANUAL_USUARIO.md` | | ✅ Principal | |
| `docs/DICCIONARIO_DATOS.md` | | ✅ Principal | |
| `docs/diagramas/*.mmd`, `*.png` | | ✅ Principal | |
| `docs/capturas/*.png` | | ✅ Principal | |
| `docs/GUIA_ROLES_Y_ACCESO.md` | | ✅ Principal | |
| `docs/MATRIZ_PERMISOS.md` | | ✅ Principal | |
| `docs/MATRIZ_RASTREABILIDAD.md` | ✅ Principal | ✅ Revisión | |
| `docs/JDBC_Y_CRUD.md`, `docs/INFORME_DE_PRUEBAS.md` | ✅ Principal | ✅ Revisión | |
| `APORTES_INDIVIDUALES.md` | ✅ Principal | ✅ Revisión | |
| `README.md` | ✅ Principal | ✅ Revisión | ✅ Actualización de roles |

---

## 4. Hitos del proyecto

| Fecha | Hito | Participantes |
|---|---|---|
| 13 – 14 sep | Estructura inicial del proyecto, modelos de datos y base CRUD | Los tres |
| 14 – 21 sep | Controladores web, enrutador, motor de plantillas y primeras plantillas | williamGUP, RegueraMcLovin, hugpers423-sketch |
| 21 – 3 oct | Estilización, diagramas, manual de usuario y capturas | williamGUP, RegueraMcLovin |
| 3 – 4 oct | Módulo de seguridad: 16 clases, CSS y JS de acceso, guías de roles | hugpers423-sketch, RegueraMcLovin |
| 4 – 5 oct | Compilación, pruebas automatizadas y documentación técnica | hugpers423-sketch |
| 5 oct | Fusión de la rama de corrección y actualización del README | williamGUP |

---

## 5. Evidencia de trabajo colaborativo

### 5.1 Ramas y fusiones

```
main                            rama principal, con todo el trabajo integrado
fix/complilar-y-case            rama de corrección de compilación e integración
jfarronan9-alt-patch-1          rama de Parallel Feature (modelo enfermedad)
```

- **PR #2**: fusión de `fix/complilar-y-case` → `main` (commit `3064ece`).
- La corrección se desarrolló en una rama aparte y se verificó compilando y
  ejecutando las dos suites de pruebas antes de fusionar.

### 5.2 Commits representativos por autor

| Autor | Commit | Descripción |
|---|---|---|
| williamGUP | `4f26b79` | Esquema de base de datos |
| williamGUP | `d08c95b` | Estructura de carpetas del proyecto |
| williamGUP | `e6b2251` | Mejoras de seguridad en la plataforma |
| RegueraMcLovin | `c0074bf` | Sistema de gestión de pacientes con CRUD |
| RegueraMcLovin | `bee8d14` | `ConsultaController` |
| RegueraMcLovin | `497b013` | Diccionario de datos |
| RegueraMcLovin | `417bde8` | Matriz de permisos |
| hugpers423-sketch | `02ace60` | Enrutador HTTP con parámetros de ruta |
| hugpers423-sketch | `7400aa4` | Pantalla de acceso |
| hugpers423-sketch | `cb371c2` | Funcionalidades de contraseñas seguras |
| hugpers423-sketch | `179bb8d` | Corrección de compilación e integración |
| jfarronan9-alt | `5cf58f3` | Modelo `Enfermedad` |
| yosminureta | `1388b24` | Modelo `operacion` |

### 5.3 Verificación de la autoría

```bash
# Commits por autor
git shortlog -sn --all

# Archivos tocados por un autor concreto
git log --author="williamGUP" --name-only --pretty=format:""

# Gráfico de colaboración
git log --graph --oneline --all -30
```

---

## 6. Declaración de autoría

> **williamGUP — Líder técnico / Backend**
> Desarrollé la arquitectura del sistema, la capa de persistencia JDBC completa
> (`Db.java` y los cuatro DAO con sus operaciones CRUD), los modelos de datos,
> el motor de plantillas, la inicialización de la base con datos de ejemplo,
> las validaciones de negocio en el servidor y la configuración del servidor
> HTTP embebido. Revisé la integración del módulo de seguridad y redacté la
> matriz de trazabilidad y la documentación técnica de JDBC y pruebas.

> **RegueraMcLovin — Controladores web / Documentación**
> Implementé los controladores de paciente, consulta, enfermedad y operación,
> junto con los catálogos y los helpers de vistas. Redacté el manual de
> usuario, el diccionario de datos, los diagramas de entidad-relación y
> flujograma, y las capturas de pantalla del sistema funcionando. Configuré los
> scripts de compilación y el manifiesto del ejecutable, y redacté las guías de
> acceso por roles.

> **hugpers423-sketch — Interfaz / Seguridad / Pruebas**
> Construí el enrutador HTTP, el contexto de petición y las plantillas HTML del
> sistema. Implementé el subsistema de acceso y seguridad: los cuatro roles con
> sus permisos, el cifrado de contraseñas, el control de sesiones, el bloqueo
> por intentos fallidos, la bitácora de auditoría y la administración de
> usuarios, junto con el diseño de la pantalla de acceso y sus estilos. Escribí
> las dos suites de pruebas automatizadas, con 94 comprobaciones en total, y la
> matriz de permisos.

> **jfarronan9-alt / yosminureta — Modelos de datos**
> Contribuyeron con los modelos `Enfermedad` y `Operacion` respectivamente.

---

## 7. Métricas finales

| Métrica | Valor |
|---|---|
| Commits totales | 100 |
| Integrantes con commits | 5 |
| Periodo de desarrollo | 13 sep – 5 oct 2026 (23 días) |
| Archivos Java | 37 |
| Clases del subsistema de seguridad | 16 |
| Plantillas HTML | 16 |
| Casos de prueba automatizada | 94 |
| Requerimientos funcionales documentados | 40 |
| Requerimientos no funcionales documentados | 10 |

---

*Documento actualizado para la entrega de la Práctica de Campo 7, criterio 6
(Documentación, repositorio y demostración). Los datos de commits se obtuvieron
del historial real del repositorio.*