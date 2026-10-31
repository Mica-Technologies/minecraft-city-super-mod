#!/usr/bin/env python3
"""
gen_venues_seating.py -- every asset the Sports & Venues seating bowl ships.

    py -3 dev-env-utils/scripts/gen_venues_seating.py               # write
    py -3 dev-env-utils/scripts/gen_venues_seating.py --check       # fail on drift
    py -3 dev-env-utils/scripts/gen_venues_seating.py --fragments   # the tab's lines

The blocks (all in the Sports & Venues - Seating tab, CsmTabVenuesSeating):

  * **Stadium seats** (BlockStadiumSeat), one block per colour, every colour drawing the same
    grey models with the shell tinted (tint index 0) by the block's colour. A seat is a multipart
    of: the seat itself (pan and back, down or folded, 2-deep or 1-deep); the standard on its left
    boundary (half in each cell, an armrest and a cupholder on top), drawn unless the cell to its
    left holds no seat of the row; and an end standard with a blank aluminium plate on either side
    where the row stops (actual state). 2-deep seats stand on the floor; 1-deep seats hang their
    standards off the riser behind them and are shallower.
  * **Aisle-end seats** (BlockStadiumAisleEnd): the same parts, the end standard's plate carrying
    the row letter (one 256 px sheet, a plate model per letter and side), everything else read
    from the seat beside it.
  * the companion seat, the wheelchair space marking, the aisle step light (with an OptiFine
    ``_e`` lens), the centre-of-aisle handrail and the glass guards (OBJ, since a 1 in 2 slope is
    no angle a JSON element can be turned to), and the section and portal signs (Core's wayfinding
    panel; only their item models and blockstates are written here).

Rails and guards follow the aisle's nosing line (StadiumSlope): level; 1 in 1 over a stair a row
(steep, line y = z - 8 in the cell over the stair); and 1 in 2, a tread then a stair (shallow
low over the tread, line y = z / 2; shallow high over the stair a block up, y = z / 2 - 8). The
handrail runs 13 px above that line, a guard's glass top 15 px.
"""
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_venues_common as vc  # noqa: E402

GEN = "gen_venues_seating.py"
C = vc.catalogue(GEN, "seating")
T = C.T
M = C.M
SEATING = vc.SEATING
FACINGS = vc.FACINGS
box = vc.box

# ------------------------------------------------------------------------------------------
# Colours: (registry suffix, Java constant 0xRRGGBB, four-language names)
# ------------------------------------------------------------------------------------------
COLOURS = [
    ("charcoal", 0x3A3C40, ("Charcoal", "Anthrazit", "Carbón", "Antracit")),
    ("navy", 0x1C2A56, ("Navy", "Marineblau", "Azul Marino", "Marinblå")),
    ("ice_blue", 0x9CC8E6, ("Ice Blue", "Eisblau", "Azul Hielo", "Isblå")),
    ("crimson", 0x961C2C, ("Crimson", "Karmesinrot", "Carmesí", "Karmosinröd")),
    ("gold", 0xD6A82A, ("Gold", "Gold", "Dorado", "Guld")),
    ("silver", 0xB0B4BA, ("Silver", "Silber", "Plateado", "Silver")),
    ("black", 0x1C1C1E, ("Black", "Schwarz", "Negro", "Svart")),
    ("red", 0xC42424, ("Red", "Rot", "Rojo", "Röd")),
    ("green", 0x1E6E38, ("Green", "Grün", "Verde", "Grön")),
    ("white", 0xEEEEEA, ("White", "Weiß", "Blanco", "Vit")),
]

LABELS = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q",
          "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA", "BB", "CC", "DD", "EE", "FF"]

# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
C.textures["shell"] = lambda: vc.swatch(vc.SHELL_GREY, 11, grain=3)
C.textures["shell_shade"] = lambda: vc.swatch(vc.shade(vc.SHELL_GREY, 0.74), 12, grain=3)
C.textures["cast"] = lambda: vc.swatch(vc.CAST, 13, grain=4)
C.textures["armrest"] = lambda: vc.swatch(vc.ARMREST, 14, grain=2)
C.textures["aluminium"] = lambda: vc.brushed(vc.ALUMINIUM, 15)
C.textures["stainless"] = lambda: vc.brushed(vc.STAINLESS, 16)
C.textures["steel_dark"] = lambda: vc.swatch(vc.STEEL_DARK, 17, grain=3)


