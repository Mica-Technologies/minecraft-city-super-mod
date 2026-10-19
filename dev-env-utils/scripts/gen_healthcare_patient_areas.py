#!/usr/bin/env python3
"""
gen_healthcare_patient_areas.py -- the Healthcare module's maternity, rehabilitation, waiting
room, teaching and playroom pieces.

Maternity: the infant radiant warmer (its heater head lit by Lighting's states), the bassinet
and the NICU incubator with its animated monitor. (The labour and delivery bed is a hospital
bed, written by gen_healthcare_wards.py.) Rehabilitation: the parallel bars, which join into one
run, the treatment plinth and the training stairs. The waiting room: beam seating in three
colours that joins into rows, the check-in kiosk, the desk's queue call console and the
now-serving display, whose number TileEntityNowServingDisplayRenderer draws. Teaching: the
patient simulation mannequin (a block set over a bed or table and drawn sunk down onto it), the
anatomical torso model, the skeleton on its stand, the dissection table and tiered lecture hall
seating. The paediatric playroom: a low table, kids' chairs and a toy box.

Nothing here is a real product: no maker's name, no red cross (a protected emblem). Screens are
animated textures (.mcmeta), so nothing ticks; the now-serving display's text is the module's
second renderer, beside the ED tracking board's.

Swatches the wards, clinical, clinics and diagnostics generators already draw are shared by
path, not drawn again.

Writes, under modules/healthcare/src/main/resources/assets/csm:

  * textures/blocks/healthcare/patient/*.png (+ .mcmeta for the screens)
  * models/block/healthcare/patient/*.json
  * blockstates/<registry>.json, and models/item/<registry>.json for the multipart blocks
  * the tile names and the queue's message in all four lang files

Every model faces north with what it stands against at +Z.

Usage:
    python gen_healthcare_patient_areas.py              # write everything
    python gen_healthcare_patient_areas.py --check      # fail if the tree has drifted
    python gen_healthcare_patient_areas.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_healthcare_common as hc  # noqa: E402
import gen_healthcare_wards as hw  # noqa: E402
import gen_healthcare_clinical as hcl  # noqa: E402
import gen_healthcare_clinics as hcn  # noqa: E402
import gen_healthcare_diagnostics as hd  # noqa: E402
from gen_healthcare_common import (  # noqa: E402
    disc, draw_text, draw_text_centred, fill, frame, rect, shade)

C = hc.catalogue("gen_healthcare_patient_areas.py", "patient")
T = C.T
M = C.M
W = hw.C.T    # the wards' shared swatches
K = hcl.C.T   # the clinical generator's
N = hcn.C.T   # the clinics generator's
D = hd.C.T    # the diagnostics generator's

el = hw.el
screen = hw.screen
rot = hw.rot
octo = hw.octo
cut = hw.cut
shift = hw.shift
model = hw.model
casters = hw.casters
mirror_x = hw.mirror_x
ITEM_SMALL = hw.ITEM_SMALL
ITEM_TALL = hw.ITEM_TALL
B = hw.B
FULL = [0, 0, 16, 16]

PC = hc.PATIENT_CARE
CL = hc.CLINICAL

STEEL = W("stainless")
GREY = W("frame_grey")
RUBBER = W("rubber")
BEECH = W("beech")


def MI(name):
    """An item model's parent: item models resolve from models/, so the block/ is needed."""
    return "csm:block/%s/%s" % (C.model_dir, name)


def front(frm, to, base, key, side="north", faces=hw.ALL):
    """A box whose front (north, or ``side``) shows the whole of a texture."""
    return el(frm, to, base, faces=faces, per={side: key}, uv={side: FULL})


def plane(frm, to, key, side):
    """A zero-thickness face showing the whole of a texture."""
    return el(frm, to, key, faces=(side,), uv={side: FULL})


def tex(name, fn):
    C.textures[name] = fn


# ==========================================================================================
# Textures
# ==========================================================================================
for i, (n, c, g) in enumerate((
        ("manikin", (214, 184, 156), 2), ("bone", (232, 224, 200), 2),
        ("kid_red", (210, 62, 52), 3), ("kid_blue", (60, 120, 200), 3),
        ("kid_green", (80, 164, 84), 3), ("kid_yellow", (240, 196, 50), 3),
        ("warmer_head", (236, 236, 232), 2), ("seat_blue", (52, 86, 140), 3),
        ("carpet", (70, 78, 92), 6))):
    tex(n, (lambda c=c, g=g, s=7000 + i: hc.swatch(c, s, grain=g)))


@C.texture("gown")
def _gown():
    """A hospital gown: pale blue with a small darker print."""
    return hc.fabric((176, 200, 222), 7020, pattern="dots")


@C.texture("swaddle")
def _swaddle():
    """The receiving blanket: white flannel with a pink and a blue stripe."""
    img = hc.fabric((240, 238, 232), 7021)
    for y, col in ((3, (226, 140, 160)), (5, (110, 160, 210)), (11, (110, 160, 210)),
                   (13, (226, 140, 160))):
        rect(img, 0, y, 16, y + 1, col)
    return img


@C.texture("acrylic")
def _acrylic():
    """A clear acrylic panel: transparent, with its edges catching the light."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    frame(img, 0, 0, 16, 16, (214, 232, 240))
    return img


@C.texture("incubator_hood")
def _incubator_hood():
    """The incubator hood's front: clear, its edges, and two hand ports with their rings."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    frame(img, 0, 0, 32, 32, (214, 232, 240))
    px = img.load()
    for cx in (9, 23):
        for y in range(32):
            for x in range(32):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - 17)
                if 4.2 <= d < 5.6:
                    px[x, y] = (226, 228, 222, 255)
    return img


