#!/usr/bin/env python3
"""The outdoor fare vending machine (Transit): a weatherproof street ticket machine, two blocks
tall, placed as one piece into its block and the one above, as a door is.

    python dev-env-utils/scripts/gen_transit_fare_vending_outdoor.py
    python dev-env-utils/scripts/gen_transit_fare_vending_outdoor.py --check
    python dev-env-utils/scripts/gen_transit_fare_vending_outdoor.py --fragments

A stainless cabinet with graphite trim on a short concrete-grey plinth, a hooded touchscreen (a
roof plate and two cheeks keep the rain off the screen), keypad and tap pad, cash, card and coin
slots and a ticket tray in relief, and a deeper cap over the whole with a small band in the
agency's colours on its front. The indoor machine (gen_transit_fare_vending.py) is one block drawing
into the air above; this one is two (BlockFareVendingMachineOutdoor's `upper`), so each half has
its own model and the upper half can be clicked.

Each half is drawn facing north, the machine standing a little back from the front of its block;
the multipart blockstate turns it by `facing`. Face art is drawn at 4 texels a model unit in a
window at the top left of a 64 x 64 sheet, the face's UV that window, so nothing is stretched.

It comes in the four invented agencies' liveries, as the shelters and canopies do: one block
each, `fare_vending_machine_outdoor` (CITYLINE, the id it shipped with) and
`fare_vending_machine_outdoor_<agency>` for the other three. The livery is only the cap's band
and the screen's header bar, both in the upper half, so the models are drawn once in CITYLINE's
colours and every other agency's upper and item model is a child naming its own `band` and
`front_upper` textures; the lower half is shared.
"""
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")
SUB = "transit/fare_vending_outdoor"

C = lc.Catalogue("gen_transit_fare_vending_outdoor.py", SUB, SUB, assets=ASSETS)
TAB = "CsmTabTransit"
REG = "fare_vending_machine_outdoor"
PX = 4          # texels a model unit

STAINLESS = (184, 189, 192)
GRAPHITE = (52, 55, 60)
PLINTH = (128, 128, 124)
TEAL = (14, 94, 111)
YELLOW = (244, 196, 40)
WHITE = (236, 240, 240)
SCREEN = (22, 34, 44)
SCREEN_LIT = (206, 232, 240)
GREEN = (60, 200, 110)
STEEL = (150, 156, 160)
RED = (176, 36, 32)

# The agencies, in the shelters' colours (gen_transit_shelters.py's AGENCIES): id, name, the
# band's ground and its accent (the line under the band, the square in the screen's header)
AGENCIES = [
    ("cityline", "CITYLINE", TEAL, YELLOW),
    ("riverway", "RIVERWAY", (31, 58, 115), (240, 138, 36)),
    ("verdant", "VERDANT", (36, 116, 60), (240, 242, 240)),
    ("emberline", "EMBERLINE", RED, GRAPHITE),
]
BASE = "cityline"   # the agency the base models are drawn in, under the id that shipped first

# The cabinet, in each half's own coordinates (north is the front)
X0, X1 = 2.0, 14.0            # 12 wide
Z0, Z1 = 4.5, 13.0            # front face at z 4.5
LOWER_FOOT = 2.0              # the plinth's top
UPPER_TOP = 12.0              # the cabinet's top in the upper half
CAP_TOP = 13.5                # the cap over it


def canvas(colour, seed):
    return lc.fill(colour, size=64, grain=3, seed=seed)


