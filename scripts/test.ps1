$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$classes = Join-Path $root 'build/classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Filter '*.java' -Recurse -File | Select-Object -ExpandProperty FullName)
if ($sources.Count -eq 0) { throw 'No Java sources found.' }
& javac --release 15 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$tests = @(Get-ChildItem -LiteralPath (Join-Path $root 'src/test/java') -Filter '*Test.java' -Recurse -File)
foreach ($test in $tests) {
    $relative = $test.FullName.Substring((Join-Path $root 'src/test/java').Length + 1)
    $className = $relative.Replace('\', '.').Replace('/', '.').Replace('.java', '')
    & java -ea '-Dfile.encoding=UTF-8' -cp $classes $className
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
Write-Host "PASS: $($tests.Count) test classes"
