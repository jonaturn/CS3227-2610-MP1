[CmdletBinding()]
param(
    [string] $JavaHome = $env:JAVA_HOME
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$portableJava = Join-Path $PSScriptRoot '_temp/tools/java25/zulu25.36.205-ca-jdk25.0.4.1-win_x64'
if ([string]::IsNullOrWhiteSpace($JavaHome) -or -not (Test-Path "$JavaHome/bin/java.exe")) {
    $JavaHome = $portableJava
}
$javaExecutable = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw 'Set JAVA_HOME to a JDK 25 installation, or pass -JavaHome <JDK25>.'
}
$javaVersion = (& $javaExecutable --version | Out-String)
if ($javaVersion -notmatch '(?m)^(openjdk|java) 25(?:[. ]|$)') {
    throw "Staniz requires Java 25. Found: $javaVersion"
}
$applicationJar = Join-Path $PSScriptRoot 'release/staniz.jar'
if (-not (Test-Path -LiteralPath $applicationJar)) {
    throw 'Build the app with gradlew shadowJar first.'
}
Push-Location $PSScriptRoot
try {
    & $javaExecutable --enable-native-access=ALL-UNNAMED -jar $applicationJar
    if ($LASTEXITCODE -ne 0) {
        throw "Staniz exited with code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
