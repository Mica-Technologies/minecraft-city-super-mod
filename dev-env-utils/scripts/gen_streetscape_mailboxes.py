#!/usr/bin/env python3
"""Every asset the Streetscape tab's mailboxes ship: the blue collection box, cluster box units
(CBU) in grey and bronze, curbside boxes on a post in three colours, and the apartment wall bank
in aluminium and brass.

    python dev-env-utils/scripts/gen_streetscape_mailboxes.py
    python dev-env-utils/scripts/gen_streetscape_mailboxes.py --check
    python dev-env-utils/scripts/gen_streetscape_mailboxes.py --fragments   # tab lines to paste

All of them are BlockMailbox (BlockMailboxCurbside for the box with a flag), BlockUtilityBox units
that settle onto sloped surfaces, so this borrows gen_streetscape_utility.py's measuring,
inventory-model and spec helpers.

A box with several compartments has one door per compartment drawn on a single front decal.
Which compartment a click opens is the door nearest the point looked at, so each door's rectangle
is written into the tab line in model pixels (facing north, y from the root cell's floor) from the
same layout the decal is drawn from; the numbers on the doors and the doors the Java picks cannot
disagree. The decal is seen from the north, so its left edge is +x: door 1 is at the viewer's
left, which is the highest x.

The collection box's legend is "UIA MAIL" (the user's choice, 2026-09-23); no emblem is drawn.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_streetscape_utility as gu  # noqa: E402

C = lc.Catalogue("gen_streetscape_mailboxes.py", "streetscape/mailboxes",
                 "streetscape/mailboxes", assets=gu.ASSETS)
TAB = "CsmTabStreetscape"
slab = gu.slab
decal = gu.decal

POSTAL_BLUE = (30, 62, 138)
WHITE = (240, 240, 236)
DARK = (36, 38, 40)
STEEL = (150, 152, 150)
WOOD = (112, 84, 54)
FLAG_RED = (196, 34, 30)
CBU_COLOURS = {"gray": (150, 154, 156), "bronze": (104, 80, 52)}
CURB_COLOURS = {"black": (40, 42, 44), "green": (38, 86, 52), "white": (226, 226, 220)}
BANK_COLOURS = {"aluminum": (182, 186, 188), "brass": (176, 142, 70)}

CANVAS = 128


# ------------------------------------------------------------------------------------------
# Door layouts: rectangles in fractions of a front panel, u from the viewer's left, v from top
# ------------------------------------------------------------------------------------------
def grid(cols, rows, u0, v0, u1, v1, gap):
    doors = []
    w = (u1 - u0 - gap * (cols - 1)) / cols
    h = (v1 - v0 - gap * (rows - 1)) / rows
    for r in range(rows):
        for c in range(cols):
            du = u0 + c * (w + gap)
            dv = v0 + r * (h + gap)
            doors.append((du, dv, du + w, dv + h))
    return doors


CBU_DOORS = (grid(4, 3, 0.03, 0.03, 0.97, 0.62, 0.02)
             + grid(2, 1, 0.03, 0.65, 0.97, 0.97, 0.02))
CBU_NAMES = ["Box %d" % (i + 1) for i in range(12)] + ["Parcel Locker 1", "Parcel Locker 2"]
CBU_LABELS = [str(i + 1) for i in range(12)] + ["P1", "P2"]
CBU_SLOTS = [9] * 12 + [27] * 2

BANK_DOORS = grid(2, 3, 0.04, 0.04, 0.96, 0.96, 0.04)
BANK_NAMES = ["Box %d" % (i + 1) for i in range(6)]
BANK_LABELS = [str(i + 1) for i in range(6)]


def panel_canvas(w_px, h_px):
    return CANVAS, int(round(CANVAS * h_px / float(w_px)))


def door_front(colour, w_px, h_px, doors, labels, seed, letter_slot=False):
    """A front panel with its doors: each a lighter face in a dark seam, a lock, and its number;
    drawn at the panel's aspect, then stretched to the square a texture must be."""
    cw, ch = panel_canvas(w_px, h_px)
    img = lc.fill(colour, cw, 3, seed)
    img = img.resize((cw, ch), Image.NEAREST)
    face = lc.shade(colour, 1.08)
    seam = lc.shade(colour, 0.55)
    ink = lc.shade(colour, 0.45) if sum(colour) > 360 else lc.shade(colour, 1.8)
    for (u0, v0, u1, v1), label in zip(doors, labels):
        x0, y0 = int(round(u0 * cw)), int(round(v0 * ch))
        x1, y1 = int(round(u1 * cw)), int(round(v1 * ch))
        lc.rect(img, x0, y0, x1, y1, seam)
        lc.rect(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, face)
        lc.draw_text(img, label, x0 + 4, y0 + 4, ink, 2)
        # The lock, at the door's right edge half way down.
        lc.disc(img, x1 - 6, (y0 + y1) / 2.0, 2.2, STEEL)
        lc.disc(img, x1 - 6, (y0 + y1) / 2.0, 0.9, DARK)
        if letter_slot:
            lc.rect(img, x0 + 6, y1 - 7, x1 - 12, y1 - 5, seam)
    return img.resize((CANVAS, CANVAS), Image.NEAREST)


