#!/usr/bin/env python3
"""Every asset the Technology - Computers & Peripherals tab ships.

    python dev-env-utils/scripts/gen_technology_computers.py
    python dev-env-utils/scripts/gen_technology_computers.py --check
    python dev-env-utils/scripts/gen_technology_computers.py --fragments   # tab lines to paste

Everything sits at the bottom of its block, as the iMac and the Mac keyboard do: a desk device
placed on a desk stands on the desk's top. Desk devices are BlockDeskDeviceFactory instances
(they turn to the placer and never stand on end); anything with a screen is a
BlockDeskScreenFactory, whose click changes the screen between a desktop, a game, code and an
idle screensaver, each an animated texture the blockstate picks, so nothing ticks.

What is here, in creative order:

- **Gaming rigs**: mid towers with a tempered glass side and RGB fans (black, white), a full
  tower, a mini-ITX cube, a workstation, a small form factor desktop, a mini PC, an open test
  bench, and a gaming laptop open and closed. The glass is not see-through: the interior is
  painted on the side as an animated texture, which costs one quad and keeps the block SOLID.
- **Monitors**: 24" office (with and without a webcam on its bezel), 27" gaming (black, white,
  an RGB ring on the back), a 34" curved ultrawide (two wings turned 22.5 degrees, reaching past
  the block), two 24" screens on one arm (past the block both ways) and a portrait monitor.
- **Keyboards**: full-size, TKL, 60%, split, low-profile, RGB (an animated underglow), retro
  beige, compact wireless and numpads, in several keycap colourways.
- **Mice and pads**: gaming, vertical, trackball and travel mice; a mouse pad and an XXL desk
  mat, plain and with an RGB edge.
- **Desk extras**: a headset on its stand, a webcam on a tripod, a microphone on a boom arm,
  desktop speakers and a subwoofer, a USB hub, an external drive, a vertical laptop dock, a KVM
  switch and a macro keypad.
- **Consoles**: the Orbix and Vertex consoles, the Pocketeer handheld in its dock and a retro
  16-bit console; their controllers and a VR headset are items (ItemDecorativeFactory).

Every brand is invented. Keyboard layouts are drawn on one 256 px sheet, a band a layout and
colourway, each at the layout's true aspect at UNIT texels a key unit; a keyboard's key bed takes
its band by uv. Screens are drawn at their screen's aspect and squeezed square, so they come out
right on a face of that aspect.

Writes models under models/block/technology/computers, textures under
textures/blocks/technology/computers, and the shared swatches under
textures/blocks/technology/common (see gen_technology_common.py).
"""
import colorsys
import math
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_park_plantings as pp  # noqa: E402
import gen_technology_common as tc  # noqa: E402

C = tc.catalogue("gen_technology_computers.py", "computers")
TAB = tc.COMPUTERS

C.add_lang("itemGroup.tabtechcomputers",
           ("CSM: Technology - Computers & Peripherals", "CSM: Technik - Computer & Peripherie",
            "CSM: Tecnología - Ordenadores y periféricos",
            "CSM: Teknik - Datorer & kringutrustning"))

ALL = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west", "down")
FULL = [0, 0, 16, 16]


# ------------------------------------------------------------------------------------------
# Drawing helpers
# ------------------------------------------------------------------------------------------
rgba = tc.rgba


def hue(h, s=0.85, v=1.0):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, s, v)
    return (int(r * 255), int(g * 255), int(b * 255))


Pic = tc.Pic
grain = tc.grain


def anim(name, n, draw, frametime, interpolate=False):
    """An animated texture of n frames in this catalogue; draw(f) returns frame f."""
    tc.anim(C, name, n, draw, frametime, interpolate)


def tex(name):
    """Decorator: a static catalogue texture."""
    return C.texture(name)


# ------------------------------------------------------------------------------------------
# Element helpers
# ------------------------------------------------------------------------------------------
E = tc.E


def post(cx, cz, r, y0, y1, t, top=True, bottom=True):
    return tc.lsc.post(cx, cz, r, y0, y1, t, top=top, bottom=bottom)


extent = tc.extent


SCREENS = ("desktop", "game", "code", "idle")


def add_device(reg, names, els, textures, display=None, screens=None, box=None):
    """Adds a desk device: its model, a facing blockstate (with the screen variants when
    ``screens`` maps texture keys to their four pictures), and the tab line."""
    b = box or extent(els)
    textures = dict(textures)
    textures.setdefault("particle", textures[sorted(textures)[0]])
    if screens:
        for k, sets in screens.items():
            textures[k] = sets[0]
    m = tc.model(textures, els)
    if display is not False:
        m = tc.with_display(m, display or tc.desk_display(0.85, 2))
    extra = None
    if screens:
        extra = {"screen": {s: {"textures": {k: v[i] for k, v in screens.items()}}
                            for i, s in enumerate(SCREENS)}}
        java = 'new BlockDeskScreenFactory("%s", %s)' % (reg, tc.box_java(b))
    else:
        java = tc.desk_device_java(reg, b)
    C.add(reg, java, names, {reg: m}, tc.facing_state(C.M(reg), extra), tab=TAB)


def N(en, de, es, sv):
    return (en, de, es, sv)


# ------------------------------------------------------------------------------------------
# Palette and catalogue swatches
# ------------------------------------------------------------------------------------------
BLACK = (34, 35, 38)
DARK = (22, 23, 26)
WHITE = (234, 235, 233)
SILVER = (176, 178, 182)
BEIGE = (214, 204, 180)
PINK = (246, 200, 216)
RETRO_GREY = (150, 150, 146)

C.textures["brushed_silver"] = lambda: _brushed(SILVER, 601)
C.textures["mesh_black"] = lambda: _mesh(BLACK, (8, 8, 9))
C.textures["mesh_white"] = lambda: _mesh(WHITE, (150, 152, 154))
C.textures["beige_plastic"] = lambda: tc.fill(BEIGE, 16, 3, 602)
C.textures["pastel_pink"] = lambda: tc.fill(PINK, 16, 2, 603)
C.textures["retro_grey"] = lambda: tc.fill(RETRO_GREY, 16, 3, 604)
C.textures["cloth_black"] = lambda: tc.fill((24, 25, 28), 16, 4, 605)
C.textures["lens_glass"] = lambda: tc.fill((12, 14, 20), 16, 2, 606)


