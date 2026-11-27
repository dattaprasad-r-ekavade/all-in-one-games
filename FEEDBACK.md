####  Feedback

1. The name of the games are something which the players dont recognise, or are not famous, need to get them changed
   - Pet Crowns is the Crowns display name (engine id stays `crowns`). Avoid Meowdoku, Petdoku, Queens.
   - Other names that need clearer recognition: Binary, Futoshiki, Mosaic, Path, Tactics.
   - Prefer a one-line descriptor on the Game screen (not in the title), or a clearer generic name where it still stays legal: Number Path / Connect Path, Chess Tactics, Klondike Solitaire, Mosaic Grid. Keep Binary if the Home preview clearly shows the two symbols.
   - Welcome still says every puzzle has one answer (false for Family B). Store listing still says four games and Crowns. Update both.

2. crowns:
   a. the animation of tapping needs to be smoother. when we drag finger, it should add the cross in sequence, now they just appear from thin air
   b. when we double tap for queen (do mention to double tap in instructions), and if placement is correct, it should populate the crosses matching the positions, that will make it easy to work with
   - Auto-X must run on any placed crown (row, column, region, eight neighbours), as one undo record. Do not only Auto-X when the crown matches the hidden solution; that would spoil the puzzle.
   - Call out the two-tap cycle (empty → X → crown) in rules and the intro coach line.
   - Default Auto-X on. Undo of a placed crown should also undo its Auto-X marks.
   - Live rule-conflict highlighting: red tint if two crowns touch or share a row, column, or region.

3. This is for all the games. dont add option to check manually, check must happen on the fly.
   - Remove the Check button from Play and the Check-credit rewarded offer from the Game screen.
   - Always-on live rule-conflict highlighting (free, no spoiler): duplicates in a house, two crowns touching or sharing a line/region, three matching Binary symbols, broken Futoshiki signs, over-filled Mosaic clues. Do not compare against the hidden answer.
   - Keep the optional Mistake Limit setting for players who want three wrong entries to fail the level.
   - Family B Check today always returns "no mistakes". Hide or remove it there either way.

4. the easy guided intro is very bad, this will get the audience sleepy in 10 seconds
   - Do not trap players in a separate intro for games most people already know (Sudoku, Mini Sudoku, Klondike, Chess Tactics, Colour Sort). Start on Level 1 with the rules info button and an optional one-line tip.
   - For unfamiliar games (Pet Crowns, Binary, Nonogram, Futoshiki, Skyscrapers, Mosaic, Path, Arrows): a skippable 2–3 move spotlight (dim the board, pulse the target cell, five-word prompt), then Level 1. Always show Skip intro.

5. Sudoku: the digits notes texts lays over the sudoku grid
   - Split Sudoku 9×9, Killer Sudoku and Futoshiki like Mini Sudoku: grid in PlayBoard, digit pad / Notes in gameControls.
   - Mosaic also embeds Fill/Cross inside the board box; split that out the same way.
   - Highlight all matching digits when a filled cell is selected.
   - Make the 9-digit keypad fit cleanly on a 360 dp phone (touch targets at least 48 dp; shorter remaining-count labels).
   - Expose an Auto-update notes toggle in Settings.

6. klondike: i like the UI, its refreshing. the issue is the UX, its unintuitive and we need drag and drop option there. and what is the job of check button on there?
   - Drag a card or run onto a legal pile. Keep tap-tap. Animate the card to the destination unless reduce-motion.
   - Hide Check, or give it a real meaning (for example highlight a stuck tableau).
   - Single-tap smart move to a legal foundation or tableau.
   - Auto-complete once all tableau cards are face-up.
   - Recycle icon on the empty stock pile when waste has cards.
   - Do not force Klondike into a 1:1 square PlayBoard; use a tall portrait layout.

7. chess tactics: are we too poor to add actual chess pieces?
   - Code-drawn piece silhouettes (king, queen, rook, bishop, knight, pawn), not FEN letters. No photo sprites, no new library.
   - Show whose turn it is and mate depth (for example "White to move · Mate in 2").
   - Flip the board when playing Black.
   - Rank/file labels (a–h, 1–8).
   - Legal-move dots when a piece is selected.

8. color sort: one work, yuck. we need 3d look on tubes, animations, this is 2d and scrapyard design
   - Tube with a 3D-looking glass (rim, inner shadow, liquid meniscus) and a pour animation into the destination. Reduce-motion keeps the snap. Paper-and-ink colourways. No new library.
   - Tube-complete celebration (pop/cap).
   - Hint must highlight source and destination tubes and name the pour, not a blank generic dialog.

9. arrows: deparately needs animations
   - Slide the arrow along empty cells to the edge, then remove it. Reduce-motion keeps the snap.
   - Currently highlights every slideable arrow in accent colour, which gives away the puzzle. Draw all arrows the same until tapped.
   - Blocked tap: bump/shake plus a life lost. Valid tap: slide off the board.

10. Home, Game and Result visuals for the 10 new games
    a. Home shows the same placeholder icon for 11 of 14 games. Give every game its own board motif preview.
    b. Result and Level Failed show an empty 5×5 box for all 10 new games. Render real thumbnails for every game.
    c. Home "Today's puzzles" wraps 14 chips into too many rows and pushes the game cards down. Use a compact horizontal scroll row or a collapsible summary.
    d. Game pack size badges show "52×52" on Klondike and "4×4".."10×10" on Colour Sort. Use game-appropriate labels (for example "4 tubes", "Draw 1").

11. Family B hints and fail screen
    a. Hints on Colour Sort and Klondike say "Apply the highlighted move" without highlighting or naming the move. Highlight the source/destination tubes or cards and describe the move.
    b. Level Failed always says "one extra mistake" even for Colour Sort (extra tube) and Klondike (extra undo).

12. Play screen layout and controls
    a. Restart disappears whenever Notes/Erase mode tools are shown (Mini Sudoku, and Sudoku/Killer/Futoshiki once split). Keep Restart accessible.
    b. Zoom / Larger cells should not appear on games that do not support enlarged panning (Colour Sort, Klondike).
    c. Hint badge shows "0" even when free daily or offline hints are available. Show total available hints, or hide / ∞ for Premium.
    d. Status bar under the board is missing for 8 of 14 games. With Mistake Limit on, games without a status line show a stray leading "· Mistakes: 0 of 3".
    e. Tactics should show "White to move · Mate in N" (or Black) in the status line.

13. App-wide reliability and Settings
    a. Rotating the device resets navigation back to Home and recreates DB, ads and billing. Preserve navigation and app singletons across configuration changes.
    b. Sound toggle in Settings defaults to ON (plan says OFF) and currently plays no sound. Wire subtle tap/solve sounds off by default, or hide the toggle until implemented.
    c. Colour-blind aids setting subtitle only mentions Pet Crowns, but Colour Sort also reads that flag. Update the subtitle.
    d. Cold start parses all 56 bank files before Home. Cache pack sizes or lazy-load per game.
    e. chargedHintKeys in DataStore never clears on a new day and grows forever.

14. Sudoku family extra QoL (with item 5)
    a. Highlight matching digits on cell select.
    b. 9-key pad must stay usable on a narrow phone.
    c. Auto-update notes setting.

15. Before H7 hand-test, fix broken play first: drag-X preview, Crowns Auto-X and copy, live check / remove Check, Sudoku-family pad split (including Mosaic), Arrows spoiler highlight, Family B hint highlights, Klondike/Colour Sort pack labels, previews and thumbnails for all 14 games, and rotation-safe navigation.