@C.texture("warmer_panel")
def _warmer_panel():
    """The warmer's controller: the skin temperature, the heater's bar graph, the timer."""
    img = fill((226, 228, 226), grain=1, seed=7030)
    rect(img, 2, 2, 14, 9, (20, 26, 30))
    draw_text(img, "36", 3, 3, (250, 160, 60))
    for k in range(4):
        rect(img, 11, 7 - k * 1, 13, 8 - k * 1, (250, 120, 40) if k < 3 else (90, 50, 30))
    rect(img, 3, 11, 6, 14, (90, 94, 100))
    rect(img, 10, 11, 13, 14, (60, 120, 200))
    return img


@C.texture("heater_off")
def _heater_off():
    """The heater's grille, cold."""
    img = fill((150, 150, 148), grain=1, seed=7031)
    for y in range(1, 16, 3):
        rect(img, 1, y, 15, y + 1, (96, 96, 94))
    return img


@C.texture("heater_on")
def _heater_on():
    """The heater's quartz elements glowing."""
    img = fill((150, 120, 100), grain=1, seed=7032)
    for y in range(1, 16, 3):
        rect(img, 1, y, 15, y + 1, (255, 120, 50))
        rect(img, 2, y, 14, y + 1, (255, 190, 90))
    return img


@C.texture("anatomy_front")
def _anatomy_front():
    """The anatomical torso's front: skin on its right half, the muscles, lungs, heart and gut
    on its left, as a teaching model is painted."""
    img = fill((214, 184, 156), size=32, grain=1, seed=7040)
    px = img.load()
    for y in range(32):
        for x in range(16):
            px[x, y] = shade((180, 70, 64), 1.0 + 0.08 * math.sin(y * 1.3 + x * 0.4)) + (255,)
    # lungs, heart, liver, stomach and the gut
    for cx, cy, r, col in ((5, 8, 4.2, (226, 140, 150)), (11, 8, 3.6, (226, 140, 150)),
                           (9, 11, 2.4, (170, 40, 46)), (6, 17, 3.6, (120, 54, 44)),
                           (12, 17, 2.6, (210, 170, 120))):
        disc(img, cx, cy, r, col)
    for y in range(21, 30, 2):
        rect(img, 2, y, 15, y + 1, (226, 170, 150))
        rect(img, 2, y + 1, 15, y + 2, (200, 140, 124))
    rect(img, 15, 0, 17, 32, (120, 40, 36))
    return img


@C.texture("kiosk_front")
def _kiosk_front():
    """The kiosk body's front: the ticket printer's slot and the card reader."""
    img = fill((236, 238, 236), size=32, grain=1, seed=7050)
    rect(img, 6, 6, 26, 9, (40, 42, 46))
    rect(img, 8, 7, 24, 8, (250, 250, 246))
    rect(img, 11, 14, 21, 22, (60, 64, 70))
    rect(img, 12, 15, 20, 17, (20, 26, 30))
    rect(img, 12, 18, 20, 21, (120, 124, 130))
    hc.emblem(img, 16, 28, 2.6, (40, 120, 200))
    return img


def kiosk_frame(i):
    """The kiosk's screen: CHECK IN over a touch prompt, the prompt pulsing."""
    def f():
        img = fill((14, 40, 76), size=32, grain=1, seed=7051)
        rect(img, 0, 0, 32, 7, (40, 120, 200))
        draw_text_centred(img, "CHECK", 16, 1, (250, 250, 250))
        draw_text_centred(img, "IN", 16, 9, (250, 250, 250))
        on = i % 2 == 0
        rect(img, 4, 17, 28, 27, (90, 200, 120) if on else (60, 150, 90))
        draw_text_centred(img, "TOUCH", 16, 20, (14, 40, 76))
        return img
    return f


hc.animation(C, "kiosk_screen", [kiosk_frame(i) for i in range(2)], 16)


@C.texture("console_top")
def _console_top():
    """The desk's call console: a NEXT key, a recall key and a small display."""
    img = fill((52, 56, 62), grain=1, seed=7052)
    rect(img, 2, 2, 14, 6, (20, 26, 30))
    draw_text(img, "NXT", 3, 2, (250, 180, 40))
    rect(img, 2, 8, 9, 14, (40, 150, 80))
    rect(img, 11, 8, 14, 14, (200, 200, 196))
    return img


# The now-serving display's screen: its place and bands, in sixteenths (the renderer's WIDTH,
# HEIGHT, SCREEN_Z, HEADER). Every word on it is the renderer's.
NS_W, NS_H = 14.0, 9.0
NS_HEADER = 2.0
NS_SCREEN_Z = 13.7


@C.texture("now_serving_screen")
def _now_serving_screen():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 255))
    k = 128 / NS_W
    rect(img, 0, 0, 128, int(round(NS_H * k)), (8, 14, 26))
    hdr = int(round(NS_HEADER * k))
    rect(img, 0, 0, 128, hdr, (24, 84, 150))
    rect(img, 0, hdr - 1, 128, hdr, (250, 250, 250))
    return img


@C.texture("toy_box_front")
def _toy_box_front():
    """The toy box's front: painted panels with big dots."""
    img = fill((238, 232, 214), grain=2, seed=7060)
    frame(img, 0, 0, 16, 16, (196, 160, 110))
    for cx, cy, col in ((4, 5, (210, 62, 52)), (8, 10, (60, 120, 200)),
                        (12, 5, (80, 164, 84)), (4, 12, (240, 196, 50)),
                        (12, 12, (210, 62, 52))):
        disc(img, cx, cy, 1.8, col)
    return img


# ==========================================================================================
# Block helpers
# ==========================================================================================
def facing_block(reg, java, names, textures, elements, tab, extra=None, display=None):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    C.add(reg, java, names, {reg: model(textures, elements, display=display)},
          hc.facing_state(M(reg), extra), tab=tab)


java_for = hcn.java_for


