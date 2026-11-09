#!/usr/bin/env python3
"""Every asset the HVAC tab's heating and cooling units ship: baked OBJ models, their MTL, the
small sprites they wear, their blockstates and their lang lines.

    py dev-env-utils/scripts/gen_hvac_units.py
    py dev-env-utils/scripts/gen_hvac_units.py --check
    py dev-env-utils/scripts/gen_hvac_units.py --fragments      # lang and tab lines
    py dev-env-utils/scripts/gen_hvac_units.py --report         # quads per model
    py dev-env-utils/scripts/gen_hvac_units.py --sheet out.png  # offline preview contact sheet

- **Condensing unit** (`hvac_heater`, `hvac_cooler` and their `_black` / `_silver`; the classes
  `BlockHvacHeater...`, `BlockHvacCooler...` on `AbstractBlockHvacCondenser`): the square
  outdoor cabinet of a split system or heat pump, a Carrier-style 2-4 ton unit. Louvred coil
  guard on four sides between corner posts, a top cap with the fan under a round wire grille, a
  service panel with its badge and rating plate on one corner, the line set and the electrical
  whip low on the back, on a composite pad. Heater and cooler share the model.
- **Rooftop unit** (`hvac_rtu_heater`, `hvac_rtu_cooler` and finishes, `AbstractBlockHvacRtu`):
  a packaged rooftop unit in the old 2 1/8 x 2 x 3 block footprint, drawn from its one block: a
  roof curb, the base rail with lifting lugs, the outside air hood on the front, filter and
  economizer louvres, hinged access doors, the condenser section at the back with its coil on
  three sides and two fans on top, the electrical disconnect, the gas flue and the gas riser.
- **Large rooftop unit** (`hvac_rtu_large` and finishes, `BlockHvacPackagedRtu`, its cells
  `hvac_rtu_large_part`): the same unit 3 x 2 x 5 blocks with four fans. A multi-block: every
  cell is a real block that draws its own slice of the unit, cut from the whole model here
  (`slice_mesh`), so nothing is drawn outside the cell that draws it.
- **PTAC** (`hvac_ptac`, `hvac_ptac_almond`, `BlockHvacPtac`): a hotel room's packaged terminal
  air conditioner on the floor against an outside wall, discharge grille on top, intake grille in
  front, the control door top right; and its **wall sleeve grille** (`hvac_ptac_wall_grille`,
  decorative) for the outside face of the wall.

Every model faces north (front toward -z); Forge blockstates turn it. Sprites: one 64 px sheet
per finish (`finish_<name>`: plain paint, the service panel, access doors, louvres, the air
hood, the coils, the PTAC's faces) worn by the material `finish`, and one shared sheet
(`parts`: colour swatches, the fan well and its wire grille, the badges, the disconnect, the
flue hood, the sleeve louvre) worn by `parts`. A blockstate picks the finish by retexturing
`#finish`, so the geometry is written once. Faces are axis aligned except the hood's slope and
the fan well's walls; UVs are planar per face.

Multi-block cells are numbered over the footprint (model frame: +x east, +y up, +z towards the
back, the master at the origin), y then z then x, skipping the master; the blockstate names
each cell's model by its offset, which `PackagedRtuBlockstateTest` reads back. The lang lines
are written into the module's four lang files by key, keeping each file's line endings.
"""

import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HVAC = os.path.join(REPO, "modules", "hvac", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(HVAC, "textures", "blocks", "hvac", "units")
MODEL_DIR = os.path.join(HVAC, "models", "block", "hvac", "units")
STATE_DIR = os.path.join(HVAC, "blockstates")
LANG_DIR = os.path.join(HVAC, "lang")
TEX_REF = "csm:blocks/hvac/units/%s"
OBJ_REF = "csm:hvac/units/%s.obj"
MODEL_REF = "csm:hvac/units/%s"
MTL_NAME = "hvac_units.mtl"
GEN = "dev-env-utils/scripts/gen_hvac_units.py"
S = 64  # every sheet is 64 px square

# --------------------------------------------------------------------------------------------
# Finishes and sprites
# --------------------------------------------------------------------------------------------

# Paint. Sandstone is the warm beige-grey of a residential condenser or a packaged unit's
# standard cabinet; silver and black are the other two cabinet finishes; off-white and almond
# are the PTAC's.
FINISHES = {
    "sandstone": (170, 160, 139),
    "silver": (184, 188, 191),
    "black": (50, 52, 55),
    "offwhite": (228, 225, 214),
    "almond": (214, 202, 178),
}
CABINET_FINISHES = ("sandstone", "black", "silver")
PTAC_FINISHES = ("offwhite", "almond")


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c[:3]) + (255,)


def shade(c, k):
    return tuple(v * k for v in c[:3])


def mix(a, b, t):
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


def fill(img, x0, y0, x1, y1, colour, rng=None, amp=0):
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            n = rng.uniform(-amp, amp) if rng and amp else 0
            px[x, y] = clamp([v + n for v in colour])


def hline(img, x0, x1, y, colour):
    fill(img, x0, y, x1, y + 1, colour)


def vline(img, x, y0, y1, colour):
    fill(img, x, y0, x + 1, y1, colour)


# Where each picture sits on the finish sheet: (x, y, w, h) in sheet pixels.
F_REG = {
    "panel": (0, 0, 16, 16),
    "svc": (0, 16, 16, 16),
    "door": (16, 0, 16, 32),
    "louver": (32, 0, 16, 16),
    "hood": (48, 0, 16, 16),
    "rtucoil": (32, 16, 32, 16),
    "coil": (0, 32, 32, 32),
    "ptac_front": (32, 32, 32, 16),
    "ptac_top": (32, 48, 32, 16),
}

# ... and on the shared parts sheet. Swatches are 8 px cells; a face wearing one samples its
# middle, so it reads as a flat colour at every mip level that matters.
SWATCHES = {
    "dark": (22, 23, 25), "rubber": (48, 48, 50), "copper": (183, 112, 68),
    "insul": (28, 28, 29), "galv": (166, 170, 173), "pad": (128, 127, 122),
    "conduit": (88, 92, 98), "gas": (214, 178, 38),
    "steel": (104, 107, 110), "red": (164, 38, 32), "label": (226, 226, 220),
    "alu": (196, 198, 200), "basepan": (36, 37, 39), "hub": (78, 80, 84),
    "brass": (178, 150, 82), "white": (236, 236, 232),
}
P_REG = {
    "well": (0, 16, 32, 32),
    "grille": (32, 16, 32, 32),
    "badge": (0, 48, 16, 8),
    "rating": (0, 56, 16, 8),
    "disconnect": (16, 48, 16, 16),
    "flue": (32, 48, 16, 16),
    "sleeve": (48, 48, 16, 16),
}


