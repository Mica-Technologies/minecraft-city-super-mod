#!/usr/bin/env python3
"""The address plaques (GitHub #364): their glyph sheet, body sheet, item icons, blockstates and
models, and the Java table the baked model reads.

    py -3 dev-env-utils/scripts/gen_address_plaques.py
    py -3 dev-env-utils/scripts/gen_address_plaques.py --check       # writes nothing, exit 1 on drift
    py -3 dev-env-utils/scripts/gen_address_plaques.py --fragments   # lang and tab lines
    py -3 dev-env-utils/scripts/gen_address_plaques.py --sheet out.png   # contact sheet for review

A plaque's number and street line are not in any texture: the block's baked model
(``PlaqueBakedModel``) lays them out at run time from ``address_plaque_glyphs.png`` and the
metrics this script writes into ``PlaqueSheet.java``, and draws the plate from a nine-slice tile
on ``address_plaque_bodies.png``. Only the item icons carry a sample number.

Fonts. ``classic`` (Libre Baskerville Bold), ``didone`` (Abril Fatface) and ``modern`` (Poppins
SemiBold) are SIL Open Font License faces from the Google Fonts repository, fetched on first use
into the gitignored ``_font_cache/`` as ``gen_ads.py`` does (so a fresh clone's ``--check`` needs
the network once); no font file is committed or shipped. ``narrow`` is the repository's own Mica
Open Highway Series C (``assets/fonts``), the one for five digits on a small plate.

Output, in the Building module's tree (``modules/building/src/main/``):
    resources/assets/csm/textures/blocks/plaques/address_plaque_glyphs.png   white on clear
    resources/assets/csm/textures/blocks/plaques/address_plaque_bodies.png   the plate tiles
    resources/assets/csm/textures/blocks/plaques/address_plaque_bodies_e.png OptiFine emissive
    resources/assets/csm/textures/blocks/plaques/<block>_icon.png            item icons
    resources/assets/csm/blockstates/<block>.json
    resources/assets/csm/models/block/plaques/<block>_base.json / <block>_item.json
    java/.../buildingmaterials/plaques/PlaqueSheet.java
"""

import argparse
import filecmp
import io
import json
import math
import os
import random
import shutil
import sys
import tempfile
import urllib.parse
import urllib.request

from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODULE = os.path.join(REPO, "modules", "building", "src", "main")
ASSETS = os.path.join(MODULE, "resources", "assets", "csm")
TEX_DIR = os.path.join(ASSETS, "textures", "blocks", "plaques")
MODEL_DIR = os.path.join(ASSETS, "models", "block", "plaques")
STATE_DIR = os.path.join(ASSETS, "blockstates")
JAVA_DIR = os.path.join(MODULE, "java", "com", "micatechnologies", "minecraft", "csm",
                        "buildingmaterials", "plaques")
FONT_CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_font_cache")
_GF = "https://github.com/google/fonts/raw/main/"

LANGS = ("en_us", "es_es", "de_de", "sv_se")

# --------------------------------------------------------------------------------------------
# Fonts and the glyph sheet
# --------------------------------------------------------------------------------------------

# id -> (source, variation weight or None). A source starting "ofl/" is fetched from Google Fonts.
FONTS = [
    ("classic", "ofl/librebaskerville/LibreBaskerville[wght].ttf", 700),
    ("didone", "ofl/abrilfatface/AbrilFatface-Regular.ttf", None),
    ("modern", "ofl/poppins/Poppins-SemiBold.ttf", None),
    ("narrow", os.path.join(REPO, "assets", "fonts", "MicaOpenHighway-SeriesC.ttf"), None),
]
# SHARED with PlaqueSheet.CHARS (written below): every character a plaque can carry.
CHARS = (" 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.,-'/&#")
CAP = 34        # cap height on the sheet, px: the largest whose glyphs fit 1024 x 512
SUPER = 4       # drawn this many times larger and reduced
PAD = 2         # clear border round each glyph, px
GLYPH_SIZES = [(512, 512), (1024, 512), (1024, 1024)]


