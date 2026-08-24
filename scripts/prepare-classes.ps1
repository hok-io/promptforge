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
$separator = [System.IO.Path]::DirectorySeparatorChar
$prefix = $build + $separator
if ([string]::Equals($target, $build, [System.StringComparison]::OrdinalIgnoreCase) -or
    -not $target.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "TargetPath must be a descendant of BuildRoot: $target"
}

# Check every existing component before any recursive removal. This also detects
# junctions and other Windows reparse points when the filesystem exposes them.
$current = $target
while ($current) {
    if (Test-Path -LiteralPath $current) {
        $item = Get-Item -LiteralPath $current -Force
        if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Refusing to clean through a reparse point: $current"
        }
    }
    $parent = [System.IO.Directory]::GetParent($current)
    if ($parent) { $current = $parent.FullName } else { $current = $null }
}

if (Test-Path -LiteralPath $target) {
    Remove-Item -LiteralPath $target -Recurse -Force
}
[System.IO.Directory]::CreateDirectory($target) | Out-Null
