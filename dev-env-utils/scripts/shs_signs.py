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
from PIL import Image, ImageDraw

try:
    from urllib.request import urlopen
except ImportError:  # pragma: no cover
    urlopen = None

CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '_shs_cache')
BOOK_URL = 'https://mutcd.fhwa.dot.gov/SHSe/%s.pdf'
INTERIM_URL = 'https://mutcd.fhwa.dot.gov/shsm_interim/zip_files/%s.zip'
RENDER_DPI = 300
MIN_FILL_PT = 6.0   # anything smaller than this is a dimension arrowhead, not sign
MIN_SIGN_PT = 20.0  # no sign on these pages is drawn smaller than this; arrowheads are 6 pt
MAX_MARK_AREA_PT = 80.0  # a white dimension bar is 1 pt wide; no white sign element is this small
SHEET_MARGIN_PT = 20.0   # a sign's edge outline sits within this of its outermost fill
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


def _is_light(colour):
    """Whether an SVG colour (``#rrggbb`` / ``#rgb`` / ``white``) is light enough to be a
    dimension mark drawn over a dark shape rather than sign artwork."""
    c = colour.strip().lower()
    if c == 'white':
        return True
    if not c.startswith('#') or len(c) not in (4, 7):
        return False
    if len(c) == 4:
        c = '#' + ''.join(ch * 2 for ch in c[1:])
    r, g, b = (int(c[i:i + 2], 16) for i in (1, 3, 5))
    return 0.299 * r + 0.587 * g + 0.114 * b > 200


def _path_area(d, matrix):
    """The area (page units) enclosed by an SVG path's straight-line subpaths through
    ``matrix``; curves are taken through their control points, which is close enough to
    tell a hairline bar from a shape."""
    a, b, c, dd, e, f = matrix
    total = 0.0
    pts = []

    def close():
        nonlocal total, pts
        if len(pts) >= 3:
            s = 0.0
            for i in range(len(pts)):
                x0, y0 = pts[i]
                x1, y1 = pts[(i + 1) % len(pts)]
                s += x0 * y1 - x1 * y0
            total += abs(s) / 2.0
        pts = []

    cmd = None
    nums = []
    for t in re.findall(r'[MLHVCZmlhvcz]|' + _NUM_RE.pattern, d):
        if t.isalpha():
            if cmd in ('M', 'L', 'C'):
                for i in range(0, len(nums) - 1, 2):
                    pts.append((nums[i], nums[i + 1]))
            elif cmd == 'H':
                for v in nums:
                    pts.append((v, pts[-1][1] if pts else 0.0))
            elif cmd == 'V':
                for v in nums:
                    pts.append((pts[-1][0] if pts else 0.0, v))
            nums = []
            cmd = t.upper()
            if cmd == 'Z':
                close()
            elif cmd == 'M':
                close()
        else:
            nums.append(float(t))
    if cmd in ('L', 'C', 'M'):
        for i in range(0, len(nums) - 1, 2):
            pts.append((nums[i], nums[i + 1]))
    close()
    return total * abs(a * dd - b * c)


# SeriesB2000 ... SeriesF2000 and HighwayC98 ...: the Highway Gothic faces the legends are set
# in. Everything else on a page (captions, reference letters, dimensions) is Nimbus Sans.
LEGEND_FONT_PREFIXES = ('Series', 'Highway')


def _legend_spans(page, rect):
    """Every run of legend-font text inside ``rect``: (text, [(ox, oy), ...], bbox)."""
    out = []
    for block in page.get_text('rawdict')['blocks']:
        for line in block.get('lines', []):
            for span in line['spans']:
                font = span['font'].split('+', 1)[-1]
                if not font.startswith(LEGEND_FONT_PREFIXES):
                    continue
                chars = [ch for ch in span['chars'] if rect.contains(fitz.Point(*ch['origin']))]
                if not chars:
                    continue
                bbox = fitz.Rect(chars[0]['bbox'])
                for ch in chars[1:]:
                    bbox |= fitz.Rect(ch['bbox'])
                out.append((''.join(ch['c'] for ch in chars).strip(),
                            [tuple(ch['origin']) for ch in chars], bbox))
    return out


_GLYPH_DEF_RE = re.compile(r'<path id="(font_[^"]+)" d="([^"]*)"')


