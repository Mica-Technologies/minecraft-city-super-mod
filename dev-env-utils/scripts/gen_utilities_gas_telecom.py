#!/usr/bin/env python3
"""Every asset the Utilities tab's gas yard and telecom pieces ship: a gas regulator station built
from pieces (the steel skid, the gas pipe, the inlet and outlet ball valves, the pressure
regulator, the turbine meter, the line heater, the relief vent stack), the odorant tank, the
yard's warning and notice signs, and on the telecom side the fibre distribution cabinet, a cell
site's equipment and battery cabinets, the cable ice bridge and its stanchions, the GPS antenna,
a monopole built to height with its antenna array, and a small cell's canister antenna and radio
for the mod's street poles.

    python dev-env-utils/scripts/gen_utilities_gas_telecom.py
    python dev-env-utils/scripts/gen_utilities_gas_telecom.py --check
    python dev-env-utils/scripts/gen_utilities_gas_telecom.py --fragments   # tab lines to paste
    python dev-env-utils/scripts/gen_utilities_gas_telecom.py --report      # OBJ quads

What is here, and the class that places each (package powergrid.gas or powergrid.telecom unless
named):

- **The gas pipe** (water.BlockWaterPipe carrying the GAS service): welded steel in gas yellow,
  joining only gas pipe and gas fittings, with a welded fitting at a bend or a tee.
- **Inline gas fittings** (water.BlockPipeFitting, GAS): the ball valve, the pressure regulator
  and the turbine meter, whose index counts by texture.
- **The relief vent stack** (BlockVentStack): the top of a vent stack of gas pipe, with its rain
  cap.
- **The skid** (BlockEquipmentSkid): a steel deck that joins side by side, drawing pipe stands up
  to the pipe above it.
- **The line heater** (water.BlockPumpUnit, GAS, Roads' utility box) and **the odorant tank**
  (Roads' BlockUtilityBox).
- **Signs** (services.BlockUtilitySign): on a wall, or on the compound's chain-link fence.
- **Telecom cabinets**: the fibre distribution cabinet (BlockCabinet, doors a click opens), the
  cell site's equipment and battery cabinets (Roads' BlockUtilityBox).
- **The ice bridge** (BlockIceBridge) on its stanchions (water.BlockTowerColumn), **the GPS
  antenna** (services.BlockUtilityFixture), **the monopole** (water.BlockTowerColumn sections
  and BlockAntennaArray), **the small cell** (BlockSmallCell on a pole's top, BlockPoleRadio on
  its side).

What exists already is not repeated: telecom pedestals, the low telecom enclosure, buried
markers and manhole covers are Roads' Streetscape; the RF and base-station signs are Roads' road
signs; the compound fences are Building's chain-link; a cell site's generator is the sewer
family's standby generator. Every text write keeps the line endings the file already has on
disk, and a file whose content has not changed is not touched (gen_utilities_meters.Catalogue).
"""
import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_utilities_meters as um  # noqa: E402
import gen_utilities_water as gw  # noqa: E402
import life_safety_gen_common as lc  # noqa: E402

ASSETS = um.ASSETS
TAB = "CsmTabUtilities"
FACINGS = um.FACINGS
GEN = "gen_utilities_gas_telecom.py"

G = um.Catalogue(GEN, "utilities/gas", "utilities/gas", assets=ASSETS)
K = um.Catalogue(GEN, "utilities/telecom", "utilities/telecom", assets=ASSETS)
S = um.Catalogue(GEN, "utilities/signs", "utilities/signs", assets=ASSETS)
CATALOGUES = (G, S, K)

box, fmt, extent = um.box, um.fmt, um.extent
octagon = um.octagon
Poly = gw.Poly
ICON_DISPLAY = gw.ICON_DISPLAY
big_display = gw.big_display
clamp_uv = gw.clamp_uv
extent_unit = gw.extent_unit
UP = (0, 16, 0)


def names_of(*n):
    assert len(n) == 4
    return n


def W(name):
    """One of the water system's textures, shared rather than drawn again."""
    return "csm:blocks/utilities/water/" + name


def MT(name):
    """One of the building service meters' textures."""
    return "csm:blocks/utilities/meters/" + name


def model(tex, els, display=None, ao=True):
    m = {"parent": "block/block", "textures": dict(tex)}
    if not ao:
        m["ambientocclusion"] = False
    if els:
        m["elements"] = els
    if display:
        m["display"] = display
    return m


def rule(cat, name, when=None, y=0):
    r = {"apply": {"model": cat.M(name) if ":" not in name else name}}
    if y:
        r["apply"]["y"] = y
    if when:
        r["when"] = {k: (("true" if v else "false") if isinstance(v, bool) else str(v))
                     for k, v in when.items()}
    return r


def multipart(cat, rules, inventory=None):
    out = {"multipart": rules}
    if inventory:
        out = {"variants": {"inventory": {"model": cat.M(inventory)}}, "multipart": rules}
    return out


def facing_rules(cat, parts):
    """[(model name, conditions)] each turned to the four facings (the model faces north)."""
    out = []
    for name, cond in parts:
        for f, y in FACINGS:
            w = {"facing": f}
            w.update(cond)
            out.append(rule(cat, name, w, y))
    return out


def box_java(els, top=16):
    return "new double[]{%s}" % ", ".join(fmt(v) for v in extent(els, top))


def unit_java(els):
    return ", ".join(fmt(v) for v in extent_unit(els))


def fix(els):
    return [clamp_uv(e) for e in els]


def plane_n(x0, y0, x1, y1, z, tex, uv):
    """A picture on a plane facing north, uv given (a window of a sheet)."""
    return box([x0, y0, z], [x1, y1, z], tex, faces=("north",), uv={"north": uv})


# ------------------------------------------------------------------------------------------
# Colours and textures
# ------------------------------------------------------------------------------------------
GAS_YELLOW = um.GAS_YELLOW
VESSEL = (160, 164, 160)
BEAM = (92, 96, 100)
CABINET = (202, 204, 196)
GALV = (168, 174, 176)
RADOME = (226, 228, 224)
RADIO = (74, 78, 82)
CANISTER = (96, 100, 104)
TEAL = (0, 118, 120)

G.textures["vessel"] = lambda: _vessel()
G.textures["meter_body"] = lambda: lc.fill((138, 146, 152), 16, 3, 611)


def _vessel():
    """The line heater's shell: grey paint with a girth weld every half block."""
    img = lc.fill(VESSEL, 16, 3, 601)
    for x in (0, 8):
        for y in range(16):
            img.putpixel((x, y), lc.shade(VESSEL, 0.82) + (255,))
    return img


@G.texture("skid_beam")
def _skid_beam():
    """The skid's perimeter beam seen side on: the flanges top and bottom, the web between."""
    img = lc.fill(BEAM, 16, 3, 602)
    for x in range(16):
        for y in (0, 1, 14, 15):
            img.putpixel((x, y), lc.shade(BEAM, 1.22 if y in (0, 14) else 1.08) + (255,))
        for y in (2, 13):
            img.putpixel((x, y), lc.shade(BEAM, 0.62) + (255,))
    for y in range(2, 13):
        img.putpixel((0, y), lc.shade(BEAM, 0.8) + (255,))
    return img


@G.texture("skid_deck")
def _skid_deck():
    """The skid's deck: painted steel checker plate, darker than the water system's aluminium
    diamond plate, which reads as a white slab in the sun."""
    base = (118, 122, 124)
    img = lc.fill(base, 16, 2, 603)
    for y in range(16):
        for x in range(16):
            if (x + 2 * y) % 8 == 0 or (x - 2 * y) % 8 == 4:
                img.putpixel((x, y), lc.shade(base, 1.18) + (255,))
    return img


