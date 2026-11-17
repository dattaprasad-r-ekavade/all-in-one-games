# U4 play layout review

`PlayLayoutMatrixTest.everyGameFitsPhoneSizesThemesAndFontScales` renders the actual Expert-pack boards at 360 x 780 dp and 432 x 936 dp, in light and dark themes at font scales 1.0 and 1.3. It checks the square board bounds, visible toolbar and Sudoku pad, and text layout overflow (including Nonogram clues). The fixture uses numbered attempt 613 through `LevelRef.forLevel`, so the displayed number keeps increasing past a bank loop.

The four contact sheets group the 32 captures by width and font scale. Each sheet has light and dark rows and Crowns, Binary, Nonogram, Mini Sudoku columns. These review the U4 layout; U5 owns the board artwork.

Additional instrumentation covers timer visibility, rules, restart, Mini Sudoku editing/notes through the play route, the full intro coach at font scales 1.0 and 2.0, and enlarged 48 dp cells with panning.

To recapture, build the debug and AndroidTest APKs, install them, run the matrix class with AndroidJUnitRunner, then export `cache/u4-play` with `adb exec-out run-as com.datathecodie.witgrid tar -cf - -C cache u4-play`. `make_sheets.py` performs the export and builds the sheets. It requires Pillow on the developer PC, with no application dependency.

Capture setup uses a 1080 x 2400 emulator canvas with density 160; the test constrains each screen to its declared dp dimensions. Restore the original density after capture. The successful run used API 35; screenshots are rendered at one pixel per dp.
