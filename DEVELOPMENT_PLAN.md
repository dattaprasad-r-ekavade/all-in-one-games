# Witgrid: Offline Logic Puzzles — Development Plan

> **Keep the progress bars current.** The "Progress" section below is generated from the Status column in `docs/TASKS.md`. Do not edit the bars by hand. After you change any status in `docs/TASKS.md`, run `python scripts/progress.py` from the repo root, and commit `DEVELOPMENT_PLAN.md` together with `docs/TASKS.md`.

Plan date: 16 Nov 2026. Four launch games are implemented. Agent workflow and task backlog added 16 Nov 2026 (section 16). UI cleanup decisions added 17 Nov 2026 (sections 2, 4.1, 5, 7.5, 9 and Phase 5). Prototype hub catalog added 21 Nov 2026 (sections 6.5, 7.3, 8.1, 8.4, 12 and Phase 7). Owner direction: `docs/decisions/prototype-catalog.md`. Launch set of 14 games decided 21 Nov 2026 (sections 1, 2, 5, 11, 12, 14).

This plan replaces the game design document (the GDD PDF in this folder) where the two differ. Section 2 lists the differences.

<!-- progress:start -->
## Progress

Updated 21 Nov 2026 from the status column in `docs/TASKS.md`. Rebuild with `python scripts/progress.py`.

█ done   ▒ in progress (active or in review)   ░ not started (ready, todo or blocked)

```text
All tasks                         █████████▒▒░░░░░░░░░  47 %  (68 of 144 done, 10 in progress)

Phase 0 — Setup                   ████████████████░░░░  80 %  (4 of 5 done)
Phase 1 — Engine and puzzle bank  ███████████████████░  96 %  (25 of 26 done)
Phase 2 — Core app                ████████████████████ 100 %  (14 of 14 done)
Phase 3 — Retention               ████████████████████ 100 %  (6 of 6 done)
Phase 4 — Monetisation and data   ████████████░░░░░░░░  62 %  (5 of 8 done)
UI cleanup                        ████████████████████ 100 %  (10 of 10 done)
Phase 5 — Closed test             ██████████░░░░░░░░░░  50 %  (2 of 4 done)
Phase 6 — Launch                  ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 1 done)
Phase 7 — Launch catalog          █▒▒░░░░░░░░░░░░░░░░░   3 %  (2 of 70 done, 10 in progress)

Launch catalog, by game
  Sudoku 9x9                      ███░░░░░░░░░░░░░░░░░  14 %  (1 of 7 done)
  Killer Sudoku                   ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 7 done)
  Futoshiki                       ▒▒▒▒▒▒░░░░░░░░░░░░░░   0 %  (0 of 7 done, 2 in progress)
  Skyscrapers                     ▒▒▒▒▒▒▒▒▒░░░░░░░░░░░   0 %  (0 of 7 done, 3 in progress)
  Mosaic                          ▒▒▒▒▒▒░░░░░░░░░░░░░░   0 %  (0 of 7 done, 2 in progress)
  Colour Sort                     ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Path                            ▒▒▒░░░░░░░░░░░░░░░░░   0 %  (0 of 7 done, 1 in progress)
  Arrows                          ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Klondike                        ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Chess tactics                   ▒▒▒░░░░░░░░░░░░░░░░░   0 %  (0 of 7 done, 1 in progress)
  Shared (B6, E3, H7)             ███████▒▒▒▒▒▒░░░░░░░  33 %  (1 of 3 done, 1 in progress)

Launch games playable             ██████░░░░░░░░░░░░░░  29 %  (4 of 14 ready)
```

Open human tasks: H0.1, H1, H4, H5, H6, H7.
<!-- progress:end -->

Labels used in this plan:
- **[Estimate]** — my estimate, not measured data.
- **[Verify]** — a fact from web research on 16 Nov 2026 that you must check again before you depend on it.

---

## 1. Product summary

| Item | Value |
|---|---|
| Title | Witgrid: Offline Logic Puzzles (30 characters, the Play limit) |
| Platform | Android, Google Play. No iOS at launch. |
| Product | A growing puzzle hub. All games work with no connection. |
| Built games | Crowns, Binary, Nonogram, Mini Sudoku 6x6 |
| Production launch set | The 4 built games plus 10 from section 12: Sudoku 9x9, Killer Sudoku, Futoshiki, Skyscrapers, Mosaic, Colour Sort, Path, Arrows, Klondike, Chess tactics. Target 14, minimum 12. |
| USP | Clean UI, low distraction, no ad during a puzzle, hints that explain the logic |
| Income | Interstitial ad at level finish, rewarded ads for hints / checks / lives, one-time Premium at $7.99 |
| Income goal | About $100 each month |
| Team | One developer with AI coding help |

## 2. Decisions and differences from the GDD

| Topic | GDD | This plan |
|---|---|---|
| Name | Gridwise / Fivefold / Daily Five (all collide with other apps) | Witgrid: Offline Logic Puzzles |
| Launch modes | 5 | 14 at the production launch (minimum 12). A full hub at launch gives more store search terms and a stronger first impression than 4 games with frequent small updates. The closed test can start with fewer games; new games go to the closed track as they are done. Owner decision, 21 Nov 2026. |
| Puzzle source | Generated on the device from a date seed | Pre-generated, solver-validated puzzle bank in the app |
| Core loop | One daily set of five puzzles | Level packs for each game, plus one daily puzzle for each game |
| Connection | Not stated | Offline first. No backend. |
| Interstitials | Maximum 3 each day, after the daily set | At level finish (each level, or each 2 levels for short levels) |
| Banner ads | Optional on the home screen | None |
| Lives | Never | Split rule (section 8.4) |
| Paid product | Remove Ads $3.99 + yearly Plus | One Premium purchase at $7.99. No subscription. |
| Ad-free period | First 3 days | First session only |
| Widget, leaderboards, share card, world median time | In scope | Not in the launch. Share card and widget are post-launch options. |
| Closed test | Risk | Not a risk. Your app community supplies the 12 testers. |
| Level count | Not stated | Endless. The UI never shows a level total, "x of y", a percent or a progress bar. Levels loop through the bank (section 7.5). Owner decision, 17 Nov 2026. |
| Number of games in text | Not stated | No text states how many games there are. More games will come (section 12), and a number would lock the product. Owner decision, 17 Nov 2026. |

