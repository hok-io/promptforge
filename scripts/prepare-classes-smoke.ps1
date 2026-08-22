$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$buildRoot = Join-Path $root 'build'
$prepare = Join-Path $PSScriptRoot 'prepare-classes.ps1'
$testTmp = Join-Path $buildRoot 'test-tmp'
[System.IO.Directory]::CreateDirectory($testTmp) | Out-Null

function Assert-BuildDescendant([string] $Path, [string] $AllowedRoot) {
    $fullPath = [System.IO.Path]::GetFullPath($Path).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    $fullRoot = [System.IO.Path]::GetFullPath($AllowedRoot).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
    if (-not $fullPath.StartsWith($fullRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Smoke cleanup path must be below build: $fullPath"
    }
}

function Remove-SmokeEntry([string] $Path, [string] $AllowedRoot) {
    Assert-BuildDescendant $Path $AllowedRoot
    if (-not (Test-Path -LiteralPath $Path)) { return }
    $entry = Get-Item -LiteralPath $Path -Force
    if (($entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        Remove-Item -LiteralPath $Path -Force
    } else {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

foreach ($name in @('classes', 'package-classes')) {
    $target = Join-Path $buildRoot $name
    [System.IO.Directory]::CreateDirectory($target) | Out-Null
    $obsolete = Join-Path $target 'ObsoleteFromPreviousBuild.class'
    [System.IO.File]::WriteAllText($obsolete, 'stale')
    & $prepare -BuildRoot $buildRoot -TargetPath $target
    if (Test-Path -LiteralPath $obsolete) { throw "Stale class survived clean for $name" }
}

$outside = Join-Path $testTmp ('junction-target-' + [Guid]::NewGuid().ToString('N'))
$link = Join-Path $buildRoot 'classes-junction-smoke'
Assert-BuildDescendant $link $buildRoot
Remove-SmokeEntry $link $buildRoot
[System.IO.Directory]::CreateDirectory($outside) | Out-Null
$sentinel = Join-Path $outside 'keep.txt'
[System.IO.File]::WriteAllText($sentinel, 'must remain')
try {
    if ([System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Windows)) {
        New-Item -ItemType Junction -Path $link -Target $outside | Out-Null
    } else {
        New-Item -ItemType SymbolicLink -Path $link -Target $outside | Out-Null
    }
    try {
        & $prepare -BuildRoot $buildRoot -TargetPath $link
        throw 'Cleaner accepted a reparse-point target.'
    } catch {
        if ($_.Exception.Message -notmatch 'reparse point') { throw }
    }
    if (-not (Test-Path -LiteralPath $sentinel)) { throw 'Cleaner changed the outside reparse-point target.' }
    if (-not (Test-Path -LiteralPath $link)) { throw 'Cleaner removed the link instead of rejecting it.' }
} finally {
    Remove-SmokeEntry $link $buildRoot
    Remove-SmokeEntry $outside $buildRoot
}
Write-Host 'PASS build class-clean safety smoke'
