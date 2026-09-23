"""
life_safety_gen_common.py -- what the Life Safety generators share.

Not run on its own. gen_fire_protection.py, gen_emergency_lighting.py and
gen_emergency_services.py import it for:

  * a Catalogue of blocks (registry, the Java that constructs it, four-language names, models,
    blockstate, optional item model) and the writing, --check and --fragments that go with it;
  * a 3 x 5 pixel font, drawn here rather than loaded from the system so a texture is the same
    on every machine and --check means something;
  * texture helpers: flat fills with a little grain, bevelled panels, text and pictograms placed
    by the model face they will be seen on;
  * element helpers: a fitted box (from gen_park_plantings.py) and "round" parts made the way the
    construction site's are, a square and the same square turned 45 degrees.

Every Life Safety model faces north with what it hangs on at +Z (a wall at z = 16), as every
wall-mounted accessory in the mod does.
"""
import argparse
import json
import math
import os
import random
import shutil
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_park_plantings as pp  # noqa: E402
import gen_trees  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "lifesafety", "src", "main", "resources", "assets", "csm")
LOCALES = ["en_us", "de_de", "es_es", "sv_se"]

box = pp.box
face = pp.face
model = pp.model


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def shade(c, k):
    """c scaled by k (k < 1 darker, > 1 lighter), clamped."""
    return clamp(tuple(v * k for v in c[:3]))


# ------------------------------------------------------------------------------------------
# Pixel font: 3 x 5 capitals and digits
# ------------------------------------------------------------------------------------------
_GLYPHS = {
    "A": ["010", "101", "111", "101", "101"], "B": ["110", "101", "110", "101", "110"],
    "C": ["011", "100", "100", "100", "011"], "D": ["110", "101", "101", "101", "110"],
    "E": ["111", "100", "110", "100", "111"], "F": ["111", "100", "110", "100", "100"],
    "G": ["011", "100", "101", "101", "011"], "H": ["101", "101", "111", "101", "101"],
    "I": ["111", "010", "010", "010", "111"], "J": ["001", "001", "001", "101", "010"],
    "K": ["101", "101", "110", "101", "101"], "L": ["100", "100", "100", "100", "111"],
    "M": ["101", "111", "111", "101", "101"], "N": ["110", "101", "101", "101", "101"],
    "O": ["010", "101", "101", "101", "010"], "P": ["110", "101", "110", "100", "100"],
    "Q": ["010", "101", "101", "110", "011"], "R": ["110", "101", "110", "101", "101"],
    "S": ["011", "100", "010", "001", "110"], "T": ["111", "010", "010", "010", "010"],
    "U": ["101", "101", "101", "101", "111"], "V": ["101", "101", "101", "101", "010"],
    "W": ["101", "101", "111", "111", "101"], "X": ["101", "101", "010", "101", "101"],
    "Y": ["101", "101", "010", "010", "010"], "Z": ["111", "001", "010", "100", "111"],
    "0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"],
    "2": ["110", "001", "010", "100", "111"], "3": ["110", "001", "010", "001", "110"],
    "4": ["101", "101", "111", "001", "001"], "5": ["111", "100", "110", "001", "110"],
    "6": ["011", "100", "111", "101", "111"], "7": ["111", "001", "010", "010", "010"],
    "8": ["111", "101", "111", "101", "111"], "9": ["111", "101", "111", "001", "110"],
    " ": ["000", "000", "000", "000", "000"], "-": ["000", "000", "111", "000", "000"],
    ".": ["000", "000", "000", "000", "010"], "&": ["010", "101", "010", "101", "011"],
    "/": ["001", "001", "010", "100", "100"], "#": ["101", "111", "101", "111", "101"],
    "+": ["000", "010", "111", "010", "000"], "!": ["010", "010", "010", "000", "010"],
}


def text_width(text, scale=1):
    return (len(text) * 4 - 1) * scale


def draw_text(img, text, x, y, colour, scale=1):
    """Draws text with its top-left at (x, y), each font pixel scale x scale texels."""
    px = img.load()
    colour = clamp(colour) + (255,)
    for i, ch in enumerate(text.upper()):
        glyph = _GLYPHS.get(ch, _GLYPHS[" "])
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            X = x + (i * 4 + gx) * scale + sx
                            Y = y + gy * scale + sy
                            if 0 <= X < img.width and 0 <= Y < img.height:
                                px[X, Y] = colour


