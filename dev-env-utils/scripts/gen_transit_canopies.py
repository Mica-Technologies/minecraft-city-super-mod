#!/usr/bin/env python3
"""Every asset the Transit tab's bus station canopy kit ships: the canopy decks in four styles
and the four agencies' liveries, the round canopy columns, the soffit downlight and the Canopy
Tool's icon.

    python dev-env-utils/scripts/gen_transit_canopies.py
    python dev-env-utils/scripts/gen_transit_canopies.py --check
    python dev-env-utils/scripts/gen_transit_canopies.py --fragments   # tab lines to paste

A canopy is built of deck tiles, one block each, set at the clearance wanted over columns. The
deck lies at the foot of its block (its underside is the clearance line), as the platform canopy's
does, so a deck set on a column rests on it. Tiles of the same block join on all four world sides
(actual state `north` .. `west`), and the fascia, with the agency's name, is drawn only round the
outside. Styles (CanopyStyle.java):

  flat       a flat steel deck with a white soffit
  glass      a steel grid with glass between the beams, translucent
  sawtooth   north-light ridges every 3 blocks: a slope rising across three tiles, then a glazed
             upstand back down
  butterfly  V roofs every 4 blocks: two slopes down to a valley gutter and two back up
  vault      shallow barrel arches every 6 blocks, rising about 1.6 blocks

The shaped styles change across their module, so a tile stores its `phase` (its place in the
module) and `axis` (which way the profile runs); the Canopy Tool sets both, and a tile placed by
hand takes them from the tile it is set beside. Each shaped model is drawn with its profile
running along +x and turned a quarter for axis z; the joins it is drawn against are turned with
it. Slopes are elements turned 22.5 or 45 degrees about the run with `rescale`, so a slope tile
meets the next one's height exactly.
"""
import math
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_canopies.py", "transit/canopies", "transit/canopies",
                 assets=ASSETS)
TAB = "CsmTabTransit"

WHITE = (240, 242, 240)
GRAPHITE = (56, 58, 62)

# The shelters' four agencies (gen_transit_shelters.py): id, name, frame, fascia ground, text,
# stripe colour, stripe place
AGENCIES = [
    ("cityline", "CITYLINE", (14, 94, 111), (14, 94, 111), (244, 196, 40), (244, 196, 40),
     "bottom"),
    ("riverway", "RIVERWAY", (31, 58, 115), (31, 58, 115), WHITE, (240, 138, 36), "left"),
    ("verdant", "VERDANT", (36, 116, 60), (36, 116, 60), WHITE, (236, 240, 232), "lines"),
    ("emberline", "EMBERLINE", GRAPHITE, (176, 36, 32), WHITE, GRAPHITE, "bottom"),
]
AGENCY_IDS = [a[0] for a in AGENCIES]
BASE = "cityline"

# style, Java constant, module (tiles in one profile; 1 for a flat style), names
STYLES = [
    ("flat", "FLAT", 1, ("Bus Station Canopy (Flat, %s)", "Busbahnhofdach (Flach, %s)",
                         "Marquesina de Estación (Plana, %s)", "Busstationstak (Platt, %s)")),
    ("glass", "GLASS", 1, ("Bus Station Canopy (Glass Skylight, %s)",
                           "Busbahnhofdach (Glasoberlicht, %s)",
                           "Marquesina de Estación (Lucernario, %s)",
                           "Busstationstak (Glasöverljus, %s)")),
    ("sawtooth", "SAWTOOTH", 3, ("Bus Station Canopy (Sawtooth, %s)",
                                 "Busbahnhofdach (Sheddach, %s)",
                                 "Marquesina de Estación (Diente de Sierra, %s)",
                                 "Busstationstak (Sågtak, %s)")),
    ("butterfly", "BUTTERFLY", 4, ("Bus Station Canopy (Butterfly, %s)",
                                   "Busbahnhofdach (Schmetterlingsdach, %s)",
                                   "Marquesina de Estación (Mariposa, %s)",
                                   "Busstationstak (Fjärilstak, %s)")),
    ("vault", "VAULT", 6, ("Bus Station Canopy (Barrel Vault, %s)",
                           "Busbahnhofdach (Tonnendach, %s)",
                           "Marquesina de Estación (Bóveda de Cañón, %s)",
                           "Busstationstak (Tunnvalv, %s)")),
]

