#!/usr/bin/env python3
"""Every asset the Streetscape tab's utility boxes ship: pad-mount transformers, telecom
pedestals, the low-profile telecom enclosure and buried utility markers.

    python dev-env-utils/scripts/gen_streetscape_utility.py
    python dev-env-utils/scripts/gen_streetscape_utility.py --check
    python dev-env-utils/scripts/gen_streetscape_utility.py --fragments   # tab lines to paste

All of them are BlockUtilityBox, or BlockUtilityBoxLabelled for the ones carrying an ID number
decal, constructed by registry name with a UtilityBoxSpec. A unit up to two cells a side is drawn
whole by its root block's model; BlockUtilityBoxPart fills its other cells. Facing north, a unit
wider than a block grows toward the placing player's right (-x), a deeper one away from them
(+z), a taller one up, so a model's elements run from x = -16 to 16 and z = 0 to 32 at most --
inside the -16..32 an element may reach.

Everything a unit's Java needs is measured here from the same elements the model is written
from: the unit's box, and where the decal sits. --fragments prints them into the tab lines, so
the box and the model cannot disagree.

The inventory icon of a unit bigger than a block is a second model: the same elements scaled and
moved into the one cube, their UVs kept as they were, so a two-block transformer shows as a
transformer in its slot and not as a corner of one.

Utility green only (the user's choice, 2026-09-23), with a "(Rusted)" twin of every metal box.
"""
import copy
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = lc.REPO
ASSETS = os.path.join(REPO, "modules", "roads", "src", "main", "resources", "assets", "csm")
C = lc.Catalogue("gen_streetscape_utility.py", "streetscape/utility", "streetscape/utility",
                 assets=ASSETS)
TAB = "CsmTabStreetscape"
box = lc.box
post = lc.post

