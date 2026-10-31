"""
venues_lines.py -- the geometry engine behind the Sports & Venues courts and track: markings
drawn where they really fall, as baked quads computed per cell, with no picture per cell.

Not run on its own. gen_venues_courts.py and gen_venues_track.py describe their layouts with it
and it writes the Java tables the game paints from (``LineArtMaterials.java``, ``CourtLayouts.java``,
``TrackLayouts.java``); ``LinePainter.java`` is this module's painter, line for line, so the
cells a generator lays and the cells the game draws cannot disagree.

A **layout** is a base material (what a cell shows where nothing is painted) and an ordered list
of **paint operations**, in metres in the layout's own frame (x east, z south, cell (i, j) the
square [i, i + 1] x [j, j + 1]). An operation paints one material over a region: the
intersection of its *positive* sets less the union of its *negative* sets, where a set is

  * a convex polygon (a list of half-planes ``a x + b z <= c``), or
  * a disc ``(cx, cz, r)``.

A disc is a circle drawn as the polygon through the points where it crosses the block grid: in
any one cell it is cut by the chords between its crossings of that cell's edges, so two cells
sharing an edge compute the same crossing on it and a curve is continuous from cell to cell
(the chord is never more than 0.007 blocks inside the true arc for a radius over 36 m). So every
region is, per cell, an intersection of half-planes, and a cell's top is a partition of its
square into convex polygons, each one material: no overlapping faces, nothing to z-fight.

Materials carry a sprite, a colour (fixed, or one of the anchor's four colours) and a raise
(the inner kerb stands 0.8 px proud).
"""
import math
import textwrap

EPS = 1e-9
MIN_AREA = 1e-7

# ------------------------------------------------------------------------------------------
# Materials: shared by both families; LineArtMaterials.java is written from this list, in
# this order (ids are indices). Colour sources: "cc" the main surface colour, "sc" the second
# surface colour (SurfaceColours), "lc" the line colour, "oc" the second line colour
# (VenueColours), or a fixed 0xRRGGBB. Sprite "SURF" is the family's surface sprite.
# ------------------------------------------------------------------------------------------
MATERIALS = [
    # name,       sprite key,   colour,     raise (px)
    ("SURFACE",   "SURF",       "cc",       0.0),
    ("SURROUND",  "SURF",       "sc",       0.0),
    ("LINE",      "paint",      "lc",       0.0),
    ("OVERLAY",   "paint",      "oc",       0.0),
    ("GRASS",     "turf",       0xFFFFFF,   0.0),
    ("SAND",      "sand",       0xFFFFFF,   0.0),
    ("TAPE",      "tape",       "lc",       0.0),
    ("KERB",      "kerb",       0xFFFFFF,   0.8),
    ("CONCRETE",  "concrete",   0xFFFFFF,   0.0),
    ("STEEL",     "steel",      0xFFFFFF,   0.0),
    ("WHITE",     "paint",      0xF4F4F0,   0.0),
    ("YELLOW",    "paint",      0xF2C230,   0.0),
    ("BLUE",      "paint",      0x2E6CC8,   0.0),
    ("GREEN",     "paint",      0x2E9A4C,   0.0),
    ("RED",       "paint",      0xD0302A,   0.0),
    ("BLACK",     "paint",      0x26262A,   0.0),
]
MAT = {m[0]: i for i, m in enumerate(MATERIALS)}


# ------------------------------------------------------------------------------------------
# Sets
# ------------------------------------------------------------------------------------------
class HP(object):
    """The half-plane a x + b z <= c."""
    __slots__ = ("a", "b", "c")

    def __init__(self, a, b, c):
        self.a, self.b, self.c = float(a), float(b), float(c)

    def val(self, x, z):
        return self.a * x + self.b * z - self.c


