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
    codex : L = gpt-6-luna (medium), M = gpt-6.2-sol (medium), H = gpt-6.2-sol (high)
    claude: L = haiku,               M = sonnet,               H = sonnet
    gemini: L = gemini-4-hydrogen-medium, M = gemini-4-hydrogen-high, H = gemini-4-hydrogen-xhigh
            (Antigravity CLI 'agy'.)
    grok  : L = grok-4.7-build-fast,     M = grok-4.6,               H = grok-4.7
            (Grok CLI, headless, acceptEdits: it edits files but cannot run Gradle or git; the lead compiles and commits.)
#>
param(
    [Parameter(Mandatory = $true)][string]$Task,
    [ValidateSet('codex', 'claude', 'gemini', 'grok')][string]$Agent = 'codex',
    [ValidateSet('L', 'M', 'H')][string]$Tier = 'M',
    [string]$Base = 'main',
    [string]$Extra = '',
    # Agents run only their own package tests by default; the lead tests finished branches together with check-batch.ps1.
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
if (-not $RunGradle) {
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
Run every command in the foreground and wait for it to finish. Never start a command in the background, and never end your turn while a command is running or while work is not committed; this is a one-shot run, so nothing continues after your turn ends.
In the handoff, write your package test command as '<command> -> pass', and each 'Done when' command as '<command> -> not run (lead batch gate)'.
"@
} else {
    $gateRules = @"
Run every 'Done when' command. Continue until all pass, or until you are blocked.
Always add --no-daemon to Gradle commands (a daemon started in the sandbox breaks later builds outside it).
"@
}
# Lane prefix -> engine id and package (KS has no engine yet; its id is fixed here so its cards agree).
$laneGames = @{
    SU = 'sudoku'; KS = 'killersudoku'; FU = 'futoshiki'; SK = 'skyscrapers'; MO = 'mosaic'
    PA = 'path'; CS = 'coloursort'; AR = 'arrows'; KL = 'klondike'; CT = 'tactics'
}
$lanePrefix = if ($Task.Length -ge 3) { $Task.Substring(0, 2) } else { '' }
$gameId = $laneGames[$lanePrefix]
$cardRules = ''

if ($gameId -and $Task -match '^[A-Z]{2}B$' -and $Agent -in @('claude', 'grok')) {
    # Claude print mode and Grok acceptEdits mode have no shell, so they cannot run bankgen.
    throw "$Task is a bank card and needs bankgen. Use -Agent codex or -Agent gemini."
}
if ($gameId -and $Task -match '^[A-Z]{2}B$') {
    # Every bank card needs bankgen and the Registry; without this, agents stop and ask (SUB, 25 Nov 2026).
    $cardRules = @"
LEAD AUTHORIZATION FOR BANK CARDS (this overrides the 'no bankgen' rule above for this card only):
1. You MAY run bank generation and validation:
   .\gradlew.bat :bankgen:run --args="generate --game $gameId --pack <easy|medium|hard|expert> --count <n> --seed <s>" --no-daemon -q
   .\gradlew.bat :bankgen:run --args="generate --game $gameId --daily --seed <s>" --no-daemon -q
   .\gradlew.bat :bankgen:run --args="validate --all" --no-daemon -q
   Take the counts from the card and plan. Pick an unused seed block (see docs/reports/bank-*.md) and record every seed in the report.
   If validate reports a duplicate across files (small boards), try the next daily seed and record the failed seeds.
2. You MAY edit engine/src/main/kotlin/com/datathecodie/witgrid/engine/Registry.kt and engine/src/test/kotlin/com/datathecodie/witgrid/engine/RegistryTest.kt.
   bankgen finds games only through Registry.engines. Add this game's engine at the end of the list and update the id list and count in RegistryTest.
3. Write docs/reports/bank-$gameId.md like docs/reports/bank-skyscrapers.md: seeds, counts, sizes, compressed bytes, grade histogram per pack,
   and a spread line (distinct solution or clue patterns), so the lead can see whether the generator repeats itself.
"@
} elseif ($gameId -and $Task -match '^[A-Z]{2}4$') {
    # x4 cards have no card of their own; agents could not find them (CT4, 25 Nov 2026).
    $cardRules = @"
CARD LOCATION: $Task is an 'x4' card. It has no section of its own. Read the hub lane table in docs/TASKS.md (the row that starts '| x4 |')
and the lines under it. x4 = independent tests from the plan rules, with hand-made fixtures.
Owns only engine/src/test/kotlin/com/datathecodie/witgrid/engine/$gameId/spec/**. Write the tests from the plan section for this game, not from the code;
read the main code only for public function names and fixture formats.
Done when: the tests compile and run. A failure that shows a real engine bug stays failing and goes in the handoff; do not change engine code.
"@
}

$prompt = @"
You are a build agent on the Witgrid project.
Do task $Task from docs/TASKS.md, and only that task.
Read AGENTS.md first and obey it. Then read the plan sections that the task card names.
Change only the files in the card's Owns line.
$gateRules
$cardRules
Commit on the current branch with the message '${Task}: <summary>'.
End with the handoff note from AGENTS.md section 7.
$Extra
"@

if ($Agent -eq 'codex') {
    $model  = @{ L = 'gpt-6-luna'; M = 'gpt-6.2-sol'; H = 'gpt-6.2-sol' }[$Tier]
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
    $model = @{ L = 'gemini-4-hydrogen-medium'; M = 'gemini-4-hydrogen-high'; H = 'gemini-4-hydrogen-xhigh' }[$Tier]
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
} elseif ($Agent -eq 'grok') {
    $model = @{ L = 'grok-4.7-build-fast'; M = 'grok-4.6'; H = 'grok-4.7' }[$Tier]
    # acceptEdits only: bypassPermissions would let it run any command unreviewed.
    $grokPrompt = $prompt + "`nYou cannot run Gradle or git in this mode. Write the code and tests carefully, then stop. The lead compiles and commits.`n"
    $promptFile = Join-Path $logDir "$Task.prompt.md"
    Set-Content -Path $promptFile -Value $grokPrompt -Encoding UTF8
    # Headless acceptEdits still asks before Edit/Write, so allow only those two tools.
    # Remove the shell tool: a denied shell call ends the headless run before any code is written.
    # Grok writes notices to stderr; with 'Stop' PowerShell 5.1 would treat them as a fatal error.
    $ErrorActionPreference = 'Continue'
    grok --prompt-file $promptFile --cwd $wt -m $model --permission-mode acceptEdits --allow Edit --allow Write `
        --disallowed-tools run_terminal_command --disable-web-search `
        --no-memory --max-turns 200 2>&1 |
        Tee-Object -Variable agentOutput
    $ErrorActionPreference = 'Stop'
    $agentOutput | Out-File -FilePath $handoff -Encoding utf8
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
