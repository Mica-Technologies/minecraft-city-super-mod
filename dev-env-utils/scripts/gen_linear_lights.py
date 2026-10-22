#!/usr/bin/env python3
"""Every asset the linear LED lights ship (Lighting tab), and the shared finish palette.

    python dev-env-utils/scripts/gen_linear_lights.py
    python dev-env-utils/scripts/gen_linear_lights.py --check
    python dev-env-utils/scripts/gen_linear_lights.py --fragments   # lang and tab lines

A linear light (BlockLinearLight) is a slim 3 px housing, one block a segment, joined into one
fixture along a run, flush against the ceiling (y 13..16) or hung lower in its block (y 10..13) on
aircraft cables. Two lenses: opal, a continuous lit strip, and louvered, white cross blades in
front of the lit lens. Ten finishes, the palette the hanging acoustic panels share so lights and
panels match: black, silver, white; light grey, charcoal; navy, sage, terracotta; light oak and
walnut. The palette's textures live in Core (`blocks/finishes/`), since Lighting and Building
both draw with them.

The blockstate is vanilla multipart, every part drawn for a run along z (facing north) and turned
by the facing: the body, an end cap where the run stops (front, or back turned half round), the
lens lit or unlit by STATE, and the cable where the segment is hung and carries one. Geometry
lives in a few parent models; a finish is a child naming its housing texture. The lens is drawn
unshaded, as the troffers' is, and uses their lens textures.
"""

import argparse
import io
import json
import os
import random
import sys

from PIL import Image

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CORE = os.path.join(REPO, "src", "main", "resources", "assets", "csm")
LIGHT = os.path.join(REPO, "modules", "lighting", "src", "main", "resources", "assets", "csm")
FINISH_DIR = os.path.join(CORE, "textures", "blocks", "finishes")
MODEL_DIR = os.path.join(LIGHT, "models", "block", "lighting", "linear")
ITEM_DIR = os.path.join(LIGHT, "models", "item")
STATE_DIR = os.path.join(LIGHT, "blockstates")
FINISH_REF = "csm:blocks/finishes/finish_%s"
CABLE_REF = "csm:blocks/finishes/aircraft_cable"
MODEL_REF = "csm:lighting/linear/%s"
LENS_ON = "csm:blocks/lighting/troffers/troffer_lens_on"
LENS_OFF = "csm:blocks/lighting/troffers/troffer_lens_off"
BLADE = "csm:blocks/lighting/troffers/troffer_body"

# name -> (kind, colour, names en/es/de/sv). Order is creative order.
FINISHES = {
    "black": ("metal", (30, 31, 34), ("Black", "Negro", "Schwarz", "Svart")),
    "silver": ("metal", (172, 176, 182), ("Silver", "Plata", "Silber", "Silver")),
    "white": ("metal", (234, 234, 232), ("White", "Blanco", "Weiß", "Vit")),
    "light_grey": ("felt", (168, 170, 172), ("Light Grey", "Gris Claro", "Hellgrau", "Ljusgrå")),
    "charcoal": ("felt", (62, 64, 68), ("Charcoal", "Carbón", "Anthrazit", "Koksgrå")),
    "navy": ("felt", (38, 52, 86), ("Navy", "Azul Marino", "Marineblau", "Marinblå")),
    "sage": ("felt", (134, 152, 124), ("Sage", "Salvia", "Salbeigrün", "Salviagrön")),
    "terracotta": ("felt", (172, 96, 70), ("Terracotta", "Terracota", "Terrakotta",
                                           "Terrakotta")),
    "oak": ("wood", (196, 158, 108), ("Light Oak", "Roble Claro", "Helle Eiche", "Ljus Ek")),
    "walnut": ("wood", (104, 72, 50), ("Walnut", "Nogal", "Nussbaum", "Valnöt")),
}

LENSES = {
    "opal": ("linear_light_%s", ("Linear LED Light (%s)", "Luminaria Lineal LED (%s)",
                                 "Lineare LED-Leuchte (%s)", "Linjär LED-armatur (%s)")),
    "louver": ("linear_light_louver_%s", (
        "Louvered Linear LED Light (%s)", "Luminaria Lineal LED con Rejilla (%s)",
        "Lineare LED-Leuchte mit Raster (%s)", "Linjär LED-armatur med Raster (%s)")),
}
HANGER = "linear_light_hanger"
HANGER_NAMES = ("Linear Light Cable Hanger", "Colgador de Cable para Luminaria Lineal",
                "Seilabhängung für Lineare Leuchten", "Vajerupphängning för Linjär Armatur")

