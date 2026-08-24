param(
    [Parameter(Position = 0, Mandatory = $true)]
    [string] $Query,
    [string] $Project = (Get-Location).Path
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$buildRoot = Join-Path $root 'build'
$classes = Join-Path $root 'build/classes'
$prepareClasses = Join-Path $root 'scripts/prepare-classes.ps1'
& $prepareClasses -BuildRoot $buildRoot -TargetPath $classes
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src/main/java') -Filter '*.java' -Recurse -File | Select-Object -ExpandProperty FullName)
& javac --release 15 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java '-Dfile.encoding=UTF-8' -cp $classes promptforge.Main --project $Project --query $Query
exit $LASTEXITCODE
