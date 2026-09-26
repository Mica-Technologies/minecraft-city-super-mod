#!/usr/bin/env python3
"""Every asset the Utilities tab's building service meters ship: electric meters, a meter bank,
the meter socket, the service disconnect, the main breaker panel, the service entrance
switchboard, gas meters single and in a bank, the indoor water meter setter, and utility room
labels.

    python dev-env-utils/scripts/gen_utilities_meters.py
    python dev-env-utils/scripts/gen_utilities_meters.py --check
    python dev-env-utils/scripts/gen_utilities_meters.py --fragments   # tab lines to paste

What is here, and the class that places each (package powergrid.services unless named):

- **Electric meters** (BlockUtilityFixture): a ringless meter socket with a round meter in it,
  digital (its display cycles through its readings) or analog (its disk turns), and the socket
  with a blanking plate and no meter. Both faces are animated textures: nothing ticks.
- **The electric meter bank** (BlockUtilityRun): one meter centre position a block, with the
  tenant breaker under its meter and the wireway along the top. Positions side by side join
  into one bank; the enclosure's end flanges are drawn only at the bank's ends.
- **The service disconnect** (BlockUtilityFixture) and **the main breaker panel**
  (BlockUtilityPanel, whose door a click opens on the breakers).
- **The service entrance switchboard**: Roads' BlockUtilityBox, one block wide and two tall,
  with a utility meter in its top section.
- **Gas meters** (BlockUtilityFixture; the bank a BlockUtilityRun): a diaphragm meter on its
  swivels with the outlet into the wall; alone it hangs on its own riser with a shut-off and the
  service regulator, in a bank every meter drops from one header, capped at one end and fed at
  the other by the riser and regulator.
- **The water meter setter** (BlockUtilityFixture): the service up through the floor, a ball
  valve either side of the meter, the dual check backflow preventer, and the pipe up into the
  building.
- **Utility room labels** (BlockUtilityFixture): four plates in the utility colours.

Every model faces north with the wall at z = 16. Rounds are regular octagons (four rectangles,
two turned 45 degrees). A face with a picture on a round part -- a meter's dial, the water
meter's register -- is one square plane whose texture has the octagon cut out of its corners,
since the ends of the turned rectangles would show a turned picture. Every text write keeps the
line endings the file already has on disk, and a file whose content has not changed is not
touched, so a run over an untouched checkout leaves ``git status`` clean.
"""
import json
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_trees  # noqa: E402
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "powergrid", "src", "main", "resources", "assets", "csm")
TAB = "CsmTabUtilities"

FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

# ------------------------------------------------------------------------------------------
# Colours
# ------------------------------------------------------------------------------------------
GREY_61 = (168, 172, 168)      # ANSI 61 light grey, the enclosures
HUB = (86, 90, 94)
STEEL = (178, 182, 186)
COVER = (206, 212, 216)
FACE_WHITE = (236, 236, 228)
RIM = (196, 204, 208)
LCD_BG = (150, 168, 140)
LCD_INK = (28, 34, 30)
GAS_YELLOW = (214, 176, 40)
REGULATOR = (70, 78, 92)
COPPER = (186, 112, 72)
BRASS = (172, 142, 72)
BLUE_LEVER = (40, 92, 190)
YELLOW_LEVER = (232, 190, 30)
RED = (186, 32, 30)
BLACK = (22, 22, 24)
WHITE = (242, 242, 238)
UTILITY_RED = (190, 34, 32)
UTILITY_YELLOW = (238, 198, 32)
UTILITY_BLUE = (30, 82, 170)


class Catalogue(lc.Catalogue):
    """lc.Catalogue, writing into the tree only what changed and in the line endings the file
    already has, so a run over an untouched checkout changes nothing."""

    def generate(self, assets):
        if os.path.normpath(assets) != os.path.normpath(self.assets):
            return lc.Catalogue.generate(self, assets)
        tmp = tempfile.mkdtemp(prefix="utilities_")
        try:
            shutil.copytree(os.path.join(assets, "lang"), os.path.join(tmp, "lang"))
            written = lc.Catalogue.generate(self, tmp)
            for rel in written:
                sync(os.path.join(tmp, rel), os.path.join(assets, rel))
            return written
        finally:
            shutil.rmtree(tmp, ignore_errors=True)


def sync(src, dst):
    """Copies src over dst unless they already say the same thing: pixels for a PNG, text with
    line endings ignored otherwise. Text keeps dst's line endings; a new file is written LF."""
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    if src.endswith(".png"):
        if os.path.exists(dst) and gen_trees.same_file(src, dst):
            return
        shutil.copyfile(src, dst)
        return
    with open(src, "rb") as fh:
        new = fh.read().decode("utf-8").replace("\r\n", "\n")
    old = None
    if os.path.exists(dst):
        with open(dst, "rb") as fh:
            old = fh.read()
        if old.decode("utf-8").replace("\r\n", "\n") == new:
            return
    eol = "\r\n" if old is not None and b"\r\n" in old else "\n"
    with open(dst, "wb") as fh:
        fh.write(new.replace("\n", eol).encode("utf-8"))


C = Catalogue("gen_utilities_meters.py", "utilities/meters", "utilities/meters", assets=ASSETS)


def names_of(*n):
    assert len(n) == 4
    return n


# ------------------------------------------------------------------------------------------
# Elements
# ------------------------------------------------------------------------------------------
def _pos_uv(fn, frm, to, shift):
    x0, y0, z0 = (frm[i] - shift[i] for i in range(3))
    x1, y1, z1 = (to[i] - shift[i] for i in range(3))
    return {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1],
        "down": [x0, 16 - z1, x1, 16 - z0],
    }[fn]


