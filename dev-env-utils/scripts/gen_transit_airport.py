#!/usr/bin/env python3
"""Every asset the Transit tab's airport terminal pieces ship: check-in, the queue, security, the
gate, the flight information boards, baggage claim, luggage carts and the wayfinding signs.

    python dev-env-utils/scripts/gen_transit_airport.py
    python dev-env-utils/scripts/gen_transit_airport.py --check
    python dev-env-utils/scripts/gen_transit_airport.py --fragments   # tab lines to paste

Terminal pieces only: no aircraft, and nothing airside (jet bridges, ground equipment and airfield
lights are for later). A duty-free shop or a cafe is fitted out from Furnishings' Market & Store
tab, so nothing here repeats it. What is here, and the class that places each (package
transit.airport unless another is named):

- **Check-in**: the check-in desk (BlockCheckinDesk: joins into a run with the bag drop scale
  beside it, end panels only at the run's ends, and a lit airline panel a click steps through the
  four invented airlines), the bag drop scale (BlockAirportCounter) and the self check-in kiosk
  (BlockSelfCheckinKiosk, which prints a Boarding Pass for a flight off the boards).
- **The queue**: stanchions whose retractable belts reach to every stanchion beside them
  (BlockQueueStanchion), black and blue.
- **Security**: the X-ray unit, roller conveyor and divesting table (BlockSecurityLine, which
  join into one lane) and security trays (BlockSecurityTray, which drop onto the lane below them).
  The walk-through metal detector is Life Safety's (the Emergency Services tab's, which already
  works), so there is none here.
- **The gate**: the gate desk (BlockAirportCounter, joining), the boarding pass scanner
  (BlockBoardingPassScanner), beam seating (transit.platform.BlockPlatformBench, whose seats it
  shares) and the hanging gate sign (BlockGateSign: A1 to D20, clicked like the platform number).
- **The flight information boards** (BlockFlightBoard, TileEntityFlightBoardRenderer): departures
  and arrivals monitors that list a made-up daily schedule off the world's clock; a bank of them
  side by side lists one page after another. The large boards (BlockFlightBoardLarge) are cells
  that join into one screen up to 8 x 5, drawn by their renderer from the small board's texture.
- **Baggage claim**: the carousel (BlockBaggageCarousel), whose pieces join on four sides into a
  loop of any size, the belt drawn moving round it clockwise.
- **Luggage carts**: a cart, and a rack of nested carts that joins into a row (BlockPlatformRun).
- **Wayfinding**: hanging signs for gates, arrivals, check-in, baggage claim and ground transport,
  with generic pictograms, their arrows reversed on the back so they point the same way from
  either side; and the large hanging sign (transit.wayfinding.BlockWayfindingPanel), cells that
  join into one panel up to 16 x 6 whose legend, pictogram and arrow are set in a GUI and drawn
  by its renderer from the sprites written here.

Every model faces north, as the platform fit-out's do (gen_transit_platforms.py, whose element
helpers this borrows): a counter's customer side, a sign's front and a monitor's screen look north,
and a wall piece has its wall at z = 16. Airlines, flight numbers and cities are invented; the
schedule itself is FlightSchedule.java's, and AIRLINES and CITIES here must list the same names in
the same order.
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_transit_platforms as gp  # noqa: E402
import sign_texture_size as sts  # noqa: E402

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(REPO, "modules", "transit", "src", "main", "resources", "assets", "csm")

C = lc.Catalogue("gen_transit_airport.py", "transit/airport", "transit/airport", assets=ASSETS)
TAB = "CsmTabTransit"

B, win, flip, line = gp.B, gp.win, gp.flip, gp.line
ALL, SIDES = gp.ALL, gp.SIDES
names_of, box_java, gui_display = gp.names_of, gp.box_java, gp.gui_display
FACINGS = gp.FACINGS

WHITE = gp.WHITE
BLACK = gp.BLACK
YELLOW = (250, 200, 30)
CHARCOAL = (44, 46, 50)
STAINLESS = gp.STAINLESS
NAVY = (14, 26, 60)
LAMINATE = (206, 208, 210)
COUNTER_TOP = (230, 230, 226)
RUBBER = (30, 30, 32)

# The four invented airlines: id, name, code, main colour, second colour. FlightSchedule.java
# lists the same names and codes in the same order.
AIRLINES = [
    # id, name, code, the panel's ground, its lettering, the mark's colour
    ("kestrel", "KESTREL AIR", "KT", (20, 32, 62), WHITE, (214, 92, 32)),
    ("lantern", "LANTERN AIRWAYS", "LW", (112, 26, 34), (240, 186, 44), (240, 186, 44)),
    ("willow", "WILLOWJET", "WJ", (240, 242, 238), (40, 120, 66), (54, 146, 82)),
    ("saxton", "SAXTON AIRLINES", "SX", (32, 84, 172), WHITE, (200, 206, 214)),
]


def grain(size, colour, amount, seed, alpha=255):
    return lc.fill(colour, size, amount, seed, alpha)


def rect(img, x0, y0, x1, y1, colour):
    """lc.rect at texel corners rounded from fractional ones."""
    lc.rect(img, int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1)), colour)


# ------------------------------------------------------------------------------------------
# Pictograms: generic, drawn as polygons (ImageDraw without anti-aliasing, so a texture is the
# same on every machine)
# ------------------------------------------------------------------------------------------
_PLANE = [(1.0, 0.0), (0.84, 0.1), (0.2, 0.1), (-0.22, 0.86), (-0.42, 0.86), (-0.2, 0.1),
          (-0.7, 0.1), (-0.86, 0.36), (-0.98, 0.36), (-0.9, 0.0)]


def plane(img, cx, cy, size, angle, colour):
    """A plane seen from above, nose along angle (degrees, 0 = right, positive = down)."""
    pts = _PLANE + [(x, -y) for x, y in reversed(_PLANE[1:])]
    a = math.radians(angle)
    ca, sa = math.cos(a), math.sin(a)
    poly = [(cx + (x * ca - y * sa) * size, cy + (x * sa + y * ca) * size) for x, y in pts]
    ImageDraw.Draw(img).polygon(poly, fill=lc.clamp(colour) + (255,))


def suitcase(img, x, y, w, h, colour):
    d = ImageDraw.Draw(img)
    c = lc.clamp(colour) + (255,)
    d.rectangle([x, y + h * 0.2, x + w - 1, y + h - 1], fill=c)
    hw = w * 0.36
    d.rectangle([x + (w - hw) / 2, y, x + (w + hw) / 2 - 1, y + h * 0.08], fill=c)
    d.rectangle([x + (w - hw) / 2, y, x + (w - hw) / 2 + h * 0.07, y + h * 0.2], fill=c)
    d.rectangle([x + (w + hw) / 2 - 1 - h * 0.07, y, x + (w + hw) / 2 - 1, y + h * 0.2],
                fill=c)


def bus(img, x, y, w, h, colour, bg):
    """A bus from the side, facing right: body, windows, two wheels."""
    d = ImageDraw.Draw(img)
    c, b = lc.clamp(colour) + (255,), lc.clamp(bg) + (255,)
    d.rectangle([x, y, x + w - 1, y + h * 0.78], fill=c)
    for i in range(4):
        wx = x + w * (0.06 + i * 0.2)
        d.rectangle([wx, y + h * 0.12, wx + w * 0.14, y + h * 0.4], fill=b)
    d.rectangle([x + w * 0.86, y + h * 0.12, x + w * 0.95, y + h * 0.6], fill=b)
    for wx in (0.24, 0.76):
        r = h * 0.16
        d.ellipse([x + w * wx - r, y + h * 0.78 - r, x + w * wx + r, y + h * 0.78 + r], fill=c)
        d.ellipse([x + w * wx - r * 0.4, y + h * 0.78 - r * 0.4, x + w * wx + r * 0.4,
                   y + h * 0.78 + r * 0.4], fill=b)


def taxi(img, x, y, w, h, colour, bg):
    """A car from the side with a roof sign."""
    d = ImageDraw.Draw(img)
    c, b = lc.clamp(colour) + (255,), lc.clamp(bg) + (255,)
    d.rectangle([x + w * 0.42, y, x + w * 0.58, y + h * 0.14], fill=c)
    d.polygon([(x + w * 0.2, y + h * 0.42), (x + w * 0.3, y + h * 0.18), (x + w * 0.72, y + h * 0.18),
               (x + w * 0.84, y + h * 0.42)], fill=c)
    d.rectangle([x, y + h * 0.42, x + w - 1, y + h * 0.74], fill=c)
    d.polygon([(x + w * 0.33, y + h * 0.4), (x + w * 0.37, y + h * 0.24), (x + w * 0.49, y + h * 0.24),
               (x + w * 0.49, y + h * 0.4)], fill=b)
    d.polygon([(x + w * 0.53, y + h * 0.4), (x + w * 0.53, y + h * 0.24), (x + w * 0.69, y + h * 0.24),
               (x + w * 0.76, y + h * 0.4)], fill=b)
    for wx in (0.22, 0.78):
        r = h * 0.15
        d.ellipse([x + w * wx - r, y + h * 0.74 - r, x + w * wx + r, y + h * 0.74 + r], fill=c)
        d.ellipse([x + w * wx - r * 0.4, y + h * 0.74 - r * 0.4, x + w * wx + r * 0.4,
                   y + h * 0.74 + r * 0.4], fill=b)


def person(img, cx, y, h, colour):
    d = ImageDraw.Draw(img)
    c = lc.clamp(colour) + (255,)
    r = h * 0.13
    d.ellipse([cx - r, y, cx + r, y + 2 * r], fill=c)
    d.rectangle([cx - h * 0.14, y + h * 0.3, cx + h * 0.14, y + h * 0.66], fill=c)
    d.rectangle([cx - h * 0.14, y + h * 0.62, cx - h * 0.03, y + h - 1], fill=c)
    d.rectangle([cx + h * 0.03, y + h * 0.62, cx + h * 0.14, y + h - 1], fill=c)


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def laminate_front():
    """The check-in desk's customer face: pale laminate over a stainless kick plate."""
    img = grain(32, LAMINATE, 2, 601)
    lc.rect(img, 0, 27, 32, 32, STAINLESS)
    lc.rect(img, 0, 26, 32, 27, lc.shade(STAINLESS, 0.7))
    lc.rect(img, 0, 3, 32, 4, lc.shade(LAMINATE, 0.86))
    return img


def gate_front():
    """The gate desk's face: charcoal with a pale band and a thin lit strip."""
    img = grain(32, CHARCOAL, 2, 602)
    lc.rect(img, 0, 4, 32, 12, (196, 198, 200))
    lc.rect(img, 0, 13, 32, 14, (120, 196, 240))
    lc.rect(img, 0, 28, 32, 32, lc.shade(CHARCOAL, 0.7))
    return img


def desk_back():
    """The agent's side of a desk: drawers and a cupboard in grey laminate."""
    img = grain(32, (170, 172, 176), 2, 603)
    for x0, x1 in ((1, 15), (17, 31)):
        lc.frame(img, x0, 2, x1, 30, (130, 132, 136))
    for y in (10, 18):
        lc.rect(img, 1, y, 15, y + 1, (130, 132, 136))
    for x0, y0 in ((6, 5), (6, 13), (6, 22), (22, 12)):
        lc.rect(img, x0, y0, x0 + 4, y0 + 1, STAINLESS)
    return img


def counter_top():
    img = grain(16, COUNTER_TOP, 3, 604)
    rng = random.Random(605)
    gp.speckle(img, rng, 0.04, lc.shade(COUNTER_TOP, 1.05), lc.shade(COUNTER_TOP, 0.9))
    return img


def agent_screen():
    """An agent's monitor: a departure-control screen, rows of text on blue."""
    img = grain(32, (26, 60, 120), 2, 606)
    lc.rect(img, 0, 0, 32, 4, (200, 204, 210))
    rng = random.Random(607)
    for y in range(6, 30, 3):
        x = 2
        while x < 29:
            w = rng.randint(2, 7)
            lc.rect(img, x, y, min(30, x + w), y + 1, (190, 214, 240))
            x += w + rng.randint(1, 3)
    return img


