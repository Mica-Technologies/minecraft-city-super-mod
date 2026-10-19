#!/usr/bin/env python3
"""
gen_healthcare_wards.py -- the Healthcare module's patient rooms and wards.

What a hospital ward and its rooms are fitted with: the hospital bed (four blanket colours, the
ICU bed with its air mattress and pump, the wider bariatric bed, the labour and delivery bed
with its split foot section and leg supports), the patient headwall and its
upper panel, the reading light, the bedside monitor on its wall arm and on a rolling stand, the
IV pole and the IV pole with its infusion pump, the overbed table, the patient recliner and
visitor chair, the wardrobe, the patient-room TV, the wall vitals unit and suction regulator,
cubicle curtains (the ceiling track, three cloth patterns and the mesh band, in panels that
stack and join), the nurse call devices, dome light and master panel, the corridor handrail and
crash rail, and the clinical side's wall dispensers, carts and bins, the corridor oxygen outlet,
and the medical gas zone sign. (The eyewash station is Life Safety's, in Emergency
Services.)

Nothing here is a real product: no maker's name, no red cross (a protected emblem). Screens are
animated textures (.mcmeta), so a monitor's trace moves with nothing ticking.

Writes, under modules/healthcare/src/main/resources/assets/csm:

  * textures/blocks/healthcare/wards/*.png (+ .mcmeta for the screens)
  * models/block/healthcare/wards/*.json
  * blockstates/<registry>.json, and models/item/<registry>.json for the multipart blocks
  * the tile names and the module's messages in all four lang files

Every model faces north with what it stands against at +Z. Multi-cell pieces (the bed, the
headwall, the wardrobe) are drawn whole in one frame and cut at the block lines into one model a
cell; the cut and the Java agree on which cell is which (BlockHospitalBed, BlockHealthcarePair).

Usage:
    python gen_healthcare_wards.py              # write everything
    python gen_healthcare_wards.py --check      # fail if the tree has drifted
    python gen_healthcare_wards.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import copy
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_healthcare_common as hc  # noqa: E402
import gen_park_plantings as pp  # noqa: E402
from gen_healthcare_common import (  # noqa: E402
    bevel, clamp, disc, draw_text, draw_text_centred, fill, frame, rect, shade, text_width)

C = hc.catalogue("gen_healthcare_wards.py", "wards")
T = C.T
M = C.M


def MI(name):
    """An item model's parent: item models resolve from models/, so the block/ is needed."""
    return "csm:block/%s/%s" % (C.model_dir, name)
PC = hc.PATIENT_CARE
CL = hc.CLINICAL
HID = hc.HIDDEN

ALL = ("north", "south", "east", "west", "up", "down")


def B(*v):
    return "new int[]{%s}" % ", ".join("%d" % int(round(x)) for x in v)


# ==========================================================================================
# Elements
# ==========================================================================================
# Texture keys that are a device's printed front: drawn to fill the texture, so the north face
# shows the whole of it rather than the window a fitted UV would cut out of it.
FRONTS = ("front", "plate", "face", "boxes", "gauge", "pump")


def el(frm, to, tex, faces=ALL, per=None, uv=None):
    """A box with UVs fitted to its size; one reaching past the cell keeps its UV size."""
    outside = any(v < 0 or v > 16 for v in list(frm) + list(to))
    e = pp.wbox(frm, to, tex, faces, per) if outside else hc.box(frm, to, tex, faces, per)
    uv = dict(uv or {})
    if (per or {}).get("north") in FRONTS and "north" not in uv:
        uv["north"] = [0, 0, 16, 16]
    for f, u in uv.items():
        if f in e["faces"]:
            e["faces"][f]["uv"] = [round(v, 3) for v in u]
    return e


def screen(frm, to, tex):
    """A screen face: one north face showing the whole texture."""
    return el(frm, to, tex, faces=("north",), uv={"north": [0, 0, 16, 16]})


def rot(e, axis, angle, origin):
    e = copy.deepcopy(e)
    e["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}
    return e


def octo(cx, cz, r, y0, y1, tex, top=True, bottom=True):
    return hc.post(cx, cz, r, y0, y1, tex, top=top, bottom=bottom)


def mirror_x(elements):
    """The same elements reflected across x = 8 (a model's west version of its east arm)."""
    out = []
    for e in elements:
        e = copy.deepcopy(e)
        x0, x1 = 16 - e["to"][0], 16 - e["from"][0]
        e["from"][0], e["to"][0] = round(x0, 3), round(x1, 3)
        faces = e["faces"]
        if "east" in faces or "west" in faces:
            east, west = faces.pop("east", None), faces.pop("west", None)
            if east is not None:
                faces["west"] = east
            if west is not None:
                faces["east"] = west
        for f, fc in faces.items():
            u = fc.get("uv")
            if u and f in ("north", "south", "up", "down"):
                fc["uv"] = [u[2], u[1], u[0], u[3]]
        if "rotation" in e:
            r = e["rotation"]
            r["origin"][0] = round(16 - r["origin"][0], 3)
            if r["axis"] in ("y", "z"):
                r["angle"] = -r["angle"]
        out.append(e)
    return out


def shift(elements, dx=0, dy=0, dz=0):
    out = []
    for e in elements:
        e = copy.deepcopy(e)
        e["from"] = [round(e["from"][0] + dx, 3), round(e["from"][1] + dy, 3),
                     round(e["from"][2] + dz, 3)]
        e["to"] = [round(e["to"][0] + dx, 3), round(e["to"][1] + dy, 3),
                   round(e["to"][2] + dz, 3)]
        if "rotation" in e:
            o = e["rotation"]["origin"]
            e["rotation"]["origin"] = [round(o[0] + dx, 3), round(o[1] + dy, 3),
                                       round(o[2] + dz, 3)]
        out.append(e)
    return out


def refit(e):
    """Re-fits an element's standard UVs after a cut or a shift (explicit full-texture UVs on
    screens are kept)."""
    if "rotation" in e:
        return e
    fresh = el(e["from"], e["to"], "x", tuple(e["faces"].keys()))
    for f, fc in e["faces"].items():
        if fc.get("uv") != [0, 0, 16, 16]:
            fc["uv"] = fresh["faces"][f]["uv"]
    return e


def cut(elements, axis, lo, hi, origin=None):
    """The parts of ``elements`` inside lo..hi along ``axis`` (0 x, 1 y, 2 z), moved so
    ``origin`` (default lo) is 0. A rotated element goes whole to the cell holding its origin.
    Pass bounds past the cell on a piece's outside, so what stands proud of it is kept."""
    origin = lo if origin is None else origin
    out = []
    for e in elements:
        if "rotation" in e:
            o = e["rotation"]["origin"][axis]
            if lo <= o < hi:
                d = [0, 0, 0]
                d[axis] = -origin
                out += shift([e], *d)
            continue
        a, b = e["from"][axis], e["to"][axis]
        if b <= lo or a >= hi:
            if not (a == b and lo <= a < hi):
                continue
        e = copy.deepcopy(e)
        e["from"][axis] = max(a, lo)
        e["to"][axis] = min(b, hi)
        faces = e["faces"]
        lo_face, hi_face = {0: ("west", "east"), 1: ("down", "up"), 2: ("north", "south")}[axis]
        if a < lo:
            faces.pop(lo_face, None)
        if b > hi:
            faces.pop(hi_face, None)
        if not faces:
            continue
        d = [0, 0, 0]
        d[axis] = -origin
        out.append(refit(shift([e], *d)[0]))
    return out


def model(textures, elements, display=None, ao=True):
    m = hc.model(textures, elements, ao=ao)
    if display:
        m["display"] = display
    return m


ITEM_SMALL = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.42] * 3},
              "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2] * 3},
              "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.4] * 3},
              "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                        "scale": [0.3] * 3},
              "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                        "scale": [0.3] * 3}}
