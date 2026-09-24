#!/usr/bin/env python3
"""Every asset the Streetscape tab's covers ship: manholes, utility vault lids, valve boxes, the
sewer cleanout, drainage grates and the storm drain marker.

    python dev-env-utils/scripts/gen_streetscape_covers.py
    python dev-env-utils/scripts/gen_streetscape_covers.py --check
    python dev-env-utils/scripts/gen_streetscape_covers.py --fragments   # tab lines to paste

All of them are BlockStreetCover (one class, constructed by registry name) in the Roads module.

A cover is ONE upward face lying a fraction of a pixel above the surface below, carrying a cutout
texture. That is what makes a round cover round: drawn as elements it would be a polygon, and an
octagon at this size reads as an octagon. Its box is a sliver, so it is walked over, and the block
settles onto sloped and partial-height road surfaces like the work zone devices.

Every texture is drawn as the player who placed the cover sees it -- legend upright, the far side
at the top -- and then turned 180 degrees, because the blockstate's "north" (no rotation) is the
cover facing a player standing north of it, who looks at the texture from its bottom edge.

Textures are 64 px a block. A cast legend in the shared 3 x 5 font is then about the size a real
one is on a 24 inch cover; at 32 px ELECTRIC would not fit on the lid at all.

The rusted blocks are the same drawing with rust worked into it (recesses first, then streaks),
seeded by name so --check is stable. The fiber optic handhole (polymer concrete) and the marker
(an enamelled plaque) have none.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = lc.REPO
ASSETS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm")
C = lc.Catalogue("gen_streetscape_covers.py", "streetscape", "streetscape", assets=ASSETS)

SIZE = 64
MID = SIZE / 2.0
FACE_Y = 0.2      # the cover's face, in pixels above the surface: clear of it, never floating
BOX_TOP = 1.0     # the box, in pixels: a sliver to walk over and a target to click

IRON = (72, 71, 70)
IRON_HI = (112, 110, 106)
IRON_LO = (44, 43, 42)
LETTER = (178, 174, 166)      # cast lettering: worn bright by traffic
LETTER_SHADOW = (26, 25, 24)
HOLE = (16, 15, 14)
CONCRETE = (160, 157, 150)
POLYMER = (124, 124, 120)
ENAMEL = (32, 88, 158)
WHITE = (236, 236, 232)
RUST = (138, 74, 36)
RUST_DARK = (92, 50, 28)
CLEAR = (0, 0, 0, 0)


# ------------------------------------------------------------------------------------------
# Drawing
# ------------------------------------------------------------------------------------------
def blank():
    return Image.new("RGBA", (SIZE, SIZE), CLEAR)


def inside(x, y, r, cx=MID, cy=MID):
    return (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r


def disc(img, r, colour, cx=MID, cy=MID):
    lc.disc(img, cx, cy, r, colour)


def grain(img, seed, amount=5):
    """A little per-texel noise over every opaque texel, so cast iron is not dead flat."""
    rng = random.Random(seed)
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b, a = px[x, y]
            d = rng.uniform(-amount, amount)
            if a:
                px[x, y] = lc.clamp((r + d, g + d, b + d)) + (a,)


def raised_text(img, text, cx, y, colour=LETTER, shadow=LETTER_SHADOW):
    """Cast lettering: the letters in a lit colour with a one-texel shadow below-right."""
    lc.draw_text_centred(img, text, cx + 1, y + 1, shadow)
    lc.draw_text_centred(img, text, cx, y, colour)


def assert_fits_disc(text, y, r, cx=MID, cy=MID):
    """A text row (5 texels tall from y) must lie inside the disc of radius r."""
    w = lc.text_width(text)
    x0 = int(round(cx - w / 2.0))
    for yy in (y, y + 4):
        for xx in (x0, x0 + w - 1):
            assert inside(xx, yy, r, cx, cy), "%r does not fit at y=%d" % (text, y)


def studs(img, r, colour=IRON_HI, cx=MID, cy=MID, pitch=4):
    """The raised waffle of a cast cover: a stud in every cell of a pitch-texel grid."""
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            if inside(x, y, r, cx, cy) and x % pitch in (1, 2) and y % pitch in (1, 2):
                px[x, y] = colour + (255,)


def manhole(legend, seed):
    img = blank()
    disc(img, 24, (60, 59, 58))           # frame
    disc(img, 21.8, HOLE)                  # the gap between frame and cover
    disc(img, 21, IRON)                    # cover
    px = img.load()
    for y in range(SIZE):                  # a raised ring just inside the edge
        for x in range(SIZE):
            if inside(x, y, 20) and not inside(x, y, 19):
                px[x, y] = IRON_HI + (255,)
    studs(img, 17.5)
    if legend:
        lc.rect(img, 12, 27, 52, 38, lc.shade(IRON, 0.85))   # a plain band for the lettering
        lc.rect(img, 12, 27, 52, 28, IRON_LO)
        lc.rect(img, 12, 37, 52, 38, IRON_HI)
        assert_fits_disc(legend, 30, 19)
        raised_text(img, legend, MID, 30)
    lc.rect(img, 31, 14, 33, 16, HOLE)       # pick holes
    lc.rect(img, 31, 48, 33, 50, HOLE)
    grain(img, seed)
    return img


def rect_box(img, x0, y0, x1, y1, lid, rim=3):
    """A concrete box of the given lid rectangle, the box's rim rim texels wide around it."""
    lc.rect(img, x0 - rim, y0 - rim, x1 + rim, y1 + rim, CONCRETE)
    lc.frame(img, x0 - 1, y0 - 1, x1 + 1, y1 + 1, (70, 70, 68))   # the lid's seam
    lc.rect(img, x0, y0, x1, y1, lid)


