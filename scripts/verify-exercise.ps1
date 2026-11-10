[CmdletBinding()]
param([string]$SqlFile = 'exercises/starter/phase-00/read-metadata.sql')

$ErrorActionPreference = 'Stop'
$ProjectRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$Failed = $false
Push-Location $ProjectRoot
try {
    if ([IO.Path]::IsPathRooted($SqlFile)) {
        $FullPath = [IO.Path]::GetFullPath($SqlFile)
    } else {
        $FullPath = [IO.Path]::GetFullPath((Join-Path $ProjectRoot $SqlFile))
    }
    $RootPrefix = $ProjectRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    if (-not $FullPath.StartsWith($RootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'The SQL file must be inside this project.'
    }
    if (-not (Test-Path -LiteralPath $FullPath -PathType Leaf)) { throw 'The SQL file was not found.' }
    # Reject links and junctions below the project root to keep file reads local.
    $Current = Get-Item -LiteralPath $FullPath -Force
    while ($Current.FullName -ne $ProjectRoot) {
        if (($Current.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw 'The SQL path must not contain symbolic links or junctions.'
        }
        if ($Current -is [IO.FileInfo]) {
            $Current = $Current.Directory
        } else {
            $Current = $Current.Parent
        }
        if ($null -eq $Current) { throw 'The SQL file must be inside this project.' }
    }
    $Sql = Get-Content -LiteralPath $FullPath -Raw -Encoding UTF8
    if ([string]::IsNullOrWhiteSpace($Sql)) { throw 'The SQL file is empty.' }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI is missing.' }
    $PreviousEncoding = $OutputEncoding
    try {
        $OutputEncoding = New-Object System.Text.UTF8Encoding($false)
        $PsqlCommand = 'exec psql -X -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A -F "|"'
        # Windows PowerShell 5.1 needs embedded quotes escaped for native argv.
        if ($PSVersionTable.PSVersion -lt [Version]'7.3' -or
            (Get-Variable PSNativeCommandArgumentPassing -ValueOnly -ErrorAction SilentlyContinue) -eq 'Legacy') {
            $PsqlCommand = $PsqlCommand.Replace('"', '\"')
        }
        $Result = $Sql | & docker compose --profile phase0 exec -T -e 'PGOPTIONS=-c default_transaction_read_only=on' db sh -c $PsqlCommand
        if ($LASTEXITCODE -ne 0) { throw 'SQL validation failed. Check your query and the running Phase 0 database.' }
    } finally {
        $OutputEncoding = $PreviousEncoding
    }
    if (($Result -join "`n") -cne 'local-learning|1') {
        throw 'The query result does not match the exercise. Review exercise.md and hints.md.'
    }
    Write-Host '[PASS] Phase 0 SQL exercise returned the expected metadata.'
} catch {
    [Console]::Error.WriteLine('[FAIL] ' + $_.Exception.Message)
    $Failed = $true
} finally {
    Pop-Location
}
if ($Failed) { exit 1 }
exit 0
