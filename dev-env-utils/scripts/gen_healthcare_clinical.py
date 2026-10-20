#!/usr/bin/env python3
"""
gen_healthcare_clinical.py -- the Healthcare module's ICU, emergency department, nursing station
and surgery support pieces.

The ventilator, the ICU pendant and the drop column every ceiling-hung piece shares, the crash
cart and the rolling defibrillator, the nurses' station counters (straight, with monitors, and the
outside corner, joining like the furniture reception desk), the patient status board, the
workstation on wheels, the portable ultrasound cart, the automated dispensing cabinet, medication cart and medication fridge,
the wheelchair, its storage rack and corral sign, the walker, the mobile and ceiling patient
lifts, the triage desk, the trauma bay exam light, the decontamination shower (Life Safety has
the decon sink and the eyewash station), the AMBULANCE ONLY floor marking and sign, the ED
tracking board (its text is TileEntityEdTrackingBoardRenderer's), and surgery support: the
equipment boom, blanket warming cabinet, autoclave, case cart and scrub attire dispenser.

Nothing here is a real product: no maker's name, no red cross (a protected emblem). Screens are
animated textures (.mcmeta), so nothing ticks; only the ED tracking board has a renderer.

Swatches the wards already draw (stainless, the frame greys, rubber, the monitor body, the
ceiling tile) are shared by path, not drawn again.

Writes, under modules/healthcare/src/main/resources/assets/csm:

  * textures/blocks/healthcare/clinical/*.png (+ .mcmeta for the screens)
  * models/block/healthcare/clinical/*.json
  * blockstates/<registry>.json, and models/item/<registry>.json for the multipart blocks
  * the tile names in all four lang files

Every model faces north with what it stands against at +Z.

Usage:
    python gen_healthcare_clinical.py              # write everything
    python gen_healthcare_clinical.py --check      # fail if the tree has drifted
    python gen_healthcare_clinical.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import math
import os
import random
import sys

from PIL import Image, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_healthcare_common as hc  # noqa: E402
import gen_healthcare_wards as hw  # noqa: E402
from gen_healthcare_common import (  # noqa: E402
    clamp, disc, draw_text, draw_text_centred, fill, frame, rect, shade, text_width)

C = hc.catalogue("gen_healthcare_clinical.py", "clinical")
T = C.T
M = C.M
W = hw.C.T  # the wards' shared swatches

el = hw.el
screen = hw.screen
rot = hw.rot
octo = hw.octo
cut = hw.cut
shift = hw.shift
mirror_x = hw.mirror_x
model = hw.model
casters = hw.casters
ITEM_SMALL = hw.ITEM_SMALL
ITEM_TALL = hw.ITEM_TALL
B = hw.B
BOOL = hw.BOOL
FULL = [0, 0, 16, 16]

PC = hc.PATIENT_CARE
CL = hc.CLINICAL
HID = hc.HIDDEN


def MI(name):
    return "csm:block/%s/%s" % (C.model_dir, name)


def front(frm, to, base, key, side="north", faces=hw.ALL):
    """A box whose front (north, or ``side``) shows the whole of a texture."""
    return el(frm, to, base, faces=faces, per={side: key}, uv={side: FULL})


def tex(name, fn):
    C.textures[name] = fn


# ==========================================================================================
# Textures
# ==========================================================================================
for i, (n, c, g) in enumerate((
        ("crash_red", (186, 34, 32), 3), ("o2_tank", hc.O2_GREEN, 3),
        ("laminate", (188, 150, 108), 3), ("solid_surface", (236, 234, 226), 2),
        ("carcass", (214, 210, 200), 2), ("cart_blue", (62, 104, 160), 3),
        ("sling", (34, 38, 46), 2), ("strap", (70, 110, 160), 2),
        ("lift_white", (236, 236, 232), 2), ("hose", (190, 214, 230), 2),
        ("bezel", (40, 42, 46), 1))):
    tex(n, (lambda c=c, g=g, s=3000 + i: hc.swatch(c, s, grain=g)))


def label_plate(w, h, lines, bg, fg, border=None, seed=3100, band=None):
    """A sign drawn at about its own w x h shape (texels), then stretched square: the face
    stretches it back. ``lines`` are (text, scale) centred one under another; ``band`` an
    optional (height, colour, text) header."""
    def f():
        img = fill(bg, size=max(w, h), grain=1, seed=seed).resize((w, h))
        y = 2
        if band:
            bh, bc, bt = band
            rect(img, 0, 0, w, bh, bc)
            draw_text_centred(img, bt, w / 2.0, (bh - 5) // 2, (250, 250, 250))
            y = bh + 2
        total = sum(5 * s + 2 for _t, s in lines) - 2
        y += max(0, (h - y - 2 - total) // 2)
        for text, s in lines:
            draw_text_centred(img, text, w / 2.0, y, fg, scale=s)
            y += 5 * s + 2
        if border:
            frame(img, 0, 0, w, h, border)
        size = 1
        while size < max(w, h):
            size *= 2
        return img.resize((size, size), Image.NEAREST)
    return f


# --- ventilator --------------------------------------------------------------------------
@C.texture("vent_front")
def _vent_front():
    img = fill((226, 228, 224), size=32, grain=1, seed=3201)
    frame(img, 0, 0, 32, 32, (196, 198, 196))
    for cx, label in ((9, "IN"), (23, "EX")):
        disc(img, cx, 18, 4.5, (150, 154, 160))
        disc(img, cx, 18, 2.5, (60, 64, 70))
        draw_text_centred(img, label, cx, 25, (80, 84, 90))
    rect(img, 4, 4, 28, 9, (60, 64, 70))
    rect(img, 5, 5, 12, 8, (90, 200, 120))
    return img


def vent_frame(i):
    def f():
        img = fill(hc.SCREEN_BLACK, size=32, grain=1, seed=3202)
        off = i * 4
        hw._trace(img, [0, 3, 6, 7, 7, 7, 6, 2, 0, 0, 0, 0, 0, 0, 0, 0] * 2, 11,
                  (240, 210, 60), off)
        hw._trace(img, [0, 5, 7, 4, 2, 1, 0, -3, -5, -3, -1, 0, 0, 0, 0, 0] * 2, 22,
                  (90, 220, 120), off)
        rect(img, 22, 0, 23, 32, (40, 44, 48))
        draw_text(img, "18", 24, 3, (240, 210, 60))
        draw_text(img, "5", 24, 13, (230, 230, 230))
        draw_text(img, "40", 24, 23, (90, 200, 240))
        return img
    return f


hc.animation(C, "vent_screen", [vent_frame(i) for i in range(8)], 4)


# --- pendant / boom ----------------------------------------------------------------------
@C.texture("pendant_outlets")
def _pendant_outlets():
    img = fill(hc.WARM_WHITE, size=32, grain=1, seed=3210)
    rect(img, 0, 2, 32, 4, hc.PALE_BLUE)
    for i, c in enumerate((hc.O2_GREEN, hc.AIR_YELLOW, (250, 250, 250))):
        cx = 8 + i * 8
        disc(img, cx, 9, 3.2, (210, 210, 206))
        disc(img, cx, 9, 2.4, c)
        disc(img, cx, 9, 1.1, (50, 50, 54))
    for y in (16, 24):
        for x in (4, 18):
            rect(img, x, y, x + 10, y + 6, (238, 238, 234))
            frame(img, x, y, x + 10, y + 6, (170, 170, 166))
            for dx in (3, 6):
                rect(img, x + dx, y + 2, x + dx + 1, y + 4, (40, 40, 40))
    return img


@C.texture("boom_outlets")
def _boom_outlets():
    img = fill((226, 228, 230), size=32, grain=1, seed=3211)
    for i, c in enumerate((hc.O2_GREEN, hc.AIR_YELLOW, (250, 250, 250), (60, 110, 190))):
        cx = 5 + i * 7.3
        disc(img, cx, 6, 2.6, c)
        disc(img, cx, 6, 1.0, (50, 50, 54))
    for y in (14, 22):
        rect(img, 3, y, 29, y + 5, (238, 238, 234))
        for x in range(6, 29, 6):
            rect(img, x, y + 2, x + 1, y + 4, (40, 40, 40))
    return img


@C.texture("esu_front")
def _esu_front():
    img = fill((70, 76, 86), size=32, grain=1, seed=3212)
    rect(img, 3, 4, 15, 12, (20, 26, 30))
    draw_text(img, "40", 5, 6, (250, 220, 60))
    rect(img, 17, 4, 29, 12, (20, 26, 30))
    draw_text(img, "30", 19, 6, (90, 160, 250))
    for cx, c in ((7, (250, 220, 60)), (16, (90, 160, 250)), (25, (230, 230, 230))):
        disc(img, cx, 22, 3, c)
        disc(img, cx, 22, 1.4, (40, 40, 44))
    return img


@C.texture("insufflator_front")
def _insufflator_front():
    img = fill((226, 228, 230), size=32, grain=1, seed=3213)
    rect(img, 3, 3, 29, 15, (24, 30, 36))
    draw_text(img, "12", 5, 5, (90, 220, 120), scale=2)
    rect(img, 22, 5, 27, 13, (60, 110, 190))
    disc(img, 24, 23, 4, (190, 194, 200))
    disc(img, 24, 23, 2, (60, 64, 70))
    rect(img, 4, 20, 14, 26, (60, 64, 70))
    return img


# --- crash cart, defibrillator ------------------------------------------------------------
@C.texture("crash_drawers")
def _crash_drawers():
    img = fill((186, 34, 32), size=32, grain=2, seed=3220)
    ys = (1, 6, 11, 17, 24, 31)
    for y0, y1 in zip(ys, ys[1:]):
        frame(img, 1, y0, 31, y1, (140, 24, 22))
        rect(img, 12, y0 + 1, 20, y0 + 2, (210, 210, 214))
    # the breakaway seal on the top drawer
    rect(img, 23, 2, 25, 5, (250, 220, 60))
    return img


@C.texture("defib_front")
def _defib_front():
    img = fill((70, 74, 80), size=32, grain=1, seed=3221)
    rect(img, 18, 4, 30, 28, (90, 94, 100))
    disc(img, 24, 10, 3.5, (210, 210, 214))
    disc(img, 24, 10, 1.5, (60, 60, 64))
    disc(img, 24, 21, 3, (236, 120, 40))
    draw_text_centred(img, "1", 24, 19, (250, 250, 250))
    rect(img, 2, 25, 16, 29, (236, 196, 32))
    return img


def defib_frame(i):
    def f():
        img = fill(hc.SCREEN_BLACK, size=32, grain=1, seed=3222)
        hw._trace(img, hw.ECG, 12, (60, 230, 90), i * 4, x1=31)
        draw_text(img, "HR 84", 2, 20, (60, 230, 90))
        draw_text(img, "200J", 2, 26, (250, 220, 60) if i % 4 < 2 else (150, 130, 40))
        return img
    return f


hc.animation(C, "defib_screen", [defib_frame(i) for i in range(8)], 3)


# --- nurses' station ---------------------------------------------------------------------
@C.texture("station_front")
def _station_front():
    """The corridor face: wood-look laminate with a pale blue accent band and a kick strip."""
    img = Image.new("RGBA", (16, 16))
    rng = random.Random(3230)
    px = img.load()
    for y in range(16):
        k = 1.0 + rng.uniform(-0.05, 0.05)
        for x in range(16):
            px[x, y] = shade((188, 150, 108), k * (1 + rng.uniform(-0.02, 0.02))) + (255,)
    rect(img, 0, 3, 16, 5, hc.PALE_BLUE)
    rect(img, 0, 15, 16, 16, (70, 66, 62))
    return img


def chart_frame(i):
    def f():
        img = fill((232, 236, 240), size=16, grain=1, seed=3231)
        rect(img, 0, 0, 16, 3, (50, 90, 150))
        for k, y in enumerate((5, 8, 11, 14)):
            rect(img, 1, y, 1 + (5 + (k * 7 + i * 3) % 9), y + 1, (90, 96, 104))
        rect(img, 0, 4 + 3 * (i % 4), 16, 7 + 3 * (i % 4), (200, 220, 250, 255))
        for k, y in enumerate((5, 8, 11, 14)):
            rect(img, 1, y, 1 + (5 + (k * 7 + i * 3) % 9), y + 1, (90, 96, 104))
        return img
    return f


hc.animation(C, "chart_screen", [chart_frame(i) for i in range(4)], 40)


@C.texture("drawer_bank")
def _drawer_bank():
    img = fill((214, 210, 200), grain=2, seed=3232)
    for y0, y1 in ((0, 5), (5, 10), (10, 16)):
        frame(img, 0, y0, 16, y1, (176, 172, 164))
        rect(img, 6, y0 + 2, 10, y0 + 3, (120, 124, 130))
    return img


# --- patient status board ----------------------------------------------------------------
def status_board():
    """The whole whiteboard, 64 x 32 for a board two blocks wide."""
    img = fill((246, 246, 244), size=64, grain=1, seed=3240).crop((0, 0, 64, 32))
    frame(img, 0, 0, 64, 32, (170, 176, 182), width=2)
    blue, red, black = (40, 80, 170), (190, 40, 40), (50, 50, 56)
    draw_text(img, "PATIENT STATUS", 4, 3, blue)
    cols = (3, 15, 33, 47)
    for x in cols[1:]:
        rect(img, x - 2, 10, x - 1, 29, (190, 196, 204))
    for y in (10, 17, 23):
        rect(img, 2, y, 62, y + 1, (190, 196, 204))
    draw_text(img, "RM", cols[0], 12, black)
    draw_text(img, "PT", cols[1], 12, black)
    draw_text(img, "RN", cols[2], 12, black)
    draw_text(img, "DC", cols[3], 12, black)
    rows = (("12", "JM", "AK", "AM", blue), ("14", "RS", "BT", "PM", red))
    for k, (rm, pt, rn, dc, c) in enumerate(rows):
        y = 18 + k * 6
        draw_text(img, rm, cols[0], y, black)
        draw_text(img, pt, cols[1], y, c)
        draw_text(img, rn, cols[2], y, c)
        draw_text(img, dc, cols[3], y, c)
    # magnets
    rect(img, 27, 19, 30, 22, (90, 190, 110))
    rect(img, 27, 25, 30, 28, (250, 200, 60))
    return img


tex("status_board_left", lambda: status_board().crop((0, 0, 32, 32)))
tex("status_board_right", lambda: status_board().crop((32, 0, 64, 32)))


# --- workstation on wheels, dispensing, medication -----------------------------------------
@C.texture("keyboard")
def _keyboard():
    img = fill((46, 48, 52), grain=1, seed=3250)
    for y in range(2, 14, 3):
        for x in range(1, 15, 2):
            rect(img, x, y, x + 1, y + 2, (120, 124, 130))
    return img


@C.texture("adc_drawers")
def _adc_drawers():
    img = fill((230, 232, 234), size=32, grain=1, seed=3251)
    rows = (2, 8, 14, 20, 26, 31)
    for y0, y1 in zip(rows, rows[1:]):
        for x0, x1 in ((2, 16), (16, 30)):
            frame(img, x0, y0, x1, y1, (176, 180, 186))
            rect(img, x0 + 2, y0 + 2, x0 + 4, y0 + 3, (90, 200, 120))
            rect(img, x0 + 6, y1 - 3, x1 - 3, y1 - 2, (120, 124, 130))
    return img


@C.texture("adc_upper")
def _adc_upper():
    img = fill((230, 232, 234), size=32, grain=1, seed=3252)
    rect(img, 0, 0, 32, 3, hc.PALE_BLUE)
    for x0, x1 in ((2, 15), (17, 30)):
        frame(img, x0, 16, x1, 30, (176, 180, 186))
        disc(img, x1 - 3, 23, 1.2, (230, 160, 40))
    # the badge reader and the biometric pad beside the screen
    rect(img, 25, 6, 29, 12, (60, 64, 70))
    disc(img, 27, 9, 1.2, (90, 200, 240))
    return img


def adc_frame(i):
    def f():
        img = fill((22, 50, 86), size=32, grain=1, seed=3253)
        hc.emblem(img, 16, 8, 4.5, (220, 230, 245))
        draw_text_centred(img, "SCAN", 16, 15, (240, 240, 240))
        draw_text_centred(img, "BADGE", 16, 22, (240, 240, 240))
        if i % 2 == 0:
            rect(img, 10, 29, 22, 30, (90, 200, 240))
        return img
    return f


hc.animation(C, "adc_screen", [adc_frame(i) for i in range(2)], 20)


@C.texture("med_drawers")
def _med_drawers():
    img = fill((236, 238, 240), size=32, grain=1, seed=3254)
    for row in range(5):
        for col in range(4):
            x0, y0 = 1 + col * 7.5, 2 + row * 6
            x0 = int(x0)
            rect(img, x0, y0, x0 + 6, y0 + 5, (62, 104, 160))
            rect(img, x0 + 1, y0 + 1, x0 + 5, y0 + 2, (240, 240, 240))
    return img


@C.texture("fridge_door")
def _fridge_door():
    img = fill((236, 238, 240), size=32, grain=1, seed=3255)
    rect(img, 3, 4, 29, 29, (176, 200, 214))
    for y in (11, 19, 27):
        rect(img, 3, y, 29, y + 1, (226, 230, 234))
        for x in range(4, 28, 3):
            c = ((250, 250, 250), (200, 230, 250), (250, 220, 200))[(x + y) % 3]
            rect(img, x, y - 4, x + 2, y, c)
            rect(img, x, y - 5, x + 2, y - 4, (190, 70, 60) if x % 2 else (70, 110, 190))
    rect(img, 22, 1, 30, 4, (20, 24, 28))
    draw_text(img, "4", 25, 0, (90, 220, 120))
    rect(img, 1, 6, 2, 26, (150, 154, 160))
    return img


# --- mobility ----------------------------------------------------------------------------
@C.texture("wheel")
def _wheel():
    """A wheelchair's push-rim wheel: tyre, rim, spokes, hub (cutout)."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(32):
        for x in range(32):
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            a = math.atan2(y + 0.5 - 16, x + 0.5 - 16)
            if 13.5 <= d < 16:
                px[x, y] = (30, 30, 32, 255)
            elif 12.5 <= d < 13.5:
                px[x, y] = (186, 190, 194, 255)
            elif d < 2.5:
                px[x, y] = (120, 124, 130, 255)
            elif d < 12.5 and abs((a * 6 / math.pi) - round(a * 6 / math.pi)) < 0.6 / max(d, 1):
                px[x, y] = (196, 200, 204, 255)
    return img


