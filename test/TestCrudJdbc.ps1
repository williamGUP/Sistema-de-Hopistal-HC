param(
    [string]$Base = "http://localhost:8080"
)

# ============================================================================
#  Hospital HC - Pruebas funcionales de JDBC y operaciones CRUD
#  Practica de Campo 7 - Integracion y validacion del proyecto
#
#  Uso (con el servidor ya arrancado y una base de datos limpia):
#      1. Deten el servidor y borra la carpeta data\
#      2. java -jar hospital.jar 8080
#      3. powershell -ExecutionPolicy Bypass -File test\TestCrudJdbc.ps1
#
#  Cubre las cuatro operaciones CRUD sobre las tablas del proyecto y, sobre
#  todo, los casos con datos NO VALIDOS, que es lo que exige el enunciado.
#
#  Cada caso registra: CODIGO, operacion CRUD, dato, resultado esperado y
#  resultado obtenido. Al terminar se muestra la tabla de evidencias.
# ============================================================================

$ErrorActionPreference = 'Continue'
$script:pass = 0
$script:fail = 0
$script:evidencia = New-Object System.Collections.ArrayList

# --- Cliente HTTP: no lanza excepcion ante 4xx/5xx y no sigue redirecciones ---

function Invoke-Http {
    param(
        [string]$Metodo = 'GET',
        [string]$Ruta = '/',
        $Cookies = $null,
        [hashtable]$Form = $null,
        [string]$Origen = ''
    )

    $peticion = [System.Net.HttpWebRequest]::Create("$Base$Ruta")
    $peticion.Method = $Metodo
    $peticion.AllowAutoRedirect = $false
    $peticion.UserAgent = 'HospitalHC-PC7/1.0'
    $peticion.Timeout = 15000
    $peticion.KeepAlive = $false

    if ($null -ne $Cookies) { $peticion.CookieContainer = $Cookies }
    if ($Origen -ne '') { $peticion.Headers.Add('Origin', $Origen) }

    if ($null -ne $Form) {
        $pares = @()
        foreach ($clave in $Form.Keys) {
            $pares += ([uri]::EscapeDataString([string]$clave) + '=' +
                       [uri]::EscapeDataString([string]$Form[$clave]))
        }
        $cuerpo = [System.Text.Encoding]::UTF8.GetBytes(($pares -join '&'))
        $peticion.ContentType = 'application/x-www-form-urlencoded; charset=UTF-8'
        $peticion.ContentLength = $cuerpo.Length
        $flujo = $peticion.GetRequestStream()
        $flujo.Write($cuerpo, 0, $cuerpo.Length)
        $flujo.Close()
    }

    $respuesta = $null; $estado = 0; $cuerpoTexto = ''; $ubicacion = ''
    try { $respuesta = $peticion.GetResponse() }
    catch [System.Net.WebException] { $respuesta = $_.Exception.Response }

    if ($null -ne $respuesta) {
        $estado = [int]$respuesta.StatusCode
        $ubicacion = $respuesta.Headers['Location']
        $lector = New-Object System.IO.StreamReader($respuesta.GetResponseStream())
        $cuerpoTexto = $lector.ReadToEnd()
        $lector.Close(); $respuesta.Close()
    }

    return [pscustomobject]@{ Codigo = $estado; Location = $ubicacion; Html = $cuerpoTexto }
}

function New-Cookies { return (New-Object System.Net.CookieContainer) }

function Get-Pagina($ruta, $c) { return Invoke-Http -Metodo GET -Ruta $ruta -Cookies $c }
function Post-Form($ruta, $form, $c) { return Invoke-Http -Metodo POST -Ruta $ruta -Form $form -Cookies $c -Origen $Base }

# --- Registro de evidencia -------------------------------------------------

$script:caso = 0