def draw_text_centred(img, text, cx, y, colour, scale=1):
    draw_text(img, text, int(round(cx - text_width(text, scale) / 2)), y, colour, scale)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def fill(colour, size=16, grain=4, seed=1, alpha=255):
    """A flat colour with a little per-texel grain, so a painted surface is not dead flat."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            d = rng.uniform(-grain, grain)
            px[x, y] = clamp(tuple(v + d for v in colour[:3])) + (alpha,)
    return img


def rect(img, x0, y0, x1, y1, colour):
    """Fills the texel rectangle x0 <= x < x1, y0 <= y < y1."""
    px = img.load()
    c = clamp(colour[:3]) + ((colour[3],) if len(colour) == 4 else (255,))
    for y in range(max(0, y0), min(img.height, y1)):
        for x in range(max(0, x0), min(img.width, x1)):
            px[x, y] = c


def frame(img, x0, y0, x1, y1, colour, width=1):
    rect(img, x0, y0, x1, y0 + width, colour)
    rect(img, x0, y1 - width, x1, y1, colour)
    rect(img, x0, y0, x0 + width, y1, colour)
    rect(img, x1 - width, y0, x1, y1, colour)


def bevel(img, x0, y0, x1, y1, colour, light=1.18, dark=0.72):
    """A raised panel: its fill with a lit top-left edge and a shaded bottom-right edge."""
    rect(img, x0, y0, x1, y1, colour)
    rect(img, x0, y0, x1, y0 + 1, shade(colour, light))
    rect(img, x0, y0, x0 + 1, y1, shade(colour, light))
    rect(img, x0, y1 - 1, x1, y1, shade(colour, dark))
    rect(img, x1 - 1, y0, x1, y1, shade(colour, dark))


def disc(img, cx, cy, r, colour):
    px = img.load()
    c = clamp(colour[:3]) + ((colour[3],) if len(colour) == 4 else (255,))
    for y in range(img.height):
        for x in range(img.width):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                px[x, y] = c


def north_region(frm, to, size):
    """The texel rectangle (x0, y0, x1, y1) a north face of the box frm..to covers on a size-px
    texture, as box() maps it: u runs from x = 16 at the left (seen from the north) to 0."""
    k = size / 16.0
    return (int(round((16 - to[0]) * k)), int(round((16 - to[1]) * k)),
            int(round((16 - frm[0]) * k)), int(round((16 - frm[1]) * k)))


def cylinder_shading(img, x0, x1, base, axis="x"):
    """Shades columns x0..x1 of a texture as the curve of a cylinder seen side on: lit left of
    centre, dark at both edges."""
    px = img.load()
    for y in range(img.height):
        for x in range(x0, x1):
            f = (x + 0.5 - x0) / (x1 - x0)
            k = 0.72 + 0.5 * (1 - abs(f - 0.38) * 2.1)
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = shade((r, g, b), k) + (a,)


# ------------------------------------------------------------------------------------------
# Elements
# ------------------------------------------------------------------------------------------
def rbox(frm, to, tex, axis, angle, origin, faces=("north", "south", "east", "west", "up",
                                                  "down")):
    b = box(frm, to, tex, faces=faces)
    b["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}
    return b


TAN_22_5 = math.tan(math.pi / 8)


def _octagon(axis, c1, c2, r, lo, hi, tex, ends, cap_front=True, cap_back=True):
    """A regular octagonal prism of inradius r along axis ("y", "z" or "x"), centred on (c1, c2)
    in the other two axes, from lo to hi along its own.

    Four rectangles: two square to the axes and two turned 45 degrees, each r long and
    r tan(22.5) wide, whose corners are exactly the octagon's. (A square and the same square
    turned 45 degrees is not round: its corners stand out as an eight-pointed star.) Only each
    rectangle's two long sides are outside faces, so only those are drawn. Each rectangle draws
    its own ends, which together cover the octagon; they sit a hair apart along the axis, so the
    four coplanar caps do not fight.
    """
    a = r * TAN_22_5
    els = []
    # (half-extent across the first cross axis, half-extent across the second, turned 45?)
    shapes = [(r, a, False), (a, r, False), (r, a, True), (a, r, True)]
    for k, (h1, h2, turned) in enumerate(shapes):
        eps = 0.004 * k
        if axis == "y":
            frm = [c1 - h1, lo - eps, c2 - h2]
            to = [c1 + h1, hi + eps, c2 + h2]
            sides = ["east", "west"] if h1 == r else ["north", "south"]
            end_lo, end_hi = "down", "up"
        elif axis == "z":
            frm = [c1 - h1, c2 - h2, lo - eps]
            to = [c1 + h1, c2 + h2, hi + eps]
            sides = ["east", "west"] if h1 == r else ["up", "down"]
            end_lo, end_hi = "north", "south"
        else:
            frm = [lo - eps, c1 - h1, c2 - h2]
            to = [hi + eps, c1 + h1, c2 + h2]
            sides = ["up", "down"] if h1 == r else ["north", "south"]
            end_lo, end_hi = "west", "east"
        faces = list(sides)
        if ends:
            if cap_front:
                faces.append(end_lo)
            if cap_back:
                faces.append(end_hi)
        b = box(frm, to, tex, faces=faces)
        if turned:
            origin = {"y": [c1, lo, c2], "z": [c1, c2, lo], "x": [lo, c1, c2]}[axis]
            b["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": 45}
        els.append(b)
    return els


def post(cx, cz, r, y0, y1, tex, top=True, bottom=True):
    """An upright round part about (cx, cz): a regular octagon of inradius r, from y0 to y1."""
    return _octagon("y", cx, cz, r, y0, y1, tex, top or bottom, cap_front=bottom, cap_back=top)


def pipe_z(cx, cy, r, z0, z1, tex, front=True):
    """A round part lying along z about (cx, cy), from z0 to z1 (its front, north, end at z0)."""
    return _octagon("z", cx, cy, r, z0, z1, tex, front, cap_front=True, cap_back=False)


def pipe_x(cy, cz, r, x0, x1, tex, ends=True):
    """A round part lying along x about (cy, cz)."""
    return _octagon("x", cy, cz, r, x0, x1, tex, ends)


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
def facing_state(model_path, extra=None):
    """Forge blockstate for a horizontal-facing block, with optional extra property variants
    (e.g. {"open": {"true": {"model": ...}, "false": {}}})."""
    variants = {
        "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}},
        "inventory": [{}],
    }
    if extra:
        variants.update(extra)
    return {"forge_marker": 1, "defaults": {"model": model_path}, "variants": variants}


def nsewud_state(model_path):
    """Forge blockstate for a block that faces any of six ways (a detector on a ceiling faces
    down), as the fire alarm devices' blockstates do."""
    return {"forge_marker": 1, "defaults": {"model": model_path},
            "variants": {
                "facing": {"down": {"x": 90}, "east": {"y": 90}, "north": {}, "south": {"y": 180},
                           "up": {"x": 270}, "west": {"y": 270}},
                "inventory": [{}], "normal": [{}]}}