def legend():
    """The collection box's front: UIA MAIL, white on blue. No emblem."""
    cw, ch = panel_canvas(8.8, 7.5)
    img = Image.new("RGBA", (cw, ch), POSTAL_BLUE + (255,))
    lc.draw_text_centred(img, "UIA", cw // 2, 18, WHITE, 5)
    lc.draw_text_centred(img, "MAIL", cw // 2, 18 + 34, WHITE, 5)
    lc.rect(img, 10, ch - 14, cw - 10, ch - 11, WHITE)
    return img.resize((CANVAS, CANVAS), Image.NEAREST)


def wood(seed):
    img = lc.fill(WOOD, 32, 4, seed)
    for x in range(1, 32, 5):
        lc.rect(img, x, 0, x + 1, 32, lc.shade(WOOD, 0.8))
    return img


def register_textures():
    tex = {
        "blue": gu.paint(POSTAL_BLUE, 101, grain=3),
        "blue_dark": gu.paint(lc.shade(POSTAL_BLUE, 0.75), 102, grain=3),
        "legend": legend(),
        "dark": gu.paint(DARK, 103, grain=2),
        "steel": gu.paint(STEEL, 104, grain=3),
        "wood": wood(105),
        "flag": gu.paint(FLAG_RED, 106, grain=3),
    }
    for key, rgb in CBU_COLOURS.items():
        tex["cbu_" + key] = gu.paint(rgb, 110 + len(key), grain=3)
        tex["cbu_front_" + key] = door_front(rgb, 23, 21, CBU_DOORS, CBU_LABELS, 120 + len(key))
    for key, rgb in CURB_COLOURS.items():
        tex["curb_" + key] = gu.paint(rgb, 130 + len(key), grain=3)
    for key, rgb in BANK_COLOURS.items():
        tex["bank_" + key] = gu.paint(rgb, 140 + len(key), grain=2)
        tex["bank_front_" + key] = door_front(rgb, 13.2, 12.7, BANK_DOORS, BANK_LABELS,
                                              150 + len(key), letter_slot=True)
    for name, img in tex.items():
        C.texture(name)(lambda img=img: img)


register_textures()


# ------------------------------------------------------------------------------------------
# Geometry
# ------------------------------------------------------------------------------------------
def door_rects(doors, x0, y0, x1, y1):
    """Door fractions on a north-facing panel x0..x1, y0..y1 to model-pixel rectangles
    {x0, y0, x1, y1}. u runs from the viewer's left, which facing north is +x."""
    w, h = x1 - x0, y1 - y0
    return [(round(x1 - u1 * w, 2), round(y1 - v1 * h, 2), round(x1 - u0 * w, 2),
             round(y1 - v0 * h, 2)) for u0, v0, u1, v1 in doors]


def collection_box():
    els = []
    for x in (3, 12):
        for z in (3, 12):
            els.append(slab([x, 0, z], [x + 1, 3, z + 1], "dark"))
    els.append(slab([3, 3, 3], [13, 20, 13], "body"))
    # The rounded top: an octagon along z, its lower half inside the body. Held a hair inside
    # the body's front and back so its end caps do not fight the body's faces.
    els += lc._octagon("z", 8, 20, 5, 3.05, 12.95, "body", True)
    els.append(slab([4.5, 14, 2.4], [11.5, 19, 3], "chute"))
    els.append(slab([6, 18.2, 1.8], [10, 18.8, 2.4], "steel"))
    els.append(decal([3.6, 5, 2.95], [12.4, 12.5, 3], "legend", "north"))
    return els, [(3, 3, 13, 20)]


CBU_FRONT = (-11.5, 8.5, 11.5, 29.5)


def cluster_box():
    x0, y0, x1, y1 = CBU_FRONT
    els = [
        slab([-6, 0, 3], [6, 0.6, 13], "dark"),
        slab([-4, 0.6, 5], [4, 8, 11], "body", ("north", "south", "east", "west")),
        slab([-12, 8, 3], [12, 30, 13], "body"),
        slab([-12.5, 30, 2.5], [12.5, 31, 13.5], "body"),
        decal([x0, y0, 2.95], [x1, y1, 3], "front", "north"),
    ]
    return els, door_rects(CBU_DOORS, x0, y0, x1, y1)


# The box sits on a post half a block taller than a real one's scale would give (the user found
# it too low, 2026-09-23), so a curbside box is a block and a half: two cells, placed as one.
POST_TOP = 17.5
R = POST_TOP - 9.5   # how far everything on the post is raised from the first drawing


def curbside(flag_up):
    els = [
        slab([6.5, 0, 6.5], [9.5, POST_TOP, 9.5], "post",
             ("north", "south", "east", "west", "down")),
        slab([5, 9.5 + R, 2], [11, 13 + R, 14], "body"),
    ]
    els += lc._octagon("z", 8, 13 + R, 3, 2.05, 13.95, "body", True)
    els.append(slab([5.2, 9.7 + R, 1.7], [10.8, 13 + R, 2], "body"))
    els.append(slab([7.4, 12.2 + R, 1.2], [8.6, 12.8 + R, 1.7], "steel"))
    return els


def flag(up):
    """The flag on the box's right as seen from the street (facing north that is -x), pivoting
    at the back: lying forward when down, standing up when raised."""
    pivot = slab([4.2, 11 + R, 10.6], [4.8, 12 + R, 11.6], "steel")
    if up:
        return [pivot, slab([4.3, 11.2 + R, 10.8], [4.8, 18 + R, 11.4], "flag"),
                slab([4.3, 16 + R, 11.4], [4.8, 18.5 + R, 13.9], "flag")]
    return [pivot, slab([4.3, 11.2 + R, 5], [4.8, 11.8 + R, 10.6], "flag"),
            slab([4.3, 10.3 + R, 3.6], [4.8, 12.7 + R, 6.1], "flag")]


BANK_FRONT = (1.4, 1.9, 14.6, 14.6)


def wall_bank():
    """A bank of six doors hung on the wall behind it (+z)."""
    x0, y0, x1, y1 = BANK_FRONT
    els = [
        slab([1, 1.5, 12.5], [15, 15, 16], "body"),
        decal([x0, y0, 12.45], [x1, y1, 12.5], "front", "north"),
    ]
    return els, door_rects(BANK_DOORS, x0, y0, x1, y1)


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def wrap(head, items, tail):
    """head, then items joined by ", ", then tail, broken onto continuation lines to stay
    within 100 columns as the tab file does."""
    lines, line = [], "        " + head
    for k, item in enumerate(items):
        piece = item + (", " if k < len(items) - 1 else tail)
        if len(line) + len(piece.rstrip()) > 100:
            lines.append(line.rstrip())
            line = "            "
        line += piece
    lines.append(line)
    return "\n".join(lines)


def java_mailbox(cls, reg, els, cells, doors, slots, names):
    return 'new %s("%s", %s,\n%s,\n%s,\n%s)' % (
        cls, reg, gu.spec_java(els, cells, None),
        wrap("new float[][]{", ["{%s}" % ", ".join("%sf" % v for v in r) for r in doors], "}"),
        wrap("new int[]{", [str(v) for v in slots], "}"),
        wrap("new String[]{", ['"%s"' % n for n in names], "}"))


def is_big(els):
    x0, y0, z0, x1, y1, z1 = gu.bounds(els)
    return max(x1 - x0, y1 - y0, z1 - z0) > 16 or x0 < 0


def add_simple(reg, names, els, textures, cells, doors, slots, door_names):
    models = {reg: gu.model_for_catalogue(C, textures, els)}
    state = lc.facing_state(C.M(reg))
    if is_big(els):
        models[reg + "_inventory"] = gu.model_for_catalogue(C, textures,
                                                            gu.inventory_elements(els))
        state["variants"]["inventory"] = [{"model": C.M(reg + "_inventory")}]
    C.add(reg, java_mailbox("BlockMailbox", reg, els, cells, doors, slots, door_names), names,
          models, state, tab=TAB)


def collection():
    els, doors = collection_box()
    add_simple("mailbox_collection_blue",
               ("Collection Mailbox (Blue)", "Briefkasten (Blau)", "Buzón de Correos (Azul)",
                "Brevlåda (Blå)"),
               els, {"body": "blue", "chute": "blue_dark", "dark": "dark", "steel": "steel",
                     "legend": "legend"},
               (1, 1, 2), doors, [27], ["Mailbox"])


def clusters():
    els, doors = cluster_box()
    for key, en, de, es, sv in (("gray", "Gray", "Grau", "Gris", "Grått"),
                                ("bronze", "Bronze", "Bronze", "Bronce", "Brons")):
        add_simple("mailbox_cluster_" + key,
                   ("Cluster Mailbox (%s)" % en, "Briefkastenanlage (%s)" % de,
                    "Buzón Comunitario (%s)" % es, "Postboxskåp (%s)" % sv),
                   els, {"body": "cbu_" + key, "dark": "dark", "front": "cbu_front_" + key},
                   (2, 1, 2), doors, CBU_SLOTS, CBU_NAMES)


def curbsides():
    body = curbside(False)
    for key, en, de, es, sv in (("black", "Black", "Schwarz", "Negro", "Svart"),
                                ("green", "Green", "Grün", "Verde", "Grön"),
                                ("white", "White", "Weiß", "Blanco", "Vit")):
        reg = "mailbox_curbside_" + key
        tex = {"body": "curb_" + key, "post": "wood", "steel": "steel"}
        ftex = {"flag": "flag", "steel": "steel"}
        models = {
            reg: gu.model_for_catalogue(C, tex, body),
            reg + "_flag_down": gu.model_for_catalogue(C, ftex, flag(False)),
            reg + "_flag_up": gu.model_for_catalogue(C, ftex, flag(True)),
            reg + "_item": gu.model_for_catalogue(C, dict(tex, flag="flag"),
                                                  gu.inventory_elements(body + flag(False))),
        }
        parts = []
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            rot = {"y": y} if y else {}
            parts.append({"when": {"facing": facing}, "apply": dict(model=C.M(reg), **rot)})
            for up in ("false", "true"):
                parts.append({"when": {"facing": facing, "flag": up},
                              "apply": dict(model=C.M(reg + ("_flag_up" if up == "true"
                                                             else "_flag_down")), **rot)})
        item = {"parent": "csm:block/%s/%s_item" % (C.model_dir, reg)}
        # The box's own door is the one compartment; the spec is measured with the flag down,
        # so the raised flag stands above the box's collision rather than making it taller.
        java = java_mailbox("BlockMailboxCurbside", reg, body + flag(False), (1, 1, 2),
                            [(5, 9.5 + R, 11, 16 + R)], [9], ["Mailbox"])
        C.add(reg, java, ("Curbside Mailbox (%s)" % en, "Briefkasten am Pfosten (%s)" % de,
                          "Buzón de Poste (%s)" % es, "Postlåda på Stolpe (%s)" % sv),
              models, {"multipart": parts}, item=item, tab=TAB)


def banks():
    els, doors = wall_bank()
    for key, en, de, es, sv in (("aluminum", "Aluminum", "Aluminium", "Aluminio", "Aluminium"),
                                ("brass", "Brass", "Messing", "Latón", "Mässing")):
        add_simple("mailbox_wall_bank_" + key,
                   ("Apartment Mailbox Bank (%s)" % en, "Wandbriefkastenanlage (%s)" % de,
                    "Buzones de Apartamento (%s)" % es, "Postboxar för Flerbostadshus (%s)" % sv),
                   els, {"body": "bank_" + key, "front": "bank_front_" + key},
                   (1, 1, 1), doors, [9] * 6, BANK_NAMES)


collection()
clusters()
curbsides()
banks()

if __name__ == "__main__":
    sys.exit(C.main())