function Verificar {
    param([string]$Codigo, [string]$Operacion, [string]$Dato, [string]$Esperado, [bool]$Cumplido, [string]$Detalle = '')
    $script:caso++
    if ($Cumplido) {
        $script:pass++
        Write-Host ("  [{0,-2}] [OK]    {1,-6} {2}" -f $script:caso, $Codigo, $Detalle) -ForegroundColor Green
    } else {
        $script:fail++
        Write-Host ("  [{0,-2}] [FALLA] {1,-6} {2}" -f $script:caso, $Codigo, $Detalle) -ForegroundColor Red
    }
    [void]$script:evidencia.Add([pscustomobject]@{
        Caso = "CP7-$('{0:D2}' -f $script:caso)"
        Codigo = $Codigo
        Operacion = $Operacion
        Dato = $Dato
        Esperado = $Esperado
        Obtenido = $(if ($Cumplido) { 'Coincide' } else { 'No coincide' })
    })
}

# Extrae el mensaje de error visible en la pagina (banner de validacion).
# El aviso de contrasena de fabrica tambien usa flash-error, pero su contenido
# empieza por una etiqueta <strong>; este patron solo captura los mensajes de
# texto plano que genera el servidor al validar los datos.
function Mensaje($html) {
    if (-not $html) { return '' }
    $m = [regex]::Match($html, 'class="flash flash-error">\s*([^<]{3,300}?)\s*</div>')
    if ($m.Success) {
        $txt = ($m.Groups[1].Value -replace '\s+', ' ').Trim()
        if ($txt) { return $txt }
    }
    # Alerta del formulario de acceso: <div class="acc-alert acc-alert-error">
    $m = [regex]::Match($html, 'acc-alert-error[^>]*>[\s\S]{0,200}?<span[^>]*>\s*</span>\s*<span>\s*([^<]{3,300}?)\s*<')
    if ($m.Success) {
        $txt = ($m.Groups[1].Value -replace '\s+', ' ').Trim()
        if ($txt) { return $txt }
    }
    return ''
}

$sufijo = Get-Random -Minimum 100000 -Maximum 999999
Write-Host ''
Write-Host '=== 0. Preparacion: inicio de sesion ===' -ForegroundColor Cyan
$c = New-Cookies
$r = Post-Form '/login' @{ username = 'admin'; password = 'Admin2026' } $c
if ($r.Codigo -ne 302) {
    Write-Host '  ERROR: no se pudo iniciar sesion como admin. Verifica que el servidor este arriba.' -ForegroundColor Red
    exit 1
}
Write-Host '  Sesion de administrador iniciada.' -ForegroundColor Green

$dniValido = "90$sufijo"
$dniAlt = "80$sufijo"

Write-Host ''
Write-Host '=== 1. CREATE - alta de paciente (dato valido) ===' -ForegroundColor Cyan
$r = Post-Form '/pacientes/nuevo' @{
    dni = $dniValido; nombres = 'Carlos'; apellidos = 'Prueba JDBC'
    sexo = 'MASCULINO'; telefono = '999888777'; direccion = 'Av. Siempre Viva 742'
    email = 'carlos.prueba@example.com'; grupoSanguineo = 'O+'
    alergias = 'Penicilina'; fechaNacimiento = '1990-05-14'
} $c
$idPaciente = 0
if ($r.Location -match '/pacientes/(\d+)') { $idPaciente = [int]$Matches[1] }
Verificar 'CRUD-01' 'CREATE paciente' "DNI $dniValido valido" 'HTTP 302 y alta registrada' `
    ($r.Codigo -eq 302 -and $idPaciente -gt 0) "Alta de paciente (id=$idPaciente)"

Write-Host ''
Write-Host '=== 2. READ - consulta y busqueda ===' -ForegroundColor Cyan
$r = Get-Pagina "/pacientes?q=$dniValido" $c
$encontrado = $r.Html -match [regex]::Escape('Carlos') -and $r.Html -match [regex]::Escape($dniValido)
Verificar 'CRUD-02' 'READ paciente' "buscar DNI $dniValido" 'El paciente aparece en el listado' `
    ($r.Codigo -eq 200 -and $encontrado) 'Lectura por busqueda con el dato creado'

