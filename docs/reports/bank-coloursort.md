# Colour Sort bank

**Status: complete.** All five Colour Sort files exist and `validate --all` passes (10,625 puzzles in 65 files).
Colour Sort is registered in `FamilyBRegistry` through `PlayEngineValidator` (`ColourSortEngine`, `ColourSortGenerator.configFor` difficulty, `ColourSortSolver.isWinnable`).

## Files

Seeds are fixed generation seeds. Sizes are compressed file sizes.

| File | Tubes | Count | Size | Difficulty histogram | Empty tubes | Colours | Distinct layouts | Seed |
|---|---|---:|---:|---|---|---|---:|---:|
| `coloursort-easy.txt.gz` | 4 | 150 | 2,676 bytes | `d1` 150 | 1 | 3 | 150 / 150 | 271011 |
| `coloursort-medium.txt.gz` | 6 | 150 | 3,288 bytes | `d2` 150 | 2 | 4 | 150 / 150 | 271012 |
| `coloursort-hard.txt.gz` | 8 | 150 | 4,156 bytes | `d3` 150 | 2 | 6 | 150 / 150 | 271013 |
| `coloursort-expert.txt.gz` | 10 | 150 | 5,093 bytes | `d4` 150 | 2 | 8 | 150 / 150 | 271014 |
| `coloursort-daily.txt.gz` | 4–10 (92/91/91/91) | 365 | 9,691 bytes | `d1` 92, `d2` 91, `d3` 91, `d4` 91 | Easy 1; others 2 | Easy 3; Medium 4; Hard 6; Expert 8 | 365 / 365 | 271015 |
| **Total** | 4–10 | **965** | **24,904 bytes** | `d1` 242, `d2` 241, `d3` 241, `d4` 241 | 1–2 | 3–8 | **965 / 965** | 271011–271015 |

## Layout spread and validation

- **Distinct layouts:** Every file has 100% distinct `size|payload|pack` layouts (`150/150` per pack, `365/365` daily, and `965/965` distinct layouts across all five files combined). Puzzle IDs are also unique (`965/965`).
- **Validation:** `./gradlew :bankgen:run --args="validate --all" --no-daemon` passed across all 65 bank files (10,625 puzzles), checking canonical five-field `PlayBankLine` encoding, pack/size/difficulty consistency, initial `!isLost && !isWon`, `ColourSortSolver.isWinnable`, unique puzzle IDs, and cross-file layout uniqueness.
- **Winnable, not unique:** Family B. Many pouring orders may win. The bank does not store a solution path.
- **Total compressed size:** 24,904 bytes (~0.024 MiB), well below the 1 MB per-game bank budget.

## Generation notes

- Packs 4 / 6 / 8 / 10 tubes match plan 12.2. Easy starts with one empty tube; Medium, Hard, and Expert start with two.
- Reverse-shuffle generation from a solved tube set, then `ColourSortSolver.isWinnable`.
- The generator attempt cap is 2000 (was 500) so Expert (10 tubes / 8 colours) does not fail a 150-puzzle run on a bad seed streak.
- `BankgenService` retries a layout when it collides with another puzzle in the same file or with an already-written Colour Sort file (Easy pack space is small enough that a 150+92 Easy split can overlap). Seeds `271011`–`271015` then produced 965 distinct layouts with no failed seeds.
