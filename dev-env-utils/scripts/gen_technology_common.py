"""
gen_technology_common.py -- what the Technology module's computer and network generators share.

Not run on its own. gen_technology_computers.py and gen_technology_networking.py import it for:

  * a Catalogue of blocks writing into the Technology module's tree: the Life Safety catalogue
    (registry, the Java that constructs it, four-language names, models, blockstate, optional
    item model, extra files) pointed at modules/technology, with its writing, --check and
    --fragments, and model_depth's separating and pruning run over everything it writes;
  * the shared swatches under textures/blocks/technology/common/: white plastic, grey ABS,
    black anodised aluminium, the RGB lighting strip (an animated texture, so it cycles with
    nothing ticking) and the Micafi badge. Every catalogue built here writes them, byte for byte
    the same, so each generator's --check covers them and neither depends on the other having
    run;
  * the Micafi logo mark: the house networking brand (the Mica family of Micaplex and
    Micarolla), an original mark -- a lowercase "m" with a single signal arc rising off its
    right shoulder -- drawn here so it is the same on every machine;
  * animated textures: a strip of frames and its .mcmeta, so a port LED blinks with no tile
    entity and no renderer;
  * the display transforms a desk device's item model uses.

Every model faces north (a desk device's front, where its user sits, at z = 0), as every
rotatable block in the mod is drawn.
"""
import colorsys
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lsc  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "technology", "src", "main", "resources", "assets", "csm")
LOCALES = lsc.LOCALES

# The Life Safety helpers, re-exported so a Technology generator imports one module.
box = lsc.box
face = lsc.face
model = lsc.model
fill = lsc.fill
rect = lsc.rect
frame = lsc.frame
bevel = lsc.bevel
disc = lsc.disc
shade = lsc.shade
clamp = lsc.clamp
draw_text = lsc.draw_text
draw_text_centred = lsc.draw_text_centred
text_width = lsc.text_width
facing_state = lsc.facing_state
dump = lsc.dump

COMPUTERS = "Technology - Computers & Peripherals"
NETWORKING = "Technology - Networking & IT"

COMMON_DIR = "technology/common"

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
WHITE_PLASTIC = (234, 235, 233)
GREY_ABS = (138, 141, 144)
BLACK_ANODISED = (34, 35, 38)
PORT_BLACK = (16, 17, 18)
PIN_GOLD = (196, 160, 72)
LED_GREEN = (70, 236, 96)
LED_AMBER = (255, 176, 40)
LED_OFF = (52, 56, 54)
MICAFI_TEAL = (0, 170, 160)


def T(name):
    """A shared swatch's texture path."""
    return "csm:blocks/%s/%s" % (COMMON_DIR, name)


# ------------------------------------------------------------------------------------------
# The Micafi mark
# ------------------------------------------------------------------------------------------
def micafi_mark(img, x, y, size, colour, k=8):
    """Draws the Micafi mark into img with its top-left at (x, y), size texels square.

    A lowercase "m" -- one stem and two round shoulders -- whose right shoulder sends up a single
    arc, as a signal leaves an antenna. Drawn k times larger and reduced, so it stays a mark and
    not a smear at eight texels. It is the brand's own: no U, no grid of squares."""
    S = size * k
    big = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    c = tuple(colour[:3]) + (255,)
    w = max(1, int(round(S * 0.13)))        # stroke
    top = S * 0.46                          # the m's x-height top
    base = S * 0.94
    left, right = S * 0.06, S * 0.70
    mid = (left + right) / 2.0
    # the stem
    d.rectangle((left, top, left + w, base), fill=c)
    # two shoulders: half-rings from stem to the middle leg, middle leg to the right leg
    for x0, x1 in ((left, mid + w / 2.0), (mid - w / 2.0, right)):
        bw = x1 - x0
        d.arc((x0, top, x1, top + bw), 180, 360, fill=c, width=w)
        d.rectangle((x1 - w, top + bw / 2.0, x1, base), fill=c)
    # the signal arc: a quarter ring about the right shoulder, from above it round to its right
    cx, cy = right - w / 2.0, top + w * 0.5
    r = S * 0.40
    d.arc((cx - r, cy - r, cx + r, cy + r), 272, 358, fill=c, width=w)
    small = big.resize((size, size), Image.LANCZOS)
    img.alpha_composite(small, (int(x), int(y)))


