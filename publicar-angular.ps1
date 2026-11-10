$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$frontendRoot = Join-Path $projectRoot 'frontend'
$buildRoot = Join-Path $frontendRoot 'dist\frontend\browser'
$staticRoot = Join-Path $projectRoot 'src\main\resources\static'
$portalRoot = Join-Path $staticRoot 'portal'

Push-Location $frontendRoot
try {
    if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules'))) {
        npm ci --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar las dependencias de Angular.' }
    }
    npm run build:portal
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de Angular.' }
} finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath (Join-Path $buildRoot 'index.html'))) {
    throw 'No se encontró el index.html compilado.'
}
New-Item -ItemType Directory -Path $portalRoot -Force | Out-Null
Copy-Item -Path (Join-Path $buildRoot '*') -Destination $portalRoot -Recurse -Force
Write-Host "Portal Angular publicado en $portalRoot"