def _brushed(c, seed):
    """Brushed aluminium: streaks running across."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        row = rng.uniform(-7, 7)
        for x in range(16):
            px[x, y] = tc.clamp(tuple(v + row + rng.uniform(-2, 2) for v in c)) + (255,)
    return img


def _mesh(c, hole):
    """Perforated steel: a grid of round holes, tiling across faces."""
    img = Image.new("RGBA", (16, 16), rgba(c))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x % 2 == 0) and (y % 2 == (x // 2) % 2):
                px[x, y] = rgba(hole)
    return img


# ------------------------------------------------------------------------------------------
# Screens
# ------------------------------------------------------------------------------------------
ASPECTS = {"w": (16, 9), "uw": (21, 9), "tall": (9, 16)}
SCREEN_FRAMES = {"desktop": (4, 20), "game": (8, 3), "code": (8, 8), "idle": (8, 6)}


def _screen_desktop(W, H, f):
    p = Pic(W, H, 64, 6)
    p.vgrad(0, 0, W, H, (40, 96, 150), (110, 70, 150))
    # soft hills on the wallpaper
    p.ell(W * 0.25, H * 1.05, W * 0.5, H * 0.35, (60, 120, 160))
    p.ell(W * 0.8, H * 1.1, W * 0.45, H * 0.4, (80, 100, 170))
    # icons down the left
    for i in range(3):
        y = 0.6 + i * 1.5
        p.rect(0.5, y, 1.4, y + 0.9, (230, 200, 90) if i == 0 else (220, 226, 236))
        p.rect(0.4, y + 1.0, 1.5, y + 1.15, (240, 240, 245))
    # a window
    x0, y0, x1, y1 = W * 0.22, H * 0.1, W * 0.86, H * 0.78
    p.rect(x0 + 0.15, y0 + 0.15, x1 + 0.15, y1 + 0.15, (20, 22, 40))
    p.rect(x0, y0, x1, y1, (238, 240, 244))
    p.rect(x0, y0, x1, y0 + 0.6, (54, 58, 70))
    for k, c in enumerate(((236, 96, 90), (240, 190, 70), (110, 200, 110))):
        p.disc(x1 - 0.4 - k * 0.5, y0 + 0.3, 0.15, c)
    p.rect(x0 + 0.4, y0 + 1.0, x0 + (x1 - x0) * 0.45, y0 + 1.25, (60, 64, 80))
    for k in range(int((y1 - y0 - 1.8) / 0.45)):
        ww = (x1 - x0 - 0.8) * (0.55 + 0.4 * ((k * 37) % 10) / 10.0)
        p.rect(x0 + 0.4, y0 + 1.6 + k * 0.45, x0 + 0.4 + ww * 0.6, y0 + 1.8 + k * 0.45,
               (150, 156, 170))
    p.rect(x0 + (x1 - x0) * 0.62, y0 + 1.0, x1 - 0.4, y0 + (y1 - y0) * 0.6, (120, 170, 220))
    # taskbar with a clock whose colon blinks
    p.rect(0, H - 0.7, W, H, (24, 26, 32))
    p.rect(0.2, H - 0.55, 0.65, H - 0.15, (80, 160, 240))
    for k in range(4):
        p.rect(1.0 + k * 0.7, H - 0.55, 1.5 + k * 0.7, H - 0.15, (150, 160, 180))
    p.rect(W - 1.6, H - 0.5, W - 0.3, H - 0.2, (220, 224, 230) if f % 2 == 0 else (160, 164, 170))
    # the cursor wanders over the window
    cx, cy = [(W * 0.5, H * 0.45), (W * 0.62, H * 0.4), (W * 0.58, H * 0.55),
              (W * 0.45, H * 0.5)][f]
    p.poly([(cx, cy), (cx, cy + 0.9), (cx + 0.25, cy + 0.65), (cx + 0.6, cy + 0.6)],
           (255, 255, 255))
    return p.done()


def _screen_game(W, H, f):
    p = Pic(W, H, 64, 6)
    horizon = H * 0.48
    p.vgrad(0, 0, W, horizon, (70, 130, 220), (190, 220, 250))
    p.disc(W * 0.78, H * 0.15, 0.7, (255, 236, 160))
    # clouds drifting
    for k in range(3):
        cx = ((k * W / 3.0 + f * W / 32.0) % (W + 2)) - 1
        p.ell(cx, H * 0.12 + k * 0.5, 1.2, 0.35, (245, 248, 252))
    # mountains sliding slowly
    off = -f * W / 64.0
    for k in range(-1, 5):
        mx = off + k * W / 3.0
        p.poly([(mx - W * 0.22, horizon), (mx, horizon - H * 0.22), (mx + W * 0.22, horizon)],
               (90, 110, 130))
        p.poly([(mx - 0.4, horizon - H * 0.22 + 0.6), (mx, horizon - H * 0.22),
                (mx + 0.4, horizon - H * 0.22 + 0.6)], (240, 244, 250))
    p.rect(0, horizon, W, H, (70, 150, 70))
    # the road and its dashes running toward the player
    cx = W / 2.0
    p.poly([(cx - 0.3, horizon), (cx + 0.3, horizon), (cx + W * 0.42, H), (cx - W * 0.42, H)],
           (70, 72, 78))
    for k in range(6):
        t = ((k + f / 8.0) / 6.0)
        y0 = horizon + (H - horizon) * t * t
        y1 = horizon + (H - horizon) * min(1.0, (t + 0.06) ** 2)
        w = 0.05 + 0.35 * t
        p.rect(cx - w / 2, y0, cx + w / 2, y1 + 0.05, (240, 230, 120))
    # the car
    p.rect(cx - 1.1, H - 2.2, cx + 1.1, H - 1.0, (210, 40, 50))
    p.rect(cx - 0.7, H - 2.7, cx + 0.7, H - 2.2, (60, 70, 90))
    # HUD: health, minimap, a speed bar
    p.rect(0.4, 0.4, 3.4, 0.8, (40, 20, 20))
    p.rect(0.4, 0.4, 2.6, 0.8, (230, 60, 60))
    p.disc(W - 1.4, H - 1.4, 1.0, (20, 30, 24))
    p.disc(W - 1.4, H - 1.4, 0.15, (240, 240, 80))
    p.rect(0.4, H - 0.9, 0.4 + 2.0 + 0.2 * (f % 4), H - 0.6, (90, 220, 255))
    return p.done()


_CODE_COLOURS = [(198, 120, 221), (97, 175, 239), (152, 195, 121), (229, 192, 123),
                 (224, 108, 117), (171, 178, 191)]


def _screen_code(W, H, f):
    p = Pic(W, H, 64, 6, bg=(30, 32, 40))
    p.rect(0, 0, W, 0.55, (24, 25, 32))
    p.rect(0.2, 0.05, 2.4, 0.55, (30, 32, 40))
    p.rect(2.5, 0.1, 4.5, 0.5, (40, 42, 52))
    p.rect(0, 0.55, 1.1, H - 0.4, (36, 38, 48))
    p.rect(0, H - 0.4, W, H, (120, 80, 200))
    rng = random.Random(77)
    lh = 0.5
    rows = int((H - 1.1) / lh)
    typed = rows - 8 + f
    for r in range(rows):
        y = 0.8 + r * lh
        p.rect(0.3, y, 0.8, y + 0.22, (90, 94, 110))
        indent = rng.choice((0, 0, 1, 1, 2, 3)) * 0.6
        x = 1.4 + indent
        toks = [(rng.uniform(0.4, 2.0), rng.choice(_CODE_COLOURS)) for _ in range(rng.randint(1, 5))]
        if r > typed:
            continue
        for k, (w, c) in enumerate(toks):
            if r == typed and k > (f % 3) + 1:
                break
            if x + w > W - 0.3:
                break
            p.rect(x, y, x + w, y + 0.24, c)
            x += w + 0.25
        if r == typed and f % 2 == 0:
            p.rect(x, y - 0.05, x + 0.12, y + 0.3, (230, 230, 230))
    return p.done()


def _screen_idle(W, H, f):
    p = Pic(W, H, 64, 6, bg=(4, 4, 8))
    path = [(0.2, 0.25), (0.5, 0.7), (0.8, 0.3), (0.65, 0.75), (0.3, 0.6), (0.15, 0.4),
            (0.45, 0.2), (0.75, 0.55)]
    u, v = path[f]
    cx, cy = u * W, v * H
    c = hue(f / 8.0, 0.7, 1.0)
    r = min(W, H) * 0.12
    p.poly([(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)], c)
    p.poly([(cx, cy - r * 0.45), (cx + r * 0.45, cy), (cx, cy + r * 0.45), (cx - r * 0.45, cy)],
           (4, 4, 8))
    return p.done()


_SCREEN_DRAW = {"desktop": _screen_desktop, "game": _screen_game, "code": _screen_code,
                "idle": _screen_idle}

for _asp, (_W, _H) in ASPECTS.items():
    for _kind, (_n, _ft) in SCREEN_FRAMES.items():
        anim("screen_%s_%s" % (_asp, _kind), _n,
             (lambda f, k=_kind, W=_W, H=_H: _SCREEN_DRAW[k](W, H, f)), _ft)


def screen_set(asp):
    return [C.T("screen_%s_%s" % (asp, k)) for k in SCREENS]


# ------------------------------------------------------------------------------------------
# Fans, interiors and fronts (the gaming rigs)
# ------------------------------------------------------------------------------------------
RIG_FRAMES = 8


def _fan(p, cx, cy, r, f, k, frame_c=(18, 18, 20)):
    """One RGB fan seen face on: a dark frame, the lit ring, the blades and the hub."""
    p.disc(cx, cy, r, frame_c)
    p.ring(cx, cy, r * 0.92, r * 0.16, hue(f / float(RIG_FRAMES) + k * 0.13))
    p.disc(cx, cy, r * 0.76, (28, 28, 32))
    for b in range(5):
        a = (b * 72 + f * 9) * math.pi / 180
        p.line([(cx, cy), (cx + math.cos(a) * r * 0.72, cy + math.sin(a) * r * 0.72)],
               (52, 52, 58), r * 0.22)
    p.disc(cx, cy, r * 0.25, hue(f / float(RIG_FRAMES) + k * 0.13 + 0.5, 0.6, 0.9))


def _front_fans(W, H, n_fans, panel, f, mesh=True):
    p = Pic(W, H, 32, 6, bg=panel)
    if mesh:
        step = 0.5
        y = 0.3
        while y < H:
            x = 0.3 + (0.25 if int(y / step) % 2 else 0)
            while x < W:
                p.disc(x, y, 0.1, tc.shade(panel, 0.45 if sum(panel) < 300 else 0.86))
                x += step
            y += step
    r = min(W * 0.4, (H - 1.2) / (2.0 * n_fans + 0.4))
    gap = (H - 1.0) / n_fans
    for k in range(n_fans):
        _fan(p, W / 2.0, 0.6 + gap * (k + 0.5), r, f, k)
    p.rect(W / 2.0 - 0.35, 0.15, W / 2.0 + 0.35, 0.45, tc.shade(panel, 1.3))
    return p.done()


def _interior(W, H, f, light, gpus=1):
    """The inside of a gaming tower, as seen through its glass side from the east: the back of
    the case at the left, the front fans at the right."""
    tray = (40, 42, 46) if not light else (214, 216, 218)
    board = (26, 28, 30) if not light else (230, 232, 234)
    p = Pic(W, H, 64, 5, bg=tray)
    hf = f / float(RIG_FRAMES)
    # the motherboard and its parts
    p.rect(1.2, 1.0, W * 0.62, H * 0.62, board)
    p.rect(1.2, 1.0, 2.0, H * 0.42, (60, 62, 66))                     # rear I/O cover
    p.rect(W * 0.28, 2.0, W * 0.44, 4.6, (70, 72, 78))                 # CPU block
    p.ring(W * 0.36, 3.3, 1.0, 0.25, hue(hf + 0.3))                   # pump ring
    p.disc(W * 0.36, 3.3, 0.55, (14, 14, 16))
    for k in range(4):                                                  # RAM sticks
        x = W * 0.48 + k * 0.42
        p.rect(x, 1.6, x + 0.28, 5.2, (30, 30, 34))
        p.rect(x, 1.6, x + 0.28, 2.1, hue(hf + k * 0.08))
    # rear fan
    _fan(p, 2.0 + 0.9, 2.6, 1.3, f, 3)
    # the graphics card(s), with an RGB edge
    for g in range(gpus):
        y = H * 0.48 + g * 1.9
        p.rect(1.6, y, W * 0.72, y + 1.7, (36, 38, 44) if not light else (200, 202, 206))
        p.rect(1.6, y + 1.45, W * 0.72, y + 1.7, hue(hf + 0.6 + g * 0.2))
        p.rect(2.2, y + 0.4, 5.4, y + 0.75, (180, 184, 190))
    # the PSU shroud along the bottom
    p.rect(0, H * 0.8, W, H, (24, 25, 28) if not light else (236, 236, 238))
    p.rect(0.8, H * 0.86, W * 0.4, H * 0.86 + 0.25, hue(hf + 0.9))
    # the front fans, edge on: three lit strips at the right
    for k in range(3):
        y = 1.0 + k * (H * 0.75 - 1.0) / 3.0
        p.rect(W - 1.5, y + 0.2, W - 0.6, y + (H * 0.75 - 1.0) / 3.0 - 0.2, (20, 20, 22))
        p.rect(W - 1.45, y + 0.3, W - 1.25, y + (H * 0.75 - 1.0) / 3.0 - 0.3, hue(hf + k * 0.13))
    img = p.done()
    # the glass: a dark tint and one reflection running across it
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            k = 0.78
            if 0 < (x + y * 0.6) - img.width * 0.55 < 5:
                k = 1.05
            px[x, y] = tc.clamp((r * k + 6, g * k + 6, b * k + 9)) + (a,)
    frame_c = (16, 16, 18) if not light else (220, 222, 224)
    tc.frame(img, 0, 0, img.width, img.height, frame_c, 2)
    return img


def _rear(W, H, light):
    """The back of a tower: the I/O shield, the expansion slots, the PSU at the bottom."""
    body = BLACK if not light else WHITE
    p = Pic(W, H, 32, 6, bg=body)
    p.rect(W * 0.12, H * 0.08, W * 0.42, H * 0.38, (60, 62, 66))
    for k in range(5):
        p.rect(W * 0.16, H * 0.1 + k * 0.6, W * 0.38, H * 0.1 + k * 0.6 + 0.35, (20, 22, 24))
    p.disc(W * 0.7, H * 0.2, min(W, H) * 0.18, (20, 20, 22))
    p.ring(W * 0.7, H * 0.2, min(W, H) * 0.18, 0.2, tc.shade(body, 0.7))
    for k in range(7):
        y = H * 0.44 + k * 0.55
        p.rect(W * 0.12, y, W * 0.88, y + 0.3, tc.shade(body, 0.7))
    p.rect(W * 0.1, H * 0.84, W * 0.9, H * 0.97, (30, 31, 34))
    p.disc(W * 0.3, H * 0.905, 0.45, (12, 12, 14))
    p.rect(W * 0.65, H * 0.87, W * 0.8, H * 0.94, (12, 12, 14))
    return p.done()


def _top_vent(W, D, light):
    body = BLACK if not light else WHITE
    p = Pic(W, D, 32, 6, bg=body)
    p.rect(0.6, 0.6, W - 0.6, D * 0.75, tc.shade(body, 0.55 if not light else 0.85))
    y = 0.8
    while y < D * 0.73:
        x = 0.8
        while x < W - 0.7:
            p.disc(x, y, 0.12, tc.shade(body, 0.3 if not light else 0.6))
            x += 0.45
        y += 0.45
    p.rect(1.0, D * 0.82, 1.8, D * 0.9, (30, 30, 32) if not light else (190, 190, 192))
    p.disc(W - 1.4, D * 0.86, 0.35, (100, 200, 255))
    return p.done()


# mid tower: 7 wide, 14.4 tall on 0.6 feet, 14 deep
MID = (4.5, 0.6, 1.0, 11.5, 15.0, 15.0)
FULLT = (4.0, 0.6, 0.5, 12.0, 16.0, 15.5)
for _light, _sfx in ((False, "black"), (True, "white")):
    _x0, _y0, _z0, _x1, _y1, _z1 = MID
    anim("mid_front_%s" % _sfx, RIG_FRAMES,
         (lambda f, L=_light: _front_fans(MID[3] - MID[0], MID[4] - MID[1], 3,
                                          WHITE if L else BLACK, f)), 3)
    anim("mid_glass_%s" % _sfx, RIG_FRAMES,
         (lambda f, L=_light: _interior(MID[5] - MID[2], MID[4] - MID[1], f, L)), 3)
    C.textures["mid_rear_%s" % _sfx] = (lambda L=_light: _rear(7, 14.4, L))
    C.textures["mid_top_%s" % _sfx] = (lambda L=_light: _top_vent(7, 14, L))
anim("full_front_black", RIG_FRAMES,
     lambda f: _front_fans(FULLT[3] - FULLT[0], FULLT[4] - FULLT[1], 2, BLACK, f), 3)
anim("full_glass_black", RIG_FRAMES,
     lambda f: _interior(FULLT[5] - FULLT[2], FULLT[4] - FULLT[1], f, False, gpus=2), 3)
C.textures["full_rear_black"] = lambda: _rear(8, 15.4, False)
C.textures["full_top_black"] = lambda: _top_vent(8, 15, False)
anim("itx_front_black", RIG_FRAMES, lambda f: _front_fans(8, 9, 1, BLACK, f), 3)
C.textures["itx_rear_black"] = lambda: _rear(8, 9, False)


def feet(x0, z0, x1, z1, h, t="feet"):
    return [E((x, 0, z), (x + 1.2, h, z + 1.2), t, SIDES)
            for x in (x0 + 0.4, x1 - 1.6) for z in (z0 + 0.6, z1 - 1.8)]


def tower(reg, names, bx, front, glass, rear, top, side_t, feet_t, body_t):
    x0, y0, z0, x1, y1, z1 = bx
    els = [E((x0, y0, z0), (x1, y1, z1), "body",
             per={"north": "front", "east": "glass", "south": "rear", "up": "top"},
             full=("north", "east", "south"),
             uv={"up": FULL}, rot={"up": 180})]
    els += feet(x0, z0, x1, z1, y0, "feet")
    add_device(reg, names, els,
               {"body": side_t, "front": front, "glass": glass, "rear": rear, "top": top,
                "feet": feet_t, "particle": side_t}, display=False)


tower("pc_tower_gaming_mid_black",
      N("Gaming PC (Mid Tower, Black)", "Gaming-PC (Midi-Tower, Schwarz)",
        "PC Gaming (Semitorre, Negro)", "Speldator (miditorn, svart)"),
      MID, C.T("mid_front_black"), C.T("mid_glass_black"), C.T("mid_rear_black"),
      C.T("mid_top_black"), tc.T("black_anodised"), tc.T("port_black"), None)
tower("pc_tower_gaming_mid_white",
      N("Gaming PC (Mid Tower, White)", "Gaming-PC (Midi-Tower, Weiß)",
        "PC Gaming (Semitorre, Blanco)", "Speldator (miditorn, vit)"),
      MID, C.T("mid_front_white"), C.T("mid_glass_white"), C.T("mid_rear_white"),
      C.T("mid_top_white"), tc.T("white_plastic"), tc.T("grey_abs"), None)
tower("pc_tower_gaming_full_black",
      N("Gaming PC (Full Tower, Black)", "Gaming-PC (Big-Tower, Schwarz)",
        "PC Gaming (Torre Completa, Negro)", "Speldator (fulltorn, svart)"),
      FULLT, C.T("full_front_black"), C.T("full_glass_black"), C.T("full_rear_black"),
      C.T("full_top_black"), tc.T("black_anodised"), tc.T("port_black"), None)


# mini-ITX: a mesh cube with one fan behind its front
def _itx():
    x0, y0, z0, x1, y1, z1 = 4.0, 0.5, 3.0, 12.0, 9.5, 14.0
    els = [E((x0, y0, z0), (x1, y1, z1), "mesh",
             per={"north": "front", "south": "rear"}, full=("north", "south"))]
    els += feet(x0, z0, x1, z1, y0)
    add_device("pc_mini_itx_black",
               N("Gaming PC (Mini-ITX, Black)", "Gaming-PC (Mini-ITX, Schwarz)",
                 "PC Gaming (Mini-ITX, Negro)", "Speldator (Mini-ITX, svart)"),
               els, {"mesh": C.T("mesh_black"), "front": C.T("itx_front_black"),
                     "rear": C.T("itx_rear_black"), "feet": tc.T("port_black")},
               display=tc.desk_display(0.7, 1))


_itx()


# the workstation: brushed aluminium, a slotted grille and two carry handles
@tex("workstation_front")
def _ws_front():
    p = Pic(8, 15, 32, 6, bg=SILVER)
    for k in range(22):
        y = 1.0 + k * 0.6
        p.rect(0.8, y, 7.2, y + 0.3, (60, 62, 66))
    p.disc(4, 14.2, 0.3, (40, 42, 46))
    p.ring(4, 14.2, 0.32, 0.08, (120, 220, 255))
    return grain(p.done(), 2, 611)


def _workstation():
    x0, y0, z0, x1, y1, z1 = 4.0, 0.6, 1.0, 12.0, 15.0, 15.0
    els = [E((x0, y0, z0), (x1, y1, z1), "body", per={"north": "front", "south": "rear"},
             full=("north", "south"))]
    els += feet(x0, z0, x1, z1, y0)
    for z in (z0 + 0.6, z1 - 1.4):
        els += [E((x0 + 0.8, y1, z), (x0 + 1.4, y1 + 0.9, z + 0.8), "handle"),
                E((x1 - 1.4, y1, z), (x1 - 0.8, y1 + 0.9, z + 0.8), "handle"),
                E((x0 + 0.8, y1 + 0.9, z), (x1 - 0.8, y1 + 1.0, z + 0.8), "handle")]
    add_device("pc_workstation_silver",
               N("Workstation PC (Silver)", "Workstation-PC (Silber)",
                 "PC Estación de Trabajo (Plata)", "Arbetsstationsdator (silver)"),
               els, {"body": C.T("brushed_silver"), "front": C.T("workstation_front"),
                     "rear": C.T("full_rear_black"), "handle": C.T("brushed_silver"),
                     "feet": tc.T("port_black")}, display=False,
               box=[4.0, 0.0, 1.0, 12.0, 16.0, 15.0])


_workstation()


@tex("sff_front")
def _sff_front():
    p = Pic(11, 3.6, 32, 8, bg=BLACK)
    p.rect(0.5, 0.5, 6.5, 0.9, (14, 14, 16))                           # optical slot
    for k in range(4):
        p.rect(7.0 + k * 0.7, 0.6, 7.45 + k * 0.7, 0.85, (16, 40, 120))
    p.rect(0.5, 1.6, 10.5, 1.65, (60, 62, 66))
    for k in range(2):
        p.disc(1.0 + k * 0.7, 2.6, 0.18, (14, 14, 16))
    p.disc(9.8, 2.6, 0.35, (70, 72, 78))
    p.disc(9.8, 2.6, 0.12, (90, 200, 255))
    y = 2.1
    while y < 3.3:
        x = 3.0
        while x < 8.8:
            p.disc(x, y, 0.08, (12, 12, 14))
            x += 0.35
        y += 0.35
    return p.done()


def _sff():
    els = [E((2.5, 0.3, 3.0), (13.5, 3.9, 13.0), "body", per={"north": "front"},
           full=("north",))]
    els += feet(2.5, 3.0, 13.5, 13.0, 0.3)
    add_device("pc_small_form_factor_black",
               N("Small Form Factor PC (Black)", "Kompakt-PC (Schwarz)",
                 "PC de Formato Reducido (Negro)", "Kompaktdator (svart)"),
               els, {"body": tc.T("black_anodised"), "front": C.T("sff_front"),
                     "feet": tc.T("port_black")}, display=tc.desk_display(0.75, 2))


_sff()


@tex("mini_pc_front")
def _mini_front():
    p = Pic(5, 1.8, 16, 10, bg=(46, 48, 52))
    p.rect(0.5, 0.6, 0.95, 1.0, (16, 40, 120))
    p.rect(1.2, 0.6, 1.65, 1.0, (16, 40, 120))
    p.disc(4.2, 0.9, 0.22, (20, 20, 22))
    p.disc(4.2, 0.9, 0.08, (90, 200, 255))
    return p.done()


@tex("mini_pc_top")
def _mini_top():
    p = Pic(5, 5, 16, 10, bg=(46, 48, 52))
    p.ring(2.5, 2.5, 1.1, 0.25, (70, 72, 78))
    p.disc(2.5, 2.5, 0.3, (70, 72, 78))
    return p.done()


add_device("pc_mini_desktop_grey",
           N("Mini Desktop PC (Grey)", "Mini-Desktop-PC (Grau)", "Mini PC de Escritorio (Gris)",
             "Minidator (grå)"),
           [E((5.5, 0.2, 5.5), (10.5, 2.0, 10.5), "body", per={"north": "front", "up": "top"},
              full=("north", "up")),
            E((6.0, 0, 6.0), (10.0, 0.2, 10.0), "feet", SIDES)],
           {"body": tc.T("grey_abs"), "front": C.T("mini_pc_front"), "top": C.T("mini_pc_top"),
            "feet": tc.T("port_black")}, display=tc.desk_display(1.2, 2))


# the open test bench: a tray on standoffs, the board flat on it, the card upright in its slot
@tex("motherboard_top")
def _mb_top():
    p = Pic(10, 10, 64, 4, bg=(24, 26, 30))
    p.rect(0.3, 0.3, 2.0, 5.0, (60, 62, 66))                    # I/O cover
    p.rect(3.6, 1.0, 6.4, 3.8, (150, 152, 158))                  # socket
    p.rect(4.0, 1.4, 6.0, 3.4, (190, 170, 110))
    for k in range(4):
        p.rect(7.0 + k * 0.6, 0.6, 7.3 + k * 0.6, 6.0, (14, 14, 16))
    for k in range(3):
        p.rect(0.8, 6.2 + k * 1.1, 8.0, 6.45 + k * 1.1, (14, 14, 16))
    p.rect(6.5, 7.0, 9.5, 9.5, (60, 62, 66))                    # chipset heatsink
    for k in range(6):
        p.rect(2.2 + k * 0.25, 0.4, 2.35 + k * 0.25, 0.9, (190, 160, 70))
    return p.done()


anim("gpu_fans", RIG_FRAMES, lambda f: _gpu_side(f), 3)


def _gpu_side(f):
    p = Pic(9, 4, 64, 4, bg=(40, 42, 48))
    for k in range(3):
        _fan(p, 1.6 + k * 2.9, 2.0, 1.35, f, k, frame_c=(30, 30, 34))
    p.rect(0, 3.6, 9, 4, hue(f / float(RIG_FRAMES) + 0.5))
    return p.done()


anim("cooler_fan", RIG_FRAMES, lambda f: _cooler_fan(f), 3)


def _cooler_fan(f):
    p = Pic(3.4, 4.5, 32, 6, bg=(30, 30, 34))
    _fan(p, 1.7, 2.25, 1.55, f, 2)
    return p.done()


@tex("heatsink_fins")
def _fins():
    img = Image.new("RGBA", (16, 16), rgba((170, 172, 178)))
    for x in range(0, 16, 2):
        tc.rect(img, x, 0, x + 1, 16, (110, 112, 118))
    return img


def _test_bench():
    els = []
    for x in (1.4, 13.8):
        for z in (2.4, 12.8):
            els.append(E((x, 0, z), (x + 0.8, 1.6, z + 0.8), "post", SIDES))
    els.append(E((1.0, 1.6, 2.0), (15.0, 2.0, 14.0), "tray"))
    els.append(E((3.0, 2.0, 3.0), (13.0, 2.3, 13.0), "tray", per={"up": "board"},
                 uv={"up": FULL}, rot={"up": 180}))
    # the graphics card standing in its slot, fans toward the user
    els.append(E((3.4, 2.3, 4.6), (12.4, 6.3, 5.8), "gpu", per={"north": "gpufan"},
                 full=("north",)))
    els.append(E((12.4, 2.3, 4.6), (12.8, 6.5, 5.8), "bracket", SIDES + ("up",)))
    # the CPU tower cooler and its fan
    els.append(E((6.6, 2.3, 8.6), (9.4, 6.8, 10.6), "fins", per={"up": "tray"}))
    els.append(E((6.3, 2.3, 8.1), (9.7, 6.8, 8.6), "tray", per={"north": "cfan"},
                 full=("north",)))
    # RAM, lit along the top
    for k in range(4):
        x = 10.2 + k * 0.55
        els.append(E((x, 2.3, 7.6), (x + 0.3, 5.0, 12.4), "ram", SIDES))
        els.append(E((x, 5.0, 7.6), (x + 0.3, 5.3, 12.4), "rgb"))
    add_device("pc_test_bench_open",
               N("Open-Frame Test Bench PC", "Offener Testbench-PC",
                 "PC de Banco de Pruebas Abierto", "Öppen testbänksdator"),
               els, {"post": C.T("brushed_silver"), "tray": tc.T("black_anodised"),
                     "board": C.T("motherboard_top"), "gpu": tc.T("black_anodised"),
                     "gpufan": C.T("gpu_fans"), "bracket": C.T("brushed_silver"),
                     "fins": C.T("heatsink_fins"), "cfan": C.T("cooler_fan"),
                     "ram": tc.T("port_black"), "rgb": tc.T("rgb_strip")},
               display=tc.desk_display(0.7, 1))


_test_bench()


# ------------------------------------------------------------------------------------------
# Keyboard layouts
# ------------------------------------------------------------------------------------------
KEY_SHEET = 256
UNIT = 128 / 22.5               # texels a key unit (a full-size layout is 128 texels across)

_MAIN = [
    [(0, 1)] + [(1 + i, 1) for i in range(12)] + [(13, 2)],
    [(0, 1.5)] + [(1.5 + i, 1) for i in range(12)] + [(13.5, 1.5)],
    [(0, 1.75)] + [(1.75 + i, 1) for i in range(11)] + [(12.75, 2.25)],
    [(0, 2.25)] + [(2.25 + i, 1) for i in range(10)] + [(12.25, 2.75)],
    [(0, 1.25), (1.25, 1.25), (2.5, 1.25), (3.75, 6.25), (10, 1.25), (11.25, 1.25),
     (12.5, 1.25), (13.75, 1.25)],
]
_FUNCTION = [(0, 1)] + [(2 + i, 1) for i in range(4)] + [(6.5 + i, 1) for i in range(4)] + \
    [(11 + i, 1) for i in range(4)] + [(15.25 + i, 1) for i in range(3)]
_NAV = {0: [(15.25, 1), (16.25, 1), (17.25, 1)], 1: [(15.25, 1), (16.25, 1), (17.25, 1)],
        3: [(16.25, 1)], 4: [(15.25, 1), (16.25, 1), (17.25, 1)]}
_PAD = {0: [(18.5, 1), (19.5, 1), (20.5, 1), (21.5, 1)],
        1: [(18.5, 1), (19.5, 1), (20.5, 1)], 2: [(18.5, 1), (19.5, 1), (20.5, 1)],
        3: [(18.5, 1), (19.5, 1), (20.5, 1)], 4: [(18.5, 2), (20.5, 1)]}
_PAD_TALL = [(21.5, 1, 1), (21.5, 3, 1)]


def _layout_keys(kind):
    """Every key of a layout as (x, y, w, h) in key units, and the layout's size in units."""
    keys = []
    if kind in ("full", "tkl"):
        keys += [(x, 0, w, 1) for x, w in _FUNCTION]
        for r, row in enumerate(_MAIN):
            keys += [(x, 1.5 + r, w, 1) for x, w in row]
            keys += [(x, 1.5 + r, w, 1) for x, w in _NAV.get(r, [])]
            if kind == "full":
                keys += [(x, 1.5 + r, w, 1) for x, w in _PAD.get(r, [])]
        if kind == "full":
            keys += [(x, 1.5 + r, w, 2) for x, r, w in _PAD_TALL]
            return keys, (22.5, 6.5)
        return keys, (18.25, 6.5)
    if kind == "sixty":
        for r, row in enumerate(_MAIN):
            keys += [(x, r, w, 1) for x, w in row]
        return keys, (15, 5)
    if kind == "compact":
        keys += [(i, 0, 1, 0.75) for i in range(15)]
        for r, row in enumerate(_MAIN):
            keys += [(x, 1 + r, w, 1) for x, w in row]
        return keys, (15, 6)
    if kind == "numpad":
        for r in range(5):
            keys += [(x - 18.5, r, w, 1) for x, w in _PAD.get(r, [])]
        keys += [(x - 18.5, r - 1, w, 2) for x, r, w in _PAD_TALL]
        return keys, (4, 5)
    if kind in ("split_left", "split_right"):
        sixty, _ = _layout_keys("sixty")
        cut = 7.0
        if kind == "split_left":
            return [k for k in sixty if k[0] + k[2] / 2 < cut], (7.25, 5)
        right = [(x - cut, y, w, h) for x, y, w, h in sixty if x + w / 2 >= cut]
        right = [(max(0, x), y, w + min(0, x), h) for x, y, w, h in right]
        return right, (8, 5)
    raise ValueError(kind)


