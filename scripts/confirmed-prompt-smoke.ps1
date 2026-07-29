$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$project = Join-Path $root 'examples/payments'
$result = & (Join-Path $root 'frame.ps1') '支付失敗後再試一次' -Project $project -Rules 'payment.retry' -Knowledge '.promptforge/knowledge/adr/0001-idempotency.md'
if ($LASTEXITCODE -ne 0) { throw 'Confirmed prompt failed.' }
$text = $result -join "`n"
if ($text -notmatch '保留冪等性' -or $text -notmatch '同一交易只可扣款一次' -or $text -notmatch '0001-idempotency.md') { throw 'Confirmed prompt missing rules, knowledge or source.' }
$none = & (Join-Path $root 'frame.ps1') '不改付款重試' -Project $project -Rules ''
if ($LASTEXITCODE -ne 0 -or ($none -join "`n") -ne '不改付款重試') { throw 'Empty selection was not forwarded.' }
$excluded = & (Join-Path $root 'frame.ps1') '不改付款重試' -Project $project -Exclude 'payment.retry'
if ($LASTEXITCODE -ne 0 -or ($excluded -join "`n") -ne '不改付款重試') { throw 'Exclusion was not forwarded.' }
$unicodeTask = '修正 log，别写进客戶資料 🙂'
$unicode = & (Join-Path $root 'frame.ps1') $unicodeTask -Project $project -Rules ''
if ($LASTEXITCODE -ne 0 -or ($unicode -join "`n") -ne $unicodeTask) { throw 'Unicode task was corrupted by native arguments.' }
Write-Host 'PASS confirmed prompt wrapper smoke test'
