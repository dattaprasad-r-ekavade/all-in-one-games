# Witgrid: Offline Logic Puzzles

An Android app with many small logic games in one place. Every game works with no connection.
Launch games: **Crowns**, **Binary**, **Nonogram** and **Mini Sudoku 6x6**.

- Product spec: [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)
- Task backlog: [docs/TASKS.md](docs/TASKS.md)
- How the AI agents build it: [docs/AGENT_WORKFLOW.md](docs/AGENT_WORKFLOW.md) and [AGENTS.md](AGENTS.md)

## Progress

_Last update: 16 Nov 2026_

**Overall: about 11 %** (7 of 61 task cards done; no cards in progress)

```
[████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  11 %
```

The percentage is the share of task cards with status `done` in `docs/TASKS.md`. Each game stream counts as 4 cards. Cards are not equal in size, so treat the value as a rough guide.

| Phase | Cards | Done | Active | Progress |
|---|---|---|---|---|
| 0 — Setup | 5 | 4 | 0 | 80 % |
| 1 — Engine and puzzle bank | 23 | 2 | 0 | 9 % |
| 2 — Core app | 14 | 1 | 0 | 7 % |
| 3 — Retention | 6 | 0 | 0 | 0 % |
| 4 — Monetisation and data | 8 | 0 | 0 | 0 % |
| 5 — Closed test | 4 | 0 | 0 | 0 % |
| 6 — Launch | 1 | 0 | 0 | 0 % |
| **Total** | **61** | **7** | **0** | **11 %** |

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

Also done: the development plan, agent rules, task backlog, Claude subagents (`builder`, `test-writer`, `chore`, `reviewer`) and the helper scripts in `scripts/`.

## In progress

No cards are currently active.

## Next steps

1. **Start the four game-model cards** (CR1, BI1, NO1, MS1) and A2. E2's test kit is ready for independent lane tests. Keep to the workflow's three-agent concurrency limit.
2. **Wire the theme and core app flow:** A3 depends on A1 and A2; A4 follows A3. Theme wiring into the manifest and activity is part of A3.
3. **Build the puzzle bank** after the game generators and B1 are ready: 600 levels and 365 daily puzzles for each game, then validate and hand-test them.
4. **Human tasks can start now** (card H0.1): trademark check of "Witgrid", Play Console account, AdMob account, developer website with privacy policy and `app-ads.txt`, and a new export of GDD section 7.

**Phase 0 exit:** the empty app runs on a phone and CI is green. Check the first CI run on GitHub after the push. H0.1 (accounts) is the last open Phase 0 card.

## Build

```bash
./gradlew check              # all tests and lint
./gradlew :app:assembleDebug # debug APK
```

Needs JDK 17 and `ANDROID_HOME` set to the Android SDK.
