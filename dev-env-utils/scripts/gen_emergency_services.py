#!/usr/bin/env python3
"""
gen_emergency_services.py -- the Life Safety module's Emergency Services tab.

What goes in a fire station, a police station and an ambulance station, and the community
warning and dispatch equipment around them. No vehicles. Grown a station at a time:

  * fire station: turnout gear lockers, SCBA racks, the cylinder cascade and fill station, hose
    racks, rolls and drying rack, the nozzle rack and tool board, the gear extractor and air
    compressor, the brass gong (BlockStationBell), the fire pole and the hole it passes through
    (BlockFirePole, BlockFirePoleHole: you slide down), the Maltese cross and the station number
    plaque (BlockStationNumberPlaque, 0-99 by clicking); the station alerting controller and
    its speakers, alert lights, relays and bay clearance lights (STATION_ALERTING_SYSTEM.md).
  * police station: the front desk, deal tray and lobby phone, the walk-through metal detector
    (BlockMetalDetector, which beeps at metal), the holding cell's sliding barred door
    (BlockCellDoor), bench and toilet, booking (height chart, fingerprint scanner, camera,
    property bins), evidence and equipment lockers, the K-9 kennel, the blue lamp and police
    star, and police and fire line tape (BlockSceneTape, laid like a fence) with its stanchion.

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

# Also recharges fire extinguishers (BlockScbaFillStation).
C.add("scba_fill_station", 'new BlockScbaFillStation("scba_fill_station", %s)'
      % B(1, 0, 3, 15, 16, 16),
      ("SCBA Fill Station", "Atemluft-Füllstation", "Estación de llenado de equipos de respiración",
       "Fyllstation för andningsskydd"),
      {"scba_fill_station": model({"y": T("yellow"), "front": T("fill_front"),
                                   "particle": T("yellow")},
                                  [box([1, 0, 3], [15, 16, 16], "y", per={"north": "front"})])},
      facing_state(M("scba_fill_station")), tab=ES)


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



# ==========================================================================================
# Station alerting (BlockStationAlertController and its devices)
# ==========================================================================================
@C.texture("alert_panel")
def _alert_panel():
    return alert_panel(False)


@C.texture("alert_panel_active")
def _alert_panel_active():
    return alert_panel(True)


def alert_panel(active):
    img = fill((60, 64, 70), size=64, grain=2, seed=301)
    x0, y0, x1, y1 = north_region((2, 1), (14, 15), 64)
    bevel(img, x0, y0, x1, y1, (60, 64, 70))
    draw_text_centred(img, "STATION", 32, y0 + 4, (220, 220, 220))
    draw_text_centred(img, "ALERTING", 32, y0 + 11, (220, 220, 220))
    disc(img, 32, y0 + 26, 5, (255, 60, 50) if active else (90, 30, 26))
    draw_text_centred(img, "ALERT", 32, y0 + 34, (255, 120, 110) if active else (140, 140, 140))
    # zone keys
    for i in range(5):
        bx = x0 + 5 + i * 8
        rect(img, bx, y1 - 14, bx + 6, y1 - 8, (200, 200, 196))
        rect(img, bx, y1 - 9, bx + 6, y1 - 8, (120, 120, 116))
    return img


@C.texture("speaker_grille")
def _speaker_grille():
    img = fill((220, 220, 216), grain=2, seed=302)
    for y in range(4, 13):
        for x in range(4, 13):
            if (x + y) % 2 == 0 and math.hypot(x + 0.5 - 8.5, y + 0.5 - 8.5) < 4.8:
                img.load()[x, y] = (70, 70, 72, 255)
    return img


def alert_lens(colour, lit):
    def draw():
        base = colour if lit else shade(colour, 0.35)
        img = fill(base, grain=3, seed=303 + lit)
        for x in range(0, 16, 3):
            rect(img, x, 0, x + 1, 16, shade(base, 0.85))
        if lit:
            rect(img, 4, 6, 12, 10, shade(colour, 1.3))
        return img
    return draw


C.textures["alert_red"] = alert_lens((240, 40, 36), False)
C.textures["alert_red_on"] = alert_lens((240, 40, 36), True)
C.textures["alert_white"] = alert_lens((240, 240, 232), False)
C.textures["alert_white_on"] = alert_lens((240, 240, 232), True)


def clearance(green):
    def draw():
        img = fill((34, 34, 36), grain=2, seed=305)
        disc(img, 8, 4.5, 3.2, (255, 40, 30) if not green else (80, 20, 16))
        disc(img, 8, 11.5, 3.2, (40, 230, 90) if green else (16, 60, 26))
        return img
    return draw


C.textures["clearance_stop"] = clearance(False)
C.textures["clearance_go"] = clearance(True)


@C.texture("relay_front")
def _relay_front():
    img = fill((150, 152, 156), grain=2, seed=306)
    draw_text_centred(img, "K1", 8, 3, (40, 40, 40))
    return img


@C.texture("relay_front_on")
def _relay_front_on():
    img = _relay_front()
    disc(img, 8, 11, 1.6, (255, 70, 50))
    return img


def device(reg, kind, box6, names, textures, elements, on_textures):
    state = facing_state(M(reg), {"active": {"false": {}, "true": {"textures": on_textures}}})
    C.add(reg, 'new BlockStationAlertDevice("%s", BlockStationAlertDevice.Kind.%s, %s)'
          % (reg, kind, B(*box6)), names, {reg: model(textures, elements)}, state, tab=ES)


C.add("station_alert_controller",
      'new BlockStationAlertController("station_alert_controller", %s)' % B(2, 1, 11, 14, 15, 16),
      ("Station Alerting Controller", "Alarmierungszentrale der Wache",
       "Controlador de alerta de estación", "Larmcentral för stationslarm"),
      {"station_alert_controller": model(
          {"steel": T("dark_steel"), "front": T("alert_panel"), "particle": T("dark_steel")},
          [box([2, 1, 11], [14, 15, 16], "steel", per={"north": "front"})])},
      facing_state(M("station_alert_controller"),
                   {"active": {"false": {}, "true": {"textures": {"front": T("alert_panel_active")}}},
                    "powered": {"false": {}, "true": {}}}), tab=ES)
device("station_alert_speaker", "SPEAKER", (3, 3, 13, 13, 13, 16),
       ("Station Alerting Speaker", "Alarmierungslautsprecher", "Altavoz de alerta de estación",
        "Högtalare för stationslarm"),
       {"white": T("silver"), "grille": T("speaker_grille"), "particle": T("silver")},
       [box([3, 3, 14], [13, 13, 16], "white", per={"north": "grille"})], {})
for colour, cn in (("red", ("Red", "rot", "roja", "röd")), ("white", ("White", "weiß", "blanca", "vit"))):
    device("station_alert_light_" + colour, "LIGHT", (2, 5, 12, 14, 11, 16),
           ("Station Alert Light (%s)" % cn[0], "Alarmierungsleuchte (%s)" % cn[1],
            "Luz de alerta de estación (%s)" % cn[2], "Larmlampa för station (%s)" % cn[3]),
           {"steel": T("dark_steel"), "lens": T("alert_" + colour), "particle": T("dark_steel")},
           [box([2, 5, 14], [14, 11, 16], "steel"),
            box([2.5, 5.5, 12.5], [13.5, 10.5, 14], "lens")],
           {"lens": T("alert_%s_on" % colour)})
device("station_alert_relay", "RELAY", (5, 4, 13, 11, 12, 16),
       ("Station Alerting Relay", "Alarmierungsrelais", "Relé de alerta de estación",
        "Relä för stationslarm"),
       {"box": T("steel"), "front": T("relay_front"), "particle": T("steel")},
       [box([5, 4, 13], [11, 12, 16], "box", per={"north": "front"})],
       {"front": T("relay_front_on")})
device("bay_clearance_light", "CLEARANCE", (4, 1, 12, 12, 15, 16),
       ("Bay Door Clearance Light", "Hallentor-Ausfahrtampel", "Semáforo de salida de la cochera",
        "Utfartssignal för vagnhallen"),
       {"black": T("black"), "face": T("clearance_stop"), "particle": T("black")},
       [box([4, 1, 13], [12, 15, 16], "black", per={"north": "face"}),
        box([4, 7.5, 11.5], [12, 8, 13], "black"), box([4, 14.5, 11.5], [12, 15, 13], "black")],
       {"face": T("clearance_go")})

for key, names in (
        ("zone", ("[Station alerting] Zone: %s (%s devices linked). Sneak and click to dispatch.",
                  "[Alarmierung] Zone: %s (%s Geräte verbunden). Schleichen und klicken zum Alarmieren.",
                  "[Alerta] Zona: %s (%s dispositivos vinculados). Agáchate y haz clic para despachar.",
                  "[Stationslarm] Zon: %s (%s enheter kopplade). Smyg och klicka för att larma.")),
        ("dispatch", ("[Station alerting] Dispatching %s.", "[Alarmierung] Alarmiere %s.",
                      "[Alerta] Despachando %s.", "[Stationslarm] Larmar %s.")),
        ("reset", ("[Station alerting] Alert reset.", "[Alarmierung] Alarm zurückgesetzt.",
                   "[Alerta] Alerta restablecida.", "[Stationslarm] Larmet återställt.")),
        ("zone.engine", ("Engine", "Löschfahrzeug", "Autobomba", "Släckbil")),
        ("zone.ladder", ("Ladder", "Drehleiter", "Escalera", "Stegbil")),
        ("zone.medic", ("Medic", "Rettungswagen", "Ambulancia", "Ambulans")),
        ("zone.battalion", ("Battalion", "Einsatzleitung", "Jefatura de batallón", "Insatsledare")),
        ("zone.all_call", ("All call", "Vollalarm", "Llamada general", "Allmänt larm"))):
    C.add_lang("csm.lifesafety.station." + key, names)


# ==========================================================================================
# Police station
# ==========================================================================================
NAVY = (36, 48, 84)
POLICE_BLUE = (40, 70, 150)
BEIGE = (206, 196, 170)
flat("navy", NAVY, seed=400)
flat("beige", BEIGE, seed=401)
flat("stainless", (184, 188, 192), grain=6, seed=402)
flat("laminate", (150, 110, 74), grain=5, seed=403)


@C.texture("counter_top")
def _counter_top():
    img = fill((196, 190, 176), grain=6, seed=404)
    rect(img, 0, 0, 16, 1, (150, 146, 136))
    return img


@C.texture("counter_front")
def _counter_front():
    img = fill((150, 110, 74), grain=5, seed=405)
    for x in range(0, 16, 4):
        rect(img, x, 0, x + 1, 16, (126, 90, 60))
    rect(img, 0, 14, 16, 16, (60, 50, 44))
    return img


@C.texture("phone_front")
def _phone_front():
    img = fill((60, 62, 66), grain=2, seed=406)
    x0, y0, x1, y1 = north_region((5, 4), (11, 12), 16)
    for r in range(3):
        for c in range(3):
            rect(img, x0 + 1 + c * 2, y0 + 4 + r, x0 + 2 + c * 2, y0 + 5 + r, (200, 200, 196))
    rect(img, x0 + 1, y0 + 1, x1 - 1, y0 + 3, (150, 180, 140))
    return img


@C.texture("md_panel")
def _md_panel():
    img = fill(BEIGE, grain=3, seed=407)
    for y in range(2, 16, 2):
        rect(img, 4, y, 12, y + 1, shade(BEIGE, 0.8))
    return img


def md_lamp(alarm):
    def draw():
        img = fill((50, 52, 56), grain=2, seed=408)
        disc(img, 8, 8, 3, (255, 40, 30) if alarm else (80, 20, 16))
        disc(img, 8, 8, 1.2, (255, 160, 140) if alarm else (100, 30, 24))
        rect(img, 1, 6, 4, 10, (40, 220, 80) if not alarm else (20, 60, 30))
        rect(img, 12, 6, 15, 10, (40, 220, 80) if not alarm else (20, 60, 30))
        return img
    return draw


C.textures["md_lamp"] = md_lamp(False)
C.textures["md_lamp_alarm"] = md_lamp(True)


@C.texture("height_chart")
def _height_chart():
    img = fill((236, 236, 230), size=64, grain=2, seed=409)
    for y in range(0, 64, 4):
        rect(img, 0, y, 64 if y % 16 == 0 else 10, y + 1, (70, 70, 76))
    for i, label in enumerate(("6-0", "5-6", "5-0", "4-6")):
        draw_text(img, label, 14, i * 16 + 2, (40, 40, 44))
    return img


@C.texture("scanner_top")
def _scanner_top():
    img = fill((40, 42, 46), grain=2, seed=410)
    rect(img, 4, 4, 12, 12, (60, 200, 110))
    rect(img, 5, 5, 11, 11, (120, 240, 170))
    return img


@C.texture("bins")
def _bins():
    img = fill((96, 100, 106), grain=3, seed=411)
    rect(img, 4, 3, 12, 6, (240, 240, 236))
    return img


@C.texture("evidence_front")
def _evidence_front():
    img = fill((150, 154, 160), size=64, grain=3, seed=412)
    for r in range(4):
        for c in range(3):
            x0, y0 = 2 + c * 20, 2 + r * 15
            bevel(img, x0, y0, x0 + 19, y0 + 14, (150, 154, 160))
            draw_text(img, str(r * 3 + c + 1), x0 + 3, y0 + 3, (40, 40, 44))
            rect(img, x0 + 15, y0 + 6, x0 + 17, y0 + 9, (60, 60, 64))
    return img


@C.texture("locker_front")
def _locker_front():
    img = fill(NAVY, grain=3, seed=413)
    rect(img, 7, 0, 9, 16, shade(NAVY, 0.7))
    for x0 in (1, 9):
        for y in (2, 3, 4):
            rect(img, x0 + 1, y, x0 + 5, y + 1, shade(NAVY, 0.6))
        rect(img, x0 + 5, 8, x0 + 6, 11, (170, 174, 180))
    return img


@C.texture("dog_bed")
def _dog_bed():
    img = fill((120, 80, 56), grain=5, seed=414)
    rect(img, 2, 2, 14, 14, (160, 120, 90))
    return img


@C.texture("police_globe")
def _police_globe():
    img = fill((60, 110, 230), grain=3, seed=415)
    rect(img, 0, 5, 16, 11, (250, 250, 250))
    draw_text_centred(img, "POL", 8, 6, (30, 50, 130))
    return img


@C.texture("police_star")
def _police_star():
    """A seven-point star, gold, with a blue disc lettered POLICE: a generic emblem, no
    department's name or seal on it."""
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0
    for y in range(size):
        for x in range(size):
            u, v = x + 0.5 - c, y + 0.5 - c
            r = math.hypot(u, v)
            a = math.atan2(u, -v)
            k = (a / (2 * math.pi / 7)) % 1
            edge = 14 + 16 * (1 - abs(k - 0.5) * 2) ** 2
            if r < edge:
                px[x, y] = (206, 168, 70, 255) if r < edge - 2 else (150, 116, 40, 255)
    disc(img, c, c, 11, POLICE_BLUE)
    draw_text_centred(img, "POLICE", c, c - 2, (240, 230, 190))
    return img


