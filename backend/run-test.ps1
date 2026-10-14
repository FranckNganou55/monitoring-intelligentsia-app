$ErrorActionPreference = 'Stop'

$jarPath = Join-Path $PSScriptRoot 'target\monitoring-backend-1.0.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "Backend JAR introuvable : $jarPath. Construisez d'abord le backend."
}

# Keep the local test API separate from any existing service on port 8089.
& java -jar $jarPath `
    --server.port=8091 `
    --spring.datasource.url=jdbc:postgresql://localhost:5432/monitoring_test

exit $LASTEXITCODE