def box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None,
        shift=(0, 0, 0), per=None):
    """An element. A face's uv is its own place in the block (moved by ``shift``, whole blocks,
    for a part past its cell), unless ``uv`` gives it; ``per`` gives a face another texture."""
    out = {}
    for fn in faces:
        u = (uv or {}).get(fn)
        if u is None:
            u = [min(16, max(0, v)) for v in _pos_uv(fn, frm, to, shift)]
            if u[0] == u[2]:
                u[2] = u[0] + 0.01
            if u[1] == u[3]:
                u[3] = u[1] + 0.01
        out[fn] = {"texture": "#" + (per or {}).get(fn, tex), "uv": [round(v, 4) for v in u]}
    return {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": out}


TAN = math.tan(math.pi / 8)


def octagon(axis, c1, c2, r, lo, hi, tex, cap_lo=False, cap_hi=False, shift=(0, 0, 0)):
    """A regular octagonal prism of inradius r along axis, centred on (c1, c2) across it, from lo
    to hi along it: two rectangles square to the axes and two turned 45 degrees, each drawing
    only its two long sides and, where asked, its ends (which sit a hair apart so the caps do
    not fight). As life_safety_gen_common's, with uv moved by ``shift``."""
    a = r * TAN
    els = []
    for k, (h1, h2, turned) in enumerate([(r, a, False), (a, r, False), (r, a, True),
                                           (a, r, True)]):
        eps = 0.004 * k
        if axis == "y":
            frm, to = [c1 - h1, lo - eps, c2 - h2], [c1 + h1, hi + eps, c2 + h2]
            sides = ["east", "west"] if h1 == r else ["north", "south"]
            ends = ("down", "up")
            origin = [c1, lo, c2]
        elif axis == "z":
            frm, to = [c1 - h1, c2 - h2, lo - eps], [c1 + h1, c2 + h2, hi + eps]
            sides = ["east", "west"] if h1 == r else ["up", "down"]
            ends = ("north", "south")
            origin = [c1, c2, lo]
        else:
            frm, to = [lo - eps, c1 - h1, c2 - h2], [hi + eps, c1 + h1, c2 + h2]
            sides = ["up", "down"] if h1 == r else ["north", "south"]
            ends = ("west", "east")
            origin = [lo, c1, c2]
        faces = list(sides)
        if cap_lo:
            faces.append(ends[0])
        if cap_hi:
            faces.append(ends[1])
        b = box(frm, to, tex, faces=faces, shift=shift)
        if turned:
            b["rotation"] = {"origin": [round(v, 4) for v in origin], "axis": axis, "angle": 45}
        els.append(b)
    return els


def post(cx, cz, r, y0, y1, tex, top=True, bottom=True, shift=(0, 0, 0)):
    return octagon("y", cx, cz, r, y0, y1, tex, bottom, top, shift)


def pipe_z(cx, cy, r, z0, z1, tex, front=True, back=False, shift=(0, 0, 0)):
    return octagon("z", cx, cy, r, z0, z1, tex, front, back, shift)


def pipe_x(cy, cz, r, x0, x1, tex, west=True, east=True):
    return octagon("x", cy, cz, r, x0, x1, tex, west, east)


def plane_north(x0, y0, x1, y1, z, tex, uv):
    """A picture on a zero-thickness plane facing north."""
    return {"from": [round(x0, 4), round(y0, 4), round(z, 4)],
            "to": [round(x1, 4), round(y1, 4), round(z, 4)],
            "faces": {"north": {"texture": "#" + tex, "uv": [round(v, 4) for v in uv]}}}


def plane_up(x0, z0, x1, z1, y, tex, uv):
    return {"from": [round(x0, 4), round(y, 4), round(z0, 4)],
            "to": [round(x1, 4), round(y, 4), round(z1, 4)],
            "faces": {"up": {"texture": "#" + tex, "uv": [round(v, 4) for v in uv]}}}


def extent(els, top=16):
    """The box round every element, in sixteenths, clipped to the cell (to ``top`` upward): the
    octagons' ends stand a hair past their length, which no box should follow."""
    lo = [min(e["from"][i] for e in els) for i in range(3)]
    hi = [max(e["to"][i] for e in els) for i in range(3)]
    return ([round(min(max(0, v), 16), 2) for v in lo]
            + [round(max(min(v, lim), 0), 2) for v, lim in zip(hi, (16, top, 16))])


def model(tex, els, display=None):
    m = {"parent": "block/block", "textures": dict(tex)}
    if els:
        m["elements"] = els
    if display:
        m["display"] = display
    return m


def child(parent, tex):
    return {"parent": "csm:block/" + C.M(parent).split(":", 1)[1], "textures": dict(tex)}


def multipart(parts):
    """Multipart rules for parts [(model name, conditions)], each turned to the four facings."""
    rules = []
    for name, cond in parts:
        for f, y in FACINGS:
            when = {"facing": f}
            for k, v in cond.items():
                when[k] = ("true" if v else "false") if isinstance(v, bool) else str(v)
            apply = {"model": C.M(name)}
            if y:
                apply["y"] = y
            rules.append({"when": when, "apply": apply})
    return {"multipart": rules}


PARTS = {}   # every part model written, by name, so a later block can reuse one


def part(name, data):
    PARTS[name] = data
    return data


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def flat(colour, seed, grain=4, size=16):
    return lc.fill(colour, size, grain, seed)


for _name, _colour, _seed in (("enclosure", GREY_61, 11), ("hub", HUB, 12), ("steel", STEEL, 13),
                              ("pipe_gas", GAS_YELLOW, 14), ("regulator", REGULATOR, 15),
                              ("copper", COPPER, 16), ("brass", BRASS, 17),
                              ("lever_blue", BLUE_LEVER, 18), ("lever_yellow", YELLOW_LEVER, 19),
                              ("lever_red", RED, 20), ("gas_body", (150, 156, 160), 21)):
    C.textures[_name] = (lambda c=_colour, s=_seed: flat(c, s))


@C.texture("cover")
def _cover():
    img = flat(COVER, 22, grain=2)
    for y in range(16):
        for x in (5, 6):
            img.putpixel((x, y), lc.shade(COVER, 1.08) + (255,))
    return img


def octagon_mask(size):
    """Which texels of a size-square texture lie inside the regular octagon inscribed in it,
    matching octagon(): flats at the square's edges and on the diagonals at the same distance."""
    R = size / 2.0
    inside = set()
    for y in range(size):
        for x in range(size):
            dx, dy = abs(x + 0.5 - R), abs(y + 0.5 - R)
            if dx <= R and dy <= R and dx + dy <= R * math.sqrt(2):
                inside.add((x, y))
    return inside


def round_face(size, frames, draw):
    """An animated round face: frames stacked in a strip, each masked to the octagon."""
    mask = octagon_mask(size)
    strip = Image.new("RGBA", (size, size * frames), (0, 0, 0, 0))
    for f in range(frames):
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw(img, f)
        px = img.load()
        for y in range(size):
            for x in range(size):
                if (x, y) not in mask:
                    px[x, y] = (0, 0, 0, 0)
                elif px[x, y][3] == 0:
                    px[x, y] = FACE_WHITE + (255,)
        strip.paste(img, (0, size * f))
    return strip


def rim(img, band, colour):
    """The cover's rim: a band of the octagon's edge in colour, and a darker line inside it."""
    size = img.width
    R = size / 2.0
    px = img.load()
    for y in range(size):
        for x in range(size):
            dx, dy = abs(x + 0.5 - R), abs(y + 0.5 - R)
            d = max(dx, dy, (dx + dy) / math.sqrt(2))
            if d > R - band:
                px[x, y] = colour + (255,)
            elif d > R - band - 1:
                px[x, y] = lc.shade(colour, 0.78) + (255,)


def mcmeta(frametime):
    return json.dumps({"animation": {"frametime": frametime}}, indent=2) + "\n"


def animated(name, frametime):
    C.extra["textures/blocks/%s/%s.png.mcmeta" % (C.tex_dir, name)] = mcmeta(frametime)


READINGS = ["04817", "04817", "04818", "88888"]


@C.texture("face_digital")
def _face_digital():
    def draw(img, f):
        lc.rect(img, 0, 0, 32, 32, FACE_WHITE)
        rim(img, 2, RIM)
        # the display window and its reading, the last frame the segment test
        lc.rect(img, 6, 10, 26, 18, lc.shade(LCD_BG, 0.7))
        lc.rect(img, 7, 11, 25, 17, LCD_BG)
        lc.draw_text_centred(img, READINGS[f], 16, 12, LCD_INK)
        lc.draw_text_centred(img, "KWH", 16, 19, (70, 72, 76))
        # the optical port and its pulse light, which blinks with the frames
        lc.disc(img, 22.5, 7.5, 1.6, (40, 40, 44))
        img.putpixel((9, 7), ((220, 40, 30, 255) if f % 2 == 0 else (90, 30, 28, 255)))
        # a bar code
        for i, x in enumerate(range(10, 23)):
            if (i * 7 + 3) % 5 < 3:
                lc.rect(img, x, 26, x + 1, 28, (40, 40, 44))
    return round_face(32, 4, draw)


animated("face_digital", 60)


@C.texture("face_analog")
def _face_analog():
    def draw(img, f):
        lc.rect(img, 0, 0, 32, 32, FACE_WHITE)
        rim(img, 2, RIM)
        # the four register dials, their hands at the reading 4-8-1-7, turning alternately
        for i, (cx, digit) in enumerate(zip((8.5, 13.5, 18.5, 23.5), (4, 8, 1, 7))):
            lc.disc(img, cx, 10.5, 2.4, (60, 62, 66))
            lc.disc(img, cx, 10.5, 1.8, FACE_WHITE)
            ang = (digit / 10.0) * 2 * math.pi * (1 if i % 2 == 0 else -1)
            for k in (0.6, 1.3):
                img.putpixel((int(cx + math.sin(ang) * k), int(10.5 - math.cos(ang) * k)),
                             (20, 20, 22, 255))
        lc.draw_text_centred(img, "KWH", 16, 15, (70, 72, 76))
        # the disk seen edge on through its slot; its black mark crosses and comes round again
        lc.rect(img, 6, 21, 26, 25, (36, 36, 40))
        lc.rect(img, 7, 22, 25, 24, (196, 198, 200))
        mark = 7 + (f * 18) // 8
        lc.rect(img, mark, 22, min(25, mark + 3), 24, (20, 20, 22))
    return round_face(32, 8, draw)


animated("face_analog", 4)


@C.texture("water_register")
def _water_register():
    def draw(img, f):
        lc.rect(img, 0, 0, 32, 32, (226, 228, 222))
        rim(img, 2, BRASS)
        lc.rect(img, 8, 7, 24, 14, (30, 30, 34))
        lc.draw_text(img, "0073", 9, 8, WHITE)
        lc.draw_text_centred(img, "GAL", 16, 16, (70, 72, 76))
        # the sweep hand
        ang = f / 4.0 * 2 * math.pi
        for k in range(1, 6):
            x, y = 16 + math.sin(ang) * k, 25 - math.cos(ang) * k * 0.7
            img.putpixel((int(x), int(y)), (200, 30, 28, 255))
        lc.disc(img, 16, 25, 1.2, (30, 30, 34))
    return round_face(32, 4, draw)


animated("water_register", 10)


@C.texture("gas_face")
def _gas_face():
    """The gas meter's front cover (28 x 28 of the 32 texture, 7 x 7 px of model): the index
    window with its cyclometer and the turning test dial, and the nameplate."""
    frames = 4
    strip = Image.new("RGBA", (32, 32 * frames), (0, 0, 0, 0))
    for f in range(frames):
        img = lc.fill((150, 156, 160), 32, 3, 31)
        lc.bevel(img, 0, 0, 28, 28, (150, 156, 160))
        # the index plate: the cyclometer's wheels behind their window, and the test dial
        lc.rect(img, 3, 3, 25, 17, (40, 42, 46))
        lc.rect(img, 4, 4, 24, 16, (228, 228, 220))
        lc.rect(img, 5, 5, 23, 11, (30, 30, 34))
        lc.draw_text(img, "0482", 6, 6, WHITE)
        lc.disc(img, 14, 13.5, 2.0, (60, 62, 66))
        lc.disc(img, 14, 13.5, 1.4, (228, 228, 220))
        ang = f / 4.0 * 2 * math.pi
        img.putpixel((int(14 + math.sin(ang) * 1.1), int(13.5 - math.cos(ang) * 1.1)),
                     (200, 30, 28, 255))
        # the nameplate
        lc.rect(img, 6, 19, 22, 25, (196, 190, 170))
        lc.frame(img, 6, 19, 22, 25, (110, 106, 96))
        lc.rect(img, 8, 21, 20, 22, (120, 116, 106))
        lc.rect(img, 8, 23, 16, 24, (120, 116, 106))
        strip.paste(img, (0, 32 * f))
    return strip


animated("gas_face", 10)


@C.texture("disconnect")
def _disconnect():
    """The service disconnect's front (21 x 30 of the 32 texture, 7 x 10 px of model)."""
    img = lc.fill(GREY_61, 32, 3, 41)
    lc.bevel(img, 0, 0, 21, 30, GREY_61)
    lc.frame(img, 1, 1, 20, 29, lc.shade(GREY_61, 0.8))
    # the warning label
    lc.rect(img, 4, 3, 17, 12, (236, 196, 30))
    lc.frame(img, 4, 3, 17, 12, BLACK)
    for row in range(7):
        half = row * 0.62
        lc.rect(img, int(round(10.5 - half - 0.5)), 4 + row + 1,
                int(round(10.5 + half + 0.5)), 5 + row + 1, BLACK)
    for row in range(1, 6):
        half = max(0.0, row * 0.62 - 1.1)
        if half > 0.2:
            lc.rect(img, int(round(10.5 - half)), 5 + row + 1, int(round(10.5 + half)),
                    6 + row + 1, (236, 196, 30))
    lc.rect(img, 10, 7, 11, 10, BLACK)
    lc.rect(img, 10, 11, 11, 12, BLACK)
    # the ON / OFF window and the padlock hasp
    lc.rect(img, 6, 14, 15, 19, (40, 42, 46))
    lc.draw_text_centred(img, "OFF", 10.5, 14, WHITE)
    lc.rect(img, 8, 23, 13, 26, lc.shade(GREY_61, 0.7))
    return img


def panel_door(img, x0, y0):
    """The main panel's door, 22 x 29 texels at (x0, y0)."""
    lc.bevel(img, x0, y0, x0 + 22, y0 + 29, GREY_61)
    lc.frame(img, x0 + 1, y0 + 1, x0 + 21, y0 + 28, lc.shade(GREY_61, 0.86))
    # the directory card in its frame
    lc.rect(img, x0 + 6, y0 + 5, x0 + 17, y0 + 20, (236, 234, 224))
    lc.frame(img, x0 + 6, y0 + 5, x0 + 17, y0 + 20, (120, 122, 126))
    for yy in range(y0 + 7, y0 + 19, 2):
        lc.rect(img, x0 + 8, yy, x0 + 15, yy + 1, (150, 150, 160))
    # the latch, on the side away from the hinge
    lc.rect(img, x0 + 19, y0 + 12, x0 + 21, y0 + 17, (60, 62, 66))


def panel_inside(img, x0, y0):
    """The dead front with its breakers, 22 x 29 texels at (x0, y0)."""
    lc.rect(img, x0, y0, x0 + 22, y0 + 29, lc.shade(GREY_61, 0.9))
    lc.frame(img, x0, y0, x0 + 22, y0 + 29, lc.shade(GREY_61, 0.7))
    # the main breaker
    lc.rect(img, x0 + 7, y0 + 2, x0 + 15, y0 + 7, (36, 38, 42))
    lc.rect(img, x0 + 9, y0 + 3, x0 + 13, y0 + 6, (60, 62, 66))
    lc.draw_text_centred(img, "200", x0 + 11, y0 + 8, (40, 40, 44))
    # two columns of branch breakers
    for row in range(8):
        y = y0 + 14 + row * 2
        for x in (x0 + 4, x0 + 12):
            lc.rect(img, x, y, x + 6, y + 1, (30, 32, 36))
            lc.rect(img, x + 2, y, x + 4, y + 1, (70, 72, 78))
    lc.rect(img, x0 + 10, y0 + 13, x0 + 12, y0 + 28, (110, 112, 116))


def switchboard_front(img):
    """The switchboard's front: 32 x 64 texels on the left half of the fronts sheet."""
    lc.rect(img, 0, 0, 32, 64, GREY_61)
    for y in range(64):
        for x in range(32):
            if (x * 7 + y * 13) % 29 == 0:
                img.putpixel((x, y), lc.shade(GREY_61, 0.96) + (255,))
    seam = lc.shade(GREY_61, 0.66)
    # the metering section: a hinged door with a sealed window
    lc.frame(img, 1, 1, 31, 22, seam)
    lc.rect(img, 12, 2, 20, 4, (236, 196, 30))
    # the main breaker section
    lc.frame(img, 1, 23, 31, 38, seam)
    lc.rect(img, 10, 26, 22, 35, (40, 42, 46))
    lc.rect(img, 13, 27, 19, 31, (70, 72, 78))
    lc.rect(img, 3, 25, 8, 28, (236, 196, 30))
    # the distribution section
    lc.frame(img, 1, 39, 31, 61, seam)
    for row in range(5):
        y = 41 + row * 4
        for x in (4, 18):
            lc.rect(img, x, y, x + 10, y + 2, (36, 38, 42))
            lc.rect(img, x + 4, y, x + 6, y + 2, (80, 82, 86))
    for (x, y) in ((2, 2), (29, 2), (2, 20), (29, 20), (2, 24), (29, 24), (2, 36), (29, 36),
                   (2, 40), (29, 40), (2, 59), (29, 59)):
        img.putpixel((x, y), lc.shade(GREY_61, 0.6) + (255,))
    lc.rect(img, 0, 62, 32, 64, lc.shade(GREY_61, 0.5))


DOOR_AT = (37, 1)       # the panel door on the fronts sheet
INSIDE_AT = (37, 33)    # its dead front and breakers


@C.texture("fronts")
def _fronts():
    img = lc.fill(GREY_61, 64, 3, 51)
    switchboard_front(img)
    panel_door(img, *DOOR_AT)
    panel_inside(img, *INSIDE_AT)
    return img


LABELS = [
    ("utility_label_electric", ("ELECTRIC", "ROOM"), UTILITY_RED, WHITE,
     names_of("Utility Label (Electric Room)", "Versorgungsschild (Elektroraum)",
              "Rótulo de Servicios (Cuarto Eléctrico)", "Försörjningsskylt (Elrum)")),
    ("utility_label_gas", ("GAS", "METERS"), UTILITY_YELLOW, BLACK,
     names_of("Utility Label (Gas Meters)", "Versorgungsschild (Gaszähler)",
              "Rótulo de Servicios (Medidores de Gas)", "Försörjningsskylt (Gasmätare)")),
    ("utility_label_water", ("WATER", "METER"), UTILITY_BLUE, WHITE,
     names_of("Utility Label (Water Meter)", "Versorgungsschild (Wasserzähler)",
              "Rótulo de Servicios (Medidor de Agua)", "Försörjningsskylt (Vattenmätare)")),
    ("utility_label_disconnect", ("MAIN", "DISCONNECT"), WHITE, UTILITY_RED,
     names_of("Utility Label (Main Disconnect)", "Versorgungsschild (Hauptschalter)",
              "Rótulo de Servicios (Desconexión Principal)",
              "Försörjningsskylt (Huvudbrytare)")),
]


@C.texture("labels")
def _labels():
    """Four label plates, one 64 x 16 band each, two lines of lettering in the utility colour
    code's colours."""
    img = Image.new("RGBA", (64, 64))
    for i, (_reg, lines, bg, ink, _n) in enumerate(LABELS):
        y0 = 16 * i
        lc.rect(img, 0, y0, 64, y0 + 16, bg)
        lc.frame(img, 0, y0, 64, y0 + 16, lc.shade(bg, 0.7) if bg != WHITE else UTILITY_RED)
        lc.draw_text_centred(img, lines[0], 32, y0 + 2, ink)
        lc.draw_text_centred(img, lines[1], 32, y0 + 9, ink)
        for x, y in ((2, y0 + 2), (61, y0 + 2), (2, y0 + 13), (61, y0 + 13)):
            img.putpixel((x, y), (150, 150, 150, 255))
    return img


# ------------------------------------------------------------------------------------------
# Electric: meters, socket, bank
# ------------------------------------------------------------------------------------------
METER_CX, METER_CY, SOCKET_BACK = 8.0, 9.0, 13.5
METER_R, RING_R = 2.6, 3.0


def meter_elements(cx, cy, back, shift=(0, 0, 0)):
    """A round meter plugged into a socket whose front is at z = back: the socket's ring, the
    meter's cover, and its face on a plane cut to the octagon."""
    els = pipe_z(cx, cy, RING_R, back - 0.6, back, "steel", front=True, shift=shift)
    els += pipe_z(cx, cy, METER_R, back - 3.2, back - 0.6, "cover", front=False, shift=shift)
    els.append(plane_north(cx - METER_R, cy - METER_R, cx + METER_R, cy + METER_R, back - 3.2,
                           "face", [0, 0, 16, 16]))
    return els


METER_TEX = {"steel": C.T("steel"), "cover": C.T("cover"), "face": C.T("face_digital"),
             "particle": C.T("cover")}
ENC_TEX = {"enclosure": C.T("enclosure"), "hub": C.T("hub"), "steel": C.T("steel"),
           "particle": C.T("enclosure")}


def socket_elements():
    cx, cy = METER_CX, METER_CY
    els = [box([cx - 3.75, cy - 5.5, SOCKET_BACK], [cx + 3.75, cy + 4.0, 16], "enclosure",
               faces=("north", "east", "west", "up", "down"))]
    els.append(box([cx - 1.2, cy - 6.2, SOCKET_BACK + 0.4], [cx + 1.2, cy - 5.5, 16], "hub",
                   faces=("north", "east", "west", "down")))
    els += post(cx, 14.75, 0.9, 0, cy - 6.2, "steel", top=False, bottom=False)
    return els


def electric():
    meter = part("meter_digital", model(METER_TEX, meter_elements(METER_CX, METER_CY,
                                                                  SOCKET_BACK)))
    analog = part("meter_analog", child("meter_digital", {"face": C.T("face_analog")}))
    socket = part("meter_socket", model(ENC_TEX, socket_elements()))
    blank = part("meter_blank", model(ENC_TEX, pipe_z(METER_CX, METER_CY, RING_R,
                                                      SOCKET_BACK - 0.6, SOCKET_BACK, "steel")
                                      + pipe_z(METER_CX, METER_CY, 2.2, SOCKET_BACK - 1.0,
                                               SOCKET_BACK - 0.6, "enclosure")))
    for reg, face_part, names in (
            ("electric_meter_digital", "meter_digital",
             names_of("Electric Meter (Digital)", "Stromzähler (Digital)",
                      "Medidor Eléctrico (Digital)", "Elmätare (Digital)")),
            ("electric_meter_analog", "meter_analog",
             names_of("Electric Meter (Analog)", "Stromzähler (Analog)",
                      "Medidor Eléctrico (Analógico)", "Elmätare (Analog)")),
            ("electric_meter_socket", "meter_blank",
             names_of("Electric Meter Socket", "Zählersteckdose",
                      "Base de Medidor Eléctrico", "Mätaruttag"))):
        models = {"meter_socket": socket, face_part: PARTS[face_part]}
        if face_part == "meter_analog":
            models["meter_digital"] = meter
        els = socket["elements"] + (meter["elements"] if face_part != "meter_blank"
                                    else blank["elements"])
        tex = dict(ENC_TEX, **METER_TEX)
        if face_part == "meter_analog":
            tex["face"] = C.T("face_analog")
        C.add(reg, 'new BlockUtilityFixture("%s", new double[]{%s})'
              % (reg, ", ".join(fmt(v) for v in extent(els))),
              names, models, multipart([("meter_socket", {}), (face_part, {})]),
              item=model(tex, els), tab=TAB)

    # the bank: one meter centre position a block
    body = [box([0, 1, SOCKET_BACK], [16, 15.5, 16], "enclosure", faces=("north", "up", "down")),
            box([0, 13.0, 12.8], [16, 15.5, SOCKET_BACK], "enclosure",
                faces=("north", "up", "down")),
            box([0, 1, SOCKET_BACK - 0.2], [0.35, 13.0, SOCKET_BACK], "hub", faces=("north",)),
            box([15.65, 1, SOCKET_BACK - 0.2], [16, 13.0, SOCKET_BACK], "hub",
                faces=("north",)),
            box([6.5, 2.0, SOCKET_BACK - 0.4], [9.5, 4.2, SOCKET_BACK], "hub",
                faces=("north", "east", "west", "up", "down")),
            box([7.6, 2.7, SOCKET_BACK - 0.9], [8.4, 3.5, SOCKET_BACK - 0.4], "steel",
                faces=("north", "east", "west", "up", "down"))]
    bank_body = part("meter_bank_body", model(ENC_TEX, body))
    end_faces = ("north", "east", "west", "up", "down")
    end_left = part("meter_bank_end_left", model(ENC_TEX, [
        box([0, 0.6, 12.5], [0.8, 15.9, 16], "enclosure", faces=end_faces)]))
    end_right = part("meter_bank_end_right", model(ENC_TEX, [
        box([15.2, 0.6, 12.5], [16, 15.9, 16], "enclosure", faces=end_faces)]))
    models = {"meter_bank_body": bank_body, "meter_digital": meter,
              "meter_bank_end_left": end_left, "meter_bank_end_right": end_right}
    els = body + meter["elements"] + end_left["elements"] + end_right["elements"]
    C.add("electric_meter_bank", 'new BlockUtilityRun("electric_meter_bank", new double[]{%s})'
          % ", ".join(fmt(v) for v in extent(els)),
          names_of("Electric Meter Bank", "Zählerschrank", "Banco de Medidores Eléctricos",
                   "Mätarskåp"),
          models, multipart([("meter_bank_body", {}), ("meter_digital", {}),
                             ("meter_bank_end_left", {"left": False}),
                             ("meter_bank_end_right", {"right": False})]),
          item=model(dict(ENC_TEX, **METER_TEX), els), tab=TAB)


def fmt(v):
    return ("%.3f" % v).rstrip("0").rstrip(".") if v != int(v) else str(int(v))


# ------------------------------------------------------------------------------------------
# Electric: disconnect, main panel, switchboard
# ------------------------------------------------------------------------------------------
def disconnect():
    tex = dict(ENC_TEX, front=C.T("disconnect"), handle=C.T("lever_red"))
    els = [box([4.5, 4, 11.5], [11.5, 14, 16], "enclosure",
               faces=("north", "east", "west", "up", "down"),
               uv={"north": [0, 0, 10.5, 15]}, per={"north": "front"}),
           box([3.7, 8, 12.5], [4.5, 11, 14.5], "hub", faces=("north", "west", "up", "down")),
           box([3.2, 9, 13.0], [3.7, 13, 14.0], "handle",
               faces=("north", "south", "west", "east", "up", "down")),
           box([6.9, 14, 12.8], [9.1, 14.6, 15.2], "hub", faces=("north", "east", "west", "up")),
           box([6.9, 3.4, 12.8], [9.1, 4, 15.2], "hub", faces=("north", "east", "west", "down"))]
    els += post(8, 14.0, 0.9, 14.6, 16, "steel", top=False, bottom=False)
    els += post(8, 14.0, 0.9, 0, 3.4, "steel", top=False, bottom=False)
    C.add("service_disconnect", 'new BlockUtilityFixture("service_disconnect", new double[]{%s})'
          % ", ".join(fmt(v) for v in extent(els)),
          names_of("Service Disconnect", "Hausanschluss-Trennschalter",
                   "Desconectador de Servicio", "Servisbrytare"),
          {"service_disconnect": model(tex, els)},
          lc.facing_state(C.M("service_disconnect")), tab=TAB)


def sheet_uv(x0, y0, w, h, size=64):
    k = 16.0 / size
    return [x0 * k, y0 * k, (x0 + w) * k, (y0 + h) * k]


def main_panel():
    tex = dict(ENC_TEX, front=C.T("fronts"))
    door_uv = sheet_uv(DOOR_AT[0], DOOR_AT[1], 22, 29)
    inside_uv = sheet_uv(INSIDE_AT[0], INSIDE_AT[1], 22, 29)
    tub = [box([2.5, 1, 14], [13.5, 15.5, 16], "enclosure",
               faces=("east", "west", "up", "down"))]
    closed = [box([2.7, 1.2, 13.6], [13.3, 15.3, 14], "enclosure",
                  faces=("north", "east", "west", "up", "down"),
                  uv={"north": door_uv}, per={"north": "front"}),
              box([3.3, 7.5, 13.2], [3.9, 9.5, 13.6], "hub",
                  faces=("north", "east", "west", "up", "down"))]
    opened = [box([2.7, 1.2, 13.9], [13.3, 15.3, 14], "enclosure", faces=("north",),
                  uv={"north": inside_uv}, per={"north": "front"}),
              box([2.5, 1, 13.9], [2.7, 15.5, 14], "enclosure", faces=("north",)),
              box([13.3, 1, 13.9], [13.5, 15.5, 14], "enclosure", faces=("north",)),
              box([13.3, 1.2, 3.0], [13.7, 15.3, 13.6], "enclosure",
                  faces=("north", "east", "west", "up", "down"),
                  uv={"east": door_uv}, per={"east": "front"})]
    models = {"main_panel_tub": part("main_panel_tub", model(tex, tub)),
              "main_panel_door_closed": part("main_panel_door_closed", model(tex, closed)),
              "main_panel_door_open": part("main_panel_door_open", model(tex, opened))}
    shut = extent(tub + closed)
    wide = extent(tub + opened)
    C.add("main_panel", 'new BlockUtilityPanel("main_panel", new double[]{%s}, new double[]{%s})'
          % (", ".join(fmt(v) for v in shut), ", ".join(fmt(v) for v in wide)),
          names_of("Main Breaker Panel", "Hauptverteiler", "Tablero Principal de Interruptores",
                   "Huvudcentral"),
          models, multipart([("main_panel_tub", {}), ("main_panel_door_closed", {"open": False}),
                             ("main_panel_door_open", {"open": True})]),
          item=model(tex, tub + closed), tab=TAB)


def switchboard():
    """One block wide, two tall, standing against the wall: Roads' utility box, the root
    drawing the whole of it. Everything above y = 16 takes its uv a block down."""
    tex = dict(ENC_TEX, front=C.T("fronts"), **{k: v for k, v in METER_TEX.items()
                                                if k != "particle"})
    up = (0, 16, 0)
    top_y = 31.5
    face_h = top_y - 0.8
    v_split = (top_y - 16) / face_h * 16
    els = [box([0.5, 0.8, 4], [15.5, 16, 16], "enclosure", faces=("north", "east", "west"),
               uv={"north": [0, v_split, 8, 16]}, per={"north": "front"}),
           box([0.5, 16, 4], [15.5, top_y, 16], "enclosure", faces=("north", "east", "west"),
               uv={"north": [0, 0, 8, v_split]}, per={"north": "front"}, shift=up),
           box([0.3, 0, 3.8], [15.7, 0.8, 16], "hub", faces=("north", "east", "west", "up")),
           box([0.3, top_y, 3.8], [15.7, 32, 16], "enclosure",
               faces=("north", "east", "west", "up", "down"), shift=up)]
    els += meter_elements(8, 26.5, 4, shift=up)
    display = {"gui": {"rotation": [30, 225, 0], "translation": [0, -3.2, 0],
                       "scale": [0.34, 0.34, 0.34]},
               "fixed": {"translation": [0, -4, 0], "scale": [0.36, 0.36, 0.36]},
               "ground": {"translation": [0, 2, 0], "scale": [0.18, 0.18, 0.18]},
               "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 1.5],
                                         "scale": [0.2, 0.2, 0.2]},
               "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0],
                                         "scale": [0.22, 0.22, 0.22]}}
    b = extent(els, top=32)
    spec = "new AxisAlignedBB(%s)" % ", ".join(fmt(round(v / 16.0, 4)) for v in b)
    C.add("electric_switchboard", 'new BlockUtilityBox("electric_switchboard", '
          'new UtilityBoxSpec(1, 1, 2, %s, null))' % spec,
          names_of("Service Entrance Switchboard", "Hausanschluss-Schaltanlage",
                   "Tablero de Acometida", "Servisställverk"),
          {"electric_switchboard": model(tex, els, display)},
          lc.facing_state(C.M("electric_switchboard")), tab=TAB)


