# Witgrid task backlog

Each card is one unit of work for one agent in one worktree. The lead updates this file. Agents never edit it.

- **Tier**: L, M, H or Human. See `docs/AGENT_WORKFLOW.md` section 2.
- **Build / Review**: the default agent family. `Codex` = Codex CLI, `Claude` = Claude Code subagent. You can swap the families. Keep the reviewer from the other family.
- **Status**: `todo` → `ready` (all needs done) → `active` → `review` → `done`. `blocked` if waiting on a person.
- Path short forms: `engine/…` = `engine/src/{main,test}/kotlin/com/datathecodie/witgrid/engine/…`; `app/…` = `app/src/{main,test,androidTest}/kotlin/com/datathecodie/witgrid/…`; `bankgen/…` = `bankgen/src/{main,test}/kotlin/com/datathecodie/witgrid/bankgen/…`.

## Index

| ID | Title | Tier | Build | Review | Needs | Status |
|---|---|---|---|---|---|---|
| **Phase 0 — Setup** |||||||
| H0.1 | Accounts, legal, website | Human | — | — | — | ready |
| H0.2 | Developer machine and repo setup | Human | — | — | — | done |
| T0.1 | Gradle project with three modules | M | Claude | Codex | H0.2 | done |
| T0.2 | GitHub Actions CI | L | Codex | Claude | T0.1 | done |
| T0.3 | Ads SDK decision memo | M | Claude | — (human decides) | — | done (Next-Gen) |
| **Phase 1 — Engine and puzzle bank** |||||||
| E1 | Engine core contracts | H | Codex | Claude + human | T0.1 | done |
| E2 | Engine test kit | L | Claude | Codex | E1 | done |
| CR1–CR4 | Crowns lane | see lane table | | | E1 | done |
| BI1–BI4 | Binary lane | see lane table | | | E1 | done |
| NO1–NO4 | Nonogram lane | see lane table | | | E1 | done |
| MS1–MS4 | Mini Sudoku lane | see lane table | | | E1 | done |
| B1 | `bankgen` command-line tool | M | Codex | Claude | E1 | done |
| B2 | Generate the launch bank and report | L | Codex | Claude | B1, CR3, BI3, NO3, MS3 | done |
| B3 | Bank validation in `check` and CI | L | Claude | Codex | B2, T0.2 | done |
| H1 | Hand-test the bank | Human | — | — | B2 | ready |
| CR5 | Crowns: no regions under 3 cells; unsigned IDs | L | Claude (Haiku) | Claude (Sonnet) | CR3 | done |
| MS5 | Mini Sudoku Easy with 18–22 givens; unsigned IDs | L | Claude (Haiku) | Claude (Sonnet) | MS3 | done |
| B4 | Unsigned level IDs for Binary and Nonogram | L | Claude (Haiku) | Claude (Sonnet) | BI3, NO3 | done |
| B5 | Regenerate the launch bank and report v2 | L | Claude (Haiku) | Claude (Sonnet) | CR5, MS5, B4 | done |
| **Phase 2 — Core app** |||||||
| A1 | Theme and design tokens | M | Claude | Codex | T0.1 | done |
| A2 | Storage and saves | M | Codex | Claude | E1 | done |
| A3 | App contracts, navigation, play controller | H | Codex | gpt-6.1-sol + human | A1, A2 | done |
| A4 | Board canvas kit and gestures | H | Claude | Codex | A3 | done |
| A5 | Crowns board | M | Claude | Codex | A4, CR2 | done |
| A6 | Binary board | M | Codex | Claude | A4, BI2 | done |
| A7 | Nonogram board | M | Claude | Codex | A4, NO2 | done |
| A8 | Mini Sudoku board | M | Codex | Claude | A4, MS2 | done |
| A9 | Home and Game screens | M | Claude | Codex | A3 | done |
| A10 | Play toolbar, Result and Level failed screens | M | Claude | Codex | A3, A4 | done |
| A11 | Hint and check UI | M | Codex | Claude | A10 | done |
| A12 | Guided first level for each game | L | Claude | Codex | A5, A6, A7, A8 | done |
| A13 | Edge-to-edge, predictive back, tablet layout | M | Codex | Claude | A9, A10 | done |
| A14 | UI smoke tests | L | Codex | Claude | A5–A8, A10, A12, A13, B2 | done |
| **Phase 3 — Retention** |||||||
| R1 | Daily, streak and freeze logic | M | Codex | Claude | A2 | done |
| R2 | Daily UI and calendar screen | M | Claude | Codex | R1, A9 | done |
| R3 | Statistics screen | L | Claude | Codex | A2, A9, R2 | done |
| R4 | Reminder notification | L | Codex | Claude | A3, R3, M3 | done |
| R5 | Settings screen | L | Claude | Codex | A1, A3, R3, R4, M1, M3 | done |
| R6 | Accessibility pass | M | Claude | Codex | A5–A8, R5 | done |
| **Phase 4 — Monetisation and data** |||||||
| M1 | Monetisation policy logic | H | Codex | Claude | A2, R1, R2, A11 | done |
| M2 | Policy tests from the spec | L | Claude | Codex | M1 | done |
| M3 | Consent and ad service (test units) | H | Claude | Codex | M1 | done |
| M4 | Ads in the player flows | M | Claude | Codex | M3, M1, A10, A11, R2, R4, R5 | done |
| M5 | Mistake limit and continue flow | M | Codex | Claude | M1, M4, A10 | done |
| M6 | Premium purchase and restore | H | Claude | Codex | M1, M3, R5 | blocked |
| H4 | Firebase project, AdMob units, licence tester | Human | — | — | H0.1 | todo |
| M7 | Analytics, Crashlytics, Remote Config | M | Codex | Claude | M1, M4–M6, H4 | todo |
| **UI cleanup (before the closed test)** |||||||
| U0 | Hotfix: invisible text in dark mode | M | Claude (lead) | — | — | done |
| U1 | Visual design target and approval | H | Claude (lead) | Human | U0 | done |
| U2 | App shell: top bar, back, splash, first-run welcome | H | Claude (lead) | Codex | U1 | done |
| U3 | Shared components and tokens | H | Codex | Claude (lead screenshots) | U2 | done |
| U4 | Play screen layout and board fit | H | Codex | Claude (lead screenshots) | U3 | done |
| U5 | Board looks for each game | M | Codex | Claude (lead screenshots) | U4 | done |
| U6 | Home and Game screens | M | Codex | Claude (lead screenshots) | U3, U9 | done |
| U7 | Result, Failed, Calendar, Statistics, Settings | M | Codex | Claude (lead screenshots) | U3, U9 | active |
| U8 | Screenshot matrix and visual QA gate | M | Codex | Claude (lead) | U4–U7 | todo |
| U9 | Endless level numbers and looping | H | Codex | Claude | U2 | done |
| **Phase 5 — Closed test** |||||||
| S1 | Store listing and policy form drafts | M | Claude | Codex | A9 | done |
| S2 | Screenshot capture script | M | Codex | Claude | A14 | done |
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
- [x] Make the developer website: privacy policy page and `app-ads.txt`.
- [ ] Export GDD section 7 (ASO, launch, closed test) again. The PDF in the repo stops after section 6; save the new export as `docs/reports/GDD-section-7.pdf`.
**Done when:** Trademark searches are dated and recorded; Play Console and AdMob accounts are verified; the public HTTPS privacy policy and `app-ads.txt` URLs are live; and `docs/reports/GDD-section-7.pdf` is available. Send the public privacy-policy URL and support email to the lead so the app setting and store drafts can be finalized. Record public URLs and account status, never credentials.
**Lead progress note (17 Nov 2026):** The privacy policy at `https://datathecodie.com/witgrid/privacy-policy.html` and `https://datathecodie.com/app-ads.txt` are live and were verified on 17 Nov; the policy matches the committed page and the Settings URL. Trademark searches, Play Console/AdMob verification, and the GDD section 7 export remain.
**Lead source check (17 Nov 2026):** Searched Drive for Witgrid, the GDD title, and DataTheCodie Studios. No matching GDD source was found; a broader ASO search returned only an unrelated LLM Clicker GDD. The repository copy still ends at section 6, so the correct section 7 source/export remains outstanding.

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
- x2: `solveWithHints` solves the hand-made unique fixtures intended to be technique-solvable. Zero-solution and multiple-solution x1 fixtures are counter-only. A unique but technique-stuck fixture is rejected by `grade`. Every `HintStep` has a non-empty, one-sentence explanation.
- x3: For each pack, seed 1 gives 10 puzzles. Each is unique, solved by the technique solver, and graded inside its pack band. The same seed gives the same puzzles on a second run.
- x4: Tests compile and run. Failures that show real bugs are listed in the handoff, not fixed.

