"""Official sign faces from the FHWA Standard Highway Signs (SHS) drawings.

The MUTCD's sign designs are published by FHWA as vector drawings -- public domain, being a
US government work -- in two places:

  * the 2004 book, one chapter PDF per series (``/SHSe/Regulatory.pdf``, ``Warning.pdf``,
    ``School.pdf``, ...), each page a dimensioned drawing of one or two signs;
  * the interim per-sign ZIPs for signs added or changed since (``/shsm_interim/zip_files/
    <code>.zip``), each holding EPS + PDF at every standard size, page 2 of each PDF being the
    clean undimensioned layout.

This module fetches those into a cache and renders a sign face out of them at any size, so a
generated sign carries the real design rather than a hand-drawn approximation.

A book page is not usable as-is: dimension lines, arrowheads and text are drawn over and
around the sign. But everything on the page is vector, and PyMuPDF exports it as SVG with
every glyph a ``<use>`` of the font's own outline. The sign is the filled paths above an
arrowhead's size, the undashed strokes heavier than a dimension line, and the glyphs set in a
Highway Gothic font (captions and dimensions are Nimbus Sans); everything else on the page is
dropped, the sign's own outer fill gives the crop, and the result is rendered with alpha, so
a diamond or a pennant keeps its silhouette.

    from shs_signs import book_sign, interim_sign, fit_plate
    face = fit_plate(book_sign('Warning', 97), 1.0)        # W11-7 on a square plate, 128 px
    face = fit_plate(interim_sign('w08_14', '24x24'), 1.0)  # W8-14

Downloads go to ``dev-env-utils/scripts/_shs_cache/`` (gitignored) on first use. Rendering is
deterministic, so a generator's ``--check`` can compare its output byte for byte.
"""

import io
import os
import re
import zipfile

import fitz
from PIL import Image

try:
    from urllib.request import urlopen
except ImportError:  # pragma: no cover
    urlopen = None

CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '_shs_cache')
BOOK_URL = 'https://mutcd.fhwa.dot.gov/SHSe/%s.pdf'
INTERIM_URL = 'https://mutcd.fhwa.dot.gov/shsm_interim/zip_files/%s.zip'
RENDER_DPI = 300
MIN_FILL_PT = 6.0   # anything smaller than this is a dimension arrowhead, not sign
DEFAULT_TEX = 128


