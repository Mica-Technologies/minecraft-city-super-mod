#!/usr/bin/env python3
"""Every asset the modern troffers ship (Lighting tab).

    python dev-env-utils/scripts/gen_troffers.py
    python dev-env-utils/scripts/gen_troffers.py --check
    python dev-env-utils/scripts/gen_troffers.py --fragments   # lang and tab lines

Three kinds, each a 2x2 (one block) and a 2x4 (reaching a block north, as the Lithonia GTL does),
all BlockBrightLightFactory lights that sit at the bottom of their cell, so the face lies in the
plane of the ceiling tiles beside them (every Interior Finishes ceiling is a full block):

- **Louvered slot**: a white face with a narrow slot down its length, cross louvers in it.
- **Center basket**: angled white reflectors either side of a raised lens.

(No edge-lit flat panel: the Lithonia GTL already is one. No parabolic louver of its own: the Lithonia 2ESL2 and 2ESL4 already are one, and this script
now writes them too -- their housing, lamps and reflectors as they were, with this family's
unshaded lit lamps and silver louver blades.)
- **Patterned lens**: a white face cut with a pattern of openings (staggered bars, dashes,
  squares, dots) over the lens. The face is one cutout picture, drawn as tall as the fixture and
  cut into a square sprite a block, since a sprite must be square.

Lit and unlit are a texture swap per STATE in the blockstate, as the decorative family does
(LIGHTING_SYSTEM.md): states 1 and 3 get the lit lens, 0 and 2 the unlit one, and the defaults
are unlit so the item and a freshly placed light read off. The lens is drawn unshaded, so a lit
one is not halved by a down face's shading; the unlit lens is drawn at half to match. Every face's uv is where it
sits in its own block, and every element is split at the block line.
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
MODULE = os.path.join(REPO, "modules", "lighting", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "lighting", "troffers")
MODEL_DIR = os.path.join(MODULE, "models", "block", "lighting", "troffers")
STATE_DIR = os.path.join(MODULE, "blockstates")
TEX_REF = "csm:blocks/lighting/troffers/%s"
MODEL_REF = "csm:lighting/troffers/%s"

BODY = (236, 236, 233)
LENS_ON = (255, 254, 246)
# The lens is drawn unshaded ("shade": false), or a down face's directional shading halves it and a
# lit troffer reads grey; the unlit lens is darkened by the same half so it matches the ceiling.
LENS_OFF = (122, 124, 128)


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + (255,)


def swatch(colour, seed, amp=2.0, size=32):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            d = rng.uniform(-amp, amp)
            px[x, y] = clamp([v + d for v in colour])
    return img


def louver_tex():
    """Specular aluminium: v runs from the blade's lower edge (bright) up into the fixture."""
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    for y in range(32):
        k = 1.0 - y / 31.0
        for x in range(32):
            band = 12 * math.sin(x / 32 * math.pi * 2)
            px[x, y] = clamp((150 + 95 * k + band, 154 + 93 * k + band, 160 + 90 * k + band))
    return img


# --------------------------------------------------------------------------------------------
# Patterned faces
# --------------------------------------------------------------------------------------------

PX = 64     # texels a block


