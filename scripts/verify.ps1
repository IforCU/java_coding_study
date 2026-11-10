[CmdletBinding()]
param([switch]$Restart)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Failed = $false

function Invoke-Compose {
    param([string[]]$Arguments)
    & docker compose --profile phase0 @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw ('Compose command failed: ' + ($Arguments -join ' '))
    }
}

function Invoke-DatabaseQuery {
    param([string]$Sql)
    $PreviousEncoding = $OutputEncoding
    try {
        $OutputEncoding = New-Object System.Text.UTF8Encoding($false)
        $PsqlCommand = 'exec psql -X -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A -F "|"'
        # Windows PowerShell 5.1 needs embedded quotes escaped for native argv.
        if ($PSVersionTable.PSVersion -lt [Version]'7.3' -or
            (Get-Variable PSNativeCommandArgumentPassing -ValueOnly -ErrorAction SilentlyContinue) -eq 'Legacy') {
            $PsqlCommand = $PsqlCommand.Replace('"', '\"')
        }
        $Result = $Sql | & docker compose --profile phase0 exec -T db sh -c $PsqlCommand
        if ($LASTEXITCODE -ne 0) { throw 'Database verification query failed.' }
        return ($Result -join "`n").Trim()
    } finally {
        $OutputEncoding = $PreviousEncoding
    }
}

function Assert-Ready {
    param([string]$BaseUrl, [int]$TimeoutSeconds = 5)
    try {
        $Ready = Invoke-RestMethod -Uri ($BaseUrl + '/health/ready') -Method Get -TimeoutSec $TimeoutSeconds
    } catch {
        throw 'Readiness HTTP request failed.'
    }
    if ($Ready.status -cne 'UP' -or $Ready.database -cne 'UP' -or
        [string]$Ready.schemaVersion -cne '1' -or $Ready.environment -cne 'local-learning') {
        throw 'Readiness response does not match the Phase 0 database contract.'
    }
}

Push-Location $ProjectRoot
try {
    # The runtime image executes mvn verify during its build, including unit tests.
    & (Join-Path $PSScriptRoot 'doctor.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Environment check failed. Runtime verification was not started.' }
    Invoke-Compose -Arguments @('up', '--build', '-d', '--wait', '--wait-timeout', '180')

    $Binding = ((Invoke-Compose -Arguments @('port', 'legacy', '8080')) -join "`n").Trim()
    if ($Binding -notmatch '^127\.0\.0\.1:([0-9]+)$') {
        throw 'Expected exactly one legacy HTTP port bound to 127.0.0.1.'
    }
    $Port = [int]$Matches[1]
    if ($Port -lt 1 -or $Port -gt 65535) { throw 'Invalid legacy HTTP port.' }
    $BaseUrl = 'http://127.0.0.1:' + $Port

    $HomeResponse = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + '/') -TimeoutSec 10
    if ($HomeResponse.StatusCode -ne 200 -or $HomeResponse.Content -notmatch 'Hello World') {
        throw 'The rendered JSP home page did not contain Hello World.'
    }
    Write-Host '[PASS] HTTP/JSP Hello World.'
    $Live = Invoke-RestMethod -Uri ($BaseUrl + '/health/live') -TimeoutSec 5
    if ($Live.status -cne 'UP') { throw 'Liveness response did not report UP.' }
    Write-Host '[PASS] Liveness.'
    Assert-Ready -BaseUrl $BaseUrl
    Write-Host '[PASS] Database readiness, schema version and environment.'

    $MetadataQuery = "SELECT environment, schema_version, (SELECT count(*) FROM bootstrap.schema_history WHERE version = 1) FROM bootstrap.app_metadata WHERE environment = 'local-learning';"
    $Before = Invoke-DatabaseQuery -Sql $MetadataQuery
    if ($Before -cne 'local-learning|1|1') { throw 'Initial database metadata is invalid.' }
    Invoke-Compose -Arguments @('run', '--rm', 'migrate')
    $After = Invoke-DatabaseQuery -Sql $MetadataQuery
    if ($After -cne $Before) { throw 'Migration re-run changed database metadata.' }
    Assert-Ready -BaseUrl $BaseUrl
    Write-Host '[PASS] Migration and seed re-run without changing metadata.'

    if ($Restart) {
        $Marker = 'phase0-' + [Guid]::NewGuid().ToString('N')
        $Inserted = Invoke-DatabaseQuery -Sql ("INSERT INTO bootstrap.verification_marker (marker_id) VALUES ('" + $Marker + "') RETURNING marker_id;")
        if ($Inserted -cne $Marker) { throw 'Persistence marker could not be inserted.' }
        Invoke-Compose -Arguments @('restart', 'db')
        Invoke-Compose -Arguments @('restart', 'legacy')
        $Timer = [Diagnostics.Stopwatch]::StartNew()
        $Recovered = $false
        while ($Timer.Elapsed.TotalSeconds -lt 60) {
            try {
                Assert-Ready -BaseUrl $BaseUrl -TimeoutSeconds 1
                $Recovered = $true
                break
            } catch {
                if ($Timer.Elapsed.TotalSeconds -lt 59) { Start-Sleep -Seconds 1 }
            }
        }
        $Timer.Stop()
        if (-not $Recovered) { throw 'Database readiness did not recover within 60 seconds after restart.' }
        $Retained = Invoke-DatabaseQuery -Sql ("SELECT marker_id FROM bootstrap.verification_marker WHERE marker_id = '" + $Marker + "';")
        if ($Retained -cne $Marker) { throw 'Persistence marker was not retained after restart.' }
        Write-Host '[PASS] DB and application restart; the inserted marker was retained.'
    } else {
        Write-Host '[NOT RUN] Restart and persistence check. Use -Restart to include it.'
    }
    Write-Host 'Requested verification checks passed. Containers are left running.'
} catch {
    [Console]::Error.WriteLine('[FAIL] ' + $_.Exception.Message)
    [Console]::Error.WriteLine('Verification stopped. Containers and database data were preserved.')
    $Failed = $true
} finally {
    Pop-Location
}
if ($Failed) { exit 1 }
exit 0