# ------------------------------------------------------------------------------------------
# Gas
# ------------------------------------------------------------------------------------------
GAS_Z = 13.25     # the pipes' centre, out from the wall
GAS_TEX = {"body": C.T("gas_body"), "face": C.T("gas_face"), "hub": C.T("hub"),
           "steel": C.T("steel"), "pipe": C.T("pipe_gas"), "regulator": C.T("regulator"),
           "lever": C.T("lever_yellow"), "particle": C.T("gas_body")}


def valve_on_riser(x, y0, y1, lever_forward=True):
    els = [box([x - 0.6, y0, GAS_Z - 0.6], [x + 0.6, y1, GAS_Z + 0.6], "steel")]
    mid = (y0 + y1) / 2
    els.append(box([x - 0.3, mid - 0.25, GAS_Z - 2.0], [x + 0.3, mid + 0.25, GAS_Z - 0.6],
                   "lever", faces=("north", "east", "west", "up", "down")))
    return els


def regulator(x, y0):
    """The service regulator on a riser: its valve body and the diaphragm dome above it, with
    the vent turned down."""
    els = [box([x - 0.9, y0, GAS_Z - 0.9], [x + 0.9, y0 + 1.6, GAS_Z + 0.9], "regulator")]
    els += post(x, GAS_Z, 1.5, y0 + 1.6, y0 + 2.8, "regulator")
    els.append(box([x - 0.35, y0 + 0.4, GAS_Z - 2.3], [x + 0.35, y0 + 1.1, GAS_Z - 0.9],
                   "regulator", faces=("north", "east", "west", "up", "down")))
    return els


