#!/usr/bin/env python3
"""Every asset the HVAC tab's exhaust equipment ships (#314).

    py dev-env-utils/scripts/gen_hvac_exhaust.py
    py dev-env-utils/scripts/gen_hvac_exhaust.py --check
    py dev-env-utils/scripts/gen_hvac_exhaust.py --fragments   # tab lines

- **Lab exhaust fan** (BlockHvacLabExhaustFan, a master, and BlockHvacExhaustFanPart): the
  high-plume fan a lab building's exhaust risers end in, 2 x 2 blocks on its roof curb and five
  tall. The master draws the curb, the plenum with its bypass air hood, and the fan housing with
  its motor (y 0 to 32, reaching one block east and south of its cell, the JSON limit); the parts
  over the master draw the white discharge nozzle (`stack`) and its windband (`top`), each a whole
  octagon centred on the corner the four columns share, turned by the unit's facing. Every other
  part draws nothing.
- **Upblast and mushroom exhaust fans** (BlockHvacExhaustFan): the one-block curb fans of a
  kitchen or toilet exhaust, spun aluminium; the upblast discharges straight up through a
  butterfly damper, the mushroom down under its hood.
- **Exhaust grille** (BlockHvacExhaustGrille): a lay-in egg-crate grille flush under the ceiling,
  the room end of an exhaust duct.

Round parts are exact octagons about the vertical axis: four rectangles, two of them turned 45
degrees. Their flat faces take flat textures, since the four rectangles' tops are coplanar.
Every model faces north (the front toward -z); Forge blockstates turn it. The lang lines are
written into the module's four lang files by key, keeping each file's line endings.
"""

