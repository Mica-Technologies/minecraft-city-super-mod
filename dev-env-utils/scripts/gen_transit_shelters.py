#!/usr/bin/env python3
"""Every asset the Transit tab's bus shelters ship: the glass-and-steel shelter, the cantilever
canopy and the minimal flat roof, each in the four invented agencies' liveries.

    python dev-env-utils/scripts/gen_transit_shelters.py
    python dev-env-utils/scripts/gen_transit_shelters.py --check
    python dev-env-utils/scripts/gen_transit_shelters.py --fragments   # tab lines to paste

A shelter is BlockBusShelter: two blocks tall, placed as one piece, lower and upper half
(`upper`). Shelters join both ways. Along their length (`left`, `right`: the same block continues
on the sitter's left or right) the end walls, end posts and the roof's end overhang are drawn only
where the run stops, so three in a row are one shelter three blocks long. Front to back
(`ahead`, `behind`: the same block continues in front or behind) they make a deeper shelter: the
back wall and bench only in the back row, the fascia only along the front. All four are actual
state, picked by the multipart blockstate; `lit` (upper half) shows the roof light's lens lit.

Everything is written in "shelter coordinates": facing north, the open front at z = 0, the back
at z = 16, the sitter's left at x = 0, and y from 0 to 32 over both halves. Each part is then cut
at y = 16 into a model per half (`_lo`, `_hi`), so a post or a glass pane runs through both.
Numbers the collision boxes share are in BusShelterStyle.java.

The glass shelter leaves the sitter's left end of its back row open, in an empty steel frame, for
Signage's shelter ad panel (AdBoardKind.SHELTER_PANEL), which a player sets against it from
outside: a lightbox 3 px deep in the next block, its back frame lip reaching 1 px into this one.
So at that end the posts, rails, back wall and roof stop 1.25 px short of the block edge, and
nothing of the shelter stands in the panel's way. Transit never refers to Signage: they meet only
in the world.

Liveries are texture variants, not geometry: every part is drawn once, in CITYLINE's colours, and
each other agency's model of a part that shows its colours is a child model naming its own
`frame` and `fascia` textures.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_shelters.py", "transit/shelters", "transit/shelters",
                 assets=ASSETS)
TAB = "CsmTabTransit"

WHITE = (240, 242, 240)
GRAPHITE = (56, 58, 62)
STEEL = (70, 74, 78)
GALV = (158, 164, 162)

# ------------------------------------------------------------------------------------------
# Agencies: the flags' four (gen_transit_stops.py), and how each paints a shelter
# ------------------------------------------------------------------------------------------
# id, name, frame colour, fascia ground, fascia text, stripe colour, stripe place
AGENCIES = [
    ("cityline", "CITYLINE", (14, 94, 111), (14, 94, 111), (244, 196, 40), (244, 196, 40),
     "bottom"),
    ("riverway", "RIVERWAY", (31, 58, 115), (31, 58, 115), WHITE, (240, 138, 36), "left"),
    ("verdant", "VERDANT", (36, 116, 60), (36, 116, 60), WHITE, (236, 240, 232), "lines"),
    ("emberline", "EMBERLINE", GRAPHITE, (176, 36, 32), WHITE, GRAPHITE, "bottom"),
]
AGENCY_IDS = [a[0] for a in AGENCIES]
BASE = "cityline"   # the agency the base models are drawn in

STYLES = [
    # style, Java constant, names (the agency goes in the brackets after them)
    ("glass", "GLASS", ("Bus Shelter (Glass, %s)", "Buswartehäuschen (Glas, %s)",
                        "Marquesina de Autobús (Vidrio, %s)", "Busskur (Glas, %s)")),
    ("cantilever", "CANTILEVER", ("Bus Shelter (Cantilever, %s)",
                                  "Buswartehäuschen (Kragdach, %s)",
                                  "Marquesina de Autobús (Voladizo, %s)",
                                  "Busskur (Konsoltak, %s)")),
    ("flat", "FLAT", ("Bus Shelter (Flat Roof, %s)", "Buswartehäuschen (Flachdach, %s)",
                      "Marquesina de Autobús (Techo Plano, %s)", "Busskur (Platt Tak, %s)")),
]

LIVERY_KEYS = ("frame", "fascia", "slim")


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
FASCIA_PX = 8   # texels a unit on the fascias: 128 px across a block


def fascia_art(agency, height_units, scale):
    """A fascia: the agency's name centred on its band, 16 x height_units units at 8 texels a
    unit in the top rows of a 128 px texture, with the agency's own stripe."""
    _, name, _, ground, text, stripe, place = agency
    img = lc.fill(ground, 128, 2, 70 + AGENCY_IDS.index(agency[0]) * 3 + scale)
    h = int(height_units * FASCIA_PX)
    th = 5 * scale
    ty = (h - th) // 2
    if place == "bottom":
        lc.rect(img, 0, h - scale - 1, 128, h, stripe)
        ty = (h - scale - 1 - th) // 2
    elif place == "left":
        for x0 in (0, 128 - 10):
            lc.rect(img, x0, 0, x0 + 10, h, stripe)
    else:  # lines
        lc.rect(img, 0, 1, 128, 2, stripe)
        lc.rect(img, 0, h - 2, 128, h - 1, stripe)
    lc.draw_text_centred(img, name, 64, ty, text, scale)
    return img


