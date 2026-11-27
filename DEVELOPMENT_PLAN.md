# Witgrid: Offline Logic Puzzles — Development Plan

> **Keep the progress bars current.** The "Progress" section below is generated from the Status column in `docs/TASKS.md`. Do not edit the bars by hand. After you change any status in `docs/TASKS.md`, run `python scripts/progress.py` from the repo root, and commit `DEVELOPMENT_PLAN.md` together with `docs/TASKS.md`.

Plan v2, 27 Nov 2026. The launch catalog of 14 games is built and playable. The v1 plan (16 to 27 Nov 2026, with the build history, the finished phases and the device-play polish list) is in `docs/archive/DEVELOPMENT_PLAN-v1.md`. The v1 task cards are in `docs/archive/TASKS-v1.md`. Section numbers 1 to 10 and 12.1 to 12.10 are the same as in v1, because code comments cite them.

What is new in v2: four display renames (section 12), the remaining launch work (section 11), and the catalog expansion in three waves (section 17).

This plan replaces the game design document (the GDD PDF in this folder) where the two differ.

<!-- progress:start -->
## Progress

Updated 27 Nov 2026 from the status column in `docs/TASKS.md`. Rebuild with `python scripts/progress.py`.

█ done   ▒ in progress (active or in review)   ░ not started (ready, todo or blocked)

```text
All tasks                         ░░░░░░░░░░░░░░░░░░░░   1 %  (1 of 168 done)

Launch                            ██░░░░░░░░░░░░░░░░░░  10 %  (1 of 10 done)
Expansion foundations             ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
Wave 1 — Solo games               ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 68 done)
Wave 2 — Against the computer     ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 71 done)
Wave 3 — Word games               ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 13 done)

Expansion games, by lane
  Nuts & Bolts                    ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Minesweeper                     ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Maze Paint                      ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Number Merge                    ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Sliding Puzzle                  ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Memory Pairs                    ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Block Escape                    ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Yatzy                           ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Shikaku                         ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 7 done)
  Spider Solitaire                ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  FreeCell                        ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Tic-Tac-Toe                     ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Four in a Row                   ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Mancala                         ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Reversi                         ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Dots and Boxes                  ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Sea Battle                      ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Checkers                        ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Chain Burst                     ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Hearts                          ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Spades                          ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Crazy Eights                    ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Dominoes                        ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Ludo                            ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Chess                           ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 5 done)
  Word Search                     ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)
  Secret Word                     ░░░░░░░░░░░░░░░░░░░░   0 %  (0 of 6 done)

Games playable                    ███████░░░░░░░░░░░░░  34 %  (14 of 41 ready)
```

Open human tasks: H0.1, H1, H4, H5, H7, H6, H8, H9, H10, H11.
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
| Launch catalog | 14 games, all playable (section 12): Pet Crowns, Binary, Nonogram, Mini Sudoku, Sudoku, Killer Sudoku, Futoshiki, Towers, Mosaic, Colour Sort, Number Link, Arrows, Solitaire, Chess Puzzles. |
| Next | Catalog expansion in three waves (section 17) |
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
| Number of games in text | Not stated | No text states how many games there are. More games will come (section 17), and a number would lock the product. Owner decision, 17 Nov 2026. |

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
- The launch catalog of 14 games (section 12), each with 4 difficulty packs and one daily puzzle.
- First-run welcome (3 pages) and the Android 12+ splash.
- Home screen with the game list, the next level of each game, and the streak.
- Play screen with undo, redo, mark mode, hint, check, and timer.
- Result screen, statistics, settings.
- Streak with one free freeze each week, calendar of daily puzzles.
- One optional local reminder notification.
- Ads, consent, Premium purchase, analytics, crash reports, Remote Config.
- Analytics and Crashlytics collection start only after explicit opt-in in Settings and stop when the player opts out.

**Not in the production launch**
- Section 17 expansion games (they follow in updates), widget, leaderboards, share card, cloud save, accounts, subscription, banner ads, app-open ads, on-device puzzle generation, iOS, web.

**Closed test and launch.** Start the closed test (H5) with the current build as soon as S3 is done. The closed track is private, so it does not use up the public launch. Upload each new game to the closed track during the 14 days. The production release (H6) waits for the closed test (H5) and the launch hand test (H7). Expansion games do not block it.

