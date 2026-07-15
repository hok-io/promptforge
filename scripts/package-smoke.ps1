$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root 'build/PromptForge.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Executable JAR missing.' }
$project = Join-Path $root 'examples/payments'
$output = & java '-Dfile.encoding=UTF-8' -jar $jar --project $project --task '請修改付款重試邏輯'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (($output -join "`n") -notmatch 'payment.retry') { throw 'Packaged CLI did not select payment rule.' }
$evidence = & java '-Dfile.encoding=UTF-8' -jar $jar --project $project --query '付款重試'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (($evidence -join "`n") -notmatch '0001-idempotency.md') { throw 'Packaged CLI did not find local ADR.' }
Write-Host 'PASS packaged JAR smoke test'