import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HVAC = os.path.join(REPO, "modules", "hvac", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(HVAC, "textures", "blocks", "hvac", "exhaust")
MODEL_DIR = os.path.join(HVAC, "models", "block", "hvac", "exhaust")
STATE_DIR = os.path.join(HVAC, "blockstates")
LANG_DIR = os.path.join(HVAC, "lang")
TEX_REF = "csm:blocks/hvac/exhaust/%s"
MODEL_REF = "csm:hvac/exhaust/%s"

GALV = (178, 182, 186)
WHITE = (228, 230, 226)
ALUM = (196, 198, 200)
DARK = (28, 30, 33)
MOTOR = (62, 74, 90)


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------

def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + (255,)


def shade(c, k):
    return tuple(v * k for v in c[:3])


def grain(colour, seed, size=16, amp=3):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = clamp([v + rng.uniform(-amp, amp) for v in colour])
    return img


def rect(img, x0, y0, x1, y1, colour):
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = clamp(colour)


def spangle(colour, seed, size=32):
    """Galvanized steel: a mottle of crystal spangles a few texels across."""
    rng = random.Random(seed)
    img = grain(colour, seed, size, 2)
    px = img.load()
    for _ in range(size * size // 10):
        cx, cy = rng.randrange(size), rng.randrange(size)
        d = rng.uniform(-9, 9)
        for dx in range(rng.randint(1, 3)):
            for dy in range(rng.randint(1, 3)):
                x, y = (cx + dx) % size, (cy + dy) % size
                px[x, y] = clamp([v + d for v in px[x, y][:3]])
    return img


def panel(colour, seed, size=32):
    """A painted panel with a seam round its edge."""
    img = grain(colour, seed, size, 2)
    rect(img, 0, 0, size, 1, shade(colour, 0.82))
    rect(img, 0, size - 1, size, size, shade(colour, 0.72))
    rect(img, 0, 0, 1, size, shade(colour, 0.86))
    rect(img, size - 1, 0, size, size, shade(colour, 0.76))
    return img


def louver():
    """The bypass air hood's face: galvanized louvre blades with a bird screen behind."""
    img = spangle(GALV, 14301)
    for y in range(3, 30, 4):
        rect(img, 2, y, 30, y + 2, shade(GALV, 0.45))
        rect(img, 2, y + 2, 30, y + 3, shade(GALV, 1.12))
    rect(img, 0, 0, 32, 2, shade(GALV, 0.8))
    rect(img, 0, 30, 32, 32, shade(GALV, 0.75))
    rect(img, 0, 0, 2, 32, shade(GALV, 0.85))
    rect(img, 30, 0, 32, 32, shade(GALV, 0.8))
    return img


def curb():
    """A roof curb: galvanized, with the roofing's counterflashing line low down."""
    img = spangle(GALV, 14302, 16)
    rect(img, 0, 11, 16, 12, shade(GALV, 0.6))
    rect(img, 0, 12, 16, 16, (74, 76, 80))
    return img


def stack():
    """The discharge nozzle: white, with a flanged joint at each block line."""
    img = grain(WHITE, 14303, 16, 2)
    rect(img, 0, 0, 16, 1, shade(WHITE, 0.78))
    rect(img, 0, 15, 16, 16, shade(WHITE, 0.84))
    return img


def housing():
    """The fan housing: galvanized with a seam band and a row of bolts."""
    img = spangle(GALV, 14304, 16)
    rect(img, 0, 3, 16, 4, shade(GALV, 0.7))
    for x in range(1, 16, 4):
        rect(img, x, 1, x + 1, 2, shade(GALV, 0.55))
    return img


def motor():
    """A motor's cooling fins, painted blue grey."""
    img = grain(MOTOR, 14305, 16, 2)
    for y in range(1, 16, 2):
        rect(img, 0, y, 16, y + 1, shade(MOTOR, 0.7))
    return img


def grille():
    """An egg-crate exhaust grille in its frame."""
    img = grain(WHITE, 14306, 16, 2)
    rect(img, 2, 2, 14, 14, shade(WHITE, 0.25))
    for i in range(2, 15, 3):
        rect(img, i, 2, i + 1, 14, shade(WHITE, 0.92))
        rect(img, 2, i, 14, i + 1, shade(WHITE, 0.92))
    rect(img, 0, 0, 16, 1, shade(WHITE, 0.85))
    rect(img, 0, 15, 16, 16, shade(WHITE, 0.8))
    return img


def damper():
    """A butterfly damper blade: thin aluminium with a stiffening rib."""
    img = grain(ALUM, 14307, 16, 2)
    rect(img, 7, 0, 9, 16, shade(ALUM, 0.82))
    return img


def switch():
    """The fan's disconnect switch box, with its handle."""
    img = grain((96, 100, 104), 14308, 16, 2)
    rect(img, 9, 4, 12, 12, (40, 40, 44))
    rect(img, 10, 3, 11, 6, (200, 40, 36))
    return img


def flat(colour, seed):
    return grain(colour, seed, 16, 1)


TEXTURES = {
    "galv": lambda: spangle(GALV, 14310),
    "galv_flat": lambda: flat(GALV, 14311),
    "white_panel": lambda: panel(WHITE, 14312),
    "white_flat": lambda: flat(WHITE, 14313),
    "louver": louver,
    "curb": curb,
    "stack": stack,
    "housing": housing,
    "motor": motor,
    "dark": lambda: flat(DARK, 14314),
    "alum": lambda: spangle(ALUM, 14315, 16),
    "alum_flat": lambda: flat(ALUM, 14316),
    "grille": grille,
    "damper": damper,
    "switch": switch,
}


# --------------------------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------------------------

SIDES = ("north", "south", "east", "west")
ALL = SIDES + ("up", "down")
WHOLE = [0, 0, 16, 16]


def r4(v):
    return round(v, 4)


def side_uv(width, y0, y1):
    """A side face's uv: centred across the texture, the block row it lies in down it."""
    base = 16 * math.floor(y0 / 16 + 1e-6)
    u0, u1 = 8 - width / 2, 8 + width / 2
    return [r4(max(0, u0)), r4(16 - (y1 - base)), r4(min(16, u1)), r4(16 - (y0 - base))]


def box(frm, to, tex, faces=ALL, per=None, uv=None, rot=None):
    """An element. Unless ``uv`` gives one, a side face's uv is its width centred across the
    texture and its height within the block row; a top or bottom takes the whole texture."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    out = {}
    for f in faces:
        if uv and f in uv:
            u = uv[f]
        elif f in ("north", "south"):
            u = side_uv(x1 - x0, y0, y1)
        elif f in ("east", "west"):
            u = side_uv(z1 - z0, y0, y1)
        else:
            u = WHOLE
        out[f] = {"texture": "#" + (per or {}).get(f, tex), "uv": u}
    e = {"from": [r4(v) for v in frm], "to": [r4(v) for v in to], "faces": out}
    if rot:
        e["rotation"] = rot
    return e


def octagon(cx, cz, r, y0, y1, side, top=None, bottom=None):
    """An exact octagon about the vertical axis through (cx, cz), across flats 2r: four
    rectangles, two of them turned 45 degrees."""
    s = r * math.tan(math.radians(22.5))
    faces = SIDES + (("up",) if top else ()) + (("down",) if bottom else ())
    per = {}
    if top:
        per["up"] = top
    if bottom:
        per["down"] = bottom
    out = []
    for angle in (0, 45):
        rot = {"origin": [cx, r4((y0 + y1) / 2), cz], "axis": "y", "angle": angle} \
            if angle else None
        out.append(box((cx - r, y0, cz - s), (cx + r, y1, cz + s), side, faces, per, rot=rot))
        out.append(box((cx - s, y0, cz - r), (cx + s, y1, cz + r), side, faces, per, rot=rot))
    return out


# The lab fan, model frame: the master at x, z 0..16, the unit over x, z 0..32, the stack's axis
# at (16, 16). Heights in sixteenths from the master's floor.
LAB_CX = LAB_CZ = 16
STACK_R = 6.5
WINDBAND_R = 9.5
HOUSING_R = 11
CONE_R = 8.5


def lab_fan_base():
    """Levels 0 and 1, drawn by the master: curb, plenum, bypass hood, housing, motor."""
    return ([box((1, 0, 1), (31, 3, 31), "curb", SIDES + ("up",),
                 uv={f: [0, 0, 16, 16] for f in SIDES}),
             box((2, 3, 2), (30, 16, 30), "white", SIDES + ("up",),
                 uv={f: WHOLE for f in SIDES}),
             # the bypass air hood on the front, louvres facing out
             box((5, 6, -1.5), (27, 15, 2), "galv", ALL, per={"north": "louver"},
                 uv={"north": WHOLE, "south": WHOLE})]
            + octagon(LAB_CX, LAB_CZ, HOUSING_R, 16, 27, "housing", top="galv_flat")
            + octagon(LAB_CX, LAB_CZ, CONE_R, 27, 32, "galv", top="galv_flat")
            # the motor on the east side, on its belt guard
            + [box((26.5, 17.5, 11.5), (31.5, 24.5, 18.5), "motor", ALL,
                   per={"north": "galv_flat", "south": "galv_flat"}),
               box((24.5, 18.5, 12.5), (27, 30, 17.5), "galv", ALL)])


def lab_fan_stack():
    """A block of the discharge nozzle, drawn by the part over the master."""
    return octagon(LAB_CX, LAB_CZ, STACK_R, 0, 16, "stack")


def lab_fan_top():
    """The nozzle's top and the windband round it."""
    return (octagon(LAB_CX, LAB_CZ, STACK_R, 0, 6, "stack")
            + octagon(LAB_CX, LAB_CZ, WINDBAND_R, 4, 14, "galv", top="dark",
                      bottom="galv_flat"))


def upblast():
    """The upblast fan: curb, base pan, a round housing, and a damper open on top."""
    return ([box((0.5, 0, 0.5), (15.5, 3, 15.5), "curb", SIDES + ("up",),
                 uv={f: [0, 0, 16, 3] for f in SIDES}),
             box((1, 3, 1), (15, 5, 15), "alum", ALL)]
            + octagon(8, 8, 6, 5, 12, "alum")
            + octagon(8, 8, 6.5, 12, 14, "alum", top="dark", bottom="alum_flat")
            + [box((3, 13.6, 8), (13, 14.1, 12.5), "damper", ALL,
                   rot={"origin": [8, 13.85, 8], "axis": "x", "angle": 22.5}),
               box((3, 13.6, 3.5), (13, 14.1, 8), "damper", ALL,
                   rot={"origin": [8, 13.85, 8], "axis": "x", "angle": -22.5}),
               box((11, 3, 0), (14.5, 7.5, 1), "switch", ALL,
                   uv={"north": [8, 4, 13, 12]})])


def mushroom():
    """The mushroom fan: curb, base, the dark discharge gap, hood, dome and cap."""
    return ([box((0.5, 0, 0.5), (15.5, 3, 15.5), "curb", SIDES + ("up",),
                 uv={f: [0, 0, 16, 3] for f in SIDES}),
             box((2.5, 3, 2.5), (13.5, 5, 13.5), "alum", ALL)]
            + octagon(8, 8, 4.5, 5, 8, "dark")
            + octagon(8, 8, 7.5, 8, 11, "alum", top="alum_flat", bottom="alum_flat")
            + octagon(8, 8, 5, 11, 13, "alum", top="alum_flat")
            + octagon(8, 8, 2.5, 13, 14, "alum", top="alum_flat")
            + [box((11, 3, 0.5), (14.5, 7.5, 1.5), "switch", ALL,
                   uv={"north": [8, 4, 13, 12]})])


def exhaust_grille():
    """A lay-in grille flush under the ceiling, 14 px square."""
    return [box((1, 14.5, 1), (15, 16, 15), "frame", ALL, per={"down": "grille"},
                uv={f: [1, 0, 15, 1.5] for f in SIDES})]


# --------------------------------------------------------------------------------------------
# Models, blockstates, lang
# --------------------------------------------------------------------------------------------

DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625] * 3},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
           "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5] * 3},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                     "scale": [0.375] * 3},
           "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                     "scale": [0.4] * 3},
           "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                    "scale": [0.4] * 3}}
