#!/usr/bin/env python3
"""Every asset the Technology tab's handheld radios ship: five radios of four makes.

    python dev-env-utils/scripts/gen_technology_radios.py
    python dev-env-utils/scripts/gen_technology_radios.py --check
    python dev-env-utils/scripts/gen_technology_radios.py --fragments   # tab lines to paste

Each is a technology.radio.BlockRadio: carried as an item, stood on a desk or a shelf as a block.

- **Bowfeng BF-5**: the cheap black dual-band handheld. Blue display in a head that stands a
  little proud of the body, an orange key and a blue one, a sixteen-key pad, a silver plate at
  the foot; whip aerial on the left as it faces you, channel knob on the right.
- **Harrow Unison XG-100**: the tall public-safety portable with grey side rails round a black
  face, a speaker grille over a small colour screen, a round navigation pad and a twelve-key
  pad; two knobs and the aerial across the top.
- **Micarolla TalkMate EM-1000**: the blister-pack consumer radio. A waisted body (wider at the
  speaker than at the display), a silver-ringed round display, a stubby moulded aerial and a red
  key on its side.
- **Micarolla APEX 8000**, black and high-visibility yellow-green: the rugged police and fire
  portable. One model and two bodies; the face, keys and knobs are the same.

The makers are invented (Micarolla is the house's own, with Micaplex and Micafi); the words on
the faces are those names and no real maker's mark is drawn.

A radio is small: its body is about 2.5 px wide in a 16 px block, so each face is one picture
(64 px, squeezed to the face's shape as gen_technology_common.Pic does) and everything else is a
flat swatch. The picture is laid over the body's whole front; a body built of two boxes (a head
standing proud, a waist) gives each box its window of the same picture. Models face north and
stand in the middle of their block, on its floor. The display transforms enlarge them in the
hand, the inventory and an item frame, where a 2.5 px model would otherwise be a speck.

The Java box in each tab line is the model's own extent, so the two cannot drift.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_technology_common as tc  # noqa: E402

C = tc.catalogue("gen_technology_radios.py", "radio")
TAB = "CsmTabTechnology"

ALL = tc.ALL
NOT_FRONT = ("south", "east", "west", "up", "down")

BLACK = (27, 28, 30)
RUBBER = (20, 21, 22)
KEY = (52, 54, 58)
KEY_LIGHT = (84, 87, 92)
RAIL_GREY = (140, 144, 150)
SILVER = (186, 190, 195)
ORANGE = (232, 100, 30)
RED = (200, 32, 30)
BLUE_KEY = (40, 96, 214)
LCD_BLUE = (47, 85, 232)
LCD_GREEN = (183, 194, 174)
LCD_PALE = (217, 224, 210)
INK = (22, 26, 20)
WHITE = (244, 247, 255)
GOLD = (196, 160, 72)
HIVIS = (196, 226, 34)
SCREEN_NAVY = (12, 26, 60)

N = 64      # a face picture's side, in texels


# ------------------------------------------------------------------------------------------
# Swatches
# ------------------------------------------------------------------------------------------
for _name, _colour, _seed in (("black", BLACK, 911), ("rubber", RUBBER, 912),
                              ("grey", RAIL_GREY, 913), ("hivis", HIVIS, 914),
                              ("orange", ORANGE, 915), ("red", RED, 916),
                              ("silver", SILVER, 917)):
    C.textures[_name] = (lambda c=_colour, s=_seed: tc.fill(c, 16, 3, s))


def label(img, text, cx, y, colour, w, h):
    """Writes text on a finished face picture, centred on cx with its top at y, both in the
    face's own units (w x h): the picture is square, so the place is scaled to its texels."""
    tc.draw_text_centred(img, text, int(round(cx / float(w) * N)), int(round(y / float(h) * N)),
                         colour)


