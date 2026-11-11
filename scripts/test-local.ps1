[CmdletBinding()]
param(
    [ValidateSet('', '00-bootstrap', '01-java-core', '02-servlet-jsp', '03-spring-mvc', '04-banking-transaction', '05-security-operations', '06-spring-boot')]
    [string]$Module = '',
    [switch]$Offline,
    [switch]$Integration
)
$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$PreviousOptions = $env:MAVEN_OPTS
$TestExitCode = 1
Push-Location $ProjectRoot
try {
    $MavenPath = Join-Path $ProjectRoot 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $MavenPath)) {
        $MavenPath = Join-Path $ProjectRoot '.tools/apache-maven-3.10.0/bin/mvn.cmd'
    }
    if (-not (Test-Path -LiteralPath $MavenPath)) {
        $MavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
        if (-not $MavenCommand) { throw 'Maven is unavailable. Use the Docker test profile or install a project-local Maven distribution.' }
        $MavenPath = $MavenCommand.Source
    }
    New-Item -ItemType Directory -Force -Path '.cache/java-home', '.cache/tmp' | Out-Null
    $env:MAVEN_OPTS = $PreviousOptions + ' -Duser.home="' + (Join-Path $ProjectRoot '.cache/java-home') + '" -Djava.io.tmpdir="' + (Join-Path $ProjectRoot '.cache/tmp') + '"'
    $MavenArguments = @('-B', '-ntp', ('-Dmaven.repo.local=' + (Join-Path $ProjectRoot '.cache/maven-repository')))
    if ($Offline) { $MavenArguments += '-o' }
    if ($Module) { $MavenArguments += @('-pl', ('modules/' + $Module), '-am') }
    if ($Integration) { $MavenArguments += '-Pintegration-tests' }
    $MavenArguments += 'verify'
    & $MavenPath @MavenArguments
    $TestExitCode = $LASTEXITCODE
} catch {
    [Console]::Error.WriteLine('[FAIL] ' + $_.Exception.Message)
} finally {
    $env:MAVEN_OPTS = $PreviousOptions
    Pop-Location
}
exit $TestExitCode
