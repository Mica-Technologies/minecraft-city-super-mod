#!/usr/bin/env python3
"""
gen_healthcare_diagnostics.py -- the Healthcare module's imaging, laboratory, pharmacy and morgue
pieces.

Imaging: the CT scanner and the MRI scanner (each one unit several blocks big, drawn whole by its
master block, BlockHealthcareUnit, with invisible healthcare_unit_part cells filling the rest of
its footprint), the ceiling X-ray tube (hung on the ceiling boom column like the ICU pendant), the
X-ray table, the wall bucky stand, the mobile C-arm and its monitor cart, the mammography unit,
the lead apron rack and the radiology reading workstation; the X-RAY IN USE and MRI IN USE lights
over a door (lit by redstone, the Lighting conventions), the four MRI zone signs and a radiation
warning sign (the trefoil, which is not a protected emblem).

Laboratory: lab benches with a reagent shelf (plain and with a sink) that join into one bench,
the chemistry analyser, the bench-top haematology analyser, centrifuge and microscope, the fume
hood and the biosafety cabinet, the blood bank refrigerator, the specimen pass-through and the
pneumatic tube station. Pharmacy: the sterile compounding hood, bin shelving and the unit-dose
drawer cabinet (the retail pharmacy counters and shelf wall are Furnishings', not repeated
here). Morgue: the refrigerated cabinet whose doors open a tier at a time, the autopsy table and
the covered body cart.

Nothing here is a real product: no maker's name, no red cross. Screens are animated textures
(.mcmeta), so nothing ticks and nothing has a renderer. Swatches the other Healthcare generators
already draw are shared by path.

Writes, under modules/healthcare/src/main/resources/assets/csm:

  * textures/blocks/healthcare/diagnostics/*.png (+ .mcmeta for the screens)
  * models/block/healthcare/diagnostics/*.json
  * blockstates/<registry>.json
  * the tile names in all four lang files

Every model faces north with what it stands against at +Z.

Usage:
    python gen_healthcare_diagnostics.py              # write everything
    python gen_healthcare_diagnostics.py --check      # fail if the tree has drifted
    python gen_healthcare_diagnostics.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import copy
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_healthcare_common as hc  # noqa: E402
import gen_healthcare_wards as hw  # noqa: E402
import gen_healthcare_clinical as hcl  # noqa: E402
import gen_healthcare_clinics as hcn  # noqa: E402
from gen_healthcare_common import (  # noqa: E402
    disc, draw_text, draw_text_centred, fill, frame, rect, shade)

C = hc.catalogue("gen_healthcare_diagnostics.py", "diagnostics")
T = C.T
M = C.M
W = hw.C.T     # the wards' shared swatches
K = hcl.C.T    # the clinical generator's
N = hcn.C.T    # the clinics generator's

el = hw.el
screen = hw.screen
rot = hw.rot
octo = hw.octo
cut = hw.cut
shift = hw.shift
model = hw.model
casters = hw.casters
ITEM_SMALL = hw.ITEM_SMALL
ITEM_TALL = hw.ITEM_TALL
B = hw.B
FULL = [0, 0, 16, 16]
SIDES4 = ("north", "south", "east", "west")

PC = hc.PATIENT_CARE
CL = hc.CLINICAL
HID = hc.HIDDEN

STEEL = W("stainless")
GREY = W("frame_grey")
RUBBER = W("rubber")
WHITE = W("white")
WARM = W("warm_white")
DARK = W("dark_grey")
CHAR = W("charcoal")


def front(frm, to, base, key, side="north", faces=hw.ALL):
    """A box whose front (north, or ``side``) shows the whole of a texture."""
    return el(frm, to, base, faces=faces, per={side: key}, uv={side: FULL})


def plane(frm, to, key, side):
    """A zero-thickness face showing the whole of a texture."""
    return el(frm, to, key, faces=(side,), uv={side: FULL})


def MI(name):
    """An item model's parent: item models resolve from models/, so the block/ is needed."""
    return "csm:block/%s/%s" % (C.model_dir, name)


def tex(name, fn):
    C.textures[name] = fn


def squash(img, size):
    """A texture drawn at its face's shape, stretched to the square the atlas wants; the face's
    UV stretches it back."""
    return img.resize((size, size), Image.LANCZOS)


def stretch_uvs(elements):
    """For a model reaching past its cell: a face whose UV window would leave 0..16 shows the
    whole texture instead (stretched), so it never samples a neighbour in the atlas."""
    out = copy.deepcopy(elements)
    for e in out:
        for f in e["faces"].values():
            u = f.get("uv")
            if u and (min(u) < 0 or max(u) > 16):
                f["uv"] = list(FULL)
    return out


# ==========================================================================================
# Textures
# ==========================================================================================
for i, (n, c, g) in enumerate((
        ("pale_blue", hc.PALE_BLUE, 2), ("epoxy", (34, 36, 38), 2),
        ("carbon", (44, 46, 50), 2), ("liner", (210, 214, 214), 1),
        ("bin_blue", (60, 110, 180), 3), ("tube_grey", (150, 156, 160), 2),
        ("carrier", (52, 110, 186), 3), ("coil", (232, 232, 228), 2))):
    tex(n, (lambda c=c, g=g, s=6000 + i: hc.swatch(c, s, grain=g)))
for i, (n, c) in enumerate((("apron_blue", (52, 82, 150)), ("apron_purple", (104, 70, 140)),
                            ("apron_teal", (40, 128, 128)), ("cover_navy", (40, 50, 78)))):
    tex(n, (lambda c=c, s=6100 + i: hc.fabric(c, s)))


@C.texture("ct_display")
def _ct_display():
    """A gantry's small display: the table position and the tilt."""
    img = fill((30, 34, 40), grain=1, seed=6201)
    rect(img, 1, 2, 15, 13, (14, 20, 26))
    draw_text(img, "0", 3, 3, (120, 220, 240))
    draw_text(img, "112", 3, 8, (120, 220, 240))
    rect(img, 11, 3, 13, 6, (90, 200, 120))
    return img


@C.texture("coll_front")
def _coll_front():
    """The X-ray collimator's front: two shutter knobs, the field size dials and a lamp key."""
    img = fill((220, 222, 220), grain=1, seed=6202)
    frame(img, 0, 0, 16, 16, (170, 172, 172))
    for x in (4, 12):
        disc(img, x, 6, 2, (60, 64, 70))
    rect(img, 6, 10, 10, 13, (40, 44, 50))
    rect(img, 7, 11, 9, 12, (240, 200, 60))
    return img


@C.texture("coll_glass")
def _coll_glass():
    img = fill((40, 44, 48), grain=1, seed=6203)
    rect(img, 2, 7, 14, 9, (60, 64, 70))
    rect(img, 7, 2, 9, 14, (60, 64, 70))
    return img


@C.texture("coll_light")
def _coll_light():
    """The light field: the collimator lamp on, its crosshair."""
    img = fill((255, 246, 200), grain=1, seed=6204)
    rect(img, 1, 7, 15, 9, (60, 56, 40))
    rect(img, 7, 1, 9, 15, (60, 56, 40))
    return img


@C.texture("bucky_face")
def _bucky_face():
    """A detector's face: the centring crosshair and the three ionisation chambers."""
    img = fill((206, 210, 212), grain=1, seed=6205)
    frame(img, 0, 0, 16, 16, (140, 146, 150))
    rect(img, 1, 8, 15, 9, (110, 116, 120))
    rect(img, 8, 1, 9, 15, (110, 116, 120))
    for x, y in ((4, 4), (11, 4), (7, 10)):
        frame(img, x - 2, y - 1, x + 2, y + 3, (176, 180, 182))
    return img


def xray_image(kind, i):
    """A grey radiograph drawn small: a chest, a knee or a hip with a wire moving across it."""
    img = Image.new("RGBA", (32, 32), (6, 6, 8, 255))
    px = img.load()
    for y in range(32):
        for x in range(32):
            dx, dy = (x - 15.5) / 16.0, (y - 15.5) / 16.0
            v = 0.0
            if kind == "chest":
                lung = max(0.0, 1 - ((abs(dx) - 0.38) / 0.28) ** 2 - ((dy + 0.05) / 0.7) ** 2)
                v = 0.55 - 0.42 * lung
                if abs(dx) < 0.1:
                    v += 0.3
                rib = 0.5 + 0.5 * math.sin((dy + 0.05 * abs(dx)) * 18)
                v += 0.08 * rib * (abs(dx) > 0.12)
                if (dx + 0.12) ** 2 / 0.06 + (dy - 0.25) ** 2 / 0.05 < 1:
                    v += 0.18
            elif kind == "knee":
                v = 0.25
                if abs(dx + 0.02 * dy) < 0.24:
                    v = 0.7 - 0.2 * abs(dy)
                if abs(dy) < 0.05:
                    v = 0.2
            else:  # an axial CT slice
                r = math.hypot(dx * 1.1, dy * 1.4)
                v = 0.0 if r > 0.85 else (0.85 if r > 0.78 else 0.42)
                if math.hypot(dx * 1.1, (dy + 0.1) * 1.4) < 0.18:
                    v = 0.6
                if math.hypot(dx - 0.35, dy) < 0.12 or math.hypot(dx + 0.35, dy) < 0.12:
                    v = 0.25
            g = int(max(0, min(1, v)) * 230)
            px[x, y] = (g, g, g + 4 if g < 250 else g, 255)
    if kind == "knee":
        wx = 6 + i * 5
        rect(img, wx, 6, wx + 1, 26, (250, 250, 250))
    return img


def fluoro_frame(i):
    return lambda: xray_image("knee", i)


hc.animation(C, "fluoro_screen", [fluoro_frame(i) for i in range(4)], 30)


def reading_frame(i):
    def f():
        kind = ("chest", "slice", "chest", "slice")[i]
        img = xray_image(kind, i)
        draw_text(img, "R", 1, 1, (230, 230, 230))
        return img
    return f


