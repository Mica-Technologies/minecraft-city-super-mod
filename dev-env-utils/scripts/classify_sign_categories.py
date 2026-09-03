#!/usr/bin/env python3
"""Prototype MUTCD category classifier for the road sign blocks.

Every road sign in CSM draws its face from a single 128x128 texture whose alpha channel carries
the sign's outline. MUTCD encodes a sign's class in exactly those two things -- its shape and its
background colour -- so the texture alone is enough to guess the category for most of the catalog.

The classifier runs three independent passes and reports where they disagree, so the residue that
actually needs a human decision is small and explicitly listed:

  1. shape    -- IoU of the alpha mask against ideal shape templates (diamond, octagon, ...)
  2. colour   -- MUTCD background/border colour of the sign face
  3. keywords -- the display name from en_us.lang, as an independent cross-check

Outputs (written next to this script, under sign_classification/):
  sign_categories.csv       one row per sign with every intermediate feature, not just the verdict
  sheet_<category>.png      contact sheet per predicted category, for eyeballing the result

Nothing here writes to src/. This is an analysis tool.

Usage:
    python classify_sign_categories.py
"""

import colorsys
import csv
import json
import os
import re
import sys
from collections import Counter, defaultdict

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_block_index as ix  # noqa: E402

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = ix.REPO_ROOT
ASSETS = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm")
LANG_FILES = ix.lang_files("en_us")
OUT_DIR = os.path.join(SCRIPT_DIR, "sign_classification")

SIGN_TAB = "tabroadsigns"

# --------------------------------------------------------------------------------------------
# Categories
# --------------------------------------------------------------------------------------------

CAT_REGULATORY = "Regulatory"
CAT_WARNING = "Warning"
CAT_WORKZONE = "Temporary Traffic Control"
CAT_GUIDE = "Guide / Route"
CAT_SERVICES = "Motorist Services"
CAT_RECREATION = "Recreational / Cultural"
CAT_SCHOOL = "School"
CAT_PEDBIKE = "Pedestrian / Bike Warning"
CAT_HARDWARE = "Hardware / Structure"
CAT_UNKNOWN = "Unclassified"

CATEGORY_ORDER = [
    CAT_HARDWARE, CAT_REGULATORY, CAT_WARNING, CAT_PEDBIKE, CAT_SCHOOL,
    CAT_WORKZONE, CAT_GUIDE, CAT_SERVICES, CAT_RECREATION, CAT_UNKNOWN,
]

# --------------------------------------------------------------------------------------------
# Shape templates
# --------------------------------------------------------------------------------------------

TEMPLATE_SIZE = 64


def _poly_template(points):
    """Rasterise a polygon given in 0..1 space into a TEMPLATE_SIZE boolean mask."""
    img = Image.new("L", (TEMPLATE_SIZE, TEMPLATE_SIZE), 0)
    scaled = [(x * (TEMPLATE_SIZE - 1), y * (TEMPLATE_SIZE - 1)) for x, y in points]
    ImageDraw.Draw(img).polygon(scaled, fill=255)
    return img


def _ellipse_template():
    img = Image.new("L", (TEMPLATE_SIZE, TEMPLATE_SIZE), 0)
    ImageDraw.Draw(img).ellipse([0, 0, TEMPLATE_SIZE - 1, TEMPLATE_SIZE - 1], fill=255)
    return img


def _crossbuck_template():
    img = Image.new("L", (TEMPLATE_SIZE, TEMPLATE_SIZE), 0)
    d = ImageDraw.Draw(img)
    w = TEMPLATE_SIZE * 0.22
    d.line([(0, 0), (TEMPLATE_SIZE, TEMPLATE_SIZE)], fill=255, width=int(w))
    d.line([(TEMPLATE_SIZE, 0), (0, TEMPLATE_SIZE)], fill=255, width=int(w))
    return img