def glass(frit=False):
    """Clear glass, faintly blue, with a streak of reflection; frit adds the band of dots set
    into real shelter glass at eye level so nobody walks into it (texture rows 6 to 9, which
    the upper pane puts at about 1.5 m)."""
    img = lc.fill((214, 232, 242), 16, 2, 81, alpha=62)
    px = img.load()
    for y in range(16):
        x = (y * 5 // 8 + 3) % 16
        r, g, b, a = px[x, y]
        px[x, y] = (min(255, r + 20), min(255, g + 20), min(255, b + 14), 92)
    if frit:
        for y in (6, 8):
            for x in range(y // 2 % 2, 16, 2):
                px[x, y] = (236, 238, 240, 190)
    return img


def timber():
    """The bench: hardwood slats running along its length, with the gaps between them."""
    img = lc.fill((150, 98, 56), 16, 7, 83)
    for v in (3, 7, 11, 15):
        lc.rect(img, 0, v, 16, v + 1, (70, 44, 26))
    return img


def perforated():
    """The cantilever's back screen: grey perforated steel, its holes cut out."""
    img = lc.fill((128, 134, 138), 16, 3, 84)
    px = img.load()
    for y in range(1, 16, 3):
        for x in range((y // 3) % 2 * 1 + 1, 16, 3):
            px[x, y] = (0, 0, 0, 0)
    return img


def lens(lit):
    img = lc.fill((255, 248, 222) if lit else (196, 198, 194), 16, 2, 85 + lit)
    lc.frame(img, 0, 0, 16, 16, (230, 226, 208) if lit else (160, 162, 160))
    return img


def register_textures():
    tex = {
        "glass": lambda: glass(),
        "glass_frit": lambda: glass(True),
        "bench": timber,
        "steel": lambda: lc.fill(STEEL, 16, 3, 86),
        "roof": lambda: lc.fill(GALV, 16, 5, 87),
        "under": lambda: lc.fill((206, 210, 210), 16, 2, 88),
        "screen": perforated,
        "lens_on": lambda: lens(True),
        "lens_off": lambda: lens(False),
    }
    for i, a in enumerate(AGENCIES):
        tex["frame_" + a[0]] = (lambda a=a, i=i: lc.fill(a[2], 16, 4, 90 + i))
        tex["fascia_" + a[0]] = (lambda a=a: fascia_art(a, 4, 3))
        tex["fascia_slim_" + a[0]] = (lambda a=a: fascia_art(a, 2.5, 2))
    for name, draw in tex.items():
        C.texture(name)(draw)


# ------------------------------------------------------------------------------------------
# Elements in shelter coordinates, and cutting them into halves
# ------------------------------------------------------------------------------------------
ALL = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")


def el(frm, to, tex, faces=ALL, per=None, uv=None, rot=None):
    """An element in shelter coordinates: `per` gives some faces another texture, `uv` gives
    some faces an explicit uv, `rot` an element rotation (the element must lie in one half)."""
    return {"frm": list(frm), "to": list(to), "tex": tex, "faces": tuple(faces),
            "per": per or {}, "uv": uv or {}, "rot": rot}


def _span(a, b):
    """A face's extent along one axis as uv, moved by whole blocks into 0..16 and never
    clamped, so the grain carries across a block edge; a span across an edge starts at 0, and
    one longer than a block is squeezed into the sprite (gen_ad_boards.py's rule)."""
    shift = 16 * (min(a, b) // 16)
    if max(a, b) - shift > 16:
        return 0.0, min(16.0, abs(b - a))
    return a - shift, b - shift


def _uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    vy = _span(16 - y1, 16 - y0)
    if face == "north":
        u = _span(16 - x1, 16 - x0)
    elif face == "south":
        u = _span(x0, x1)
    elif face == "east":
        u = _span(16 - z1, 16 - z0)
    elif face == "west":
        u = _span(z0, z1)
    elif face == "up":
        u, vy = _span(x0, x1), _span(z0, z1)
    else:
        u, vy = _span(x0, x1), _span(16 - z1, 16 - z0)
    uv = [u[0], vy[0], u[1], vy[1]]
    if uv[0] == uv[2]:
        uv[2] = uv[0] + 0.01
    if uv[1] == uv[3]:
        uv[3] = uv[1] + 0.01
    return [round(v, 4) for v in uv]


def build(e, dy, cut_lo, cut_hi):
    """The JSON element for e moved down by dy, with the faces on a cut plane left out."""
    f = [e["frm"][0], e["frm"][1] - dy, e["frm"][2]]
    t = [e["to"][0], e["to"][1] - dy, e["to"][2]]
    faces = {}
    for name in e["faces"]:
        if (name == "down" and cut_lo) or (name == "up" and cut_hi):
            continue
        uv = e["uv"].get(name) or _uv(name, f, t)
        faces[name] = {"texture": "#" + e["per"].get(name, e["tex"]),
                       "uv": [round(v, 4) for v in uv]}
    out = {"from": [round(v, 4) for v in f], "to": [round(v, 4) for v in t], "faces": faces}
    if e["rot"]:
        r = dict(e["rot"])
        r["origin"] = [r["origin"][0], round(r["origin"][1] - dy, 4), r["origin"][2]]
        out["rotation"] = r
    return out


def cut(els, upper):
    """The part of every element in one half, in that half's own coordinates."""
    out = []
    lo, hi = (16, 32) if upper else (0, 16)
    for e in els:
        y0, y1 = e["frm"][1], e["to"][1]
        if y1 <= lo or y0 >= hi:
            continue
        if e["rot"] and (y0 < lo or y1 > hi):
            raise SystemExit("a rotated element crosses the cut: %r" % (e,))
        piece = dict(e, frm=[e["frm"][0], max(y0, lo), e["frm"][2]],
                     to=[e["to"][0], min(y1, hi), e["to"][2]])
        out.append(build(piece, lo, y0 < lo, y1 > hi))
    return out


# ------------------------------------------------------------------------------------------
# Parts: (name, conditions, elements). Conditions are one dict or a list of alternative dicts
# over left, right, ahead, behind, lit.
# ------------------------------------------------------------------------------------------
Y_DECK0, Y_DECK1 = 29.5, 31.5     # the glass shelter's roof deck
SLOT_X = 1.25                     # where the glass shelter stops at its ad panel end


def post(x0, x1, z0, z1, top=Y_DECK0, tex="frame"):
    return el((x0, 0, z0), (x1, top, z1), tex, SIDES)


def pane_x(x0, x1, z0, z1, rail_z0, rail_z1):
    """A glass wall along x: kick rail, lower pane, mid rail, upper pane with its frit band,
    top rail. Rails and panes leave out their end faces, which are always in a post or carry
    on into the next block."""
    rf = ("north", "south", "up", "down")
    return [el((x0, 0, rail_z0), (x1, 1.5, rail_z1), "frame", rf),
            el((x0, 1.5, z0), (x1, 14.5, z1), "glass", ("north", "south")),
            el((x0, 14.5, rail_z0), (x1, 15.5, rail_z1), "frame", rf),
            el((x0, 15.5, z0), (x1, 28, z1), "glass_frit", ("north", "south")),
            el((x0, 28, rail_z0), (x1, Y_DECK0, rail_z1), "frame", rf)]


def pane_z(x0, x1, rail_x0, rail_x1, z0, z1):
    """A glass wall along z, an end wall, built as pane_x is."""
    rf = ("east", "west", "up", "down")
    return [el((rail_x0, 0, z0), (rail_x1, 1.5, z1), "frame", rf),
            el((x0, 1.5, z0), (x1, 14.5, z1), "glass", ("east", "west")),
            el((rail_x0, 14.5, z0), (rail_x1, 15.5, z1), "frame", rf),
            el((x0, 15.5, z0), (x1, 28, z1), "glass_frit", ("east", "west")),
            el((rail_x0, 28, z0), (rail_x1, Y_DECK0, z1), "frame", rf)]


FASCIA_UV = [0, 0, 16, 4]


def fascia(x0, x1, z0, z1, y0, y1, tex="fascia", window=FASCIA_UV):
    """A fascia band whose front (north) face shows the agency name: the texture's window is
    the band's own width, counted from the viewer's left, which is x = 16."""
    uv = [16 - x1 + window[0], window[1], 16 - x0 + window[0], window[3]]
    return el((x0, y0, z0), (x1, y1, z1), "frame", ALL, per={"north": tex}, uv={"north": uv})


def deck(x0, x1, z0, z1, y0=Y_DECK0, y1=Y_DECK1):
    return el((x0, y0, z0), (x1, y1, z1), "frame", ALL, per={"up": "roof", "down": "under"})


def bench(x0, x1, z0=10, z1=14.5):
    return [el((x0, 7, z0), (x1, 8, z1), "bench")]


def bench_support(z0=10.5, z1=14.5, zl0=12, zl1=13.2, x=8):
    return [el((x - 0.8, 6, z0), (x + 0.8, 7, z1), "steel", ("north", "east", "west", "down")),
            el((x - 0.6, 0, zl0), (x + 0.6, 6, zl1), "steel", SIDES)]


def glass_parts():
    F, T = False, True
    P = []
    # back wall, back row only; at the ad panel end it starts at the frame
    P.append(("back_wall", {"behind": F, "left": T}, pane_x(0, 16, 15, 15.4, 14.75, 15.75)))
    P.append(("back_wall_slot", {"behind": F, "left": F},
              pane_x(SLOT_X, 16, 15, 15.4, 14.75, 15.75)))
    # posts: at every joint along the back, and at the ends
    P.append(("post_joint", {"behind": F, "left": T}, [post(-0.75, 0.75, 14.5, 16)]))
    P.append(("post_back_left", {"left": F, "behind": T}, [post(0, 1.5, 14.5, 16)]))
    P.append(("post_back_right", {"right": F}, [post(14.5, 16, 14.5, 16)]))
    P.append(("post_front_left", {"left": F, "ahead": F, "behind": T},
              [post(0, 1.5, 0.5, 2)]))
    P.append(("post_front_right", {"right": F, "ahead": F}, [post(14.5, 16, 0.5, 2)]))
    # the ad panel frame, at the sitter's left end of the back row
    slot = [post(SLOT_X, SLOT_X + 1.5, 14.5, 16)]
    P.append(("slot_frame", {"left": F, "behind": F, "ahead": F},
              slot + [post(SLOT_X, SLOT_X + 1.5, 0.5, 2)] + slot_rails(2)))
    P.append(("slot_frame_deep", {"left": F, "behind": F, "ahead": T},
              slot + [post(SLOT_X, SLOT_X + 1.5, 0, 1.5)] + slot_rails(1.5)))
    # end walls: glass from the front post (or the row in front) to the row's back post; the
    # sitter's left end of the back row is the ad panel frame instead
    for ahead in (F, T):
        z0 = 0 if ahead else 2
        deep = "_deep" if ahead else ""
        P.append(("end_left" + deep, {"left": F, "behind": T, "ahead": ahead},
                  pane_z(0.55, 0.95, 0.25, 1.25, z0, 14.5)))
        P.append(("end_right" + deep, {"right": F, "ahead": ahead},
                  pane_z(15.05, 15.45, 14.75, 15.75, z0, 14.5)))
    # the bench, back row only
    P.append(("bench", {"behind": F}, bench(3, 14.5) + bench_support()))
    P.append(("bench_join_left", {"behind": F, "left": T}, bench(0, 3)))
    P.append(("bench_join_right", {"behind": F, "right": T}, bench(14.5, 16)))
    # the roof
    P.append(("deck", [{"left": T}, {"left": F, "behind": T}], [deck(0, 16, 0, 16)]))
    P.append(("deck_slot", {"left": F, "behind": F}, [deck(SLOT_X, 16, 0, 16)]))
    front = lambda x0: [deck(x0, 16, -4, 0), fascia(x0, 16, -4.5, -4, 28, 32)]  # noqa: E731
    P.append(("front", [{"ahead": F, "left": T}, {"ahead": F, "left": F, "behind": T}],
              front(0)))
    P.append(("front_slot", {"ahead": F, "left": F, "behind": F}, front(SLOT_X)))
    back = lambda x0: [deck(x0, 16, 16, 17),  # noqa: E731
                       el((x0, 29, 17), (16, 32, 17.5), "frame")]
    P.append(("back", {"behind": F, "left": T}, back(0)))
    P.append(("back_slot", {"behind": F, "left": F}, back(SLOT_X)))
    P.append(("end_left_roof", {"left": F, "behind": T},
              [deck(-1, 0, 0, 16), el((-1.5, 29, 0), (-1, 32, 16), "frame")]))
    P.append(("corner_front_left", {"left": F, "behind": T, "ahead": F},
              [deck(-1, 0, -4, 0), fascia(-1.5, 0, -4.5, -4, 28, 32, "frame", [0, 0, 1.5, 4]),
               el((-1.5, 29, -4), (-1, 32, 0), "frame")]))
    P.append(("end_right_roof", {"right": F},
              [deck(16, 17, 0, 16), el((17, 29, 0), (17.5, 32, 16), "frame")]))
    P.append(("corner_front_right", {"right": F, "ahead": F},
              [deck(16, 17, -4, 0), fascia(16, 17.5, -4.5, -4, 28, 32, "frame", [0, 0, 1.5, 4]),
               el((17, 29, -4), (17.5, 32, 0), "frame")]))
    P.append(("corner_back_right", {"right": F, "behind": F},
              [deck(16, 17, 16, 17), el((16, 29, 17), (17.5, 32, 17.5), "frame"),
               el((17, 29, 16), (17.5, 32, 17), "frame")]))
    P += light_parts(4, 12, 6, 10, Y_DECK0)
    return P


def slot_rails(z0):
    """The empty frame's rails along the bottom and under the roof, between its posts."""
    rf = ("east", "west", "up", "down")
    return [el((SLOT_X, 0, z0), (SLOT_X + 1.5, 1.5, 14.5), "frame", rf),
            el((SLOT_X + 0.25, 1.5, z0), (SLOT_X + 1.25, 2, 14.5), "steel", rf),
            el((SLOT_X, 28, z0), (SLOT_X + 1.5, Y_DECK0, 14.5), "frame", rf),
            el((SLOT_X + 0.25, 27.5, z0), (SLOT_X + 1.25, 28, 14.5), "steel", rf)]


def light_parts(x0, x1, z0, z1, ceiling):
    """The roof light: a lens under the roof, lit or not."""
    def lens_el(tex):
        return [el((x0, ceiling - 0.5, z0), (x1, ceiling, z1), tex,
                   ("north", "south", "east", "west", "down"))]
    return [("light_on", {"lit": True}, lens_el("lens_on")),
            ("light_off", {"lit": False}, lens_el("lens_off"))]


# --- the cantilever ---------------------------------------------------------------------------
C_DECK0, C_DECK1 = 29.5, 31.0


def cantilever_parts():
    F, T = False, True
    P = []
    column = [post(7, 9, 14, 16, C_DECK0),
              el((6, 0, 13.5), (10, 0.5, 16), "steel", ("north", "east", "west", "up", "south")),
              # the arm under the canopy, and the brace up to it from the column
              el((7.3, 27.5, -3), (8.7, C_DECK0, 14), "frame", ("north", "east", "west", "down")),
              el((7.5, 22, 2), (8.5, 23, 14), "frame", ("north", "east", "west", "up", "down"),
                 rot={"origin": [8, 22.5, 14], "axis": "x", "angle": 22.5})]
    P.append(("column", {"behind": F}, column))
    # the perforated screen behind the bench, with stiles at the run's ends
    rf = ("north", "south", "up", "down")
    screen = lambda x0, x1: [  # noqa: E731
        el((x0, 1.5, 14.6), (x1, 20, 15), "screen", ("north", "south")),
        el((x0, 1, 14.4), (x1, 1.5, 15.2), "frame", rf),
        el((x0, 20, 14.4), (x1, 21, 15.2), "frame", rf)]
    P.append(("screen", {"behind": F}, screen(1.5, 14.5)))
    P.append(("screen_join_left", {"behind": F, "left": T}, screen(0, 1.5)))
    P.append(("screen_join_right", {"behind": F, "right": T}, screen(14.5, 16)))
    P.append(("stile_left", {"behind": F, "left": F}, [post(0.5, 1.5, 14.3, 15.3, 21)]))
    P.append(("stile_right", {"behind": F, "right": F}, [post(14.5, 15.5, 14.3, 15.3, 21)]))
    P.append(("bench", {"behind": F}, bench(1.5, 14.5, 10, 14) + [
        el((7.2, 6, 10.5), (8.8, 7, 14), "steel", ("north", "east", "west", "down"))]))
    P.append(("bench_join_left", {"behind": F, "left": T}, bench(0, 1.5, 10, 14)))
    P.append(("bench_join_right", {"behind": F, "right": T}, bench(14.5, 16, 10, 14)))
    # the canopy, the fascia along its front and the curve down at its back
    P.append(("canopy", {}, [deck(0, 16, 0, 16, C_DECK0, C_DECK1)]))
    P.append(("front", {"ahead": F}, [deck(0, 16, -5, 0, C_DECK0, C_DECK1),
                                      fascia(0, 16, -5.5, -5, 28, 32)]))
    back = [deck(0, 16, 16, 18, C_DECK0, C_DECK1),
            el((0, C_DECK0, 18), (16, C_DECK1, 21), "frame", ALL,
               per={"up": "roof", "down": "under"},
               rot={"origin": [8, C_DECK1, 18], "axis": "x", "angle": 22.5}),
            el((0, 28.35, 20.77), (16, 29.85, 23.27), "frame", ALL,
               per={"up": "roof", "down": "under"},
               rot={"origin": [8, 29.85, 20.77], "axis": "x", "angle": 45})]
    P.append(("back", {"behind": F}, back))
    P += light_parts(5, 11, 2, 6, C_DECK0)
    return P


# --- the flat roof ------------------------------------------------------------------------------
F_DECK0, F_DECK1 = 29.0, 30.5
SLIM_UV = [0, 0, 16, 2.5]


def flat_parts():
    F, T = False, True
    P = []
    top = F_DECK0
    P.append(("post_front_left", {"left": F, "ahead": F}, [post(0.5, 1.7, 1, 2.2, top)]))
    P.append(("post_front_right", {"right": F, "ahead": F}, [post(14.3, 15.5, 1, 2.2, top)]))
    P.append(("post_back_left", {"left": F, "behind": F}, [post(0.5, 1.7, 13.8, 15, top)]))
    P.append(("post_back_right", {"right": F, "behind": F}, [post(14.3, 15.5, 13.8, 15, top)]))
    P.append(("post_joint", {"left": T, "behind": F}, [post(-0.6, 0.6, 13.8, 15, top)]))
    # the lean rail: a timber perch on a steel bar
    rail = lambda x0, x1: [  # noqa: E731
        el((x0, 12, 13.3), (x1, 12.8, 14.9), "bench"),
        el((x0, 11, 13.8), (x1, 12, 14.6), "steel", ("north", "south", "down"))]
    P.append(("rail", {"behind": F}, rail(1.7, 14.3)))
    P.append(("rail_join_left", {"behind": F, "left": T}, rail(0, 1.7)))
    P.append(("rail_join_right", {"behind": F, "right": T}, rail(14.3, 16)))
    band = lambda f, t: el(f, t, "frame")  # noqa: E731
    P.append(("roof", {}, [deck(0, 16, 0, 16, F_DECK0, F_DECK1)]))
    P.append(("front", {"ahead": F}, [deck(0, 16, -2, 0, F_DECK0, F_DECK1),
                                      fascia(0, 16, -2.5, -2, 28.5, 31, "slim", SLIM_UV)]))
    P.append(("back", {"behind": F}, [deck(0, 16, 16, 17, F_DECK0, F_DECK1),
                                      band((0, 28.5, 17), (16, 31, 17.5))]))
    for side, x0, x1, bx0, bx1 in (("left", -1, 0, -1.5, -1), ("right", 16, 17, 17, 17.5)):
        P.append(("end_" + side, {side: F}, [deck(x0, x1, 0, 16, F_DECK0, F_DECK1),
                                             band((bx0, 28.5, 0), (bx1, 31, 16))]))
        P.append(("corner_front_" + side, {side: F, "ahead": F},
                  [deck(x0, x1, -2, 0, F_DECK0, F_DECK1),
                   fascia(min(x0, bx0), max(x1, bx1), -2.5, -2, 28.5, 31, "frame", SLIM_UV),
                   band((bx0, 28.5, -2), (bx1, 31, 0))]))
        P.append(("corner_back_" + side, {side: F, "behind": F},
                  [deck(x0, x1, 16, 17, F_DECK0, F_DECK1),
                   band((min(x0, bx0), 28.5, 17), (max(x1, bx1), 31, 17.5)),
                   band((bx0, 28.5, 16), (bx1, 31, 17))]))
    P += light_parts(5, 11, 6, 10, F_DECK0)
    return P


PARTS = {"glass": glass_parts, "cantilever": cantilever_parts, "flat": flat_parts}


# ------------------------------------------------------------------------------------------
# Models, blockstates, items
# ------------------------------------------------------------------------------------------
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def keys_of(els):
    keys = set()
    for e in els:
        for f in e["faces"].values():
            keys.add(f["texture"][1:])
    return keys


def livery(k, agency):
    return C.T({"frame": "frame_", "fascia": "fascia_", "slim": "fascia_slim_"}[k] + agency)


def textures(keys, agency):
    """A model's textures: the livery keys in the agency's colours, the rest shared."""
    t = {k: livery(k, agency) if k in LIVERY_KEYS else C.T(k) for k in sorted(keys)}
    t["particle"] = livery("frame", agency)
    return t


def livery_textures(keys, agency):
    """A child model's textures: only what the agency changes."""
    t = {k: livery(k, agency) for k in sorted(keys) if k in LIVERY_KEYS}
    t["particle"] = livery("frame", agency)
    return t


def item_display():
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -2.6, 0], "scale": [0.34] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.18] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, -2.6, 0], "scale": [0.36] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0],
                                  "scale": [0.2] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0],
                                  "scale": [0.22] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -1, 0],
                                 "scale": [0.22] * 3},
    }


