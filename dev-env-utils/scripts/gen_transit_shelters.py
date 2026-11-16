#!/usr/bin/env python3
"""Every asset the Transit tab's bus shelters ship: the glass-and-steel shelter, the cantilever
canopy and the minimal flat roof, each in the four invented agencies' liveries.

    python dev-env-utils/scripts/gen_transit_shelters.py
    python dev-env-utils/scripts/gen_transit_shelters.py --check
    python dev-env-utils/scripts/gen_transit_shelters.py --fragments   # tab lines to paste

A shelter is BlockBusShelter, built at real scale: a column of sections, three blocks tall at
least, its roof's underside about 2.75 m up. From the ground: BOTTOM, SECOND, any number of
MIDDLE sections (each one block more headroom) and the TOP, which carries the roof. The section
is actual state, worked out from the column, and picks the models with `section` in the multipart
blockstate. Placing one puts down a whole shelter, four columns long and two rows deep.

Shelters join both ways. Along their length (`left`, `right`: the same block continues on the
sitter's left or right) the end walls, end posts and the roof's end overhang are drawn only where
the run stops. Front to back (`ahead`, `behind`: the same block continues in front or behind) the
back wall, bench and screen are drawn only in the back row, the fascia only along the front, and
the end walls only in the back row, so a two-row shelter is open at its front corners as real ones
are. `lit` (TOP) shows the roof light's lens lit.

Everything is written in "column coordinates" for a three-section column: facing north, the open
front at z = 0, the back at z = 16, the sitter's left at x = 0, and y from 0 to 48 (BOTTOM 0..16,
SECOND 16..32, TOP 32..48). Each part is cut at the section lines into a model per section, so a
post or a glass pane is one piece here and three models in the game. A MIDDLE section is the
parts marked `run` (posts, the walls' glass), drawn the full height of the section in plain
glass: they are what a taller shelter has more of. Numbers the collision boxes share are in
BusShelterStyle.java.

The glass shelter leaves the sitter's left end of its back row open, in an empty steel frame, for
Signage's shelter ad panel (AdBoardKind.SHELTER_PANEL), which a player sets against it from
outside: a lightbox two blocks tall and 3 px deep in the next block, its back frame lip reaching
1 px into this one. So in BOTTOM and SECOND at that end the frame posts, rails, back wall and
bench stop 1.25 px short of the block edge, and nothing of the shelter stands in the panel's way;
above the panel the end is glazed in the frame. Transit never refers to Signage: they meet only
in the world.

Liveries are texture variants, not geometry: every part is drawn once, in CITYLINE's colours, and
each other agency's model of a part that shows its colours is a child model naming its own
`frame` and `fascia` textures.
"""
import os
import shutil
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

SECTIONS = ("bottom", "second", "middle", "top")
BANDS = {"bottom": (0, 16), "second": (16, 32), "top": (32, 48)}


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
FASCIA_PX = 8   # texels a unit on the fascias: 128 px across a block
FASCIA_H = 6    # the deep fascia's height in units
SLIM_H = 3      # the flat roof's slim fascia


def fascia_art(agency, height_units, scale):
    """A fascia: the agency's name centred on its band, 16 x height_units units at 8 texels a
    unit in the top rows of a 128 px texture, with the agency's own stripe."""
    _, name, _, ground, text, stripe, place = agency
    img = lc.fill(ground, 128, 2, 70 + AGENCY_IDS.index(agency[0]) * 3 + scale)
    h = int(height_units * FASCIA_PX)
    th = 5 * scale
    ty = (h - th) // 2
    if place == "bottom":
        lc.rect(img, 0, h - scale - 2, 128, h, stripe)
        ty = (h - scale - 2 - th) // 2
    elif place == "left":
        for x0 in (0, 128 - 12):
            lc.rect(img, x0, 0, x0 + 12, h, stripe)
    else:  # lines
        lc.rect(img, 0, 2, 128, 4, stripe)
        lc.rect(img, 0, h - 4, 128, h - 2, stripe)
    lc.draw_text_centred(img, name, 64, ty, text, scale)
    return img


