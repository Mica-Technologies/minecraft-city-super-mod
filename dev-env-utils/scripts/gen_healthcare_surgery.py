#!/usr/bin/env python3
"""
gen_healthcare_surgery.py -- the Healthcare module's operating room set.

A CSM operating room, made for the hospital builder as a consistent alternative to a third-party
theatre pack (nothing here is taken from one: no names, no models): the operating table (two
cells, a seat to lie on as the bed's sit-up is), the anaesthesia machine with an animated
workstation screen, the twin-head surgical light hung from the ceiling on the drop column every
ceiling-hung Healthcare piece uses (lit by Lighting's states), the scrub sink (one bay a block,
bays joining into one trough), the instrument back table (two cells), the Mayo stand, the kick
bucket, the supply cabinet (two tall) and the sterile drape trolley.

Swatches the wards and clinical generators already draw (stainless, the frame grey, rubber, the
warm white, the monitor body, the exam light's lenses, the drawer banks, gas cylinders, wire
shelves) are shared by path, not drawn again; this generator draws only the pad, the drape, the
instrument layout, the anaesthesia machine's front and screen, the bellows and the cabinet
glazing.

Nothing here is a real product: no maker's name, no red cross (a protected emblem). The screen
is an animated texture (.mcmeta), so nothing ticks and nothing has a renderer.

Writes, under modules/healthcare/src/main/resources/assets/csm:

  * textures/blocks/healthcare/surgery/*.png (+ .mcmeta for the screen)
  * models/block/healthcare/surgery/*.json
  * blockstates/<registry>.json, and models/item/<registry>.json for the multipart blocks
  * the tile names in all four lang files

Every model faces north with what it stands against at +Z.

Usage:
    python gen_healthcare_surgery.py              # write everything
    python gen_healthcare_surgery.py --check      # fail if the tree has drifted
    python gen_healthcare_surgery.py --fragments  # print the tab registration lines

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
from gen_healthcare_common import fill, rect, shade  # noqa: E402

C = hc.catalogue("gen_healthcare_surgery.py", "surgery")
T = C.T
M = C.M
W = hw.C.T    # the wards' shared swatches
K = hcl.C.T   # the clinical generator's

el = hw.el
screen = hw.screen
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

CL = hc.CLINICAL

STEEL = W("stainless")
GREY = W("frame_grey")
RUBBER = W("rubber")
WHITE = W("warm_white")
DARK = W("dark_grey")


def MI(name):
    """An item model's parent: item models resolve from models/, so the block/ is needed."""
    return "csm:block/%s/%s" % (C.model_dir, name)


def front(frm, to, base, key, side="north", faces=hw.ALL):
    """A box whose front (north, or ``side``) shows the whole of a texture."""
    return el(frm, to, base, faces=faces, per={side: key}, uv={side: FULL})


FACE_DIMS = {"north": (0, 1), "south": (0, 1), "east": (2, 1), "west": (2, 1), "up": (0, 2),
             "down": (0, 2)}


def safe_uv(elements):
    """A round part built past the cell (the octagon helper clamps its UVs to the cell, which
    leaves a sliver at u 16) takes a window of its own size from the texture's corner instead:
    every one of these is a flat swatch, so where in it the window sits does not matter."""
    for e in elements:
        for f, spec in e["faces"].items():
            uv = spec.get("uv")
            if uv is None:
                continue
            a, b = FACE_DIMS[f]
            w = abs(e["to"][a] - e["from"][a])
            h = abs(e["to"][b] - e["from"][b])
            if min(uv) < 0 or max(uv) > 16 or abs(uv[2] - uv[0]) < 0.05 < w:
                spec["uv"] = [0, 0, round(min(16, w), 3), round(min(16, h), 3)]
    return elements


def plane(frm, to, key, side):
    """A zero-thickness face showing the whole of a texture."""
    return el(frm, to, key, faces=(side,), uv={side: FULL})


# ==========================================================================================
# Textures
# ==========================================================================================
@C.texture("or_pad")
def _or_pad():
    """The table's pads: dark slate gel vinyl."""
    return hc.swatch((44, 52, 66), 9101, grain=2)


