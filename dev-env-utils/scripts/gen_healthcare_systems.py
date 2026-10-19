#!/usr/bin/env python3
"""
gen_healthcare_systems.py -- the Healthcare module's wayfinding and building systems.

Wayfinding: the hospital wayfinding panel, a drop-in for Transit's airport_wayfinding_panel
(the same NBT and metadata): cells that join into one panel up to 16 x 6, each cell a charcoal
slab with a frame on the panel's outer edges (actual state), and the legend, pictogram, arrow and
hanger rods BAKED into the chunk by HospitalWayfindingBakedModel from one sheet this script draws:
the printable ASCII glyphs of the committed highway font (Series D), the fourteen hospital
pictograms (black art on a white square), the two arrows and a white swatch for the coloured
backgrounds. The glyph metrics go into HospitalWayfindingGlyphs.java (written here too), so the
Java and the sheet cannot drift.

Building systems: the code blue button and annunciator (a blue strobe, an animated texture, with
the chime from gen_healthcare_sounds.py), the medical gas zone valve box and master alarm panel,
normal and emergency power outlets, and the infant security door panel.

Nothing here is a real product: no maker's name, no red cross (a protected emblem). Lamps and
strobes are animated textures (.mcmeta), so nothing ticks and nothing has a renderer.

Writes, under modules/healthcare/src/main/resources/assets/csm:

  * textures/blocks/healthcare/systems/*.png (+ .mcmeta)
  * models/block/healthcare/systems/*.json
  * blockstates/<registry>.json, and models/item/<registry>.json where needed
  * the tile names and messages in all four lang files

and modules/healthcare/src/main/java/.../healthcare/wayfinding/HospitalWayfindingGlyphs.java.

Usage:
    py gen_healthcare_systems.py              # write everything
    py gen_healthcare_systems.py --check      # fail if the tree has drifted
    py gen_healthcare_systems.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_healthcare_common as hc  # noqa: E402
import gen_healthcare_wards as hw  # noqa: E402
from gen_healthcare_common import (  # noqa: E402
    disc, draw_text_centred, fill, frame, rect, shade)

C = hc.catalogue("gen_healthcare_systems.py", "systems")
T = C.T
M = C.M
W = hw.C.T    # the wards' shared swatches

el = hw.el
rot = hw.rot
model = hw.model
ITEM_SMALL = hw.ITEM_SMALL
B = hw.B
FULL = [0, 0, 16, 16]

PC = hc.PATIENT_CARE
CL = hc.CLINICAL

JAVA_GLYPHS = os.path.join(hc.REPO, "modules", "healthcare", "src", "main", "java", "com",
                           "micatechnologies", "minecraft", "csm", "healthcare", "wayfinding",
                           "HospitalWayfindingGlyphs.java")


def tex(name, fn):
    C.textures[name] = fn


def lamp(img, cx, cy, size, colour):
    """A status lamp: a square, never a small disc (a disc a few texels across draws as a plus
    sign, and a red one as a cross)."""
    x0, y0 = int(round(cx - size / 2.0)), int(round(cy - size / 2.0))
    rect(img, x0, y0, x0 + size, y0 + size, colour)


def MI(name):
    return "csm:block/%s/%s" % (C.model_dir, name)


def front(frm, to, base, key, side="north", faces=hw.ALL):
    """A box whose front shows the whole of a texture."""
    return el(frm, to, base, faces=faces, per={side: key}, uv={side: FULL})


def wall_block(reg, java, names, textures, elements, tab, extra=None, display=None):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    C.add(reg, java, names, {reg: model(textures, elements, display=display)},
          hc.facing_state(M(reg), extra), tab=tab)


def java_for(cls, reg, box6, mat, collides=False):
    return 'new %s("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.WALL, %s)' % (
        cls, reg, B(*box6), mat, "true" if collides else "false")


# ==========================================================================================
# The wayfinding sheet
# ==========================================================================================
SHEET = 512
CAP = 32            # the glyphs' cap height on the sheet, px
GLYPH_ROW = 50      # a row of glyphs: ascenders, caps, descenders and padding
PICTO = 64          # a pictogram's (and an arrow's) square, px
PICTO_TOP = SHEET - 2 * PICTO   # the pictograms and arrows fill the bottom two rows
SUPER = 4           # drawn this many times larger and reduced
FONT = os.path.join(hc.FONT_DIR, "MicaOpenHighway-SeriesD.ttf")

# The pictograms, in HospitalWayfindingSign.Pictogram's order after NONE (the Java stores them
# by id, never by ordinal, and finds each one's cell here by id).
PICTOGRAMS = ("emergency", "xray", "lab", "pharmacy", "maternity", "elevator", "stairs",
              "restroom", "cafe", "chapel", "parking", "info", "accessible", "exit")
ARROWS = ("arrow_straight", "arrow_diagonal")
# A white swatch for the coloured background (and the rods), sampled at its middle.
WHITE_CELL = (SHEET - 16, PICTO_TOP - 16, 12)


def _font(size):
    return ImageFont.truetype(FONT, size)


def _cap_size():
    """The font size whose capitals are CAP * SUPER px tall."""
    f = _font(400)
    b = f.getbbox("H")
    return int(round(400 * CAP * SUPER / float(b[3] - b[1])))


def glyph_images():
    """Each printable ASCII character drawn white on clear at CAP px capitals: its sheet image
    (with a one px clear margin), the image's left edge from the pen and top from the cap line,
    in px, and its advance."""
    size = _cap_size()
    f = _font(size)
    cap_top = f.getbbox("H", anchor="ls")[1]   # negative: the cap line above the baseline
    from fontTools.ttLib import TTFont
    cmap = TTFont(FONT).getBestCmap()
    out = {}
    for code in range(32, 127):
        ch = chr(code)
        if code not in cmap:
            # the font has no such glyph (% ; < > [ \ ] ^ _ ` { | } ~): a gap, never tofu
            out[ch] = (None, 0, 0, CAP * 0.5)
            continue
        adv = f.getlength(ch) / SUPER
        if ch == " ":
            out[ch] = (None, 0, 0, adv)
            continue
        pen_x, base_y = 4 * SUPER * 4, size * 2
        canvas = Image.new("L", (size * 3, size * 3), 0)
        ImageDraw.Draw(canvas).text((pen_x, base_y), ch, font=f, fill=255, anchor="ls")
        bbox = canvas.getbbox()
        if bbox is None:
            out[ch] = (None, 0, 0, adv)
            continue
        cap_y = base_y + cap_top
        x0 = int(math.floor((bbox[0] - pen_x) / float(SUPER))) - 1
        y0 = int(math.floor((bbox[1] - cap_y) / float(SUPER))) - 1
        x1 = int(math.ceil((bbox[2] - pen_x) / float(SUPER))) + 1
        y1 = int(math.ceil((bbox[3] - cap_y) / float(SUPER))) + 1
        crop = canvas.crop((pen_x + x0 * SUPER, cap_y + y0 * SUPER,
                            pen_x + x1 * SUPER, cap_y + y1 * SUPER))
        small = crop.resize((x1 - x0, y1 - y0), Image.BOX)
        out[ch] = (small, x0, y0, adv)
    return out


# --- pictograms: drawn at SUPER x 64 in black on white, reduced ---------------------------
S = PICTO * SUPER
BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)


def _figure(d, cx, top, h, walk=0.0):
    """A standing person, the generic signage figure: a round head and a rounded body with
    legs, h px tall from ``top``; ``walk`` spreads the legs for a walking figure."""
    r = h * 0.09
    d.ellipse([cx - r, top, cx + r, top + 2 * r], fill=BLACK)
    bt = top + 2 * r + h * 0.04
    bw = h * 0.13
    d.rounded_rectangle([cx - bw, bt, cx + bw, bt + h * 0.38], radius=bw * 0.8, fill=BLACK)
    lw = h * 0.055
    hip = bt + h * 0.34
    for side in (-1, 1):
        fx = cx + side * (bw * 0.55 + walk * h * 0.18)
        d.line([(cx + side * bw * 0.45, hip), (fx, top + h)], fill=BLACK, width=int(lw * 2))
    # arms
    for side in (-1, 1):
        sx = cx + side * bw
        d.line([(sx, bt + h * 0.05), (sx + side * (h * 0.06 + walk * h * 0.12), bt + h * 0.3)],
               fill=BLACK, width=int(lw * 1.6))


def _text(d, text, cx, cy, size, series="D"):
    f = ImageFont.truetype(os.path.join(hc.FONT_DIR, "MicaOpenHighway-Series%s.ttf" % series),
                           size)
    d.text((cx, cy), text, font=f, fill=BLACK, anchor="mm")


def picto_emergency(d):
    """An ambulance from the side, its light bar on the cab, a six-armed star on the box."""
    d.rectangle([20, 70, 170, 180], fill=BLACK)                      # box
    d.polygon([(170, 100), (205, 100), (236, 140), (236, 180), (170, 180)], fill=BLACK)
    d.polygon([(178, 108), (203, 108), (226, 138), (178, 138)], fill=WHITE)   # windscreen
    d.rectangle([150, 58, 190, 70], fill=BLACK)                      # light bar
    for wx in (62, 196):
        d.ellipse([wx - 24, 160, wx + 24, 208], fill=BLACK)
        d.ellipse([wx - 11, 173, wx + 11, 195], fill=WHITE)
    cx, cy, r = 95, 122, 36
    for a in (90, 30, 150):
        t = math.radians(a)
        dx, dy = math.cos(t) * r, -math.sin(t) * r
        d.line([(cx - dx, cy - dy), (cx + dx, cy + dy)], fill=WHITE, width=20)


def picto_xray(d):
    """A chest film: the ribs and spine light on dark."""
    d.rounded_rectangle([36, 28, 220, 228], radius=14, fill=BLACK)
    d.rectangle([122, 44, 134, 212], fill=WHITE)                     # spine
    for i in range(6):
        y = 64 + i * 24
        for side in (-1, 1):
            x0 = 128 + side * 14
            pts = [(x0, y), (x0 + side * 40, y - 10), (x0 + side * 72, y + 6),
                   (x0 + side * 76, y + 26)]
            d.line(pts, fill=WHITE, width=9, joint="curve")


def picto_lab(d):
    """A conical lab flask with liquid and two bubbles."""
    d.rectangle([108, 26, 148, 40], fill=BLACK)
    d.polygon([(112, 40), (144, 40), (144, 100), (214, 214), (42, 214), (112, 100)], fill=BLACK)
    d.polygon([(124, 52), (132, 52), (132, 104), (190, 200), (66, 200), (124, 104)], fill=WHITE)
    d.polygon([(88, 150), (168, 150), (190, 200), (66, 200)], fill=BLACK)
    d.ellipse([110, 116, 126, 132], fill=BLACK)
    d.ellipse([136, 92, 146, 102], fill=BLACK)


def picto_pharmacy(d):
    """Rx, the prescription mark, and a capsule under it."""
    _text(d, "Rx", 128, 104, 150, series="E")
    d.rounded_rectangle([56, 176, 200, 220], radius=22, fill=BLACK)
    d.rounded_rectangle([132, 184, 192, 212], radius=14, fill=WHITE)


def picto_maternity(d):
    """A parent holding a baby."""
    d.ellipse([104, 22, 152, 70], fill=BLACK)                        # head
    d.polygon([(96, 80), (160, 80), (196, 236), (60, 236)], fill=BLACK)   # dress
    d.ellipse([132, 96, 196, 160], fill=WHITE)                       # the arm's cradle
    d.ellipse([140, 104, 188, 152], fill=BLACK)                      # the baby's bundle
    d.ellipse([168, 86, 200, 118], fill=BLACK)                       # the baby's head
    d.ellipse([174, 92, 194, 112], fill=BLACK)


def picto_elevator(d):
    """A lift car with a person inside, up and down triangles beside it."""
    d.rectangle([28, 28, 168, 228], fill=BLACK)
    d.rectangle([42, 42, 154, 214], fill=WHITE)
    _figure(d, 98, 62, 140)
    d.polygon([(186, 110), (228, 110), (207, 72)], fill=BLACK)
    d.polygon([(186, 146), (228, 146), (207, 184)], fill=BLACK)


def picto_stairs(d):
    """A flight of stairs rising to the right with a person climbing it."""
    pts = [(20, 236)]
    x, y = 20, 236
    for _ in range(4):
        y -= 40
        pts.append((x, y))
        x += 54
        pts.append((x, y))
    pts += [(236, y), (236, 236)]
    d.polygon(pts, fill=BLACK)
    _figure(d, 112, 40, 110, walk=0.6)


def picto_restroom(d):
    """A man and a woman either side of a bar."""
    _figure(d, 66, 34, 196)
    d.rectangle([124, 30, 132, 226], fill=BLACK)
    # the woman: a head and a dress
    d.ellipse([172, 34, 208, 70], fill=BLACK)
    d.polygon([(170, 80), (210, 80), (234, 176), (146, 176)], fill=BLACK)
    for lx in (176, 204):
        d.rectangle([lx - 6, 176, lx + 6, 230], fill=BLACK)


def picto_cafe(d):
    """A cup of coffee on a saucer, steam rising."""
    d.rounded_rectangle([40, 214, 216, 230], radius=8, fill=BLACK)
    d.rectangle([60, 120, 172, 206], fill=BLACK)
    d.ellipse([60, 180, 172, 220], fill=BLACK)
    d.ellipse([150, 128, 214, 186], fill=BLACK)
    d.ellipse([166, 142, 198, 172], fill=WHITE)
    for sx in (86, 116, 146):
        d.line([(sx, 104), (sx - 10, 80), (sx + 4, 58), (sx - 6, 34)], fill=BLACK, width=10,
               joint="curve")


def picto_chapel(d):
    """A lit candle on a holder: a quiet room, of no one faith."""
    d.ellipse([110, 22, 146, 92], fill=BLACK)                        # flame
    d.ellipse([120, 52, 136, 86], fill=WHITE)
    d.rectangle([126, 92, 130, 104], fill=BLACK)                     # wick
    d.rectangle([98, 104, 158, 206], fill=BLACK)                     # candle
    d.rounded_rectangle([56, 206, 200, 226], radius=8, fill=BLACK)


def picto_parking(d):
    """P, for parking."""
    d.rounded_rectangle([34, 24, 222, 232], radius=18, fill=BLACK)
    d.rounded_rectangle([46, 36, 210, 220], radius=12, fill=WHITE)
    _text(d, "P", 128, 132, 210, series="E")


def picto_info(d):
    """i in a ring: information."""
    d.ellipse([22, 22, 234, 234], fill=BLACK)
    d.ellipse([40, 40, 216, 216], fill=WHITE)
    d.ellipse([112, 56, 144, 88], fill=BLACK)
    d.rectangle([112, 104, 144, 196], fill=BLACK)
    d.rectangle([98, 104, 144, 116], fill=BLACK)
    d.rectangle([98, 186, 158, 198], fill=BLACK)


def picto_accessible(d):
    """A person in a wheelchair: step-free access."""
    d.ellipse([100, 18, 136, 54], fill=BLACK)
    d.line([(112, 66), (104, 146)], fill=BLACK, width=26)            # back
    d.line([(108, 140), (176, 140), (196, 210)], fill=BLACK, width=22, joint="curve")
    d.line([(110, 92), (166, 92)], fill=BLACK, width=18)             # arm
    d.ellipse([46, 128, 166, 248], outline=BLACK, width=18)          # wheel
    d.ellipse([194, 200, 222, 228], fill=BLACK)


def picto_exit(d):
    """A person walking out through a doorway, an arrow leading out."""
    d.rectangle([140, 24, 222, 232], fill=BLACK)
    d.rectangle([154, 38, 208, 232], fill=WHITE)
    _figure(d, 182, 52, 170, walk=0.7)
    d.polygon([(26, 128), (86, 96), (86, 116), (126, 116), (126, 140), (86, 140), (86, 160)],
              fill=BLACK)


PICTO_DRAW = {"emergency": picto_emergency, "xray": picto_xray, "lab": picto_lab,
              "pharmacy": picto_pharmacy, "maternity": picto_maternity,
              "elevator": picto_elevator, "stairs": picto_stairs, "restroom": picto_restroom,
              "cafe": picto_cafe, "chapel": picto_chapel, "parking": picto_parking,
              "info": picto_info, "accessible": picto_accessible, "exit": picto_exit}


def pictogram(kind):
    big = Image.new("RGBA", (S, S), WHITE)
    PICTO_DRAW[kind](ImageDraw.Draw(big))
    return big.resize((PICTO, PICTO), Image.LANCZOS)


def arrow(kind):
    """The arrows: white on clear, tinted with the legend's colour. Straight points right, the
    diagonal up and to the right; the baked model turns them."""
    big = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(big)
    shaft, head = 40, 90
    pts = [(28, 128 - shaft / 2), (150, 128 - shaft / 2), (150, 128 - head),
           (232, 128), (150, 128 + head), (150, 128 + shaft / 2), (28, 128 + shaft / 2)]
    d.polygon(pts, fill=255)
    if kind == "arrow_diagonal":
        big = big.rotate(45, resample=Image.BICUBIC, center=(128, 128))
    alpha = big.resize((PICTO, PICTO), Image.LANCZOS)
    out = Image.new("RGBA", (PICTO, PICTO), (255, 255, 255, 0))
    out.putalpha(alpha)
    return out


def pack_glyphs(glyphs):
    """Lays the glyphs out in rows from the top of the sheet: {char: (x, y)}."""
    pos = {}
    x, y = 0, 0
    for code in range(33, 127):
        img = glyphs[chr(code)][0]
        if img is None:
            continue
        if x + img.width > SHEET:
            x, y = 0, y + GLYPH_ROW
        pos[chr(code)] = (x, y + 2)
        x += img.width + 2
    assert y + GLYPH_ROW <= PICTO_TOP - 16, "the glyphs overflow the sheet"
    return pos


def picto_cells():
    """Each pictogram's and arrow's cell: {name: (x, y)}, eight to a row."""
    names = list(PICTOGRAMS) + list(ARROWS)
    return {n: ((i % 8) * PICTO, PICTO_TOP + (i // 8) * PICTO) for i, n in enumerate(names)}


_GLYPHS = None


def glyphs():
    global _GLYPHS
    if _GLYPHS is None:
        _GLYPHS = glyph_images()
    return _GLYPHS


@C.texture("wayfinding_sheet")
def _wayfinding_sheet():
    g = glyphs()
    pos = pack_glyphs(g)
    # white everywhere, clear where nothing is drawn, so a reduced glyph has no dark fringe
    sheet = Image.new("RGBA", (SHEET, SHEET), (255, 255, 255, 0))
    for ch, (x, y) in pos.items():
        img = g[ch][0]
        white = Image.new("RGBA", img.size, (255, 255, 255, 255))
        white.putalpha(img)
        sheet.paste(white, (x, y))
    for name, (x, y) in picto_cells().items():
        sheet.paste(arrow(name) if name in ARROWS else pictogram(name), (x, y))
    wx, wy, ws = WHITE_CELL
    rect(sheet, wx, wy, wx + ws, wy + ws, (255, 255, 255))
    return sheet


def glyphs_java():
    """HospitalWayfindingGlyphs.java: the sheet's layout, for the baked model."""
    g = glyphs()
    pos = pack_glyphs(g)
    rows = []
    for code in range(32, 127):
        ch = chr(code)
        img, x0, y0, adv = g[ch]
        if img is None:
            rows.append("      0, 0, 0, 0, 0, 0, %d, // %r" % (int(round(adv * 16)), ch))
        else:
            x, y = pos[ch]
            rows.append("      %d, %d, %d, %d, %d, %d, %d, // %s" % (
                x, y, img.width, img.height, x0, y0, int(round(adv * 16)),
                "'\\\\'" if ch == "\\" else repr(ch)))
    cells = picto_cells()
    ids = ", ".join('"%s"' % p for p in PICTOGRAMS)
    pxy = ", ".join("%d, %d" % cells[p] for p in PICTOGRAMS)
    wx, wy, ws = WHITE_CELL
    return """package com.micatechnologies.minecraft.csm.healthcare.wayfinding;

/**
 * The hospital wayfinding panel's sheet ({@code textures/blocks/healthcare/systems/
 * wayfinding_sheet.png}): where each glyph, pictogram and arrow is on it and how the glyphs are
 * set. GENERATED by {@code dev-env-utils/scripts/gen_healthcare_systems.py} with the sheet; do
 * not edit by hand (its {@code --check} fails if the two disagree).
 *
 * @since 2026.10
 */
public final class HospitalWayfindingGlyphs {

  /** The sheet's size, px. */
  public static final int SHEET = %d;
  /** The glyphs' cap height on the sheet, px: the unit every glyph number below is in. */
  public static final int CAP = %d;
  /** A pictogram's or an arrow's square, px. */
  public static final int PICTO = %d;

  /**
   * Per printable ASCII character from 32 (space) to 126, seven numbers: its image's x and y on
   * the sheet, its width and height (0 for none), its left edge from the pen and its top below
   * the cap line (px, the top negative when it reaches above the capitals), and its advance in
   * sixteenths of a px.
   */
  static final int[] GLYPHS = {
%s
  };

  /** The pictograms' ids, in HospitalWayfindingSign.Pictogram's order after NONE. */
  static final String[] PICTOGRAM_IDS = {%s};
  /** Each pictogram's cell's top-left corner on the sheet, x then y. */
  static final int[] PICTOGRAM_XY = {%s};
  /** The straight arrow (pointing right) and the diagonal (up and right): x, y. */
  static final int[] ARROW_STRAIGHT = {%d, %d};
  static final int[] ARROW_DIAGONAL = {%d, %d};
  /** A white swatch, sampled at its middle for the background and the rods: x, y, size. */
  static final int[] WHITE = {%d, %d, %d};

  private HospitalWayfindingGlyphs() {
  }

  /**
   * The index of a character in {@link #GLYPHS} (times seven), or -1 for one the sheet lacks.
   *
   * @param c a character
   * @return its offset into {@link #GLYPHS}, or -1
   */
  static int offset(char c) {
    return c < 32 || c > 126 ? -1 : (c - 32) * 7;
  }

  /**
   * A pictogram's cell by id.
   *
   * @param id the pictogram's id
   * @return its cell's x and y on the sheet, or null for none
   */
  static int[] pictogram(String id) {
    for (int i = 0; i < PICTOGRAM_IDS.length; i++) {
      if (PICTOGRAM_IDS[i].equals(id)) {
        return new int[]{PICTOGRAM_XY[2 * i], PICTOGRAM_XY[2 * i + 1]};
      }
    }
    return null;
  }
}
""" % (SHEET, CAP, PICTO, "\n".join(rows), ids, pxy, cells["arrow_straight"][0],
       cells["arrow_straight"][1], cells["arrow_diagonal"][0], cells["arrow_diagonal"][1],
       wx, wy, ws)


