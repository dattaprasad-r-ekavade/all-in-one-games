# AGENTS.md — rules for every coding agent on Witgrid

Codex reads this file automatically. Claude Code reads it through `CLAUDE.md`.
Human contributors follow the same rules.

## 1. What the project is

Witgrid: Offline Logic Puzzles. An Android puzzle hub. All play works with no connection.

- Product spec (source of truth): `DEVELOPMENT_PLAN.md`
- Task backlog: `docs/TASKS.md`
- How tasks move from agent to merge: `docs/AGENT_WORKFLOW.md`
- The GDD PDF is old. If it differs from `DEVELOPMENT_PLAN.md`, the plan is correct.

## 2. How to do a task

1. Find your task card in `docs/TASKS.md`. Do that task only.
2. Read the plan sections that the card names. You do not need to read the full plan.
3. Change only the files and folders in the card's **Owns** line.
   - Exception: you may add test files for the code you own.
   - Exception: the card can permit a dependency in `gradle/libs.versions.toml`.
4. If you must change a file outside **Owns**, stop. Explain the need in your handoff note.
5. Run every command in the card's **Done when** list. All must pass.
6. Commit on the current branch. Message format: `<TASK-ID>: <short summary>`.
7. End with a handoff note (section 7).

Do not edit `docs/TASKS.md` or `DEVELOPMENT_PLAN.md`. The lead updates them.
Do not push, open pull requests, or merge. The lead does that.

## 3. Build and test commands

| Shell | Command form |
|---|---|
| Git Bash, Linux CI | `./gradlew <task>` |
| PowerShell | `.\gradlew.bat <task>` |

| Purpose | Command |
|---|---|
| Engine tests | `./gradlew :engine:test` |
| Bank tool tests | `./gradlew :bankgen:test` |
| Validate the committed bank | `./gradlew :bankgen:run --args="validate --all"` |
| App unit tests | `./gradlew :app:testDebugUnitTest` |
| Build the debug app | `./gradlew :app:assembleDebug` |
| All checks (gate for every task) | `./gradlew check` |

The Android SDK path comes from the `ANDROID_HOME` environment variable. Do not commit `local.properties`.

## 4. Module rules

```
engine/    Pure Kotlin/JVM. No Android imports. Family A grid engines, Family B play engines, solvers, generators, graders, hints.
bankgen/   JVM command-line tool. Builds and validates the puzzle bank with engine/.
app/       Android app. Compose UI, storage, ads, billing, analytics.
```

Package root: `com.datathecodie.witgrid`. Each game has its own package, for example `...engine.crowns` and `...app.games.crowns`.

**engine/**
- No `android.*`, `androidx.*` or Compose imports.
- Integer maths only. No `Float` or `Double` in puzzle logic.
- Random numbers come only from the project `SplitMix64`. Never use `kotlin.random.Random` or `java.util.Random`.
- The same seed must give the same output on every machine and every Kotlin version.
- Each public function has a unit test.

**bankgen/**
- Output goes to `app/src/main/assets/bank/`. The bank files are committed.
- Generation is slow and runs on the developer PC. CI runs only `validate`. GitHub CI runs only when started by hand.

**app/**
- Game rules and policy logic (ads, streaks, hints, premium) go in plain Kotlin classes with JVM unit tests. Compose code only shows state and sends events.
- Ads, billing and Firebase go behind interfaces with a fake implementation for tests.

## 5. Hard rules (never break)

1. **No ad during a puzzle.** An interstitial shows only after "Next" on the result screen (plan 8.1).
2. **Test ad units only.** Use Google's public test ad unit IDs. Never write a real AdMob app ID or ad unit ID into the repository.
3. **No secrets.** Never create, read or commit a keystore, `keystore.properties`, signing passwords or service-account keys.
4. **Name rules.** Never use these words in code strings, resources or store text: Queens, Tango, Zip, LinkedIn, NYT, Takuzu, Binairo, Picross, KenKen, Tetris, Block Blast, 2048, Flow Free, Rush Hour, Wordle, Meowdoku, Petdoku. Use the names in the plan (Pet Crowns, Binary, Nonogram, Mini Sudoku, Sudoku, Colour Sort, Path, Arrows, Klondike, Tactics). The Crowns engine id stays `crowns`.
5. **No new libraries** unless the task card permits them.
6. **Fair puzzles.** Family A bank puzzles have exactly one solution and the technique solver solves them with no guess. Family B levels are winnable (or have a legal fail); uniqueness is not required (plan 6.5, 7.3).
7. **Offline first.** No feature may block play when there is no connection.

## 6. Code style

- Kotlin official code style. Four spaces. No wildcard imports.
- Small files: one main class or one group of related functions in each file.
- Names: say what a thing is. No abbreviations except `rng`, `id`, `ui`.
- Comments explain *why*, not *what*.
- User-visible text goes in `strings.xml`, not in code. Exception: hint explanation templates in `engine/`.
- Do not leave `TODO` comments. Put open points in the handoff note.

## 7. Handoff note

End every task with this note, in this format:

```
## Handoff: <TASK-ID>
Status: done | blocked
Changed: <files or folders>
Commands run: <command> -> <pass/fail>
Decisions: <choices you made that the spec did not fix>
Open questions: <items for the lead, or "none">
```