hc.animation(C, "reading_screen", [reading_frame(i) for i in range(4)], 160)


def lens(text, lit, colour):
    """An IN USE light's lens: the legend at the lens's 10 x 4 shape, stretched square."""
    def f():
        bg = colour if lit else shade(colour, 0.32)
        img = Image.new("RGBA", (80, 32), tuple(bg) + (255,))
        fg = (255, 255, 255) if lit else shade(colour, 0.55)
        hc.stamp_legend(img, text, 40, 10, fg, 12, "C", max_w=72)
        frame(img, 0, 0, 80, 32, shade(bg, 0.7), width=2)
        return squash(img, 64)
    return f


for _name, _text, _c in (("xray", "X-RAY IN USE", (226, 40, 32)),
                         ("mri", "MRI IN USE", (226, 40, 32))):
    tex("%s_in_use_off" % _name, lens(_text, False, _c))
    tex("%s_in_use_on" % _name, lens(_text, True, _c))


ZONES = (("I", "PUBLIC ACCESS", None, (40, 140, 70)),
         ("II", "PATIENT", "SCREENING", (40, 90, 170)),
         ("III", "RESTRICTED", "SCREENED ONLY", (236, 196, 32)),
         ("IV", "MAGNET ROOM", "ALWAYS ON", (200, 30, 30)))


def zone_sign(z, l1, l2, colour):
    """An MRI zone sign: the zone in a coloured band over its meaning, in the highway font."""
    def f():
        img = Image.new("RGBA", (128, 128), (250, 250, 246, 255))
        dark = colour == (236, 196, 32)
        rect(img, 0, 0, 128, 62, colour)
        fg = (20, 20, 22) if dark else (255, 255, 255)
        hc.stamp_legend(img, "MRI ZONE", 64, 8, fg, 16, "C", max_w=116)
        hc.stamp_legend(img, z, 64, 30, fg, 26, "D", max_w=116)
        ink = (24, 24, 26)
        if l2 is None:
            hc.stamp_legend(img, l1, 64, 84, ink, 14, "C", max_w=118)
        else:
            hc.stamp_legend(img, l1, 64, 72, ink, 14, "C", max_w=118)
            hc.stamp_legend(img, l2, 64, 96, ink, 14, "C", max_w=118)
        frame(img, 0, 0, 128, 128, (30, 30, 32), width=3)
        return img
    return f


for _i, (_z, _l1, _l2, _c) in enumerate(ZONES):
    tex("mri_zone_%d" % (_i + 1), zone_sign(_z, _l1, _l2, _c))


def trefoil(img, cx, cy, r, colour):
    """The radiation trefoil: a centre disc and three blades 60 degrees wide, 120 apart."""
    px = img.load()
    c = tuple(colour) + (255,)
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r * 0.2:
                px[x, y] = c
                continue
            if r * 0.3 <= d <= r:
                a = (math.degrees(math.atan2(-dy, dx)) - 90) % 120
                if a < 30 or a > 90:
                    px[x, y] = c


@C.texture("radiation_sign")
def _radiation_sign():
    img = Image.new("RGBA", (64, 64), (246, 206, 40, 255))
    trefoil(img, 32, 20, 15, (30, 24, 30))
    hc.stamp_legend(img, "CAUTION", 32, 39, (30, 24, 30), 8, "C", max_w=56)
    hc.stamp_legend(img, "X-RAY", 32, 51, (30, 24, 30), 8, "C", max_w=56)
    frame(img, 0, 0, 64, 64, (30, 24, 30), width=2)
    return img


# --- the laboratory ------------------------------------------------------------------------
@C.texture("lab_cabinet")
def _lab_cabinet():
    """The bench's base, drawn as the block's front (one texel a half pixel): a drawer over two
    doors, a toe kick."""
    img = fill((214, 212, 204), size=32, grain=1, seed=6301)
    edge = (178, 176, 168)
    frame(img, 1, 2, 31, 9, edge)
    rect(img, 13, 5, 19, 6, (130, 134, 140))
    frame(img, 1, 10, 16, 30, edge)
    frame(img, 16, 10, 31, 30, edge)
    rect(img, 13, 12, 14, 18, (130, 134, 140))
    rect(img, 18, 12, 19, 18, (130, 134, 140))
    rect(img, 0, 30, 32, 32, (44, 44, 46))
    return img


@C.texture("bottles")
def _bottles():
    """A row of reagent bottles, brown and clear with coloured caps, on a transparent ground."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    x = 1
    k = 0
    for w, h, body, cap in ((4, 22, (120, 70, 30), (30, 30, 30)), (3, 16, (220, 226, 230),
                                                                     (40, 90, 180)),
                            (5, 26, (120, 70, 30), (200, 40, 40)), (3, 14, (220, 226, 230),
                                                                     (240, 200, 40)),
                            (4, 20, (170, 200, 220), (30, 30, 30)),
                            (4, 24, (120, 70, 30), (40, 140, 70)),
                            (3, 18, (220, 226, 230), (30, 30, 30))):
        y0 = 32 - h
        rect(img, x, y0 + 4, x + w, 32, body)
        rect(img, x + 1, y0 + 2, x + w - 1, y0 + 4, shade(body, 0.85))
        rect(img, x + 1, y0, x + w - 1, y0 + 2, cap)
        rect(img, x, y0 + 10, x + w, y0 + 13, (240, 240, 236))
        x += w + 1
        k += 1
    return img


@C.texture("sink_bowl")
def _sink_bowl():
    img = fill((60, 62, 64), grain=1, seed=6302)
    disc(img, 8, 8, 1.6, (20, 20, 22))
    return img


@C.texture("chem_front")
def _chem_front():
    """The chemistry analyser's front: the sample loading bay, the reagent door, the status
    lamps and the waste drawer."""
    img = fill((232, 234, 232), size=32, grain=1, seed=6303)
    frame(img, 0, 0, 32, 32, (196, 198, 196))
    rect(img, 2, 3, 15, 13, (60, 64, 70))
    for k in range(5):
        rect(img, 3 + k * 2 + (k // 2), 5, 4 + k * 2 + (k // 2), 12,
             ((200, 60, 50), (240, 200, 40), (90, 150, 220), (200, 60, 50), (120, 200, 120))[k])
    frame(img, 17, 2, 30, 18, (176, 178, 178))
    rect(img, 27, 9, 28, 12, (130, 134, 140))
    for k, c in enumerate(((90, 220, 120), (240, 190, 40), (90, 94, 100))):
        disc(img, 4 + k * 4, 17, 1.2, c)
    frame(img, 2, 21, 30, 30, (176, 178, 178))
    rect(img, 13, 24, 19, 25, (130, 134, 140))
    return img


@C.texture("chem_top")
def _chem_top():
    """Looking down through the analyser's lid: the reagent carousel."""
    img = fill((200, 204, 206), grain=1, seed=6304)
    disc(img, 8, 8, 6.5, (70, 74, 80))
    for k in range(10):
        a = 2 * math.pi * k / 10
        disc(img, 8 + math.cos(a) * 4.6, 8 + math.sin(a) * 4.6, 1.1,
             ((240, 240, 236), (240, 200, 60))[k % 2])
    disc(img, 8, 8, 1.6, (150, 154, 160))
    return img


def analyser_frame(i):
    """An analyser's screen: a run list ticking down and a progress bar."""
    def f():
        img = fill((236, 240, 244), size=32, grain=1, seed=6310)
        rect(img, 0, 0, 32, 6, (40, 90, 160))
        draw_text(img, "RUN", 2, 1, (250, 250, 250))
        for k in range(4):
            y = 8 + k * 5
            done = k < (i % 5)
            rect(img, 2, y, 4, y + 3, (60, 170, 90) if done else (200, 204, 210))
            rect(img, 6, y + 1, 22, y + 2, (90, 96, 104))
            draw_text(img, "%d" % (k + 1), 25, y, (40, 44, 50))
        rect(img, 2, 29, 30, 31, (200, 204, 210))
        rect(img, 2, 29, 2 + 5 * (i + 1) + 1, 31, (60, 120, 210))
        return img
    return f


hc.animation(C, "analyser_screen", [analyser_frame(i) for i in range(5)], 40)


@C.texture("haem_front")
def _haem_front():
    """The haematology analyser's front: the aspiration probe's door and the tube rack slot."""
    img = fill((232, 234, 232), grain=1, seed=6311)
    frame(img, 0, 0, 16, 16, (196, 198, 196))
    rect(img, 2, 3, 7, 12, (190, 194, 196))
    rect(img, 4, 4, 5, 9, (150, 154, 160))
    rect(img, 1, 13, 15, 15, (60, 64, 70))
    return img


@C.texture("tube_rack")
def _tube_rack():
    """A rack of blood tubes, caps up, in their colours."""
    img = fill((236, 236, 232), grain=1, seed=6312)
    for k, c in enumerate(((150, 60, 170), (150, 60, 170), (40, 120, 200), (200, 40, 40),
                           (240, 200, 40), (90, 160, 80), (150, 60, 170))):
        rect(img, 1 + k * 2, 2, 2 + k * 2, 14, c)
    return img


@C.texture("centrifuge_front")
def _centrifuge_front():
    img = fill((232, 234, 232), grain=1, seed=6313)
    rect(img, 2, 4, 10, 10, (20, 26, 30))
    draw_text(img, "15", 3, 5, (120, 220, 240))
    disc(img, 12.5, 7, 1.5, (60, 170, 90))
    rect(img, 2, 12, 14, 13, (190, 194, 196))
    return img


@C.texture("centrifuge_top")
def _centrifuge_top():
    img = fill((150, 154, 160), grain=1, seed=6314)
    disc(img, 8, 8, 5.5, (60, 70, 80))
    disc(img, 8, 8, 4.5, (110, 130, 150))
    disc(img, 8, 8, 1, (40, 44, 50))
    return img


