"""
court_layouts.py -- the courts of the Sports & Venues Courts, Track & Pool tab, as venues_lines
layouts, one block a metre, every line where it really falls.

Not run on its own (gen_venues_courts.py imports it); ``py -3 court_layouts.py --preview DIR``
writes a picture of each layout, eight pixels a metre, for checking the geometry offline.

Every court's length runs north-south (along z) in its own frame, its net (or centre line)
along x. Lines are measured to their outer edges, as the rules measure courts:

  * tennis (ITF): doubles 23.77 x 10.97 m, singles 8.23 m wide, service lines 6.40 m from the
    net, the centre service line and centre marks; baselines 10 cm, every other line 5 cm. The
    centre of the court is the middle of cell (0, 0), so the net runs along the middle of row 0;
  * pickleball: 13.41 x 6.10 m (44 x 20 ft), the non-volley line 2.13 m (7 ft) from the net, centre
    lines from it to the baselines, 5 cm lines; centred as tennis;
  * tennis with a pickleball court over it, its lines in the second line colour, the tennis lines
    painted over them;
  * basketball (FIBA): 28 x 15 m, the key 4.9 m wide and 5.8 m deep, the restricted area arc
    (1.25 m), the three-point line (6.75 m, 0.9 m from the sidelines), the free throw circle
    (1.8 m, dashed inside the key) and the centre circle (1.8 m); the key and circle in the
    second surface colour; full and half court;
  * volleyball (FIVB indoor): 18 x 9 m, the centre line under the net, attack lines 3 m from it
    and their dashed extensions beyond the sidelines; free zone in the second surface colour;
  * sand volleyball (FIVB beach): 16 x 8 m on sand, the boundary as tapes in the line colour, no
    centre line. Its centre is a cell's west edge across (the posts then sit in cell middles
    0.5 m outside the sidelines) and a cell's middle along.

Colours default per layout (surface palette ``cc``/``sc``, venue colours ``lc``/``oc``).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import venues_lines as vl  # noqa: E402

W = 0.05

# surface colours (SurfaceColours.java)
S_GREEN, S_DARK_GREEN, S_BLUE, S_LIGHT_BLUE, S_RED, S_TERRACOTTA, S_PURPLE, S_GREY = range(8)
S_TAN, S_NAVY, S_ORANGE = 8, 9, 10
# venue colours (VenueColours.java)
V_ROYAL, V_GOLD, V_WHITE = 1, 6, 14


def R(x0, z0, x1, z1, mat):
    return vl.Op(mat, [vl.rect(x0, z0, x1, z1)])


def tennis_lines(cx, cz, mat):
    hd, hs, hl, sv = 5.485, 4.115, 11.885, 6.40
    ops = []
    for s in (-1, 1):
        ops.append(R(cx + s * hd, cz - hl, cx + s * (hd - W), cz + hl, mat))     # doubles side
        ops.append(R(cx + s * hs, cz - hl, cx + s * (hs - W), cz + hl, mat))     # singles side
        ops.append(R(cx - hd, cz + s * hl, cx + hd, cz + s * (hl - 0.10), mat))  # baseline
        ops.append(R(cx - hs, cz + s * sv, cx + hs, cz + s * (sv - W), mat))     # service line
        ops.append(R(cx - W / 2, cz + s * (hl - 0.10), cx + W / 2, cz + s * (hl - 0.20), mat))
    ops.append(R(cx - W / 2, cz - sv, cx + W / 2, cz + sv, mat))                 # centre service
    return ops


def pickleball_lines(cx, cz, mat):
    hw, hl, nv = 3.05, 6.705, 2.13
    ops = []
    for s in (-1, 1):
        ops.append(R(cx + s * hw, cz - hl, cx + s * (hw - W), cz + hl, mat))
        ops.append(R(cx - hw, cz + s * hl, cx + hw, cz + s * (hl - W), mat))
        ops.append(R(cx - hw, cz + s * nv, cx + hw, cz + s * (nv - W), mat))
        ops.append(R(cx - W / 2, cz + s * nv, cx + W / 2, cz + s * hl, mat))
    return ops


def tennis(overlay=False):
    cx, cz = 0.5, 0.5
    ops = [R(cx - 5.485, cz - 11.885, cx + 5.485, cz + 11.885, "SURFACE")]
    if overlay:
        ops += pickleball_lines(cx, cz, "OVERLAY")
    ops += tennis_lines(cx, cz, "LINE")
    name = "tennis_pickleball" if overlay else "tennis"
    return vl.Layout(name, "SURROUND", ops, extent_of(ops),
                     (S_BLUE, S_GREEN, V_WHITE, V_GOLD))


def pickleball():
    cx, cz = 0.5, 0.5
    ops = [R(cx - 3.05, cz - 6.705, cx + 3.05, cz + 6.705, "SURFACE")]
    ops += pickleball_lines(cx, cz, "LINE")
    return vl.Layout("pickleball", "SURROUND", ops, extent_of(ops),
                     (S_BLUE, S_GREEN, V_WHITE, V_GOLD))


def _basket_end(cx, cz, s):
    """One end of a FIBA court: s = -1 the north end (baseline at cz - 14), +1 the south."""
    ops = []
    inner = cz + s * (14 - W)                 # the baseline's court-side edge
    hoop = inner - s * 1.575                  # the basket's centre
    ft = inner - s * 5.8                      # the free throw line's far (court) edge
    lo, hi = sorted((inner, ft))
    ops.append(R(cx - 2.45, lo, cx + 2.45, hi, "SURROUND"))        # the key, painted
    # the free throw circle's outer half filled too, so the key reads as one shape
    ops.append(vl.Op("SURROUND", [vl.Disc(cx, ft, 1.8), _beyond(ft, s)]))
    for sx in (-1, 1):
        ops.append(R(cx + sx * 2.45, lo, cx + sx * (2.45 - W), hi, "LINE"))
    ops.append(R(cx - 2.45, ft, cx + 2.45, ft + s * W, "LINE"))
    # the free throw circle: solid beyond the line, six dashes inside the key
    ops.append(vl.ring(cx, ft, 1.8 - W, 1.8, "LINE", _beyond(ft, s)))
    for k in range(6):
        import math
        a0 = math.radians(180.0 * (k * 2 + 0.5) / 12.0)
        a1 = math.radians(180.0 * (k * 2 + 1.5) / 12.0)
        p = [(cx - 1.9 * math.cos(a0), ft + s * 1.9 * math.sin(a0)),
             (cx - 1.9 * math.cos(a1), ft + s * 1.9 * math.sin(a1)), (cx, ft)]
        ops.append(vl.ring(cx, ft, 1.8 - W, 1.8, "LINE", vl.polygon(p)))
    # the restricted area: a 1.25 m semicircle (inner edge) and 0.375 m straights to the hoop line
    ops.append(vl.ring(cx, hoop, 1.25, 1.25 + W, "LINE", _beyond(hoop, s)))
    for sx in (-1, 1):
        x0 = cx + sx * 1.25
        ops.append(R(x0, hoop, x0 + sx * W, hoop + s * 0.375, "LINE"))
    # the three-point line: straights 0.9 m in from the sidelines, then the 6.75 m arc
    xo = 7.5 - W - 0.9
    import math
    zj = math.sqrt(6.75 ** 2 - xo ** 2)
    for sx in (-1, 1):
        ops.append(R(cx + sx * xo, inner, cx + sx * (xo - W), hoop - s * zj, "LINE"))
    slab = vl.Convex([vl.HP(-1, 0, -(cx - xo)), vl.HP(1, 0, cx + xo)])
    ops.append(vl.ring(cx, hoop, 6.75 - W, 6.75, "LINE", slab, _beyond(hoop, s)))
    return ops


def _beyond(z, s):
    """The half-plane past z in the direction -s along z (towards the court's middle for an
    end with sign s)."""
    return vl.half(0, z, 0, -s)


def basketball(half=False):
    cx, cz = 0.5, 0.5
    z0 = cz - 14
    z1 = cz if half else cz + 14
    ops = [R(cx - 7.5, z0, cx + 7.5, z1 + (0 if not half else 1.8), "SURFACE")]
    ops.append(vl.Op("SURROUND", [vl.Disc(cx, cz, 1.8 - W)] + ([vl.half(0, cz, 0, -1)] if half else [])))
    ops += _basket_end(cx, cz, -1)
    if not half:
        ops += _basket_end(cx, cz, 1)
    for sx in (-1, 1):
        ops.append(R(cx + sx * 7.5, z0, cx + sx * (7.5 - W), z1, "LINE"))
    ops.append(R(cx - 7.5, z0, cx + 7.5, z0 + W, "LINE"))
    if not half:
        ops.append(R(cx - 7.5, z1, cx + 7.5, z1 - W, "LINE"))
        ops.append(R(cx - 7.5, cz - W / 2, cx + 7.5, cz + W / 2, "LINE"))
        ops.append(vl.ring(cx, cz, 1.8 - W, 1.8, "LINE"))
    else:
        ops.append(R(cx - 7.5, cz - W, cx + 7.5, cz, "LINE"))
        ops.append(vl.ring(cx, cz, 1.8 - W, 1.8, "LINE", vl.half(0, cz, 0, -1)))
    return vl.Layout("basketball_half" if half else "basketball", "SURROUND", ops,
                     extent_of(ops), (S_BLUE, S_GREEN, V_WHITE, V_GOLD))


def volleyball():
    cx, cz = 0.5, 0.5
    ops = [R(cx - 4.5, cz - 9, cx + 4.5, cz + 9, "SURFACE")]
    for s in (-1, 1):
        ops.append(R(cx + s * 4.5, cz - 9, cx + s * (4.5 - W), cz + 9, "LINE"))
        ops.append(R(cx - 4.5, cz + s * 9, cx + 4.5, cz + s * (9 - W), "LINE"))
        ops.append(R(cx - 4.5, cz + s * 3, cx + 4.5, cz + s * (3 - W), "LINE"))
        for sx in (-1, 1):
            for k in range(5):
                a = 4.5 + 0.20 + k * 0.35
                ops.append(R(cx + sx * a, cz + s * 3, cx + sx * (a + 0.15), cz + s * (3 - W),
                             "LINE"))
    ops.append(R(cx - 4.5, cz - W / 2, cx + 4.5, cz + W / 2, "LINE"))
    return vl.Layout("volleyball", "SURROUND", ops, extent_of(ops, 1),
                     (S_ORANGE, S_BLUE, V_WHITE, V_GOLD))


def sand_volleyball():
    cx, cz = 0.0, 0.5
    ops = []
    for s in (-1, 1):
        ops.append(R(cx + s * 4, cz - 8, cx + s * (4 - W), cz + 8, "TAPE"))
        ops.append(R(cx - 4, cz + s * 8, cx + 4, cz + s * (8 - W), "TAPE"))
    return vl.Layout("sand_volleyball", "SAND", ops, extent_of(ops),
                     (S_TAN, S_TAN, V_ROYAL, V_GOLD))


def extent_of(ops, margin=0):
    import math
    x0 = min(o.bbox[0] for o in ops)
    z0 = min(o.bbox[1] for o in ops)
    x1 = max(o.bbox[2] for o in ops)
    z1 = max(o.bbox[3] for o in ops)
    return (int(math.floor(x0)) - margin, int(math.floor(z0)) - margin,
            int(math.ceil(x1)) - 1 + margin, int(math.ceil(z1)) - 1 + margin)


def layouts():
    """Every court layout, in the order of their ids (an anchor's ``lay``): append only."""
    return [tennis(), pickleball(), tennis(True), basketball(), basketball(True), volleyball(),
            sand_volleyball()]


# Each layout's run-off: the plain cells round it --layout lays as well (metres from the
# court's outline: behind its ends, beside its sides), and its centre in its frame.
RUNOFF = {"tennis": (6.40, 3.66), "pickleball": (3.05, 1.52), "tennis_pickleball": (6.40, 3.66),
          "basketball": (2.0, 2.0), "basketball_half": (2.0, 2.0), "volleyball": (3.0, 3.0),
          "sand_volleyball": (3.0, 3.0)}

PREVIEW = {"SURFACE": (52, 98, 160), "SURROUND": (60, 116, 72), "LINE": (245, 245, 240),
           "OVERLAY": (240, 180, 30), "SAND": (214, 196, 150), "TAPE": (32, 84, 196),
           "GRASS": (96, 150, 64), "KERB": (200, 200, 205), "CONCRETE": (170, 168, 160),
           "STEEL": (120, 124, 130), "WHITE": (250, 250, 250), "YELLOW": (242, 194, 48),
           "BLUE": (46, 108, 200), "GREEN": (46, 154, 76), "RED": (208, 48, 42),
           "BLACK": (30, 30, 30)}

if __name__ == "__main__":
    if "--preview" in sys.argv:
        out = sys.argv[sys.argv.index("--preview") + 1]
        os.makedirs(out, exist_ok=True)
        for lay in layouts():
            p = vl.preview(lay, os.path.join(out, lay.name + ".png"), ppm=24, colours=PREVIEW)
            print(p, lay.extent, len(lay.pictures()), "pictures", len(lay.anchors()), "anchors")