@C.texture("drape")
def _drape():
    """A sterile drape: ceil blue nonwoven."""
    return hc.fabric((92, 146, 190), 9102)


@C.texture("instruments")
def _instruments():
    """Instruments laid out on a drape, seen from above: clamps, scissors, a scalpel, a row of
    retractors and a folded towel, in steel on the drape's blue."""
    img = hc.fabric((92, 146, 190), 9103, size=32)
    steel = (196, 200, 204)
    dark = (120, 126, 132)
    # a folded towel
    rect(img, 22, 2, 30, 10, (232, 234, 230))
    rect(img, 22, 6, 30, 7, (210, 212, 208))
    # clamps and scissors: a shank and two finger rings each, in a row
    for i, x in enumerate(range(3, 19, 4)):
        rect(img, x, 3, x + 1, 13, steel)
        rect(img, x - 1, 13, x + 1, 15, dark)
        rect(img, x + 1, 13, x + 3, 15, dark)
        if i % 2:
            rect(img, x + 1, 3, x + 2, 6, steel)
    # a scalpel and a probe
    rect(img, 4, 19, 16, 20, steel)
    rect(img, 16, 19, 19, 20, dark)
    rect(img, 4, 23, 18, 24, steel)
    # retractors: an L each
    for x in (22, 25, 28):
        rect(img, x, 14, x + 1, 26, steel)
        rect(img, x - 1, 26, x + 2, 27, steel)
    # a kidney dish
    rect(img, 4, 27, 14, 30, (180, 184, 188))
    rect(img, 5, 28, 13, 29, (150, 154, 158))
    return img


@C.texture("anaesthesia_front")
def _anaesthesia_front():
    """The anaesthesia machine's front, 32 px: three flowmeter tubes (oxygen green, nitrous
    oxide blue, air yellow, a float in each) over their knobs, and the pressure gauges."""
    img = fill((226, 228, 226), size=32, grain=1, seed=9104)
    rect(img, 0, 0, 32, 32, (226, 228, 226))
    rect(img, 2, 2, 18, 22, (60, 66, 72))
    for i, colour in enumerate(((60, 170, 90), (60, 110, 200), (230, 200, 50))):
        x = 4 + i * 5
        rect(img, x, 3, x + 3, 17, (196, 214, 222))
        rect(img, x, 6 + 3 * i, x + 3, 8 + 3 * i, (40, 44, 48))
        rect(img, x, 18, x + 3, 21, colour)
    for cx in (23, 28):
        rect(img, cx - 3, 3, cx + 2, 8, (40, 44, 48))
        rect(img, cx - 2, 4, cx + 1, 7, (232, 232, 228))
        rect(img, cx - 1, 5, cx, 6, (40, 44, 48))
    rect(img, 20, 12, 31, 22, (200, 204, 206))
    for y in (14, 17, 20):
        rect(img, 21, y, 30, y + 1, (150, 154, 158))
    rect(img, 2, 25, 30, 30, (190, 194, 196))
    for x in range(4, 30, 6):
        rect(img, x, 26, x + 3, 29, (90, 96, 102))
    return img


def _screen_frame(i):
    """The anaesthesia workstation's screen: airway pressure, flow and CO2 traces moving left
    to right a quarter at a time, and the numbers down the right."""
    def f():
        img = Image.new("RGBA", (32, 32), (10, 14, 18, 255))
        px = img.load()
        traces = (((250, 220, 70), 6, lambda t: 0.6 if (t % 16) < 7 else -0.2),
                  ((90, 200, 240), 14, lambda t: math.sin(t * math.pi / 8)),
                  ((150, 230, 120), 23, lambda t: 1.0 if 3 < (t % 16) < 12 else -0.6))
        for colour, base, fn in traces:
            for x in range(22):
                if (x - i * 5) % 22 in (0, 1):
                    continue
                y = int(round(base - 3 * fn(x + i * 4)))
                px[x, max(0, min(31, y))] = colour + (255,)
        for y, colour in ((4, (250, 220, 70)), (12, (90, 200, 240)), (21, (150, 230, 120))):
            rect(img, 24, y, 30, y + 4, shade(colour, 0.5))
            rect(img, 25, y + 1, 29, y + 3, colour)
        rect(img, 0, 29, 32, 32, (40, 60, 80))
        return img
    return f