@C.texture("cup")
def _cup():
    img = vc.swatch(vc.ARMREST, 18, grain=2)
    vc.disc(img, 8, 8, 6.2, vc.shade(vc.ARMREST, 1.6))
    vc.disc(img, 8, 8, 5.0, (10, 10, 11))
    return img


@C.texture("plate_blank")
def _plate_blank():
    img = vc.brushed(vc.ALUMINIUM, 19)
    vc.frame(img, 0, 0, 16, 16, vc.shade(vc.ALUMINIUM, 0.72))
    return img


# The row letter sheet: 256 px, cells 32 x 24 (a plate 4 x 3 px), eight across.
CELL_W, CELL_H, SHEET = 32, 24, 256


def label_cell(i):
    """The uv window (in sixteenths of the sheet) of row letter i's cell."""
    cx, cy = (i % 8) * CELL_W, (i // 8) * CELL_H
    k = 16.0 / SHEET
    return [round(cx * k, 4), round(cy * k, 4), round((cx + CELL_W) * k, 4),
            round((cy + CELL_H) * k, 4)]


@C.texture("row_labels")
def _row_labels():
    img = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
    base = vc.brushed(vc.ALUMINIUM, 20, size=SHEET)
    img.paste(base, (0, 0))
    for i, text in enumerate(LABELS):
        cx, cy = (i % 8) * CELL_W, (i // 8) * CELL_H
        vc.frame(img, cx, cy, cx + CELL_W, cy + CELL_H, vc.shade(vc.ALUMINIUM, 0.66))
        scale = 4 if len(text) == 1 else 3
        w = vc.text_width(text, scale)
        h = 5 * scale
        vc.draw_text(img, text, cx + (CELL_W - w) // 2, cy + (CELL_H - h) // 2, (20, 20, 22),
                     scale)
    return img


@C.texture("glass")
def _glass():
    rng = random.Random(21)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            a = 64 + rng.randint(-3, 3)
            if x in (0, 15):
                a = 110
            px[x, y] = (196, 218, 222, a)
    # a faint diagonal sheen
    for i in range(16):
        for d in (0, 1):
            x, y = (i + 3 + d) % 16, 15 - i
            if 1 <= x <= 14:
                px[x, y] = (232, 244, 246, 92)
    return img


@C.texture("glass_item")
def _glass_item():
    # the glass for the inventory icon only: denser, so the pane reads in a 16 px slot
    img = _glass()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, min(255, a + 90))
    return img


@C.texture("step_housing")
def _step_housing():
    return vc.brushed(vc.ALUMINIUM, 22)


def _lens(lit_only):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0) if lit_only else (40, 40, 42, 255))
    for x in range(1, 15):
        for y in range(5, 11):
            edge = x in (1, 14) or y in (5, 10)
            c = vc.shade(vc.LED_WARM, 0.86 if edge else 1.0)
            img.putpixel((x, y), c + (255,))
    return img


C.textures["step_lens"] = lambda: _lens(False)
C.textures["step_lens_e"] = lambda: _lens(True)


@C.texture("wheelchair")
def _wheelchair():
    s = 32
    img = Image.new("RGBA", (s, s), vc.ACCESS_BLUE + (255,))
    white = (246, 246, 246)
    vc.frame(img, 1, 1, s - 1, s - 1, white, width=1)
    px = img.load()

    def dot(x, y, r):
        for yy in range(s):
            for xx in range(s):
                if (xx + 0.5 - x) ** 2 + (yy + 0.5 - y) ** 2 <= r * r:
                    px[xx, yy] = white + (255,)

    def line(x0, y0, x1, y1, w):
        steps = 40
        for i in range(steps + 1):
            t = i / float(steps)
            dot(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, w / 2.0)
    # the international symbol of access: head, back, arm, seat, leg, and the wheel's open ring
    dot(15.5, 7.0, 2.2)
    line(15.0, 10.0, 14.2, 17.5, 2.4)
    line(14.6, 12.6, 19.5, 12.6, 1.8)
    line(14.2, 17.5, 20.5, 17.5, 2.2)
    line(20.5, 17.5, 23.2, 23.5, 2.2)
    for k in range(0, 300, 4):
        import math
        a = math.radians(k + 60)
        dot(14.0 + 6.4 * math.cos(a), 20.5 + 6.4 * math.sin(a), 0.95)
    return img