def prop(reg, box6, mat, mount, collides, names, textures, elements, tab, seat=None,
         display=None, cls="BlockHealthcare"):
    facing_block(reg, java_for(cls, reg, box6, mat, mount, collides, seat),
                 names, textures, elements, tab, display=display)


def screen_prop(reg, box6, mat, mount, collides, names, textures, elements, tab, on_tex,
                display=None):
    on = {"on": {"false": {}, "true": {"textures": {"screen": on_tex}}}}
    facing_block(reg, java_for("BlockHealthcareScreen", reg, box6, mat, mount, collides),
                 names, textures, elements, tab, extra=on, display=display)


def pair_block(reg, names, whole, axis, textures, java, tab, item_display=None):
    """As the wards': first cell 0..16, second -16..0 (wide) or 16..32 (tall)."""
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


pair_java = hcn.pair_java


def run_state(parts):
    """A multipart blockstate for a run piece: ``parts`` is a list of (model name, extra
    conditions), each drawn for every facing, turned."""
    out = []
    for f, y in hc.ROT_Y.items():
        for name, when in parts:
            w = {"facing": f}
            w.update(when)
            out.append({"when": w, "apply": {"model": M(name), "y": y}})
    return {"multipart": out}


def run_block(reg, java, names, textures, body, end, tab, display=ITEM_SMALL):
    """A run piece: the body in every cell, the east end (model +x, the viewer's left) where
    the run stops on that side, and the mirrored west end."""
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    models = {reg + "_body": model(textures, body),
              reg + "_end_east": model(textures, end),
              reg + "_end_west": model(textures, mirror_x(end)),
              reg + "_item": model(textures, body + end + mirror_x(end), display=display)}
    C.add(reg, java, names, models,
          run_state([(reg + "_body", {}), (reg + "_end_east", {"east": "false"}),
                     (reg + "_end_west", {"west": "false"})]),
          item={"parent": MI(reg + "_item")}, tab=tab)


def run_java(reg, box6, mat, collides, group, seat=None):
    java = 'new BlockHealthcareRun("%s", %s, HealthcareMaterial.%s, %s, "%s")' % (
        reg, B(*box6), mat, "true" if collides else "false", group)
    if seat:
        java += ".withSeat(%g, %g)" % seat
    return java


# ==========================================================================================
# Maternity
# ==========================================================================================
SPOKES = casters([(3, 3.5), (13, 3.5), (3, 13), (13, 13)])
CART_BASE = SPOKES + [el([2.5, 0.8, 3], [13.5, 1.6, 13.5], "grey")]


def clear_tub(x0, x1, z0, z1, y0, y1, key="acrylic"):
    """An open clear box: four panels and a bottom, each panel a face both ways."""
    return [el([x0, y0, z0], [x1, y1, z0 + 0.3], key, faces=("north", "south", "up")),
            el([x0, y0, z1 - 0.3], [x1, y1, z1], key, faces=("north", "south", "up")),
            el([x0, y0, z0 + 0.3], [x0 + 0.3, y1, z1 - 0.3], key, faces=("east", "west", "up")),
            el([x1 - 0.3, y0, z0 + 0.3], [x1, y1, z1 - 0.3], key, faces=("east", "west", "up"))]


def baby(cx, cz, y, along_z=True):
    """A swaddled newborn: a bundle in the striped blanket and a little head, lying along z."""
    if along_z:
        return [el([cx - 1.4, y, cz - 2.6], [cx + 1.4, y + 1.8, cz + 1.4], "swaddle"),
                el([cx - 0.9, y + 0.2, cz + 1.4], [cx + 0.9, y + 1.8, cz + 3], "manikin")]
    return [el([cx - 2.6, y, cz - 1.4], [cx + 1.4, y + 1.8, cz + 1.4], "swaddle"),
            el([cx + 1.4, y + 0.2, cz - 0.9], [cx + 3, y + 1.8, cz + 0.9], "manikin")]


# the radiant warmer: a cabinet on casters, the open bed on top with clear drop sides, a
# column at the back carrying the controller and, overhead, the heater head facing down
WARMER = (CART_BASE
          + [el([3, 1.6, 3.5], [13, 12.5, 13], "body", per={"north": "drawers"},
                uv={"north": FULL}),
             el([2, 12.5, 2], [14, 13.5, 13.5], "grey"),
             el([2.6, 13.5, 2.6], [13.4, 14.5, 12.9], "pad")]
          + clear_tub(2, 14, 2, 13.5, 13.5, 17)
          + baby(8, 7.5, 14.5)
          + [el([7, 1.6, 13], [9, 30, 15], "body"),
             front([5.5, 18, 12.6], [10.5, 23, 13.2], "body", "panel"),
             el([7, 29, 6], [9, 30.5, 15], "body"),
             el([3.5, 27.5, 3], [12.5, 30, 9], "head", per={"down": "lens"},
                uv={"down": FULL})])
C.add("infant_warmer",
      'new BlockHealthcareLight("infant_warmer", %s, 9)' % B(2, 0, 2, 14, 16, 15),
      ("Infant Radiant Warmer", "Säuglings-Wärmebett", "Cuna térmica radiante",
       "Värmebädd för nyfödda"),
      {"infant_warmer": model(
          {"body": W("warm_white"), "drawers": K("drawer_bank"), "grey": GREY,
           "rubber": RUBBER, "pad": W("mattress"), "acrylic": T("acrylic"),
           "swaddle": T("swaddle"), "manikin": T("manikin"), "panel": T("warmer_panel"),
           "head": T("warmer_head"), "lens": T("heater_off"), "particle": W("warm_white")},
          WARMER, display=ITEM_TALL)},
      hw.light_state(M("infant_warmer"), {"lens": T("heater_on")}), tab=PC)

