param(
    [Parameter(Position = 0, Mandatory = $true)]
    [string] $Task,
    [string] $Project = (Get-Location).Path
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$classes = Join-Path $root 'build/classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src/main/java') -Filter '*.java' -Recurse -File | Select-Object -ExpandProperty FullName)
& javac --release 15 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java '-Dfile.encoding=UTF-8' -cp $classes promptforge.Main --project $Project --task $Task
exit $LASTEXITCODE

