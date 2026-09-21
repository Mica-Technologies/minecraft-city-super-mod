#!/usr/bin/env python3
"""The parody advertisements the Signage & Advertising boards show.

Every ad is an invented brand -- no real company, product, logo or person, and no near miss of a
real trademark -- drawn by this script in four shapes, because a 2 x 3 kiosk and a 40 x 8
billboard cannot show the same picture well:

    portrait   2:3    512 x 768    kiosks
    square     1:1    512 x 512    fallback for boards near square
    poster     2:1   1024 x 512    wall poster boards
    bulletin   7:2   1008 x 288    billboards

Each shape is laid out on its own (headline, illustration, brand, call to action rearranged)
rather than cropped from one master, so nothing important falls off an edge. An ad is an SVG:
the text is measured here with the same font files resvg renders it with, and the illustrations
are hand-drawn SVG in ``ad_art.py``.

    python dev-env-utils/scripts/gen_ads.py
    python dev-env-utils/scripts/gen_ads.py --check           # writes nothing, exit 1 on drift
    python dev-env-utils/scripts/gen_ads.py --sheet out.png   # contact sheet for review
    python dev-env-utils/scripts/gen_ads.py --only cube_burger --sheet out.png

The fonts are SIL Open Font License faces from the Google Fonts repository, fetched on first use
into the gitignored ``_font_cache/``. They are only used to render the images; no font file is
committed or shipped.

Output, in the signage module's tree:
    assets/csm/textures/ads/parody/<id>_<shape>.png
    assets/csm/ads/parody.json      the index the mod reads: id, brand, category, shapes, colour
"""

import argparse
import io
import json
import os
import sys
import tempfile
import urllib.request
from xml.sax.saxutils import escape

import resvg_py
from PIL import Image, ImageDraw, ImageFont

import ad_art

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "modules", "signage", "src", "main", "resources", "assets", "csm")
TEX_DIR = os.path.join(ASSETS, "textures", "ads", "parody")
INDEX_PATH = os.path.join(ASSETS, "ads", "parody.json")
FONT_CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_font_cache")

SHAPES = {
    "portrait": (512, 768),
    "square": (512, 512),
    "poster": (1024, 512),
    "bulletin": (1008, 288),
}
SHAPE_ORDER = ["portrait", "square", "poster", "bulletin"]

# --------------------------------------------------------------------------------------------
# Fonts
# --------------------------------------------------------------------------------------------

_GF = "https://github.com/google/fonts/raw/main/"

# key -> (path in the Google Fonts repository, family name resvg knows it by, weight,
#         line height as a multiple of the size, whether it is set in capitals)
FONTS = {
    "anton": ("ofl/anton/Anton-Regular.ttf", "Anton", 400, 1.02, True),
    "bebas": ("ofl/bebasneue/BebasNeue-Regular.ttf", "Bebas Neue", 400, 0.92, True),
    "archivo": ("ofl/archivoblack/ArchivoBlack-Regular.ttf", "Archivo Black", 400, 1.05, False),
    "bangers": ("ofl/bangers/Bangers-Regular.ttf", "Bangers", 400, 1.0, True),
    "righteous": ("ofl/righteous/Righteous-Regular.ttf", "Righteous", 400, 1.08, False),
    "abril": ("ofl/abrilfatface/AbrilFatface-Regular.ttf", "Abril Fatface", 400, 1.08, False),
    "bungee": ("ofl/bungee/Bungee-Regular.ttf", "Bungee", 400, 1.0, True),
    "lobster": ("ofl/lobster/Lobster-Regular.ttf", "Lobster", 400, 1.1, False),
    "pacifico": ("ofl/pacifico/Pacifico-Regular.ttf", "Pacifico", 400, 1.25, False),
    "marker": ("apache/permanentmarker/PermanentMarker-Regular.ttf", "Permanent Marker", 400,
               1.1, False),
    "poppins": ("ofl/poppins/Poppins-Regular.ttf", "Poppins", 400, 1.2, False),
    "poppins-semi": ("ofl/poppins/Poppins-SemiBold.ttf", "Poppins", 600, 1.2, False),
    "poppins-bold": ("ofl/poppins/Poppins-Bold.ttf", "Poppins", 700, 1.15, False),
    "poppins-black": ("ofl/poppins/Poppins-Black.ttf", "Poppins", 900, 1.08, False),
}


