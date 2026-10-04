#!/usr/bin/env python3
"""Every asset the Transit tab's airport airside pieces ship: the airfield lights and signs, the
wind sock and beacon, the stand sign, ground equipment and the jet bridge.

    python dev-env-utils/scripts/gen_transit_airside.py
    python dev-env-utils/scripts/gen_transit_airside.py --check
    python dev-env-utils/scripts/gen_transit_airside.py --fragments   # tab lines to paste

The terminal's pieces are gen_transit_airport.py's; this is the other side of the gate. There are
no aircraft of any kind, so nothing here docks, taxis or moves: the jet bridge is a corridor to
walk out along, the ground equipment stands where it is parked. What is here, and the class that
places each (package transit.airport unless another is named):

- **Airfield lights** (BlockAirfieldLight): elevated runway and taxiway edge lights, the
  runway threshold light (green to the approach, red to the runway), inset runway and taxiway
  centreline lights and the stop bar, the approach light bar, the airport beacon and the
  obstruction-lit wind sock and antenna mast. Each is lit or not (`lit`, in its metadata, with
  `powered` beside it) and glows by a lit lens texture and its block light, never a renderer.
  Lights of one circuit within eight blocks of each other switch together, from a click or a
  change of redstone power at any of them, so one lever lights a runway.
- **Airfield signs** (BlockAirfieldSign): taxiway location (yellow on black), direction (black
  on yellow, the arrow either side), runway holding position (white on red) and distance
  remaining (white on black) signs, lit with the taxiway lights. What a sign says is its
  `legend` (and a direction sign's `arrow`), kept in the platform signs' tile entity; the
  blockstate swaps the face's texture.
- **The airfield mast** (transit.platform.BlockPlatformColumn): a slim galvanised pole that
  stacks, for the approach light bar, the wind sock, the beacon and the stand sign to stand on.
- **The stand sign** (BlockStandSign): the stand's letter and number on a post, clicked like the
  gate sign, whose cell textures it borrows.
- **Ground equipment** (Roads' streetscape.BlockUtilityBox, so it settles onto a road surface and
  a piece two blocks long is placed and broken whole): wheel chocks, a ground power unit, a
  baggage tug, a covered baggage cart and towable air stairs. Cones are Roads' work-zone cones.
- **The jet bridge** (BlockJetBridge): tunnel lengths two blocks wide that join into one
  corridor, an octagonal rotunda three blocks across that the tunnel meets, and a cab with its
  canopy and a safety bar; the drive leg and the rotunda's column (BlockPlatformColumn) stack
  under them.

Every model faces north, as the terminal's do: a light's lens, a sign's legend, a vehicle's nose
and the jet bridge's aircraft end look north. The element helpers are gen_transit_platforms.py's,
and the stand sign's cells gen_transit_airport.py's.
"""
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_transit_platforms as gp  # noqa: E402
import gen_transit_airport as ga  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_airside.py", "transit/airside", "transit/airside", assets=ASSETS)
TAB = "CsmTabTransit"

B, win, flip = gp.B, gp.win, gp.flip
ALL, SIDES = gp.ALL, gp.SIDES
names_of, box_java, gui_display = gp.names_of, gp.box_java, gp.gui_display
FACINGS = gp.FACINGS

WHITE = gp.WHITE
BLACK = gp.BLACK
YELLOW = (250, 196, 24)
RED = (196, 30, 34)
GALV = (150, 156, 160)
GSE_YELLOW = (236, 178, 22)
GSE_WHITE = (226, 228, 226)
JB_SKIN = (200, 204, 208)
JB_BAND = (22, 44, 92)


def grain(size, colour, amount, seed, alpha=255):
    return lc.fill(colour, size, amount, seed, alpha)


def rect(img, x0, y0, x1, y1, colour):
    lc.rect(img, int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1)), colour)


def noshade(els):
    """Marks elements unshaded, as a lamp's lens is: lit the same from every side."""
    for e in els:
        e["shade"] = False
    return els


def post(cx, cz, r, y0, y1, tex, top=True, bottom=True):
    return gp.octagon_y(cx, cz, r, y0, y1, tex, top=top, bottom=bottom)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
# lit and unlit lens colours: (lit, unlit)
LENSES = {
    "white": ((255, 244, 208), (118, 116, 106)),
    "blue": ((78, 136, 255), (32, 46, 92)),
    "green": ((88, 255, 128), (26, 84, 44)),
    "red": ((255, 64, 52), (92, 26, 24)),
}


def lens(colour, lit, seed):
    """A lamp's lens: lit, a bright glass with a hot core; unlit, dim tinted glass."""
    img = grain(16, colour, 3 if lit else 5, seed)
    if lit:
        lc.disc(img, 8, 8, 4, lc.clamp(tuple(v * 0.6 + 255 * 0.4 for v in colour)))
        lc.disc(img, 8, 8, 1.6, lc.clamp(tuple(v * 0.3 + 255 * 0.7 for v in colour)))
    else:
        lc.disc(img, 6, 6, 2, lc.shade(colour, 1.35))
    return img


def inset_ring():
    img = grain(16, (126, 130, 134), 5, 811)
    lc.frame(img, 0, 0, 16, 16, (96, 98, 102))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        rect(img, x, y, x + 1, y + 1, (70, 72, 76))
    return img


# --- the airfield signs --------------------------------------------------------------------
SIGN_TEX = 128
SIGN_K = 8                # texels a unit on a sign's face
SIGN_X0, SIGN_X1 = 1.0, 15.0
SIGN_Y0, SIGN_Y1 = 5.0, 13.0
SIGN_Z0, SIGN_Z1 = 6.8, 9.2
FACE_W = int((SIGN_X1 - SIGN_X0) * SIGN_K)     # 112
FACE_H = int((SIGN_Y1 - SIGN_Y0) * SIGN_K)     # 64
CELL_W = FACE_W // 2                            # a direction sign's half: 56

LOCATIONS = "ABCDEFGH"
HOLDING = ["4-22", "9-27", "13-31", "18-36", "ILS"]
DISTANCES = [str(n) for n in range(1, 10)]
LEGENDS = 9               # BlockAirfieldSign.LEGEND runs 1 to 9 whatever the sign


