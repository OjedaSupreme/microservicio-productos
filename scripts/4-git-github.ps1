# Paso 4: crea el repositorio Git con commits estructurados (Conventional Commits)
# y, si confirmas, lo publica en GitHub con GitHub CLI (gh).
. (Join-Path $PSScriptRoot "comun.ps1")
$ErrorActionPreference = "Continue"

# Limpieza de archivos temporales que dejó la preparación del proyecto
Remove-Item scripts\analizar-sonar.ps1 -ErrorAction SilentlyContinue
Remove-Item ..\microservicio-productos.tgz -ErrorAction SilentlyContinue

if (Test-Path .git) { Write-Host "Ya existe un repositorio Git aquí; no se rehacen los commits." -ForegroundColor Yellow }
else {
    git init -b main | Out-Null
    $commits = @(
        @{ m = "chore: estructura inicial Spring Boot 3.4 con Maven Wrapper y configuración de MongoDB";
           f = @("pom.xml", "mvnw", "mvnw.cmd", ".mvn", ".gitignore", "src/main/java/mx/edu/utch/productos/ProductosApplication.java", "src/main/resources/application.yml", "src/main/java/mx/edu/utch/productos/config") },
        @{ m = "feat(modelo): entidad Producto, DTOs con validaciones y repositorio MongoDB";
           f = @("src/main/java/mx/edu/utch/productos/model", "src/main/java/mx/edu/utch/productos/dto", "src/main/java/mx/edu/utch/productos/repository") },
        @{ m = "feat(errores): excepciones de dominio y manejador global con respuestas JSON";
           f = @("src/main/java/mx/edu/utch/productos/exception") },
        @{ m = "feat(servicio): lógica de negocio del CRUD con control de duplicados";
           f = @("src/main/java/mx/edu/utch/productos/service") },
        @{ m = "feat(api): controlador REST /api/v1/productos documentado con OpenAPI";
           f = @("src/main/java/mx/edu/utch/productos/controller") },
        @{ m = "test: pruebas unitarias del servicio con JUnit 5 y Mockito";
           f = @("src/test/java/mx/edu/utch/productos/service") },
        @{ m = "test: pruebas de capa web (MockMvc) e integración con MongoDB embebido";
           f = @("src/test/java/mx/edu/utch/productos/controller", "src/test/java/mx/edu/utch/productos/integration", "src/test/resources") },
        @{ m = "build: automatización de pruebas, cobertura JaCoCo y análisis SonarQube";
           f = @("scripts", "1-probar-y-cobertura.bat", "2-levantar-api-y-evidencias.bat", "3-analisis-sonarqube.bat", "docker-compose.yml") },
        @{ m = "docs: README, colección de Postman y evidencias de calidad";
           f = @("README.md", "postman", "docs") }
    )
    foreach ($c in $commits) {
        foreach ($f in $c.f) { if (Test-Path $f) { git add -- $f } }
        git commit -q -m $c.m
        Write-Host "  commit: $($c.m)" -ForegroundColor DarkGray
    }
    git add -A; git commit -q -m "chore: archivos restantes del proyecto" 2>$null
    git log --oneline | Tee-Object -FilePath logs\4-git-log.txt
}

if (Get-Command gh -ErrorAction SilentlyContinue) {
    $r = Read-Host "¿Crear el repositorio PÚBLICO 'microservicio-productos' en tu GitHub y subir el código? (s/n)"
    if ($r -match '^[sS]') {
        cmd /c "gh repo create microservicio-productos --public --source . --remote origin --push --description `"HU-01 Microservicio CRUD con MongoDB, JaCoCo y SonarQube`" 2>&1" | Tee-Object -FilePath logs\4-github.txt
        cmd /c "gh repo view --json url -q .url 2>&1" | Tee-Object -FilePath logs\4-github-url.txt
    }
} else {
    Write-Host "GitHub CLI (gh) no está disponible; sube el repo manualmente (git remote add origin ... ; git push -u origin main)." -ForegroundColor Yellow
}
