# Paso 2: levanta el microservicio contra MongoDB local (27017), ejecuta la colección
# de Postman con Newman y guarda capturas de Swagger UI y del reporte de Newman.
. (Join-Path $PSScriptRoot "comun.ps1")

if (-not (Puerto-Escuchando 27017)) {
    Write-Host "MongoDB no está escuchando en 27017; intentando iniciar el servicio 'MongoDB'..." -ForegroundColor Yellow
    try { Start-Service MongoDB } catch { Write-Host "No se pudo iniciar (¿requiere administrador?). Inicia MongoDB y vuelve a ejecutar." -ForegroundColor Red; exit 1 }
    Start-Sleep -Seconds 3
}
if (Puerto-Escuchando 8080) { Write-Host "El puerto 8080 ya está ocupado; cierra la otra instancia." -ForegroundColor Red; exit 1 }

Write-Host "== Iniciando microservicio (mvnw spring-boot:run) ==" -ForegroundColor Cyan
$api = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", ".\mvnw.cmd -B spring-boot:run > logs\2-api.log 2>&1" -PassThru -WindowStyle Minimized

$listo = $false
for ($i = 0; $i -lt 90 -and -not $listo; $i++) {
    Start-Sleep -Seconds 2
    try { Invoke-WebRequest "http://localhost:8080/v3/api-docs" -UseBasicParsing -TimeoutSec 2 | Out-Null; $listo = $true } catch { Write-Host -NoNewline "." }
}
if (-not $listo) { Write-Host "`nLa API no arrancó; revisa logs\2-api.log" -ForegroundColor Red; exit 1 }
Write-Host "`nAPI arriba en http://localhost:8080" -ForegroundColor Green

# Colección completa con Newman (cliente CLI oficial de Postman)
Write-Host "== Ejecutando colección Postman con Newman ==" -ForegroundColor Cyan
Ejecutar ("npx --yes -p newman -p newman-reporter-htmlextra newman run postman\HU-01-Productos.postman_collection.json " +
    "-r cli,htmlextra,json --reporter-htmlextra-export docs\evidencias\newman-reporte.html " +
    "--reporter-json-export logs\2-newman.json --color off") "logs\2-newman.log" | Out-Null

# Respuestas crudas de cada endpoint con curl.exe (encabezados + cuerpo), para el reporte técnico
$base = "http://localhost:8080/api/v1/productos"
New-Item -ItemType Directory -Force -Path logs\http | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
$n = 0
function Llamar($nombre, $metodo, $url, $cuerpo) {
    $script:n++
    $pref = "logs\http\{0:D2}-{1}" -f $script:n, $nombre
    $extra = ""
    if ($cuerpo) {
        [IO.File]::WriteAllText((Join-Path $Raiz "$pref.req.json"), $cuerpo, $utf8)
        $extra = "-H `"Content-Type: application/json`" --data-binary `"@$pref.req.json`""
    }
    $code = cmd /c "curl.exe -s -X $metodo $extra -D `"$pref.headers.txt`" -o `"$pref.body.json`" -w %{http_code} `"$url`""
    "$metodo $url" | Set-Content "$pref.cmd.txt"
    Write-Host ("  {0,-20} {1,-6} -> {2}" -f $nombre, $metodo, $code)
    if (Test-Path "$pref.body.json") { return [IO.File]::ReadAllText((Join-Path $Raiz "$pref.body.json"), $utf8) }
}
$prod = '{"codigo":"HER-0100","nombre":"Taladro inalámbrico","descripcion":"Taladro 20V con dos baterías","categoria":"Herramientas","precio":1899.00,"stock":12}'
$creado = Llamar "crear" POST $base $prod | ConvertFrom-Json
$id = $creado.id
Llamar "crear-segundo" POST $base '{"codigo":"HER-0101","nombre":"Llave de impacto","categoria":"Herramientas","precio":2450.50,"stock":4}' | Out-Null
Llamar "listar" GET $base | Out-Null
Llamar "listar-categoria" GET "${base}?categoria=herramientas" | Out-Null
Llamar "obtener" GET "$base/$id" | Out-Null
Llamar "actualizar" PUT "$base/$id" '{"codigo":"HER-0100","nombre":"Taladro inalámbrico Pro","descripcion":"Taladro 20V brushless","categoria":"Herramientas","precio":2199.00,"stock":9,"activo":true}' | Out-Null
Llamar "error-400" POST $base '{"codigo":"abc","nombre":"","categoria":"","precio":-5,"stock":-1}' | Out-Null
Llamar "error-400-json" POST $base '{"codigo": "HER-0102", ' | Out-Null
Llamar "error-409" POST $base $prod | Out-Null
Llamar "error-404" GET "$base/000000000000000000000000" | Out-Null
Llamar "error-405" PATCH "$base/$id" $null | Out-Null
Llamar "eliminar" DELETE "$base/$id" | Out-Null
Llamar "obtener-eliminado" GET "$base/$id" | Out-Null

# Capturas
Capturar-Pagina "http://localhost:8080/swagger-ui/index.html" "swagger-ui.png" 1400 1100
Capturar-Pagina "file:///$((Resolve-Path docs\evidencias\newman-reporte.html).Path -replace '\\','/')" "newman-reporte.png" 1400 2200

Write-Host "`nListo. La API sigue corriendo (ventana minimizada). Abre http://localhost:8080/swagger-ui.html o usa Postman." -ForegroundColor Green
Start-Process "http://localhost:8080/swagger-ui.html"
