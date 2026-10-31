"""
ballpark_field.py -- the baseball and softball diamonds as geometry, for gen_venues_ballpark.py.

Not run on its own (``--analyse`` prints the cell census for a developer). It holds:

  * the field: every line, circle and region of a full-size diamond laid with its centre line
    along the grid (home plate at the south, centre field to the north), in blocks, one block a
    metre, as numpy predicates on world points;
  * the raster: a cell's 32 x 32 coverage of each surface (turf, infield skin, packing clay,
    warning track, chalk, rubber), supersampled;
  * the standard pieces (a turf/clay diagonal, a chalk line cutting a corner, ...) as the same
    coverages, so the generator can tell which cells a plain block draws and which need a
    picture round a marking anchor.

The canonical frame: x east, z south (Minecraft's), the home plate's back point at
``P = (0.5, 1.0)``, the centre line x = 0.5 running north. The foul lines are x + z = 1.5 (first
base, north-east) and x - z = -0.5 (third base, north-west): half-integers, so a 45 degree line
crosses every cell it meets through two mid-edges and a chalk line is a band across a corner,
never pinched to a point at a block corner. Surface boundaries at 45 degrees are on integer
diagonals instead (x + z = 0, 3, ...), so they split a cell corner to corner.
"""
import math
import sys

import numpy as np

RES = 32           # texels a block
SS = 3             # samples a texel, each way
SQ2 = math.sqrt(2.0)
FT = 0.3048
IN = 0.0254

TURF, CLAY, PACK, TRACK, CHALK, RUBBER = range(6)
LABELS = ("turf", "clay", "pack", "track", "chalk", "rubber")
NL = len(LABELS)

W = 0.09           # a chalk line (3 in), drawn a little wide so it reads at 32 px a block
P = (0.5, 1.0)     # the back point of home plate
PLATE_DEPTH = 17 * IN
PLATE_HALF = 8.5 * IN
HC = (0.5, 1.0 - PLATE_DEPTH / 2.0)   # home plate's middle


def t1(X, Z):
    """Distance along the first base foul line from home's point."""
    return ((X - P[0]) - (Z - P[1])) / SQ2


def n1(X, Z):
    """Distance out from the first base foul line into foul territory (negative: fair)."""
    return (X + Z - 1.5) / SQ2


def t3(X, Z):
    return (-(X - P[0]) - (Z - P[1])) / SQ2


def n3(X, Z):
    return (Z - X - 0.5) / SQ2


def from_tn1(t, n):
    """World point at t along the first base line and n out from it."""
    return (P[0] + (t + n) / SQ2, P[1] - (t - n) / SQ2)


def from_tn3(t, n):
    x, z = from_tn1(t, n)
    return (1.0 - x, z)


def disc(X, Z, c, r):
    return (X - c[0]) ** 2 + (Z - c[1]) ** 2 <= r * r


def ring(X, Z, c, r, w=W):
    return np.abs(np.hypot(X - c[0], Z - c[1]) - r) <= w / 2.0


def rect(X, Z, x0, x1, z0, z1):
    return (X >= x0) & (X <= x1) & (Z >= z0) & (Z <= z1)


def outline(X, Z, x0, x1, z0, z1, w=W, sides="nsew"):
    """A box's chalk outline drawn inside its outer edges."""
    inside = rect(X, Z, x0, x1, z0, z1)
    m = np.zeros(X.shape, dtype=bool)
    if "w" in sides:
        m |= inside & (X <= x0 + w)
    if "e" in sides:
        m |= inside & (X >= x1 - w)
    if "n" in sides:
        m |= inside & (Z <= z0 + w)
    if "s" in sides:
        m |= inside & (Z >= z1 - w)
    return m


def seg(X, Z, a, b, w=W):
    du, dv = b[0] - a[0], b[1] - a[1]
    ll = du * du + dv * dv
    t = np.clip(((X - a[0]) * du + (Z - a[1]) * dv) / ll, 0, 1)
    return (X - a[0] - t * du) ** 2 + (Z - a[1] - t * dv) ** 2 <= (w / 2.0) ** 2