def hp_through(p, q, inside):
    """The half-plane bounded by the line through p and q that holds the point ``inside``."""
    a = q[1] - p[1]
    b = p[0] - q[0]
    c = a * p[0] + b * p[1]
    if a * inside[0] + b * inside[1] > c:
        a, b, c = -a, -b, -c
    return HP(a, b, c)


class Convex(object):
    """A convex region: the intersection of half-planes."""
    kind = 0

    def __init__(self, hps):
        self.hps = list(hps)

    def cell(self, i, j):
        """'ALL', 'NONE' or the half-planes that cut the cell [i, i+1] x [j, j+1]."""
        out = []
        corners = ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1))
        for h in self.hps:
            vals = [h.val(x, z) for x, z in corners]
            if min(vals) >= -EPS:
                return "NONE"   # every corner outside or on the line: nothing of the cell in
            if max(vals) <= EPS:
                continue        # the whole cell inside this one
            out.append(h)
        return out if out else "ALL"

    def java(self):
        flat = [0, len(self.hps)]
        for h in self.hps:
            flat += [h.a, h.b, h.c]
        return flat


class Disc(object):
    """A disc (cx, cz, r), drawn as the polygon through its crossings of the block grid."""
    kind = 1

    def __init__(self, cx, cz, r):
        self.cx, self.cz, self.r = float(cx), float(cz), float(r)
        # the grid the polygon's corners are taken on: N lines a block, fine enough that no
        # chord strays more than 4 mm inside the arc (1 for a track's bends, 5 for a 1.25 m arc)
        self.n = max(1, int(math.ceil(1.0 / math.sqrt(8.0 * self.r * 0.004))))

    def cell(self, i, j):
        cx, cz, r = self.cx, self.cz, self.r
        # quick outs on the cell's distance from the centre
        nx = min(max(cx, i), i + 1)
        nz = min(max(cz, j), j + 1)
        if (nx - cx) ** 2 + (nz - cz) ** 2 >= r * r:
            return "NONE"
        fx = max(abs(i - cx), abs(i + 1 - cx))
        fz = max(abs(j - cz), abs(j + 1 - cz))
        if fx * fx + fz * fz <= r * r:
            return "ALL"
        pts = []
        n = self.n
        for k in range(n + 1):
            x = i + k / float(n)
            d = r * r - (x - cx) ** 2
            if d >= 0:
                s = math.sqrt(d)
                for z in (cz - s, cz + s):
                    if j - EPS <= z <= j + 1 + EPS:
                        pts.append((x, min(max(z, float(j)), float(j + 1))))
        for k in range(n + 1):
            z = j + k / float(n)
            d = r * r - (z - cz) ** 2
            if d >= 0:
                s = math.sqrt(d)
                for x in (cx - s, cx + s):
                    if i - EPS <= x <= i + 1 + EPS:
                        pts.append((min(max(x, float(i)), float(i + 1)), z))
        uniq = []
        for p in pts:
            if all(abs(p[0] - q[0]) > 1e-9 or abs(p[1] - q[1]) > 1e-9 for q in uniq):
                uniq.append(p)
        if len(uniq) < 2:
            return "ALL" if (i + 0.5 - cx) ** 2 + (j + 0.5 - cz) ** 2 < r * r else "NONE"
        uniq.sort(key=lambda p: math.atan2(p[1] - cz, p[0] - cx))
        out = []
        n = len(uniq)
        for k in range(n):
            p, q = uniq[k], uniq[(k + 1) % n]
            ta = math.atan2(p[1] - cz, p[0] - cx)
            tb = math.atan2(q[1] - cz, q[0] - cx)
            if tb <= ta:
                tb += 2 * math.pi
            tm = (ta + tb) / 2
            mx, mz = cx + r * math.cos(tm), cz + r * math.sin(tm)
            if i < mx < i + 1 and j < mz < j + 1:
                out.append(hp_through(p, q, (cx, cz)))
        if not out:
            return "ALL" if (i + 0.5 - cx) ** 2 + (j + 0.5 - cz) ** 2 < r * r else "NONE"
        return out

    def java(self):
        return [1, self.cx, self.cz, self.r, self.n]


