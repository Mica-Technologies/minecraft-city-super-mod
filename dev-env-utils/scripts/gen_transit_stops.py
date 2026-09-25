#!/usr/bin/env python3
"""Every asset the Transit tab's bus stops ship: the agency flag signs with their route plates,
the timetable and route map cases, the real-time arrival display and the curb plaque.

    python dev-env-utils/scripts/gen_transit_stops.py
    python dev-env-utils/scripts/gen_transit_stops.py --check
    python dev-env-utils/scripts/gen_transit_stops.py --fragments   # tab lines to paste

A stop is built on the road sign system, which is why Transit requires Roads: the flag, the
arrival display and the two poster cases are road signs (Roads' AbstractBlockSign, through
BlockTrafficSign), each a length of Roads' sign post -- the same five bars every road sign's
model carries, at z 0.5 to 3.5 behind the front of the block -- with its piece on the front of
it. A player builds a stop as a signed post: sign posts, then a case and the display, the flag on
top. The sign system gives them all the rest: the facing taken from the sign below, the extension
post onto a slab, and the three shift models every road sign has, which this script writes for
each piece:

    none          as drawn below: the piece on the front of the post
    setback       the whole model 12.5 back, the post reaching the back of the block, to stand in
                  line with a signal arm's hardware or a span wire
    back_to_back  the piece alone, 28.3 back, on the far side of the partner sign's post

28.3 rather than the 28.5 the other road signs use: this way the face stands 0.2 clear of the end
of the partner's post (SignFaceDepthTest's gap) instead of a hundredth of a unit in front of it,
and needs no separate art sliver. SignShiftModelTest holds these models to the same rules as
Roads' own (their models are named sign_*).

Every agency is invented: CITYLINE (teal and yellow, the fare machine's livery), RIVERWAY,
VERDANT and EMBERLINE. The flag is printed on both faces and stands on top of its block and
above it, like the road signs' tall plates; its three route plates hang under it in the block,
where a click reaches them. Flag and plates hang off the side of the post, NYC style, reaching
to the reader's right or left: six flag models (sign_flag and sign_flag_left, each with its two
shifts), which the flag's `hang` property picks, its `shift` variants left empty. The flag models are shared by the four agencies: the blockstate
fills slot 1 with the agency's flag, and each route plate's slot (p1..p3) with the agency's plate
while it carries a number (`route1`..`route3`, from TileEntityBusStopFlag) or with a clear
texture while it does not -- so the plates are baked in every shift model without a model per
combination. The number itself is drawn by TileEntityBusStopFlagRenderer on both faces, at the
plate middles written into BlockBusStopFlag (PLATE_*). The arrival display's screen is dark in
the model; its text is the renderer's.

Faces that carry a picture use a window of their texture at the face's own aspect, so a texel is
square: the flag art is 49 x 64 texels on a 10 x 13 plate.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_stops.py", "transit/stops", "transit/stops", assets=ASSETS)
TAB = "CsmTabTransit"
box = lc.box

WHITE = (240, 242, 240)
BLACK = (18, 20, 22)
STEEL = (122, 128, 132)
GRAPHITE = (48, 52, 56)
CHARCOAL = (34, 36, 40)
ISA_BLUE = (18, 82, 160)
LED_OFF = (40, 26, 8)

# ------------------------------------------------------------------------------------------
# Agencies
# ------------------------------------------------------------------------------------------
AGENCIES = [
    # id, name on the flag, primary, accent, layout
    ("cityline", "CITYLINE", (14, 94, 111), (244, 196, 40), "band",
     ("Bus Stop Flag (CITYLINE)", "Bushaltestellenschild (CITYLINE)",
      "Señal de Parada de Autobús (CITYLINE)", "Busshållplatsskylt (CITYLINE)")),
    ("riverway", "RIVERWAY", (31, 58, 115), (240, 138, 36), "stripe",
     ("Bus Stop Flag (RIVERWAY)", "Bushaltestellenschild (RIVERWAY)",
      "Señal de Parada de Autobús (RIVERWAY)", "Busshållplatsskylt (RIVERWAY)")),
    ("verdant", "VERDANT", (36, 116, 60), (236, 240, 232), "panel",
     ("Bus Stop Flag (VERDANT)", "Bushaltestellenschild (VERDANT)",
      "Señal de Parada de Autobús (VERDANT)", "Busshållplatsskylt (VERDANT)")),
    ("emberline", "EMBERLINE", (176, 36, 32), (56, 58, 62), "split",
     ("Bus Stop Flag (EMBERLINE)", "Bushaltestellenschild (EMBERLINE)",
      "Señal de Parada de Autobús (EMBERLINE)", "Busshållplatsskylt (EMBERLINE)")),
]

# ------------------------------------------------------------------------------------------
# Pixel art
# ------------------------------------------------------------------------------------------
def bus_icon(img, x, y, colour, window, wheel=BLACK):
    """A bus seen side on, facing left, 26 x 13 texels with (x, y) its top-left: a body with
    rounded corners, a row of windows, the windscreen and front door, two wheels."""
    lc.rect(img, x + 1, y, x + 25, y + 11, colour)
    lc.rect(img, x, y + 1, x + 26, y + 10, colour)
    # windscreen, door, then the side windows
    lc.rect(img, x + 1, y + 2, x + 4, y + 7, window)
    lc.rect(img, x + 5, y + 2, x + 8, y + 10, window)
    lc.rect(img, x + 6, y + 2, x + 7, y + 10, colour)
    for wx in range(10, 24, 4):
        lc.rect(img, x + wx, y + 2, x + wx + 3, y + 6, window)
    # destination sign over the windscreen
    lc.rect(img, x + 1, y + 1, x + 4, y + 2, lc.shade(window, 0.8))
    for cx in (x + 6.5, x + 20.5):
        lc.disc(img, cx, y + 11, 2.2, wheel)
        lc.disc(img, cx, y + 11, 0.9, lc.shade(colour, 1.0) if wheel == BLACK else colour)


def small_bus(img, x, y, colour, window):
    """A bus 9 x 6 texels, for a route plate."""
    lc.rect(img, x, y, x + 9, y + 5, colour)
    lc.rect(img, x + 1, y + 1, x + 3, y + 3, window)
    lc.rect(img, x + 4, y + 1, x + 8, y + 3, window)
    lc.rect(img, x + 1, y + 5, x + 3, y + 6, colour)
    lc.rect(img, x + 6, y + 5, x + 8, y + 6, colour)


ISA = [
    "....##...",
    "....##...",
    ".........",
    "....#....",
    "....####.",
    "..#.#....",
    ".#..####.",
    ".#.....#.",
    "..###..##",
]


def isa(img, x, y):
    """The accessibility symbol: a white figure in a wheelchair on blue, 11 x 11 texels."""
    lc.rect(img, x, y, x + 11, y + 11, ISA_BLUE)
    for r, line in enumerate(ISA):
        for c, ch in enumerate(line):
            if ch == "#":
                lc.rect(img, x + 1 + c, y + 1 + r, x + 2 + c, y + 2 + r, WHITE)


FLAG_W, FLAG_H = 49, 64  # the flag art's window on its 64 px texture


def flag_art(agency):
    """A flag's face. Four layouts, one per agency, so the four read as four agencies."""
    _, name, primary, accent, layout, _ = agency
    img = lc.fill(WHITE, 64, 2, 50 + [a[1] for a in AGENCIES].index(name))
    w, h = FLAG_W, FLAG_H
    cx = w / 2.0
    if layout == "band":
        lc.rect(img, 0, 0, w, 12, primary)
        lc.draw_text_centred(img, name, cx, 4, accent)
        lc.rect(img, 0, 12, w, 14, accent)
        bus_icon(img, int(cx - 13), 17, primary, WHITE)
        lc.draw_text_centred(img, "BUS", cx, 32, primary, 2)
        lc.draw_text_centred(img, "STOP", cx, 43, primary, 2)
        lc.rect(img, 0, 55, w, h, primary)
        isa(img, w - 13, 53)
    elif layout == "stripe":
        lc.rect(img, 0, 0, w, h, primary)
        lc.rect(img, 0, 0, 5, h, accent)
        lc.draw_text_centred(img, name, cx + 2.5, 4, WHITE)
        lc.rect(img, 8, 11, w - 3, 12, accent)
        bus_icon(img, int(cx - 10), 16, WHITE, primary)
        lc.draw_text_centred(img, "BUS", cx + 2.5, 31, WHITE, 2)
        lc.draw_text_centred(img, "STOP", cx + 2.5, 42, WHITE, 2)
        lc.rect(img, 8, 54, w - 16, 55, accent)
        isa(img, w - 14, 51)
    elif layout == "panel":
        lc.rect(img, 0, 0, w, h, primary)
        lc.draw_text_centred(img, name, cx, 4, WHITE)
        lc.rect(img, 4, 12, w - 4, 52, accent)
        for (px, py) in ((4, 12), (w - 5, 12), (4, 51), (w - 5, 51)):
            lc.rect(img, px, py, px + 1, py + 1, primary)
        bus_icon(img, int(cx - 13), 15, primary, accent)
        lc.draw_text_centred(img, "BUS", cx, 30, primary, 2)
        lc.draw_text_centred(img, "STOP", cx, 41, primary, 2)
        isa(img, w - 15, 53)
        lc.rect(img, 0, 60, w - 16, h, lc.shade(primary, 0.8))
    else:  # split
        lc.rect(img, 0, 0, w, 31, primary)
        lc.draw_text_centred(img, name, cx, 4, WHITE)
        bus_icon(img, int(cx - 13), 14, WHITE, primary, wheel=accent)
        lc.rect(img, 0, 31, w, 34, accent)
        lc.draw_text_centred(img, "BUS", cx - 5, 37, primary, 2)
        lc.draw_text_centred(img, "STOP", cx - 5, 49, primary, 2)
        isa(img, w - 13, 44)
    # the sheeting's edge, a texel of pale grey all round
    lc.frame(img, 0, 0, w, h, (206, 210, 212))
    return img


