#!/usr/bin/env python3
"""Every asset the Micafi wireless gear in the Technology - Networking & IT tab ships.

    python dev-env-utils/scripts/gen_technology_wireless.py
    python dev-env-utils/scripts/gen_technology_wireless.py --check
    python dev-env-utils/scripts/gen_technology_wireless.py --fragments   # tab lines to paste

Micafi is the house networking brand, an invented one, with its own mark drawn in
gen_technology_common.py. Every product name here is Micafi's own (Beam, Station, Sector,
Link 60); nothing copies a real maker's name, mark or livery.

What is here, in creative order, and the class that places each
(technology.network.*):

- **Indoor access points**: the ceiling AP (a white disc with a lit ring round the mark), a
  smaller Lite one, and the ceiling AP on a short drop pipe (BlockMicafiCeilingAp: on a ceiling
  facing down, on a floor facing up, from the face clicked, so placing one while flying never
  stands it on edge); the wall AP and the in-wall AP on its jack plate (BlockMicafiDevice).
- **Outdoor wireless**: the outdoor mesh AP; the Micafi Beam building bridge, a white disc with a
  lit rim on a ball-jointed wall arm, and the same bridge on a pole clamp; the Micafi Station
  CPE (a tall slim panel), a 120 degree sector antenna, a small and a large dish (the large one
  reaching three pixels past its block every way), and the Micafi Link 60, a square 60 GHz
  radome placed in pairs. Everything outdoor is BlockMicafiDevice.PoleFitted: its clamp is
  drawn three times, for the large, thin and pedestal poles (CsmPoleFit), the clamp's saddles
  moved back to each pole's skin by gen_pole_fit_models.fitted; on a wall the clamp is hidden
  in it.

Clicking changes the status light (MicafiLight): an access point's ring (blue, white, off, and
adopting, which flashes white and blue) or a radio's link light (green, blue, white, off). The
light is two bits of metadata, the blockstate swapping one texture: the AP face picture (a
square plane with the disc cut out of it, so a round face costs one quad) or a 16 px LED
swatch. Nothing ticks and there is no tile entity.

Writes models under models/block/technology/wireless and textures under
textures/blocks/technology/wireless.
"""
import math
import os
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_pole_fit_models as gpf  # noqa: E402
import gen_technology_common as tc  # noqa: E402

C = tc.catalogue("gen_technology_wireless.py", "wireless")
TAB = tc.NETWORKING
lsc = tc.lsc
Pic = tc.Pic
E = tc.E

WHITE = tc.WHITE_PLASTIC
MOUNT_GREY = (150, 154, 158)
CABLE_BLUE = (52, 96, 176)

RING_VALUES = ("blue", "white", "off", "adopting")
LINK_VALUES = ("green", "blue", "white", "off")
RING_COLOURS = {"blue": ((70, 110, 255), (170, 192, 255)),
                "white": ((252, 253, 255), (214, 222, 236)),
                "off": ((204, 207, 212), (220, 222, 225))}
LED_COLOURS = {"green": (70, 232, 110), "blue": (76, 130, 255), "white": (246, 249, 255),
               "off": (72, 76, 80)}
ADOPT_FRAMES = ("white", "blue")


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def cutout(img, rgb=WHITE):
    """Hard alpha (the cutout layer keeps a texel or drops it) and, under the clear texels, the
    face's own colour, so a mipmap does not darken the edge."""
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255) if a >= 128 else tuple(rgb[:3]) + (0,)
    return img


def ngon(cx, cy, r, n=8):
    """The corners of a regular n-gon (8 or 16) of inradius r, flats square to the axes: the
    outline of lsc._octagon's prism (n 8) or round_z's (n 16), so a face picture cut to it
    covers the prism's end exactly."""
    if n == 8:
        a = r * lsc.TAN_22_5
        return [(cx + r, cy - a), (cx + r, cy + a), (cx + a, cy + r), (cx - a, cy + r),
                (cx - r, cy + a), (cx - r, cy - a), (cx - a, cy - r), (cx + a, cy - r)]
    rc = r / math.cos(math.pi / n)
    return [(cx + rc * math.cos(math.pi / n + 2 * math.pi * k / n),
             cy + rc * math.sin(math.pi / n + 2 * math.pi * k / n)) for k in range(n)]


