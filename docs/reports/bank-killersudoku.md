# Killer Sudoku bank

**Status: complete.** All five Killer Sudoku files exist and `validate --all` passes (8,693 puzzles in 53 files).
`KillerSudokuEngine` is registered in `Registry.engines` after `SudokuEngine`, so `bankgen` can generate and validate it.

## Files

Seeds are fixed generation seeds. Sizes are compressed file sizes in bytes. Grade histograms count the hardest technique recorded for each puzzle.

| File | Board | Count | Compressed bytes | Grade histogram | Givens range | Seed |
|---|---|---:|---:|---|---:|---:|
| `killersudoku-easy.txt.gz` | 9×9 | 150 | 27,552 | `cage-combinations` 150 | 0 | 260901 |
| `killersudoku-medium.txt.gz` | 9×9 | 150 | 24,764 | `rule-45` 150 | 3–10 (avg 6.86) | 260902 |
| `killersudoku-hard.txt.gz` | 9×9 | 150 | 24,746 | `locked-candidates` 119, `naked-pair` 31 | 3–12 (avg 6.49) | 260903 |
| `killersudoku-expert.txt.gz` | 9×9 | 150 | 24,369 | `innie-outie` 146, `hidden-pair` 2, `naked-triple` 2 | 1–9 (avg 4.66) | 260904 |
| `killersudoku-daily.txt.gz` | 9×9 | 365 | 62,339 | `cage-combinations` 92, `rule-45` 91, `locked-candidates` 81, `naked-pair` 10, `innie-outie` 88, `hidden-pair` 3 | 0–12 | 260905 |

Total Killer Sudoku bank size: 163,770 bytes across 965 puzzles (5 files).

Daily pack distribution: Easy 92, Medium 91, Hard 91, Expert 91.
Daily givens ranges by pack: Easy 0 (avg 0.00); Medium 3–12 (avg 7.01); Hard 3–10 (avg 6.52); Expert 1–10 (avg 4.75).

## Spread

- `killersudoku-easy.txt.gz`: 150 distinct cage layouts and 150 distinct solutions out of 150 puzzles.
- `killersudoku-medium.txt.gz`: 150 distinct cage layouts and 150 distinct solutions out of 150 puzzles.
- `killersudoku-hard.txt.gz`: 150 distinct cage layouts and 150 distinct solutions out of 150 puzzles.
- `killersudoku-expert.txt.gz`: 150 distinct cage layouts and 150 distinct solutions out of 150 puzzles.
- `killersudoku-daily.txt.gz`: 365 distinct cage layouts and 365 distinct solutions out of 365 puzzles.

No cage layout or solution repeats within a file, and `validate --all` confirms no puzzle repeats across files.

## Generation notes

- All five seeds (`260901`–`260905`) succeeded on the first run with no duplicate failures (~48–81 seconds per file).
- Easy keeps 0 givens and peels cages until `cage-combinations` solves the board; Medium, Hard, and Expert retain a minimal set of givens (1–12) needed to land in each pack's technique band.
