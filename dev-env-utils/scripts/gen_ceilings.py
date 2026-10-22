#!/usr/bin/env python3
"""Every asset the modern ceiling finishes ship (Building, Interior Finishes tab).

    python dev-env-utils/scripts/gen_ceilings.py
    python dev-env-utils/scripts/gen_ceilings.py --check
    python dev-env-utils/scripts/gen_ceilings.py --fragments   # lang and tab lines

Every one is a full-size block (BlockCeilingFinish, one class constructed by registry name):

- **Plain finishes**: a lay-in tile, perforated metal or pressed tin: the tile's face below, its
  plain edge on the sides (no grid, the white wall angle along the bottom, so the edge of a
  ceiling does not repeat the face), and plain fibreboard above (a metal tile's own metal).
  Relief (the tegular reveal, the embossing, the tin's bevels) is shaded into the texture. A
  tile's grid is drawn half on each edge, so a field of tiles shows one T-bar between
  neighbours.
- **Directional finishes** (wood slats, corrugated metal): the block stores the axis its slats or
  ribs run along, the placer's look, and the blockstate turns the model a quarter for x. Every
  slat and rib pitch divides the block, so a field lines up across blocks either way.
- **Grids** (open cell, exposed T-bar): the grid drawn at the bottom of the block from elements,
  cutout, open to the plenum above; a full block to stand on and select all the same.

Textures are 64 px (32 for the corrugated, whose profile is gen_cladding.py's, recoloured).
"""

import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image

