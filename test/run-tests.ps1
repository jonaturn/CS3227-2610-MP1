[CmdletBinding()]
param(
    [string] $JavaHome
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$previousJavaHome = $env:JAVA_HOME

try {
    if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
        $env:JAVA_HOME = $JavaHome
    }
    Push-Location $repositoryRoot
    try {
        & .\gradlew.bat guiTest --console=plain
        if ($LASTEXITCODE -ne 0) {
            throw "The JavaFX UI suite failed. See build/reports/tests/guiTest/index.html."
        }
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