**Lane rules from the E1 review (16 Nov 2026):**
- x1 KDoc must define what `mistakes()` counts for the game. Crowns: is an X on a crown cell a mistake? Nonogram: is an X on a filled cell a mistake? (Recommended: yes for both, so "check" and the mistake limit catch it.)
- MS1 must define the note/value policy: `SetDigit` on a cell clears that cell's notes.
- Auto-X (Crowns), auto-cross (Nonogram) and auto-update notes (Mini Sudoku) are not engine work. The app emits them as extra absolute `Move`s in one `History` record per gesture.

**Game lane progress (16 Nov 2026):** NO1–NO4 and MS1–MS4 are done; B1–B3 are complete and H1 is ready for hand-testing.

**CR1 lead-approved scope extension (16 Nov 2026):** CR1 also owns `engine/src/test/kotlin/com/datathecodie/witgrid/engine/RegistryTest.kt` so the first concrete engine can replace its registry stub. Update the shared test once to cover the registered Crowns implementation and remove the obsolete assumption that every lane must remain a stub. Later lanes keep changes in their game-local files and tests.

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

### Bank quality fixes (lead check of the Codex run, 16 Nov 2026)

Found by the lead: every Crowns puzzle has 2–3 one-cell regions (free crowns) and ~2 two-cell regions, in all packs; Mini Sudoku Easy has 27.5 of 36 givens on average (Medium has 13); level IDs print negative numbers as `-hex`, which gives IDs like `MS-EASY--379dc9…`.

**ID rule for CR5, MS5, B4:** make the ID hex with `rng.nextLong().toULong().toString(16).padStart(16, '0')`. Consume the RNG exactly as before, so the same seeds give the same puzzles. Add a test that IDs match `^[A-Z]{2}-[A-Z]+-[0-9a-f]{16}$`.

### CR5 — Crowns: no regions under 3 cells
Tier L · Build Claude (Haiku) · Review Claude (Sonnet)
Plan: 6.1, 6.5.
**Owns:** `engine/…/crowns/**` and its tests.
**Do:** Change the region growth so every region has at least 3 cells, in every pack and size. Keep uniqueness and technique-solvability. Keep grade bands; adjust them only if a pack can no longer fill, and say so. Apply the ID rule.
**Done when:** `./gradlew :engine:test` passes. A new test generates 10 puzzles for each pack with seed 1 and asserts the smallest region has ≥ 3 cells. Handoff lists the region-size histogram for those puzzles.

### MS5 — Mini Sudoku Easy with 18–22 givens
Tier L · Build Claude (Haiku) · Review Claude (Sonnet)
Plan: 6.4.
**Owns:** `engine/…/minisudoku/**` and its tests.
**Do:** Easy keeps "naked single only", but removes givens until 18–22 remain (target about 20). Medium, Hard and Expert stay as they are. Apply the ID rule.
**Done when:** `./gradlew :engine:test` passes. A test generates 10 Easy puzzles with seed 1 and asserts 18 ≤ givens ≤ 22, unique, naked-single solvable. Handoff gives the average givens for each pack.

### B4 — Unsigned level IDs for Binary and Nonogram
Tier L · Build Claude (Haiku) · Review Claude (Sonnet)
**Owns:** `engine/…/binary/BinaryGenerator.kt`, `engine/…/nonogram/NonogramGenerator.kt`, their tests.
**Do:** Apply the ID rule.
**Done when:** `./gradlew :engine:test` passes.

### B5 — Regenerate the launch bank and report v2
Tier L · Build Claude (Haiku) · Review Claude (Sonnet)
**Owns:** `app/src/main/assets/bank/**`, `docs/reports/bank-v2.md`.
**Do:** Same counts and seeds as `docs/reports/bank-v1.md`. Run `validate --all` and `stats`. Report: Crowns region-size histogram, Mini Sudoku givens per pack, and a note on what changed from v1.
**Done when:** `./gradlew check` passes. No ID contains `--`.

### H1 — Hand-test the bank (Human)
**Owns:** `docs/reports/bank-hand-test.md`.
**Do:** Solve 20 puzzles for each of the four games in each of the four packs (320 solves) using the first app build. Record puzzle ID, elapsed time, whether a guess was needed, and any grade or clue issue. Give issue details to the lead for bug-card creation; do not edit `docs/TASKS.md` directly.
**Done when:** The report has 20 records for every game/pack pair; the lead creates a bug card for every issue found before H1 closes. If no issues were found, record that explicitly.
**Lead prep note (17 Nov 2026):** The log is prefilled with the first 20 regular bank IDs in app order for each game/pack pair (320 IDs total). H1 remains ready until the human records all solves and any issues.

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
**Owns:** `app/…/data/**`, `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`.
**Do:** Room tables for level progress (state bytes, elapsed time, hints used, completed, best time). DataStore for settings, streak, freeze count, Premium flag, reward balances. A `ProgressRepository` interface. Auto Backup rules that include both.
**Done when:** `./gradlew :app:testDebugUnitTest` passes with Room in-memory tests. A save survives a repository re-creation.