def _openings(pattern, w, h, seed):
    """The lit openings of a pattern, as a set of (x, y) texels inside the face's margin."""
    rng = random.Random(seed)
    m = 6
    out = set()
    if pattern == "bars":
        # staggered bars: rows of bars of random length, offset from row to row
        y = m
        while y + 5 <= h - m:
            x = m + rng.randint(0, 8)
            while x < w - m:
                ln = rng.randint(7, 18)
                for xx in range(x, min(x + ln, w - m)):
                    for yy in range(y, y + 5):
                        out.add((xx, yy))
                x += ln + rng.randint(3, 6)
            y += 9
    elif pattern == "dashes":
        y = m + 1
        while y + 3 <= h - m:
            x = m + (0 if (y // 8) % 2 == 0 else 6)
            while x + 8 <= w - m:
                if rng.random() < 0.72:
                    for xx in range(x, x + rng.randint(6, 10)):
                        for yy in range(y, y + 3):
                            if xx < w - m:
                                out.add((xx, yy))
                x += 13
            y += 8
    elif pattern == "squares":
        pitch, size = 10, 6
        for gy in range((h - 2 * m) // pitch):
            for gx in range((w - 2 * m) // pitch):
                if (gx + gy) % 2 == 0:
                    for yy in range(size):
                        for xx in range(size):
                            out.add((m + 2 + gx * pitch + xx, m + 2 + gy * pitch + yy))
    elif pattern == "dots":
        pitch = 8
        for gy in range((h - 2 * m) // pitch):
            off = pitch // 2 if gy % 2 else 0
            for gx in range((w - 2 * m - off) // pitch):
                cx, cy = m + 4 + off + gx * pitch, m + 4 + gy * pitch
                for yy in range(cy - 3, cy + 3):
                    for xx in range(cx - 3, cx + 3):
                        if (xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2 <= 7.5:
                            out.add((xx, yy))
    return out


def pattern_faces(pattern, cells, seed):
    """The whole face, PX wide and PX * cells tall (north at the top), with the openings cut
    clear, cut into one sprite a block, north first. The lens shows through from behind."""
    w, h = PX, PX * cells
    holes = _openings(pattern, w, h, seed)
    rng = random.Random(seed + 7)
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            e = min(x, y, w - 1 - x, h - 1 - y)
            if e < 2:
                px[x, y] = clamp([v - 10 for v in BODY] if e == 0 else [v + 6 for v in BODY])
            elif (x, y) in holes:
                px[x, y] = (0, 0, 0, 0)
            else:
                px[x, y] = clamp([v + rng.uniform(-1.5, 1.5) for v in BODY])
    return [img.crop((0, PX * i, PX, PX * (i + 1))) for i in range(cells)]


# --------------------------------------------------------------------------------------------
# Elements
# --------------------------------------------------------------------------------------------

ALL = ("down", "up", "north", "south", "east", "west")


def box(frm, to, tex, faces=ALL, per=None, rot=None, shade=True):
    """Elements for a box, split at the block line (z 0), each face's uv where it sits in its
    own block. rot is (axis, angle, origin) for a turned element (not split)."""
    out = []
    (x0, y0, z0), (x1, y1, z1) = frm, to
    spans = [(z0, z1)] if (z0 >= 0 or z1 <= 0) else [(z0, 0), (0, z1)]
    for a, b in spans:
        off = 16 if b <= 0 else 0
        la, lb = a + off, b + off
        uv = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
              "east": [16 - lb, 16 - y1, 16 - la, 16 - y0], "west": [la, 16 - y1, lb, 16 - y0],
              "up": [x0, la, x1, lb], "down": [x0, 16 - lb, x1, 16 - la]}
        fs = {}
        for f in faces:
            if (f == "north" and a != z0) or (f == "south" and b != z1):
                continue
            u = [round(min(16, max(0, v)), 4) for v in uv[f]]
            fs[f] = {"texture": "#" + (per or {}).get(f, tex), "uv": u}
        el = {"from": [x0, y0, a], "to": [x1, y1, b], "faces": fs}
        if not shade:
            el["shade"] = False
        if rot:
            axis, angle, origin = rot
            el["rotation"] = {"origin": list(origin), "axis": axis, "angle": angle}
        out.append(el)
    return out


def frame(zmin, zmax, depth=1.0, w=1.0):
    """The door frame round the face: four bars, white, inner faces showing into the cavity."""
    els = []
    els += box((0, 0, zmin), (w, depth, zmax), "body", ("down", "east", "north", "south"))
    els += box((16 - w, 0, zmin), (16, depth, zmax), "body", ("down", "west", "north", "south"))
    els += box((w, 0, zmin), (16 - w, depth, zmin + w), "body", ("down", "south", "north"))
    els += box((w, 0, zmax - w), (16 - w, depth, zmax), "body", ("down", "north", "south"))
    return els


def can(zmin, zmax, y0=1.0, y1=5.0):
    """The housing above the face, closed, so the fixture reads whole from the plenum."""
    return box((0.5, y0, zmin + 0.5), (15.5, y1, zmax - 0.5), "body",
               ("up", "north", "south", "east", "west"))


def slot(zmin, zmax):
    els = []
    els += box((0, 0, zmin), (6, 1, zmax), "body", ("down", "north", "south", "west", "east"))
    els += box((10, 0, zmin), (16, 1, zmax), "body", ("down", "north", "south", "east", "west"))
    els += box((6, 0, zmin), (10, 1, zmin + 1), "body", ("down", "north", "south"))
    els += box((6, 0, zmax - 1), (10, 1, zmax), "body", ("down", "north", "south"))
    els += box((6, 2.5, zmin + 1), (10, 3, zmax - 1), "lens", ("down",), shade=False)
    els += box((5.75, 1, zmin + 1), (6, 2.5, zmax - 1), "louver", ("east",))
    els += box((10, 1, zmin + 1), (10.25, 2.5, zmax - 1), "louver", ("west",))
    length = zmax - zmin - 2
    n = int(length / 1.25)
    for k in range(1, n):
        z = zmin + 1 + length * k / n
        els += box((6, 0.5, z - 0.15), (10, 2.5, z + 0.15), "louver", ("down", "north", "south"))
    return els + can(zmin, zmax, 1, 4)


def basket(zmin, zmax):
    els = frame(zmin, zmax, 1.0, 1.0)
    # the reflectors fall from the frame to the basket, turned 22.5 degrees about the run
    els += box((1, 0.5, zmin + 1), (5.5, 0.75, zmax - 1), "body", ("down",),
               rot=("z", -22.5, (1, 0.5, 0)))
    els += box((10.5, 0.5, zmin + 1), (15, 0.75, zmax - 1), "body", ("down",),
               rot=("z", 22.5, (15, 0.5, 0)))
    els += box((5.5, 0.6, zmin + 1), (10.5, 2.75, zmax - 1), "lens", ("down", "east", "west"),
               shade=False)
    els += box((1, 3, zmin + 1), (15, 3.25, zmax - 1), "body", ("down",))
    return els + can(zmin, zmax, 3.25, 5.5)


def pattern_face(zmin, zmax):
    """The cut face, its sides white, over the lens a quarter pixel behind it."""
    cells = round((zmax - zmin) / 16)
    els = []
    for i in range(cells):
        a = zmin + 16 * i
        els += box((0, 0, a), (16, 1, a + 16), "face%d" % i,
                   ("down", "north", "south", "east", "west"), per={
                       "north": "body", "south": "body", "east": "body", "west": "body"})
    els += box((0.5, 1.25, zmin + 0.5), (15.5, 1.5, zmax - 0.5), "lens", ("down",), shade=False)
    return els + can(zmin, zmax, 1.5, 5)


# The Lithonia 2ESL2 and 2ESL4 (registry l2esl2, l2esl4), rebuilt with this family's lighting: the
# old model's housing, its two lamps and the reflectors over them are kept where they were, the
# lamps now the unshaded lens swapped per state, and its white louvers are replaced by the silver
# mirror blades (the spine down the middle and the cross blades, two cells a block). Their housing
# keeps the troffer atlas's grey quadrant and the old models' item display.
ATLAS = "csm:blocks/lighting/shared_textures/troffers"
ESL_DISPLAY = {
    "thirdperson_righthand": {"translation": [0, 4, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"translation": [0, 4, 0], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"scale": [0.5, 0.5, 0.5]},
    "ground": {"scale": [0.25, 0.25, 0.25]},
    "gui": {"rotation": [-58, -48, -47], "translation": [-1.25, 3.25, 0],
            "scale": [0.4, 0.4, 0.4]},
    "head": {"scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 92, 2], "translation": [2, 0, -2.25],
              "scale": [0.25, 0.25, 0.25]}}
ESL = {"l2esl2": 1, "l2esl4": 2}


def housing(frm, to):
    """An old-model housing piece: every face on the atlas's grey quadrant, as it was."""
    return {"from": list(frm), "to": list(to),
            "faces": {f: {"texture": "#housing", "uv": [8, 0, 16, 8]} for f in ALL}}


def esl(zmin, zmax):
    els = [housing((1, 5, zmin + 1), (15, 6, zmax - 1)),
           housing((0, 0, zmin), (1, 6, zmax)), housing((15, 0, zmin), (16, 6, zmax)),
           housing((1, 0, zmin), (15, 6, zmin + 1)), housing((1, 0, zmax - 1), (15, 6, zmax)),
           housing((2, 6, zmin), (14, 8, zmax)), housing((4, 8, zmin), (12, 10, zmax))]
    for x in (3, 11):
        els += box((x, 2, zmin + 1), (x + 2, 4, zmax - 1), "lens", shade=False)
    for x in (2, 10):
        els += box((x, 4, zmin + 1), (x + 4, 5, zmax - 1), "body", ("down", "east", "west"))
    els += box((7.5, 0, zmin + 1), (8.5, 2.5, zmax - 1), "louver", ("down", "east", "west"))
    n = round((zmax - zmin) / 16 * 3)
    for k in range(1, n):
        z = zmin + (zmax - zmin) * k / n
        els += box((1, 0, z - 0.25), (15, 2.5, z + 0.25), "louver", ("down", "north", "south"))
    return els


KINDS = {
    "slot": (slot, (
        "Louvered Slot Troffer", "Luminaria Empotrada de Ranura con Rejilla",
        "Schlitz-Einbauleuchte mit Raster", "Spaltarmatur med Raster")),
    "basket": (basket, (
        "Center Basket Troffer", "Luminaria Empotrada de Cesta Central",
        "Einbauleuchte mit Mittelkorb", "Armatur med Mittkorg")),
}

PATTERNS = {
    "bars": (4401, ("Staggered Bars", "Barras Escalonadas", "Versetzte Balken",
                    "Förskjutna Balkar")),
    "dashes": (4402, ("Dashes", "Guiones", "Striche", "Streck")),
    "squares": (4403, ("Squares", "Cuadrados", "Quadrate", "Kvadrater")),
    "dots": (4404, ("Dots", "Puntos", "Punkte", "Prickar")),
}
PATTERN_NAMES = ("Patterned Lens Troffer", "Luminaria Empotrada con Difusor Decorativo",
                 "Einbauleuchte mit Musterblende", "Armatur med Mönstrad Lins")

SIZES = {"2x2": 1, "2x4": 2}


def catalogue():
    """registry -> (kind, pattern or None, cells, names). Order is creative order."""
    out = {}
    for kind, (_, names) in KINDS.items():
        for size, cells in SIZES.items():
            out["troffer_%s_%s" % (kind, size)] = (
                kind, None, cells, tuple("%s (%s)" % (n, size) for n in names))
    for pat, (_, pnames) in PATTERNS.items():
        for size, cells in SIZES.items():
            out["troffer_pattern_%s_%s" % (pat, size)] = (
                "pattern", pat, cells,
                tuple("%s (%s, %s)" % (n, p, size) for n, p in zip(PATTERN_NAMES, pnames)))
    return out


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------

def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def js(d):
    return (json.dumps(d, indent=2) + "\n").encode()


def outputs():
    out = {}
    tex = {"body": swatch(BODY, 4411), "lens_on": swatch(LENS_ON, 4412, 1.0),
           "lens_off": swatch(LENS_OFF, 4413), "louver": louver_tex()}
    for name, img in tex.items():
        out[os.path.join(TEX_DIR, "troffer_%s.png" % name)] = png(img)
    for reg, (kind, pat, cells, _) in catalogue().items():
        zmin, zmax = 16 - 16 * cells, 16
        t = {"body": TEX_REF % "troffer_body", "particle": TEX_REF % "troffer_body"}
        on, off = {}, {}
        on["lens"], off["lens"] = TEX_REF % "troffer_lens_on", TEX_REF % "troffer_lens_off"
        if kind == "pattern":
            seed = PATTERNS[pat][0] * 10 + cells
            for i, img in enumerate(pattern_faces(pat, cells, seed)):
                tn = "%s_%d" % (reg, i)
                out[os.path.join(TEX_DIR, tn + ".png")] = png(img)
                t["face%d" % i] = TEX_REF % tn
            els = pattern_face(zmin, zmax)
        else:
            t["louver"] = TEX_REF % "troffer_louver"
            els = KINDS[kind][0](zmin, zmax)
        t.update(off)
        model = {"parent": "block/block", "ambientocclusion": False, "textures": t,
                 "elements": els,
                 "display": {"gui": {"rotation": [-30, 225, 0], "translation": [0, 0, 0],
                                     "scale": [0.55 if cells == 2 else 0.625] * 3}}}
        out[os.path.join(MODEL_DIR, reg + ".json")] = js(model)
        state = {"forge_marker": 1,
                 "defaults": {"model": MODEL_REF % reg, "textures": dict(off)},
                 "variants": {
                     "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                                "west": {"y": 270}},
                     # 0 and 2 name the unlit lens rather than inherit it, so
                     # gen_lighting_lit_atlases.py (whose fixtures leave them empty) never takes
                     # these for its own
                     "state": {"0": {"textures": dict(off)}, "1": {"textures": dict(on)},
                               "2": {"textures": dict(off)}, "3": {"textures": dict(on)}},
                     "inventory": [{}]}}
        out[os.path.join(STATE_DIR, reg + ".json")] = js(state)
    for reg, cells in ESL.items():
        zmin = 16 - 16 * cells
        model_name = "lithonia_2esl%d" % (2 * cells)
        t = {"housing": ATLAS, "body": TEX_REF % "troffer_body",
             "louver": TEX_REF % "troffer_louver", "particle": ATLAS,
             "lens": TEX_REF % "troffer_lens_off"}
        on = {"lens": TEX_REF % "troffer_lens_on"}
        off = {"lens": TEX_REF % "troffer_lens_off"}
        out[os.path.join(MODEL_DIR, model_name + ".json")] = js(
            {"parent": "block/block", "ambientocclusion": False, "textures": t,
             "elements": esl(zmin, 16), "display": ESL_DISPLAY})
        out[os.path.join(STATE_DIR, reg + ".json")] = js(
            {"forge_marker": 1,
             "defaults": {"model": MODEL_REF % model_name, "textures": dict(off)},
             "variants": {
                 "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                            "west": {"y": 270}},
                 "state": {"0": {"textures": dict(off)}, "1": {"textures": dict(on)},
                           "2": {"textures": dict(off)}, "3": {"textures": dict(on)}},
                 "inventory": [{}]}})
    return out


def java_line(reg, cells, name):
    z0 = "-1.000000" if cells == 2 else "0.000000"
    return ('    initTabBlock(new BlockBrightLightFactory("%s", new AxisAlignedBB(0.000000, '
            '0.000000, %s, 1.000000, 0.312500, 1.000000), 0)); // %s' % (reg, z0, name))


def fragments():
    cat = catalogue()
    for i, lang in enumerate(("en_us", "es_es", "de_de", "sv_se")):
        print("# %s.lang" % lang)
        for reg, (_, _, _, names) in cat.items():
            print("tile.%s.name=%s" % (reg, names[i]))
    print("# CsmTabLighting")
    for reg, (_, _, cells, names) in cat.items():
        print(java_line(reg, cells, names[0]))


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
            # git may have checked a text file out with CRLF endings; compare it as LF
            have = open(p, "rb").read() if os.path.exists(p) else None
            if have is not None and p.endswith(".json"):
                have = have.replace(b"\r\n", b"\n")
            if have != data:
                bad.append(os.path.relpath(p, REPO))
        if bad:
            print("gen_troffers.py: %d file(s) differ:\n  %s" % (len(bad), "\n  ".join(bad)))
            return 1
        print("gen_troffers.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