def diamond_plate(img, x0, y0, x1, y1):
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            if (x + 2 * y) % 6 == 0 and (x - 2 * y) % 6 != 0:
                px[x, y] = IRON_HI + (255,)


def pull_box(lines, x0, y0, x1, y1, seed):
    img = blank()
    rect_box(img, x0, y0, x1, y1, IRON)
    diamond_plate(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1)
    total = len(lines) * 7 - 2
    ty = int(round((y0 + y1) / 2.0 - total / 2.0))
    lc.rect(img, x0 + 2, ty - 2, x1 - 2, ty + total + 2, lc.shade(IRON, 0.85))
    for i, line in enumerate(lines):
        assert lc.text_width(line) <= x1 - x0 - 4, line
        raised_text(img, line, (x0 + x1) / 2.0, ty + i * 7)
    cx = (x0 + x1) // 2                                  # lifting slots, clear of the legend
    lc.rect(img, cx - 2, y0 + 1, cx + 2, y0 + 2, HOLE)
    lc.rect(img, cx - 2, y1 - 2, cx + 2, y1 - 1, HOLE)
    grain(img, seed)
    return img


def handhole(lines, x0, y0, x1, y1, seed):
    """Polymer concrete: a grey speckled lid with bolts at the corners and moulded lettering."""
    img = blank()
    rect_box(img, x0, y0, x1, y1, POLYMER)
    rng = random.Random(seed)
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            if rng.random() < 0.12:
                px[x, y] = lc.shade(POLYMER, rng.choice((0.8, 1.2))) + (255,)
    total = len(lines) * 7 - 2
    ty = int(round((y0 + y1) / 2.0 - total / 2.0))
    for i, line in enumerate(lines):
        assert lc.text_width(line) <= x1 - x0 - 8, line
        raised_text(img, line, (x0 + x1) / 2.0, ty + i * 7, lc.shade(POLYMER, 0.42),
                    lc.shade(POLYMER, 1.3))
    for bx, by in ((x0 + 2, y0 + 2), (x1 - 4, y0 + 2), (x0 + 2, y1 - 4), (x1 - 4, y1 - 4)):
        lc.rect(img, bx, by, bx + 2, by + 2, (64, 64, 62))
    grain(img, seed, 3)
    return img


def meter_box(seed):
    img = pull_box(["WATER", "METER"], 10, 18, 54, 46, seed)
    disc(img, 4, IRON_LO, 48, 24)             # the reading lid, in a corner clear of the legend
    disc(img, 3, IRON, 48, 24)
    lc.rect(img, 47, 23, 49, 25, HOLE)
    return img


