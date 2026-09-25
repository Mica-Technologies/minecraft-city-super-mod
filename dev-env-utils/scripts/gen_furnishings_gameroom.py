#!/usr/bin/env python3
"""
gen_furnishings_gameroom.py -- the bar, game room and interior furnishings in the Furniture &
Novelties module (the Furniture and Gaming tabs): wine rack, beer rack, beer tap, pool table,
dartboard, deck of cards, office chair, cast-iron radiator, coat tree, tall wall mirror, the two
restroom signs, hot tub, hanging chain and boarded-up planks, and a plain wooden
barrel.

Each block keeps its registry name, class and facing; this writes its model, textures and
blockstate, drawn here from scratch at real-world scale (1 block = 1 m):

  * textures/blocks/furniture/gameroom/*.png
  * models/block/furniture/gameroom/<registry>.json
  * blockstates/<registry>.json

under modules/furnishings/src/main/resources/assets/csm. No lang is written: the blocks keep
the names they have.

Every model faces north with its back (the wall it hangs on, a chair's backrest) at +Z, as the
rotatable blocks in the mod are drawn. Anything bigger than a block (the pool table, the hot tub,
the beer rack, the coat tree, the mirror, the boards) is centred on the placed block and kept
inside the -16..32 a JSON element may reach; every face of such an element carries an explicit
UV inside 0..16 so it never samples a neighbouring sprite.

Usage:
    python gen_furnishings_gameroom.py            # write everything
    python gen_furnishings_gameroom.py --check    # fail if the tree has drifted

Requires Pillow.
"""
import argparse
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lsc  # noqa: E402
import furniture_depth  # noqa: E402
import gen_trees  # noqa: E402

REPO = lsc.REPO
ASSETS = os.path.join(REPO, "modules", "furnishings", "src", "main", "resources", "assets", "csm")
SUB = "furniture/gameroom"
TEX = "csm:blocks/%s/" % SUB
MODEL = "csm:%s/" % SUB

clamp = lsc.clamp
shade = lsc.shade
fill = lsc.fill
rect = lsc.rect
disc = lsc.disc
frame = lsc.frame
face = lsc.face

ALL = ("north", "south", "east", "west", "up", "down")


def T(name):
    return TEX + name


# ------------------------------------------------------------------------------------------
# Element helper: fitted UVs, or scaled-down fitted UVs past the cell
# ------------------------------------------------------------------------------------------
def _face_uv(f, frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    std = {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1],
        "down": [x0, 16 - z1, x1, 16 - z0],
    }[f]
    if all(0 <= v <= 16 for v in std):
        uv = std
    else:
        # The element reaches past the cell: map the face from the texture's corner, scaled
        # down (keeping its aspect) if it is longer than the texture.
        w = abs(std[2] - std[0])
        h = abs(std[3] - std[1])
        k = min(1.0, 16.0 / max(w, h, 1e-6))
        uv = [0, 0, w * k, h * k]
    if abs(uv[2] - uv[0]) < 0.01:
        uv[2] = uv[0] + 0.01
    if abs(uv[3] - uv[1]) < 0.01:
        uv[3] = uv[1] + 0.01
    return uv


def B(frm, to, tex, faces=ALL, per=None, uv=None, rot=None, shade_=True):
    """An element. per: face -> texture key; uv: one uv for every face, or face -> uv;
    rot: (axis, angle, origin)."""
    out = {}
    for f in faces:
        if isinstance(uv, dict) and f in uv:
            u = uv[f]
        elif isinstance(uv, list):
            u = uv
        else:
            u = _face_uv(f, frm, to)
        out[f] = face((per or {}).get(f, tex), u)
    e = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": out}
    if rot:
        axis, angle, origin = rot
        e["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}
    if not shade_:
        e["shade"] = False
    return e


def refit(elements):
    """The octagon helpers' elements with this generator's UVs (they clamp to 0..16)."""
    out = []
    for e in elements:
        tex = next(iter(e["faces"].values()))["texture"][1:]
        n = B(e["from"], e["to"], tex, faces=list(e["faces"]))
        if "rotation" in e:
            n["rotation"] = e["rotation"]
        out.append(n)
    return out


def post(cx, cz, r, y0, y1, tex, top=True, bottom=True):
    return refit(lsc.post(cx, cz, r, y0, y1, tex, top=top, bottom=bottom))


def pipe_x(cy, cz, r, x0, x1, tex, ends=True):
    return refit(lsc.pipe_x(cy, cz, r, x0, x1, tex, ends=ends))


def pipe_z(cx, cy, r, z0, z1, tex, front=True):
    return refit(lsc.pipe_z(cx, cy, r, z0, z1, tex, front=front))


def gui(scale, dy=0.0):
    """Display transforms for a model bigger than a block: shrunk to fit a slot."""
    s = [scale] * 3
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, dy, 0], "scale": s},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [scale * 0.4] * 3},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, dy, 0], "scale": [scale * 0.8] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [scale * 0.6] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [scale * 0.6] * 3},
    }


def model(textures, elements, display=None):
    m = {"parent": "block/block", "textures": dict(textures), "elements": elements}
    if display:
        m["display"] = display
    return m


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
TEXTURES = {}


def texture(name):
    def reg(fn):
        TEXTURES[name] = fn
        return fn
    return reg


def flat(name, colour, grain=4, seed=1, size=16):
    TEXTURES[name] = lambda: fill(colour, size=size, grain=grain, seed=seed)


