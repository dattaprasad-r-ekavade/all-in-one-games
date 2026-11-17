"""Render a 1024x500 Play feature-graphic draft using the app's paper-and-ink palette."""

from functools import lru_cache
import gzip
import os

from PIL import Image, ImageDraw, ImageFont


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUTPUT = os.path.join(ROOT, "docs", "store", "art", "feature-graphic-draft.png")

WIDTH = 1024
HEIGHT = 500
SCALE = 2

PAPER = "#F6F1E7"
CARD = "#FFFCF7"
INK = "#1C1B19"
MUTED = "#5F5A52"
ACCENT = "#3B4FA0"
PALE_ACCENT = "#E8EBF7"
LINE = "#C9C2B3"


@lru_cache(maxsize=None)
def font(size, bold=False):
    windows_fonts = os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts")
    candidates = (
        os.path.join(windows_fonts, "segoeuib.ttf" if bold else "segoeui.ttf"),
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
        if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf"
        if bold
        else "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
        "/Library/Fonts/Arial Bold.ttf" if bold else "/Library/Fonts/Arial.ttf",
    )
    for candidate in candidates:
        if os.path.isfile(candidate):
            return ImageFont.truetype(candidate, size * SCALE)
    raise FileNotFoundError("Install Segoe UI, DejaVu Sans or Liberation Sans to render the graphic")


def box(draw, bounds, radius, fill, outline=None, width=1):
    scaled = tuple(round(value * SCALE) for value in bounds)
    draw.rounded_rectangle(
        scaled,
        radius=radius * SCALE,
        fill=fill,
        outline=outline,
        width=width * SCALE,
    )


def text(draw, position, value, size, color, bold=False, anchor=None):
    x, y = (round(value * SCALE) for value in position)
    draw.text(
        (x, y),
        value,
        font=font(size, bold),
        fill=color,
        anchor=anchor,
    )


def draw_brand_mark(draw, x, y, size):
    cell = size / 5
    pattern = ("1...1", "1...1", "1.2.1", "1.2.1", ".1.1.")
    box(draw, (x, y, x + size, y + size), 12, CARD, INK, 3)
    for row, line in enumerate(pattern):
        for column, value in enumerate(line):
            left = x + column * cell + 5
            top = y + row * cell + 5
            right = x + (column + 1) * cell - 5
            bottom = y + (row + 1) * cell - 5
            bounds = tuple(round(item * SCALE) for item in (left, top, right, bottom))
            color = INK if value == "1" else ACCENT
            if value != ".":
                draw.rounded_rectangle(bounds, radius=4 * SCALE, fill=color)


def draw_grid(draw, x, y, size, count):
    cell = size / count
    box(draw, (x, y, x + size, y + size), 6, CARD, INK, 2)
    for index in range(1, count):
        position = round((x + index * cell) * SCALE)
        draw.line(
            (position, round(y * SCALE), position, round((y + size) * SCALE)),
            fill=LINE,
            width=SCALE,
        )
        position = round((y + index * cell) * SCALE)
        draw.line(
            (round(x * SCALE), position, round((x + size) * SCALE), position),
            fill=LINE,
            width=SCALE,
        )


def draw_crowns(draw, x, y, size, puzzle):
    count = puzzle["size"]
    cell = size / count
    regions = puzzle["payload"]
    solution = puzzle["solution"]
    draw_grid(draw, x, y, size, count)

    for row in range(count):
        for column in range(count - 1):
            left = row * count + column
            if regions[left] != regions[left + 1]:
                boundary = round((x + (column + 1) * cell) * SCALE)
                draw.line(
                    (
                        boundary,
                        round((y + row * cell) * SCALE),
                        boundary,
                        round((y + (row + 1) * cell) * SCALE),
                    ),
                    fill=INK,
                    width=3 * SCALE,
                )
    for row in range(count - 1):
        for column in range(count):
            top = row * count + column
            if regions[top] != regions[top + count]:
                boundary = round((y + (row + 1) * cell) * SCALE)
                draw.line(
                    (
                        round((x + column * cell) * SCALE),
                        boundary,
                        round((x + (column + 1) * cell) * SCALE),
                        boundary,
                    ),
                    fill=INK,
                    width=3 * SCALE,
                )

    for row, column_value in enumerate(solution):
        column = int(column_value)
        center = (x + (column + 0.5) * cell, y + (row + 0.5) * cell)
        radius = 6
        bounds = tuple(
            round(value * SCALE)
            for value in (
                center[0] - radius,
                center[1] - radius,
                center[0] + radius,
                center[1] + radius,
            )
        )
        draw.ellipse(bounds, fill=ACCENT)


def draw_binary(draw, x, y, size, puzzle):
    count = puzzle["size"]
    solution = puzzle["solution"]
    draw_grid(draw, x, y, size, count)
    cell = size / count
    for row in range(count):
        for column in range(count):
            center = (
                x + (column + 0.5) * cell,
                y + (row + 0.5) * cell,
            )
            index = row * count + column
            text(draw, center, solution[index], 19, ACCENT if solution[index] == "1" else INK, anchor="mm")


def decode_nonogram_clues(payload):
    rows, columns = payload.split("/")

    def decode_side(value):
        return [
            [] if line == "-" else [int(clue) for clue in line.split(",")]
            for line in value.split(";")
        ]

    return decode_side(rows), decode_side(columns)