def tn_box_lines(X, Z, mirror, t0, t1_, n0, n1_, w=W):
    """A coach's box: three chalk sides of a rectangle in a foul line's frame (along t, out n),
    the side nearest the line left open."""
    f = from_tn3 if mirror else from_tn1
    a, b, c, d = f(t0, n1_), f(t1_, n1_), f(t0, n0), f(t1_, n0)
    return seg(X, Z, a, b, w) | seg(X, Z, c, a, w) | seg(X, Z, d, b, w)


class Field(object):
    """One diamond: its geometry and the anchors that carry its pictures."""

    def __init__(self, kind):
        self.kind = kind
        bb = kind == "baseball"
        self.L = 90 * FT if bb else 60 * FT
        s = 7.5 * IN                                   # half a base
        self.bags = {
            "first": (P[0] + (self.L - 2 * s) / SQ2, P[1] - self.L / SQ2),
            "third": (1.0 - (P[0] + (self.L - 2 * s) / SQ2), P[1] - self.L / SQ2),
            "second": (0.5, P[1] - math.hypot(self.L, self.L)),
        }
        if bb:
            self.box_w, self.box_l = 4 * FT, 6 * FT
            self.catcher_w, self.catcher_l = 43 * IN, 8 * FT
            self.mound = (0.5, P[1] - 59 * FT)
            self.mound_r = 9 * FT
            self.rubber_front = (0.5, P[1] - 60.5 * FT)
            self.arc_c, self.arc_r = self.rubber_front, 95 * FT
            self.home_r = 13 * FT
            self.cut_r = 13 * FT
            self.foul_end = 105.0       # past the wall at x = 72: the wall's cells end it
        else:
            self.box_w, self.box_l = 3 * FT, 7 * FT
            self.catcher_w, self.catcher_l = 2 * (6 * IN + 8.5 * IN) + 2 * 3 * FT, 10 * FT
            self.plate_front = (0.5, P[1] - 43 * FT)
            self.circle_r = 8 * FT
            self.arc_c, self.arc_r = self.plate_front, 60 * FT
            self.foul_end = 90.0        # past the ground's edge, which ends it
        # the batter's boxes, 6 in off the plate, their middle level with the plate's
        inner = PLATE_HALF + 6 * IN
        z0, z1 = HC[1] - self.box_l / 2.0, HC[1] + self.box_l / 2.0
        self.box_first = (0.5 + inner, 0.5 + inner + self.box_w, z0, z1)
        self.box_third = (0.5 - inner - self.box_w, 0.5 - inner, z0, z1)
        self.catcher = (0.5 - self.catcher_w / 2.0, 0.5 + self.catcher_w / 2.0, z1,
                        z1 + self.catcher_l)
        # where a foul line comes out of the batter's box: it starts there
        t = 0.0
        x0, x1, bz0, bz1 = self.box_first
        while True:
            x, z = from_tn1(t, 0.0)
            if not (x0 - W <= x <= x1 + W and bz0 - W <= z <= bz1 + W) and t > 0.5:
                break
            t += 0.005
        self.foul_start = t
        # the coach's boxes: 20 ft x 10 ft, 15 ft out from the line, ending level with the base
        self.coach_t = (90 * FT - 20 * FT, 90 * FT)
        if not bb:
            # softball's box sits seven cells nearer home along the diagonal, so its anchor
            # lands on the same fraction of a cell and baseball's picture serves
            back = 7 * SQ2
            self.coach_t = (self.coach_t[0] - back, self.coach_t[1] - back)
        self.coach_n = (15 * FT, 25 * FT)
        if not bb:
            # and a cell further out, clear of the skin beside the foul line
            self.coach_n = (self.coach_n[0] + SQ2, self.coach_n[1] + SQ2)
        # the on-deck circles: 5 ft across, a block's middle each side of home
        self.on_deck = [(12.5, 0.5), (-11.5, 0.5)]
        self.on_deck_r = 2.5 * FT

    # ------------------------------------------------------------------------------------
    def walls(self):
        """The outfield wall's cells (baseball): (x, z, piece, facing), where piece is
        'straight' or 'diagonal'. Facing as the venue blocks keep it: towards the field."""
        out = []
        if self.kind != "baseball":
            return out
        for z in range(-77, -65):
            out.append((72, z, "straight", "west"))
            out.append((-72, z, "straight", "east"))
        for k in range(0, 44):
            out.append((71 - k, -78 - k, "diagonal", "south"))   # faces south-west
            out.append((-71 + k, -78 - k, "diagonal", "east"))   # faces south-east
        for x in range(-27, 28):
            out.append((x, -122, "straight", "south"))
        return out

    def ground(self, cx, cz):
        """Whether a cell is part of the field's ground in the layout."""
        if self.kind == "baseball":
            return (-71 <= cx <= 71 and -121 <= cz <= 14 and cx - cz <= 149
                    and -cx - cz <= 149)
        return -45 <= cx <= 46 and -60 <= cz <= 10 and n1(cx + .5, cz + .5) < 13 \
            and n3(cx + .5, cz + .5) < 13

    # ------------------------------------------------------------------------------------
    def labels(self, X, Z):
        """The surface at each world point, as label indices."""
        lab = np.full(X.shape, TURF, dtype=np.int8)
        fair = (n1(X, Z) <= 0) & (n3(X, Z) <= 0)
        in_arc = disc(X, Z, self.arc_c, self.arc_r)
        T1, T3 = t1(X, Z), t3(X, Z)
        if self.kind == "baseball":
            lab[fair & in_arc] = CLAY
            grass = (X + Z <= 0) & (X - Z >= 1) & (X - Z <= 37) & (X + Z >= -36)
            lab[grass] = TURF
            strip1 = (X + Z >= 0) & (X + Z <= 3) & (T1 >= -1) & (T1 <= self.L + 1)
            strip3 = (X - Z <= 1) & (X - Z >= -2) & (T3 >= -1) & (T3 <= self.L + 1)
            lab[strip1 | strip3] = CLAY
            lab[disc(X, Z, HC, self.home_r)] = CLAY
            lab[disc(X, Z, self.bags["first"], self.cut_r)] = CLAY
            lab[disc(X, Z, self.bags["third"], self.cut_r)] = CLAY
            lab[disc(X, Z, self.mound, self.mound_r)] = PACK
            track = ((Z < -116) | (X >= 67) | (X < -66) | (X - Z >= 142) | (-X - Z >= 141)
                     | (Z >= 10))
            lab[track] = TRACK
        else:
            skin = in_arc & (X + Z <= 6) & (X - Z >= -5)
            lab[skin] = CLAY
        for (cx, cz) in self.on_deck:
            lab[disc(X, Z, (cx, cz), self.on_deck_r)] = CLAY
        lab[self.chalk(X, Z)] = CHALK
        if self.kind == "softball":
            pf = self.plate_front
            lab[rect(X, Z, pf[0] - 12 * IN, pf[0] + 12 * IN, pf[1] - 6 * IN, pf[1])] = RUBBER
        return lab

    def chalk(self, X, Z):
        m = np.zeros(X.shape, dtype=bool)
        T1, T3 = t1(X, Z), t3(X, Z)
        m |= (np.abs(n1(X, Z)) <= W / 2) & (T1 >= self.foul_start) & (T1 <= self.foul_end)
        m |= (np.abs(n3(X, Z)) <= W / 2) & (T3 >= self.foul_start) & (T3 <= self.foul_end)
        m |= outline(X, Z, *self.box_first)
        m |= outline(X, Z, *self.box_third)
        x0, x1, z0, z1 = self.catcher
        m |= outline(X, Z, x0, x1, z0, z1, sides="ews")
        for mirror in (False, True):
            m |= tn_box_lines(X, Z, mirror, self.coach_t[0], self.coach_t[1], self.coach_n[0],
                              self.coach_n[1])
        for c in self.on_deck:
            m |= ring(X, Z, c, self.on_deck_r - W / 2)
        if self.kind == "softball":
            ring_m = ring(X, Z, self.plate_front, self.circle_r)
            m |= ring_m
        return m

    # ------------------------------------------------------------------------------------
    def anchors(self):
        """The marking anchors that carry the field's pictures, before the arc's:
        (name, cell, kind key, mirrored)."""
        out = []
        if self.kind == "baseball":
            out.append(("home", (0, 0), "bb_home", False))
            b1, b3 = self.bags["first"], self.bags["third"]
            out.append(("first cutout", (int(math.floor(b1[0])), int(math.floor(b1[1]))),
                        "bb_cutout", False))
            out.append(("third cutout", (int(math.floor(b3[0])), int(math.floor(b3[1]))),
                        "bb_cutout", True))
        else:
            out.append(("home", (0, 0), "sb_home", False))
            pf = self.plate_front
            out.append(("pitcher's circle", (0, int(math.floor(pf[1]))), "sb_circle", False))
        for mirror in (False, True):
            f = from_tn3 if mirror else from_tn1
            mx, mz = f(sum(self.coach_t) / 2, sum(self.coach_n) / 2)
            out.append(("coach's box", (int(math.floor(mx)), int(math.floor(mz))), "coach",
                        mirror))
        for (cx, cz) in self.on_deck:
            out.append(("on-deck circle", (int(math.floor(cx)), int(math.floor(cz))), "on_deck",
                        False))
        return out