def glass(frit=False):
    """Clear glass, faintly blue, with a streak of reflection; frit is the band of dots set
    into real shelter glass at eye level so nobody walks into it."""
    img = lc.fill((214, 232, 242), 16, 2, 81, alpha=62)
    px = img.load()
    for y in range(16):
        x = (y * 5 // 8 + 3) % 16
        r, g, b, a = px[x, y]
        px[x, y] = (min(255, r + 20), min(255, g + 20), min(255, b + 14), 92)
    if frit:
        for y in range(16):
            for x in range(y % 2, 16, 2):
                px[x, y] = (236, 238, 240, 150)
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
        # scale 3, not 4: the name repeats every block, and needs the gaps to read as a pattern
        tex["fascia_" + a[0]] = (lambda a=a: fascia_art(a, FASCIA_H, 3))
        tex["fascia_slim_" + a[0]] = (lambda a=a: fascia_art(a, SLIM_H, 2))
    for name, draw in tex.items():
        C.texture(name)(draw)


# ------------------------------------------------------------------------------------------
# Elements in column coordinates, and cutting them into sections
# ------------------------------------------------------------------------------------------
ALL = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")


def el(frm, to, tex, faces=ALL, per=None, uv=None, rot=None, run=False):
    """An element in column coordinates: `per` gives some faces another texture, `uv` gives
    some faces an explicit uv, `rot` an element rotation (the element must lie in one section),
    `run` puts a full-height copy of it in every MIDDLE section."""
    return {"frm": list(frm), "to": list(to), "tex": tex, "faces": tuple(faces),
            "per": per or {}, "uv": uv or {}, "rot": rot, "run": run}


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


def cut(els, section):
    """The part of every element in one section, in that section's own coordinates."""
    out = []
    if section == "middle":
        for e in els:
            if not e["run"]:
                continue
            tex = "glass" if e["tex"] == "glass_frit" else e["tex"]
            piece = dict(e, tex=tex, frm=[e["frm"][0], 0, e["frm"][2]],
                         to=[e["to"][0], 16, e["to"][2]])
            out.append(build(piece, 0, True, True))
        return out
    lo, hi = BANDS[section]
    for e in els:
        y0, y1 = e["frm"][1], e["to"][1]
        if y1 <= lo or y0 >= hi:
            continue
        if e["rot"] and (y0 < lo or y1 > hi):
            raise SystemExit("a rotated element crosses a section line: %r" % (e,))
        # a run element is cut open where a MIDDLE may be set in: the SECOND/TOP line
        piece = dict(e, frm=[e["frm"][0], max(y0, lo), e["frm"][2]],
                     to=[e["to"][0], min(y1, hi), e["to"][2]])
        out.append(build(piece, lo, y0 < lo, y1 > hi))
    return out


# ------------------------------------------------------------------------------------------
# Parts: (name, conditions, elements). Conditions are one dict or a list of alternative dicts
# over left, right, ahead, behind, lit.
# ------------------------------------------------------------------------------------------
Y_DECK0, Y_DECK1 = 44.0, 46.0     # the glass shelter's roof deck: 2.75 m clear at three tall
SLOT_X = 1.25                     # where the glass shelter stops at its ad panel end
SLOT_TOP = 32.0                   # the ad panel is two blocks tall
FRONT = -6.0                      # the roof's front overhang


def post(x0, x1, z0, z1, top=Y_DECK0, tex="frame"):
    return el((x0, 0, z0), (x1, top, z1), tex, SIDES, run=True)


def pane_x(x0, x1, z0, z1, rail_z0, rail_z1, top=Y_DECK0):
    """A glass wall along x: kick rail, the pane with a frit band at eye level, top rail. Rails
    and panes leave out their end faces, which are always in a post or carry on into the next
    block. The lower pane is the one a MIDDLE section repeats."""
    rf = ("north", "south", "up", "down")
    out = [el((x0, 0, rail_z0), (x1, 1, rail_z1), "frame", rf),
           el((x0, 1, z0), (x1, 21, z1), "glass", ("north", "south"), run=True),
           el((x0, 21, z0), (x1, 25, z1), "glass_frit", ("north", "south"))]
    if top > 25:
        out.append(el((x0, 25, z0), (x1, top - 1, z1), "glass", ("north", "south")))
        out.append(el((x0, top - 1, rail_z0), (x1, top, rail_z1), "frame", rf))
    return out


def pane_z(x0, x1, rail_x0, rail_x1, z0, z1, y0=0.0, top=Y_DECK0, run=True):
    """A glass wall along z, an end wall, built as pane_x is; from y0 up when it starts above
    something (the ad panel)."""
    rf = ("east", "west", "up", "down")
    out = []
    if y0 == 0:
        out += [el((rail_x0, 0, z0), (rail_x1, 1, z1), "frame", rf),
                el((x0, 1, z0), (x1, 21, z1), "glass", ("east", "west"), run=run),
                el((x0, 21, z0), (x1, 25, z1), "glass_frit", ("east", "west")),
                el((x0, 25, z0), (x1, top - 1, z1), "glass", ("east", "west"))]
    else:
        out.append(el((x0, y0, z0), (x1, top - 1, z1), "glass", ("east", "west"), run=run))
    out.append(el((rail_x0, top - 1, z0), (rail_x1, top, z1), "frame", rf))
    return out


FASCIA_UV = [0, 0, 16, FASCIA_H]


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


def roof_edges(P, y0, y1, fy0, fy1, front, fascia_tex, window, back_band):
    """The overhangs every style's roof has: the front with its fascia (front row only), the
    back edge (back row only), the ends, and the four corners."""
    F, T = False, True
    P.append(("front", {"ahead": F}, [deck(0, 16, front, 0, y0, y1),
                                      fascia(0, 16, front - 0.5, front, fy0, fy1, fascia_tex,
                                             window)]))
    P.append(("back", {"behind": F}, [deck(0, 16, 16, 17, y0, y1),
                                      el((0, back_band, 17), (16, fy1, 17.5), "frame")]))
    for side, x0, x1, bx0, bx1 in (("left", -1, 0, -1.5, -1), ("right", 16, 17, 17, 17.5)):
        P.append(("end_roof_" + side, {side: F},
                  [deck(x0, x1, 0, 16, y0, y1), el((bx0, back_band, 0), (bx1, fy1, 16), "frame")]))
        P.append(("corner_front_" + side, {side: F, "ahead": F},
                  [deck(x0, x1, front, 0, y0, y1),
                   fascia(min(x0, bx0), max(x1, bx1), front - 0.5, front, fy0, fy1, "frame",
                          [0, 0, 1.5, window[3]]),
                   el((bx0, back_band, front), (bx1, fy1, 0), "frame")]))
        P.append(("corner_back_" + side, {side: F, "behind": F},
                  [deck(x0, x1, 16, 17, y0, y1),
                   el((min(x0, bx0), back_band, 17), (max(x1, bx1), fy1, 17.5), "frame"),
                   el((bx0, back_band, 16), (bx1, fy1, 17), "frame")]))


def glass_parts():
    F, T = False, True
    P = []
    # back wall, back row only; at the ad panel end it starts at the frame
    P.append(("back_wall", {"behind": F, "left": T}, pane_x(0, 16, 15, 15.4, 14.75, 15.75)))
    P.append(("back_wall_slot", {"behind": F, "left": F},
              pane_x(SLOT_X, 16, 15, 15.4, 14.75, 15.75)))
    # slim mullions at every joint along the back, posts at the ends
    P.append(("post_joint", {"behind": F, "left": T}, [post(-0.5, 0.5, 14.5, 16)]))
    P.append(("post_back_right", {"right": F, "behind": F}, [post(14.5, 16, 14.5, 16)]))
    P.append(("post_front_right", {"right": F, "ahead": F}, [post(14.5, 16, 0.5, 2)]))
    P.append(("post_front_left", {"left": F, "ahead": F, "behind": T},
              [post(0, 1.5, 0.5, 2)]))
    # the ad panel frame, at the sitter's left end of the back row: posts, rails at the
    # panel's foot and head, and glass in the frame above it
    for ahead in (F, T):
        z0 = 0 if ahead else 2
        frame = [post(SLOT_X, SLOT_X + 1.5, 14.5, 16),
                 post(SLOT_X, SLOT_X + 1.5, 0 if ahead else 0.5, 1.5 if ahead else 2)]
        frame += slot_rails(z0)
        frame += pane_z(SLOT_X + 0.55, SLOT_X + 0.95, SLOT_X, SLOT_X + 1.5, z0, 14.5,
                        y0=SLOT_TOP)
        P.append(("slot_frame" + ("_deep" if ahead else ""),
                  {"left": F, "behind": F, "ahead": ahead}, frame))
    # the right end wall, back row only, from the front post (or the row in front)
    for ahead in (F, T):
        z0 = 0 if ahead else 2
        P.append(("end_right" + ("_deep" if ahead else ""), {"right": F, "behind": F,
                                                            "ahead": ahead},
                  pane_z(15.05, 15.45, 14.75, 15.75, z0, 14.5)))
    # the bench, back row only
    P.append(("bench", {"behind": F}, bench(3, 14.5) + bench_support()))
    P.append(("bench_join_left", {"behind": F, "left": T}, bench(0, 3)))
    P.append(("bench_join_right", {"behind": F, "right": T}, bench(14.5, 16)))
    # the roof
    P.append(("deck", {}, [deck(0, 16, 0, 16)]))
    roof_edges(P, Y_DECK0, Y_DECK1, Y_DECK1 - FASCIA_H + 2, Y_DECK1 + 2, FRONT, "fascia",
               FASCIA_UV, Y_DECK0 - 1)
    P += light_parts(4, 12, 5, 11, Y_DECK0)
    return P


def slot_rails(z0):
    """The empty frame's rails at the panel's foot and head, between its posts."""
    rf = ("east", "west", "up", "down")
    return [el((SLOT_X, 0, z0), (SLOT_X + 1.5, 1.5, 14.5), "frame", rf),
            el((SLOT_X + 0.25, 1.5, z0), (SLOT_X + 1.25, 2, 14.5), "steel", rf),
            el((SLOT_X, SLOT_TOP - 1.5, z0), (SLOT_X + 1.5, SLOT_TOP, 14.5), "frame", rf),
            el((SLOT_X + 0.25, SLOT_TOP - 2, z0), (SLOT_X + 1.25, SLOT_TOP - 1.5, 14.5), "steel",
               rf)]


def light_parts(x0, x1, z0, z1, ceiling):
    """The roof light: a lens under the roof, lit or not."""
    def lens_el(tex):
        return [el((x0, ceiling - 0.5, z0), (x1, ceiling, z1), tex,
                   ("north", "south", "east", "west", "down"))]
    return [("light_on", {"lit": True}, lens_el("lens_on")),
            ("light_off", {"lit": False}, lens_el("lens_off"))]


# --- the cantilever ---------------------------------------------------------------------------
C_DECK0, C_DECK1 = 44.0, 45.5


def cantilever_parts():
    F, T = False, True
    P = []
    column = [post(7, 9, 14, 16, C_DECK0),
              el((6, 0, 13.5), (10, 0.5, 16), "steel", ("north", "east", "west", "up", "south")),
              # the brace up from the column to the arm, under the canopy
              el((7.5, 37, 2), (8.5, 38, 14), "frame", ("north", "east", "west", "up", "down"),
                 rot={"origin": [8, 37.5, 14], "axis": "x", "angle": 22.5})]
    P.append(("column", {"behind": F}, column))
    # the arm under the canopy, through every row on the column's line
    for ahead in (F, T):
        for behind in (F, T):
            z0 = 0 if ahead else FRONT + 1
            z1 = 16 if behind else 14
            P.append(("arm_%s_%s" % ("a" if ahead else "f", "b" if behind else "k"),
                      {"ahead": ahead, "behind": behind},
                      [el((7.3, 42, z0), (8.7, C_DECK0, z1), "frame",
                          ("north", "east", "west", "down"))]))
    # the perforated screen behind the bench, with stiles at the run's ends
    rf = ("north", "south", "up", "down")
    screen = lambda x0, x1: [  # noqa: E731
        el((x0, 1.5, 14.6), (x1, 22, 15), "screen", ("north", "south")),
        el((x0, 1, 14.4), (x1, 1.5, 15.2), "frame", rf),
        el((x0, 22, 14.4), (x1, 23, 15.2), "frame", rf)]
    P.append(("screen", {"behind": F}, screen(1.5, 14.5)))
    P.append(("screen_join_left", {"behind": F, "left": T}, screen(0, 1.5)))
    P.append(("screen_join_right", {"behind": F, "right": T}, screen(14.5, 16)))
    P.append(("stile_left", {"behind": F, "left": F}, [post(0.5, 1.5, 14.3, 15.3, 23)]))
    P.append(("stile_right", {"behind": F, "right": F}, [post(14.5, 15.5, 14.3, 15.3, 23)]))
    P.append(("bench", {"behind": F}, bench(1.5, 14.5, 10, 14) + [
        el((7.2, 6, 10.5), (8.8, 7, 14), "steel", ("north", "east", "west", "down"))]))
    P.append(("bench_join_left", {"behind": F, "left": T}, bench(0, 1.5, 10, 14)))
    P.append(("bench_join_right", {"behind": F, "right": T}, bench(14.5, 16, 10, 14)))
    # the canopy, the fascia along its front and the curve down at its back
    P.append(("canopy", {}, [deck(0, 16, 0, 16, C_DECK0, C_DECK1)]))
    P.append(("front", {"ahead": F}, [deck(0, 16, FRONT, 0, C_DECK0, C_DECK1),
                                      fascia(0, 16, FRONT - 0.5, FRONT, C_DECK1 - FASCIA_H + 2,
                                             C_DECK1 + 2)]))
    dy = C_DECK0 - 29.5
    back = [deck(0, 16, 16, 18, C_DECK0, C_DECK1),
            el((0, C_DECK0, 18), (16, C_DECK1, 21), "frame", ALL,
               per={"up": "roof", "down": "under"},
               rot={"origin": [8, C_DECK1, 18], "axis": "x", "angle": 22.5}),
            el((0, 28.35 + dy, 20.77), (16, 29.85 + dy, 23.27), "frame", ALL,
               per={"up": "roof", "down": "under"},
               rot={"origin": [8, 29.85 + dy, 20.77], "axis": "x", "angle": 45})]
    P.append(("back", {"behind": F}, back))
    P += light_parts(5, 11, 2, 6, C_DECK0)
    return P


# --- the flat roof ------------------------------------------------------------------------------
F_DECK0, F_DECK1 = 43.5, 45.0
SLIM_UV = [0, 0, 16, SLIM_H]


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
    P.append(("roof", {}, [deck(0, 16, 0, 16, F_DECK0, F_DECK1)]))
    roof_edges(P, F_DECK0, F_DECK1, F_DECK1 - SLIM_H + 1, F_DECK1 + 1, -3.0, "slim", SLIM_UV,
               F_DECK0 - 0.5)
    P += light_parts(5, 11, 5, 11, F_DECK0)
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
        "gui": {"rotation": [30, 225, 0], "translation": [0, -1.2, 0], "scale": [0.26] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.14] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, -1.2, 0], "scale": [0.28] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0],
                                  "scale": [0.16] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0],
                                  "scale": [0.18] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -1, 0],
                                 "scale": [0.18] * 3},
    }


