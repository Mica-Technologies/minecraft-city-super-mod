#!/usr/bin/env python3
"""Replace existing road signs' faces with the official FHWA Standard Highway Signs drawings.

The catalogue below maps a registry name to the SHS page (or interim ZIP) its design is
drawn on. For each, the script reads the sign's own blockstate to find the plate model and
the texture it paints (slot ``1``; the file is often not named after the registry), measures
the plate's aspect off the model's elements, renders the drawing through
``shs_signs.official_face`` and writes the texture -- nothing else. A silhouette sign whose
blockstate names a ``_back`` texture on slot ``2`` gets that regenerated too. Registration,
lang and blockstates are never touched: this is a texture swap for signs that already exist,
which is what makes ``--check`` a byte comparison and a batch reversible with
``git checkout <commit> -- <texture>``.

    python dev-env-utils/scripts/gen_official_faces.py [--only a,b] [--sheet out.png] [--check]
    python dev-env-utils/scripts/gen_official_faces.py --verify-sheet <prefix>

``--sheet`` writes a before/after contact sheet at plate aspect (current texture left, new
face right) without touching the tree -- the batch review the user sees before anything is
written. ``--verify-sheet`` renders every ``?`` row of
``assets/docs/agent_progress/SHS_MATCH_TABLE.md`` as the current texture beside a thumbnail
of the cited book page, eight per sheet, so the token-matched guesses can be confirmed or
rejected by eye before they are catalogued.

Numbered signs (Speed Limit, CURVE xx MPH, ...) use ``replace=('50', '35')``: the book draws
one numeral, which is dropped and the mod's set in its place at the same cap height.
"""

import json
import os
import re
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import shs_signs as shs  # noqa: E402

MATCH_TABLE = os.path.join(layout.REPO_ROOT, 'assets', 'docs', 'agent_progress', 'SHS_MATCH_TABLE.md')


# ----------------------------------------------------------------------------- sources

def SHS(chapter, page, pick=0, mirror=False, palette=None, replace=None, inner=None,
        rotate_symbols=0, mirror_symbols=False, condense=False, legend_colour=None):
    """A face from a 2004 SHS book page (0-based). ``pick`` for pages with more than one
    sign, ``mirror`` for the left-hand version of a symbol the book draws right-handed only
    (``mirror_symbols`` when it carries a legend: the arrow flips, the words do not),
    ``rotate_symbols`` to turn an arrow in place, ``replace=(old, new)`` to re-set the one
    numeral the book draws."""
    return lambda: (shs.book_sign(chapter, page, pick, inner=inner, replace=replace,
                                  rotate_symbols=rotate_symbols, mirror_symbols=mirror_symbols,
                                  condense=condense, legend_colour=legend_colour),
                    mirror, palette)


class SymbolFace(object):
    """A bare pictogram from the book plus the panel colour it goes on; composed by
    :func:`render` once the sign's plate aspect is known."""

    def __init__(self, symbol, colour):
        self.symbol, self.colour = symbol, colour


def SYM(chapter, page, picks, colour):
    """A recreational / services pictogram the book draws without a panel (``picks`` are the
    outline numbers, several for a symbol drawn in pieces), set in white on a rounded panel
    of ``colour`` ('brown', 'blue', 'green') with a white border."""
    return lambda: (SymbolFace(shs.symbol_face(chapter, page, picks), shs.MOD_COLOURS[colour]), False, None)


class ComposedFace(object):
    """A face built by a function of the plate aspect, for the few signs that are an official
    drawing plus a step of the mod's own (a panel cut to a point)."""

    def __init__(self, fn):
        self.fn = fn


def _inset_polygon(pts, d):
    """A convex polygon moved ``d`` inward along every edge (clockwise or not)."""
    n = len(pts)
    area = sum(pts[i][0] * pts[(i + 1) % n][1] - pts[(i + 1) % n][0] * pts[i][1] for i in range(n))
    sign = 1.0 if area > 0 else -1.0
    lines = []
    for i in range(n):
        (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % n]
        ex, ey = x1 - x0, y1 - y0
        ln = (ex * ex + ey * ey) ** 0.5
        nx, ny = -ey / ln * sign, ex / ln * sign          # the inward normal
        lines.append(((x0 + nx * d, y0 + ny * d), (ex, ey)))
    out = []
    for i in range(n):
        (px, py), (ax, ay) = lines[i - 1]
        (qx, qy), (bx, by) = lines[i]
        t = ((qx - px) * by - (qy - py) * bx) / float(ax * by - ay * bx)
        out.append((px + ax * t, py + ay * t))
    return out


def _rounded_polygon(pts, r, steps=12):
    """A convex polygon with every corner replaced by an arc of radius ``r``."""
    import math
    out = []
    n = len(pts)
    for i in range(n):
        px, py = pts[i]
        ux, uy = pts[i - 1][0] - px, pts[i - 1][1] - py
        vx, vy = pts[(i + 1) % n][0] - px, pts[(i + 1) % n][1] - py
        lu, lv = math.hypot(ux, uy), math.hypot(vx, vy)
        ux, uy, vx, vy = ux / lu, uy / lu, vx / lv, vy / lv
        half = math.acos(max(-1.0, min(1.0, ux * vx + uy * vy))) / 2.0
        t = r / math.tan(half)
        bx, by = ux + vx, uy + vy
        lb = math.hypot(bx, by)
        cx, cy = px + bx / lb * r / math.sin(half), py + by / lb * r / math.sin(half)
        a0 = math.atan2(py + uy * t - cy, px + ux * t - cx)
        a1 = math.atan2(py + vy * t - cy, px + vx * t - cx)
        sweep = (a1 - a0 + math.pi) % (2 * math.pi) - math.pi   # the short way round
        out += [(cx + r * math.cos(a0 + sweep * k / steps), cy + r * math.sin(a0 + sweep * k / steps))
                for k in range(steps + 1)]
    return out