BULLET_W, BULLET_H = 64, 16  # a route plate's window: 10 x 2.5 units


def bullet_art(agency):
    """A route plate: the agency colour, a white edge and a small bus; the renderer draws the
    number in the middle."""
    _, name, primary, accent, _, _ = agency
    img = lc.fill(primary, 64, 2, 60 + [a[1] for a in AGENCIES].index(name))
    lc.frame(img, 0, 0, BULLET_W, BULLET_H, WHITE)
    small_bus(img, 4, 5, WHITE, primary)
    lc.rect(img, 15, 3, 16, 13, lc.shade(WHITE, 0.9))
    return img


# --- the posters ---------------------------------------------------------------------------
TT_W, TT_H = 51, 64      # timetable window (8 x 10 units)
MAP_W, MAP_H = 64, 57    # route map window (9 x 8 units)
HEAD = (38, 50, 64)


def timetable():
    img = lc.fill((246, 246, 240), 64, 2, 41)
    w = TT_W
    lc.rect(img, 0, 0, w, 11, HEAD)
    lc.draw_text_centred(img, "TIMETABLE", w / 2.0, 4, WHITE)
    lc.draw_text(img, "WEEKDAYS", 5, 13, (110, 116, 122))
    minutes = ["05 25 45", "00 20 40", "10 30 50", "05 25 45", "15 35 55", "00 30",
               "10 40"]
    for i, row in enumerate(minutes):
        y = 19 + i * 6
        if i % 2 == 0:
            lc.rect(img, 3, y - 1, w - 3, y + 5, (226, 232, 236))
        lc.rect(img, 3, y - 1, 13, y + 5, lc.shade(HEAD, 1.35) if i % 2 else HEAD)
        lc.draw_text_centred(img, str(6 + i * 2), 8, y, WHITE)
        lc.draw_text(img, row, 15, y, (40, 44, 48))
    glass_sheen(img, w, TT_H)
    return img