## 6. The first four games

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
| Name rule | Display name **"Pet Crowns"**. Engine id stays `crowns`. Do not use "Queens", "Meowdoku" or "Petdoku". Owner decision 27 Nov 2026. Home tile and store art: original crowned-cat mark (H5), not Oakever/Meowdoku assets. |

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
- **Family A** (Pet Crowns, Binary, Nonogram, Mini Sudoku, Sudoku, Killer Sudoku, Futoshiki, Towers, Mosaic, Number Link, Chess Puzzles; later Shikaku): each bank puzzle stores its solution, its difficulty pack and its technique grade. Two solvers check each puzzle: the technique solver and a brute-force solver that counts solutions (cap 2). Unique and no-guess.
- **Family B** (Colour Sort, Arrows, Solitaire; most Wave 1 and Wave 3 games): each level is winnable, or has a legal fail. Many solution paths are allowed. `bankgen validate` checks winnable (or `isLost` only after a wrong action), not uniqueness. Hints name one legal improving action.
- **Family C** (Wave 2, games against the computer): no bank and no uniqueness. A level is one match; the pack is the computer strength (section 17.2).

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
- Chess Puzzles (`tactics`) uses cell picks for from-square and to-square. Unique winning move (or unique mate sequence). Pure Kotlin rules and a bounded mate search. No chess-engine library and no online play.

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

Default `levelsPerInterstitial`: the table in section 12 for launch games, and the tables in section 17 for expansion games. Every registered game must have a default in `ConfigKeys` (a test checks it).

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
| Family A logic grids (all Family A games) | "Mistake limit" is a setting, **off by default**. When it is on, the game flags a wrong entry immediately, and three mistakes fail the level. |
| Family B, failure is natural (Colour Sort, Arrows, Solitaire; Wave 1 and 3 games with a fail state) | Lives are always part of the game: extra tube (Colour Sort), extra life (Arrows), extra undo (Solitaire); section 17 for new games. |
| Family C, games against the computer | A lost match is a failed level. Continue takes back the last full turn. |

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

## 11. Remaining launch work

The production launch needs these cards. Phases 0 to 7 of v1 are otherwise done. The cards are in `docs/TASKS.md`, group "Launch (remaining)".

| Card | Work | Owner | Waits on |
|---|---|---|---|
| H0.1 | Google and Play name search, Play Console, AdMob account, developer website with privacy policy and `app-ads.txt` | Human | — |
| H1 | Hand-test 20 puzzles of each pack of the first four games | Human | — |
| H4 | Firebase project, AdMob ad units, licence-test account | Human | H0.1 |
| M6 | Premium purchase and restore | Agent (H) | H4 |
| M7 | Analytics, Crashlytics, Remote Config | Agent (M) | H4, M6 |
| S3 | Release build configuration | Agent (M) | M6, M7 |
| H5 | Art, upload keystore, closed test (12+ testers, 14 days), policy forms, production access | Human | S3 |
| H7 | Hand-test the 14-game launch catalog | Human | — |
| H6 | Live ad units, staged rollout, weekly review of the section 10 measures | Human | H5, H7 |

**Rules for the launch.** Start the closed test (H5) with the current build as soon as S3 is done. Expansion games (section 17) do not block the launch. Upload each finished expansion game to the closed track. Live ad unit IDs go in Gradle properties outside the repository (H6), never in the repository.

## 12. Launch catalog (14 games, all playable)

| Display name | Engine id | Family | Spec | Interstitial | Continue |
|---|---|---|---|---|---|
| Pet Crowns | `crowns` | A | 6.1 | Each level | Extra mistake |
| Binary | `binary` | A | 6.2 | Each 2 levels | Extra mistake |
| Nonogram | `nonogram` | A | 6.3 | 1 (10x10+) or 2 (5x5) | Extra mistake |
| Mini Sudoku | `minisudoku` | A | 6.4 | Each 2 levels | Extra mistake |
| Sudoku | `sudoku` | A | 12.1 | Each level | Extra mistake |
| Colour Sort | `coloursort` | B | 12.2 | Each 2 levels | Extra empty tube |
| Number Link | `path` | A | 12.3 | Each 2 levels | Extra mistake |
| Arrows | `arrows` | B | 12.4 | Each 2 levels | Extra life |
| Solitaire | `klondike` | B | 12.5 | Each deal | Extra undo |
| Chess Puzzles | `tactics` | A | 12.6 | Each 2 levels | Extra mistake |
| Killer Sudoku | `killersudoku` | A | 12.7 | Each level | Extra mistake |
| Futoshiki | `futoshiki` | A | 12.8 | Each 2 levels | Extra mistake |
| Towers | `skyscrapers` | A | 12.9 | Each 2 levels | Extra mistake |
| Mosaic | `mosaic` | A | 12.10 | Each 2 levels | Extra mistake |

