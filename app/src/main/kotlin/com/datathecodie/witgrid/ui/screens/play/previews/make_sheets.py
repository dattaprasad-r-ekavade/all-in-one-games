"""Export only U4 test captures and make review contact sheets."""
import io
import os
from pathlib import Path
import subprocess
import tarfile
from PIL import Image, ImageDraw, ImageFont

repository = next(parent for parent in Path(__file__).resolve().parents if (parent / "gradlew.bat").is_file())
raw = repository / "app/build/reports/u4-play"
raw.mkdir(parents=True, exist_ok=True)
adb = Path(os.environ["ANDROID_HOME"]) / "platform-tools/adb.exe"
archive = subprocess.run([str(adb), "exec-out", "run-as", "com.datathecodie.witgrid", "tar", "-cf", "-", "-C", "cache", "u4-play"], check=True, capture_output=True).stdout
with tarfile.open(fileobj=io.BytesIO(archive)) as captures:
    for member in captures.getmembers():
        if member.isfile() and Path(member.name).suffix == ".png":
            destination = raw / Path(member.name).name
            destination.write_bytes(captures.extractfile(member).read())
font = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 18)
for width, height in [(360, 780), (432, 936)]:
    for scale in ["1.0", "1.3"]:
        gap, heading = 16, 40
        sheet = Image.new("RGB", (width * 4 + gap * 5, (height + heading) * 2 + gap * 3), "#dedbd4")
        draw = ImageDraw.Draw(sheet)
        for row, theme in enumerate(["light", "dark"]):
            for column, game in enumerate(["crowns", "binary", "nonogram", "minisudoku"]):
                capture = Image.open(raw / f"{game}-{width}-{theme}-{scale}.png").convert("RGB")
                x, y = gap + column * (width + gap), gap + row * (height + heading + gap)
                draw.text((x, y), f"{game} | {theme} | {width}dp | {scale}", fill="#242321", font=font)
                sheet.paste(capture, (x, y + heading))
        sheet.save(Path(__file__).parent / f"play-{width}-{scale}.png")
print(f"Exported {len(list(raw.glob('*.png')))} captures to {raw}")
