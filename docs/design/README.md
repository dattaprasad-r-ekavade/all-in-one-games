# Approved UI design (U1)

The owner approved these mockups on 17 Nov 2026. They are the visual target for U2 to U8.
Each screen is 390 × 844 at 2x scale, in light (`*-light.png`) and dark (`*-dark.png`).

| Screen | Files |
|---|---|
| First-run welcome | `welcome-*.png` |
| Home | `home-*.png` |
| Game (packs) | `game-*.png` |
| Play: Crowns, Binary, Nonogram, Mini Sudoku | `play-*-*.png` |
| Result | `result-*.png` |
| Settings | `settings-*.png` |

## Tokens

| Token | Light | Dark |
|---|---|---|
| Paper (background) | `#F6F1E7` | `#000000` |
| Card | `#FFFDF8` | `#151517` |
| Card edge | `#E3DCCD` | `#2C2C31` |
| Ink | `#1C1B19` | `#EDE9E0` |
| Muted ink | `#65605A` | `#A9A59C` |
| Accent | `#3B4FA0` | `#9AA8F0` |
| On accent | `#FFFFFF` | `#000000` |
| Accent soft | `#E5E8F5` | (accent at low alpha) |

Font: Inter or the system sans. Radii: cards 16–18 dp, buttons 14 dp. Screen margins 16 dp.

## Rules that the images show

- Top bar on every screen: back arrow, title, icon actions. Home actions: Calendar, Statistics, Settings.
- Play: the board fits the free space with 16 dp margins; the tools are an icon bar at the bottom (Undo, Redo, Restart or mode tools, Hint with a credit badge, Check).
- Crowns aids are off by default. Binary uses a filled dot and a ring, not letters.
- **Endless levels:** show only "Level N". No totals, no "x of y", no percent, no progress bars for levels or packs.
- **No fixed game count:** no text says how many games there are.
- Ads: one quiet row on the Game screen ("3 hints and 1 check saved · Get more"). No ad buttons on Home.

The mock data (level numbers, times, streak, pack sizes) is an example only.
