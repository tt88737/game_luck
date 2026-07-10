param(
    [switch]$SkipPackage,
    [switch]$OpenBrowser
)

$ErrorActionPreference = 'Stop'

$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$BackendDir = Join-Path $Root 'backend'
$AdminUiDir = Join-Path $Root 'admin-ui'
$JarPath = Join-Path $BackendDir 'gameluck-admin\target\gameluck-admin.jar'
$Maven = 'C:\tools\apache-maven-3.9.16\bin\mvn.cmd'
$BackendLog = Join-Path $env:TEMP 'gameluck-main-backend.out.log'
$BackendErr = Join-Path $env:TEMP 'gameluck-main-backend.err.log'
$AdminLog = Join-Path $env:TEMP 'gameluck-main-admin-ui.out.log'
$AdminErr = Join-Path $env:TEMP 'gameluck-main-admin-ui.err.log'

function Get-ListenerProcess {
    param([int]$Port)

    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $conn) {
        return $null
    }
    Get-CimInstance Win32_Process -Filter "ProcessId = $($conn.OwningProcess)" -ErrorAction SilentlyContinue
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)

    throw "Timed out waiting for $Url"
}

if (-not (Test-Path $AdminUiDir)) {
    throw "Missing admin-ui directory: $AdminUiDir"
}
if (-not (Test-Path $BackendDir)) {
    throw "Missing backend directory: $BackendDir"
}

$backendProcess = Get-ListenerProcess -Port 8080
if (-not $backendProcess) {
    if (-not $SkipPackage) {
        if (-not (Test-Path $Maven)) {
            throw "Maven not found: $Maven"
        }
        Write-Host "Packaging backend..."
        & $Maven -pl gameluck-admin -am package -Plocal -DskipTests
        if ($LASTEXITCODE -ne 0) {
            throw "Backend package failed."
        }
    }

    if (-not (Test-Path $JarPath)) {
        throw "Backend jar not found: $JarPath"
    }

    Remove-Item $BackendLog, $BackendErr -Force -ErrorAction SilentlyContinue
    Write-Host "Starting backend on http://localhost:8080 ..."
    Start-Process -FilePath 'java' `
        -ArgumentList @('-jar', 'gameluck-admin\target\gameluck-admin.jar', '--spring.profiles.active=local') `
        -WorkingDirectory $BackendDir `
        -RedirectStandardOutput $BackendLog `
        -RedirectStandardError $BackendErr `
        -WindowStyle Hidden
} else {
    Write-Host "Backend already listening on 8080, pid=$($backendProcess.ProcessId)."
}

Wait-HttpOk -Url 'http://localhost:8080/' -TimeoutSeconds 90

$adminProcess = Get-ListenerProcess -Port 5173
if (-not $adminProcess) {
    $pnpm = (Get-Command pnpm.cmd -ErrorAction Stop).Source
    Remove-Item $AdminLog, $AdminErr -Force -ErrorAction SilentlyContinue
    Write-Host "Starting Admin UI on http://localhost:5173 ..."
    Start-Process -FilePath $pnpm `
        -ArgumentList @('--dir', 'admin-ui', 'dev') `
        -WorkingDirectory $Root `
        -RedirectStandardOutput $AdminLog `
        -RedirectStandardError $AdminErr `
        -WindowStyle Hidden
} else {
    Write-Host "Admin UI already listening on 5173, pid=$($adminProcess.ProcessId)."
}

Wait-HttpOk -Url 'http://localhost:5173/' -TimeoutSeconds 90

Write-Host ''
Write-Host 'GameLuck B-side is ready.'
Write-Host 'Admin UI : http://localhost:5173/'
Write-Host 'Backend  : http://localhost:8080/'
Write-Host "Backend log: $BackendLog"
Write-Host "Admin log  : $AdminLog"

if ($OpenBrowser) {
    Start-Process 'http://localhost:5173/'
}
