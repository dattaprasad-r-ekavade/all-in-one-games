# Klondike bank

**Status: complete.** All five Klondike files exist. `KlondikeEngine` is registered in `FamilyBRegistry` (`PlayEngineValidator` + `KlondikeSolver.isWinnable` on the dealt tableau), so bankgen can generate and validate it.

## Files

Seeds are fixed generation seeds. Sizes are compressed file sizes. Difficulty tokens come from `KlondikeGenerator.configFor`.

| File | Size field | Count | Compressed bytes | Difficulty | Distinct deal seeds | Seed |
|---|---|---:|---:|---|---:|---:|
| `klondike-easy.txt.gz` | 52 | 150 | 3,525 | `d1` 150 | 150 / 150 | 281001 |
| `klondike-medium.txt.gz` | 52 | 150 | 3,568 | `d2` 150 | 150 / 150 | 281002 |
| `klondike-hard.txt.gz` | 52 | 150 | 3,532 | `d3` 150 | 150 / 150 | 281003 |
| `klondike-expert.txt.gz` | 52 | 150 | 3,573 | `d4` 150 | 150 / 150 | 281004 |
| `klondike-daily.txt.gz` | 52 | 365 | 8,760 | `d1` 92, `d2` 91, `d3` 91, `d4` 91 | 365 / 365 | 281005 |

Total Klondike bank size: 22,958 bytes across 965 deals (5 files).

## Daily

| Seed | Count | Compressed bytes | Pack counts |
|---:|---:|---:|---|
| 281005 | 365 | 8,760 | Easy 92 (`d1`); Medium 91 (`d2`); Hard 91 (`d3`); Expert 91 (`d4`) |

## Generation notes

- Payload is the Draw-1 deal seed (`KlondikeCodec`); uniqueness is per `size|payload|pack`.
- Seeds `281001`–`281005` succeeded on the first run. No intra-file or cross-file seed collisions.
- Wall times on this PC: Easy 28 s, Medium 34 s, Hard 36 s, Expert 49 s, daily 69 s (8 workers). Each stored deal was accepted by `KlondikeSolver.solveForGeneration` and the pack move/recycle floors.
- Pack floors: Easy max 135 total moves; Medium 76–135 total and ≥52 non-draw; Hard ≥134 total, ≥80 non-draw, ≥2 recycles; Expert ≥140 total, ≥83 non-draw, ≥2 recycles.

## Family B wiring notes

- `FamilyBRegistry` adds `PlayEngineValidator(KlondikeEngine, …) { KlondikeSolver.isWinnable(KlondikeDeal.deal(it)) }`.
- Deal-start invariants stay in `PlayEngineValidator`: `!isLost` and `!isWon` on `initialState` (Klondike `isLost` is false at `moveCount == 0`).
- `KlondikeBankValidatorTest` covers production registration, canonical-line rejects, and a 2-deal Easy CLI generate.