def tape_texture(bg, fg, words):
    def draw():
        img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        rect(img, 0, 22, 64, 28, bg)
        for i, word in enumerate(words):
            draw_text_centred(img, word, 16 + i * 32, 23, fg)
        return img
    return draw


def tape_item(bg):
    def draw():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        disc(img, 8, 8, 6.5, bg)
        disc(img, 8, 8, 3, (0, 0, 0, 0))
        disc(img, 8, 8, 2.2, (150, 150, 150))
        rect(img, 8, 11, 16, 14, bg)
        return img
    return draw


C.textures["tape_police"] = tape_texture((250, 214, 30), (20, 20, 20), ("POLICE", "LINE"))
C.textures["tape_fire"] = tape_texture((200, 30, 30), (250, 250, 250), ("FIRE", "LINE"))
C.textures["tape_police_item"] = tape_item((250, 214, 30))
C.textures["tape_fire_item"] = tape_item((200, 30, 30))

prop("front_desk_counter", (0, 0, 3, 16, 16, 16), True,
     ("Front Desk Counter", "Empfangstheke", "Mostrador de recepción", "Receptionsdisk"),
     {"top": T("counter_top"), "front": T("counter_front"), "side": T("laminate"),
      "particle": T("counter_top")},
     [box([0, 0, 5], [16, 14, 16], "side", per={"north": "front"}),
      box([0, 14, 3], [16, 16, 16], "top")])