@C.texture("section_icon")
def _section_icon():
    img = vc.swatch((28, 42, 68), 23, grain=2)
    vc.frame(img, 0, 0, 16, 16, (16, 24, 40))
    vc.draw_text_centred(img, "101", 8, 6, (242, 244, 248))
    return img


@C.texture("portal_icon")
def _portal_icon():
    img = vc.swatch((21, 23, 26), 24, grain=2)
    vc.frame(img, 0, 0, 16, 16, (8, 8, 9))
    vc.draw_text_centred(img, "SEC", 8, 3, (255, 255, 255))
    vc.draw_text_centred(img, "101", 8, 9, (255, 255, 255))
    return img


# ------------------------------------------------------------------------------------------
# Seat parts (facing north: the sitter faces -z, the riser behind at z = 16)
# ------------------------------------------------------------------------------------------
TEX = {"shell": T("shell"), "shell_shade": T("shell_shade"), "cast": T("cast"),
       "armrest": T("armrest"), "cup": T("cup"), "plate": T("plate_blank"),
       "labels": T("row_labels"), "particle": T("cast")}
P0, P1 = 1.5, 14.5      # the pan's and back's sides


def shell_box(frm, to, rear=None, under=None):
    """A part of the moulded shell, tinted: its faces in the light grey, ``rear``/``under``
    faces in the shaded grey (the shell's back and underside)."""
    per = {}
    if rear:
        per[rear] = "shell_shade"
    if under:
        per[under] = "shell_shade"
    return vc.tinted(box(frm, to, "shell", per=per))


def seat_parts(shape, up):
    """The seat: pan and back (and the hinge bar under them)."""
    els = []
    if shape == "floor":
        els.append(box([P0, 5.5, 9.5], [P1, 6.5, 10.5], "cast"))
        back = shell_box([P0, 8.5, 11.0], [P1, 16.0, 12.75], rear="south")
        back["rotation"] = {"origin": [8, 8.5, 11.0], "axis": "x", "angle": 22.5}
        els.append(back)
        if up:
            els.append(shell_box([P0, 7.0, 9.0], [P1, 15.0, 11.0], rear="south"))
        else:
            els.append(shell_box([P0, 6.5, 3.0], [P1, 8.5, 11.0], under="down"))
    else:
        els.append(box([P0, 5.5, 12.0], [P1, 6.5, 13.0], "cast"))
        els.append(box([3.0, 9.0, 15.5], [13.0, 11.0, 16.0], "cast"))
        els.append(shell_box([P0, 8.5, 13.5], [P1, 16.0, 15.5], rear="south"))
        if up:
            els.append(shell_box([P0, 7.0, 11.5], [P1, 15.0, 13.5], rear="south"))
        else:
            els.append(shell_box([P0, 6.5, 5.5], [P1, 8.5, 13.5], under="down"))
    return els


def standard_parts(shape):
    """The standard on the seat's left boundary, half in each cell: an armrest with a cupholder
    in front, on a cast leg (2-deep) or a bracket off the riser (1-deep)."""
    els = []
    if shape == "floor":
        els.append(box([-0.75, 0, 8.0], [0.75, 10.5, 12.0], "cast"))
        els.append(box([-1.0, 0, 5.0], [1.0, 0.75, 14.0], "cast"))
        els.append(box([-0.75, 9.5, 4.5], [0.75, 10.5, 8.0], "cast"))
        els.append(box([-1.0, 10.5, 3.5], [1.0, 11.5, 12.5], "armrest"))
        els.append(box([-1.25, 9.25, 1.25], [1.25, 11.0, 3.5], "armrest", per={"up": "cup"}))
    else:
        els.append(box([-0.75, 4.0, 10.0], [0.75, 10.5, 16.0], "cast"))
        els.append(box([-0.75, 9.5, 5.0], [0.75, 10.5, 10.0], "cast"))
        els.append(box([-1.0, 10.5, 4.0], [1.0, 11.5, 13.0], "armrest"))
        els.append(box([-1.25, 9.25, 1.75], [1.25, 11.0, 4.0], "armrest", per={"up": "cup"}))
    return els