def octagon(cx, cy, r):
    return ngon(cx, cy, r, 8)


def _disc_shading(p, r=8.0, n=8):
    """A white round end lit from the upper left, as a disc: the n-gon, then a few nested
    discs, each a little lighter. The face picture of a round part is cut to the n-gon and is
    that part's only end face, so nothing lies coplanar with it."""
    p.poly(ngon(8, 8, r, n), tc.shade(WHITE, 0.9))
    p.disc(8, 8, r, tc.shade(WHITE, 0.93))
    p.disc(7.8, 7.8, r * 0.94, tc.shade(WHITE, 0.98))
    p.disc(7.5, 7.5, r * 0.8, tc.shade(WHITE, 1.02))
    p.disc(7.3, 7.3, r * 0.55, tc.shade(WHITE, 1.04))


def ap_face(colour):
    """An access point's face, the disc filling the picture: the ring at 0.39 of the radius,
    glowing, round the Micafi mark."""
    p = Pic(16, 16, 32, bg=(0, 0, 0, 0))
    _disc_shading(p)
    ring, glow = RING_COLOURS[colour]
    if colour != "off":
        p.ring(8, 8, 3.55, 0.9, glow)
    p.ring(8, 8, 3.25, 0.62, ring)
    if colour != "blue":
        # a white or dark ring on a white disc: edged in grey so it reads
        p.ring(8, 8, 3.62, 0.14, tc.shade(WHITE, 0.8))
        p.ring(8, 8, 2.92, 0.14, tc.shade(WHITE, 0.8))
    img = cutout(p.done())
    tc.micafi_mark(img, 13, 13, 6, (172, 176, 182))
    return img


for _c in RING_COLOURS:
    C.textures["ap_face_%s" % _c] = (lambda c=_c: ap_face(c))
tc.anim(C, "ap_face_adopting", len(ADOPT_FRAMES), lambda f: ap_face(ADOPT_FRAMES[f]), 10)

for _c, _rgb in LED_COLOURS.items():
    C.textures["led_%s" % _c] = (lambda rgb=_rgb: tc.fill(rgb, 16, 3, sum(rgb)))
tc.anim(C, "led_adopting", len(ADOPT_FRAMES),
        lambda f: tc.fill(LED_COLOURS[ADOPT_FRAMES[f]], 16, 3, 77 + f), 10)

C.textures["mount_grey"] = lambda: tc.fill(MOUNT_GREY, 16, 3, 5101)
C.textures["cable_blue"] = lambda: tc.fill(CABLE_BLUE, 16, 4, 5102)


MARK_GREY = (168, 172, 178)


def draw_mark(p, x, y, size, colour):
    """The Micafi mark drawn into a Pic before it is squeezed: (x, y) its top-left and size its
    width, in the face's units."""
    tc.micafi_mark(p.img, int(round(x * p.s)), int(round(y * p.s)), int(round(size * p.s)),
                   colour, k=2)


def pic_tex(name, w, h, draw, round_face=False, mark=None):
    """A static face picture; ``round_face`` cuts it to the disc; ``mark`` (x, y, size), in
    the face's units, draws the Micafi mark in grey before the picture is squeezed square, so it
    keeps its shape on a face that is not square."""
    def make():
        p = Pic(w, h, 32, bg=(0, 0, 0, 0) if round_face else WHITE)
        draw(p)
        if mark:
            draw_mark(p, mark[0], mark[1], mark[2], MARK_GREY)
        img = p.done()
        if round_face:
            img = cutout(img)
        return tc.grain(img, 1.2, zlib.crc32(name.encode("utf-8")) & 0xffff)
    C.textures[name] = make


pic_tex("beam_face", 16, 16, lambda p: _disc_shading(p, n=16), round_face=True,
        mark=(6.2, 6.6, 3.6))


def _dish_face(p):
    _disc_shading(p, n=16)
    p.ring(8, 8, 6.6, 0.18, tc.shade(WHITE, 0.88))


pic_tex("dish_face", 16, 16, _dish_face, round_face=True, mark=(6.0, 6.0, 4.0))