def font_file(key):
    path = os.path.join(FONT_CACHE, os.path.basename(FONTS[key][0]))
    if not os.path.exists(path):
        os.makedirs(FONT_CACHE, exist_ok=True)
        url = _GF + FONTS[key][0]
        print("fetching " + url)
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
        with open(path, "wb") as out:
            out.write(data)
    return path


_pil_fonts = {}


def pil_font(key, size):
    size = max(1, int(round(size)))
    if (key, size) not in _pil_fonts:
        _pil_fonts[(key, size)] = ImageFont.truetype(font_file(key), size)
    return _pil_fonts[(key, size)]


def text_width(key, size, text):
    return pil_font(key, size).getlength(text)


def ink_extent(key, size, text):
    """Top and bottom of the text's ink relative to its baseline (top negative)."""
    box = pil_font(key, size).getbbox(text, anchor="ls")
    return box[1], box[3]


# --------------------------------------------------------------------------------------------
# Text layout
# --------------------------------------------------------------------------------------------

def _strip(text):
    return text.replace("*", "")


def wrap(key, size, text, width, max_lines):
    """Greedy word wrap. ``|`` in the text forces a break. Returns the lines, or None if the
    text does not fit in ``max_lines`` at this size."""
    lines = []
    for para in text.split("|"):
        words = para.split()
        line = ""
        for word in words:
            if text_width(key, size, _strip(word)) > width:
                return None
            trial = (line + " " + word).strip()
            if text_width(key, size, _strip(trial)) <= width:
                line = trial
            else:
                lines.append(line)
                line = word
        lines.append(line)
    if len(lines) > max_lines:
        return None
    return lines


def fit(key, text, box_w, box_h, max_lines, max_size, min_size=6):
    """The largest size at which ``text`` wraps into the box. Returns (size, lines)."""
    lh = FONTS[key][3]
    size = max_size
    while size >= min_size:
        lines = wrap(key, size, text, box_w, max_lines)
        if lines:
            top, _ = ink_extent(key, size, _strip(lines[0]))
            _, bottom = ink_extent(key, size, _strip(lines[-1]))
            height = (len(lines) - 1) * size * lh + (bottom - top)
            if height <= box_h:
                return size, lines
        size -= 1 if size < 40 else 2
    lines = wrap(key, min_size, text, box_w, 99) or [text]
    return min_size, lines


def text_block(key, text, box, colour, accent, align="middle", valign="middle", max_lines=3,
               max_size=400, min_size=6, upper=None, spacing=0.0, shadow=False):
    """SVG for ``text`` fitted into ``box`` = (x, y, w, h). Words wrapped in ``*`` take the
    accent colour."""
    x, y, w, h = box
    if upper is None:
        upper = FONTS[key][4]
    if upper:
        text = text.upper()
    size, lines = fit(key, text, w, h, max_lines, max_size, min_size)
    lh = FONTS[key][3]
    top, _ = ink_extent(key, size, _strip(lines[0]))
    _, bottom = ink_extent(key, size, _strip(lines[-1]))
    height = (len(lines) - 1) * size * lh + (bottom - top)
    if valign == "top":
        first = y - top
    elif valign == "bottom":
        first = y + h - height - top
    else:
        first = y + (h - height) / 2 - top
    anchor = {"start": x, "middle": x + w / 2, "end": x + w}[align]
    family, weight = FONTS[key][1], FONTS[key][2]
    out = []
    for i, line in enumerate(lines):
        parts = line.split("*")
        spans = []
        for j, part in enumerate(parts):
            if not part:
                continue
            fill = accent if j % 2 else colour
            spans.append('<tspan fill="%s">%s</tspan>' % (fill, escape(part)))
        out.append(
            '<text x="%.1f" y="%.1f" font-family="%s" font-weight="%d" font-size="%d" '
            'text-anchor="%s"%s%s>%s</text>'
            % (anchor, first + i * size * lh, family, weight, size, align,
               ' letter-spacing="%.2f"' % (spacing * size) if spacing else "",
               ' filter="url(#shadow)"' if shadow else "", "".join(spans)))
    return "\n".join(out), size