def end_parts(shape):
    """The end standard on the seat's left, inside its cell: a full side panel and its armrest.
    The plate on its outer face is a part of its own."""
    if shape == "floor":
        return [box([0.2, 0, 2.5], [1.5, 11.0, 14.5], "cast"),
                box([0.2, 11.0, 2.5], [1.5, 12.0, 14.0], "armrest")]
    return [box([0.2, 3.0, 3.0], [1.5, 11.0, 16.0], "cast"),
            box([0.2, 11.0, 4.0], [1.5, 12.0, 14.0], "armrest")]


# the plate on an end standard's outer face, 4.67 x 3.5 px (the letter cells' 4:3), above
# a vanilla stair's half step and in front of its upper half, so a 2-deep row's aisle stair
# beside it never hides it
PLATE = ([0.0, 8.25, 3.0], [0.2, 11.75, 7.67])


def plate_parts(tex, uv=None):
    el = box(PLATE[0], PLATE[1], tex, faces=("west", "north", "south", "up", "down"))
    if uv is not None:
        el["faces"]["west"]["uv"] = uv
    return [el]


def seat_model(els):
    return vc.model(TEX, els)


def part_name(*bits):
    return "_".join(bits)


MODELS = {}
for shape in ("floor", "riser"):
    for up in (False, True):
        MODELS[part_name("seat", shape, "up" if up else "down")] = seat_model(seat_parts(shape, up))
    MODELS[part_name("standard", shape)] = seat_model(standard_parts(shape))
    left = end_parts(shape)
    MODELS[part_name("end", shape, "left")] = seat_model(left)
    MODELS[part_name("end", shape, "right")] = seat_model([vc.mirror_x(e) for e in left])
blank = plate_parts("plate")
MODELS["plate_blank_left"] = seat_model(blank)
MODELS["plate_blank_right"] = seat_model([vc.mirror_x(e) for e in blank])
for i, text in enumerate(LABELS):
    uv = label_cell(i)
    el = plate_parts("labels", uv)
    MODELS["plate_%s_left" % text.lower()] = seat_model(el)
    right = [vc.mirror_x(e) for e in el]
    right[0]["faces"]["east"]["uv"] = uv
    MODELS["plate_%s_right" % text.lower()] = seat_model(right)

SEAT_ITEM = seat_model(end_parts("floor") + plate_parts("plate") + seat_parts("floor", False)
                       + [vc.mirror_x(e) for e in end_parts("floor") + plate_parts("plate")])


def when(**cond):
    return {k: ("true" if v is True else "false" if v is False else str(v))
            for k, v in cond.items()}


def apply(name, y):
    a = {"model": M(name)}
    if y:
        a["y"] = y
    return a


def seat_rules():
    rules = []
    for facing, y in FACINGS:
        for riser in (False, True):
            shape = "riser" if riser else "floor"
            for up in (False, True):
                rules.append({"when": when(facing=facing, riser=riser, up=up),
                              "apply": apply(part_name("seat", shape, "up" if up else "down"), y)})
            rules.append({"when": when(facing=facing, riser=riser, end_l=False),
                          "apply": apply(part_name("standard", shape), y)})
            rules.append({"when": when(facing=facing, riser=riser, end_l=True),
                          "apply": apply(part_name("end", shape, "left"), y)})
            rules.append({"when": when(facing=facing, riser=riser, end_r=True),
                          "apply": apply(part_name("end", shape, "right"), y)})
        rules.append({"when": when(facing=facing, end_l=True), "apply": apply("plate_blank_left", y)})
        rules.append({"when": when(facing=facing, end_r=True),
                      "apply": apply("plate_blank_right", y)})
    return {"multipart": rules}


