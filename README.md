# Witgrid: Offline Logic Puzzles

An Android app with many small logic games in one place. Every game works with no connection.
Launch games: **Crowns**, **Binary**, **Nonogram** and **Mini Sudoku 6x6**.

- Product spec: [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)
- Task backlog: [docs/TASKS.md](docs/TASKS.md)
- How the AI agents build it: [docs/AGENT_WORKFLOW.md](docs/AGENT_WORKFLOW.md) and [AGENTS.md](AGENTS.md)

## Progress

_Last update: 17 Nov 2026_

**Overall: about 69 %** (44 of 64 task cards done; 1 active)

```
[############################------------]  69 %
```

The percentage is the share of task cards with status `done` in `docs/TASKS.md`. Each game stream counts as 4 cards. Cards are not equal in size, so treat the value as a rough guide.

| Phase | Cards | Done | Ready | Active | Review | Blocked | Progress |
|---|---|---|---|---|---|---|---|
| 0 — Setup | 5 | 4 | 1 | 0 | 0 | 0 | 80 % |
| 1 — Engine and puzzle bank | 26 | 25 | 1 | 0 | 0 | 0 | 96 % |
| 2 - Core app | 14 | 14 | 0 | 0 | 0 | 0 | 100 % |
| 3 - Retention | 6 | 1 | 0 | 1 | 0 | 0 | 17 % |
| 4 — Monetisation and data | 8 | 0 | 0 | 0 | 0 | 0 | 0 % |
| 5 - Closed test | 4 | 0 | 1 | 0 | 0 | 0 | 0 % |
| 6 — Launch | 1 | 0 | 0 | 0 | 0 | 0 | 0 % |
| **Total** | **64** | **44** | **3** | **1** | **0** | **0** | **69 %** |

## Done