# the bassinet: a clear tub on a steel cart with a drawer under it, a newborn in it
BASSINET = (CART_BASE
            + [el([3.5, 1.6, 4], [4.5, 11, 5], "metal"), el([11.5, 1.6, 4], [12.5, 11, 5], "metal"),
               el([3.5, 1.6, 11.5], [4.5, 11, 12.5], "metal"),
               el([11.5, 1.6, 11.5], [12.5, 11, 12.5], "metal"),
               el([3.5, 5, 4], [12.5, 9, 12.5], "body"),
               el([3, 11, 3.5], [13, 11.6, 13], "metal"),
               el([3.5, 11.6, 4], [12.5, 12.2, 12.5], "pad")]
            + clear_tub(3, 13, 3.5, 13, 11.6, 16)
            + baby(8, 7.5, 12.2))
prop("bassinet", (3, 0, 3, 13, 16, 13), "METAL", "FLOOR", True,
     ("Hospital Bassinet", "Neugeborenenbett", "Cuna de hospital", "Sjukhusbabysäng"),
     {"metal": STEEL, "grey": GREY, "rubber": RUBBER, "body": W("warm_white"),
      "pad": W("mattress"), "acrylic": T("acrylic"), "swaddle": T("swaddle"),
      "manikin": T("manikin"), "particle": STEEL},
     BASSINET, PC)

# the incubator: a cabinet on casters, the clear hood with its hand ports, a monitor on a post
INCUBATOR = (CART_BASE
             + [el([2.5, 1.6, 3.5], [13.5, 11, 13], "body"),
                el([2, 11, 2.5], [14, 12, 13.5], "grey"),
                el([2.6, 12, 3.1], [13.4, 12.8, 12.9], "pad"),
                el([2.2, 12, 2.7], [13.8, 19, 3], "hood_front", faces=("north", "south"),
                   uv={"north": FULL, "south": FULL}),
                el([2.2, 12, 13], [13.8, 19, 13.3], "acrylic", faces=("north", "south")),
                el([2.2, 12, 3], [2.5, 19, 13], "acrylic", faces=("east", "west")),
                el([13.5, 12, 3], [13.8, 19, 13], "acrylic", faces=("east", "west")),
                el([2.2, 19, 2.7], [13.8, 19.3, 13.3], "acrylic", faces=("up", "down"))]
             + baby(8, 7.5, 12.8, along_z=False)
             + [el([14, 1.6, 11], [15, 24, 12], "metal"),
                el([12.8, 21, 9.6], [15.6, 25.4, 11], "mbody"),
                screen([13, 21.4, 9.55], [15.4, 25, 9.55], "screen")])
screen_prop("nicu_incubator", (2, 0, 2, 16, 16, 14), "METAL", "FLOOR", True,
            ("NICU Incubator", "Inkubator (Neonatologie)", "Incubadora neonatal",
             "Kuvös (neonatal)"),
            {"metal": STEEL, "grey": GREY, "rubber": RUBBER, "body": W("warm_white"),
             "pad": W("mattress"), "acrylic": T("acrylic"), "hood_front": T("incubator_hood"),
             "swaddle": T("swaddle"), "manikin": T("manikin"), "mbody": W("monitor_body"),
             "screen": W("screen_off"), "particle": W("warm_white")},
            INCUBATOR, PC, W("monitor_screen"), display=ITEM_TALL)

# ==========================================================================================
# Rehabilitation
# ==========================================================================================
# the parallel bars: a wooden walkway between two round rails at hip height, an upright under
# each rail every block; at a run's end the walkway's edge is bevelled down to the floor
BARS_Y = 15
BARS_BODY = ([el([0, 0, 1], [16, 1, 15], "wood")]
             + hc.pipe_x(BARS_Y, 2, 0.7, 0, 16, "metal", ends=False)
             + hc.pipe_x(BARS_Y, 14, 0.7, 0, 16, "metal", ends=False)
             + octo(8, 2, 0.6, 1, BARS_Y, "metal", top=False)
             + octo(8, 14, 0.6, 1, BARS_Y, "metal", top=False)
             + [el([6.5, 1, 0.5], [9.5, 1.6, 3.5], "grey"),
                el([6.5, 1, 12.5], [9.5, 1.6, 15.5], "grey")])
BARS_END = ([el([15, 0, 1], [16, 0.5, 15], "wood", faces=("east", "up", "north", "south")),
             el([15, 0.5, 1], [15.2, 1, 15], "wood", faces=("east",))]
            + octo(15.3, 2, 0.6, 1, BARS_Y + 0.7, "metal")
            + octo(15.3, 14, 0.6, 1, BARS_Y + 0.7, "metal"))
run_block("parallel_bars", run_java("parallel_bars", (0, 0, 0, 16, 16, 16), "WOOD", False,
                                    "parallel_bars"),
          ("Parallel Bars", "Gehbarren", "Barras paralelas", "Gåbarr"),
          {"wood": BEECH, "metal": STEEL, "grey": GREY}, BARS_BODY, BARS_END, PC)

# the treatment plinth: two cells long, a padded top on a steel frame with a scissor lift, a
# pillow at the head end (the first cell)
PLINTH = ([el([-14, 0, 3], [-12, 1, 13], "grey"), el([12, 0, 3], [14, 1, 13], "grey"),
           el([-12, 0.6, 7], [12, 1.2, 9], "grey")]
          + [rot(el([-9, 1, 7.4], [9, 2, 8.6], "metal"), "z", a, [0, 4, 8]) for a in (22.5, -22.5)]
          + [el([-15, 6, 2.5], [15, 7, 13.5], "grey"),
             el([-15.5, 7, 2], [15.5, 9.5, 14], "pad"),
             el([9, 9.5, 4], [14.5, 10.8, 12], "pillow")])