# ------------------------------------------------------------------------------------------
# Rasters
# ------------------------------------------------------------------------------------------
# Sample points inside a cell: jittered off the half-texel grid by a different amount each way, so
# no sample sits exactly on a diagonal (x + z or x - z an integer or a half), where float error
# would decide which side it falls.
_TU = (np.arange(RES * SS) + 0.5 + 0.137) / (RES * SS)
_TV = (np.arange(RES * SS) + 0.5 + 0.271) / (RES * SS)


def coverage(field, cx, cz):
    """A cell's coverage of each label: an (NL, RES, RES) float array, row 0 the north edge."""
    X = cx + np.broadcast_to(_TU[None, :], (RES * SS, RES * SS))
    Z = cz + np.broadcast_to(_TV[:, None], (RES * SS, RES * SS))
    lab = field.labels(X, Z)
    out = np.zeros((NL, RES, RES))
    for k in range(NL):
        out[k] = (lab == k).reshape(RES, SS, RES, SS).mean(axis=(1, 3))
    return out


_TC = (np.arange(RES) + 0.5) / RES


def quick_labels(field, cx0, cx1, cz):
    """One row of cells at a texel a sample: (ncells, RES, RES) labels."""
    n = cx1 - cx0
    X = cx0 + np.arange(n * RES)[None, :].repeat(RES, 0) / RES + 0.5 / RES
    Z = cz + np.broadcast_to(_TC[:, None], (RES, n * RES))
    lab = field.labels(X, Z)
    return lab.reshape(RES, n, RES).transpose(1, 0, 2)


