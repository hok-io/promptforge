$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root 'build/PromptForge.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Executable JAR missing.' }
$project = Join-Path $root 'examples/payments'
$output = & java '-Dfile.encoding=UTF-8' -jar $jar --project $project --task '請修改付款重試邏輯'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (($output -join "`n") -notmatch 'payment.retry') { throw 'Packaged CLI did not select payment rule.' }
Write-Host 'PASS packaged JAR smoke test'

