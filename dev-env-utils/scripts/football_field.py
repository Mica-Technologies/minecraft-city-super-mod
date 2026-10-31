"""
football_field.py -- the American football field and the soccer pitch as geometry, for
gen_venues_football.py.

Not run on its own (``--analyse`` prints the cell census for a developer). Like
ballpark_field.py, which it borrows from (the raster resolution, the picture key), it holds:

  * the field: every line, number, arrow and region of a full-size field, in blocks, one block a
    metre, as shapes painted onto a label canvas (a few samples a texel);
  * the cells: each cell's 32 x 32 coverage of each label (turf light, turf dark, white paint,
    end zone colour), and a census telling which cells are one plain label and which are a
    picture;
  * the anchors: a regular grid of marking anchors, each owning the picture cells nearest it,
    and the picture tables a painted cell is looked up in.

**Sub-block lines.** One block is a metre and five yards are 4.572 m, so a football field's
lines do not fall on block edges. Every line, number and mark is drawn where it really falls,
snapped only to the texel (1/32 of a block, about 3 cm): a cell a yard line crosses shows the
line at its true offset in that cell. Because a picture is the whole cell's 32 x 32 coverage,
a cell can hold two lines (a yard line and a hash a yard away), a line and the edge of a
number, or a stripe edge under a line, with no special case: the census simply finds each
distinct picture. Pictures that are turns of one another are stored once (a painted cell's
quarter turn comes from its table entry), so the far side's hash marks and the soccer pitch's
four corners cost nothing extra.

The canonical frames (x east, z south, blocks):

  * football: the field's centre at (0.5, 0.5), the middle of cell (0, 0); the 50 yard line runs
    north-south through the middle of column 0; the field's length runs east-west; the home (press
    box) sideline is the south one. Numbers read upright from the far sideline, their tops
    towards the nearer sideline, as on a real field.
  * soccer: the centre spot at (0.5, 0.0); the halfway line through the middle of column 0, the
    pitch's long axis east-west; the touchlines on block edges (z = -34 and 34), the goal lines
    on block edges (x = -52 and 53): a 105 x 68 pitch.
"""
import math
import sys

import numpy as np

import ballpark_field as bf

RES = bf.RES        # texels a block
SS = 3              # samples a texel, each way
YD = 0.9144
FT = 0.3048
IN = 0.0254

TURF_L, TURF_D, WHITE, ZONE = range(4)
LABELS = ("turf", "turf_dark", "white", "zone")
NL = len(LABELS)


def snap(v):
    """A coordinate snapped to the texel grid."""
    return round(v * RES) / float(RES)


class Canvas(object):
    """A label canvas over a box of cells, SS samples a texel each way. Shapes are painted in
    order, later over earlier."""

    def __init__(self, x0, x1, z0, z1, fill=TURF_L):
        self.x0, self.x1, self.z0, self.z1 = x0, x1, z0, z1
        self.n = RES * SS
        self.lab = np.full(((z1 - z0 + 1) * self.n, (x1 - x0 + 1) * self.n), fill, dtype=np.int8)

    def _ix(self, x):
        return int(round((x - self.x0) * self.n))

    def _iz(self, z):
        return int(round((z - self.z0) * self.n))

    def rect(self, ax, bx, az, bz, label):
        """An axis-aligned rectangle, its edges snapped to the texel grid."""
        ax, bx, az, bz = snap(min(ax, bx)), snap(max(ax, bx)), snap(min(az, bz)), snap(max(az, bz))
        i0, i1 = max(0, self._ix(ax)), min(self.lab.shape[1], self._ix(bx))
        j0, j1 = max(0, self._iz(az)), min(self.lab.shape[0], self._iz(bz))
        if i1 > i0 and j1 > j0:
            self.lab[j0:j1, i0:i1] = label

    def shape(self, bbox, pred, label):
        """Any shape: ``pred(X, Z)`` on the samples inside ``bbox`` (ax, bx, az, bz)."""
        ax, bx, az, bz = bbox
        i0, i1 = max(0, self._ix(ax) - 1), min(self.lab.shape[1], self._ix(bx) + 1)
        j0, j1 = max(0, self._iz(az) - 1), min(self.lab.shape[0], self._iz(bz) + 1)
        if i1 <= i0 or j1 <= j0:
            return
        xs = self.x0 + (np.arange(i0, i1) + 0.5) / self.n
        zs = self.z0 + (np.arange(j0, j1) + 0.5) / self.n
        X, Z = np.meshgrid(xs, zs)
        m = pred(X, Z)
        sub = self.lab[j0:j1, i0:i1]
        sub[m] = label

    def coverage(self, cx, cz):
        """A cell's coverage of each label: (NL, RES, RES), row 0 its north edge."""
        j = (cz - self.z0) * self.n
        i = (cx - self.x0) * self.n
        lab = self.lab[j:j + self.n, i:i + self.n]
        out = np.zeros((NL, RES, RES))
        for k in range(NL):
            out[k] = (lab == k).reshape(RES, SS, RES, SS).mean(axis=(1, 3))
        return out

    def uniform(self, cx, cz):
        j = (cz - self.z0) * self.n
        i = (cx - self.x0) * self.n
        lab = self.lab[j:j + self.n, i:i + self.n]
        first = lab[0, 0]
        return int(first) if (lab == first).all() else None