# ------------------------------------------------------------------------------------------
# Polygons
# ------------------------------------------------------------------------------------------
def area(poly):
    s = 0.0
    n = len(poly)
    for k in range(n):
        x0, z0 = poly[k]
        x1, z1 = poly[(k + 1) % n]
        s += x0 * z1 - x1 * z0
    return s / 2.0


def split(poly, h):
    """(inside, outside) of a convex polygon cut by a half-plane; either may be None."""
    vals = [h.val(x, z) for x, z in poly]
    if max(vals) <= EPS:
        return poly, None
    if min(vals) >= -EPS:
        return None, poly
    ins, outs = [], []
    n = len(poly)
    for k in range(n):
        p, q = poly[k], poly[(k + 1) % n]
        vp, vq = vals[k], vals[(k + 1) % n]
        if vp <= EPS:
            ins.append(p)
        if vp >= -EPS:
            outs.append(p)
        if (vp < -EPS and vq > EPS) or (vp > EPS and vq < -EPS):
            t = vp / (vp - vq)
            m = (p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)
            ins.append(m)
            outs.append(m)
    ins = ins if len(ins) >= 3 and abs(area(ins)) > MIN_AREA else None
    outs = outs if len(outs) >= 3 and abs(area(outs)) > MIN_AREA else None
    return ins, outs


class Op(object):
    def __init__(self, mat, pos, neg=()):
        self.mat = MAT[mat] if isinstance(mat, str) else mat
        self.pos = list(pos)
        self.neg = list(neg)
        self.bbox = bbox_of(self.pos)

    def java(self):
        flat = [self.mat] + [round(v, 6) for v in self.bbox] + [len(self.pos), len(self.neg)]
        for s in self.pos + self.neg:
            flat += s.java()
        return flat


def bbox_of(pos):
    x0, z0, x1, z1 = -1e5, -1e5, 1e5, 1e5
    for s in pos:
        if isinstance(s, Disc):
            x0, z0 = max(x0, s.cx - s.r), max(z0, s.cz - s.r)
            x1, z1 = min(x1, s.cx + s.r), min(z1, s.cz + s.r)
    poly = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
    for s in pos:
        if isinstance(s, Convex):
            for h in s.hps:
                poly, _o = split(poly, h)
                if poly is None:
                    return (0.0, 0.0, 0.0, 0.0)
    xs = [p[0] for p in poly]
    zs = [p[1] for p in poly]
    return (min(xs) - 1e-6, min(zs) - 1e-6, max(xs) + 1e-6, max(zs) + 1e-6)


def paint(layout, i, j):
    """The cell's top: [(polygon, material id)], polygons in layout metres, partitioning the
    square [i, i+1] x [j, j+1]."""
    pieces = [([(float(i), float(j)), (i + 1.0, float(j)), (i + 1.0, j + 1.0),
                (float(i), j + 1.0)], layout.base)]
    for op in layout.ops:
        b = op.bbox
        if b[2] <= i or b[0] >= i + 1 or b[3] <= j or b[1] >= j + 1:
            continue
        cuts_pos = []
        dead = False
        for s in op.pos:
            c = s.cell(i, j)
            if c == "NONE":
                dead = True
                break
            if c != "ALL":
                cuts_pos.append(c)
        if dead:
            continue
        cuts_neg = []
        for s in op.neg:
            c = s.cell(i, j)
            if c == "ALL":
                dead = True
                break
            if c != "NONE":
                cuts_neg.append(c)
        if dead:
            continue
        nxt = []
        for poly, mat in pieces:
            inside = [poly]
            kept = []
            for hs in cuts_pos:
                for h in hs:
                    keep_in = []
                    for p in inside:
                        a, o = split(p, h)
                        if o is not None:
                            kept.append(o)
                        if a is not None:
                            keep_in.append(a)
                    inside = keep_in
            for hs in cuts_neg:
                out = []
                for p in inside:
                    rem = p
                    for h in hs:
                        a, o = split(rem, h)
                        if o is not None:
                            out.append(o)
                        rem = a
                        if rem is None:
                            break
                    if rem is not None:
                        kept.append(rem)
                inside = out
            nxt += [(p, mat) for p in kept] + [(p, op.mat) for p in inside]
        pieces = nxt
    return pieces