# ------------------------------------------------------------------------------------------
# Standard pieces, drawn facing north (unturned); their world rasters for each facing
# ------------------------------------------------------------------------------------------
def piece_coverage(fn, turns=0):
    """fn(U, V) -> label array over the cell (U east, V south, 0..1), as the world sees it with
    the piece turned ``turns`` quarter turns clockwise seen from above (a blockstate's y / 90):
    a world point is taken back to the piece's own frame, so the samples are the field's."""
    U = np.broadcast_to(_TU[None, :], (RES * SS, RES * SS))
    V = np.broadcast_to(_TV[:, None], (RES * SS, RES * SS))
    back = [(U, V), (V, 1 - U), (1 - U, 1 - V), (1 - V, U)][turns % 4]
    lab = fn(*back)
    out = np.zeros((NL, RES, RES))
    for k in range(NL):
        out[k] = (lab == k).reshape(RES, SS, RES, SS).mean(axis=(1, 3))
    return out


def lab_where(cond, a, b):
    return np.where(cond, a, b).astype(np.int8)


def band(U, V):
    """A chalk line across the north-west corner: u + v = 0.5."""
    return np.abs(U + V - 0.5) / SQ2 <= W / 2


def mid(U, V):
    """A chalk line down the middle, north to south."""
    return np.abs(U - 0.5) <= W / 2


PIECES = {}


