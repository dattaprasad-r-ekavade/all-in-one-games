# Witgrid: Offline Logic Puzzles — Development Plan

Plan date: 16 Nov 2026. Status: no code written yet. Agent workflow and task backlog added 16 Nov 2026 (section 16).

This plan replaces the game design document (the GDD PDF in this folder) where the two differ. Section 2 lists the differences.

Labels used in this plan:
- **[Estimate]** — my estimate, not measured data.
- **[Verify]** — a fact from web research on 16 Nov 2026 that you must check again before you depend on it.

---

## 1. Product summary

| Item | Value |
|---|---|
| Title | Witgrid: Offline Logic Puzzles (30 characters, the Play limit) |
| Platform | Android, Google Play. No iOS at launch. |
| Product | Many small logic games in one app. All games work with no connection. |
| Launch games | Crowns, Binary, Nonogram, Mini Sudoku 6x6 |
| USP | Clean UI, low distraction, no ad during a puzzle, hints that explain the logic |
| Income | Interstitial ad at level finish, rewarded ads for hints / checks / lives, one-time Premium at $7.99 |
| Income goal | About $100 each month |
| Team | One developer with AI coding help |

## 2. Decisions and differences from the GDD

| Topic | GDD | This plan |
|---|---|---|
| Name | Gridwise / Fivefold / Daily Five (all collide with other apps) | Witgrid: Offline Logic Puzzles |
| Launch modes | 5 | 4. Path moves to wave 3. |
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
| 90 days after launch | Income trend toward $100 each month | Add wave 1 games; test Premium price; then decide to continue or stop |

## 4. Design principles and art direction

1. **Clean UI.** One screen has one job. No pop-up offers during play.
2. **No ad interrupts a puzzle.** Ads are presented only outside an active puzzle board. Interstitials show only after a level finish and the player's tap on "Next".
3. **Offline first.** Each game, each level and each save works with no connection.
4. **Fair puzzles.** Each puzzle has exactly one solution and needs no guess.
5. **Hints explain.** A hint shows the next logical step and says why.
6. **Generic names.** No name or look from another company's game.

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

**Art work that you must make:** app icon, feature graphic (1024 x 500), 6 to 8 screenshots, and the colour palette.

## 5. Launch scope

**In the launch**
- 4 games, each with 4 difficulty packs and one daily puzzle.
- Home screen with the game list, progress, and streak.
- Play screen with undo, redo, mark mode, hint, check, and timer.
- Result screen, statistics, settings.
- Streak with one free freeze each week, calendar of daily puzzles.
- One optional local reminder notification.
- Ads, consent, Premium purchase, analytics, crash reports, Remote Config.
- Analytics and Crashlytics collection start only after explicit opt-in in Settings and stop when the player opts out.

**Not in the launch**
- Path mode, widget, leaderboards, share card, cloud save, accounts, subscription, banner ads, app-open ads, on-device puzzle generation, iOS, web.

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

- Each bank puzzle stores its solution, its difficulty pack and its technique grade.
- Two different solvers check each puzzle: the technique solver and a brute-force solver that counts solutions.
- The first level of each game is a small, guided puzzle with one coach line. There is no tutorial wall.

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
  engine/      Pure Kotlin, no Android code. Grid model, solvers, generators, graders, hint logic.
  bankgen/     JVM command-line tool. Uses engine/ to build and validate the puzzle bank on the PC.
  app/         Android app. Compose UI, storage, ads, billing, analytics.
    assets/bank/   One compressed file for each game and pack.
