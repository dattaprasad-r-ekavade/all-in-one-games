# Witgrid extra launch games — engineering-fit effort

Date of notes: 18 Nov 2026. Repo read-only. Local plan and code are the source of truth for card counts. Public pages are used only for collection-app architecture comparison.

Baseline cost of one *new logic game* in this project, from the launch-lane template and the post-launch template:

- Engine: `x1` model + `x2` technique solver/hints + `x3` generator + `x4` independent tests. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md)
- App: board card (A5–A8 pattern), intro level, register in `GameRegistry`. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md)
- Bank: generate 4 packs × 150 + 365 daily (600 + 365), plus one intro. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §6
- Lives games add one revive card (plan 8.4). — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) post-launch template; [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §8.4
- Post-launch template also adds `levelsPerInterstitial` / config key and store update notes. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md)

That baseline is **8–10 task cards** for a banked logic grid, **9–11** if the game uses lives. Plan calendar for post-launch waves is **one game each 2 to 3 weeks**. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §12

Closed-test remaining *code* cards on 18 Nov 2026: **M6** (blocked on Play billing product / licence tester), **M7** analytics, **S3** release config. Human-gated: **H0.1**, **H1** bank hand-test, **H4**, **H5**. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) index

---

## Which candidates reuse Mini Sudoku, Crowns, Binary, Nonogram, or BoardCanvas — vs a new input model?

### Takeaway

Sudoku 9x9, Killer, Calcudoku, Futoshiki, and Skyscrapers reuse the Mini Sudoku *input* (select cell + digit/notes pad, `Move.SetDigit` / `SetNotes`). Tents and no-guess Minesweeper reuse Binary/Crowns/Nonogram *marks* (`Move.SetMark` + tap/drag). Hashi, Slitherlink, Numberlink, colour sort, arrows, and block puzzle need a new input model; `Move` is a sealed three-variant type and BoardCanvas is a square cell grid.

### Cited Findings