def win(w, h):
    """The UV window a w x h unit face's art is drawn in."""
    return [0, 0, round(w * PX / 4.0, 4), round(h * PX / 4.0, 4)]


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def front_upper(ground=TEAL, accent=YELLOW):
    """The upper front, 12 x 12 units: the screen with a fare menu under the hood, its header
    bar in the agency's colours, the PIN and TAP labels over the keypad and tap pad."""
    img = canvas(STAINLESS, 31)
    w = int((X1 - X0) * PX)
    # the screen, between the hood's cheeks: y 5.5 .. 10.5 (rows 6 .. 26)
    lc.rect(img, 4, 5, w - 4, 27, GRAPHITE)
    lc.rect(img, 6, 7, w - 6, 25, SCREEN)
    lc.rect(img, 7, 8, w - 7, 12, ground)
    lc.rect(img, 8, 9, 11, 11, accent)
    lc.draw_text_centred(img, "FARES", w / 2.0 + 1, 13, SCREEN_LIT)
    for i, c in enumerate(((66, 160, 220), (90, 190, 120))):
        y = 19 + i * 3
        lc.rect(img, 8, y, w - 8, y + 2, c)
    # a north face reads from +x on the viewer's left: the keypad (x 8.8 .. 12.3) is on the
    # left, the tap pad (x 3.7 .. 7.2) on the right
    lc.draw_text(img, "PIN", 9, 27, GRAPHITE)
    lc.draw_text(img, "TAP", 30, 27, GRAPHITE)
    return img


def front_lower():
    """The lower front, 12 x 14 units: the cash and card slot labels, the coin slot, the tray's
    label, a graphite kick plate."""
    img = canvas(STAINLESS, 32)
    w, h = int((X1 - X0) * PX), int((16 - LOWER_FOOT) * PX)
    lc.draw_text(img, "CASH", 30, 9, GRAPHITE)
    lc.draw_text(img, "CARD", 7, 9, GRAPHITE)
    # coin slot under the cash slot
    lc.rect(img, 34, 23, 41, 28, GRAPHITE)
    lc.rect(img, 37, 24, 38, 27, (10, 10, 12))
    # card symbols: generic, no network marks
    for i, c in enumerate(((200, 60, 60), (60, 110, 200), (230, 180, 40))):
        lc.rect(img, 8 + i * 5, 23, 12 + i * 5, 26, c)
    lc.draw_text_centred(img, "TICKETS", w / 2.0, 31, GRAPHITE)
    lc.rect(img, 0, h - 5, w, h, GRAPHITE)
    return img


def side():
    """The cabinet's side, 8.5 units deep (both halves): stainless, a graphite edge at the front
    and the back, a vent grille."""
    img = canvas(STAINLESS, 33)
    w, h = int((Z1 - Z0) * PX), int((16 - LOWER_FOOT) * PX)
    lc.rect(img, 0, 0, 2, h, GRAPHITE)
    lc.rect(img, w - 2, 0, w, h, GRAPHITE)
    for y in range(12, 36, 3):
        lc.rect(img, 9, y, w - 9, y + 1, lc.shade(STAINLESS, 0.6))
    return img


def band(ground=TEAL, accent=YELLOW):
    """The cap's front, 13 x 1.5 units: the agency's colour, its accent in a line under it
    (CITYLINE teal with yellow)."""
    img = canvas(ground, 34)
    w, h = int(13 * PX), int((CAP_TOP - UPPER_TOP) * PX)
    lc.rect(img, 0, h - 1, w, h, accent)
    return img


def keypad():
    """3.5 x 2.5 units: a 3 x 4 grid of steel keys on graphite."""
    img = lc.fill(GRAPHITE, size=64, grain=2, seed=35)
    for r in range(3):
        for c in range(4):
            lc.rect(img, 1 + c * 3, 1 + r * 3, 3 + c * 3, 3 + r * 3, STEEL)
    return img


def tap():
    """3.5 x 2.5 units: a dark pad with the contactless waves and a green light bar."""
    img = lc.fill((24, 28, 32), size=64, grain=2, seed=36)
    for r in (2, 3, 4):
        for y in range(10):
            for x in range(14):
                d = ((x + 0.5 - 4) ** 2 + (y + 0.5 - 5) ** 2) ** 0.5
                if r - 0.5 <= d < r and x >= 4:
                    img.putpixel((x, y), WHITE + (255,))
    lc.rect(img, 1, 8, 13, 9, GREEN)
    return img


def slot():
    img = lc.fill(STEEL, size=64, grain=3, seed=37)
    lc.rect(img, 1, 1, 13, 3, (8, 8, 10))
    return img


def tray():
    img = lc.fill((20, 22, 24), size=64, grain=2, seed=38)
    lc.rect(img, 0, 0, 28, 1, lc.shade(STEEL, 0.9))
    return img


