#!/usr/bin/env python3
"""Every asset the exposed ducts and pipes ship (HVAC tab).

    python dev-env-utils/scripts/gen_exposed_services.py
    python dev-env-utils/scripts/gen_exposed_services.py --check
    python dev-env-utils/scripts/gen_exposed_services.py --fragments   # lang and tab lines

Decorative ductwork and pipe for an open ceiling (BlockExposedDuct): round spiral duct 12 px
across, rectangular duct 12 x 8, and painted pipe 4 px across. The round ones are exact octagons
-- four rectangles, two turned 45 degrees about the run -- since a square and the same square
turned 45 is a star.

The blockstate is vanilla multipart, every part drawn for a run along z (north) and turned:

- an arm from the middle out to each face the segment reaches (`north` .. `down`), turned by y
  for the level sides and by x for up and down;
- the middle: a length along the one axis all its arms share (the conditions say which), or, where
  the arms turn, a fitting a little larger than the duct;
- where the segment is level under a solid ceiling or a threaded rod hanger (`rod`), a threaded
  rod up to it, and a strap round the duct when the run is straight. The hanger
  (BlockExposedServiceHanger) stacks under a ceiling to hang a run lower.
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
TEX_DIR = os.path.join(HVAC, "textures", "blocks", "hvac", "exposed")
MODEL_DIR = os.path.join(HVAC, "models", "block", "hvac", "exposed")
ITEM_DIR = os.path.join(HVAC, "models", "item")
STATE_DIR = os.path.join(HVAC, "blockstates")
TEX_REF = "csm:blocks/hvac/exposed/%s"
MODEL_REF = "csm:hvac/exposed/%s"
PARENT_REF = "csm:block/hvac/exposed/%s"

COLOURS = {
    "galvanized": ((176, 180, 184), ("Galvanized", "Galvanizado", "Verzinkt", "Galvaniserad")),
    "black": ((40, 41, 44), ("Black", "Negro", "Schwarz", "Svart")),
    "white": ((230, 230, 228), ("White", "Blanco", "Weiß", "Vit")),
    "charcoal": ((70, 72, 76), ("Charcoal", "Carbón", "Anthrazit", "Koksgrå")),
    "red": ((176, 40, 36), ("Red", "Rojo", "Rot", "Röd")),
    "silver": ((160, 164, 168), ("Silver", "Plata", "Silber", "Silver")),
}

# kind -> (half width, half height, round, colours, names en/es/de/sv)
KINDS = {
    "duct_round": (6, 6, True, ("galvanized", "black", "white", "charcoal"), (
        "Exposed Spiral Duct (%s)", "Conducto Espiral Visto (%s)",
        "Sichtbares Wickelfalzrohr (%s)", "Synlig Spiralkanal (%s)")),
    "duct_rect": (6, 4, False, ("galvanized", "black"), (
        "Exposed Rectangular Duct (%s)", "Conducto Rectangular Visto (%s)",
        "Sichtbarer Rechteckkanal (%s)", "Synlig Rektangulär Kanal (%s)")),
    "pipe": (2, 2, True, ("black", "white", "red", "silver"), (
        "Exposed Pipe (%s)", "Tubería Vista (%s)", "Sichtbares Rohr (%s)", "Synligt Rör (%s)")),
}


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + (255,)


def texture(kind, colour, seed):
    """16 px. The round duct's spiral lock seam runs diagonally; the rectangular duct's joints
    are on the block lines, drawn on every edge of the sprite so whichever face shows only the
    joints that lie on a block line; pipe is plain paint with a faint sheen."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = rng.uniform(-1.5, 1.5)
            if kind == "duct_round":
                k = (x + y) % 8
                d += -26 if k == 0 else 14 if k == 1 else 0
            elif kind == "duct_rect":
                if x in (0, 15) or y in (0, 15):
                    d -= 24
            else:
                d += 10 if y in (6, 7) else 0
            px[x, y] = clamp([v + d for v in colour])
    return img


def steel_texture():
    rng = random.Random(4630)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = rng.uniform(-4, 4) + (12 if (x + y) % 3 == 0 else 0)
            px[x, y] = clamp((96 + d, 98 + d, 102 + d))
    return img