HEIGHTS = {"s": 13.0, "h": 10.0}   # surface and hung: the housing's underside


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + (255,)


def finish_texture(kind, colour, seed):
    """32 px: a powder coat's faint grain, a felt's fuzz, or wood grain running along u."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    if kind == "wood":
        rows = [rng.uniform(-10, 10) for _ in range(32)]
        for y in range(32):
            for x in range(32):
                px[x, y] = clamp([v + rows[y] + rng.uniform(-3, 3) for v in colour])
    else:
        amp = 3 if kind == "metal" else 9
        for y in range(32):
            for x in range(32):
                px[x, y] = clamp([v + rng.uniform(-amp, amp) for v in colour])
    return img


def cable_texture():
    rng = random.Random(4520)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            # a twisted strand: a lit diagonal every 4 texels
            d = 18 if (x + y) % 4 == 0 else rng.uniform(-4, 4)
            px[x, y] = clamp((118 + d, 120 + d, 124 + d))
    return img


# --------------------------------------------------------------------------------------------
# Geometry (facing north, the run along z)
# --------------------------------------------------------------------------------------------

def box(frm, to, tex, faces, shade=True):
    (x0, y0, z0), (x1, y1, z1) = frm, to
    uv = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
          "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
          "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
    el = {"from": [x0, y0, z0], "to": [x1, y1, z1],
          "faces": {f: {"texture": "#" + tex, "uv": [round(v, 4) for v in uv[f]]}
                    for f in faces}}
    if not shade:
        el["shade"] = False
    return el


def body(y0):
    return [box((6.5, y0 + 0.5, 0), (9.5, y0 + 3, 16), "housing", ("east", "west", "up")),
            box((6.5, y0, 0), (7.25, y0 + 0.5, 16), "housing", ("down", "east", "west")),
            box((8.75, y0, 0), (9.5, y0 + 0.5, 16), "housing", ("down", "east", "west"))]


def cap(y0):
    """The end cap where the run stops: a plate across the housing's north end."""
    return [box((6.5, y0, 0), (9.5, y0 + 3, 0.25), "housing",
                ("north", "south", "down", "up", "east", "west"))]


def lens(kind, y0, lit=False):
    """The lens. A louvered one's white blades catch the lamp's light: in the lit model they are
    drawn unshaded too, as the lens is, or they stay a shaded grey in front of a lit lens."""
    if kind == "opal":
        return [box((7.25, y0 + 0.25, 0), (8.75, y0 + 0.5, 16), "lens", ("down",),
                    shade=False)]
    els = [box((7.25, y0 + 1.5, 0), (8.75, y0 + 1.75, 16), "lens", ("down",), shade=False),
           box((7.25, y0, 0), (7.4, y0 + 1.5, 16), "blade", ("east",), shade=not lit),
           box((8.6, y0, 0), (8.75, y0 + 1.5, 16), "blade", ("west",), shade=not lit)]
    for k in range(16):
        z = k + 0.5
        els.append(box((7.4, y0 + 0.25, z - 0.15), (8.6, y0 + 1.5, z + 0.15), "blade",
                       ("down", "north", "south"), shade=not lit))
    return els


def cable_els(y_bottom, y_top):
    return [box((7.9, y_bottom, 7.9), (8.1, y_top, 8.1), "cable",
                ("north", "south", "east", "west"))]


def model(textures, elements, display=None):
    m = {"parent": "block/block", "ambientocclusion": False, "textures": textures,
         "elements": elements}
    if display:
        m["display"] = display
    return m


DISPLAY = {
    "gui": {"rotation": [30, 45, 0], "translation": [0, -4.5, 0], "scale": [0.85] * 3},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.3] * 3},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -3, 0], "scale": [0.6] * 3},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, -1, 0],
                              "scale": [0.375] * 3},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0],
                              "scale": [0.4] * 3},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -2, 0],
                             "scale": [0.4] * 3},
}


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------

def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def js(d):
    return (json.dumps(d, indent=2) + "\n").encode()


def registries():
    """registry -> (lens, finish, names), in creative order."""
    out = {}
    for lens_kind, (pattern, names) in LENSES.items():
        for fin, (_, _, fnames) in FINISHES.items():
            out[pattern % fin] = (lens_kind, fin, tuple(n % f for n, f in zip(names, fnames)))
    return out


