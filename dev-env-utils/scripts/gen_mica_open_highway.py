#!/usr/bin/env python3
"""Build the Mica Open Highway fonts (FHWA Standard Alphabets, Series B, C, D, E, E(M), F).

Source: FHWA *Standard Alphabets for Traffic Control Devices* (2000 edition), fetched by
``shs_signs`` into ``_shs_cache/Alphabets.pdf``. Each series in that book is a spacing table (the
left bearing, width and right bearing of every character in inches at a 4-inch capital) followed by
pages drawing every character on a grid. The outlines here are traced from those drawings and the
spacing is the table's; nothing is taken from the font programs embedded in the PDF.

For each letter page: the grid (the page's line art) is removed, the page is rendered in grey, the
ink is split into connected blobs, blobs are grouped into rows and into characters (a dot joins its
stem; the two marks of a quotation mark join) and matched in reading order to the page's character
sequence. Each character is traced to curves (potracer), converted to TrueType quadratics, scaled so
the capital H is 700 units, sat on its row's baseline, and centred in the table's width box at the
table's left bearing. The word space is the book's, measured off its sign legends.

    python dev-env-utils/scripts/gen_mica_open_highway.py            # write assets/fonts/*.ttf
    python dev-env-utils/scripts/gen_mica_open_highway.py --check    # fail if the files differ
    python dev-env-utils/scripts/gen_mica_open_highway.py --only D   # one series

Needs PyMuPDF, fontTools and potracer (``pip install potracer``).
"""

import io
import os
import re
import sys
from collections import Counter

import fitz
import numpy as np
import potrace
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.cu2quPen import Cu2QuPen
from fontTools.pens.ttGlyphPen import TTGlyphPen

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import shs_signs as shs  # noqa: E402

OUT_DIR = os.path.join(layout.REPO_ROOT, 'assets', 'fonts')
FAMILY = 'Mica Open Highway'
VERSION = '1.000'
COPYRIGHT = 'Copyright 2026 Mica Technologies. Letterforms after the FHWA Standard Alphabets (public domain).'

UPM = 1000
CAP = 700                        # capital height in font units: the book's 4 inches
INCH = CAP / 4.0
SCALE = 8.0                      # page render scale, px per pt (a 2-inch capital is ~1160 px)

# series: (file suffix, style name, table page, letter pages, word space in capitals)
SERIES = {
    'B': ('SeriesB', 'Series B', 5, range(6, 11), 0.49),
    'C': ('SeriesC', 'Series C', 11, range(12, 18), 0.49),
    'D': ('SeriesD', 'Series D', 18, range(19, 26), 0.49),
    'E': ('SeriesE', 'Series E', 26, range(27, 34), 0.69),
    'EM': ('SeriesEM', 'Series E Modified', 34, range(35, 43), 0.69),
    'F': ('SeriesF', 'Series F', 43, range(44, 52), 0.69),
}

# the table names a few characters in words; the PDF's text layer gives the cent sign as U+FFFD
TABLE_NAMES = {'aster': '*', 'period': '.', 'comma / apos': ',', 'colon': ':', 'hyphen': '-', '�': '¢'}
PAGE_CHARS = {'�': '¢'}
# spacing table misprints: a bearing out of line with every other series and with the book's own sign
# legends set in that series
ERRATA = {('E', 'k'): (0.36, 2.401, 0.08),        # printed right bearing 2.08
          ('D', '3'): (0.32, 2.723, 0.4)}         # printed left bearing .72
GLYPH_NAMES = {' ': 'space', '&': 'ampersand', '!': 'exclam', '"': 'quotedbl', '#': 'numbersign',
               '$': 'dollar', '¢': 'cent', '/': 'slash', '*': 'asterisk', '.': 'period', ',': 'comma',
               ':': 'colon', '(': 'parenleft', ')': 'parenright', '-': 'hyphen', '@': 'at', '=': 'equal',
               '+': 'plus', '?': 'question', "'": 'quotesingle', '’': 'quoteright',
               '0': 'zero', '1': 'one', '2': 'two', '3': 'three', '4': 'four', '5': 'five', '6': 'six',
               '7': 'seven', '8': 'eight', '9': 'nine'}
# characters whose bottom sits exactly on the baseline (round letters overshoot it, descenders pass it)
FLAT_BOTTOM = set('BDEFHIKLMNPRTXZ1247hiklmnrxz#/')
DOT_BOTTOM = set('.:?!')


def book():
    return fitz.open(shs._fetch(shs.BOOK_URL % 'Alphabets', 'Alphabets.pdf'))


