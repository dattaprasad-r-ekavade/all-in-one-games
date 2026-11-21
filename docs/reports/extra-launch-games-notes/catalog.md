# Catalog of extra launch game types for Witgrid

Scope: paper-and-ink / code-drawn 2D grid logic types that could join Witgrid at or soon after Android launch. Launch set already in: Crowns (Star Battle, one star, no diagonal touch), Binary (Takuzu / Binairo), Nonogram (abstract, not pictures), Mini Sudoku 6x6. Plan later waves are listed as options, not decisions. Names banned in product copy: Queens, Tango, Zip, LinkedIn, NYT, Takuzu, Binairo, Picross, KenKen, Tetris, Block Blast, 2048, Flow Free, Rush Hour, Wordle.

Human session-length distributions were not published for most types. Where a source gives a size, time, or download count, it is cited. Where it does not, the gap is stated.

## What other grid-logic / paper-and-ink puzzles are popular on mobile or in Nikoli/Conceptis catalogs?

### Takeaway
Nikoli, Conceptis, Simon Tatham, PuzzleMix, and LinkedIn/NYT daily apps all ship the same core families: Latin-square numbers, shading, loops/paths, object placement, and region division. Typical published boards run from about 5x5 (easy Futoshiki / Zip / Tents) to 10x10 or 16x16 (Battleship, Kakuro). Published human solve times are scarce; Tatham designed the collection for two-to-three-minute breaks.

### Cited Findings

**Catalogues (who publishes what)**

