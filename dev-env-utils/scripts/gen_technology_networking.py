#!/usr/bin/env python3
"""Every asset the Technology - Networking & IT tab ships: Micafi network gear.

    python dev-env-utils/scripts/gen_technology_networking.py
    python dev-env-utils/scripts/gen_technology_networking.py --check
    python dev-env-utils/scripts/gen_technology_networking.py --fragments   # tab lines to paste

Micafi is the house networking brand, an invented one (the Mica family of Micaplex and
Micarolla), with its own mark drawn in gen_technology_common.py. Nothing here copies a real
maker's name, mark or livery.

What is here, and the class that places each:

- **Desktop switches** (technology.BlockDeskDeviceFactory): the Micafi 5-, 8- and 16-port
  (PoE) switches and the Flex Mini, white boxes with their ports and link and activity LEDs on
  the front and the mark and wordmark on top. The LEDs blink by an animated texture whose frame
  carries both the front and the top, so nothing ticks.
- **Gateways and boxes** (BlockDeskDeviceFactory): the Micafi Gateway, the Micafi Control Hub
  (the small box the controller software runs on), an invented ISP's cable modem stood on end
  (Corvane; its status lights an animated texture) and the Micafi PoE injector.
- **Wiring**: keystone wall jacks with one, two or four ports, flush on a wall, and a floor box
  (BlockDeskDeviceFactory); and two cable trays, a wire basket and a cable ladder
  (technology.network.BlockCableTray), which join along runs and into tees, corners and
  crossings from actual state, lie at the bottom of their block or hang at its top on rods, and
  are drawn by a multipart blockstate from three parts (the middle, an arm reaching out of it,
  a side rail) turned to each side; and the threaded rod hanger (BlockCableTrayHanger), stacked
  under a ceiling so a hung tray of either kind can sit lower, its rods lined up with the tray's.

Writes models under models/block/technology/networking, textures under
textures/blocks/technology/networking, and the shared swatches under
textures/blocks/technology/common (see gen_technology_common.py).
"""
import json
import os
import random
import sys
import zlib

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_technology_common as tc  # noqa: E402

C = tc.catalogue("gen_technology_networking.py", "networking")
TAB = tc.NETWORKING

C.add_lang("itemGroup.tabtechnetworking",
           ("CSM: Technology - Networking & IT", "CSM: Technik - Netzwerk & IT",
            "CSM: Tecnología - Redes y TI", "CSM: Teknik - Nätverk & IT"))

SIDES = ("east", "west", "south", "down")


# ------------------------------------------------------------------------------------------
# Desktop switches
# ------------------------------------------------------------------------------------------
# The 8-port switch: 8 x 2 x 6 units. Its face texture is 64 px square at 8 texels a unit: the
# front (8 x 2 units) is rows 0..15, the top (8 x 6) rows 16..63, so one animated texture
# carries both and the top's mark is drawn once with the LEDs.
SW8_BOX = (4, 0, 5, 12, 2, 11)
SW8_PORTS = 8
SW8_LINKED = 6          # ports 0..5 have a cable in; 6 and 7 are dark
SW8_FRAMES = 8


def switch_face_frame(frame_no, ports, linked, seed):
    """One frame of a desktop switch's face texture (front band and top)."""
    img = tc.fill(tc.WHITE_PLASTIC, 64, 2, 401)
    # the front: a slightly shaded band with a darker lip at the foot
    tc.rect(img, 0, 0, 64, 16, tc.shade(tc.WHITE_PLASTIC, 0.94))
    tc.rect(img, 0, 15, 64, 16, tc.shade(tc.WHITE_PLASTIC, 0.78))
    pitch = 7
    x0 = (64 - ports * pitch + 1) // 2
    rng = random.Random(seed)
    # every port's activity for every frame, from one seed, so the frames are a fixed loop
    activity = [[rng.random() < 0.55 for _ in range(SW8_FRAMES)] for _ in range(ports)]
    for p in range(ports):
        x = x0 + p * pitch
        # the RJ45 jack: a black opening, its contacts along the top, the latch notch below
        tc.rect(img, x, 5, x + 6, 12, tc.PORT_BLACK)
        tc.rect(img, x + 1, 6, x + 5, 7, tc.PIN_GOLD)
        tc.rect(img, x + 2, 12, x + 4, 13, tc.PORT_BLACK)
        on = p < linked
        tc.rect(img, x, 2, x + 2, 4, tc.LED_GREEN if on else tc.LED_OFF)
        blink = on and activity[p][frame_no]
        tc.rect(img, x + 4, 2, x + 6, 4, tc.LED_AMBER if blink else tc.LED_OFF)
    # the top, drawn as its user sees it (front at the foot): a shallow border, the mark and
    # wordmark, and the white status light by the front edge
    top = Image.new("RGBA", (64, 48), (0, 0, 0, 0))
    tc.frame(top, 1, 1, 63, 47, tc.shade(tc.WHITE_PLASTIC, 0.9))
    tc.micafi_mark(top, 26, 9, 12, tc.MICAFI_TEAL)
    tc.draw_text_centred(top, "MICAFI", 32, 25, tc.shade(tc.GREY_ABS, 0.9))
    tc.rect(top, 30, 40, 34, 42, (246, 250, 255))
    img.alpha_composite(top, (0, 16))
    return img


