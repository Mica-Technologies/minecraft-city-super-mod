#!/usr/bin/env python3
"""
gen_venues_exterior.py -- the Sports & Venues exterior, and the outdoor pieces every sport shares.

    py -3 dev-env-utils/scripts/gen_venues_exterior.py               # write
    py -3 dev-env-utils/scripts/gen_venues_exterior.py --check       # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_exterior.py --fragments   # the tabs' lines

The arena's outside and entry (the Sports & Venues - Concourse tab, CsmTabVenuesConcourse, after
the concourse):

  * **The exterior marquee board** (BlockMarqueeBoard, BlockMarqueeController): an LED video
    board built to size from one-block cabinets, single-sided on a wall or double-sided on a
    pylon (BlockMarqueePylon). Each cell draws its square of the dark screen and, where the
    board stops (actual state), its share of the frame; what the board shows (three lines of
    event text, the scoreboard's idle video, or both in turn) is drawn once across it by the
    controller's renderer, TileEntityMarqueeRenderer, which shares MARQUEE_* with this file.
  * **Wayfinding blades** (BlockVenueBlade): gate and section blades on Core's wayfinding panel,
    and the steel bracket (BlockBladeBracket) a projecting blade hangs from.
  * **The box office window** (BlockBoxOfficeWindow): a ticket window two blocks tall with a
    speaking grille, a counter ledge with a deal tray and a lit header (BOX OFFICE, WILL CALL,
    TICKETS or blank), joining into a run.

Shared outdoor pieces (the Seating & Venue tab, CsmTabVenuesSeating, after the bowl):

  * **Aluminium bleachers** (BlockBleacher): a deck cell with its seat plank or the aisle's half
    step in the cell above, rows a block up and one or two deep like the bowl, end and back guard
    rails by actual state; the understructure (BlockBleacherFrame) fills the cells under them.
  * **The press box** (BlockPressBox): window and wall modules with outward-leaning tinted glass,
    joining into a wall and turning outside corners.
  * **The ticket booth** (BlockTicketBooth): a freestanding 2 x 2 x 3 booth, one OBJ.
  * **Stadium light towers**: the monopole and lattice shafts (BlockLightPole), stacked to any
    height, and a head of LED floodlight banks (BlockLightHead) in two sizes, OBJ, lit or dark.

Every model faces north (the reader, the customer, the field) with its back at +Z. No brand: the
marquee's words are typed, the headers say BOX OFFICE, WILL CALL and TICKETS.
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402

GEN = "gen_venues_exterior.py"
C = vc.catalogue(GEN, "exterior")
T = C.T
M = C.M
box = vc.box
lsc = vc.lsc
wbox = lsc.pp.wbox

CONC = "Sports & Venues - Concourse"
SEAT = vc.SEATING
FACINGS = vc.FACINGS

# The marquee's screen depth in its cell, shared with TileEntityMarqueeRenderer.
MARQUEE_SINGLE_FACE_Z = 9
MARQUEE_DOUBLE_FRONT_Z = 4
MARQUEE_DOUBLE_BACK_Z = 12

# ------------------------------------------------------------------------------------------
# Palette
# ------------------------------------------------------------------------------------------
ANODISED = (44, 46, 50)
SCREEN = (16, 17, 20)
BRONZE = (78, 64, 50)
CONCRETE = (168, 166, 160)
ALUMINIUM = (196, 200, 204)
STEEL = (198, 202, 206)
STEEL_DARK = (90, 94, 100)
BLACK = (28, 28, 30)
GRANITE = (98, 96, 96)
NAVY = (24, 38, 82)
LEGEND = (255, 236, 186)
GALV = (172, 178, 182)
SIDING = (222, 220, 212)
FASCIA = (40, 52, 84)
DESK = (176, 160, 136)
LENS_ON = (255, 252, 236)
LENS_OFF = (150, 156, 162)
BOOTH = (36, 62, 112)
ROOF = (60, 62, 66)


def rgba(c, a=255):
    return tuple(c[:3]) + (a,)


def only(el, *faces):
    """Keeps just the named faces of an element."""
    el["faces"] = {k: v for k, v in el["faces"].items() if k in faces}
    return el


def uv(el, face, tex, rect):
    """Gives one face its own texture and uv window."""
    el["faces"][face] = {"texture": "#" + tex, "uv": [round(v, 3) for v in rect]}
    return el


def rot(el, axis, angle, origin):
    el["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}
    return el


def ys(y):
    return {"y": y} if y else {}


def tall(frm, to, tex, faces=("north", "south", "east", "west", "up", "down")):
    """wbox() for an element taller than a block: cut at the block lines, so no face's uv
    window is longer than the 16 px a texture has. Only the top piece keeps an up face and
    only the bottom piece a down face."""
    out = []
    y = frm[1]
    while y < to[1] - 1e-6:
        top = min(to[1], (math.floor(y / 16.0) + 1) * 16.0)
        f = [k for k in faces if (k != "up" or top >= to[1]) and (k != "down" or y <= frm[1])]
        out.append(wbox([frm[0], y, frm[2]], [to[0], top, to[2]], tex, faces=tuple(f)))
        y = top
    return out


def unit_cells(boxes_px):
    """The arena generator's rule: every cell a box reaches 6 px into, but the master."""
    def axis(lo, hi, tol=6.0):
        out = []
        for c in range(int(math.floor(lo / 16.0)), int(math.ceil(hi / 16.0))):
            ov = min(hi, 16.0 * (c + 1)) - max(lo, 16.0 * c)
            if ov >= min(tol, hi - lo - 1e-6):
                out.append(c)
        return out
    cells = set()
    for b in boxes_px:
        for cx in axis(b[0], b[3]):
            for cy in axis(b[1], b[4]):
                for cz in axis(b[2], b[5]):
                    cells.add((cx, cy, cz))
    cells.discard((0, 0, 0))
    return sorted(cells, key=lambda c: (c[1], c[2], c[0]))


def unit_java(cls, reg, boxes_px):
    """The tab line of a unit: its footprint written from its boxes."""
    flat_c = ", ".join("%d, %d, %d" % c for c in unit_cells(boxes_px))
    flat_b = ", ".join(", ".join("%d" % int(round(v)) for v in b) for b in boxes_px)
    return 'new %s("%s", new int[]{%s}, new int[]{%s})' % (cls, reg, flat_c, flat_b)


def icon_display(scale, dy=0.0):
    """An item model's display: the block model shrunk to fit the slot."""
    return {"gui": {"rotation": [30, 225, 0], "translation": [0, dy, 0],
                    "scale": [scale, scale, scale]},
            "ground": {"translation": [0, 3, 0], "scale": [scale * 0.5] * 3},
            "fixed": {"translation": [0, dy, 0], "scale": [scale, scale, scale]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                      "scale": [scale * 0.6] * 3},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [scale * 0.7] * 3}}


def item_of(model_name, scale, dy=0.0):
    return {"parent": "csm:block/venues/exterior/" + model_name,
            "display": icon_display(scale, dy)}


