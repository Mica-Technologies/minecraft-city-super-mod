#!/usr/bin/env python3
"""
gen_parks_tools.py -- the Parks & Greenery tree tools' assets.

The chainsaw (with a running sprite its item model swaps in by the "csm:running" property), the
pole trimmer and the tree shears, each a 16 px item sprite drawn here as distance fields round a
few line segments, so a sprite is the same on every machine; and the brush pile a chainsaw
felling leaves on the ground: two cutout textures, a low model of two crossed planes and two flat
ones, and its blockstate. Writes under modules/parks/src/main/resources/assets/csm:

  * textures/items/parks/{chainsaw,chainsaw_running,pole_trimmer,tree_shears}.png
  * models/item/{chainsaw,chainsaw_running,pole_trimmer,tree_shears}.json
  * textures/blocks/parks/brush_pile_{top,side}.png
  * models/block/parks/brush_pile.json, blockstates/brush_pile.json

The lang lines and tab lines are hand-written (ItemChainsaw and friends are classes, not a
catalogue). The sounds are gen_parks_tool_sounds.py's.

Usage:
    python gen_parks_tools.py            # write everything
    python gen_parks_tools.py --check    # fail if the tree has drifted

Requires Pillow.
"""
import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'modules', 'parks', 'src', 'main', 'resources', 'assets', 'csm')


# ------------------------------------------------------------------------------------------
# Drawing
# ------------------------------------------------------------------------------------------
def seg_dist(px, py, a, b):
    """Distance from the pixel centre (px + .5, py + .5) to the segment a-b."""
    x, y = px + 0.5, py + 0.5
    (ax, ay), (bx, by) = a, b
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((x - ax) * dx + (y - ay) * dy) / max(1e-9, dx * dx + dy * dy)))
    return math.hypot(x - (ax + t * dx), y - (ay + t * dy))


def stroke(img, a, b, width, colour, outline=None, ow=0.75):
    """A line a-b, width pixels across, with an optional darker rim ow pixels wide."""
    w, h = img.size
    px = img.load()
    for y in range(h):
        for x in range(w):
            d = seg_dist(x, y, a, b)
            if d <= width / 2.0:
                px[x, y] = colour
            elif outline is not None and d <= width / 2.0 + ow and px[x, y][3] == 0:
                px[x, y] = outline


def rect(img, x0, y0, x1, y1, colour):
    px = img.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = colour


def outline_all(img, colour):
    """Rims every opaque region with colour, one pixel out, where it is clear."""
    w, h = img.size
    src = img.copy().load()
    px = img.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] and src[nx, ny] != colour:
                    px[x, y] = colour
                    break


def blank(size=16):
    return Image.new('RGBA', (size, size), (0, 0, 0, 0))


ORANGE = (232, 112, 28, 255)
ORANGE_DARK = (176, 74, 16, 255)
STEEL = (196, 198, 204, 255)
STEEL_DARK = (118, 120, 128, 255)
CHAIN = (64, 64, 70, 255)
CHAIN_LIT = (150, 150, 158, 255)
BLACK = (34, 32, 34, 255)
INK = (22, 20, 22, 255)


def chainsaw(running):
    """Engine housing low left, bar to the top right, chain round the bar. Running, the chain
    teeth are blurred bright and a puff of exhaust trails the engine."""
    img = blank()
    # Bar and its chain.
    stroke(img, (7.5, 8.5), (14.2, 1.8), 2.2, STEEL)
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = seg_dist(x, y, (7.5, 8.5), (14.2, 1.8))
            if 1.1 < d <= 1.9 and x + y >= 14:
                lit = (x + y + (1 if running else 0)) % 2 == 0
                px[x, y] = (CHAIN_LIT if running else CHAIN) if lit else CHAIN
    # Engine housing.
    rect(img, 2, 7, 8, 12, ORANGE)
    rect(img, 2, 11, 8, 12, ORANGE_DARK)
    rect(img, 3, 8, 4, 9, BLACK)          # air filter cover
    # Top handle and rear handle.
    stroke(img, (3.0, 6.0), (7.5, 4.5), 1.2, BLACK)
    stroke(img, (7.5, 4.5), (8.5, 7.0), 1.1, BLACK)
    stroke(img, (1.0, 10.0), (1.0, 13.5), 1.1, BLACK)
    stroke(img, (1.0, 13.5), (4.0, 13.5), 1.1, BLACK)
    outline_all(img, INK)
    if running:
        px = img.load()
        for (x, y, a) in ((0, 6, 150), (1, 5, 110), (0, 4, 80), (1, 3, 60)):
            if px[x, y][3] == 0:
                px[x, y] = (200, 200, 205, a)
    return img