def swatch_cell(name):
    i = list(SWATCHES).index(name)
    return (i % 8) * 8, (i // 8) * 8


def finish_sheet(name):
    c = FINISHES[name]
    dark = name == "black"
    rng = random.Random(hash_seed("finish", name))
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    lit = 1.18 if dark else 1.08    # a highlight that still reads on black paint
    edge = 0.72 if dark else 0.8

    def at(reg):
        return F_REG[reg]

    # plain paint
    x, y, w, h = at("panel")
    fill(img, x, y, x + w, y + h, c, rng, 1.5)

    # the condenser's service panel: a seam round it, four screws, a small louvre near the top
    x, y, w, h = at("svc")
    fill(img, x, y, x + w, y + h, c, rng, 1.5)
    for i in range(w):
        img.putpixel((x + i, y), clamp(shade(c, lit)))
        img.putpixel((x + i, y + h - 1), clamp(shade(c, edge)))
    for j in range(h):
        img.putpixel((x, y + j), clamp(shade(c, edge * 1.05)))
        img.putpixel((x + w - 1, y + j), clamp(shade(c, edge)))
    for sx, sy in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
        img.putpixel((x + sx, y + sy), clamp(shade(c, 0.55)))
    for r in (4, 6, 8):
        hline(img, x + 4, x + w - 4, y + r, shade(c, 0.45))
        hline(img, x + 4, x + w - 4, y + r - 1, shade(c, lit))

    # a packaged unit's hinged access door: seam, two hinges on the left, the latch handle
    x, y, w, h = at("door")
    fill(img, x, y, x + w, y + h, c, rng, 1.5)
    fill(img, x, y, x + w, y + 1, shade(c, 0.5))
    fill(img, x, y + h - 1, x + w, y + h, shade(c, 0.5))
    fill(img, x, y, x + 1, y + h, shade(c, 0.55))
    fill(img, x + w - 1, y, x + w, y + h, shade(c, 0.5))
    hline(img, x + 1, x + w - 1, y + 1, shade(c, lit))
    for hy in (5, h - 7):
        fill(img, x + 1, y + hy, x + 3, y + hy + 3, shade(c, 0.6))
        img.putpixel((x + 1, y + hy), clamp(shade(c, lit)))
    hx, hy = x + w - 5, y + h // 2 - 4
    fill(img, hx, hy, hx + 3, hy + 8, (36, 36, 38))
    fill(img, hx + 1, hy + 1, hx + 2, hy + 7, (92, 94, 98))
    img.putpixel((hx + 1, hy + 2), clamp((140, 142, 146)))

    # louvred panel (filter and economizer section): blades pitch 4
    x, y, w, h = at("louver")
    fill(img, x, y, x + w, y + h, c, rng, 1.0)
    for r in range(1, h - 1, 4):
        hline(img, x + 1, x + w - 1, y + r, shade(c, lit))
        hline(img, x + 1, x + w - 1, y + r + 1, shade(c, 0.95))
        hline(img, x + 1, x + w - 1, y + r + 2, shade(c, 0.62))
        hline(img, x + 1, x + w - 1, y + r + 3, (30, 31, 33))
    for j in range(h):
        img.putpixel((x, y + j), clamp(shade(c, edge)))
        img.putpixel((x + w - 1, y + j), clamp(shade(c, edge)))
    hline(img, x, x + w, y, shade(c, edge))
    hline(img, x, x + w, y + h - 1, shade(c, edge * 0.9))

    # the outside air hood's face: deep louvres with the bird screen behind
    x, y, w, h = at("hood")
    fill(img, x, y, x + w, y + h, c, rng, 1.0)
    for r in range(1, h - 1, 3):
        hline(img, x + 1, x + w - 1, y + r, shade(c, lit))
        hline(img, x + 1, x + w - 1, y + r + 1, shade(c, 0.7))
        for i in range(1, w - 1):
            img.putpixel((x + i, y + r + 2),
                         clamp((52, 54, 56) if (i + r) % 2 else (24, 25, 27)))
    for j in range(h):
        img.putpixel((x, y + j), clamp(shade(c, edge)))
        img.putpixel((x + w - 1, y + j), clamp(shade(c, edge)))
    hline(img, x, x + w, y + h - 1, shade(c, edge))

    def coil_rows(x, y, w, h, pitch):
        """A coil guard: slats in the cabinet paint, the fin coil in the gaps. The coil reads as
        fine grey lines -- aluminium fins with the tube rows across them."""
        for r in range(h):
            k = r % pitch
            for i in range(w):
                if k == 0:
                    col = shade(c, lit)
                elif k < pitch - 2:
                    col = shade(c, 0.94)
                elif k == pitch - 2:
                    col = shade(c, 0.6)
                else:
                    fin = (92, 96, 99) if i % 2 else (64, 67, 70)
                    col = fin
                img.putpixel((x + i, y + r), clamp([v + rng.uniform(-1, 1) for v in col]))

    # the condenser's coil guard: pitch 4, a frame down each side
    x, y, w, h = at("coil")
    coil_rows(x, y, w, h, 4)
    for j in range(h):
        img.putpixel((x, y + j), clamp(shade(c, edge)))
        img.putpixel((x + w - 1, y + j), clamp(shade(c, edge * 0.92)))
    hline(img, x, x + w, y, shade(c, edge))
    hline(img, x, x + w, y + h - 1, shade(c, edge * 0.85))

    # the rooftop unit's condenser coil behind its hail guard: pitch 3 over a 32 x 16 strip
    x, y, w, h = at("rtucoil")
    coil_rows(x, y, w, h, 3)
    for j in range(h):
        img.putpixel((x, y + j), clamp(shade(c, edge)))
        img.putpixel((x + w - 1, y + j), clamp(shade(c, edge)))

    # PTAC front: a plain band with its badge, the intake grille below
    x, y, w, h = at("ptac_front")
    fill(img, x, y, x + w, y + h, c, rng, 1.0)
    hline(img, x, x + w, y, shade(c, 1.05))
    hline(img, x, x + w, y + h - 1, shade(c, 0.72))
    vline(img, x, y, y + h, shade(c, 0.82))
    vline(img, x + w - 1, y, y + h, shade(c, 0.8))
    fill(img, x + 3, y + 2, x + 9, y + 3, (110, 112, 116))       # badge
    fill(img, x + 3, y + 3, x + 9, y + 4, shade(c, 0.86))
    hline(img, x + 2, x + w - 2, y + 4, shade(c, 0.78))           # the grille's frame
    for r in range(5, h - 2, 2):
        hline(img, x + 3, x + w - 3, y + r, (54, 55, 57))
        hline(img, x + 3, x + w - 3, y + r + 1, shade(c, 0.97))
    hline(img, x + 2, x + w - 2, y + h - 2, shade(c, 0.8))

    # PTAC top (u along the width, v front to back): the control door at the left of the
    # texture (the unit's right, seen from the room), the discharge grille's slots across the
    # rest, running front to back
    x, y, w, h = at("ptac_top")
    fill(img, x, y, x + w, y + h, c, rng, 1.0)
    hline(img, x, x + w, y, shade(c, 1.06))
    fill(img, x + 1, y + 2, x + 9, y + h - 2, shade(c, 0.96))     # control door
    hline(img, x + 1, x + 9, y + 2, shade(c, 0.7))
    hline(img, x + 1, x + 9, y + h - 3, shade(c, 0.7))
    vline(img, x + 1, y + 2, y + h - 2, shade(c, 0.72))
    vline(img, x + 8, y + 2, y + h - 2, shade(c, 0.72))
    fill(img, x + 4, y + 3, x + 6, y + 4, shade(c, 0.6))          # finger notch
    fill(img, x + 10, y + 2, x + w - 2, y + h - 3, shade(c, 0.8))  # grille frame
    for i in range(11, w - 2, 2):
        vline(img, x + i, y + 3, y + h - 4, (40, 41, 43))
    return img


def hash_seed(*parts):
    h = 0
    for p in parts:
        for ch in str(p):
            h = (h * 131 + ord(ch)) & 0x7FFFFFFF
    return h


def supersampled(size, draw_fn, factor=4):
    big = Image.new("RGBA", (size * factor, size * factor), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(big), factor)
    return big.resize((size, size), Image.LANCZOS)


def parts_sheet():
    rng = random.Random(hash_seed("parts"))
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    for name, col in SWATCHES.items():
        x, y = swatch_cell(name)
        fill(img, x, y, x + 8, y + 8, col, rng, 1.0)

    # the fan well: the dark orifice, three swept blades and the motor hub
    def well(d, f):
        n = 32 * f
        c = n / 2
        d.rectangle((0, 0, n, n), fill=(16, 16, 18, 255))
        d.ellipse((c - 15.5 * f, c - 15.5 * f, c + 15.5 * f, c + 15.5 * f), fill=(30, 31, 34, 255))
        d.ellipse((c - 14.5 * f, c - 14.5 * f, c + 14.5 * f, c + 14.5 * f), fill=(14, 14, 16, 255))
        for k in range(3):
            a0 = math.radians(k * 120 + 10)
            pts = []
            for t in range(0, 11):
                r = (3.5 + 10.5 * t / 10) * f
                a = a0 + 0.35 * t / 10
                pts.append((c + r * math.cos(a), c + r * math.sin(a)))
            for t in range(10, -1, -1):
                r = (3.5 + 10.5 * t / 10) * f
                a = a0 + 0.35 * t / 10 + (0.55 + 0.25 * t / 10)
                pts.append((c + r * math.cos(a), c + r * math.sin(a)))
            d.polygon(pts, fill=(112, 115, 120, 255))
            d.line(pts[:11], fill=(150, 153, 158, 255), width=max(1, f))
        d.ellipse((c - 4 * f, c - 4 * f, c + 4 * f, c + 4 * f), fill=(88, 90, 95, 255))
        d.ellipse((c - 2 * f, c - 2 * f, c + 2 * f, c + 2 * f), fill=(132, 134, 138, 255))
    x, y, w, h = P_REG["well"]
    img.paste(supersampled(32, well), (x, y))

    # the wire fan guard: rings and spokes, clear between them
    def grille(d, f):
        n = 32 * f
        c = n / 2
        col = (54, 56, 59, 255)
        for r, wd in ((15.1, 1.7), (12.0, 1.0), (9.0, 1.0), (6.0, 1.0), (3.2, 1.2)):
            d.ellipse((c - r * f, c - r * f, c + r * f, c + r * f), outline=col,
                      width=int(round(wd * f)))
        for k in range(8):
            a = math.radians(k * 45 + 22.5)
            d.line((c + 3.2 * f * math.cos(a), c + 3.2 * f * math.sin(a),
                    c + 15.0 * f * math.cos(a), c + 15.0 * f * math.sin(a)), fill=col,
                   width=f)
        d.ellipse((c - 2.2 * f, c - 2.2 * f, c + 2.2 * f, c + 2.2 * f), fill=(44, 45, 48, 255))
    g = supersampled(32, grille)
    gp = g.load()
    for j in range(32):
        for i in range(32):
            r, gg, b, a = gp[i, j]
            gp[i, j] = (r, gg, b, 255) if a > 100 else (0, 0, 0, 0)
    x, y, w, h = P_REG["grille"]
    for j in range(32):
        for i in range(32):
            img.putpixel((x + i, y + j), gp[i, j])

    # the badge: a dark plate with a bright bar, no maker's name
    x, y, w, h = P_REG["badge"]
    fill(img, x, y, x + w, y + h, (44, 46, 50))
    fill(img, x + 1, y + 1, x + w - 1, y + h - 1, (64, 67, 72))
    fill(img, x + 3, y + 3, x + 13, y + 5, (200, 202, 206))
    fill(img, x + 3, y + 3, x + 5, y + 5, (186, 40, 34))
    # the rating plate: a silver label with lines of print
    x, y, w, h = P_REG["rating"]
    fill(img, x, y, x + w, y + h, (196, 198, 200))
    for r in (1, 3, 5):
        hline(img, x + 2, x + w - 2 - r, y + r, (96, 98, 102))
    hline(img, x, x + w, y + h - 1, (150, 152, 156))
    # the disconnect switch: grey enclosure, the operating handle, a warning label
    x, y, w, h = P_REG["disconnect"]
    fill(img, x, y, x + w, y + h, (120, 124, 128), rng, 1.5)
    hline(img, x, x + w, y, (150, 154, 158))
    hline(img, x, x + w, y + h - 1, (80, 82, 86))
    vline(img, x, y, y + h, (96, 100, 104))
    vline(img, x + w - 1, y, y + h, (84, 86, 90))
    fill(img, x + 10, y + 4, x + 13, y + 12, (34, 34, 36))
    fill(img, x + 11, y + 3, x + 12, y + 7, (60, 60, 64))
    fill(img, x + 2, y + 3, x + 8, y + 6, (232, 186, 40))
    fill(img, x + 2, y + 6, x + 8, y + 7, (40, 40, 40))
    # the gas flue hood: galvanized, with its louvred outlet
    x, y, w, h = P_REG["flue"]
    fill(img, x, y, x + w, y + h, SWATCHES["galv"], rng, 2.0)
    for r in range(3, h - 2, 3):
        hline(img, x + 2, x + w - 2, y + r, (40, 42, 44))
        hline(img, x + 2, x + w - 2, y + r - 1, (196, 200, 204))
    vline(img, x, y, y + h, (130, 134, 138))
    vline(img, x + w - 1, y, y + h, (120, 124, 128))
    # the architectural louvre of a PTAC's wall sleeve: anodized aluminium, deep blades
    x, y, w, h = P_REG["sleeve"]
    al = (188, 190, 192)
    fill(img, x, y, x + w, y + h, al, rng, 1.0)
    for r in range(2, h - 3, 3):
        hline(img, x + 1, x + w - 1, y + r, shade(al, 1.1))
        hline(img, x + 1, x + w - 1, y + r + 1, shade(al, 0.72))
        hline(img, x + 1, x + w - 1, y + r + 2, (36, 37, 39))
    hline(img, x, x + w, y, shade(al, 0.82))
    hline(img, x, x + w, y + 1, shade(al, 1.06))
    hline(img, x, x + w, y + h - 1, shade(al, 0.7))
    vline(img, x, y, y + h, shade(al, 0.85))
    vline(img, x + w - 1, y, y + h, shade(al, 0.78))
    return img


# --------------------------------------------------------------------------------------------
# Geometry: planar polygons in sixteenths, each with its sheet uv (v down, as Minecraft's rows)
# --------------------------------------------------------------------------------------------

FACE_N = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0),
          "up": (0, 1, 0), "down": (0, -1, 0)}