### A3 — App contracts, navigation, play controller
Tier H · Build Codex (`gpt-6.1-sol`, high) · Review independent `gpt-6.1-sol` agent + human read
Plan: 7.3, 9.
**Owns:** `engine/src/main/kotlin/com/datathecodie/witgrid/engine/core/VersionedStateCodec.kt` and the `*Engine.kt` and `*StateCodec.kt` files in `engine/src/main/kotlin/com/datathecodie/witgrid/engine/{crowns,binary,nonogram,minisudoku}/`; `app/src/main/kotlin/com/datathecodie/witgrid/games/GameModule.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/GameRegistry.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/play/PlayController.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/**`, stub screens in `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/**`, `app/src/main/kotlin/com/datathecodie/witgrid/MainActivity.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/AndroidManifest.xml`.
**Do:**
1. App-side `GameModule`: wraps one `GameEngine`, adds `levelsPerInterstitial` and `@Composable Board(state, onMove)`.
2. `PlayController` (plain Kotlin, no Compose): loads a level from the bank, applies moves, undo and redo, timer that pauses in the background, saves after each move through `ProgressRepository`, emits a solved event. One gesture (including auto-X, auto-cross or note updates) and one hint "Apply" each make one `History` record. Undo history is not saved: after an app restart, undo starts empty (accepted for plan 7.5).
3. Set `android:theme="@style/Theme.Witgrid"` in the manifest and wrap `MainActivity` content in `WitgridTheme` (from A1). The manifest and `MainActivity.kt` are in Owns for this step.
4. Navigation: Home → Game → Play → Result / Level failed; Calendar, Statistics, Settings. Stub screens show their name.
 5. Implement versioned, self-contained state codecs in the four owned engines. Round-trip puzzle context, entries and notes, and reject malformed or unsupported versions.
**Done when:** `./gradlew check` passes. `PlayController` has JVM tests with a fake engine and a fake repository. All four engine state codecs pass round-trip and malformed-save tests. The human has read the interfaces.

### A4 — Board canvas kit and gestures
Tier H · Build Claude (`builder`) · Review Codex
Plan: 4 (layout, gestures, feedback).
**Owns:** `app/…/ui/board/**`, `app/src/main/res/values/strings.xml` (localized cell accessibility labels).
**Do:** A reusable Compose Canvas grid: cell hit-testing, tap, drag-paint path, long-press, two-finger tap. Haptic tick (light and strong), haptics switch. Completion wave animation with reduce-motion support. Grid fills width with 16 dp margins.
**Done when:** `./gradlew check` passes. Hit-testing and drag-path logic have JVM tests. A demo board in a preview responds to all gestures.