TEXTURES = {
    "front_upper": front_upper,
    "front_lower": front_lower,
    "side": side,
    "band": band,
    "keypad": keypad,
    "tap": tap,
    "slot": slot,
    "tray": tray,
    "trim": lambda: lc.fill(GRAPHITE, size=16, grain=3, seed=39),
    "plinth": lambda: lc.fill(PLINTH, size=16, grain=5, seed=40),
    "steel": lambda: lc.fill(STEEL, size=16, grain=4, seed=41),
}
LIVERY_KEYS = ("band", "front_upper")
for _aid, _name, _ground, _accent in AGENCIES:
    if _aid != BASE:
        TEXTURES["band_" + _aid] = (lambda g=_ground, a=_accent: band(g, a))
        TEXTURES["front_upper_" + _aid] = (lambda g=_ground, a=_accent: front_upper(g, a))
for _name, _draw in TEXTURES.items():
    C.texture(_name)(_draw)


# ------------------------------------------------------------------------------------------
# Elements
# ------------------------------------------------------------------------------------------
def face(tex, uv):
    return {"texture": "#" + tex, "uv": [round(v, 4) for v in uv]}


def box(frm, to, faces):
    return {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}


def plain(frm, to, tex, which):
    """Faces whose UV is the box's own extent, slid into 0..16 (plain fills)."""
    w, h, d = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
    sizes = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h),
             "up": (w, d), "down": (w, d)}
    return {f: face(tex, [0, 0, min(16, sizes[f][0]), min(16, sizes[f][1])]) for f in which}


def cabinet(lo, hi, front, side_h):
    """A half of the cabinet, y lo .. hi, its front art `front`."""
    h = hi - lo
    d = Z1 - Z0
    faces = {"north": face(front, win(X1 - X0, h)),
             "east": face("side", [d, 0, 0, side_h]),
             "west": face("side", [0, 0, d, side_h]),
             "south": face("trim", [0, 0, X1 - X0, min(16, h)])}
    return box([X0, lo, Z0], [X1, hi, Z1], faces)


def relief(x0, x1, y0, y1, depth, tex, art):
    """A part standing proud of the cabinet's front by `depth`: its face art, trim round it."""
    w, h = x1 - x0, y1 - y0
    return box([x0, y0, Z0 - depth], [x1, y1, Z0],
               {"north": face(art, win(w, h)),
                "east": face(tex, [0, 0, depth, h]), "west": face(tex, [0, 0, depth, h]),
                "up": face(tex, [0, 0, w, depth]), "down": face(tex, [0, 0, w, depth])})


def lower_elements():
    els = [box([1.5, 0, 3.5], [14.5, LOWER_FOOT, 13.5],
               plain([1.5, 0, 3.5], [14.5, LOWER_FOOT, 13.5], "plinth",
                     ("north", "south", "east", "west", "up"))),
           cabinet(LOWER_FOOT, 16, "front_lower", 16 - LOWER_FOOT)]
    # card and cash slot bezels, under their labels
    for x0, x1 in ((3.2, 6.8), (9.2, 12.8)):
        els.append(relief(x0, x1, 11, 12, 0.5, "steel", "slot"))
    # the ticket and change tray: a lip standing out, its dark inside seen from above
    els.append(box([4.5, 4, 3.5], [11.5, 6.5, Z0],
                   {"north": face("steel", [0, 0, 7, 2.5]),
                    "east": face("steel", [0, 0, 1, 2.5]), "west": face("steel", [0, 0, 1, 2.5]),
                    "up": face("tray", win(7, 1)), "down": face("steel", [0, 0, 7, 1])}))
    return els