# --------------------------------------------------------------------------------------------
# Backgrounds and decoration
# --------------------------------------------------------------------------------------------

def _rays(cx, cy, radius, count, colour, opacity):
    import math
    out = []
    for i in range(count):
        a0 = 2 * math.pi * i / count
        a1 = a0 + math.pi / count
        out.append("M%.1f %.1f L%.1f %.1f L%.1f %.1f Z" % (
            cx, cy, cx + radius * math.cos(a0), cy + radius * math.sin(a0),
            cx + radius * math.cos(a1), cy + radius * math.sin(a1)))
    return '<path d="%s" fill="%s" opacity="%.2f"/>' % (" ".join(out), colour, opacity)


def background(ad, w, h, art_box):
    p = ad["palette"]
    out = []
    if p.get("bg2") and ad.get("deco") != "diag":
        out.append('<defs><linearGradient id="bg" x1="0" y1="0" x2="%s" y2="1">'
                   '<stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/>'
                   '</linearGradient></defs>' % ("1" if w > h else "0", p["bg"], p["bg2"]))
        out.append('<rect width="%d" height="%d" fill="url(#bg)"/>' % (w, h))
    else:
        out.append('<rect width="%d" height="%d" fill="%s"/>' % (w, h, p["bg"]))
    deco = ad.get("deco")
    ax, ay, aw, ah = art_box
    cx, cy = ax + aw / 2, ay + ah / 2
    tone = p.get("deco", p["accent"])
    if deco == "rays":
        out.append(_rays(cx, cy, max(w, h) * 1.5, 18, tone, 0.18))
    elif deco == "circle":
        out.append('<circle cx="%.1f" cy="%.1f" r="%.1f" fill="%s"/>'
                   % (cx, cy, min(aw, ah) * 0.52, tone))
    elif deco == "stripes":
        step = max(w, h) / 14
        paths = []
        i = -h
        while i < w + h:
            paths.append("M%.1f 0 L%.1f 0 L%.1f %d L%.1f %d Z" % (
                i, i + step / 2, i + step / 2 - h, h, i - h, h))
            i += step
        out.append('<path d="%s" fill="%s" opacity="0.10"/>' % (" ".join(paths), tone))
    elif deco == "diag":
        if w >= h:
            pts = "%.1f 0 %d 0 %d %d %.1f %d" % (w * 0.58, w, w, h, w * 0.46, h)
            if ad.get("art_side", "right") == "left":
                pts = "0 0 %.1f 0 %.1f %d 0 %d" % (w * 0.46, w * 0.34, h, h)
        else:
            pts = "0 %.1f %d %.1f %d %d 0 %d" % (h * 0.42, w, h * 0.32, w, h, h)
        out.append('<polygon points="%s" fill="%s"/>' % (pts, p.get("bg2", tone)))
    elif deco == "dots":
        r = max(w, h) / 90
        dots = []
        gx = 0
        while gx < w + r * 6:
            gy = 0
            while gy < h + r * 6:
                dots.append('<circle cx="%.1f" cy="%.1f" r="%.1f"/>' % (gx, gy, r))
                gy += r * 5
            gx += r * 5
        out.append('<g fill="%s" opacity="0.12">%s</g>' % (tone, "".join(dots)))
    elif deco == "grid":
        step = max(w, h) / 24
        lines = []
        gx = 0
        while gx <= w:
            lines.append("M%.1f 0 V%d" % (gx, h))
            gx += step
        gy = 0
        while gy <= h:
            lines.append("M0 %.1f H%d" % (gy, w))
            gy += step
        out.append('<path d="%s" stroke="%s" stroke-width="%.1f" opacity="0.18" fill="none"/>'
                   % (" ".join(lines), tone, max(1.0, step / 30)))
    return "\n".join(out)