def gas():
    body = [box([4.5, 5.5, 11], [11.5, 12.5, 15.5], "body",
                faces=("north", "east", "west", "up", "down"),
                uv={"north": [0, 0, 14, 14]}, per={"north": "face"}),
            box([6, 7, 15.5], [10, 11, 16], "hub", faces=("north", "east", "west", "up", "down"))]
    for x in (6, 10):
        body += post(x, GAS_Z, 0.75, 12.5, 13.6, "steel", bottom=False)
    gas_body = part("gas_meter_body", model(GAS_TEX, body))
    outlet = post(6, GAS_Z, 0.55, 13.6, 14.1, "pipe", top=False, bottom=False)
    outlet.append(box([5.3, 13.4, GAS_Z - 0.7], [6.7, 14.8, GAS_Z + 0.7], "pipe"))
    outlet += pipe_z(6, 14.1, 0.55, GAS_Z + 0.7, 16, "pipe", front=False)
    gas_outlet = part("gas_meter_outlet", model(GAS_TEX, outlet))

    # alone: its own riser up the wall beside it, the shut-off, the regulator, over the top
    rx = 13.5
    riser = post(rx, GAS_Z, 0.55, 0, 14.5, "pipe", top=False, bottom=False)
    riser += valve_on_riser(rx, 2.6, 3.8)
    riser += regulator(rx, 7.6)
    riser.append(box([rx - 0.7, 14.5, GAS_Z - 0.7], [rx + 0.7, 15.8, GAS_Z + 0.7], "pipe"))
    riser += pipe_x(15.15, GAS_Z, 0.55, 10.7, rx - 0.7, "pipe", west=False, east=False)
    riser.append(box([9.3, 14.5, GAS_Z - 0.7], [10.7, 15.8, GAS_Z + 0.7], "pipe"))
    riser += post(10, GAS_Z, 0.55, 13.6, 14.5, "pipe", top=False, bottom=False)
    gas_riser = part("gas_meter_riser", model(GAS_TEX, riser))
    models = {"gas_meter_body": gas_body, "gas_meter_outlet": gas_outlet,
              "gas_meter_riser": gas_riser}
    els = body + outlet + riser
    C.add("gas_meter", 'new BlockUtilityFixture("gas_meter", new double[]{%s})'
          % ", ".join(fmt(v) for v in extent(els)),
          names_of("Gas Meter", "Gaszähler", "Medidor de Gas", "Gasmätare"),
          models, multipart([("gas_meter_body", {}), ("gas_meter_outlet", {}),
                             ("gas_meter_riser", {})]),
          item=model(GAS_TEX, els), tab=TAB)

    # in a bank: every meter drops from one header, which is capped at the bank's left end and
    # fed at its right end by the riser and regulator
    hy, hr, ex = 15.3, 0.6, 14.4
    drop = post(10, GAS_Z, 0.5, 13.6, hy - hr, "pipe", top=False, bottom=False)
    drop += valve_on_riser(10, 13.75, 14.55)
    drop += pipe_x(hy, GAS_Z, hr, 0, ex, "pipe", west=False, east=False)
    header_on = pipe_x(hy, GAS_Z, hr, ex, 16, "pipe", west=False, east=False)
    cap = pipe_x(hy, GAS_Z, hr + 0.15, 0.1, 0.9, "steel")
    feed = [box([ex - 0.75, hy - 0.75, GAS_Z - 0.75], [ex + 0.75, hy + 0.6, GAS_Z + 0.75],
                "pipe")]
    feed += post(ex, GAS_Z, 0.6, 0, hy - 0.75, "pipe", top=False, bottom=False)
    feed += valve_on_riser(ex, 2.6, 3.8)
    feed += regulator(ex, 7.6)
    models = {"gas_meter_body": gas_body, "gas_meter_outlet": gas_outlet,
              "gas_bank_drop": part("gas_bank_drop", model(GAS_TEX, drop)),
              "gas_bank_header_right": part("gas_bank_header_right", model(GAS_TEX, header_on)),
              "gas_bank_cap": part("gas_bank_cap", model(GAS_TEX, cap)),
              "gas_bank_feed": part("gas_bank_feed", model(GAS_TEX, feed))}
    els = body + outlet + drop + cap + feed
    C.add("gas_meter_bank", 'new BlockUtilityRun("gas_meter_bank", new double[]{%s})'
          % ", ".join(fmt(v) for v in extent(els + header_on)),
          names_of("Gas Meter Bank", "Gaszählerbatterie", "Banco de Medidores de Gas",
                   "Gasmätarbank"),
          models, multipart([("gas_meter_body", {}), ("gas_meter_outlet", {}),
                             ("gas_bank_drop", {}), ("gas_bank_header_right", {"right": True}),
                             ("gas_bank_cap", {"left": False}),
                             ("gas_bank_feed", {"right": False})]),
          item=model(GAS_TEX, els), tab=TAB)


