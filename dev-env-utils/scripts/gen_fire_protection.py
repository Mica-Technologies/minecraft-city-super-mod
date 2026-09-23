#!/usr/bin/env python3
"""
gen_fire_protection.py -- the Life Safety module's building fire protection equipment.

Everything a building fights a fire with besides its alarm, and the new detection devices:

  * extinguishers on their brackets (ABC, CO2, water, class K);
  * cabinets with doors that open (extinguisher, hose, AED -- which chirps -- and the Knox box);
  * the standpipe (hose valve, riser pipe) and fire department connections (wall Siamese in brass
    and chrome, freestanding, Storz);
  * the riser room (alarm check valve, OS&Y valve, post indicator valve, backflow preventer, the
    water motor gong outside, which rings with the panel's horns);
  * the magnetic door holders, which hold a door open by redstone until the panel alarms;
  * wall sign plates;
  * in the Fire Alarm & Detection tab: a photoelectric smoke detector, a duct detector, a beam
    detector and a remote annunciator.

One catalogue says, for each block, the Java that constructs it (and so its box), its models,
blockstate and names in the four languages. The script writes, under the Life Safety module's
assets/csm:

  * textures/blocks/lifesafety/fireprotection/*.png
  * models/block/lifesafety/fireprotection/*.json
  * blockstates/<registry>.json
  * the tile names and the annunciator's messages in all four lang files, kept in place by key

--fragments prints the tab registration lines, grouped by tab, which carry each block's box, so
the Java and the models are written from the same numbers.

Usage:
    python gen_fire_protection.py              # write everything
    python gen_fire_protection.py --check      # fail if the tree has drifted
    python gen_fire_protection.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from life_safety_gen_common import (  # noqa: E402
    Catalogue, bevel, box, clamp, cylinder_shading, disc, draw_text, draw_text_centred, face,
    facing_state,
    fill, frame, model, north_region, nsewud_state, pipe_x, pipe_z, post, rect, shade, text_width)

C = Catalogue("gen_fire_protection.py", "lifesafety/fireprotection", "lifesafety/fireprotection")
T = C.T
M = C.M

FP = "Fire Protection"
FA = "Fire Alarm & Detection"

RED = (188, 30, 36)
RED_DARK = (140, 20, 26)
WHITE = (232, 232, 226)
OFFWHITE = (214, 210, 196)
BEIGE = (206, 196, 170)
CHROME = (196, 200, 206)
STEEL = (122, 126, 132)
BRASS = (198, 158, 74)
BRONZE = (96, 76, 52)
BLACK = (34, 34, 36)
GREEN = (0, 140, 70)
YELLOW = (236, 190, 36)
GLASS_GLARE = (226, 238, 244)


def B(*v):
    """A box in sixteenths for the Java side, as the tab line writes it."""
    return "new int[]{%s}" % ", ".join("%g" % x for x in v)


def aabb(*v):
    return "new AxisAlignedBB(%s)" % ", ".join("%.6f" % (x / 16.0) for x in v)


# ==========================================================================================
# Textures
# ==========================================================================================
def flat(name, colour, grain=4, seed=1, size=16):
    C.textures[name] = lambda: fill(colour, size=size, grain=grain, seed=seed)


flat("red", RED, seed=2)
flat("red_dark", RED_DARK, seed=3)
flat("white", WHITE, seed=4, grain=3)
flat("beige", BEIGE, seed=5, grain=3)
flat("chrome", CHROME, seed=6, grain=6)
flat("steel", STEEL, seed=7, grain=5)
flat("brass", BRASS, seed=8, grain=7)
flat("bronze", BRONZE, seed=9, grain=5)
flat("black", BLACK, seed=10, grain=3)
flat("yellow", YELLOW, seed=11, grain=4)


def body(colour, label=None, seed=1):
    """An extinguisher shell's paint, with a label band wrapped round it: white, with a coloured
    rule for the agent class and rows of fine print. Every side of the octagonal shell samples a
    different column range, so nothing here depends on the column."""
    def draw():
        img = fill(colour, grain=3, seed=seed)
        if label:
            rect(img, 0, 8, 16, 12, (240, 238, 230))
            rect(img, 0, 8, 16, 9, label)
            px = img.load()
            rng = random.Random(seed)
            for y in (10, 11):
                for x in range(16):
                    if rng.random() < 0.5:
                        px[x, y] = (60, 60, 64, 255)
        # the shell's highlight near the top
        rect(img, 0, 5, 16, 6, shade(colour, 1.15))
        return img
    return draw


C.textures["ext_red"] = body(RED, seed=21)
C.textures["ext_red_label"] = body(RED, label=(20, 90, 200), seed=21)
C.textures["ext_silver"] = body(CHROME, seed=22)
C.textures["ext_silver_label"] = body(CHROME, label=(20, 120, 60), seed=22)
C.textures["ext_k_label"] = body(CHROME, label=(20, 20, 20), seed=23)


@C.texture("gauge")
def _gauge():
    img = fill(WHITE, grain=1)
    disc(img, 8, 8, 7, (30, 30, 30))
    disc(img, 8, 8, 6, (244, 244, 240))
    # green "charged" arc and the needle in it
    px = img.load()
    for y in range(16):
        for x in range(16):
            a = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 4 < d < 6 and -2.2 < a < -0.9:
                px[x, y] = (30, 160, 60, 255)
    for k in range(5):
        px[8 + k // 2, 8 - k] = (200, 20, 20, 255)
    return img


@C.texture("hose")
def _hose():
    img = fill(BLACK, grain=5, seed=31)
    px = img.load()
    for y in range(0, 16, 3):
        for x in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = shade((r, g, b), 1.5) + (a,)
    return img


@C.texture("hose_folds")
def _hose_folds():
    """Rack-folded fire hose seen from the front: red fabric in horizontal loops."""
    img = fill((196, 50, 44), grain=6, seed=32)
    for y in range(16):
        k = 0.7 + 0.45 * math.sin((y % 4) / 4.0 * math.pi)
        rect(img, 0, y, 16, y + 1, shade((196, 50, 44), k))
    return img


def door_glass(text_lines, colour):
    """A glass cabinet door 12 x 14 px (north face of x 2..14, y 1..15) on a 64 px texture: clear
    glass with glare, lettered in colour."""
    def draw():
        img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        x0, y0, x1, y1 = north_region((2, 1), (14, 15), 64)
        frame(img, x0, y0, x1, y1, WHITE, 1)
        px = img.load()
        for i in range(y1 - y0):
            for off in (10, 14, 34):
                x = x0 + i + off - 40
                if x0 + 1 <= x < x1 - 1 and (i // 3) % 4 != 0:
                    px[x, y1 - 1 - i] = GLASS_GLARE + (255,)
        y = y0 + 4
        for line in text_lines:
            draw_text_centred(img, line, (x0 + x1) / 2.0, y, colour)
            y += 7
        return img
    return draw


def door_solid(colour, text_lines, text_colour, louvre=False):
    def draw():
        img = fill(colour, size=64, grain=3, seed=41)
        x0, y0, x1, y1 = north_region((2, 1), (14, 15), 64)
        bevel(img, x0, y0, x1, y1, colour)
        y = y0 + 6
        for line in text_lines:
            draw_text_centred(img, line, (x0 + x1) / 2.0, y, text_colour)
            y += 7
        if louvre:
            for yy in range(y1 - 20, y1 - 6, 3):
                rect(img, x0 + 10, yy, x1 - 10, yy + 1, shade(colour, 0.6))
        # the handle
        rect(img, x0 + 4, (y0 + y1) // 2 - 4, x0 + 6, (y0 + y1) // 2 + 4, CHROME)
        return img
    return draw


C.textures["door_ext_glass"] = door_glass(["FIRE", "EXTINGUISHER"], RED)
C.textures["door_ext_red"] = door_solid(RED, ["FIRE", "EXTINGUISHER"], WHITE)
C.textures["door_hose"] = door_solid(RED, ["FIRE HOSE"], WHITE, louvre=True)


@C.texture("door_aed")
def _door_aed():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    x0, y0, x1, y1 = north_region((3, 2), (13, 14), 64)
    rect(img, x0, y0, x1, y1, WHITE)
    rect(img, x0, y0, x1, y0 + 14, GREEN)
    draw_text_centred(img, "AED", (x0 + x1) / 2.0, y0 + 2, WHITE, scale=2)
    # a window onto the AED
    rect(img, x0 + 5, y0 + 18, x1 - 5, y1 - 5, (0, 0, 0, 0))
    frame(img, x0 + 4, y0 + 17, x1 - 4, y1 - 4, shade(WHITE, 0.8))
    return img


@C.texture("aed_unit")
def _aed_unit():
    img = fill(YELLOW, grain=3, seed=51)
    rect(img, 4, 5, 12, 10, (40, 40, 40))
    disc(img, 8, 7.5, 1.6, (40, 170, 70))
    rect(img, 3, 12, 13, 13, shade(YELLOW, 0.7))
    return img


@C.texture("knox_front")
def _knox_front():
    img = fill(BRONZE, grain=4, seed=61)
    x0, y0, x1, y1 = north_region((5, 4), (11, 11), 16)
    bevel(img, x0, y0, x1, y1, BRONZE)
    disc(img, 8, 6.5, 1.1, (200, 190, 150))
    img.load()[8, 6] = (30, 30, 30, 255)
    rect(img, x0, y0 + 1, x0 + 1, y0 + 2, (60, 50, 40))
    rect(img, x0, y1 - 2, x0 + 1, y1 - 1, (60, 50, 40))
    return img


@C.texture("keys")
def _keys():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in (6, 8, 10):
        rect(img, x, 6, x + 1, 10, BRASS)
        disc(img, x + 0.5, 6, 1.1, BRASS)
    return img


@C.texture("handwheel")
def _handwheel():
    """A handwheel seen from above: rim and four spokes, the rest clear (cutout)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 6 <= d <= 7.9 or d < 1.5 or ((abs(x - 7.5) < 0.9 or abs(y - 7.5) < 0.9) and d < 7):
                px[x, y] = RED + (255,)
    return img