prop("pass_through_tray", (3, 0, 2, 13, 3, 14), False,
     ("Pass-Through Deal Tray", "Durchreiche-Schale", "Bandeja pasamonedas", "Kassalucka"),
     {"s": T("stainless"), "particle": T("stainless")},
     [box([3, 0, 2], [13, 0.5, 14], "s"), box([3, 0.5, 2], [3.5, 2.5, 14], "s"),
      box([12.5, 0.5, 2], [13, 2.5, 14], "s")])
prop("lobby_phone", (5, 3, 12, 11, 13, 16), False,
     ("Lobby Courtesy Phone", "Lobby-Telefon", "Teléfono del vestíbulo", "Lobbytelefon"),
     {"body": T("dark_steel"), "front": T("phone_front"), "black": T("black"),
      "particle": T("dark_steel")},
     [box([5, 4, 14], [11, 12, 16], "body", per={"north": "front"}),
      box([10.5, 5, 13], [12, 11.5, 14.5], "black")])

# Two blocks tall: the arch stands up into the block above, which should be left air.
C.add("metal_detector", 'new BlockMetalDetector("metal_detector", %s)' % B(0, 0, 5, 16, 16, 11),
      ("Walk-Through Metal Detector", "Metalldetektor-Torrahmen", "Arco detector de metales",
       "Metalldetektorbåge"),
      {"metal_detector": model(
          {"panel": T("md_panel"), "lamp": T("md_lamp"), "beige": T("beige"),
           "particle": T("beige")},
          [box([0, 0, 5.5], [2, 29, 10.5], "beige", per={"north": "panel", "south": "panel"}),
           box([14, 0, 5.5], [16, 29, 10.5], "beige", per={"north": "panel", "south": "panel"}),
           box([0, 29, 5], [16, 32, 11], "beige", per={"north": "lamp", "south": "lamp"})])},
      facing_state(M("metal_detector"),
                   {"alarm": {"false": {}, "true": {"textures": {"lamp": T("md_lamp_alarm")}}}}),
      tab=ES)
for f in ("north", "south"):
    C.blocks[-1]["models"]["metal_detector"]["elements"][2]["faces"][f]["uv"] = [0, 5, 16, 11]


def bars(dx):
    els = [box([x + dx, 0, 7.5], [x + 1 + dx, 16, 8.5], "steel") for x in range(1, 16, 2)]
    els += [box([0 + dx, y, 7.3], [16 + dx, y + 1, 8.7], "steel") for y in (0.5, 7.5, 14.5)]
    return els


C.add("holding_cell_door", 'new BlockCellDoor("holding_cell_door", %s)' % B(0, 0, 7, 16, 16, 9),
      ("Holding Cell Door", "Zellentür (Gitter)", "Puerta de celda", "Celldörr (galler)"),
      {"holding_cell_door": model({"steel": T("dark_steel"), "particle": T("dark_steel")},
                                  bars(0)),
       "holding_cell_door_open": model({"steel": T("dark_steel"), "particle": T("dark_steel")},
                                       bars(14))},
      facing_state(M("holding_cell_door"),
                   {"open": {"false": {}, "true": {"model": M("holding_cell_door_open")}},
                    "powered": {"false": {}, "true": {}}}), tab=ES)
prop("holding_cell_bench", (0, 5, 8, 16, 8, 16), True,
     ("Holding Cell Bench", "Zellenbank", "Banco de celda", "Cellbänk"),
     {"s": T("stainless"), "particle": T("stainless")},
     [box([0, 6.5, 8], [16, 7.5, 16], "s"), box([1, 4, 14], [2, 6.5, 16], "s"),
      box([14, 4, 14], [15, 6.5, 16], "s")])
# The combination unit is one steel cabinet as deep as the bowl is long, with the bowl out
# front and the basin let into the cabinet's top, whose floor is the
# cabinet's own top face drawn dark.
prop("holding_cell_toilet", (3, 0, 2, 13, 15, 16), True,
     ("Holding Cell Toilet and Sink", "Zellen-WC mit Waschbecken", "Inodoro y lavabo de celda",
      "Celltoalett med tvättställ"),
     {"s": T("stainless"), "black": T("black"), "particle": T("stainless")},
     [box([4, 0, 4], [12, 5, 9], "s"), box([3.5, 5, 2.5], [12.5, 6, 9], "s"),
      box([4.5, 6, 3.5], [11.5, 6.01, 8], "black", faces=("up",)),
      box([3, 0, 9], [13, 13, 16], "s", per={"up": "black"}),
      box([3, 13, 9], [13, 15, 10], "s"), box([3, 13, 15], [13, 15, 16], "s"),
      box([3, 13, 10], [4, 15, 15], "s"), box([12, 13, 10], [13, 15, 15], "s"),
      box([7.5, 15, 13], [8.5, 16, 15], "s"), box([7.5, 15, 11.5], [8.5, 16, 13], "s")])
prop("height_chart", (0, 0, 15, 16, 16, 16), False,
     ("Booking Height Chart", "Messlatte für Erkennungsdienst", "Tabla de estatura para fichaje",
      "Längdskala för registrering"),
     {"chart": T("height_chart"), "particle": T("height_chart")},
     [box([0, 0, 15.5], [16, 16, 16], "chart")])
prop("fingerprint_scanner", (4, 0, 5, 12, 4, 12), False,
     ("Fingerprint Scanner", "Fingerabdruckscanner", "Escáner de huellas", "Fingeravtrycksläsare"),
     {"body": T("black"), "top": T("scanner_top"), "particle": T("black")},
     [box([4, 0, 5], [12, 3, 10], "body", per={"up": "top"}),
      box([4, 0, 10], [12, 4, 12], "body")])
prop("booking_camera", (5, 0, 5, 11, 16, 11), True,
     ("Booking Camera", "Erkennungsdienst-Kamera", "Cámara de fichaje", "Registreringskamera"),
     {"black": T("black"), "steel": T("steel"), "particle": T("black")},
     post(8, 8, 2.5, 0, 0.5, "steel") + post(8, 8, 0.5, 0.5, 12, "steel", bottom=False)
     + [box([5, 12, 6], [11, 16, 11], "black")] + pipe_z(8, 14, 1.4, 4.5, 6, "black"))
prop("property_bins", (0, 0, 6, 16, 16, 16), True,
     ("Property Bin Shelf", "Asservatenregal", "Estante de pertenencias",
      "Hylla för tillhörigheter"),
     {"steel": T("steel"), "bin": T("bins"), "particle": T("steel")},
     [box([0, 0, 6], [16, 1, 16], "steel"), box([0, 5.5, 6], [16, 6.5, 16], "steel"),
      box([0, 11, 6], [16, 12, 16], "steel"), box([0, 15, 6], [16, 16, 16], "steel"),
      box([0, 0, 15.5], [16, 16, 16], "steel")]
     + [box([x, y + 0.2, 7], [x + 7, y + 4.5, 15], "bin")
        for x in (0.5, 8.5) for y in (1, 6.5, 12)])
