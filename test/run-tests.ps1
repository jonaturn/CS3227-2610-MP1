[CmdletBinding()]
param(
    [string] $JavaHome
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$testRunner = Join-Path $repositoryRoot ".codex\skills\test-ui\scripts\run_test_plan.py"

if (-not (Test-Path -LiteralPath $testRunner -PathType Leaf)) {
    throw "The UI test runner was not found at: $testRunner"
}

$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
if ($null -ne $pythonCommand) {
    $pythonExecutable = $pythonCommand.Source
} else {
    $bundledPython = Join-Path $env:USERPROFILE `
            ".cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
    if (-not (Test-Path -LiteralPath $bundledPython -PathType Leaf)) {
        throw "Python was not found. Install Python 3 or run the test-ui skill through Codex."
    }
    $pythonExecutable = $bundledPython
}

$runnerArguments = @("-B", $testRunner)
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $runnerArguments += @("--java-home", $JavaHome)
}

Push-Location $repositoryRoot
try {
    & $pythonExecutable @runnerArguments
    $testExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($testExitCode -ne 0) {
    throw "The UI test suite failed with exit code $testExitCode."
}