def sign_location(ch):
    img = Image.new("RGBA", (SIGN_TEX, SIGN_TEX), BLACK + (255,))
    lc.frame(img, 5, 5, FACE_W - 5, FACE_H - 5, YELLOW, 3)
    lc.draw_text_centred(img, ch, FACE_W / 2.0, (FACE_H - 45) // 2, YELLOW, 9)
    return img


def sign_direction_letter(ch):
    img = Image.new("RGBA", (64, 64), YELLOW + (255,))
    lc.draw_text_centred(img, ch, CELL_W / 2.0, (FACE_H - 45) // 2, BLACK, 9)
    return img


def sign_direction_arrow(right):
    img = Image.new("RGBA", (64, 64), YELLOW + (255,))
    gp.arrow(img, 8, 18, CELL_W - 16, 28, BLACK, right=right)
    return img


def outlined_text(img, text, cx, y, colour, outline, scale):
    for dx in (-2, -1, 0, 1, 2):
        for dy in (-2, -1, 0, 1, 2):
            if dx or dy:
                x = int(round(cx - lc.text_width(text, scale) / 2)) + dx
                lc.draw_text(img, text, x, y + dy, outline, scale)
    lc.draw_text_centred(img, text, cx, y, colour, scale)


def sign_holding(text):
    img = Image.new("RGBA", (SIGN_TEX, SIGN_TEX), RED + (255,))
    outlined_text(img, text, FACE_W / 2.0, (FACE_H - 25) // 2, WHITE, BLACK, 5)
    return img


def sign_distance(text):
    img = Image.new("RGBA", (SIGN_TEX, SIGN_TEX), BLACK + (255,))
    lc.draw_text_centred(img, text, FACE_W / 2.0, (FACE_H - 45) // 2, WHITE, 9)
    return img


# --- the beacon ----------------------------------------------------------------------------
BEACON_FRAMES = 8


def beacon_strip(phase, emissive=False):
    """One side of the beacon's lens, frame by frame: it flashes white when the beam sweeps
    past it and green half a turn later, dark otherwise. The eight sides are a frame apart, so
    the flash runs round the lens as a rotating beacon's does."""
    img = Image.new("RGBA", (16, 16 * BEACON_FRAMES), (0, 0, 0, 0))
    for f in range(BEACON_FRAMES):
        step = (f - phase) % BEACON_FRAMES
        colour = {0: (255, 252, 236), 4: (96, 255, 140)}.get(step)
        if colour is None:
            if not emissive:
                img.paste(grain(16, (34, 58, 44), 3, 820 + f), (0, 16 * f))
            continue
        tile = grain(16, colour, 3, 830 + f)
        lc.disc(tile, 8, 8, 4, (255, 255, 255))
        img.paste(tile, (0, 16 * f))
    return img


# --- the wind sock -------------------------------------------------------------------------
def sock(colour, seed):
    img = grain(16, colour, 4, seed)
    for y in (3, 11):
        rect(img, 0, y, 16, y + 1, lc.shade(colour, 0.88))
    return img


# --- ground equipment ----------------------------------------------------------------------
def diamond_plate():
    img = grain(16, (150, 154, 158), 3, 841)
    for y in range(0, 16, 4):
        for x in range(0, 16, 4):
            ox = 2 if (y // 4) % 2 else 0
            rect(img, x + ox, y + 1, x + ox + 2, y + 2, (196, 200, 204))
    return img


def tug_grille():
    img = grain(16, GSE_YELLOW, 3, 842)
    rect(img, 2, 3, 14, 12, (30, 30, 32))
    for y in range(4, 12, 2):
        rect(img, 2, y, 14, y + 1, (70, 70, 74))
    rect(img, 1, 13, 4, 15, (255, 236, 170))
    rect(img, 12, 13, 15, 15, (255, 236, 170))
    return img


def gpu_side():
    img = grain(32, (214, 216, 212), 3, 843)
    lc.frame(img, 2, 3, 30, 29, lc.shade((214, 216, 212), 0.78))
    for y in range(7, 17, 2):
        rect(img, 5, y, 16, y + 1, (86, 88, 90))
    rect(img, 20, 6, 21, 25, lc.shade((214, 216, 212), 0.7))
    rect(img, 0, 27, 32, 30, GSE_YELLOW)
    rect(img, 18, 14, 20, 17, (60, 62, 64))
    return img


def gpu_panel():
    img = grain(32, (214, 216, 212), 3, 844)
    rect(img, 5, 5, 27, 22, (48, 50, 54))
    rect(img, 7, 7, 17, 12, (40, 110, 70))
    lc.draw_text(img, "400HZ", 7, 14, (230, 230, 230))
    lc.disc(img, 22, 10, 2.5, (40, 180, 70))
    lc.disc(img, 22, 17, 2.5, (200, 40, 36))
    rect(img, 0, 27, 32, 30, GSE_YELLOW)
    return img


def canvas():
    img = grain(16, (34, 70, 140), 4, 845)
    for x in (0, 8):
        rect(img, x, 0, x + 1, 16, (26, 54, 110))
    return img


def suitcase(colour, seed):
    img = grain(16, colour, 4, seed)
    lc.frame(img, 0, 0, 16, 16, lc.shade(colour, 0.7))
    rect(img, 6, 1, 10, 2, (30, 30, 32))
    return img


def hazard():
    img = Image.new("RGBA", (16, 16), YELLOW + (255,))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if ((x + y) // 4) % 2:
                px[x, y] = (24, 24, 26, 255)
    return img


# --- the jet bridge -------------------------------------------------------------------------
JB_K = 2                  # texels a unit on the jet bridge's walls
JB_WALL_H = 29            # floor 1.5 to ceiling 30.5
WALL_WIN = win(16 * JB_K, JB_WALL_H * JB_K, 64)


def jb_wall(outside, cab=False):
    """A wall 16 long by 29 tall, 2 texels a unit: panels with a seam at the block's end, a
    band near the floor and a window at eye level (a bigger one on the cab)."""
    w, h = 16 * JB_K, JB_WALL_H * JB_K
    base = JB_SKIN if outside else (222, 214, 196)
    img = grain(64, base, 3, 850 if outside else 851)
    if outside:
        for y in range(0, h, 6):
            rect(img, 0, y, w, y + 1, lc.shade(base, 0.93))
        rect(img, 0, h - 14, w, h - 8, JB_BAND)
        rect(img, 0, 0, 1, h, lc.shade(base, 0.72))
    else:
        rect(img, 0, h - 8, w, h, (70, 72, 78))
        rect(img, 0, h - 26, w, h - 25, (150, 146, 136))
    top, bottom = (h - 2 * 25, h - 2 * 12) if not cab else (h - 2 * 27, h - 2 * 9)
    x0, x1 = (8, 24) if not cab else (3, 29)
    rect(img, x0 - 1, top - 1, x1 + 1, bottom + 1, (60, 62, 66))
    rect(img, x0, top, x1, bottom, (40, 56, 70))
    rect(img, x0 + 1, top + 1, x0 + 4, top + 3, (110, 130, 146))
    return img


def jb_floor():
    img = grain(64, (70, 78, 96), 6, 852)
    return img


def jb_ceiling():
    """The ceiling at 1 texel a unit: a light strip down the middle of the corridor's 32."""
    img = grain(64, (232, 232, 226), 2, 853)
    rect(img, 13, 0, 19, 64, (255, 250, 228))
    return img


def jb_bellows():
    img = grain(16, (28, 28, 30), 3, 854)
    for y in range(0, 16, 3):
        rect(img, 0, y, 16, y + 1, (60, 60, 64))
    return img


def jb_console():
    img = grain(16, (60, 62, 68), 2, 855)
    rect(img, 2, 3, 14, 9, (20, 30, 40))
    for x in (3, 7, 11):
        rect(img, x, 11, x + 2, 13, (220, 180, 40))
    return img


def register_textures():
    tex = {
        "can": lambda: grain(16, (176, 180, 184), 3, 801),
        "frangible": lambda: grain(16, YELLOW, 3, 802),
        "inset_ring": inset_ring,
        "lamp_body": lambda: grain(16, (60, 64, 68), 2, 803),
        "sign_back": lambda: grain(16, (40, 42, 46), 2, 804),
        "sign_case": lambda: grain(16, (168, 172, 176), 3, 805),
        "mast": lambda: grain(16, GALV, 4, 806),
        "plinth": lambda: grain(16, (150, 150, 144), 6, 807),
        "beacon_housing": lambda: grain(16, (40, 76, 52), 3, 808),
        "beacon_off": lambda: grain(16, (34, 58, 44), 3, 809),
        "sock_orange": lambda: sock((240, 110, 24), 810),
        "sock_white": lambda: sock((236, 236, 230), 812),
        "sock_inside": lambda: grain(16, (120, 60, 20), 3, 813),
        "dish": lambda: grain(16, (230, 232, 230), 2, 814),
        "gse_yellow": lambda: grain(16, GSE_YELLOW, 3, 815),
        "gse_white": lambda: grain(16, GSE_WHITE, 3, 816),
        "gse_steel": lambda: grain(16, (110, 114, 118), 3, 817),
        "gse_black": lambda: grain(16, (32, 32, 34), 2, 818),
        "tyre": lambda: grain(16, (26, 26, 28), 3, 819),
        "amber": lambda: grain(16, (230, 140, 20), 3, 821),
        "diamond_plate": diamond_plate,
        "tug_grille": tug_grille,
        "gpu_side": gpu_side,
        "gpu_panel": gpu_panel,
        "canvas": canvas,
        "bag_red": lambda: suitcase((160, 36, 40), 822),
        "bag_blue": lambda: suitcase((40, 70, 150), 823),
        "bag_grey": lambda: suitcase((96, 98, 104), 824),
        "chock": lambda: grain(16, YELLOW, 4, 825),
        "rope": lambda: grain(16, (40, 40, 44), 3, 826),
        "hazard": hazard,
        "jb_skin": lambda: jb_wall(True),
        "jb_inner": lambda: jb_wall(False),
        "jb_cab": lambda: jb_wall(True, cab=True),
        "jb_cab_inner": lambda: jb_wall(False, cab=True),
        "jb_floor": jb_floor,
        "jb_ceiling": jb_ceiling,
        "jb_roof": lambda: grain(64, (120, 124, 128), 4, 856),
        "jb_under": lambda: grain(64, (70, 72, 76), 3, 857),
        "jb_frame": lambda: grain(16, (54, 56, 60), 2, 858),
        "jb_bellows": jb_bellows,
        "jb_console": jb_console,
        "jb_leg": lambda: grain(16, (160, 164, 168), 3, 859),
        "jb_column": lambda: grain(16, (176, 176, 170), 4, 860),
    }
    for i, (colour, (lit, unlit)) in enumerate(sorted(LENSES.items())):
        tex["lens_%s_on" % colour] = (lambda c=lit, i=i: lens(c, True, 870 + i))
        tex["lens_%s_on_e" % colour] = (lambda c=lit, i=i: lens(c, True, 870 + i))
        tex["lens_%s_off" % colour] = (lambda c=unlit, i=i: lens(c, False, 880 + i))
    for ch in LOCATIONS:
        tex["sign_location_" + ch.lower()] = (lambda ch=ch: sign_location(ch))
        tex["sign_direction_" + ch.lower()] = (lambda ch=ch: sign_direction_letter(ch))
    tex["sign_arrow_left"] = lambda: sign_direction_arrow(False)
    tex["sign_arrow_right"] = lambda: sign_direction_arrow(True)
    for i, text in enumerate(HOLDING, 1):
        tex["sign_holding_%d" % i] = (lambda t=text: sign_holding(t))
    for i, text in enumerate(DISTANCES, 1):
        tex["sign_distance_%d" % i] = (lambda t=text: sign_distance(t))
    for p in range(BEACON_FRAMES):
        tex["beacon_%d" % p] = (lambda p=p: beacon_strip(p))
        tex["beacon_%d_e" % p] = (lambda p=p: beacon_strip(p, emissive=True))
    for name, draw in tex.items():
        C.texture(name)(draw)
    for p in range(BEACON_FRAMES):
        for suffix in ("", "_e"):
            C.extra["textures/blocks/transit/airside/beacon_%d%s.png.mcmeta" % (p, suffix)] = \
                '{\n  "animation": {\n    "frametime": 4\n  }\n}\n'


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
def light_state(model, lens_keys, colour, extra=None):
    """A facing blockstate whose `lit` swaps each lens texture for the lit one. `powered` only
    remembers the redstone and draws nothing, so it has no variants: the block's state mapper
    leaves it out of the model locations (BlockAirfieldLight.propertiesNoModelReads)."""
    lit = {"true": {"textures": {k: C.T("lens_%s_on" % c) for k, c in lens_keys.items()}},
           "false": {"textures": {k: C.T("lens_%s_off" % c) for k, c in lens_keys.items()}}}
    variants = {"lit": lit}
    if extra:
        variants.update(extra)
    return lc.facing_state(C.M(model), variants)


def light(reg, java, names, els, tex, lens_keys, display, extra=None):
    m = lc.model(tex, els, ao=False)
    m["display"] = display
    C.add(reg, java, names, {reg: m}, light_state(reg, lens_keys, None, extra), tab=TAB)


def light_java(reg, box, circuit, level):
    return 'new BlockAirfieldLight("%s", %s, "%s", %d)' % (reg, box_java(box), circuit, level)


def multipart(parts, prefix, textures, facings=True, ao=True):
    models = {}
    rules = []
    for name, cond, els in parts:
        mname = "%s_%s" % (prefix, name)
        models[mname] = lc.model(textures, els, ao=ao)
        for f, y in (FACINGS if facings else ((None, 0),)):
            when = {"facing": f} if f else {}
            for k, v in cond.items():
                when[k] = ("true" if v else "false") if isinstance(v, bool) else str(v)
            apply = {"model": C.M(mname)}
            if y:
                apply["y"] = y
            rules.append({"when": when, "apply": apply} if when else {"apply": apply})
    return models, {"multipart": rules}


def item_of(parts, textures, keep=lambda cond: all(v is False for v in cond.values()),
            display=None):
    els = []
    for name, cond, e in parts:
        if keep(cond):
            els += e
    m = lc.model(textures, els, ao=False)
    if display:
        m["display"] = display
    return m


# ------------------------------------------------------------------------------------------
# Airfield lights
# ------------------------------------------------------------------------------------------
def elevated(lens_parts):
    """An elevated edge light: a base can, a yellow frangible coupling, a stalk and the lens
    under a cap."""
    return (post(8, 8, 2.2, 0, 2.4, "can")
            + post(8, 8, 0.9, 2.4, 3.6, "frangible", top=False, bottom=False)
            + post(8, 8, 0.6, 3.6, 9, "can", top=False, bottom=False)
            + lens_parts
            + post(8, 8, 1.3, 12.2, 12.8, "can", bottom=False))


def airfield_lights():
    box = (5.8, 0, 5.8, 10.2, 12.8, 10.2)
    for reg, colour, circuit, level, names in (
            ("airport_runway_edge_light", "white", "runway", 12,
             names_of("Runway Edge Light", "Pistenrandfeuer", "Luz de Borde de Pista",
                      "Banlampa")),
            ("airport_taxiway_edge_light", "blue", "taxiway", 10,
             names_of("Taxiway Edge Light", "Rollwegrandfeuer", "Luz de Borde de Calle de Rodaje",
                      "Taxibanans kantljus"))):
        els = elevated(noshade(post(8, 8, 1.7, 9, 12.2, "lens", top=False, bottom=False)))
        tex = {"can": C.T("can"), "frangible": C.T("frangible"),
               "lens": C.T("lens_%s_off" % colour), "particle": C.T("can")}
        light(reg, light_java(reg, box, circuit, level), names, els, tex, {"lens": colour},
              gui_display(1.1, 1.0))

    # the threshold light: green to the approach (the model's north), red back up the runway
    reg = "airport_runway_threshold_light"
    lens = noshade([B((6.3, 9, 6.3), (9.7, 12.2, 8), "green", ("north", "east", "west")),
                    B((6.3, 9, 8), (9.7, 12.2, 9.7), "red", ("south", "east", "west"))])
    els = elevated(lens)
    tex = {"can": C.T("can"), "frangible": C.T("frangible"), "green": C.T("lens_green_off"),
           "red": C.T("lens_red_off"), "particle": C.T("can")}
    light(reg, light_java(reg, box, "runway", 12),
          names_of("Runway Threshold Light", "Schwellenfeuer", "Luz de Umbral de Pista",
                   "Tröskelljus"),
          els, tex, {"green": "green", "red": "red"}, gui_display(1.1, 1.0))

    # inset lights, flush in the pavement: a steel ring and a raised lens
    for reg, colour, circuit, level, names in (
            ("airport_runway_centreline_light", "white", "runway", 11,
             names_of("Runway Centreline Light", "Pistenmittellinienfeuer",
                      "Luz de Eje de Pista", "Banans mittlinjeljus")),
            ("airport_taxiway_centreline_light", "green", "taxiway", 10,
             names_of("Taxiway Centreline Light", "Rollwegmittellinienfeuer",
                      "Luz de Eje de Calle de Rodaje", "Taxibanans mittlinjeljus")),
            ("airport_stop_bar_light", "red", "taxiway", 10,
             names_of("Stop Bar Light", "Haltebalkenfeuer", "Luz de Barra de Parada",
                      "Stopprampsljus"))):
        els = (post(8, 8, 4.2, 0, 0.6, "ring", bottom=False)
               + noshade(post(8, 8, 2.0, 0.6, 1.3, "lens", bottom=False)))
        tex = {"ring": C.T("inset_ring"), "lens": C.T("lens_%s_off" % colour),
               "particle": C.T("inset_ring")}
        light(reg, light_java(reg, (3.8, 0, 3.8, 12.2, 1.3, 12.2), circuit, level), names, els,
              tex, {"lens": colour}, gui_display(1.4, 3.0))

    # the approach light bar: five lamps on a crossbar two blocks wide, facing the approach
    reg = "airport_approach_light_bar"
    els = post(8, 8, 1.2, 0, 10, "mast", bottom=False)
    els += [B((-8, 10, 7), (24, 11.4, 9), "mast", ALL, uv={
        "north": [0, 4, 16, 5.4], "south": [0, 4, 16, 5.4], "up": [0, 7, 16, 9],
        "down": [0, 7, 16, 9]})]
    lamps = []
    for x in (-4.8, 1.6, 8.0, 14.4, 20.8):
        els += [B((x - 0.4, 11.4, 7.4), (x + 0.4, 12.2, 8.6), "body", SIDES),
                B((x - 1.6, 12.2, 6.2), (x + 1.6, 15.4, 9.4), "body",
                  ("south", "east", "west", "up", "down"))]
        lamps.append(B((x - 1.6, 12.2, 6.2), (x + 1.6, 15.4, 6.2), "lens", ("north",),
                       uv={"north": [2, 2, 14, 14]}))
    els += noshade(lamps)
    tex = {"mast": C.T("mast"), "body": C.T("lamp_body"), "lens": C.T("lens_white_off"),
           "particle": C.T("mast")}
    light(reg, light_java(reg, (-8, 0, 6.2, 24, 15.4, 9.4), "runway", 15),
          names_of("Approach Light Bar", "Anflugfeuerbalken", "Barra de Luces de Aproximación",
                   "Inflygningsljusbom"),
          els, tex, {"lens": "white"}, gui_display(0.5, 0.0))

    # the airport beacon: a lens band whose eight sides flash in turn, white then green
    reg = "airport_beacon"
    els = [B((3, 0, 3), (13, 1, 13), "housing", SIDES + ("up", "down"))] \
        + post(8, 8, 1.4, 1, 2.2, "housing", top=False, bottom=False) \
        + post(8, 8, 3.8, 2.2, 3.2, "housing") + post(8, 8, 3.8, 9.2, 10.2, "housing") \
        + post(8, 8, 2.6, 10.2, 11.2, "housing", bottom=False) \
        + post(8, 8, 0.6, 11.2, 12.4, "housing", bottom=False)
    band = noshade(post(8, 8, 3.4, 3.2, 9.2, "b0", top=False, bottom=False))
    # sides by compass bearing: an element's east/west side is east/west, turned 45 degrees
    # (about y, positive) it looks north-east/south-west; north/south turned look north-west/
    # south-east
    bearing = {(False, "north"): 0, (False, "east"): 2, (False, "south"): 4, (False, "west"): 6,
               (True, "east"): 1, (True, "south"): 3, (True, "west"): 5, (True, "north"): 7}
    for e in band:
        turned = "rotation" in e
        for f, face in e["faces"].items():
            face["texture"] = "#b%d" % bearing[(turned, f)]
            face["uv"] = [0, 0, 16, 16]
    els += band
    tex = {"housing": C.T("beacon_housing"), "particle": C.T("beacon_housing")}
    tex.update({"b%d" % p: C.T("beacon_off") for p in range(BEACON_FRAMES)})
    m = lc.model(tex, els, ao=False)
    m["display"] = gui_display(0.9, 0.0)
    state = lc.facing_state(C.M(reg), {
        "lit": {"true": {"textures": {"b%d" % p: C.T("beacon_%d" % p)
                                      for p in range(BEACON_FRAMES)}},
                "false": {"textures": {"b%d" % p: C.T("beacon_off")
                                       for p in range(BEACON_FRAMES)}}}})
    C.add(reg, light_java(reg, (3, 0, 3, 13, 12.4, 13), "beacon", 15),
          names_of("Airport Beacon", "Flughafen-Leuchtfeuer", "Faro de Aeródromo",
                   "Flygplatsfyr"),
          {reg: m}, state, tab=TAB)


# ------------------------------------------------------------------------------------------
# The wind sock and the antenna mast (obstruction lit)
# ------------------------------------------------------------------------------------------
def wind_sock():
    """A wind sock on its pivot, blowing north: a square hoop at the mouth and five tapering
    bands, orange and white, drooping a little; a red obstruction light on the pivot."""
    reg = "airport_wind_sock"
    els = post(8, 8, 1.2, 0, 12, "mast", bottom=False)
    els += post(8, 8, 1.7, 12, 14.4, "frame", bottom=True, top=True)
    els += [B((7.6, 12.6, 5.4), (8.4, 13.4, 8), "frame", ("east", "west", "up", "down"))]
    # the hoop, a square round the mouth at z 5..5.8
    cy, r0 = 12.4, 4.0
    els += [B((8 - r0 - 0.6, cy - r0 - 0.6, 4.8), (8 + r0 + 0.6, cy - r0, 5.6), "frame"),
            B((8 - r0 - 0.6, cy + r0, 4.8), (8 + r0 + 0.6, cy + r0 + 0.6, 5.6), "frame"),
            B((8 - r0 - 0.6, cy - r0, 4.8), (8 - r0, cy + r0, 5.6), "frame"),
            B((8 + r0, cy - r0, 4.8), (8 + r0 + 0.6, cy + r0, 5.6), "frame")]
    # five bands, from the mouth (z 5) out to the tip (z -15)
    for i in range(5):
        z1 = 5.0 - 4.18 * i
        z0 = z1 - 4.18
        r = r0 - 0.46 * (i + 0.5)
        y = cy - 0.45 * (i + 0.5)
        colour = "orange" if i % 2 == 0 else "white"
        seg = lc._octagon("z", 8, y, r, z0, z1, colour, i in (0, 4),
                          cap_front=(i == 4), cap_back=(i == 0))
        for e in seg:
            if i == 0 and "south" in e["faces"]:
                e["faces"]["south"]["texture"] = "#inside"
        els += seg
    els += noshade(post(8, 8, 1.0, 14.4, 16, "lens", bottom=False))
    tex = {"mast": C.T("mast"), "frame": C.T("mast"), "orange": C.T("sock_orange"),
           "white": C.T("sock_white"), "inside": C.T("sock_inside"),
           "lens": C.T("lens_red_off"), "particle": C.T("sock_orange")}
    light(reg, light_java(reg, (3.8, 0, -16, 12.2, 16, 8), "obstruction", 9),
          names_of("Wind Sock", "Windsack", "Manga de Viento", "Vindstrut"),
          els, tex, {"lens": "red"}, gui_display(0.55, 0.0, rot=(30, 135, 0)))

    reg = "airport_antenna_mast"
    els = [B((4.5, 0, 4.5), (11.5, 0.8, 11.5), "mast", SIDES + ("up",))]
    els += post(8, 8, 0.8, 0.8, 15, "mast", bottom=False, top=True)
    els += [B((2, 6, 7.6), (14, 6.6, 8.4), "mast", ALL),
            B((7.6, 10, 2), (8.4, 10.6, 14), "mast", ALL),
            B((3.6, 0.8, 3.6), (4.2, 13, 4.2), "dish", SIDES + ("up",)),
            B((11.8, 0.8, 11.8), (12.4, 11, 12.4), "dish", SIDES + ("up",)),
            B((8.8, 3.2, 7.6), (10, 4.4, 8.4), "mast", ALL)]
    els += gp.octagon_z(10, 3.8, 2.4, 3.2, 3.8, "dish", front=True, back=True)
    els += noshade(post(8, 8, 0.9, 15, 17, "lens", bottom=False))
    tex = {"mast": C.T("mast"), "dish": C.T("dish"), "lens": C.T("lens_red_off"),
           "particle": C.T("mast")}
    light(reg, light_java(reg, (2, 0, 2, 14, 17, 14), "obstruction", 9),
          names_of("Antenna Mast", "Antennenmast", "Mástil de Antenas", "Antennmast"),
          els, tex, {"lens": "red"}, gui_display(0.8, 0.0))


# ------------------------------------------------------------------------------------------
# Airfield signs
# ------------------------------------------------------------------------------------------
def sign_frame():
    """The sign's case on two frangible legs, without its face."""
    els = []
    for x in (3.5, 12.5):
        els += post(x, 8, 0.6, 0, 1, "case", bottom=False, top=False)
        els += post(x, 8, 0.75, 1, 1.8, "frangible", bottom=False, top=False)
        els += post(x, 8, 0.6, 1.8, SIGN_Y0, "case", bottom=False, top=False)
    els.append(B((SIGN_X0, SIGN_Y0, SIGN_Z0), (SIGN_X1, SIGN_Y1, SIGN_Z1), "case",
                 ("south", "east", "west", "up", "down"), per={"south": "back"}))
    return els


SIGN_BOX = (SIGN_X0, 0, SIGN_Z0, SIGN_X1, SIGN_Y1, SIGN_Z1)


def sign_java(reg, labels, arrows):
    """BlockAirfieldSign's line: what each legend reads, for the action bar, in the order of
    the blockstate's legend textures."""
    return 'new BlockAirfieldSign("%s", %s, new String[]{%s}, %s)' % (
        reg, box_java(SIGN_BOX), ", ".join('"%s"' % t for t in labels),
        "true" if arrows else "false")


def airfield_signs():
    base_tex = {"case": C.T("sign_case"), "frangible": C.T("frangible"),
                "back": C.T("sign_back"), "particle": C.T("sign_back")}
    face_uv = win(FACE_W, FACE_H, SIGN_TEX)
    for reg, prefix, labels, texts, names in (
            ("airport_taxiway_location_sign", "sign_location_",
             [c.lower() for c in LOCATIONS], list(LOCATIONS),
             names_of("Taxiway Location Sign", "Rollweg-Positionsschild",
                      "Letrero de Posición de Calle de Rodaje", "Taxibanans lägesskylt")),
            ("airport_runway_holding_sign", "sign_holding_",
             [str(i) for i in range(1, len(HOLDING) + 1)], HOLDING,
             names_of("Runway Holding Position Sign", "Rollhalteort-Schild",
                      "Letrero de Punto de Espera de Pista", "Skylt för väntläge")),
            ("airport_runway_distance_sign", "sign_distance_",
             [str(i) for i in range(1, len(DISTANCES) + 1)], DISTANCES,
             names_of("Runway Distance Remaining Sign", "Restlängenschild",
                      "Letrero de Distancia Restante", "Skylt för återstående banlängd"))):
        els = sign_frame() + [B((SIGN_X0, SIGN_Y0, SIGN_Z0), (SIGN_X1, SIGN_Y1, SIGN_Z0),
                                "face", ("north",), uv={"north": face_uv})]
        tex = dict(base_tex, face=C.T(prefix + labels[0]))
        m = lc.model(tex, els)
        m["display"] = gui_display(0.8, 0.0)
        legend = {str(v): {"textures": {"face": C.T(prefix + labels[min(v, len(labels)) - 1])}}
                  for v in range(1, LEGENDS + 1)}
        state = lc.facing_state(C.M(reg), {
            "legend": legend})
        C.add(reg, sign_java(reg, texts, False), names, {reg: m}, state, tab=TAB)

    # the direction sign: a letter cell and an arrow cell, the arrow on the side it points
    reg = "airport_taxiway_direction_sign"
    cell_uv = win(CELL_W, FACE_H, 64)
    mid = (SIGN_X0 + SIGN_X1) / 2
    models = {}
    for side in ("left", "right"):
        # seen from the north, high x is on the left
        left_tex, right_tex = ("arrow", "legend") if side == "left" else ("legend", "arrow")
        els = sign_frame() + [
            B((mid, SIGN_Y0, SIGN_Z0), (SIGN_X1, SIGN_Y1, SIGN_Z0), left_tex, ("north",),
              uv={"north": cell_uv}),
            B((SIGN_X0, SIGN_Y0, SIGN_Z0), (mid, SIGN_Y1, SIGN_Z0), right_tex, ("north",),
              uv={"north": cell_uv})]
        tex = dict(base_tex, legend=C.T("sign_direction_a"), arrow=C.T("sign_arrow_" + side))
        m = lc.model(tex, els)
        m["display"] = gui_display(0.8, 0.0)
        models["%s_%s" % (reg, side)] = m
    legend = {str(v): {"textures": {"legend": C.T("sign_direction_" + LOCATIONS[
        min(v, len(LOCATIONS)) - 1].lower())}} for v in range(1, LEGENDS + 1)}
    state = lc.facing_state(C.M(reg + "_left"), {
        "legend": legend,
        "arrow": {"left": {"model": C.M(reg + "_left")}, "right": {"model": C.M(reg + "_right")}}})
    C.add(reg, sign_java(reg, list(LOCATIONS), True),
          names_of("Taxiway Direction Sign", "Rollweg-Richtungsschild",
                   "Letrero de Dirección de Calle de Rodaje", "Taxibanans riktningsskylt"),
          models, state, tab=TAB)


# ------------------------------------------------------------------------------------------
# The airfield mast and the stand sign
# ------------------------------------------------------------------------------------------
MAST_R = 1.2


def column(reg, java, names, shaft, base, cap, tex, display):
    parts = [("shaft", {}, shaft), ("base", {"down": False}, base), ("cap", {"up": False}, cap)]
    models, state = multipart(parts, reg, tex)
    C.add(reg, java, names, models, state, item=item_of(parts, tex, display=display), tab=TAB)


def airfield_mast():
    reg = "airport_airfield_mast"
    tex = {"mast": C.T("mast"), "plinth": C.T("plinth"), "particle": C.T("mast")}
    column(reg, 'new BlockPlatformColumn("%s", %s)' % (reg, box_java((6.5, 0, 6.5, 9.5, 16,
                                                                      9.5))),
           names_of("Airfield Mast", "Flugfeldmast", "Mástil de Aeródromo", "Flygfältsmast"),
           post(8, 8, MAST_R, 0, 16, "mast", top=False, bottom=False),
           [B((4.5, 0, 4.5), (11.5, 1.2, 11.5), "plinth", SIDES + ("up",))]
           + post(8, 8, 1.8, 1.2, 2.2, "mast", bottom=False),
           post(8, 8, 1.5, 15.2, 16, "mast", bottom=False), tex, gui_display(0.9, 0.0))


def stand_sign(large=False):
    """The stand's letter and number, twice the gate sign's size, on a post as tall as the
    airfield mast is thick, so it stands on one. Cells are the gate sign's textures, laid out as
    the gate sign lays them out (the back mirrored so it reads the same).

    `large` draws it twice as big again, three blocks across, for an apron seen from a terminal
    built at a large scale: the panel's corners are the furthest a JSON element may reach
    (-16 and 32), and the post grows to stand it on the mast."""
    reg = "airport_stand_sign_large" if large else "airport_stand_sign"
    f = 2.0 if large else 1.0
    x0, x1 = 8 - 12.0 * f, 8 + 12.0 * f
    y1 = 15.0 if not large else 32.0
    y0 = y1 - 10.0 * f
    z0, z1 = 7.2, 8.8
    lw = ga.GATE_LETTER_W * 2 * f
    k = ga.GATE_K / 2.0 / f
    letter_uv = [0, 0, lw * k * 16.0 / 64, (y1 - y0) * k * 16.0 / 64]
    number_uv = [0, 0, (x1 - x0 - lw) * k * 16.0 / 64, (y1 - y0) * k * 16.0 / 64]
    fx = x1 - lw          # the front's letter is on its left: high x
    lx = x0 + lw          # the back's on the other side
    els = [B((x0, y0, z0), (x1, y1, z1), "edge", ("east", "west", "up", "down"),
             uv={"up": [0, 7, 16, 8.6], "down": [0, 7, 16, 8.6], "east": [7, 1, 8.6, 11],
                 "west": [7, 1, 8.6, 11]}),
           B((fx, y0, z0), (x1, y1, z0), "letter", ("north",), uv={"north": letter_uv}),
           B((x0, y0, z0), (fx, y1, z0), "number", ("north",), uv={"north": number_uv}),
           B((x0, y0, z1), (lx, y1, z1), "letter", ("south",), uv={"south": letter_uv}),
           B((lx, y0, z1), (x1, y1, z1), "number", ("south",), uv={"south": number_uv})]
    els += post(8, 8, MAST_R * f, 0, y0, "steel", top=False, bottom=True)
    tex = {"edge": ga.C.T("graphite"), "letter": ga.C.T("gate_letter_a"),
           "number": ga.C.T("gate_number_1"), "steel": C.T("mast"),
           "particle": ga.C.T("gate_label")}
    extra = {"letter": {ch.lower(): {"textures": {"letter": ga.C.T("gate_letter_" + ch.lower())}}
                        for ch in ga.GATE_LETTERS},
             "number": {str(n): {"textures": {"number": ga.C.T("gate_number_%d" % n)}}
                        for n in range(1, ga.GATE_NUMBERS + 1)}}
    m = lc.model(tex, els)
    m["display"] = gui_display(0.27 if large else 0.5, 0.0)
    if large:
        names = names_of("Large Stand Sign", "Großes Standplatzschild",
                         "Letrero de Puesto de Estacionamiento Grande",
                         "Stor Uppställningsplatsskylt")
    else:
        names = names_of("Stand Sign", "Standplatzschild", "Letrero de Puesto de Estacionamiento",
                         "Uppställningsplatsskylt")
    C.add(reg, 'new BlockStandSign("%s", %s)' % (reg, box_java((x0, 0, 7, x1, y1, 9))),
          names, {reg: m}, lc.facing_state(C.M(reg), extra), tab=TAB)


# ------------------------------------------------------------------------------------------
# Ground equipment (Roads' BlockUtilityBox: settles, two-block pieces placed whole)
# ------------------------------------------------------------------------------------------
def gse(reg, els, tex, width, depth, height, unit, names, display):
    m = lc.model(tex, els)
    m["display"] = display
    java = ('new BlockUtilityBox("%s", new UtilityBoxSpec(%d, %d, %d, new AxisAlignedBB(%s), '
            'null))' % (reg, width, depth, height, ", ".join("%g" % v for v in unit)))
    C.add(reg, java, names, {reg: m}, lc.facing_state(C.M(reg)), tab=TAB)


def ground_equipment():
    # wheel chocks: a pair of yellow wedges on a rope
    els = []
    # each chock a square prism turned 45 about x with its lower half in the ground: a
    # triangular wedge, ridge up, 3.2 tall and 6.4 long
    h = 3.2
    for x0 in (2.0, 10.0):
        els += [B((x0, -h / math.sqrt(2), 8 - h / math.sqrt(2)),
                  (x0 + 4, h / math.sqrt(2), 8 + h / math.sqrt(2)), "chock", ALL,
                  uv={f: [0, 0, 16, 16] for f in ("east", "west")},
                  rot=("x", 45, (x0, 0, 8)))]
    els += [B((6, 0.4, 7.7), (10, 0.9, 8.3), "rope", ("up", "north", "south", "down"))]
    gse("airport_wheel_chocks", els, {"chock": C.T("chock"), "rope": C.T("rope"),
                                      "particle": C.T("chock")},
        1, 1, 1, (0.125, 0, 0.28, 0.875, 0.25, 0.72),
        names_of("Wheel Chocks", "Bremskeile", "Calzos", "Hjulklossar"), gui_display(1.1, 2.0))

    # the ground power unit: a trailer with louvred sides, its control panel at the back, a
    # cable coiled on its side and a towbar at the front
    els = [B((1.5, 3.2, 3), (14.5, 5, 29), "steel", ALL),
           B((1.5, 5, 3), (14.5, 19, 29), "body", ALL,
             per={"north": "panel_front", "south": "panel", "up": "top"},
             uv={"east": [0, 0, 16, 7], "west": [0, 0, 16, 7], "south": [0, 0, 16, 16],
                 "north": [0, 0, 16, 16], "up": [0, 0, 6.5, 13]}),
           B((3, 19, 22), (4.2, 22, 23.2), "black", SIDES + ("up",)),
           B((7.3, 3.6, -4), (8.7, 4.6, 3), "steel", ("east", "west", "up", "down", "north")),
           B((6.8, 3.2, -5.2), (9.2, 4.8, -3.8), "steel")]
    els += lc.pipe_x(10, 12, 3.2, 14.5, 16, "cable")
    for cz in (6, 26):
        els += lc.pipe_x(2.6, cz, 2.6, 0.6, 2.6, "tyre") + lc.pipe_x(2.6, cz, 2.6, 13.4, 15.4,
                                                                    "tyre")
    gse("airport_ground_power_unit", els,
        {"steel": C.T("gse_steel"), "body": C.T("gpu_side"), "panel": C.T("gpu_panel"),
         "panel_front": C.T("gse_white"), "top": C.T("gse_white"), "black": C.T("gse_black"),
         "cable": C.T("gse_black"), "tyre": C.T("tyre"), "particle": C.T("gse_white")},
        1, 2, 2, (0, 0, 0, 1, 1.4, 2),
        names_of("Ground Power Unit", "Bodenstromaggregat", "Grupo Eléctrico de Tierra",
                 "Markströmsaggregat"), gui_display(0.5, 0.0))

    # the baggage tug: a low tractor with a grille, a seat under a canopy, a tow hitch at the
    # back and an amber beacon on the roof
    els = [B((1.5, 3, 1), (14.5, 7, 31), "paint", ALL),
           B((1, 2.4, 0), (15, 5.6, 1.2), "black"),
           B((2, 7, 1.2), (14, 12, 11), "paint", ALL, per={"north": "grille"},
             uv={"north": [0, 0, 16, 16]}),
           B((2, 7, 11), (14, 15, 12), "paint", ALL),
           B((2, 7, 22), (14, 13, 31), "paint", ALL),
           B((5, 7, 16.5), (11, 10, 21), "seat"),
           B((5, 10, 20), (11, 16, 21.5), "seat"),
           B((7.5, 12, 12.5), (8.5, 15, 13.5), "black", SIDES),
           B((5.8, 15, 12), (10.2, 15.6, 14), "black"),
           B((7, 3.6, 31), (9, 5.4, 32), "black")]
    for x in (2.0, 13.2):
        els += [B((x, 12, 11.2), (x + 0.8, 28, 12), "paint", SIDES),
                B((x, 13, 29.2), (x + 0.8, 28, 30), "paint", SIDES)]
    els += [B((1.5, 28, 10.4), (14.5, 29, 30.8), "paint", ALL, uv={"up": [0, 0, 13, 16],
                                                                 "down": [0, 0, 13, 16]}),
            B((7, 29, 26), (9, 31, 28), "amber", SIDES + ("up",))]
    for cz in (6, 25):
        els += lc.pipe_x(3.2, cz, 3.2, 0.5, 3.5, "tyre") + lc.pipe_x(3.2, cz, 3.2, 12.5, 15.5,
                                                                    "tyre")
    gse("airport_baggage_tug", els,
        {"paint": C.T("gse_yellow"), "black": C.T("gse_black"), "grille": C.T("tug_grille"),
         "seat": C.T("gse_black"), "amber": C.T("amber"), "tyre": C.T("tyre"),
         "particle": C.T("gse_yellow")},
        1, 2, 2, (0, 0, 0, 1, 1.9, 2),
        names_of("Baggage Tug", "Gepäckschlepper", "Tractor de Equipajes", "Bagagetraktor"),
        gui_display(0.5, 0.0))

    # the covered baggage cart: a deck on four wheels under a canvas roof, the curtain down on
    # one side and rolled up on the other, bags aboard, a towbar at the front
    els = [B((1, 4, 1), (15, 6, 31), "steel", ALL),
           B((1.2, 6, 1.2), (14.8, 6.6, 30.8), "steel", ("up",), per={"up": "deck"},
             uv={"up": [0, 0, 16, 16]}),
           B((7.3, 4.4, -5), (8.7, 5.4, 1), "steel", ("east", "west", "up", "down", "north")),
           B((6.8, 4, -6.2), (9.2, 5.6, -4.8), "steel")]
    for x, z in ((1, 1), (14.2, 1), (1, 30.2), (14.2, 30.2)):
        els.append(B((x, 6.6, z), (x + 0.8, 26, z + 0.8), "steel", SIDES))
    els += [B((0.6, 26, 0.6), (15.4, 27.2, 31.4), "canvas", ALL,
              uv={"up": [0, 0, 14.8, 16], "down": [0, 0, 14.8, 16]}),
            B((0.8, 8, 1.8), (1.3, 26, 30.2), "canvas", ("east", "west")),
            B((14.6, 23.4, 1.8), (15.6, 26, 30.2), "canvas", ("east", "west", "down")),
            B((1.8, 8, 1), (14.2, 26, 1.6), "canvas", ("north", "south")),
            B((1.8, 8, 30.4), (14.2, 26, 31), "canvas", ("north", "south"))]
    for x0, y0, z0, x1, y1, z1, t in ((2.5, 6.6, 3, 9, 11.6, 12, "bag_red"),
                                      (9.5, 6.6, 3, 14, 13.6, 10, "bag_grey"),
                                      (2.5, 6.6, 13, 8, 13.6, 22, "bag_blue"),
                                      (8.5, 6.6, 12, 14, 10.6, 20, "bag_red"),
                                      (3, 6.6, 23, 13.5, 11.6, 29, "bag_grey"),
                                      (4, 11.6, 23.5, 12, 15.6, 28.5, "bag_blue")):
        els.append(B((x0, y0, z0), (x1, y1, z1), t))
    for cz in (4, 28):
        els += lc.pipe_x(2.2, cz, 2.2, 1, 3, "tyre") + lc.pipe_x(2.2, cz, 2.2, 13, 15, "tyre")
    gse("airport_baggage_cart", els,
        {"steel": C.T("gse_steel"), "deck": C.T("diamond_plate"), "canvas": C.T("canvas"),
         "bag_red": C.T("bag_red"), "bag_blue": C.T("bag_blue"), "bag_grey": C.T("bag_grey"),
         "tyre": C.T("tyre"), "particle": C.T("canvas")},
        1, 2, 2, (0, 0, 0, 1, 1.7, 2),
        names_of("Baggage Cart", "Gepäckanhänger", "Carro de Equipajes", "Bagagevagn (Släp)"),
        gui_display(0.5, 0.0))

    # towable air stairs: a platform at the front (north), seven steps down at 45 degrees to
    # the back, handrails both sides
    top, foot = 18.0, 4.0
    els = [B((1, 3, 2), (15, 5.5, 30), "steel", ALL),
           B((1, top - 1, 0.5), (15, top, 8), "white", ALL, per={"up": "tread"},
             uv={"up": [0, 0, 14, 7.5]})]
    for x in (1.5, 13.3):
        els += [B((x, 5.5, 2), (x + 1.2, top - 1, 3.2), "white", SIDES),
                B((x, 5.5, 6.4), (x + 1.2, top - 1, 7.6), "white", SIDES)]
    steps = 6
    run = (top - foot) / steps
    for i in range(steps):
        z0 = 8 + i * run
        y = top - (i + 1) * run
        els.append(B((2.6, y, z0), (13.4, y + 0.8, z0 + run + 0.4), "white", ALL,
                     per={"up": "tread"}))
    # stringers and rails in lengths short enough to stay within an element's limits once
    # turned 45 degrees about x (down towards the back)
    drop = top - foot
    seg = drop * math.sqrt(2) / 3
    for x0, x1 in ((1.8, 2.6), (13.4, 14.2)):
        for j in range(3):
            cz = 8 + drop * (j + 0.5) / 3
            cy = top - drop * (j + 0.5) / 3
            for dy, t, h in ((-1.0, "white", 1.6), (12.6, "rail", 0.7)):
                els.append(B((x0 if t == "white" else x0 + 0.1, cy + dy - h / 2, cz - seg / 2),
                             (x1 if t == "white" else x1 - 0.1, cy + dy + h / 2, cz + seg / 2),
                             t, ("east", "west", "up", "down"), rot=("x", 45, (8, cy + dy, cz))))
        # the platform's side rails and posts
        els += [B((x0, top, 0.8), (x1, top + 13, 1.6), "rail", SIDES),
                B((x0, top, 7.6), (x1, top + 13, 8.4), "rail", SIDES),
                B((x0 + 0.1, top + 12.3, 0.8), (x1 - 0.1, top + 13, 8.4), "rail",
                  ("east", "west", "up", "down")),
                B((x0 + 0.1, top + 6, 1.6), (x1 - 0.1, top + 6.6, 7.6), "rail",
                  ("east", "west", "up", "down"))]
    for cz in (5, 27):
        els += lc.pipe_x(2.4, cz, 2.4, 0.6, 2.6, "tyre") + lc.pipe_x(2.4, cz, 2.4, 13.4, 15.4,
                                                                    "tyre")
    gse("airport_air_stairs", els,
        {"steel": C.T("gse_steel"), "white": C.T("gse_white"), "tread": C.T("diamond_plate"),
         "rail": C.T("gse_yellow"), "tyre": C.T("tyre"), "particle": C.T("gse_white")},
        1, 2, 2, (0, 0, 0, 1, 1.95, 2),
        names_of("Air Stairs", "Fluggasttreppe", "Escalerilla de Embarque", "Flygplanstrappa"),
        gui_display(0.5, 0.0))


# ------------------------------------------------------------------------------------------
# The jet bridge
# ------------------------------------------------------------------------------------------
# BlockJetBridge: the corridor's section, in sixteenths, centred on the block's middle (x 8)
JB_X0, JB_X1 = -8.0, 24.0        # the tunnel's outside
JB_WT = 1.5                      # its walls' thickness
JB_FLOOR = 1.5                   # the floor's top
JB_CEIL = 30.5                   # the ceiling
JB_ROOF = 31.6                   # the roof's top
CAB_X0, CAB_X1 = -10.0, 26.0     # the cab's outside
ROT = 22.0                       # the rotunda's half width, from the block's middle
ROT_C = 6.0                      # its chamfered corners


def floor_uv(w, d):
    """A floor or ceiling face w x d units at 1 texel a unit on a 64 texture (the carpet,
    ceiling and roof textures repeat nothing across a block, so a face may take any window)."""
    return [0, 0, round(w * 16.0 / 64, 4), round(d * 16.0 / 64, 4)]


def slab(x0, x1, z0, z1, y0, y1, up, down, sides="frame", faces=("up", "down")):
    w, d = x1 - x0, z1 - z0
    return B((x0, y0, z0), (x1, y1, z1), sides, faces, per={"up": up, "down": down},
             uv={"up": floor_uv(w, d), "down": floor_uv(w, d)})


def wall_x(x0, x1, z0, z1, outer, inner, outside_west):
    """A wall along z between x0 and x1: its outside face west or east, its window of the
    wall texture as long as the wall (a block's length holds one window)."""
    faces = {"west": outer, "east": inner} if outside_west else {"east": outer, "west": inner}
    uv = win(min(64, (z1 - z0) * JB_K), JB_WALL_H * JB_K, 64)
    return B((x0, JB_FLOOR, z0), (x1, JB_CEIL, z1), outer, ("east", "west"), per=faces,
             uv={"east": uv, "west": uv})


def tunnel_section(z0, z1, x0=JB_X0, x1=JB_X1, skin="skin", inner="inner"):
    """Floor, walls and roof of a length of corridor from z0 to z1."""
    els = [slab(x0, x1, z0, z1, 0, JB_FLOOR, "floor", "under", faces=("up", "down", "east",
                                                                      "west")),
           wall_x(x0, x0 + JB_WT, z0, z1, skin, inner, True),
           wall_x(x1 - JB_WT, x1, z0, z1, skin, inner, False),
           slab(x0, x1, z0, z1, JB_CEIL, JB_ROOF, "roof", "ceiling",
                faces=("up", "down", "east", "west"))]
    for e in els:
        for f in ("east", "west"):
            if f in e["faces"] and e["faces"][f]["texture"] == "#frame":
                e["faces"][f]["uv"] = [0, 0, 16, 1.5]
    return els


def end_ring(z0, z1, x0=JB_X0, x1=JB_X1):
    """The frame round an open end of the corridor, standing a little proud of it."""
    o = 0.6
    return [B((x0 - o, -0.4, z0), (x0 + 2, 32, z1), "frame"),
            B((x1 - 2, -0.4, z0), (x1 + o, 32, z1), "frame"),
            B((x0 + 2, JB_CEIL - 0.8, z0), (x1 - 2, 32, z1), "frame",
              ("north", "south", "up", "down"), uv={"up": [0, 0, 16, 1.6],
                                                     "down": [0, 0, 16, 1.6],
                                                     "north": [0, 0, 16, 2.3],
                                                     "south": [0, 0, 16, 2.3]}),
            B((x0 + 2, -0.4, z0), (x1 - 2, JB_FLOOR + 0.3, z1), "frame",
              ("north", "south", "up", "down"), uv={"up": [0, 0, 16, 1.6],
                                                     "down": [0, 0, 16, 1.6],
                                                     "north": [0, 0, 16, 2.2],
                                                     "south": [0, 0, 16, 2.2]})]


JB_TEX = {"floor": C.T("jb_floor"), "under": C.T("jb_under"), "skin": C.T("jb_skin"),
          "inner": C.T("jb_inner"), "roof": C.T("jb_roof"), "ceiling": C.T("jb_ceiling"),
          "frame": C.T("jb_frame"), "particle": C.T("jb_skin")}


def jet_bridge():
    # the tunnel: one block of corridor, a frame at each end that nothing continues from
    reg = "airport_jet_bridge_tunnel"
    parts = [("body", {}, tunnel_section(0, 16)),
             ("end_ahead", {"ahead": False}, end_ring(-0.6, 1.0)),
             ("end_behind", {"behind": False}, end_ring(15.0, 16.6))]
    models, state = multipart(parts, reg, JB_TEX, ao=False)
    C.add(reg, 'new BlockJetBridge("%s", BlockJetBridge.Kind.TUNNEL)' % reg,
          names_of("Jet Bridge (Tunnel)", "Fluggastbrücke (Tunnel)",
                   "Pasarela de Embarque (Túnel)", "Flygbrygga (Tunnel)"),
          models, state, item=item_of(parts, JB_TEX, display=gui_display(0.32, 0.0)), tab=TAB)

    # the cab: wider, with big windows, a console, the canopy's bellows round the open front,
    # a hazard-striped bumper and a safety bar across it
    reg = "airport_jet_bridge_cab"
    tex = CAB_TEX
    body, front = cab_elements()
    parts = [("body", {}, body + front),
             ("end_behind", {"behind": False}, end_ring(15.0, 16.6, CAB_X0, CAB_X1))]
    models, state = multipart(parts, reg, tex, ao=False)
    C.add(reg, 'new BlockJetBridge("%s", BlockJetBridge.Kind.CAB)' % reg,
          names_of("Jet Bridge (Cab)", "Fluggastbrücke (Kabine)",
                   "Pasarela de Embarque (Cabina)", "Flygbrygga (Hytt)"),
          models, state, item=item_of(parts, tex, display=gui_display(0.3, 0.0)), tab=TAB)
    jet_bridge_rest()


CAB_TEX = dict(JB_TEX, cab=C.T("jb_cab"), cab_inner=C.T("jb_cab_inner"),
               bellows=C.T("jb_bellows"), hazard=C.T("hazard"), console=C.T("jb_console"),
               rail=C.T("gse_yellow"))


def cab_elements():
    """The cab's corridor and console (`body`), and its open front: the canopy's bellows, the
    hazard-striped bumper and the safety bar (`front`)."""
    body = tunnel_section(0, 16, CAB_X0, CAB_X1, "cab", "cab_inner")
    # where the narrower tunnel meets the cab's back: the step between the two walls
    body += [B((CAB_X0 + JB_WT, JB_FLOOR, 15.2), (JB_X0 + JB_WT, JB_CEIL, 16), "frame",
               ("north",), uv={"north": [0, 0, 3, 16]}),
             B((JB_X1 - JB_WT, JB_FLOOR, 15.2), (CAB_X1 - JB_WT, JB_CEIL, 16), "frame",
               ("north",), uv={"north": [0, 0, 3, 16]}),
             B((-7.6, JB_FLOOR, 1.2), (-2.4, 11, 4.6), "frame", ALL, per={"up": "console"},
               uv={"up": [0, 0, 16, 16]})]
    front = []
    for i, (inset, z1) in enumerate(((0.0, 0.0), (0.6, -1.8), (1.2, -3.6))):
        z0 = z1 - 1.8
        x0, x1 = CAB_X0 - 0.6 + inset, CAB_X1 + 0.6 - inset
        top = 32 - inset
        front += [B((x0, 0, z0), (x0 + 2.2, top, z1), "bellows", ALL,
                    uv={"north": [0, 0, 2.2, 16], "south": [0, 0, 2.2, 16]}),
                  B((x1 - 2.2, 0, z0), (x1, top, z1), "bellows", ALL,
                    uv={"north": [0, 0, 2.2, 16], "south": [0, 0, 2.2, 16]}),
                  B((x0 + 2.2, top - 2.2, z0), (x1 - 2.2, top, z1), "bellows",
                    ("north", "south", "up", "down"),
                    uv={f: [0, 0, 16, 2.2] for f in ("north", "south", "up", "down")})]
    front += [slab(-7.8, 23.8, -5.4, 0, 0, JB_FLOOR, "hazard", "under",
                   faces=("up", "down", "north")),
              B((-7.8, 17, -4.8), (23.8, 18.2, -3.8), "rail", ALL,
                uv={f: [0, 0, 16, 1.2] for f in ("north", "south", "up", "down")}),
              B((-7.8, 9, -4.8), (23.8, 10.2, -3.8), "rail", ALL,
                uv={f: [0, 0, 16, 1.2] for f in ("north", "south", "up", "down")})]
    body[0]["faces"]["up"]["uv"] = floor_uv(CAB_X1 - CAB_X0, 16)
    for e in front:
        if e["faces"].get("up", {}).get("texture") == "#hazard":
            e["faces"]["up"]["uv"] = [0, 0, 16, 2.7]
            e["faces"]["north"]["uv"] = [0, 0, 16, 0.75]
    return body, front


def jet_bridge_rest():
    # the rotunda: an octagonal room three blocks across on the block's middle, its two open
    # sides (north and south) the corridor's width, each with a collar out to the edge of the
    # three blocks, where the next tunnel meets it
    reg = "airport_jet_bridge_rotunda"
    lo, hi = 8 - ROT, 8 + ROT          # -14, 30
    a0, a1 = JB_X0, JB_X1               # the straight sides' ends: -8, 24
    els = []
    for y0, y1, up, down in ((0, JB_FLOOR, "floor", "under"), (JB_CEIL, JB_ROOF, "roof",
                                                                  "ceiling")):
        els += [slab(a0, a1, lo, hi, y0, y1, up, down),
                slab(lo, a0, a0, a1, y0, y1, up, down),
                slab(a1, hi, a0, a1, y0, y1, up, down)]
        # the four chamfered corners' floor and roof, a square turned 45 a little inside
        for cx, cz in ((a0, a0), (a1, a0), (a0, a1), (a1, a1)):
            h = ROT_C / math.sqrt(2)
            yy = (y0 - 0.3, y1 - 0.3) if up == "floor" else (y0 + 0.3, y1 + 0.3)
            els.append(B((cx - h, yy[0], cz - h), (cx + h, yy[1], cz + h), "frame",
                         ("up",) if up == "floor" else ("down",), per={"up": up, "down": down},
                         uv={"up": [0, 0, 2, 2], "down": [0, 0, 2, 2]},
                         rot=("y", 45, (cx, yy[0], cz))))
    # the straight east and west walls
    els += [wall_x(lo, lo + JB_WT, a0, a1, "skin", "inner", True),
            wall_x(hi - JB_WT, hi, a0, a1, "skin", "inner", False)]
    # chamfers: a wall's length centred on each corner's diagonal, turned 45
    diag = ROT_C * math.sqrt(2)
    inset = JB_WT / 2 / math.sqrt(2)
    for sx, sz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        cx = 8 + sx * (ROT - ROT_C / 2) - sx * inset
        cz = 8 + sz * (ROT - ROT_C / 2) - sz * inset
        angle = 45 if sx == sz else -45
        # a box long along x; turned, its north face looks north-west (45) or north-east (-45)
        outer = "north" if sz < 0 else "south"
        inner = "south" if sz < 0 else "north"
        els.append(B((cx - diag / 2, JB_FLOOR, cz - JB_WT / 2), (cx + diag / 2, JB_CEIL,
                                                                  cz + JB_WT / 2),
                     "skin", (outer, inner), per={outer: "skin", inner: "inner"},
                     uv={outer: [0, 0, diag * JB_K * 16.0 / 64, 14.5],
                         inner: [0, 0, diag * JB_K * 16.0 / 64, 14.5]},
                     rot=("y", angle, (cx, JB_FLOOR, cz))))
    # the collars to the edge of the three blocks, and a cap on the roof
    els += tunnel_section(-16, lo) + tunnel_section(hi, 32)
    els += [B((0, JB_ROOF, 0), (16, 32, 16), "roof", ("up", "north", "south", "east",
                                                                "west"),
              uv={"up": [0, 0, 16, 16]})]
    parts = [("body", {}, els),
             ("end_ahead", {"ahead": False}, end_ring(-16.0, -14.6)),
             ("end_behind", {"behind": False}, end_ring(30.6, 32.0))]
    models, state = multipart(parts, reg, JB_TEX, ao=False)
    C.add(reg, 'new BlockJetBridge("%s", BlockJetBridge.Kind.ROTUNDA)' % reg,
          names_of("Jet Bridge (Rotunda)", "Fluggastbrücke (Rotunde)",
                   "Pasarela de Embarque (Rotonda)", "Flygbrygga (Rotunda)"),
          models, state, item=item_of(parts, JB_TEX, keep=lambda c: True,
                                      display=gui_display(0.22, 0.0)), tab=TAB)

    # the drive leg under a tunnel: two legs under its walls, a yoke under the floor where no
    # leg continues above, the wheel bogie where none continues below
    reg = "airport_jet_bridge_drive"
    tex = {"leg": C.T("jb_leg"), "tyre": C.T("tyre"), "particle": C.T("jb_leg")}
    legs = []
    for x in (-5.0, 21.0):
        legs.append(B((x - 1.5, 0, 6.5), (x + 1.5, 16, 9.5), "leg", SIDES))
    beam_uv = {"north": [0, 0, 16, 2.6], "south": [0, 0, 16, 2.6], "up": [0, 0, 16, 4.8],
               "down": [0, 0, 16, 4.8]}
    yoke = [B((-7, 13.4, 5.6), (23, 16, 10.4), "leg", ALL, uv=beam_uv)]
    bogie = [B((-7, 5.4, 5.6), (23, 7.8, 10.4), "leg", ALL, uv=beam_uv)]
    for x in (-5.0, 21.0):
        bogie += lc.pipe_x(2.7, 8, 2.7, x - 2.4, x + 2.4, "tyre")
    column(reg, 'new BlockPlatformColumn("%s", %s)' % (reg, box_java((-7, 0, 5.6, 23, 16,
                                                                      10.4))),
           names_of("Jet Bridge (Drive Leg)", "Fluggastbrücke (Fahrwerk)",
                    "Pasarela de Embarque (Tren de Rodaje)", "Flygbrygga (Drivben)"),
           legs, bogie, yoke, tex, gui_display(0.35, 0.0))

    # the rotunda's column: thick, round, with a plinth and a head
    reg = "airport_jet_bridge_column"
    tex = {"column": C.T("jb_column"), "plinth": C.T("plinth"), "particle": C.T("jb_column")}
    column(reg, 'new BlockPlatformColumn("%s", %s)' % (reg, box_java((2, 0, 2, 14, 16, 14))),
           names_of("Jet Bridge (Rotunda Column)", "Fluggastbrücke (Rotundensäule)",
                    "Pasarela de Embarque (Columna de Rotonda)", "Flygbrygga (Rotundapelare)"),
           post(8, 8, 5.0, 0, 16, "column", top=False, bottom=False),
           [B((1.5, 0, 1.5), (14.5, 1.6, 14.5), "plinth", SIDES + ("up",))],
           [B((1, 14.4, 1), (15, 16, 15), "column", ALL)], tex,
           gui_display(0.6, 0.0))


# ------------------------------------------------------------------------------------------
# The sloped jet bridge tunnel
# ------------------------------------------------------------------------------------------
# A real bridge runs down (or up) from the terminal's door to the aircraft's sill. In blocks, a
# run of sloped tunnel drops one whole block over N pieces (N = 8, about 7 degrees, or 4, about
# 14), so the piece after the run meets a level tunnel, the cab or the next run a block lower.
# Each piece is the level tunnel's corridor sheared along its length: piece `step` of a run is
# drawn from step x D down at its high end (the model's south) to (step + 1) x D at its low end
# (north), D = 16 / N sixteenths. A JSON element cannot be sheared, nor turned to an angle like
# that, so the pieces are OBJ, one a (grade, step), written from the same elements the level
# tunnel's JSON is made of. BlockJetBridgeSlope works out the step from its neighbours and turns
# the model so its low end faces downhill; its collision shares these numbers.

JS_GRADES = {"8": 8, "4": 4}        # pieces a run, by grade
JS_GRADE_STATES = (("down8", "8", False), ("down4", "4", False), ("up8", "8", True),
                   ("up4", "4", True))
# Minecraft's corner order and uv corners for each face of a box (FaceBakery, no rotation)
_FACE_CORNERS = {
    "down": ((0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)),
    "up": ((0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)),
    "north": ((1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)),
    "south": ((0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)),
    "west": ((0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)),
    "east": ((1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)),
}
_FACE_OUT = {"down": (0, -1, 0), "up": (0, 1, 0), "north": (0, 0, -1), "south": (0, 0, 1),
             "west": (-1, 0, 0), "east": (1, 0, 0)}


def _sheared_obj(name, mtl, elements, drop):
    """An OBJ of JSON-style elements (no element rotation) with each corner lowered by
    drop(z) sixteenths; every face keeps its texture window. Coordinates in blocks, v down the
    texture (no flip-v: a multipart part cannot pass Forge's custom data)."""
    vs, vts, vns, faces = [], [], [], []
    for e in elements:
        assert "rotation" not in e, name
        lo, hi = e["from"], e["to"]
        for f, face in e["faces"].items():
            u0, v0, u1, v1 = face["uv"]
            uvs = ((u0, v0), (u0, v1), (u1, v1), (u1, v0))
            pts = []
            for c in _FACE_CORNERS[f]:
                x = hi[0] if c[0] else lo[0]
                y = hi[1] if c[1] else lo[1]
                z = hi[2] if c[2] else lo[2]
                pts.append((x, y - drop(z), z))
            a = [pts[1][k] - pts[0][k] for k in range(3)]
            b = [pts[2][k] - pts[0][k] for k in range(3)]
            n = (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
            length = math.sqrt(sum(c * c for c in n)) or 1.0
            n = tuple(c / length for c in n)
            if sum(n[k] * _FACE_OUT[f][k] for k in range(3)) < 0:   # keep it facing out
                pts, uvs, n = pts[::-1], uvs[::-1], tuple(-c for c in n)
            refs = []
            for (x, y, z), (u, v) in zip(pts, uvs):
                vs.append("v %.5f %.5f %.5f" % (x / 16.0, y / 16.0, z / 16.0))
                vts.append("vt %.5f %.5f" % (min(1, max(0, u / 16.0)), min(1, max(0, v / 16.0))))
                vns.append("vn %.5f %.5f %.5f" % n)
                refs.append(len(vs))
            faces.append((face["texture"].lstrip("#"), refs))
    lines = ["# Generated by dev-env-utils/scripts/gen_transit_airside.py -- do not edit",
             "mtllib %s.mtl" % mtl, "o %s" % name] + vs + vts + vns
    current = None
    for mat, refs in sorted(faces, key=lambda f: f[0]):
        if mat != current:
            lines.append("usemtl " + mat)
            current = mat
        lines.append("f " + " ".join("%d/%d/%d" % (r, r, r) for r in refs))
    return "\n".join(lines) + "\n"


def jet_bridge_slope():
    reg = "airport_jet_bridge_slope"
    folder = C.M("x").split(":", 1)[1].rsplit("/", 1)[0]       # this catalogue's model folder
    mtl = "jet_bridge_slope"
    C.extra["models/block/%s/%s.mtl" % (folder, mtl)] = "\n".join(
        ["# Generated by dev-env-utils/scripts/gen_transit_airside.py -- do not edit"]
        + ["newmtl %s\nmap_Kd %s" % (k, v) for k, v in sorted(JB_TEX.items())
           if k != "particle"]) + "\n"
    for grade, n in JS_GRADES.items():
        d = 16.0 / n
        for step in range(n):
            def drop(z, step=step, d=d):
                return step * d + (16.0 - z) / 16.0 * d
            for part, els in (("body", tunnel_section(0, 16)),
                              ("north", end_ring(-0.6, 1.0)), ("south", end_ring(15.0, 16.6))):
                mname = "%s_%s_%d_%s" % (reg, grade, step, part)
                C.extra["models/block/%s/%s.obj" % (folder, mname)] = _sheared_obj(
                    mname, mtl, els, drop)
    # The model's low end is its north. A down piece is turned to its facing (downhill towards
    # the aircraft); an up piece the other way, its step counted from its own high end.
    rules = []
    for gname, grade, up in JS_GRADE_STATES:
        n = JS_GRADES[grade]
        for facing, y in FACINGS:
            turn = (y + 180) % 360 if up else y
            for step in range(n):
                base = {"facing": facing, "grade": gname, "step": str(step)}
                prefix = "csm:%s/%s_%s_%d_" % (folder, reg, grade, step)

                def apply(part, prefix=prefix, turn=turn):
                    out = {"model": prefix + part + ".obj"}
                    if turn:
                        out["y"] = turn
                    return out
                rules.append({"when": dict(base), "apply": apply("body")})
                # the frame at each end that nothing continues from: ahead is towards the
                # aircraft, which is the model's north for a down piece and south for an up one
                for prop, end in (("ahead", "south" if up else "north"),
                                  ("behind", "north" if up else "south")):
                    rules.append({"when": dict(base, **{prop: "false"}), "apply": apply(end)})
    item = lc.model(JB_TEX, tunnel_section(0, 16))
    item["display"] = gui_display(0.32, 0.0)
    C.add(reg, 'new BlockJetBridgeSlope("%s")' % reg,
          names_of("Jet Bridge (Sloped Tunnel)", "Fluggastbrücke (Geneigter Tunnel)",
                   "Pasarela de Embarque (Túnel Inclinado)", "Flygbrygga (Lutande Tunnel)"),
          {}, {"multipart": rules}, item=item, tab=TAB)


# ------------------------------------------------------------------------------------------
# The large jet bridge
# ------------------------------------------------------------------------------------------
# For terminals built at a large scale (14 block ceilings and more): the corridor three blocks
# wide and four tall inside, against the level bridge's 29 sixteenths each way. It is the same
# bridge scaled, drawn from the same elements: across by LJ_SX about the block's middle, and up
# by LJ_SY above the floor's top, so the floor keeps its thickness and the level and large
# bridges meet a door at the same height. Every piece reaches up to two and a half blocks past its
# cell, past what a JSON element may, so all of it is OBJ (the sloped tunnel's writer). It still
# needs no other block: a player inside is never more than a block from the piece's own cell,
# which is as far as the game looks for a block's collision boxes (BlockJetBridge, LARGE_*).
# The cab's bumper and safety bar keep their size: a fence's height is a fence's height.
LJ_SX = 48.0 / 29.0                # the inside, 29 sixteenths across, to three blocks
LJ_SY = 64.0 / 29.0                # and 29 tall, to four


def _lj_x(x):
    return round(8 + (x - 8) * LJ_SX, 4)


def _lj_y(y):
    return y if y <= JB_FLOOR else round(JB_FLOOR + (y - JB_FLOOR) * LJ_SY, 4)


def large_of(els, scale_y=True):
    """Elements scaled to the large bridge (see LJ_SX): positions only, every face keeps its
    texture window, so the walls' windows grow with them."""
    out = []
    for e in els:
        assert "rotation" not in e
        e = dict(e, faces={f: dict(v) for f, v in e["faces"].items()})
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        if scale_y:
            y0, y1 = _lj_y(y0), _lj_y(y1)
        e["from"] = [_lj_x(x0), y0, z0]
        e["to"] = [_lj_x(x1), y1, z1]
        out.append(e)
    return out


def _obj_part(folder, mtl, name, els, drop=lambda z: 0.0):
    C.extra["models/block/%s/%s.obj" % (folder, name)] = _sheared_obj(name, mtl, els, drop)
    return "csm:%s/%s.obj" % (folder, name)


def _mtl(folder, mtl, tex):
    C.extra["models/block/%s/%s.mtl" % (folder, mtl)] = "\n".join(
        ["# Generated by dev-env-utils/scripts/gen_transit_airside.py -- do not edit"]
        + ["newmtl %s\nmap_Kd %s" % (k, v) for k, v in sorted(tex.items())
           if k != "particle"]) + "\n"


def _turned(model, y):
    out = {"model": model}
    if y:
        out["y"] = y
    return out


def jet_bridge_large():
    folder = C.M("x").split(":", 1)[1].rsplit("/", 1)[0]
    _mtl(folder, "jet_bridge_large", JB_TEX)
    _mtl(folder, "jet_bridge_large_cab", CAB_TEX)

    # the tunnel
    reg = "airport_jet_bridge_large_tunnel"
    body = _obj_part(folder, "jet_bridge_large", reg + "_body", large_of(tunnel_section(0, 16)))
    ahead = _obj_part(folder, "jet_bridge_large", reg + "_end_ahead",
                      large_of(end_ring(-0.6, 1.0)))
    behind = _obj_part(folder, "jet_bridge_large", reg + "_end_behind",
                       large_of(end_ring(15.0, 16.6)))
    rules = []
    for facing, y in FACINGS:
        rules += [{"when": {"facing": facing}, "apply": _turned(body, y)},
                  {"when": {"facing": facing, "ahead": "false"}, "apply": _turned(ahead, y)},
                  {"when": {"facing": facing, "behind": "false"}, "apply": _turned(behind, y)}]
    item = lc.model(JB_TEX, tunnel_section(0, 16))
    item["display"] = gui_display(0.32, 0.0)
    C.add(reg, 'new BlockJetBridge("%s", BlockJetBridge.Kind.TUNNEL, true)' % reg,
          names_of("Large Jet Bridge (Tunnel)", "Große Fluggastbrücke (Tunnel)",
                   "Pasarela de Embarque Grande (Túnel)", "Stor Flygbrygga (Tunnel)"),
          {}, {"multipart": rules}, item=item, tab=TAB)

    # the cab: the corridor scaled, the bumper and safety bar only widened
    reg = "airport_jet_bridge_large_cab"
    cab_body, front = cab_elements()
    keep_height = [e for e in front if e["faces"].get("up", {}).get("texture") == "#hazard"
                   or e["faces"].get("north", {}).get("texture") == "#rail"]
    bellows = [e for e in front if e not in keep_height]
    # the console (cab_elements' last body element) keeps its height too: it is a desk
    els = (large_of(cab_body[:-1]) + large_of(cab_body[-1:], scale_y=False)
           + large_of(bellows) + large_of(keep_height, scale_y=False))
    body = _obj_part(folder, "jet_bridge_large_cab", reg + "_body", els)
    behind = _obj_part(folder, "jet_bridge_large", reg + "_end_behind",
                       large_of(end_ring(15.0, 16.6, CAB_X0, CAB_X1)))
    rules = []
    for facing, y in FACINGS:
        rules += [{"when": {"facing": facing}, "apply": _turned(body, y)},
                  {"when": {"facing": facing, "behind": "false"}, "apply": _turned(behind, y)}]
    item = lc.model(CAB_TEX, cab_body + front)
    item["display"] = gui_display(0.3, 0.0)
    C.add(reg, 'new BlockJetBridge("%s", BlockJetBridge.Kind.CAB, true)' % reg,
          names_of("Large Jet Bridge (Cab)", "Große Fluggastbrücke (Kabine)",
                   "Pasarela de Embarque Grande (Cabina)", "Stor Flygbrygga (Hytt)"),
          {}, {"multipart": rules}, item=item, tab=TAB)

    # the sloped tunnel: the sloped pieces' shear on the large corridor
    reg = "airport_jet_bridge_large_slope"
    for grade, n in JS_GRADES.items():
        d = 16.0 / n
        for step in range(n):
            def drop(z, step=step, d=d):
                return step * d + (16.0 - z) / 16.0 * d
            for part, els in (("body", tunnel_section(0, 16)),
                              ("north", end_ring(-0.6, 1.0)), ("south", end_ring(15.0, 16.6))):
                _obj_part(folder, "jet_bridge_large", "%s_%s_%d_%s" % (reg, grade, step, part),
                          large_of(els), drop)
    rules = []
    for gname, grade, up in JS_GRADE_STATES:
        n = JS_GRADES[grade]
        for facing, y in FACINGS:
            turn = (y + 180) % 360 if up else y
            for step in range(n):
                base = {"facing": facing, "grade": gname, "step": str(step)}
                prefix = "csm:%s/%s_%s_%d_" % (folder, reg, grade, step)
                rules.append({"when": dict(base), "apply": _turned(prefix + "body.obj", turn)})
                for prop, end in (("ahead", "south" if up else "north"),
                                  ("behind", "north" if up else "south")):
                    rules.append({"when": dict(base, **{prop: "false"}),
                                  "apply": _turned(prefix + end + ".obj", turn)})
    item = lc.model(JB_TEX, tunnel_section(0, 16))
    item["display"] = gui_display(0.32, 0.0)
    C.add(reg, 'new BlockJetBridgeSlope("%s", true)' % reg,
          names_of("Large Jet Bridge (Sloped Tunnel)", "Große Fluggastbrücke (Geneigter Tunnel)",
                   "Pasarela de Embarque Grande (Túnel Inclinado)",
                   "Stor Flygbrygga (Lutande Tunnel)"),
          {}, {"multipart": rules}, item=item, tab=TAB)

    # the drive leg: the two posts under the large tunnel's floor, near its walls
    reg = "airport_jet_bridge_large_drive"
    tex = {"leg": C.T("jb_leg"), "tyre": C.T("tyre"), "particle": C.T("jb_leg")}
    legs = [B((x - 1.5, 0, 6.5), (x + 1.5, 16, 9.5), "leg", SIDES) for x in (-13.5, 29.5)]
    beam_uv = {"north": [0, 0, 16, 2.6], "south": [0, 0, 16, 2.6], "up": [0, 0, 16, 4.8],
               "down": [0, 0, 16, 4.8]}
    yoke = [B((-16, 13.4, 5.6), (32, 16, 10.4), "leg", ALL, uv=beam_uv)]
    bogie = [B((-16, 5.4, 5.6), (32, 7.8, 10.4), "leg", ALL, uv=beam_uv)]
    for x in (-13.5, 29.5):
        bogie += lc.pipe_x(2.7, 8, 2.7, x - 2.4, x + 2.4, "tyre")
    column(reg, 'new BlockPlatformColumn("%s", %s)' % (reg, box_java((-16, 0, 5.6, 32, 16,
                                                                       10.4))),
           names_of("Large Jet Bridge (Drive Leg)", "Große Fluggastbrücke (Fahrwerk)",
                    "Pasarela de Embarque Grande (Tren de Rodaje)",
                    "Stor Flygbrygga (Drivben)"),
           legs, bogie, yoke, tex, gui_display(0.3, 0.0))


# ------------------------------------------------------------------------------------------
# Jet bridge turns
# ------------------------------------------------------------------------------------------
# A corridor two blocks wide (three and a bit for the large bridge) cannot turn inside one cell:
# two tunnel lines at right angles next to the same cell overlap each other's walls, and across a
# large turn the far wall is past the one block the game looks for collision boxes. So a turn is
# several real blocks placed as one (BlockJetBridgeTurn, all its cells or none, as the mast arm
# curves are), each drawing and colliding its own share of one shape. Every cell is the same
# block; which cell it is lives in a tile entity, read into the `cell` actual-state property.
#
# Each shape is written once, for a right-hand turn, in a canonical frame: the turn's entry
# cell (end A) is (0, 0), entered through its south face heading north; end B is the exit. A
# left-hand turn is the same cells turned a quarter (corner, curve: B's east face turned to
# face south, so B becomes the entry) or entered from the other leg (U-turn), so no shape needs
# a mirrored model. The shapes:
#
#   corner  a square room centred where the two corridor lines cross, three cells across (large
#           five), open to the south and the east to the corridor's width
#   turn    a quarter circle, centreline radius 2.5 blocks (large 3.5)
#   uturn   a half circle back to a line two blocks over (large four), as close as two
#           corridors can run side by side
#
# Sizes come from one rule: the tunnels that join a turn start where neither overlaps the
# other's band, the corridor's half width (16, large 26.5 sixteenths) past the crossing line,
# rounded up to the next whole block. Everything is drawn from the straight tunnel's section
# (JB_* or its large scaling) and textures: carpet, panelled walls with a window a block, the
# lit strip down the middle of the ceiling (following the centreline), the skin with its navy
# band, the roof. Curves are many short straight pieces. The whole shape is built as flat
# polygons, then cut at the block lines and each piece handed to the cell it lies in (or, past
# the turn's cells, the cell beside it), and written as one OBJ a cell.
#
# The generator also writes JetBridgeTurnShape.java: each shape's cells, its two ends and each
# cell's collision boxes, from the same walls and floors the models are drawn from, so the two
# cannot disagree. Before writing it, it walks a player through every shape the way the game
# looks for collision boxes (World.getCollisionBoxes: blocks within a block of the moving box,
# skipping the corner columns of that range) and adds a cell wherever a box could be missed.

TURN_JAVA_REL = os.path.join("modules", "transit", "src", "main", "java", "com",
                             "micatechnologies", "minecraft", "csm", "transit", "airport",
                             "JetBridgeTurnShape.java")
TURN_KINDS = ("corner", "turn", "uturn")
TURN_ENUM = {"corner": "CORNER", "turn": "TURN", "uturn": "UTURN"}
_EPS = 1e-6


def _turn_section(large):
    """The corridor's section, in sixteenths: the straight tunnel's (JB_*), or as large_of
    scales it."""
    if not large:
        return {"half": (JB_X1 - JB_X0) / 2.0, "wt": JB_WT, "floor": JB_FLOOR,
                "ceil": JB_CEIL, "roof": JB_ROOF}
    x0 = _lj_x(JB_X0)
    return {"half": round(8 - x0, 4), "wt": round(_lj_x(JB_X0 + JB_WT) - x0, 4),
            "floor": JB_FLOOR, "ceil": _lj_y(JB_CEIL), "roof": _lj_y(JB_ROOF)}


# --- the shapes as runs of wall and pieces of floor ---------------------------------------
class _Run(object):
    """A wall: its skin side P and its inside face Q, sampled at the same stations (Q is P
    offset by the wall's thickness), the panel each segment belongs to on each face, and
    whether its ends are jambs (a face across the wall's thickness, where an opening starts)."""

    def __init__(self, P, Q, panels_p, panels_q, skin=True, caps=(False, False)):
        self.P, self.Q = P, Q
        self.panels_p, self.panels_q = panels_p, panels_q
        self.skin = skin
        self.caps = caps


def _panels(points, breaks):
    """Panel ranges along a polyline: for each segment, (start, end, s0, s1) in its own length,
    a panel ending at each index in `breaks` (and at the last point)."""
    s = [0.0]
    for a, b in zip(points, points[1:]):
        s.append(s[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    ends = sorted(set(list(breaks) + [len(points) - 1]))
    out = []
    start = 0
    for e in ends:
        if e <= start:
            continue
        for i in range(start, e):
            out.append((s[start], s[e], s[i], s[i + 1]))
        start = e
    return out


def _grid_breaks(points):
    """Panel breaks of a straight-sided run: at its corners and wherever it crosses a block
    line, so a full block of wall carries one window, as a tunnel's does."""
    pts = [points[0]]
    breaks = []
    for a, b in zip(points, points[1:]):
        ax, az = a
        bx, bz = b
        cuts = []
        if abs(bx - ax) > _EPS:
            lo, hi = sorted((ax, bx))
            k = math.floor(lo / 16.0) + 1
            while k * 16.0 < hi - _EPS:
                t = (k * 16.0 - ax) / (bx - ax)
                cuts.append(t)
                k += 1
        if abs(bz - az) > _EPS:
            lo, hi = sorted((az, bz))
            k = math.floor(lo / 16.0) + 1
            while k * 16.0 < hi - _EPS:
                t = (k * 16.0 - az) / (bz - az)
                cuts.append(t)
                k += 1
        for t in sorted(cuts):
            if _EPS < t < 1 - _EPS:
                pts.append((ax + (bx - ax) * t, az + (bz - az) * t))
                breaks.append(len(pts) - 1)
        pts.append(b)
        breaks.append(len(pts) - 1)
    return pts, breaks


def _room_runs(sec, H):
    """The corner room's two walls, running round it between its two openings."""
    half, wt = sec["half"], sec["wt"]
    hi = half - wt                       # the opening's half width, inside the walls
    zc = 16 - H                          # the exit's centreline
    x0, x1, z0, z1 = 8 - H, 8 + H, 16 - 2 * H, 16.0
    runs = []
    for P, Q in (([(8 - hi, z1), (x0, z1), (x0, z0), (x1, z0), (x1, zc - hi)],
                  [(8 - hi, z1 - wt), (x0 + wt, z1 - wt), (x0 + wt, z0 + wt),
                   (x1 - wt, z0 + wt), (x1 - wt, zc - hi)]),
                 ([(x1, zc + hi), (x1, z1), (8 + hi, z1)],
                  [(x1 - wt, zc + hi), (x1 - wt, z1 - wt), (8 + hi, z1 - wt)])):
        # split both lines at the same block lines, taken from the skin
        Pp, Pb = _grid_breaks(P)
        # the inside line, cut where the skin's cuts are: same x (or z) as the skin's station
        Qp = []
        for i, p in enumerate(Pp):
            # find the segment of P this station is on and offset it the way Q is
            for j in range(len(P) - 1):
                a, b = P[j], P[j + 1]
                if _on_segment(p, a, b):
                    qa, qb = Q[j], Q[j + 1]
                    t = _param(p, a, b)
                    Qp.append(_qpoint(p, a, b, qa, qb, t))
                    break
        runs.append(_Run(Pp, Qp, _panels(Pp, Pb), _panels(Qp, Pb), caps=(True, True)))
    return runs


def _on_segment(p, a, b):
    cross = (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
    if abs(cross) > 1e-6:
        return False
    return (min(a[0], b[0]) - _EPS <= p[0] <= max(a[0], b[0]) + _EPS
            and min(a[1], b[1]) - _EPS <= p[1] <= max(a[1], b[1]) + _EPS)


def _param(p, a, b):
    L2 = (b[0] - a[0]) ** 2 + (b[1] - a[1]) ** 2
    return ((p[0] - a[0]) * (b[0] - a[0]) + (p[1] - a[1]) * (b[1] - a[1])) / L2


def _qpoint(p, a, b, qa, qb, t):
    """The inside line's station across the wall from skin station p on skin segment a-b: at
    the segment's ends the mitred corner, between them straight across."""
    if t < _EPS:
        return qa
    if t > 1 - _EPS:
        return qb
    # the inside segment runs parallel: move p across by the offset at the segment's middle
    dx = (qa[0] + qb[0]) / 2 - (a[0] + b[0]) / 2
    dz = (qa[1] + qb[1]) / 2 - (a[1] + b[1]) / 2
    ux, uz = b[0] - a[0], b[1] - a[1]
    L = math.hypot(ux, uz)
    ux, uz = ux / L, uz / L
    across = dx * (-uz) + dz * ux                 # the offset's part square to the wall
    return (p[0] + across * (-uz), p[1] + across * ux)


def _arc_point(O, r, th):
    return (O[0] - r * math.cos(th), O[1] - r * math.sin(th))


class _Shape(object):
    """A turn in its canonical frame: walls, floor pieces (convex plan polygons with how their
    ceiling is textured), the cells that hold it and its two ends."""

    def __init__(self, kind, large):
        self.kind, self.large = kind, large
        self.sec = sec = _turn_section(large)
        half, wt = sec["half"], sec["wt"]
        self.runs = []
        self.floors = []            # (polygon, ceiling uv function)
        self.core = None            # in(x, z): on the block-wide band about the centreline
        self.spot = None            # in(x, z): a player's feet can be centred here
        if kind == "corner":
            H = 24.0 if not large else 40.0
            self.H = H
            self.runs = _room_runs(sec, H)
            self.end_a = ((0, 0), "south")
            self.end_b = ((int((8 + H) // 16) - 1, int(math.floor((16 - H) / 16.0))), "east")
            self._room_floors()
            x0, x1, z0, z1 = 8 - H, 8 + H, 16 - 2 * H, 16.0
            self.core = lambda x, z: x0 <= x <= x1 and z0 <= z <= z1
            p, hi, zc = _PLAYER / 2, half - wt, 16 - H
            self.spot = lambda x, z: (
                (x0 + wt + p <= x <= x1 - wt - p and z0 + wt + p <= z <= z1 - wt - p)
                or (8 - hi + p <= x <= 8 + hi - p and z0 + wt + p <= z <= z1)
                or (zc - hi + p <= z <= zc + hi - p and x0 + wt + p <= x <= x1))
            self.footprint = self.core
            return
        R = {("turn", False): 40.0, ("turn", True): 56.0,
             ("uturn", False): 16.0, ("uturn", True): 32.0}[(kind, large)]
        self.R = R
        self.O = O = (8 + R, 16.0)
        self.th_end = math.pi / 2 if kind == "turn" else math.pi
        r_out, r_in = R + half, R - half
        radii = {"out_skin": r_out, "out_in": r_out - wt, "in_skin": max(0.0, r_in),
                 "in_in": max(0.0, r_in) + wt if r_in > 0 else wt}
        self.radii = radii
        if kind == "turn":
            self.end_b = ((int((8 + R) // 16) - 1, int(math.floor((16 - R) / 16.0))), "east")
        else:
            self.end_b = ((int(2 * R // 16), 0), "south")
        self.end_a = ((0, 0), "south")
        # stations: fine enough that the outer skin's chords are about 4 sixteenths, and at
        # every panel's ends on all four faces
        n = int(math.ceil(self.th_end * r_out / 4.0))
        angles = set(self.th_end * i / n for i in range(n + 1))
        panel_ks = {}
        for key, r in radii.items():
            L = self.th_end * r
            k = max(1, int(round(L / 16.0)))
            panel_ks[key] = k
            for j in range(k + 1):
                angles.add(self.th_end * j / k)
        self.angles = sorted(angles)
        th = self.angles

        def run_for(rp, rq, kp, kq, skin):
            P = [_arc_point(O, rp, a) for a in th]
            Q = [_arc_point(O, rq, a) for a in th]
            bp = [i for i, a in enumerate(th) if any(abs(a - self.th_end * j / kp) < 1e-9
                                                     for j in range(kp + 1))]
            bq = [i for i, a in enumerate(th) if any(abs(a - self.th_end * j / kq) < 1e-9
                                                     for j in range(kq + 1))]
            return _Run(P, Q, _panels(P, bp), _panels(Q, bq), skin=skin)
        self.runs = [run_for(radii["out_skin"], radii["out_in"], panel_ks["out_skin"],
                             panel_ks["out_in"], True),
                     run_for(radii["in_skin"], radii["in_in"], panel_ks["in_skin"],
                             panel_ks["in_in"], r_in > 0)]
        W = 2 * half
        for a0, a1 in zip(th, th[1:]):
            poly = [_arc_point(O, radii["in_skin"], a0), _arc_point(O, radii["out_skin"], a0),
                    _arc_point(O, radii["out_skin"], a1), _arc_point(O, radii["in_skin"], a1)]
            if radii["in_skin"] <= 0:
                poly = poly[:3]
            self.floors.append((poly, _polar_ceiling(O, R, W, (a0 + a1) / 2)))
        r_lo, r_hi = radii["in_skin"], radii["out_skin"]

        def polar(x, z):
            dx, dz = O[0] - x, O[1] - z
            return math.hypot(dx, dz), dz

        def in_sector(x, z):
            if kind == "turn" and x > O[0] + _EPS:
                return False
            return z <= O[1] + _EPS
        self.footprint = lambda x, z: (in_sector(x, z)
                                       and r_lo - _EPS <= polar(x, z)[0] <= r_hi + _EPS)
        self.core = lambda x, z: in_sector(x, z) and abs(polar(x, z)[0] - R) <= 8.0
        self.spot = lambda x, z: (in_sector(x, z) and radii["in_in"] + _PLAYER / 2
                                  <= polar(x, z)[0] <= radii["out_in"] - _PLAYER / 2)

    def _room_floors(self):
        """The room's floor in nine rectangles: the lit strip runs up the middle of the entry,
        to the middle of the room and out along the exit's middle, an L; the rest is ceiling."""
        sec, H = self.sec, self.H
        sw = 6.0 * (2 * sec["half"]) / 32.0           # the strip, as wide as a tunnel's
        zc = 16 - H
        xs = (8 - H, 8 - sw / 2, 8 + sw / 2, 8 + H)
        zs = (16 - 2 * H, zc - sw / 2, zc + sw / 2, 16.0)
        W = 2 * sec["half"]
        for i in range(3):
            for j in range(3):
                poly = [(xs[i], zs[j]), (xs[i + 1], zs[j]), (xs[i + 1], zs[j + 1]),
                        (xs[i], zs[j + 1])]
                if i == 1 and j in (1, 2):
                    uvf = _strip_ceiling(W, along="z", centre=8.0)
                elif i == 2 and j == 1:
                    uvf = _strip_ceiling(W, along="x", centre=zc)
                else:
                    uvf = _plain_ceiling()
                self.floors.append((poly, uvf))

    def ends(self):
        return [self.end_a, self.end_b]


def _polar_ceiling(O, R, W, th_mid):
    """The ceiling round a curve: across the corridor as across a tunnel (the lit strip on the
    centreline), along it by the centreline's length."""
    s_base = 32.0 * math.floor(th_mid * R / 32.0)

    def uvf(x, y, z, region):
        dx, dz = O[0] - x, O[1] - z
        if abs(dz) < 1e-9:
            dz = 0.0
        r = math.hypot(dx, dz)
        th = math.atan2(dz, dx) if r > 1e-6 else th_mid
        return ((16 + (r - R) * 32.0 / W) / 4.0, (th * R - s_base) / 4.0)
    return uvf


def _strip_ceiling(W, along, centre):
    def uvf(x, y, z, region):
        if along == "z":
            across, length, base = x, z, 64 * math.floor(region[1] / 64.0)
        else:
            across, length, base = z, x, 64 * math.floor(region[0] / 64.0)
        return ((16 + (across - centre) * 32.0 / W) / 4.0, (length - base) / 4.0)
    return uvf


def _plain_ceiling():
    """Ceiling clear of the lit strip: a window of the texture right of it."""
    def uvf(x, y, z, region):
        bx = 32 * math.floor(region[0] / 32.0)
        bz = 64 * math.floor(region[1] / 64.0)
        return ((24 + (x - bx)) / 4.0, (z - bz) / 4.0)
    return uvf


def _planar():
    """Carpet, roof and underside: a texel a sixteenth, as floor_uv, from the block grid."""
    def uvf(x, y, z, region):
        bx = 64 * math.floor(region[0] / 64.0)
        bz = 64 * math.floor(region[1] / 64.0)
        return ((x - bx) / 4.0, (z - bz) / 4.0)
    return uvf


# --- the faces ------------------------------------------------------------------------------
WALL_V = JB_WALL_H * JB_K * 16.0 / 64       # the wall texture's height in uv: 14.5


def _wall_uv(s0, s1, s, frame=False):
    """u along a panel s0..s1: a whole window if the panel is a block's worth, else the plain
    stretch right of the window (frame: the frame texture once along it)."""
    L = s1 - s0
    t = (s - s0) / L if L > _EPS else 0.0
    if frame:
        return 16.0 * t
    if L >= 12.0:
        return 32.0 * t / 4.0
    return (24 + 8 * t) / 4.0


def _vface(mat, p0, p1, y0, y1, normal, u0, u1, v0, v1):
    """A vertical quad over plan points p0-p1 between y0 and y1: u runs p0 to p1, v from the top
    (y1) down."""
    pts = [(p0[0], y1, p0[1]), (p0[0], y0, p0[1]), (p1[0], y0, p1[1]), (p1[0], y1, p1[1])]
    dx, dz = p1[0] - p0[0], p1[1] - p0[1]
    L2 = dx * dx + dz * dz

    def uvf(x, y, z, region):
        t = ((x - p0[0]) * dx + (z - p0[1]) * dz) / L2 if L2 > _EPS else 0.0
        return (u0 + (u1 - u0) * t, v0 + (v1 - v0) * (y1 - y) / (y1 - y0))
    return (mat, pts, (normal[0], 0.0, normal[1]), uvf)


def _hface(mat, poly, y, up, uvf):
    return (mat, [(x, y, z) for x, z in poly], (0.0, 1.0 if up else -1.0, 0.0), uvf)


def _unit(dx, dz):
    L = math.hypot(dx, dz)
    return (dx / L, dz / L) if L > _EPS else (0.0, 0.0)


def _turn_faces(shape):
    sec = shape.sec
    fl, ce, rf = sec["floor"], sec["ceil"], sec["roof"]
    faces = []
    for poly, ceiling in shape.floors:
        faces += [_hface("floor", poly, fl, True, _planar()),
                  _hface("under", poly, 0.0, False, _planar()),
                  _hface("roof", poly, rf, True, _planar()),
                  _hface("ceiling", poly, ce, False, ceiling)]
    for run in shape.runs:
        P, Q = run.P, run.Q
        for i in range(len(P) - 1):
            a, b, qa, qb = P[i], P[i + 1], Q[i], Q[i + 1]
            d = _unit(qb[0] - qa[0], qb[1] - qa[1])
            n = (-d[1], d[0])
            mid_out = ((a[0] + b[0] - qa[0] - qb[0]) / 2, (a[1] + b[1] - qa[1] - qb[1]) / 2)
            if n[0] * mid_out[0] + n[1] * mid_out[1] < 0:
                n = (-n[0], -n[1])
            if run.skin and math.hypot(b[0] - a[0], b[1] - a[1]) > _EPS:
                s0, s1, sa, sb = run.panels_p[i]
                faces.append(_vface("skin", a, b, fl, ce, n, _wall_uv(s0, s1, sa),
                                    _wall_uv(s0, s1, sb), 0.0, WALL_V))
                fa, fb = _wall_uv(s0, s1, sa, True), _wall_uv(s0, s1, sb, True)
                faces.append(_vface("frame", a, b, 0.0, fl, n, fa, fb, 0.0, 1.5))
                faces.append(_vface("frame", a, b, ce, rf, n, fa, fb, 0.0, 1.5))
            s0, s1, sa, sb = run.panels_q[i]
            faces.append(_vface("inner", qa, qb, fl, ce, (-n[0], -n[1]), _wall_uv(s0, s1, sa),
                                _wall_uv(s0, s1, sb), 0.0, WALL_V))
        # the jambs where an opening starts: across the wall's thickness, facing the opening
        for end, cap in ((0, run.caps[0]), (-1, run.caps[1])):
            if not cap:
                continue
            p, q = P[end], Q[end]
            nxt = P[1] if end == 0 else P[-2]
            n = _unit(p[0] - nxt[0], p[1] - nxt[1])
            faces.append(_vface("inner", p, q, fl, ce, n, 24 / 4.0, (24 + 2 * sec["wt"]) / 4.0,
                                0.0, WALL_V))
    return faces


# --- cutting at the block lines ------------------------------------------------------------
def _clip(pts, axis, value, keep_above):
    """Sutherland-Hodgman against the plane x (axis 0) or z (axis 2) = value."""
    out = []
    n = len(pts)
    for i in range(n):
        a, b = pts[i], pts[(i + 1) % n]
        ina = (a[axis] >= value - 1e-9) if keep_above else (a[axis] <= value + 1e-9)
        inb = (b[axis] >= value - 1e-9) if keep_above else (b[axis] <= value + 1e-9)
        if ina:
            out.append(a)
        if ina != inb:
            t = (value - a[axis]) / (b[axis] - a[axis])
            out.append(tuple(a[k] + (b[k] - a[k]) * t for k in range(3)))
    return out


def _area(pts, normal):
    sx = sy = sz = 0.0
    for i in range(len(pts)):
        a, b = pts[i], pts[(i + 1) % len(pts)]
        sx += (a[1] - b[1]) * (a[2] + b[2])
        sy += (a[2] - b[2]) * (a[0] + b[0])
        sz += (a[0] - b[0]) * (a[1] + b[1])
    return abs(sx * normal[0] + sy * normal[1] + sz * normal[2]) / 2.0


def _dedupe(pts):
    out = []
    for p in pts:
        if not out or max(abs(p[k] - out[-1][k]) for k in range(3)) > 1e-7:
            out.append(p)
    while len(out) > 1 and max(abs(out[0][k] - out[-1][k]) for k in range(3)) <= 1e-7:
        out.pop()
    return out


def _faces_into(pts, normal, cx, cz):
    """Whether a face lying on one of the cell's block lines looks into the cell. Such a face
    belongs to the cell it looks out of: kept in both, it would be drawn twice, once at the
    depth of the neighbouring block's own face."""
    for axis, n in ((0, normal[0]), (2, normal[2])):
        lo = (cx if axis == 0 else cz) * 16.0
        for edge, inward in ((lo, n > 0.5), (lo + 16, n < -0.5)):
            if inward and all(abs(p[axis] - edge) < 1e-6 for p in pts):
                return True
    return False


def _cut_faces(faces):
    """Each face cut at the block lines: {(cx, cz): [(mat, pts, normal, uvf)]}, by the cell
    each piece lies in."""
    pieces = {}
    for mat, pts, normal, uvf in faces:
        xs = [p[0] for p in pts]
        zs = [p[2] for p in pts]
        # a block either side of a face lying on a block line, so the one it looks out of
        # is offered it
        for cx in range(int(math.floor((min(xs) - 1e-6) / 16.0)),
                        int(math.floor((max(xs) + 1e-6) / 16.0)) + 1):
            for cz in range(int(math.floor((min(zs) - 1e-6) / 16.0)),
                            int(math.floor((max(zs) + 1e-6) / 16.0)) + 1):
                if _faces_into(pts, normal, cx, cz):
                    continue
                q = list(pts)
                for axis, value, above in ((0, cx * 16.0, True), (0, cx * 16.0 + 16, False),
                                           (2, cz * 16.0, True), (2, cz * 16.0 + 16, False)):
                    q = _clip(q, axis, value, above)
                    if len(q) < 3:
                        break
                q = _dedupe(q)
                if len(q) < 3 or _area(q, normal) < 1e-5:
                    continue
                pieces.setdefault((cx, cz), []).append((mat, q, normal, uvf))
    return pieces


# --- collision boxes ------------------------------------------------------------------------
def _clip_box(b):
    """A box (x0, y0, z0, x1, y1, z1) cut at the block lines: {(cx, cz): [box]}."""
    out = {}
    for cx in range(int(math.floor(b[0] / 16.0)), int(math.floor((b[3] - 1e-9) / 16.0)) + 1):
        for cz in range(int(math.floor(b[2] / 16.0)), int(math.floor((b[5] - 1e-9) / 16.0)) + 1):
            x0, x1 = max(b[0], cx * 16.0), min(b[3], cx * 16.0 + 16)
            z0, z1 = max(b[2], cz * 16.0), min(b[5], cz * 16.0 + 16)
            if x1 - x0 > 1e-6 and z1 - z0 > 1e-6:
                out.setdefault((cx, cz), []).append((x0, b[1], z0, x1, b[4], z1))
    return out


def _rects(cells_on, step):
    """Grid squares (i, k) of side `step` merged into rectangles: runs along x, stacked along z
    while a run repeats."""
    rows = {}
    for i, k in cells_on:
        rows.setdefault(k, []).append(i)
    runs = {}
    for k, xs in rows.items():
        xs.sort()
        out = []
        start = prev = xs[0]
        for x in xs[1:]:
            if x != prev + 1:
                out.append((start, prev))
                start = x
            prev = x
        out.append((start, prev))
        runs[k] = out
    rects = []
    open_ = {}
    prev = None
    for k in sorted(runs):
        if prev is not None and k != prev + 1:
            for r, k0 in open_.items():
                rects.append((r[0] * step, k0 * step, (r[1] + 1) * step, (prev + 1) * step))
            open_ = {}
        nxt = {}
        for r in runs[k]:
            nxt[r] = open_.pop(r) if r in open_ else k
        for r, k0 in open_.items():
            rects.append((r[0] * step, k0 * step, (r[1] + 1) * step, k * step))
        open_ = nxt
        prev = k
    for r, k0 in open_.items():
        rects.append((r[0] * step, k0 * step, (r[1] + 1) * step, (prev + 1) * step))
    return sorted(rects)


def _turn_boxes(shape):
    """Every collision box in the canonical frame, in sixteenths, cut at the block lines:
    {(cx, cz): [box]}. Floor (and, on the level bridge, the roof) as rectangles of the floor's
    plan; walls as the box of each short length of wall, from floor to ceiling, as a tunnel's."""
    sec = shape.sec
    fl, ce, rf = sec["floor"], sec["ceil"], sec["roof"]
    boxes = {}

    def add(b):
        for cell, bs in _clip_box(b).items():
            boxes.setdefault(cell, []).extend(bs)
    # floor and roof
    if shape.kind == "corner":
        H = shape.H
        plans = [(8 - H, 16 - 2 * H, 8 + H, 16.0)]
    else:
        step = 2.0
        on = set()
        r_hi = shape.radii["out_skin"]
        O = shape.O
        for i in range(int(math.floor((O[0] - r_hi) / step)) - 1,
                       int(math.ceil((O[0] + r_hi) / step)) + 1):
            for k in range(int(math.floor((O[1] - r_hi) / step)) - 1,
                           int(math.ceil(O[1] / step)) + 1):
                if shape.footprint((i + 0.5) * step, (k + 0.5) * step):
                    on.add((i, k))
        # merge per block, so no rectangle crosses a block line
        per = {}
        for i, k in on:
            per.setdefault((int(math.floor(i * step / 16.0)), int(math.floor(k * step / 16.0))),
                           set()).add((i, k))
        plans = []
        for cells_on in per.values():
            plans += _rects(cells_on, step)
    for x0, z0, x1, z1 in plans:
        add((x0, 0.0, z0, x1, fl, z1))
        if not shape.large:
            add((x0, ce, z0, x1, rf, z1))
    # walls: the room's straight walls whole, a curve's in lengths of about 2
    for run in shape.runs:
        P, Q = run.P, run.Q
        for i in range(len(P) - 1):
            a, b, qa, qb = P[i], P[i + 1], Q[i], Q[i + 1]
            L = max(math.hypot(b[0] - a[0], b[1] - a[1]), math.hypot(qb[0] - qa[0],
                                                                     qb[1] - qa[1]))
            n = 1 if shape.kind == "corner" else max(1, int(math.ceil(L / 2.0)))
            for j in range(n):
                t0, t1 = j / float(n), (j + 1) / float(n)
                pts = [_lerp(a, b, t0), _lerp(a, b, t1), _lerp(qa, qb, t0), _lerp(qa, qb, t1)]
                xs = [p[0] for p in pts]
                zs = [p[1] for p in pts]
                if max(xs) - min(xs) < 1e-6 or max(zs) - min(zs) < 1e-6:
                    continue
                add((min(xs), fl, min(zs), max(xs), ce, max(zs)))
    return boxes


def _lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


# --- which blocks hold a turn ---------------------------------------------------------------
_PLAYER = 9.6        # a player's width, in sixteenths
_TALL = 28.8         # and height
_MOVE = 4.8          # how far a box is swept in a tick, each way, for the scan below


def _owner(cell, members):
    """The cell that draws and collides what lies in `cell`: itself if it is one of the turn's,
    else the nearest of the turn's beside it."""
    if cell in members:
        return cell
    cx, cz = cell
    best = None
    for c in sorted(members):
        d = (abs(c[0] - cx) + abs(c[1] - cz), max(abs(c[0] - cx), abs(c[1] - cz)), c)
        if best is None or d < best:
            best = d
    return best[2]


def _scanned(box):
    """The blocks (cx, cz) the game asks for collision boxes for a moving box, in sixteenths:
    a block past the box each way, but not the four corner columns (World.getCollisionBoxes)."""
    i0 = int(math.floor(box[0] / 16.0)) - 1
    i1 = int(math.ceil(box[3] / 16.0))
    k0 = int(math.floor(box[2] / 16.0)) - 1
    k1 = int(math.ceil(box[5] / 16.0))
    out = set()
    for i in range(i0, i1 + 1):
        for k in range(k0, k1 + 1):
            if (i in (i0, i1)) and (k in (k0, k1)):
                continue
            out.add((i, k))
    return out


def _turn_cells(shape, boxes):
    """The turn's cells: the block-wide band along its centreline (every cell of a room), plus
    any cell a player inside could otherwise miss a box of. Returns (members, cells added)."""
    xs = [c[0] for c in boxes]
    zs = [c[1] for c in boxes]
    members = set()
    for cx in range(min(xs), max(xs) + 1):
        for cz in range(min(zs), max(zs) + 1):
            if any(shape.core(cx * 16 + k + 0.25, cz * 16 + j + 0.25)
                   for k in range(0, 16) for j in range(0, 16)):
                members.add((cx, cz))
    # where a player can stand, from the middle of their feet: every 2 sixteenths
    spots = [(x + 1.0, z + 1.0) for x in range(min(xs) * 16 - 16, max(xs) * 16 + 32, 2)
             for z in range(min(zs) * 16 - 16, max(zs) * 16 + 32, 2)
             if shape.spot(x + 1.0, z + 1.0)]
    by_region = {}
    for cell, bs in boxes.items():
        by_region[cell] = bs
    fl = shape.sec["floor"]
    added = []
    for _ in range(64):
        missed = None
        for x, z in spots:
            for mx in (-_MOVE, 0.0, _MOVE):
                for mz in (-_MOVE, 0.0, _MOVE):
                    pb = (x - _PLAYER / 2 + min(0.0, mx), fl - 2, z - _PLAYER / 2 + min(0.0, mz),
                          x + _PLAYER / 2 + max(0.0, mx), fl + _TALL + 8,
                          z + _PLAYER / 2 + max(0.0, mz))
                    scan = _scanned(pb)
                    for cx in range(int(math.floor(pb[0] / 16.0)),
                                    int(math.floor(pb[3] / 16.0)) + 1):
                        for cz in range(int(math.floor(pb[2] / 16.0)),
                                        int(math.floor(pb[5] / 16.0)) + 1):
                            for b in by_region.get((cx, cz), ()):
                                if (b[0] < pb[3] and b[3] > pb[0] and b[1] < pb[4]
                                        and b[4] > pb[1] and b[2] < pb[5] and b[5] > pb[2]
                                        and _owner((cx, cz), members) not in scan):
                                    missed = (cx, cz)
                                    break
                            if missed:
                                break
                        if missed:
                            break
                    if missed:
                        break
                if missed:
                    break
            if missed:
                break
        if not missed:
            return members, added
        members.add(missed)
        added.append(missed)
    raise AssertionError("%s: could not make every box reachable" % shape.kind)


# --- writing --------------------------------------------------------------------------------
def _poly_obj(name, mtl, faces, origin):
    """An OBJ of flat polygons, moved by origin (sixteenths) into its cell; quads kept, other
    polygons fanned into quads and a triangle; each face wound to its normal. Coordinates in
    blocks, v down the texture, as _sheared_obj writes them."""
    vs, vts, vns, out = [], [], [], []
    for mat, pts, normal, uvf, region in faces:
        polys = []
        if len(pts) == 4:
            polys.append(pts)
        else:
            i = 1
            while i < len(pts) - 1:
                if i + 2 < len(pts):
                    polys.append([pts[0], pts[i], pts[i + 1], pts[i + 2]])
                    i += 2
                else:
                    polys.append([pts[0], pts[i], pts[i + 1]])
                    i += 1
        for poly in polys:
            # Newell's normal, to wind the face to its outside
            nx = ny = nz = 0.0
            for j in range(len(poly)):
                p, q = poly[j], poly[(j + 1) % len(poly)]
                nx += (p[1] - q[1]) * (p[2] + q[2])
                ny += (p[2] - q[2]) * (p[0] + q[0])
                nz += (p[0] - q[0]) * (p[1] + q[1])
            # wound so its right-hand normal points out, as _sheared_obj winds a box's faces
            if nx * normal[0] + ny * normal[1] + nz * normal[2] < 0:
                poly = poly[::-1]
            refs = []
            for x, y, z in poly:
                u, v = uvf(x, y, z, region)
                vs.append("v %.5f %.5f %.5f" % ((x - origin[0]) / 16.0, y / 16.0,
                                                (z - origin[1]) / 16.0))
                vts.append("vt %.5f %.5f" % (min(1, max(0, u / 16.0)), min(1, max(0, v / 16.0))))
                vns.append("vn %.5f %.5f %.5f" % normal)
                refs.append(len(vs))
            out.append((mat, refs))
    lines = ["# Generated by dev-env-utils/scripts/gen_transit_airside.py -- do not edit",
             "mtllib %s.mtl" % mtl, "o %s" % name] + vs + vts + vns
    current = None
    for mat, refs in sorted(out, key=lambda f: f[0]):
        if mat != current:
            lines.append("usemtl " + mat)
            current = mat
        lines.append("f " + " ".join("%d/%d/%d" % (r, r, r) for r in refs))
    return "\n".join(lines) + "\n"


_FACE_STEPS = {"north": 0, "east": 1, "south": 2, "west": 3}


def _icon_box(a, b, width, y0, y1, tex):
    """A JSON element over plan points a-b (already in the icon's sixteenths), `width` across,
    turned about y to the nearest step a JSON element allows (22.5 degrees, at most 45 either
    way, so a box nearer z than x runs along z). Minecraft turns +x towards -z for a positive
    angle."""
    mx, mz = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
    dx, dz = b[0] - a[0], b[1] - a[1]
    L = math.hypot(dx, dz)
    if L < 1e-6:
        return None

    def fold(deg):
        while deg > 90:
            deg -= 180
        while deg <= -90:
            deg += 180
        return deg
    ang_x = fold(math.degrees(math.atan2(-dz, dx)))
    if abs(ang_x) <= 45 + 1e-6:
        ang = ang_x
        frm, to = (mx - L / 2, y0, mz - width / 2), (mx + L / 2, y1, mz + width / 2)
    else:
        ang = fold(math.degrees(math.atan2(dx, dz)))
        frm, to = (mx - width / 2, y0, mz - L / 2), (mx + width / 2, y1, mz + L / 2)
    ang = round(ang / 22.5) * 22.5
    el = B(frm, to, tex, ALL)
    for face in el["faces"].values():
        u = face["uv"]
        face["uv"] = [0, 0, round(min(16, max(0.5, abs(u[2] - u[0]))), 4),
                      round(min(16, max(0.5, abs(u[3] - u[1]))), 4)]
    if ang:
        el["rotation"] = {"origin": [round(mx, 4), round(y0, 4), round(mz, 4)], "axis": "y",
                          "angle": ang}
    return el


def _icon(shape):
    """The item: the turn seen from above, its floor and walls (no roof) shrunk to fit a block,
    as JSON elements. A JSON element turns only in steps of 22.5 degrees, so a curve is drawn as
    chords centred on those angles (half chords at its ends)."""
    sec = shape.sec
    if shape.kind == "corner":
        H = shape.H
        bx, bz = (8 - H, 8 + H), (16 - 2 * H, 16.0)
    else:
        r = shape.radii["out_skin"]
        O = shape.O
        bx = (O[0] - r, O[0] + (r if shape.kind == "uturn" else 0))
        bz = (O[1] - r, O[1])
    f = 30.0 / max(bx[1] - bx[0], bz[1] - bz[0])
    cx, cz = (bx[0] + bx[1]) / 2, (bz[0] + bz[1]) / 2

    def t(p):
        return (8 + (p[0] - cx) * f, 8 + (p[1] - cz) * f)
    fl = round(max(0.6, sec["floor"] * f), 4)
    top = round(fl + (sec["ceil"] - sec["floor"]) * f, 4)
    els = []
    if shape.kind == "corner":
        H = shape.H
        a, b = t((8 - H, 16 - 2 * H)), t((8 + H, 16.0))
        els.append(B((a[0], 0, a[1]), (b[0], fl, b[1]), "floor", ALL,
                     uv={k: [0, 0, 16, 16] for k in ALL}))
        for run in shape.runs:
            for i in range(len(run.P) - 1):
                pts = [t(p) for p in (run.P[i], run.P[i + 1], run.Q[i], run.Q[i + 1])]
                xs = [p[0] for p in pts]
                zs = [p[1] for p in pts]
                if max(xs) - min(xs) < 1e-6 or max(zs) - min(zs) < 1e-6:
                    continue
                els.append(B((min(xs), fl, min(zs)), (max(xs), top, max(zs)), "skin", ALL,
                             uv={k: [0, 0, 4, 4] for k in ALL}))
    else:
        O, R = shape.O, shape.R
        half, wt = sec["half"], sec["wt"]
        step = math.radians(11.25)
        n = int(round(math.degrees(shape.th_end) / 22.5))
        for j in range(n + 1):
            mid = math.radians(22.5 * j)
            lo = max(-1.0, (0.0 - mid) / step)
            hi = min(1.0, (shape.th_end - mid) / step)
            if hi - lo < 1e-6:
                continue
            tan = (math.sin(mid), -math.cos(mid))
            for r_o, width, tex, y0, y1 in ((R + half, 2 * half, "floor", 0.004 * (j % 2), fl),
                                            (R + half - wt / 2, wt, "skin", fl, top),
                                            (R - half + wt / 2, wt, "skin", fl, top)):
                if r_o <= 0:
                    continue
                if tex == "floor":
                    c = _arc_point(O, R * math.cos(step), mid)
                else:
                    c = _arc_point(O, r_o * math.cos(step), mid)
                hl = r_o * math.sin(step)
                a = (c[0] + tan[0] * hl * lo, c[1] + tan[1] * hl * lo)
                b = (c[0] + tan[0] * hl * hi, c[1] + tan[1] * hl * hi)
                el = _icon_box(t(a), t(b), width * f, y0, y1, tex)
                if el:
                    els.append(el)
    m = lc.model(JB_TEX, els, ao=False)
    m["display"] = gui_display(0.34, 0.0)
    return m


def jet_bridge_turns():
    """The six turns: their per-cell OBJs, blockstates and items, and JetBridgeTurnShape.java."""
    folder = C.M("x").split(":", 1)[1].rsplit("/", 1)[0]
    java_shapes = []
    for large in (False, True):
        for kind in TURN_KINDS:
            shape = _Shape(kind, large)
            reg = "airport_jet_bridge_%s%s" % ("large_" if large else "", kind)
            mtl = "jet_bridge_large" if large else "jet_bridge_slope"
            boxes = _turn_boxes(shape)
            members, added = _turn_cells(shape, boxes)
            a_cell, b_cell = shape.end_a[0], shape.end_b[0]
            assert a_cell in members and b_cell in members, (reg, a_cell, b_cell)
            order = [a_cell] + sorted((c for c in members if c != a_cell),
                                      key=lambda c: (c[1], c[0]))
            index = {c: i for i, c in enumerate(order)}
            # the models: every piece to the cell that holds it
            per_cell = {}
            for region, pieces in _cut_faces(_turn_faces(shape)).items():
                owner = _owner(region, members)
                for mat, pts, normal, uvf in pieces:
                    per_cell.setdefault(owner, []).append(
                        (mat, pts, normal, uvf, (region[0] * 16.0, region[1] * 16.0)))
            models = {}
            for c in order:
                assert per_cell.get(c), (reg, c)
                name = "%s_c%d" % (reg, index[c])
                C.extra["models/block/%s/%s.obj" % (folder, name)] = _poly_obj(
                    name, mtl, per_cell.get(c, []), (c[0] * 16.0, c[1] * 16.0))
                models[c] = "csm:%s/%s.obj" % (folder, name)
            # collision, per cell, in the cell's own sixteenths
            cell_boxes = {c: [] for c in order}
            for region, bs in sorted(boxes.items()):
                owner = _owner(region, members)
                ox, oz = owner[0] * 16.0, owner[1] * 16.0
                for b in bs:
                    cell_boxes[owner].append(tuple(round(v, 4) for v in (
                        b[0] - ox, b[1], b[2] - oz, b[3] - ox, b[4], b[5] - oz)))
            # the blockstate: each cell's model turned with the turn, and the tunnel's own end
            # frame at an end that nothing continues from
            frame = ("csm:%s/airport_jet_bridge_large_tunnel_end_behind.obj" % folder if large
                     else C.M("airport_jet_bridge_tunnel_end_behind"))
            left_steps = (_FACE_STEPS["south"] - _FACE_STEPS[shape.end_b[1]]) % 4
            rules = []
            for facing, y in FACINGS:
                for left in (False, True):
                    steps = (y // 90 + (left_steps if left else 0)) % 4
                    for c in order:
                        rules.append({"when": {"facing": facing, "left": str(left).lower(),
                                               "cell": str(index[c])},
                                      "apply": _turned(models[c], steps * 90)})
                    for cell, face in shape.ends():
                        fy = (_FACE_STEPS[face] - _FACE_STEPS["south"] + steps) % 4 * 90
                        rules.append({"when": {"facing": facing, "left": str(left).lower(),
                                               "cell": str(index[cell]), "frame": "true"},
                                      "apply": _turned(frame, fy)})
            enum = TURN_ENUM[kind] + ("_LARGE" if large else "")
            names = _large_name(TURN_NAMES[kind]) if large else TURN_NAMES[kind]
            C.add(reg, 'new BlockJetBridgeTurn("%s", JetBridgeTurnShape.%s)' % (reg, enum),
                  names, {}, {"multipart": rules}, item=_icon(shape), tab=TAB)
            java_shapes.append((enum, large, order, index[b_cell], shape.end_b[1], cell_boxes,
                                len(added)))
    return java_shapes


TURN_NAMES = {
    "corner": ("Jet Bridge (Corner)", "Fluggastbrücke (Ecke)", "Pasarela de Embarque (Esquina)",
               "Flygbrygga (Hörn)"),
    "turn": ("Jet Bridge (Curved Turn)", "Fluggastbrücke (Kurve)",
             "Pasarela de Embarque (Curva)", "Flygbrygga (Kurva)"),
    "uturn": ("Jet Bridge (U-Turn)", "Fluggastbrücke (Kehre)",
              "Pasarela de Embarque (Giro en U)", "Flygbrygga (U-sväng)"),
}
def _large_name(names):
    """A large piece's name from the level one's, as the other large pieces are named."""
    en, de, es, sv = names
    return ("Large " + en, "Große " + de,
            es.replace("Pasarela de Embarque", "Pasarela de Embarque Grande"), "Stor " + sv)


def turn_java(java_shapes):
    """JetBridgeTurnShape.java: each shape's cells, ends and collision boxes."""
    lines = [
        "package com.micatechnologies.minecraft.csm.transit.airport;",
        "",
        "import net.minecraft.util.EnumFacing;",
        "",
        "/**",
        " * The jet bridge turns' cells and collision boxes. GENERATED by",
        " * {@code dev-env-utils/scripts/gen_transit_airside.py} from the same walls and floors the",
        " * turns' models are drawn from, so placement, collision and drawing cannot disagree. Do not",
        " * edit; change the generator and re-run it.",
        " *",
        " * <p>Each shape is a right-hand turn in its own frame: cell 0 is end A, the entry, at",
        " * (0, 0), entered through its south face heading north (the model's north); {@code endB}",
        " * is the exit, open on {@code faceB}. Cells are (x, z) in blocks, east and south positive.",
        " * A left-hand turn is the same cells turned until end B's face looks south, entered at",
        " * end B. Boxes are each cell's own, in sixteenths of that cell, facing north.</p>",
        " *",
        " * @since 2026.10",
        " */",
        "public enum JetBridgeTurnShape {",
    ]
    entries = []
    for enum, large, order, b_index, b_face, cell_boxes, _added in java_shapes:
        cells = ";".join("%d,%d" % c for c in order)
        boxes = "|".join(";".join(",".join("%g" % v for v in b) for b in cell_boxes[c])
                         for c in order)
        entries.append('  %s(%s, "%s", %d, EnumFacing.%s,\n      "%s")'
                       % (enum, "true" if large else "false", cells, b_index, b_face.upper(),
                          boxes))
    lines.append(",\n".join(entries) + ";")
    lines += [
        "",
        "  private final boolean large;",
        "  private final int[][] cells;",
        "  private final int endB;",
        "  private final EnumFacing faceB;",
        "  private final double[][][] boxes;",
        "",
        "  JetBridgeTurnShape(boolean large, String cells, int endB, EnumFacing faceB,",
        "      String boxes) {",
        "    this.large = large;",
        "    String[] c = cells.split(\";\");",
        "    this.cells = new int[c.length][];",
        "    for (int i = 0; i < c.length; i++) {",
        "      String[] xz = c[i].split(\",\");",
        "      this.cells[i] = new int[]{Integer.parseInt(xz[0]), Integer.parseInt(xz[1])};",
        "    }",
        "    this.endB = endB;",
        "    this.faceB = faceB;",
        "    String[] perCell = boxes.split(\"\\\\|\", -1);",
        "    this.boxes = new double[perCell.length][][];",
        "    for (int i = 0; i < perCell.length; i++) {",
        "      String[] b = perCell[i].isEmpty() ? new String[0] : perCell[i].split(\";\");",
        "      this.boxes[i] = new double[b.length][];",
        "      for (int j = 0; j < b.length; j++) {",
        "        String[] v = b[j].split(\",\");",
        "        this.boxes[i][j] = new double[v.length];",
        "        for (int k = 0; k < v.length; k++) {",
        "          this.boxes[i][j][k] = Double.parseDouble(v[k]);",
        "        }",
        "      }",
        "    }",
        "  }",
        "",
        "  /** Whether this is a turn of the large bridge. */",
        "  public boolean isLarge() {",
        "    return large;",
        "  }",
        "",
        "  /** How many cells the turn has. */",
        "  public int getCellCount() {",
        "    return cells.length;",
        "  }",
        "",
        "  /** A cell's place in the right-hand turn's frame: x (east), z (south), in blocks. */",
        "  public int[] getCell(int index) {",
        "    return cells[index];",
        "  }",
        "",
        "  /** End B, the right-hand turn's exit (end A is cell 0, open to the south). */",
        "  public int getEndB() {",
        "    return endB;",
        "  }",
        "",
        "  /** The face end B is open on, in the right-hand turn's frame. */",
        "  public EnumFacing getFaceB() {",
        "    return faceB;",
        "  }",
        "",
        "  /** A cell's collision boxes, in its own sixteenths, facing north. */",
        "  public double[][] getBoxes(int index) {",
        "    return boxes[index];",
        "  }",
        "}",
    ]
    return "\n".join(lines) + "\n"


# ------------------------------------------------------------------------------------------
# Lang the Java reads
# ------------------------------------------------------------------------------------------
C.add_lang("csm.transit.stand", ("Stand %s", "Standplatz %s", "Puesto %s", "Plats %s"))
C.add_lang("csm.transit.airfield_sign", ("Sign reads: %s", "Schild zeigt: %s",
                                         "El letrero dice: %s", "Skylten visar: %s"))
C.add_lang("csm.transit.airfield_lights", (
    "Airfield lights %s (%s on this circuit)", "Flugfeldbefeuerung %s (%s in diesem Kreis)",
    "Luces del aeródromo %s (%s en este circuito)", "Flygfältsljus %s (%s i denna krets)"))
C.add_lang("csm.transit.airfield_lights.on", ("on", "ein", "encendidas", "på"))
C.add_lang("csm.transit.airfield_lights.off", ("off", "aus", "apagadas", "av"))
C.add_lang("csm.transit.jet_bridge_slope", (
    "Slope: %s", "Neigung: %s", "Pendiente: %s", "Lutning: %s"))
C.add_lang("csm.transit.jet_bridge_slope.down8", (
    "down 1 block over 8", "1 Block abwärts auf 8", "baja 1 bloque en 8", "ned 1 block på 8"))
C.add_lang("csm.transit.jet_bridge_slope.down4", (
    "down 1 block over 4", "1 Block abwärts auf 4", "baja 1 bloque en 4", "ned 1 block på 4"))
C.add_lang("csm.transit.jet_bridge_slope.up8", (
    "up 1 block over 8", "1 Block aufwärts auf 8", "sube 1 bloque en 8", "upp 1 block på 8"))
C.add_lang("csm.transit.jet_bridge_slope.up4", (
    "up 1 block over 4", "1 Block aufwärts auf 4", "sube 1 bloque en 4", "upp 1 block på 4"))
C.add_lang("csm.transit.jet_bridge_turn.blocked", (
    "No room for the turn: blocked at %s, %s, %s", "Kein Platz für die Kurve: blockiert bei %s, %s, %s",
    "No hay espacio para el giro: bloqueado en %s, %s, %s", "Inget utrymme för svängen: blockerad vid %s, %s, %s"))
C.add_lang("csm.transit.jet_bridge_turn.hand", ("Turns %s", "Biegt %s ab", "Gira a la %s", "Svänger %s"))
C.add_lang("csm.transit.jet_bridge_turn.left", ("left", "links", "izquierda", "vänster"))
C.add_lang("csm.transit.jet_bridge_turn.right", ("right", "rechts", "derecha", "höger"))

register_textures()
airfield_lights()
wind_sock()
airfield_signs()
airfield_mast()
stand_sign()
stand_sign(large=True)
ground_equipment()
jet_bridge()
jet_bridge_slope()
jet_bridge_large()
TURN_SHAPES = jet_bridge_turns()


def _main():
    """The catalogue's own run, then JetBridgeTurnShape.java, written or (--check) compared."""
    rc = C.main()
    if "--fragments" in sys.argv:
        return rc
    path = os.path.join(REPO, TURN_JAVA_REL)
    text = turn_java(TURN_SHAPES)
    if "--check" in sys.argv:
        try:
            with open(path, encoding="utf-8") as fh:
                same = fh.read() == text
        except IOError:
            same = False
        if not same:
            print("out of date (re-run without --check):\n  " + TURN_JAVA_REL)
            return 1
        return rc
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        fh.write(text)
    return rc


if __name__ == "__main__":
    sys.exit(_main())