## 3. Goal, numbers and stop criteria

**Income goal: $100 each month.** There are two paths, and they add together.

- **Ads.** [Estimate] Ad income for each daily player is $0.006 to $0.045 each day. The value depends on the country mix. This gives a need of about 75 to 550 daily players.
- **Premium.** You keep about $6.79 from each $7.99 sale after the 15 % Play fee. About 15 sales each month reach the goal.

**Installs needed.** [Estimate] One install gives about 6 active days in the first 90 days. A level of 300 daily players needs about 50 new installs each day.

**Growth source.** Paid ads cannot profit: a US puzzle install costs about $4.81 and earns about $0.10 to $0.45. Growth must come from Play search, your app community, and community posts.

**Review points**

| When | Check | Action if the value is low |
|---|---|---|
| End of closed test | Crash-free sessions ≥ 99 %; no puzzle with two solutions | Fix before production |
| 30 days after launch | Day-1 retention ≈ 30 %, day-7 ≈ 12 % (puzzle benchmarks: 29 % and 11 %) | Improve onboarding and the first 10 levels |
| 90 days after launch | Income trend toward $100 each month | Tune ads and Premium price. Keep adding hub games from section 12. Then decide to continue or stop. |

## 4. Design principles and art direction

1. **Clean UI.** One screen has one job. No pop-up offers during play.
2. **No ad interrupts a puzzle.** Ads are presented only outside an active puzzle board. Interstitials show only after a level finish and the player's tap on "Next".
3. **Offline first.** Each game, each level and each save works with no connection.
4. **Fair puzzles.** Family A (unique grids) has exactly one solution and needs no guess. Family B (sort, arrows, cards) is winnable, or has a legal fail. Family B does not use the unique-no-guess bank rule (section 7.3).
5. **Hints explain.** A Family A hint shows the next logical step and says why. A Family B hint names one legal improving action.
6. **Generic names.** No name or look from another company's game.
7. **Endless play.** Show only "Level N". Never show a total, "x of y", a percent or a progress bar for levels or packs. A total gives a sense of an end; the player must feel that the puzzles never run out. Counts inside one puzzle (for example "2 of 7 crowns placed", mistakes) are permitted.
8. **No fixed game count.** No string, screen title or store text says how many games there are. Screen layouts must work for any number of games. Game descriptions do not go in titles.

**Art style: flat "paper and ink".** Code draws all graphics. There are no sprites and no 3D.

| Area | Spec |
|---|---|
| Paper and ink | Warm off-white paper in light mode. True black in dark mode. The app follows the system setting. |
| Colourways | 5 selectable sets |
| Region colours | 8 hues with equal lightness steps. Optional letters and hatch patterns for colour-blind players. |
| Font | One sans family with tabular numerals (for example Inter, Open Font License). Clue text minimum 14 sp, scalable. |
| Layout | The grid fills the width with 16 dp margins. Tools are in the bottom third. Nothing is tappable in the top 15 % during play. |
| Gestures | Tap, drag to paint marks, long-press to clear, two-finger tap to undo |
| Feedback | Light haptic tick on each mark, a stronger tick on a placement, sound off by default, a completion wave across the grid |
| Timer | Counts up. The player can hide it. It pauses when the app goes to the background. |
| Accessibility | TalkBack label for each cell, font scaling, reduce-motion, haptics switch. Dense boards keep a fit-to-screen overview and offer an optional enlarged pannable mode with cell touch targets at least 48 dp. |

### 4.1 Approved UI design (17 Nov 2026)

The owner approved the mockups in `docs/design/` (light and dark PNGs for Welcome, Home, Game, the four Play boards, Result and Settings). `docs/design/README.md` lists the colour tokens. Where the images and older text in this plan differ, the images are correct.