@C.texture("caster_wheel")
def _caster_wheel():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    disc(img, 8, 8, 7.5, (30, 30, 32))
    disc(img, 8, 8, 2.5, (150, 154, 160))
    return img


def _wheelchair_glyph(img, x, y, s, c):
    """A generic wheelchair user glyph (the access symbol's shape, drawn here)."""
    disc(img, x + 4 * s, y + 1.5 * s, 1.3 * s, c)
    rect(img, int(x + 3.5 * s), int(y + 3 * s), int(x + 5 * s), int(y + 7 * s), c)
    rect(img, int(x + 3.5 * s), int(y + 6.5 * s), int(x + 8 * s), int(y + 8 * s), c)
    rect(img, int(x + 7 * s), int(y + 6.5 * s), int(x + 8.5 * s), int(y + 10 * s), c)
    px = img.load()
    cx, cy, r = x + 4.5 * s, y + 9 * s, 3.4 * s
    for yy in range(img.height):
        for xx in range(img.width):
            if abs(math.hypot(xx + 0.5 - cx, yy + 0.5 - cy) - r) < 0.55 * s:
                px[xx, yy] = clamp(c) + (255,)


@C.texture("wc_sign_full")
def _wc_sign_full():
    """The corral sign: the wheelchair symbol and WHEEL / CHAIRS in highway capitals, drawn at the
    plate's own shape (14 x 9.5 px, 128 x 87) and stretched square for the face to stretch
    back."""
    img = fill((36, 88, 170), size=128, grain=1, seed=3260).resize((128, 87))
    frame(img, 0, 0, 128, 87, (250, 250, 250), width=3)
    _wheelchair_glyph(img, 9, 13, 4.8, (250, 250, 250))
    for text, y in (("WHEEL", 18), ("CHAIRS", 49)):
        hc.stamp_legend(img, text, 88, y, (250, 250, 250), 20, "D", max_w=72)
    return img.resize((128, 128), Image.NEAREST)