def cell_class(layout, i, j):
    """('plain', material) when one material covers the whole cell, else ('picture', pieces)."""
    pieces = paint(layout, i, j)
    mats = set(m for _p, m in pieces)
    if len(mats) == 1:
        return ("plain", mats.pop())
    return ("picture", pieces)


# ------------------------------------------------------------------------------------------
# Shapes (Python only: they make operations)
# ------------------------------------------------------------------------------------------
def rect(x0, z0, x1, z1):
    """An axis-aligned rectangle."""
    x0, x1 = min(x0, x1), max(x0, x1)
    z0, z1 = min(z0, z1), max(z0, z1)
    return Convex([HP(-1, 0, -x0), HP(1, 0, x1), HP(0, -1, -z0), HP(0, 1, z1)])


def polygon(pts):
    """A convex polygon from its corners (either winding)."""
    n = len(pts)
    cx = sum(p[0] for p in pts) / n
    cz = sum(p[1] for p in pts) / n
    return Convex([hp_through(pts[k], pts[(k + 1) % n], (cx, cz)) for k in range(n)])


def strip(p, q, w):
    """A straight line from p to q, w wide (square ends)."""
    dx, dz = q[0] - p[0], q[1] - p[1]
    m = math.hypot(dx, dz)
    nx, nz = -dz / m * w / 2, dx / m * w / 2
    return polygon([(p[0] + nx, p[1] + nz), (q[0] + nx, q[1] + nz), (q[0] - nx, q[1] - nz),
                    (p[0] - nx, p[1] - nz)])


def half(px, pz, nx, nz):
    """The half-plane of points whose offset from (px, pz) has a non-negative dot with (nx, nz)."""
    return Convex([HP(-nx, -nz, -(nx * px + nz * pz))])


def ring(cx, cz, r0, r1, mat, *more_pos):
    """An operation painting the annulus r0..r1 (optionally cut by more convex sets)."""
    return Op(mat, [Disc(cx, cz, r1)] + list(more_pos), [Disc(cx, cz, r0)])


# ------------------------------------------------------------------------------------------
# Painted numerals: strokes on a 4 x 7 grid, so a lane number is geometry like every line
# ------------------------------------------------------------------------------------------
def _arc(cx, cy, rx, ry, a0, a1, n=6):
    return [(cx + rx * math.cos(math.radians(a0 + (a1 - a0) * k / n)),
             cy + ry * math.sin(math.radians(a0 + (a1 - a0) * k / n))) for k in range(n + 1)]


# polylines in glyph units, x 0..4 right, y 0..7 down
DIGITS = {
    "0": [_arc(2, 3.5, 2, 3.5, 0, 360, 16)],
    "1": [[(1, 1.4), (2.3, 0), (2.3, 7)], [(1.0, 7), (3.6, 7)]],
    "2": [_arc(2, 2, 2, 2, 180, 360, 6)[:-1] + _arc(2, 2, 2, 2, 0, 50, 2)[1:] + [(0, 7), (4, 7)]],
    "3": [_arc(2, 1.75, 1.9, 1.75, 200, 450, 7), _arc(2, 5.25, 2, 1.75, 270, 520, 7)],
    "4": [[(3, 7), (3, 0), (0, 5), (4, 5)]],
    "5": [[(3.8, 0), (0.4, 0), (0.2, 3.2)] + _arc(2, 4.9, 2, 2.1, 235, 495, 8)[1:]],
    "6": [_arc(2, 5, 2, 2, 0, 360, 12),
          [(0, 5)] + _arc(3.6, 5, 3.6, 5, 180, 240, 4)[1:]],
    "7": [[(0, 0), (4, 0), (1.6, 7)]],
    "8": [_arc(2, 1.75, 1.75, 1.75, 0, 360, 12), _arc(2, 5.25, 2, 1.75, 0, 360, 12)],
    "9": [_arc(2, 2, 2, 2, 0, 360, 12),
          [(4, 2)] + _arc(0.4, 2, 3.6, 5, 0, 60, 4)[1:]],
}


