#!/usr/bin/env python3
"""
gen_venues_track.py -- track and field in the Sports & Venues Courts, Track & Pool tab.

    py -3 dev-env-utils/scripts/gen_venues_track.py                    # write
    py -3 dev-env-utils/scripts/gen_venues_track.py --check            # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_track.py --fragments        # the tab's lines
    py -3 dev-env-utils/scripts/gen_venues_track.py --layout track_400m   # a layout's blocks (JSON)
    py -3 dev-env-utils/scripts/gen_venues_track.py --layout track_400m --commands X Y Z [--rot R]
    py -3 dev-env-utils/scripts/gen_venues_track.py --preview DIR      # pictures of the layouts

A World Athletics 400 m track, eight lanes, at one block a metre, every lane line, stagger,
relay zone and hurdle mark where it really falls, the bends drawn at their true radii; the field
events inside it; the long and triple jump and the pole vault runways. The geometry is
track_layout.py's, painted by venues_lines.py's engine (see gen_venues_courts.py, which writes
the engine's materials): ``track_marking_anchor`` and ``track_painted`` cells, ``track_surface``
(polyurethane in sixteen colours) where no line falls, ``field_turf`` for the grass and
``sports_sand`` for the pits.

Then the equipment: hurdles (four heights), starting blocks, the finish line camera tower, the
lap counter and its bell, the high jump mat with its standards and bar, the pole vault pit with
its standards and bar, the shot put toe board, the discus and hammer cage and the steeplechase
barrier. No brand.
"""
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402
import venues_lines as vl  # noqa: E402
import venues_units as vu  # noqa: E402
import track_layout as tl  # noqa: E402
import gen_venues_courts as gc  # noqa: E402

GEN = "gen_venues_track.py"
C = vc.catalogue(GEN, "track")
T = C.T
M = C.M
U = vu.Units(C, GEN, "track")
box = vc.box
wbox = vc.lsc.pp.wbox
TAB = gc.TAB
FACINGS = vc.FACINGS
ys = vu.ys
LAYOUTS = tl.layouts()

RUBBER = (40, 40, 42)
WHITE = (240, 240, 236)
BLACK = (26, 26, 28)
ALU = (196, 200, 204)
MAT_BLUE = (34, 78, 160)
MAT_TOP = (22, 44, 96)
BRASS = (196, 160, 70)
NET = (220, 222, 224)


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


@C.texture("polyurethane")
def _pu():
    """Polyurethane with its EPDM granules: light grey (the colour is a tint or the vertex
    colour), a strong fleck so it reads as a track up close."""
    return gc.grain((228, 228, 228), 20261301, 0.05,
                    flecks=(((196, 196, 196), 0.12), ((248, 248, 248), 0.06)))


@C.texture("track_side")
def _side():
    return gc.grain(RUBBER, 20261302, 0.1, size=16, flecks=(((70, 70, 72), 0.1),))


@C.texture("painted_icon")
def _painted_icon():
    img = gc.grain((228, 228, 228), 20261303, 0.05)
    for x0 in (6, 22):
        vc.rect(img, x0, 0, x0 + 2, 32, rgba((250, 250, 250)))
    return img


@C.texture("anchor_icon")
def _anchor_icon():
    img = _painted_icon()
    vc.rect(img, 11, 11, 17, 17, rgba((240, 180, 30)))
    return img