@C.texture("lift_front")
def _lift_front():
    img = fill((236, 236, 232), grain=1, seed=3270)
    rect(img, 4, 3, 12, 7, (30, 36, 42))
    rect(img, 5, 4, 9, 6, (90, 200, 120))
    for x in (4, 9):
        rect(img, x, 9, x + 3, 12, (60, 110, 190))
    rect(img, 0, 14, 16, 16, hc.PALE_BLUE)
    return img


@C.texture("exam_lens_off")
def _exam_lens_off():
    img = fill((200, 202, 200), grain=1, seed=3271)
    for r, n in ((5.5, 10), (3, 6)):
        for k in range(n):
            a = 2 * math.pi * k / n
            disc(img, 8 + math.cos(a) * r, 8 + math.sin(a) * r, 1.1, (160, 162, 160))
    disc(img, 8, 8, 1.2, (160, 162, 160))
    return img


@C.texture("exam_lens_on")
def _exam_lens_on():
    img = fill((255, 252, 236), grain=1, seed=3272)
    for r, n in ((5.5, 10), (3, 6)):
        for k in range(n):
            a = 2 * math.pi * k / n
            disc(img, 8 + math.cos(a) * r, 8 + math.sin(a) * r, 1.2, (255, 255, 255))
    disc(img, 8, 8, 1.3, (255, 255, 255))
    return img


# --- decontamination, ambulance ------------------------------------------------------------
tex("decon_sign", label_plate(32, 18, [("DECON", 1), ("SHOWER", 1)], hc.SAFETY_YELLOW,
                              (30, 30, 34), border=(30, 30, 34), seed=3280))


@C.texture("shower_rose")
def _shower_rose():
    img = fill((196, 200, 204), grain=1, seed=3281)
    for y in range(2, 16, 3):
        for x in range(2, 16, 3):
            img.load()[x, y] = (70, 74, 80, 255)
    return img


def marking():
    """AMBULANCE / ONLY in road-marking yellow on nothing, across two cells (128 x 64)."""
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    yellow = (240, 200, 40)
    # road legends are drawn tall, so they read when seen at a low angle: each line is set at
    # in the highway font at 14 texels and stretched to 28 texels high
    for text, y in (("AMBULANCE", 2), ("ONLY", 33)):
        line = Image.new("RGBA", (128, 14), (0, 0, 0, 0))
        hc.stamp_legend(line, text, 64, 0, yellow, 14, "E", max_w=124)
        line = line.resize((128, 28), Image.LANCZOS)
        # a texel heavier and hard-edged: the floor is seen at a low angle and from afar,
        # where the atlas's mip levels would thin a stroke out of a cutout face
        alpha = line.getchannel("A").filter(ImageFilter.MaxFilter(3)).point(
            lambda v: 255 if v >= 96 else 0)
        line = Image.new("RGBA", line.size, yellow + (255,))
        line.putalpha(alpha)
        img.alpha_composite(line, (0, y))
    return img


@C.texture("amb_sign")
def _amb_sign():
    """AMBULANCE / ONLY, white on red in the highway font, as large as the plate allows
    (14 x 14 px)."""
    img = fill(hc.CALL_RED, size=128, grain=1, seed=3282)
    white = (250, 250, 250)
    frame(img, 0, 0, 128, 128, white, width=3)
    hc.stamp_legend(img, "AMBULANCE", 64, 22, white, 30, "E", max_w=118)
    hc.stamp_legend(img, "ONLY", 64, 64, white, 44, "E", max_w=118)
    return img


tex("mark_left", lambda: marking().crop((0, 0, 64, 64)))
tex("mark_right", lambda: marking().crop((64, 0, 128, 64)))


# --- surgery support -----------------------------------------------------------------------
@C.texture("warmer_door")
def _warmer_door():
    img = fill((200, 214, 222), size=32, grain=1, seed=3290)
    frame(img, 0, 0, 32, 32, (176, 180, 186), width=2)
    for y in (13, 27):
        rect(img, 3, y, 29, y + 1, (186, 190, 194))
        for k in range(4):
            yy = y - 2 - k * 2
            rect(img, 4, yy, 28, yy + 2, (246, 246, 242) if k % 2 == 0 else (226, 234, 240))
    rect(img, 27, 4, 29, 28, (150, 154, 160))
    return img


@C.texture("warmer_display")
def _warmer_display():
    img = fill((60, 64, 70), size=16, grain=1, seed=3291)
    rect(img, 2, 4, 14, 12, (20, 24, 28))
    draw_text(img, "54", 4, 6, (250, 170, 60))
    return img


@C.texture("autoclave_door")
def _autoclave_door():
    img = fill(hc.STAINLESS, grain=1, seed=3292)
    disc(img, 8, 8, 7.4, (150, 154, 160))
    disc(img, 8, 8, 6.4, hc.STAINLESS)
    rect(img, 3, 7, 13, 9, (90, 94, 100))
    disc(img, 8, 8, 1.6, (90, 94, 100))
    return img


def autoclave_frame(i):
    def f():
        img = fill((18, 30, 44), size=16, grain=1, seed=3293)
        draw_text(img, "121", 1, 2, (250, 250, 250))
        draw_text(img, "%d" % (9 - i), 12, 2, (250, 200, 60))
        rect(img, 1, 11, 1 + 2 * (i + 1), 13, (90, 200, 120))
        return img
    return f


hc.animation(C, "autoclave_screen", [autoclave_frame(i) for i in range(7)], 40)


@C.texture("case_cart_front")
def _case_cart_front():
    img = hw.C.textures["stainless"]()
    rect(img, 7, 1, 9, 15, (150, 154, 160))
    for x in (5, 10):
        rect(img, x, 6, x + 1, 10, (90, 94, 100))
    frame(img, 0, 0, 16, 16, (150, 154, 160))
    return img


@C.texture("scrub_window")
def _scrub_window():
    img = fill((196, 212, 222), size=32, grain=1, seed=3295)
    frame(img, 0, 0, 32, 32, (176, 180, 186), width=2)
    for k, size in enumerate(("S", "M", "L", "XL")):
        x = 3 + k * 7
        for j in range(5):
            y = 26 - j * 3
            rect(img, x, y, x + 6, y + 3, (58, 130, 128) if j % 2 == 0 else (74, 150, 148))
        draw_text_centred(img, size, x + 3, 4, (40, 44, 50))
    rect(img, 2, 29, 30, 30, (150, 154, 160))
    return img


@C.texture("dispense_door")
def _dispense_door():
    img = fill((70, 74, 80), size=16, grain=1, seed=3296)
    rect(img, 1, 1, 15, 11, (30, 34, 40))
    rect(img, 3, 12, 13, 14, (150, 154, 160))
    return img


@C.texture("scrub_label")
def _scrub_label():
    img = Image.new("RGBA", (32, 32))
    rect(img, 0, 0, 32, 32, (58, 130, 128))
    draw_text_centred(img, "SCRUBS", 16, 13, (250, 250, 250))
    return img


@C.texture("triage_front")
def _triage_front():
    img = fill((214, 210, 200), size=32, grain=1, seed=3297)
    rect(img, 0, 8, 32, 17, hc.PALE_BLUE)
    draw_text_centred(img, "TRIAGE", 16, 10, (30, 50, 90))
    rect(img, 0, 30, 32, 32, (70, 66, 62))
    return img


# --- ED tracking board ---------------------------------------------------------------------
ED_W, ED_H = 14.0, 9.0          # the screen, in sixteenths (renderer WIDTH, HEIGHT)
ED_HEADER, ED_COLHEAD, ED_PITCH = 1.5, 0.8, 0.95
ED_ROWS = 7
ED_SCREEN_Z = 13.7              # the screen's face (renderer SCREEN_Z)


@C.texture("ed_board_screen")
def _ed_board_screen():
    """The board's screen: header band, column heads' band, zebra rows. Every word is the
    renderer's (TileEntityEdTrackingBoardRenderer, which shares these numbers)."""
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 255))
    k = 128 / ED_W
    rect(img, 0, 0, 128, int(round(ED_H * k)), (10, 18, 30))
    hdr = int(round(ED_HEADER * k))
    rect(img, 0, 0, 128, hdr, (120, 26, 30))
    rect(img, 0, hdr - 1, 128, hdr, (250, 250, 250))
    col = int(round((ED_HEADER + ED_COLHEAD) * k))
    rect(img, 0, hdr, 128, col, (20, 40, 70))
    for i in range(ED_ROWS):
        if i % 2:
            y0 = int(round((ED_HEADER + ED_COLHEAD + i * ED_PITCH) * k))
            y1 = int(round((ED_HEADER + ED_COLHEAD + (i + 1) * ED_PITCH) * k))
            rect(img, 0, y0, 128, y1, (18, 30, 48))
    return img


# ==========================================================================================
# Block helpers (the wards' helpers, bound to this catalogue)
# ==========================================================================================
def facing_block(reg, java, names, textures, elements, tab, extra=None, display=None):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    C.add(reg, java, names, {reg: model(textures, elements, display=display)},
          hc.facing_state(M(reg), extra), tab=tab)


def prop(reg, box6, mat, mount, collides, names, textures, elements, tab, seat=None,
         display=None):
    java = 'new BlockHealthcare("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.%s, %s)' % (
        reg, B(*box6), mat, mount, "true" if collides else "false")
    if seat:
        java += ".withSeat(%g, %g)" % seat
    facing_block(reg, java, names, textures, elements, tab, display=display)


def screen_extra(on_tex, key="screen"):
    return {"on": {"false": {}, "true": {"textures": {key: T(on_tex) if ":" not in on_tex
                                                      else on_tex}}}}