def font_file(source):
    if not source.startswith("ofl/"):
        return source
    path = os.path.join(FONT_CACHE, os.path.basename(source))
    if not os.path.exists(path):
        os.makedirs(FONT_CACHE, exist_ok=True)
        url = _GF + urllib.parse.quote(source)
        print("fetching " + url)
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
        with open(path, "wb") as out:
            out.write(data)
    return path


def _load(source, weight, size):
    f = ImageFont.truetype(font_file(source), size)
    if weight is not None:
        try:
            f.set_variation_by_axes([weight])
        except (OSError, AttributeError):
            pass
    return f


def _font_at_cap(source, weight, cap_px):
    """The font scaled so that a capital H is cap_px tall."""
    probe = _load(source, weight, 1000)
    box = probe.getbbox("H", anchor="ls")
    h = box[3] - box[1]
    return _load(source, weight, max(8, int(round(1000.0 * cap_px / h))))


def _glyph(font, ch):
    """One glyph, reduced: (image or None, left px, top px from baseline, advance in 1/16 px)."""
    adv = font.getlength(ch) / SUPER
    if ch == " ":
        return None, 0, 0, int(round(adv * 16))
    l, t, r, b = font.getbbox(ch, anchor="ls")
    # out to whole reduced pixels, so the metrics are exact
    l = (l // SUPER) * SUPER
    t = (t // SUPER) * SUPER
    r = -((-r) // SUPER) * SUPER
    b = -((-b) // SUPER) * SUPER
    big = Image.new("L", (r - l, b - t), 0)
    ImageDraw.Draw(big).text((-l, -t), ch, font=font, fill=255, anchor="ls")
    small = big.resize(((r - l) // SUPER, (b - t) // SUPER), Image.BOX)
    img = Image.new("L", (small.width + 2 * PAD, small.height + 2 * PAD), 0)
    img.paste(small, (PAD, PAD))
    return img, l // SUPER - PAD, t // SUPER - PAD, int(round(adv * 16))


_GLYPHS = None


def glyphs():
    """{font id: [(image, left, top, advance16) per CHARS]}"""
    global _GLYPHS
    if _GLYPHS is None:
        _GLYPHS = {}
        for fid, source, weight in FONTS:
            font = _font_at_cap(source, weight, CAP * SUPER)
            _GLYPHS[fid] = [_glyph(font, ch) for ch in CHARS]
    return _GLYPHS


def pack_glyphs():
    """Shelf-packs every font's glyphs: (sheet size, {font: [(x, y) per char]})."""
    all_glyphs = glyphs()
    for w, h in GLYPH_SIZES:
        places = {}
        x = y = 0
        row = 0
        ok = True
        for fid, _, _ in FONTS:
            out = []
            for img, _, _, _ in all_glyphs[fid]:
                if img is None:
                    out.append((0, 0))
                    continue
                if x + img.width > w:
                    x = 0
                    y += row
                    row = 0
                if y + img.height > h:
                    ok = False
                    break
                out.append((x, y))
                x += img.width
                row = max(row, img.height)
            if not ok:
                break
            places[fid] = out
        if ok:
            return (w, h), places
    raise SystemExit("the glyphs do not fit the largest glyph sheet")


def glyph_sheet():
    (w, h), places = pack_glyphs()
    sheet = Image.new("RGBA", (w, h), (255, 255, 255, 0))
    for fid, _, _ in FONTS:
        for (img, _, _, _), (x, y) in zip(glyphs()[fid], places[fid]):
            if img is not None:
                white = Image.new("RGBA", img.size, (255, 255, 255, 255))
                white.putalpha(img)
                sheet.paste(white, (x, y))
    return sheet


# --------------------------------------------------------------------------------------------
# The body sheet: one 64 px nine-slice tile per face finish
# --------------------------------------------------------------------------------------------

TILE = 64
BORDER = 12              # texels of a tile's border band
BODY_SHEET = 256
TILES = ["brass", "cast_iron", "enamel", "stone", "wood", "steel", "lamp_lit", "lamp_unlit",
         "lamp_housing"]
WHITE = (BODY_SHEET - 16, BODY_SHEET - 16, 8)    # x, y, size of the flat white swatch


def tile_origin(name):
    i = TILES.index(name)
    return (i % 3) * TILE, (i // 3) * TILE


def _rng(name):
    return random.Random(sum(ord(c) * (i + 1) for i, c in enumerate(name)))


def _lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def _shade(rgb, k):
    return tuple(max(0, min(255, int(round(c * k)))) for c in rgb)


def _field(size, top, bottom, rng, noise, streak=None):
    """A vertical gradient with noise; streak 'h' adds brushed lines, 'g' wood grain."""
    img = Image.new("RGB", (size, size))
    px = img.load()
    lines = [rng.uniform(-1, 1) for _ in range(size)]
    for y in range(size):
        base = _lerp(top, bottom, y / (size - 1))
        for x in range(size):
            n = rng.uniform(-noise, noise)
            if streak == "h":
                n += lines[y] * noise * 1.6
            elif streak == "g":
                n += noise * (0.5 + 0.5 * math.sin((y + 3 * math.sin(x / 9.0)) / 2.3))
            px[x, y] = tuple(max(0, min(255, int(c + n))) for c in base)
    return img


def _frame(img, outer, inner, width, light=None, dark=None):
    """Rings round the tile: an outer edge in 'outer', then a band in 'inner'."""
    d = ImageDraw.Draw(img)
    s = img.width
    for i in range(width):
        d.rectangle([i, i, s - 1 - i, s - 1 - i], outline=outer if i < 2 else inner)
    if light:
        d.line([(0, 0), (s - 1, 0)], fill=light)
        d.line([(0, 0), (0, s - 1)], fill=light)
    if dark:
        d.line([(0, s - 1), (s - 1, s - 1)], fill=dark)
        d.line([(s - 1, 0), (s - 1, s - 1)], fill=dark)


def _screw(d, cx, cy, r, rgb):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=_shade(rgb, 0.75))
    d.ellipse([cx - r + 1, cy - r + 1, cx + r - 1, cy + r - 1], fill=rgb)
    d.line([(cx - r + 1, cy), (cx + r - 1, cy)], fill=_shade(rgb, 0.55))


def tile(name):
    rng = _rng(name)
    s = TILE
    if name == "brass":
        img = _field(s, (226, 188, 98), (170, 128, 52), rng, 5, "h")
        _frame(img, (120, 88, 32), (205, 165, 80), 4, light=(250, 222, 150), dark=(96, 70, 26))
        d = ImageDraw.Draw(img)
        d.rectangle([BORDER - 3, BORDER - 3, s - BORDER + 2, s - BORDER + 2],
                    outline=(140, 104, 40))
        for cx, cy in ((6, 6), (s - 7, 6), (6, s - 7), (s - 7, s - 7)):
            _screw(d, cx, cy, 3, (200, 165, 85))
    elif name == "cast_iron":
        img = _field(s, (34, 34, 36), (22, 22, 24), rng, 4)
        d = ImageDraw.Draw(img)
        _frame(img, (16, 16, 18), (28, 28, 30), 3, light=(70, 70, 72), dark=(8, 8, 8))
        d.rectangle([5, 5, s - 6, s - 6], outline=(196, 156, 62))
        d.rectangle([6, 6, s - 7, s - 7], outline=(150, 116, 44))
        for cx, cy in ((9, 9), (s - 10, 9), (9, s - 10), (s - 10, s - 10)):
            d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(196, 156, 62))
    elif name == "enamel":
        img = _field(s, (250, 250, 247), (236, 237, 234), rng, 2)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, s - 1, s - 1], outline=(150, 156, 166))
        d.rectangle([1, 1, s - 2, s - 2], outline=(244, 245, 242))
        for i in range(4, 8):
            d.rectangle([i, i, s - 1 - i, s - 1 - i], outline=(28, 64, 146))
        d.line([(4, 4), (s - 5, 4)], fill=(60, 96, 178))
    elif name == "stone":
        img = _field(s, (206, 196, 174), (186, 176, 154), rng, 9)
        img = img.filter(ImageFilter.SMOOTH)
        d = ImageDraw.Draw(img)
        for i in range(0, 5):
            k = 1.12 - i * 0.03
            d.line([(i, i), (s - 1 - i, i)], fill=_shade((200, 190, 168), k))
            d.line([(i, i), (i, s - 1 - i)], fill=_shade((200, 190, 168), k - 0.04))
            d.line([(i, s - 1 - i), (s - 1 - i, s - 1 - i)], fill=_shade((200, 190, 168), 0.78))
            d.line([(s - 1 - i, i), (s - 1 - i, s - 1 - i)], fill=_shade((200, 190, 168), 0.84))
        d.rectangle([7, 7, s - 8, s - 8], outline=(160, 150, 130))
    elif name == "wood":
        img = _field(s, (38, 78, 52), (30, 66, 44), rng, 10, "g")
        d = ImageDraw.Draw(img)
        _frame(img, (20, 44, 28), (46, 92, 62), 5, light=(78, 128, 94), dark=(16, 36, 22))
        d.rectangle([6, 6, s - 7, s - 7], outline=(230, 214, 170))
    elif name == "steel":
        img = _field(s, (196, 200, 204), (162, 167, 172), rng, 6, "h")
        d = ImageDraw.Draw(img)
        _frame(img, (110, 114, 120), (186, 190, 195), 2, light=(232, 235, 238),
               dark=(96, 100, 106))
        for cx, cy in ((7, 7), (s - 8, 7), (7, s - 8), (s - 8, s - 8)):
            _screw(d, cx, cy, 3, (214, 218, 222))
    elif name in ("lamp_lit", "lamp_unlit", "lamp_housing"):
        housing = (58, 46, 36)
        if name == "lamp_lit":
            img = _field(s, (255, 247, 222), (252, 236, 196), rng, 2)
        elif name == "lamp_unlit":
            img = _field(s, (212, 208, 198), (196, 192, 182), rng, 2)
        else:
            img = _field(s, (70, 56, 44), (50, 40, 32), rng, 3)
        d = ImageDraw.Draw(img)
        for i in range(BORDER - 2):
            d.rectangle([i, i, s - 1 - i, s - 1 - i], outline=_shade(housing, 1.0 + (
                0.25 if i < 2 else 0) - (0.3 if i == BORDER - 3 else 0)))
    else:
        raise ValueError(name)
    return img.convert("RGBA")


def body_sheet():
    sheet = Image.new("RGBA", (BODY_SHEET, BODY_SHEET), (0, 0, 0, 0))
    for name in TILES:
        sheet.paste(tile(name), tile_origin(name))
    x, y, n = WHITE
    sheet.paste(Image.new("RGBA", (n, n), (255, 255, 255, 255)), (x, y))
    return sheet


def body_sheet_emissive():
    """Only the lit lens's centre, for OptiFine's emissive rendering."""
    sheet = Image.new("RGBA", (BODY_SHEET, BODY_SHEET), (0, 0, 0, 0))
    x, y = tile_origin("lamp_lit")
    lens = tile("lamp_lit").crop((BORDER, BORDER, TILE - BORDER, TILE - BORDER))
    sheet.paste(lens, (x + BORDER, y + BORDER))
    return sheet


# --------------------------------------------------------------------------------------------
# The blocks
# --------------------------------------------------------------------------------------------

# SHARED with PlaqueStyle.java (registry name, tile, default font and ink); the names in the
# four languages. A tile of None is a block with no plate.
INKS = {
    "black": (21, 21, 21), "white": (244, 241, 234), "gold": (226, 184, 74),
    "brass": (196, 154, 60), "bronze": (107, 74, 43), "blue": (31, 63, 143),
    "steel": (185, 190, 196), "red": (155, 34, 38), "green": (36, 81, 58),
    "slate": (74, 70, 64),
}
BLOCKS = [
    ("address_plaque_brass", "brass", "classic", "black", "minecraft:blocks/gold_block",
     ("Address Plaque (Brass)", "Placa de dirección (latón)", "Hausnummernschild (Messing)",
      "Husnummerskylt (mässing)")),
    ("address_plaque_cast_iron", "cast_iron", "classic", "gold", "minecraft:blocks/coal_block",
     ("Address Plaque (Cast Iron)", "Placa de dirección (hierro fundido)",
      "Hausnummernschild (Gusseisen)", "Husnummerskylt (gjutjärn)")),
    ("address_plaque_enamel", "enamel", "didone", "blue", "minecraft:blocks/quartz_block_side",
     ("Address Plaque (White Enamel)", "Placa de dirección (esmalte blanco)",
      "Hausnummernschild (weißes Email)", "Husnummerskylt (vit emalj)")),
    ("address_plaque_stone", "stone", "classic", "slate", "minecraft:blocks/sandstone_smooth",
     ("Address Tablet (Carved Stone)", "Lápida de dirección (piedra tallada)",
      "Hausnummerntafel (behauener Stein)", "Husnummertavla (huggen sten)")),
    ("address_plaque_wood", "wood", "classic", "white", "minecraft:blocks/planks_spruce",
     ("Address Plaque (Painted Wood)", "Placa de dirección (madera pintada)",
      "Hausnummernschild (bemaltes Holz)", "Husnummerskylt (målat trä)")),
    ("address_plaque_steel", "steel", "modern", "black", "minecraft:blocks/iron_block",
     ("Address Plaque (Brushed Steel)", "Placa de dirección (acero cepillado)",
      "Hausnummernschild (gebürsteter Stahl)", "Husnummerskylt (borstat stål)")),
    ("address_numerals", None, "classic", "brass", "minecraft:blocks/gold_block",
     ("House Numerals (Stand-Off)", "Números de casa (separados de la pared)",
      "Hausnummern (Einzelziffern mit Abstand)", "Husnummer (fristående siffror)")),
    ("address_transom_decal", None, "didone", "gold", "minecraft:blocks/glass",
     ("Transom Numbers (Gold Leaf)", "Números de montante (pan de oro)",
      "Oberlichtnummer (Blattgold)", "Överljusnummer (bladguld)")),
    ("address_lamp_box", "lamp_lit", "modern", "black", "minecraft:blocks/glowstone",
     ("Illuminated Address Box", "Caja de número iluminada", "Beleuchtete Hausnummer",
      "Belyst husnummerskylt")),
]
# The plates' depth in px, as PlaqueStyle; the item icon is drawn the same depth.
DEPTH = {"address_plaque_brass": 0.5, "address_plaque_cast_iron": 0.75,
         "address_plaque_enamel": 0.5, "address_plaque_stone": 2.0, "address_plaque_wood": 1.0,
         "address_plaque_steel": 0.5, "address_numerals": 0.0, "address_transom_decal": 0.0,
         "address_lamp_box": 3.0}
SAMPLE = "123"

FONT_IDS = [f[0] for f in FONTS]


def _text_image(text, fid, cap, rgb):
    """The sample text at a cap height (px), tinted, on clear."""
    (sw, sh), places = pack_glyphs()
    gl = glyphs()[fid]
    scale = cap / float(CAP)
    width = sum(gl[CHARS.index(c)][3] for c in text) / 16.0 * scale
    out = Image.new("RGBA", (int(width) + 4, int(cap * 1.6) + 4), (0, 0, 0, 0))
    pen = 2.0
    base = 2 + cap
    for c in text:
        img, left, top, adv = gl[CHARS.index(c)]
        if img is not None:
            g = img.resize((max(1, int(round(img.width * scale))),
                            max(1, int(round(img.height * scale)))), Image.LANCZOS)
            col = Image.new("RGBA", g.size, rgb + (255,))
            col.putalpha(g)
            out.alpha_composite(col, (int(round(pen + left * scale)),
                                      int(round(base + top * scale))))
        pen += adv / 16.0 * scale
    return out.crop(out.getbbox() or (0, 0, 1, 1))


def icon(name):
    """The item icon: a medium plate (14 x 7 px) with the sample number, 64 x 32."""
    _, tile_name, fid, ink, _, _ = next(b for b in BLOCKS if b[0] == name)
    w, h = 64, 32
    if tile_name is not None:
        t = tile(tile_name)
        img = t.resize((w, h), Image.LANCZOS)
        b = 6
        img.paste(t.crop((0, 0, BORDER, BORDER)).resize((b, b)), (0, 0))
        img.paste(t.crop((TILE - BORDER, 0, TILE, BORDER)).resize((b, b)), (w - b, 0))
        img.paste(t.crop((0, TILE - BORDER, BORDER, TILE)).resize((b, b)), (0, h - b))
        img.paste(t.crop((TILE - BORDER, TILE - BORDER, TILE, TILE)).resize((b, b)),
                  (w - b, h - b))
    else:
        img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    text = _text_image(SAMPLE, fid, 15, INKS[ink])
    if text.width > w - 14:
        text = text.resize((w - 14, int(text.height * (w - 14) / text.width)), Image.LANCZOS)
    if name in ("address_numerals", "address_transom_decal"):
        shadow = _text_image(SAMPLE, fid, 15, (20, 16, 10))
        if shadow.width > w - 14:
            shadow = shadow.resize(text.size, Image.LANCZOS)
        img.alpha_composite(shadow, ((w - text.width) // 2 + 1, (h - text.height) // 2 + 1))
    img.alpha_composite(text, ((w - text.width) // 2, (h - text.height) // 2))
    if tile_name is None:
        # cutout: every pixel opaque or clear
        a = img.getchannel("A").point(lambda v: 255 if v >= 128 else 0)
        img.putalpha(a)
    return img


def blockstate(name):
    return {
        "forge_marker": 1,
        "defaults": {"model": "csm:plaques/%s_base" % name},
        "variants": {
            "facing": {"north": {}, "east": {}, "south": {}, "west": {}},
            "inventory": [{"model": "csm:plaques/%s_item" % name}],
        },
    }


def base_model(name):
    particle = next(b for b in BLOCKS if b[0] == name)[4]
    return {"textures": {"particle": particle,
                         "glyphs": "csm:blocks/plaques/address_plaque_glyphs",
                         "bodies": "csm:blocks/plaques/address_plaque_bodies"}}


def item_model(name):
    depth = max(DEPTH[name], 0.5) if next(b for b in BLOCKS if b[0] == name)[1] else 0
    tex = "csm:blocks/plaques/%s_icon" % name
    x0, x1, y0, y1 = 1.0, 15.0, 4.5, 11.5
    if depth == 0:
        el = {"from": [x0, y0, 0.5], "to": [x1, y1, 0.5],
              "faces": {"south": {"texture": "#icon", "uv": [0, 0, 16, 8]},
                        "north": {"texture": "#icon", "uv": [16, 0, 0, 8]}}}
    else:
        el = {"from": [x0, y0, 0], "to": [x1, y1, depth],
              "faces": {"south": {"texture": "#icon", "uv": [0, 0, 16, 8]},
                        "north": {"texture": "#icon", "uv": [0, 0, 0.25, 8]},
                        "east": {"texture": "#icon", "uv": [15.75, 0, 16, 8]},
                        "west": {"texture": "#icon", "uv": [0, 0, 0.25, 8]},
                        "up": {"texture": "#icon", "uv": [0, 0, 16, 0.25]},
                        "down": {"texture": "#icon", "uv": [0, 7.75, 16, 8]}}}
    return {"textures": {"icon": tex, "particle": tex}, "elements": [el]}


# --------------------------------------------------------------------------------------------
# PlaqueSheet.java
# --------------------------------------------------------------------------------------------


def java_sheet():
    (w, h), places = pack_glyphs()
    out = []
    out.append("package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;")
    out.append("")
    out.append("/**")
    out.append(" * The address plaques' two sheets: where each glyph of each font is on")
    out.append(" * {@code address_plaque_glyphs.png} and how it is set, and where each face finish's")
    out.append(" * nine-slice tile is on {@code address_plaque_bodies.png} (both under")
    out.append(" * {@code textures/blocks/plaques/}).")
    out.append(" *")
    out.append(" * <p>GENERATED by {@code dev-env-utils/scripts/gen_address_plaques.py} with the sheets; do")
    out.append(" * not edit by hand (its {@code --check} fails if the Java and the sheets disagree).</p>")
    out.append(" *")
    out.append(" * @since 2026.11")
    out.append(" */")
    out.append("public final class PlaqueSheet {")
    out.append("")
    out.append("  /** The glyph sheet's sprite. */")
    out.append("  public static final String GLYPH_SPRITE = \"csm:blocks/plaques/address_plaque_glyphs\";")
    out.append("  /** The body sheet's sprite. */")
    out.append("  public static final String BODY_SPRITE = \"csm:blocks/plaques/address_plaque_bodies\";")
    out.append("  /** The glyph sheet's size, px. */")
    out.append("  public static final int GLYPH_W = %d;" % w)
    out.append("  public static final int GLYPH_H = %d;" % h)
    out.append("  /** The glyphs' cap height on the sheet, px: the unit every glyph number is in. */")
    out.append("  public static final int CAP = %d;" % CAP)
    out.append("  /** The body sheet's size, a tile's size and its border band, px. */")
    out.append("  public static final int BODY = %d;" % BODY_SHEET)
    out.append("  public static final int TILE = %d;" % TILE)
    out.append("  public static final int TILE_BORDER = %d;" % BORDER)
    out.append("  /** The flat white swatch on the body sheet: x, y, size. */")
    out.append("  public static final int[] WHITE = {%d, %d, %d};" % WHITE)
    out.append("")
    out.append("  /** Every character a plaque can carry, in the order of the glyph tables. */")
    out.append("  public static final String CHARS = \"%s\";" % CHARS.replace("\\", "\\\\")
               .replace("\"", "\\\""))
    out.append("")
    out.append("  /** The fonts, in the order of {@link #GLYPHS}. */")
    out.append("  public static final String[] FONTS = {%s};"
               % ", ".join("\"%s\"" % f for f in FONT_IDS))
    out.append("")
    out.append("  /** Each tile's x and y on the body sheet, by name. */")
    out.append("  public static final String[] TILE_NAMES = {%s};"
               % ", ".join("\"%s\"" % t for t in TILES))
    out.append("  public static final int[][] TILE_XY = {")
    for t in TILES:
        x, y = tile_origin(t)
        out.append("      {%d, %d}, // %s" % (x, y, t))
    out.append("  };")
    out.append("")
    out.append("  /**")
    out.append("   * Per font, per character of {@link #CHARS}, seven numbers: its image's x and y on the")
    out.append("   * glyph sheet, its width and height (0 for none), its left edge from the pen and its top")
    out.append("   * from the baseline (px, negative above it), and its advance in sixteenths of a px.")
    out.append("   */")
    out.append("  static final int[][] GLYPHS = {")
    for fid in FONT_IDS:
        out.append("      { // %s" % fid)
        for ch, (img, left, top, adv), (x, y) in zip(CHARS, glyphs()[fid], places[fid]):
            if img is None:
                vals = (0, 0, 0, 0, 0, 0, adv)
            else:
                vals = (x, y, img.width, img.height, left, top, adv)
            out.append("          %d, %d, %d, %d, %d, %d, %d, // %s"
                       % (vals + (repr(ch),)))
        out.append("      },")
    out.append("  };")
    out.append("")
    out.append("  private PlaqueSheet() {")
    out.append("  }")
    out.append("}")
    return "\n".join(out) + "\n"


# --------------------------------------------------------------------------------------------
# Writing
# --------------------------------------------------------------------------------------------


def _write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def write_all(roots):
    written = []
    for d in roots.values():
        os.makedirs(d, exist_ok=True)
    textures = {"address_plaque_glyphs": glyph_sheet(),
                "address_plaque_bodies": body_sheet(),
                "address_plaque_bodies_e": body_sheet_emissive()}
    for name, *_ in BLOCKS:
        textures[name + "_icon"] = icon(name)
    for name, img in sorted(textures.items()):
        img.save(os.path.join(roots["tex"], name + ".png"))
        written.append(("tex", name + ".png"))
    for name, *_ in BLOCKS:
        _write_json(os.path.join(roots["state"], name + ".json"), blockstate(name))
        written.append(("state", name + ".json"))
        _write_json(os.path.join(roots["model"], name + "_base.json"), base_model(name))
        _write_json(os.path.join(roots["model"], name + "_item.json"), item_model(name))
        written += [("model", name + "_base.json"), ("model", name + "_item.json")]
    with io.open(os.path.join(roots["java"], "PlaqueSheet.java"), "w", encoding="utf-8",
                 newline="\n") as fh:
        fh.write(java_sheet())
    written.append(("java", "PlaqueSheet.java"))
    return written


def check(roots):
    tmp = tempfile.mkdtemp(prefix="csm_plaques_")
    try:
        tmp_roots = {k: os.path.join(tmp, k) for k in roots}
        written = write_all(tmp_roots)
        drifted = []
        for kind, filename in written:
            here = os.path.join(roots[kind], filename)
            there = os.path.join(tmp_roots[kind], filename)
            same = (os.path.exists(here) and filecmp.cmp(here, there, shallow=False)
                    if filename.endswith(".png")
                    else os.path.exists(here) and csm_layout.same_generated_text(there, here))
            if not same:
                drifted.append(os.path.relpath(here, REPO))
        if drifted:
            print("DRIFT: %d file(s) differ from the generator:" % len(drifted))
            for path in drifted:
                print("  " + path)
            return 1
        print("%d generated address plaque files are up to date" % len(written))
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def fragments():
    lines = ["# lang lines, one per language file under the Building module's assets/csm/lang/", ""]
    for i, lang in enumerate(LANGS):
        lines.append("## " + lang)
        for name, _, _, _, _, names in BLOCKS:
            lines.append("tile.%s.name=%s" % (name, names[i]))
        lines.append("")
    lines.append("# tab registration, in CsmTabBuildingMaterials")
    for name, _, _, _, _, names in BLOCKS:
        lines.append('    initTabBlock(new BlockAddressPlaque("%s")); // %s' % (name, names[0]))
    return "\n".join(lines)


def contact_sheet(path):
    """Every icon and both sheets on one image, for review."""
    icons = [icon(b[0]).resize((256, 128), Image.NEAREST) for b in BLOCKS]
    gs = glyph_sheet()
    bs = body_sheet().resize((512, 512), Image.NEAREST)
    w = max(gs.width, 3 * 260, 512)
    h = 3 * 132 + gs.height + 512 + 20
    out = Image.new("RGBA", (w, h), (120, 120, 120, 255))
    for i, im in enumerate(icons):
        out.alpha_composite(im, ((i % 3) * 260, (i // 3) * 132))
    dark = Image.new("RGBA", gs.size, (40, 40, 40, 255))
    dark.alpha_composite(gs)
    out.alpha_composite(dark, (0, 3 * 132 + 10))
    out.alpha_composite(bs, (0, 3 * 132 + gs.height + 20))
    out.save(path)
    print("wrote " + path)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true",
                    help="write nothing; exit 1 if the tree differs from what would be generated")
    ap.add_argument("--fragments", action="store_true",
                    help="print the lang and tab-registration lines and exit")
    ap.add_argument("--sheet", metavar="PNG", help="write a contact sheet for review and exit")
    args = ap.parse_args()
    if args.fragments:
        print(fragments())
        return 0
    if args.sheet:
        contact_sheet(args.sheet)
        return 0
    roots = {"tex": TEX_DIR, "model": MODEL_DIR, "state": STATE_DIR, "java": JAVA_DIR}
    if args.check:
        return check(roots)
    written = write_all(roots)
    print("Wrote %d address plaque files" % len(written))
    return 0


if __name__ == "__main__":
    sys.exit(main())