def keypad(p, x0, y0, x1, y1, cols, rows, key=KEY, edge=KEY_LIGHT, gap=0.7):
    """A grid of keys filling a rectangle: each a dark cap with a lit top edge and a pale dab
    where its legend is printed."""
    kw = (x1 - x0 - gap * (cols - 1)) / float(cols)
    kh = (y1 - y0 - gap * (rows - 1)) / float(rows)
    for r in range(rows):
        for c in range(cols):
            kx = x0 + c * (kw + gap)
            ky = y0 + r * (kh + gap)
            p.rect(kx, ky, kx + kw, ky + kh, key)
            p.rect(kx, ky, kx + kw, ky + kh * 0.22, edge)
            p.rect(kx + kw * 0.3, ky + kh * 0.45, kx + kw * 0.7, ky + kh * 0.75, (206, 210, 214))


def grille_slots(p, x0, y0, x1, y1, rows, dark=(8, 8, 9), lit=(58, 60, 64)):
    """A speaker grille of horizontal slots, each broken in the middle, lit along its top."""
    step = (y1 - y0) / float(rows)
    mid = (x0 + x1) / 2.0
    for r in range(rows):
        y = y0 + r * step
        for a, b in ((x0, mid - 0.5), (mid + 0.5, x1)):
            p.rect(a, y, b, y + step * 0.2, lit)
            p.rect(a, y + step * 0.2, b, y + step * 0.62, dark)


def grille_dots(p, x0, y0, x1, y1, cols, rows, r, dark=(6, 6, 7)):
    for j in range(rows):
        for i in range(cols):
            p.disc(x0 + (x1 - x0) * (i + 0.5) / cols, y0 + (y1 - y0) * (j + 0.5) / rows, r, dark)


def readout(p, x0, y0, x1, y1, ink, lines):
    """Lines of writing on a display, each a row of dashes: (top, height, [start, end]...),
    the starts and ends fractions of the display's width."""
    for top, h, runs in lines:
        for a, b in runs:
            p.rect(x0 + (x1 - x0) * a, y0 + (y1 - y0) * top, x0 + (x1 - x0) * b,
                   y0 + (y1 - y0) * (top + h), ink)


# ------------------------------------------------------------------------------------------
# Faces. Each is drawn in tenths of a model pixel, x right and y down as the radio faces you.
# ------------------------------------------------------------------------------------------
BF5_W, BF5_H = 26, 46


@C.texture("bf5_front")
def bf5_front():
    w, h = BF5_W, BF5_H
    p = tc.Pic(w, h, N, bg=BLACK)
    # the head, with the display in it
    p.rect(0, 0, w, 15, (34, 35, 38))
    p.rect(3, 2.5, 23, 13, (8, 8, 10))
    p.rect(4, 3.5, 22, 12, LCD_BLUE)
    readout(p, 4, 3.5, 22, 12, WHITE, [
        (0.10, 0.10, [(0.05, 0.35), (0.45, 0.60), (0.70, 0.95)]),
        (0.30, 0.25, [(0.12, 0.26), (0.30, 0.44), (0.48, 0.62), (0.68, 0.80), (0.84, 0.95)]),
        (0.65, 0.25, [(0.12, 0.26), (0.30, 0.44), (0.48, 0.62), (0.68, 0.80), (0.84, 0.95)]),
    ])
    # the orange key, the indicator under it, and the grille beside them
    p.rect(1.5, 16.5, 8, 20, (120, 50, 14))
    p.rect(2, 17, 7.5, 19.5, ORANGE)
    p.rect(2.5, 21.5, 4.5, 23.5, (96, 100, 112))
    grille_slots(p, 9.5, 16, 24.5, 24.5, 5)
    # the blue key, the name plate and the band key
    p.rect(1.5, 25.5, 8, 29, BLUE_KEY)
    p.rect(9.5, 25.5, 19.5, 29, (150, 154, 160))
    p.rect(10, 26, 19, 28.5, (34, 35, 38))
    p.rect(21, 26, 24.5, 28.5, KEY)
    keypad(p, 2, 30.5, 24, 42, 4, 4)
    # the silver plate at the foot
    p.rect(7, 43, 19, 45.5, SILVER)
    img = p.done()
    label(img, "BF-5", 13, 43.2, (40, 42, 46), w, h)
    return img


XG_W, XG_H = 24, 54