def valve_box(legend, seed, collar=12, lid=8):
    img = blank()
    disc(img, collar, CONCRETE)
    disc(img, lid + 1, IRON_LO)
    disc(img, lid, IRON)
    assert_fits_disc(legend, 30, lid)
    raised_text(img, legend, MID, 30)
    grain(img, seed)
    return img


def catch_basin(seed):
    img = blank()
    lc.rect(img, 12, 12, 52, 52, IRON_LO)            # frame
    lc.frame(img, 12, 12, 52, 52, (60, 59, 58), 2)
    lc.rect(img, 15, 15, 49, 49, IRON)               # grate
    px = img.load()
    for y in range(16, 48):
        for x in range(16, 48):
            if (x - y) % 6 in (0, 1, 2):             # diagonal, bicycle-safe vanes
                px[x, y] = HOLE + (255,)
    lc.rect(img, 15, 31, 49, 33, IRON)               # the stiffening bars
    lc.rect(img, 31, 15, 33, 49, IRON)
    grain(img, seed)
    return img


def trench_drain(seed):
    """A strip the whole width of the block, so trench drains laid end to end are one drain. The
    slot pitch (4) divides the block, so the joint between two blocks falls on the pattern."""
    img = blank()
    lc.rect(img, 0, 22, SIZE, 42, IRON_LO)
    lc.rect(img, 0, 24, SIZE, 40, IRON)
    px = img.load()
    for y in range(25, 39):
        for x in range(SIZE):
            if x % 4 in (1, 2):
                px[x, y] = HOLE + (255,)
    grain(img, seed)
    return img


def gutter_inlet(seed):
    """A long grate at the far edge of the block, against the curb, with its slots running across
    the flow of the gutter as a bicycle-safe grate's do."""
    img = blank()
    lc.rect(img, 4, 2, 60, 28, IRON_LO)
    lc.rect(img, 6, 4, 58, 26, IRON)
    px = img.load()
    for y in range(5, 25):
        for x in range(7, 57):
            if x % 4 in (1, 2):
                px[x, y] = HOLE + (255,)
    lc.rect(img, 6, 14, 58, 16, IRON)
    grain(img, seed)
    return img