def airline_panel(entry):
    """The airline's lit panel on the post behind a check-in desk: its mark over its name, 7 x 5
    units (64 x 46 texels)."""
    aid, name, code, bg, fg, mark = entry
    img = grain(64, bg, 2, 610 + len(aid))
    w, h = 64, 46
    lc.frame(img, 0, 0, w, h, lc.shade(bg, 0.7), 1)
    words = name.split(" ")
    top, bottom = (words[0], " ".join(words[1:])) if len(words) > 1 else (name[:6], name[6:])
    d = ImageDraw.Draw(img)
    mc = lc.clamp(mark) + (255,)
    cx = w / 2.0
    if aid == "kestrel":      # a stooping bird's wings
        d.polygon([(cx - 9, 4), (cx, 14), (cx + 9, 4), (cx, 9)], fill=mc)
    elif aid == "lantern":    # a lantern
        d.rectangle([cx - 4, 6, cx + 4, 16], fill=mc)
        d.rectangle([cx - 2, 3, cx + 2, 6], fill=mc)
        d.rectangle([cx - 2, 9, cx + 2, 13], fill=lc.clamp(bg) + (255,))
    elif aid == "willow":     # a leaf
        d.ellipse([cx - 4, 3, cx + 4, 16], fill=mc)
        d.rectangle([cx, 5, cx, 16], fill=lc.clamp(bg) + (255,))
    else:                     # a peak
        d.polygon([(cx - 8, 16), (cx, 3), (cx + 8, 16)], fill=mc)
        d.polygon([(cx - 3, 8), (cx, 3), (cx + 3, 8)], fill=lc.clamp(bg) + (255,))
    lc.draw_text_centred(img, top, cx, 20, fg, 2)
    lc.draw_text_centred(img, bottom, cx, 33, fg, 2 if lc.text_width(bottom, 2) <= 62 else 1)
    return img


def printer():
    img = grain(16, (60, 62, 66), 2, 620)
    lc.rect(img, 3, 7, 13, 8, (20, 20, 22))
    lc.rect(img, 11, 3, 13, 4, (60, 200, 90))
    return img


def scale_belt():
    """The bag drop's belt, ribbed across."""
    img = grain(16, RUBBER, 2, 621)
    for y in range(0, 16, 2):
        lc.rect(img, 0, y, 16, y + 1, lc.shade(RUBBER, 1.35))
    return img


def weight_display():
    img = grain(16, (18, 20, 22), 1, 622)
    lc.rect(img, 1, 4, 15, 12, (40, 8, 6))
    lc.draw_text_centred(img, "23", 8, 6, (255, 70, 40), 1)
    return img


def curtain(colour=(24, 24, 26), seed=623):
    """Rubber curtain strips, hung side by side with slits between them (cutout)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(seed)
    for x0 in range(0, 16, 4):
        d = rng.uniform(-6, 6)
        lc.rect(img, x0, 0, x0 + 3, 16, tuple(v + d for v in colour))
    return img


def kiosk_screen():
    img = grain(64, (240, 244, 248), 1, 624)
    lc.rect(img, 0, 0, 64, 12, (30, 70, 140))
    lc.draw_text_centred(img, "CHECK-IN", 32, 3, WHITE, 1)
    plane(img, 32, 28, 9, -25, (30, 70, 140))
    lc.draw_text_centred(img, "TOUCH TO", 32, 40, (40, 44, 50), 1)
    lc.draw_text_centred(img, "START", 32, 47, (40, 44, 50), 1)
    lc.rect(img, 12, 55, 52, 60, (40, 160, 90))
    return img


def kiosk_front():
    """The kiosk's lower face: a card reader, a passport reader and the pass printer's slot."""
    img = grain(32, (220, 222, 224), 2, 625)
    lc.rect(img, 3, 4, 13, 12, (40, 42, 46))
    lc.rect(img, 5, 7, 11, 8, (80, 200, 120))
    lc.rect(img, 17, 4, 29, 12, (40, 42, 46))
    lc.rect(img, 18, 5, 28, 11, (70, 90, 110))
    lc.rect(img, 6, 20, 26, 22, (18, 18, 20))
    lc.draw_text_centred(img, "PASS", 16, 24, (90, 94, 100), 1)
    return img


def kiosk_header():
    """The kiosk's lit header, 9 x 4 units: the top 32 x 14 texels."""
    img = grain(32, (30, 70, 140), 2, 626)
    lc.draw_text_centred(img, "CHECK-IN", 16, 3, WHITE, 1)
    lc.rect(img, 4, 10, 28, 11, YELLOW)
    return img


def belt_webbing(base, edge, seed):
    """A retractable queue belt, as its face is seen: the webbing with its edges, drawn the whole
    height of the texture, which the arm maps onto its 1.8 units."""
    img = grain(16, base, 2, seed)
    if edge:
        lc.rect(img, 0, 1, 16, 3, edge)
        lc.rect(img, 0, 13, 16, 15, edge)
    px = img.load()
    for y in range(16):
        for x in range(0, 16, 2):
            r, g, b, a = px[x, y]
            px[x, y] = lc.shade((r, g, b), 0.9) + (a,)
    return img


def chrome():
    img = grain(16, (196, 200, 206), 3, 630)
    for y in range(16):
        lc.rect(img, 5, y, 7, y + 1, (236, 238, 242))
        lc.rect(img, 11, y, 12, y + 1, (150, 154, 160))
    return img


def xray_upper():
    """The X-ray unit's upper side: pale housing, a dark band and its label."""
    img = grain(64, (220, 218, 210), 2, 640)
    lc.rect(img, 0, 38, 64, 42, (60, 64, 70))
    lc.rect(img, 6, 10, 46, 26, (250, 210, 40))
    lc.frame(img, 6, 10, 46, 26, (30, 30, 32), 1)
    # a generic radiation trefoil, three sectors round a dot
    cx, cy = 14, 18
    d = ImageDraw.Draw(img)
    for k in range(3):
        a = math.radians(90 + k * 120)
        d.pieslice([cx - 6, cy - 6, cx + 6, cy + 6], math.degrees(a) - 30,
                   math.degrees(a) + 30, fill=(20, 20, 22, 255))
    lc.disc(img, cx, cy, 1.6, (250, 210, 40))
    lc.disc(img, cx, cy, 1.0, (20, 20, 22))
    lc.draw_text(img, "X-RAY", 22, 16, (20, 20, 22), 1)
    lc.draw_text(img, "SCREENING", 6, 30, (60, 64, 70), 1)
    return img


def xray_lower():
    img = grain(32, (206, 204, 196), 2, 641)
    lc.rect(img, 0, 0, 32, 2, (60, 64, 70))
    lc.rect(img, 0, 29, 32, 32, (90, 92, 96))
    return img


def conveyor(width, frames, pitch, base, slat, seed, edge=None):
    """An animated belt: `frames` frames, each width x width, the pattern moving one texel a
    frame along +u, repeating every `pitch` texels (pitch divides width, and frames = pitch, so
    the loop has no jump)."""
    rng = random.Random(seed)
    frame0 = Image.new("RGBA", (width, width))
    px = frame0.load()
    for y in range(width):
        for x in range(width):
            d = rng.uniform(-3, 3)
            k = x % pitch
            if k == 0:
                c = lc.shade(slat, 1.25)
            elif k == pitch - 1:
                c = lc.shade(base, 0.7)
            else:
                c = slat if k < pitch - 2 else base
            px[x, y] = lc.clamp(tuple(v + d for v in c)) + (255,)
    if edge:
        for y in (0, width - 1):
            for x in range(width):
                px[x, y] = lc.clamp(edge) + (255,)
    strip = Image.new("RGBA", (width, width * frames))
    for f in range(frames):
        shifted = Image.new("RGBA", (width, width))
        shifted.paste(frame0, (f, 0))
        shifted.paste(frame0, (f - width, 0))
        strip.paste(shifted, (0, f * width))
    return strip


def rollers():
    """A roller conveyor's top: rollers across the lane, repeated along it."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rng = random.Random(642)
    for y in range(16):
        for x in range(16):
            k = x % 4
            c = [(96, 98, 102), (200, 204, 208), (170, 174, 180), (40, 42, 46)][k]
            px[x, y] = lc.clamp(tuple(v + rng.uniform(-3, 3) for v in c)) + (255,)
    return img


def tray_side():
    """A stack of security trays from the side: a lip line for every tray."""
    img = grain(16, (126, 130, 136), 2, 643)
    for i in range(6):
        y = int(round(i * 16 / 6.0))
        lc.rect(img, 0, y, 16, y + 1, (178, 182, 188))
    return img


def tray_inside():
    img = grain(16, (110, 114, 120), 2, 644)
    lc.frame(img, 0, 0, 16, 16, (140, 144, 150))
    return img


def laptop():
    img = grain(16, (70, 72, 78), 2, 645)
    lc.frame(img, 0, 0, 16, 16, (120, 124, 130))
    lc.disc(img, 8, 8, 1.5, (130, 134, 140))
    return img


def jacket():
    img = grain(16, (40, 70, 130), 5, 646)
    for y in range(2, 16, 5):
        lc.rect(img, 0, y, 16, y + 1, (30, 56, 104))
    return img


def scanner_top():
    """A boarding pass reader's glass: dark, a red scan line, a pass outline."""
    img = grain(16, (22, 24, 30), 1, 647)
    lc.frame(img, 2, 3, 14, 13, (70, 76, 90))
    lc.rect(img, 1, 8, 15, 9, (230, 40, 36))
    return img


def upholstery(colour, seed):
    img = grain(16, colour, 3, seed)
    lc.rect(img, 0, 15, 16, 16, lc.shade(colour, 0.7))
    lc.rect(img, 0, 0, 16, 1, lc.shade(colour, 1.2))
    return img


def carousel_corner():
    """A carousel's plates turning a corner: slats radiating from the inner corner (the texture's
    bottom left, the block's south-west, for the north-east piece)."""
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    rng = random.Random(650)
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5, 32 - (y + 0.5)
            a = math.degrees(math.atan2(dy, dx))
            k = int(a / 7.5) % 4
            c = (60, 62, 66) if k else (110, 112, 118)
            if (dx * dx + dy * dy) ** 0.5 < 5:
                c = (140, 144, 150)
            px[x, y] = lc.clamp(tuple(v + rng.uniform(-3, 3) for v in c)) + (255,)
    return img


def carousel_end():
    img = Image.new("RGBA", (32, 32))
    px = img.load()
    rng = random.Random(651)
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            a = math.degrees(math.atan2(dy, dx))
            k = int((a + 180) / 11.25) % 4
            c = (60, 62, 66) if k else (110, 112, 118)
            if (dx * dx + dy * dy) ** 0.5 < 4:
                c = (150, 154, 160)
            px[x, y] = lc.clamp(tuple(v + rng.uniform(-3, 3) for v in c)) + (255,)
    return img


def carousel_skirt():
    """A carousel's side, drawn to be squeezed onto its 7.6 units: a black rubber bumper along
    the top over brushed stainless."""
    img = grain(32, STAINLESS, 3, 652)
    lc.rect(img, 0, 0, 32, 7, RUBBER)
    lc.rect(img, 0, 7, 32, 8, lc.shade(STAINLESS, 0.6))
    lc.rect(img, 0, 29, 32, 32, lc.shade(STAINLESS, 0.55))
    return img


# --- the flight information boards ---------------------------------------------------------
# BlockFlightBoard / TileEntityFlightBoardRenderer: the screen's place on the model and the rows,
# in sixteenths. The screen is drawn with its window FIDS_W x FIDS_H texels of a 256 square, then
# stored at FIDS_TEX (85 texels a block on a face under a block): the renderer and the model take
# the window as a fraction of the texture (WINDOW_V = 148f / 256f), whatever size it is stored at.
SCREEN_X0, SCREEN_X1 = 0.4, 15.6
SCREEN_Y0, SCREEN_Y1 = 3.6, 12.4
SCREEN_Z = 14.6   # 0.4 proud of the bezel: model_depth moves a face closer than 0.2 to another
HEADER_H, COLHEAD_H, ROW_PITCH, ROWS = 1.6, 0.8, 0.9, 7
FIDS_W = 256
FIDS_TEX = 128
FIDS_H = int(round(FIDS_W * (SCREEN_Y1 - SCREEN_Y0) / (SCREEN_X1 - SCREEN_X0)))   # 148
FIDS_BG = (10, 22, 52)


