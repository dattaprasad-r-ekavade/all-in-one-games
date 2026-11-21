# Generation difficulty of fair unique no-guess banks for Witgrid extra games

Scope: score each candidate on whether a **seeded generator + technique solver** can fill Witgrid’s bank (exactly one solution, no guess, integer math, SplitMix64, pre-generated gzip: 150×4 packs + 365 daily). Witgrid already generates Crowns, Binary, Nonogram and Mini Sudoku 6×6 that way ([DEVELOPMENT_PLAN.md](https://github.com)). Post-launch wave list is in the same plan, section 12.

Tatham’s editorial rule matches Witgrid: a fair puzzle is one a player can fail only through lack of skill; every instance must have a solution; hidden information must still allow a correct move from what is visible now. Unique solutions are “a little more negotiable” depending on the puzzle, but Solo (Sudoku) would be “unacceptably low quality” without uniqueness. ([Tatham writing.md, ch. 6.1](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))

Tatham’s generation methods (the taxonomy used below): mathematically tractable; inverse-move from solved; brute generate-and-test; generate-then-tweak; complete-then-punch clues; solver-in-the-loop while placing clues. Failures are allowed if the checker never false-positives. ([Tatham writing.md, ch. 6.2.1](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))

PuzzleScript is a *game* engine for action/sokoban-style rules, not a unique-logic-bank generator. No source in this pass treated it as a Nikoli-style unique generator. “qqqq” as a named generator family was not found (see Gaps).

---

## For each type: generation method, uniqueness cost, published techniques, no-guess status

### Takeaway
Latin-square family (Sudoku 9×9, Killer, Calcudoku, Futoshiki, Skyscrapers) and Tatham-style complete-then-punch grids (Akari, Hitori, Shikaku, Fill-a-Pix, Hashi) are the cheapest fair banks. Loop/path/shading NP-complete types (Slitherlink, Masyu, Nurikabe, Numberlink uniqueness) are medium-to-hard. Arcade/casual types (colour-sort, number-merge, 8×8 block-place, word search, Klondike) either are not unique or fail Tatham/Witgrid fairness.

### Cited Findings

#### Shared generation taxonomy (applies to many types)
- Tatham lists: (1) brute generate-and-test (Pattern/Nonogram, Dominosa); (2) generate-then-tweak local stuck spots (Mines, Net); (3) fill a complete solution then remove clues while a technique solver still succeeds (Solo, Slant) — this method “cannot fail” once a filled grid exists, except when a *minimum* difficulty is required, in which case Solo must throw the grid away; (4) run the solver *while* placing clues (Rectangles/Shikaku). ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- Dominosa brute force succeeds about 1 in 20 candidate grids after a generation-time tweak that “improved generation speed by over a factor of 10”; Rectangles fails “well under 50% of the time.” ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- A solver used as generator only emits puzzles the solver can handle; Tatham says this is usually fine because computers search exhaustively while humans prefer richer deductions, except Solo, where a simple solver produced “qualitatively easier” puzzles and the solver had to be made more advanced. ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- Witgrid Mini Sudoku already uses: make a full valid grid, remove givens while unique *and* the technique solver still solves; techniques are naked single, hidden single, locked candidates, naked pair. Two solvers check every bank puzzle: technique solver + brute-force solution counter. ([DEVELOPMENT_PLAN.md](https://github.com) sections 4 and 7)
- Simon Tatham’s Portable Puzzle Collection is MIT-licensed (copyright 2004–2024 Tatham and contributors; Android port copyright notice 2004–2024). ([chrisboyle/sgtpuzzles LICENCE](https://raw.githubusercontent.com/chrisboyle/sgtpuzzles/released/LICENCE); [Tatham collection page](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))

#### Sudoku 9×9 (reuse 6×6)
- Generation: complete a valid grid (backtrack or band/stack/relabel transforms), then punch clues; uniqueness via a counting solver that stops at the second solution. Difficulty from the hardest technique a logic solver needs. ([Final Sudoku generator article](https://finalsudoku.com/sudoku-generator))
- Tatham Solo: solver used both to check solubility while removing numbers and to Solve user-entered puzzles; modes of reasoning are enabled/disabled to set difficulty. Killer minmax elimination is already in the same solver. Generation “loop until we get a grid of the required difficulty… unpleasantly hard” for *minimum* difficulty, not for “at most” a level. 2×2 (4×4) puzzles are all Trivial. ([solo.c comments via git.tartarus.org](https://git.tartarus.org/?p=simon/puzzles.git;a=blob;f=solo.c;hb=HEAD))
- Published technique set is large and stable. HoDoKu SSTS (Simple Sudoku Technique Set): Singles, Locked Candidates, Hidden/Naked/Locked Subsets, X-Wing, Swordfish, Jellyfish, XY-Wing, Simple Colors, Multi Colors. ([HoDoKu creating sudokus](https://hodoku.sourceforge.net/en/docs_cre.php))
- Sudoku Explainer / HoDoKu ratings start: Lonely Single 1.0, Naked Single 1.1, Hidden Single 1.2, Point/Claim 2.1–2.2, pairs/triples, X-Wing (“Swampfish”) 3.5, then XY-Wing, fish, uniqueness, chains, forcing nets. ([Enjoy Sudoku forum extract of SE/HoDoKu ratings](http://forum.enjoysudoku.com/sudokuexplainer-running-on-hodoku-engine-t38271.html); [HoDoKu singles](https://hodoku.sourceforge.net/en/tech_singles.php))
- Unique Rectangle / BUG techniques *assume* uniqueness; they are not needed to *prove* uniqueness. ([HoDoKu uniqueness](https://hodoku.sourceforge.net/en/tech_ur.php))
- hodoku-py: Python port of HoDoKu 2.2.0 solver + generator with symmetric and pattern-based generation. ([alexdej/hodoku-py](https://github.com/alexdej/hodoku-py))
- No-guess is **standard** for published Sudoku: uniqueness is the product rule. Easy packs can stay at singles + locked candidates + pairs (Witgrid 6×6 already does this). Hard 9×9 without chains is still standard.

#### Killer Sudoku
- Rules: Sudoku + cages with sums; digits in a cage do not repeat. Unique combinations (e.g. 2-cell 4 = {1,3}; 3-cell 24 = {7,8,9}) plus the 45-rule (row/col/box sum 45) for innies/outies. ([Wikipedia Killer sudoku](https://en.wikipedia.org/wiki/Killer_sudoku); [Greg Puzzles Sudoku solver](https://gregpuzzles.com/pages/sudoku-solver))
- Tatham Solo already has killer blocks, `compute_kclues`, and killer minmax elimination in the same generator/solver loop as vanilla Solo. ([solo.c comments](https://git.tartarus.org/?p=simon/puzzles.git;a=blob;f=solo.c;hb=HEAD))
- Cage layout is extra work vs 9×9: generate a Latin/Sudoku solution, partition into cages, compute sums, then punch or thin cages until unique + technique-solvable.
- No-guess is **standard**. Integer-only: yes (sums of 1–9).
- Name: “Killer Sudoku” is used in the Witgrid plan; not on the forbidden-name list. ([DEVELOPMENT_PLAN.md](https://github.com) §12)

#### Calcudoku (do not call it KenKen)
- Latin square (no boxes) plus cages with +, −, ×, ÷. Numbers **may repeat inside a cage** (unlike Killer). − and ÷: start from the largest number. ([puzzlemix Calcudoku rules](https://www.puzzlemix.com/rules-calcudoku.php))
- Tatham name: **Keen**. MIT source in the collection. ([Tatham games list](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [chrisboyle/sgtpuzzles tree has keen.c](https://github.com/chrisboyle/sgtpuzzles/tree/d3290195da55beae04d1bb86f811b6f8dd6b0663))
- Division is integer in published Calcudoku/Keen (result must be an integer). Fits Witgrid integer-math rule if ÷ cages are restricted to exact integer quotients.
- Forbidden store word: **KenKen**. Plan already uses Calcudoku. ([DEVELOPMENT_PLAN.md](https://github.com) §12)
- No-guess is **standard**. Technique set: Latin singles/pairs plus cage combination tables (same family as Kakuro/Killer).

#### Futoshiki
- Solved Futoshiki is a Latin square; clues are < > between neighbours. ([sudoku-online-puzzles comparison](https://sudoku-online-puzzles.com/blog/games-like-sudoku/))
- Tatham name: **Unequal**. MIT. ([Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))
- Generation: fill Latin square, add all true inequalities, punch inequalities while unique + technique-solvable (complete-then-punch). Same family as Solo/Slant in Tatham’s taxonomy. ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- No-guess is **standard**. Small technique set: Latin singles, inequality forcing, naked/hidden pairs. Integer-only: yes.

#### Skyscrapers
- Latin square of heights; edge clues count visible towers. Author unknown; popularised at 1992 WPC. ([GMPuzzles via sudoku-online-puzzles](https://sudoku-online-puzzles.com/blog/games-like-sudoku/))
- Tatham name: **Towers**. MIT. ([Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))
- Generation: fill Latin square, compute 4n visibility clues, punch clues while unique. Visibility is integer comparisons, no floats.
- No-guess is **standard**. Techniques: Latin + “clue 1 ⇒ tallest at edge”, “clue n ⇒ 1..n in order”, pair reasoning.

#### Kakuro
- Crossword-shaped runs of 1–9, no repeat in a run, each run sums to its clue. Unwritten construction rule: each clue has at least two cells. NP-complete as integer programs. Harder puzzles use Sudoku-style chains. ([Wikipedia Kakuro](https://en.wikipedia.org/wiki/Kakuro))
- “Magic sums” (unique combination for a length+total, e.g. 2-in-17 = {8,9}, 3-in-6 = {1,2,3}) are the published beginner technique set, plus intersection of across/down sets and the “box” overlapping-sum trick. ([puzzlepageanswers Kakuro guide](https://puzzlepageanswers.org/guide/kakuro); Wikipedia)
- Generic constraint solver covering Sudoku, Killer, KenKen, Futoshiki, Kakuro exists (C#). Solver, not a uniqueness-graded generator. ([rich-newman solver](https://github.com/rich-newman/sudoku-killer-kenken-futoshiki-kakuro-solver))
- No-guess is **standard** for published Kakuro. Generation is harder than Sudoku because the *black-cell pattern* must be invented, then sums assigned, then uniqueness proved. Uniqueness check is a constrained permutation search; cheap on small runs, expensive on long intersecting runs.

#### Hashi / Bridges
- Islands with numbers 1–8; at most two bridges between a pair; no crossing; all islands one connected network. ([Greg Puzzles Hashi solver](https://gregpuzzles.com/pages/hashi-solver))
- Forced-bridge techniques are small and published: island with one neighbour must connect; island whose number = 2×neighbour-count must double-bridge all; two 1s cannot join unless they are the whole puzzle (connectivity). Greg’s solver “explains the next step” and “checks the solution is the only one.” ([Greg Puzzles](https://gregpuzzles.com/pages/hashi-solver); [miniwebtool Hashi generator copy](https://miniwebtool.com/hashi-bridges-puzzle-generator/))
- Tatham name: **Bridges**. MIT. ([Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))
- Generation pattern: place islands, assign a spanning tree/multigraph of bridges as solution, write island numbers, optionally remove islands or reduce until unique. Connectivity is global (like Tents), so uniqueness is more than local counting.
- No-guess is **standard**. Integer-only: yes.

#### Tents
- One tent orthogonally beside each tree; tents do not touch even diagonally; row and column counts. Tatham source comment: the puzzle “has many global deductions and doesn’t permit limited clue sets, generating grids for this puzzle is hard enough that I see no better option than to simply” brute-generate; then `tents_solve` at `params->diff` must succeed and at `diff-1` must fail; 4×4 cannot hold Hard (downgraded to Easy to prevent a tight loop). Tree count `w*h/5`. ([tents.c comments via git.tartarus.org](https://git.tartarus.org/?p=simon/puzzles.git;a=blob;f=tents.c;hb=HEAD))
- No-guess is **standard** in Tatham Tents (solver-gated). Uniqueness cost is high relative to local-clue puzzles because of global matching + non-adjacency.

#### Nurikabe
- Islands of given size, wall connected, no 2×2 wall. NP-complete (cited via SAT encoding). ([Groenen 2008 LIACS thesis](https://theses.liacs.nl/pdf/18-JohanGroenen.pdf))
- Exhaustive generation for m×n ≤ 25 (pivots 0–9): time grows exponentially with size; for 5×5, 935763 correct puzzles, **45.4% uniquely solvable**, black-cell share 53.1 ± 7.8%. “Total time to calculate all puzzles grows linearly with the total number of puzzles” once candidates exist — uniqueness check dominates. ([Groenen 2008](https://theses.liacs.nl/pdf/18-JohanGroenen.pdf))
- Terzer 2007: Nurikabe → propositional SAT. ([cited in Groenen](https://theses.liacs.nl/pdf/18-JohanGroenen.pdf))
- A JS generator/solver exists; author says generating past ~15×15 “will take a while” and calls it beta. ([kakuro-online Nurikabe](https://www.kakuro-online.com/nurikabe/))
- Tatham collection does **not** include Nurikabe (closest: Filling = Fillomino, Mosaic = Fill-a-Pix). ([Tatham game list](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))
- No-guess is **standard** for published Nikoli Nurikabe, but a small closed technique set that solves *all* unique puzzles without look-ahead is **not** documented in the sources fetched. Uniqueness at bank sizes (8×8–10×10) is a SAT/DLX-class cost.

#### Slitherlink
- Generate a loop, write edge-counts, punch clues while unique. Carleton comps: greedy loop, populate numbers, iteratively delete while unique and not too hard; uniqueness by solving then trying opposite guesses. ([Carleton slitherlink comps](https://cs.carleton.edu/cs_comps_archive/1516/slither/final-results/abstract.html))
- Slinker: create/solve by a ruleset, verify unique, decide whether a given ruleset solves with no recursion; “slitherlink is NP-complete: checking unique solutions gets very expensive with increasing size”; for a given size e.g. 7×7 unique puzzles are feasible; turning recursion off yields puzzles “directly solvable without having to follow alternatives.” ([timhutton/slinker README](https://github.com/timhutton/slinker/blob/main/README.txt))
- Liam Appelbe: loop → full clues → remove while unique; 10×10 “about 10 to 30 seconds”, 20×20 “about 3 to 5 minutes”, 30×30 “about 10 to 20 minutes” (author’s generator, not a paper). Recursive depth used as difficulty; max depth 10. ([Medium, Appelbe 2020](https://liamappelbe.medium.com/how-to-generate-slither-link-puzzles-6c65510b2ba1))
- davidjosepha generator reported runtimes: depth 0 (no guessing) 0.610963 s (1 of 9 puzzles); depth 1 174 s (3/9); depth 2 405 s (9/9). Proposed runtime O((mn)^(2d+1)). ([davidjosepha/slitherlink](https://github.com/davidjosepha/slitherlink))
- Tatham name: **Loopy**. MIT. Technique-limited generation is the Tatham/Slinker “no recursion” path. Hard packs that need bifurcation **fail Witgrid no-guess** unless rejected.
- No-guess is **standard for Easy/Medium published puzzles**, exceptional for “hard” packs that advertise trial-and-error. A published local technique set exists (corners, 0s, adjacent 3-3, vertex degree); Slinker can even invent more rules.

#### Masyu
- Single loop through black/white pearls. Same loop-generation family as Slitherlink. Tatham name: **Pearl**. MIT. ([Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))
- ZDD paper treats Masyu as a sibling of Numberlink/Slitherlink (paths/cycles with local+global constraints). ([MDPI Algorithms 2012](https://www.mdpi.com/1999-4893/5/2/176))
- No-guess is **standard** for Nikoli Masyu. Uniqueness cost similar to Slitherlink (loop counting). Pearl-local rules (white: go straight, turn on one side; black: turn, go straight on both sides) are a small published set.

#### Akari (Light Up)
- Place bulbs so every cell is lit; bulbs do not see each other; numbered blacks constrain orthogonally adjacent bulbs. Tatham name: **Light Up**. MIT. ([Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))
- Generation: place blacks + solution bulbs, write numbers, punch/thin until unique. Local counting (0/1/4 around a numbered wall) plus “this cell is the only way to light that square.”
- No-guess is **standard**. Integer-only: yes.

#### Hitori
- Shade duplicates so no number repeats in a row/col; shaded cells do not orthogonally touch; unshaded cells connected. Tatham name: **Singles**. MIT. ([Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))
- Generation: start from a Latin-ish filled grid (or a grid with controlled duplicates), shade a valid solution, punch? More naturally: fill numbers with extras, then the puzzle *is* the number grid; uniqueness is over shading. Complete-then-validate.
- No-guess is **standard**. Small technique set: pair shading, adjacency, connectivity last.

#### Numberlink (path filling)
- Classic Numberlink: connect equal numbers with non-crossing paths. Flow Free-style **relaxes uniqueness** and adds “fill every cell.” “Usual Numberlink solving techniques which require uniqueness of solution cannot be applied” to Flow Free. ([Any Puzzle Media Numberlink](https://anypuzzle.com/Numberlink))
- thomasahle/numberlink: generates and solves large instances (40×40 “casually”; 270 Janko puzzles solved in **0.14 seconds**). **Cannot check uniqueness**; assumes one solution, fill-all, no self-touch. Non-unique puzzles may be solved or reported IMPOSSIBLE. License: **AGPL-3.0** (copyleft; “If you generate puzzles using the scripts and post them in books or use them on your website, put clear reference”). ([thomasahle/numberlink](https://github.com/thomasahle/numberlink))
- SAT generation (Lelipaly 2025 LIACS): random endpoints, MiniSat solve; heuristics to speed generation. Thesis date 22 Jan 2025. ([Lelipaly thesis](https://theses.liacs.nl/pdf/2025-2026-LelipalyJJoshua.pdf); [LIACS record](https://theses.liacs.nl/3575))
- ZDDs enumerate *all* Numberlink/Slitherlink solutions and instances. ([MDPI 2012](https://www.mdpi.com/1999-4893/5/2/176))
- Witgrid plan: “Number link and Path”; “The Path generator needs walls for a unique solution.” Forbidden name: **Flow Free**. ([DEVELOPMENT_PLAN.md](https://github.com) §12)
- No-guess: **standard only if uniqueness is enforced**. Fill-all Numberlink is often unique in practice but Ahle explicitly says do not trust his solver as a uniqueness checker. Hint techniques that use uniqueness (as in classic Numberlink) need a real counter (ZDD/SAT/second-path search).

#### No-guess Minesweeper
- No-guess is **exceptional**, not the Windows default. Standard boards can reach 50/50s. ([minesweeper.now FAQ](https://minesweeper.now/faq/no-guessing-minesweeper); [Tatham mines-solver blog](https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/))
- Tatham Mines: no closed-form “forbidden configurations” that keep all soluble grids; that would leak generator knowledge. Method: human-style solver + **perturb uncovered region** when stuck; then re-solve from scratch to confirm no new blockage; last resort full regenerate. Solubility depends on the first click. At 16×16/40, random-and-test would work because “a large enough proportion of grids are soluble”; the perturber then allowed **over twice Windows mine density**. Interactive generation is delayed until first click. ([Tatham 2019 mines-solver](https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/))
- Tentaizu-style (all clues shown, unique mine placement): hellpig generator uses one-number logic, two-number logic, recursive brute force, SAT; puzzle-minesweeper.com easy = one-number, rest = two-number (author’s 2024 tests). ([hellpig/minesweeper-puzzle-generator](https://github.com/hellpig/minesweeper-puzzle-generator))
- jwang541: SAT on CNF of clues; generate random 16×30/99 until the SAT deduction solver finishes with no guess. ([jwang541/Minesweeper-Solver-SAT](https://github.com/jwang541/Minesweeper-Solver-SAT))
- JSMinesweeper (MIT, 177 stars): “No Guess” option generates on first click. Java solver 41% win on classic expert, 54.3% on modern expert (those are *play* win rates, not generation success). ([DavidNHill/JSMinesweeper](https://github.com/DavidNHill/JSMinesweeper))
- Two products: (A) **classic hidden mines**, first-click, Tatham perturber — uniqueness of remaining mines is *local at each step*, not a single static unique solution; (B) **pencil-puzzle Tentaizu** with all numbers visible — static unique solution, SAT/count. Witgrid “fair bank” matches (B) better; the plan’s “procedural + solver, 1 to 5 min” matches (A). ([DEVELOPMENT_PLAN.md](https://github.com) §12)

#### Fill-a-Pix
- Each clue is the count of painted cells in the 3×3 (or edge) neighbourhood. Tatham name: **Mosaic**. MIT. Recent Tatham work (Dec 2025): Mosaic game-description encoding fixes. ([git.tartarus.org summary](https://git.tartarus.org/?p=simon/puzzles.git;a=summary))
- Generation: paint a picture/binary grid, write all 0–9 clues, punch while unique. Local 0/9 forcing is trivial; uniqueness of the binary paint is a 2-SAT / counting problem of modest size.
- No-guess is **standard**. Integer-only: yes. Closest relative in Witgrid: Nonogram (already shipped).

#### Battleship logic (solitaire)
- Fleet + row/col counts + no-touch; generator “counts valid fleet placements” and only shows a puzzle with exactly one layout. ([logic-puzzles-online Battleship](https://logic-puzzles-online.com/battleship/))
- No-guess is **standard** for the *logic* product, exceptional for the two-player shooting game.
- Uniqueness cost: enumerate fleet placements consistent with counts (DLX/exact cover is the natural model). Cheap on 6×6, heavier on 10×10 with sparse givens.
- Integer-only: yes.

#### Shikaku
- Dissect the grid into rectangles, each containing one number equal to its area. Tatham name: **Rectangles**. MIT. Tatham: generate the rectangle partition first, then place numbers using a solver that runs *before* numbers are placed (solver copes with uncertainty); if it finishes, uniqueness is guaranteed with no extra check. ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- No-guess is **standard**. Integer area arithmetic only.

#### Arrows-escape
- Chaoschemy documents an “Arrow Escape” solver: tap order on a board of arrows up to 12×12; 12 free levels. ([Chaoschemy tools, updated 29 Oct 2026](https://chaoschemy.com/tools/))
- Witgrid plan: Wave 2, bank, 30–120 s, “2026 breakout genre.” ([DEVELOPMENT_PLAN.md](https://github.com) §12)
- No published Nikoli-style technique ladder was found. Uniqueness means: a unique *sequence of taps* (or unique set) that clears the board. Generation: place arrows, BFS/DFS the deterministic tap graph, reject if 0 or >1 clearing orders (or if order exists but needs guess among legal taps).
- No-guess can be **made standard** by rejecting boards with a choice that is not forced. Not a classic deduction-hint genre.

#### Colour-sort (water sort)
- Chaoschemy: “Water Sort Puzzle Solver — enter the visible colors… get every pour, step by step.” ([Chaoschemy tools](https://chaoschemy.com/tools/))
- Commercial apps advertise “500 levels plus infinite play,” not unique-solution banks. ([Puzzlette Play listing](https://play.google.com/store/apps/details?id=com.falanapp.puzzlette&hl=en_US))
- State space is a pouring graph. Many sequences solve the same start. “Unique solution” is the wrong object; “shortest pour sequence” or “solvable in ≤k pours with N tubes” is the usual generator target.
- No-guess is **not a standard product requirement**. Hints are “pour A onto B,” not a named technique. Integer-only: yes (discrete tubes).

#### Number-merge
- Plan: procedural endless, undo via rewarded ad. Forbidden name: **2048**. ([DEVELOPMENT_PLAN.md](https://github.com) §12)
- Not a unique-solution puzzle. No technique solver. RNG + merge physics. Does not fit the bank model (the plan already marks it procedural/endless).

#### 8×8 block-place
- Plan: procedural endless, interstitial at game over, revive ad. Forbidden name: **Block Blast**. ([DEVELOPMENT_PLAN.md](https://github.com) §12)
- Chaoschemy Block Blast solver: 8×8, 1–3 shapes, optional rotations — a *move advisor*, not a unique puzzle generator. ([Chaoschemy tools](https://chaoschemy.com/tools/))
- Not unique; not a bank; not no-guess in the Witgrid sense.

#### Klondike winnable deals
- Tatham **rejects Klondike as unfair**: hidden cards plus arbitrary choices; a better player could not have known; Undo does not repair fairness. ([Tatham writing.md §6.1](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- Thoughtful (all cards known) winnability, Solvitaire, JAIR 2026 (arXiv 1906.12314): Draw-1 **90.480% ± 0.116%**, Draw-3 **81.945% ± 0.084%**, unlimited redeals, worrying-back allowed. Without worrying-back: 90.204% and 81.524%. ([Blake & Gent JAIR 85, 2026](https://www.jair.org/index.php/jair/article/view/17167); [arXiv PDF](https://arxiv.org/pdf/1906.12314))
- Ordinary (hidden) win rates are lower and not a uniqueness number. Generation of “winnable deals” = deal random, run a solver, reject ~10% (draw-1 thoughtful) or ~18% (draw-3 thoughtful). That guarantees *a* win exists with full information, **not** that each move is forced, **not** a unique solution path, **not** hint-by-technique.
- Solvitaire: DFS + transposition tables, symmetry, dominances, streamliners. ([JAIR abstract](https://www.jair.org/index.php/jair/article/view/17167))

#### Word search
- Plan: procedural + word list, 90–240 s. ([DEVELOPMENT_PLAN.md](https://github.com) §12)
- Typical generators place words then fill noise letters. Many accidental extra words. Uniqueness is not a product rule. Needs a word list (language data, not integer grid logic). Hints are “the word is HERE,” not a technique.
- Fails uniqueness, fails “technique solver with no guess,” needs word lists.

### Inferences
- Anything Tatham already generates with a technique-limited solver is **in-family** with Witgrid’s engine: Solo/Keen/Unequal/Towers/Bridges/Light Up/Singles/Rectangles/Mosaic/Loopy/Pearl/Tents/Mines.
- Complete-then-punch is the default for clue-optional Latin and loop puzzles; brute generate-and-test is the default when every cell is a clue (Tents, Pattern, Dominosa).
- Uniqueness cost tracks constraint globality: Latin local < Hashi connectivity < Tents matching < Slitherlink loop < Nurikabe SAT < Numberlink path-count.
- “No-guess” is the published default for Nikoli-style types and the *exception* for Minesweeper, Flow-style Numberlink, Klondike, and arcade merge/sort/block games.
- Witgrid 9×9 Sudoku is the cheapest next bank: same generator as 6×6, bigger technique table optional for Hard/Expert.

### Gaps
- No measured rejection rate for Tatham Keen/Unequal/Towers/Bridges/Pearl/Light Up/Mosaic in the fetched docs (only Dominosa 1/20, Rectangles <50% fail, Solo min-difficulty loops, Tents brute + dual-diff check).
- Conceptis “about” pages and Nikoli generator internals were not fetched as primary method docs; commercial uniqueness claims without algorithms were not used as facts.
- “qqqq” generator family: no matching primary source found.
- PuzzleScript: no unique-logic-bank docs found; treated as out of scope.
- Arrows-escape: no paper on unique-order generation rates.
- Water-sort: no source giving the fraction of random tube fills that are solvable or unique-shortest-path.
- Kakuro black-pattern generation algorithm and typical times: not found in this pass.
- Fill-a-Pix uniqueness complexity class: not cited beyond Tatham Mosaic existing.

---

## Typical generation time and rejection rates

### Takeaway
Where numbers exist, small Latin/loop puzzles are seconds or less per accepted grid on a PC; Slitherlink uniqueness grows steeply with size and guess-depth; Nurikabe exhaustive uniqueness is exponential in cells; Numberlink *solving* is fast but uniqueness is a different, costlier problem. Many types have **no published rate** — those are marked below.

### Cited Findings
- Slitherlink (Appelbe, one generator): 10×10 ~10–30 s; 20×20 ~3–5 min; 30×30 ~10–20 min. Author generates the next puzzle in the background while the player solves. ([Medium 2020](https://liamappelbe.medium.com/how-to-generate-slither-link-puzzles-6c65510b2ba1))
- Slitherlink (davidjosepha, 9 mixed puzzles): depth 0 = 0.611 s (1 solved); depth 1 = 174 s (3); depth 2 = 405 s (9). Empty 3×3 depth 4 = 2587.52 s. ([davidjosepha/slitherlink](https://github.com/davidjosepha/slitherlink))
- Dominosa (Tatham): ~1/20 candidates usable after a tweak that sped generation >10×; generator “very fast so this doesn’t matter.” ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- Rectangles/Shikaku (Tatham): fail well under 50%; slower generator than Dominosa. ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- Pattern/Nonogram (Tatham): brute generate-and-test; probability of unique instances “decreases sharply as puzzle size goes up.” ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- Tatham Mines: at standard 16×16/40, random-and-test would work (large soluble fraction, no number given); perturber then supported >2× Windows density without “too long.” First-click dependent. ([Tatham mines-solver](https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/))
- Nurikabe 5×5 exhaustive: 935763 valid, 45.4% unique; generation of *all* puzzles exponential in m×n. ([Groenen 2008](https://theses.liacs.nl/pdf/18-JohanGroenen.pdf))
- Numberlink solve (Ahle): 270 Janko in 0.14 s; 40×40 generate/solve “casual.” Heights >20 in the Python generator “may take a while.” Not a uniqueness time. ([thomasahle/numberlink](https://github.com/thomasahle/numberlink))
- Klondike thoughtful reject rate implied by winnability: ~9.5% of Draw-1 deals and ~18.1% of Draw-3 deals are unwinnable even with full information (100% − 90.480% and 100% − 81.945%). ([Blake & Gent 2026](https://arxiv.org/pdf/1906.12314))
- HoDoKu Medium scores “normally between 600 and 1200 approximately” with Singles, Locked Candidates, Pairs, Triples — a rating fact, not a generation-time fact. ([HoDoKu docs_cre](https://hodoku.sourceforge.net/en/docs_cre.php))
- MiniWebtool Slitherlink difficulties (product copy, not a paper): Easy 5×5 ~85% clues, Medium 7×7 ~60%, Hard 10×10 ~42%, Expert 10×10 ~28%. Treat as one vendor’s clue-density knobs, not a uniqueness theorem. ([miniwebtool Slitherlink](https://miniwebtool.com/slitherlink-puzzle-generator/))

### Inferences
- A Witgrid PC bank of 600+365 puzzles is cheap for 9×9 Sudoku, Futoshiki, Skyscrapers, Calcudoku, Shikaku, Fill-a-Pix, Akari, Hitori at Easy–Hard technique caps.
- Slitherlink/Masyu at 10×10 with **no bifurcation** is feasible as a batch (seconds–tens of seconds each if Appelbe’s numbers transfer; Slinker’s no-recursion path is the Witgrid-shaped one).
- Nurikabe 10×10 unique no-guess should be budgeted as a SAT-in-the-loop batch, not a 6×6-style punch loop, unless a Tatham-like technique solver is written first and used as the only acceptor.
- Numberlink bank: generating a fill-all path layout is cheap; **proving uniqueness** is the budget line (second solver / ZDD / SAT count).
- Classic no-guess Minesweeper is not a static gzip bank of grids unless first-click is stored with the bank (Tatham encodes layout after click, or stores seed until click). Tentaizu *is* a static bank.

### Gaps
- No PC-side ms/puzzle figures for Sudoku 9×9 unique SSTS-only generation in the fetched sources (widely believed milliseconds; **not cited**).
- No rejection rates for Hashi, Tents (beyond “hard, brute”), Keen, Futoshiki, Skyscrapers, Kakuro, Battleship, Fill-a-Pix, Hitori, Akari.
- hellpig/jwang541 Minesweeper generators: no published accept-rate at Expert 16×30/99 no-guess.

---

## Open-source unique generators Witgrid could learn from (licenses)

### Takeaway
Tatham (MIT) is the single best study corpus for unique, fair, seeded, technique-limited generators. Sudoku Explainer / HoDoKu (and hodoku-py) are the technique encyclopaedia for 9×9/Killer. Copying Tatham C into Kotlin is allowed under MIT with copyright notice; copying Ahle Numberlink is AGPL and is a product-risk. Do not copy if the task card forbids new libraries — reading algorithms is not copying.

### Cited Findings
- **Simon Tatham Portable Puzzle Collection — MIT.** Seeded IDs; `new_game_desc(params, random_state)`; uniqueness/solubility inside the generator; 40 games including Solo (Sudoku+Killer), Keen (Calcudoku), Unequal (Futoshiki), Towers (Skyscrapers), Bridges, Tents, Loopy (Slitherlink), Pearl (Masyu), Light Up (Akari), Singles (Hitori), Rectangles (Shikaku), Mosaic (Fill-a-Pix), Mines, Pattern (Nonogram), Unruly (Binary). ([LICENCE](https://raw.githubusercontent.com/chrisboyle/sgtpuzzles/released/LICENCE); [writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html); [collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/))
- **chrisboyle/sgtpuzzles — MIT Android port**, 748 stars at fetch. Study the C, do not take the Android UI. ([GitHub](https://github.com/chrisboyle/sgtpuzzles/))
- **HoDoKu** technique docs + generator (SourceForge). hodoku-py: complete engine port, generator with symmetry/patterns. ([HoDoKu](https://hodoku.sourceforge.net/en/docs_cre.php); [hodoku-py](https://github.com/alexdej/hodoku-py))
- **Sudoku Explainer** ratings/technique order (forum-extracted from source). ([Enjoy Sudoku](http://forum.enjoysudoku.com/sudokuexplainer-running-on-hodoku-engine-t38271.html))
- **timhutton/slinker** — Slitherlink generate/solve/unique/no-recursion. ([slinker](https://github.com/timhutton/slinker))
- **ctbo/slitherlink** — Haskell solver+unique generator, BSD. ([ctbo/slitherlink](https://github.com/ctbo/slitherlink))
- **thomasahle/numberlink** — AGPL-3.0; fast solve/generate; **not** a uniqueness checker. ([repo](https://github.com/thomasahle/numberlink))
- **hellpig/minesweeper-puzzle-generator** — unique Tentaizu-like, SAT hybrid. ([repo](https://github.com/hellpig/minesweeper-puzzle-generator))
- **jwang541/Minesweeper-Solver-SAT** — no-guess board filter via SAT. ([repo](https://github.com/jwang541/Minesweeper-Solver-SAT))
- **DavidNHill/JSMinesweeper** — MIT; no-guess option. ([repo](https://github.com/DavidNHill/JSMinesweeper))
- **sam-gc/minesweeper-generator-solver** — GPL-2.0 no-guess. ([repo](https://github.com/sam-gc/minesweeper-generator-solver))
- **rich-newman sudoku-killer-kenken-futoshiki-kakuro-solver** — generic CSP solver, not a bank generator. ([repo](https://github.com/rich-newman/sudoku-killer-kenken-futoshiki-kakuro-solver))
- **Blake & Gent Solvitaire** — Klondike winnability; academic code referenced from JAIR/arXiv, not a Witgrid-shaped hint engine. ([JAIR](https://www.jair.org/index.php/jair/article/view/17167))
- **ZDD Numberlink/Slitherlink instance enumeration** — research code path for *all* unique instances. ([MDPI 2012](https://www.mdpi.com/1999-4893/5/2/176))

### Inferences
- Prefer MIT Tatham + HoDoKu docs as the study set. AGPL Numberlink and GPL minesweeper should be **algorithm inspiration only**, or avoided.
- latin.c in Tatham is shared infrastructure for Solo/Keen/Unequal/Towers — the same split Witgrid would want (Latin generator + overlay constraints).
- Witgrid must reimplement in Kotlin with SplitMix64; Tatham’s `random_state` is the analogue, not a drop-in.

### Gaps
- HoDoKu / Sudoku Explainer exact license strings were not fetched in this pass (SourceForge HoDoKu is commonly GPL — **unverified here**; do not copy until license is read).
- hellpig generator license not fetched.
- imos Numberlink uniqueness solver (linked from Ahle README) not inspected.
- qqqwing / qqwing Sudoku (possible “qqqq” typo) not fetched.

---

## Types that fail Witgrid fair / hints-explain / integer-only / name / bank rules

### Takeaway
Arcade and hidden-info games in the post-launch table are popular but are **not** fair unique no-guess banks. Several names are already forbidden. Integer math is rarely the blocker; uniqueness, hidden information, word lists, and “many winning lines” are.

### Cited Findings
- **Forbidden names** in app/store: Queens, Tango, Zip, LinkedIn, NYT, Takuzu, Binairo, Picross, KenKen, Tetris, Block Blast, 2048, Flow Free, Rush Hour, Wordle. ([DEVELOPMENT_PLAN.md](https://github.com) §12; [AGENTS.md](https://github.com))
- **Do not add:** screw sort, hexa sort, tile match, mahjong, jigsaw, five-letter word guess, sliding block escape. ([DEVELOPMENT_PLAN.md](https://github.com) §12)
- **Klondike:** Tatham fairness fail (hidden info + arbitrary choice). Winnable-deal filter ≠ unique no-guess. Hints are “move this card,” not a technique name with cells. ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html); [Blake & Gent](https://arxiv.org/pdf/1906.12314))
- **Word search:** word list required; uniqueness not standard; hints are locations not deductions. Plan already marks “procedural + word list.” ([DEVELOPMENT_PLAN.md](https://github.com))
- **Number-merge / 8×8 block-place:** plan marks procedural endless, ads at game over; forbidden 2048 / Block Blast names; Chaoschemy tools are move solvers, not unique banks. ([DEVELOPMENT_PLAN.md](https://github.com); [Chaoschemy](https://chaoschemy.com/tools/))
- **Colour-sort:** many pour sequences; solver exists as path search; commercial “infinite play.” Not a unique bank. ([Chaoschemy](https://chaoschemy.com/tools/); [Puzzlette listing](https://play.google.com/store/apps/details?id=com.falanapp.puzzlette&hl=en_US))
- **Flow-style Numberlink:** uniqueness relaxed. ([Any Puzzle Media](https://anypuzzle.com/Numberlink))
- **Classic Minesweeper:** no-guess is a filter on an otherwise unfair hidden-info game; Tatham still treats a labelled-unfair option as allowed. ([Tatham writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html); [mines-solver](https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/))
- **Calcudoku ÷:** published rules use integer quotients; floats are unnecessary if cages with non-integer ÷ are never emitted. ([puzzlemix](https://www.puzzlemix.com/rules-calcudoku.php))
- **Integer-only:** none of the Nikoli-style candidates require floats in the rules. Animation physics is a UI concern for colour-sort/block-place, not for Hashi/Tents/etc.
- **Lives-as-core:** mistake-limit is a Witgrid monetisation layer, not a generator property; Klondike/block-place/merge use fail-states as the core loop instead of a single solved grid.

### Inferences
- Fit the **bank + technique hint** product: Sudoku 9×9, Killer, Calcudoku, Futoshiki, Skyscrapers, Kakuro, Hashi, Tents, Akari, Hitori, Shikaku, Fill-a-Pix, Battleship logic, Masyu, Slitherlink (no bifurcation), Nurikabe (if SAT+techniques budgeted), Numberlink (if uniqueness counted), Tentaizu-Mines.
- Fit a **different** product (endless/procedural, ads at death): block-place, number-merge, colour-sort, Klondike, classic Mines, word search. Do not pretend these pass section-5 “fair puzzles.”
- Arrows-escape can be banked if uniqueness is defined as a forced tap order; it still lacks a public technique vocabulary for `nextHint()`.

### Gaps
- No source that colour-sort levels in hit apps are unique-shortest-path.
- No source that arrows-escape 2026 apps ship unique-order banks vs handmade levels.

---

## Difficulty of a seeded generator + technique solver (easy / medium / hard)

### Takeaway
**Easy:** Sudoku 9×9, Futoshiki, Skyscrapers, Calcudoku, Shikaku, Fill-a-Pix, Akari, Hitori. **Medium:** Killer Sudoku, Kakuro, Hashi, Tents, Battleship logic, Masyu, Slitherlink (technique-only), Tentaizu Mines, Numberlink-with-counter, Arrows-escape. **Hard:** Nurikabe, Slitherlink-with-deep-unique, classic no-guess Mines as a static bank, Numberlink uniqueness at large size without fill-all. **Wrong product (do not score as logic banks):** colour-sort, number-merge, 8×8 block-place, Klondike, word search.

### Cited Findings
- Witgrid 6×6 Mini Sudoku already implements complete-then-punch + technique cap + solution counter — the 9×9 path. ([DEVELOPMENT_PLAN.md](https://github.com))
- Tatham: complete-then-punch “cannot fail” for Solo/Slant-like clue-optional puzzles; brute-test scales poorly; Mines/Net tweak needs *local* failure modes; Tents has no limited clue set so brute is the option; Solo minimum-difficulty is the painful loop. ([writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html); [tents.c comments](https://git.tartarus.org/?p=simon/puzzles.git;a=blob;f=tents.c;hb=HEAD))
- Slitherlink unique + no-recursion is explicitly supported (Slinker); unique + deep guess is seconds to minutes. ([slinker README](https://github.com/timhutton/slinker/blob/main/README.txt); [Appelbe](https://liamappelbe.medium.com/how-to-generate-slither-link-puzzles-6c65510b2ba1))
- Nurikabe uniqueness ~half of valid 5×5, exponential in size. ([Groenen](https://theses.liacs.nl/pdf/18-JohanGroenen.pdf))
- Numberlink solvers that ignore uniqueness are fast; uniqueness is a separate tool. ([Ahle README](https://github.com/thomasahle/numberlink))
- Mines no-guess has no simple characterisation; Tatham perturber is a research-sized component. ([mines-solver](https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/))
- Klondike fails Tatham fairness even with a winnability oracle. ([writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))

### Inferences (scores)

Scores assume: Kotlin engine, SplitMix64, technique `nextHint`, brute/SAT counter for uniqueness, gzip bank of 965 puzzles, integer math, no new libraries.

| Candidate | Score | Why (short) |
|---|---|---|
| Sudoku 9×9 | **Easy** | Reuse 6×6 punch loop; HoDoKu/SE technique table; uniqueness DLX/count is standard. Hard packs need more techniques, not a new generator architecture. |
| Futoshiki | **Easy** | Latin + punch inequalities; Tatham Unequal; tiny technique set. |
| Skyscrapers | **Easy** | Latin + punch visibility; Tatham Towers; integer comparisons. |
| Calcudoku | **Easy–medium** | Latin + cages (Tatham Keen). Combination tables extra; integer ÷ only. Do not name KenKen. |
| Shikaku | **Easy** | Tatham Rectangles method places clues with the solver in the loop; uniqueness comes for free if it finishes. |
| Fill-a-Pix | **Easy** | Paint then punch; local 0–9 rules; Tatham Mosaic; sibling of shipped Nonogram. |
| Akari | **Easy** | Place walls/bulbs, number, punch; local lighting rules; Tatham Light Up. |
| Hitori | **Easy** | Number grid + shading uniqueness; Tatham Singles; small rule set. |
| Killer Sudoku | **Medium** | 9×9 + cage partition + 45-rule techniques. Tatham Solo already has killer. Cage generation is the extra design. |
| Kakuro | **Medium** | Magic-sum techniques are public; grid-shape generation + unique run intersections are more work than a Latin punch. |
| Hashi | **Medium** | Local forced bridges + global connectivity; Tatham Bridges to study. |
| Tents | **Medium** | Tatham: brute because clues cannot be thinned; dual-diff check; 4×4 cannot be Hard. Matching + no-diagonal-touch is global. |
| Battleship logic | **Medium** | Exact-cover uniqueness; small published placement techniques; no Tatham sibling. |
| Masyu | **Medium** | Loop gen + pearl rules; Tatham Pearl; uniqueness like a milder Slitherlink. |
| Slitherlink (no guess) | **Medium** | Loop + punch + *technique-only* acceptor (Slinker no-recursion). Stay at ≤10×10. |
| Tentaizu Mines (all clues shown) | **Medium** | Unique mine-placement SAT/count; one- and two-number techniques exist; static bank OK. |
| Numberlink (fill-all, unique) | **Medium** | Path layout cheap; must add a real uniqueness counter (not Ahle). Path-with-walls (plan) is the uniqueness hack. Do not name Flow Free. |
| Arrows-escape | **Medium** | Forced-order search is easy to write; no public technique vocabulary for explained hints; genre is new (2026). |
| Slitherlink (hard unique) | **Hard** | Uniqueness at depth ≥1 is slow (174 s class in one student solver); Witgrid forbids guess. |
| Nurikabe | **Hard** | NP-complete; 5×5 already 45% non-unique; no Tatham code; SAT + a human technique ladder both needed. |
| Classic no-guess Mines (hidden, first click) | **Hard** as a *static unique bank* | Solubility is start-cell-dependent; Tatham perturber is non-trivial; not one solution in the gzip sense unless first click is stored. Easier as on-device generate-at-click (plan says procedural). |
| Colour-sort | **Fails bank model** | Many solutions; hint = pour; still integer. Could bank *one* shortest path per seed but that is not Nikoli-fair uniqueness. |
| Number-merge | **Fails** | Endless, not unique; name 2048 banned. |
| 8×8 block-place | **Fails** | Endless; name Block Blast banned. |
| Klondike winnable | **Fails fair/hints** | ~82–90% thoughtful winnable ≠ unique no-guess; hidden cards. |
| Word search | **Fails** | Word list; not unique; no techniques. |

**Cheaper extra candidates not on the original list (if research cheapness/popularity matters):**
- **Dominosa** — Tatham brute, 1/20 accept, MIT, unique. Easy–medium. ([writing.md](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html))
- **Slant / Galaxies / Magnets / Range / Palisade / Tracks / Signpost** — already in Tatham MIT with unique generators; same engineering shape as Hashi/Tents. Popularity lower than Sudoku/Killer.
- **Star Battle** — Witgrid already ships Crowns (same family).
- **Unruly** — Witgrid already ships Binary.

### Gaps
- Scores are inferences from methods and the few published times, not from a Witgrid prototype run.
- Popularity vs cheapness trade-off (store demand) is out of this generation-methods scope.
- Exact Kotlin person-weeks per type were not in any source.

---

## Source index (primary, used above)

- Tatham generator methods: https://www.chiark.greenend.org.uk/~sgtatham/puzzles/devel/writing.html
- Tatham Mines no-guess: https://www.chiark.greenend.org.uk/~sgtatham/quasiblog/mines-solver/
- Tatham collection / MIT ports: https://www.chiark.greenend.org.uk/~sgtatham/puzzles/ ; https://github.com/chrisboyle/sgtpuzzles/ ; https://raw.githubusercontent.com/chrisboyle/sgtpuzzles/released/LICENCE
- Tatham solo.c / tents.c comments: https://git.tartarus.org/?p=simon/puzzles.git;a=blob;f=solo.c;hb=HEAD ; https://git.tartarus.org/?p=simon/puzzles.git;a=blob;f=tents.c;hb=HEAD
- HoDoKu: https://hodoku.sourceforge.net/en/docs_cre.php ; https://hodoku.sourceforge.net/en/tech_singles.php ; https://hodoku.sourceforge.net/en/tech_ur.php
- Sudoku Explainer ratings extract: http://forum.enjoysudoku.com/sudokuexplainer-running-on-hodoku-engine-t38271.html
- Slitherlink: https://github.com/timhutton/slinker ; https://liamappelbe.medium.com/how-to-generate-slither-link-puzzles-6c65510b2ba1 ; https://cs.carleton.edu/cs_comps_archive/1516/slither/final-results/abstract.html
- Nurikabe thesis: https://theses.liacs.nl/pdf/18-JohanGroenen.pdf
- Numberlink: https://github.com/thomasahle/numberlink ; https://anypuzzle.com/Numberlink ; https://www.mdpi.com/1999-4893/5/2/176
- Klondike: https://www.jair.org/index.php/jair/article/view/17167 ; https://arxiv.org/pdf/1906.12314
- Mines generators: https://github.com/hellpig/minesweeper-puzzle-generator ; https://github.com/DavidNHill/JSMinesweeper
- Hashi techniques: https://gregpuzzles.com/pages/hashi-solver
- Kakuro/Killer techniques: https://en.wikipedia.org/wiki/Kakuro ; https://en.wikipedia.org/wiki/Killer_sudoku
- Calcudoku rules: https://www.puzzlemix.com/rules-calcudoku.php