# ------------------------------------------------------------------------------------------
# Shared textures
# ------------------------------------------------------------------------------------------
C.textures["anodised"] = lambda: vc.brushed(ANODISED, 601)
C.textures["steel"] = lambda: vc.brushed(STEEL, 602)
C.textures["steel_dark"] = lambda: vc.brushed(STEEL_DARK, 603)
C.textures["black"] = lambda: vc.swatch(BLACK, 604, grain=2)
C.textures["concrete"] = lambda: vc.swatch(CONCRETE, 605, grain=6)
C.textures["galv"] = lambda: vc.brushed(GALV, 606)
C.textures["bronze"] = lambda: vc.brushed(BRONZE, 607)
C.textures["aluminium"] = lambda: vc.brushed(ALUMINIUM, 608)


@C.texture("marquee_screen")
def _marquee_screen():
    """The board's screen, dark: black with the LED pitch faintly in it, two pixels a dot."""
    img = Image.new("RGBA", (16, 16), rgba(SCREEN))
    for y in range(0, 16, 2):
        for x in range(0, 16, 2):
            img.putpixel((x, y), rgba((30, 32, 36)))
    return img


@C.texture("marquee_back")
def _marquee_back():
    img = vc.swatch((70, 72, 76), 611, grain=2)
    vc.rect(img, 0, 0, 16, 1, (52, 54, 58))
    for y in (4, 6, 8, 10):
        vc.rect(img, 3, y, 13, y + 1, (40, 42, 46))
    return img


@C.texture("pylon_cladding")
def _pylon_cladding():
    img = vc.brushed(BRONZE, 612)
    vc.rect(img, 0, 15, 16, 16, vc.shade(BRONZE, 0.72))
    vc.rect(img, 0, 0, 16, 1, vc.shade(BRONZE, 1.12))
    return img


@C.texture("granite")
def _granite():
    rng = random.Random(613)
    img = vc.swatch(GRANITE, 613, grain=2)
    px = img.load()
    for _ in range(18):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = rgba(vc.shade(GRANITE, rng.choice((0.8, 1.15, 1.25))))
    vc.rect(img, 0, 15, 16, 16, vc.shade(GRANITE, 0.7))
    return img


@C.texture("glass")
def _glass():
    img = Image.new("RGBA", (16, 16), (170, 200, 214, 70))
    for i in range(4, 12):
        img.putpixel((i, 15 - i), (235, 245, 255, 110))
    return img


@C.texture("glass_tint")
def _glass_tint():
    img = Image.new("RGBA", (16, 16), (40, 60, 74, 150))
    for i in range(3, 13):
        img.putpixel((i, 15 - i), (150, 180, 200, 170))
    return img


