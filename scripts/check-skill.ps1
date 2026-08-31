$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$skill = Join-Path $root 'skills/prompt-frame/SKILL.md'
if (-not (Test-Path -LiteralPath $skill)) { throw 'Skill entrypoint missing.' }
$content = Get-Content -LiteralPath $skill -Raw -Encoding UTF8
if ($content -notmatch '(?s)^---\s*\nname: prompt-frame\s*\ndescription: .+?\n---') { throw 'Skill frontmatter is invalid.' }
if ($content -notmatch 'PROMPTFORGE_HOME' -or $content -notmatch 'frame\.ps1') { throw 'Skill needs a configurable local CLI location.' }
if ($content -match '[A-Z]:\\') { throw 'Skill must not hardcode a machine-specific drive.' }
if (-not (Test-Path -LiteralPath (Join-Path $root 'frame.ps1'))) { throw 'Referenced CLI is missing.' }
Write-Host 'PASS prompt-frame skill contract'
