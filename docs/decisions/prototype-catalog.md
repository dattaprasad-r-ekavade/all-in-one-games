# Prototype catalog: extra games

Status: owner direction, 18 Nov 2026. Development time is not a constraint. Aim: ship a prototype and keep adding modes.

Promoted into `DEVELOPMENT_PLAN.md` section 12 and `docs/TASKS.md` Phase 7 on 21 Nov 2026. Background research: [docs/reports/extra-launch-games.md](../reports/extra-launch-games.md).

## Decision

Keep the four launch games. Add six more as a growing hub, not as a 40-game library and not as Chess.com-plus-match-3.

Time is not the limit. Fit is. Unique no-guess grid banks stay on `GameEngine`. Cards, colour sort, arrows, and later block puzzle use a second contract.

## Current set

Crowns, Binary, Nonogram, Mini Sudoku 6×6.

## Add these six

| Slot | Build | Do not build |
|---|---|---|
| Sudoku | **Sudoku 9×9** | Another 6×6 pack |
| Colour sort | **Colour Sort** (tubes; extra tube as Continue) | Screw sort, hexa sort, Magic Sort clones |
| Card | **Klondike**, winnable deals only | 50-variant casino solitaire |
| Chess | **Chess tactics** (mate in 2, unique puzzle bank) | Full Chess.com (ratings, matchmaking, online engine) |
| Top game 1 | **Path** (Flow-style, unique fill) | The name Flow Free |
| Top game 2 | **Arrows** (line art) | Block Blast, Royal Match, Candy Crush, mahjong |

That is **10 modes**. Listing stays a puzzle hub: logic grids plus a few mass-market puzzles.

## Why those two chart games

Play Puzzle in November 2026 is led by Block Blast (~1.1B installs), then match-3, merge, and mahjong. Those need sprites, lives metas, and they break paper-and-ink. The name **Block Blast** is banned in this repo (`AGENTS.md` rule 5.4).

The two chart games that still fit this app:

- **Path** — Flow Free is about 226M lifetime installs and about 6.7M monthly players.
- **Arrows** — Lessmore *Arrows – Puzzle Escape* is 100M+; Arrows GO! is about 133M. Line art matches the current boards.

**Colour Sort** is already a top Puzzle title (Water Sort about 133M). Keep one sort game. Do not add a second.

A generic **block puzzle** (8×8, place shapes, clear lines) can come later if we want that chart. Treat it as a third family, not as a Block Blast clone.

## Engine split

**Family A — unique no-guess grids** (`GameEngine` as today)

- The four launch games
- Sudoku 9×9
- Chess tactics
- Path, with a real uniqueness check

**Family B — many solutions or failure-as-play** (new contract)

- Colour Sort: pours, extra tube
- Klondike: deal, tableau, foundations
- Arrows: slide / rotate
- Later block puzzle: piece drag, revive

Do not force unique-no-guess on Family B.

`Move` today is only `SetMark`, `SetDigit`, and `SetNotes` on a cell. Tube pours, card drags, and arrow slides need new moves or the new contract. Adding a fourth `Move` variant would touch every existing engine `when`. Prefer a second interface.

Chess tactics can stay on cell picks (from-square and to-square as cells). Full chess needs an engine library, clocks, and a backend if online. That fights offline-first. Do not add it.

## Store pitch

List the app as a **deep puzzle hub**. Do not say “all-in-one.” Do not say a game count (plan 4, 17 Nov 2026).

Lead with Sudoku, Nonogram, Path, Colour Sort. Crowns and Binary stay as daily-habit extras. Chess tactics and Klondike sit in the list. They do not go in the title.

If we later add match-3 or mahjong, the title “Witgrid: Offline Logic Puzzles” becomes false. Change the title then, or do not add those genres.

Banned name words still apply in code, resources, and store text: Queens, Tango, Zip, LinkedIn, NYT, Takuzu, Binairo, Picross, KenKen, Tetris, Block Blast, 2048, Flow Free, Rush Hour, Wordle.

## Build order

Dependencies, not calendar:

1. **Sudoku 9×9** — copy Mini Sudoku; size 9; 3×3 boxes. Size is hardcoded to 6 today, so this is a new lane, not a one-line change.
2. **Colour Sort** — new UI; proves Family B.
3. **Path** — high demand; still a grid.
4. **Arrows** — high demand; still line art.
5. **Klondike** — winnable-deal generator; code-drawn cards.
6. **Chess tactics** — puzzle bank, not a chess server.

## Lives

Plan 8.4 still holds.

- Family A: optional mistake limit, off by default.
- Family B: failure is part of the game. Colour Sort Continue = extra tube. Arrows / later block puzzle = revive. Restart is always free and never shows an ad.

## Not in this catalog

- Match-3 (Royal Match, Candy Crush, Toon Blast)
- Mahjong and tile match
- Full online chess
- Screw sort, hexa sort, jigsaw, five-letter word guess, sliding-block escape
- A 40-game Simon Tatham museum as the launch pitch

Chess.com-plus-Royal-Match-plus-the-current-four is not one app. This six-game add is.
