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

    def __init__(self, fn, extras=None):
        # extras(aspect) -> {texture file name beside the face: image}, for a sign whose
        # blockstate swaps in a second face (the lit state of a blank-out sign)
        self.fn, self.extras = fn, extras


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
    """One FHWA series for a block of lines at cap height ``cap``: the widest of D, C, B in which
    every line fits, so the lines read as one series rather than each picking its own."""
    return shs.pick_series(texts, cap, max_w, ('D', 'C', 'B'))


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
        margin = 0.5 if shift else (0.1 if tight else 0.25)
        offsets = [R * i / 40.0 for i in range(-12, 13)] if shift else [0.0]
        # the alphabets a block may drop to: a tight block (or a low min_condense) reaches Series B
        allowed = ('D', 'C', 'B') if (tight or MIN_CONDENSE < 0.7) else ('D', 'C')
        def fit(series):
            unit = [shs.legend_width(t, series, 1000) / 1000.0 for t in lines]   # width per cap
            best = None
            for dy in sorted(offsets, key=abs):
                cap = 0.24 * S
                while cap > 8 and (best is None or cap > best[0]):
                    lh = cap * GAP
                    if n * lh + 2 * abs(dy) <= 2 * R * 0.9:
                        # measured ``margin`` caps out from the line's middle: by default the
                        # corners of a line's end letters may run a little toward the border, as
                        # on real signs; a shifted block keeps the whole cap clear
                        if all(u * cap <= 2 * (R - abs((k - (n - 1) / 2.0) * lh + dy) - cap * margin)
                               for k, u in enumerate(unit)):
                            best = (cap, dy)
                            break
                    cap -= 2
            return best
        # the widest alphabet unless a narrower one buys a tenth more height
        fits = [(s_, fit(s_)) for s_ in allowed]
        fits = [(s_, f_) for s_, f_ in fits if f_]
        series, (cap, dy) = fits[0]
        for s_, f_ in fits[1:]:
            if f_[0] > 1.1 * cap:
                series, (cap, dy) = s_, f_
        lh = cap * GAP
        for k, t in enumerate(lines):
            cy = S / 2 + (k - (n - 1) / 2.0) * lh + dy
            gg._legend_line(img, t, cy, cap, S, colour=ink or shs.MOD_COLOURS['black'], condense=series)
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


