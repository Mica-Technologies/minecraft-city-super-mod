#!/usr/bin/env python3
"""Every asset the Streetscape tab's hydrants and news racks ship: dry-barrel fire hydrants in
six colour schemes, a wall hydrant, a sidewalk standpipe connection, and newspaper racks that
join into banks.

    python dev-env-utils/scripts/gen_streetscape_street_furniture.py
    python dev-env-utils/scripts/gen_streetscape_street_furniture.py --check
    python dev-env-utils/scripts/gen_streetscape_street_furniture.py --fragments

The hydrants are one OBJ (a barrel, a bonnet and three nozzles, all lathed, so they are round)
with two materials, #body and #cap; each colour scheme is a blockstate retexturing them. NFPA 291
colour-codes the bonnet and caps by flow, so the yellow hydrants come with blue, green, orange
and red bonnets; the red ones with white and with silver. The side nozzles are lathes turned onto
their sides by a proper rotation, so their winding and normals stay right.

The news racks are BlockNewsRack: side by side, facing the same way, they form a bank whose
plinth and top rail run on through the joints, with an end panel only on the two racks at the
ends. The blockstate is a multipart picking the end panels from the LEFT/RIGHT actual state.
Mastheads are invented; no real paper's name is drawn.

Wall hydrant and standpipe are JSON elements. Everything is a BlockUtilityBox, so it settles
onto sloped surfaces.
"""
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_pedestal_pole as pole  # noqa: E402
import gen_streetscape_utility as gu  # noqa: E402

C = lc.Catalogue("gen_streetscape_street_furniture.py", "streetscape/furniture",
                 "streetscape/furniture", assets=gu.ASSETS)
TAB = "CsmTabStreetscape"
slab = gu.slab
decal = gu.decal
post = lc.post

HYDRANT_YELLOW = (226, 184, 28)
HYDRANT_RED = (178, 30, 30)
CAP_COLOURS = {
    "blue": (36, 78, 170), "green": (40, 136, 62), "orange": (228, 118, 30),
    "red": (186, 30, 30), "white": (232, 232, 226), "silver": (180, 184, 186),
}
BRASS = (184, 150, 70)
STEEL = (150, 152, 150)
FRAME = (40, 42, 44)
PAPER = (214, 212, 204)
GLASS = (60, 70, 76)

LATHE_SIDES = 20


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def aspect_texture(width, height, draw):
    """A decal for a face width x height pixels: drawn at that aspect on a 64-texel-wide
    canvas, then stretched to the square a block texture must be; the face squashes it back."""
    h = max(4, int(round(64 * height / float(width))))
    img = Image.new("RGBA", (64, h), (0, 0, 0, 255))
    draw(img)
    return img.resize((64, 64), Image.NEAREST)