@C.texture("hood_band")
def _hood_band():
    """The fume hood's top band: the airflow monitor."""
    img = fill((236, 236, 232), grain=1, seed=6320)
    rect(img, 10, 4, 15, 12, (40, 44, 50))
    rect(img, 11, 5, 14, 8, (30, 60, 40))
    disc(img, 12.5, 10, 1, (90, 220, 120))
    rect(img, 1, 6, 8, 7, (190, 194, 196))
    rect(img, 1, 9, 6, 10, (190, 194, 196))
    return img


@C.texture("sash")
def _sash():
    """Sash glass: clear, with a few bright streaks so it reads as glass."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for k in range(5):
        x = 3 + k
        y = 12 - k
        px[x, y] = (232, 240, 244, 255)
    for k in range(3):
        px[10 + k, 6 - k] = (232, 240, 244, 255)
    return img


@C.texture("bsc_band")
def _bsc_band():
    """The biosafety cabinet's top band: the biohazard mark and the alarm panel."""
    img = fill((236, 236, 232), size=32, grain=1, seed=6321)
    hw._biohazard(img, 8, 16, 7, hc.BIOHAZARD_RED)
    rect(img, 18, 10, 30, 22, (40, 44, 50))
    rect(img, 19, 11, 29, 16, (30, 60, 40))
    for k, c in enumerate(((90, 220, 120), (240, 190, 40), (200, 50, 40))):
        disc(img, 21 + k * 3.5, 19, 1.1, c)
    return img


