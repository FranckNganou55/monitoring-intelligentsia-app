$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'local-config/application-state.json'))) { throw 'Le fichier de configuration locale est absent.' }
& mvn '-Dspring-boot.run.profiles=local-config' '-Dspring-boot.run.arguments=--server.port=8089' spring-boot:run
exit $LASTEXITCODE
