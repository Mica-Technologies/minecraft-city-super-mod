#!/usr/bin/env python3
"""Every asset the HVAC tab's data hall pieces and zone boundary ship (#305).

    python dev-env-utils/scripts/gen_hvac_data_hall.py
    python dev-env-utils/scripts/gen_hvac_data_hall.py --check
    python dev-env-utils/scripts/gen_hvac_data_hall.py --fragments   # tab lines

- **CRAC and CRAH units** (BlockHvacCrac, TileEntityHvacCrac): a floor-standing cabinet 30 px
  tall drawn from one block, its control panel and filter doors on the front. The downflow unit
  takes its return air through a grille on top; the upflow unit carries a discharge plenum on
  top with a grille on its front; the chilled-water CRAH is the downflow cabinet in graphite.
- **HVAC Zone Boundary** (BlockHvacZoneBoundary): a slim strip flush under the ceiling, running
  north and south, in a grey with blue dashes so a builder can see where the zones meet.

Every model faces north (the front toward -z); Forge blockstates turn it. The lang lines are
written into the module's four lang files by key.
"""

import argparse
import io
import json
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import model_depth  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HVAC = os.path.join(REPO, "modules", "hvac", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(HVAC, "textures", "blocks", "hvac", "datahall")
MODEL_DIR = os.path.join(HVAC, "models", "block", "hvac", "datahall")
STATE_DIR = os.path.join(HVAC, "blockstates")
LANG_DIR = os.path.join(HVAC, "lang")
TEX_REF = "csm:blocks/hvac/datahall/%s"
MODEL_REF = "csm:hvac/datahall/%s"

WHITE = (226, 228, 224)
GRAPHITE = (70, 74, 80)
DARK = (30, 32, 36)
BLUE = (70, 130, 220)


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + (255,)


def shade(c, k):
    return tuple(v * k for v in c[:3])


def grain(colour, seed, size=32, amp=3):
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


def panel(colour, seed):
    """A side panel: the finish with a seam round its edge."""
    img = grain(colour, seed)
    rect(img, 0, 0, 32, 1, shade(colour, 0.8))
    rect(img, 0, 31, 32, 32, shade(colour, 0.7))
    rect(img, 0, 0, 1, 32, shade(colour, 0.85))
    rect(img, 31, 0, 32, 32, shade(colour, 0.75))
    return img


def front_upper(colour, seed):
    """The upper front: the controller's screen with its status lights, over the door seam."""
    img = panel(colour, seed)
    rect(img, 9, 6, 23, 15, DARK)
    for i, y in enumerate((8, 10, 12)):
        rect(img, 11, y, 11 + (9, 6, 8)[i], y + 1, (90, 200, 120))
    rect(img, 11, 18, 13, 20, (60, 200, 90))
    rect(img, 15, 18, 17, 20, BLUE)
    rect(img, 1, 26, 31, 27, shade(colour, 0.7))
    return img


def front_lower(colour, seed):
    """The lower front: two filter doors with louvres and a handle each."""
    img = panel(colour, seed)
    rect(img, 15, 1, 17, 31, shade(colour, 0.7))
    for x0 in (3, 19):
        for y in range(4, 26, 3):
            rect(img, x0, y, x0 + 10, y + 1, shade(colour, 0.55))
        rect(img, x0 + 4, 28, x0 + 6, 30, DARK)
    return img


def grille(colour, seed):
    """A return or discharge grille: dark slots across a frame."""
    img = grain(shade(colour, 0.9), seed)
    for y in range(3, 29, 3):
        rect(img, 3, y, 29, y + 2, DARK)
    return img


def plinth():
    return grain((40, 42, 46), 9301, 16, 2)


def boundary():
    """The zone boundary strip: grey, a blue dash every half block."""
    img = grain((150, 154, 158), 9302, 16, 2)
    for y in range(16):
        if (y // 4) % 2 == 0:
            rect(img, 6, y, 10, y + 1, BLUE)
    return img


TEXTURES = {
    "crac_panel": lambda: panel(WHITE, 9311),
    "crac_front_upper": lambda: front_upper(WHITE, 9312),
    "crac_front_lower": lambda: front_lower(WHITE, 9313),
    "crac_grille": lambda: grille(WHITE, 9314),
    "crah_panel": lambda: panel(GRAPHITE, 9321),
    "crah_front_upper": lambda: front_upper(GRAPHITE, 9322),
    "crah_front_lower": lambda: front_lower(GRAPHITE, 9323),
    "crah_grille": lambda: grille(GRAPHITE, 9324),
    "plinth": plinth,
    "zone_boundary": boundary,
}

ALL = ("north", "south", "east", "west", "up", "down")


def box(frm, to, tex, faces=ALL, per=None, uv=None):
    (x0, y0, z0), (x1, y1, z1) = frm, to

    def wrap(v):
        return v - 16 if v > 16 else v
    uvs = {"north": [16 - x1, 16 - wrap(y1), 16 - x0, 16 - wrap(y0)],
           "south": [x0, 16 - wrap(y1), x1, 16 - wrap(y0)],
           "east": [16 - z1, 16 - wrap(y1), 16 - z0, 16 - wrap(y0)],
           "west": [z0, 16 - wrap(y1), z1, 16 - wrap(y0)],
           "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
    out = {}
    for f in faces:
        u = (uv or {}).get(f) or [round(min(16, max(0, v)), 3) for v in uvs[f]]
        out[f] = {"texture": "#" + (per or {}).get(f, tex), "uv": u}
    return {"from": list(frm), "to": list(to), "faces": out}


def cabinet(upflow):
    """The cabinet from y 1 to 30 on a plinth, front at z 1. The upper element (y 16 to 30) takes
    whole faces of its textures, so each face's uv is set whole."""
    whole = [0, 0, 16, 16]
    els = [box((1, 0, 2), (15, 1, 15.5), "plinth"),
           box((0.5, 1, 1), (15.5, 16, 16), "panel", ("north", "south", "east", "west", "down"),
               per={"north": "front_lower"},
               uv={f: whole for f in ("north", "south", "east", "west")}),
           box((0.5, 16, 1), (15.5, 30, 16), "panel", ALL,
               per={"north": "front_upper", "up": "panel" if upflow else "grille"},
               uv={f: whole for f in ALL})]
    if upflow:
        els.append(box((1.5, 30, 2), (14.5, 32, 15), "panel", ALL, per={"north": "grille"},
                       uv={f: whole for f in ALL}))
    return els


def strip():
    return [box((7.25, 15.4, 0), (8.75, 16, 16), "strip")]


DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0], "scale": [0.4] * 3},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2] * 3},
           "fixed": {"rotation": [0, 180, 0], "translation": [0, -2, 0], "scale": [0.4] * 3},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                     "scale": [0.3] * 3},
           "firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                     "scale": [0.3] * 3},
           "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                    "scale": [0.3] * 3}}
