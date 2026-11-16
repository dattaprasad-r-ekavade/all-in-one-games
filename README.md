# Witgrid: Offline Logic Puzzles

An Android app with many small logic games in one place. Every game works with no connection.
Launch games: **Crowns**, **Binary**, **Nonogram** and **Mini Sudoku 6x6**.

- Product spec: [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md)
- Task backlog: [docs/TASKS.md](docs/TASKS.md)
- How the AI agents build it: [docs/AGENT_WORKFLOW.md](docs/AGENT_WORKFLOW.md) and [AGENTS.md](AGENTS.md)

## Progress

_Last update: 16 Nov 2026_

**Overall: about 5 %** (3 of 61 task cards done; 3 more in progress)

```
[██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  5 %
```

The percentage is the share of task cards with status `done` in `docs/TASKS.md`. Each game stream counts as 4 cards. Cards are not equal in size, so treat the value as a rough guide.

| Phase | Cards | Done | Active | Progress |
|---|---|---|---|---|
| 0 — Setup | 5 | 3 | 1 | 60 % |
| 1 — Engine and puzzle bank | 23 | 0 | 1 | 0 % |
| 2 — Core app | 14 | 0 | 1 | 0 % |
| 3 — Retention | 6 | 0 | 0 | 0 % |
| 4 — Monetisation and data | 8 | 0 | 0 | 0 % |
| 5 — Closed test | 4 | 0 | 0 | 0 % |
| 6 — Launch | 1 | 0 | 0 | 0 % |
| **Total** | **61** | **3** | **3** | **5 %** |

## Done

| Card | Result |
|---|---|
| H0.2 | Developer machine ready: Android SDK (platforms 34 to 37), JDK 17, GitHub CLI, Codex CLI, Claude Code. Repository pushed to GitHub. |
| T0.1 | Gradle project with three modules: `engine` (pure Kotlin), `bankgen` (command-line tool), `app` (Android + Compose). Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, compileSdk 37, targetSdk 36, minSdk 24. The empty app builds. |
| T0.3 | Ads SDK decision: Google Mobile Ads **Next-Gen SDK** (`ads-mobile-sdk` 1.6.0 or later), AdMob only at launch. See [docs/decisions/ads-sdk.md](docs/decisions/ads-sdk.md). |

Also done: the development plan, agent rules, task backlog, Claude subagents (`builder`, `test-writer`, `chore`, `reviewer`) and the helper scripts in `scripts/`.

## In progress

| Card | Work | Agent |
|---|---|---|
| T0.2 | GitHub Actions CI (`build` job) | Codex `gpt-6-luna` |
| E1 | Engine core contracts: PRNG, grid, moves, undo, `GameEngine` interface, bank line format | Codex `gpt-6.1-sol` (high) |
| A1 | Theme: paper and ink, dark mode, 5 colourways, 8 region colours, Inter font | Claude `builder` (Sonnet) |

## Next steps

1. **Merge the three active cards.** Each one gets a review from the other agent family. You read the E1 interfaces before merge.
2. **Start the four game streams** (Crowns, Binary, Nonogram, Mini Sudoku) in parallel after E1. Also start E2 (engine test kit) and A2 (storage).
3. **Build the puzzle bank** (B1 to B3): 600 levels and 365 daily puzzles for each game.
4. **Human tasks to start now** (card H0.1): trademark check of "Witgrid", Play Console account, AdMob account, developer website with privacy policy and `app-ads.txt`, and a new export of GDD section 7.

**Phase 0 exit:** the empty app runs on a phone and CI is green. This needs T0.2.

## Build

```bash
./gradlew check              # all tests and lint
./gradlew :app:assembleDebug # debug APK
```

Needs JDK 17 and `ANDROID_HOME` set to the Android SDK.
