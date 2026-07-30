$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $root 'build/PromptForge.jar'
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Run package.ps1 first.' }
$project = Join-Path $root 'docs/evaluations/fixtures/shop'
$resultDir = Join-Path $root 'build/confirmed-prompt-evaluation'
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null
$cases = @(Get-Content (Join-Path $root 'docs/evaluations/cases.json') -Raw | ConvertFrom-Json)
$results = @()
foreach ($case in $cases) {
    $arguments = @('-jar', $jarPath, '--project', $project, '--stdin')
    if ($case.expectedRules.Count -eq 0) { $arguments += '--no-rules' }
    else { $arguments += @('--rules', ($case.expectedRules -join ',')) }
    $sources = @($case.expectedKnowledge | ForEach-Object { ".promptforge/knowledge/adr/$_.md" })
    if ($sources.Count -gt 0) { $arguments += @('--knowledge', ($sources -join ',')) }
    $errorFile = Join-Path $resultDir "$($case.id).stderr.txt"
    $previousOutputEncoding = $OutputEncoding
    try {
        $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        $text = ($case.task | & java @arguments 2> $errorFile) -join "`n"
    } finally { $OutputEncoding = $previousOutputEncoding }
    $code = $LASTEXITCODE
    if ($code -ne 0) { throw "Confirmed case $($case.id) failed" }
    $selected = @([regex]::Matches($text, '\[([a-z][a-z0-9._-]*); ') | ForEach-Object { $_.Groups[1].Value })
    if ($selected.Count -ne $case.expectedRules.Count -or @($selected | Where-Object { $_ -notin $case.expectedRules }).Count -gt 0) {
        throw "Confirmed rule selection mismatch: $($case.id)"
    }
    foreach ($source in $sources) {
        $lines = @(Get-Content (Join-Path $project $source))
        $body = ($lines[1..($lines.Count-1)] -join "`n").Trim()
        if (-not $text.Contains($source) -or -not $text.Contains($body)) { throw "Full card missing: $source" }
    }
    if (-not $text.StartsWith($case.task) -or $text.Length -gt 6000) { throw "Task or budget violated in $($case.id): $text" }
    $results += [pscustomobject]@{id=$case.id;task=$case.task;providedRules=@($case.expectedRules);providedSources=$sources;output=$text;characters=$text.Length;exit=$code}
}
[pscustomobject]@{
    measuredAt=[DateTimeOffset]::Now.ToString('o')
    selectionProvidedBy='Predefined gold selections supplied by evaluator, not inferred by the tool'
    jarSha256=(Get-FileHash $jarPath -Algorithm SHA256).Hash
    passedCases=$results.Count;cases=$results
} | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $resultDir 'results.json') -Encoding utf8
Write-Host "PASS: $($results.Count) confirmed prompt cases (oracle-assisted, not automatic accuracy)"