def masthead(colour, text, ink=(250, 250, 246)):
    def draw(img):
        lc.rect(img, 0, 0, 64, img.height, colour)
        lc.draw_text_centred(img, text, 32, (img.height - 5) // 2, ink)
    return aspect_texture(13.6, 2.7, draw)


def window(paper_title):
    """A rack's window: a folded paper behind glass, its headline a dark bar."""
    def draw(img):
        lc.rect(img, 0, 0, 64, img.height, GLASS)
        lc.rect(img, 6, 4, 58, img.height - 2, PAPER)
        lc.draw_text_centred(img, paper_title, 32, 6, (40, 40, 40))
        for y in range(14, img.height - 4, 4):
            lc.rect(img, 9, y, 30, y + 1, (120, 120, 116))
            lc.rect(img, 34, y, 55, y + 1, (120, 120, 116))
    return aspect_texture(12.8, 7.5, draw)


RACKS = [
    # registry suffix, colour, masthead, names
    ("blue", (30, 70, 150), "DAILY CITY",
     ("Newspaper Rack (Blue)", "Zeitungsautomat (Blau)", "Expendedor de Periódicos (Azul)",
      "Tidningsautomat (Blå)")),
    ("red", (168, 30, 30), "METRO TIMES",
     ("Newspaper Rack (Red)", "Zeitungsautomat (Rot)", "Expendedor de Periódicos (Rojo)",
      "Tidningsautomat (Röd)")),
    ("green", (30, 108, 60), "THE HERALD",
     ("Newspaper Rack (Green)", "Zeitungsautomat (Grün)", "Expendedor de Periódicos (Verde)",
      "Tidningsautomat (Grön)")),
    ("yellow", (218, 176, 30), "EVENING POST",
     ("Newspaper Rack (Yellow)", "Zeitungsautomat (Gelb)", "Expendedor de Periódicos (Amarillo)",
      "Tidningsautomat (Gul)")),
    ("free", (226, 226, 220), "FREE WEEKLY",
     ("Newspaper Rack (Free Paper)", "Zeitungsständer (Gratiszeitung)",
      "Expendedor de Periódicos (Gratuito)", "Tidningsställ (Gratistidning)")),
]


def register_textures():
    tex = {
        "yellow": gu.paint(HYDRANT_YELLOW, 61, grain=4),
        "red": gu.paint(HYDRANT_RED, 62, grain=4),
        "brass": gu.paint(BRASS, 63, grain=5),
        "steel": gu.paint(STEEL, 64, grain=3),
        "frame": gu.paint(FRAME, 65, grain=2),
    }
    for name, rgb in CAP_COLOURS.items():
        tex["cap_" + name] = gu.paint(rgb, 70 + len(name), grain=3)
    for key, rgb, title, _ in RACKS:
        tex["rack_" + key] = gu.paint(rgb, 80 + len(key), grain=3)
        # A coin rack's masthead is a darker band of its colour with white letters; the free
        # rack is white all over, so its masthead is white with dark letters.
        tex["masthead_" + key] = (masthead(rgb, title, (30, 30, 30)) if key == "free"
                                  else masthead(lc.shade(rgb, 0.7), title))
        tex["window_" + key] = window(title)
    for name, img in tex.items():
        C.texture(name)(lambda img=img: img)


register_textures()


# ------------------------------------------------------------------------------------------
# The hydrant OBJ
# ------------------------------------------------------------------------------------------
def rotated(mesh, origin, direction):
    """A view onto mesh in which a lathe about the vertical axis at x = z = 0 comes out lying
    along direction ('north', 'east' or 'west') from origin. Each map is a proper rotation, so
    faces keep their winding and normals their sense."""
    ox, oy, oz = origin
    if direction == "north":        # (x, y, z) -> (x, z, -y)
        xf = lambda p: (ox + p[0], oy + p[2], oz - p[1])
        nxf = lambda n: (n[0], n[2], -n[1])
    elif direction == "east":       # (x, y, z) -> (y, z, x)
        xf = lambda p: (ox + p[1], oy + p[2], oz + p[0])
        nxf = lambda n: (n[1], n[2], n[0])
    else:                           # west: (x, y, z) -> (-y, z, -x)
        xf = lambda p: (ox - p[1], oy + p[2], oz - p[0])
        nxf = lambda n: (-n[1], n[2], -n[0])
    return mesh.sub(xf, nxf)


def hydrant_obj():
    """A dry-barrel hydrant: flange, barrel, bonnet flange (all #body), and the domed bonnet,
    operating nut and the three nozzle caps (#cap). The pumper nozzle faces north (the street,
    as placed facing the player)."""
    body = pole.Mesh()
    cap = pole.Mesh()
    lathe = pole.lathe
    lathe(body, [(4.0, 0.0), (4.0, 0.8), (3.1, 0.8), (3.0, 1.1), (3.0, 8.8), (3.5, 8.8),
                 (3.5, 9.6), (3.1, 9.6)], sides=LATHE_SIDES)
    lathe(cap, [(3.1, 9.6), (3.0, 10.4), (2.6, 11.2), (1.9, 11.8), (1.0, 12.2), (0.9, 12.2),
                (0.9, 13.0), (0.0, 13.0)], sides=LATHE_SIDES)
    # Pumper nozzle, north, and the two hose nozzles, east and west.
    for direction, origin, r_stub, r_cap in (("north", (8.0, 6.0, 5.4), 1.5, 1.8),
                                             ("east", (10.6, 6.8, 8.0), 1.0, 1.2),
                                             ("west", (5.4, 6.8, 8.0), 1.0, 1.2)):
        lathe(rotated(body, origin, direction), [(r_stub, 0.0), (r_stub, 1.4)],
              sides=LATHE_SIDES, cx=0.0, cz=0.0)
        lathe(rotated(cap, origin, direction),
              [(r_stub, 1.4), (r_cap, 1.4), (r_cap, 2.8), (r_cap * 0.6, 2.8),
               (r_cap * 0.6, 3.2), (0.0, 3.2)],
              sides=LATHE_SIDES, cx=0.0, cz=0.0)
    return obj_text([("body", body), ("cap", cap)], "streetscape_hydrant.mtl")


def obj_text(groups, mtl):
    """Several meshes, one material each, into one OBJ (indices offset per group)."""
    lines = ["# Procedurally generated by dev-env-utils/scripts/"
             "gen_streetscape_street_furniture.py -- do not hand edit",
             "mtllib " + mtl, "o streetscape_hydrant"]
    faces = []
    nv = nt = nn = 0
    for material, mesh in groups:
        for p in mesh.v:
            lines.append("v %.6f %.6f %.6f" % (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0))
        for t in mesh.vt:
            lines.append("vt %.6f %.6f" % t)
        for n in mesh.vn:
            lines.append("vn %.6f %.6f %.6f" % n)
        faces.append("usemtl " + material)
        for tri in mesh.f:
            faces.append("f " + " ".join("%d/%d/%d" % (i[0] + nv, i[1] + nt, i[2] + nn)
                                         for i in tri))
        nv, nt, nn = nv + len(mesh.v), nt + len(mesh.vt), nn + len(mesh.vn)
    return "\n".join(lines + faces) + "\n"


# ------------------------------------------------------------------------------------------
# JSON pieces
# ------------------------------------------------------------------------------------------
def capped_pipe_z(cx, cy, r, z0, z1, tex):
    """A round part along z closed at both ends. A cap wider than the pipe it fits on must close
    its back too, or the ring between the two radii is open and the model is see-through there."""
    return lc._octagon("z", cx, cy, r, z0, z1, tex, True, cap_front=True, cap_back=True)


def wall_hydrant():
    """A wall hydrant: a plate on the wall behind (+z) and one outlet with its cap."""
    return ([slab([4.5, 4.5, 15], [11.5, 11.5, 16], "plate")]
            + lc.pipe_z(8, 8, 1.8, 11.5, 15, "body", front=False)
            + capped_pipe_z(8, 8, 2.1, 10.2, 11.5, "cap"))


def standpipe():
    """A sidewalk standpipe (Siamese) connection: a post, a cross head and two inlets facing
    the street, capped in brass."""
    return (post(8, 8, 1.6, 0, 9, "body", top=False)
            + lc.pipe_x(9.8, 8, 1.6, 4.4, 11.6, "body")
            + lc.pipe_z(5.6, 9.8, 1.2, 5.6, 8.0, "body", front=False)
            + lc.pipe_z(10.4, 9.8, 1.2, 5.6, 8.0, "body", front=False)
            + capped_pipe_z(5.6, 9.8, 1.45, 4.6, 5.6, "cap")
            + capped_pipe_z(10.4, 9.8, 1.45, 4.6, 5.6, "cap"))


RACK_BODY_TEX = ("frame", "body", "window", "masthead", "steel")


# The rack stands on a pedestal half a block high (the user found a rack on the ground too low,
# 2026-09-23), so the whole unit is a block and a half: two cells, placed as one.
STAND = 8.0


def rack_body():
    """What every rack draws: its pedestal stand and foot, the plinth and top rail (full width,
    so they run on through a bank), the body, the window and masthead on the front, and the coin
    mechanism on top."""
    s = STAND
    return [
        slab([3, 0, 5], [13, 0.6, 11], "frame"),
        slab([4.5, 0.6, 6.5], [11.5, s, 9.5], "frame", ("north", "south", "east", "west")),
        slab([0, s, 4.5], [16, s + 2.2, 11.5], "frame"),
        slab([0.6, s + 2.2, 3], [15.4, s + 15, 13], "body"),
        slab([0, s + 15, 2.7], [16, s + 15.8, 13.3], "frame"),
        decal([1.6, s + 4, 2.95], [14.4, s + 11.5, 3], "window", "north"),
        decal([1.2, s + 11.9, 2.95], [14.8, s + 14.6, 3], "masthead", "north"),
        slab([10.5, s + 15.8, 3.5], [14, s + 16, 6.5], "steel"),
    ]


def rack_end(left):
    """The end panel at a bank's end, on the rack above its stand. Facing north the viewer's
    left is east (+x)."""
    x0, x1 = (15.2, 16) if left else (0, 0.8)
    return [slab([x0, STAND, 2.6], [x1, STAND + 16, 13.4], "frame")]


def multipart(model_body, model_left, model_right):
    parts = []
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        rot = {"y": y} if y else {}
        parts.append({"when": {"facing": facing}, "apply": dict(model=model_body, **rot)})
        parts.append({"when": {"facing": facing, "left": "false"},
                      "apply": dict(model=model_left, **rot)})
        parts.append({"when": {"facing": facing, "right": "false"},
                      "apply": dict(model=model_right, **rot)})
    return {"multipart": parts}


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def spec(els):
    return gu.spec_java(els, (1, 1, 1), None)


def hydrants():
    C.extra["models/block/%s/streetscape_hydrant.obj" % C.model_dir] = hydrant_obj()
    C.extra["models/block/%s/streetscape_hydrant.mtl" % C.model_dir] = (
        "# Procedurally generated by gen_streetscape_street_furniture.py -- do not hand edit\n"
        "newmtl body\nmap_Kd %s\nnewmtl cap\nmap_Kd %s\n" % (C.T("yellow"), C.T("cap_blue")))
    # Out to the nozzle caps: x 2.2..13.8, z 2.2 (the pumper cap) to 12 (the flange), 13 high.
    box = "new AxisAlignedBB(0.137500, 0.000000, 0.137500, 0.862500, 0.812500, 0.750000)"
    schemes = [
        ("yellow", "blue", ("Yellow", "Blue")), ("yellow", "green", ("Yellow", "Green")),
        ("yellow", "orange", ("Yellow", "Orange")), ("yellow", "red", ("Yellow", "Red")),
        ("red", "white", ("Red", "White")), ("red", "silver", ("Red", "Silver")),
    ]
    # Body colour as a bare adjective, cap colour agreeing with the cap noun in each language.
    de_body = {"Yellow": "Gelb", "Red": "Rot"}
    de_cap = {"Blue": "Blaue", "Green": "Grüne", "Orange": "Orange", "Red": "Rote",
              "White": "Weiße", "Silver": "Silberne"}
    es_body = {"Yellow": "Amarillo", "Red": "Rojo"}
    es_cap = {"Blue": "Azul", "Green": "Verde", "Orange": "Naranja", "Red": "Roja",
              "White": "Blanca", "Silver": "Plateada"}
    sv_body = {"Yellow": "Gul", "Red": "Röd"}
    sv_cap = {"Blue": "Blå", "Green": "Grön", "Orange": "Orange", "Red": "Röd", "White": "Vit",
              "Silver": "Silverfärgad"}
    for body, capc, (bn, cn) in schemes:
        reg = "hydrant_%s_%s_cap" % (body, capc)
        state = {"forge_marker": 1,
                 "defaults": {"model": "csm:%s/streetscape_hydrant.obj" % C.model_dir,
                              "custom": {"flip-v": True},
                              "textures": {"#body": C.T(body), "#cap": C.T("cap_" + capc),
                                           "particle": C.T(body)}},
                 "variants": {"facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                                         "west": {"y": 270}},
                              "inventory": [{"transform": "forge:default-block"}]}}
        java = ('new BlockUtilityBox("%s", new UtilityBoxSpec(1, 1, 1,\n        %s,\n'
                '        null))' % (reg, box))
        C.add(reg, java,
              ("Fire Hydrant (%s, %s Cap)" % (bn, cn),
               "Hydrant (%s, %s Kappe)" % (de_body[bn], de_cap[cn]),
               "Hidrante (%s, Tapa %s)" % (es_body[bn], es_cap[cn]),
               "Brandpost (%s, %s Huv)" % (sv_body[bn], sv_cap[cn])),
              {}, state, tab=TAB)


def fittings():
    els = wall_hydrant()
    C.add("hydrant_wall", 'new BlockUtilityBox("hydrant_wall", %s)' % spec(els),
          ("Wall Hydrant", "Wandhydrant", "Hidrante de Pared", "Väggbrandpost"),
          {"hydrant_wall": gu.model_for_catalogue(
              C, {"body": "brass", "cap": "brass", "plate": "steel"}, els)},
          lc.facing_state(C.M("hydrant_wall")), tab=TAB)
    els = standpipe()
    C.add("standpipe_sidewalk", 'new BlockUtilityBox("standpipe_sidewalk", %s)' % spec(els),
          ("Sidewalk Standpipe Connection", "Steigleitungsanschluss (Gehweg)",
           "Toma Siamesa de Acera", "Stigarledningsanslutning (Trottoar)"),
          {"standpipe_sidewalk": gu.model_for_catalogue(
              C, {"body": "red", "cap": "brass"}, els)},
          lc.facing_state(C.M("standpipe_sidewalk")), tab=TAB)


def racks():
    body_els = rack_body()
    full = body_els + rack_end(True) + rack_end(False)
    for key, _, _, names in RACKS:
        reg = "news_rack_" + key
        tex = {"frame": "frame", "body": "rack_" + key, "window": "window_" + key,
               "masthead": "masthead_" + key, "steel": "steel"}
        models = {
            reg + "_body": gu.model_for_catalogue(C, tex, body_els),
            reg + "_end_left": gu.model_for_catalogue(C, {"frame": "frame"}, rack_end(True)),
            reg + "_end_right": gu.model_for_catalogue(C, {"frame": "frame"}, rack_end(False)),
            reg + "_item": gu.model_for_catalogue(C, tex, gu.inventory_elements(full)),
        }
        state = multipart(C.M(reg + "_body"), C.M(reg + "_end_left"), C.M(reg + "_end_right"))
        java = 'new BlockNewsRack("%s", %s)' % (reg, gu.spec_java(full, (1, 1, 2), None))
        # An item model's parent is not resolved under models/block/ as a blockstate's model is,
        # so it names block/ itself.
        item = {"parent": "csm:block/%s/%s_item" % (C.model_dir, reg)}
        C.add(reg, java, names, models, state, item=item, tab=TAB)


hydrants()
fittings()
racks()

if __name__ == "__main__":
    sys.exit(C.main())