# keycap colourways: (cap, modifier, accent for Esc and Enter, plate)
COLOURWAYS = {
    "black": ((40, 41, 45), (40, 41, 45), (112, 114, 120), (14, 14, 16)),
    "white": ((238, 238, 234), (206, 208, 210), (120, 200, 190), (200, 200, 198)),
    "pink": ((248, 204, 220), (196, 214, 246), (255, 236, 160), (250, 232, 238)),
    "retro": ((206, 204, 196), (104, 104, 108), (236, 120, 40), (60, 60, 64)),
    "beige": ((218, 208, 186), (176, 166, 146), (176, 166, 146), (190, 180, 158)),
    "silver": ((242, 242, 244), (242, 242, 244), (242, 242, 244), (170, 172, 176)),
    "rgb": ((26, 27, 30), (26, 27, 30), (26, 27, 30), (10, 10, 12)),
}

# (band name, layout, colourway), in sheet order
BANDS = [
    ("full_black", "full", "black"), ("tkl_black", "tkl", "black"),
    ("full_white", "full", "white"), ("tkl_pink", "tkl", "pink"),
    ("full_rgb", "full", "rgb"), ("full_beige", "full", "beige"),
    ("full_silver", "full", "silver"), ("compact_white", "compact", "white"),
    ("numpad_black", "numpad", "black"),
    ("sixty_white", "sixty", "white"), ("sixty_retro", "sixty", "retro"),
    ("split_left_black", "split_left", "black"), ("split_right_black", "split_right", "black"),
    ("numpad_white", "numpad", "white"),
]