prop("evidence_locker", (0, 0, 2, 16, 16, 16), True,
     ("Evidence Locker", "Asservatenschließfach", "Taquilla de pruebas", "Beslagsskåp"),
     {"steel": T("steel"), "front": T("evidence_front"), "particle": T("steel")},
     [box([0, 0, 2], [16, 16, 16], "steel", per={"north": "front"})])
prop("equipment_locker", (0, 0, 3, 16, 16, 16), True,
     ("Equipment Locker", "Ausrüstungsspind", "Taquilla de equipo", "Utrustningsskåp"),
     {"navy": T("navy"), "front": T("locker_front"), "particle": T("navy")},
     [box([0, 0, 3], [16, 16, 16], "navy", per={"north": "front"})])
prop("k9_kennel", (0, 0, 0, 16, 12, 16), True,
     ("K-9 Kennel", "Diensthundezwinger", "Perrera K-9", "Hundgård för tjänstehund"),
     {"mesh": T("mesh"), "steel": T("dark_steel"), "bed": T("dog_bed"),
      "particle": T("dark_steel")},
     [box([0, 0, 0], [16, 12, 0.5], "mesh"), box([0, 0, 15.5], [16, 12, 16], "mesh"),
      box([0, 0, 0.5], [0.5, 12, 15.5], "mesh"), box([15.5, 0, 0.5], [16, 12, 15.5], "mesh"),
      box([0, 11.5, 0], [16, 12, 16], "steel"),
      box([0, 0, 0], [1, 12, 1], "steel"), box([15, 0, 0], [16, 12, 1], "steel"),
      box([0, 0, 15], [1, 12, 16], "steel"), box([15, 0, 15], [16, 12, 16], "steel"),
      box([3, 0, 7], [13, 1.5, 14], "bed")])

C.add("police_lamp", 'new BlockLitProp("police_lamp", %s, 14)' % B(4, 2, 4, 12, 15, 16),
      ("Police Station Blue Lamp", "Blaue Polizeilaterne", "Farol azul de comisaría",
       "Blå polislykta"),
      {"police_lamp": model(
          {"globe": T("police_globe"), "black": T("black"), "particle": T("police_globe")},
          [box([7.5, 13, 8], [8.5, 14, 16], "black"), box([6, 12, 14.5], [10, 15, 16], "black")]
          + post(8, 8, 3.2, 3, 11, "globe", top=False, bottom=False)
          + post(8, 8, 3.6, 11, 12.5, "black") + post(8, 8, 1.5, 2, 3, "black"))},
      facing_state(M("police_lamp")), tab=ES)
prop("police_star_emblem", (1, 1, 15, 15, 15, 16), False,
     ("Police Star Emblem", "Polizeistern-Emblem", "Emblema de estrella policial",
      "Polisstjärna-emblem"),
     {"star": T("police_star"), "particle": T("police_star")},
     [box([1, 1, 15.5], [15, 15, 16], "star", faces=("north", "south"))])
C.blocks[-1]["models"]["police_star_emblem"]["elements"][0]["faces"]["north"]["uv"] = [0, 0, 16, 16]
C.blocks[-1]["models"]["police_star_emblem"]["elements"][0]["faces"]["south"]["uv"] = [16, 0, 0, 16]


def tape(reg, tex, names):
    arm = model({"tape": T(tex), "particle": T(tex)},
                [box([7.9, 9, 0], [8.1, 10.5, 8], "tape", faces=("east", "west"))], ao=False)
    single = model({"tape": T(tex), "particle": T(tex)},
                   [box([0, 9, 7.9], [16, 10.5, 8.1], "tape", faces=("north", "south"))],
                   ao=False)
    # Toward a stanchion the tape runs on into its block, to the post's 0.8 px radius; textured
    # as the block beyond, since box() clamps UVs to the cell.
    stub = box([7.9, 9, 8.8], [8.1, 10.5, 16], "tape", faces=("east", "west"))
    stub["from"][2] -= 16
    stub["to"][2] -= 16
    to_post = model({"tape": T(tex), "particle": T(tex)}, [stub], ao=False)
    parts = [{"when": {"north": "none", "east": "none", "south": "none", "west": "none"},
              "apply": {"model": M(reg + "_single")}}]
    for d, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for when, part in (("tape|post", "_arm"), ("post", "_to_post")):
            apply = {"model": M(reg + part)}
            if rot:
                apply["y"] = rot
            parts.append({"when": {d: when}, "apply": apply})
    item = {"parent": "item/generated", "textures": {"layer0": T(tex + "_item")}}
    C.add(reg, 'new BlockSceneTape("%s")' % reg, names,
          {reg + "_arm": arm, reg + "_single": single, reg + "_to_post": to_post},
          {"multipart": parts}, item=item, tab=ES)


tape("police_line_tape", "tape_police",
     ("Police Line Tape", "Polizei-Absperrband", "Cinta de acordonamiento policial",
      "Polisens avspärrningsband"))
tape("fire_line_tape", "tape_fire",
     ("Fire Line Tape", "Feuerwehr-Absperrband", "Cinta de acordonamiento de bomberos",
      "Räddningstjänstens avspärrningsband"))
C.add("tape_stanchion", 'new BlockTapeStanchion("tape_stanchion", %s)' % B(5, 0, 5, 11, 14, 11),
      ("Tape Stanchion", "Absperrpfosten", "Poste para cinta", "Avspärrningsstolpe"),
      {"tape_stanchion": model(
          {"black": T("black"), "yellow": T("yellow"), "particle": T("black")},
          post(8, 8, 2.8, 0, 1, "black") + post(8, 8, 0.8, 1, 12, "black", bottom=False)
          + post(8, 8, 1.2, 12, 14, "yellow"))},
      facing_state(M("tape_stanchion")), tab=ES)


# ==========================================================================================
# Ambulance (EMS) station
# ==========================================================================================
STAR_BLUE = (0, 90, 180)
flat("stretcher_yellow", (236, 190, 30), seed=500)
flat("mattress", (40, 42, 46), seed=501)
flat("backboard", (236, 110, 30), seed=502)
flat("o2_green", (30, 140, 70), seed=503)
flat("wire", (170, 174, 180), grain=3, seed=504)


@C.texture("supply_boxes")
def _supply_boxes():
    img = fill((230, 230, 224), grain=2, seed=505)
    for i, c in enumerate(((60, 120, 200), (230, 230, 230), (200, 60, 60), (80, 170, 90))):
        rect(img, i * 4, 2, i * 4 + 3, 14, c)
        rect(img, i * 4, 6, i * 4 + 3, 8, shade(c, 0.6))
    return img


@C.texture("safe_front")
def _safe_front():
    img = fill((110, 114, 120), grain=3, seed=506)
    x0, y0, x1, y1 = north_region((4, 3), (12, 12), 16)
    bevel(img, x0, y0, x1, y1, (110, 114, 120))
    rect(img, x0 + 1, y0 + 1, x0 + 4, y0 + 5, (30, 30, 32))
    for r in range(3):
        for c in range(2):
            img.load()[x0 + 1 + c * 2, y0 + 1 + r] = (200, 200, 196, 255)
    rect(img, x1 - 2, y0 + 3, x1 - 1, y0 + 6, (190, 194, 200))
    return img


