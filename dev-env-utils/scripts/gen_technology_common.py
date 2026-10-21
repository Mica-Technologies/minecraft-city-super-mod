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