def route_map():
    img = lc.fill((242, 238, 226), 64, 2, 42)
    w, h = MAP_W, MAP_H
    # a park and a river, the ground the lines run over
    lc.rect(img, 38, 14, 56, 28, (196, 222, 180))
    for x in range(0, w):
        y = int(37 + (x - 30) * 0.35)
        lc.rect(img, x, y, x + 1, y + 4, (170, 204, 230))
    lc.rect(img, 0, 0, w, 10, HEAD)
    lc.draw_text_centred(img, "ROUTE MAP", w / 2.0, 3, WHITE)
    lines = [((14, 94, 111), [(5, 20), (22, 20), (32, 30), (58, 30)]),
             ((240, 138, 36), [(10, 42), (10, 36), (32, 30), (32, 14)]),
             ((36, 116, 60), [(5, 41), (20, 41), (32, 30), (45, 41), (58, 41)])]
    for colour, pts in lines:
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            steps = max(abs(x1 - x0), abs(y1 - y0))
            for s in range(steps + 1):
                x = round(x0 + (x1 - x0) * s / float(steps))
                y = round(y0 + (y1 - y0) * s / float(steps))
                lc.rect(img, x - 1, y - 1, x + 1, y + 1, colour)
        for x, y in pts:
            lc.rect(img, x - 1, y - 1, x + 2, y + 2, WHITE)
            lc.frame(img, x - 2, y - 2, x + 3, y + 3, colour)
    # the interchange, and "you are here"
    lc.rect(img, 30, 28, 35, 33, WHITE)
    lc.frame(img, 29, 27, 36, 34, BLACK)
    lc.disc(img, 22.5, 20.5, 3.2, (220, 40, 40))
    lc.disc(img, 22.5, 20.5, 1.4, WHITE)
    # legend
    for i, (colour, num) in enumerate((((14, 94, 111), "12"), ((240, 138, 36), "40"),
                                       ((36, 116, 60), "7"))):
        x = 6 + i * 18
        lc.rect(img, x, 46, x + 13, 52, colour)
        lc.draw_text_centred(img, num, x + 6.5, 47, WHITE)
    glass_sheen(img, w, h)
    return img