@C.texture("grille")
def _grille():
    """A round speaking grille: a stainless ring with holes, clear outside the circle."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d <= 7.6:
                if d > 6.3:
                    px[x, y] = rgba(vc.shade(STEEL, 0.9))
                elif (x + y) % 2 == 0:
                    px[x, y] = rgba((40, 42, 46))
                else:
                    px[x, y] = rgba(STEEL)
    return img


# ------------------------------------------------------------------------------------------
# The exterior marquee board
# ------------------------------------------------------------------------------------------
MQ_TEX = {"screen": T("marquee_screen"), "frame": T("anodised"), "back": T("marquee_back"),
          "particle": T("anodised")}


def marquee_parts(double):
    """The cell's body and its four frame edges, as the reader sees them: the left edge is at
    x 16 (east, facing north), the right at x 0."""
    if double:
        z0, z1, l0, l1 = MARQUEE_DOUBLE_FRONT_Z, MARQUEE_DOUBLE_BACK_Z, 3, 13
        body = only(box([0, 0, z0], [16, 16, z1], "screen"), "north", "south")
        edge_faces = ("north", "south")
    else:
        z0, z1, l0, l1 = MARQUEE_SINGLE_FACE_Z, 16, 8, 16
        body = box([0, 0, z0], [16, 16, z1], "screen", faces=("north", "south"))
        body["faces"]["south"]["texture"] = "#back"
        edge_faces = ("north",)
    return {
        "body": [body],
        "left": [box([15, 0, l0], [16, 16, l1], "frame", faces=edge_faces + ("east",))],
        "right": [box([0, 0, l0], [1, 16, l1], "frame", faces=edge_faces + ("west",))],
        "up": [box([0, 15, l0], [16, 16, l1], "frame", faces=edge_faces + ("up",))],
        "down": [box([0, 0, l0], [16, 1, l1], "frame", faces=edge_faces + ("down",))],
    }


def marquee_models():
    out = {}
    for double in (False, True):
        tag = "double" if double else "single"
        for part, els in marquee_parts(double).items():
            out["marquee_%s_%s" % (tag, part)] = vc.model(MQ_TEX, els)
    icon = [box([1, 3, 9], [15, 13, 15], "screen", faces=("north", "south", "up", "down",
                                                         "east", "west")),
            box([0, 2, 8], [16, 3, 16], "frame"), box([0, 13, 8], [16, 14, 16], "frame"),
            box([0, 3, 8], [1, 13, 16], "frame"), box([15, 3, 8], [16, 13, 16], "frame")]
    icon[0]["faces"]["south"]["texture"] = "#back"
    out["marquee_item"] = vc.model(MQ_TEX, icon)
    return out


def marquee_state():
    rules = []
    for facing, y in FACINGS:
        for double in ("false", "true"):
            tag = "double" if double == "true" else "single"
            w = {"facing": facing, "double": double}
            rules.append({"when": dict(w), "apply": dict(model=M("marquee_%s_body" % tag),
                                                          **ys(y))})
            for side in ("left", "right", "up", "down"):
                rules.append({"when": dict(w, **{side: "true"}),
                              "apply": dict(model=M("marquee_%s_%s" % (tag, side)), **ys(y))})
    return {"multipart": rules}


_mq = marquee_models()
C.add("marquee_board", 'new BlockMarqueeBoard("marquee_board")',
      ("Marquee Board", "Außen-LED-Wand", "Pantalla Exterior", "Utomhus-LED-skylt"),
      _mq, marquee_state(), item={"parent": M("marquee_item").replace("csm:", "csm:block/")},
      tab=CONC)
C.add("marquee_board_controller", 'new BlockMarqueeController("marquee_board_controller")',
      ("Marquee Board Controller", "Außen-LED-Wand (Steuerung)",
       "Pantalla Exterior (Controlador)", "Utomhus-LED-skylt (Styrenhet)"),
      {}, marquee_state(), item={"parent": M("marquee_item").replace("csm:", "csm:block/")},
      tab=CONC)


# ------------------------------------------------------------------------------------------
# The pylon
# ------------------------------------------------------------------------------------------
PY_TEX = {"clad": T("pylon_cladding"), "concrete": T("concrete"), "cap": T("aluminium"),
          "particle": T("pylon_cladding")}


def pylon_models():
    body = [box([0, 0, 3], [16, 16, 13], "clad", faces=("north", "south", "east", "west"))]
    cap = [box([0, 15, 2.5], [16, 16, 13.5], "cap")]
    plinth = [box([0, 0, 1.5], [16, 3, 14.5], "concrete")]
    return {"pylon": vc.model(PY_TEX, body), "pylon_cap": vc.model(PY_TEX, cap),
            "pylon_plinth": vc.model(PY_TEX, plinth),
            "pylon_item": vc.model(PY_TEX, body + cap + plinth)}


def pylon_state():
    rules = []
    for facing, y in FACINGS:
        rules.append({"when": {"facing": facing}, "apply": dict(model=M("pylon"), **ys(y))})
        rules.append({"when": {"facing": facing, "up": "true"},
                      "apply": dict(model=M("pylon_cap"), **ys(y))})
        rules.append({"when": {"facing": facing, "down": "true"},
                      "apply": dict(model=M("pylon_plinth"), **ys(y))})
    return {"multipart": rules}


C.add("marquee_pylon", 'new BlockMarqueePylon("marquee_pylon")',
      ("Marquee Pylon", "Pylon für LED-Wand", "Pilono de Pantalla", "Skyltpylon"),
      pylon_models(), pylon_state(), item={"parent": "csm:block/venues/exterior/pylon_item"},
      tab=CONC)


# ------------------------------------------------------------------------------------------
# Wayfinding blades (Core's wayfinding panel) and the blade bracket
# ------------------------------------------------------------------------------------------
@C.texture("blade_icon")
def _blade_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    vc.rect(img, 4, 0, 12, 16, (18, 32, 74))
    vc.rect(img, 4, 0, 12, 1, (10, 18, 44))
    vc.draw_text_centred(img, "G", 8, 3, (242, 244, 248))
    vc.draw_text_centred(img, "A", 8, 9, (242, 244, 248))
    return img


@C.texture("blade_section_icon")
def _blade_section_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    vc.rect(img, 0, 4, 16, 12, (18, 32, 74))
    vc.draw_text_centred(img, "101", 8, 6, (242, 244, 248))
    return img


def blade_state(item_model):
    return {"forge_marker": 1, "defaults": {"model": "csm:wayfinding/wayfinding_panel_base"},
            "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                         "inventory": [{"model": M(item_model)}]}}


C.add("venue_gate_blade", 'new BlockVenueBlade("venue_gate_blade", true)',
      ("Gate Blade Sign", "Tor-Fahnenschild", "Letrero de Bandera de Puerta",
       "Flaggskylt för Port"),
      {"gate_blade_item": vc.model({"face": T("blade_icon"), "back": T("steel_dark"),
                                    "particle": T("blade_icon")},
                                   [box([4, 0, 7], [12, 16, 9], "back"),
                                    uv(only(box([4, 0, 6.9], [12, 16, 7], "face"), "north"),
                                       "north", "face", [4, 0, 12, 16])])},
      blade_state("gate_blade_item"), tab=CONC)
C.add("venue_section_blade", 'new BlockVenueBlade("venue_section_blade", false)',
      ("Section Blade Sign", "Sektor-Fahnenschild", "Letrero de Bandera de Sección",
       "Flaggskylt för Sektion"),
      {"section_blade_item": vc.model({"face": T("blade_section_icon"), "back": T("steel_dark"),
                                       "particle": T("blade_section_icon")},
                                      [box([0, 4, 7], [16, 12, 9], "back"),
                                       uv(only(box([0, 4, 6.9], [16, 12, 7], "face"), "north"),
                                          "north", "face", [0, 4, 16, 12])])},
      blade_state("section_blade_item"), tab=CONC)

BR_TEX = {"steel": T("black"), "particle": T("black")}


def bracket_models():
    arm = [box([6.5, 0, 0], [9.5, 3, 16], "steel", faces=("east", "west", "up", "down"))]
    tip = [box([6, 0, 0], [10, 3.5, 0.75], "steel")]
    plate = [box([5, 0, 15.25], [11, 14, 16], "steel"),
             rot(box([7.25, 7.25, 2.1], [8.75, 8.75, 15.9], "steel",
                     faces=("east", "west", "up", "down")), "x", -45, [8, 8, 9])]
    return {"bracket_arm": vc.model(BR_TEX, arm), "bracket_tip": vc.model(BR_TEX, tip),
            "bracket_plate": vc.model(BR_TEX, plate),
            "bracket_item": vc.model(BR_TEX, arm + tip + plate)}


def bracket_state():
    rules = []
    for facing, y in FACINGS:
        rules.append({"when": {"facing": facing}, "apply": dict(model=M("bracket_arm"), **ys(y))})
        rules.append({"when": {"facing": facing, "tip": "true"},
                      "apply": dict(model=M("bracket_tip"), **ys(y))})
        rules.append({"when": {"facing": facing, "plate": "true"},
                      "apply": dict(model=M("bracket_plate"), **ys(y))})
    return {"multipart": rules}


C.add("venue_blade_bracket", 'new BlockBladeBracket("venue_blade_bracket")',
      ("Blade Sign Bracket", "Ausleger für Fahnenschild", "Ménsula para Letrero de Bandera",
       "Konsol för Flaggskylt"),
      bracket_models(), bracket_state(), item={"parent": "csm:block/venues/exterior/bracket_item"},
      tab=CONC)


# ------------------------------------------------------------------------------------------
# The box office window (2 tall: the master below, a part above)
# ------------------------------------------------------------------------------------------
LEGENDS = [("BOX", "OFFICE"), ("WILL", "CALL"), ("TICKETS",), ()]
HEADER_UV = [0, 0, 16, 7]   # the header's 16 x 7 px face on its 64 px texture: rows 0..28


def header(lines, lit_only=False):
    """A header band's legend: lit letters on a navy band, two lines or one."""
    def draw():
        img = Image.new("RGBA", (64, 64), (0, 0, 0, 0) if lit_only else rgba(NAVY))
        if not lit_only:
            vc.rect(img, 0, 0, 64, 2, vc.shade(NAVY, 0.7))
            vc.rect(img, 0, 26, 64, 28, vc.shade(NAVY, 0.7))
            vc.rect(img, 0, 28, 64, 64, vc.shade(NAVY, 0.9))
        if len(lines) == 2:
            vc.draw_text_centred(img, lines[0], 32, 3, LEGEND, scale=2)
            vc.draw_text_centred(img, lines[1], 32, 15, LEGEND, scale=2)
        elif len(lines) == 1:
            vc.draw_text_centred(img, lines[0], 32, 9, LEGEND, scale=2)
        return img
    return draw


for _i, _l in enumerate(LEGENDS):
    C.textures["header_%d" % _i] = header(_l)
    if _l:
        C.textures["header_%d_e" % _i] = header(_l, lit_only=True)

BO_TEX = {"panel": T("granite"), "steel": T("steel"), "glass": T("glass"),
          "bronze": T("bronze"), "black": T("black"), "grille": T("grille"),
          "head": T("header_0"), "navy": T("header_3"), "particle": T("granite")}