pair_block("treatment_plinth",
           ("Treatment Plinth", "Therapieliege", "Camilla de tratamiento", "Behandlingsbänk"),
           PLINTH, 0,
           {"grey": GREY, "metal": STEEL, "pad": W("vinyl_blue"), "pillow": W("pillow"),
            "particle": W("vinyl_blue")},
           pair_java("treatment_plinth", (0, 0, 2, 16, 10, 14), "CLOTH", "FLOOR", "WIDE")
           + ".withSeat(9.5, 0)", PC)

# the training stairs: three steps up from each end to a platform, a handrail each side
# (BlockExerciseStairs' collision boxes are these steps)
STEPS = [(-15, -11, 4), (-11, -7, 8), (-7, 7, 12), (7, 11, 8), (11, 15, 4)]
STAIRS = [el([x0, 0, 2], [x1, h, 14], "wood", per={"up": "tread"}) for x0, x1, h in STEPS]
for _z in (1.2, 14.8):
    for _x in (-15, -7, 7, 15):
        _h = 4 if abs(_x) == 15 else 12
        STAIRS += octo(_x, _z, 0.5, _h, _h + 14, "metal", bottom=False)
        STAIRS += octo(_x, _z, 0.5, 0, _h, "metal", top=False)
    STAIRS += hc.pipe_x(26, _z, 0.55, -7, 7, "metal")
    for _sign in (-1, 1):
        # the sloped rail over each flight: 8 along, 8 up, so 45 degrees
        _cx = _sign * 11
        STAIRS.append(rot(el([_cx - 5.66, 21.45, _z - 0.5], [_cx + 5.66, 22.55, _z + 0.5],
                             "metal"), "z", 45 * _sign * -1, [_cx, 22, _z]))
pair_block("exercise_stairs",
           ("Training Stairs (Rehabilitation)", "Therapietreppe", "Escalera de rehabilitación",
            "Träningstrappa (rehabilitering)"),
           STAIRS, 0,
           {"wood": BEECH, "tread": T("carpet"), "metal": STEEL, "particle": BEECH},
           "new BlockExerciseStairs()", PC)

# ==========================================================================================
# The waiting room
# ==========================================================================================
# beam seating: a steel beam, a seat and back each block on a bracket, an arm on the block's
# west edge (so one between every two seats); legs and the last arm where the row ends
CHAIR_BODY = [el([0, 4, 7], [16, 5.4, 9], "metal", faces=("north", "south", "up", "down")),
              el([7, 5.4, 6], [9, 6.2, 10], "grey"),
              el([1, 6.2, 3], [15, 7.6, 11.5], "seat"),
              el([7.2, 6.2, 11], [8.8, 10, 12.5], "grey"),
              rot(el([1, 8.2, 11.4], [15, 15.5, 12.8], "seat"), "x", -22.5, [8, 8.2, 12.1]),
              el([0, 5.4, 6.8], [1, 6.4, 9.2], "grey"),
              el([0.1, 6.4, 7.5], [0.9, 10.5, 8.5], "grey"),
              el([0, 10.5, 3.5], [1, 11.3, 11], "arm")]
CHAIR_END = [el([15.99, 4, 7], [16, 5.4, 9], "metal", faces=("east",)),
             el([14.2, 0, 3], [15.4, 0.8, 13], "grey"),
             el([14.3, 0.8, 7.4], [15.3, 4, 8.6], "grey"),
             el([15, 5.4, 6.8], [16, 6.4, 9.2], "grey"),
             el([15.1, 6.4, 7.5], [15.9, 10.5, 8.5], "grey"),
             el([15, 10.5, 3.5], [16, 11.3, 11], "arm")]
for _cid, _tex, _names in (
        ("blue", W("vinyl_blue"), ("Blue", "Blau", "azul", "blå")),
        ("teal", W("vinyl_teal"), ("Teal", "Petrol", "verde azulado", "petrol")),
        ("charcoal", W("charcoal"), ("Charcoal", "Anthrazit", "antracita", "antracit"))):
    _reg = "waiting_beam_chair_" + _cid
    run_block(_reg, run_java(_reg, (0, 0, 3, 16, 15, 13), "METAL", True, "waiting_beam_chair",
                             (7.6, 0)),
              tuple("%s (%s)" % (n, c) for n, c in zip(
                  ("Waiting Room Beam Seating", "Wartezimmer-Traversenbank",
                   "Bancada de sala de espera", "Väntrumsbänk på balk"), _names)),
              {"metal": STEEL, "grey": W("dark_grey"), "seat": _tex, "arm": W("charcoal")},
              CHAIR_BODY, CHAIR_END, PC)

# the check-in kiosk: a pedestal on a base plate, the body with the ticket printer and card
# reader, the touch screen tilted back on top
KIOSK = [el([3, 0, 4], [13, 0.8, 13], "grey"),
         el([6, 0.8, 8], [10, 8, 11], "body"),
         front([4, 8, 6], [12, 19, 12], "body", "front"),
         rot(el([3.5, 19, 5], [12.5, 26, 7], "body", per={"north": "bezel"}), "x", -22.5,
             [8, 19, 7]),
         rot(screen([4.2, 19.6, 4.95], [11.8, 25.4, 4.95], "screen"), "x", -22.5, [8, 19, 7])]
facing_block("check_in_kiosk",
             'new BlockQueueController("check_in_kiosk", %s, true)' % B(3, 0, 4, 13, 16, 13),
             ("Check-In Kiosk", "Check-in-Terminal", "Quiosco de registro",
              "Incheckningsautomat"),
             {"body": W("warm_white"), "front": T("kiosk_front"), "grey": GREY,
              "bezel": W("charcoal"), "screen": T("kiosk_screen"), "particle": W("warm_white")},
             KIOSK, PC, display=ITEM_TALL)

# the desk's call console: a small wedge on the counter with a NEXT key and a display
CONSOLE = [el([4, 0, 6], [12, 1.2, 12], "body"),
           rot(el([4, 1.2, 6], [12, 2.2, 12], "body", per={"up": "keys"}, uv={"up": FULL}),
               "x", -22.5, [8, 1.2, 12])]