# ==========================================================================================
# The wayfinding panel's model: a slab and the frame on the panel's outer edges
# ==========================================================================================
PANEL_Z0, PANEL_Z1 = 6.0, 10.0   # SHARED with HospitalWayfindingBakedModel.FACE_Z0/FACE_Z1
PANEL_FRAME = 0.8                 # SHARED with HospitalWayfindingLayout.FRAME (in sixteenths)
PANEL_LIP = 0.8                   # the frame stands this proud of each face (> the art's lift)


def wayfinding_panel():
    reg = "hospital_wayfinding_panel"
    f, lip = PANEL_FRAME, PANEL_LIP
    z0, z1 = PANEL_Z0 - lip, PANEL_Z1 + lip
    parts = [
        ("body", {}, [el([0, 0, PANEL_Z0], [16, 16, PANEL_Z1], "panel",
                         faces=("north", "south"))]),
        ("edge_left", {"edge_left": "true"}, [el([16 - f, 0, z0], [16, 16, z1], "frame")]),
        ("edge_right", {"edge_right": "true"}, [el([0, 0, z0], [f, 16, z1], "frame")]),
        ("edge_top", {"edge_top": "true"}, [el([0, 16 - f, z0], [16, 16, z1], "frame")]),
        ("edge_bottom", {"edge_bottom": "true"}, [el([0, 0, z0], [16, f, z1], "frame")]),
    ]
    textures = {"panel": W("charcoal"), "frame": W("stainless"), "particle": W("charcoal")}
    models = {}
    rules = []
    for name, cond, els in parts:
        mname = reg + "_" + name
        models[mname] = model(textures, els)
        for facing, y in hc.ROT_Y.items():
            w = {"facing": facing}
            w.update(cond)
            apply = {"model": M(mname)}
            if y:
                apply["y"] = y
            rules.append({"when": w, "apply": apply})
    # The item: a framed slab with the information pictogram, which also puts the sheet on the
    # atlas through a model (the baked model draws from it; nothing else names it).
    cells = picto_cells()
    px, py = cells["info"]
    k = 16.0 / SHEET
    item_els = [el([0, 0, PANEL_Z0], [16, 16, PANEL_Z1], "panel"),
                el([16 - f, 0, z0], [16, 16, z1], "frame"), el([0, 0, z0], [f, 16, z1], "frame"),
                el([0, 16 - f, z0], [16, 16, z1], "frame"), el([0, 0, z0], [16, f, z1], "frame"),
                el([4, 4, PANEL_Z0 - 0.3], [12, 12, PANEL_Z0], "sheet", faces=("north",),
                   uv={"north": [round(px * k, 4), round(py * k, 4), round((px + PICTO) * k, 4),
                                 round((py + PICTO) * k, 4)]})]
    item_tex = dict(textures, sheet=T("wayfinding_sheet"))
    models[reg + "_item"] = model(item_tex, item_els, display=ITEM_SMALL)
    C.add(reg, 'new BlockHospitalWayfindingPanel()',
          ("Hospital Wayfinding Panel", "Krankenhaus-Wegweisertafel",
           "Panel de señalización hospitalaria", "Vägvisningsskylt (sjukhus)"),
          models, {"multipart": rules}, item={"parent": MI(reg + "_item")}, tab=PC)

    def lang(key, en, de, es, sv):
        C.add_lang("csm.healthcare.wayfinding." + key, (en, de, es, sv))
    lang("preset", "[Wayfinding] Sign: %s", "[Wegweiser] Schild: %s", "[Señalización] Letrero: %s",
         "[Vägvisning] Skylt: %s")
    lang("arrow", "[Wayfinding] Arrow: %s", "[Wegweiser] Pfeil: %s", "[Señalización] Flecha: %s",
         "[Vägvisning] Pil: %s")