POLE = (222, 186, 42, 255)
POLE_DARK = (168, 132, 24, 255)
GRIP = (40, 92, 52, 255)


def pole_trimmer():
    """A fibreglass pole up to a pruning saw blade and the lopper hook beside it."""
    img = blank()
    stroke(img, (1.0, 15.0), (11.0, 5.0), 1.6, POLE)
    stroke(img, (1.0, 15.0), (3.5, 12.5), 1.8, GRIP)
    stroke(img, (5.5, 10.5), (6.2, 9.8), 1.8, POLE_DARK)    # the joint between sections
    # Saw blade, curving up and over.
    stroke(img, (11.0, 5.0), (13.0, 1.5), 1.6, STEEL)
    stroke(img, (13.0, 1.5), (14.8, 0.6), 1.2, STEEL)
    # Hook and its cutter.
    stroke(img, (11.0, 5.0), (14.5, 4.0), 1.2, STEEL_DARK)
    stroke(img, (14.5, 4.0), (14.5, 2.6), 1.0, STEEL_DARK)
    outline_all(img, INK)
    return img


RED_GRIP = (176, 36, 34, 255)


def tree_shears():
    """Loppers: two long handles from the bottom left, crossing at a pivot, and the bypass
    blades open at the top right."""
    img = blank()
    stroke(img, (0.6, 11.0), (7.5, 8.5), 1.5, STEEL_DARK)
    stroke(img, (5.0, 15.4), (7.5, 8.5), 1.5, STEEL_DARK)
    stroke(img, (0.6, 11.0), (3.4, 10.0), 1.8, RED_GRIP)
    stroke(img, (5.0, 15.4), (6.0, 12.6), 1.8, RED_GRIP)
    stroke(img, (7.5, 8.5), (15.0, 3.4), 1.7, STEEL)
    stroke(img, (7.5, 8.5), (10.4, 0.8), 1.4, STEEL)
    rect(img, 7, 8, 8, 9, BLACK)                             # pivot bolt
    outline_all(img, INK)
    return img


# ------------------------------------------------------------------------------------------
# The brush pile
# ------------------------------------------------------------------------------------------
BARK = [(92, 66, 40, 255), (110, 80, 50, 255), (76, 54, 32, 255)]
LEAF = [(70, 112, 40, 255), (88, 132, 48, 255), (58, 94, 34, 255), (122, 140, 52, 255),
        (140, 104, 44, 255)]


def brush_top():
    """From above: sticks at all angles and clumps of wilting leaves, clear between."""
    rnd = random.Random(2511)
    img = blank()
    for _ in range(14):
        cx, cy = rnd.uniform(2, 14), rnd.uniform(2, 14)
        ang = rnd.uniform(0, math.pi)
        ln = rnd.uniform(4, 8)
        a = (cx - math.cos(ang) * ln / 2, cy - math.sin(ang) * ln / 2)
        b = (cx + math.cos(ang) * ln / 2, cy + math.sin(ang) * ln / 2)
        stroke(img, a, b, rnd.choice((0.9, 1.1, 1.4)), rnd.choice(BARK))
    px = img.load()
    for _ in range(16):
        cx, cy = rnd.randint(1, 14), rnd.randint(1, 14)
        r = rnd.uniform(0.8, 1.9)
        col = rnd.choice(LEAF)
        for y in range(16):
            for x in range(16):
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r and rnd.random() < 0.85:
                    px[x, y] = col
    return img


def brush_side():
    """From the side: a low heap, sticks poking out of it, in the bottom six rows."""
    rnd = random.Random(2512)
    img = blank()
    px = img.load()
    for x in range(16):
        top = 16 - int(round(3.5 + 2.2 * math.sin(math.pi * (x + 0.5) / 16) + rnd.uniform(-1, 1)))
        for y in range(top, 16):
            if rnd.random() < 0.8:
                px[x, y] = rnd.choice(LEAF if rnd.random() < 0.65 else BARK)
    for _ in range(4):
        x0 = rnd.uniform(1, 15)
        stroke(img, (x0, 15.5), (x0 + rnd.uniform(-4, 4), rnd.uniform(9.5, 11.5)), 0.9,
               rnd.choice(BARK))
    return img


