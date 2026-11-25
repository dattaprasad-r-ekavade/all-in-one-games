# Futoshiki bank

Generated with the deterministic Futoshiki engine generator. All files passed
the bank validator, including the cross-file duplicate check.

## Packs

| Pack | Seed | Count | Compressed bytes | Grade histogram | Sign count range |
|---|---:|---:|---:|---|---:|
| Easy (5×5) | 240801 | 150 | 6,104 | sign-bounds 142, naked-single 7, hidden-single 1 | 0–8 |
| Medium (6×6) | 240802 | 150 | 8,073 | sign-chains 149, sign-bounds 1 | 1–13 |
| Hard (7×7) | 240803 | 150 | 10,744 | sign-chains 150 | 10–16 |
| Expert (7×7) | 240804 | 150 | 10,410 | naked-pair 150 | 4–8 |

The plan's sign-count rule is met: every Hard puzzle has at least 10 signs, and
every Expert puzzle has at most 8.

## Daily

| Seed | Count | Compressed bytes | Size / pack counts | Grade histogram | Sign count range by pack |
|---:|---:|---:|---|---|---|
| 240805 | 365 | 21,917 | 5×5: 92 Easy; 6×6: 91 Medium; 7×7: 91 Hard, 91 Expert | sign-chains 182, naked-pair 91, sign-bounds 90, naked-single 1, hidden-single 1 | Easy 0–9; Medium 2–11; Hard 10–14; Expert 3–8 |

## Solution pattern

The generator builds each solution from a cyclic Latin-square template, then
randomizes row order, column order, digit mapping, and sometimes transposes it.
The resulting solutions preserve that permuted cyclic structure; they are not
independent arbitrary Latin squares. This is a visible regularity of the bank.