wayfinding_panel()


# ==========================================================================================
# Code blue
# ==========================================================================================
CODE_BLUE = (28, 84, 176)
CODE_BLUE_DARK = (18, 52, 116)


@C.texture("code_blue_body")
def _code_blue_body():
    return hc.swatch(CODE_BLUE, 9101, grain=2)


def code_blue_face(lit):
    """The code blue station's front: CODE BLUE over a white push button, PUSH on it, and a
    lamp that lights while the code is active."""
    def f():
        img = fill(CODE_BLUE, size=32, grain=1, seed=9102)
        frame(img, 0, 0, 32, 32, shade(CODE_BLUE, 0.8))
        hc.stamp_legend(img, "CODE", 16, 3, (250, 250, 250), 5)
        hc.stamp_legend(img, "BLUE", 16, 10, (250, 250, 250), 5)
        rect(img, 8, 17, 24, 28, (236, 236, 232))
        frame(img, 8, 17, 24, 28, (170, 172, 176))
        hc.stamp_legend(img, "PUSH", 16, 20, CODE_BLUE_DARK, 4)
        lamp(img, 27.5, 4.5, 3, (255, 230, 120) if lit else (90, 70, 40))
        return img
    return f


tex("code_blue_front", code_blue_face(False))
tex("code_blue_front_on", code_blue_face(True))