facing_block("queue_desk_console",
             'new BlockQueueController("queue_desk_console", %s, false)'
             % B(4, 0, 5, 12, 4, 12),
             ("Queue Call Console (Desk)", "Aufrufkonsole (Schalter)",
              "Consola de llamada de turnos (mostrador)", "Kösystemets anropspanel (disk)"),
             {"body": W("charcoal"), "keys": T("console_top"), "particle": W("charcoal")},
             CONSOLE, PC)

# the now-serving display: a wall screen on a bracket; its words are the renderer's
C.add("now_serving_display", "new BlockNowServingDisplay()",
      ("Now Serving Display", "Aufrufanzeige", "Pantalla de turnos", "Kölappsskärm"),
      {"now_serving_display": model(
          {"body": W("tv_body"), "screen": T("now_serving_screen"), "grey": GREY,
           "particle": W("tv_body")},
          [el([6, 6, 15.5], [10, 10, 16], "grey"),
           el([0.5, 3, 14], [15.5, 13, 15.5], "body"),
           # 0.3 px proud of the bezel, clear of model_depth's separating; the renderer draws its
           # lit copy just in front (SCREEN_Z)
           el([1, 3.5, NS_SCREEN_Z], [15, 12.5, 14], "screen", faces=("north",),
              uv={"north": [0, 0, 16, round(16 * NS_H / NS_W, 3)]})])},
      hc.facing_state(M("now_serving_display")), tab=PC)

C.add_lang("csm.healthcare.queue.serving",
           ("[Queue] Now serving %s.", "[Aufruf] Jetzt an der Reihe: %s.",
            "[Turno] Atendiendo al %s.", "[Kö] Nu betjänas %s."))

# ==========================================================================================
# The paediatric playroom
# ==========================================================================================
KID_TABLE = ([el([1, 7, 1], [15, 8, 15], "top", per={"down": "wood"}),
              el([1.5, 6, 1.5], [14.5, 7, 14.5], "wood")]
             + [el([x, 0, z], [x + 1.6, 6, z + 1.6], "wood") for x in (2, 12.4) for z in (2, 12.4)])
prop("kids_play_table", (1, 0, 1, 15, 8, 15), "WOOD", "FLOOR", True,
     ("Kids' Play Table", "Kinderspieltisch", "Mesa de juego infantil", "Barnens lekbord"),
     {"top": T("kid_yellow"), "wood": BEECH, "particle": BEECH}, KID_TABLE, PC)

KID_CHAIR = ([el([x, 0, z], [x + 1.2, 4.5, z + 1.2], "wood") for x in (4, 10.8) for z in (4, 10.8)]
             + [el([4, 4.5, 4], [12, 5.5, 12], "seat"),
                el([4, 5.5, 10.8], [5.2, 11, 12], "wood"),
                el([10.8, 5.5, 10.8], [12, 11, 12], "wood"),
                el([4, 8, 11], [12, 11, 12], "seat")])
for _cid, _names in (("red", ("Red", "Rot", "rojo", "röd")),
                     ("blue", ("Blue", "Blau", "azul", "blå")),
                     ("green", ("Green", "Grün", "verde", "grön"))):
    prop("kids_chair_" + _cid, (4, 0, 4, 12, 11, 12), "WOOD", "FLOOR", True,
         tuple("%s (%s)" % (n, c) for n, c in zip(
             ("Kids' Chair", "Kinderstuhl", "Silla infantil", "Barnstol"), _names)),
         {"wood": BEECH, "seat": T("kid_" + _cid), "particle": T("kid_" + _cid)},
         KID_CHAIR, PC, seat=(5.5, 0))

TOY_BOX = ([front([2, 0, 4], [14, 7, 13], "wood", "front"),
            rot(el([2, 7, 12.6], [14, 7.8, 21.5], "lid"), "x", -45, [8, 7.4, 13]),
            el([2.6, 6.4, 4.6], [13.4, 6.5, 12.4], "inside", faces=("up",)),
            el([4, 6.5, 6], [6.5, 9, 8.5], "red"),
            el([9.5, 6.5, 8], [12, 8.5, 10.5], "blue")]
           + octo(7.5, 10, 1.6, 6.5, 9.7, "green"))
prop("playroom_toy_box", (2, 0, 4, 14, 10, 13), "WOOD", "FLOOR", True,
     ("Playroom Toy Box", "Spielzimmer-Spielzeugkiste", "Baúl de juguetes de la sala de juegos",
      "Leksaksbox (lekrum)"),
     {"wood": BEECH, "front": T("toy_box_front"), "lid": T("kid_red"), "inside": BEECH,
      "red": T("kid_red"), "blue": T("kid_blue"), "green": T("kid_green"), "particle": BEECH},
     TOY_BOX, PC)

# ==========================================================================================
# Teaching
# ==========================================================================================
def mannequin_body(y0):
    """The manikin lying on its back along z, its feet towards -z (the way the block faces),
    its middle on z = 0, its back on y0."""
    half = [el([4.4, y0, 1.6], [8, y0 + 3, 9.6], "gown"),            # chest and belly
            el([3.0, y0 + 0.2, 1.4], [4.4, y0 + 1.8, 9.2], "gown"),   # upper arm, sleeve
            el([3.1, y0 + 0.2, -2.6], [4.3, y0 + 1.6, 1.4], "skin"),  # forearm and hand
            el([5.0, y0, -4], [8, y0 + 2.6, 1.6], "gown"),            # the gown over the hips
            el([5.2, y0, -13], [7.6, y0 + 2.2, -4], "skin"),          # leg
            el([5.4, y0 + 1.4, -14.2], [7.4, y0 + 3.8, -13], "skin")]  # foot
    body = half + mirror_x(half)
    body += [el([6.4, y0, 10.4], [9.6, y0 + 3.4, 13.8], "skin"),     # head
             el([7.2, y0 + 0.6, 9.6], [8.8, y0 + 2.2, 10.4], "skin")]  # neck
    return body