def _pack():
    """Shelf-packs the bands onto the sheet: name -> (x, y, w, h) in texels."""
    out = {}
    x = y = shelf = 0
    for name, kind, _ in BANDS:
        _, (wu, hu) = _layout_keys(kind)
        w, h = int(math.ceil(wu * UNIT)), int(math.ceil(hu * UNIT))
        if x + w > KEY_SHEET:
            x, y, shelf = 0, y + shelf + 1, 0
        out[name] = (x, y, w, h)
        x += w + 1
        shelf = max(shelf, h)
    assert y + shelf <= KEY_SHEET
    return out


BAND_RECTS = _pack()


def _draw_band(img, rect_, kind, way):
    x0, y0, w, h = rect_
    cap, mod, accent, plate = COLOURWAYS[way]
    keys, (wu, hu) = _layout_keys(kind)
    tc.rect(img, x0, y0, x0 + w, y0 + h, plate)
    for i, (x, y, kw, kh) in enumerate(keys):
        a = x0 + int(round(x * UNIT)) + 1
        b = y0 + int(round(y * UNIT)) + 1
        c = x0 + int(round((x + kw) * UNIT))
        d = y0 + int(round((y + kh) * UNIT))
        colour = cap if (kw == 1 and kh <= 1) or kw >= 6 else mod
        if i == 0 or (kw >= 2 and y == (1.5 + 2 if kind in ("full", "tkl") else 2)) or \
                (kw in (2.25,) and kind not in ("full", "tkl")):
            colour = accent
        if way == "silver":
            tc.rect(img, a, b, c, d, (250, 250, 252))
            tc.rect(img, a, d - 1, c, d, (200, 200, 204))
            continue
        tc.rect(img, a, b, c, d, tc.shade(colour, 0.78))
        tc.rect(img, a, b, c, d - 1, colour)
        tc.rect(img, a + 1, b, c - 1, b + 1, tc.shade(colour, 1.25))
        if way == "rgb":
            # the legend lit through the cap, the colour running across the board
            lc = hue((x + kw / 2.0) / wu * 0.85, 0.9, 1.0)
            mx, my = (a + c) // 2, (b + d) // 2 - 1
            tc.rect(img, mx - 1, my, mx + 1, my + 1, lc)
        elif way in ("white", "pink", "beige", "retro") and kw == 1:
            mx, my = (a + c) // 2, (b + d) // 2 - 1
            tc.rect(img, mx - 1, my, mx, my + 1, tc.shade(colour, 0.55))


@tex("keyboard_layouts")
def tex_keyboard_layouts():
    img = tc.fill(tc.BLACK_ANODISED, KEY_SHEET, 2, 501)
    for name, kind, way in BANDS:
        _draw_band(img, BAND_RECTS[name], kind, way)
    return img


def band_uv(name):
    x, y, w, h = BAND_RECTS[name]
    k = 16.0 / KEY_SHEET
    return [round(x * k, 4), round(y * k, 4), round((x + w) * k, 4), round((y + h) * k, 4)]


def band_depth(name, width):
    """The depth of a key bed ``width`` across that shows band ``name`` at its own aspect."""
    _, _, w, h = BAND_RECTS[name]
    return width * h / float(w)


def key_bed(x0, z0, width, y0, y1, band):
    """A key bed width across from (x0, z0) toward the far side, showing its band."""
    d = band_depth(band, width)
    return [E((x0, y0, z0), (x0 + width, y1, z0 + d), "keys", ("up",),
              uv={"up": band_uv(band)}, rot={"up": 180}),
            E((x0, y0, z0), (x0 + width, y1, z0 + d), "bed", ("north", "south", "east", "west"))], d


KB_TEX = {"keys": C.T("keyboard_layouts"), "bed": tc.T("port_black")}


def keyboard(reg, names, band, width, case_t, case_h=1.35, bed_h=0.55, rim=0.3, extra=(),
             scale=0.85, x0=None, z0=None, lift=0.0, textures=None):
    """A keyboard: a case rim around a key bed showing its layout band, centred in the block
    toward the user."""
    d = band_depth(band, width)
    W, D = width + 2 * rim, d + 2 * rim
    x0 = (16 - W) / 2.0 if x0 is None else x0
    z0 = (16 - D) / 2.0 if z0 is None else z0
    top = lift + case_h
    els = [E((x0, lift, z0), (x0 + W, top - bed_h, z0 + D), "case")]
    bed, _ = key_bed(x0 + rim, z0 + rim, width, top - bed_h, top, band)
    els += bed
    els += list(extra)
    t = dict(KB_TEX)
    t["case"] = case_t
    t.update(textures or {})
    add_device(reg, names, els, t, display=tc.desk_display(scale, 2))


keyboard("keyboard_mech_full_black",
         N("Mechanical Keyboard (Full-Size, Black)", "Mechanische Tastatur (Vollformat, Schwarz)",
           "Teclado Mecánico (Tamaño Completo, Negro)", "Mekaniskt tangentbord (fullstorlek, svart)"),
         "full_black", 14.2, tc.T("black_anodised"))
keyboard("keyboard_mech_full_white",
         N("Mechanical Keyboard (Full-Size, White)", "Mechanische Tastatur (Vollformat, Weiß)",
           "Teclado Mecánico (Tamaño Completo, Blanco)", "Mekaniskt tangentbord (fullstorlek, vit)"),
         "full_white", 14.2, tc.T("white_plastic"))
keyboard("keyboard_mech_tkl_black",
         N("Mechanical Keyboard (Tenkeyless, Black)", "Mechanische Tastatur (TKL, Schwarz)",
           "Teclado Mecánico (TKL, Negro)", "Mekaniskt tangentbord (TKL, svart)"),
         "tkl_black", 11.6, tc.T("black_anodised"))
keyboard("keyboard_mech_tkl_pink",
         N("Mechanical Keyboard (Tenkeyless, Pastel Pink)",
           "Mechanische Tastatur (TKL, Pastellrosa)", "Teclado Mecánico (TKL, Rosa Pastel)",
           "Mekaniskt tangentbord (TKL, pastellrosa)"),
         "tkl_pink", 11.6, C.T("pastel_pink"))
keyboard("keyboard_mech_60_white",
         N("Mechanical Keyboard (60%, White)", "Mechanische Tastatur (60 %, Weiß)",
           "Teclado Mecánico (60 %, Blanco)", "Mekaniskt tangentbord (60 %, vitt)"),
         "sixty_white", 9.6, tc.T("white_plastic"))
keyboard("keyboard_mech_60_retro",
         N("Mechanical Keyboard (60%, Retro Grey and Orange)",
           "Mechanische Tastatur (60 %, Retro Grau und Orange)",
           "Teclado Mecánico (60 %, Gris Retro y Naranja)",
           "Mekaniskt tangentbord (60 %, retrogrått och orange)"),
         "sixty_retro", 9.6, C.T("retro_grey"))

# RGB: the case stands on a lit underglow strip
_rgbw = 14.2
_rgbd = band_depth("full_rgb", _rgbw) + 0.6
_rx0, _rz0 = (16 - _rgbw - 0.6) / 2.0, (16 - _rgbd) / 2.0
keyboard("keyboard_mech_rgb_black",
         N("RGB Mechanical Keyboard (Black)", "RGB-Mechanische Tastatur (Schwarz)",
           "Teclado Mecánico RGB (Negro)", "Mekaniskt RGB-tangentbord (svart)"),
         "full_rgb", _rgbw, tc.T("black_anodised"), lift=0.35,
         extra=[E((_rx0 + 0.3, 0, _rz0 + 0.3), (_rx0 + _rgbw + 0.3, 0.35, _rz0 + _rgbd - 0.3),
                  "glow", SIDES, shade=False)],
         textures={"glow": tc.T("rgb_strip")})

# low-profile: a thin aluminium slab and flat chiclet caps
keyboard("keyboard_slim_silver",
         N("Low-Profile Keyboard (Silver)", "Flache Tastatur (Silber)",
           "Teclado de Perfil Bajo (Plata)", "Lågprofiltangentbord (silver)"),
         "full_silver", 14.0, C.T("brushed_silver"), case_h=0.7, bed_h=0.2, rim=0.35)

# retro beige: a deep case with a raised back strip and its three lock lights
_bw = 14.0
_bd = band_depth("full_beige", _bw) + 0.8
_bx0, _bz0 = (16 - _bw - 0.8) / 2.0, (16 - _bd - 1.6) / 2.0


@tex("retro_kb_strip")
def _retro_strip():
    p = Pic(14.8, 1.6, 32, 8, bg=BEIGE)
    for k in range(3):
        p.disc(11.0 + k * 1.0, 0.8, 0.18, (70, 200, 90) if k == 1 else (60, 70, 60))
    p.rect(0.8, 0.55, 4.0, 1.05, (120, 112, 98))
    return p.done()