# ------------------------------------------------------------------------------------------
# Water
# ------------------------------------------------------------------------------------------
def water():
    tex = {"copper": C.T("copper"), "brass": C.T("brass"), "lever": C.T("lever_blue"),
           "steel": C.T("steel"), "cover": C.T("cover"), "register": C.T("water_register"),
           "particle": C.T("brass")}
    py, pz = 6.5, 13.5
    els = post(13.4, pz, 0.65, 0, py - 0.7, "copper", top=False, bottom=False)
    els.append(box([12.7, py - 0.7, pz - 0.7], [14.1, py + 0.7, pz + 0.7], "copper"))
    els += pipe_x(py, pz, 0.55, 2.2, 12.7, "copper", west=False, east=False)
    for x0, x1 in ((10.9, 12.3), (2.3, 3.7)):
        els += pipe_x(py, pz, 0.95, x0, x1, "brass")
        mid = (x0 + x1) / 2
        els.append(box([mid - 0.15, py + 0.95, pz - 0.15], [mid + 0.15, py + 1.35, pz + 0.15],
                       "steel", faces=("north", "south", "east", "west")))
        els.append(box([mid - 0.4, py + 1.35, pz - 2.4], [mid + 0.4, py + 1.75, pz + 0.3],
                       "lever"))
    els.append(box([7.3, 5.3, 12.3], [10.5, 7.7, 14.7], "brass"))
    els += post(8.9, pz, 1.35, 7.7, 8.9, "cover", top=False, bottom=False)
    els.append(plane_up(8.9 - 1.35, pz - 1.35, 8.9 + 1.35, pz + 1.35, 8.9, "register",
                        [16, 16, 0, 0]))
    els += pipe_x(py, pz, 1.1, 4.1, 6.9, "brass")
    for x in (4.8, 6.2):
        els += post(x, pz, 0.3, py + 1.1, py + 1.8, "brass", bottom=False)
    els.append(box([0.8, py - 0.7, pz - 0.7], [2.2, py + 0.7, pz + 0.7], "copper"))
    els += post(1.5, pz, 0.65, py + 0.7, 16, "copper", top=False, bottom=False)
    for x, y in ((1.5, 11.5), (13.4, 2.5)):
        els.append(box([x - 0.9, y, pz - 0.9], [x + 0.9, y + 0.6, 16], "steel",
                       faces=("north", "east", "west", "up", "down")))
    C.add("water_meter_setter", 'new BlockUtilityFixture("water_meter_setter", new double[]{%s})'
          % ", ".join(fmt(v) for v in extent(els)),
          names_of("Water Meter Setter", "Wasserzähleranlage", "Montaje de Medidor de Agua",
                   "Vattenmätarinstallation"),
          {"water_meter_setter": model(tex, els)},
          lc.facing_state(C.M("water_meter_setter")), tab=TAB)


