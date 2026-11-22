# Paso 3: instala (la primera vez) y arranca SonarQube Community Build en local,
# genera un token y ejecuta el análisis con la cobertura de JaCoCo. Requiere JDK 21.
. (Join-Path $PSScriptRoot "comun.ps1")
$Version = "26.11.0.134175"
$Dir = Join-Path $env:USERPROFILE "sonarqube"
$Home_SQ = Join-Path $Dir "sonarqube-$Version"
$Url = "http://localhost:9000"
$Proyecto = "microservicio-productos"

if (-not (Test-Path $Home_SQ)) {
    New-Item -ItemType Directory -Force -Path $Dir | Out-Null
    $zip = Join-Path $Dir "sonarqube-$Version.zip"
    if (-not (Test-Path $zip)) {
        Write-Host "Descargando SonarQube Community Build $Version (~800 MB, solo la primera vez)..." -ForegroundColor Yellow
        Invoke-WebRequest "https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-$Version.zip" -OutFile $zip
    }
    Write-Host "Descomprimiendo..." -ForegroundColor Yellow
    cmd /c "tar -xf `"$zip`" -C `"$Dir`""
}

# Si el disco tiene menos de 10 % libre, Elasticsearch (motor de búsqueda de SonarQube) se niega a arrancar.
# En esta instancia local de pruebas se desactiva ese umbral.
$conf = Join-Path $Home_SQ "conf\sonar.properties"
if (-not (Select-String -Path $conf -Pattern "^sonar.search.javaAdditionalOpts=.*threshold_enabled=false" -Quiet)) {
    Add-Content $conf "`r`nsonar.search.javaAdditionalOpts=-Dcluster.routing.allocation.disk.threshold_enabled=false"
}

function Estado { try { (Invoke-RestMethod "$Url/api/system/status" -TimeoutSec 3 -ErrorAction Stop).status } catch { "DOWN" } }
if ((Estado) -ne "UP") {
    if (-not $env:JAVA_HOME) { Write-Host "SonarQube necesita JDK 21." -ForegroundColor Red; exit 1 }
    $env:SONAR_JAVA_PATH = Join-Path $env:JAVA_HOME "bin\java.exe"
    Write-Host "Arrancando SonarQube (ventana aparte; tarda 1-3 min)..." -ForegroundColor Cyan
    Start-Process -FilePath (Join-Path $Home_SQ "bin\windows-x86-64\StartSonar.bat") -WindowStyle Minimized
    for ($i = 0; $i -lt 120 -and (Estado) -ne "UP"; $i++) { Start-Sleep -Seconds 3; Write-Host -NoNewline "." }
    Write-Host ""
    if ((Estado) -ne "UP") { Write-Host "SonarQube no arrancó; revisa $Home_SQ\logs" -ForegroundColor Red; exit 1 }
}
Write-Host "SonarQube UP en $Url" -ForegroundColor Green

function Cred($u, $p) { @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${u}:${p}")) } }
$valida = $false
try { $valida = (Invoke-RestMethod "$Url/api/authentication/validate" -Headers (Cred "admin" "admin") -ErrorAction Stop).valid } catch { $valida = $false }
if ($valida) {
    Write-Host "Primera vez: elige una contraseña nueva para el usuario admin de SonarQube." -ForegroundColor Yellow
    Write-Host "(mínimo 12 caracteres, con mayúscula, minúscula, número y símbolo)" -ForegroundColor DarkGray
    $nueva = Read-Host "Nueva contraseña"
    Invoke-RestMethod -Method Post "$Url/api/users/change_password" -Headers (Cred "admin" "admin") `
        -Body @{ login = "admin"; previousPassword = "admin"; password = $nueva } | Out-Null
    $pass = $nueva
} else {
    $pass = Read-Host "Contraseña del usuario admin de SonarQube"
}
$auth = Cred "admin" $pass
$tok = (Invoke-RestMethod -Method Post "$Url/api/user_tokens/generate" -Headers $auth `
        -Body @{ name = "hu01-$(Get-Date -Format yyyyMMddHHmmss)"; type = "GLOBAL_ANALYSIS_TOKEN" }).token

Write-Host "== mvnw clean verify sonar:sonar ==" -ForegroundColor Cyan
$env:SONAR_TOKEN = $tok   # el plugin lee el token de esta variable; no queda en la línea de comandos ni en el log
$rc = Ejecutar ".\mvnw.cmd -B clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.host.url=$Url -Dsonar.qualitygate.wait=true" "logs\3-sonar.log"
"CODIGO_SALIDA=$rc" | Add-Content logs\3-sonar.log

# Resultados vía API (para el reporte)
$metricas = "bugs,vulnerabilities,security_hotspots,code_smells,coverage,line_coverage,branch_coverage,duplicated_lines_density,ncloc,reliability_rating,security_rating,sqale_rating,sqale_index"
$res = [ordered]@{
    qualityGate = Invoke-RestMethod "$Url/api/qualitygates/project_status?projectKey=$Proyecto" -Headers $auth
    medidas     = Invoke-RestMethod "$Url/api/measures/component?component=$Proyecto&metricKeys=$metricas" -Headers $auth
    issues      = Invoke-RestMethod "$Url/api/issues/search?componentKeys=$Proyecto&resolved=false&ps=100" -Headers $auth
    version     = Invoke-RestMethod "$Url/api/server/version" -Headers $auth
}
$res | ConvertTo-Json -Depth 10 | Set-Content -Encoding UTF8 logs\3-sonar-resultados.json

# Insignias oficiales de SonarQube como evidencia gráfica
try {
    $badge = (Invoke-RestMethod "$Url/api/project_badges/token?project=$Proyecto" -Headers $auth).token
    foreach ($m in "alert_status", "bugs", "vulnerabilities", "code_smells", "coverage", "duplicated_lines_density", "sqale_rating", "security_rating", "reliability_rating") {
        Invoke-WebRequest "$Url/api/project_badges/measure?project=$Proyecto&metric=$m&token=$badge" -OutFile "docs\evidencias\sonar-badge-$m.svg" -UseBasicParsing
    }
    Invoke-WebRequest "$Url/api/project_badges/quality_gate?project=$Proyecto&token=$badge" -OutFile "docs\evidencias\sonar-quality-gate.svg" -UseBasicParsing
} catch { Write-Host "No se pudieron descargar las insignias: $_" -ForegroundColor Yellow }

# Instancia local: permite ver el panel sin iniciar sesión para poder capturarlo
try { Invoke-RestMethod -Method Post "$Url/api/settings/set" -Headers $auth -Body @{ key = "sonar.forceAuthentication"; value = "false" } -ErrorAction Stop | Out-Null } catch { }
Write-Host "Capturando el panel de SonarQube..." -ForegroundColor Cyan
Capturar-Pagina "$Url/dashboard?id=$Proyecto" "sonarqube-panel.png" 1400 1000
Capturar-Pagina "$Url/project/issues?id=$Proyecto&resolved=false" "sonarqube-issues.png" 1400 900
Capturar-Pagina "$Url/component_measures?id=$Proyecto&metric=coverage&view=list" "sonarqube-cobertura.png" 1400 900
Write-Host "`nAnálisis terminado." -ForegroundColor Green
Start-Process "$Url/dashboard?id=$Proyecto"