@C.texture("plate_fdc_brass")
def _plate_fdc_brass():
    img = fill(BRASS, size=64, grain=6, seed=71)
    x0, y0, x1, y1 = north_region((2, 3), (14, 13), 64)
    frame(img, x0, y0, x1, y1, shade(BRASS, 0.7), 2)
    draw_text_centred(img, "AUTO SPKR", (x0 + x1) / 2.0, y0 + 4, shade(BRASS, 0.45))
    return img


@C.texture("plate_fdc_chrome")
def _plate_fdc_chrome():
    img = fill(CHROME, size=64, grain=6, seed=72)
    x0, y0, x1, y1 = north_region((2, 3), (14, 13), 64)
    frame(img, x0, y0, x1, y1, shade(CHROME, 0.7), 2)
    draw_text_centred(img, "STANDPIPE", (x0 + x1) / 2.0, y0 + 4, shade(CHROME, 0.4))
    return img


@C.texture("storz")
def _storz():
    img = fill(CHROME, grain=6, seed=73)
    disc(img, 8, 8, 6.5, shade(CHROME, 0.8))
    disc(img, 8, 8, 3.5, shade(CHROME, 1.1))
    for (x, y) in ((8, 1), (8, 14), (1, 8), (14, 8)):
        rect(img, x - 1, y - 1, x + 1, y + 1, shade(CHROME, 0.55))
    return img


@C.texture("cap_brass")
def _cap_brass():
    img = fill(BRASS, grain=6, seed=74)
    disc(img, 8, 8, 6, shade(BRASS, 0.85))
    for a in range(6):
        ang = a * math.pi / 3
        rect(img, int(8 + 5 * math.cos(ang)), int(8 + 5 * math.sin(ang)),
             int(8 + 5 * math.cos(ang)) + 1, int(8 + 5 * math.sin(ang)) + 1, shade(BRASS, 0.6))
    return img


