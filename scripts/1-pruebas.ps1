# Paso 1: pruebas unitarias + integración, reporte JaCoCo y verificación de cobertura mínima (80 %).
. (Join-Path $PSScriptRoot "comun.ps1")
Write-Host "== mvnw clean verify ==" -ForegroundColor Cyan
$rc = Ejecutar ".\mvnw.cmd -B clean verify" "logs\1-pruebas.log"
"CODIGO_SALIDA=$rc" | Add-Content logs\1-pruebas.log
if (Test-Path target\site\jacoco\index.html) {
    Copy-Item target\site\jacoco -Destination docs\evidencias\jacoco -Recurse -Force
    $base = (Resolve-Path target\site\jacoco).Path -replace '\\', '/'
    Capturar-Pagina "file:///$base/index.html" "jacoco-resumen.png" 1400 600
    Capturar-Pagina "file:///$base/mx.edu.utch.productos.service/ProductoService.java.html" "jacoco-servicio.png" 1400 1700
    Start-Process target\site\jacoco\index.html
}
if ($rc -eq 0) { Write-Host "`nBUILD OK: pruebas y cobertura aprobadas." -ForegroundColor Green }
else { Write-Host "`nLa compilación/pruebas fallaron (código $rc). Revisa logs\1-pruebas.log" -ForegroundColor Red }
