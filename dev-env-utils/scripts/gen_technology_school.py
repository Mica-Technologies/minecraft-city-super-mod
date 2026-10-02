#!/usr/bin/env python3
"""Every asset the Technology tab's school time and PA set ships: two PA speakers, the clocks,
the clock/speaker panels, the bell schedule controller, the hallway bell and the bell linker.

    python dev-env-utils/scripts/gen_technology_school.py
    python dev-env-utils/scripts/gen_technology_school.py --check
    python dev-env-utils/scripts/gen_technology_school.py --fragments   # tab lines to paste

What is here, and the class that places each (package technology.school unless named):

- **PA speakers** (technology.BlockSpeakerFactory, so the TTS Linker and the Bell System Linker
  both take them and right-click cycles the ambient sound, as every speaker in the tab does): a
  white cube speaker with a round perforated grille, and a beige surface-mount wall box with a
  round dot grille and four screws. Both face any of six ways, so the cube hangs on a ceiling.
- **Clocks** (BlockSchoolClock): the black-rim classroom clock, the double-dial clock on a wall
  bracket (its dials face along the wall, as a hallway clock's do) and the double-dial clock hung
  from a ceiling. The hands are TileEntitySchoolClockRenderer's, drawn from the world's time;
  each clock's dials (middle, face, radius, which way they look, whether there is a red sweep
  hand) are written into its tab line from the numbers its model is drawn from, so the two
  cannot drift.
- **Clock/speaker panels** (BlockSchoolClockSpeakerPanel): a long grey panel with two pinstripes,
  a clock and an octagonal speaker grille, two blocks wide or two blocks tall. Each half is its
  own block with its own model, placed and broken together; the speaker half is an ordinary
  speaker (TileEntitySpeaker), the clock half a clock.
- **The bell schedule controller** (BlockBellController): a wall unit with a small display and
  keypad. Its schedule and links are its tile entity's; its model does not change.
- **The hallway bell** (BlockSchoolBell): the red dome gong on a black back box, its striker
  under the dome's rim.
- **The Bell System Linker** (ItemBellLinker): its item sprite.

The maker's name on the dials, the panel and the controller is an invented one, Micaplex.

Every model faces north with its wall at z = 16, as every wall-mounted device in the mod does.
Dials are 128 px squares whose corners are transparent, laid on a plane in front of an octagonal
case whose corners stay just inside the dial's circle, so the case is round from the front and
octagonal only from the side. The numerals are set in Poppins (OFL), fetched once into the
gitignored _font_cache/ as gen_ads.py fetches it, so a fresh clone's --check needs the network
once.
"""
import math
import os
import random
import sys
import urllib.request

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "technology", "src", "main", "resources", "assets",
                      "csm")

C = lc.Catalogue("gen_technology_school.py", "technology/school", "technology/school",
                 assets=ASSETS)
TAB = "CsmTabTechnology"

ALL = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
OCT = math.cos(math.pi / 8)        # an octagon whose corners touch a circle: inradius / radius

WHITE = (238, 239, 236)
CUBE_WHITE = (232, 234, 234)
BEIGE = (226, 220, 196)
PANEL_GREY = (178, 180, 180)
STRIPE = (44, 46, 48)
CASE_DARK = (44, 46, 46)
BLACK = (22, 23, 24)
RED = (186, 28, 26)
STEEL = (160, 164, 168)
CREAM = (236, 228, 204)

# ------------------------------------------------------------------------------------------
# Fonts (Poppins, OFL, from the Google Fonts repository into _font_cache/, as gen_ads.py does)
# ------------------------------------------------------------------------------------------
FONT_CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_font_cache")
_GF = "https://github.com/google/fonts/raw/main/ofl/poppins/"


def font(name, size):
    path = os.path.join(FONT_CACHE, name)
    if not os.path.exists(path):
        os.makedirs(FONT_CACHE, exist_ok=True)
        print("fetching " + _GF + name)
        with urllib.request.urlopen(_GF + name, timeout=60) as response:
            data = response.read()
        with open(path, "wb") as out:
            out.write(data)
    return ImageFont.truetype(path, size)


# ------------------------------------------------------------------------------------------
# Drawing helpers
# ------------------------------------------------------------------------------------------
def grain(colour, size=16, amount=3, seed=1):
    return lc.fill(colour, size, amount, seed)


def rgba(c, a=255):
    return tuple(int(v) for v in c[:3]) + (a,)


