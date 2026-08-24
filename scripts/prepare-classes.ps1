param(
    [Parameter(Mandatory = $true)]
    [string] $BuildRoot,
    [Parameter(Mandatory = $true)]
    [string] $TargetPath
)

$ErrorActionPreference = 'Stop'

if (-not [System.IO.Path]::IsPathRooted($BuildRoot) -or -not [System.IO.Path]::IsPathRooted($TargetPath)) {
    throw 'BuildRoot and TargetPath must be absolute paths.'
}
$build = [System.IO.Path]::GetFullPath($BuildRoot).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
$target = [System.IO.Path]::GetFullPath($TargetPath).TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
$prefix = $build + [System.IO.Path]::DirectorySeparatorChar
$comparison = if ([System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Windows)) {
    [System.StringComparison]::OrdinalIgnoreCase
} else {
    [System.StringComparison]::Ordinal
}
if ([string]::Equals($target, $build, $comparison) -or -not $target.StartsWith($prefix, $comparison)) {
    throw "TargetPath must be a descendant of BuildRoot: $target"
}

# Check every existing component before any recursive removal. This also detects
# junctions and other Windows reparse points when the filesystem exposes them.
$current = $target
while ($current) {
    $item = Get-Item -LiteralPath $current -Force -ErrorAction SilentlyContinue
    if ($item -and (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0)) {
        throw "Refusing to clean through a reparse point: $current"
    }
    $parent = [System.IO.Directory]::GetParent($current)
    if ($parent) { $current = $parent.FullName } else { $current = $null }
}

if (Test-Path -LiteralPath $target) {
    Remove-Item -LiteralPath $target -Recurse -Force
}
[System.IO.Directory]::CreateDirectory($target) | Out-Null