SIDES = ("north", "south", "east", "west")
ALL = SIDES + ("up", "down")


def reg(sheet, name):
    """A picture's rectangle as (material, u0, v0, u1, v1) in sheet pixels."""
    x, y, w, h = (F_REG if sheet == "finish" else P_REG)[name]
    return (sheet, x, y, x + w, y + h)


def sw(name):
    """A swatch: the middle of its cell, so its edges never sample a neighbour."""
    x, y = swatch_cell(name)
    return ("parts", x + 2, y + 2, x + 6, y + 6)


PANEL = reg("finish", "panel")


def to3d(face, a, b, c):
    if face in ("north", "south"):
        return (a, b, c)
    if face in ("west", "east"):
        return (c, b, a)
    return (a, c, b)


def face_uv(face, a, b, bounds):
    """Where a point of a face falls on its picture, 0..1 each way. On a north face the
    picture's left is the block's east (x max): the face is seen from the north."""
    a0, b0, a1, b1 = bounds
    fa = (a - a0) / (a1 - a0)
    fb = (b - b0) / (b1 - b0)
    if face == "north":
        return 1 - fa, 1 - fb
    if face in ("south", "west"):
        return fa, 1 - fb
    if face == "east":
        return 1 - fa, 1 - fb
    if face == "up":
        return fa, fb
    return fa, 1 - fb