def _legend_span_boxes(page, rect, text):
    """The drawn extent (page units) of each legend run reading ``text``: the union of its
    glyph outlines through their placement, which is the cap height the numerals really have
    -- the raw text box is the font's full ascender-to-descender and far too tall."""
    svg = page.get_svg_image()
    head_end = svg.index('</defs>')
    defs = {m.group(1): m.group(2) for m in _GLYPH_DEF_RE.finditer(svg[:head_end])}
    uses = {}
    for m in _USE_RE.finditer(svg[head_end:]):
        nums = [float(v) for v in m.group(2).split(',')]
        uses.setdefault((round(nums[4], 2), round(nums[5], 2)), (m.group(1), nums))
    boxes = []
    for t, origins, _bbox in _legend_spans(page, rect):
        if t != text:
            continue
        box = None
        for ox, oy in origins:
            hit = uses.get((round(ox, 2), round(oy, 2)))
            if hit is None:
                continue
            gb = _path_bbox(defs.get(hit[0], ''), hit[1])
            if gb is not None:
                box = gb if box is None else box | gb
        if box is not None:
            boxes.append(box)
    return boxes


def _legend_glyph_origins(page, rect, drop=None):
    """Origins of every character set in a legend font inside ``rect``, less the spans whose
    text is in ``drop`` (a string or a list of them)."""
    drops = set() if drop is None else ({drop} if isinstance(drop, str) else set(drop))
    return [o for text, origins, _bbox in _legend_spans(page, rect)
            if text not in drops for o in origins]


def _sheet_colour(fills, rect):
    """What the margin between a sign's outermost fill and its edge outline is: the sign's own
    colour for the warning family (a yellow diamond's edge is yellow) and white for everything
    else (a regulatory sign's sheet shows outside its black border, a guide sign's outside its
    blue panel)."""
    for d in fills:
        r = d['rect']
        if d.get('fill') is None or abs(r.x0 - rect.x0) > 1.5 or abs(r.y0 - rect.y0) > 1.5                 or abs(r.x1 - rect.x1) > 1.5 or abs(r.y1 - rect.y1) > 1.5:
            continue
        red, green, blue = d['fill'][:3]
        # Only the warning family (yellow, orange, fluorescent yellow-green) runs its colour
        # to the edge; regulatory, guide and services signs all sit on a white sheet
        if red > 0.6 and green > 0.5 and blue < 0.45:
            return '#%02x%02x%02x' % (int(round(red * 255)), int(round(green * 255)), int(round(blue * 255)))
        return '#ffffff'
    return '#ffffff'


def _sheet_rect(page, rect):
    """The sign's own edge, when the page draws it: a closed stroke-only outline (any
    width) enclosing the outermost fill by a few points on every side. The white margin
    between it and the border is the sign's sheet, not the page."""
    best = None
    for d in page.get_drawings():
        r = d['rect']
        if d.get('fill') is not None or d.get('color') is None:
            continue
        if str(d.get('dashes') or '[] 0') != '[] 0':
            continue
        margins = (rect.x0 - r.x0, rect.y0 - r.y0, r.x1 - rect.x1, r.y1 - rect.y1)
        if not all(-0.5 <= m <= SHEET_MARGIN_PT for m in margins) or max(margins) < 1.5:
            continue
        if best is None or r.width * r.height < best.width * best.height:
            best = fitz.Rect(r)
    return best


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