FINISHES = [
    ("steel", (120, 126, 130), ("Canopy Column (Steel)", "Dachstütze (Stahl)",
                                "Columna de Marquesina (Acero)", "Takpelare (Stål)")),
    ("white", (226, 228, 226), ("Canopy Column (White)", "Dachstütze (Weiß)",
                                "Columna de Marquesina (Blanca)", "Takpelare (Vit)")),
    ("graphite", (58, 60, 64), ("Canopy Column (Graphite)", "Dachstütze (Graphit)",
                                "Columna de Marquesina (Grafito)", "Takpelare (Grafit)")),
]

LIVERY_KEYS = ("frame", "fascia")

# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
FASCIA_PX = 8   # texels a unit: 128 px across a block
FASCIA_H = 8    # units


def fascia_art(agency):
    """The fascia: the agency's name centred on a band 16 x FASCIA_H units, with its stripe."""
    _, name, _, ground, text, stripe, place = agency
    scale = 3   # the name repeats every block; at 4 the longest names run edge to edge
    img = lc.fill(ground, 128, 2, 140 + AGENCY_IDS.index(agency[0]))
    h = FASCIA_H * FASCIA_PX
    th = 5 * scale
    ty = (h - th) // 2
    if place == "bottom":
        lc.rect(img, 0, h - 8, 128, h - 2, stripe)
        ty = (h - 8 - th) // 2
    elif place == "left":
        for x0 in (0, 128 - 14):
            lc.rect(img, x0, 0, x0 + 14, h, stripe)
    else:
        lc.rect(img, 0, 4, 128, 7, stripe)
        lc.rect(img, 0, h - 7, 128, h - 4, stripe)
    lc.draw_text_centred(img, name, 64, ty, text, scale)
    return img


def soffit():
    """White powder-coated soffit panels with their seams."""
    img = lc.fill((226, 228, 226), 16, 2, 150)
    lc.rect(img, 0, 0, 16, 1, (196, 198, 198))
    lc.rect(img, 0, 8, 16, 9, (204, 206, 206))
    return img