@C.texture("eyewash_sign")
def _eyewash_sign():
    img = fill((0, 140, 70), size=64, grain=2, seed=507)
    draw_text_centred(img, "EYE", 32, 6, WHITE, scale=2)
    draw_text_centred(img, "WASH", 32, 20, WHITE, scale=2)
    draw_text_centred(img, "EMERGENCY", 32, 36, WHITE)
    return img


@C.texture("star_of_life")
def _star_of_life():
    """The Star of Life: a blue six-armed cross with the rod and serpent in white. The symbol is
    a public emergency medical services emblem; nothing here names a service."""
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    c = size / 2.0
    for y in range(size):
        for x in range(size):
            u, v = x + 0.5 - c, y + 0.5 - c
            inside = False
            for k in range(3):
                a = k * math.pi / 3
                along = u * math.sin(a) - v * math.cos(a)
                across = u * math.cos(a) + v * math.sin(a)
                if abs(along) < 29 and abs(across) < 7.5:
                    inside = True
            if inside:
                px[x, y] = STAR_BLUE + (255,)
    # the rod, and the serpent wound round it
    rect(img, 31, 12, 33, 52, (250, 250, 250))
    for i in range(36):
        yy = 14 + i
        xx = int(round(32 + 4.5 * math.sin(i * 0.45)))
        rect(img, xx - 1, yy, xx + 1, yy + 1, (250, 250, 250))
    return img


def zbox(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), per=None):
    """A box that may reach north past the cell (z below 0): split at the cell edge, the part
    past it textured a whole block along, since box() clamps UVs to the cell and would stretch
    it."""
    if frm[2] >= 0:
        return [box(frm, to, tex, faces, per)]
    out = []
    if to[2] > 0:
        out.append(box([frm[0], frm[1], 0], to, tex,
                       tuple(f for f in faces if f != "north"), per))
    z1 = min(to[2], 0)
    b = box([frm[0], frm[1], frm[2] + 16], [to[0], to[1], z1 + 16], tex,
            tuple(f for f in faces if f != "south" or z1 == to[2]), per)
    b["from"][2] -= 16
    b["to"][2] -= 16
    out.append(b)
    return out


# A stretcher is two metres long, so two blocks: it lies north-south with its foot in the block
# in front, which should be left clear, and its raised head over the placed block.
def cot(lowered):
    y = 3 if lowered else 8
    els = (zbox([2, y, -15], [14, y + 1, 15], "frame")
           + zbox([2.5, y + 1, -14.5], [13.5, y + 2.5, 14.5], "mattress")
           + zbox([2.5, y + 2.5, 8.5], [13.5, y + 5, 14.5], "mattress")
           + zbox([1.5, y + 1.5, -13], [2, y + 3, 13], "frame")
           + zbox([14, y + 1.5, -13], [14.5, y + 3, 13], "frame"))
    for x in (3, 12):
        for z in (-14, 13):
            els += zbox([x, 1, z], [x + 1, y, z + 1], "frame")
            els += post(x + 0.5, z + 0.5, 0.9, 0, 1, "mattress")
    return els


def cot_state(model_path):
    """The stretcher's blockstate, its icon shrunk and brought back over the slot's centre."""
    st = facing_state(model_path)
    st["variants"]["inventory"] = [{"transform": {"gui": {
        "rotation": [{"x": 30}, {"y": 225}], "translation": [0, 0, 0], "scale": 0.4}}}]
    return st


STRETCHER_TEX = {"frame": T("stretcher_yellow"), "mattress": T("mattress"),
                 "particle": T("stretcher_yellow")}
prop("ems_stretcher", (1, 0, -15, 15, 14, 15), True,
     ("Ambulance Stretcher", "Krankentrage (Fahrtrage)", "Camilla de ambulancia",
      "Ambulansbår"), STRETCHER_TEX, cot(False), state=cot_state(M("ems_stretcher")))
prop("ems_stretcher_lowered", (1, 0, -15, 15, 9, 15), True,
     ("Ambulance Stretcher (Lowered)", "Krankentrage (abgesenkt)", "Camilla de ambulancia (bajada)",
      "Ambulansbår (sänkt)"), STRETCHER_TEX, cot(True),
     state=cot_state(M("ems_stretcher_lowered")))
prop("ems_stair_chair", (4, 0, 4, 12, 16, 14), True,
     ("Stair Chair", "Tragestuhl", "Silla de evacuación", "Trappstol"),
     {"frame": T("stretcher_yellow"), "seat": T("mattress"), "particle": T("stretcher_yellow")},
     [box([4.5, 0, 12], [5.5, 16, 13], "frame"), box([10.5, 0, 12], [11.5, 16, 13], "frame"),
      box([4.5, 6, 5], [11.5, 7, 12], "seat"), box([5, 7, 11.5], [11, 15, 12.5], "seat"),
      box([4.5, 0, 5], [5.5, 6, 6], "frame"), box([10.5, 0, 5], [11.5, 6, 6], "frame")]
     + pipe_x(1, 13, 1, 4, 12, "seat"))
prop("ems_backboard_rack", (1, 0, 10, 15, 16, 16), True,
     ("Backboard Wall Rack", "Spineboard-Halterung", "Soporte de tablas espinales",
      "Väggställ för spineboards"),
     {"board": T("backboard"), "steel": T("steel"), "particle": T("backboard")},
     [box([2, 1, 14], [14, 2, 16], "steel"), box([2, 14, 14], [14, 15, 16], "steel"),
      box([3, 0.5, 13], [7, 16, 14], "board"), box([9, 0.5, 12], [13, 16, 13], "board")])
prop("ems_supply_shelving", (0, 0, 6, 16, 16, 16), True,
     ("Medical Supply Shelving", "Sanitätsmaterial-Regal", "Estantería de material sanitario",
      "Hylla för sjukvårdsmaterial"),
     {"wire": T("wire"), "boxes": T("supply_boxes"), "particle": T("wire")},
     [box([0, y, 6], [16, y + 0.5, 16], "wire") for y in (0.5, 5.5, 10.5, 15.5)]
     + [box([x, 0, z], [x + 0.6, 16, z + 0.6], "wire") for x in (0.2, 15.2) for z in (6.2, 15.2)]
     + [box([1, y + 0.5, 7], [15, y + 4.5, 15], "boxes") for y in (0.5, 5.5, 10.5)])
prop("oxygen_cylinder_rack", (1, 0, 4, 15, 15, 14), True,
     ("Oxygen Cylinder Rack", "Sauerstoffflaschen-Gestell", "Soporte de botellas de oxígeno",
      "Ställ för syrgasflaskor"),
     {"o2": T("o2_green"), "steel": T("steel"), "white": T("silver"), "particle": T("o2_green")},
     [box([1, 0, 4], [15, 1, 14], "steel"), box([1, 8, 12], [15, 9, 14], "steel")]
     + post(4, 9, 2.2, 1, 13, "o2") + post(4, 9, 0.6, 13, 15, "white", bottom=False)
     + post(9, 9, 1.5, 1, 9, "o2") + post(9, 9, 0.5, 9, 10.5, "white", bottom=False)
     + post(12.5, 9, 1.5, 1, 9, "o2") + post(12.5, 9, 0.5, 9, 10.5, "white", bottom=False))
prop("medication_safe", (3, 2, 9, 13, 13, 16), True,
     ("Controlled Medication Safe", "Betäubungsmittel-Tresor", "Caja fuerte de medicamentos",
      "Läkemedelskassaskåp"),
     {"steel": T("dark_steel"), "front": T("safe_front"), "particle": T("dark_steel")},
     [box([4, 3, 10], [12, 12, 16], "steel", per={"north": "front"})])