@C.texture("xg100_front")
def xg100_front():
    w, h = XG_W, XG_H
    p = tc.Pic(w, h, N, bg=RAIL_GREY)
    p.rect(0, 0, 1, h, (112, 116, 122))
    p.rect(w - 1, 0, w, h, (112, 116, 122))
    # the black face between the rails
    p.rect(3, 0, 21, h, (26, 27, 30))
    grille_dots(p, 4.5, 4, 19.5, 17, 5, 5, 0.95, (70, 72, 76))
    grille_dots(p, 4.5, 4, 19.5, 17, 5, 5, 0.6, (6, 6, 7))
    # the model strip
    p.rect(6, 18.2, 18, 20.4, (20, 34, 78))
    p.rect(7, 19, 17, 19.6, (190, 204, 240))
    # the colour screen
    p.rect(4, 21.5, 20, 37, (6, 6, 8))
    p.rect(5, 22.5, 19, 36, SCREEN_NAVY)
    p.rect(5, 22.5, 19, 24.6, (40, 86, 190))
    readout(p, 5, 24.6, 19, 36, WHITE, [
        (0.12, 0.14, [(0.18, 0.82)]),
        (0.42, 0.16, [(0.10, 0.90)]),
        (0.72, 0.12, [(0.30, 0.70)]),
    ])
    p.rect(5, 34.6, 19, 36, (240, 180, 40))
    # soft keys and the round navigation pad
    p.rect(4, 38.5, 7.5, 43.5, KEY)
    p.rect(16.5, 38.5, 20, 43.5, KEY)
    p.disc(12, 41, 3.4, KEY_LIGHT)
    p.disc(12, 41, 2.9, (20, 21, 23))
    p.disc(12, 41, 1.3, (226, 222, 208))
    keypad(p, 4, 45.2, 20, 53.2, 3, 4, gap=0.6)
    img = p.done()
    label(img, "HARROW", 12, 0.6, WHITE, w, h)
    return img


EM_W, EM_H = 28, 36


@C.texture("em1000_front")
def em1000_front():
    w, h = EM_W, EM_H
    p = tc.Pic(w, h, N, bg=BLACK)
    # the silver ring, its four screws and the display inside it
    p.disc(14, 10.5, 8.2, (96, 100, 106))
    p.disc(14, 10.5, 7.7, SILVER)
    for sx, sy in ((8.6, 5.1), (19.4, 5.1), (8.6, 15.9), (19.4, 15.9)):
        p.disc(sx, sy, 0.9, (70, 72, 76))
    p.disc(14, 10.5, 5.9, (14, 15, 16))
    p.rect(9.6, 6.6, 18.4, 14.4, LCD_GREEN)
    # two big digits and a small code under them
    for dx in (10.4, 14.4):
        p.rect(dx, 7.3, dx + 3.2, 8.0, INK)
        p.rect(dx + 2.5, 7.3, dx + 3.2, 9.6, INK)
        p.rect(dx, 9.2, dx + 3.2, 9.9, INK)
        p.rect(dx, 9.2, dx + 0.7, 11.6, INK)
        p.rect(dx, 11.0, dx + 3.2, 11.7, INK)
    p.rect(11.5, 12.6, 16.5, 13.4, INK)
    # the five keys under the ring
    for kx0, kx1 in ((3.5, 7.5), (8.5, 11.5), (16.5, 19.5), (20.5, 24.5)):
        p.rect(kx0, 20.5, kx1, 22.8, KEY)
        p.rect(kx0, 20.5, kx1, 21.0, KEY_LIGHT)
    p.rect(12.3, 20.2, 15.7, 23.4, KEY)
    p.rect(12.3, 20.2, 15.7, 20.8, KEY_LIGHT)
    # the speaker
    grille_dots(p, 7, 28.6, 21, 35, 7, 4, 0.55, (4, 4, 5))
    # the indicator beside the aerial
    p.disc(3.4, 2.2, 0.8, (210, 214, 218))
    img = p.done()
    label(img, "MICAROLLA", 14, 24.6, WHITE, w, h)
    return img


APEX_W, APEX_H = 28, 56