def _station_face(p):
    p.rect(0, 0, 4, 13, WHITE)
    p.frame(0, 0, 4, 13, tc.shade(WHITE, 0.88), 0.15)
    p.rect(0.3, 10.2, 3.7, 10.32, tc.shade(WHITE, 0.9))
    p.rect(0.3, 11.2, 3.7, 12.6, tc.shade(WHITE, 0.95))


pic_tex("station_face", 4, 13, _station_face, mark=(1.1, 1.0, 1.8))


def _sector_face(p):
    p.rect(0, 0, 5.5, 15, WHITE)
    p.rect(0, 0, 5.5, 0.6, tc.shade(WHITE, 0.8))
    p.rect(0, 14.4, 5.5, 15, tc.shade(WHITE, 0.8))
    for x in (1.3, 4.2):
        p.rect(x, 0.6, x + 0.1, 14.4, tc.shade(WHITE, 0.92))


pic_tex("sector_face", 5.5, 15, _sector_face, mark=(1.85, 12.2, 1.8))


def _airlink_face(p):
    p.rect(0, 0, 9, 9, tc.shade(WHITE, 0.88))
    p.rect(0.35, 0.35, 8.65, 8.65, tc.shade(WHITE, 0.95))
    p.rect(0.8, 0.8, 8.2, 8.2, WHITE)
    p.ring(4.5, 4.5, 2.9, 0.12, tc.shade(WHITE, 0.9))


pic_tex("airlink_face", 9, 9, _airlink_face, mark=(3.3, 3.3, 2.4))


def _inwall_plate(p):
    p.rect(0, 0, 6, 12, WHITE)
    p.frame(0, 0, 6, 12, tc.shade(WHITE, 0.86), 0.18)
    for i in range(3):
        x = 0.9 + i * 1.5
        p.rect(x - 0.1, 8.5, x + 1.3, 9.9, tc.shade(WHITE, 0.9))
        p.rect(x, 8.65, x + 1.2, 9.75, tc.PORT_BLACK)
        p.rect(x + 0.2, 8.72, x + 1.0, 8.88, tc.PIN_GOLD)
    p.disc(3, 11.3, 0.16, tc.shade(WHITE, 0.7))


pic_tex("inwall_plate", 6, 12, _inwall_plate)


def _inwall_body(p):
    p.rect(0, 0, 4.8, 6.4, WHITE)
    p.frame(0, 0, 4.8, 6.4, tc.shade(WHITE, 0.9), 0.15)


pic_tex("inwall_body", 4.8, 6.4, _inwall_body, mark=(1.5, 2.6, 1.8))


# ------------------------------------------------------------------------------------------
# Geometry helpers
# ------------------------------------------------------------------------------------------
def oct_y(cx, cz, r, y0, y1, t, top=True, bottom=True):
    """An upright octagon of inradius r about (cx, cz)."""
    return lsc._octagon("y", cx, cz, r, y0, y1, t, top or bottom, cap_front=bottom,
                        cap_back=top)


def oct_z(cx, cy, r, z0, z1, t, front=True, back=True):
    """An octagon of inradius r lying along z about (cx, cy); its front is north, at z0."""
    return lsc._octagon("z", cx, cy, r, z0, z1, t, front or back, cap_front=front,
                        cap_back=back)


TAN_11_25 = math.tan(math.pi / 16)


def round_z(cx, cy, r, z0, z1, t, front=True, back=True):
    """A regular 16-gon prism of inradius r lying along z about (cx, cy), front (north) at z0:
    lsc._octagon's pattern with eight rectangles, each 2r long and 2r tan(11.25) wide, turned
    0, 22.5, -22.5 and 45 degrees about z (the steps a model element may turn), whose long
    sides are the sixteen outside faces. A large dish or bridge read as a blocky octagon. Each
    rectangle draws its own ends, a hair apart along z so the coplanar caps do not fight; a
    round picture that ends the part replaces them (front=False), as it must be the part's only
    end face."""
    a = r * TAN_11_25
    faces = []
    if front:
        faces.append("north")
    if back:
        faces.append("south")
    els = []
    k = 0
    for ang in (0, 22.5, -22.5, 45):
        for h1, h2 in ((r, a), (a, r)):
            eps = 0.003 * k
            k += 1
            sides = ("east", "west") if h1 == r else ("up", "down")
            turn = ("z", ang, (cx, cy, z0)) if ang else None
            els.append(E((cx - h1, cy - h2, z0 - eps), (cx + h1, cy + h2, z1 + eps), t,
                         faces=sides + tuple(faces), turn=turn))
    return els