prop("decon_sink", (1, 0, 5, 15, 16, 16), True,
     ("Decontamination Sink", "Dekontaminationsbecken", "Fregadero de descontaminación",
      "Saneringsdiskho"),
     {"s": T("stainless"), "black": T("black"), "particle": T("stainless")},
     # The basin is let into the top: its floor is the cabinet's own top face, drawn dark, and
     # its rim stands round it. (A dark face laid on the steel top z-fought with it.)
     [box([1, 0, 6], [15, 8, 16], "s", per={"up": "black"}),
      box([1, 8, 6], [15, 10, 7], "s"), box([1, 8, 15], [15, 10, 16], "s"),
      box([1, 8, 7], [2, 10, 15], "s"), box([14, 8, 7], [15, 10, 15], "s"),
      box([1, 10, 15], [15, 13, 16], "s"), box([7.5, 10, 13.5], [8.5, 16, 14.5], "s"),
      box([7.5, 15, 10], [8.5, 16, 13.5], "s")])
prop("eyewash_station", (2, 1, 9, 14, 15, 16), True,
     ("Emergency Eyewash Station", "Augendusche", "Estación lavaojos", "Ögondusch"),
     {"sign": T("eyewash_sign"), "s": T("stainless"), "yellow": T("yellow"),
      "green": T("o2_green"), "particle": T("eyewash_sign")},
     [box([3, 9, 15.5], [13, 15, 16], "green", per={"north": "sign"}),
      box([7.5, 1, 14], [8.5, 7, 15], "s")]
     + post(8, 11, 3, 6, 7.5, "s")
     + [box([3, 5, 10], [4, 6, 12], "yellow"), box([3, 1, 9.5], [4, 6, 10.5], "yellow")])
C.blocks[-1]["models"]["eyewash_station"]["elements"][0]["faces"]["north"]["uv"] = [0, 0, 16, 16]
prop("star_of_life_emblem", (1, 1, 15, 15, 15, 16), False,
     ("Star of Life Emblem", "Star-of-Life-Emblem", "Emblema de la Estrella de la Vida",
      "Star of Life-emblem"),
     {"star": T("star_of_life"), "particle": T("star_of_life")},
     [box([1, 1, 15.5], [15, 15, 16], "star", faces=("north", "south"))])
C.blocks[-1]["models"]["star_of_life_emblem"]["elements"][0]["faces"]["north"]["uv"] = [0, 0, 16, 16]
C.blocks[-1]["models"]["star_of_life_emblem"]["elements"][0]["faces"]["south"]["uv"] = [16, 0, 0, 16]


# ==========================================================================================
# Community warning and dispatch
# ==========================================================================================
SIREN_GRAY = (170, 174, 178)
flat("siren_gray", SIREN_GRAY, grain=5, seed=600)
flat("siren_dark", (70, 72, 76), grain=4, seed=601)
flat("callbox_blue", (30, 70, 150), grain=3, seed=602)


@C.texture("horn_mouth")
def _horn_mouth():
    img = fill((40, 42, 46), grain=3, seed=603)
    for i in range(0, 16, 3):
        rect(img, i, 0, i + 1, 16, (80, 82, 88))
    return img


@C.texture("array_front")
def _array_front():
    img = fill(SIREN_GRAY, grain=3, seed=604)
    rect(img, 2, 2, 14, 14, (60, 62, 66))
    for y in range(3, 14, 2):
        rect(img, 3, y, 13, y + 1, (100, 102, 108))
    return img


@C.texture("siren_panel")
def _siren_panel():
    img = fill((150, 154, 160), size=64, grain=2, seed=605)
    x0, y0, x1, y1 = north_region((2, 1), (14, 15), 64)
    bevel(img, x0, y0, x1, y1, (150, 154, 160))
    draw_text_centred(img, "OUTDOOR", 32, y0 + 4, (30, 30, 34))
    draw_text_centred(img, "WARNING", 32, y0 + 11, (30, 30, 34))
    labels = (("ALERT", (230, 180, 30)), ("ATTACK", (210, 40, 40)), ("FIRE", (230, 110, 30)),
              ("TEST", (60, 150, 220)), ("CANCEL", (60, 60, 64)))
    for i, (label, c) in enumerate(labels):
        yy = y0 + 20 + i * 7
        rect(img, x0 + 5, yy, x0 + 11, yy + 5, c)
        draw_text(img, label, x0 + 14, yy, (30, 30, 34))
    return img


@C.texture("callbox_panel")
def _callbox_panel():
    img = fill((30, 70, 150), size=64, grain=2, seed=606)
    draw_text_centred(img, "EMERGENCY", 32, 6, WHITE)
    disc(img, 32, 30, 9, (200, 30, 30))
    disc(img, 32, 30, 7, (240, 50, 40))
    for i in range(0, 12, 2):
        rect(img, 24, 46 + i, 40, 47 + i, (20, 30, 60))
    return img


@C.texture("callbox_lamp")
def _callbox_lamp():
    img = fill((80, 150, 255), grain=4, seed=607)
    rect(img, 0, 7, 16, 9, (200, 230, 255))
    return img


def screen(kind):
    def draw():
        img = fill((20, 22, 28), size=32, grain=2, seed=610 + len(kind))
        px = img.load()
        rng = random.Random(len(kind) * 17)
        if kind == "map":
            for _ in range(14):
                x = rng.randrange(2, 30)
                rect(img, x, 2, x + 1, 30, (60, 70, 80))
                y = rng.randrange(2, 30)
                rect(img, 2, y, 30, y + 1, (60, 70, 80))
            for _ in range(6):
                disc(img, rng.randrange(4, 28), rng.randrange(4, 28), 1.2,
                     (230, 60, 60) if rng.random() < 0.5 else (60, 200, 90))
        elif kind == "cad":
            for y in range(3, 29, 3):
                w = rng.randrange(8, 26)
                c = (230, 200, 60) if y == 6 else (140, 220, 140)
                rect(img, 3, y, 3 + w, y + 1, c)
        else:
            for i in range(6):
                bx = 3 + (i % 3) * 9
                by = 4 + (i // 3) * 12
                rect(img, bx, by, bx + 7, by + 9, (40, 90, 150) if i != 1 else (200, 60, 50))
        return img
    return draw


for kind in ("map", "cad", "radio"):
    C.textures["screen_" + kind] = screen(kind)


def rotating_head():
    """The chopper drum on its motor, and the horn out of it pointing north."""
    return (post(8, 8, 3, 10, 14, "gray")
            + pipe_z(8, 12, 2.4, -2, 5, "gray", front=False)
            + mouth(pipe_z(8, 12, 3.6, -4, -2, "gray")))


def mouth(els):
    """The horn's bell, its front cap drawn as the mouth: a square plate in front of it left the
    bell's open edge showing along each flat."""
    for el in els:
        if "north" in el["faces"]:
            el["faces"]["north"]["texture"] = "#mouth"
    return els


def siren_base_elements():
    return (post(8, 8, 1.6, 0, 5, "dark", top=False)
            + [box([4.5, 5, 4.5], [11.5, 10, 11.5], "dark")])


SIREN_TEX = {"gray": T("siren_gray"), "dark": T("siren_dark"), "mouth": T("horn_mouth"),
             "front": T("array_front"), "particle": T("siren_gray")}


def siren_state(reg):
    parts = []
    for f, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        base = {"model": M(reg + "_base")}
        head = {"model": M(reg + "_head")}
        if rot:
            base["y"] = rot
            head["y"] = rot
        parts.append({"when": {"facing": f, "head": "false"}, "apply": base})
        parts.append({"when": {"OR": [{"facing": f, "active": "false", "head": "false"},
                                      {"facing": f, "head": "true"}]}, "apply": head})
    return {"multipart": parts}


C.add("warning_siren_rotating",
      'new BlockWarningSiren("warning_siren_rotating", %s, true)' % B(3, 0, 3, 13, 16, 13),
      ("Rotating Outdoor Warning Siren", "Drehende Motorsirene", "Sirena rotativa de alerta",
       "Roterande tyfon (varningssiren)"),
      {"warning_siren_rotating_base": model(SIREN_TEX, siren_base_elements()),
       "warning_siren_rotating_head": model(SIREN_TEX, rotating_head()),
       "warning_siren_rotating_item": dict(model(SIREN_TEX,
                                                 siren_base_elements() + rotating_head()),
                                           display={"gui": {"rotation": [30, 225, 0],
                                                            "translation": [0, 0, 0],
                                                            "scale": [0.55, 0.55, 0.55]}})},
      siren_state("warning_siren_rotating"),
      item={"parent": "csm:block/" + "lifesafety/services/warning_siren_rotating_item"}, tab=ES)


def array_elements():
    els = siren_base_elements()
    for y0 in (10, 13):
        els.append(box([4, y0, 4], [12, y0 + 3, 12], "gray",
                       per={f: "front" for f in ("north", "south", "east", "west")}))
    return els


C.add("warning_siren_electronic",
      'new BlockWarningSiren("warning_siren_electronic", %s, false)' % B(4, 0, 4, 12, 16, 12),
      ("Electronic Outdoor Warning Siren", "Elektronische Sirene", "Sirena electrónica de alerta",
       "Elektronisk varningssiren"),
      {"warning_siren_electronic": model(SIREN_TEX, array_elements())},
      {"multipart": [{"when": {"facing": f, "head": "false"},
                      "apply": dict({"model": M("warning_siren_electronic")},
                                    **({"y": r} if r else {}))}
                     for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))]},
      item={"parent": "csm:block/lifesafety/services/warning_siren_electronic"}, tab=ES)

