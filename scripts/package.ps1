$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$buildRoot = Join-Path $root 'build'
$classes = Join-Path $root 'build/package-classes'
$jar = Join-Path $root 'build/PromptForge.jar'
$prepareClasses = Join-Path $PSScriptRoot 'prepare-classes.ps1'
& $prepareClasses -BuildRoot $buildRoot -TargetPath $classes
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src/main/java') -Filter '*.java' -Recurse -File | Select-Object -ExpandProperty FullName)
if ($sources.Count -eq 0) { throw 'No Java sources found.' }
& javac --release 15 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$jarTool = Get-Command jar -ErrorAction SilentlyContinue
if ($jarTool) {
    $jarExe = $jarTool.Source
} else {
    $settings = & java '-XshowSettings:properties' -version 2>&1
    $javaHomeLine = $settings | Select-String 'java.home = '
    if (-not $javaHomeLine) { throw 'Cannot find jar.exe or Java home.' }
    $match = [regex]::Match($javaHomeLine[0].ToString(), 'java.home =\s*(.+)$')
    $jarExe = Join-Path $match.Groups[1].Value 'bin/jar.exe'
    if (-not (Test-Path -LiteralPath $jarExe)) { throw "jar.exe missing at $jarExe" }
}
& $jarExe --create --file $jar --main-class promptforge.Main -C $classes .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Built $jar"
