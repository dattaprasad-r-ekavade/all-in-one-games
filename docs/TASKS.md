# Witgrid task backlog

Each card is one unit of work for one agent in one worktree. The lead updates this file. Agents never edit it.

- **Tier**: L, M, H or Human. See `docs/AGENT_WORKFLOW.md` section 2.
- **Build / Review**: the default agent family. `Codex` = Codex CLI, `Claude` = Claude Code subagent. You can swap the families. Keep the reviewer from the other family.
- **Status**: `todo` → `ready` (all needs done) → `active` → `review` → `done`. `blocked` if waiting on a person.
- Path short forms: `engine/…` = `engine/src/{main,test}/kotlin/com/datathecodie/witgrid/engine/…`; `app/…` = `app/src/{main,test,androidTest}/kotlin/com/datathecodie/witgrid/app/…`; `bankgen/…` = `bankgen/src/{main,test}/kotlin/com/datathecodie/witgrid/bankgen/…`.

## Index

| ID | Title | Tier | Build | Review | Needs | Status |
|---|---|---|---|---|---|---|
| **Phase 0 — Setup** |||||||
| H0.1 | Accounts, legal, website | Human | — | — | — | ready |
| H0.2 | Developer machine and repo setup | Human | — | — | — | done |
| T0.1 | Gradle project with three modules | M | Claude | Codex | H0.2 | done |
| T0.2 | GitHub Actions CI | L | Codex | Claude | T0.1 | active |
| T0.3 | Ads SDK decision memo | M | Claude | — (human decides) | — | done (Next-Gen) |
| **Phase 1 — Engine and puzzle bank** |||||||
| E1 | Engine core contracts | H | Codex | Claude + human | T0.1 | active |
| E2 | Engine test kit | L | Claude | Codex | E1 | todo |
| CR1–CR4 | Crowns lane | see lane table | | | E1 | todo |
| BI1–BI4 | Binary lane | see lane table | | | E1 | todo |
| NO1–NO4 | Nonogram lane | see lane table | | | E1 | todo |
| MS1–MS4 | Mini Sudoku lane | see lane table | | | E1 | todo |
| B1 | `bankgen` command-line tool | M | Codex | Claude | E1 | todo |
| B2 | Generate the launch bank and report | L | Codex | Claude | B1, CR3, BI3, NO3, MS3 | todo |
| B3 | Bank validation in `check` and CI | L | Claude | Codex | B2, T0.2 | todo |
| H1 | Hand-test the bank | Human | — | — | B2 | todo |
| **Phase 2 — Core app** |||||||
| A1 | Theme and design tokens | M | Claude | Codex | T0.1 | active |
| A2 | Storage and saves | M | Codex | Claude | E1 | todo |
| A3 | App contracts, navigation, play controller | H | Claude | Codex + human | A1, A2 | todo |
| A4 | Board canvas kit and gestures | H | Claude | Codex | A3 | todo |
| A5 | Crowns board | M | Claude | Codex | A4, CR2 | todo |
| A6 | Binary board | M | Codex | Claude | A4, BI2 | todo |
| A7 | Nonogram board | M | Claude | Codex | A4, NO2 | todo |
| A8 | Mini Sudoku board | M | Codex | Claude | A4, MS2 | todo |
| A9 | Home and Game screens | M | Claude | Codex | A3 | todo |
| A10 | Play toolbar, Result and Level failed screens | M | Claude | Codex | A3, A4 | todo |
| A11 | Hint and check UI | M | Codex | Claude | A10 | todo |
| A12 | Guided first level for each game | L | Claude | Codex | A5, A6, A7, A8 | todo |
| A13 | Edge-to-edge, predictive back, tablet layout | M | Codex | Claude | A9, A10 | todo |
| A14 | UI smoke tests | L | Codex | Claude | A5–A8, A10, B2 | todo |
| **Phase 3 — Retention** |||||||
| R1 | Daily, streak and freeze logic | M | Codex | Claude | A2 | todo |
| R2 | Daily UI and calendar screen | M | Claude | Codex | R1, A9 | todo |
| R3 | Statistics screen | L | Claude | Codex | A2, A9 | todo |
| R4 | Reminder notification | L | Codex | Claude | A3 | todo |
| R5 | Settings screen | L | Claude | Codex | A1, A3 | todo |
| R6 | Accessibility pass | M | Claude | Codex | A5–A8, R5 | todo |
| **Phase 4 — Monetisation and data** |||||||
| M1 | Monetisation policy logic | H | Codex | Claude | A2 | todo |
| M2 | Policy tests from the spec | L | Claude | Codex | M1 | todo |
| M3 | Consent and ad service (test units) | H | Claude | Codex | M1 | todo |
| M4 | Ads in the player flows | M | Claude | Codex | M3, A10, A11, R2 | todo |
| M5 | Mistake limit and continue flow | M | Codex | Claude | M1, A10 | todo |
| M6 | Premium purchase and restore | H | Claude | Codex | M1, R5 | todo |
| H4 | Firebase project, AdMob units, licence tester | Human | — | — | H0.1 | todo |
| M7 | Analytics, Crashlytics, Remote Config | M | Codex | Claude | M1, H4 | todo |
| **Phase 5 — Closed test** |||||||
| S1 | Store listing and policy form drafts | M | Claude | Codex | A9 | todo |
| S2 | Screenshot capture script | M | Codex | Claude | A14 | todo |
| S3 | Release build configuration | M | Codex | Claude | M3, M6, M7 | todo |
| H5 | Art, keystore, closed test, production access | Human | — | — | S1, S2, S3 | todo |
| **Phase 6 — Launch** |||||||
| H6 | Live ad units, staged rollout, weekly review | Human | — | — | H5 | todo |

