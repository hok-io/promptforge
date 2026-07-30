param([switch] $SkipPackaging)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$project = Join-Path $root 'docs/evaluations/fixtures/shop'
$resultDir = Join-Path $root 'build/usefulness-evaluation'
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null
if (-not $SkipPackaging) {
    & (Join-Path $PSScriptRoot 'package.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Packaging failed.' }
}
$jarPath = Join-Path $root 'build/PromptForge.jar'
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'JAR missing.' }
$cases = @(Get-Content -LiteralPath (Join-Path $root 'docs/evaluations/cases.json') -Raw | ConvertFrom-Json)
function Invoke-Cli($target, $mode, $text, $name) {
    $errorFile = Join-Path $resultDir "$name.stderr.txt"
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $arguments = @('-jar', $jarPath, '--project', $target)
    if ($mode -eq 'task') {
        $arguments += '--stdin'
        $previousOutputEncoding = $OutputEncoding
        try {
            $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
            $output = @($text | & java @arguments 2> $errorFile)
        } finally { $OutputEncoding = $previousOutputEncoding }
    } else {
        $arguments += "--$mode"
        if ($null -ne $text) { $arguments += $text }
        $output = @(& java @arguments 2> $errorFile)
    }
    $exitCode = $LASTEXITCODE
    $timer.Stop()
    $stdout = $output -join "`n"
    [IO.File]::WriteAllText((Join-Path $resultDir "$name.stdout.txt"), $stdout, [Text.UTF8Encoding]::new($false))
    return [pscustomobject]@{stdout=$stdout;stderr=[IO.File]::ReadAllText($errorFile);exit=$exitCode;milliseconds=$timer.ElapsedMilliseconds}
}
$validation = Invoke-Cli $project 'validate' $null 'validate'
if ($validation.exit -ne 0) { throw 'Fixture validation failed.' }
$results = @()
foreach ($case in $cases) {
    foreach ($id in $case.expectedRules) {
        if (-not (Test-Path -LiteralPath (Join-Path $project ".promptforge/rules/$id.rule"))) { throw "Unknown gold rule $id" }
    }
    foreach ($id in $case.expectedKnowledge) {
        if (-not (Test-Path -LiteralPath (Join-Path $project ".promptforge/knowledge/adr/$id.md"))) { throw "Unknown gold card $id" }
    }
    $frame = Invoke-Cli $project 'task' $case.task "$($case.id)-frame"
    $query = Invoke-Cli $project 'query' $case.task "$($case.id)-query"
    if ($frame.exit -ne 0 -or $query.exit -ne 0) { throw "CLI error in $($case.id); see raw output." }
    $selectedRules = @([regex]::Matches($frame.stdout, '\[([a-z][a-z0-9._-]*); ') | ForEach-Object { $_.Groups[1].Value })
    $selectedCards = @([regex]::Matches($query.stdout, '來源：.*?/([^/\r\n]+)\.md') | ForEach-Object { $_.Groups[1].Value })
    $missingRules = @($case.expectedRules | Where-Object { $_ -notin $selectedRules })
    $extraRules = @($selectedRules | Where-Object { $_ -notin $case.expectedRules })
    $missingCards = @($case.expectedKnowledge | Where-Object { $_ -notin $selectedCards })
    $unrequestedCards = @($selectedCards | Where-Object { $_ -notin $case.expectedKnowledge })
    $results += [pscustomobject]@{
        id=$case.id;task=$case.task;expectedRules=@($case.expectedRules);selectedRules=$selectedRules
        missingRules=$missingRules;extraRules=$extraRules;exactFrame=($missingRules.Count -eq 0 -and $extraRules.Count -eq 0)
        expectedKnowledge=@($case.expectedKnowledge);selectedKnowledge=$selectedCards;missingKnowledge=$missingCards;unrequestedKnowledge=$unrequestedCards
        frame=$frame;query=$query
    }
}
$conflict = Invoke-Cli (Join-Path $root 'examples/conflicts') 'task' '修改付款重試' 'conflict'
if ($conflict.exit -ne 6 -or -not [string]::IsNullOrEmpty($conflict.stdout)) { throw 'Conflict guard failed.' }
$conflictValidation = Invoke-Cli (Join-Path $root 'examples/conflicts') 'validate' $null 'conflict-validate'
$maintenanceProject = Join-Path $resultDir 'maintenance-project'
$maintenanceRules = Join-Path $maintenanceProject '.promptforge/rules'
New-Item -ItemType Directory -Path $maintenanceRules -Force | Out-Null
$maintenanceRule = Join-Path $maintenanceRules 'maintenance.rule'
[IO.File]::WriteAllText($maintenanceRule, "id=maintenance.check`nkeywords=修改`ninstruction=`n", [Text.UTF8Encoding]::new($false))
$badValidation = Invoke-Cli $maintenanceProject 'validate' $null 'bad-validate'
if ($badValidation.exit -ne 3) { throw 'Invalid rule should fail validation.' }
[IO.File]::WriteAllText($maintenanceRule, "id=maintenance.check`nkeywords=修改`ninstruction=版本一：確認回歸測試`n", [Text.UTF8Encoding]::new($false))
$beforeUpdate = Invoke-Cli $maintenanceProject 'task' '修改功能' 'before-rule-update'
[IO.File]::WriteAllText($maintenanceRule, "id=maintenance.check`nkeywords=修改`ninstruction=版本二：確認回歸測試及回復方案`n", [Text.UTF8Encoding]::new($false))
$afterUpdate = Invoke-Cli $maintenanceProject 'task' '修改功能' 'after-rule-update'
if ($beforeUpdate.exit -ne 0 -or $afterUpdate.exit -ne 0 -or $beforeUpdate.stdout -notmatch '版本一' -or $afterUpdate.stdout -notmatch '版本二') {
    throw 'Rule edits should take effect without rebuilding the JAR.'
}
$sourceHashes = @(Get-ChildItem (Join-Path $root 'src/main/java') -Filter '*.java' -Recurse -File | Sort-Object FullName | ForEach-Object {
    [pscustomobject]@{path=$_.FullName.Substring($root.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
})
$fixtureHashes = @(Get-ChildItem (Join-Path $project '.promptforge') -Recurse -File | Sort-Object FullName | ForEach-Object {
    [pscustomobject]@{path=$_.FullName.Substring($project.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
})
$missingCount = @($results | ForEach-Object missingRules).Count
$extraCount = @($results | ForEach-Object extraRules).Count
$expectedCount = @($results | ForEach-Object expectedRules).Count
$selectedCount = @($results | ForEach-Object selectedRules).Count
$summary = [pscustomobject]@{
    cases=$results.Count;exactFrames=@($results | Where-Object exactFrame).Count
    expectedRuleItems=$expectedCount;selectedRuleItems=$selectedCount;correctRuleItems=($expectedCount-$missingCount)
    missedRuleItems=$missingCount;extraRuleItems=$extraCount
    requiredKnowledgeItems=@($results | ForEach-Object expectedKnowledge).Count
    missingKnowledgeItems=@($results | ForEach-Object missingKnowledge).Count
    returnedKnowledgeItems=@($results | ForEach-Object selectedKnowledge).Count
    unrequestedKnowledgeItems=@($results | ForEach-Object unrequestedKnowledge).Count
}
$report = [pscustomobject]@{
    measuredAt=[DateTimeOffset]::Now.ToString('o');jarSha256=(Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
    casesSha256=(Get-FileHash -LiteralPath (Join-Path $root 'docs/evaluations/cases.json') -Algorithm SHA256).Hash
    sourceHashes=$sourceHashes;fixtureHashes=$fixtureHashes;summary=$summary;validation=$validation;conflict=$conflict
    maintenance=[pscustomobject]@{conflictValidation=$conflictValidation;badValidation=$badValidation;beforeUpdate=$beforeUpdate;afterUpdate=$afterUpdate}
    cases=$results
}
$report | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $resultDir 'results.json') -Encoding utf8
$results | Select-Object id,exactFrame,@{n='missing';e={$_.missingRules -join ','}},@{n='extra';e={$_.extraRules -join ','}},@{n='knowledgeMissing';e={$_.missingKnowledge -join ','}} | Format-Table -AutoSize
Write-Host "Results: $resultDir/results.json"