import gen_cladding

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MODULE = os.path.join(REPO, "modules", "building", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(MODULE, "textures", "blocks", "buildingmaterials", "ceilings")
MODEL_DIR = os.path.join(MODULE, "models", "block", "buildingmaterials", "ceilings")
STATE_DIR = os.path.join(MODULE, "blockstates")
TEX_REF = "csm:blocks/buildingmaterials/ceilings/%s"
MODEL_REF = "csm:buildingmaterials/ceilings/%s"

S = 64


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + (255,)


def shift(c, d):
    return clamp([v + d for v in c[:3]])


def canvas(fn, size=S):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = fn(x, y)
    return img


def noise(seed, size=S, amp=6.0):
    rng = random.Random(seed)
    return [[rng.uniform(-amp, amp) for _ in range(size)] for _ in range(size)]


def grid_edge(x, y, size=S, w=3):
    """How far a texel is inside the T-bar half drawn on this tile's edges: 0 on the bar."""
    return min(x, y, size - 1 - x, size - 1 - y) - (w - 1)


# --------------------------------------------------------------------------------------------
# Drawings
# --------------------------------------------------------------------------------------------

TBAR = (236, 236, 232)


def fissured_field(seed, body, fissures):
    """Mineral fibre: speckle, plus short worm-like fissures."""
    n = noise(seed, amp=5)
    rng = random.Random(seed + 1)
    holes = set()
    for _ in range(fissures):
        x, y = rng.randrange(S), rng.randrange(S)
        for _ in range(rng.randint(2, 6)):
            holes.add((x % S, y % S))
            x += rng.choice((-1, 0, 1))
            y += rng.choice((-1, 0, 1))
    return lambda x, y: shift(body, n[y][x] - (34 if (x, y) in holes else 0))


def tegular(seed, body):
    """A tegular lay-in tile: the T-bar half on each edge, flat and white; then the reveal, the
    tile's square edge stepping down past the bar, shaded the same all round (a light from one
    side would turn the wrong way on every turned or mirrored tile); then the fissured face."""
    field = fissured_field(seed, body, 90)

    def px(x, y):
        e = grid_edge(x, y)
        if e < 0:
            return clamp(TBAR)
        if e < 3:
            return shift(body, -34)
        if e == 3:
            return shift(body, -14)
        return field(x, y)
    return canvas(px)


def side_edge(seed, body, metal):
    """A plain tile's sides, seen at a ceiling's edge: the tile's body with no grid on it, and
    along the bottom the white wall angle the perimeter tiles rest on (a metal tile's own
    turned-down edge)."""
    n = noise(seed + 50, amp=3 if metal else 5)

    def px(x, y):
        if y >= S - 3:
            return clamp(TBAR) if not metal else shift(body, 12)
        if y == S - 4:
            return shift(body, -22)
        return shift(body, n[y][x])
    return canvas(px)


BACK = (198, 192, 178)


def back(seed):
    """The back of a lay-in tile, seen from the plenum: plain fibreboard."""
    n = noise(seed, amp=6)
    return canvas(lambda x, y: shift(BACK, n[y][x]))


def fissured(seed, body):
    field = fissured_field(seed, body, 150)

    def px(x, y):
        e = grid_edge(x, y)
        if e < 0:
            return clamp(TBAR)
        # the tile's square edge against the bar: a faint line, or a white field of white tiles
        # shows no grid at all
        return shift(body, -26) if e == 0 else field(x, y)
    return canvas(px)


def embossed(seed, body):
    """Square-embossed: a 4 x 4 field of raised squares, each bevelled light top-left."""
    n = noise(seed, amp=3)
    cell = 14

    def px(x, y):
        e = grid_edge(x, y)
        if e < 0:
            return clamp(TBAR)
        if e == 0:
            return shift(body, -26)
        cx, cy = (x - 4) % cell, (y - 4) % cell
        d = n[y][x]
        if 1 <= cx <= 11 and 1 <= cy <= 11:
            if cx == 1 or cy == 1:
                d += 12
            elif cx == 11 or cy == 11:
                d -= 18
        else:
            d -= 6
        return shift(body, d)
    return canvas(px)


def perforated(seed, body):
    """Perforated metal: round holes on a 4-texel pitch, a plain border, a faint brushed grain."""
    rng = random.Random(seed)
    grain = [rng.uniform(-3, 3) for _ in range(S)]

    def px(x, y):
        e = grid_edge(x, y)
        if e < 0:
            return shift(body, 10)
        d = grain[y]
        if e >= 3 and x % 4 in (1, 2) and y % 4 in (1, 2):
            return clamp((22, 22, 24))
        if e < 3:
            d += 4
        return shift(body, d)
    return canvas(px)


def tin(seed, body):
    """Pressed tin: a medallion square in a bevelled frame, with a raised boss at the middle and
    a ring of beads, all lit top-left as stamped metal is."""
    n = noise(seed, amp=4)
    c = (S - 1) / 2

    def px(x, y):
        d = n[y][x]
        e = min(x, y, S - 1 - x, S - 1 - y)
        if e < 2:
            d -= 26
        elif e < 5:
            d += 22 if min(x, y) < min(S - 1 - x, S - 1 - y) else -20
        elif e in (12, 13):
            d += 16 if min(x, y) < min(S - 1 - x, S - 1 - y) else -14
        r = math.hypot(x - c, y - c)
        if r < 8:
            d += 14 * math.cos((x - c + y - c) / 10)
        elif 13 <= r < 15:
            d -= 18
        a = math.atan2(y - c, x - c)
        if 19 <= r < 22 and (math.degrees(a) % 30) < 10:
            d += 20
        return shift(body, d)
    return canvas(px)


def slats(seed, body):
    """Wood slats running along v (the block's axis): four 12-texel slats a block, each with its
    own grain and a lit edge, over a 4-texel black acoustic gap."""
    rng = random.Random(seed)
    tone = [rng.uniform(-10, 10) for _ in range(4)]
    grain = [[rng.uniform(-7, 7) for _ in range(S)] for _ in range(S)]
    # the grain runs along the slat: smear each value down its column
    for x in range(S):
        for y in range(1, S):
            if rng.random() < 0.8:
                grain[x][y] = grain[x][y - 1]

    def px(x, y):
        i, o = divmod(x, 16)
        if o < 2 or o >= 14:
            return clamp((18, 17, 16))
        d = tone[i] + grain[x][y]
        if o == 2:
            d += 14
        elif o == 13:
            d -= 16
        return shift(body, d)
    return canvas(px)


def corrugated(seed, body):
    # gen_cladding's ribs run plumb (along v), every pitch dividing the block
    return gen_cladding.corrugated(random.Random(seed), body)


def open_cell(seed, body):
    """The open cell's blades, seen from below or the side: a plain swatch with a lit edge."""
    n = noise(seed, size=16, amp=3)
    return canvas(lambda x, y: shift(body, n[y][x]), size=16)


def tbar(seed, body):
    n = noise(seed, size=16, amp=2)
    return canvas(lambda x, y: shift(body, n[y][x]), size=16)


# --------------------------------------------------------------------------------------------
# Catalogue: registry -> (drawing, colour, seed, names en/es/de/sv). Order is creative order.
# --------------------------------------------------------------------------------------------

CATALOGUE = {
    "ceiling_tegular_white": (tegular, (232, 231, 226), 4301, (
        "Tegular Ceiling Tile (White)", "Placa de Techo Tegular (Blanca)",
        "Tegular-Deckenplatte (Weiß)", "Tegulär Takplatta (Vit)")),
    "ceiling_tegular_grey": (tegular, (176, 178, 180), 4302, (
        "Tegular Ceiling Tile (Light Grey)", "Placa de Techo Tegular (Gris Claro)",
        "Tegular-Deckenplatte (Hellgrau)", "Tegulär Takplatta (Ljusgrå)")),
    "ceiling_fissured_white": (fissured, (234, 233, 228), 4303, (
        "Fine Fissured Ceiling Tile", "Placa de Techo Fisurada Fina",
        "Fein Gerissene Deckenplatte", "Finsprickig Takplatta")),
    "ceiling_embossed_white": (embossed, (232, 231, 227), 4304, (
        "Square Embossed Ceiling Tile", "Placa de Techo con Relieve Cuadrado",
        "Deckenplatte mit Quadratprägung", "Takplatta med Kvadratrelief")),
    "ceiling_perforated_silver": (perforated, (176, 180, 184), 4305, (
        "Perforated Metal Ceiling Tile (Silver)", "Placa de Techo de Metal Perforado (Plata)",
        "Gelochte Metalldeckenplatte (Silber)", "Perforerad Metalltakplatta (Silver)")),
    "ceiling_perforated_white": (perforated, (228, 228, 226), 4306, (
        "Perforated Metal Ceiling Tile (White)", "Placa de Techo de Metal Perforado (Blanca)",
        "Gelochte Metalldeckenplatte (Weiß)", "Perforerad Metalltakplatta (Vit)")),
    "ceiling_tin_silver": (tin, (178, 180, 182), 4307, (
        "Pressed Tin Ceiling (Silver)", "Techo de Hojalata Prensada (Plata)",
        "Geprägte Blechdecke (Silber)", "Pressat Plåttak (Silver)")),
    "ceiling_tin_copper": (tin, (150, 96, 62), 4308, (
        "Pressed Tin Ceiling (Antique Copper)", "Techo de Hojalata Prensada (Cobre Antiguo)",
        "Geprägte Blechdecke (Antikkupfer)", "Pressat Plåttak (Antik Koppar)")),
    "ceiling_slat_oak": (slats, (196, 158, 108), 4309, (
        "Wood Slat Ceiling (Light Oak)", "Techo de Listones de Madera (Roble Claro)",
        "Holzlamellendecke (Helle Eiche)", "Träribbstak (Ljus Ek)")),
    "ceiling_slat_walnut": (slats, (104, 72, 50), 4310, (
        "Wood Slat Ceiling (Walnut)", "Techo de Listones de Madera (Nogal)",
        "Holzlamellendecke (Nussbaum)", "Träribbstak (Valnöt)")),
    "ceiling_corrugated_silver": (corrugated, (168, 172, 176), 4311, (
        "Corrugated Metal Ceiling (Silver)", "Techo de Metal Corrugado (Plata)",
        "Wellblechdecke (Silber)", "Korrugerat Plåttak (Silver)")),
    "ceiling_corrugated_white": (corrugated, (226, 226, 224), 4312, (
        "Corrugated Metal Ceiling (White)", "Techo de Metal Corrugado (Blanco)",
        "Wellblechdecke (Weiß)", "Korrugerat Plåttak (Vitt)")),
    "ceiling_corrugated_black": (corrugated, (44, 45, 48), 4313, (
        "Corrugated Metal Ceiling (Black)", "Techo de Metal Corrugado (Negro)",
        "Wellblechdecke (Schwarz)", "Korrugerat Plåttak (Svart)")),
    "ceiling_open_cell_black": (open_cell, (34, 35, 38), 4314, (
        "Open Cell Ceiling (Black)", "Techo de Celda Abierta (Negro)",
        "Rasterdecke (Schwarz)", "Öppet Rastertak (Svart)")),
    "ceiling_open_cell_white": (open_cell, (230, 230, 228), 4315, (
        "Open Cell Ceiling (White)", "Techo de Celda Abierta (Blanco)",
        "Rasterdecke (Weiß)", "Öppet Rastertak (Vitt)")),
    "ceiling_grid_white": (tbar, (238, 238, 234), 4316, (
        "Exposed Ceiling Grid (White)", "Perfilería de Techo Vista (Blanca)",
        "Offenes Deckenraster (Weiß)", "Synligt Takbärverk (Vitt)")),
    "ceiling_grid_black": (tbar, (32, 33, 36), 4317, (
        "Exposed Ceiling Grid (Black)", "Perfilería de Techo Vista (Negra)",
        "Offenes Deckenraster (Schwarz)", "Synligt Takbärverk (Svart)")),
}


def kind(reg):
    if "_open_cell_" in reg or "_grid_" in reg:
        return "grid"
    if "_slat_" in reg or "_corrugated_" in reg:
        return "axis"
    return "plain"


# --------------------------------------------------------------------------------------------
# Models
# --------------------------------------------------------------------------------------------

def box(frm, to, faces):
    """An element whose faces take their uv from where they sit in the block."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    uv = {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
          "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
          "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}
    return {"from": list(frm), "to": list(to),
            "faces": {f: {"texture": "#b", "uv": uv[f]} for f in faces}}


SIDES = ("north", "south", "east", "west")


def grid_model(reg):
    """The open cell: blades every 4 px each way, 4 px deep at the bottom of the block, the
    outermost half a blade on each edge so neighbours make one blade. The exposed grid: the
    T-bar's flange (1 px each side of the block line, so two blocks make one 2 px flange) and
    its web above it, round the block's edges only."""
    els = []
    if "_open_cell_" in reg:
        depth, t = 4, 0.5
        for i in range(0, 17, 4):
            a, b = max(0, i - t), min(16, i + t)
            els.append(box((a, 0, 0), (b, depth, 16), ("down", "up", "east", "west")))
            els.append(box((0, 0, a), (16, depth, b), ("down", "up", "north", "south")))
    else:
        for a, b in ((0, 1), (15, 16)):
            els.append(box((a, 0, 0), (b, 0.5, 16), ("down", "up", "east", "west")))
            els.append(box((0, 0, a), (16, 0.5, b), ("down", "up", "north", "south")))
        for a, b in ((0, 0.25), (15.75, 16)):
            els.append(box((a, 0.5, 0), (b, 2, 16), ("up", "east", "west")))
            els.append(box((0, 0.5, a), (16, 2, b), ("up", "north", "south")))
    return {"parent": "block/block", "ambientocclusion": False,
            "textures": {"b": TEX_REF % reg, "particle": TEX_REF % reg}, "elements": els}


def blockstate(reg):
    k = kind(reg)
    if k == "grid":
        model = MODEL_REF % reg
        return {"forge_marker": 1, "defaults": {"model": model},
                "variants": {"normal": [{}], "inventory": [{}]}}
    if k == "axis":
        d = {"model": "cube_all", "textures": {"all": TEX_REF % reg}}
        return {"forge_marker": 1, "defaults": d,
                "variants": {"axis": {"z": {}, "x": {"y": 90}}, "inventory": [{}]}}
    # a plain tile: its face below, its plain edge on the sides, its back above
    side = TEX_REF % (reg + "_side")
    top = TEX_REF % ("ceiling_tile_back" if not metal(reg) else reg + "_side")
    d = {"model": "cube", "textures": {
        "down": TEX_REF % reg, "up": top, "north": side, "south": side, "east": side,
        "west": side, "particle": TEX_REF % reg}}
    return {"forge_marker": 1, "defaults": d, "variants": {"normal": [{}], "inventory": [{}]}}


def metal(reg):
    return "_perforated_" in reg or "_tin_" in reg


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------

def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def outputs():
    """Every file this script writes: path -> bytes."""
    out = {os.path.join(TEX_DIR, "ceiling_tile_back.png"): png(back(4399))}
    for reg, (draw, colour, seed, _) in CATALOGUE.items():
        out[os.path.join(TEX_DIR, reg + ".png")] = png(draw(seed, colour))
        if kind(reg) == "plain":
            out[os.path.join(TEX_DIR, reg + "_side.png")] = png(side_edge(seed, colour,
                                                                          metal(reg)))
        out[os.path.join(STATE_DIR, reg + ".json")] = (
            json.dumps(blockstate(reg), indent=2) + "\n").encode()
        if kind(reg) == "grid":
            out[os.path.join(MODEL_DIR, reg + ".json")] = (
                json.dumps(grid_model(reg), indent=2) + "\n").encode()
    return out


def fragments():
    langs = ("en_us", "es_es", "de_de", "sv_se")
    for i, lang in enumerate(langs):
        print("# %s.lang" % lang)
        for reg, (_, _, _, names) in CATALOGUE.items():
            print("tile.%s.name=%s" % (reg, names[i]))
    print("# CsmTabInteriorFinishes")
    for reg, (_, _, _, names) in CATALOGUE.items():
        print('    initTabBlock(new BlockCeilingFinish("%s")); // %s' % (reg, names[0]))


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
            print("gen_ceilings.py: %d file(s) differ:\n  %s" % (len(bad), "\n  ".join(bad)))
            return 1
        print("gen_ceilings.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
