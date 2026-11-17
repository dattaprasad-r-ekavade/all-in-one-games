"""Generate the Witgrid launcher icon. Run: python scripts/icon/make_icon.py <repo> <out-dir>. Writes res/ icons; out-dir gets the Play 512 PNG and a preview."""
import os
import sys
from PIL import Image, ImageDraw

REPO = sys.argv[1]
OUT = sys.argv[2]
RES = os.path.join(REPO, "app", "src", "main", "res")

PAPER = (0xF6, 0xF1, 0xE7)
INK = (0x1C, 0x1B, 0x19)
ACCENT = (0x3B, 0x4F, 0xA0)
LINE = (0xC9, 0xC2, 0xB3)

# Pixel W on a 5x5 grid. 1 = ink, 2 = accent.
PATTERN = [
    "1...1",
    "1...1",
    "1.2.1",
    "1.2.1",
    ".1.1.",
]

# Geometry in dp on the 108 dp adaptive canvas. The grid fits inside the 66 dp safe circle.
N = 5
GRID = 45.0
ORIGIN = (108 - GRID) / 2
CELL = GRID / N
GAP = 1.1
RADIUS = 1.6
FRAME = 2.0
INNER = 0.8
FRAME_RADIUS = 3.0
FRAME_PAD = 1.8


def cells():
    for r, row in enumerate(PATTERN):
        for c, ch in enumerate(row):
            if ch != ".":
                yield r, c, ch


def cell_rect(r, c):
    x = ORIGIN + c * CELL + GAP
    y = ORIGIN + r * CELL + GAP
    return x, y, x + CELL - 2 * GAP, y + CELL - 2 * GAP


def hexc(rgb):
    return "#FF%02X%02X%02X" % rgb


def rrect_path(x0, y0, x1, y1, rad):
    return (f"M{x0 + rad:.2f},{y0:.2f} H{x1 - rad:.2f} A{rad},{rad} 0 0 1 {x1:.2f},{y0 + rad:.2f} "
            f"V{y1 - rad:.2f} A{rad},{rad} 0 0 1 {x1 - rad:.2f},{y1:.2f} H{x0 + rad:.2f} "
            f"A{rad},{rad} 0 0 1 {x0:.2f},{y1 - rad:.2f} V{y0 + rad:.2f} A{rad},{rad} 0 0 1 {x0 + rad:.2f},{y0:.2f} Z")


def vector(include_lines, ink_hex, accent_hex):
    parts = []
    if include_lines:
        d = []
        for i in range(1, N):
            p = ORIGIN + i * CELL
            d.append(f"M{p:.2f},{ORIGIN:.2f} V{ORIGIN + GRID:.2f}")
            d.append(f"M{ORIGIN:.2f},{p:.2f} H{ORIGIN + GRID:.2f}")
        parts.append(f'    <path android:strokeColor="{hexc(LINE)}" android:strokeWidth="{INNER}" '
                     f'android:pathData="{" ".join(d)}" />')
    ink = [rrect_path(*cell_rect(r, c), RADIUS) for r, c, ch in cells() if ch == "1"]
    acc = [rrect_path(*cell_rect(r, c), RADIUS) for r, c, ch in cells() if ch == "2"]
    parts.append(f'    <path android:fillColor="{ink_hex}" android:pathData="{" ".join(ink)}" />')
    parts.append(f'    <path android:fillColor="{accent_hex}" android:pathData="{" ".join(acc)}" />')
    h = FRAME / 2 + FRAME_PAD
    parts.append(f'    <path android:strokeColor="{ink_hex}" android:strokeWidth="{FRAME}" android:fillColor="#00000000" '
                 f'android:pathData="{rrect_path(ORIGIN - h, ORIGIN - h, ORIGIN + GRID + h, ORIGIN + GRID + h, FRAME_RADIUS + FRAME_PAD)}" />')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n'
            + "\n".join(parts) + "\n</vector>\n")


def render(px, view_size, shape):
    """Render at px pixels showing the centre view_size dp of the 108 dp canvas."""
    ss = 4
    size = px * ss
    s = size / view_size
    off = (108 - view_size) / 2

    def m(v):
        return (v - off) * s

    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if shape == "square":
        d.rectangle([0, 0, size, size], fill=PAPER)
    elif shape == "rounded":
        pad = size * 2 / 48
        d.rounded_rectangle([pad, pad, size - pad, size - pad], radius=size * 8 / 48, fill=PAPER)
    elif shape == "circle":
        pad = size * 2 / 48
        d.ellipse([pad, pad, size - pad, size - pad], fill=PAPER)
    lw = max(1, round(INNER * s))
    for i in range(1, N):
        p = m(ORIGIN + i * CELL)
        d.line([p, m(ORIGIN), p, m(ORIGIN + GRID)], fill=LINE, width=lw)
        d.line([m(ORIGIN), p, m(ORIGIN + GRID), p], fill=LINE, width=lw)
    for r, c, ch in cells():
        x0, y0, x1, y1 = cell_rect(r, c)
        d.rounded_rectangle([m(x0), m(y0), m(x1), m(y1)], radius=RADIUS * s, fill=INK if ch == "1" else ACCENT)
    e = FRAME + FRAME_PAD
    d.rounded_rectangle([m(ORIGIN - e), m(ORIGIN - e), m(ORIGIN + GRID + e), m(ORIGIN + GRID + e)],
                        radius=(FRAME_RADIUS + FRAME_PAD + FRAME / 2) * s, outline=INK, width=round(FRAME * s))
    return img.resize((px, px), Image.LANCZOS)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


write(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"), vector(True, hexc(INK), hexc(ACCENT)))
write(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), vector(False, "#FF000000", "#FF000000"))
write(os.path.join(RES, "values", "ic_launcher_background.xml"),
      '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
      f'    <color name="ic_launcher_background">{hexc(PAPER)}</color>\n</resources>\n')
adaptive = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@color/ic_launcher_background" />\n'
            '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
            '</adaptive-icon>\n')
write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher.xml"), adaptive)
write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher_round.xml"), adaptive)

for name, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    folder = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(folder, exist_ok=True)
    render(px, 72, "rounded").save(os.path.join(folder, "ic_launcher.png"))
    render(px, 72, "circle").save(os.path.join(folder, "ic_launcher_round.png"))

os.makedirs(OUT, exist_ok=True)
play = render(512, 72, "square").convert("RGB")
play.save(os.path.join(OUT, "play-icon-512.png"))

# Preview: Play icon, circle mask, squircle mask, small size, dark launcher background.
prev = Image.new("RGB", (1240, 360), (40, 40, 44))
prev.paste(render(300, 72, "square"), (20, 30))
mask = Image.new("L", (300, 300), 0)
ImageDraw.Draw(mask).ellipse([0, 0, 300, 300], fill=255)
prev.paste(render(300, 72, "square"), (340, 30), mask)
sq = Image.new("L", (300, 300), 0)
ImageDraw.Draw(sq).rounded_rectangle([0, 0, 300, 300], radius=80, fill=255)
prev.paste(render(300, 72, "square"), (660, 30), sq)
small = render(48, 72, "circle")
prev.paste(small, (1000, 150), small)
small96 = render(96, 72, "rounded")
prev.paste(small96, (1080, 126), small96)
prev.save(os.path.join(OUT, "icon-preview.png"))
print("ok")