def aisle_end_rules(offset):
    rules = []
    for facing, y in FACINGS:
        for riser in (False, True):
            shape = "riser" if riser else "floor"
            for up in (False, True):
                rules.append({"when": when(facing=facing, riser=riser, up=up),
                              "apply": apply(part_name("seat", shape, "up" if up else "down"), y)})
            rules.append({"when": when(facing=facing, riser=riser, right=False),
                          "apply": apply(part_name("end", shape, "left"), y)})
            rules.append({"when": when(facing=facing, riser=riser, right=True),
                          "apply": apply(part_name("standard", shape), y)})
            rules.append({"when": when(facing=facing, riser=riser, right=True),
                          "apply": apply(part_name("end", shape, "right"), y)})
        for i in range(16):
            text = LABELS[offset + i].lower()
            rules.append({"when": when(facing=facing, label=i, right=False),
                          "apply": apply("plate_%s_left" % text, y)})
            rules.append({"when": when(facing=facing, label=i, right=True),
                          "apply": apply("plate_%s_right" % text, y)})
    return {"multipart": rules}


SEAT = ("Stadium Seat", "Stadionsitz", "Asiento de Estadio", "Arenastol")
first = True
for key, rgb, names in COLOURS:
    reg = "stadium_seat_" + key
    models = dict(MODELS) if first else {}
    if first:
        models["seat_item"] = SEAT_ITEM
        first = False
    C.add(reg, 'new BlockStadiumSeat("%s", 0x%06X)' % (reg, rgb),
          tuple("%s (%s)" % (SEAT[i], names[i]) for i in range(4)), models, seat_rules(),
          item={"parent": "csm:block/venues/seating/seat_item"}, tab=SEATING)

AISLE = ("Stadium Seat, Aisle End", "Stadionsitz, Gangende", "Asiento de Estadio, Fin de Pasillo",
         "Arenastol, Radände")
item_end = seat_model(end_parts("floor") + plate_parts("labels", label_cell(0))
                      + seat_parts("floor", False))
for reg, offset, rows in (("stadium_seat_aisle_end", 0, "A-P"),
                          ("stadium_seat_aisle_end_2", 16, "Q-FF")):
    models = {}
    if offset == 0:
        models["aisle_end_item"] = item_end
    C.add(reg, 'new BlockStadiumAisleEnd("%s", %d)' % (reg, offset),
          tuple("%s (%s)" % (AISLE[i], rows) for i in range(4)), models, aisle_end_rules(offset),
          item={"parent": "csm:block/venues/seating/aisle_end_item"}, tab=SEATING)


# ------------------------------------------------------------------------------------------
# Companion seat and wheelchair space
# ------------------------------------------------------------------------------------------
def companion():
    tex = {"shell": T("shell"), "shell_shade": T("shell_shade"), "frame": T("steel_dark"),
           "particle": T("steel_dark")}
    els = []
    for x in (3.0, 12.0):
        els.append(box([x, 0, 3.5], [x + 1, 8.0, 4.5], "frame"))
        els.append(box([x, 0, 12.0], [x + 1, 15.0, 13.0], "frame"))
        els.append(box([x, 1.5, 4.5], [x + 1, 2.5, 12.0], "frame"))
    els.append(box([4.0, 7.0, 4.0], [12.0, 8.0, 5.0], "frame"))
    els.append(vc.tinted(box([2.5, 8.0, 3.0], [13.5, 9.5, 12.5], "shell", per={"down": "shell_shade"})))
    els.append(vc.tinted(box([2.5, 10.0, 12.6], [13.5, 15.5, 14.0], "shell",
                             per={"south": "shell_shade"})))
    return vc.model(tex, els)


C.add("stadium_companion_seat", 'new BlockStadiumCompanionSeat("stadium_companion_seat")',
      ("Stadium Companion Seat", "Stadion-Begleitsitz", "Asiento de Acompañante de Estadio",
       "Arenastol för Ledsagare"),
      {"companion_seat": companion()}, vc.facing_state(M("companion_seat")), tab=SEATING)

C.add("stadium_wheelchair_space", 'new BlockStadiumWheelchairSpace("stadium_wheelchair_space")',
      ("Wheelchair Space Marking", "Rollstuhlplatz-Markierung", "Marca de Espacio para Silla de Ruedas",
       "Markering för Rullstolsplats"),
      {"wheelchair_space": vc.model({"mark": T("wheelchair"), "particle": T("wheelchair")},
                                    [box([0, 0, 0], [16, 0.1, 16], "mark", faces=("up",))],
                                    ao=False)},
      vc.facing_state(M("wheelchair_space")), tab=SEATING)


