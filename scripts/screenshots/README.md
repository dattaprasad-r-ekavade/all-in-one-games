# Local screenshot gate

Run from PowerShell with a booted emulator and `ANDROID_HOME` set:

```powershell
.\scripts\screenshots\capture.ps1 -Serial emulator-5554
```

The script builds/installs the debug app and capture test once, then captures 12 screens in each of 12 configurations: 360dp and 480dp, light and dark, system font scales 1.0, 1.3 and 2.0. Both profiles use 1080×1920 pixels; density is 480 or 360 respectively. The test checks the activity's actual width and system font scale. Font scaling uses Android settings, including Android's nonlinear large-text behavior, rather than a Compose density override.

Local, ignored `build/u8/` contains:

- `360dp/` and `480dp/`: 144 PNGs total, grouped by theme and font scale, with per-run instrumentation results.
- `manifest.json`: the complete expected matrix.
- `contact-sheet-360dp.png` and `contact-sheet-480dp.png`: six labeled configuration columns and twelve screen rows.
- `offline_message.png`: the retained offline billing example outside the matrix.

The tracked `app/src/androidTest/screenshots/` store candidate set is refreshed only after the full matrix succeeds: the twelve light/1.0/360dp captures, a dark Home capture (`dark_mode.png`), and the existing offline example. The larger set gives the owner choices; selecting store images and uploading them remain separate actions.

Screens use deterministic representative Easy data, production composables and navigation frames, and idle callbacks. Captures are top-of-screen views; content below the viewport remains scrollable. The capture harness does not request ads or purchases. Airplane mode is enabled while capturing. Size, density, font scale, airplane mode and the process's `ANDROID_SERIAL` are restored independently in `finally`, including after a failed capture. Existing display overrides and an unset font setting are preserved.

Reruns overwrite the same expected files. Instrumentation deletes its named MediaStore captures before each configuration. Missing files, test failures, wrong dimensions and incomplete matrix counts fail the run. Contact sheets may also be regenerated with:

```powershell
.\scripts\screenshots\contact-sheets.ps1 -Directory .\build\u8
```

The lead must inspect both sheets and representative full-size images before every closed-test upload. Pay particular attention to 360dp at font scale 2.0, board fit, scrolling, and bottom tools. Script success verifies capture completeness, not visual approval. No upload or publishing is performed.
