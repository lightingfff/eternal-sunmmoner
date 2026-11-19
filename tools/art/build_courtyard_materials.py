"""Generate the seamless material tiles for the Sanctuary GLES courtyard."""

from math import cos, pi, sin, sqrt
from pathlib import Path
from random import Random

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "app/src/main/assets/diorama"
SIZE = 128


def periodic_noise(x, y):
    """A small hand-painted tonal variation which repeats at the tile edges."""
    u, v = x * 2 * pi / SIZE, y * 2 * pi / SIZE
    return sin(3 * u + 5 * v) * 2.4 + cos(9 * u - 6 * v) * 1.5 + sin(17 * u + 11 * v) * 1.7


def paint(image, x, y, color, shift):
    image.putpixel((x, y), tuple(round(max(0, min(255, c + shift))) for c in color))


def cobblestone():
    rng = Random(14119)
    count = 8
    interval = SIZE / count
    samples = [
        (i * interval + rng.uniform(-4, 4), j * interval + (6 if i % 2 else 0) + rng.uniform(-4, 4), rng.uniform(-20, 20))
        for i in range(count) for j in range(count)
    ]
    image = Image.new("RGB", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            neighbor = []
            for sx, sy, shade in samples:
                dx = (x - sx + SIZE / 2) % SIZE - SIZE / 2
                dy = (y - sy + SIZE / 2) % SIZE - SIZE / 2
                neighbor.append((dx * dx + dy * dy, dx, dy, shade))
            first, second = sorted(neighbor)[:2]
            _, dx, dy, shade = first
            gap = (second[0] - first[0]) / max(1, 2 * sqrt((second[1] - dx)**2 + (second[2] - dy)**2))
            noise = periodic_noise(x, y) + rng.uniform(-2.9, 2.9)
            if gap < .84:
                paint(image, x, y, (84, 76, 66), noise)
            else:
                height = min(1, (gap - .84) / 1.85)
                bevel = max(-13, min(16, -dy * 1.8 - dx * .82)) * (1 - height)
                paint(image, x, y, (150, 140, 121), shade + bevel + 18 * height + noise)
    return image


def masonry():
    rng = Random(8512)
    image = Image.new("RGB", (SIZE, SIZE))
    rows, columns = 8, 5
    courses = [[rng.uniform(-14, 12) for _ in range(columns)] for _ in range(rows)]
    for y in range(SIZE):
        row, v = divmod(y, SIZE // rows)
        for x in range(SIZE):
            u = (x + (SIZE // (2 * columns) if row % 2 else 0)) % SIZE * columns / SIZE
            col, vline = int(u), (u % 1) * SIZE / columns
            edge = vline < 1.2 or vline > SIZE / columns - .8
            shift = courses[row][col] + periodic_noise(x, y) + rng.uniform(-3, 3)
            # The pixel boundary falls inside a block rather than on a mortar joint.
            u_out, v_out = (x - 6) % SIZE, (y - 5) % SIZE
            if v == 0 or edge:
                paint(image, u_out, v_out, (79, 76, 64), shift * .45)
            else:
                lip = 10 if v == 1 else -19 if v == 15 else 0
                mottling = 6 * sin(2 * pi * (10 * x + 9 * y) / SIZE + row)
                paint(image, u_out, v_out, (163, 151, 128), shift + lip + mottling)
    return image


def slate():
    rng = Random(579)
    image = Image.new("RGB", (SIZE, SIZE))
    courses = [[rng.uniform(-14, 14) for _ in range(8)] for _ in range(8)]
    for y in range(SIZE):
        row, v = divmod(y, 16)
        for x in range(SIZE):
            col, u = divmod((x + 8 * (row % 2)) % SIZE, 16)
            grain = periodic_noise(x, y) + rng.uniform(-2.9, 2.9)
            # Rows overlap toward the eave; seams are staggered between rows.
            lip = [-22, 16, 22, 16, 7, 2, 0, -1, 0, 0, -2, -2, -5, -9, -15, -26][v]
            joint = -22 if u == 0 else 9 if u == 1 else -11 if u == 15 else 0
            paint(image, x, y, (98, 130, 136), courses[row][col] + grain + lip + joint)
    return image


def plaster():
    rng = Random(621)
    image = Image.new("RGB", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            u, v = x * 2 * pi / SIZE, y * 2 * pi / SIZE
            chips = rng.uniform(-31, -14) if rng.random() < .02 else 0
            shift = 8 * sin(u + v) + 6 * cos(3 * u - 4 * v) + periodic_noise(x, y) + chips + rng.uniform(-4, 4)
            paint(image, x, y, (191, 175, 140), shift)
    return image


if __name__ == "__main__":
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for generator in (cobblestone, masonry, slate, plaster):
        generator().save(OUTPUT / f"{generator.__name__}.png", optimize=True)