# ------------------------------------------------------------------------------------------
# Aisle step light
# ------------------------------------------------------------------------------------------
def step_light(y0):
    tex = {"housing": T("step_housing"), "lens": T("step_lens"), "particle": T("step_housing")}
    els = [box([4.5, y0, 15.0], [11.5, y0 + 3.0, 16.0], "housing",
               faces=("north", "east", "west", "up", "down")),
           box([4.5, y0 + 2.5, 14.25], [11.5, y0 + 3.0, 15.0], "housing"),
           box([5.25, y0 + 0.5, 14.9], [10.75, y0 + 2.5, 15.0], "lens", faces=("north",))]
    els[-1]["faces"]["north"]["uv"] = [1, 5, 15, 11]
    return vc.model(tex, els, ao=False)


def step_light_item():
    # the low light lifted to the middle of the cell and shown larger, so the slot shows it
    m = step_light(6.5)
    for el in m["elements"]:
        el["from"][2] -= 7.0
        el["to"][2] -= 7.0
    m["display"] = {"gui": {"rotation": [20, 200, 0], "scale": [1.5, 1.5, 1.5]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                    "fixed": {"rotation": [0, 180, 0], "scale": [1.2, 1.2, 1.2]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                              "scale": [0.6, 0.6, 0.6]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.8, 0.8, 0.8]}}
    return m


C.add("stadium_aisle_step_light", 'new BlockStadiumStepLight("stadium_aisle_step_light")',
      ("Aisle Step Light", "Gangstufenleuchte", "Luz de Escalón de Pasillo", "Trappstegsbelysning"),
      {"step_light_low": step_light(3.0), "step_light_high": step_light(11.0),
       "step_light_item": step_light_item()},
      vc.facing_state(M("step_light_low"),
                      {"high": {"false": {}, "true": {"model": M("step_light_high")}},
                       "inventory": [{"model": M("step_light_item")}]}),
      tab=SEATING)


# ------------------------------------------------------------------------------------------
# Rails and guards (OBJ)
# ------------------------------------------------------------------------------------------
# The nosing line of each piece in its own cell (y at z), and the treads under it
LINE = {
    "level": lambda z: 0.0,
    "steep": lambda z: z - 8.0,
    "shallow_low": lambda z: z / 2.0,
    "shallow_high": lambda z: z / 2.0 - 8.0,
}
TREADS = {
    "level": [(0.0, 16.0, 0.0)],
    "steep": [(0.0, 8.0, -8.0), (8.0, 16.0, 0.0)],
    "shallow_low": [(0.0, 16.0, 0.0)],
    "shallow_high": [(0.0, 8.0, -8.0), (8.0, 16.0, 0.0)],
}
PIECES = ["level", "steep", "shallow_low", "shallow_high"]
RAIL_UP = 13.0      # the handrail's centre above the nosing line
GLASS_UP = 15.0     # a guard's glass top above it


def write_obj(name, mtl, polys):
    o = vc.Obj(GEN, mtl, name)
    for p in polys:
        o.add(p)
    C.extra["models/block/venues/seating/%s.obj" % name] = o.text()
    return M(name) + ".obj"


def rail_line(piece):
    base = LINE[piece]
    if piece == "level":
        return lambda z: RAIL_UP
    return lambda z: base(z) + RAIL_UP


def handrail(piece):
    line = rail_line(piece)
    polys = vc.orail_z(8.0, line, 0.85, 0.0, 16.0, "rail")
    zp = 8.0 if piece in ("level", "shallow_low") else 12.0
    polys += vc.opost(8.0, zp, 0.7, 0.0, line(zp), "rail", cap=False)
    polys += vc.obox(6.75, 9.25, 0.0, 0.4, zp - 1.25, zp + 1.25, "rail", faces="nsewu")
    return polys