---

## Phase 0 — Setup

### H0.1 — Accounts, legal, website (Human)
Plan: 11 (phase 0), 8.7, 15.
- [ ] Check "Witgrid" in USPTO, EUIPO and UKIPO.
- [ ] Open the Play Console account. Complete developer verification. Check which personal details Play shows.
- [ ] Open the AdMob account. Do **not** create live ad units in code yet.
- [ ] Make the developer website: privacy policy page and `app-ads.txt`.
- [ ] Export GDD section 7 (ASO, launch, closed test) again. The PDF in the repo stops after section 6.

### H0.2 — Developer machine and repo setup (Human)
Do the steps in `docs/AGENT_WORKFLOW.md` section 3.
- [ ] `ANDROID_HOME` set. `gh auth login` done. First commit pushed. Codex trusts the folder.

### T0.1 — Gradle project with three modules
Tier M · Build Claude (`builder`) · Review Codex
Plan: 7.1, 7.2.
**Owns:** `settings.gradle.kts`, `build.gradle.kts`, `gradle/**`, `gradlew*`, `gradle.properties`, `engine/build.gradle.kts`, `bankgen/build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, one placeholder source file in each module.
**Do:**
1. Check the newest stable versions of Kotlin, AGP, Gradle, Compose BOM. Use them. Write the versions you chose in the handoff.
2. Make a version catalog `gradle/libs.versions.toml`.
3. `engine`: Kotlin JVM library, JUnit Jupiter (newest stable; JUnit 6 is accepted). `bankgen`: Kotlin JVM application that depends on `engine`. `app`: Android application with Compose, `applicationId = "com.datathecodie.witgrid"`, minSdk 24, targetSdk 36, compileSdk as the Compose BOM needs (37 for BOM 2026.09).
4. The app shows one Compose screen with the text "Witgrid".
5. Configure JVM toolchain to the JDK version that the chosen AGP needs.
**Done when:** `./gradlew check` and `./gradlew :app:assembleDebug` pass. `engine` has no Android dependency.

### T0.2 — GitHub Actions CI
Tier L · Build Codex (`gpt-6-luna`) · Review Claude
**Owns:** `.github/workflows/ci.yml`.
**Do:** On push and pull request to `main`: set up JDK, Gradle cache, Android SDK; run `./gradlew check :app:assembleDebug`. Upload test reports when a job fails.
**Done when:** The YAML is valid (`actionlint` if available). The lead confirms a green run after merge.

### T0.3 — Ads SDK decision memo
Tier M · Build Claude (`builder`) · Human decides
Plan: 7.1 "Open choice", 8.3, 12 (mediation note).
**Owns:** `docs/decisions/ads-sdk.md`.
**Do:** Compare `play-services-ads` (Legacy) with the Next-Gen `ads-mobile-sdk`: status, UMP support, rewarded and interstitial APIs, offline preload, mediation adapters for later. Give one recommendation with reasons. Mark facts with source links and the date.
**Done when:** The memo exists. The lead records the decision in plan section 15.

---

## Phase 1 — Engine and puzzle bank

### E1 — Engine core contracts
Tier H · Build Codex (`gpt-6.1-sol`, high) · Review Claude + human read
Plan: 6.5, 7.2, 7.3, 7.4, 7.6.
**Owns:** `engine/…/core/**`, `engine/…/Registry.kt`, stub files `engine/…/{crowns,binary,nonogram,minisudoku}/*Engine.kt`.
**Do:**
1. `SplitMix64` with tests against the published reference output.
2. `Grid` (size, cell index, row, column, 8 neighbours), `Cell`, `Move`, `History` (undo and redo).
3. `Pack` (EASY, MEDIUM, HARD, EXPERT), `Technique` (id, display name, difficulty rank), `HintStep` (technique, cells, explanation, moves to apply), `Grade` (hardest technique, step count, pack).
4. `GameEngine<P, S>` interface — the pure-Kotlin half of plan 7.3:
   `id`, `decodePuzzle(line)`, `encodePuzzle(p)`, `initialState(p)`, `applyMove(s, m)`, `isSolved(p, s)`, `mistakes(p, s)`, `countSolutions(p, limit = 2)`, `nextStep(p, s)`, `grade(p)`, `generate(rng, pack)`, `encodeState(s)`, `decodeState(bytes)`.
5. `BankLine` codec for `id|size|payload|solution|pack|grade`.
6. `Registry` lists four stub engines. Each stub throws `NotImplementedError` with the lane ID.
**Done when:** `./gradlew :engine:test` passes. Tests cover the PRNG, the grid, history and the codec. The human has read the interfaces.

### E2 — Engine test kit
Tier L · Build Claude (`test-writer`) · Review Codex
**Owns:** `engine/src/test/kotlin/…/testkit/**`.
**Do:** A text fixture format (ASCII grid in a Kotlin raw string) and a parser. Helpers: `assertUnique(engine, puzzle)`, `assertTechniqueSolves(engine, puzzle)`, `assertDeterministic(engine, seed, pack)`, `solveWithHints(engine, puzzle)` (applies `nextStep` until solved).
**Done when:** `./gradlew :engine:test` passes. Each helper has one test with a fake engine.

### Game lanes (CR, BI, NO, MS)

Each launch game is one lane of four cards. The lanes are independent. Run them in parallel.

| Card | Work | Tier | Build | Review | Needs |
|---|---|---|---|---|---|
| x1 | Rules, puzzle and state model, bank payload format, `countSolutions` (brute force with a cap of 2) | M | Codex `gpt-6.1-sol` | Claude | E1 |
| x2 | Technique solver, `grade`, `nextStep` with explanation text, pack grade bands | H | Codex `gpt-6.1-sol` high | Claude | x1 |
| x3 | Generator for each pack | see table | Codex | Claude | x2 |
| x4 | Independent tests from the plan rules, with hand-made fixtures | L | Claude `test-writer` | Codex | x2, E2 |

**Owns (all four cards):** `engine/…/<game>/**` and its tests. x4 owns only `engine/src/test/kotlin/…/<game>/spec/**`.

| Lane | Game package | Plan | Sizes | x2 techniques | x3 method | x3 tier |
|---|---|---|---|---|---|---|
| CR | `crowns` | 6.1 | 6–10 | Region confined to one line, touch exclusion, k regions in k lines. No look-ahead. | Crown layout, random flood-fill regions, reject unless unique and technique-solvable | H |
| BI | `binary` | 6.2 | 6, 8, 10, 12 | Pair cap, gap fill, count completion, row/column comparison (Hard, Expert) | Full valid grid, remove givens while the technique solver still solves | M |
| NO | `nonogram` | 6.3 | 5, 10, 15 | Overlap, edge rules, line-by-line solve. Grade = line passes + hardest rule. | Random pattern at a density target, clues, accept only if the line solver solves | H |
| MS | `minisudoku` | 6.4 | 6x6, 2x3 boxes | Naked single, hidden single, locked candidates, naked pair | Full valid grid, remove givens while unique and technique-solvable | M |

**Done when (each card):** `./gradlew :engine:test` passes. In addition:
- x1: `countSolutions` returns 0, 1 and 2 on hand fixtures.
- x2: `solveWithHints` solves all x1 fixtures. Every `HintStep` has a non-empty explanation of one sentence.
- x3: For each pack, seed 1 gives 10 puzzles. Each is unique, solved by the technique solver, and graded inside its pack band. The same seed gives the same puzzles on a second run.
- x4: Tests compile and run. Failures that show real bugs are listed in the handoff, not fixed.

### B1 — `bankgen` command-line tool
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 6, 7.2, 7.4.
**Owns:** `bankgen/**`.
**Do:** Three commands:
- `generate --game <id> --pack <p> --count <n> --seed <s>` writes `app/src/main/assets/bank/<game>-<pack>.txt.gz`. Option `--daily` writes `<game>-daily.txt.gz` (365 puzzles, mixed packs).
- `validate --all` reads each bank file. It checks: unique solution (`countSolutions`), technique-solvable, grade inside pack band, no duplicate puzzle, IDs stable. Exit code 1 on any failure.
- `stats` prints a grade histogram for each file.
Use worker threads for `generate`. Show progress.
**Done when:** `./gradlew :bankgen:test` passes with a fake engine. `validate` fails on a corrupted test file.

### B2 — Generate the launch bank and report
Tier L · Build Codex (`gpt-6-luna`) · Review Claude
Plan: 6 (counts).
**Owns:** `app/src/main/assets/bank/**`, `docs/reports/bank-v1.md`.
**Do:** Generate 150 levels per pack (4 packs) and 365 daily puzzles for each of the 4 games. Fixed seeds; write the seeds in the report. Run `validate --all` and `stats`. The report lists counts, file sizes, generation times and histograms.
**Done when:** `validate --all` passes. The total bank is smaller than 1 MB, or the report explains why not.

### B3 — Bank validation in `check` and CI
Tier L · Build Claude (`chore`) · Review Codex
**Owns:** `bankgen/build.gradle.kts`, `bankgen/src/test/**`.
**Do:** Make `./gradlew check` run the bank validation on the committed files.
**Done when:** `./gradlew check` fails when one bank line is broken by hand, and passes again when it is restored.

### H1 — Hand-test the bank (Human)
Solve 20 puzzles of each game and pack on the PC or the first app build. Note the time and if any step needed a guess. Make bug cards for bad grades.

---

## Phase 2 — Core app

### A1 — Theme and design tokens
Tier M · Build Claude (`builder`) · Review Codex
Plan: 4 (art table).
**Owns:** `app/…/ui/theme/**`, `app/src/main/res/font/**`, `app/src/main/res/values*/colors.xml`, `themes.xml`.
**Do:** Paper and ink light theme, true-black dark theme, follows the system. 5 colourways. 8 region hues with equal lightness steps, each with a letter and a hatch pattern ID. Inter font (OFL) with tabular numerals. Spacing and type scale tokens.
**Done when:** `./gradlew :app:testDebugUnitTest` passes. A Compose preview screen shows all tokens in light and dark. The handoff lists the region colours with their contrast values.

### A2 — Storage and saves
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 7.1 (storage, backup), 7.5.
**Owns:** `app/…/data/**`, `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`.
**Do:** Room tables for level progress (state bytes, elapsed time, hints used, completed, best time). DataStore for settings, streak, freeze count, Premium flag, reward balances. A `ProgressRepository` interface. Auto Backup rules that include both.
**Done when:** `./gradlew :app:testDebugUnitTest` passes with Room in-memory tests. A save survives a repository re-creation.

### A3 — App contracts, navigation, play controller
Tier H · Build Claude (`builder`) · Review Codex + human read
Plan: 7.3, 9.
**Owns:** `app/…/games/GameModule.kt`, `app/…/games/GameRegistry.kt`, `app/…/play/PlayController.kt`, `app/…/nav/**`, stub screens in `app/…/ui/screens/**`.
**Do:**
1. App-side `GameModule`: wraps one `GameEngine`, adds `levelsPerInterstitial` and `@Composable Board(state, onMove)`.
2. `PlayController` (plain Kotlin, no Compose): loads a level from the bank, applies moves, undo and redo, timer that pauses in the background, saves after each move through `ProgressRepository`, emits a solved event.
3. Navigation: Home → Game → Play → Result / Level failed; Calendar, Statistics, Settings. Stub screens show their name.
**Done when:** `./gradlew check` passes. `PlayController` has JVM tests with a fake engine and a fake repository. The human has read the interfaces.

### A4 — Board canvas kit and gestures
Tier H · Build Claude (`builder`) · Review Codex
Plan: 4 (layout, gestures, feedback).
**Owns:** `app/…/ui/board/**`.
**Do:** A reusable Compose Canvas grid: cell hit-testing, tap, drag-paint path, long-press, two-finger tap. Haptic tick (light and strong), haptics switch. Completion wave animation with reduce-motion support. Grid fills width with 16 dp margins.
**Done when:** `./gradlew check` passes. Hit-testing and drag-path logic have JVM tests. A demo board in a preview responds to all gestures.

### A5–A8 — Game boards
Tier M · Build per index · Review the other family
Plan: section 6 input row for the game.
**Owns:** `app/…/games/<game>/**`.
**Do:** Implement the `GameModule` and `Board` for the game with the A4 kit. Use the input rules from the plan. Draw region letters/patterns when the setting is on. Register the module in `GameRegistry` (one line).
**Done when:** `./gradlew check` passes. A Compose UI test plays the first bank puzzle to the solved event by applying the solution moves.

### A9 — Home and Game screens
Tier M · Build Claude (`builder`) · Review Codex
Plan: 9 (Home, Game).
**Owns:** `app/…/ui/screens/home/**`, `app/…/ui/screens/game/**`.
**Do:** Home: game cards with progress, daily state placeholder, streak placeholder, one quiet Premium row (no action yet). Game: four packs with progress, daily puzzle entry, "Continue".
**Done when:** `./gradlew check` passes. ViewModels have JVM tests.

### A10 — Play toolbar, Result and Level failed screens
Tier M · Build Claude (`builder`) · Review Codex
Plan: 4 (layout, timer), 9 (Play, Result, Level failed).
**Owns:** `app/…/ui/screens/play/**`, `app/…/ui/screens/result/**`, `app/…/ui/screens/failed/**`.
**Do:** Tools in the bottom third: undo, redo, mark mode, hint, check, timer (hideable). Nothing tappable in the top 15 %. Result: time, best time, "Next". Level failed: "Continue" and "Restart" (Continue is a placeholder until M5).
**Done when:** `./gradlew check` passes.

### A11 — Hint and check UI
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 7.6, 8.2 (allowances come later in M4).
**Owns:** `app/…/ui/screens/play/hint/**`, `app/…/play/HintController.kt`.
**Do:** Hint: mistakes first, else `nextStep`; highlight cells, technique name, one-sentence explanation, "Apply" button. Check: flag wrong cells. Use an `Allowance` interface with an "always allow" stub (M4 replaces it).
**Done when:** `./gradlew check` passes. `HintController` has JVM tests.

### A12 — Guided first level for each game
Tier L · Build Claude (`chore`) · Review Codex
Plan: 6.5 (first level), GDD onboarding row.
**Owns:** `app/src/main/assets/bank/*-intro.txt`, `app/…/games/*/Intro.kt`, intro strings.
**Do:** One small hand-made puzzle per game with one coach line each. Validate them with `bankgen validate`.
**Done when:** `./gradlew check` passes. A fresh install starts each game on its intro level.

### A13 — Edge-to-edge, predictive back, tablet layout
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 7.1 (Play rules).
**Owns:** `app/…/MainActivity.kt`, `app/…/ui/layout/**`, window-size handling in screen files (list them in the handoff).
**Done when:** `./gradlew check` passes. Screens work at 360 dp phone and 840 dp tablet widths in previews.

### A14 — UI smoke tests
Tier L · Build Codex (`gpt-6-luna`) · Review Claude
**Owns:** `app/src/androidTest/**/smoke/**`.
**Do:** One instrumented test for each game: open the app, open the game, play level 1 to the result screen.
**Done when:** `./gradlew :app:connectedDebugAndroidTest` passes on an emulator (the lead runs it if the agent has no emulator).

---

## Phase 3 — Retention

### R1 — Daily, streak and freeze logic
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 6 (daily index), 5, 7.5, 8.2 (streak repair).
**Owns:** `app/…/domain/daily/**`.
**Do:** Plain Kotlin. Daily index = days since a fixed date mod 365. Streak rules, one free freeze each week applied automatically, repair within 48 hours (the reward hook comes in M4). Use an injected clock and time zone.
**Done when:** JVM tests cover: day change at midnight, time zone change, a missed day with and without a freeze, a 7-day offline streak, clock moved backwards.

### R2 — Daily UI and calendar screen
Tier M · Build Claude (`builder`) · Review Codex
**Owns:** `app/…/ui/screens/calendar/**`, daily parts of Home/Game screens (list in handoff).
**Done when:** `./gradlew check` passes.

### R3 — Statistics screen
Tier L · Build Claude (`chore`) · Review Codex
Plan: 9 (Statistics).
**Owns:** `app/…/ui/screens/stats/**`.
**Done when:** `./gradlew check` passes. The stats calculation has JVM tests.

### R4 — Reminder notification
Tier L · Build Codex (`gpt-6-luna`) · Review Claude
Plan: 5 (one optional reminder).
**Owns:** `app/…/reminder/**`, manifest entries (list in handoff).
**Do:** Off by default. Player selects the time. Ask notification permission (API 33+) only when the player turns it on.
**Done when:** `./gradlew check` passes.

### R5 — Settings screen
Tier L · Build Claude (`chore`) · Review Codex
Plan: 9 (Settings).
**Owns:** `app/…/ui/screens/settings/**`.
**Do:** Theme, colourway, assists, mistake limit, haptics, sound, reminder, Premium row and restore placeholders, privacy link, licences.
**Done when:** `./gradlew check` passes.

### R6 — Accessibility pass
Tier M · Build Claude (`builder`) · Review Codex
Plan: 4 (accessibility).
**Owns:** accessibility changes in board and screen files (list in handoff).
**Do:** TalkBack label for each cell, font scaling to 200 %, reduce-motion, colour-blind letters and patterns, touch targets ≥ 48 dp.
**Done when:** `./gradlew check` passes. The handoff lists each screen and its result.

---

## Phase 4 — Monetisation and data

### M1 — Monetisation policy logic
Tier H · Build Codex (`gpt-6.1-sol`, high) · Review Claude
Plan: 8.1–8.6.
**Owns:** `app/…/domain/monetisation/**`, `app/…/domain/config/**`.
**Do:** Plain Kotlin, no SDK code.
- `InterstitialPolicy.shouldShow(...)` with every condition in 8.1.
- `AllowanceLedger`: free hints each day, checks, continues, streak repairs, offline extra hints; Premium makes all unlimited. Rewards are stored before the callback returns.
- `ConfigKeys` with every key and default in 8.6, behind a `Config` interface.
**Done when:** `./gradlew :app:testDebugUnitTest` passes.

### M2 — Policy tests from the spec
Tier L · Build Claude (`test-writer`) · Review Codex
**Owns:** `app/src/test/**/monetisation/spec/**`.
**Do:** One test for each rule and each "Never" line in 8.1–8.5. Read only the plan and M1's public signatures.
**Done when:** Tests run. The handoff lists any failures as bugs.

### M3 — Consent and ad service (test units)
Tier H · Build Claude (`builder`) · Review Codex
Plan: 7.1, 8.3, 8.7, decision from T0.3.
**Owns:** `app/…/ads/**`, ads entries in `app/build.gradle.kts` and manifest.
**Do:** `AdService` interface with a real implementation and a fake. UMP consent before the first ad request. Preload one interstitial and one rewarded ad when online; drop ads older than one hour. Google's public test app ID and test unit IDs only. Live IDs come later from Gradle properties outside the repo (S3).
**Done when:** `./gradlew check` passes. The lead sees test ads on a device.

### M4 — Ads in the player flows
Tier M · Build Claude (`builder`) · Review Codex
Plan: 8.1–8.3, 8.5 (offer placement).
**Owns:** wiring in play, result, failed and calendar screens; `app/…/ads/flow/**`.
**Do:** Interstitial after "Next" through `InterstitialPolicy`. Rewarded buttons that state the reward first. Offline fallbacks. Premium offer when a rewarded ad cannot load.
**Done when:** `./gradlew check` passes. The handoff has a manual test table: each placement × online/offline × free/Premium.

### M5 — Mistake limit and continue flow
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 8.4.
**Owns:** `app/…/play/MistakeLimit.kt`, failed screen actions.
**Done when:** `./gradlew check` passes. Restart never shows an ad.

### M6 — Premium purchase and restore
Tier H · Build Claude (`builder`) · Review Codex
Plan: 8.5.
**Owns:** `app/…/billing/**`.
**Do:** `BillingService` interface, real and fake. One non-consumable product. Acknowledge purchases. Local flag works offline; re-check when online. Restore in Settings. Handle pending purchases.
**Done when:** `./gradlew check` passes. The lead buys and restores with a licence-test account.

### H4 — Firebase project, AdMob units, licence tester (Human)
- [ ] Create the Firebase project. Put `google-services.json` in `app/` locally (it is git-ignored).
- [ ] Create the AdMob app and ad units. Store the IDs in `~/.gradle/gradle.properties`, not in the repo.
- [ ] Add your Google account as a Play licence tester.

### M7 — Analytics, Crashlytics, Remote Config
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 8.6, 10.
**Owns:** `app/…/analytics/**`, Firebase entries in Gradle files.
**Do:** `Analytics` interface with Firebase and fake implementations. All events in plan 10. Remote Config bound to `Config` from M1, in-app defaults first. The build works when `google-services.json` is missing (CI and agents): use the fake.
**Done when:** `./gradlew check` passes in CI without `google-services.json`.

---

## Phase 5 — Closed test

### S1 — Store listing and policy form drafts
Tier M · Build Claude (`builder`) · Review Codex
Plan: 8.7, 13.
**Owns:** `docs/store/**`.
**Do:** Title, short and full description, release notes, Data safety answers draft, content rating answers draft, ads declaration. Check the name rules.
**Done when:** Character limits are met. The lead approves the text.

### S2 — Screenshot capture script
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
**Owns:** `scripts/screenshots/**`, `app/src/androidTest/**/screenshots/**`.
**Do:** A script that captures 8 screens (one board per game, dark mode, result, home, offline message) on an emulator at Play sizes.
**Done when:** The lead runs it and gets the image files.

### S3 — Release build configuration
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
**Owns:** release parts of `app/build.gradle.kts`, `app/proguard-rules.pro`.
**Do:** R8 on, rules for Room, Billing, Ads, Firebase. Version code scheme. Signing config reads `keystore.properties` if it exists (never committed). Live ad IDs from Gradle properties; debug always uses test IDs.
**Done when:** `./gradlew :app:bundleRelease` builds an unsigned bundle when no keystore is present.

### H5 — Art, keystore, closed test, production access (Human)
Plan: 11 phase 5, 4 (art).
- [ ] App icon, feature graphic, final screenshots (agents may draft; you approve).
- [ ] Create the upload keystore. Back it up outside the PC.
- [ ] Upload to the closed test track. 12+ testers for 14 days.
- [ ] Make bug cards from tester reports.
- [ ] Data safety, content rating, target audience, ads declaration. Apply for production.

## Phase 6 — Launch

### H6 — Live ad units, staged rollout, weekly review (Human)
Plan: 11 phase 6, 3.

---

## Template for post-launch games (plan section 12)

Each new game gets the same card set as a launch lane, with a new two-letter prefix (for example `SU` for Sudoku 9x9):
`x1`–`x4` engine lane → bank cards → board card → intro level → `levelsPerInterstitial` and config key → store update notes.
Games with lives (block puzzle, minesweeper, sort) add one card for the revive flow (plan 8.4).