GREEN = (74, 94, 68)             # pad-mount green
PLASTIC = (96, 118, 88)          # the same green, moulded: a shade lighter and flatter
DARK = (38, 40, 38)
CONCRETE = (158, 155, 148)
WHITE = (232, 232, 226)
ORANGE = (226, 108, 32)
RED = (196, 40, 32)
YELLOW = (232, 196, 40)
RUST = (138, 74, 36)
RUST_DARK = (92, 50, 28)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def rusted(img, seed):
    """Rust worked into paint: blotches from a coarse value noise, heaviest toward the top where
    water stands, and a few runs down. The alpha is left exactly as it was."""
    size = img.width
    rng = random.Random(seed)
    lattice = max(4, size // 4)
    n = size // lattice + 2
    field = [[rng.random() for _ in range(n)] for _ in range(n)]

    def noise(x, y):
        fx, fy = x / float(lattice), y / float(lattice)
        ix, iy = int(fx), int(fy)
        tx, ty = fx - ix, fy - iy
        a = field[iy][ix] * (1 - tx) + field[iy][ix + 1] * tx
        b = field[iy + 1][ix] * (1 - tx) + field[iy + 1][ix + 1] * tx
        return a * (1 - ty) + b * ty

    out = img.copy()
    px = out.load()
    for y in range(img.height):
        for x in range(size):
            r, g, b, a = px[x, y]
            if not a:
                continue
            k = max(0.0, noise(x, y) - 0.45) * 1.8
            k = min(0.8, k)
            tint = RUST_DARK if (r + g + b) < 150 else RUST
            px[x, y] = lc.clamp(tuple(v * (1 - k) + t * k for v, t in zip((r, g, b), tint))) + (a,)
    for _ in range(max(2, size // 8)):
        x = rng.randrange(size)
        y = rng.randrange(img.height // 2)
        for step in range(rng.randrange(3, max(4, img.height // 3))):
            yy = y + step
            if yy < img.height and px[x, yy][3]:
                r, g, b, a = px[x, yy]
                px[x, yy] = lc.clamp(tuple(v * 0.55 + t * 0.45
                                           for v, t in zip((r, g, b), RUST))) + (a,)
    return out


def paint(colour, seed, size=32, grain=4):
    return lc.fill(colour, size, grain, seed)


def ribbed(colour, seed):
    """Moulded plastic with a horizontal rib every four texels."""
    img = lc.fill(colour, 32, 3, seed)
    for y in range(0, 32, 4):
        lc.rect(img, 0, y, 32, y + 1, lc.shade(colour, 0.72))
        lc.rect(img, 0, y + 1, 32, y + 2, lc.shade(colour, 1.12))
    return img


def concrete(seed):
    img = lc.fill(CONCRETE, 32, 7, seed)
    rng = random.Random(seed)
    px = img.load()
    for _ in range(40):
        x, y = rng.randrange(32), rng.randrange(32)
        px[x, y] = lc.shade(CONCRETE, rng.choice((0.8, 1.12))) + (255,)
    return img


def danger_label():
    """A DANGER sticker: black header band with the word, white body with lines of small print."""
    img = Image.new("RGBA", (32, 32), WHITE + (255,))
    lc.rect(img, 0, 0, 32, 9, DARK)
    lc.rect(img, 2, 2, 30, 7, RED)
    lc.draw_text_centred(img, "DANGER", 16, 2, WHITE)
    for y in range(12, 30, 3):
        lc.rect(img, 3, y, 29, y + 1, (80, 80, 80))
    return img


# The transformer's warning stickers (issue #252): a HIGH VOLTAGE sticker, and under it a
# smaller one reading IN CASE OF TROUBLE / CALL with the utility's black DWP square at its
# right. Both live on one square texture (a block texture must be square), HIGH VOLTAGE in its
# top half and the trouble sticker under it; the phone number's place after CALL is left blank,
# because the number is the player's to set and TileEntityUtilityBoxLabelRenderer draws it
# there. Every number the Java needs about that blank is measured from these constants.
STICKER_TEX = 128
HV_H = 64                       # HIGH VOLTAGE: texture rows 0..64, 2:1
TROUBLE_TOP, TROUBLE_H = 64, 53  # the trouble sticker: rows 64..117, about 2.4:1
TROUBLE_CAP = 12                # its legend's cap height, texels
TROUBLE_LINE1_TOP, TROUBLE_LINE2_TOP = 8, 33
TROUBLE_TEXT_X0, TROUBLE_TEXT_X1 = 5, 97
STICKER_YELLOW = (246, 192, 18)
STICKER_EDGE = (204, 150, 8)
STICKER_INK = (26, 22, 14)
TROUBLE_INK = (52, 36, 12)      # the trouble sticker's print is a browner black
FONT_DIR = os.path.join(REPO, "assets", "fonts")


def legend_mask(text, series, cap_h, squash=1.0):
    """A legend in one of the committed FHWA-style fonts as an alpha mask: cap_h texels from
    cap line to baseline, its width scaled by squash. Drawn large and reduced, so the result
    is the same on every run."""
    from PIL import ImageDraw, ImageFont
    big = 240
    font = ImageFont.truetype(os.path.join(FONT_DIR, "MicaOpenHighway-Series%s.ttf" % series),
                              big)
    top, bottom = font.getbbox("H")[1], font.getbbox("H")[3]
    l, _, r, _ = font.getbbox(text)
    img = Image.new("L", (r - l + 8, bottom + 8), 0)
    ImageDraw.Draw(img).text((4 - l, 0), text, font=font, fill=255)
    img = img.crop((4, top, 4 + r - l, bottom))
    k = cap_h / float(bottom - top)
    w = max(1, int(round(img.width * k * squash)))
    return img.resize((w, cap_h), Image.LANCZOS)


def stamp(img, mask, x, y, colour):
    img.paste(Image.new("RGBA", mask.size, colour + (255,)), (x, y), mask)


def trouble_layout():
    """Where the trouble sticker's legend sits, in its own texels: line 1's mask, CALL's mask,
    their common squash, and the blank the phone number goes in (x0, x1)."""
    width = TROUBLE_TEXT_X1 - TROUBLE_TEXT_X0
    line1 = legend_mask("IN CASE OF TROUBLE", "E", TROUBLE_CAP)
    squash = min(1.0, width / float(line1.width))
    line1 = legend_mask("IN CASE OF TROUBLE", "E", TROUBLE_CAP, squash)
    call = legend_mask("CALL", "E", TROUBLE_CAP, squash)
    blank_x0 = TROUBLE_TEXT_X0 + call.width + 4
    return line1, call, (blank_x0, TROUBLE_TEXT_X1)


def hv_stickers():
    img = Image.new("RGBA", (STICKER_TEX, STICKER_TEX), (0, 0, 0, 0))
    # HIGH VOLTAGE, filling its sticker
    lc.rect(img, 0, 0, STICKER_TEX, HV_H, STICKER_EDGE)
    lc.rect(img, 1, 1, STICKER_TEX - 1, HV_H - 1, STICKER_YELLOW)
    m = legend_mask("HIGH VOLTAGE", "EM", 50)
    m = m.resize((STICKER_TEX - 10, 50), Image.LANCZOS)
    stamp(img, m, 5, (HV_H - 50) // 2, STICKER_INK)
    # IN CASE OF TROUBLE / CALL ..., and the utility's square
    t0, t1 = TROUBLE_TOP, TROUBLE_TOP + TROUBLE_H
    lc.rect(img, 0, t0, STICKER_TEX, t1, STICKER_EDGE)
    lc.rect(img, 1, t0 + 1, STICKER_TEX - 1, t1 - 1, STICKER_YELLOW)
    line1, call, _ = trouble_layout()
    stamp(img, line1, TROUBLE_TEXT_X0, t0 + TROUBLE_LINE1_TOP, TROUBLE_INK)
    stamp(img, call, TROUBLE_TEXT_X0, t0 + TROUBLE_LINE2_TOP, TROUBLE_INK)
    sq0, sq1 = TROUBLE_TEXT_X1 + 3, STICKER_TEX - 4
    lc.rect(img, sq0, t0 + 5, sq1, t1 - 5, (18, 18, 16))
    # D, W and P stepping diagonally down the square, each kept two texels inside it
    for i, ch in enumerate("DWP"):
        g = legend_mask(ch, "EM", 11)
        lo, hi = sq0 + 2, sq1 - 2 - g.width
        stamp(img, g, int(round(lo + (hi - lo) * i / 2.0)), t0 + 8 + i * 12, STICKER_YELLOW)
    return img


def telecom_emblem(seed):
    """The round moulded emblem on a telecom enclosure's lid: a ring and a generic legend."""
    img = lc.fill(PLASTIC, 32, 3, seed)
    lc.disc(img, 16, 16, 13, lc.shade(PLASTIC, 1.15))
    lc.disc(img, 16, 16, 11.5, lc.shade(PLASTIC, 0.9))
    lc.disc(img, 16, 16, 10.5, PLASTIC)
    lc.draw_text_centred(img, "TELE", 16, 11, lc.shade(PLASTIC, 0.62))
    lc.draw_text_centred(img, "COM", 16, 17, lc.shade(PLASTIC, 0.62))
    return img


def marker_face(colour):
    """A buried utility marker's face: the utility's colour with a white WARNING panel near the
    top. Square, because a block texture must be (a taller one is read as animation frames and,
    with no .mcmeta, drawn as the missing texture); the face it covers is 14 times taller than
    it is wide, so the panel is drawn where it lands on that face, not where it looks right
    here."""
    img = Image.new("RGBA", (32, 32), colour + (255,))
    lc.rect(img, 2, 2, 30, 12, WHITE)
    lc.rect(img, 2, 2, 30, 4, colour)
    for y in range(6, 12, 2):
        lc.rect(img, 5, y, 27, y + 1, (60, 60, 60))
    return img


def register_textures():
    tex = {
        "paint": paint(GREEN, 11),
        "plastic": paint(PLASTIC, 12, grain=2),
        "ribbed": ribbed(PLASTIC, 13),
        "concrete": concrete(14),
        "dark": paint(DARK, 15, grain=3),
        "danger": danger_label(),
        "hv_stickers": hv_stickers(),
        "emblem": telecom_emblem(16),
        "marker_electric": marker_face(RED),
        "marker_gas": marker_face(YELLOW),
        "marker_telecom": marker_face(ORANGE),
    }
    tex["paint_rusted"] = rusted(tex["paint"], 21)
    for name, img in tex.items():
        C.texture(name)(lambda img=img: img)


register_textures()


# ------------------------------------------------------------------------------------------
# Geometry helpers
# ------------------------------------------------------------------------------------------
def slab(frm, to, tex, faces=("north", "south", "east", "west", "up", "down")):
    return box(frm, to, tex, faces=faces)


def decal(frm, to, tex, face):
    """A face carrying a whole picture (a sticker, an emblem): the full texture across it, where
    box() would fit the UVs to the face's size and show only a patch of it."""
    el = box(frm, to, tex, faces=(face,))
    el["faces"][face]["uv"] = [0, 0, 16, 16]
    return el


def hv_decals(cx, top, width, face_z):
    """The HIGH VOLTAGE sticker, width wide and half as tall, centred at cx with its top edge
    at top, and the trouble sticker centred under it at 0.7 of its width; both stuck on the
    north face at face_z. Returns the elements and where the phone number goes: the blank after
    CALL, as the renderer wants it (its centre, the face it sits on, a cap height and the width
    it may not outgrow), measured off the texture's own layout."""
    hv_h = width * HV_H / float(STICKER_TEX)
    tw = width * 0.7
    th = tw * TROUBLE_H / float(STICKER_TEX)
    gap = width * 0.06
    hx0, hx1 = cx - width / 2.0, cx + width / 2.0
    tx0, tx1 = cx - tw / 2.0, cx + tw / 2.0
    ty1 = top - hv_h - gap
    ty0 = ty1 - th
    z = face_z - 0.2  # where model_depth.py would move a decal on this face to anyway
    hv = decal([hx0, top - hv_h, z], [hx1, top, face_z], "hv_stickers", "north")
    hv["faces"]["north"]["uv"] = [0, 0, 16, 16.0 * HV_H / STICKER_TEX]
    tr = decal([tx0, ty0, z], [tx1, ty1, face_z], "hv_stickers", "north")
    tr["faces"]["north"]["uv"] = [0, 16.0 * TROUBLE_TOP / STICKER_TEX, 16,
                                  16.0 * (TROUBLE_TOP + TROUBLE_H) / STICKER_TEX]
    # Facing north the texture's left edge is at the face's +x end.
    bx0, bx1 = trouble_layout()[2]
    per_texel = tw / float(STICKER_TEX)
    phone = {
        "x": round(tx1 - (bx0 + bx1) / 2.0 * per_texel, 4),
        "y": round(ty1 - (TROUBLE_LINE2_TOP + TROUBLE_CAP / 2.0) * per_texel, 4),
        "z": round(z, 4),
        "height": round(TROUBLE_CAP * per_texel, 4),
        "width": round((bx1 - bx0) * per_texel, 4),
    }
    return [hv, tr], phone


def element_corners(el):
    """The eight corners of an element, turned by its rotation if it has one."""
    (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
    corners = [(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    rot = el.get("rotation")
    if not rot:
        return corners
    ox, oy, oz = rot["origin"]
    a = math.radians(rot["angle"])
    c, s = math.cos(a), math.sin(a)
    out = []
    for x, y, z in corners:
        x, y, z = x - ox, y - oy, z - oz
        if rot["axis"] == "y":
            x, z = x * c + z * s, -x * s + z * c
        elif rot["axis"] == "x":
            y, z = y * c - z * s, y * s + z * c
        else:
            x, y = x * c - y * s, x * s + y * c
        out.append((x + ox, y + oy, z + oz))
    return out


def bounds(elements):
    pts = [p for el in elements for p in element_corners(el)]
    return (min(p[0] for p in pts), min(p[1] for p in pts), min(p[2] for p in pts),
            max(p[0] for p in pts), max(p[1] for p in pts), max(p[2] for p in pts))


def inventory_elements(elements):
    """The unit's elements scaled and moved into one cube, centred, UVs unchanged."""
    x0, y0, z0, x1, y1, z1 = bounds(elements)
    k = min(1.0, 15.0 / max(x1 - x0, y1 - y0, z1 - z0))
    cx, cz = (x0 + x1) / 2.0, (z0 + z1) / 2.0
    out = []
    for el in elements:
        e = copy.deepcopy(el)

        def tr(p):
            return [round((p[0] - cx) * k + 8, 4), round((p[1] - y0) * k + 0.5, 4),
                    round((p[2] - cz) * k + 8, 4)]
        e["from"] = tr(el["from"])
        e["to"] = tr(el["to"])
        if "rotation" in e:
            e["rotation"]["origin"] = tr(el["rotation"]["origin"])
        out.append(e)
    return out


def model_for(textures, elements):
    return model_for_catalogue(C, textures, elements)


def model_for_catalogue(cat, textures, elements):
    """A model whose texture keys map to textures of catalogue cat; the first is the particle.
    Other Streetscape generators borrow this with their own catalogue."""
    t = {name: cat.T(tex) for name, tex in textures.items()}
    t["particle"] = t[list(textures)[0]]
    return lc.model(t, elements)


# ------------------------------------------------------------------------------------------
# The boxes
# ------------------------------------------------------------------------------------------
def pad(x0, z0, x1, z1):
    return slab([x0, 0, z0], [x1, 1, z1], "pad")


def low_profile(x0, x1, z0, z1, h, stickers, hinges=True):
    """A single-phase pad-mount transformer: a squat box whose top edge is rounded (a stepped
    chamfer), with the lid's hinges along the back and a padlock hasp at the front."""
    r = 1.0
    els = [
        pad(x0 - 1.2, z0 - 1.5, x1 + 1.2, z1 + 1.2),
        slab([x0, 1, z0], [x1, h - r, z1], "body", ("north", "south", "east", "west", "up")),
        slab([x0 + 0.35, h - r, z0 + 0.35], [x1 - 0.35, h - 0.35, z1 - 0.35], "body",
             ("north", "south", "east", "west", "up")),
        slab([x0 + r, h - 0.35, z0 + r], [x1 - r, h, z1 - r], "body", ("up", "north", "south",
                                                                       "east", "west")),
        # the seam where the lid meets the body, a hair proud of the front
        slab([x0 + 0.5, h - r - 1.2, z0 - 0.05], [x1 - 0.5, h - r - 1.0, z0], "dark",
             ("north",)),
        # padlock hasp
        slab([(x0 + x1) / 2 - 0.4, h - r - 2.6, z0 - 0.4], [(x0 + x1) / 2 + 0.4, h - r - 1.0,
                                                           z0], "dark"),
        # DANGER sticker, left of centre
        decal([x0 + 1.2, h - r - 5.6, z0 - 0.04], [x0 + 3.6, h - r - 2.4, z0], "danger",
              "north"),
    ]
    # HIGH VOLTAGE and the trouble sticker: stickers is (centre x, top edge, width)
    hv, phone = hv_decals(stickers[0], stickers[1], stickers[2], z0)
    els += hv
    if hinges:
        for hx in (x0 + 2.5, x1 - 4.5):
            els.append(slab([hx, h, z1 - 3], [hx + 2, h + 0.5, z1 - 1.5], "dark"))
    return els, phone


def three_phase():
    """The tall three-phase cabinet on its pad: two front doors, a latch handle, a skirt at the
    bottom, a lip at the roof, lifting lugs on the sides."""
    x0, x1, z0, z1, h = -13.0, 13.0, 2.0, 24.0, 27.0
    els = [
        pad(-15, 0, 15, 27),
        slab([x0 - 0.3, 1, z0 - 0.3], [x1 + 0.3, 4, z1 + 0.3], "body", ("north", "south", "east",
                                                                        "west", "up")),
        slab([x0, 4, z0], [x1, h, z1], "body", ("north", "south", "east", "west")),
        slab([x0 - 0.4, h, z0 - 0.4], [x1 + 0.4, h + 0.6, z1 + 0.4], "body"),
        # the doors' meeting seam and their outer seams
        slab([-0.15, 4.5, z0 - 0.05], [0.15, h - 0.5, z0], "dark", ("north",)),
        slab([x0 + 0.4, 4.5, z0 - 0.05], [x0 + 0.7, h - 0.5, z0], "dark", ("north",)),
        slab([x1 - 0.7, 4.5, z0 - 0.05], [x1 - 0.4, h - 0.5, z0], "dark", ("north",)),
        # latch handle on the right-hand door
        slab([-2.2, 12, z0 - 0.9], [-1.4, 17, z0], "dark"),
        slab([-2.2, 12, z0 - 0.9], [-0.6, 12.8, z0], "dark"),
        # DANGER sticker on the left-hand door
        decal([5.5, 18, z0 - 0.04], [8.5, 22, z0], "danger", "north"),
    ]
    # HIGH VOLTAGE and the trouble sticker under the DANGER sticker, on the same door
    hv, phone = hv_decals(7.0, 16.5, 10.0, z0)
    els += hv
    # lifting lugs, two a side
    for lz in (z0 + 3, z1 - 5):
        els.append(slab([x0 - 1.2, h - 3, lz], [x0, h - 1.5, lz + 2], "dark"))
        els.append(slab([x1, h - 3, lz], [x1 + 1.2, h - 1.5, lz + 2], "dark"))
    return els, phone


def square_pedestal(height):
    """A tall narrow steel pedestal with a pressed cap that overhangs a little."""
    return [
        slab([4.5, 0, 4.5], [11.5, height, 11.5], "body", ("north", "south", "east", "west",
                                                           "down")),
        slab([4.2, height, 4.2], [11.8, height + 0.8, 11.8], "body"),
        slab([5.2, height + 0.8, 5.2], [10.8, height + 1.2, 10.8], "body", ("up", "north",
                                                                           "south", "east",
                                                                           "west")),
        slab([6, height - 3, 4.45], [10, height - 2.8, 4.5], "dark", ("north",)),
    ]


def round_pedestal(height):
    """A round pedestal with a domed cap: octagon rings stepping in, as the mod draws round
    things."""
    return (post(8, 8, 3.4, 0, height, "body")
            + post(8, 8, 3.0, height, height + 0.9, "body", bottom=False)
            + post(8, 8, 2.2, height + 0.9, height + 1.6, "body", bottom=False)
            + post(8, 8, 1.2, height + 1.6, height + 2.0, "body", bottom=False))


def ribbed_pedestal():
    """The ribbed plastic telecom pedestal: a square body with a flat lid and a clamp bar down
    the front."""
    return [
        slab([4, 0, 4], [12, 17, 12], "ribbed",
             ("north", "south", "east", "west", "down", "up")),
        slab([4.3, 17, 4.3], [11.7, 18, 11.7], "lid"),
        slab([7.2, 2, 3.5], [8.8, 15, 4], "lid"),
        slab([6.6, 9, 3.4], [9.4, 10.5, 4], "lid"),
    ]


def low_telecom():
    """The low-profile moulded telecom enclosure: a ribbed box with a lid carrying its emblem."""
    return [
        slab([1, 0, 4.5], [15, 7.5, 12.5], "ribbed", ("north", "south", "east", "west",
                                                      "down", "up")),
        slab([1.3, 7.5, 4.8], [14.7, 8.5, 12.2], "lid", ("north", "south", "east", "west")),
        decal([1.3, 7.5, 4.8], [14.7, 8.5, 12.2], "emblem", "up"),
    ]


def marker_post(face_tex):
    """A buried utility marker: a flat flexible post, the utility's colour, a warning panel at
    the top of the face."""
    return [
        slab([7.3, 0, 7.6], [8.7, 20, 8.4], "post", ("south", "east", "west", "up", "down")),
        decal([7.3, 0, 7.6], [8.7, 20, 8.4], "face", "north"),
    ]


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def spec_java(els, cells, label, phone=None):
    x0, y0, z0, x1, y1, z1 = bounds(els)
    k = 1 / 16.0
    aabb = "new AxisAlignedBB(%s)" % ", ".join(
        "%.6f" % round(v * k, 6) for v in (x0, max(0.0, y0), z0, x1, y1, z1))
    lab = "null"
    if label:
        lab = "new UtilityBoxSpec.Label(%sf, %sf, %sf, %d, %sf, %s)" % (
            label["x"], label["y"], label["z"], label["lines"], label["height"],
            "true" if label.get("vertical") else "false")
    if phone:
        lab += ",\n        new UtilityBoxSpec.Phone(%sf, %sf, %sf, %sf, %sf)" % (
            phone["x"], phone["y"], phone["z"], phone["height"], phone["width"])
    return "new UtilityBoxSpec(%d, %d, %d,\n        %s,\n        %s)" % (
        cells[0], cells[1], cells[2], aabb, lab)


def add_box(registry, names, els, textures, cells, label=None, rusted_names=None,
            phone=None):
    variants = [(registry, textures, names)]
    if rusted_names:
        rt = {k: ("paint_rusted" if v == "paint" else v) for k, v in textures.items()}
        variants.append((registry + "_rusted", rt, rusted_names))
    big = max(bounds(els)[3] - bounds(els)[0], bounds(els)[4] - bounds(els)[1],
              bounds(els)[5] - bounds(els)[2]) > 16 or bounds(els)[0] < 0
    for reg, tex, nm in variants:
        models = {reg: model_for(tex, els)}
        state = lc.facing_state(C.M(reg))
        if big:
            models[reg + "_inventory"] = model_for(tex, inventory_elements(els))
            state["variants"]["inventory"] = [{"model": C.M(reg + "_inventory")}]
        cls = "BlockUtilityBoxLabelled" if label else "BlockUtilityBox"
        java = 'new %s("%s", %s)' % (cls, reg, spec_java(els, cells, label, phone))
        C.add(reg, java, nm, models, state, tab=TAB)


def names(en, de, es, sv):
    return (en, de, es, sv)


def rusted_names(n):
    en, de, es, sv = n
    return ("%s (Rusted)" % en if "(" not in en else en[:-1] + ", Rusted)",
            "%s (rostig)" % de if "(" not in de else de[:-1] + ", rostig)",
            "%s (Oxidado)" % es if "(" not in es else es[:-1] + ", Oxidado)",
            # Swedish agrees with the noun: a kopplingsskåp is neuter
            _sv_rusted(sv))


def _sv_rusted(sv):
    word = "Rostigt" if sv.startswith("Kopplingsskåp") else "Rostig"
    return "%s (%s)" % (sv, word) if "(" not in sv else sv[:-1] + ", %s)" % word


METAL = {"body": "paint", "pad": "concrete", "dark": "dark", "danger": "danger",
         "hv_stickers": "hv_stickers"}


def transformers():
    # The stickers sit under the ID number, clear of the DANGER sticker and the hasp.
    small, phone = low_profile(1.5, 14.5, 2.5, 14.5, 12, (8.0, 5.8, 5.0))
    n = names("Pad-Mount Transformer (Small)", "Pad-Mount-Transformator (Klein)",
              "Transformador de Pedestal (Pequeño)", "Markstation (Liten)")
    add_box("transformer_padmount_small", n, small, METAL, (1, 1, 1),
            {"x": 8.0, "y": 8.4, "z": 2.5, "lines": 2, "height": 1.1}, rusted_names(n),
            phone)

    medium, phone = low_profile(-11, 11, 2, 14.5, 13, (-2.5, 8.6, 7.0))
    n = names("Pad-Mount Transformer (Medium)", "Pad-Mount-Transformator (Mittel)",
              "Transformador de Pedestal (Mediano)", "Markstation (Mellan)")
    add_box("transformer_padmount_medium", n, medium, METAL, (2, 1, 1),
            {"x": 5.0, "y": 9.0, "z": 2.0, "lines": 2, "height": 1.4}, rusted_names(n),
            phone)

    large, phone = low_profile(-12, 12, 2, 24, 15, (-3.0, 10.4, 8.0))
    n = names("Pad-Mount Transformer (Large)", "Pad-Mount-Transformator (Groß)",
              "Transformador de Pedestal (Grande)", "Markstation (Stor)")
    add_box("transformer_padmount_large", n, large, METAL, (2, 2, 1),
            {"x": 5.0, "y": 10.6, "z": 2.0, "lines": 2, "height": 1.6}, rusted_names(n),
            phone)

    tall, phone = three_phase()
    n = names("Pad-Mount Transformer (Three-Phase)", "Pad-Mount-Transformator (Drehstrom)",
              "Transformador de Pedestal (Trifásico)", "Markstation (Trefas)")
    add_box("transformer_padmount_three_phase", n, tall, METAL, (2, 2, 2),
            {"x": -6.5, "y": 23.5, "z": 2.0, "lines": 1, "height": 2.2}, rusted_names(n),
            phone)


def pedestals():
    for key, els, cells, en, de, es, sv in (
            ("square_tall", square_pedestal(19), (1, 1, 2), "Square, Tall", "Eckig, Hoch",
             "Cuadrado, Alto", "Fyrkantigt, Högt"),
            ("square_short", square_pedestal(12), (1, 1, 1), "Square, Short", "Eckig, Niedrig",
             "Cuadrado, Bajo", "Fyrkantigt, Lågt"),
            ("round_tall", round_pedestal(16.5), (1, 1, 2), "Round, Tall", "Rund, Hoch",
             "Redondo, Alto", "Runt, Högt"),
            ("round_short", round_pedestal(10.5), (1, 1, 1), "Round, Short", "Rund, Niedrig",
             "Redondo, Bajo", "Runt, Lågt")):
        n = names("Utility Pedestal (%s)" % en, "Verteilersäule (%s)" % de,
                  "Pedestal de Servicios (%s)" % es, "Kopplingsskåp (%s)" % sv)
        add_box("utility_pedestal_" + key, n, els, {"body": "paint", "dark": "dark"}, cells,
                None, rusted_names(n))


def telecom():
    add_box("telecom_pedestal_ribbed",
            names("Telecom Pedestal (Ribbed)", "Telekom-Verteiler (Gerippt)",
                  "Pedestal de Telecomunicaciones (Acanalado)", "Telekopplingsskåp (Räfflat)"),
            ribbed_pedestal(), {"ribbed": "ribbed", "lid": "plastic"}, (1, 1, 2),
            {"x": 10.3, "y": 14.0, "z": 4.0, "lines": 1, "height": 1.2, "vertical": True})
    add_box("telecom_enclosure_low",
            names("Telecom Enclosure (Low Profile)", "Telekom-Gehäuse (Flach)",
                  "Caja de Telecomunicaciones (Baja)", "Telekapsling (Låg)"),
            low_telecom(), {"ribbed": "ribbed", "lid": "plastic", "emblem": "emblem"},
            (1, 1, 1))


def markers():
    for key, en, de, es, sv in (
            ("electric", "Electric", "Strom", "Eléctrico", "El"),
            ("gas", "Gas", "Gas", "Gas", "Gas"),
            ("telecom", "Telecom", "Telekom", "Telecomunicaciones", "Tele")):
        add_box("utility_marker_" + key,
                names("Buried Utility Marker (%s)" % en, "Leitungsmarkierung (%s)" % de,
                      "Marcador de Servicio Enterrado (%s)" % es, "Ledningsmarkering (%s)" % sv),
                marker_post("marker_" + key),
                {"post": "marker_" + key, "face": "marker_" + key}, (1, 1, 2))


def part():
    """The invisible cells of a big unit: an empty model with the paint as its particle, so a
    part being broken throws green chips."""
    C.add("utility_box_part", "BlockUtilityBoxPart.class",
          ("Utility Box (Part)", "Versorgungskasten (Teil)", "Caja de Servicios (Parte)",
           "Kopplingsskåp (Del)"),
          {"utility_box_part": {"parent": "block/block",
                                "textures": {"particle": C.T("paint")}}},
          {"forge_marker": 1, "defaults": {"model": C.M("utility_box_part")},
           "variants": {"normal": [{}], "inventory": [{}]}},
          tab="CsmTabRoadsHidden")


def gui_lang():
    """The ID number and trouble phone editor (UtilityBoxLabelGui)."""
    for key, en, de, es, sv in (
            ("title", "Utility Box Number", "Nummer des Versorgungskastens",
             "Número de la caja de servicios", "Kopplingsskåpets nummer"),
            ("number", "Number", "Nummer", "Número", "Nummer"),
            ("line", "Line %d", "Zeile %d", "Línea %d", "Rad %d"),
            ("automatic", "Leave the number empty for an automatic one",
             "Nummer leer lassen für eine automatische",
             "Deje el número vacío para uno automático",
             "Lämna numret tomt för ett automatiskt"),
            ("phone", "Trouble Phone Number", "Störungsrufnummer",
             "Teléfono de averías", "Felanmälningsnummer"),
            ("phone_default", "Leave the phone number empty for %s",
             "Rufnummer leer lassen für %s", "Deje el teléfono vacío para %s",
             "Lämna telefonnumret tomt för %s")):
        C.add_lang("gui.csm.utility_box." + key, (en, de, es, sv))


gui_lang()
transformers()
pedestals()
telecom()
markers()
part()

if __name__ == "__main__":
    sys.exit(C.main())
