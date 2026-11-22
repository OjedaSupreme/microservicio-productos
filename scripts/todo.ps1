# Ejecuta en orden los pasos 2, 3 y 4 (API + evidencias, SonarQube, Git/GitHub).
$aqui = $PSScriptRoot
foreach ($paso in "2-api-y-evidencias.ps1", "3-sonarqube.ps1", "4-git-github.ps1") {
    Write-Host "`n==================== $paso ====================" -ForegroundColor Magenta
    powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $aqui $paso)
}
Write-Host "`nTODO TERMINADO. Avísale a Claude." -ForegroundColor Green
