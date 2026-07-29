param(
    [Parameter(Position = 0, Mandatory = $true)]
    [string] $Task,
    [string] $Project = (Get-Location).Path,
    [AllowEmptyString()]
    [string] $Rules,
    [AllowEmptyString()]
    [string] $Exclude,
    [AllowEmptyString()]
    [string] $Knowledge
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$classes = Join-Path $root 'build/classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src/main/java') -Filter '*.java' -Recurse -File | Select-Object -ExpandProperty FullName)
& javac --release 15 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$arguments = @('-Dfile.encoding=UTF-8', '-cp', $classes, 'promptforge.Main', '--project', $Project, '--task', $Task)
if ($PSBoundParameters.ContainsKey('Rules')) {
    if ($Rules.Length -eq 0) { $arguments += '--no-rules' }
    else { $arguments += @('--rules', $Rules) }
}
if ($PSBoundParameters.ContainsKey('Exclude') -and $Exclude.Length -gt 0) { $arguments += @('--exclude', $Exclude) }
if ($PSBoundParameters.ContainsKey('Knowledge') -and $Knowledge.Length -gt 0) { $arguments += @('--knowledge', $Knowledge) }
& java @arguments
exit $LASTEXITCODE
