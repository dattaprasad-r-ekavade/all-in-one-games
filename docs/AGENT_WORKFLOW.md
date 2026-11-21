# Agent workflow

This file tells the lead (you, with the main Claude Code session) how to run the build with AI agents.

## 1. Roles

| Role | Who | Job |
|---|---|---|
| Lead | You + main Claude Code session | Pick tasks, start agents, read handoff notes, run reviews, merge, update `docs/TASKS.md` |
| Builder | Codex or Claude subagent | Do one task card in one worktree |
| Test writer | Haiku or `gpt-6-luna` | Write tests from the spec, not from the code |
| Reviewer | The *other* agent family | Review the branch before merge |
| Human | You only | Accounts, keys, live ad IDs, hand tests, art approval, store upload (cards with tier **Human**) |

## 2. Model tiers

Each task card has a tier. The tier selects the model.

| Tier | Work type | Claude | Codex | Gemini (`agy`) |
|---|---|---|---|---|
| **L** light | Tests from spec, CI YAML, strings, wiring, running `bankgen`, reports, drafts | Haiku 5.5 (`chore`, `test-writer`) | `gpt-6-luna`, effort medium | `gemini-3.8-flash-medium` |
| **M** standard | Screens, storage, boards, normal features | Sonnet 5.5 (`builder`) | `gpt-6.1-sol`, effort medium | `gemini-3.8-flash-high` |
| **H** hard | Contracts, solvers, generators, gestures, consent, billing | Sonnet 5.5 (`builder`) | `gpt-6.1-sol`, effort high | Not recommended (Flash only) |
| Escalation | A task that failed its gate two times | Opus 5.5 | `gpt-6-astra` or `gpt-6.1-sol` effort xhigh | — |

Your Codex default is `gpt-6-luna` at `xhigh`. That is a poor fit for most cards. The scripts always set the model and effort explicitly.

**Split of work between the two families (default)**
- Codex builds most of `engine/` and `bankgen/` (algorithm code with a clear test oracle).
- Claude builds most of `app/` (Compose, Android APIs, UX details).
- The test cards for each engine lane go to the other family. Then the tests and the code come from two independent readings of the spec.
- The reviewer is always from the family that did not build the task.

**Gemini (added 21 Nov 2026).** A third family through the Antigravity CLI (`agy`). It reads `AGENTS.md` by itself. Use it for L and M cards when Codex or Claude is busy: `.\scriptsgent-task.ps1 -Task FU1 -Agent gemini -Tier M`. Codex or Claude reviews its work (`review-task.ps1 -Reviewer codex`). `--mode auto` is not available on this account; the default mode runs edits and shell commands in print mode.

**Quota (22 Nov 2026).** Five or six parallel Gemini runs used the individual Gemini quota in about 4 hours (error 429). Run 2 or 3 Gemini agents at a time. Move unfinished Gemini work to Codex on the same worktree (`agent-task.ps1 -Task <ID> -Agent codex` keeps the uncommitted files).

**Package tests for every agent (22 Nov 2026).** All build agents (Codex, Claude and Gemini) now run only their own package tests by default. Use `-RunGradle` for the full 'Done when' commands. Codex builds use `gpt-6.2-sol` for M and H cards.

**Batch gate for Gemini (21 Nov 2026).** Parallel Gradle runs slow the PC too much. Gemini runs do not run the full suite by default. They write code and tests, run only the tests of their own package (for example `:engine:test --tests 'com.datathecodie.witgrid.engine.mosaic.*'`), fix every failure without weakening a test, commit, and hand off. The lead then tests the finished branches together: `.\scripts\check-batch.ps1 -Tasks FU1,SK1,MO1`. It merges them into `..\witgrid-wt\_batch` and runs `gradlew check` one time. Errors go back to the builder with `-Extra`. Use `-RunGradle` to let one Gemini run test its own work.

The **Build** column in `docs/TASKS.md` shows the default for each card. You can swap families when one quota is low. Keep the tier.