**Display renames (owner decision, 27 Nov 2026).** Path → **Number Link**, Klondike → **Solitaire**, Chess tactics → **Chess Puzzles**, Skyscrapers → **Towers**. Only the user-visible name changes. Engine ids, packages, bank files, save keys and Remote Config keys keep the old ids. Colour Sort keeps its name: "Water Sort" has open USPTO filings by other companies, so it is used only as a store keyword (section 13). Reason for the renames: the store search names for these genres are "number link", "solitaire", "chess puzzles" and "towers"; the old names were rare search terms.

### 12.1 Sudoku

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
| Name rule | Use "Colour Sort". "Water Sort" only as a store keyword (section 13), never as a display name. |

### 12.3 Number Link (engine id `path`)

| Item | Spec |
|---|---|
| Package | `path` |
| Rule | Join each equal-number pair with a path. Paths do not branch or cross. Every cell is filled. A path never touches itself (no cell of a path is next to another cell of the same path except its neighbours on the path; so no 2x2 block of one colour). Exactly one solution. |
| Uniqueness (lead decision, 22 Nov 2026) | Because paths never touch themselves, one route equals one colouring, so `countSolutions` counts colourings, which is what the player enters. |
| Sizes | Easy 6x6, Medium 8x8, Hard 10x10, Expert 12x12. Walls allowed. |
| Input | Drag along empty cells. `SetDigit` stores the pair colour. |
| Generator | Place pairs (and optional walls). Accept only if `countSolutions` is 1 and the technique solver fills the board. |
| Solver techniques | Endpoint extension, forced neighbour, last empty cell in a region. No guess. |
| Name rule | Display name "Number Link" (two words). Do not use Zip, Flow Free or "Numberlink" as one word. |

### 12.4 Arrows

| Item | Spec |
|---|---|
| Package | `arrows` |
| Rule | Each cell holds one arrow or is empty. Tap an arrow. It slides off the board in its direction if every cell on that path is empty. Win when the board is empty. A tap on an arrow that cannot slide is a mistake: the arrow bumps and the player loses one life. Three mistakes fail the level. |
| Why the mistake rule (lead decision, 22 Nov 2026) | A slide only removes an arrow, so it can only open paths. On a clearable layout every legal slide still leads to a win, so "no arrow can slide" never happens and the level could never be lost. Lives come from blocked taps instead. Continue gives one extra life. Layouts that are already deadlocked are rejected by the bank. |
| Sizes | Easy 5x5, Medium 6x6, Hard 8x8, Expert 10x10. |
| Input | Tap an arrow. Family B action `Slide`. |
| Generator | Place arrows so at least one sequence clears the board. Uniqueness of tap order is not required. |
| Hint | One arrow that can slide now. |
| Art | Line-art arrows on the paper grid. No sprites. |

### 12.5 Solitaire (engine id `klondike`)

| Item | Spec |
|---|---|
| Package | `klondike` |
| Rule | Standard Klondike: 7 tableau columns, 4 foundations A→K by suit, stock. Draw-1. Win when all 52 cards sit on foundations. |
| Packs | Easy: more face-up tableau cards. Medium: standard. Hard / Expert: fewer face-up, longer solutions. |
| Input | Tap or drag a face-up card to a legal dest. Family B action `CardMove`. |
| Generator | Deal from `SplitMix64`. A solver must win. Store the seed in the bank line. Reject unwinnable deals. |
| Hint | One legal move that the solver uses on the winning line. |
| Art | Code-drawn cards. Ranks as tabular numerals, suits as simple marks. No face sprites. |
| Name rule | Display name "Solitaire". "Klondike" is a store keyword and the rules name. Other solitaire games are separate games (Spider Solitaire, FreeCell, section 17), not variants of this one. |

### 12.6 Chess Puzzles (engine id `tactics`)