def art(ad, box):
    x, y, w, h = box
    side = min(w, h)
    return ('<svg x="%.1f" y="%.1f" width="%.1f" height="%.1f" viewBox="0 0 100 100">%s</svg>'
            % (x + (w - side) / 2, y + (h - side) / 2, side, side,
               getattr(ad_art, ad["art"])(ad["palette"])))


# --------------------------------------------------------------------------------------------
# Layouts
# --------------------------------------------------------------------------------------------

def _brand(ad, box, align, colour=None, max_size=200):
    p = ad["palette"]
    return text_block(ad["brand_font"], ad["brand"], box, colour or p["brand"], p["accent"],
                      align=align, max_lines=1, max_size=max_size, upper=ad.get("brand_upper"))[0]


def _fine(ad, w, h, x=None, align="end", colour=None):
    if not ad.get("fine"):
        return ""
    size = max(8, round(h * (0.022 if h > w else 0.026)))
    room = w * (0.9 if h > w else 0.5)
    while size > 7 and text_width("poppins", size, ad["fine"]) > room:
        size -= 1
    colour = colour or ad["palette"].get("fine", ad["palette"]["ink"])
    if h > w:
        x, align = w / 2, "middle"
    x = w - size * 0.8 if x is None else x
    return ('<text x="%.1f" y="%.1f" font-family="Poppins" font-weight="400" font-size="%d" '
            'text-anchor="%s" fill="%s" opacity="0.8">%s</text>'
            % (x, h - size * 0.7, size, align, colour, escape(ad["fine"])))


def layout_portrait(ad, w, h, square=False):
    p = ad["palette"]
    m = w * 0.07
    if square:
        head = (m, h * 0.045, w - 2 * m, h * 0.22)
        art_box = (m, h * 0.285, w - 2 * m, h * 0.385)
        sub = (m, h * 0.675, w - 2 * m, h * 0.07)
        band = (0, h * 0.765, w, h * 0.235)
    else:
        head = (m, h * 0.045, w - 2 * m, h * 0.29)
        art_box = (m, h * 0.35, w - 2 * m, h * 0.34)
        sub = (m, h * 0.70, w - 2 * m, h * 0.075)
        band = (0, h * 0.80, w, h * 0.20)
    out = [background(ad, w, h, art_box), art(ad, art_box)]
    out.append(text_block(ad["head_font"], ad["headline"], head, p["fg"], p["accent"],
                          max_lines=4 if not square else 3, shadow=ad.get("shadow"))[0])
    if ad.get("sub"):
        out.append(text_block(ad.get("sub_font", "poppins-semi"), ad["sub"], sub, p["ink"],
                              p["accent"], max_lines=2, max_size=int(h * 0.04))[0])
    bx, by, bw, bh = band
    band_colour = p.get("band")
    if band_colour:
        out.append('<rect x="0" y="%.1f" width="%d" height="%.1f" fill="%s"/>'
                   % (by, w, h - by, band_colour))
    brand_colour = p.get("band_ink", p["brand"]) if band_colour else p["brand"]
    out.append(_brand(ad, (m, by + bh * 0.12, w - 2 * m, bh * 0.46), "middle", brand_colour))
    if ad.get("cta"):
        out.append(text_block("poppins-bold", ad["cta"], (m, by + bh * 0.62, w - 2 * m, bh * 0.2),
                              brand_colour, p["accent"], max_lines=1,
                              max_size=int(h * 0.04))[0])
    out.append(_fine(ad, w, h, colour=p.get("band_ink") if band_colour else None))
    return out