# ------------------------------------------------------------------------------------------
# Catalogue and output
# ------------------------------------------------------------------------------------------
class Catalogue(object):
    """One generator's blocks, textures and extra lang, and how to write and check them."""

    def __init__(self, name, tex_dir, model_dir):
        self.name = name
        self.tex_dir = tex_dir          # e.g. "lifesafety/fireprotection"
        self.model_dir = model_dir      # e.g. "lifesafety/fireprotection"
        self.blocks = []
        self.textures = {}
        self.lang = {loc: {} for loc in LOCALES}
        self.items = []                 # (registry, java) item tab lines

    def T(self, name):
        return "csm:blocks/%s/%s" % (self.tex_dir, name)

    def M(self, name):
        return "csm:%s/%s" % (self.model_dir, name)

    def texture(self, name):
        """Decorator: registers a function drawing the named texture."""
        def reg(fn):
            self.textures[name] = fn
            return fn
        return reg

    def add(self, registry, java, names, models, blockstate, item=None, tab=None):
        assert len(names) == 4, registry
        assert registry not in [b["registry"] for b in self.blocks], registry
        self.blocks.append({"registry": registry, "java": java, "names": names,
                            "models": models, "blockstate": blockstate, "item": item,
                            "tab": tab})

    def add_item(self, registry, java, names, texture, tab=None):
        """An item: its Java, names (item.<registry>.name), and the name of one of this
        catalogue's textures, which is written under textures/items/<tex_dir>/ as well."""
        assert len(names) == 4, registry
        self.items.append({"registry": registry, "java": java, "names": names,
                           "texture": texture, "tab": tab})

    def add_lang(self, key, names):
        for i, loc in enumerate(LOCALES):
            self.lang[loc][key] = names[i]

    def lang_entries(self):
        out = {loc: dict(v) for loc, v in self.lang.items()}
        for b in self.blocks:
            for i, loc in enumerate(LOCALES):
                out[loc]["tile.%s.name" % b["registry"]] = b["names"][i]
        for it in self.items:
            for i, loc in enumerate(LOCALES):
                out[loc]["item.%s.name" % it["registry"]] = it["names"][i]
        return out

    def generate(self, assets):
        written = []
        item_textures = set(it["texture"] for it in self.items)
        for name, draw in sorted(self.textures.items()):
            if name in item_textures:
                continue
            rel = "textures/blocks/%s/%s.png" % (self.tex_dir, name)
            path = os.path.join(assets, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            draw().save(path)
            written.append(rel)
        for b in self.blocks:
            for name, data in b["models"].items():
                rel = "models/block/%s/%s.json" % (self.model_dir, name)
                dump(os.path.join(assets, rel), data)
                written.append(rel)
            rel = "blockstates/%s.json" % b["registry"]
            dump(os.path.join(assets, rel), b["blockstate"])
            written.append(rel)
            if b["item"]:
                rel = "models/item/%s.json" % b["registry"]
                dump(os.path.join(assets, rel), b["item"])
                written.append(rel)
        for it in self.items:
            rel = "textures/items/%s/%s.png" % (self.tex_dir, it["texture"])
            path = os.path.join(assets, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            self.textures[it["texture"]]().save(path)
            written.append(rel)
            rel = "models/item/%s.json" % it["registry"]
            dump(os.path.join(assets, rel), {"parent": "item/generated", "textures": {
                "layer0": "csm:items/%s/%s" % (self.tex_dir, it["texture"])}})
            written.append(rel)
        gen_trees.write_lang(os.path.join(assets, "lang"), self.lang_entries())
        written += ["lang/%s.lang" % loc for loc in LOCALES]
        return written

    def fragments(self):
        out = []
        tabs = []
        for b in self.blocks:
            if b["tab"] not in tabs:
                tabs.append(b["tab"])
        for it in self.items:
            if it["tab"] not in tabs:
                tabs.append(it["tab"])
        for t in tabs:
            out.append("    // --- %s (%s --fragments) ---" % (t or "tab", self.name))
            out += ["    initTabBlock(%s);" % b["java"] for b in self.blocks if b["tab"] == t]
            out += ["    initTabItem(%s);" % it["java"] for it in self.items if it["tab"] == t]
        return "\n".join(out)

    def main(self):
        ap = argparse.ArgumentParser(description=self.name)
        ap.add_argument("--check", action="store_true")
        ap.add_argument("--fragments", action="store_true")
        args = ap.parse_args()
        if args.fragments:
            print(self.fragments())
            return 0
        if not args.check:
            written = self.generate(ASSETS)
            print("wrote %d files under %s" % (len(written), ASSETS))
            return 0
        tmp = tempfile.mkdtemp(prefix="lifesafety_")
        try:
            shutil.copytree(os.path.join(ASSETS, "lang"), os.path.join(tmp, "lang"))
            written = self.generate(tmp)
            stale = [rel for rel in written
                     if not gen_trees.same_file(os.path.join(tmp, rel),
                                                os.path.join(ASSETS, rel))]
            if stale:
                print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
                return 1
            print("%s: up to date" % self.name)
            return 0
        finally:
            shutil.rmtree(tmp, ignore_errors=True)


def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")