| Item | Spec |
|---|---|
| Package | `tactics` |
| Rule | 8x8 chess, FIDE movement. The side to move has a unique mate in N. Player plays the winning side only. Opponent replies are forced (or the unique defence). |
| Uniqueness (lead decision, 22 Nov 2026) | Unique mate sequence: at every player turn along the stored line, exactly one move keeps a forced mate. `countSolutions` returns the worst turn, capped at 2. A second mate on the last move makes the puzzle invalid. |
| Volume (review, 22 Nov 2026) | The bank needs at least 241 distinct starting positions per pack (150 regular plus the daily share). The first generator made only 40 Expert positions. Fixed: 965 distinct positions. |
| Packs | Easy mate in 1. Medium mate in 2. Hard mate in 2 with more pieces. Expert mate in 3. |
| Input | Tap from-square, tap to-square. Encode as cell picks on `GameEngine`. Promotion: queen only in the bank. |
| Generator | Pure Kotlin rules and a bounded mate search. Place a king plus a small piece set from a pattern list, then search. Accept only if the mate is unique. No Stockfish, no JNI, no new library. |
| Solver techniques | Checks, forced captures, mating patterns named in the hint sentence. |
| Name rule | Display name "Chess Puzzles". Not a chess server, not ratings, not clocks, not online play. |

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
| Sizes | Easy 5x5, Medium 6x6, Hard 7x7, Expert 7x7 with fewer signs. Hard has at least 10 signs; Expert has at most 8 (review fix, 22 Nov 2026). |
| Input | Mini Sudoku digit pad and notes. Signs drawn on cell edges. |
| Generator | Random Latin square. Add all signs, then remove signs and givens while unique and technique-solvable. |
| Solver techniques | Singles, sign chains (min/max bounds), naked pair. |

### 12.9 Towers (engine id `skyscrapers`)

| Item | Spec |
|---|---|
| Package | `skyscrapers` |
| Rule | Latin square of building heights 1 to n. A clue outside the grid gives how many buildings you see from that side. |
| Sizes | Easy 4x4, Medium 5x5, Hard 6x6, Expert 7x7. |
| Input | Digit pad and notes. Outside clues use the Nonogram clue band. |
| Generator | Random Latin square. Compute all clues, then remove clues while unique and technique-solvable. |
| Solver techniques | Edge clues 1 and n, height bounds from clues, singles. |
| Name rule | Display name "Towers". "Skyscrapers" is a store keyword. |

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
| Generator speed | Expert 15x15 takes about 50 s per puzzle, so the full Expert pack takes about 2 hours on the developer PC. Profile it before the bank card (MOB). |

## 13. Store listing

| Field | Draft |
|---|---|
| Title (30 max) | Witgrid: Offline Logic Puzzles |
| Short description (80 max) | Offline puzzles: sudoku, nonogram, solitaire, colour sort. No wifi needed. |
| Category | Puzzle |
| Target audience | 13 and older |

- Lead the listing with Sudoku, Nonogram, Solitaire and Colour Sort. The other games sit in the list, not in the title. Never state a game count.
- Long description keywords: "water sort" and "color sort" (for Colour Sort), "number link" and "connect the dots" (Number Link), "klondike" (Solitaire), "chess puzzles" and "mate in one" (Chess Puzzles), "skyscrapers" (Towers), "star battle" (Pet Crowns), "binary puzzle" (Binary). Use each term one time, in a sentence that describes the game. "Water Sort" never appears as a game name in the app or the title.
- The head search terms "offline games" and "no wifi games" belong to apps with 100M+ installs. Aim at long terms: "offline logic puzzles", "nonogram offline", "sudoku offline", "solitaire offline".
- Play rules: no emoji in the title, no repeated keywords, no ranking claims, no other company's brand.
- Screenshots: show one clean board for each lead game, the dark mode, and the "works offline" message.
- When expansion games ship, add their genre terms the same way. Never use a name from the section 17.1 avoid list.

## 14. Risks