# The lab fan's item shows the master's model (y 0 to 32 over two blocks square), shrunk and
# pulled back to the slot's middle.
LAB_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0],
                       "scale": [0.36] * 3},
               "ground": {"rotation": [0, 0, 0], "translation": [-2, 1, -2],
                          "scale": [0.15] * 3},
               "fixed": {"rotation": [0, 180, 0], "translation": [2, -2.5, 2],
                         "scale": [0.3] * 3},
               "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0, 0],
                                         "scale": [0.2] * 3},
               "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0],
                                         "scale": [0.22] * 3},
               "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -2, 0],
                                        "scale": [0.22] * 3}}
GRILLE_DISPLAY = dict(DISPLAY, gui={"rotation": [-60, 0, 0], "translation": [0, -3, 0],
                                    "scale": [0.625] * 3})


def tex(*names, particle=None):
    t = {}
    for n in names:
        key, _, file = n.partition("=")
        t[key] = TEX_REF % (file or key)
    t["particle"] = TEX_REF % (particle or names[0].partition("=")[2] or names[0])
    return t


MODELS = {
    "lab_fan": (lab_fan_base,
                tex("curb", "white=white_panel", "galv", "louver", "housing", "galv_flat",
                    "motor", particle="galv"), LAB_DISPLAY, False),
    "lab_fan_stack": (lab_fan_stack, tex("stack"), None, True),
    "lab_fan_top": (lab_fan_top, tex("stack", "galv", "dark", "galv_flat",
                                     particle="galv"), None, True),
    "lab_fan_none": (lambda: [], tex("galv"), None, True),
    "upblast": (upblast, tex("curb", "alum", "dark", "alum_flat", "damper", "switch",
                             particle="alum"), DISPLAY, False),
    "mushroom": (mushroom, tex("curb", "alum", "dark", "alum_flat", "switch",
                               particle="alum"), DISPLAY, False),
    "exhaust_grille": (exhaust_grille, tex("frame=white_flat", "grille", particle="grille"),
                       GRILLE_DISPLAY, False),
}