def switch_8():
    reg = "micafi_switch_8_port"
    tex = "switch_8_port_face"
    tc.animation(C, tex, [lambda f=f: switch_face_frame(f, SW8_PORTS, SW8_LINKED, 8008)
                          for f in range(SW8_FRAMES)], 3)
    x0, y0, z0, x1, y1, z1 = SW8_BOX
    els = [tc.B((x0, y0, z0), (x1, y1, z1), "face", ("north", "up"),
                uv={"north": [0, 0, 16, 4], "up": [0, 4, 16, 16]}, rot={"up": 180}),
           tc.B((x0, y0, z0), (x1, y1, z1), "shell", SIDES)]
    m = tc.model({"face": C.T(tex), "shell": tc.T("white_plastic"),
                  "particle": tc.T("white_plastic")}, els)
    C.add(reg, tc.desk_device_java(reg, SW8_BOX),
          tc.names("Micafi 8-Port Desktop Switch", "Micafi 8-Port-Desktop-Switch",
                   "Switch de Escritorio Micafi de 8 Puertos", "Micafi 8-ports skrivbordsswitch"),
          {reg: tc.with_display(m, tc.desk_display(1.2, 1.5))},
          tc.facing_state(C.M(reg)), tab=TAB)


switch_8()

# ------------------------------------------------------------------------------------------
# The rest of the desktop switches, the gateway and controller, the ISP modem and PoE
# injector, the wall jacks and floor box, and the cable trays
# ------------------------------------------------------------------------------------------
Pic = tc.Pic
E = tc.E
rgba = tc.rgba
WHITE = tc.WHITE_PLASTIC
PLASTIC_BLACK = (28, 29, 31)
SCREEN = (14, 18, 26)
LCD_BLUE = (90, 170, 255)
LED_BLUE = (70, 140, 255)
STAINLESS = (178, 181, 184)


def wall_display(scale=1.0, y=0.0):
    """Item transforms for a thin piece hung on a wall (its back at z = 16): the block's
    three-quarter view, larger than a full block's, since the piece is small."""
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, y, 0], "scale": [scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, -4], "scale": [scale] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.5] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.5] * 3},
    }


def _activity(seed, ports, frames):
    rng = random.Random(seed)
    return [[rng.random() < 0.55 for _ in range(frames)] for _ in range(ports)]


SWITCH_FRAMES = 4