def glass():
    img = lc.fill((206, 228, 240), 16, 2, 151, alpha=78)
    px = img.load()
    for y in range(16):
        x = (y * 3 // 4 + 5) % 16
        r, g, b, a = px[x, y]
        px[x, y] = (min(255, r + 22), min(255, g + 22), min(255, b + 14), 110)
    return img


def lens(lit):
    img = lc.fill((255, 250, 230) if lit else (190, 192, 190), 16, 2, 152 + lit)
    lc.frame(img, 0, 0, 16, 16, (180, 182, 182))
    return img


def tool_icon():
    """The Canopy Tool: a deck over two columns, with a set square."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    teal = (14, 94, 111, 255)
    lc.rect(img, 1, 3, 15, 5, teal)
    lc.rect(img, 1, 5, 15, 6, (244, 196, 40, 255))
    for x in (3, 11):
        lc.rect(img, x, 6, x + 2, 14, (150, 156, 160, 255))
    lc.rect(img, 1, 14, 15, 15, (90, 94, 98, 255))
    lc.rect(img, 7, 8, 8, 13, (230, 230, 230, 255))
    lc.rect(img, 7, 12, 10, 13, (230, 230, 230, 255))
    return img


def register_textures():
    tex = {
        "roof": lambda: lc.fill((158, 164, 162), 16, 5, 160),
        "soffit": soffit,
        "glass": glass,
        "steel": lambda: lc.fill((84, 88, 92), 16, 3, 161),
        "gutter": lambda: lc.fill((128, 132, 134), 16, 3, 162),
        "lens_on": lambda: lens(True),
        "lens_off": lambda: lens(False),
        "canopy_tool": tool_icon,
    }
    for i, a in enumerate(AGENCIES):
        tex["frame_" + a[0]] = (lambda a=a, i=i: lc.fill(a[2], 16, 4, 170 + i))
        tex["fascia_" + a[0]] = (lambda a=a: fascia_art(a))
    for i, (fid, col, _) in enumerate(FINISHES):
        tex["column_" + fid] = (lambda col=col, i=i: lc.fill(col, 16, 3, 180 + i))
    for name, draw in tex.items():
        C.texture(name)(draw)


# ------------------------------------------------------------------------------------------
# Elements
# ------------------------------------------------------------------------------------------
ALL = ("north", "south", "east", "west", "up", "down")


def _span(a, b):
    shift = 16 * (min(a, b) // 16)
    if max(a, b) - shift > 16:
        return 0.0, min(16.0, abs(b - a))
    return a - shift, b - shift


def _uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    vy = _span(16 - y1, 16 - y0)
    if face == "north":
        u = _span(16 - x1, 16 - x0)
    elif face == "south":
        u = _span(x0, x1)
    elif face == "east":
        u = _span(16 - z1, 16 - z0)
    elif face == "west":
        u = _span(z0, z1)
    elif face == "up":
        u, vy = _span(x0, x1), _span(z0, z1)
    else:
        u, vy = _span(x0, x1), _span(16 - z1, 16 - z0)
    uv = [u[0], vy[0], u[1], vy[1]]
    if uv[0] == uv[2]:
        uv[2] = uv[0] + 0.01
    if uv[1] == uv[3]:
        uv[3] = uv[1] + 0.01
    return [round(v, 4) for v in uv]


def el(frm, to, tex, faces=ALL, per=None, uv=None, rot=None):
    faces_out = {}
    for name in faces:
        u = (uv or {}).get(name) or _uv(name, frm, to)
        faces_out[name] = {"texture": "#" + (per or {}).get(name, tex),
                           "uv": [round(v, 4) for v in u]}
    out = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to],
           "faces": faces_out}
    if rot:
        out["rotation"] = rot
    return out


DECK_T = 3.0          # deck thickness
FASCIA_LO = -4.0      # the fascia hangs this far below the deck's underside
FASCIA_UV = [0, 0, 16, FASCIA_H]


def deck_slab(x0, x1, y0=0.0, y1=DECK_T, z0=0.0, z1=16.0):
    return el((x0, y0, z0), (x1, y1, z1), "frame", ALL, per={"up": "roof", "down": "soffit"})


def fascia_north(x0=0.0, x1=16.0, y0=FASCIA_LO, y1=FASCIA_LO + FASCIA_H, z0=-0.5):
    """A fascia band along the north edge, its name facing out."""
    uv = [16 - x1, 0, 16 - x0, FASCIA_H]
    return el((x0, y0, z0), (x1, y1, z0 + 0.5), "frame", ALL, per={"north": "fascia"},
              uv={"north": uv})


def corner_nw(y0=FASCIA_LO, y1=FASCIA_LO + FASCIA_H):
    return el((-0.5, y0, -0.5), (0, y1, 0), "frame")


# --- flat and glass ---------------------------------------------------------------------------
def flat_parts():
    """(name, model elements, kind): kind 'centre', 'edge' (drawn on the north side), 'corner'
    (drawn at the north-west), or for the shaped styles a phase rule."""
    return [("deck", [deck_slab(0, 16)], "centre"),
            ("edge", [fascia_north()], "edge"),
            ("corner", [corner_nw()], "corner")]


def glass_parts():
    beams = [el((0, 0, 7.25), (16, DECK_T, 8.75), "steel"),
             el((7.25, 0, 0), (8.75, DECK_T, 7.25), "steel"),
             el((7.25, 0, 8.75), (8.75, DECK_T, 16), "steel"),
             el((0, 1.4, 0), (16, 1.9, 16), "glass", ("up", "down"))]
    edge = [fascia_north(), el((0, 0, 0), (16, DECK_T, 1.25), "steel")]
    return [("deck", beams, "centre"), ("edge", edge, "edge"), ("corner", [corner_nw()], "corner")]


# --- the shaped decks -------------------------------------------------------------------------
R22 = 16 * math.tan(math.radians(22.5))      # rise of a tile turned 22.5 degrees
H22 = 8 * math.tan(math.radians(22.5))       # and of half a tile
SLAB = 2.5                                   # the thickness of a sloped deck
# A block model's positive angle about z raises the +x end (right-handed about +z), so a slope
# rising toward +x is turned by a positive angle. Checked in game from a side elevation: from
# below, the perspective makes a correct vault read as a bowl.
TURN_SIGN = 1


def slope(x0, x1, ya, yb, angle):
    """A sloped deck piece from (x0, ya) to (x1, yb) (underside heights), turned about the
    run. rescale stretches it across the turned length; the angle must be 0, 22.5 or 45."""
    xc = (x0 + x1) / 2
    yc = (ya + yb) / 2
    if angle == 0:
        return deck_slab(x0, x1, ya, ya + SLAB)
    return el((x0, yc, 0), (x1, yc + SLAB, 16), "frame", ALL,
              per={"up": "roof", "down": "soffit"},
              rot={"origin": [xc, yc, 8], "axis": "z", "angle": TURN_SIGN * angle,
                   "rescale": True})


def end_rib(x0, x1, ya, yb, angle):
    """The gable end of a sloped piece on the north edge: a rib following the slope, with the
    fascia band under it."""
    xc = (x0 + x1) / 2
    yc = (ya + yb) / 2
    if angle == 0:
        return [el((x0, ya - 1, -0.5), (x1, ya + SLAB + 0.5, 0), "frame")]
    return [el((x0, yc - 1, -0.5), (x1, yc + SLAB + 0.5, 0), "frame", ALL,
               rot={"origin": [xc, yc, -0.25], "axis": "z", "angle": TURN_SIGN * angle,
                    "rescale": True})]


def eave(x, edge_y, side):
    """The fascia across an outer edge of the profile (west at x = 0 or east at x = 16): the
    standard band, hung from the deck's edge at that height."""
    x0, x1 = (-0.5, 0) if side == "west" else (16, 16.5)
    return [el((x0, edge_y + FASCIA_LO, 0), (x1, edge_y + FASCIA_LO + FASCIA_H, 16), "frame",
               ALL, per={side: "fascia"}, uv={side: [0, 0, 16, FASCIA_H]})]


def gutter(x0, x1, y):
    return el((x0, y - 1.5, 0), (x1, y + 0.5, 16), "gutter",
              ("north", "south", "east", "west", "down"))


def profile(style):
    """The profile tile by tile: for each phase, the list of (x0, x1, ya, yb, angle) segments
    in that tile's own x, underside heights."""
    if style == "sawtooth":
        return [[(0, 16, p * R22, (p + 1) * R22, 22.5)] for p in range(3)]
    if style == "butterfly":
        hs = [2 * R22, R22, 0, R22, 2 * R22]
        out = []
        for p in range(4):
            a = -22.5 if p < 2 else 22.5
            out.append([(0, 16, hs[p], hs[p + 1], a)])
        return out
    # vault: twelve half tiles
    angles = [45, 45, 22.5, 22.5, 22.5, 0, 0, -22.5, -22.5, -22.5, -45, -45]
    ys = [0.0]
    for a in angles:
        ys.append(ys[-1] + {45: 8.0, 22.5: H22, 0: 0.0, -22.5: -H22, -45: -8.0}[a])
    out = []
    for p in range(6):
        segs = []
        for k in (0, 1):
            i = 2 * p + k
            segs.append((8 * k, 8 * k + 8, ys[i], ys[i + 1], angles[i]))
        out.append(segs)
    return out


def shaped_parts(style):
    """Per phase: the deck, its north end rib (drawn when the run stops), and the pieces at the
    module's edges: gutters, the sawtooth's glazed upstand, the eaves fascia when the canopy
    stops there."""
    prof = profile(style)
    module = len(prof)
    parts = []
    for p, segs in enumerate(prof):
        deck = [slope(*s) for s in segs]
        rib = []
        for s in segs:
            rib += end_rib(*s)
        parts.append(("deck_%d" % p, deck, ("phase", p)))
        parts.append(("end_%d" % p, rib, ("end", p)))
        lo_y, hi_y = segs[0][2], segs[-1][3]
        # the module's first tile: its west edge
        if p == 0:
            west = []
            if style == "sawtooth":
                west.append(gutter(0, 1.5, 0))
            elif style == "vault":
                west.append(gutter(0, 1.5, 0))
            parts.append(("join_west_%d" % p, west, ("west_joined", p)))
            parts.append(("eave_west_%d" % p, eave(0, lo_y, "west"), ("west_open", p)))
        if p == module - 1:
            east = []
            if style == "sawtooth":
                # the glazed upstand from the next module's eave up to this ridge
                east += [el((15.25, 0, 0), (16, hi_y + SLAB, 16), "glass", ("east", "west")),
                         el((15, 0, 0), (16, 1, 16), "steel"),
                         el((15, hi_y, 0), (16, hi_y + SLAB, 16), "frame")]
            if style == "vault":
                east.append(gutter(14.5, 16, 0))
            parts.append(("join_east_%d" % p, east, ("east_joined", p)))
            cap = eave(16, hi_y, "east")
            if style == "sawtooth":
                cap = [el((15.25, 0, 0), (16, hi_y + SLAB, 16), "glass", ("east", "west"))] + cap
            parts.append(("eave_east_%d" % p, cap, ("east_open", p)))
        if style == "butterfly" and p == 1:
            parts.append(("valley", [gutter(14.5, 16, 0)], ("phase", p)))
        if style == "butterfly" and p == 2:
            parts.append(("valley_b", [gutter(0, 1.5, 0)], ("phase", p)))
    return parts


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
SIDE_ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
# a model drawn for axis x, turned a quarter for axis z: where each of its sides lands
TURN = {"north": "east", "east": "south", "south": "west", "west": "north"}
CORNERS = (("north", "west", 0), ("east", "north", 90), ("south", "east", 180),
           ("west", "south", 270))


def keys_of(els):
    keys = set()
    for e in els:
        for f in e["faces"].values():
            keys.add(f["texture"][1:])
    return keys


def textures(keys, agency):
    t = {k: C.T(k + "_" + agency) if k in LIVERY_KEYS else C.T(k) for k in sorted(keys)}
    t["particle"] = C.T("frame_" + agency)
    return t


def livery_textures(keys, agency):
    t = {k: C.T(k + "_" + agency) for k in sorted(keys) if k in LIVERY_KEYS}
    t["particle"] = C.T("frame_" + agency)
    return t


def deck_rules(style, module, names):
    """(model name, when, y) for every part of a style."""
    rules = []
    if module == 1:
        for name, els, kind in names:
            if kind == "centre":
                rules.append((name, {}, 0))
            elif kind == "edge":
                for side, y in SIDE_ROT.items():
                    rules.append((name, {side: "false"}, y))
            else:
                for a, b, y in CORNERS:
                    rules.append((name, {a: "false", b: "false"}, y))
        return rules
    for name, els, (kind, p) in names:
        for axis, turn in (("x", 0), ("z", 90)):
            def w(side):
                return TURN[side] if turn else side
            base = {"axis": axis, "phase": str(p)}
            if kind == "phase":
                rules.append((name, base, turn))
            elif kind == "end":
                for side, extra in (("north", 0), ("south", 180)):
                    when = dict(base, **{w(side): "false"})
                    rules.append((name, when, (turn + extra) % 360, side))
            elif kind == "west_joined":
                rules.append((name, dict(base, **{w("west"): "true"}), turn))
            elif kind == "west_open":
                rules.append((name, dict(base, **{w("west"): "false"}), turn))
            elif kind == "east_joined":
                rules.append((name, dict(base, **{w("east"): "true"}), turn))
            elif kind == "east_open":
                rules.append((name, dict(base, **{w("east"): "false"}), turn))
    return rules


def mirror_x(els):
    """The south end rib: the north one turned a half about the tile's centre would also flip
    the profile, so it is drawn again mirrored front to back instead."""
    out = []
    for e in els:
        f, t = e["from"], e["to"]
        e2 = dict(e)
        e2["from"] = [f[0], f[1], 16 - t[2]]
        e2["to"] = [t[0], t[1], 16 - f[2]]
        if "rotation" in e:
            r = dict(e["rotation"])
            r["origin"] = [r["origin"][0], r["origin"][1], 16 - r["origin"][2]]
            e2["rotation"] = r
        faces = {}
        swap = {"north": "south", "south": "north"}
        for k, v in e["faces"].items():
            faces[swap.get(k, k)] = v
        e2["faces"] = faces
        out.append(e2)
    return out


def decks():
    for style, java_const, module, names in STYLES:
        parts = flat_parts() if style == "flat" else glass_parts() if style == "glass" \
            else shaped_parts(style)
        base_models = {}
        for name, els, kind in parts:
            if not els:
                continue
            base_models["%s/%s" % (style, name)] = lc.model(textures(keys_of(els), BASE), els)
            if module > 1 and kind[0] == "end":
                base_models["%s/%s_s" % (style, name)] = lc.model(
                    textures(keys_of(els), BASE), mirror_x(els))
        rules = deck_rules(style, module, [(n, e, k) for n, e, k in parts if e])
        for aid, aname, *_ in AGENCIES:
            reg = "canopy_deck_%s_%s" % (style, aid)
            models = dict(base_models) if aid == BASE else {}
            refs = {}
            for mname, data in base_models.items():
                keys = keys_of(data.get("elements", []))
                ref = mname
                if aid != BASE and keys & set(LIVERY_KEYS):
                    ref = "%s/%s/%s" % (style, aid, mname.split("/", 1)[1])
                    models[ref] = {"parent": "csm:block/transit/canopies/" + mname,
                                   "textures": livery_textures(keys, aid)}
                refs[mname] = ref
            mp = []
            for rule in rules:
                name, when, y = rule[0], rule[1], rule[2]
                mname = "%s/%s" % (style, name)
                if len(rule) > 3 and rule[3] == "south":
                    mname += "_s"
                    y = (y - 180) % 360
                apply = {"model": C.M(refs[mname])}
                if y:
                    apply["y"] = y
                entry = {"apply": apply}
                if when:
                    entry["when"] = when
                mp.append(entry)
            item_els = item_elements(style, parts, module)
            item = lc.model(textures(keys_of(item_els), aid), item_els)
            item["display"] = item_display()
            java = 'new BlockCanopyDeck("%s", CanopyStyle.%s)' % (reg, java_const)
            C.add(reg, java, tuple(n % aname for n in names), models, {"multipart": mp},
                  item=item, tab=TAB)


def item_elements(style, parts, module):
    """The item: a flat style's tile with its fascia round it; a shaped style's three tiles
    (the crown, for the vault) side by side, from x -16 to 32."""
    if module == 1:
        out = []
        for name, els, kind in parts:
            if kind == "centre":
                out += els
            elif kind == "edge":
                out += els
                out += [turn_y(e, 90) for e in els] + [turn_y(e, 180) for e in els] + \
                       [turn_y(e, 270) for e in els]
        return out
    first = {"vault": 1, "butterfly": 0, "sawtooth": 0}[style]
    out = []
    for i in range(3):
        p = first + i
        for name, els, kind in parts:
            if kind == ("phase", p):
                out += [shift_x(e, (i - 1) * 16) for e in els]
    return out


def shift_x(e, dx):
    e2 = dict(e)
    e2["from"] = [e["from"][0] + dx, e["from"][1], e["from"][2]]
    e2["to"] = [e["to"][0] + dx, e["to"][1], e["to"][2]]
    if "rotation" in e:
        r = dict(e["rotation"])
        r["origin"] = [r["origin"][0] + dx, r["origin"][1], r["origin"][2]]
        e2["rotation"] = r
    return e2


def turn_y(e, deg):
    """An unrotated element turned about the block's vertical centre line (90, 180, 270)."""
    def pt(x, z):
        for _ in range(deg // 90):
            x, z = 16 - z, x
        return x, z
    (ax, az), (bx, bz) = pt(e["from"][0], e["from"][2]), pt(e["to"][0], e["to"][2])
    e2 = dict(e)
    e2["from"] = [min(ax, bx), e["from"][1], min(az, bz)]
    e2["to"] = [max(ax, bx), e["to"][1], max(az, bz)]
    order = ["north", "east", "south", "west"]
    faces = {}
    for k, v in e["faces"].items():
        if k in order:
            k = order[(order.index(k) + deg // 90) % 4]
        faces[k] = v
    e2["faces"] = faces
    return e2


def item_display():
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [0.36] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.4] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.25] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.3] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.3] * 3},
    }