def triangle(p, q, r):
    """A filled triangle predicate."""
    def pred(X, Z):
        def side(a, b):
            return (b[0] - a[0]) * (Z - a[1]) - (b[1] - a[1]) * (X - a[0])
        s1, s2, s3 = side(p, q), side(q, r), side(r, p)
        return ((s1 >= 0) & (s2 >= 0) & (s3 >= 0)) | ((s1 <= 0) & (s2 <= 0) & (s3 <= 0))
    return pred


# ------------------------------------------------------------------------------------------
# Numerals: a squared block face, each digit drawn in a 4 x 6 grid of 1 ft units (4 ft wide,
# 6 ft tall, as a field's numbers are), strokes a foot wide. (x right, y down, in the digit's
# own frame: its top is y = 0.)
# ------------------------------------------------------------------------------------------
DIGITS = {
    "0": [(0, 0, 4, 1), (0, 5, 4, 6), (0, 0, 1, 6), (3, 0, 4, 6)],
    "1": [(1.5, 0, 2.5, 6), (0.75, 0, 1.5, 1), (0.75, 5, 3.25, 6)],
    "2": [(0, 0, 4, 1), (3, 0, 4, 3.5), (0, 2.5, 4, 3.5), (0, 2.5, 1, 6), (0, 5, 4, 6)],
    "3": [(0, 0, 4, 1), (3, 0, 4, 6), (1, 2.5, 4, 3.5), (0, 5, 4, 6)],
    "4": [(0, 0, 1, 3.5), (0, 2.5, 4, 3.5), (2.75, 0, 3.75, 6)],
    "5": [(0, 0, 4, 1), (0, 0, 1, 3.5), (0, 2.5, 4, 3.5), (3, 2.5, 4, 6), (0, 5, 4, 6)],
}


def digit_rects(ch, ox, oz, right, down):
    """A digit's rectangles in world blocks: the digit's top-left corner at (ox, oz), its x axis
    ``right`` and its y axis ``down`` (unit vectors along the world axes)."""
    out = []
    for (a, b, c, d) in DIGITS[ch]:
        pts = [(ox + right[0] * u * FT + down[0] * v * FT, oz + right[1] * u * FT + down[1] * v * FT)
               for (u, v) in ((a, b), (c, d))]
        out.append((pts[0][0], pts[1][0], pts[0][1], pts[1][1]))
    return out