def micafi_badge(size=16):
    """The badge: the mark in white on a rounded teal square, for a decal."""
    k = 8
    S = size * k
    big = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(big).rounded_rectangle((0, 0, S - 1, S - 1), radius=S * 0.22,
                                          fill=MICAFI_TEAL + (255,))
    img = big.resize((size, size), Image.LANCZOS)
    inset = max(1, size // 8)
    micafi_mark(img, inset, inset, size - 2 * inset, (250, 252, 250))
    return img


# ------------------------------------------------------------------------------------------
# Animated textures
# ------------------------------------------------------------------------------------------
def strip(frames):
    """Stacks equal square frames into the vertical strip an animated texture is."""
    w = frames[0].width
    out = Image.new("RGBA", (w, w * len(frames)))
    for i, im in enumerate(frames):
        out.paste(im, (0, i * w))
    return out


def mcmeta(frametime, interpolate=False):
    meta = {"animation": {"frametime": frametime}}
    if interpolate:
        meta["animation"]["interpolate"] = True
    return json.dumps(meta, indent=2) + "\n"


def rgb_strip():
    """The RGB strip: a rainbow running along a 16 px band, one hue step a frame, blended
    between frames so the colours flow rather than step."""
    frames = []
    n = 16
    for f in range(n):
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        for x in range(16):
            r, g, b = colorsys.hsv_to_rgb(((x + f) % 16) / 16.0, 0.85, 1.0)
            for y in range(16):
                # brighter along the middle, as an LED strip behind a diffuser is
                k = 1.0 - 0.25 * abs(y - 7.5) / 7.5
                px[x, y] = clamp((r * 255 * k, g * 255 * k, b * 255 * k)) + (255,)
        frames.append(img)
    return strip(frames)


# ------------------------------------------------------------------------------------------
# Shared swatches
# ------------------------------------------------------------------------------------------
SHARED_TEXTURES = {
    "white_plastic": lambda: fill(WHITE_PLASTIC, 16, 2, 301),
    "grey_abs": lambda: fill(GREY_ABS, 16, 3, 302),
    "black_anodised": lambda: fill(BLACK_ANODISED, 16, 2, 303),
    "port_black": lambda: fill(PORT_BLACK, 16, 1, 304),
    "rgb_strip": rgb_strip,
    "micafi_badge": lambda: micafi_badge(16),
}
SHARED_MCMETA = {"rgb_strip": mcmeta(2, interpolate=True)}


class TechCatalogue(lsc.Catalogue):
    """The Life Safety catalogue pointed at the Technology module, that also writes the shared
    swatches under textures/blocks/technology/common/."""

    def generate(self, assets):
        written = []
        for name, draw in sorted(SHARED_TEXTURES.items()):
            rel = "textures/blocks/%s/%s.png" % (COMMON_DIR, name)
            path = os.path.join(assets, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            draw().save(path)
            written.append(rel)
        for name, text in sorted(SHARED_MCMETA.items()):
            rel = "textures/blocks/%s/%s.png.mcmeta" % (COMMON_DIR, name)
            with open(os.path.join(assets, rel), "w", newline="\n", encoding="utf-8") as fh:
                fh.write(text)
            written.append(rel)
        return written + lsc.Catalogue.generate(self, assets)

    # Items drawn by a model of their own (a rack unit is a small box on the unit sheet), not
    # by a sprite: no texture is written for them, so they add nothing to the atlas.
    def add_model_item(self, registry, java, names, item_model, tab=None):
        """An item whose models/item/<registry>.json is ``item_model``; its names are
        item.<registry>.name and its tab line is ``initTabItem(<java>)``."""
        assert len(names) == 4, registry
        if not hasattr(self, "model_items"):
            self.model_items = []
        self.model_items.append({"registry": registry, "java": java, "names": names,
                                 "tab": tab})
        self.extra["models/item/%s.json" % registry] = json.dumps(item_model, indent=2) + "\n"

    def lang_entries(self):
        out = lsc.Catalogue.lang_entries(self)
        for it in getattr(self, "model_items", []):
            for i, loc in enumerate(LOCALES):
                out[loc]["item.%s.name" % it["registry"]] = it["names"][i]
        return out

    def fragments(self):
        model_items = getattr(self, "model_items", [])
        if not model_items:
            return lsc.Catalogue.fragments(self)
        out = []
        tabs = []
        for entry in self.blocks + self.items + model_items:
            if entry["tab"] not in tabs:
                tabs.append(entry["tab"])
        for t in tabs:
            out.append("    // --- %s (%s --fragments) ---" % (t or "tab", self.name))
            out += ["    initTabBlock(%s);" % b["java"] for b in self.blocks if b["tab"] == t]
            out += ["    initTabItem(%s);" % it["java"] for it in self.items + model_items
                    if it["tab"] == t]
        return "\n".join(out)


def catalogue(name, sub):
    """A catalogue writing textures under textures/blocks/technology/<sub> and models under
    models/block/technology/<sub>, into the Technology module's tree."""
    return TechCatalogue(name, "technology/" + sub, "technology/" + sub, assets=ASSETS)


def animation(cat, name, frames, frametime, interpolate=False):
    """Registers an animated texture in cat: ``frames`` is a list of functions drawing one
    square frame each; the texture is their strip and its .mcmeta goes into the catalogue's
    extra files."""
    cat.textures[name] = lambda: strip([f() for f in frames])
    rel = "textures/blocks/%s/%s.png.mcmeta" % (cat.tex_dir, name)
    cat.extra[rel] = mcmeta(frametime, interpolate)


# ------------------------------------------------------------------------------------------
# Pictures and elements shared by the computer, network and rack generators
# ------------------------------------------------------------------------------------------
ALL = ("north", "south", "east", "west", "up", "down")
FULL = [0, 0, 16, 16]


def _pp():
    """gen_park_plantings, for its fitted boxes; imported on first use."""
    import gen_park_plantings
    return gen_park_plantings


def rgba(c):
    return tuple(int(v) for v in c[:3]) + ((int(c[3]),) if len(c) == 4 else (255,))


class Pic(object):
    """A picture for one model face, drawn in the face's own units (model pixels, x right and y
    down as the face is seen), supersampled and squeezed into an n x n texture: on a face of that
    aspect with uv 0..16 it comes out the right shape."""

    def __init__(self, w, h, n=32, ss=6, bg=(0, 0, 0)):
        self.w, self.h, self.n = float(w), float(h), n
        self.s = n * ss / max(self.w, self.h)
        self.img = Image.new("RGBA", (max(1, int(round(w * self.s))),
                                      max(1, int(round(h * self.s)))), rgba(bg))
        self.d = ImageDraw.Draw(self.img)

    def rect(self, x0, y0, x1, y1, c):
        s = self.s
        if x1 * s - 1 >= x0 * s and y1 * s - 1 >= y0 * s:
            self.d.rectangle((x0 * s, y0 * s, x1 * s - 1, y1 * s - 1), fill=rgba(c))

    def frame(self, x0, y0, x1, y1, c, w):
        self.rect(x0, y0, x1, y0 + w, c)
        self.rect(x0, y1 - w, x1, y1, c)
        self.rect(x0, y0, x0 + w, y1, c)
        self.rect(x1 - w, y0, x1, y1, c)

    def ell(self, cx, cy, rx, ry, c):
        s = self.s
        self.d.ellipse(((cx - rx) * s, (cy - ry) * s, (cx + rx) * s, (cy + ry) * s), fill=rgba(c))

    def disc(self, cx, cy, r, c):
        self.ell(cx, cy, r, r, c)

    def ring(self, cx, cy, r, w, c):
        s = self.s
        self.d.ellipse(((cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s),
                       outline=rgba(c), width=max(1, int(round(w * s))))

    def poly(self, pts, c):
        self.d.polygon([(x * self.s, y * self.s) for x, y in pts], fill=rgba(c))

    def line(self, pts, c, w):
        self.d.line([(x * self.s, y * self.s) for x, y in pts], fill=rgba(c),
                    width=max(1, int(round(w * self.s))))

    def vgrad(self, x0, y0, x1, y1, c0, c1, steps=40):
        for i in range(steps):
            k = i / float(steps - 1)
            c = tuple(c0[j] + (c1[j] - c0[j]) * k for j in range(3))
            self.rect(x0, y0 + (y1 - y0) * i / steps, x1, y0 + (y1 - y0) * (i + 1) / steps + 0.01,
                      c)

    def done(self):
        return self.img.resize((self.n, self.n), Image.LANCZOS)


def grain(img, amount, seed):
    """A little per-texel grain over a finished texture."""
    rng = random.Random(seed)
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            d = rng.uniform(-amount, amount)
            px[x, y] = clamp((r + d, g + d, b + d)) + (a,)
    return img


def anim(cat, name, n, draw, frametime, interpolate=False):
    """An animated texture of n frames in catalogue cat; draw(f) returns frame f."""
    animation(cat, name, [(lambda f=f: draw(f)) for f in range(n)], frametime, interpolate)


def E(frm, to, t, faces=ALL, per=None, uv=None, rot=None, full=(), turn=None, shade=True):
    """A box whose faces take their uv from their place (slid, never clamped, when it reaches
    past the cell), a per-face texture key from per, explicit uvs, face rotations, faces in
    ``full`` taking a whole picture (uv 0..16), and an element turn (axis, angle, origin)."""
    outside = any(v < 0 or v > 16 for v in list(frm) + list(to))
    el = (_pp().wbox if outside else _pp().box)(list(frm), list(to), t, faces=faces, per=per)
    for f in full:
        if f in el["faces"]:
            el["faces"][f]["uv"] = list(FULL)
    for f, u in (uv or {}).items():
        if f in el["faces"]:
            el["faces"][f]["uv"] = [round(v, 4) for v in u]
    for f, r in (rot or {}).items():
        if f in el["faces"]:
            el["faces"][f]["rotation"] = r
    if turn:
        axis, angle, origin = turn
        el["rotation"] = {"origin": [round(v, 4) for v in origin], "axis": axis, "angle": angle}
    if not shade:
        el["shade"] = False
    return el


def extent(els, clamp_to_block=True):
    """The Java box of a model: its elements' extent (turned ones by their corners), in
    sixteenths, clamped to the block across and to 0..16 up (a selection box past the block
    would hide its neighbours from the cursor)."""
    pts = []
    for e in els:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        corners = [[x, y, z] for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
        r = e.get("rotation")
        if r:
            a = math.radians(r["angle"])
            c, n = math.cos(a), math.sin(a)
            o = r["origin"]
            i, j = {"x": (1, 2), "y": (2, 0), "z": (0, 1)}[r["axis"]]
            for p in corners:
                u, v = p[i] - o[i], p[j] - o[j]
                p[i], p[j] = o[i] + c * u - n * v, o[j] + n * u + c * v
        pts += corners
    lo = [min(p[k] for p in pts) for k in range(3)]
    hi = [max(p[k] for p in pts) for k in range(3)]
    b = [lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]]
    b = [round(math.floor(v * 4 + 1e-6) / 4.0, 2) if i < 3 else round(math.ceil(v * 4 - 1e-6) / 4.0, 2)
         for i, v in enumerate(b)]
    if clamp_to_block:
        b = [max(0.0, min(16.0, v)) for v in b]
    return b


# ------------------------------------------------------------------------------------------
# Models and Java
# ------------------------------------------------------------------------------------------
def B(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None, rot=None):
    """A box whose faces take their uv from their place, or as given; rot turns a face's
    texture (a desk device's top is drawn as its user sees it, so it is turned 180)."""
    el = box(list(frm), list(to), tex, faces=faces)
    for f, u in (uv or {}).items():
        if f in el["faces"]:
            el["faces"][f]["uv"] = [round(v, 4) for v in u]
    for f, r in (rot or {}).items():
        if f in el["faces"]:
            el["faces"][f]["rotation"] = r
    return el


def desk_display(scale=0.8, y=0.0):
    """Item transforms for a low device that sits on a desk: the block's three-quarter view,
    larger than a full block's and lifted, since the device is a few pixels tall."""
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, y, 0], "scale": [scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4] * 3},
        "fixed": {"rotation": [90, 0, 0], "translation": [0, 0, 0], "scale": [scale] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0],
                                  "scale": [0.5] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0],
                                 "scale": [0.5] * 3},
    }


def with_display(m, display):
    m = dict(m)
    m["display"] = display
    return m


def jnum(v):
    s = "%g" % v
    return s if "." in s else s + ".0"


def box_java(b):
    return "new double[]{%s}" % ", ".join(jnum(v) for v in b)


def desk_device_java(registry, b):
    """The tab line of a BlockDeskDeviceFactory: turns to the placer, never stands on end."""
    return 'new BlockDeskDeviceFactory("%s", %s)' % (registry, box_java(b))


def names(*n):
    assert len(n) == 4
    return n
