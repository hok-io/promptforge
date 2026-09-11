param([ValidateRange(1024,65535)][int] $Port = 8081)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $root 'web/target/promptforge-web-1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Run ./scripts/build-web.ps1 first.' }
$javaExecutable = 'java'
$bundledJava = Join-Path $root '.tools/jdk-21.0.12.1+1/bin/java.exe'
if (Test-Path -LiteralPath $bundledJava) { $javaExecutable = $bundledJava }
Write-Host "Open http://127.0.0.1:$Port ; Ctrl+C stops the server."
& $javaExecutable -jar $jarPath "--promptforge.home=$root" "--server.port=$Port"
if ($LASTEXITCODE -ne 0) { throw 'The local server stopped with an error.' }
