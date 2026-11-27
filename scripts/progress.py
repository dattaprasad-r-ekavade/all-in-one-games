"""Rebuild the progress bars in DEVELOPMENT_PLAN.md from the status column in docs/TASKS.md.

Run from the repo root:  python scripts/progress.py

Each card counts one. A lane row such as "SK1–SK4" counts as four cards. Its status can be one
word for the whole lane ("done", "todo") or a list per card ("SK1 review, SK2 active (Gemini)");
cards that the list does not name count as todo.
"""
import datetime
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TASKS = ROOT / "docs" / "TASKS.md"
PLAN = ROOT / "DEVELOPMENT_PLAN.md"
START = "<!-- progress:start -->"
END = "<!-- progress:end -->"

DONE = {"done"}
IN_PROGRESS = {"active", "review"}
WIDTH = 20

GAMES = {
    # Wave 1
    "NB": "Nuts & Bolts", "MI": "Minesweeper", "MP": "Maze Paint", "NM": "Number Merge",
    "SL": "Sliding Puzzle", "ME": "Memory Pairs", "BE": "Block Escape", "YA": "Yatzy",
    "SH": "Shikaku", "SP": "Spider Solitaire", "FC": "FreeCell",
    # Wave 2
    "TT": "Tic-Tac-Toe", "FR": "Four in a Row", "MA": "Mancala", "RV": "Reversi",
    "DB": "Dots and Boxes", "SB": "Sea Battle", "CK": "Checkers", "CH": "Chain Burst",
    "HE": "Hearts", "SD": "Spades", "CE": "Crazy Eights", "DO": "Dominoes", "LU": "Ludo",
    "CC": "Chess",
    # Wave 3
    "WS": "Word Search", "HM": "Secret Word",
}
LAUNCH_GAMES = 14  # the v1 launch catalog, all playable (plan section 12)


def normalise(status):
    word = status.strip().lower().split(" ")[0] if status.strip() else "todo"
    return word if word in {"done", "active", "review", "ready", "todo", "blocked"} else "todo"


def expand(card_id, status):
    """Return [(card, status)] for one index row."""
    lane = re.fullmatch(r"([A-Z]+)(\d+)[–-]\1?(\d+)", card_id)
    if not lane:
        return [(card_id, normalise(status))]
    prefix, first, last = lane.group(1), int(lane.group(2)), int(lane.group(3))
    cards = [f"{prefix}{n}" for n in range(first, last + 1)]
    named = dict(re.findall(rf"\b({prefix}\d+)\s+(\w+)", status))
    if not named:
        return [(card, normalise(status)) for card in cards]
    return [(card, normalise(named.get(card, "todo"))) for card in cards]


def read_index():
    groups, current = {}, None
    in_index = False
    for line in TASKS.read_text(encoding="utf-8").splitlines():
        if line.startswith("| ID | Title |"):
            in_index = True
            continue
        if not in_index:
            continue
        if not line.startswith("|"):
            break
        cells = [cell.strip() for cell in line.strip("|").split("|")]
        heading = re.fullmatch(r"\*\*(.+)\*\*", cells[0])
        if heading:
            current = heading.group(1)
            groups[current] = []
            continue
        if cells[0].startswith("---") or current is None or len(cells) < 7:
            continue
        human = cells[2] == "Human"
        groups[current].extend((card, state, human) for card, state in expand(cells[0], cells[6]))
    return groups


def bar(cards):
    total = len(cards)
    done = sum(1 for _, state, _ in cards if state in DONE)
    moving = sum(1 for _, state, _ in cards if state in IN_PROGRESS)
    if total == 0:
        return "", 0, 0, 0
    full = round(WIDTH * done / total)
    part = min(WIDTH - full, round(WIDTH * (done + moving) / total) - full)
    text = "█" * full + "▒" * part + "░" * (WIDTH - full - part)
    return text, done, moving, total


def line(label, cards):
    text, done, moving, total = bar(cards)
    percent = round(100 * done / total) if total else 0
    extra = f", {moving} in progress" if moving else ""
    return f"{label:<33} {text} {percent:>3} %  ({done} of {total} done{extra})"


def game_cards(cards, prefix):
    return [c for c in cards if re.match(rf"{prefix}(\d|[A-Z]$)", c[0])]


def render(groups):
    every = [card for cards in groups.values() for card in cards]
    out = [line("All tasks", every), ""]
    for name, cards in groups.items():
        out.append(line(re.sub(r" \(.*\)", "", name), cards))

    waves = [cards for name, cards in groups.items() if name.lower().startswith("wave")]
    expansion = [card for cards in waves for card in cards]
    ready_games = LAUNCH_GAMES
    out += ["", "Expansion games, by lane"]
    for prefix, game in GAMES.items():
        cards = game_cards(expansion, prefix)
        if cards and all(state in DONE for _, state, _ in cards):
            ready_games += 1
        out.append(line(f"  {game}", cards))

    target = LAUNCH_GAMES + len(GAMES)
    games_bar = [("g", "done", False)] * ready_games + [("g", "todo", False)] * (target - ready_games)
    out += ["", line("Games playable", games_bar).replace("done", "ready")]

    human_open = [card for card, state, human in every if human and state not in DONE]
    today = datetime.date.today().strftime("%d %b %Y")
    return "\n".join([
        START,
        "## Progress",
        "",
        f"Updated {today} from the status column in `docs/TASKS.md`. Rebuild with `python scripts/progress.py`.",
        "",
        "█ done   ▒ in progress (active or in review)   ░ not started (ready, todo or blocked)",
        "",
        "```text",
        *out,
        "```",
        "",
        f"Open human tasks: {', '.join(human_open) if human_open else 'none'}.",
        END,
    ])


def main():
    plan = PLAN.read_text(encoding="utf-8")
    block = render(read_index())
    if START in plan:
        plan = re.sub(re.escape(START) + ".*?" + re.escape(END), lambda _: block, plan, flags=re.S)
    else:
        anchor = "Labels used in this plan:"
        if anchor not in plan:
            sys.exit("Cannot find where to put the progress section.")
        plan = plan.replace(anchor, block + "\n\n" + anchor, 1)
    PLAN.write_text(plan, encoding="utf-8", newline="")
    sys.stdout.reconfigure(encoding="utf-8")  # the Windows console default cannot print the bar blocks
    print(block)


if __name__ == "__main__":
    main()
