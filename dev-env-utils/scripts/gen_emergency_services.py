#!/usr/bin/env python3
"""
gen_emergency_services.py -- the Life Safety module's Emergency Services tab.

What goes in a fire station, a police station and an ambulance station, and the community
warning and dispatch equipment around them. No vehicles. Grown a station at a time:

  * fire station: turnout gear lockers, SCBA racks, the cylinder cascade and fill station, hose
    racks, rolls and drying rack, the nozzle rack and tool board, the gear extractor and air
    compressor, the brass gong (BlockStationBell), the fire pole and the hole it passes through
    (BlockFirePole, BlockFirePoleHole: you slide down), the Maltese cross and the station number
    plaque (BlockStationNumberPlaque, 0-99 by clicking).

Emblems are generic (the Maltese cross, the Star of Life, a plain police star); no real agency's
name, patch or badge is drawn here.

Writes, under the Life Safety module's assets/csm:

  * textures/blocks/lifesafety/services/*.png
  * models/block/lifesafety/services/*.json
  * blockstates/<registry>.json
  * the tile names in all four lang files

Usage:
    python gen_emergency_services.py              # write everything
    python gen_emergency_services.py --check      # fail if the tree has drifted
    python gen_emergency_services.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from life_safety_gen_common import (  # noqa: E402
    Catalogue, bevel, box, clamp, disc, draw_text, draw_text_centred, face, facing_state, fill,
    frame, model, north_region, pipe_x, pipe_z, post, rect, shade, text_width)

C = Catalogue("gen_emergency_services.py", "lifesafety/services", "lifesafety/services")
T = C.T
M = C.M
ES = "Emergency Services"

RED = (178, 28, 34)
STEEL = (128, 132, 138)
DARK_STEEL = (70, 74, 80)
BRASS = (204, 164, 78)
TAN = (176, 150, 96)
BLACK = (32, 32, 34)
YELLOW = (230, 184, 30)
LIME = (190, 220, 40)
WHITE = (232, 232, 226)
SILVER = (206, 210, 214)
GREEN = (60, 120, 70)
WOOD = (150, 106, 62)
CONCRETE = (150, 150, 146)


def B(*v):
    return "new int[]{%s}" % ", ".join("%g" % x for x in v)


def flat(name, colour, grain=4, seed=1, size=16):
    C.textures[name] = lambda: fill(colour, size=size, grain=grain, seed=seed)


# Only colours a model uses are written: an unused texture fails the integrity check.
for i, (n, c) in enumerate((("red", RED), ("steel", STEEL), ("dark_steel", DARK_STEEL),
                            ("brass", BRASS), ("black", BLACK), ("yellow", YELLOW),
                            ("silver", SILVER), ("wood", WOOD))):
    flat(n, c, seed=100 + i)


# ==========================================================================================
# Textures
# ==========================================================================================
@C.texture("mesh")
def _mesh():
    """Expanded-metal locker sides: a diamond grid, the gaps clear (cutout)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                px[x, y] = shade(DARK_STEEL, 1.0 + ((x * 7 + y * 3) % 5) * 0.03) + (255,)
    return img


@C.texture("turnout")
def _turnout():
    """Turnout gear fabric: tan with the lime-and-silver reflective trim bands."""
    img = fill(TAN, grain=6, seed=201)
    for y0 in (4, 11):
        rect(img, 0, y0, 16, y0 + 1, LIME)
        rect(img, 0, y0 + 1, 16, y0 + 2, (220, 222, 226))
        rect(img, 0, y0 + 2, 16, y0 + 3, LIME)
    return img


@C.texture("helmet")
def _helmet():
    img = fill(YELLOW, grain=4, seed=202)
    rect(img, 6, 0, 10, 16, shade(YELLOW, 1.15))
    rect(img, 0, 13, 16, 16, shade(YELLOW, 0.75))
    return img


@C.texture("helmet_front")
def _helmet_front():
    img = fill(YELLOW, grain=4, seed=203)
    # the leather front shield
    rect(img, 5, 3, 11, 10, (120, 40, 30))
    frame(img, 5, 3, 11, 10, (80, 24, 18))
    draw_text_centred(img, "1", 8, 4, (240, 220, 180))
    return img


@C.texture("boots")
def _boots():
    img = fill(BLACK, grain=4, seed=204)
    rect(img, 0, 12, 16, 16, (20, 20, 22))
    rect(img, 0, 5, 16, 6, LIME)
    return img


