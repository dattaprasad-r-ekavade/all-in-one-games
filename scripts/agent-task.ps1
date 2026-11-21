<#
.SYNOPSIS
  Start one task card from docs/TASKS.md in its own git worktree, with Codex or Claude.

.EXAMPLE
  .\scripts\agent-task.ps1 -Task CR2 -Agent codex -Tier H
  .\scripts\agent-task.ps1 -Task CR4 -Agent claude -Tier L
  .\scripts\agent-task.ps1 -Task FU1 -Agent gemini -Tier M

.NOTES
  Worktrees go to ..\witgrid-wt\<Task>. Handoff notes go to ..\witgrid-wt\_logs\<Task>.md.
  Tier -> model:
    codex : L = gpt-6-luna (medium), M = gpt-6.1-sol (medium), H = gpt-6.1-sol (high)
    claude: L = haiku,               M = sonnet,               H = sonnet
    gemini: L = gemini-3.8-flash-medium, M = gemini-3.8-flash-high, H = gemini-3.8-flash-high
            (Antigravity CLI 'agy'. Flash only; prefer codex for H cards.)
#>
param(
    [Parameter(Mandatory = $true)][string]$Task,
    [ValidateSet('codex', 'claude', 'gemini')][string]$Agent = 'codex',
    [ValidateSet('L', 'M', 'H')][string]$Tier = 'M',
    [string]$Base = 'main',
    [string]$Extra = '',
    # Gemini runs skip Gradle by default; the lead tests finished branches together with check-batch.ps1.
    [switch]$RunGradle
)
$ErrorActionPreference = 'Stop'

$repo = (git rev-parse --show-toplevel).Trim()
$wtRoot = Join-Path (Split-Path $repo -Parent) 'witgrid-wt'
$wt = Join-Path $wtRoot $Task
$logDir = Join-Path $wtRoot '_logs'
$branch = "task/$Task"
New-Item -ItemType Directory -Force $logDir | Out-Null

if (-not (Test-Path $wt)) {
    git -C $repo worktree add $wt -b $branch $Base
    if ($LASTEXITCODE -ne 0) { throw "git worktree add failed for $branch" }
} else {
    Write-Host "Worktree exists, resuming: $wt"
}

$handoff = Join-Path $logDir "$Task.md"
if ($Agent -eq 'gemini' -and -not $RunGradle) {
    # Full test runs in parallel block the PC; the lead runs one batch gate instead (scripts/check-batch.ps1).
    # The agent runs only its own package's tests: this catches bad fixtures and compile errors cheaply.
    $gateRules = @"
Do NOT run 'check', the full test suite, bankgen validate, or any other build task. The lead runs all tests later in one batch.
Write the code and the unit tests that the card asks for.
Before you commit, run ONLY the tests of the package you changed (this also compiles the module):
- engine:  .\gradlew.bat :engine:test --tests 'com.datathecodie.witgrid.engine.<package>.*' --no-daemon -q
- bankgen: .\gradlew.bat :bankgen:test --tests 'com.datathecodie.witgrid.bankgen.*' --no-daemon -q
- app:     .\gradlew.bat :app:testDebugUnitTest --tests 'com.datathecodie.witgrid.games.<package>.*' --no-daemon -q
Replace <package> with your game package, for example 'mosaic'.
Fix every compile error and test failure, then run the command again until it passes. Never weaken a test to make it pass: fix the code or build a correct fixture. Then commit.
In the handoff, write your package test command as '<command> -> pass', and each 'Done when' command as '<command> -> not run (lead batch gate)'.
"@
} else {
    $gateRules = @"
Run every 'Done when' command. Continue until all pass, or until you are blocked.
Always add --no-daemon to Gradle commands (a daemon started in the sandbox breaks later builds outside it).
"@
}
$prompt = @"
You are a build agent on the Witgrid project.
Do task $Task from docs/TASKS.md, and only that task.
Read AGENTS.md first and obey it. Then read the plan sections that the task card names.
Change only the files in the card's Owns line.
$gateRules
Commit on the current branch with the message '${Task}: <summary>'.
End with the handoff note from AGENTS.md section 7.
$Extra
"@

if ($Agent -eq 'codex') {
    $model  = @{ L = 'gpt-6-luna'; M = 'gpt-6.1-sol'; H = 'gpt-6.1-sol' }[$Tier]
    $effort = @{ L = 'medium';     M = 'medium';      H = 'high' }[$Tier]
    $gradleHome = Join-Path $env:USERPROFILE '.gradle'
    New-Item -ItemType Directory -Force $gradleHome | Out-Null
    # Gradle needs the network (dependencies) and write access to ~/.gradle (cache).
    $prompt | codex exec `
        -m $model `
        -c "model_reasoning_effort=$effort" `
        -c 'sandbox_workspace_write.network_access=true' `
        -s workspace-write `
        -C $wt `
        --add-dir $gradleHome `
        --add-dir (Join-Path $repo '.git') `
        --add-dir $env:ANDROID_HOME `
        -o $handoff `
        -
} elseif ($Agent -eq 'gemini') {
    $model = @{ L = 'gemini-3.8-flash-medium'; M = 'gemini-3.8-flash-high'; H = 'gemini-3.8-flash-high' }[$Tier]
    # Windows PowerShell 5.1 breaks quotes inside long native arguments, so the prompt goes in a file.
    $promptFile = Join-Path $logDir "$Task.prompt.md"
    Set-Content -Path $promptFile -Value $prompt -Encoding UTF8
    Push-Location $wt
    try {
        # Tee-Object in PowerShell 5.1 writes UTF-16, so write the handoff as UTF-8 separately.
        agy --model $model "-p=Read the file $promptFile and do exactly what it says. The current folder is the worktree for the task." |
            Tee-Object -Variable agentOutput
        $agentOutput | Out-File -FilePath $handoff -Encoding utf8
    } finally {
        Pop-Location
    }
} else {
    $model = @{ L = 'haiku'; M = 'sonnet'; H = 'sonnet' }[$Tier]
    Push-Location $wt
    try {
        $prompt | claude -p --model $model --permission-mode acceptEdits | Tee-Object -FilePath $handoff
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "Handoff note: $handoff"
Write-Host "Next: .\scripts\review-task.ps1 -Task $Task -Reviewer <other agent family>"