keyboard("keyboard_retro_beige",
         N("Retro Keyboard (Beige)", "Retro-Tastatur (Beige)", "Teclado Retro (Beige)",
           "Retrotangentbord (beige)"),
         "full_beige", _bw, C.T("beige_plastic"), case_h=1.9, bed_h=0.6, rim=0.4,
         x0=_bx0, z0=_bz0,
         extra=[E((_bx0, 0, _bz0 + _bd), (_bx0 + _bw + 0.8, 2.1, _bz0 + _bd + 1.6), "case",
                  per={"up": "strip"}, uv={"up": FULL}, rot={"up": 180})],
         textures={"strip": C.T("retro_kb_strip")})

# compact wireless: thin white, a battery bar along the back
_cw = 10.2
_cd = band_depth("compact_white", _cw) + 0.5
_cx0, _cz0 = (16 - _cw - 0.5) / 2.0, (16 - _cd - 0.9) / 2.0
keyboard("keyboard_wireless_compact_white",
         N("Compact Wireless Keyboard (White)", "Kompakte Funktastatur (Weiß)",
           "Teclado Inalámbrico Compacto (Blanco)", "Kompakt trådlöst tangentbord (vitt)"),
         "compact_white", _cw, tc.T("white_plastic"), case_h=0.6, bed_h=0.2, rim=0.25,
         x0=_cx0, z0=_cz0,
         extra=[E((_cx0, 0, _cz0 + _cd), (_cx0 + _cw + 0.5, 0.95, _cz0 + _cd + 0.9), "case")])


# split ergonomic: two halves apart, each with its palm rest
def _split():
    w_l, w_r = 6.6, 7.3
    d = band_depth("split_left_black", w_l)
    rim = 0.3
    z0 = 4.5
    els = []
    for x0, w, band in ((15.0 - w_l - 2 * rim, w_l, "split_left_black"),
                        (1.0, w_r, "split_right_black")):
        # the user's left half is at high x (the user faces south)
        els.append(E((x0, 0, z0), (x0 + w + 2 * rim, 0.8, z0 + d + 2 * rim), "case"))
        bed, _ = key_bed(x0 + rim, z0 + rim, w, 0.8, 1.35, band)
        els += bed
        els.append(E((x0 + 0.6, 0, z0 - 2.4), (x0 + w + 2 * rim - 0.6, 0.9, z0), "rest"))
    add_device("keyboard_split_ergonomic_black",
               N("Split Ergonomic Keyboard (Black)", "Geteilte ergonomische Tastatur (Schwarz)",
                 "Teclado Ergonómico Dividido (Negro)", "Delat ergonomiskt tangentbord (svart)"),
               els, dict(KB_TEX, case=tc.T("black_anodised"), rest=C.T("cloth_black")),
               display=tc.desk_display(0.85, 2))


_split()

keyboard("numpad_black",
         N("Numpad (Black)", "Ziffernblock (Schwarz)", "Teclado Numérico (Negro)",
           "Numeriskt tangentbord (svart)"),
         "numpad_black", 3.4, tc.T("black_anodised"), scale=1.4)
keyboard("numpad_white",
         N("Numpad (White)", "Ziffernblock (Weiß)", "Teclado Numérico (Blanco)",
           "Numeriskt tangentbord (vitt)"),
         "numpad_white", 3.4, tc.T("white_plastic"), scale=1.4)


# ------------------------------------------------------------------------------------------
# Mice and pads
# ------------------------------------------------------------------------------------------
def _mouse_gaming():
    x0, z0 = 6.7, 5.8
    els = [E((x0, 0, z0), (x0 + 2.6, 0.7, z0 + 4.4), "body"),
           E((x0 + 0.2, 0.7, z0 + 0.3), (x0 + 2.4, 1.2, z0 + 4.2), "body"),
           E((x0 + 0.5, 1.2, z0 + 0.8), (x0 + 2.1, 1.45, z0 + 3.6), "body"),
           E((x0 + 1.15, 1.2, z0 + 3.2), (x0 + 1.45, 1.55, z0 + 4.0), "wheel"),
           E((x0 + 1.25, 1.45, z0 + 1.2), (x0 + 1.35, 1.5, z0 + 2.4), "rgb", shade=False),
           E((x0 - 0.1, 0.5, z0 + 2.4), (x0, 0.9, z0 + 3.4), "wheel", SIDES + ("up",)),
           E((x0 + 0.2, 0.2, z0 - 0.05), (x0 + 2.4, 0.5, z0), "rgb", ("north",), shade=False)]
    add_device("mouse_gaming_black",
               N("Gaming Mouse (Black)", "Gaming-Maus (Schwarz)", "Ratón Gaming (Negro)",
                 "Spelmus (svart)"),
               els, {"body": tc.T("black_anodised"), "wheel": tc.T("grey_abs"),
                     "rgb": tc.T("rgb_strip")}, display=tc.desk_display(2.0, 2))


_mouse_gaming()


def _mouse_vertical():
    x0, z0 = 6.8, 5.6
    els = [E((x0, 0, z0), (x0 + 2.8, 0.5, z0 + 4.6), "base"),
           E((x0 + 0.5, 0.5, z0 + 0.3), (x0 + 2.0, 2.6, z0 + 4.2), "body"),
           E((x0 + 0.8, 2.6, z0 + 0.8), (x0 + 1.8, 3.0, z0 + 3.8), "body"),
           E((x0 + 2.0, 1.2, z0 + 2.6), (x0 + 2.15, 2.4, z0 + 3.9), "button", SIDES + ("up",)),
           E((x0 + 2.0, 1.2, z0 + 1.5), (x0 + 2.25, 1.5, z0 + 2.2), "button")]
    add_device("mouse_vertical_ergonomic_grey",
               N("Vertical Ergonomic Mouse (Grey)", "Vertikale ergonomische Maus (Grau)",
                 "Ratón Ergonómico Vertical (Gris)", "Vertikal ergonomisk mus (grå)"),
               els, {"base": tc.T("black_anodised"), "body": tc.T("grey_abs"),
                     "button": tc.T("black_anodised")}, display=tc.desk_display(2.0, 2))


_mouse_vertical()


def _trackball():
    els = [E((5.8, 0, 5.4), (10.2, 0.9, 10.6), "body"),
           E((6.2, 0.9, 5.8), (9.8, 1.3, 10.2), "body")]
    els += post(8.4, 8.6, 1.3, 1.0, 3.2, "ball")
    els += [E((6.0, 0.9, 5.6), (7.4, 1.6, 6.8), "button"),
            E((9.4, 0.9, 5.6), (10.0, 1.6, 7.6), "button")]
    add_device("mouse_trackball_black",
               N("Trackball Mouse (Black)", "Trackball-Maus (Schwarz)", "Ratón Trackball (Negro)",
                 "Styrkulemus (svart)"),
               els, {"body": tc.T("black_anodised"), "ball": C.T("trackball_red"),
                     "button": tc.T("grey_abs")}, display=tc.desk_display(1.8, 2))


@tex("trackball_red")
def _ball():
    img = tc.fill((200, 30, 40), 16, 6, 621)
    for x, y in ((3, 4), (10, 2), (6, 11), (13, 9), (1, 14), (8, 7)):
        tc.rect(img, x, y, x + 1, y + 1, (120, 10, 20))
    return img


_trackball()

add_device("mouse_travel_white",
           N("Travel Mouse (White)", "Reisemaus (Weiß)", "Ratón de Viaje (Blanco)",
             "Resemus (vit)"),
           [E((7.0, 0, 6.3), (9.0, 0.6, 9.7), "body"),
            E((7.2, 0.6, 6.6), (8.8, 0.95, 9.4), "body"),
            E((7.9, 0.95, 8.6), (8.1, 1.05, 9.2), "wheel")],
           {"body": tc.T("white_plastic"), "wheel": tc.T("grey_abs")},
           display=tc.desk_display(2.2, 2))


@tex("mouse_pad_black")
def _mousepad():
    p = Pic(7, 6, 32, 6, bg=(26, 27, 30))
    p.frame(0, 0, 7, 6, (60, 62, 66), 0.2)
    p.rect(5.4, 4.6, 6.4, 5.4, (40, 42, 46))
    return grain(p.done(), 3, 631)


add_device("mouse_pad_black",
           N("Mouse Pad (Black)", "Mauspad (Schwarz)", "Alfombrilla de Ratón (Negra)",
             "Musmatta (svart)"),
           [E((4.5, 0, 5.0), (11.5, 0.15, 11.0), "pad", per={"up": "top"}, uv={"up": FULL},
              rot={"up": 180})],
           {"pad": C.T("cloth_black"), "top": C.T("mouse_pad_black")},
           display=tc.desk_display(1.1, 3))


@tex("desk_mat_xxl")
def _deskmat():
    p = Pic(16, 9, 64, 4, bg=(24, 26, 32))
    # contour lines, as a topographic map
    for k in range(9):
        p.ring(4.0, 6.5, 1.0 + k * 1.1, 0.06, (44, 50, 64))
        p.ring(13.5, 1.5, 0.6 + k * 0.9, 0.06, (40, 46, 60))
    p.frame(0, 0, 16, 9, (70, 72, 78), 0.15)
    return grain(p.done(), 2, 641)


add_device("desk_mat_xxl_black",
           N("XXL Desk Mat (Black)", "XXL-Schreibtischunterlage (Schwarz)",
             "Alfombrilla de Escritorio XXL (Negra)", "XXL-skrivbordsmatta (svart)"),
           [E((0.0, 0, 3.5), (16.0, 0.15, 12.5), "pad", per={"up": "top"}, uv={"up": FULL},
              rot={"up": 180})],
           {"pad": C.T("cloth_black"), "top": C.T("desk_mat_xxl")},
           display=tc.desk_display(0.85, 3))
add_device("desk_mat_xxl_rgb",
           N("XXL Desk Mat (RGB Edge)", "XXL-Schreibtischunterlage (RGB-Rand)",
             "Alfombrilla de Escritorio XXL (Borde RGB)", "XXL-skrivbordsmatta (RGB-kant)"),
           [E((0.3, 0, 3.8), (15.7, 0.15, 12.2), "pad", per={"up": "top"}, uv={"up": FULL},
              rot={"up": 180}),
            E((0.0, 0, 3.5), (16.0, 0.22, 3.8), "rgb", shade=False),
            E((0.0, 0, 12.2), (16.0, 0.22, 12.5), "rgb", shade=False),
            E((0.0, 0, 3.8), (0.3, 0.22, 12.2), "rgb", shade=False),
            E((15.7, 0, 3.8), (16.0, 0.22, 12.2), "rgb", shade=False)],
           {"pad": C.T("cloth_black"), "top": C.T("desk_mat_xxl"), "rgb": tc.T("rgb_strip")},
           display=tc.desk_display(0.85, 3))


# ------------------------------------------------------------------------------------------
# Monitors
# ------------------------------------------------------------------------------------------
def panel(x0, x1, y0, y1, zf, depth, bezel, chin, skey="screen", uvwin=FULL, turn=None):
    """A flat panel: its housing and the screen a hair proud of its front, inset by the
    bezel at the sides and top and the chin at the bottom."""
    return [E((x0, y0, zf), (x1, y1, zf + depth), "case", turn=turn),
            E((x0 + bezel, y0 + chin, zf - 0.08), (x1 - bezel, y1 - bezel, zf), skey, ("north",),
              uv={"north": uvwin}, turn=turn, shade=False)]


def stand_office(cx, zf, top_y, depth=0.6):
    """A round-cornered base and a neck behind the panel."""
    return [E((cx - 3.0, 0, zf - 0.5), (cx + 3.0, 0.4, zf + 4.0), "stand"),
            E((cx - 0.7, 0.4, zf + depth + 0.8), (cx + 0.7, top_y, zf + depth + 1.6), "stand"),
            E((cx - 1.8, top_y - 2.6, zf + depth), (cx + 1.8, top_y, zf + depth + 0.8), "stand")]


MON_TEX = {"case": tc.T("black_anodised"), "stand": tc.T("black_anodised")}


