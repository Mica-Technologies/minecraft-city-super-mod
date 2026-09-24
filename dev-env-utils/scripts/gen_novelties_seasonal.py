#!/usr/bin/env python3
"""Every asset the seasonal novelties in Furniture & Novelties ship: the Christmas wreath, a
group of pumpkins, the nutcracker (the Novelties tab's icon), the scarecrow and the snowman.

    python dev-env-utils/scripts/gen_novelties_seasonal.py            # write
    python dev-env-utils/scripts/gen_novelties_seasonal.py --check    # exit 1 on drift
    python dev-env-utils/scripts/gen_novelties_seasonal.py --boxes    # print bounding boxes

The catalogue, element and OBJ helpers and the conventions (real scale, facing north, a wall at
z = 16, textures drawn here and nothing copied) are gen_novelties.py's; read its docstring.

The wreath, pumpkins and snowman are round, so they are OBJ: a bumped torus, ribbed lathes and
stacked spheres. The nutcracker and scarecrow are figures made of boxes; their faces are drawn
at 64 px onto the one face of the head that shows them.
"""
import math
import os
import random
import sys
from collections import OrderedDict

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_novelties as gn  # noqa: E402

C = gn.Catalogue("gen_novelties_seasonal.py", "novelties/seasonal")
JAVA = gn.JAVA
fbox = gn.fbox
shade = gn.shade
rect = gn.rect
disc = gn.disc
paint = gn.paint
at = gn.at
turned = gn.turned


def speckle(base, size=32, seed=1, spread=0.06, dots=None):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            img.putpixel((x, y), shade(base, 1 + rng.uniform(-spread, spread)) + (255,))
    for colour, n in (dots or []):
        for _ in range(n):
            img.putpixel((rng.randrange(size), rng.randrange(size)), colour + (255,))
    return img


# ==========================================================================================
# Christmas wreath: a bushy evergreen ring for a wall or door, with berries, cones and a bow
# ==========================================================================================
def needles():
    rng = random.Random(101)
    img = speckle((34, 84, 44), seed=102, spread=0.1)
    for _ in range(170):
        x, y = rng.randrange(32), rng.randrange(32)
        c = shade((60, 128, 66), rng.uniform(0.75, 1.2))
        dx = rng.choice((-1, 1))
        for k in range(rng.randint(2, 4)):
            gn.put(img, (x + k * dx) % 32, (y + k) % 32, c)
    return img


def cmaswreath():
    C.tex("needles", needles())
    C.tex("berry", paint((196, 20, 30), seed=103, grain=8))
    C.tex("cone", speckle((110, 70, 38), size=16, seed=104, spread=0.15))
    bow = paint((190, 22, 32), seed=105, grain=4)
    for y in range(0, 16, 4):
        rect(bow, 0, y, 16, y + 1, (220, 60, 66))
    C.tex("ribbon", bow)
    o = gn.Obj()
    cx, cy, cz = 8.0, 8.5, 14.3
    R, r = 4.5, 1.45
    rng = random.Random(106)
    seg, sides = 32, 8
    bumps = [[rng.uniform(-0.25, 0.35) for _ in range(sides)] for _ in range(seg)]
    gn.torus(o, "needles", R, r, at(cx, cy, cz), seg=seg, sides=sides,
             bump=lambda i, j: bumps[i][j])
    # Berries in threes and pine cones, on the front of the ring.
    for k in range(9):
        a = math.radians(20 + k * 40 + rng.uniform(-8, 8))
        rr = R + rng.uniform(-0.8, 0.8)
        bx, by = cx + rr * math.cos(a), cy + rr * math.sin(a)
        if k % 3 == 1:
            gn.rbox(o, "cone", (bx - 0.4, by - 0.6, cz - r - 0.3), (bx + 0.4, by + 0.6, cz - r + 0.5),
                    "z", math.degrees(a), (bx, by, cz))
            continue
        # Each berry of a cluster a little further forward, so no two fronts share a plane.
        for n, (dx, dy) in enumerate(((0, 0), (0.45, 0.3), (-0.2, 0.45))):
            dz = 0.07 * n
            gn.obox(o, "berry", (bx + dx - 0.25, by + dy - 0.25, cz - r - 0.3 - dz),
                    (bx + dx + 0.25, by + dy + 0.25, cz - r + 0.3 - dz))
    # The bow at the bottom: knot, two loops and two tails.
    by, bz = cy - R, cz - r - 0.55
    gn.obox(o, "ribbon", (7.4, by - 0.6, bz - 0.2), (8.6, by + 0.6, bz + 0.6))
    gn.rbox(o, "ribbon", (8.4, by - 0.7, bz), (11.2, by + 0.7, bz + 0.5), "z", 22, (8.4, by, bz))
    gn.rbox(o, "ribbon", (4.8, by - 0.7, bz), (7.6, by + 0.7, bz + 0.5), "z", -22, (7.6, by, bz))
    gn.rbox(o, "ribbon", (8.0, by - 3.0, bz + 0.05), (8.9, by, bz + 0.3), "z", 18, (8.2, by, bz))
    gn.rbox(o, "ribbon", (7.1, by - 3.0, bz + 0.05), (8.0, by, bz + 0.3), "z", -18, (7.8, by, bz))
    C.add_obj("cmaswreath", JAVA + "furniture/BlockCmasWreath.java", "cmaswreath", o,
              OrderedDict([("needles", "needles"), ("berry", "berry"), ("cone", "cone"),
                           ("ribbon", "ribbon")]), "needles")