def _filtered_svg(page, rect, only_inside=True, drop=None, sheet_colour='#ffffff',
                  mirror_symbols=False, rotate_symbols=0):
    """The page's SVG reduced to the sign inside ``rect``: filled paths above the arrowhead
    threshold, undashed strokes heavier than a dimension line (their width taken through the
    path's own transform, which is where a scaled-up outline like the W10-1's X keeps its
    thickness), and the glyphs set in a legend font. Everything is exactly as the PDF draws it."""
    svg = page.get_svg_image()
    head_end = svg.index('</defs>') + len('</defs>')
    kept = []
    fill_boxes = []   # of the fills kept so far, to recognise their own outlines
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
            # Dimension marks over a black symbol are drawn as white 1 pt bars and 5 pt
            # arrowheads (the break lines on a Keep Right hood, the ticks across a curve
            # arrow); a white shape that encloses next to nothing for its extent is one of
            # those -- a letter's counter (the hole in an A) is small too, but fills its box
            if attrs.get('fill', '').lower() in ('#ffffff', '#fff', 'white'):
                area = _path_area(attrs.get('d', ''), nums)
                box_area = bbox.width * bbox.height
                # (a 1 pt leader across a panel fills ~1% of its box; a fraction's slash ~20%)
                hairline = min(bbox.width, bbox.height) < 2.5 or area < 0.06 * box_area
                thin = area < 0.3 * box_area
                speck = max(bbox.width, bbox.height) < 8    # an arrowhead; a counter is bigger
                # a hairline of any length (a long diagonal leader is 200 pt^2), or a small
                # thin shape or speck; a counter fills its box and is left alone
                if hairline or (area < MAX_MARK_AREA_PT and (thin or speck)):
                    continue
        elif stroke:
            width = float(attrs.get('stroke-width', '1')) * scale
            closed = attrs.get('d', '').rstrip().upper().endswith('Z')
            encloses = (closed and bbox.x0 <= rect.x0 + 1 and bbox.y0 <= rect.y0 + 1
                        and bbox.x1 >= rect.x1 - 1 and bbox.y1 >= rect.y1 - 1)
            if 'stroke-dasharray' in attrs or (width < MIN_SIGN_STROKE_PT and not encloses):
                continue
            # Every panel is drawn as its fill and then the same outline as a stroke; the
            # stroke adds nothing, and painted white it would fringe the panel's edge
            if closed and any(abs(bbox.x0 - fb.x0) < 1.5 and abs(bbox.y0 - fb.y0) < 1.5 and
                              abs(bbox.x1 - fb.x1) < 1.5 and abs(bbox.y1 - fb.y1) < 1.5
                              for fb in fill_boxes):
                continue
            if encloses:
                # The sign's own edge: the sheet, white, behind everything the page drew
                # inside it (the margin outside a regulatory sign's border is white sheet)
                el = el.replace('fill="none"', 'fill="%s"' % sheet_colour, 1).replace(' stroke=', ' data-stroke=', 1)
                kept.insert(0, el)
                continue
            # dimension marks drawn over a black symbol or border are white or light-grey
            # strokes; no sign outline is lighter than its background
            if _is_light(attrs.get('stroke', '')):
                continue
            if closed and bbox.width >= 40 and bbox.height >= 40:
                # A closed, sign-sized outline drawn as a stroke alone is a white panel on the
                # page's white -- a crossbuck's arms -- so it is given the white it relies on
                el = el.replace('fill="none"', 'fill="#ffffff"', 1)
        else:
            continue
        centre = fitz.Point((bbox.x0 + bbox.x1) / 2, (bbox.y0 + bbox.y1) / 2)
        if only_inside and not rect.contains(centre):
            continue
        if any(ins.contains(centre) for ins in insets):
            continue
        if fill:
            fill_boxes.append(bbox)
        # a symbol rather than the panel: an arrow may span most of the width but not the area
        small = bbox.width * bbox.height < 0.3 * rect.width * rect.height
        if rotate_symbols and small:
            # a symbol (the arrow), not the panel: turned about its own centre, where it is
            el = '<g transform="rotate(%s,%s,%s)">%s</g>' % (
                rotate_symbols, (bbox.x0 + bbox.x1) / 2, (bbox.y0 + bbox.y1) / 2, el)
        if mirror_symbols == 'both' and small:
            # A double-headed arrow from a single one: the head half of the arrow (its
            # left half, clipped) and that half's mirror image, meeting at the arrow's own
            # centre with one shaft width -- mirroring the whole arrow would lay its
            # squared tail over the other head. The clip is in the element's own space, so
            # the same rectangle serves both copies.
            cid = 'half_%d' % len(kept)
            cx = (bbox.x0 + bbox.x1) / 2
            kept.append('<clipPath id="%s"><rect x="%s" y="%s" width="%s" height="%s"/></clipPath>'
                        % (cid, bbox.x0 - 1, bbox.y0 - 1, cx - bbox.x0 + 1, bbox.height + 2))
            kept.append('<g clip-path="url(#%s)">%s</g>' % (cid, el))
            kept.append('<g transform="matrix(-1,0,0,1,%s,0)" clip-path="url(#%s)">%s</g>'
                        % (2 * cx, cid, el))
            continue
        kept.append(el)
    # Legend glyphs are the ones set in a Series (Highway Gothic) font; captions, reference
    # letters and dimensions are all Nimbus Sans. Match each SVG glyph to the raw text's
    # per-character origins to tell them apart -- the SVG itself does not name the font.
    legend_origins = _legend_glyph_origins(page, rect, drop)
    n_glyphs = 0
    for m in _USE_RE.finditer(body):
        nums = [float(v) for v in m.group(2).split(',')]
        e, f = nums[4], nums[5]
        if not any(abs(e - ox) < 0.75 and abs(f - oy) < 0.75 for ox, oy in legend_origins):
            continue
        # the glyph outlines carry no fill rule; a counter (the hole in an A) needs even-odd
        kept.append(m.group(0).replace('<use ', '<use fill-rule="evenodd" ', 1))
        n_glyphs += 1
    if mirror_symbols is True:
        # The paths (panel, border, arrow) flipped about the sign's centre line; the glyphs,
        # which came last into ``kept``, left as they are so the legend still reads
        n_paths = len(kept) - n_glyphs
        kept = (['<g transform="matrix(-1,0,0,1,%s,0)">' % (rect.x0 + rect.x1)]
                + kept[:n_paths] + ['</g>'] + kept[n_paths:])
    return svg[:head_end] + '\n' + '\n'.join(kept) + '\n</svg>'