@C.texture("cap_chrome")
def _cap_chrome():
    img = fill(CHROME, grain=6, seed=75)
    disc(img, 8, 8, 6, shade(CHROME, 0.85))
    return img


@C.texture("piv_head")
def _piv_head():
    img = fill(RED, grain=3, seed=81)
    x0, y0, x1, y1 = north_region((5, 12), (11, 15), 16)
    rect(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, BLACK)
    # "OPEN" in white is too long for six pixels: the window shows its first two letters.
    img.load()[x0 + 2, y0 + 1] = (240, 240, 240, 255)
    img.load()[x0 + 3, y0 + 1] = (240, 240, 240, 255)
    return img


@C.texture("gong_face")
def _gong_face():
    # Drawn over the dome's stepped rings, each ring's front showing its own band: lighter
    # toward the crown, a highlight, and the chrome bolt at the centre.
    img = fill(RED, grain=3, seed=91)
    disc(img, 8, 8, 5.6, shade(RED, 1.08))
    disc(img, 8, 8, 4.2, shade(RED, 1.16))
    disc(img, 8, 8, 2.4, shade(RED, 1.24))
    disc(img, 6.6, 6.6, 1.4, shade(RED, 1.45))
    disc(img, 8, 8, 1.0, CHROME)
    return img


@C.texture("smoke_face")
def _smoke_face():
    img = fill(WHITE, grain=2, seed=101)
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            a = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            if 3 < d < 5 and (int((a + math.pi) / (2 * math.pi) * 16) % 2 == 0):
                px[x, y] = shade(WHITE, 0.68) + (255,)
    px[10, 5] = (40, 200, 60, 255)
    return img


@C.texture("duct_front")
def _duct_front():
    img = fill((150, 152, 156), grain=3, seed=111)
    x0, y0, x1, y1 = north_region((2, 3), (14, 13), 16)
    bevel(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, (196, 208, 214))
    disc(img, 8, 8, 2, (70, 72, 76))
    img.load()[5, 5] = (220, 30, 30, 255)
    return img


@C.texture("beam_front")
def _beam_front():
    img = fill(OFFWHITE, grain=3, seed=121)
    disc(img, 8, 8, 3.2, (30, 30, 34))
    disc(img, 7.2, 7.2, 1.0, (120, 150, 190))
    img.load()[8, 4] = (40, 200, 60, 255)
    return img


def annunciator(alarm):
    def draw():
        img = fill(BEIGE, size=64, grain=2, seed=131)
        x0, y0, x1, y1 = north_region((3, 3), (13, 13), 64)
        bevel(img, x0, y0, x1, y1, BEIGE)
        # LCD
        lx0, ly0, lx1, ly1 = x0 + 5, y0 + 5, x1 - 5, y0 + 17
        rect(img, lx0, ly0, lx1, ly1, (60, 88, 46) if alarm else (40, 52, 36))
        draw_text(img, "FIRE" if alarm else "SYSTEM", lx0 + 2, ly0 + 1,
                  (200, 240, 120) if alarm else (110, 150, 90))
        draw_text(img, "ALARM" if alarm else "NORMAL", lx0 + 2, ly0 + 7,
                  (200, 240, 120) if alarm else (110, 150, 90))
        # lamps: FIRE (red), TROUBLE (amber), POWER (green)
        for i, (on, off) in enumerate((((255, 40, 40), (90, 20, 20)),
                                       ((200, 150, 30), (80, 60, 20)),
                                       ((60, 230, 80), (60, 230, 80)))):
            lit = on if (i != 0 or alarm) else off
            disc(img, x0 + 9 + i * 11, y1 - 12, 2.5, lit)
        draw_text(img, "ACK", x0 + 5, y1 - 7, (60, 60, 60))
        return img
    return draw


C.textures["annunciator"] = annunciator(False)
C.textures["annunciator_alarm"] = annunciator(True)


@C.texture("magnet")
def _magnet():
    img = fill((70, 72, 76), grain=4, seed=141)
    disc(img, 8, 8, 6.5, (110, 114, 120))
    disc(img, 8, 8, 4.5, (70, 72, 76))
    disc(img, 8, 8, 2, (150, 154, 160))
    return img


flat("led_green", (60, 240, 90), grain=2, seed=151)
flat("led_off", (40, 60, 44), grain=2, seed=152)


# --- sign plates: 12 x 14 px (x 2..14, y 1..15) on a 64 px texture ---
def sign(bg, fg, lines, picto=None):
    """lines: (text, scale) from the top; picto(img, region) draws a pictogram above them."""
    def draw():
        img = fill(bg, size=64, grain=2, seed=161)
        x0, y0, x1, y1 = north_region((2, 1), (14, 15), 64)
        frame(img, x0, y0, x1, y1, shade(bg, 0.75), 1)
        cy = y0 + 4
        if picto:
            picto(img, (x0, y0, x1, y1), fg)
            cy = y0 + 32
        for text, scale in lines:
            w = text_width(text, scale)
            if w > (x1 - x0):
                raise ValueError("sign line too wide: %r" % text)
            draw_text_centred(img, text, (x0 + x1) / 2.0, cy, fg, scale)
            cy += 6 * scale + 1
        return img
    return draw


def picto_extinguisher(img, r, fg):
    x0, y0, x1, y1 = r
    cx = (x0 + x1) // 2
    rect(img, cx - 4, y0 + 10, cx + 4, y0 + 28, fg)          # body
    rect(img, cx - 2, y0 + 6, cx + 2, y0 + 10, fg)            # neck
    rect(img, cx - 5, y0 + 5, cx + 6, y0 + 7, fg)             # handle
    rect(img, cx + 6, y0 + 6, cx + 8, y0 + 18, fg)            # hose
    rect(img, cx + 8, y0 + 17, cx + 10, y0 + 20, fg)


def picto_hose(img, r, fg):
    x0, y0, x1, y1 = r
    cx, cy = (x0 + x1) / 2.0, y0 + 16
    px = img.load()
    for y in range(y0 + 2, y0 + 30):
        for x in range(x0 + 2, x1 - 2):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if (d < 12.5 and int(d) % 3 == 0) or d < 2:
                px[x, y] = clamp(fg) + (255,)
    rect(img, int(cx) + 10, int(cy), int(cx) + 12, int(cy) + 12, fg)