_OCT = 1.0 / (1.0 + 2.0 ** 0.5) / 2.0 * 2.0  # octagon corner cut, ~0.293 of the side

SHAPE_TEMPLATES = {
    "rectangle": _poly_template([(0, 0), (1, 0), (1, 1), (0, 1)]),
    "diamond": _poly_template([(0.5, 0), (1, 0.5), (0.5, 1), (0, 0.5)]),
    "octagon": _poly_template([
        (_OCT, 0), (1 - _OCT, 0), (1, _OCT), (1, 1 - _OCT),
        (1 - _OCT, 1), (_OCT, 1), (0, 1 - _OCT), (0, _OCT),
    ]),
    "circle": _ellipse_template(),
    "triangle_down": _poly_template([(0, 0), (1, 0), (0.5, 1)]),
    "triangle_up": _poly_template([(0.5, 0), (1, 1), (0, 1)]),
    "pennant": _poly_template([(0, 0), (1, 0.5), (0, 1)]),
    "pentagon": _poly_template([(0.5, 0), (1, 0.38), (1, 1), (0, 1), (0, 0.38)]),
    "shield": _poly_template([
        (0, 0), (1, 0), (1, 0.55), (0.75, 0.85), (0.5, 1), (0.25, 0.85), (0, 0.55),
    ]),
    "trapezoid": _poly_template([(0.15, 0), (0.85, 0), (1, 1), (0, 1)]),
    "crossbuck": _crossbuck_template(),
}

# Shapes whose IoU is only meaningful when the mask is genuinely non-rectangular.
_RECT_FILL_FLOOR = 0.97


def detect_shape(alpha):
    """Return (shape_name, iou, fill_ratio) for an alpha-channel Image."""
    mask = alpha.point(lambda a: 255 if a >= 128 else 0)
    bbox = mask.getbbox()
    if bbox is None:
        return "empty", 0.0, 0.0
    cropped = mask.crop(bbox).resize((TEMPLATE_SIZE, TEMPLATE_SIZE), Image.NEAREST)
    pixels = cropped.load()
    fill = sum(1 for y in range(TEMPLATE_SIZE) for x in range(TEMPLATE_SIZE)
               if pixels[x, y]) / float(TEMPLATE_SIZE * TEMPLATE_SIZE)
    if fill >= _RECT_FILL_FLOOR:
        return "rectangle", 1.0, fill

    best, best_iou = "other", 0.0
    for name, template in SHAPE_TEMPLATES.items():
        tp = template.load()
        inter = union = 0
        for y in range(TEMPLATE_SIZE):
            for x in range(TEMPLATE_SIZE):
                a = pixels[x, y] > 0
                b = tp[x, y] > 0
                if a and b:
                    inter += 1
                if a or b:
                    union += 1
        iou = inter / float(union) if union else 0.0
        if iou > best_iou:
            best, best_iou = name, iou
    if best_iou < 0.80:
        return "other", best_iou, fill
    return best, best_iou, fill


# --------------------------------------------------------------------------------------------
# Colour
# --------------------------------------------------------------------------------------------

def mutcd_color(rgb):
    """Bucket an RGB triple into the MUTCD colour vocabulary."""
    r, g, b = [v / 255.0 for v in rgb]
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    hue = h * 360.0
    if s < 0.20:
        if v >= 0.72:
            return "white"
        if v <= 0.28:
            return "black"
        return "grey"
    if 15 <= hue < 50 and v < 0.58:
        return "brown"
    if hue < 12 or hue >= 340:
        return "red" if v > 0.35 else "brown"
    if hue < 45:
        return "orange"
    if hue < 66:
        return "yellow"
    if hue < 95:
        return "fluor_yellow_green"
    if hue < 190:
        return "green"
    if hue < 205:
        return "blue" if s > 0.35 else "white"
    if hue < 265:
        return "blue"
    if hue < 310:
        return "purple"
    return "pink"