@C.texture("hepa")
def _hepa():
    """A HEPA filter face: a fine perforated grille."""
    img = fill((206, 210, 212), grain=1, seed=6322)
    for y in range(1, 16, 2):
        for x in range(1 + (y // 2) % 2, 16, 2):
            img.load()[x, y] = (150, 156, 160, 255)
    frame(img, 0, 0, 16, 16, (170, 174, 176))
    return img


@C.texture("compounding_band")
def _compounding_band():
    img = fill((236, 236, 232), size=32, grain=1, seed=6323)
    rect(img, 0, 0, 32, 32, (236, 236, 232))
    draw_text_centred(img, "STERILE", 16, 9, (40, 90, 160))
    draw_text_centred(img, "FLOW", 16, 17, (40, 90, 160))
    disc(img, 27, 25, 1.4, (90, 220, 120))
    return img


def blood_door(upper):
    """The blood bank refrigerator's glass door, drawn as the block's front (one texel a half
    pixel): shelves of bags, and on the upper half the display and the BLOOD BANK band."""
    def f():
        img = Image.new("RGBA", (32, 32), (30, 34, 40, 255))
        frame(img, 0, 0, 32, 32, (226, 228, 230), width=2)
        shelves = (6, 18, 30) if not upper else (22, 32)
        for sy in shelves:
            rect(img, 2, sy, 30, sy + 1, (170, 176, 180))
            for k in range(5):
                bx = 3 + k * 5 + (sy % 3)
                rect(img, bx, sy - 7, bx + 4, sy, (150, 20, 28))
                rect(img, bx + 1, sy - 6, bx + 3, sy - 4, (240, 240, 236))
        if upper:
            # the door's top edge is at texture row 4 (it stops 2 px under the cell's top)
            rect(img, 0, 4, 32, 12, (226, 228, 230))
            rect(img, 2, 5, 30, 11, (190, 30, 36))
            draw_text_centred(img, "BLOOD", 11, 6, (250, 250, 250))
            rect(img, 21, 6, 29, 10, (20, 26, 30))
            draw_text(img, "4", 23, 6, (120, 220, 240))
        return img
    return f


tex("blood_door_lo", blood_door(False))
tex("blood_door_hi", blood_door(True))


@C.texture("pt_door")
def _pt_door():
    """The specimen pass-through's door: a window, the handle and its label."""
    img = hc.brushed(hc.STAINLESS, 6330)
    frame(img, 0, 0, 16, 16, (150, 154, 158))
    rect(img, 4, 2, 12, 7, (60, 70, 80))
    rect(img, 5, 3, 7, 4, (130, 150, 170))
    rect(img, 3, 9, 13, 12, (246, 246, 240))
    draw_text_centred(img, "LAB", 8, 9, (200, 40, 40))
    rect(img, 6, 13, 10, 14, (90, 94, 100))
    return img


@C.texture("tube_front")
def _tube_front():
    """The pneumatic tube station's front: the send door with a carrier behind its window, the
    display and the keypad."""
    img = fill((232, 234, 232), size=32, grain=1, seed=6331)
    frame(img, 0, 0, 32, 32, (190, 192, 192))
    rect(img, 3, 2, 17, 26, (200, 204, 206))
    frame(img, 3, 2, 17, 26, (150, 154, 158))
    rect(img, 6, 5, 14, 20, (70, 80, 92))
    rect(img, 8, 7, 12, 18, (52, 110, 186))
    rect(img, 8, 11, 12, 13, (200, 210, 230))
    rect(img, 20, 3, 29, 9, (20, 26, 30))
    draw_text(img, "12", 21, 4, (130, 230, 150))
    for r in range(4):
        for c in range(3):
            rect(img, 20 + c * 3, 12 + r * 3, 22 + c * 3, 14 + r * 3, (90, 94, 100))
    rect(img, 20, 25, 29, 28, (60, 170, 90))
    return img


# --- the pharmacy --------------------------------------------------------------------------
@C.texture("bin_front")
def _bin_front():
    """A row of three blue bins' fronts with their white labels, drawn at the row's shape."""
    img = Image.new("RGBA", (52, 18), (60, 110, 180, 255))
    for k in range(3):
        x0 = k * 17 + 1
        rect(img, x0, 1, x0 + 16, 18, (70, 124, 196))
        rect(img, x0, 1, x0 + 16, 2, (40, 80, 140))
        rect(img, x0 + 3, 7, x0 + 13, 12, (246, 246, 240))
        rect(img, x0 + 4, 9, x0 + 12, 10, (90, 94, 100))
    return squash(img, 32)


@C.texture("unit_dose")
def _unit_dose():
    """The unit-dose cabinet's front, drawn as the block's front: rows of small drawers, each
    with a label and a pull."""
    img = fill((206, 210, 214), size=32, grain=1, seed=6340)
    for r in range(5):
        y0 = 1 + r * 6
        for c in range(4):
            x0 = 1 + c * 8
            rect(img, x0, y0, x0 + 7, y0 + 5, (226, 230, 232))
            rect(img, x0 + 1, y0 + 1, x0 + 4, y0 + 2, (246, 246, 240))
            rect(img, x0 + 2, y0 + 3, x0 + 5, y0 + 4, (120, 126, 132))
    return img


# --- the morgue ----------------------------------------------------------------------------
@C.texture("morgue_door")
def _morgue_door():
    """A morgue cabinet door, drawn as the block's front: the latch handle, a card holder."""
    img = hc.brushed(hc.STAINLESS, 6350, size=32)
    frame(img, 2, 2, 30, 30, (130, 134, 138))
    rect(img, 4, 5, 12, 10, (246, 246, 240))
    frame(img, 4, 5, 12, 10, (110, 114, 118))
    rect(img, 22, 12, 28, 20, (110, 114, 118))
    rect(img, 23, 15, 27, 17, (200, 204, 208))
    return img


@C.texture("morgue_recess")
def _morgue_recess():
    img = fill((40, 42, 44), grain=1, seed=6351)
    rect(img, 0, 13, 16, 16, (70, 72, 74))
    return img


@C.texture("autopsy_top")
def _autopsy_top():
    """The autopsy table's tray: perforated stainless falling to a drain."""
    img = hc.brushed(hc.STAINLESS, 6352)
    for y in range(2, 15, 3):
        for x in range(2, 15, 3):
            img.load()[x, y] = (110, 114, 118, 255)
    disc(img, 8, 8, 1.4, (40, 42, 44))
    return img


# ==========================================================================================
# Block helpers
# ==========================================================================================
def facing_block(reg, java, names, textures, elements, tab, extra=None, display=None):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    C.add(reg, java, names, {reg: model(textures, elements, display=display)},
          hc.facing_state(M(reg), extra), tab=tab)


def java_for(cls, reg, box6, mat, mount, collides):
    return 'new %s("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.%s, %s)' % (
        cls, reg, B(*box6), mat, mount, "true" if collides else "false")


def prop(reg, box6, mat, mount, collides, names, textures, elements, tab, display=None):
    facing_block(reg, java_for("BlockHealthcare", reg, box6, mat, mount, collides),
                 names, textures, elements, tab, display=display)


def screen_prop(reg, box6, mat, mount, collides, names, textures, elements, tab, on_tex,
                display=None, java=None):
    on = {"on": {"false": {}, "true": {"textures": {"screen": on_tex}}}}
    facing_block(reg, java or java_for("BlockHealthcareScreen", reg, box6, mat, mount,
                                       collides),
                 names, textures, elements, tab, extra=on, display=display)


def pair_block(reg, names, whole, axis, textures, java, tab, item_display=None):
    """As the wards': first cell 0..16, second -16..0 (wide, the viewer's right) or 16..32
    (tall)."""
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    if axis == 0:
        first, second = cut(whole, 0, 0, 32, origin=0), cut(whole, 0, -32, 0, origin=-16)
    else:
        first, second = cut(whole, 1, -16, 16, origin=0), cut(whole, 1, 16, 48, origin=16)
    models = {reg + "_first": model(textures, first),
              reg + "_second": model(textures, second),
              reg + "_item": model(textures, shift(whole, dx=8) if axis == 0 else whole,
                                   display=item_display or ITEM_SMALL)}
    state = hc.facing_state(M(reg + "_first"), {
        "second": {"false": {}, "true": {"model": M(reg + "_second")}}})
    state["variants"]["inventory"] = [{"model": M(reg + "_item")}]
    C.add(reg, java, names, models, state, tab=tab)


def pair_java(reg, box6, mat, mount, shape, collides=True):
    extra = "" if collides else ", false"
    return ('new BlockHealthcarePair("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.%s, '
            'BlockHealthcarePair.Shape.%s%s)' % (reg, B(*box6), mat, mount, shape, extra))


# ==========================================================================================
# Multi-block units (the CT and MRI scanners)
# ==========================================================================================
UNIT_ITEM = {"gui": {"rotation": [30, 225, 0], "translation": [0, -1.5, 0],
                     "scale": [0.2] * 3},
             "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.1] * 3},
             "fixed": {"rotation": [0, 0, 0], "translation": [0, -2, 0], "scale": [0.2] * 3},
             "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                       "scale": [0.12] * 3},
             "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                       "scale": [0.12] * 3}}

# What each unit reserves, for the plan's table and the Java: cells after the master as
# (dx, dy, dz) in the model's frame (+x east = the viewer's left, +z south = towards the back),
# and its solid boxes in sixteenths relative to the master cell.
UNITS = {}


def unit(reg, names, textures, elements, cells, boxes, tab):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    els = stretch_uvs(elements)
    for e in els:
        for v in e["from"] + e["to"]:
            assert -16 <= v <= 32, (reg, e)
    UNITS[reg] = (cells, boxes)
    flat_cells = [v for c in cells for v in c]
    flat_boxes = [v for b in boxes for v in b]
    java = 'new BlockHealthcareUnit("%s", %s, %s)' % (reg, B(*flat_cells), B(*flat_boxes))
    C.add(reg, java, names, {reg: model(textures, els, display=UNIT_ITEM)},
          hc.facing_state(M(reg)), tab=tab)


def bore_face(x0, x1, y0, y1, z0, z1, hx0, hx1, hy0, hy1, key, chamfer):
    """A slab from x0..x1, y0..y1, z0..z1 with a square hole hx0..hx1 x hy0..hy1 through it
    along z, its corners filled with 45-degree chamfers ``chamfer`` deep so the hole reads as an
    octagonal bore. Its outer top corners are chamfered too."""
    els = [el([x0, y0, z0], [hx0, y1, z1], key), el([hx1, y0, z0], [x1, y1, z1], key),
           el([hx0, y0, z0], [hx1, hy0, z1], key), el([hx0, hy1, z0], [hx1, y1, z1], key)]
    s = chamfer * math.sqrt(2) / 2
    for cx, cy in ((hx0, hy0), (hx1, hy0), (hx0, hy1), (hx1, hy1)):
        els.append(rot(el([cx - s, cy - s, z0 + 0.1], [cx + s, cy + s, z1 - 0.1], key,
                          faces=SIDES4[:2] + ("east", "west", "up", "down")),
                       "z", 45, [cx, cy, (z0 + z1) / 2.0]))
    return els


def corner_chamfers(points, d, z0, z1, key):
    s = d * math.sqrt(2) / 2
    return [rot(el([cx - s, cy - s, z0 + 0.05], [cx + s, cy + s, z1 - 0.05], key), "z", 45,
                [cx, cy, (z0 + z1) / 2.0]) for cx, cy in points]


def trim_ring(hx0, hx1, hy0, hy1, z, w, key):
    """A thin coloured ring round a bore's mouth, standing just proud of the face at z."""
    return [el([hx0 - w, hy0 - w, z - 0.4], [hx0, hy1 + w, z], key),
            el([hx1, hy0 - w, z - 0.4], [hx1 + w, hy1 + w, z], key),
            el([hx0, hy0 - w, z - 0.4], [hx1, hy0, z], key),
            el([hx0, hy1, z - 0.4], [hx1, hy1 + w, z], key)]


SCAN_TEX = {"shell": WARM, "white": WHITE, "accent": T("pale_blue"), "grey": GREY,
            "cradle": T("carbon"), "pad": W("mattress"), "display": T("ct_display"),
            "coil": T("coil"), "dark": CHAR, "particle": WARM}

# --- the CT scanner -----------------------------------------------------------------------
# The master is the table's middle cell. The table runs from the front (z -16) through the
# gantry's bore; the gantry stands in the row behind, three cells wide and two tall.
CT = (
    # the table
    [el([4, 0, -12], [12, 1.2, 6], "grey"),
     el([5, 1.2, -10], [11, 9, 4], "shell"),
     el([3.5, 9, -14], [12.5, 11, 14], "shell"),
     el([4, 11, -16], [12, 12, 28], "cradle"),
     el([4.5, 12, -15], [11.5, 12.6, 8], "pad"),
     el([5.5, 12, 8], [10.5, 14, 11], "pad"),
     el([3, 9.5, -8], [3.5, 10.5, 2], "grey"), el([12.5, 9.5, -8], [13, 10.5, 2], "grey")]
    # the gantry: a slab round the bore, chamfered at the top
    + bore_face(-10, 26, 0, 27, 18, 30, 2, 14, 9, 21, "shell", 3)
    + [el([-6, 27, 18], [22, 31, 30], "shell")]
    + corner_chamfers(((-6, 27), (22, 27)), 4, 18, 30, "shell")
    + [el([-10, 0, 17.6], [26, 1, 18], "dark")]
    + trim_ring(2, 14, 9, 21, 18, 1.5, "accent")
    + [front([-7, 15, 17.7], [-2, 20, 18], "shell", "display"),
       front([18, 15, 17.7], [23, 20, 18], "shell", "display")])
CT_CELLS = [(0, 0, -1)] + [(x, y, 1) for y in (0, 1) for x in (-1, 0, 1)]
CT_BOXES = [(3, 0, -16, 13, 12, 16), (-10, 0, 18, 26, 31, 30)]
unit("ct_scanner", ("CT Scanner", "CT-Scanner", "Escáner de TC", "Datortomograf (DT)"),
     SCAN_TEX, CT, CT_CELLS, CT_BOXES, CL)

# --- the MRI scanner ----------------------------------------------------------------------
# The master is the table's middle cell; the magnet fills the 3 x 2 cells around and behind it
# (its front cover from z 6), the table's foot reaches one cell forward.
MRI = (
    [el([3, 0, -16], [13, 1, 4], "grey"),
     el([4, 1, -14], [12, 9, 4], "shell"),
     el([3, 9, -16], [13, 10.5, 6], "shell"),
     el([4, 10.5, -16], [12, 11.5, 30], "cradle"),
     el([4.5, 11.5, -15], [11.5, 12.1, 3], "pad"),
     front([5, 11.5, 3], [11, 16.5, 9], "coil", "coil")]
    + bore_face(-12, 28, 0, 26, 8, 32, 2, 14, 10, 22, "shell", 3)
    + [el([-8, 26, 8], [24, 30, 32], "shell")]
    + corner_chamfers(((-8, 26), (24, 26)), 4, 8, 32, "shell")
    # the front cover, a ring round the bore's mouth
    + bore_face(-4, 20, 4, 28, 6, 8, 2, 14, 10, 22, "white", 3)
    + trim_ring(2, 14, 10, 22, 6, 1.2, "accent")
    + [el([-12, 0, 7.6], [28, 1, 8], "dark"),
       front([15, 24, 5.7], [19, 27, 6], "white", "display")])
MRI_CELLS = ([(x, y, 0) for y in (0, 1) for x in (-1, 0, 1) if (x, y) != (0, 0)]
             + [(x, y, 1) for y in (0, 1) for x in (-1, 0, 1)] + [(0, 0, -1)])
MRI_BOXES = [(3, 0, -16, 13, 12, 6), (-12, 0, 6, 28, 30, 32)]
unit("mri_scanner", ("MRI Scanner", "MRT-Scanner", "Escáner de RM", "MR-kamera"),
     SCAN_TEX, MRI, MRI_CELLS, MRI_BOXES, CL)

# ==========================================================================================
# X-ray
# ==========================================================================================
# The ceiling tube: the tube housing across a carriage on its telescoping column (the shared
# ceiling boom column above), the collimator under it reaching into the cell below, its light
# field the "screen" a click turns on.
XRAY_TUBE = (hcl.tube_up(12)
             + [el([5, 9, 5], [11, 12, 11], "grey")]
             + hc.pipe_x(6.5, 8, 2.6, 1.5, 14.5, "shell")
             + [el([0.8, 4.2, 5.6], [1.6, 8.8, 10.4], "grey"),
                el([14.4, 4.2, 5.6], [15.2, 8.8, 10.4], "grey"),
                front([4.5, -1, 4.5], [11.5, 3.9, 11.5], "shell", "coll"),
                el([4.8, -1.05, 4.8], [11.2, -1.05, 11.2], "screen", faces=("down",),
                   uv={"down": FULL}),
                el([3, 0, 7], [4.5, 2.6, 9], "grey"), el([11.5, 0, 7], [13, 2.6, 9], "grey")])
screen_prop("xray_ceiling_tube", (2, 0, 2, 14, 16, 14), "METAL", "FLOOR", True,
            ("Ceiling X-Ray Tube", "Deckenstativ-Röntgenstrahler",
             "Tubo de rayos X de techo", "Takmonterat röntgenrör"),
            {"shell": WARM, "grey": GREY, "metal": STEEL, "coll": T("coll_front"),
             "screen": T("coll_glass"), "particle": WARM},
            XRAY_TUBE, CL, T("coll_light"),
            java=hcl.HEAD_JAVA % ("xray_ceiling_tube", B(2, 0, 2, 14, 16, 14), 2),
            display=ITEM_SMALL)

# the radiographic table: two cells along the wall, a floating carbon top on a pedestal, the
# bucky tray under it
XTABLE = [el([-6, 0, 3], [6, 1, 13], "grey"),
          el([-4, 1, 5], [4, 8.5, 11], "shell"),
          el([-7, 8.5, 3.5], [7, 10.5, 12.5], "shell"),
          el([-15, 10.5, 2], [15, 12, 14], "carbon"),
          front([-5, 9, 2.5], [5, 10.5, 3.5], "grey", "bucky"),
          el([-14, 9, 2], [-13, 10.5, 2.6], "grey"), el([13, 9, 2], [14, 10.5, 2.6], "grey"),
          el([-14.5, 12, 3], [-13.5, 12.6, 13], "grey")]
pair_block("xray_table",
           ("X-Ray Table", "Röntgentisch", "Mesa de rayos X", "Röntgenbord"),
           XTABLE, 0,
           {"shell": WARM, "grey": GREY, "carbon": T("carbon"), "bucky": T("bucky_face"),
            "particle": WARM},
           pair_java("xray_table", (0, 0, 2, 16, 12, 14), "METAL", "FLOOR", "WIDE"), CL)

# the wall bucky stand: a column against the wall, the detector at chest height in the upper
# cell
BUCKY = [el([3, 0, 10], [13, 1, 16], "grey"),
         el([6, 1, 13], [10, 31, 16], "shell"),
         el([5, 31, 12.5], [11, 32, 16], "grey"),
         el([4.5, 17.5, 11], [11.5, 28.5, 13], "grey"),
         front([2, 16.5, 8.5], [14, 30, 11], "shell", "bucky"),
         el([2.5, 13, 9], [3.5, 16.5, 10], "grey"), el([12.5, 13, 9], [13.5, 16.5, 10], "grey"),
         el([2.5, 13, 9], [13.5, 14, 10], "grey")]
pair_block("xray_wall_bucky",
           ("Wall Bucky Stand", "Rasterwandstativ", "Bucky de pared", "Väggstativ (bucky)"),
           BUCKY, 1,
           {"shell": WARM, "grey": GREY, "bucky": T("bucky_face"), "particle": WARM},
           pair_java("xray_wall_bucky", (2, 0, 8, 14, 16, 16), "METAL", "FLOOR", "TALL"), CL,
           item_display=ITEM_TALL)


def arc(cy, cz, r, t, x0, x1, key, start=90, end=-90, step=22.5):
    """A C: boxes along a circle in the y-z plane centred on (cy, cz), radius r, thickness t,
    from ``start`` to ``end`` degrees (0 towards +z, 90 up). Each box is centred on a multiple
    of 22.5 degrees, so its turn is one a model element can take."""
    els = []
    seg = 2 * r * math.sin(math.radians(step / 2)) + t * 0.45
    phi = start
    while phi >= end - 1e-6:
        p = math.radians(phi)
        y, z = cy + r * math.sin(p), cz + r * math.cos(p)
        dy, dz = math.cos(p), -math.sin(p)          # the tangent
        if abs(dz) >= abs(dy):                       # long along z
            if dz < 0:
                dy, dz = -dy, -dz
            a = math.degrees(math.atan2(-dy, dz))
            box = el([x0, y - t / 2, z - seg / 2], [x1, y + t / 2, z + seg / 2], key)
        else:                                        # long along y
            if dy < 0:
                dy, dz = -dy, -dz
            a = math.degrees(math.atan2(dz, dy))
            box = el([x0, y - seg / 2, z - t / 2], [x1, y + seg / 2, z + t / 2], key)
        a = round(a / 22.5) * 22.5
        els.append(rot(box, "x", a, [round((x0 + x1) / 2, 3), round(y, 3), round(z, 3)])
                   if a else box)
        phi -= step
    return els


# the C-arm: its base and column in its own cell, the C reaching forward into the next cell
# over a table (drawn there, with no collision), the detector at the top, the tube below
CARM = (casters([(3.5, 4), (12.5, 4), (3.5, 15), (12.5, 15)])
        + [el([3, 0.8, 3], [13, 4, 16], "shell"),
           el([6, 4, 10], [10, 19, 14], "shell"),
           el([6.5, 16, 1], [9.5, 19, 10], "shell"),
           el([7, 15, -2], [9, 20, 1], "grey"),
           el([2, 12, 14], [3, 22, 15], "grey"), el([13, 12, 14], [14, 22, 15], "grey"),
           el([2, 21, 14], [14, 22, 15], "grey")]
        + arc(17.5, -9, 7.5, 2, 6.9, 9.1, "shell")
        + [el([5, 24, -13.5], [11, 26.5, -7], "shell", per={"down": "bucky"},
              uv={"down": FULL}),
           el([5.8, 7, -12], [10.2, 10, -7.5], "grey"),
           el([6.4, 10, -11.4], [9.6, 10.6, -8.1], "dark")])
pair_block("c_arm",
           ("Mobile C-Arm", "Mobiler C-Bogen", "Arco en C móvil", "Mobil C-båge"),
           CARM, 1,
           {"shell": WARM, "grey": GREY, "dark": CHAR, "rubber": RUBBER,
            "bucky": T("bucky_face"), "particle": WARM},
           pair_java("c_arm", (2, 0, 2, 14, 16, 16), "METAL", "FLOOR", "TALL"), CL,
           item_display=ITEM_TALL)

# the C-arm's monitor cart: two monitors side by side over a cart of recorders
CMON = (casters([(3, 4), (13, 4), (3, 12), (13, 12)])
        + [el([2.5, 0.8, 3.5], [13.5, 2, 12.5], "grey"),
           el([3, 2, 4], [13, 12, 12], "shell"),
           el([7, 12, 7], [9, 17, 9], "grey")]
        + [el([0.5, 16.5, 6.5], [7.8, 23, 8], "mbody"),
           screen([1, 17, 6.45], [7.3, 22.5, 6.45], "screen"),
           el([8.2, 16.5, 6.5], [15.5, 23, 8], "mbody"),
           screen([8.7, 17, 6.45], [15, 22.5, 6.45], "screen")])
screen_prop("c_arm_monitor_cart", (2, 0, 3, 14, 16, 13), "METAL", "FLOOR", True,
            ("C-Arm Monitor Cart", "Monitorwagen (C-Bogen)", "Carro de monitores del arco en C",
             "Monitorvagn (C-båge)"),
            {"shell": WARM, "grey": GREY, "rubber": RUBBER, "mbody": W("monitor_body"),
             "screen": W("screen_off"), "particle": WARM},
            CMON, CL, T("fluoro_screen"), display=ITEM_TALL)

# the mammography unit: a column, the arm with the tube head on top, the breast support and
# compression paddle at its foot, the face shield between
MAMMO = [el([3, 0, 7], [13, 1, 16], "grey"),
         el([5, 1, 12], [11, 31, 16], "shell"),
         el([6, 13, 8.5], [10, 30, 12], "shell"),
         el([4, 25.5, 2], [12, 30, 11], "shell"),
         el([5, 24.5, 3], [11, 25.5, 9], "grey"),
         el([4, 14.5, 1], [12, 17, 10], "dark"),
         el([4.5, 19.5, 1.5], [11.5, 20, 9], "glass"),
         el([7.5, 20, 7.5], [8.5, 24.5, 8.5], "grey"),
         el([4, 20.5, 8.6], [12, 26, 9], "glass"),
         el([2.4, 16, 4], [3.4, 21, 8], "grey"), el([12.6, 16, 4], [13.6, 21, 8], "grey")]
pair_block("mammography_unit",
           ("Mammography Unit", "Mammographiegerät", "Mamógrafo", "Mammografiapparat"),
           MAMMO, 1,
           {"shell": WARM, "grey": GREY, "dark": CHAR, "glass": T("sash"), "particle": WARM},
           pair_java("mammography_unit", (3, 0, 1, 13, 16, 16), "METAL", "FLOOR", "TALL"), CL,
           item_display=ITEM_TALL)

# the lead apron rack: a wall rail, four aprons on their hangers and two thyroid collars
APRONS = [el([1, 13.5, 14.5], [15, 14.5, 16], "metal")]
for _k, (_x, _key) in enumerate(((1.6, "blue"), (4.9, "purple"), (8.2, "teal"),
                                 (11.5, "blue"))):
    APRONS += [el([_x + 1.3, 12.5, 13.6], [_x + 1.9, 13.5, 14.5], "metal"),
               el([_x, 2.5, 12.9], [_x + 3.1, 12.5, 14.2], _key),
               el([_x + 0.4, 12.5, 13.1], [_x + 2.7, 13, 14], _key)]
APRONS += [el([5, 1, 14.4], [11, 2.2, 15.6], "teal")]
prop("lead_apron_rack", (1, 1, 12, 15, 15, 16), "CLOTH", "WALL", False,
     ("Lead Apron Rack", "Bleischürzen-Wandhalter", "Percha de delantales plomados",
      "Blyförklädeshängare"),
     {"metal": STEEL, "blue": T("apron_blue"), "purple": T("apron_purple"),
      "teal": T("apron_teal"), "particle": T("apron_blue")},
     APRONS, CL)

# the reading workstation: a desk two cells wide, two portrait diagnostic monitors and a
# worklist monitor, the keyboard, mouse and dictation microphone
READ = [el([-15.5, 11, 3], [15.5, 12, 15], "desk"),
        el([-15.5, 0, 4], [-14.5, 11, 14], "grey"), el([14.5, 0, 4], [15.5, 11, 14], "grey"),
        el([-14.5, 4, 13.5], [14.5, 11, 14], "grey")]
for _x0 in (-11.5, 0.5):
    READ += [el([_x0 + 4, 12, 11], [_x0 + 7, 12.4, 13], "grey"),
             el([_x0 + 5, 12.4, 12], [_x0 + 6, 14, 12.6], "grey"),
             el([_x0, 13.5, 11], [_x0 + 11, 27, 12], "mbody"),
             el([_x0 + 0.5, 14, 10.95], [_x0 + 10.5, 26.5, 10.95], "screen",
                faces=("north",), uv={"north": FULL})]
READ += [el([11.8, 12, 9], [15.2, 19, 10], "mbody"),
         el([12.1, 13.3, 8.95], [14.9, 18.7, 8.95], "work", faces=("north",),
            uv={"north": FULL}),
         el([-6, 12, 4.5], [4, 12.4, 7.5], "mbody", per={"up": "kbd"}, uv={"up": FULL}),
         el([6, 12, 5], [7.4, 12.5, 7], "mbody"),
         el([-10, 12, 5], [-8.6, 13.2, 9], "dark")]
pair_block("radiology_reading_workstation",
           ("Radiology Reading Workstation", "Befundungsarbeitsplatz (Radiologie)",
            "Estación de informes de radiología", "Granskningsstation (radiologi)"),
           READ, 0,
           {"desk": K("laminate"), "grey": GREY, "mbody": W("monitor_body"),
            "screen": T("reading_screen"), "work": K("chart_screen"), "kbd": K("keyboard"),
            "dark": CHAR, "particle": K("laminate")},
           pair_java("radiology_reading_workstation", (0, 0, 3, 16, 16, 15), "WOOD", "FLOOR",
                     "WIDE"), CL)

# ==========================================================================================
# Warning lights and signs
# ==========================================================================================
for _name, _en, _de, _es, _sv in (
        ("xray", "X-Ray In Use Warning Light", "Warnleuchte \"Röntgen in Betrieb\"",
         "Luz de aviso de rayos X en uso", "Varningslampa röntgen pågår"),
        ("mri", "MRI In Use Warning Light", "Warnleuchte \"MRT in Betrieb\"",
         "Luz de aviso de RM en uso", "Varningslampa MR pågår")):
    _reg = "%s_in_use_light" % _name
    C.add(_reg, 'new BlockHealthcareLight("%s", %s, 7)' % (_reg, B(2, 4, 12, 14, 12, 16)),
          (_en, _de, _es, _sv),
          {_reg: model({"body": DARK, "plate": GREY, "lens": T("%s_in_use_off" % _name),
                        "particle": DARK},
                       [el([1.5, 4.5, 15], [14.5, 11.5, 16], "plate"),
                        el([2, 5, 12.5], [14, 11, 15], "body"),
                        el([2.6, 5.6, 12.45], [13.4, 10.4, 12.45], "lens", faces=("north",),
                           uv={"north": FULL})])},
          hw.light_state(M(_reg), {"lens": T("%s_in_use_on" % _name)}), tab=CL)

ZONE_NAMES = {
    1: ("MRI Zone I Sign", "Schild MRT-Zone I", "Señal de zona I de RM", "Skylt MR-zon I"),
    2: ("MRI Zone II Sign", "Schild MRT-Zone II", "Señal de zona II de RM", "Skylt MR-zon II"),
    3: ("MRI Zone III Sign", "Schild MRT-Zone III", "Señal de zona III de RM",
        "Skylt MR-zon III"),
    4: ("MRI Zone IV Sign", "Schild MRT-Zone IV", "Señal de zona IV de RM", "Skylt MR-zon IV"),
}


def sign(reg, names, texture):
    prop(reg, (2, 2, 15, 14, 14, 16), "PLASTIC", "WALL", False, names,
         {"plate": WHITE, "face": texture, "particle": WHITE},
         [el([2, 2, 15.5], [14, 14, 16], "plate", faces=("south", "east", "west", "up", "down")),
          plane([2, 2, 15.5], [14, 14, 15.5], "face", "north")], CL)


for _z in (1, 2, 3, 4):
    sign("mri_zone_%d_sign" % _z, ZONE_NAMES[_z], T("mri_zone_%d" % _z))
sign("radiation_warning_sign",
     ("Radiation Warning Sign (X-Ray)", "Strahlenwarnschild (Röntgen)",
      "Señal de advertencia de radiación (rayos X)", "Strålningsvarningsskylt (röntgen)"),
     T("radiation_sign"))

# ==========================================================================================
# The laboratory
# ==========================================================================================
LAB_TEX = {"cab": N("cabinet"), "doors": T("lab_cabinet"), "epoxy": T("epoxy"),
           "grey": GREY, "metal": STEEL, "bottles": T("bottles"), "bowl": T("sink_bowl"),
           "particle": T("epoxy")}
BENCH_BASE = [el([0, 0, 1], [16, 14.5, 16], "cab", faces=("north", "south", "down"),
                 per={"north": "doors"})]
BENCH_TOP = [el([0, 14.5, 0], [16, 16, 16], "epoxy")]
BENCH_SHELF = [el([0, 16, 15.5], [16, 17.5, 16], "epoxy"),
               el([0, 16, 12.5], [1, 30, 16], "grey"), el([15, 16, 12.5], [16, 30, 16], "grey"),
               el([1, 23, 12], [15, 23.8, 16], "grey"),
               el([1, 29.2, 12], [15, 30, 16], "grey"),
               el([1, 23.8, 15.4], [15, 29.2, 16], "grey", faces=("north",)),
               plane([1, 23.8, 13.5], [15, 29.2, 13.5], "bottles", "north")]
BENCH_END = [el([15.25, 0, 0.5], [16, 14.5, 16], "cab")]
# the sink: a basin let into the worktop, a gooseneck tap from the shelf's riser
SINK_TOP = [el([0, 14.5, 0], [16, 16, 3], "epoxy"), el([0, 14.5, 11], [16, 16, 16], "epoxy"),
            el([0, 14.5, 3], [4, 16, 11], "epoxy"), el([12, 14.5, 3], [16, 16, 11], "epoxy"),
            el([4, 11, 3], [12, 11, 11], "bowl", faces=("up",), uv={"up": FULL}),
            el([4, 11, 3], [12, 16, 3], "metal", faces=("south",)),
            el([4, 11, 11], [12, 16, 11], "metal", faces=("north",)),
            el([4, 11, 3], [4, 16, 11], "metal", faces=("east",)),
            el([12, 11, 3], [12, 16, 11], "metal", faces=("west",)),
            el([7.4, 16, 12.6], [8.6, 22, 13.8], "metal"),
            el([7.4, 21, 8], [8.6, 22.2, 13.8], "metal"),
            el([7.5, 19.5, 8], [8.5, 21, 9], "metal"),
            el([5, 16, 12.8], [6.2, 17.4, 14], "metal"), el([9.8, 16, 12.8], [11, 17.4, 14],
                                                           "metal")]


def bench_state(body):
    out = []
    for f, y in hc.ROT_Y.items():
        out.append({"when": {"facing": f}, "apply": {"model": M(body), "y": y}})
        out.append({"when": {"facing": f, "east": "false"},
                    "apply": {"model": M("lab_bench_end_east"), "y": y}})
        out.append({"when": {"facing": f, "west": "false"},
                    "apply": {"model": M("lab_bench_end_west"), "y": y}})
    return {"multipart": out}


C.add("lab_bench", 'new BlockLabBench("lab_bench")',
      ("Lab Bench with Reagent Shelf", "Laborbank mit Reagenzienregal",
       "Mesa de laboratorio con estante de reactivos", "Labbänk med reagenshylla"),
      {"lab_bench": model(LAB_TEX, BENCH_BASE + BENCH_TOP + BENCH_SHELF),
       "lab_bench_end_east": model(LAB_TEX, BENCH_END),
       "lab_bench_end_west": model(LAB_TEX, hw.mirror_x(BENCH_END)),
       "lab_bench_item": model(LAB_TEX, BENCH_BASE + BENCH_TOP + BENCH_SHELF + BENCH_END
                               + hw.mirror_x(BENCH_END), display=ITEM_TALL)},
      bench_state("lab_bench"), item={"parent": MI("lab_bench_item")}, tab=CL)
C.add("lab_bench_sink", 'new BlockLabBench("lab_bench_sink")',
      ("Lab Bench with Sink", "Laborbank mit Spüle", "Mesa de laboratorio con fregadero",
       "Labbänk med diskho"),
      {"lab_bench_sink": model(LAB_TEX, BENCH_BASE + SINK_TOP + BENCH_SHELF),
       "lab_bench_sink_item": model(LAB_TEX, BENCH_BASE + SINK_TOP + BENCH_SHELF + BENCH_END
                                    + hw.mirror_x(BENCH_END), display=ITEM_TALL)},
      bench_state("lab_bench_sink"), item={"parent": MI("lab_bench_sink_item")}, tab=CL)

# the chemistry analyser: a floor-standing cabinet, its lid over the reagent carousel, a
# monitor on an arm
CHEM = [front([0.5, 0, 2], [15.5, 13.5, 15], "body", "front"),
        el([0.5, 13.5, 2], [15.5, 14.5, 15], "body", per={"up": "top"}, uv={"up": FULL}),
        el([0.5, 0, 1.6], [15.5, 1, 2], "dark"),
        el([13, 14.5, 10], [14, 20, 11], "grey"),
        el([8.5, 18, 6.5], [15.5, 23.5, 8], "mbody"),
        screen([9, 18.5, 6.45], [15, 23, 6.45], "screen")]
screen_prop("chemistry_analyser", (0, 0, 2, 16, 16, 15), "METAL", "FLOOR", True,
            ("Chemistry Analyser", "Klinisch-chemischer Analysator",
             "Analizador de bioquímica", "Kemianalysator"),
            {"body": WHITE, "front": T("chem_front"), "top": T("chem_top"), "dark": CHAR,
             "grey": GREY, "mbody": W("monitor_body"), "screen": W("screen_off"),
             "particle": WHITE},
            CHEM, CL, T("analyser_screen"), display=ITEM_TALL)

# the haematology analyser: a bench-top unit with its screen on the front, a rack of tubes
HAEM = [front([2, 0, 4], [14, 10, 14], "body", "front"),
        el([8, 5, 3.6], [13.5, 9.5, 4], "dark"),
        screen([8.4, 5.4, 3.55], [13.1, 9.1, 3.55], "screen"),
        el([2.5, 0, 1], [7.5, 2.5, 3.5], "rack", per={"up": "tubes"}, uv={"up": FULL})]
screen_prop("haematology_analyser", (2, 0, 1, 14, 10, 14), "METAL", "FLOOR", True,
            ("Haematology Analyser (Bench-Top)", "Hämatologie-Analysator (Tischgerät)",
             "Analizador de hematología (de sobremesa)", "Hematologianalysator (bänk)"),
            {"body": WHITE, "front": T("haem_front"), "dark": CHAR, "rack": WHITE,
             "tubes": T("tube_rack"), "screen": W("screen_off"), "particle": WHITE},
            HAEM, CL, T("analyser_screen"))

CENT = [front([3, 0, 3], [13, 7, 13], "body", "front"),
        el([3.5, 7, 3.5], [12.5, 8.4, 12.5], "grey", per={"up": "top"}, uv={"up": FULL}),
        el([3, 0, 2.6], [13, 0.6, 3], "dark")]
prop("lab_centrifuge", (3, 0, 3, 13, 9, 13), "METAL", "FLOOR", True,
     ("Centrifuge (Bench-Top)", "Zentrifuge (Tischgerät)", "Centrífuga (de sobremesa)",
      "Centrifug (bänk)"),
     {"body": WHITE, "front": T("centrifuge_front"), "grey": GREY, "top": T("centrifuge_top"),
      "dark": CHAR, "particle": WHITE}, CENT, CL)

MICRO = [el([5, 0, 4], [11, 1.5, 12.5], "dark"),
         el([6.5, 1.5, 9.5], [9.5, 11.5, 12], "dark"),
         el([4.5, 5, 3], [11.5, 5.8, 9.5], "grey"),
         el([7.2, 2.5, 5.5], [8.8, 5, 7], "metal"),
         el([6.5, 9.5, 4.5], [9.5, 10.5, 9.5], "dark"),
         el([7.4, 8, 5.4], [8.6, 9.5, 6.6], "metal"),
         el([6, 10.5, 6], [10, 13, 11], "white"),
         rot(el([6, 12, 3.5], [7.4, 13.4, 7], "dark"), "x", 45, [6.7, 12.7, 6]),
         rot(el([8.6, 12, 3.5], [10, 13.4, 7], "dark"), "x", 45, [9.3, 12.7, 6]),
         el([9.5, 4, 10], [10.8, 5.6, 11.3], "metal"), el([5.2, 4, 10], [6.5, 5.6, 11.3],
                                                         "metal")]
prop("lab_microscope", (4, 0, 3, 12, 14, 13), "METAL", "FLOOR", True,
     ("Microscope", "Mikroskop", "Microscopio", "Mikroskop"),
     {"dark": CHAR, "grey": GREY, "metal": STEEL, "white": WHITE, "particle": CHAR},
     MICRO, CL)


def hood(work_open):
    """What the fume hood and the biosafety cabinet share: the sides, back and top band of a
    cabinet over a worktop, from y 15 to 32."""
    return [el([0, 15.5, 1], [1.5, 29, 16], "body"), el([14.5, 15.5, 1], [16, 29, 16], "body"),
            el([1.5, 15.5, 15], [14.5, 29, 16], "liner", faces=("north",)),
            el([0, 29, 1], [16, 32, 16], "body", per={"north": "band"}, uv={"north": FULL})]


FUME = (BENCH_BASE + [el([0, 14.5, 0.5], [16, 15.5, 16], "epoxy")] + hood(True)
        + [el([1.5, 15.5, 0.5], [14.5, 16.2, 2], "metal"),
           el([1.5, 22, 1.5], [14.5, 23, 3], "body"),
           el([1.5, 23, 2], [14.5, 29, 2.5], "glass", faces=("north", "south")),
           el([1.5, 28.4, 1.5], [14.5, 29, 3], "body"),
           el([1.5, 15.5, 14.6], [14.5, 17, 15], "metal")])
pair_block("fume_hood",
           ("Fume Hood", "Laborabzug", "Campana de extracción de gases", "Dragskåp"),
           FUME, 1,
           {"cab": N("cabinet"), "doors": T("lab_cabinet"), "epoxy": T("epoxy"), "body": WHITE,
            "liner": T("liner"), "band": T("hood_band"), "metal": STEEL, "glass": T("sash"),
            "particle": WHITE},
           pair_java("fume_hood", (0, 0, 0, 16, 16, 16), "METAL", "FLOOR", "TALL"), CL,
           item_display=ITEM_TALL)

BSC = ([el([1, 0, 2], [2, 14, 3], "grey"), el([14, 0, 2], [15, 14, 3], "grey"),
        el([1, 0, 14], [2, 14, 15], "grey"), el([14, 0, 14], [15, 14, 15], "grey"),
        el([1, 3, 2.5], [15, 3.8, 14.5], "grey"),
        el([0, 14, 1], [16, 15.5, 16], "body")]
       + hood(False)
       + [el([1.5, 15.5, 0.5], [14.5, 16.4, 3], "metal"),
          el([1.5, 16.4, 2], [14.5, 16.8, 15], "metal"),
          el([1.5, 21.5, 1.5], [14.5, 22.3, 3], "body"),
          el([1.5, 22.3, 2], [14.5, 29, 2.5], "glass", faces=("north", "south"))])
pair_block("biosafety_cabinet",
           ("Biosafety Cabinet", "Mikrobiologische Sicherheitswerkbank",
            "Cabina de bioseguridad", "Säkerhetsbänk (mikrobiologisk)"),
           BSC, 1,
           {"body": WHITE, "liner": T("liner"), "band": T("bsc_band"), "metal": STEEL,
            "grey": GREY, "glass": T("sash"), "particle": WHITE},
           pair_java("biosafety_cabinet", (0, 0, 1, 16, 16, 16), "METAL", "FLOOR", "TALL"), CL,
           item_display=ITEM_TALL)

# the blood bank refrigerator: a glass door showing shelves of bags, its display at the top
BLOOD = [el([0.5, 0, 1], [15.5, 31, 16], "body"),
         el([1, 1, 0.5], [15, 16, 1], "body", per={"north": "door_lo"}),
         el([1, 16, 0.5], [15, 30, 1], "body", per={"north": "door_hi"}),
         el([13.6, 10, 0], [14.4, 22, 0.5], "metal"),
         el([1, 0, 0.6], [15, 1, 1], "dark")]
pair_block("blood_bank_fridge",
           ("Blood Bank Refrigerator", "Blutbank-Kühlschrank", "Refrigerador de banco de sangre",
            "Blodbankskylskåp"),
           BLOOD, 1,
           {"body": WHITE, "door_lo": T("blood_door_lo"), "door_hi": T("blood_door_hi"),
            "metal": STEEL, "dark": CHAR, "particle": WHITE},
           pair_java("blood_bank_fridge", (0, 0, 0, 16, 16, 16), "METAL", "FLOOR", "TALL"), CL,
           item_display=ITEM_TALL)

prop("specimen_pass_through", (2, 3, 12, 14, 13, 16), "METAL", "WALL", False,
     ("Specimen Pass-Through", "Probendurchreiche", "Ventanilla de paso de muestras",
      "Provgenomlämning"),
     {"metal": STEEL, "front": T("pt_door"), "grey": GREY, "particle": STEEL},
     [el([2, 3, 13], [14, 13, 16], "metal"),
      front([3, 4, 12.6], [13, 12, 13], "metal", "front"),
      el([11.5, 6.5, 12], [12.5, 9.5, 12.6], "grey")], CL)

prop("pneumatic_tube_station", (3, 0, 9, 13, 16, 16), "METAL", "WALL", False,
     ("Pneumatic Tube Station", "Rohrpoststation", "Estación de tubo neumático",
      "Rörpoststation"),
     {"body": WHITE, "front": T("tube_front"), "tube": T("tube_grey"), "grey": GREY,
      "carrier": T("carrier"), "particle": WHITE},
     [front([3, 2, 12], [13, 13, 16], "body", "front"),
      el([6.5, 13, 13], [9.5, 16, 16], "tube"),
      el([4, 0.5, 9], [12, 1, 12], "grey"),
      el([4, 1, 9], [4.5, 4, 12], "grey"), el([11.5, 1, 9], [12, 4, 12], "grey"),
      el([4.5, 1, 9], [11.5, 4, 9.5], "grey"),
      el([5, 1, 10], [11, 3, 11.8], "carrier")], CL)

# ==========================================================================================
# The pharmacy
# ==========================================================================================
COMPOUND = ([el([1, 0, 2], [2, 14, 3], "grey"), el([14, 0, 2], [15, 14, 3], "grey"),
             el([1, 0, 14], [2, 14, 15], "grey"), el([14, 0, 14], [15, 14, 15], "grey"),
             el([1, 4, 2.5], [15, 4.8, 14.5], "grey"),
             el([0, 14, 1], [16, 15.5, 16], "body"),
             el([1, 15.5, 2], [15, 16, 15], "metal"),
             el([0, 15.5, 1], [1, 28.5, 16], "glass", faces=("east", "west")),
             el([15, 15.5, 1], [16, 28.5, 16], "glass", faces=("east", "west")),
             el([0, 15.5, 1], [1, 28.5, 2], "body"), el([15, 15.5, 1], [16, 28.5, 2], "body"),
             front([1, 16, 14], [15, 28.5, 16], "body", "hepa"),
             el([0, 28.5, 1], [16, 32, 16], "body", per={"north": "band"}, uv={"north": FULL}),
             el([2, 26, 7], [14, 26.5, 7.6], "metal")]
            + sum(([el([_x, 21.5, 7], [_x + 2, 25, 7.6], "bag"),
                    el([_x + 0.8, 25, 7.1], [_x + 1.2, 26, 7.5], "metal")]
                   for _x in (3.5, 7, 10.5)), []))
pair_block("iv_compounding_hood",
           ("Sterile Compounding Hood", "Sterilwerkbank (Zytostatika/IV)",
            "Cabina de preparación estéril", "LAF-bänk för sterilberedning"),
           COMPOUND, 1,
           {"body": WHITE, "metal": STEEL, "grey": GREY, "glass": T("sash"), "hepa": T("hepa"),
            "band": T("compounding_band"), "bag": W("iv_bag"), "particle": WHITE},
           pair_java("iv_compounding_hood", (0, 0, 1, 16, 16, 16), "METAL", "FLOOR", "TALL"),
           CL, item_display=ITEM_TALL)

BINS = []
for _x, _z in ((0.5, 1), (14.5, 1), (0.5, 14), (14.5, 14)):
    BINS.append(el([_x, 0, _z], [_x + 1, 31, _z + 1], "metal"))
for _y in (1, 8, 15.5, 23, 30):
    BINS.append(el([0.5, _y, 1], [15.5, _y + 0.6, 15], "shelf"))
for _y in (1.6, 8.6, 16.1, 23.6):
    BINS.append(el([1.5, _y, 1.5], [14.5, _y + 5, 13.5], "bin", per={"north": "bins"},
                   uv={"north": FULL}))
pair_block("pharmacy_bin_shelving",
           ("Inpatient Pharmacy Bin Shelving", "Klinikapotheke: Regal mit Sichtlagerkästen",
            "Estantería con cubetas de farmacia hospitalaria",
            "Sjukhusapotek: hyllställ med backar"),
           BINS, 1,
           {"metal": STEEL, "shelf": W("wire_shelf"), "bin": T("bin_blue"), "bins": T("bin_front"),
            "particle": T("bin_blue")},
           pair_java("pharmacy_bin_shelving", (0, 0, 1, 16, 16, 15), "METAL", "FLOOR", "TALL"),
           CL, item_display=ITEM_TALL)

UNIT_DOSE = [el([0.5, 0, 1], [15.5, 31, 16], "body", faces=("south", "east", "west", "up",
                                                            "down")),
             el([0.5, 0, 0.6], [15.5, 1, 1], "dark"),
             el([1, 1, 0.8], [15, 16, 1], "body", per={"north": "drawers"}),
             el([1, 16, 0.8], [15, 30, 1], "body", per={"north": "drawers"}),
             el([0.5, 1, 1], [15.5, 31, 1], "body", faces=("north",))]
pair_block("pharmacy_unit_dose_cabinet",
           ("Inpatient Pharmacy Unit-Dose Cabinet", "Klinikapotheke: Einzeldosis-Schubladenschrank",
            "Armario de dosis unitarias de farmacia hospitalaria",
            "Sjukhusapotek: dosdispenseringsskåp"),
           UNIT_DOSE, 1,
           {"body": W("warm_white"), "drawers": T("unit_dose"), "dark": CHAR,
            "particle": W("warm_white")},
           pair_java("pharmacy_unit_dose_cabinet", (0, 0, 0, 16, 16, 16), "METAL", "FLOOR",
                     "TALL"), CL, item_display=ITEM_TALL)

# ==========================================================================================
# The morgue
# ==========================================================================================
# The cabinet: two tiers, one door each. Each cell's model is drawn on its own (no cut), closed
# or open: open, the door stands square to the front on its hinge (the viewer's left, model +x)
# and the tray is slid half out.
MORGUE_TEX = {"body": STEEL, "door": T("morgue_door"), "recess": T("morgue_recess"),
              "dark": CHAR, "grey": GREY, "particle": STEEL}


def morgue_cell(upper, open_):
    els = [el([0, 0, 0.5], [16, 16, 16], "body", faces=("south", "east", "west")
              + (("up",) if upper else ("down",)))]
    if not upper:
        els.append(el([0, 0, 0.2], [16, 1, 0.5], "dark"))
    else:
        els.append(el([0, 15, 0.2], [16, 16, 0.5], "body"))
    lo, hi = (1, 15)
    if not open_:
        els.append(el([0, lo, 0.5], [16, hi, 0.5], "body", faces=("north",)))
        els.append(el([1, lo + 0.5, 0], [15, hi - 0.5, 0.5], "door",
                      faces=("north", "east", "west", "up", "down"), per={"north": "door"}))
    else:
        els += [el([0, lo, 0.5], [1, hi, 1], "body"), el([15, lo, 0.5], [16, hi, 1], "body"),
                el([1, lo, 0.5], [15, lo + 0.5, 1], "body"),
                el([1, hi - 0.5, 0.5], [15, hi, 1], "body"),
                el([1, lo + 0.5, 6], [15, hi - 0.5, 6], "recess", faces=("north",),
                   uv={"north": FULL}),
                el([1, lo + 0.5, 1], [1, hi - 0.5, 6], "dark", faces=("east",)),
                el([15, lo + 0.5, 1], [15, hi - 0.5, 6], "dark", faces=("west",)),
                el([1, lo + 0.5, 1], [15, lo + 0.5, 6], "dark", faces=("up",)),
                el([1, hi - 0.5, 1], [15, hi - 0.5, 6], "dark", faces=("down",)),
                # the door, swung square to the front
                el([15, lo + 0.5, -13.5], [15.5, hi - 0.5, 0.5], "body",
                   per={"west": "door", "east": "door"}),
                # the tray, slid half out on its rails
                el([2.5, lo + 2, -10], [13.5, lo + 2.6, 6], "body"),
                el([2.5, lo + 2.6, -10], [3, lo + 3.4, 6], "body"),
                el([13, lo + 2.6, -10], [13.5, lo + 3.4, 6], "body"),
                el([2.5, lo + 2.6, -10], [13.5, lo + 3.4, -9.5], "body"),
                el([2, lo + 1, -10], [14, lo + 2, -9], "grey")]
    return els


_MC = {}
for _u in (False, True):
    for _o in (False, True):
        _MC["morgue_cabinet_%s_%s" % ("upper" if _u else "lower", "open" if _o else "closed")] = \
            model(MORGUE_TEX, morgue_cell(_u, _o))
_MC["morgue_cabinet_item"] = model(MORGUE_TEX, morgue_cell(False, False)
                                   + shift(morgue_cell(True, False), dy=16), display=ITEM_TALL)
_ms = hc.keyed_state([("facing", list(hc.ROT_Y)), ("second", ["false", "true"]),
                      ("open", ["false", "true"])],
                     lambda v: {"model": M("morgue_cabinet_%s_%s" % (
                         "upper" if v["second"] == "true" else "lower",
                         "open" if v["open"] == "true" else "closed")),
                         "y": hc.ROT_Y[v["facing"]]})
_ms["variants"]["inventory"] = [{"model": M("morgue_cabinet_item")}]
C.add("morgue_cabinet", "new BlockMorgueCabinet()",
      ("Morgue Refrigerator Cabinet", "Leichenkühlschrank", "Cámara frigorífica mortuoria",
       "Bårhuskylskåp"),
      _MC, _ms, tab=CL)

AUTOPSY = [el([-6, 0, 3], [6, 1, 13], "metal"),
           el([-3, 1, 5], [3, 9, 11], "metal"),
           el([-15, 9, 2], [15, 10.5, 14], "metal", per={"up": "top"}, uv={"up": FULL}),
           el([-15, 10.5, 2], [15, 11.5, 2.8], "metal"),
           el([-15, 10.5, 13.2], [15, 11.5, 14], "metal"),
           el([-15, 10.5, 2.8], [-14.2, 11.5, 13.2], "metal"),
           el([14.2, 10.5, 2.8], [15, 11.5, 13.2], "metal"),
           el([11, 9, 14], [15, 13, 16], "metal"),
           el([12.4, 13, 14.6], [13.6, 17, 15.6], "metal"),
           el([12.4, 16, 10], [13.6, 17.2, 15.6], "metal"),
           el([-12, 9.2, 5], [-8, 10.6, 11], "dark")]
pair_block("autopsy_table",
           ("Autopsy Table", "Sektionstisch", "Mesa de autopsias", "Obduktionsbord"),
           AUTOPSY, 0,
           {"metal": STEEL, "top": T("autopsy_top"), "dark": CHAR, "particle": STEEL},
           pair_java("autopsy_table", (0, 0, 2, 16, 12, 16), "METAL", "FLOOR", "WIDE"), CL)

BODY_CART = (casters([(-13, 4), (13, 4), (-13, 12), (13, 12)])
             + [el([-14, 0.8, 3.5], [14, 1.6, 12.5], "metal"),
                el([-14, 1.6, 3], [-13, 8, 4], "metal"), el([13, 1.6, 3], [14, 8, 4], "metal"),
                el([-14, 1.6, 12], [-13, 8, 13], "metal"), el([13, 1.6, 12], [14, 8, 13], "metal"),
                el([-14.5, 3.5, 3], [14.5, 4, 13], "metal"),
                el([-15, 8, 2.5], [15, 9, 13.5], "metal"),
                el([-14.5, 9, 3], [14.5, 14.5, 13], "cover"),
                el([-14, 14.5, 4], [14, 15.8, 12], "cover"),
                el([-15.8, 9, 5], [-15, 12, 11], "metal"), el([15, 9, 5], [15.8, 12, 11],
                                                           "metal")])
pair_block("body_cart",
           ("Body Cart (Covered)", "Leichentransportwagen (abgedeckt)",
            "Carro mortuorio (cubierto)", "Bårvagn (täckt)"),
           BODY_CART, 0,
           {"metal": STEEL, "cover": T("cover_navy"), "rubber": RUBBER,
            "particle": STEEL},
           pair_java("body_cart", (0, 0, 2, 16, 16, 14), "METAL", "FLOOR", "WIDE"), CL)

# ==========================================================================================
# The unit part (hidden): draws nothing; its blockstate names an empty model
# ==========================================================================================
C.add("healthcare_unit_part", "new BlockHealthcareUnitPart()",
      ("Healthcare Unit Part", "Teil einer Healthcare-Einheit", "Parte de unidad sanitaria",
       "Del av vårdenhet"),
      {"unit_part": {"textures": {"particle": WARM}, "elements": []}},
      {"forge_marker": 1, "defaults": {"model": M("unit_part")},
       "variants": {"toward": {d: {} for d in ("down", "up", "north", "south", "west",
                                               "east")},
                    "inventory": [{}]}}, tab=HID)


def main():
    rc = C.main()
    if rc == 0 and "--check" not in sys.argv and "--fragments" not in sys.argv:
        written = []
        for root, _dirs, files in os.walk(hc.ASSETS):
            written += [os.path.join(root, f) for f in files]
        hc.check_json(written)
    return rc


if __name__ == "__main__":
    sys.exit(main())