def _monitor_24(reg, names, webcam=False):
    zf = 8.6
    x0, x1, y0 = 1.75, 14.25, 3.0
    inner = (x1 - x0) - 0.7
    y1 = y0 + 0.6 + inner * 9 / 16.0 + 0.35
    els = panel(x0, x1, y0, y1, zf, 0.6, 0.35, 0.6)
    els += [E((x0 + 3.5, y0 + 0.8, zf + 0.6), (x1 - 3.5, y1 - 0.8, zf + 1.2), "case")]
    els += stand_office(8.0, zf, y0 + 3.5, 1.2)
    t = dict(MON_TEX)
    if webcam:
        els += [E((6.0, y1, zf - 0.5), (10.0, y1 + 1.1, zf + 0.7), "cam", per={"north": "lens"},
                  full=("north",)),
                E((7.2, y1 - 1.2, zf + 0.6), (8.8, y1, zf + 0.9), "cam")]
        t.update(cam=tc.T("black_anodised"), lens=C.T("webcam_front"))
    add_device(reg, names, els, t, screens={"screen": screen_set("w")},
               display=tc.desk_display(0.8, 0))


@tex("webcam_front")
def _webcam_front():
    p = Pic(4, 1.1, 16, 12, bg=BLACK)
    p.disc(2.0, 0.55, 0.42, (60, 62, 70))
    p.disc(2.0, 0.55, 0.3, (10, 12, 18))
    p.disc(1.9, 0.48, 0.08, (120, 140, 200))
    p.disc(3.3, 0.55, 0.08, (60, 220, 90))
    return p.done()


_monitor_24("monitor_24_office_black",
            N("24\" Office Monitor (Black)", "24-Zoll-Bürobildschirm (Schwarz)",
              "Monitor de Oficina de 24\" (Negro)", "24-tums kontorsskärm (svart)"))
_monitor_24("monitor_24_office_webcam_black",
            N("24\" Office Monitor with Webcam (Black)",
              "24-Zoll-Bürobildschirm mit Webcam (Schwarz)",
              "Monitor de Oficina de 24\" con Cámara Web (Negro)",
              "24-tums kontorsskärm med webbkamera (svart)"), webcam=True)


def _monitor_27(reg, names, light):
    zf = 8.4
    x0, x1, y0 = 1.0, 15.0, 3.4
    inner = (x1 - x0) - 0.4
    y1 = y0 + 0.45 + inner * 9 / 16.0 + 0.2
    els = panel(x0, x1, y0, y1, zf, 0.5, 0.2, 0.45)
    hx0, hx1, hy0, hy1, hz0, hz1 = 3.0, 13.0, y0 + 1.0, y1 - 1.0, zf + 0.5, zf + 1.6
    els.append(E((hx0, hy0, hz0), (hx1, hy1, hz1), "case"))
    # the RGB ring on the back
    els += [E((hx0 + 1.2, hy0 + 1.0, hz1), (hx1 - 1.2, hy0 + 1.4, hz1 + 0.1), "rgb", shade=False),
            E((hx0 + 1.2, hy1 - 1.4, hz1), (hx1 - 1.2, hy1 - 1.0, hz1 + 0.1), "rgb", shade=False),
            E((hx0 + 1.2, hy0 + 1.4, hz1), (hx0 + 1.6, hy1 - 1.4, hz1 + 0.1), "rgb", shade=False),
            E((hx1 - 1.6, hy0 + 1.4, hz1), (hx1 - 1.2, hy1 - 1.4, hz1 + 0.1), "rgb", shade=False)]
    # a neck and splayed legs
    els += [E((7.2, 0.5, hz1 - 0.2), (8.8, y0 + 4.0, hz1 + 0.8), "stand"),
            E((7.4, 0, 6.0), (8.6, 0.5, 13.0), "stand", turn=("y", 22.5, (8.0, 0, 12.0))),
            E((7.4, 0, 6.0), (8.6, 0.5, 13.0), "stand", turn=("y", -22.5, (8.0, 0, 12.0)))]
    t = {"case": tc.T("white_plastic" if light else "black_anodised"),
         "stand": tc.T("white_plastic" if light else "black_anodised"), "rgb": tc.T("rgb_strip")}
    add_device(reg, names, els, t, screens={"screen": screen_set("w")},
               display=tc.desk_display(0.75, 0), box=[1.0, 0.0, 5.0, 15.0, round(y1 + 0.24, 2), 13.0])


_monitor_27("monitor_27_gaming_black",
            N("27\" Gaming Monitor (Black)", "27-Zoll-Gaming-Monitor (Schwarz)",
              "Monitor Gaming de 27\" (Negro)", "27-tums spelskärm (svart)"), False)
_monitor_27("monitor_27_gaming_white",
            N("27\" Gaming Monitor (White)", "27-Zoll-Gaming-Monitor (Weiß)",
              "Monitor Gaming de 27\" (Blanco)", "27-tums spelskärm (vit)"), True)


def _ultrawide():
    zf = 8.6
    y0, depth, bz = 3.2, 0.6, 0.25
    wing = 5.0
    cx0, cx1 = 2.2, 13.8
    inner_h = 9.1
    y1 = y0 + 0.45 + inner_h + bz
    # screen widths: the outer wings lose their end bezel
    sw = [wing - bz, cx1 - cx0, wing - bz]
    tot = float(sum(sw))
    u1 = 16 * sw[0] / tot
    u2 = 16 * (sw[0] + sw[1]) / tot
    els = []
    # centre
    els += panel(cx0, cx1, y0, y1, zf, depth, 0.0, 0.45, uvwin=[u1, 0, u2, 16])
    els[-1]["from"][1] = y0 + 0.45
    els[-1]["to"][1] = y1 - bz
    # wings: the one at +x (the user's left) and the one at -x, turned toward the user
    for sx0, sx1, scr, uvw, ang, ox in ((cx1, cx1 + wing, (cx1, cx1 + wing - bz), [0, 0, u1, 16],
                                         22.5, cx1),
                                        (cx0 - wing, cx0, (cx0 - wing + bz, cx0), [u2, 0, 16, 16],
                                         -22.5, cx0)):
        turn = ("y", ang, (ox, y0, zf))
        els.append(E((sx0, y0, zf), (sx1, y1, zf + depth), "case", turn=turn))
        els.append(E((scr[0], y0 + 0.45, zf - 0.08), (scr[1], y1 - bz, zf), "screen", ("north",),
                     uv={"north": uvw}, turn=turn, shade=False))
    els += [E((4.0, y0 + 1.0, zf + depth), (12.0, y1 - 1.2, zf + depth + 1.0), "case")]
    els += [E((3.5, 0, zf + 0.6), (12.5, 0.4, zf + 5.0), "stand"),
            E((7.2, 0.4, zf + depth + 1.0), (8.8, y0 + 4.0, zf + depth + 1.8), "stand")]
    add_device("monitor_34_ultrawide_curved_black",
               N("34\" Curved Ultrawide Monitor (Black)",
                 "34-Zoll-Curved-Ultrawide-Monitor (Schwarz)",
                 "Monitor Ultrapanorámico Curvo de 34\" (Negro)",
                 "34-tums böjd ultrabred skärm (svart)"),
               els, MON_TEX, screens={"screen": screen_set("uw")},
               display=tc.desk_display(0.55, 0))


_ultrawide()


def _dual_arm():
    zf = 7.6
    bezel, chin = 0.35, 0.5
    w = 12.0
    inner = w - 2 * bezel
    y0 = 5.2
    y1 = y0 + chin + inner * 9 / 16.0 + bezel
    els = []
    # the user's left screen is at +x
    els += panel(8.25, 8.25 + w, y0, y1, zf, 0.6, bezel, chin, skey="screen")
    els += panel(7.75 - w, 7.75, y0, y1, zf, 0.6, bezel, chin, skey="screen2")
    mid = (y0 + y1) / 2.0
    els += [
        # VESA plates on each back
        E((11.0, mid - 1.5, zf + 0.6), (15.0, mid + 1.5, zf + 1.0), "arm"),
        E((1.0, mid - 1.5, zf + 0.6), (5.0, mid + 1.5, zf + 1.0), "arm"),
        # the cross bar and its links to the plates
        E((2.0, mid - 0.4, zf + 1.6), (14.0, mid + 0.4, zf + 2.4), "arm"),
        E((12.6, mid - 0.4, zf + 1.0), (13.4, mid + 0.4, zf + 1.6), "arm"),
        E((2.6, mid - 0.4, zf + 1.0), (3.4, mid + 0.4, zf + 1.6), "arm"),
        # the arm back to the pole, the pole and its desk clamp
        E((7.5, mid - 0.4, zf + 2.4), (8.5, mid + 0.4, 14.0), "arm"),
        E((7.4, 0, 13.6), (8.6, mid + 1.0, 14.8), "arm"),
        E((6.6, 0, 12.8), (9.4, 1.2, 15.6), "arm"),
    ]
    add_device("monitor_dual_arm_black",
               N("Dual 24\" Monitors on a Desk Stand (Black)", "Zwei 24-Zoll-Monitore am Arm (Schwarz)",
                 "Dos Monitores de 24\" en un Brazo (Negro)",
                 "Två 24-tumsskärmar på arm (svart)"),
               els, {"case": tc.T("black_anodised"), "arm": C.T("brushed_silver")},
               screens={"screen": screen_set("w"),
                        "screen2": [screen_set("w")[(i + 2) % 4] for i in range(4)]},
               display=tc.desk_display(0.45, 0))


_dual_arm()


def _portrait():
    zf = 8.6
    bezel, chin = 0.38, 0.5
    inner_w = 6.64
    x0 = 8.0 - inner_w / 2.0 - bezel
    x1 = 8.0 + inner_w / 2.0 + bezel
    y0 = 2.6
    y1 = y0 + chin + inner_w * 16 / 9.0 + bezel
    els = panel(x0, x1, y0, y1, zf, 0.6, bezel, chin)
    els += [E((x0 + 1.4, y0 + 2.0, zf + 0.6), (x1 - 1.4, y1 - 2.0, zf + 1.2), "case")]
    els += stand_office(8.0, zf, y0 + 6.5, 1.2)
    add_device("monitor_portrait_black",
               N("Portrait Monitor (Black)", "Hochkant-Monitor (Schwarz)",
                 "Monitor Vertical (Negro)", "Stående skärm (svart)"),
               els, MON_TEX, screens={"screen": screen_set("tall")},
               display=tc.desk_display(0.75, 0))


_portrait()


# ------------------------------------------------------------------------------------------
# The gaming laptop
# ------------------------------------------------------------------------------------------
@tex("laptop_gaming_lid")
def _lid():
    p = Pic(14, 9.5, 64, 4, bg=(30, 31, 35))
    # an invented chevron mark and two vent lines
    p.poly([(7.0, 3.2), (8.4, 4.6), (7.0, 6.0), (7.0, 5.2), (7.6, 4.6), (7.0, 4.0)],
           (220, 40, 60))
    p.poly([(7.0, 3.2), (5.6, 4.6), (7.0, 6.0), (7.0, 5.2), (6.4, 4.6), (7.0, 4.0)],
           (180, 30, 50))
    p.rect(1.0, 8.4, 13.0, 8.55, (50, 52, 58))
    p.rect(1.0, 0.95, 13.0, 1.1, (50, 52, 58))
    return grain(p.done(), 2, 651)


def _laptop_open():
    z0, z1 = 3.5, 12.5
    base_h = 0.8
    els = [E((1.0, 0, z0), (15.0, base_h, z1), "case")]
    width = 12.4
    bed, d = key_bed(1.8, z1 - 0.5 - band_depth("full_rgb", width), width, base_h, base_h + 0.08,
                     "full_rgb")
    els += bed
    els += [E((5.5, base_h, z0 + 0.6), (10.5, base_h + 0.04, z0 + 3.4), "pad", ("up",)),
            E((1.4, 0.2, z0 - 0.05), (14.6, 0.5, z0), "rgb", ("north",), shade=False)]
    # the lid, leant back from its hinge
    turn = ("x", 22.5, (8.0, base_h, z1))
    els += panel(1.0, 15.0, base_h, base_h + 9.0, z1 - 0.45, 0.45, 0.35, 0.7, turn=turn)
    els[-2]["faces"]["south"]["texture"] = "#lid"
    els[-2]["faces"]["south"]["uv"] = list(FULL)
    add_device("laptop_gaming_open_black",
               N("Gaming Laptop (Open, Black)", "Gaming-Laptop (Offen, Schwarz)",
                 "Portátil Gaming (Abierto, Negro)", "Spelbärbar dator (öppen, svart)"),
               els, dict(KB_TEX, case=tc.T("black_anodised"), pad=tc.T("port_black"),
                         rgb=tc.T("rgb_strip"), lid=C.T("laptop_gaming_lid")),
               screens={"screen": screen_set("w")}, display=tc.desk_display(0.8, 1))


_laptop_open()