C.extra["models/block/venues/seating/handrail.mtl"] = vc.mtl_text(GEN, {"rail": T("stainless")})
rail_models = {p: write_obj("handrail_" + p, "handrail", handrail(p)) for p in PIECES}
rail_state = {"forge_marker": 1, "defaults": {"model": rail_models["level"]},
              "variants": {"facing": {f: ({"y": y} if y else {}) for f, y in FACINGS},
                           "piece": {p: {"model": rail_models[p]} for p in PIECES},
                           "inventory": [{"model": M("handrail_item")}]}}
rail_item = vc.model({"rail": T("stainless"), "particle": T("stainless")},
                     [box([7.15, 12.15, 0], [8.85, 13.85, 16], "rail"),
                      box([7.3, 0, 7.3], [8.7, 12.15, 8.7], "rail"),
                      box([6.75, 0, 6.75], [9.25, 0.4, 9.25], "rail")])
C.add("stadium_aisle_handrail", 'new BlockStadiumHandrail("stadium_aisle_handrail")',
      ("Aisle Handrail", "Ganghandlauf", "Pasamanos de Pasillo", "Gångledstång"),
      {"handrail_item": rail_item}, rail_state, tab=SEATING)


def guard_level():
    tex = {"shoe": T("aluminium"), "glass": T("glass"), "cap": T("stainless"),
           "particle": T("aluminium")}
    return vc.model(tex, [box([0, 0, 0.5], [16, 2.0, 3.0], "shoe"),
                          box([0, 2.0, 1.5], [16, 15.0, 2.0], "glass", faces=("north", "south")),
                          box([0, 15.0, 1.0], [16, 16.25, 2.5], "cap")])


def guard_slope(piece, right):
    """A sloped guard cell: the glass at the left (x 1.5 to 2) or right edge, standing on the
    treads under it, its top on the nosing line plus GLASS_UP, the cap rail above."""
    line = LINE[piece]
    x0, x1 = (1.5, 2.0) if not right else (14.0, 14.5)
    top = (lambda z: line(z) + GLASS_UP)
    polys = []
    for z0, z1, floor in TREADS[piece]:
        lo = (lambda f: (lambda z: f + 2.0))(floor)
        polys += vc.oplane_x(x0, lo, top, z0, z1, "glass")
        polys += vc.oplane_x(x1, lo, top, z0, z1, "glass")
        polys += vc.obar_z(x0 - 1.0, x1 + 1.0, (lambda f: (lambda z: f))(floor), lo, z0, z1, "shoe")
    polys += vc.obar_z(x0 - 0.5, x1 + 0.5, top, (lambda z: top(z) + 1.25), 0.0, 16.0, "cap")
    return polys


C.extra["models/block/venues/seating/glass_guard.mtl"] = vc.mtl_text(
    GEN, {"glass": T("glass"), "shoe": T("aluminium"), "cap": T("stainless")})
GUARD_ITEM = guard_level()
GUARD_ITEM["textures"]["glass"] = T("glass_item")
C.add("stadium_glass_guard",
      'new BlockStadiumGlassGuard("stadium_glass_guard", BlockStadiumGlassGuard.Kind.LEVEL)',
      ("Glass Guard", "Glasbrüstung", "Barandilla de Vidrio", "Glasräcke"),
      {"glass_guard_level": GUARD_ITEM}, vc.facing_state(M("glass_guard_level")), tab=SEATING)


def guard_state(pieces):
    variants = {}
    for facing, y in FACINGS:
        for right in (False, True):
            for piece in pieces:
                name = "glass_guard_%s_%s" % (piece, "right" if right else "left")
                key = ["facing=%s" % facing, "right=%s" % ("true" if right else "false")]
                if len(pieces) > 1:
                    key.append("high=%s" % ("true" if piece == "shallow_high" else "false"))
                v = {"model": M(name) + ".obj"}
                if y:
                    v["y"] = y
                variants[",".join(sorted(key))] = [v]
    variants["inventory"] = [{"model": M("glass_guard_level")}]
    return {"forge_marker": 1, "variants": variants}


for piece in ("steep", "shallow_low", "shallow_high"):
    for right in (False, True):
        write_obj("glass_guard_%s_%s" % (piece, "right" if right else "left"), "glass_guard",
                  guard_slope(piece, right))
