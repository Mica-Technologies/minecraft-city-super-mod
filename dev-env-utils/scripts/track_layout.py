"""
track_layout.py -- the 400 m track and its field events as venues_lines layouts, one block a
metre, every line where it really falls.

Not run on its own (gen_venues_track.py imports it); ``py -3 track_layout.py --preview DIR``
writes pictures of the layouts.

**The track** is the World Athletics standard 400 m track: two straights of 84.39 m and two
bends whose inner kerb (the inner edge of lane 1) has a radius of 36.50 m, eight lanes of
1.22 m, each lane's 5 cm line on its outer side, so lane n's outer line ends at 36.50 + 1.22 n
and the whole is 176.91 x 92.52 m to the outside of lane 8. A lane is measured 0.30 m from the
kerb in lane 1 and 0.20 m from its inner line in the others; lane 1's measurement line is 400 m.

The layout frame: the track's centre is (0.5, 0.0), the west edge of the middle of a cell
across, so a soccer pitch laid from the football generator's ``--layout soccer`` with the same
centre cell sits concentric (its halfway line is the middle of column 0, its touchlines on block
edges). The straights run east-west, the home straight to the south; running is anticlockwise
seen from above, so east along the home straight, the finish line at its east end (x = 42.695),
bend 1 the east bend and bend 2 the west.

Painted, in this order:

  * the surface: lanes and a 1 m apron outside lane 8, the home straight's chute carried west of
    bend 2 to 3 m behind the 110 m hurdles start, and the west D (the half disc inside bend 2)
    for the high jump fan and the javelin runway; grass everywhere else;
  * the raised inner kerb (5 x 5 cm), round the oval;
  * the lane lines, straight on the straights and the chute, arcs on the bends;
  * the finish line, the 100 m and 110 m hurdles starts across the chute, the 200 m staggers in
    bend 2, the 400 m staggers in bend 1, the 800 m staggers (green) in bend 1 and its break line
    (green) across the back straight where bend 1 ends; the 4 x 100 m relay takeover zones
    (yellow, 30 m, the scratch line 20 m in) and the 4 x 400 m first takeover zone (blue, 10 m
    each side of the finish); hurdle marks on the lane lines (100 m yellow, 110 m blue,
    400 m green); lane numbers before the finish and beyond the 100 m start;
  * the field events inside: the javelin runway (4 m, 30 m long) and its 8 m arc in the west D
    with its 28.96 degree sector into the infield, the shot put circle (2.135 m, a concrete
    floor, 34.92 degree sector) and the discus and hammer circle (2.50 m) in the east D.

**The jumps** (a layout of its own, laid beside the back straight): a 1.22 m runway 45 m long,
its take-off boards (long jump 2 m from the pit, triple jump 11 and 13 m) and a 2.75 x 9 m
sand pit with a concrete rim. **The pole vault**: a runway, the planting box (1 m long, 60 cm
tapering to 15 cm, steel) and the zero line, and a surfaced landing area for the pit.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import venues_lines as vl  # noqa: E402
import court_layouts as cl  # noqa: E402

W = 0.05
CX = 0.5                     # the centre's x; z = 0
HALF = 84.39 / 2             # half a straight
R0 = 36.50                   # the kerb's outer edge: lane 1's inner edge
LANE = 1.22
LANES = 8
APRON = 1.0
EX = CX + HALF               # the east bend's centre x (and the finish line)
WX = CX - HALF               # the west bend's centre x
ROUT = R0 + LANES * LANE     # 46.26: outside of lane 8
CHUTE = WX - (110 - 84.39) - 3.0       # the chute's west end: 3 m behind the 110 m start
V_WHITE = 14


def r_in(k):
    return R0 + (k - 1) * LANE


def r_line(k):
    """Lane k's outer line's outer edge."""
    return R0 + k * LANE


def r_meas(k):
    return R0 + 0.30 if k == 1 else R0 + (k - 1) * LANE + 0.20


def R(x0, z0, x1, z1, mat):
    return vl.Op(mat, [vl.rect(x0, z0, x1, z1)])


# ------------------------------------------------------------------------------------------
# Lane positions: distance along lane k's measurement line from the finish line, forwards
# ------------------------------------------------------------------------------------------
def lane_at(k, d):
    """Where distance d (metres past the finish, along lane k's measurement line) falls:
    ('bend', centre, angle_fn) or ('straight', x, z-sign). Returns (kind, params)."""
    r = r_meas(k)
    bend = math.pi * r
    if d < 0:                            # before the finish: the home straight and its chute
        return ("straight", EX + d, 1)
    d = d % (2 * bend + 4 * HALF)
    if d < bend:                         # bend 1, east: from south (phi 0) round to north
        return ("bend", (EX, 0.0), d / r, 1)
    d -= bend
    if d < 2 * HALF:                     # back straight, running west at z < 0
        return ("straight", EX - d, -1)
    d -= 2 * HALF
    if d < bend:                         # bend 2, west: from north round to south
        return ("bend", (WX, 0.0), d / r, -1)
    d -= bend
    return ("straight", WX + d, 1)       # home straight, running east at z > 0


def bend_point(centre, phi, side, r):
    """A point on a bend: side 1 the east bend (phi from south through east), -1 the west (phi
    from north through west)."""
    cx, cz = centre
    if side == 1:
        return (cx + r * math.sin(phi), cz + r * math.cos(phi))
    return (cx - r * math.sin(phi), cz - r * math.cos(phi))


def across(k, d, mat, ra=None, rb=None, w=W):
    """A line across lane k (from radius ra to rb, its inner edge to its outer line by default)
    at distance d past the finish along its measurement line, w wide, its far edge (the way the
    runners go) on the mark."""
    ra = r_in(k) if ra is None else ra
    rb = r_line(k) - W if rb is None else rb
    where = lane_at(k, d)
    if where[0] == "straight":
        x, sgn = where[1], where[2]
        if sgn > 0:      # home straight, running east: line from x - w to x
            return R(x - w, sgn * ra, x, sgn * rb, mat)
        return R(x, sgn * ra, x + w, sgn * rb, mat)
    _kind, centre, phi, side = where
    phi_mid = phi - (w / 2) / r_meas(k)
    p = bend_point(centre, phi_mid, side, ra - 0.6)
    q = bend_point(centre, phi_mid, side, rb + 0.6)
    return vl.Op(mat, [vl.strip(p, q, w), vl.Disc(centre[0], centre[1], rb)],
                 [vl.Disc(centre[0], centre[1], ra)])


def tick(k, d, mat, edge):
    """A hurdle mark: 10 cm of line, 5 cm along the lane, on lane k's inner (0) or outer (1)
    edge at distance d."""
    if edge == 0:
        return across(k, d, mat, r_in(k), r_in(k) + 0.10)
    return across(k, d, mat, r_line(k) - W - 0.10, r_line(k) - W)


# ------------------------------------------------------------------------------------------
# The track
# ------------------------------------------------------------------------------------------
def bend_ring(cx, r0, r1, mat, east):
    return vl.ring(cx, 0.0, r0, r1, mat, vl.half(cx, 0.0, 1 if east else -1, 0))


def surface_ops():
    ops = []
    for east, cx in ((True, EX), (False, WX)):
        ops.append(bend_ring(cx, R0 - W, ROUT + APRON, "SURFACE", east))
    for sgn in (-1, 1):
        x0 = CHUTE if sgn > 0 else WX
        ops.append(R(x0, sgn * (R0 - W), EX, sgn * (ROUT + APRON), "SURFACE"))
    # the west D: the half disc inside bend 2
    ops.append(vl.Op("SURFACE", [vl.Disc(WX, 0.0, R0 - W), vl.half(WX, 0.0, -1, 0)]))
    return ops


def kerb_ops():
    ops = [bend_ring(EX, R0 - W, R0, "KERB", True), bend_ring(WX, R0 - W, R0, "KERB", False)]
    for sgn in (-1, 1):
        ops.append(R(WX, sgn * (R0 - W), EX, sgn * R0, "KERB"))
    return ops


def lane_line_ops():
    ops = []
    for k in range(1, LANES + 1):
        a, b = r_line(k) - W, r_line(k)
        ops.append(bend_ring(EX, a, b, "LINE", True))
        ops.append(bend_ring(WX, a, b, "LINE", False))
        ops.append(R(CHUTE, a, EX, b, "LINE"))
        ops.append(R(WX, -b, EX, -a, "LINE"))
    # the chute's lane 1 inner line, west of bend 2 (the kerb stops where the oval does)
    ops.append(R(CHUTE, R0 - W, WX, R0, "LINE"))
    return ops


def numbers(x_left, ux, uz, h=0.8):
    """Lane numbers 1-8, one in each lane on the home straight, read along (ux, uz)."""
    ops = []
    for k in range(1, LANES + 1):
        zc = r_in(k) + (LANE - W) / 2
        if (ux, uz) == (1, 0):           # read from outside the track (south)
            ops += vl.numeral(str(k), x_left, zc - h / 2, h, 1, 0, "LINE")
        else:                            # read by a runner at the start, facing east
            gw = 4.0 / 7.0 * h
            ops += vl.numeral(str(k), x_left + h, zc - gw / 2, h, 0, 1, "LINE")
    return ops


def markings_ops():
    ops = []
    # the finish line, across the eight lanes (and the lane lines) at the end of the straight
    ops.append(R(EX - W, R0, EX, ROUT, "LINE"))
    # the 100 m and 110 m hurdles starts, straight across the chute
    for dist in (100, 110):
        x = EX - dist
        ops.append(R(x - W, R0, x, ROUT, "LINE"))
    for k in range(1, LANES + 1):
        dk = r_meas(k) - r_meas(1)
        s400 = 2 * math.pi * dk
        s800 = math.pi * dk + (math.sqrt((2 * HALF) ** 2 + dk ** 2) - 2 * HALF)
        # 200 m: lane 1 at the start of bend 2, the others one bend's stagger into it
        d200 = math.pi * r_meas(k) + 2 * HALF + math.pi * dk
        if k > 1:
            ops.append(across(k, s400, "LINE"))
            ops.append(across(k, s800, "GREEN"))
        ops.append(across(k, d200, "LINE"))
        # 4 x 100 m: takeover zones 30 m long, the scratch line 20 m in, at 100, 200, 300 m
        for leg in (100, 200, 300):
            scratch = s400 + leg
            ops.append(across(k, scratch - 20 + W, "YELLOW"))
            ops.append(across(k, scratch + 10, "YELLOW"))
        # 4 x 400 m first takeover: 10 m either side of the 400 m start
        ops.append(across(k, s400 - 10 + W, "BLUE"))
        ops.append(across(k, s400 + 10, "BLUE"))
        # hurdles: 110 m (13.72 + 9.14 n), 100 m (13.00 + 8.50 n), 400 m (45 + 35 n)
        for n in range(10):
            for d, mat in ((-110 + 13.72 + 9.14 * n, "BLUE"),
                           (-100 + 13.00 + 8.50 * n, "YELLOW"),
                           (s400 + 45 + 35 * n, "GREEN")):
                ops.append(tick(k, d, mat, 0))
                ops.append(tick(k, d, mat, 1))
    # the 800 m break line, across the eight lanes where bend 1 ends
    ops.append(R(EX, -ROUT, EX + W, -R0, "GREEN"))
    # 400 m lane 1 start and 800 m lane 1 start are the finish line itself
    ops += numbers(EX - 2.0 - 0.46, 1, 0)
    ops += numbers(EX - 100 + 1.0, 0, 1)
    return ops


def field_ops():
    ops = []
    # javelin: the runway in the west D, throwing east, the arc's crown at the D's edge
    jz = 0.0
    arc_c = (WX - 8.0, jz)
    ops.append(R(WX - 30.0, jz - 2.0, WX - 0.5, jz - 2.0 + W, "LINE"))
    ops.append(R(WX - 30.0, jz + 2.0 - W, WX - 0.5, jz + 2.0, "LINE"))
    ops.append(vl.Op("WHITE", [vl.Disc(arc_c[0], arc_c[1], 8.0),
                               vl.rect(WX - 1.0, jz - 2.0, WX + 0.1, jz + 2.0)],
                     [vl.Disc(arc_c[0], arc_c[1], 8.0 - 0.07)]))
    ops += sector(arc_c, 0.0, 28.96, 8.0, 85.0)
    # shot put: the circle in the east D, throwing west
    ops += circle((60.5, -13.5), 2.135 / 2, 180.0, 34.92, 25.0)
    # discus and hammer: the 2.50 m circle (the cage is a block), throwing west
    ops += circle((64.5, 10.5), 2.50 / 2, 180.0, 34.92, 80.0)
    return ops


def circle(c, r, heading, angle, length):
    """A throwing circle of inner radius r: a concrete floor, the 5 cm band round it and its
    sector lines, centred on ``heading`` (degrees: 0 east, 90 south)."""
    ops = [vl.Op("CONCRETE", [vl.Disc(c[0], c[1], r)]),
           vl.ring(c[0], c[1], r, r + W, "LINE")]
    ops += sector(c, heading, angle, r + W, length)
    # the 75 cm lines out from the circle's sides, where the sector's middle is measured from
    for side in (-1, 1):
        a = math.radians(heading + 90 * side)
        p = (c[0] + (r + W) * math.cos(a), c[1] + (r + W) * math.sin(a))
        q = (c[0] + (r + W + 0.75) * math.cos(a), c[1] + (r + W + 0.75) * math.sin(a))
        ops.append(vl.Op("LINE", [vl.strip(p, q, W)]))
    return ops


def sector(c, heading, angle, r0, r1):
    ops = []
    for side in (-1, 1):
        a = math.radians(heading + side * angle / 2)
        p = (c[0] + r0 * math.cos(a), c[1] + r0 * math.sin(a))
        q = (c[0] + r1 * math.cos(a), c[1] + r1 * math.sin(a))
        ops.append(vl.Op("LINE", [vl.strip(p, q, W)]))
    return ops


def track():
    ops = surface_ops() + kerb_ops() + lane_line_ops() + markings_ops() + field_ops()
    return vl.Layout("track_400m", "GRASS", ops, cl.extent_of(ops), (cl.S_RED, cl.S_GREEN, V_WHITE, 6))


def jumps():
    z = 0.5
    ops = [R(0.0, z - 0.66, 45.0, z + 0.66, "SURFACE"),
           R(0.0, z - 0.66, 45.0, z - 0.61, "LINE"), R(0.0, z + 0.61, 45.0, z + 0.66, "LINE")]
    for take_off in (43.0, 34.0, 32.0):
        ops.append(R(take_off - 0.20, z - 0.61, take_off, z + 0.61, "WHITE"))
    ops.append(R(45.0, z - 1.375 - 0.15, 54.15, z + 1.375 + 0.15, "CONCRETE"))
    ops.append(R(45.0, z - 1.375, 54.0, z + 1.375, "SAND"))
    return vl.Layout("long_jump", "GRASS", ops, cl.extent_of(ops), (cl.S_RED, cl.S_GREEN, V_WHITE, 6))


def pole_vault():
    z = 0.5
    ops = [R(0.0, z - 0.66, 46.0, z + 0.66, "SURFACE"),
           R(46.0, z - 4.0, 53.0, z + 4.0, "SURFACE"),
           R(0.0, z - 0.66, 45.0, z - 0.61, "LINE"), R(0.0, z + 0.61, 45.0, z + 0.66, "LINE"),
           vl.Op("STEEL", [vl.polygon([(45.0, z - 0.30), (46.0, z - 0.075), (46.0, z + 0.075),
                                       (45.0, z + 0.30)])]),
           R(46.0, z - 3.0, 46.05, z - 0.075, "WHITE"), R(46.0, z + 0.075, 46.05, z + 3.0, "WHITE")]
    return vl.Layout("pole_vault", "GRASS", ops, cl.extent_of(ops), (cl.S_RED, cl.S_GREEN, V_WHITE, 6))


def layouts():
    """Every track layout, by id (an anchor's ``lay``): append only."""
    return [track(), jumps(), pole_vault()]


PREVIEW = dict(cl.PREVIEW)
PREVIEW["SURFACE"] = (176, 72, 52)

if __name__ == "__main__":
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        os.makedirs(out, exist_ok=True)
        for lay in layouts():
            ppm = 6 if lay.name == "track_400m" else 16
            p = vl.preview(lay, os.path.join(out, lay.name + ".png"), ppm=ppm, colours=PREVIEW)
            print(p, lay.extent, len(lay.ops), "ops", len(lay.pictures()), "pictures",
                  len(lay.anchors()), "anchors")
        if "--zoom" in sys.argv:
            t = track()
            for name, reg in (("finish", (35, 34, 52, 48)), ("bend1_stagger", (60, -10, 90, 48)),
                              ("chute", (-75, 33, -40, 48)), ("shot", (55, -20, 70, -8))):
                vl.preview(t, os.path.join(out, "z_" + name + ".png"), ppm=32, colours=PREVIEW,
                           region=reg)
