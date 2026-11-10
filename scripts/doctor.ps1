[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Failed = $false
Push-Location $ProjectRoot
try {
    Write-Host 'Phase 0 environment check (read only)'
    if (Get-Command java -ErrorAction SilentlyContinue) {
        Write-Host '[INFO] Host Java is available. The build uses the Java 17 container.'
    } else {
        Write-Host '[INFO] Host Java is optional; the build uses the Java 17 container.'
    }
    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        Write-Host '[INFO] Host Maven is available. The container build is the reference build.'
    } else {
        Write-Host '[INFO] Host Maven is optional; Maven is supplied by the build container.'
    }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI is missing. Install Docker Desktop with Linux containers before running Phase 0.'
    }
    & docker compose version
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose v2 is unavailable.'
    }
    $ServerType = & docker info --format '{{.OSType}}'
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker engine is unavailable. Start Docker Desktop and select Linux containers, then retry.'
    }
    if (($ServerType -join "`n").Trim() -ne 'linux') {
        throw 'Phase 0 requires a Linux Docker engine. Select Linux containers and retry.'
    }
    Write-Host '[PASS] Linux Docker engine is reachable.'
    & docker compose --profile phase0 config --quiet
    if ($LASTEXITCODE -ne 0) {
        throw 'Compose configuration is invalid. Review compose.yaml and the project .env file.'
    }
    Write-Host '[PASS] Compose configuration is valid.'
    Write-Host 'Environment check passed. Runtime build and HTTP/DB checks have not been run.'
} catch {
    [Console]::Error.WriteLine('[FAIL] ' + $_.Exception.Message)
    $Failed = $true
} finally {
    Pop-Location
}
if ($Failed) { exit 1 }
exit 0