| Card | Result |
|---|---|
| H0.2 | Developer machine ready: Android SDK (platforms 34 to 37), JDK 17, GitHub CLI, Codex CLI, Claude Code. Repository pushed to GitHub. |
| T0.1 | Gradle project with three modules: `engine` (pure Kotlin), `bankgen` (command-line tool), `app` (Android + Compose). Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, compileSdk 37, targetSdk 36, minSdk 24. The empty app builds. |
| T0.2 | GitHub Actions CI: the `build` job runs `./gradlew check :app:assembleDebug` on each push and pull request to `main`. |
| T0.3 | Ads SDK decision: Google Mobile Ads **Next-Gen SDK** (`ads-mobile-sdk` 1.6.0 or later), AdMob only at launch. See [docs/decisions/ads-sdk.md](docs/decisions/ads-sdk.md). |
| E1 | Engine contracts, deterministic SplitMix64, grid geometry, moves/history, bank-line codec and typed game stubs. `:engine:test` and `check` pass; reviewed by `gpt-6.1-sol` with no blockers. |
| A1 | Paper-and-ink themes, five colourways, eight tested region hues, Inter typography, spacing/type tokens and light/dark previews. App tests and `check` pass; `gpt-6.1-sol` approved with no blockers. The preview source compiled; a direct IDE screenshot was unavailable. |
| A2 | Room level progress and DataStore player data, with cloud/device-transfer backup rules. `:app:testDebugUnitTest` and `check` pass; Room and DataStore persistence tests pass; reviewed by `gpt-6.1-sol` with no remaining blockers. |
| A3 | Game module and bank contracts, Compose navigation and screens, serialized play/save controller, and versioned state codecs for all four engines. App, engine, and full `check` gates pass; reviewed by `gpt-6.1-sol` and human-approved. |
| A4 | Responsive board canvas, hit testing, drag interpolation, tap/long-press/two-finger undo, haptics, reduce-motion completion wave, localized cell semantics, and an interactive preview. Geometry tests and full `check` pass; reviewed with all findings fixed. |
| A5 | Crowns board with tap cycle, X drag, grouped Auto-X, optional region cues, mark/region-aware TalkBack labels, and explicit region borders. JVM gesture/undo tests and a real-touch first-bank solve test pass; reviewed by `gpt-6.1-sol`; full `check` passes. |
| A6 | Binary board with A/B tap cycling, long-press clearing, immutable givens, accessible row/column labels, and drag disabled. Emulator tests cover the first bank solve and touch behavior; reviewer fixed dense-grid text sizing; full `check` passes. |
| A7 | Nonogram board with row and column clues, Fill/X modes, drag painting, long-press clearing, and optional auto-cross. Emulator tests solve the first bank puzzle and verify drag plus dense 15×15 clues at 2× font scale; independent review and `check` pass. |
| A8 | Mini Sudoku board with cell selection, digit palette, notes mode, auto-note removal, and clear action. Strong 2×3 box separators, mode-aware TalkBack actions, solve and clear tests, and full `check` pass. |
| A9 | Home and Game screens with real progress cards, four pack entries, daily puzzle entry and Continue. Daily and streak placeholders stay localized; bank loading runs off the main thread. Seven A9 tests and full check pass; reviewed by gpt-6.1-sol with no blockers. |
| A10 | Play/Result/Level failed screens with live board input, save-safe undo/redo, game-mode controls in the lower toolbar, a hideable timer, result times and next-level navigation. Full check and seven connected tests pass; reviewed by gpt-6.1-sol with no blockers. |
| A11 | Hint starts with mistakes, otherwise shows the next logical technique and explanation; highlights are visible and accessible, and Check flags wrong cells. Five controller tests, three emulator tests and full check pass; reviewed by gpt-6.1-sol with no blockers. |
| E2 | ASCII-grid fixtures and generic engine test helpers for uniqueness, hint solving and deterministic generation. Six test-kit tests pass; `check` passes; reviewed by `gpt-6.1-sol` with no blockers. |
| CR1 | Crowns puzzle/state model, bank codec, rule checks, mistake handling and capped solution counter exposed through `GameEngine`. `:engine:test` and `check` pass; reviewed by `gpt-6.1-sol` after fixes. |
| CR2 | Crowns technique solver, deterministic hints, one-sentence explanations, grade and size-based pack bands. `:engine:test` and `check` pass; reviewed and fixed by `gpt-6.1-sol`. |
| CR3 | Seeded `SplitMix64` generator, connected flood-fill regions, unique and technique-solvable puzzles. Ten puzzles per pack pass for seed 1, including both Expert sizes; reviewed by `gpt-6.1-sol`. H1 should calibrate the immediate placements from small regions. |
| CR4 | Independent plan-rule tests for valid layouts, touch rules, solution counts, mistakes and solved-state semantics. `:engine:test` and `check` pass. |
| BI1 | Binary puzzle/state model, rules, bank codec and capped solution counter. Engine tests and check pass; reviewed by gpt-6.1-sol with exact count and invalid-line regression coverage. |
| BI2 | Binary pair-cap, gap-fill, count-completion and Hard/Expert line-comparison solver, hints and grading. Engine tests and check pass; reviewed by gpt-6.1-sol. |
| BI3 | Seeded randomized Binary grids and solver-checked clue removal. Ten puzzles per pack pass seed-1 uniqueness, technique-solving, grading and determinism checks; reviewed by gpt-6.1-sol. |
| BI4 | Independent tests cover Binary balance, triples, distinct rows and columns, solution counts, marks, mistakes and solved states. Engine tests and check pass; reviewed by gpt-6.1-sol. |
| NO1 | Nonogram puzzle/state model, clue payload, rule checks and capped solution counter. Engine tests and check pass; independently reviewed, including counter cross-checks against exhaustive enumeration. |
| NO2 | Nonogram overlap, edge and line-by-line solver, deterministic hints, line-pass grading and pack difficulty bands. Engine tests and check pass; independently reviewed and fixed for edge-run closure and one-pass difficulty cases. |
| NO3 | Seeded density-target pattern generator with clue derivation and uniqueness/grade rejection. Ten deterministic puzzles per pack plus boundary-seed cases pass; independent review found no blockers. |
| NO4 | Independent plan-rule tests cover ordered row/column runs, 0/1/2 solution counts, mistakes and solved-state semantics using hand-built fixtures. Engine tests and check pass; independent review found no blockers. |
| MS1 | Mini Sudoku 6×6 model, 2×3 box rules, bank codec, capped solution counter and immutable digit/note state. `SetDigit` clears cell notes; engine tests and check pass; independently reviewed with no blockers. |
| MS2 | Guess-free Mini Sudoku solver, one-sentence hints for all four published techniques, grade metadata and technique-based pack bands. Unique fixtures cover all techniques and a unique stuck puzzle; engine tests and check pass; reviewed by `gpt-6.1-sol` with no blockers. |
| MS3 | Seeded Mini Sudoku generator randomizes valid 6×6 grids and clue layouts, retaining unique technique-solvable puzzles in each pack. Ten seed-1 puzzles per pack are unique, solve with hints, meet their grade band, and repeat deterministically; `:engine:test` and `check` pass; reviewed by `gpt-6.1-sol` with no blockers. |
| MS4 | Independent hand-built tests cover 6×6 row, column and 2×3 box rules, capped solution counts, mistakes, notes and solved-state semantics. `:engine:test` and `check` pass. |
| B1 | `bankgen` generate, validate and stats CLI with deterministic parallel generation, compressed atomic writes, daily pack checks and validation diagnostics. Fake-engine tests and `check` pass; reviewed with no blockers. |
| B2 | Generated 150 levels per pack and 365 daily puzzles for each game: 3,860 puzzles in 20 files. A12 adds four guided intro puzzles. `validate --all` and `stats` pass; the compressed bank is 198,145 bytes and the report includes seeds, timings and histograms. Independently reviewed with no blockers. |
| B3 | `bankgen validate --all` now runs during Gradle `check`. `check` failed when a bank line was corrupted and passed after restoring the file; reviewed with no blockers. |
| CR5 | Crowns regions are at least 3 cells and level IDs use unsigned values; per-pack technique caps and size bounds are covered by tests. |
| MS5 | Mini Sudoku Easy uses 18–22 givens; level IDs use unsigned values. |
| B4 | Binary and Nonogram bank IDs use unsigned values. |
| B5 | Regenerated bank v2 and report: all 3,860 generated puzzles validate; Crowns region sizes and Mini Sudoku givens are reported. A12 intro assets bring `validate --all` to 3,864 puzzles across 24 files. `check` passes. |
| A12 | Four guided intro puzzles, localized coach lines and fresh-install Continue targeting. Intros are validated with the bank; `check` and four connected tests pass. Independently reviewed; short-screen coach clipping was fixed and layout tests pass at 1×/2× font scale. |
| A13 | Edge-to-edge with safe drawing/IME insets, progress-driven predictive back that preserves Play saves, centered tablet content, and a side-tool landscape Play layout. Phone/tablet previews cover all screens; actual Mini Sudoku and 15×15 Nonogram boards preview at 360/840/840×360 dp. `check` and Android test-source compilation pass; independently reviewed with blockers fixed. |
| A14 | Four emulator smoke journeys open each launch game from Home, Continue through its coach line, solve the intro board, and reach Result. The full connected Android test suite passes (20 tests); independently reviewed with no blockers. |
| R1 | Local 365-day puzzle index, shared daily streak, one-at-a-time Monday freeze refill, offline reconciliation and 48-hour latest-miss repair. Streak and repair metadata persist in DataStore; API-24-compatible date logic, unit tests and full `check` pass; independently reviewed with no blockers. |

Also done: the development plan, agent rules, task backlog, Claude subagents (`builder`, `test-writer`, `chore`, `reviewer`) and the helper scripts in `scripts/`.

## In progress

| R2 | Daily UI and calendar screen | Claude |

## Next steps

1. **Build R2:** daily entry and calendar UI, using R1's reconciliation API and persisted streak data.
2. **Build R3 next:** statistics screen after R2 is complete.
3. **Hand-test the bank:** H1 is ready; solve 20 puzzles of each game and pack and note any guesses or grading issues.
4. **Complete human setup:** H0.1 covers accounts, legal setup and the website.

**Phase 0 exit:** the empty app runs on a phone and CI is green. Check the first CI run on GitHub after the push. H0.1 (accounts) is the last open Phase 0 card.

## Build

```bash
./gradlew check              # all tests and lint
./gradlew :app:assembleDebug # debug APK
```

Needs JDK 17 and `ANDROID_HOME` set to the Android SDK.