def glass_sheen(img, w, h):
    """The case's glass: two faint diagonal highlights across the poster."""
    px = img.load()
    for k in (0.3, 0.42):
        for y in range(h):
            x0 = int(w * k + y * 0.55 - h * 0.25)
            for x in range(x0, x0 + (3 if k == 0.3 else 2)):
                if 0 <= x < w:
                    r, g, b, a = px[x, y]
                    px[x, y] = lc.clamp((r + (255 - r) * 0.14, g + (255 - g) * 0.14,
                                         b + (255 - b) * 0.14)) + (a,)


DISPLAY_W, DISPLAY_H = 64, 29  # the display's front: 14 x 6.4 units, 4.57 texels a unit


def display_front():
    """The display's face: charcoal housing, a black screen with a faint dot grid (the renderer
    lights the text), a small stop-info label under it."""
    img = lc.fill(CHARCOAL, 64, 2, 43)
    lc.rect(img, 3, 3, 61, 23, BLACK)
    px = img.load()
    for y in range(4, 22, 2):
        for x in range(4, 60, 2):
            px[x, y] = LED_OFF + (255,)
    lc.frame(img, 2, 2, 62, 24, lc.shade(CHARCOAL, 1.4))
    lc.draw_text(img, "NEXT BUS", 4, 24, (150, 156, 160))
    lc.rect(img, 54, 25, 60, 27, (40, 170, 90))
    return img


PLAQUE = (142, 104, 58)


def plaque():
    """A cast bronze plate for the curb: a raised border, BUS STOP and a bus, drawn upside down
    because the plate's top face shows the texture turned 180 to the player who placed it."""
    from PIL import Image
    img = lc.fill(PLAQUE, 64, 5, 44)
    lc.frame(img, 2, 2, 62, 62, lc.shade(PLAQUE, 1.35), 2)
    lc.frame(img, 4, 4, 60, 60, lc.shade(PLAQUE, 0.7), 1)
    raised = lc.shade(PLAQUE, 1.3)
    shadow = lc.shade(PLAQUE, 0.62)
    for dx, dy, col in ((1, 1, shadow), (0, 0, raised)):
        bus_icon(img, 19 + dx, 12 + dy, col, lc.shade(PLAQUE, 0.85), wheel=col)
        lc.draw_text_centred(img, "BUS", 32 + dx, 30 + dy, col, 2)
        lc.draw_text_centred(img, "STOP", 32 + dx, 44 + dy, col, 2)
    return img.transpose(Image.ROTATE_180)