def desk_switch_frame(f, W, H, D, ports, linked, seed, poe=0):
    """One frame of a desktop switch W x H x D units: its front (rows 0..H*k) and its top
    (rows H*k..(H+D)*k), both at k texels a unit on a 64 px frame, so one animated texture
    carries both. ``poe`` ports (the first ones) light a blue PoE lamp rather than amber."""
    k = min(64.0 / W, 64.0 / (H + D))
    img = tc.fill(WHITE, 64, 2, seed)
    d = ImageDraw.Draw(img)

    def R(x0, y0, x1, y1, c, oy=0.0):
        X0, Y0 = int(round(x0 * k)), int(round((y0 + oy) * k))
        X1, Y1 = max(X0 + 1, int(round(x1 * k))), max(Y0 + 1, int(round((y1 + oy) * k)))
        d.rectangle((X0, Y0, X1 - 1, Y1 - 1), fill=rgba(c))

    R(0, 0, W, H, tc.shade(WHITE, 0.94))
    R(0, H - 0.14, W, H, tc.shade(WHITE, 0.78))
    act = _activity(seed, ports, SWITCH_FRAMES)
    margin = 0.45
    pitch = (W - 2 * margin) / ports
    pw = pitch * 0.78
    for p in range(ports):
        x = margin + p * pitch + (pitch - pw) / 2
        y0, y1 = H * 0.36, H * 0.86
        R(x, y0, x + pw, y1, tc.PORT_BLACK)
        R(x + pw * 0.15, y0 + (y1 - y0) * 0.12, x + pw * 0.85, y0 + (y1 - y0) * 0.26, tc.PIN_GOLD)
        on = p < linked
        lw = pw * 0.3
        R(x, H * 0.1, x + lw, H * 0.26, tc.LED_GREEN if on else tc.LED_OFF)
        lamp = LED_BLUE if p < poe else tc.LED_AMBER
        R(x + pw - lw, H * 0.1, x + pw, H * 0.26, lamp if on and act[p][f] else tc.LED_OFF)
    # the top, drawn as its user sees it (front at the foot): a shallow border, the mark, the
    # wordmark where it fits, and the white status light by the front edge
    oy = H
    R(0, 0, W, D, WHITE, oy)
    edge = tc.shade(WHITE, 0.9)
    R(0.1, 0.1, W - 0.1, 0.22, edge, oy)
    R(0.1, D - 0.22, W - 0.1, D - 0.1, edge, oy)
    R(0.1, 0.1, 0.22, D - 0.1, edge, oy)
    R(W - 0.22, 0.1, W - 0.1, D - 0.1, edge, oy)
    size = int(round(min(W, D) * k * 0.32))
    cx, cy = W * k / 2.0, (oy + D * 0.4) * k
    tc.micafi_mark(img, cx - size / 2.0, cy - size / 2.0, size, tc.MICAFI_TEAL)
    if W * k >= 30 and D * k * 0.3 >= 7:
        tc.draw_text_centred(img, "MICAFI", W * k / 2.0, int(cy + size / 2.0 + 2),
                             tc.shade(tc.GREY_ABS, 0.9))
    R(W / 2.0 - 0.25, D - 0.55, W / 2.0 + 0.25, D - 0.35, (246, 250, 255), oy)
    return img


def desk_switch(reg, names, W, H, D, ports, linked, seed, poe=0, display=1.4):
    tex = reg.replace("micafi_", "") + "_face"
    tc.anim(C, tex, SWITCH_FRAMES,
            lambda f: desk_switch_frame(f, W, H, D, ports, linked, seed, poe), 3)
    k = min(64.0 / W, 64.0 / (H + D))
    u1 = round(W * k / 4.0, 4)
    vf = round(H * k / 4.0, 4)
    vt = round((H + D) * k / 4.0, 4)
    x0, z0 = (16 - W) / 2.0, (16 - D) / 2.0
    b = (x0, 0, z0, x0 + W, H, z0 + D)
    els = [tc.B(b[:3], b[3:], "face", ("north", "up"),
                uv={"north": [0, 0, u1, vf], "up": [0, vf, u1, vt]}, rot={"up": 180}),
           tc.B(b[:3], b[3:], "shell", SIDES)]
    m = tc.model({"face": C.T(tex), "shell": tc.T("white_plastic"),
                  "particle": tc.T("white_plastic")}, els)
    C.add(reg, tc.desk_device_java(reg, b), names,
          {reg: tc.with_display(m, tc.desk_display(display, 1.5))},
          tc.facing_state(C.M(reg)), tab=TAB)


desk_switch("micafi_switch_5_port",
            tc.names("Micafi 5-Port Desktop Switch", "Micafi 5-Port-Desktop-Switch",
                     "Switch de Escritorio Micafi de 5 Puertos", "Micafi 5-ports skrivbordsswitch"),
            6.0, 2.0, 4.5, 5, 4, 5005, display=1.6)
desk_switch("micafi_switch_16_port",
            tc.names("Micafi 16-Port PoE Desktop Switch", "Micafi 16-Port-PoE-Desktop-Switch",
                     "Switch PoE de Escritorio Micafi de 16 Puertos",
                     "Micafi 16-ports PoE-skrivbordsswitch"),
            14.0, 2.5, 7.0, 16, 12, 1616, poe=8, display=1.0)
desk_switch("micafi_switch_flex_mini",
            tc.names("Micafi Flex Mini Switch", "Micafi Flex-Mini-Switch",
                     "Switch Micafi Flex Mini", "Micafi Flex mini-switch"),
            4.5, 1.25, 3.5, 5, 3, 4545, display=2.0)
# The 8-port switch (above) sits between the 5- and 16-port ones in the tab.
C.blocks.insert(1, C.blocks.pop(0))