def _erode(mask, radius):
    """Cheap square erosion of a boolean mask stored as an L-mode Image."""
    from PIL import ImageFilter
    return mask.filter(ImageFilter.MinFilter(radius * 2 + 1))


def color_features(img):
    """Return (background colour, border colour, set of colours present) for an RGBA Image."""
    alpha = img.getchannel("A")
    mask = alpha.point(lambda a: 255 if a >= 128 else 0)
    interior = _erode(mask, 5)
    border_ring = Image.eval(interior, lambda p: 255 - p)

    px = img.load()
    mp, ip, bp = mask.load(), interior.load(), border_ring.load()
    w, h = img.size

    interior_counts = Counter()
    border_counts = Counter()
    present = Counter()
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            if not mp[x, y]:
                continue
            name = mutcd_color(px[x, y][:3])
            present[name] += 1
            if ip[x, y]:
                interior_counts[name] += 1
            elif bp[x, y]:
                border_counts[name] += 1

    total = max(1, sum(present.values()))
    background = interior_counts.most_common(1)[0][0] if interior_counts else "empty"
    border = border_counts.most_common(1)[0][0] if border_counts else background
    # A colour "counts as present" once it covers 4% of the face -- enough to catch a red
    # prohibition ring or slash on an otherwise white regulatory sign.
    significant = {name for name, n in present.items() if n / float(total) >= 0.04}
    return background, border, significant, {k: v / float(total) for k, v in present.items()}


# --------------------------------------------------------------------------------------------
# Category rules
# --------------------------------------------------------------------------------------------

# Hardware is decided by the block model, never by its name. The lang file calls plenty of
# ordinary signs "Sign Pole <something> Sign" and their textures are named pole_*, but they
# render on the same flat panel model as every other sign -- "Sign Pole Speed 50 Sign" is
# just a Speed Limit 50 sign. Only these one-off models are actual structure.
STRUCTURAL_MODEL_RE = re.compile(
    r"/(sign_pole"                                  # the bare pole
    r"|sign_pole_mount"                             # pole-top mount
    r"|sign_back_w_\w*pole"                         # blank backing panel, with and without pole
    r"|sign_pole_\w*back"                           # circle/diamond/octagon/half/tall backs
    r"|sign_pole_street_name_sign_mount_\d"         # street name sign mounts
    r"|sign_post_\w*wall_mount_\w+"                 # wall mounts, standard and wide
    r"|sign_strip_\w+"                              # coloured strips
    r")$")

SHAPE_RULES = {
    "octagon": (CAT_REGULATORY, "high"),
    "triangle_down": (CAT_REGULATORY, "high"),
    "crossbuck": (CAT_WARNING, "high"),
    "pennant": (CAT_WARNING, "high"),
}

COLOR_RULES = {
    "orange": (CAT_WORKZONE, "high"),
    "yellow": (CAT_WARNING, "high"),
    "fluor_yellow_green": (CAT_PEDBIKE, "high"),
    "green": (CAT_GUIDE, "high"),
    "blue": (CAT_SERVICES, "high"),
    "brown": (CAT_RECREATION, "high"),
    "red": (CAT_REGULATORY, "high"),
    "pink": (CAT_WORKZONE, "medium"),
    "purple": (CAT_GUIDE, "low"),
}


ACCENT_FLOOR = 0.15
NEUTRAL = {"white", "black", "grey", "empty"}


def _dominant_accent(fractions):
    """Largest non-neutral colour on the face, if it covers enough of it to be the sign's class."""
    ranked = sorted(((n, f) for n, f in fractions.items() if n not in NEUTRAL),
                    key=lambda t: -t[1])
    if ranked and ranked[0][1] >= ACCENT_FLOOR:
        return ranked[0]
    return None