def draw_nonogram(draw, x, y, size, puzzle):
    count = puzzle["size"]
    solution = puzzle["solution"]
    row_clues, column_clues = decode_nonogram_clues(puzzle["payload"])
    draw_grid(draw, x, y, size, count)
    cell = size / count
    for row in range(count):
        for column in range(count):
            if solution[row * count + column] == "1":
                pad = 2
                left = x + column * cell + pad
                top = y + row * cell + pad
                right = x + (column + 1) * cell - pad
                bottom = y + (row + 1) * cell - pad
                draw.rectangle(
                    tuple(round(item * SCALE) for item in (left, top, right, bottom)),
                    fill=INK,
                )
    for row, clues in enumerate(row_clues):
        for offset, clue in enumerate(reversed(clues)):
            text(
                draw,
                (x - 9 - offset * 13, y + (row + 0.5) * cell),
                str(clue),
                11,
                INK,
                bold=True,
                anchor="rm",
            )
    for column, clues in enumerate(column_clues):
        for offset, clue in enumerate(reversed(clues)):
            text(
                draw,
                (x + (column + 0.5) * cell, y - 11 - offset * 13),
                str(clue),
                11,
                INK,
                bold=True,
                anchor="mm",
            )


def draw_mini_sudoku(draw, x, y, size, puzzle):
    count = puzzle["size"]
    givens = puzzle["payload"]
    draw_grid(draw, x, y, size, count)
    cell = size / count
    for column in (3,):
        boundary = round((x + column * cell) * SCALE)
        draw.line(
            (boundary, round(y * SCALE), boundary, round((y + size) * SCALE)),
            fill=INK,
            width=3 * SCALE,
        )
    for row in (2, 4):
        boundary = round((y + row * cell) * SCALE)
        draw.line(
            (round(x * SCALE), boundary, round((x + size) * SCALE), boundary),
            fill=INK,
            width=3 * SCALE,
        )
    for index, value in enumerate(givens):
        if value != ".":
            row, column = divmod(index, count)
            center = (
                x + (column + 0.5) * cell,
                y + (row + 0.5) * cell,
            )
            text(draw, center, value, 16, ACCENT, anchor="mm")


def draw_game_card(draw, x, y, title, draw_board, puzzle):
    width = 202
    height = 198
    box(draw, (x, y, x + width, y + height), 18, CARD, LINE, 1)
    board_size = 128
    label_y = y + 162
    if title == "Nonogram":
        board_size = 110
        board_x = x + 60
        board_y = y + 42
        label_y = y + 174
    else:
        board_x = x + (width - board_size) / 2
        board_y = y + 13
    draw_board(draw, board_x, board_y, board_size, puzzle)
    text(draw, (x + width / 2, label_y), title, 25, INK, bold=True, anchor="mm")


def draw_pill(draw, x, y, width, label):
    box(draw, (x, y, x + width, y + 42), 21, PALE_ACCENT)
    center_x = x + 22
    center_y = y + 21
    radius = 5
    draw.ellipse(
        tuple(
            round(value * SCALE)
            for value in (
                center_x - radius,
                center_y - radius,
                center_x + radius,
                center_y + radius,
            )
        ),
        fill=ACCENT,
    )
    text(draw, (x + 38, y + 21), label, 17, INK, bold=True, anchor="lm")


def load_easy_puzzle(game):
    path = os.path.join(ROOT, "app", "src", "main", "assets", "bank", f"{game}-easy.txt.gz")
    with gzip.open(path, "rt", encoding="utf-8") as bank:
        fields = bank.readline().rstrip("\r\n").split("|")
    if len(fields) != 6:
        raise ValueError(f"Malformed first puzzle in {path}")
    puzzle = {
        "id": fields[0],
        "size": int(fields[1]),
        "payload": fields[2],
        "solution": fields[3],
    }
    solution_length = puzzle["size"] if game == "crowns" else puzzle["size"] * puzzle["size"]
    if len(puzzle["solution"]) != solution_length:
        raise ValueError(f"Unexpected solution dimensions in {path}")
    return puzzle


def main():
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    image = Image.new("RGB", (WIDTH * SCALE, HEIGHT * SCALE), PAPER)
    draw = ImageDraw.Draw(image)

    # A slim indigo rule echoes the app's selection colour without crowding the title.
    box(draw, (64, 64, 72, 440), 4, ACCENT)
    text(draw, (96, 70), "OFFLINE LOGIC PUZZLES", 18, ACCENT, bold=True)
    text(draw, (90, 116), "Witgrid", 78, INK, bold=True)
    draw_brand_mark(draw, 446, 104, 86)
    text(draw, (96, 224), "Four games. One quiet challenge.", 29, INK, bold=True)
    text(draw, (96, 274), "Play a quick round, or settle into a longer solve.", 21, MUTED)
    draw.line(
        (round(96 * SCALE), round(334 * SCALE), round(500 * SCALE), round(334 * SCALE)),
        fill=LINE,
        width=2 * SCALE,
    )
    draw_pill(draw, 96, 356, 164, "Works offline")
    draw_pill(draw, 274, 356, 150, "No account")

    draw_game_card(draw, 554, 38, "Crowns", draw_crowns, load_easy_puzzle("crowns"))
    draw_game_card(draw, 772, 38, "Binary", draw_binary, load_easy_puzzle("binary"))
    draw_game_card(draw, 554, 264, "Nonogram", draw_nonogram, load_easy_puzzle("nonogram"))
    draw_game_card(
        draw,
        772,
        264,
        "Mini Sudoku",
        draw_mini_sudoku,
        load_easy_puzzle("minisudoku"),
    )

    image = image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)
    image.save(OUTPUT, format="PNG", optimize=True)
    print(f"Wrote {WIDTH}x{HEIGHT} PNG: {OUTPUT}")


if __name__ == "__main__":
    main()