hc.animation(C, "anaesthesia_screen", [_screen_frame(i) for i in range(4)], 6)


@C.texture("bellows")
def _bellows():
    """The ventilator bellows in its clear housing: pale blue folds."""
    img = fill((206, 222, 230), size=16, grain=1, seed=9105)
    for y in range(1, 16, 3):
        rect(img, 0, y, 16, y + 1, (150, 176, 194))
    return img


@C.texture("cabinet_glass")
def _cabinet_glass():
    """The supply cabinet's glazed door: a steel frame, glass, and two shelves of sterile packs
    (blue wraps, white peel pouches, green boxes) behind it."""
    img = fill((198, 214, 222), size=16, grain=1, seed=9106)
    rect(img, 0, 0, 16, 1, (186, 190, 194))
    rect(img, 0, 15, 16, 16, (186, 190, 194))
    rect(img, 0, 0, 1, 16, (186, 190, 194))
    rect(img, 15, 0, 16, 16, (186, 190, 194))
    rng = random.Random(9107)
    for shelf in (7, 14):
        rect(img, 1, shelf, 15, shelf + 1, (150, 156, 160))
        x = 2
        while x < 14:
            w = rng.choice((2, 3))
            colour = rng.choice(((92, 146, 190), (236, 236, 230), (110, 170, 120)))
            h = rng.choice((3, 4, 5))
            rect(img, x, shelf - h, min(14, x + w), shelf, colour)
            x += w + 1
    rect(img, 13, 6, 14, 10, (120, 126, 132))
    return img


# ==========================================================================================
# Block helpers
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


def prop(reg, box6, mat, collides, names, textures, elements, display=None):
    facing_block(reg, java_for("BlockHealthcare", reg, box6, mat, "FLOOR", collides),
                 names, textures, elements, CL, display=display)


def pair_block(reg, names, whole, axis, textures, java, item_display=None):
    """As the wards': first cell 0..16, second -16..0 (wide) or 16..32 (tall)."""
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    if axis == 0:
        first, second = cut(whole, 0, 0, 32, origin=0), cut(whole, 0, -32, 0, origin=-16)
    else:
        first, second = cut(whole, 1, -16, 16, origin=0), cut(whole, 1, 16, 48, origin=16)
    # the item is the two cut halves put back together, so no face is longer than its texture
    item = (shift(first + shift(second, dx=-16), dx=8) if axis == 0
            else first + shift(second, dy=16))
    models = {reg + "_first": model(textures, first),
              reg + "_second": model(textures, second),
              reg + "_item": model(textures, item, display=item_display or ITEM_SMALL)}
    state = hc.facing_state(M(reg + "_first"), {
        "second": {"false": {}, "true": {"model": M(reg + "_second")}}})
    state["variants"]["inventory"] = [{"model": M(reg + "_item")}]
    C.add(reg, java, names, models, state, tab=CL)


def pair_java(reg, box6, mat, shape, collides=True):
    extra = "" if collides else ", false"
    return ('new BlockHealthcarePair("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.FLOOR, '
            'BlockHealthcarePair.Shape.%s%s)' % (reg, B(*box6), mat, shape, extra))


def run_block(reg, java, names, textures, body, end, display=ITEM_SMALL):
    """A run piece: the body in every cell, the east end (model +x, the viewer's left) where
    the run stops on that side, and the mirrored west end."""
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    models = {reg + "_body": model(textures, body),
              reg + "_end_east": model(textures, end),
              reg + "_end_west": model(textures, mirror_x(end)),
              reg + "_item": model(textures, body + end + mirror_x(end), display=display)}
    parts = []
    for f, y in hc.ROT_Y.items():
        for name, when in ((reg + "_body", {}), (reg + "_end_east", {"east": "false"}),
                           (reg + "_end_west", {"west": "false"})):
            w = {"facing": f}
            w.update(when)
            parts.append({"when": w, "apply": {"model": M(name), "y": y}})
    C.add(reg, java, names, models, {"multipart": parts}, item={"parent": MI(reg + "_item")},
          tab=CL)