def screen_prop(reg, box6, mat, mount, collides, names, textures, elements, tab, on_tex,
                display=None, java=None):
    java = java or 'new BlockHealthcareScreen("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.%s, %s)' % (
        reg, B(*box6), mat, mount, "true" if collides else "false")
    facing_block(reg, java, names, textures, elements, tab, extra=screen_extra(on_tex),
                 display=display)


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


GREY = W("frame_grey")
STEEL = W("stainless")
RUBBER = W("rubber")

# ==========================================================================================
# Ventilator
# ==========================================================================================
screen_prop("icu_ventilator", (3, 0, 3, 13, 16, 13), "METAL", "FLOOR", True,
            ("ICU Ventilator", "Intensivbeatmungsgerät", "Ventilador de UCI",
             "Intensivvårdsrespirator"),
            {"body": W("warm_white"), "front": T("vent_front"), "metal": STEEL, "grey": GREY,
             "rubber": RUBBER, "bezel": T("bezel"), "screen": W("screen_off"),
             "hose": T("hose")},
            hw.STAND_BASE + casters([(2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)])
            + octo(8, 8, 0.8, 1.4, 10, "metal")
            + [front([3, 10, 4], [13, 17, 12], "body", "front"),
               el([3.5, 17, 5], [12.5, 25, 6.5], "bezel"),
               screen([4.2, 17.7, 4.95], [11.8, 24.3, 4.95], "screen"),
               el([7.5, 17, 6.5], [8.5, 18.5, 9], "grey"),
               # the breathing circuit: out of the ports, down and away to the bed
               el([5.1, 8.5, 2.6], [6.1, 13.4, 3.6], "hose"),
               el([9.9, 8.5, 2.6], [10.9, 13.4, 3.6], "hose"),
               el([5.1, 7.5, 2.6], [10.9, 8.5, 3.6], "hose"),
               el([5.4, 12.8, 3.6], [5.8, 13.2, 4], "hose")],
            CL, "vent_screen", display=ITEM_TALL)

# ==========================================================================================
# Ceiling-hung pieces: the drop column, the ICU pendant, the equipment boom, the exam light
# ==========================================================================================
COLUMN_TEX = {"metal": STEEL, "grey": GREY, "particle": STEEL}
SIDES4 = ("north", "south", "east", "west")
C.add("ceiling_boom_column", "new BlockCeilingColumn()",
      ("Ceiling Boom Column", "Deckenversorgungssäule", "Columna de brazo de techo",
       "Takpendelpelare"),
      {"boom_column": model(COLUMN_TEX, [el([6, 0, 6], [10, 16, 10], "metal", faces=SIDES4)]),
       "boom_column_top": model(COLUMN_TEX, [el([6, 0, 6], [10, 14.5, 10], "metal",
                                                faces=SIDES4),
                                             el([3.5, 14.5, 3.5], [12.5, 16, 12.5], "grey",
                                                faces=SIDES4 + ("down",))])},
      hc.facing_state(M("boom_column"), {"top": {"false": {},
                                                 "true": {"model": M("boom_column_top")}}}),
      tab=HID)


def tube_up(y0):
    """The head's own length of drop tube, from y0 to the top of its cell."""
    return [el([6, y0, 6], [10, 16, 10], "metal", faces=SIDES4)]


HEAD_JAVA = 'new BlockCeilingHead("%s", %s, %d)'


def monitor(x0, y0, z0):
    """The wards' monitor (12 x 9 x 3, screen north), in the monitor body's grey."""
    return [el([x0, y0, z0], [x0 + 12, y0 + 9, z0 + 3], "mbody"),
            screen([x0 + 1, y0 + 1, z0 - 0.05], [x0 + 11, y0 + 8, z0 - 0.05], "screen"),
            el([x0 + 1, y0 + 9, z0 + 0.5], [x0 + 3, y0 + 9.5, z0 + 2.5], "grey")]

screen_prop("icu_pendant", (2, 0, 1, 14, 16, 14), "METAL", "FLOOR", True,
            ("ICU Pendant", "Intensiv-Deckenversorgungseinheit", "Columna de techo de UCI",
             "IVA-takpendel"),
            {"body": W("warm_white"), "outlets": T("pendant_outlets"), "metal": STEEL,
             "grey": GREY, "shelf": STEEL, "screen": W("screen_off"),
             "mbody": W("monitor_body"), "particle": W("warm_white")},
            tube_up(15)
            + [el([5, 0, 5], [11, 15, 11], "body", per={"north": "outlets", "south": "outlets"},
                  uv={"north": FULL, "south": FULL}),
               el([4.5, 15, 4.5], [11.5, 15.6, 11.5], "grey"),
               el([2, 3, 4], [14, 3.8, 13], "shelf"),
               el([7.5, 9.5, 4], [8.5, 10.5, 5], "metal")]
            + monitor(2, 6.5, 1),
            CL, W("monitor_screen"),
            java=HEAD_JAVA % ("icu_pendant", B(2, 0, 1, 14, 16, 14), 1), display=ITEM_TALL)

screen_prop("surgical_equipment_boom", (2, 0, 2, 14, 16, 14), "METAL", "FLOOR", True,
            ("Surgical Equipment Boom", "OP-Geräteampel", "Brazo de equipos quirúrgicos",
             "Operationspendel"),
            {"body": W("warm_white"), "outlets": T("boom_outlets"), "metal": STEEL,
             "grey": GREY, "shelf": STEEL, "device": T("bezel"), "esu": T("esu_front"),
             "insuf": T("insufflator_front"), "screen": W("screen_off"), "mbody": W("monitor_body"),
             "particle": W("warm_white")},
            tube_up(15)
            + [el([5, 0, 6], [11, 15, 11], "body", per={"north": "outlets"},
                  uv={"north": FULL}),
               el([4.5, 15, 5.5], [11.5, 15.6, 11.5], "grey"),
               el([2, 3, 2], [14, 3.7, 11], "shelf"),
               el([2, 8, 2], [14, 8.7, 11], "shelf"),
               front([3, 3.7, 2.5], [13, 7.5, 9], "device", "esu"),
               front([3.5, 8.7, 2.5], [12.5, 11.8, 9], "device", "insuf"),
               el([7.5, 11.8, 4], [8.5, 13, 5], "metal")]
            + monitor(2, 13, 1),
            CL, W("monitor_screen"),
            java=HEAD_JAVA % ("surgical_equipment_boom", B(2, 0, 2, 14, 16, 14), 1),
            display=ITEM_TALL)

EXAM_TEX = {"housing": W("warm_white"), "metal": STEEL, "grey": GREY,
            "lens": T("exam_lens_off"), "particle": W("warm_white")}
C.add("trauma_exam_light",
      'new BlockCeilingHeadLight("trauma_exam_light", %s, 15, 2)' % B(2, 2, 2, 14, 16, 14),
      ("Trauma Bay Exam Light", "Untersuchungsleuchte (Schockraum)",
       "Lámpara de exploración de sala de trauma", "Undersökningslampa (traumarum)"),
      {"trauma_exam_light": model(
          EXAM_TEX,
          tube_up(10)
          + [el([7.2, 7, 7.2], [8.8, 10, 8.8], "grey"),
             el([2.5, 7, 7.4], [13.5, 8, 8.6], "metal"),
             el([2.5, 5, 7.4], [3.5, 7, 8.6], "metal"),
             el([12.5, 5, 7.4], [13.5, 7, 8.6], "metal")]
          + octo(8, 8, 5.2, 4, 6.5, "housing")
          + [el([3.6, 3.95, 3.6], [12.4, 4, 12.4], "lens", faces=("down",), uv={"down": FULL}),
             el([7.4, 2.2, 7.4], [8.6, 3.95, 8.6], "grey")],
          display=ITEM_SMALL)},
      hw.light_state(M("trauma_exam_light"), {"lens": T("exam_lens_on")}), tab=CL)

# ==========================================================================================
# Crash cart and defibrillator
# ==========================================================================================
DEFIB_TEX = {"defib": T("defib_front"), "dark": W("dark_grey"), "screen": W("screen_off"),
             "grey": GREY, "particle": W("dark_grey")}


def defib(x0, y0, z0):
    """A defibrillator/monitor 10 wide, 4.5 tall, 6 deep, its corner at (x0, y0, z0)."""
    return [front([x0, y0, z0], [x0 + 10, y0 + 4.5, z0 + 6], "dark", "defib"),
            screen([x0 + 1, y0 + 1.2, z0 - 0.05], [x0 + 5.2, y0 + 4, z0 - 0.05], "screen"),
            el([x0 + 3, y0 + 4.5, z0 + 2], [x0 + 7, y0 + 5.2, z0 + 3], "grey"),
            el([x0 + 10, y0 + 0.5, z0 + 1], [x0 + 11, y0 + 3.5, z0 + 5], "grey")]


screen_prop("crash_cart", (1, 0, 3, 15, 16, 13), "METAL", "FLOOR", True,
            ("Crash Cart", "Notfallwagen", "Carro de paradas", "Akutvagn"),
            dict(DEFIB_TEX, body=T("crash_red"), drawers=T("crash_drawers"), rubber=RUBBER,
                 board=W("charcoal"), tank=T("o2_tank"), particle=T("crash_red")),
            casters([(2.5, 4.5), (13.5, 4.5), (2.5, 11.5), (13.5, 11.5)])
            + [front([1.5, 1, 3.5], [14.5, 13, 12.5], "body", "drawers"),
               el([1, 13, 3], [15, 13.8, 13], "grey"),
               el([14.5, 3, 4.5], [15.2, 12.5, 11.5], "board")]
            + octo(1.6, 8, 1.1, 2, 11, "tank")
            + defib(2.5, 13.8, 5),
            CL, "defib_screen")

screen_prop("defibrillator", (3, 0, 3, 13, 16, 13), "METAL", "FLOOR", True,
            ("Defibrillator (Rolling Stand)", "Defibrillator (Rollstativ)",
             "Desfibrilador (soporte rodante)", "Defibrillator (rullstativ)"),
            dict(DEFIB_TEX, metal=STEEL, rubber=RUBBER),
            hw.STAND_BASE + casters([(2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)])
            + octo(8, 8, 0.6, 1.4, 9, "metal")
            + [el([2.5, 9, 4], [13.5, 9.6, 12], "grey")]
            + defib(3, 9.6, 5),
            CL, "defib_screen")