$r = Get-Pagina "/pacientes/$idPaciente" $c
$historia = $r.Html -match 'Carlos' -and $r.Html -match 'Prueba JDBC'
Verificar 'CRUD-03' 'READ paciente' "detalle id=$idPaciente" 'La historia clinica se muestra' `
    ($r.Codigo -eq 200 -and $historia) 'Lectura del detalle del paciente'

Write-Host ''
Write-Host '=== 3. UPDATE - edicion de paciente ===' -ForegroundColor Cyan
$r = Post-Form "/pacientes/$idPaciente/editar" @{
    dni = $dniValido; nombres = 'Carlos Andres'; apellidos = 'Prueba JDBC Actualizado'
    sexo = 'MASCULINO'; telefono = '900000000'; direccion = 'Av. Nueva 100'
    email = 'carlos.nuevo@example.com'; grupoSanguineo = 'A+'
    alergias = 'Ninguna'; fechaNacimiento = '1990-05-14'
} $c
Verificar 'CRUD-04' 'UPDATE paciente' "editar id=$idPaciente" 'HTTP 302 y cambios guardados' `
    ($r.Codigo -eq 302) 'Actualizacion de los datos generales'

$r = Get-Pagina "/pacientes/$idPaciente" $c
$actualizado = $r.Html -match 'Carlos Andres' -and $r.Html -match 'Prueba JDBC Actualizado'
Verificar 'CRUD-05' 'UPDATE paciente' "verificar id=$idPaciente" 'Los datos nuevos aparecen' `
    ($actualizado) 'Lectura posterior a la actualizacion'

Write-Host ''
Write-Host '=== 4. CREATE de tablas relacionadas (datos validos) ===' -ForegroundColor Cyan
$r = Post-Form "/pacientes/$idPaciente/consultas/nueva" @{
    motivo = 'Dolor abdominal'; sintomas = 'Dolor en fosa iliaca derecha'
    diagnostico = 'Sospecha de apendicitis'; tratamiento = 'Analgesia y observacion'
    medico = 'Dr. Carlos Mendoza'; fecha = '2026-09-01 09:30:00'; observaciones = 'Se deriva a cirugia'
} $c
Verificar 'CRUD-06' 'CREATE consulta' 'motivo obligatorio' 'HTTP 302 y consulta registrada' `
    ($r.Codigo -eq 302) 'Alta de consulta medica'

$r = Post-Form "/pacientes/$idPaciente/enfermedades/nueva" @{
    nombre = 'Apendicitis aguda'; fechaDiagnostico = '2026-09-01'
    estado = 'ACTIVA'; observaciones = 'En tratamiento'
} $c
Verificar 'CRUD-07' 'CREATE enfermedad' 'estado ACTIVA' 'HTTP 302 y enfermedad registrada' `
    ($r.Codigo -eq 302) 'Alta de enfermedad'

$r = Post-Form "/pacientes/$idPaciente/operaciones/nueva" @{
    tipoOperacion = 'Apendicectomia laparoscopica'; fecha = '2026-09-03'
    cirujano = 'Dr. Roberto Salazar'; resultado = 'EXITOSA'; observaciones = 'Sin complicaciones'
} $c
Verificar 'CRUD-08' 'CREATE operacion' 'resultado EXITOSA' 'HTTP 302 y operacion registrada' `
    ($r.Codigo -eq 302) 'Alta de operacion quirurgica'

$r = Get-Pagina "/pacientes/$idPaciente" $c
$clinico = $r.Html -match 'Apendicectomia' -and $r.Html -match 'Apendicitis aguda'
Verificar 'CRUD-09' 'READ relacionada' "historia id=$idPaciente" 'Consulta, enfermedad y operacion listadas' `
    ($clinico) 'Las tres tablas relacionadas se muestran en la historia clinica'

Write-Host ''
Write-Host '=== 5. DELETE - baja logica de paciente ===' -ForegroundColor Cyan
$r = Post-Form "/pacientes/$idPaciente/eliminar" @{} $c
Verificar 'CRUD-10' 'DELETE paciente' "baja id=$idPaciente" 'HTTP 302 y baja registrada' `
    ($r.Codigo -eq 302) 'Baja logica (activo = FALSE, no borra la informacion)'