def apex_front(body):
    def draw():
        w, h = APEX_W, APEX_H
        p = tc.Pic(w, h, N, bg=body)
        dark = tc.shade(body, 0.72)
        p.rect(0, 0, 1.2, h, dark)
        p.rect(w - 1.2, 0, w, h, dark)
        # the display
        p.rect(5, 8, 23, 21, (8, 8, 9))
        p.rect(6.2, 9.2, 21.8, 19.8, LCD_PALE)
        readout(p, 6.2, 9.2, 21.8, 19.8, INK, [
            (0.08, 0.10, [(0.04, 0.30), (0.40, 0.58), (0.70, 0.96)]),
            (0.32, 0.20, [(0.14, 0.86)]),
            (0.60, 0.12, [(0.24, 0.76)]),
            (0.82, 0.12, [(0.04, 0.28), (0.38, 0.62), (0.72, 0.96)]),
        ])
        # the black key field: three soft keys, the navigation cluster, the twelve-key pad
        p.rect(4, 22.5, 24, 51, (16, 17, 18))
        keypad(p, 5.5, 23.5, 22.5, 26, 3, 1, gap=1.0)
        p.rect(5.5, 28, 9, 32.5, ORANGE)
        p.rect(19, 28, 22.5, 32.5, KEY)
        p.rect(10.5, 27.2, 17.5, 33.3, KEY)
        p.rect(12.6, 28.6, 15.4, 31.9, (226, 228, 224))
        keypad(p, 5.5, 35, 22.5, 50, 3, 4, gap=0.9)
        # the charging contacts at the foot
        p.rect(4, 52.3, 24, 55.2, (14, 14, 15))
        for i in range(4):
            p.rect(6.2 + i * 4.3, 52.9, 8.8 + i * 4.3, 54.6, GOLD)
        img = p.done()
        ink = INK if sum(body) > 384 else WHITE
        label(img, "MICAROLLA", 14, 1.8, ink, w, h)
        return img
    return draw


C.textures["apex8000_front_black"] = apex_front((30, 31, 33))
C.textures["apex8000_front_hivis"] = apex_front(HIVIS)


# ------------------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------------------
class Body(object):
    """A radio's parts, placed as it faces you: across from its left edge (v), up from the
    floor (y), back from its face (d), all in model pixels. The body is w wide, h tall and
    deep thick, centred in the block."""

    def __init__(self, w, h, deep):
        self.w, self.h, self.deep = float(w), float(h), float(deep)
        self.els = []

    def _box(self, v0, v1, y0, y1, d0, d1):
        # the radio's left, as it faces you, is the block's east
        x0, x1 = 8 + self.w / 2.0 - v1, 8 + self.w / 2.0 - v0
        z0 = 8 - self.deep / 2.0
        return (x0, y0, z0 + d0), (x1, y1, z0 + d1)

    def part(self, v0, v1, y0, y1, d0, d1, tex, faces=ALL):
        frm, to = self._box(v0, v1, y0, y1, d0, d1)
        self.els.append(tc.E(frm, to, tex, faces=faces))

    def faced(self, v0, v1, y0, y1, d0, d1, tex):
        """A piece of the body: its front the window of the face picture that it covers."""
        frm, to = self._box(v0, v1, y0, y1, d0, d1)
        window = [16 * v0 / self.w, 16 * (1 - y1 / self.h), 16 * v1 / self.w,
                  16 * (1 - y0 / self.h)]
        self.els.append(tc.E(frm, to, tex, per={"north": "front"}, uv={"north": window}))

    def aerial(self, v, y, length, thick=0.5, base=0.8, d=None):
        """A whip standing on the top at v (its middle), with a thicker foot."""
        d = self.deep / 2.0 if d is None else d
        self.part(v - base / 2, v + base / 2, y, y + 0.9, d - base / 2, d + base / 2, "rubber",
                  ("north", "south", "east", "west", "up"))
        self.part(v - thick / 2, v + thick / 2, y + 0.9, y + length, d - thick / 2,
                  d + thick / 2, "rubber", ("north", "south", "east", "west", "up"))

    def knob(self, v, y, size, tall, tex="rubber"):
        d = self.deep / 2.0
        self.part(v - size / 2, v + size / 2, y, y + tall, d - size / 2, d + size / 2, tex,
                  ("north", "south", "east", "west", "up"))

    def clip(self, y0, y1, tex="rubber"):
        """The belt clip on the back."""
        self.part(self.w * 0.3, self.w * 0.7, y0, y1, self.deep, self.deep + 0.25, tex,
                  ("south", "east", "west", "up", "down"))