# ------------------------------------------------------------------------------------------
# Columns, the downlight, the tool
# ------------------------------------------------------------------------------------------
COL_R = 3.5   # a column's inradius: 7 px, about 0.45 m across


def columns():
    for fid, _, names in FINISHES:
        reg = "canopy_column_" + fid
        tex = {"c": C.T("column_" + fid), "particle": C.T("column_" + fid)}
        shaft = lc.post(8, 8, COL_R, 0, 16, "c", top=False, bottom=False)
        base = [el((3, 0, 3), (13, 1, 13), "c")] + lc.post(8, 8, COL_R + 1, 1, 3, "c")
        cap = lc.post(8, 8, COL_R + 1, 13, 15, "c") + [el((2.5, 15, 2.5), (13.5, 16, 13.5), "c")]
        models = {
            "column/%s_shaft" % fid: lc.model(tex, shaft),
            "column/%s_base" % fid: lc.model(tex, base),
            "column/%s_cap" % fid: lc.model(tex, cap),
        }
        mp = [{"apply": {"model": C.M("column/%s_shaft" % fid)}},
              {"when": {"base": "true"}, "apply": {"model": C.M("column/%s_base" % fid)}},
              {"when": {"cap": "true"}, "apply": {"model": C.M("column/%s_cap" % fid)}}]
        item = lc.model(tex, shaft + base + cap)
        C.add(reg, 'new BlockCanopyColumn("%s")' % reg, names, models, {"multipart": mp},
              item=item, tab=TAB)


