$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$initializers = @(
    @{ Source = 'PromptForge-v1.json'; Destination = '.promptforge/program.json' },
    @{ Source = 'PromptForge-context-v1.json'; Destination = '.promptforge/outlines/PromptForge-context.json' }
)

foreach ($initializer in $initializers) {
    $destination = Join-Path $root $initializer.Destination
    if (Test-Path -LiteralPath $destination) {
        Write-Host "Preserved existing local data: $destination"
        continue
    }
    New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $root "templates/$($initializer.Source)") -Destination $destination
    Write-Host "Initialized $destination from templates/$($initializer.Source)."
}
