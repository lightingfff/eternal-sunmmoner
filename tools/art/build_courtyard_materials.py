"""Generate the seamless material tiles for the Sanctuary GLES courtyard."""

from math import cos, pi, sin, sqrt
from pathlib import Path
from random import Random

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "app/src/main/assets/diorama"
SIZE = 128
EXTENDED_SIZE = 256


def periodic_noise(x, y, size=SIZE):
    """A small hand-painted tonal variation which repeats at the tile edges."""
    u, v = x * 2 * pi / size, y * 2 * pi / size
    return sin(3 * u + 5 * v) * 2.4 + cos(9 * u - 6 * v) * 1.5 + sin(17 * u + 11 * v) * 1.7


def paint(image, x, y, color, shift):
    image.putpixel((x, y), tuple(round(max(0, min(255, c + shift))) for c in color))


def cobblestone():
    rng = Random(14119)
    size = EXTENDED_SIZE
    interval = 16
    count = size // interval
    samples = []
    for col in range(count):
        column = []
        for row in range(count):
            sx = col * interval + rng.uniform(-4.6, 4.6)
            sy = row * interval + (6 if col % 2 else 0) + rng.uniform(-4.6, 4.6)
            # Wear varies across several stones at once, with gentler variations per stone.
            u, v = 2 * pi * sx / size, 2 * pi * sy / size
            wear = 8 * sin(2 * u - v) + 5 * cos(u + 3 * v)
            column.append((sx, sy, wear + rng.uniform(-14, 14)))
        samples.append(column)

    image = Image.new("RGB", (size, size))
    for y in range(size):
        for x in range(size):
            neighbor = []
            for col in range(x // interval - 2, x // interval + 3):
                for row in range(y // interval - 2, y // interval + 3):
                    sx, sy, shade = samples[col % count][row % count]
                    dx = (x - sx + size / 2) % size - size / 2
                    dy = (y - sy + size / 2) % size - size / 2
                    neighbor.append((dx * dx + dy * dy, dx, dy, shade))
            first, second = sorted(neighbor)[:2]
            _, dx, dy, shade = first
            gap = (second[0] - first[0]) / max(1, 2 * sqrt((second[1] - dx)**2 + (second[2] - dy)**2))
            noise = periodic_noise(x, y, size) + rng.uniform(-3.5, 3.5)
            mortar = .72 + .14 * sin(1.21 * dx + .37 * dy)
            if gap < mortar:
                paint(image, x, y, (95, 89, 77), noise + shade * .18)
            else:
                height = min(1, (gap - mortar) / 2.15)
                bevel = max(-12, min(12, -dy * 1.45 - dx * .80)) * (1 - height)
                pitting = -rng.uniform(7, 17) if rng.random() < .045 else 0
                paint(image, x, y, (151, 145, 132), shade + bevel + 14 * height + noise + pitting)
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
    size = EXTENDED_SIZE
    image = Image.new("RGB", (size, size))
    nominal = 16
    count = size // nominal
    # Jitter cumulative edges to vary individual tile widths without breaking the repeat.
    borders = [i * nominal + (rng.randint(-2, 2) if 0 < i < count else 0) for i in range(count + 1)]
    for row, (top, bottom) in enumerate(zip(borders, borders[1:])):
        offset = (8 * (row % 2) + rng.randint(-3, 3)) % size
        tile_edges = [i * nominal + (rng.randint(-4, 4) if 0 < i < count else 0) for i in range(count + 1)]
        for left, right in zip(tile_edges, tile_edges[1:]):
            variation = rng.uniform(-13, 13) + 4 * cos(2 * pi * row / count)
            chip_side = rng.choice((-1, 0, 1))
            color = rng.choice(((89, 121, 129), (98, 127, 132), (97, 130, 136)))
            for y in range(top, bottom):
                v = (y - top) / (bottom - top)
                for tx in range(left, right):
                    x = (tx + offset) % size
                    u = tx - left
                    edge = min(u, right - tx - 1)
                    grain = periodic_noise(x, y, size) + rng.uniform(-3.1, 3.1)
                    brown = 3 * sin(x * 4 * pi / size + .35 * row)
                    lap = -22 if v == 0 else max(0, 20 - 68 * v) - 18 * v**4
                    joint = -17 if edge == 0 else 6 if edge == 1 else 0
                    chipped = chip_side != 0 and v < .19 and (
                        (chip_side < 0 and u < 2) or (chip_side > 0 and right - tx < 3)
                    )
                    paint(image, x, y, color, variation + grain + brown + lap + joint - (9 if chipped else 0))
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