# --------------------------------------------------------------------------------------------
# Geometry: a run along z, centred on (8, 8)
# --------------------------------------------------------------------------------------------

def el(frm, to, tex, faces, rot=None):
    (x0, y0, z0), (x1, y1, z1) = frm, to
    uv = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
          "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
          "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
    e = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to],
         "faces": {f: {"texture": "#" + tex,
                       "uv": [round(min(16, max(0, v)), 4) for v in uv[f]]} for f in faces}}
    if rot is not None:
        e["rotation"] = {"origin": [8, 8, 8], "axis": "z", "angle": rot}
    return e


def section(hw, hh, rnd, z0, z1, tex, ends=()):
    """A length of the cross-section from z0 to z1: an exact octagon of four rectangles, or the
    plain rectangle. ``ends`` adds the north and/or south end faces."""
    sides = ("east", "west", "up", "down") + tuple(ends)
    if not rnd:
        return [el((8 - hw, 8 - hh, z0), (8 + hw, 8 + hh, z1), tex, sides)]
    s = hw * math.tan(math.radians(22.5))
    out = []
    for rot in (None, 45):
        out.append(el((8 - hw, 8 - s, z0), (8 + hw, 8 + s, z1), tex, sides, rot))
        out.append(el((8 - s, 8 - hw, z0), (8 + s, 8 + hw, z1), tex, sides, rot))
    return out


def arm(hw, hh, rnd):
    reach = 8 - hw
    return section(hw, hh, rnd, 0, reach, "pipe", ("north",))


def middle(hw, hh, rnd):
    return section(hw, hh, rnd, 8 - hw, 8 + hw, "pipe")


def joint(hw, hh):
    """The fitting where a run turns: a box a little larger than the section."""
    g = 0.5 if hw > 2 else 0.4
    return [el((8 - hw - g, 8 - hh - g, 8 - hw - g), (8 + hw + g, 8 + hh + g, 8 + hw + g),
               "pipe", ("north", "south", "east", "west", "up", "down"))]


def rod(hh):
    return [el((7.6, 8 + hh, 7.6), (8.4, 16, 8.4), "steel", ("north", "south", "east", "west"))]


def strap(hw, hh, rnd):
    """A band round the duct at the rod, a quarter pixel proud of it."""
    t = 0.25
    if rnd:
        return section(hw + t, hw + t, True, 7.5, 8.5, "steel")
    return [el((8 - hw - t, 8 - hh - t, 7.5), (8 + hw + t, 8 + hh + t, 8.5), "steel",
               ("east", "west", "up", "down"))]


def model(textures, elements, display=None):
    m = {"parent": "block/block", "textures": textures, "elements": elements}
    if display:
        m["display"] = display
    return m


DISPLAY = {
    "gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.625] * 3},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5] * 3},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                              "scale": [0.375] * 3},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                              "scale": [0.4] * 3},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                             "scale": [0.4] * 3},
}

SIDES = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
         "up": {"x": 270}, "down": {"x": 90}}
AXES = {"z": ("north", "south"), "x": ("east", "west"), "y": ("up", "down")}


def blockstate(kind, colour):
    def m(name, **rot):
        a = {"model": MODEL_REF % ("%s_%s_%s" % (kind, colour, name))}
        a.update(rot)
        return a
    parts = []
    for side, rot in SIDES.items():
        if kind == "duct_rect" and side in ("up", "down"):
            continue
        parts.append({"when": {side: "true"}, "apply": m("arm", **rot)})
    # the middle along an axis when no arm leaves it, a fitting where arms span two axes
    for axis, rot in (("z", {}), ("x", {"y": 90}), ("y", {"x": 90})):
        if kind == "duct_rect" and axis == "y":
            continue
        when = {s: "false" for a, ss in AXES.items() if a != axis for s in ss}
        parts.append({"when": when, "apply": m("middle", **rot)})
    pairs = []
    for a, b in (("z", "x"), ("z", "y"), ("x", "y")):
        for sa in AXES[a]:
            for sb in AXES[b]:
                pairs.append({sa: "true", sb: "true"})
    parts.append({"when": {"OR": pairs}, "apply": m("joint")})
    parts.append({"when": {"rod": "true"}, "apply": m("rod")})
    parts.append({"when": {"rod": "true", "east": "false", "west": "false"},
                  "apply": m("strap")})
    parts.append({"when": {"rod": "true", "north": "false", "south": "false"},
                  "apply": m("strap", y=90)})
    return {"multipart": parts}


