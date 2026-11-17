<#
.SYNOPSIS
Capture the local visual QA matrix and refresh the store capture set. No upload occurs.
.EXAMPLE
.\scripts\screenshots\capture.ps1 -Serial emulator-5554
#>
param([string]$Serial)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$adbPath = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
$outputDirectory = Join-Path $repositoryRoot 'build\u8'
$storeDirectory = Join-Path $repositoryRoot 'app\src\androidTest\screenshots'
$deviceDirectory = '/sdcard/Pictures/WitgridCapture'
$screenNames = @('welcome', 'home', 'game', 'crowns_board', 'binary_board', 'nonogram_board',
    'mini_sudoku_board', 'result', 'level_failed', 'calendar', 'statistics', 'settings')
if (-not (Test-Path -LiteralPath $adbPath)) { throw 'Set ANDROID_HOME to an installed Android SDK.' }
if ([string]::IsNullOrWhiteSpace($Serial)) {
    $device = & $adbPath devices | Where-Object { $_ -match '^emulator-\S+\s+device$' } | Select-Object -First 1
    if (-not $device) { throw 'Connect and boot an Android emulator before running this script.' }
    $Serial = ($device -split '\s+')[0]
}
if ($Serial -notmatch '^emulator-\d+$') { throw 'Screenshot profiles require an emulator.' }
function Invoke-Adb {
    param([string[]]$Arguments)
    # adb reports successful transfer progress on stderr in Windows PowerShell.
    $ErrorActionPreference = 'Continue'
    $result = & $adbPath -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb $($Arguments -join ' ') failed ($LASTEXITCODE)." }
    return $result
}
function Get-Override([string]$Kind) {
    $state = (Invoke-Adb @('shell', 'wm', $Kind)) -join "`n"
    $value = [regex]::Match($state, "Override ${Kind}:\s*(\S+)").Groups[1].Value
    if ($value) { return $value }
    return 'reset'
}
$originalSize = Get-Override 'size'
$originalDensity = Get-Override 'density'
$originalFont = (Invoke-Adb @('shell', 'settings', 'get', 'system', 'font_scale')).Trim()
$originalAirplane = (Invoke-Adb @('shell', 'cmd', 'connectivity', 'airplane-mode')).Trim()
if ($originalAirplane -notin @('enabled', 'disabled')) { throw "Unknown airplane mode: $originalAirplane" }
$previousAndroidSerial = $env:ANDROID_SERIAL
$primaryError = $null
$manifest = [System.Collections.Generic.List[object]]::new()
New-Item -ItemType Directory -Force -Path $outputDirectory, $storeDirectory | Out-Null
# A failed rerun must not leave a previous run's success manifest or review sheets.
foreach ($name in @('manifest.json', 'contact-sheet-360dp.png', 'contact-sheet-480dp.png')) {
    $oldSummary = Join-Path $outputDirectory $name
    if (Test-Path -LiteralPath $oldSummary) { Remove-Item -LiteralPath $oldSummary }
}
try {
    $env:ANDROID_SERIAL = $Serial
    Push-Location $repositoryRoot
    try {
        & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
        if ($LASTEXITCODE -ne 0) { throw 'Screenshot APK build failed.' }
    } finally { Pop-Location }
    Invoke-Adb @('install', '-r', (Join-Path $repositoryRoot 'app\build\outputs\apk\debug\app-debug.apk')) | Out-Null
    Invoke-Adb @('install', '-r', (Join-Path $repositoryRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')) | Out-Null
    Invoke-Adb @('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable') | Out-Null
    # Both profiles retain 1080 x 1920 store-sized pixels; density sets the real dp width.
    foreach ($width in @(360, 480)) {
        Invoke-Adb @('shell', 'wm', 'size', '1080x1920') | Out-Null
        $density = if ($width -eq 360) { '480' } else { '360' }
        Invoke-Adb @('shell', 'wm', 'density', $density) | Out-Null
        foreach ($font in @('1.0', '1.3', '2.0')) {
            Invoke-Adb @('shell', 'settings', 'put', 'system', 'font_scale', $font) | Out-Null
            foreach ($theme in @('light', 'dark')) {
                $profile = "${width}dp/$theme-font$font"
                $directory = Join-Path $outputDirectory $profile
                New-Item -ItemType Directory -Force -Path $directory | Out-Null
                $primary = $width -eq 360 -and $font -eq '1.0' -and $theme -eq 'light'
                Write-Host "Capturing $profile"
                Invoke-Adb @('shell', 'am', 'force-stop', 'com.datathecodie.witgrid') | Out-Null
                # A fresh instrumentation process must consume the changed system configuration.
                $result = Invoke-Adb @('shell', 'am', 'instrument', '-w', '-r',
                    '-e', 'class', 'com.datathecodie.witgrid.screenshots.ScreenshotCaptureTest',
                    '-e', 'captureTheme', $theme, '-e', 'captureFontScale', $font,
                    '-e', 'captureWidthDp', "$width", '-e', 'captureOffline', "$primary".ToLowerInvariant(),
                    'com.datathecodie.witgrid.test/androidx.test.runner.AndroidJUnitRunner')
                $result | Set-Content -LiteralPath (Join-Path $directory 'instrumentation.txt')
                if (($result -join "`n") -notmatch 'OK \(1 test\)' -or ($result -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED') {
                    throw "Capture failed for $profile. See its instrumentation.txt."
                }
                foreach ($name in $screenNames) {
                    $localFile = Join-Path $directory "$name.png"
                    Invoke-Adb @('pull', "$deviceDirectory/$name.png", $localFile) | Out-Null
                    if (-not (Test-Path -LiteralPath $localFile) -or (Get-Item -LiteralPath $localFile).Length -eq 0) {
                        throw "Missing capture: $localFile"
                    }
                    $manifest.Add([pscustomobject]@{ widthDp = $width; density = $density; fontScale = $font;
                        theme = $theme; screen = $name; file = "$profile/$name.png" })
                }
                if ($primary) {
                    Invoke-Adb @('pull', "$deviceDirectory/offline_message.png", (Join-Path $outputDirectory 'offline_message.png')) | Out-Null
                }
            }
        }
    }
    if ($manifest.Count -ne 144) { throw "Expected 144 matrix captures, got $($manifest.Count)." }
    $manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDirectory 'manifest.json')
    & (Join-Path $PSScriptRoot 'contact-sheets.ps1') -Directory $outputDirectory
    foreach ($name in $screenNames) {
        Copy-Item -LiteralPath (Join-Path $outputDirectory "360dp/light-font1.0/$name.png") -Destination (Join-Path $storeDirectory "$name.png")
    }
    Copy-Item -LiteralPath (Join-Path $outputDirectory '360dp/dark-font1.0/home.png') -Destination (Join-Path $storeDirectory 'dark_mode.png')
    Copy-Item -LiteralPath (Join-Path $outputDirectory 'offline_message.png') -Destination (Join-Path $storeDirectory 'offline_message.png')
    if ((Get-ChildItem -LiteralPath $storeDirectory -Filter '*.png').Count -ne 14) { throw 'Expected 14 store captures.' }
    Write-Host "Verified 144 matrix images, two contact sheets, and 14 store captures. Review $outputDirectory before every closed-test upload."
} catch {
    $primaryError = $_
    throw
} finally {
    $restoreErrors = [System.Collections.Generic.List[string]]::new()
    $restoreActions = @(
        @{ label = 'size'; arguments = @('shell', 'wm', 'size', $originalSize) },
        @{ label = 'density'; arguments = @('shell', 'wm', 'density', $originalDensity) },
        @{ label = 'font'; arguments = if ($originalFont -eq 'null') {
            @('shell', 'settings', 'delete', 'system', 'font_scale')
        } else { @('shell', 'settings', 'put', 'system', 'font_scale', $originalFont) } },
        @{ label = 'airplane'; arguments = @('shell', 'cmd', 'connectivity', 'airplane-mode',
            $(if ($originalAirplane -eq 'enabled') { 'enable' } else { 'disable' })) }
    )
    foreach ($action in $restoreActions) {
        try { Invoke-Adb $action.arguments | Out-Null }
        catch { $restoreErrors.Add("$($action.label): $($_.Exception.Message)") }
    }
    $env:ANDROID_SERIAL = $previousAndroidSerial
    if ($restoreErrors.Count -gt 0) {
        $message = "Failed to restore emulator state: $($restoreErrors -join '; ')"
        if ($primaryError) { Write-Warning $message } else { throw $message }
    }
}