def display(height):
    """Item transforms for a model a few pixels across standing on the block's floor: enlarged
    everywhere, and lifted so its middle and not its foot is where the transform turns it."""
    def lift(scale):
        return round((8 - height / 2.0) * scale, 2)
    gui = min(1.7, 15.0 / height)
    return {
        "gui": {"rotation": [12, 200, 0], "translation": [0, lift(gui), 0],
                "scale": [round(gui, 3)] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.8] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, lift(1.4), 0],
                  "scale": [1.4] * 3},
        "head": {"rotation": [0, 180, 0], "translation": [0, lift(1.4), 0],
                 "scale": [1.4] * 3},
        "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 6.5, 1.5],
                                  "scale": [0.9] * 3},
        "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 6.5, 1.5],
                                 "scale": [0.9] * 3},
        "firstperson_righthand": {"rotation": [0, 195, 0], "translation": [0, 7, 0],
                                  "scale": [1.0] * 3},
        "firstperson_lefthand": {"rotation": [0, 165, 0], "translation": [0, 7, 0],
                                 "scale": [1.0] * 3},
    }


def radio(registry, java_model, names, body, textures):
    els = body.els
    box = tc.extent(els)
    height = box[4]
    m = tc.with_display(tc.model(dict(textures, particle=textures["body"]), els),
                        display(height))
    C.add(registry,
          'new BlockRadio("%s", RadioModel.%s, %s)' % (registry, java_model, tc.box_java(box)),
          names, {registry: m}, tc.facing_state(C.M(registry)), tab=TAB)


def bf5():
    b = Body(2.6, 4.6, 1.4)
    b.faced(0, 2.6, 0, 3.1, 0, 1.4, "body")
    b.faced(0, 2.6, 3.1, 4.6, -0.15, 1.4, "body")      # the head, proud of the body
    b.aerial(0.5, 4.6, 5.6)
    b.knob(2.0, 4.6, 0.7, 0.75)
    b.knob(1.3, 4.6, 0.35, 0.3, "silver")              # the lamp
    b.part(-0.12, 0, 2.2, 3.6, 0.3, 1.1, "rubber", ("north", "south", "east", "up", "down"))
    b.part(-0.12, 0, 3.75, 4.15, 0.4, 0.9, "orange", ("north", "south", "east", "up", "down"))
    b.clip(1.6, 4.2)
    radio("radio_bowfeng_bf5", "BOWFENG_BF5",
          tc.names("Bowfeng BF-5 Handheld Radio", "Bowfeng BF-5 Handfunkgerät",
                   "Radio Portátil Bowfeng BF-5", "Bowfeng BF-5 Handradio"),
          b, {"front": C.T("bf5_front"), "body": C.T("black"), "rubber": C.T("rubber"),
              "orange": C.T("orange"), "silver": C.T("silver")})


def xg100():
    b = Body(2.4, 5.4, 1.4)
    b.faced(0, 2.4, 0, 5.4, 0, 1.4, "body")
    b.part(0.3, 2.1, 5.4, 5.6, 0.1, 1.3, "rubber", ("north", "south", "east", "west", "up"))
    b.knob(0.55, 5.6, 0.5, 0.55)
    b.knob(1.2, 5.6, 0.55, 0.75)
    b.aerial(1.95, 5.6, 5.6, base=0.7)
    b.part(-0.1, 0, 3.0, 4.3, 0.35, 1.05, "orange", ("north", "south", "east", "up", "down"))
    b.clip(2.2, 5.0)
    radio("radio_harrow_xg100", "HARROW_XG100",
          tc.names("Harrow Unison XG-100 Portable Radio", "Harrow Unison XG-100 Handfunkgerät",
                   "Radio Portátil Harrow Unison XG-100", "Harrow Unison XG-100 Handradio"),
          b, {"front": C.T("xg100_front"), "body": C.T("grey"), "rubber": C.T("rubber"),
              "orange": C.T("orange")})