def upper_elements():
    els = [cabinet(0, UPPER_TOP, "front_upper", UPPER_TOP)]
    # keypad and tap pad under the screen
    els.append(relief(8.8, 12.3, 1.5, 4, 0.6, "trim", "keypad"))
    els.append(relief(3.7, 7.2, 1.5, 4, 0.8, "trim", "tap"))
    # the screen's hood: a roof plate and two cheeks
    els.append(box([2.5, 10.9, 2.0], [13.5, 11.5, Z0],
                   plain([2.5, 10.9, 2.0], [13.5, 11.5, Z0], "trim",
                         ("north", "east", "west", "up", "down"))))
    for x0 in (2.5, 12.9):
        els.append(box([x0, 5.5, 2.5], [x0 + 0.6, 10.9, Z0],
                       plain([x0, 5.5, 2.5], [x0 + 0.6, 10.9, Z0], "trim",
                             ("north", "east", "west", "down"))))
    # the cap over the cabinet, overhanging it, the teal band on its front
    els.append(box([1.5, UPPER_TOP, 3.5], [14.5, CAP_TOP, 13.5],
                   {"north": face("band", win(13, CAP_TOP - UPPER_TOP)),
                    "east": face("trim", [0, 0, 10, CAP_TOP - UPPER_TOP]),
                    "west": face("trim", [0, 0, 10, CAP_TOP - UPPER_TOP]),
                    "south": face("trim", [0, 0, 13, CAP_TOP - UPPER_TOP]),
                    "up": face("trim", [0, 0, 13, 10]), "down": face("trim", [0, 0, 13, 10])}))
    return els


def shifted(els, dy):
    out = []
    for e in els:
        e2 = dict(e)
        e2["from"] = [e["from"][0], e["from"][1] + dy, e["from"][2]]
        e2["to"] = [e["to"][0], e["to"][1] + dy, e["to"][2]]
        out.append(e2)
    return out


def check(name, els):
    for e in els:
        for v in e["from"] + e["to"]:
            assert -16 <= v <= 32, (name, e)
        for f in e["faces"].values():
            for v in f["uv"]:
                assert 0 <= v <= 16, (name, f)


AGENCY_TEXTURES = set(k + "_" + a[0] for a in AGENCIES if a[0] != BASE for k in LIVERY_KEYS)


def textures():
    t = {k: C.T(k) for k in TEXTURES if k not in AGENCY_TEXTURES}
    t["particle"] = C.T("side")
    return t


def livery_textures(aid):
    """A child model's textures: only what the agency changes."""
    return {k: C.T(k + "_" + aid) for k in LIVERY_KEYS}


def machine():
    lower, upper = lower_elements(), upper_elements()
    check("lower", lower)
    check("upper", upper)
    base_models = {"lower": lc.model(textures(), lower, ao=False),
                   "upper": lc.model(textures(), upper, ao=False)}
    item_els = lower + shifted(upper, 16)
    check("item", item_els)
    item = lc.model(textures(), item_els, ao=False)
    item["display"] = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0], "scale": [0.36] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2] * 3},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, -3, 0], "scale": [0.36] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.25] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.28] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.28] * 3},
    }
    names = ("Outdoor Fare Vending Machine (%s)", "Fahrscheinautomat (Außen, %s)",
             "Máquina Expendedora de Tarifas (Exterior, %s)", "Biljettautomat (Utomhus, %s)")
    for aid, name, _ground, _accent in AGENCIES:
        reg = REG if aid == BASE else "%s_%s" % (REG, aid)
        if aid == BASE:
            models, upper_ref, agency_item = dict(base_models), "upper", item
        else:
            upper_ref = "%s/upper" % aid
            models = {upper_ref: {"parent": "csm:block/%s/upper" % SUB,
                                  "textures": livery_textures(aid)}}
            agency_item = {"parent": "csm:item/" + REG, "textures": livery_textures(aid)}
        mp = []
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            for half in ("lower", "upper"):
                apply = {"model": C.M(upper_ref if half == "upper" else half)}
                if y:
                    apply["y"] = y
                mp.append({"when": {"facing": facing,
                                    "upper": "true" if half == "upper" else "false"},
                           "apply": apply})
        C.add(reg, 'new BlockFareVendingMachineOutdoor("%s")' % reg,
              tuple(n % name for n in names), models, {"multipart": mp}, item=agency_item,
              tab=TAB)


machine()

if __name__ == "__main__":
    if "--check" not in sys.argv and "--fragments" not in sys.argv:
        shutil.rmtree(os.path.join(ASSETS, "models", "block", SUB), ignore_errors=True)
    sys.exit(C.main())
