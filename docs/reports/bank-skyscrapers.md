# Skyscrapers bank

**Status: complete.** All five Skyscrapers files exist and `validate --all` passes (4,829 puzzles in 29 files).
The engine registry lists Skyscrapers after Mini Sudoku, so bankgen can generate and validate it.

## Files

Seeds are fixed generation seeds. Sizes are compressed file sizes.

| File | Board | Count | Size | Grade histogram | Seed |
|---|---|---:|---:|---|---:|
| `skyscrapers-easy.txt.gz` | 4×4 | 150 | 3,527 bytes | `height-bounds` 149, `naked-single` 1 | 230901 |
| `skyscrapers-medium.txt.gz` | 5×5 | 150 | 4,805 bytes | `height-bounds` 150 | 230902 |
| `skyscrapers-hard.txt.gz` | 6×6 | 150 | 6,098 bytes | `height-bounds` 150 | 230903 |
| `skyscrapers-expert.txt.gz` | 7×7 | 150 | 6,066 bytes | `height-bounds` 150 | 230904 |
| `skyscrapers-daily.txt.gz` | 4–7 (92/91/91/91) | 365 | 13,184 bytes | `height-bounds` 365 | 230910 |

## Generation notes

- Expert first failed with `Generator returned a duplicate puzzle`. When random grids miss the Expert band, the generator's last attempt used a fixed cyclic Latin square with only four variants. The fallback grid now gets a random row permutation, column shift, column reversal, transpose and symbol inversion. An engine test checks that 150 Expert puzzles from seed 230904 are distinct.
- Daily seeds 230905–230909 failed: either a duplicate inside the daily file, or a daily 4×4 puzzle equal to an Easy puzzle (validation rejects duplicates across files). The 4×4 puzzle space is small. Seed 230910 passes. If the Easy file is regenerated, the daily seed may need another search.

## Quality points for the lead

- **Patterned Expert solutions.** 84 of 150 Expert puzzles (56 %) and 83 daily puzzles have a solution in which every row is a cyclic shift of the first row. Random 7×7 grids rarely reach the Expert band, so the ordered fallback grid supplies most of them. Hard has 0 such grids, Medium 8.
- **Flat grades.** Almost every puzzle grades as `height-bounds`, the strongest technique the solver has. Packs differ only by board size, not by technique.