def classify(registry, display, model, shape, background, border, significant, fractions):
    """Return (category, confidence, reason)."""
    if model and STRUCTURAL_MODEL_RE.search(model):
        return CAT_HARDWARE, "high", "structural model %s" % model.split("/")[-1]

    if shape == "pentagon" and (background == "fluor_yellow_green"
                                or "fluor_yellow_green" in significant):
        return CAT_SCHOOL, "high", "fluorescent yellow-green pentagon"

    if shape in SHAPE_RULES:
        cat, conf = SHAPE_RULES[shape]
        return cat, conf, "shape=%s is category-defining" % shape

    if shape == "diamond":
        if background == "orange":
            return CAT_WORKZONE, "high", "orange diamond"
        if background == "fluor_yellow_green":
            return CAT_PEDBIKE, "high", "fluorescent yellow-green diamond"
        if background == "yellow":
            return CAT_WARNING, "high", "yellow diamond"
        return CAT_WARNING, "medium", "diamond with %s background" % background

    # Rectangles and everything else fall through to colour.
    if background in COLOR_RULES:
        cat, conf = COLOR_RULES[background]
        return cat, conf, "%s background" % background

    if background in ("white", "grey"):
        if "red" in significant:
            return CAT_REGULATORY, "high", "white face with red legend"
        if border in ("green", "blue", "brown"):
            return COLOR_RULES[border][0], "medium", "white face, %s border" % border
        if significant <= {"white", "black", "grey"}:
            return CAT_REGULATORY, "medium", "black-on-white, no other colour"
        # A white-faced sign carrying one large block of MUTCD colour is really a sign of that
        # colour's class with a white panel -- a blue parking "P", a green guide legend, a
        # fluorescent yellow-green ped/school panel. Let that colour decide.
        accent = _dominant_accent(fractions)
        if accent:
            name, share = accent
            if name == "fluor_yellow_green":
                cat = CAT_SCHOOL if "school" in display.lower() else CAT_PEDBIKE
                return cat, "medium", "white face, %s accent (%.0f%%)" % (name, share * 100)
            if name in COLOR_RULES:
                return (COLOR_RULES[name][0], "medium",
                        "white face, %s accent (%.0f%%)" % (name, share * 100))
        return CAT_UNKNOWN, "low", "white face, mixed legend %s" % sorted(significant)

    if background == "black":
        if "white" in significant:
            return CAT_REGULATORY, "low", "white-on-black"
        return CAT_UNKNOWN, "low", "black face"

    return CAT_UNKNOWN, "low", "background=%s shape=%s" % (background, shape)


# --------------------------------------------------------------------------------------------
# Independent keyword cross-check
# --------------------------------------------------------------------------------------------

KEYWORD_RULES = [
    (CAT_SCHOOL, r"\bschool\b"),
    (CAT_WORKZONE,
     r"\b(work zone|road ?work|flagger|detour|construction|shoulder work|lane closed|"
     r"road closed|pilot car|survey crew|shift (left|right)|utility work)\b"),
    (CAT_PEDBIKE, r"\b(pedestrian|ped |crosswalk|bike|bicycle|cyclist)\b"),
    (CAT_RECREATION,
     r"\b(camp|picnic|trail|park\b|golf|beach|museum|zoo|boat|fish|hik|ski|archery|"
     r"historic|scenic)\b"),
    (CAT_SERVICES,
     r"\b(hospital|gas|fuel|food|lodging|rest area|airport|ferry|phone|charging|"
     r"ambulance|police|information)\b"),
    (CAT_GUIDE,
     r"\b(route|interstate|exit|mile|junction|jct|north|south|east|west|alt|business|"
     r"shield|street name|welcome|city limit|population)\b"),
    (CAT_WARNING,
     r"\b(ahead|curve|crossing|xing|slippery|bump|dip|merge|narrow|dead end|hill|"
     r"advisory|caution|hazard|falling|deer|animal|low clearance|soft shoulder|"
     r"no outlet|winding|steep|signal ahead|stop ahead|yield ahead)\b"),
    (CAT_REGULATORY,
     r"\b(no |stop|yield|speed limit|do not|one way|keep right|keep left|turn|parking|"
     r"hov|weight|limit|prohibit|required|must|only|restricted|permit|tow|passing|"
     r"lane use|left lane|right lane)\b"),
]


