# Mosaic bank

**Status: complete.** All five Mosaic files exist. `MosaicEngine` is registered in `Registry.engines` after `KillerSudokuEngine`, so bankgen can generate and validate it.

## Files

Seeds are fixed generation seeds where recorded. Sizes are compressed file sizes. Grade histograms count the hardest technique (every Mosaic puzzle in this bank is `clue-overlap`).

| File | Board | Count | Size | Grade histogram | Clue count | Seed |
|---|---|---:|---:|---|---|---:|
| `mosaic-easy.txt.gz` | 8×8 | 150 | 8,160 bytes | `clue-overlap` 150 | 21–37 (avg 28.3) | not recorded |
| `mosaic-medium.txt.gz` | 10×10 | 150 | 11,405 bytes | `clue-overlap` 150 | 34–60 (avg 44.7) | not recorded |
| `mosaic-hard.txt.gz` | 12×12 | 150 | 15,260 bytes | `clue-overlap` 150 | 52–79 (avg 64.5) | not recorded |
| `mosaic-expert.txt.gz` | 15×15 | 150 | 22,191 bytes | `clue-overlap` 150 | 87–116 (avg 99.7) | 251204 |
| `mosaic-daily.txt.gz` | 8–15 (92/91/91/91) | 365 | 34,918 bytes | `clue-overlap` 365 | 23–117 (avg 59.1) | 251205 |

Total Mosaic bank size: 91,934 bytes across 965 puzzles (5 files).

## Daily

| Seed | Count | Compressed bytes | Size / pack counts | Clue count by pack |
|---:|---:|---:|---|---|
| 251205 | 365 | 34,918 | 8×8: 92 Easy; 10×10: 91 Medium; 12×12: 91 Hard; 15×15: 91 Expert | Easy 23–36; Medium 34–56; Hard 55–74; Expert 85–117 |

## Generation notes

- Easy, Medium and Hard were generated on 23 Nov 2026 in the paused MOB run. Those three files still validate on current `main`. Their generation seeds were not recorded; do not regenerate them unless a later card needs a known seed.
- Expert seed `251204` wrote 150 puzzles on the first run (`BUILD SUCCESSFUL in 43m 21s`). Parallel generate of 150 Expert boards took about 10 minutes on 8 workers; the rest of the time is sequential `validateLine` (uniqueness + technique grade) against the existing Easy/Medium/Hard files.
- Daily seed `251205` wrote 365 puzzles on the first run (`BUILD SUCCESSFUL in 40m 16s`). No cross-file duplicate with the four pack files.
- Plan 12.10's "50 s per Expert puzzle / ~2 h pack" is a single-thread estimate. This PC generates the Expert pack much faster when bankgen uses up to 8 workers.

## Quality points for the lead

- **Flat hardest technique.** All 965 puzzles grade as `clue-overlap`, the strongest Mosaic technique. Packs differ by board size and step count (Easy steps 21–37, Medium 34–59, Hard 52–78, Expert 87–116), not by which technique is required. Easy's band only needs 10+ steps, so the generator never lands in `empty-area` / `full-area` / `remaining-count` as the hardest step.
- **Step counts sit above pack floors.** Easy ≥ 10, Medium ≥ 25, Hard ≥ 45, Expert ≥ 70. Medium/Hard/Expert also meet the rank floors (`remaining-count` / `clue-overlap`).
- **Solution fill.** Easy filled cells 15–36 of 64; Medium 25–52 of 100; Hard 49–81 of 144; Expert 84–123 of 225.