def mannequin(surface, hinge=None, pivot_drop=0.0, dz=0.0):
    """The manikin on a surface ``surface`` sixteenths up the cell below (0: its own floor),
    its back raised 22.5 degrees from z = ``hinge`` about a line ``pivot_drop`` under the
    surface, the whole moved ``dz`` along z."""
    y0 = surface - 16 if surface else 0
    body = mannequin_body(y0)
    if hinge is not None:
        lower = cut(body, 2, -32, hinge, origin=0)
        upper = cut(body, 2, hinge, 32, origin=0)
        body = lower + [rot(e, "x", -22.5, [8, y0 - pivot_drop, hinge]) for e in upper]
    return shift(body, dz=dz) if dz else body


BED_HINGE = 4.5        # the hospital bed's back section hinges 4.5 px into its head cell
BED_DROP = 3.6         # its pivot (the deck) is this far under the blanket
EXAM_HINGE = 7         # the exam table's head hinges 7 px into its first cell
EXAM_DROP = 2.05
MANNEQUIN_RESTS = {
    "flat": mannequin(0),
    "bed_low": mannequin(9.6, BED_HINGE, BED_DROP),
    "bed_high": mannequin(13.6, BED_HINGE, BED_DROP),
    "bed_low_foot": mannequin(9.6, BED_HINGE + 16, BED_DROP, 16),
    "bed_high_foot": mannequin(13.6, BED_HINGE + 16, BED_DROP, 16),
    "exam": mannequin(11.25, EXAM_HINGE, EXAM_DROP),
    "exam_flat": mannequin(11.25),
    "table": mannequin(10.5),
}
MAN_TEX = {"skin": T("manikin"), "gown": T("gown"), "particle": T("gown")}
_man_models = {"simulation_mannequin_" + k: model(MAN_TEX, v, ao=False)
               for k, v in MANNEQUIN_RESTS.items()}
_man_models["simulation_mannequin_item"] = model(MAN_TEX, shift(MANNEQUIN_RESTS["flat"],
                                                                dz=1), display=ITEM_SMALL)
_man_state = hc.facing_state(M("simulation_mannequin_flat"), {
    "rest": {k: {"model": M("simulation_mannequin_" + k)} for k in MANNEQUIN_RESTS}})
_man_state["variants"]["inventory"] = [{"model": M("simulation_mannequin_item")}]
C.add("simulation_mannequin", "new BlockSimulationMannequin()",
      ("Patient Simulation Mannequin", "Patientensimulator-Puppe", "Maniquí de simulación",
       "Patientsimulator (docka)"),
      _man_models, _man_state, tab=CL)

# the anatomical torso: a bust on a round base, painted half skin and half dissected
TORSO = ([el([5, 0, 5], [11, 1, 11], "base")]
         + octo(8, 8, 0.8, 1, 2.5, "base")
         + [front([5.2, 2.5, 6.6], [10.8, 11.5, 9.6], "skin", "front"),
            el([4.4, 9, 6.9], [11.6, 11, 9.3], "skin"),
            el([7.2, 11.5, 7.2], [8.8, 12.5, 8.8], "skin"),
            el([6.6, 12.5, 6.6], [9.4, 15.5, 9.4], "skin", per={"north": "face"},
               uv={"north": [8, 0, 16, 8]})])
prop("anatomical_torso_model", (4, 0, 5, 12, 16, 11), "PLASTIC", "FLOOR", False,
     ("Anatomical Torso Model", "Anatomisches Torsomodell", "Torso anatómico",
      "Anatomisk torsomodell"),
     {"base": W("charcoal"), "skin": T("manikin"), "front": T("anatomy_front"),
      "face": T("anatomy_front"), "particle": T("manikin")},
     TORSO, CL)

# the skeleton on its stand: a base with casters, a post to a hook at the skull, the skeleton
# hanging from it -- bones as thin boxes, the ribcage a stack of hoops
SKEL = (casters([(3.5, 11), (12.5, 11), (8, 14.5)])
        + [el([3, 0.8, 10.5], [13, 1.6, 14], "grey")]
        + octo(8, 13, 0.6, 1.6, 31, "metal")
        + [el([7.4, 30.4, 9.2], [8.6, 31, 13], "metal")])
_b = []
_b += [el([6.6, 26.6, 7.2], [9.4, 29.8, 10], "bone"),        # skull
       el([7.2, 25.4, 6.8], [8.8, 26.6, 8.8], "bone"),        # jaw
       el([7.7, 13, 8.8], [8.3, 26.6, 9.4], "bone")]          # spine
for _k in range(6):
    _y = 18 + _k * 1.2
    _w = 2.4 + min(_k, 3) * 0.3
    _b += [el([8 - _w, _y, 7], [8 + _w, _y + 0.5, 7.4], "bone"),
           el([8 - _w, _y, 9.4], [8 + _w, _y + 0.5, 9.8], "bone"),
           el([8 - _w, _y, 7.4], [8 - _w + 0.4, _y + 0.5, 9.4], "bone"),
           el([8 + _w - 0.4, _y, 7.4], [8 + _w, _y + 0.5, 9.4], "bone")]
_b += [el([4.4, 24.6, 8], [11.6, 25.2, 9], "bone"),          # collar bones
       el([5.4, 12, 7.6], [10.6, 14, 9.6], "bone")]          # pelvis
_arm = [el([4, 17.6, 8.1], [4.6, 24.6, 8.7], "bone"), el([4, 11.4, 8.1], [4.6, 17.6, 8.7], "bone"),
        el([3.8, 9.6, 7.9], [4.8, 11.4, 8.9], "bone")]