def clear():
    """A texture with nothing in it: the route plate slots take it while the plate is not there."""
    from PIL import Image
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def register_textures():
    tex = {
        "clamp": lc.fill(STEEL, 16, 4, 5),
        "case": lc.fill(GRAPHITE, 16, 3, 7),
        "timetable": timetable(),
        "route_map": route_map(),
        "display_housing": lc.fill(CHARCOAL, 16, 3, 8),
        "display_front": display_front(),
        "plaque": plaque(),
        "plaque_edge": lc.fill(lc.shade(PLAQUE, 0.8), 16, 4, 9),
    }
    tex["none"] = clear()
    for a in AGENCIES:
        tex["flag_" + a[0]] = flag_art(a)
        tex["route_" + a[0]] = bullet_art(a)
    for name, img in tex.items():
        C.texture(name)(lambda img=img: img)


# ------------------------------------------------------------------------------------------
# Geometry (every piece faces north, on the road sign post at the front of the block)
# ------------------------------------------------------------------------------------------
POST_TEX = "csm:blocks/trafficsigns/absolutely_nothing_sign"   # every road sign's post and metal
SIGN_POLE = "csm:trafficsigns/sign_pole"                       # the extension post below a sign
SHIFTS = (("", 0.0), ("_setback", 12.5), ("_back_to_back", 28.3))   # BusStopSigns.SHIFT_Z
FACINGS = (("n", 0), ("nw", 45), ("w", 90), ("sw", 135),
           ("s", 180), ("se", 225), ("e", 270), ("ne", 315))


def face(tex, uv):
    return {"texture": "#" + tex, "uv": [round(v, 4) for v in uv]}


def win(w_px, h_px):
    """The uv of a picture window w_px x h_px texels at the top-left of a 64 px texture."""
    return [0, 0, round(w_px / 4.0, 4), round(h_px / 4.0, 4)]


def sign_post():
    """The road sign post, the five nested bars every road sign's model stands on, its front at
    z 0.5 -- the same bars gen_route_markers.py copies from the shared sign models, so a bus stop
    piece stands on exactly the post its neighbours in the column do.

    One difference: the bars' ends all lie on the block's top and bottom, and there every bar
    paints one texel of the metal rather than its own stretch of it. The ends are coplanar by
    design (the post is one solid), and painted alike model_depth has nothing to separate;
    painted differently it would stair-step them past the block, into the post of the sign
    above and below."""
    bars = ((7.25, 8.75, 0.75, 3.25, None), (7.0, 9.0, 1.0, 3.0, None),
            (6.5, 9.5, 1.5, 2.5, None), (6.75, 9.25, 1.25, 2.75, None),
            (7.5, 8.5, 0.5, 3.5, "north"))
    els = []
    for x0, x1, z0, z1, drop in bars:
        faces = {}
        for side in ("north", "east", "south", "west", "up", "down"):
            if side == drop:
                continue
            uv = [1, 1, 1.01, 1.01] if side in ("up", "down") else [0, 0, 4, 16]
            faces[side] = {"uv": uv, "texture": "#0"}
        els.append({"from": [x0, 0, z0], "to": [x1, 16, z1], "faces": faces})
    return els


def band(y0, y1):
    """A clamp band round the sign post, 0.3 clear of it at the sides and front and 0.25 at the
    back."""
    return box([6.2, y0, 0.2], [9.8, y1, 3.75], "clamp")


def shifted(els, dz):
    """Elements moved dz back, and held inside the z 32 an element may reach: back to back, a
    band's back comes to 32.05 and is drawn to 32, still clear of the partner's post."""
    out = []
    for el in els:
        el = dict(el, **{"from": [el["from"][0], el["from"][1],
                                  min(32.0, round(el["from"][2] + dz, 4))],
                         "to": [el["to"][0], el["to"][1], min(32.0, round(el["to"][2] + dz, 4))]})
        out.append(el)
    return out


def shift_models(name, textures, piece, display=None):
    """The three shift models of one piece: the piece and the post as drawn, both 12.5 back,
    and the piece alone 28.3 back."""
    models = {}
    for suffix, dz in SHIFTS:
        els = shifted(piece, dz)
        if suffix != "_back_to_back":
            els += shifted(sign_post(), dz)
        m = lc.model(textures, els)
        if display and not suffix:
            m["display"] = display
        models[name + suffix] = m
    return models