def marker(seed):
    img = blank()
    disc(img, 21, (200, 200, 196))           # the plaque's edge
    disc(img, 20, ENAMEL)
    px = img.load()
    rows = [("NO", 17), ("DUMPING", 24), ("DRAINS TO", 31), ("RIVER", 38)]
    for text, y in rows:
        assert_fits_disc(text, y, 18)
        lc.draw_text_centred(img, text, MID, y, WHITE)
    for row in (45, 48):                       # waves
        for x in range(24, 41):
            if inside(x, row, 18):
                px[x, row + (1 if (x // 3) % 2 else 0)] = WHITE + (255,)
    grain(img, seed, 3)
    return img


def rusted(img, seed):
    """Rust worked into a cast cover: most in the recesses (the darkest texels), then in blotches
    from a coarse value noise, then a few streaks. The alpha is left exactly as it was."""
    rng = random.Random(seed)
    lattice = 8
    n = SIZE // lattice + 2
    field = [[rng.random() for _ in range(n)] for _ in range(n)]

    def noise(x, y):
        fx, fy = x / float(lattice), y / float(lattice)
        ix, iy = int(fx), int(fy)
        tx, ty = fx - ix, fy - iy
        a = field[iy][ix] * (1 - tx) + field[iy][ix + 1] * tx
        b = field[iy + 1][ix] * (1 - tx) + field[iy + 1][ix + 1] * tx
        return a * (1 - ty) + b * ty

    out = img.copy()
    px = out.load()
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b, a = px[x, y]
            if not a:
                continue
            lum = (r + g + b) / 3.0
            k = max(0.0, noise(x, y) - 0.35) * 1.4
            if lum < 40:
                k += 0.35
            k = min(0.85, k)
            tint = RUST_DARK if lum < 60 else RUST
            px[x, y] = lc.clamp(tuple(v * (1 - k) + t * k for v, t in zip((r, g, b), tint))) + (a,)
    for _ in range(6):
        x = rng.randrange(SIZE)
        y = rng.randrange(SIZE)
        for step in range(rng.randrange(4, 12)):
            yy = y + step
            if yy < SIZE and px[x, yy][3]:
                r, g, b, a = px[x, yy]
                px[x, yy] = lc.clamp(tuple(v * 0.5 + t * 0.5
                                           for v, t in zip((r, g, b), RUST))) + (a,)
    return out


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def placed(img):
    """The drawing as the placing player sees it, turned to the blockstate's north (see the
    module docstring)."""
    return img.rotate(180)


def footprint(img):
    """The block-space box of the texture's opaque texels, the cover's box facing north."""
    x0, y0, x1, y1 = img.getbbox()
    k = 1.0 / SIZE
    return (round(x0 * k, 6), 0.0, round(y0 * k, 6), round(x1 * k, 6), BOX_TOP / 16.0,
            round(y1 * k, 6))


def cover_model(tex):
    return lc.model({"cover": C.T(tex), "particle": C.T(tex)}, [
        {"from": [0, FACE_Y, 0], "to": [16, FACE_Y, 16],
         "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#cover"}}}], ao=False)


def cover_state(tex):
    return {"forge_marker": 1, "defaults": {"model": C.M(tex)},
            "variants": {"facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                                    "west": {"y": 270}}}}


def cover_item(tex):
    """The inventory icon: the cover's own texture as a flat item, turned back upright. The
    texture is stored turned 180 degrees for the block (see the module docstring), so without
    this every legend in the inventory reads upside down. The rotations are item/generated's
    own display values with a half turn about the view axis added."""
    return {"parent": "item/generated", "textures": {"layer0": C.T(tex)},
            "display": {
                "gui": {"rotation": [0, 0, 180]},
                "fixed": {"rotation": [0, 180, 180]},
                "ground": {"rotation": [0, 0, 180], "translation": [0, 2, 0],
                           "scale": [0.5, 0.5, 0.5]}}}


def add_cover(registry, draw, names, rusted_names=None):
    """A cover and, when rusted_names is given, its rusted twin."""
    seed = sum(ord(c) * (i + 1) for i, c in enumerate(registry))
    clean = placed(draw(seed))
    variants = [(registry, clean, names)]
    if rusted_names:
        variants.append((registry + "_rusted", rusted(clean, seed + 7), rusted_names))
    for reg, img, nm in variants:
        C.texture(reg)(lambda img=img: img)
        box = footprint(img)
        java = 'new BlockStreetCover("%s", new AxisAlignedBB(%s))' % (
            reg, ", ".join("%.6f" % v for v in box))
        C.add(reg, java, nm, {reg: cover_model(reg)}, cover_state(reg),
              item=cover_item(reg),
              tab="CsmTabStreetscape")


def manholes():
    kinds = [
        ("sewer", "SEWER", ("Sewer", "Abwasser", "Alcantarillado", "Avlopp")),
        ("storm", "STORM", ("Storm", "Regenwasser", "Pluvial", "Dagvatten")),
        ("water", "WATER", ("Water", "Wasser", "Agua", "Vatten")),
        ("electric", "ELECTRIC", ("Electric", "Strom", "Eléctrica", "El")),
        ("telecom", "TELECOM", ("Telecom", "Telekom", "Telecomunicaciones", "Tele")),
        ("gas", "GAS", ("Gas", "Gas", "Gas", "Gas")),
        ("plain", None, ("Plain", "Glatt", "Lisa", "Slät")),
    ]
    for key, legend, (en, de, es, sv) in kinds:
        add_cover("manhole_" + key, lambda seed, legend=legend: manhole(legend, seed),
                  ("Manhole Cover (%s)" % en, "Kanaldeckel (%s)" % de,
                   "Tapa de Registro (%s)" % es, "Brunnslock (%s)" % sv),
                  ("Manhole Cover (%s, Rusted)" % en, "Kanaldeckel (%s, rostig)" % de,
                   "Tapa de Registro (%s, Oxidada)" % es, "Brunnslock (%s, Rostigt)" % sv))


def vault_lids():
    add_cover("vault_lid_electric",
              lambda seed: pull_box(["ELECTRIC"], 12, 21, 52, 43, seed),
              ("Electric Pull Box", "Kabelziehschacht (Strom)", "Caja de Registro Eléctrica",
               "Kabelbrunn (El)"),
              ("Electric Pull Box (Rusted)", "Kabelziehschacht (Strom, rostig)",
               "Caja de Registro Eléctrica (Oxidada)", "Kabelbrunn (El, Rostig)"))
    add_cover("vault_lid_traffic_signal",
              lambda seed: pull_box(["TRAFFIC", "SIGNAL"], 10, 18, 54, 46, seed),
              ("Traffic Signal Pull Box", "Kabelziehschacht (Ampelanlage)",
               "Caja de Registro de Semáforos", "Kabelbrunn (Trafiksignal)"),
              ("Traffic Signal Pull Box (Rusted)", "Kabelziehschacht (Ampelanlage, rostig)",
               "Caja de Registro de Semáforos (Oxidada)", "Kabelbrunn (Trafiksignal, Rostig)"))
    add_cover("vault_lid_fiber_optic",
              lambda seed: handhole(["FIBER", "OPTIC"], 8, 14, 56, 50, seed),
              ("Fiber Optic Handhole", "Glasfaser-Handloch", "Arqueta de Fibra Óptica",
               "Fiberbrunn"))
    add_cover("vault_lid_water_meter", meter_box,
              ("Water Meter Box", "Wasserzählerschacht", "Caja de Medidor de Agua",
               "Vattenmätarbrunn"),
              ("Water Meter Box (Rusted)", "Wasserzählerschacht (rostig)",
               "Caja de Medidor de Agua (Oxidada)", "Vattenmätarbrunn (Rostig)"))


def valve_boxes():
    add_cover("valve_box_water", lambda seed: valve_box("W", seed),
              ("Water Valve Box", "Wasserschieberkappe", "Caja de Válvula de Agua",
               "Vattenventilbetäckning"),
              ("Water Valve Box (Rusted)", "Wasserschieberkappe (rostig)",
               "Caja de Válvula de Agua (Oxidada)", "Vattenventilbetäckning (Rostig)"))
    add_cover("valve_box_gas", lambda seed: valve_box("GAS", seed),
              ("Gas Valve Box", "Gasschieberkappe", "Caja de Válvula de Gas",
               "Gasventilbetäckning"),
              ("Gas Valve Box (Rusted)", "Gasschieberkappe (rostig)",
               "Caja de Válvula de Gas (Oxidada)", "Gasventilbetäckning (Rostig)"))
    add_cover("sewer_cleanout", lambda seed: valve_box("CO", seed, collar=10, lid=6),
              ("Sewer Cleanout", "Reinigungsöffnung (Abwasser)",
               "Registro de Limpieza de Alcantarillado", "Rensbrunn (Avlopp)"),
              ("Sewer Cleanout (Rusted)", "Reinigungsöffnung (Abwasser, rostig)",
               "Registro de Limpieza de Alcantarillado (Oxidado)", "Rensbrunn (Avlopp, Rostig)"))


def drainage():
    add_cover("catch_basin_grate", catch_basin,
              ("Catch Basin Grate", "Straßenablauf (Rost)", "Rejilla de Sumidero",
               "Dagvattenbrunn (Galler)"),
              ("Catch Basin Grate (Rusted)", "Straßenablauf (Rost, rostig)",
               "Rejilla de Sumidero (Oxidada)", "Dagvattenbrunn (Galler, Rostigt)"))
    add_cover("trench_drain", trench_drain,
              ("Trench Drain", "Entwässerungsrinne", "Canaleta de Drenaje", "Dräneringsränna"),
              ("Trench Drain (Rusted)", "Entwässerungsrinne (rostig)",
               "Canaleta de Drenaje (Oxidada)", "Dräneringsränna (Rostig)"))
    add_cover("gutter_inlet_grate", gutter_inlet,
              ("Gutter Inlet Grate", "Rinnenablauf (Rost)", "Rejilla de Imbornal",
               "Rännstensbrunn (Galler)"),
              ("Gutter Inlet Grate (Rusted)", "Rinnenablauf (Rost, rostig)",
               "Rejilla de Imbornal (Oxidada)", "Rännstensbrunn (Galler, Rostigt)"))
    add_cover("storm_drain_marker", marker,
              ("Storm Drain Marker", "Gully-Markierung", "Placa de Alcantarilla Pluvial",
               "Dagvattenmärke"))


manholes()
vault_lids()
valve_boxes()
drainage()

if __name__ == "__main__":
    sys.exit(C.main())