def _render_svg(page, rect, only_inside=True, dpi=RENDER_DPI, drop=None, sheet_colour='#ffffff',
                mirror_symbols=False, rotate_symbols=0):
    doc = fitz.open('svg', _filtered_svg(page, rect, only_inside, drop, sheet_colour,
                                         mirror_symbols, rotate_symbols).encode('utf-8'))
    return _render_clip(doc[0], rect, dpi)


def _outer_rects(fills):
    """The outermost sign rects on a page: fills not contained in a larger fill, with any
    that overlap merged into one -- a crossbuck is two arms, neither inside the other."""
    outer = []
    for d in sorted(fills, key=lambda d: -(d['rect'].width * d['rect'].height)):
        r = d['rect']
        if r.width < MIN_SIGN_PT or r.height < MIN_SIGN_PT:   # an arrowhead or leader, not a sign
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


def book_sign(chapter, page, pick=0, only_inside=True, inner=None, replace=None,
              mirror_symbols=False, rotate_symbols=0):
    """One sign from a book page, rendered with alpha and cropped to its outline.

    ``pick`` chooses among the page's outermost sign rects, sorted top to bottom then left to
    right, for pages that carry a left- and right-hand version or a sign and its plaque.
    ``only_inside`` keeps only the fills that lie within the chosen rect (the other sign's
    fills otherwise bleed in when two signs share a page). ``inner`` instead crops to the
    n-th largest fill inside the chosen outer rect -- the panel of an in-street sign drawn
    with its post, say. ``replace=(old, new)`` drops the legend run reading ``old`` (the one
    size of numeral the book draws: "50" on the Speed Limit page) and sets ``new`` in its
    place at the same cap height, in the mod's Highway Gothic. ``mirror_symbols`` flips the
    page's paths -- the arrow, the panel -- about the sign's centre line but not its glyphs:
    the right-hand version of a sign the book draws left-handed with a legend;
    ``rotate_symbols`` (degrees, clockwise) turns each symbol smaller than half the sign --
    the arrow, not the panel -- about its own centre, so an up arrow points left or right
    where it is; ``mirror_symbols='both'`` keeps each symbol and adds its mirror image, a
    single arrow becoming the double-headed one. ``replace`` may also be a list of (old,
    new) pairs.
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
    sheet = _sheet_rect(p, rect)
    sheet_colour = _sheet_colour(fills, rect)
    if sheet is not None:
        rect = sheet
    if replace is None:
        return _render_svg(p, rect, only_inside, sheet_colour=sheet_colour,
                           mirror_symbols=mirror_symbols, rotate_symbols=rotate_symbols)
    pairs = [replace] if isinstance(replace[0], str) else list(replace)
    todo = []
    for old, new in pairs:
        boxes = _legend_span_boxes(p, rect, old)
        if not boxes:
            raise SystemExit('%s p%d: no legend run reads %r (have %s)' % (
                chapter, page, old, [t for t, _o, _b in _legend_spans(p, rect)]))
        todo.append((new, boxes))
    img = _render_svg(p, rect, only_inside, drop=[old for old, _new in pairs],
                      sheet_colour=sheet_colour, mirror_symbols=mirror_symbols,
                      rotate_symbols=rotate_symbols)
    scale = RENDER_DPI / 72.0
    for new, boxes in todo:
        for box in boxes:
            px = ((box.x0 - rect.x0) * scale, (box.y0 - rect.y0) * scale,
                  (box.x1 - rect.x0) * scale, (box.y1 - rect.y0) * scale)
            _set_legend(img, new, px)
    return img


def _set_legend(img, text, box, colour=(31, 26, 23, 255)):
    """Draw ``text`` centred on ``box`` (pixels) with the digits' cap height matching the
    box, in the mod's sign font; the colour is the book's black so recolour() maps it."""
    from PIL import ImageDraw, ImageFont
    import render_sign as rs
    x0, y0, x1, y1 = box
    target = y1 - y0
    size = max(8, int(target))
    f = ImageFont.truetype(rs.FONT_PATH, size)
    bb = f.getbbox('0')
    size = max(8, int(round(size * target / (bb[3] - bb[1]))))
    f = ImageFont.truetype(rs.FONT_PATH, size)
    bb = f.getbbox(text)
    # a longer legend ("100" for "50") keeps the old run's side margins rather than the panel
    max_w = img.width - 2 * min(x0, img.width - x1)
    if bb[2] - bb[0] > max_w:
        f = ImageFont.truetype(rs.FONT_PATH, max(8, int(size * max_w / (bb[2] - bb[0]))))
        bb = f.getbbox(text)
    d = ImageDraw.Draw(img)
    d.text(((x0 + x1) / 2 - (bb[2] + bb[0]) / 2, (y0 + y1) / 2 - (bb[3] + bb[1]) / 2),
           text, font=f, fill=colour)


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


# The mod's sign palette, as render_sign.py and the sign generators draw it. YELLOW is the
# real MUTCD yellow (Pantone 116); the drawings print an artist's approximation of each.
MOD_COLOURS = {
    'yellow': (252, 209, 22, 255),
    'orange': (255, 98, 0, 255),
    'red': (196, 30, 38, 255),
    'black': (20, 20, 20, 255),
    'white': (245, 245, 245, 255),
    'fyg': (186, 255, 41, 255),   # fluorescent yellow-green, the pedestrian / school family
    'blue': (3, 94, 159, 255),    # the guide / services blue the mod's D9 signs use
    'green': (3, 112, 95, 255),   # the guide green of the mod's D1 / D8 / D13 signs
    'brown': (135, 94, 20, 255),  # the recreational brown of the mod's RS signs
}

# The drawings' printed colours onto that palette: the book's and the interim files' yellow,
# the book's orange and red, the interim red, the blacks (the book's, pure, the interim's),
# white, and the interim files' fluorescent yellow-green.
SHS_PALETTE = {
    (255, 245, 0): MOD_COLOURS['yellow'], (255, 208, 70): MOD_COLOURS['yellow'],
    (232, 120, 26): MOD_COLOURS['orange'],
    (217, 38, 28): MOD_COLOURS['red'], (191, 48, 26): MOD_COLOURS['red'],
    (31, 26, 23): MOD_COLOURS['black'], (0, 0, 0): MOD_COLOURS['black'],
    (35, 31, 32): MOD_COLOURS['black'],
    (255, 255, 255): MOD_COLOURS['white'],
    (190, 215, 61): MOD_COLOURS['fyg'],
    (0, 125, 194): MOD_COLOURS['blue'],
    (0, 145, 64): MOD_COLOURS['green'],
}


def official_face(face, aspect, mirror=False, palette=None, size=DEFAULT_TEX, stretch_tol=None):
    """A rendered drawing onto a sign texture: palette-mapped, optionally mirrored (the
    left-hand version of a symbol the book draws right-handed only), fitted to the plate.
    ``stretch_tol=0`` keeps the face's true proportions whatever the plate -- a silhouette
    sign (pennant, pentagon, crossbuck) IS its outline, so it must not be squashed to fill."""
    mapping = dict(SHS_PALETTE)
    mapping.update(palette or {})
    face = recolour(face, mapping)
    if mirror:
        face = face.transpose(Image.FLIP_LEFT_RIGHT)
    return fit_plate(face, aspect, size, stretch_tol=stretch_tol)


def back_texture(face, gray=(150, 150, 150, 255)):
    """The back of a silhouette sign: the face's outline in unpainted gray, mirrored as the
    back face's UVs are."""
    back = Image.new('RGBA', face.size, gray)
    back.putalpha(face.getchannel('A'))
    return back.transpose(Image.FLIP_LEFT_RIGHT)