def newell(pts):
    nx = ny = nz = 0.0
    for i in range(len(pts)):
        x0, y0, z0 = pts[i]
        x1, y1, z1 = pts[(i + 1) % len(pts)]
        nx += (y0 - y1) * (z0 + z1)
        ny += (z0 - z1) * (x0 + x1)
        nz += (x0 - x1) * (y0 + y1)
    m = math.sqrt(nx * nx + ny * ny + nz * nz)
    return (nx / m, ny / m, nz / m) if m > 1e-12 else (0.0, 0.0, 0.0)


class Mesh:
    """Planar convex polygons: (material, [(x, y, z)], [(u, v)] in 0..1 of the sheet, normal)."""

    def __init__(self):
        self.polys = []

    def poly(self, mat, pts, uvs, normal):
        n = newell(pts)
        if n == (0.0, 0.0, 0.0):
            return
        if sum(n[k] * normal[k] for k in range(3)) < 0:
            pts, uvs = pts[::-1], uvs[::-1]
            n = tuple(-v for v in n)
        self.polys.append((mat, list(pts), list(uvs), n))

    def face(self, face, region, pts2d, c, bounds=None):
        """A polygon in an axis-aligned face plane, its picture stretched over ``bounds``
        (a0, b0, a1, b1) in the face's own axes, or over the polygon's extent."""
        if bounds is None:
            a = [p[0] for p in pts2d]
            b = [p[1] for p in pts2d]
            bounds = (min(a), min(b), max(a), max(b))
        mat, u0, v0, u1, v1 = region
        pts, uvs = [], []
        for a, b in pts2d:
            pts.append(to3d(face, a, b, c))
            fu, fv = face_uv(face, a, b, bounds)
            uvs.append(((u0 + fu * (u1 - u0)) / S, (v0 + fv * (v1 - v0)) / S))
        self.poly(mat, pts, uvs, FACE_N[face])

    def rect(self, face, region, a0, b0, a1, b1, c, bounds=None):
        self.face(face, region, [(a0, b0), (a1, b0), (a1, b1), (a0, b1)], c, bounds)

    def bands(self, face, region, a0, b0, a1, b1, c, n):
        """A rect as ``n`` bands stacked up its height, each wearing the whole picture: a
        louvre keeps its blades' pitch on a face taller than the picture is drawn for."""
        for i in range(n):
            lo = b0 + (b1 - b0) * i / n
            hi = b0 + (b1 - b0) * (i + 1) / n
            self.rect(face, region, a0, lo, a1, hi, c)

    def box(self, x0, y0, z0, x1, y1, z1, region, faces=ALL, per=None):
        per = per or {}
        for f in faces:
            r = per.get(f, region)
            if f == "north":
                self.rect(f, r, x0, y0, x1, y1, z0)
            elif f == "south":
                self.rect(f, r, x0, y0, x1, y1, z1)
            elif f == "west":
                self.rect(f, r, z0, y0, z1, y1, x0)
            elif f == "east":
                self.rect(f, r, z0, y0, z1, y1, x1)
            elif f == "up":
                self.rect(f, r, x0, z0, x1, z1, y1)
            else:
                self.rect(f, r, x0, z0, x1, z1, y0)

    def quad(self, region, pts, normal):
        """A quadrilateral of any orientation, its picture laid corner to corner (the first
        point at the picture's top left, then round the top edge)."""
        mat, u0, v0, u1, v1 = region
        uv = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
        self.poly(mat, pts, [(u / S, v / S) for u, v in uv], normal)

    def quads(self):
        return sum(len(split(p[1])) for p in self.polys)


def ngon(cx, cz, r, n=8):
    return [(cx + r * math.cos(2 * math.pi * k / n), cz + r * math.sin(2 * math.pi * k / n))
            for k in range(n)]


def ring_up(m, region, cx, cz, r, rect, y, n=8, bounds=None):
    """A top face over ``rect`` (x0, z0, x1, z1) with a regular n-gon hole of radius ``r``:
    one convex piece per edge of the hole, out to the rectangle along the rays through its
    corners, taking in any corner of the rectangle between them."""
    x0, z0, x1, z1 = rect
    hole = ngon(cx, cz, r, n)

    def hit(t):
        dx, dz = math.cos(t), math.sin(t)
        s = []
        if dx > 1e-9:
            s.append((x1 - cx) / dx)
        if dx < -1e-9:
            s.append((x0 - cx) / dx)
        if dz > 1e-9:
            s.append((z1 - cz) / dz)
        if dz < -1e-9:
            s.append((z0 - cz) / dz)
        k = min(s)
        return (cx + k * dx, cz + k * dz)

    corners = []
    for px, pz in ((x0, z0), (x1, z0), (x1, z1), (x0, z1)):
        corners.append((math.atan2(pz - cz, px - cx) % (2 * math.pi), (px, pz)))
    for k in range(n):
        t0 = 2 * math.pi * k / n
        t1 = 2 * math.pi * (k + 1) / n
        mid = [p for a, p in sorted(corners) if t0 + 1e-9 < a < t1 - 1e-9]
        pts = [hole[k], hit(t0)] + mid + [hit(t1), hole[(k + 1) % n]]
        m.face("up", region, pts, y, bounds or (x0, z0, x1, z1))


def fan(m, cx, cz, r, rect, top, depth, grille_lift=0.2, cap_region=PANEL):
    """A condenser fan in a top panel: the panel round its opening, the dark well with the
    blades and hub on its floor, the motor's hub, and the wire guard just above the panel."""
    ring_up(m, cap_region, cx, cz, r, rect, top)
    hole = ngon(cx, cz, r)
    bot = top - depth
    for k in range(8):
        (ax, az), (bx, bz) = hole[k], hole[(k + 1) % 8]
        mx, mz = (ax + bx) / 2, (az + bz) / 2
        n = (cx - mx, 0, cz - mz)
        m.quad(sw("dark"), [(ax, top, az), (bx, top, bz), (bx, bot, bz), (ax, bot, az)], n)
    m.face("up", reg("parts", "well"), hole, bot, (cx - r, cz - r, cx + r, cz + r))
    h = max(0.8, r * 0.18)
    m.box(cx - h, bot, cz - h, cx + h, bot + min(1.4, depth * 0.45), cz + h, sw("hub"),
          SIDES + ("up",))
    g = r + 0.45
    m.rect("up", reg("parts", "grille"), cx - g, cz - g, cx + g, cz + g, top + grille_lift)


# --------------------------------------------------------------------------------------------
# The condensing unit: one block
# --------------------------------------------------------------------------------------------

