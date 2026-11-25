# Sudoku 9x9 bank

Generated with the deterministic Sudoku engine generator. The fixed seeds make each file reproducible. The validator was not run in this task; the lead will run `validate --all` in the batch gate.

## Packs

Compressed sizes are bytes. Grade histograms show the hardest technique recorded for each puzzle.

| File | Board | Count | Compressed bytes | Grade histogram | Givens range | Seed |
|---|---:|---:|---:|---|---:|---:|
| `sudoku-easy.txt.gz` | 9×9 | 150 | 13,037 | naked-single 150 | 23–29 | 250901 |
| `sudoku-medium.txt.gz` | 9×9 | 150 | 12,892 | hidden-single 150 | 21–27 | 250902 |
| `sudoku-hard.txt.gz` | 9×9 | 150 | 12,994 | locked-candidates 116, naked-pair 34 | 21–27 | 250903 |
| `sudoku-expert.txt.gz` | 9×9 | 150 | 12,995 | hidden-pair 116, naked-triple 20, x-wing 14 | 22–27 | 250904 |

## Daily

| File | Count | Compressed bytes | Pack distribution | Grade histogram | Seed |
|---|---:|---:|---|---|---:|
| `sudoku-daily.txt.gz` | 365 | 31,269 | Easy 92; Medium 91; Hard 91; Expert 91 | hidden-pair 80, hidden-single 91, locked-candidates 72, naked-pair 19, naked-single 92, naked-triple 7, x-wing 4 | 250905 |

Daily givens ranges by pack: Easy 24–29; Medium 22–29; Hard 22–27; Expert 21–27.

## Clue-pattern spread

Each pack has 150 distinct given-cell layouts out of 150 puzzles; the daily file has 365 out of 365. No given-cell layout repeats within a file. This compares only which cells are givens, independent of the digits in those cells.

## Registry

Sudoku is registered after Path, making it the eighth Family A engine available to bankgen.