def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def js(d):
    return (json.dumps(d, indent=2) + "\n").encode()


HANGER = "exposed_service_hanger"
HANGER_NAMES = ("Threaded Rod Hanger", "Colgador de Varilla Roscada", "Gewindestangen-Abhängung",
                "Gängstångshängare")


def registries():
    out = {}
    for kind, (_, _, _, colours, names) in KINDS.items():
        for c in colours:
            cn = COLOURS[c][1]
            out["%s_%s" % (kind, c)] = (kind, c, tuple(n % x for n, x in zip(names, cn)))
    return out


def outputs():
    out = {os.path.join(TEX_DIR, "hanger_steel.png"): png(steel_texture())}
    for i, (kind, (hw, hh, rnd, colours, _)) in enumerate(KINDS.items()):
        geo = {"arm": arm(hw, hh, rnd), "middle": middle(hw, hh, rnd), "joint": joint(hw, hh),
               "rod": rod(hh), "strap": strap(hw, hh, rnd)}
        for name, els in geo.items():
            out[os.path.join(MODEL_DIR, "geo_%s_%s.json" % (kind, name))] = js(model({}, els))
        item = section(hw, hh, rnd, 0, 16, "pipe", ("north", "south"))
        out[os.path.join(MODEL_DIR, "geo_%s_item.json" % kind)] = js(model({}, item, DISPLAY))
        for j, c in enumerate(colours):
            tn = "%s_%s" % (kind, c)
            out[os.path.join(TEX_DIR, tn + ".png")] = png(
                texture(kind, COLOURS[c][0], 4600 + i * 10 + j))
            t = {"pipe": TEX_REF % tn, "steel": TEX_REF % "hanger_steel",
                 "particle": TEX_REF % tn}
            for name in geo:
                out[os.path.join(MODEL_DIR, "%s_%s.json" % (tn, name))] = js(
                    {"parent": PARENT_REF % ("geo_%s_%s" % (kind, name)), "textures": t})
            out[os.path.join(STATE_DIR, tn + ".json")] = js(blockstate(kind, c))
            out[os.path.join(ITEM_DIR, tn + ".json")] = js(
                {"parent": PARENT_REF % ("geo_%s_item" % kind), "textures": t})
    # the threaded rod hanger: the rod the full height of its block, and a thicker one for the
    # item, or a 0.8 px rod is lost in a slot
    steel = {"steel": TEX_REF % "hanger_steel", "particle": TEX_REF % "hanger_steel"}
    out[os.path.join(MODEL_DIR, "hanger.json")] = js(model(steel, [
        el((7.6, 0, 7.6), (8.4, 16, 8.4), "steel", ("north", "south", "east", "west"))]))
    out[os.path.join(MODEL_DIR, "hanger_item.json")] = js(model(steel, [
        el((7.25, 0, 7.25), (8.75, 16, 8.75), "steel",
           ("north", "south", "east", "west", "up", "down"))], DISPLAY))
    out[os.path.join(STATE_DIR, HANGER + ".json")] = js(
        {"forge_marker": 1, "defaults": {"model": MODEL_REF % "hanger"},
         "variants": {"normal": [{}], "inventory": [{"model": MODEL_REF % "hanger_item"}]}})
    return out


def fragments():
    regs = registries()
    for i, lang in enumerate(("en_us", "es_es", "de_de", "sv_se")):
        print("# %s.lang" % lang)
        for reg, (_, _, names) in regs.items():
            print("tile.%s.name=%s" % (reg, names[i]))
        print("tile.%s.name=%s" % (HANGER, HANGER_NAMES[i]))
    print("# CsmTabHvac")
    for reg, (_, _, names) in regs.items():
        print('    initTabBlock(new BlockExposedDuct("%s")); // %s' % (reg, names[0]))
    print("    initTabBlock(new BlockExposedServiceHanger()); // %s" % HANGER_NAMES[0])


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
            print("gen_exposed_services.py: %d file(s) differ:\n  %s" % (len(bad),
                                                                        "\n  ".join(bad)))
            return 1
        print("gen_exposed_services.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
