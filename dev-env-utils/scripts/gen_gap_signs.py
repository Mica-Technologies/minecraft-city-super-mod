#!/usr/bin/env python3
"""Generate the signs that fill the gaps found by the 2026-09 catalogue review.

One catalogue, one command. Each entry names a sign, the shape of plate it goes on, how its
face is drawn (Highway Gothic text through render_sign, or a symbol drawn here), the plain
sign it sits beside in the creative tab, and its display name in the four shipped languages.
From that the script writes:

    textures/blocks/trafficsigns/<registry>.png          128 x 128, drawn at the plate's aspect
    textures/blocks/trafficsigns/<registry>_back.png     silhouette signs only: the gray back
    blockstates/<registry>.json                          the shape sibling's, retextured

and with --apply also inserts the lang line into every language file and the tab line after
its sibling in CsmTabRoadSigns. Without --apply the lang and tab lines are printed.

Plates and aspects. A sign texture is square but is stretched onto its model's plate, so the
face is drawn at the plate's aspect and then squished to 128 x 128; in the world it comes back
out at the right proportions. The shapes and the sibling whose blockstate is cloned for each:

    diamond    23 x 23  signbump            warning diamonds
    portrait   16 x 21  signspeed65         speed limits and other tall regulatory signs
    square     16 x 16  signnouturn         square regulatory signs and 24" plaques
    wide       22 x 16  signwrongway        landscape regulatory signs
    plaque     16 x 8   signaheadplaque     supplemental plaques
    silhouette 23 x 23  yieldsign           anything whose outline is none of the above: the
                                            yield model draws the texture's own alpha on a bare
                                            plate, with a gray back texture on slot "2"

Run from the repo root:  python dev-env-utils/scripts/gen_gap_signs.py [--apply] [--check]
"""

import json
import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import render_sign as rs  # noqa: E402

SS = rs.SS
SIZE = rs.SIZE  # supersampled square canvas
OWNER = layout.owner_of_folder('trafficsigns')
TEX_DIR = layout.asset_dir_for_write(OWNER, 'textures/blocks/trafficsigns')
BS_DIR = layout.asset_dir_for_write(OWNER, 'blockstates')
LANG_DIR = layout.asset_dir_for_write(OWNER, 'lang')
TAB = layout.resolve_source('tabs/CsmTabRoadSigns.java')

YELLOW = (252, 209, 22, 255)
WHITE = (245, 245, 245, 255)
BLACK = (20, 20, 20, 255)
RED = (196, 30, 38, 255)
BACK_GRAY = (150, 150, 150, 255)
BACK_EDGE = (110, 110, 110, 255)

SHAPES = {
    'diamond': ('signbump', 1.0),
    'portrait': ('signspeed65', 16 / 21),
    'square': ('signnouturn', 1.0),
    'wide': ('signwrongway', 22 / 16),
    'plaque': ('signaheadplaque', 2.0),
    'silhouette': ('yieldsign', 1.0),
    'circle': ('signfdcstandpipe', 1.0),
}
LANGS = ('en_us', 'es_es', 'de_de', 'sv_se')


# ----------------------------------------------------------------------------- canvases

def _canvas(aspect):
    """A transparent supersampled canvas at the plate's aspect (width = aspect * height)."""
    w = int(round(SIZE * aspect)) if aspect >= 1 else SIZE
    h = SIZE if aspect >= 1 else int(round(SIZE / aspect))
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def _finish(img):
    """Squish the aspect canvas to the square texture the plate stretches back out."""
    return img.resize((128, 128), Image.LANCZOS)


def _rounded_panel(img, bg, fg, radius=7):
    """Rounded panel filling the canvas with the thin inset border every rendered sign has."""
    d = ImageDraw.Draw(img)
    m, inset, stroke = 3 * SS, 2 * SS, 3 * SS
    box = (m, m, img.width - m, img.height - m)
    d.rounded_rectangle(box, radius=radius * SS, fill=bg)
    d.rounded_rectangle((box[0] + inset, box[1] + inset, box[2] - inset, box[3] - inset),
                        radius=max(2, radius * SS - inset), outline=fg, width=stroke)
    pad = inset + stroke + 5 * SS
    return d, (box[0] + pad, box[1] + pad, box[2] - pad, box[3] - pad)