def plane_up(cx, cz, half, y, t):
    """A picture laid flat, facing up, 2 * half across, uv the whole picture."""
    return {"from": [cx - half, y, cz - half], "to": [cx + half, y, cz + half],
            "faces": {"up": {"texture": "#" + t, "uv": [0, 0, 16, 16]}}}


def plane_north(x0, y0, x1, y1, z, t, uv=(0, 0, 16, 16)):
    """A picture standing square to z, facing north."""
    return {"from": [x0, y0, z], "to": [x1, y1, z],
            "faces": {"north": {"texture": "#" + t, "uv": list(uv)}}}


def plane_down(x0, z0, x1, z1, y, t):
    return {"from": [x0, y, z0], "to": [x1, y, z1],
            "faces": {"down": {"texture": "#" + t, "uv": [0, 0, 16, 16]}}}


def clamp(y0, y1):
    """A pole clamp at the back of the block: a back plate on the block face and two saddles
    reaching two pixels on into the next block, to the large pole's skin. gen_pole_fit_models
    stretches the saddles for the thinner poles; on a wall they are inside it."""
    return [E((6.5, y0, 15), (9.5, y1, 16), "g", faces=("north", "east", "west", "up", "down")),
            E((7, y0 + 0.4, 15.5), (9, y0 + 1.4, 18), "g", faces=("east", "west", "up", "down")),
            E((7, y1 - 1.4, 15.5), (9, y1 - 0.4, 18), "g", faces=("east", "west", "up", "down"))]


def display(scale, y=0.0, fixed_z=0.0):
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, y, 0], "scale": [scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.3] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, fixed_z], "scale": [scale] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.4] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.4] * 3},
    }


def light_textures(prop, key):
    """The light variants: for each value of ``prop`` (ring or link), the texture its ``key``
    (the AP face, ring, or an LED swatch, led) takes."""
    values = RING_VALUES if prop == "ring" else LINK_VALUES
    name = "ap_face_%s" if key == "ring" else "led_%s"
    return {v: {"textures": {key: C.T(name % v)}} for v in values}


def add_device(reg, names, els, textures, prop, key, disp, pole=False, java_box=None):
    """A wall or pole device (BlockMicafiDevice, PoleFitted when ``pole``): its model (and the
    thin and pedestal pole fits), its facing blockstate with the light variants, and the tab
    line."""
    t = dict(textures)
    t.setdefault("particle", tc.T("white_plastic"))
    first = RING_VALUES[0] if prop == "ring" else LINK_VALUES[0]
    t[key] = C.T(("ap_face_%s" if key == "ring" else "led_%s") % first)
    m = tc.with_display(tc.model(t, els), disp)
    models = {reg: m}
    extra = {prop: light_textures(prop, key)}
    if pole:
        fits = {"large": {}}
        for value, suffix, delta in gpf.FITS:
            models[reg + suffix] = gpf.fitted(m, delta)
            fits[value] = {"model": C.M(reg + suffix)}
        extra["polefit"] = fits
    state = tc.facing_state(C.M(reg), extra)
    b = java_box or tc.extent(els)
    cls = "BlockMicafiDevice.PoleFitted" if pole else "BlockMicafiDevice"
    java = 'new %s("%s", %s, MicafiLight.%s)' % (cls, reg, tc.box_java(b), prop.upper())
    C.add(reg, java, names, models, state, tab=TAB)


def N(en, de, es, sv):
    return (en, de, es, sv)


W_T = {"w": tc.T("white_plastic"), "g": C.T("mount_grey"), "c": C.T("cable_blue")}