def _pic_tex(name, w, h, draw, n=32, bg=WHITE, after=None, mark=None):
    """A static face picture: draw(p) on a Pic of w x h units, squeezed square. ``mark``
    (x, y, size, in the face's units) draws the Micafi mark before the squeeze, so it keeps its
    shape on a face that is not square; ``after`` draws on the squeezed texture."""
    def make():
        p = Pic(w, h, n, bg=bg)
        draw(p)
        if mark:
            tc.micafi_mark(p.img, int(round(mark[0] * p.s)), int(round(mark[1] * p.s)),
                           int(round(mark[2] * p.s)), tc.MICAFI_TEAL, k=2)
        img = tc.grain(p.done(), 1.5, zlib.crc32(name.encode("utf-8")) & 0xffff)
        if after:
            after(img)
        return img
    C.textures[name] = make


def _mark_after(cx, cy, size, colour=tc.MICAFI_TEAL):
    return lambda img: tc.micafi_mark(img, cx - size / 2.0, cy - size / 2.0, size, colour)


def add_desk(reg, names, b, faces, shell, display):
    """A desk device: one box ``b`` whose faces in ``faces`` ({face: texture}) take a whole
    picture each and whose other faces take ``shell``."""
    tex = {"shell": shell, "particle": shell}
    tex.update(faces)
    els = [E(b[:3], b[3:], "shell", faces=tuple(tc.ALL), per={f: f for f in faces},
             full=tuple(faces))]
    m = tc.model(tex, els)
    C.add(reg, tc.desk_device_java(reg, b), names,
          {reg: tc.with_display(m, tc.desk_display(*display))},
          tc.facing_state(C.M(reg)), tab=TAB)


# --- the Micafi Gateway: a white desktop box, a status screen and four ports on its front ---
def _gateway_front(p):
    p.rect(0, 0, 7, 3, tc.shade(WHITE, 0.95))
    p.rect(0.6, 0.55, 2.7, 2.35, (40, 42, 46))
    p.rect(0.75, 0.7, 2.55, 2.2, SCREEN)
    p.rect(0.95, 0.95, 2.1, 1.15, LCD_BLUE)
    p.rect(0.95, 1.4, 1.9, 1.52, (170, 176, 186))
    p.rect(0.95, 1.75, 2.3, 1.87, (170, 176, 186))
    for i in range(4):
        x = 3.3 + i * 0.9
        p.rect(x, 1.05, x + 0.7, 2.25, tc.PORT_BLACK)
        p.rect(x + 0.12, 1.15, x + 0.58, 1.32, tc.PIN_GOLD)
        p.rect(x, 0.65, x + 0.2, 0.85, tc.LED_GREEN if i < 3 else tc.LED_OFF)
    p.rect(0, 2.85, 7, 3, tc.shade(WHITE, 0.8))


def _gateway_top(p):
    p.rect(0, 0, 7, 7, WHITE)
    p.frame(0.1, 0.1, 6.9, 6.9, tc.shade(WHITE, 0.9), 0.12)
    p.ring(3.5, 3.5, 1.9, 0.12, (210, 226, 246))


_pic_tex("gateway_front", 7, 3, _gateway_front)
_pic_tex("gateway_top", 7, 7, _gateway_top, after=_mark_after(16, 16, 9))
add_desk("micafi_gateway",
         tc.names("Micafi Gateway", "Micafi-Gateway", "Puerta de Enlace Micafi", "Micafi gateway"),
         (4.5, 0, 4.5, 11.5, 3, 11.5),
         {"north": C.T("gateway_front"), "up": C.T("gateway_top")}, tc.T("white_plastic"),
         (1.2, 1.5))


# --- the Micafi Control Hub: the small box the network's controller software runs on ---
def _hub_front(p):
    p.rect(0, 0, 4, 1.6, tc.shade(WHITE, 0.95))
    p.rect(0.4, 0.3, 2.2, 1.25, SCREEN)
    p.rect(0.55, 0.45, 1.5, 0.6, LCD_BLUE)
    p.rect(0.55, 0.8, 1.9, 0.9, (170, 176, 186))
    p.rect(2.7, 0.55, 3.5, 0.75, (232, 240, 255))
    p.rect(0, 1.48, 4, 1.6, tc.shade(WHITE, 0.8))


def _hub_top(p):
    p.rect(0, 0, 4, 6, WHITE)
    p.frame(0.1, 0.1, 3.9, 5.9, tc.shade(WHITE, 0.9), 0.12)