def window_parts():
    els = [box([0, 0, 6], [16, 13, 16], "panel", faces=("north", "south")),
           box([0, 13, 1.5], [16, 14.5, 16], "steel"),
           box([5, 14.5, 3], [11, 14.6, 8], "black", faces=("up",)),
           only(wbox([0, 14.5, 8], [16, 24, 8.5], "glass"), "north", "south"),
           box([0, 14.5, 7], [16, 15, 9.5], "bronze", faces=("north", "south", "up")),
           wbox([0, 24, 6.5], [16, 25, 10], "bronze", faces=("north", "south", "down")),
           wbox([0, 25, 6], [16, 32, 10], "navy", faces=("south", "up", "down")),
           wbox([1, 15, 7.2], [1.75, 24, 9.3], "bronze", faces=("north", "south", "east",
                                                                "west")),
           ]
    head = only(wbox([0, 25, 6], [16, 32, 10], "head"), "north")
    els.append(uv(head, "north", "head", HEADER_UV))
    grille = {"from": [6, 17.5, 7.95], "to": [10, 21.5, 7.95], "faces": {
        "north": {"texture": "#grille", "uv": [0, 0, 16, 16]}}}
    grille_in = {"from": [6, 17.5, 8.55], "to": [10, 21.5, 8.55], "faces": {
        "south": {"texture": "#grille", "uv": [0, 0, 16, 16]}}}
    els += [grille, grille_in]
    return els


def window_end():
    return [box([0, 0, 5], [1, 16, 10.5], "bronze", faces=("north", "south", "west")),
            wbox([0, 16, 5], [1, 32, 10.5], "bronze", faces=("north", "south", "west", "up"))]


def window_models():
    out = {}
    for i, _l in enumerate(LEGENDS):
        tex = dict(BO_TEX, head=T("header_%d" % i))
        out["window_%d" % i] = vc.model(tex, window_parts())
    out["window_end_l"] = vc.model(BO_TEX, window_end())
    out["window_end_r"] = vc.model(BO_TEX, [vc.mirror_x(e) for e in window_end()])
    out["window_item"] = {"parent": M("window_0").replace("csm:", "csm:block/"),
                          "display": icon_display(0.38, -3)}
    return out


def window_state():
    rules = []
    for facing, y in FACINGS:
        for i in range(4):
            rules.append({"when": {"facing": facing, "legend": str(i)},
                          "apply": dict(model=M("window_%d" % i), **ys(y))})
        rules.append({"when": {"facing": facing, "end_l": "true"},
                      "apply": dict(model=M("window_end_l"), **ys(y))})
        rules.append({"when": {"facing": facing, "end_r": "true"},
                      "apply": dict(model=M("window_end_r"), **ys(y))})
    return {"multipart": rules}


C.add("box_office_window",
      unit_java("BlockBoxOfficeWindow", "box_office_window",
                [(0, 0, 6, 16, 32, 16), (0, 13, 1, 16, 15, 6)]),
      ("Box Office Window", "Kassenschalter", "Ventanilla de Taquilla", "Biljettlucka"),
      window_models(), window_state(),
      item={"parent": "csm:block/venues/exterior/window_item"}, tab=CONC)


# ------------------------------------------------------------------------------------------
# Aluminium bleachers
# ------------------------------------------------------------------------------------------
@C.texture("alu_plank")
def _alu_plank():
    img = vc.brushed(ALUMINIUM, 621)
    for y in (3, 7, 11, 15):
        vc.rect(img, 0, y, 16, y + 1, vc.shade(ALUMINIUM, 0.78))
    return img


@C.texture("alu_riser")
def _alu_riser():
    img = vc.brushed(vc.shade(ALUMINIUM, 0.93), 622)
    vc.rect(img, 0, 0, 16, 1, vc.shade(ALUMINIUM, 1.08))
    vc.rect(img, 0, 15, 16, 16, vc.shade(ALUMINIUM, 0.7))
    return img


BL_TEX = {"plank": T("alu_plank"), "riser": T("alu_riser"), "rail": T("galv"),
          "frame": T("steel_dark"), "particle": T("alu_plank")}


def deck():
    els = []
    for z0, z1 in ((0.5, 5), (5.75, 10.25), (11, 15.5)):
        els.append(box([0, 14.5, z0], [16, 16, z1], "plank"))
    els += [box([0, 12.5, 1], [16, 14.5, 2.5], "frame", faces=("north", "south", "down")),
            box([0, 12.5, 13], [16, 14.5, 14.5], "frame", faces=("north", "south", "down")),
            box([0, 0, 0], [16, 14.5, 0.75], "riser", faces=("north", "south"))]
    return els


def seat_plank():
    return [wbox([0, 22.5, 9], [16, 24, 12], "plank"),
            wbox([0, 22.5, 12.5], [16, 24, 15.5], "plank"),
            wbox([0, 21, 10], [16, 22.5, 11.5], "frame", faces=("north", "south", "down"))]


def seat_bracket(x0):
    return [wbox([x0, 16, 12], [x0 + 1.5, 21, 14], "frame"),
            wbox([x0, 21, 9.5], [x0 + 1.5, 22.5, 15], "frame")]


def aisle_step():
    return [wbox([0, 22.5, 8], [16, 24, 12], "plank"),
            wbox([0, 22.5, 12], [16, 24, 16], "plank"),
            wbox([0, 16, 8], [16, 22.5, 8.75], "riser", faces=("north", "south"))]


def end_l():
    return [box([0, 0, 0], [0.75, 14.5, 16], "riser", faces=("west", "east")),
            wbox([0, 16, 0.5], [1.5, 31, 2], "rail", faces=("north", "south", "east", "west")),
            wbox([0, 16, 13.5], [1.5, 31, 15], "rail", faces=("north", "south", "east",
                                                              "west")),
            wbox([0, 29.5, 0], [1.5, 31, 16], "rail"),
            wbox([0, 23, 0], [1.5, 24.5, 16], "rail")]


def back():
    return [box([0, 0, 15.25], [16, 14.5, 16], "riser", faces=("south", "north")),
            wbox([0, 30, 14.5], [16, 31.5, 16], "rail"),
            wbox([0, 23, 14.5], [16, 24.5, 16], "rail"),
            wbox([0, 16, 14.5], [1.5, 30, 16], "rail", faces=("east", "west", "north",
                                                            "south"))]


def back_post_r():
    return [wbox([14.5, 16, 14.5], [16, 30, 16], "rail", faces=("east", "west", "north",
                                                               "south"))]


def bleacher_models():
    mx = vc.mirror_x
    out = {"bleacher_deck": vc.model(BL_TEX, deck()),
           "bleacher_seat": vc.model(BL_TEX, seat_plank() + seat_bracket(0.5)),
           "bleacher_seat_r": vc.model(BL_TEX, seat_bracket(14)),
           "bleacher_aisle": vc.model(BL_TEX, aisle_step() + seat_bracket(0.5)
                                      + seat_bracket(14)),
           "bleacher_end_l": vc.model(BL_TEX, end_l()),
           "bleacher_end_r": vc.model(BL_TEX, [mx(e) for e in end_l()]),
           "bleacher_back": vc.model(BL_TEX, back()),
           "bleacher_back_r": vc.model(BL_TEX, back_post_r())}
    out["bleacher_icon"] = vc.model(BL_TEX, deck() + seat_plank() + seat_bracket(0.5)
                                    + seat_bracket(14) + end_l())
    out["bleacher_aisle_icon"] = vc.model(BL_TEX, deck() + aisle_step() + seat_bracket(0.5)
                                          + seat_bracket(14))
    out["bleacher_item"] = {"parent": M("bleacher_icon").replace("csm:", "csm:block/"),
                            "display": icon_display(0.42, -3)}
    out["bleacher_aisle_item"] = {"parent": M("bleacher_aisle_icon").replace("csm:", "csm:block/"),
                                  "display": icon_display(0.42, -3)}
    return out


