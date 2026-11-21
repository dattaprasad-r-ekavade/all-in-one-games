<#
.SYNOPSIS
  Cross-review a task branch. Use the agent family that did NOT build the task.

.EXAMPLE
  .\scripts\review-task.ps1 -Task CR2 -Reviewer claude   # Codex built it
  .\scripts\review-task.ps1 -Task A3  -Reviewer codex    # Claude built it
  .\scripts\review-task.ps1 -Task FU1 -Reviewer codex    # Gemini built it
#>
param(
    [Parameter(Mandatory = $true)][string]$Task,
    [Parameter(Mandatory = $true)][ValidateSet('codex', 'claude', 'gemini')][string]$Reviewer,
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
    $instructions = @"
You are the reviewer for task $Task on this branch. You do not change files.
Run: git diff $Base...HEAD and git log $Base..HEAD --oneline.
Review against the $Task card in docs/TASKS.md, the plan sections it names, and AGENTS.md.
Check: spec match, correctness, AGENTS.md section 5 hard rules, scope (Owns line), test quality.
Do not run Gradle; the lead runs the gate.
Output: '## Review: $Task', 'Verdict: merge | fix first', 'Blockers:' numbered with file:line or 'none', 'Suggestions:'.
Report only real problems.
"@
    if ($Reviewer -eq 'codex') {
        # 'codex review --base' does not accept a custom prompt, so use a read-only exec run.
        codex exec -m gpt-6.1-sol -c 'model_reasoning_effort="high"' -s read-only -o $review $instructions
        Get-Content $review
    } elseif ($Reviewer -eq 'gemini') {
        # Windows PowerShell 5.1 breaks quotes inside long native arguments, so the prompt goes in a file.
        $promptFile = Join-Path (Join-Path $wtRoot '_logs') "$Task.review-prompt.md"
        Set-Content -Path $promptFile -Value $instructions -Encoding UTF8
        agy --model gemini-3.8-flash-high "-p=Read the file $promptFile and do exactly what it says. Do not edit any file." |
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