def sign_state(model, textures, extra=None, shift_picks_model=True):
    """A road sign's Forge blockstate: the eight facings, the extension post below, the three
    shift models; plus any other property's variants. With shift_picks_model False the shift
    variants are left empty, for a block whose model another property picks (the flag's hang):
    two properties that both name a model clash, and the last one wins."""
    facing = {}
    for f, angle in FACINGS:
        if angle:
            facing[f] = {"transform": {"rotation": [{"x": 0}, {"y": angle}, {"z": 0}]}}
        else:
            facing[f] = {}
    variants = {
        "facing": facing,
        "inventory": [{}],
        "downward": {"false": {}, "true": {"submodel": {"extension": {
            "model": SIGN_POLE, "transform": {"translation": [0.0, -1.0, 0.0]}}}}},
        "shift": ({"none": {}, "setback": {"model": model + "_setback"},
                   "backtoback": {"model": model + "_back_to_back"}} if shift_picks_model
                  else {"none": {}, "setback": {}, "backtoback": {}}),
    }
    variants.update(extra or {})
    variants["normal"] = [{}]
    return {"forge_marker": 1, "defaults": {"model": model, "textures": textures},
            "variants": variants}


def gui(scale=0.625, y=0.0, x=0):
    return {"gui": {"rotation": [0, 180, 0], "translation": [x, y, 0],
                    "scale": [scale, scale, scale]}}


# --- the flag -------------------------------------------------------------------------------
# The flag and its plates hang off the side of the post, as NYC's bus stop flags do: the post runs
# up one edge of the sign and the sign's edge is bolted across the post's front, ending at the
# post's middle. The model faces north and the sign frame mirrors x, so the reader's right is the
# model's LOW x: a flag reaching to the reader's right (the post at its left edge, the default)
# is x -2 to 8, and one reaching left is 8 to 18. BlockBusStopFlag.FLAG_X_* match.
#
# Why the middle and not further across: from behind, the post (x 6.5 to 9.5) stands in front
# of the back's inner edge. Ending at x 8 it hides 1.5 units of the ten, the margin beside the
# art; ending a unit further across it hid 2.5 and cut the P off STOP and the end off a
# two-digit route number.
SIDES = (("right", -2.0, 8.0), ("left", 8.0, 18.0))
FLAG_Y0, FLAG_Y1 = 8.3, 21.3    # the flag, on the top of the block and above it: 10 x 13
PLATE_TOPS = (7.9, 5.2, 2.5)    # each route plate's top; it is PLATE_HT tall
PLATE_HT = 2.5
PZ0, PZ1 = 0.0, 0.5             # every plate's two faces: BlockBusStopFlag.FACE_*_Z


def flag_elements(px0, px1):
    """The flag and its three route plates, each printed on both faces, spanning x px0 to px1.
    The flag's edges are the sign metal; a plate's edges are its own white rim, so a plate that
    is not there (its slot given the clear texture) takes its edges with it. Past the block's
    x 0..16 every face already names its uv, so none is sampled from a neighbour sprite."""
    art = win(FLAG_W, FLAG_H)
    h = FLAG_Y1 - FLAG_Y0
    els = [{"from": [px0, FLAG_Y0, PZ0], "to": [px1, FLAG_Y1, PZ1], "faces": {
        "north": face("1", art),
        # seen from behind the plate is the same art, read the same way round
        "south": face("1", art),
        "east": face("0", [0, 0, 0.5, h]), "west": face("0", [0, 0, 0.5, h]),
        "up": face("0", [0, 0, 10, 0.5]), "down": face("0", [0, 0, 10, 0.5])}}]
    plate = win(BULLET_W, BULLET_H)
    for i, top in enumerate(PLATE_TOPS):
        slot = "p%d" % (i + 1)
        els.append({"from": [px0, top - PLATE_HT, PZ0], "to": [px1, top, PZ1], "faces": {
            "north": face(slot, plate), "south": face(slot, plate),
            "east": face(slot, [0, 0, 0.25, 4]), "west": face(slot, [0, 0, 0.25, 4]),
            "up": face(slot, [0, 0, 16, 0.25]), "down": face(slot, [0, 0, 16, 0.25])}})
    return els