def _diamond_panel(img, bg, fg):
    """The warning diamond on a square canvas, as render_sign draws it."""
    m, inset, stroke = 3 * SS, 2 * SS, 3 * SS
    side = int((SIZE - 2 * m) / math.sqrt(2))
    sq = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sq)
    r = int(8 * SS)
    sd.rounded_rectangle((0, 0, side - 1, side - 1), radius=r, fill=bg)
    sd.rounded_rectangle((inset, inset, side - 1 - inset, side - 1 - inset),
                         radius=max(2, r - inset), outline=fg, width=stroke)
    sq = sq.rotate(45, expand=True, resample=Image.BICUBIC)
    img.alpha_composite(sq, ((SIZE - sq.width) // 2, (SIZE - sq.height) // 2))
    return ImageDraw.Draw(img)


def text_sign(shape, lines, bg, fg):
    """A text-only sign on the given plate shape."""
    aspect = SHAPES[shape][1]
    if shape == 'diamond':
        img = _canvas(1.0)
        _diamond_panel(img, bg, fg)
        R = (SIZE / 2 - 3 * SS) - (2 * SS + 3 * SS) - 4 * SS
        rs._draw_text_diamond(img, lines, fg, SIZE / 2, SIZE / 2, R)
        return _finish(img)
    img = _canvas(aspect)
    _d, box = _rounded_panel(img, bg, fg)
    rs._draw_text(img, lines, fg, box)
    return _finish(img)


# ----------------------------------------------------------------------------- symbols
# Coordinates are fractions of the canvas; every stroke is drawn at 4x and comes out smooth.

def _P(img, fx, fy):
    return (fx * img.width, fy * img.height)


def _stroke(d, img, pts, width, color=BLACK):
    d.line([_P(img, x, y) for x, y in pts], fill=color, width=int(width * SIZE), joint='curve')


def _arrowhead(d, img, tip, direction, size, color=BLACK):
    """Triangular head with its tip at ``tip``; direction is a unit (dx, dy)."""
    tx, ty = _P(img, *tip)
    dx, dy = direction
    L = size * SIZE
    bx, by = tx - dx * L, ty - dy * L
    px, py = -dy, dx
    hw = L * 0.6
    d.polygon([(tx, ty), (bx + px * hw, by + py * hw), (bx - px * hw, by - py * hw)],
              fill=color)


def _bezier(p0, p1, p2, p3, n=40):
    out = []
    for i in range(n + 1):
        t = i / n
        u = 1 - t
        out.append((u**3 * p0[0] + 3 * u**2 * t * p1[0] + 3 * u * t**2 * p2[0] + t**3 * p3[0],
                    u**3 * p0[1] + 3 * u**2 * t * p1[1] + 3 * u * t**2 * p2[1] + t**3 * p3[1]))
    return out


def sym_reverse_curve(img, right, sharp):
    """W1-4 (curve) / W1-3 (turn): the road bends one way then back, ending offset. Drawn
    bottom to top with the head pointing up."""
    d = ImageDraw.Draw(img)
    s = 1 if right else -1
    x0, x1 = 0.5 - s * 0.09, 0.5 + s * 0.09
    if sharp:
        pts = [(x0, 0.74), (x0, 0.56), (x1, 0.46), (x1, 0.30)]
    else:
        pts = _bezier((x0, 0.74), (x0, 0.52), (x1, 0.52), (x1, 0.30))
    _stroke(d, img, pts, 0.055)
    _arrowhead(d, img, (x1, 0.24), (0, -1), 0.10)


def sym_road_narrows(img):
    """W5-1: the road's two edges converging ahead."""
    d = ImageDraw.Draw(img)
    for s in (-1, 1):
        _stroke(d, img, [(0.5 + s * 0.20, 0.74), (0.5 + s * 0.20, 0.56), (0.5 + s * 0.09, 0.40),
                         (0.5 + s * 0.09, 0.26)], 0.05)


def sym_falling_rocks(img):
    """W8-14: a cliff on the right shedding rocks onto the road."""
    d = ImageDraw.Draw(img)
    d.polygon([_P(img, 0.62, 0.26), _P(img, 0.74, 0.26), _P(img, 0.74, 0.74),
               _P(img, 0.64, 0.74), _P(img, 0.60, 0.50)], fill=BLACK)
    for cx, cy, r in ((0.50, 0.36, 0.03), (0.44, 0.47, 0.025), (0.52, 0.55, 0.035),
                      (0.40, 0.62, 0.03), (0.50, 0.70, 0.028), (0.34, 0.72, 0.022)):
        x, y = _P(img, cx, cy)
        d.ellipse((x - r * SIZE, y - r * SIZE, x + r * SIZE, y + r * SIZE), fill=BLACK)


def sym_horse(img):
    """W11-7: horse and rider, in profile, walking left."""
    d = ImageDraw.Draw(img)
    P = lambda x, y: _P(img, x, y)
    # body
    d.ellipse((*P(0.34, 0.44), *P(0.68, 0.62)), fill=BLACK)
    # neck: a thick stroke rising from the shoulder; head: an angled muzzle, ears up
    _stroke(d, img, [(0.42, 0.50), (0.31, 0.34)], 0.075)
    d.polygon([P(0.19, 0.40), P(0.29, 0.30), P(0.35, 0.36), P(0.24, 0.45)], fill=BLACK)
    d.polygon([P(0.29, 0.31), P(0.31, 0.23), P(0.35, 0.31)], fill=BLACK)
    # legs
    for x0, x1, y1 in ((0.40, 0.37, 0.76), (0.45, 0.47, 0.76), (0.58, 0.55, 0.76),
                       (0.64, 0.67, 0.76)):
        _stroke(d, img, [(x0, 0.58), (x1, y1)], 0.045)
    # tail
    _stroke(d, img, [(0.67, 0.48), (0.74, 0.62)], 0.035)
    # rider
    x, y = P(0.51, 0.27)
    d.ellipse((x - 0.045 * SIZE, y - 0.045 * SIZE, x + 0.045 * SIZE, y + 0.045 * SIZE),
              fill=BLACK)
    d.polygon([P(0.47, 0.32), P(0.55, 0.32), P(0.58, 0.50), P(0.45, 0.50)], fill=BLACK)
    _stroke(d, img, [(0.47, 0.48), (0.43, 0.62)], 0.035)


def sym_keep_side(img, keep_right):
    """R4-7 / R4-8: an obstruction bar, and an arrow that starts below it and passes it on the
    side to keep to."""
    d = ImageDraw.Draw(img)
    s = 1 if keep_right else -1
    bx = 0.5 - s * 0.13
    d.rounded_rectangle((*_P(img, bx - 0.06, 0.20), *_P(img, bx + 0.06, 0.60)),
                        radius=int(0.025 * SIZE), fill=BLACK)
    ax0, ax1 = 0.5 - s * 0.04, 0.5 + s * 0.16
    pts = [(ax0, 0.86)] + _bezier((ax0, 0.76), (ax0, 0.56), (ax1, 0.66), (ax1, 0.46)) \
        + [(ax1, 0.28)]
    _stroke(d, img, pts, 0.065)
    _arrowhead(d, img, (ax1, 0.16), (0, -1), 0.12)


def sym_no_left_or_uturn(img):
    """R3-18: a left-turn arrow and a U-turn arrow under one prohibition circle."""
    d = ImageDraw.Draw(img)
    # left turn: up then left
    _stroke(d, img, [(0.40, 0.78), (0.40, 0.46), (0.30, 0.46)], 0.06)
    _arrowhead(d, img, (0.16, 0.46), (-1, 0), 0.11)
    # u-turn: up, over the top, down
    pts = [(0.54, 0.78), (0.54, 0.40)] + _bezier((0.54, 0.40), (0.54, 0.20), (0.78, 0.20),
                                                 (0.78, 0.40)) + [(0.78, 0.50)]
    _stroke(d, img, pts, 0.06)
    _arrowhead(d, img, (0.78, 0.64), (0, 1), 0.11)
    # prohibition ring and slash
    r = 0.38 * SIZE
    cx, cy = _P(img, 0.5, 0.5)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=RED, width=int(0.06 * SIZE))
    k = r / math.sqrt(2)
    d.line([(cx - k, cy - k), (cx + k, cy + k)], fill=RED, width=int(0.06 * SIZE))


def sym_roundabout_arrow(img):
    """R6-4: the circulating arrow, counter-clockwise, on a wide panel."""
    d = ImageDraw.Draw(img)
    cx, cy = img.width * 0.5, img.height * 0.5
    r = img.height * 0.30
    d.arc((cx - r, cy - r, cx + r, cy + r), start=40, end=290, fill=BLACK,
          width=int(0.07 * SIZE))
    # head at the end of the arc (290 degrees), pointing the way the arc was travelling:
    # PIL's angles increase clockwise on screen, so the tangent is (-sin a, cos a)
    a = math.radians(290)
    tip = ((cx + r * math.cos(a)) / img.width, (cy + r * math.sin(a)) / img.height)
    _arrowhead(d, img, tip, (-math.sin(a), math.cos(a)), 0.13)


# ----------------------------------------------------------------------------- catalogue
# registry, display (en, es, de, sv), shape, drawer, tab sibling (insert after)

def T(shape, lines, bg=YELLOW, fg=BLACK):
    return lambda: text_sign(shape, lines, bg, fg)


def D(shape, sym, bg=YELLOW, fg=BLACK, **kw):
    """A symbol sign: the panel for the shape, then the drawer on top."""
    def make():
        aspect = SHAPES[shape][1]
        if shape == 'diamond':
            img = _canvas(1.0)
            _diamond_panel(img, bg, fg)
        else:
            img = _canvas(aspect)
            _rounded_panel(img, bg, fg)
        sym(img, **kw)
        return _finish(img)
    return make


FYG = (186, 255, 41, 255)  # fluorescent yellow-green, as the mod's pedestrian diamond


def _walker(d, img, cx, cy, h, color=BLACK, stride=1.0):
    """The MUTCD walking figure, ``h`` tall (fraction of canvas height), centred on (cx, cy)."""
    P = lambda x, y: _P(img, x, y)
    top = cy - h / 2
    r = h * 0.11
    hx, hy = P(cx, top + r)
    d.ellipse((hx - r * SIZE, hy - r * SIZE, hx + r * SIZE, hy + r * SIZE), fill=color)
    torso_top, torso_bot = top + 2.3 * r, top + h * 0.56
    d.polygon([P(cx - h * 0.09, torso_top), P(cx + h * 0.09, torso_top),
               P(cx + h * 0.07, torso_bot), P(cx - h * 0.07, torso_bot)], fill=color)
    # arms swung, legs mid-stride
    w = h * 0.045
    _stroke(d, img, [(cx + h * 0.05, torso_top + h * 0.04), (cx + h * 0.20 * stride, torso_top + h * 0.22)], w, color)
    _stroke(d, img, [(cx - h * 0.05, torso_top + h * 0.04), (cx - h * 0.16 * stride, torso_top + h * 0.20)], w, color)
    _stroke(d, img, [(cx - h * 0.03, torso_bot), (cx - h * 0.17 * stride, top + h)], w * 1.3, color)
    _stroke(d, img, [(cx + h * 0.03, torso_bot), (cx + h * 0.14 * stride, top + h * 0.98)], w * 1.3, color)


def sym_yield_here_to_peds(img):
    """R1-5: YIELD HERE TO, the walking figure, and the arrow pointing down at the line."""
    d = ImageDraw.Draw(img)
    rs._draw_text(img, ['YIELD', 'HERE TO'], BLACK,
                  (img.width * 0.12, img.height * 0.08, img.width * 0.88, img.height * 0.38))
    _walker(d, img, 0.42, 0.60, 0.34)
    _stroke(d, img, [(0.72, 0.46), (0.72, 0.66)], 0.05)
    _arrowhead(d, img, (0.72, 0.78), (0, 1), 0.10)


def sym_state_law_paddle(img):
    """R1-6: STATE LAW / STOP FOR [figure] / WITHIN CROSSWALK, on the in-street paddle."""
    d = ImageDraw.Draw(img)
    W, H = img.width, img.height
    rs._draw_text(img, ['STATE', 'LAW'], BLACK, (W * 0.10, H * 0.06, W * 0.90, H * 0.26))
    # the STOP legend in its own red octagon
    cx, cy, r = W * 0.30, H * 0.44, H * 0.11
    pts = [(cx + r * math.cos(math.radians(22.5 + 45 * i)),
            cy + r * math.sin(math.radians(22.5 + 45 * i))) for i in range(8)]
    d.polygon(pts, fill=RED)
    rs._draw_text(img, ['STOP'], WHITE, (cx - r * 0.8, cy - r * 0.45, cx + r * 0.8, cy + r * 0.45))
    rs._draw_text(img, ['FOR'], BLACK, (W * 0.46, H * 0.36, W * 0.62, H * 0.52))
    _walker(d, img, 0.76, 0.44, 0.24)
    rs._draw_text(img, ['WITHIN', 'CROSSWALK'], BLACK, (W * 0.10, H * 0.62, W * 0.90, H * 0.90))


def pentagon(img, bg, fg):
    """The school-sign pentagon, point up, on a bare silhouette plate."""
    d = ImageDraw.Draw(img)
    m = 4 * SS
    poly = [(SIZE / 2, m), (SIZE - m, SIZE * 0.40), (SIZE - m, SIZE - m), (m, SIZE - m),
            (m, SIZE * 0.40)]
    d.polygon(poly, fill=bg)
    d.polygon(poly, outline=fg, width=3 * SS)
    return d


def school_crossing():
    """S1-1: two figures walking, on the fluorescent yellow-green pentagon."""
    img = _canvas(1.0)
    d = pentagon(img, FYG, BLACK)
    _walker(d, img, 0.40, 0.58, 0.46)
    _walker(d, img, 0.62, 0.64, 0.34)
    return _finish(img)


def school_bus_stop_ahead():
    """S3-1: the pentagon with the legend."""
    img = _canvas(1.0)
    pentagon(img, FYG, BLACK)
    # the box stays under the shoulders, where the pentagon is full width
    rs._draw_text(img, ['SCHOOL', 'BUS', 'STOP', 'AHEAD'], BLACK,
                  (SIZE * 0.24, SIZE * 0.42, SIZE * 0.76, SIZE * 0.90))
    return _finish(img)


def crossbuck():
    """R15-1: the two white arms crossed at 90 degrees, RAILROAD on one and CROSSING on the
    other, on a bare silhouette plate. Each arm is drawn upright with its legend and rotated
    into place, so the letters run along the arm as they do on the sign."""
    img = _canvas(1.0)
    arm_w, arm_h = int(SIZE * 1.20), int(SIZE * 0.17)
    # Each word is split around the crossing, as on the real sign, so neither arm's legend is
    # buried under the other where they cross
    for angle, (first, second) in ((45, ('RAIL', 'ROAD')), (-45, ('CROS', 'SING'))):
        arm = Image.new('RGBA', (arm_w, arm_h), (0, 0, 0, 0))
        d = ImageDraw.Draw(arm)
        d.rectangle((0, 0, arm_w - 1, arm_h - 1), fill=WHITE)
        d.rectangle((SS, SS, arm_w - 1 - SS, arm_h - 1 - SS), outline=BLACK, width=2 * SS)
        rs._draw_text(arm, [first], BLACK, (arm_h * 0.6, arm_h * 0.2, arm_w * 0.40, arm_h * 0.8))
        rs._draw_text(arm, [second], BLACK, (arm_w * 0.60, arm_h * 0.2, arm_w - arm_h * 0.6, arm_h * 0.8))
        arm = arm.rotate(angle, expand=True, resample=Image.BICUBIC)
        img.alpha_composite(arm, ((SIZE - arm.width) // 2, (SIZE - arm.height) // 2))
    # the corners of a 1.2-wide arm poke past the square: keep to the plate
    img = img.crop(((img.width - SIZE) // 2, (img.height - SIZE) // 2,
                    (img.width + SIZE) // 2, (img.height + SIZE) // 2))
    return _finish(img)


def rr_advance():
    """W10-1: the round yellow advance warning, a black X with R either side."""
    img = _canvas(1.0)
    d = ImageDraw.Draw(img)
    m = 3 * SS
    d.ellipse((m, m, SIZE - m, SIZE - m), fill=YELLOW)
    d.ellipse((m + 2 * SS, m + 2 * SS, SIZE - m - 2 * SS, SIZE - m - 2 * SS), outline=BLACK,
              width=3 * SS)
    w = 0.055
    _stroke(d, img, [(0.28, 0.28), (0.72, 0.72)], w)
    _stroke(d, img, [(0.72, 0.28), (0.28, 0.72)], w)
    rs._draw_text(img, ['R'], BLACK, (SIZE * 0.13, SIZE * 0.36, SIZE * 0.30, SIZE * 0.64))
    rs._draw_text(img, ['R'], BLACK, (SIZE * 0.70, SIZE * 0.36, SIZE * 0.87, SIZE * 0.64))
    return _finish(img)


def pennant_no_passing():
    """W14-3: the yellow pennant, point to the right, on a bare silhouette plate."""
    img = _canvas(1.0)
    d = ImageDraw.Draw(img)
    m = 4 * SS
    top, bot = SIZE * 0.20, SIZE * 0.80
    poly = [(m, top), (SIZE - m, SIZE / 2), (m, bot)]
    d.polygon(poly, fill=YELLOW)
    d.polygon(poly, outline=BLACK, width=3 * SS)
    rs._draw_text(img, ['NO', 'PASSING', 'ZONE'], BLACK,
                  (SIZE * 0.08, SIZE * 0.30, SIZE * 0.50, SIZE * 0.70))
    return _finish(img)


def gray_back(face):
    """The back of a silhouette sign: the face's outline in unpainted gray."""
    alpha = face.getchannel('A')
    back = Image.new('RGBA', face.size, BACK_GRAY)
    back.putalpha(alpha)
    return back.transpose(Image.FLIP_LEFT_RIGHT)


CATALOGUE = [
    # --- regulatory
    ('signpostkeepleft', ('Keep Left Sign', 'Señal de Mantenerse a la Izquierda',
                          'Links Halten Schild', 'Håll Vänster-Vägmärke'),
     'portrait', D('portrait', sym_keep_side, WHITE, BLACK, keep_right=False), 'signpostkeepright'),
    ('signspeed10', ('Speed Limit 10 Sign', 'Señal de Límite de Velocidad 10',
                     'Geschwindigkeitsbegrenzung 10 Schild', 'Hastighetsbegränsning 10-Vägmärke'),
     'portrait', T('portrait', ['SPEED', 'LIMIT', '10'], WHITE, BLACK), 'signspeed0'),
    ('signspeed60', ('Speed Limit 60 Sign', 'Señal de Límite de Velocidad 60',
                     'Geschwindigkeitsbegrenzung 60 Schild', 'Hastighetsbegränsning 60-Vägmärke'),
     'portrait', T('portrait', ['SPEED', 'LIMIT', '60'], WHITE, BLACK), 'signpostspeed55'),
    ('signspeed70', ('Speed Limit 70 Sign', 'Señal de Límite de Velocidad 70',
                     'Geschwindigkeitsbegrenzung 70 Schild', 'Hastighetsbegränsning 70-Vägmärke'),
     'portrait', T('portrait', ['SPEED', 'LIMIT', '70'], WHITE, BLACK), 'signspeed65'),
    ('signrightmustturnright', ('Right Lane Must Turn Right Sign',
                                'Señal de Carril Derecho Debe Girar a la Derecha',
                                'Rechte Spur Muss Rechts Abbiegen Schild',
                                'Höger Körfält Måste Svänga Höger-Vägmärke'),
     'square', T('square', ['RIGHT LANE', 'MUST', 'TURN RIGHT'], WHITE, BLACK), 'signrightahead'),
    ('signnoleftoruturn', ('No Left Turn or U-Turn Sign', 'Señal de Prohibido Girar a la Izquierda o en U',
                           'Kein Linksabbiegen oder Wenden Schild', 'Förbjuden Vänstersväng eller U-sväng-Vägmärke'),
     'square', D('square', sym_no_left_or_uturn, WHITE, BLACK), 'signnoleftturn'),
    ('signroundaboutdirectional', ('Roundabout Directional Arrow Sign', 'Señal de Flecha Direccional de Rotonda',
                                   'Kreisverkehr Richtungspfeil Schild', 'Rondell Riktningspil-Vägmärke'),
     'wide', D('wide', sym_roundabout_arrow, WHITE, BLACK), 'signr105a'),
    ('signroundaboutplaque', ('Roundabout Sign (Plaque)', 'Señal de Rotonda (Placa)',
                              'Kreisverkehr Schild (Zusatzschild)', 'Rondell-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['ROUNDABOUT'], WHITE, BLACK), 'signroundaboutdirectional'),
    # --- warning
    ('signbepreparedtostop', ('Be Prepared To Stop Sign', 'Señal de Prepárese para Detenerse',
                              'Bremsbereit Sein Schild', 'Var Beredd att Stanna-Vägmärke'),
     'diamond', T('diamond', ['BE', 'PREPARED', 'TO STOP']), 'basestationradiosign'),
    ('signfallingrocks', ('Falling Rocks Sign', 'Señal de Caída de Rocas',
                          'Steinschlag Schild', 'Stenras-Vägmärke'),
     'diamond', D('diamond', sym_falling_rocks), 'signexit25'),
    ('signhorse', ('Horse Crossing Sign', 'Señal de Cruce de Caballos',
                   'Reiter Schild', 'Ridande-Vägmärke'),
     'diamond', D('diamond', sym_horse), 'signhill'),
    ('signloosegravel', ('Loose Gravel Sign', 'Señal de Gravilla Suelta',
                         'Rollsplitt Schild', 'Löst Grus-Vägmärke'),
     'diamond', T('diamond', ['LOOSE', 'GRAVEL']), 'signleftrightarrow'),
    ('signreversecurveleft', ('Reverse Curve Left Sign', 'Señal de Curva Inversa a la Izquierda',
                              'Doppelkurve Links Schild', 'Dubbelkurva Vänster-Vägmärke'),
     'diamond', D('diamond', sym_reverse_curve, right=False, sharp=False), 'signrampsignalahead'),
    ('signreversecurveright', ('Reverse Curve Right Sign', 'Señal de Curva Inversa a la Derecha',
                               'Doppelkurve Rechts Schild', 'Dubbelkurva Höger-Vägmärke'),
     'diamond', D('diamond', sym_reverse_curve, right=True, sharp=False), 'signreversecurveleft'),
    ('signreverseturnleft', ('Reverse Turn Left Sign', 'Señal de Giro Inverso a la Izquierda',
                             'Doppelkurve Scharf Links Schild', 'Skarp Dubbelkurva Vänster-Vägmärke'),
     'diamond', D('diamond', sym_reverse_curve, right=False, sharp=True), 'signreversecurveright'),
    ('signreverseturnright', ('Reverse Turn Right Sign', 'Señal de Giro Inverso a la Derecha',
                              'Doppelkurve Scharf Rechts Schild', 'Skarp Dubbelkurva Höger-Vägmärke'),
     'diamond', D('diamond', sym_reverse_curve, right=True, sharp=True), 'signreverseturnleft'),
    ('signroadnarrows', ('Road Narrows Sign', 'Señal de Estrechamiento de Calzada',
                         'Fahrbahnverengung Schild', 'Avsmalnande Väg-Vägmärke'),
     'diamond', D('diamond', sym_road_narrows), 'signhightideroadflood'),
    ('signroughroad', ('Rough Road Sign', 'Señal de Calzada Irregular',
                       'Unebene Fahrbahn Schild', 'Ojämn Väg-Vägmärke'),
     'diamond', T('diamond', ['ROUGH', 'ROAD']), 'signroadsplit'),
    ('signrunawaytruckramp', ('Runaway Truck Ramp Sign', 'Señal de Rampa de Escape para Camiones',
                              'Notfallspur Schild', 'Nödficka för Lastbilar-Vägmärke'),
     'diamond', T('diamond', ['RUNAWAY', 'TRUCK', 'RAMP']), 'signroundabout'),
    ('signnopassingzone', ('No Passing Zone Sign', 'Señal de Zona de Prohibido Adelantar',
                           'Überholverbot Schild', 'Omkörningsförbud-Vägmärke'),
     'silhouette', pennant_no_passing, 'signnooutlet'),
    # --- plaques
    ('signcrosstrafficdoesnotstop', ('Cross Traffic Does Not Stop Sign (Plaque)',
                                     'Señal de Tráfico Transversal No Se Detiene (Placa)',
                                     'Querverkehr Hält Nicht Schild (Zusatzschild)',
                                     'Korsande Trafik Stannar Inte-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['CROSS TRAFFIC', 'DOES NOT STOP']), 'signcow'),
    ('sign500feet', ('500 Feet Sign (Plaque)', 'Señal de 500 Pies (Placa)',
                     '500 Fuß Schild (Zusatzschild)', '500 Fot-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['500 FEET']), 'sign4way'),
    ('sign1000feet', ('1000 Feet Sign (Plaque)', 'Señal de 1000 Pies (Placa)',
                      '1000 Fuß Schild (Zusatzschild)', '1000 Fot-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['1000 FEET']), 'sign500feet'),
    ('signnext2miles', ('Next 2 Miles Sign (Plaque)', 'Señal de Próximas 2 Millas (Placa)',
                        'Nächste 2 Meilen Schild (Zusatzschild)', 'Nästa 2 Miles-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['NEXT 2 MILES']), 'signnewsignal'),
    # --- pedestrian and school (Phase 2)
    ('signyieldheretopeds', ('Yield Here To Pedestrians Sign', 'Señal de Ceda el Paso Aquí a Peatones',
                             'Hier Fußgängern Vorfahrt Gewähren Schild', 'Lämna Företräde Här för Fotgängare-Vägmärke'),
     'portrait', D('portrait', sym_yield_here_to_peds, WHITE, BLACK), 'signusecrosswalkright'),
    ('signendschoolzone', ('End School Zone Sign', 'Señal de Fin de Zona Escolar',
                           'Ende Schulzone Schild', 'Slut på Skolzon-Vägmärke'),
     'square', T('square', ['END', 'SCHOOL', 'ZONE'], WHITE, BLACK), 'signyieldheretopeds'),
    ('signschoolbusstopahead', ('School Bus Stop Ahead Sign', 'Señal de Parada de Autobús Escolar Adelante',
                                'Schulbushaltestelle Voraus Schild', 'Skolbusshållplats Framför-Vägmärke'),
     'silhouette', school_bus_stop_ahead, 'signendschoolzone'),
    ('signschoolcrossing', ('School Crossing Sign', 'Señal de Cruce Escolar',
                            'Schulweg Schild', 'Skolövergång-Vägmärke'),
     'silhouette', school_crossing, 'signschoolbusstopahead'),
    # --- rail crossing (Phase 4)
    ('signrailroadcrossbuck', ('Railroad Crossing Sign (Crossbuck)', 'Señal de Cruce Ferroviario (Cruz de San Andrés)',
                               'Bahnübergang Andreaskreuz Schild', 'Järnvägskorsning Kryssmärke-Vägmärke'),
     'silhouette', crossbuck, 'signphotoenforced'),
    ('signrailroadtracks2', ('2 Tracks Sign (Plaque)', 'Señal de 2 Vías (Placa)',
                             '2 Gleise Schild (Zusatzschild)', '2 Spår-Vägmärke (Tilläggsskylt)'),
     'plaque', T('plaque', ['2 TRACKS'], WHITE, BLACK), 'signrailroadcrossbuck'),
    ('signdonotstopontracks', ('Do Not Stop On Tracks Sign', 'Señal de No Detenerse Sobre las Vías',
                               'Nicht auf den Gleisen Anhalten Schild', 'Stanna Inte på Spåret-Vägmärke'),
     'square', T('square', ['DO NOT', 'STOP ON', 'TRACKS'], WHITE, BLACK), 'signdonotpass'),
    ('signrailroadadvance', ('Railroad Crossing Advance Warning Sign', 'Señal de Advertencia Anticipada de Cruce Ferroviario',
                             'Bahnübergang Vorwarnung Schild', 'Järnvägskorsning Förvarning-Vägmärke'),
     'circle', rr_advance, 'signradioradiation'),
    # --- work zone (Phase 3): the lane-closure family, all legends
    ('signleftlaneclosedahead', ('Left Lane Closed Ahead Sign', 'Señal de Carril Izquierdo Cerrado Adelante',
                                 'Linke Spur Gesperrt Voraus Schild', 'Vänster Körfält Avstängt Framför-Vägmärke'),
     'diamond', T('diamond', ['LEFT LANE', 'CLOSED', 'AHEAD'], rs.ORANGE), 'landworkaheadsign'),
    ('signrightlaneclosedahead', ('Right Lane Closed Ahead Sign', 'Señal de Carril Derecho Cerrado Adelante',
                                  'Rechte Spur Gesperrt Voraus Schild', 'Höger Körfält Avstängt Framför-Vägmärke'),
     'diamond', T('diamond', ['RIGHT LANE', 'CLOSED', 'AHEAD'], rs.ORANGE), 'rgrchickensign'),
    ('signonelaneroadahead', ('One Lane Road Ahead Sign', 'Señal de Carretera de Un Carril Adelante',
                              'Einspurige Straße Voraus Schild', 'Enfilig Väg Framför-Vägmärke'),
     'diamond', T('diamond', ['ONE LANE', 'ROAD', 'AHEAD'], rs.ORANGE), 'noguardrailssignrr'),
    ('signutilityworkahead', ('Utility Work Ahead Sign', 'Señal de Trabajos de Servicios Adelante',
                              'Versorgungsarbeiten Voraus Schild', 'Ledningsarbete Framför-Vägmärke'),
     'diamond', T('diamond', ['UTILITY', 'WORK', 'AHEAD'], rs.ORANGE), 'signunmarkedpavement'),
    ('signshoulderwork', ('Shoulder Work Sign', 'Señal de Trabajos en el Arcén',
                          'Arbeiten am Seitenstreifen Schild', 'Vägrensarbete-Vägmärke'),
     'diamond', T('diamond', ['SHOULDER', 'WORK'], rs.ORANGE), 'signsignalworkahead'),
    ('signbridgeout', ('Bridge Out Sign', 'Señal de Puente Fuera de Servicio',
                       'Brücke Gesperrt Schild', 'Bro Avstängd-Vägmärke'),
     'wide', T('wide', ['BRIDGE', 'OUT'], WHITE, BLACK), 'signblastingzone'),
    ('signstatelawstopforpeds', ('State Law Stop For Pedestrians In Crosswalk Sign',
                                 'Señal de Ley Estatal Deténgase por Peatones en el Cruce',
                                 'Landesgesetz Für Fußgänger im Zebrastreifen Anhalten Schild',
                                 'Delstatslag Stanna för Fotgängare på Övergångsstället-Vägmärke'),
     'portrait', D('portrait', sym_state_law_paddle, FYG, BLACK), 'signslowschool'),
]
for _mph, _after in ((10, 'signaddright'), (15, 'signadvisoryspeed10'), (20, 'signadvisoryspeed15'),
                     (25, 'signadvisoryspeed20'), (30, 'signadvisoryspeed25'), (35, 'signadvisoryspeed30'),
                     (40, 'signadvisoryspeed35'), (45, 'signadvisoryspeed40')):
    CATALOGUE.append(('signadvisoryspeed%d' % _mph,
                      ('Advisory Speed %d MPH Sign (Plaque)' % _mph,
                       'Señal de Velocidad Recomendada %d MPH (Placa)' % _mph,
                       'Richtgeschwindigkeit %d MPH Schild (Zusatzschild)' % _mph,
                       'Rekommenderad Hastighet %d MPH-Vägmärke (Tilläggsskylt)' % _mph),
                      'square', T('square', ['%d' % _mph, 'MPH']), _after))


# ----------------------------------------------------------------------------- wiring

def write_blockstate(registry, shape, has_back):
    sibling = SHAPES[shape][0]
    src = os.path.join(BS_DIR, sibling + '.json')
    with open(src, 'rb') as fh:
        raw = fh.read()
    text = raw.decode('utf-8')
    d = json.loads(text)
    old_tex = d['defaults']['textures']['1']
    new_tex = 'csm:blocks/trafficsigns/' + registry
    assert old_tex in text, (sibling, old_tex)
    out = text.replace('"%s"' % old_tex, '"%s"' % new_tex)
    if has_back:
        # the yield model reads the back from slot "2"; give this sign its own
        eol = '\r\n' if '\r\n' in text else '\n'
        out = out.replace('"1": "%s"' % new_tex,
                          '"1": "%s",%s      "2": "%s_back"' % (new_tex, eol, new_tex), 1)
        assert '_back' in out, registry
    dst = os.path.join(BS_DIR, registry + '.json')
    with open(dst, 'wb') as fh:
        fh.write(out.encode('utf-8'))
    return dst


def insert_after(path, needle_line_pred, new_line, exists_pred):
    """Insert ``new_line`` after the first line matching the predicate, keeping the file's
    line endings; no-op if a line already satisfies exists_pred."""
    with open(path, 'rb') as fh:
        raw = fh.read()
    eol = b'\r\n' if b'\r\n' in raw else b'\n'
    lines = raw.decode('utf-8').split(eol.decode())
    if any(exists_pred(l) for l in lines):
        return False
    idx = next(i for i, l in enumerate(lines) if needle_line_pred(l))
    lines.insert(idx + 1, new_line)
    with open(path, 'wb') as fh:
        fh.write(eol.decode().join(lines).encode('utf-8'))
    return True


def main():
    apply = '--apply' in sys.argv
    check = '--check' in sys.argv
    drift = []
    lang_lines, tab_lines = [], []
    for registry, names, shape, draw, after in CATALOGUE:
        face = draw()
        png = os.path.join(TEX_DIR, registry + '.png')
        if check:
            if not os.path.exists(png):
                drift.append(png)
            else:
                cur = Image.open(png).convert('RGBA')
                if cur.size != face.size or cur.tobytes() != face.tobytes():
                    drift.append(png)
            continue
        face.save(png)
        has_back = shape == 'silhouette'
        if has_back:
            gray_back(face).save(os.path.join(TEX_DIR, registry + '_back.png'))
        write_blockstate(registry, shape, has_back)
        for code, name in zip(LANGS, names):
            lang_lines.append((code, registry, name))
        tab_lines.append((registry, after))
        print('wrote %s (%s%s)' % (registry, shape, ', +back' if has_back else ''))

    if check:
        if drift:
            print('DRIFT: %d texture(s) differ from the generator:' % len(drift))
            for p in drift:
                print('  ' + p)
            sys.exit(1)
        print('all %d gap-sign textures match the generator' % len(CATALOGUE))
        return

    if not apply:
        print('\n# lang (en_us):')
        for code, registry, name in lang_lines:
            if code == 'en_us':
                print('tile.%s.name=%s' % (registry, name))
        print('\n# tab:')
        for registry, after in tab_lines:
            print('    initTabBlock(new BlockTrafficSign("%s"));  // after %s' % (registry, after))
        print('\n(--apply inserts these into the lang files and the tab)')
        return

    # Tab lines: in catalogue order, so a sign that names an earlier catalogue entry as its
    # sibling finds it already inserted.
    for registry, after in tab_lines:
        needle = 'new BlockTrafficSign("%s")' % after
        line = '    initTabBlock(new BlockTrafficSign("%s"));' % registry
        added = insert_after(TAB, lambda l: needle in l, line,
                             lambda l: 'new BlockTrafficSign("%s")' % registry in l)
        print('tab: %s %s' % (registry, 'inserted after ' + after if added else 'already present'))
    for code in LANGS:
        path = os.path.join(LANG_DIR, code + '.lang')
        n = 0
        for lcode, registry, name in lang_lines:
            if lcode != code:
                continue
            # after the sibling's lang line when it has one, else at the end of the sign block
            _r, _n, _s, _d, after = next(e for e in CATALOGUE if e[0] == registry)
            key = 'tile.%s.name=' % registry
            n += insert_after(path, lambda l, a=after: l.startswith('tile.%s.name=' % a),
                              key + name, lambda l, k=key: l.startswith(k))
        print('lang %s: %d added' % (code, n))


if __name__ == '__main__':
    main()