def condenser():
    m = Mesh()
    # the composite pad and the unit's base pan
    m.box(0.25, 0, 0.25, 15.75, 1, 15.75, sw("pad"), SIDES + ("up",))
    m.box(1.25, 1, 1.25, 14.75, 2.25, 14.75, sw("basepan"), SIDES + ("up",))
    lo, hi = 2.25, 13.5
    # corner posts (the service panel takes the north-west one): outer faces and returns
    for px, pz in ((13.25, 1), (1, 13.25), (13.25, 13.25)):
        x0, x1, z0, z1 = px, px + 1.75, pz, pz + 1.75
        faces = []
        faces.append("north" if pz < 8 else "south")
        faces.append("west" if px < 8 else "east")
        faces.append("east" if px < 8 else "west")   # the return along the coil face
        faces.append("south" if pz < 8 else "north")
        m.box(x0, lo, z0, x1, hi, z1, PANEL, faces)
    # the coil guard on four sides, 0.5 behind the posts' faces, one picture per side
    coil = reg("finish", "coil")
    span = (2.75, lo, 13.25, hi)
    m.rect("north", coil, 6.5, lo, 13.25, hi, 1.5, span)
    m.rect("south", coil, 2.75, lo, 13.25, hi, 14.5, span)
    m.rect("east", coil, 2.75, lo, 13.25, hi, 14.5, span)
    m.rect("west", coil, 6.5, lo, 13.25, hi, 1.5, span)
    # the service panel round the north-west corner, a touch proud of the posts
    svc = reg("finish", "svc")
    m.rect("north", svc, 0.8, lo, 6.5, hi, 0.8)
    m.rect("west", PANEL, 0.8, lo, 6.5, hi, 0.8)
    m.rect("east", PANEL, 0.8, lo, 1.5, hi, 6.5)
    m.rect("south", PANEL, 0.8, lo, 1.5, hi, 6.5)
    m.rect("north", reg("parts", "badge"), 1.6, 10.6, 5.7, 12.4, 0.7)
    m.rect("west", reg("parts", "rating"), 2.2, 4.0, 5.8, 6.4, 0.7)
    # the top cap: a lip over the cabinet, the fan in it
    cap0, cap1, ctop = 0.6, 15.4, 15.0
    m.box(cap0, hi, cap0, cap1, ctop, cap1, PANEL, SIDES + ("down",))
    fan(m, 8, 8, 6.1, (cap0, cap0, cap1, cap1), ctop, 3.4, grille_lift=0.15)
    # the line set and the electrical whip, low on the back
    m.box(3.3, 3.4, 14.5, 4.7, 4.8, 15.85, sw("insul"), ("south", "east", "west", "up", "down"))
    m.box(5.4, 3.65, 14.5, 6.1, 4.35, 15.7, sw("copper"), ("south", "east", "west", "up", "down"))
    m.box(10.1, 3.5, 14.5, 11.1, 4.5, 15.6, sw("conduit"), ("south", "east", "west", "up", "down"))
    m.box(9.8, 4.6, 14.5, 12.2, 7.4, 14.95, sw("steel"), ("south", "east", "west", "up", "down"))
    return m


# --------------------------------------------------------------------------------------------
# The packaged rooftop unit, at any size
# --------------------------------------------------------------------------------------------

BASE_TOP = 6.0
CAB_TOP = 27.0


def rtu(X0, X1, Z0, Z1, fans_x, fans_z, doors):
    """A packaged rooftop unit filling x X0..X1, z Z0..Z1 (front at Z0), 0..~27.5 high."""
    m = Mesh()
    cx0, cx1 = X0 + 1.5, X1 - 1.5
    cz0, cz1 = Z0 + 3.0, Z1 - 0.5
    width = cx1 - cx0
    tile_w = width / fans_x
    lc = tile_w * fans_z                     # the condenser section, square tiles
    zc = cz1 - lc
    le = max(7.0, (zc - cz0) * 0.3)
    ze = cz0 + le

    # the roof curb under it, and the base rail with its lifting lugs
    m.box(X0 + 3, 0, cz0 + 2, X1 - 3, 4, cz1 - 2, sw("galv"), SIDES)
    m.box(cx0 - 0.5, 4, cz0 - 0.5, cx1 + 0.5, BASE_TOP, cz1 + 0.5, sw("basepan"), ALL)
    for side in (-1, 1):
        x = cx0 - 0.5 if side < 0 else cx1 + 0.5
        for z in (cz0 + 2, cz1 - 4):
            xa, xb = (x - 0.6, x) if side < 0 else (x, x + 0.6)
            m.box(xa, 4.2, z, xb, BASE_TOP, z + 2, sw("steel"),
                  ("west" if side < 0 else "east", "up", "north", "south"))

    # the cabinet's ends: the front with the outside air hood, the coil round the back
    m.rect("north", PANEL, cx0, BASE_TOP, cx1, CAB_TOP, cz0)
    rtucoil = reg("finish", "rtucoil")
    m.bands("south", rtucoil, cx0, BASE_TOP, cx1, CAB_TOP, cz1, 2)
    hx0, hx1 = cx0 + 2.0, cx1 - 2.0
    hy0, hy1, hz = 11.0, 24.0, Z0 + 0.25
    drop = 1.6
    hood = reg("finish", "hood")
    m.rect("north", hood, hx0, hy0, hx1, hy1 - drop, hz)
    m.rect("down", PANEL, hx0, hz, hx1, cz0, hy0)
    for face, x in (("west", hx0), ("east", hx1)):
        m.face(face, PANEL, [(hz, hy0), (cz0, hy0), (cz0, hy1), (hz, hy1 - drop)], x)
    m.quad(PANEL, [(hx0, hy1, cz0), (hx1, hy1, cz0), (hx1, hy1 - drop, hz),
                   (hx0, hy1 - drop, hz)], (0, cz0 - hz, -drop))

    # the long sides: filter and economizer louvres, the access doors, the condenser coil
    louver, door = reg("finish", "louver"), reg("finish", "door")
    dl = (zc - ze) / doors
    for face, x in (("west", cx0), ("east", cx1)):
        m.bands(face, louver, cz0, BASE_TOP, ze, CAB_TOP, x, 2)
        for i in range(doors):
            m.rect(face, door, ze + i * dl, BASE_TOP, ze + (i + 1) * dl, CAB_TOP, x)
        m.bands(face, rtucoil, zc, BASE_TOP, cz1, CAB_TOP, x, 2)
    m.rect("west", reg("parts", "rating"), ze + 1.5, 21.5, ze + 5.5, 23.5, cx0 - 0.05)

    # the roof, and the condenser fans in their tiles
    m.rect("up", PANEL, cx0, cz0, cx1, zc, CAB_TOP)
    tile_l = lc / fans_z
    r = min(tile_w, tile_l) * 0.4
    for i in range(fans_x):
        for j in range(fans_z):
            tx0, tz0 = cx0 + i * tile_w, zc + j * tile_l
            fan(m, tx0 + tile_w / 2, tz0 + tile_l / 2, r,
                (tx0, tz0, tx0 + tile_w, tz0 + tile_l), CAB_TOP, 3.5)

    # the electrical disconnect on the west side, by the condenser section
    m.box(cx0 - 1.2, 12, zc - 5.5, cx0, 19, zc - 1.5, sw("steel"),
          ("west", "north", "south", "up", "down"), per={"west": reg("parts", "disconnect")})
    # the gas heat section's flue hood on the east side, and the gas riser beside it
    fz = ze + dl * 0.5
    m.box(cx1, 15, fz - 2, cx1 + 1.2, 20, fz + 2, sw("galv"),
          ("east", "north", "south", "up", "down"), per={"east": reg("parts", "flue")})
    gz = ze + dl * 0.15
    m.box(cx1 + 0.6, 0, gz, cx1 + 1.3, 11.3, gz + 0.7, sw("gas"), ("east", "north", "south", "up"))
    m.box(cx1, 10.6, gz, cx1 + 0.6, 11.3, gz + 0.7, sw("gas"), ("north", "south", "up", "down"))
    return m


def rtu_standard():
    """The original rooftop unit's footprint: x -9..25, z -16..32 about its one block."""
    return rtu(-9, 25, -16, 32, 2, 1, 2)


# The large unit: three blocks wide (the master's column in the middle), five long, two tall.
LARGE = {"x": (-1, 1), "y": (0, 1), "z": (0, 4)}


def rtu_large():
    return rtu(-16, 32, 0, 80, 2, 2, 3)


# --------------------------------------------------------------------------------------------
# The PTAC and its wall sleeve grille: one block each, against a wall at the back (z 16)
# --------------------------------------------------------------------------------------------

PTAC_BOX = (0.25, 1.0, 9.75, 15.75, 8.0, 16.0)
GRILLE_BOX = (0.25, 1.0, 15.0, 15.75, 8.0, 16.0)


