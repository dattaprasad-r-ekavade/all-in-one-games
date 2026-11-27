# Tactics bank

**Status: complete.** All five Tactics files exist. `TacticsEngine` is registered in `Registry.engines` after `MosaicEngine`, so bankgen can generate and validate it.

## Files

Seeds are fixed generation seeds. Sizes are compressed file sizes. Grade histograms count the hardest named mating pattern on the unique line.

| File | Size field | Count | Compressed bytes | Mate in | Grade histogram | Pieces | Seed |
|---|---|---:|---:|---|---|---|---:|
| `tactics-easy.txt.gz` | 8 | 150 | 3,941 | 1 | `smothered-mate` 71, `arabian-mate` 55, `back-rank-mate` 24 | 7–12 (avg 9.8) | 291001 |
| `tactics-medium.txt.gz` | 8 | 150 | 3,851 | 2 | `back-rank-mate` 88, `smothered-mate` 61, `checkmate` 1 | 9–10 (avg 9.8) | 291002 |
| `tactics-hard.txt.gz` | 8 | 150 | 4,547 | 2 | `smothered-mate` 84, `back-rank-mate` 66 | 13–16 (avg 14.3) | 291003 |
| `tactics-expert.txt.gz` | 8 | 150 | 4,152 | 3 | `smothered-mate` 110, `checkmate` 40 | 10–15 (avg 11.9) | 291004 |
| `tactics-daily.txt.gz` | 8 | 365 | 10,647 | 1–3 | `smothered-mate` 187, `back-rank-mate` 107, `checkmate` 37, `arabian-mate` 34 | 7–16 (avg 11.5) | 291005 |

Total Tactics bank size: 27,138 bytes across 965 puzzles (5 files). All 965 fingerprints (`size|payload|pack`) are distinct.

## Daily

| Seed | Count | Compressed bytes | Pack counts |
|---:|---:|---:|---|
| 291005 | 365 | 10,647 | Easy 92 (mate in 1); Medium 91 (mate in 2); Hard 91 (mate in 2, more pieces); Expert 91 (mate in 3) |

## Generation notes

- Payload is FEN plus the mate horizon (`TacticsCodec`); uniqueness is per `size|payload|pack`.
- Seeds `291001`–`291005` succeeded on the first run. No intra-file or cross-file fingerprint collisions.
- Wall times on this PC: Easy 49 s, Medium 15 s, Hard 15 s, Expert 31 s, daily 40 s. Unique-mate volume is no longer a cut risk: 150 per pack plus 365 daily all accepted.
- Pack bands match plan 12.6: Easy mate in 1; Medium mate in 2 with fewer pieces; Hard mate in 2 with more pieces; Expert mate in 3.

## Family A wiring notes

- `Registry` adds `TacticsEngine` after `MosaicEngine` so bankgen `generate` / `validate --all` can see it. GameRegistry / intro stay on CTC.
- Generator tests already proved 241 distinct positions per pack; this bank keeps 965 distinct starting fingerprints across the five files.
- `./gradlew :bankgen:run --args="validate --all"` passed: 13,523 puzzles in 83 files (53 m 20 s).