def wood(base, size=32, seed=1, vertical=False, streak=0.10, rings=5):
    """Wood grain running along u (or along v if vertical): streaks of darker and lighter lines
    that wander a little along their length."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    rows = []
    for j in range(size):
        rows.append(1.0 + rng.uniform(-streak, streak))
    phase = [rng.uniform(0, 6.28) for _ in range(rings)]
    for y in range(size):
        for x in range(size):
            a, b = (x, y) if not vertical else (y, x)
            wob = int(round(math.sin(a * 0.35 + phase[b % rings]) * 0.8))
            k = rows[(b + wob) % size]
            k *= 1.0 + rng.uniform(-0.025, 0.025)
            if (b + wob) % 7 == 0:
                k *= 0.9
            px[x, y] = shade(base, k) + (255,)
    return img


def metal(base, size=16, seed=1, brushed=True, vertical=False):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    lines = [rng.uniform(-0.06, 0.06) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            b = y if not vertical else x
            k = 1.0 + (lines[b] if brushed else 0) + rng.uniform(-0.02, 0.02)
            px[x, y] = shade(base, k) + (255,)
    return img


OAK = (168, 118, 70)
WALNUT = (96, 60, 38)
CEDAR = (150, 96, 60)
IRON = (58, 58, 60)
STEEL = (176, 180, 184)
BLACK = (30, 30, 32)
FELT = (22, 110, 62)


@texture("oak")
def _oak():
    return wood(OAK, seed=11)


@texture("oak_v")
def _oak_v():
    return wood(OAK, seed=11, vertical=True)


@texture("walnut")
def _walnut():
    return wood(WALNUT, seed=12)


@texture("walnut_v")
def _walnut_v():
    return wood(WALNUT, seed=12, vertical=True)


@texture("cedar_slats")
def _cedar_slats():
    """Vertical tongue-and-groove cedar cladding, a groove every 4 texels: sixteen slats across
    the 2.2 m side the texture is stretched over, so each is about 14 cm."""
    img = wood(CEDAR, size=64, seed=13, vertical=True, streak=0.07)
    for x in range(0, 64, 4):
        rect(img, x, 0, x + 1, 64, shade(CEDAR, 0.62))
    return img


@texture("steel")
def _steel():
    return metal(STEEL, seed=14)


@texture("steel_v")
def _steel_v():
    return metal(STEEL, seed=14, vertical=True)


@texture("chrome")
def _chrome():
    img = metal((200, 204, 210), seed=15)
    rect(img, 0, 5, 16, 7, (236, 238, 242))
    return img


@texture("black_plastic")
def _black_plastic():
    return fill((34, 34, 36), grain=3, seed=16)


@texture("mesh")
def _mesh():
    """Office chair back mesh: a fine dark weave on a charcoal frame."""
    img = fill((44, 44, 48), grain=2, seed=17)
    for y in range(16):
        for x in range(16):
            if (x + y) % 2 == 0:
                rect(img, x, y, x + 1, y + 1, (30, 30, 33))
    return img


@texture("fabric_seat")
def _fabric_seat():
    img = fill((40, 42, 50), grain=5, seed=18)
    return img


@texture("iron_paint")
def _iron_paint():
    """Cast iron in silver radiator paint: slightly rough."""
    return fill((150, 150, 146), grain=9, seed=19)


@texture("iron_paint_dark")
def _iron_paint_dark():
    return fill((108, 108, 104), grain=8, seed=20)


@texture("brass")
def _brass():
    return metal((196, 160, 72), seed=21)


@texture("red_plastic")
def _red_plastic():
    return fill((170, 34, 30), grain=4, seed=22)


@texture("felt")
def _felt():
    return fill(FELT, grain=5, seed=23)


@texture("pocket")
def _pocket():
    img = fill((16, 16, 16), grain=2, seed=24)
    return img


@texture("balls")
def _balls():
    """Sixteen pool balls, one 4 x 4 cell each: the cue ball, 1-8 solid, 9-15 striped."""
    cols = [(236, 232, 220), (230, 190, 30), (30, 60, 170), (200, 30, 30), (90, 40, 140),
            (230, 110, 20), (20, 120, 60), (120, 30, 30), (20, 20, 20), (230, 190, 30),
            (30, 60, 170), (200, 30, 30), (90, 40, 140), (230, 110, 20), (20, 120, 60),
            (120, 30, 30)]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for i, c in enumerate(cols):
        x0, y0 = (i % 4) * 4, (i // 4) * 4
        if i >= 9:
            rect(img, x0, y0, x0 + 4, y0 + 4, (236, 232, 220))
            rect(img, x0, y0 + 1, x0 + 4, y0 + 3, c)
        else:
            rect(img, x0, y0, x0 + 4, y0 + 4, c)
        rect(img, x0 + 1, y0 + 1, x0 + 2, y0 + 2, shade(c if i < 9 else (236, 232, 220), 1.3))
    return img


# Wine bottles lying in the diamond rack's four bins. The bins are the quadrants the X cuts, so
# the bottles stack square to the boards: centres at a whole number of pitches out from each
# board, in (u, v) = the two diagonal axes. A bottle is 1.5 px (9 cm) across, a touch over a
# real 7.5 cm one so the rows read; kept only where it fits clear of the frame.
WINE_PITCH = 1.5
WINE_GLASS = [(34, 92, 50), (44, 38, 40), (86, 26, 42)]      # deep green, near black, burgundy
WINE_FOILS = [(128, 22, 38), (196, 160, 70), (26, 24, 26), (226, 214, 180)]


def wine_bottles():
    r = WINE_PITCH / 2.0
    rng = random.Random(25)
    out = []
    k = 1.0 / math.sqrt(2.0)
    for su in (-1, 1):
        for sv in (-1, 1):
            for i in range(8):
                for j in range(8):
                    u = su * (0.4 + r + i * WINE_PITCH)
                    v = sv * (0.4 + r + j * WINE_PITCH)
                    x = 8 + (u + v) * k
                    y = 8 + (u - v) * k
                    if 1 + r - 0.05 <= x <= 15 - r + 0.05 and 1 + r - 0.05 <= y <= 15 - r + 0.05:
                        out.append((round(x, 3), round(y, 3), rng.randrange(3), rng.randrange(4)))
    return out


def _wine_bottles():
    """The bottles' shoulders behind their necks: a disc of dark glass for each bottle in
    wine_bottles(), with a lit rim, on the shadow at the back of the bins. The plane it is on
    spans x 1..15, y 1..15 and is seen from the north, so texel u runs from x = 16 down."""
    img = Image.new("RGBA", (64, 64), (10, 8, 8, 255))
    for (x, y, g, _f) in wine_bottles():
        tx_, ty = (16 - x) * 4, (16 - y) * 4
        glass = WINE_GLASS[g]
        disc(img, tx_, ty, WINE_PITCH * 2 - 0.3, shade(glass, 0.8))
        disc(img, tx_ - 0.6, ty - 0.6, WINE_PITCH * 2 - 1.2, glass)
        rect(img, int(tx_ - 2.2), int(ty - 2.2), int(tx_ - 1.2), int(ty - 1.2), shade(glass, 2.2))
    return img


@texture("wine_glass")
def _wine_glass():
    """Three 8 x 8 cells of bottle glass (a neck's sides) and one spare."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for i, c in enumerate(WINE_GLASS):
        x0, y0 = (i % 2) * 8, (i // 2) * 8
        rect(img, x0, y0, x0 + 8, y0 + 8, c)
        rect(img, x0 + 1, y0, x0 + 2, y0 + 8, shade(c, 1.9))
    return img


@texture("wine_foils")
def _wine_foils():
    """Four 8 x 8 cells of foil capsule: burgundy, gold, black, cream, with a pressed top ring."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for i, c in enumerate(WINE_FOILS):
        x0, y0 = (i % 2) * 8, (i // 2) * 8
        rect(img, x0, y0, x0 + 8, y0 + 8, c)
        frame(img, x0 + 1, y0 + 1, x0 + 7, y0 + 7, shade(c, 0.75))
        rect(img, x0 + 2, y0 + 2, x0 + 3, y0 + 3, shade(c, 1.5))
    return img


TEXTURES["wine_bottles"] = _wine_bottles


@texture("keg")
def _keg():
    """A stainless keg seen side on: brushed steel with two rolling rings."""
    img = metal((190, 194, 198), seed=26, vertical=False)
    for y in (3, 12):
        rect(img, 0, y, 16, y + 1, (130, 134, 140))
        rect(img, 0, y + 1, 16, y + 2, (222, 226, 230))
    return img


@texture("keg_top")
def _keg_top():
    img = metal((170, 174, 180), seed=27)
    disc(img, 8, 8, 3.2, (60, 60, 64))
    disc(img, 8, 8, 1.6, (150, 150, 156))
    return img


def _crate(colour, seed):
    def side():
        img = fill(colour, grain=4, seed=seed)
        frame(img, 0, 0, 16, 16, shade(colour, 0.7))
        rect(img, 5, 3, 11, 5, shade(colour, 0.35))   # hand hole
        rect(img, 2, 8, 14, 9, shade(colour, 0.8))    # rib
        rect(img, 2, 11, 14, 12, shade(colour, 0.8))
        return img

    def top():
        """Bottle caps in a 4 x 6 crate, amber glass necks between the dividers."""
        img = fill(shade(colour, 0.6), grain=3, seed=seed + 1)
        frame(img, 0, 0, 16, 16, colour, 1)
        for i in range(4):
            for j in range(4):
                cx, cy = 2.5 + i * 3.7, 2.5 + j * 3.7
                disc(img, cx, cy, 1.6, (120, 72, 20))
                disc(img, cx, cy, 1.0, (210, 196, 150) if (i + j) % 2 else (190, 40, 30))
        return img
    return side, top


TEXTURES["crate_red_side"], TEXTURES["crate_red_top"] = _crate((168, 36, 32), 28)
TEXTURES["crate_green_side"], TEXTURES["crate_green_top"] = _crate((40, 110, 58), 30)


@texture("wire_shelf")
def _wire_shelf():
    """A chrome wire shelf seen from above: wires every 2 texels, open between (cutout)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(0, 16, 2):
        rect(img, x, 0, x + 1, 16, (200, 204, 208))
    rect(img, 0, 0, 16, 1, (214, 218, 222))
    rect(img, 0, 7, 16, 8, (214, 218, 222))
    rect(img, 0, 15, 16, 16, (214, 218, 222))
    return img


@texture("drip_tray")
def _drip_tray():
    img = metal((188, 192, 196), seed=31)
    for y in range(2, 15, 2):
        rect(img, 1, y, 15, y + 1, (70, 72, 76))
    frame(img, 0, 0, 16, 16, (210, 214, 218))
    return img


flat("handle_black", (26, 26, 28), grain=2, seed=32)
flat("handle_red", (160, 30, 26), grain=3, seed=33)
flat("handle_gold", (200, 160, 60), grain=3, seed=34)
flat("handle_blue", (30, 70, 150), grain=3, seed=35)


@texture("dartboard")
def _dartboard():
    """A bristle dartboard, 64 px across its full 225 mm radius (so 1 texel ~ 7 mm): black
    number ring, then the doubles, outer singles, trebles and inner singles in their 20
    segments, the 25 and the bull."""
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0
    k = c / 225.0
    black, cream = (28, 26, 24), (226, 212, 178)
    red, green = (190, 34, 32), (26, 120, 60)
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            r = math.hypot(dx, dy) / k
            if r > 225:
                continue
            # 20 segments, 20 at the top, each 18 degrees wide centred on its angle
            ang = (math.degrees(math.atan2(dx, -dy)) + 9.0) % 360.0
            seg = int(ang // 18)
            even = seg % 2 == 0
            if r <= 6.35:
                col = red
            elif r <= 15.9:
                col = green
            elif r <= 99:
                col = black if even else cream
            elif r <= 107:
                col = red if even else green
            elif r <= 162:
                col = black if even else cream
            elif r <= 170:
                col = red if even else green
            else:
                col = black
                # a white tick where each number sits
                frac = (ang % 18.0) / 18.0
                if 190 < r < 212 and 0.35 < frac < 0.65:
                    col = (220, 220, 220)
            px[x, y] = col + (255,)
    # the wire spider at the ring edges is too fine to draw; a lit highlight upper left instead
    return img


flat("surround", (20, 20, 22), grain=3, seed=36)


def _dart_flight(colour, seed):
    return lambda: fill(colour, grain=3, seed=seed)


TEXTURES["flight_red"] = _dart_flight((200, 30, 30), 37)
TEXTURES["flight_blue"] = _dart_flight((30, 80, 190), 38)


@texture("card_back")
def _card_back():
    """A card back, drawn in the 22 x 31 texel window a card's up face samples: red with a white
    border and a diamond lattice."""
    img = Image.new("RGBA", (32, 32), (255, 255, 255, 255))
    rect(img, 0, 0, 22, 31, (246, 244, 238))
    rect(img, 2, 2, 20, 29, (176, 28, 36))
    for y in range(2, 29):
        for x in range(2, 20):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                img.putpixel((x, y), (214, 90, 96, 255))
    frame(img, 2, 2, 20, 29, (130, 16, 24))
    return img


@texture("card_face")
def _card_face():
    """The ace of hearts in the same window."""
    img = Image.new("RGBA", (32, 32), (255, 255, 255, 255))
    rect(img, 0, 0, 22, 31, (248, 247, 242))
    red = (196, 24, 32)

    def heart(cx, cy, s):
        disc(img, cx - s * 0.5, cy - s * 0.3, s * 0.55, red)
        disc(img, cx + s * 0.5, cy - s * 0.3, s * 0.55, red)
        for i in range(int(s * 1.6)):
            w = s * 1.05 - i * 0.66
            if w <= 0:
                break
            rect(img, int(round(cx - w)), int(cy - s * 0.2 + i), int(round(cx + w)),
                 int(cy - s * 0.2 + i + 1), red)
    heart(11, 15, 4)
    lsc.draw_text(img, "A", 2, 2, red)
    heart(3.5, 9.5, 1.3)
    lsc.draw_text(img, "A", 17, 24, red)
    return img


@texture("card_edge")
def _card_edge():
    img = fill((236, 234, 228), grain=2, seed=39)
    for y in range(0, 16, 2):
        rect(img, 0, y, 16, y + 1, (206, 204, 198))
    return img


@texture("mirror_glass")
def _mirror_glass():
    """Silvered glass: a cool grey-blue gradient with two soft diagonal highlights."""
    size = 32
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = y / (size - 1.0)
            base = (170 - 30 * t, 184 - 28 * t, 196 - 24 * t)
            d = (x + y * 0.45) % 32
            if 6 <= d < 9 or 13 <= d < 14:
                base = tuple(v + 30 for v in base)
            px[x, y] = clamp(base) + (255,)
    return img


# 5 x 5 capitals for the restroom legends: the shared 3 x 5 font's M and W close up into H.
_LEGEND = {
    "W": ["10001", "10001", "10101", "11011", "10001"],
    "O": ["01110", "10001", "10001", "10001", "01110"],
    "M": ["10001", "11011", "10101", "10001", "10001"],
    "E": ["11111", "10000", "11110", "10000", "11111"],
    "N": ["10001", "11001", "10101", "10011", "10001"],
}


def _legend(img, text, cx, y, colour, sx=1, sy=2):
    """Draws text in _LEGEND's letters, centred on cx, each letter pixel sx wide and sy tall
    (condensed, as a door sign's legend is, so WOMEN fits the plate)."""
    width = (len(text) * 6 - 1) * sx
    x = int(round(cx - width / 2.0))
    for i, ch in enumerate(text):
        for gy, row in enumerate(_LEGEND[ch]):
            for gx, bit in enumerate(row):
                if bit == "1":
                    X = x + (i * 6 + gx) * sx
                    rect(img, X, y + gy * sy, X + sx, y + (gy + 1) * sy, colour)


def _restroom(female):
    """A restroom sign plate: ADA blue with a white border, pictogram, legend and the braille
    beneath it. The plate is x 5..11, y 4..13 on the model, so texels 40..88 x 24..96 of this
    128 px texture (8 texels to the model pixel)."""
    def draw():
        blue = (28, 70, 150)
        white = (240, 242, 246)
        img = fill(blue, size=128, grain=2, seed=40 if female else 41)
        x0, y0, x1, y1 = lsc.north_region((5, 4), (11, 13), 128)
        frame(img, x0 + 2, y0 + 2, x1 - 2, y1 - 2, white, 2)
        cx = (x0 + x1) // 2
        top = y0 + 7
        disc(img, cx, top + 4.5, 4.5, white)                    # head
        if female:
            rect(img, cx - 4, top + 11, cx + 4, top + 14, white)    # shoulders
            rect(img, cx - 11, top + 11, cx + 11, top + 13, white)
            for i in range(15):                                     # dress, widening
                w = 4 + i * 0.6
                rect(img, int(round(cx - w)), top + 14 + i, int(round(cx + w)),
                     top + 15 + i, white)
            for sx in (-1, 1):                                      # arms
                rect(img, cx + sx * 10 - 2, top + 11, cx + sx * 10 + 2, top + 21, white)
            rect(img, cx - 5, top + 29, cx - 1, top + 37, white)    # legs
            rect(img, cx + 1, top + 29, cx + 5, top + 37, white)
        else:
            rect(img, cx - 6, top + 11, cx + 6, top + 26, white)    # torso
            for sx in (-1, 1):                                      # arms, a gap from the torso
                rect(img, cx + sx * 9 - 2, top + 11, cx + sx * 9 + 2, top + 25, white)
            rect(img, cx - 6, top + 26, cx - 1, top + 37, white)    # legs
            rect(img, cx + 1, top + 26, cx + 6, top + 37, white)
        text = "WOMEN" if female else "MEN"
        _legend(img, text, cx, top + 40, white)
        # Grade 2 braille is not something to fake letter by letter: a plain row of raised dots
        # in two cells per letter reads as braille from any distance a player sees it at.
        rng = random.Random(42 if female else 43)
        n = len(text)
        bx = cx - (n * 7 - 3) // 2
        for i in range(n):
            for dy in range(3):
                for dx in range(2):
                    if rng.random() < 0.5 or (dy == 0 and dx == 0):
                        rect(img, bx + i * 7 + dx * 3, top + 51 + dy * 3,
                             bx + i * 7 + dx * 3 + 2, top + 51 + dy * 3 + 2, white)
        return img
    return draw


TEXTURES["restroom_female"] = _restroom(True)
TEXTURES["restroom_male"] = _restroom(False)
flat("sign_edge", (22, 56, 124), grain=2, seed=44)


@texture("spa_shell")
def _spa_shell():
    """The acrylic shell of a spa: pearl white with a faint marble fleck."""
    rng = random.Random(45)
    img = fill((226, 228, 230), grain=3, seed=45)
    for _ in range(20):
        x, y = rng.randrange(16), rng.randrange(16)
        img.putpixel((x, y), (200, 204, 210, 255))
    return img


@texture("spa_water")
def _spa_water():
    """Water in a lit spa: turquoise with brighter ripple lines."""
    size = 32
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            w = math.sin((x * 0.55 + math.sin(y * 0.4) * 2.2)) + math.sin(y * 0.7 + x * 0.2)
            base = (40, 150, 170)
            if w > 1.25:
                base = (120, 210, 222)
            elif w > 0.6:
                base = (70, 180, 196)
            px[x, y] = base + (255,)
    return img


flat("spa_pillow", (54, 56, 60), grain=3, seed=46)
flat("spa_skirt", (70, 50, 36), grain=4, seed=47)


@texture("chain_link")
def _chain_link():
    """One link of a chain seen flat on: an oval ring, cut out. It is stretched to a 1.6 x 2.6
    face, so the ring's sides are drawn thicker in v than they look."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx = (x + 0.5 - 8) / 8.0
            dy = (y + 0.5 - 8) / 8.0
            r = math.hypot(dx, dy * 0.95)
            if 0.52 <= r <= 1.0:
                k = 1.0 + 0.35 * (-dx - dy) * 0.5
                px[x, y] = shade((118, 118, 124), k) + (255,)
    return img


flat("chain_side", (70, 70, 74), grain=3, seed=48)


@texture("boards")
def _boards():
    """Eight rough boards, one to each 64 x 8 band: grey-brown weathered pine, split ends and
    two nail heads near each end."""
    rng = random.Random(49)
    img = Image.new("RGBA", (64, 64))
    bases = [(128, 100, 72), (116, 92, 68), (138, 110, 80), (110, 86, 64),
             (124, 104, 82), (132, 102, 70), (104, 84, 64), (120, 96, 70)]
    for b in range(8):
        band = wood(bases[b], size=64, seed=50 + b, streak=0.12)
        crop = band.crop((0, 0, 64, 8))
        img.paste(crop, (0, b * 8))
        y0 = b * 8
        rect(img, 0, y0, 64, y0 + 1, shade(bases[b], 0.72))
        rect(img, 0, y0 + 7, 64, y0 + 8, shade(bases[b], 0.66))
        for x in (3, 60):
            for ny in (y0 + 2, y0 + 5):
                rect(img, x, ny, x + 2, ny + 1, (58, 58, 60))
                img.putpixel((x, ny), (96, 96, 100, 255))
        if rng.random() < 0.6:
            rect(img, 0, y0 + 3, rng.randrange(5, 12), y0 + 4, shade(bases[b], 0.55))
    return img


# --- wooden barrel textures ---
BARREL_OAK = (150, 100, 58)


@texture("barrel_stave")
def _barrel_stave():
    """Staves running along u, a dark joint every 4 texels (a stave ~ 2 px, 12 cm, wide)."""
    img = wood(BARREL_OAK, seed=60, streak=0.12)
    for y in range(0, 32, 4):
        rect(img, 0, y, 32, y + 1, shade(BARREL_OAK, 0.55))
    return img


@texture("barrel_stave_v")
def _barrel_stave_v():
    img = wood(BARREL_OAK, seed=60, streak=0.12, vertical=True)
    for x in range(0, 32, 4):
        rect(img, x, 0, x + 1, 32, shade(BARREL_OAK, 0.55))
    return img


@texture("barrel_head")
def _barrel_head():
    """A barrel head: boards across it with end grain, a dark chamfer where it meets the
    staves, cut to the octagon it fills (the square texture's corners are clear)."""
    size = 64
    img = wood(shade(BARREL_OAK, 0.92), size=size, seed=61, vertical=True, streak=0.09)
    for x in range(0, size, 13):
        rect(img, x, 0, x + 1, size, shade(BARREL_OAK, 0.5))
    px = img.load()
    c = size / 2.0
    for y in range(size):
        for x in range(size):
            dx, dy = abs(x + 0.5 - c), abs(y + 0.5 - c)
            d = max(dx, dy, (dx + dy) * math.sqrt(0.5))
            if d > c:
                px[x, y] = (0, 0, 0, 0)
            elif d > c - 3:
                r, g, b, a = px[x, y]
                px[x, y] = shade((r, g, b), 0.62) + (255,)
    disc(img, c, c * 0.55, 3, shade(BARREL_OAK, 0.45))      # bung
    disc(img, c, c * 0.55, 2, shade(BARREL_OAK, 0.7))
    return img


@texture("hoop")
def _hoop():
    img = metal((70, 70, 74), seed=62)
    rect(img, 0, 0, 16, 1, (110, 110, 116))
    return img


# ------------------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------------------
BLOCKS = []   # (registry, java class, facing kind, model json, bounding box note)


def add(registry, kind, mdl):
    BLOCKS.append((registry, kind, mdl))


def tx(*names):
    d = {n: T(n) for n in names}
    d["particle"] = T(names[0])
    return d


# --- wine rack: a 1 m cube with an X across it, bottles in the four bins -------------------
def winerack():
    els = []
    z0, z1 = 8, 16
    els.append(B([0, 15, z0], [16, 16, z1], "oak"))
    els.append(B([0, 0, z0], [16, 1, z1], "oak"))
    els.append(B([0, 1, z0], [1, 15, z1], "oak_v"))
    els.append(B([15, 1, z0], [16, 15, z1], "oak_v"))
    els.append(B([1, 1, 15], [15, 15, 16], "oak_v", faces=("north", "south")))  # back panel
    els.append(B([1, 1, 9.5], [15, 15, 9.51], "wine_bottles", faces=("north",),
                 uv=[1, 1, 15, 15]))
    # each bottle's shoulder, neck and foil capsule, the capsule standing out past the front
    # of the rack
    for (x, y, g, f) in wine_bottles():
        gu, gv = (g % 2) * 8, (g // 2) * 8
        fu, fv = (f % 2) * 8, (f // 2) * 8
        els.append(B([x - 0.55, y - 0.55, 9.0], [x + 0.55, y + 0.55, 9.5], "wine_glass",
                     faces=("north", "east", "west", "up", "down"), uv=[gu, gv, gu + 8, gv + 8]))
        els.append(B([x - 0.3, y - 0.3, 8.1], [x + 0.3, y + 0.3, 9.0], "wine_glass",
                     faces=("east", "west", "up", "down"), uv=[gu, gv, gu + 8, gv + 8]))
        els.append(B([x - 0.33, y - 0.33, 7.2], [x + 0.33, y + 0.33, 8.1], "wine_foils",
                     faces=("north", "east", "west", "up", "down"),
                     uv=[fu, fv, fu + 8, fv + 8]))
    half = 10.4
    for ang in (45, -45):
        els.append(B([8 - half, 7.6, z0 + 0.2], [8 + half, 8.4, z1 - 1], "oak",
                     faces=("north", "up", "down"),
                     uv={"north": [0, 7.6, 16, 8.4], "up": [0, 0, 16, 7], "down": [0, 0, 16, 7]},
                     rot=("z", ang, [8, 8, 12])))
    return model(tx("oak", "oak_v", "wine_bottles", "wine_glass", "wine_foils"), els)


add("winerack", "nsewud", winerack())


# --- beer rack: steel shelving, kegs below and crates of bottles above ---------------------
def beerrack():
    els = []
    x0, x1, z0, z1 = -8, 24, 9, 16
    for x in (x0, x1 - 0.8):
        for z in (z0, z1 - 0.8):
            els.append(B([x, 0, z], [x + 0.8, 30, z + 0.8], "steel_v",
                         faces=("north", "south", "east", "west", "up")))
    for y in (1.0, 10.0, 19.0, 27.2):
        els.append(B([x0, y, z0], [x1, y + 0.4, z1], "wire_shelf", faces=("up", "down"),
                     uv=[0, 0, 16, 16]))
        els.append(B([x0, y, z0 - 0.01], [x1, y + 0.4, z0], "steel",
                     faces=("north",), uv=[0, 0, 16, 0.4]))
    # three half-barrel kegs on the bottom shelf
    for cx in (-2.5, 6.5, 15.5):
        els += post(cx, 12.5, 3.1, 1.4, 8.8, "keg", top=True, bottom=False)
    for e in els[-12:]:
        if "up" in e["faces"]:
            e["faces"]["up"]["texture"] = "#keg_top"
    # crates: 6.4 wide, 4.4 high, 6 deep
    def crate(x, y, colour):
        per = {"up": "crate_%s_top" % colour}
        return B([x, y, 9.6], [x + 6.4, y + 4.4, 15.6], "crate_%s_side" % colour, per=per,
                 uv={"north": [0, 0, 16, 16], "south": [0, 0, 16, 16], "east": [0, 0, 16, 16],
                     "west": [0, 0, 16, 16], "up": [0, 0, 16, 16], "down": [0, 0, 16, 16]})
    xs = [-7, 0.8, 8.6, 16.4]
    for i, x in enumerate(xs):
        els.append(crate(x, 10.4, "red" if i % 2 == 0 else "green"))
    for x in (xs[0], xs[2]):
        els.append(crate(x, 14.8, "green"))
    for i, x in enumerate(xs[:3]):
        els.append(crate(x, 19.4, "green" if i % 2 == 0 else "red"))
    els.append(crate(xs[3] - 1, 27.6, "red"))
    els.append(crate(xs[0] + 1, 27.6, "green"))
    return model(tx("steel", "steel_v", "wire_shelf", "keg", "keg_top", "crate_red_side",
                    "crate_red_top", "crate_green_side", "crate_green_top"), els, gui(0.3, -2.2))


add("beerrack", "nsewud", beerrack())


# --- beer tap: a T tower of three faucets on a drip tray, sat on the bar top ---------------
def beertap():
    els = []
    els.append(B([3.5, 0, 5], [12.5, 0.6, 10], "drip_tray", per={"north": "steel",
                "south": "steel", "east": "steel", "west": "steel", "down": "steel"}))
    els.append(B([6.6, 0.6, 7.3], [9.4, 0.9, 9.7], "chrome"))              # tower flange
    els += post(8, 8.5, 0.75, 0.9, 5.4, "chrome", bottom=False)
    els += pipe_x(6.0, 8.5, 0.8, 4.2, 11.8, "chrome")
    for x, handle in ((5.4, "handle_black"), (8.0, "handle_red"), (10.6, "handle_gold")):
        els.append(B([x - 0.4, 5.4, 6.6], [x + 0.4, 6.4, 7.8], "chrome"))       # faucet body
        els.append(B([x - 0.3, 4.3, 6.6], [x + 0.3, 5.4, 7.2], "chrome"))       # spout
        els.append(B([x - 0.15, 6.4, 7.0], [x + 0.15, 6.9, 7.3], "chrome"))     # lever collar
        els.append(B([x - 0.35, 6.9, 6.9], [x + 0.35, 10.2, 7.45], handle))     # tap handle
    return model(tx("chrome", "drip_tray", "steel", "handle_black", "handle_red",
                    "handle_gold"), els)


add("beertap", "nsewud", beertap())


# --- pool table: 2.75 x 1.5 m, slate bed under green felt, six pockets --------------------
def billardtable():
    els = []
    X0, X1, Z0, Z1 = -14, 30, -4, 20
    for x in (-12.5, 25.5):
        for z in (-2.5, 15.5):
            els.append(B([x, 0, z], [x + 3, 9, z + 3], "walnut_v", faces=ALL[:4] + ("down",)))
            els.append(B([x - 0.3, 0, z - 0.3], [x + 3.3, 1, z + 3.3], "walnut",
                         faces=ALL[:5]))
    els.append(B([X0 + 1, 9, Z0 + 1], [X1 - 1, 11.5, Z1 - 1], "walnut",
                 faces=("north", "south", "east", "west", "down")))
    els.append(B([X0 + 2, 11.5, Z0 + 2], [X1 - 2, 11.8, Z1 - 2], "felt", faces=("up",),
                 uv=[0, 0, 16, 16]))
    # rails: the wooden top rail round the bed
    els.append(B([X0, 11, Z0], [X1, 13, Z0 + 2], "walnut"))
    els.append(B([X0, 11, Z1 - 2], [X1, 13, Z1], "walnut"))
    els.append(B([X0, 11, Z0 + 2], [X0 + 2, 13, Z1 - 2], "walnut", faces=("east", "west",
                 "up", "down")))
    els.append(B([X1 - 2, 11, Z0 + 2], [X1, 13, Z1 - 2], "walnut", faces=("east", "west",
                 "up", "down")))
    # cushions: felt noses inside the rails
    els.append(B([X0 + 3, 11.8, Z0 + 2], [X1 - 3, 12.6, Z0 + 2.8], "felt",
                 faces=("south", "up")))
    els.append(B([X0 + 3, 11.8, Z1 - 2.8], [X1 - 3, 12.6, Z1 - 2], "felt",
                 faces=("north", "up")))
    els.append(B([X0 + 2, 11.8, Z0 + 3], [X0 + 2.8, 12.6, Z1 - 3], "felt",
                 faces=("east", "up")))
    els.append(B([X1 - 2.8, 11.8, Z0 + 3], [X1 - 2, 12.6, Z1 - 3], "felt",
                 faces=("west", "up")))
    # pockets: black, cut into the rail at the corners and at the middle of each long side
    for x in (X0 + 1, 7, X1 - 3):
        for z in (Z0 + 1, Z1 - 3):
            w = 2 if x == 7 else 2
            els.append(B([x, 11.2, z], [x + w, 13.05, z + 2], "pocket",
                         faces=("up", "north", "south", "east", "west")))
    # the balls: the cue ball at the head spot, the fifteen racked at the foot spot
    r = 0.45
    els.append(B([-6 - r, 11.8, 8 - r], [-6 + r, 12.7, 8 + r], "balls", uv=[0, 0, 4, 4]))
    n = 1
    for row in range(5):
        for j in range(row + 1):
            cx = 19.5 + row * 0.8
            cz = 8 + (j - row / 2.0) * 0.92
            i = n
            n += 1
            u, v = (i % 4) * 4, (i // 4) * 4
            els.append(B([cx - r, 11.8, cz - r], [cx + r, 12.7, cz + r], "balls",
                         faces=("north", "south", "east", "west", "up"),
                         uv=[u, v, u + 4, v + 4]))
    return model(tx("walnut", "walnut_v", "felt", "pocket", "balls"), els, gui(0.28, -0.5))


add("billardtable", "nsew", billardtable())


# --- dartboard: a 451 mm board in its black surround, three darts in it --------------------
def dartboard():
    els = []
    cx, cy = 8, 8.5
    els += refit(lsc._octagon("z", cx, cy, 5.6, 14.8, 16, "surround", True))
    els += refit(lsc._octagon("z", cx, cy, 3.7, 13.8, 14.8, "surround", False))
    s = 3.9
    els.append(B([cx - s, cy - s, 13.78], [cx + s, cy + s, 13.8], "dartboard", faces=("north",),
                 uv=[0, 0, 16, 16]))
    for dx, dy, tex in ((0.6, 1.2, "flight_red"), (-1.1, -0.4, "flight_blue"),
                        (1.8, -1.6, "flight_red")):
        x, y = cx + dx, cy + dy
        els.append(B([x - 0.15, y - 0.15, 11.2], [x + 0.15, y + 0.15, 13.8], "chrome",
                     faces=("north", "east", "west", "up", "down")))
        els.append(B([x - 0.8, y - 0.05, 10.2], [x + 0.8, y + 0.05, 11.4], tex,
                     faces=("up", "down", "north")))
        els.append(B([x - 0.05, y - 0.8, 10.2], [x + 0.05, y + 0.8, 11.4], tex,
                     faces=("east", "west", "north")))
    return model(tx("surround", "dartboard", "chrome", "flight_red", "flight_blue"), els)


add("dartboard", "nsew", dartboard())


# --- a deck of cards and three dealt beside it ---------------------------------------------
def carddeck():
    els = []
    w, l = 2.2, 3.1
    x, z = 5.6, 6.2
    els.append(B([x, 0, z], [x + w, 0.5, z + l], "card_edge", per={"up": "card_back"},
                 uv={"up": [0, 0, 11, 15.5], "north": [0, 0, 8, 1], "south": [0, 0, 8, 1],
                     "east": [0, 0, 8, 1], "west": [0, 0, 8, 1], "down": [0, 0, 11, 15.5]},
                 faces=("north", "south", "east", "west", "up")))
    for (cx, cz, ang, tex, y) in ((10.2, 7.4, 22.5, "card_back", 0.02),
                                  (10.8, 9.6, -22.5, "card_back", 0.12),
                                  (8.6, 11.0, 0, "card_face", 0.22)):
        el = B([cx - w / 2, y, cz - l / 2], [cx + w / 2, y + 0.06, cz + l / 2], tex,
               faces=("up", "down"), per={"down": "card_edge"},
               uv={"up": [0, 0, 11, 15.5], "down": [0, 0, 11, 15.5]},
               rot=("y", ang, [cx, y, cz]) if ang else None)
        els.append(el)
    return model(tx("card_edge", "card_back", "card_face"), els)


add("carddeck", "nsew", carddeck())


# --- office chair: five-star base on casters, gas lift, seat, mesh back, arms ---------------
def officechair():
    els = []
    c = 8.0
    L = 5.0
    # leg directions (degrees from north, clockwise seen from above) as near to 72 degrees
    # apart as an element's 22.5 degree turns allow, symmetric about the front leg
    for d in (0, 67.5, 135, 225, 292.5):
        base = int(round(d / 90.0)) * 90 % 360
        res = d - base
        if res > 45:
            base, res = base + 90, res - 90
        # a half-leg from the centre out along the base axis
        if base == 0:
            frm, to = [c - 0.45, 1.0, c - L], [c + 0.45, 1.8, c]
            tip = (c, c - L + 0.4)
        elif base == 90:
            frm, to = [c, 1.0, c - 0.45], [c + L, 1.8, c + 0.45]
            tip = (c + L - 0.4, c)
        elif base == 180:
            frm, to = [c - 0.45, 1.0, c], [c + 0.45, 1.8, c + L]
            tip = (c, c + L - 0.4)
        else:
            frm, to = [c - L, 1.0, c - 0.45], [c, 1.8, c + 0.45]
            tip = (c - L + 0.4, c)
        rot = ("y", -res, [c, 1.0, c]) if res else None
        els.append(B(frm, to, "black_plastic", faces=("north", "south", "east", "west", "up"),
                     rot=rot))
        # where the tip lands after the turn (Minecraft turns +angle about y as the previewer
        # does: x' = x cos + z sin, z' = -x sin + z cos)
        a = math.radians(-res)
        dx, dz = tip[0] - c, tip[1] - c
        tx_ = c + dx * math.cos(a) + dz * math.sin(a)
        tz_ = c - dx * math.sin(a) + dz * math.cos(a)
        els.append(B([tx_ - 0.55, 0, tz_ - 0.55], [tx_ + 0.55, 1.0, tz_ + 0.55], "black_plastic"))
    els += post(c, c, 1.2, 1.0, 2.2, "black_plastic", bottom=False)    # hub
    els += post(c, c, 0.55, 2.2, 6.4, "chrome", top=False, bottom=False)  # gas lift
    els.append(B([6.2, 6.4, 6.4], [9.8, 7.1, 9.8], "black_plastic"))       # mechanism
    els.append(B([3.8, 7.1, 3.6], [12.2, 8.5, 11.8], "fabric_seat"))        # seat, 0.53 m
    # back: a spine rising from the rear of the seat, the mesh back on it
    els.append(B([7.2, 6.8, 11.8], [8.8, 10.2, 12.6], "black_plastic"))
    els.append(B([4.4, 9.4, 12.2], [11.6, 16.6, 12.8], "mesh",
                 per={"south": "black_plastic", "east": "black_plastic", "west": "black_plastic",
                      "up": "black_plastic", "down": "black_plastic"}))
    els.append(B([4.2, 9.2, 12.8], [11.8, 16.8, 13.3], "black_plastic"))
    # arms
    for x in (2.9, 12.3):
        els.append(B([x + 0.2, 7.1, 8.2], [x + 0.6, 11.0, 9.2], "black_plastic"))
        els.append(B([x - 0.1, 7.1, 9.2], [x + 0.9, 7.6, 10.4], "black_plastic"))
        els.append(B([x, 11.0, 5.4], [x + 0.8, 11.6, 10.6], "fabric_seat"))
    return model(tx("black_plastic", "chrome", "fabric_seat", "mesh"), els)


add("officechair", "nsewud", officechair())


# --- cast-iron column radiator: eleven sections on two feet, valve and bleed -----------------
def csmradiator():
    els = []
    n = 10
    pitch = 1.36
    x0 = 8 - (n * pitch) / 2 + 0.18
    for i in range(n):
        x = x0 + i * pitch
        els.append(B([x, 1.8, 12.0], [x + 1.0, 10.2, 15.2], "iron_paint"))
        els.append(B([x + 0.05, 10.2, 12.4], [x + 0.95, 10.8, 14.8], "iron_paint"))
        els.append(B([x + 0.05, 1.2, 12.4], [x + 0.95, 1.8, 14.8], "iron_paint",
                     faces=("north", "south", "east", "west", "down")))
    xe = x0 + (n - 1) * pitch + 1.0
    for y in (2.4, 9.0):
        els.append(B([x0 + 0.5, y, 13.0], [xe - 0.5, y + 1.0, 14.2], "iron_paint_dark",
                     faces=("north", "south", "up", "down")))
    for x in (x0 + pitch, x0 + (n - 2) * pitch):
        els.append(B([x - 0.1, 0, 12.2], [x + 1.1, 1.2, 15.0], "iron_paint_dark"))
    # the valve on the left end (seen from the front, x high) and the bleed screw on the right
    els += pipe_x(2.9, 13.6, 0.45, xe, 15.3, "brass")
    els += post(15.3, 13.6, 0.4, 0, 3.4, "brass", bottom=False)
    els.append(B([14.9, 3.4, 13.2], [15.7, 4.2, 14.0], "brass"))
    els.append(B([14.7, 4.2, 13.0], [15.9, 4.6, 14.2], "red_plastic"))
    els.append(B([x0 - 0.5, 9.4, 13.4], [x0, 9.9, 13.9], "brass"))
    return model(tx("iron_paint", "iron_paint_dark", "brass", "red_plastic"), els)


add("csmradiator", "nsewud", csmradiator())


# --- coat tree: a 1.8 m turned pole on a cross foot, hooks at the top, a hat on one ----------
def coatrack():
    els = []
    c = 8.0
    els.append(B([c - 5, 0, c - 0.6], [c + 5, 0.9, c + 0.6], "walnut",
                 rot=("y", 45, [c, 0, c])))
    els.append(B([c - 0.6, 0, c - 5], [c + 0.6, 0.9, c + 5], "walnut_v",
                 rot=("y", 45, [c, 0, c])))
    els += post(c, c, 1.1, 0, 2.6, "walnut_v")
    els += post(c, c, 0.55, 2.6, 28.4, "walnut_v", bottom=False)
    els += post(c, c, 0.85, 22.6, 23.4, "walnut_v")                   # collar under the hooks
    els += post(c, c, 0.8, 28.4, 29.4, "walnut_v")                    # finial
    # four hooks sweeping up and out: an arm turned 45 degrees and an upturned tip
    for d in ("n", "s", "e", "w"):
        if d in ("e", "w"):
            sgn = 1 if d == "e" else -1
            frm = [c, 24.6, c - 0.3] if sgn > 0 else [c - 3.4, 24.6, c - 0.3]
            to = [c + 3.4, 25.2, c + 0.3] if sgn > 0 else [c, 25.2, c + 0.3]
            els.append(B(frm, to, "walnut", rot=("z", 45 * sgn, [c, 24.9, c])))
            tipx = c + sgn * 3.4 * math.cos(math.pi / 4)
            els.append(B([tipx - 0.35, 26.9, c - 0.35], [tipx + 0.35, 28.0, c + 0.35],
                         "walnut_v"))
        else:
            sgn = 1 if d == "s" else -1
            frm = [c - 0.3, 24.6, c] if sgn > 0 else [c - 0.3, 24.6, c - 3.4]
            to = [c + 0.3, 25.2, c + 3.4] if sgn > 0 else [c + 0.3, 25.2, c]
            els.append(B(frm, to, "walnut", rot=("x", -45 * sgn, [c, 24.9, c])))
            tipz = c + sgn * 3.4 * math.cos(math.pi / 4)
            els.append(B([c - 0.35, 26.9, tipz - 0.35], [c + 0.35, 28.0, tipz + 0.35],
                         "walnut_v"))
    return model(tx("walnut", "walnut_v"), els, gui(0.4, -3.0))


add("coatrack", "nsewud", coatrack())


# --- tall wall mirror: 0.75 x 1.85 m in a walnut frame ---------------------------------------
def tallwallmirror():
    els = []
    x0, x1, y0, y1 = 2, 14, 1, 30.6
    els.append(B([x0, y0, 14.8], [x1, y0 + 1.2, 16], "walnut"))
    els.append(B([x0, y1 - 1.2, 14.8], [x1, y1, 16], "walnut"))
    els.append(B([x0, y0 + 1.2, 14.8], [x0 + 1.2, y1 - 1.2, 16], "walnut_v",
                 faces=("north", "south", "east", "west")))
    els.append(B([x1 - 1.2, y0 + 1.2, 14.8], [x1, y1 - 1.2, 16], "walnut_v",
                 faces=("north", "south", "east", "west")))
    els.append(B([x0 + 1.2, y0 + 1.2, 15.3], [x1 - 1.2, y1 - 1.2, 15.8], "mirror_glass",
                 faces=("north",), uv=[0, 0, 16, 16]))
    els.append(B([x0 + 1.2, y0 + 1.2, 15.8], [x1 - 1.2, y1 - 1.2, 16], "walnut_v",
                 faces=("south",)))
    return model(tx("walnut", "walnut_v", "mirror_glass"), els, gui(0.45, -3.3))


add("tallwallmirror", "nsewud", tallwallmirror())


# --- restroom signs: a 6 x 9 in plate ------------------------------------------------------
def restroom(female):
    tex = "restroom_female" if female else "restroom_male"
    els = [B([5, 4, 15.4], [11, 13, 16], tex, per={"east": "sign_edge", "west": "sign_edge",
             "up": "sign_edge", "down": "sign_edge", "south": "sign_edge"})]
    return model(tx(tex, "sign_edge"), els)


add("restroomsignfemale", "nsewud", restroom(True))
add("restroomsignmale", "nsewud", restroom(False))


# --- hot tub: a 2.2 m square spa, cedar cabinet, acrylic lip, water, headrests ----------------
def hottub():
    els = []
    lo, hi = -9.5, 25.5
    top = 14.0
    lip = 12.6
    # cabinet: cedar cladding on a dark skirt
    for (frm, to, faces) in (([lo, 0.6, lo], [hi, lip, lo + 1], ("north", "up")),
                             ([lo, 0.6, hi - 1], [hi, lip, hi], ("south", "up")),
                             ([lo, 0.6, lo + 1], [lo + 1, lip, hi - 1], ("west", "up")),
                             ([hi - 1, 0.6, lo + 1], [hi, lip, hi - 1], ("east", "up"))):
        els.append(B(frm, to, "cedar_slats", faces=faces, uv=[0, 0, 16, 5.5]))
    els.append(B([lo + 0.2, 0, lo + 0.2], [hi - 0.2, 0.6, hi - 0.2], "spa_skirt",
                 faces=("north", "south", "east", "west", "down"), uv=[0, 0, 16, 0.6]))
    # the acrylic lip: a frame 3 px wide whose inner faces are the shell down to the water
    w = 3.2
    water = 11.8
    for (frm, to) in (([lo - 0.2, water, lo - 0.2], [hi + 0.2, top, lo + w]),
                      ([lo - 0.2, water, hi - w], [hi + 0.2, top, hi + 0.2]),
                      ([lo - 0.2, water, lo + w], [lo + w, top, hi - w]),
                      ([hi - w, water, lo + w], [hi + 0.2, top, hi - w])):
        els.append(B(frm, to, "spa_shell", uv=[0, 0, 16, 2.2]))
    els.append(B([lo + w, water, lo + w], [hi - w, water + 0.4, hi - w], "spa_water",
                 faces=("up",), uv=[0, 0, 16, 16]))
    # headrest pillows in two corners, the control panel on the lip
    els.append(B([lo + w, water + 0.6, lo + w], [lo + w + 3, top + 0.6, lo + w + 0.9],
                 "spa_pillow"))
    els.append(B([hi - w - 3, water + 0.6, hi - w - 0.9], [hi - w, top + 0.6, hi - w],
                 "spa_pillow"))
    els.append(B([6, top, lo + 0.6], [10, top + 0.3, lo + 2.2], "black_plastic"))
    return model(tx("cedar_slats", "spa_skirt", "spa_shell", "spa_water", "spa_pillow",
                    "black_plastic"), els, gui(0.3, -0.8))


add("hottub", "nsewud", hottub())


# --- hanging chain: links alternating flat and edge on --------------------------------------
def chains():
    els = []
    pitch = 1.9
    for i in range(8):
        y = i * pitch
        if i % 2 == 0:
            els.append(B([7.2, y, 7.92], [8.8, y + 2.6, 8.08], "chain_link",
                         faces=("north", "south"), uv=[0, 0, 16, 16]))
        else:
            els.append(B([7.92, y, 7.2], [8.08, y + 2.6, 8.8], "chain_link",
                         faces=("east", "west"), uv=[0, 0, 16, 16]))
    return model(tx("chain_link"), els)


add("chains", "nsewud", chains())


# --- boarded up: rough boards nailed across an opening --------------------------------------
def boardedwoodplanks():
    els = []
    rows = [(1.0, -2.0, 18.0, 0), (7.4, -1.4, 17.6, 1), (13.6, -2.2, 18.2, 2),
            (19.8, -1.6, 17.4, 3), (26.2, -2.0, 18.0, 4)]
    for (y, x0, x1, b) in rows:
        els.append(B([x0, y, 15.2], [x1, y + 3.2, 16], "boards",
                     uv={"north": [0, 2 * b, 16, 2 * b + 2], "up": [0, 2 * b, 16, 2 * b + 0.3],
                         "down": [0, 2 * b + 1.7, 16, 2 * b + 2],
                         "east": [0, 2 * b, 0.8, 2 * b + 2], "west": [15.2, 2 * b, 16, 2 * b + 2]},
                     faces=("north", "south", "up", "down", "east", "west")))
    for (cy, ang, b) in ((9.2, 22.5, 5), (22.6, -22.5, 6)):
        els.append(B([-1.0, cy - 1.6, 14.4], [17.0, cy + 1.6, 15.2], "boards",
                     uv={"north": [0, 2 * b, 16, 2 * b + 2], "up": [0, 2 * b, 16, 2 * b + 0.3],
                         "down": [0, 2 * b + 1.7, 16, 2 * b + 2],
                         "east": [0, 2 * b, 0.8, 2 * b + 2], "west": [15.2, 2 * b, 16, 2 * b + 2]},
                     faces=("north", "south", "up", "down", "east", "west"),
                     rot=("z", ang, [8, cy, 14.8])))
    return model(tx("boards"), els, gui(0.42, -3.0))


add("boardedwoodplanks", "nsewud", boardedwoodplanks())


# --- wooden barrel: 0.85 m of staves, 0.6 m across the belly, iron hoops ---------------------
# Measured along the barrel from one head: the bulge in five octagon sections, the hoops, and
# where each head sits in from the stave ends (the chime).
BARREL_LEN = 13.6
BARREL_SECTIONS = [(0.0, 1.4, 4.1), (1.4, 3.8, 4.45), (3.8, 9.8, 4.8), (9.8, 12.2, 4.45),
                   (12.2, 13.6, 4.1)]
BARREL_STEPS = [(1.4, 4.1, 4.45, "lo"), (3.8, 4.45, 4.8, "lo"), (9.8, 4.45, 4.8, "hi"),
                (12.2, 4.1, 4.45, "hi")]
BARREL_HOOPS = [(0.45, 1.05, 4.1), (2.1, 2.8, 4.45), (10.8, 11.5, 4.45), (12.55, 13.15, 4.1)]
BARREL_HEAD_IN = 0.55


def woodenbarrel(upright):
    """The barrel lying along z on the floor, a head to the north (facing north, east, south,
    west), or stood on the floor on a head, centred in the block (facing up and down: its own
    model rather than a turn of the lying one, which would pivot it off centre)."""
    els = []
    if upright:
        axis, c1, c2, off = "y", 8.0, 8.0, 0.0
        lo_face, hi_face = "down", "up"
        vertical_faces = ("north", "south", "east", "west")
    else:
        axis, c1, c2, off = "z", 8.0, 4.8, (16 - BARREL_LEN) / 2.0
        lo_face, hi_face = "north", "south"
        vertical_faces = ("up", "down")

    def staves(elements):
        # the stave joints run along the barrel, whichever way a face's u and v lie
        for e in elements:
            for f in vertical_faces:
                if f in e["faces"]:
                    e["faces"][f]["texture"] = "#barrel_stave_v"
        return elements

    for (t0, t1, r) in BARREL_SECTIONS:
        els += staves(refit(lsc._octagon(axis, c1, c2, r, off + t0, off + t1, "barrel_stave",
                                         False)))
    # the steps where the belly widens: rings facing the nearer head
    for (t, r0, r1, end) in BARREL_STEPS:
        f = lo_face if end == "lo" else hi_face
        for e in refit(lsc._octagon(axis, c1, c2, r1, off + t - 0.01, off + t + 0.01,
                                    "barrel_stave", True, cap_front=(end == "lo"),
                                    cap_back=(end == "hi"))):
            e["faces"] = {k: v for k, v in e["faces"].items() if k == f}
            els.append(e)
    for (t0, t1, r) in BARREL_HOOPS:
        els += refit(lsc._octagon(axis, c1, c2, r + 0.12, off + t0, off + t1, "hoop", False))
    # the heads, set in from the ends of the staves
    rh = BARREL_SECTIONS[0][2]
    for t, f in ((off + BARREL_HEAD_IN, lo_face), (off + BARREL_LEN - BARREL_HEAD_IN, hi_face)):
        if upright:
            frm, to = [c1 - rh, t, c2 - rh], [c1 + rh, t, c2 + rh]
        else:
            frm, to = [c1 - rh, c2 - rh, t], [c1 + rh, c2 + rh, t]
        els.append(B(frm, to, "barrel_head", faces=(f,), uv=[0, 0, 16, 16]))
    return model(tx("barrel_stave", "barrel_stave_v", "barrel_head", "hoop"), els)


EXTRA_MODELS = {"woodenbarrel_upright": woodenbarrel(True)}
# facing up and down stand the barrel on its own upright model, unturned
STATE_OVERRIDES = {"woodenbarrel": {"up": {"model": MODEL + "woodenbarrel_upright"},
                                    "down": {"model": MODEL + "woodenbarrel_upright"}}}
add("woodenbarrel", "nsewud", woodenbarrel(False))


# ------------------------------------------------------------------------------------------
# Blockstates and output
# ------------------------------------------------------------------------------------------
def blockstate(registry, kind):
    facing = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
    variants = {}
    if kind == "nsewud":
        facing = {"down": {"x": 90}, "east": {"y": 90}, "north": {}, "south": {"y": 180},
                  "up": {"x": 270}, "west": {"y": 270}}
    facing.update(STATE_OVERRIDES.get(registry, {}))
    variants["facing"] = facing
    variants["inventory"] = [{}]
    if kind == "nsewud":
        variants["normal"] = [{}]
    return {"forge_marker": 1, "defaults": {"model": MODEL + registry}, "variants": variants}


def generate(assets):
    written = []
    for name, draw in sorted(TEXTURES.items()):
        rel = "textures/blocks/%s/%s.png" % (SUB, name)
        path = os.path.join(assets, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)
        written.append(rel)
    for registry, kind, mdl in BLOCKS:
        rel = "models/block/%s/%s.json" % (SUB, registry)
        lsc.dump(os.path.join(assets, rel), mdl)
        written.append(rel)
        rel = "blockstates/%s.json" % registry
        lsc.dump(os.path.join(assets, rel), blockstate(registry, kind))
        written.append(rel)
    for name, mdl in sorted(EXTRA_MODELS.items()):
        rel = "models/block/%s/%s.json" % (SUB, name)
        lsc.dump(os.path.join(assets, rel), mdl)
        written.append(rel)
    furniture_depth.separate(assets, written, lsc.dump)
    return written


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    if not args.check:
        print("wrote %d files under %s" % (len(generate(ASSETS)), ASSETS))
        return 0
    tmp = tempfile.mkdtemp(prefix="gameroom_")
    try:
        stale = [rel for rel in generate(tmp)
                 if not gen_trees.same_file(os.path.join(tmp, rel), os.path.join(ASSETS, rel))]
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("gen_furnishings_gameroom: up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