FACINGS = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}


def facing_state(model_name):
    return {"forge_marker": 1, "defaults": {"model": MODEL_REF % model_name},
            "variants": {"facing": FACINGS, "inventory": [{}]}}


def part_state():
    return {"forge_marker": 1, "defaults": {"model": MODEL_REF % "lab_fan_none"},
            "variants": {"facing": FACINGS,
                         "piece": {"none": {"model": MODEL_REF % "lab_fan_none"},
                                   "stack": {"model": MODEL_REF % "lab_fan_stack"},
                                   "top": {"model": MODEL_REF % "lab_fan_top"}},
                         "inventory": [{"model": MODEL_REF % "lab_fan"}]}}


BLOCKS = {
    # registry: (java, blockstate, names en/de/es/sv)
    "hvac_lab_exhaust_fan": (
        'new BlockHvacLabExhaustFan("hvac_lab_exhaust_fan")', facing_state("lab_fan"),
        ("Lab Exhaust Fan (High Plume)", "Laborabluftventilator (Hochstrahl)",
         "Extractor de laboratorio (penacho alto)", "Laboratoriefrånluftsfläkt (högstråle)")),
    "hvac_upblast_exhaust_fan": (
        'new BlockHvacExhaustFan("hvac_upblast_exhaust_fan")', facing_state("upblast"),
        ("Upblast Exhaust Fan", "Dachventilator (senkrecht ausblasend)",
         "Extractor de techo (descarga vertical)", "Takfläkt (uppåtblåsande)")),
    "hvac_mushroom_exhaust_fan": (
        'new BlockHvacExhaustFan("hvac_mushroom_exhaust_fan")', facing_state("mushroom"),
        ("Mushroom Exhaust Fan", "Dachventilator (Pilzhaube)",
         "Extractor de techo (tipo hongo)", "Takfläkt (svamphuv)")),
    "hvac_exhaust_grille": (
        "new BlockHvacExhaustGrille()",
        {"forge_marker": 1, "defaults": {"model": MODEL_REF % "exhaust_grille"},
         "variants": {"normal": [{}], "inventory": [{}]}},
        ("HVAC Exhaust Grille", "HVAC-Abluftgitter", "Rejilla de extracción HVAC",
         "HVAC-frånluftsgaller")),
    "hvac_exhaust_fan_part": (
        "new BlockHvacExhaustFanPart()", part_state(),
        ("Lab Exhaust Fan", "Laborabluftventilator", "Extractor de laboratorio",
         "Laboratoriefrånluftsfläkt")),
}
LOCALES = ("en_us", "de_de", "es_es", "sv_se")
LANG_ANCHOR = "tile.hvac_crac_downflow.name="