def _pieces():
    def surf(s):
        return lambda U, V: np.full(U.shape, s, dtype=np.int8)

    def chalk_on(s, fn):
        return lambda U, V: lab_where(fn(U, V), CHALK, s)
    PIECES["turf"] = surf(TURF)
    PIECES["clay"] = surf(CLAY)
    PIECES["pack"] = surf(PACK)
    PIECES["track"] = surf(TRACK)
    # the turf/clay edges: turf on the north-west half, the north half, the north quarter
    PIECES["edge_diag"] = lambda U, V: lab_where(U + V < 1, TURF, CLAY)
    PIECES["edge_half"] = lambda U, V: lab_where(V < 0.5, TURF, CLAY)
    PIECES["edge_quarter"] = lambda U, V: lab_where((V < U) & (V < 1 - U), TURF, CLAY)
    PIECES["edge_quarter_in"] = lambda U, V: lab_where((V < U) & (V < 1 - U), CLAY, TURF)
    PIECES["track_diag"] = lambda U, V: lab_where(U + V < 1, TURF, TRACK)
    PIECES["track_half"] = lambda U, V: lab_where(V < 0.5, TURF, TRACK)
    PIECES["chalk_turf_band"] = chalk_on(TURF, band)
    PIECES["chalk_turf_mid"] = chalk_on(TURF, mid)
    PIECES["chalk_clay_band"] = chalk_on(CLAY, band)
    PIECES["chalk_clay_mid"] = chalk_on(CLAY, mid)
    PIECES["chalk_track_band"] = chalk_on(TRACK, band)
    PIECES["chalk_track_mid"] = chalk_on(TRACK, mid)


_pieces()
# facing -> quarter turns clockwise seen from above (a blockstate's y / 90)
FACING_TURNS = {"north": 0, "east": 1, "south": 2, "west": 3}
PIECE_COV = {(k, f): piece_coverage(fn, t) for k, fn in PIECES.items()
             for f, t in FACING_TURNS.items()}


def match_piece(cov):
    """The standard piece and facing a cell's coverage is, or None."""
    for (name, facing), pc in PIECE_COV.items():
        d = np.abs(pc - cov)
        if d.max() <= 0.02:
            return name, facing
    return None


def uniform_label(cov):
    for k in range(NL):
        if cov[k].min() > 0.999:
            return k
    return None


# ------------------------------------------------------------------------------------------
# A census of a field: what every cell is
# ------------------------------------------------------------------------------------------
def census(field, x0, x1, z0, z1):
    """Classifies every ground cell: {(cx, cz): ('uniform', label) | ('piece', name, facing) |
    ('picture', cov)}."""
    out = {}
    for cz in range(z0, z1 + 1):
        row = quick_labels(field, x0, x1 + 1, cz)
        for i, cx in enumerate(range(x0, x1 + 1)):
            if not field.ground(cx, cz):
                continue
            lab = row[i]
            first = lab.flat[0]
            if (lab == first).all():
                # a texel a sample can miss nothing a chalk line's width wide: check finely
                # only where the coarse labels disagree
                out[(cx, cz)] = ("uniform", int(first))
                continue
            cov = coverage(field, cx, cz)
            u = uniform_label(cov)
            if u is not None:
                out[(cx, cz)] = ("uniform", u)
                continue
            m = match_piece(cov)
            if m:
                out[(cx, cz)] = ("piece", m[0], m[1])
            else:
                out[(cx, cz)] = ("picture", cov)
    return out


EXTENT = {"baseball": (-71, 71, -121, 14), "softball": (-58, 59, -67, 10)}


def main():
    for kind in ("baseball", "softball"):
        f = Field(kind)
        c = census(f, *EXTENT[kind])
        kinds = {}
        for v in c.values():
            key = v[0] if v[0] != "piece" else "piece:" + v[1]
            kinds[key] = kinds.get(key, 0) + 1
        print(kind, "foul start %.3f" % f.foul_start, "bags", f.bags)
        for k in sorted(kinds):
            print("  %-24s %d" % (k, kinds[k]))
        pics = sorted(k for k, v in c.items() if v[0] == "picture")
        print("  picture cells:", len(pics))
        if "--cells" in sys.argv:
            print("  ", pics)


if __name__ == "__main__":
    if "--analyse" in sys.argv:
        main()


# ------------------------------------------------------------------------------------------
# Who draws each picture cell: the mound, a marking anchor, or an arc anchor
# ------------------------------------------------------------------------------------------
REACH = 7          # how far (each way) a painted cell looks for its anchor


