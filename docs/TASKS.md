# Witgrid task backlog (v2)

Each card is one unit of work for one agent in one worktree. The lead updates this file. Agents never edit it.

The v1 backlog (Phases 0 to 7 and the device-play polish cards, all launch build work) is in `docs/archive/TASKS-v1.md`. Read a v1 card there when a v2 card says "same pattern as v1".

- **Tier**: L, M, H or Human. See `docs/AGENT_WORKFLOW.md` section 2.
- **Build / Review**: the default agent family. You can swap the families. Keep the reviewer from another family. Haiku and Grok have no shell, so they cannot do xB (bank) cards.
- **Status**: `todo` → `ready` (all needs done) → `active` → `review` → `done`. `blocked` if waiting on a person.
- Path short forms: `engine/…` = `engine/src/{main,test}/kotlin/com/datathecodie/witgrid/engine/…`; `app/…` = `app/src/{main,test,androidTest}/kotlin/com/datathecodie/witgrid/…`; `bankgen/…` = `bankgen/src/{main,test}/kotlin/com/datathecodie/witgrid/bankgen/…`.

## Index

| ID | Title | Tier | Build | Review | Needs | Status |
|---|---|---|---|---|---|---|
| **Launch (remaining)** |||||||
| H0.1 | Accounts, legal, website | Human | — | — | — | ready |
| H1 | Hand-test the bank | Human | — | — | — | ready |
| H4 | Firebase project, AdMob units, licence tester | Human | — | — | H0.1 Play Console and AdMob accounts only | ready |
| M6 | Premium purchase and restore | H | Claude | Codex | H4 | blocked |
| M7 | Analytics, Crashlytics, Remote Config | M | Codex | Claude | M6, H4 | todo |
| S3 | Release build configuration | M | Codex | Claude | M6, M7 | todo |
| H5 | Art, keystore, closed test, production access | Human | — | — | S3 | todo |
| H7 | Hand-test the launch catalog | Human | — | — | — | ready |
| H6 | Live ad units, staged rollout, weekly review | Human | — | — | H5, H7 | todo |
| RN1 | Display renames: Number Link, Solitaire, Chess Puzzles, Towers | L | Claude (lead) | — | — | done |
| **Expansion foundations** |||||||
| F4 | Home at scale: category sections | M | Codex | Claude | — | ready |
| F1 | Card kit out of Klondike | M | Codex | Claude | — | ready |
| F2 | Family C `MatchEngine` contract | H | Codex | Claude + human | — | ready |
| F3 | Opponent AI kit | H | Codex | Claude | F2 | todo |
| H8 | Pick the word list and licence | Human | — | — | — | ready |
| F5 | Word list asset | M | Codex | Claude | H8 | todo |
| **Wave 1 — Solo games** |||||||
| NB1–NB3 | Nuts & Bolts engine | see wave lane table | | | — | NB1 ready |
| NBA | Nuts & Bolts board | M | Gemini | Codex | NB1 | todo |
| NBB | Nuts & Bolts bank | L | Codex | Claude | NB2 | todo |
| NBC | Nuts & Bolts intro, extra bolt, config | L | Claude | Codex | NBA, NBB, F4 | todo |
| MI1–MI3 | Minesweeper engine | see wave lane table | | | — | MI1 ready |
| MIA | Minesweeper board | M | Gemini | Codex | MI1 | todo |
| MIB | Minesweeper bank | L | Codex | Claude | MI2 | todo |
| MIC | Minesweeper intro, continue, config | L | Claude | Codex | MIA, MIB, F4 | todo |
| MP1–MP3 | Maze Paint engine | see wave lane table | | | — | MP1 ready |
| MPA | Maze Paint board | M | Gemini | Codex | MP1 | todo |
| MPB | Maze Paint bank | L | Codex | Claude | MP2 | todo |
| MPC | Maze Paint intro, config | L | Claude | Codex | MPA, MPB, F4 | todo |
| NM1–NM3 | Number Merge engine | see wave lane table | | | — | NM1 ready |
| NMA | Number Merge board | M | Gemini | Codex | NM1 | todo |
| NMB | Number Merge bank | L | Codex | Claude | NM2 | todo |
| NMC | Number Merge intro, continue, config | L | Claude | Codex | NMA, NMB, F4 | todo |
| SL1–SL3 | Sliding Puzzle engine | see wave lane table | | | — | SL1 ready |
| SLA | Sliding Puzzle board | M | Gemini | Codex | SL1 | todo |
| SLB | Sliding Puzzle bank | L | Codex | Claude | SL2 | todo |
| SLC | Sliding Puzzle intro, config | L | Claude | Codex | SLA, SLB, F4 | todo |
| ME1–ME3 | Memory Pairs engine | see wave lane table | | | — | ME1 ready |
| MEA | Memory Pairs board | M | Gemini | Codex | ME1 | todo |
| MEB | Memory Pairs bank | L | Codex | Claude | ME2 | todo |
| MEC | Memory Pairs intro, continue, config | L | Claude | Codex | MEA, MEB, F4 | todo |
| BE1–BE3 | Block Escape engine | see wave lane table | | | — | BE1 ready |
| BEA | Block Escape board | M | Gemini | Codex | BE1 | todo |
| BEB | Block Escape bank | L | Codex | Claude | BE2 | todo |
| BEC | Block Escape intro, config | L | Claude | Codex | BEA, BEB, F4 | todo |
| YA1–YA3 | Yatzy engine | see wave lane table | | | — | YA1 ready |
| YAA | Yatzy board | M | Gemini | Codex | YA1 | todo |
| YAB | Yatzy bank | L | Codex | Claude | YA2 | todo |
| YAC | Yatzy intro, extra reroll, config | L | Claude | Codex | YAA, YAB, F4 | todo |
| SH1–SH4 | Shikaku lane | see wave lane table | | | — | SH1 ready |
| SHA | Shikaku board | M | Gemini | Codex | SH2 | todo |
| SHB | Shikaku bank | L | Codex | Claude | SH3 | todo |
| SHC | Shikaku intro, config, strings | L | Claude | Codex | SHA, SHB, F4 | todo |
| SP1–SP3 | Spider Solitaire engine | see wave lane table | | | F1 | todo |
| SPA | Spider Solitaire board | M | Codex | Claude | SP1, F1 | todo |
| SPB | Spider Solitaire winnable-deal bank | L | Codex | Claude | SP2 | todo |
| SPC | Spider Solitaire intro, extra undo, config | L | Claude | Codex | SPA, SPB, F4 | todo |
| FC1–FC3 | FreeCell engine | see wave lane table | | | F1 | todo |
| FCA | FreeCell board | M | Codex | Claude | FC1, F1 | todo |
| FCB | FreeCell winnable-deal bank | L | Codex | Claude | FC2 | todo |
| FCC | FreeCell intro, extra undo, config | L | Claude | Codex | FCA, FCB, F4 | todo |
| H9 | Hand-test Wave 1 games | Human | — | — | Wave 1 xC cards | todo |
| **Wave 2 — Against the computer** |||||||
| TT1–TT3 | Tic-Tac-Toe engine and AI | see wave lane table | | | F2, F3 | todo |
| TTA | Tic-Tac-Toe board | M | Gemini | Codex | TT1 | todo |
| TTC | Tic-Tac-Toe intro, config | L | Claude | Codex | TTA, TT2 | todo |
| FR1–FR3 | Four in a Row engine and AI | see wave lane table | | | F2, F3 | todo |
| FRA | Four in a Row board | M | Gemini | Codex | FR1 | todo |
| FRC | Four in a Row intro, config | L | Claude | Codex | FRA, FR2 | todo |
| MA1–MA3 | Mancala engine and AI | see wave lane table | | | F2, F3 | todo |
| MAA | Mancala board | M | Gemini | Codex | MA1 | todo |
| MAC | Mancala intro, config | L | Claude | Codex | MAA, MA2 | todo |
| RV1–RV3 | Reversi engine and AI | see wave lane table | | | F2, F3 | todo |
| RVA | Reversi board | M | Gemini | Codex | RV1 | todo |
| RVC | Reversi intro, config | L | Claude | Codex | RVA, RV2 | todo |
| DB1–DB3 | Dots and Boxes engine and AI | see wave lane table | | | F2, F3 | todo |
| DBA | Dots and Boxes board | M | Gemini | Codex | DB1 | todo |
| DBC | Dots and Boxes intro, config | L | Claude | Codex | DBA, DB2 | todo |
| SB1–SB3 | Sea Battle engine and AI | see wave lane table | | | F2, F3 | todo |
| SBA | Sea Battle board | M | Gemini | Codex | SB1 | todo |
| SBC | Sea Battle intro, config | L | Claude | Codex | SBA, SB2 | todo |
| CK1–CK3 | Checkers engine and AI | see wave lane table | | | F2, F3 | todo |
| CKA | Checkers board | M | Gemini | Codex | CK1 | todo |
| CKC | Checkers intro, config | L | Claude | Codex | CKA, CK2 | todo |
| CH1–CH3 | Chain Burst engine and AI | see wave lane table | | | F2, F3 | todo |
| CHA | Chain Burst board | M | Gemini | Codex | CH1 | todo |
| CHC | Chain Burst intro, config | L | Claude | Codex | CHA, CH2 | todo |
| HE1–HE3 | Hearts engine and AI | see wave lane table | | | F1, F2, F3 | todo |
| HEA | Hearts board | M | Codex | Claude | HE1 | todo |
| HEC | Hearts intro, config | L | Claude | Codex | HEA, HE2 | todo |
| SD1–SD3 | Spades engine and AI | see wave lane table | | | F1, F2, F3 | todo |
| SDA | Spades board | M | Codex | Claude | SD1 | todo |
| SDC | Spades intro, config | L | Claude | Codex | SDA, SD2 | todo |
| CE1–CE3 | Crazy Eights engine and AI | see wave lane table | | | F1, F2, F3 | todo |
| CEA | Crazy Eights board | M | Codex | Claude | CE1 | todo |
| CEC | Crazy Eights intro, config | L | Claude | Codex | CEA, CE2 | todo |
| DO1–DO3 | Dominoes engine and AI | see wave lane table | | | F2, F3 | todo |
| DOA | Dominoes board | M | Gemini | Codex | DO1 | todo |
| DOC | Dominoes intro, config | L | Claude | Codex | DOA, DO2 | todo |
| LU1–LU3 | Ludo engine and AI | see wave lane table | | | F2, F3 | todo |
| LUA | Ludo board | M | Gemini | Codex | LU1 | todo |
| LUC | Ludo intro, config | L | Claude | Codex | LUA, LU2 | todo |
| CC1–CC3 | Chess engine and AI | see wave lane table | | | F2, F3 | todo |
| CCA | Chess board | M | Codex | Claude | CC1 | todo |
| CCC | Chess intro, config | L | Claude | Codex | CCA, CC2 | todo |
| H10 | Hand-test Wave 2 games | Human | — | — | Wave 2 xC cards | todo |
| **Wave 3 — Word games** |||||||
| WS1–WS3 | Word Search engine | see wave lane table | | | F5 | todo |
| WSA | Word Search board | M | Gemini | Codex | WS1 | todo |
| WSB | Word Search bank | L | Codex | Claude | WS2 | todo |
| WSC | Word Search intro, config | L | Claude | Codex | WSA, WSB, F4 | todo |
| HM1–HM3 | Secret Word engine | see wave lane table | | | F5 | todo |
| HMA | Secret Word board | M | Gemini | Codex | HM1 | todo |
| HMB | Secret Word bank | L | Codex | Claude | HM2 | todo |
| HMC | Secret Word intro, continue, config | L | Claude | Codex | HMA, HMB, F4 | todo |
| H11 | Hand-test Wave 3 games | Human | — | — | Wave 3 xC cards | todo |