def em1000():
    b = Body(2.8, 3.6, 1.3)
    b.faced(0, 2.8, 0, 1.7, 0, 1.3, "body")            # the speaker end, the wider
    b.faced(0.2, 2.6, 1.7, 3.6, 0.05, 1.25, "body")    # the display end
    # the moulded aerial on the right as it faces you, and the nub on the left
    b.part(1.75, 2.55, 3.6, 6.1, 0.25, 1.05, "body", ("north", "south", "east", "west", "up"))
    b.part(1.85, 2.45, 6.1, 6.5, 0.35, 0.95, "body", ("north", "south", "east", "west", "up"))
    b.knob(0.65, 3.6, 0.6, 0.45, "body")
    b.part(0.08, 0.2, 2.2, 3.1, 0.35, 0.95, "red", ("north", "south", "east", "up", "down"))
    b.clip(1.2, 3.2, "body")
    radio("radio_micarolla_em1000", "MICAROLLA_EM1000",
          tc.names("Micarolla TalkMate EM-1000 Walkie-Talkie",
                   "Micarolla TalkMate EM-1000 Walkie-Talkie",
                   "Walkie-Talkie Micarolla TalkMate EM-1000",
                   "Micarolla TalkMate EM-1000 Walkie-talkie"),
          b, {"front": C.T("em1000_front"), "body": C.T("black"), "red": C.T("red")})


def apex8000(colour, body_texture, names):
    b = Body(2.8, 5.6, 1.8)
    b.faced(0, 2.8, 0, 5.6, 0, 1.8, "body")
    b.aerial(0.55, 5.6, 5.9, thick=0.55, base=0.85)
    b.knob(1.35, 5.6, 0.5, 0.5)
    b.knob(1.35, 6.1, 0.3, 0.2, "orange")              # the emergency key
    b.knob(2.2, 5.6, 0.85, 0.85)
    # the talk key's panel on the left side as it faces you
    b.part(-0.12, 0, 2.6, 4.6, 0.35, 1.45, "rubber", ("north", "south", "east", "up", "down"))
    b.clip(2.4, 5.2)
    radio("radio_micarolla_apex8000_" + colour, "MICAROLLA_APEX8000", names, b,
          {"front": C.T("apex8000_front_" + colour), "body": C.T(body_texture),
           "rubber": C.T("rubber"), "orange": C.T("orange")})


bf5()
xg100()
em1000()
apex8000("black", "black",
         tc.names("Micarolla APEX 8000 Portable Radio (Black)",
                  "Micarolla APEX 8000 Handfunkgerät (Schwarz)",
                  "Radio Portátil Micarolla APEX 8000 (Negro)",
                  "Micarolla APEX 8000 Handradio (Svart)"))
apex8000("hivis", "hivis",
         tc.names("Micarolla APEX 8000 Portable Radio (Hi-Vis)",
                  "Micarolla APEX 8000 Handfunkgerät (Signalgelb)",
                  "Radio Portátil Micarolla APEX 8000 (Alta Visibilidad)",
                  "Micarolla APEX 8000 Handradio (Varselgul)"))

C.add_lang("csm.radio.tooltip.channel",
           ("Channel: %s (%s MHz)", "Kanal: %s (%s MHz)", "Canal: %s (%s MHz)",
            "Kanal: %s (%s MHz)"))
C.add_lang("csm.radio.tooltip.on",
           ("Power: On", "Eingeschaltet", "Encendida", "Påslagen"))
C.add_lang("csm.radio.tooltip.off",
           ("Power: Off", "Ausgeschaltet", "Apagada", "Avstängd"))
C.add_lang("csm.radio.tooltip.unit",
           ("Unit ID: %s", "Gerätekennung: %s", "ID de unidad: %s", "Enhets-ID: %s"))
C.add_lang("csm.radio.tooltip.use",
           ("Right-click to open the radio", "Rechtsklick öffnet das Funkgerät",
            "Clic derecho para abrir la radio", "Högerklicka för att öppna radion"))
C.add_lang("csm.radio.tooltip.place",
           ("Sneak and right-click a surface to set it down",
            "Schleichen und Rechtsklick auf eine Fläche stellt es ab",
            "Agáchate y haz clic derecho en una superficie para dejarla",
            "Smyg och högerklicka på en yta för att ställa ner den"))

if __name__ == "__main__":
    sys.exit(C.main())