def ptac():
    m = Mesh()
    x0, y0, z0, x1, y1, z1 = PTAC_BOX
    m.box(1.25, 0, 10.75, 14.75, y0, 15.9, sw("rubber"), ("north", "east", "west"))
    m.box(x0, y0, z0, x1, y1, z1, PANEL, ("north", "east", "west", "up", "down"),
          per={"north": reg("finish", "ptac_front"), "up": reg("finish", "ptac_top")})
    # the control door on top at the unit's right, a little raised, wearing its own part of
    # the top picture
    top = reg("finish", "ptac_top")
    bounds = (x0, z0, x1, z1)
    dx0, dx1, dz0, dz1 = 0.75, 4.1, 10.55, 15.2
    m.rect("up", top, dx0, dz0, dx1, dz1, y1 + 0.25, bounds)
    m.box(dx0, y1, dz0, dx1, y1 + 0.25, dz1, PANEL, SIDES)
    return m


def wall_grille():
    m = Mesh()
    x0, y0, z0, x1, y1, z1 = GRILLE_BOX
    m.box(x0, y0, z0, x1, y1, z1, sw("alu"), ("north", "east", "west", "up", "down"),
          per={"north": reg("parts", "sleeve")})
    return m


# --------------------------------------------------------------------------------------------
# Slicing a model into its cells, and fitting one to an item slot
# --------------------------------------------------------------------------------------------

def clip(pts, uvs, axis, val, keep_above):
    out_p, out_u = [], []
    n = len(pts)
    for i in range(n):
        p, q = pts[i], pts[(i + 1) % n]
        a, b = uvs[i], uvs[(i + 1) % n]
        pin = p[axis] >= val - 1e-9 if keep_above else p[axis] <= val + 1e-9
        qin = q[axis] >= val - 1e-9 if keep_above else q[axis] <= val + 1e-9
        if pin:
            out_p.append(p)
            out_u.append(a)
        if pin != qin:
            t = (val - p[axis]) / (q[axis] - p[axis])
            out_p.append(tuple(p[k] + t * (q[k] - p[k]) for k in range(3)))
            out_u.append(tuple(a[k] + t * (b[k] - a[k]) for k in range(2)))
    return out_p, out_u


def slice_mesh(mesh, lo, hi):
    """The part of a mesh inside the box lo..hi, moved so lo is the origin. A polygon lying in
    one of the box's planes belongs to the cell it faces out of, so a face on the line between
    two cells is drawn by one of them only."""
    out = Mesh()
    for mat, pts, uvs, n in mesh.polys:
        skip = False
        for ax in range(3):
            vals = [p[ax] for p in pts]
            if max(vals) < lo[ax] - 1e-9 or min(vals) > hi[ax] + 1e-9:
                skip = True
                break
            if max(vals) - min(vals) < 1e-9:
                v = vals[0]
                if abs(v - lo[ax]) < 1e-9 and n[ax] > 0:
                    skip = True
                if abs(v - hi[ax]) < 1e-9 and n[ax] < 0:
                    skip = True
        if skip:
            continue
        p, u = list(pts), list(uvs)
        for ax in range(3):
            p, u = clip(p, u, ax, lo[ax], True)
            if len(p) < 3:
                break
            p, u = clip(p, u, ax, hi[ax], False)
            if len(p) < 3:
                break
        if len(p) < 3:
            continue
        p = [tuple(v[k] - lo[k] for k in range(3)) for v in p]
        if area(p) < 1e-6:
            continue
        out.poly(mat, p, u, n)
    return out


def transformed(mesh, scale, offset):
    out = Mesh()
    for mat, pts, uvs, n in mesh.polys:
        out.poly(mat, [tuple(v[k] * scale + offset[k] for k in range(3)) for v in pts], uvs, n)
    return out


def extent(mesh):
    lo = [min(p[k] for _, pts, _, _ in mesh.polys for p in pts) for k in range(3)]
    hi = [max(p[k] for _, pts, _, _ in mesh.polys for p in pts) for k in range(3)]
    return lo, hi


def for_item(mesh):
    """A model larger than its block, shrunk to fit one block on the floor, centred, for the
    item (whose display transform is Forge's default block one)."""
    lo, hi = extent(mesh)
    size = max(hi[k] - lo[k] for k in range(3))
    s = 15.0 / size
    off = [8 - s * (lo[0] + hi[0]) / 2, 0.5 - s * lo[1], 8 - s * (lo[2] + hi[2]) / 2]
    return transformed(mesh, s, off)


# --------------------------------------------------------------------------------------------
# OBJ and MTL
# --------------------------------------------------------------------------------------------

def area(pts):
    n = [0.0, 0.0, 0.0]
    for i in range(1, len(pts) - 1):
        a, b, c = pts[0], pts[i], pts[i + 1]
        u = [b[k] - a[k] for k in range(3)]
        v = [c[k] - a[k] for k in range(3)]
        n[0] += u[1] * v[2] - u[2] * v[1]
        n[1] += u[2] * v[0] - u[0] * v[2]
        n[2] += u[0] * v[1] - u[1] * v[0]
    return 0.5 * math.sqrt(sum(k * k for k in n))


def split(pts):
    """A convex polygon as quads (and a last triangle): Forge's OBJ loader takes no more."""
    n = len(pts)
    if n <= 4:
        return [list(range(n))]
    out, i = [], 1
    while i < n - 1:
        if i + 2 < n:
            out.append([0, i, i + 1, i + 2])
            i += 2
        else:
            out.append([0, i, i + 1])
            i += 1
    return out


def obj_text(mesh, header):
    v, vt, vn = {}, {}, {}
    lines_v, lines_t, lines_n, faces = [], [], [], []

    def idx(store, lines, key, fmt):
        if key not in store:
            store[key] = len(store) + 1
            lines.append(fmt % key)
        return store[key]

    for mat, pts, uvs, n in sorted(mesh.polys, key=lambda p: p[0]):
        ni = idx(vn, lines_n, tuple(round(k, 5) for k in n), "vn %.5f %.5f %.5f")
        for f in split(pts):
            sub = [pts[i] for i in f]
            if area(sub) < 1e-6:
                continue
            corners = []
            for i in f:
                p = tuple(round(pts[i][k] / 16.0, 5) for k in range(3))
                u, w = uvs[i]
                t = (round(min(1.0, max(0.0, u)), 5), round(min(1.0, max(0.0, 1.0 - w)), 5))
                corners.append("%d/%d/%d" % (idx(v, lines_v, p, "v %.5f %.5f %.5f"),
                                             idx(vt, lines_t, t, "vt %.5f %.5f"), ni))
            faces.append((mat, "f " + " ".join(corners)))
    out = ["# Generated by %s -- do not edit" % GEN, "# %s" % header, "mtllib %s" % MTL_NAME,
           "o unit"]
    out += lines_v + lines_t + lines_n
    cur = None
    for mat, f in faces:
        if mat != cur:
            out.append("usemtl %s" % mat)
            cur = mat
        out.append(f)
    return "\n".join(out) + "\n", len(faces)


def mtl_text():
    return "\n".join([
        "# Generated by %s -- do not edit" % GEN,
        "# Two materials for every HVAC unit; each blockstate retextures #finish.",
        "newmtl finish", "map_Kd %s" % (TEX_REF % "finish_sandstone"), "",
        "newmtl parts", "map_Kd %s" % (TEX_REF % "parts"), ""])


# --------------------------------------------------------------------------------------------
# The multi-block's cells
# --------------------------------------------------------------------------------------------