```

- `engine/` has no Android dependency. It runs in JVM unit tests on Windows, and it can become a Kotlin Multiplatform module later.
- `bankgen/` writes the bank files into `app/assets/bank/`. The bank is a build output that you commit to the repository.
- Use integer maths only in `engine/`. Write a small integer PRNG (for example SplitMix64). Do not use `kotlin.random.Random(seed)`, because a new Kotlin version can change its output.

### 7.3 One interface for each game

Each game is one package that implements the same interface. A new game needs no change to the home screen, the save system, the ad logic or the hint UI.

The interface has two halves, so that `engine/` stays free of Android code:
- `GameEngine<P, S>` in `engine/` (task E1): rules, solution counter, technique solver, grader, hints, generator, state codec.
- `GameModule` in `app/` (task A3): wraps one `GameEngine`, and adds `levelsPerInterstitial` and the Compose `Board`.

The sketch below shows the combined surface.

```kotlin
interface GameModule<S : GameState> {
    val id: String                      // "crowns", "binary", ...
    val levelsPerInterstitial: Int      // 1 for long levels, 2 for short levels
    fun loadLevel(ref: LevelRef): Level
    fun newState(level: Level): S
    fun applyMove(state: S, move: Move): S
    fun isSolved(state: S): Boolean
    fun findMistakes(state: S): List<Cell>
    fun nextHint(state: S): Hint        // technique name, cells, explanation text
    fun encode(state: S): ByteArray     // for the save system
    fun decode(bytes: ByteArray): S
    @Composable fun Board(state: S, onMove: (Move) -> Unit)
}
```

### 7.4 Puzzle bank format

One text line for each puzzle, in a gzip file for each game and pack:

`id | size | givens or regions | solution | pack | technique grade`

[Estimate] The full launch bank is smaller than 1 MB.

### 7.5 Saves

- Save the level state after each move. A crash or a closed app loses nothing.
- Store for each level: state, elapsed time, hints used, completed flag, best time.
- One shared streak day is earned by completing any one of that local date's four daily puzzles. Completing more daily puzzles that date does not add another day.
- Keep at most one free streak freeze. Refill it to one each local Monday; unused freezes do not accumulate. Automatically use it to cover one missed local day.
- Store the streak and freeze state with the device date. There is no server check.

### 7.6 Hint logic

1. If the board has a mistake, the hint points at the mistake first.
2. If not, the technique solver finds the easiest next step.
3. The hint highlights the cells, names the technique, and shows one sentence that explains the step.
4. The player then applies the step, or taps "Apply".

## 8. Monetisation specification

### 8.1 Interstitial ads

Show an interstitial only when all these conditions are true:
- The player finished a level and then tapped "Next" on the result screen.
- The count of finished levels since the last interstitial is equal to or more than the game's `levelsPerInterstitial`.
- The time since the last interstitial is equal to or more than `interstitial_min_gap_sec`.
- The session is not the first session after install.
- The player does not have Premium.
- An ad is loaded. If no ad is loaded, continue with no delay.

Default `levelsPerInterstitial`: Crowns 1, Nonogram 10x10 and larger 1, Binary 2, Mini Sudoku 2, Nonogram 5x5 2.

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
| Logic grids (the four launch games, later sudoku and killer sudoku) | "Mistake limit" is a setting, **off by default**. When it is on, the game flags a wrong entry immediately, and three mistakes fail the level. |
| Games where failure is natural (later: block puzzle, minesweeper, sort games) | Lives are always part of the game: a revive in block puzzle, a second chance in minesweeper, an extra tube or undo in sort games. |

When a level fails or lives are gone, show two buttons:
1. "Continue" — one rewarded ad, or free with Premium.
2. "Restart" — always free, with no ad.

For the four launch logic games, Continue grants one additional mistake and keeps the board as-is. Each later Continue grants one more mistake.

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
| Home | Game cards with progress, daily puzzle state, streak and freeze count | One quiet Premium row |
| Game | Four packs with progress, the daily puzzle, "Continue" | Optional rewarded hint and check credits before entering Play |
| Play | Grid, undo, redo, mark mode, hint, check, timer | None; use free, offline or previously earned credits |
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
- H0.1 (you): trademark check, Play Console, AdMob account, developer website with privacy policy and `app-ads.txt`, export GDD section 7 again.
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
- H5 (you): art approval, upload keystore, closed test with 12 or more testers for 14 continuous days, policy forms, production access. Tester bugs become new cards.

**Exit:** Google approves production access.

### Phase 6 — Launch and first 30 days (week 10 onward)
- H6 (you): replace test ad units with live ad units (Gradle properties outside the repo), check `app-ads.txt` status, staged rollout, posts in your app community and puzzle communities, weekly review of the section 10 measures, one Remote Config change at a time.
- Start wave 1 with the post-launch card template at the end of `docs/TASKS.md`.

## 12. Post-launch game waves

Release about one game each 2 to 3 weeks. Each new game is a store update and a reason to post.

| Wave | Game | Content source | Level time | Interstitial rule | Notes |
|---|---|---|---|---|---|
| 1 | Sudoku 9x9 | Bank | 5 to 15 min | Each level | Reuses the 6x6 engine |
| 1 | Block puzzle (8x8, place shapes, clear lines) | Procedural, endless | 3 to 10 min | At game over | Revive with a rewarded ad |
| 1 | Minesweeper, no-guess | Procedural + solver | 1 to 5 min | Each level | Second chance with a rewarded ad |
| 2 | Arrows escape | Bank | 30 to 120 s | Each 2 levels | 2026 breakout genre; line art fits the style |
| 2 | Colour sort | Bank | 45 to 120 s | Each 2 levels | Extra tube with a rewarded ad |
| 2 | Number merge | Procedural, endless | 3 to 10 min | At game over | Undo with a rewarded ad |
| 3 | Number link and Path | Bank | 20 to 90 s | Each 2 levels | The Path generator needs walls for a unique solution |
| 3 | Killer sudoku | Bank | 10 to 20 min | Each level | Strong fit with the brand |
| 3 | Klondike solitaire | Procedural + solver (winnable deals) | 3 to 7 min | Each game | Needs code-drawn cards |
| 4 | Word search | Procedural + word list | 90 to 240 s | Each level | Widens the audience |
| 4 | Bridges, tents, calcudoku | Bank | 1 to 5 min | Each level | Low demand; cheap library growth |

**Do not add:** screw sort, hexa sort, tile match, mahjong, jigsaw, five-letter word guess, sliding block escape.

**Names to avoid in the app and the store listing:** Queens, Tango, Zip, LinkedIn, NYT, Takuzu, Binairo, Picross, KenKen, Tetris, Block Blast, 2048, Flow Free, Rush Hour, Wordle.

**Later options, after the income goal is met:** share card, home-screen widget, nonogram picture packs, more ad networks through AdMob mediation (after about 1,000 to 2,000 daily players), iOS.

## 13. Store listing

| Field | Draft |
|---|---|
| Title (30 max) | Witgrid: Offline Logic Puzzles |
| Short description (80 max) | Clean offline logic puzzles: nonogram, sudoku, star battle, binary. No wifi. |
| Category | Puzzle |
| Target audience | 13 and older |

- The head search terms "offline games" and "no wifi games" belong to apps with 100M+ installs. Aim at long terms: "offline logic puzzles", "nonogram offline", "mini sudoku", "binary puzzle", "star battle".
- Play rules: no emoji in the title, no repeated keywords, no ranking claims, no other company's brand.
- Screenshots: show one clean board for each game, the dark mode, and the "works offline" message.
- **[Verify]** "star battle" as a free term. The web check found no registration, but it was not an official search.

## 14. Risks

| Risk | Effect | Mitigation |
|---|---|---|
| Few installs. A competitor with a similar feature list has only 100+ installs. | Income stays below the goal | Your app community, long search terms, one new game each 2 to 3 weeks |
| Offline players see no ads | Lower ad income | Premium offer when an ad cannot load; measure the offline share |
| AdMob account disabled for invalid traffic | Ad income stops permanently | Test ad units only; never tap live ads; no ad placement near tap targets |
| A puzzle with two solutions or a need to guess | 1-star reviews | Two solvers check each puzzle; hand test of each pack |
| Scope growth | Late launch | Section 5 is fixed. New ideas go to section 12. |
| Trademark complaint | Listing removed | The name rules in sections 6 and 12 |
| Interstitial after each level harms reviews | Lower rating, lower search rank | Minimum gap, "each 2 levels" for short levels, Remote Config tuning |
| $7.99 is higher than comparable apps | Fewer Premium sales | Premium includes unlimited hints and lives; price experiment |
| Play rule changes (API level, 16 KB pages) | Blocked updates | Native stack; update the target API each year |

## 15. Open items

1. Official trademark check of "Witgrid".
2. ~~Ads SDK choice~~ — decided 16 Nov 2026: Next-Gen (`docs/decisions/ads-sdk.md`).
3. Public developer details on Play for an account that earns money.
4. GDD section 7 is missing from the PDF.
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

- **Engine first.** `engine/` is pure Kotlin with a strong test oracle: "exactly one solution, and the technique solver solves it". Agents do well on this work, and the tests catch their mistakes.
- **Contracts before lanes.** E1 (engine) and A3 (app) fix the interfaces. After that, the four game lanes and the four boards run in parallel, with no shared files.
- **Two families.** Codex builds most of the engine and Claude builds most of the UI. Tests and reviews come from the other family, so one model's misreading of the spec does not pass through unseen.
- **Logic out of the UI.** Ad rules, streaks and allowances are plain Kotlin with JVM tests. Agents cannot check a Compose screen by eye, but they can check a unit test.
- **Humans keep the risky steps.** Accounts, keys, live ad IDs, hand tests, art approval and store forms are cards with tier **Human**.