## 3. One-time setup

Do these steps before the first task. Each step is also task **H0.2** in the backlog.

1. Set `ANDROID_HOME` for your user. All worktrees then find the SDK.
   ```powershell
   [Environment]::SetEnvironmentVariable('ANDROID_HOME', "$env:LOCALAPPDATA\Android\Sdk", 'User')
   ```
2. Log in to GitHub CLI: `gh auth login`.
3. Make the first commit and push `main`:
   ```powershell
   git add .
   git commit -m "Plan, agent rules and task backlog"
   git push -u origin main
   ```
4. Open Codex one time in the repo (`codex`) and accept "trust this folder".
5. Optional: on GitHub, protect `main` (require pull request and the CI check from task **T0.2**).

## 4. The task loop

Do these steps for each task.

1. **Pick.** Open `docs/TASKS.md`. Pick a card with status `ready` whose **Needs** are all `done`. Set it to `active`.
2. **Start.** Run the builder:
   ```powershell
   .\scripts\agent-task.ps1 -Task CR2 -Agent codex -Tier H
   ```
   Or, from the main Claude Code session: "Start the builder agent on task A3 in a worktree."
3. **Read the handoff** in `..\witgrid-wt\_logs\<Task>.md`. If the status is `blocked`, answer the question, then run the same command again with `-Extra "<your answer>"`.
4. **Review** with the other family:
   ```powershell
   .\scripts\review-task.ps1 -Task CR2 -Reviewer claude
   ```
5. **Fix.** If the verdict is "fix first", send the blockers back to the builder (`-Extra "Fix these review blockers: ..."`).
6. **Merge.**
   ```powershell
   cd ..\witgrid-wt\CR2
   git push -u origin task/CR2
   gh pr create --fill
   gh pr merge --squash --delete-branch
   cd "..\..\all-in-one games"
   git pull
   git worktree remove ..\witgrid-wt\CR2
   ```
7. **Update** the card status to `done` in `docs/TASKS.md`. Commit on `main`.

## 5. Parallel work

- Run **3 agents at the same time or fewer**. Your review time is the limit, not the agents.
- Tasks in parallel must not share files. The **Owns** line on each card makes this possible.
- The best parallel lanes: the four game lanes in phase 1 (CR, BI, NO, MS) and the four boards in phase 2 (A5 to A8).
- Never run two agents in the same worktree.

## 6. Rules for the lead

- **Gate before merge.** `./gradlew check` passes on the branch. GitHub CI runs only on request (`gh workflow run CI --ref main`), for example before a release.
- **Contract tasks get a human read.** E1 and A3 define the interfaces that every later card depends on. Read them yourself before merge.
- **Small cards.** If a builder fails two times, split the card or escalate one tier. Do not keep retrying.
- **Spec changes go to the plan first.** If a review shows the plan is wrong, change `DEVELOPMENT_PLAN.md` on `main`. Then restart the task.
- **Do not paste secrets into prompts.** Agents never need keys, live ad IDs or passwords.

## 7. Known setup risks

| Risk | Sign | Fix |
|---|---|---|
| Codex sandbox blocks Gradle downloads | `Could not resolve` or network errors in Codex runs | The script sets `sandbox_workspace_write.network_access=true` **[Verify]** on your Codex version. Or run `./gradlew build` one time yourself to fill the cache. |
| Codex sandbox cannot write the Gradle cache | Errors under `~/.gradle` | The script adds `~/.gradle` with `--add-dir`. |
| Claude headless run asks for permission and stops | The handoff ends early | Commands in `.claude/settings.json` are pre-allowed. Add a missing safe command there. |
| Gradle daemons use too much memory with 3 worktrees | PC slows down | Run `./gradlew --stop` between tasks, or run fewer agents. |
| AGP needs a newer JDK | Build error on JDK version at T0.1 | Install the JDK version that AGP 9.x needs **[Verify]**. You have JDK 17 now. |