# ==========================================================================================
# The operating table: two cells, the head (with its headrest) the first, a column on a base
# plate under the middle, the top in four pad sections on a steel frame with side rails, an arm
# board out to the front at the shoulders
# ==========================================================================================
PAD_TOP = 12
OR_TABLE = ([el([-7, 0, 3.5], [7, 1.2, 12.5], "dark"),
             el([-3, 1.2, 5.5], [3, 8.6, 10.5], "body"),
             el([-4, 8.6, 4.5], [4, 9.2, 11.5], "grey"),
             el([-15.5, 9.2, 3], [15.5, 10, 13], "metal"),
             el([-15.5, 9.4, 2.5], [15.5, 10, 3], "metal"),
             el([-15.5, 9.4, 13], [15.5, 10, 13.5], "metal")]
            + [el([x0, 10, 3.2], [x1, PAD_TOP, 12.8], "pad")
               for x0, x1 in ((-15.4, -6.1), (-5.9, 3.9), (4.1, 11.4))]
            + [el([11.6, 10, 4.5], [15.4, PAD_TOP - 0.4, 11.5], "pad"),
               # the arm board at the shoulders, out to the front
               el([5.2, 9.6, -3.5], [8.8, 10.2, 2.5], "metal"),
               el([5.4, 10.2, -3.3], [8.6, 11.2, 2.4], "pad"),
               # the control pendant hung on the far rail
               el([-2, 7, 13.5], [0, 9.4, 14.2], "grey")])
pair_block("or_table",
           ("Operating Table", "Operationstisch", "Mesa de operaciones", "Operationsbord"),
           OR_TABLE, 0,
           {"pad": T("or_pad"), "metal": STEEL, "grey": GREY, "body": WHITE, "dark": DARK,
            "particle": WHITE},
           pair_java("or_table", (0, 0, 2, 16, 12, 14), "METAL", "WIDE")
           + ".withSeat(%g, 0)" % PAD_TOP)

# ==========================================================================================
# The anaesthesia machine: a drawer cart on casters, the work shelf, the gas panel with its
# flowmeters, two vaporizers, the bellows and the absorber, gas cylinders on the back, the
# breathing circuit, and the workstation screen on top, switched by a click
# ==========================================================================================
ANAESTHESIA = (casters([(2.5, 4.5), (13.5, 4.5), (2.5, 14.5), (13.5, 14.5)])
               + [front([1.5, 0.8, 4], [14.5, 9, 15.5], "body", "drawers"),
                  el([1, 9, 2.5], [15, 10, 15.5], "metal"),
                  front([2, 10, 6], [14, 21, 14.5], "body", "panel"),
                  # vaporizers on the back bar, in their agents' colours
                  el([3, 21, 7], [6.5, 24.5, 11], "vap_a"),
                  el([7.5, 21, 7], [11, 24.5, 11], "vap_b"),
                  el([2, 21, 11], [14, 21.6, 14.5], "grey"),
                  # the absorber canister and the bellows in its clear housing
                  el([11.5, 10, 3], [14.5, 15, 6], "absorber")]
               + octo(4.5, 4.5, 1.8, 10, 16, "bellows")
               + octo(3.5, 15.5, 1.1, 1, 13, "cyl")
               + octo(12.5, 15.5, 1.1, 1, 13, "cyl")
               + [el([7.4, 21.6, 12], [8.6, 26, 13.2], "metal"),
                  el([2, 25, 9.5], [14, 32, 12], "mbody"),
                  screen([2.6, 25.6, 9.45], [13.4, 31.4, 9.45], "screen"),
                  # the breathing circuit off the front, looping down
                  el([6, 10.5, 2.4], [7, 15.5, 3.4], "hose"),
                  el([9, 10.5, 2.4], [10, 15.5, 3.4], "hose"),
                  el([6, 9.5, 2.4], [10, 10.5, 3.4], "hose")])