def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def js(d):
    return (json.dumps(d, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def lang_file(loc):
    """The lang file with each key set: a key already there keeps its place, new keys go after
    the CRAC units. The file's own line endings are kept."""
    path = os.path.join(LANG_DIR, loc + ".lang")
    raw = open(path, encoding="utf-8", newline="").read()
    eol = "\r\n" if "\r\n" in raw else "\n"
    lines = raw.replace("\r\n", "\n").split("\n")
    i = LOCALES.index(loc)
    anchor = next(n for n, ln in enumerate(lines) if ln.startswith(LANG_ANCHOR))
    for reg, b in BLOCKS.items():
        key = "tile.%s.name" % reg
        line = "%s=%s" % (key, b[2][i])
        at = [n for n, ln in enumerate(lines) if ln.startswith(key + "=")]
        if at:
            lines[at[0]] = line
            anchor = max(anchor, at[0])
            continue
        anchor += 1
        lines.insert(anchor, line)
    return path, eol.join(lines).encode("utf-8")


def outputs():
    out = {os.path.join(TEX_DIR, n + ".png"): png(f()) for n, f in TEXTURES.items()}
    for name, (els, textures, display, no_ao) in MODELS.items():
        m = {"parent": "block/block", "textures": textures, "elements": els()}
        if no_ao or name == "lab_fan":
            # parts of the unit draw past their cell, where vanilla's ambient occlusion would
            # shade them by the wrong blocks' light
            m["ambientocclusion"] = False
        if display:
            m["display"] = display
        out[os.path.join(MODEL_DIR, name + ".json")] = js(m)
    for reg, b in BLOCKS.items():
        out[os.path.join(STATE_DIR, reg + ".json")] = js(b[1])
    for loc in LOCALES:
        path, data = lang_file(loc)
        out[path] = data
    return out


def fragments():
    print("# CsmTabHvac, after the CRAC units")
    for reg, b in BLOCKS.items():
        if reg != "hvac_exhaust_fan_part":
            print("    initTabBlock(%s); // %s" % (b[0], b[2][0]))
    print("# CsmTabHvacHidden")
    print("    initTabBlock(%s);" % BLOCKS["hvac_exhaust_fan_part"][0])


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    a = ap.parse_args()
    if a.fragments:
        fragments()
        return 0
    out = outputs()
    if a.check:
        bad = []
        for p, data in out.items():
            have = open(p, "rb").read() if os.path.exists(p) else None
            if have is not None and p.endswith(".json"):
                have = have.replace(b"\r\n", b"\n")
            if have is not None and p.endswith(".lang"):
                have, data = have.replace(b"\r\n", b"\n"), data.replace(b"\r\n", b"\n")
            if have != data:
                bad.append(os.path.relpath(p, REPO))
        if bad:
            print("gen_hvac_exhaust.py: %d file(s) differ:\n  %s" % (len(bad), "\n  ".join(bad)))
            return 1
        print("gen_hvac_exhaust.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