def cells(spec):
    """Every cell but the master's, y then z then x: the order of the part's ``cell`` values."""
    out = []
    for y in range(spec["y"][0], spec["y"][1] + 1):
        for z in range(spec["z"][0], spec["z"][1] + 1):
            for x in range(spec["x"][0], spec["x"][1] + 1):
                if (x, y, z) != (0, 0, 0):
                    out.append((x, y, z))
    return out


def cell_name(prefix, c):
    def s(v):
        return ("m%d" % -v) if v < 0 else str(v)
    return "%s_x%s_y%s_z%s" % (prefix, s(c[0]), s(c[1]), s(c[2]))


def cell_mesh(mesh, c):
    lo = (c[0] * 16, c[1] * 16, c[2] * 16)
    return slice_mesh(mesh, lo, (lo[0] + 16, lo[1] + 16, lo[2] + 16))


# --------------------------------------------------------------------------------------------
# Blockstates, lang
# --------------------------------------------------------------------------------------------

FACING4 = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
# the old units were NSEWUD; one placed up or down stands upright, facing north
FACING6 = dict(FACING4, up={}, down={})
INVENTORY = [{"transform": "forge:default-block"}]


def tex(finish):
    return {"#finish": TEX_REF % ("finish_" + finish), "#parts": TEX_REF % "parts"}


def obj_state(model, finish, facings, ambient=True, inventory_model=None):
    custom = {"flip-v": True}
    if not ambient:
        custom["ambient"] = False
    inv = {"transform": "forge:default-block"}
    if inventory_model:
        inv = {"model": OBJ_REF % inventory_model, "transform": "forge:default-block"}
    return {"forge_marker": 1,
            "defaults": {"model": OBJ_REF % model, "custom": custom, "textures": tex(finish)},
            "variants": {"facing": facings, "inventory": [inv]}}


def part_state(prefix, spec):
    cellmap = {str(i): {"model": OBJ_REF % cell_name(prefix, c)}
               for i, c in enumerate(cells(spec))}
    cellmap[str(len(cells(spec)))] = {"model": MODEL_REF % "unit_part_none"}
    return {"forge_marker": 1,
            "defaults": {"model": MODEL_REF % "unit_part_none", "custom": {"flip-v": True},
                         "textures": tex("sandstone")},
            "variants": {"facing": FACING4,
                         "finish": {f: {"textures": tex(f)} for f in CABINET_FINISHES},
                         "cell": cellmap,
                         "inventory": [{"model": OBJ_REF % (prefix + "_item"),
                                        "transform": "forge:default-block"}]}}


def suffix(finish):
    return "" if finish == "sandstone" else "_" + finish


BLOCKS = {}   # registry: (java, blockstate, (en, de, es, sv) or None to leave the lang alone)
for f in CABINET_FINISHES:
    for kind in ("heater", "cooler"):
        BLOCKS["hvac_%s%s" % (kind, suffix(f))] = (None, obj_state("condenser", f, FACING6), None)
for f in CABINET_FINISHES:
    for kind in ("heater", "cooler"):
        BLOCKS["hvac_rtu_%s%s" % (kind, suffix(f))] = (
            None, obj_state("rtu", f, FACING6, ambient=False, inventory_model="rtu_item"), None)
FIN_NAMES = {"sandstone": ("", "", "", ""),
             "black": (" (Black)", " (Schwarz)", " (Negro)", " (Svart)"),
             "silver": (" (Silver)", " (Silber)", " (Plateado)", " (Silver)")}
for f in CABINET_FINISHES:
    n = FIN_NAMES[f]
    BLOCKS["hvac_rtu_large" + suffix(f)] = (
        'new BlockHvacPackagedRtu("hvac_rtu_large%s")' % suffix(f),
        obj_state(cell_name("rtu_large", (0, 0, 0)), f, FACING4, inventory_model="rtu_large_item"),
        ("Large Rooftop Unit" + n[0], "Großes Dachklimagerät" + n[1],
         "Unidad de azotea grande" + n[2], "Stort takaggregat" + n[3]))
BLOCKS["hvac_rtu_large_part"] = (
    'new BlockHvacPackagedRtuPart("hvac_rtu_large_part")', part_state("rtu_large", LARGE),
    ("Large Rooftop Unit", "Großes Dachklimagerät", "Unidad de azotea grande",
     "Stort takaggregat"))
BLOCKS["hvac_ptac"] = (
    'new BlockHvacPtac("hvac_ptac")', obj_state("ptac", "offwhite", FACING4),
    ("PTAC Unit (Heat and Cool)", "PTAC-Klimagerät (Heizen und Kühlen)",
     "Unidad PTAC (calor y frío)", "PTAC-aggregat (värme och kyla)"))
BLOCKS["hvac_ptac_almond"] = (
    'new BlockHvacPtac("hvac_ptac_almond")', obj_state("ptac", "almond", FACING4),
    ("PTAC Unit (Heat and Cool, Almond)", "PTAC-Klimagerät (Heizen und Kühlen, Mandel)",
     "Unidad PTAC (calor y frío, almendra)", "PTAC-aggregat (värme och kyla, mandel)"))
BLOCKS["hvac_ptac_wall_grille"] = (
    "new BlockHvacPtacWallGrille()", obj_state("ptac_wall_grille", "offwhite", FACING4),
    ("PTAC Wall Sleeve Grille", "PTAC-Wandhülsengitter", "Rejilla exterior de PTAC",
     "PTAC-väggaller"))

LOCALES = ("en_us", "de_de", "es_es", "sv_se")
LANG_ANCHOR = "tile.hvac_rtu_heater_silver.name="
HIDDEN = ("hvac_rtu_large_part",)


def lang_file(loc):
    """The lang file with each key set: a key already there keeps its place, new keys go after
    the rooftop units. The file's own line endings are kept."""
    path = os.path.join(LANG_DIR, loc + ".lang")
    raw = open(path, encoding="utf-8", newline="").read()
    eol = "\r\n" if "\r\n" in raw else "\n"
    lines = raw.replace("\r\n", "\n").split("\n")
    i = LOCALES.index(loc)
    anchor = next(n for n, ln in enumerate(lines) if ln.startswith(LANG_ANCHOR))
    for regname, b in BLOCKS.items():
        if b[2] is None:
            continue
        key = "tile.%s.name" % regname
        line = "%s=%s" % (key, b[2][i])
        at = [n for n, ln in enumerate(lines) if ln.startswith(key + "=")]
        if at:
            lines[at[0]] = line
            anchor = max(anchor, at[0])
            continue
        anchor += 1
        lines.insert(anchor, line)
    return path, eol.join(lines).encode("utf-8")


# --------------------------------------------------------------------------------------------
# Outputs
# --------------------------------------------------------------------------------------------

def models():
    """name -> (mesh, header)."""
    out = {
        "condenser": (condenser(), "Condensing unit / heat pump (hvac_heater, hvac_cooler)"),
        "rtu": (rtu_standard(), "Packaged rooftop unit, drawn from its one block"),
        "rtu_item": (for_item(rtu_standard()), "Packaged rooftop unit, shrunk for its item"),
        "ptac": (ptac(), "PTAC, against a wall at the back"),
        "ptac_wall_grille": (wall_grille(), "PTAC wall sleeve grille, on the outside face"),
    }
    large = rtu_large()
    out["rtu_large_item"] = (for_item(large), "Large rooftop unit, shrunk for its item")
    for c in [(0, 0, 0)] + cells(LARGE):
        out[cell_name("rtu_large", c)] = (cell_mesh(large, c),
                                         "Large rooftop unit, the cell at %d, %d, %d" % c)
    return out


