# Launch puzzle bank v2

Regenerated on 16 November 2026 with the same seeds and counts as v1 (`docs/reports/bank-v1.md`). The bank has 150 levels in each of four packs and 365 mixed-pack daily puzzles per game: 3,860 puzzles across 20 gzip files.

## Generation results

| File | Seed | Puzzles | Compressed bytes | v1 bytes | Generation time (s) |
|---|---:|---:|---:|---:|---:|
| binary-daily.txt.gz | 20261266 | 365 | 18506 | 18521 | n/r |
| binary-easy.txt.gz | 20261216 | 150 | 4440 | 4449 | n/r |
| binary-expert.txt.gz | 20261219 | 150 | 10624 | 10643 | n/r |
| binary-hard.txt.gz | 20261218 | 150 | 8188 | 8216 | n/r |
| binary-medium.txt.gz | 20261217 | 150 | 6114 | 6121 | n/r |
| crowns-daily.txt.gz | 20261166 | 365 | 15805 | 15261 | n/r |
| crowns-easy.txt.gz | 20261116 | 150 | 4304 | 4306 | n/r |
| crowns-expert.txt.gz | 20261119 | 150 | 8575 | 8010 | n/r |
| crowns-hard.txt.gz | 20261118 | 150 | 6514 | 6333 | n/r |
| crowns-medium.txt.gz | 20261117 | 150 | 5500 | 5338 | n/r |
| minisudoku-daily.txt.gz | 20261466 | 365 | 15802 | 15790 | n/r |
| minisudoku-easy.txt.gz | 20261416 | 150 | 6888 | 6678 | n/r |
| minisudoku-expert.txt.gz | 20261419 | 150 | 6390 | 6399 | n/r |
| minisudoku-hard.txt.gz | 20261418 | 150 | 6513 | 6514 | n/r |
| minisudoku-medium.txt.gz | 20261417 | 150 | 6549 | 6549 | n/r |
| nonogram-daily.txt.gz | 20261366 | 365 | 26614 | 26601 | n/r |
| nonogram-easy.txt.gz | 20261316 | 150 | 4328 | 4328 | n/r |
| nonogram-expert.txt.gz | 20261319 | 150 | 18356 | 18337 | n/r |
| nonogram-hard.txt.gz | 20261318 | 150 | 9844 | 9846 | n/r |
| nonogram-medium.txt.gz | 20261317 | 150 | 9893 | 9905 | n/r |
| **Total** | - | **3860** | **199747** | **198145** | - |

The total compressed bank is **199747 bytes (0.190 MiB)**, up from 198145 bytes in v1. Both are below the 1 MB target.

Generation time is `n/r` (not recorded) where the log has no single clean record for that run. The run log was interleaved after a script restart, so these values are not reliable. Sizes and counts are exact.

## Validation and stats

- `./gradlew :bankgen:run --args="validate --all"` -> pass: 3,860 puzzles in 20 files.
- `./gradlew :bankgen:run --args="stats"` -> pass: histograms printed for all 20 files.
- ID check: every ID matches `XX-PACK-` followed by 16 lowercase hex digits. No ID contains `--`.

## Grade histograms

Each histogram uses `technique (stepCount=puzzleCount, ...)`. Each daily file contains 365 puzzles.