_pic_tex("control_hub_front", 4, 1.6, _hub_front)
_pic_tex("control_hub_top", 4, 6, _hub_top, mark=(1.2, 1.6, 1.6))
add_desk("micafi_control_hub",
         tc.names("Micafi Control Hub", "Micafi-Steuerzentrale", "Centro de Control Micafi",
                  "Micafi kontrollnav"),
         (6, 0, 5, 10, 1.6, 11),
         {"north": C.T("control_hub_front"), "up": C.T("control_hub_top")},
         tc.T("white_plastic"), (1.8, 1.5))


# --- an ISP's cable modem, stood on end: a black tower with a column of status lights. The
# ISP, Corvane, is invented, as is its mark (two arcs) ---
MODEM_FRAMES = 4


def _modem_front(f):
    p = Pic(2.5, 9, 32, bg=PLASTIC_BLACK)
    p.rect(0, 0, 2.5, 0.25, tc.shade(PLASTIC_BLACK, 1.5))
    p.ring(1.25, 1.5, 0.75, 0.16, (190, 194, 200))
    p.ring(1.25, 1.5, 0.4, 0.16, (230, 120, 40))
    rng = random.Random(9901 + f)
    lights = [tc.LED_GREEN, tc.LED_GREEN, (80, 150, 255), (80, 150, 255), tc.LED_GREEN,
              tc.LED_AMBER]
    blink = [False, True, True, False, False, True]
    for i, c in enumerate(lights):
        y = 3.2 + i * 0.85
        lit = not blink[i] or rng.random() < 0.6
        p.rect(0.85, y, 1.65, y + 0.32, c if lit else tc.LED_OFF)
    return p.done()


tc.anim(C, "corvane_modem_front", MODEM_FRAMES, _modem_front, 4)


def _modem_side(p):
    p.rect(0, 0, 6, 9, PLASTIC_BLACK)
    for i in range(9):
        y = 1.2 + i * 0.75
        p.rect(1.0, y, 5.0, y + 0.3, (10, 10, 11))


_pic_tex("corvane_modem_side", 6, 9, _modem_side, bg=PLASTIC_BLACK)
C.textures["plastic_black"] = lambda: tc.fill(PLASTIC_BLACK, 16, 2, 4210)
add_desk("corvane_cable_modem",
         tc.names("Corvane Cable Modem", "Corvane-Kabelmodem", "Módem de Cable Corvane",
                  "Corvane kabelmodem"),
         (6.75, 0, 5, 9.25, 9, 11),
         {"north": C.T("corvane_modem_front"), "east": C.T("corvane_modem_side"),
          "west": C.T("corvane_modem_side")}, C.T("plastic_black"), (0.9, 0.5))


# --- the Micafi PoE injector: a black brick, LAN in and PoE out ---
def _poe_front(p):
    p.rect(0, 0, 3, 1.5, PLASTIC_BLACK)
    for x in (0.35, 1.65):
        p.rect(x, 0.45, x + 1.0, 1.3, tc.PORT_BLACK)
        p.rect(x + 0.15, 0.52, x + 0.85, 0.66, tc.PIN_GOLD)
    p.rect(1.42, 0.15, 1.58, 0.3, tc.LED_GREEN)


def _poe_top(p):
    p.rect(0, 0, 3, 5.5, PLASTIC_BLACK)
    p.rect(0.4, 3.8, 2.6, 4.6, (200, 202, 206))
    p.rect(0.55, 3.95, 1.4, 4.45, tc.MICAFI_TEAL)


_pic_tex("poe_injector_front", 3, 1.5, _poe_front, bg=PLASTIC_BLACK)
_pic_tex("poe_injector_top", 3, 5.5, _poe_top, bg=PLASTIC_BLACK)
add_desk("micafi_poe_injector",
         tc.names("Micafi PoE Injector", "Micafi-PoE-Injektor", "Inyector PoE Micafi",
                  "Micafi PoE-injektor"),
         (6.5, 0, 5.25, 9.5, 1.5, 10.75),
         {"north": C.T("poe_injector_front"), "up": C.T("poe_injector_top")},
         C.T("plastic_black"), (2.0, 1.5))


# --- wall jacks: a keystone faceplate flush on a wall, with one, two or four jacks ---
PLATE_W, PLATE_H = 4.5, 7.0