# ==========================================================================================
# Pumpkins: two orange and a small white one, ribbed, with stems
# ==========================================================================================
RIBS = 8


def pumpkin_skin(base, groove, seed):
    """32 px: a rib every 4 texels round (u), darker toward the stem and the base (v)."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            v = (y + 0.5) / 32.0
            k = 0.78 + 0.3 * math.sin(math.pi * v) + rng.uniform(-0.03, 0.03)
            c = shade(base, k)
            if x % 4 == 0:
                c = shade(groove, k)
            elif x % 4 == 2:
                c = shade(c, 1.08)
            img.putpixel((x, y), c + (255,))
    return img


def pumpkin(o, mat, cx, cz, R, h, stem_tilt):
    prof = []
    for k in range(13):
        t = k / 12.0
        y = h * (0.04 + 0.9 * (1 - math.cos(math.pi * t)) / 2)
        r = R * math.sin(math.pi * t) ** 0.6
        prof.append((r, y))
    prof[0] = (0.0, prof[0][1])
    prof[-1] = (0.0, prof[-1][1])
    gn.lathe(o, mat, prof, at(cx, 0, cz), sides=32,
             radial=lambda a: 1 - 0.08 * (1 + math.cos(RIBS * a)) / 2)
    top = prof[-1][1]
    gn.lathe(o, "stem", [(0.0, -0.3), (0.42, -0.3), (0.34, 0.6), (0.3, 1.2), (0.0, 1.2)],
             at(cx, top, cz).then(turned("z", stem_tilt)), sides=6)


def pumpkins():
    C.tex("pumpkin_orange", pumpkin_skin((230, 124, 32), (178, 80, 20), 111))
    C.tex("pumpkin_deep", pumpkin_skin((212, 100, 26), (160, 64, 16), 112))
    C.tex("pumpkin_white", pumpkin_skin((238, 230, 206), (190, 186, 160), 113))
    C.tex("stem", speckle((104, 110, 56), size=16, seed=114, spread=0.12))
    o = gn.Obj()
    pumpkin(o, "orange", 6.2, 8.2, 3.6, 5.2, 10)
    pumpkin(o, "deep", 11.4, 10.6, 2.6, 3.8, -14)
    pumpkin(o, "white", 10.8, 5.0, 1.9, 2.8, 6)
    C.add_obj("pumpkins", JAVA + "novelties/BlockPumpkins.java", "pumpkins", o,
              OrderedDict([("orange", "pumpkin_orange"), ("deep", "pumpkin_deep"),
                           ("white", "pumpkin_white"), ("stem", "stem")]), "pumpkin_orange")


# ==========================================================================================
# Nutcracker: a painted wooden soldier, 0.9 m, on a plinth
# ==========================================================================================
SKIN = (240, 204, 172)
RED = (178, 28, 36)
GOLD = (222, 180, 60)
BLACK = (28, 28, 30)
WHITE = (240, 240, 236)


def nutcracker():
    C.tex("nut_red", paint(RED, seed=121, grain=4))
    C.tex("nut_white", paint(WHITE, seed=122, grain=3))
    C.tex("nut_black", paint(BLACK, seed=123, grain=3))
    C.tex("nut_gold", paint(GOLD, seed=124, grain=6))
    C.tex("nut_skin", paint(SKIN, seed=125, grain=3))
    C.tex("nut_green", paint((30, 96, 58), seed=126, grain=4))
    head = ((6.7, 9.0, 6.9), (9.3, 11.6, 9.1))
    jacket = ((5.9, 5.0, 6.7), (10.1, 8.7, 9.3))
    hat = ((6.5, 11.6, 6.7), (9.5, 13.8, 9.3))

    face = paint(SKIN, size=64, seed=127, grain=3)
    x0, y0, x1, y1 = gn.region(head[0], head[1], "north", 64)
    mx = (x0 + x1) // 2
    rect(face, x0, y0, x1, y0 + 2, WHITE)                       # hair line
    for ex in (mx - 5, mx + 3):
        rect(face, ex, y0 + 2, ex + 3, y0 + 3, WHITE)           # brows
        rect(face, ex, y0 + 3, ex + 2, y0 + 5, BLACK)           # eyes
        gn.put(face, ex, y0 + 3, (250, 250, 250))
        rect(face, ex - 1, y0 + 6, ex + 3, y0 + 7, (226, 120, 120))   # cheeks
    rect(face, mx - 1, y0 + 4, mx + 1, y0 + 7, shade(SKIN, 0.85))     # nose
    rect(face, mx - 5, y0 + 7, mx + 5, y0 + 8, WHITE)           # moustache
    rect(face, mx - 4, y0 + 8, mx + 4, y0 + 9, WHITE)
    rect(face, mx - 3, y0 + 9, mx + 3, y1, (60, 20, 20))        # the mouth that cracks nuts
    rect(face, mx - 2, y0 + 9, mx + 2, y0 + 10, WHITE)          # teeth
    C.tex("nut_face", face)

    front = paint(RED, size=64, seed=128, grain=4)
    x0, y0, x1, y1 = gn.region(jacket[0], jacket[1], "north", 64)
    mx = (x0 + x1) // 2
    rect(front, mx - 1, y0, mx + 1, y1, GOLD)                   # placket
    for y in range(y0 + 2, y1 - 1, 4):
        rect(front, mx - 6, y, mx + 6, y + 1, GOLD)             # frogging
        rect(front, mx - 7, y - 1, mx - 5, y + 2, shade(GOLD, 1.1))  # buttons
        rect(front, mx + 5, y - 1, mx + 7, y + 2, shade(GOLD, 1.1))
    C.tex("nut_jacket", front)

    shako = paint(BLACK, size=64, seed=129, grain=3)
    x0, y0, x1, y1 = gn.region(hat[0], hat[1], "north", 64)
    mx, my = (x0 + x1) // 2, (y0 + y1) // 2 - 1
    for k in range(4):
        rect(shako, mx - 3 + k, my - k, mx + 3 - k, my + k + 1, GOLD)   # badge
    rect(shako, x0, y0 + 1, x1, y0 + 2, GOLD)
    C.tex("nut_shako", shako)

    els = [
        fbox((5.2, 0.0, 6.2), (10.8, 0.8, 9.8), "green"),
        # Boots and trousers.
        fbox((6.2, 0.8, 6.3), (7.8, 2.4, 9.2), "black"),
        fbox((8.2, 0.8, 6.3), (9.8, 2.4, 9.2), "black"),
        fbox((6.3, 2.4, 7.1), (7.8, 5.0, 8.9), "white"),
        fbox((8.2, 2.4, 7.1), (9.7, 5.0, 8.9), "white"),
        # Jacket, its skirt, belt, collar and epaulettes.
        fbox(jacket[0], jacket[1], "red", per={"north": "jacket"}),
        fbox((5.7, 4.5, 6.5), (10.3, 5.5, 9.5), "red"),
        fbox((5.8, 5.5, 6.6), (10.2, 6.0, 9.4), "black"),
        fbox((7.5, 5.45, 6.5), (8.5, 6.05, 6.62), "gold"),
        fbox((6.9, 8.7, 7.2), (9.1, 9.0, 8.8), "gold"),
        fbox((5.3, 8.2, 7.0), (6.3, 8.9, 9.0), "gold"),
        fbox((9.7, 8.2, 7.0), (10.7, 8.9, 9.0), "gold"),
        # Arms, cuffs, gloves.
        fbox((4.9, 5.6, 7.3), (5.9, 8.4, 8.7), "red"),
        fbox((10.1, 5.6, 7.3), (11.1, 8.4, 8.7), "red"),
        fbox((4.85, 5.1, 7.25), (5.95, 5.6, 8.75), "white"),
        fbox((10.05, 5.1, 7.25), (11.15, 5.6, 8.75), "white"),
        fbox((5.0, 4.4, 7.4), (5.8, 5.1, 8.6), "white"),
        fbox((10.2, 4.4, 7.4), (11.0, 5.1, 8.6), "white"),
        # Head, beard, white hair at the sides and back.
        fbox(head[0], head[1], "skin", per={"north": "face"}),
        fbox((6.6, 7.9, 6.6), (9.4, 9.05, 7.3), "white"),
        fbox((6.55, 9.0, 7.4), (6.7, 11.4, 9.25), "white"),
        fbox((9.3, 9.0, 7.4), (9.45, 11.4, 9.25), "white"),
        fbox((6.7, 9.0, 9.1), (9.3, 11.4, 9.25), "white"),
        # Shako, its band, peak and plume.
        fbox(hat[0], hat[1], "black", per={"north": "shako"}),
        fbox((6.45, 11.6, 6.65), (9.55, 12.0, 9.35), "gold"),
        fbox((6.7, 11.6, 6.1), (9.3, 11.75, 6.7), "black"),
        fbox((7.6, 13.8, 7.6), (8.4, 14.5, 8.4), "white"),
        fbox((7.75, 14.5, 7.75), (8.25, 14.9, 8.25), "red"),
    ]
    C.add_json("nutcracker", JAVA + "novelties/BlockNutcracker.java", "nutcracker",
               OrderedDict([("red", "nut_red"), ("white", "nut_white"), ("black", "nut_black"),
                            ("gold", "nut_gold"), ("skin", "nut_skin"), ("green", "nut_green"),
                            ("face", "nut_face"), ("jacket", "nut_jacket"),
                            ("shako", "nut_shako")]),
               els, "nut_red")


# ==========================================================================================
# Scarecrow: flannel shirt and jeans on a cross of posts, a burlap head and a straw hat
# ==========================================================================================
def plaid():
    img = Image.new("RGBA", (16, 16))
    red, dark, black = (170, 34, 34), (110, 20, 24), (36, 30, 30)
    for y in range(16):
        for x in range(16):
            h, v = y % 8 < 3, x % 8 < 3
            c = black if h and v else dark if h or v else red
            if y % 8 == 5 or x % 8 == 5:
                c = shade(c, 1.3)
            img.putpixel((x, y), c + (255,))
    return img


def straw(size=16, seed=1):
    rng = random.Random(seed)
    img = speckle((214, 184, 104), size=size, seed=seed, spread=0.08)
    for _ in range(size * 3):
        x, y = rng.randrange(size), rng.randrange(size)
        for k in range(rng.randint(2, 5)):
            gn.put(img, x, (y + k) % size, shade((236, 206, 124), rng.uniform(0.85, 1.1)))
    return img


def scarecrow():
    C.tex("flannel", plaid())
    denim = speckle((60, 84, 130), size=16, seed=131, spread=0.08)
    for y in range(0, 16, 2):
        for x in range(16):
            if (x + y) % 4 == 0:
                gn.put(denim, x, y, (80, 104, 150))
    C.tex("denim", denim)
    C.tex("straw", straw(seed=132))
    C.tex("rope", paint((150, 116, 70), seed=133, grain=8))
    C.tex("post_wood", gn.wood((120, 96, 70), seed=134, boards=2, vertical=True))
    C.tex("hat_straw", gn.weave((222, 192, 116), (196, 164, 90), size=16, strand=1))
    C.tex("hat_band", paint((150, 30, 30), seed=135))
    C.tex("burlap", gn.weave((190, 160, 110), (170, 140, 94), size=16, strand=1))
    sack = ((5.9, 21.6, 6.7), (10.1, 25.6, 10.5))
    face = gn.weave((190, 160, 110), (170, 140, 94), size=64, strand=1)
    x0, y0, x1, y1 = gn.region(sack[0], sack[1], "north", 64)
    mx = (x0 + x1) // 2
    for ex in (mx - 6, mx + 2):                                   # eyes: black triangles
        for k in range(4):
            rect(face, ex + k // 2, y0 + 4 + k, ex + 4 - k // 2, y0 + 5 + k, (26, 22, 20))
    rect(face, mx - 1, y0 + 9, mx + 1, y0 + 10, (26, 22, 20))      # nose
    rect(face, mx - 6, y0 + 12, mx + 6, y0 + 13, (40, 30, 24))     # stitched grin
    for x in range(mx - 6, mx + 7, 2):
        rect(face, x, y0 + 11, x + 1, y0 + 14, (40, 30, 24))
    C.tex("sack_face", face)
    els = [
        fbox((7.3, 0.0, 8.7), (8.7, 24.0, 10.1), "post"),
        fbox((-4.0, 19.5, 8.8), (20.0, 20.7, 10.0), "post"),
        # Shirt and sleeves along the cross bar, straw out of the cuffs.
        fbox((5.2, 13.0, 7.0), (10.8, 21.3, 10.6), "shirt"),
        fbox((-2.5, 18.9, 8.1), (5.2, 21.3, 10.6), "shirt"),
        fbox((10.8, 18.9, 8.1), (18.5, 21.3, 10.6), "shirt"),
        fbox((-3.3, 18.6, 8.0), (-2.5, 21.5, 10.7), "straw"),
        fbox((18.5, 18.6, 8.0), (19.3, 21.5, 10.7), "straw"),
        fbox((6.5, 21.3, 7.8), (9.5, 21.8, 9.8), "straw"),
        # Jeans and the rope belt; straw out of the legs.
        fbox((5.5, 7.5, 7.4), (7.9, 13.0, 10.2), "denim"),
        fbox((8.1, 7.5, 7.4), (10.5, 13.0, 10.2), "denim"),
        fbox((5.6, 6.6, 7.5), (7.8, 7.5, 10.1), "straw"),
        fbox((8.2, 6.6, 7.5), (10.4, 7.5, 10.1), "straw"),
        fbox((5.15, 12.9, 6.95), (10.85, 13.5, 10.65), "rope"),
        # Burlap head tied at the neck.
        fbox(sack[0], sack[1], "burlap", per={"north": "face"}),
        fbox((6.3, 21.4, 7.1), (9.7, 21.95, 10.1), "rope"),
        # Straw hat.
        fbox((4.2, 25.4, 5.0), (11.8, 25.8, 12.2), "hat"),
        fbox((5.8, 25.8, 6.6), (10.2, 27.8, 10.6), "hat"),
        fbox((5.75, 25.8, 6.55), (10.25, 26.3, 10.65), "band"),
    ]
    C.add_json("scarecrow", JAVA + "novelties/BlockScarecrow.java", "scarecrow",
               OrderedDict([("post", "post_wood"), ("shirt", "flannel"), ("straw", "straw"),
                            ("denim", "denim"), ("rope", "rope"), ("burlap", "burlap"),
                            ("face", "sack_face"), ("hat", "hat_straw"),
                            ("band", "hat_band")]),
               els, "straw")


# ==========================================================================================
# Snowman: three balls, top hat, carrot, coal, scarf and stick arms
# ==========================================================================================
def snowman():
    C.tex("snow", speckle((238, 242, 248), seed=141, spread=0.035,
                          dots=[((206, 214, 228), 40), ((255, 255, 255), 30)]))
    C.tex("coal", speckle((34, 34, 36), size=16, seed=142, spread=0.2))
    C.tex("hat_black", paint((26, 26, 30), seed=143, grain=3))
    C.tex("hat_red", paint((180, 30, 36), seed=144, grain=4))
    carrot = paint((236, 120, 30), seed=145, grain=6)
    for y in range(1, 16, 3):
        rect(carrot, 0, y, 16, y + 1, (196, 92, 22))
    C.tex("carrot", carrot)
    scarf = paint((190, 30, 40), seed=146, grain=4)
    for y in range(0, 16, 5):
        rect(scarf, 0, y, 16, y + 2, (30, 120, 60))
    C.tex("scarf", scarf)
    C.tex("stick", gn.wood((100, 70, 44), seed=147, boards=2))
    o = gn.Obj()
    balls = ((5.6, 6.0), (14.3, 4.4), (21.0, 3.1))
    for i, (y0, r) in enumerate(balls):
        gn.lathe(o, "snow", gn.sphere_profile(r, steps=12, y0=y0, cut_below=0.0 if i == 0 else None),
                 at(8, 0, 8), sides=20)
    # Top hat: brim, crown, band.
    gn.lathe(o, "hat", [(0.0, 23.25), (3.6, 23.25), (3.6, 23.65), (2.4, 23.65), (2.4, 27.2),
                        (0.0, 27.2)], at(8, 0, 8), sides=20)
    gn.lathe(o, "band", [(2.46, 23.65), (2.46, 24.35)], at(8, 0, 8), sides=20)
    # Carrot nose, pointing north.
    hy, hr = balls[2]
    gn.lathe(o, "carrot", [(0.0, 0.0), (0.45, 0.0), (0.32, 1.3), (0.0, 2.7)],
             turned("x", -90, (8, hy - 0.2, 8 - hr + 0.25)), sides=8)

    def on_front(dx, dy, cy, r, s):
        z = 8 - math.sqrt(max(0.0, r * r - dx * dx - dy * dy))
        gn.obox(o, "coal", (8 + dx - s, cy + dy - s, z - s * 0.8), (8 + dx + s, cy + dy + s, z + s))
    for dx in (-1.1, 1.1):
        on_front(dx, 0.9, hy, hr, 0.3)
    for k in range(5):
        dx = -1.2 + 0.6 * k
        on_front(dx, -1.3 + 0.35 * (dx / 1.2) ** 2, hy, hr, 0.2)
    for dy in (1.8, 0.3, -1.2):
        on_front(0.0, dy, balls[1][0], balls[1][1], 0.32)
    # Scarf round the neck and its tail down the front.
    gn.torus(o, "scarf", 1.95, 0.62, turned("x", 90, (8, 18.35, 8)), seg=16, sides=6)
    gn.rbox(o, "scarf", (8.7, 18.2 - 3.4, 5.55), (9.7, 18.2, 6.05), "x", 40, (9.2, 18.2, 5.8))
    # Stick arms out of the middle ball, each with a twig.
    for side in (1, -1):
        pivot = (8 + 4.0 * side, 15.4, 8)
        arm = turned("z", 30 * side, pivot)
        lo, hi = (0.0, -0.18, -0.18), (5.6, 0.18, 0.18)
        if side < 0:
            lo, hi = (-5.6, -0.18, -0.18), (0.0, 0.18, 0.18)
        gn.obox(o, "stick", lo, hi, arm)
        twig = arm.then(turned("z", 40 * side, (3.4 * side, 0, 0)))
        tl, th = ((0.0, -0.12, -0.12), (1.6, 0.12, 0.12)) if side > 0 else \
            ((-1.6, -0.12, -0.12), (0.0, 0.12, 0.12))
        gn.obox(o, "stick", tl, th, twig)
    C.add_obj("snowman", JAVA + "furniture/BlockSnowman.java", "snowman", o,
              OrderedDict([("snow", "snow"), ("coal", "coal"), ("hat", "hat_black"),
                           ("band", "hat_red"), ("carrot", "carrot"), ("scarf", "scarf"),
                           ("stick", "stick")]), "snow")


if __name__ == "__main__":
    for _build in (cmaswreath, pumpkins, nutcracker, scarecrow, snowman):
        _build()
    sys.exit(C.main())