def bleacher_state(aisle):
    rules = []
    for facing, y in FACINGS:
        w = {"facing": facing}
        rules.append({"when": dict(w), "apply": dict(model=M("bleacher_deck"), **ys(y))})
        if aisle:
            rules.append({"when": dict(w), "apply": dict(model=M("bleacher_aisle"), **ys(y))})
        else:
            rules.append({"when": dict(w, seat="true"),
                          "apply": dict(model=M("bleacher_seat"), **ys(y))})
            rules.append({"when": dict(w, seat="true", end_r="true"),
                          "apply": dict(model=M("bleacher_seat_r"), **ys(y))})
        rules.append({"when": dict(w, end_l="true"),
                      "apply": dict(model=M("bleacher_end_l"), **ys(y))})
        rules.append({"when": dict(w, end_r="true"),
                      "apply": dict(model=M("bleacher_end_r"), **ys(y))})
        rules.append({"when": dict(w, back="true"),
                      "apply": dict(model=M("bleacher_back"), **ys(y))})
        rules.append({"when": dict(w, back="true", end_r="true"),
                      "apply": dict(model=M("bleacher_back_r"), **ys(y))})
    return {"multipart": rules}


_bm = bleacher_models()
C.add("aluminium_bleacher", 'new BlockBleacher("aluminium_bleacher", false)',
      ("Aluminium Bleacher", "Aluminium-Tribüne", "Grada de Aluminio", "Aluminiumläktare"),
      {k: v for k, v in _bm.items() if "aisle" not in k}, bleacher_state(False),
      item={"parent": "csm:block/venues/exterior/bleacher_item"}, tab=SEAT)
C.add("aluminium_bleacher_aisle", 'new BlockBleacher("aluminium_bleacher_aisle", true)',
      ("Aluminium Bleacher (Aisle)", "Aluminium-Tribüne (Gang)", "Grada de Aluminio (Pasillo)",
       "Aluminiumläktare (Gång)"),
      {k: v for k, v in _bm.items() if "aisle" in k}, bleacher_state(True),
      item={"parent": "csm:block/venues/exterior/bleacher_aisle_item"}, tab=SEAT)

FR_TEX = {"frame": T("steel_dark"), "particle": T("steel_dark")}


def frame_models():
    ledger = [box([0, 14, 0.75], [16, 16, 2.5], "frame", faces=("north", "south", "down",
                                                                 "up"))]
    post_l = [box([0, 0, 0.75], [1.75, 14, 2.5], "frame", faces=("north", "south", "east",
                                                                  "west")),
              rot(box([0.25, 7.25, -0.9], [1.5, 8.75, 18.9], "frame",
                      faces=("east", "west", "up", "down")), "x", -45, [0.875, 8, 9])]
    post_r = [box([14.25, 0, 0.75], [16, 14, 2.5], "frame", faces=("north", "south", "east",
                                                                    "west"))]
    return {"frame_ledger": vc.model(FR_TEX, ledger), "frame_post_l": vc.model(FR_TEX, post_l),
            "frame_post_r": vc.model(FR_TEX, post_r),
            "frame_item": vc.model(FR_TEX, ledger + post_l + post_r)}


def frame_state():
    rules = []
    for facing, y in FACINGS:
        rules.append({"when": {"facing": facing},
                      "apply": dict(model=M("frame_ledger"), **ys(y))})
        rules.append({"when": {"facing": facing, "post_l": "true"},
                      "apply": dict(model=M("frame_post_l"), **ys(y))})
        rules.append({"when": {"facing": facing, "post_r": "true"},
                      "apply": dict(model=M("frame_post_r"), **ys(y))})
    return {"multipart": rules}


C.add("aluminium_bleacher_frame", 'new BlockBleacherFrame("aluminium_bleacher_frame")',
      ("Aluminium Bleacher Frame", "Aluminium-Tribünengestell", "Estructura de Grada de Aluminio",
       "Läktarstomme i Aluminium"),
      frame_models(), frame_state(), item={"parent": "csm:block/venues/exterior/frame_item"},
      tab=SEAT)


# ------------------------------------------------------------------------------------------
# The press box
# ------------------------------------------------------------------------------------------
@C.texture("pb_siding")
def _pb_siding():
    img = vc.swatch(SIDING, 631, grain=2)
    for x in (0, 4, 8, 12):
        vc.rect(img, x, 0, x + 1, 16, vc.shade(SIDING, 0.78))
        vc.rect(img, x + 1, 0, x + 2, 16, vc.shade(SIDING, 1.05))
    return img


@C.texture("pb_fascia")
def _pb_fascia():
    img = vc.swatch(FASCIA, 632, grain=2)
    vc.rect(img, 0, 0, 16, 1, vc.shade(FASCIA, 1.3))
    vc.rect(img, 0, 15, 16, 16, vc.shade(FASCIA, 0.7))
    return img


C.textures["pb_desk"] = lambda: vc.swatch(DESK, 633, grain=3)

PB_TEX = {"siding": T("pb_siding"), "fascia": T("pb_fascia"), "glass": T("glass_tint"),
          "desk": T("pb_desk"), "bronze": T("bronze"), "particle": T("pb_siding")}
PB_TILT = -22.5
PB_HINGE = [8, 11, 1.5]


def press_glass():
    return [rot(e, "x", PB_TILT, PB_HINGE)
            for e in tall([0, 11, 1.1], [16, 28.5, 1.9], "glass", faces=("north", "south"))]


def press_mullion():
    return [rot(e, "x", PB_TILT, PB_HINGE)
            for e in tall([0, 11, 0.6], [1, 28.5, 2.4], "bronze",
                          faces=("north", "south", "east", "west"))]


def press_window():
    return ([box([0, 0, 0], [16, 10, 3], "siding", faces=("north", "south")),
             box([0, 10, -1], [16, 11, 3.5], "bronze", faces=("north", "up", "down", "south")),
             box([0, 10, 3.5], [16, 11, 9], "desk", faces=("up", "down", "south"))]
            + press_glass() + press_mullion())


def press_fascia():
    return [wbox([0, 27.5, -6.5], [16, 32, 3], "fascia", faces=("north", "up", "down",
                                                               "south"))]


def press_wall():
    return tall([0, 0, 0], [16, 27.5, 3], "siding", faces=("north", "south"))


def press_end():
    return (tall([0, 10, -6.5], [1, 27.5, 3], "siding", faces=("west", "east", "north"))
            + [box([0, 0, 0], [1, 10, 3], "siding", faces=("west",))])


def press_corner():
    return (tall([0, 0, 0], [3, 27.5, 16], "siding", faces=("west", "east", "south"))
            + [wbox([-6.5, 27.5, -6.5], [3, 32, 16], "fascia",
                    faces=("west", "north", "up", "down", "south"))])


def press_models():
    mx = vc.mirror_x
    out = {"press_window": vc.model(PB_TEX, press_window() + press_fascia()),
           "press_wall": vc.model(PB_TEX, press_wall() + press_fascia()),
           "press_end_l": vc.model(PB_TEX, press_end()),
           "press_end_r": vc.model(PB_TEX, [mx(e) for e in press_end()]),
           "press_mullion_r": vc.model(PB_TEX, [mx(e) for e in press_mullion()]),
           "press_corner": vc.model(PB_TEX, press_corner())}
    out["press_item"] = {"parent": M("press_window").replace("csm:", "csm:block/"),
                         "display": icon_display(0.45, -3)}
    return out