@C.texture("hurdle_bar")
def _hurdle_bar():
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        c = BLACK if (x // 4) % 2 == 0 else WHITE
        vc.rect(img, x, 0, x + 1, 16, rgba(c))
    return img


for _n, _c, _s in (("white", WHITE, 1), ("black", BLACK, 2), ("alu", ALU, 3),
                   ("mat_blue", MAT_BLUE, 4), ("mat_top", MAT_TOP, 5), ("brass", BRASS, 6),
                   ("red", (196, 40, 36), 7), ("steel", (110, 114, 120), 8),
                   ("yellow", (240, 200, 40), 9), ("grey", (128, 132, 136), 10),
                   ("camera", (40, 42, 46), 11)):
    C.textures[_n] = (lambda c=_c, s=_s: vc.swatch(c, 20261320 + s))


@C.texture("cage_net")
def _cage_net():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(32):
        for x in range(32):
            if x % 4 == 0 or y % 4 == 0:
                px[x, y] = rgba(vc.shade(NET, 1.0 - 0.06 * ((x + y) % 3)))
    return img


@C.texture("bar_stripe")
def _bar_stripe():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        c = (230, 200, 40) if (y // 4) % 2 == 0 else (30, 30, 30)
        vc.rect(img, 0, y, 16, y + 1, rgba(c))
    return img


@C.texture("mat_piping")
def _mat_piping():
    img = vc.swatch(MAT_TOP, 20261340)
    vc.rect(img, 0, 0, 16, 1, rgba(WHITE))
    return img


# ------------------------------------------------------------------------------------------
# The surface and the line art blocks
# ------------------------------------------------------------------------------------------
C.add("track_surface",
      'new BlockSportSurface("track_surface", BlockArenaFloorBase.Surface.POLYURETHANE, true)',
      ("Track Surface (Polyurethane)", "Laufbahnbelag (Polyurethan)",
       "Superficie de Pista de Atletismo (Poliuretano)", "Löparbanebeläggning (Polyuretan)"),
      {"track_surface": gc.cube(T("polyurethane"), T("track_side"), tint=True)},
      {"forge_marker": 1, "defaults": {"model": M("track_surface")},
       "variants": {"inventory": [{}], "colour": {str(k): {} for k in range(16)}}},
      tab=TAB)
C.add("track_marking_anchor",
      'new BlockLineAnchor("track_marking_anchor", BlockArenaFloorBase.Surface.POLYURETHANE, LineArtFamily.TRACK)',
      ("Track Marking Anchor", "Laufbahnmarkierungs-Anker", "Ancla de Marcado de Pista de Atletismo",
       "Löparbanans Markeringsankare"),
      {"track_anchor": gc.cube(T("anchor_icon"), T("track_side"))},
      {"forge_marker": 1, "defaults": {"model": M("track_anchor")},
       "variants": {"inventory": [{}], "rot": {str(k): {} for k in range(4)}}}, tab=TAB)
C.add("track_painted",
      'new BlockLinePainted("track_painted", BlockArenaFloorBase.Surface.POLYURETHANE, LineArtFamily.TRACK)',
      ("Track Surface (Painted)", "Laufbahnbelag (Markiert)",
       "Superficie de Pista de Atletismo (Pintada)", "Löparbanebeläggning (Målad)"),
      {"track_painted": gc.cube(T("painted_icon"), T("track_side"))},
      {"forge_marker": 1, "defaults": {"model": M("track_painted")},
       "variants": {"inventory": [{}], "normal": [{}]}}, tab=TAB)


# ------------------------------------------------------------------------------------------
# Hurdles and starting blocks (BlockSportFixture)
def icon_state(obj):
    """An OBJ fixture's blockstate whose inventory is a small JSON icon model (``<obj>_icon``)."""
    state = U.obj_state(obj, 1.0)
    state["variants"]["inventory"] = [{"model": M(obj + "_icon"), "custom": {}}]
    return state


# ------------------------------------------------------------------------------------------
HURDLE_PX = [12.19, 13.41, 14.62, 17.07]      # BlockSportFixture.HURDLE_PX
FIX_TEX = {"bar": T("hurdle_bar"), "steel": T("steel"), "black": T("black"), "alu": T("alu"),
           "red": T("red"), "white": T("white"), "particle": T("hurdle_bar")}


def hurdle(h):
    half = 1.18 * 16 / 2
    x0, x1 = 8 - half, 8 + half
    els = [wbox([x0, h - 1.12, 7.6], [x1, h, 8.4], "bar")]
    for xa in (x0, x1 - 0.8):
        els.append(wbox([xa, 0, 7.7], [xa + 0.8, h - 1.12, 8.3], "steel"))
        els.append(wbox([xa, 0, 7.7], [xa + 0.8, 0.8, 19.2], "steel"))
        els.append(wbox([xa - 0.3, 0, 15.5], [xa + 1.1, 1.6, 18.5], "black"))
    els.append(wbox([x0, 4.5, 7.8], [x1, 5.1, 8.2], "steel"))
    return els


C.add("track_hurdle", 'new BlockSportFixture("track_hurdle", BlockSportFixture.Kind.HURDLE)',
      ("Hurdle", "Hürde", "Valla", "Häck"),
      {"hurdle_%d" % v: vc.model(FIX_TEX, hurdle(h)) for v, h in enumerate(HURDLE_PX)},
      {"forge_marker": 1, "defaults": {"model": M("hurdle_0")},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "variant": {str(v): {"model": M("hurdle_%d" % v)} for v in range(4)},
                    "inventory": [{"model": M("hurdle_3")}]}},
      item={"parent": "csm:block/venues/track/hurdle_3", "display": vu.icon_display(0.55)},
      tab=TAB)


def starting_block():
    els = [box([7.2, 0, 1], [8.8, 1.2, 15], "alu"), box([6.5, 0, 0.5], [9.5, 0.6, 2], "black")]
    for xa, z0 in ((3.4, 4), (9.4, 9)):
        p = box([xa, 0.5, z0], [xa + 3.2, 4.5, z0 + 0.6], "red")
        p["rotation"] = {"origin": [xa, 0.5, z0], "axis": "x", "angle": -45}
        els.append(p)
        els.append(box([xa + 0.4, 0, z0 - 0.2], [xa + 2.8, 1.2, z0 + 3.2], "steel"))
        els.append(box([min(xa + 3.2, 7.2), 0.2, z0 + 1], [max(xa, 8.8), 0.8, z0 + 2], "steel"))
    return els


C.add("starting_block", 'new BlockSportFixture("starting_block", BlockSportFixture.Kind.STARTING_BLOCK)',
      ("Starting Block", "Startblock", "Taco de Salida", "Startblock"),
      {"starting_block": vc.model(FIX_TEX, starting_block())},
      vc.facing_state(M("starting_block")),
      item={"parent": "csm:block/venues/track/starting_block", "display": vu.icon_display(0.9)},
      tab=TAB)


# The shot put toe board: an arc on the circle's front edge, drawn from the circle's centre cell
TB_MATS = {"white": T("white")}


def toe_board():
    r0 = 1.0675 * 16          # the circle's inner edge
    r1 = r0 + 0.112 * 16
    h = 0.10 * 16
    half = math.degrees(1.21 / 1.0675) / 2
    out = []
    n = 10
    for k in range(n):
        a0 = math.radians(-90 - half + 2 * half * k / n)
        a1 = math.radians(-90 - half + 2 * half * (k + 1) / n)
        pi0 = (8 + r0 * math.cos(a0), 8 + r0 * math.sin(a0))
        pi1 = (8 + r0 * math.cos(a1), 8 + r0 * math.sin(a1))
        po0 = (8 + r1 * math.cos(a0), 8 + r1 * math.sin(a0))
        po1 = (8 + r1 * math.cos(a1), 8 + r1 * math.sin(a1))
        am = (a0 + a1) / 2
        out.append(vc.ohpoly([pi0, pi1, po1, po0], h, "white"))
        out.append(vu.vc.ovquad(pi0, pi1, 0, h, "white", (-math.cos(am), -math.sin(am))))
        out.append(vu.vc.ovquad(po1, po0, 0, h, "white", (math.cos(am), math.sin(am))))
        if k == 0:
            out.append(vu.vc.ovquad(po0, pi0, 0, h, "white", (math.sin(a0), -math.cos(a0))))
        if k == n - 1:
            out.append(vu.vc.ovquad(pi1, po1, 0, h, "white", (-math.sin(a1), math.cos(a1))))
    return out


U.write_obj("toe_board", "toe_board", toe_board())
U.mtl("toe_board", TB_MATS)
C.add("shot_put_toe_board", 'new BlockSportFixture("shot_put_toe_board", BlockSportFixture.Kind.TOE_BOARD)',
      ("Shot Put Toe Board", "Kugelstoß-Stoßbalken", "Contenedor de Lanzamiento de Peso",
       "Stoppbräda för Kulstötning"), {},
      icon_state("toe_board"), tab=TAB)
C.blocks[-1]["models"]["toe_board_icon"] = dict(vc.model(
    {"white": T("white"), "particle": T("white")},
    [box([3, 0, 5], [13, 1.6, 7.5], "white"), box([0.5, 0, 6.5], [3.5, 1.6, 9], "white"),
     box([12.5, 0, 6.5], [15.5, 1.6, 9], "white")]), display=vu.icon_display(1.0))


# ------------------------------------------------------------------------------------------
# The finish line camera tower and the lap counter
# ------------------------------------------------------------------------------------------
TW_MATS = {"steel": T("steel"), "cam": T("camera"), "white": T("white"), "black": T("black")}


def timing_tower():
    out = []
    for dx, dz in ((-4, -4), (4, -4), (0, 5)):
        out += vu.tube((8, 30, 8), (8 + dx, 0, 8 + dz), 0.4, "steel", sides=6)
    out += vu.box(7, 9, 0, 56, 7, 9, "steel")
    out += vu.box(3, 13, 56, 57, 3, 13, "steel")
    out += vu.box(5, 11, 57, 62, 4, 12, "cam")
    out += vu.box(6.6, 9.4, 58, 60.8, 1.5, 4, "black")
    out += vu.box(4.5, 11.5, 62, 62.6, 3.5, 12.5, "white")
    return out


U.write_obj("timing_tower", "timing_tower", timing_tower())
U.mtl("timing_tower", TW_MATS)
C.add("track_timing_tower",
      vu.unit_java("BlockVenueUnit", "track_timing_tower", [(6, 0, 6, 10, 63, 10)]),
      ("Finish Line Camera Tower", "Zielkamera-Turm", "Torre de Cámara de Meta",
       "Målkameratorn"), {},
      icon_state("timing_tower"), tab=TAB)
C.blocks[-1]["models"]["timing_tower_icon"] = dict(vc.model(
    {"steel": T("steel"), "cam": T("camera"), "black": T("black"), "particle": T("steel")},
    [box([7.3, 0, 7.3], [8.7, 24, 8.7], "steel"), box([4, 24, 4], [12, 25, 12], "steel"),
     box([5, 25, 4.5], [11, 30, 11.5], "cam"), box([6.8, 26, 2.5], [9.2, 28.6, 4.5], "black"),
     box([3, 0, 3], [13, 0.8, 13], "steel")]), display=vu.icon_display(0.5, -3))

LAP_TEX = {"steel": T("steel"), "cab": T("black"), "brass": T("brass"), "white": T("white"),
           "particle": T("black")}


def lap_counter():
    return [box([7, 0, 7], [9, 20, 9], "steel"), box([5, 0, 5], [11, 0.6, 11], "steel"),
            box([0, 20, 6], [16, 32, 11], "cab"),
            box([16, 27, 8], [19.5, 28, 9], "steel"),
            box([17.4, 21.5, 6.9], [20.6, 26.5, 10.1], "brass"),
            box([18.5, 26.5, 8], [19.5, 27.4, 9], "steel")]


C.add("track_lap_counter",
      vu.unit_java("BlockCourtBoard", "track_lap_counter", [(7, 0, 7, 9, 20, 9), (0, 20, 6, 16, 32, 11)],
                   ", true"),
      ("Lap Counter and Bell", "Rundenzähler mit Glocke", "Cuentavueltas con Campana",
       "Varvräknare med Klocka"),
      {"lap_counter": vc.model(LAP_TEX, lap_counter())}, vc.facing_state(M("lap_counter")),
      item={"parent": "csm:block/venues/track/lap_counter", "display": vu.icon_display(0.45, -2)},
      tab=TAB)


# ------------------------------------------------------------------------------------------
# High jump and pole vault: landing areas with their standards and bars
# ------------------------------------------------------------------------------------------
JUMP_MATS = {"mat": T("mat_blue"), "top": T("mat_top"), "pipe": T("mat_piping"),
             "alu": T("alu"), "black": T("black"), "bar": T("bar_stripe")}


def mat(x0, x1, z0, z1, h):
    out = vu.box(x0, x1, 0, h - 1.0, z0, z1, "mat", faces="nsew")
    out += vu.box(x0, x1, h - 1.0, h, z0, z1, "pipe", faces="nsew")
    out += vu.panel((x0, h, z1), (x1, h, z1), (x1, h, z0), (x0, h, z0), (0, 1, 0), "top",
                    both=False)
    return out


def standard(xc, zc, top, foot_dir=1):
    out = vu.box(xc - 0.7, xc + 0.7, 0, top, zc - 0.7, zc + 0.7, "alu")
    out += vu.box(xc - 0.6, xc + 0.6, 0, 0.8, zc - 7, zc + 7, "black")
    out += vu.box(xc - 5 * foot_dir if foot_dir > 0 else xc, xc if foot_dir > 0 else xc + 5,
                  0, 0.8, zc - 0.6, zc + 0.6, "black")
    return out


HJ_BAR_Y = 2.0 * 16
HJ_L, HJ_R = 8 - 2.01 * 16, 8 + 2.01 * 16


def high_jump():
    out = mat(-40, 56, 0, 64, 11.2)
    out += standard(HJ_L, -4, 41.6, 1) + standard(HJ_R, -4, 41.6, -1)
    for x in (HJ_L, HJ_R):
        out += vu.box(x - 0.4, x + 0.4, HJ_BAR_Y - 1.2, HJ_BAR_Y - 0.8, -4, -2.2, "black")
    out += vu.tube((HJ_L + 0.7, HJ_BAR_Y, -2.8), (HJ_R - 0.7, HJ_BAR_Y, -2.8), 0.3, "bar",
                   sides=6)
    return out


U.write_obj("high_jump", "jumps", high_jump())
PV_BAR_Y = 5.0 * 16
PV_L, PV_R = 8 - 2.175 * 16, 8 + 2.175 * 16


def pole_vault():
    out = mat(-24, 40, 0, 96, 12.8)
    out += mat(-24, -2, -32, 0, 12.8) + mat(18, 40, -32, 0, 12.8)
    out += standard(PV_L, 8, 104, 1) + standard(PV_R, 8, 104, -1)
    for x in (PV_L, PV_R):
        out += vu.box(x - 0.4, x + 0.4, PV_BAR_Y - 1.2, PV_BAR_Y - 0.8, 8, 10.5, "black")
    out += vu.tube((PV_L + 0.7, PV_BAR_Y, 9.8), (PV_R - 0.7, PV_BAR_Y, 9.8), 0.3, "bar", sides=6)
    return out


U.write_obj("pole_vault", "jumps", pole_vault())
U.mtl("jumps", JUMP_MATS)

C.add("high_jump_mat",
      vu.unit_java("BlockVenueUnit", "high_jump_mat",
                   [(-40, 0, 0, 56, 11, 64), (int(HJ_L) - 2, 0, -6, int(HJ_L) + 2, 42, -2),
                    (int(HJ_R) - 2, 0, -6, int(HJ_R) + 2, 42, -2)]),
      ("High Jump Mat and Standards", "Hochsprungmatte mit Ständern",
       "Colchoneta y Saltómetros de Salto de Altura", "Höjdhoppsmatta med Ställningar"), {},
      U.obj_state("high_jump", 0.11, ty=-0.1, tz=-0.5), tab=TAB)
C.add("pole_vault_pit",
      vu.unit_java("BlockVenueUnit", "pole_vault_pit",
                   [(-24, 0, 0, 40, 13, 96), (-24, 0, -32, -2, 13, 0), (18, 0, -32, 40, 13, 0),
                    (int(PV_L) - 2, 0, 6, int(PV_L) + 2, 104, 10),
                    (int(PV_R) - 2, 0, 6, int(PV_R) + 2, 104, 10)]),
      ("Pole Vault Pit and Standards", "Stabhochsprunganlage mit Ständern",
       "Foso y Saltómetros de Salto con Pértiga", "Stavhoppsbädd med Ställningar"), {},
      U.obj_state("pole_vault", 0.08, ty=-0.2, tz=-0.3), tab=TAB)


# ------------------------------------------------------------------------------------------
# The discus and hammer cage, the steeplechase barrier
# ------------------------------------------------------------------------------------------
CAGE_R = 4.0 * 16
CAGE_H = 7.0 * 16
CAGE_C = (8, 8 - CAGE_R)            # the circle's centre, 4 m in front of the master
CAGE_MOUTH = math.degrees(math.asin(3.0 / 4.0))
CAGE_MATS = {"net": T("cage_net"), "steel": T("steel")}


def cage_angles():
    """The posts' angles, from straight ahead (the throw), round the back."""
    a0, a1 = CAGE_MOUTH, 360 - CAGE_MOUTH
    n = 9
    return [a0 + (a1 - a0) * k / n for k in range(n + 1)]


def cage_point(a):
    r = math.radians(a)
    return (CAGE_C[0] + CAGE_R * math.sin(r), CAGE_C[1] - CAGE_R * math.cos(r))


def cage():
    out = []
    angs = cage_angles()
    for a in angs:
        x, z = cage_point(a)
        out += vu.tube((x, 0, z), (x, CAGE_H, z), 0.9, "steel", sides=6)
    for a, b in zip(angs, angs[1:]):
        (xa, za), (xb, zb) = cage_point(a), cage_point(b)
        nx, nz = (za - zb), (xb - xa)
        out += vu.panel((xa, 0, za), (xb, 0, zb), (xb, CAGE_H, zb), (xa, CAGE_H, za), (nx, 0, nz),
                        "net", tile=16.0)
        out += vu.tube((xa, CAGE_H, za), (xb, CAGE_H, zb), 0.4, "steel", caps=False, sides=6)
    return out


def cage_boxes():
    """A box a cell along the netting, the cells face-connected round the back."""
    cells = []
    for k in range(0, 721):
        a = CAGE_MOUTH + (360 - 2 * CAGE_MOUTH) * k / 720.0
        x, z = cage_point(a)
        c = (int(math.floor(x / 16.0)), int(math.floor(z / 16.0)))
        if cells and c == cells[-1]:
            continue
        if cells and abs(c[0] - cells[-1][0]) + abs(c[1] - cells[-1][1]) > 1:
            cells.append((c[0], cells[-1][1]))
        cells.append(c)
    seen, out = set(), []
    for c in cells:
        if c in seen:
            continue
        seen.add(c)
        out.append((c[0] * 16 + 6, 0, c[1] * 16 + 6, c[0] * 16 + 10, int(CAGE_H), c[1] * 16 + 10))
    return out


U.write_obj("throwing_cage", "throwing_cage", cage())
U.mtl("throwing_cage", CAGE_MATS)
C.add("throwing_cage", vu.unit_java("BlockVenueUnit", "throwing_cage", cage_boxes()),
      ("Discus and Hammer Cage", "Diskus- und Hammerwurfkäfig", "Jaula de Disco y Martillo",
       "Diskus- och Släggburen"), {},
      U.obj_state("throwing_cage", 0.1, ty=-0.25, tz=0.45), tab=TAB)

SC_MATS = {"bar": T("hurdle_bar"), "steel": T("steel"), "black": T("black")}


def steeple():
    h = 0.914 * 16
    out = vu.box(0.4, 63.6, h - 2.03, h, 6.98, 9.02, "bar")
    for x in (2, 32, 62):
        out += vu.box(x - 0.8, x + 0.8, 0, h - 2.03, 7.2, 8.8, "steel")
        out += vu.box(x - 0.8, x + 0.8, 0, 1.0, 1, 15, "black")
    return out


U.write_obj("steeplechase_barrier", "steeplechase", steeple())
U.mtl("steeplechase", SC_MATS)
C.add("steeplechase_barrier",
      vu.unit_java("BlockVenueUnit", "steeplechase_barrier", [(0, 0, 6, 64, 15, 10)]),
      ("Steeplechase Barrier", "Hindernis (Hindernislauf)", "Obstáculo de Carrera de Obstáculos",
       "Hinderbom"), {},
      U.obj_state("steeplechase_barrier", 0.22, ty=0.1, tx=-0.6), tab=TAB)


# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
for _k, _n in {"track_400m": ("400 m track", "400-m-Bahn", "Pista de 400 m", "400 m-bana"),
               "long_jump": ("Long and triple jump", "Weit- und Dreisprung",
                             "Salto de longitud y triple", "Längd- och tresteg"),
               "pole_vault": ("Pole vault", "Stabhochsprung", "Salto con pértiga",
                              "Stavhopp")}.items():
    L("csm.venues.lines." + _k, _n)
for _i, _n in enumerate((("0.762 m (400 m, women)", "0,762 m (400 m, Frauen)",
                          "0,762 m (400 m, mujeres)", "0,762 m (400 m, damer)"),
                         ("0.838 m (100 m)", "0,838 m (100 m)", "0,838 m (100 m)",
                          "0,838 m (100 m)"),
                         ("0.914 m (400 m, men)", "0,914 m (400 m, Männer)",
                          "0,914 m (400 m, hombres)", "0,914 m (400 m, herrar)"),
                         ("1.067 m (110 m)", "1,067 m (110 m)", "1,067 m (110 m)",
                          "1,067 m (110 m)"))):
    L("csm.venues.track.hurdle.%d" % _i, _n)
L("csm.venues.track.laps", ("Laps to go: %s", "Noch %s Runden", "Vueltas restantes: %s",
                            "Varv kvar: %s"))
L("gui.csm.venue_text.laps.0", ("Laps to go", "Verbleibende Runden", "Vueltas restantes",
                                "Varv kvar"))


# ------------------------------------------------------------------------------------------
# Java and layouts
# ------------------------------------------------------------------------------------------
JAVA = {os.path.join(gc.JAVA_DIR, "TrackLayouts.java"): vl.layouts_java(
    GEN, "TrackLayouts",
    "The track: the 400 m track with its field events, the long and triple jump runway and pit, "
    "the pole vault runway and box ({@code track_layout.py}).", LAYOUTS),
    os.path.join(gc.TEST_DIR, "TrackSamples.java"): gc.samples_file(
        "TrackSamples", "TRACK", gc.samples_java("TRACK", LAYOUTS, every=41), GEN)}

PLAIN = {"SURFACE": ("track_surface", "cc"), "GRASS": ("field_turf", None),
         "SAND": ("sports_sand", None), "CONCRETE": None, "KERB": None}


def ground(lay, colours=None, sectors=True):
    """{(i, j): (block, meta, nbt)}: the layout's extent, painted cells where a line falls."""
    cols = list(colours or lay.colours)
    out = {}
    lid = [x.name for x in LAYOUTS].index(lay.name)
    for (i, j), v in lay.census().items():
        if v[0] == "picture":
            out[(i, j)] = ("track_painted", 0, None)
            continue
        name = vl.MATERIALS[v[1]][0]
        if name == "SURFACE":
            out[(i, j)] = ("track_surface", cols[0], None)
        elif name == "SAND":
            out[(i, j)] = ("sports_sand", 0, None)
        elif name == "GRASS":
            out[(i, j)] = ("field_turf", 0, None)
        else:
            out[(i, j)] = ("track_painted", 0, None)
    extra = ""
    if colours is not None:
        extra = "".join(",%s:%d" % (k, cols[n]) for n, k in enumerate(("cc", "sc", "lc", "oc")))
    for (ai, aj) in lay.anchors():
        out[(ai, aj)] = ("track_marking_anchor", 0, "{lay:%d,ax:%d,az:%d%s}" % (lid, ai, aj,
                                                                               extra))
    return out


def lane_cell(k):
    """The cell row nearest the middle of lane k on the home straight."""
    return int(math.floor(tl.r_in(k) + 0.6))


def track_objects(name):
    out = []
    if name == "track_400m":
        ex = int(math.floor(tl.EX))
        wx = int(math.floor(tl.WX))
        out.append((ex, 1, 48, "track_timing_tower", 2))
        out.append((ex + 1, 1, 35, "track_lap_counter", 0))
        # a flight of 110 m hurdles at the first hurdle, starting blocks at the 100 m start
        x110 = int(math.floor(tl.EX - 110 + 13.72))
        x100 = int(math.floor(tl.EX - 100 - 0.6))
        for k in range(1, 9):
            out.append((x110, 1, lane_cell(k), "track_hurdle", 3 | 3 << 2))
            out.append((x100, 1, lane_cell(k), "starting_block", 3))
        out.append((0, 1, -41, "steeplechase_barrier", 3))
        out.append((wx - 24, 1, 15, "high_jump_mat", 3))
        out.append((60, 1, -14, "shot_put_toe_board", 1))
        out.append((68, 1, 10, "throwing_cage", 1))
    elif name == "pole_vault":
        out.append((46, 1, 0, "pole_vault_pit", 1))
    return out


def turn_meta(block, meta, rot):
    if block in ("track_surface", "sports_sand", "track_painted", "field_turf"):
        return meta
    if block == "track_marking_anchor":
        return (meta + rot) & 3
    return (meta & ~3) | (((meta & 3) + rot) % 4)


def commands(name, x0, y0, z0, rot=0, colours=None):
    lay = next(l for l in LAYOUTS if l.name == name)
    return gc.lines_commands(ground(lay, colours), track_objects(name), x0, y0, z0, rot,
                             turn_meta)


def layout_json(name):
    lay = next(l for l in LAYOUTS if l.name == name)
    g = ground(lay)
    return {"layout": name, "id": [x.name for x in LAYOUTS].index(name),
            "frame": "track_layout.py's: the track's centre is the west edge of cell (0, 0)'s "
                     "middle row, its straights east-west, the home straight south; ground "
                     "blocks at the floor level, objects dy above it",
            "ground": [[i, j, b, m, nbt] for (i, j), (b, m, nbt) in sorted(
                g.items(), key=lambda kv: (kv[0][1], kv[0][0]))],
            "objects": [list(o) for o in track_objects(name)]}


def main():
    if "--layout" in sys.argv:
        name = sys.argv[sys.argv.index("--layout") + 1]
        rot = int(sys.argv[sys.argv.index("--rot") + 1]) if "--rot" in sys.argv else 0
        if "--commands" in sys.argv:
            i = sys.argv.index("--commands")
            x, y, z = (int(v) for v in sys.argv[i + 1:i + 4])
            print("\n".join(commands(name, x, y, z, rot)))
        else:
            print(json.dumps(layout_json(name)))
        return 0
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        os.makedirs(out, exist_ok=True)
        for lay in LAYOUTS:
            ppm = 6 if lay.name == "track_400m" else 16
            print(vl.preview(lay, os.path.join(out, lay.name + ".png"), ppm=ppm,
                             colours=tl.PREVIEW))
        return 0
    if "--doc" in sys.argv:
        print(json.dumps({lay.name: {"extent": lay.extent, "painted": len(lay.pictures()),
                                     "anchors": len(lay.anchors()), "ops": len(lay.ops)}
                          for lay in LAYOUTS}, indent=1))
        return 0
    if "--check" in sys.argv:
        stale = gc.write_java(JAVA, True)
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        gc.write_java(JAVA, False)
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, fn) for dp, _, fs in os.walk(C.assets) for fn in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
