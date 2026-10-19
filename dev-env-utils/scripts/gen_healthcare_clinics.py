#!/usr/bin/env python3
"""
gen_healthcare_clinics.py -- the Healthcare module's clinic, dialysis, infusion and endoscopy
pieces.

The exam room (an exam table with its paper roll, a rolling exam light, the wall diagnostic set,
a column patient scale with its height rod, the phlebotomy chair and a step stool), the dialysis
unit (the treatment chair, the dialysis machine with its moving display, and the reverse osmosis
water unit for the plant room: membrane vessels on a frame with a carbon tank and a booster
pump, which is not Utilities' municipal chemical feed skid), the infusion recliner with its IV
pump on a pole beside it, the dental chair (its operatory light on a post and its delivery
unit's tray), the eye exam lane (the refraction chair with its phoropter on an arm, and the
acuity chart), and the endoscopy tower (a cart of video processor, light source and
insufflator under its monitor, the scope hung on its side).

Nothing here is a real product: no maker's name, no red cross (a protected emblem). Screens are
animated textures (.mcmeta), so nothing ticks and nothing has a renderer.

Swatches the wards and the clinical generator already draw (stainless, the frame greys, vinyl,
rubber, the monitor body, the screens, the exam light's lenses) are shared by path, not drawn
again.

Writes, under modules/healthcare/src/main/resources/assets/csm:

  * textures/blocks/healthcare/clinics/*.png (+ .mcmeta for the screens)
  * models/block/healthcare/clinics/*.json
  * blockstates/<registry>.json
  * the tile names in all four lang files

Every model faces north with what it stands against at +Z.

Usage:
    python gen_healthcare_clinics.py              # write everything
    python gen_healthcare_clinics.py --check      # fail if the tree has drifted
    python gen_healthcare_clinics.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_healthcare_common as hc  # noqa: E402
import gen_healthcare_wards as hw  # noqa: E402
import gen_healthcare_clinical as hcl  # noqa: E402
from gen_healthcare_common import (  # noqa: E402
    disc, draw_text, draw_text_centred, fill, frame, rect, shade)

C = hc.catalogue("gen_healthcare_clinics.py", "clinics")
T = C.T
M = C.M
W = hw.C.T    # the wards' shared swatches
K = hcl.C.T   # the clinical generator's

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

PC = hc.PATIENT_CARE
CL = hc.CLINICAL

STEEL = W("stainless")
GREY = W("frame_grey")
RUBBER = W("rubber")


def front(frm, to, base, key, side="north", faces=hw.ALL):
    """A box whose front (north, or ``side``) shows the whole of a texture."""
    return el(frm, to, base, faces=faces, per={side: key}, uv={side: FULL})


def tex(name, fn):
    C.textures[name] = fn


# ==========================================================================================
# Textures
# ==========================================================================================
for i, (n, c, g) in enumerate((
        ("exam_vinyl", (120, 92, 128), 3), ("dialysis_vinyl", (64, 108, 146), 3),
        ("infusion_vinyl", (86, 128, 104), 3), ("cabinet", (226, 222, 212), 2),
        ("membrane", (238, 238, 232), 2), ("carbon_tank", (40, 84, 150), 3),
        ("pump_blue", (52, 96, 170), 3), ("acid_jug", (236, 236, 228), 2),
        ("bicarb_jug", (90, 140, 210), 3), ("scope_black", (30, 30, 34), 2),
        ("blood_line", (168, 28, 34), 2), ("tread", (60, 62, 66), 2),
        ("cord_black", (24, 24, 26), 1))):
    tex(n, (lambda c=c, g=g, s=4000 + i: hc.swatch(c, s, grain=g)))


@C.texture("exam_paper")
def _exam_paper():
    """Exam table paper: white with faint creases across it."""
    img = fill((246, 246, 242), grain=1, seed=4101)
    for y in (3, 9, 14):
        rect(img, 0, y, 16, y + 1, (232, 232, 228))
    return img


@C.texture("paper_roll_end")
def _paper_roll_end():
    img = fill((244, 244, 240), grain=1, seed=4102)
    for r in (6.5, 4.5, 2.5):
        px = img.load()
        for y in range(16):
            for x in range(16):
                if abs(math.hypot(x + 0.5 - 8, y + 0.5 - 8) - r) < 0.45:
                    px[x, y] = (214, 214, 208, 255)
    disc(img, 8, 8, 1.6, (150, 120, 90))
    return img


@C.texture("exam_cabinet_front")
def _exam_cabinet_front():
    """The exam table's base: two drawer stacks, a cupboard and the pull-out step's face."""
    img = fill((226, 222, 212), size=32, grain=1, seed=4103)
    edge = (196, 192, 184)
    for x0, x1 in ((1, 10), (22, 31)):
        for k in range(3):
            y0 = 2 + k * 8
            rect(img, x0, y0, x1, y0 + 7, (234, 230, 222))
            frame(img, x0, y0, x1, y0 + 7, edge)
            rect(img, (x0 + x1) // 2 - 2, y0 + 3, (x0 + x1) // 2 + 2, y0 + 4, (150, 154, 160))
    rect(img, 11, 2, 21, 25, (234, 230, 222))
    frame(img, 11, 2, 21, 25, edge)
    rect(img, 19, 10, 20, 16, (150, 154, 160))
    rect(img, 0, 27, 32, 32, (70, 66, 62))
    return img


@C.texture("diag_panel")
def _diag_panel():
    """The wall diagnostic set's base plate: two handle cradles, the cuff's gauge and a specula
    dispenser, on a white transformer plate."""
    img = fill((236, 236, 232), size=32, grain=1, seed=4104)
    frame(img, 0, 0, 32, 32, (200, 200, 196))
    for x in (4, 11):
        rect(img, x, 6, x + 4, 15, (210, 210, 206))
        rect(img, x + 1, 7, x + 3, 9, (60, 64, 70))
    rect(img, 18, 20, 29, 29, (200, 204, 210))
    frame(img, 18, 20, 29, 29, (150, 154, 160))
    for k in range(4):
        rect(img, 19 + k * 2 + (1 if k > 1 else 0), 22, 20 + k * 2 + (1 if k > 1 else 0), 27,
             (30, 30, 34))
    rect(img, 3, 20, 15, 27, (60, 64, 70))
    rect(img, 4, 21, 9, 23, (90, 200, 120))
    return img


@C.texture("bp_gauge")
def _bp_gauge():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    disc(img, 8, 8, 7.5, (60, 64, 70))
    disc(img, 8, 8, 6.4, (246, 246, 240))
    for k in range(12):
        a = 2 * math.pi * k / 12
        img.load()[int(8 + math.cos(a) * 5.3), int(8 + math.sin(a) * 5.3)] = (40, 40, 44, 255)
    for t in range(6):
        img.load()[8 + t // 2, 8 - t // 2] = (200, 40, 40, 255)
    return img


@C.texture("scale_mat")
def _scale_mat():
    img = fill((52, 54, 58), grain=1, seed=4105)
    for y in range(1, 16, 3):
        rect(img, 1, y, 15, y + 1, (70, 72, 76))
    return img


@C.texture("scale_head")
def _scale_head():
    img = fill((226, 228, 226), grain=1, seed=4106)
    rect(img, 2, 3, 14, 10, (20, 26, 30))
    rect(img, 3, 12, 6, 14, (60, 110, 190))
    rect(img, 10, 12, 13, 14, (90, 94, 100))
    return img


@C.texture("scale_screen")
def _scale_screen():
    img = fill((20, 26, 30), grain=1, seed=4107)
    draw_text(img, "72", 1, 5, (130, 230, 150))
    img.load()[9, 9] = (130, 230, 150, 255)
    draw_text(img, "4", 11, 5, (130, 230, 150))
    return img


@C.texture("dialysis_front")
def _dialysis_front():
    """The dialysis machine's working face: the blood pump and its arterial line, the venous
    clamp and drip chamber, the heparin syringe, the concentrate connectors."""
    img = fill((232, 234, 230), size=32, grain=1, seed=4108)
    frame(img, 0, 0, 32, 32, (196, 198, 196))
    disc(img, 9, 9, 6, (180, 182, 186))
    disc(img, 9, 9, 4.6, (70, 74, 80))
    for a in (0, 120, 240):
        t = math.radians(a)
        disc(img, 9 + math.cos(t) * 3, 9 + math.sin(t) * 3, 1.2, (210, 210, 214))
    red = (178, 30, 36)
    rect(img, 2, 15, 16, 16, red)
    rect(img, 15, 4, 16, 16, red)
    rect(img, 15, 4, 24, 5, red)
    rect(img, 22, 4, 26, 12, (226, 220, 222))
    rect(img, 23, 6, 25, 11, red)
    rect(img, 23, 12, 24, 22, red)
    rect(img, 21, 22, 27, 25, (60, 64, 70))
    rect(img, 4, 20, 14, 23, (246, 246, 244))
    rect(img, 4, 21, 9, 22, (210, 220, 240))
    for x, c in ((5, (220, 60, 50)), (12, (60, 110, 200))):
        disc(img, x, 28, 1.6, c)
    return img


def dialysis_frame(i):
    """The treatment screen: the pressures' bar graph, the time left counting down, the blood
    flow, and a heartbeat-slow sweep along the bottom."""
    def f():
        img = fill(hc.SCREEN_BLACK, size=32, grain=1, seed=4110)
        rect(img, 0, 0, 32, 6, (30, 70, 120))
        draw_text(img, "HD", 2, 1, (250, 250, 250))
        draw_text(img, "3:%02d" % (40 - i), 14, 1, (250, 250, 250))
        for k, (h, c) in enumerate(((10 + (i % 3), (230, 80, 70)), (7 + (i % 2), (90, 160, 240)),
                                    (12 - (i % 3), (240, 200, 60)))):
            rect(img, 2 + k * 6, 22 - h, 6 + k * 6, 22, c)
        draw_text(img, "300", 20, 9, (230, 80, 70))
        draw_text(img, "500", 20, 16, (90, 220, 140))
        x = 2 + i * 3
        rect(img, 2, 26, 30, 27, (40, 60, 70))
        rect(img, x, 25, x + 3, 28, (90, 220, 140))
        return img
    return f


hc.animation(C, "dialysis_screen", [dialysis_frame(i) for i in range(10)], 20)


def ro_frame(i):
    """The water unit's controller: product conductivity and flow, and the pump's run lamp."""
    def f():
        img = fill((70, 76, 86), size=32, grain=1, seed=4120)
        rect(img, 3, 3, 29, 17, (20, 26, 30))
        draw_text(img, "%d" % (6 + (i % 3)), 5, 5, (130, 230, 150))
        draw_text(img, "US", 14, 5, (130, 230, 150))
        draw_text(img, "%d" % (40 + i), 5, 11, (240, 200, 60))
        draw_text(img, "LM", 14, 11, (240, 200, 60))
        disc(img, 7, 23, 2.2, (90, 220, 120) if i % 2 == 0 else (40, 90, 50))
        disc(img, 16, 23, 2.2, (240, 190, 40) if i == 3 else (90, 70, 20))
        rect(img, 21, 21, 29, 26, (200, 40, 40))
        return img
    return f


hc.animation(C, "ro_panel", [ro_frame(i) for i in range(4)], 20)


@C.texture("ro_label")
def _ro_label():
    img = fill((40, 84, 150), size=32, grain=1, seed=4121)
    draw_text_centred(img, "DIALYSIS", 16, 7, (250, 250, 250))
    draw_text_centred(img, "RO WATER", 16, 14, (250, 250, 250))
    draw_text_centred(img, "LOOP", 16, 21, (250, 250, 250))
    return img


@C.texture("endo_light_front")
def _endo_light_front():
    img = fill((226, 228, 230), size=32, grain=1, seed=4130)
    disc(img, 7, 16, 4, (40, 42, 46))
    disc(img, 7, 16, 2, (120, 124, 130))
    rect(img, 14, 10, 29, 16, (20, 26, 30))
    draw_text(img, "100", 16, 11, (130, 230, 150))
    for x in (15, 20, 25):
        rect(img, x, 20, x + 3, 23, (150, 154, 160))
    return img


@C.texture("endo_proc_front")
def _endo_proc_front():
    img = fill((226, 228, 230), size=32, grain=1, seed=4131)
    rect(img, 3, 10, 13, 22, (40, 42, 46))
    rect(img, 4, 11, 12, 21, (70, 74, 80))
    rect(img, 16, 10, 29, 15, (20, 26, 30))
    for x in (16, 21, 26):
        rect(img, x, 18, x + 3, 21, (60, 110, 190) if x == 16 else (150, 154, 160))
    return img


def endo_frame(i):
    """The scope's view: the lumen of a bowel going away from the camera, drifting."""
    def f():
        img = Image.new("RGBA", (32, 32), (8, 8, 10, 255))
        cx, cy = 15 + math.cos(i * math.pi / 4) * 2, 16 + math.sin(i * math.pi / 4) * 1.5
        px = img.load()
        for y in range(2, 30):
            for x in range(4, 28):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                fold = 0.5 + 0.5 * math.cos(d * 1.4 - i * 0.6)
                k = max(0.0, min(1.0, d / 13.0))
                r = int(80 + 160 * k + 20 * fold)
                g = int(20 + 70 * k + 14 * fold)
                b = int(24 + 60 * k + 12 * fold)
                if d < 2.4:
                    r, g, b = 26, 10, 12
                px[x, y] = (min(r, 255), min(g, 255), min(b, 255), 255)
        rect(img, 0, 0, 4, 32, (8, 8, 10))
        rect(img, 28, 0, 32, 32, (8, 8, 10))
        draw_text(img, "1", 0, 2, (220, 220, 220))
        return img
    return f