def _fetch(url, name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        print('fetching %s' % url)
        with urlopen(url, timeout=300) as r, open(path, 'wb') as fh:
            fh.write(r.read())
    return path


def book_page(chapter, page):
    """The fitz page for ``page`` (0-based) of a 2004 SHS chapter."""
    return fitz.open(_fetch(BOOK_URL % chapter, chapter + '.pdf'))[page]


def interim_pdf(code, variant=None):
    """The clean-layout page of an interim sign PDF; ``variant`` picks among the sizes /
    versions in the ZIP by substring (the first PDF when omitted)."""
    path = _fetch(INTERIM_URL % code, code + '.zip')
    z = zipfile.ZipFile(path)
    pdfs = [n for n in z.namelist() if n.lower().endswith('.pdf')]
    if variant:
        pdfs = [n for n in pdfs if variant in n] or pdfs
    if not pdfs:
        raise SystemExit('no PDF in %s' % path)
    doc = fitz.open(stream=z.read(pdfs[0]), filetype='pdf')
    # page 1 is the undimensioned layout; a single-page PDF is that layout already
    return doc[1 if doc.page_count > 1 else 0]


MIN_SIGN_STROKE_PT = 0.5  # dimension lines are 0.22 pt; a sign outline drawn as a stroke is 1 pt


def _sign_fills(page):
    """The page's sign drawings in their original draw order: filled shapes above the
    arrowhead threshold, plus stroked outlines heavier than a dimension line (a crossbuck's
    arms are drawn that way, white being the page)."""
    out = []
    for d in page.get_drawings():
        r = d['rect']
        if r.width < MIN_FILL_PT and r.height < MIN_FILL_PT:
            continue
        if d.get('fill') is not None:
            out.append(d)
        elif (d.get('color') is not None and (d.get('width') or 0) >= MIN_SIGN_STROKE_PT
              and str(d.get('dashes') or '[] 0') == '[] 0'):   # dashed = construction line
            out.append(d)
    return out


def _render_clip(page, clip, dpi=RENDER_DPI):
    pix = page.get_pixmap(dpi=dpi, clip=clip, alpha=True)
    return Image.frombytes('RGBA', (pix.width, pix.height), pix.samples)


_USE_RE = re.compile(r'<use [^>]*xlink:href="#(font_[^"]+)"[^>]*transform="matrix\(([^)]+)\)"[^>]*/>')
_PATH_RE = re.compile(r'<path (?![^>]*id="font_)[^>]*/>')
_ATTR_RE = re.compile(r'([a-z-]+)="([^"]*)"')
_NUM_RE = re.compile(r'-?\d*\.?\d+(?:e-?\d+)?')


def _path_bbox(d, matrix):
    """The bounding box of an SVG path's absolute M/L/H/V/C/Z data, through ``matrix``."""
    a, b, c, dd, e, f = matrix
    xs, ys = [], []
    x = y = 0.0
    cmd = None
    tokens = re.findall(r'[MLHVCZmlhvcz]|' + _NUM_RE.pattern, d)
    nums = []

    def flush():
        nonlocal x, y, nums
        if cmd in ('M', 'L'):
            for i in range(0, len(nums) - 1, 2):
                x, y = nums[i], nums[i + 1]
                xs.append(x); ys.append(y)
        elif cmd == 'H':
            for v in nums:
                x = v; xs.append(x); ys.append(y)
        elif cmd == 'V':
            for v in nums:
                y = v; xs.append(x); ys.append(y)
        elif cmd == 'C':
            for i in range(0, len(nums) - 5, 6):
                for j in (0, 2, 4):
                    xs.append(nums[i + j]); ys.append(nums[i + j + 1])
                x, y = nums[i + 4], nums[i + 5]
        nums = []

    for t in tokens:
        if t.isalpha():
            flush()
            cmd = t.upper()
        else:
            nums.append(float(t))
    flush()
    if not xs:
        return None
    pts = [(a * px + c * py + e, b * px + dd * py + f) for px, py in zip(xs, ys)]
    return fitz.Rect(min(p[0] for p in pts), min(p[1] for p in pts),
                     max(p[0] for p in pts), max(p[1] for p in pts))


# SeriesB2000 ... SeriesF2000 and HighwayC98 ...: the Highway Gothic faces the legends are set
# in. Everything else on a page (captions, reference letters, dimensions) is Nimbus Sans.
LEGEND_FONT_PREFIXES = ('Series', 'Highway')


def _legend_glyph_origins(page, rect):
    """Origins of every character set in a legend font inside ``rect``."""
    out = []
    for block in page.get_text('rawdict')['blocks']:
        for line in block.get('lines', []):
            for span in line['spans']:
                font = span['font'].split('+', 1)[-1]
                if not font.startswith(LEGEND_FONT_PREFIXES):
                    continue
                for ch in span['chars']:
                    ox, oy = ch['origin']
                    if rect.contains(fitz.Point(ox, oy)):
                        out.append((ox, oy))
    return out


def _inset_rects(page, rect, fills):
    """Small same-colour panels inside the sign's rect that are not the sign: the reduced
    left-hand copy the W1 pages draw beside the right-hand one, whose corner falls inside the
    big diamond's bounding square."""
    main = None
    for d in fills:
        if fitz.Rect(d['rect']) == rect:
            main = d
            break
    if main is None:
        return []
    area = rect.width * rect.height
    cx, cy = (rect.x0 + rect.x1) / 2, (rect.y0 + rect.y1) / 2
    insets = []
    for d in fills:
        r = d['rect']
        # the sign's own symbols share its border colour and sit inside it; the inset pokes out
        if r == rect or d.get('fill') != main.get('fill') or not rect.intersects(r) or rect.contains(r):
            continue
        if r.width * r.height > 0.2 * area:
            continue
        rcx, rcy = (r.x0 + r.x1) / 2, (r.y0 + r.y1) / 2
        if abs(rcx - cx) > 0.25 * rect.width or abs(rcy - cy) > 0.25 * rect.height:
            insets.append(fitz.Rect(r))
    return insets


def _filtered_svg(page, rect, only_inside=True):
    """The page's SVG reduced to the sign inside ``rect``: filled paths above the arrowhead
    threshold, undashed strokes heavier than a dimension line (their width taken through the
    path's own transform, which is where a scaled-up outline like the W10-1's X keeps its
    thickness), and the glyphs set in a legend font. Everything is exactly as the PDF draws it."""
    svg = page.get_svg_image()
    head_end = svg.index('</defs>') + len('</defs>')
    kept = []
    insets = _inset_rects(page, rect, _sign_fills(page)) if only_inside else []
    body = svg[head_end:]   # the defs hold the page clip as a <path> too
    for m in _PATH_RE.finditer(body):
        el = m.group(0)
        attrs = dict(_ATTR_RE.findall(el))
        mt = attrs.get('transform', '')
        nums = [float(v) for v in _NUM_RE.findall(mt)] if mt.startswith('matrix') else [1, 0, 0, 1, 0, 0]
        if len(nums) != 6:
            nums = [1, 0, 0, 1, 0, 0]
        scale = abs(nums[0] * nums[3] - nums[1] * nums[2]) ** 0.5
        bbox = _path_bbox(attrs.get('d', ''), nums)
        if bbox is None:
            continue
        fill = attrs.get('fill', '#000000') != 'none'   # SVG's default fill is black, not none
        stroke = attrs.get('stroke', 'none') != 'none'
        if fill:
            if bbox.width < MIN_FILL_PT and bbox.height < MIN_FILL_PT:
                continue
        elif stroke:
            width = float(attrs.get('stroke-width', '1')) * scale
            if width < MIN_SIGN_STROKE_PT or 'stroke-dasharray' in attrs:
                continue
            # dimension marks drawn over a black symbol are white strokes; no sign outline is
            if attrs.get('stroke', '').lower() in ('#ffffff', '#fff', 'white'):
                continue
            # A closed, sign-sized outline drawn as a stroke alone is a white panel on the
            # page's white -- a crossbuck's arms -- so it is given the white it relies on
            if attrs.get('d', '').rstrip().upper().endswith('Z') and bbox.width >= 40 and bbox.height >= 40:
                el = el.replace('fill="none"', 'fill="#ffffff"', 1)
        else:
            continue
        centre = fitz.Point((bbox.x0 + bbox.x1) / 2, (bbox.y0 + bbox.y1) / 2)
        if only_inside and not rect.contains(centre):
            continue
        if any(ins.contains(centre) for ins in insets):
            continue
        kept.append(el)
    # Legend glyphs are the ones set in a Series (Highway Gothic) font; captions, reference
    # letters and dimensions are all Nimbus Sans. Match each SVG glyph to the raw text's
    # per-character origins to tell them apart -- the SVG itself does not name the font.
    legend_origins = _legend_glyph_origins(page, rect)
    for m in _USE_RE.finditer(body):
        nums = [float(v) for v in m.group(2).split(',')]
        e, f = nums[4], nums[5]
        if not any(abs(e - ox) < 0.75 and abs(f - oy) < 0.75 for ox, oy in legend_origins):
            continue
        kept.append(m.group(0))
    return svg[:head_end] + '\n' + '\n'.join(kept) + '\n</svg>'


def _render_svg(page, rect, only_inside=True, dpi=RENDER_DPI):
    doc = fitz.open('svg', _filtered_svg(page, rect, only_inside).encode('utf-8'))
    return _render_clip(doc[0], rect, dpi)


def _outer_rects(fills):
    """The outermost sign rects on a page: fills not contained in a larger fill, with any
    that overlap merged into one -- a crossbuck is two arms, neither inside the other."""
    outer = []
    for d in sorted(fills, key=lambda d: -(d['rect'].width * d['rect'].height)):
        r = d['rect']
        if r.width < MIN_FILL_PT or r.height < MIN_FILL_PT:   # a leader line, not a sign
            continue
        if any(o.contains(r) for o in outer):
            continue
        outer.append(fitz.Rect(r))
    # Two arms of one sign are the same size; the small left-hand inset the W1 pages draw
    # beside the right-hand sign overlaps its bounding square but is a tenth of its area.
    def _same_sign(a, b):
        aa, ab = a.width * a.height, b.width * b.height
        return a.intersects(b) and min(aa, ab) * 3 >= max(aa, ab)
    merged = True
    while merged:
        merged = False
        for i in range(len(outer)):
            for j in range(i + 1, len(outer)):
                if _same_sign(outer[i], outer[j]):
                    outer[i] = outer[i] | outer[j]
                    del outer[j]
                    merged = True
                    break
            if merged:
                break
    return outer


def book_sign(chapter, page, pick=0, only_inside=True, inner=None):
    """One sign from a book page, rendered with alpha and cropped to its outline.

    ``pick`` chooses among the page's outermost sign rects, sorted top to bottom then left to
    right, for pages that carry a left- and right-hand version or a sign and its plaque.
    ``only_inside`` keeps only the fills that lie within the chosen rect (the other sign's
    fills otherwise bleed in when two signs share a page). ``inner`` instead crops to the
    n-th largest fill inside the chosen outer rect -- the panel of an in-street sign drawn
    with its post, say.
    """
    p = book_page(chapter, page)
    fills = _sign_fills(p)
    outers = _outer_rects(fills)
    outers.sort(key=lambda r: (round(r.y0 / 40), r.x0))
    rect = outers[pick]
    if inner is not None:
        inside = sorted((d['rect'] for d in fills if rect.contains(d['rect']) and d['rect'] != rect),
                        key=lambda r: -(r.width * r.height))
        rect = fitz.Rect(inside[inner])
    return _render_svg(p, rect, only_inside)


def interim_sign(code, variant=None):
    """The clean layout of an interim sign, cropped to its outline."""
    p = interim_pdf(code, variant)
    rect = _outer_rects(_sign_fills(p))[0]
    return _render_svg(p, rect, only_inside=False)


def recolour(img, mapping, tol=60):
    """Map each pixel within ``tol`` (RGB distance) of a source colour to its target; pixels
    near none of them are left alone. The drawings print their colours as an artist's
    approximation (the book's yellow is #fff500, the interim files' #ffd046) so the faces are
    brought onto one palette here; done on the full-resolution render, where an anti-aliased
    edge pixel is a negligible fraction, and the downscale then blends the new colours."""
    import numpy as np
    arr = np.asarray(img.convert('RGBA')).astype(np.int32)
    rgb = arr[..., :3]
    out = arr.copy()
    best = np.full(rgb.shape[:2], tol * tol, dtype=np.int64)
    for src, dst in mapping.items():
        dist = ((rgb - np.array(src[:3], dtype=np.int32)) ** 2).sum(axis=-1)
        hit = dist < best
        best[hit] = dist[hit]
        out[..., :3][hit] = np.array(dst[:3], dtype=np.int32)
    return Image.fromarray(out.astype(np.uint8), 'RGBA')


def fit_plate(face, aspect, size=DEFAULT_TEX, margin=0.02):
    """Squish a sign face to the square texture its plate stretches back to ``aspect``.

    The face is placed on a transparent square scaled so that, once the plate's stretch is
    applied, it keeps its true proportions and fills the plate less ``margin``.
    """
    w, h = face.size
    # in plate units: the plate is aspect wide by 1 tall; the sign is w/h wide by 1 tall
    sign_aspect = w / float(h)
    if sign_aspect / aspect >= 1.0:        # width-limited
        pw = 1.0 - 2 * margin
        ph = pw * aspect / sign_aspect
    else:                                   # height-limited
        ph = 1.0 - 2 * margin
        pw = ph * sign_aspect / aspect
    # supersample, then down to the texture size
    ss = 4
    canvas = Image.new('RGBA', (size * ss, size * ss), (0, 0, 0, 0))
    tw, th = max(1, int(round(pw * size * ss))), max(1, int(round(ph * size * ss)))
    scaled = face.resize((tw, th), Image.LANCZOS)
    canvas.alpha_composite(scaled, ((size * ss - tw) // 2, (size * ss - th) // 2))
    return canvas.resize((size, size), Image.LANCZOS)


def find_pages(chapter, phrase):
    """Pages (0-based) of a chapter whose text contains ``phrase`` -- for locating a sign."""
    doc = fitz.open(_fetch(BOOK_URL % chapter, chapter + '.pdf'))
    return [i for i, p in enumerate(doc) if phrase in p.get_text().replace('\n', ' ')]


if __name__ == '__main__':
    import sys
    # shs_signs.py find <Chapter> <phrase>   |   shs_signs.py book <Chapter> <page> [pick] out.png
    if sys.argv[1] == 'find':
        print(find_pages(sys.argv[2], ' '.join(sys.argv[3:])))
    elif sys.argv[1] == 'book':
        pick = int(sys.argv[4]) if len(sys.argv) > 5 else 0
        book_sign(sys.argv[2], int(sys.argv[3]), pick).save(sys.argv[-1])
    elif sys.argv[1] == 'interim':
        interim_sign(sys.argv[2], sys.argv[3] if len(sys.argv) > 4 else None).save(sys.argv[-1])
    else:
        raise SystemExit(__doc__)