# ==========================================================================================
# Nurses' station
# ==========================================================================================
ST_TEX = {"front": T("station_front"), "top": T("solid_surface"), "carcass": T("carcass"),
          "drawers": T("drawer_bank"), "body": W("monitor_body"), "chart": T("chart_screen"),
          "grey": GREY, "particle": T("carcass")}

ST_BODY = [el([0, 0, 1.5], [16, 15, 2.5], "front", faces=("north", "south")),
           el([0, 15, 0], [16, 16, 6], "top"),
           el([0, 11, 5.5], [16, 15, 6], "carcass", faces=("south",)),
           el([0, 11, 2.5], [16, 11.75, 15], "top"),
           el([10, 0, 3], [15.5, 11, 14], "carcass", per={"south": "drawers"},
              uv={"south": FULL})]
ST_END = [el([15.25, 0, 1.5], [16, 15, 15], "carcass"),
          el([15.25, 15, 0], [16, 16, 6], "top", faces=("east",))]


def st_monitor_body(x0):
    """A charting monitor on the work surface, facing the nurse (south), without its screen."""
    return [el([x0 + 2, 11.75, 9], [x0 + 4, 12.2, 11], "grey"),
            el([x0 + 2.6, 12.2, 10.2], [x0 + 3.4, 13.5, 10.8], "grey"),
            el([x0, 13, 10], [x0 + 6, 17, 10.6], "body")]


# The two screens (left and right as the nurse sees them, facing north from the back of the
# counter): x from, x to; y from, y to; z of the face.
ST_SCREENS = ((1.9, 7.1), (8.9, 14.1))
ST_SCREEN_Y = (13.4, 16.6)
ST_SCREEN_Z = 10.65


def st_screen(i, tex, uv=FULL):
    (x0, x1), (y0, y1) = ST_SCREENS[i], ST_SCREEN_Y
    return el([x0, y0, ST_SCREEN_Z], [x1, y1, ST_SCREEN_Z], tex, faces=("south",),
              uv={"south": uv})


def st_monitor(x0):
    """A charting monitor on the work surface with its chart on."""
    return st_monitor_body(x0) + [st_screen(0 if x0 < 8 else 1, "chart")]


ST_KBD = [el([3, 11.75, 4], [10, 12.1, 7], "body", per={"up": "kbd"}, uv={"up": FULL})]
ST_MON = st_monitor(1.5) + st_monitor(8.5) + ST_KBD

# The call screen: the station_call texture's top half is the left screen (rooms 1-3), its bottom
# half the right one (rooms 4-6), each a CALL band over three room cells. SHARED by the
# texture, the screens' UVs and the room overlays: a cell's texels in its half.
CALL_BAND = 7
CALL_CELLS = [(1 + 10 * c, CALL_BAND, 10 + 10 * c, 15) for c in range(3)]
ST_CALL_GLOW = 0.05  # the lit room cell in front of the screen


def st_room_overlay(i):
    """Room i's lit cell (0-5): a quad over its cell of the call screen."""
    half, c = divmod(i, 3)
    tx0, ty0, tx1, ty1 = CALL_CELLS[c]
    (x0, x1), (y0, y1) = ST_SCREENS[half], ST_SCREEN_Y
    w, h = x1 - x0, y1 - y0
    frm = [round(x0 + w * tx0 / 32.0, 4), round(y1 - h * ty1 / 16.0, 4),
           ST_SCREEN_Z + ST_CALL_GLOW]
    to = [round(x0 + w * tx1 / 32.0, 4), round(y1 - h * ty0 / 16.0, 4),
          ST_SCREEN_Z + ST_CALL_GLOW]
    return el(frm, to, "lit", faces=("south",),
              uv={"south": [tx0 / 2.0, (ty0 + 16 * half) / 2.0, tx1 / 2.0,
                            (ty1 + 16 * half) / 2.0]})


def _call_screen(lit, flash):
    img = Image.new("RGBA", (32, 32), (16, 20, 26, 255))
    for half in range(2):
        oy = 16 * half
        if not lit:
            band = hc.CALL_RED if flash else (96, 26, 22)
            rect(img, 0, oy, 32, oy + CALL_BAND - 1, band)
            hc.draw_text_centred(img, "CALL", 16, oy + 1,
                                 hc.CALL_WHITE if flash else (176, 120, 112))
        for c, (x0, y0, x1, y1) in enumerate(CALL_CELLS):
            rect(img, x0, oy + y0, x1, oy + y1, hc.CALL_AMBER if lit else (40, 46, 54))
            hc.draw_text_centred(img, str(3 * half + c + 1), (x0 + x1) / 2.0, oy + y0 + 2,
                                 (40, 26, 8) if lit else (122, 128, 136))
    return img


hc.animation(C, "station_call", [lambda: _call_screen(False, True),
                                 lambda: _call_screen(False, False)], 10)


@C.texture("station_call_lit")
def _station_call_lit():
    return _call_screen(True, False)


def station_state(parts):
    out = []
    for f, y in hc.ROT_Y.items():
        out.append({"when": {"facing": f}, "apply": {"model": M(parts[0]), "y": y}})
        out.append({"when": {"facing": f, "east": "false"},
                    "apply": {"model": M("station_end_east"), "y": y}})
        out.append({"when": {"facing": f, "west": "false"},
                    "apply": {"model": M("station_end_west"), "y": y}})
    return {"multipart": out}


ST_NAMES = {
    "nurse_station_counter": ("Nurses' Station Counter", "Stationstresen",
                              "Mostrador del control de enfermería", "Expeditionsdisk"),
    "nurse_station_counter_monitors": ("Nurses' Station Counter (Monitors)",
                                       "Stationstresen (Monitore)",
                                       "Mostrador del control de enfermería (monitores)",
                                       "Expeditionsdisk (skärmar)"),
    "nurse_station_corner": ("Nurses' Station Corner", "Stationstresen (Ecke)",
                             "Esquina del control de enfermería", "Expeditionsdisk (hörn)"),
}
ST_BOX = (0, 0, 0, 16, 16, 16)
st_tex = dict(ST_TEX, kbd=T("keyboard"))
C.add("nurse_station_counter",
      'new BlockNurseStation("nurse_station_counter", %s, false)' % B(*ST_BOX),
      ST_NAMES["nurse_station_counter"],
      {"station_body": model(st_tex, ST_BODY),
       "station_end_east": model(st_tex, ST_END),
       "station_end_west": model(st_tex, mirror_x(ST_END)),
       "station_item": model(st_tex, ST_BODY + ST_END + mirror_x(ST_END), display=ITEM_SMALL)},
      station_state(["station_body"]), item={"parent": MI("station_item")}, tab=CL)
# The counter with monitors shows its master panel's calls (BlockNurseStationMonitors): the
# room lamps are actual state from its tile entity, room1..room6 as the panel's. With no call the
# screens chart; with any, both show the CALL screen, and each calling room's cell is lit by an
# overlay part.
def monitors_models():
    call_tex = dict(st_tex, call=T("station_call"), lit=T("station_call_lit"))
    out = {"station_monitors": model(st_tex, st_monitor_body(1.5) + st_monitor_body(8.5)
                                     + ST_KBD),
           "station_monitors_chart": model(st_tex, [st_screen(0, "chart"),
                                                    st_screen(1, "chart")]),
           "station_monitors_call": model(call_tex, [st_screen(0, "call", [0, 0, 16, 8]),
                                                     st_screen(1, "call", [0, 8, 16, 16])]),
           "station_monitors_item": model(st_tex, ST_BODY + ST_END + mirror_x(ST_END) + ST_MON,
                                          display=ITEM_SMALL)}
    for i in range(6):
        out["station_call_room%d" % (i + 1)] = model(call_tex, [st_room_overlay(i)])
    return out


def monitors_state():
    parts = station_state(["station_body"])["multipart"]
    for f, y in hc.ROT_Y.items():
        parts.append({"when": {"facing": f}, "apply": {"model": M("station_monitors"), "y": y}})
        quiet = {"facing": f}
        quiet.update({"room%d" % (i + 1): "false" for i in range(6)})
        parts.append({"when": quiet, "apply": {"model": M("station_monitors_chart"), "y": y}})
        parts.append({"when": {"OR": [{"facing": f, "room%d" % (i + 1): "true"}
                                      for i in range(6)]},
                      "apply": {"model": M("station_monitors_call"), "y": y}})
        for i in range(6):
            parts.append({"when": {"facing": f, "room%d" % (i + 1): "true"},
                          "apply": {"model": M("station_call_room%d" % (i + 1)), "y": y}})
    return {"multipart": parts}


C.add("nurse_station_counter_monitors",
      'new BlockNurseStationMonitors(%s)' % B(*ST_BOX),
      ST_NAMES["nurse_station_counter_monitors"],
      monitors_models(), monitors_state(),
      item={"parent": MI("station_monitors_item")}, tab=CL)

ST_CORNER = [el([0, 0, 1.5], [16, 15, 2.5], "front", faces=("north", "south", "east")),
             el([1.5, 0, 2.5], [2.5, 15, 16], "front", faces=("west", "east"),
                per={"west": "front", "east": "front"}),
             el([0, 15, 0], [16, 16, 6], "top"),
             el([0, 15, 6], [6, 16, 16], "top"),
             el([6, 11, 5.5], [16, 15, 6], "carcass", faces=("south",)),
             el([5.5, 11, 6], [6, 15, 16], "carcass", faces=("east",)),
             el([2.5, 11, 6], [5.5, 11.75, 16], "top"),
             el([2.5, 11, 2.5], [16, 11.75, 6], "top"),
             el([6, 11, 6], [16, 11.75, 16], "top")]
C.add("nurse_station_corner",
      'new BlockNurseStation("nurse_station_corner", %s, true)' % B(*ST_BOX),
      ST_NAMES["nurse_station_corner"],
      {"station_corner": model(st_tex, ST_CORNER, display=ITEM_SMALL)},
      hc.facing_state(M("station_corner"), {"east": {"false": {}, "true": {}},
                                            "west": {"false": {}, "true": {}}}), tab=CL)