BUTTON = [el([4, 3, 14], [12, 13, 16], "body", per={"north": "front"}, uv={"north": FULL})]
wall_block("code_blue_button",
           'new BlockCodeBlueButton("code_blue_button", %s)' % B(4, 3, 13, 12, 13, 16),
           ("Code Blue Button", "Code-Blue-Taster", "Pulsador de código azul",
            "Larmknapp för hjärtstopp (Code Blue)"),
           {"body": T("code_blue_body"), "front": T("code_blue_front")},
           BUTTON, CL, extra={"active": {"false": {}, "true": {"textures": {
               "front": T("code_blue_front_on")}}}})


@C.texture("code_blue_lens_off")
def _code_blue_lens_off():
    img = fill((50, 96, 190), grain=1, seed=9110)
    for y in range(1, 16, 3):
        rect(img, 0, y, 16, y + 1, (70, 120, 210))
    return img


def strobe_frame(i):
    """The strobe's flash: dark but for two bright frames in twenty, once a second."""
    def f():
        if i in (0, 1, 2):
            img = fill((200, 226, 255), grain=1, seed=9111)
            for y in range(1, 16, 3):
                rect(img, 0, y, 16, y + 1, (240, 248, 255))
            if i == 2:
                img = fill((120, 170, 250), grain=1, seed=9112)
            return img
        return _code_blue_lens_off()
    return f