def picto_heart(img, r, fg):
    x0, y0, x1, y1 = r
    cx, cy = (x0 + x1) / 2.0, y0 + 14
    px = img.load()
    for y in range(y0 + 2, y0 + 30):
        for x in range(x0 + 2, x1 - 2):
            u, v = (x + 0.5 - cx) / 11.0, -(y + 0.5 - cy) / 11.0
            if (u * u + v * v - 1) ** 3 - u * u * v ** 3 <= 0:
                px[x, y] = clamp(fg) + (255,)
    # the lightning bolt through it, in the background colour
    bg = px[x0 + 3, y1 - 3]
    for k in range(9):
        px[int(cx) + 2 - k // 2, int(cy) - 6 + k] = bg
        px[int(cx) - 1 + k // 3, int(cy) + 1 + k // 2] = bg


C.textures["sign_extinguisher"] = sign(RED, WHITE, [("FIRE", 1), ("EXTINGUISHER", 1)],
                                       picto_extinguisher)
C.textures["sign_hose"] = sign(RED, WHITE, [("FIRE", 1), ("HOSE", 1)], picto_hose)
C.textures["sign_fdc"] = sign(RED, WHITE, [("FDC", 3), ("FIRE DEPT", 1), ("CONNECTION", 1)])
C.textures["sign_riser"] = sign(RED, WHITE, [("RISER", 2), ("ROOM", 2), ("SPRINKLER", 1),
                                             ("CONTROL", 1)])
C.textures["sign_fire_door"] = sign(WHITE, RED, [("FIRE", 2), ("DOOR", 2), ("KEEP", 1),
                                                 ("CLOSED", 1)])
C.textures["sign_aed"] = sign(GREEN, WHITE, [("AED", 2)], picto_heart)
flat("sign_back", (170, 172, 176), grain=2, seed=171)


# ==========================================================================================
# Blocks
# ==========================================================================================
def fp_prop(reg, box6, collides, names, models, state, tab=FP):
    C.add(reg, 'new BlockFireProtectionProp("%s", %s, %s)' % (reg, B(*box6),
                                                              "true" if collides else "false"),
          names, models, state, tab=tab)


# --- extinguishers -------------------------------------------------------------------------
def extinguisher_elements(body_tex, label_tex, kind, cz=12.5, r=1.8, y0=0.5, y1=11.5,
                          bracket=True):
    els = []
    if bracket:
        els += [box([6.5, 4, 15], [9.5, 10, 16], "steel"),
                box([7, 8.5, cz + r - 0.2], [9, 9.5, 15], "steel")]
    body = post(8, cz, r, y0, y1, "body")
    for el in body:
        for f, spec in el["faces"].items():
            if f not in ("up", "down"):
                spec["texture"] = "#label"
    els += body
    els += post(8, cz, r * 0.4, y1, y1 + 1, "chrome", bottom=False)
    top = y1 + 1
    els += [box([7, top, cz - 1.5], [9, top + 1, cz + 1.5], "chrome"),
            box([7.25, top + 1, cz - 2], [8.75, top + 1.5, cz + 2], "black")]
    if kind == "abc" or kind == "water":
        els.append(box([7.25, top + 0.1, cz - 2], [8.75, top + 0.9, cz - 1.5], "chrome",
                       faces=("east", "west", "up", "down")))
        els.append(box([7.3, top + 0.15, cz - 2.01], [8.7, top + 0.85, cz - 2], "gauge",
                       faces=("north",)))
    if kind == "co2":
        # the horn, clipped to the side of the body
        els += [box([8 + r + 0.2, y0 + 2, cz - 1.2], [8 + r + 2.2, y0 + 8, cz + 1.2], "black"),
                box([8 + r + 0.6, y0 + 8, cz - 0.4], [8 + r + 1.4, top + 0.6, cz + 0.4], "hose")]
    elif kind == "k":
        # the long discharge wand, clipped alongside
        els += [box([8 + r + 0.3, y0 + 1, cz - 0.3], [8 + r + 0.9, top + 0.5, cz + 0.3], "black"),
                box([8 + r + 0.1, y0 + 0.5, cz - 0.6], [8 + r + 1.1, y0 + 1.5, cz + 0.6], "black")]
    else:
        # the hose, down the front-right and into the strap
        els += [box([8 + r - 0.6, y0 + 3, cz - r - 0.4], [8 + r + 0.2, top + 0.5, cz - r + 0.4],
                    "hose"),
                box([8 + r - 0.8, y0 + 2, cz - r - 0.6], [8 + r + 0.4, y0 + 3.5, cz - r + 0.6],
                    "black")]
    return els


EXTINGUISHERS = [
    ("abc", "ext_red", "ext_red_label",
     ("ABC Dry Chemical Fire Extinguisher", "ABC-Pulverlöscher", "Extintor de polvo ABC",
      "ABC-pulversläckare")),
    ("co2", "ext_red", "ext_red", ("CO2 Fire Extinguisher", "CO2-Feuerlöscher",
                                   "Extintor de CO2", "Koldioxidsläckare")),
    ("water", "ext_silver", "ext_silver_label",
     ("Water Fire Extinguisher", "Wasserlöscher", "Extintor de agua", "Vattensläckare")),
    ("k", "ext_silver", "ext_k_label",
     ("Class K Fire Extinguisher", "Fettbrandlöscher (Klasse F)", "Extintor clase K",
      "Fettbrandsläckare (klass F)")),
]
for kind, body_tex, label_tex, names in EXTINGUISHERS:
    reg = "fire_extinguisher_" + kind
    tex = {"body": T(body_tex), "label": T(label_tex), "chrome": T("chrome"),
           "steel": T("steel"), "black": T("black"), "hose": T("hose"), "gauge": T("gauge"),
           "particle": T(body_tex)}
    fp_prop(reg, (4, 0, 8, 12, 14, 16), False, names,
            {reg: model(tex, extinguisher_elements(body_tex, label_tex, kind))},
            facing_state(M(reg)))


# --- cabinets ------------------------------------------------------------------------------
def cabinet_shell(tex, x0, y0, x1, y1, z_front, lip=1):
    """An open-fronted box against the wall: back, sides, top and bottom, lip thick."""
    return [box([x0, y0, 15], [x1, y1, 16], tex),
            box([x0, y0, z_front], [x0 + lip, y1, 15], tex),
            box([x1 - lip, y0, z_front], [x1, y1, 15], tex),
            box([x0 + lip, y1 - lip, z_front], [x1 - lip, y1, 15], tex),
            box([x0 + lip, y0, z_front], [x1 - lip, y0 + lip, 15], tex)]


def door_closed(x0, y0, x1, y1, z, tex, edge):
    return box([x0, y0, z - 0.5], [x1, y1, z], edge, per={"north": tex})


def door_open(x0, y0, x1, y1, z, tex, edge):
    """The door swung 90 degrees on its hinge at x0 (the viewer's right), out into the room.
    Its faces carry the closed door's UVs so the lettering is not stretched."""
    w = x1 - x0
    d = box([x0 - 0.5, y0, z - w], [x0, y1, z], edge)
    d["faces"]["east"] = face(tex, [16 - x1, 16 - y1, 16 - x0, 16 - y0])
    d["faces"]["west"] = face(tex, [16 - x0, 16 - y1, 16 - x1, 16 - y0])
    return d


def cabinet(reg, names, shell_tex, door_tex, door_edge, inside, rect6, alarmed=False,
            extra_tex=None):
    x0, y0, z0, x1, y1, _ = rect6
    tex = {"shell": T(shell_tex), "door": T(door_tex), "edge": T(door_edge),
           "particle": T(shell_tex)}
    tex.update(extra_tex or {})
    shell = cabinet_shell("shell", x0, y0, x1, y1, z0 + 0.5)
    closed = model(tex, shell + inside + [door_closed(x0, y0, x1, y1, z0 + 0.5, "door", "edge")])
    opened = model(tex, shell + inside + [door_open(x0, y0, x1, y1, z0 + 0.5, "door", "edge")])
    C.add(reg, 'new BlockFireProtectionCabinet("%s", %s, %s)'
          % (reg, B(*rect6), "true" if alarmed else "false"),
          names, {reg: closed, reg + "_open": opened},
          facing_state(M(reg), {"open": {"false": {}, "true": {"model": M(reg + "_open")}}}),
          tab=FP)


EXT_TEX = {"body": T("ext_red"), "label": T("ext_red_label"), "chrome": T("chrome"),
           "steel": T("steel"), "black": T("black"), "hose": T("hose"), "gauge": T("gauge")}
inside_ext = extinguisher_elements("ext_red", "ext_red_label", "abc", cz=12.8, r=1.6, y0=2,
                                   y1=11, bracket=False)
cabinet("extinguisher_cabinet_glass",
        ("Fire Extinguisher Cabinet (Glass Door)", "Feuerlöscherschrank (Glastür)",
         "Gabinete de extintor (puerta de vidrio)", "Brandsläckarskåp (glasdörr)"),
        "white", "door_ext_glass", "white", inside_ext, (2, 1, 9, 14, 15, 16),
        extra_tex=EXT_TEX)
cabinet("extinguisher_cabinet_steel",
        ("Fire Extinguisher Cabinet (Steel Door)", "Feuerlöscherschrank (Stahltür)",
         "Gabinete de extintor (puerta de acero)", "Brandsläckarskåp (ståldörr)"),
        "red", "door_ext_red", "red", inside_ext, (2, 1, 9, 14, 15, 16), extra_tex=EXT_TEX)

inside_hose = ([box([3, 3, 11], [13, 12, 15], "folds")]
               + pipe_z(4, 13.5, 0.9, 11.5, 15, "brassx")
               + post(4, 12.5, 1.2, 12.6, 13.6, "wheel"))
cabinet("fire_hose_cabinet",
        ("Fire Hose Cabinet", "Wandhydrant (Schlauchschrank)",
         "Gabinete de manguera contra incendios", "Brandpostskåp"),
        "red", "door_hose", "red", inside_hose, (2, 1, 9, 14, 15, 16),
        extra_tex={"folds": T("hose_folds"), "brassx": T("brass"), "wheel": T("red_dark")})

inside_aed = [box([5, 4, 12], [11, 10, 15], "aed", per={"north": "aedf"}),
              box([7, 10, 13], [9, 11, 14], "black")]
cabinet("aed_cabinet",
        ("AED Cabinet", "AED-Wandschrank", "Gabinete de DEA", "Hjärtstartarskåp"),
        "white", "door_aed", "white", inside_aed, (3, 2, 10, 13, 14, 16), alarmed=True,
        extra_tex={"aed": T("yellow"), "aedf": T("aed_unit"), "black": T("black")})

cabinet("knox_box",
        ("Knox Box (Key Lock Box)", "Feuerwehr-Schlüsseldepot", "Caja de llaves para bomberos",
         "Nyckelskåp för räddningstjänsten"),
        "bronze", "knox_front", "bronze",
        [box([5.5, 4.5, 15], [10.5, 10.5, 15.1], "keys", faces=("north",))],
        (5, 4, 12, 11, 11, 16), extra_tex={"keys": T("keys")})


# --- standpipe and fire department connections -----------------------------------------------
fp_prop("standpipe_hose_valve", (5, 4, 5, 11, 12, 16), True,
        ("Standpipe Hose Valve", "Steigleitungs-Schlauchventil", "Válvula de manguera de columna seca",
         "Stigarledningsventil"),
        {"standpipe_hose_valve": model(
            {"red": T("red"), "brass": T("brass"), "cap": T("cap_brass"), "wheel": T("handwheel"),
             "particle": T("brass")},
            pipe_z(8, 8, 1.6, 12, 16, "red", front=False)
            + [box([6.4, 6.4, 9], [9.6, 9.6, 12], "brass")]
            + post(8, 10.5, 0.6, 9.6, 11, "brass", bottom=False)
            + [box([4.5, 11, 7], [11.5, 11.6, 14], "wheel", faces=("up", "down"))]
            + pipe_z(8, 7.2, 1.3, 6.5, 9, "brass", front=False)
            + pipe_z(8, 7.2, 1.6, 5.5, 6.5, "brass")
            + [box([6.4, 5.6, 5.49], [9.6, 8.8, 5.5], "cap", faces=("north",))])},
        facing_state(M("standpipe_hose_valve")))

fp_prop("standpipe_riser", (5, 0, 9, 11, 16, 16), True,
        ("Standpipe Riser", "Steigleitung", "Tubería vertical de columna seca", "Stigarledning"),
        {"standpipe_riser": model({"red": T("red"), "steel": T("steel"), "particle": T("red")},
                                  post(8, 12, 2.5, 0, 16, "red", top=False, bottom=False)
                                  + [box([5, 6, 14.5], [11, 7, 16], "steel")])},
        facing_state(M("standpipe_riser")))


def siamese(reg, names, metal, plate, cap):
    fp_prop(reg, (2, 3, 8, 14, 13, 16), True, names,
            {reg: model({"m": T(metal), "plate": T(plate), "cap": T(cap), "particle": T(metal)},
                        [box([2, 3, 15], [14, 13, 16], "m", per={"north": "plate"})]
                        + pipe_z(5, 7, 1.8, 10.5, 15, "m", front=False)
                        + pipe_z(11, 7, 1.8, 10.5, 15, "m", front=False)
                        + pipe_z(5, 7, 2.1, 9.5, 10.5, "cap")
                        + pipe_z(11, 7, 2.1, 9.5, 10.5, "cap"))},
            facing_state(M(reg)))


siamese("fdc_siamese_brass",
        ("Fire Department Connection (Brass)", "Feuerwehreinspeisung (Messing)",
         "Conexión para bomberos (latón)", "Brandförsörjningsanslutning (mässing)"),
        "brass", "plate_fdc_brass", "cap_brass")
siamese("fdc_siamese_chrome",
        ("Fire Department Connection (Chrome)", "Feuerwehreinspeisung (Chrom)",
         "Conexión para bomberos (cromo)", "Brandförsörjningsanslutning (krom)"),
        "chrome", "plate_fdc_chrome", "cap_chrome")

fp_prop("fdc_freestanding", (4, 0, 3, 12, 13, 11), True,
        ("Freestanding Fire Department Connection", "Freistehende Feuerwehreinspeisung",
         "Conexión para bomberos exenta", "Fristående brandförsörjningsanslutning"),
        {"fdc_freestanding": model(
            {"red": T("red"), "brass": T("brass"), "cap": T("cap_brass"), "particle": T("brass")},
            post(8, 8, 1.6, 0, 9, "red", top=False)
            + [box([4, 9, 6], [12, 12, 10], "brass")]
            + pipe_z(5.5, 10.5, 1.4, 4, 6, "brass", front=False)
            + pipe_z(10.5, 10.5, 1.4, 4, 6, "brass", front=False)
            + pipe_z(5.5, 10.5, 1.7, 3, 4, "cap")
            + pipe_z(10.5, 10.5, 1.7, 3, 4, "cap"))},
        facing_state(M("fdc_freestanding")))

fp_prop("fdc_storz", (3, 3, 9, 13, 13, 16), True,
        ("Storz Fire Department Connection", "Storz-Feuerwehreinspeisung",
         "Conexión Storz para bomberos", "Storz-anslutning för räddningstjänsten"),
        {"fdc_storz": model(
            {"chrome": T("chrome"), "storz": T("storz"), "particle": T("chrome")},
            [box([3, 3, 15], [13, 13, 16], "chrome")]
            + pipe_z(8, 8, 3, 10.5, 15, "chrome", front=False)
            + [box([4.5, 4.5, 9.5], [11.5, 11.5, 10.5], "chrome", per={"north": "storz"})])},
        facing_state(M("fdc_storz")))


# --- riser room ----------------------------------------------------------------------------
fp_prop("sprinkler_alarm_valve", (4, 0, 7, 12, 16, 16), True,
        ("Sprinkler Riser Alarm Check Valve", "Nassalarmventil (Sprinklersteigleitung)",
         "Válvula de alarma del rociador", "Larmventil för sprinklerstam"),
        {"sprinkler_alarm_valve": model(
            {"red": T("red"), "dark": T("red_dark"), "gauge": T("gauge"), "brass": T("brass"),
             "particle": T("red")},
            post(8, 11.5, 2.5, 0, 16, "red", top=False, bottom=False)
            + post(8, 11.5, 3.5, 4, 10, "dark")
            + pipe_x(12, 11.5, 0.6, 3.5, 12.5, "brass")
            + [box([3, 11, 9.5], [5.5, 13.5, 11], "brass", per={"north": "gauge"}),
               box([10.5, 11, 9.5], [13, 13.5, 11], "brass", per={"north": "gauge"})])},
        facing_state(M("sprinkler_alarm_valve")))

fp_prop("osy_gate_valve", (3, 0, 7, 13, 16, 16), True,
        ("OS&Y Gate Valve", "Absperrschieber mit steigender Spindel", "Válvula de compuerta OS&Y",
         "Slidventil med stigande spindel"),
        {"osy_gate_valve": model(
            {"red": T("red"), "dark": T("red_dark"), "brass": T("brass"), "wheel": T("handwheel"),
             "particle": T("red")},
            post(8, 11.5, 2.5, 0, 16, "red", top=False, bottom=False)
            + [box([4.5, 3, 8.5], [11.5, 9, 14.5], "dark"),
               box([5.5, 9, 9.5], [10.5, 10, 13.5], "dark"),
               box([5.5, 10, 7.5], [6.5, 13, 8.5], "red"),
               box([9.5, 10, 7.5], [10.5, 13, 8.5], "red"),
               box([5.5, 13, 7.5], [10.5, 14, 8.5], "red"),
               box([7.6, 9, 7.6], [8.4, 16, 8.4], "brass"),
               box([3.5, 14.5, 3.5], [12.5, 15.1, 12.5], "wheel", faces=("up", "down"))])},
        facing_state(M("osy_gate_valve")))

fp_prop("post_indicator_valve", (5, 0, 4, 11, 16, 12), True,
        ("Post Indicator Valve", "Hydrantenschieber mit Stellungsanzeige",
         "Válvula de poste indicador", "Ventilpost med lägesvisare"),
        {"post_indicator_valve": model(
            {"red": T("red"), "head": T("piv_head"), "steel": T("steel"), "particle": T("red")},
            post(8, 8, 2.2, 0, 12, "red", top=False)
            + [box([5, 12, 5], [11, 15, 11], "red", per={"north": "head"}),
               box([7.5, 15, 4], [8.5, 16, 12], "steel"),
               box([7, 15.5, 7], [9, 16, 9], "steel")])},
        facing_state(M("post_indicator_valve")))

fp_prop("fire_backflow_preventer", (0, 0, 4, 16, 14, 12), True,
        ("Fire Line Backflow Preventer", "Systemtrenner (Löschwasserleitung)",
         "Válvula antirretorno de la red de incendios", "Återströmningsskydd för brandvattenledning"),
        {"fire_backflow_preventer": model(
            {"red": T("red"), "bronze": T("bronze"), "steel": T("steel"), "wheel": T("handwheel"),
             "particle": T("red")},
            post(2.5, 8, 1.8, 0, 8, "red", top=False)
            + post(13.5, 8, 1.8, 0, 8, "red", top=False)
            + pipe_x(8, 8, 2, 0.7, 15.3, "red")
            + [box([5, 5.5, 5.5], [11, 11, 10.5], "bronze"),
               box([7.6, 11, 7.6], [8.4, 13, 8.4], "steel"),
               box([4.5, 13, 4.5], [11.5, 13.6, 11.5], "wheel", faces=("up", "down"))])},
        facing_state(M("fire_backflow_preventer")))

def gong_dome():
    """The gong's dome, crown out: stepped rings, each capped at the front with its band of the
    face texture, so the steps read as one rounded bell. (Each ring must be capped: an open ring
    shows its inside, as the first gong did.)"""
    els = [box([6, 6, 15.5], [10, 10, 16], "steel")]
    for r, z0, z1 in ((6.5, 13.6, 15.5), (5.6, 12.2, 13.6), (4.2, 11.1, 12.2), (2.4, 10.4, 11.1)):
        for el in pipe_z(8, 8, r, z0, z1, "red"):
            if "north" in el["faces"]:
                el["faces"]["north"]["texture"] = "#face"
            els.append(el)
    return els


# The water motor gong rings with the panel's horns: it is a sounder, and a sounder is six-way
# like every fire alarm appliance. Its sound is its own, a struck gong, not the electric bell.
C.add("water_motor_gong",
      'new BlockFireAlarmSounderFactory("water_motor_gong", "csm:water_motor_gong", %s)'
      % aabb(1.5, 1.5, 10.4, 14.5, 14.5, 16),
      ("Water Motor Gong", "Wassermotorglocke", "Campana hidráulica de alarma",
       "Vattenmotorklocka"),
      {"water_motor_gong": model({"red": T("red"), "face": T("gong_face"), "steel": T("steel"),
                                  "particle": T("red")}, gong_dome())},
      nsewud_state(M("water_motor_gong")), tab=FP)


# --- magnetic door holders -----------------------------------------------------------------
def holder_state(reg):
    return facing_state(M(reg), {"alarm": {"false": {}, "true": {"textures": {"led": T("led_off")}}}})


C.add("magnetic_door_holder",
      'new BlockMagneticDoorHolder("magnetic_door_holder", %s)' % B(5, 4, 12, 11, 12, 16),
      ("Magnetic Door Holder (Wall)", "Haftmagnet (Wand)", "Retenedor magnético de puerta (pared)",
       "Magnetisk dörrhållare (vägg)"),
      {"magnetic_door_holder": model(
          {"beige": T("beige"), "magnet": T("magnet"), "led": T("led_green"), "particle": T("beige")},
          [box([5, 4, 15], [11, 12, 16], "beige")]
          + pipe_z(8, 7.5, 2.5, 12.5, 15, "beige", front=False)
          + [box([5.5, 5, 12], [10.5, 10, 12.5], "beige", per={"north": "magnet"}),
             box([7.5, 10.6, 14.4], [8.5, 11.4, 15], "led")])},
      holder_state("magnetic_door_holder"), tab=FP)

C.add("magnetic_door_holder_floor",
      'new BlockMagneticDoorHolder("magnetic_door_holder_floor", %s)' % B(5, 0, 4, 11, 9, 12),
      ("Magnetic Door Holder (Floor)", "Haftmagnet (Boden)", "Retenedor magnético de puerta (suelo)",
       "Magnetisk dörrhållare (golv)"),
      {"magnetic_door_holder_floor": model(
          {"beige": T("beige"), "magnet": T("magnet"), "led": T("led_green"), "steel": T("steel"),
           "particle": T("beige")},
          post(8, 8, 2.5, 0, 1, "steel")
          + post(8, 8, 1, 1, 5.5, "steel", bottom=False)
          + [box([5.5, 4.5, 6.5], [10.5, 9, 10], "beige", per={"north": "magnet"}),
             box([7.5, 9, 8], [8.5, 9.4, 9], "led")])},
      holder_state("magnetic_door_holder_floor"), tab=FP)


# --- sign plates ---------------------------------------------------------------------------
SIGNS = [
    ("sign_extinguisher", ("Fire Extinguisher Sign", "Schild Feuerlöscher", "Señal de extintor",
                           "Skylt brandsläckare")),
    ("sign_hose", ("Fire Hose Sign", "Schild Löschschlauch", "Señal de manguera contra incendios",
                   "Skylt brandslang")),
    ("sign_fdc", ("Fire Department Connection Sign", "Schild Feuerwehreinspeisung",
                  "Señal de conexión para bomberos", "Skylt brandförsörjningsanslutning")),
    ("sign_riser", ("Sprinkler Riser Room Sign", "Schild Sprinklerzentrale",
                    "Señal de sala de válvulas de rociadores", "Skylt sprinklercentral")),
    ("sign_fire_door", ("Fire Door Keep Closed Sign", "Schild Brandschutztür geschlossen halten",
                        "Señal de puerta cortafuego mantener cerrada",
                        "Skylt branddörr hålls stängd")),
    ("sign_aed", ("AED Sign", "Schild AED", "Señal de DEA", "Skylt hjärtstartare")),
]
for tex_name, names in SIGNS:
    reg = tex_name[len("sign_"):] + "_sign"
    fp_prop(reg, (2, 1, 15, 14, 15, 16), False, names,
            {reg: model({"face": T(tex_name), "back": T("sign_back"), "particle": T(tex_name)},
                        [box([2, 1, 15.5], [14, 15, 16], "back", per={"north": "face"})])},
            facing_state(M(reg)))


# --- detection (Fire Alarm & Detection tab) --------------------------------------------------
def detector(reg, bbox, names, textures, elements):
    C.add(reg, 'new BlockFireAlarmDetectorFactory("%s", %s)' % (reg, aabb(*bbox)), names,
          {reg: model(textures, elements)}, nsewud_state(M(reg)), tab=FA)


detector("smoke_detector_photoelectric", (3.5, 3.5, 13, 12.5, 12.5, 16),
         ("Photoelectric Smoke Detector", "Optischer Rauchmelder", "Detector de humo fotoeléctrico",
          "Optisk rökdetektor"),
         {"white": T("white"), "face": T("smoke_face"), "particle": T("white")},
         pipe_z(8, 8, 4.5, 14.8, 16, "white")
         + pipe_z(8, 8, 3.5, 13.6, 14.8, "white")
         + [box([5, 5, 13.5], [11, 11, 13.6], "face", faces=("north",))])
detector("duct_smoke_detector", (2, 3, 12, 14, 13, 16),
         ("Duct Smoke Detector", "Kanalrauchmelder", "Detector de humo para conductos",
          "Kanalrökdetektor"),
         {"gray": T("steel"), "front": T("duct_front"), "particle": T("steel")},
         [box([2, 3, 12.5], [14, 13, 16], "gray", per={"north": "front"})])
detector("beam_smoke_detector", (4, 3, 10, 12, 13, 16),
         ("Beam Smoke Detector", "Linienförmiger Rauchmelder", "Detector de humo de haz",
          "Linjerökdetektor"),
         {"body": T("beige"), "front": T("beam_front"), "particle": T("beige")},
         [box([4.5, 3.5, 15], [11.5, 12.5, 16], "body"),
          box([4, 3, 10.5], [12, 13, 15], "body", per={"north": "front"})])

C.add("remote_annunciator",
      'new BlockRemoteAnnunciator("remote_annunciator", %s)' % B(3, 3, 14, 13, 13, 16),
      ("Remote Fire Alarm Annunciator", "Feuerwehr-Anzeigetableau", "Anunciador remoto de alarma",
       "Brandlarmstablå"),
      {"remote_annunciator": model({"face": T("annunciator"), "beige": T("beige"),
                                    "particle": T("beige")},
                                   [box([3, 3, 14.5], [13, 13, 16], "beige",
                                        per={"north": "face"})])},
      facing_state(M("remote_annunciator"),
                   {"alarm": {"false": {}, "true": {"textures": {"face": T("annunciator_alarm")}}}}),
      tab=FA)

# The annunciator's read-out. %s are the device's name and x, y, z.
for key, names in (
        ("normal", ("[Annunciator] System normal.", "[Tableau] System in Ruhe.",
                    "[Anunciador] Sistema en reposo.", "[Tablå] Systemet i normalläge.")),
        ("alarm", ("[Annunciator] FIRE ALARM.", "[Tableau] FEUERALARM.",
                   "[Anunciador] ALARMA DE INCENDIO.", "[Tablå] BRANDLARM.")),
        ("silenced", ("[Annunciator] Fire alarm, signals silenced.",
                      "[Tableau] Feueralarm, Signale abgeschaltet.",
                      "[Anunciador] Alarma de incendio, señales silenciadas.",
                      "[Tablå] Brandlarm, larmdon tystade.")),
        ("drill", ("[Annunciator] Fire drill in progress.", "[Tableau] Räumungsübung läuft.",
                   "[Anunciador] Simulacro de incendio en curso.", "[Tablå] Utrymningsövning pågår.")),
        ("origin", ("[Annunciator] First alarm: %s at %s, %s, %s.",
                    "[Tableau] Erstmeldung: %s bei %s, %s, %s.",
                    "[Anunciador] Primera alarma: %s en %s, %s, %s.",
                    "[Tablå] Första larm: %s vid %s, %s, %s.")),
        ("no_panel", ("[Annunciator] Cannot reach the panel at %s, %s, %s.",
                      "[Tableau] Zentrale bei %s, %s, %s nicht erreichbar.",
                      "[Anunciador] No se puede contactar con el panel en %s, %s, %s.",
                      "[Tablå] Når inte centralen vid %s, %s, %s.")),
        ("unlinked", ("[Annunciator] Not linked to a panel; no alarm sounding nearby.",
                      "[Tableau] Mit keiner Zentrale verbunden; kein Alarm in der Nähe.",
                      "[Anunciador] Sin panel vinculado; ninguna alarma cerca.",
                      "[Tablå] Inte kopplad till en central; inget larm i närheten.")),
        ("unlinked_alarm", ("[Annunciator] Not linked to a panel; a fire alarm is sounding nearby.",
                            "[Tableau] Mit keiner Zentrale verbunden; in der Nähe läuft ein Feueralarm.",
                            "[Anunciador] Sin panel vinculado; suena una alarma de incendio cerca.",
                            "[Tablå] Inte kopplad till en central; ett brandlarm ljuder i närheten."))):
    C.add_lang("csm.lifesafety.annunciator." + key, names)


if __name__ == "__main__":
    sys.exit(C.main())