# ==========================================================================================
# Status board, workstation on wheels, dispensing, medication
# ==========================================================================================
pair_block("patient_status_board",
           ("Patient Status Board", "Patienten-Statustafel", "Pizarra de estado de pacientes",
            "Patientstatustavla"),
           [el([0, 0, 15], [16, 16, 16], "board", per={"north": "left"}, uv={"north": FULL}),
            el([-16, 0, 15], [0, 16, 16], "board", per={"north": "right"}, uv={"north": FULL}),
            el([-12, 0.5, 13.5], [12, 1.2, 15], "grey")],
           0, {"board": W("white"), "left": T("status_board_left"),
               "right": T("status_board_right"), "grey": GREY, "particle": W("white")},
           pair_java("patient_status_board", (0, 0, 13, 16, 16, 16), "PLASTIC", "WALL", "WIDE",
                     False), CL)

screen_prop("workstation_on_wheels", (2, 0, 2, 14, 16, 14), "METAL", "FLOOR", True,
            ("Workstation on Wheels", "Visitenwagen mit Computer",
             "Estación de trabajo móvil", "Datorvagn"),
            {"metal": STEEL, "grey": GREY, "rubber": RUBBER, "body": W("monitor_body"),
             "kbd": T("keyboard"), "screen": W("screen_off"), "battery": W("dark_grey"),
             "particle": GREY},
            hw.STAND_BASE + casters([(2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)])
            + [el([5, 1.4, 6], [11, 5, 10], "battery")]
            + octo(8, 8, 0.8, 5, 13, "metal")
            + [el([2, 13, 2.5], [14, 13.6, 11], "grey"),
               el([3.5, 13.6, 3], [12.5, 14, 8], "body", per={"up": "kbd"}, uv={"up": FULL}),
               el([3.5, 14, 8.5], [12.5, 21, 9.5], "body"),
               screen([4, 14.6, 8.45], [12, 20.4, 8.45], "screen"),
               el([12.5, 12, 4], [14.5, 13, 6], "grey")],
            CL, T("chart_screen"), display=ITEM_TALL)


# --- portable ultrasound cart ---------------------------------------------------------------
@C.texture("us_panel")
def _us_panel():
    """The ultrasound console's top: keys along the back, the touch screen, knobs and the
    depth-gain sliders, and the trackball nearest the sonographer. Drawn as seen from above with
    the sonographer at the bottom, then turned half round: an up face's top edge is north, and
    the sonographer stands to the north of the cart."""
    img = fill((58, 62, 68), size=32, grain=1, seed=3330)
    frame(img, 0, 0, 32, 32, (40, 42, 46))
    # the touch screen across the back
    rect(img, 4, 2, 28, 9, (22, 26, 30))
    rect(img, 5, 3, 27, 8, (52, 92, 128))
    for x in (6, 12, 18, 24):
        rect(img, x, 4, x + 3, 7, (120, 170, 210))
    # a row of keys under it, and the depth-gain sliders on the right
    for x in range(3, 19, 3):
        rect(img, x, 11, x + 2, 13, (150, 154, 160))
    for k, x in enumerate(range(21, 30, 2)):
        rect(img, x, 11, x + 1, 19, (34, 36, 40))
        rect(img, x, 13 + (k * 3) % 5, x + 1, 15 + (k * 3) % 5, (220, 220, 214))
    # knobs, a green freeze key, and the trackball in its well
    # knobs as squares: a small disc draws as a plus at the mip levels
    for cx in (4, 9, 14):
        rect(img, cx, 16, cx + 3, 19, (180, 184, 188))
        rect(img, cx + 1, 17, cx + 2, 18, (110, 114, 120))
    rect(img, 3, 22, 7, 25, (70, 160, 100))
    disc(img, 16, 25, 4.2, (34, 36, 40))
    disc(img, 16, 25, 3.0, (150, 156, 166))
    disc(img, 15, 24, 1.0, (210, 214, 220))
    rect(img, 22, 22, 26, 25, (150, 154, 160))
    rect(img, 22, 26, 26, 29, (150, 154, 160))
    return img.rotate(180)


@C.texture("us_tower")
def _us_tower():
    """The cart's body, front: a storage drawer, the printer's paper slot and vents."""
    img = fill(hc.WARM_WHITE, grain=2, seed=3331)
    frame(img, 0, 0, 16, 16, shade(hc.WARM_WHITE, 0.84))
    rect(img, 2, 2, 14, 6, shade(hc.WARM_WHITE, 0.9))
    rect(img, 4, 3, 12, 4, (30, 32, 36))
    rect(img, 2, 8, 14, 13, shade(hc.WARM_WHITE, 0.94))
    rect(img, 6, 10, 10, 11, (120, 124, 130))
    for x in range(3, 14, 2):
        rect(img, x, 14, x + 1, 15, (150, 150, 146))
    return img


@C.texture("us_gel")
def _us_gel():
    img = fill((150, 196, 226), grain=2, seed=3332)
    rect(img, 0, 0, 16, 4, (240, 240, 236))
    rect(img, 2, 8, 14, 12, (250, 250, 250))
    rect(img, 4, 9, 12, 10, (60, 110, 170))
    return img


def us_frame(i):
    """One frame of the ultrasound image: a grey-scale sector fanning down from the probe at the
    top, tissue layers, a dark oval (a fluid-filled structure) with a bright back wall and the
    brighter band behind it, and the speckle changing frame to frame as a probe held still
    shows it. Depth marks down the right edge."""
    def f():
        size = 32
        img = Image.new("RGBA", (size, size), hc.SCREEN_BLACK + (255,))
        px = img.load()
        rng = random.Random(3340 + i)
        ax, ay = 15.5, 2.0
        half = math.radians(38)
        breathe = 0.5 * math.sin(i * math.pi / 3)
        ox, oy, rx, ry = 16.5, 17.5 + breathe, 5.0, 3.4
        for y in range(size):
            for x in range(size):
                dx, dy = x + 0.5 - ax, y + 0.5 - ay
                r = math.hypot(dx, dy)
                if r < 4 or r > 27 or abs(math.atan2(dx, dy)) > half:
                    continue
                v = 70 + 50 * math.exp(-((r - 8) / 1.6) ** 2) - 1.2 * (r - 4)
                if 13 < r < 14.5:
                    v += 25
                e = ((x + 0.5 - ox) / rx) ** 2 + ((y + 0.5 - oy) / ry) ** 2
                if e < 1:
                    v = 12
                elif e < 1.45 and y + 0.5 > oy:
                    v += 55
                elif abs(x + 0.5 - ox) < rx * 0.8 and y + 0.5 > oy:
                    v += 22
                v *= rng.uniform(0.55, 1.35)
                g = int(clamp((v,))[0])
                px[x, y] = (g, g, g, 255)
        for y in range(6, 30, 4):
            rect(img, 30, y, 31, y + 1, (200, 200, 190))
        rect(img, 1, 1, 4, 2, (240, 200, 60))
        return img
    return f


hc.animation(C, "us_screen", [us_frame(i) for i in range(6)], 5)

screen_prop("ultrasound_cart", (2, 0, 2, 14, 16, 14), "METAL", "FLOOR", True,
            ("Portable Ultrasound Cart", "Mobiles Ultraschallgerät",
             "Ecógrafo portátil con carro", "Mobil ultraljudsvagn"),
            {"body": W("warm_white"), "front": T("us_tower"), "panel": W("dark_grey"),
             "top": T("us_panel"), "metal": STEEL, "grey": GREY, "rubber": RUBBER,
             "bezel": T("bezel"), "screen": W("screen_off"), "probe": W("white"),
             "cable": W("charcoal"), "gel": T("us_gel"), "particle": W("warm_white")},
            casters([(3.5, 3.5), (12.5, 3.5), (3.5, 12.5), (12.5, 12.5)])
            + [el([2.5, 0.8, 2.5], [13.5, 2.2, 13.5], "body"),
               front([4.5, 2.2, 6], [11.5, 12.5, 12.5], "body", "front"),
               # the console, its top the keyboard, trackball and touch screen
               el([2, 12.5, 2], [14, 13.8, 11.5], "panel", per={"up": "top"},
                  uv={"up": FULL}),
               # the handle across the front
               el([3, 13, 0.6], [13, 13.8, 1.4], "metal"),
               el([3, 13, 1.4], [4, 13.8, 2], "metal"),
               el([12, 13, 1.4], [13, 13.8, 2], "metal"),
               # probe holders on the sonographer's left, the probes standing in them, their
               # cables looped down to a hook; the gel bottle in its cup on the right. The
               # sonographer stands at the front (north, z 0) facing south, so their left is +x
               el([14, 10.5, 3], [15.2, 13.2, 9], "grey"),
               el([14.25, 13.2, 3.6], [14.95, 15.4, 4.6], "probe"),
               el([14.25, 13.2, 5.6], [14.95, 15, 6.4], "probe"),
               el([14.25, 13.2, 7.4], [14.95, 15.6, 8.4], "probe"),
               el([14.4, 6, 4], [14.8, 10.5, 4.4], "cable"),
               el([14.4, 6, 7.8], [14.8, 10.5, 8.2], "cable"),
               el([14.4, 5.6, 4], [14.8, 6, 8.2], "cable"),
               el([0.8, 10.5, 7], [2, 13, 10], "grey"),
               el([1, 13, 7.6], [1.8, 15.5, 9.4], "gel"),
               # the monitor on its arm above the back of the console
               el([7.3, 13.8, 10], [8.7, 17.5, 11.4], "metal"),
               el([2.5, 16.5, 10], [13.5, 24, 11], "bezel"),
               screen([3, 17, 9.95], [13, 23.5, 9.95], "screen")],
            CL, T("us_screen"), display=ITEM_TALL)