def parse_table(doc, pno):
    """{character: (left, width, right)} in inches at a 4-inch capital."""
    words = doc[pno].get_text('words')
    mid = doc[pno].rect.width / 2
    rows = {}
    for w in words:
        rows.setdefault((round(w[1] / 3), w[0] < mid), []).append(w)
    table = {}
    for ws in rows.values():
        toks = [w[4] for w in sorted(ws, key=lambda w: w[0])]
        if len(toks) < 4 or not all(re.fullmatch(r'\d*\.?\d+', t) for t in toks[-3:]):
            continue
        name = ' '.join(toks[:-3])
        table[TABLE_NAMES.get(name, name)] = tuple(float(t) for t in toks[-3:])
    return table


def page_ink(doc, pno):
    tmp = fitz.open()
    tmp.insert_pdf(doc, from_page=pno, to_page=pno)
    page = tmp[0]
    page.add_redact_annot(page.rect)
    page.apply_redactions(images=fitz.PDF_REDACT_IMAGE_NONE,
                          graphics=fitz.PDF_REDACT_LINE_ART_REMOVE_IF_TOUCHED, text=fitz.PDF_REDACT_TEXT_NONE)
    pix = page.get_pixmap(matrix=fitz.Matrix(SCALE, SCALE), colorspace=fitz.csGRAY, alpha=False)
    img = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width)
    # the page label is white on a black box that went with the line art, so it has no ink
    text = re.sub(r'SERIES [A-Z]+ 2000', '', doc[pno].get_text())
    chars = [PAGE_CHARS.get(c, c) for c in text if not c.isspace()]
    return img < 128, chars


def blobs(ink):
    """Bounding boxes (x0, y0, x1, y1) of 4-connected ink blobs, by run-length union-find."""
    parent = []

    def find(a):
        while parent[a] != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a
    runs, prev = [], []
    for y in range(ink.shape[0]):
        row = ink[y]
        if not row.any():
            prev = []
            continue
        d = np.diff(np.concatenate(([0], row.astype(np.int8), [0])))
        cur = []
        for s, e in zip(np.nonzero(d == 1)[0], np.nonzero(d == -1)[0]):
            mine = None
            for ps, pe, pl in prev:
                if ps < e and pe > s:
                    r = find(pl)
                    if mine is None:
                        mine = r
                    elif r != mine:
                        parent[r] = mine
            if mine is None:
                parent.append(len(parent))
                mine = len(parent) - 1
            cur.append((s, e, mine))
            runs.append((y, s, e, mine))
        prev = cur
    boxes = {}
    for y, s, e, l in runs:
        r = find(l)
        b = boxes.get(r)
        boxes[r] = (s, y, e, y + 1) if b is None else (min(b[0], s), min(b[1], y), max(b[2], e), max(b[3], y + 1))
    return [b for b in boxes.values() if (b[2] - b[0]) * (b[3] - b[1]) > 64]


def characters(ink, chars):
    """[(char, box, baseline)] in reading order."""
    allb = blobs(ink)
    tall = 144.0 * SCALE                                        # the pages draw a 2-inch capital
    big = [b for b in allb if b[3] - b[1] > 0.45 * tall]
    small = [b for b in allb if b[3] - b[1] <= 0.45 * tall]
    # rows from the letters proper; dots, periods, hyphens and quote marks join a row afterwards,
    # since a dot can sit clear of every other blob in its own row
    rows = []
    for b in sorted(big, key=lambda b: b[1]):
        for r in rows:
            if min(b[3], r[3]) - max(b[1], r[1]) > 0.3 * (b[3] - b[1]):
                r[0].append(b)
                r[1], r[3] = min(r[1], b[1]), max(r[3], b[3])
                break
        else:
            rows.append([[b], b[1], None, b[3]])
    for b in small:
        cy = (b[1] + b[3]) / 2.0
        inside = [r for r in rows if r[1] <= cy <= r[3]]
        pool = inside or rows
        r = min(pool, key=lambda r: abs(cy - (r[1] + r[3]) / 2.0))
        r[0].append(b)
    rows.sort(key=lambda r: r[1])
    groups = []
    for r in rows:
        merged = []
        for b in sorted(r[0], key=lambda b: b[0]):
            m = merged[-1] if merged else None
            # a dot over its stem overlaps it almost entirely in x; a slash leaning into the next
            # character only clips a corner
            if m and min(b[2], m[2]) - max(b[0], m[0]) > 0.5 * min(b[2] - b[0], m[2] - m[0]):
                m = merged[-1]
                merged[-1] = (min(m[0], b[0]), min(m[1], b[1]), max(m[2], b[2]), max(m[3], b[3]))
            else:
                merged.append(b)
        groups.append(merged)
    # a quotation mark is two marks side by side: join the closest pair until the counts agree
    while sum(len(g) for g in groups) > len(chars):
        best = None
        for gi, g in enumerate(groups):
            for i in range(len(g) - 1):
                if max(g[i][3] - g[i][1], g[i + 1][3] - g[i + 1][1]) > 0.45 * tall:
                    continue                                   # only two small marks make one character
                gap = g[i + 1][0] - g[i][2]
                if best is None or gap < best[0]:
                    best = (gap, gi, i)
        if best is None:
            break
        _, gi, i = best
        a, b = groups[gi][i], groups[gi][i + 1]
        groups[gi][i:i + 2] = [(a[0], min(a[1], b[1]), b[2], max(a[3], b[3]))]
    if sum(len(g) for g in groups) != len(chars):
        raise SystemExit('found %d characters, the page lists %d: %s' % (sum(len(g) for g in groups), len(chars), ''.join(chars)))
    out, k = [], 0
    for g in groups:
        row_chars = chars[k:k + len(g)]
        k += len(g)
        flat = [b[3] for b, c in zip(g, row_chars) if c in FLAT_BOTTOM]
        dots = [b[3] for b, c in zip(g, row_chars) if c in DOT_BOTTOM]
        if flat:
            baseline = float(np.median(flat))
        elif dots:
            # a round dot overshoots the baseline by about as much as an O does
            baseline = float(np.median(dots)) - 0.012 * 144.0 * SCALE
        else:
            # only round letters: the highest bottom in the row is the baseline plus an overshoot;
            # snapped to a measured row later
            baseline = -float(min(b[3] for b in g))
        out += [(c, b, baseline) for c, b in zip(row_chars, g)]
    return out