def mound_cells(field):
    """The cells the baseball mound covers (a heightfield each), and its anchor (the rubber's
    cell)."""
    if field.kind != "baseball":
        return None, []
    c, r = field.mound, field.mound_r
    cells = []
    for cz in range(int(math.floor(c[1] - r)), int(math.floor(c[1] + r)) + 1):
        for cx in range(int(math.floor(c[0] - r)), int(math.floor(c[0] + r)) + 1):
            nx = min(max(c[0], cx), cx + 1)
            nz = min(max(c[1], cz), cz + 1)
            if math.hypot(nx - c[0], nz - c[1]) < r:
                cells.append((cx, cz))
    anchor = (0, int(math.floor(field.rubber_front[1] - 3 * IN)))
    return anchor, cells


def window_cells(field, centre, r):
    """Cells a circle of radius r round ``centre`` reaches."""
    out = set()
    for cz in range(int(math.floor(centre[1] - r)) - 1, int(math.floor(centre[1] + r)) + 2):
        for cx in range(int(math.floor(centre[0] - r)) - 1, int(math.floor(centre[0] + r)) + 2):
            out.add((cx, cz))
    return out


def plan(field, cen):
    """Assigns the census's picture cells: returns (anchors, owner) where anchors is a list of
    dicts {name, cell, key, mirror, cells} (cells: every cell its picture covers, plain ones
    included where its region asks) and owner maps a cell to the anchor index drawing it
    (-1 for the mound)."""
    anchors = []
    for name, cell, key, mirror in field.anchors():
        anchors.append({"name": name, "cell": cell, "key": key, "mirror": mirror,
                        "cells": set()})
    m_anchor, m_cells = mound_cells(field)
    owner = {}
    for c in m_cells:
        owner[c] = -1
    # regions: home and the cutouts take every cell of their circle's box; the small markings
    # only the cells their chalk touches
    # the small markings first: their pictures are drawn on plain turf and travel to any field,
    # so they keep every cell their chalk touches even inside a base cutout's square
    order = sorted(range(len(anchors)), key=lambda n: anchors[n]["key"] not in ("coach", "on_deck"))
    for i in order:
        a = anchors[i]
        if a["key"] in ("bb_home", "sb_home"):
            region = window_cells(field, HC, field.home_r if field.kind == "baseball"
                                  else 4.0)
            region = {c for c in region if max(abs(c[0] - a["cell"][0]),
                                               abs(c[1] - a["cell"][1])) <= 5}
        elif a["key"] == "bb_cutout":
            b = field.bags["third" if a["mirror"] else "first"]
            region = window_cells(field, b, field.cut_r)
            region = {c for c in region if max(abs(c[0] - a["cell"][0]),
                                               abs(c[1] - a["cell"][1])) <= 5}
        else:
            region = set()
            ax, az = a["cell"]
            for dz in range(-REACH, REACH + 1):
                for dx in range(-REACH, REACH + 1):
                    c = (ax + dx, az + dz)
                    if c in cen and cen[c][0] == "picture":
                        region.add(c)
            if a["key"] == "sb_circle":
                region |= {c for c in window_cells(field, field.plate_front, field.circle_r)
                           if c in cen}
            region = {c for c in region if marking_touches(field, a, c)}
        for c in region:
            if c in owner or c not in cen:
                continue
            owner[c] = i
            a["cells"].add(c)
    # the arc: what is left
    left = sorted(c for c, v in cen.items() if v[0] == "picture" and c not in owner)
    arc_anchors = arc_segments(field, left)
    for seg in arc_anchors:
        idx = len(anchors)
        anchors.append(seg)
        for c in seg["cells"]:
            owner[c] = idx
    # a band along the arc: the plain cells beside its cells, so painted blocks laid three wide
    # along it come out right
    for idx, seg in enumerate(anchors):
        if not seg["key"].startswith("arc"):
            continue
        ax, az = seg["cell"]
        extra = set()
        for (cx, cz) in seg["cells"]:
            for dz in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    c = (cx + dx, cz + dz)
                    if (c in cen and cen[c][0] == "uniform" and c not in owner
                            and max(abs(c[0] - ax), abs(c[1] - az)) <= REACH):
                        extra.add(c)
        for c in extra:
            owner[c] = idx
            seg["cells"].add(c)
    return anchors, owner, m_anchor