_leg = [el([6.2, 6.4, 8.3], [6.9, 12, 9], "bone"), el([6.2, 1.8, 8.3], [6.9, 6.4, 9], "bone"),
        el([5.9, 1.6, 7], [7, 2.1, 9], "bone")]
_b += _arm + mirror_x(_arm) + _leg + mirror_x(_leg)
SKEL += _b
pair_block("skeleton_stand",
           ("Skeleton on Stand", "Skelett auf Ständer", "Esqueleto con soporte",
            "Skelett på stativ"),
           SKEL, 1,
           {"grey": GREY, "metal": STEEL, "rubber": RUBBER, "bone": T("bone"),
            "particle": T("bone")},
           pair_java("skeleton_stand", (3, 0, 6, 13, 16, 15), "PLASTIC", "FLOOR", "TALL"),
           CL, item_display=ITEM_TALL)

# the dissection table: stainless, two cells long, its top at the autopsy table's height (the
# mannequin's TABLE rest), the downdraft lid in two halves standing open along the back
DISSECT = ([el([-12, 0, 4], [-9, 1, 12], "metal"), el([9, 0, 4], [12, 1, 12], "metal"),
            el([-11.5, 1, 6], [-9.5, 9, 10], "metal"), el([9.5, 1, 6], [11.5, 9, 10], "metal"),
            el([-15, 9, 2.5], [15, 10.5, 13.5], "metal", per={"up": "top"}, uv={"up": FULL}),
            el([-15, 10.5, 2.5], [15, 11.2, 3.2], "metal")]
           + [rot(el([x0, 10.5, 13], [x1, 11, 20], "metal"), "x", -45, [0, 10.75, 13.5])
              for x0, x1 in ((-15, -0.2), (0.2, 15))]
           + [el([-15, 10.5, 13], [15, 11.2, 13.8], "metal")])
pair_block("dissection_table",
           ("Dissection Table", "Präpariertisch", "Mesa de disección", "Dissektionsbord"),
           DISSECT, 0,
           {"metal": STEEL, "top": D("autopsy_top"), "particle": STEEL},
           pair_java("dissection_table", (0, 0, 2, 16, 12, 14), "METAL", "FLOOR", "WIDE"), CL)

# tiered lecture hall seating: the seat and back at the back of the cell, a standard on the
# cell's west edge with an arm, the tablet over the lap on the east arm; raised, the row stands
# on a carpeted half-block riser with a nosing on its front edge
def lecture_body(lift):
    y = lift
    els = [el([1, y + 6.2, 6], [15, y + 7.6, 12.5], "seat"),
           el([7.3, y, 8.5], [8.7, y + 6.2, 10], "metal"),
           el([5, y, 7.5], [11, y + 0.6, 11], "metal"),
           rot(el([1, y + 7.8, 12.4], [15, y + 15.5, 13.6], "seat"), "x", -22.5,
               [8, y + 7.8, 13]),
           el([0, y, 6], [0.8, y + 11, 15], "wood"),
           el([0, y + 11, 5], [0.8, y + 11.6, 13], "wood"),
           el([8.5, y + 11.6, 1.5], [16, y + 12.1, 6], "wood"),
           el([15, y + 9.5, 5], [15.6, y + 11.6, 6], "metal")]
    if lift:
        # the riser's sides are the ends' (drawn only where the row stops)
        els += [el([0, 0, 0], [16, lift, 16], "riser", faces=("north", "south", "up"),
                   per={"up": "carpet"}),
                el([0, lift - 0.4, -0.3], [16, lift + 0.1, 0.6], "nosing",
                   faces=("north", "up", "down"))]
    return els


def lecture_end(lift):
    y = lift
    els = [el([15.2, y, 6], [16, y + 11, 15], "wood"),
           el([15.2, y + 11, 5], [16, y + 11.6, 13], "wood")]
    if lift:
        els += [el([15.99, 0, 0], [16, lift, 16], "riser", faces=("east",)),
                el([15.99, lift - 0.4, -0.3], [16, lift + 0.1, 0.6], "nosing", faces=("east",))]
    return els


LECT_TEX = {"seat": T("seat_blue"), "metal": W("dark_grey"), "wood": BEECH,
            "riser": W("charcoal"), "carpet": T("carpet"), "nosing": STEEL, "particle": BEECH}
_lect_models = {}
_lect_parts = []
for _raised, _lift in (("false", 0), ("true", 8)):
    _suf = "_raised" if _lift else ""
    _lect_models["lecture_hall_seat_body" + _suf] = model(LECT_TEX, lecture_body(_lift))
    _lect_models["lecture_hall_seat_end_east" + _suf] = model(LECT_TEX, lecture_end(_lift))
    _lect_models["lecture_hall_seat_end_west" + _suf] = model(LECT_TEX,
                                                              mirror_x(lecture_end(_lift)))
    _lect_parts += [("lecture_hall_seat_body" + _suf, {"raised": _raised}),
                    ("lecture_hall_seat_end_east" + _suf, {"raised": _raised, "east": "false"}),
                    ("lecture_hall_seat_end_west" + _suf, {"raised": _raised, "west": "false"})]
_lect_models["lecture_hall_seat_item"] = model(
    LECT_TEX, lecture_body(0) + lecture_end(0) + mirror_x(lecture_end(0)), display=ITEM_SMALL)
C.add("lecture_hall_seat", "new BlockLectureSeating()",
      ("Lecture Hall Seat (Tiered)", "Hörsaalbestuhlung (ansteigend)",
       "Butaca de aula magna (gradas)", "Hörsalsstol (amfiteater)"),
      _lect_models, run_state(_lect_parts),
      item={"parent": MI("lecture_hall_seat_item")}, tab=CL)


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