def layout_landscape(ad, w, h, bulletin=False):
    p = ad["palette"]
    left_art = ad.get("art_side", "right") == "left"
    art_w = w * (0.30 if bulletin else 0.40)
    m = h * (0.09 if bulletin else 0.08)
    art_box = (0 if left_art else w - art_w, h * 0.06, art_w, h * 0.88)
    tx = art_w + m * 0.5 if left_art else m
    tw = w - art_w - m * 1.5
    out = [background(ad, w, h, art_box), art(ad, art_box)]
    if bulletin:
        # A billboard is read at speed: headline, brand, and nothing smaller than the number.
        head = (tx, h * 0.08, tw, h * 0.54)
        sub = None
        brand = (tx, h * 0.68, tw * 0.60, h * 0.24)
        cta = (tx + tw * 0.62, h * 0.72, tw * 0.38, h * 0.16)
        out.append(text_block(ad["head_font"], ad.get("headline_short", ad["headline"]), head,
                              p["fg"], p["accent"], align="start", max_lines=2,
                              shadow=ad.get("shadow"))[0])
    else:
        head = (tx, h * 0.08, tw, h * 0.46)
        sub = (tx, h * 0.57, tw, h * 0.12)
        brand = (tx, h * 0.73, tw, h * 0.13)
        cta = (tx, h * 0.87, tw, h * 0.065)
        out.append(text_block(ad["head_font"], ad["headline"], head, p["fg"], p["accent"],
                              align="start", max_lines=3, shadow=ad.get("shadow"))[0])
    if ad.get("sub") and sub:
        out.append(text_block(ad.get("sub_font", "poppins-semi"), ad["sub"], sub, p["ink"],
                              p["accent"], align="start", max_lines=2,
                              max_size=int(h * 0.06))[0])
    out.append(_brand(ad, brand, "start"))
    if ad.get("cta"):
        out.append(text_block("poppins-bold", ad["cta"], cta, p["ink"], p["accent"],
                              align="end" if bulletin else "start", max_lines=1,
                              max_size=int(h * (0.12 if bulletin else 0.07)))[0])
    out.append(_fine(ad, w, h, x=(tx if left_art else w - m * 0.5),
                     align="start" if left_art else "end"))
    return out


def svg_for(ad, shape):
    w, h = SHAPES[shape]
    if shape == "portrait":
        body = layout_portrait(ad, w, h)
    elif shape == "square":
        body = layout_portrait(ad, w, h, square=True)
    else:
        body = layout_landscape(ad, w, h, bulletin=(shape == "bulletin"))
    defs = ('<defs><filter id="shadow" x="-10%%" y="-10%%" width="120%%" height="130%%">'
            '<feDropShadow dx="0" dy="%.1f" stdDeviation="%.1f" flood-color="#000" '
            'flood-opacity="0.35"/></filter></defs>' % (h / 160, h / 220))
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" '
            'viewBox="0 0 %d %d">%s%s</svg>' % (w, h, w, h, defs, "\n".join(body)))


def render(ad, shape):
    svg = svg_for(ad, shape)
    fonts = sorted({font_file(k) for k in FONTS})
    png = resvg_py.svg_to_bytes(svg_string=svg, font_files=fonts, skip_system_fonts=True)
    image = Image.open(io.BytesIO(bytes(png))).convert("RGB")
    return image