| Risk | Effect | Mitigation |
|---|---|---|
| Few installs | Income stays below the goal | App community, long search terms, a new game in each update (section 17) |
| Offline players see no ads | Lower ad income | Premium offer when an ad cannot load; measure the offline share |
| AdMob account disabled for invalid traffic | Ad income stops permanently | Test ad units only; never tap live ads; no ad placement near tap targets |
| A Family A puzzle with two solutions or a need to guess | 1-star reviews | Two solvers check each Family A puzzle; hand test of each pack |
| Agent code passes its own tests but has logic bugs | Wrong puzzles, wrong hints, weak AI | Cross-family review; x3/x4 spec tests written from this plan, not from the code; lead batch gate |
| Generators that pass their tests but repeat themselves | Players see the same shapes | Every bank report lists grade histograms and distinct patterns. Reviews check variety, not only validity. |
| Hints that never finish | Players loop on Hint | Family B hints return the first move of a shortest winning path. Each engine has a hints-only test from deal to win. |
| Computer opponents that are too weak, too strong or slow | Bad reviews on board and card games | Strength is set by a fixed search budget, never by clock time. Each Family C lane has an AI report: win rate of each level against the level below, and the slowest move time on a low-end device. |
| Trademark complaint | Listing removed | Name rules in AGENTS.md and section 17.1. Check each new display name before its xC card. |
| Home gets long as games are added | Players cannot find games | F4 Home categories before Wave 1 games register |
| Scope growth before launch | Late launch | Expansion games do not block H5 or H6 (section 11) |
| Play rule changes (API level, 16 KB pages) | Blocked updates | Native stack; update the target API each year |

## 15. Open items

1. Public developer details on Play for an account that earns money.
2. Word list for Wave 3: source and licence (card H8).
3. Display names marked [Verify] in section 17: run a store and trademark search before each game's xC card.
4. Daily puzzle for Family C games: none at first (section 17.2). Review after Wave 2.

## 16. Build method: AI agents

All code comes from AI agents. The lead picks tasks, reads handoff notes, runs reviews and merges.

| File | Content |
|---|---|
| `AGENTS.md` | Rules for every agent: commands, module rules, hard rules, handoff format. |
| `CLAUDE.md` | Imports `AGENTS.md`, plus notes for Claude Code. |
| `docs/TASKS.md` | The backlog: one card for each task, with tier, owner, owned files, plan sections and "done when" commands. |
| `docs/AGENT_WORKFLOW.md` | Setup, the task loop, parallel work, known risks. |
| `scripts/agent-task.ps1` | Starts one task in its own git worktree with Codex, Claude, Gemini or Grok. |
| `scripts/review-task.ps1` | Cross-review of a task branch by another agent family. |
| `scripts/check-batch.ps1` | Merges several finished task branches into one scratch worktree and runs `gradlew check` one time. |
| `scripts/progress.py` | Rebuilds the progress bars at the top of this plan from `docs/TASKS.md`. |

### 16.1 Model tiers

| Tier | Work | Claude | Codex | Gemini (`agy`) | Grok |
|---|---|---|---|---|---|
| L | Tests from spec, wiring, strings, reports, banks | Haiku | `gpt-6-luna` | `gemini-4-hydrogen-medium` | `grok-4.7-build-fast` |
| M | Boards, screens, normal engines | Sonnet | `gpt-6.2-sol` medium | `gemini-4-hydrogen-high` | `grok-4.6` |
| H | Contracts, solvers, generators, AI search, gestures | Sonnet | `gpt-6.2-sol` high | `gemini-4-hydrogen-xhigh` | `grok-4.7` |

Haiku and Grok have no shell: they cannot run Gradle, bankgen or git, so they cannot do bank (xB) cards. The lead compiles, tests and commits their work. Codex cannot commit; the lead commits Codex work.

### 16.2 Test gate and machine load

1. A build agent runs only the tests of its own package, fixes every failure without weakening a test, and commits.
2. The lead tests finished branches together with `scripts/check-batch.ps1` (one `gradlew check`).
3. Run **one heavy job at a time**: a Gradle `check`, a bank generation or a full engine test run. Agents that only write code can run in parallel.
4. Run 2 or 3 Gemini agents at a time (quota, error 429).
5. Keep slow generator and AI tests small: sample 20 to 60 levels or games, not 150.
6. Local `./gradlew check` is the merge gate. GitHub CI runs only when started by hand.

## 17. Catalog expansion (owner, 27 Nov 2026)

After the launch catalog, the hub grows by one or more games in each update. Three waves, in order of value for effort. Popularity is from store charts and clone counts on 27 Nov 2026, not from keyword volumes [Verify with a keyword tool].