def _faceplate(n):
    def draw(p):
        p.rect(0, 0, PLATE_W, PLATE_H, WHITE)
        p.frame(0, 0, PLATE_W, PLATE_H, tc.shade(WHITE, 0.86), 0.18)
        p.disc(PLATE_W / 2, 0.55, 0.16, tc.shade(WHITE, 0.7))
        p.disc(PLATE_W / 2, PLATE_H - 0.55, 0.16, tc.shade(WHITE, 0.7))
        xs = [PLATE_W / 2] if n < 4 else [1.45, 3.05]
        ys = {1: [3.5], 2: [2.45, 4.55], 4: [2.45, 4.55]}[n]
        for y in ys:
            for x in xs:
                p.rect(x - 0.72, y - 0.62, x + 0.72, y + 0.62, tc.shade(WHITE, 0.9))
                p.rect(x - 0.55, y - 0.45, x + 0.55, y + 0.45, tc.PORT_BLACK)
                p.rect(x - 0.38, y - 0.4, x + 0.38, y - 0.25, tc.PIN_GOLD)
                p.rect(x - 0.15, y + 0.35, x + 0.15, y + 0.45, tc.shade(WHITE, 0.9))
    return draw


for _n, _names in ((1, ("Network Wall Jack (1 Port)", "Netzwerkdose (1 Port)",
                        "Toma de Red de Pared (1 Puerto)", "Nätverksuttag (1 port)")),
                   (2, ("Network Wall Jack (2 Ports)", "Netzwerkdose (2 Ports)",
                        "Toma de Red de Pared (2 Puertos)", "Nätverksuttag (2 portar)")),
                   (4, ("Network Wall Jack (4 Ports)", "Netzwerkdose (4 Ports)",
                        "Toma de Red de Pared (4 Puertos)", "Nätverksuttag (4 portar)"))):
    _reg = "network_faceplate_%d_port" % _n
    _pic_tex("faceplate_%d" % _n, PLATE_W, PLATE_H, _faceplate(_n))
    _x0 = (16 - PLATE_W) / 2.0
    _b = (_x0, 4.5, 15.5, _x0 + PLATE_W, 4.5 + PLATE_H, 16)
    _els = [E(_b[:3], _b[3:], "shell", faces=("north", "east", "west", "up", "down"),
              per={"north": "face"}, full=("north",))]
    _m = tc.model({"face": C.T("faceplate_%d" % _n), "shell": tc.T("white_plastic"),
                   "particle": tc.T("white_plastic")}, _els)
    C.add(_reg, tc.desk_device_java(_reg, _b), tc.names(*_names),
          {_reg: tc.with_display(_m, wall_display(1.6, 0))}, tc.facing_state(C.M(_reg)),
          tab=TAB)


# --- a floor box: a stainless cover flush on the floor, its flap lifted on two jacks and an
# outlet ---
def _floor_box(p):
    p.rect(0, 0, 10, 10, STAINLESS)
    for i in range(20):
        p.rect(0, i * 0.5, 10, i * 0.5 + 0.08, tc.shade(STAINLESS, 1.06))
    p.frame(0, 0, 10, 10, tc.shade(STAINLESS, 0.8), 0.25)
    p.rect(1.2, 1.2, 8.8, 8.8, tc.shade(STAINLESS, 0.92))
    p.frame(1.2, 1.2, 8.8, 8.8, tc.shade(STAINLESS, 0.7), 0.15)
    p.rect(2.0, 5.2, 8.0, 8.0, (40, 40, 42))
    for x in (2.6, 4.1):
        p.rect(x, 5.9, x + 1.1, 7.0, tc.PORT_BLACK)
        p.rect(x + 0.15, 6.0, x + 0.95, 6.15, tc.PIN_GOLD)
        p.rect(x - 0.15, 5.75, x + 1.25, 5.85, WHITE)
    p.rect(5.7, 5.6, 7.6, 7.6, (226, 222, 210))
    for y in (6.0, 6.9):
        p.rect(6.2, y, 6.35, y + 0.4, (30, 30, 30))
        p.rect(6.9, y, 7.05, y + 0.4, (30, 30, 30))
    p.rect(1.6, 4.6, 8.4, 4.9, tc.shade(STAINLESS, 0.6))
    p.disc(2.0, 2.0, 0.22, tc.shade(STAINLESS, 0.6))
    p.disc(8.0, 2.0, 0.22, tc.shade(STAINLESS, 0.6))


_pic_tex("floor_box", 10, 10, _floor_box, bg=STAINLESS)
C.textures["stainless"] = lambda: tc.fill(STAINLESS, 16, 3, 4220)
add_desk("floor_box_network",
         tc.names("Network Floor Box", "Netzwerk-Bodentank", "Caja de Suelo de Red",
                  "Golvbox för nätverk"),
         (3, 0, 3, 13, 0.6, 13), {"up": C.T("floor_box")}, C.T("stainless"), (1.0, 2.5))