def shelters():
    for style, java_const, names in STYLES:
        parts = PARTS[style]()
        seen = set()
        halves = []   # (model name, half, conditions list, elements)
        for name, conds, els in parts:
            assert name not in seen, name
            seen.add(name)
            alts = conds if isinstance(conds, list) else [conds]
            for upper in (False, True):
                cut_els = cut(els, upper)
                if cut_els:
                    halves.append(("%s/%s_%s" % (style, name, "hi" if upper else "lo"), upper,
                                   alts, cut_els))
        base_models = {}
        for mname, upper, alts, els in halves:
            base_models[mname] = lc.model(textures(keys_of(els), BASE), els)
        # the item: a one-block shelter on its own, both halves, the light off
        item_els = []
        for mname, upper, alts, els in halves:
            if any(all(not v for k, v in a.items()) for a in alts):
                for e in els:
                    e2 = dict(e, **{"from": [e["from"][0], e["from"][1] + (16 if upper else 0),
                                             e["from"][2]],
                                    "to": [e["to"][0], e["to"][1] + (16 if upper else 0),
                                           e["to"][2]]})
                    if "rotation" in e:
                        r = dict(e["rotation"])
                        r["origin"] = [r["origin"][0], r["origin"][1] + (16 if upper else 0),
                                       r["origin"][2]]
                        e2["rotation"] = r
                    item_els.append(e2)
        for aid, name, *_ in AGENCIES:
            reg = "bus_shelter_%s_%s" % (style, aid)
            models = dict(base_models) if aid == BASE else {}
            rules = []
            for mname, upper, alts, els in halves:
                keys = keys_of(els)
                ref = mname
                if aid != BASE and keys & set(LIVERY_KEYS):
                    ref = "%s/%s/%s" % (style, aid, mname.split("/", 1)[1])
                    models[ref] = {"parent": "csm:block/transit/shelters/" + mname,
                                   "textures": livery_textures(keys, aid)}
                for f, y in FACINGS:
                    for a in alts:
                        when = {"facing": f, "upper": "true" if upper else "false"}
                        when.update({k: "true" if v else "false" for k, v in a.items()})
                        apply = {"model": C.M(ref)}
                        if y:
                            apply["y"] = y
                        rules.append({"when": when, "apply": apply})
            item = lc.model(textures(keys_of(item_els), aid), item_els)
            item["display"] = item_display()
            java = 'new BlockBusShelter("%s", BusShelterStyle.%s)' % (reg, java_const)
            C.add(reg, java, tuple(n % name for n in names), models, {"multipart": rules},
                  item=item, tab=TAB)


register_textures()
shelters()

if __name__ == "__main__":
    sys.exit(C.main())