Legend: 🔥 top-chart genre, ▲ popular, ● niche. Effort: S mostly reuse, M new engine on a known pattern, L needs a new shared part, XL very large.

### 17.1 Rules for every expansion game

- One package per game: `engine/…/<id>/` and `app/…/games/<id>/`. Registration through `GameRegistry` / `PlayRegistry` / `MatchRegistry` (F2). Home, saves and ads need no game-specific change.
- Integer maths and `SplitMix64` only. The same seed gives the same deal, layout and AI move on every machine.
- Code-drawn art in the paper-and-ink style. No sprites, no photos, no new library unless the card permits it.
- Each game has a guided first level (intro), an interstitial default in `ConfigKeys` (`interstitial_levels_<id>`), rules text, a Home motif and a result thumbnail.
- **Names to avoid** (add to AGENTS.md rule 4): UNO, Yahtzee, Connect Four, Connect 4, Battleship, Othello, Unblock Me, Amaze, Ludo King, Scrabble, Boggle, Minecraft, Candy, plus the existing list. "Water Sort" only as a store keyword.
- **Do not add:** match-3, mahjong, tile match, online play of any kind, 50-variant solitaire, hexa sort, jigsaw, five-letter word guess.

### 17.2 Shared foundations (before or beside Wave 1)

| Card | Work | Tier | Unlocks |
|---|---|---|---|
| F1 | **Card kit.** Move the deck, card model and code-drawn card faces out of `klondike` into `engine/…/cards/` and `app/…/games/cards/`: `Card`, `Suit`, `Rank`, `Deck.shuffled(rng)`, card drawing, pile layout, drag-and-drop of a card run, fly-to-pile animation. Solitaire keeps working with no visible change. | M | Spider, FreeCell, Hearts, Spades, Crazy Eights |
| F2 | **Family C contract: games against the computer.** `MatchEngine<P, S, A>`: players, side to move, legal actions, apply, outcome (win, loss, draw), codec. `MatchGameModule` wraps it for Home, Play, Result and saves. A "level" is one match; the four packs are four computer strengths. Result shows Won / Lost / Draw. Loss = level failed: Continue takes back the last full turn (rewarded, or free with Premium); Restart is free. No bank and no daily puzzle for Family C (Home daily row skips it). Human reads this contract before merge. | H | All Wave 2 games |
| F3 | **Opponent AI kit.** Deterministic search with a node budget, never a clock: alpha-beta with iterative deepening and integer evaluation (perfect-information games), and determinised Monte Carlo playouts with `SplitMix64` (hidden-card and dice games). Strength levels Easy / Medium / Hard / Expert = budget plus a chance to pick a weaker move. A test harness plays level against level and reports win rates. | H | All Wave 2 games |
| F4 | **Home at scale.** Category sections on Home (Logic, Sort and Move, Cards, Board, Word), each a 2-column grid; a category is hidden when it has no game. Category is a field on the game module. No count in any text. | M | Needed before the first Wave 1 game registers |
| F5 | **Word list asset.** Word list from the source that H8 picks, profanity and slur filter, length bands, stored as a gzip asset; bankgen reads it. Licence text in the app licence screen. | M | Wave 3 |
| H8 | **Pick the word list and licence** (Human). For example a public-domain or permissive list; record the licence in `docs/decisions/word-list.md`. | Human | F5 |

### 17.3 Wave 1: solo games, reuse what exists

| Lane | Display name | Engine id | Family | Popularity | Effort | Interstitial | Continue |
|---|---|---|---|---|---|---|---|
| SP | Spider Solitaire | `spider` | B | 🔥 | S (after F1) | Each deal | Extra undo |
| FC | FreeCell | `freecell` | B | 🔥 | S (after F1) | Each deal | Extra undo |
| NB | Nuts & Bolts [Verify] | `nutsbolts` | B | 🔥 crowded | S | Each 2 levels | Extra empty bolt |
| MI | Minesweeper [Verify] | `minesweeper` | B | ▲ evergreen | S–M | Each 2 levels | Mine hit is flagged, play goes on |
| MP | Maze Paint | `mazepaint` | B | ▲ | S–M | Each 2 levels | None (no fail) |
| BE | Block Escape | `blockescape` | B | ▲ | M | Each 2 levels | None (no fail) |
| NM | Number Merge | `numbermerge` | B | 🔥 saturated | S | Each level | Remove the two smallest tiles |
| YA | Yatzy [Verify] | `yatzy` | B | ▲ | M | Each level | One extra reroll |
| SL | Sliding Puzzle | `sliding` | B | ● | S | Each 2 levels | None (no fail) |
| ME | Memory Pairs | `memory` | B | ▲ | S | Each 2 levels | Five extra flips |
| SH | Shikaku | `shikaku` | A | ● | M | Each 2 levels | Extra mistake |