# ------------------------------------------------------------------------------------------
# Cable trays: a wire basket and a ladder rack, joining along runs (BlockCableTray)
# ------------------------------------------------------------------------------------------
# Every part is drawn for one direction and turned by the multipart blockstate: the middle
# (rungs across a run along z), an arm reaching north out of the middle to the block edge, and
# a side rail along the middle's north side (drawn where the tray does not reach that way). The
# tray is 12 wide (x 2..14). Lying at the bottom of its block it starts at y 0; hung at the top
# its parts are lifted and the rails carry rods up to the ceiling. Ladder rungs are 4 apart
# across block lines too: each arm carries half a rung at the block face, which meets the next
# block's half.
GALV = (172, 176, 178)
LADDER_BLACK = (36, 37, 40)
# the ladder's item icon: its black powder coat lifted to a charcoal, or the icon is a black
# shape on the dark hotbar
LADDER_ICON = (100, 103, 108)
ROD = (90, 92, 96)


@C.texture("wire_mesh")
def _wire_mesh():
    """A wire basket's mesh: 32 px, a wire every 8 texels each way, clear between."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(32):
        for x in range(32):
            if x % 8 == 0 or y % 8 == 0:
                k = 1.0 if (x % 8 == 0) != (y % 8 == 0) else 1.12
                px[x, y] = tc.clamp(tuple(v * k for v in GALV)) + (255,)
    return img


C.textures["galvanized"] = lambda: tc.fill(GALV, 16, 4, 4230)
C.textures["ladder_black"] = lambda: tc.fill(LADDER_BLACK, 16, 2, 4231)
C.textures["ladder_icon"] = lambda: tc.fill(LADDER_ICON, 16, 3, 4233)
C.textures["hanger_rod"] = lambda: tc.fill(ROD, 16, 2, 4232)


def _copy(els):
    return json.loads(json.dumps(els))


def _lift(els, dy):
    out = _copy(els)
    for e in out:
        e["from"][1] = round(e["from"][1] + dy, 4)
        e["to"][1] = round(e["to"][1] + dy, 4)
    return out


def _turn_x(els):
    """Elements turned a quarter about the block's vertical axis: a run along z to one along x."""
    fm = {"north": "east", "east": "south", "south": "west", "west": "north", "up": "up",
          "down": "down"}
    out = _copy(els)
    for e in out:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        e["from"], e["to"] = [16 - z1, y0, x0], [16 - z0, y1, x1]
        e["faces"] = {fm[k]: v for k, v in e["faces"].items()}
    return out


def _flip_z(els):
    out = _copy(els)
    for e in out:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        e["from"], e["to"] = [x0, y0, 16 - z1], [x1, y1, 16 - z0]
        e["faces"] = {{"north": "south", "south": "north"}.get(k, k): v
                      for k, v in e["faces"].items()}
    return out


TRAY_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 2, 0], "scale": [0.625] * 3},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5] * 3},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                              "scale": [0.375] * 3},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                              "scale": [0.4] * 3},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                             "scale": [0.4] * 3},
}