def keyword_guess(display):
    low = display.lower()
    for cat, pattern in KEYWORD_RULES:
        if re.search(pattern, low):
            return cat
    return CAT_UNKNOWN


# --------------------------------------------------------------------------------------------
# Inputs
# --------------------------------------------------------------------------------------------

def load_lang():
    names = {}
    for path in LANG_FILES:
        with open(path, encoding="utf-8") as handle:
            for line in handle:
                match = re.match(r"^tile\.([A-Za-z0-9_]+)\.name=(.*)$", line.strip())
                if match:
                    names[match.group(1)] = match.group(2)
    return names


def load_sign_textures():
    """Map each road-sign registry name to its face texture path, in tab registration order."""
    _, tabs, _ = ix.build_index()
    entries = [r for r, _ in tabs.get(SIGN_TAB, []) if r]
    resolved = []
    for registry in entries:
        path = ix.blockstate_path(registry)
        if path is None:
            continue
        with open(path, encoding="utf-8") as handle:
            data = json.load(handle)
        textures = (data.get("defaults", {}) or {}).get("textures", {}) or {}
        # Key "1" is the sign face in the shared sign model; "all" covers the simpler blockstates.
        ref = textures.get("1") or textures.get("all") or textures.get("particle")
        if not ref:
            continue
        rel = ref.split(":", 1)[-1]
        resolved.append((registry, os.path.join(ASSETS, "textures", rel + ".png"),
                         (data.get("defaults", {}) or {}).get("model", "")))
    return resolved


# --------------------------------------------------------------------------------------------
# Contact sheets
# --------------------------------------------------------------------------------------------

TILE = 96
LABEL_H = 28
PAD = 8
CELL_W = 150
COLS = 7


def _font(size):
    for candidate in (r"C:\Windows\Fonts\segoeui.ttf", r"C:\Windows\Fonts\arial.ttf",
                      "/System/Library/Fonts/Supplemental/Arial.ttf"):
        if os.path.exists(candidate):
            try:
                return ImageFont.truetype(candidate, size)
            except OSError:
                pass
    return ImageFont.load_default()


def _fit(draw, text, font, max_width):
    """Truncate text with an ellipsis so it fits max_width pixels."""
    if draw.textlength(text, font=font) <= max_width:
        return text
    while text and draw.textlength(text + "...", font=font) > max_width:
        text = text[:-1]
    return text + "..."