STRIP_DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, -6, 0], "scale": [0.9] * 3},
                 "ground": {"rotation": [0, 0, 0], "translation": [0, -10, 0],
                            "scale": [0.5] * 3},
                 "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, -7],
                           "scale": [0.9] * 3},
                 "thirdperson_righthand": {"rotation": [75, 45, 0],
                                           "translation": [0, -5, 0], "scale": [0.5] * 3},
                 "firstperson_righthand": {"rotation": [0, 45, 0],
                                           "translation": [0, -6, 0], "scale": [0.5] * 3},
                 "firstperson_lefthand": {"rotation": [0, 45, 0],
                                          "translation": [0, -6, 0], "scale": [0.5] * 3}}

BLOCKS = {
    # registry: (model, elements, textures prefix, java, names en/de/es/sv)
    "hvac_crac_downflow": (
        "crac_downflow", lambda: cabinet(False), "crac",
        'new BlockHvacCrac("hvac_crac_downflow")',
        ("CRAC Unit (Downflow)", "Präzisionsklimagerät (Downflow)",
         "Unidad CRAC (flujo descendente)", "CRAC-aggregat (nedåtblåsande)")),
    "hvac_crac_upflow": (
        "crac_upflow", lambda: cabinet(True), "crac",
        'new BlockHvacCrac("hvac_crac_upflow")',
        ("CRAC Unit (Upflow)", "Präzisionsklimagerät (Upflow)",
         "Unidad CRAC (flujo ascendente)", "CRAC-aggregat (uppåtblåsande)")),
    "hvac_crah": (
        "crah", lambda: cabinet(False), "crah",
        'new BlockHvacCrac("hvac_crah")',
        ("CRAH Unit (Chilled Water)", "Umluftkühlgerät (Kaltwasser)",
         "Unidad CRAH (agua helada)", "CRAH-aggregat (kylvatten)")),
}
BOUNDARY_NAMES = ("HVAC Zone Boundary", "HVAC-Zonengrenze", "Límite de zona HVAC",
                  "HVAC-zongräns")