def tray(reg, names, kind):
    """A cable tray: its six part models (middle, arm, rail; bottom and top), an inventory
    model, and the multipart blockstate."""
    if kind == "ladder":
        t = {"m": C.T("ladder_black"), "particle": C.T("ladder_black"), "r": C.T("hanger_rod")}
        rail_h, top_lift = 2.0, 12.0
        middle = [E((2, 0.5, z), (14, 1.25, z + 0.75), "m", faces=("north", "south", "up", "down"))
                  for z in (3.625, 7.625, 11.625)]
        arm = [E((2, 0, 0), (2.75, rail_h, 2), "m", faces=("east", "west", "up", "down")),
               E((13.25, 0, 0), (14, rail_h, 2), "m", faces=("east", "west", "up", "down")),
               E((2.75, 0.5, 0), (13.25, 1.25, 0.375), "m", faces=("south", "up", "down"))]
        rail = [E((2, 0, 2), (14, rail_h, 2.75), "m", faces=("north", "south", "up", "down"))]
    else:
        t = {"m": C.T("wire_mesh"), "particle": C.T("galvanized"), "g": C.T("galvanized"),
             "r": C.T("hanger_rod")}
        rail_h, top_lift = 3.0, 11.0
        middle = [E((2, 0, 2), (14, 0.25, 14), "m", faces=("up", "down"))]
        arm = [E((2, 0, 0), (14, 0.25, 2), "m", faces=("up", "down")),
               E((2, 0, 0), (2.25, rail_h, 2), "m", faces=("east", "west")),
               E((13.75, 0, 0), (14, rail_h, 2), "m", faces=("east", "west")),
               E((2, rail_h - 0.25, 0), (2.25, rail_h, 2), "g", faces=("up",)),
               E((13.75, rail_h - 0.25, 0), (14, rail_h, 2), "g", faces=("up",))]
        rail = [E((2, 0, 2), (14, rail_h, 2.25), "m", faces=("north", "south")),
                E((2, rail_h - 0.25, 2), (14, rail_h, 2.25), "g", faces=("up",))]
    rods = [E((7.75, rail_h, 2.0), (8.25, 16 - top_lift, 2.5), "r",
              faces=("north", "south", "east", "west"))]
    models = {}
    for name, els in (("middle", middle), ("arm", arm), ("rail", rail)):
        models["%s_%s_bottom" % (reg, name)] = tc.model(t, els)
        lifted = _lift(els, top_lift) + (_lift(rods, top_lift) if name == "rail" else [])
        models["%s_%s_top" % (reg, name)] = tc.model(t, lifted)
    inv_t = dict(t, m=C.T("ladder_icon")) if kind == "ladder" else t
    inv = tc.model(inv_t, _turn_x(middle + arm + _flip_z(arm) + rail + _flip_z(rail)))
    inv["display"] = TRAY_DISPLAY
    models["%s_inventory" % reg] = inv
    multipart = []
    for top in ("false", "true"):
        half = "top" if top == "true" else "bottom"
        for axis, y in (("z", 0), ("x", 90)):
            a = {"model": C.M("%s_middle_%s" % (reg, half))}
            if y:
                a["y"] = y
            multipart.append({"when": {"axis": axis, "top": top}, "apply": a})
        for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            for prop, part in (("true", "arm"), ("false", "rail")):
                a = {"model": C.M("%s_%s_%s" % (reg, part, half))}
                if y:
                    a["y"] = y
                multipart.append({"when": {side: prop, "top": top}, "apply": a})
    state = {"variants": {"inventory": {"model": C.M("%s_inventory" % reg)}},
             "multipart": multipart}
    java = 'new BlockCableTray("%s", new double[]{%s}, new double[]{%s})' % (
        reg, ", ".join(tc.jnum(v) for v in (0, rail_h)),
        ", ".join(tc.jnum(v) for v in (top_lift, 16)))
    C.add(reg, java, names, models, state, tab=TAB)


tray("cable_tray_basket",
     tc.names("Wire Basket Cable Tray", "Gitterkabelrinne", "Bandeja Portacables de Rejilla",
              "Kabelränna av trådnät"), "basket")
tray("cable_tray_ladder",
     tc.names("Cable Ladder Tray", "Kabelleiter", "Bandeja Portacables de Escalera",
              "Kabelstege"), "ladder")


# The hanger extension (D12): threaded rod a block long, stacked under a ceiling so a hung tray
# can sit one or more blocks lower. Its rod is the trays' own (ROD_X, ROD_Z: the north rail's
# rod, turned to each side by the blockstate) carried the whole height of the block, with a
# coupling nut at its foot where it meets the rod below; BlockCableTrayHanger draws one on each
# side where the tray beneath it has a rail, so the rods run on unbroken into the tray's.
def hanger(reg, names):
    t = {"r": C.T("hanger_rod"), "particle": C.T("hanger_rod")}
    rod = [E((7.75, 0, 2.0), (8.25, 16, 2.5), "r", faces=("north", "south", "east", "west")),
           E((7.55, 0, 1.8), (8.45, 0.9, 2.7), "r")]
    models = {"%s_rod" % reg: tc.model(t, rod)}
    inv = tc.model(t, _turn_x(rod + _flip_z(rod)))
    inv["display"] = TRAY_DISPLAY
    models["%s_inventory" % reg] = inv
    multipart = []
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        a = {"model": C.M("%s_rod" % reg)}
        if y:
            a["y"] = y
        multipart.append({"when": {side: "true"}, "apply": a})
    state = {"variants": {"inventory": {"model": C.M("%s_inventory" % reg)}},
             "multipart": multipart}
    C.add(reg, 'new BlockCableTrayHanger("%s")' % reg, names, models, state, tab=TAB)


hanger("cable_tray_hanger",
       tc.names("Cable Tray Threaded Rod Hanger", "Gewindestangen-Abhängung für Kabelrinnen",
                "Colgador de Varilla Roscada para Bandeja Portacables",
                "Gängstångshängare för kabelränna"))

if __name__ == "__main__":
    sys.exit(C.main())
