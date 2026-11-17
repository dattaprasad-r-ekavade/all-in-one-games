<#
.SYNOPSIS
  Capture the eight store screens on a connected Android emulator.

.EXAMPLE
  .\scripts\screenshots\capture.ps1
  .\scripts\screenshots\capture.ps1 -Serial emulator-5554
#>
param(
    [string]$Serial
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
$adbPath = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
$screenshotDirectory = Join-Path $repositoryRoot 'app\src\androidTest\screenshots'
$deviceScreenshotDirectory = '/sdcard/Pictures/WitgridCapture'
$expectedScreenshots = @(
    'home.png',
    'dark_mode.png',
    'crowns_board.png',
    'binary_board.png',
    'nonogram_board.png',
    'mini_sudoku_board.png',
    'result.png',
    'offline_message.png'
)

if (-not (Test-Path -LiteralPath $adbPath)) {
    throw "Android Debug Bridge was not found at '$adbPath'. Set ANDROID_HOME first."
}

if ([string]::IsNullOrWhiteSpace($Serial)) {
    $connectedEmulator = & $adbPath devices | Where-Object { $_ -match '^emulator-\S+\s+device$' } | Select-Object -First 1
    if (-not $connectedEmulator) {
        throw 'Connect and boot an Android emulator before running this script.'
    }
    $Serial = ($connectedEmulator -split '\s+')[0]
}

function Invoke-Adb {
    param([string[]]$Arguments)
    & $adbPath -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Arguments -join ' ') failed with exit code $LASTEXITCODE."
    }
}

$displayState = (& $adbPath -s $Serial shell wm size) -join "`n"
$existingOverride = [regex]::Match($displayState, 'Override size:\s*(\d+x\d+)').Groups[1].Value
$airplaneMode = (& $adbPath -s $Serial shell cmd connectivity airplane-mode).Trim()
$previousAndroidSerial = $env:ANDROID_SERIAL
$resolvedScreenshotDirectory = [System.IO.Path]::GetFullPath($screenshotDirectory)
$expectedOutputPrefix = [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot 'app\src\androidTest'))
if (-not $resolvedScreenshotDirectory.StartsWith($expectedOutputPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'The screenshot output directory resolved outside app/src/androidTest.'
}

New-Item -ItemType Directory -Force -Path $resolvedScreenshotDirectory | Out-Null
foreach ($name in $expectedScreenshots) {
    $oldScreenshot = Join-Path $resolvedScreenshotDirectory $name
    if (Test-Path -LiteralPath $oldScreenshot) {
        Remove-Item -LiteralPath $oldScreenshot
    }
}

$primaryError = $null
try {
    $env:ANDROID_SERIAL = $Serial
    Invoke-Adb @('shell', 'wm', 'size', '1080x1920') | Out-Null
    Invoke-Adb @('shell', 'mkdir', '-p', $deviceScreenshotDirectory) | Out-Null
    foreach ($name in $expectedScreenshots) {
        Invoke-Adb @('shell', 'rm', '-f', "$deviceScreenshotDirectory/$name") | Out-Null
    }
    if ($airplaneMode -ne 'enabled') {
        Invoke-Adb @('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable') | Out-Null
    }

    Push-Location $repositoryRoot
    try {
        & $gradleWrapper ':app:connectedDebugAndroidTest' `
            '-Pandroid.testInstrumentationRunnerArguments.class=com.datathecodie.witgrid.screenshots.ScreenshotCaptureTest'
        if ($LASTEXITCODE -ne 0) {
            throw "Screenshot instrumentation failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }

    foreach ($name in $expectedScreenshots) {
        $deviceFile = "$deviceScreenshotDirectory/$name"
        $localFile = Join-Path $resolvedScreenshotDirectory $name
        Invoke-Adb @('pull', $deviceFile, $localFile) | Out-Null
        if (-not (Test-Path -LiteralPath $localFile) -or (Get-Item -LiteralPath $localFile).Length -eq 0) {
            throw "Screenshot '$name' was not captured."
        }
    }
    foreach ($name in $expectedScreenshots) {
        Invoke-Adb @('shell', 'rm', '-f', "$deviceScreenshotDirectory/$name") | Out-Null
    }

    Write-Host "Captured $($expectedScreenshots.Count) screenshots in $resolvedScreenshotDirectory"
} catch {
    $primaryError = $_
    throw
} finally {
    $restoreErrors = [System.Collections.Generic.List[string]]::new()
    try {
        if ($airplaneMode -ne 'enabled') {
            Invoke-Adb @('shell', 'cmd', 'connectivity', 'airplane-mode', 'disable') | Out-Null
        }
    } catch {
        $restoreErrors.Add("Airplane mode: $($_.Exception.Message)")
    }
    try {
        if ($existingOverride) {
            Invoke-Adb @('shell', 'wm', 'size', $existingOverride) | Out-Null
        } else {
            Invoke-Adb @('shell', 'wm', 'size', 'reset') | Out-Null
        }
    } catch {
        $restoreErrors.Add("Display size: $($_.Exception.Message)")
    }
    try {
        if ([string]::IsNullOrEmpty($previousAndroidSerial)) {
            Remove-Item Env:\ANDROID_SERIAL -ErrorAction Stop
        } else {
            $env:ANDROID_SERIAL = $previousAndroidSerial
        }
    } catch {
        $restoreErrors.Add("ANDROID_SERIAL: $($_.Exception.Message)")
    }
    if ($restoreErrors.Count -gt 0) {
        $restoreMessage = "Failed to restore emulator state: $($restoreErrors -join '; ')"
        if ($null -ne $primaryError) {
            Write-Warning $restoreMessage
        } else {
            throw $restoreMessage
        }
    }
}