def flags():
    textures = {"0": POST_TEX, "1": C.T("flag_cityline"), "p1": C.T("route_cityline"),
                "p2": C.T("none"), "p3": C.T("none"), "particle": C.T("flag_cityline")}
    # the flag reaches from y 0 to 21.3: shrunk and brought down to sit in the slot. The slot
    # shows the right-hanging flag, x -2 to 9.5 with the post, a block turned to face the viewer:
    # moved 4.25 units back toward the middle, at the slot's scale.
    models = {}
    for side, px0, px1 in SIDES:
        name = "sign_flag" if side == "right" else "sign_flag_" + side
        models.update(shift_models(name, textures, flag_elements(px0, px1),
                                   gui(0.58, -1.5, -2.5) if side == "right" else None))
    hang = {}
    for side, _, _ in SIDES:
        base = C.M("sign_flag" if side == "right" else "sign_flag_" + side)
        for shift, suffix in (("none", ""), ("setback", "_setback"),
                              ("backtoback", "_back_to_back")):
            hang["%s_%s" % (shift, side)] = {"model": base + suffix}
    for i, a in enumerate(AGENCIES):
        aid, _, _, _, _, names = a
        reg = "bus_stop_flag_" + aid
        tex = {"0": POST_TEX, "1": C.T("flag_" + aid), "p1": C.T("route_" + aid),
               "p2": C.T("none"), "p3": C.T("none"), "particle": C.T("flag_" + aid)}
        routes = {}
        for k in range(3):
            slot = "p%d" % (k + 1)
            routes["route%d" % (k + 1)] = {"true": {"textures": {slot: C.T("route_" + aid)}},
                                           "false": {"textures": {slot: C.T("none")}}}
        routes["hang"] = hang
        state = sign_state(C.M("sign_flag"), tex, routes, shift_picks_model=False)
        java = 'new BlockBusStopFlag("%s")' % reg
        C.add(reg, java, names, models if i == 0 else {}, state, tab=TAB)


# --- the cases -------------------------------------------------------------------------------
def case_elements(x0, x1, y0, y1, poster_w, poster_h):
    """A poster case on the front of the post: a shallow box whose front is the poster, a frame
    standing proud of it, two bands round the post behind."""
    z0, z1 = -0.6, 0.5
    rim = 0.35
    els = [{"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": {
        "north": face("1", win(poster_w, poster_h)),
        "south": face("case", [x0, 16 - y1, x1, 16 - y0]),
        "east": face("case", [0, 16 - y1, z1 - z0, 16 - y0]),
        "west": face("case", [0, 16 - y1, z1 - z0, 16 - y0]),
        "up": face("case", [x0, 0, x1, z1 - z0]), "down": face("case", [x0, 0, x1, z1 - z0])}}]
    fz0 = z0 - 0.3
    rims = [([x0, y1 - rim, fz0], [x1, y1, z0]), ([x0, y0, fz0], [x1, y0 + rim, z0]),
            ([x0, y0 + rim, fz0], [x0 + rim, y1 - rim, z0]),
            ([x1 - rim, y0 + rim, fz0], [x1, y1 - rim, z0])]
    for frm, to in rims:
        els.append(box(frm, to, "case", faces=("north", "east", "west", "up", "down")))
    for y in (y0 + 1.5, y1 - 2.3):
        els.append(band(y, y + 0.8))
    return els


def cases():
    specs = [
        ("bus_stop_timetable_case", "timetable", (4.0, 12.0, 3.0, 13.0), (TT_W, TT_H),
         ("Bus Stop Timetable Case", "Fahrplanvitrine", "Vitrina de Horarios",
          "Tidtabellsskåp")),
        ("bus_stop_route_map_case", "route_map", (3.5, 12.5, 4.0, 12.0), (MAP_W, MAP_H),
         ("Bus Stop Route Map Case", "Liniennetzvitrine", "Vitrina de Mapa de Rutas",
          "Linjekartsskåp")),
    ]
    for reg, poster, (x0, x1, y0, y1), (pw, ph), names in specs:
        name = "sign_" + reg[len("bus_stop_"):]
        tex = {"0": POST_TEX, "1": C.T(poster), "case": C.T("case"), "clamp": C.T("clamp"),
               "particle": C.T("case")}
        models = shift_models(name, tex, case_elements(x0, x1, y0, y1, pw, ph), gui())
        C.add(reg, 'new BlockTrafficSign("%s")' % reg, names, models,
              sign_state(C.M(name), tex), tab=TAB)


# --- the arrival display ------------------------------------------------------------------
# BlockBusArrivalDisplay.SCREEN_*: the housing's front at DZ0, the screen in its middle
DX0, DX1, DY0, DY1, DZ0, DZ1 = 1.0, 15.0, 5.6, 12.0, -0.9, 0.5


