# Path bank

Generated with the deterministic Path engine generator (`PathEngine.generate` / `PathGenerator`). All files passed the bank validator, including canonical line encoding, uniqueness verification (`countSolutions == 1`), technique-grade consistency, and the cross-file duplicate check.

## Packs

| Pack | Seed | Count | Compressed bytes | Grade histogram | Wall count range | Pair count range |
|---|---:|---:|---:|---|---:|---:|
| Easy (6×6) | 241101 | 150 | 7,197 | forced-neighbour 130, endpoint-extension 20 | 0–6 | 6–10 |
| Medium (8×8) | 241102 | 150 | 11,377 | forced-neighbour 150 | 0–9 | 9–16 |
| Hard (10×10) | 241103 | 150 | 16,883 | forced-neighbour 150 | 2–13 | 13–21 |
| Expert (12×12) | 241104 | 150 | 23,600 | forced-neighbour 150 | 5–17 | 17–27 |

Every puzzle meets the plan 12.3 rules:
- Paths never branch or cross.
- Paths never touch themselves (no cell of a path touches another cell of the same path except its path neighbours; no 2×2 block of one colour).
- Every non-wall cell is filled.
- Exactly one solution.
- Solvable by logical techniques with no guess.

## Daily

| Seed | Count | Compressed bytes | Size / pack counts | Grade histogram | Wall count range by pack | Pair count range by pack |
|---:|---:|---:|---|---|---|---|
| 241105 | 365 | 36,839 | 6×6: 92 Easy; 8×8: 91 Medium; 10×10: 91 Hard; 12×12: 91 Expert | forced-neighbour 354, endpoint-extension 10, dead-end-avoidance 1 | Easy 0–6; Medium 1–8; Hard 2–11; Expert 4–15 | Easy 6–10; Medium 10–15; Hard 13–21; Expert 18–29 |

## Generation pattern

The generator builds boards using constrained random walks with non-self-touching path rules. Unassigned cells are prioritized using a Warnsdorff-like degree heuristic to avoid isolated single cells, and isolated cells that cannot form paths are converted into interior walls (within budget limits). Candidates are validated with `PathRules.countSolutions(limit = 2) == 1` and graded by `PathTechniqueSolver.gradeByTechniques` to ensure guess-free logical solvability.