def numeral(text, ox, oz, height, ux, uz, mat, stroke=None):
    """Operations painting ``text`` in strokes, its top-left at (ox, oz), ``height`` tall, read
    with its 'right' along (ux, uz) (a unit vector; 'down' is that turned a quarter clockwise)."""
    k = height / 7.0
    w = stroke if stroke is not None else 0.75 * k
    dx, dz = -uz, ux          # down, in a frame where x east and z south
    ops = []
    pen = 0.0
    for ch in text:
        for line in DIGITS[ch]:
            pts = [(ox + (pen + gx) * k * ux + gy * k * dx, oz + (pen + gx) * k * uz + gy * k * dz)
                   for gx, gy in line]
            for a, b in zip(pts, pts[1:]):
                if math.hypot(b[0] - a[0], b[1] - a[1]) < 1e-6:
                    continue
                ext = w * 0.35       # overlap at the joins, so a bend has no notch
                m = math.hypot(b[0] - a[0], b[1] - a[1])
                ea = (a[0] - (b[0] - a[0]) / m * ext, a[1] - (b[1] - a[1]) / m * ext)
                eb = (b[0] + (b[0] - a[0]) / m * ext, b[1] + (b[1] - a[1]) / m * ext)
                ops.append(Op(mat, [strip(ea, eb, w)]))
        pen += 5.6
    return ops


# ------------------------------------------------------------------------------------------
# Layouts, anchors and Java
# ------------------------------------------------------------------------------------------
REACH = 12          # how far a painted cell looks for its anchor (Chebyshev): LineArtFamily.REACH

# ------------------------------------------------------------------------------------------
# Legibility: the drawn width of a painted line. A line is geometry, not a mipmapped texture,
# so a 5 cm line is under a screen pixel past about 50 m and breaks up into dashes rather than
# fading. Every paint material's band thinner than MIN_LINE is drawn MIN_LINE wide, centred on
# where the true line falls (a line measured to its outer edge keeps its centre, not its edge).
# Applied here, as each layout is built, so the Java tables the game paints from carry the drawn
# widths and LinePainter.java needs no rule of its own.
# ------------------------------------------------------------------------------------------
MIN_LINE = 0.125
PAINTS = {MAT[m] for m in ("LINE", "OVERLAY", "TAPE", "WHITE", "YELLOW", "BLUE", "GREEN",
                           "RED", "BLACK")}


def _grow_convex(cv):
    """The convex set grown by g on every side if it is a band thinner than MIN_LINE (two of
    its half-planes facing apart closer than that); otherwise itself."""
    thin = None
    for p in range(len(cv.hps)):
        for q in range(p + 1, len(cv.hps)):
            a, b = cv.hps[p], cv.hps[q]
            na, nb = math.hypot(a.a, a.b), math.hypot(b.a, b.b)
            if na < EPS or nb < EPS:
                continue
            if (a.a * b.a + a.b * b.b) / (na * nb) > -1 + 1e-9:
                continue
            w = a.c / na + b.c / nb
            if w < MIN_LINE - 1e-9 and (thin is None or w < thin):
                thin = w
    if thin is None:
        return cv
    g = (MIN_LINE - thin) / 2.0
    return Convex([HP(h.a, h.b, h.c + g * math.hypot(h.a, h.b)) for h in cv.hps])