def PANEL(lines, colour='orange', arrow=None, band=(0.1, 0.9), cap_max=0.24, width=0.84, ink='black',
          footer=None, layout=None):
    """A rectangular temporary-traffic-control panel no single page draws at the mod's wording:
    a rounded ``colour`` panel with the inset black border at the M4-9b's proportions, ``lines``
    set in ``band`` at one size and narrowing, and optionally the M6-2's diagonal arrow lifted
    off its page (``arrow='right'`` points up-right, ``'left'`` up-left) in the lower half.
    ``ink`` colours the border, legend and arrow (white on a green guide panel). ``footer``
    (text, top) rules the panel off at ``top`` and sets ``text`` in the strip below it (CALL 911).
    ``layout`` replaces the evenly spread ``lines`` with the original sign's own: (text, centre y,
    cap height, width, centre x, series) as measured by detect_legend_series.py."""
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
        if layout:
            for text, cy, lcap, lw, cx, series in layout:
                gg._legend_line_at(img, text, cx * Wd, cy * H, lcap * H, lw * Wd, colour=black, condense=series)
            cap, condense = layout[-1][2] * H, layout[-1][5]
        else:
            n = len(lines)
            pitch = (band[1] - band[0]) / n
            cap = min(cap_max, pitch * 0.66) * H
            condense = _series_condense(lines, cap, width * Wd)
            for i, text in enumerate(lines):
                gg._legend_line(img, text, (band[0] + pitch * (i + 0.5)) * H, cap, width * Wd,
                                colour=black, condense=condense)
        if footer:
            text, top = footer
            d.rectangle((inset, int(top * H) - stroke // 2, Wd - inset, int(top * H) + stroke // 2), fill=black)
            gg._legend_line(img, text, (top * H + H - inset) / 2, min(cap, (1.0 - top) * H * 0.55), width * Wd, colour=black,
                            condense=condense)
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


def FALLOUT_SHELTER(basement=False):
    """The Civil Defense fallout shelter sign (1961, public domain, no FHWA drawing): a yellow
    field holding the black disc and its three yellow triangles, over a black band lettered
    FALLOUT SHELTER in yellow, measured off a scan of the real 14 x 20 sign. ``basement``
    adds the IN BASEMENT supplement some signs carried, black on a yellow strip in the band."""
    def make(aspect):
        import gen_gap_signs as gg
        W, H = 1425, 2000                        # the scan's own units
        yellow, black = shs.MOD_COLOURS['yellow'], shs.MOD_COLOURS['black']
        img = Image.new('RGBA', (W, H), black)
        d = ImageDraw.Draw(img)
        band = 1415                              # where the yellow field ends
        d.rectangle((22, 22, W - 22, band), fill=yellow)
        cx, cy, r = 712, 720, 598
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=black)
        g = 8                                    # the black gap either side of each triangle's edge
        for tri in (((440 + g, 235), (1000 - g, 235), (718, 718 - 2 * g)),
                    ((155 + 2 * g, 725 + g), (715 - g, 725 + g), (435, 1205 - g)),
                    ((720 + g, 725 + g), (1280 - 2 * g, 725 + g), (995, 1205 - g))):
            d.polygon(tri, fill=yellow)
        if basement:
            gg._legend_line(img, 'FALLOUT SHELTER', 1545, 180, 0.94 * W, colour=yellow)
            d.rectangle((330, 1710, W - 330, 1900), fill=yellow)
            gg._legend_line(img, 'IN BASEMENT', 1805, 120, W - 720, colour=black, condense=0.8)
        else:
            gg._legend_line(img, 'FALLOUT SHELTER', 1580, 200, 0.94 * W, colour=yellow)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def _bus_icon(w):
    """The front of a bus, white, ``w`` wide: the pictogram on New York's bus lane banners."""
    h = int(w * 1.15)
    icon = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(icon)
    white = (255, 255, 255, 255)
    clear = (0, 0, 0, 0)
    d.rounded_rectangle((0, 0, w - 1, int(h * 0.86)), radius=int(w * 0.16), fill=white)
    d.rounded_rectangle((int(w * 0.12), int(h * 0.12), int(w * 0.88), int(h * 0.5)), radius=int(w * 0.06), fill=clear)
    for x0 in (0.12, 0.66):
        d.ellipse((int(w * x0), int(h * 0.6), int(w * (x0 + 0.22)), int(h * 0.76)), fill=clear)
    for x0 in (0.08, 0.7):
        d.rectangle((int(w * x0), int(h * 0.8), int(w * (x0 + 0.22)), h - 1), fill=white)
    return icon


def BUS_LANE(hours=True):
    """New York City's overhead bus lane banner (NYCDOT, no FHWA drawing), measured off
    photographs: a blue band with BUS LANE between two bus pictograms, then (``hours``) a black
    band with 7AM - 7PM and MON - FRI, then a white panel with a down arrow over the lane and
    BUSES ONLY & RIGHT TURNS, all inside a thin black border."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 600
        Wd = int(round(H * aspect))
        blue, black = shs.MOD_COLOURS['blue'], shs.MOD_COLOURS['black']
        white = shs.MOD_COLOURS['white']
        img = Image.new('RGBA', (Wd, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        e = int(H * 0.012)                       # the white sheet edge
        b = int(H * 0.022)                       # the black border
        d.rounded_rectangle((0, 0, Wd - 1, H - 1), radius=int(H * 0.03), fill=white)
        d.rectangle((e, e, Wd - 1 - e, H - 1 - e), fill=black)
        d.rectangle((e + b, e + b, Wd - 1 - e - b, H - 1 - e - b), fill=white)
        blue_end = 0.3 if hours else 0.34
        d.rectangle((e, e, Wd - 1 - e, int(H * blue_end)), fill=blue)
        gg._legend_line_at(img, 'BUS   LANE', Wd / 2, (blue_end * H + e) / 2, 0.16 * H, 0.6 * Wd, colour=white)
        icon = _bus_icon(int(H * 0.15))
        for x in (0.07, 0.93):
            img.alpha_composite(icon, (int(Wd * x - icon.width / 2), int((blue_end * H + e) / 2 - icon.height / 2)))
        top = blue_end
        if hours:
            d.rectangle((e, int(H * blue_end), Wd - 1 - e, int(H * 0.5)), fill=black)
            gg._legend_line_at(img, '7AM - 7PM', 0.3 * Wd, 0.405 * H, 0.11 * H, 0.3 * Wd, colour=white)
            gg._legend_line_at(img, 'MON - FRI', 0.63 * Wd, 0.405 * H, 0.11 * H, 0.3 * Wd, colour=white)
            top = 0.5
        # the arrow over the lane, and the legend beside it
        mid = (top + 0.97) / 2
        ax, aw = (0.3 if hours else 0.2) * Wd, 0.13 * Wd
        at, ab = (top + 0.08) * H, 0.9 * H
        stem = aw * 0.32
        head = (ab - at) * 0.5
        d.polygon([(ax - stem / 2, at), (ax + stem / 2, at), (ax + stem / 2, ab - head), (ax + aw / 2, ab - head),
                   (ax, ab), (ax - aw / 2, ab - head), (ax - stem / 2, ab - head)], fill=black)
        lx = 0.66 * Wd if hours else 0.6 * Wd
        gg._legend_line_at(img, 'BUSES ONLY', lx, (mid - 0.09) * H, (0.17 if hours else 0.2) * H, 0.52 * Wd, colour=black)
        gg._legend_line_at(img, '& RIGHT TURNS', lx, (mid + 0.14) * H, (0.09 if hours else 0.1) * H, 0.4 * Wd, colour=black)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def SNOWFLAKE():
    """A snowflake in white on the green pictogram panel (winter-conditions and snow-route
    signage use one; the 2004 book does not draw it): six arms with two pairs of branches."""
    def make(aspect):
        import math
        S = 1000
        sym = Image.new('RGBA', (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(sym)
        ink = (0, 0, 0, 255)
        c = S / 2
        arm, wid = S * 0.46, int(S * 0.055)
        for k in range(6):
            a = math.radians(90 + 60 * k)
            ux, uy = math.cos(a), -math.sin(a)
            d.line((c, c, c + ux * arm, c + uy * arm), fill=ink, width=wid)
            for t, blen in ((0.45, 0.2), (0.72, 0.14)):
                px, py = c + ux * arm * t, c + uy * arm * t
                for side in (-1, 1):
                    bb = a + side * math.radians(45)
                    d.line((px, py, px + math.cos(bb) * S * blen, py - math.sin(bb) * S * blen), fill=ink, width=wid)
            ex, ey = c + ux * arm, c + uy * arm
            d.ellipse((ex - wid / 2, ey - wid / 2, ex + wid / 2, ey + wid / 2), fill=ink)
        d.regular_polygon((c, c, S * 0.1), 6, rotation=0, fill=ink)
        return shs.symbol_on_panel(sym, shs.MOD_COLOURS['green'], aspect, size=256, symbol_frac=0.8)
    return lambda: (ComposedFace(make), False, None)


def SYM_ART(colour, art):
    """A pictogram made from page art rather than outline picks, set white on the rounded
    ``colour`` panel: ``art`` returns black-ink-on-clear (an arrow lifted off its page, or a
    a later drawing's symbol)."""
    return lambda: (SymbolFace(art(), shs.MOD_COLOURS[colour]), False, None)


def _page_arrow(page, pick, turn=0):
    """The arrow alone off a guide arrow plaque (M6-1 / M6-3), turned ``turn`` degrees clockwise."""
    def art():
        a = shs.recolour(shs.book_sign(G, page, pick, symbols_only=True), shs.SHS_PALETTE)
        a = a.crop(a.getchannel('A').getbbox())
        return a.rotate(-turn, expand=True, resample=Image.BICUBIC) if turn else a
    return art


def HEAR_BANJOS():
    """The HEAR BANJOS? / WALK FASTER novelty trail sign, set from photographs of the real one:
    a brown panel with a white border, the book's hiking pictogram (Guide p149) under the
    question with two eighth notes beside it, and WALK FASTER below in two sizes."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 1024
        Wd = int(round(H * aspect))
        white, brown = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['brown']
        img = Image.new('RGBA', (Wd, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        m = int(H * 0.01)
        d.rounded_rectangle((m, m, Wd - m, H - m), radius=int(H * 0.06), fill=white)
        b = int(H * 0.028)
        d.rounded_rectangle((m + b, m + b, Wd - m - b, H - m - b), radius=int(H * 0.045), fill=brown)
        gg._legend_line(img, 'HEAR BANJOS?', 0.125 * H, 0.075 * H, 0.8 * Wd, colour=white)
        # the hikers, white, a little right of centre to leave room for the notes
        import numpy as np
        sym = shs.symbol_face(G, 149, [0, 1, 2])
        a = np.asarray(sym.convert('RGBA')).astype(np.float32)
        lum = (0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]) / 255.0
        alpha = (a[..., 3] * (1.0 - lum)).clip(0, 255).astype(np.uint8)
        hik = Image.new('RGBA', sym.size, white)
        hik.putalpha(Image.fromarray(alpha, 'L'))
        hik = hik.crop(hik.getchannel('A').getbbox())
        hh = int(H * 0.36)
        hik = hik.resize((int(hik.width * hh / hik.height), hh), Image.LANCZOS)
        img.alpha_composite(hik, (int(Wd * 0.58 - hik.width / 2), int(H * 0.2)))
        # two beamed-free eighth notes, upper left of the hikers
        for nx, ny, sc in ((0.19, 0.29, 1.0), (0.26, 0.315, 1.0)):
            x, y, r = nx * Wd, ny * H, H * 0.018 * sc
            d.ellipse((x - r * 1.3, y - r, x + r * 1.3, y + r), fill=white)
            st = H * 0.007
            d.rectangle((x + r * 1.3 - st, y - H * 0.075, x + r * 1.3, y), fill=white)
            d.polygon([(x + r * 1.3 - st, y - H * 0.075), (x + r * 1.3 + H * 0.028, y - H * 0.045),
                       (x + r * 1.3 + H * 0.02, y - H * 0.035), (x + r * 1.3, y - H * 0.055)], fill=white)
        gg._legend_line(img, 'WALK', 0.68 * H, 0.1 * H, 0.8 * Wd, colour=white)
        gg._legend_line(img, 'FASTER', 0.845 * H, 0.14 * H, 0.84 * Wd, colour=white)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def LIGHTS_OUT():
    """The IF LIGHTS OUT / NO POWER / SIGNAL NOT WORKING blank-out sign: white legend on black
    with two lamps pointed at by arrows. The block swaps faces when powered, so the unlit
    face goes to the texture slot and the lit one (amber lamps) to its ``_on`` twin."""
    def draw(aspect, lit):
        import gen_gap_signs as gg
        S = 1024
        white, black = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['black']
        img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rounded_rectangle((8, 8, S - 8, S - 8), radius=60, fill=white)
        d.rounded_rectangle((34, 34, S - 34, S - 34), radius=42, fill=black)
        cap = 0.11 * S
        gg._legend_line(img, 'IF LIGHTS OUT', 0.16 * S, cap, 0.84 * S, colour=white)
        for x0 in (0.07, 0.70):
            box = (int(x0 * S), int(0.3 * S), int((x0 + 0.23) * S), int(0.47 * S))
            if lit:
                d.rounded_rectangle(box, radius=30, fill=(255, 196, 40, 255))
                d.rounded_rectangle((box[0] + 26, box[1] + 22, box[2] - 26, box[3] - 22), radius=20,
                                    fill=(255, 244, 190, 255))
            else:
                d.rounded_rectangle(box, radius=30, fill=(120, 120, 120, 255))
        # an arrow from the centre toward each lamp
        art = shs.recolour(shs.book_sign(G, 21, symbols_only=True), shs.SHS_PALETTE)
        art = art.crop(art.getchannel('A').getbbox())
        art = Image.composite(Image.new('RGBA', art.size, white), art, art)
        h = int(0.12 * S)
        art = art.resize((int(art.width * h / art.height), h), Image.LANCZOS)
        right = art.transpose(Image.FLIP_TOP_BOTTOM)             # down-right
        left = right.transpose(Image.FLIP_LEFT_RIGHT)            # down-left
        img.alpha_composite(left, (int(0.475 * S - left.width), int(0.3 * S)))
        img.alpha_composite(right, (int(0.525 * S), int(0.3 * S)))
        gg._legend_line(img, 'NO POWER', 0.64 * S, 0.14 * S, 0.84 * S, colour=white)
        gg._legend_line(img, 'SIGNAL NOT WORKING', 0.84 * S, 0.075 * S, 0.84 * S, colour=white)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(lambda a: draw(a, False),
                                 extras=lambda a: {'lightsoutnopowersign_on.png': draw(a, True)}), False, None)


def BOOK_LINES(chapter, page, lines, pick=0, draw=None):
    """A book panel with every legend dropped and the mod's own lines set on it, each at its own
    size: ``lines`` are (text, centre y, cap height, width[, colour name[, centre x]]) as fractions
    of the sign, black unless named. For
    a sign that is an official panel with a different legend (CITY SPEED LIMIT 35, END 35 MPH
    LIMIT). ``draw(img)`` adds artwork after the lines."""
    def make(aspect):
        import gen_gap_signs as gg
        panel = shs.recolour(shs.book_sign(chapter, page, pick, blank=True), shs.SHS_PALETTE)
        H = 1024
        Wd = int(round(H * aspect))
        img = panel.resize((Wd, H), Image.LANCZOS)
        for line in lines:
            text, cy, cap, width = line[:4]
            colour = shs.MOD_COLOURS[line[4]] if len(line) > 4 else shs.MOD_COLOURS['black']
            gg._legend_line_at(img, text, (line[5] if len(line) > 5 else 0.5) * Wd, cy * H, cap * H,
                               width * Wd, colour=colour, condense=line[6] if len(line) > 6 else 1.0)
        if draw:
            draw(img)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def _hov_diamond(img):
    """The HOV diamond, white outline, in the black header's left third (the R3-11a's)."""
    d = ImageDraw.Draw(img)
    W, H = img.size
    cx, cy, rx, ry = W * 0.2, H * 0.165, W * 0.09, H * 0.12
    white, black = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['black']
    d.polygon([(cx, cy - ry), (cx + rx, cy), (cx, cy + ry), (cx - rx, cy)], fill=white)
    t = W * 0.035
    d.polygon([(cx, cy - ry + t * 1.4), (cx + rx - t, cy), (cx, cy + ry - t * 1.4), (cx - rx + t, cy)], fill=black)


def DANGER_PLACARD(lines, header='DANGER'):
    """An ANSI-style facility DANGER placard (no FHWA drawing): white sign with a thin black
    border, a red header band lettered white, and black ``lines`` set at one size below it."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 1024
        Wd = int(round(H * aspect))
        white, black, red = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['black'], shs.MOD_COLOURS['red']
        img = Image.new('RGBA', (Wd, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        e, b = int(H * 0.03), int(H * 0.015)     # the edge outside the border line, the line
        band = 0.27
        # the sheet: red out to the edge across the header, white below it
        d.rounded_rectangle((4, 4, Wd - 4, H - 4), radius=int(H * 0.06), fill=white)
        d.rounded_rectangle((4, 4, Wd - 4, int(H * band) + 40), radius=int(H * 0.06), fill=red)
        d.rectangle((4, int(H * band), Wd - 4, int(H * band) + 41), fill=white)
        d.rounded_rectangle((e, e, Wd - e, H - e), radius=int(H * 0.045), outline=black, width=b)
        d.rectangle((e + b, e + b + int(H * 0.03), Wd - e - b, int(H * band)), fill=red)
        d.rounded_rectangle((e + b, e + b, Wd - e - b, int(H * band)), radius=int(H * 0.035), fill=red)
        d.rectangle((e, int(H * band), Wd - e, int(H * band) + b), fill=black)
        gg._legend_line(img, header, (band * H + e + b) / 2, 0.14 * H, 0.8 * Wd, colour=white)
        n = len(lines)
        pitch = (0.95 - band - 0.04) / n
        cap = min(0.12, pitch * 0.62) * H
        cond = _series_condense(lines, cap, 0.84 * Wd)
        for i, t in enumerate(lines):
            gg._legend_line(img, t, (band + 0.04 + pitch * (i + 0.5)) * H, cap, 0.84 * Wd, colour=black, condense=cond)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def HV_DANGER(footer=None):
    """The DANGER / KEEP OFF! DO NOT CLIMB! high-voltage placard on utility poles and towers:
    the red header, a red prohibition circle over a lattice tower, and the warning below.
    ``footer`` adds the owner's line in small type along the bottom (the Alto DWP sign's)."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 1024
        Wd = int(round(H * aspect))
        white, black, red = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['black'], shs.MOD_COLOURS['red']
        img = Image.new('RGBA', (Wd, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        e, b = int(H * 0.022), int(H * 0.011)
        d.rounded_rectangle((4, 4, Wd - 4, H - 4), radius=int(H * 0.045), fill=white)
        d.rounded_rectangle((e, e, Wd - e, H - e), radius=int(H * 0.035), fill=black)
        d.rounded_rectangle((e + b, e + b, Wd - e - b, H - e - b), radius=int(H * 0.028), fill=white)
        band = 0.15
        d.rounded_rectangle((e + b, e + b, Wd - e - b, int(H * band)), radius=int(H * 0.028), fill=red)
        d.rectangle((e + b, int(H * band) - int(H * 0.03), Wd - e - b, int(H * band)), fill=red)
        d.rectangle((e + b, int(H * band), Wd - e - b, int(H * band) + b), fill=black)
        cy = (band * H + e + b) / 2
        gg._legend_line_at(img, 'DANGER', Wd * 0.57, cy, 0.08 * H, 0.62 * Wd, colour=white)
        tw, th = Wd * 0.12, H * 0.085                  # the warning triangle, white with a red "!"
        tx = Wd * 0.17
        d.polygon([(tx, cy - th / 2), (tx + tw / 2, cy + th / 2), (tx - tw / 2, cy + th / 2)], fill=white)
        d.rectangle((tx - tw * 0.05, cy - th * 0.18, tx + tw * 0.05, cy + th * 0.2), fill=red)
        d.rectangle((tx - tw * 0.05, cy + th * 0.28, tx + tw * 0.05, cy + th * 0.4), fill=red)
        # the tower: a tapering lattice
        cx, top, bot = Wd / 2, H * 0.24, H * 0.58
        half_top, half_bot = Wd * 0.04, Wd * 0.14
        lw = max(3, int(Wd * 0.012))
        d.line((cx - half_top, top, cx - half_bot, bot), fill=black, width=lw)
        d.line((cx + half_top, top, cx + half_bot, bot), fill=black, width=lw)
        steps = 5
        for i in range(steps + 1):
            t0, t1 = i / steps, min(1, (i + 1) / steps)
            y0, y1 = top + (bot - top) * t0, top + (bot - top) * t1
            h0 = half_top + (half_bot - half_top) * t0
            h1 = half_top + (half_bot - half_top) * t1
            d.line((cx - h0, y0, cx + h0, y0), fill=black, width=lw)
            if i < steps:
                d.line((cx - h0, y0, cx + h1, y1), fill=black, width=lw)
                d.line((cx + h0, y0, cx - h1, y1), fill=black, width=lw)
        d.line((cx - Wd * 0.12, top + H * 0.04, cx + Wd * 0.12, top + H * 0.04), fill=black, width=lw)
        r = Wd * 0.26
        cy = (top + bot) / 2 - H * 0.01
        ring = int(Wd * 0.045)
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=red, width=ring)
        import math
        a = math.radians(45)
        d.line((cx - r * math.cos(a), cy - r * math.sin(a), cx + r * math.cos(a), cy + r * math.sin(a)), fill=red, width=ring)
        rows = (('KEEP OFF!', 0.69, 0.075), ('DO NOT CLIMB!', 0.785, 0.055),
                ('HAZARDOUS VOLTAGE', 0.865, 0.04), ('WILL SHOCK, BURN OR KILL', 0.92, 0.04))
        if footer:
            rows = (('KEEP OFF!', 0.675, 0.07), ('DO NOT CLIMB!', 0.765, 0.05),
                    ('HAZARDOUS VOLTAGE', 0.84, 0.037), ('WILL SHOCK, BURN OR KILL', 0.893, 0.037),
                    (footer, 0.95, 0.024))
        rule = (rows[0][1] - rows[0][2] / 2 - 0.035) * H
        d.rectangle((e + b, rule, Wd - e - b, rule + b), fill=black)
        left = e + b + Wd * 0.05
        for text, y, cap in rows:
            layer = Image.new('RGBA', img.size, (0, 0, 0, 0))
            gg._legend_line(layer, text, y * H, cap * H, 0.86 * Wd, colour=black)
            box = layer.getbbox()
            img.alpha_composite(layer.crop(box), (int(left), box[1]))
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def RED_ZONE_PARKING():
    """New York's RED ZONE / DON'T EVEN THINK OF PARKING HERE: black panel, red border and a red
    RED ZONE band, white legend with THINK the largest line."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 1024
        Wd = int(round(H * aspect))
        white, black, red = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['black'], shs.MOD_COLOURS['red']
        img = Image.new('RGBA', (Wd, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        e, b, w = int(H * 0.012), int(H * 0.022), int(H * 0.006)
        d.rounded_rectangle((4, 4, Wd - 4, H - 4), radius=int(H * 0.045), fill=white)
        d.rounded_rectangle((e, e, Wd - e, H - e), radius=int(H * 0.04), fill=red)
        inner = (e + b, int(H * 0.1), Wd - e - b, H - e - b)
        d.rectangle((inner[0] - w, inner[1] - w, inner[2] + w, inner[3] + w), fill=white)
        d.rectangle(inner, fill=black)
        gg._legend_line(img, 'RED ZONE', (0.1 * H + e) / 2, 0.045 * H, 0.6 * Wd, colour=white)
        for text, y, cap in (("DON'T", 0.2, 0.09), ('EVEN', 0.33, 0.09), ('THINK', 0.5, 0.15),
                             ('OF', 0.64, 0.07), ('PARKING', 0.76, 0.09), ('HERE', 0.89, 0.09)):
            gg._legend_line(img, text, y * H, cap * H, 0.86 * Wd, colour=white)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def CARELESS_PERSON():
    """A CARELESS PERSON IS JUST AN ACCIDENT GOING SOMEPLACE TO HAPPEN, the old safety slogan
    banner, as photographed: white with a bright green border, italic black lettering, and
    CARELESS and ACCIDENT in red with a red underline."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 480
        Wd = int(round(H * aspect))
        white, black, red = shs.MOD_COLOURS['white'], shs.MOD_COLOURS['black'], shs.MOD_COLOURS['red']
        green = (46, 160, 67, 255)
        img = Image.new('RGBA', (Wd, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rounded_rectangle((4, 4, Wd - 4, H - 4), radius=int(H * 0.05), fill=green)
        b = int(H * 0.05)
        d.rounded_rectangle((4 + b, 4 + b, Wd - 4 - b, H - 4 - b), radius=int(H * 0.03), fill=white)
        cap = 0.2 * H
        space = cap * 0.6
        rows = (((('A', black, False), ('CARELESS', red, True), ('PERSON', black, False), ('IS', black, False))),
                ((('JUST', black, False), ('AN', black, False), ('ACCIDENT', red, True), ('GOING', black, False))),
                ((('SOMEPLACE', black, False), ('TO', black, False), ('HAPPEN', black, False))))
        for runs, cy in zip(rows, (0.26, 0.5, 0.74)):
            # the words side by side on one layer, a word space apart, then the line is sheared
            # italic and centred
            layer = Image.new('RGBA', (Wd * 2, H), (0, 0, 0, 0))
            ld = ImageDraw.Draw(layer)
            x = 0
            for n, (text, colour, underline) in enumerate(runs):
                part = Image.new('RGBA', (Wd * 2, H), (0, 0, 0, 0))
                gg._legend_line_at(part, text, Wd, cy * H, cap, Wd * 2, colour=colour)
                box = part.getbbox()
                if n:
                    x += space
                layer.alpha_composite(part.crop(box), (int(x), box[1]))
                if underline:
                    y = box[3] + cap * 0.08
                    ld.rectangle((x, y, x + box[2] - box[0], y + cap * 0.07), fill=colour)
                x += box[2] - box[0]
            box = layer.getbbox()
            line = layer.crop(box)
            k = 0.22
            wide = Image.new('RGBA', (line.width + int(line.height * k) + 2, line.height), (0, 0, 0, 0))
            wide.paste(line, (0, 0))
            line = wide.transform(wide.size, Image.AFFINE, (1, k, -k * line.height, 0, 1, 0), Image.BICUBIC)
            line = line.crop(line.getbbox())
            maxw = int(Wd * 0.91)
            if line.width > maxw:
                line = line.resize((maxw, line.height), Image.LANCZOS)
            img.alpha_composite(line, ((Wd - line.width) // 2, box[1]))
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def BULLSEYE():
    """The FDC standpipe bullseye: concentric red and white rings, drawn crisp."""
    def make(aspect):
        S = 1024
        img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        red, white = shs.MOD_COLOURS['red'], shs.MOD_COLOURS['white']
        for r, c in ((0.5, red), (0.33, white), (0.16, red)):
            d.ellipse((S * (0.5 - r), S * (0.5 - r), S * (0.5 + r) - 1, S * (0.5 + r) - 1), fill=c)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def PANEL_LINES(lines, colour='white', ink='black', art=None, condense=1.0):
    """A plain rounded panel with the inset border (PANEL's) and ``lines`` each at its own size:
    (text, centre y, cap height, width) as fractions of the sign, in ``ink``. ``art(img)`` draws
    over it (a symbol lifted off a book page)."""
    def make(aspect):
        import gen_gap_signs as gg
        H = 1024
        Wd = int(round(H * aspect))
        img = Image.new('RGBA', (Wd, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        inkc = shs.MOD_COLOURS[ink]
        m = int(H * 0.012)
        d.rounded_rectangle((m, m, Wd - m, H - m), radius=int(H * 0.07), fill=shs.MOD_COLOURS[colour])
        inset, stroke = int(H * 0.035), int(H * 0.03)
        d.rounded_rectangle((inset, inset, Wd - inset, H - inset), radius=int(H * 0.05), outline=inkc, width=stroke)
        for line in lines:
            text, cy, cap, width = line[:4]
            cx = line[4] if len(line) > 4 else 0.5
            series = line[5] if len(line) > 5 else condense
            gg._legend_line_at(img, text, cx * Wd, cy * H, cap * H, width * Wd, colour=inkc, condense=series)
        if art:
            art(img)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def _book_symbol(chapter, page, box, pick=0):
    """Paste a book sign's face inside its border into ``box`` (fractions of the
    image: x0, y0, x1, y1), fitted and centred: the R3-2's crossed-out turn over ON RED."""
    def art(img):
        sym = shs.recolour(shs.book_sign(chapter, page, pick), shs.SHS_PALETTE)
        # inside the sign's black border: the panel white stays, the border and sheet go
        m = int(min(sym.size) * 0.05)
        sym = sym.crop((m, m, sym.width - m, sym.height - m))
        W, H = img.size
        bw, bh = (box[2] - box[0]) * W, (box[3] - box[1]) * H
        k = min(bw / sym.width, bh / sym.height)
        sym = sym.resize((int(sym.width * k), int(sym.height * k)), Image.LANCZOS)
        img.alpha_composite(sym, (int(box[0] * W + (bw - sym.width) / 2), int(box[1] * H + (bh - sym.height) / 2)))
    return art


def FLASHING_YELLOW_YIELD():
    """LEFT TURN YIELD ON FLASHING with a yellow left arrow: the R10-12 with its ON GREEN line
    re-set and the green ball replaced by the flashing yellow arrow on a black disc."""
    def make(aspect):
        import numpy as np
        face = shs.book_sign(R, 143, replace=('ON GREEN', 'ON FLASHING'), condense=True)
        a = np.asarray(face).astype(np.int32)
        green = (np.abs(a[..., 0] - 0) < 60) & (a[..., 1] > 110) & (a[..., 2] < 110) & (a[..., 3] > 0)
        ys, xs = np.nonzero(green)
        cx, cy = xs.mean(), ys.mean()
        r = max(xs.max() - xs.min(), ys.max() - ys.min()) / 2.0 + 2
        face = shs.recolour(face, shs.SHS_PALETTE)
        d = ImageDraw.Draw(face)
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=shs.MOD_COLOURS['black'])
        yellow = shs.MOD_COLOURS['yellow']
        w = max(3, int(r * 0.12))
        tip = (cx - r * 0.72, cy)
        d.line((cx + r * 0.05, cy - r * 0.66, tip[0], tip[1], cx + r * 0.05, cy + r * 0.66), fill=yellow,
               width=w, joint='curve')
        d.line((cx - r * 0.24, cy, cx + r * 0.72, cy), fill=yellow, width=w)
        return shs.fit_plate(face, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def _runs(img, runs, baseline, cx, colour):
    """Words of different sizes side by side on one baseline, centred on ``cx``: ``runs`` are
    (text, cap height px, underlined), a word space of the larger neighbour between them."""
    import gen_gap_signs as gg
    d = ImageDraw.Draw(img)
    parts = []
    for text, cap, under in runs:
        layer = Image.new('RGBA', img.size, (0, 0, 0, 0))
        gg._legend_line_at(layer, text, img.width / 2, baseline - cap / 2, cap, img.width, colour=colour)
        box = layer.getbbox()
        parts.append((layer.crop(box), box, cap, under))
    gap = [max(parts[k][2], parts[k + 1][2]) * 0.28 for k in range(len(parts) - 1)]
    total = sum(pt[0].width for pt in parts) + sum(gap)
    x = cx - total / 2
    for k, (im, box, cap, under) in enumerate(parts):
        img.alpha_composite(im, (int(x), box[1]))
        if under:
            d.rectangle((x, box[3] + cap * 0.18, x + im.width, box[3] + cap * 0.36), fill=colour)
        x += im.width + (gap[k] if k < len(gap) else 0)


def _car(d, x, y, w, colour, hole):
    """A car in side view, white silhouette with wheel holes, ``w`` long, its roof at ``y``."""
    h = w * 0.42
    d.rounded_rectangle((x, y + h * 0.38, x + w, y + h * 0.8), radius=h * 0.12, fill=colour)
    d.polygon([(x + w * 0.22, y + h * 0.4), (x + w * 0.34, y), (x + w * 0.68, y), (x + w * 0.8, y + h * 0.4)],
              fill=colour)
    for wx in (0.24, 0.76):
        rr = h * 0.2
        d.ellipse((x + w * wx - rr, y + h * 0.78 - rr, x + w * wx + rr, y + h * 0.78 + rr), fill=colour)
        d.ellipse((x + w * wx - rr * 0.45, y + h * 0.78 - rr * 0.45, x + w * wx + rr * 0.45, y + h * 0.78 + rr * 0.45),
                  fill=hole)


def LA_NO_STOPPING():
    """Los Angeles' NO STOPPING 7AM to 9AM / 4PM to 6PM tow-away sign, set from a photograph of
    the city's sign: red border on white, a red band with a tow truck lifting a car, NO reversed
    out of a red block beside STOPPING, the hours with a small underlined "to", and the
    EXCEPT SATURDAY & SUNDAY / impound lines in small red type."""
    def make(aspect):
        import gen_gap_signs as gg
        S = 1024
        W = int(round(S * aspect))
        red, white = shs.MOD_COLOURS['red'], shs.MOD_COLOURS['white']
        img = Image.new('RGBA', (W, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rounded_rectangle((4, 4, W - 4, S - 4), radius=int(S * 0.05), fill=white)
        e, b = int(S * 0.025), int(S * 0.022)
        d.rounded_rectangle((e, e, W - e, S - e), radius=int(S * 0.04), outline=red, width=b)
        d.rounded_rectangle((e, e, W - e, int(S * 0.19)), radius=int(S * 0.04), fill=red)
        d.rectangle((e, int(S * 0.12), W - e, int(S * 0.19)), fill=red)
        # the tow: a car on the left, its front lifted, hooked to the truck on the right
        car = Image.new('RGBA', (int(W * 0.34), int(S * 0.2)), (0, 0, 0, 0))
        _car(ImageDraw.Draw(car), car.width * 0.03, car.height * 0.2, car.width * 0.94, white, red)
        car = car.rotate(7, expand=True, resample=Image.BICUBIC)
        layer = Image.new('RGBA', img.size, (0, 0, 0, 0))
        layer.alpha_composite(car, (int(W * 0.1), int(S * 0.02)))
        ld = ImageDraw.Draw(layer)
        tx, ty, tw = W * 0.55, S * 0.045, W * 0.36
        _car(ld, tx, ty, tw, white, red)
        ld.line((W * 0.43, S * 0.1, tx + tw * 0.1, ty + tw * 0.2), fill=white, width=int(S * 0.012))
        img.alpha_composite(layer)
        d.rectangle((e, int(S * 0.19), int(W * 0.31), int(S * 0.41)), fill=red)
        gg._legend_line_at(img, 'NO', W * 0.165, S * 0.3, S * 0.16, W * 0.26, colour=white)
        gg._legend_line_at(img, 'STOPPING', W * 0.645, S * 0.3, S * 0.12, W * 0.6, colour=red)
        _runs(img, [('7', S * 0.14, False), ('AM', S * 0.1, False), ('TO', S * 0.045, True),
                    ('9', S * 0.14, False), ('AM', S * 0.1, False)], S * 0.58, W / 2, red)
        _runs(img, [('4', S * 0.14, False), ('PM', S * 0.1, False), ('TO', S * 0.045, True),
                    ('6', S * 0.14, False), ('PM', S * 0.1, False)], S * 0.77, W / 2, red)
        gg._legend_line(img, 'EXCEPT SATURDAY & SUNDAY', S * 0.845, S * 0.036, W * 0.8, colour=red)
        gg._legend_line(img, 'TO RECOVER IMPOUNDED VEHICLE CALL 3-1-1', S * 0.905, S * 0.03, W * 0.86, colour=red)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def LA_TWO_HOUR():
    """The green 2 HOUR PARKING 9AM to 8PM EXCEPT SUNDAY panel that hangs under Los Angeles'
    no stopping sign: a green block with the 2 reversed out, HOUR / PARKING beside it."""
    def make(aspect):
        import gen_gap_signs as gg
        S = 1024
        W = int(round(S * aspect))
        green, white = shs.MOD_COLOURS['green'], shs.MOD_COLOURS['white']
        img = Image.new('RGBA', (W, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.rounded_rectangle((4, 4, W - 4, S - 4), radius=int(S * 0.05), fill=white)
        e, b = int(S * 0.025), int(S * 0.022)
        d.rounded_rectangle((e, e, W - e, S - e), radius=int(S * 0.04), outline=green, width=b)
        d.rounded_rectangle((e, e, int(W * 0.34), int(S * 0.5)), radius=int(S * 0.04), fill=green)
        d.rectangle((int(W * 0.2), e, int(W * 0.34), int(S * 0.5)), fill=green)
        d.rectangle((e, int(S * 0.2), int(W * 0.34), int(S * 0.5)), fill=green)
        gg._legend_line_at(img, '2', W * 0.18, S * 0.27, S * 0.3, W * 0.26, colour=white)
        gg._legend_line_at(img, 'HOUR', W * 0.66, S * 0.15, S * 0.13, W * 0.56, colour=green)
        gg._legend_line_at(img, 'PARKING', W * 0.66, S * 0.36, S * 0.15, W * 0.6, colour=green)
        _runs(img, [('9', S * 0.14, False), ('AM', S * 0.1, False), ('TO', S * 0.045, True),
                    ('8', S * 0.14, False), ('PM', S * 0.1, False)], S * 0.74, W / 2, green)
        gg._legend_line(img, 'EXCEPT  SUNDAY', S * 0.87, S * 0.05, W * 0.6, colour=green)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def STOP_BANG():
    """The LHS "STOP!" sign, deliberately not the MUTCD's: the R1-1 octagon from the book with
    its legend dropped and STOP! set underlined in Highway Gothic."""
    def make(aspect):
        import gen_gap_signs as gg
        face = shs.recolour(shs.book_sign(R, 0, blank=True), shs.SHS_PALETTE)
        S = 1024
        img = face.resize((S, S), Image.LANCZOS)
        white = shs.MOD_COLOURS['white']
        gg._legend_line(img, 'STOP!', S * 0.47, S * 0.24, S * 0.74, colour=white)
        d = ImageDraw.Draw(img)
        d.rectangle((S * 0.16, S * 0.64, S * 0.84, S * 0.68), fill=white)
        return shs.fit_plate(img, aspect, size=256)
    return lambda: (ComposedFace(make), False, None)


def _signal_head(box):
    """A black three-lamp signal head (red, yellow, green) in ``box`` (fractions of the image)."""
    def art(img):
        d = ImageDraw.Draw(img)
        W, H = img.size
        x0, y0, x1, y1 = box[0] * W, box[1] * H, box[2] * W, box[3] * H
        d.rounded_rectangle((x0, y0, x1, y1), radius=(x1 - x0) * 0.18, fill=shs.MOD_COLOURS['black'])
        r = min(x1 - x0, (y1 - y0) / 3) * 0.36
        cx = (x0 + x1) / 2
        for k, c in enumerate(('red', 'yellow', 'green')):
            cy = y0 + (y1 - y0) * (k + 0.5) / 3
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=shs.MOD_COLOURS[c])
    return art


def _rule(y):
    """A full-width black rule inside the border at ``y`` (fraction of the height)."""
    def art(img):
        d = ImageDraw.Draw(img)
        W, H = img.size
        d.rectangle((W * 0.035, H * y - H * 0.012, W * 0.965, H * y + H * 0.012), fill=shs.MOD_COLOURS['black'])
    return art


def _header_band(text, bottom):
    """A black band across the top of the panel down to ``bottom``, ``text`` in white on it."""
    def art(img):
        import gen_gap_signs as gg
        d = ImageDraw.Draw(img)
        W, H = img.size
        d.rounded_rectangle((H * 0.035, H * 0.035, W - H * 0.035, H * bottom), radius=int(H * 0.05),
                            fill=shs.MOD_COLOURS['black'])
        d.rectangle((H * 0.035, H * bottom - H * 0.06, W - H * 0.035, H * bottom), fill=shs.MOD_COLOURS['black'])
        gg._legend_line(img, text, H * (bottom + 0.035) / 2, H * 0.06, W * 0.8, colour=shs.MOD_COLOURS['white'])
    return art


def _both(*arts):
    def art(img):
        for a in arts:
            a(img)
    return art


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
    ('conezonesign', PANEL(['SLOW FOR', 'THE CONE', 'ZONE'], band=(0.1, 0.9), width=0.76,
     layout=[('SLOW FOR', 0.258, 0.148, 0.86, 0.5, 'B'), ('THE CONE', 0.51, 0.14, 0.86, 0.5, 'B'), ('ZONE', 0.755, 0.142, 0.86, 0.5, 'B')]), 'orange panel'),
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
    ('hwyentrance', PANEL(['HIGHWAY', 'ENTRANCE'], 'green', ink='white', band=(0.2, 0.8),
     layout=[('HIGHWAY', 0.405, 0.096, 0.86, 0.5, 'E'), ('ENTRANCE', 0.579, 0.096, 0.86, 0.5, 'ModE')]), 'green panel'),
    ('pkwyentrancesign', PANEL(['PARKWAY', 'ENTRANCE'], 'green', ink='white', band=(0.2, 0.8),
     layout=[('PARKWAY', 0.378, 0.174, 0.86, 0.5, 'B'), ('ENTRANCE', 0.621, 0.176, 0.86, 0.5, 'B')]), 'green panel'),
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
    ('signweighstationnextright', PANEL(['WEIGH', 'STATION', 'NEXT RIGHT'], 'green', ink='white', band=(0.1, 0.9), width=0.78,
     layout=[('WEIGH', 0.258, 0.164, 0.86, 0.5, 'D'), ('STATION', 0.521, 0.166, 0.86, 0.5, 'D'), ('NEXT RIGHT', 0.764, 0.148, 0.86, 0.5, 'D')]),
     'green panel (D8-2 legend)'),
    ('landareastudysign', PANEL(['LAND AREA', 'UNDER STUDY'], 'blue', ink='white', band=(0.2, 0.8), width=0.76,
     layout=[('LAND AREA', 0.363, 0.172, 0.86, 0.5, 'B'), ('UNDER STUDY', 0.655, 0.174, 0.86, 0.5, 'B')]), 'blue panel'),
    ('altowelcomesyousign', PANEL(['ALTO', 'WELCOMES YOU', 'ESTABLISHED IN 2020'], 'blue', ink='white',
                                  band=(0.22, 0.78), cap_max=0.1), 'blue panel'),
    ('respectuianatparkssign', PANEL(['PLEASE', 'RESPECT ALL UIA', 'NATIONAL PARKS', 'AND PROPERTY', 'FOR EVERYONE',
                                      'UIA CODES APPLY'], 'white', ink='green', band=(0.08, 0.92), width=0.76), 'white panel'),
    ('falloutsheltersign', FALLOUT_SHELTER(), 'Civil Defense (drawn)'),
    ('falloutsheltersignalt', FALLOUT_SHELTER(basement=True), 'Civil Defense, IN BASEMENT (drawn)'),
    ('buslanesign', BUS_LANE(), 'NYCDOT bus lane (drawn)'),
    ('signsnowflake', SNOWFLAKE(), 'snowflake on green (drawn)'),
    # --- Remaining-signs batch 18. Left as drawn: 99onlypricesignnew2, uiawelcomesyousign,
    # signarv, signlaundry (the page reverses the washer out
    # of a black square); signpolice / sheriffstation kept their artwork
    # with the blue recoloured (one-off, not catalogued).
    ('signphone', SHS(G, 57), 'D9-1'),
    ('hearbanjossign', HEAR_BANJOS(), 'novelty trail sign (photo)'),
    # --- Remaining-signs batch 19, the last rows. Left as drawn: tolledbikelanesign (Alto MTA
    # toll plaque), steepedgesign (facility placard), signsharedpathway.
    ('signpicnic', SYM(G, 132, [2], 'brown'), 'picnic area (brown)'),
    ('signshelter', SYM(G, 133, [0], 'brown'), 'picnic shelter (brown)'),
    ('signrightarrowbrown', SYM_ART('brown', _page_arrow(20, 4)), 'M6-1 arrow (brown)'),
    ('r1012uturn', SHS(R, 143, replace=('LEFT TURN', 'U TURN')), 'R10-12 (U TURN)'),
    ('rightlanebussign', SHS(R, 48, replace=('6AM-9AM', '7AM-7PM')), 'R3-11b (7AM-7PM)'),
    ('lightsoutnopowersign', LIGHTS_OUT(), 'blank-out sign (drawn), + _on'),
    # --- Second pass, batch A (signs first left as drawn). Kept: bearcrossingsign (no bear
    # symbol to draw from; already the mod's yellow).
    ('carelesspersonsign', CARELESS_PERSON(), 'safety slogan (drawn)'),
    ('beginfwysign', PANEL(['BEGIN', 'FREEWAY'], 'white', band=(0.2, 0.8), width=0.76,
     layout=[('BEGIN', 0.353, 0.189, 0.86, 0.5, 'B'), ('FREEWAY', 0.654, 0.187, 0.86, 0.5, 'B')]), 'white panel'),
    ('beginhwysign', PANEL(['BEGIN', 'HIGHWAY'], 'white', band=(0.2, 0.8), width=0.76,
     layout=[('BEGIN', 0.354, 0.189, 0.86, 0.5, 'B'), ('HIGHWAY', 0.653, 0.189, 0.86, 0.5, 'B')]), 'white panel'),
    ('beginpkwysign', PANEL(['BEGIN', 'PARKWAY'], 'white', band=(0.2, 0.8), width=0.76,
     layout=[('BEGIN', 0.355, 0.191, 0.86, 0.5, 'B'), ('PARKWAY', 0.656, 0.183, 0.86, 0.5, 'B')]), 'white panel'),
    ('signbeginplaque', PANEL(['BEGIN'], 'white', band=(0.2, 0.8), cap_max=0.45, width=0.7,
     layout=[('BEGIN', 0.498, 0.519, 0.86, 0.5, 'B')]), 'white plaque'),
    ('signcenterlanebusonly69', BOOK_LINES(R, 39, [('CENTER', 0.1, 0.085, 0.8, 'white'), ('LANE', 0.23, 0.085, 0.8, 'white'),
                                                   ('BUSES', 0.43, 0.11, 0.8), ('ONLY', 0.6, 0.11, 0.8),
                                                   ('6AM - 9AM', 0.76, 0.075, 0.8), ('MON-FRI', 0.88, 0.06, 0.8)]),
     'R3-9f panel (CENTER BUSES ONLY)'),
    ('signcenterhov6a9a', BOOK_LINES(R, 39, [('CENTER', 0.1, 0.085, 0.6, 'white', 0.6), ('LANE', 0.23, 0.085, 0.6, 'white', 0.6),
                                             ('HOV 2+', 0.43, 0.11, 0.8), ('ONLY', 0.6, 0.11, 0.8),
                                             ('6AM - 9AM', 0.76, 0.075, 0.8), ('MON-FRI', 0.88, 0.06, 0.8)],
                                     draw=_hov_diamond), 'R3-9f panel (CENTER HOV 2+)'),
    ('signcenterlanenouse79', SHS(R, 39), 'R3-9f'),
    ('signcityspeed35', BOOK_LINES(R, 11, [('CITY', 0.146, 0.096, 0.86, 'black', 0.5, 'ModE'), ('SPEED', 0.309, 0.097, 0.86, 'black', 0.5, 'ModE'), ('LIMIT', 0.471, 0.094, 0.86, 'black', 0.5, 'F'), ('35', 0.74, 0.328, 0.86, 'black', 0.5, 'D')]), 'R2-1 panel'),
    ('signendspeed35', BOOK_LINES(R, 11, [('END', 0.167, 0.131, 0.86, 'black', 0.5, 'D'), ('35', 0.428, 0.265, 0.86, 'black', 0.5, 'D'), ('MPH', 0.682, 0.094, 0.86, 'black', 0.5, 'F'), ('LIMIT', 0.846, 0.094, 0.86, 'black', 0.5, 'E')]), 'R2-1 panel'),
    ('signdividedhw1', SHS(R, 89), 'R6-3'),
    ('signdividedhw2', SHS(R, 90), 'R6-3a'),
    ('dangerbadwatersign', DANGER_PLACARD(['DO NOT DRINK', 'THIS WATER']), 'danger placard'),
    ('dangerfallingmaterialsign', DANGER_PLACARD(['FALLING', 'MATERIAL']), 'danger placard'),
    ('generichvdangersign', HV_DANGER(), 'high voltage placard'),
    ('altodwphvdangersign', HV_DANGER(footer='ALTO DEPARTMENT OF WATER AND POWER'), 'high voltage placard'),
    ('signdontthinkparking', RED_ZONE_PARKING(), 'NYC red zone (drawn)'),
    ('signexceptbus', TEXT_PANEL(R, 60, ['EXCEPT', 'BUS'], pick=1, band=(0.14, 0.86)), 'R3-17aP plaque'),
    ('signfdcstandpipe', BULLSEYE(), 'bullseye (drawn)'),
    # --- Second pass, batch B. Approved as they are: signmetro, signpostmbtalogo,
    # signlitteringillegal. Redo later: kathieevanssign (not a road sign).
    ('ladotnostopping', LA_NO_STOPPING(), 'LA no stopping (photo)'),
    ('lhsstopsign', STOP_BANG(), 'R1-1 octagon, STOP! (non-compliant on purpose)'),
    # --- Second pass, batch C. signresidentnormal paints signresidentlarge's texture (done in B).
    ('noovernightparkingsign', PANEL(['NO', 'OVERNIGHT', 'PARKING', 'AND', 'CAMPING'], 'white', ink='red',
                                     band=(0.06, 0.94), width=0.8,
     layout=[('NO', 0.163, 0.103, 0.86, 0.5, 'B'), ('OVERNIGHT', 0.333, 0.103, 0.86, 0.5, 'C'), ('PARKING', 0.501, 0.103, 0.86, 0.5, 'C'), ('AND', 0.67, 0.101, 0.86, 0.49, 'C'), ('CAMPING', 0.839, 0.103, 0.86, 0.5, 'C')]), 'white panel, red'),
    ('signnoovernightparking', PANEL_LINES([('NO', 0.165, 0.103, 0.86, 0.5, 'D'), ('OVERNIGHT', 0.302, 0.103, 0.86, 0.51, 'C'), ('PARKING', 0.44, 0.103, 0.86, 0.5, 'C'), ('VIOLATORS', 0.567, 0.049, 0.86, 0.5, 'D'), ('TOWED AWAY', 0.642, 0.049, 0.86, 0.5, 'D'), ('AT VEHICLE', 0.716, 0.049, 0.86, 0.5, 'D'), ("OWNER'S", 0.793, 0.051, 0.86, 0.5, 'D'), ('EXPENSE', 0.868, 0.049, 0.86, 0.5, 'D')], ink='red'),
     'white panel, red'),
    ('noparkingeairssign', PANEL(['NO PARKING', 'IN THIS AREA', 'FOR 1 MILE', 'EMERGENCY', 'AIRSTRIP'], 'white',
                                 ink='red', band=(0.06, 0.94), width=0.8,
     layout=[('NO PARKING', 0.203, 0.082, 0.86, 0.5, 'C'), ('IN THIS AREA', 0.349, 0.08, 0.86, 0.5, 'C'), ('FOR 1 MILE', 0.494, 0.082, 0.86, 0.5, 'C'), ('EMERGENCY', 0.639, 0.082, 0.86, 0.5, 'C'), ('AIRSTRIP', 0.786, 0.08, 0.86, 0.5, 'D')]), 'white panel, red'),
    ('noparkinginalleysign', PANEL_LINES([('NO', 0.19, 0.158, 0.86, 0.5, 'C'), ('PARKING', 0.401, 0.158, 0.86, 0.5, 'B'), ('IN', 0.621, 0.101, 0.86, 0.5, 'D'), ('ALLEY', 0.78, 0.103, 0.86, 0.5, 'E')], ink='red'),
     'white panel, red'),
    ('noparkingonbridgesign', PANEL_LINES([('NO', 0.208, 0.181, 0.86, 0.5, 'D'), ('PARKING', 0.417, 0.158, 0.86, 0.5, 'B'), ('ON', 0.647, 0.092, 0.86, 0.49, 'F'), ('BRIDGE', 0.806, 0.092, 0.86, 0.5, 'F')], ink='red'),
     'white panel, red'),
    ('signnorightred', PANEL_LINES([('ON RED', 0.83, 0.11, 0.8)], art=_book_symbol(R, 22, (0.17, 0.11, 0.83, 0.67))),
     'R3-1 symbol + ON RED'),
    ('signnotrucksleftlane', PANEL_LINES([('NO', 0.163, 0.107, 0.86, 0.5, 'B'), ('TRUCKS', 0.315, 0.107, 0.86, 0.49, 'C'), ('LEFT LANE', 0.47, 0.103, 0.86, 0.5, 'C'), ('EXCEPT', 0.703, 0.105, 0.86, 0.5, 'C'), ('LEFT TURNS', 0.856, 0.107, 0.86, 0.5, 'C')], art=_rule(0.56)), 'white panel'),
    ('signnoturnsofficialonly', PANEL(['NO TURNS', 'OFFICIAL', 'USE ONLY'], 'white', band=(0.12, 0.88), width=0.8,
     layout=[('NO TURNS', 0.244, 0.156, 0.86, 0.5, 'C'), ('OFFICIAL', 0.504, 0.125, 0.86, 0.5, 'D'), ('USE ONLY', 0.762, 0.125, 0.86, 0.5, 'D')]),
     'white panel'),
    ('signonbridge', PANEL_LINES([('ON', 0.324, 0.23, 0.86, 0.5, 'B'), ('BRIDGE', 0.654, 0.23, 0.86, 0.5, 'B')], ink='red'),
     'R8 plaque, red'),
    ('signonpavement', PANEL_LINES([('ON', 0.331, 0.217, 0.86, 0.5, 'B'), ('PAVEMENT', 0.661, 0.209, 0.86, 0.5, 'B')], ink='red'),
     'R8 plaque, red'),
    ('signonecarpergreeneachlane', PANEL(['ONE CAR', 'PER GREEN', 'EACH LANE'], 'white', band=(0.1, 0.9), width=0.8,
     layout=[('ONE CAR', 0.202, 0.17, 0.86, 0.5, 'C'), ('PER GREEN', 0.492, 0.172, 0.86, 0.5, 'C'), ('EACH LANE', 0.787, 0.172, 0.86, 0.5, 'C')]),
     'white panel'),
    ('signonecarpergreen', PANEL(['ONE', 'VEHICLE', 'PER', 'GREEN'], 'white', band=(0.07, 0.93), width=0.8,
     layout=[('ONE', 0.171, 0.131, 0.86, 0.5, 'C'), ('VEHICLE', 0.394, 0.131, 0.86, 0.5, 'C'), ('PER', 0.617, 0.129, 0.86, 0.5, 'C'), ('GREEN', 0.841, 0.131, 0.86, 0.5, 'C')]),
     'white panel'),
    ('signphotoenforced', PANEL_LINES([('PHOTO', 0.75, 0.075, 0.8), ('ENFORCED', 0.87, 0.075, 0.8)],
                                      art=_signal_head((0.36, 0.08, 0.64, 0.64))), 'signal head + PHOTO ENFORCED'),
    ('positivelynosmokingsign', PANEL(['POSITIVELY NO', 'SMOKING OR OPEN', 'LIGHTS PERMITTED'], 'white',
                                      band=(0.14, 0.86), width=0.86), 'white plaque'),
    ('signredlightphoto', PANEL_LINES([('RED', 0.18, 0.12, 0.52, 0.62), ('LIGHT', 0.39, 0.12, 0.52, 0.62),
                                       ('PHOTO', 0.6, 0.12, 0.52, 0.62), ('ENFORCED', 0.81, 0.12, 0.52, 0.62)],
                                      art=_signal_head((0.1, 0.1, 0.28, 0.62))), 'signal head + legend'),
    ('signfine400', PANEL_LINES([('RED LIGHT', 0.186, 0.101, 0.86, 0.5, 'C'), ('VIOLATION', 0.369, 0.101, 0.86, 0.5, 'C'), ('$400', 0.597, 0.193, 0.86, 0.5, 'C'), ('FINE', 0.816, 0.097, 0.86, 0.5, 'B')]), 'white panel'),
    ('signpostreduced30', BOOK_LINES(R, 11, [('REDUCED', 0.187, 0.135, 0.86, 'black', 0.5, 'B'), ('SPEED', 0.389, 0.137, 0.86, 'black', 0.49, 'B'), ('30', 0.71, 0.369, 0.86, 'black', 0.49, 'C')]), 'R2-1 panel'),
    ('signpostreducedspeedahead', BOOK_LINES(R, 11, [('REDUCED', 0.233, 0.15, 0.86, 'black', 0.5, 'B'), ('SPEED', 0.503, 0.15, 0.86, 'black', 0.5, 'B'), ('AHEAD', 0.773, 0.144, 0.86, 'black', 0.49, 'B')]), 'R2-1 panel'),
    ('restrictedareasign', PANEL_LINES([('NO TRESPASSING', 0.42, 0.065, 0.84), ('BEYOND THIS POINT', 0.54, 0.065, 0.84),
                                        ('PHOTOGRAPHY', 0.66, 0.065, 0.84), ('IS PROHIBITED', 0.78, 0.065, 0.84)],
                                       art=_header_band('RESTRICTED AREA', 0.2)), 'facility placard'),
    ('forestryvehiclesonlysign', PANEL(['FORESTRY', 'VEHICLES', 'ONLY'], 'white', band=(0.12, 0.88), width=0.8,
     layout=[('FORESTRY', 0.291, 0.133, 0.86, 0.5, 'D'), ('VEHICLES', 0.51, 0.133, 0.86, 0.5, 'D'), ('ONLY', 0.729, 0.131, 0.86, 0.5, 'D')]),
     'white panel'),
    ('signhov6a9a', BOOK_LINES(R, 49, [('HOV 2+', 0.4, 0.1, 0.8), ('ONLY', 0.55, 0.1, 0.8),
                                       ('6AM-9AM', 0.72, 0.075, 0.8), ('MON-FRI', 0.85, 0.075, 0.8)]),
     'R3-11c panel (6-9)'),
    ('signhov2onlyoverhead', BOOK_LINES(R, 55, [('HOV 2+', 0.2, 0.15, 0.62, 'black', 0.59),
                                                ('ONLY', 0.43, 0.15, 0.62, 'black', 0.59),
                                                ('6AM-9AM', 0.66, 0.075, 0.34, 'black', 0.27),
                                                ('MON-FRI', 0.66, 0.075, 0.34, 'black', 0.78)]), 'R3-12a (lines raised)'),
    ('signhovahead', SHS(R, 50, replace=('HOV 2+', 'HOV')), 'R3-14 (HOV)'),
    ('signhovends', BOOK_LINES(R, 51, [('HOV 2+', 0.125, 0.085, 0.8), ('LANE', 0.255, 0.085, 0.8), ('ENDS', 0.875, 0.085, 0.8)]), 'R3-15 (HOV 2+)'),
    ('signhovrules', BOOK_LINES(R, 49, [('HOV 2+ ONLY', 0.42, 0.075, 0.84), ('2 OR MORE', 0.58, 0.07, 0.84),
                                        ('PERSONS', 0.71, 0.07, 0.84), ('PER VEHICLE', 0.84, 0.07, 0.84)]),
     'R3-11c panel (R3-13 legend)'),
    ('ladotantigridlockzone', PANEL(['ANTI-GRIDLOCK', 'ZONE', 'L.A.M.C. 80.70'], 'red', ink='white',
                                    band=(0.24, 0.76), width=0.8,
     layout=[('ANTI-GRIDLOCK', 0.369, 0.07, 0.86, 0.5, 'D'), ('ZONE', 0.493, 0.068, 0.86, 0.5, 'D'), ('L.A.M.C. 80.70', 0.619, 0.07, 0.86, 0.5, 'D')]), 'red panel'),
    ('signresidentlarge', PANEL_LINES([('PERMIT PARKING', 0.159, 0.111, 0.86, 0.5, 'B'), ('FOR', 0.313, 0.111, 0.86, 0.5, 'B'), ('RESIDENTS ONLY', 0.468, 0.111, 0.86, 0.5, 'B'), ('VEHICLES WITHOUT VALID', 0.602, 0.054, 0.86, 0.5, 'C'), ('PARKING PERMITS', 0.692, 0.053, 0.86, 0.5, 'C'), ('WILL BE TOWED AT', 0.784, 0.053, 0.86, 0.5, 'C'), ("VEHICLE OWNER'S EXPENSE", 0.875, 0.054, 0.86, 0.5, 'C')], condense=0.74),
     'white panel, narrow series'),
    ('signleftplaque', PANEL(['LEFT'], 'white', band=(0.2, 0.8), cap_max=0.45, width=0.6,
     layout=[('LEFT', 0.516, 0.59, 0.86, 0.5, 'B')]), 'white plaque'),
    ('signltyofy', FLASHING_YELLOW_YIELD(), 'R10-12 (flashing yellow arrow)'),
    ('signnodumping', PANEL(['NO', 'DUMPING'], 'white', band=(0.14, 0.86), width=0.8,
     layout=[('NO', 0.293, 0.25, 0.86, 0.5, 'B'), ('DUMPING', 0.688, 0.207, 0.86, 0.5, 'B')]), 'white panel'),
    ('signnobridgefishing', PANEL(['NO', 'FISHING', 'FROM', 'BRIDGE'], 'white', band=(0.08, 0.92), width=0.8,
     layout=[('NO', 0.179, 0.166, 0.86, 0.5, 'C'), ('FISHING', 0.418, 0.121, 0.86, 0.5, 'D'), ('FROM', 0.625, 0.121, 0.86, 0.5, 'D'), ('BRIDGE', 0.832, 0.121, 0.86, 0.5, 'D')]),
     'white panel'),
    ('noforestparkingsign', PANEL(['NO', 'FOREST', 'PARKING'], 'white', ink='red', band=(0.14, 0.86), width=0.8,
     layout=[('NO', 0.289, 0.133, 0.86, 0.5, 'B'), ('FOREST', 0.504, 0.133, 0.86, 0.51, 'C'), ('PARKING', 0.719, 0.133, 0.86, 0.5, 'C')]),
     'white panel, red'),
    ('signnoleftred', PANEL_LINES([('ON RED', 0.83, 0.11, 0.8)], art=_book_symbol(R, 24, (0.17, 0.11, 0.83, 0.67))),
     'R3-2 symbol + ON RED'),
    ('signarchery', SYM(G, 153, [0], 'brown'), 'archer (brown)'),
    ('signmotorbike', SYM(G, 151, [4, 5], 'brown'), 'trail bike (brown)'),
    ('signoffroad', SYM(G, 151, [0, 1, 2], 'brown'), 'off-road vehicle (brown)'),
    ('signfamily', SYM(G, 143, [3, 4, 5, 6, 7, 8], 'brown'), 'family (brown)'),
    ('signhikingbrown', SYM(G, 149, [0, 1, 2], 'brown'), 'hiking (brown)'),
    ('signaheadbrown', SYM_ART('brown', _page_arrow(21, 2)), 'M6-3 arrow (brown)'),
    ('signbrownleft', SYM_ART('brown', _page_arrow(20, 4, turn=180)), 'M6-1 arrow (brown)'),
    ('signparkingarea1mile', PANEL(['PARKING AREA', '1 MILE'], 'blue', ink='white', band=(0.14, 0.86), width=0.8,
     layout=[('PARKING AREA', 0.33, 0.246, 0.86, 0.5, 'B'), ('1 MILE', 0.687, 0.236, 0.86, 0.5, 'B')]),
     'blue panel (D5-3 legend)'),
    ('signscenicoverlookright', PANEL(['SCENIC', 'OVERLOOK'], 'blue', ink='white', arrow='right', band=(0.06, 0.52),
                                      cap_max=0.17, width=0.8), 'blue panel + M6-2 arrow'),
    ('reportdrunkdriversign', PANEL(['REPORT', 'DRUNK', 'DRIVERS'], 'blue', ink='white', band=(0.07, 0.73),
                                    width=0.78, footer=('CALL 911', 0.75),
     layout=[('REPORT', 0.17, 0.113, 0.86, 0.51, 'D'), ('DRUNK', 0.364, 0.111, 0.86, 0.51, 'D'), ('DRIVERS', 0.556, 0.115, 0.86, 0.51, 'D')]), 'blue panel'),
    ('signsignalremovalstudy', PANEL(['SIGNAL', 'UNDER', 'STUDY FOR', 'REMOVAL'], 'blue', ink='white',
                                     band=(0.08, 0.92), width=0.78,
     layout=[('SIGNAL', 0.156, 0.133, 0.86, 0.5, 'C'), ('UNDER', 0.39, 0.131, 0.86, 0.5, 'C'), ('STUDY FOR', 0.621, 0.133, 0.86, 0.5, 'C'), ('REMOVAL', 0.854, 0.129, 0.86, 0.5, 'C')]), 'blue panel'),
    ('altextremeheatdangersign', PANEL(['CAUTION!', 'EXTREME', 'HEAT', 'DANGER'], 'brown', ink='white',
                                       band=(0.08, 0.92), width=0.76,
     layout=[('CAUTION!', 0.205, 0.1, 0.86, 0.5, 'D'), ('EXTREME', 0.391, 0.109, 0.86, 0.5, 'D'), ('HEAT', 0.591, 0.111, 0.86, 0.5, 'C'), ('DANGER', 0.796, 0.119, 0.86, 0.5, 'D')]), 'brown panel'),
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
        composed = source()[0]
        if isinstance(composed, ComposedFace) and composed.extras:
            for name, img in composed.extras(info['aspect']).items():
                targets.append((os.path.join(os.path.dirname(info['texture']), name), img))
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