**Spider Solitaire.** 10 tableau columns, 104 cards. Packs set the suits: Easy 1 suit, Medium 2, Hard 4 with more face-up cards, Expert 4. Build down regardless of suit; move only same-suit runs; a full K→A same-suit run leaves the tableau. Deal from the stock adds one card to each column (not allowed with an empty column). Generator: deal from `SplitMix64`, keep solver-won seeds (Klondike `SolveVerdict` pattern: Unknown is never a loss). Bank stores seeds.

**FreeCell.** 8 columns, 4 free cells, 4 foundations, all cards face up. Move one card (or a run up to the free-space limit). Packs by solver length and free cells needed: Easy, Medium, Hard, Expert (fewest free cells at the end). Generator: seed deal, solver must win. Almost every deal is winnable; reject the rest.

**Nuts & Bolts.** Copy the Colour Sort engine as a new package (do not edit `coloursort`). Bolts hold coloured nuts; move the top run of nuts onto a bolt with the same top colour or an empty bolt. Packs: 4, 6, 8, 10 bolts; capacity 4 (Easy) to 5 (Expert). Board: vertical bolts with threaded lines and hex nuts; a nut spins up and across unless reduce-motion.

**Minesweeper.** No-guess: each level opens with a given safe area, and a deduction solver must clear the board with no guess. Packs: Easy 8x8 10 mines, Medium 12x12 25, Hard 16x16 45, Expert 16x24 80 [Estimate]. Actions: Reveal, Flag (long-press or mode switch, same as Nonogram Fill/Cross). Reuse the Mosaic neighbour-count code where it fits. Bank stores the seed and the opening cell.

**Maze Paint.** A ball slides in one direction until a wall stops it and paints each cell it passes. Win when every open cell is painted. Swipe input. Generator: random walls, BFS for a solution, pack by shortest solution length. No fail state.

**Block Escape.** Sliding blocks on a 6x6 board (Expert 7x7); slide the key block out of the exit. Generator: random layout, BFS from the goal for the minimum move count; pack by move count. No fail state. Never "Rush Hour" or "Unblock Me".

**Number Merge.** 4x4 board (Easy 5x5). Swipe merges equal tiles. New tiles come from the state's `SplitMix64`, so a level replays the same. Win when a tile reaches the pack target (Easy 256, Medium 512, Hard 1024, Expert 4096). Fail when no swipe can move. Never name the game or a pack after a number.

**Yatzy.** Solo score attack with five dice and three rolls per turn. 15 categories (Scandinavian rules, upper bonus at 63). Win when the final score reaches the pack target (Easy 150, Medium 190, Hard 220, Expert 250) [Estimate; tune with a simulation in YA2]. Dice from the state's `SplitMix64`. Code-drawn dice. Never "Yahtzee".

**Sliding Puzzle.** 3x3 (Easy) to 6x6 (Expert) numbered tiles. Generator: random solvable permutation (parity check), pack by optimal or bounded move count. Picture tiles are a later option.

**Memory Pairs.** Turn two cards; a pair stays face up. Grid 4x4 to 6x6 with code-drawn symbols (shapes and colours, colour-blind safe). Harder packs have a flip limit; a fail gives Continue. Layout from seed.

**Shikaku.** Family A. Divide the grid into rectangles; each rectangle holds one number equal to its area. Unique and technique-solvable. Input: drag a rectangle; encode each cell as `SetDigit` with a rectangle id (the Number Link pattern). Sizes 7x7, 10x10, 12x12, 15x15. Techniques: single fitting rectangle, cell reachable by one clue, overlap of all placements.

### 17.4 Wave 2: games against the computer (after F2 and F3)