def dial(face, numerals, rim, rim_frac, font_name, numeral_px, numeral_r, ticks, maker,
         maker_colour):
    """A clock dial: drawn at 512 px and reduced to 128, so the numerals and the rim are
    smooth. Outside the rim is transparent; the octagonal case behind it never shows there.

    numeral_px is the numerals' cap height and numeral_r the radius they are set on, both as a
    fraction of the dial's radius; ticks is (minute tick inner radius, hour tick inner radius,
    outer radius, minute tick width, hour tick width), fractions again, or None."""
    S = 512
    R = S / 2.0
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((1, 1, S - 2, S - 2), fill=rgba(rim))
    inner = R * (1 - rim_frac)
    d.ellipse((R - inner, R - inner, R + inner, R + inner), fill=rgba(face))
    if ticks:
        m_in, h_in, out, m_w, h_w = ticks
        for m in range(60):
            a = math.radians(m * 6)
            r0 = (h_in if m % 5 == 0 else m_in) * inner
            r1 = out * inner
            w = (h_w if m % 5 == 0 else m_w) * inner
            d.line((R + math.sin(a) * r0, R - math.cos(a) * r0,
                    R + math.sin(a) * r1, R - math.cos(a) * r1), fill=rgba(BLACK), width=int(w))
    f = font(font_name, int(numeral_px * inner * 1.38))
    for h in range(1, 13):
        a = math.radians(h * 30)
        cx = R + math.sin(a) * numeral_r * inner
        cy = R - math.cos(a) * numeral_r * inner
        d.text((cx, cy), str(h), font=f, fill=rgba(BLACK), anchor="mm")
    if maker:
        # The maker's name, small under the twelve as the real ones carry theirs: the pixel
        # font at three texels of the 512 a font pixel, under one texel tall a stroke at 128.
        lc_img = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
        lc.draw_text_centred(lc_img, "MICAPLEX", 32, 5, maker_colour, 1)
        wm = lc_img.resize((64 * 3, 16 * 3), Image.NEAREST)
        img.alpha_composite(wm, (int(R - 96), int(R - inner * 0.46 - 24)))
    return img.resize((128, 128), Image.LANCZOS)


def dot_grid(img, inside, pitch, dot, colour, offset=0.0):
    """Perforations: a dot every pitch texels wherever inside(x, y) holds (texel centres)."""
    d = ImageDraw.Draw(img)
    n = int(img.width / pitch) + 2
    for j in range(n):
        for i in range(n):
            x = offset + i * pitch + (pitch / 2 if j % 2 else 0)
            y = offset + j * pitch * 0.866
            if inside(x, y):
                d.ellipse((x - dot, y - dot, x + dot, y + dot), fill=rgba(colour))


def ring_dots(img, cx, cy, radius, pitch, dot, colour):
    """Perforations set in rings round a centre, as a round PA grille's are."""
    d = ImageDraw.Draw(img)
    d.ellipse((cx - dot, cy - dot, cx + dot, cy + dot), fill=rgba(colour))
    k = 1
    while k * pitch <= radius:
        r = k * pitch
        count = max(6, int(round(2 * math.pi * r / pitch)))
        for i in range(count):
            a = 2 * math.pi * i / count + k * 0.37
            x, y = cx + r * math.cos(a), cy + r * math.sin(a)
            d.ellipse((x - dot, y - dot, x + dot, y + dot), fill=rgba(colour))
        k += 1


def supersampled(size, colour, draw, k=4, seed=1, amount=2):
    """Draws at k times the size over a grained ground and reduces, for smooth round parts."""
    big = lc.fill(colour, size, amount, seed).resize((size * k, size * k), Image.NEAREST)
    draw(big, k)
    return big.resize((size, size), Image.LANCZOS)