def blockstate(reg, lens_kind, fin):
    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for h in ("s", "h"):
            hung = "true" if h == "h" else "false"

            def apply(name, turn=0):
                a = {"model": MODEL_REF % name}
                if (y + turn) % 360:
                    a["y"] = (y + turn) % 360
                return a
            when = {"facing": facing, "hung": hung}
            parts.append({"when": dict(when), "apply": apply("body_%s_%s" % (fin, h))})
            parts.append({"when": dict(when, front="false"), "apply": apply(
                "cap_%s_%s" % (fin, h))})
            parts.append({"when": dict(when, back="false"), "apply": apply(
                "cap_%s_%s" % (fin, h), 180)})
            for st, lit in (("1|3", "on"), ("0|2", "off")):
                parts.append({"when": dict(when, state=st), "apply": apply(
                    "lens_%s_%s_%s" % (lens_kind, lit, h))})
    parts.append({"when": {"hung": "true", "cable": "true"},
                  "apply": {"model": MODEL_REF % "cable"}})
    return {"multipart": parts}


def outputs():
    out = {}
    for i, (fin, (kind, colour, _)) in enumerate(FINISHES.items()):
        out[os.path.join(FINISH_DIR, "finish_%s.png" % fin)] = png(
            finish_texture(kind, colour, 4500 + i))
    # the aircraft cable is Core's too: the hanging acoustic panels hang on it
    out[os.path.join(FINISH_DIR, "aircraft_cable.png")] = png(cable_texture())

    # parents: geometry only
    for h, y0 in HEIGHTS.items():
        out[os.path.join(MODEL_DIR, "geo_body_%s.json" % h)] = js(model({}, body(y0)))
        out[os.path.join(MODEL_DIR, "geo_cap_%s.json" % h)] = js(model({}, cap(y0)))
        for lk in LENSES:
            for lit, tex in (("on", LENS_ON), ("off", LENS_OFF)):
                out[os.path.join(MODEL_DIR, "lens_%s_%s_%s.json" % (lk, lit, h))] = js(model(
                    {"lens": tex, "blade": BLADE, "particle": BLADE},
                    lens(lk, y0, lit == "on")))
    cable_t = {"cable": CABLE_REF, "particle": CABLE_REF}
    out[os.path.join(MODEL_DIR, "cable.json")] = js(model(cable_t, cable_els(13, 16)))
    out[os.path.join(MODEL_DIR, "hanger.json")] = js(model(cable_t, cable_els(0, 16)))
    for lk in LENSES:
        y0 = HEIGHTS["s"]
        north_cap = cap(y0)
        south_cap = [box((6.5, y0, 15.75), (9.5, y0 + 3, 16), "housing",
                         ("north", "south", "down", "up", "east", "west"))]
        out[os.path.join(MODEL_DIR, "geo_item_%s.json" % lk)] = js(model(
            {"lens": LENS_OFF, "blade": BLADE},
            body(y0) + north_cap + south_cap + lens(lk, y0), DISPLAY))

    # a finish: children naming its housing
    for fin in FINISHES:
        t = {"housing": FINISH_REF % fin, "particle": FINISH_REF % fin}
        for h in HEIGHTS:
            for part in ("body", "cap"):
                out[os.path.join(MODEL_DIR, "%s_%s_%s.json" % (part, fin, h))] = js(
                    {"parent": "csm:block/lighting/linear/geo_%s_%s" % (part, h), "textures": t})

    for reg, (lk, fin, _) in registries().items():
        out[os.path.join(STATE_DIR, reg + ".json")] = js(blockstate(reg, lk, fin))
        out[os.path.join(ITEM_DIR, reg + ".json")] = js(
            {"parent": "csm:block/lighting/linear/geo_item_%s" % lk,
             "textures": {"housing": FINISH_REF % fin, "particle": FINISH_REF % fin}})
    out[os.path.join(STATE_DIR, HANGER + ".json")] = js(
        {"forge_marker": 1, "defaults": {"model": MODEL_REF % "hanger"},
         "variants": {"normal": [{}], "inventory": [{}]}})
    return out


def fragments():
    regs = registries()
    for i, lang in enumerate(("en_us", "es_es", "de_de", "sv_se")):
        print("# %s.lang" % lang)
        for reg, (_, _, names) in regs.items():
            print("tile.%s.name=%s" % (reg, names[i]))
        print("tile.%s.name=%s" % (HANGER, HANGER_NAMES[i]))
    print("# CsmTabLighting")
    for reg, (_, _, names) in regs.items():
        print('    initTabBlock(new BlockLinearLight("%s")); // %s' % (reg, names[0]))
    print("    initTabBlock(new BlockLinearLightHanger()); // %s" % HANGER_NAMES[0])


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
            print("gen_linear_lights.py: %d file(s) differ:\n  %s" % (len(bad),
                                                                     "\n  ".join(bad)))
            return 1
        print("gen_linear_lights.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
