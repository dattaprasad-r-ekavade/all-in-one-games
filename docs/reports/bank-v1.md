# Launch puzzle bank v1

Generated on 16 November 2026 from the four registered game engines. The bank has 150 levels in each of four packs and 365 mixed-pack daily puzzles per game: 3,860 puzzles across 20 gzip files.

## Generation results

| File | Seed | Puzzles | Compressed bytes | Generation time (s) |
|---|---:|---:|---:|---:|
| binary-daily.txt.gz | 20261266 | 365 | 18521 | 24.76 |
| binary-easy.txt.gz | 20261216 | 150 | 4449 | 3.76 |
| binary-expert.txt.gz | 20261219 | 150 | 10643 | 74.66 |
| binary-hard.txt.gz | 20261218 | 150 | 8216 | 5.56 |
| binary-medium.txt.gz | 20261217 | 150 | 6121 | 4.00 |
| crowns-daily.txt.gz | 20261166 | 365 | 15261 | 6.56 |
| crowns-easy.txt.gz | 20261116 | 150 | 4306 | 5.50 |
| crowns-expert.txt.gz | 20261119 | 150 | 8010 | 6.88 |
| crowns-hard.txt.gz | 20261118 | 150 | 6333 | 4.79 |
| crowns-medium.txt.gz | 20261117 | 150 | 5338 | 3.36 |
| minisudoku-daily.txt.gz | 20261466 | 365 | 15790 | 4.35 |
| minisudoku-easy.txt.gz | 20261416 | 150 | 6678 | 3.84 |
| minisudoku-expert.txt.gz | 20261419 | 150 | 6399 | 5.14 |
| minisudoku-hard.txt.gz | 20261418 | 150 | 6514 | 4.09 |
| minisudoku-medium.txt.gz | 20261417 | 150 | 6549 | 3.91 |
| nonogram-daily.txt.gz | 20261366 | 365 | 26601 | 46.19 |
| nonogram-easy.txt.gz | 20261316 | 150 | 4328 | 3.25 |
| nonogram-expert.txt.gz | 20261319 | 150 | 18337 | 27.09 |
| nonogram-hard.txt.gz | 20261318 | 150 | 9846 | 4.33 |
| nonogram-medium.txt.gz | 20261317 | 150 | 9905 | 6.09 |
| **Total** | - | **3860** | **198145** | **248.11** |

The total compressed bank is **198145 bytes (0.189 MiB)**. The uncompressed UTF-8 bank data is **733778 bytes (0.700 MiB)**, so both are below the 1 MB target. Generation times are end-to-end wall time for each Gradle CLI invocation, including Gradle startup; total measured time was 248.11 seconds (4.14 minutes).

## Validation and stats

- `.\gradlew.bat -q :bankgen:run --args="validate --all"` -> pass: 3,860 puzzles in 20 files.
- `.\gradlew.bat -q :bankgen:run --args="stats"` -> pass: histograms printed for all 20 files.
- Validation checked canonical bank lines, unique solutions, technique grades and pack bands, duplicate IDs, and duplicate puzzle content.

## Grade histograms

Each daily file contains 92 EASY puzzles and 91 each of MEDIUM, HARD and EXPERT.

Each histogram preserves the exact grade key in compact form: `technique (stepCount=puzzleCount, ...)`.

