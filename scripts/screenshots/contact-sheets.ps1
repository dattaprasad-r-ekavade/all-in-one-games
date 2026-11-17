param([Parameter(Mandatory = $true)][string]$Directory)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$manifest = Get-Content -LiteralPath (Join-Path $Directory 'manifest.json') -Raw | ConvertFrom-Json
if ($manifest.Count -ne 144) { throw 'Contact sheets require the complete 144-image matrix.' }
foreach ($width in @(360, 480)) {
    $entries = @($manifest | Where-Object { $_.widthDp -eq $width })
    if ($entries.Count -ne 72) { throw "Expected 72 images for ${width}dp." }
    # Six columns (theme/font combinations), twelve screen rows; labels remain readable at full size.
    $sheet = [System.Drawing.Bitmap]::new(1440, 12 * 464 + 64)
    $graphics = [System.Drawing.Graphics]::FromImage($sheet)
    $font = [System.Drawing.Font]::new('Segoe UI', 11)
    try {
        $graphics.Clear([System.Drawing.Color]::White)
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.DrawString("Witgrid | ${width}dp | actual system font scales | local visual QA", $font,
            [System.Drawing.Brushes]::Black, 12, 12)
        for ($index = 0; $index -lt $entries.Count; $index++) {
            $entry = $entries[$index]
            $column = [int][math]::Floor($index / 12)
            $row = $index % 12
            $x = $column * 240
            $y = 64 + $row * 464
            $image = [System.Drawing.Image]::FromFile((Join-Path $Directory $entry.file))
            try {
                if ($image.Width -ne 1080 -or $image.Height -ne 1920) { throw "Unexpected image dimensions: $($entry.file)" }
                $graphics.DrawImage($image, $x + 3, $y + 42, 234, 416)
            } finally { $image.Dispose() }
            $graphics.DrawString("$($entry.screen)`n$($entry.theme) | font $($entry.fontScale) | ${width}dp", $font,
                [System.Drawing.Brushes]::Black, $x + 3, $y)
        }
        $sheet.Save((Join-Path $Directory "contact-sheet-${width}dp.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $font.Dispose(); $graphics.Dispose(); $sheet.Dispose() }
}