@C.texture("scba_cyl")
def _scba_cyl():
    img = fill((196, 198, 204), grain=4, seed=205)
    rect(img, 0, 2, 16, 3, (40, 40, 44))
    rect(img, 0, 9, 16, 11, LIME)
    return img


@C.texture("cascade_cyl")
def _cascade_cyl():
    img = fill((60, 120, 90), grain=4, seed=206)
    rect(img, 0, 1, 16, 3, (230, 230, 230))
    return img


@C.texture("hose_folds")
def _hose_folds():
    img = fill((196, 50, 44), grain=6, seed=207)
    for y in range(16):
        k = 0.7 + 0.45 * math.sin((y % 4) / 4.0 * math.pi)
        rect(img, 0, y, 16, y + 1, shade((196, 50, 44), k))
    return img


@C.texture("hose_roll_end")
def _hose_roll_end():
    """A rolled hose seen end on: a spiral of flat hose round a small core."""
    img = fill((150, 30, 28), grain=4, seed=208)
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            a = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            ring = (d + a / (2 * math.pi) * 1.6) % 1.6
            if d < 1.6:
                px[x, y] = (40, 40, 40, 255)
            elif ring < 0.45:
                px[x, y] = shade((150, 30, 28), 0.6) + (255,)
            else:
                px[x, y] = shade((200, 48, 42), 1.0 - d * 0.02) + (255,)
    return img


@C.texture("pegboard")
def _pegboard():
    img = fill((170, 140, 100), grain=4, seed=209)
    for y in range(1, 16, 3):
        for x in range(1, 16, 3):
            img.load()[x, y] = (90, 70, 50, 255)
    return img


@C.texture("axe_head")
def _axe_head():
    img = fill(RED, grain=3, seed=210)
    rect(img, 0, 0, 4, 16, (210, 214, 220))
    return img


@C.texture("washer_door")
def _washer_door():
    img = fill(SILVER, grain=3, seed=211)
    disc(img, 8, 8, 7, (90, 94, 100))
    disc(img, 8, 8, 5.5, (40, 60, 80))
    disc(img, 6.5, 6.5, 1.5, (120, 150, 180))
    return img


@C.texture("washer_panel")
def _washer_panel():
    img = fill(WHITE, grain=2, seed=212)
    rect(img, 2, 2, 9, 6, (40, 60, 40))
    draw_text(img, "88", 3, 2 - 1 + 1, (140, 220, 120))
    disc(img, 12, 4, 1.8, (120, 120, 124))
    return img


@C.texture("fill_front")
def _fill_front():
    """The SCBA fill station's front: the fill chamber's window, gauges and the legend."""
    img = fill(YELLOW, size=64, grain=3, seed=213)
    x0, y0, x1, y1 = north_region((1, 0), (15, 16), 64)
    frame(img, x0, y0, x1, y1, shade(YELLOW, 0.7), 2)
    draw_text_centred(img, "SCBA FILL", 32, 6, BLACK)
    rect(img, x0 + 8, 16, x1 - 8, 44, (50, 56, 62))
    rect(img, x0 + 10, 18, x1 - 10, 42, (90, 110, 128))
    for i, gx in enumerate((18, 32, 46)):
        disc(img, gx, 52, 4.5, (240, 240, 236))
        disc(img, gx, 52, 3.5, (250, 250, 246))
        img.load()[gx, 50] = (200, 30, 30, 255)
    return img