| File | Grade histogram |
|---|---|
| binary-daily.txt.gz | count-completion (21=4, 22=5, 23=9, 24=12, 25=29, 26=19, 27=10, 28=4, 38=1, 39=4, 40=4, 41=6, 42=16, 43=18, 44=11, 45=9, 46=12, 47=7, 48=2, 52=1, 61=1, 66=2, 67=1, 68=1, 69=3, 70=5, 71=4, 72=1, 76=1, 87=1, 90=1, 92=1, 93=1, 94=2, 95=2, 96=3, 97=5, 98=7, 99=2, 100=3, 101=3, 103=2); line-comparison (59=1, 61=2, 62=1, 63=4, 64=1, 65=7, 66=7, 67=7, 68=13, 69=4, 70=7, 71=6, 72=7, 73=3, 75=2, 90=2, 91=3, 93=4, 94=6, 95=4, 96=9, 97=4, 98=4, 99=5, 100=9, 101=2, 102=4, 103=1, 106=1) |
| binary-easy.txt.gz | count-completion (21=3, 22=12, 23=19, 24=31, 25=37, 26=28, 27=9, 28=8, 29=2, 30=1) |
| binary-expert.txt.gz | count-completion (90=2, 91=1, 92=3, 93=3, 94=4, 95=5, 96=8, 97=4, 98=5, 99=2, 100=3, 101=1, 102=3, 103=5, 104=2, 106=1, 107=1); line-comparison (87=1, 89=1, 90=2, 91=5, 92=4, 93=5, 94=9, 95=3, 96=10, 97=9, 98=13, 99=7, 100=4, 101=3, 102=6, 103=9, 104=3, 106=1, 107=1, 110=1) |
| binary-hard.txt.gz | count-completion (61=1, 63=2, 64=1, 65=2, 66=6, 67=1, 68=2, 69=5, 70=4, 71=4, 72=3, 74=1, 75=1); line-comparison (60=2, 61=2, 62=2, 63=5, 64=7, 65=3, 66=20, 67=15, 68=12, 69=14, 70=11, 71=14, 72=2, 73=6, 75=2) |
| binary-medium.txt.gz | count-completion (39=2, 40=5, 41=14, 42=23, 43=16, 44=27, 45=29, 46=19, 47=11, 48=3, 49=1) |
| crowns-daily.txt.gz | k-regions-k-lines (14=7, 15=13, 16=15, 17=27, 18=24, 19=26, 20=23, 21=16, 22=14, 23=25, 24=12, 25=12, 26=2, 27=2, 28=2); region-line (12=30, 13=33, 14=29, 15=16, 16=17, 17=7, 18=3, 19=3, 21=1, 22=3, 23=2, 24=1) |
| crowns-easy.txt.gz | region-line (12=54, 13=65, 14=23, 15=8) |
| crowns-expert.txt.gz | k-regions-k-lines (19=5, 20=8, 21=12, 22=21, 23=19, 24=21, 25=20, 26=19, 27=9, 28=3, 29=1, 30=1); region-line (19=2, 20=4, 21=2, 23=2, 24=1) |
| crowns-hard.txt.gz | k-regions-k-lines (17=10, 18=29, 19=46, 20=25, 21=14, 22=3); region-line (16=3, 17=14, 18=4, 19=2) |
| crowns-medium.txt.gz | k-regions-k-lines (13=2, 14=11, 15=25, 16=29, 17=26, 18=11, 19=3); region-line (14=12, 15=16, 16=13, 17=2) |
| minisudoku-daily.txt.gz | hidden-single (20=2, 21=4, 22=10, 23=27, 24=26, 25=17, 26=5); locked-candidates (20=1, 21=5, 22=20, 23=43, 24=22); naked-pair (22=3, 23=10, 24=40, 25=25, 26=13); naked-single (14=15, 15=25, 16=24, 17=17, 18=11) |
| minisudoku-easy.txt.gz | naked-single (14=31, 15=46, 16=20, 17=27, 18=26) |
| minisudoku-expert.txt.gz | naked-pair (22=3, 23=19, 24=42, 25=56, 26=30) |
| minisudoku-hard.txt.gz | locked-candidates (20=2, 21=12, 22=34, 23=55, 24=47) |
| minisudoku-medium.txt.gz | hidden-single (17=1, 19=1, 20=4, 21=10, 22=26, 23=41, 24=38, 25=25, 26=4) |
| nonogram-daily.txt.gz | edge-rules (1=1); line-by-line (1=37, 2=145, 3=35, 4=46, 5=28, 6=35, 7=17, 8=9, 9=3, 10=5, 11=1, 12=2, 13=1) |
| nonogram-easy.txt.gz | line-by-line (1=53, 2=97) |
| nonogram-expert.txt.gz | line-by-line (4=36, 5=44, 6=18, 7=13, 8=16, 9=12, 10=2, 11=3, 13=3, 14=1, 15=2) |
| nonogram-hard.txt.gz | line-by-line (3=63, 4=53, 5=19, 6=7, 7=4, 9=1, 10=1, 11=2) |
| nonogram-medium.txt.gz | line-by-line (2=150) |

## Crowns region sizes

Region size is the number of cells in one region. Computed from the bank files.

| Pack | Min | Max | Average | Regions |
|---|---:|---:|---:|---:|
| Easy | 3 | 15 | 6.00 | 900 |
| Medium | 3 | 17 | 7.00 | 1050 |
| Hard | 3 | 20 | 8.00 | 1200 |
| Expert | 3 | 25 | 9.56 | 1430 |
| Daily | 3 | 25 | 7.86 | 2784 |

## Mini Sudoku givens per pack

| Pack | Average givens | Min | Max |
|---|---:|---:|---:|
| Easy | 20.19 | 18 | 22 |
| Medium | 12.79 | 10 | 19 |
| Hard | 13.11 | 12 | 16 |
| Expert | 11.39 | 10 | 14 |
| Daily | 14.35 | 10 | 22 |

## Changes from v1

- CR5: Crowns regions have 3 to size*5/2 cells. Each pack has its own technique cap.
- MS5: Mini Sudoku Easy puzzles have 18 to 22 givens.
- B4, CR5, MS5: puzzle IDs use unsigned 16-hex suffixes. No ID contains `--`.
- File sizes changed by small amounts because the generators changed. Total compressed size rose by 1,602 bytes.
- Seeds and counts are unchanged from v1.
