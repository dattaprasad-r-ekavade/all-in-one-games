<#
.SYNOPSIS
  Cross-review a task branch. Use the agent family that did NOT build the task.

.EXAMPLE
  .\scripts\review-task.ps1 -Task CR2 -Reviewer claude   # Codex built it
  .\scripts\review-task.ps1 -Task A3  -Reviewer codex    # Claude built it
#>
param(
    [Parameter(Mandatory = $true)][string]$Task,
    [Parameter(Mandatory = $true)][ValidateSet('codex', 'claude')][string]$Reviewer,
    [string]$Base = 'main'
)
$ErrorActionPreference = 'Stop'

$repo = (git rev-parse --show-toplevel).Trim()
$wtRoot = Join-Path (Split-Path $repo -Parent) 'witgrid-wt'
$wt = Join-Path $wtRoot $Task
$review = Join-Path (Join-Path $wtRoot '_logs') "$Task.review.md"
if (-not (Test-Path $wt)) { throw "No worktree for $Task at $wt" }

Push-Location $wt
try {
    if ($Reviewer -eq 'codex') {
        $instructions = "Review task $Task against docs/TASKS.md and AGENTS.md. Check spec match, correctness, AGENTS.md section 5 hard rules, and scope. Report blockers with file:line."
        codex review --base $Base -c 'model=gpt-6.1-sol' -c 'model_reasoning_effort=high' $instructions |
            Tee-Object -FilePath $review
    } else {
        "Use the reviewer agent to review task $Task on this branch against $Base." |
            claude -p --model sonnet | Tee-Object -FilePath $review
    }
} finally {
    Pop-Location
}
Write-Host ""
Write-Host "Review saved: $review"