@C.texture("gauge")
def _gauge():
    img = fill(WHITE, grain=1, seed=214)
    disc(img, 8, 8, 7, (30, 30, 30))
    disc(img, 8, 8, 6, (246, 246, 242))
    for k in range(5):
        img.load()[8 + k // 2, 8 - k] = (200, 20, 20, 255)
    return img


@C.texture("maltese")
def _maltese():
    """A Maltese cross, red with a gold rim, clear round it (cutout). A generic emblem: no
    department's name or number is on it."""
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0

    def inside(x, y, grow):
        u, v = x - c, y - c
        au, av = abs(u), abs(v)
        r = max(au, av)
        # four flared arms, notched at their ends, meeting at the centre
        if r > 29 + grow:
            return False
        lo, hi = min(au, av), r
        width = 3 + hi * 0.55 + grow
        notch = hi > 22 and lo < (hi - 22) * 0.9
        return lo < width and not notch

    for y in range(size):
        for x in range(size):
            if inside(x + 0.5, y + 0.5, 0):
                px[x, y] = RED + (255,)
            elif inside(x + 0.5, y + 0.5, 2):
                px[x, y] = BRASS + (255,)
    disc(img, c, c, 9, BRASS)
    disc(img, c, c, 7.5, RED)
    return img


@C.texture("plaque")
def _plaque():
    img = fill(RED, size=64, grain=3, seed=215)
    x0, y0, x1, y1 = north_region((2, 2), (14, 14), 64)
    frame(img, x0, y0, x1, y1, BRASS, 2)
    draw_text_centred(img, "STATION", 32, y0 + 5, BRASS)
    return img


def digit(n):
    """One digit of the plaque's number: brass on clear, filling the texture."""
    def draw():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        draw_text(img, str(n), 2, 0, BRASS, scale=3)
        return img
    return draw


for n in range(10):
    C.textures["digit_%d" % n] = digit(n)


@C.texture("station_floor")
def _station_floor():
    img = fill((120, 124, 126), grain=5, seed=216)
    for y in range(0, 16, 4):
        for x in range(16):
            if (x // 2 + y // 4) % 2 == 0:
                img.load()[x, y] = shade((120, 124, 126), 1.15) + (255,)
    return img


@C.texture("compressor_tank")
def _compressor_tank():
    img = fill(RED, grain=3, seed=217)
    rect(img, 0, 7, 16, 8, shade(RED, 1.2))
    return img


# ==========================================================================================
# Blocks: fire station
# ==========================================================================================
def prop(reg, box6, collides, names, textures, elements, extra_models=None, state=None):
    models = {reg: model(textures, elements)}
    models.update(extra_models or {})
    C.add(reg, 'new BlockFireProtectionProp("%s", %s, %s)' % (reg, B(*box6),
                                                              "true" if collides else "false"),
          names, models, state or facing_state(M(reg)), tab=ES)


def helmet(cx, y, cz):
    return (post(cx, cz, 3.2, y, y + 0.6, "helmet")
            + post(cx, cz, 2.2, y + 0.6, y + 3, "helmet", bottom=False)
            + [box([cx - 1.5, y + 0.6, cz - 3.3], [cx + 1.5, y + 3.2, cz - 2.2], "helmet",
                   per={"north": "helmet_front"})])


def locker(stocked):
    els = [box([0, 0, 15], [16, 16, 16], "steel"),
           box([0, 0, 2], [1, 16, 15], "mesh"), box([15, 0, 2], [16, 16, 15], "mesh"),
           box([1, 12, 2], [15, 12.5, 15], "steel"),
           box([1, 15.5, 2], [15, 16, 15], "steel"),
           box([1, 0, 2], [15, 0.5, 15], "steel")]
    if stocked:
        els += helmet(8, 12.5, 9)
        # coat on its hook, then pants folded down over the boots
        els += [box([7.5, 11, 13.5], [8.5, 12, 15], "steel"),
                box([3, 5.5, 8.5], [13, 11.5, 14.5], "turnout"),
                box([3.5, 2.5, 9], [12.5, 5.5, 14], "turnout"),
                box([3.5, 0.5, 7.5], [7.5, 3, 13.5], "boots"),
                box([8.5, 0.5, 7.5], [12.5, 3, 13.5], "boots")]
    return els


LOCKER_TEX = {"steel": T("dark_steel"), "mesh": T("mesh"), "helmet": T("helmet"),
              "helmet_front": T("helmet_front"), "turnout": T("turnout"), "boots": T("boots"),
              "particle": T("dark_steel")}
prop("turnout_gear_locker", (0, 0, 2, 16, 16, 16), True,
     ("Turnout Gear Locker", "Einsatzkleidungsspind", "Taquilla de equipo de intervención",
      "Skåp för insatsställ"), LOCKER_TEX, locker(True))
prop("turnout_gear_locker_empty", (0, 0, 2, 16, 16, 16), True,
     ("Turnout Gear Locker (Empty)", "Einsatzkleidungsspind (leer)",
      "Taquilla de equipo de intervención (vacía)", "Skåp för insatsställ (tomt)"),
     LOCKER_TEX, locker(False))


def scba(cx, cz):
    return (post(cx, cz, 1.8, 2, 11, "cyl")
            + post(cx, cz, 0.5, 11, 12, "silver", bottom=False)
            + [box([cx - 2.2, 3, cz + 1.6], [cx + 2.2, 12, cz + 2.4], "black"),
               box([cx - 2.4, 12, cz - 1.5], [cx + 2.4, 13, cz + 2.4], "black"),
               box([cx - 1.6, 12.5, cz - 2.6], [cx + 1.6, 15, cz - 1.2], "black")])


prop("scba_wall_rack", (1, 1, 9, 15, 15, 16), True,
     ("SCBA Wall Rack", "Atemschutzgeräte-Wandhalterung",
      "Soporte de pared para equipos de respiración", "Väggställ för andningsskydd"),
     {"cyl": T("scba_cyl"), "silver": T("silver"), "black": T("black"), "steel": T("steel"),
      "particle": T("scba_cyl")},
     [box([1, 1, 15.5], [15, 2, 16], "steel"), box([1, 13.5, 15.5], [15, 14.5, 16], "steel")]
     + scba(4.5, 12.5) + scba(11.5, 12.5))

prop("scba_cylinder_cascade", (0, 0, 3, 16, 16, 13), True,
     ("SCBA Cylinder Cascade", "Atemluft-Flaschenbündel", "Cascada de cilindros de aire",
      "Luftflaskpaket för andningsskydd"),
     {"cyl": T("cascade_cyl"), "brass": T("brass"), "steel": T("steel"),
      "particle": T("cascade_cyl")},
     post(3, 8, 2.6, 0, 14, "cyl") + post(8, 8, 2.6, 0, 14, "cyl") + post(13, 8, 2.6, 0, 14, "cyl")
     + post(3, 8, 0.7, 14, 15, "brass", bottom=False)
     + post(8, 8, 0.7, 14, 15, "brass", bottom=False)
     + post(13, 8, 0.7, 14, 15, "brass", bottom=False)
     + pipe_x(15.2, 8, 0.4, 2.5, 13.5, "brass")
     + [box([0.5, 4, 5], [15.5, 5, 5.4], "steel"), box([0.5, 4, 10.6], [15.5, 5, 11], "steel")])

prop("scba_fill_station", (1, 0, 3, 15, 16, 16), True,
     ("SCBA Fill Station", "Atemluft-Füllstation", "Estación de llenado de equipos de respiración",
      "Fyllstation för andningsskydd"),
     {"y": T("yellow"), "front": T("fill_front"), "particle": T("yellow")},
     [box([1, 0, 3], [15, 16, 16], "y", per={"north": "front"})])


# --- hose ---------------------------------------------------------------------------------------
prop("hose_rack_wall", (2, 2, 9, 14, 14, 16), False,
     ("Wall Hose Rack (Loaded)", "Schlauchhalter (bestückt)", "Soporte de manguera de pared (cargado)",
      "Slanghållare på vägg (laddad)"),
     {"steel": T("steel"), "folds": T("hose_folds"), "brass": T("brass"), "particle": T("steel")},
     [box([7, 12, 12], [9, 14, 16], "steel"), box([2, 12, 11], [14, 13, 13], "steel"),
      box([2.5, 3, 9.5], [13.5, 12, 14], "folds")]
     + pipe_z(4, 3, 1, 8, 9.5, "brass"))

prop("hose_rolls", (1, 0, 3, 15, 9, 13), True,
     ("Rolled Fire Hose", "Gerollte Feuerwehrschläuche", "Mangueras enrolladas",
      "Rullade brandslangar"),
     {"side": T("hose_folds"), "end": T("hose_roll_end"), "particle": T("hose_roll_end")},
     [dict(e, faces={f: (face("end", v.get("uv")) if f in ("east", "west") else v)
                     for f, v in e["faces"].items()})
      for x0 in (1.5, 5.5, 9.5)
      for e in pipe_x(4.2, 8, 4.2, x0, x0 + 3.4, "side")])

prop("hose_drying_rack", (0, 0, 5, 16, 16, 11), True,
     ("Hose Drying Rack", "Schlauchtrockengestell", "Secadero de mangueras",
      "Torkställning för slangar"),
     {"steel": T("steel"), "folds": T("hose_folds"), "particle": T("steel")},
     [box([0.5, 0, 7.5], [1.5, 16, 8.5], "steel"), box([14.5, 0, 7.5], [15.5, 16, 8.5], "steel"),
      box([0.5, 15, 7], [15.5, 16, 9], "steel")]
     + [box([x, 1, 7.3], [x + 1.6, 15, 8.7], "folds") for x in (2.5, 5, 7.5, 10, 12.5)])

prop("nozzle_rack", (1, 3, 11, 15, 13, 16), False,
     ("Nozzle Rack", "Strahlrohr-Halterung", "Soporte de lanzas", "Ställ för strålrör"),
     {"steel": T("steel"), "chrome": T("silver"), "black": T("black"), "particle": T("steel")},
     [box([1, 9, 15], [15, 10, 16], "steel")]
     + pipe_z(4, 8, 1.3, 12, 15.2, "chrome") + pipe_z(4, 8, 1.0, 10.5, 12, "black")
     + pipe_z(8, 8, 1.3, 12, 15.2, "chrome") + pipe_z(8, 8, 1.0, 10.5, 12, "black")
     + pipe_z(12, 8, 1.3, 12, 15.2, "chrome") + pipe_z(12, 8, 1.0, 10.5, 12, "black"))

prop("fire_tool_board", (0, 0, 13, 16, 16, 16), False,
     ("Firefighting Tool Board", "Werkzeugwand der Feuerwache", "Panel de herramientas de bomberos",
      "Verktygstavla för räddningstjänsten"),
     {"peg": T("pegboard"), "wood": T("wood"), "axe": T("axe_head"), "steel": T("steel"),
      "yellow": T("yellow"), "red": T("red"), "particle": T("pegboard")},
     [box([0, 0, 15.5], [16, 16, 16], "peg"),
      # flat-head axe: handle and head
      box([2.5, 2, 14.5], [3.5, 13, 15.5], "wood"), box([1, 11, 14.3], [5, 14, 15.5], "axe"),
      # halligan bar: shaft, adze and fork
      box([7.3, 1, 14.5], [8.2, 14, 15.5], "steel"), box([6, 13, 14.3], [9.5, 14, 15.5], "steel"),
      box([6.8, 1, 14.3], [8.7, 2, 15.5], "steel"),
      # pike pole, full height
      box([11.2, 0, 14.6], [11.9, 16, 15.4], "yellow"), box([10.8, 14, 14.4], [12.3, 16, 15.5], "steel"),
      # bolt cutters
      box([13.5, 2, 14.6], [14.1, 9, 15.4], "red"), box([14.5, 2, 14.6], [15.1, 9, 15.4], "red"),
      box([13.6, 9, 14.5], [15, 11.5, 15.5], "steel")])

prop("gear_extractor", (1, 0, 2, 15, 15, 16), True,
     ("Turnout Gear Extractor", "Waschmaschine für Einsatzkleidung",
      "Lavadora extractora de equipo de intervención", "Tvättmaskin för insatsställ"),
     {"silver": T("silver"), "door": T("washer_door"), "panel": T("washer_panel"),
      "particle": T("silver")},
     [box([1, 0, 3], [15, 15, 16], "silver"),
      box([2, 1, 2.5], [14, 12, 3], "silver", per={"north": "door"}),
      box([1, 12, 2.6], [15, 15, 3], "silver", per={"north": "panel"})])

prop("air_compressor", (0, 0, 3, 16, 13, 13), True,
     ("Air Compressor", "Druckluftkompressor", "Compresor de aire", "Luftkompressor"),
     {"tank": T("compressor_tank"), "steel": T("dark_steel"), "black": T("black"),
      "gauge": T("gauge"), "particle": T("compressor_tank")},
     [box([1.5, 0, 6], [3, 2, 10], "black"), box([13, 0, 6], [14.5, 2, 10], "black")]
     + pipe_x(5.5, 8, 3.6, 0.5, 15.5, "tank")
     + [box([3, 9, 5], [9, 13, 11], "steel"), box([10, 9, 6], [14, 12, 10], "steel"),
        box([10.5, 9.5, 5.5], [13, 12, 6], "steel", per={"north": "gauge"})])


# --- the gong, the pole, emblems -------------------------------------------------------------
C.add("station_alarm_gong",
      'new BlockStationBell("station_alarm_gong", %s)' % B(3, 3, 10, 13, 13, 16),
      ("Firehouse Alarm Gong", "Alarmglocke der Feuerwache", "Campana de alarma del cuartel",
       "Larmklocka på brandstationen"),
      {"station_alarm_gong": model(
          {"brass": T("brass"), "wood": T("wood"), "steel": T("dark_steel"), "particle": T("brass")},
          [box([4, 3, 15], [12, 13, 16], "wood"),
           box([7.5, 7.5, 13], [8.5, 8.5, 15], "steel")]
          + pipe_z(8, 8, 4.8, 11.5, 13, "brass")
          + pipe_z(8, 8, 1.6, 10.5, 11.5, "brass")
          + [box([11, 3.5, 12], [12, 11, 13], "steel"), box([9.5, 3.5, 11], [12, 4.5, 12], "steel")])},
      facing_state(M("station_alarm_gong"), {"powered": {"false": {}, "true": {}}}), tab=ES)

POLE_TEX = {"brass": T("brass"), "floor": T("station_floor"), "particle": T("brass")}
C.add("fire_pole", "new BlockFirePole()",
      ("Fire Pole", "Rutschstange", "Barra de bomberos", "Brandmansstång"),
      {"fire_pole": model(POLE_TEX, post(8, 8, 1.3, 0, 16, "brass", top=False, bottom=False))},
      {"forge_marker": 1, "defaults": {"model": M("fire_pole")},
       "variants": {"normal": [{}], "inventory": [{}]}}, tab=ES)
C.add("fire_pole_hole", "new BlockFirePoleHole()",
      ("Fire Pole Floor Opening", "Rutschstangen-Deckendurchbruch", "Hueco de barra en el suelo",
       "Golvöppning för brandmansstång"),
      {"fire_pole_hole": model(POLE_TEX,
                               [box([0, 0, 0], [3, 16, 16], "floor"),
                                box([13, 0, 0], [16, 16, 16], "floor"),
                                box([3, 0, 0], [13, 16, 3], "floor"),
                                box([3, 0, 13], [13, 16, 16], "floor")]
                               + post(8, 8, 1.3, 0, 16, "brass", top=False, bottom=False))},
      {"forge_marker": 1, "defaults": {"model": M("fire_pole_hole")},
       "variants": {"normal": [{}], "inventory": [{}]}}, tab=ES)

prop("maltese_cross_emblem", (1, 1, 15, 15, 15, 16), False,
     ("Maltese Cross Emblem", "Malteserkreuz-Emblem", "Emblema de cruz de Malta",
      "Maltesiskt kors-emblem"),
     {"cross": T("maltese"), "particle": T("maltese")},
     [box([1, 1, 15.5], [15, 15, 16], "cross", faces=("north", "south"))])
# the whole texture on the plate, not the window box() would take
C.blocks[-1]["models"]["maltese_cross_emblem"]["elements"][0]["faces"]["north"]["uv"] = [0, 0, 16, 16]
C.blocks[-1]["models"]["maltese_cross_emblem"]["elements"][0]["faces"]["south"]["uv"] = [16, 0, 0, 16]


def number_plaque():
    reg = "station_number_plaque"
    tex = {"plaque": T("plaque"), "tens": T("digit_0"), "ones": T("digit_1"),
           "red": T("red"), "particle": T("plaque")}
    tens = box([8.2, 3.5, 15.2], [12.2, 10.5, 15.3], "tens", faces=("north",))
    ones = box([3.8, 3.5, 15.2], [7.8, 10.5, 15.3], "ones", faces=("north",))
    tens["faces"]["north"]["uv"] = [0, 0, 16, 16]
    ones["faces"]["north"]["uv"] = [0, 0, 16, 16]
    m = model(tex, [box([2, 2, 15.3], [14, 14, 16], "red", per={"north": "plaque"}), tens, ones])
    state = facing_state(M(reg), {
        "tens": {str(n): {"textures": {"tens": T("digit_%d" % n)}} for n in range(10)},
        "ones": {str(n): {"textures": {"ones": T("digit_%d" % n)}} for n in range(10)},
    })
    C.add(reg, 'new BlockStationNumberPlaque("%s", %s)' % (reg, B(2, 2, 15, 14, 14, 16)),
          ("Station Number Plaque", "Wachennummernschild", "Placa con número de estación",
           "Stationsnummerskylt"), {reg: m}, state, tab=ES)


number_plaque()


if __name__ == "__main__":
    sys.exit(C.main())