C.add("stadium_glass_guard_slope",
      'new BlockStadiumGlassGuard("stadium_glass_guard_slope", BlockStadiumGlassGuard.Kind.SLOPE)',
      ("Glass Guard (1:2 Slope)", "Glasbrüstung (Neigung 1:2)", "Barandilla de Vidrio (Pendiente 1:2)",
       "Glasräcke (Lutning 1:2)"),
      {}, guard_state(["shallow_low", "shallow_high"]), tab=SEATING)
C.add("stadium_glass_guard_steep",
      'new BlockStadiumGlassGuard("stadium_glass_guard_steep", BlockStadiumGlassGuard.Kind.STEEP)',
      ("Glass Guard (1:1 Slope)", "Glasbrüstung (Neigung 1:1)", "Barandilla de Vidrio (Pendiente 1:1)",
       "Glasräcke (Lutning 1:1)"),
      {}, guard_state(["steep"]), tab=SEATING)


# ------------------------------------------------------------------------------------------
# Section and portal signs (Core's wayfinding panel)
# ------------------------------------------------------------------------------------------
def sign_item(face_tex):
    tex = {"face": T(face_tex), "rim": T("steel_dark"), "particle": T("steel_dark")}
    return vc.model(tex, [box([0, 2, 7.0], [16, 14, 9.0], "rim"),
                          box([0.5, 2.5, 6.9], [15.5, 13.5, 9.1], "face",
                              faces=("north", "south"))])


def sign_state(item):
    return {"forge_marker": 1, "defaults": {"model": "csm:wayfinding/wayfinding_panel_base"},
            "variants": {"facing": {f: ({"y": y} if y else {}) for f, y in FACINGS},
                         "inventory": [{"model": M(item)}]}}


_section_item = sign_item("section_icon")
_section_item["elements"][1]["faces"]["north"]["uv"] = [0, 0, 16, 16]
_section_item["elements"][1]["faces"]["south"]["uv"] = [0, 0, 16, 16]
_portal_item = sign_item("portal_icon")
_portal_item["elements"][1]["faces"]["north"]["uv"] = [0, 0, 16, 16]
_portal_item["elements"][1]["faces"]["south"]["uv"] = [0, 0, 16, 16]
C.add("stadium_section_sign", 'new BlockStadiumSectionSign("stadium_section_sign")',
      ("Section Number Sign", "Blocknummernschild", "Letrero de Número de Sección",
       "Sektionsnummerskylt"),
      {"section_sign_item": _section_item}, sign_state("section_sign_item"), tab=SEATING)
C.add("stadium_portal_sign", 'new BlockStadiumPortalSign("stadium_portal_sign")',
      ("Vomitory Portal Sign", "Mundloch-Portalschild", "Letrero de Vomitorio",
       "Portalskylt för Uppgång"),
      {"portal_sign_item": _portal_item}, sign_state("portal_sign_item"), tab=SEATING)

# ------------------------------------------------------------------------------------------
# Messages
# ------------------------------------------------------------------------------------------
C.add_lang("csm.venues.seat.taken", ("Someone is already sitting there.",
                                     "Hier sitzt schon jemand.", "Ya hay alguien sentado ahí.",
                                     "Någon sitter redan där."))
C.add_lang("csm.venues.aisle_end.row", ("Row %s", "Reihe %s", "Fila %s", "Rad %s"))
for piece, names in (("level", ("Level", "Eben", "Nivelado", "Plan")),
                     ("steep", ("Steep (1:1)", "Steil (1:1)", "Empinado (1:1)", "Brant (1:1)")),
                     ("shallow_low", ("1:2, over the tread", "1:2, über dem Auftritt",
                                      "1:2, sobre la huella", "1:2, över steget")),
                     ("shallow_high", ("1:2, over the stair", "1:2, über der Stufe",
                                       "1:2, sobre el escalón", "1:2, över trappan"))):
    C.add_lang("csm.venues.slope." + piece, names)


def main():
    rc = C.main()
    if rc == 0 and "--fragments" not in sys.argv and "--check" not in sys.argv:
        vc.check_json([os.path.join(C.assets, rel) for rel in
                       [os.path.join(dp, f) for dp, _, fs in os.walk(C.assets) for f in fs]])
    return rc


if __name__ == "__main__":
    sys.exit(main())