def press_state():
    rules = []
    for facing, y in FACINGS:
        w = {"facing": facing}
        rules.append({"when": dict(w, wall="false"),
                      "apply": dict(model=M("press_window"), **ys(y))})
        rules.append({"when": dict(w, wall="true"),
                      "apply": dict(model=M("press_wall"), **ys(y))})
        rules.append({"when": dict(w, wall="false", end_l="true"),
                      "apply": dict(model=M("press_end_l"), **ys(y))})
        rules.append({"when": dict(w, wall="false", end_r="true"),
                      "apply": dict(model=M("press_end_r"), **ys(y))})
        rules.append({"when": dict(w, wall="false", end_r="true"),
                      "apply": dict(model=M("press_mullion_r"), **ys(y))})
        rules.append({"when": dict(w, corner="true"),
                      "apply": dict(model=M("press_corner"), **ys(y))})
    return {"multipart": rules}


C.add("press_box_module",
      unit_java("BlockPressBox", "press_box_module", [(0, 0, 0, 16, 32, 3)]),
      ("Press Box Module", "Pressekabinen-Modul", "Módulo de Cabina de Prensa",
       "Pressläktarmodul"),
      press_models(), press_state(), item={"parent": "csm:block/venues/exterior/press_item"},
      tab=SEAT)


# ------------------------------------------------------------------------------------------
# OBJ helpers
# ------------------------------------------------------------------------------------------
def obox(x0, x1, y0, y1, z0, z1, mat, faces="nsewud", over=None):
    """vc.obox, with some faces given another material and uv rectangle:
    over = {"n": (mat, (u0, v0, u1, v1))}."""
    polys = vc.obox(x0, x1, y0, y1, z0, z1, mat, faces)
    if over:
        order = [f for f in "nsewud" if f in faces]
        for p, f in zip(polys, order):
            if f in over:
                m, (u0, v0, u1, v1) = over[f]
                p.mat = m
                # every face's corners start at its bottom-left as seen from outside
                p.uvs = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
    return polys


def turned(polys, pivot, angle_x):
    """Polygons turned about an x axis through ``pivot`` by ``angle_x`` degrees (positive tips
    a north face down)."""
    a = math.radians(angle_x)
    ca, sa = math.cos(a), math.sin(a)

    def r(p):
        y, z = p[1] - pivot[1], p[2] - pivot[2]
        return (p[0], pivot[1] + y * ca + z * sa, pivot[2] - y * sa + z * ca)
    out = []
    for p in polys:
        n = p.normal
        nn = (n[0], n[1] * ca + n[2] * sa, -n[1] * sa + n[2] * ca)
        q = vc.Poly([r(v) for v in p.pts], p.mat, nn, list(p.uvs))
        out.append(q)
    return out


def write_obj(name, mtl, polys):
    o = vc.Obj(GEN, mtl, name)
    for p in polys:
        o.add(p)
    C.extra["models/block/venues/exterior/%s.obj" % name] = o.text()


# ------------------------------------------------------------------------------------------
# The ticket booth (2 x 2 x 3, OBJ)
# ------------------------------------------------------------------------------------------
@C.texture("booth_panel")
def _booth_panel():
    img = vc.swatch(BOOTH, 641, grain=2)
    vc.rect(img, 0, 0, 16, 1, vc.shade(BOOTH, 1.25))
    vc.rect(img, 0, 15, 16, 16, vc.shade(BOOTH, 0.7))
    return img


@C.texture("booth_back")
def _booth_back():
    """The back wall: panel with a door drawn on its left half."""
    img = Image.new("RGBA", (64, 64), rgba(BOOTH))
    base = vc.swatch(BOOTH, 642, grain=2).resize((64, 64))
    img.paste(base, (0, 0))
    vc.rect(img, 6, 6, 28, 64, vc.shade(BOOTH, 0.8))
    vc.frame(img, 6, 6, 28, 64, vc.shade(BOOTH, 0.55))
    vc.rect(img, 9, 10, 25, 26, (60, 80, 96))
    vc.rect(img, 23, 36, 26, 38, STEEL)
    return img


SIGN_ROWS = 10   # the band's face on its 64 px texture: rows 0..10 (32 x 4.5 px at 7:1)


@C.texture("booth_sign")
def _booth_sign():
    img = Image.new("RGBA", (64, 64), rgba((236, 236, 230)))
    vc.rect(img, 0, 0, 64, 1, (180, 180, 176))
    vc.rect(img, 0, SIGN_ROWS - 1, 64, 64, (180, 180, 176))
    vc.draw_text_centred(img, "TICKETS", 32, 2, (176, 26, 32), scale=1)
    return img


@C.texture("booth_sign_e")
def _booth_sign_e():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    vc.rect(img, 1, 1, 63, SIGN_ROWS - 1, (236, 236, 230))
    vc.draw_text_centred(img, "TICKETS", 32, 2, (176, 26, 32), scale=1)
    return img


C.textures["roof"] = lambda: vc.swatch(ROOF, 643, grain=3)

BOOTH_MTL = {"panel": T("booth_panel"), "back": T("booth_back"), "glass": T("glass"),
             "steel": T("steel"), "concrete": T("concrete"), "sign": T("booth_sign"),
             "roof": T("roof"), "black": T("black"), "grille": T("grille"),
             "desk": T("pb_desk")}


def booth_polys():
    P = []
    P += obox(-0.5, 32.5, 0, 1, -0.5, 32.5, "concrete")
    # lower walls
    P += obox(1.5, 30.5, 1, 14, 0, 1.5, "panel", faces="nsu")
    P += obox(0, 1.5, 1, 14, 1.5, 30.5, "panel", faces="weu")
    P += obox(30.5, 32, 1, 14, 1.5, 30.5, "panel", faces="weu")
    P += obox(0, 32, 1, 36, 30.5, 32, "back", faces="ns")
    # corner posts
    for x0, z0 in ((0, 0), (30.5, 0), (0, 30.5), (30.5, 30.5)):
        P += obox(x0, x0 + 1.5, 1, 36, z0, z0 + 1.5, "steel", faces="nsew")
    # counter ledge outside, deal tray, desk inside
    P += obox(1, 31, 14, 15, -3.5, 3, "steel")
    P += obox(13, 19, 15, 15.1, -2, 1, "black", faces="u")
    P += obox(1.5, 30.5, 13, 14, 1.5, 10, "desk", faces="ud")
    # glass: front and both sides, two-sided
    P += obox(1.5, 30.5, 15, 34, 0.6, 0.9, "glass", faces="ns")
    P += obox(0.6, 0.9, 15, 34, 1.5, 30.5, "glass", faces="ew")
    P += obox(31.1, 31.4, 15, 34, 1.5, 30.5, "glass", faces="ew")
    # mullions
    P += obox(15.25, 16.75, 14, 34, 0.2, 1.3, "steel", faces="nsew")
    # speaking grille on the front pane
    P += [vc.Poly([(18, 20, 0.5), (14, 20, 0.5), (14, 24, 0.5), (18, 24, 0.5)], "grille",
                  (0, 0, -1), [(0, 1), (1, 1), (1, 0), (0, 0)])]
    # sign band and roof
    band = (0, 0, 1, SIGN_ROWS / 64.0)
    P += obox(0, 32, 34, 38.5, 0, 32, "sign", faces="nsew",
              over={f: ("sign", band) for f in "nsew"})
    P += obox(-2.5, 34.5, 38.5, 41.5, -2.5, 34.5, "roof")
    return P