# ------------------------------------------------------------------------------------------
# The football field
# ------------------------------------------------------------------------------------------
class FootballField(object):
    """A pro field: 120 yd x 53 1/3 yd, 6 ft white border, 8 in goal lines, 4 in lines, hash
    marks 70 ft 9 in from the sidelines, 2 ft yard ticks inside the sidelines, numbers every 10
    yards with arrows, 2-yard line marks, end zones of the end zone colour."""

    kind = "football"
    centre = (0.5, 0.5)
    half_len = 50 * YD             # midfield to the goal line's field edge
    zone = 10 * YD                 # an end zone, the goal line included
    half_w = 80 * FT               # midfield to the sideline's inner edge
    border = 6 * FT
    line = 4.0 / RES               # 4 in lines drawn 4 texels (4 in is 3.25)
    goal_line = 8 * IN
    hash_from_sideline = 70 * FT + 9 * IN
    hash_len = 2 * FT
    tick_in = 8 * IN
    number_top = 9 * YD            # the numbers' tops, from the sideline
    number_h = 6 * FT
    digit_gap = 6 * IN             # yard line centre to a digit's inner edge
    extent = (-57, 58, -26, 27)    # cells: border and end lines included

    def __init__(self):
        cx, cz = self.centre
        self.gx = (cx - self.half_len, cx + self.half_len)            # field edges of goal lines
        self.ex = (cx - self.half_len - self.zone, cx + self.half_len + self.zone)   # end lines
        self.sz = (cz - self.half_w, cz + self.half_w)                # sideline inner edges

    def stripe_dark(self, x):
        """Five-yard mowing stripes from goal line to goal line, a stripe's edges under the yard
        lines; the end zones light."""
        d = x - self.gx[0]
        if d < 0 or x > self.gx[1]:
            return False
        return int(d // (5 * YD)) % 2 == 1

    def paint(self, cv):
        cx, cz = self.centre
        h = self.line / 2
        # mowing stripes
        for k in range(20):
            if k % 2 == 1:
                cv.rect(self.gx[0] + 5 * k * YD, self.gx[0] + 5 * (k + 1) * YD, self.sz[0],
                        self.sz[1], TURF_D)
        # end zones (inside the goal line and end line)
        cv.rect(self.ex[0], self.gx[0] - self.goal_line, self.sz[0], self.sz[1], ZONE)
        cv.rect(self.gx[1] + self.goal_line, self.ex[1], self.sz[0], self.sz[1], ZONE)
        # goal lines, in the end zone
        cv.rect(self.gx[0] - self.goal_line, self.gx[0], self.sz[0], self.sz[1], WHITE)
        cv.rect(self.gx[1], self.gx[1] + self.goal_line, self.sz[0], self.sz[1], WHITE)
        # yard lines every 5 yards
        for k in range(1, 20):
            x = self.gx[0] + 5 * k * YD
            cv.rect(x - h, x + h, self.sz[0], self.sz[1], WHITE)
        # 2-yard marks: a yard long, centred
        for x in (self.gx[0] + 2 * YD, self.gx[1] - 2 * YD):
            cv.rect(x - h, x + h, cz - YD / 2, cz + YD / 2, WHITE)
        # hash marks and sideline ticks, every yard but the 5s
        for k in range(1, 100):
            if k % 5 == 0:
                continue
            # a hash's centre on every second texel from the field's centre: a texel off at
            # most, half the pictures, and the far half's still the near half's turned
            x = self.centre[0] + round((self.gx[0] + k * YD - self.centre[0]) * RES / 2.0)                 * 2.0 / RES
            for side in (0, 1):
                s = self.sz[side]
                d = 1 if side == 0 else -1        # into the field
                a = s + d * (self.hash_from_sideline - self.hash_len)
                b = s + d * self.hash_from_sideline
                cv.rect(x - h, x + h, a, b, WHITE)
                a = s + d * self.tick_in
                b = s + d * (self.tick_in + self.hash_len)
                cv.rect(x - h, x + h, a, b, WHITE)
        # numbers and arrows
        for (x0, x1, z0, z1) in self.number_rects():
            cv.rect(x0, x1, z0, z1, WHITE)
        for tri in self.arrows():
            xs = [p[0] for p in tri]
            zs = [p[1] for p in tri]
            cv.shape((min(xs), max(xs), min(zs), max(zs)), triangle(*tri), WHITE)
        # the border: 6 ft of white outside the sidelines and end lines
        b = self.border
        cv.rect(self.ex[0] - b, self.ex[1] + b, self.sz[0] - b, self.sz[0], WHITE)
        cv.rect(self.ex[0] - b, self.ex[1] + b, self.sz[1], self.sz[1] + b, WHITE)
        cv.rect(self.ex[0] - b, self.ex[0], self.sz[0] - b, self.sz[1] + b, WHITE)
        cv.rect(self.ex[1], self.ex[1] + b, self.sz[0] - b, self.sz[1] + b, WHITE)

    def numbered_lines(self):
        """(x, label, side of the field) for every numbered yard line: side -1 west half."""
        out = []
        for k in range(1, 10):
            x = self.gx[0] + 10 * k * YD
            yards = 10 * k if k <= 5 else 100 - 10 * k
            out.append((x, "%d" % yards, -1 if k < 5 else (0 if k == 5 else 1)))
        return out

    def number_rects(self):
        out = []
        for (x, label, _side) in self.numbered_lines():
            for near in (True, False):
                out += self._number(x, label, near)
        return out

    def _frame(self, near):
        """A number's frame: its baseline row and its top; ``right`` and ``down`` the digit's x
        and y axes in the world, reading it from beyond its near sideline."""
        if near:            # the south (home) half: tops towards the south sideline
            top = self.sz[1] - self.number_top
            return top, (-1, 0), (0, -1)
        top = self.sz[0] + self.number_top
        return top, (1, 0), (0, 1)

    def _number(self, x, label, near):
        top, right, down = self._frame(near)
        w = 4 * FT
        g = self.digit_gap
        rects = []
        # the first digit sits before the line along the reading direction, the second after
        first_left = x - right[0] * (g + w)        # its left edge (along ``right``)
        second_left = x + right[0] * g
        for ch, left in ((label[0], first_left), (label[1], second_left)):
            rects += digit_rects(ch, left, top, right, down)
        return rects

    def arrows(self):
        """The direction arrows: an isosceles triangle 18 in across its base and 36 in along its
        sides, its tip towards the nearer goal, beside the number's outer digit at its top."""
        out = []
        base = 18 * IN
        length = math.sqrt((36 * IN) ** 2 - (base / 2) ** 2)
        for (x, label, side) in self.numbered_lines():
            if side == 0:
                continue
            for near in (True, False):
                top, right, down = self._frame(near)
                dirx = side                                    # towards the nearer goal: -1 west
                outer = x + dirx * (self.digit_gap + 4 * FT + 6 * IN)
                zc = top + down[1] * base / 2 + down[1] * 3 * IN
                tip = (outer + dirx * length, zc)
                out.append(((outer, zc - base / 2), (outer, zc + base / 2), tip))
        return out

    def ground(self, cx, cz):
        return True


# ------------------------------------------------------------------------------------------
# The soccer pitch
# ------------------------------------------------------------------------------------------
class SoccerPitch(object):
    """105 x 68 m, 12 cm lines, to the Laws of the Game: halfway line, centre circle (9.15 m)
    and spot, penalty areas (16.5 m x 40.32 m), goal areas (5.5 m x 18.32 m), penalty spots
    (11 m) and arcs (9.15 m from the spot), corner arcs (1 m). Five metre mowing stripes across
    the pitch on block edges, the middle one centred on the halfway line."""

    kind = "soccer"
    centre = (0.5, 0.0)
    line = 0.12
    extent = (-54, 55, -36, 36)

    def __init__(self):
        cx, cz = self.centre
        self.gx = (cx - 52.5, cx + 52.5)        # goal lines' outer edges
        self.tz = (cz - 34.0, cz + 34.0)        # touchlines' outer edges

    def stripe_dark(self, x):
        k = int(math.floor((x - self.centre[0] + 2.5) / 5.0))
        return k % 2 != 0

    def paint(self, cv):
        cx, cz = self.centre
        w = self.line
        gx0, gx1 = self.gx
        tz0, tz1 = self.tz
        for k in range(-10, 11):
            if k % 2 != 0:
                cv.rect(cx - 2.5 + 5 * k, cx + 2.5 + 5 * k, tz0, tz1, TURF_D)
        # touchlines, goal lines, halfway line
        cv.rect(gx0, gx1, tz0, tz0 + w, WHITE)
        cv.rect(gx0, gx1, tz1 - w, tz1, WHITE)
        cv.rect(gx0, gx0 + w, tz0, tz1, WHITE)
        cv.rect(gx1 - w, gx1, tz0, tz1, WHITE)
        cv.rect(cx - w / 2, cx + w / 2, tz0, tz1, WHITE)
        # centre circle and spot
        r = 9.15
        cv.shape((cx - r, cx + r, cz - r, cz + r),
                 lambda X, Z: np.abs(np.hypot(X - cx, Z - cz) - (r - w / 2)) <= w / 2, WHITE)
        cv.shape((cx - 0.2, cx + 0.2, cz - 0.2, cz + 0.2),
                 lambda X, Z: np.hypot(X - cx, Z - cz) <= 0.11, WHITE)
        for end in (-1, 1):
            g = gx0 if end < 0 else gx1
            d = -end                              # into the pitch
            # penalty area and goal area: their lines part of the areas
            for depth, half in ((16.5, 20.16), (5.5, 9.16)):
                far = g + d * depth
                cv.rect(far - d * w, far, cz - half, cz + half, WHITE)
                cv.rect(g, far, cz - half, cz - half + w, WHITE)
                cv.rect(g, far, cz + half - w, cz + half, WHITE)
            spot = (g + d * 11.0, cz)
            cv.shape((spot[0] - 0.2, spot[0] + 0.2, cz - 0.2, cz + 0.2),
                     lambda X, Z, s=spot: np.hypot(X - s[0], Z - s[1]) <= 0.11, WHITE)
            far = g + d * 16.5
            cv.shape((spot[0] - r, spot[0] + r, cz - r, cz + r),
                     lambda X, Z, s=spot, f=far, dd=d: (np.abs(np.hypot(X - s[0], Z - s[1])
                                                            - (r - w / 2)) <= w / 2)
                     & ((X - f) * dd > 0), WHITE)
        # corner arcs, radius 1 m to the line's outer edge
        for (px, pz) in ((gx0, tz0), (gx0, tz1), (gx1, tz0), (gx1, tz1)):
            cv.shape((px - 1.1, px + 1.1, pz - 1.1, pz + 1.1),
                     lambda X, Z, a=px, b=pz: np.abs(np.hypot(X - a, Z - b) - (1.0 - w / 2))
                     <= w / 2, WHITE)
        # re-draw the outer lines over the arcs' ends outside the pitch: nothing outside
        cv.rect(gx0 - 2, gx0, tz0 - 2, tz1 + 2, TURF_L)
        cv.rect(gx1, gx1 + 2, tz0 - 2, tz1 + 2, TURF_L)
        cv.rect(gx0 - 2, gx1 + 2, tz0 - 2, tz0, TURF_L)
        cv.rect(gx0 - 2, gx1 + 2, tz1, tz1 + 2, TURF_L)

    def ground(self, cx, cz):
        return True


# ------------------------------------------------------------------------------------------
# Tint classes: which tint each texel of a cell takes
# ------------------------------------------------------------------------------------------
# A picture is drawn once, in the light mowing tone; the tone is a tint the painted cell takes
# from the plain turf beside it, so a picture serves both stripes. Each texel is in one class:
FIXED, STRIPE, WEST, EAST, ZONE = -1, 0, 1, 2, 3
# FIXED: off the field (the border, the surround): never tinted. STRIPE: on the field, one tone.
# WEST, EAST: on the field in a cell a stripe edge crosses (under a yard line): the tone on that
# side of it. ZONE: the end zone, tinted with the end zone colour.


def split_col(field, cx):
    """The texel column (0..32) of a mowing stripe's edge in cell column cx, or None."""
    if field.kind != "football":
        return None
    h = field.line / 2
    for k in range(1, 20):
        x = snap(field.gx[0] + 5 * k * YD)
        left, right = int(round((x - h) * RES)) - cx * RES, int(round((x + h) * RES)) - cx * RES
        if right > 0 and left < RES:
            return max(0, min(RES, int(round(x * RES)) - cx * RES))
    return None


def class_map(field, cx, cz):
    """A cell's (RES, RES) tint classes, row 0 its north edge."""
    xs = cx + (np.arange(RES) + 0.5) / RES
    zs = cz + (np.arange(RES) + 0.5) / RES
    X, Z = np.meshgrid(xs, zs)
    out = np.full((RES, RES), FIXED, dtype=np.int8)
    if field.kind == "football":
        x0, x1 = field.gx
        z0, z1 = field.sz
        on = (X >= x0) & (X <= x1) & (Z >= z0) & (Z <= z1)
        zone = (((X >= field.ex[0]) & (X <= x0 - field.goal_line))
                | ((X >= x1 + field.goal_line) & (X <= field.ex[1]))) & (Z >= z0) & (Z <= z1)
        out[zone] = ZONE
        s = split_col(field, cx)
        if s is None:
            out[on] = STRIPE
        else:
            west = np.arange(RES)[None, :].repeat(RES, 0) < s
            out[on & west] = WEST
            out[on & ~west] = EAST
    else:
        on = (X >= field.gx[0]) & (X <= field.gx[1]) & (Z >= field.tz[0]) & (Z <= field.tz[1])
        out[on] = STRIPE
    return out


def turn_classes(cm, t):
    """A class map turned t quarter turns clockwise (t even only: a stripe edge stays north-
    south); turned 180, what was west of the edge is east of it."""
    out = np.rot90(cm, -t).copy()
    if t % 4 == 2:
        w = out == WEST
        out[out == EAST] = WEST
        out[w] = EAST
    return out


# ------------------------------------------------------------------------------------------
# Census and pictures
# ------------------------------------------------------------------------------------------
def canvas(field):
    x0, x1, z0, z1 = field.extent
    cv = Canvas(x0, x1, z0, z1)
    field.paint(cv)
    return cv


def census(field, cv):
    """{(cx, cz): ('uniform', label, class) | ('picture', cov, classes)} over the field's
    extent. Pictures are tone free: the dark stripe is a tint."""
    x0, x1, z0, z1 = field.extent
    out = {}
    for cz in range(z0, z1 + 1):
        for cx in range(x0, x1 + 1):
            cm = class_map(field, cx, cz)
            u = cv.uniform(cx, cz)
            if u == TURF_D:
                u = TURF_L
            first = cm[0, 0]
            if u is not None and (cm == first).all() and first in (FIXED, STRIPE, ZONE):
                out[(cx, cz)] = ("uniform", u, int(first))
                continue
            cov = cv.coverage(cx, cz)
            cov[TURF_L] += cov[TURF_D]
            cov[TURF_D] = 0
            if u is not None and (cm == first).all() and first in (FIXED, STRIPE, ZONE):
                out[(cx, cz)] = ("uniform", u, int(first))
            else:
                out[(cx, cz)] = ("picture", cov, cm)
    return out


def picture_key(cov, cm):
    return np.rint(cov * 36).astype(np.int16).tobytes() + cm.tobytes()


def turned(cov, t):
    """A picture turned t quarter turns clockwise seen from above (a blockstate's y / 90)."""
    return np.rot90(cov, -t, axes=(1, 2))


def canonical(cov, cm):
    """(key, t, cov, cm): the picture as stored (unturned, or turned 180 degrees, whichever key
    sorts first), and t, 0 or 1, the half turns that take the stored picture back to this one.
    Only half turns: a stored picture's stripe edges must stay north-south, the axis its painted
    cell's tint looks along."""
    a = (picture_key(cov, cm), 0, cov, cm)
    c2, m2 = turned(cov, 2), turn_classes(cm, 2)
    b = (picture_key(c2, m2), 1, c2, m2)
    return a if a[0] <= b[0] else b


def plain_picture(label, cls):
    cov = np.zeros((NL, RES, RES))
    cov[label] = 1.0
    return cov, np.full((RES, RES), cls, dtype=np.int8)


# ------------------------------------------------------------------------------------------
# Anchors: a regular grid, each owning the picture cells nearest it
# ------------------------------------------------------------------------------------------
REACH = 12          # how far (each way) a painted cell looks for its anchor


def anchor_grid(field):
    x0, x1, z0, z1 = field.extent
    span = 2 * REACH + 1

    def centres(a, b):
        size = b - a + 1
        n = int(math.ceil(size / float(span)))
        step = size / float(n)
        return [int(math.floor(a + step * (k + 0.5))) for k in range(n)]
    return [(x, z) for z in centres(z0, z1) for x in centres(x0, x1)]


def plan(field, cen):
    """[(anchor cell, [picture cells])]: every picture cell given to its nearest anchor (in
    rings, then by distance, then west and north first), within REACH."""
    anchors = anchor_grid(field)
    owned = [[] for _ in anchors]
    for c, v in sorted(cen.items()):
        if v[0] != "picture":
            continue
        best = None
        for n, a in enumerate(anchors):
            ring = max(abs(c[0] - a[0]), abs(c[1] - a[1]))
            key = (ring, (c[0] - a[0]) ** 2 + (c[1] - a[1]) ** 2, n)
            if ring <= REACH and (best is None or key < best[0]):
                best = (key, n)
        if best is None:
            raise SystemExit("%s: cell %s has no anchor within %d" % (field.kind, c, REACH))
        owned[best[1]].append(c)
    return list(zip(anchors, owned))


def main():
    for field in (FootballField(), SoccerPitch()):
        cv = canvas(field)
        cen = census(field, cv)
        keys = set()
        n = 0
        for c, v in cen.items():
            if v[0] != "picture":
                continue
            n += 1
            keys.add(canonical(v[1], v[2])[0])
        print(field.kind, "picture cells", n, "pictures", len(keys), "anchors",
              len(anchor_grid(field)))


if __name__ == "__main__" and "--analyse" in sys.argv:
    main()