def display_elements():
    els = [{"from": [DX0, DY0, DZ0], "to": [DX1, DY1, DZ1], "faces": {
        "north": face("1", win(DISPLAY_W, DISPLAY_H)),
        "south": face("housing", [DX0, 16 - DY1, DX1, 16 - DY0]),
        "east": face("housing", [0, 16 - DY1, DZ1 - DZ0, 16 - DY0]),
        "west": face("housing", [0, 16 - DY1, DZ1 - DZ0, 16 - DY0]),
        "up": face("housing", [DX0, 0, DX1, DZ1 - DZ0]),
        "down": face("housing", [DX0, 0, DX1, DZ1 - DZ0])}}]
    # a hood over the screen
    els.append(box([DX0 - 0.3, DY1, DZ0 - 0.6], [DX1 + 0.3, DY1 + 0.4, DZ1], "housing"))
    for y in (DY0 + 1.0, DY1 - 1.8):
        els.append(band(y, y + 0.8))
    return els


def display():
    reg = "bus_stop_arrival_display"
    tex = {"0": POST_TEX, "1": C.T("display_front"), "housing": C.T("display_housing"),
           "clamp": C.T("clamp"), "particle": C.T("display_housing")}
    models = shift_models("sign_arrival_display", tex, display_elements(), gui())
    C.add(reg, 'new BlockBusArrivalDisplay("%s")' % reg,
          ("Bus Arrival Display", "Abfahrtsanzeige (Bus)", "Pantalla de Llegadas de Autobús",
           "Avgångsskylt (Buss)"),
          models, sign_state(C.M("sign_arrival_display"), tex), tab=TAB)


# --- the curb plaque ------------------------------------------------------------------------
def curb_plaque():
    reg = "bus_stop_curb_plaque"
    els = [{"from": [4, 0, 4], "to": [12, 0.4, 12], "faces": {
        "up": face("art", [0, 0, 16, 16]),
        "north": face("edge", [4, 15.6, 12, 16]), "south": face("edge", [4, 15.6, 12, 16]),
        "east": face("edge", [4, 15.6, 12, 16]), "west": face("edge", [4, 15.6, 12, 16])}}]
    m = lc.model({"art": C.T("plaque"), "edge": C.T("plaque_edge"), "particle": C.T("plaque")},
                 els)
    m["display"] = {
        # face up to the viewer, turned back the half turn the texture is stored in
        "gui": {"rotation": [90, 180, 0], "translation": [0, 0, 0], "scale": [1.3, 1.3, 1.3]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [90, 180, 0], "translation": [0, 0, 0], "scale": [1.3, 1.3, 1.3]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 0],
                                  "scale": [0.4, 0.4, 0.4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 4, 0],
                                 "scale": [0.4, 0.4, 0.4]},
    }
    state = lc.facing_state(C.M(reg))
    C.add(reg, 'new BlockBusStopPlaque("%s")' % reg,
          ("Bus Stop Curb Plaque", "Haltestellen-Bordsteinplakette",
           "Placa de Parada en el Bordillo", "Hållplatsplakett för Trottoarkant"),
          {reg: m}, state, tab=TAB)


C.add_lang("csm.transit.flag.route", ("Route plate %s: %s", "Linienschild %s: %s",
                                      "Placa de ruta %s: %s", "Linjeskylt %s: %s"))
C.add_lang("csm.transit.flag.side_right", ("Flag hangs to the right of the post",
                                           "Schild hängt rechts vom Mast",
                                           "Señal colgada a la derecha del poste",
                                           "Skylten hänger till höger om stolpen"))
C.add_lang("csm.transit.flag.side_left", ("Flag hangs to the left of the post",
                                          "Schild hängt links vom Mast",
                                          "Señal colgada a la izquierda del poste",
                                          "Skylten hänger till vänster om stolpen"))
C.add_lang("csm.transit.flag.route_none", ("Route plate %s: none", "Linienschild %s: keins",
                                           "Placa de ruta %s: ninguna", "Linjeskylt %s: ingen"))

register_textures()
flags()
cases()
display()
curb_plaque()

if __name__ == "__main__":
    sys.exit(C.main())