def downlight():
    reg = "canopy_downlight"
    models = {}
    for lit in (False, True):
        tex = {"b": C.T("steel"), "l": C.T("lens_on" if lit else "lens_off"),
               "particle": C.T("steel")}
        els = [el((4.5, 15, 4.5), (11.5, 16, 11.5), "b", ("north", "south", "east", "west",
                                                          "down")),
               el((5.5, 14.75, 5.5), (10.5, 15, 10.5), "l",
                  ("north", "south", "east", "west", "down"))]
        models["downlight_%s" % ("on" if lit else "off")] = lc.model(tex, els, ao=False)
    bs = {"multipart": [{"when": {"lit": "false"}, "apply": {"model": C.M("downlight_off")}},
                        {"when": {"lit": "true"}, "apply": {"model": C.M("downlight_on")}}]}
    item = {"parent": "csm:block/transit/canopies/downlight_on",
            "display": {"gui": {"rotation": [-60, 0, 0], "translation": [0, 0, 0],
                                "scale": [0.9] * 3}}}
    C.add(reg, 'new BlockCanopyDownlight("%s")' % reg,
          ("Canopy Downlight", "Dach-Downlight", "Foco de Marquesina", "Takdownlight"),
          models, bs, item=item, tab=TAB)


MESSAGES = {
    "corner": ("Canopy corner set at %s %s %s: click the opposite corner",
               "Dachecke bei %s %s %s gesetzt: gegenüberliegende Ecke anklicken",
               "Esquina fijada en %s %s %s: haz clic en la esquina opuesta",
               "Hörn satt vid %s %s %s: klicka på motsatt hörn"),
    "cleared": ("Canopy corner cleared", "Dachecke gelöscht", "Esquina borrada",
                "Hörn rensat"),
    "clearance": ("Canopy clearance: %s blocks", "Dachhöhe: %s Blöcke",
                  "Altura libre: %s bloques", "Fri höjd: %s block"),
    "no_deck": ("Hold a bus station canopy deck in your off hand",
                "Halte ein Busbahnhofdach in der Nebenhand",
                "Sostén una marquesina de estación en la mano secundaria",
                "Håll ett busstationstak i andra handen"),
    "too_big": ("A canopy is at most %s blocks a side", "Ein Dach ist höchstens %s Blöcke lang",
                "Una marquesina mide como máximo %s bloques por lado",
                "Ett tak är högst %s block per sida"),
    "too_small": ("This style needs a depth of at least %s", "Dieser Stil braucht mindestens %s Tiefe",
                  "Este estilo necesita al menos %s de fondo",
                  "Den här stilen kräver minst %s djup"),
    "blocked": ("%s %s %s is in the way", "%s %s %s ist im Weg", "%s %s %s estorba",
                "%s %s %s är i vägen"),
    "short": ("Not enough: needs %s decks, %s columns, %s lights",
              "Nicht genug: braucht %s Dachteile, %s Stützen, %s Leuchten",
              "No alcanza: hacen falta %s tejados, %s columnas, %s focos",
              "Inte nog: kräver %s takdelar, %s pelare, %s lampor"),
    "built": ("Built a %s x %s canopy at %s clear: %s decks, %s columns, %s lights",
              "Dach %s x %s gebaut, %s hoch: %s Dachteile, %s Stützen, %s Leuchten",
              "Marquesina de %s x %s construida a %s: %s tejados, %s columnas, %s focos",
              "Byggde ett tak %s x %s, %s högt: %s takdelar, %s pelare, %s lampor"),
    "trimmed": ("Depth trimmed to %s to fit whole %s-block modules",
                "Tiefe auf %s gekürzt, damit ganze %s-Block-Module passen",
                "Fondo recortado a %s para módulos enteros de %s bloques",
                "Djupet kortat till %s för hela moduler om %s block"),
    "phase": ("Canopy profile tile %s of %s", "Dachprofil Teil %s von %s",
              "Pieza de perfil %s de %s", "Takprofil del %s av %s"),
}


def tool():
    C.add_item("canopy_tool", 'ItemCanopyTool.class, fmlPreInitializationEvent',
               ("Canopy Tool", "Dachwerkzeug", "Herramienta de Marquesinas", "Takverktyg"),
               "canopy_tool", tab=TAB)
    for key, names in MESSAGES.items():
        C.add_lang("csm.transit.canopy." + key, names)


register_textures()
decks()
columns()
downlight()
tool()

if __name__ == "__main__":
    if "--check" not in sys.argv and "--fragments" not in sys.argv:
        shutil.rmtree(os.path.join(ASSETS, "models", "block", "transit", "canopies"),
                      ignore_errors=True)
    sys.exit(C.main())