hc.animation(C, "endo_screen", [endo_frame(i) for i in range(8)], 4, interpolate=True)


# ==========================================================================================
# Block helpers (the wards' helpers, bound to this catalogue)
# ==========================================================================================
def facing_block(reg, java, names, textures, elements, tab, extra=None, display=None):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    C.add(reg, java, names, {reg: model(textures, elements, display=display)},
          hc.facing_state(M(reg), extra), tab=tab)


def java_for(cls, reg, box6, mat, mount, collides, seat=None):
    java = 'new %s("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.%s, %s)' % (
        cls, reg, B(*box6), mat, mount, "true" if collides else "false")
    if seat:
        java += ".withSeat(%g, %g)" % seat
    return java


def prop(reg, box6, mat, mount, collides, names, textures, elements, tab, seat=None,
         display=None):
    facing_block(reg, java_for("BlockHealthcare", reg, box6, mat, mount, collides, seat),
                 names, textures, elements, tab, display=display)


def screen_prop(reg, box6, mat, mount, collides, names, textures, elements, tab, on_tex,
                display=None, seat=None):
    on = {"on": {"false": {}, "true": {"textures": {"screen": on_tex}}}}
    facing_block(reg, java_for("BlockHealthcareScreen", reg, box6, mat, mount, collides, seat),
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


def pair_java(reg, box6, mat, mount, shape, collides=True):
    extra = "" if collides else ", false"
    return ('new BlockHealthcarePair("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.%s, '
            'BlockHealthcarePair.Shape.%s%s)' % (reg, B(*box6), mat, mount, shape, extra))


def iv_stand(x, z, top=30):
    """A small IV stand: four short feet with casters, a pole to ``top`` and a hook bar."""
    els = [el([x - 2.5, 0.6, z - 0.4], [x + 2.5, 1.2, z + 0.4], "grey"),
           el([x - 0.4, 0.6, z - 2.5], [x + 0.4, 1.2, z + 2.5], "grey")]
    els += casters([(x - 2.2, z), (x + 2.2, z), (x, z - 2.2), (x, z + 2.2)])
    els += octo(x, z, 0.4, 1.2, top, "metal")
    els += [el([x - 2.5, top - 0.6, z - 0.3], [x + 2.5, top - 0.1, z + 0.3], "metal")]
    return els


# ==========================================================================================
# Exam room
# ==========================================================================================
# The exam table: two cells wide along the wall, its head (backrest raised) to the viewer's
# left, the paper roll under the head end feeding paper down the table, a pull-out step in
# front.
EXAM = [front([-14, 0, 3], [14, 8.5, 13], "cabinet", "front"),
        el([-14.5, 8.5, 2.5], [14.5, 9.2, 13.5], "grey"),
        el([-6, 2.5, 0.5], [6, 4.5, 3], "cabinet"),
        el([-6, 4.5, 0.5], [6, 4.8, 3], "tread"),
        el([-14, 9.2, 3.5], [7, 11.2, 12.5], "pad"),
        el([-14, 11.2, 5], [7, 11.25, 11], "paper", faces=("up",)),
        rot(el([7, 9.2, 3.5], [14.5, 11.2, 12.5], "pad"), "z", 22.5, [7, 9.2, 8]),
        rot(el([7, 11.2, 5], [14.5, 11.25, 11], "paper", faces=("up",)), "z", 22.5,
            [7, 9.2, 8]),
        el([14.6, 8.6, 5.5], [16, 10.2, 10.5], "roll", per={"east": "roll_end"},
           uv={"east": FULL}),
        el([14.5, 8.4, 5], [15.9, 8.6, 11], "metal"),
        el([14.5, 8.6, 5], [15.9, 10.4, 5.4], "metal"),
        el([14.5, 8.6, 10.6], [15.9, 10.4, 11], "metal")]
pair_block("exam_table",
           ("Exam Table", "Untersuchungsliege", "Camilla de exploración", "Undersökningsbrits"),
           EXAM, 0,
           {"cabinet": T("cabinet"), "front": T("exam_cabinet_front"), "pad": T("exam_vinyl"),
            "paper": T("exam_paper"), "roll": T("exam_paper"), "roll_end": T("paper_roll_end"),
            "tread": T("tread"), "grey": GREY, "metal": STEEL, "particle": T("cabinet")},
           pair_java("exam_table", (0, 0, 2, 16, 12, 14), "WOOD", "FLOOR", "WIDE"), PC)

# the rolling exam light: a stand, a gooseneck rising forward, a round LED head facing down
LIGHT_TEX = {"metal": STEEL, "grey": GREY, "rubber": RUBBER, "housing": W("warm_white"),
             "neck": T("cord_black"),
             "lens": K("exam_lens_off"), "particle": W("warm_white")}
EXAM_LIGHT = (hw.STAND_BASE + casters([(2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)])
              + octo(8, 8, 0.5, 1.4, 22, "metal")
              # the gooseneck: up out of the pole, then over and down to the head in front
              + [el([7.5, 22, 7.5], [8.5, 25, 8.5], "neck"),
                 rot(el([7.5, 24.4, 3.6], [8.5, 25.4, 8.4], "neck"), "x", -22.5,
                     [8, 24.9, 8]),
                 el([7.5, 24.2, 2.6], [8.5, 25.2, 4.2], "neck")]
              + octo(8, 3.4, 2.8, 21.6, 24.2, "housing")
              + [el([5.8, 21.55, 1.2], [10.2, 21.6, 5.6], "lens", faces=("down",),
                    uv={"down": FULL}),
                 el([10.6, 22.4, 3], [12.6, 23, 3.8], "grey")])
C.add("clinic_exam_light",
      'new BlockHealthcareLight("clinic_exam_light", %s, 12)' % B(4, 0, 0, 12, 16, 12),
      ("Exam Light (Rolling Stand)", "Untersuchungsleuchte (Rollstativ)",
       "Lámpara de exploración (soporte rodante)", "Undersökningslampa (rullstativ)"),
      {"clinic_exam_light": model(LIGHT_TEX, EXAM_LIGHT, display=ITEM_TALL)},
      hw.light_state(M("clinic_exam_light"), {"lens": K("exam_lens_on")}), tab=PC)

# the wall diagnostic set: a plate with two handles in their cradles, coiled cords, the blood
# pressure gauge and its cuff basket
DIAG = [front([2, 3, 15.2], [14, 13, 16], "plate", "front"),
        el([2.6, 8.6, 13.4], [4.6, 13.6, 15.2], "handle"),
        el([3.0, 13.6, 13.8], [4.2, 15.2, 14.8], "head"),
        el([5.4, 8.6, 13.4], [7.4, 13.6, 15.2], "handle"),
        el([5.8, 13.6, 13.4], [7.0, 14.8, 15.2], "head"),
        el([3.3, 4.2, 14.3], [3.9, 8.6, 14.9], "cord"),
        el([6.1, 4.2, 14.3], [6.7, 8.6, 14.9], "cord"),
        el([9, 7.5, 14.6], [13.5, 12, 15.2], "body"),
        el([9.2, 7.7, 14.55], [13.3, 11.8, 14.55], "gauge", faces=("north",), uv={"north": FULL}),
        el([9, 3.5, 13], [13.5, 7, 15.2], "basket"),
        el([11, 2, 13.2], [11.6, 3.5, 13.8], "cord")]
prop("wall_diagnostic_set", (2, 2, 13, 14, 16, 16), "PLASTIC", "WALL", False,
     ("Wall Diagnostic Set", "Wanddiagnostikstation", "Equipo de diagnóstico de pared",
      "Väggmonterat diagnostikset"),
     {"plate": W("white"), "front": T("diag_panel"), "handle": T("cord_black"),
      "head": W("dark_grey"), "cord": T("cord_black"), "body": W("white"),
      "gauge": T("bp_gauge"), "basket": W("dark_grey"), "particle": W("white")},
     DIAG, PC)

# the column scale: a platform, a column at the back with the head (display) at chest height
# and the height rod rising out of it to its headpiece
SCALE = [el([3, 0, 2.5], [13, 1.4, 12.5], "body", per={"up": "mat"}, uv={"up": FULL}),
         el([7, 1.4, 12.5], [9, 18, 14], "body"),
         front([5, 18, 11], [11, 22, 14], "body", "head"),
         screen([6, 20.2, 10.95], [10, 21.6, 10.95], "screen"),
         el([7.6, 22, 12.8], [8.4, 31, 13.6], "metal"),
         el([7.4, 30.4, 9.5], [8.6, 31, 13.6], "metal")]
screen_prop("patient_scale", (3, 0, 2, 13, 16, 14), "METAL", "FLOOR", True,
            ("Patient Scale (Column)", "Säulenwaage", "Báscula de columna", "Pelarvåg"),
            {"body": W("warm_white"), "mat": T("scale_mat"), "head": T("scale_head"),
             "screen": W("screen_off"), "metal": STEEL, "particle": W("warm_white")},
            SCALE, PC, T("scale_screen"), display=ITEM_TALL)

# the phlebotomy chair: a padded seat and back on a pedestal, two long padded arms
PHLEB = [el([5, 0, 5], [11, 0.8, 11], "grey"),
         el([7, 0.8, 7], [9, 6, 9], "metal"),
         el([3, 6, 3], [13, 8.5, 13], "pad"),
         el([3.5, 8, 12], [12.5, 20, 14.5], "pad"),
         el([3.5, 6, 13.2], [12.5, 12, 14], "grey"),
         el([0.6, 10.5, 1], [3.2, 12, 13], "pad"),
         el([12.8, 10.5, 1], [15.4, 12, 13], "pad"),
         el([1.4, 6, 9], [2.4, 10.5, 10], "metal"),
         el([13.6, 6, 9], [14.6, 10.5, 10], "metal"),
         el([2.4, 7, 9.2], [3, 8, 9.8], "metal"),
         el([13, 7, 9.2], [13.6, 8, 9.8], "metal")]
prop("phlebotomy_chair", (1, 0, 1, 15, 16, 15), "CLOTH", "FLOOR", True,
     ("Phlebotomy Chair", "Blutentnahmestuhl", "Sillón de extracciones", "Provtagningsstol"),
     {"pad": W("vinyl_blue"), "grey": GREY, "metal": STEEL, "particle": W("vinyl_blue")},
     PHLEB, PC, seat=(8.5, 0.5), display=ITEM_TALL)

# a two-step stool with a hand rail
STOOL = [el([3, 0.6, 2.8], [3.8, 4.4, 3.6], "metal"), el([12.2, 0.6, 2.8], [13, 4.4, 3.6], "metal"),
         el([3, 0.6, 7.6], [3.8, 9.4, 8.4], "metal"), el([12.2, 0.6, 7.6], [13, 9.4, 8.4], "metal"),
         el([3, 0.6, 12.2], [3.8, 9.4, 13], "metal"), el([12.2, 0.6, 12.2], [13, 9.4, 13], "metal"),
         el([3, 4.4, 2.4], [13, 5, 8], "tread"),
         el([3, 9.4, 7.6], [13, 10, 13.4], "tread"),
         el([2.9, 0, 2.7], [3.9, 0.6, 3.7], "rubber"), el([12.1, 0, 2.7], [13.1, 0.6, 3.7], "rubber"),
         el([2.9, 0, 7.5], [3.9, 0.6, 8.5], "rubber"), el([12.1, 0, 7.5], [13.1, 0.6, 8.5], "rubber"),
         el([2.9, 0, 12.1], [3.9, 0.6, 13.1], "rubber"),
         el([12.1, 0, 12.1], [13.1, 0.6, 13.1], "rubber"),
         el([12.4, 10, 12.4], [13, 24, 13], "metal"),
         el([12.4, 23.4, 6], [13, 24, 13], "metal"),
         el([12.3, 22, 6.2], [13.1, 23.4, 7], "metal")]
prop("clinic_step_stool", (3, 0, 2, 13, 16, 14), "METAL", "FLOOR", True,
     ("Step Stool (Clinic)", "Tritthocker (Praxis)", "Escalón para pacientes",
      "Fotpall (mottagning)"),
     {"metal": STEEL, "tread": T("tread"), "rubber": RUBBER, "particle": STEEL},
     STOOL, PC, display=ITEM_TALL)

# ==========================================================================================
# Dialysis
# ==========================================================================================
def treatment_chair(pad, x0=1, x1=15):
    """A reclining treatment chair between x0 and x1: a base, a seat, two padded arms, a tall
    back with a head pillow, and the leg rest raised in front."""
    return [el([x0 + 1, 0, 3], [x1 - 1, 3, 14], "grey"),
            el([x0 + 1, 3, 3], [x1 - 1, 7.5, 13], pad),
            el([x0, 3, 3.5], [x0 + 2, 11, 14], pad), el([x1 - 2, 3, 3.5], [x1, 11, 14], pad),
            el([x0 + 1.5, 7, 12], [x1 - 1.5, 22, 15.5], pad),
            el([x0 + 3, 19, 11], [x1 - 3, 22, 12], pad),
            rot(el([x0 + 2, 3.5, -1], [x1 - 2, 6, 3.5], pad), "x", 22.5, [8, 6, 3.5])]


DCHAIR = treatment_chair("vinyl") + [el([0.2, 11, 4], [1.2, 11.5, 12], "grey"),
                                     el([14.8, 11, 4], [15.8, 11.5, 12], "grey")]
prop("dialysis_chair", (1, 0, 0, 15, 16, 16), "CLOTH", "FLOOR", True,
     ("Dialysis Chair", "Dialysestuhl", "Sillón de diálisis", "Dialysstol"),
     {"vinyl": T("dialysis_vinyl"), "grey": GREY, "particle": T("dialysis_vinyl")},
     DCHAIR, PC, seat=(8, 1), display=ITEM_TALL)

# the dialysis machine: a cabinet on casters, its working face (blood pump, lines, clamp), the
# dialyzer upright on its left, the concentrate jugs on the base, the screen on top
DMACHINE = (casters([(3.5, 4.5), (12.5, 4.5), (3.5, 13), (12.5, 13)])
            + [el([3, 0.8, 4], [13, 2, 14], "grey"),
               el([3.5, 2, 5], [12.5, 13, 14], "body"),
               front([3.5, 13, 4.5], [12.5, 21, 14], "body", "front"),
               el([4, 21, 6], [12, 22, 11], "grey"),
               el([3.2, 22, 5.4], [12.8, 29.6, 7.2], "bezel"),
               screen([3.8, 22.6, 5.35], [12.2, 29, 5.35], "screen"),
               el([3.5, 2.2, 3], [7, 7, 5], "acid"), el([4.6, 7, 3.6], [5.9, 7.8, 4.4], "red"),
               el([8.5, 2.2, 3], [12, 7, 5], "bicarb"),
               el([9.6, 7, 3.6], [10.9, 7.8, 4.4], "blue")]
            + octo(2.2, 9, 0.9, 13, 19, "dialyzer")
            + [el([2.6, 15.5, 8.6], [3.5, 16, 9.4], "grey"),
               el([1.9, 19, 8.7], [2.5, 21, 9.3], "line"),
               el([1.9, 11, 8.7], [2.5, 13, 9.3], "line")])
screen_prop("dialysis_machine", (2, 0, 3, 14, 16, 14), "METAL", "FLOOR", True,
            ("Dialysis Machine", "Dialysegerät", "Máquina de diálisis", "Dialysmaskin"),
            {"body": W("warm_white"), "front": T("dialysis_front"), "grey": GREY,
             "rubber": RUBBER, "bezel": K("bezel"), "screen": W("screen_off"),
             "acid": T("acid_jug"), "bicarb": T("bicarb_jug"), "red": W("red"),
             "blue": T("pump_blue"), "dialyzer": T("membrane"), "line": T("blood_line"),
             "particle": W("warm_white")},
            DMACHINE, CL, T("dialysis_screen"), display=ITEM_TALL)

# the RO water unit: a steel frame two tall, three membrane vessels lying across it, the carbon
# tank and booster pump below, the controller and the loop's label on the front
RO = [el([0.5, 0, 9], [1.5, 31, 10], "metal"), el([14.5, 0, 9], [15.5, 31, 10], "metal"),
      el([0.5, 0, 14.5], [1.5, 31, 15.5], "metal"), el([14.5, 0, 14.5], [15.5, 31, 15.5], "metal"),
      el([0.5, 0, 9], [15.5, 1, 15.5], "metal"), el([0.5, 30.5, 9], [15.5, 31.5, 15.5], "metal"),
      el([0.5, 12.5, 9], [15.5, 13.3, 15.5], "metal")]
for _y in (21, 24.8, 28.6):
    RO += hc.pipe_x(_y, 11.6, 1.7, 0.9, 15.1, "vessel")
    RO += hc.pipe_x(_y, 11.6, 1.9, 0.4, 1.1, "grey")
    RO += hc.pipe_x(_y, 11.6, 1.9, 14.9, 15.6, "grey")
RO += octo(10.5, 4.5, 3.2, 1, 12, "tank")
RO += octo(10.5, 4.5, 1.2, 12, 13.5, "grey")
RO += [el([2, 1, 5], [7, 5, 9], "pump"), el([2.5, 5, 6], [6.5, 6, 8], "grey"),
       el([4, 6, 6.6], [4.8, 16, 7.4], "pipe"), el([4, 15.2, 7.4], [4.8, 16, 12.2], "pipe"),
       front([1.5, 13.6, 8.2], [8.5, 18.6, 9], "grey", "panel"),
       front([9.2, 13.6, 8.4], [14.6, 18.2, 9], "grey", "label")]
pair_block("dialysis_ro_unit",
           ("Dialysis RO Water Unit", "Umkehrosmoseanlage für Dialysewasser",
            "Unidad de ósmosis inversa para diálisis", "Omvänd osmos-anläggning för dialysvatten"),
           RO, 1,
           {"metal": STEEL, "grey": GREY, "vessel": T("membrane"), "tank": T("carbon_tank"),
            "pump": T("pump_blue"), "pipe": W("white"), "panel": T("ro_panel"),
            "label": T("ro_label"), "particle": STEEL},
           pair_java("dialysis_ro_unit", (0, 0, 1, 16, 16, 16), "METAL", "FLOOR", "TALL"),
           CL, item_display=ITEM_TALL)

# ==========================================================================================
# Infusion
# ==========================================================================================
# the treatment chair drawn narrower, with an IV stand, pump and bag beside its right arm
INFUSION = (treatment_chair("vinyl", 0.5, 12.5)
            + iv_stand(14, 12)
            + [el([12.6, 14, 10.6], [15.4, 18.5, 13.4], "pbody"),
               screen([12.9, 16, 10.55], [15.1, 18, 10.55], "screen"),
               el([13.7, 23, 11.6], [14.3, 23.6, 12.4], "metal"),
               el([12.4, 23.6, 11.4], [14.2, 28.4, 12.6], "bag"),
               el([13.2, 18.5, 11.8], [13.5, 23.6, 12.1], "tube")])
screen_prop("infusion_recliner", (0, 0, 0, 16, 16, 16), "CLOTH", "FLOOR", True,
            ("Infusion Recliner (with IV Pump)", "Infusionssessel (mit Infusionspumpe)",
             "Sillón de infusión (con bomba)", "Infusionsstol (med infusionspump)"),
            {"vinyl": T("infusion_vinyl"), "grey": GREY, "metal": STEEL, "rubber": RUBBER,
             "pbody": W("warm_white"), "screen": W("screen_off"), "bag": W("iv_bag"),
             "tube": W("cord"), "particle": T("infusion_vinyl")},
            INFUSION, PC, W("pump_screen"), display=ITEM_TALL, seat=(8, 1))

# ==========================================================================================
# Dental and eye exam
# ==========================================================================================
@C.texture("acuity_chart")
def _acuity_chart():
    """An eye chart in the highway font: one big letter on top, rows getting smaller, a red
    and a green line near the foot. Drawn at the chart's shape (10 x 14 px) and stretched
    square."""
    img = fill((250, 250, 246), size=128, grain=1, seed=4140).resize((92, 128))
    ink = (24, 24, 26)
    frame(img, 0, 0, 92, 128, (196, 196, 192), width=2)
    y = 6
    for text, cap in (("E", 26), ("F P", 13), ("T O Z", 9), ("L P E D", 7), ("P E C F D", 5),
                      ("E D F C Z P", 4)):
        hc.stamp_legend(img, text, 46, y, ink, cap, "D", max_w=80)
        y += cap + 6
    rect(img, 12, y + 2, 80, y + 3, (200, 40, 40))
    rect(img, 12, y + 6, 80, y + 7, (40, 160, 70))
    return img.resize((128, 128), Image.LANCZOS)


@C.texture("operatory_lens_off")
def _operatory_lens_off():
    img = fill((196, 198, 196), grain=1, seed=4141)
    rect(img, 2, 4, 14, 12, (150, 152, 150))
    return img


@C.texture("operatory_lens_on")
def _operatory_lens_on():
    img = fill((255, 252, 236), grain=1, seed=4142)
    rect(img, 2, 4, 14, 12, (255, 255, 255))
    return img


@C.texture("delivery_top")
def _delivery_top():
    img = fill((236, 236, 232), grain=1, seed=4143)
    for x in (3, 6, 9, 12):
        rect(img, x, 2, x + 1, 12, (60, 64, 70))
    rect(img, 2, 13, 14, 15, (90, 200, 120))
    return img


@C.texture("phoropter_front")
def _phoropter_front():
    img = fill((40, 42, 46), grain=1, seed=4144)
    for cx in (4.5, 11.5):
        disc(img, cx, 8, 3.2, (150, 154, 160))
        disc(img, cx, 8, 1.8, (20, 24, 30))
    rect(img, 7, 3, 9, 13, (90, 94, 100))
    return img


# the dental chair: a reclined couch on its base, the delivery unit's tray on an arm over the
# patient's right, the operatory light on a post from the back, the cuspidor on the left
DENTAL = [el([5, 0, 5], [11, 1, 13], "grey"),
          el([6.5, 1, 7], [9.5, 5, 11], "body"),
          el([3, 5, 2], [13, 7.5, 13], "vinyl"),
          rot(el([4, 6, -4], [12, 8, 2], "vinyl"), "x", 22.5, [8, 7, 2]),
          rot(el([3.5, 7, 12], [12.5, 18, 14.5], "vinyl"), "x", -22.5, [8, 7, 13]),
          el([5.5, 17, 14.6], [10.5, 20, 16.5], "vinyl"),
          el([2.2, 7.5, 4], [3.2, 9.5, 11], "body"), el([12.8, 7.5, 4], [13.8, 9.5, 11], "body"),
          # delivery unit
          el([13.5, 1, 9], [15, 12, 10.5], "body"),
          el([10, 11, 2], [15.5, 11.8, 9], "tray"),
          el([10.5, 11.8, 2.5], [15, 12, 8.5], "delivery", faces=("up",), uv={"up": FULL}),
          # cuspidor
          el([0.4, 1, 10], [2.2, 11, 11.8], "body")]
DENTAL += octo(1.6, 8.5, 1.6, 11, 12, "bowl")
DENTAL += [el([7.5, 20, 15], [8.5, 30, 16], "metal"),
           el([7.5, 29, 6], [8.5, 30, 16], "metal"),
           # the light's lens is the "screen" key, so the on state lights it
           el([5.5, 26.6, 4.5], [10.5, 29, 7.5], "body", per={"down": "screen"},
              uv={"down": FULL})]
screen_prop("dental_chair", (2, 0, 1, 14, 16, 16), "CLOTH", "FLOOR", True,
            ("Dental Chair", "Zahnarztstuhl", "Sillón dental", "Tandläkarstol"),
            {"vinyl": T("dialysis_vinyl"), "body": W("warm_white"), "grey": GREY,
             "metal": STEEL, "tray": W("warm_white"), "delivery": T("delivery_top"),
             "bowl": W("white"), "screen": T("operatory_lens_off"), "particle": T("dialysis_vinyl")},
            DENTAL, PC, T("operatory_lens_on"), display=ITEM_TALL, seat=(7.5, 0.5))

# the eye exam lane's chair: a padded chair on a pedestal, the instrument stand at its side,
# its arm reaching over to hold the phoropter in front of the patient's eyes
REFRACT = [el([5, 0, 5], [11, 0.8, 11], "grey"),
           el([7, 0.8, 7], [9, 6, 9], "metal"),
           el([3, 6, 3], [13, 8.5, 13], "vinyl"),
           el([3.5, 8, 12], [12.5, 22, 14.5], "vinyl"),
           el([1.2, 8.5, 4], [3, 11, 12], "vinyl"), el([13, 8.5, 4], [14.8, 11, 12], "vinyl"),
           el([14.6, 0, 11], [16, 25, 13], "body"),
           el([8, 24, 11.4], [14.6, 25, 12.4], "metal"),
           el([8, 22.6, 7], [9, 24, 12], "metal"),
           front([5, 18.5, 6], [11, 22.6, 8], "dark", "phoropter")]
prop("refraction_chair", (1, 0, 3, 16, 16, 15), "CLOTH", "FLOOR", True,
     ("Eye Exam Chair (Phoropter)", "Augenuntersuchungsstuhl (Phoropter)",
      "Sillón de oftalmología (foróptero)", "Ögonundersökningsstol (foropter)"),
     {"vinyl": W("vinyl_blue"), "body": W("warm_white"), "grey": GREY, "metal": STEEL,
      "dark": W("charcoal"), "phoropter": T("phoropter_front"), "particle": W("vinyl_blue")},
     REFRACT, PC, seat=(8.5, 0.5), display=ITEM_TALL)

prop("visual_acuity_chart", (3, 1, 15, 13, 15, 16), "PLASTIC", "WALL", False,
     ("Visual Acuity Chart", "Sehtesttafel", "Optotipo de agudeza visual", "Synkontrolltavla"),
     {"chart": T("acuity_chart"), "body": W("white")},
     [el([3, 1, 15.5], [13, 15, 16], "body", faces=("south", "east", "west", "up", "down")),
      screen([3, 1, 15.5], [13, 15, 15.5], "chart")], PC)

# ==========================================================================================
# Endoscopy
# ==========================================================================================
ENDO = (casters([(3, 4), (13, 4), (3, 12.5), (13, 12.5)])
        + [el([2.5, 0.8, 3.5], [13.5, 1.6, 13], "grey"),
           el([2.5, 1.6, 12.2], [3.3, 22, 13], "grey"), el([12.7, 1.6, 12.2], [13.5, 22, 13], "grey"),
           el([2.5, 1.6, 3.5], [3.3, 22, 4.3], "grey"), el([12.7, 1.6, 3.5], [13.5, 22, 4.3], "grey"),
           el([2.5, 7, 3.5], [13.5, 7.6, 13], "grey"),
           el([2.5, 13.4, 3.5], [13.5, 14, 13], "grey"),
           el([2.5, 21.4, 3.5], [13.5, 22, 13], "grey"),
           front([3.5, 1.6, 4.5], [12.5, 6.6, 12], "dev", "light"),
           front([3.5, 7.6, 4.5], [12.5, 12.8, 12], "dev", "proc"),
           front([3.5, 14, 4.5], [12.5, 18, 12], "dev", "insuf"),
           el([7, 22, 9], [9, 24, 11], "grey"),
           el([2, 23.4, 8], [14, 31.4, 9.4], "mbody"),
           screen([2.6, 24, 7.95], [13.4, 30.8, 7.95], "screen"),
           # the scope: its control head on a hook on the right post, the insertion tube looped
           el([13.5, 18, 5], [14.5, 19, 6], "metal"),
           el([13.6, 15, 4.6], [15.4, 18.4, 6.4], "scope"),
           el([14.1, 4, 5.1], [14.9, 15, 5.9], "scope"),
           el([14.1, 4, 5.9], [14.9, 4.8, 11], "scope"),
           el([14.1, 4.8, 10.2], [14.9, 12, 11], "scope"),
           el([14.0, 9, 6.2], [15.0, 10, 10], "cord")])
screen_prop("endoscopy_tower", (2, 0, 3, 15, 16, 13), "METAL", "FLOOR", True,
            ("Endoscopy Tower", "Endoskopieturm", "Torre de endoscopia", "Endoskopitorn"),
            {"grey": GREY, "rubber": RUBBER, "metal": STEEL, "dev": W("warm_white"),
             "light": T("endo_light_front"), "proc": T("endo_proc_front"),
             "insuf": K("insufflator_front"), "mbody": W("monitor_body"),
             "screen": W("screen_off"), "scope": T("scope_black"), "cord": T("cord_black"),
             "particle": GREY},
            ENDO, CL, T("endo_screen"), display=ITEM_TALL)


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