facing_block("or_anaesthesia_machine",
             java_for("BlockHealthcareScreen", "or_anaesthesia_machine", (1, 0, 2, 15, 16, 16),
                      "METAL", "FLOOR", True),
             ("Anaesthesia Machine", "Narkosegerät", "Máquina de anestesia", "Anestesiapparat"),
             {"body": WHITE, "drawers": K("med_drawers"), "panel": T("anaesthesia_front"),
              "metal": STEEL, "grey": GREY, "rubber": RUBBER, "vap_a": W("yellow"),
              "vap_b": K("cart_blue"), "absorber": T("bellows"), "bellows": T("bellows"),
              "cyl": W("canister"), "mbody": W("monitor_body"), "screen": W("screen_off"),
              "hose": K("hose"), "particle": WHITE},
             ANAESTHESIA, CL, extra=hcl.screen_extra(T("anaesthesia_screen")), display=ITEM_TALL)

# ==========================================================================================
# The surgical light: the drop column's own tube, a hub, two arms out either side and a lamp
# head at the end of each, round housings with their lenses facing down, a sterile handle in
# each. Hung as the trauma bay light is (BlockCeilingHeadLight, two above the floor by hand),
# its lenses lit by Lighting's states.
# ==========================================================================================
def lamp_head(cx, cz):
    return (octo(cx, cz, 5.2, 4, 6.8, "housing")
            + octo(cx, cz, 1.6, 6.8, 9.6, "grey", bottom=False)
            + [el([cx - 3.6, 3.95, cz - 3.6], [cx + 3.6, 4, cz + 3.6], "lens", faces=("down",),
                  uv={"down": FULL}),
               el([cx - 0.6, 2, cz - 0.6], [cx + 0.6, 3.95, cz + 0.6], "grey")])


SURGICAL_LIGHT = ([el([6, 12, 6], [10, 16, 10], "metal", faces=hcl.SIDES4),
                   el([5.5, 10.4, 5.5], [10.5, 12, 10.5], "grey"),
                   # the arm, in two so no face is longer than its texture
                   el([-6.5, 10.6, 7.3], [8, 11.6, 8.7], "metal"),
                   el([8, 10.6, 7.3], [22.5, 11.6, 8.7], "metal"),
                   el([-6.3, 9.6, 7.5], [-4.7, 10.6, 8.5], "metal"),
                   el([20.7, 9.6, 7.5], [22.3, 10.6, 8.5], "metal")]
                  + safe_uv(lamp_head(-5.5, 8) + lamp_head(21.5, 8)))
C.add("or_surgical_light",
      'new BlockCeilingHeadLight("or_surgical_light", %s, 15, 2)' % B(2, 2, 2, 14, 16, 14),
      ("Surgical Light (Twin Head)", "OP-Leuchte (Doppelkopf)",
       "Lámpara quirúrgica (doble cúpula)", "Operationslampa (dubbel)"),
      {"or_surgical_light": model(
          {"housing": WHITE, "metal": STEEL, "grey": GREY, "lens": K("exam_lens_off"),
           "particle": WHITE},
          SURGICAL_LIGHT, display=hw.ITEM_SMALL)},
      hw.light_state(M("or_surgical_light"), {"lens": K("exam_lens_on")}), tab=CL)

# ==========================================================================================
# The scrub sink: a deep stainless trough with a tall splashback, one bay a block with its own
# gooseneck tap and knee panel, bays side by side joining into one trough; legs and side panels
# only where the run stops. It stands against a wall.
# ==========================================================================================
SINK_BODY = ([el([0, 14, 5], [16, 14.6, 6], "metal"),                       # the front rim
              el([0, 8, 5], [16, 14, 5.6], "metal", faces=("north", "south", "down")),
              el([0, 7.4, 5.6], [16, 8, 15], "metal"),                     # the trough's floor
              el([0, 8, 6], [16, 8.2, 14.5], "dark", faces=("up",)),
              # the splashback, in two so no face is taller than its texture
              el([0, 8, 14.5], [16, 16, 16], "metal", faces=("north", "south", "down")),
              el([0, 16, 14.5], [16, 30, 16], "metal", faces=("north", "south")),
              el([0, 30, 13.5], [16, 30.6, 16], "metal"),
              front([5, 2, 7], [11, 6, 8], "metal", "knee"),               # the knee panel
              el([7.4, 6, 7.4], [8.6, 7.4, 8.6], "grey"),
              # the gooseneck tap and its sensor
              el([7.4, 16, 13.4], [8.6, 24, 14.5], "metal"),
              el([7.4, 22.8, 9], [8.6, 24, 13.4], "metal"),
              el([7.6, 20.5, 9.2], [8.4, 22.8, 10], "metal"),
              el([6.5, 25, 14.3], [9.5, 26.5, 14.5], "sensor", faces=("north",))])