def brush_model():
    def plane_up(x0, y, z0, x1, z1, rot=None):
        e = {"from": [x0, y, z0], "to": [x1, y, z1],
             "faces": {"up": {"uv": [x0, z0, x1, z1], "texture": "#top"},
                       "down": {"uv": [x0, z0, x1, z1], "texture": "#top"}}}
        if rot:
            e["rotation"] = {"origin": [8, y, 8], "axis": "y", "angle": rot}
        return e

    def cross(angle):
        return {"from": [0.8, 0, 8], "to": [15.2, 6, 8],
                "rotation": {"origin": [8, 0, 8], "axis": "y", "angle": angle, "rescale": True},
                "shade": False,
                "faces": {"north": {"uv": [0, 10, 16, 16], "texture": "#side"},
                          "south": {"uv": [16, 10, 0, 16], "texture": "#side"}}}

    return {
        "parent": "block/block",
        "ambientocclusion": False,
        "textures": {"top": "csm:blocks/parks/brush_pile_top",
                     "side": "csm:blocks/parks/brush_pile_side",
                     "particle": "csm:blocks/parks/brush_pile_top"},
        "elements": [cross(45), cross(-45),
                     plane_up(1, 2.5, 1, 15, 15),
                     plane_up(3, 4, 3, 13, 13, rot=22.5)],
    }


BLOCKSTATE = {
    "forge_marker": 1,
    "defaults": {"model": "csm:parks/brush_pile"},
    "variants": {"normal": [{}], "inventory": [{}]},
}


def item_model(texture, overrides=None):
    m = {"parent": "item/handheld", "textures": {"layer0": texture}}
    if overrides:
        m["overrides"] = overrides
    return m


# ------------------------------------------------------------------------------------------
# Output
# ------------------------------------------------------------------------------------------
def png(img):
    buf = io.BytesIO()
    img.save(buf, format='PNG', optimize=False)
    return buf.getvalue()


def js(obj):
    return (json.dumps(obj, indent=2) + '\n').encode('utf-8')


def outputs():
    out = {}
    tex_items = 'textures/items/parks/'
    out[tex_items + 'chainsaw.png'] = png(chainsaw(False))
    out[tex_items + 'chainsaw_running.png'] = png(chainsaw(True))
    out[tex_items + 'pole_trimmer.png'] = png(pole_trimmer())
    out[tex_items + 'tree_shears.png'] = png(tree_shears())
    out['models/item/chainsaw.json'] = js(item_model(
        'csm:items/parks/chainsaw',
        [{"predicate": {"csm:running": 1}, "model": "csm:item/chainsaw_running"}]))
    out['models/item/chainsaw_running.json'] = js(item_model('csm:items/parks/chainsaw_running'))
    out['models/item/pole_trimmer.json'] = js(item_model('csm:items/parks/pole_trimmer'))
    out['models/item/tree_shears.json'] = js(item_model('csm:items/parks/tree_shears'))
    out['textures/blocks/parks/brush_pile_top.png'] = png(brush_top())
    out['textures/blocks/parks/brush_pile_side.png'] = png(brush_side())
    out['models/block/parks/brush_pile.json'] = js(brush_model())
    out['blockstates/brush_pile.json'] = js(BLOCKSTATE)
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n\n')[0])
    ap.add_argument('--check', action='store_true', help='fail if the tree has drifted')
    args = ap.parse_args()
    stale = []
    for rel, data in sorted(outputs().items()):
        path = os.path.join(ASSETS, rel)
        old = open(path, 'rb').read() if os.path.exists(path) else None
        if old == data:
            continue
        stale.append(rel)
        if not args.check:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, 'wb') as fh:
                fh.write(data)
    if args.check:
        if stale:
            print('out of date (re-run without --check):\n  ' + '\n  '.join(stale))
            sys.exit(1)
        print('up to date')
    else:
        print('wrote %d file(s)' % len(stale))


if __name__ == '__main__':
    main()