hc.animation(C, "code_blue_lens_flash", [strobe_frame(i) for i in range(20)], 1)


@C.texture("code_blue_label")
def _code_blue_label():
    img = fill((240, 240, 236), size=32, grain=1, seed=9113)
    rect(img, 0, 0, 32, 32, CODE_BLUE)
    hc.stamp_legend(img, "CODE BLUE", 16, 12, (250, 250, 250), 7, max_w=30)
    return img


@C.texture("speaker_grille")
def _speaker_grille():
    img = fill((226, 228, 226), grain=1, seed=9114)
    for y in range(1, 16, 2):
        for x in range(1, 16, 2):
            rect(img, x, y, x + 1, y + 1, (120, 122, 126))
    return img


ANNUNCIATOR = ([el([3, 1, 14], [13, 15, 16], "body"),
                front([4, 2, 13.6], [12, 6, 14], "body", "label", faces=("north", "east",
                                                                         "west", "up", "down")),
                front([4.5, 7, 13.6], [11.5, 9.5, 14], "body", "grille",
                      faces=("north", "east", "west", "up", "down"))]
               + hc.pipe_z(8, 12, 1.9, 12.2, 14, "lens"))
wall_block("code_blue_annunciator",
           'new BlockCodeBlueAnnunciator("code_blue_annunciator", %s)' % B(3, 1, 12, 13, 15, 16),
           ("Code Blue Annunciator (Strobe and Chime)", "Code-Blue-Melder (Blitz und Gong)",
            "Anunciador de código azul (estroboscópico y carillón)",
            "Code Blue-larmdon (blixt och klocka)"),
           {"body": W("white"), "label": T("code_blue_label"), "grille": T("speaker_grille"),
            "lens": T("code_blue_lens_off")},
           ANNUNCIATOR, CL, extra={"active": {"false": {}, "true": {"textures": {
               "lens": T("code_blue_lens_flash")}}}})


