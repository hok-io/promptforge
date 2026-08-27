param([switch] $SkipBrowser)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$previousJavaHome = $env:JAVA_HOME
$previousMavenHome = $env:MAVEN_USER_HOME
$previousBrowser = $env:PROMPTFORGE_BROWSER_CHANNEL
Push-Location $root
try {
    $bundledJdk = Join-Path $root '.tools/jdk-21.0.12.1+1'
    if (Test-Path -LiteralPath $bundledJdk) { $env:JAVA_HOME = $bundledJdk }
    $env:MAVEN_USER_HOME = Join-Path $root '.tools/maven-home'
    New-Item -ItemType Directory -Path (Join-Path $root 'build/test-tmp') -Force | Out-Null
    if ($env:OS -eq 'Windows_NT' -and (Test-Path -LiteralPath 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe')) {
        $env:PROMPTFORGE_BROWSER_CHANNEL = 'msedge'
    }
    $arguments = @('clean', 'verify')
    if ($SkipBrowser) { $arguments += '-Dtest=!BrowserWorkflowTest' }
    & ./mvnw.cmd @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Web build or tests failed. Read the failing test above.' }
    Write-Host 'Built web/target/promptforge-web-1.0-SNAPSHOT.jar'
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:MAVEN_USER_HOME = $previousMavenHome
    $env:PROMPTFORGE_BROWSER_CHANNEL = $previousBrowser
    Pop-Location
}