def legible(op):
    """``op`` with every band of paint thinner than MIN_LINE widened to it about its centre:
    a thin convex band grown on every side, an annulus (a disc less a concentric disc) grown
    both ways. Anything else, and anything not paint, is returned as it is."""
    if op.mat not in PAINTS:
        return op
    pos = [_grow_convex(s) if isinstance(s, Convex) else s for s in op.pos]
    neg = list(op.neg)
    for pi, s in enumerate(pos):
        if not isinstance(s, Disc):
            continue
        for ni, t in enumerate(neg):
            if (isinstance(t, Disc) and abs(t.cx - s.cx) < 1e-9 and abs(t.cz - s.cz) < 1e-9
                    and s.r - t.r < MIN_LINE - 1e-9):
                g = (MIN_LINE - (s.r - t.r)) / 2.0
                pos[pi] = Disc(s.cx, s.cz, s.r + g)
                neg[ni] = Disc(t.cx, t.cz, t.r - g)
                break
    return Op(op.mat, pos, neg)


class Layout(object):
    """One court or track: its id, base material, operations, the cells it may paint (inclusive
    i0, j0, i1, j1) and its default colours (cc, sc, lc, oc)."""

    def __init__(self, name, base, ops, extent, colours):
        self.name = name
        self.base = MAT[base]
        self.ops = [legible(op) for op in ops]
        self.extent = extent
        self.colours = colours
        self._census = None

    def census(self):
        """{(i, j): ('plain', mat) | ('picture', pieces)} over the extent."""
        if self._census is None:
            i0, j0, i1, j1 = self.extent
            self._census = {(i, j): cell_class(self, i, j)
                            for j in range(j0, j1 + 1) for i in range(i0, i1 + 1)}
        return self._census

    def pictures(self):
        return sorted(c for c, v in self.census().items() if v[0] == "picture")

    def anchors(self):
        """Anchor cells on a lattice 2 REACH + 1 apart, kept only where a painted cell is within
        reach: every painted cell then has one within REACH."""
        pics = set(self.pictures())
        step = 2 * REACH + 1
        i0, j0, i1, j1 = self.extent
        out = []
        for aj in range(j0 + REACH, j1 + REACH + 1, step):
            for ai in range(i0 + REACH, i1 + REACH + 1, step):
                ci, cj = min(ai, i1), min(aj, j1)
                if any((ci + di, cj + dj) in pics for dj in range(-REACH, REACH + 1)
                       for di in range(-REACH, REACH + 1)):
                    out.append((ci, cj))
        self.verify_reach(out, pics)
        return out

    def verify_reach(self, anchors, pics):
        aset = set(anchors)
        for (i, j) in pics:
            if not any(abs(i - a) <= REACH and abs(j - b) <= REACH for a, b in aset):
                raise SystemExit("%s: painted cell %s has no anchor in reach" % (self.name, (i, j)))

    def java(self, index):
        flat = []
        for op in self.ops:
            flat += op.java()
        nums = " ".join(_jnum(v) for v in flat)
        # the table goes in as text, cut into short literals, parsed as the class loads:
        # as an array initializer a track's 20,000 numbers would pass a method's 64 KB limit
        parts = []
        while nums:
            cut = nums.rfind(" ", 0, 86) if len(nums) > 86 else len(nums)
            parts.append(nums[:cut])
            nums = nums[cut:].lstrip()
        sep = ",\n" + " " * 10
        lits = sep.join('"%s"' % p for p in parts)
        return ('new LineArtLayout("%s", %d, new int[]{%d, %d, %d, %d}, new int[]{%d, %d, %d, %d},'
                '\n          LineArtLayout.table(\n          %s))' % (
                    self.name, self.base, self.extent[0], self.extent[1], self.extent[2],
                    self.extent[3], self.colours[0], self.colours[1], self.colours[2],
                    self.colours[3], lits))