ITEM_TALL = {"gui": {"rotation": [30, 225, 0], "translation": [0, -2.5, 0],
                     "scale": [0.42] * 3},
             "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2] * 3},
             "fixed": {"rotation": [0, 0, 0], "translation": [0, -3, 0], "scale": [0.4] * 3},
             "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                       "scale": [0.3] * 3},
             "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                       "scale": [0.3] * 3}}


# ==========================================================================================
# Textures
# ==========================================================================================
def tex(name, fn):
    C.textures[name] = fn


for i, (n, c, g) in enumerate((
        ("white", hc.HOSPITAL_WHITE, 2), ("warm_white", hc.WARM_WHITE, 3),
        ("frame_grey", hc.FRAME_GREY, 3), ("dark_grey", hc.DARK_GREY, 3),
        ("charcoal", hc.CHARCOAL, 2), ("vinyl_blue", hc.VINYL_BLUE, 3),
        ("vinyl_teal", hc.VINYL_TEAL, 3), ("screen_off", hc.SCREEN_BLACK, 1),
        ("red", hc.BIOHAZARD_RED, 3), ("yellow", hc.SAFETY_YELLOW, 3),
        ("rubber", (30, 30, 32), 2),
        ("mattress", (88, 104, 130), 3), ("pillow", (240, 240, 236), 2),
        ("bag_blue", (60, 96, 160), 4), ("cord", (226, 226, 220), 1))):
    tex(n, (lambda c=c, g=g, s=1000 + i: hc.swatch(c, s, grain=g)))

tex("stainless", lambda: hc.brushed(hc.STAINLESS, 1101))
tex("beech", lambda: pp_wood())


def pp_wood():
    rng = random.Random(1102)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rows = [1.0 + rng.uniform(-0.06, 0.06) for _ in range(16)]
    for y in range(16):
        for x in range(16):
            k = rows[y] * (0.88 if y % 5 == 0 else 1.0) * (1 + rng.uniform(-0.02, 0.02))
            px[x, y] = shade(hc.BEECH, k) + (255,)
    return img


BLANKETS = {"white": ((236, 236, 230), None), "blue": ((96, 140, 190), "check"),
            "green": ((110, 160, 128), "check"), "mauve": ((168, 128, 160), "check")}
for i, (n, (c, pat)) in enumerate(BLANKETS.items()):
    tex("blanket_" + n, (lambda c=c, pat=pat, s=1200 + i: hc.fabric(c, s, pattern=pat)))


@C.texture("air_mattress")
def _air_mattress():
    img = fill((206, 212, 220), grain=2, seed=1210)
    for y in range(0, 16, 3):
        rect(img, 0, y, 16, y + 1, (168, 176, 188))
    return img


@C.texture("bed_rail")
def _bed_rail():
    """A side rail: a frame with three openings (cutout)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    c = (214, 216, 212)
    rect(img, 0, 0, 16, 2, c)
    rect(img, 0, 14, 16, 16, c)
    for x in (0, 5, 10, 15):
        rect(img, x, 0, x + 1, 16, c)
    rect(img, 0, 2, 16, 3, shade(c, 0.8))
    return img


@C.texture("footboard")
def _footboard():
    img = fill(hc.WARM_WHITE, grain=2, seed=1211)
    frame(img, 0, 0, 16, 16, shade(hc.WARM_WHITE, 0.82))
    # the bed controls on the foot end, a small grey keypad with a lit screen
    rect(img, 5, 5, 11, 10, (70, 74, 80))
    rect(img, 6, 6, 10, 7, (120, 190, 220))
    for x in (6, 8):
        rect(img, x, 8, x + 1, 9, (200, 200, 196))
    return img


@C.texture("headboard")
def _headboard():
    img = fill(hc.WARM_WHITE, grain=2, seed=1212)
    frame(img, 0, 0, 16, 16, shade(hc.WARM_WHITE, 0.82))
    rect(img, 2, 3, 14, 4, hc.PALE_BLUE)
    return img


@C.texture("bed_pump")
def _bed_pump():
    img = fill((60, 64, 70), grain=2, seed=1213)
    rect(img, 2, 2, 14, 8, (24, 30, 34))
    draw_text(img, "AIR", 3, 3, (110, 220, 140))
    for x in (3, 7, 11):
        rect(img, x, 11, x + 2, 13, (190, 190, 186))
    return img


# --- headwall ----------------------------------------------------------------------------
def headwall_face(seed, draw, fine=None):
    """A headwall face drawn at 32 px; ``fine`` then draws on it doubled to 64 px, for labels
    the 3 x 5 font would make too big at 32."""
    def f():
        img = fill(hc.WARM_WHITE, size=32, grain=2, seed=seed)
        # the coloured band through the headwall at outlet height
        rect(img, 0, 22, 32, 25, hc.PALE_BLUE)
        draw(img)
        if fine:
            img = img.resize((64, 64), Image.NEAREST)
            fine(img)
        return img
    return f


# the gas outlets' centres across the left panel's texture (32 px), matching the outlet elements
# headwall_elements() stands proud of it (x = 11.4, 6.9, 2.4; u = 16 - x, two texels a unit)
HW_GAS = ((hc.O2_GREEN, "O2"), (hc.AIR_YELLOW, "AIR"), ((250, 250, 250), "VAC"))
HW_GAS_U = (9, 18, 27)


def _hw_left(img):
    # gas outlet plates: oxygen, medical air, vacuum; the outlet stands on the plate's lower half
    for (c, _label), u in zip(HW_GAS, HW_GAS_U):
        rect(img, u - 4, 4, u + 4, 18, (232, 232, 228))
        frame(img, u - 4, 4, u + 4, 18, (170, 170, 166))
        rect(img, u - 3, 5, u + 3, 8, c)


def _hw_left_labels(img):
    for (_c, label), u in zip(HW_GAS, HW_GAS_U):
        draw_text_centred(img, label, u * 2, 18, (60, 60, 64))


def _hw_right(img):
    # two duplex power outlets, a data jack and the nurse call jack
    for x in (3, 11):
        rect(img, x, 6, x + 6, 16, (236, 236, 232))
        frame(img, x, 6, x + 6, 16, (170, 170, 166))
        for y in (8, 12):
            rect(img, x + 2, y, x + 3, y + 2, (40, 40, 40))
            rect(img, x + 4, y, x + 5, y + 2, (40, 40, 40))
    rect(img, 20, 6, 26, 12, (236, 236, 232))
    frame(img, 20, 6, 26, 12, (170, 170, 166))
    disc(img, 23, 9, 1.6, (40, 40, 40))
    rect(img, 20, 13, 26, 16, (230, 120, 40))


def _hw_upper(img):
    # an equipment channel and two spare gas outlets
    rect(img, 0, 6, 32, 9, (170, 176, 182))
    rect(img, 0, 8, 32, 9, (130, 136, 142))


tex("headwall_left", headwall_face(1301, _hw_left, _hw_left_labels))
tex("headwall_right", headwall_face(1302, _hw_right))
tex("headwall_upper", headwall_face(1303, _hw_upper))


@C.texture("gas_o2")
def _gas_o2():
    img = fill((232, 232, 228), grain=1, seed=1304)
    disc(img, 8, 8, 6, hc.O2_GREEN)
    disc(img, 8, 8, 3, (60, 60, 60))
    return img


@C.texture("gas_air")
def _gas_air():
    img = fill((232, 232, 228), grain=1, seed=1305)
    disc(img, 8, 8, 6, hc.AIR_YELLOW)
    disc(img, 8, 8, 3, (60, 60, 60))
    return img


@C.texture("gas_vac")
def _gas_vac():
    img = fill((232, 232, 228), grain=1, seed=1306)
    disc(img, 8, 8, 6, (250, 250, 250))
    disc(img, 8, 8, 5, (60, 60, 60))
    disc(img, 8, 8, 3, (250, 250, 250))
    return img


# --- lights ------------------------------------------------------------------------------
@C.texture("lens_off")
def _lens_off():
    return fill((200, 198, 186), grain=2, seed=1401)


@C.texture("lens_on")
def _lens_on():
    img = fill((255, 250, 222), grain=1, seed=1402)
    disc(img, 8, 8, 5, (255, 255, 246))
    return img


@C.texture("dome_white")
def _dome_white():
    img = fill((214, 214, 206), grain=2, seed=1403)
    for x in range(0, 16, 4):
        rect(img, x, 0, x + 1, 16, (190, 190, 182))
    return img


@C.texture("dome_amber")
def _dome_amber():
    img = fill(shade(hc.CALL_AMBER, 0.55), grain=2, seed=1404)
    for x in range(0, 16, 4):
        rect(img, x, 0, x + 1, 16, shade(hc.CALL_AMBER, 0.45))
    return img


@C.texture("dome_amber_on")
def _dome_amber_on():
    img = fill(hc.CALL_AMBER, grain=2, seed=1405)
    for x in range(0, 16, 4):
        rect(img, x, 0, x + 1, 16, shade(hc.CALL_AMBER, 1.1))
    disc(img, 8, 8, 4, (255, 236, 150))
    return img


# --- screens (animated) ------------------------------------------------------------------
ECG = [0, 0, 0, 0, 1, 0, 0, -1, 7, -3, 0, 0, 0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
       0, 0, 0, 0]
PLETH = [0, 1, 3, 4, 4, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0] * 2


def _trace(img, wave, y0, colour, offset, x0=1, x1=21):
    px = img.load()
    prev = None
    for x in range(x0, x1):
        v = wave[(x + offset) % len(wave)]
        y = y0 - v
        if prev is not None:
            for yy in range(min(prev, y), max(prev, y) + 1):
                px[x, yy] = colour + (255,)
        else:
            px[x, y] = colour + (255,)
        prev = y


def monitor_frame(i):
    def f():
        img = fill(hc.SCREEN_BLACK, size=32, grain=1, seed=1500)
        off = i * 4
        _trace(img, ECG, 9, (60, 230, 90), off)
        _trace(img, PLETH, 18, (60, 210, 230), off)
        _trace(img, [0, 0, 1, 1, 2, 2, 1, 1] * 4, 27, (240, 220, 60), off // 2)
        rect(img, 22, 0, 23, 32, (40, 44, 48))
        draw_text(img, "72", 24, 3, (60, 230, 90))
        draw_text(img, "98", 24, 13, (60, 210, 230))
        draw_text(img, "16", 24, 23, (240, 220, 60))
        if i % 4 < 2:
            rect(img, 30, 3, 31, 4, (60, 230, 90))
        return img
    return f


hc.animation(C, "monitor_screen", [monitor_frame(i) for i in range(8)], 3)


def pump_frame(i):
    def f():
        img = fill((18, 30, 44), size=32, grain=1, seed=1501)
        draw_text(img, "125", 3, 4, (230, 230, 230), scale=2)
        draw_text(img, "ML/H", 3, 16, (150, 200, 230))
        # the dripping drop indicator
        for k in range(4):
            c = (90, 200, 250) if k == i % 4 else (40, 70, 90)
            rect(img, 4 + k * 6, 24, 8 + k * 6, 28, c)
        return img
    return f


hc.animation(C, "pump_screen", [pump_frame(i) for i in range(4)], 10)


def tv_frame(i):
    def f():
        rng = random.Random(1510 + i)
        img = Image.new("RGBA", (32, 32))
        sky = [(110, 170, 220), (120, 180, 210), (230, 170, 110), (100, 150, 200)][i]
        ground = [(80, 140, 70), (210, 200, 160), (90, 90, 120), (70, 120, 160)][i]
        rect(img, 0, 0, 32, 18, sky)
        rect(img, 0, 18, 32, 32, ground)
        for _ in range(3):
            x = rng.randrange(0, 28)
            rect(img, x, rng.randrange(10, 20), x + rng.randrange(3, 7), 24,
                 shade(ground, rng.uniform(0.6, 1.3)))
        disc(img, 24, 6, 3, (250, 236, 170))
        return img
    return f


hc.animation(C, "tv_screen", [tv_frame(i) for i in range(4)], 60)


def vitals_frame(i):
    def f():
        img = fill(hc.SCREEN_BLACK, size=32, grain=1, seed=1520)
        draw_text(img, "BP", 2, 2, (200, 200, 200))
        draw_text(img, "118/76", 2, 9, (250, 250, 250))
        draw_text(img, "SPO2 97", 2, 17, (60, 210, 230))
        draw_text(img, "HR 74", 2, 25, (60, 230, 90) if i % 2 else (40, 150, 60))
        return img
    return f


hc.animation(C, "vitals_screen", [vitals_frame(i) for i in range(2)], 20)


@C.texture("monitor_body")
def _monitor_body():
    img = fill((70, 74, 80), grain=2, seed=1530)
    return img


# --- nurse call --------------------------------------------------------------------------
@C.texture("call_station")
def _call_station():
    img = fill((236, 236, 232), size=32, grain=1, seed=1601)
    hc.emblem(img, 16, 6, 3.5, (60, 110, 170))
    draw_text_centred(img, "CALL", 16, 12, (70, 70, 74))
    rect(img, 13, 19, 19, 25, (120, 124, 130))
    draw_text_centred(img, "OFF", 16, 26, (90, 90, 94))
    return img


@C.texture("call_button")
def _call_button():
    img = fill((206, 40, 36), grain=2, seed=1602)
    return img


@C.texture("call_lamp_off")
def _call_lamp_off():
    return fill(shade(hc.CALL_AMBER, 0.45), grain=1, seed=1603)


@C.texture("call_lamp_on")
def _call_lamp_on():
    img = fill(hc.CALL_AMBER, grain=1, seed=1604)
    disc(img, 8, 8, 4, (255, 236, 150))
    return img


@C.texture("pillow_speaker")
def _pillow_speaker():
    img = fill((210, 210, 204), size=16, grain=2, seed=1605)
    rect(img, 4, 2, 12, 6, (90, 94, 100))
    for y in range(3, 6, 2):
        for x in range(5, 12, 2):
            img.load()[x, y] = (40, 40, 44, 255)
    rect(img, 5, 8, 11, 11, (206, 40, 36))
    rect(img, 5, 12, 8, 14, (60, 110, 170))
    rect(img, 8, 12, 11, 14, (240, 240, 236))
    return img


@C.texture("pull_station")
def _pull_station():
    img = fill((236, 236, 232), size=32, grain=1, seed=1606)
    draw_text_centred(img, "PULL", 16, 17, (200, 40, 36), scale=2)
    return img


# the master panel's lamp rows on its face texture (32 px): each lamp's number sits under it
PANEL_ROWS = (10.5, 21.5)


@C.texture("panel_face")
def _panel_face():
    img = fill((52, 56, 62), size=32, grain=1, seed=1607)
    draw_text_centred(img, "NURSE", 17, 3, (220, 220, 220))
    for i in range(6):
        cx = 8 + (i % 3) * 8
        cy = PANEL_ROWS[i // 3]
        draw_text_centred(img, str(i + 1), cx, int(cy + 4), (200, 200, 200))
    return img


@C.texture("panel_lamp_off")
def _panel_lamp_off():
    return fill((90, 70, 40), grain=1, seed=1608)


@C.texture("panel_lamp_on")
def _panel_lamp_on():
    img = fill((255, 196, 60), grain=1, seed=1609)
    disc(img, 8, 8, 5, (255, 240, 170))
    return img


@C.texture("panel_alarm_off")
def _panel_alarm_off():
    return fill((80, 24, 22), grain=1, seed=1610)


@C.texture("panel_alarm_on")
def _panel_alarm_on():
    img = fill((240, 50, 40), grain=1, seed=1611)
    disc(img, 8, 8, 5, (255, 160, 140))
    return img


# --- curtains ----------------------------------------------------------------------------
def pleated(colour, seed, pattern):
    """A curtain cloth: the pattern with vertical pleat shading every four texels."""
    def f():
        img = hc.fabric(colour, seed, pattern=pattern)
        px = img.load()
        for y in range(16):
            for x in range(16):
                k = (0.9, 1.0, 1.06, 0.98)[x % 4]
                r, g, b, a = px[x, y]
                px[x, y] = shade((r, g, b), k) + (a,)
        return img
    return f


# The cloths, in BlockCurtainPanel.Pattern's order (stored in metadata by ordinal).
CURTAINS = [
    # (pattern, colour, print)
    ("mint", (168, 214, 196), None),
    ("blue_check", (120, 162, 206), "check"),
    ("beige", (216, 200, 168), "dots"),
    ("white", (238, 238, 232), None),
]
for i, (n, c, pat) in enumerate(CURTAINS):
    tex("curtain_" + n, pleated(c, 1700 + i, pat))
    tex("curtain_%s_hem" % n, (lambda c=c, s=1710 + i: hc.swatch(shade(c, 0.82), s, grain=2)))


@C.texture("curtain_mesh")
def _curtain_mesh():
    """The open mesh band at a curtain's top: a white net (cutout)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x % 2 == 0 or y % 2 == 0:
                px[x, y] = (240, 240, 236, 255)
    return img


@C.texture("curtain_tape")
def _curtain_tape():
    img = fill((244, 244, 240), grain=1, seed=1721)
    for x in range(1, 16, 4):
        rect(img, x, 0, x + 2, 16, (196, 198, 204))
    return img


@C.texture("ceiling_tile")
def _ceiling_tile():
    """A plain acoustic ceiling tile, finely pitted, with the grid's edge round it."""
    rng = random.Random(1722)
    img = fill((236, 236, 230), grain=3, seed=1723)
    px = img.load()
    for _ in range(26):
        px[rng.randrange(16), rng.randrange(16)] = (206, 206, 200, 255)
    frame(img, 0, 0, 16, 16, (214, 214, 210))
    return img


# --- dispensers, carts, bins --------------------------------------------------------------
@C.texture("sharps_front")
def _sharps_front():
    img = fill((196, 36, 36), size=32, grain=2, seed=1801)
    rect(img, 0, 0, 32, 7, (230, 210, 60))
    draw_text_centred(img, "SHARPS", 16, 14, (250, 230, 230))
    return img


@C.texture("glove_boxes")
def _glove_boxes():
    """Three glove boxes on their ends, side by side: each a box with a printed band, an oval
    opening and a glove's cuff showing through it (sizes S, M, L)."""
    img = fill((236, 236, 232), size=32, grain=1, seed=1802)
    for i, c in enumerate(((96, 130, 210), (150, 110, 200), (90, 180, 190))):
        x = 2 + i * 10
        rect(img, x, 4, x + 9, 28, (244, 244, 240))
        frame(img, x, 4, x + 9, 28, shade(c, 0.8))
        rect(img, x + 1, 5, x + 8, 9, c)
        rect(img, x + 1, 23, x + 8, 27, c)
        for cy in (14, 17):
            disc(img, x + 4.5, cy, 3.4, (80, 84, 90))
        for cy in (14, 17):
            disc(img, x + 4.5, cy, 2.4, shade(c, 1.45))
    return img


@C.texture("sanitizer_front")
def _sanitizer_front():
    img = fill((240, 240, 236), grain=1, seed=1803)
    rect(img, 4, 3, 12, 8, (120, 170, 220))
    rect(img, 5, 4, 11, 6, (180, 210, 240))
    hc.emblem(img, 8, 11.5, 2.2, (60, 110, 170), width=1.2)
    return img


@C.texture("isolation_drawers")
def _isolation_drawers():
    img = fill(hc.SAFETY_YELLOW, grain=3, seed=1804)
    for y in (0, 5, 10):
        frame(img, 0, y, 16, y + 5, shade(hc.SAFETY_YELLOW, 0.75))
        rect(img, 6, y + 2, 10, y + 3, (80, 80, 84))
    return img


@C.texture("gowns")
def _gowns():
    img = fill((240, 210, 120), grain=4, seed=1805)
    for y in range(0, 16, 3):
        rect(img, 0, y, 16, y + 1, (220, 190, 100))
    return img


def _biohazard(img, cx, cy, r, colour):
    for a in (90, 210, 330):
        t = math.radians(a)
        disc(img, cx + math.cos(t) * r * 0.45, cy - math.sin(t) * r * 0.45, r * 0.42, colour)
    disc(img, cx, cy, r * 0.22, (196, 36, 36))
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if abs(d - r * 0.62) < 0.6:
                px[x, y] = clamp(colour) + (255,)


@C.texture("biohazard_front")
def _biohazard_front():
    img = fill((196, 36, 36), size=32, grain=2, seed=1806)
    _biohazard(img, 16, 18, 9, (20, 20, 20))
    return img


@C.texture("linen_stack")
def _linen_stack():
    img = fill((240, 240, 236), grain=2, seed=1807)
    for y in range(0, 16, 2):
        rect(img, 0, y, 16, y + 1, (214, 218, 222))
    for y in range(4, 16, 8):
        rect(img, 0, y, 16, y + 1, (150, 190, 220))
    return img


@C.texture("wire_shelf")
def _wire_shelf():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(0, 16, 2):
        rect(img, x, 0, x + 1, 16, (196, 200, 204))
    rect(img, 0, 0, 16, 1, (196, 200, 204))
    rect(img, 0, 15, 16, 16, (196, 200, 204))
    return img


@C.texture("wardrobe_doors")
def _wardrobe_doors():
    img = pp_wood()
    rect(img, 7, 0, 8, 16, shade(hc.BEECH, 0.7))
    rect(img, 5, 6, 6, 10, (150, 154, 160))
    rect(img, 9, 6, 10, 10, (150, 154, 160))
    return img


@C.texture("wardrobe_doors_top")
def _wardrobe_doors_top():
    img = pp_wood()
    rect(img, 7, 0, 8, 16, shade(hc.BEECH, 0.7))
    return img


@C.texture("suction_gauge")
def _suction_gauge():
    img = fill((236, 236, 232), grain=1, seed=1808)
    disc(img, 8, 8, 6.5, (250, 250, 250))
    px = img.load()
    for k in range(9):
        t = math.radians(225 - k * 33.75)
        x = int(8 + math.cos(t) * 5)
        y = int(8 - math.sin(t) * 5)
        px[x, y] = (40, 40, 40, 255)
    for k in range(5):
        px[8 + k, 8 - k // 2] = (200, 40, 36, 255)
    disc(img, 8, 8, 1, (40, 40, 40))
    return img


@C.texture("canister")
def _canister():
    img = fill((200, 214, 226), grain=2, seed=1809)
    rect(img, 0, 9, 16, 16, (196, 160, 150))
    for y in range(2, 16, 3):
        rect(img, 12, y, 14, y + 1, (120, 130, 140))
    return img


@C.texture("gas_sign")
def _gas_sign():
    # drawn at about the sign's own 14 x 8 shape, then stretched square: the face stretches it
    # back
    img = fill((250, 250, 248), size=64, grain=1, seed=1810).resize((64, 32))
    frame(img, 1, 1, 63, 31, (40, 40, 44))
    draw_text_centred(img, "MEDICAL GAS", 32, 5, (30, 30, 34))
    draw_text_centred(img, "ZONE VALVES", 32, 13, (30, 30, 34))
    for i, c in enumerate((hc.O2_GREEN, hc.AIR_YELLOW, (210, 210, 210))):
        rect(img, 14 + i * 13, 22, 24 + i * 13, 26, c)
    return img.resize((64, 64), Image.NEAREST)


@C.texture("o2_plate")
def _o2_plate():
    img = fill((236, 236, 232), grain=1, seed=1811)
    rect(img, 0, 0, 16, 7, hc.O2_GREEN)
    draw_text_centred(img, "O2", 8, 1, (250, 250, 250))
    disc(img, 8, 10, 3.5, (60, 60, 60))
    disc(img, 8, 10, 1.5, (20, 20, 20))
    return img


@C.texture("tv_body")
def _tv_body():
    return fill((28, 28, 30), grain=1, seed=1813)


@C.texture("iv_bag")
def _iv_bag():
    img = fill((226, 234, 236), grain=2, seed=1814)
    rect(img, 0, 6, 16, 16, (196, 214, 222))
    rect(img, 3, 1, 13, 4, (250, 250, 250))
    return img


# ==========================================================================================
# The hospital bed
# ==========================================================================================
def bed_elements(rails, high, wide=False, icu=False, labor=False):
    """The whole bed in its frame: foot z 0..16, head z 16..32 (against the wall). The labour
    and delivery bed (``labor``) has its foot section split in two and dropped below the seat,
    no footboard, rails at the head only, and a padded leg support on a post each side."""
    x0, x1 = (0.25, 15.75) if wide else (1.5, 14.5)
    D = 10 if high else 6
    Mt = D + 3
    els = []
    # casters and the base frame
    for cx in (x0 + 1, x1 - 2.5):
        for cz in (2, 28.5):
            els.append(el([cx, 0, cz], [cx + 1.5, 1.5, cz + 1.5], "rubber"))
    for bx in (x0 + 1, x1 - 2):
        els.append(el([bx, 1.5, 2], [bx + 1, 3, 30], "frame_grey"))
    for bz in (2, 29):
        els.append(el([x0 + 1, 1.5, bz], [x1 - 1, 3, bz + 1], "frame_grey"))
    # the lift columns
    for cz in (7, 23):
        els.append(el([6.5, 3, cz], [9.5, D - 1, cz + 2], "stainless"))
    # the deck
    els.append(el([x0, D - 1, 1], [x1, D, 31], "frame_grey"))
    # the mattress: a flat seat and leg section, and the back raised
    if labor:
        # the seat, and the foot section split down the middle and dropped 2 px below it
        els.append(el([x0 + 0.5, D, 10.5], [x1 - 0.5, Mt, 20.5], "mattress",
                      per={"up": "blanket"}))
        for fx in ((x0 + 0.5, 7.6), (8.4, x1 - 0.5)):
            els.append(el([fx[0], D - 2, 2.5], [fx[1], Mt - 2, 10.2], "mattress"))
    else:
        els.append(el([x0 + 0.5, D, 1.5], [x1 - 0.5, Mt, 20.5], "mattress",
                      per={"up": "blanket"}))
    back = el([x0 + 0.5, D, 20.5], [x1 - 0.5, Mt, 30.5], "mattress")
    els.append(rot(back, "x", -22.5, [8, D, 20.5]))
    pillow = el([4, Mt, 24], [12, Mt + 2, 29.5], "pillow")
    els.append(rot(pillow, "x", -22.5, [8, D, 20.5]))
    # the blanket over the legs, its turned-down sheet, and the sides it drapes over
    b0 = 10.8 if labor else 1.2
    els.append(el([x0 + 0.2, Mt, b0], [x1 - 0.2, Mt + 0.6, 19], "blanket"))
    els.append(el([x0 + 0.2, Mt + 0.6, 16.5], [x1 - 0.2, Mt + 1.0, 19], "pillow"))
    for sx in ((x0 + 0.2 - 0.4, x0 + 0.2), (x1 - 0.2, x1 - 0.2 + 0.4)):
        els.append(el([sx[0], Mt - 2.5, b0], [sx[1], Mt + 0.6, 19], "blanket"))
    # head and foot boards
    els.append(el([x0, D - 2, 30.5], [x1, D + 8, 31.5], "warm_white",
                  per={"north": "headboard", "south": "headboard"}))
    if labor:
        # no footboard: a grab bar across the end of the frame, and a leg support each side,
        # a padded calf rest on a post rising from the frame beside the dropped foot section
        els.append(el([x0 + 1, D - 1.5, 1], [x1 - 1, D - 0.5, 2], "stainless"))
        for px, bx in ((x0 - 0.4, (x0 - 0.9, x0 + 2.1)), (x1 - 0.6, (x1 - 2.1, x1 + 0.9))):
            els.append(el([px, D - 1, 6], [px + 1, Mt + 6, 7], "stainless"))
            els.append(el([bx[0], Mt + 5, 3.5], [bx[1], Mt + 7.5, 9], "mattress"))
    else:
        els.append(el([x0, D - 2, 0.5], [x1, D + 6, 1.5], "warm_white",
                      per={"north": "footboard"}))
    # side rails: a split rail each side, up beside the mattress or folded down
    ry0, ry1 = (Mt - 0.5, Mt + 4.5) if rails else (D - 5, D)
    for rx in ((x0 - 0.6, x0 + 0.1), (x1 - 0.1, x1 + 0.6)):
        els.append(el([rx[0], ry0, 21], [rx[1], ry1, 29.5], "rail"))
        if not labor:
            els.append(el([rx[0], ry0, 6], [rx[1], ry1, 18.5], "rail"))
    if icu:
        # the mattress pump hung on the footboard, facing the room
        els.append(el([5, D + 1, -0.6], [11, D + 5, 0.5], "dark_grey",
                      per={"north": "pump"}))
    return els


BED_TEX = {"frame_grey": T("frame_grey"), "stainless": T("stainless"), "rubber": T("rubber"),
           "mattress": T("mattress"), "pillow": T("pillow"), "warm_white": T("warm_white"),
           "headboard": T("headboard"), "footboard": T("footboard"), "rail": T("bed_rail"),
           "dark_grey": T("dark_grey"), "pump": T("bed_pump"), "blanket": T("blanket_white"),
           "particle": T("frame_grey")}

BED_VARIANTS = [
    # (variant, geometry, blanket, mattress, en, de, es, sv)
    ("white", "std", "blanket_white", "mattress",
     "Hospital Bed (White Blanket)", "Krankenhausbett (weiße Decke)",
     "Cama de hospital (manta blanca)", "Sjukhussäng (vit filt)"),
    ("blue", "std", "blanket_blue", "mattress",
     "Hospital Bed (Blue Blanket)", "Krankenhausbett (blaue Decke)",
     "Cama de hospital (manta azul)", "Sjukhussäng (blå filt)"),
    ("green", "std", "blanket_green", "mattress",
     "Hospital Bed (Green Blanket)", "Krankenhausbett (grüne Decke)",
     "Cama de hospital (manta verde)", "Sjukhussäng (grön filt)"),
    ("mauve", "std", "blanket_mauve", "mattress",
     "Hospital Bed (Mauve Blanket)", "Krankenhausbett (malvenfarbene Decke)",
     "Cama de hospital (manta malva)", "Sjukhussäng (malvafärgad filt)"),
    ("icu", "icu", "blanket_white", "air_mattress",
     "ICU Bed", "Intensivbett", "Cama de UCI", "Intensivvårdssäng"),
    ("bariatric", "wide", "blanket_blue", "mattress",
     "Bariatric Hospital Bed", "Schwerlast-Krankenhausbett", "Cama de hospital bariátrica",
     "Bariatrisk sjukhussäng"),
    ("labor", "labor", "blanket_mauve", "mattress",
     "Labor and Delivery Bed", "Kreißbett", "Cama de partos", "Förlossningssäng"),
]
GEOMS = {"std": dict(), "icu": dict(icu=True), "wide": dict(wide=True),
         "labor": dict(labor=True)}
BOOL = ("false", "true")


def bed_models():
    models = {}
    for g, kw in GEOMS.items():
        for rails in (False, True):
            for high in (False, True):
                whole = bed_elements(rails, high, **kw)
                key = "%s_%s_%s" % (g, "up" if rails else "down", "high" if high else "low")
                head = cut(whole, 2, 16, 48, origin=16)
                foot = cut(whole, 2, -16, 16, origin=0)
                if g != "icu":
                    models["bed_head_" + key] = model(BED_TEX, head, ao=False)
                models["bed_foot_" + key] = model(BED_TEX, foot, ao=False)
        models["bed_item_" + g] = model(BED_TEX, shift(bed_elements(True, False, **kw), dz=-8),
                                        display=ITEM_SMALL)
    return models


def bed_geometry_head(g):
    return "std" if g == "icu" else g


ALL_BED_MODELS = bed_models()


def bed_states():
    for variant, g, blanket, mattress, *names in BED_VARIANTS:
        reg = "hospital_bed_" + variant
        textures = {"blanket": T(blanket), "mattress": T(mattress)}

        def choose(v, g=g, textures=textures):
            key = "%s_%s_%s" % (bed_geometry_head(g), "up" if v["rails"] == "true" else "down",
                                "high" if v["high"] == "true" else "low")
            return {"model": M("bed_head_" + key), "y": hc.ROT_Y[v["facing"]],
                    "textures": dict(textures)}
        state = hc.keyed_state([("facing", ["north", "east", "south", "west"]),
                                ("rails", list(BOOL)), ("high", list(BOOL))], choose)
        state["variants"]["inventory"] = [{"model": M("bed_item_" + g),
                                           "textures": dict(textures)}]
        models = {}
        if variant == "white":
            models = dict(ALL_BED_MODELS)
        C.add(reg, "new BlockHospitalBed(HospitalBedVariant.%s)" % variant.upper(), names,
              models, state, tab=PC)

    variants = {v[0]: v for v in BED_VARIANTS}

    def choose_foot(v):
        variant, g, blanket, mattress = variants[v["variant"]][:4]
        key = "%s_%s_%s" % (g, "up" if v["rails"] == "true" else "down",
                            "high" if v["high"] == "true" else "low")
        return {"model": M("bed_foot_" + key), "y": hc.ROT_Y[v["facing"]],
                "textures": {"blanket": T(blanket), "mattress": T(mattress)}}
    foot = hc.keyed_state([("facing", ["north", "east", "south", "west"]),
                           ("variant", [v[0] for v in BED_VARIANTS]),
                           ("rails", list(BOOL)), ("high", list(BOOL))], choose_foot)
    foot["variants"]["inventory"] = [{"model": M("bed_item_std")}]
    C.add("hospital_bed_foot", "new BlockHospitalBedFoot()",
          ("Hospital Bed (Foot)", "Krankenhausbett (Fußende)", "Cama de hospital (pies)",
           "Sjukhussäng (fotände)"), {}, foot, tab=HID)


bed_states()


# ==========================================================================================
# Simple pieces
# ==========================================================================================
def facing_block(reg, java, names, textures, elements, tab, extra=None, display=None,
                 extra_models=None):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    models = {reg: model(textures, elements, display=display)}
    models.update(extra_models or {})
    C.add(reg, java, names, models, hc.facing_state(M(reg), extra), tab=tab)


def prop(reg, box6, mat, mount, collides, names, textures, elements, tab, seat=None,
         display=None, cls="BlockHealthcare"):
    java = 'new %s("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.%s, %s)' % (
        cls, reg, B(*box6), mat, mount, "true" if collides else "false")
    if seat:
        java += ".withSeat(%g, %g)" % seat
    facing_block(reg, java, names, textures, elements, tab, display=display)


def screen_prop(reg, box6, mat, mount, collides, names, textures, elements, tab, on_tex,
                display=None):
    java = 'new BlockHealthcareScreen("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.%s, %s)' % (
        reg, B(*box6), mat, mount, "true" if collides else "false")
    facing_block(reg, java, names, textures, elements, tab,
                 extra={"on": {"false": {}, "true": {"textures": {"screen": T(on_tex)}}}},
                 display=display)


# --- headwall ------------------------------------------------------------------------------
def headwall_elements(upper):
    """The two-cell headwall drawn whole: x -16..16 (the second cell, to the viewer's right, is
    x -16..0), the panel 1.5 px deep against the wall at z 16."""
    els = []
    if upper:
        els.append(el([0, 0, 14.5], [15, 15, 16], "panel", per={"north": "upper"}))
        els.append(el([-15, 0, 14.5], [0, 15, 16], "panel", per={"north": "upper"}))
        els.append(el([-15.5, 14, 13.8], [15.5, 15.5, 16], "panel"))
    else:
        els.append(el([0, 0.5, 14.5], [15, 16, 16], "panel", per={"north": "left"}))
        els.append(el([-15, 0.5, 14.5], [0, 16, 16], "panel", per={"north": "right"}))
        # the gas outlets stand proud of the panel
        for i, g in enumerate(("o2", "air", "vac")):
            x = 13 - i * 4.5
            els.append(el([x - 2.6, 7.6, 13.9], [x - 0.6, 9.6, 14.5], "panel",
                          per={"north": "gas_" + g}, uv={"north": [0, 0, 16, 16]}))
        # the equipment rail along the bottom
        els.append(el([-15, 2.5, 13.6], [15, 3.5, 14.5], "stainless"))
        for x in (-12, -4, 4, 12):
            els.append(el([x, 2.5, 14.5], [x + 1, 3.5, 14.5], "stainless", faces=()))
    return [e for e in els if e["faces"]]


HW_TEX = {"panel": T("warm_white"), "left": T("headwall_left"), "right": T("headwall_right"),
          "upper": T("headwall_upper"), "gas_o2": T("gas_o2"), "gas_air": T("gas_air"),
          "gas_vac": T("gas_vac"), "stainless": T("stainless"), "particle": T("warm_white")}


def pair_block(reg, names, whole, axis, textures, java, tab, item_display=None, box_note=""):
    """A two-cell piece: the first cell's model is ``whole`` cut to 0..16 along ``axis``, the
    second's -16..0 (wide) or 16..32 (tall)."""
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


pair_block("patient_headwall",
           ("Patient Headwall", "Patienten-Wandversorgungseinheit",
            "Cabecero de pared del paciente", "Patientens sängvägg"),
           headwall_elements(False), 0, HW_TEX,
           'new BlockHealthcarePair("patient_headwall", %s, HealthcareMaterial.PLASTIC, '
           'BlockHealthcare.Mount.WALL, BlockHealthcarePair.Shape.WIDE)' % B(0, 0, 13, 16, 16, 16),
           PC)
pair_block("patient_headwall_upper",
           ("Patient Headwall (Upper Panel)", "Patienten-Wandversorgungseinheit (Oberteil)",
            "Cabecero de pared del paciente (panel superior)",
            "Patientens sängvägg (övre panel)"),
           headwall_elements(True), 0, HW_TEX,
           'new BlockHealthcarePair("patient_headwall_upper", %s, HealthcareMaterial.PLASTIC, '
           'BlockHealthcare.Mount.WALL, BlockHealthcarePair.Shape.WIDE)'
           % B(0, 0, 14, 16, 16, 16), PC)


# --- the reading light ---------------------------------------------------------------------
def light_state(model_path, lit_textures):
    return {"forge_marker": 1, "defaults": {"model": model_path},
            "variants": {
                "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                           "west": {"y": 270}},
                "state": {"0": {}, "1": {"textures": lit_textures}, "2": {},
                          "3": {"textures": lit_textures}},
                "inventory": [{}]}}


C.add("patient_reading_light", 'new BlockHealthcareLight("patient_reading_light", %s, 12)'
      % B(5, 8, 6, 11, 14, 16),
      ("Patient Reading Light", "Patienten-Leseleuchte", "Luz de lectura del paciente",
       "Patientens läslampa"),
      {"patient_reading_light": model(
          {"body": T("warm_white"), "metal": T("stainless"), "lens": T("lens_off"),
           "particle": T("warm_white")},
          [el([6, 10, 15], [10, 14, 16], "body"),
           el([7.5, 11.5, 9], [8.5, 12.5, 15], "metal"),
           el([5, 8.5, 6], [11, 11, 10], "body", per={"down": "lens"}),
           el([5.5, 8.3, 6.5], [10.5, 8.5, 9.5], "lens", faces=("down",))])},
      light_state(M("patient_reading_light"), {"lens": T("lens_on")}), tab=PC)


# --- monitors, IV, pump --------------------------------------------------------------------
MON_TEX = {"body": T("monitor_body"), "screen": T("screen_off"), "metal": T("stainless"),
           "grey": T("frame_grey"), "particle": T("monitor_body")}


def monitor_head(x0, y0, z0):
    """A monitor 12 x 9 x 3 with its screen on the north face, its corner at (x0, y0, z0)."""
    return [el([x0, y0, z0], [x0 + 12, y0 + 9, z0 + 3], "body"),
            screen([x0 + 1, y0 + 1, z0 - 0.05], [x0 + 11, y0 + 8, z0 - 0.05], "screen"),
            el([x0 + 1, y0 + 9, z0 + 0.5], [x0 + 3, y0 + 9.5, z0 + 2.5], "grey")]


screen_prop("patient_monitor_wall", (2, 4, 6, 14, 14, 16), "PLASTIC", "WALL", False,
            ("Bedside Monitor (Wall Arm)", "Patientenmonitor (Wandarm)",
             "Monitor de paciente (brazo de pared)", "Patientmonitor (väggarm)"),
            MON_TEX,
            [el([6, 6, 15], [10, 12, 16], "grey"),
             el([7.5, 8, 10], [8.5, 9, 15], "metal"),
             el([7, 7.5, 9], [9, 9.5, 10], "grey")] + monitor_head(2, 4, 6),
            PC, "monitor_screen")

STAND_BASE = []
for a in (0, 45):
    STAND_BASE.append(rot(el([2, 0.6, 7.4], [14, 1.4, 8.6], "grey"), "y", a, [8, 1, 8])
                      if a else el([2, 0.6, 7.4], [14, 1.4, 8.6], "grey"))
    STAND_BASE.append(rot(el([7.4, 0.6, 2], [8.6, 1.4, 14], "grey"), "y", a, [8, 1, 8])
                      if a else el([7.4, 0.6, 2], [8.6, 1.4, 14], "grey"))


def casters(points):
    return [el([x - 0.6, 0, z - 0.6], [x + 0.6, 0.8, z + 0.6], "rubber") for x, z in points]


screen_prop("patient_monitor_stand", (3, 0, 3, 13, 16, 13), "METAL", "FLOOR", True,
            ("Bedside Monitor (Rolling Stand)", "Patientenmonitor (Rollstativ)",
             "Monitor de paciente (soporte rodante)", "Patientmonitor (rullstativ)"),
            dict(MON_TEX, rubber=T("rubber")),
            STAND_BASE + casters([(2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)])
            + octo(8, 8, 0.6, 1.4, 18, "metal")
            + [el([6, 13, 9.5], [10, 14, 11.5], "grey")]
            + monitor_head(2, 18, 6.5),
            PC, "monitor_screen", display=ITEM_TALL)


def iv_pole(pump):
    els = list(STAND_BASE) + casters([(2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)])
    els += octo(8, 8, 0.45, 1.4, 30, "metal")
    els += [el([4.5, 29.4, 7.6], [11.5, 29.9, 8.4], "metal"),
            el([4.5, 28.4, 7.7], [5, 29.4, 8.3], "metal"),
            el([11, 28.4, 7.7], [11.5, 29.4, 8.3], "metal"),
            el([9.6, 21.5, 7.2], [12.4, 28.5, 8.8], "bag"),
            el([10.85, 12, 7.85], [11.15, 21.5, 8.15], "cord")]
    if pump:
        els += [el([5, 13, 6.2], [11, 19, 9.6], "body", per={"north": "body"}),
                screen([5.6, 15.8, 6.15], [10.4, 18.4, 6.15], "screen"),
                el([7.3, 14, 9.6], [8.7, 15, 10.5], "grey")]
    return els


IV_TEX = {"metal": T("stainless"), "grey": T("frame_grey"), "rubber": T("rubber"),
          "bag": T("iv_bag"), "cord": T("cord"), "body": T("warm_white"),
          "screen": T("screen_off"), "particle": T("stainless")}
prop("iv_pole", (6, 0, 6, 10, 16, 10), "METAL", "FLOOR", True,
     ("IV Pole", "Infusionsständer", "Portasueros", "Droppställning"),
     IV_TEX, iv_pole(False), PC, display=ITEM_TALL)
screen_prop("iv_pole_pump", (5, 0, 5, 11, 16, 11), "METAL", "FLOOR", True,
            ("IV Pole with Infusion Pump", "Infusionsständer mit Infusionspumpe",
             "Portasueros con bomba de infusión", "Droppställning med infusionspump"),
            IV_TEX, iv_pole(True), PC, "pump_screen", display=ITEM_TALL)

# --- room furniture ------------------------------------------------------------------------
prop("overbed_table", (0, 0, 2, 16, 13, 14), "METAL", "FLOOR", True,
     ("Overbed Table", "Betttisch", "Mesa de cama", "Sängbord"),
     {"top": T("beech"), "metal": T("stainless"), "grey": T("frame_grey"),
      "rubber": T("rubber")},
     [el([2, 0.8, 11], [14, 2, 14], "grey")] + casters([(2.8, 12.5), (13.2, 12.5)])
     + [el([7, 2, 11.5], [9, 12, 13.5], "metal"),
        el([0, 12, 2], [16, 13, 13], "top"),
        el([0, 11.4, 9], [16, 12, 13], "grey")], PC)

prop("patient_recliner", (1, 0, 1, 15, 16, 16), "CLOTH", "FLOOR", True,
     ("Patient Recliner", "Patientenruhesessel", "Sillón reclinable para pacientes",
      "Vilstol för patienter"),
     {"vinyl": T("vinyl_teal"), "grey": T("frame_grey"), "rubber": T("rubber")},
     [el([2, 0, 2], [14, 3, 14], "grey"),
      el([2, 3, 2], [14, 7.5, 13], "vinyl"),
      el([1, 3, 2.5], [3, 11, 14], "vinyl"), el([13, 3, 2.5], [15, 11, 14], "vinyl"),
      el([2.5, 7, 12], [13.5, 21, 15.5], "vinyl"),
      el([3, 2, 0.5], [13, 6, 2], "vinyl")]
     + casters([(2.5, 2.5), (13.5, 2.5), (2.5, 13.5), (13.5, 13.5)]),
     PC, seat=(8, 1), display=ITEM_TALL)

prop("visitor_chair", (2, 0, 2, 14, 16, 15), "WOOD", "FLOOR", True,
     ("Visitor Chair", "Besucherstuhl", "Silla de visitas", "Besöksstol"),
     {"wood": T("beech"), "vinyl": T("vinyl_blue")},
     [el([2.5, 0, 2.5], [3.5, 8, 3.5], "wood"), el([12.5, 0, 2.5], [13.5, 8, 3.5], "wood"),
      el([2.5, 0, 12.5], [3.5, 16, 13.5], "wood"), el([12.5, 0, 12.5], [13.5, 16, 13.5], "wood"),
      el([2, 7, 2], [14, 8.5, 13], "vinyl"),
      el([3, 9.5, 12.8], [13, 15.5, 14.3], "vinyl"),
      el([2.2, 11, 3], [3.8, 11.8, 13], "wood"), el([12.2, 11, 3], [13.8, 11.8, 13], "wood"),
      el([2.5, 8, 3], [3.5, 11, 4], "wood"), el([12.5, 8, 3], [13.5, 11, 4], "wood")],
     PC, seat=(8.5, 0.5))

# the wardrobe: two tall
WARDROBE = [el([1, 0, 2], [15, 16, 16], "carcass", per={"north": "doors"}),
            el([1, 16, 2], [15, 29, 16], "carcass", per={"north": "doors_top"}),
            el([0.5, 29, 1.5], [15.5, 30, 16], "carcass"),
            el([1, 0, 1.8], [15, 1, 2], "grey", faces=("north",))]
pair_block("patient_wardrobe",
           ("Patient Wardrobe", "Patientenkleiderschrank", "Armario del paciente",
            "Patientgarderob"),
           WARDROBE, 1,
           {"carcass": T("warm_white"), "doors": T("wardrobe_doors"),
            "doors_top": T("wardrobe_doors_top"), "grey": T("frame_grey"),
            "particle": T("warm_white")},
           'new BlockHealthcarePair("patient_wardrobe", %s, HealthcareMaterial.WOOD, '
           'BlockHealthcare.Mount.FLOOR, BlockHealthcarePair.Shape.TALL)'
           % B(1, 0, 2, 15, 16, 16), PC, item_display=ITEM_TALL)

# the wardrobe's doors read as one tall door per side: draw the door texture on the first cell
# and a plain top on the second (the cut keeps the north face on both)

screen_prop("patient_room_tv", (1, 3, 8, 15, 13, 16), "PLASTIC", "WALL", False,
            ("Patient Room TV", "Patientenzimmer-Fernseher", "Televisor de habitación",
             "Patientrums-TV"),
            {"body": T("tv_body"), "screen": T("screen_off"), "grey": T("frame_grey")},
            [el([6, 6, 15], [10, 10, 16], "grey"), el([7.5, 7.5, 10.5], [8.5, 8.5, 15], "grey"),
             el([1, 3, 9], [15, 13, 10.5], "body"),
             screen([1.5, 3.5, 8.95], [14.5, 12.5, 8.95], "screen")],
            PC, "tv_screen")

screen_prop("vitals_wall_unit", (3, 2, 11, 13, 14, 16), "PLASTIC", "WALL", False,
            ("Wall Vitals Unit", "Wand-Vitaldateneinheit", "Unidad de constantes vitales de pared",
             "Väggmonterad vitalparameterenhet"),
            {"body": T("warm_white"), "screen": T("screen_off"), "grey": T("frame_grey"),
             "cuff": T("vinyl_blue"), "cord": T("charcoal")},
            [el([4, 6, 13], [12, 14, 16], "body"),
             screen([5, 8.5, 12.95], [11, 13, 12.95], "screen"),
             el([4, 2, 12], [12, 5, 16], "grey"),
             el([5, 3, 11.5], [11, 5.5, 13], "cuff"),
             el([11.5, 5, 13.5], [12, 9, 14], "cord")],
            PC, "vitals_screen")

prop("suction_regulator", (4, 1, 10, 12, 15, 16), "PLASTIC", "WALL", False,
     ("Wall Suction Regulator", "Wand-Absaugregler", "Regulador de aspiración de pared",
      "Väggsugregulator"),
     {"body": T("white"), "gauge": T("suction_gauge"), "can": T("canister"),
      "grey": T("frame_grey")},
     [el([6, 9, 13], [10, 14, 16], "body", per={"north": "gauge"}),
      el([7, 8, 13.5], [9, 9, 15], "grey"),
      el([4.5, 7.2, 14], [11.5, 7.8, 16], "grey")]
     + octo(8, 12.5, 2.3, 1, 7.2, "can"), PC)

# --- nurse call ----------------------------------------------------------------------------
def call_device(reg, names, textures, elements, box6, lamp_key="lamp"):
    java = 'new BlockNurseCallDevice("%s", %s)' % (reg, B(*box6))
    facing_block(reg, java, names, textures, elements, PC,
                 extra={"calling": {"false": {},
                                    "true": {"textures": {lamp_key: T("call_lamp_on")}}}})


call_device("nurse_call_button",
            ("Nurse Call Station", "Schwesternruf-Taster", "Pulsador de llamada a enfermería",
             "Sköterskeanrop (väggstation)"),
            {"plate": T("call_station"), "body": T("white"), "button": T("call_button"),
             "lamp": T("call_lamp_off"), "particle": T("white")},
            [el([4, 4, 15], [12, 12, 16], "body", per={"north": "plate"}),
             el([6.5, 6, 14.3], [9.5, 9, 15], "button"),
             el([7, 9.8, 14.6], [9, 10.8, 15], "lamp")],
            (4, 4, 14, 12, 12, 16))
call_device("nurse_call_pillow_speaker",
            ("Nurse Call Pillow Speaker", "Schwesternruf-Kissenlautsprecher",
             "Mando de llamada de almohada", "Sköterskeanrop (kuddhögtalare)"),
            {"body": T("white"), "face": T("pillow_speaker"), "cord": T("charcoal"),
             "grey": T("frame_grey"), "lamp": T("call_lamp_off"), "particle": T("white")},
            [el([6.5, 11, 15], [9.5, 13, 16], "grey"),
             el([6, 3, 13.4], [10, 11.5, 15], "body", per={"north": "face"}),
             el([7, 11.5, 13.8], [9, 12.2, 14.6], "lamp"),
             el([7.8, -8, 14], [8.2, 3, 14.4], "cord")],
            (6, 3, 13, 10, 13, 16))
call_device("nurse_call_pull_cord",
            ("Nurse Call Pull Cord", "Schwesternruf-Zugschnur", "Cordón de llamada a enfermería",
             "Sköterskeanrop (dragsnöre)"),
            {"body": T("white"), "face": T("pull_station"), "cord": T("red"),
             "lamp": T("call_lamp_off"), "particle": T("white")},
            [el([5, 10, 15], [11, 16, 16], "body", per={"north": "face"}),
             el([7, 14.6, 14.6], [9, 15.6, 15], "lamp"),
             el([7.9, -12, 14.6], [8.1, 10, 14.8], "cord"),
             el([7, -14, 14.4], [9, -12, 15], "cord")],
            (5, 0, 14, 11, 16, 16))

C.add("nurse_call_dome_light", 'new BlockNurseCallDomeLight("nurse_call_dome_light", %s)'
      % B(4, 5, 10, 12, 11, 16),
      ("Nurse Call Dome Light", "Schwesternruf-Zimmerleuchte",
       "Luz de pasillo de llamada a enfermería", "Sköterskeanrop (korridorlampa)"),
      {"nurse_call_dome_light": model(
          {"base": T("white"), "white": T("dome_white"), "amber": T("dome_amber"),
           "particle": T("white")},
          [el([4, 5, 15], [12, 11, 16], "base"),
           el([5, 6, 12.5], [8, 10, 15], "white"),
           el([8, 6, 12.5], [11, 10, 15], "amber"),
           el([5.5, 6.5, 11], [10.5, 9.5, 12.5], "white")])},
      light_state(M("nurse_call_dome_light"), {"amber": T("dome_amber_on")}), tab=PC)


# the master panel: multipart (the room lamps are actual state from the tile entity)
def panel_models():
    base = model({"body": T("charcoal"), "face": T("panel_face"), "particle": T("charcoal")},
                 [el([1, 2, 14], [15, 14, 16], "body"),
                  screen([1.5, 2.5, 13.95], [14.5, 13.5, 13.95], "face")])
    models = {"nurse_call_panel_base": base}
    for i in range(6):
        cx = 16 - (1.5 + 13 * (8 + (i % 3) * 8) / 32.0)
        cy = 16 - (2.5 + 11 * PANEL_ROWS[i // 3] / 32.0)
        for on in (False, True):
            models["nurse_call_panel_lamp%d_%s" % (i + 1, "on" if on else "off")] = model(
                {"lamp": T("panel_lamp_on" if on else "panel_lamp_off"),
                 "particle": T("charcoal")},
                [el([cx - 0.8, cy - 0.8, 13.5], [cx + 0.8, cy + 0.8, 14], "lamp",
                    faces=("north", "east", "west", "up", "down"))])
    for on in (False, True):
        models["nurse_call_panel_alarm_%s" % ("on" if on else "off")] = model(
            {"lamp": T("panel_alarm_on" if on else "panel_alarm_off"),
             "particle": T("charcoal")},
            [el([12.5, 11.2, 13.4], [14, 12.8, 14], "lamp",
                faces=("north", "east", "west", "up", "down"))])
    return models


def panel_state():
    parts = []
    for f, y in hc.ROT_Y.items():
        parts.append({"when": {"facing": f}, "apply": {"model": M("nurse_call_panel_base"),
                                                       "y": y}})
        for i in range(6):
            for on in (False, True):
                parts.append({"when": {"facing": f, "room%d" % (i + 1): BOOL[on]},
                              "apply": {"model": M("nurse_call_panel_lamp%d_%s"
                                                   % (i + 1, "on" if on else "off")), "y": y}})
        quiet = {"facing": f}
        quiet.update({"room%d" % (i + 1): "false" for i in range(6)})
        parts.append({"when": quiet, "apply": {"model": M("nurse_call_panel_alarm_off"), "y": y}})
        parts.append({"when": {"OR": [{"facing": f, "room%d" % (i + 1): "true"}
                                      for i in range(6)]},
                      "apply": {"model": M("nurse_call_panel_alarm_on"), "y": y}})
    return {"multipart": parts}


C.add("nurse_call_master_panel", "new BlockNurseCallMasterPanel()",
      ("Nurse Call Master Panel", "Schwesternruf-Zentrale", "Panel central de llamada a enfermería",
       "Sköterskeanrop (centralpanel)"),
      panel_models(), panel_state(),
      item={"parent": MI("nurse_call_panel_base")}, tab=CL)


# ==========================================================================================
# Cubicle curtains
# ==========================================================================================
def track_models():
    t = {"metal": T("stainless"), "grey": T("frame_grey"), "tile": T("ceiling_tile"),
         "particle": T("ceiling_tile")}
    arm = [el([7, -1.2, 0], [9, 0, 8], "metal", faces=("east", "west", "down", "north")),
           el([7.4, -1.6, 1], [8.6, -1.2, 7], "grey")]
    return {
        # the ceiling tile the track takes the place of, and the track's middle below it
        "curtain_track_tile": model(t, [el([0, 0, 0], [16, 1, 16], "tile")]),
        "curtain_track_hub": model(t, [el([6.8, -1.4, 6.8], [9.2, 0, 9.2], "metal",
                                          faces=("north", "south", "east", "west", "down"))]),
        "curtain_track_arm": model(t, arm),
        "curtain_track_end": model(t, arm + [el([6.6, -1.8, 0], [9.4, 0, 0.8], "grey",
                                                faces=("north", "south", "east", "west",
                                                       "down"))]),
        "curtain_track_item": model(t, [el([0, 0, 0], [16, 1, 16], "tile"),
                                        el([7, -1.2, 0], [9, 0, 16], "metal")],
                                    display=ITEM_SMALL),
    }


def track_state():
    parts = [{"apply": {"model": M("curtain_track_tile")}},
             {"apply": {"model": M("curtain_track_hub")}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        parts.append({"when": {side: "join"}, "apply": {"model": M("curtain_track_arm"), "y": y}})
        parts.append({"when": {side: "end"}, "apply": {"model": M("curtain_track_end"), "y": y}})
    return {"multipart": parts}


C.add("cubicle_curtain_track", "new BlockCurtainTrack()",
      ("Cubicle Curtain Track", "Vorhangschiene für Behandlungskabinen",
       "Riel de cortina de cubículo", "Skena för draperi"),
      track_models(), track_state(),
      item={"parent": MI("curtain_track_item")}, tab=PC)

HEM_Y = 1      # the bottom panel's hem: this far above its cell's floor
MESH_Y = 9     # the top panel: cloth below, the mesh band above
TAPE_Y = 15    # the heading tape the carriers hang it by


def curtain_models(name):
    """A cloth's part models. An arm runs from the cell's middle to its north edge; the four
    kinds are a middle panel, the bottom one (hem), the top one (mesh band over the cloth) and a
    panel that is both. The bunch is the cloth gathered at the run's end, with the same four."""
    t = {"cloth": T("curtain_" + name), "hem": T("curtain_%s_hem" % name),
         "mesh": T("curtain_mesh"), "tape": T("curtain_tape"), "particle": T("curtain_" + name)}
    c0, c1 = 7.75, 8.25
    sides = ("east", "west", "north")

    def arm(top, bottom):
        lo = HEM_Y + 1 if bottom else 0
        hi = MESH_Y if top else 16
        els = [el([c0, lo, 0], [c1, hi, 8], "cloth", faces=sides)]
        if bottom:
            els.append(el([c0 - 0.1, HEM_Y, 0], [c1 + 0.1, HEM_Y + 1, 8], "hem",
                          faces=sides + ("down",)))
        if top:
            els.append(el([c0 + 0.05, MESH_Y, 0], [c1 - 0.05, TAPE_Y, 8], "mesh",
                          faces=("east", "west")))
            els.append(el([c0 - 0.15, TAPE_Y, 0], [c1 + 0.15, 16, 8], "tape",
                          faces=sides + ("up", "down")))
        return els

    def bunch(top, bottom):
        lo = HEM_Y + 1 if bottom else 0
        hi = MESH_Y if top else 16
        ring = ("north", "south", "east", "west")
        els = [el([5.4, lo, 5.4], [10.6, hi, 10.6], "cloth", faces=ring)]
        if bottom:
            els.append(el([5.2, HEM_Y, 5.2], [10.8, HEM_Y + 1, 10.8], "hem"))
        if top:
            els.append(el([5.8, MESH_Y, 5.8], [10.2, TAPE_Y, 10.2], "mesh", faces=ring))
            els.append(el([5.5, TAPE_Y, 5.5], [10.5, 16, 10.5], "tape"))
        return els

    m = {}
    for top in (False, True):
        for bottom in (False, True):
            k = "%s%s" % ("top" if top else "mid", "_hem" if bottom else "")
            m["curtain_%s_arm_%s" % (name, k)] = model(t, arm(top, bottom))
            m["curtain_%s_bunch_%s" % (name, k)] = model(t, bunch(top, bottom))
    m["curtain_%s_corner" % name] = model(t, [el([c0, HEM_Y, c0], [c1, TAPE_Y, c1], "cloth",
                                                 faces=("up", "down"))])
    return m


ARM_SHAPES = {"north": "z|ne|nw", "south": "z|se|sw", "east": "x|ne|se", "west": "x|nw|sw"}


def curtain_state():
    parts = []
    for name, _c, _p in CURTAINS:
        for top in (False, True):
            for bottom in (False, True):
                k = "%s%s" % ("top" if top else "mid", "_hem" if bottom else "")
                tb = {"pattern": name, "top": BOOL[top], "bottom": BOOL[bottom]}
                for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                    when = dict(tb, drawn="closed", shape=ARM_SHAPES[side])
                    parts.append({"when": when, "apply": {
                        "model": M("curtain_%s_arm_%s" % (name, k)), "y": y}})
                parts.append({"when": dict(tb, drawn="bunched"), "apply": {
                    "model": M("curtain_%s_bunch_%s" % (name, k))}})
        parts.append({"when": {"pattern": name, "drawn": "closed", "shape": "ne|nw|se|sw"},
                      "apply": {"model": M("curtain_%s_corner" % name)}})
    return {"multipart": parts}


CURTAIN_MODELS = {}
for _n, _c, _p in CURTAINS:
    CURTAIN_MODELS.update(curtain_models(_n))
C.add("cubicle_curtain", "new BlockCurtainPanel()",
      ("Cubicle Curtain", "Kabinenvorhang", "Cortina de cubículo", "Draperi"),
      CURTAIN_MODELS, curtain_state(),
      item={"parent": MI("curtain_mint_arm_top")}, tab=HID)


# ==========================================================================================
# Corridor rails
# ==========================================================================================
RAIL_Z = 13.6  # the rail's centre line, 2.4 px off the wall


def rail_section(style, x0, x1, z=RAIL_Z, along="x"):
    """A length of rail from x0 to x1 (or z0 to z1 when along z), at the rail's height."""
    if style == "handrail":
        if along == "x":
            return [el([x0, 13.4, z - 0.8], [x1, 15, z + 0.8], "rail",
                       faces=("north", "south", "up", "down"))]
        return [el([z - 0.8, 13.4, x0], [z + 0.8, 15, x1], "rail",
                   faces=("east", "west", "up", "down"))]
    if along == "x":
        return [el([x0, 12, z - 0.6], [x1, 15.5, z + 1.6], "rail",
                   faces=("north", "south", "up", "down"))]
    return [el([z - 0.6, 12, x0], [z + 1.6, 15.5, x1], "rail",
               faces=("east", "west", "up", "down"))]


def bracket(style, x):
    if style == "handrail":
        return [el([x - 0.5, 13.6, RAIL_Z + 0.8], [x + 0.5, 14.6, 16], "metal")]
    return [el([x - 0.6, 12.5, RAIL_Z + 1.6], [x + 0.6, 15, 16], "metal")]


def rail_models(style):
    t = {"rail": T("beech" if style == "handrail" else "vinyl_blue"), "metal": T("stainless"),
         "particle": T("beech" if style == "handrail" else "vinyl_blue")}
    m = {}
    m["%s_centre" % style] = model(t, rail_section(style, 4, 12) + bracket(style, 8))
    east = {
        "join": rail_section(style, 12, 16),
        "end": rail_section(style, 12, 14.4)
        + (rail_section(style, RAIL_Z, 16, z=14.4, along="z") if style == "handrail"
           else [el([13.6, 12, RAIL_Z - 0.6], [14.4, 15.5, 16], "rail")]),
        "inner": rail_section(style, 12, 16 - (16 - RAIL_Z))
        + rail_section(style, 0, RAIL_Z + 0.8, z=RAIL_Z, along="z"),
        "outer": rail_section(style, 12, 16 + (16 - RAIL_Z) + 0.8),
    }
    # the inner turn runs along the side wall at x = RAIL_Z (2.4 px off it); along="z" puts the
    # rail's centre at "z" in x, so pass RAIL_Z there
    east["inner"] = rail_section(style, 12, RAIL_Z + 0.8) + [
        e for e in rail_section(style, 0, RAIL_Z, z=RAIL_Z, along="z")]
    for k, els in east.items():
        m["%s_east_%s" % (style, k)] = model(t, els)
        m["%s_west_%s" % (style, k)] = model(t, mirror_x(els))
    m["%s_item" % style] = model(t, rail_section(style, 0, 16) + bracket(style, 4)
                                 + bracket(style, 12), display=ITEM_SMALL)
    return m


def rail_state(style):
    parts = []
    for f, y in hc.ROT_Y.items():
        parts.append({"when": {"facing": f}, "apply": {"model": M("%s_centre" % style), "y": y}})
        for side in ("east", "west"):
            for k in ("end", "join", "inner", "outer"):
                parts.append({"when": {"facing": f, side: k},
                              "apply": {"model": M("%s_%s_%s" % (style, side, k)), "y": y}})
    return {"multipart": parts}


for style, names, box6 in (
        ("handrail", ("Corridor Handrail", "Flurhandlauf", "Pasamanos de pasillo",
                      "Korridorledstång"), (0, 13, 12, 16, 16, 16)),
        ("crash_rail", ("Corridor Crash Rail", "Flur-Rammschutzleiste",
                        "Protector de pared de pasillo", "Korridorens stötlist"),
         (0, 12, 12, 16, 16, 16))):
    reg = "corridor_" + style
    C.add(reg, 'new BlockCorridorRail("%s", %s)' % (reg, B(*box6)), names,
          rail_models(style), rail_state(style),
          item={"parent": MI("%s_item" % style)}, tab=PC)


# ==========================================================================================
# Clinical: dispensers, carts, bins, gas, eyewash
# ==========================================================================================
prop("sharps_container", (4, 3, 11, 12, 13, 16), "PLASTIC", "WALL", False,
     ("Sharps Container", "Kanülenabwurfbehälter", "Contenedor de objetos punzantes",
      "Kanylburk"),
     {"body": T("red"), "front": T("sharps_front"), "lid": T("yellow"),
      "grey": T("frame_grey")},
     [el([4, 3, 15], [12, 9, 16], "grey"),
      el([4.5, 4, 11.5], [11.5, 11, 15], "body", per={"north": "front"}),
      el([4.3, 11, 11.3], [11.7, 12.5, 15], "lid")], CL)

prop("glove_box_dispenser", (1, 4, 12, 15, 12, 16), "PLASTIC", "WALL", False,
     ("Glove Box Dispenser", "Handschuhspender", "Dispensador de guantes",
      "Handskhållare"),
     {"frame": T("white"), "boxes": T("glove_boxes")},
     [el([1, 4, 15], [15, 12, 16], "frame"),
      el([1.5, 4.5, 12.5], [14.5, 11.5, 15], "frame", per={"north": "boxes"}),
      el([1, 4, 12], [15, 4.5, 15], "frame")], CL)

prop("hand_sanitizer_dispenser", (5, 3, 12, 11, 13, 16), "PLASTIC", "WALL", False,
     ("Hand Sanitizer Dispenser", "Händedesinfektionsspender",
      "Dispensador de desinfectante de manos", "Handspritautomat"),
     {"body": T("white"), "front": T("sanitizer_front"), "grey": T("frame_grey")},
     [el([5, 4, 12.5], [11, 13, 16], "body", per={"north": "front"}),
      el([6, 3, 12], [10, 4, 14.5], "grey")], CL)

prop("ppe_isolation_cart", (2, 0, 3, 14, 14, 13), "PLASTIC", "FLOOR", True,
     ("PPE Isolation Cart", "Isolationswagen (PSA)", "Carro de aislamiento (EPI)",
      "Isoleringsvagn (skyddsutrustning)"),
     {"body": T("yellow"), "front": T("isolation_drawers"), "gowns": T("gowns"),
      "rubber": T("rubber"), "grey": T("frame_grey")},
     [el([2, 1, 3], [14, 12, 13], "body", per={"north": "front"}),
      el([2.5, 12, 3.5], [13.5, 13, 12.5], "grey"),
      el([3, 13, 4], [13, 14, 12], "gowns")]
     + casters([(3, 4), (13, 4), (3, 12), (13, 12)]), CL)

prop("biohazard_bin", (4, 0, 4, 12, 11, 12), "PLASTIC", "FLOOR", True,
     ("Biohazard Waste Bin", "Abfallbehälter für infektiösen Abfall",
      "Contenedor de residuos biológicos", "Behållare för smittförande avfall"),
     {"body": T("red"), "front": T("biohazard_front"), "grey": T("dark_grey")},
     [el([4, 0, 4], [12, 10, 12], "body", per={"north": "front"}),
      el([3.8, 10, 3.8], [12.2, 11, 12.2], "body"),
      el([5.5, 0, 2.6], [10.5, 0.8, 4], "grey")], CL)

prop("soiled_linen_hamper", (3, 0, 3, 13, 14, 13), "METAL", "FLOOR", True,
     ("Soiled Linen Hamper", "Wäschesammler für Schmutzwäsche", "Carro de ropa sucia",
      "Smutstvättsäck på ställ"),
     {"metal": T("stainless"), "bag": T("bag_blue"), "rubber": T("rubber"),
      "lid": T("dark_grey")},
     [el([3.5, 0.8, 3.5], [12.5, 1.6, 4.5], "metal"), el([3.5, 0.8, 11.5], [12.5, 1.6, 12.5], "metal"),
      el([3.5, 1.6, 3.5], [4.5, 13, 4.5], "metal"), el([11.5, 1.6, 3.5], [12.5, 13, 4.5], "metal"),
      el([3.5, 1.6, 11.5], [4.5, 13, 12.5], "metal"), el([11.5, 1.6, 11.5], [12.5, 13, 12.5], "metal"),
      el([3, 13, 3], [13, 14, 13], "lid")]
     + octo(8, 8, 3.8, 3, 13, "bag", top=False)
     + casters([(4, 4), (12, 4), (4, 12), (12, 12)]), CL)

prop("clean_linen_cart", (1, 0, 3, 15, 16, 13), "METAL", "FLOOR", True,
     ("Clean Linen Cart", "Wäschewagen für saubere Wäsche", "Carro de ropa limpia",
      "Vagn för ren tvätt"),
     {"metal": T("stainless"), "shelf": T("wire_shelf"), "linen": T("linen_stack"),
      "rubber": T("rubber")},
     [el([1, 0.8, 3], [2, 16, 4], "metal"), el([14, 0.8, 3], [15, 16, 4], "metal"),
      el([1, 0.8, 12], [2, 16, 13], "metal"), el([14, 0.8, 12], [15, 16, 13], "metal")]
     + [el([1, y, 3], [15, y + 0.4, 13], "shelf", faces=("up", "down")) for y in (1.5, 6.5, 11.5)]
     + [el([2.5, y + 0.4, 4.5], [13.5, y + 4, 11.5], "linen") for y in (1.5, 6.5, 11.5)]
     + casters([(1.5, 3.5), (14.5, 3.5), (1.5, 12.5), (14.5, 12.5)]), CL)

prop("medical_gas_outlet_o2", (5, 5, 14, 11, 11, 16), "METAL", "WALL", False,
     ("Medical Gas Outlet (Oxygen)", "Medizinische Gasentnahmestelle (Sauerstoff)",
      "Toma de gas medicinal (oxígeno)", "Medicinskt gasuttag (syrgas)"),
     {"plate": T("o2_plate"), "body": T("white"), "o2": T("gas_o2")},
     [el([5, 5, 15.2], [11, 11, 16], "body", per={"north": "plate"}),
      el([6.5, 5.5, 14.4], [9.5, 8.5, 15.2], "body", per={"north": "o2"})], CL)

prop("medical_gas_zone_sign", (1, 4, 15, 15, 12, 16), "PLASTIC", "WALL", False,
     ("Medical Gas Zone Valve Sign", "Schild Medizinische Gase (Zonenventile)",
      "Señal de válvulas de zona de gases medicinales",
      "Skylt för medicinska gaser (zonventiler)"),
     {"sign": T("gas_sign"), "body": T("white")},
     [el([1, 4, 15.4], [15, 12, 16], "body", faces=("south", "east", "west", "up", "down")),
      screen([1, 4, 15.4], [15, 12, 15.4], "sign")], CL)

# ==========================================================================================
# Messages
# ==========================================================================================
for key, names in (
        ("seat.taken", ("Someone is already there.", "Hier sitzt schon jemand.",
                        "Ya hay alguien.", "Någon sitter redan där.")),
        ("nursecall.picked", ("[Nurse call] Picked %s: now sneak-click the dome lights or devices"
                              " to link to it.",
                              "[Schwesternruf] %s gewählt: jetzt Zimmerleuchten oder Geräte"
                              " mit Schleichen und Klicken verbinden.",
                              "[Llamada] %s elegido: ahora agáchate y haz clic en las luces o"
                              " dispositivos para vincularlos.",
                              "[Sköterskeanrop] %s vald: smyg och klicka nu på lampor eller"
                              " enheter för att koppla dem.")),
        ("nursecall.linked", ("[Nurse call] Linked to %s.", "[Schwesternruf] Mit %s verbunden.",
                              "[Llamada] Vinculado a %s.", "[Sköterskeanrop] Kopplad till %s.")),
        ("nursecall.link_failed", ("[Nurse call] Not linked: %s.",
                                   "[Schwesternruf] Nicht verbunden: %s.",
                                   "[Llamada] No vinculado: %s.",
                                   "[Sköterskeanrop] Inte kopplad: %s.")),
        ("nursecall.pick_first", ("[Nurse call] Sneak-click a master panel or dome light first.",
                                  "[Schwesternruf] Zuerst eine Zentrale oder Zimmerleuchte mit"
                                  " Schleichen und Klicken wählen.",
                                  "[Llamada] Primero agáchate y haz clic en un panel o una luz.",
                                  "[Sköterskeanrop] Smyg och klicka först på en centralpanel"
                                  " eller korridorlampa.")),
        ("nursecall.pick_panel_first", ("[Nurse call] Sneak-click a master panel first, then"
                                        " this monitor.",
                                        "[Schwesternruf] Zuerst eine Zentrale mit Schleichen"
                                        " und Klicken wählen, dann diesen Monitor.",
                                        "[Llamada] Primero agáchate y haz clic en un panel,"
                                        " luego en este monitor.",
                                        "[Sköterskeanrop] Smyg och klicka först på en"
                                        " centralpanel, sedan på den här skärmen.")),
        ("nursecall.monitor_unlinked", ("[Nurse call] Not linked to a master panel: sneak-click"
                                        " a panel, then these monitors.",
                                        "[Schwesternruf] Mit keiner Zentrale verbunden: eine"
                                        " Zentrale, dann diese Monitore mit Schleichen und"
                                        " Klicken wählen.",
                                        "[Llamada] Sin panel vinculado: agáchate y haz clic en"
                                        " un panel y luego en estos monitores.",
                                        "[Sköterskeanrop] Inte kopplad till en centralpanel:"
                                        " smyg och klicka på en panel, sedan på skärmarna.")),
        ("nursecall.cancelled", ("[Nurse call] %s call(s) cancelled.",
                                 "[Schwesternruf] %s Ruf(e) abgestellt.",
                                 "[Llamada] %s llamada(s) cancelada(s).",
                                 "[Sköterskeanrop] %s anrop återställda.")),
        ("nursecall.no_call", ("[Nurse call] No call in this room.",
                               "[Schwesternruf] Kein Ruf in diesem Zimmer.",
                               "[Llamada] No hay llamadas en esta habitación.",
                               "[Sköterskeanrop] Inga anrop i rummet.")),
        ("nursecall.panel_quiet", ("[Nurse call] No calls (%s rooms linked).",
                                   "[Schwesternruf] Keine Rufe (%s Zimmer verbunden).",
                                   "[Llamada] Sin llamadas (%s habitaciones vinculadas).",
                                   "[Sköterskeanrop] Inga anrop (%s rum kopplade).")),
        ("nursecall.panel_calling", ("[Nurse call] Calling: room %s.",
                                     "[Schwesternruf] Ruf aus Zimmer %s.",
                                     "[Llamada] Llamando: habitación %s.",
                                     "[Sköterskeanrop] Anrop från rum %s."))):
    C.add_lang("csm.healthcare." + key, names)


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