# ------------------------------------------------------------------------------------------
# Labels
# ------------------------------------------------------------------------------------------
def labels():
    for i, (reg, _lines, _bg, _ink, names) in enumerate(LABELS):
        v0 = 4.0 * i
        els = [box([1, 9, 15.5], [15, 12.5, 16], "label",
                   faces=("north", "east", "west", "up", "down"),
                   uv={"north": [0, v0, 16, v0 + 4], "up": [0, v0, 16, v0 + 0.25],
                       "down": [0, v0 + 3.75, 16, v0 + 4], "east": [0, v0, 0.25, v0 + 4],
                       "west": [15.75, v0, 16, v0 + 4]})]
        C.add(reg, 'new BlockUtilityFixture("%s", new double[]{%s})'
              % (reg, ", ".join(fmt(v) for v in extent(els))),
              names, {reg: model({"label": C.T("labels"), "particle": C.T("labels")}, els)},
              lc.facing_state(C.M(reg)), tab=TAB)


electric()
disconnect()
main_panel()
switchboard()
gas()
water()
labels()
C.add_lang("itemGroup.tabutilities", ("CSM: Utilities", "CSM: Versorgung", "CSM: Servicios",
                                      "CSM: Försörjning"))

if __name__ == "__main__":
    sys.exit(C.main())