ITEM_SHIFT = {"bottom": -16, "second": 0, "top": 16}   # the item: one column, in -16..32


def shelters():
    for style, java_const, names in STYLES:
        parts = PARTS[style]()
        seen = set()
        pieces = []   # (model name, section, conditions list, elements)
        for name, conds, els in parts:
            assert name not in seen, name
            seen.add(name)
            alts = conds if isinstance(conds, list) else [conds]
            for section in SECTIONS:
                cut_els = cut(els, section)
                if cut_els:
                    pieces.append(("%s/%s_%s" % (style, name, section), section, alts, cut_els))
        base_models = {}
        for mname, section, alts, els in pieces:
            base_models[mname] = lc.model(textures(keys_of(els), BASE), els)
        # the item: a one-column shelter on its own, three sections, the light off
        item_els = []
        for mname, section, alts, els in pieces:
            if section == "middle":
                continue
            if any(all(not v for v in a.values()) for a in alts):
                dy = ITEM_SHIFT[section]
                for e in els:
                    e2 = dict(e, **{"from": [e["from"][0], e["from"][1] + dy, e["from"][2]],
                                    "to": [e["to"][0], e["to"][1] + dy, e["to"][2]]})
                    if "rotation" in e:
                        r = dict(e["rotation"])
                        r["origin"] = [r["origin"][0], r["origin"][1] + dy, r["origin"][2]]
                        e2["rotation"] = r
                    item_els.append(e2)
        for aid, name, *_ in AGENCIES:
            reg = "bus_shelter_%s_%s" % (style, aid)
            models = dict(base_models) if aid == BASE else {}
            rules = []
            for mname, section, alts, els in pieces:
                keys = keys_of(els)
                ref = mname
                if aid != BASE and keys & set(LIVERY_KEYS):
                    ref = "%s/%s/%s" % (style, aid, mname.split("/", 1)[1])
                    models[ref] = {"parent": "csm:block/transit/shelters/" + mname,
                                   "textures": livery_textures(keys, aid)}
                for f, y in FACINGS:
                    for a in alts:
                        when = {"facing": f, "section": section}
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