@G.texture("meter_index")
def _meter_index():
    """The turbine meter's index: a mechanical counter whose last wheel turns, as an animated
    strip, so nothing ticks."""
    frames = 4
    strip = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        img = lc.fill((60, 64, 70), 16, 2, 621 + f)
        lc.rect(img, 1, 1, 15, 15, (236, 234, 224))
        lc.rect(img, 2, 5, 14, 11, (22, 22, 24))
        lc.draw_text(img, "%02d" % (41 + f // 2), 3, 6, (240, 240, 236))
        lc.rect(img, 10, 5, 14, 11, (170, 30, 26))
        lc.draw_text(img, "%d" % ((7 + f) % 10), 11, 6, (250, 250, 246))
        lc.rect(img, 3, 2, 13, 3, (120, 124, 130))
        lc.rect(img, 3, 13, 9, 14, (120, 124, 130))
        strip.paste(img, (0, 16 * f))
    return strip


G.extra["textures/blocks/utilities/gas/meter_index.png.mcmeta"] = um.mcmeta(10)


@G.texture("odorant_label")
def _odorant_label():
    """The odorant tank's label, 64 x 32 texels at the top of the texture: ODORANT in red over
    FLAMMABLE LIQUID on white, in a black edge; the rest is clear."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    lc.rect(img, 0, 0, 64, 32, (244, 244, 238))
    lc.frame(img, 0, 0, 64, 32, (26, 26, 28))
    lc.draw_text_centred(img, "ODORANT", 32, 5, (196, 30, 28), scale=2)
    lc.draw_text_centred(img, "FLAMMABLE LIQUID", 32, 20, (26, 26, 28))
    return img


# ------------------------------------------------------------------------------------------
# The gas pipe and its inline fittings
# ------------------------------------------------------------------------------------------
GAS_R = 3.0         # the pipe's radius: a 6-inch line at the mod's scale
JOINT_R = 3.7       # a welded elbow or tee
FLANGE_R = 4.0
PIPE_TEX = {"pipe": MT("pipe_gas"), "particle": MT("pipe_gas")}
SIDES = ("north", "south", "east", "west", "up", "down")


def gas_arm(side):
    """The pipe from the block's centre out to one face; no face at the block's face, so a joining
    pipe or fitting meets it with nothing coplanar."""
    axis, sign = {"north": ("z", -1), "south": ("z", 1), "west": ("x", -1), "east": ("x", 1),
                  "down": ("y", -1), "up": ("y", 1)}[side]
    lo, hi = (0, 8) if sign < 0 else (8, 16)
    return octagon(axis, 8, 8, GAS_R, lo, hi, "pipe", False, False)


def gas_stub(z0, z1):
    """A fitting's own length of pipe along z, capped at the block's face it reaches (where a
    joining pipe's arm has no face of its own)."""
    return octagon("z", 8, 8, GAS_R, z0, z1, "pipe", z0 <= 0, z1 >= 16)


def gas_pipe():
    models = {"gas_pipe_arm_" + s: model(PIPE_TEX, gas_arm(s)) for s in SIDES}
    models["gas_pipe_joint"] = model(PIPE_TEX, octagon("y", 8, 8, JOINT_R, 4.3, 11.7, "pipe",
                                                       True, True))
    rules = [rule(G, "gas_pipe_joint", {"joint": True})]
    rules += [rule(G, "gas_pipe_arm_" + s, {s: True}) for s in SIDES]
    G.add("gas_pipe", 'new BlockWaterPipe("gas_pipe", BlockWaterPipe.GAS, %s)' % fmt(JOINT_R),
          names_of("Gas Pipe", "Gasleitung", "Tubería de Gas", "Gasledning"),
          models, multipart(G, rules),
          item=model(PIPE_TEX, gas_arm("north") + gas_arm("south")), tab=TAB)


FIT_TEX = {"pipe": MT("pipe_gas"), "reg": MT("regulator"), "lever": MT("lever_yellow"),
           "dark": W("dark"), "steel": W("steel"), "meter": "csm:blocks/utilities/gas/meter_body",
           "index": G.T("meter_index"), "particle": MT("pipe_gas")}


def flange_pair(z_a, z_b, r=FLANGE_R, tex="pipe"):
    """Flanges 0.8 thick either side of a body that runs z_a..z_b."""
    return (octagon("z", 8, 8, r, z_a - 0.8, z_a, tex, True, True)
            + octagon("z", 8, 8, r, z_b, z_b + 0.8, tex, True, True))


def gas_fittings():
    """The inline fittings (BlockPipeFitting, GAS): each carries the pipe through along the way
    the player was looking and joins a gas pipe at either end."""
    ball = gas_stub(0, 3.4) + gas_stub(12.6, 16) + flange_pair(4.2, 11.8, 4.6)
    ball += octagon("z", 8, 8, 4.1, 4.2, 11.8, "pipe", False, False)
    ball += octagon("y", 8, 8, 0.9, 12.1, 14.2, "steel", False, True)
    ball.append(box([7.3, 14.2, 1.4], [8.7, 15.0, 9.2], "lever"))
    ball.append(box([7.0, 13.9, 1.0], [9.0, 15.3, 3.2], "dark"))

    reg = gas_stub(0, 3.4) + gas_stub(12.6, 16) + flange_pair(4.2, 11.8, 4.6, "reg")
    reg.append(box([4.4, 3.6, 4.2], [11.6, 12, 11.8], "reg"))
    reg += octagon("y", 8, 8, 1.4, 12, 14, "reg", False, False)
    reg += octagon("y", 8, 8, 5.6, 14, 15.6, "reg", True, False)
    reg += octagon("y", 8, 8, 6.1, 15.6, 16, "reg", True, False)
    reg += octagon("y", 8, 8, 6.1, 16, 16.4, "reg", False, True, shift=UP)
    reg += octagon("y", 8, 8, 5.6, 16.4, 18.0, "reg", False, True, shift=UP)
    reg += octagon("y", 8, 8, 1.8, 18.0, 22.6, "reg", False, True, shift=UP)
    reg += octagon("y", 8, 8, 0.9, 22.6, 23.6, "dark", False, True, shift=UP)
    # the control line from the downstream pipe up to the diaphragm case
    reg.append(box([11.6, 7.4, 12.8], [12.2, 8.0, 13.4], "steel"))
    reg.append(box([11.6, 8.0, 12.8], [12.2, 16.0, 13.4], "steel",
                   faces=("north", "south", "east", "west")))
    reg.append(box([11.6, 16.0, 12.8], [12.2, 16.8, 13.4], "steel", shift=UP,
                   faces=("north", "south", "east", "west", "up")))
    reg.append(box([8.6, 16.8, 12.2], [12.2, 17.4, 12.8], "steel", shift=UP))

    meter = gas_stub(0, 3.0) + gas_stub(13.0, 16) + flange_pair(3.8, 12.2, 4.9)
    meter += octagon("z", 8, 8, 4.4, 3.8, 12.2, "meter", False, False)
    meter.append(box([6.6, 12.2, 6.6], [9.4, 13.4, 9.4], "meter",
                     faces=("north", "south", "east", "west")))
    meter.append(box([5.0, 13.4, 5.0], [11.0, 16.0, 11.0], "meter",
                     faces=("north", "south", "east", "west", "down")))
    meter.append(box([5.0, 16.0, 5.0], [11.0, 18.2, 11.0], "meter", shift=UP,
                     faces=("north", "south", "east", "west", "up")))
    meter.append(plane_n(5.6, 13.9, 10.4, 17.7, 4.98, "index", [0, 0, 16, 16]))

    for reg_name, els, names in (
            ("gas_ball_valve", ball,
             names_of("Gas Ball Valve", "Gas-Kugelhahn", "Válvula de Bola de Gas",
                      "Kulventil för Gas")),
            ("gas_pressure_regulator", reg,
             names_of("Gas Pressure Regulator", "Gas-Druckregler", "Regulador de Presión de Gas",
                      "Gastrycksregulator")),
            ("gas_turbine_meter", meter,
             names_of("Gas Turbine Meter", "Gas-Turbinenradzähler", "Medidor de Turbina de Gas",
                      "Turbingasmätare"))):
        els = fix(els)
        G.add(reg_name, 'new BlockPipeFitting("%s", %s, BlockWaterPipe.GAS)'
              % (reg_name, box_java(els)), names, {reg_name: model(FIT_TEX, els)},
              lc.facing_state(G.M(reg_name)), tab=TAB)


def vent_stack():
    """The top of a relief vent stack (BlockVentStack): the vent pipe, its bird screen and the
    hinged rain cap blown a little open, on a gas pipe that joins it from below."""
    els = octagon("y", 8, 8, GAS_R, 0, 12.4, "pipe", True, False)
    els += octagon("y", 8, 8, GAS_R + 0.35, 11.6, 13.2, "dark", False, False)
    els.append(box([5.4, 13.0, 5.4], [10.6, 13.05, 10.6], "dark", faces=("up",)))
    flap = box([4.2, 13.3, 4.2], [11.8, 13.9, 11.8], "steel")
    flap["rotation"] = {"origin": [8, 13.6, 11.8], "axis": "x", "angle": -22.5}
    els.append(flap)
    els.append(box([7.2, 12.6, 11.4], [8.8, 14.4, 12.4], "dark"))
    tex = dict(FIT_TEX)
    G.add("gas_vent_stack", 'new BlockVentStack("gas_vent_stack", %s)' % box_java(els),
          names_of("Gas Vent Stack", "Gas-Abblaserohr", "Chimenea de Venteo de Gas",
                   "Gasavblåsningsrör"),
          {"gas_vent_stack": model(tex, els)}, lc.facing_state(G.M("gas_vent_stack")), tab=TAB)


# ------------------------------------------------------------------------------------------
# The skid
# ------------------------------------------------------------------------------------------
def skid():
    """The regulator station's skid (BlockEquipmentSkid): a diamond-plate deck on a perimeter
    of steel beam, the beam drawn only where the skid stops; a pipe stand and saddle up to a pipe
    above it (support x, turned for z)."""
    tex = {"plate": G.T("skid_deck"), "beam": G.T("skid_beam"), "steel": W("steel"),
           "dark": W("dark"), "particle": G.T("skid_deck")}
    deck = [box([0, 0, 0], [16, 16, 16], "plate", faces=("up",))]
    models = {"skid_deck": model(tex, deck)}
    for side in ("north", "south", "east", "west"):
        models["skid_edge_" + side] = model(tex, [box([0, 0, 0], [16, 16, 16], "beam",
                                                      faces=(side,))])
    pipe_bottom = 16 + 8 - GAS_R
    stand = [box([6.4, 16, 5.6], [9.6, 16.4, 10.4], "dark", shift=UP),
             box([7.3, 16.4, 7.3], [8.7, pipe_bottom - 0.7, 8.7], "steel", shift=UP,
                 faces=("north", "south", "east", "west")),
             box([7.0, pipe_bottom - 0.7, 4.6], [9.0, pipe_bottom, 11.4], "steel", shift=UP)]
    models["skid_support"] = model(tex, stand)
    rules = [rule(G, "skid_deck")]
    for side in ("north", "south", "east", "west"):
        rules.append(rule(G, "skid_edge_" + side, {side: False}))
    # the stand's saddle runs across the pipe: as drawn it carries a pipe running along x
    rules.append(rule(G, "skid_support", {"support": "x"}))
    rules.append(rule(G, "skid_support", {"support": "z"}, y=90))
    icon = deck + [box([0, 0, 0], [16, 16, 16], "beam", faces=("north", "south", "east", "west"))]
    G.add("gas_station_skid", 'new BlockEquipmentSkid("gas_station_skid")',
          names_of("Regulator Station Skid", "Reglerstation-Grundrahmen",
                   "Patín de Estación Reguladora", "Regulatorstationens Ram"),
          models, multipart(G, rules), item=model(tex, icon, ICON_DISPLAY), tab=TAB)


# ------------------------------------------------------------------------------------------
# The line heater and the odorant tank (Roads' utility box)
# ------------------------------------------------------------------------------------------
def line_heater():
    """An indirect-fired line heater: the water bath vessel on its saddles, two blocks long
    (growing to the placer's right, the model's west), the firetube's stack at the far end and
    the burner below it; the gas passes through the coil header in the root block, front to back,
    where a gas pipe joins it (BlockPumpUnit, FRONT_BACK, GAS)."""
    tex = {"vessel": G.T("vessel"), "pipe": MT("pipe_gas"), "dark": W("dark"),
           "steel": W("steel"), "panel": W("panel_grey"), "display": W("flow_display"),
           "particle": G.T("vessel")}
    cy = 11.0
    r = 6.2
    els = [box([-15.5, 0, 2.5], [5.0, 1.2, 4.0], "dark", shift=(0, 0, 0)),
           box([-15.5, 0, 12.0], [5.0, 1.2, 13.5], "dark")]
    for x in (-11.0, 0.0):
        els.append(box([x, 1.2, 3.2], [x + 1.4, cy - r + 0.2, 12.8], "dark"))
    els += octagon("x", cy, 8, r, -14.6, 3.5, "vessel", False, False)
    els += octagon("x", cy, 8, r - 1.4, -15.4, -14.6, "vessel", True, False)
    els += octagon("x", cy, 8, r + 0.5, 3.5, 4.3, "vessel", True, True)
    # the coil header the gas passes through, and the pipe's nozzles front and back
    els.append(box([4.3, 3.8, 4.2], [12.4, 13.4, 11.8], "vessel"))
    els += gas_stub(0, 3.4) + gas_stub(12.6, 16)
    els += flange_pair(4.2, 11.8, FLANGE_R)
    # the firetube's stack at the far end, its rain cap, and the burner below it
    sx = -11.8
    els += octagon("y", sx, 8, 1.6, cy + r - 0.4, 30.4, "dark", False, False, shift=(-16, 16, 0))
    els += octagon("y", sx, 8, 2.5, 30.4, 31.2, "dark", True, True, shift=(-16, 16, 0))
    els.append(box([-15.9, 4.6, 5.2], [-15.4, 11.6, 10.8], "dark", shift=(-16, 0, 0)))
    els.append(box([-14.6, 1.2, 1.6], [-9.4, 5.6, 3.2], "dark", shift=(-16, 0, 0)))
    # the fill neck and relief on top, the temperature controller on the front
    els += octagon("y", -4.0, 8, 1.1, cy + r - 0.3, cy + r + 2.4, "vessel", False, True,
                   shift=(-16, 16, 0))
    els += octagon("y", 1.2, 8, 0.7, cy + r - 0.3, cy + r + 3.4, "steel", False, True, shift=UP)
    els.append(box([-6.0, 5.6, 1.6], [-1.6, 10.4, 2.2], "panel", shift=(-16, 0, 0)))
    els.append(plane_n(-5.4, 6.4, -2.2, 9.6, 1.56, "display", [0, 2, 16, 13]))
    els = fix(els)
    G.add("gas_line_heater", 'new BlockPumpUnit("gas_line_heater", new UtilityBoxSpec(2, 1, 2, '
          'new AxisAlignedBB(%s), null), BlockPumpUnit.Nozzles.FRONT_BACK, BlockWaterPipe.GAS)'
          % unit_java(els),
          names_of("Gas Line Heater", "Gas-Vorwärmer", "Calentador de Línea de Gas",
                   "Gasförvärmare"),
          {"gas_line_heater": model(tex, els, big_display(0.42))},
          lc.facing_state(G.M("gas_line_heater")), tab=TAB)


def odorant_tank():
    """The odorant tank: the horizontal tank on saddles in its containment pan, two blocks long,
    with the injection pump's cabinet on a post at the root's end and its tubing to the tank."""
    tex = {"tank": W("poly"), "dark": W("dark"), "steel": W("steel"), "pan": W("black"),
           "panel": W("panel_grey"), "display": W("flow_display"), "nfpa": W("nfpa"),
           "label": G.T("odorant_label"), "particle": W("poly")}
    cy, r = 9.6, 5.2
    els = [box([-15.5, 0, 1], [15.5, 0.6, 15], "pan"),
           box([-15.5, 0.6, 1], [15.5, 3.2, 1.6], "pan"),
           box([-15.5, 0.6, 14.4], [15.5, 3.2, 15], "pan"),
           box([-15.5, 0.6, 1.6], [-14.9, 3.2, 14.4], "pan"),
           box([14.9, 0.6, 1.6], [15.5, 3.2, 14.4], "pan")]
    for x in (-10.2, 1.0):
        els.append(box([x, 0.6, 4.0], [x + 1.2, cy - r + 0.3, 12.0], "dark"))
    els += octagon("x", cy, 8, r, -13.0, 5.0, "tank", False, False)
    els += octagon("x", cy, 8, r - 1.2, -13.8, -13.0, "tank", True, False)
    els += octagon("x", cy, 8, r - 1.2, 5.0, 5.8, "tank", False, True)
    els += octagon("y", -4.0, 8, 1.8, cy + r - 0.2, cy + r + 1.0, "tank", False, True,
                   shift=(-16, 0, 0))
    els += octagon("y", 1.6, 8, 0.6, cy + r - 0.2, 16, "steel", False, False)
    els += octagon("y", 1.6, 8, 0.6, 16, 17.6, "steel", False, True, shift=UP)
    face_z = 8 - r - 0.04
    els.append(plane_n(-12.0, 7.6, -4.0, 11.6, face_z, "label", [0, 0, 16, 8]))
    els.append(plane_n(-2.6, 7.4, 1.4, 11.4, face_z, "nfpa", [0, 0, 16, 16]))
    # the injection pump's cabinet on its post, and its tubing to the tank
    els.append(box([11.4, 0.6, 7.4], [12.6, 9.0, 8.6], "steel",
                   faces=("north", "south", "east", "west")))
    els.append(box([8.8, 9.0, 4.8], [15.0, 16.0, 11.2], "panel",
                   faces=("north", "south", "east", "west", "down")))
    els.append(box([8.8, 16.0, 4.8], [15.0, 17.4, 11.2], "panel", shift=UP,
                   faces=("north", "south", "east", "west", "up")))
    els.append(plane_n(9.8, 12.2, 14.0, 15.4, 4.76, "display", [0, 2, 16, 13]))
    els.append(box([5.6, 10.6, 7.7], [8.8, 11.2, 8.3], "steel"))
    els = fix(els)
    G.add("gas_odorant_tank", 'new BlockUtilityBox("gas_odorant_tank", new UtilityBoxSpec(2, 1, '
          '2, new AxisAlignedBB(%s), null))' % unit_java(els),
          names_of("Gas Odorant Tank", "Gas-Odoriermittelbehälter", "Tanque de Odorizante de Gas",
                   "Luktämnestank för Gas"),
          {"gas_odorant_tank": model(tex, els, big_display(0.42))},
          lc.facing_state(G.M("gas_odorant_tank")), tab=TAB)


# ------------------------------------------------------------------------------------------
# Signs: one 128 sheet of six, hung on a wall or on a chain-link fence
# ------------------------------------------------------------------------------------------
SIGN_W, SIGN_H = 64, 42      # one sign's cell on the sheet, in texels
FLAME = ["....#....",
         "....##...",
         "...##....",
         "...###...",
         "..####.#.",
         "..#####..",
         ".####.##.",
         ".###..##.",
         ".##....#.",
         ".##...##.",
         "..##.##..",
         "...###...",
         "#########"]


def flame(img, x0, y0, colour):
    """The generic flame pictogram, 9 x 13 texels."""
    for y, row in enumerate(FLAME):
        for x, c in enumerate(row):
            if c == "#":
                img.putpixel((x0 + x, y0 + y), lc.clamp(colour) + (255,))


def triangle_flame(img, x0, y0):
    """A warning triangle (yellow, black edge) round the flame, 21 x 19 texels."""
    d = ImageDraw.Draw(img)
    d.polygon([(x0 + 10, y0), (x0 + 20, y0 + 18), (x0, y0 + 18)], fill=(20, 20, 22, 255))
    d.polygon([(x0 + 10, y0 + 3), (x0 + 17, y0 + 16), (x0 + 3, y0 + 16)],
              fill=(246, 204, 30, 255))
    flame(img, x0 + 6, y0 + 4, (20, 20, 22))


def no_flame(img, x0, y0):
    """The flame in a red circle with its bar: no open flames, 21 x 21 texels."""
    flame(img, x0 + 6, y0 + 4, (20, 20, 22))
    cx, cy = x0 + 10.5, y0 + 10.5
    for y in range(y0, y0 + 21):
        for x in range(x0, x0 + 21):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if 8.2 <= d <= 10.4 or (d < 9 and abs(dx - dy) < 1.6):
                img.putpixel((x, y), (206, 30, 28, 255))


def text_lines(img, x0, lines, y0, colour, centre=None, step=7):
    for i, t in enumerate(lines):
        if centre is not None:
            lc.draw_text_centred(img, t, centre, y0 + step * i, colour)
        else:
            lc.draw_text(img, t, x0, y0 + step * i, colour)


def header(img, x0, y0, colour, text, ink, scale=2, h=12):
    lc.rect(img, x0 + 1, y0 + 1, x0 + SIGN_W - 1, y0 + h, colour)
    th = 5 * scale
    lc.draw_text_centred(img, text, x0 + SIGN_W / 2, y0 + 1 + (h - 1 - th) // 2 + 0, ink, scale)


BLACK = (22, 22, 24)
WHITE = (246, 246, 242)
SIGNS = [
    # registry, names, drawn by
    ("gas_sign_warning",
     names_of("Gas Warning Sign", "Gas-Warnschild", "Señal de Advertencia de Gas",
              "Varningsskylt för Gas")),
    ("gas_sign_no_smoking",
     names_of("No Smoking Sign", "Rauchverbotsschild", "Señal de Prohibido Fumar",
              "Rökförbudsskylt")),
    ("gas_sign_emergency",
     names_of("Gas Emergency Sign", "Gas-Notfallschild", "Señal de Emergencia de Gas",
              "Nödskylt för Gas")),
    ("gas_sign_station",
     names_of("Regulator Station Sign", "Reglerstationsschild",
              "Señal de Estación Reguladora", "Regulatorstationsskylt")),
    ("gas_sign_authorized",
     names_of("Authorized Personnel Sign", "Schild Nur für Befugte",
              "Señal de Solo Personal Autorizado", "Skylt Endast Behörig Personal")),
    ("cell_site_sign",
     names_of("Cell Site Sign", "Mobilfunkstandort-Schild", "Señal de Sitio Celular",
              "Skylt för Mobilmast")),
]


@S.texture("signs")
def _signs():
    """Six 64 x 42 signs, two across and three down: the gas yard's warning, no smoking,
    emergency contact, station nameplate and authorised personnel signs, and a cell site's
    identification sign. Invented utility names and numbers from the 555-01xx range."""
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for i in range(6):
        x0, y0 = (i % 2) * SIGN_W, (i // 2) * SIGN_H
        lc.rect(img, x0, y0, x0 + SIGN_W, y0 + SIGN_H, WHITE)
        lc.frame(img, x0, y0, x0 + SIGN_W, y0 + SIGN_H, (40, 40, 44))
        if i == 0:
            header(img, x0, y0, (240, 122, 22), "WARNING", BLACK)
            triangle_flame(img, x0 + 3, y0 + 17)
            text_lines(img, x0 + 27, ["HIGH", "PRESSURE", "GAS", "PIPING"], y0 + 14, BLACK)
        elif i == 1:
            header(img, x0, y0, (200, 30, 28), "DANGER", WHITE)
            no_flame(img, x0 + 3, y0 + 16)
            text_lines(img, x0 + 27, ["NO", "SMOKING", "NO OPEN", "FLAMES"], y0 + 14, BLACK)
        elif i == 2:
            header(img, x0, y0, (200, 30, 28), "EMERGENCY", WHITE, scale=1, h=10)
            text_lines(img, 0, ["OAKDALE GAS", "CALL", "1-800-555-0142", "STATION R-14"],
                       y0 + 12, BLACK, centre=x0 + SIGN_W / 2)
        elif i == 3:
            lc.rect(img, x0 + 1, y0 + 11, x0 + SIGN_W - 1, y0 + SIGN_H - 1, (28, 60, 128))
            lc.draw_text_centred(img, "OAKDALE GAS", x0 + SIGN_W / 2, y0 + 3, (28, 60, 128))
            text_lines(img, 0, ["NATURAL GAS", "REGULATOR", "STATION R-14", "NO TRESPASSING"],
                       y0 + 13, WHITE, centre=x0 + SIGN_W / 2)
        elif i == 4:
            header(img, x0, y0, (30, 82, 170), "NOTICE", WHITE)
            text_lines(img, 0, ["AUTHORIZED", "PERSONNEL", "ONLY"], y0 + 16, BLACK,
                       centre=x0 + SIGN_W / 2, step=8)
        else:
            header(img, x0, y0, TEAL, "WIRELESS SITE", WHITE, scale=1, h=10)
            text_lines(img, 0, ["CEDARWAVE", "SITE OAK-0147", "NOC", "1-800-555-0187"],
                       y0 + 12, BLACK, centre=x0 + SIGN_W / 2)
        lc.frame(img, x0, y0, x0 + SIGN_W, y0 + SIGN_H, (40, 40, 44))
    return img


def sign_uv(i):
    u0, v0 = (i % 2) * SIGN_W / 8.0, (i // 2) * SIGN_H / 8.0
    return [u0, round(v0, 4), u0 + SIGN_W / 8.0, round(v0 + SIGN_H / 8.0, 4)]


SIGN_X, SIGN_Y = (2.0, 14.0), (5.0, 13.0)    # the plate, 12 x 8 px
FENCE_Z = 21.0                               # in front of a chain-link post, behind the mesh


def signs():
    """Each sign is a plate facing the player: on a wall at the wall's face, on a fence
    (BlockUtilitySign.FENCE) reaching back into the fence's block to sit in front of its posts,
    wired to the mesh."""
    x0, x1 = SIGN_X
    y0, y1 = SIGN_Y
    tex = {"back": W("aluminium"), "dark": W("dark"), "face": S.T("signs"),
           "particle": W("aluminium")}
    wall = [box([x0, y0, 15.4], [x1, y1, 16], "back", faces=("east", "west", "up", "down"))]
    fence = [box([x0, y0, FENCE_Z], [x1, y1, FENCE_Z + 0.6], "back",
                 faces=("east", "west", "up", "down", "south"), shift=(0, 0, 16))]
    for x in (x0 + 0.8, x1 - 1.1):
        for y in (y0 + 0.8, y1 - 1.1):
            fence.append(box([x, y, FENCE_Z + 0.6], [x + 0.3, y + 0.3, 24.0], "dark",
                             faces=("north", "south", "east", "west", "up", "down"),
                             shift=(0, 0, 16)))
    fence = fix(fence)
    models = {"sign_wall": model(tex, wall), "sign_fence": model(tex, fence)}
    for i, (reg, names) in enumerate(SIGNS):
        uv = sign_uv(i)
        face_w = [plane_n(x0, y0, x1, y1, 15.4, "face", uv)]
        face_f = fix([box([x0, y0, FENCE_Z], [x1, y1, FENCE_Z], "face", faces=("north",),
                          uv={"north": uv}, shift=(0, 0, 16))])
        m = dict(models) if i == 0 else {}
        m[reg + "_wall"] = model(tex, face_w)
        m[reg + "_fence"] = model(tex, face_f)
        rules = facing_rules(S, [("sign_wall", {"fence": "false"}),
                                 ("sign_fence", {"fence": "true"}),
                                 (reg + "_wall", {"fence": "false"}),
                                 (reg + "_fence", {"fence": "true"})])
        S.add(reg, 'new BlockUtilitySign("%s", %s)' % (reg, box_java(wall + face_w)), names, m,
              multipart(S, rules), item=model(tex, wall + face_w, ICON_DISPLAY), tab=TAB)


# ------------------------------------------------------------------------------------------
# Telecom textures
# ------------------------------------------------------------------------------------------
K.textures["cabinet"] = lambda: lc.fill(CABINET, 16, 2, 701)
K.textures["canister"] = lambda: _canister()
K.textures["radio"] = lambda: _radio()


@K.texture("cabinet_louvre")
def _cabinet_louvre():
    img = lc.fill(CABINET, 16, 2, 702)
    for y in range(2, 14, 2):
        for x in range(2, 14):
            img.putpixel((x, y), lc.shade(CABINET, 0.58) + (255,))
    return img


@K.texture("galv")
def _galv():
    """Hot-dip galvanised steel: a mottled grey with brighter spangle."""
    img = lc.fill(GALV, 16, 5, 711)
    for x, y in ((3, 2), (11, 5), (6, 9), (13, 12), (1, 13), (9, 1), (4, 6), (15, 8)):
        img.putpixel((x, y), lc.shade(GALV, 1.14) + (255,))
    return img


@K.texture("radome")
def _radome():
    """A panel antenna's radome, facing out: pale grey with its end caps darker."""
    img = lc.fill(RADOME, 16, 2, 721)
    for x in range(16):
        for y in (0, 15):
            img.putpixel((x, y), lc.shade(RADOME, 0.7) + (255,))
    for y in range(16):
        img.putpixel((0, y), lc.shade(RADOME, 0.86) + (255,))
        img.putpixel((15, y), lc.shade(RADOME, 0.86) + (255,))
    return img


def _radio():
    """A remote radio's cooling fins, running up its sides."""
    img = lc.fill(RADIO, 16, 2, 731)
    for x in range(0, 16, 2):
        for y in range(16):
            img.putpixel((x, y), lc.shade(RADIO, 0.7) + (255,))
    return img


def _canister():
    """A small cell's radome: dark grey with a lighter seam every quarter."""
    img = lc.fill(CANISTER, 16, 2, 741)
    for y in range(16):
        img.putpixel((0, y), lc.shade(CANISTER, 1.2) + (255,))
    return img


@K.texture("cable")
def _cable():
    """Coax and fibre in a tray, seen from above: black runs with a grey one."""
    img = lc.fill((30, 30, 32), 16, 3, 751)
    for x in range(16):
        for y in (3, 8, 13):
            img.putpixel((x, y), (16, 16, 18, 255))
        img.putpixel((x, 6), (110, 112, 116, 255))
    return img


@K.texture("bridge_grating")
def _bridge_grating():
    """The ice bridge's canopy grating: bearing bars every two texels and a cross bar every
    eight, cut out, so the sky shows through from below."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if x % 2 == 0 or y % 8 == 0:
                img.putpixel((x, y), lc.shade(GALV, 0.95 if x % 2 == 0 else 1.1) + (255,))
    return img


@K.texture("ac_grille")
def _ac_grille():
    """A door-mounted air conditioner's front: the condenser grille and the intake below."""
    img = lc.fill(CABINET, 16, 2, 761)
    lc.rect(img, 2, 2, 14, 9, (60, 62, 66))
    for x in range(3, 13, 2):
        lc.rect(img, x, 3, x + 1, 8, (120, 124, 128))
    for y in range(11, 15, 2):
        lc.rect(img, 2, y, 14, y + 1, lc.shade(CABINET, 0.6))
    return img


def _door_panel(img, x0, y0, w, h, louvres=False, label=None):
    lc.bevel(img, x0, y0, x0 + w, y0 + h, CABINET, 1.1, 0.8)
    lc.frame(img, x0, y0, x0 + w, y0 + h, lc.shade(CABINET, 0.62))
    if louvres:
        for y in range(y0 + 4, y0 + 12, 2):
            lc.rect(img, x0 + 3, y, x0 + w - 3, y + 1, lc.shade(CABINET, 0.55))
        for y in range(y0 + h - 12, y0 + h - 4, 2):
            lc.rect(img, x0 + 3, y, x0 + w - 3, y + 1, lc.shade(CABINET, 0.55))
    if label:
        lc.rect(img, x0 + 3, y0 + h // 2 - 8, x0 + w - 3, y0 + h // 2 - 1, (246, 206, 40))
        lc.draw_text_centred(img, label, x0 + w / 2, y0 + h // 2 - 7, (22, 22, 24))


@K.texture("fronts")
def _fronts():
    """The cabinets' fronts on one 128 sheet (8 texels a sixteenth):
    the fibre cabinet's pair of doors at (0, 0) 56 x 42 and its inside, splice trays and patch
    panels, at (0, 42) 56 x 42; the equipment cabinet's door at (56, 0) 26 x 54 and the battery
    cabinet's louvred door at (82, 0) 26 x 54."""
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    lc.rect(img, 0, 0, 128, 128, CABINET)
    # the fibre cabinet's doors: two leaves, a handle each at the meeting stile
    _door_panel(img, 0, 0, 28, 42, louvres=True)
    _door_panel(img, 28, 0, 28, 42, louvres=True)
    lc.rect(img, 25, 18, 27, 25, (60, 62, 66))
    lc.rect(img, 29, 18, 31, 25, (60, 62, 66))
    lc.rect(img, 33, 15, 55, 21, (246, 206, 40))
    lc.draw_text(img, "FIBER", 35, 16, (22, 22, 24))
    # its inside: splice tray shelves on the left, patch panels on the right
    y0 = 42
    lc.rect(img, 0, y0, 56, y0 + 42, (48, 50, 54))
    for i in range(7):
        y = y0 + 3 + i * 5
        lc.rect(img, 2, y, 26, y + 4, (206, 208, 210))
        lc.rect(img, 3, y + 1, 25, y + 2, (160, 164, 168))
    for i in range(6):
        y = y0 + 3 + i * 6
        lc.rect(img, 30, y, 54, y + 4, (30, 30, 34))
        for x in range(31, 54, 2):
            lc.rect(img, x, y + 1, x + 1, y + 3, (40, 110, 200) if (x + i) % 4 else (60, 180, 80))
    for x in (5, 9, 13, 17, 21):
        lc.rect(img, x, y0 + 38, x + 1, y0 + 42, (240, 200, 40))
    # the equipment and battery cabinets' doors
    _door_panel(img, 56, 0, 26, 54)
    lc.rect(img, 78, 28, 80, 36, (60, 62, 66))
    _door_panel(img, 82, 0, 26, 54, louvres=True, label="DC")
    lc.rect(img, 104, 28, 106, 36, (60, 62, 66))
    return img


# ------------------------------------------------------------------------------------------
# Telecom cabinets
# ------------------------------------------------------------------------------------------
CAB_TEX = {"cabinet": K.T("cabinet"), "louvre": K.T("cabinet_louvre"), "fronts": K.T("fronts"),
           "concrete": W("concrete"), "dark": W("dark"), "ac": K.T("ac_grille"),
           "particle": K.T("cabinet")}


def fdh_cabinet():
    """The fibre distribution cabinet (BlockCabinet): pad-mounted, two blocks wide, its two
    doors opening (a click) on the splice trays and patch panels."""
    top = 22.0
    body = [box([-15.5, 0, 1], [15.5, 1, 15], "concrete"),
            box([-14, 1, 4], [14, 16, 12], "cabinet", faces=("south", "east", "west"),
                per={"east": "louvre", "west": "louvre"}),
            box([-14, 16, 4], [14, top, 12], "cabinet", faces=("south", "east", "west"),
                shift=UP),
            box([-14.4, top, 3.6], [14.4, top + 1, 12.4], "cabinet", shift=UP)]
    # the opening's jambs, drawn whichever way the doors are
    body += [box([-14, 1, 4], [-13.4, top, 4.6], "cabinet", faces=("north", "east")),
             box([13.4, 1, 4], [14, top, 4.6], "cabinet", faces=("north", "west")),
             box([-13.4, top - 0.8, 4], [13.4, top, 4.6], "cabinet", faces=("north", "down")),
             box([-13.4, 1, 4], [13.4, 1.8, 4.6], "cabinet", faces=("north", "up"))]
    body = fix(body)
    doors_uv = [0, 0, 7, 5.25]
    shut = [box([-14, 1, 3.6], [14, top, 4.0], "cabinet", faces=("north", "up", "down"),
                uv={"north": doors_uv}, per={"north": "fronts"}),
            box([-1.4, 10, 3.2], [-0.6, 13, 3.6], "dark"),
            box([0.6, 10, 3.2], [1.4, 13, 3.6], "dark")]
    shut = fix(shut)
    w = 14.0
    inside = [plane_n(-13.4, 1.8, 13.4, top - 0.8, 4.6, "fronts", [0, 5.25, 7, 10.5])]
    opened = [box([-14.4, 1, 4 - w], [-14, top, 4], "cabinet",
                  faces=("west", "east", "north", "up", "down"),
                  uv={"west": [3.5, 0, 7, 5.25]}, per={"west": "fronts"},
                  shift=(0, 0, -16)),
              box([14, 1, 4 - w], [14.4, top, 4], "cabinet",
                  faces=("west", "east", "north", "up", "down"),
                  uv={"east": [0, 0, 3.5, 5.25]}, per={"east": "fronts"},
                  shift=(0, 0, -16))]
    opened = fix(opened)
    models = {"fdh_body": model(CAB_TEX, body), "fdh_shut": model(CAB_TEX, shut),
              "fdh_open": model(CAB_TEX, opened + inside)}
    rules = facing_rules(K, [("fdh_body", {}), ("fdh_shut", {"on": "false"}),
                             ("fdh_open", {"on": "true"})])
    models["fdh_icon"] = model(CAB_TEX, body + shut, big_display(0.42))
    K.add("fiber_distribution_cabinet", 'new BlockCabinet("fiber_distribution_cabinet", new '
          'UtilityBoxSpec(2, 1, 2, new AxisAlignedBB(%s), null))' % unit_java(body + shut),
          names_of("Fiber Distribution Cabinet", "Glasfaser-Verteilerschrank",
                   "Gabinete de Distribución de Fibra", "Fiberskåp"),
          models, multipart(K, rules, inventory="fdh_icon"), tab=TAB)


def cell_cabinets():
    """A cell site's equipment cabinet (its door-mounted air conditioner) and battery cabinet
    (louvred): one body, a door each, Roads' utility box a block wide and two tall."""
    top = 28.0
    body = [box([0.5, 0, 0.5], [15.5, 1, 15.5], "concrete"),
            box([1.5, 1, 3], [14.5, 16, 15], "cabinet", faces=("south", "east", "west"),
                per={"east": "louvre"}),
            box([1.5, 16, 3], [14.5, top, 15], "cabinet", faces=("south", "east", "west"),
                shift=UP),
            box([1, top, 2.4], [15, top + 1, 15.6], "cabinet", shift=UP)]
    body = fix(body)

    def door(u0):
        return fix([box([1.5, 1, 2.6], [14.5, top, 3], "cabinet",
                        faces=("north", "east", "west", "up", "down"),
                        uv={"north": [u0, 0, u0 + 3.25, 6.75]}, per={"north": "fronts"}),
                    box([12.6, 13.2, 2.2], [13.2, 16.8, 2.6], "dark")])

    eq = door(7.0) + fix([box([3.5, 13, 1.2], [11.5, 16, 2.6], "cabinet",
                              faces=("north", "east", "west", "down"), per={"north": "ac"},
                              uv={"north": [0, 12, 16, 16]}),
                          box([3.5, 16, 1.2], [11.5, 25, 2.6], "cabinet",
                              faces=("north", "east", "west", "up"), per={"north": "ac"},
                              uv={"north": [0, 0, 16, 12]}, shift=UP)])
    bat = door(10.25)
    models = {"cab_body": model(CAB_TEX, body), "cab_door_equipment": model(CAB_TEX, eq),
              "cab_door_battery": model(CAB_TEX, bat)}
    for reg, door_name, names in (
            ("cell_equipment_cabinet", "cab_door_equipment",
             names_of("Cell Site Equipment Cabinet", "Mobilfunk-Technikschrank",
                      "Gabinete de Equipos de Sitio Celular", "Utrustningsskåp för Mobilmast")),
            ("cell_battery_cabinet", "cab_door_battery",
             names_of("Cell Site Battery Cabinet", "Mobilfunk-Batterieschrank",
                      "Gabinete de Baterías de Sitio Celular", "Batteriskåp för Mobilmast"))):
        els = body + models[door_name]["elements"]
        m = dict(models) if reg == "cell_equipment_cabinet" else {}
        m[reg + "_icon"] = model(CAB_TEX, els, big_display(0.5))
        K.add(reg, 'new BlockUtilityBox("%s", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(%s), '
              'null))' % (reg, unit_java(els)), names, m,
              multipart(K, facing_rules(K, [("cab_body", {}), (door_name, {})]),
                        inventory=reg + "_icon"), tab=TAB)


# ------------------------------------------------------------------------------------------
# The ice bridge and the stanchions; the GPS antenna
# ------------------------------------------------------------------------------------------
BRIDGE_TEX = {"grating": K.T("bridge_grating"), "steel": K.T("galv"), "cable": K.T("cable"),
              "concrete": W("concrete"), "dark": W("dark"), "particle": K.T("galv")}
POST_R = 1.4
BEAM_Y = 10.0


def column(cat, reg, r, footing, cap, names, java, tex, item_extra=()):
    """A BlockTowerColumn: its shaft, and the footing and cap it shows at its ends."""
    shaft_name, foot_name, cap_name = reg + "_shaft", reg + "_footing", reg + "_cap"
    shaft = gw.polygon16(8, 8, r, 0, 16, "shaft") if r > 3 else octagon("y", 8, 8, r, 0, 16,
                                                                          "shaft", False, False)
    models = {shaft_name: model(tex, shaft), foot_name: model(tex, footing),
              cap_name: model(tex, cap)}
    rules = [rule(cat, shaft_name), rule(cat, foot_name, {"base": True}),
             rule(cat, cap_name, {"top": True})]
    cat.add(reg, java, names, models, multipart(cat, rules),
            item=model(tex, shaft + footing + cap + list(item_extra), ICON_DISPLAY), tab=TAB)
    return shaft


def ice_bridge():
    """The cable ice bridge (BlockIceBridge), drawn running along z: the grating canopy on its
    side rails, the cable tray with its cables under it, a strap tying the two every block, the
    rails' ends where the run stops, and over a stanchion the stanchion's top and its T-beam."""
    canopy = [box([1.2, 14.8, 0], [14.8, 15.2, 16], "grating", faces=("up", "down"),
                  uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]})]
    for x in (0.8, 14.2):
        canopy.append(box([x, 14.0, 0], [x + 1.0, 15.6, 16], "steel",
                          faces=("up", "down", "east", "west")))
    tray = []
    for x in (3.4, 11.8):
        tray.append(box([x, 11.2, 0], [x + 0.8, 13.2, 16], "steel",
                        faces=("up", "down", "east", "west")))
    tray.append(box([4.2, 11.4, 0], [11.8, 12.6, 16], "cable", faces=("up", "down")))
    tray.append(box([1.0, 13.4, 7.6], [15.0, 14.0, 8.4], "steel"))
    run = canopy + tray

    def ends(face):
        els = [box([x, 14.0, 0], [x + 1.0, 15.6, 16], "steel", faces=(face,))
               for x in (0.8, 14.2)]
        els += [box([x, 11.2, 0], [x + 0.8, 13.2, 16], "steel", faces=(face,))
                for x in (3.4, 11.8)]
        els.append(box([4.2, 11.4, 0], [11.8, 12.6, 16], "cable", faces=(face,)))
        return els

    post = octagon("y", 8, 8, POST_R, 0, BEAM_Y, "steel", False, False)
    post += [box([1.0, BEAM_Y, 7.0], [15.0, BEAM_Y + 1.2, 9.0], "steel"),
             box([1.1, BEAM_Y + 1.2, 7.6], [1.7, 14.0, 8.4], "steel",
                 faces=("north", "south", "east", "west")),
             box([14.3, BEAM_Y + 1.2, 7.6], [14.9, 14.0, 8.4], "steel",
                 faces=("north", "south", "east", "west"))]
    models = {"bridge_run": model(BRIDGE_TEX, run), "bridge_end_n": model(BRIDGE_TEX,
                                                                          ends("north")),
              "bridge_end_s": model(BRIDGE_TEX, ends("south")),
              "bridge_post": model(BRIDGE_TEX, post)}
    rules = []
    for name, cond in (("bridge_run", {}), ("bridge_end_n", {"ahead": False}),
                       ("bridge_end_s", {"behind": False}), ("bridge_post", {"post": True})):
        for axis, y in (("z", 0), ("x", 90)):
            w = {"axis": axis}
            w.update(cond)
            rules.append(rule(K, name, w, y))
    K.add("ice_bridge", 'new BlockIceBridge("ice_bridge")',
          names_of("Cable Ice Bridge", "Kabel-Eisschutzbrücke", "Puente de Cables",
                   "Kabelbrygga"),
          models, multipart(K, rules),
          item=model(BRIDGE_TEX, run + ends("north") + ends("south"), ICON_DISPLAY), tab=TAB)

    tex = dict(BRIDGE_TEX, shaft=K.T("galv"))
    foot = octagon("y", 8, 8, 3.2, 0, 2.0, "concrete", False, True)
    foot.append(box([5.4, 2.0, 5.4], [10.6, 2.5, 10.6], "steel"))
    cap = octagon("y", 8, 8, POST_R + 0.4, 15.2, 16, "steel", True, True)
    column(K, "ice_bridge_stanchion", POST_R, foot, cap,
           names_of("Ice Bridge Stanchion", "Eisschutzbrücken-Stütze",
                    "Montante de Puente de Cables", "Kabelbryggsstolpe"),
           'new BlockTowerColumn("ice_bridge_stanchion", %s)' % fmt(POST_R), tex)


def gps_antenna():
    """The GPS timing antenna on its wall bracket: a white puck on a short mast."""
    tex = {"steel": K.T("galv"), "puck": W("poly"), "dark": W("dark"), "particle": W("poly")}
    els = [box([6.2, 2.0, 15.4], [9.8, 8.0, 16.0], "steel"),
           box([7.4, 4.0, 10.6], [8.6, 5.2, 15.4], "steel")]
    els += octagon("y", 8, 10, 0.7, 5.2, 11.0, "steel", False, False)
    els += octagon("y", 8, 10, 0.9, 4.0, 5.2, "steel", True, True)
    els += octagon("y", 8, 10, 1.9, 11.0, 12.2, "puck", True, True)
    els += octagon("y", 8, 10, 1.2, 12.2, 12.7, "puck", False, True)
    els.append(box([7.8, 2.4, 10.4], [8.2, 4.0, 10.8], "dark"))
    K.add("gps_antenna", 'new BlockUtilityFixture("gps_antenna", %s)' % box_java(els),
          names_of("GPS Antenna", "GPS-Antenne", "Antena GPS", "GPS-antenn"),
          {"gps_antenna": model(tex, els)}, lc.facing_state(K.M("gps_antenna")), tab=TAB)


# ------------------------------------------------------------------------------------------
# The monopole and its antenna array (OBJ, three sectors at 120 degrees)
# ------------------------------------------------------------------------------------------
MONO_R = 6.0
MTL = "telecom"
MATERIALS = {"galv": K.T("galv"), "radome": K.T("radome"), "radio": K.T("radio"),
             "dark": W("dark")}
OBJ_QUADS = {}


class Obj(gw.Obj):
    def text(self):
        return gw.Obj.text(self).replace("gen_utilities_water.py", GEN, 1)


def write_obj(name, polys):
    o = Obj(MTL, name)
    for p in polys:
        o.add(p)
    K.extra["models/block/utilities/telecom/%s.obj" % name] = o.text()
    OBJ_QUADS[name] = o.count
    return "csm:utilities/telecom/%s.obj" % name


def _mtl():
    text = ["# Generated by dev-env-utils/scripts/%s -- do not edit" % GEN]
    for mat, tex in MATERIALS.items():
        text += ["newmtl " + mat, "map_Kd " + tex]
    K.extra["models/block/utilities/telecom/%s.mtl" % MTL] = "\n".join(text) + "\n"


_mtl()


def obox(phi, t0, t1, w0, w1, y0, y1, mat, faces=("t0", "t1", "w0", "w1", "y0", "y1")):
    """A box in a sector's frame, in blocks: t along the face (tangent), w out from the pole
    (the sector's outward normal at angle phi, 0 = north, clockwise seen from above), y up;
    about the cell's centre. Each face's uv is the whole texture."""
    n = (math.sin(phi), 0.0, -math.cos(phi))
    t = (math.cos(phi), 0.0, math.sin(phi))

    def p(tt, ww, yy):
        return (0.5 + t[0] * tt + n[0] * ww, yy, 0.5 + t[2] * tt + n[2] * ww)

    out = []
    spec = {
        "t0": ([(t0, w1, y1), (t0, w0, y1), (t0, w0, y0), (t0, w1, y0)], (-t[0], 0, -t[2])),
        "t1": ([(t1, w0, y1), (t1, w1, y1), (t1, w1, y0), (t1, w0, y0)], t),
        "w1": ([(t0, w1, y1), (t1, w1, y1), (t1, w1, y0), (t0, w1, y0)], n),
        "w0": ([(t1, w0, y1), (t0, w0, y1), (t0, w0, y0), (t1, w0, y0)], (-n[0], 0, -n[2])),
        "y1": ([(t0, w0, y1), (t1, w0, y1), (t1, w1, y1), (t0, w1, y1)], (0, 1, 0)),
        "y0": ([(t0, w1, y0), (t1, w1, y0), (t1, w0, y0), (t0, w0, y0)], (0, -1, 0)),
    }
    uvs = [(0, 0), (1, 0), (1, 1), (0, 1)]
    for f in faces:
        corners, normal = spec[f]
        normal = tuple(float(v) for v in normal)
        out.append(Poly([p(*c) for c in corners], uvs, [normal] * 4, mat, outward=normal))
    return out


SECTOR_D = 0.8          # the frame's face pipes, out from the pole's axis, in blocks
SECTOR_L = SECTOR_D * math.tan(math.pi / 3)    # half a face's length: the triangle's corners
ANT_W, ANT_D = 0.3, 0.1
ANT_Y = (0.05, 1.65)


def array_polys():
    """Three sectors at 0, 120 and 240 degrees: two face pipes and two arms from the pole a
    sector, and on each face three panel antennas on their mount pipes with a radio behind
    each."""
    polys = []
    side = ("t0", "t1", "w0", "w1")
    for k in range(3):
        phi = k * 2 * math.pi / 3
        for y in (0.3, 1.4):
            # each sector's pipes a hair higher than the last, so where two cross at a corner
            # their tops and bottoms are not in one plane
            yk = y + 0.004 * k
            polys += obox(phi, -SECTOR_L, SECTOR_L, SECTOR_D - 0.04, SECTOR_D + 0.04, yk - 0.04,
                          yk + 0.04, "galv", faces=("w0", "w1", "y0", "y1"))
            polys += obox(phi, -0.05, 0.05, MONO_R / 16 - 0.02, SECTOR_D - 0.04, y - 0.05,
                          y + 0.05, "galv", faces=("t0", "t1", "y0", "y1"))
        for j in (-1, 0, 1):
            c = j * SECTOR_L * 0.62
            polys += obox(phi, c - 0.03, c + 0.03, SECTOR_D + 0.04, SECTOR_D + 0.1, 0.0, 1.72,
                          "galv", faces=side)
            polys += obox(phi, c - ANT_W / 2, c + ANT_W / 2, SECTOR_D + 0.12,
                          SECTOR_D + 0.12 + ANT_D, ANT_Y[0], ANT_Y[1], "radome")
            polys += obox(phi, c - 0.11, c + 0.11, SECTOR_D - 0.16, SECTOR_D - 0.04, 0.42, 0.76,
                          "radio")
    return polys


def monopole():
    """The monopole (BlockTowerColumn sections, galvanised, a 16-gon) built to height, and its
    antenna array (BlockAntennaArray) placed in the stack: the sector frame is an OBJ, since
    sectors 120 degrees apart are turns no JSON element can make."""
    tex = {"shaft": K.T("galv"), "galv": K.T("galv"), "concrete": W("concrete"),
           "dark": W("dark"), "radome": K.T("radome"), "particle": K.T("galv")}
    foot = octagon("y", 8, 8, 9.4, 0, 3.0, "concrete", False, True)
    foot.append(box([1.4, 3.0, 1.4], [14.6, 3.8, 14.6], "galv"))
    for x in (2.2, 12.8):
        for z in (2.2, 12.8):
            foot.append(box([x, 3.8, z], [x + 1.0, 4.9, z + 1.0], "dark",
                            faces=("north", "south", "east", "west", "up")))
    foot += [box([6.2, 6.0, 1.3], [9.8, 10.4, 2.1], "galv", faces=("north", "east", "west",
                                                                     "up")),
             box([6.2, 6.0, 1.3], [9.8, 6.0, 2.1], "dark", faces=("down",))]
    cap = gw.polygon16(8, 8, MONO_R + 0.4, 15.2, 16, "galv", top=True, bottom=True)
    rod = octagon("y", 8, 8, 0.35, 16, 30, "galv", False, True, shift=UP)
    shaft = column(K, "monopole_section", MONO_R, foot, cap + fix(rod),
                   names_of("Monopole Section", "Monopol-Mastschuss", "Sección de Monoposte",
                            "Monopolsektion"),
                   'new BlockTowerColumn("monopole_section", %s)' % fmt(MONO_R), tex)

    # the array: the shaft through it, the frame, and over it (top) the pole's last length
    top = gw.polygon16(8, 8, MONO_R, 16, 27.2, "galv") + gw.polygon16(
        8, 8, MONO_R + 0.4, 27.2, 28.0, "galv", top=True, bottom=True)
    top += octagon("y", 8, 8, 0.35, 28.0, 31.9, "galv", False, True, shift=UP)
    top = fix(top)
    frame = write_obj("monopole_array", array_polys())
    models = {"array_top": model(tex, top)}
    rules = [rule(K, "monopole_section_shaft")]
    rules += facing_rules(K, [(frame, {}), ("array_top", {"top": "true"})])
    # an OBJ is no item model's parent here, so the icon draws the frame in JSON: a sector's
    # face and its antennas in front
    icon = octagon("y", 8, 8, 2.6, 0, 16, "galv", True, True)
    for x, z in ((2.2, 6.0), (6.6, 2.4), (11.0, 6.0)):
        icon.append(box([x, 1.5, z], [x + 2.8, 15.0, z + 0.9], "radome"))
        icon.append(box([x + 0.8, 6.0, z + 0.9], [x + 2.0, 9.0, z + 1.8], "dark"))
    for y in (3.0, 13.0):
        icon.append(box([1.4, y, 7.0], [14.6, y + 0.7, 7.7], "galv"))
        icon.append(box([7.6, y, 3.3], [8.4, y + 0.7, 7.0], "galv"))
    models["monopole_array_icon"] = model(tex, icon, ICON_DISPLAY)
    K.add("monopole_antenna_array",
          'new BlockAntennaArray("monopole_antenna_array", new double[]{%s})'
          % ", ".join(fmt(v) for v in (8 - MONO_R, 0, 8 - MONO_R, 8 + MONO_R, 16, 8 + MONO_R)),
          names_of("Monopole Antenna Array", "Monopol-Antennenträger",
                   "Conjunto de Antenas de Monoposte", "Monopolens Antennsystem"),
          models, multipart(K, rules, inventory="monopole_array_icon"), tab=TAB)


# ------------------------------------------------------------------------------------------
# The small cell: the canister on a pole's top, the radio on its side
# ------------------------------------------------------------------------------------------
SC_TEX = {"canister": K.T("canister"), "radio": K.T("radio"), "dark": W("dark"),
          "steel": K.T("galv"), "particle": K.T("canister")}
FITS = (("large", 6.0), ("thin", 4.0), ("pedestal", 3.0))
CAN_R = 4.6


def small_cell():
    """The canister antenna (BlockSmallCell) on a pole's top: a collar sleeved over the pole,
    sized to it, a short mast and the radome; and the radio (BlockPoleRadio) on the pole's side,
    its straps reaching back to the skin of the pole behind it."""
    can = gw.polygon16(8, 8, CAN_R, 5.0, 16, "canister")
    can += gw.polygon16(8, 8, CAN_R, 16, 24.0, "canister")
    for e in can:
        if e["from"][1] >= 16 - 0.1:
            for f in e["faces"].values():
                f["uv"] = [f["uv"][0], 0, f["uv"][2], 16]
    can += octagon("y", 8, 8, CAN_R + 0.3, 24.0, 24.8, "canister", True, True, shift=UP)
    can += octagon("y", 8, 8, 1.2, 24.8, 25.6, "dark", False, True, shift=UP)
    can += octagon("y", 8, 8, CAN_R + 0.3, 4.4, 5.0, "dark", True, True)
    can += octagon("y", 8, 8, 1.6, 2.8, 4.4, "steel", False, False)
    can = fix(can)
    models = {"sc_canister": model(SC_TEX, can)}
    rules = [rule(K, "sc_canister")]
    for fit, r in FITS:
        collar = octagon("y", 8, 8, r + 1.0, 0, 2.8, "steel", False, True)
        models["sc_collar_" + fit] = model(SC_TEX, collar)
        rules.append(rule(K, "sc_collar_" + fit, {"polefit": fit}))
    icon = can + octagon("y", 8, 8, 7.0, 0, 2.8, "steel", False, True)
    K.add("small_cell_antenna", 'new BlockSmallCell("small_cell_antenna", new double[]{%s})'
          % ", ".join(fmt(v) for v in (8 - CAN_R, 0, 8 - CAN_R, 8 + CAN_R, 16, 8 + CAN_R)),
          names_of("Small Cell Canister Antenna", "Kleinzellen-Rundantenne",
                   "Antena Cilíndrica de Celda Pequeña", "Småcellsantenn"),
          models, multipart(K, rules), item=model(SC_TEX, icon, ICON_DISPLAY), tab=TAB)

    radio = [box([4.2, 3.0, 6.4], [11.8, 14.0, 11.4], "radio", per={"north": "dark"}),
             box([5.0, 1.8, 7.8], [6.6, 3.0, 9.4], "dark"),
             box([9.4, 1.8, 7.8], [11.0, 3.0, 9.4], "dark"),
             box([5.4, 11.0, 6.2], [10.6, 12.6, 6.4], "steel", faces=("north",))]
    models = {"sc_radio": model(SC_TEX, radio)}
    parts = [("sc_radio", {})]
    for fit, r in FITS:
        skin = 24.0 - r
        wband = 1.6 * r
        strap = []
        for y in (4.6, 11.0):
            strap.append(box([7.2, y, 11.4], [8.8, y + 1.2, skin], "steel",
                             faces=("up", "down", "east", "west"), shift=(0, 0, 16)))
            strap.append(box([8 - wband / 2, y - 0.2, skin - 0.5], [8 + wband / 2, y + 1.4, skin],
                             "steel", shift=(0, 0, 16)))
        models["sc_radio_straps_" + fit] = model(SC_TEX, fix(strap))
        parts.append(("sc_radio_straps_" + fit, {"polefit": fit}))
    K.add("small_cell_radio", 'new BlockPoleRadio("small_cell_radio", %s)' % box_java(radio),
          names_of("Small Cell Radio", "Kleinzellen-Funkeinheit", "Radio de Celda Pequeña",
                   "Småcellsradio"),
          models, multipart(K, facing_rules(K, parts)),
          item=model(SC_TEX, radio, ICON_DISPLAY), tab=TAB)


# ------------------------------------------------------------------------------------------
# Everything, and the command line
# ------------------------------------------------------------------------------------------
gas_pipe()
gas_fittings()
vent_stack()
skid()
line_heater()
odorant_tank()
signs()
fdh_cabinet()
cell_cabinets()
ice_bridge()
gps_antenna()
monopole()
small_cell()


def report():
    for k in sorted(OBJ_QUADS):
        print("%-18s %4d quads" % (k, OBJ_QUADS[k]))


def main():
    if "--report" in sys.argv:
        report()
        return 0
    if "--fragments" in sys.argv:
        for cat in CATALOGUES:
            print(cat.fragments())
        return 0
    status = 0
    for cat in CATALOGUES:
        status = max(status, cat.main())
    return status


if __name__ == "__main__":
    sys.exit(main())