add_device("laptop_gaming_closed_black",
           N("Gaming Laptop (Closed, Black)", "Gaming-Laptop (Geschlossen, Schwarz)",
             "Portátil Gaming (Cerrado, Negro)", "Spelbärbar dator (stängd, svart)"),
           [E((1.0, 0, 3.0), (15.0, 1.3, 12.5), "case", per={"up": "lid"}, uv={"up": FULL},
              rot={"up": 180}),
            E((2.0, 0.3, 12.5), (14.0, 0.8, 12.6), "rgb", ("south",), shade=False)],
           {"case": tc.T("black_anodised"), "lid": C.T("laptop_gaming_lid"),
            "rgb": tc.T("rgb_strip")}, display=tc.desk_display(0.85, 2))


# ------------------------------------------------------------------------------------------
# Desk extras
# ------------------------------------------------------------------------------------------
def _headset():
    els = post(8.0, 8.0, 2.4, 0, 0.5, "stand")
    els += post(8.0, 8.0, 0.45, 0.5, 9.4, "stand")
    els += [E((5.6, 9.4, 7.4), (10.4, 10.0, 8.6), "stand")]
    # the band over the hanger, its two sides and the ear cups
    els += [E((5.2, 10.0, 7.0), (10.8, 10.7, 9.0), "band"),
            E((4.2, 7.6, 7.0), (5.0, 10.4, 9.0), "band"),
            E((11.0, 7.6, 7.0), (11.8, 10.4, 9.0), "band"),
            E((3.4, 4.4, 6.2), (5.0, 8.0, 9.8), "cup", per={"west": "glow"}, full=("west",)),
            E((11.0, 4.4, 6.2), (12.6, 8.0, 9.8), "cup", per={"east": "glow"}, full=("east",)),
            E((5.0, 4.8, 6.6), (5.4, 7.6, 9.4), "pad"),
            E((10.6, 4.8, 6.6), (11.0, 7.6, 9.4), "pad"),
            E((3.0, 5.2, 7.7), (3.4, 5.6, 9.6), "pad", SIDES + ("up",))]
    add_device("headset_on_stand_black",
               N("Gaming Headset on a Stand (Black)", "Gaming-Headset auf Ständer (Schwarz)",
                 "Auriculares Gaming en su Soporte (Negro)",
                 "Spelheadset på ställ (svart)"),
               els, {"stand": tc.T("black_anodised"), "band": tc.T("black_anodised"),
                     "cup": tc.T("black_anodised"), "glow": C.T("headset_cup"),
                     "pad": C.T("cloth_black")}, display=tc.desk_display(0.9, 0))


anim("headset_cup", RIG_FRAMES, lambda f: _cup(f), 3)


def _cup(f):
    p = Pic(3.6, 3.6, 16, 8, bg=BLACK)
    p.ring(1.8, 1.8, 1.4, 0.35, hue(f / float(RIG_FRAMES)))
    p.disc(1.8, 1.8, 0.6, (50, 52, 58))
    return p.done()


_headset()


def _webcam_tripod():
    els = [E((6.0, 4.4, 7.4), (10.0, 5.5, 8.6), "cam", per={"north": "lens"}, full=("north",)),
           E((7.6, 3.6, 7.6), (8.4, 4.4, 8.4), "cam")]
    els += post(8.0, 8.0, 0.25, 1.2, 3.6, "leg")
    els.append(E((7.8, 0, 8.0), (8.2, 0.3, 10.6), "leg"))
    for ang in (45, -45):
        els.append(E((7.8, 0, 5.4), (8.2, 0.3, 8.0), "leg", turn=("y", ang, (8.0, 0, 8.0))))
    els.append(E((7.8, 0.3, 7.8), (8.2, 1.2, 8.2), "leg"))
    add_device("webcam_tripod_black",
               N("Webcam on a Tripod (Black)", "Webcam auf Stativ (Schwarz)",
                 "Cámara Web en Trípode (Negra)", "Webbkamera på stativ (svart)"),
               els, {"cam": tc.T("black_anodised"), "lens": C.T("webcam_front"),
                     "leg": tc.T("port_black")}, display=tc.desk_display(1.4, 0))


_webcam_tripod()


@tex("mic_grille")
def _mic_grille():
    img = Image.new("RGBA", (16, 16), rgba((60, 62, 66)))
    for y in range(16):
        for x in range(16):
            if (x + y) % 3 == 0 or (x - y) % 3 == 0:
                img.putpixel((x, y), rgba((24, 25, 28)))
    return img


def _mic():
    els = [E((12.4, 0, 12.4), (15.6, 1.4, 15.6), "arm"),
           E((13.6, 1.4, 13.6), (14.4, 10.0, 14.4), "arm"),
           E((13.6, 10.0, 7.4), (14.4, 10.8, 14.4), "arm"),
           E((8.0, 10.0, 7.4), (14.4, 10.8, 8.2), "arm"),
           E((8.2, 9.2, 7.6), (8.8, 10.0, 8.0), "arm"),
           # the shock mount's ring
           E((6.4, 7.0, 6.4), (9.6, 7.4, 6.8), "arm"),
           E((6.4, 7.0, 9.2), (9.6, 7.4, 9.6), "arm"),
           E((6.4, 7.0, 6.8), (6.8, 7.4, 9.2), "arm"),
           E((9.2, 7.0, 6.8), (9.6, 7.4, 9.2), "arm"),
           E((8.4, 7.4, 7.6), (8.8, 9.2, 8.0), "arm")]
    els += post(8.0, 8.0, 1.05, 2.6, 6.6, "body", top=False)
    els += post(8.0, 8.0, 1.15, 6.6, 9.0, "grille")
    els += post(8.0, 8.0, 0.5, 2.0, 2.6, "body")
    add_device("microphone_boom_arm_black",
               N("Streaming Microphone with Boom (Black)", "Mikrofon am Schwenkarm (Schwarz)",
                 "Micrófono en Brazo Articulado (Negro)", "Mikrofon på bomarm (svart)"),
               els, {"arm": tc.T("black_anodised"), "body": tc.T("black_anodised"),
                     "grille": C.T("mic_grille")}, display=tc.desk_display(0.9, 0),
               box=[6.0, 0.0, 6.0, 16.0, 11.0, 16.0])


_mic()


@tex("speaker_front")
def _spk_front():
    p = Pic(3.2, 5.5, 32, 8, bg=(28, 29, 32))
    p.disc(1.6, 3.7, 1.2, (16, 16, 18))
    p.ring(1.6, 3.7, 1.2, 0.15, (70, 72, 78))
    p.disc(1.6, 3.7, 0.4, (40, 42, 46))
    p.disc(1.6, 1.4, 0.55, (16, 16, 18))
    p.ring(1.6, 1.4, 0.55, 0.1, (90, 92, 98))
    p.disc(1.6, 5.15, 0.1, (90, 200, 255))
    return p.done()


add_device("speakers_desktop_pair_black",
           N("Desktop Speakers (Pair, Black)", "Desktop-Lautsprecher (Paar, Schwarz)",
             "Altavoces de Escritorio (Par, Negros)", "Datorhögtalare (par, svarta)"),
           [E((0.6, 0, 6.0), (3.8, 5.5, 9.6), "box", per={"north": "front"}, full=("north",)),
            E((12.2, 0, 6.0), (15.4, 5.5, 9.6), "box", per={"north": "front"}, full=("north",))],
           {"box": tc.T("black_anodised"), "front": C.T("speaker_front")},
           display=tc.desk_display(0.8, 0))


@tex("subwoofer_front")
def _sub_front():
    p = Pic(7, 7.5, 32, 6, bg=(28, 29, 32))
    p.disc(3.5, 3.4, 2.7, (16, 16, 18))
    p.ring(3.5, 3.4, 2.7, 0.25, (70, 72, 78))
    p.ring(3.5, 3.4, 2.0, 0.1, (40, 42, 46))
    p.disc(3.5, 3.4, 0.9, (40, 42, 46))
    p.rect(1.0, 6.6, 6.0, 7.0, (12, 12, 14))
    return p.done()


add_device("subwoofer_desktop_black",
           N("Subwoofer (Black)", "Subwoofer (Schwarz)", "Subwoofer (Negro)", "Subwoofer (svart)"),
           [E((4.5, 0.4, 4.5), (11.5, 7.9, 11.5), "box", per={"north": "front"}, full=("north",))]
           + feet(4.5, 4.5, 11.5, 11.5, 0.4),
           {"box": tc.T("black_anodised"), "front": C.T("subwoofer_front"),
            "feet": tc.T("port_black")}, display=False)


@tex("usb_hub_front")
def _hub_front():
    p = Pic(6, 1.0, 32, 8, bg=(40, 42, 46))
    for k in range(4):
        p.rect(0.5 + k * 1.4, 0.3, 1.4 + k * 1.4, 0.65, (16, 40, 120))
        p.rect(0.6 + k * 1.4, 0.38, 1.3 + k * 1.4, 0.48, (200, 200, 205))
    p.disc(5.75, 0.5, 0.08, (90, 200, 255))
    return p.done()


add_device("usb_hub_grey",
           N("USB Hub (Grey)", "USB-Hub (Grau)", "Concentrador USB (Gris)", "USB-hubb (grå)"),
           [E((5.0, 0, 7.0), (11.0, 1.0, 9.0), "body", per={"north": "front"}, full=("north",)),
            E((10.4, 0.2, 9.0), (10.8, 0.6, 12.0), "cable", SIDES + ("up",))],
           {"body": tc.T("grey_abs"), "front": C.T("usb_hub_front"), "cable": tc.T("port_black")},
           display=tc.desk_display(1.6, 2))

add_device("external_drive_black",
           N("External Drive (Black)", "Externe Festplatte (Schwarz)",
             "Disco Externo (Negro)", "Extern hårddisk (svart)"),
           [E((6.6, 0, 6.0), (9.4, 0.8, 10.2), "body"),
            E((7.7, 0.8, 6.4), (8.3, 0.85, 6.6), "led", ("up",), shade=False),
            E((7.6, 0.2, 10.2), (8.4, 0.5, 13.0), "cable", SIDES + ("up",))],
           {"body": C.T("brushed_silver"), "led": C.T("led_blue"), "cable": tc.T("port_black")},
           display=tc.desk_display(2.0, 2))
C.textures["led_blue"] = lambda: tc.fill((90, 200, 255), 16, 2, 661)


def _dock():
    els = [E((6.4, 0, 3.0), (9.6, 1.6, 13.0), "dock"),
           E((7.3, 1.6, 3.6), (8.7, 11.2, 12.4), "laptop",
             per={"east": "lid", "west": "lid"}, uv={"east": FULL, "west": FULL}),
           E((6.2, 0.6, 3.0), (6.4, 1.0, 4.0), "led", ("west",), shade=False)]
    add_device("docking_station_vertical_silver",
               N("Vertical Docking Station with Laptop", "Vertikale Dockingstation mit Laptop",
                 "Base de Acoplamiento Vertical con Portátil",
                 "Vertikal dockningsstation med bärbar dator"),
               els, {"dock": C.T("brushed_silver"), "laptop": C.T("brushed_silver"),
                     "lid": C.T("laptop_silver_lid"), "led": C.T("led_blue")},
               display=tc.desk_display(0.85, 0))


@tex("laptop_silver_lid")
def _silver_lid():
    p = Pic(8.8, 9.6, 32, 6, bg=SILVER)
    p.ring(4.4, 4.8, 0.9, 0.18, (140, 142, 148))
    return grain(p.done(), 3, 671)


_dock()


@tex("kvm_front")
def _kvm_front():
    p = Pic(6, 1.4, 32, 8, bg=(46, 48, 52))
    for k in range(4):
        x = 0.6 + k * 1.1
        p.rect(x, 0.35, x + 0.8, 1.05, (90, 92, 98))
        p.disc(x + 0.4, 0.2, 0.07, (70, 220, 90) if k == 1 else (52, 56, 54))
    p.rect(5.0, 0.5, 5.6, 0.9, (16, 40, 120))
    return p.done()


add_device("kvm_switch_grey",
           N("KVM Switch (Grey)", "KVM-Switch (Grau)", "Conmutador KVM (Gris)", "KVM-switch (grå)"),
           [E((5.0, 0, 6.0), (11.0, 1.4, 10.0), "body", per={"north": "front"}, full=("north",))],
           {"body": tc.T("grey_abs"), "front": C.T("kvm_front")}, display=tc.desk_display(1.5, 2))

anim("macro_keys", 4, lambda f: _macro(f), 10)


