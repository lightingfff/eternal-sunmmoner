"""Generate the seamless material tiles for the Sanctuary GLES courtyard."""

from math import cos, pi, sin, sqrt
from pathlib import Path
from random import Random

from PIL import Image, ImageDraw


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


def relief_texture(height, strength):
    """Pack two tangent-space slopes and a crevice mask into a repeatable RGB tile."""
    size = height.width
    source = height.load()
    relief = Image.new("RGB", (size, size))
    for y in range(size):
        for x in range(size):
            dx = (source[(x - 1) % size, y] - source[(x + 1) % size, y]) * strength
            dy = (source[x, (y - 1) % size] - source[x, (y + 1) % size]) * strength
            u, v = (round(128 + max(-108, min(108, slope))) for slope in (dx, dy))
            relief.putpixel((x, y), (u, v, source[x, y]))
    return relief


def cobblestone():
    rng = Random(14119)
    color_rng = Random(6103)
    colors = ((151, 143, 127), (146, 142, 134), (137, 143, 139),
              (155, 142, 132), (145, 135, 121), (152, 151, 144))
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
            column.append((sx, sy, wear + rng.uniform(-14, 14), color_rng.choice(colors)))
        samples.append(column)

    image = Image.new("RGB", (size, size))
    heights = Image.new("L", (size, size))
    for y in range(size):
        for x in range(size):
            neighbor = []
            for col in range(x // interval - 2, x // interval + 3):
                for row in range(y // interval - 2, y // interval + 3):
                    sx, sy, shade, color = samples[col % count][row % count]
                    dx = (x - sx + size / 2) % size - size / 2
                    dy = (y - sy + size / 2) % size - size / 2
                    neighbor.append((dx * dx + dy * dy, dx, dy, shade, color))
            first, second = sorted(neighbor)[:2]
            _, dx, dy, shade, color = first
            gap = (second[0] - first[0]) / max(1, 2 * sqrt((second[1] - dx)**2 + (second[2] - dy)**2))
            noise = periodic_noise(x, y, size) + rng.uniform(-3.5, 3.5)
            mortar = .72 + .14 * sin(1.21 * dx + .37 * dy)
            if gap < mortar:
                paint(image, x, y, (95, 89, 77), noise + shade * .18)
                heights.putpixel((x, y), 26)
            else:
                height = min(1, (gap - mortar) / 2.15)
                bevel = max(-12, min(12, -dy * 1.45 - dx * .80)) * (1 - height)
                pitting = -rng.uniform(7, 17) if rng.random() < .045 else 0
                paint(image, x, y, color, shade + bevel + 14 * height + noise + pitting)
                heights.putpixel((x, y), round(44 + 193 * height + .6 * pitting))
    return image, relief_texture(heights, .37)


def masonry():
    rng = Random(8512)
    image = Image.new("RGB", (SIZE, SIZE))
    heights = Image.new("L", (SIZE, SIZE))
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
                heights.putpixel((u_out, v_out), 30)
            else:
                lip = 10 if v == 1 else -19 if v == 15 else 0
                mottling = 6 * sin(2 * pi * (10 * x + 9 * y) / SIZE + row)
                paint(image, u_out, v_out, (163, 151, 128), shift + lip + mottling)
                bevel = min(1, min(vline - 1.2, SIZE / columns - .8 - vline, v, 16 - v) / 2.3)
                heights.putpixel((u_out, v_out), round(63 + 172 * bevel))
    return image, relief_texture(heights, .42)


def slate():
    rng = Random(579)
    size = EXTENDED_SIZE
    image = Image.new("RGB", (size, size))
    heights = Image.new("L", (size, size))
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
            color = rng.choice(((74, 108, 121), (90, 123, 130), (94, 125, 119)))
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
                    crown = max(0, sin(pi * min(1, (u + .5) / (right - left)))) ** .65
                    height = 45 + 135 * crown + 54 * v - (20 if chipped else 0)
                    heights.putpixel((x, y), 22 if y == top else round(height))
    return image, relief_texture(heights, .62)


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


def quay_planks():
    rng = Random(16121)
    image = Image.new("RGB", (SIZE, SIZE))
    for x in range(SIZE):
        board, edge = divmod(x, 16)
        shade = 7 * sin(board * 1.9)
        for y in range(SIZE):
            end = (y + board * 43) % SIZE
            grain = 7 * sin((x * 29 + 3 * sin(2 * pi * y / SIZE)) * 2 * pi / SIZE)
            grain += 6 * sin(2 * pi * (x * 13 + y * 2) / SIZE) + rng.uniform(-3, 3)
            seam = -35 if edge == 0 or end == 0 else 12 if edge == 1 or end == 1 else 0
            nail = -32 if edge in (3, 12) and end in (4, SIZE - 5) else 0
            paint(image, x, y, (137, 106, 73), grain + shade + seam + nail)
    return image


def market_canvas():
    rng = Random(8087)
    image = Image.new("RGB", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            weave = 3 if (x + y) % 4 == 0 else -2
            flecks = -12 if rng.random() < .02 else 0
            light = 4 * cos(2 * pi * y / SIZE) + 5 * sin(4 * pi * x / SIZE)
            paint(image, x, y, (218, 202, 172), weave + flecks + light + rng.uniform(-2.5, 2.5))
    return image


def blossom_canopy():
    rng = Random(16109)
    image = Image.new("RGB", (SIZE, SIZE), (100, 61, 68))
    draw = ImageDraw.Draw(image)
    palette = ((158, 96, 104), (178, 108, 116), (200, 128, 135),
               (216, 151, 151), (229, 167, 160), (239, 193, 177))
    for _ in range(820):
        x, y = rng.randrange(SIZE), rng.randrange(SIZE)
        radius = rng.randint(2, 6)
        color = palette[rng.randrange(len(palette))]
        for yy in (y - SIZE, y, y + SIZE):
            for xx in (x - SIZE, x, x + SIZE):
                if xx + radius < 0 or xx - radius >= SIZE or yy + radius < 0 or yy - radius >= SIZE:
                    continue
                draw.polygon([(xx - radius, yy), (xx - 1, yy - radius),
                              (xx + radius - 1, yy - radius // 2), (xx + radius, yy + 1),
                              (xx + 1, yy + radius)], fill=color)
                draw.point((xx - 1, yy - radius // 2), fill=(240, 198, 180))
    return image


if __name__ == "__main__":
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for generator in (cobblestone, masonry, slate):
        albedo, relief = generator()
        albedo.save(OUTPUT / f"{generator.__name__}.png", optimize=True)
        relief.save(OUTPUT / f"{generator.__name__}_relief.png", optimize=True)
    for generator in (plaster, quay_planks, market_canvas, blossom_canopy):
        generator().save(OUTPUT / f"{generator.__name__}.png", optimize=True)