def marking_touches(field, a, c):
    """Whether a small marking's own chalk (or its disc) reaches cell c."""
    X = c[0] + np.broadcast_to(_TC[None, :], (RES, RES))
    Z = c[1] + np.broadcast_to(_TC[:, None], (RES, RES))
    if a["key"] == "coach":
        return tn_box_lines(X, Z, a["mirror"], field.coach_t[0], field.coach_t[1],
                            field.coach_n[0], field.coach_n[1], W * 1.5).any()
    if a["key"] == "on_deck":
        cc = (a["cell"][0] + 0.5, a["cell"][1] + 0.5)
        return disc(X, Z, cc, field.on_deck_r + W).any()
    if a["key"] == "sb_circle":
        pf = field.plate_front
        return (ring(X, Z, pf, field.circle_r, W * 1.5)
                | rect(X, Z, pf[0] - 13 * IN, pf[0] + 13 * IN, pf[1] - 7 * IN, pf[1])).any()
    return False


def arc_segments(field, cells):
    """Splits the arc's cells into segments, each a window of REACH round an anchor on the
    arc, first base side first; the third base side is its mirror (x -> -x)."""
    east = sorted([c for c in cells if c[0] >= 0],
                  key=lambda c: math.atan2(c[0] + 0.5 - field.arc_c[0],
                                           -(c[1] + 0.5 - field.arc_c[1])))
    segs = []
    todo = list(east)
    while todo:
        first = todo[0]
        # the anchor: the furthest cell along the arc whose window still holds the first
        best, most = first, -1
        for c in todo:
            if max(abs(c[0] - first[0]), abs(c[1] - first[1])) > REACH:
                continue
            n = sum(1 for d in todo if max(abs(d[0] - c[0]), abs(d[1] - c[1])) <= REACH)
            if n > most:
                best, most = c, n
        held = [c for c in todo if max(abs(c[0] - best[0]), abs(c[1] - best[1])) <= REACH]
        segs.append({"name": "arc %d" % (len(segs) + 1), "cell": best, "key": "arc%d" % len(
            segs), "mirror": False, "cells": set(held)})
        todo = [c for c in todo if c not in set(held)]
    out = []
    for s in segs:
        out.append(s)
        m = dict(s)
        m["mirror"] = True
        m["cell"] = (-s["cell"][0], s["cell"][1])
        m["cells"] = {(-c[0], c[1]) for c in s["cells"]}
        out.append(m)
    missing = [c for c in cells if c[0] < 0 and (-c[0], c[1]) not in east]
    if missing:
        raise SystemExit("arc cells with no first base twin: %s" % missing[:10])
    return out


def plan_report():
    for kind in ("baseball", "softball"):
        f = Field(kind)
        cen = census(f, *EXTENT[kind])
        anchors, owner, m_anchor = plan(f, cen)
        print(kind, "mound anchor", m_anchor)
        for a in anchors:
            print("  %-16s %-10s cell %-12s mirror %-5s cells %d" % (
                a["name"], a["key"], a["cell"], a["mirror"], len(a["cells"])))
        orphan = [c for c, v in cen.items() if v[0] == "picture" and c not in owner]
        print("  orphans", orphan)


if __name__ == "__main__" and "--plan" in sys.argv:
    plan_report()


def picture_key(cov):
    return np.rint(cov * 64).astype(np.int16).tobytes()


if __name__ == "__main__" and "--count" in sys.argv:
    pics = set()
    total = 0
    for kind in ("baseball", "softball"):
        f = Field(kind)
        cen = census(f, *EXTENT[kind])
        anchors, owner, m_anchor = plan(f, cen)
        for a in anchors:
            if a["mirror"]:
                continue
            for c in a["cells"]:
                cov = cen[c][1] if cen[c][0] == "picture" else coverage(f, *c)
                pics.add(picture_key(cov))
                total += 1
    print("cells in tables", total, "distinct pictures", len(pics))