def fids(arrivals):
    """A board's screen: the header band with DEPARTURES or ARRIVALS and a plane, the column
    heads' band, and the rows' zebra bands. Everything that changes is the renderer's."""
    img = Image.new("RGBA", (FIDS_W, FIDS_W), (0, 0, 0, 255))
    k = FIDS_W / (SCREEN_X1 - SCREEN_X0)
    lc.rect(img, 0, 0, FIDS_W, FIDS_H, FIDS_BG)
    hdr = int(round(HEADER_H * k))
    lc.rect(img, 0, 0, FIDS_W, hdr, (22, 52, 116))
    lc.rect(img, 0, hdr - 1, FIDS_W, hdr, YELLOW)
    col = int(round((HEADER_H + COLHEAD_H) * k))
    lc.rect(img, 0, hdr, FIDS_W, col, (16, 34, 78))
    for i in range(ROWS):
        y0 = int(round((HEADER_H + COLHEAD_H + i * ROW_PITCH) * k))
        y1 = int(round((HEADER_H + COLHEAD_H + (i + 1) * ROW_PITCH) * k))
        if i % 2:
            lc.rect(img, 0, y0, FIDS_W, min(y1, FIDS_H), (16, 32, 70))
    plane(img, 14, hdr / 2.0, 9, 30 if arrivals else -30, WHITE)
    lc.draw_text(img, "ARRIVALS" if arrivals else "DEPARTURES", 30, hdr // 2 - 7, WHITE, 3)
    return sts.reduce(img, FIDS_TEX)


def bezel():
    img = grain(16, (22, 22, 24), 1, 660)
    return img


# --- wayfinding ----------------------------------------------------------------------------
WAY_W, WAY_H = 192, 48    # a 24 x 6 panel
WAY_BG = (36, 38, 42)

WAYFINDING = [
    # id, lines, pictogram
    ("gates", ["GATES"], "depart"),
    ("arrivals", ["ARRIVALS"], "arrive"),
    ("check_in", ["CHECK-IN"], "checkin"),
    ("baggage_claim", ["BAGGAGE", "CLAIM"], "baggage"),
    ("ground_transport", ["GROUND", "TRANSPORT"], "ground"),
]


def pictogram(img, kind, x, y, s, bg=YELLOW):
    """A black pictogram on a square s texels a side, yellow unless `bg` says otherwise."""
    rect(img, x, y, x + s, y + s, bg)
    if kind == "depart":
        plane(img, x + s / 2.0, y + s / 2.0, s * 0.42, -30, BLACK)
    elif kind == "arrive":
        plane(img, x + s / 2.0, y + s * 0.42, s * 0.4, 30, BLACK)
        rect(img, x + 4, y + s - 6, x + s - 4, y + s - 4, BLACK)
    elif kind == "checkin":
        person(img, x + s * 0.3, y + 4, s * 0.62, BLACK)
        rect(img, x + s * 0.44, y + s * 0.5, x + s - 4, y + s * 0.56, BLACK)
        rect(img, x + s * 0.5, y + s * 0.56, x + s - 6, y + s - 4, BLACK)
    elif kind == "baggage":
        suitcase(img, x + s * 0.24, y + 4, s * 0.52, s * 0.52, BLACK)
        rect(img, x + 3, y + s * 0.7, x + s - 3, y + s * 0.78, BLACK)
        for i in range(4):
            lc.disc(img, x + 6 + i * (s - 12) / 3.0, y + s * 0.86, 1.8, BLACK)
    elif kind == "ground":
        half = s // 2
        bus(img, x + 3, y + 3, s - 6, half - 3, BLACK, bg)
        taxi(img, x + 5, y + half + 1, s - 10, half - 4, BLACK, bg)


def wayfinding(entry, right):
    """A hanging wayfinding sign: yellow capitals on charcoal, the pictogram in a yellow square
    and an arrow; `right` False is the back's art, the arrow turned to point the same way in the
    world."""
    wid, lines_, kind = entry
    img = grain(256, WAY_BG, 2, 670 + len(wid))
    lc.frame(img, 0, 0, WAY_W, WAY_H, lc.shade(WAY_BG, 1.6))
    s = 36
    aw = 30
    scale = 3 if len(lines_) == 1 else 2
    tw = max(lc.text_width(t, scale) for t in lines_)
    if right:
        px = 6
        tx = px + s + 8
        ax = WAY_W - aw - 8
    else:
        ax = 8
        px = WAY_W - s - 6
        tx = px - 8 - tw
    pictogram(img, kind, px, 6, s)
    if len(lines_) == 1:
        lc.draw_text(img, lines_[0], tx, 24 - 7, YELLOW, scale)
    else:
        lc.draw_text(img, lines_[0], tx, 12, YELLOW, scale)
        lc.draw_text(img, lines_[1], tx, 26, YELLOW, scale)
    gp.arrow(img, ax, 12, aw, 24, YELLOW, right)
    return img


# --- the gate sign ---------------------------------------------------------------------------
GATE_LETTERS = "ABCD"
GATE_NUMBERS = 20
GATE_K = 8   # texels a unit on the gate sign's cells


def gate_label():
    """The strip across the gate sign's top: GATE and a departing plane, 12 x 2 units."""
    img = grain(128, (22, 24, 28), 1, 680)
    lc.draw_text(img, "GATE", 6, 3, WHITE, 2)
    plane(img, 80, 8, 6, -30, YELLOW)
    return img


def gate_letter(ch):
    """The letter cell, 4.5 x 5 units: black on yellow."""
    img = grain(64, YELLOW, 2, 681 + ord(ch))
    lc.draw_text_centred(img, ch, 18, 5, BLACK, 6)
    return img


def gate_number(n):
    """The number cell, 7.5 x 5 units: yellow on charcoal."""
    img = grain(64, (30, 32, 36), 1, 690)
    lc.draw_text_centred(img, str(n), 30, 5, YELLOW, 6)
    return img


# --- luggage carts ---------------------------------------------------------------------------
def cart_bed():
    img = grain(16, (66, 68, 72), 2, 700)
    for y in range(0, 16, 3):
        lc.rect(img, 0, y, 16, y + 1, (96, 98, 104))
    return img


def cart_sign():
    """The cart rack's sign: CARTS over a cart, and RETURN HERE."""
    img = grain(32, (30, 70, 140), 2, 701)
    lc.draw_text_centred(img, "CARTS", 16, 3, WHITE, 1)
    lc.rect(img, 8, 17, 22, 18, WHITE)
    lc.rect(img, 21, 10, 22, 18, WHITE)
    lc.rect(img, 19, 10, 24, 11, WHITE)
    lc.rect(img, 10, 14, 19, 17, WHITE)
    lc.disc(img, 10, 20, 1.3, WHITE)
    lc.disc(img, 20, 20, 1.3, WHITE)
    lc.draw_text_centred(img, "RETURN", 16, 24, YELLOW, 1)
    return img


def boarding_pass_icon():
    """The Boarding Pass item: a pass with its stub, a plane and a barcode."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    lc.rect(img, 1, 4, 15, 12, (246, 246, 240))
    lc.rect(img, 1, 4, 15, 6, (30, 70, 140))
    lc.rect(img, 11, 6, 12, 12, (190, 190, 186))
    plane(img, 5, 8.5, 2.2, -25, (30, 70, 140))
    for x in (8, 10, 13, 14):
        lc.rect(img, x, 8, x + 1, 11, (30, 30, 32))
    lc.frame(img, 1, 4, 15, 12, (150, 150, 146))
    return img


def register_textures():
    tex = {
        "laminate_front": laminate_front, "gate_front": gate_front, "desk_back": desk_back,
        "counter_top": counter_top, "agent_screen": agent_screen, "printer": printer,
        "scale_belt": scale_belt, "weight_display": weight_display, "curtain": curtain,
        "kiosk_screen": kiosk_screen, "kiosk_front": kiosk_front, "kiosk_header": kiosk_header,
        "kiosk_body": lambda: grain(16, (226, 228, 230), 2, 627),
        "belt_black": lambda: belt_webbing((26, 26, 28), None, 628),
        "belt_blue": lambda: belt_webbing((30, 60, 150), (200, 206, 220), 629),
        "chrome": chrome,
        "xray_upper": xray_upper, "xray_lower": xray_lower,
        "xray_belt": lambda: conveyor(16, 4, 4, (26, 26, 28), (44, 44, 48), 648),
        "xray_curtain": lambda: curtain((50, 44, 36), 649),
        "rollers": rollers, "divest_top": lambda: grain(16, STAINLESS, 4, 653),
        "rails": lambda: grain(16, (150, 154, 160), 3, 654),
        "tray_side": tray_side, "tray_inside": tray_inside, "laptop": laptop,
        "jacket": jacket, "shoe": lambda: grain(16, (60, 40, 30), 4, 655),
        "scanner_top": scanner_top,
        "seat_black": lambda: upholstery((36, 36, 40), 656),
        "seat_blue": lambda: upholstery((34, 70, 140), 657),
        "beam": lambda: grain(16, (58, 60, 64), 2, 658),
        "carousel_belt": lambda: conveyor(32, 8, 8, (40, 42, 46), (84, 86, 92), 659),
        "carousel_corner": carousel_corner, "carousel_end": carousel_end,
        "carousel_skirt": carousel_skirt,
        "carousel_island": lambda: grain(16, STAINLESS, 4, 661),
        "rubber": lambda: grain(16, RUBBER, 2, 662),
        "fids_departures": lambda: fids(False), "fids_arrivals": lambda: fids(True),
        "bezel": bezel,
        "gate_label": gate_label,
        "cart_frame": lambda: grain(16, (176, 180, 186), 3, 702),
        "cart_bed": cart_bed,
        "cart_grip": lambda: grain(16, (190, 40, 36), 3, 703),
        "cart_sign": cart_sign,
        "cart_post": lambda: grain(16, (30, 70, 140), 2, 704),
        "graphite": lambda: grain(16, gp.GRAPHITE, 2, 705),
        "steel": lambda: grain(16, gp.STEEL, 3, 706),
        "stainless": lambda: grain(16, STAINLESS, 3, 707),
        "white": lambda: grain(16, (226, 228, 226), 2, 708),
        "black": lambda: grain(16, (30, 32, 34), 2, 709),
        "boarding_pass": boarding_pass_icon,
    }
    for s, name in enumerate(("idle", "ok", "no")):
        tex["scanner_" + name] = (lambda s=s: gp.validator_screen(s))
    for entry in AIRLINES:
        tex["airline_" + entry[0]] = (lambda e=entry: airline_panel(e))
    for entry in WAYFINDING:
        tex["way_" + entry[0]] = (lambda e=entry: wayfinding(e, True))
        tex["way_" + entry[0] + "_back"] = (lambda e=entry: wayfinding(e, False))
    for ch in GATE_LETTERS:
        tex["gate_letter_" + ch.lower()] = (lambda ch=ch: gate_letter(ch))
    for n in range(1, GATE_NUMBERS + 1):
        tex["gate_number_%d" % n] = (lambda n=n: gate_number(n))
    for name, draw in tex.items():
        C.texture(name)(draw)
    for name, frametime in (("xray_belt", 3), ("carousel_belt", 2)):
        C.extra["textures/blocks/transit/airport/%s.png.mcmeta" % name] = \
            '{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % frametime


# ------------------------------------------------------------------------------------------
# Blockstates
# ------------------------------------------------------------------------------------------
def multipart(parts, prefix, textures, facings=True):
    """gen_transit_platforms.multipart, for this catalogue: models and multipart rules for parts
    [(name, conditions, elements or the name of a part to retexture)], each drawn turned to each
    of the four facings."""
    models = {}
    rules = []
    for name, cond, els in parts:
        mname = "%s_%s" % (prefix, name)
        if isinstance(els, tuple):          # (parent part, texture overrides)
            parent, over = els
            models[mname] = {"parent": "csm:block/" + C.M("%s_%s" % (prefix, parent))
                             .split(":", 1)[1], "textures": over}
        else:
            models[mname] = lc.model(textures, els)
        for f, y in (FACINGS if facings else ((None, 0),)):
            when = {"facing": f} if f else {}
            for k, v in cond.items():
                when[k] = ("true" if v else "false") if isinstance(v, bool) else str(v)
            apply = {"model": C.M(mname)}
            if y:
                apply["y"] = y
            rules.append({"when": when, "apply": apply} if when else {"apply": apply})
    return models, {"multipart": rules}


def item_of(parts, textures, keep=lambda cond: all(v is False for v in cond.values()),
            display=None):
    els = []
    for name, cond, e in parts:
        if keep(cond) and not isinstance(e, tuple):
            els += e
    m = lc.model(textures, els)
    if display:
        m["display"] = display
    return m


def fixture(reg, java, names, els, textures, ao=True, display=None, extra_state=None):
    m = lc.model(textures, els, ao=ao)
    if display:
        m["display"] = display
    C.add(reg, java, names, {reg: m}, lc.facing_state(C.M(reg), extra_state), tab=TAB)


# ------------------------------------------------------------------------------------------
# Check-in and the gate desk
# ------------------------------------------------------------------------------------------
def agent_monitor(x0=1.8):
    """The agent's monitor on the work top, its screen facing the agent (south)."""
    x1 = x0 + 6.4
    xc = (x0 + x1) / 2
    return [B((xc - 1.5, 11.6, 8.6), (xc + 1.5, 11.9, 11), "black", SIDES + ("up",)),
            B((xc - 0.4, 11.9, 9.6), (xc + 0.4, 13.6, 10.2), "black", SIDES),
            B((x0, 13.4, 9.8), (x1, 17.4, 10.4), "black", ALL, per={"south": "screen"},
              uv={"south": [0, 0, 16, 10]})]


def counter_parts(front_tex, depth=15.4, extra=()):
    """A desk one block of a run: its customer face and ledge, the agent's work top and cabinet,
    and end panels only where the run stops (BlockAirportCounter's LEFT and RIGHT)."""
    F = False
    body = [B((0, 0, 1.6), (16, 15, 2.6), "front", ("north",), uv={"north": [0, 0, 16, 16]}),
            B((0, 15, 1.0), (16, 15.8, 5.5), "top", ("north", "up", "down", "south")),
            B((0, 0, 2.6), (16, 11, depth), "back", ("south",), uv={"south": [0, 0, 16, 16]}),
            B((0, 11, 2.6), (16, 11.6, depth + 0.2), "top", ("up", "south"))]
    body += list(extra)

    def end(x0, x1, face):
        return [B((x0, 0, 1.0), (x1, 15.8, depth + 0.2), "end", ALL)]
    return [("body", {}, body),
            ("end_left", {"left": F}, end(0, 0.8, "west")),
            ("end_right", {"right": F}, end(15.2, 16, "east"))]


def checkin():
    tex = {"front": C.T("laminate_front"), "top": C.T("counter_top"), "back": C.T("desk_back"),
           "end": C.T("counter_top"), "black": C.T("black"), "screen": C.T("agent_screen"),
           "printer": C.T("printer"), "panel": C.T("airline_kestrel"), "post": C.T("steel"),
           "particle": C.T("laminate_front")}
    extra = agent_monitor() + [
        B((10, 11.6, 6.5), (13, 12.8, 9), "printer", ("north", "south", "east", "west", "up"),
          per={"north": "printer"}),
    ]
    parts = counter_parts("laminate_front", extra=extra)
    # the airline's lit panel on its post behind the desk, one retexture per airline
    panel = [B((11.9, 11.6, 14.2), (12.7, 19.5, 15.0), "post", SIDES),
             B((8.8, 19.5, 13.4), (15.8, 24.5, 14.2), "post", ALL, per={"north": "panel"},
               uv={"north": win(64, 46, 64)})]
    parts.append(("panel_0", {"airline": 0}, panel))
    for i, entry in enumerate(AIRLINES[1:], 1):
        parts.append(("panel_%d" % i, {"airline": i},
                      ("panel_0", {"panel": C.T("airline_" + entry[0])})))
    models, state = multipart(parts, "airport_checkin_desk", tex)
    item = item_of(parts, tex, keep=lambda c: all(v is False or v == 0 for v in c.values()),
                   display=gui_display(0.5, -1.0))
    C.add("airport_checkin_desk",
          'new BlockCheckinDesk("airport_checkin_desk", new double[]{0, 0, 1, 16, 16, 15.6})',
          names_of("Check-In Desk", "Check-in-Schalter", "Mostrador de Facturación",
                   "Incheckningsdisk"),
          models, state, item=item, tab=TAB)

    # the bag drop scale, between desks: a low weighing belt into the wall behind, its weight
    # read out on the customer's side
    els = [B((1, 0, 1.2), (15, 4.4, 15.6), "rails", ("north", "east", "west"),
             per={"north": "front"}),
           B((1.6, 4.4, 1.6), (14.4, 4.9, 15.6), "rails", ("north", "up"), per={"up": "belt"},
             uv={"up": [0, 0, 16, 16]}),
           B((0.4, 0, 1.2), (1.6, 7.2, 15.6), "stainless", ALL),
           B((14.4, 0, 1.2), (15.6, 7.2, 15.6), "stainless", ALL),
           # the rubber curtain where the bag leaves, and the hood over it
           B((1.6, 4.9, 15.2), (14.4, 11, 15.2), "curtain", ("north",),
             uv={"north": [0, 0, 16, 16]}),
           B((0.4, 11, 13.4), (15.6, 12.2, 16), "stainless", ALL),
           # the weight display on its post
           B((13.6, 7.2, 1.6), (14.8, 12.4, 2.6), "stainless", SIDES),
           B((12.6, 12.4, 1.0), (15.6, 15.2, 2.8), "black", ALL, per={"north": "display"},
             uv={"north": [0, 2, 16, 14]})]
    tex = {"rails": C.T("rails"), "front": C.T("laminate_front"), "belt": C.T("scale_belt"),
           "stainless": C.T("stainless"), "curtain": C.T("curtain"), "black": C.T("black"),
           "display": C.T("weight_display"), "particle": C.T("stainless")}
    models, state = multipart([("body", {}, els)], "airport_checkin_scale", tex)
    C.add("airport_checkin_scale",
          'new BlockAirportCounter("airport_checkin_scale", "checkin", '
          'new double[]{0.4, 0, 1, 15.6, 12.2, 16})',
          names_of("Bag Drop Scale", "Gepäckwaage", "Báscula de Equipaje", "Bagagevåg"),
          models, state, item=item_of([("body", {}, els)], tex, display=gui_display(0.6)),
          tab=TAB)

    # the gate desk: darker, with a boarding pass reader set in its ledge
    tex = {"front": C.T("gate_front"), "top": C.T("counter_top"), "back": C.T("desk_back"),
           "end": C.T("graphite"), "black": C.T("black"), "screen": C.T("agent_screen"),
           "reader": C.T("scanner_top"), "particle": C.T("gate_front")}
    extra = agent_monitor(7.6) + [
        B((2, 15.8, 1.4), (5.4, 16.6, 4.6), "black", ALL, per={"up": "reader"},
          uv={"up": [0, 0, 16, 16]})]
    parts = counter_parts("gate_front", depth=13.0, extra=extra)
    models, state = multipart(parts, "airport_gate_desk", tex)
    C.add("airport_gate_desk",
          'new BlockAirportCounter("airport_gate_desk", "gate", '
          'new double[]{0, 0, 1, 16, 16, 13.2})',
          names_of("Gate Desk", "Flugsteigschalter", "Mostrador de Puerta de Embarque",
                   "Gatedisk"),
          models, state, item=item_of(parts, tex, display=gui_display(0.5, -1.0)), tab=TAB)


def kiosk():
    """The self check-in kiosk: a pedestal, a tilted touchscreen over the card, passport and pass
    slots, and a lit header on a stem. The screen and slots are all in the lower block, where a
    click reaches (a click reaches only the block the aim passes through)."""
    tilt = ("x", 22.5, (8, 10.5, 6))
    els = [B((3, 0, 4), (13, 0.6, 13), "body", SIDES + ("up",)),
           B((5.5, 0.6, 7.5), (10.5, 6, 11), "body", SIDES),
           B((3.5, 6, 6), (12.5, 10.5, 12), "body", ALL, per={"north": "front"},
             uv={"north": [0, 0, 16, 16]}),
           B((3.4, 10.5, 6), (12.6, 15.8, 7.2), "body", ALL, per={"north": "screen"},
             uv={"north": win(64, 64, 64)}, rot=tilt),
           B((3.5, 10.5, 7.2), (12.5, 13.5, 12), "body", SIDES + ("up",)),
           # the header on its stem
           B((7.2, 13.5, 10.2), (8.8, 20, 11.4), "body", SIDES),
           B((3.5, 20, 9.2), (12.5, 24, 11.8), "body", ALL, per={"north": "header"},
             uv={"north": [0, 0, 16, 7.1]})]
    tex = {"body": C.T("kiosk_body"), "front": C.T("kiosk_front"),
           "screen": C.T("kiosk_screen"), "header": C.T("kiosk_header"),
           "particle": C.T("kiosk_body")}
    fixture("airport_self_checkin_kiosk",
            'new BlockSelfCheckinKiosk("airport_self_checkin_kiosk", '
            'new double[]{3, 0, 4, 13, 16, 13})',
            names_of("Self Check-In Kiosk", "Check-in-Automat", "Quiosco de Autofacturación",
                     "Incheckningsautomat"),
            els, tex, ao=False, display=gui_display(0.45, -2.0))


# ------------------------------------------------------------------------------------------
# The queue
# ------------------------------------------------------------------------------------------
BELT_Y0, BELT_Y1 = 11.8, 13.6


def stanchions():
    """A queue stanchion: a weighted base, a chrome post and a belt cassette; its belt reaches
    out to every stanchion beside it (north to west, world sides, actual state), so a row of
    them is one barrier. The belt's face takes the whole texture top to bottom."""
    for colour, names in (
            ("black", names_of("Queue Stanchion (Black Belt)", "Absperrpfosten (Schwarzes Band)",
                               "Poste Separador (Cinta Negra)", "Avspärrningsstolpe (Svart Band)")),
            ("blue", names_of("Queue Stanchion (Blue Belt)", "Absperrpfosten (Blaues Band)",
                              "Poste Separador (Cinta Azul)", "Avspärrningsstolpe (Blått Band)"))):
        reg = "airport_queue_stanchion_" + colour
        post = (gp.octagon_y(8, 8, 3.0, 0, 0.6, "chrome", top=True, bottom=False)
                + gp.octagon_y(8, 8, 0.7, 0.6, 13.4, "chrome", top=False, bottom=False)
                + gp.octagon_y(8, 8, 1.25, 11.2, 15.6, "chrome", top=True, bottom=True))
        t = 0.15
        c = 8.0

        def belt(frm, to, faces, along):
            uv = {}
            for f in faces:
                if f in ("up", "down"):
                    continue
                u = gp._uv(f, frm, to)
                uv[f] = [u[0], 0, u[2], 16]
            return B(frm, to, "belt", faces, uv=uv)
        arms = {
            "north": belt((c - t, BELT_Y0, 0), (c + t, BELT_Y1, c - 1.2), ("east", "west", "up",
                                                                           "down"), "z"),
            "south": belt((c - t, BELT_Y0, c + 1.2), (c + t, BELT_Y1, 16), ("east", "west", "up",
                                                                             "down"), "z"),
            "west": belt((0, BELT_Y0, c - t), (c - 1.2, BELT_Y1, c + t), ("north", "south", "up",
                                                                          "down"), "x"),
            "east": belt((c + 1.2, BELT_Y0, c - t), (16, BELT_Y1, c + t), ("north", "south",
                                                                           "up", "down"), "x"),
        }
        parts = [("post", {}, post)] + [("arm_" + s, {s: True}, [arms[s]]) for s in
                                        ("north", "east", "south", "west")]
        tex = {"chrome": C.T("chrome"), "belt": C.T("belt_" + colour),
               "particle": C.T("chrome")}
        models, state = multipart(parts, reg, tex, facings=False)
        item = item_of(parts, tex, keep=lambda cnd: not cnd or cnd.get("east") or cnd.get("west"),
                       display=gui_display(0.6))
        C.add(reg, 'new BlockQueueStanchion("%s")' % reg, names, models, state, item=item,
              tab=TAB)


# ------------------------------------------------------------------------------------------
# Security
# ------------------------------------------------------------------------------------------
LANE_Y = 12.0   # the security lane's top: the X-ray's belt, the rollers and the divesting table


def security_line():
    F = False
    # the X-ray unit: a housing with a tunnel through it along x, the belt animated in it
    els = [B((0, 0, 2), (16, 11.4, 14), "lower", ALL, uv={"north": [0, 0, 16, 16],
                                                          "south": [0, 0, 16, 16]}),
           B((0, 11.4, 2), (16, 22, 3.6), "body", ALL, per={"north": "upper"},
             uv={"north": [0, 0, 16, 10.6]}),
           B((0, 11.4, 12.4), (16, 22, 14), "body", ALL),
           B((0, 18, 3.6), (16, 22, 12.4), "body", ("up", "down", "east", "west")),
           B((0, 11.4, 3.6), (16, LANE_Y, 12.4), "body", ("up",), per={"up": "belt"},
             uv={"up": [0, 0, 16, 16]}),
           B((0.6, LANE_Y, 3.6), (0.6, 18, 12.4), "curtain", ("east", "west"),
             uv={"east": [0, 0, 16, 16], "west": [0, 0, 16, 16]}),
           B((15.4, LANE_Y, 3.6), (15.4, 18, 12.4), "curtain", ("east", "west"),
             uv={"east": [0, 0, 16, 16], "west": [0, 0, 16, 16]}),
           # the operator's monitor on the south side
           B((9, 22, 10), (10, 24, 11), "black", SIDES),
           B((6, 23.5, 10.6), (13, 28, 11.2), "black", ALL, per={"south": "screen"},
             uv={"south": [0, 0, 16, 10]})]
    tex = {"lower": C.T("xray_lower"), "upper": C.T("xray_upper"), "body": C.T("kiosk_body"),
           "belt": C.T("xray_belt"), "curtain": C.T("xray_curtain"), "black": C.T("black"),
           "screen": C.T("agent_screen"), "particle": C.T("xray_lower")}
    models, state = multipart([("body", {}, els)], "airport_xray_scanner", tex)
    C.add("airport_xray_scanner",
          'new BlockSecurityLine("airport_xray_scanner", new double[]{0, 0, 2, 16, 16, 14}, '
          '22.0)',
          names_of("Security X-Ray Scanner", "Röntgen-Gepäckscanner",
                   "Escáner de Rayos X de Seguridad", "Röntgenskanner för Bagage"),
          models, state, item=item_of([("body", {}, els)], tex, display=gui_display(0.45, -2.0)),
          tab=TAB)

    def frame(top):
        """The lane's frame one block long: side rails, a pair of legs mid-block with a brace,
        the top given, and end plates only where the lane stops."""
        rail_faces = ("north", "south", "up", "down")
        body = [B((0, 9, 2.8), (16, 12.6, 3.6), "rails", rail_faces),
                B((0, 9, 12.4), (16, 12.6, 13.2), "rails", rail_faces),
                B((7.2, 0, 3), (8.8, 9, 4), "rails", SIDES),
                B((7.2, 0, 12), (8.8, 9, 13), "rails", SIDES),
                B((7.4, 3, 4), (8.6, 4, 12), "rails", ("north", "south", "up", "down"))] + top
        return [("body", {}, body),
                ("end_left", {"left": F}, [B((0, 8.6, 2.8), (0.6, 12.6, 13.2), "rails", ALL)]),
                ("end_right", {"right": F}, [B((15.4, 8.6, 2.8), (16, 12.6, 13.2), "rails",
                                               ALL)])]
    rollers_top = [B((0, 11.2, 3.6), (16, LANE_Y, 12.4), "rails", ("up", "down"),
                     per={"up": "rollers"}, uv={"up": [0, 0, 16, 16]})]
    divest_top = [B((0, 11.4, 3.6), (16, LANE_Y, 12.4), "top", ("up", "down")),
                  B((0, LANE_Y, 11.6), (16, 13.2, 12.4), "top", ("north", "up"))]
    for reg, top, names in (
            ("airport_security_roller", rollers_top,
             names_of("Security Roller Conveyor", "Rollenbahn (Sicherheitskontrolle)",
                      "Transportador de Rodillos de Seguridad", "Rullbana (Säkerhetskontroll)")),
            ("airport_divest_table", divest_top,
             names_of("Divesting Table", "Ablagetisch (Sicherheitskontrolle)",
                      "Mesa de Preparación de Seguridad", "Avlastningsbord (Säkerhetskontroll)"))):
        tex = {"rails": C.T("rails"), "rollers": C.T("rollers"), "top": C.T("divest_top"),
               "particle": C.T("rails")}
        parts = frame(top)
        models, state = multipart(parts, reg, tex)
        C.add(reg, 'new BlockSecurityLine("%s", new double[]{0, 0, 2.8, 16, 13.2, 13.2}, 0)' % reg,
              names, models, state, item=item_of(parts, tex, display=gui_display(0.6)), tab=TAB)


def trays():
    """Security trays: a stack of them, or one with a traveller's things in it. Set on the
    security lane (the block below is BlockSecurityLine), they drop onto its top (`low`)."""
    def stack(dy):
        h = 3.9
        return [B((3, dy, 5), (13, dy + h, 11), "side", SIDES + ("down",),
                  uv={f: [0, 0, 16, 16] for f in SIDES}),
                B((3, dy + h - 1.1, 5), (13, dy + h, 5.6), "side", ("up",)),
                B((3, dy + h - 1.1, 10.4), (13, dy + h, 11), "side", ("up",)),
                B((3, dy + h - 1.1, 5.6), (3.6, dy + h, 10.4), "side", ("up",)),
                B((12.4, dy + h - 1.1, 5.6), (13, dy + h, 10.4), "side", ("up",)),
                B((3.6, dy + h - 1.1, 5.6), (12.4, dy + h - 1.1, 10.4), "inside", ("up",),
                  uv={"up": [0, 0, 16, 16]})]

    def with_items(dy):
        h = 1.6
        els = [B((3, dy, 5), (13, dy + h, 11), "side", SIDES + ("down",),
                 uv={f: [0, 0, 16, 16] for f in SIDES}),
               B((3, dy + h - 1.1, 5), (13, dy + h, 5.6), "side", ("up",)),
               B((3, dy + h - 1.1, 10.4), (13, dy + h, 11), "side", ("up",)),
               B((3, dy + h - 1.1, 5.6), (3.6, dy + h, 10.4), "side", ("up",)),
               B((12.4, dy + h - 1.1, 5.6), (13, dy + h, 10.4), "side", ("up",)),
               B((3.6, dy + h - 1.1, 5.6), (12.4, dy + h - 1.1, 10.4), "inside", ("up",),
                 uv={"up": [0, 0, 16, 16]}),
               # a laptop, a folded jacket and a pair of shoes
               B((4, dy + 0.5, 6), (9, dy + 0.9, 9.6), "laptop", ALL, per={"up": "laptop"},
                 uv={"up": [0, 0, 16, 16]}),
               B((8.6, dy + 0.5, 6.2), (12.2, dy + 2.4, 8.8), "jacket"),
               B((9.2, dy + 0.5, 9.0), (10.6, dy + 1.5, 10.2), "shoe"),
               B((10.9, dy + 0.5, 9.0), (12.3, dy + 1.5, 10.2), "shoe")]
        return els
    for reg, draw, names in (
            ("airport_security_trays", stack,
             names_of("Security Trays (Stack)", "Sicherheitswannen (Stapel)",
                      "Bandejas de Seguridad (Pila)", "Säkerhetsbackar (Trave)")),
            ("airport_security_tray_items", with_items,
             names_of("Security Tray (With Belongings)", "Sicherheitswanne (Mit Gepäck)",
                      "Bandeja de Seguridad (Con Pertenencias)",
                      "Säkerhetsback (Med Tillhörigheter)"))):
        tex = {"side": C.T("tray_side"), "inside": C.T("tray_inside"), "laptop": C.T("laptop"),
               "jacket": C.T("jacket"), "shoe": C.T("shoe"), "particle": C.T("tray_side")}
        parts = [("floor", {"low": False}, draw(0.0)),
                 ("low", {"low": True}, draw(LANE_Y - 16))]
        models, state = multipart(parts, reg, tex)
        C.add(reg, 'new BlockSecurityTray("%s")' % reg, names, models, state,
              item=item_of(parts, tex, display=gui_display(0.9, 2.0)), tab=TAB)


def scanner():
    """A boarding pass scanner on a pedestal: the pass is read face down on the glass on top,
    and the screen on the front shows a tick or a cross (BlockBoardingPassScanner's `light`)."""
    reg = "airport_boarding_pass_scanner"
    els = [B((5.5, 0, 5.5), (10.5, 0.4, 10.5), "stainless", SIDES + ("up",)),
           B((7, 0.4, 7.5), (9, 11, 9.5), "stainless", SIDES),
           B((4.5, 11, 4.5), (11.5, 14, 11), "body", ALL, per={"up": "glass"},
             uv={"up": [0, 0, 16, 16]}),
           B((5.4, 11.5, 4.4), (10.6, 13.5, 4.5), "screen", ("north",),
             uv={"north": [0, 3, 16, 13]})]
    tex = {"stainless": C.T("stainless"), "body": C.T("graphite"), "glass": C.T("scanner_top"),
           "screen": C.T("scanner_idle"), "particle": C.T("graphite")}
    lights = {str(i): {"textures": {"screen": C.T("scanner_" + n)}}
              for i, n in enumerate(("idle", "ok", "no"))}
    fixture(reg, 'new BlockBoardingPassScanner("%s", new double[]{4.5, 0, 4.5, 11.5, 14, 11})'
            % reg,
            names_of("Boarding Pass Scanner", "Bordkartenleser", "Lector de Tarjetas de Embarque",
                     "Boardingkortsläsare"),
            els, tex, display=gui_display(0.8, 0.0), extra_state={"light": lights})


# ------------------------------------------------------------------------------------------
# Gate seating
# ------------------------------------------------------------------------------------------
SEATS = gp.SEATS   # BlockPlatformBench's seats: the same places


def seating_parts():
    F, T = False, True
    beam_faces = ("north", "south", "up", "down")

    def armrest(x):
        return [B((x - 0.35, 5.2, 8.4), (x + 0.35, 10.6, 9.2), "chrome", SIDES),
                B((x - 0.35, 10.6, 3.6), (x + 0.35, 11.2, 9.2), "chrome", ("east", "west",
                                                                            "north", "down")),
                B((x - 0.6, 11.2, 3.4), (x + 0.6, 11.8, 9.4), "pad")]
    body = [B((1, 3.2, 8.2), (15, 4.6, 9.6), "beam", beam_faces),
            B((7.2, 0, 8.3), (8.8, 3.2, 9.5), "beam", SIDES),
            B((6.2, 0, 4.2), (9.8, 0.5, 13.2), "beam", SIDES + ("up",))]
    for x0, x1 in SEATS:
        xc = (x0 + x1) / 2
        body += [B((x0, 6.2, 2.8), (x1, 7.8, 9.6), "seat"),
                 B((x0 + 0.4, 5.6, 3.2), (x1 - 0.4, 6.2, 9.4), "chrome", SIDES + ("down",)),
                 B((xc - 0.8, 4.6, 6.6), (xc + 0.8, 5.6, 9.2), "chrome", SIDES),
                 B((x0, 8.0, 9.6), (x1, 15.4, 11.0), "seat",
                   rot=("x", 22.5, (xc, 8.0, 9.6)))]
    body += armrest(8)
    return [("body", {}, body),
            ("join_left", {"left": T}, [B((0, 3.2, 8.2), (1, 4.6, 9.6), "beam", beam_faces)]
             + armrest(0)),
            ("join_right", {"right": T}, [B((15, 3.2, 8.2), (16, 4.6, 9.6), "beam",
                                            beam_faces)]),
            ("end_left", {"left": F}, [B((0.5, 3.2, 8.2), (1, 4.6, 9.6), "beam",
                                         beam_faces + ("west",))] + armrest(0.8)),
            ("end_right", {"right": F}, [B((15, 3.2, 8.2), (15.5, 4.6, 9.6), "beam",
                                           beam_faces + ("east",))] + armrest(15.2))]


def seating():
    for colour, names in (
            ("black", names_of("Airport Seating (Black)", "Flughafen-Sitzbank (Schwarz)",
                               "Asientos de Aeropuerto (Negro)", "Flygplatsbänk (Svart)")),
            ("blue", names_of("Airport Seating (Blue)", "Flughafen-Sitzbank (Blau)",
                              "Asientos de Aeropuerto (Azul)", "Flygplatsbänk (Blå)"))):
        reg = "airport_seating_" + colour
        tex = {"beam": C.T("beam"), "chrome": C.T("chrome"), "seat": C.T("seat_" + colour),
               "pad": C.T("black"), "particle": C.T("seat_" + colour)}
        parts = seating_parts()
        models, state = multipart(parts, reg, tex)
        C.add(reg, 'new BlockPlatformBench("%s", new double[]{0, 0, 2.8, 16, 15, 11})' % reg,
              names, models, state, item=item_of(parts, tex), tab=TAB)


# ------------------------------------------------------------------------------------------
# The flight information boards
# ------------------------------------------------------------------------------------------
def boards():
    """A departures or arrivals monitor: a slim landscape screen on the wall (z = 16), or hung
    on rods where there is no wall behind it (BlockFlightBoard's `hung`). The screen's texture
    is its header and bands; the renderer draws the same texture lit, and the rows on it."""
    body = [B((0, 3.2, 15), (16, 12.8, 16), "bezel", ALL),
            B((SCREEN_X0, SCREEN_Y0, SCREEN_Z), (SCREEN_X1, SCREEN_Y1, 15), "bezel",
              ("north", "east", "west", "up", "down"), per={"north": "screen"},
              uv={"north": win(FIDS_W, FIDS_H, FIDS_W)})]
    rods = []
    for x in (3.0, 13.0):
        rods += [gp.rod(x, 15.5, 12.8, 15.6), B((x - 0.8, 15.6, 14.7), (x + 0.8, 16, 16.0),
                                                 "steel", SIDES + ("down",))]
    for reg, screen, arrivals, names in (
            ("airport_flight_board_departures", "fids_departures", "false",
             names_of("Flight Information Board (Departures)", "Fluginformationsanzeige (Abflug)",
                      "Pantalla de Información de Vuelos (Salidas)",
                      "Flyginformationstavla (Avgångar)")),
            ("airport_flight_board_arrivals", "fids_arrivals", "true",
             names_of("Flight Information Board (Arrivals)", "Fluginformationsanzeige (Ankunft)",
                      "Pantalla de Información de Vuelos (Llegadas)",
                      "Flyginformationstavla (Ankomster)"))):
        tex = {"bezel": C.T("bezel"), "screen": C.T(screen), "steel": C.T("steel"),
               "particle": C.T("bezel")}
        parts = [("monitor", {}, body), ("rods", {"hung": True}, rods)]
        models, state = multipart(parts, reg, tex)
        C.add(reg, 'new BlockFlightBoard("%s", new double[]{0, 3, 14.8, 16, 13, 16}, %s)'
              % (reg, arrivals), names, models, state,
              item=item_of(parts, tex, display=gui_display(0.6, 0.0, (0, 180, 0))), tab=TAB)


# BlockFlightBoardLarge / TileEntityFlightBoardLargeRenderer: a cell's slab against the back of
# its block, its face at z 13 (the renderer's FACE_Z), and the frame on the board's outer edges
# standing 0.6 proud of it. The renderer draws the screen 1.4 in from the board's outer edges
# (INSET), inside the frame.
BOARD_Z0 = 13.0
BOARD_FRAME = 0.8
BOARD_LIP = 0.6
BOARD_FRAME_BACK = 15.6   # short of the slab's back (z 16), so the two backs never meet


def boards_large():
    """The large flight boards: cells that join into one screen up to 8 x 5
    (BlockFlightBoardLarge), each a dark bezel slab with a frame only on the board's outer edges
    (`edge_*`, actual state). Nothing of the screen is baked: the controller's renderer draws it
    from the small board's texture, with every word. The item shows a cell with the small board's
    screen on it, which no placed cell draws."""
    f, z0, z1 = BOARD_FRAME, BOARD_Z0 - BOARD_LIP, BOARD_FRAME_BACK
    # facing north the reader looks south, so the reader's left is east (x = 16)
    parts = [
        ("body", {}, [B((0, 0, BOARD_Z0), (16, 16, 16), "bezel", ("north", "south"))]),
        ("edge_left", {"edge_left": True}, [B((16 - f, 0, z0), (16, 16, z1), "frame", ALL)]),
        ("edge_right", {"edge_right": True}, [B((0, 0, z0), (f, 16, z1), "frame", ALL)]),
        ("edge_top", {"edge_top": True}, [B((0, 16 - f, z0), (16, 16, z1), "frame", ALL)]),
        ("edge_bottom", {"edge_bottom": True}, [B((0, 0, z0), (16, f, z1), "frame", ALL)]),
    ]
    tex = {"bezel": C.T("bezel"), "frame": C.T("graphite"), "particle": C.T("bezel")}
    # the two boards' cells are the same: one set of models, written with the first
    models, state = multipart(parts, "airport_flight_board_large", tex)
    for reg, screen, arrivals, names in (
            ("airport_flight_board_large_departures", "fids_departures", "false",
             names_of("Large Flight Board (Departures)",
                      "Große Fluginformationsanzeige (Abflug)",
                      "Pantalla Grande de Información de Vuelos (Salidas)",
                      "Stor Flyginformationstavla (Avgångar)")),
            ("airport_flight_board_large_arrivals", "fids_arrivals", "true",
             names_of("Large Flight Board (Arrivals)",
                      "Große Fluginformationsanzeige (Ankunft)",
                      "Pantalla Grande de Información de Vuelos (Llegadas)",
                      "Stor Flyginformationstavla (Ankomster)"))):
        item = item_of(parts, tex, keep=lambda cond: True,
                       display=gui_display(0.6, 0.0, (0, 180, 0)))
        item["textures"]["screen"] = C.T(screen)
        item["elements"].append(
            B((1.4, 3.6, BOARD_Z0 - 0.3), (14.6, 12.4, BOARD_Z0), "screen", ("north",),
              uv={"north": win(FIDS_W, FIDS_H, FIDS_W)}))
        C.add(reg, 'new BlockFlightBoardLarge("%s", %s)' % (reg, arrivals), names, models, state,
              item=item, tab=TAB)
        models = {}


# ------------------------------------------------------------------------------------------
# Baggage claim
# ------------------------------------------------------------------------------------------
SKIRT_H = 7.6
BELT_TOP = 7.0


def carousel():
    """A baggage carousel, one block at a time: pieces side by side join on all four sides
    (north to west, world sides, actual state) into a loop of any size. A side with no carousel
    beyond it gets the stainless skirt and its rubber bumper. The top depends on which sides are
    open: one open side is a straight run of plates moving clockwise round the loop (along the
    open side, seen from above), two open sides that meet are a corner the plates turn on, none
    is the middle island, and anything else a round end plate."""
    reg = "airport_baggage_carousel"
    tex = {"belt": C.T("carousel_belt"), "corner": C.T("carousel_corner"),
           "end": C.T("carousel_end"), "island": C.T("carousel_island"),
           "skirt": C.T("carousel_skirt"), "lip": C.T("rubber"), "particle": C.T("carousel_skirt")}
    models = {}
    rules = []

    def top(name, texname, y1=BELT_TOP):
        m = "%s_%s" % (reg, name)
        models[m] = lc.model(tex, [B((0, y1 - 0.8, 0), (16, y1, 16), texname, ("up",),
                                     uv={"up": [0, 0, 16, 16]})])
        return C.M(m)
    straight = top("straight", "belt")
    corner = top("corner", "corner")
    end = top("end", "end")
    island = top("island", "island", 6.6)
    sm = "%s_skirt" % reg
    models[sm] = lc.model(tex, [B((0, 0, 0), (16, SKIRT_H, 1.0), "skirt", ALL,
                                  per={"up": "lip"}, uv={"north": [0, 0, 16, 16],
                                                         "south": [0, 0, 16, 16]})])
    turn = {"north": 0, "east": 90, "south": 180, "west": 270}
    for side, y in turn.items():
        a = {"model": C.M(sm)}
        if y:
            a["y"] = y
        rules.append({"when": {side: "false"}, "apply": a})
    order = ("north", "east", "south", "west")
    for mask in range(16):
        joined = {s: bool(mask >> i & 1) for i, s in enumerate(order)}
        open_ = [s for s in order if not joined[s]]
        if not open_:
            model, y = island, 0
        elif len(open_) == 1:
            model, y = straight, turn[open_[0]]
        elif len(open_) == 2 and set(open_) == {"north", "south"}:
            model, y = straight, 0
        elif len(open_) == 2 and set(open_) == {"east", "west"}:
            model, y = straight, 90
        elif len(open_) == 2:
            # a corner: named by its first open side going clockwise (north-east is north's)
            a, b = open_
            first = a if order[(order.index(a) + 1) % 4] == b else b
            model, y = corner, turn[first]
        else:
            model, y = end, 0
        apply = {"model": model}
        if y:
            apply["y"] = y
        rules.append({"when": {s: ("true" if joined[s] else "false") for s in order},
                      "apply": apply})
    item = lc.model(tex, models["%s_straight" % reg]["elements"] + models[sm]["elements"])
    item["display"] = gui_display(0.6)
    C.add(reg, 'new BlockBaggageCarousel("%s")' % reg,
          names_of("Baggage Carousel", "Gepäckband", "Cinta de Equipaje", "Bagageband"),
          models, {"multipart": rules}, item=item, tab=TAB)


# ------------------------------------------------------------------------------------------
# Signs
# ------------------------------------------------------------------------------------------
def wayfinding_signs():
    en = {"gates": "Gates", "arrivals": "Arrivals", "check_in": "Check-In",
          "baggage_claim": "Baggage Claim", "ground_transport": "Ground Transport"}
    de = {"gates": "Flugsteige", "arrivals": "Ankunft", "check_in": "Check-in",
          "baggage_claim": "Gepäckausgabe", "ground_transport": "Bus und Taxi"}
    es = {"gates": "Puertas", "arrivals": "Llegadas", "check_in": "Facturación",
          "baggage_claim": "Recogida de Equipaje", "ground_transport": "Transporte Terrestre"}
    sv = {"gates": "Gater", "arrivals": "Ankomster", "check_in": "Incheckning",
          "baggage_claim": "Bagageutlämning", "ground_transport": "Buss och Taxi"}
    face = win(WAY_W, WAY_H, 256)
    for wid, _, _ in WAYFINDING:
        reg = "airport_sign_" + wid
        els = gp.hanging_panel(-4, 20, 5, 11, face, face, rods=(2.0, 14.0))
        tex = {"face": C.T("way_" + wid), "back": C.T("way_" + wid + "_back"),
               "edge": C.T("graphite"), "steel": C.T("steel"), "particle": C.T("way_" + wid)}
        fixture(reg, 'new BlockPlatformFixture("%s", new double[]{0, 5, 7, 16, 16, 9})' % reg,
                names_of("Airport Sign (%s)" % en[wid], "Flughafenschild (%s)" % de[wid],
                         "Letrero de Aeropuerto (%s)" % es[wid],
                         "Flygplatsskylt (%s)" % sv[wid]),
                els, tex, display=gui_display(0.45, 0.0))


# ------------------------------------------------------------------------------------------
# The large hanging sign (transit.wayfinding.BlockWayfindingPanel)
# ------------------------------------------------------------------------------------------
# Cells of it side by side and stacked join into one panel up to 16 x 6 blocks. Each cell's baked
# model is only the graphite slab and, on the panel's outer edges, its frame; the legend, the
# pictogram, the arrow and the hanger rods are TileEntityWayfindingPanelRenderer's, drawn once
# across the whole panel by its bottom-left cell. The renderer draws its sprites over the
# slab's face, so the face must stay where it is: z 6 (front) and 10 (back), the frame's inner
# edge 0.8 in from an outer edge (WayfindingPanelLayout.FRAME).
PANEL_Z0, PANEL_Z1 = 6.0, 10.0
PANEL_FRAME = 0.8          # the frame's width, in sixteenths
PANEL_LIP = 0.6            # how far the frame stands proud of each face

# The pictograms the renderer can draw, in WayfindingPictogram's order (the Java stores them by
# this id, never by ordinal). Each is a white square with the art in black, which the renderer
# tints: yellow for the airport scheme, white for the others.
PANEL_PICTOGRAMS = ("depart", "arrive", "checkin", "baggage", "ground", "train", "bus", "taxi",
                    "restroom", "exit")
PANEL_SPRITE = 64


def train_front(img, x, y, s, colour, bg):
    """A train seen from the front: body, windscreen, two lamps, on rails."""
    rect(img, x + s * 0.24, y + s * 0.1, x + s * 0.76, y + s * 0.72, colour)
    rect(img, x + s * 0.3, y + s * 0.18, x + s * 0.7, y + s * 0.42, bg)
    lc.disc(img, x + s * 0.35, y + s * 0.58, s * 0.05, bg)
    lc.disc(img, x + s * 0.65, y + s * 0.58, s * 0.05, bg)
    gp.line(img, x + s * 0.32, y + s * 0.72, x + s * 0.2, y + s * 0.88, colour, s * 0.06)
    gp.line(img, x + s * 0.68, y + s * 0.72, x + s * 0.8, y + s * 0.88, colour, s * 0.06)
    rect(img, x + s * 0.12, y + s * 0.86, x + s * 0.88, y + s * 0.91, colour)


def restroom(img, x, y, s, colour):
    """A man and a woman either side of a divider: the generic restroom sign."""
    person(img, x + s * 0.27, y + s * 0.12, s * 0.76, colour)
    cx, h, top = x + s * 0.73, s * 0.76, y + s * 0.12
    r = h * 0.13
    lc.disc(img, cx, top + r, r, colour)
    ImageDraw.Draw(img).polygon(
        [(cx, top + h * 0.28), (cx - h * 0.2, top + h * 0.68), (cx + h * 0.2, top + h * 0.68)],
        fill=lc.clamp(colour) + (255,))
    rect(img, cx - h * 0.12, top + h * 0.66, cx - h * 0.03, top + h, colour)
    rect(img, cx + h * 0.03, top + h * 0.66, cx + h * 0.12, top + h, colour)
    rect(img, x + s * 0.48, y + s * 0.08, x + s * 0.52, y + s * 0.92, colour)


def running_exit(img, x, y, s, colour, bg):
    """A figure running out through a doorway: the generic emergency exit sign."""
    rect(img, x + s * 0.6, y + s * 0.1, x + s * 0.9, y + s * 0.9, colour)
    rect(img, x + s * 0.66, y + s * 0.16, x + s * 0.84, y + s * 0.9, bg)
    w = s * 0.085

    def at(u, v):
        return x + s * u, y + s * v
    lc.disc(img, *at(0.43, 0.2), r=s * 0.075, colour=colour)
    gp.line(img, *at(0.39, 0.33), *at(0.31, 0.58), colour=colour, width=w)       # torso
    gp.line(img, *at(0.38, 0.36), *at(0.52, 0.46), colour=colour, width=w)       # front arm
    gp.line(img, *at(0.52, 0.46), *at(0.6, 0.38), colour=colour, width=w)
    gp.line(img, *at(0.38, 0.36), *at(0.24, 0.4), colour=colour, width=w)        # back arm
    gp.line(img, *at(0.24, 0.4), *at(0.17, 0.5), colour=colour, width=w)
    gp.line(img, *at(0.31, 0.58), *at(0.46, 0.7), colour=colour, width=w)        # front leg
    gp.line(img, *at(0.46, 0.7), *at(0.44, 0.88), colour=colour, width=w)
    gp.line(img, *at(0.31, 0.58), *at(0.2, 0.74), colour=colour, width=w)        # back leg
    gp.line(img, *at(0.2, 0.74), *at(0.08, 0.74), colour=colour, width=w)


def panel_pictogram(kind):
    """One of the large sign's pictograms: black art on a white square, PANEL_SPRITE a side."""
    s = PANEL_SPRITE
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    if kind in ("depart", "arrive", "checkin", "baggage", "ground"):
        pictogram(img, kind, 0, 0, s, bg=WHITE)
        return img
    rect(img, 0, 0, s, s, WHITE)
    if kind == "train":
        train_front(img, 0, 0, s, BLACK, WHITE)
    elif kind == "bus":
        bus(img, 4, s * 0.24, s - 8, s * 0.52, BLACK, WHITE)
    elif kind == "taxi":
        taxi(img, 4, s * 0.2, s - 8, s * 0.6, BLACK, WHITE)
    elif kind == "restroom":
        restroom(img, 0, 0, s, BLACK)
    elif kind == "exit":
        running_exit(img, 0, 0, s, BLACK, WHITE)
    return img


def panel_arrow():
    """The large sign's arrow: white, pointing right, on clear; the renderer tints and turns it."""
    s = PANEL_SPRITE
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gp.arrow(img, 4, 12, s - 8, 40, WHITE, True)
    return img


def wayfinding_panel():
    reg = "airport_wayfinding_panel"
    for kind in PANEL_PICTOGRAMS:
        C.texture("wayfinding_picto_" + kind)(lambda k=kind: panel_pictogram(k))
    C.texture("wayfinding_arrow")(panel_arrow)
    f, lip = PANEL_FRAME, PANEL_LIP
    z0, z1 = PANEL_Z0 - lip, PANEL_Z1 + lip
    # facing north the reader looks south, so the reader's left is east (x = 16)
    parts = [
        ("body", {}, [B((0, 0, PANEL_Z0), (16, 16, PANEL_Z1), "panel", ("north", "south"))]),
        ("edge_left", {"edge_left": True}, [B((16 - f, 0, z0), (16, 16, z1), "frame", ALL)]),
        ("edge_right", {"edge_right": True}, [B((0, 0, z0), (f, 16, z1), "frame", ALL)]),
        ("edge_top", {"edge_top": True}, [B((0, 16 - f, z0), (16, 16, z1), "frame", ALL)]),
        ("edge_bottom", {"edge_bottom": True}, [B((0, 0, z0), (16, f, z1), "frame", ALL)]),
    ]
    tex = {"panel": C.T("graphite"), "frame": C.T("stainless"), "particle": C.T("graphite")}
    models, state = multipart(parts, reg, tex)
    item = item_of(parts, tex, keep=lambda cond: True, display=gui_display(0.6))
    # The renderer's sprites are on the atlas through TileEntityWayfindingPanelRenderer's
    # texture stitch handler, since no face of a model draws them. They are named here too, where
    # nothing draws with them, so atlas_budget.py counts them.
    for kind in PANEL_PICTOGRAMS:
        item["textures"]["picto_" + kind] = C.T("wayfinding_picto_" + kind)
    item["textures"]["arrow"] = C.T("wayfinding_arrow")
    C.add(reg, 'new BlockWayfindingPanel("%s")' % reg,
          names_of("Large Hanging Sign", "Großes Hängeschild", "Letrero Colgante Grande",
                   "Stor Hängande Skylt"),
          models, state, item=item, tab=TAB)

    def lang(key, en, de, es, sv):
        C.add_lang("gui.csm.wayfinding." + key, (en, de, es, sv))
    lang("line1", "Top line", "Obere Zeile", "Línea superior", "Övre rad")
    lang("line2", "Bottom line (optional)", "Untere Zeile (optional)",
         "Línea inferior (opcional)", "Nedre rad (valfri)")
    lang("pictogram", "Pictogram: %s", "Piktogramm: %s", "Pictograma: %s", "Piktogram: %s")
    lang("arrow", "Arrow: %s", "Pfeil: %s", "Flecha: %s", "Pil: %s")
    lang("scheme", "Colours: %s", "Farben: %s", "Colores: %s", "Färger: %s")
    lang("double", "Both sides: %s", "Beide Seiten: %s", "Ambas caras: %s", "Båda sidor: %s")
    lang("preset", "Preset: %s", "Vorlage: %s", "Plantilla: %s", "Mall: %s")
    lang("preset.none", "Apply a preset", "Vorlage anwenden", "Aplicar una plantilla",
         "Använd en mall")
    lang("size", "Sign: %d x %d blocks", "Schild: %d x %d Blöcke", "Letrero: %d x %d bloques",
         "Skylt: %d x %d block")
    lang("hint", "Panels side by side and stacked join into one sign, up to 16 x 6",
         "Tafeln neben- und übereinander bilden ein Schild, bis 16 x 6",
         "Los paneles contiguos y apilados forman un letrero, hasta 16 x 6",
         "Paneler bredvid och ovanpå varandra blir en skylt, upp till 16 x 6")
    for key, en, de, es, sv in (
            ("none", "None", "Keins", "Ninguno", "Inget"),
            ("depart", "Departures", "Abflug", "Salidas", "Avgångar"),
            ("arrive", "Arrivals", "Ankunft", "Llegadas", "Ankomster"),
            ("checkin", "Check-in", "Check-in", "Facturación", "Incheckning"),
            ("baggage", "Baggage claim", "Gepäckausgabe", "Recogida de equipaje",
             "Bagageutlämning"),
            ("ground", "Ground transport", "Bus und Taxi", "Transporte terrestre",
             "Buss och taxi"),
            ("train", "Train", "Zug", "Tren", "Tåg"),
            ("bus", "Bus", "Bus", "Autobús", "Buss"),
            ("taxi", "Taxi", "Taxi", "Taxi", "Taxi"),
            ("restroom", "Restrooms", "Toiletten", "Aseos", "Toaletter"),
            ("exit", "Exit", "Ausgang", "Salida", "Utgång")):
        lang("pictogram." + key, en, de, es, sv)
    for key, en, de, es, sv in (
            ("none", "None", "Keiner", "Ninguna", "Ingen"),
            ("left", "Left", "Links", "Izquierda", "Vänster"),
            ("right", "Right", "Rechts", "Derecha", "Höger"),
            ("up", "Up (ahead)", "Oben (geradeaus)", "Arriba (de frente)", "Upp (rakt fram)"),
            ("down", "Down", "Unten", "Abajo", "Ned"),
            ("up_left", "Up left", "Links oben", "Arriba a la izquierda", "Upp vänster"),
            ("up_right", "Up right", "Rechts oben", "Arriba a la derecha", "Upp höger"),
            ("down_left", "Down left", "Links unten", "Abajo a la izquierda", "Ned vänster"),
            ("down_right", "Down right", "Rechts unten", "Abajo a la derecha", "Ned höger")):
        lang("arrow." + key, en, de, es, sv)
    for key, en, de, es, sv in (
            ("airport", "Airport", "Flughafen", "Aeropuerto", "Flygplats"),
            ("metro", "Metro", "Metro", "Metro", "Tunnelbana"),
            ("exit", "Exit", "Ausgang", "Salida", "Utgång"),
            ("info", "Information", "Information", "Información", "Information")):
        lang("scheme." + key, en, de, es, sv)


# BlockGateSign: the panel and its three cells, facing north, in sixteenths
GATE_X0, GATE_X1 = 2.0, 14.0
GATE_Y0, GATE_SPLIT_Y, GATE_Y1 = 3.0, 8.0, 10.0
GATE_LETTER_W = 4.5


def gate_sign():
    """The hanging gate sign: GATE across the top, then the gate's letter (black on yellow)
    and number (yellow on charcoal), on both faces. The letter and the number are separate cells
    so the blockstate can swap each one's texture (`letter` A to D, `number` 1 to 20) without a
    texture for every gate; the back's cells are laid out mirrored, so it reads the same."""
    reg = "airport_gate_sign"
    z0, z1 = 7.3, 8.7
    lx = GATE_X0 + GATE_LETTER_W
    k = GATE_K
    label_uv = [0, 0, (GATE_X1 - GATE_X0) * k * 16.0 / 128, (GATE_Y1 - GATE_SPLIT_Y) * k * 16.0 / 128]
    letter_uv = [0, 0, GATE_LETTER_W * k * 16.0 / 64, (GATE_SPLIT_Y - GATE_Y0) * k * 16.0 / 64]
    number_uv = [0, 0, (GATE_X1 - lx) * k * 16.0 / 64, (GATE_SPLIT_Y - GATE_Y0) * k * 16.0 / 64]
    # the front's viewer has high x on their left, the back's low x
    fx = GATE_X1 - GATE_LETTER_W
    els = [B((GATE_X0, GATE_Y0, z0), (GATE_X1, GATE_Y1, z1), "edge",
             ("east", "west", "up", "down")),
           B((GATE_X0, GATE_SPLIT_Y, z0), (GATE_X1, GATE_Y1, z0), "label", ("north",),
             uv={"north": label_uv}),
           B((fx, GATE_Y0, z0), (GATE_X1, GATE_SPLIT_Y, z0), "letter", ("north",),
             uv={"north": letter_uv}),
           B((GATE_X0, GATE_Y0, z0), (fx, GATE_SPLIT_Y, z0), "number", ("north",),
             uv={"north": number_uv}),
           B((GATE_X0, GATE_SPLIT_Y, z1), (GATE_X1, GATE_Y1, z1), "label", ("south",),
             uv={"south": label_uv}),
           B((GATE_X0, GATE_Y0, z1), (lx, GATE_SPLIT_Y, z1), "letter", ("south",),
             uv={"south": letter_uv}),
           B((lx, GATE_Y0, z1), (GATE_X1, GATE_SPLIT_Y, z1), "number", ("south",),
             uv={"south": number_uv})]
    for x in (4.0, 12.0):
        els += [gp.rod(x, 8, GATE_Y1, 15.6), gp.ceiling_plate(x, 8, 0.8)]
    tex = {"edge": C.T("graphite"), "label": C.T("gate_label"),
           "letter": C.T("gate_letter_a"), "number": C.T("gate_number_1"),
           "steel": C.T("steel"), "particle": C.T("gate_label")}
    extra = {"letter": {ch.lower(): {"textures": {"letter": C.T("gate_letter_" + ch.lower())}}
                        for ch in GATE_LETTERS},
             "number": {str(n): {"textures": {"number": C.T("gate_number_%d" % n)}}
                        for n in range(1, GATE_NUMBERS + 1)}}
    fixture(reg, 'new BlockGateSign("%s", new double[]{2, 3, 7, 14, 16, 9})' % reg,
            names_of("Gate Sign", "Flugsteigschild", "Letrero de Puerta de Embarque",
                     "Gateskylt"),
            els, tex, display=gui_display(0.7, 1.0), extra_state=extra)


# ------------------------------------------------------------------------------------------
# Luggage carts
# ------------------------------------------------------------------------------------------
def luggage_cart():
    """A luggage cart, nose north: a ribbed bed on four small wheels, a nose stop, and the
    frame at the back up to the handle."""
    reg = "airport_luggage_cart"
    els = [B((3.5, 1.6, 1), (12.5, 2.4, 13.4), "frame", ALL, per={"up": "bed"},
             uv={"up": [0, 0, 16, 16]}),
           B((3.5, 2.4, 1), (12.5, 4.6, 1.7), "frame"),
           B((3.5, 2.4, 1.7), (4.1, 3.2, 13), "frame", ("east", "west", "up")),
           B((11.9, 2.4, 1.7), (12.5, 3.2, 13), "frame", ("east", "west", "up")),
           B((3.6, 1.6, 13), (4.4, 14.4, 13.8), "frame", SIDES),
           B((11.6, 1.6, 13), (12.4, 14.4, 13.8), "frame", SIDES),
           B((4.4, 6, 13.1), (11.6, 6.7, 13.7), "frame", ("north", "south", "up", "down")),
           B((4.4, 10, 13.1), (11.6, 10.7, 13.7), "frame", ("north", "south", "up", "down")),
           B((3, 14.2, 13.6), (13, 15.4, 14.8), "grip")]
    for x in (4.0, 11.0):
        for z in (2.2, 11.6):
            els.append(B((x, 0, z), (x + 1.0, 1.6, z + 1.4), "rubber", SIDES))
    tex = {"frame": C.T("cart_frame"), "bed": C.T("cart_bed"), "grip": C.T("cart_grip"),
           "rubber": C.T("rubber"), "particle": C.T("cart_frame")}
    fixture(reg, 'new BlockPlatformFixture("%s", new double[]{3, 0, 1, 13, 15.4, 14.8})' % reg,
            names_of("Luggage Cart", "Gepäckwagen", "Carrito de Equipaje", "Bagagevagn"),
            els, tex, display=gui_display(0.7, 0.0))


def cart_rack():
    """A rack of nested luggage carts between two guide rails, running along the player's left
    and right (BlockPlatformRun): a hoop at the carts' nose end (left) and a post with the CARTS
    sign where they are taken (right), only at the ends of a row."""
    reg = "airport_cart_rack"
    F = False
    body = [B((0, 1.6, 4.2), (16, 2.4, 11.8), "frame", ("up", "down", "north", "south"),
              per={"up": "bed"}, uv={"up": [0, 0, 16, 16]}),
            B((0, 7, 2.4), (16, 7.8, 3.0), "post", ("north", "south", "up", "down")),
            B((0, 7, 13), (16, 7.8, 13.6), "post", ("north", "south", "up", "down")),
            B((7.6, 0, 2.4), (8.4, 7, 3.0), "post", SIDES),
            B((7.6, 0, 13), (8.4, 7, 13.6), "post", SIDES)]
    for x in (2.2, 7.5, 12.8):
        body += [B((x, 1.6, 4.3), (x + 0.8, 14.4, 5.1), "frame", SIDES),
                 B((x, 1.6, 10.9), (x + 0.8, 14.4, 11.7), "frame", SIDES),
                 B((x - 0.2, 14.2, 3.8), (x + 1.0, 15.4, 12.2), "grip"),
                 B((x + 0.1, 0, 4.6), (x + 0.9, 1.6, 5.6), "rubber", SIDES),
                 B((x + 0.1, 0, 10.4), (x + 0.9, 1.6, 11.4), "rubber", SIDES)]
    hoop = [B((0, 0, 2.4), (0.6, 7.8, 3.0), "post", SIDES + ("up",)),
            B((0, 0, 13), (0.6, 7.8, 13.6), "post", SIDES + ("up",)),
            B((0, 7, 3.0), (0.6, 7.8, 13), "post", ("west", "east", "up", "down"))]
    sign = [B((13.6, 0, 13.6), (15.2, 17, 15.2), "post", SIDES + ("up",)),
            B((12.4, 17, 13.4), (16, 22, 14.2), "post", ALL, per={"north": "sign"},
              uv={"north": [0, 0, 16, 16 * 5.0 / 3.6]})]
    parts = [("body", {}, body), ("end_left", {"left": F}, hoop),
             ("end_right", {"right": F}, sign)]
    tex = {"frame": C.T("cart_frame"), "bed": C.T("cart_bed"), "grip": C.T("cart_grip"),
           "rubber": C.T("rubber"), "post": C.T("cart_post"), "sign": C.T("cart_sign"),
           "particle": C.T("cart_frame")}
    models, state = multipart(parts, reg, tex)
    C.add(reg, 'new BlockPlatformRun("%s", new double[]{0, 0, 2.4, 16, 15.4, 13.6})' % reg,
          names_of("Luggage Cart Rack", "Gepäckwagen-Station", "Estación de Carritos de Equipaje",
                   "Bagagevagnsställ"),
          models, state, item=item_of(parts, tex, display=gui_display(0.55)), tab=TAB)


# ------------------------------------------------------------------------------------------
# The boarding pass, and the lang the Java reads
# ------------------------------------------------------------------------------------------
C.add_item("boarding_pass", "new ItemBoardingPass()",
           names_of("Boarding Pass", "Bordkarte", "Tarjeta de Embarque", "Boardingkort"),
           "boarding_pass", tab=TAB)

C.add_lang("csm.transit.airline", (
    "Check-in desk: %s", "Check-in-Schalter: %s", "Mostrador de facturación: %s",
    "Incheckningsdisk: %s"))
C.add_lang("csm.transit.gate", ("Gate %s", "Flugsteig %s", "Puerta %s", "Gate %s"))
C.add_lang("csm.transit.kiosk.printed", (
    "Boarding pass printed: %s to %s, gate %s",
    "Bordkarte gedruckt: %s nach %s, Flugsteig %s",
    "Tarjeta de embarque impresa: %s a %s, puerta %s",
    "Boardingkort utskrivet: %s till %s, gate %s"))
C.add_lang("csm.transit.pass.flight", (
    "Flight %s to %s", "Flug %s nach %s", "Vuelo %s a %s", "Flyg %s till %s"))
C.add_lang("csm.transit.pass.departs", (
    "Departs %s from gate %s", "Abflug %s von Flugsteig %s", "Sale a las %s por la puerta %s",
    "Avgår %s från gate %s"))
C.add_lang("csm.transit.pass.seat", ("Seat %s", "Sitzplatz %s", "Asiento %s", "Plats %s"))
C.add_lang("csm.transit.pass.boarded", ("Boarded", "Eingestiegen", "Embarcado", "Ombordstigen"))
C.add_lang("csm.transit.pass.blank", (
    "Printed at a self check-in kiosk", "Wird am Check-in-Automaten gedruckt",
    "Se imprime en un quiosco de autofacturación", "Skrivs ut i en incheckningsautomat"))
C.add_lang("csm.transit.scanner.ok", (
    "Boarding: %s to %s, seat %s", "Einsteigen: %s nach %s, Sitzplatz %s",
    "Embarque: %s a %s, asiento %s", "Ombordstigning: %s till %s, plats %s"))
C.add_lang("csm.transit.scanner.boarded", (
    "This boarding pass has already been used", "Diese Bordkarte wurde bereits benutzt",
    "Esta tarjeta de embarque ya se ha usado", "Det här boardingkortet har redan använts"))
C.add_lang("csm.transit.scanner.departed", (
    "Flight %s has already departed", "Flug %s ist bereits abgeflogen",
    "El vuelo %s ya ha salido", "Flyg %s har redan avgått"))
C.add_lang("csm.transit.scanner.cancelled", (
    "Flight %s is cancelled", "Flug %s ist annulliert", "El vuelo %s está cancelado",
    "Flyg %s är inställt"))
C.add_lang("csm.transit.scanner.none", (
    "Scan a boarding pass", "Bordkarte scannen", "Escanee una tarjeta de embarque",
    "Skanna ett boardingkort"))

register_textures()
checkin()
kiosk()
stanchions()
security_line()
trays()
scanner()
seating()
boards()
boards_large()
carousel()
gate_sign()
wayfinding_signs()
wayfinding_panel()
luggage_cart()
cart_rack()

if __name__ == "__main__":
    sys.exit(C.main())