| Area | Decision |
|---|---|
| Tokens | Light: paper `#F6F1E7`, card `#FFFDF8`, card edge `#E3DCCD`, ink `#1C1B19`, muted ink `#65605A`, accent `#3B4FA0`. Dark: paper `#000000`, card `#151517`, card edge `#2C2C31`, ink `#EDE9E0`, muted ink `#A9A59C`, accent `#9AA8F0`. In `WitgridColors` (`ui/theme/Theme.kt`). |
| Shape and spacing | Cards 16–18 dp corner radius with a 1 dp edge. Buttons 14 dp radius, at least 48 dp high (primary 54 dp). Screen margin 16 dp. No fixed text heights. |
| Top bar | Every non-play screen: back arrow, title, icon actions. Home: logo, "Witgrid", Calendar, Statistics and Settings icons. Built in `ui/layout/WitgridTopBar.kt`. |
| Icons | Vector drawables in `res/drawable/ic_*.xml`, 24 dp, round stroke. No icon library. |
| Splash | Android 12+ system splash with the Witgrid mark on paper (`values-v31` themes). Older versions show the paper window background. |
| Welcome | Three pages, shown once on first start: offline logic puzzles, hints, ads only between levels. Skip and Next. Stored as `AppSettings.welcomeSeen`. |
| Home | Today's puzzles card (date, streak chip, one chip per game, "Play daily <game>" button). A 2-column grid of game cards: small board preview, name, "Level N · Pack". One quiet Premium row at the bottom. No ad buttons. |
| Game | Top bar with the game name. Continue card (board preview, "Level N", in-puzzle status). Daily row with a done tick. Pack cards with name, board size and "Next: level N". One quiet row "N hints and N checks saved · Get more" that opens the rewarded offer. |
| Play | Top bar: back, game name and "Pack · Level N", timer chip (tap to hide), info icon for the rules. The board is the largest square that fits the free space, with 16 dp margins; it never clips. Bottom icon tool bar: Undo, Redo, Restart (or the game's mode tools), Hint with a credit badge, Check. |
| Boards | Crowns: region aids (letters, hatching) off by default, thin lines inside a region, thick lines between regions, a drawn crown, a soft X. Binary: a filled accent dot and an orange ring, not letters (letters only for TalkBack). Nonogram: clue band sized from the longest clue, active row and column tint, finished clues fade, 5-cell guide lines, Fill/Cross switch, zoom icon. Mini Sudoku: given digits darker than player digits, row/column/box tint, digit pad always visible with a "left" count for each digit, Notes and Erase tools. |
| Result | Close icon, solved-board thumbnail, "Solved", "Game · Pack · Level N", Time / Best / Hints tiles, best-time banner, "Next level" (primary) and "Back to packs". |
| Settings | Theme as a 3-way segmented control, colourway swatches, assist switches with one-line subtitles, a "More" list (reminder, Premium, restore purchase, privacy). |

**Art work that you must make:** app icon, feature graphic (1024 x 500), 6 to 8 screenshots, and the colour palette.

## 5. Launch scope

**In the production launch**
- The launch set of 14 games (section 12; minimum 12), each with 4 difficulty packs and one daily puzzle.
- First-run welcome (3 pages) and the Android 12+ splash.
- Home screen with the game list, the next level of each game, and the streak.
- Play screen with undo, redo, mark mode, hint, check, and timer.
- Result screen, statistics, settings.
- Streak with one free freeze each week, calendar of daily puzzles.
- One optional local reminder notification.
- Ads, consent, Premium purchase, analytics, crash reports, Remote Config.
- Analytics and Crashlytics collection start only after explicit opt-in in Settings and stop when the player opts out.

**Not in the production launch**
- Section 12.11 later options, widget, leaderboards, share card, cloud save, accounts, subscription, banner ads, app-open ads, on-device puzzle generation, iOS, web.

**Closed test and launch.** Start the closed test (H5) with the current build as soon as S3 is done. The closed track is private, so it does not use up the public launch. Upload each new game to the closed track during the 14 days. The production release (H6) waits for the launch set (Phase 7) and the launch hand test (H7).

## 6. The four launch games

Level content for each game: 4 packs x 150 levels = 600 regular levels, plus 365 daily puzzles. The app also has one guided intro level per game, outside the regular packs: 4 intro levels total. Overall content is 2,400 regular levels, 1,460 daily puzzles and 4 guided intro levels. The daily puzzle index is `days since a fixed date mod 365`, so the daily list never ends.

### 6.1 Crowns (rule set also known as Star Battle, one star)

| Item | Spec |
|---|---|
| Rule | One crown in each row, each column and each colour region. Crowns never touch, also not diagonally. |
| Sizes | Easy 6x6, Medium 7x7, Hard 8x8, Expert 9x9 and 10x10 |
| Input | First tap = X mark, second tap = crown, third tap = clear. Drag paints X marks. Auto-X around a crown (setting). |
| Generator | Place a random valid crown layout. Grow the regions with a random flood fill. Run the solver. Reject the puzzle unless it is unique and the technique set solves it. |
| Solver techniques | Region confined to one line, touch exclusion, k regions confined to k lines. No look-ahead. |
| Level time | 60 to 150 seconds |

### 6.2 Binary

| Item | Spec |
|---|---|
| Rule | Fill each cell with one of two symbols. No more than two equal symbols are adjacent in a row or column. Each row and column has an equal count of each symbol. No two rows are equal, and no two columns are equal. |
| Sizes | Easy 6x6, Medium 8x8, Hard 10x10, Expert 12x12 |
| Input | Tap cycles empty → symbol A → symbol B. Long-press clears. |
| Generator | Make a full valid grid. Remove givens one at a time while the technique solver can still solve the puzzle. |
| Solver techniques | Pair cap, gap fill, count completion, then row and column comparison for Hard and Expert |
| Level time | 45 to 120 seconds for 6x6 |
| Name rule | Use "Binary". Do not use "Takuzu", "Binairo" or "Tango" in the app or the store listing. |

### 6.3 Nonogram

| Item | Spec |
|---|---|
| Rule | The numbers at each row and column give the lengths of the filled runs, in sequence. |
| Sizes | Easy 5x5, Medium 10x10, Hard 10x10 with fewer easy lines, Expert 15x15 |
| Input | Fill mode and X mode with a switch. Drag paints a line. Auto-cross of completed lines (setting). |
| Generator | Make a random pattern with a density target. Compute the clues. Accept only if a line solver solves the puzzle with no guess. |
| Solver techniques | Overlap, edge rules, line-by-line solve. The grade is the count of line passes and the hardest rule used. |
| Level time | 60 to 180 seconds |
| Limit | Launch puzzles are abstract patterns, not pictures. Picture packs are a post-launch option. |
| Name rule | Use "Nonogram". Do not use "Picross". |

### 6.4 Mini Sudoku 6x6

| Item | Spec |
|---|---|
| Rule | Digits 1 to 6. Each digit appears one time in each row, each column and each 2x3 box. |
| Sizes | 6x6 for all packs. The pack sets the technique level. |
| Input | Select a cell, then tap a digit. Notes mode. Notes update automatically (setting). |
| Generator | Make a full valid grid. Remove givens while the puzzle stays unique and the technique solver can solve it. |
| Solver techniques | Naked single, hidden single, locked candidates, naked pair |
| Level time | 45 to 120 seconds |

### 6.5 Rules for all games

- The first level of each game is a small, guided puzzle with one coach line. There is no tutorial wall.
- **Family A** (Crowns, Binary, Nonogram, Mini Sudoku, Sudoku 9x9, Path, Chess tactics): each bank puzzle stores its solution, its difficulty pack and its technique grade. Two solvers check each puzzle: the technique solver and a brute-force solver that counts solutions (cap 2). Unique and no-guess.
- **Family B** (Colour Sort, Arrows, Klondike): each level is winnable, or has a legal fail. Many solution paths are allowed. `bankgen validate` checks winnable (or `isLost` only after a wrong action), not uniqueness. Hints name one legal improving action.

## 7. Tech stack and architecture

### 7.1 Stack

Native Kotlin + Jetpack Compose. Versions are as reported on 16 Nov 2026. **[Verify]** all versions at project setup.

| Part | Choice |
|---|---|
| Language and build | Kotlin 2.4.x, Android Gradle Plugin 9.5.x, Gradle 9.6 |
| UI | Jetpack Compose (BOM 2026.09 or later), Compose Canvas for grids |
| Target | Target API 36 (required for new apps). Min API 24 is my suggestion. |
| Ads | Google Mobile Ads Next-Gen SDK (`ads-mobile-sdk` 1.6.0 or later), AdMob only at launch, behind the `AdService` interface |
| Consent | Google UMP SDK 4.x |
| Purchase | Play Billing Library 9.x (`billing-ktx`) |
| Storage | Room for level progress, DataStore for settings, streak and the Premium flag |
| Backup | Android Auto Backup, so progress survives a reinstall with no backend |
| Reminder | AlarmManager or WorkManager with a local notification |
| Data | Firebase Analytics, Crashlytics, Remote Config (Firebase BoM 35.x) |

**Decision (16 Nov 2026):** Next-Gen SDK. Reasons and sources: `docs/decisions/ads-sdk.md`. Legacy `play-services-ads` is deprecated on 30 Jun 2027 [Verify]. Before you add mediation, check adapter support: AppLovin MAX and Chartboost do not support Next-Gen yet [Verify]; Unity LevelPlay does.

**Play rules that affect the build [Verify]**
- Edge-to-edge display is mandatory at API 36. Predictive back is on by default.
- From 1 Feb 2027, Play blocks updates that do not support 16 KB memory pages. Pure Kotlin code complies.
- Orientation locks are ignored on screens of 600 dp or wider. Make the layout work on tablets.

### 7.2 Modules

```
witgrid/
  engine/      Pure Kotlin, no Android code. Family A grid engines, Family B play engines, generators, graders, hints.
  bankgen/     JVM command-line tool. Uses engine/ to build and validate the puzzle bank on the PC.
  app/         Android app. Compose UI, storage, ads, billing, analytics.
    assets/bank/   One compressed file for each game and pack.
```

- `engine/` has no Android dependency. It runs in JVM unit tests on Windows, and it can become a Kotlin Multiplatform module later.
- `bankgen/` writes the bank files into `app/assets/bank/`. The bank is a build output that you commit to the repository.
- Use integer maths only in `engine/`. Write a small integer PRNG (for example SplitMix64). Do not use `kotlin.random.Random(seed)`, because a new Kotlin version can change its output.

### 7.3 Two engine families

Each game is one package. A new game needs no change to the home screen, the save system or the ad logic. Home, Game and endless levels already iterate `GameRegistry`. Never state a game count.

**Family A — unique no-guess grids.** `GameEngine<P, S>` (task E1) plus `GameModule` (task A3). `Move` stays `SetMark`, `SetDigit` and `SetNotes`. Do not add a fourth `Move` variant; every existing engine uses an exhaustive `when`.

- Sudoku 9x9 copies Mini Sudoku. Size, digits, boxes and notes are hardcoded to 6 today, so this is a new `sudoku` package, not a one-line change.
- Path uses `SetDigit` as the pair colour (1..n, 0 clears). Uniqueness is required. The generator may add walls so the fill is unique.
- Chess tactics uses cell picks for from-square and to-square. Unique winning move (or unique mate sequence). Pure Kotlin rules and a bounded mate search. No chess-engine library and no online play.

**Family B — many solutions or failure-as-play.** New `PlayEngine<P, S, A>` (task E3). Game-defined actions (`Pour`, `Slide`, `CardMove`). Colour Sort, Arrows and Klondike use this. Later block puzzle would use it too.

- Do not force unique-no-guess on Family B.
- `PlayGameModule` in `app/` wraps one `PlayEngine` so Play, Result, Continue and saves still work.
- Integer maths and `SplitMix64` still apply.

Family A `GameModule` is unchanged from A3. Family B `PlayEngine` (E3) uses a game-defined action type instead of `Move`. Both register in `GameRegistry` so Home and Play iterate one list.

### 7.4 Puzzle bank format

One text line for each puzzle, in a gzip file for each game and pack.

Family A: `id | size | givens or regions | solution | pack | technique grade`

Family B: `id | size | layout or deal seed | pack | difficulty`. No unique-solution field.

[Estimate] The four-game closed-test bank is smaller than 1 MB. Hub banks add more files; keep each game under 1 MB compressed.

### 7.5 Saves

- Save the level state after each move. A crash or a closed app loses nothing.
- Store for each level: state, elapsed time, hints used, completed flag, best time.
- **Endless levels.** Each game and pack has a level number that only goes up. Level N plays bank puzzle `(N − 1) mod packSize` (0-based). For example, with 600 puzzles, level 601 plays bank puzzle 1 again. The player sees only "Level N". A looped level starts from an empty board; its best time is kept per bank puzzle.
- One shared streak day is earned by completing any one of that local date's daily puzzles. Completing more daily puzzles that date does not add another day.
- Keep at most one free streak freeze. Refill it to one each local Monday; unused freezes do not accumulate. Automatically use it to cover one missed local day.
- Store the streak and freeze state with the device date. There is no server check.

### 7.6 Hint logic

Family A:
1. If the board has a mistake, the hint points at the mistake first.
2. If not, the technique solver finds the easiest next step.
3. The hint highlights the cells, names the technique, and shows one sentence that explains the step.
4. The player then applies the step, or taps "Apply".

Family B: the hint names one legal improving action (a pour, a sliding arrow, a card move). There is no unique-technique grade.

## 8. Monetisation specification

### 8.1 Interstitial ads

Show an interstitial only when all these conditions are true:
- The player finished a level and then tapped "Next" on the result screen.
- The count of finished levels since the last interstitial is equal to or more than the game's `levelsPerInterstitial`.
- The time since the last interstitial is equal to or more than `interstitial_min_gap_sec`.
- The session is not the first session after install.
- The player does not have Premium.
- An ad is loaded. If no ad is loaded, continue with no delay.

Default `levelsPerInterstitial`: Crowns 1, Nonogram 10x10 and larger 1, Binary 2, Mini Sudoku 2, Nonogram 5x5 2, Sudoku 9x9 1, Path 2, Chess tactics 2, Colour Sort 2, Arrows 2, Klondike 1 (each finished deal).

**Never:** an interstitial during a puzzle, at app start, at level start, or on a back action.

### 8.2 Rewarded ads

| Item | Free allowance | After the allowance |
|---|---|---|
| Hint (next step with explanation) | 1 each day | 1 rewarded ad = 1 hint |
| Check (flags wrong cells). This is my reading of "tips". | Free in the Easy pack | 1 rewarded ad = 1 check |
| Extra life | See section 8.4 | 1 rewarded ad = 1 life |
| Streak repair | 1 free freeze each week, applied automatically | 1 rewarded ad within 48 hours |

Rules:
- Each rewarded ad is the player's choice. The button states the reward before the ad starts.
- Give the reward when the SDK reports it. Store the reward immediately, so that a killed app does not lose it.
- Hint allowances are shared across all games. Spend a hint when useful advice is revealed; revealing the same hint again on an unchanged board does not spend another. An unavailable hint costs nothing. This also applies to the offline hint allowance.
- A rewarded streak repair restores the most recent missed local day and must be completed within 48 hours after that day.
- Never show or start an ad while a puzzle board is active. Rewarded hint and check credits are earned from the pre-puzzle Game screen, then spent in Play.

### 8.3 Offline behaviour

- Preload one interstitial and one rewarded ad when a connection is available. Drop a preloaded ad after one hour. Google documents this expiry only for the Legacy SDK, so the app enforces it itself. Make the reward grant idempotent.
- With no connection and no loaded ad: skip the interstitial with no message.
- With no connection, the player gets `offline_free_hints_per_day` extra hints (default 3). After that, the hint button shows the Premium offer.
- Never block play because of the connection.
- Log an `offline_session` event. Firebase sends it later. This measures the share of offline play.

### 8.4 Lives: the split rule

| Game type | Rule |
|---|---|
| Family A logic grids (the four closed-test games, Sudoku 9x9, Path, Chess tactics) | "Mistake limit" is a setting, **off by default**. When it is on, the game flags a wrong entry immediately, and three mistakes fail the level. |
| Family B, failure is natural (Colour Sort, Arrows, Klondike; later block puzzle) | Lives are always part of the game: extra tube (Colour Sort), revive (Arrows, later block puzzle), extra undo (Klondike). |

When a level fails or lives are gone, show two buttons:
1. "Continue" — one rewarded ad, or free with Premium.
2. "Restart" — always free, with no ad.

For Family A, Continue grants one additional mistake and keeps the board as-is. Each later Continue grants one more mistake.
For Colour Sort, Continue adds one empty tube. For Arrows, Continue revives the board to the last sliding position (or undoes the last slide). For Klondike, Continue grants one extra undo. Restart is always free and never shows an ad.

The default of the mistake limit is a Remote Config key, so you can test "on by default" later.

### 8.5 Premium ($7.99, one-time, non-consumable)

Premium gives:
- No interstitials.
- Unlimited hints and checks.
- Unlimited continues and streak repairs with no ad.

Rules:
- Show the Premium offer in Settings, on the home screen as one quiet row, and when a rewarded ad cannot load.
- Store the Premium flag locally, so it works offline. Check it again with Play Billing when a connection is available.
- Add "Restore purchase" in Settings.
- Premium removes the ad or allowance cost of streak repair, but does not extend the latest-uncovered-miss or 48-hour eligibility rules.
- Comparable apps charge $3.99 to $5.99 for ad removal only. Use a Play price experiment after launch to compare $5.99 and $7.99.

### 8.6 Remote Config keys

All keys have a default in the app, so the app works offline from the first start.

| Key | Default |
|---|---|
| `interstitial_enabled` | true |
| `interstitial_min_gap_sec` | 90 |
| `interstitial_levels_<game>` | 1 or 2 (section 8.1) |
| `interstitial_skip_first_session` | true |
| `free_hints_per_day` | 1 |
| `offline_free_hints_per_day` | 3 |
| `mistake_limit_default` | false |
| `mistake_limit_count` | 3 |
| `streak_free_freezes_per_week` | 1 |
| `premium_offer_on_ad_fail` | true |

### 8.7 Consent and policy

- Show the UMP consent form before the first ad request. EEA, UK and Switzerland need a certified consent platform; UMP qualifies.
- Declare the target audience as 13 and older. Do not use child-like art.
- Use **test ad units** in all debug builds and in the closed test. Never tap a live ad in your own app. A disabled AdMob account cannot be replaced.
- Publish `app-ads.txt` on the developer website that the store listing names.
- Complete the Data safety form for the ad SDK, Analytics and Crashlytics.
- Each full-screen ad must be closeable. Do not add a custom delay or a false close button.

## 9. Screens

| Screen | Content | Ad or offer |
|---|---|---|
| Welcome | Three pages on first start only (section 4.1) | None |
| Home | Game cards with the next level, daily puzzle state, streak and freeze count. No totals. | One quiet Premium row |
| Game | Packs with the next level (no totals), the daily puzzle, "Continue" | One quiet row for rewarded hint and check credits before entering Play |
| Play | Grid, bottom icon tool bar (undo, redo, mark mode, hint, check), timer chip, rules behind an info icon | None; use free, offline or previously earned credits |
| Result | Time, best time, streak update, "Next" | Interstitial after "Next" (section 8.1) |
| Level failed | "Continue" and "Restart" | Rewarded continue |
| Calendar | Month view of daily puzzles and streak | Rewarded streak repair |
| Statistics | Solves, best and average time for each game | None |
| Settings | Theme, colourway, assists, mistake limit, haptics, sound, reminder, Premium, restore purchase, privacy | Premium |

## 10. Analytics events

| Event | Parameters |
|---|---|
| `level_start`, `level_complete`, `level_fail` | game, pack, level id, time, hints used, mistakes |
| `hint_used`, `check_used` | game, source (free, ad, premium, offline) |
| `ad_interstitial_shown`, `ad_interstitial_skipped` | game, reason |
| `ad_rewarded_offered`, `ad_rewarded_completed`, `ad_rewarded_unavailable` | placement |
| `premium_offer_shown`, `premium_purchased` | source screen |
| `daily_complete`, `streak_changed`, `streak_freeze_used` | game, streak length |
| `offline_session` | — |

Analytics and Crashlytics are disabled by default. An explicit Settings opt-in enables both; opting out disables both. This choice is separate from UMP ad consent, which does not enable analytics or crash reporting.

Measures to watch: day-1, day-7 and day-30 retention, levels for each session, interstitials for each daily player, rewarded ads for each daily player, Premium conversion, share of offline sessions, crash-free sessions.

## 11. Development phases

Durations are **[Estimate]** values for one developer who directs AI coding agents. Each phase lists its task cards from `docs/TASKS.md`. The cards hold the detail; this section holds the sequence and the exit test.

### Phase 0 — Setup (week 1)
- H0.1 (you): Google and Play Store name search, Play Console, AdMob account, developer website with privacy policy and `app-ads.txt`.
- H0.2 (you): `ANDROID_HOME`, `gh auth login`, first commit, Codex folder trust.
- T0.1: Gradle project with `engine`, `bankgen`, `app`. Application id `com.datathecodie.witgrid`.
- T0.2: GitHub Actions CI.
- T0.3: ads SDK memo (Legacy or Next-Gen). You decide.

**Exit:** an empty app builds and runs on a phone, and CI is green.

### Phase 1 — Engine and puzzle bank (weeks 1 to 3)
- E1: engine contracts (you read them before merge). E2: test kit.
- Four parallel lanes, four cards each: CR (Crowns), BI (Binary), NO (Nonogram), MS (Mini Sudoku). Each lane: rules and solution counter → technique solver, grader and hints → generator; independent spec tests from the other agent family.
- B1: `bankgen`. B2: generate 600 levels and 365 daily puzzles for each game. B3: bank validation in CI.
- H1 (you): solve 20 puzzles of each game and pack by hand.

Open-source references with the MIT licence: Simon Tatham's Portable Puzzle Collection (Solo, Unruly, Pattern) and small Star Battle projects. If you reuse MIT code, keep the copyright notice in the app's licence screen.

**Exit:** the bank files exist and all tests pass.

### Phase 2 — Core app (weeks 3 to 6)
- A1 theme · A2 storage · A3 app contracts and play controller (you read them before merge) · A4 board kit and gestures.
- A5 to A8: the four boards, in parallel.
- A9 Home and Game · A10 Play, Result, Level failed · A11 hint and check UI · A12 guided first levels · A13 edge-to-edge and tablets · A14 UI smoke tests.

**Exit:** all 2,400 levels are playable offline from start to result.

### Phase 3 — Retention (weeks 6 to 7)
- R1 daily, streak and freeze logic · R2 calendar · R3 statistics · R4 reminder · R5 settings · R6 accessibility pass.

**Exit:** a player can keep a streak for 7 days with the device offline.

### Phase 4 — Monetisation and data (weeks 7 to 8)
- M1 policy logic (plain Kotlin) · M2 policy tests from the spec · M3 consent and ad service with test units · M4 ads in the flows · M5 mistake limit and continue · M6 Premium purchase and restore.
- H4 (you): Firebase project, AdMob ad units, licence-test account. Then M7: analytics, Crashlytics, Remote Config.

**Exit:** each ad and purchase path works with test units and a licence-test account, online and offline.

### Phase 5 — Closed test (weeks 8 to 10)
- S1 store listing and policy form drafts · S2 screenshot script · S3 release build configuration.
- U0 to U8 UI cleanup (`docs/TASKS.md`, section "UI cleanup") before the closed test. U0, U1 and U2 are done.
- H5 (you): art approval, upload keystore, closed test with 12 or more testers for 14 continuous days, policy forms, production access. Tester bugs become new cards. Start H5 with the current games; do not wait for Phase 7. Add new games to the closed track as they are done.

**Exit:** Google approves production access. Production release still waits for the Phase 7 launch set and H7.

### Phase 6 — Launch and first 30 days (week 10 onward)
- H6 (you): replace test ad units with live ad units (Gradle properties outside the repo), check `app-ads.txt` status, staged rollout, posts in your app community and puzzle communities, weekly review of the section 10 measures, one Remote Config change at a time.
- Launch catalog (Phase 7 in `docs/TASKS.md`) starts now and must finish before H6. Sudoku 9x9 does not wait on E3. Colour Sort, Arrows and Klondike wait on E3. Closed test (H5) does not wait on Phase 7.

## 12. Launch catalog (owner, 18 and 21 Nov 2026)

The production launch ships 14 games: the 4 built games plus the 10 below. Minimum 12. If the build runs late, cut Chess tactics first, then Klondike. Do not cut a cheap Family A game to save time. After launch, keep adding games from 12.11. Phase 7 in `docs/TASKS.md` has the cards. Research: `docs/reports/extra-launch-games.md`. Decision: `docs/decisions/prototype-catalog.md`.

Build order (dependencies, not calendar): Sudoku 9x9 → Killer Sudoku, with Futoshiki, Skyscrapers and Mosaic in parallel; E3 → Colour Sort → Arrows → Klondike; then Path → Chess tactics. Killer Sudoku, Futoshiki, Skyscrapers and Mosaic reuse the existing Family A input (`SetDigit`, `SetMark`), so they are the cheapest.

| Order | Game | Family | Content | Level time | Interstitial | Continue |
|---|---|---|---|---|---|---|
| 1 | Sudoku 9x9 | A | Bank, 4×150 + 365 daily | 5 to 15 min | Each level | Extra mistake |
| 2 | Colour Sort | B | Bank of tube layouts, winnable, not unique | 45 to 120 s | Each 2 levels | Extra empty tube |
| 3 | Path | A | Bank, unique fill. Walls allowed | 20 to 90 s | Each 2 levels | Extra mistake |
| 4 | Arrows | B | Bank of layouts, at least one clear-all | 30 to 120 s | Each 2 levels | Revive / undo last slide |
| 5 | Klondike | B | Winnable deals stored as seeds | 3 to 7 min | Each deal | Extra undo |
| 6 | Chess tactics | A | Bank of unique mates | 30 to 180 s | Each 2 levels | Extra mistake |
| 7 | Killer Sudoku | A | Bank, 4×150 + 365 daily | 8 to 20 min | Each level | Extra mistake |
| 8 | Futoshiki | A | Bank, 4×150 + 365 daily | 2 to 6 min | Each 2 levels | Extra mistake |
| 9 | Skyscrapers | A | Bank, 4×150 + 365 daily | 2 to 6 min | Each 2 levels | Extra mistake |
| 10 | Mosaic | A | Bank, 4×150 + 365 daily | 2 to 8 min | Each 2 levels | Extra mistake |

### 12.1 Sudoku 9x9

| Item | Spec |
|---|---|
| Package | `sudoku` (copy Mini Sudoku; do not generalise the 6x6 lane) |
| Rule | Digits 1 to 9. Each digit one time in each row, column and 3x3 box. |
| Sizes | 9x9 for all packs. Pack sets the technique level. |
| Input | Select a cell, tap a digit. Notes mode. Same `Move`s as Mini Sudoku. |
| Generator | Full valid grid. Remove givens while unique and the technique solver still solves. |
| Solver techniques | Easy: naked single. Medium: hidden single. Hard: locked candidates, naked pair. Expert: hidden pair, naked triple, X-Wing. |
| Name rule | Use "Sudoku". Mini Sudoku stays the 6x6 game. |

### 12.2 Colour Sort

| Item | Spec |
|---|---|
| Package | `coloursort` |
| Rule | Pour from one tube onto another. A pour is legal when the source top colour matches the dest top colour, or the dest is empty, and the dest has space. Win when every tube is empty or a single colour. |
| Packs | Easy 4 tubes, Medium 6, Hard 8, Expert 10. One or two empty tubes at the start. |
| Input | Tap source tube, tap dest tube. Family B action `Pour`. |
| Generator | Start from a solved set of tubes, shuffle pours in reverse. Accept if a solver can re-sort. Many solutions allowed. |
| Hint | One pour that does not increase the number of colour runs. |
| Name rule | Use "Colour Sort". Do not use Water Sort as a product name. |

### 12.3 Path

| Item | Spec |
|---|---|
| Package | `path` |
| Rule | Join each equal-number pair with a path. Paths do not branch or cross. Every cell is filled. Exactly one solution. |
| Sizes | Easy 6x6, Medium 8x8, Hard 10x10, Expert 12x12. Walls allowed. |
| Input | Drag along empty cells. `SetDigit` stores the pair colour. |
| Generator | Place pairs (and optional walls). Accept only if `countSolutions` is 1 and the technique solver fills the board. |
| Solver techniques | Endpoint extension, forced neighbour, last empty cell in a region. No guess. |
| Name rule | Use "Path". Do not use Zip or Flow Free. |

### 12.4 Arrows

| Item | Spec |
|---|---|
| Package | `arrows` |
| Rule | Each cell holds one arrow or is empty. Tap an arrow. It slides off the board in its direction if every cell on that path is empty. Win when the board is empty. Fail when no arrow can slide and arrows remain. |
| Sizes | Easy 5x5, Medium 6x6, Hard 8x8, Expert 10x10. |
| Input | Tap an arrow. Family B action `Slide`. |
| Generator | Place arrows so at least one sequence clears the board. Uniqueness of tap order is not required. |
| Hint | One arrow that can slide now. |
| Art | Line-art arrows on the paper grid. No sprites. |

### 12.5 Klondike

| Item | Spec |
|---|---|
| Package | `klondike` |
| Rule | Standard Klondike: 7 tableau columns, 4 foundations A→K by suit, stock. Draw-1. Win when all 52 cards sit on foundations. |
| Packs | Easy: more face-up tableau cards. Medium: standard. Hard / Expert: fewer face-up, longer solutions. |
| Input | Tap or drag a face-up card to a legal dest. Family B action `CardMove`. |
| Generator | Deal from `SplitMix64`. A solver must win. Store the seed in the bank line. Reject unwinnable deals. |
| Hint | One legal move that the solver uses on the winning line. |
| Art | Code-drawn cards. Ranks as tabular numerals, suits as simple marks. No face sprites. |
| Name rule | Use "Klondike". Do not ship 50 solitaire variants. |

### 12.6 Chess tactics

| Item | Spec |
|---|---|
| Package | `tactics` |
| Rule | 8x8 chess, FIDE movement. The side to move has a unique mate in N. Player plays the winning side only. Opponent replies are forced (or the unique defence). |
| Packs | Easy mate in 1. Medium mate in 2. Hard mate in 2 with more pieces. Expert mate in 3. |
| Input | Tap from-square, tap to-square. Encode as cell picks on `GameEngine`. Promotion: queen only in the bank. |
| Generator | Pure Kotlin rules and a bounded mate search. Place a king plus a small piece set from a pattern list, then search. Accept only if the mate is unique. No Stockfish, no JNI, no new library. |
| Solver techniques | Checks, forced captures, mating patterns named in the hint sentence. |
| Name rule | Use "Tactics". Not a chess server, not ratings, not clocks, not online play. |

### 12.7 Killer Sudoku

| Item | Spec |
|---|---|
| Package | `killersudoku` (copy the `sudoku` lane; do not edit it) |
| Rule | Sudoku 9x9 rules. Dashed cages show a sum. Digits in a cage do not repeat and add up to the sum. Few or no given digits. |
| Input | Same as Sudoku 9x9. `SetDigit` and `SetNotes`. |
| Generator | Full valid grid. Grow random cages (size 1 to 5). Accept only if unique and the technique solver solves it. |
| Solver techniques | Sudoku techniques plus cage combinations, the 45 rule (house sums) and innie/outie. |
| Name rule | Use "Killer Sudoku". |

### 12.8 Futoshiki

| Item | Spec |
|---|---|
| Package | `futoshiki` |
| Rule | Latin square: each digit 1 to n one time in each row and column. A < or > sign between two cells must hold. |
| Sizes | Easy 5x5, Medium 6x6, Hard 7x7, Expert 7x7 with fewer signs. |
| Input | Mini Sudoku digit pad and notes. Signs drawn on cell edges. |
| Generator | Random Latin square. Add all signs, then remove signs and givens while unique and technique-solvable. |
| Solver techniques | Singles, sign chains (min/max bounds), naked pair. |

### 12.9 Skyscrapers

| Item | Spec |
|---|---|
| Package | `skyscrapers` |
| Rule | Latin square of building heights 1 to n. A clue outside the grid gives how many buildings you see from that side. |
| Sizes | Easy 4x4, Medium 5x5, Hard 6x6, Expert 7x7. |
| Input | Digit pad and notes. Outside clues use the Nonogram clue band. |
| Generator | Random Latin square. Compute all clues, then remove clues while unique and technique-solvable. |
| Solver techniques | Edge clues 1 and n, height bounds from clues, singles. |

### 12.10 Mosaic

| Item | Spec |
|---|---|
| Package | `mosaic` |
| Rule | Fill some cells. A number in a cell gives how many cells are filled in its 3x3 area (the cell and its neighbours). |
| Sizes | Easy 8x8, Medium 10x10, Hard 12x12, Expert 15x15. |
| Input | Fill and Cross, the same as Nonogram (`SetMark`). |
| Generator | Random fill. Compute all clues, then remove clues while unique and technique-solvable. |
| Solver techniques | Full and empty areas (0 and 9), overlap between two clues. |
| Name rule | Use "Mosaic". |

### 12.11 Later options

After the launch: Tents, no-guess Minesweeper, generic 8x8 block puzzle (third family: piece drag + revive; never use the name Block Blast).

**Do not add:** match-3, mahjong, tile match, full online chess, 50-variant solitaire, screw sort, hexa sort, jigsaw, five-letter word guess, sliding block escape, number merge, word search as a near-term game.

**Names to avoid in the app and the store listing:** Queens, Tango, Zip, LinkedIn, NYT, Takuzu, Binairo, Picross, KenKen, Tetris, Block Blast, 2048, Flow Free, Rush Hour, Wordle.

**After the income goal is met:** share card, home-screen widget, nonogram picture packs, more ad networks through AdMob mediation (after about 1,000 to 2,000 daily players), iOS.

## 13. Store listing

| Field | Draft |
|---|---|
| Title (30 max) | Witgrid: Offline Logic Puzzles |
| Short description (80 max) | Offline puzzles: sudoku, nonogram, path, colour sort. No wifi needed. |
| Category | Puzzle |
| Target audience | 13 and older |

- Lead the listing with Sudoku, Nonogram, Path and Colour Sort once those hub games ship. Crowns, Binary, Tactics and Klondike sit in the list, not in the title. Never state a game count.
- The head search terms "offline games" and "no wifi games" belong to apps with 100M+ installs. Aim at long terms: "offline logic puzzles", "nonogram offline", "sudoku offline", "colour sort", "path puzzle".
- Play rules: no emoji in the title, no repeated keywords, no ranking claims, no other company's brand.
- Screenshots: show one clean board for each game, the dark mode, and the "works offline" message.
- **[Verify]** "star battle" as a free term. The web check found no registration, but it was not an official search.

## 14. Risks

| Risk | Effect | Mitigation |
|---|---|---|
| Few installs. A competitor with a similar feature list has only 100+ installs. | Income stays below the goal | Your app community, long search terms, hub catalog updates from section 12 |
| Offline players see no ads | Lower ad income | Premium offer when an ad cannot load; measure the offline share |
| AdMob account disabled for invalid traffic | Ad income stops permanently | Test ad units only; never tap live ads; no ad placement near tap targets |
| A Family A puzzle with two solutions or a need to guess | 1-star reviews | Two solvers check each Family A puzzle; hand test of each pack |
| Scope growth | Late launch | The launch set is fixed at 14 (section 12). Nothing from 12.11 enters before launch. Cut Chess tactics, then Klondike, if late. Closed test runs in parallel. |
| 14 games are not checked well | 1-star reviews on a weak game | Same two-solver bank checks for every Family A game; H7 hand test of every new game; the U8 screenshot sheet covers every game |
| Trademark complaint | Listing removed | The name rules in sections 6 and 12 |
| Interstitial after each level harms reviews | Lower rating, lower search rank | Minimum gap, "each 2 levels" for short levels, Remote Config tuning |
| $7.99 is higher than comparable apps | Fewer Premium sales | Premium includes unlimited hints and lives; price experiment |
| Play rule changes (API level, 16 KB pages) | Blocked updates | Native stack; update the target API each year |

## 15. Open items

1. ~~Official trademark check of "Witgrid"~~ — owner decision 18 Nov 2026: not required. A Google and Play Store name search is enough (H0.1).
2. ~~Ads SDK choice~~ — decided 16 Nov 2026: Next-Gen (`docs/decisions/ads-sdk.md`).
3. Public developer details on Play for an account that earns money.
4. ~~GDD section 7 is missing from the PDF~~ — dropped 18 Nov 2026. No source exists; this plan covers ASO, launch and the closed test.
5. ~~Nonogram pictures at launch?~~ - abstract patterns are the launch scope; see section 6.3.
6. ~~Minimum Android version?~~ - minSdk 24 is set in T0.1.
7. ~~JDK version~~ — settled in T0.1: AGP 9.5.1 works with JDK 17. Versions: Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, compileSdk 37, targetSdk 36, minSdk 24, JUnit 6.1.3.
8. Codex sandbox flags for Gradle (`network_access`, `--add-dir ~/.gradle`) on your Codex version. Settle at the first Codex task.

## 16. Build method: AI agents

All code comes from AI agents. You are the lead: you pick tasks, read handoff notes, run reviews and merge. Full detail is in these files:

| File | Content |
|---|---|
| `AGENTS.md` | Rules for every agent: commands, module rules, hard rules, handoff format. Codex reads it automatically. |
| `CLAUDE.md` | Imports `AGENTS.md`, plus notes for Claude Code. |
| `docs/TASKS.md` | The backlog: one card for each task, with tier, owner, owned files, plan sections and "done when" commands. |
| `docs/AGENT_WORKFLOW.md` | Setup, the task loop, parallel work, known risks. |
| `.claude/agents/` | Claude subagents: `builder` (Sonnet), `test-writer` (Haiku), `chore` (Haiku), `reviewer` (Sonnet). |
| `scripts/agent-task.ps1` | Starts one task in its own git worktree with Codex or Claude. |
| `scripts/review-task.ps1` | Cross-review of a task branch by the other agent family. |

### 16.1 Model tiers

| Tier | Work | Claude | Codex |
|---|---|---|---|
| L | Tests from spec, wiring, CI, strings, reports | Haiku 5.5 | `gpt-6-luna`, effort medium |
| M | Screens, storage, boards, normal features | Sonnet 5.5 | `gpt-6.1-sol`, effort medium |
| H | Contracts, solvers, generators, gestures, consent, billing | Sonnet 5.5 | `gpt-6.1-sol`, effort high |
| Escalation | Failed the gate two times | Opus 5.5 | `gpt-6-astra` |

### 16.2 Why this split works

- **Engine first.** Family A in `engine/` has a strong test oracle: "exactly one solution, and the technique solver solves it". Family B uses winnable / `isLost` oracles. Agents do well on this work, and the tests catch their mistakes.
- **Contracts before lanes.** E1 (Family A) and A3 (app) are done. E3 (Family B) must land before Colour Sort, Arrows and Klondike. Sudoku 9x9 and Path do not wait on E3.
- **Two families.** Codex builds most of the engine and Claude builds most of the UI. Tests and reviews come from the other family, so one model's misreading of the spec does not pass through unseen.
- **Logic out of the UI.** Ad rules, streaks and allowances are plain Kotlin with JVM tests. Agents cannot check a Compose screen by eye, but they can check a unit test.
- **Humans keep the risky steps.** Accounts, keys, live ad IDs, hand tests, art approval and store forms are cards with tier **Human**.