def symbol_face(chapter, page, picks):
    """A pictogram the book draws bare -- a black symbol with no panel, sometimes in several
    pieces (a fish and its hook) -- as one image on transparency: the union of the listed
    outlines on the page (``picks`` as :func:`book_sign` numbers them)."""
    p = book_page(chapter, page)
    outers = _outer_rects(_sign_fills(p))
    outers.sort(key=lambda r: (round(r.y0 / 40), r.x0))
    rect = None
    for i in picks:
        rect = fitz.Rect(outers[i]) if rect is None else rect | outers[i]
    return _render_svg(p, rect, only_inside=True)


def symbol_on_panel(symbol, panel_colour, aspect=1.0, size=DEFAULT_TEX, symbol_frac=0.7,
                    border=0.045, radius=0.07):
    """The pictogram in white on a rounded panel with a white border, at the plate's
    proportions (``aspect`` wide by 1 tall, then squished to the square texture as every
    face is) -- the RS series sign the mod draws the recreational symbols on."""
    ss = 4
    W, H = (int(size * ss * aspect), size * ss) if aspect >= 1 else (size * ss, int(size * ss / aspect))
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = int(min(W, H) * radius)
    d.rounded_rectangle((0, 0, W - 1, H - 1), radius=r, fill=(255, 255, 255, 255))
    b = int(min(W, H) * border)
    d.rounded_rectangle((b, b, W - 1 - b, H - 1 - b), radius=max(1, r - b), fill=tuple(panel_colour[:3]) + (255,))
    # only the ink becomes white: a wheel's white centre or a window goes clear, so the
    # panel shows through it as it does on the real sign
    import numpy as np
    arr = np.asarray(symbol.convert('RGBA')).astype(np.float32)
    lum = (0.299 * arr[..., 0] + 0.587 * arr[..., 1] + 0.114 * arr[..., 2]) / 255.0
    alpha = (arr[..., 3] * (1.0 - lum)).clip(0, 255).astype(np.uint8)
    white = Image.new('RGBA', symbol.size, (255, 255, 255, 255))
    white.putalpha(Image.fromarray(alpha, 'L'))
    bw, bh = W * symbol_frac, H * symbol_frac
    scale = min(bw / white.width, bh / white.height)
    white = white.resize((max(1, int(white.width * scale)), max(1, int(white.height * scale))), Image.LANCZOS)
    img.alpha_composite(white, ((W - white.width) // 2, (H - white.height) // 2))
    return img.resize((size, size), Image.LANCZOS)


def fit_plate(face, aspect, size=DEFAULT_TEX, margin=0.0, stretch_tol=None):
    """Squish a sign face to the square texture its plate stretches back to ``aspect``.

    The plate models come in a handful of proportions and the real signs in many more, so a
    face whose proportions are within ``stretch_tol`` of the plate's is stretched to fill it
    edge to edge (a 24 x 30 sign on the 16 x 21 plate is 5% off, the 1 MILE runaway ramp
    rectangle on the 22 x 16 plate 18% -- neither shows in the world, where a gap of bare
    plate around the face does). A face further off than that -- the 1:3 in-street paddle
    -- keeps its true proportions, centred, with ``margin`` of transparent plate around it.
    """
    w, h = face.size
    sign_aspect = w / float(h)
    ratio = sign_aspect / aspect
    if stretch_tol is None or abs(ratio - 1.0) <= stretch_tol:
        # the default: every face fills its plate edge to edge, as the hand-drawn faces
        # did; a sign whose drawing is far from its plate gets a better plate instead
        pw = ph = 1.0 - 2 * margin
    elif ratio > 1.0:                        # width-limited
        pw = 1.0 - 2 * margin
        ph = pw / ratio
    else:                                    # height-limited
        ph = 1.0 - 2 * margin
        pw = ph * ratio
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
