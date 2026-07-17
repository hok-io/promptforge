$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root 'build/PromptForge.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Executable JAR missing.' }
$project = Join-Path $root 'examples/payments'
$output = & java -jar $jar --project $project --task '請修改付款重試邏輯'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (($output -join "`n") -notmatch 'payment.retry') { throw 'Packaged CLI did not select payment rule.' }
if (($output -join "`n") -notmatch '保留冪等性') { throw 'Packaged CLI corrupted Chinese rule text.' }
$evidence = & java -jar $jar --project $project --query '付款重試'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (($evidence -join "`n") -notmatch '0001-idempotency.md') { throw 'Packaged CLI did not find local ADR.' }
if (($evidence -join "`n") -notmatch '付款重試與冪等性') { throw 'Packaged CLI corrupted Chinese ADR text.' }
Write-Host 'PASS packaged JAR smoke test'