- Each game is one package that implements `GameEngine` in `engine/` and `GameModule` in `app/`. Plan text: a new game needs no change to the home screen, the save system, the ad logic or the hint UI. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §7.3
- `GameEngine<P,S>` requires decode/encode puzzle, initial state, applyMove, isSolved, mistakes, countSolutions (cap 2), nextStep, grade, generate, encode/decode state. — [engine/.../core/GameEngine.kt](D:\cash-grabs\all-in-one games\engine\src\main\kotlin\com\datathecodie\witgrid\engine\core\GameEngine.kt)
- `Move` is a sealed interface with exactly three variants: `SetMark`, `SetDigit`, `SetNotes`. Lanes handle these with exhaustive `when`. — [engine/.../core/Move.kt](D:\cash-grabs\all-in-one games\engine\src\main\kotlin\com\datathecodie\witgrid\engine\core\Move.kt)
- Mini Sudoku state rejects `SetMark`. Crowns requires `SetMark` only. Adding a fourth `Move` variant would force every existing engine `when` to be updated. — [engine/.../minisudoku/MiniSudokuState.kt](D:\cash-grabs\all-in-one games\engine\src\main\kotlin\com\datathecodie\witgrid\engine\minisudoku\MiniSudokuState.kt); [engine/.../crowns/CrownsState.kt](D:\cash-grabs\all-in-one games\engine\src\main\kotlin\com\datathecodie\witgrid\engine\crowns\CrownsState.kt)
- Mini Sudoku geometry is hardcoded: `MINI_SUDOKU_SIZE = 6`, digits `1..6`, 2×3 boxes, note mask 6 bits. The board keypad iterates `1..MINI_SUDOKU_SIZE`. Input rejects digits above 6. — [engine/.../minisudoku/MiniSudokuPuzzle.kt](D:\cash-grabs\all-in-one games\engine\src\main\kotlin\com\datathecodie\witgrid\engine\minisudoku\MiniSudokuPuzzle.kt); [app/.../games/minisudoku/MiniSudokuBoard.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\minisudoku\MiniSudokuBoard.kt); [app/.../games/minisudoku/MiniSudokuBoardInput.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\minisudoku\MiniSudokuBoardInput.kt)
- Mini Sudoku techniques at launch: naked single, hidden single, locked candidates, naked pair. Pack bands map Easy→naked single, Medium→hidden single, Hard→locked candidates, Expert→naked pair. — [engine/.../minisudoku/MiniSudokuTechniques.kt](D:\cash-grabs\all-in-one games\engine\src\main\kotlin\com\datathecodie\witgrid\engine\minisudoku\MiniSudokuTechniques.kt)
- Mini Sudoku play uses `BoardCanvas` with `dragPaintEnabled = false`, cell selection, and `SudokuPlayActions` (notes, erase, digit). — [app/.../games/minisudoku/MiniSudokuBoard.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\minisudoku\MiniSudokuBoard.kt); [app/.../ui/board/SudokuPlayActions.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\ui\board\SudokuPlayActions.kt)
- Crowns input: tap cycles empty → X → crown; drag paints X; optional auto-X around a crown as extra `SetMark`s in one history record. — [app/.../games/crowns/CrownsBoardInput.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\crowns\CrownsBoardInput.kt); [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §6.1
- Binary input: tap cycles empty → A → B; long-press clears; uses `SetMark` mapped onto two symbols. — [app/.../games/binary/BinaryBoardInput.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\binary\BinaryBoardInput.kt); [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §6.2
- Nonogram input: Fill/X mode switch, drag paints a line, optional auto-cross of completed lines. — [app/.../games/nonogram/NonogramBoardInput.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\nonogram\NonogramBoardInput.kt); [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §6.3
- `BoardCanvas` is a square cell grid: tap, drag-paint, long-press, two-finger undo, haptics, completion wave. `gridSize` must be in `1..32`. Drag paint can be disabled. Panning exists for enlarged boards. — [app/.../ui/board/BoardCanvas.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\ui\board\BoardCanvas.kt); [app/.../ui/board/BoardGestures.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\ui\board\BoardGestures.kt)
- Registration is one object in `GameRegistry.games` plus one object in `engine.Registry.engines`. `EngineGameModule` already owns bank paths `bank/$id-{pack}.txt.gz`, intro, daily, interstitial interval, and level looping. — [app/.../games/GameRegistry.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\GameRegistry.kt); [engine/.../Registry.kt](D:\cash-grabs\all-in-one games\engine\src\main\kotlin\com\datathecodie\witgrid\engine\Registry.kt); [app/.../games/GameModule.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\GameModule.kt)
- `bankgen` maps engines from `Registry`, but grade encoding is a hardcoded `when (gameId)` of the four launch IDs. A fifth ID currently throws. — [bankgen/.../BankGame.kt](D:\cash-grabs\all-in-one games\bankgen\src\main\kotlin\com\datathecodie\witgrid\bankgen\BankGame.kt)
- Plan wave table: Sudoku 9x9 “Reuses the 6x6 engine”; Killer sudoku wave 3 “Strong fit with the brand”; Bridges, tents, calcudoku wave 4 “Low demand; cheap library growth”; Block puzzle / Minesweeper / Colour sort / Arrows / Number merge are lives or endless procedural games. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §12
- Name ban list includes KenKen (Calcudoku must not use that word), Flow Free (Numberlink), Rush Hour, Tetris, Block Blast, 2048. — [AGENTS.md](D:\cash-grabs\all-in-one games\AGENTS.md) §5; [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §12
- Guru Games (Total Sudoku, Jul 2026): Classic Sudoku encoded 3×3 boxes and “row+column+box” into the engine. Killer added cages and “broke half of what we had written”. Jigsaw needed stored region maps, not coordinate-derived boxes. Killer and Mathdoku/Calcudoku look similar but do not share cage logic because Calcudoku cages use + − × ÷. Shared pieces: rows, columns, game state, input, broad hint process. Not shared: cage arithmetic, irregular regions, variant explanations. — [What Building Six Sudoku Variants Taught Us About Software Architecture](https://medium.com/@gurugames/what-building-six-sudoku-variants-taught-us-about-software-architecture-f98bf26b901b)
- Simon Tatham Portable Puzzle Collection: a new puzzle is a new backend with four data structures (`game_params`, description, `game_state`, UI); people adding backends should generally not edit the middle end. — [SGT Puzzles HACKING](https://www.chiark.greenend.org.uk/ucgi/~ian/git?p=sgt-puzzles.git;a=blob_plain;f=HACKING;hb=HEAD)
- PuzzlePack (Flutter 8-in-1): each mode is a feature folder; no game imports another; home grid is driven by `modes_config`; generators are pure Dart with zero UI imports. — [PuzzlePack product page](https://graphicfort.com/codecanyon/item/puzzlepack-8in1-flutter-puzzle-game-bundle/64717542)

### Inferences

Reuse map against Witgrid’s *actual* primitives (not the plan’s “reuses 6x6 engine” slogan):

| Candidate | Input primitive | Engine reuse | New Move / geometry? |
|---|---|---|---|
| Sudoku 9x9 | Mini Sudoku select+digit+notes | Copy Mini Sudoku lane; size/digits/boxes/techniques are *not* parameterized today | No new Move. Hardcoded 6 must become 9 / 3×3 / digits 1–9 |
| Killer Sudoku | Same digit pad | Mini Sudoku Latin square + stored cages (like Crowns regions for drawing) | No new Move. Cage overlay + sum uniqueness. Guru Games: this is the first “second variant” that breaks Classic assumptions |
| Calcudoku | Same digit pad | Killer-like cages *plus* operators | No new Move. Solver/generator are a new family; name must be Calcudoku, not KenKen |
| Futoshiki | Digit pad + inequality glyphs on edges | Latin square + pairwise inequalities | No new Move. Draw extra edge marks on BoardCanvas |
| Skyscrapers | Digit pad + outside clues | Latin square + visibility counts | No new Move. Clues sit outside the 9×9 / 6×6 cells (Nonogram already draws outside-line clues) |
| Kakuro | Digit pad on white cells; black clue cells | Digit + run-sum, not a Latin square | No new Move if clues are givens. Cell drawing is mixed (clue vs playable) |
| Tents | Crowns/Binary `SetMark` cycle (tent / grass / empty) | New rules (trees, adjacency, row/col counts) | No new Move. BoardCanvas tap/drag-X fits |
| Hashi (Bridges) | Connect island pairs, 1 or 2 bridges | New graph, not a cell fill | **New input.** `SetMark` on cells cannot represent an edge between two islands. Would extend sealed `Move` or overload a cell as an edge — both leak into other lanes |
| Slitherlink | Draw/X edges of cells | Loop on grid *lines*, not cell fills | **New geometry.** BoardCanvas hit-tests *cells*, not edges. Dots+edges is a different canvas |
| Numberlink | Draw a path between pair endpoints | Path occupancy | **New input.** Drag-paint exists, but a path-occupancy Move (or many SetMarks with colour IDs) is not in `Mark` (only EMPTY/FILLED/CROSSED) |
| No-guess Minesweeper | Nonogram Fill/X (open vs flag) | New flood-open + mine field; fail is natural | `SetMark` can encode open/flag/closed. Flood-open is extra Moves in one gesture, like Crowns auto-X. Lives/second-chance is a new fail model |
| Colour sort | Pour between tubes | Not a grid | **New UI.** BoardCanvas square grid does not apply |
| Arrows escape | Rotate/slide arrows | Line-art grid possible | **New gesture** (rotate), not tap-cycle or digit pad |
| Block puzzle | Place polyominoes, clear lines | Piece inventory + 8×8 field | **New input** (drag a shape, not paint cells). Endless/procedural, revive |

“Minimal effort” in this codebase means: keep `Move` unchanged, keep BoardCanvas cell hit-testing, keep the mistake-limit (not lives) rule, and copy an existing board-input object.

Simon Tatham’s “don’t touch the middle end” already matches Witgrid: `GameEngine` + `EngineGameModule` are the middle end. Cost lives in the *backend* (techniques + unique generator) and the *board draw/input*, not in Home/ads/saves.

Guru Games is direct evidence that Mini Sudoku → 9x9 is a size/technique fork, while Mini Sudoku → Killer is an architecture fork of constraint groups. Calcudoku is a second fork on top of Killer.

### Gaps

- No Witgrid-local design for Hashi/Slitherlink edge hit-testing. Whether BoardCanvas can grow an “edge mode” without a new card on `ui/board/**` is untested.
- Whether 9x9 Expert can stay guess-free with only the current four Mini Sudoku techniques is not measured in-repo. Extra techniques (hidden pair, naked triple, X-wing, etc.) would inflate the `x2` card from M/H toward the Nonogram/Crowns H generator tier.
- No file-by-file hour log for CR/BI/NO/MS lanes exists in TASKS.md, only card tiers (L/M/H). Calendar time below is inferred from those tiers plus the plan’s “2 to 3 weeks per game”.

---

## Realistic card count and calendar time for (a) Sudoku 9x9, (b) Killer or Calcudoku, (c) another Nikoli grid, (d) no-guess Minesweeper, (e) colour sort / arrows / block puzzle

### Takeaway

Cheapest extra is Sudoku 9x9 at about **7–9 cards / ~2 weeks**. Killer is **10–12 / ~3 weeks**; Calcudoku is a step above Killer. Nikoli grids split: Futoshiki/Skyscrapers/Tents ~**9–11 / 2–3 weeks**; Hashi/Slitherlink/Numberlink/Kakuro ~**12–15 / 3–4 weeks** plus a possible core `Move`/canvas change. Minesweeper ~**10–12** including revive. Colour sort / arrows / block puzzle ~**12–16** and they are not BoardCanvas games.

### Cited Findings

Launch-lane card set (the unit of estimate):

| Card | Work | Tier |
|---|---|---|
| x1 | Rules, puzzle/state, bank payload, `countSolutions` | M |
| x2 | Technique solver, grade, nextStep text, pack bands | H |
| x3 | Generator for each pack | M or H (Crowns/Nonogram H; Binary/Mini Sudoku M) |
| x4 | Independent tests / fixtures | L |
| Board (A5–A8) | Compose board + UI test that plays first bank puzzle to solved | M |
| Intro (A12 pattern) | Guided first level + coach string | L (launch A12 covered all four games in one card) |
| Bank generate | 600 + 365, unique, technique-solvable, committed assets | L (B2/B5 pattern); generation is slow on the developer PC |
| Config + store notes | `levelsPerInterstitial`, Remote Config key, listing | L |

— [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) game-lane table, A5–A8, A12, B2, post-launch template

Hard rules that inflate `x2`/`x3` for every banked game:

- Every bank puzzle has exactly one solution and the technique solver solves it with no guess. — [AGENTS.md](D:\cash-grabs\all-in-one games\AGENTS.md) §5 rule 6; [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §6.5
- Integer maths only; PRNG is project `SplitMix64` only; same seed, same output. — [AGENTS.md](D:\cash-grabs\all-in-one games\AGENTS.md) §4
- Two solvers check each puzzle: technique solver and brute-force `countSolutions`. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §6.5
- Content per game: 4×150 regular + 365 daily + 1 intro. Current committed bank: 3,864 puzzles in 24 files (4 games × 6 files). — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §6; [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) U7/U9 lead notes
- Bank target: full launch bank smaller than 1 MB. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §7.4
- Lives split: logic grids use optional mistake limit (off by default). Block puzzle / minesweeper / sort always have lives and a revive/second-chance/extra-tube. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §8.4
- Post-launch cadence in the plan: about one game each 2 to 3 weeks. Wave 1 listed games: Sudoku 9x9, block puzzle, minesweeper. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §12
- 90-day review: “Add wave 1 games” only if income trend is toward $100/month. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §3
- H1 hand-test load for four games: 20 solves × 4 games × 4 packs = 320 solves. Each extra game adds 80 solves at the same rule. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) H1
- Hashi generators in the wild explicitly note non-unique boards as a plot-twist. Witgrid would *reject* those. — [erthium/hashiwokakero](https://github.com/erthium/hashiwokakero)
- Slitherlink creation is typically “start from a legal loop and work backwards” using the same tricks as the solver. — [Game Developer: What game devs can learn from Nikoli](https://www.gamedeveloper.com/design/what-game-devs-can-learn-from-japan-s-most-interesting-puzzle-magazine)
- Guru Games: adding Killer was not “one extra if”; candidate generation, error highlighting, completion, hints, and creation all depended on the Classic three-group assumption. Mathdoku operators made valid combinations depend on operator, cell count, and ordering. — [Guru Games, Jul 2026](https://medium.com/@gurugames/what-building-six-sudoku-variants-taught-us-about-software-architecture-f98bf26b901b)

### Inferences

Card counts are in *Witgrid task-card units*, using the post-launch template. Calendar assumes one agent family per card, lanes parallel where the launch games were (x1→x2→x3, x4 after x2, board after x2, bank after x3). Review + `check` is inside each card, not extra.

**(a) Sudoku 9x9 — 7–9 cards, ~10–14 calendar days**

- Plan claim “reuses the 6x6 engine” is **not true of the current code**. Size, digits, boxes, note mask, keypad, and pack techniques are compile-time 6. A honest path is a new `sudoku` package copied from `minisudoku`, not a one-line constant change.
- Still the cheapest extra: same `Move`s, same `SudokuPlayActions`, same BoardCanvas (drag off), same generate-by-digging method (Mini Sudoku x3 was tier **M**, not H).
- Extra cost vs Mini Sudoku: 9-digit notes drawing, 3×3 box highlights, likely extra `x2` techniques so Expert 9x9 is guess-free. Interstitial rule is “each level” (long levels), not Mini Sudoku’s `2`. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §12; [app/.../minisudoku/MiniSudokuGameModule.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\minisudoku\MiniSudokuGameModule.kt)
- Suggested cards: SU1 (M), SU2 (H), SU3 (M), SU4 (L), board (M), intro (L), bank generate+validate (L), interstitial/config/store (L). If SU1 copies Mini Sudoku aggressively, SU1 can stay M. Do **not** count a “generalize Mini Sudoku” refactor; that would touch a done lane outside Owns.

**(b) Killer or Calcudoku — Killer 10–12 cards / ~3 weeks; Calcudoku 11–13 / ~3–4 weeks**

- Digit pad and BoardCanvas reuse Mini Sudoku. Cage drawing can borrow Crowns region fills / `RegionPalette`.
- `x1` must store cages (and for Calcudoku, operators). Bank payload is no longer “givens + solution”.
- `x2` is H+ : cage combination tables, 45-rule, unique cage digits, plus the four Mini Sudoku techniques. Guru Games: this is the wall that forced a constraint-group rewrite.
- `x3` is H: generate a valid 9x9 (or 6x6) then overlay cages that stay unique and technique-solvable.
- Killer first, not Calcudoku. Plan puts Killer in wave 3 and Calcudoku in wave 4 with “cheap library growth” as a *demand* note, not an engineering note. Calcudoku’s four operators are a second solver. Name: Calcudoku, never KenKen.

**(c) Another Nikoli grid — split band**

Cheaper band (**9–11 cards, 2–3 weeks**), still logic-grid / mistake-limit:

- **Futoshiki**: Latin square + inequality edges. Digit pad. Small sizes (4–7) keep `countSolutions` cheap. Draw `<` `>` on BoardCanvas cell borders.
- **Skyscrapers**: Latin square + outside clues. Digit pad. Outside-clue layout is already a Nonogram problem (clues beside the grid). BoardCanvas is square-aspect; Skyscrapers needs a margin for clues (Nonogram already does this in its own Board, not in the shared canvas).
- **Tents**: `SetMark` like Crowns; trees are givens. Drag-paint grass/X. Generator is closer to Crowns (place tents, grow/check uniqueness) so x3 is H.

Dearer band (**12–15 cards, 3–4 weeks**), often a core-kit change:

- **Kakuro**: mixed clue/playable cells, digit combinations per run. Input is Mini Sudoku-like on white cells, but drawing and `countSolutions` on irregular runs are new. Still `SetDigit`.
- **Hashi**: island graph + 0–2 bridges. New Move or a hidden edge encoding. BoardCanvas cell tap is the wrong hit target (tap is between islands).
- **Slitherlink**: loop-on-edges. New canvas hit-test. Technique set is large (0s/3s, corner, Jordan curve). x2 and x3 both H, plus a board-kit card outside the game Owns line (stop-and-ask the lead).
- **Numberlink**: path colouring. `Mark` has only three values; many pair-colours do not fit. Drag-paint helps. Unique-path generation is the expensive part (plan already parked Path in wave 3 because “the Path generator needs walls for a unique solution”).

Do not treat plan §12 “cheap library growth” for Bridges/Tents/Calcudoku as a card-count claim. That cell is about *demand*. Tents is cheap-ish; Bridges is not.

**(d) No-guess Minesweeper — 10–12 cards, ~3 weeks**

- Input can reuse Nonogram Fill/X + drag. Flood-open is extra `SetMark`s in one history record (same pattern as Crowns auto-X / Nonogram auto-cross).
- Plan: procedural + solver, 1–5 min, interstitial each level, second chance with a rewarded ad. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §12, §8.4
- Extra vs a logic grid: (1) fail-is-natural lives card; (2) no 600-line bank *or* a generated-on-PC bank of mine layouts that the technique solver can clear with no guess; (3) PlayController currently treats non-note edits as potential mistakes for the *optional* mistake limit — Minesweeper fail is stepping on a mine, a different policy.
- Unique no-guess generation is a known solver-backed generator, closer to Nonogram x3 (tier H) than Binary x3 (tier M).
- This is a wave-1 *product* pick, not a *cheap* pick.

**(e) Colour sort / arrows / block puzzle — 12–16 cards each, 3–4+ weeks**

- Plan explicitly lists them as lives games (extra tube / revive / undo). — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §8.4, §12
- Colour sort: not a grid. BoardCanvas unused. New tube widget, pour gesture, new state (stacks), new `Move` (pour from i to j).
- Arrows: “2026 breakout genre; line art fits the style.” Rotation is not in BoardGestures (tap, drag-paint, long-press, two-finger undo only).
- Block puzzle: 8×8 place shapes, clear lines, procedural endless, interstitial at game over, revive. Piece-drag onto a grid is a new gesture layer. No bank uniqueness in the logic-grid sense; still needs a fair RNG and a revive card.
- Plan “Do not add” list already excludes screw sort, hexa sort, tile match, mahjong, jigsaw, five-letter word guess, sliding block escape. Colour sort is in wave 2, not in that ban list. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §12
- These also need Remote Config interstitial keys at game-over, not `levelsPerInterstitial`.

**2–4 extras that are truly minimal effort (ordered):**

1. **Sudoku 9x9** — only extra that reuses Mini Sudoku end-to-end (input, moves, generate-by-digging, play controls).
2. **Futoshiki** — Latin square + edge marks; small boards; digit pad; no lives.
3. **Skyscrapers** — Latin square + outside clues; digit pad; Nonogram-like chrome.
4. **Tents** — `SetMark` + Crowns-like drag-X; still a logic grid with mistake limit.

Killer is the best *brand* extra but not minimal-effort. Do not put Hashi, Slitherlink, Numberlink, Minesweeper, colour sort, arrows, or block puzzle in a “cheap extra” set.

### Gaps

- No measured wall-clock for MS1–MS4 or A8 in TASKS.md, so “2 weeks” is inferred from card tiers and the plan’s 2–3 week wave cadence, not from a timesheet.
- Bankgen runtime for a fifth 965-puzzle set is not recorded here. B2/B5 notes say generation is slow and runs on the developer PC; CI runs only `validate`.
- Whether Futoshiki/Skyscrapers Expert packs can be graded with a small technique set (like Mini Sudoku’s four) is not specified in the plan.

---

## What would slip closed test if 2 extra games were added now vs after launch?

### Takeaway

Adding two extras *now* would push closed test by roughly **3–5 calendar weeks** plus a longer H5, even if the extras are the cheap pair (Sudoku 9x9 + Futoshiki). The remaining closed-test path is already human-gated (Play product, 14-day tester run). The plan already defers wave 1 until the 90-day income check. After launch, extras do not block production access and match the written 2–3 week cadence.

### Cited Findings

- Closed test is Phase 5, weeks 8–10. Exit: Google approves production access. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §11
- On 18 Nov 2026, remaining *code* cards: M6 blocked, M7 todo, S3 todo. M6 code/review is complete; it waits on H4’s Play product `witgrid_premium_lifetime` and a licence-tester buy/restore. M7 needs H4 Firebase. S3 still open for production-only ad IDs, M7 Firebase rules, H4 consent, and M6/M7 dependencies. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) M6, H4, M7, S3
- H5 requires 12+ testers for 14 continuous days, then production access. Tester bugs become new cards. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) H5; [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §11
- H1 (hand-test the launch bank, 320 solves) is still **ready**, not done. Extra games add 80 solves each before a fair closed test. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) H1
- U8 screenshot matrix is done for the *four* Play boards (144 screenshots, 14 store captures). U8 review is required before every closed-test upload. A fifth/sixth game means a new matrix, new store shots, new contact sheets. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) U8
- Closed-test exit check: crash-free sessions ≥ 99%; no puzzle with two solutions. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §3
- Wave 1 is scheduled in Phase 6 *after* launch: “Start wave 1 with the post-launch card template”. The 90-day review action is “Add wave 1 games”. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §11 Phase 6, §3, §12
- Fair-puzzle rule applies to every bank puzzle. A generator bug found in closed test is a production-access blocker, not a later patch. — [AGENTS.md](D:\cash-grabs\all-in-one games\AGENTS.md) §5; [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §3
- Launch lanes originally ran **in parallel** after E1 (CR/BI/NO/MS). That parallelism still needs four engine Owns trees plus shared `Registry` / `bankgen` grade `when` / screenshot script / store listing — all outside a single game’s Owns line, so the lead must extend cards. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md); [AGENTS.md](D:\cash-grabs\all-in-one games\AGENTS.md) §2
- S1 store drafts are done for the four-game listing. Adding games needs new names, screenshots, and a check of the name-ban list. No text may say how many games there are, so the listing does not need a “now 6 games” rewrite of the count, but it does need new mode descriptions. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) S1; [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §2
- Daily set grows with each game (one daily per game). Home daily chips iterate `state.games`. Calendar “today’s daily puzzles as one list” grows. — [app/.../ui/screens/home/HomeScreen.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\ui\screens\home\HomeScreen.kt); [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) U7

### Inferences

**If 2 extras are added now (best case: Sudoku 9x9 + Futoshiki):**

- Engine+board+bank: ~16–20 cards. Even fully parallel, the critical path is x1 → x2 (H) → x3 → bank generate → H1 extra 160 solves → U8 reshoot → store. That is not a weekend.
- Shared files that a game card is **not** allowed to edit without a lead exception: `engine/Registry.kt`, `bankgen/.../BankGame.kt` grade `when`, duplicated `gameNameResource` `when`s (Home, Play, Game, Calendar, Stats, StubScreens, Navigation), `GameBoardPreview.kt`, screenshot capture list, `strings.xml` name/rules, Remote Config key table, S1 listing.
- Closed test currently waits on **humans** (Play product, 14-day testers). Extra games do not unblock M6. They add surface area for H5 bugs and for the “no two-solution puzzle” exit check.
- Realistic slip: **miss the current Phase 5 window by 3–5 weeks**. If H5’s 14-day clock starts after the extras, a generator defect resets that clock.
- Lives extras (Minesweeper, block, sort) also reopen PlayController / LevelFailed policy (plan 8.4). That collides with done M5 and is a closed-test regression risk.

**If 2 extras wait until after launch:**

- Matches the written plan (wave 1 after 90-day income, or at least after H6 staged rollout).
- Closed test stays on 4 known boards, 3,864 already-validated puzzles, existing screenshot matrix.
- Each extra ships as its own store update (“a reason to post”), which is the point of §12.
- UI is already built to grow the list (see next question), so post-launch cost is almost entirely engine + board + bank.

**What specifically slips, item by item:**

| Closed-test item | 2 extras now | After launch |
|---|---|---|
| M6 device buy/restore | Unchanged (still H4) | Unchanged |
| M7 / S3 | Unchanged, but more strings/events/game IDs to wire | Wire per-game IDs in the wave card |
| H1 bank hand-test | +160 solves; H1 still open today | Separate hand-test per wave |
| U8 screenshots | Must redo 4-board matrix as 6-board; blocks H5 upload | New shots in the store-update card |
| H5 14-day testers | Starts later; more crash/puzzle surface | Testers already know the 4 games |
| Production access exit | Two-solution risk on new banks | New banks are a later update, not an access gate |

Recommendation from this study: **do not add extras before closed test.** The cheap pair is still ~two weeks of H-tier solver work plus bankgen plus a screenshot/store pass, and it does not shorten the H4/H5 wait.

### Gaps

- Exact remaining calendar to H5 given H0.1/H4 human work is unknown (those cards are still ready, not timed).
- Whether the lead would allow a one-card Owns extension covering `Registry` + `BankGame` + all `gameNameResource` `when`s is not written. Without that, each extra game is blocked on a lead glue card.

---

## Does Home / endless-level / “never state game count” make adding games cheap in UI?

### Takeaway

Yes for *shell* UI: Home, Game, stats, daily chips, and navigation already iterate `GameRegistry`. Endless levels and the “never say how many games” rule remove progress-bar and copy work. No, it is not free: name/rules `when`s, `GameBoardPreview`, bankgen grades, screenshot scripts, and daily-chip crowding still need per-game edits. The cheap part is UI chrome; the expensive part remains engine uniqueness + techniques.

### Cited Findings

- Owner decision 17 Nov 2026: no text states how many games there are, because more games will come. Screen layouts must work for any number of games. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §2, principle 8 at §4
- Endless levels: UI never shows a level total, “x of y”, a percent, or a progress bar. Level N plays bank puzzle `(N − 1) mod packSize`. — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §2; [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) U9
- Plan §7.3: “A new game needs no change to the home screen, the save system, the ad logic or the hint UI.” — [DEVELOPMENT_PLAN.md](D:\cash-grabs\all-in-one games\DEVELOPMENT_PLAN.md) §7.3
- `GameRegistry` is a list; `find(gameId)` is a map. Navigation passes `gameIds = GameRegistry.games.map { it.id }` into Home, Game, stats. Play/result load modules by id. — [app/.../games/GameRegistry.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\GameRegistry.kt); [app/.../nav/WitgridNavigation.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\nav\WitgridNavigation.kt)
- Home games are `state.games.chunked(2)` cards plus a daily chip per game. Unknown IDs fall through to `R.string.game_unknown`. — [app/.../ui/screens/home/HomeScreen.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\ui\screens\home\HomeScreen.kt)
- `gameNameResource` (and sibling rules/title helpers) is copy-pasted as a four-branch `when` in Home, Play, PlayScreenLayout, GameScreen, Calendar, Statistics, StubScreens, WitgridNavigation. — grep of `app/src/main` `when (gameId)`
- `GameBoardPreview` has an explicit `when (gameId)` motif and an `is MiniSudokuState -> 6` size. A new id draws the Mini Sudoku checkerboard fallback. — [app/.../ui/screens/home/GameBoardPreview.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\ui\screens\home\GameBoardPreview.kt)
- `EngineGameModule` already implements pack listing, intro-first-in-Easy, daily index 0..364, wrap-around `nextLevelRef`, and asset paths from `engine.id`. A new module that delegates to it inherits endless levels. — [app/.../games/GameModule.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\GameModule.kt)
- Mini Sudoku registration is ~20 lines: `EngineGameModule(engine, puzzleOf, intro, interstitialRule)` plus a `Board` override. — [app/.../games/minisudoku/MiniSudokuGameModule.kt](D:\cash-grabs\all-in-one games\app\src\main\kotlin\com\datathecodie\witgrid\games\minisudoku\MiniSudokuGameModule.kt)
- U6 done-when: “No level total appears on these screens.” Statistics may show a solved count, not “of total”. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) U6, U7
- Welcome (U2) has no game list and no game count. — [docs/TASKS.md](D:\cash-grabs\all-in-one games\docs\TASKS.md) U2
- PuzzlePack’s comparable trick: disable a mode in `modes_config.dart` and it leaves the home grid, with a test for the empty case. — [PuzzlePack](https://graphicfort.com/codecanyon/item/puzzlepack-8in1-flutter-puzzle-game-bundle/64717542)
- SGT Puzzles: new backends should not need to edit the middle end. — [SGT HACKING](https://www.chiark.greenend.org.uk/ucgi/~ian/git?p=sgt-puzzles.git;a=blob_plain;f=HACKING;hb=HEAD)

### Inferences

UI cost for a *logic-grid* extra that reuses an existing input:

- **Free by design:** Home grid growth, pack screen, play toolbar, hint sheet, result “Next”, ads, Premium, streak, calendar month grid, endless N, store copy that never says “4 games”.
- **Small glue (should be one lead L card, not four game cards):** `Registry` list, `GameRegistry` list, `bankgen` grade `when`, `strings.xml` name + one rules line + intro coach, six `gameNameResource` `when`s, `GameBoardPreview` branch, screenshot allow-list.
- **Not free:** the Board composable, accessibility labels, intro bank line, 965 generated puzzles, technique explanations.

Daily UI is the one shell that gets *worse* with more games: Home already lays one chip per game plus “Play daily {name}” for the first unfinished. Six to eight games will wrap the chip row. That is a layout concern, not a game-count string concern. The rule forbids “8 games”, not “8 chips”.

Lives / non-grid extras break the “no change to hint UI / fail UI” claim: colour sort and block puzzle do not use `nextStep` the same way; Minesweeper fail is not “three mistakes”. Those need Play/Failed work that logic grids skip.

Net: the Home/endless/no-count rules make extras **cheap in chrome** and **do not change the 8–10 card engine+board+bank bill**. That is why Sudoku 9x9 can be a 2-week wave after launch, and why adding it before closed test is still a slip.

### Gaps

- No tablet/large-font screenshot of Home with more than four game cards exists, so wrap/scroll behaviour at 6–8 games is unverified.
- Remote Config key `interstitial_levels_<game>` must be added per game (plan 8.6). Whether M7’s analytics event schema hard-codes the four IDs was not fully read (M7 is still todo).

---

## Bottom line for the report writer

**Minimal-effort extras (2–4):** Sudoku 9x9 (7–9 cards), Futoshiki (9–11), Skyscrapers (9–11), Tents (9–11). First two are the only ones worth calling “cheap”.

**Do not add before closed test.** Remaining gate is H4/M6/M7/S3/H5, not missing modes. Two extras now ≈ 3–5 weeks slip plus a heavier 14-day test.

**Do not add as “cheap”:** Killer/Calcudoku (solver wall), Hashi/Slitherlink/Numberlink (new geometry/Move), Minesweeper/block/sort/arrows (lives + new input). Those belong in the written post-launch waves.

**UI is not the bottleneck.** `GameEngine` + `EngineGameModule` + registry lists already match SGT-style backends. The bottleneck is guess-free unique generation (`x2`/`x3`) and, for non-grid games, a new input model on a sealed `Move` and a cell-only `BoardCanvas`.