def _macro(f):
    p = Pic(4.8, 3.2, 32, 8, bg=(20, 20, 22))
    for r in range(3):
        for c in range(5):
            x, y = 0.25 + c * 0.9, 0.25 + r * 0.95
            h = (r * 5 + c) / 15.0 + f * 0.25
            p.rect(x, y, x + 0.75, y + 0.8, (40, 42, 46))
            p.rect(x + 0.1, y + 0.1, x + 0.65, y + 0.7, hue(h, 0.6, 0.85))
            p.disc(x + 0.375, y + 0.4, 0.15, (240, 240, 245))
    return p.done()


add_device("macro_keypad_black",
           N("Macro Keypad (15 Key, Black)", "Makro-Tastenfeld (15 Tasten, Schwarz)",
             "Teclado de Macros (15 Teclas, Negro)", "Makroknappsats (15 knappar, svart)"),
           [E((5.6, 0, 6.4), (10.4, 1.0, 9.6), "body", per={"up": "keys"}, uv={"up": FULL},
              rot={"up": 180}),
            E((6.6, 0, 9.6), (9.4, 2.0, 10.4), "body")],
           {"body": tc.T("black_anodised"), "keys": C.T("macro_keys")},
           display=tc.desk_display(1.6, 2))


# ------------------------------------------------------------------------------------------
# Consoles
# ------------------------------------------------------------------------------------------
@tex("orbix_front")
def _orbix_front():
    p = Pic(1.2, 11.1, 16, 8, bg=(24, 25, 28))
    p.rect(0.4, 0.6, 0.8, 1.0, (100, 170, 255))
    p.rect(0.2, 9.0, 1.0, 9.3, (60, 62, 66))
    p.rect(0.2, 9.7, 1.0, 10.0, (60, 62, 66))
    return p.done()


add_device("console_orbix_white",
           N("Orbix Console (White)", "Orbix-Konsole (Weiß)", "Consola Orbix (Blanca)",
             "Orbix-konsol (vit)"),
           [E((6.0, 0.6, 3.0), (7.4, 12.6, 13.0), "white"),
            E((8.6, 0.6, 3.0), (10.0, 12.6, 13.0), "white"),
            E((7.4, 0.9, 3.4), (8.6, 12.0, 12.6), "core", per={"north": "front"},
              full=("north",)),
            E((5.8, 0, 6.0), (10.2, 0.6, 10.0), "core")],
           {"white": tc.T("white_plastic"), "core": tc.T("black_anodised"),
            "front": C.T("orbix_front")}, display=tc.desk_display(0.8, 0))


@tex("vertex_front")
def _vertex_front():
    p = Pic(11, 2.6, 32, 8, bg=(20, 21, 24))
    p.rect(0.6, 1.1, 5.0, 1.3, (8, 8, 10))
    p.rect(6.5, 0.4, 10.4, 0.55, (40, 140, 255))
    p.disc(10.0, 1.6, 0.25, (60, 62, 66))
    for k in range(2):
        p.rect(7.4 + k * 0.8, 1.5, 7.9 + k * 0.8, 1.75, (16, 40, 120))
    return p.done()


@tex("vertex_top")
def _vertex_top():
    p = Pic(11, 8, 32, 6, bg=(20, 21, 24))
    for k in range(10):
        p.rect(4.0 + k * 0.6, 0.8, 4.3 + k * 0.6, 7.2, (8, 8, 10))
    p.rect(0.8, 0.8, 3.0, 2.0, (34, 35, 40))
    return p.done()


add_device("console_vertex_black",
           N("Vertex Console (Black)", "Vertex-Konsole (Schwarz)", "Consola Vertex (Negra)",
             "Vertex-konsol (svart)"),
           [E((2.5, 0.4, 4.0), (13.5, 3.0, 12.0), "body",
              per={"north": "front", "up": "top"}, full=("north", "up"), rot={"up": 180})]
           + feet(2.5, 4.0, 13.5, 12.0, 0.4),
           {"body": tc.T("black_anodised"), "front": C.T("vertex_front"),
            "top": C.T("vertex_top"), "feet": tc.T("port_black")},
           display=tc.desk_display(0.8, 2))


def _pocketeer():
    els = [E((4.6, 0, 7.2), (11.4, 2.6, 9.2), "dock", per={"north": "dockfront"},
             full=("north",))]
    # the handheld standing in the dock: screen between two coloured grips
    y0, y1 = 1.4, 5.3
    els += [E((5.0, y0, 7.6), (11.0, y1, 8.2), "body"),
            E((5.3, y0 + 0.3, 7.52), (10.7, y1 - 0.3, 7.6), "screen", ("north",),
              uv={"north": FULL}, shade=False),
            E((11.0, y0, 7.6), (12.4, y1, 8.3), "gripl"),
            E((3.6, y0, 7.6), (5.0, y1, 8.3), "gripr"),
            E((11.5, y0 + 2.4, 7.5), (11.9, y0 + 2.8, 7.6), "body", ("north",)),
            E((4.1, y0 + 1.0, 7.5), (4.5, y0 + 1.4, 7.6), "body", ("north",))]
    add_device("console_pocketeer_docked",
               N("Pocketeer Handheld Console in its Dock",
                 "Pocketeer-Handheld-Konsole im Dock",
                 "Consola Portátil Pocketeer en su Base",
                 "Pocketeer handhållen konsol i dockan"),
               els, {"dock": tc.T("black_anodised"), "dockfront": C.T("pocketeer_dock"),
                     "body": tc.T("port_black"), "screen": C.T("screen_w_game"),
                     "gripl": C.T("grip_teal"), "gripr": C.T("grip_orange")},
               display=tc.desk_display(1.0, 1))


C.textures["grip_teal"] = lambda: tc.fill((20, 180, 190), 16, 3, 681)
C.textures["grip_orange"] = lambda: tc.fill((250, 120, 40), 16, 3, 682)


@tex("pocketeer_dock")
def _pk_dock():
    p = Pic(6.8, 2.6, 32, 8, bg=(30, 31, 35))
    # an invented mark: two offset rounded squares
    p.rect(2.8, 0.9, 3.4, 1.6, (200, 200, 205))
    p.rect(3.5, 1.1, 4.1, 1.8, (200, 200, 205))
    p.disc(6.2, 2.2, 0.08, (70, 220, 90))
    return p.done()


_pocketeer()


@tex("retro_console_top")
def _retro_top():
    p = Pic(8, 6, 32, 6, bg=(176, 176, 172))
    p.rect(1.5, 0.6, 6.5, 2.0, (60, 60, 64))                 # cartridge slot
    p.rect(0.8, 3.6, 2.2, 4.4, (120, 60, 160))               # power
    p.rect(2.8, 3.6, 4.2, 4.4, (110, 110, 116))              # reset
    p.rect(0.6, 5.2, 7.4, 5.4, (150, 150, 146))
    return p.done()


add_device("console_retro_grey",
           N("Retro 16-Bit Console (Grey)", "Retro-16-Bit-Konsole (Grau)",
             "Consola Retro de 16 Bits (Gris)", "Retro 16-bitarskonsol (grå)"),
           [E((4.0, 0, 5.0), (12.0, 2.0, 11.0), "body", per={"up": "top"}, uv={"up": FULL},
              rot={"up": 180}),
            E((5.8, 2.0, 9.6), (10.2, 4.6, 10.4), "cart"),
            E((6.0, 4.6, 9.7), (10.0, 4.8, 10.3), "cart")],
           {"body": C.T("retro_grey"), "top": C.T("retro_console_top"),
            "cart": tc.T("grey_abs")}, display=tc.desk_display(1.0, 2))


# ------------------------------------------------------------------------------------------
# Items: controllers and a VR headset
# ------------------------------------------------------------------------------------------
def _pad_sprite(body, accent, sticks, buttons):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((1, 5, 14, 11), radius=2, fill=rgba(body))
    d.ellipse((1, 7, 5, 13), fill=rgba(body))
    d.ellipse((10, 7, 14, 13), fill=rgba(body))
    d.rectangle((3, 7, 3, 9), fill=rgba(accent))
    d.rectangle((2, 8, 4, 8), fill=rgba(accent))
    for x, y in sticks:
        img.putpixel((x, y), rgba((30, 30, 34)))
        img.putpixel((x + 1, y), rgba((30, 30, 34)))
    for (x, y), c in buttons:
        img.putpixel((x, y), rgba(c))
    # outline
    px = img.load()
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + a < 16 and 0 <= y + b < 16 and px[x + a, y + b][3]
                                        for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), rgba(tc.shade(body, 0.45)))
    return out


C.textures["controller_orbix_white"] = lambda: _pad_sprite(
    WHITE, (40, 40, 44), [(5, 9), (9, 9)],
    [((11, 6), (90, 140, 230)), ((12, 7), (230, 80, 80)), ((10, 7), (90, 200, 120)),
     ((11, 8), (230, 200, 80))])
C.textures["controller_vertex_black"] = lambda: _pad_sprite(
    (44, 45, 50), (150, 150, 156), [(4, 7), (9, 9)],
    [((11, 6), (240, 200, 60)), ((12, 7), (230, 70, 70)), ((10, 7), (80, 140, 240)),
     ((11, 8), (90, 210, 110))])
C.textures["controller_retro_grey"] = lambda: _pad_sprite(
    (190, 190, 186), (60, 60, 64), [],
    [((11, 7), (120, 60, 160)), ((12, 8), (120, 60, 160)), ((7, 8), (90, 90, 94)),
     ((8, 8), (90, 90, 94))])


@tex("controller_pocketeer_grips")
def _grips():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((2, 2, 6, 14), radius=2, fill=rgba((20, 180, 190)))
    d.rounded_rectangle((9, 2, 13, 14), radius=2, fill=rgba((250, 120, 40)))
    d.rectangle((6, 2, 6, 14), fill=rgba((24, 24, 28)))
    d.rectangle((9, 2, 9, 14), fill=rgba((24, 24, 28)))
    for x, y in ((4, 5), (11, 9)):
        d.ellipse((x - 1, y - 1, x + 1, y + 1), fill=rgba((30, 30, 34)))
    for x, y in ((3, 10), (5, 10), (4, 9), (4, 11), (11, 5), (12, 4), (10, 4), (11, 3)):
        img.putpixel((x, y), rgba((40, 40, 44)))
    return img


@tex("vr_headset_black")
def _vr():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((1, 7, 14, 8), fill=rgba((50, 52, 58)))
    d.rounded_rectangle((2, 4, 13, 11), radius=3, fill=rgba((34, 35, 38)))
    d.rounded_rectangle((3, 5, 12, 10), radius=2, fill=rgba((20, 21, 24)))
    for x in (5, 10):
        img.putpixel((x, 7), rgba((90, 92, 100)))
    img.putpixel((7, 6), rgba((100, 200, 255)))
    img.putpixel((8, 6), rgba((100, 200, 255)))
    return img


for _reg, _what, _names in (
        ("controller_orbix_white", "controller",
         N("Orbix Controller (White)", "Orbix-Controller (Weiß)", "Mando Orbix (Blanco)",
           "Orbix-handkontroll (vit)")),
        ("controller_vertex_black", "controller",
         N("Vertex Controller (Black)", "Vertex-Controller (Schwarz)", "Mando Vertex (Negro)",
           "Vertex-handkontroll (svart)")),
        ("controller_pocketeer_grips", "controller",
         N("Pocketeer Controller Grips (Teal and Orange)",
           "Pocketeer-Controller-Griffe (Türkis und Orange)",
           "Mandos Pocketeer (Turquesa y Naranja)",
           "Pocketeer-handkontroller (turkos och orange)")),
        ("controller_retro_grey", "controller",
         N("Retro 16-Bit Controller (Grey)", "Retro-16-Bit-Controller (Grau)",
           "Mando Retro de 16 Bits (Gris)", "Retro 16-bitars handkontroll (grå)")),
        ("vr_headset_black", "VR headset",
         N("VR Headset (Black)", "VR-Headset (Schwarz)", "Visor de RV (Negro)",
           "VR-headset (svart)"))):
    C.add_item(_reg, 'new ItemDecorativeFactory("%s", "This %s does nothing and is only for '
               'looks!")' % (_reg, _what), _names, _reg, tab=TAB)


# Creative order: rigs and laptops, monitors, keyboards, mice and pads, desk extras, consoles
# (the sections above are in the order their helpers need).
_GROUPS = (("pc_", "laptop_"), ("monitor_",), ("keyboard_", "numpad_"), ("mouse_", "desk_mat_"),
           None, ("console_",))


def _group(reg):
    for i, g in enumerate(_GROUPS):
        if g and reg.startswith(g):
            return i
    return _GROUPS.index(None)


C.blocks.sort(key=lambda b: _group(b["registry"]))  # stable: keeps each group's own order

if __name__ == "__main__":
    sys.exit(C.main())