### A5–A8 — Game boards
Tier M · Build per index · Review the other family
Plan: section 6 input row for the game.
**Owns:** `app/…/games/<game>/**`, `app/src/androidTest/kotlin/com/datathecodie/witgrid/games/<game>/**`, game-board accessibility labels in `app/src/main/res/values/strings.xml`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/board/BoardCanvas.kt` and `BoardGestures.kt` (allow a board to disable unsupported drag input), `app/src/main/kotlin/com/datathecodie/witgrid/games/GameModule.kt` (forward board undo and read packaged bank assets), `app/src/test/kotlin/com/datathecodie/witgrid/games/GameModuleTest.kt`, one module registration in `app/src/main/kotlin/com/datathecodie/witgrid/games/GameRegistry.kt`, and Compose UI test setup in `gradle/libs.versions.toml` and `app/build.gradle.kts`. The added dependencies are limited to the Compose UI test API and the AndroidX test runner/JUnit extension.
**Do:** Implement the `GameModule` and `Board` for the game with the A4 kit. Use the input rules from the plan. Draw region letters/patterns when the setting is on. Register the module in `GameRegistry` (one line).
**Done when:** `./gradlew check` passes. A Compose UI test plays the first bank puzzle to the solved event by applying the solution moves.

### A9 — Home and Game screens
Tier M · Build Claude (`builder`) · Review Codex
Plan: 9 (Home, Game).
**Owns:** `app/…/ui/screens/home/**`, `app/…/ui/screens/game/**`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/progress/LevelProgressDao.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/progress/ProgressRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/progress/RoomProgressRepository.kt`, `app/src/main/res/values/strings.xml`.
**Do:** Home: game cards with progress, daily state placeholder, streak placeholder, one quiet Premium row (no action yet). Game: four packs with progress, daily puzzle entry, "Continue".
**Done when:** `./gradlew check` passes. ViewModels have JVM tests.

### A10 — Play toolbar, Result and Level failed screens
Tier M · Build Claude (`builder`) · Review Codex
Plan: 4 (layout, timer), 9 (Play, Result, Level failed).
**Owns:** `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/play/**`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/result/**`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/failed/**`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/kotlin/com/datathecodie/witgrid/games/nonogram/NonogramBoard.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/minisudoku/MiniSudokuBoard.kt`.
**Do:** Tools in the bottom third: undo, redo, mark mode, hint, check, timer (hideable). Nothing tappable in the top 15 %. Result: time, best time, "Next". Level failed: "Continue" and "Restart" (Continue is a placeholder until M5).
**Done when:** `./gradlew check` passes.

### A11 — Hint and check UI
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 7.6, 8.2 (allowances come later in M4).
**Owns:** `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/play/hint/**`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/play/PlayScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/play/HintController.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/board/BoardCanvas.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/board/BoardHighlights.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/board/BoardAccessibilityCells.kt`.
**Do:** Hint: mistakes first, else `nextStep`; highlight cells, technique name, one-sentence explanation, "Apply" button. Check: flag wrong cells. A mistake hint has no `Technique`: use an app-side hint type (for example `sealed interface HintUi { Mistake; Step(HintStep) }`), not a fake technique. Use an `Allowance` interface with an "always allow" stub (M4 replaces it).
**Done when:** `./gradlew check` passes. `HintController` has JVM tests.

### A12 — Guided first level for each game
Tier L · Build Claude (`chore`) · Review Codex
Plan: 6.5 (first level), GDD onboarding row.
**Owns:** `app/src/main/assets/bank/*-intro.txt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/GameModule.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/binary/BinaryGameModule.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/binary/Intro.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/crowns/CrownsGameModule.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/crowns/Intro.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/minisudoku/MiniSudokuGameModule.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/minisudoku/Intro.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/nonogram/NonogramGameModule.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/nonogram/Intro.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/game/GameViewModel.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/play/PlayScreen.kt`, `app/src/main/res/values/strings.xml`, `bankgen/src/main/kotlin/com/datathecodie/witgrid/bankgen/BankgenService.kt`.
**Do:** Add one small hand-made intro puzzle per game as the first Easy entry, with one coach line. Validate the raw `*-intro.txt` assets with `bankgen validate --all`. On a fresh install, each game's default Continue target is its intro; the next level after solving it is the regular Easy level.
**Done when:** `./gradlew check` passes. A fresh install starts each game on its intro level.

### A13 — Edge-to-edge, predictive back, tablet layout
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 7.1 (Play rules).
**Owns:** `app/…/MainActivity.kt`, `app/…/nav/WitgridNavigation.kt`, `app/…/ui/layout/**`, window-size handling in screen files (list them in the handoff).
**Do:** Draw edge-to-edge and apply system-bar and IME insets so controls remain usable. Preserve each screen's back action and use predictive-back progress to animate the current screen, reset on cancellation, and navigate only after completion. Make Home, Game, Play, Result, Level failed, Calendar, Statistics and Settings responsive, with centered/constrained content at tablet width. Reflow Play controls beside the board in wide, short windows so large puzzles remain usable. Add or update previews for 360 dp, 840 dp, and 840×360 dp; include a real large Nonogram board preview. Do not add a new dependency.
**Done when:** `./gradlew check` passes. Screens work at 360 dp phone and 840 dp tablet widths in previews, including the short landscape Play layout.

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
**Owns:** `app/…/domain/daily/**`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerData.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`.
**Do:** Keep daily and streak rules in plain Kotlin. Daily index is the local date's days since the Unix epoch date (1970-01-01) mod 365. One shared streak day is earned when any one of the four daily puzzles is completed that local date. Hold at most one free freeze; refill it to one each local Monday before reconciling missed dates, with no accumulation, and automatically use it to cover the earliest missed local date. A freeze covers only one date; a longer gap still breaks the streak. A rewarded repair restores the latest uncovered missed local date and must complete within 48 hours after that day ends; M4 supplies the reward callback. Persist the freeze refill date and repairable missed date with the existing streak data. Inject a millisecond clock and a time-zone provider so changes to the device zone take effect. Do not rewind streak state or grant a duplicate day when the clock moves backwards. Keep production code compatible with minSdk 24; do not add a library for date support.
**Done when:** JVM tests cover local midnight, time-zone changes, one and multiple missed days with and without a freeze, Monday refill including unused-freeze and missed-week behavior, repair deadline boundaries including an overlapping midnight, a 7-day offline streak, and the clock moving backwards. Existing `DataStorePlayerDataRepository` tests still pass; `./gradlew check` passes.

### R2 — Daily UI and calendar screen
Tier M · Build Claude (`builder`) · Review Codex
**Owns:** `app/…/ui/screens/calendar/**`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/home/HomeScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/game/GameScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/game/DailyPuzzleIndex.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/domain/daily/DailyProgressCoordinator.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/progress/**`, `app/src/main/res/values/strings.xml`.
**Do:** Wire daily puzzle entry, completion state and calendar navigation to R1's local-date rules and the committed daily bank. Persist daily completion history by local epoch day so recurring 365-day bank IDs do not merge different dates. Reconcile and persist streak state before displaying repair availability; serialize read-transition-write updates and supply the current device time zone each time. Keep the daily status localized. The calendar shows completion history for past dates; only today's daily puzzles are playable, and future dates are unavailable.
**Done when:** `./gradlew check` passes.
**Decision:** Past daily puzzles are history-only for launch. A completion row is replayed into streak state before missed-day rules, including after offline recovery.

### R3 — Statistics screen
Tier L · Build Claude (`chore`) · Review Codex
Plan: 9 (Statistics).
**Owns:** `app/…/ui/screens/stats/**`, `app/…/domain/stats/**`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/progress/**`, `app/src/main/res/values/strings.xml`.
**Do:** Count distinct completed regular levels per game; exclude guided intro levels and do not count replays as new solves. Show daily completions separately, keyed by local date and game. For regular levels, show the fastest best time and the arithmetic mean of one best time per completed level; label the latter "Average best time". Do not add attempt-history storage.
**Done when:** `./gradlew check` passes. The stats calculation has JVM tests.

### R4 — Reminder notification
Tier L · Build Codex (`gpt-6-luna`) · Review Claude
Plan: 5 (one optional reminder).
**Owns:** `app/…/reminder/**`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerData.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`.
**Do:** Off by default. Persist the enabled state and local hour/minute through PlayerData. `ReminderController.enable` reports when API-33+ notification permission is required; R5 requests permission only from the player's explicit enable action and retries after a grant. The receiver schedules the next local occurrence after delivery and restores it after reboot, clock/time-zone change, and app update.
**Done when:** `./gradlew check` passes.

### R5 — Settings screen
Tier L · Build Claude (`chore`) · Review Codex
Plan: 9 (Settings).
**Owns:** `app/src/main/kotlin/com/datathecodie/witgrid/MainActivity.kt`, `app/…/ui/screens/settings/**`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/StubScreens.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/layout/ScreenPreviews.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerData.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`.
**Do:** Theme, colourway, assists, mistake limit, haptics, sound, reminder, Premium row and restore placeholders, privacy link, licences. Read the policy destination from the `settings_privacy_policy_url` resource and open only a valid HTTPS link; H0.1 supplies the published URL before release. Add a privacy-options entry that calls the consent options exposed by M3. Wire reminders through `ReminderController`; request API-33+ notification permission only from the explicit reminder-enable action and retry after a grant, including after activity recreation. Do not prompt at app startup.
**Done when:** `./gradlew check` passes.

### R6 — Accessibility pass
Tier M · Build Claude (`builder`) · Review Codex
Plan: 4 (accessibility).
**Owns:** `app/src/main/kotlin/com/datathecodie/witgrid/ui/board/**`, `app/src/main/kotlin/com/datathecodie/witgrid/games/crowns/CrownsBoard.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/binary/BinaryBoard.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/nonogram/NonogramBoard.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/games/minisudoku/MiniSudokuBoard.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/play/**`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/settings/SettingsScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerData.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/res/values/strings.xml`.
**Do:** TalkBack label for each cell, font scaling to 200 %, reduce-motion, colour-blind letters and patterns, touch targets ≥ 48 dp. Dense boards retain a fit-to-screen overview and add an optional enlarged pannable mode whose cell touch targets are at least 48 dp.
**Done when:** `./gradlew check` passes. The handoff lists each screen and its result.

---

## Phase 4 — Monetisation and data

### M1 — Monetisation policy logic
Tier H · Build Codex (`gpt-6.1-sol`, high) · Review Claude
Plan: 8.1–8.6.
**Owns:** `app/…/domain/monetisation/**`, `app/…/domain/config/**`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerData.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/play/HintController.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/play/PlayController.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/play/PlayScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/AndroidManifest.xml`.
**Do:** Plain Kotlin, no SDK code.
- `InterstitialPolicy.shouldShow(...)` with every condition in 8.1.
- `AllowanceLedger`: shared free hints each local day, free checks in Easy, continues, streak repairs and offline extra hints; Premium removes allowance costs. Spend a hint when useful advice is revealed, but do not charge again for the same advice on an unchanged board; unavailable hints cost nothing. Persist allowance usage, reward balances, processed reward grant IDs and interstitial counters atomically before returning success. Reward grant IDs make callback retries idempotent. Wire the allowance into Play and provide its local-date and connectivity inputs without showing ads there.
- Premium removes the ad or allowance cost for streak repair, but the same latest-uncovered-miss and 48-hour eligibility rules still apply. Authorize only a fresh repairable-miss snapshot, rechecking its persisted miss and deadline atomically before spending a credit.
- `ConfigKeys` with every key and default in 8.6, behind a `Config` interface.
**Done when:** `./gradlew :app:testDebugUnitTest` passes. `./gradlew check` also passes as the project gate.

### M2 — Policy tests from the spec
Tier L · Build Claude (`test-writer`) · Review Codex
**Owns:** `app/src/test/**/monetisation/spec/**`.
**Do:** Add independent tests for each rule and "Never" line exposed by M1's public policy and allowance APIs. Read only the plan and M1's public signatures.
**Coverage boundary:** SDK and flow behavior is verified by its owner card: M3 covers consent, preload/expiry and SDK reward callbacks; M4 covers rewarded choices/labels, placements, no ads on an active board and offline fallback; M5 covers mistake-limit, continue and restart behavior. R1/R2 already cover freeze reconciliation and streak-repair application. R5/M6 cover Premium offer, restore and online revalidation; M7 covers offline-session analytics.
**Done when:** Tests run. The handoff lists any failures as bugs.

### M3 — Consent and ad service (test units)
Tier H · Build Claude (`builder`) · Review Codex
Plan: 7.1, 8.3, 8.7, decision from T0.3.
**Owns:** `app/…/ads/**`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`.
**Do:** `AdService` interface with a real implementation and a fake. UMP consent before the first ad request, with a privacy-options action exposed for Settings. Preload one interstitial and one rewarded ad when online; drop ads older than one hour. Reward completion returns a stable grant ID for `AllowanceLedger`, so callback retries cannot grant twice. Google's public test app ID and test unit IDs only. Live IDs come later from Gradle properties outside the repo (S3).
**Done when:** `./gradlew check` passes. The M3 handoff identifies the Google test IDs; M4 records on-device test-ad rendering in its online placement matrix after adding the first ad callsite.

### M4 — Ads in the player flows
Tier M · Build Claude (`builder`) · Review Codex
Plan: 8.1–8.3, 8.5 (offer placement).
**Owns:** `app/…/ui/screens/game/GameScreen.kt`, `app/…/ui/screens/play/**`, `app/…/ui/screens/result/**`, `app/…/ui/screens/failed/**`, `app/…/ui/screens/calendar/**`, `app/…/nav/WitgridNavigation.kt`, `app/…/ads/flow/**`, `app/…/data/settings/PlayerDataRepository.kt`, `app/…/data/settings/DataStorePlayerDataRepository.kt`, `app/src/main/res/values/strings.xml`.
**Do:** Interstitial after "Next" through `InterstitialPolicy`. Never show or start an ad while a puzzle board is active. Offer rewarded hint/check credits from the pre-puzzle Game screen; spend those credits in Play. Offer rewarded Continue on Level failed and rewarded streak repair on Calendar. Each rewarded button states the reward first. Add offline fallbacks and a Premium offer when a rewarded ad cannot load. Premium repair follows the same latest-uncovered-miss and 48-hour eligibility rules.
**Done when:** `./gradlew check` passes. The handoff has a manual test table: each placement × online/offline × free/Premium, with a visible Google test ad confirmed on a device in an eligible online flow.

### M5 — Mistake limit and continue flow
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 8.4.
**Owns:** `app/…/play/MistakeLimit.kt`, `app/…/play/PlayController.kt`, `app/…/ui/screens/play/**`, `app/…/ui/screens/failed/**`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/progress/**`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerData.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/domain/monetisation/AllowanceLedger.kt`, `app/src/main/res/values/strings.xml`.
**Do:** With the setting on, three mistakes fail a logic level. Persist the mistake count with level progress and restore it when the level is reopened. Continue grants exactly one additional mistake and leaves the board as-is; each later Continue grants one more. Restart is always free and never shows an ad.
**Done when:** `./gradlew check` passes. Restart never shows an ad.

### M6 — Premium purchase and restore
Tier H · Build Claude (`builder`) · Review Codex
Plan: 8.5.
**Owns:** `app/…/billing/**`, `app/…/ads/flow/**`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/settings/SettingsScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/home/HomeScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/game/GameScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/failed/LevelFailedScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/calendar/CalendarScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/res/values/strings.xml`.
**Do:** `BillingService` interface, real and fake. Add Google Play Billing Library 9.1.0 as the only new library for this card. Implement one non-consumable product with ID `witgrid_premium_lifetime`; show its localized Play price. Acknowledge completed purchases. Keep the local flag working offline and re-check owned purchases when connected. Restore in Settings. Handle pending purchases without granting Premium early. Make the Premium offer actionable from Home, Settings, unavailable rewarded offers, failed-level Continue, and streak repair. H4 creates the Play product at the plan's $7.99 price and supplies the licence tester.
**Done when:** `./gradlew check` passes. The lead buys and restores with a licence-test account.
  **Lead status note (17 Nov 2026):** Code and review are complete. Waiting on H4's Play product and licence tester for the required device purchase and restore check.

### H4 — Firebase project, AdMob units, licence tester (Human)
- [ ] Create the Firebase project, register this Android app, and enable Analytics, Crashlytics, and Remote Config. Put `google-services.json` in `app/` locally (it is git-ignored).
- [ ] Create the AdMob app and ad units. Store the IDs in `~/.gradle/gradle.properties`, not in the repo.
- [ ] Configure and publish the required UMP consent message(s) in AdMob. S3 verifies the in-app flow after wiring the owner app ID; keep Google test app and ad unit IDs in test builds.
- [ ] Create the one-time non-consumable Play product `witgrid_premium_lifetime` at $7.99 and add your Google account as a Play licence tester.
**Done when:** Firebase is registered for this Android app and its local config remains uncommitted; AdMob has the app, interstitial/rewarded units, and published consent messages; the Play product exists at the planned price and the licence tester is enabled. Keep Google test ad IDs in all debug and closed-test builds; production IDs stay outside the repository.

### M7 — Analytics, Crashlytics, Remote Config
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 8.6, 10.
**Owns:** `app/…/analytics/**`, `app/…/play/**`, `app/…/nav/WitgridNavigation.kt`, `app/…/ui/screens/home/**`, `app/…/ui/screens/play/**`, `app/…/ui/screens/result/**`, `app/…/ui/screens/failed/**`, `app/…/ui/screens/calendar/**`, `app/…/ui/screens/settings/**`, `app/…/ads/flow/**`, `app/…/billing/**`, `app/…/domain/daily/**`, `app/…/MainActivity.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerData.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`, `app/src/main/res/values/strings.xml`, `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`.
**Do:** `Analytics` interface with Firebase and fake implementations. Instrument every event in plan 10 at its source. Analytics and Crashlytics are disabled by default; enable them only after explicit Settings opt-in and disable them when consent is withdrawn. This is separate from UMP ad consent. Remote Config bound to `Config` from M1, in-app defaults first. The build works when `google-services.json` is missing (CI and agents): use the fake.
**Done when:** `./gradlew check` passes in CI without `google-services.json`.

---

## UI cleanup (before the closed test)

The owner tested the app on a phone (17 Nov 2026) and rejected the UI. The lead's audit on the emulator (light and dark, font scale 1.0 and 1.3) found:

- **Dark mode:** text without an explicit colour was black on black. All titles, Settings labels, the rules line and the timer were invisible. Fixed in U0.
- **No app shell:** no top bar, no back arrow, no icons. Navigation is text links ("Calendar", "Statistics", "Settings") at the bottom of Home and text buttons ("Home", "Back").
- **Board fit:** the board is clipped at the bottom (Mini Sudoku, Binary at 1.3 font), the Nonogram grid runs off the right edge, and the Crowns board touches the screen edges (the plan asks for 16 dp margins).
- **Play tools:** a row of plain text buttons (Undo, Redo, Hide timer, Hint, Check, Back) plus a "Larger cells" chip in the middle of the screen. A large empty band above the board.
- **Crowns:** region letters and hatching are on by default, so the board looks noisy. They are colour-blind aids and must be off by default.
- **Binary:** cells show the letters A and B. Use two clear symbols instead.
- **Game screen:** two "Watch an ad" buttons sit above Continue and the packs. The plan says one quiet offer, not a wall of ads.
- **Home:** flat grey cards with text only; no game preview, no progress bar, the streak and the daily puzzle look like plain notes.
- **First run:** no splash and no welcome. The app opens straight into a list.

Rules for every U card:
- Follow plan section 4 (paper and ink, 16 dp margins, tools in the bottom third, nothing tappable in the top 15 % during play) and section 9 (one job per screen).
- No fixed heights for text. Every screen must work at font scale 1.0, 1.3 and 2.0, on a 360 dp wide phone and on a large phone, in light and dark.
- **Done when** for U2 to U7: `./gradlew check` passes, and the lead has reviewed a screenshot sheet of each changed screen in light and dark at font scale 1.0 and 1.3.
- No new library unless the card says so.
- **Endless levels (owner decision, 17 Nov 2026).** Never show a level total, "x of y", a percent or a progress bar for levels or packs. Show only "Level N". After the last bank puzzle of a pack, the level number keeps going up and the puzzles loop (level 601 plays bank puzzle 1 again). Remove `home_progress` and `game_pack_progress`. Puzzle-internal counts (for example "2 of 7 crowns", mistakes) are permitted.
- **No fixed game count.** No text may say how many games there are ("four games"), and screen titles do not include game descriptions. Layouts must work for any number of games.

### U0 — Hotfix: invisible text in dark mode (done)
Root `Surface` sets `contentColor = onBackground` in `MainActivity`.

### U1 — Visual design target and approval (done)
Tier H · Lead
**Do:** Mockups of Home, Game, Play (all four boards), Result and Settings in light and dark, from the plan's paper-and-ink direction and the existing tokens. Decide: the top bar style, the icon set (drawn as vector resources, no icon library), the Play toolbar, Binary symbols, card style and progress bars.
**Done when:** the owner approves the mockups. The approved images go in `docs/design/`.

### U2 — App shell (done)
Tier H · Lead · Review Codex
**Owns:** `MainActivity.kt`, `nav/**`, `ui/layout/**`, `res/values*/themes.xml`, `res/drawable/**`, `strings.xml`.
**Do:** One `Scaffold` per screen with a top bar (title, back arrow, actions). Home actions: Calendar, Statistics, Settings as icons. System splash with the app icon (theme attributes; Android 12+ API, window background below). A first-run welcome of two or three short pages (logic puzzles that work offline, how hints work, ads only between levels), shown once. No game count and no game list. The UMP consent form keeps its current place before the first ad request.

Lead note (U2): The navigation wraps Home, Game, Calendar, Statistics and Settings in `WitgridScreenFrame` + `WitgridTopBar` (`ui/layout/`). The old screen titles and "Home" buttons are removed, but the unused `onHome`, `onOpenStatistics` and `onOpenSettings` parameters stay until U6 and U7 rebuild those screens. Play, Result and Level failed have no shared bar yet (U4, U7). The welcome flag is `AppSettings.welcomeSeen` (outside the card's Owns, needed to show the pages once). Card, edge and muted-ink colours were added to `WitgridColors` early for the welcome page. Icons are vector drawables `ic_back`, `ic_close`, `ic_calendar`, `ic_statistics`, `ic_settings`, `ic_info`.

Hand-off note (17 Nov 2026): U3 to U9 go to Codex (`gpt-6.1-sol`). The design target is `docs/design/` (PNGs and README) and plan section 4.1. The rules at the top of this section apply to every card, especially "Endless levels" and "No fixed game count". Claude (lead) reviews each card with a screenshot sheet (light and dark, font scale 1.0 and 1.3) before merge. Order: U3 and U9 first (they do not overlap), then U4, U6 and U7 in parallel, then U5, then U8.

### U3 — Shared components and tokens
Tier H · Build Codex · Review Claude (lead screenshots)
Plan: 4, 4.1. Design: `docs/design/`.
**Owns:** `ui/theme/**`, new `ui/components/**`, `ui/layout/**`.
**Do:**
- Map the tokens in `WitgridColors` (`paper`, `card`, `cardEdge`, `ink`, `mutedInk`, `accent`, `onAccent`) into the Material 3 scheme too (`surface`, `surfaceContainer*`, `outline`, `onSurfaceVariant`), so default Material parts look correct. Add `accentSoft` (light `#E5E8F5`; dark: accent at low alpha on black).
- Components in `ui/components/`: `WitgridCard` (card colour, 1 dp edge, 16–18 dp radius, optional onClick), `PrimaryButton` (accent, 54 dp min height, 14 dp radius), `SecondaryButton` (outlined), `ListRow` (title, optional subtitle, optional trailing value, chevron or switch), `SectionHeader`, `StatTile` (big number over a label), `Chip` (streak chip, status chip), `SegmentedControl` (theme switch, Fill/Cross), `ToolBarItem` (icon over label, optional badge, selected state), `IconActionButton` (48 dp).
- A `@Preview` for each component in light and dark.
- No progress bar component (endless rule).
**Done when:** `./gradlew check` passes. A preview screenshot sheet of all components in light and dark matches the mockup style.
**Lead completion note (17 Nov 2026):** U3 merged locally as `ba5a159`. The full `check` gate passed from the lead PowerShell workspace, validating 3,864 puzzles in 24 bank files. Two palette unit tests, three emulator component tests, lint, and lead screenshot review passed at font scales 1.0, 1.3, and 2.0 in both themes.

### U4 — Play screen layout and board fit
Tier H · Build Codex · Review Claude (lead screenshots)
Plan: 4, 4.1, 9. Design: `docs/design/play-*.png`.
**Owns:** `ui/screens/play/**`, `ui/board/**`, `games/minisudoku/MiniSudokuBoard.kt`, `games/nonogram/NonogramBoard.kt`, `nav/WitgridNavigation.kt`, `res/values/strings.xml` (play strings only), and their tests.
**Do:** Top bar: back, game name with "Pack · Level N" below, timer chip (tap to hide), info icon that opens the rules in a bottom sheet. The board is the largest square that fits the free width and height with 16 dp margins, so it never clips (today Mini Sudoku and Binary clip at font scale 1.3, and Nonogram runs off the right edge). Bottom tool bar of `ToolBarItem`s: Undo, Redo, Restart or the game's mode tools, Hint with a credit badge, Check. "Larger cells" becomes a zoom icon in the top bar. The Mini Sudoku digit pad (with a "left" count for each digit) and the Nonogram clues always stay visible. A status line under the board for in-puzzle counts (for example "2 of 7 crowns placed", mistakes).
**Done when:** `./gradlew check` passes. Screenshots of all four games on a 360 dp and a large phone, light and dark, font 1.0 and 1.3, show no clipping.

**Lead scope extension (17 Nov 2026):** Added the Mini Sudoku and Nonogram board implementations so U4 can expose their in-play controls while keeping the digit pad and clues visible, and `WitgridNavigation.kt` so the play route can receive the saved hint-credit balance for its badge. Board visual restyling remains with U5.
**Lead completion note (17 Nov 2026):** Integrated locally as `d75ea24`. `check` passed, including lint and validation of 3,864 puzzles across 24 bank files. Seven targeted instrumentation tests passed, including 32 screenshot cases covering four games, 360/432 dp, light/dark, and font scales 1.0/1.3. The lead reviewed all four contact sheets and representative full-size captures; no clipping was visible. GPT-6.1-Sol approved with no blockers.

### U5 — Board looks for each game
Tier M · Build Codex · Review Claude (lead screenshots)
Plan: 4.1. Design: `docs/design/play-*.png`.
**Owns:** `games/*/…Board.kt`, `games/*/…BoardOptions.kt`, the default of `AppSettings.crownsShowRegionAids` in `data/settings/PlayerData.kt` and `data/settings/DataStorePlayerDataRepository.kt`, and their tests.
**Do:** Crowns: region aids off by default, thin lines inside a region, thick lines between regions, a drawn crown, a soft X. Binary: a filled accent dot and an orange ring (`#C2622F` light, `#E9A27A` dark), not letters; letters only in TalkBack labels. Nonogram: clue band sized from the longest clue, active row and column tint, finished clues fade, 5-cell guide lines. Mini Sudoku: given digits darker than player digits, selected row, column and box tint, notes in small digits.
**Done when:** `./gradlew check` passes. Screenshots match the mockups in light and dark.
**Lead scope extension (17 Nov 2026):** Added `DataStorePlayerDataRepository.kt` because its explicit preference fallback overrides `AppSettings` for fresh installs; it must use the same off-by-default value for the Crowns region aids.
**Lead completion note (17 Nov 2026):** Integrated locally as `8e9e027`. Full `check` passed, including the fresh-default/persisted-opt-in test and validation of 3,864 bank puzzles. The viewport regression and U4 32-case layout matrix passed; U5 captured and reviewed all four boards in light and dark. GPT-6.1-Sol approved with no blockers.

### U6 — Home and Game screens
Tier M · Build Codex · Review Claude (lead screenshots)
Plan: 4.1, 9. Design: `docs/design/home-*.png`, `game-*.png`.
**Owns:** `ui/screens/home/**`, `ui/screens/game/**`, `ui/layout/ScreenPreviews.kt`, `nav/WitgridNavigation.kt`, `res/values/strings.xml` (home and game strings), and their tests.
**Do:** Home: "Today's puzzles" card (date, streak chip, one status chip for each game in a wrapping row, "Play daily <game>" button for the first unfinished game), then a 2-column grid of game cards (small drawn board preview, name, "Level N · Pack"), then one quiet Premium row. The grid works for any number of games. Game screen: Continue card (preview, "Level N", in-puzzle status), daily row with a done tick, pack cards with name, board size and "Next: level N", and one quiet row "N hints and N checks saved · Get more" that opens a sheet with the two rewarded offers. Remove `home_progress`, `game_pack_progress`, the "x of 4 daily puzzles" text, and every other level total. Remove the unused `onHome`, `onOpenStatistics` and `onOpenSettings` parameters (U2 moved those actions into the top bar).
**Done when:** `./gradlew check` passes. Screenshots match the mockups. No level total appears on these screens.
**Lead scope extension (17 Nov 2026):** Added `WitgridNavigation.kt` so Home can receive per-game daily completion state and open that game's daily puzzle directly, Game can receive saved hint/check balances, and obsolete callback arguments can be removed from their route call sites.
**Lead completion note (17 Nov 2026):** Integrated locally as `303c67f` after the U6 builder commit and GPT-6.1-Sol review. The full `check`, debug APK assembly, and Android-test assembly passed; `check` validated 3,864 puzzles in 24 files. Three focused emulator tests passed. I reviewed eight Home/Game screenshots across light/dark themes and font scales 1.0/1.3. Review fixes corrected credit plurals, added the approved rules/calendar/continue/credits/Premium cues, and read board-size ranges from bank envelopes instead of decoding every puzzle. Captures: `D:\cash-grabs\witgrid-wt\U6\build\u6-screens\contact-sheet-final.jpg`.

### U7 — Result, Failed, Calendar, Statistics, Settings
Tier M · Build Codex · Review Claude (lead screenshots)
Plan: 4.1, 9. Design: `docs/design/result-*.png`, `settings-*.png`.
**Owns:** `ui/screens/result/**` (not `ResultNavigation.kt`, which is U9), `ui/screens/failed/**`, `ui/screens/calendar/**`, `ui/screens/stats/**`, `ui/screens/settings/**`, `ads/flow/RewardedActionButton.kt`, `nav/WitgridNavigation.kt`, `ui/layout/ScreenPreviews.kt`, `app/src/androidTest/kotlin/com/datathecodie/witgrid/screenshots/ScreenshotCaptureTest.kt`, `res/values/strings.xml` (these screens' strings), and their tests.
**Do:** Result: close icon, solved-board thumbnail, "Solved", "Game · Pack · Level N", Time / Best / Hints tiles, best-time banner, "Next level" (primary) and "Back to packs". Level failed: the same frame with Continue and Restart. Calendar: month grid with done and missed marks, today's daily puzzles as one list. Statistics: one card for each game with `StatTile`s (a solved count is permitted; no "of total"). Settings: theme as a segmented control, colourway swatches, assist switches with one-line subtitles, a "More" list (reminder, Premium, restore purchase, privacy, licences). Remove the unused `onHome` parameters.
**Done when:** `./gradlew check` passes. Screenshots match the mockups.
**Lead scope extension (17 Nov 2026):** Added `WitgridNavigation.kt` for Result/Failed framing and pack-return routing after removing old Home callbacks; added `ScreenPreviews.kt` and `ScreenshotCaptureTest.kt` because they call the screens whose U7 APIs are being rebuilt.
**Lead scope extension (18 Nov 2026):** Added `ads/flow/RewardedActionButton.kt` so Level Failed can use the approved 54 dp primary-button treatment while retaining the shared rewarded and Premium-offer behavior. Its new styling option defaults off for existing callers.

### U8 — Screenshot matrix and visual QA gate
Tier M · Build Codex · Review Claude (lead)
**Owns:** `scripts/screenshots/**`, `app/src/androidTest/**/screenshots/**`.
**Do:** Extend the screenshot script to capture every screen (Welcome, Home, Game, the four Play boards, Result, Level failed, Calendar, Statistics, Settings) in light and dark, at font scale 1.0, 1.3 and 2.0, on a small (360 dp) and a large emulator profile. Build one contact sheet for each profile. Update the Play Store capture set to the new UI.
**Done when:** The lead runs the script and reviews the sheets. This review is required before every closed-test upload.

### U9 — Endless level numbers and looping
Tier H · Build Codex · Review Claude
Plan: 2 ("Level count"), 4 (principle 7), 7.5 ("Endless levels").
**Owns:** `games/GameModule.kt` (`LevelRef`), `play/PlayController.kt`, `data/progress/**` (with a Room migration if the schema changes), `ui/screens/result/ResultNavigation.kt`, `ui/screens/home/HomeViewModel.kt`, `ui/screens/home/HomeScreen.kt`, `ui/screens/game/GameViewModel.kt`, `ui/screens/game/GameScreen.kt`, `ui/layout/ScreenPreviews.kt`, `res/values/strings.xml` (level labels only), and their tests.
**Do:** Give each non-daily attempt a player-facing level number N that only goes up for each game and pack. Level N plays bank puzzle `(N − 1) mod packSize`. "Next" after the last bank puzzle goes to level N + 1, which plays bank puzzle 0 again from an empty board; `nextResultLevel` never returns null for a non-daily level. Persist attempt identity separately from bank puzzle identity so a later loop never restores the earlier solved board. Keep best time per bank puzzle. Existing saves keep working (a player on bank puzzle k is on level k + 1). Home and Game view models and screens show the next level number for each game and pack, not completed/total counts, percentages or progress bars; U6 can then restyle those screens without changing the level model.
**Done when:** `./gradlew check` passes. Unit tests cover the level-to-bank mapping, wrap from the last puzzle to the first, level numbers above the pack size, fresh state for repeated bank puzzles, and existing progress.
**Lead scope clarification (17 Nov 2026):** Added the play controller, Home/Game composables and preview, and level strings because attempt identity and the replacement level labels cross those files. The Home/Game screens must show the next "Level N · Pack" and remove level totals; U6 can then restyle those screens on top of the new level model. A repeated bank puzzle must start as a new attempt while the best time remains keyed to the bank puzzle.
**Lead completion note (17 Nov 2026):** Integrated locally as `450af76`. `check` passed, including 199 app unit tests, lint, and validation of 3,864 puzzles across 24 bank files. GPT-6.1-Sol reviewed the complete diff and approved with no blockers. The guided Easy intro stays unnumbered and outside regular bank progression; its Next action opens regular Level 1.

## Phase 5 — Closed test

### S1 — Store listing and policy form drafts
Tier M · Build Claude (`builder`) · Review Codex
Plan: 8.7, 13.
**Owns:** `docs/store/**`.
**Do:** Title, short and full description, release notes, Data safety answers draft, content rating answers draft, ads declaration. Check the name rules.
**Done when:** Character limits are met. The lead approves the text.
**Lead completion note (17 Nov 2026):** Draft listing and policy forms are in `docs/store/`. The title (30), short description (75), full description (1,373), and release notes (134) are within current Play limits. GPT-6.1-Sol review approved after the Nonogram wording was aligned with the plan; `check` passes. Final Play Console declarations await H0.1, H4, M7, and release-bundle review.

### S2 — Screenshot capture script
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
**Owns:** `scripts/screenshots/**`, `app/src/androidTest/**/screenshots/**`.
**Do:** A script that captures 8 screens (one board per game, dark mode, result, home, offline message) on an emulator at Play sizes.
**Done when:** The lead runs it and gets the image files.
**Lead completion note (17 Nov 2026):** The lead ran the script on the Pixel 6a Play emulator; all eight 1080×1920 captures were committed. GPT-6.1-Sol reviewed and approved the task.

### S3 — Release build configuration
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
**Owns:** release parts of `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/com/datathecodie/witgrid/ads/GoogleAdService.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ads/TestAdIds.kt`.
**Do:** R8 on, rules for Room, Billing, Ads, Firebase. Version code scheme. Gradle may consume owner-provided `keystore.properties` if present; agents must never open, print or commit signing files or values. Debug and closed-test builds always use Google's test ad IDs; live IDs activate only for production builds from Gradle properties outside the repo.
**Done when:** `./gradlew :app:bundleRelease` builds an unsigned bundle when no keystore is present.
  **Lead scope clarification (17 Nov 2026):** Manifest and ad-service files are included so S3 can implement the required test-versus-production ID selection within its Owns boundary.
  **Lead status note (17 Nov 2026):** Release signing activates only when all four owner signing properties are nonblank; R8 and resource shrinking are enabled. `:app:bundleRelease` succeeds unsigned with blank signing overrides, `jarsigner -verify` confirms it is unsigned, and `check` passes. GPT-6.1-Sol review found no remaining signing-config blocker. S3 remains open: production-only ad IDs, M7 Firebase rules, H4 consent verification, and M6/M7 dependencies remain.

### H5 — Art, keystore, closed test, production access (Human)
Plan: 11 phase 5, 4 (art).
**Lead progress note (17 Nov 2026):** Launcher/Play icon and 1024×500 feature-graphic drafts are committed. The graphic uses first Easy puzzle examples from the committed bank; GPT-6.1-Sol found no visual/spec blockers. Human artwork/screenshot approval, keystore, closed-test upload, and production-access steps remain.
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
