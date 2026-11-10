[CmdletBinding()]
param([switch]$Integration, [switch]$Restart)
$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ComposeArguments = @('compose', '--profile', 'phase0', '--profile', 'phase1', '--profile', 'phase2', '--profile', 'phase3', '--profile', 'phase4', '--profile', 'phase5', '--profile', 'phase6')
function Invoke-AllCompose {
    param([string[]]$Arguments)
    & docker @ComposeArguments @Arguments
    if ($LASTEXITCODE -ne 0) { throw ('Compose failed: ' + ($Arguments -join ' ')) }
}
Push-Location $ProjectRoot
try {
    & (Join-Path $PSScriptRoot 'doctor.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Docker environment is unavailable; runtime checks were not run.' }
    Invoke-AllCompose -Arguments @('up', '--build', '-d', '--wait', '--wait-timeout', '240', 'legacy', 'servlet-jsp', 'spring-mvc', 'banking-transaction', 'security-operations', 'spring-boot')
    Invoke-AllCompose -Arguments @('run', '--build', '--rm', 'java-core')
    if ($Restart) {
        & (Join-Path $PSScriptRoot 'verify.ps1') -Restart
        if ($LASTEXITCODE -ne 0) { throw 'Restart/persistence verification failed.' }
    }
    foreach ($Service in @('legacy', 'servlet-jsp', 'spring-mvc', 'banking-transaction', 'security-operations', 'spring-boot')) {
        $Binding = ((Invoke-AllCompose -Arguments @('port', $Service, '8080')) -join "`n").Trim()
        if ($Binding -notmatch '^127\.0\.0\.1:([0-9]+)$') { throw ('Unexpected binding for ' + $Service) }
        $HealthPath = if ($Service -eq 'legacy') { '/health/ready' } else { '/health' }
        $Response = Invoke-RestMethod -Uri ('http://127.0.0.1:' + $Matches[1] + $HealthPath) -TimeoutSec 10
        if ($Response.status -cne 'UP') { throw ('Health failed for ' + $Service) }
        Write-Host ('[PASS] HTTP/DB health: ' + $Service)
    }
    if ($Integration) {
        & docker compose --profile integration run --build --rm integration-test
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL integration tests failed.' }
    } else {
        Write-Host '[NOT RUN] PostgreSQL integration tests. Use -Integration to run them.'
    }
    Write-Host 'Requested checks passed. Containers and volumes are retained.'
} catch {
    [Console]::Error.WriteLine('[FAIL] ' + $_.Exception.Message)
    exit 1
} finally { Pop-Location }