def cb_lang(key, en, de, es, sv):
    C.add_lang("csm.healthcare.codeblue." + key, (en, de, es, sv))


cb_lang("activated", "[Code Blue] Activated at %s.", "[Code Blue] Ausgelöst bei %s.",
        "[Código azul] Activado en %s.", "[Code Blue] Utlöst vid %s.")
cb_lang("reset", "[Code Blue] Reset (%s station(s) cleared).",
        "[Code Blue] Zurückgesetzt (%s Taster gelöscht).",
        "[Código azul] Restablecido (%s pulsador(es) borrado(s)).",
        "[Code Blue] Återställt (%s knapp(ar) kvitterade).")
cb_lang("powered", "[Code Blue] Still held on by redstone.",
        "[Code Blue] Wird noch durch Redstone gehalten.",
        "[Código azul] Sigue activo por redstone.", "[Code Blue] Hålls fortfarande av redstone.")

# ==========================================================================================
# Medical gas: the zone valve box and the master alarm panel
# ==========================================================================================
GAS = (("O2", hc.O2_GREEN, (250, 250, 250)), ("AIR", hc.AIR_YELLOW, (30, 30, 30)),
       ("VAC", hc.VAC_WHITE, (30, 30, 30)))


@C.texture("valve_box_back")
def _valve_box_back():
    """The box's back panel: each gas's label band over its riser."""
    img = fill((214, 216, 214), size=32, grain=1, seed=9201)
    for i, (name, bg, fg) in enumerate(GAS):
        x0 = 1 + i * 10
        rect(img, x0, 2, x0 + 10, 7, bg)
        frame(img, x0, 2, x0 + 10, 7, shade(bg, 0.7))
        draw_text_centred(img, name, x0 + 5, 2, fg)
    return img