def snap_baselines(found, measured):
    """A row of only round letters (which overshoot the baseline) takes the nearest baseline measured
    off a flat-bottomed row: on its own page if one is close, otherwise anywhere in the series. The
    letter pages mostly share one grid, but the punctuation pages sit lower, so there is no single
    pitch to snap to."""
    for c, (pno, box, base) in list(found.items()):
        if base >= 0:
            continue
        est = -base
        pool = [b for p, b in measured if p == pno and abs(b - est) < 60] or [b for _p, b in measured]
        found[c] = (pno, box, min(pool, key=lambda b: abs(b - est)))


def trace(ink, box, pad=4):
    x0, y0, x1, y1 = box
    crop = ink[y0 - pad:y1 + pad, x0 - pad:x1 + pad]
    return potrace.Bitmap(~crop).trace(turdsize=20, alphamax=1.0, opticurve=True, opttolerance=0.2), \
        (x0 - pad, y0 - pad)


def outline(path, off, box, baseline, cap_px, left, width, dy=0.0):
    """TrueType glyph: ink centred in the table's width box, which starts at the left bearing."""
    k = CAP / cap_px
    extra = (width * INCH - (box[2] - box[0]) * k) / 2.0
    pen = TTGlyphPen(None)
    q = Cu2QuPen(pen, max_err=0.8, reverse_direction=True)

    def T(p):
        return (int(round((p.x + off[0] - box[0]) * k + left * INCH + extra)),
                int(round((baseline - (p.y + off[1])) * k + dy)))
    for curve in path:
        q.moveTo(T(curve.start_point))
        for seg in curve:
            if seg.is_corner:
                q.lineTo(T(seg.c))
                q.lineTo(T(seg.end_point))
            else:
                q.curveTo(T(seg.c1), T(seg.c2), T(seg.end_point))
        q.closePath()
    return pen.glyph()


def notdef():
    pen = TTGlyphPen(None)
    for pts in ([(50, 0), (50, CAP), (450, CAP), (450, 0)], [(100, 50), (400, 50), (400, CAP - 50), (100, CAP - 50)]):
        pen.moveTo(pts[0])
        for p in pts[1:]:
            pen.lineTo(p)
        pen.closePath()
    return pen.glyph()