- Nikoli’s English Wikipedia list of popular types includes Hashiwokakero (Bridges), Heyawake, Hitori, Kakuro, Light Up, LITS, Masyu, Numberlink, Nurikabe, Shikaku, Slitherlink, Sudoku, Tentai Show (Galaxies), Yajilin, Fillomino, and Edel (Nonogram), among others. — [Wikipedia: Nikoli (publisher)](https://en.wikipedia.org/wiki/Nikoli_(publisher))
- Nikoli still sells giant Kakuro and Slitherlink books as of 7 July 2025 (35 of each). — [Nikoli news](https://www.nikoli.co.jp/en/news/nikolis-best-selection-of-giant-puzzles-kakuro-slitherlink-is-now-on-sale/)
- Conceptis’ number-logic catalog for publishers lists Sudoku, Kakuro, Hitori, Battleships, Slitherlink, Hashi, CalcuDoku, Nurikabe, Skyscrapers, Tic-Tac-Logic. Picture-logic catalog lists Pic-a-Pix, Sym-a-Pix, Link-a-Pix, Fill-a-Pix, Maze-a-Pix, Dot-a-Pix, Cross-a-Pix, Block-a-Pix. — [Conceptis Download Center](https://www.conceptispuzzles.com/c2/download/index.html)
- Conceptis ships Android apps for Pic-a-Pix Color, Fill-a-Pix, MultiSudoku, SumSudoku (Killer), Link-a-Pix, Cross-a-Pix, Sudoku, Hashi, Block-a-Pix, Tic-Tac-Logic, Kakuro, Dot-a-Pix, Slitherlink, SeaBattle, Sym-a-Pix, Nurikabe, Pix-o-Mania. — [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile)
- Fun With Puzzles hosts 11 daily Conceptis widgets: Kakuro, Battleships, Hashi, Calcudoku, Slitherlink, Hitori, Nurikabe, Sudoku, Skyscrapers, Tic-Tac-Logic, Chain Sudoku. — [Fun With Puzzles daily Conceptis](https://www.funwithpuzzles.com/p/conceptis-daily-puzzles.html)
- PuzzleMix (Gareth Moore) lists Sudoku, Hanjie, Kakuro, Futoshiki, Calcudoku, Hitori, Killer Sudoku, Nurikabe, Slitherlink, Skyscraper, and several Sudoku variants as playable on PC, Mac, iPad, iPhone, and Android. — [puzzlemix.com](https://puzzlemix.com/)
- Simon Tatham’s Portable Puzzle Collection is 40 one-player games, MIT-licensed, with JS/WASM web play and Windows binaries; Tatham wrote them as “little games you can pop up in a window and play for two or three minutes.” Android port by Chris Boyle is described as free, ad-free, offline. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [DBotMaker review 2026](https://dbotmaker.io/review/simon-tatham-s-portable-puzzle-collection)
- Tatham mappings of classic types: Bridges = Hashiwokakero; Loopy = Slitherlink; Light Up = Akari; Pattern = Nonogram; Pearl = Masyu; Rectangles = Shikaku; Filling = Fillomino; Singles = Hitori; Keen = KenKen/Calcudoku; Towers = Skyscrapers; Unequal = Futoshiki; Unruly = binary/Takuzu-like; Tents = Tents and Trees; Solo = Sudoku; Mines = Minesweeper; Mosaic = Fill-a-Pix-like neighbor counts. — [Libregamewiki](https://libregamewiki.org/Simon_Tatham's_Portable_Puzzle_Collection); [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- A 2026 taxonomy groups the family as: Latin square (Sudoku, Futoshiki, KenKen); totals (Kakuro, KenKen); fill-or-blank (Nonograms, Nurikabe, Tapa, LITS); loop/path (Slitherlink, Masyu, Yajilin, Numberlink); object placement (Battleships, stars). Nikoli itself lists ~47 types alphabetically with no families. — [The Logicat, 7 Aug 2026](https://www.thelogicat.com/map-of-logic-puzzles/)

**Per-type rules, typical size, session length**

Sizes below are what a cited source actually states. Session length is marked “not published” unless a source gives a number.

- **Sudoku 9x9.** Fill 1–9 so each row, column, and 3x3 box has each digit once. Standard daily size is 9x9; NYT publishes Easy, Medium, and Hard each day. Session length: not published by NYT; Witgrid plan estimate for this wave is 5–15 min (local plan, not a measured source). — [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games); [NYT Games Play listing](https://play.google.com/store/apps/details?id=com.nytimes.crossword&hl=en_CA)
- **Killer Sudoku (Sumdoku / samunamupure).** Same 9x9 Sudoku regions, empty start, cages whose digits sum to a given total and do not repeat. Invented in Japan (Tetsuya Nishio / Miyuki Nisawa); *The Times* from 2005. Conceptis app name: SumSudoku. Session: not published. — [Wikipedia: Killer sudoku](https://en.wikipedia.org/wiki/Killer_sudoku); [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile)
- **Kakuro (Cross Sums).** Fill white cells with 1–9 so each run sums to its clue and digits in a run do not repeat. “Puzzles are usually 16×16 in size, although these dimensions can vary widely.” Popularity in Japan “second only to Sudoku among Nikoli’s famed logic-puzzle offerings.” Session: not published. Solver writeups use 15x15, 20x20, 30x30 bank puzzles. — [Wikipedia: Kakuro](https://en.wikipedia.org/wiki/Kakuro); [marcelgarus.dev Kakuro](https://marcelgarus.dev/kakuro)
- **Futoshiki (More or Less / inequalities).** Latin square of size n with > / < signs between some neighbours. Developed by Tamaki Seto in 2001. Wikipedia example is 5x5. UK papers publish it daily or weekly (*Times*, *Guardian*, *Telegraph*). Tatham name: Unequal. Session: not published. — [Wikipedia: Futoshiki](https://en.wikipedia.org/wiki/Futoshiki); [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **Skyscrapers (Towers).** Latin square of building heights; edge clues count how many buildings are visible along that line. Tatham “Towers”: “Complete the latin square of towers in accordance with the clues.” Conceptis and PuzzleMix both ship it. Typical size: not stated as a single standard; Tatham/Archipelago lists 3x3 up through 5x5 in one port’s presets. Session: not published. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/); [Archipelago SGT Puzzles](https://archipelago.miraheze.org/wiki/Simon_Tatham%27s_Portable_Puzzle_Collection)
- **Calcudoku (generic name for KenKen).** Latin square plus cages that must reach a target by +, −, ×, or ÷. Tatham “Keen”: 4x4 easy through 9x9. Conceptis lists CalcuDoku in both web catalog and daily widgets. Session: not published. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [Conceptis home](https://www.conceptispuzzles.com/); [KenKen Wikipedia via ipfs snapshot](https://ipfs.io/ipfs/QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco/wiki/KenKen.html)
- **Thermometers.** Values increase from bulb to tip along thermometer shapes. Listed as a Japanese-style number puzzle; no standard size or session time found in the sources fetched. — [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Hashiwokakero (Bridges / Hashi).** Connect numbered islands with orthogonal bridges, at most two between a pair, no crossing, each island’s degree equals its number, whole graph connected. “Played on a rectangular grid with no standard size.” First Nikoli appearance 1990 (issue 31). Conceptis Android app: Hashi. Tatham: Bridges. Session: not published. — [Wikipedia: Hashiwokakero](https://en.wikipedia.org/wiki/Hashiwokakero); [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile)
- **Nurikabe (Islands and walls).** Numbered white islands of exact size, one number per island; black “sea” is one orthogonally connected wall with no 2x2 pools. Wikipedia example: “moderately difficult 10x9.” Conceptis Android app: Nurikabe. Online solver UI on one site supports 4x4–8x8 custom grids (that is a tool limit, not a published standard). Session: not published. — [Wikipedia: Nurikabe](https://en.wikipedia.org/wiki/Nurikabe_(puzzle)); [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile); [logic-puzzles-online Nurikabe solver](https://logic-puzzles-online.com/tools/nurikabe-solver/)
- **Slitherlink (Fences / Loopy).** Draw one simple loop on the lattice; a number in a cell is the count of its edges used by the loop. Nikoli original, first in *Puzzle Communication Nikoli* #26 (June 1989). Tatham Loopy presets include 7x7 square easy and 10x10 square easy/normal/hard (plus exotic tilings). Conceptis Android app: Slitherlink. Hudson Soft shipped a Nintendo DS volume in 2006. Session: not published. — [Wikipedia: Slitherlink](https://en.wikipedia.org/wiki/Slitherlink); [Archipelago SGT Puzzles](https://archipelago.miraheze.org/wiki/Simon_Tatham%27s_Portable_Puzzle_Collection); [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile)
- **Masyu (Pearl).** One closed loop; white pearls force a straight through the pearl and a turn on at least one side; black pearls force a turn on the pearl and straights on both sides. Tatham name: Pearl. Session and size: not published in fetched pages. — [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/); [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **Akari / Light Up.** Place bulbs so every white cell is lit; bulbs shine along row/column until a black cell; bulbs may not shine on each other; numbered blacks force that many adjacent bulbs (0–4). Tatham Light Up presets: 7x7 / 10x10 / 14x14 at easy, tricky, hard. Nikoli Japanese title: 美術館 (*bijutsukan*). Session: not published. — [Wikipedia: Light Up](https://en.wikipedia.org/wiki/Light_Up_(puzzle)); [Archipelago SGT Puzzles](https://archipelago.miraheze.org/wiki/Simon_Tatham%27s_Portable_Puzzle_Collection)
- **Shikaku (Rectangles / Divide by Squares).** Partition the grid into rectangles; each contains exactly one number equal to its area. Tatham: Rectangles. LinkedIn Patches is the same family with extra shape icons (square / tall / wide / any). Session: not published. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [LinkedIn Patches guide](https://minutecryptic.org/patches-answer-today); [Wikipedia Nikoli list](https://en.wikipedia.org/wiki/Nikoli_(publisher))
- **Fillomino (Filling).** Divide the grid into polyominoes whose size equals the number written in them. Tatham: Filling. Session: not published. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [Wikipedia Nikoli list](https://en.wikipedia.org/wiki/Nikoli_(publisher))
- **Tents (Tents and Trees).** Place one tent orthogonally next to each tree; tents do not touch even diagonally; row and column counts match. Tatham: “Place a tent next to each tree.” Session: not published. Witgrid plan later-wave estimate: 1–5 min (local plan). — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Battleship logic (Solitaire Battleships / Bimaru / Yubotu / SeaBattle).** Hide a fleet on a 10x10 grid (one 4, two 3s, three 2s, four 1s); ships do not touch even diagonally; row/column occupancy numbers are given. Invented in Argentina 1982 (Jaime Poniachik). WPC from 1992. Conceptis Android app: SeaBattle. Session: not published. — [Wikipedia: Battleship (puzzle)](https://en.wikipedia.org/wiki/Battleship_(puzzle)); [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile)
- **Numberlink (Arukone / Nanbarinku).** Join each equal-number pair with a path; paths do not branch or cross. Well-designed instances have a unique solution and fill every cell. Mobile clones include Flow Free (Big Duck Games). Session: not published. Witgrid plan Path/Number link estimate: 20–90 s (local plan). — [Wikipedia: Numberlink](https://en.wikipedia.org/wiki/Numberlink)
- **Hidato.** Complete a consecutive-number path through the grid from given anchors. Listed as a Japanese-style number-path puzzle; size/session not published in fetched pages. — [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Hitori (Singles).** Shade some cells so remaining numbers in each row/column are unique, shaded cells do not touch orthogonally, remaining white cells stay connected. Tatham: Singles. Conceptis lists Hitori in publisher catalog and daily widgets (no dedicated Android tile on the mobile index fetched). Session: not published. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [Conceptis Download Center](https://www.conceptispuzzles.com/c2/download/index.html)
- **LITS (Nuruomino).** Shade a tetromino in each region so all shaded cells form one connected wall, no 2x2, and identical tetrominoes do not touch by an edge. Nikoli type. Size/session not published in fetched pages. — [Wikipedia Nikoli list](https://en.wikipedia.org/wiki/Nikoli_(publisher)); [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Fill-a-Pix / Mosaic.** Each clue counts how many of the cell and its neighbours (up to 9) are painted; the painted set often forms a picture. Conceptis Android app: Fill-a-Pix: Minesweeper Puzzle. Tatham Mosaic: “Fill in the grid given clues about number of nearby black squares.” Size varies with the picture; session not published. — [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile); [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **Easy as ABC (End View).** Place letters so each appears once per row/column; edge clues name the first letter seen from that direction. Indexed as End View in the MIT Mystery Hunt Nikoli-style list. Size/session not published in fetched pages. — [MIT Mystery Hunt index](https://devjoe.appspot.com/huntindex/category/nikolistylelogic)
- **Yajilin (Arrow Ring).** Shade some cells per arrow-count clues and draw a loop through remaining cells. Nikoli type. Size/session not published in fetched pages. — [Wikipedia Nikoli list](https://en.wikipedia.org/wiki/Nikoli_(publisher)); [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Galaxies (Tentai Show).** Divide the grid into 180°-symmetric regions, each centred on a given dot. Tatham: Galaxies. Session: not published. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [Wikipedia Nikoli list](https://en.wikipedia.org/wiki/Nikoli_(publisher))
- **Heyawake.** Room-based shading: numbered rooms give the black-cell count; black cells do not share an edge; white corridors cannot span more than two rooms. Nikoli type. Size/session not published. — [Wikipedia Nikoli list](https://en.wikipedia.org/wiki/Nikoli_(publisher)); [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Tapa.** Clue digits describe lengths of consecutive shaded blocks among the eight neighbours; shaded wall connected, no 2x2. Size/session not published. — [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Suguru.** Irregular regions of size n contain 1..n; equal numbers may not touch even diagonally. Size/session not published. — [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Ripple Effect (Hakyuu).** Fill rooms with 1..size; equal numbers must be separated by at least that many cells in row/column. Nikoli type. Size/session not published. — [Wikipedia Nikoli list](https://en.wikipedia.org/wiki/Nikoli_(publisher)); [logic-puzzles-online Japanese list](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Dominosa / Pips-like.** Tile the rectangle with a full set of dominoes (Tatham Dominosa), or place given dominoes to satisfy region conditions (NYT Pips, 2025). Tatham: Dominosa. NYT Pips: “players place dominoes on a gameboard in order to satisfy certain conditions.” Session: not published. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games)
- **Minesweeper (no-guess variant).** Find mines from neighbour counts without stepping on one. Tatham Mines generates puzzles. Classic Windows sizes (not Tatham-specific): not restated here because this fetch did not include Microsoft’s size table. Session: not published. Witgrid plan estimate: 1–5 min (local plan). — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **LinkedIn Zip (Numberlink + Hamiltonian path).** One orthogonal path that visits every cell once, hits numbered cells in order, and cannot cross walls. Practice boards on one fan site: 5x5, 6x6, 7x7. Official help: daily at midnight PT. Session: not published. Witgrid Path estimate 20–90 s is the closest plan figure. — [LinkedIn Help: Zip](https://www.linkedin.com/help/linkedin/answer/a7445030); [tryhardpuzzles Zip](https://tryhardpuzzles.com/linkedin-zip)
- **LinkedIn Patches (Shikaku + shape icons).** Partition into rectangles; each has one clue = area; icon forces square, tall, wide, or any. Fan practice board cited as 6x6. Launched 18 March 2026. Session: not published. — [Patches guide](https://minutecryptic.org/patches-answer-today); [AWA launch dates](https://awa.games/linkedin-games/resources/new-linkedin-game/)
- **Arrows escape.** Tap an arrow; it slides off the board in the direction it points iff the path to the edge is empty. Lessmore GmbH *Arrows – Puzzle Escape*: App Store 9 Aug 2025; Google Play “over 100 million” downloads and 4.5 stars on 2.19 million reviews as of an 18 Oct 2026 Italian writeup. Solver tools accept 3x3–12x12. Clone listings use 5x5 up to 12x9 or larger picture boards. Session: not published; Witgrid plan estimate 30–120 s (local plan). — [Fantagiochi, 18 Oct 2026](https://www.fantagiochi.it/arrows-puzzle-escape-soluzioni/); [GridPaw rules](https://gridpaw.com/arrows-puzzle/); [chaoschemy solver](https://chaoschemy.com/tools/arrow-escape-solver/)
- **Colour sort / number merge / Klondike / word search.** These sit in Witgrid’s later-wave table as options. They are not Nikoli/Conceptis paper-logic types. Colour sort is a tube-stacking casual; number merge is procedural/endless; Klondike is card solitaire; word search is a word list in a letter grid. Session estimates in the local plan only. Do-not-add list already excludes screw sort, hexa sort, tile match, mahjong, jigsaw, five-letter word guess, sliding-block escape.

### Inferences
- The densest overlap with Witgrid’s paper-and-ink, unique-solution, no-guess brief is the Nikoli/Conceptis/Tatham set (Bridges, Slitherlink, Nurikabe, Kakuro, Hashi, Tents, Shikaku, Akari, Futoshiki, Skyscrapers, Calcudoku, Killer, Battleship, Hitori, Masyu, Numberlink), not the casual sort/merge/solitaire/word cluster.
- Short-session Tatham design (two to three minutes) matches Witgrid’s current 45–150 s launch games better than Killer Sudoku or 16x16 Kakuro.
- LinkedIn Patches and Zip are the 2025–2026 daily-app forms of Shikaku and Numberlink/Path, so those two paper types now have mass-market daily demand even though Witgrid cannot use the LinkedIn names.

### Gaps
- No source in this pass published median human solve times for Hashi, Slitherlink, Nurikabe, Kakuro, Futoshiki, Skyscrapers, Tents, Akari, Shikaku, Battleship, Masyu, Hitori, LITS, Fill-a-Pix, Easy as ABC, Thermometers, or Hidato.
- Nikoli’s own English `/en/puzzles/` page redirected and was not fetched in full; type list is from Wikipedia’s Nikoli article plus individual Wikipedia pages.
- Play Store install counts for Conceptis apps, Andoku, and Sudoku.com were not captured (Conceptis mobile page has no counts).
- Easy as ABC, LITS, Thermometers, Heyawake, Tapa, Suguru, Ripple Effect: rules confirmed in secondary catalogs; primary Nikoli English rule pages and typical published sizes were not fetched.

## Which of these appear in top mobile apps or LinkedIn/NYT-adjacent daily games in 2024–2026?

### Takeaway
LinkedIn’s 2024–2026 daily slate is mostly types Witgrid already ships (Crowns, Binary, Mini Sudoku) plus Zip (path), Patches (rectangles), and two word games, with Sumbreak (number merge) added on 18 Nov 2026. NYT’s 2024–2026 hits are word games plus Sudoku and, from August 2025, Pips (domino logic). Conceptis still covers the classic Nikoli set on Android. Arrows-escape became a 100M-download casual-logic breakout in 2025–2026.

### Cited Findings

**LinkedIn Games (as of 18 Nov 2026)**

- Launch timeline from one dedicated tracker: Queens 1 May 2024; Tango 8 Oct 2024; Zip 18 Mar 2025; Mini Sudoku 12 Aug 2025; Patches 18 Mar 2026; Wend 9 Jun 2026; Sumbreak 18 Nov 2026. Pinpoint and Crossclimb opened the series with Queens on 1 May 2024. That is nine dailies. — [AWA: New LinkedIn Game](https://awa.games/linkedin-games/resources/new-linkedin-game/)
- Alldle (1 Nov 2026, before Sumbreak) listed eight: Queens, Tango, Zip, Mini Sudoku, Pinpoint, Crossclimb, Patches, Wend. Queens = one queen per row, column, and coloured region. Tango = two symbols, equal counts per row/column. Zip = one path through every cell, numbers in order. Mini Sudoku = 6x6. Pinpoint and Crossclimb are word games. — [Alldle LinkedIn Games](https://www.alldle.net/blog/linkedin-games)
- Official Zip help: “Draw a single path that fills every cell. Follow the numbered cells in order. If the grid has walls, your path cannot go through them.” New puzzle daily at midnight PT. — [LinkedIn Help: Zip](https://www.linkedin.com/help/linkedin/answer/a7445030)
- Patches: split the grid into rectangles; each clue is area plus a shape icon (square / tall / wide / any); one clue per patch; cover with no gaps. — [Patches how-to](https://axeetech.com/puzzles/how-to-play-linkedin-patches/)
- Wend: letter grid; find winding orthogonal words; every letter used once; length clues under the grid. Word game, not number logic. — [AWA Wend how-to](https://awa.games/linkedin-games/wend/howto/)
- Sumbreak (18 Nov 2026): drag a number onto an orthogonal neighbour with the same value; they merge into their sum; wildcards add or multiply; solved when one number remains. This is a merge puzzle, not a unique-bank logic grid. — [AWA Sumbreak how-to](https://awa.games/linkedin-games/sumbreak/howto/)
- Fan answer sites still treat Queens, Tango, Zip, Mini Sudoku, Pinpoint, and Crossclimb as the daily core through mid-November 2026 (e.g. puzzle numbers for 16 Nov 2026: Queens #930, Tango #770, Zip #609, Mini Sudoku #462). — [LiAnswers](https://lianswers.com/); [Fresh Calendars 15 Nov 2026](https://freshcalendars.com/news/linkedin-games-answers-today-november-15-2026-queens-tango-zip/)

**NYT Games (2024–2026)**

- 2024 play counts, NYT spokesperson to *The Verge* (reported June 2025): 11.1 billion puzzle plays; Wordle 5.3B, Connections 3.3B, Strands 1.3B. — [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games)
- Strands launched (left beta) late June 2024: themed word-search on a letter grid. Connections: Sports Edition launched 9 Feb 2025. — [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games)
- Pips entered beta in Canada 18 Apr 2025 and launched 18 Aug 2025. Wikipedia: “the second logic puzzle besides Sudoku on NYT Games, and its first original logic puzzle.” Domino placement on a conditioned board. Free in the listing table. — [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games)
- Sudoku remains in the NYT app, three daily difficulties, described as free for the first chosen daily (table truncated in fetch). — [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games); [Play listing](https://play.google.com/store/apps/details?id=com.nytimes.crossword&hl=en_CA)
- Fall Harvest promo (live through 18 Nov 2026) gave free archive copies of Wordle, Connections, Mini, Strands, Midi — no extra logic types. — [Beebom, 10 Nov 2026](https://beebom.com/puzzle/nyt-games-fall-harvest-event-free-archived-puzzles/)
- Active NYT set also includes Crossword, Mini, Midi, Spelling Bee, Letter Boxed, Tiles (visual matching), Crossplay (multiplayer word, US launch 21 Jan 2026). Tiles is not a unique-solution logic grid. Vertex was discontinued Aug 2024. Digits (number game) shut down Aug 2023. — [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games)
- Independent 2026 roundup of NYT Games lists Sudoku and Pips as the number/logic pair, Tiles as the only other “logic” item, and the rest as word/crossword. — [Puzznest NYT Games list](https://puzznest.com/nytimes-games)

**Conceptis / PuzzleMix / Tatham on mobile**

- Conceptis Android lineup (fetched Nov 2026 page, copyright line 1997–2025): Pic-a-Pix Color, Fill-a-Pix, MultiSudoku, SumSudoku, Link-a-Pix, Cross-a-Pix, Sudoku, Hashi, Block-a-Pix, Tic-Tac-Logic, Kakuro, Dot-a-Pix, Slitherlink, SeaBattle, Sym-a-Pix, Nurikabe, Pix-o-Mania. — [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile)
- PuzzleMix remains a current (page date 16 Nov 2026) browser/mobile catalog of Sudoku, Kakuro, Futoshiki, Calcudoku, Killer, Nurikabe, Slitherlink, Skyscraper, Hitori, Hanjie. — [puzzlemix.com](https://puzzlemix.com/)
- Tatham collection: 40 puzzles; Android port described in a Nov 2026 review as always free, no ads, offline, on-demand generation. — [DBotMaker, 11 Nov 2026](https://dbotmaker.io/review/simon-tatham-s-portable-puzzle-collection)

**Arrows-escape breakout (2025–2026)**

- Lessmore GmbH *Arrows – Puzzle Escape*: App Store 9 Aug 2025; Google Play “oltre 100 milioni” downloads, 4.5 stars / 2.19 million reviews, App Store Italy rank 87 in puzzles as of 18 Oct 2026 writeup; iOS build 0.23 dated 7–9 Oct 2026. — [Fantagiochi](https://www.fantagiochi.it/arrows-puzzle-escape-soluzioni/)
- Multiple Android clones in 2026 (Arowes, Arrow Escape by Neukaps, Arrow Escape: Unblock Puzzle, Arrow Puzzle) copy the tap-to-slide rule, 3-heart fail, and no-timer pitch. — [Play: Arowes](https://play.google.com/store/apps/details?id=com.anggastudio.arowes&hl=en_US); [Play: Arrow Escape Neukaps](https://play.google.com/store/apps/details?id=com.neukaps.arrowescape&hl=en_US)
- CrazyGames *Arrow Escape* (Bravestars) listed as released January 2026. — [CrazyGames](https://www.crazygames.com/game/arrow-exit-puzzle)

**Not found in 2024–2026 LinkedIn/NYT dailies**

- Hashi, Slitherlink, Nurikabe, Kakuro, Futoshiki, Skyscrapers, Tents, Akari, Battleship, Masyu, Hitori, Fill-a-Pix, Killer Sudoku, Calcudoku do not appear as named LinkedIn or NYT daily games in the 2024–2026 sources above. They remain in Conceptis/PuzzleMix/Tatham catalogs instead.

### Inferences
- If Witgrid wants “what daily-app players already know in 2026,” the unpaid demand is Crowns, Binary, Mini Sudoku (already shipping), then Path/Zip-style, Rectangles/Patches, Sudoku 9x9, Pips-style dominoes, and arrows-escape — not a new Nikoli deep cut.
- NYT’s only original grid-logic addition in this window is Pips, which is closer to Tatham Dominosa than to Crowns/Binary/Nonogram/Mini Sudoku.
- Arrows-escape is a high-volume 2025–2026 Play genre, but it is a tap-order casual with hearts, not a unique no-guess bank in the Nikoli sense. Witgrid’s plan already flags it as a wave-2 option.

### Gaps
- No Play Console or Sensor Tower rank tables were fetched, so “top” here means (a) presence in LinkedIn/NYT daily suites, (b) Conceptis first-party apps, (c) one cited 100M-download arrows title. Install ranks for Sudoku.com, Easybrain, Andoku, Puzzle Page, and individual Conceptis apps are unknown from this pass.
- LinkedIn official help pages for Queens, Tango, Mini Sudoku, Patches, Wend, and Sumbreak were not fetched (Zip help was). Rules for those come from trackers and fan how-tos plus Alldle’s summary of LinkedIn Help.
- Whether LinkedIn Mini Sudoku uses 2x3 boxes (Witgrid’s 6x6) or another region scheme was not confirmed from LinkedIn’s own help.

## Which candidates share rules, board UI, or generators with Witgrid’s four (true reuse) vs which are a new genre?

### Takeaway
True reuse is almost entirely inside the Latin-square family (Mini Sudoku → 9x9 / Killer / Futoshiki / Skyscrapers / Calcudoku) and the cell-shade family (Nonogram → Fill-a-Pix / Nurikabe / Hitori) plus object-placement next to Crowns (Tents, Battleship). Loops, bridges, paths, arrows-escape, merge/sort, solitaire, word, and domino-placement need new engines and new gestures.

### Cited Findings

**Witgrid launch rules (for the reuse map)**

- Crowns: one crown per row, column, and colour region; crowns never touch, including diagonally. Sizes 6x6–10x10. Input: tap cycle X / crown / clear. — local `DEVELOPMENT_PLAN.md` §6.1 (assignment context; not a web URL)
- Binary: two symbols; no three equal in a row/column; equal counts; unique rows and unique columns. — local plan §6.2
- Nonogram: row/column run-length clues, abstract patterns. — local plan §6.3
- Mini Sudoku 6x6 with digit pad and notes. — local plan §6.4

**Same-or-near rules (true reuse)**

- **Sudoku 9x9.** Same Latin-square + box constraint as Mini Sudoku, larger alphabet and 3x3 boxes. Conceptis and NYT both ship it. Engine reuse is the plan’s own note for wave 1. — [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile); [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games)
- **Killer Sudoku.** Same 9x9 Sudoku regions plus cages and sums. Digit pad, notes, and uniqueness solver extend the Mini Sudoku stack; cage overlay is extra UI. Conceptis SumSudoku is this type on Android. — [Wikipedia: Killer sudoku](https://en.wikipedia.org/wiki/Killer_sudoku); [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile)
- **Futoshiki.** Latin square plus inequality signs. Tatham Unequal. Digit pad reuse; inequality glyphs are extra board chrome. No boxes. — [Wikipedia: Futoshiki](https://en.wikipedia.org/wiki/Futoshiki)
- **Skyscrapers / Towers.** Latin square plus visibility counts on the edges. Digit pad reuse; clue band around the grid is new (similar layout problem to Nonogram clue bands). — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [logic-puzzles-online](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Calcudoku.** Latin square plus arithmetic cages. Same family as Killer but no 3x3 boxes; cages use four operations. Tatham Keen. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [KenKen generic-name note](https://ipfs.io/ipfs/QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco/wiki/KenKen.html)
- **Tic-Tac-Logic (Conceptis name for Binary).** “Fill remaining squares so there are no more than two adjacent X or O in a row or column.” This is the Binary/Takuzu rule set Conceptis already ships on Android. Generator/solver reuse with Witgrid Binary is direct if uniqueness-of-rows is also enforced (Conceptis blurb on the mobile tile does not restate the equal-count and unique-row rules; the home-page one-liner only states the pair-cap). — [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile); [Conceptis home](https://www.conceptispuzzles.com/)
- **Tatham Unruly.** “Fill in the black and white grid to avoid runs of three.” Pair-cap is shared with Binary; equal-count / unique-row rules are not stated on the Tatham one-liner. Partial rule overlap, not a drop-in. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **Fill-a-Pix / Mosaic.** Cell paint from numeric clues, Fill/Cross gestures like Nonogram, but clues are neighbour counts (Minesweeper-like) not run lengths. Picture result is extra art, not required. — [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile); [Tatham Mosaic](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **Nurikabe, Hitori, Tapa, Heyawake, LITS.** All are binary cell-shade puzzles. Nonogram Fill/Cross UI transfers; solvers and generators do not. Nurikabe adds connectivity and no-2x2; Hitori starts from a filled number grid. — [Wikipedia: Nurikabe](https://en.wikipedia.org/wiki/Nurikabe_(puzzle)); [logic-puzzles-online](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Tents.** Object placement with no-touch (including diagonal, like Crowns) plus row/column counts and a 1-1 pairing with trees. Crowns tap-cycle (X / object / clear) and auto-X-around-object transfer. Regions are absent; trees are extra givens. — [Tatham Tents](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/); [logic-puzzles-online](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Battleship logic.** Object placement with row/column counts and no-touch, like Tents/Crowns, but ships have length and orientation. 10x10 standard fleet. Crowns-style marks (water / ship / unknown) transfer; generator is a different placement problem. — [Wikipedia: Battleship (puzzle)](https://en.wikipedia.org/wiki/Battleship_(puzzle))
- **Star Battle (2-star).** Same family as Crowns; Crowns is the 1-star no-diagonal-touch variant. Increasing star count is a ruleset knob, not a new genre. — [logic-puzzles-online Star Battle](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)
- **Shikaku / LinkedIn Patches.** Region division by area. Not a fill-digit or shade-cell puzzle; closest Witgrid analogue is Crowns *regions* as display, not as the solve action. Drag-to-draw a rectangle is a new gesture (Patches: “draw a patch by dragging”). Generator is rectangle packing, not Crowns flood-fill. — [Patches how-to](https://axeetech.com/puzzles/how-to-play-linkedin-patches/); [Tatham Rectangles](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **Minesweeper no-guess.** Neighbour-count clues like Fill-a-Pix, but the player reveals cells and can lose. Fill-a-Pix is the picture, unique-solution cousin. Partial overlap with Nonogram paint UI only if designed as paint-not-reveal. — [Conceptis Fill-a-Pix](https://www.conceptispuzzles.com/index.aspx?uri=mobile); [Tatham Mines](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)

**New genre (new rules, new board chrome, new generator)**

- **Slitherlink / Masyu / Yajilin.** Edge-or-vertex drawing, not cell fill. New hit-testing (edges vs cells). Tatham Loopy even uses non-square tilings. — [Wikipedia: Slitherlink](https://en.wikipedia.org/wiki/Slitherlink); [Tatham Loopy](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **Hashi / Bridges.** Islands in open space, 0–2 parallel bridges, connectivity of a planar graph. Not a cell-fill grid. Conceptis and Tatham both ship it. — [Wikipedia: Hashiwokakero](https://en.wikipedia.org/wiki/Hashiwokakero)
- **Numberlink / Path / LinkedIn Zip.** Path drawing through cells, non-crossing, often Hamiltonian. LinkedIn Zip adds walls and ordered waypoints. Plan already lists this as wave 3. Flow Free is the mass-market form. — [Wikipedia: Numberlink](https://en.wikipedia.org/wiki/Numberlink); [LinkedIn Help: Zip](https://www.linkedin.com/help/linkedin/answer/a7445030)
- **Akari / Light Up.** Placement of bulbs with line-of-sight lighting. Related to object placement but lighting simulation is new. — [Wikipedia: Light Up](https://en.wikipedia.org/wiki/Light_Up_(puzzle))
- **Arrows escape.** Tap-to-remove in order; hearts; not a unique-fill bank. New genre vs all four launch games. — [GridPaw](https://gridpaw.com/arrows-puzzle/)
- **Pips / Dominosa.** Place physical domino tiles on a board. NYT’s 2025 original logic type. New piece-dragging UI. — [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games); [Tatham Dominosa](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)
- **Colour sort.** Tube stacking. Plan wave 2. Not paper-logic.
- **Number merge / Sumbreak.** Merge-equal-neighbours. LinkedIn Sumbreak 18 Nov 2026. Plan already lists number merge and bans the name 2048. — [AWA Sumbreak](https://awa.games/linkedin-games/sumbreak/howto/)
- **Block puzzle 8x8 (place shapes, clear lines).** Plan wave 1. Closer to falling-block / Block Blast than to Crowns. Name Block Blast is banned.
- **Klondike solitaire.** Cards. Plan wave 3. Needs code-drawn cards.
- **Word search / Wend / Pinpoint / Crossclimb / Strands.** Letter/word, not number-logic. Plan wave 4 for word search; five-letter guess is banned.
- **Galaxies, Fillomino, Country Road, Signpost, Tracks, Slant, Magnets, Map, Untangle, Net.** Tatham/Nikoli types with distinct geometry (symmetry cuts, polyomino growth, arrow-followed Hamiltonian path, railway, diagonal slashes, magnets, four-colouring, planar embedding, tile rotation). Each is a new engine. — [Tatham collection](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/)

### Inferences
- Cheapest post-launch logic additions that stay inside Witgrid’s existing UI grammar (tap cell, digit pad, Fill/Cross, X-marks): Sudoku 9x9, Killer, Futoshiki, Calcudoku, Skyscrapers, Tents, Battleship, Fill-a-Pix, Nurikabe, Hitori.
- Medium cost, still “paper logic,” new gestures: Shikaku/Patches (drag rectangles), Hashi (draw bridges), Slitherlink (draw edges), Numberlink/Path/Zip (draw paths), Akari (place + light sim).
- High cost / off-brand for a no-guess unique bank: arrows-escape (hearts, order puzzle), colour sort, number merge/Sumbreak, block-blast, Klondike, word games, Pips if it needs free-form domino dragging and NYT-like region predicates.

### Gaps
- No code-level comparison of Witgrid’s Crowns/Binary/Nonogram/Mini Sudoku generators was done in this research pass; reuse claims are rule-and-UI, not a measured “shared solver techniques” audit.
- Whether Conceptis Tic-Tac-Logic enforces Binary’s equal-count and unique-row rules is not confirmed from the one-line store blurbs.
- Thermometers, Easy as ABC, Ripple Effect, Suguru: likely digit-pad reuse, but primary rule pages and generator notes were not fetched.

## Trademark / clone risk and generic names Witgrid can use

### Takeaway
Use the generic English names already in the Witgrid plan (Bridges, Tents, Calcudoku, Killer, Path, Nonogram, Binary, Crowns, Mini Sudoku). Do not use KenKen, Picross, Flow Free, LinkedIn product names, or Conceptis coined *-a-Pix* marks. Nikoli Japanese titles (Hashiwokakero, Nurikabe, Slitherlink, Masyu, Akari, Shikaku, Hitori) are widely used as type names in English Wikipedia and catalogs; still prefer a plain English descriptor in store text.

### Cited Findings

**Registered or explicitly trademarked names**

- **PICROSS** is a live US registration (Reg. 6077332, filed 18 Oct 2019, registered 16 Jun 2020) owned by Nintendo of America Inc., covering recorded/downloadable game software and related entertainment services. **PICROSS 3D** is also registered (Reg. 4218237). — [Justia: PICROSS 79278074](https://trademarks.justia.com/792/78/picross-79278074.html); [Justia: PICROSS 3D](https://trademarks.justia.com/779/43/picross-77943337.html)
- **KENKEN** is a registered mark. US Trademarkia record: Reg. 3551731, owned by KenKen Puzzle LLC (formerly Nextoy, LLC), filed 1 Jun 2007, registered 23 Dec 2008, covering books and downloadable games/puzzles. Canadian TMA756398 is live, owner KenKen Puzzle LLC, expiry 5 Jan 2035. Wikipedia: “The names Calcudoku and Mathdoku are sometimes used by those who don’t have the rights to use the KenKen or KenDoku trademarks.” — [Trademarkia KENKEN](https://www.trademarkia.com/kenken-77976074); [CIPO KENKEN 1404243](https://ised-isde.canada.ca/cipo/trademark-search/1404243); [KenKen Wikipedia snapshot](https://ipfs.io/ipfs/QmXoypizjW3WknFiJnKLwHCnL72vedxjQkDDP1mXWo6uco/wiki/KenKen.html)
- Witgrid’s own banned-name list already includes Queens, Tango, Zip, LinkedIn, NYT, Takuzu, Binairo, Picross, KenKen, Tetris, Block Blast, 2048, Flow Free, Rush Hour, Wordle. — local `DEVELOPMENT_PLAN.md` §12 (assignment constraint)

**Product names that are not generic type names**

- **Flow Free** is the Big Duck Games mobile product implementing Numberlink (`com.bigduckgames.flow` on Google Play, App Store listing cited by Wikipedia). Using “Flow Free” in copy is on Witgrid’s ban list. Generic: Number link, Paths, Letter link. — [Wikipedia: Numberlink](https://en.wikipedia.org/wiki/Numberlink)
- **Tic-Tac-Logic**, **Pic-a-Pix**, **Fill-a-Pix**, **Link-a-Pix**, **Cross-a-Pix**, **Sym-a-Pix**, **Block-a-Pix**, **Dot-a-Pix**, **SeaBattle**, **SumSudoku**, **Pix-o-Mania** are Conceptis product titles on the mobile index. Treat as their brands, not generic type names. Generic stand-ins: Binary, Nonogram, Mosaic, Numberlink-pictures, Battleships, Killer Sudoku. — [Conceptis Mobile](https://www.conceptispuzzles.com/index.aspx?uri=mobile)
- **Arrows – Puzzle Escape** is a Lessmore GmbH product; fan guides mark themselves unofficial. Generic: Arrows, Arrow escape. Clone apps already crowd Play with “Arrow Escape” titles. — [GridPaw](https://gridpaw.com/arrows-puzzle/); [Fantagiochi](https://www.fantagiochi.it/arrows-puzzle-escape-soluzioni/)
- **Pips** is NYT’s original 2025 logic game name. Generic: Dominoes, Domino logic. — [Wikipedia: NYT Games](https://en.wikipedia.org/wiki/The_New_York_Times_Games)
- **Patches**, **Wend**, **Sumbreak**, **Pinpoint**, **Crossclimb** are LinkedIn game names as of 2026. Generic: Rectangles, Word paths, Number merge, Category guess, Word ladder. — [AWA launch dates](https://awa.games/linkedin-games/resources/new-linkedin-game/)

**Names that catalogs already treat as generic English type names**

- Hashiwokakero is published in English as Bridges (and sometimes Hashi, Chopsticks, Ai-Ki-Ai). Wikipedia states Chopsticks is a mistranslation. **Bridges** is the Tatham and Conceptis English name and is already in Witgrid’s wave-4 table. — [Wikipedia: Hashiwokakero](https://en.wikipedia.org/wiki/Hashiwokakero)
- Slitherlink English aliases listed by Wikipedia: Fences, Takegaki, Loop the Loop, Loopy, Ouroboros, Suriza, Rundweg, Tectonic, Dotty Dilemma. Tatham uses Loopy. Store-safe generic: Loop, Fences. — [Wikipedia: Slitherlink](https://en.wikipedia.org/wiki/Slitherlink)
- Light Up / Akari: Nikoli English page title Light Up; Japanese 美術館 / 明かり. Tatham: Light Up. Generic: Lamps, Lights. — [Wikipedia: Light Up](https://en.wikipedia.org/wiki/Light_Up_(puzzle))
- Nurikabe also called Cell Structure, Islands in the Stream. Generic: Islands, Walls. — [Wikipedia: Nurikabe](https://en.wikipedia.org/wiki/Nurikabe_(puzzle))
- Kakuro also Cross Sums, Kakro. Wikipedia uses Kakuro as the headword. — [Wikipedia: Kakuro](https://en.wikipedia.org/wiki/Kakuro)
- Battleship puzzle also Bimaru, Yubotu, Solitaire Battleships. Conceptis app SeaBattle. Generic: Ships, Fleet. Hasbro’s *Battleship* is a separate guessing-game trademark not checked in this pass. — [Wikipedia: Battleship (puzzle)](https://en.wikipedia.org/wiki/Battleship_(puzzle))
- Futoshiki also More or Less. Tatham Unequal. Generic: Inequalities. — [Wikipedia: Futoshiki](https://en.wikipedia.org/wiki/Futoshiki)
- Shikaku: Divide by Squares. Tatham Rectangles. LinkedIn Patches is the branded daily form. Generic: Rectangles. — [Wikipedia Nikoli list](https://en.wikipedia.org/wiki/Nikoli_(publisher))
- Killer sudoku aliases: sumdoku, samunamupure. Generic: Killer, Cage sudoku, Sum sudoku — but “SumSudoku” is Conceptis’ app name. — [Wikipedia: Killer sudoku](https://en.wikipedia.org/wiki/Killer_sudoku)
- Star Battle is used as a generic type name on solver sites; Witgrid already chose **Crowns** to avoid Queens. Plan item: “[Verify] ‘star battle’ as a free term.” This pass did not run a trademark-office search on “Star Battle.” — [logic-puzzles-online](https://logic-puzzles-online.com/blog/japanese-logic-puzzles/)

**Suggested generic names (research inference, tied to sources above)**

| Type | Avoid | Use |
|---|---|---|
| Star Battle 1-star | Queens, LinkedIn | Crowns (already shipping) |
| Takuzu | Tango, Takuzu, Binairo, Tic-Tac-Logic | Binary (already shipping) |
| Nonogram | Picross, Pic-a-Pix | Nonogram (already shipping) |
| 6x6 Sudoku | — | Mini Sudoku (already shipping) |
| 9x9 Sudoku | — | Sudoku |
| Killer | SumSudoku (Conceptis) | Killer, Cages |
| Calcudoku | KenKen, KenDoku | Calcudoku, Math cages |
| Futoshiki | — | Inequalities, More or less |
| Skyscrapers | — | Towers, Skyscrapers |
| Hashiwokakero | — | Bridges |
| Slitherlink | — | Loop, Fences |
| Nurikabe | — | Islands |
| Akari | — | Lamps |
| Shikaku / Patches | Patches (LinkedIn) | Rectangles |
| Numberlink / Zip | Zip, Flow Free | Path, Number link |
| Tents | — | Tents |
| Battleship logic | SeaBattle, possibly Battleship | Ships, Fleet |
| Fill-a-Pix | Fill-a-Pix | Mosaic |
| Hitori | — | Singles, Shade duplicates |
| Masyu | — | Pearls |
| Arrows escape | Arrows – Puzzle Escape | Arrows |
| Pips | Pips (NYT) | Dominoes |
| Number merge | 2048, Sumbreak | Merge (plan already lists Number merge) |

### Inferences
- Clone risk is highest for LinkedIn-identical rules under LinkedIn names (already banned), for Flow Free-style Numberlink marketing, and for arrows-escape clones that copy Lessmore’s winding-path look. Shipping Path, Rectangles, or Arrows under generic names with Witgrid’s paper-and-ink art is the same pattern Crowns/Binary already use.
- KenKen and Picross are the two marks with clear live registrations found in this pass. Calcudoku and Nonogram are the documented safe generics.
- Hasbro *Battleship* and Nintendo *Picross* are the two toy/game companies most likely to notice a store listing that uses those words. Prefer Fleet/Ships and Nonogram.

### Gaps
- No USPTO/EUIPO search was run for Hashi, Akari, Nurikabe, Slitherlink, Masyu, Shikaku, Star Battle, Tic-Tac-Logic, Fill-a-Pix, SeaBattle, Pips, Patches, Sumbreak, or Arrows Puzzle Escape.
- No search was run for Hasbro’s Battleship mark as it applies to a logic-puzzle app.
- Flow Free’s registration status was not pulled; only the product-and-ban-list evidence is cited.
- “Star Battle” as a free term remains unverified at a trademark office, matching the plan’s own open verify item.