SINK_END = ([el([15.4, 7.4, 5], [16, 14.6, 16], "metal"),
             el([15.4, 14.6, 13.5], [16, 30.6, 16], "metal")]
            + [el([14, 0, z0], [15.4, 7.4, z0 + 1.4], "grey") for z0 in (6, 13.5)])
run_block("or_scrub_sink",
          'new BlockHealthcareRun("or_scrub_sink", %s, HealthcareMaterial.METAL, true, '
          '"or_scrub_sink")' % B(0, 0, 5, 16, 16, 16),
          ("Surgical Scrub Sink", "Chirurgisches Waschbecken", "Lavabo quirúrgico",
           "Kirurgisk tvättränna"),
          {"metal": STEEL, "grey": GREY, "dark": DARK, "knee": GREY,
           "sensor": W("charcoal"), "particle": STEEL},
          SINK_BODY, SINK_END, display=ITEM_TALL)

# ==========================================================================================
# The instrument back table: two cells of stainless table on casters, draped, with the set laid
# out on it, and a lower shelf
# ==========================================================================================
BACK_TABLE = ([el([-15.5, 13, 2.5], [15.5, 14, 13.5], "metal"),
               el([-15.7, 14, 2.3], [15.7, 14.2, 13.7], "drape"),
               el([-15.7, 9.5, 2.3], [15.7, 14, 2.5], "drape", faces=("north", "south")),
               el([-15.7, 9.5, 13.5], [15.7, 14, 13.7], "drape", faces=("north", "south")),
               el([-15.5, 4.5, 3], [15.5, 5.2, 13], "metal"),
               plane([0.5, 14.25, 3], [15, 14.25, 13], "set", "up"),
               plane([-15, 14.25, 3], [-0.5, 14.25, 13], "set", "up")]
              + [el([x - 0.5, 0.8, z - 0.5], [x + 0.5, 13, z + 0.5], "metal")
                 for x in (-15, 15) for z in (3.2, 12.8)]
              + casters([(-15, 3.2), (15, 3.2), (-15, 12.8), (15, 12.8)]))
pair_block("or_back_table",
           ("Instrument Back Table", "Instrumententisch", "Mesa auxiliar de instrumental",
            "Instrumentbord"),
           BACK_TABLE, 0,
           {"metal": STEEL, "drape": T("drape"), "set": T("instruments"), "rubber": RUBBER,
            "particle": STEEL},
           pair_java("or_back_table", (0, 0, 2, 16, 14, 14), "METAL", "WIDE"))

# ==========================================================================================
# The Mayo stand: a U base on casters open to the front, so it rolls over the table, a column
# at the back and the tray reaching forward, draped and set
# ==========================================================================================
MAYO = ([el([2.5, 0.8, 2], [3.7, 1.6, 14], "metal"), el([12.3, 0.8, 2], [13.5, 1.6, 14], "metal"),
         el([3.7, 0.8, 12.8], [12.3, 1.6, 14], "metal")]
        + casters([(3.1, 2.5), (12.9, 2.5), (3.1, 13.5), (12.9, 13.5)])
        + octo(8, 13.4, 0.9, 1.6, 15, "metal")
        + [el([7.2, 14.4, 2], [8.8, 15, 13.4], "metal"),
           el([2, 15, 1], [14, 15.6, 12], "metal"),
           el([1.8, 15.6, 0.8], [14.2, 15.8, 12.2], "drape"),
           el([1.8, 12.5, 0.6], [14.2, 15.8, 0.8], "drape", faces=("north", "south")),
           plane([2.2, 15.85, 1.2], [13.8, 15.85, 11.8], "set", "up")])