def build(key, doc):
    suffix, style, table_page, pages, word_space = SERIES[key]
    table = parse_table(doc, table_page)
    table.update({c: v for (k, c), v in ERRATA.items() if k == key})
    found, measured = {}, set()
    for pno in pages:
        ink, chars = page_ink(doc, pno)
        for c, box, baseline in characters(ink, chars):
            if baseline >= 0:
                measured.add((pno, baseline))
            if c not in found:                                  # a character repeated across pages
                found[c] = (pno, box, baseline)
    snap_baselines(found, sorted(measured))
    hb = found['H'][1]
    cap_px = hb[3] - hb[1]

    glyphs = {'.notdef': notdef(), 'space': TTGlyphPen(None).glyph()}
    metrics = {'.notdef': (500, 50), 'space': (int(round(word_space * CAP)), 0)}
    cmap = {0x20: 'space', 0xA0: 'space'}
    by_page = {}
    for c, (pno, box, baseline) in found.items():
        by_page.setdefault(pno, []).append((c, box, baseline))
    for pno in sorted(by_page):
        ink, _chars = page_ink(doc, pno)
        for c, box, baseline in by_page[pno]:
            if c not in table:
                raise SystemExit('Series %s: %r is drawn but not in the table' % (key, c))
            left, width, right = table[c]
            ink_w = (box[2] - box[0]) / cap_px * 4.0
            if abs(ink_w - width) > 0.3:
                # a table row that disagrees with its own drawing (Series D's '=' and '"'): keep the
                # drawing's width, the table's bearings
                print('  Series %s %r: table width %.3f, drawn %.3f -- using the drawing' % (key, c, width, ink_w))
                width = round(ink_w, 3)
            path, off = trace(ink, box)
            name = GLYPH_NAMES.get(c, c)
            glyphs[name] = outline(path, off, box, baseline, cap_px, left, width)
            metrics[name] = (int(round((left + width + right) * INCH)), glyphs[name].xMin if hasattr(glyphs[name], 'xMin') else 0)
            cmap[ord(c)] = name
            if c == ',':
                # no alphabet draws an apostrophe: the comma, raised so its top meets the capital line
                top = (box[1] - baseline) * -CAP / cap_px
                glyphs['quotesingle'] = outline(path, off, box, baseline, cap_px, left, width, dy=CAP - top)
                metrics['quotesingle'] = metrics[name]
                cmap[ord("'")] = cmap[0x2019] = 'quotesingle'
    order = ['.notdef', 'space'] + sorted(n for n in glyphs if n not in ('.notdef', 'space'))

    fb = FontBuilder(UPM, isTTF=True)
    fb.setupGlyphOrder(order)
    fb.setupCharacterMap(cmap)
    fb.setupGlyf(glyphs)
    glyf = fb.font['glyf']
    hmtx = {}
    for n in order:
        g = glyf[n]
        g.recalcBounds(glyf)
        hmtx[n] = (metrics[n][0], getattr(g, 'xMin', 0) if g.numberOfContours else 0)
    fb.setupHorizontalMetrics(hmtx)
    y_max = max(getattr(glyf[n], 'yMax', 0) for n in order)
    y_min = min(getattr(glyf[n], 'yMin', 0) for n in order)
    x_height = int(round(2.9 * INCH))
    fb.setupHorizontalHeader(ascent=y_max, descent=y_min)
    full = '%s %s' % (FAMILY, style)
    fb.setupNameTable({
        'copyright': COPYRIGHT,
        'familyName': full, 'styleName': 'Regular',
        'uniqueFontIdentifier': '%s;%s' % (full.replace(' ', ''), VERSION),
        'fullName': full, 'version': 'Version ' + VERSION, 'psName': 'MicaOpenHighway-' + suffix,
        'typographicFamily': FAMILY, 'typographicSubfamily': style,
        'licenseDescription': 'This Font Software is licensed under the SIL Open Font License, Version 1.1.',
        'licenseInfoURL': 'https://openfontlicense.org',
    })
    fb.setupOS2(sTypoAscender=y_max, sTypoDescender=y_min, sTypoLineGap=0, usWinAscent=y_max,
                usWinDescent=-y_min, sxHeight=x_height, sCapHeight=CAP, fsType=0, achVendID='MICA')
    fb.setupPost()
    fb.font['head'].created = fb.font['head'].modified = 3786825600      # fixed: byte-stable output
    buf = io.BytesIO()
    fb.save(buf)
    return 'MicaOpenHighway-%s.ttf' % suffix, buf.getvalue()


def main():
    args = sys.argv[1:]
    check = '--check' in args
    only = args[args.index('--only') + 1].split(',') if '--only' in args else list(SERIES)
    doc = book()
    os.makedirs(OUT_DIR, exist_ok=True)
    drift = []
    for key in only:
        name, data = build(key, doc)
        path = os.path.join(OUT_DIR, name)
        if check:
            if not os.path.exists(path) or open(path, 'rb').read() != data:
                drift.append(path)
        else:
            with open(path, 'wb') as fh:
                fh.write(data)
            print('wrote %s (%d bytes)' % (name, len(data)))
    if check:
        if drift:
            print('DRIFT: %d font(s) differ from the generator:' % len(drift))
            for p in drift:
                print('  ' + p)
            sys.exit(1)
        print('all %d fonts match the generator' % len(only))


if __name__ == '__main__':
    main()