def _jnum(v):
    if isinstance(v, int):
        return str(v)
    r = round(v, 6)
    if r == int(r):
        return "%d" % int(r)
    return ("%.6f" % r).rstrip("0")


def wrap_java(text, indent=10, width=99):
    """Wraps a long Java expression at its commas."""
    out, line = [], ""
    for tok in text.split(", "):
        piece = tok if not line else ", " + tok
        if len(line) + len(piece) > width:
            out.append(line + ",")
            line = " " * indent + tok
        else:
            line += piece
    out.append(line)
    return "\n".join(out)


def layouts_java(gen, cls, family_doc, layouts):
    body = []
    for k, lay in enumerate(layouts):
        body.append("  /** Layout %d: %s. */\n  public static final int %s = %d;" % (
            k, lay.name, lay.name.upper(), k))
    items = ",\n".join("      " + lay.java(k) for k, lay in enumerate(layouts))
    doc = "\n".join(" * " + line for line in textwrap.wrap(
        family_doc + (" Generated by {@code dev-env-utils/scripts/%s} from the geometry it lays "
                      "(its {@code --layout}), through {@code venues_lines.py}, so a laid "
                      "layout and the painted cells' picture of it cannot disagree. Do not edit."
                      % gen), 95, break_on_hyphens=False))
    return ("""package com.micatechnologies.minecraft.csm.venues.lines;

/**
%s
 *
 * @since 2026.10
 */
public final class %s {

%s

  /** Every layout, by index (an anchor's {@code lay}). */
  public static final LineArtLayout[] LAYOUTS = {
%s};

  private %s() {
  }
}
""" % (doc, cls, "\n".join(body), items, cls))


def materials_java(gen):
    rows = []
    for name, sprite, colour, rise in MATERIALS:
        src = ("LineArtMaterial.Colour.%s" % colour.upper() if isinstance(colour, str)
               else "LineArtMaterial.Colour.FIXED")
        rgb = 0 if isinstance(colour, str) else colour
        rows.append('      new LineArtMaterial("%s", "%s", %s, 0x%06X, %sf)' % (
            name, sprite, src, rgb, _jnum(float(rise))))
    consts = "\n".join("  public static final int %s = %d;" % (m[0], i)
                       for i, m in enumerate(MATERIALS))
    return """package com.micatechnologies.minecraft.csm.venues.lines;

/**
 * The materials a court or track cell is painted in: a sprite ({@code SURF} is the family's own
 * surface), a colour (fixed, or one of the anchor's four) and how far the face stands proud of
 * the block's top, in pixels. Generated by {@code dev-env-utils/scripts/%s} from
 * {@code venues_lines.py}'s list. Do not edit.
 *
 * @since 2026.10
 */
public final class LineArtMaterials {

%s

  /** Every material, by id. */
  public static final LineArtMaterial[] ALL = {
%s};

  private LineArtMaterials() {
  }
}
""" % (gen, consts, ",\n".join(rows))


# ------------------------------------------------------------------------------------------
# A preview: the layout's cells as an image, a few pixels a metre, for checking geometry offline
# ------------------------------------------------------------------------------------------
def preview(layout, path, ppm=16, colours=None, sprites=None, region=None):
    from PIL import Image, ImageDraw
    pal = colours or {}
    i0, j0, i1, j1 = region or layout.extent
    w, h = (i1 - i0 + 1) * ppm, (j1 - j0 + 1) * ppm
    img = Image.new("RGB", (w, h), (40, 40, 40))
    dr = ImageDraw.Draw(img)
    for j in range(j0, j1 + 1):
        for i in range(i0, i1 + 1):
            for poly, m in paint(layout, i, j):
                col = pal.get(MATERIALS[m][0], (255, 0, 255))
                dr.polygon([((x - i0) * ppm, (z - j0) * ppm) for x, z in poly], fill=col)
    img.save(path)
    return path
