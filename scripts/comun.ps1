# Utilidades compartidas por los scripts del proyecto.
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$Raiz = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $Raiz
New-Item -ItemType Directory -Force -Path "logs", "docs\evidencias" | Out-Null

# Usa JDK 21 si está instalado (SonarQube lo exige; el proyecto compila con 17+)
$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) { $env:JAVA_HOME = $jdk.FullName; $env:Path = "$($jdk.FullName)\bin;$env:Path" }

$Edge = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"

function Ejecutar([string]$Linea, [string]$Log) {
    # Ejecuta un comando nativo vía cmd (stderr unido a stdout) mostrando la salida y guardándola en $Log
    cmd /c "$Linea 2>&1" | Tee-Object -FilePath $Log | Out-Host
    return $LASTEXITCODE
}

function Capturar-Pagina([string]$Url, [string]$Archivo, [int]$Ancho = 1400, [int]$Alto = 900) {
    # Captura una página con Edge en modo headless (sin abrir ventanas)
    $destino = Join-Path $Raiz "docs\evidencias\$Archivo"
    $perfil = Join-Path $env:TEMP "edge-hu01"
    $argumentos = @("--headless=new", "--disable-gpu", "--hide-scrollbars", "--virtual-time-budget=8000",
        "--user-data-dir=`"$perfil`"", "--window-size=$Ancho,$Alto", "--screenshot=`"$destino`"", $Url)
    Start-Process -FilePath $Edge -ArgumentList $argumentos -Wait -WindowStyle Hidden
    if (Test-Path $destino) { Write-Host "  captura: docs\evidencias\$Archivo" -ForegroundColor DarkGray }
}

function Puerto-Escuchando([int]$Puerto) {
    return [bool](Get-NetTCPConnection -LocalPort $Puerto -State Listen -ErrorAction SilentlyContinue)
}