def POINTED_ONE_WAY(left):
    """The ONE WAY sign whose panel is cut to a point behind the arrowhead, as Los Angeles and
    other cities make it: a pentagon panel at the plate's proportions (thin white border,
    black field), the R6-1's own arrowhead and legend set on it with the shaft run out to
    the square end. The cut is parallel to the arrowhead's edges, so the black margin round
    the head is as even as it is above and below it; the plate is longer than the R6-1's 3:1,
    so the extra shaft goes to the word space and either side of the legend."""
    def make(aspect):
        import math
        import numpy as np
        H = 480
        W = int(round(H * aspect))
        white, black = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['black']
        b = 0.04 * H                            # white border
        m = 0.075 * H                           # black field above and below the head
        tail = 0.2 * H                          # black field beyond the end of the shaft

        # the arrow and legend alone, off the page (the R6-1L; the right sign mirrors the
        # arrow, never the words)
        art = shs.recolour(shs.book_sign(R, 87, 1, symbols_only=True), shs.SHS_PALETTE)
        art = art.crop(art.getchannel('A').getbbox())
        a = np.asarray(art).astype(np.float32)
        alpha = a[..., 3] / 255.0
        lum = a[..., :3].mean(axis=2)
        ink = alpha * np.clip((white[0] - lum) / float(white[0] - black[0]), 0.0, 1.0)
        solid = alpha > 0.5

        # the head's slope (x per y) off its upper edge, and the shaft off the last column
        ah, aw = solid.shape
        def lead(row):
            return float(np.argmax(solid[row]))
        k = (lead(int(ah * 0.2)) - lead(int(ah * 0.45))) / (ah * 0.45 - ah * 0.2)
        rows = np.nonzero(solid[:, -4])[0]
        shaft_top, shaft_bot = rows[0], rows[-1] + 1
        # the outline's anti-aliased edge renders dark; the legend is inside the shaft only
        ink[:shaft_top + 4] = 0
        ink[shaft_bot - 4:] = 0
        cols = np.nonzero(ink.max(axis=0) > 0.5)[0]
        legend_x0, legend_x1 = cols[0], cols[-1] + 1

        scale = (H - 2 * (b + m)) / float(ah)
        slant = math.sqrt(1 + k * k)            # horizontal run of a unit perpendicular
        tip = (b + m) * slant                   # the head's point, in panel px
        shaft_end = W - b - tail

        # the panel: outer outline, then the black field inset by the border
        shoulder = k * H / 2.0
        outer = [(0, H / 2.0), (shoulder, 0), (W, 0), (W, H), (shoulder, H)]
        inner = _inset_polygon(outer, b)
        panel = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(panel)
        d.polygon(_rounded_polygon(outer, 0.08 * H), fill=white)
        d.polygon(_rounded_polygon(inner, 0.04 * H), fill=black)

        # the arrow: the book's silhouette, its shaft carried on to shaft_end
        sw, sh = int(round(aw * scale)), int(round(ah * scale))
        top = (H - sh) / 2.0
        sil = Image.fromarray((solid * 255).astype(np.uint8)).resize((sw, sh), Image.LANCZOS)
        arrow = Image.new('L', (W, H), 0)
        arrow.paste(sil, (int(round(tip)), int(round(top))))
        ImageDraw.Draw(arrow).rectangle(
            [tip + sw - 2, top + shaft_top * scale, shaft_end, top + shaft_bot * scale - 1], fill=255)

        # the legend: split into its two words at the widest gap, the spare shaft shared out
        cols_ink = ink[:, legend_x0:legend_x1].max(axis=0) > 0.02
        gaps, run = [], None
        for i, on in enumerate(cols_ink):
            if not on and run is None:
                run = i
            elif on and run is not None:
                gaps.append((i - run, run, i))
                run = None
        _, g0, g1 = max(gaps)
        words = [(legend_x0, legend_x0 + g0), (legend_x0 + g1, legend_x1)]
        start = tip + legend_x0 * scale                  # where the book sets it
        room = shaft_end - (tip + sw)                    # the shaft added past the book's
        offsets = [start + room * 0.45, start + room * 0.55]
        # the right sign's legend is the same block mirrored whole, its words kept in order
        block_x1 = offsets[1] + (legend_x1 - legend_x0) * scale
        shift = 0.0 if left else W - block_x1 - offsets[0]
        ink_img = Image.new('L', (W, H), 0)
        for (x0, x1), ox in zip(words, offsets):
            word = Image.fromarray((ink[:, x0:x1] * 255).astype(np.uint8))
            word = word.resize((int(round((x1 - x0) * scale)), sh), Image.LANCZOS)
            ox = ox + (x0 - legend_x0) * scale + shift
            ink_img.paste(word, (int(round(ox)), int(round(top))))

        if not left:
            arrow = arrow.transpose(Image.FLIP_LEFT_RIGHT)
            panel = panel.transpose(Image.FLIP_LEFT_RIGHT)
        panel.paste(Image.new('RGBA', (W, H), white), (0, 0), arrow)
        panel.paste(Image.new('RGBA', (W, H), black), (0, 0), ink_img)
        return shs.fit_plate(panel, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def STATE_PROPERTY():
    """California's state-property sign (no drawing exists for it): a white panel with a thin
    black border and six lines in four sizes, set from a photograph of the real sign. The
    real sign is about 30 x 18, the plate 1.38:1, so each line is narrowed to the share of
    the width it takes on the sign rather than scaled down."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 1024
        W = int(round(H * aspect))
        img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        white, black = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['black']
        m = int(H * 0.012)
        d.rounded_rectangle((m, m, W - m, H - m), radius=int(H * 0.06), fill=white)
        inset, stroke = int(H * 0.035), int(H * 0.022)
        d.rounded_rectangle((inset, inset, W - inset, H - inset), radius=int(H * 0.045),
                            outline=black, width=stroke)
        # (text, centre y, cap height, width) as fractions of the panel
        for text, cy, cap, width in (
                ('STATE PROPERTY', 0.130, 0.080, 0.67),
                ('NO DUMPING', 0.285, 0.120, 0.70),
                ('NO PARKING', 0.465, 0.120, 0.69),
                ('NO TRESPASSING', 0.645, 0.120, 0.87),
                ('VIOLATORS WILL BE PROSECUTED', 0.800, 0.058, 0.79),
                ('PENAL CODE SEC. 374.3, 602-m; VEHICLE CODE 22523, 22659', 0.905, 0.042, 0.88)):
            gg._legend_line(img, text, cy * H, cap * H, width * W, colour=black)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def _with(mapping, extra):
    out = dict(mapping)
    out.update(extra)
    return out


def _series_condense(texts, cap, max_w):
    """One horizontal narrowing for a block of lines at cap height ``cap``, set by the widest,
    so the lines read as one series rather than each squeezed to its own width."""
    from PIL import ImageFont
    import render_sign as rs
    f = ImageFont.truetype(rs.FONT_PATH, int(cap * 1.4))
    bb = f.getbbox('H')
    f = ImageFont.truetype(rs.FONT_PATH, int(round(int(cap * 1.4) * cap / (bb[3] - bb[1]))))
    widest = max(f.getbbox(t)[2] - f.getbbox(t)[0] for t in texts) + 4
    return min(1.0, max_w / widest)


def TWO_PANEL(top, lines, colour='yellow'):
    """A facility WARNING / CAUTION sign laid out like the W13-3 RAMP advisory: a short top
    panel over a black divider, the book's panel (border, divider, corner radii) in ``colour``
    with its legends dropped, ``top`` set in the top panel and ``lines`` spread down the
    bottom one at one size and one narrowing."""
    def make(aspect):
        import gen_gap_signs as gg
        panel = shs.book_sign(W, 111, blank=True)
        panel = shs.recolour(panel, _with(shs.SHS_PALETTE, {(255, 245, 0): shs.MOD_COLOURS[colour]}))
        H = 1024
        Wd = int(round(H * aspect))
        img = panel.resize((Wd, H), Image.LANCZOS)
        black = shs.MOD_COLOURS['black']
        # the page's panels, as fractions of the sign's height: top 0.03-0.33, bottom 0.35-0.97
        gg._legend_line(img, top, 0.18 * H, 0.15 * H, 0.84 * Wd, colour=black)
        n = len(lines)
        spacing = min(0.17, 0.56 / n)
        cap = min(0.095, spacing * 0.56) * H
        condense = _series_condense(lines, cap, 0.84 * Wd)
        for i, text in enumerate(lines):
            cy = 0.66 + (i - (n - 1) / 2.0) * spacing
            gg._legend_line(img, text, cy * H, cap, 0.84 * Wd, colour=black, condense=condense)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def TEXT_DIAMOND(lines, colour=None, tight=False, ink=None, gap=None, min_condense=None, shift=False):
    """A worded warning sign the book has no drawing for: the W8-1 BUMP diamond (border and
    corner radii) with its legend dropped and ``lines`` set inside it. The largest cap height
    is taken at which every line fits the diamond's width across its own band, allowing the
    block one shared narrowing down to ``MIN_CONDENSE`` (the narrower Highway Gothic series).
    ``colour`` is an RGBA replacing the warning yellow (FAWE's magenta). ``tight`` is for a
    long block (four lines under a long word): closer lines, more narrowing and a text area
    run nearer the border, as the hand-set original had it. ``ink`` replaces the black of the
    border and legend (white on a blue courtesy sign); ``gap`` (line pitch in caps) and
    ``min_condense`` tune a tight block between the two. ``shift`` lets the block move off
    centre, so its longest line sits nearer the diamond's widest point and a line near a point
    clears the border; it then keeps each line's whole cap clear of the border."""
    MIN_CONDENSE = min_condense or (0.58 if tight else 0.7)
    GAP = gap or (1.2 if tight else 1.35)
    def make(aspect):
        import gen_gap_signs as gg
        from PIL import ImageFont
        import render_sign as rs
        panel = shs.book_sign(W, 58, blank=True)
        mapping = shs.SHS_PALETTE if colour is None else _with(shs.SHS_PALETTE, {(255, 245, 0): colour})
        if ink is not None:
            mapping = _with(mapping, {k: ink for k, v in shs.SHS_PALETTE.items() if v == shs.MOD_COLOURS['black']})
        panel = shs.recolour(panel, mapping)
        S = 1024
        img = panel.resize((S, S), Image.LANCZOS)
        R = S / 2 * (0.86 if tight else 0.80)                        # half-diagonal inside the black border, less a margin
        n = len(lines)
        f = ImageFont.truetype(rs.FONT_PATH, 200)
        hb = f.getbbox('H')
        unit = [(f.getbbox(t)[2] - f.getbbox(t)[0]) / float(hb[3] - hb[1]) for t in lines]  # width per cap
        margin = 0.5 if shift else (0.1 if tight else 0.25)
        offsets = [R * i / 40.0 for i in range(-12, 13)] if shift else [0.0]
        def fit(min_condense):
            best = None
            for dy in sorted(offsets, key=abs):
                cap = 0.24 * S
                while cap > 8 and (best is None or cap > best[0]):
                    lh = cap * GAP
                    if n * lh + 2 * abs(dy) <= 2 * R * 0.9:
                        cond = 1.0
                        for k, u in enumerate(unit):
                            # measured ``margin`` caps out from the line's middle: by default the
                            # corners of a line's end letters may run a little toward the border,
                            # as on real signs; a shifted block keeps the whole cap clear
                            band = abs((k - (n - 1) / 2.0) * lh + dy) + cap * margin
                            avail = 2 * (R - band)
                            cond = min(cond, avail / (u * cap) if avail > 0 else 0)
                        if cond >= min_condense:
                            best = (cap, cond, dy)
                            break
                    cap -= 2
            return best
        # the letters' own width unless that costs a tenth of the height narrowing would give
        wide, narrow = fit(0.92), fit(MIN_CONDENSE)
        cap, cond, dy = wide if wide[0] >= 0.9 * narrow[0] and not tight else narrow
        lh = cap * GAP
        for k, t in enumerate(lines):
            cy = S / 2 + (k - (n - 1) / 2.0) * lh + dy
            gg._legend_line(img, t, cy, cap, S, colour=ink or shs.MOD_COLOURS['black'], condense=cond)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def TEXT_PANEL(chapter, page, lines, pick=0, band=(0.1, 0.9), cap_max=0.3, width=0.84, palette=None):
    """A book panel with its legend dropped and the mod's own ``lines`` set in the vertical
    ``band`` (fractions of the sign's height) at one size and one narrowing: the R9-5's bike
    over a worded bike sign, the R3-17aP plaque under an EXCEPT legend. The symbols the page
    keeps (the bike) stay where the book draws them. ``cap_max`` caps the letter height as a
    fraction of the sign's height; ``width`` is the share of the sign a line may take."""
    def make(aspect):
        import gen_gap_signs as gg
        panel = shs.book_sign(chapter, page, pick, blank=True)
        panel = shs.recolour(panel, shs.SHS_PALETTE if palette is None else _with(shs.SHS_PALETTE, palette))
        H = 1024
        Wd = int(round(H * aspect))
        img = panel.resize((Wd, H), Image.LANCZOS)
        n = len(lines)
        pitch = (band[1] - band[0]) / n
        cap = min(cap_max, pitch * 0.66) * H
        condense = _series_condense(lines, cap, width * Wd)
        for i, text in enumerate(lines):
            cy = (band[0] + pitch * (i + 0.5)) * H
            gg._legend_line(img, text, cy, cap, width * Wd, colour=shs.MOD_COLOURS['black'], condense=condense)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def SHS_PANEL_COLOUR(chapter, page, colour, pick=0):
    """A book warning sign moved onto another panel colour when its symbol also carries the
    warning yellow (the W3-3's amber lamp): only the yellow joined to the panel is recoloured,
    found by flood fill from the top of the field, so a lamp the black housing encloses keeps
    its colour. A plain ``palette`` swap turns every yellow pixel."""
    def make(aspect):
        import numpy as np
        from PIL import ImageDraw as _Draw
        face = shs.recolour(shs.book_sign(chapter, page, pick), shs.SHS_PALETTE)
        a = np.asarray(face).astype(np.int32)
        yellow = np.array(shs.MOD_COLOURS['yellow'][:3])
        near = (np.abs(a[..., :3] - yellow).sum(axis=2) < 90) & (a[..., 3] > 0)
        mask = Image.fromarray((near * 255).astype(np.uint8)).copy()   # floodfill needs its own buffer
        # seed every yellow run down the centre column above the symbol: the sheet rim outside
        # the border, then the field inside it
        col = a.shape[1] // 2
        for y in range(int(a.shape[0] * 0.15)):
            if near[y, col] and mask.getpixel((col, y)) == 255:
                _Draw.floodfill(mask, (col, y), 128)
        panel = np.asarray(mask) == 128
        out = a.copy()
        out[panel, :3] = shs.MOD_COLOURS[colour][:3]
        face = Image.fromarray(out.astype(np.uint8), 'RGBA')
        return shs.fit_plate(face, aspect, size=shs.DEFAULT_TEX)
    return lambda: (ComposedFace(make), False, None)


def PANEL(lines, colour='orange', arrow=None, band=(0.1, 0.9), cap_max=0.24, width=0.84, ink='black'):
    """A rectangular temporary-traffic-control panel no single page draws at the mod's wording:
    a rounded ``colour`` panel with the inset black border at the M4-9b's proportions, ``lines``
    set in ``band`` at one size and narrowing, and optionally the M6-2's diagonal arrow lifted
    off its page (``arrow='right'`` points up-right, ``'left'`` up-left) in the lower half.
    ``ink`` colours the border, legend and arrow (white on a green guide panel)."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 1024
        Wd = int(round(H * aspect))
        img = Image.new('RGBA', (Wd, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        black = shs.MOD_COLOURS[ink]
        m = int(H * 0.012)
        d.rounded_rectangle((m, m, Wd - m, H - m), radius=int(H * 0.07), fill=shs.MOD_COLOURS[colour])
        inset, stroke = int(H * 0.035), int(H * 0.03)
        d.rounded_rectangle((inset, inset, Wd - inset, H - inset), radius=int(H * 0.05),
                            outline=black, width=stroke)
        n = len(lines)
        pitch = (band[1] - band[0]) / n
        cap = min(cap_max, pitch * 0.66) * H
        condense = _series_condense(lines, cap, width * Wd)
        for i, text in enumerate(lines):
            gg._legend_line(img, text, (band[0] + pitch * (i + 0.5)) * H, cap, width * Wd,
                            colour=black, condense=condense)
        if arrow:
            art = shs.recolour(shs.book_sign(G, 21, symbols_only=True), shs.SHS_PALETTE)
            if ink != 'black':
                art = Image.composite(Image.new('RGBA', art.size, shs.MOD_COLOURS[ink]), art, art)
            art = art.crop(art.getchannel('A').getbbox())
            if arrow == 'left':
                art = art.transpose(Image.FLIP_LEFT_RIGHT)
            h = int(H * 0.36)
            art = art.resize((int(art.width * h / art.height), h), Image.LANCZOS)
            img.alpha_composite(art, ((Wd - art.width) // 2, int(H * 0.52)))
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def SHSI(code, variant=None, palette=None):
    """A face from an interim SHS ZIP (a sign added or redrawn since the book)."""
    return lambda: (shs.interim_sign(code, variant), False, palette)


# ----------------------------------------------------------------------------- catalogue
# registry, source, SHS code (for the sheet caption)

R = 'Regulatory'
W = 'Warning'
G = 'Guide'
E = 'EM'
# the R7-8 page draws the wheelchair panel in a slate blue; the mod's is the D9-6 blue
SLATE_TO_BLUE = {(74, 87, 120): shs.MOD_COLOURS['blue']}
FYG_FACE = {(255, 245, 0): shs.MOD_COLOURS['fyg']}   # the book's yellow onto fluorescent yellow-green
ORANGE_FACE = {(255, 245, 0): shs.MOD_COLOURS['orange']}   # ... and onto work-zone orange
INCIDENT_PINK = (255, 0, 255, 255)   # the mod's incident-management magenta (FAWE's)
CATALOGUE = [
    # --- Phase 1: Regulatory, the confident exact matches. Not here, and why: signahead is
    # the up-arrow plaque, not the AHEAD legend; onewaytlsignright is the arrow-shaped R6-1;
    # signphotoenforced carries a signal head the R10-19 legend does not; signhovlaneends
    # was drawn portrait; its plate is now the wide one, so it is back in.
    ('signpoststopsign', SHS(R, 0), 'R1-1'),
    ('yieldsign', SHS(R, 1), 'R1-2'),
    ('signpost4way', SHS(R, 3), 'R1-3'),
    ('signpostallway', SHS(R, 4), 'R1-4'),
    ('signpostspeed50', SHS(R, 11), 'R2-1'),
    ('signpostmin40', SHS(R, 17), 'R2-4'),
    ('signnorightturn', SHS(R, 22), 'R3-1'),
    ('signnoleftturn', SHS(R, 24), 'R3-2'),
    ('signnoturns', SHS(R, 26), 'R3-3'),
    ('signnouturn', SHS(R, 27), 'R3-4'),
    ('signbuslane', SHS(R, 30, pick=3), 'R3-5b'),
    ('signleftmustturnleft', SHS(R, 31), 'R3-7L'),
    ('buslaneahead', SHS(R, 44), 'R3-10a'),
    ('signhovlaneends', SHS(R, 51), 'R3-14'),   # portrait plate: the R3-14 is 24 x 30
    ('signhovlaneahead', SHS(R, 58), 'R3-15'),
    ('signaheadplaque', SHS(R, 60, pick=1), 'R3-17a'),
    ('signendsplaque', SHS(R, 60, pick=2), 'R3-17b'),
    ('signdonotpass', SHS(R, 62), 'R4-1'),
    ('signpasswithcare', SHS(R, 63), 'R4-2'),
    ('signslowertraffickeepright', SHS(R, 64), 'R4-3'),
    ('signtrucksuserightlanes', SHS(R, 66), 'R4-5'),
    ('signtrucklane500ft', SHS(R, 67), 'R4-6'),
    ('signpostkeepright', SHS(R, 68), 'R4-7'),
    ('signdonotenter', SHS(R, 74), 'R5-1'),
    ('signwrongway', SHS(R, 75), 'R5-1a'),
    ('signnotrucks', SHS(R, 77), 'R5-2'),
    ('signnomotorvehicles', SHS(R, 79), 'R5-3'),
    ('signcommercialexclude', SHS(R, 80), 'R5-4'),   # portrait plate (2026-09-13)
    ('signvehiclelugsprohibit', SHS(R, 81), 'R5-5'),   # portrait plate (2026-09-13)
    ('signmotorcycleprohibit', SHS(R, 84), 'R5-8'),
    ('signpedestrianprohibit', SHS(R, 86, pick=1), 'R5-10b'),
    ('signloadzonenoparking', SHS(R, 93, pick=1), 'R7-6'),
    ('signbusstopnoparking', SHS(R, 93, pick=2), 'R7-7'),
    ('signnoparkingbikelane', SHS(R, 95), 'R7-9'),
    ('signtowawayzone', SHS(R, 100), 'R7-201'),
    ('signnoparkingonpave', SHS(R, 102), 'R8-1'),
    ('signnoparkingexceptshoulder', SHS(R, 103), 'R8-2'),
    ('signnoparkingtext', SHS(R, 104), 'R8-3'),
    ('signnoparking', SHS(R, 105), 'R8-3a'),
    ('signemergencyparkingonly', SHS(R, 109), 'R8-4'),
    ('signnostoppingpavement', SHS(R, 110), 'R8-5'),
    ('signnostoppingexceptshoulder', SHS(R, 111), 'R8-6'),
    ('signemergencystoppingonly', SHS(R, 112), 'R8-7'),
    ('signstophereflashing', SHS(R, 115), 'R8-10'),
    ('signwalkleft', SHS(R, 116), 'R9-1'),
    ('signcrossatcrosswalks', SHS(R, 116, pick=1), 'R9-2'),
    ('signnohitchhiking', SHS(R, 120), 'R9-4a'),
    ('signpostsidewalkclosed', SHS(R, 127), 'R9-11a'),   # left arrow; the right one is a gap sign
    ('signleftongreenarrow', SHS(R, 135), 'R10-5'),
    ('signpostdonotblock', SHS(R, 137), 'R10-7'),
    ('signuselanewithgreenarrow', SHS(R, 138), 'R10-8'),
    ('signleftturnsignal', SHS(R, 139), 'R10-10L'),
    ('signpostleftturnyieldgreen', SHS(R, 143), 'R10-12'),
    ('signesignal', SHS(R, 144), 'R10-13'),
    ('signr1016', SHS(R, 146), 'R10-16'),
    ('signkeepoffmedian', SHS(R, 153), 'R11-1'),
    ('signrdclosed', SHS(R, 154), 'R11-2'),
    ('signrdclosedthrutraffic', SHS(R, 156), 'R11-4'),
    ('signweightlimit10ton', SHS(R, 157), 'R12-1'),
    ('signaxle5tonlimit', SHS(R, 159), 'R12-2'),
    # --- Phase 2: Warning, the confident exact matches. Not here: signtruckhalf is a TRUCK
    # legend, not the W11-10 symbol; signleftrightarrow is the two-headed W1-7, not W16-5p.
    ('signpsotstopahead', SHS(W, 19), 'W3-1'),
    ('signyieldahead', SHS(W, 21), 'W3-2'),
    ('signsignalahead', SHS(W, 23), 'W3-3'),
    ('signpostspeed55', SHS(R, 11, replace=('50', '55')), 'R2-1 (55)'),
    ('signaddleft', SHS(W, 31, pick=1), 'W4-3L'),
    ('signaddright', SHS(W, 31), 'W4-3R'),
    ('signnarrowbridge', SHS(W, 36), 'W5-2'),
    ('signonelanebridge', SHS(W, 37), 'W5-3'),
    ('signdivhw', SHS(W, 41), 'W6-1a'),
    ('signdividedroad', SHS(W, 42), 'W6-1b'),
    ('signdivhwend', SHS(W, 44), 'W6-2a'),
    ('signdividedhwstart', SHS(W, 40), 'W6-1'),    # the symbol pair; the worded ones are above
    ('signdividedhwend', SHS(W, 43), 'W6-2'),
    ('signtwowaytraffic', SHS(W, 46), 'W6-3'),
    ('signtruckhill', SHS(W, 47), 'W7-1'),
    ('signhill', SHS(W, 48), 'W7-1a'),
    ('signtruck8grade', SHS(W, 49), 'W7-1b'),
    ('signbump', SHS(W, 58), 'W8-1'),
    ('signdip', SHS(W, 59), 'W8-2'),
    ('signpavementends', SHS(W, 60), 'W8-3'),
    ('signsoftshoulder', SHS(W, 61), 'W8-4'),
    ('signtruckcrossing', SHS(W, 63), 'W8-6'),
    ('signunevenlanes', SHS(W, 69), 'W8-11'),
    # the mod's pedestrian family is fluorescent yellow-green, as the current MUTCD allows
    ('signbicycle', SHS(W, 90, palette=FYG_FACE), 'W11-1'),
    ('signpedestrian', SHS(W, 91, palette=FYG_FACE), 'W11-2'),
    ('signdeer', SHS(W, 92), 'W11-3'),
    ('signexit25', SHS(W, 109), 'W13-2'),
    ('signcurve25', SHS(W, 113), 'W13-5'),
    ('signdeadend', SHS(W, 115), 'W14-1'),
    ('signnooutlet', SHS(W, 117), 'W14-2'),
    ('signplayground', SHS(W, 119), 'W15-1'),
    ('signshareroad', SHS(W, 120), 'W16-1'),
    ('signnosigns', SHS(W, 133), 'W18-1'),
    ('signoncominghasextendedgreen', SHS(W, 134), 'W25-1'),
    ('signoncomingmayextendedgreen', SHS(W, 135), 'W25-2'),
    ('signendroadwork', SHS(W, 168, pick=2), 'G20-2'),
    ('signexitclosed', SHS(W, 169, pick=2), 'E5-2a'),
    # --- Phase 3: Guide and EM panel signs. The recreational pictograms (camping, dog, kayak,
    # ...) are drawn in the book as bare symbols, some in several pieces, with no panel; they
    # wait for a symbol-on-panel mode. Fallout Shelter stays: the mod's is the classic yellow
    # sign, not the EM chapter's white directional one. Police is a badge, not the D9-14
    # legend. The D5-3 and M1-10 pages draw no white border, so those two keep their faces.
    ('signnorth', SHS(G, 13), 'M3-1'),
    ('signeast', SHS(G, 13, pick=1), 'M3-2'),
    ('signsouth', SHS(G, 14), 'M3-3'),
    ('signwest', SHS(G, 14, pick=1), 'M3-4'),
    ('signtemporary', SHS(G, 18, pick=1), 'M4-7'),
    ('signrestarea1mile', SHS(G, 35), 'D5-1'),
    ('signscenicoverlook2miles', SHS(G, 51), 'D6-1'),
    ('signweighstation1mile', SHS(G, 54), 'D8-1'),
    ('signhospital', SHS(G, 60), 'D9-2'),
    ('signgas', SHS(G, 65), 'D9-7'),
    ('signfreewayentrance', SHS(G, 92), 'D13-3'),
    ('signairport', SHS(G, 98), 'I-5'),
    ('signbusstation', SHS(G, 99), 'I-6'),
    ('signtrainstation', SHS(G, 100), 'I-7'),
    ('signlibrary', SHS(G, 101), 'I-8'),
    ('signtrafficctlpoint', SHS(E, 1, pick=2), 'EM-3'),
    # --- Phase 4: the near matches -- the book draws one numeral, replace= sets the mod's.
    # Not here: signkeepright1/2 (KEEP RIGHT with an arrow, not the R4-7 symbol), sign14_4
    # (a diamond, not the W12-2p plaque), the hurricane left/right (the EM-1 arrow points
    # up only), and the pictograms (Phase 3b).
    ('signspeed0', SHS(R, 11, replace=('50', '0')), 'R2-1 (0)'),
    ('signspeed5', SHS(R, 11, replace=('50', '5')), 'R2-1 (5)'),
    ('signspeed15', SHS(R, 11, replace=('50', '15')), 'R2-1 (15)'),
    ('signspeed20', SHS(R, 11, replace=('50', '20')), 'R2-1 (20)'),
    ('signspeed25', SHS(R, 11, replace=('50', '25')), 'R2-1 (25)'),
    ('signpostspeed30', SHS(R, 11, replace=('50', '30')), 'R2-1 (30)'),
    ('signspeed35', SHS(R, 11, replace=('50', '35')), 'R2-1 (35)'),
    ('signspeed40', SHS(R, 11, replace=('50', '40')), 'R2-1 (40)'),
    ('signspeed45', SHS(R, 11, replace=('50', '45')), 'R2-1 (45)'),
    ('signspeed65', SHS(R, 11, replace=('50', '65')), 'R2-1 (65)'),
    ('signspeed75', SHS(R, 11, replace=('50', '75')), 'R2-1 (75)'),
    ('signpost50min30', SHS(R, 19, replace=('55', '50')), 'R2-4a (50/30)'),   # 20 x 40 plate
    ('signhandicapreservedparking', SHS(R, 93, pick=3, palette=SLATE_TO_BLUE), 'R7-8'),
    ('signcurve15', SHS(W, 113, replace=('25', '15')), 'W13-5 (15)'),
    ('signcurve35', SHS(W, 113, replace=('25', '35')), 'W13-5 (35)'),
    ('signcurve45', SHS(W, 113, replace=('25', '45')), 'W13-5 (45)'),
    ('signramp15', SHS(W, 111, replace=('30', '15')), 'W13-3 (15)'),
    ('signramp25', SHS(W, 111, replace=('30', '25')), 'W13-3 (25)'),
    ('signramp35', SHS(W, 111, replace=('30', '35')), 'W13-3 (35)'),
    ('signtractor', SHS(W, 94), 'W11-5'),
    ('signhandicap', SHS(G, 64), 'D9-6'),
    ('signcrossoverquartermile', SHS(G, 91), 'D13-1'),
    # --- Phase 3b: the recreational pictograms, white on the mod's panel colours
    ('signcamping', SYM(G, 127, [0], 'blue'), 'RS-010'),
    ('signdog', SYM(G, 126, [0, 1], 'brown'), 'RS-030'),
    ('signseaplane', SYM(G, 126, [3], 'brown'), 'RS-080'),
    ('signhelicopter', SYM(G, 144, [0], 'brown'), 'RG-070'),
    ('signatv', SYM(G, 152, [2, 3, 4, 5, 6], 'brown'), 'RL-020'),
    ('signhangglider', SYM(G, 153, [2], 'brown'), 'RL-060'),
    ('signfishing', SYM(G, 156, [0, 1], 'brown'), 'RW-010'),
    ('signswimming', SYM(G, 160, [0], 'brown'), 'RW-080'),
    ('signkayak', SYM(G, 162, [0], 'brown'), 'RW-090'),
    ('signwindsurf', SYM(G, 162, [2], 'brown'), 'RW-120'),
    ('signskilift', SYM(G, 167, [2], 'brown'), 'RM-050'),
    ('signcamper', SYM(G, 135, [2, 3, 4], 'blue'), 'RA-130'),
    ('signboats', SYM(G, 156, [3], 'green'), 'RW-050'),   # the anchor (marina), as the mod draws it
    # signparkingnoarrow stays as drawn: it is one of a matching set with the arrow versions
    # --- Phase 5: the audit's unverified guesses, each checked against the page by eye and
    # kept only where the mod's sign IS that drawing (a custom number, lane or wording stays
    # as the mod drew it). Left/right twins of a legend sign use mirror_symbols. Not taken:
    # the arrow-shaped ONE WAY pair and the W16-5p arrow plaques (3:1 and 4:3 drawings on
    # 4.3:1 and 2:1 plates), the M6 arrows (the mod's are combined Y / T arrows), Weigh
    # Station Next Right (its page draws no white border).
    ('signbustaxionly', SHS(R, 57), 'R3-14b'),
    ('signcenterlaneturnsonly', SHS(R, 37), 'R3-9b'),
    ('signhov2ormorepervehicle', SHS(R, 53), 'R3-13'),
    ('signr105', SHS(R, 135), 'R10-5'),
    ('noparkingsundayholiday', SHS(R, 92), 'R7-3'),
    ('signnotrucksover7000', SHS(R, 161), 'R12-3'),
    ('signweightlimit2peraxle', SHS(R, 161, pick=1), 'R12-4'),
    ('signnonmotorprohibit', SHS(R, 83), 'R5-7'),
    ('signonewayright', SHS(R, 88), 'R6-2R'),
    ('signonewayleft', SHS(R, 88, mirror_symbols=True), 'R6-2L'),
    ('signstopherered', SHS(R, 136), 'R10-6'),   # straight arrow, as the mod drew both
    ('signstopherered2', SHS(R, 136, mirror_symbols=True), 'R10-6 (right)'),
    ('signstophereflashred2', SHS(R, 115, mirror_symbols=True), 'R8-10 (right)'),
    ('signnoparkinganytime', SHS(R, 91, mirror_symbols='both'), 'R7-1 (both ways)'),
    ('signnoturnred', SHS(R, 141), 'R10-11a'),
    ('signoturnonred', SHS(R, 142), 'R10-11b'),
    ('signonehrparking97', SHS(R, 93), 'R7-5 (1 hr)'),
    ('signrightturn', SHS(W, 0), 'W1-1R'),
    ('signleftturn', SHS(W, 0, pick=1), 'W1-1L'),
    ('signhairpinright', SHS(W, 10), 'W1-11R'),
    ('signhairpinleft', SHS(W, 10, mirror=True), 'W1-11L'),
    ('signloopright', SHS(W, 12), 'W1-15R'),
    ('signmergeleftlanends', SHS(W, 30), 'W4-2R'),
    ('signleftends', SHS(W, 30, pick=1), 'W4-2L'),
    ('signblastingzone', SHS(W, 159), 'W22-1'),
    ('rwrkbepreptostop', SHS(W, 25, palette={(255, 245, 0): shs.MOD_COLOURS['orange']}), 'W3-4 (TTC)'),
    ('rwrkflagger', SHS(W, 149), 'W20-7'),
    ('signworkdetourright', SHS(W, 172), 'M4-9R'),
    ('signworkdetouerleft', SHS(W, 172, mirror_symbols=True), 'M4-9L'),
    ('signturnoff2way', SHS(W, 160), 'W22-2'),
    ('signparkingarearight', SHS(G, 40), 'D5-4'),
    ('signrestarearight', SHS(G, 37, pick=1), 'D5-2a'),
    # --- Remaining-signs batch 1 (tab order from the top). Custom, left as drawn:
    # carelesspersonsign, signaheadleftright (three-headed), signbeginplaque (the book's BEGIN
    # is green), bearcrossingsign, beginfwysign / beginhwysign / beginpkwysign.
    ('signupright', SHS(G, 22, pick=2), 'M6-6R'),
    ('signupleft', SHS(G, 22, pick=2, mirror=True), 'M6-6L'),
    ('signaheadright', SHS(G, 22, pick=2), 'M6-6R'),
    ('signaheadleft', SHS(G, 22, pick=2, mirror=True), 'M6-6L'),
    ('signupslightright', SHS(G, 22, pick=4), 'M6-7R'),
    ('signupslightleft', SHS(G, 22, pick=4, mirror=True), 'M6-7L'),
    ('signahead', SHS(G, 21, pick=2), 'M6-3'),
    ('signaheadonly', SHS(R, 29), 'R3-5a'),
    ('signalternate', SHS(G, 15), 'M4-1a'),
    ('signalt', SHS(G, 15, pick=1), 'M4-1'),
    ('signbikelane', SHS(R, 60), 'R3-17'),
    ('signbikelanelarge', SHS(R, 60), 'R3-17'),
    ('signpostweightlimit', SHS(R, 163), 'R12-5'),
    # --- Remaining-signs batch 2. Left as drawn: the centre-lane hour panels
    # (signcenterlanebusonly69, signcenterhov6a9a, signcenterlanenouse79), signcityspeed35,
    # the danger / high-voltage set, signdividedhw1 / 2 (crossing arrows), signdontthinkparking,
    # signdownleftupright.
    ('signbypass', SHS(G, 15, pick=2), 'M4-2'),
    ('signbusiness', SHS(G, 15, pick=3), 'M4-3'),
    ('signend', SHS(G, 16, pick=1), 'M4-6'),
    ('signdoubleoneway', SHS(R, 87, pick=1), 'R6-1L'),
    ('signdoubleonewayb', SHS(R, 87), 'R6-1R'),
    ('signarrowdownright', SHS(G, 21, rotate_symbols=90), 'M6-2R (down)'),
    ('signarrowdownleft', SHS(G, 21, rotate_symbols=90, mirror=True), 'M6-2L (down)'),
    # --- Remaining-signs batch 3. Left as drawn: signexceptbus, signfdcstandpipe (NFPA),
    # forestryvehiclesonlysign, signhov6a9a, signhov2onlyoverhead, signhovahead / signhovends
    # (the book's R3-15a / R3-14 carry the "2+" the other way round), signhovrules,
    # kathieevanssign, signmetro, ladotantigridlockzone, ladotnostopping, signresidentlarge.
    ('signendplaque', SHS(G, 16, pick=1), 'M4-6'),
    ('signkeepright1', SHS(R, 69), 'R4-7b'),
    ('signkeepright2', SHS(R, 70), 'R4-7c'),
    ('signleftright', SHS(G, 21, pick=4), 'M6-4'),
    ('signaheadleftright', SHS(G, 23, pick=1), 'M6-5'),   # batch 1's leftover: it is on p23
    ('signleftahead', SHS(R, 28, pick=2), 'R3-6L'),
    ('signleftonly', SHS(R, 28), 'R3-5L'),
    # --- Remaining-signs batch 4. Left as drawn: signltyofy (flashing yellow arrow, not in
    # the book), lhsstopsign (deliberately non-compliant), signlitteringillegal,
    # signpostmbtalogo, signnodumping, signnobridgefishing, noforestparkingsign,
    # noovernightparkingsign, signnoovernightparking, noparkingeairssign, noparkinginalleysign,
    # noparkingonbridgesign (custom text on R8-style panels), signnoleftred.
    ('signleft', SHS(G, 20, pick=4, mirror=True), 'M6-1L'),
    ('signnobikes', SHS(R, 82), 'R5-6'),
    ('signnopedestrians', SHS(R, 118), 'R9-3a'),
    ('signnohitchhiker', SHS(R, 121), 'R9-4a'),
    ('signhm', SHS(R, 168), 'R14-2'),
    ('signnohm', SHS(R, 169), 'R14-3'),
    ('noparking830530', SHS(R, 91, pick=1, mirror_symbols='both'), 'R7-2 (both ways)'),
    ('noparkinglogo830530', SHS(R, 91, pick=2), 'R7-2a'),
    # --- Remaining-signs batch 5. Left as drawn: signnorightred, signnotrucksleftlane,
    # signnoturnsofficialonly, signonbridge, signonpavement, signonecarpergreen(eachlane),
    # signresidentnormal, signphotoenforced / signredlightphoto (the book's R10-18 / R10-19
    # are text only; the mod's carry a signal head), positivelynosmokingsign, signfine400,
    # signpostreduced30, signpostreducedspeedahead (no R2-5 in the book), restrictedareasign.
    ('nostandingsign', SHS(R, 92, pick=1), 'R7-4'),
    ('onewaytlsignright', POINTED_ONE_WAY(left=False), 'R6-1R (pointed)'),
    # --- Remaining-signs batch 6. Left as drawn: rightlanefreewayonlysign, signrightplaque
    # (text, as signleftplaque), the four school street sweeping signs, signdontblockthebox,
    # snownotremovedsign, signpostspeedzoneahead (the book's W3-5a is a yellow diamond).
    ('signstatepropertynotrasspassing', STATE_PROPERTY(), 'California state property (photo)'),
    ('signrightahead', SHS(R, 28, pick=3), 'R3-6R'),
    ('signrightonly', SHS(R, 28, pick=1), 'R3-5R'),
    ('signright', SHS(G, 20, pick=4), 'M6-1R'),
    ('signr105a', SHS(R, 135, replace=('LEFT ON', 'RIGHT ON'), condense=True), 'R10-5 (right)'),
    ('signaheadsharpleft', SHS(G, 20), 'M5-1L'),
    ('signaheadsharpright', SHS(G, 20, mirror=True), 'M5-1R'),
    ('signaheadslightleft', SHS(G, 20, pick=2), 'M5-2L'),
    ('signaheadslightright', SHS(G, 20, pick=2, mirror=True), 'M5-2R'),
    ('signslightright', SHS(G, 21), 'M6-2R'),
    ('signslightleft', SHS(G, 21, mirror=True), 'M6-2L'),
    # --- Remaining-signs batch 7. Left as drawn: signgatecode, extremeheatdangersign,
    # signstopherepedleft / right (R1-5b/c are 2009 signs: neither the book nor an interim ZIP),
    # stopbridgeclearancesign, the five street sweeping signs, doorsunlockedbiz,
    # shouldertravelongreenarrowsign, signturnflashred, utilityvehiclesonlysign.
    ('signto', SHS(G, 16, pick=2), 'M4-5'),
    ('signtruckhalf', SHS(G, 16), 'M4-4'),
    ('signposttruck40', SHS(R, 13), 'R2-2'),
    ('signturnsonly', SHS(R, 36), 'R3-9a'),
    ('signupleftdownright', SHS(G, 21, pick=4, rotate_symbols=35), 'M6-4 (turned)'),
    ('signdownleftupright', SHS(G, 21, pick=4, rotate_symbols=-35), 'M6-4 (turned)'),
    ('deadlyforcesign', TWO_PANEL('WARNING', ('BEYOND THIS POINT', 'DEADLY FORCE', 'IS AUTHORIZED'), 'white'),
     'W13-3 layout, white'),
    # --- Remaining-signs batch 8. Left as drawn: verizondig (utility placard),
    # basestationradiosign (white over yellow, landscape), signcautiondriveslowly.
    ('sign14_4', SHS(W, 105, replace=[('12', '14'), ('-6', ' -4')], condense='box'), 'W12-2 (14-4)'),
    ('sign3wayt', SHS(W, 16), 'W2-4'),
    ('sign4way', SHS(W, 13), 'W2-1'),
    ('signcow', SHS(W, 93), 'W11-4'),
    ('signpostcurvyroad', SHS(W, 5), 'W1-5'),
    ('signfiretruck', SHS(W, 98), 'W11-8'),
    ('fallhazzardsign', TWO_PANEL('WARNING', ('FALL HAZARD AREA', 'DO NOT ENTER'), 'white'), 'W13-3 layout, white'),
    ('earprotectionsign', TWO_PANEL('CAUTION', ('EAR PROTECTION', 'REQUIRED BEYOND', 'THIS POINT')), 'W13-3 layout'),
    ('switchequipmentwarningsign', TWO_PANEL('CAUTION', ('REMOTE CONTROLLED', 'EQUIPMENT MAY OPERATE',
                                                         'AT ANY TIME', 'KEEP CLEAR OF', 'MOVING PARTS')), 'W13-3 layout'),
    ('absolutelynothingsign', TEXT_DIAMOND(['ABSOLUTELY', 'NOTHING']), 'W8-1 diamond'),
    ('signduststor', TEXT_DIAMOND(['OCCASIONAL', 'BLINDING', 'DUST', 'STORMS'], tight=True), 'W8-1 diamond'),
    ('cautiondriveways', TEXT_DIAMOND(['CAUTION', 'DRIVEWAYS']), 'W8-1 diamond'),
    ('dangerousroadcurves', TEXT_DIAMOND(['DANGEROUS', 'ROAD CURVES']), 'W8-1 diamond'),
    ('endcountymaintainedroadsign', TEXT_DIAMOND(['END', 'COUNTY', 'MAINTAINED', 'ROAD']), 'W8-1 diamond'),
    ('roadend', TEXT_DIAMOND(['END']), 'W8-1 diamond'),
    ('faweincidentsign', TEXT_DIAMOND(['FAWE', 'INCIDENT', 'AHEAD'], INCIDENT_PINK), 'W8-1 diamond, magenta'),
    ('fwyintersectionsign', TEXT_DIAMOND(['FREEWAY', 'INTERSECTION', 'AHEAD']), 'W8-1 diamond'),
    # --- Remaining-signs batch 9. Left as drawn: calaneendsignleft / right (a diagonal arrow
    # on a diamond, no drawing), signleftlaneends (LANE ENDS between two arrows).
    ('signhardleftshift', SHS(W, 3, pick=1), 'W1-3L'),
    ('signhardrightshift', SHS(W, 3), 'W1-3R'),
    ('signleftshift', SHS(W, 4, pick=1), 'W1-4L'),
    ('signleftcurve', SHS(W, 2, inner=2, mirror=True), 'W1-2L'),
    ('signleftarrow', SHS(W, 6, pick=1), 'W1-6L'),
    ('signleftrightarrow', SHS(W, 7), 'W1-7'),
    ('signleftchevron', SHS(W, 8, pick=1), 'W1-8L'),
    ('sign3left', SHS(W, 14, pick=1), 'W2-2L'),
    ('signyleft', SHS(W, 15, pick=1), 'W2-3L'),
    ('signmergeleft', SHS(W, 29), 'W4-1R'),
    ('signmergeright', SHS(W, 29, pick=1), 'W4-1L'),
    ('signtrainleft', SHS(W, 77, pick=1), 'W10-3L'),
    ('signtrainright', SHS(W, 77), 'W10-3R'),
    ('signhwintersection', TEXT_DIAMOND(['HIGHWAY', 'INTERSECTION', 'AHEAD']), 'W8-1 diamond'),
    ('landslidearea', TEXT_DIAMOND(['LANDSLIDE', 'AREA']), 'W8-1 diamond'),
    ('massdotheavymergesign', TEXT_DIAMOND(['HEAVY', 'MERGE', 'AHEAD']), 'W8-1 diamond'),
    ('massdotsidestreettrafficsign', TEXT_DIAMOND(['PLEASE', 'SHOW', 'COURTESY', 'TO SIDE', 'STREET', 'TRAFFIC'],
                                                  shs.MOD_COLOURS['blue'], ink=shs.MOD_COLOURS['white']),
     'W8-1 diamond, blue'),
    # --- Remaining-signs batch 10. Left as drawn: signnarrowbridgeimg (the symbol is not in
    # the 2004 book, only the worded W5-2), signradioradiation (FCC placard), signrightlaneends.
    ('sign3right', SHS(W, 14), 'W2-2R'),
    ('signrightarrow', SHS(W, 6), 'W1-6R'),
    ('signrightchevron', SHS(W, 8), 'W1-8R'),
    ('signrightcurve', SHS(W, 2, inner=2), 'W1-2R'),
    ('signrightshift', SHS(W, 4), 'W1-4R'),
    ('signyright', SHS(W, 15), 'W2-3R'),
    ('limitedmaintroadsign', TEXT_DIAMOND(['MINIMUM', 'MAINTENANCE', 'ROAD']), 'W8-1 diamond'),
    ('signnewsignal', TEXT_DIAMOND(['NEW', 'SIGNAL', 'AHEAD']), 'W8-1 diamond'),
    ('nofwyaccesssign', TEXT_DIAMOND(['NO', 'FREEWAY', 'ACCESS']), 'W8-1 diamond'),
    ('noguardrailssign', TEXT_DIAMOND(['NO', 'GUARDRAILS'], shift=True), 'W8-1 diamond'),
    ('nohwyaccesssign', TEXT_DIAMOND(['NO', 'HIGHWAY', 'ACCESS']), 'W8-1 diamond'),
    ('nopkwyaccesssign', TEXT_DIAMOND(['NO', 'PARKWAY', 'ACCESS']), 'W8-1 diamond'),
    ('parkwayintersectionsign', TEXT_DIAMOND(['PARKWAY', 'INTERSECTION', 'AHEAD']), 'W8-1 diamond'),
    ('plantentrancesign', TEXT_DIAMOND(['PLANT', 'ENTRANCE']), 'W8-1 diamond'),
    ('signrampsignalahead', TEXT_DIAMOND(['RAMP', 'SIGNAL', 'AHEAD']), 'W8-1 diamond'),
    ('signroadends', TEXT_DIAMOND(['ROAD', 'ENDS']), 'W8-1 diamond'),
    ('signhightideroadflood', TEXT_DIAMOND(['ROAD', 'FLOODS', 'DURING', 'HIGH TIDE'], tight=True, gap=1.3, min_condense=0.68,
                                                    shift=True), 'W8-1 diamond'),
    # --- Remaining-signs batch 11. Left as drawn: notmaintainedroadsign (a yellow placard of
    # small text), signroadsplit (split arrows, no drawing), signslowdangerousintersection,
    # signloookbothways, signtrolley (the book's W10-7 is the black light-rail panel).
    ('signslippery', SHS(W, 62), 'W8-5'),
    ('signroundabout', SHS(W, 18), 'W2-6'),
    ('signtruckroll', SHS(W, 11), 'W1-13'),
    ('signtruck', SHS(W, 100), 'W11-10'),
    ('rualintersectionsign', TEXT_DIAMOND(['RURAL', 'INTERSECTION', 'AHEAD'], shift=True), 'W8-1 diamond'),
    ('ruralroadsign', TEXT_DIAMOND(['RURAL', 'ROAD'], shift=True), 'W8-1 diamond'),
    ('signseverestorm', TEXT_DIAMOND(['SEVERE', 'STORM', 'AREA'], shift=True), 'W8-1 diamond'),
    ('slowtruckssign', TEXT_DIAMOND(['SLOW', 'TRUCKS'], shift=True), 'W8-1 diamond'),
    ('signspeedhump', TEXT_DIAMOND(['SPEED', 'BUMP'], shift=True), 'W8-1 diamond'),
    ('thicklysettledsign', TEXT_DIAMOND(['THICKLY', 'SETTLED'], shift=True), 'W8-1 diamond'),
    ('signtrafficislands', TEXT_DIAMOND(['TRAFFIC', 'ISLANDS', 'AHEAD'], shift=True), 'W8-1 diamond'),
    ('truckturnaroundsign', TEXT_DIAMOND(['TRUCK', 'TURNAROUND', 'AHEAD'], shift=True), 'W8-1 diamond'),
    ('watchdownhillspeedsign', TEXT_DIAMOND(['WATCH', 'DOWNHILL', 'SPEED'], shift=True), 'W8-1 diamond'),
    ('signwatchemergency', TEXT_DIAMOND(['WATCH', 'FOR', 'EMERGENCY', 'VEHICLES'], tight=True, gap=1.3, min_condense=0.68, shift=True), 'W8-1 diamond'),
    ('signlowaircraft', TEXT_DIAMOND(['WATCH', 'FOR', 'LOW FLYING', 'AIRCRAFT'], tight=True, gap=1.3, min_condense=0.68, shift=True), 'W8-1 diamond'),
    # --- Remaining-signs batch 12. Left as drawn: signbikelaneplaque (bike + LANE; the book's
    # R3-17 is the black BIKE LANE panel), signbikesallowedusefulllane / large (square plate:
    # the R9-5 panel's bike would stretch half as wide again), signbusstopahead (the symbol
    # S3-1 is 2009; the book's is worded), signexceptbicycleicon (EXCEPT over a bike).
    ('signyintersection', SHS(W, 17), 'W2-5'),
    ('signaheadplaquefloyellow', SHS(W, 126, pick=1, palette=FYG_FACE), 'W16-9p (FYG)'),
    ('signarrowplaquefloyellowdownleft', SHS(W, 125, palette=FYG_FACE), 'W16-7pL (FYG)'),
    ('signarrowplaquefloyellowdownright', SHS(W, 125, pick=1, palette=FYG_FACE), 'W16-7pR (FYG)'),
    ('signbeginrightlaneyieldbikes', SHS(R, 65), 'R4-4'),
    ('signbeginleftlaneyieldbikes', SHS(R, 65, replace=('RIGHT TURN LANE', 'LEFT TURN LANE'), mirror_symbols=True,
                                        condense=True), 'R4-4 (left)'),
    ('bikesusepedsignalsign', SHS(R, 122), 'R9-5'),
    ('signbikeyieldtopeds', SHS(R, 123), 'R9-6'),
    ('signbikelaneahead', TEXT_PANEL(R, 122, ['LANE', 'AHEAD'], band=(0.42, 0.92), cap_max=0.14, width=0.76), 'R9-5 panel'),
    ('signbikelaneends', TEXT_PANEL(R, 122, ['LANE', 'ENDS'], band=(0.42, 0.92), cap_max=0.14, width=0.76), 'R9-5 panel'),
    ('signbikesignal', TEXT_PANEL(R, 122, ['SIGNAL'], band=(0.5, 0.9), cap_max=0.2, width=0.8), 'R9-5 panel'),
    ('campgroundcrossingsign', TEXT_DIAMOND(['CAMPGROUND', 'CROSSINGS'], shs.MOD_COLOURS['fyg'], shift=True),
     'W8-1 diamond, FYG'),
    ('signexceptbicycle', TEXT_PANEL(R, 60, ['EXCEPT', 'BICYCLES'], pick=1, band=(0.14, 0.86)), 'R3-17aP plaque'),
    ('signexceptbusbicycle', TEXT_PANEL(R, 60, ['EXCEPT BUS', '& BICYCLES'], pick=1, band=(0.14, 0.86)),
     'R3-17aP plaque'),
    # --- Remaining-signs batch 13. Left as drawn: hikersaheadsign (no hiker symbol in the
    # book), signleftbikerightpark, mivehiclessharecenterlanesign, paytocrosssign,
    # signrightlanebikeonly, signslowdownpedestriantraffic, signslowschool,
    # thicklysettledspeedlimit25mphsign, the six turning-vehicles-yield signs (the book's R10-15
    # is text only; the mod's are the later symbol versions), schoolsafetyzonesign,
    # streetsweepmonschool, 99onlypricesignold.
    ('signusecrosswalkright', SHS(R, 118, pick=1), 'R9-3bP'),
    ('signusecrosswalkleft', SHS(R, 118, pick=1, rotate_symbols=180), 'R9-3bP (left)'),
    ('trailheadcrossingssign', TEXT_DIAMOND(['TRAILHEAD', 'CROSSINGS'], shs.MOD_COLOURS['fyg'], shift=True),
     'W8-1 diamond, FYG'),
    # --- Remaining-signs batch 14. Left as drawn: safetyglassesandfaceshieldsign (facility
    # placard), castraightdetoursign / freewaydetoursign (DETOUR over an arrow; the book's W20-2
    # page carries construction lines), signworkpulloffleft / right.
    ('signallmergeleft', SHS(W, 138, replace=('THRU', 'ALL'), palette=ORANGE_FACE), 'W4-7 (ALL, orange)'),
    ('signallmergeright', SHS(W, 138, pick=1, replace=('THRU', 'ALL'), palette=ORANGE_FACE), 'W4-7 (ALL, orange)'),
    ('endlandworksign', SHS(W, 168, pick=2, replace=('ROAD WORK', 'LAND WORK'), condense=True), 'G20-2 (LAND)'),
    ('landworkaheadsign', TEXT_DIAMOND(['LAND', 'WORK', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('alwaysroadworksign', TEXT_DIAMOND(['ALWAYS', 'ROAD WORK', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('beachclosedsign', TEXT_DIAMOND(['BEACH', 'CLOSED'], shift=True), 'W8-1 diamond'),
    ('dangerousroadcurvesrr', TEXT_DIAMOND(['DANGEROUS', 'ROAD CURVES'], shs.MOD_COLOURS['orange']), 'W8-1 diamond, orange'),
    ('caltransduicheckpointaheadsign', TEXT_DIAMOND(['DUI /', "DRIVER'S LICENSE", 'CHECK POINT', 'AHEAD'], INCIDENT_PINK,
                                                    tight=True, gap=1.3, min_condense=0.68, shift=True),
     'W8-1 diamond, incident pink'),
    ('buildathoneventsign', TEXT_DIAMOND(['HUGE', 'BUILDATHON', 'EVENT', 'AHEAD'], shs.MOD_COLOURS['orange'], tight=True, gap=1.3,
                                         min_condense=0.68, shift=True), 'W8-1 diamond, orange'),
    ('massdotheavymergesignrw', TEXT_DIAMOND(['HEAVY', 'MERGE', 'AHEAD'], shs.MOD_COLOURS['orange']), 'W8-1 diamond, orange'),
    ('noguardrailssignrr', TEXT_DIAMOND(['NO', 'GUARDRAILS'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('signrampclosedahead', TEXT_DIAMOND(['RAMP', 'CLOSED', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('rgraheadsign', TEXT_DIAMOND(['RGR', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('rgrbabysign', TEXT_DIAMOND(['RGR', 'BABY', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('rgrchickensign', TEXT_DIAMOND(['RGR', 'CHICKEN', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    # --- Remaining-signs batch 15. Left as drawn: seniorsafetyzonesign.
    ('signworkexitleft', PANEL(['EXIT'], arrow='left', band=(0.08, 0.5), cap_max=0.2), 'orange panel + M6-2 arrow'),
    ('signworkexitright', PANEL(['EXIT'], arrow='right', band=(0.08, 0.5), cap_max=0.2), 'orange panel + M6-2 arrow'),
    ('conezonesign', PANEL(['SLOW FOR', 'THE CONE', 'ZONE'], band=(0.1, 0.9), width=0.76), 'orange panel'),
    ('rwrkshiftleft2lanes', SHS(W, 136, pick=1), 'W1-4bL'),
    ('rwrkshiftright2lanes', SHS(W, 136), 'W1-4bR'),
    ('signrwrkshiftleftsingle', SHS(W, 4, pick=1, palette=ORANGE_FACE), 'W1-4L (orange)'),
    ('signrwrkshiftrightsingle', SHS(W, 4, palette=ORANGE_FACE), 'W1-4R (orange)'),
    ('rwrksignalahead', SHS_PANEL_COLOUR(W, 23, 'orange'), 'W3-3 (orange)'),
    ('rwrkstopahead', SHS(W, 19, palette=ORANGE_FACE), 'W3-1 (orange)'),
    ('rwrklowshoulder', SHS(W, 66, palette=ORANGE_FACE), 'W8-9 (orange)'),
    ('signpeddetourleft', SHS(W, 174), 'M4-9b'),
    ('signpeddetourright', SHS(W, 174, mirror_symbols=True), 'M4-9b (right)'),
    ('signpostroadwork', TEXT_DIAMOND(['ROAD', 'WORK', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('roadendsinwatersign', TEXT_DIAMOND(['ROAD', 'ENDS IN', 'WATER'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('signrworkfinesdouble', TEXT_DIAMOND(['TRAFFIC', 'FINES DOUBLED', 'IN WORK', 'ZONES'], shs.MOD_COLOURS['orange'], tight=True, gap=1.3, min_condense=0.68, shift=True),
     'W8-1 diamond, orange'),
    ('rwrknewtrafficpatternsign', TEXT_DIAMOND(['NEW', 'TRAFFIC', 'PATTERN', 'AHEAD'], shs.MOD_COLOURS['orange'], tight=True, gap=1.3, min_condense=0.68, shift=True),
     'W8-1 diamond, orange'),
    ('rwrknoshouldersign', TEXT_DIAMOND(['NO', 'SHOULDER'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('signsignalworkahead', TEXT_DIAMOND(['SIGNAL', 'WORK', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('specialeventsign', TEXT_DIAMOND(['SPECIAL', 'EVENT', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    # --- Remaining-signs batch 16. Left as drawn: 1hrtruckparkingsign, sign24hrparking,
    # signcrossoverleft (green panel with a horizontal arrow), signparkingnoarrow (the D4-1 is
    # drawn with its arrow), signpostca_pch (California route shield); signhm was Phase 4.
    ('signstreetworkahead', TEXT_DIAMOND(['STREET', 'WORK', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('signunmarkedpavement', TEXT_DIAMOND(['UNMARKED', 'PAVEMENT', 'AHEAD'], shs.MOD_COLOURS['orange'], shift=True), 'W8-1 diamond, orange'),
    ('signworkturnlaneleft', PANEL(['TURN', 'LANE'], arrow='left', band=(0.06, 0.52), cap_max=0.17), 'orange panel + M6-2 arrow'),
    ('signworkturnlaneright', PANEL(['TURN', 'LANE'], arrow='right', band=(0.06, 0.52), cap_max=0.17), 'orange panel + M6-2 arrow'),
    ('twohourpark830530', SHS(R, 98), 'R7-108'),
    ('signjct', SHS(G, 12), 'M2-1'),
    ('signhiking', SYM(G, 149, [0, 1, 2], 'green'), 'RS-? hiking (green)'),
    ('hwyentrance', PANEL(['HIGHWAY', 'ENTRANCE'], 'green', ink='white', band=(0.2, 0.8)), 'green panel'),
    ('pkwyentrancesign', PANEL(['PARKWAY', 'ENTRANCE'], 'green', ink='white', band=(0.2, 0.8)), 'green panel'),
    ('signparkingright', SHS(G, 31, pick=2, replace=('ARKING', 'ARKING'), condense=True,
                                legend_colour=(0, 145, 64, 255)), 'D4-1 (up right)'),
    ('signparkingr', SHS(G, 31, pick=2, rotate_symbols=(45, 0.65), replace=('ARKING', 'ARKING'), condense=True,
                                legend_colour=(0, 145, 64, 255)), 'D4-1 (right)'),
    ('signparkingahead', SHS(G, 31, pick=2, rotate_symbols=-45, replace=('ARKING', 'ARKING'), condense=True,
                                legend_colour=(0, 145, 64, 255)), 'D4-1 (ahead)'),
    ('signparkingleft', SHS(G, 31, pick=2, rotate_symbols=(-90, 0.85), replace=('ARKING', 'ARKING'), condense=True,
                                legend_colour=(0, 145, 64, 255)), 'D4-1 (up left)'),
    ('signparkingl', SHS(G, 31, pick=2, rotate_symbols=(-135, 0.65), replace=('ARKING', 'ARKING'), condense=True,
                                legend_colour=(0, 145, 64, 255)), 'D4-1 (left)'),
    # --- Remaining-signs batch 17. Left as drawn: signsnowflake, signvisitornolongtermparking,
    # 99onlypricesignnew, signambulance (the Star of Life is not in the book; D9-13 is the H),
    # signbluestop (deliberately non-compliant), buslanesign, signeisenhower and the two fallout
    # shelter signs (reviewed in Phase 3), hgblissgreenhwysign (artwork), ladotsignalsync.
    ('signfood', SHS(G, 66), 'D9-8'),
    ('signlodging', SHS(G, 67), 'D9-9'),
    ('signinformation', SHS(G, 68), 'D9-10'),
    ('signdiesel', SHS(G, 69), 'D9-11'),
    ('signweighstationright', SHS(G, 56), 'D8-3'),
    ('signweighstationnextright', PANEL(['WEIGH', 'STATION', 'NEXT RIGHT'], 'green', ink='white', band=(0.1, 0.9), width=0.78),
     'green panel (D8-2 legend)'),
    ('landareastudysign', PANEL(['LAND AREA', 'UNDER STUDY'], 'blue', ink='white', band=(0.2, 0.8), width=0.76), 'blue panel'),
    ('altowelcomesyousign', PANEL(['ALTO', 'WELCOMES YOU', 'ESTABLISHED IN 2020'], 'blue', ink='white',
                                  band=(0.22, 0.78), cap_max=0.1), 'blue panel'),
    ('respectuianatparkssign', PANEL(['PLEASE', 'RESPECT ALL UIA', 'NATIONAL PARKS', 'AND PROPERTY', 'FOR EVERYONE',
                                      'UIA CODES APPLY'], 'white', ink='green', band=(0.08, 0.92), width=0.76), 'white panel'),
    ('onewaytlsignleft', POINTED_ONE_WAY(left=True), 'R6-1L (pointed)'),
    ('signpostonewayright', SHS(R, 87), 'R6-1R'),
    ('signpostonewayleft', SHS(R, 87, pick=1), 'R6-1L'),
    ('signhurricane', SHS(E, 0), 'EM-1'),   # the three moved to the 24 x 24 square plate
    ('signhurricaneleft', SHS(E, 0, rotate_symbols=-90), 'EM-1 (left)'),
    ('signhurricaneright', SHS(E, 0, rotate_symbols=90), 'EM-1 (right)'),
    ('signbridgeice', SHS(W, 71), 'W8-13'),   # renamed Bridge Ices Before Road
]


# ----------------------------------------------------------------------------- the sign's own plate

def _asset_json(relative_path):
    path = layout.resolve_asset(relative_path)
    if path is None:
        raise SystemExit('missing asset %s' % relative_path)
    with open(path, encoding='utf-8') as fh:
        return json.load(fh)


def plate_aspect(model_ref):
    """Width / height of the plate face the sign texture (slot ``1``) is painted on, read off
    the model's elements: the largest vertical face textured ``#1``."""
    name = model_ref.split(':', 1)[1]
    model = _asset_json('models/block/%s.json' % name)
    best = None
    for el in model.get('elements', []):
        f, t = el['from'], el['to']
        dims = [abs(t[i] - f[i]) for i in range(3)]
        for side, face in el.get('faces', {}).items():
            if face.get('texture') != '#1' or side in ('up', 'down'):
                continue
            w = dims[0] if side in ('north', 'south') else dims[2]
            h = dims[1]
            if best is None or w * h > best[0] * best[1]:
                best = (w, h)
    if best is None:
        raise SystemExit('%s has no face textured #1' % model_ref)
    return best[0] / float(best[1])


def sign_info(registry):
    """Where the sign's face texture lives and the plate it is stretched onto."""
    bs_path = layout.blockstate_file(registry)
    if bs_path is None:
        raise SystemExit('no blockstate for %s' % registry)
    with open(bs_path, encoding='utf-8') as fh:
        bs = json.load(fh)
    defaults = bs['defaults']
    textures = defaults['textures']
    face_ref = textures['1']
    back_ref = textures.get('2')

    def tex_path(ref):
        rel = 'textures/' + ref.split(':', 1)[1] + '.png'
        path = layout.resolve_asset(rel)
        return path or layout.asset_for_write(layout.owner_of(registry), rel)

    return {
        'model': defaults['model'],
        'aspect': plate_aspect(defaults['model']),
        'texture': tex_path(face_ref),
        'back': tex_path(back_ref) if back_ref and back_ref.endswith('_back') else None,
    }


def render(source, info):
    face, mirror, palette = source()
    if isinstance(face, SymbolFace):
        return shs.symbol_on_panel(face.symbol, face.colour, info['aspect'], size=256)
    if isinstance(face, ComposedFace):
        return face.fn(info['aspect'])
    # a silhouette sign (one with a _back texture) IS its outline: never squash it to fill
    # a 2:1 plaque squishes its legend to 8 px per plate unit across at 128; 256 keeps it
    # readable a few blocks away
    return shs.official_face(face, info['aspect'], mirror, palette,
                             size=256 if info['aspect'] >= 1.8 else shs.DEFAULT_TEX,
                             stretch_tol=0.0 if info['back'] else None)


# ----------------------------------------------------------------------------- sheets

def _at_aspect(img, aspect, h=150, max_w=200):
    """The square texture stretched back to its plate, fitted in ``max_w`` x ``h``."""
    w = int(round(h * aspect))
    if w > max_w:
        w, h = max_w, int(round(max_w / aspect))
    return img.convert('RGBA').resize((w, h), Image.LANCZOS)


def contact_sheet(entries, out, cols=3):
    """Before / after per entry: the current texture and the new face, both stretched to
    the plate, captioned with the registry and the SHS code."""
    cell_w, cell_h = 420, 200
    rows = (len(entries) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * cell_w, rows * cell_h), (60, 60, 70, 255))
    d = ImageDraw.Draw(sheet)
    for i, (registry, source, code) in enumerate(entries):
        info = sign_info(registry)
        x0, y0 = (i % cols) * cell_w, (i // cols) * cell_h
        before = _at_aspect(Image.open(info['texture']), info['aspect']) \
            if os.path.exists(info['texture']) else None
        after = _at_aspect(render(source, info), info['aspect'])
        if before is not None:
            sheet.alpha_composite(before, (x0 + 10 + (200 - before.width) // 2, y0 + 10))
        sheet.alpha_composite(after, (x0 + 210 + (200 - after.width) // 2, y0 + 10))
        d.text((x0 + 10, y0 + 170), '%s  <-  %s' % (registry, code), fill=(235, 235, 235, 255))
        d.text((x0 + 10, y0 + 184), os.path.basename(info['texture']), fill=(170, 170, 170, 255))
    sheet.save(out)
    return sheet.size


_ROW_RE = re.compile(r'^\| (\S+) \| (.*?) \| (EXACT|NEAR|NONE)\? \| (\w+) p(\d+)\s*([^|]*)\| (.*?) \|$')


def verify_sheets(prefix, per_sheet=8, categories=('EXACT', 'NEAR')):
    """Every ``?`` row of the match table in ``categories`` that cites a page and is not yet
    catalogued: current texture beside the page thumbnail, so a human can confirm or reject
    the guess."""
    rows = []
    done = {e[0] for e in CATALOGUE}
    with open(MATCH_TABLE, encoding='utf-8') as fh:
        for line in fh:
            m = _ROW_RE.match(line.rstrip('\n'))
            if m and m.group(3) in categories and m.group(1) not in done:
                rows.append(m.groups())
    print('%d ? rows with a cited page' % len(rows))
    cell_w, cell_h = 640, 330
    for s in range(0, len(rows), per_sheet):
        chunk = rows[s:s + per_sheet]
        sheet = Image.new('RGBA', (2 * cell_w, ((len(chunk) + 1) // 2) * cell_h), (60, 60, 70, 255))
        d = ImageDraw.Draw(sheet)
        for i, (registry, display, cat, chapter, page, code, note) in enumerate(chunk):
            x0, y0 = (i % 2) * cell_w, (i // 2) * cell_h
            try:
                info = sign_info(registry)
                cur = _at_aspect(Image.open(info['texture']), info['aspect'], h=200)
                sheet.alpha_composite(cur, (x0 + 10 + (230 - cur.width) // 2, y0 + 10))
            except SystemExit as e:
                d.text((x0 + 10, y0 + 100), str(e), fill=(255, 120, 120, 255))
            pix = shs.book_page(chapter, int(page)).get_pixmap(dpi=28)
            thumb = Image.frombytes('RGB', (pix.width, pix.height), pix.samples).convert('RGBA')
            sheet.alpha_composite(thumb, (x0 + 250, y0 + 10))
            d.text((x0 + 10, y0 + 230), '%s (%s?)' % (registry, cat), fill=(235, 235, 235, 255))
            d.text((x0 + 10, y0 + 246), display[:60], fill=(200, 200, 200, 255))
            d.text((x0 + 10, y0 + 262), '%s p%s %s' % (chapter, page, code.strip()), fill=(200, 200, 200, 255))
            d.text((x0 + 10, y0 + 278), note[:90], fill=(160, 160, 160, 255))
        out = '%s_%02d.png' % (prefix, s // per_sheet)
        sheet.save(out)
        print('wrote', out)


# ----------------------------------------------------------------------------- main

def main():
    args = sys.argv[1:]

    def opt(name):
        if name in args:
            i = args.index(name)
            return args[i + 1]
        return None

    if '--verify-sheet' in args:
        verify_sheets(opt('--verify-sheet'))
        return

    only = opt('--only')
    entries = CATALOGUE if not only else [e for e in CATALOGUE if e[0] in only.split(',')]
    if not entries:
        raise SystemExit('nothing selected')

    sheet = opt('--sheet')
    if sheet:
        print('sheet %s %s' % (sheet, contact_sheet(entries, sheet)))
        return

    check = '--check' in args
    drift = []
    for registry, source, code in entries:
        info = sign_info(registry)
        face = render(source, info)
        targets = [(info['texture'], face)]
        if info['back']:
            targets.append((info['back'], shs.back_texture(face)))
        for path, img in targets:
            if check:
                cur = Image.open(path).convert('RGBA') if os.path.exists(path) else None
                if cur is None or cur.size != img.size or cur.tobytes() != img.tobytes():
                    drift.append(path)
            else:
                img.save(path)
        if not check:
            print('wrote %s <- %s (%s, plate %.2f)' % (
                os.path.basename(info['texture']), code, registry, info['aspect']))
    if check:
        if drift:
            print('DRIFT: %d texture(s) differ from the generator:' % len(drift))
            for p in drift:
                print('  ' + p)
            sys.exit(1)
        print('all %d official faces match the generator' % len(entries))


if __name__ == '__main__':
    main()