# ------------------------------------------------------------------------------------------
# Indoor access points
# ------------------------------------------------------------------------------------------
def ceiling_ap(reg, names, r, h, drop=0.0, scale=0.9):
    """A ceiling AP drawn facing up (standing on a floor); the blockstate turns it over to hang
    it under a ceiling. ``drop`` stands it on a pipe that long from a small ceiling flange."""
    els = []
    if drop:
        els += oct_y(8, 8, 1.6, 0, 0.4, "w")
        els += oct_y(8, 8, 0.6, 0.4, drop, "w", top=False, bottom=False)
    els += oct_y(8, 8, r, drop, drop + h, "w")
    rb = round(r * 0.92, 3)
    els += oct_y(8, 8, rb, drop + h, drop + h + 0.35, "w", top=False, bottom=False)
    els.append(plane_up(8, 8, rb, drop + h + 0.35, "ring"))
    t = dict(W_T, ring=C.T("ap_face_blue"), particle=tc.T("white_plastic"))
    m = tc.with_display(tc.model(t, els), display(scale, 2.0 if not drop else 0.0))
    state = {"forge_marker": 1, "defaults": {"model": C.M(reg)},
             "variants": {"facing": {"up": {}, "down": {"x": 180}}, "inventory": [{}],
                          "ring": light_textures("ring", "ring")}}
    b = tc.extent(els)
    C.add(reg, 'new BlockMicafiCeilingAp("%s", %s)' % (reg, tc.box_java(b)), names, {reg: m},
          state, tab=TAB)


ceiling_ap("micafi_ap_ceiling",
           N("Micafi Ceiling Access Point", "Micafi-Decken-Access-Point",
             "Punto de Acceso de Techo Micafi", "Micafi takmonterad accesspunkt"), 6.0, 1.25)
ceiling_ap("micafi_ap_ceiling_lite",
           N("Micafi Ceiling Access Point Lite", "Micafi-Decken-Access-Point Lite",
             "Punto de Acceso de Techo Micafi Lite", "Micafi takmonterad accesspunkt Lite"),
           4.0, 1.0, scale=1.1)
ceiling_ap("micafi_ap_ceiling_drop",
           N("Micafi Access Point on a Drop", "Micafi-Access-Point an Abhängung",
             "Punto de Acceso Micafi Suspendido", "Micafi accesspunkt på pendelrör"),
           6.0, 1.25, drop=8.0, scale=0.7)

# the wall AP: a slim white slab on the wall, the ring and mark on its face
add_device("micafi_ap_wall",
           N("Micafi Wall Access Point", "Micafi-Wand-Access-Point",
             "Punto de Acceso de Pared Micafi", "Micafi väggmonterad accesspunkt"),
           [E((4.5, 3, 14.6), (11.5, 13, 16), "w", faces=("east", "west", "up", "down")),
            E((4.5, 3, 14.6), (11.5, 13, 16), "w", faces=("north",)),
            E((4.85, 3.35, 14.25), (11.15, 12.65, 14.6), "w"),
            plane_north(5.1, 5.6, 10.9, 11.4, 14.22, "ring")],
           W_T, "ring", "ring", display(1.2, 0, -4))

# the in-wall AP: on a single-gang jack plate, three jacks below it, its light along its top
add_device("micafi_ap_in_wall",
           N("Micafi In-Wall Access Point", "Micafi-Unterputz-Access-Point",
             "Punto de Acceso Empotrado Micafi", "Micafi infälld accesspunkt"),
           [E((5, 2, 15.5), (11, 14, 16), "w", faces=("north", "east", "west", "up", "down"),
              per={"north": "plate"}, full=("north",)),
            E((5.6, 6.8, 14.1), (10.4, 13.2, 15.5), "w", per={"north": "body"},
              full=("north",)),
            plane_north(6.6, 12.35, 9.4, 12.7, 14.08, "led")],
           dict(W_T, plate=C.T("inwall_plate"), body=C.T("inwall_body")), "ring", "led",
           display(1.2, 0, -4))


