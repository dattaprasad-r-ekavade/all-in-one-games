<#
.SYNOPSIS
  Start one task card from docs/TASKS.md in its own git worktree, with Codex or Claude.

.EXAMPLE
  .\scripts\agent-task.ps1 -Task CR2 -Agent codex -Tier H
  .\scripts\agent-task.ps1 -Task CR4 -Agent claude -Tier L

.NOTES
  Worktrees go to ..\witgrid-wt\<Task>. Handoff notes go to ..\witgrid-wt\_logs\<Task>.md.
  Tier -> model:
    codex : L = gpt-6-luna (medium), M = gpt-6.1-sol (medium), H = gpt-6.1-sol (high)
    claude: L = haiku,               M = sonnet,               H = sonnet
#>
param(
    [Parameter(Mandatory = $true)][string]$Task,
    [ValidateSet('codex', 'claude')][string]$Agent = 'codex',
    [ValidateSet('L', 'M', 'H')][string]$Tier = 'M',
    [string]$Base = 'main',
    [string]$Extra = ''
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
$prompt = @"
You are a build agent on the Witgrid project.
Do task $Task from docs/TASKS.md, and only that task.
Read AGENTS.md first and obey it. Then read the plan sections that the task card names.
Change only the files in the card's Owns line.
Run every 'Done when' command. Continue until all pass, or until you are blocked.
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
        -o $handoff `
        -
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