$r = Get-Pagina "/pacientes?q=$dniValido" $c
# El campo de busqueda siempre muestra el termino escrito, por eso se comprueba
# el estado vacio del listado y no la simple ausencia del DNI en la pagina.
$listadoVacio = $r.Html -match 'No hay pacientes que coincidan'
Verificar 'CRUD-11' 'DELETE paciente' "verificar id=$idPaciente" 'El listado activo ya no lo muestra' `
    ($listadoVacio) 'La baja logica se refleja en las consultas (activo = FALSE)'

Write-Host ''
Write-Host '=== 6. Validacion de datos NO VALIDOS ===' -ForegroundColor Cyan

# 6.1 DNI con formato incorrecto
$r = Post-Form '/pacientes/nuevo' @{ dni = '123'; nombres = 'Formato'; apellidos = 'Invalido'; sexo = 'MASCULINO' } $c
$m = Mensaje $r.Html
Verificar 'VAL-01' 'CREATE paciente' 'DNI = "123" (3 digitos)' 'Rechazado con mensaje de validacion' `
    ($r.Codigo -eq 200 -and $m -match '8') "Rechazado: $m"

# 6.2 Nombres y apellidos vacios
$r = Post-Form '/pacientes/nuevo' @{ dni = "70$sufijo"; nombres = ''; apellidos = '' } $c
$m = Mensaje $r.Html
Verificar 'VAL-02' 'CREATE paciente' 'nombres y apellidos vacios' 'Rechazado por campos obligatorios' `
    ($r.Codigo -eq 200 -and $m -match 'nombres') "Rechazado: $m"

# 6.3 DNI duplicado
$r = Post-Form '/pacientes/nuevo' @{ dni = '45678912'; nombres = 'Duplicado'; apellidos = 'De DNI' } $c
$m = Mensaje $r.Html
Verificar 'VAL-03' 'CREATE paciente' 'DNI 45678912 ya existe' 'Rechazado por clave duplicada' `
    ($r.Codigo -eq 200 -and $m -match 'existe') "Rechazado: $m"

# 6.4 Fecha de nacimiento futura
$futuro = (Get-Date).AddYears(3).ToString('yyyy-MM-dd')
$r = Post-Form '/pacientes/nuevo' @{ dni = "60$sufijo"; nombres = 'Fecha'; apellidos = 'Futura'; fechaNacimiento = $futuro } $c
$m = Mensaje $r.Html
Verificar 'VAL-04' 'CREATE paciente' "fechaNacimiento = $futuro" 'Rechazada por fecha futura' `
    ($r.Codigo -eq 200 -and $m -match 'futura') "Rechazado: $m"

# 6.5 Sexo fuera del catalogo
$r = Post-Form '/pacientes/nuevo' @{ dni = "50$sufijo"; nombres = 'Sexo'; apellidos = 'Invalido'; sexo = 'X' } $c
$m = Mensaje $r.Html
Verificar 'VAL-05' 'CREATE paciente' 'sexo = "X"' 'Rechazado por catalogo invalido' `
    ($r.Codigo -eq 200 -and $m -match 'sexo') "Rechazado: $m"