ADC_WHOLE = [front([0.5, 0, 2], [15.5, 16, 16], "body", "drawers"),
             front([0.5, 16, 2], [15.5, 30, 16], "body", "upper"),
             el([0.25, 30, 1.75], [15.75, 31, 16], "grey"),
             el([2.5, 18.5, 1.4], [11.5, 25.5, 2], "bezel"),
             el([3, 19, 1.35], [11, 25, 1.35], "screen", faces=("north",), uv={"north": FULL}),
             el([2, 16.5, -1], [14, 17.2, 2], "grey"),
             el([3, 17.2, -0.5], [11, 17.5, 1.5], "bezel", per={"up": "kbd"},
                uv={"up": FULL})]
pair_block("medication_dispensing_cabinet",
           ("Automated Dispensing Cabinet", "Automatisierter Medikamentenschrank",
            "Armario dispensador automatizado", "Automatiserat läkemedelsskåp"),
           ADC_WHOLE, 1,
           {"body": T("carcass"), "drawers": T("adc_drawers"), "upper": T("adc_upper"),
            "grey": GREY, "bezel": T("bezel"), "screen": T("adc_screen"), "kbd": T("keyboard"),
            "particle": T("carcass")},
           pair_java("medication_dispensing_cabinet", (0, 0, 2, 16, 16, 16), "METAL", "FLOOR",
                     "TALL"), CL, item_display=ITEM_TALL)

prop("medication_cart", (1, 0, 2, 15, 16, 14), "PLASTIC", "FLOOR", True,
     ("Medication Cart", "Medikamentenwagen", "Carro de medicación", "Läkemedelsvagn"),
     {"body": W("white"), "front": T("med_drawers"), "grey": GREY, "rubber": RUBBER,
      "blue": T("cart_blue")},
     casters([(2.5, 3.5), (13.5, 3.5), (2.5, 12.5), (13.5, 12.5)])
     + [front([1.5, 1, 3], [14.5, 13.5, 13], "body", "front"),
        el([1, 13.5, 2.5], [15, 14.3, 13.5], "blue"),
        el([14.5, 9, 5], [15.5, 10, 11], "grey"),
        el([14.8, 10, 5.5], [15.4, 12, 6.5], "grey"), el([14.8, 10, 9.5], [15.4, 12, 10.5],
                                                          "grey")], CL)

prop("medication_fridge", (1, 0, 2, 15, 15, 16), "METAL", "FLOOR", True,
     ("Medication Refrigerator", "Medikamentenkühlschrank", "Refrigerador de medicamentos",
      "Läkemedelskylskåp"),
     {"body": W("white"), "front": T("fridge_door"), "grey": GREY},
     [front([1, 0.6, 2], [15, 15, 16], "body", "front"),
      el([1.5, 0, 3], [14.5, 0.6, 15], "grey")], CL)

# ==========================================================================================
# Mobility
# ==========================================================================================
WC_TEX = {"sling": T("sling"), "metal": STEEL, "wheel": T("wheel"), "caster": T("caster_wheel"),
          "rubber": RUBBER, "grey": GREY, "particle": T("sling")}


def wheel(x, cy, cz, r):
    """A wheel on the x plane: one cutout card, drawn both sides."""
    return [el([x, cy - r, cz - r], [x + 0.01, cy + r, cz + r], "wheel", faces=("east", "west"),
               uv={"east": FULL, "west": FULL})]


def wheelchair(x0=0.0, folded=False):
    els = []
    if folded:
        # folded flat: the two big wheels close together, the sling hanging between
        for x in (x0 + 0.5, x0 + 3.5):
            els += wheel(x, 6, 9, 6)
        els += [el([x0 + 1, 2, 3], [x0 + 3, 13, 4], "sling"),
                el([x0 + 1.2, 6, 3.5], [x0 + 2.8, 6.6, 13], "metal"),
                el([x0 + 1.5, 13, 12], [x0 + 2.5, 15, 15], "rubber"),
                el([x0 + 1, 0, 2], [x0 + 3, 2.5, 3], "metal")]
        return els
    for x in (2.6, 13.4):
        els += wheel(x, 6, 10, 6)
    for x in (4.5, 11):
        els += [el([x, 0, 2.5], [x + 0.5, 2.6, 4.5], "caster", faces=("east", "west"),
                   uv={"east": FULL, "west": FULL}),
                el([x - 0.1, 2.6, 3], [x + 0.6, 7, 3.6], "metal")]
    els += [el([3.6, 6.5, 3], [12.4, 7.2, 12], "sling"),
            el([3.8, 7.2, 11.6], [12.2, 15, 12.1], "sling"),
            el([12.2, 6.3, 3], [12.8, 7, 12], "metal"),
            el([3.2, 6.3, 3], [3.8, 7, 12], "metal"),
            el([3.2, 7, 11.6], [3.8, 15.5, 12.2], "metal"),
            el([12.2, 7, 11.6], [12.8, 15.5, 12.2], "metal"),
            el([3.2, 15, 12.2], [3.8, 15.6, 14.5], "rubber"),
            el([12.2, 15, 12.2], [12.8, 15.6, 14.5], "rubber"),
            el([3.2, 10, 4.5], [3.8, 10.6, 11], "sling"),
            el([12.2, 10, 4.5], [12.8, 10.6, 11], "sling"),
            el([3.4, 7, 4.5], [3.6, 10, 5], "metal"), el([12.4, 7, 4.5], [12.6, 10, 5], "metal"),
            el([4.5, 2.5, 0.8], [7.5, 3, 3], "grey"), el([8.5, 2.5, 0.8], [11.5, 3, 3], "grey"),
            el([5, 3, 2.5], [5.5, 6.5, 3], "metal"), el([10.5, 3, 2.5], [11, 6.5, 3], "metal")]
    return els


prop("wheelchair", (2, 0, 1, 14, 16, 15), "METAL", "FLOOR", True,
     ("Wheelchair", "Rollstuhl", "Silla de ruedas", "Rullstol"),
     WC_TEX, wheelchair(), PC, seat=(7.2, 0))

prop("wheelchair_storage_rack", (0, 0, 1, 16, 16, 16), "METAL", "FLOOR", True,
     ("Wheelchair Storage Rack", "Rollstuhl-Abstellständer", "Soporte para sillas de ruedas",
      "Rullstolsställ"),
     WC_TEX,
     wheelchair(1, True) + wheelchair(6, True) + wheelchair(11, True)
     + [el([0, 0, 15], [16, 1, 16], "grey"), el([0, 12, 15], [16, 13, 16], "grey"),
        el([0, 1, 15], [1, 12, 16], "grey"), el([15, 1, 15], [16, 12, 16], "grey"),
        el([0, 0, 1], [16, 0.5, 2], "grey")], PC)

prop("wheelchair_corral_sign", (1, 3, 15, 15, 13, 16), "PLASTIC", "WALL", False,
     ("Wheelchair Corral Sign", "Schild Rollstuhlstellplatz", "Señal de aparcamiento de sillas"
      " de ruedas", "Skylt för rullstolsparkering"),
     {"sign": T("wc_sign_full"), "body": W("white")},
     [el([1, 3.25, 15.4], [15, 12.75, 16], "body", faces=("south", "east", "west", "up", "down")),
      screen([1, 3.25, 15.4], [15, 12.75, 15.4], "sign")], PC)

WALKER = []
for _x in (3, 12.4):
    WALKER += [el([_x, 0.8, 4], [_x + 0.6, 13, 4.6], "metal"),
               el([_x, 0, 11.4], [_x + 0.6, 13, 12], "metal"),
               el([_x - 0.1, 0, 11.3], [_x + 0.7, 0.8, 12.1], "rubber"),
               el([_x + 0.05, 13, 4], [_x + 0.55, 13.5, 12], "metal"),
               el([_x - 0.1, 13.4, 6], [_x + 0.7, 14.2, 10], "rubber"),
               el([_x + 0.05, 0, 3], [_x + 0.55, 1.6, 4.6], "caster", faces=("east", "west"),
                  uv={"east": FULL, "west": FULL})]
WALKER += [el([3.6, 8, 4.1], [12.4, 8.6, 4.5], "metal")]
prop("walker", (2, 0, 3, 14, 15, 13), "METAL", "FLOOR", True,
     ("Walker", "Gehgestell", "Andador", "Gåbock"),
     WC_TEX, WALKER, PC)

LIFT_WHOLE = ([el([3, 0.8, 1], [4.5, 2.2, 15], "white"), el([11.5, 0.8, 1], [13, 2.2, 15], "white"),
               el([3, 0.8, 12], [13, 2.2, 15], "white")]
              + casters([(3.75, 1.5), (12.25, 1.5), (3.75, 14.3), (12.25, 14.3)])
              + [el([7, 2.2, 12], [9, 26, 14], "white"),
                 el([6.5, 12, 14], [9.5, 16, 15.5], "panel", per={"south": "front"},
                    uv={"south": FULL}),
                 el([7.4, 8, 10.8], [8.6, 22, 12], "grey"),
                 el([7.2, 24, 3], [8.8, 26, 12], "white"),
                 el([7.8, 19, 3.3], [8.2, 24, 3.7], "strap"),
                 el([3, 18, 2.6], [13, 19, 4.4], "grey"),
                 el([3, 15, 3.3], [3.4, 18, 3.7], "strap"),
                 el([12.6, 15, 3.3], [13, 18, 3.7], "strap")])
pair_block("patient_lift_mobile",
           ("Mobile Patient Lift", "Mobiler Patientenlifter", "Grúa de pacientes móvil",
            "Mobil patientlyft"),
           LIFT_WHOLE, 1,
           {"white": T("lift_white"), "panel": T("lift_white"), "front": T("lift_front"),
            "grey": GREY, "strap": T("strap"), "rubber": RUBBER, "particle": T("lift_white")},
           pair_java("patient_lift_mobile", (2, 0, 0, 14, 16, 16), "METAL", "FLOOR", "TALL"),
           PC, item_display=ITEM_TALL)

CEILING_LIFT = [el([0, 0, 0], [16, 1, 16], "tile"),
                el([6.5, -1.4, 0], [9.5, 0, 16], "white", faces=("east", "west", "down")),
                el([4, -7.5, 4], [12, -1.4, 12], "white", per={"north": "front"},
                   uv={"north": FULL}),
                el([7.5, -14.5, 7.5], [8.5, -7.5, 8.5], "strap"),
                el([2, -15.5, 7], [14, -14.5, 9], "grey"),
                el([2, -16, 7.6], [2.6, -15.5, 8.4], "grey"),
                el([13.4, -16, 7.6], [14, -15.5, 8.4], "grey")]