| File | Grade histogram |
|---|---|
| binary-daily.txt.gz | count-completion (21=4, 22=5, 23=9, 24=12, 25=29, 26=19, 27=10, 28=4, 38=1, 39=4, 40=4, 41=6, 42=16, 43=18, 44=11, 45=9, 46=12, 47=7, 48=2, 52=1, 61=1, 66=2, 67=1, 68=1, 69=3, 70=5, 71=4, 72=1, 76=1, 87=1, 90=1, 92=1, 93=1, 94=2, 95=2, 96=3, 97=5, 98=7, 99=2, 100=3, 101=3, 103=2); line-comparison (59=1, 61=2, 62=1, 63=4, 64=1, 65=7, 66=7, 67=7, 68=13, 69=4, 70=7, 71=6, 72=7, 73=3, 75=2, 90=2, 91=3, 93=4, 94=6, 95=4, 96=9, 97=4, 98=4, 99=5, 100=9, 101=2, 102=4, 103=1, 106=1) |
| binary-easy.txt.gz | count-completion (21=3, 22=12, 23=19, 24=31, 25=37, 26=28, 27=9, 28=8, 29=2, 30=1) |
| binary-expert.txt.gz | count-completion (90=2, 91=1, 92=3, 93=3, 94=4, 95=5, 96=8, 97=4, 98=5, 99=2, 100=3, 101=1, 102=3, 103=5, 104=2, 106=1, 107=1); line-comparison (87=1, 89=1, 90=2, 91=5, 92=4, 93=5, 94=9, 95=3, 96=10, 97=9, 98=13, 99=7, 100=4, 101=3, 102=6, 103=9, 104=3, 106=1, 107=1, 110=1) |
| binary-hard.txt.gz | count-completion (61=1, 63=2, 64=1, 65=2, 66=6, 67=1, 68=2, 69=5, 70=4, 71=4, 72=3, 74=1, 75=1); line-comparison (60=2, 61=2, 62=2, 63=5, 64=7, 65=3, 66=20, 67=15, 68=12, 69=14, 70=11, 71=14, 72=2, 73=6, 75=2) |
| binary-medium.txt.gz | count-completion (39=2, 40=5, 41=14, 42=23, 43=16, 44=27, 45=29, 46=19, 47=11, 48=3, 49=1) |
| crowns-daily.txt.gz | k-regions-k-lines (13=3, 14=4, 15=6, 16=6, 17=8, 18=6, 19=18, 20=5, 21=6, 22=2, 23=6, 24=4, 25=2); region-line (11=15, 12=39, 13=36, 14=20, 15=31, 16=29, 17=25, 18=20, 19=24, 20=19, 21=11, 22=10, 23=7, 24=3) |
| crowns-easy.txt.gz | k-regions-k-lines (12=1, 13=2, 14=5, 15=5); region-line (11=28, 12=66, 13=39, 14=4) |
| crowns-expert.txt.gz | k-regions-k-lines (20=1, 21=3, 22=3, 23=6, 24=3, 25=1, 26=1, 27=1); region-line (17=2, 18=8, 19=29, 20=28, 21=31, 22=22, 23=8, 24=3) |
| crowns-hard.txt.gz | k-regions-k-lines (17=3, 18=4, 19=8, 20=12, 21=5, 22=1); region-line (15=7, 16=20, 17=42, 18=31, 19=13, 20=4) |
| crowns-medium.txt.gz | k-regions-k-lines (15=2, 16=9, 17=6, 18=5, 19=1); region-line (13=10, 14=51, 15=44, 16=19, 17=3) |
| minisudoku-daily.txt.gz | hidden-single (20=2, 21=4, 22=10, 23=27, 24=26, 25=17, 26=5); locked-candidates (20=1, 21=5, 22=20, 23=43, 24=22); naked-pair (22=3, 23=10, 24=40, 25=25, 26=13); naked-single (8=69, 9=17, 10=5, 11=1) |
| minisudoku-easy.txt.gz | naked-single (8=101, 9=27, 10=19, 11=3) |
| minisudoku-expert.txt.gz | naked-pair (22=3, 23=19, 24=42, 25=56, 26=30) |
| minisudoku-hard.txt.gz | locked-candidates (20=2, 21=12, 22=34, 23=55, 24=47) |
| minisudoku-medium.txt.gz | hidden-single (17=1, 19=1, 20=4, 21=10, 22=26, 23=41, 24=38, 25=25, 26=4) |
| nonogram-daily.txt.gz | edge-rules (1=1); line-by-line (1=37, 2=145, 3=35, 4=46, 5=28, 6=35, 7=17, 8=9, 9=3, 10=5, 11=1, 12=2, 13=1) |
| nonogram-easy.txt.gz | line-by-line (1=53, 2=97) |
| nonogram-expert.txt.gz | line-by-line (4=36, 5=44, 6=18, 7=13, 8=16, 9=12, 10=2, 11=3, 13=3, 14=1, 15=2) |
| nonogram-hard.txt.gz | line-by-line (3=63, 4=53, 5=19, 6=7, 7=4, 9=1, 10=1, 11=2) |
| nonogram-medium.txt.gz | line-by-line (2=150) |

## Seeds

All seeds are fixed and listed per file in the generation table. The pack sequence is easy, medium, hard, expert; daily uses the separate seed shown in its row.