@C.texture("valve_box_glass")
def _valve_box_glass():
    """The cover's window: clear, a couple of glints, and the instruction along the bottom."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for k in range(5):
        for t in range(10):
            x, y = 3 + t + k * 0, 3 + t + k
            if 0 <= x < 32 and 0 <= y < 32 and k in (0, 3):
                px[x + 14, y] = (250, 252, 255, 110)
    rect(img, 0, 25, 32, 32, (200, 30, 30))
    draw_text_centred(img, "SHUTOFF", 16, 26, (250, 250, 250))
    frame(img, 0, 0, 32, 32, (190, 192, 196))
    return img


@C.texture("gas_gauge")
def _gas_gauge():
    """A pressure gauge's round dial, clear outside its steel rim (a cutout on a flat face):
    the scale's ticks, a red band at the low end and the needle at the working pressure."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            d = math.hypot(dx, dy)
            if d >= 8:
                continue
            a = math.degrees(math.atan2(-dy, dx))
            c = (246, 246, 242)
            if d >= 6.8:
                c = (150, 154, 158)
            elif 5.2 <= d < 6.6 and (a > -45 or a < -135) and int(a + 360) % 30 < 9:
                c = (40, 40, 44)
            elif 5.2 <= d < 6.6 and -135 <= a < -110:
                c = (200, 40, 40)
            px[x, y] = c + (255,)
    for t in range(6):
        px[8 + t // 2, 8 - t] = (20, 20, 24, 255)   # the needle, at the working pressure
    rect(img, 7, 7, 9, 9, (40, 40, 44))
    return img


tex("copper", lambda: hc.swatch((184, 112, 72), 9203, grain=3))

VALVE = [el([1, 1, 14.5], [15, 15, 16], "frame", faces=("east", "west", "up", "down")),
         front([1.5, 1.5, 15.8], [14.5, 14.5, 16], "frame", "back", faces=("north",)),
         # the cover's frame and its window
         el([0.5, 0.5, 13], [15.5, 1.5, 14.5], "frame"), el([0.5, 14.5, 13], [15.5, 15.5, 14.5],
                                                            "frame"),
         el([0.5, 1.5, 13], [1.5, 14.5, 14.5], "frame"), el([14.5, 1.5, 13], [15.5, 14.5, 14.5],
                                                             "frame"),
         el([1.5, 1.5, 13.2], [14.5, 14.5, 13.2], "glass", faces=("north", "south"),
            uv={"north": FULL, "south": FULL})]
for i, (name, colour, _fg) in enumerate(GAS):
    cx = 13.25 - i * 5.0     # north face mirrors x: the first gas on the reader's left
    VALVE += hc.pipe_z(cx, 6.5, 0.9, 14.6, 15.8, "copper", front=False)
    VALVE += [el([cx - 0.6, 3, 15], [cx + 0.6, 12, 15.8], "copper")]
    VALVE += hc.pipe_z(cx, 10.4, 1.4, 14.6, 15.4, "steel", front=False)
    VALVE += [el([cx - 1.6, 8.8, 14.5], [cx + 1.6, 12, 14.5], "gauge", faces=("north",),
                 uv={"north": FULL})]
    VALVE += [el([cx - 0.5, 5.6, 13.9], [cx + 0.5, 7.4, 15], "steel"),
              el([cx - 2.2, 6.1, 13.7], [cx + 0.4, 6.9, 14.3], "h%d" % i)]
VALVE_TEX = {"frame": W("stainless"), "back": T("valve_box_back"), "glass": T("valve_box_glass"),
             "copper": T("copper"), "gauge": T("gas_gauge"), "steel": W("stainless"),
             "h0": W("gas_o2"), "h1": W("yellow"), "h2": W("white"), "particle": W("stainless")}
wall_block("medical_gas_zone_valve_box",
           java_for("BlockHealthcare", "medical_gas_zone_valve_box", (0, 0, 13, 16, 16, 16),
                    "METAL"),
           ("Medical Gas Zone Valve Box", "Medizingas-Zonenabsperrkasten",
            "Caja de válvulas de zona de gases medicinales", "Medicinsk gas, zonventilskåp"),
           VALVE_TEX, VALVE, CL)


def gas_alarm_face(alarm, muted, flash):
    """The master alarm's front: the title, and per gas a NORMAL lamp and a LOW/HIGH lamp; in
    alarm the oxygen line's LOW lamp is lit (flashing) and its NORMAL lamp dark; the amber lamp
    by the silence key shows the buzzer is silenced."""
    def f():
        img = fill((232, 232, 228), size=32, grain=1, seed=9210)
        rect(img, 0, 0, 32, 7, (60, 64, 70))
        hc.stamp_legend(img, "MED GAS", 16, 1, (250, 250, 250), 5, series="D", max_w=30)
        for i, (name, colour, fg) in enumerate(GAS):
            y = 8 + i * 6
            rect(img, 1, y, 13, y + 5, colour)
            frame(img, 1, y, 13, y + 5, shade(colour, 0.6))
            draw_text_centred(img, name, 7, y, fg)
            fault = alarm and i == 0
            lamp(img, 18, y + 2.5, 3, (40, 200, 70) if not fault else (30, 70, 40))
            lamp(img, 25, y + 2.5, 3, (250, 40, 30) if fault and flash else (110, 40, 36))
        rect(img, 2, 27, 12, 31, (70, 74, 80))
        lamp(img, 28.5, 29, 3, (250, 190, 40) if muted else (110, 80, 30))
        return img
    return f


tex("gas_alarm_normal", gas_alarm_face(False, False, False))
tex("gas_alarm_normal_muted", gas_alarm_face(False, True, False))
hc.animation(C, "gas_alarm_alarm", [gas_alarm_face(True, False, i == 0) for i in range(2)], 8)
hc.animation(C, "gas_alarm_alarm_muted", [gas_alarm_face(True, True, i == 0) for i in range(2)],
             8)

ALARM_PANEL = [el([2, 2, 14.5], [14, 14, 16], "body", per={"north": "face"},
                  uv={"north": FULL})]
_alarm_states = {"0": {}, "1": {"textures": {"face": T("gas_alarm_alarm")}},
                 "2": {"textures": {"face": T("gas_alarm_normal_muted")}},
                 "3": {"textures": {"face": T("gas_alarm_alarm_muted")}}}
wall_block("medical_gas_master_alarm",
           'new BlockMedicalGasAlarm("medical_gas_master_alarm", %s)' % B(2, 2, 14, 14, 14, 16),
           ("Medical Gas Master Alarm Panel", "Medizingas-Zentralalarmtafel",
            "Panel de alarma maestra de gases medicinales", "Medicinsk gas, huvudlarmtablå"),
           {"body": W("frame_grey"), "face": T("gas_alarm_normal")}, ALARM_PANEL, CL,
           extra={"state": _alarm_states})
C.add_lang("csm.healthcare.gasalarm.muted",
           ("[Medical Gas] Buzzer silenced.", "[Medizingas] Summer stummgeschaltet.",
            "[Gases medicinales] Zumbador silenciado.", "[Medicinsk gas] Summern tystad."))
C.add_lang("csm.healthcare.gasalarm.unmuted",
           ("[Medical Gas] Buzzer on.", "[Medizingas] Summer an.",
            "[Gases medicinales] Zumbador activado.", "[Medicinsk gas] Summern på."))

# ==========================================================================================
# Power outlets: normal (white) and emergency power (red)
# ==========================================================================================


def outlet_face(plate, body):
    """A duplex receptacle on its plate: two faces, each with two slots and a ground hole."""
    def f():
        img = fill(plate, grain=1, seed=9301 + plate[0])
        frame(img, 0, 0, 16, 16, shade(plate, 0.82))
        for y0 in (2, 9):
            rect(img, 4, y0, 12, y0 + 5, body)
            frame(img, 4, y0, 12, y0 + 5, shade(body, 0.85))
            rect(img, 5, y0 + 1, 6, y0 + 3, (30, 30, 30))
            rect(img, 10, y0 + 1, 11, y0 + 3, (30, 30, 30))
            rect(img, 7, y0 + 3, 9, y0 + 4, (30, 30, 30))
        rect(img, 7, 7, 9, 8, shade(plate, 0.6))   # the plate screw
        return img
    return f


tex("outlet_white", outlet_face((236, 234, 226), (242, 240, 232)))
tex("outlet_red", outlet_face((196, 36, 32), (214, 44, 38)))


for reg, key, names in (
        ("hospital_outlet_normal", "white",
         ("Power Outlet (Normal Power)", "Steckdose (Normalnetz)",
          "Toma de corriente (red normal)", "Eluttag (normalnät)")),
        ("hospital_outlet_emergency", "red",
         ("Power Outlet (Emergency Power, Red)", "Steckdose (Notstrom, rot)",
          "Toma de corriente (red de emergencia, roja)", "Eluttag (reservkraft, rött)"))):
    wall_block(reg, java_for("BlockHealthcare", reg, (5, 4, 15, 11, 12, 16), "PLASTIC"), names,
               {key: W("white") if key == "white" else W("red"),
                key + "_face": T("outlet_" + key)},
               [el([5, 4, 15.4], [11, 12, 16], key, per={"north": key + "_face"},
                   uv={"north": FULL})], CL)

wall_block("hospital_outlet_pair",
           java_for("BlockHealthcare", "hospital_outlet_pair", (2, 4, 15, 14, 12, 16), "PLASTIC"),
           ("Power Outlets (Normal and Emergency)", "Steckdosen (Normal- und Notstrom)",
            "Tomas de corriente (normal y de emergencia)", "Eluttag (normal- och reservkraft)"),
           {"white": W("white"), "white_face": T("outlet_white"), "red": W("red"),
            "red_face": T("outlet_red")},
           [el([8.3, 4, 15.4], [14, 12, 16], "white", per={"north": "white_face"},
               uv={"north": FULL}),
            el([2, 4, 15.4], [7.7, 12, 16], "red", per={"north": "red_face"},
               uv={"north": FULL})], CL)

# ==========================================================================================
# The infant security door panel
# ==========================================================================================


def infant_face(alarm, lit):
    """The panel's front: a badge reader, a keypad and a status band, SECURE in green, or ALARM
    in red flashing."""
    def f():
        img = fill((52, 56, 62), size=32, grain=1, seed=9401)
        frame(img, 0, 0, 32, 32, (36, 38, 42))
        band = (200, 30, 30) if alarm and lit else (90, 24, 24) if alarm else (30, 150, 70)
        rect(img, 3, 3, 29, 10, band)
        hc.stamp_legend(img, "ALARM" if alarm else "SECURE", 16, 4, (250, 250, 250), 5,
                        max_w=24)
        rect(img, 4, 13, 14, 23, (34, 36, 40))
        hc.emblem(img, 9, 18, 3.4, (120, 170, 220))
        for r in range(3):
            for c in range(3):
                rect(img, 17 + c * 4, 13 + r * 4, 20 + c * 4, 16 + r * 4, (200, 202, 206))
        lamp(img, 9, 27, 3, (250, 60, 40) if alarm and lit else (60, 200, 90) if not alarm
             else (90, 40, 30))
        return img
    return f


tex("infant_panel_secure", infant_face(False, False))
hc.animation(C, "infant_panel_alarm", [infant_face(True, i == 0) for i in range(2)], 6)
wall_block("infant_security_panel",
           'new BlockInfantSecurityPanel("infant_security_panel", %s)' % B(4, 3, 14, 12, 13, 16),
           ("Infant Security Door Panel", "Säuglingsschutz-Türterminal",
            "Panel de seguridad infantil de puerta", "Spädbarnslarm, dörrpanel"),
           {"body": W("dark_grey"), "face": T("infant_panel_secure")},
           [el([4, 3, 14.5], [12, 13, 16], "body", per={"north": "face"}, uv={"north": FULL})],
           CL, extra={"alarm": {"false": {}, "true": {"textures": {
               "face": T("infant_panel_alarm")}}}})
C.add_lang("csm.healthcare.infant.reset",
           ("[Infant Security] Alarm reset.", "[Säuglingsschutz] Alarm zurückgesetzt.",
            "[Seguridad infantil] Alarma restablecida.", "[Spädbarnslarm] Larmet återställt."))
C.add_lang("csm.healthcare.infant.held",
           ("[Infant Security] The tag alarm is still active.",
            "[Säuglingsschutz] Der Tag-Alarm ist noch aktiv.",
            "[Seguridad infantil] La alarma de la etiqueta sigue activa.",
            "[Spädbarnslarm] Brickans larm är fortfarande aktivt."))
C.add_lang("csm.healthcare.infant.secure",
           ("[Infant Security] Secure; no alarm.", "[Säuglingsschutz] Gesichert, kein Alarm.",
            "[Seguridad infantil] Seguro; sin alarma.", "[Spädbarnslarm] Säkrat; inget larm."))


# ==========================================================================================
# Writing
# ==========================================================================================
def _write_java(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        fh.write(text)


def _same_java(path, text):
    if not os.path.exists(path):
        return False
    with open(path, encoding="utf-8") as fh:
        return fh.read().replace("\r\n", "\n") == text


def main():
    check = "--check" in sys.argv
    frag = "--fragments" in sys.argv
    rc = C.main()
    if frag:
        return rc
    text = glyphs_java()
    if check:
        if not _same_java(JAVA_GLYPHS, text):
            print("out of date (re-run without --check):\n  " + JAVA_GLYPHS)
            return 1
        return rc
    _write_java(JAVA_GLYPHS, text)
    if rc == 0:
        written = []
        for root, _dirs, files in os.walk(hc.ASSETS):
            written += [os.path.join(root, f) for f in files]
        hc.check_json(written)
    return rc


if __name__ == "__main__":
    sys.exit(main())