LOCALES = ("en_us", "de_de", "es_es", "sv_se")


def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def js(d):
    return (json.dumps(d, indent=2) + "\n").encode()


def facing_state(model_name):
    return {"forge_marker": 1, "defaults": {"model": MODEL_REF % model_name},
            "variants": {"facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                                    "west": {"y": 270}},
                         "inventory": [{}]}}


def lang_lines():
    out = {loc: {} for loc in LOCALES}
    for reg, b in BLOCKS.items():
        for i, loc in enumerate(LOCALES):
            out[loc]["tile.%s.name" % reg] = b[4][i]
    for i, loc in enumerate(LOCALES):
        out[loc]["tile.hvac_zone_boundary.name"] = BOUNDARY_NAMES[i]
    return out


def lang_file(loc, entries):
    """The lang file with each key set: a key already there keeps its place, a new key goes after
    the rooftop units."""
    path = os.path.join(LANG_DIR, loc + ".lang")
    text = open(path, encoding="utf-8").read().replace("\r\n", "\n")
    lines = text.split("\n")
    for key, value in entries.items():
        line = "%s=%s" % (key, value)
        at = [i for i, ln in enumerate(lines) if ln.startswith(key + "=")]
        if at:
            lines[at[0]] = line
            continue
        anchor = max(i for i, ln in enumerate(lines) if ln.startswith("tile.hvac_rtu_heater_"))
        lines.insert(anchor + 1, line)
    return path, "\n".join(lines).encode("utf-8")


def outputs():
    out = {os.path.join(TEX_DIR, n + ".png"): png(f()) for n, f in TEXTURES.items()}
    for reg, (mname, els, prefix, _, _) in BLOCKS.items():
        t = {k: TEX_REF % ("%s_%s" % (prefix, k)) for k in
             ("panel", "front_upper", "front_lower", "grille")}
        t["plinth"] = TEX_REF % "plinth"
        t["particle"] = t["panel"]
        out[os.path.join(MODEL_DIR, mname + ".json")] = js(
            {"parent": "block/block", "ambientocclusion": False, "textures": t,
             "elements": els(), "display": DISPLAY})
        out[os.path.join(STATE_DIR, reg + ".json")] = js(facing_state(mname))
    out[os.path.join(MODEL_DIR, "zone_boundary.json")] = js(
        {"parent": "block/block",
         "textures": {"strip": TEX_REF % "zone_boundary", "particle": TEX_REF % "zone_boundary"},
         "elements": strip(), "display": STRIP_DISPLAY})
    out[os.path.join(STATE_DIR, "hvac_zone_boundary.json")] = js(facing_state("zone_boundary"))
    for loc, entries in lang_lines().items():
        path, data = lang_file(loc, entries)
        out[path] = data
    return out


def fragments():
    print("# CsmTabHvac, after the rooftop coolers")
    for reg, b in BLOCKS.items():
        print("    initTabBlock(%s); // %s" % (b[3], b[4][0]))
    print("# after the zone thermostat")
    print("    initTabBlock(new BlockHvacZoneBoundary()); // HVAC Zone Boundary")


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    a = ap.parse_args()
    if a.fragments:
        fragments()
        return 0
    out = outputs()
    # separate the faces that z-fight and prune the hidden ones, as every JSON-model generator
    # does; in memory, so writing and --check see the same models
    model_depth.separate_outputs(out, HVAC, js)
    if a.check:
        bad = []
        for p, data in out.items():
            have = open(p, "rb").read() if os.path.exists(p) else None
            if have is not None and (p.endswith(".json") or p.endswith(".lang")):
                have = have.replace(b"\r\n", b"\n")
            if have != data:
                bad.append(os.path.relpath(p, REPO))
        if bad:
            print("gen_hvac_data_hall.py: %d file(s) differ:\n  %s" % (len(bad), "\n  ".join(bad)))
            return 1
        print("gen_hvac_data_hall.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
