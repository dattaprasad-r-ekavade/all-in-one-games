<#
.SYNOPSIS
  Test several finished task branches together, with one Gradle run.

.DESCRIPTION
  Merges main and each task/<ID> branch into a scratch worktree (..\witgrid-wt\_batch), then runs
  'gradlew check' one time. Build agents can then skip Gradle, so parallel builds do not slow the PC.
  A merge conflict stops the script and names the branch. The result goes to ..\witgrid-wt\_logs\batch.md.
  The _batch worktree is lead scratch space: the script resets it to main on every run.

.EXAMPLE
  .\scripts\check-batch.ps1 -Tasks FU1,SK1,MO1
#>
param(
    [Parameter(Mandatory = $true)][string[]]$Tasks,
    [string]$Base = 'main'
)
$ErrorActionPreference = 'Stop'
# 'powershell -File' passes "A,B,C" as one string, so split it here.
$Tasks = @($Tasks | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

$repo = (git rev-parse --show-toplevel).Trim()
$wtRoot = Join-Path (Split-Path $repo -Parent) 'witgrid-wt'
$wt = Join-Path $wtRoot '_batch'
$log = Join-Path (Join-Path $wtRoot '_logs') 'batch.md'
New-Item -ItemType Directory -Force (Split-Path $log) | Out-Null

if (-not (Test-Path $wt)) {
    git -C $repo worktree add $wt -B batch $Base
    if ($LASTEXITCODE -ne 0) { throw "git worktree add failed for _batch" }
} else {
    git -C $wt checkout -B batch $Base
    git -C $wt reset --hard $Base
    if ($LASTEXITCODE -ne 0) { throw "Could not reset _batch to $Base" }
}

foreach ($task in $Tasks) {
    git -C $wt merge --no-edit --no-ff "task/$task"
    if ($LASTEXITCODE -ne 0) {
        git -C $wt merge --abort
        throw "Merge conflict with task/$task. Test it alone, or fix the conflict first."
    }
}

Push-Location $wt
try {
    # One run, so the Gradle daemon is allowed here; it makes later batch runs faster.
    # PowerShell 5.1 turns redirected stderr lines into errors, so 'Stop' would end the script on a warning.
    $ErrorActionPreference = 'Continue'
    $output = & .\gradlew.bat check 2>&1
    $exit = $LASTEXITCODE
} finally {
    Pop-Location
}

$result = if ($exit -eq 0) { 'pass' } else { 'fail' }
@(
    "## Batch check: $($Tasks -join ', ')"
    "Result: $result"
    ''
    '```'
    ($output | Select-Object -Last 60)
    '```'
) | Out-File -FilePath $log -Encoding utf8

Write-Host "Batch check $result for $($Tasks -join ', '). Log: $log"
if ($exit -ne 0) {
    Write-Host 'Find the failing task in the log, then send the errors back:'
    Write-Host "  .\scripts\agent-task.ps1 -Task <ID> -Agent gemini -Extra 'Fix these errors: ...'"
    exit 1
}