def png(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def js(d):
    return (json.dumps(d, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


_SHEETS = {}


def sheets():
    if not _SHEETS:
        _SHEETS["parts"] = parts_sheet()
        for f in FINISHES:
            _SHEETS["finish_" + f] = finish_sheet(f)
    return _SHEETS


def outputs():
    out = {}
    for name, img in sheets().items():
        out[os.path.join(TEX_DIR, name + ".png")] = png(img)
    out[os.path.join(MODEL_DIR, MTL_NAME)] = mtl_text().encode("utf-8")
    for name, (mesh, header) in models().items():
        text, _ = obj_text(mesh, header)
        out[os.path.join(MODEL_DIR, name + ".obj")] = text.encode("utf-8")
    out[os.path.join(MODEL_DIR, "unit_part_none.json")] = js(
        {"textures": {"particle": TEX_REF % "finish_sandstone"}, "elements": []})
    for regname, b in BLOCKS.items():
        out[os.path.join(STATE_DIR, regname + ".json")] = js(b[1])
    for loc in LOCALES:
        path, data = lang_file(loc)
        out[path] = data
    return out


def report():
    total = 0
    for name, (mesh, _) in models().items():
        _, q = obj_text(mesh, "")
        if "_x" not in name:
            print("%-28s %4d quads" % (name, q))
        total += q
    cells_q = sum(obj_text(m, "")[1] for n, (m, _) in models().items() if "_x" in n)
    print("%-28s %4d quads over %d cells" % ("rtu_large (all cells)", cells_q,
                                            len(cells(LARGE)) + 1))


def fragments():
    print("# lang (en_us)")
    for regname, b in BLOCKS.items():
        if b[2]:
            print("tile.%s.name=%s" % (regname, b[2][0]))
    print("# CsmTabHvac")
    for regname, b in BLOCKS.items():
        if b[0] and regname not in HIDDEN:
            print("    initTabBlock(%s); // %s" % (b[0], b[2][0]))
    print("# CsmTabHvacHidden")
    for regname in HIDDEN:
        print("    initTabBlock(%s);" % BLOCKS[regname][0])


# --------------------------------------------------------------------------------------------
# Preview contact sheet (offline, through preview_block_model's rasteriser)
# --------------------------------------------------------------------------------------------

def preview_quads(mesh):
    import numpy as np
    quads = []
    for mat, pts, uvs, n in mesh.polys:
        for f in split(pts):
            for k in range(1, len(f) - 1):
                tri = [f[0], f[k], f[k + 1]]
                p = np.array([pts[i] for i in tri], dtype=float)
                u = np.array([uvs[i] for i in tri], dtype=float)
                quads.append((np.vstack([p, p[2]]), np.vstack([u, u[2]]), np.array(n),
                              "#" + mat))
    return quads


def render_mesh(mesh, finish, yaw=-38.0, pitch=-24.0, size=360):
    import numpy as np
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import preview_block_model as pbm
    lo, hi = extent(mesh)
    span = max(hi[k] - lo[k] for k in range(3))
    s = 15.0 / span
    off = [8 - s * (lo[k] + hi[k]) / 2 for k in range(3)]
    fitted = transformed(mesh, s, off)
    sh = sheets()
    textures = {"#finish": np.asarray(sh["finish_" + finish].convert("RGBA"), dtype=np.float32) / 255,
                "#parts": np.asarray(sh["parts"].convert("RGBA"), dtype=np.float32) / 255}
    return pbm.render(preview_quads(fitted), textures, size=size, yaw=yaw, pitch=pitch,
                      scale=0.95, background=(0.62, 0.70, 0.78))


def old_cube(tex_png, box_px):
    """The old look, for the 'before' column: a box wearing one flat colour texture."""
    m = Mesh()
    x0, y0, z0, x1, y1, z1 = box_px
    m.box(x0, y0, z0, x1, y1, z1, ("old", 0, 0, 64, 64), ALL)
    return m, tex_png


def sheet(path, old_dir=None):
    import numpy as np
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import preview_block_model as pbm
    from PIL import ImageFont
    rows = []
    large = rtu_large()
    items = [("Condensing unit", condenser(), CABINET_FINISHES, "hvac_cooler",
              (0, 0, 0, 16, 16, 16)),
             ("Rooftop unit", rtu_standard(), CABINET_FINISHES, "hvac_rtu_cooler",
              (-9, 0, -16, 25, 32, 32)),
             ("Large rooftop unit", large, CABINET_FINISHES, None, None),
             ("PTAC", ptac(), PTAC_FINISHES, None, None),
             ("PTAC wall grille", wall_grille(), ("offwhite",), None, None)]
    cell = 300
    for label, mesh, finishes, old, oldbox in items:
        imgs = []
        if old and old_dir:
            o = Image.open(os.path.join(old_dir, old + ".png")).convert("RGBA").resize((64, 64),
                                                                                        Image.NEAREST)
            m, _ = old_cube(o, oldbox)
            lo, hi = extent(m)
            span = max(hi[k] - lo[k] for k in range(3))
            s = 15.0 / span
            fitted = transformed(m, s, [8 - s * (lo[k] + hi[k]) / 2 for k in range(3)])
            arr = np.asarray(o, dtype=np.float32) / 255
            imgs.append(("before", pbm.render(preview_quads(fitted), {"#old": arr}, size=cell,
                                              scale=0.95, background=(0.62, 0.70, 0.78))))
        for f in finishes:
            imgs.append((f, render_mesh(mesh, f, size=cell)))
        imgs.append(("back", render_mesh(mesh, finishes[0], yaw=142, pitch=-24, size=cell)))
        imgs.append(("above", render_mesh(mesh, finishes[0], yaw=-20, pitch=-62, size=cell)))
        rows.append((label, imgs))
    cols = max(len(r[1]) for r in rows)
    out = Image.new("RGB", (cols * cell, len(rows) * (cell + 18)), (40, 40, 44))
    d = ImageDraw.Draw(out)
    for ri, (label, imgs) in enumerate(rows):
        for ci, (cap, im) in enumerate(imgs):
            out.paste(im, (ci * cell, ri * (cell + 18) + 18))
            d.text((ci * cell + 4, ri * (cell + 18) + 3), "%s: %s" % (label, cap),
                   fill=(230, 230, 230))
    out.save(path)
    print("wrote", path)


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    ap.add_argument("--report", action="store_true")
    ap.add_argument("--sheet", metavar="PNG")
    ap.add_argument("--old", metavar="DIR", help="the old flat textures, for --sheet's before")
    a = ap.parse_args()
    if a.fragments:
        fragments()
        return 0
    if a.report:
        report()
        return 0
    if a.sheet:
        sheet(a.sheet, a.old)
        return 0
    out = outputs()
    if a.check:
        bad = []
        for p, data in out.items():
            have = open(p, "rb").read() if os.path.exists(p) else None
            if p.endswith(".png") and have is not None:
                same = Image.open(io.BytesIO(have)).convert("RGBA").tobytes() == \
                    Image.open(io.BytesIO(data)).convert("RGBA").tobytes()
                if not same:
                    bad.append(os.path.relpath(p, REPO))
                continue
            if have is not None:
                have, data = have.replace(b"\r\n", b"\n"), data.replace(b"\r\n", b"\n")
            if have != data:
                bad.append(os.path.relpath(p, REPO))
        if bad:
            print("gen_hvac_units.py: %d file(s) differ:\n  %s" % (len(bad), "\n  ".join(bad)))
            return 1
        print("gen_hvac_units.py: up to date")
        return 0
    for p, data in out.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        if os.path.exists(p) and not p.endswith(".png"):
            have = open(p, "rb").read()
            if have.replace(b"\r\n", b"\n") == data.replace(b"\r\n", b"\n"):
                continue
        with open(p, "wb") as fh:
            fh.write(data)
    print("wrote %d files" % len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
