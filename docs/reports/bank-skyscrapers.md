# Skyscrapers bank generation

**Status: incomplete.** The engine registry now discovers Skyscrapers after Mini Sudoku. Three pack files were generated, but a complete launch bank could not be produced with the current engine output.

## Generated files

Seeds are fixed generation seeds. Sizes are compressed file sizes.

| Pack | Size | Count | Grade histogram | Seed |
|---|---:|---:|---|---:|
| Easy (4×4) | 3,527 bytes | 150 | `height-bounds:16` 149; `naked-single:16` 1 | 230901 |
| Medium (5×5) | 4,805 bytes | 150 | `height-bounds:25` 150 | 230902 |
| Hard (6×6) | 6,098 bytes | 150 | `height-bounds:36` 150 | 230903 |

## Missing files

Expert (7×7, seed 230904; retry 230906) and daily (seed 230905) were not written. `bankgen` rejected each generated batch with `Generator returned a duplicate puzzle`. Expert reached 150 candidates on both attempts; daily reached 365. The generator checks fingerprints before atomically writing the file.

## Validation

`validate --all` passed for the committed files currently present (4,314 puzzles in 27 files). This does not mean the Skyscrapers bank is complete: only its Easy, Medium and Hard packs exist. `:engine:test` also fails because `RegistryTest.listsFourStableUniqueGameIdsInProductOrder` expects the old four-engine registry; that test is outside this bank card's ownership.