---

## Launch (remaining)

Plan section 11. These v1 cards are copied without change.

### H0.1 — Accounts, legal, website (Human)
Plan: 11 (phase 0), 8.7, 15.
- [ ] Search Google and the Play Store for "Witgrid". Record the date and any game or app with the same or a very similar name.
- [ ] Open the Play Console account. Complete developer verification. Check which personal details Play shows.
- [ ] Open the AdMob account. Do **not** create live ad units in code yet.
- [x] Make the developer website: privacy policy page and `app-ads.txt`.
**Done when:** The Google and Play Store name search is dated and recorded; Play Console and AdMob accounts are verified; the public HTTPS privacy policy and `app-ads.txt` URLs are live. Send the public privacy-policy URL and support email to the lead so the app setting and store drafts can be finalized. Record public URLs and account status, never credentials.
**Lead progress note (17 Nov 2026):** The privacy policy at `https://datathecodie.com/witgrid/privacy-policy.html` and `https://datathecodie.com/app-ads.txt` are live and were verified on 17 Nov; the policy matches the committed page and the Settings URL.

**Lead progress note (18 Nov 2026):** An initial USPTO Wordmark query for "Witgrid" returned 0 records on the [official Trademark Search](https://tmsearch.uspto.gov/search/) with all statuses included.

**Owner decision (18 Nov 2026):** No formal trademark check (USPTO, EUIPO, UKIPO) is required. A Google and Play Store search for the name is enough. The GDD section 7 export is dropped: no Witgrid source exists, and `DEVELOPMENT_PLAN.md` is the source of truth. Still open: the name search, Play Console verification and AdMob account.

### H1 — Hand-test the bank (Human)
**Owns:** `docs/reports/bank-hand-test.md`.
**Do:** Solve 20 puzzles for each of the four games in each of the four packs (320 solves) using the first app build. Record puzzle ID, elapsed time, whether a guess was needed, and any grade or clue issue. Give issue details to the lead for bug-card creation; do not edit `docs/TASKS.md` directly.
**Done when:** The report has 20 records for every game/pack pair; the lead creates a bug card for every issue found before H1 closes. If no issues were found, record that explicitly.
**Lead prep note (17 Nov 2026):** The log is prefilled with the first 20 regular bank IDs in app order for each game/pack pair (320 IDs total). H1 remains ready until the human records all solves and any issues.

### H4 — Firebase project, AdMob units, licence tester (Human)
Needs only the verified Play Console and AdMob accounts from H0.1. The name search does not block H4 (owner decision, 18 Nov 2026). Do the licence tester and Premium product first: they unblock M6.
- [ ] Create the Firebase project, register this Android app, and enable Analytics, Crashlytics, and Remote Config. Put `google-services.json` in `app/` locally (it is git-ignored).
- [ ] Create the AdMob app and ad units. Store the IDs in `~/.gradle/gradle.properties`, not in the repo.
- [ ] Configure and publish the required UMP consent message(s) in AdMob. S3 verifies the in-app flow after wiring the owner app ID; keep Google test app and ad unit IDs in test builds.
- [ ] Create the one-time non-consumable Play product `witgrid_premium_lifetime` at $7.99 and add your Google account as a Play licence tester.
**Done when:** Firebase is registered for this Android app and its local config remains uncommitted; AdMob has the app, interstitial/rewarded units, and published consent messages; the Play product exists at the planned price and the licence tester is enabled. Keep Google test ad IDs in all debug and closed-test builds; production IDs stay outside the repository.

### M6 — Premium purchase and restore
Tier H · Build Claude (`builder`) · Review Codex
Plan: 8.5.
**Owns:** `app/…/billing/**`, `app/…/ads/flow/**`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/settings/SettingsScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/home/HomeScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/game/GameScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/failed/LevelFailedScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/ui/screens/calendar/CalendarScreen.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/nav/WitgridNavigation.kt`, `app/src/main/res/values/strings.xml`.
**Do:** `BillingService` interface, real and fake. Add Google Play Billing Library 9.1.0 as the only new library for this card. Implement one non-consumable product with ID `witgrid_premium_lifetime`; show its localized Play price. Acknowledge completed purchases. Keep the local flag working offline and re-check owned purchases when connected. Restore in Settings. Handle pending purchases without granting Premium early. Make the Premium offer actionable from Home, Settings, unavailable rewarded offers, failed-level Continue, and streak repair. H4 creates the Play product at the plan's $7.99 price and supplies the licence tester.
**Done when:** `./gradlew check` passes. The lead buys and restores with a licence-test account.
  **Lead status note (17 Nov 2026):** Code and review are complete. Waiting on H4's Play product and licence tester for the required device purchase and restore check.

### M7 — Analytics, Crashlytics, Remote Config
Tier M · Build Codex (`gpt-6.1-sol`) · Review Claude
Plan: 8.6, 10.
**Owns:** `app/…/analytics/**`, `app/…/play/**`, `app/…/nav/WitgridNavigation.kt`, `app/…/ui/screens/home/**`, `app/…/ui/screens/play/**`, `app/…/ui/screens/result/**`, `app/…/ui/screens/failed/**`, `app/…/ui/screens/calendar/**`, `app/…/ui/screens/settings/**`, `app/…/ads/flow/**`, `app/…/billing/**`, `app/…/domain/daily/**`, `app/…/MainActivity.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerData.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/PlayerDataRepository.kt`, `app/src/main/kotlin/com/datathecodie/witgrid/data/settings/DataStorePlayerDataRepository.kt`, `app/src/main/res/values/strings.xml`, `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`.
**Do:** `Analytics` interface with Firebase and fake implementations. Instrument every event in plan 10 at its source. Analytics and Crashlytics are disabled by default; enable them only after explicit Settings opt-in and disable them when consent is withdrawn. This is separate from UMP ad consent. Remote Config bound to `Config` from M1, in-app defaults first. The build works when `google-services.json` is missing (CI and agents): use the fake.
**Done when:** `./gradlew check` passes in CI without `google-services.json`.

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

### H7 — Hand-test the launch catalog (Human)
**Owns:** `docs/reports/catalog-hand-test.md`.
**Do:** For each of the 10 new games, solve 5 puzzles in each of the 4 packs (200 solves), plus 2 daily puzzles each. Record puzzle ID, time, whether a guess was needed (Family A) or the level was winnable (Family B), and any issue. Give issue details to the lead.
**Done when:** The report has 20 records for every new game, and the lead has a bug card for each issue. H6 waits for H7.

### H6 — Live ad units, staged rollout, weekly review (Human)
Plan: 11 phase 6, 3.


### RN1 — Display renames
Tier L · Build Claude (lead)
Plan: 12, 13.
**Done (27 Nov 2026):** `game_path` Number Link, `game_klondike` Solitaire, `game_tactics` Chess Puzzles, `game_skyscrapers` Towers, plus copy that names those games. Engine ids, packages, bank files and config keys unchanged. Colour Sort keeps its name; "water sort" is a store keyword only.

---

## Expansion foundations

Plan 17.2. F4 must merge before the first Wave 1 xC card registers a game.

### F4 — Home at scale: category sections
Tier M · Build Codex · Review Claude
Plan: 17.2, 4, 4.1, 9.
**Owns:** `app/…/ui/screens/home/**`, `app/…/games/GameModule.kt`, `app/…/games/PlayGameModule.kt`, `app/…/games/GameRegistry.kt` (category field only), `app/src/main/res/values/strings.xml` (new keys only), their tests.
**Do:** Add `category` to each game module: Logic, Sort and Move, Cards, Board, Word. Set it for the 14 launch games (Colour Sort, Arrows: Sort and Move; Solitaire: Cards; the rest: Logic). Home shows one titled section for each category that has a game, each a 2-column grid of the existing game cards, in registry order. Hide an empty category. No count in any text. Today's puzzles card stays first.
**Done when:** `./gradlew :app:testDebugUnitTest` passes. A unit test shows that an empty category is hidden and that every registered game is in exactly one section.

### F1 — Card kit out of Klondike
Tier M · Build Codex · Review Claude
Plan: 17.2, 12.5.
**Owns:** new `engine/…/cards/**`, new `app/…/games/cards/**`, `engine/…/klondike/**`, `app/…/games/klondike/**`, their tests.
**Do:** Move `Card`, `Suit`, `Rank`, deck build and `SplitMix64` shuffle to `engine/…/cards/`. Move the code-drawn card face and back, pile layout, run drag-and-drop and the fly-to-pile animation to `app/…/games/cards/`. Klondike uses the kit. The deck shuffle must give the same order as today for the same seed, so the Klondike bank stays valid.
**Done when:** `./gradlew :engine:test :app:testDebugUnitTest` pass. `./gradlew :bankgen:run --args="validate --all"` passes with no bank change. No visible change to Solitaire.

### F2 — Family C `MatchEngine` contract
Tier H · Build Codex (high) · Review Claude + human
Plan: 17.2, 17.4, 7.3, 8.4.
**Owns:** new `engine/…/core/MatchEngine.kt` (and neighbouring core types), its tests, new `app/…/games/MatchGameModule.kt`, new `app/…/games/MatchRegistry.kt`, the Play, Result, Level failed and Statistics code that must show Won / Lost / Draw, and their tests. Do not change `GameEngine` or `PlayEngine`.
**Do:** `MatchEngine<P, S, A>`: new match from a seed and a strength, players and side to move, legal actions, apply, outcome (ongoing, win, loss, draw), computer move hook (filled by F3), hint hook, codec. `MatchGameModule` wraps it for Home, Play, Result, saves and endless levels (a level = one match, packs = strengths). Loss → Level failed: Continue takes back the last full turn (one rewarded ad, or free with Premium); Restart is free. No bank and no daily puzzle; Home daily row skips Family C. Interstitial after "Next" as plan 8.1. A tiny fake game (for example Nim) proves the contract. Human reads the contract before merge.
**Done when:** `./gradlew :engine:test :app:testDebugUnitTest` pass with the fake game. The 14 launch games still play in unit tests.

### F3 — Opponent AI kit
Tier H · Build Codex (high) · Review Claude
Plan: 17.2, 17.4.
**Owns:** new `engine/…/ai/**` and its tests.
**Do:** (1) Alpha-beta with iterative deepening, move ordering and an integer evaluation function supplied by the game. (2) Determinised Monte Carlo playouts for hidden-card and dice games, `SplitMix64` only. Both stop on a **node budget**, never on a clock, so a seed gives the same move on every machine. Strength levels Easy, Medium, Hard, Expert = budget plus a chance to pick a weaker move. A harness plays strength against strength for N games and prints win rates.
**Done when:** `./gradlew :engine:test` passes. With the F2 fake game, Expert never loses a won position, and each strength beats the one below in more than 60 % of 100 harness games.

### H8 — Pick the word list and licence (Human)
Plan: 17.2, 17.5.
**Do:** Choose an English word list with a licence that allows use in a paid app, for example a public-domain or permissive list. Record the source, version and licence in `docs/decisions/word-list.md`.

### F5 — Word list asset
Tier M · Build Codex · Review Claude
Plan: 17.2, 17.5.
**Owns:** new `engine/…/words/**`, new `bankgen` word command, `app/src/main/assets/words/**`, the licence screen entry, their tests.
**Do:** Import the H8 list. Remove profanity, slurs and proper nouns with a reviewed block list. Bands by length (3 to 12). Theme lists for Word Search (animals, food, nature, …) from a hand-made file. Gzip asset under 1 MB.
**Done when:** `./gradlew :engine:test :bankgen:test` pass. A test shows no block-list word is in the asset.

---

## Waves 1 to 3

Plan 17.3, 17.4, 17.5. Same lane pattern as the v1 launch catalog (`docs/archive/TASKS-v1.md`, "Hub Family A lanes" and "Hub Family B lanes"). Each lane is one package: `engine/…/<id>/**` and `app/…/games/<id>/**`.

### Wave lane table

| Lane | Engine id | Family | Plan | Actions or input | Generator or AI | x2 tier |
|---|---|---|---|---|---|---|
| NB | `nutsbolts` | B | 17.3 | `MoveNuts(from, to)` | Copy `coloursort` as a new package (do not edit it). Reverse-shuffle, solver re-sorts. 4 / 6 / 8 / 10 bolts. | M |
| MI | `minesweeper` | B | 17.3 | `Reveal(cell)`, `Flag(cell)` | Seeded mines plus a given opening; a deduction solver clears the board with no guess. | H |
| MP | `mazepaint` | B | 17.3 | `Roll(direction)` | Random walls, BFS solution, pack by solution length. No fail. | M |
| NM | `numbermerge` | B | 17.3 | `Swipe(direction)` | Spawns from state `SplitMix64`. Target tile per pack. Fail when no swipe moves. | M |
| SL | `sliding` | B | 17.3 | `SlideTile(cell)` | Random solvable permutation (parity), pack by bounded move count. | M |
| ME | `memory` | B | 17.3 | `Flip(cell)` | Seeded layout; flip limit in harder packs. | L |
| BE | `blockescape` | B | 17.3 | `SlideBlock(block, steps)` | Random layout, BFS minimum moves, pack by move count. No fail. | H |
| YA | `yatzy` | B | 17.3 | `Roll(keepMask)`, `Score(category)` | Dice from state `SplitMix64`. Pack target score; tune targets with a simulation of a simple strategy (report). | M |
| SH | `shikaku` | A | 17.3 | `SetDigit` = rectangle id | Unique and technique-solvable. Techniques: single fitting rectangle, cell reachable by one clue, overlap. 7 / 10 / 12 / 15. | H |
| SP | `spider` | B | 17.3 | `CardMove`, `DealRow` | F1 kit. Seed deal, solver must win (`SolveVerdict`: Unknown is never a loss). 1 / 2 / 4 / 4 suits. | H |
| FC | `freecell` | B | 17.3 | `CardMove` | F1 kit. Seed deal, solver must win. Pack by solution length. | H |
| TT | `tictactoe` | C | 17.4 | `Place(cell)` | F3 alpha-beta. 3x3, 5x5 four-in-line, 7x7 five-in-line. | M |
| FR | `fourinrow` | C | 17.4 | `Drop(column)` | F3 alpha-beta, 7x6. | M |
| MA | `mancala` | C | 17.4 | `Sow(pit)` | F3 alpha-beta, Kalah 6 pits 4 seeds. | M |
| RV | `reversi` | C | 17.4 | `Place(cell)`, `Pass` | F3 alpha-beta with a position table, 8x8. | H |
| DB | `dotsboxes` | C | 17.4 | `DrawLine(edge)` | F3 alpha-beta with chain counting. 4x4 to 6x6 boxes. | H |
| SB | `seabattle` | C | 17.4 | `PlaceFleet`, `Fire(cell)` | Seeded computer fleet. Hunt-and-target AI with probability counts. 10x10. | M |
| CK | `checkers` | C | 17.4 | `Move(path)` | English draughts, forced capture. F3 alpha-beta. | H |
| CH | `chainburst` | C | 17.4 | `Place(cell)` | Critical-mass explosions, 2 players. F3 alpha-beta, shallow. | M |
| HE | `hearts` | C | 17.4 | `Pass(cards)`, `Play(card)` | F1 kit, 3 computer players, F3 Monte Carlo. | H |
| SD | `spades` | C | 17.4 | `Bid(n)`, `Play(card)` | F1 kit, partners, bidding, F3 Monte Carlo. | H |
| CE | `crazyeights` | C | 17.4 | `Play(card)`, `Draw` | F1 kit plus our own action cards (original names and art). F3 Monte Carlo. | H |
| DO | `dominoes` | C | 17.4 | `Place(tile, end)`, `Draw` | Draw game, double-six set. F3 Monte Carlo. | H |
| LU | `ludo` | C | 17.4 | `Roll`, `MoveToken(token)` | 4 players, dice from `SplitMix64`, F3 Monte Carlo or rule AI. | H |
| CC | `chess` | C | 17.4 | `Move(from, to)` | Reuse `tactics` move rules (read-only; do not edit `tactics`). F3 alpha-beta with a node budget. | H |
| WS | `wordsearch` | B | 17.5 | `Select(start, end)` | F5 theme lists. Words in allowed directions, filler letters with no extra block-list words. | M |
| HM | `hangman` | B | 17.5 | `Guess(letter)` | F5 word bands by pack. Six wrong letters fail. | L |

### Engine cards

| Card | Family A (SH) | Family B | Family C | Tier |
|---|---|---|---|---|
| x1 | Rules, codec, solution counter | Rules, state, actions, `isWon` / `isLost`, codec. `isLost` is false at deal start. | Rules, state, actions, outcome, codec, on F2 | M |
| x2 | Technique solver, grader, hints | Hint action (first move of a shortest win where a solver exists), packs, generator. Same seed, same output. | F3 AI at four strengths, hint = Expert move. Write `docs/reports/ai-<id>.md`: harness win rates and the slowest move time. | see lane table |
| x3 | Generator | Independent spec tests from plan 17 with hand-made fixtures (`engine/…/<id>/spec/**` only) | Independent spec tests from plan 17 (`engine/…/<id>/spec/**` only) | L |
| x4 | Independent spec tests (`engine/…/<id>/spec/**` only) | — | — | L |

**Done when (each engine card):** `./gradlew :engine:test` passes. Family B x2: seed 1 gives 10 winnable levels in each pack, a hints-only test reaches a win. Family C x2: each strength beats the one below in more than 60 % of 100 harness games; Expert move under 1 s on the harness PC with the node budget.

### App cards

**xA — board.** Tier M. **Owns:** `app/…/games/<id>/**`. Code-drawn board in the paper-and-ink style; TalkBack labels; reduce-motion keeps snaps; haptics from Settings (`Local*BoardOptions`). **Done when:** `./gradlew :app:testDebugUnitTest` passes and a test plays a fixture to the end.

**xB — bank (Family A and B only).** Tier L. Codex or Gemini only. **Owns:** `app/src/main/assets/bank/<id>-*.txt.gz`, `docs/reports/bank-<id>.md`, the game's line in `engine/…/Registry.kt` (Family A) or `bankgen/…/FamilyBRegistry.kt` (Family B) and its registry test. 4×150 + 365 daily. Seeds, counts, sizes, histograms and a spread line in the report. **Done when:** `./gradlew :bankgen:run --args="validate --all"` passes.

**xC — intro, Continue, config, registration.** Tier L. **Owns:** `app/…/games/<id>/Intro.kt`, `app/src/main/assets/intro/<id>-intro.txt` (A and B), `app/src/main/res/values/strings.xml` (new keys only), `GameRegistry` / `PlayRegistry` / `MatchRegistry` line, the six `gameNameResource` maps, `playRulesResource`, `INTERSTITIAL_GAME_IDS`, `ConfigKeys` interstitial default, Home motif and result thumbnail, category (F4). Continue as plan 17.3 / 17.4. Run the checklist in `_logs/xC-checklist.txt`. Check the display name in Play search and a trademark database; write the result in the handoff. **Done when:** `./gradlew check` passes (lead batch).

### H9, H10, H11 — Hand-test each wave (Human)
Play 10 levels (or matches) of each pack of each new game on a phone. Note any unfair level, wrong hint, too-strong or too-weak computer, or unclear rule in `FEEDBACK.md`. The lead turns notes into cards.