# ------------------------------------------------------------------------------------------
# Outdoor wireless (pole-fitted)
# ------------------------------------------------------------------------------------------
# the outdoor mesh AP: a slim white cylinder on two arms, its light near the top
add_device("micafi_ap_outdoor_mesh",
           N("Micafi Outdoor Mesh Access Point", "Micafi-Outdoor-Mesh-Access-Point",
             "Punto de Acceso Mesh Exterior Micafi", "Micafi utomhus mesh-accesspunkt"),
           oct_y(8, 9.5, 1.6, 2.5, 13, "w", top=False, bottom=False)
           + oct_y(8, 9.5, 1.1, 13, 13.6, "w", bottom=False)
           + oct_y(8, 9.5, 1.1, 1.9, 2.5, "w", top=False)
           + [plane_north(7.6, 11.2, 8.4, 11.8, 7.88, "led"),
              E((7.4, 5, 11.1), (8.6, 6.4, 15), "g"),
              E((7.4, 9.4, 11.1), (8.6, 10.8, 15), "g")]
           + clamp(4, 12),
           W_T, "ring", "led", display(1.0, 0, -4), pole=True)


def beam_disc(cy=9.5):
    """The Micafi Beam's disc, facing north: a domed white front, the lit rim, the back shell
    and the hub its joint takes."""
    return (round_z(8, cy, 4, 3.0, 4.6, "w", front=False, back=False)
            + round_z(8, cy, 4.08, 4.6, 5.0, "led", back=False)
            + round_z(8, cy, 3.7, 5.0, 6.2, "w", front=False)
            + oct_z(8, cy, 1.5, 6.2, 7.4, "g", front=False)
            + [plane_north(4, cy - 4, 12, cy + 4, 3.0, "face")])


BEAM_T = dict(W_T, face=C.T("beam_face"))
add_device("micafi_beam_bridge",
           N("Micafi Beam Building Bridge", "Micafi Beam Gebäudebrücke",
             "Puente entre Edificios Micafi Beam", "Micafi Beam byggnadslänk"),
           beam_disc()
           + [E((7.1, 8.6, 7.4), (8.9, 10.4, 8.8), "g"),
              E((7.4, 7.0, 8.8), (8.6, 8.6, 12.0), "g"),
              E((7.2, 5.6, 11.4), (8.8, 7.4, 13.4), "g"),
              E((6.6, 4.6, 13.4), (9.4, 7.8, 15.3), "g"),
              E((5.4, 3.8, 15.3), (10.6, 8.6, 16), "g", faces=("north", "east", "west", "up",
                                                                 "down")),
              E((7.75, 1.4, 4.6), (8.25, 5.6, 5.1), "c"),
              E((7.75, 1.4, 5.1), (8.25, 1.9, 16), "c", faces=("east", "west", "up", "down"))],
           BEAM_T, "link", "led", display(1.1, 0, -4))
add_device("micafi_beam_bridge_pole_fitted",
           N("Micafi Beam Building Bridge (for Poles)", "Micafi Beam Gebäudebrücke (für Masten)",
             "Puente entre Edificios Micafi Beam (para Postes)", "Micafi Beam byggnadslänk (för stolpar)"),
           beam_disc()
           + [E((7.3, 8.8, 7.4), (8.7, 10.2, 15), "g"),
              E((7.75, 2.5, 4.6), (8.25, 5.6, 5.1), "c"),
              E((7.75, 2.5, 5.1), (8.25, 3.0, 15), "c", faces=("east", "west", "up", "down"))]
           + clamp(5.5, 13),
           BEAM_T, "link", "led", display(1.1, 0, -4), pole=True)

# the Micafi Station: a tall slim outdoor CPE panel, its link lights along the foot
add_device("micafi_station_outdoor",
           N("Micafi Station Outdoor CPE", "Micafi Station Außen-CPE",
             "CPE Exterior Micafi Station", "Micafi Station utomhus-CPE"),
           [E((6, 1.5, 9), (10, 14.5, 11.5), "w", per={"north": "face"}, full=("north",)),
            E((6.25, 14.5, 9.25), (9.75, 14.85, 11.25), "w", faces=("north", "south", "east",
                                                                      "west", "up")),
            plane_north(6.7, 2.3, 9.3, 2.6, 8.97, "led"),
            E((7, 4, 11.5), (9, 5, 15), "g"),
            E((7, 11, 11.5), (9, 12, 15), "g")]
           + clamp(3.5, 12.5),
           dict(W_T, face=C.T("station_face")), "link", "led", display(1.0, 0, -4), pole=True)