def png_bytes(image):
    """An indexed PNG: flat colour art loses nothing visible at 256 colours, and it halves the
    jar. Max coverage, not median cut -- median cut merged the sunburst rays into grey."""
    image = image.quantize(colors=256, method=Image.Quantize.MAXCOVERAGE,
                           dither=Image.Dither.FLOYDSTEINBERG)
    buffer = io.BytesIO()
    image.save(buffer, format="PNG", optimize=True)
    return buffer.getvalue()


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------

def index_json(ads):
    doc = {
        "source": "parody",
        "shapes": {k: list(SHAPES[k]) for k in SHAPE_ORDER},
        "ads": [dict(id=ad["id"], brand=ad["brand"], category=ad["category"],
                     shapes=SHAPE_ORDER, background=ad["palette"]["bg"]) for ad in ads],
    }
    return (json.dumps(doc, indent=2) + "\n").encode("utf-8")


def outputs(ads):
    files = {}
    for ad in ads:
        for shape in SHAPE_ORDER:
            files[os.path.join(TEX_DIR, "%s_%s.png" % (ad["id"], shape))] = \
                png_bytes(render(ad, shape))
    files[INDEX_PATH] = index_json(ads)
    return files


def contact_sheet(ads, path):
    scale = 0.34
    pad = 14
    label_h = 22
    widths = [int(SHAPES[s][0] * scale) for s in SHAPE_ORDER]
    row_h = int(max(SHAPES[s][1] for s in SHAPE_ORDER) * scale) + label_h + pad
    sheet = Image.new("RGB", (sum(widths) + pad * (len(widths) + 1), row_h * len(ads) + pad),
                      (40, 40, 44))
    draw = ImageDraw.Draw(sheet)
    label = pil_font("poppins-semi", 15)
    for r, ad in enumerate(ads):
        y = pad + r * row_h
        draw.text((pad, y), "%s  --  %s" % (ad["id"], ad["brand"]), font=label,
                  fill=(230, 230, 230))
        x = pad
        for shape, sw in zip(SHAPE_ORDER, widths):
            image = render(ad, shape)
            sh = int(image.height * scale)
            sheet.paste(image.resize((sw, sh), Image.LANCZOS), (x, y + label_h))
            x += sw + pad
    sheet.save(path)
    print("wrote " + path)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--sheet", metavar="PNG")
    parser.add_argument("--only", nargs="+", metavar="ID")
    args = parser.parse_args()

    ads = ad_art.CATALOGUE
    if args.only:
        ads = [ad for ad in ads if ad["id"] in args.only]
    ids = [ad["id"] for ad in ad_art.CATALOGUE]
    if len(ids) != len(set(ids)):
        sys.exit("duplicate ad id in the catalogue")

    if args.sheet:
        contact_sheet(ads, args.sheet)
        return 0

    files = outputs(ad_art.CATALOGUE)
    if args.check:
        stale = [p for p, data in files.items()
                 if not os.path.exists(p) or open(p, "rb").read() != data]
        extra = []
        if os.path.isdir(TEX_DIR):
            extra = [os.path.join(TEX_DIR, n) for n in os.listdir(TEX_DIR)
                     if os.path.join(TEX_DIR, n) not in files]
        for p in stale:
            print("stale: " + os.path.relpath(p, REPO))
        for p in extra:
            print("not generated: " + os.path.relpath(p, REPO))
        return 1 if stale or extra else 0

    os.makedirs(TEX_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(INDEX_PATH), exist_ok=True)
    for name in os.listdir(TEX_DIR):
        if os.path.join(TEX_DIR, name) not in files:
            os.remove(os.path.join(TEX_DIR, name))
    written = 0
    for p, data in files.items():
        if not os.path.exists(p) or open(p, "rb").read() != data:
            with open(p, "wb") as out:
                out.write(data)
            written += 1
    total = sum(len(d) for d in files.values())
    print("%d ads, %d files (%d written), %.1f MB" % (len(ad_art.CATALOGUE), len(files), written,
                                                       total / 1e6))
    return 0


if __name__ == "__main__":
    sys.exit(main())