MESSAGES = {
    "taller": ("Shelter is now %s blocks tall", "Wartehäuschen ist jetzt %s Blöcke hoch",
               "La marquesina mide ahora %s bloques de alto", "Busskuren är nu %s block hög"),
    "shorter": ("Shelter is now %s blocks tall", "Wartehäuschen ist jetzt %s Blöcke hoch",
                "La marquesina mide ahora %s bloques de alto", "Busskuren är nu %s block hög"),
    "lowest": ("A shelter is at least %s blocks tall",
               "Ein Wartehäuschen ist mindestens %s Blöcke hoch",
               "Una marquesina mide al menos %s bloques de alto",
               "En busskur är minst %s block hög"),
    "tallest": ("A shelter is at most %s blocks tall",
                "Ein Wartehäuschen ist höchstens %s Blöcke hoch",
                "Una marquesina mide como máximo %s bloques de alto",
                "En busskur är högst %s block hög"),
    "blocked": ("No room to raise the roof: %s %s %s is in the way",
                "Kein Platz, um das Dach anzuheben: %s %s %s ist im Weg",
                "No hay sitio para subir el techo: %s %s %s estorba",
                "Ingen plats att höja taket: %s %s %s är i vägen"),
    "no_room": ("No room for a whole shelter at %s %s %s (sneak to place one column)",
                "Kein Platz für ein ganzes Wartehäuschen bei %s %s %s "
                "(schleichen für eine Säule)",
                "No hay sitio para una marquesina entera en %s %s %s "
                "(agáchate para colocar una columna)",
                "Ingen plats för en hel busskur vid %s %s %s (smyg för att sätta en sektion)"),
    "one_column": ("A whole shelter takes %s; placed one column",
                   "Ein ganzes Wartehäuschen braucht %s; eine Säule gesetzt",
                   "Una marquesina entera necesita %s; se colocó una columna",
                   "En hel busskur kräver %s; en sektion placerad"),
}
for key, names in MESSAGES.items():
    C.add_lang("csm.transit.shelter." + key, names)

register_textures()
shelters()

if __name__ == "__main__":
    if "--check" not in sys.argv and "--fragments" not in sys.argv:
        # the models are all rewritten: clear out what an older layout left behind
        shutil.rmtree(os.path.join(ASSETS, "models", "block", "transit", "shelters"),
                      ignore_errors=True)
    sys.exit(C.main())