# a 120 degree sector antenna: a tall panel with its radio behind it
add_device("micafi_sector_antenna",
           N("Micafi Sector Antenna (120°)", "Micafi-Sektorantenne (120°)",
             "Antena Sectorial Micafi (120°)", "Micafi sektorantenn (120°)"),
           [E((5.25, 0.5, 7.5), (10.75, 15.5, 9.5), "w", per={"north": "face"}, full=("north",)),
            E((6.25, 2.5, 9.5), (9.75, 8.5, 12), "w", faces=("south", "east", "west", "up",
                                                             "down")),
            plane_north(7, 1.0, 9, 1.3, 7.47, "led"),
            E((7.4, 12, 9.5), (8.6, 13, 15), "g"),
            E((7.4, 4.5, 12), (8.6, 5.5, 15), "g")]
           + clamp(3.5, 14),
           dict(W_T, face=C.T("sector_face")), "link", "led", display(0.9, 0, -4), pole=True)


def dish(reg, names, r, z_front, cones, radio, arm_z0, clamp_y, scale):
    """A dish facing north about (8, 8): its white radome (the picture cut to a disc), the grey
    rim, the back stepping in as cones, the radio box (its link light underneath, where it is
    seen from below on a pole) and the arm to the clamp."""
    z = z_front
    els = round_z(8, 8, r, z, z + 1.2, "w", front=False, back=False)
    els.append(plane_north(8 - r, 8 - r, 8 + r, 8 + r, z, "face"))
    z += 1.2
    els += round_z(8, 8, round(r * 1.03, 3), z, z + 0.7, "g")
    z += 0.7
    for cr, dz in cones:
        els += round_z(8, 8, cr, z, z + dz, "g", front=False, back=True)
        z += dz
    h = radio
    els.append(E((8 - h, 8 - h, z), (8 + h, 8 + h, z + 3.2), "w", faces=("south", "east", "west",
                                                                           "up", "down")))
    els.append(plane_down(7.2, z + 0.8, 8.8, z + 2.0, 8 - h - 0.02, "led"))
    els.append(E((7.3, 7.3, arm_z0), (8.7, 8.7, 15.2), "g"))
    els += clamp(*clamp_y)
    add_device(reg, names, els, dict(W_T, face=C.T("dish_face")), "link", "led",
               display(scale, 0, -4), pole=True)


dish("micafi_dish_small",
     N("Micafi Dish (Small)", "Micafi-Richtfunkschüssel (klein)",
       "Antena Parabólica Micafi (Pequeña)", "Micafi parabolantenn (liten)"),
     5.0, 4.0, [(4.3, 1.1), (3.0, 1.0)], 1.8, 10.6, (4.5, 11.5), 1.0)
dish("micafi_dish_large",
     N("Micafi Dish (Large)", "Micafi-Richtfunkschüssel (groß)",
       "Antena Parabólica Micafi (Grande)", "Micafi parabolantenn (stor)"),
     11.0, 1.0, [(9.0, 1.7), (6.5, 1.8), (3.8, 1.6)], 2.4, 11.6, (4.0, 12.0), 0.55)

# the Micafi Link 60: a square 60 GHz radome, placed in pairs facing each other
add_device("micafi_airlink_60",
           N("Micafi Link 60 Bridge (60 GHz)", "Micafi Link 60 Richtfunkbrücke (60 GHz)",
             "Puente Micafi Link 60 (60 GHz)",
             "Micafi Link 60-länk (60 GHz)"),
           [E((3.5, 3.5, 5.6), (12.5, 12.5, 8.6), "w", per={"north": "face"}, full=("north",)),
            plane_north(7.2, 4.15, 8.8, 4.45, 5.57, "led"),
            E((5, 5, 8.6), (11, 11, 10.6), "g", faces=("south", "east", "west", "up", "down")),
            E((7.3, 7.3, 10.6), (8.7, 8.7, 15.2), "g")]
           + clamp(4.5, 11.5),
           dict(W_T, face=C.T("airlink_face")), "link", "led", display(1.0, 0, -4), pole=True)

if __name__ == "__main__":
    sys.exit(C.main())