# 6.6 Grupo sanguíneo fuera del catalogo
$r = Post-Form '/pacientes/nuevo' @{ dni = "40$sufijo"; nombres = 'Grupo'; apellidos = 'Invalido'; grupoSanguineo = 'Z+' } $c
$m = Mensaje $r.Html
Verificar 'VAL-06' 'CREATE paciente' 'grupoSanguineo = "Z+"' 'Rechazado por catalogo invalido' `
    ($r.Codigo -eq 200 -and $m -match 'sangu') "Rechazado: $m"

# 6.7 Operacion sin tipo (campo obligatorio)
$r = Post-Form "/pacientes/$idPaciente/operaciones/nueva" @{ tipoOperacion = ''; fecha = '2026-09-10'; resultado = 'EXITOSA' } $c
$m = Mensaje $r.Html
Verificar 'VAL-07' 'CREATE operacion' 'tipoOperacion vacio' 'Rechazado por campo obligatorio' `
    ($r.Codigo -ne 302 -and $m -ne '') "Rechazado: $m"

# 6.8 Estado de enfermedad fuera del catalogo
$r = Post-Form "/pacientes/$idPaciente/enfermedades/nueva" @{ nombre = 'Prueba estado'; estado = 'INVENTADA' } $c
$m = Mensaje $r.Html
Verificar 'VAL-08' 'CREATE enfermedad' 'estado = "INVENTADA"' 'Rechazado por catalogo invalido' `
    ($r.Codigo -ne 302) "Rechazado: $m"

# 6.9 Fecha de operacion con formato incorrecto
$r = Post-Form "/pacientes/$idPaciente/operaciones/nueva" @{ tipoOperacion = 'Prueba fecha'; fecha = 'no-es-fecha'; resultado = 'EXITOSA' } $c
$m = Mensaje $r.Html
Verificar 'VAL-09' 'CREATE operacion' 'fecha = "no-es-fecha"' 'Rechazado por formato de fecha' `
    ($r.Codigo -ne 302) "Rechazado: $m"

# 6.10 Paciente inexistente
$r = Post-Form '/pacientes/999999/editar' @{ dni = '11111111'; nombres = 'Fantasma'; apellidos = 'Inexistente' } $c
Verificar 'VAL-10' 'UPDATE paciente' 'id = 999999 (inexistente)' 'Redirige con aviso de no encontrado' `
    ($r.Codigo -eq 302 -and $r.Location -match 'error') 'Manejo de registro inexistente'

# 6.11 Ruta inexistente
$r = Get-Pagina '/ruta/que/no/existe' $c
Verificar 'VAL-11' 'Navegacion' 'ruta inexistente' 'Pagina 404 controlada' `
    ($r.Codigo -eq 404) 'La aplicacion no cae ante rutas desconocidas'

Write-Host ''
Write-Host '=== 7. Control de errores de conexion JDBC ===' -ForegroundColor Cyan
# Se comprueba que la base existe y que la aplicacion responde cuando el motor
# esta disponible; el aislamiento de la BD se documenta en el informe.
$r = Get-Pagina '/pacientes' $c
$dbViva = $r.Codigo -eq 200
Verificar 'ERR-01' 'Persistencia' 'consulta general sin sesion' 'Redirige al login (BD operativa)' `
    ((Invoke-Http -Metodo GET -Ruta '/pacientes' -Cookies (New-Cookies)).Codigo -eq 302 -and $dbViva) `
    'La conexion JDBC responde; sin sesion no se exponen datos'

Write-Host ''
Write-Host '===================== EVIDENCIAS =====================' -ForegroundColor Cyan
$script:evidencia | Format-Table -AutoSize | Out-String -Width 190

Write-Host ''
Write-Host '===================== RESULTADO =====================' -ForegroundColor Cyan
Write-Host ("  Casos ejecutados:  {0}" -f $script:caso)
Write-Host ("  Casos correctos:   {0}" -f $script:pass)
if ($fail -eq 0) {
    Write-Host '  Casos fallidos:    0' -ForegroundColor Green
    Write-Host ''
    Write-Host '  CRUD Y VALIDACION: TODO CORRECTO' -ForegroundColor Green
} else {
    Write-Host ("  Casos fallidos:    {0}" -f $fail) -ForegroundColor Red
}
Write-Host ''
exit $(if ($fail -eq 0) { 0 } else { 1 })