write_obj("ticket_booth", "booth", booth_polys())
C.extra["models/block/venues/exterior/booth.mtl"] = vc.mtl_text(GEN, BOOTH_MTL)

BOOTH_BOXES = [(0, 0, 0, 32, 38, 32), (-2, 38, -2, 34, 42, 34)]


def booth_icon():
    tex = {"panel": T("booth_panel"), "glass": T("glass"), "roof": T("roof"),
           "sign": T("booth_sign"), "steel": T("steel"), "particle": T("booth_panel")}
    sign = box([2, 11, 2], [14, 13, 14], "sign")
    for f in ("north", "south", "east", "west"):
        uv(sign, f, "sign", [0, 0, 16, 16.0 * SIGN_ROWS / 64])
    return vc.model(tex, [box([2, 0, 2], [14, 6, 14], "panel"),
                          box([2.5, 6, 2.5], [13.5, 11, 13.5], "glass"),
                          box([1.5, 6, 1.5], [2.5, 11, 2.5], "steel"),
                          box([13.5, 6, 1.5], [14.5, 11, 2.5], "steel"),
                          sign, box([1, 13, 1], [15, 14.5, 15], "roof")])


C.add("ticket_booth", unit_java("BlockTicketBooth", "ticket_booth", BOOTH_BOXES),
      ("Ticket Booth", "Kassenhäuschen", "Caseta de Boletos", "Biljettkiosk"),
      {"booth_item": booth_icon()},
      {"forge_marker": 1, "defaults": {"model": M("ticket_booth") + ".obj",
                                       "custom": {"ambient": False}},
       "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                    "inventory": [{"model": M("booth_item"), "custom": {}}]}},
      tab=SEAT)


# ------------------------------------------------------------------------------------------
# Stadium light towers: the shafts
# ------------------------------------------------------------------------------------------
@C.texture("lattice")
def _lattice():
    """A lattice face's lacing: an X of angle and a strut at the top, clear between."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    col = rgba(GALV)
    dark = rgba(vc.shade(GALV, 0.75))
    for i in range(16):
        img.putpixel((i, i), col)
        img.putpixel((15 - i, i), col)
        if i < 15:
            img.putpixel((i + 1, i), dark)
            img.putpixel((14 - i, i), dark)
    vc.rect(img, 0, 0, 16, 1, GALV)
    return img


PO_TEX = {"galv": T("galv"), "concrete": T("concrete"), "steel": T("steel_dark"),
          "lattice": T("lattice"), "particle": T("galv")}


def monopole_parts(base):
    y0 = 7 if base else 0
    els = lsc.post(8, 8, 4, y0, 16, "galv", top=False, bottom=False)
    for yb, xb in ((4, 6.5), (12, 9.5)):
        if yb >= y0:
            els.append(box([xb - 0.5, yb, 11.5], [xb + 0.5, yb + 1, 13.5], "steel"))
    if base:
        els += [box([2, 0, 2], [14, 6, 14], "concrete"),
                box([3.5, 6, 3.5], [12.5, 7, 12.5], "steel")]
        for x, z in ((4.5, 4.5), (10.5, 4.5), (4.5, 10.5), (10.5, 10.5)):
            els.append(box([x, 7, z], [x + 1, 7.75, z + 1], "steel"))
    return els


def lattice_parts(base):
    y0 = 5 if base else 0
    els = []
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        els.append(box([x, y0, z], [x + 2, 16, z + 2], "galv",
                       faces=("north", "south", "east", "west")))
    for f, frm, to in (("north", [3, y0, 2], [13, 16, 2]), ("south", [3, y0, 14], [13, 16, 14]),
                       ("west", [2, y0, 3], [2, 16, 13]), ("east", [14, y0, 3], [14, 16, 13])):
        faces = ("north", "south") if f in ("north", "south") else ("east", "west")
        els.append({"from": frm, "to": to,
                    "faces": {k: {"texture": "#lattice", "uv": [0, 0, 16, 16 - y0]}
                              for k in faces}})
    if base:
        els += [box([0, 0, 0], [16, 4, 16], "concrete"),
                box([0.5, 4, 0.5], [3.5, 5, 3.5], "steel"),
                box([12.5, 4, 0.5], [15.5, 5, 3.5], "steel"),
                box([0.5, 4, 12.5], [3.5, 5, 15.5], "steel"),
                box([12.5, 4, 12.5], [15.5, 5, 15.5], "steel")]
    return els


def pole_state(prefix):
    return {"forge_marker": 1, "defaults": {"model": M(prefix)},
            "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                         "base": {"true": {"model": M(prefix + "_base")}, "false": {}},
                         "inventory": [{"model": M(prefix + "_base")}]}}


C.add("stadium_light_pole", 'new BlockLightPole("stadium_light_pole", false)',
      ("Stadium Light Pole (Monopole)", "Flutlichtmast (Rundmast)",
       "Poste de Iluminación de Estadio (Monoposte)", "Belysningsmast (Rundmast)"),
      {"monopole": vc.model(PO_TEX, monopole_parts(False)),
       "monopole_base": vc.model(PO_TEX, monopole_parts(True))},
      pole_state("monopole"), tab=SEAT)
C.add("stadium_light_lattice", 'new BlockLightPole("stadium_light_lattice", true)',
      ("Stadium Light Tower (Lattice)", "Flutlichtmast (Gittermast)",
       "Torre de Iluminación de Estadio (Celosía)", "Belysningsmast (Fackverk)"),
      {"lattice": vc.model(PO_TEX, lattice_parts(False)),
       "lattice_base": vc.model(PO_TEX, lattice_parts(True))},
      pole_state("lattice"), tab=SEAT)


# ------------------------------------------------------------------------------------------
# Stadium light towers: the heads (OBJ, lit and dark)
# ------------------------------------------------------------------------------------------
@C.texture("flood_lens_on")
def _lens_on():
    img = Image.new("RGBA", (16, 16), rgba(LENS_ON))
    for y in range(1, 16, 3):
        for x in range(1, 16, 3):
            vc.rect(img, x, y, x + 2, y + 2, (255, 255, 250))
    vc.frame(img, 0, 0, 16, 16, (210, 214, 218))
    return img


@C.texture("flood_lens_on_e")
def _lens_on_e():
    img = Image.new("RGBA", (16, 16), rgba(LENS_ON))
    vc.frame(img, 0, 0, 16, 16, (0, 0, 0, 0))
    return img


@C.texture("flood_lens_off")
def _lens_off():
    img = Image.new("RGBA", (16, 16), rgba(LENS_OFF))
    for y in range(1, 16, 3):
        for x in range(1, 16, 3):
            vc.rect(img, x, y, x + 2, y + 2, vc.shade(LENS_OFF, 0.82))
    vc.frame(img, 0, 0, 16, 16, (120, 124, 128))
    return img


@C.texture("flood_housing")
def _housing():
    img = vc.swatch((58, 60, 64), 651, grain=2)
    for y in range(1, 16, 3):
        vc.rect(img, 0, y, 16, y + 1, (40, 42, 46))
    return img


HEADS = {
    # name: (columns, rows, half width in px, frame top)
    "large": (5, 4, 40, 46),
    "small": (3, 2, 24, 30),
}
FLOOD_W, FLOOD_H, FLOOD_D = 11.0, 7.0, 5.0
FLOOD_TILT = 20.0


def head_polys(kind, lit):
    cols, rows, half, top = HEADS[kind]
    lens = "lens_on" if lit else "lens_off"
    P = []
    P += vc.opost(8, 10, 3, 0, 10, "galv")
    x0, x1 = 8 - half, 8 + half
    P += obox(x0 + 2, x1 - 2, 10, 12, 9, 11, "frame")
    P += obox(x0 + 2, x1 - 2, top - 2, top, 9, 11, "frame")
    P += obox(x0, x0 + 2, 10, top, 9, 11, "frame")
    P += obox(x1 - 2, x1, 10, top, 9, 11, "frame")
    P += obox(7, 9, 12, top - 2, 9, 11, "frame", faces="nsew")
    pitch_x = (x1 - x0 - 4) / float(cols)
    pitch_y = (top - 12 - 2) / float(rows)
    for r in range(rows):
        yc = 12 + pitch_y * (r + 0.5)
        P += obox(x0 + 2, x1 - 2, yc - 0.75, yc + 0.75, 9.5, 10.5, "frame", faces="nsud")
        for c in range(cols):
            xc = x0 + 2 + pitch_x * (c + 0.5)
            hx, hy = FLOOD_W / 2, FLOOD_H / 2
            body = obox(xc - hx, xc + hx, yc - hy, yc + hy, 2, 2 + FLOOD_D, "housing",
                        over={"n": (lens, (0, 0, 1, 1))})
            P += turned(body, (xc, yc, 2 + FLOOD_D), FLOOD_TILT)
            # the yoke: an arm each side back to the rail
            P += obox(xc - hx - 0.75, xc - hx, yc - 0.6, yc + 0.6, 4, 9.5, "frame",
                      faces="nsewud")
            P += obox(xc + hx, xc + hx + 0.75, yc - 0.6, yc + 0.6, 4, 9.5, "frame",
                      faces="nsewud")
    return P


HEAD_MTL_ON = {"galv": T("galv"), "frame": T("steel_dark"), "housing": T("flood_housing"),
               "lens_on": T("flood_lens_on")}
HEAD_MTL_OFF = {"galv": T("galv"), "frame": T("steel_dark"), "housing": T("flood_housing"),
                "lens_off": T("flood_lens_off")}
C.extra["models/block/venues/exterior/head_on.mtl"] = vc.mtl_text(GEN, HEAD_MTL_ON)
C.extra["models/block/venues/exterior/head_off.mtl"] = vc.mtl_text(GEN, HEAD_MTL_OFF)


def head_boxes(kind):
    _c, _r, half, top = HEADS[kind]
    return [(8 - half, 8, 2, 8 + half, top, 12), (5, 0, 7, 11, 10, 13)]


def head_icon(kind):
    cols, rows, _h, _t = HEADS[kind]
    tex = {"frame": T("steel_dark"), "lens": T("flood_lens_on"), "housing": T("flood_housing"),
           "galv": T("galv"), "particle": T("flood_housing")}
    els = [box([7, 0, 7], [9, 4, 9], "galv"), box([1, 4, 8], [15, 5, 9], "frame"),
           box([1, 15, 8], [15, 16, 9], "frame"), box([1, 4, 8], [2, 16, 9], "frame"),
           box([14, 4, 8], [15, 16, 9], "frame")]
    pw = 12.0 / cols
    ph = 11.0 / rows
    for r in range(rows):
        for c in range(cols):
            fx0 = 2 + pw * c + 0.3
            fy0 = 5 + ph * r + 0.3
            el = box([fx0, fy0, 5], [fx0 + pw - 0.6, fy0 + ph - 0.6, 8], "housing")
            uv(el, "north", "lens", [0, 0, 16, 16])
            els.append(el)
    return vc.model(tex, els)


for _kind, _reg, _names in (
        ("large", "stadium_light_head",
         ("Stadium Light Head", "Flutlichtkopf", "Cabezal de Iluminación de Estadio",
          "Strålkastarhuvud")),
        ("small", "stadium_light_head_small",
         ("Stadium Light Head (Small)", "Flutlichtkopf (klein)",
          "Cabezal de Iluminación de Estadio (Pequeño)", "Strålkastarhuvud (Litet)"))):
    write_obj("head_%s_on" % _kind, "head_on", head_polys(_kind, True))
    write_obj("head_%s_off" % _kind, "head_off", head_polys(_kind, False))
    C.add(_reg, unit_java("BlockLightHead", _reg, head_boxes(_kind)), _names,
          {"head_%s_item" % _kind: head_icon(_kind)},
          {"forge_marker": 1, "defaults": {"model": M("head_%s_on" % _kind) + ".obj",
                                           "custom": {"ambient": False}},
           "variants": {"facing": {f: ys(y) for f, y in FACINGS},
                        "lit": {"true": {}, "false": {"model": M("head_%s_off" % _kind)
                                                      + ".obj"}},
                        "inventory": [{"model": M("head_%s_item" % _kind), "custom": {}}]}},
          tab=SEAT)


# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
L = C.add_lang
for _i, _n in enumerate((("Event line", "Veranstaltungszeile", "Línea del evento",
                          "Evenemangsrad"),
                         ("Second line", "Zweite Zeile", "Segunda línea", "Andra raden"),
                         ("Third line", "Dritte Zeile", "Tercera línea", "Tredje raden"))):
    L("gui.csm.venue_text.marquee.%d" % _i, _n)
L("gui.csm.venue_text.choice.mode", ("Show: %s", "Anzeige: %s", "Mostrar: %s", "Visa: %s"))
for _i, _n in enumerate((("text and video in turn", "Text und Video im Wechsel",
                          "texto y vídeo alternados", "text och video växelvis"),
                         ("text only", "nur Text", "solo texto", "bara text"),
                         ("video only", "nur Video", "solo vídeo", "bara video"))):
    L("csm.venues.marquee.mode.%d" % _i, _n)
L("csm.venues.box_office.legend", ("Header: %s", "Schild: %s", "Rótulo: %s", "Skylt: %s"))
for _i, _n in enumerate((("BOX OFFICE", "BOX OFFICE", "BOX OFFICE", "BOX OFFICE"),
                         ("WILL CALL", "WILL CALL", "WILL CALL", "WILL CALL"),
                         ("TICKETS", "TICKETS", "TICKETS", "TICKETS"),
                         ("blank", "leer", "vacío", "tom"))):
    L("csm.venues.box_office.legend.%d" % _i, _n)


# ------------------------------------------------------------------------------------------
# Main
# ------------------------------------------------------------------------------------------
def main():
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, f) for dp, _, fs in os.walk(C.assets) for f in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