def screw(d, x, y, r, k):
    d.ellipse((x - r, y - r, x + r, y + r), fill=rgba((150, 150, 146)))
    d.ellipse((x - r * 0.55, y - r * 0.55, x + r * 0.55, y + r * 0.55),
              fill=rgba((112, 112, 108)))
    d.line((x - r * 0.6, y, x + r * 0.6, y), fill=rgba((70, 70, 68)), width=max(1, k // 2))


def octagon_contains(cx, cy, r):
    """A regular octagon with its edges square to the axes, of inradius r."""
    t = r * math.sqrt(2)

    def inside(x, y):
        dx, dy = abs(x - cx), abs(y - cy)
        return dx <= r and dy <= r and dx + dy <= t
    return inside


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
@C.texture("dial_classroom")
def tex_dial_classroom():
    """The classroom clock (and the panels' and the ceiling clock's): white, a black rim, bold
    numerals, a minute track of short ticks and the maker's name under the twelve."""
    return dial(WHITE, True, BLACK, 0.10, "Poppins-SemiBold.ttf", 0.165, 0.76,
                (0.925, 0.88, 0.985, 0.022, 0.03), True, (96, 96, 100))


@C.texture("dial_vintage")
def tex_dial_vintage():
    """The double-dial bracket clock's: cream, a thin dark rim, lighter numerals inside a
    minute track, as the old wall-bracket clocks are."""
    return dial(CREAM, True, (52, 52, 50), 0.05, "Poppins-Regular.ttf", 0.15, 0.70,
                (0.90, 0.85, 0.975, 0.02, 0.03), True, (110, 104, 92))


@C.texture("white")
def tex_white():
    return grain(CUBE_WHITE, seed=11)


@C.texture("beige")
def tex_beige():
    return grain(BEIGE, seed=12)


@C.texture("panel_grey")
def tex_panel_grey():
    return grain(PANEL_GREY, seed=13, amount=2)


@C.texture("case_dark")
def tex_case_dark():
    return grain(CASE_DARK, seed=14)


@C.texture("black")
def tex_black():
    return grain(BLACK, seed=15, amount=2)


@C.texture("steel")
def tex_steel():
    return grain(STEEL, seed=16, amount=5)


@C.texture("bell_red")
def tex_bell_red():
    return grain(RED, seed=17, amount=4)


@C.texture("cube_front")
def tex_cube_front():
    """The cube speaker's front: a white face, a slight border, and a round grille of fine
    perforations filling most of it (64 px over 11 units)."""
    def draw(img, k):
        d = ImageDraw.Draw(img)
        s = img.width
        d.rectangle((0, 0, s - 1, s - 1), outline=rgba((206, 208, 208)), width=2 * k)
    img = supersampled(64, CUBE_WHITE, draw, seed=21)
    # Perforations on whole texels, every other one, so the grille does not shimmer.
    for y in range(64):
        for x in range(64):
            if (x + y) % 2 == 0 and x % 2 == 0 and (x + 0.5 - 32) ** 2 + (y + 0.5 - 32) ** 2 <= 25.5 ** 2:
                img.putpixel((x, y), rgba((104, 106, 108)))
    return img


@C.texture("wallbox_front")
def tex_wallbox_front():
    """The surface-mount wall box's front: beige, a round grille of dots set in rings, and a
    screw in each corner (64 px over 14 units)."""
    def draw(img, k):
        d = ImageDraw.Draw(img)
        s = img.width
        d.rectangle((0, 0, s - 1, s - 1), outline=rgba((204, 198, 174)), width=k)
        for x in (5, 59):
            for y in (5, 59):
                screw(d, x * k, y * k, 1.3 * k, k)
    img = supersampled(64, BEIGE, draw, seed=22)
    # Holes of two by two texels set in rings, as the photographed grille's are.
    px = img.load()
    taken = set()
    for k in range(0, 7):
        r = k * 3.2
        count = 1 if k == 0 else int(round(2 * math.pi * r / 3.4))
        for i in range(count):
            a = 2 * math.pi * i / count + k * 0.41
            x = int(round(31 + r * math.cos(a)))
            y = int(round(31 + r * math.sin(a)))
            if any((x + dx, y + dy) in taken for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                continue
            taken.add((x, y))
            for dx in (0, 1):
                for dy in (0, 1):
                    px[x + dx, y + dy] = rgba((58, 54, 46))
    return img


# The clock/speaker panels: a 64 px texture holds a 16 x 12 unit face (4 texels a unit), in
# the rows (or columns, upright) 8..56. The pinstripes run 1.6 units in from the long edges.
PANEL_LONG = 12.0          # the panel's short dimension, in units
STRIPE_IN = 1.6


def panel_face(horizontal, speaker, seed):
    def draw(img, k):
        d = ImageDraw.Draw(img)
        u = 4 * k          # texels a unit, at the supersampled size
        lo = 8 * k         # the face's first row (or column) of texels
        hi = 56 * k
        for edge in (lo + STRIPE_IN * u, hi - STRIPE_IN * u - u * 0.3):
            if horizontal:
                d.rectangle((0, edge, img.width, edge + u * 0.3), fill=rgba(STRIPE))
            else:
                d.rectangle((edge, 0, edge + u * 0.3, img.height), fill=rgba(STRIPE))
        if speaker:
            # Two screws on the line where the halves meet, inside the pinstripes.
            if horizontal:
                for y in (lo + 3.0 * u, hi - 3.0 * u):
                    screw(d, 64 * k - 1.2 * u, y, 0.32 * u, k)
            else:
                for x in (lo + 3.0 * u, hi - 3.0 * u):
                    screw(d, x, 1.2 * u, 0.32 * u, k)
    img = supersampled(64, PANEL_GREY, draw, seed=seed, amount=2)
    if speaker:
        # The octagonal grille's perforations, every other texel each way.
        inside = octagon_contains(32, 32, 17.5)
        for y in range(1, 64, 2):
            for x in range(1, 64, 2):
                if inside(x + 0.5, y + 0.5):
                    img.putpixel((x, y), rgba((70, 72, 74)))
    return img


@C.texture("panel_h_clock")
def tex_panel_h_clock():
    return panel_face(True, False, 31)


@C.texture("panel_h_speaker")
def tex_panel_h_speaker():
    return panel_face(True, True, 32)


@C.texture("panel_v_clock")
def tex_panel_v_clock():
    return panel_face(False, False, 33)


@C.texture("panel_v_speaker")
def tex_panel_v_speaker():
    return panel_face(False, True, 34)


@C.texture("controller_front")
def tex_controller_front():
    """The bell schedule controller's face (10 x 12 units at 4 texels a unit, columns 12..52,
    rows 8..56): the maker's name, a green display showing a bell time, a keypad and a red
    ring-now button."""
    img = grain((206, 206, 200), 64, 2, 41)
    lc.draw_text_centred(img, "MICAPLEX", 32, 11, (60, 62, 66), 1)
    lc.rect(img, 15, 18, 49, 31, (30, 34, 30))
    lc.rect(img, 16, 19, 48, 30, (52, 88, 48))
    lc.draw_text_centred(img, "08 50", 32, 20, (176, 236, 150), 1)
    lc.rect(img, 31, 21, 32, 22, (176, 236, 150))
    lc.rect(img, 31, 23, 32, 24, (176, 236, 150))
    lc.draw_text_centred(img, "BELL", 32, 25, (150, 210, 128), 1)
    for row in range(4):
        for col in range(3):
            x = 18 + col * 8
            y = 34 + row * 5
            lc.bevel(img, x, y, x + 6, y + 4, (224, 224, 218))
    lc.bevel(img, 43, 34, 48, 53, (196, 40, 36))
    lc.draw_text(img, "R", 44, 41, (255, 236, 230), 1)
    return img


@C.texture("bell_linker")
def tex_bell_linker():
    """The Bell System Linker's sprite: a grey handset with a small red bell on its screen."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    lc.rect(img, 4, 1, 12, 15, (70, 72, 76))
    lc.rect(img, 5, 2, 11, 14, (120, 124, 128))
    lc.rect(img, 6, 3, 10, 8, (210, 220, 212))
    lc.rect(img, 7, 4, 9, 7, RED)
    lc.rect(img, 6, 6, 10, 7, RED)
    lc.rect(img, 7, 7, 9, 8, (60, 60, 60))
    for y in (10, 12):
        for x in (6, 8):
            lc.rect(img, x, y, x + 1, y + 1, (40, 40, 44))
    lc.rect(img, 7, 0, 9, 1, (200, 40, 36))
    return img


# ------------------------------------------------------------------------------------------
# Elements
# ------------------------------------------------------------------------------------------
def B(frm, to, tex, faces=ALL, uv=None):
    """A box whose faces take their uv from their place (or as given)."""
    el = lc.box(list(frm), list(to), tex, faces=faces)
    for f, u in (uv or {}).items():
        if f in el["faces"]:
            el["faces"][f]["uv"] = [round(v, 4) for v in u]
    return el


def dial_plane(face, c1, c2, at, r, tex):
    """A dial: a square plane 2r across at `at` on the axis it faces along, centred on (c1, c2),
    its whole texture on the one face that looks out."""
    full = {face: [0, 0, 16, 16]}
    if face in ("north", "south"):
        return B((c1 - r, c2 - r, at), (c1 + r, c2 + r, at), tex, (face,), full)
    return B((at, c2 - r, c1 - r), (at, c2 + r, c1 + r), tex, (face,), full)


def octagon_z(cx, cy, r, z0, z1, tex, front=False, back=False):
    return lc._octagon("z", cx, cy, r, z0, z1, tex, front or back, cap_front=front,
                       cap_back=back)


def sixteen_z(cx, cy, r, z0, z1, tex, front=True, back=False):
    """A regular 16-sided prism of inradius r along z: eight rectangles r long and
    r tan(11.25) wide, turned 0, 22.5, 45 and -22.5 degrees each way round, whose corners are
    exactly the 16-gon's (element rotations come in steps of 22.5 degrees, so this is the
    roundest a JSON element model can draw). Each draws its two long sides and, where asked,
    its ends, a hair apart along z so the coplanar caps do not fight."""
    a = r * math.tan(math.pi / 16)
    els = []
    k = 0
    for angle in (0, 22.5, 45, -22.5):
        for h1, h2 in ((r, a), (a, r)):
            eps = 0.003 * k
            k += 1
            faces = ["east", "west"] if h1 == r else ["up", "down"]
            if front:
                faces.append("north")
            if back:
                faces.append("south")
            b = B((cx - h1, cy - h2, z0 - eps), (cx + h1, cy + h2, z1 + eps), tex, faces)
            if angle:
                b["rotation"] = {"origin": [cx, cy, z0], "axis": "z", "angle": angle}
            els.append(b)
    return els


def octagon_x(cy, cz, r, x0, x1, tex, ends=False):
    return lc._octagon("x", cy, cz, r, x0, x1, tex, ends)


def display(scale=0.75, rot=(0, 180, 0), y=0.0):
    return {
        "gui": {"rotation": list(rot), "translation": [0, y, 0], "scale": [scale] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25] * 3},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [scale] * 3},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.375] * 3},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0],
                                  "scale": [0.4] * 3},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0],
                                 "scale": [0.4] * 3},
    }


def names(*n):
    assert len(n) == 4
    return n


def jnum(v):
    return ("%g" % v) + ("" if "." in ("%g" % v) else ".0")


def box_java(box):
    return "new double[]{%s}" % ", ".join(jnum(v) for v in box)


def dial_java(face, cx, cy, cz, r, sweep):
    return "new SchoolClockDial(EnumFacing.%s, %s, %s, %s, %s, %s)" % (
        face.upper(), jnum(cx), jnum(cy), jnum(cz), jnum(r), "true" if sweep else "false")


def facing_state(model_path, extra=None):
    return lc.facing_state(model_path, extra)


def model(textures, els, disp=None, ao=True):
    m = lc.model(textures, els, ao=ao)
    if disp:
        m["display"] = disp
    return m


# ------------------------------------------------------------------------------------------
# PA speakers
# ------------------------------------------------------------------------------------------
CUBE_BOX = (2.5, 2.5, 7.0, 13.5, 13.5, 16.0)
WALLBOX_BOX = (1.0, 1.0, 9.5, 15.0, 15.0, 16.0)


def speaker_java(reg, box):
    return ('new BlockSpeakerFactory("%s", Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, '
            '10F, 0F, 0, new AxisAlignedBB(%s), false, false, false, '
            'BlockRenderLayer.CUTOUT_MIPPED, false, false)'
            % (reg, ", ".join("%.6f" % (v / 16.0) for v in box)))


def pa_speakers():
    x0, y0, z0, x1, y1, z1 = CUBE_BOX
    els = [B((x0, y0, z0), (x1, y1, z1), "body", ("east", "west", "up", "down")),
           B((x0, y0, z0), (x1, y1, z1), "front", ("north",), {"north": [0, 0, 16, 16]})]
    reg = "school_pa_speaker_cube"
    C.add(reg, speaker_java(reg, CUBE_BOX),
          names("School PA Speaker (Cube)", "Schul-Lautsprecher (Würfel)",
                "Altavoz de Megafonía Escolar (Cubo)", "Skolhögtalare (Kub)"),
          {reg: model({"body": C.T("white"), "front": C.T("cube_front"),
                       "particle": C.T("white")}, els, display(0.8, (20, 200, 0)))},
          lc.nsewud_state(C.M(reg)), tab=TAB)

    x0, y0, z0, x1, y1, z1 = WALLBOX_BOX
    els = [B((x0, y0, z0), (x1, y1, z1), "body", ("east", "west", "up", "down")),
           B((x0, y0, z0), (x1, y1, z1), "front", ("north",), {"north": [0, 0, 16, 16]})]
    reg = "school_pa_speaker_wallbox"
    C.add(reg, speaker_java(reg, WALLBOX_BOX),
          names("School PA Speaker (Wall Box)", "Schul-Lautsprecher (Wandgehäuse)",
                "Altavoz de Megafonía Escolar (Caja de Pared)", "Skolhögtalare (Väggbox)"),
          {reg: model({"body": C.T("beige"), "front": C.T("wallbox_front"),
                       "particle": C.T("beige")}, els, display(0.8, (20, 200, 0)))},
          lc.nsewud_state(C.M(reg)), tab=TAB)


# ------------------------------------------------------------------------------------------
# Clocks
# ------------------------------------------------------------------------------------------
# BlockSchoolClock / TileEntitySchoolClockRenderer: each dial's middle, face and radius, in
# sixteenths, facing north. The renderer sizes the hands from the radius.
WALL_R, WALL_FACE = 6.6, 13.0
DOUBLE_R, DOUBLE_CY, DOUBLE_CZ, DOUBLE_W, DOUBLE_E = 5.8, 8.6, 6.4, 5.4, 10.6
HANG_R, HANG_CY, HANG_N, HANG_S = 5.8, 7.4, 5.6, 10.4


def clock_java(reg, box, dials):
    return 'new BlockSchoolClock("%s", %s, %s)' % (reg, box_java(box), ", ".join(dials))


def clocks():
    # The classroom clock: a dial on a black octagonal case 3 units deep.
    reg = "school_wall_clock"
    r = WALL_R
    els = [dial_plane("north", 8, 8, WALL_FACE, r, "dial")]
    els += octagon_z(8, 8, r * OCT - 0.02, WALL_FACE + 0.3, 16, "case")
    box = (8 - r, 8 - r, WALL_FACE - 0.2, 8 + r, 8 + r, 16)
    C.add(reg, clock_java(reg, box, [dial_java("north", 8, 8, WALL_FACE, r, True)]),
          names("Micaplex Classroom Clock", "Micaplex Klassenzimmeruhr",
                "Reloj de Aula Micaplex", "Micaplex Klassrumsklocka"),
          {reg: model({"dial": C.T("dial_classroom"), "case": C.T("black"),
                       "particle": C.T("black")}, els, display(0.8))},
          facing_state(C.M(reg)), tab=TAB)

    # The double-dial clock on a wall bracket: a drum lying along the wall, a dial at each end
    # looking along it, held off the wall by an arm from a plate.
    reg = "school_double_clock"
    r = DOUBLE_R
    els = [dial_plane("west", DOUBLE_CZ, DOUBLE_CY, DOUBLE_W, r, "dial"),
           dial_plane("east", DOUBLE_CZ, DOUBLE_CY, DOUBLE_E, r, "dial")]
    els += octagon_x(DOUBLE_CY, DOUBLE_CZ, r * OCT - 0.02, DOUBLE_W + 0.3, DOUBLE_E - 0.3,
                     "case")
    # a band round the drum's middle, as the old cases have
    els += octagon_x(DOUBLE_CY, DOUBLE_CZ, r * OCT + 0.12, 7.6, 8.4, "case")
    els += [B((7.25, DOUBLE_CY - 1.4, DOUBLE_CZ + r * OCT - 0.5), (8.75, DOUBLE_CY + 1.4, 15.2),
              "case", SIDES + ("up", "down")),
            B((6.0, 3.6, 15.2), (10.0, 13.6, 16), "case", SIDES + ("up", "down"))]
    box = (DOUBLE_W - 0.2, DOUBLE_CY - r, DOUBLE_CZ - r, DOUBLE_E + 0.2, max(13.6, DOUBLE_CY + r),
           16)
    C.add(reg, clock_java(reg, box, [dial_java("west", DOUBLE_W, DOUBLE_CY, DOUBLE_CZ, r, True),
                                     dial_java("east", DOUBLE_E, DOUBLE_CY, DOUBLE_CZ, r, True)]),
          names("Micaplex Double-Dial Clock (Wall)", "Micaplex Doppelseitige Uhr (Wand)",
                "Reloj de Doble Esfera Micaplex (Pared)", "Micaplex Dubbelsidig Klocka (Vägg)"),
          {reg: model({"dial": C.T("dial_vintage"), "case": C.T("case_dark"),
                       "particle": C.T("case_dark")}, els, display(0.75, (20, 225, 0)))},
          facing_state(C.M(reg)), tab=TAB)

    # The double-dial clock hung from a ceiling: the same drum turned to face the corridor, on a
    # stem from a canopy.
    reg = "school_hanging_clock"
    r = HANG_R
    els = [dial_plane("north", 8, HANG_CY, HANG_N, r, "dial"),
           dial_plane("south", 8, HANG_CY, HANG_S, r, "dial")]
    els += octagon_z(8, HANG_CY, r * OCT - 0.02, HANG_N + 0.3, HANG_S - 0.3, "case")
    els += octagon_z(8, HANG_CY, r * OCT + 0.12, 7.6, 8.4, "case")
    els += [B((7.4, HANG_CY + r * OCT - 0.3, 7.4), (8.6, 15.5, 8.6), "steel", SIDES),
            B((6.2, 15.5, 6.2), (9.8, 16, 9.8), "steel", SIDES + ("down",))]
    box = (8 - r, HANG_CY - r, HANG_N - 0.2, 8 + r, 16, HANG_S + 0.2)
    C.add(reg, clock_java(reg, box, [dial_java("north", 8, HANG_CY, HANG_N, r, True),
                                     dial_java("south", 8, HANG_CY, HANG_S, r, True)]),
          names("Micaplex Double-Dial Clock (Ceiling)", "Micaplex Doppelseitige Uhr (Decke)",
                "Reloj de Doble Esfera Micaplex (Techo)", "Micaplex Dubbelsidig Klocka (Tak)"),
          {reg: model({"dial": C.T("dial_classroom"), "case": C.T("case_dark"),
                       "steel": C.T("steel"), "particle": C.T("case_dark")}, els,
                      display(0.75, (20, 200, 0)))},
          facing_state(C.M(reg)), tab=TAB)


# ------------------------------------------------------------------------------------------
# Clock/speaker panels
# ------------------------------------------------------------------------------------------
# BlockSchoolClockSpeakerPanel: the panel is 12 units across its short way and 1 deep; the
# clock sits in the middle of its half, its dial PANEL_FACE units out from the wall.
PANEL_Z = 15.0
PANEL_R, PANEL_FACE = 6.3, 13.4


def panel_half(horizontal, clock, inner_end):
    """One half's elements. horizontal: the panel runs along x, y 2..14; inner_end is the side
    where it meets the other half ("east" or "west"; "up" or "down" for an upright panel), which
    gets no face."""
    if horizontal:
        frm, to = (0, 2, PANEL_Z), (16, 14, 16)
        uv = {"north": [0, 2, 16, 14]}
    else:
        frm, to = (2, 0, PANEL_Z), (14, 16, 16)
        uv = {"north": [2, 0, 14, 16]}
    edges = [f for f in ("east", "west", "up", "down") if f != inner_end]
    els = [B(frm, to, "face", ("north",), uv), B(frm, to, "edge", edges)]
    if clock:
        els.append(dial_plane("north", 8, 8, PANEL_FACE, PANEL_R, "dial"))
        els += octagon_z(8, 8, PANEL_R * OCT - 0.02, PANEL_FACE + 0.3, PANEL_Z, "case")
    return els


def scaled(els, cx, cy, k=0.5):
    """The same elements k times the size about (cx, cy, 8), moved to the block's middle, for an
    item model that shows a two-block piece whole in one slot."""
    out = []
    for el in els:
        e = dict(el)
        e["from"] = [round(8 + k * (el["from"][0] - cx), 4), round(8 + k * (el["from"][1] - cy), 4),
                     round(8 + k * (el["from"][2] - 8), 4)]
        e["to"] = [round(8 + k * (el["to"][0] - cx), 4), round(8 + k * (el["to"][1] - cy), 4),
                   round(8 + k * (el["to"][2] - 8), 4)]
        if "rotation" in el:
            o = el["rotation"]["origin"]
            e["rotation"] = dict(el["rotation"])
            e["rotation"]["origin"] = [round(8 + k * (o[0] - cx), 4), round(8 + k * (o[1] - cy), 4),
                                       round(8 + k * (o[2] - 8), 4)]
        out.append(e)
    return out


def moved(els, dx, dy):
    out = []
    for el in els:
        e = dict(el)
        e["from"] = [el["from"][0] + dx, el["from"][1] + dy, el["from"][2]]
        e["to"] = [el["to"][0] + dx, el["to"][1] + dy, el["to"][2]]
        if "rotation" in el:
            o = el["rotation"]["origin"]
            e["rotation"] = dict(el["rotation"])
            e["rotation"]["origin"] = [o[0] + dx, o[1] + dy, o[2]]
        out.append(e)
    return out


def panels():
    for horizontal in (True, False):
        reg = "school_clock_speaker_panel" + ("" if horizontal else "_vertical")
        kind = "h" if horizontal else "v"
        if horizontal:
            # The clock half is the one placed; the speaker half is to the viewer's right, west
            # of it in the model's frame (x 0 is the right as seen from the north).
            clock_els = panel_half(True, True, "west")
            speaker_els = panel_half(True, False, "east")
            whole = clock_els + moved([dict(e) for e in speaker_els], -16, 0)
            inv = scaled(whole, 0, 8)
            box = (0, 8 - PANEL_R, PANEL_FACE - 0.2, 16, 8 + PANEL_R, 16)
            java = 'new BlockSchoolClockSpeakerPanel("%s", false, %s, %s, %s)' % (
                reg, box_java(box), box_java((0, 2, PANEL_Z, 16, 14, 16)),
                dial_java("north", 8, 8, PANEL_FACE, PANEL_R, True))
            disp = display(0.95)
        else:
            # The speaker half is the one placed; the clock half stands on it.
            clock_els = panel_half(False, True, "down")
            speaker_els = panel_half(False, False, "up")
            whole = moved([dict(e) for e in clock_els], 0, 16) + speaker_els
            inv = scaled(whole, 8, 16)
            box = (8 - PANEL_R, 0, PANEL_FACE - 0.2, 8 + PANEL_R, 16, 16)
            java = 'new BlockSchoolClockSpeakerPanel("%s", true, %s, %s, %s)' % (
                reg, box_java(box), box_java((2, 0, PANEL_Z, 14, 16, 16)),
                dial_java("north", 8, 8, PANEL_FACE, PANEL_R, True))
            disp = display(0.95)
        tex_clock = {"face": C.T("panel_%s_clock" % kind), "edge": C.T("panel_grey"),
                     "dial": C.T("dial_classroom"), "case": C.T("black"),
                     "particle": C.T("panel_grey")}
        tex_speaker = {"face": C.T("panel_%s_speaker" % kind), "edge": C.T("panel_grey"),
                       "particle": C.T("panel_grey")}
        tex_inv = {"cface": C.T("panel_%s_clock" % kind), "sface": C.T("panel_%s_speaker" % kind),
                   "edge": C.T("panel_grey"), "dial": C.T("dial_classroom"),
                   "case": C.T("black"), "particle": C.T("panel_grey")}
        # In the item model the two faces need their own textures.
        n_clock = len(clock_els)
        inv_fixed = []
        for i, el in enumerate(inv):
            e = dict(el)
            e["faces"] = {}
            from_clock = i < n_clock
            for f, fc in el["faces"].items():
                fc = dict(fc)
                if fc["texture"] == "#face":
                    fc["texture"] = "#cface" if from_clock else "#sface"
                e["faces"][f] = fc
            inv_fixed.append(e)
        models = {
            reg + "_clock": model(tex_clock, clock_els),
            reg + "_speaker": model(tex_speaker, speaker_els),
            reg + "_item": model(tex_inv, inv_fixed, disp),
        }
        first, second = ((reg + "_clock", reg + "_speaker") if horizontal
                         else (reg + "_speaker", reg + "_clock"))
        state = {"forge_marker": 1, "defaults": {"model": C.M(first)},
                 "variants": {
                     "facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                                "west": {"y": 270}},
                     "secondary": {"false": {"model": C.M(first)},
                                   "true": {"model": C.M(second)}},
                     "inventory": [{"model": C.M(reg + "_item")}]}}
        en = "Micaplex Clock/Speaker Panel" + ("" if horizontal else " (Vertical)")
        de = "Micaplex Uhr-/Lautsprecherpaneel" + ("" if horizontal else " (Vertikal)")
        es = "Panel de Reloj y Altavoz Micaplex" + ("" if horizontal else " (Vertical)")
        sv = "Micaplex Klock-/Högtalarpanel" + ("" if horizontal else " (Vertikal)")
        C.add(reg, java, names(en, de, es, sv), models, state, tab=TAB)


# ------------------------------------------------------------------------------------------
# Bell schedule controller and hallway bell
# ------------------------------------------------------------------------------------------
CONTROLLER_BOX = (3, 2, 13.5, 13, 14, 16)
BELL_BOX = (2.2, 1.0, 8.6, 13.8, 13.8, 16)


def controller():
    reg = "school_bell_controller"
    x0, y0, z0, x1, y1, z1 = CONTROLLER_BOX
    # the face's window: columns 12..52 and rows 8..56 of the 64 px texture, 4 texels a unit
    els = [B((x0, y0, z0), (x1, y1, z1), "face", ("north",), {"north": [3, 2, 13, 14]}),
           B((x0, y0, z0), (x1, y1, z1), "body", ("east", "west", "up", "down"))]
    C.add(reg, 'new BlockBellController("%s", %s)' % (reg, box_java(CONTROLLER_BOX)),
          names("Micaplex Bell Schedule Controller", "Micaplex Pausenklingel-Steuerung",
                "Controlador de Horario de Timbres Micaplex", "Micaplex Ringschemastyrning"),
          {reg: model({"face": C.T("controller_front"), "body": C.T("panel_grey"),
                       "particle": C.T("panel_grey")}, els, display(0.8, (20, 200, 0)))},
          facing_state(C.M(reg), {"powered": {"true": {}, "false": {}}}), tab=TAB)


# The gong's dome, front to back: (inradius, z0, z1), stepped so the front reads as a dome.
DOME = ((1.9, 9.0, 9.5), (3.4, 9.5, 10.2), (4.6, 10.2, 11.0), (5.4, 11.0, 12.0),
        (5.75, 12.0, 13.1))
BELL_CY = 7.8


def bell():
    reg = "school_bell_gong"
    els = []
    for i, (r, z0, z1) in enumerate(DOME):
        els += sixteen_z(8, BELL_CY, r, z0, z1, "red", front=True, back=(i == len(DOME) - 1))
    # the nut that holds the dome, and the back box behind it
    els += octagon_z(8, BELL_CY, 0.7, 8.6, 9.0, "steel", front=True)
    els += sixteen_z(8, BELL_CY, 4.4, 13.1, 16, "back", front=True)
    # the striker: an arm from the back box under the rim, its hammer just inside the dome
    els += [B((7.4, 1.6, 11.6), (8.6, 2.6, 14.0), "steel", SIDES + ("up", "down")),
            B((7.1, 1.4, 11.0), (8.9, 3.4, 11.6), "back", ALL)]
    C.add(reg, 'new BlockSchoolBell("%s", %s)' % (reg, box_java(BELL_BOX)),
          names("School Hallway Bell", "Schulflurglocke", "Timbre de Pasillo Escolar",
                "Skolkorridorklocka"),
          {reg: model({"red": C.T("bell_red"), "steel": C.T("steel"), "back": C.T("black"),
                       "particle": C.T("bell_red")}, els, display(0.85, (20, 200, 0)))},
          facing_state(C.M(reg), {"powered": {"true": {}, "false": {}}}), tab=TAB)


def items():
    C.add_item("school_bell_linker", "ItemBellLinker.class, fmlPreInitializationEvent",
               names("Bell System Linker", "Klingelsystem-Verknüpfer",
                     "Enlazador del Sistema de Timbres", "Ringsystemslänkare"),
               "bell_linker", tab=TAB)


pa_speakers()
clocks()
panels()
controller()
bell()
items()

if __name__ == "__main__":
    sys.exit(C.main())
