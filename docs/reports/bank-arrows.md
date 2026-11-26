# Arrows bank

**Status: complete.** All five Arrows files exist and `validate --all` passes (8,693 puzzles in 53 files).
Arrows is the first Family B game wired into production `bankgen` through `FamilyBRegistry` and `PlayEngineValidator` (using `ArrowsEngine`, `ArrowsRules.isClearable`, and `engine.core.PlayBankLine`).

## Files

Seeds are fixed generation seeds. Sizes are compressed file sizes.

| File | Board | Count | Size | Difficulty histogram | Arrow count range | Distinct layouts | Seed |
|---|---|---:|---:|---|---:|---:|---:|
| `arrows-easy.txt.gz` | 5×5 | 150 | 3,359 bytes | `d1` 150 | 8–16 (avg 11.73) | 150 / 150 | 261001 |
| `arrows-medium.txt.gz` | 6×6 | 150 | 4,042 bytes | `d2` 150 | 14–24 (avg 19.06) | 150 / 150 | 261002 |
| `arrows-hard.txt.gz` | 8×8 | 150 | 5,390 bytes | `d3` 150 | 22–40 (avg 31.31) | 150 / 150 | 261003 |
| `arrows-expert.txt.gz` | 10×10 | 150 | 7,114 bytes | `d4` 150 | 31–60 (avg 46.47) | 150 / 150 | 261004 |
| `arrows-daily.txt.gz` | 5–10 (92/91/91/91) | 365 | 12,388 bytes | `d1` 92, `d2` 91, `d3` 91, `d4` 91 | Easy 8–16; Medium 14–24; Hard 22–40; Expert 30–60 | 365 / 365 | 261005 |
| **Total** | 5–10 | **965** | **32,293 bytes** | `d1` 242, `d2` 241, `d3` 241, `d4` 241 | 8–60 | **965 / 965** | 261001–261005 |

## Layout spread and validation

- **Distinct layouts:** Every file has 100% distinct `size|payload` layouts (`150/150` per pack, `365/365` daily, and `965/965` distinct layouts across all five files combined). Ignoring arrow directions, occupied-cell masks are also `150/150` distinct per pack and `365/365` distinct in daily.
- **Validation:** `.\gradlew.bat :bankgen:run --args="validate --all" --no-daemon -q` passed across all 53 bank files (8,693 puzzles), checking canonical five-field `PlayBankLine` encoding, pack/size/difficulty consistency, initial `!isLost && !isWon`, `ArrowsRules.isClearable`, unique puzzle IDs, and cross-file layout uniqueness.
- **No failed seeds:** Seeds `261001`–`261005` succeeded on the first run with no intra-file or cross-file collisions.
- **Total compressed size:** 32,293 bytes (~0.031 MiB), well below the 1 MB per-game bank budget.

## Family B wiring notes

- `FamilyBRegistry` now supplies `defaultFamilyBValidators` via `PlayEngineValidator<P, S, A>`, adapting `PlayEngine` implementations with `expectedDifficulty` and `canWin` predicates while enforcing `!isLost` and `!isWon` on `initialState`.
- `PlayEngineBankGame` and `bankgen` tests now use `com.datathecodie.witgrid.engine.core.PlayBankLine` directly (duplicate `bankgen/PlayBankLine.kt` removed).
- Future Family B engines (`ColourSortEngine`, `KlondikeEngine`) can be registered in `defaultFamilyBValidators` with a single `PlayEngineValidator(...)` entry each; Family A `TacticsEngine` continues to register via `engine/Registry.kt`.
