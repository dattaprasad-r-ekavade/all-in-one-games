# Witgrid: Offline Logic Puzzles

An Android app with many small logic games in one place. Every game works with no connection.
Launch games: **Crowns**, **Binary**, **Nonogram** and **Mini Sudoku 6x6**.

- Product spec: [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)
- Task backlog: [docs/TASKS.md](docs/TASKS.md)
- How the AI agents build it: [docs/AGENT_WORKFLOW.md](docs/AGENT_WORKFLOW.md) and [AGENTS.md](AGENTS.md)

## Progress

_Last update: 16 Nov 2026_

**Overall: about 36 %** (22 of 61 task cards done; 1 more in progress)

```
[##############--------------------------]  36 %
```

The percentage is the share of task cards with status `done` in `docs/TASKS.md`. Each game stream counts as 4 cards. Cards are not equal in size, so treat the value as a rough guide.

| Phase | Cards | Done | Active | Progress |
|---|---|---|---|---|
| 0 — Setup | 5 | 4 | 0 | 80 % |
| 1 — Engine and puzzle bank | 23 | 17 | 1 | 74 % |
| 2 — Core app | 14 | 1 | 0 | 7 % |
| 3 — Retention | 6 | 0 | 0 | 0 % |
| 4 — Monetisation and data | 8 | 0 | 0 | 0 % |
| 5 — Closed test | 4 | 0 | 0 | 0 % |
| 6 — Launch | 1 | 0 | 0 | 0 % |
| **Total** | **61** | **22** | **1** | **36 %** |

## Done

| Card | Result |
|---|---|
| H0.2 | Developer machine ready: Android SDK (platforms 34 to 37), JDK 17, GitHub CLI, Codex CLI, Claude Code. Repository pushed to GitHub. |
| T0.1 | Gradle project with three modules: `engine` (pure Kotlin), `bankgen` (command-line tool), `app` (Android + Compose). Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, compileSdk 37, targetSdk 36, minSdk 24. The empty app builds. |
| T0.2 | GitHub Actions CI: the `build` job runs `./gradlew check :app:assembleDebug` on each push and pull request to `main`. |
| T0.3 | Ads SDK decision: Google Mobile Ads **Next-Gen SDK** (`ads-mobile-sdk` 1.6.0 or later), AdMob only at launch. See [docs/decisions/ads-sdk.md](docs/decisions/ads-sdk.md). |
| E1 | Engine contracts, deterministic SplitMix64, grid geometry, moves/history, bank-line codec and typed game stubs. `:engine:test` and `check` pass; reviewed by `gpt-6.1-sol` with no blockers. |
| A1 | Paper-and-ink themes, five colourways, eight tested region hues, Inter typography, spacing/type tokens and light/dark previews. App tests and `check` pass; `gpt-6.1-sol` approved with no blockers. The preview source compiled; a direct IDE screenshot was unavailable. |
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

Also done: the development plan, agent rules, task backlog, Claude subagents (`builder`, `test-writer`, `chore`, `reviewer`) and the helper scripts in `scripts/`.

## In progress

| MS4 | Independent Mini Sudoku rule tests with hand-made fixtures | Claude |

## Next steps

1. **Build the Mini Sudoku engine:** MS1–MS3 are complete; MS4 is active for independent rule tests.
2. **Start independent ready work:** B1 (bank tooling), A2 (storage) and human card H0.1 (accounts and legal setup).
3. **Wire the core app flow:** A3 depends on A1 and A2; A4 follows A3. Theme wiring into the manifest and activity is part of A3.
4. **Build the puzzle bank** after all game generators and B1 are ready: 600 levels and 365 daily puzzles for each game, then validate and hand-test them.

**Phase 0 exit:** the empty app runs on a phone and CI is green. Check the first CI run on GitHub after the push. H0.1 (accounts) is the last open Phase 0 card.

## Build

```bash
./gradlew check              # all tests and lint
./gradlew :app:assembleDebug # debug APK
```

Needs JDK 17 and `ANDROID_HOME` set to the Android SDK.
