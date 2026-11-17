# U3 shared components

The components use `WitgridTheme`, Inter typography, the approved paper and ink
palette, 18 dp cards with 1 dp borders, and 14 dp buttons. Text grows naturally;
primary buttons have a 54 dp minimum height and other interactive controls have
at least 48 dp targets. Callers supply localized labels and drawable painters.

`ListRow` supports a subtitle, trailing value, navigation chevron, or a switch.
Supply both `checked` and `onCheckedChange` for a switch; the whole row is one
accessible toggle target. A row has one action. `SegmentedControl` takes labels,
a selected index, and a selection callback. It wraps options into rows at larger
font scales, keeping options within each row the same height. `ToolBarItem`
supports enabled, selected, and badge states. A growing badge reserves space
beside its icon. `Chip` supports quiet status, accent status, and an optional tap.

Material surface containers use paper/card tones. `outlineVariant` uses the card
edge for quiet borders; `outline` uses muted ink because Material also uses that
role for unchecked switch thumbs. Light `accentSoft` is `#E5E8F5`; dark uses the
selected accent at 18% composited over black. The top bar uses
`IconActionButton`. Screen adoption of the other components belongs to U4–U7.

## Visual review

Every component has light and dark Studio previews at font scales 1.0, 1.3, and
2.0. `ComponentGalleryTest` renders those same samples at 360 dp, verifies switch
and segment interactions and the primary button target, and exports 60 captures
plus paired contact sheets. On Android 10+, sheets are published through
MediaStore so they survive the test installer's app cleanup:

`/sdcard/Pictures/WitgridU3Previews/components-*.png`

The `previews/components-*.png` sheets are the artifacts for lead review. The
samples reuse existing localized labels; icons illustrate component states with
the vector drawables available from U2.

Captured on an Android 15 emulator on 17 Nov 2026: all three component tests
passed. Both JVM palette tests also pass with one Gradle worker. Lead screenshot
review passed on 17 Nov 2026 for both themes at font scales 1.0, 1.3, and 2.0;
the sheets show no clipping and match the approved palette and card style.
`:app:lintDebug` passes after removing the redundant test API annotation.

```powershell
.\gradlew.bat check --no-daemon -Pkotlin.compiler.execution.strategy=in-process
.\gradlew.bat :app:testDebugUnitTest --tests com.datathecodie.witgrid.ui.theme.MaterialPaletteTest `
  :app:lintDebug --no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon `
  -Pkotlin.compiler.execution.strategy=in-process `
  --init-script app/src/main/kotlin/com/datathecodie/witgrid/ui/components/previews/capture.init.gradle `
  -Pandroid.testInstrumentationRunnerArguments.class=com.datathecodie.witgrid.ui.components.ComponentGalleryTest
```

The capture init script uses a separate application ID for the test build to keep
the emulator's installed app and data intact when debug signatures differ.

For this sandbox, set these process-local environment values before building:

```powershell
$env:GRADLE_USER_HOME = 'C:\Users\ekava\.gradle'
$env:ANDROID_USER_HOME = "$PWD\app\build\android-user-home"
$env:JAVA_TOOL_OPTIONS = '-Duser.home=C:\Users\ekava\.gradle -Dmaven.repo.local=C:\Users\ekava\.m2\repository'
```

The cache paths avoid writes under `C:\`. The builder sandbox initially hit a
Windows native runtime crash in existing Robolectric tests (`Unable to find
static field mNativePtr`, exit `0xC0000409`), including with
`-Drobolectric.graphicsMode=LEGACY`. The lead reran the required `check` from the
workspace PowerShell on 17 Nov 2026; it passed without excluding tests and
validated all 3,864 puzzles in 24 bank files.

The builder sandbox could not stage or commit because the shared Git metadata
directory had a write-deny ACL. The lead committed the reviewed changes from the
workspace after the required gate passed.
