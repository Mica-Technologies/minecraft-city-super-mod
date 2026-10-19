"""
gen_healthcare_common.py -- what the Healthcare generators share.

Not run on its own. gen_healthcare_wards.py (and the later departments' generators) import it
for:

  * a Catalogue of blocks writing into the Healthcare module's tree: the Life Safety catalogue
    (registry, the Java that constructs it, four-language names, models, blockstate, optional item
    model, extra files) pointed at modules/healthcare, with its writing, --check and --fragments,
    and model_depth's separating and pruning run over everything it writes;
  * the hospital palette: hospital white, pale blue and green, stainless, vinyl and the frame
    greys, drawn as small swatch textures that every model shares;
  * a generic medical emblem (a six-armed star drawn here; never a red cross, which is a
    protected emblem, and never a real brand);
  * animated screen textures: a strip of frames and its .mcmeta, so a monitor's trace moves with
    no tile entity and no renderer;
  * Forge blockstates for the two facings the module stores: a floor piece's horizontal facing
    (metadata S0 W1 N2 E3, as AbstractBlockRotatableNSEW stores it) and a wall device's facing
    (metadata 2 N, 3 S, 4 W, 5 E, EnumFacing's own index) -- both are the "facing" property to a
    blockstate, so the JSON is the same.

Every model faces north with what it stands against at +Z (a wall at z = 16), as every
rotatable block in the mod is drawn.
"""
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lsc  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "healthcare", "src", "main", "resources", "assets", "csm")
LOCALES = lsc.LOCALES

# The Life Safety helpers, re-exported so a Healthcare generator imports one module.
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
north_region = lsc.north_region
post = lsc.post
pipe_x = lsc.pipe_x
pipe_z = lsc.pipe_z
rbox = lsc.rbox
facing_state = lsc.facing_state
dump = lsc.dump

PATIENT_CARE = "Healthcare - Patient Care"
CLINICAL = "Healthcare - Clinical"
HIDDEN = "hidden"

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
HOSPITAL_WHITE = (232, 234, 230)
WARM_WHITE = (226, 222, 210)
PALE_BLUE = (170, 198, 214)
PALE_GREEN = (176, 206, 184)
MAUVE = (176, 140, 170)
STAINLESS = (186, 190, 194)
FRAME_GREY = (128, 134, 140)
DARK_GREY = (62, 66, 72)
CHARCOAL = (40, 42, 46)
VINYL_BLUE = (70, 112, 150)
VINYL_TEAL = (58, 130, 128)
VINYL_GREEN = (84, 134, 92)
BEECH = (204, 170, 120)
SCREEN_BLACK = (14, 18, 20)
CALL_AMBER = (250, 178, 40)
CALL_RED = (236, 48, 40)
CALL_WHITE = (246, 244, 236)
O2_GREEN = (40, 150, 70)
AIR_YELLOW = (226, 196, 40)
VAC_WHITE = (232, 232, 232)
BIOHAZARD_RED = (196, 36, 36)
SAFETY_YELLOW = (236, 196, 32)


def catalogue(name, sub):
    """A catalogue writing textures under textures/blocks/healthcare/<sub> and models under
    models/block/healthcare/<sub>, into the Healthcare module's tree."""
    return lsc.Catalogue(name, "healthcare/" + sub, "healthcare/" + sub, assets=ASSETS)


def swatch(colour, seed, grain=3, size=16):
    """A flat painted or plastic surface: the swatch every model of that colour shares."""
    return fill(colour, size=size, grain=grain, seed=seed)


def brushed(colour, seed, size=16):
    """Brushed stainless: fine horizontal streaks."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    rows = [rng.uniform(-0.06, 0.06) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            px[x, y] = shade(colour, 1.0 + rows[y] + rng.uniform(-0.015, 0.015)) + (255,)
    return img


def fabric(colour, seed, size=16, pattern=None):
    """A woven cloth: a fine basket weave, and optionally a print over it ("check", "dots",
    "leaf") in a lighter or darker shade of the cloth."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            k = (1.03 if (x + y) % 2 == 0 else 0.97) * (1.0 + rng.uniform(-0.025, 0.025))
            c = shade(colour, k)
            if pattern == "check" and (x % 8 < 2 or y % 8 < 2):
                c = shade(colour, 0.82 if (x % 8 < 2 and y % 8 < 2) else 0.9)
            elif pattern == "dots" and (x % 4 == 1 and y % 4 == (1 if (x // 4) % 2 == 0 else 3)):
                c = shade(colour, 1.22)
            elif pattern == "wave":
                band = (y + int(round(1.5 * math.sin(x * 2 * math.pi / size)))) % 8
                if band in (0, 1):
                    c = shade(colour, 0.84)
            px[x, y] = c + (255,)
    return img


# ------------------------------------------------------------------------------------------
# The generic medical emblem
# ------------------------------------------------------------------------------------------
def emblem(img, cx, cy, r, colour, width=None):
    """A six-armed star of three crossed bars, 60 degrees apart, centred on (cx, cy) with arms r
    long: the generic medical mark the module uses wherever a sign or a device wants one. It is
    not a red cross (a protected emblem) and not any agency's or maker's mark."""
    px = img.load()
    width = width if width is not None else max(1.0, r * 0.42)
    c = clamp(colour[:3]) + (255,)
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            if dx * dx + dy * dy > r * r:
                continue
            for a in (90, 30, 150):
                t = math.radians(a)
                # distance from the bar's centre line
                d = abs(-math.sin(t) * dx + math.cos(t) * dy)
                if d <= width / 2:
                    px[x, y] = c
                    break


# ------------------------------------------------------------------------------------------
# Animated textures
# ------------------------------------------------------------------------------------------
def animation(cat, name, frames, frametime, interpolate=False):
    """Registers an animated texture: ``frames`` is a list of functions drawing one square frame
    each; the texture is their vertical strip, and its .mcmeta goes into the catalogue's extra
    files, so the game plays it with nothing ticking."""
    def draw():
        imgs = [f() for f in frames]
        w = imgs[0].width
        strip = Image.new("RGBA", (w, w * len(imgs)))
        for i, im in enumerate(imgs):
            strip.paste(im, (0, i * w))
        return strip
    cat.textures[name] = draw
    meta = {"animation": {"frametime": frametime}}
    if interpolate:
        meta["animation"]["interpolate"] = True
    rel = "textures/blocks/%s/%s.png.mcmeta" % (cat.tex_dir, name)
    cat.extra[rel] = json.dumps(meta, indent=2) + "\n"


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
ROT_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def keyed_state(props, choose):
    """A Forge blockstate listing every combination of ``props`` (an ordered list of
    (name, [values])) as a full variant key; ``choose(values_dict)`` returns the variant body
    ({"model": ..., "y": ..., "textures": ...}). Used where two properties together pick the
    geometry, which a property-keyed Forge blockstate cannot express."""
    variants = {}

    def walk(i, acc):
        if i == len(props):
            key = ",".join("%s=%s" % (n, acc[n]) for n in sorted(acc))
            variants[key] = [choose(dict(acc))]
            return
        name, values = props[i]
        for v in values:
            acc[name] = v
            walk(i + 1, acc)
        del acc[name]
    walk(0, {})
    return {"forge_marker": 1, "variants": variants}


def check_json(paths):
    """Parses every JSON file the generator wrote, so a malformed one fails the run."""
    for p in paths:
        if p.endswith(".json") or p.endswith(".mcmeta"):
            with open(p, encoding="utf-8") as fh:
                json.load(fh)