C.add("warning_siren_controller",
      'new BlockSirenController("warning_siren_controller", %s)' % B(2, 1, 11, 14, 15, 16),
      ("Warning Siren Controller", "Sirenensteuergerät", "Controlador de sirenas de alerta",
       "Styrenhet för varningssirener"),
      {"warning_siren_controller": model(
          {"steel": T("steel"), "front": T("siren_panel"), "particle": T("steel")},
          [box([2, 1, 11], [14, 15, 16], "steel", per={"north": "front"})])},
      facing_state(M("warning_siren_controller"), {"powered": {"false": {}, "true": {}}}), tab=ES)

C.add("blue_light_call_box", 'new BlockCallBox("blue_light_call_box", %s, 12)'
      % B(4, 0, 4, 12, 16, 12),
      ("Blue Light Emergency Call Box", "Notrufsäule mit Blaulicht",
       "Poste de llamada de emergencia con luz azul", "Nödtelefonstolpe med blått ljus"),
      {"blue_light_call_box": model(
          {"blue": T("callbox_blue"), "panel": T("callbox_panel"), "lamp": T("callbox_lamp"),
           "dark": T("siren_dark"), "particle": T("callbox_blue")},
          [box([4.5, 0, 4.5], [11.5, 26, 11.5], "blue", per={"north": "panel"}),
           box([4, 26, 4], [12, 30, 12], "lamp"), box([4.5, 30, 4.5], [11.5, 31, 11.5], "dark")])},
      facing_state(M("blue_light_call_box")), tab=ES)
el = C.blocks[-1]["models"]["blue_light_call_box"]["elements"][0]
el["faces"]["north"]["uv"] = [0, 0, 16, 16]
for f in ("south", "east", "west"):
    el["faces"][f]["uv"] = [4.5, 0, 11.5, 16]

C.add("dispatch_console", 'new BlockFireProtectionProp("dispatch_console", %s, true)'
      % B(0, 0, 2, 16, 16, 16),
      ("911 Dispatch Console", "Leitstellen-Arbeitsplatz", "Consola de despacho 911",
       "Larmoperatörsbord"),
      {"dispatch_console": model(
          {"desk": T("laminate"), "dark": T("siren_dark"), "black": T("black"),
           "map": T("screen_map"), "cad": T("screen_cad"), "radio": T("screen_radio"),
           "particle": T("laminate")},
          [box([0, 11, 2], [16, 12, 16], "desk"), box([0, 0, 14], [16, 11, 16], "dark"),
           box([0, 0, 2], [1, 11, 16], "dark"), box([15, 0, 2], [16, 11, 16], "dark"),
           box([3, 12, 5], [13, 12.5, 8], "black"),
           box([0.5, 13, 12], [5.5, 18, 13], "black", per={"north": "radio"}),
           box([5.5, 13, 12], [10.5, 18, 13], "black", per={"north": "map"}),
           box([10.5, 13, 12], [15.5, 18, 13], "black", per={"north": "cad"}),
           box([7.5, 12, 13], [8.5, 13, 14], "black")])},
      facing_state(M("dispatch_console")), tab=ES)
for i in (5, 6, 7):
    C.blocks[-1]["models"]["dispatch_console"]["elements"][i]["faces"]["north"]["uv"] = [0, 0, 16, 16]

prop("radio_console_speaker", (4, 0, 6, 12, 7, 12), False,
     ("Radio Console Speaker", "Funklautsprecher", "Altavoz de consola de radio",
      "Radiohögtalare"),
     {"dark": T("siren_dark"), "grille": T("speaker_grille"), "particle": T("siren_dark")},
     [box([4, 0, 6], [12, 7, 12], "dark", per={"north": "grille"})])


def picto_trefoil(img, r, fg):
    """The shelter symbol: three triangles pointing in, in a circle."""
    x0, y0, x1, y1 = r
    cx, cy = (x0 + x1) / 2.0, y0 + 15
    disc(img, cx, cy, 13, fg)
    disc(img, cx, cy, 11, (250, 214, 30))
    px = img.load()
    for y in range(int(cy - 11), int(cy + 11)):
        for x in range(int(cx - 11), int(cx + 11)):
            a = math.atan2(y + 0.5 - cy, x + 0.5 - cx)
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            k = ((a + math.pi / 2) / (2 * math.pi / 3)) % 1
            if 2 < d < 11 and abs(k - 0.5) < 0.23 * (d / 11):
                px[x, y] = clamp(fg) + (255,)


def picto_tornado(img, r, fg):
    x0, y0, x1, y1 = r
    cx = (x0 + x1) / 2.0
    for i in range(12):
        w = 16 - i * 1.2
        rect(img, int(cx - w / 2 + i * 0.4), y0 + 4 + i * 2, int(cx + w / 2 + i * 0.4),
             y0 + 5 + i * 2, fg)


def picto_people(img, r, fg):
    x0, y0, x1, y1 = r
    cx = (x0 + x1) // 2
    for dx in (-10, 0, 10):
        disc(img, cx + dx, y0 + 8, 2.5, fg)
        rect(img, cx + dx - 2, y0 + 11, cx + dx + 2, y0 + 20, fg)
        rect(img, cx + dx - 2, y0 + 20, cx + dx - 1, y0 + 27, fg)
        rect(img, cx + dx + 1, y0 + 20, cx + dx + 2, y0 + 27, fg)


def shelter_sign(bg, fg, lines, picto):
    def draw():
        img = fill(bg, size=64, grain=2, seed=620)
        x0, y0, x1, y1 = north_region((2, 1), (14, 15), 64)
        frame(img, x0, y0, x1, y1, shade(bg, 0.7), 1)
        picto(img, (x0, y0, x1, y1), fg)
        yy = y0 + 32
        for text, scale in lines:
            draw_text_centred(img, text, (x0 + x1) / 2.0, yy, fg, scale)
            yy += 6 * scale + 1
        return img
    return draw


C.textures["sign_fallout"] = shelter_sign((250, 214, 30), (20, 20, 20),
                                          [("FALLOUT", 1), ("SHELTER", 1)], picto_trefoil)
C.textures["sign_storm"] = shelter_sign((30, 70, 150), WHITE, [("STORM", 1), ("SHELTER", 1)],
                                        picto_tornado)