| Lane | Display name | Engine id | Popularity | Effort | Notes |
|---|---|---|---|---|---|
| TT | Tic-Tac-Toe | `tictactoe` | ▲ recognised, low value | S | First user of F2 and F3. Packs: 3x3, then 5x5 four-in-line, 7x7 five-in-line. |
| FR | Four in a Row | `fourinrow` | ▲ | S–M | 7x6 board, alpha-beta. Never "Connect Four". |
| MA | Mancala | `mancala` | ▲ | S–M | Kalah rules, 6 pits, 4 seeds. |
| RV | Reversi | `reversi` | ▲ | M | 8x8, alpha-beta with a position table. Never "Othello". |
| DB | Dots and Boxes | `dotsboxes` | ● | M | 4x4 to 6x6 boxes; chain rule in the AI evaluation. |
| SB | Sea Battle | `seabattle` | ▲ | M | Hidden fleets, 10x10. AI uses hit-and-hunt with probability counts. Never "Battleship". |
| CK | Checkers | `checkers` | ▲ | M–L | English draughts (8x8, forced capture). One rule set only. |
| CH | Chain Burst | `chainburst` | ▲ | M | Orbs explode at critical mass and convert neighbours. 2 players against the computer. [Verify name] |
| HE | Hearts | `hearts` | ▲ | M–L | F1 card kit; 3 computer players; pass three cards. |
| SD | Spades | `spades` | 🔥 (US) | L | Partners and bidding; 3 computer players. |
| CE | Crazy Eights | `crazyeights` | 🔥 | L | Shedding game with original card art and action cards of our own design. Never "UNO" or its card names. |
| DO | Dominoes | `dominoes` | ▲ | M–L | Draw and block games; code-drawn tiles. |
| LU | Ludo | `ludo` | 🔥 | L | 4 players, dice, 3 computer players. Offline only. Never "Ludo King". |
| CC | Chess | `chess` | 🔥 dominated | XL | Reuse the `tactics` move rules. Strength by search budget. No online play, no ratings. Last in the wave. |

**Family C level rules.** A pack is a computer strength (Easy, Medium, Hard, Expert). "Level N" counts matches in that pack; it only goes up. The hint names one good move from the AI at Expert strength. Statistics show wins, losses and draws.

### 17.5 Wave 3: word games (after F5 and H8)

| Lane | Display name | Engine id | Family | Popularity | Effort | Notes |
|---|---|---|---|---|---|---|
| WS | Word Search | `wordsearch` | B | 🔥 | M | Grid with hidden words in 8 directions; drag to select. Theme lists from the word asset. Packs by grid size (8x8 to 14x14) and directions allowed. |
| HM | Secret Word [Verify] | `hangman` | B | ▲ | M | Guess letters; six wrong letters fail the level. No gallows drawing: a code-drawn candle row burns down. Continue = one extra wrong letter. |

English only at first. A language list is a later card.

### 17.6 Card lanes

Same lane pattern as the launch catalog (v1 Phase 7, in `docs/archive/TASKS-v1.md`). Cards are in `docs/TASKS.md`.

| Family | Engine cards | App cards |
|---|---|---|
| A (Shikaku) | x1 rules and counter, x2 technique solver and hints, x3 generator, x4 independent spec tests | xA board, xB bank, xC intro, config, registration |
| B (Waves 1 and 3) | x1 rules, state, actions, `isWon` / `isLost`, codec; x2 hint, packs, generator; x3 independent spec tests | xA board, xB bank of layouts or seeds, xC intro, Continue, config, registration |
| C (Wave 2) | x1 rules, state, actions, outcome, codec; x2 AI strengths and hint, AI report; x3 independent spec tests | xA board, xC intro, Continue, config, registration (no bank) |

Each xC card also: interstitial default in `ConfigKeys`, display name and descriptor strings, rules text, Home motif, result thumbnail, the six `gameNameResource` maps, and a trademark and store check of the display name.

**Order.** F4 first (Home at scale). Then Wave 1 lanes in parallel, at most one heavy job at a time: NB, MI, MP, NM, SL, ME, SH and BE can start now; SP and FC after F1; YA any time. F2 and F3 run beside Wave 1. Wave 2 starts with TT (the contract test), then FR, MA, RV. Card games after F1 and F3. Wave 3 after H8 and F5.