def write_sheet(category, rows, path, subtitle=None):
    font = _font(11)
    cell_w = CELL_W
    cell_h = TILE + LABEL_H + PAD * 2
    cols = COLS
    n = len(rows)
    grid_rows = (n + cols - 1) // cols
    width = cols * cell_w
    height = 40 + grid_rows * cell_h
    sheet = Image.new("RGB", (width, height), (32, 34, 38))
    draw = ImageDraw.Draw(sheet)
    heading = "%s  --  %d signs" % (category, n)
    if subtitle:
        heading += "   (%s)" % subtitle
    draw.text((PAD, 12), heading, font=_font(16), fill=(240, 240, 240))

    for i, row in enumerate(rows):
        cx = (i % cols) * cell_w
        cy = 40 + (i // cols) * cell_h
        try:
            face = Image.open(row["texture"]).convert("RGBA")
        except OSError:
            continue
        face = face.resize((TILE, TILE), Image.NEAREST)
        # Checkerboard so transparent sign shapes read correctly.
        board = Image.new("RGBA", (TILE, TILE), (70, 70, 76, 255))
        cdraw = ImageDraw.Draw(board)
        for by in range(0, TILE, 12):
            for bx in range(0, TILE, 12):
                if (bx // 12 + by // 12) % 2 == 0:
                    cdraw.rectangle([bx, by, bx + 11, by + 11], fill=(88, 88, 94, 255))
        board.alpha_composite(face)
        sheet.paste(board.convert("RGB"), (cx + (cell_w - TILE) // 2, cy + PAD))

        conf_color = {"high": (150, 220, 150), "medium": (230, 210, 130),
                      "low": (235, 150, 150)}.get(row["confidence"], (200, 200, 200))
        text_w = cell_w - PAD * 2
        draw.text((cx + PAD, cy + PAD + TILE + 2),
                  _fit(draw, row["display"], font, text_w), font=font, fill=(225, 225, 225))
        detail = "%s/%s" % (row["shape"], row["background"])
        if not row["agrees"]:
            detail += " != %s" % row["keyword"]
        draw.text((cx + PAD, cy + PAD + TILE + 15),
                  _fit(draw, detail, font, text_w), font=font, fill=conf_color)
    sheet.save(path)


# --------------------------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------------------------

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    lang = load_lang()
    signs = load_sign_textures()
    print("classifying %d road signs..." % len(signs))

    rows = []
    for registry, texture, model in signs:
        display = lang.get(registry, registry)
        try:
            img = Image.open(texture).convert("RGBA")
        except OSError:
            rows.append(dict(registry=registry, display=display, texture=texture,
                             shape="missing", iou=0.0, fill=0.0, background="", border="",
                             present="", category=CAT_UNKNOWN, confidence="low",
                             reason="texture not found", keyword=keyword_guess(display),
                             agrees=False))
            continue
        shape, iou, fill = detect_shape(img.getchannel("A"))
        background, border, significant, fractions = color_features(img)
        category, confidence, reason = classify(
            registry, display, model, shape, background, border, significant, fractions)
        kw = keyword_guess(display)
        agrees = (kw == category) or kw == CAT_UNKNOWN
        rows.append(dict(
            registry=registry, display=display, texture=texture, shape=shape,
            iou=round(iou, 3), fill=round(fill, 3), background=background, border=border,
            present=";".join("%s=%.2f" % (k, v) for k, v in
                             sorted(fractions.items(), key=lambda t: -t[1])[:4]),
            category=category, confidence=confidence, reason=reason, keyword=kw, agrees=agrees))

    csv_path = os.path.join(OUT_DIR, "sign_categories.csv")
    with open(csv_path, "w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    by_cat = defaultdict(list)
    for row in rows:
        by_cat[row["category"]].append(row)
    for category in CATEGORY_ORDER:
        if by_cat.get(category):
            slug = re.sub(r"[^a-z0-9]+", "_", category.lower()).strip("_")
            write_sheet(category, by_cat[category], os.path.join(OUT_DIR, "sheet_%s.png" % slug))

    print("\n%-28s %6s   %s" % ("CATEGORY", "COUNT", "confidence high/med/low"))
    for category in CATEGORY_ORDER:
        group = by_cat.get(category, [])
        if not group:
            continue
        conf = Counter(r["confidence"] for r in group)
        print("%-28s %6d   %d / %d / %d" % (
            category, len(group), conf["high"], conf["medium"], conf["low"]))
    print("%-28s %6d" % ("TOTAL", len(rows)))

    disagree = [r for r in rows if not r["agrees"]]
    low = [r for r in rows if r["confidence"] == "low"]
    queue = [r for r in rows if not r["agrees"] or r["confidence"] == "low"]
    if queue:
        write_sheet("Review queue", queue, os.path.join(OUT_DIR, "sheet_review_queue.png"),
                    subtitle="low confidence, or shape+colour disagrees with the name")
    print("\nneeds review: %d low-confidence, %d disagree with the name keywords "
          "(%d unique signs)" % (len(low), len(disagree),
                                 len({r["registry"] for r in low + disagree})))
    print("output: %s" % OUT_DIR)


if __name__ == "__main__":
    main()
