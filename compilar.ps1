# Hospital HC - script de compilacion (PowerShell)
#
# Uso:
#     powershell -ExecutionPolicy Bypass -File compilar.ps1
#     (o doble clic en compilar.bat, que llama a este archivo)
#
# Que hace:
#   1. Compila las clases de src/ en build/classes
#   2. Genera hospital.jar usando manifest.mf (Class-Path incluido)
#
# Nota sobre los tres archivos heredados que se excluyen del empaquetado:
# vienen en el ZIP original con un BOM UTF-8 que javac rechaza, nunca se
# compilaron y no los usaba nadie. Su funcionalidad esta reimplementada en el
# paquete com.hospital.seguridad.

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$lib = 'lib\h2-2.2.224.jar'
$out = 'build\classes'

# Archivos heredados con BOM que no compilan (nunca estuvieron en el jar).
$excluidos = @(
    'com\hospital\auth\AuthManager.java',
    'com\hospital\db\UsuarioDao.java',
    'com\hospital\web\AuthController.java'
)

Write-Host ''
Write-Host '=== Hospital HC - compilacion ===' -ForegroundColor Cyan
Write-Host ''

if (-not (Test-Path $out)) { New-Item -ItemType Directory -Path $out -Force | Out-Null }

$fuentes = Get-ChildItem -Path 'src' -Recurse -Filter *.java |
    Where-Object { $rel = $_.FullName.Substring((Join-Path $PSScriptRoot 'src').Length + 1); $excluidos -notcontains $rel }

Write-Host ("Fuentes a compilar: {0}" -f $fuentes.Count)

$argumentos = @(
    '-encoding', 'UTF-8', '-nowarn',
    '-d', $out,
    '-cp', $lib
) + ($fuentes | ForEach-Object { $_.FullName })

& javac @argumentos
if ($LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host 'ERROR: la compilacion fallo.' -ForegroundColor Red
    exit 1
}

Write-Host 'Generando hospital.jar...' -ForegroundColor Cyan
$manifesto = Join-Path $PSScriptRoot 'manifest.mf'
$jar = Join-Path $PSScriptRoot 'hospital.jar'
Push-Location $out
& jar cfm $jar $manifesto com
$codigoJar = $LASTEXITCODE
Pop-Location

if ($codigoJar -ne 0) {
    Write-Host 'ERROR: no se pudo generar el jar.' -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host 'Listo. Para arrancar el sistema:' -ForegroundColor Green
Write-Host '    java -jar hospital.jar'
Write-Host ''