C.textures["sign_assembly"] = shelter_sign((0, 130, 70), WHITE, [("ASSEMBLY", 1), ("POINT", 1)],
                                           picto_people)
flat("sign_back", (170, 172, 176), grain=2, seed=621)
for tex, reg, names in (
        ("sign_fallout", "fallout_shelter_sign",
         ("Fallout Shelter Sign", "Schild Schutzraum", "Señal de refugio nuclear",
          "Skylt skyddsrum")),
        ("sign_storm", "storm_shelter_sign",
         ("Storm Shelter Sign", "Schild Sturmschutzraum", "Señal de refugio contra tormentas",
          "Skylt stormskydd")),
        ("sign_assembly", "assembly_point_sign",
         ("Evacuation Assembly Point Sign", "Schild Sammelplatz",
          "Señal de punto de encuentro", "Skylt återsamlingsplats"))):
    prop(reg, (2, 1, 15, 14, 15, 16), False, names,
         {"face": T(tex), "back": T("sign_back"), "particle": T(tex)},
         [box([2, 1, 15.5], [14, 15, 16], "back", per={"north": "face"})])

for key, names in (
        ("choice", ("[Sirens] Set to %s (%s sirens linked). Sneak and click to carry it out.",
                    "[Sirenen] Eingestellt: %s (%s Sirenen verbunden). Schleichen und klicken zum Auslösen.",
                    "[Sirenas] Seleccionado: %s (%s sirenas vinculadas). Agáchate y haz clic para activar.",
                    "[Sirener] Vald: %s (%s sirener kopplade). Smyg och klicka för att utlösa.")),
        ("done", ("[Sirens] %s sent to %s sirens.", "[Sirenen] %s an %s Sirenen gesendet.",
                  "[Sirenas] %s enviado a %s sirenas.", "[Sirener] %s skickat till %s sirener.")),
        ("weekly_on", ("[Sirens] Weekly noon test on.", "[Sirenen] Wöchentlicher Probealarm an.",
                       "[Sirenas] Prueba semanal activada.", "[Sirener] Veckotest på.")),
        ("weekly_off", ("[Sirens] Weekly noon test off.", "[Sirenen] Wöchentlicher Probealarm aus.",
                        "[Sirenas] Prueba semanal desactivada.", "[Sirener] Veckotest av.")),
        ("choice.alert", ("Alert (steady)", "Warnung (Dauerton)", "Alerta (continuo)",
                          "Viktigt meddelande (ton)")),
        ("choice.attack", ("Attack (wail)", "Angriff (auf- und abschwellend)", "Ataque (ondulante)",
                           "Flyglarm (stigande och fallande)")),
        ("choice.fire", ("Fire (hi-lo)", "Feuer (hoch-tief)", "Incendio (alto-bajo)",
                         "Brand (hög-låg)")),
        ("choice.test", ("Test", "Probe", "Prueba", "Prov")),
        ("choice.cancel", ("Cancel", "Entwarnung", "Cancelar", "Avbryt")),
        ("choice.weekly_test", ("Weekly test switch", "Wöchentlicher Probealarm (Schalter)",
                                "Interruptor de prueba semanal", "Omkopplare för veckotest"))):
    C.add_lang("csm.lifesafety.siren." + key, names)


# ==========================================================================================
# The working items, and the first aid cabinet that hands out kits
# ==========================================================================================
@C.texture("item_extinguisher")
def _item_extinguisher():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 6, 4, 11, 15, RED)
    rect(img, 6, 4, 7, 15, shade(RED, 1.3))
    rect(img, 7, 8, 10, 11, WHITE)
    rect(img, 7, 2, 10, 4, (190, 194, 200))
    rect(img, 5, 1, 11, 2, BLACK)
    rect(img, 10, 3, 13, 4, BLACK)
    rect(img, 12, 4, 13, 9, BLACK)
    return img


@C.texture("item_first_aid")
def _item_first_aid():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rect(img, 2, 4, 14, 14, WHITE)
    frame(img, 2, 4, 14, 14, shade(WHITE, 0.7))
    rect(img, 6, 2, 10, 4, shade(WHITE, 0.6))
    rect(img, 7, 6, 9, 12, RED)
    rect(img, 5, 8, 11, 10, RED)
    return img


@C.texture("first_aid_cabinet_front")
def _first_aid_cabinet_front():
    img = fill(WHITE, grain=2, seed=700)
    x0, y0, x1, y1 = north_region((3, 2), (13, 14), 16)
    bevel(img, x0, y0, x1, y1, WHITE)
    rect(img, 7, 4, 9, 11, RED)
    rect(img, 5, 6, 11, 8, RED)
    rect(img, x0 + 1, 12, x0 + 2, 14, (150, 150, 150))
    return img


flat("white_paint", WHITE, grain=2, seed=701)
C.add("first_aid_cabinet", 'new BlockFirstAidCabinet("first_aid_cabinet", %s)'
      % B(3, 2, 11, 13, 14, 16),
      ("First Aid Cabinet", "Erste-Hilfe-Schrank", "Botiquín de pared", "Första hjälpen-skåp"),
      {"first_aid_cabinet": model({"white": T("white_paint"), "front": T("first_aid_cabinet_front"),
                                   "particle": T("white_paint")},
                                  [box([3, 2, 11], [13, 14, 16], "white",
                                       per={"north": "front"})])},
      facing_state(M("first_aid_cabinet")), tab=ES)
C.add_item("fire_extinguisher_item", "new ItemFireExtinguisher()",
           ("Fire Extinguisher", "Feuerlöscher", "Extintor", "Brandsläckare"),
           "item_extinguisher", tab=ES)
C.add_item("first_aid_kit", "new ItemFirstAidKit()",
           ("First Aid Kit", "Verbandkasten", "Botiquín de primeros auxilios", "Förbandslåda"),
           "item_first_aid", tab=ES)
for key, names in (
        ("extinguisher.charge", ("Charge: %s%%", "Füllung: %s %%", "Carga: %s %%",
                                 "Laddning: %s %%")),
        ("extinguisher.hint", ("Hold use to spray. Sneak-use on a wall to hang it. Refill at an SCBA fill station.",
                               "Benutzen halten zum Sprühen. Schleichend an eine Wand zum Aufhängen. Nachfüllen an der Atemluft-Füllstation.",
                               "Mantén usar para rociar. Agáchate y úsalo en una pared para colgarlo. Recárgalo en una estación de llenado.",
                               "Håll inne använd för att spruta. Smyg och använd mot en vägg för att hänga upp. Fyll på vid en fyllstation.")),
        ("extinguisher.refilled", ("[Fill station] Extinguisher recharged.",
                                   "[Füllstation] Feuerlöscher nachgefüllt.",
                                   "[Estación de llenado] Extintor recargado.",
                                   "[Fyllstation] Brandsläckaren påfylld.")),
        ("first_aid.hint", ("Hold use to heal four hearts. Kits rest thirty seconds after.",
                            "Benutzen halten heilt vier Herzen. Danach ruhen Verbandkästen dreißig Sekunden.",
                            "Mantén usar para curar cuatro corazones. Después los botiquines esperan treinta segundos.",
                            "Håll inne använd för att hela fyra hjärtan. Därefter vilar lådorna i trettio sekunder.")),
        ("first_aid.empty", ("[First aid] You have already taken a kit from this cabinet today.",
                             "[Erste Hilfe] Du hast heute schon einen Verbandkasten aus diesem Schrank genommen.",
                             "[Primeros auxilios] Ya has cogido un botiquín de este armario hoy.",
                             "[Första hjälpen] Du har redan tagit en låda ur det här skåpet i dag."))):
    C.add_lang("csm.lifesafety." + key, names)


if __name__ == "__main__":
    sys.exit(C.main())