prop("ceiling_patient_lift", (0, 0, 0, 16, 1, 16), "METAL", "FLOOR", True,
     ("Ceiling Patient Lift", "Deckenlifter", "Grúa de techo para pacientes",
      "Taklyft"),
     {"tile": W("ceiling_tile"), "white": T("lift_white"), "front": T("lift_front"),
      "strap": T("strap"), "grey": GREY, "particle": W("ceiling_tile")},
     CEILING_LIFT, PC,
     display=dict(ITEM_SMALL, gui={"rotation": [30, 225, 0], "translation": [0, 4, 0],
                                   "scale": [0.42] * 3}))

# ==========================================================================================
# Emergency department
# ==========================================================================================
screen_prop("triage_desk", (0, 0, 1, 16, 16, 16), "WOOD", "FLOOR", True,
            ("Triage Desk", "Triage-Arbeitsplatz", "Mostrador de triaje", "Triagedisk"),
            {"front": T("triage_front"), "top": T("solid_surface"), "carcass": T("carcass"),
             "body": W("monitor_body"), "chart": T("chart_screen"), "grey": GREY,
             "metal": STEEL, "screen": W("screen_off"), "particle": T("carcass")},
            [front([0, 0, 1.5], [16, 11, 2.5], "carcass", "front",
                   faces=("north", "south", "up")),
             el([0, 11, 1], [16, 11.8, 15], "top"),
             el([0.5, 0, 2.5], [1.5, 11, 14.5], "carcass"),
             el([14.5, 0, 2.5], [15.5, 11, 14.5], "carcass")]
            + st_monitor(8.5)
            # the vitals monitor on its pole, turned to the patient
            + octo(3.5, 5, 0.5, 11.8, 15, "metal")
            + [el([1, 15, 4], [6, 19, 5.5], "body"),
               screen([1.4, 15.4, 3.95], [5.6, 18.6, 3.95], "screen")],
            CL, W("vitals_screen"))

pair_block("decontamination_shower",
           ("Decontamination Shower", "Dekontaminationsdusche", "Ducha de descontaminación",
            "Saneringsdusch"),
           [el([5, 4, 15], [11, 27, 16], "metal"),
            el([6, 10, 13.5], [10, 14, 15], "metal"),
            el([4, 12, 12.8], [12, 13, 13.5], "paddle"),
            front([10.5, 3.5, 15.5], [15.5, 7.5, 16], "white", "sign"),
            el([7.5, 14, 14], [8.5, 26, 15], "metal"),
            el([7.5, 25, 6], [8.5, 26, 14], "metal"),
            el([2.5, 6, 13], [4, 11, 14.5], "grey"),
            el([2.9, 3, 13.4], [3.5, 6, 14], "hose"),
            el([3.5, 3, 13.4], [7, 3.6, 14], "hose"),
            el([6.4, 3.6, 13.4], [7, 10, 14], "hose")]
           + octo(8, 6, 3, 23.6, 25, "metal")
           + [el([5.3, 23.5, 3.3], [10.7, 23.55, 8.7], "rose", faces=("down",),
                 uv={"down": FULL})],
           1, {"metal": STEEL, "paddle": W("yellow"), "white": W("white"),
               "sign": T("decon_sign"), "grey": GREY, "hose": T("hose"),
               "rose": T("shower_rose"), "particle": STEEL},
           pair_java("decontamination_shower", (2, 0, 3, 14, 16, 16), "METAL", "WALL", "TALL",
                     False), CL, item_display=ITEM_TALL)

C.add("ed_tracking_board", "new BlockEdTrackingBoard()",
      ("ED Tracking Board", "Notaufnahme-Patientenübersicht", "Pantalla de seguimiento de urgencias",
       "Akutmottagningens patienttavla"),
      {"ed_tracking_board": model(
          {"body": W("tv_body"), "screen": T("ed_board_screen"), "grey": GREY,
           "particle": W("tv_body")},
          [el([6, 6, 15.5], [10, 10, 16], "grey"),
           el([0.5, 3, 14], [15.5, 13, 15.5], "body"),
           # the screen stands 0.3 px proud of the bezel, clear of model_depth's separating;
           # the renderer draws its lit copy just in front (SCREEN_Z)
           el([1, 3.5, ED_SCREEN_Z], [15, 12.5, 14], "screen", faces=("north",),
              uv={"north": [0, 0, 16, round(16 * ED_H / ED_W, 3)]})])},
      hc.facing_state(M("ed_tracking_board")), tab=CL)

# the floor marking: two cells, one up face each, the text's top away from whoever placed it
MARK = []
for _x0, _key in ((0, "left"), (-16, "right")):
    _e = el([_x0, 0, 0], [_x0 + 16, 0.05, 16], "mark", faces=("up",), per={"up": _key},
            uv={"up": FULL})
    _e["faces"]["up"]["rotation"] = 180
    MARK.append(_e)
pair_block("ambulance_only_marking",
           ("AMBULANCE ONLY Floor Marking", "Bodenmarkierung NUR RETTUNGSWAGEN",
            "Marca vial SOLO AMBULANCIAS", "Vägmarkering ENDAST AMBULANS"),
           MARK, 0, {"mark": T("mark_left"), "left": T("mark_left"), "right": T("mark_right"),
                     "particle": T("mark_left")},
           pair_java("ambulance_only_marking", (0, 0, 0, 16, 1, 16), "PLASTIC", "FLOOR",
                     "WIDE", False), CL)

prop("ambulance_only_sign", (1, 1, 15, 15, 15, 16), "METAL", "WALL", False,
     ("AMBULANCE ONLY Sign", "Schild NUR RETTUNGSWAGEN", "Señal SOLO AMBULANCIAS",
      "Skylt ENDAST AMBULANS"),
     {"sign": T("amb_sign"), "body": W("white")},
     [el([1, 1, 15.4], [15, 15, 16], "body", faces=("south", "east", "west", "up", "down")),
      screen([1, 1, 15.4], [15, 15, 15.4], "sign")], CL)

# ==========================================================================================
# Surgery support
# ==========================================================================================
pair_block("blanket_warming_cabinet",
           ("Blanket Warming Cabinet", "Wärmeschrank für Decken", "Armario calientamantas",
            "Värmeskåp för filtar"),
           [el([1, 0.6, 2], [15, 30, 16], "body"),
            front([1.5, 1.6, 1.6], [14.5, 14.6, 2], "body", "door"),
            front([1.5, 15.4, 1.6], [14.5, 27.4, 2], "body", "door"),
            front([3, 27.8, 1.6], [13, 29.6, 2], "body", "display"),
            el([1.5, 0, 3], [14.5, 0.6, 15], "grey")],
           1, {"body": STEEL, "door": T("warmer_door"), "display": T("warmer_display"),
               "grey": GREY, "particle": STEEL},
           pair_java("blanket_warming_cabinet", (1, 0, 1, 15, 16, 16), "METAL", "FLOOR", "TALL"),
           CL, item_display=ITEM_TALL)

screen_prop("autoclave", (1, 0, 1, 15, 15, 16), "METAL", "FLOOR", True,
            ("Autoclave", "Autoklav", "Autoclave", "Autoklav"),
            {"body": STEEL, "door": T("autoclave_door"), "grey": GREY,
             "panel": W("dark_grey"), "screen": W("screen_off"), "particle": STEEL},
            [el([1, 0.6, 2], [15, 15, 16], "body"),
             el([1.5, 0, 3], [14.5, 0.6, 15], "grey")]
            + [front([2.5, 1.5, 1.4], [13.5, 11.5, 2], "body", "door"),
               el([2, 12, 1.4], [14, 14.4, 2], "panel"),
               screen([2.6, 12.4, 1.35], [7, 14, 1.35], "screen"),
               el([9, 12.8, 1.2], [10, 13.6, 1.4], "grey"),
               el([11, 12.8, 1.2], [12, 13.6, 1.4], "grey")],
            CL, "autoclave_screen")

prop("case_cart", (1, 0, 2, 15, 16, 14), "METAL", "FLOOR", True,
     ("Surgical Case Cart", "OP-Transportwagen", "Carro de material quirúrgico",
      "Operationsvagn"),
     {"body": STEEL, "front": T("case_cart_front"), "grey": GREY, "rubber": RUBBER},
     casters([(2.5, 3.5), (13.5, 3.5), (2.5, 12.5), (13.5, 12.5)])
     + [front([1.5, 1, 2.5], [14.5, 14.5, 13.5], "body", "front"),
        el([1.2, 14.5, 2.2], [14.8, 15, 13.8], "grey"),
        el([2, 13, 13.5], [3, 14, 15], "grey"), el([13, 13, 13.5], [14, 14, 15], "grey"),
        el([2, 13, 14.6], [14, 14, 15.4], "grey")], CL)

pair_block("scrub_attire_dispenser",
           ("Scrub Attire Dispenser", "Ausgabeautomat für OP-Kleidung",
            "Dispensador de ropa quirúrgica", "Automat för operationskläder"),
           [el([0.5, 0, 2], [15.5, 30, 16], "body"),
            el([0.25, 30, 1.75], [15.75, 31, 16], "grey"),
            front([2, 15, 1.6], [14, 28, 2], "body", "window"),
            front([3, 5, 1.6], [13, 11, 2], "body", "door"),
            front([3, 12, 1.6], [13, 14.5, 2], "body", "label"),
            el([11, 15.5, 1.2], [13.5, 17, 1.6], "grey")],
           1, {"body": W("white"), "grey": W("dark_grey"), "window": T("scrub_window"),
               "door": T("dispense_door"), "label": T("scrub_label"), "particle": W("white")},
           pair_java("scrub_attire_dispenser", (0, 0, 1, 16, 16, 16), "METAL", "FLOOR", "TALL"),
           CL, item_display=ITEM_TALL)


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