prop("or_mayo_stand", (2, 0, 1, 14, 16, 14), "METAL", False,
     ("Mayo Stand (Instrument Tray)", "Mayo-Instrumententisch", "Mesa de Mayo", "Mayobord"),
     {"metal": STEEL, "drape": T("drape"), "set": T("instruments"), "rubber": RUBBER,
      "particle": STEEL},
     MAYO, display=ITEM_TALL)

# ==========================================================================================
# The kick bucket: a stainless bucket in a ring frame on casters, low enough to push by foot
# ==========================================================================================
KICK = (casters([(4.5, 4.5), (11.5, 4.5), (4.5, 11.5), (11.5, 11.5)])
        + [el([4, 0.8, 7.4], [12, 1.4, 8.6], "grey"), el([7.4, 0.8, 4], [8.6, 1.4, 12], "grey")]
        + octo(8, 8, 3.6, 1.4, 7.5, "metal", top=False)
        + [el([5.2, 1.6, 5.2], [10.8, 1.65, 10.8], "dark", faces=("up",))]
        + octo(8, 8, 4.1, 6.6, 7.4, "grey", top=False, bottom=False))
prop("or_kick_bucket", (4, 0, 4, 12, 8, 12), "METAL", True,
     ("Kick Bucket", "Abwurfeimer (OP)", "Cubo rodante de quirófano", "Rullhink (operation)"),
     {"metal": STEEL, "grey": GREY, "dark": DARK, "rubber": RUBBER, "particle": STEEL},
     KICK)

# ==========================================================================================
# The supply cabinet: two tall, stainless, drawers below and a glazed door above showing the
# sterile packs on its shelves
# ==========================================================================================
CABINET = ([el([0.5, 0, 2.5], [15.5, 1, 15.5], "dark"),
            front([0.5, 1, 2], [15.5, 16, 16], "metal", "drawers"),
            front([0.5, 16, 2], [15.5, 31, 16], "metal", "glass"),
            el([0.3, 31, 1.8], [15.7, 32, 16], "metal")])
pair_block("or_supply_cabinet",
           ("OR Supply Cabinet", "OP-Materialschrank", "Armario de suministros de quirófano",
            "Materialskåp (operation)"),
           CABINET, 1,
           {"metal": STEEL, "dark": DARK, "drawers": K("med_drawers"),
            "glass": T("cabinet_glass"), "particle": STEEL},
           pair_java("or_supply_cabinet", (0, 0, 2, 16, 16, 16), "METAL", "TALL"),
           item_display=ITEM_TALL)

# ==========================================================================================
# The sterile drape trolley: a wire cart with three shelves of folded drapes and gowns
# ==========================================================================================
TROLLEY = (casters([(2.5, 3), (13.5, 3), (2.5, 13), (13.5, 13)])
           + [el([2, 0.8 + y, 2.5], [14, 1.4 + y, 13.5], "wire") for y in (1, 7, 13)]
           + [el([x - 0.4, 0.8, z - 0.4], [x + 0.4, 15.5, z + 0.4], "metal")
              for x in (2.4, 13.6) for z in (2.9, 13.1)]
           + [el([3, 2.4, 3.5], [13, 6.5, 12.5], "drape"),
              el([3, 8.4, 3.5], [8, 11.5, 12.5], "drape"),
              el([8.5, 8.4, 3.5], [13, 11, 12.5], "linen"),
              el([3, 14.4, 4], [13, 16, 12], "drape")])
prop("or_drape_trolley", (2, 0, 2, 14, 16, 14), "METAL", True,
     ("Sterile Drape Trolley", "Sterilgutwagen (Abdecktücher)", "Carro de paños estériles",
      "Vagn för sterila draperingar"),
     {"metal": STEEL, "wire": W("wire_shelf"), "drape": T("drape"), "linen": W("linen_stack"),
      "rubber": RUBBER, "particle": STEEL},
     TROLLEY)


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
