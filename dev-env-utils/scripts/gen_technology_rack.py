#!/usr/bin/env python3
"""Every asset the Technology - Networking & IT tab's server racks ship, and the Java table of
the units that go in them.

    python dev-env-utils/scripts/gen_technology_rack.py
    python dev-env-utils/scripts/gen_technology_rack.py --check
    python dev-env-utils/scripts/gen_technology_rack.py --fragments   # tab lines to paste

What is here, and the class that places each:

- **42U server racks** (technology.rack.BlockServerRack), two blocks tall: an open two-post
  frame, and an enclosed black cabinet with a perforated door or a glass door. Cabinets side by
  side in a row join: their side panels are drawn only at the row's ends, picked from actual
  state (``panel_w`` / ``panel_e``, named in model space), so nothing about the row is stored.
- **The 12U wall-mount network cabinet** (technology.rack.BlockNetworkCabinet), one block, with
  a glass door, hung against the wall behind it.
- **Rack-mount units** (technology.rack.ItemRackUnit, one factory constructed per
  ``RackUnitType``): servers, storage, switches, patch panel, firewall, UPS, PDU, KVM drawer,
  cable manager, blanking plate and the Micafi Cloud Gateway. Clicked into a rack, a unit is
  stored on the rack's tile entity and baked into the chunk mesh by ``RackBakedModel``: no tile
  entity renderer, nothing ticks.

**The U scale.** A 42U rack is about two metres tall and a block is a metre, so the rack is two
blocks: 2 px of plinth, 42U on y 2..30 and 2 px of roof, which makes a U exactly 2/3 px (three U
to two pixels). The cell line at y 16 then falls exactly between U21 and U22. A rack is about
600 mm wide and drawn a block wide, so a unit's 19-inch face is 12 px across (x 2..14) and
reads wider than it is tall by more than a real one; at 2/3 px a U the alternative, a true
aspect, would have left a 42U rack three blocks tall. The wall cabinet uses the same 2/3 px a
U, so a unit looks the same wherever it is mounted; its 12U is y 4..12 and it is only 400 mm
deep, so it takes the units no deeper than 6 px (``RackFrame.WALL_12U``): the switches, patch
panel, firewall, gateway, PDU, cable manager and blanking plate.

**The unit sheet.** Every unit's face, and the solid colour of its sides, is one animated 128
px texture (``rack/rack_units``, 8 frames): a band 6 texels a U per unit, top to bottom in
catalogue order, and colour swatches along the bottom 8 rows. One 128 px sprite on the atlas
for every unit there is; the LEDs blink because the sheet does. The band rows are written into
``RackUnitType.java``, which the baked model reads, so the two cannot drift. **Units are only
ever appended** to ``UNITS``: the enum's ordinals are saved in every rack, and a new unit's
band goes below the last one.

Writes models under models/block/technology/rack, textures under
textures/blocks/technology/rack, the item models, the four languages, RackUnitType.java and the
units' recipes in Core's recipes folder (each conditional on csm_technology).
"""
import argparse
import io
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_technology_common as tc  # noqa: E402

C = tc.catalogue("gen_technology_rack.py", "rack")
TAB = tc.NETWORKING
E = tc.E

JAVA_DIR = os.path.join(tc.REPO, "modules", "technology", "src", "main", "java", "com",
                        "micatechnologies", "minecraft", "csm", "technology", "rack")
RECIPES_DIR = os.path.join(tc.REPO, "src", "main", "resources", "assets", "csm", "recipes")

# ------------------------------------------------------------------------------------------
# The unit sheet
# ------------------------------------------------------------------------------------------
SHEET = 128             # the sheet is SHEET x SHEET texels a frame
TEX_PER_U = 6           # texels a U, down the sheet
FRAMES = 8
FRAMETIME = 3
SWATCH_ROW = 120        # the colour swatches: rows 120..127, 32 texels each across
SWATCHES = [(34, 35, 38), (180, 183, 188), (232, 233, 231), (66, 68, 72)]
SW_BLACK, SW_SILVER, SW_WHITE, SW_GRAPHITE = range(4)

FACE_BLACK = (38, 39, 43)
FACE_SILVER = (176, 179, 184)
FACE_GRAPHITE = (62, 64, 68)
TRAY = (78, 80, 85)
GAP = (14, 14, 16)
GREEN = (60, 230, 90)
AMBER = (255, 170, 40)
BLUE = (70, 150, 255)
RED = (240, 60, 50)
WHITE = (240, 244, 250)
OFF = (40, 44, 42)


def R(img, x0, y0, x1, y1, c):
    tc.rect(img, x0, y0, x1, y1, c)


def px(img, x, y, c):
    R(img, x, y, x + 1, y + 1, c)


def base(w, h, c, seed):
    """A unit's face: its colour with a little grain, a lit top edge and a shaded foot."""
    img = tc.fill(c, max(w, h), 2, seed).crop((0, 0, w, h))
    R(img, 0, 0, w, 1, tc.shade(c, 1.22))
    R(img, 0, h - 1, w, h, tc.shade(c, 0.62))
    return img


def ears(img, c):
    """The mounting ears at both ends, a shade lighter, each with its screw."""
    h = img.height
    for x0 in (0, img.width - 5):
        R(img, x0, 0, x0 + 5, h, tc.shade(c, 1.1))
        R(img, x0 + (4 if x0 == 0 else 0), 0, x0 + (5 if x0 == 0 else 1), h, tc.shade(c, 0.7))
        cy = h // 2 - 1
        R(img, x0 + 1, cy, x0 + 3, cy + 2, tc.shade(c, 0.45))


def activity(seed, n):
    """n LEDs' activity over the loop: each is a list of FRAMES booleans, from one seed, so the
    frames are a fixed loop."""
    rng = random.Random(seed)
    return [[rng.random() < 0.5 for _ in range(FRAMES)] for _ in range(n)]


def vents(img, x0, x1, y0, y1, c):
    for x in range(x0, x1):
        if x % 2 == 0:
            R(img, x, y0, x + 1, y1, tc.shade(c, 0.55))


# ---- one drawing per unit: draw(f) returns the band for frame f ---------------------------
def _server_1u(f):
    img = base(SHEET, 6, FACE_BLACK, 601)
    ears(img, FACE_BLACK)
    act = activity(611, 8)
    px(img, 7, 1, BLUE)
    px(img, 7, 3, GREEN)
    R(img, 9, 2, 12, 5, (92, 94, 98))
    px(img, 10, 3, GREEN)
    for i in range(8):
        x = 18 + i * 10
        R(img, x, 1, x + 9, 5, TRAY)
        R(img, x, 4, x + 9, 5, tc.shade(TRAY, 0.7))
        R(img, x + 1, 2, x + 6, 3, tc.shade(TRAY, 0.6))
        px(img, x + 7, 1, GREEN if not act[i][f] else AMBER if i % 3 == 0 else OFF)
    vents(img, 100, 122, 1, 5, FACE_BLACK)
    return img


def _server_2u(f):
    img = base(SHEET, 12, FACE_BLACK, 602)
    ears(img, FACE_BLACK)
    act = activity(612, 24)
    tc.disc(img, 9.5, 4.5, 2.2, (96, 98, 104))
    px(img, 9, 4, GREEN)
    px(img, 7, 8, BLUE)
    px(img, 9, 8, GREEN)
    px(img, 11, 8, GREEN if f % 4 else OFF)
    for row in range(2):
        for i in range(12):
            x = 16 + i * 7
            y = 1 + row * 5
            R(img, x, y, x + 6, y + 5, TRAY)
            R(img, x, y + 4, x + 6, y + 5, tc.shade(TRAY, 0.68))
            R(img, x + 1, y + 1, x + 4, y + 2, tc.shade(TRAY, 0.6))
            px(img, x + 5, y + 1, GREEN if act[row * 12 + i][f] else OFF)
            px(img, x + 5, y + 2, BLUE if row == 0 and i == 0 else tc.shade(TRAY, 0.9))
    R(img, 101, 2, 121, 6, (18, 30, 46))
    tc.draw_text(img, "MCPX", 103, 1, (120, 190, 255))
    vents(img, 101, 122, 7, 11, FACE_BLACK)
    return img


def _storage_4u(f):
    img = base(SHEET, 24, FACE_BLACK, 603)
    ears(img, FACE_BLACK)
    act = activity(613, 24)
    for i in range(4):
        px(img, 7, 3 + i * 5, GREEN if i < 3 else BLUE)
    for row in range(4):
        for col in range(6):
            x = 10 + col * 18
            y = 1 + row * 6
            R(img, x, y, x + 17, y + 5, TRAY)
            R(img, x, y + 4, x + 17, y + 5, tc.shade(TRAY, 0.66))
            R(img, x + 2, y + 1, x + 12, y + 2, tc.shade(TRAY, 0.58))
            R(img, x + 2, y + 3, x + 12, y + 4, tc.shade(TRAY, 0.84))
            on = act[row * 6 + col][f]
            px(img, x + 15, y + 1, GREEN)
            px(img, x + 15, y + 3, AMBER if on and (row + col) % 5 == 0 else
               GREEN if on else OFF)
    return img


def _switch_face(f, ports_per_row, rows, seed, sfp):
    img = base(SHEET, 6, FACE_SILVER, seed)
    ears(img, FACE_SILVER)
    R(img, 6, 1, 8, 5, tc.MICAFI_TEAL)
    px(img, 10, 2, WHITE)
    act = activity(seed + 10, ports_per_row * rows)
    pitch = 4
    x_start = 14
    for r in range(rows):
        for i in range(ports_per_row):
            x = x_start + i * pitch + (2 if i >= ports_per_row // 2 else 0)
            if rows == 1:
                R(img, x, 2, x + 3, 5, tc.PORT_BLACK)
                px(img, x + 1, 2, tc.PIN_GOLD)
                linked = (i * 7 + seed) % 5 != 0
                px(img, x, 1, GREEN if linked else OFF)
                px(img, x + 2, 1, AMBER if linked and act[i][f] else OFF)
            else:
                y = 1 + r * 2
                R(img, x, y, x + 3, y + 2, tc.PORT_BLACK)
                linked = (i * 7 + r * 3 + seed) % 6 != 0
                k = r * ports_per_row + i
                px(img, x + (0 if r == 0 else 2), y + (0 if r == 0 else 1),
                   (GREEN if not act[k][f] else AMBER) if linked else OFF)
    x0 = x_start + ports_per_row * pitch + 4
    for j in range(sfp):
        x = x0 + j * 5
        R(img, x, 1, x + 4, 5, tc.shade(FACE_SILVER, 0.55))
        R(img, x + 1, 2, x + 3, 4, (24, 24, 26))
        px(img, x + 1, 1, GREEN if act[j][(f + 3) % FRAMES] else BLUE)
    return img


def _switch_24(f):
    return _switch_face(f, 24, 1, 604, 2)


def _switch_48(f):
    return _switch_face(f, 24, 2, 605, 2)


def _patch_panel(f):
    img = base(SHEET, 6, FACE_BLACK, 606)
    ears(img, FACE_BLACK)
    rng = random.Random(616)
    boots = [(60, 120, 230), (240, 210, 60), (70, 190, 90), (220, 70, 60), (230, 230, 230)]
    for i in range(24):
        x = 14 + i * 4 + (2 if i >= 12 else 0)
        px(img, x + 1, 1, (200, 202, 206))
        R(img, x, 2, x + 3, 5, tc.PORT_BLACK)
        if rng.random() < 0.7:
            R(img, x, 2, x + 3, 4, boots[rng.randrange(len(boots))])
    R(img, 116, 1, 122, 5, (200, 200, 196))
    return img


def _firewall(f):
    img = base(SHEET, 6, (30, 30, 34), 607)
    ears(img, (30, 30, 34))
    R(img, 6, 0, 30, 6, (200, 72, 40))
    R(img, 6, 5, 30, 6, (130, 44, 24))
    R(img, 9, 1, 27, 5, (10, 14, 12))
    rng = random.Random(617 + f // 4)
    for x in range(10, 26, 2):
        px(img, x, 2, GREEN if rng.random() < 0.7 else OFF)
        px(img, x, 3, GREEN if rng.random() < 0.4 else OFF)
    act = activity(627, 8)
    for i in range(8):
        x = 36 + i * 4
        R(img, x, 2, x + 3, 5, tc.PORT_BLACK)
        px(img, x, 1, GREEN)
        px(img, x + 2, 1, AMBER if act[i][f] else OFF)
    for j in range(2):
        x = 72 + j * 5
        R(img, x, 1, x + 4, 5, (70, 72, 76))
        R(img, x + 1, 2, x + 3, 4, (16, 16, 18))
    px(img, 112, 2, GREEN)
    px(img, 115, 2, GREEN if f % 2 else OFF)
    px(img, 118, 2, BLUE)
    return img


def _ups_2u(f):
    img = base(SHEET, 12, FACE_BLACK, 608)
    ears(img, FACE_BLACK)
    R(img, 9, 2, 41, 10, (20, 26, 30))
    R(img, 10, 3, 40, 9, (70, 160, 200))
    load = [5, 5, 6, 6, 6, 5, 5, 6][f]
    for i in range(load):
        R(img, 12 + i * 3, 6, 14 + i * 3, 8, (20, 50, 70))
    tc.draw_text(img, "100", 12, 3, (20, 50, 70)) if False else None
    R(img, 30, 4, 38, 5, (20, 50, 70))
    R(img, 30, 6, 36, 7, (20, 50, 70))
    for i in range(4):
        R(img, 45 + i * 4, 4, 48 + i * 4, 7, (96, 98, 104))
    px(img, 46, 9, GREEN)
    px(img, 50, 9, GREEN if f % 4 < 2 else OFF)
    for y in range(2, 11, 2):
        R(img, 64, y, 122, y + 1, tc.shade(FACE_BLACK, 0.5))
    return img


def _pdu(f):
    img = base(SHEET, 6, FACE_GRAPHITE, 609)
    ears(img, FACE_GRAPHITE)
    R(img, 6, 0, 19, 6, (14, 8, 8))
    tc.draw_text(img, ["34", "35", "34", "36", "35", "34", "35", "36"][f], 8, 0, RED)
    for i in range(8):
        x = 24 + i * 11
        R(img, x, 1, x + 8, 5, (24, 24, 26))
        R(img, x + 1, 2, x + 7, 4, (10, 10, 12))
        px(img, x + 2, 3, (60, 60, 62))
        px(img, x + 5, 3, (60, 60, 62))
    R(img, 113, 1, 121, 5, (30, 30, 32))
    R(img, 115, 2, 119, 4, (210, 210, 210))
    return img


def _kvm(f):
    img = base(SHEET, 6, FACE_BLACK, 610)
    ears(img, FACE_BLACK)
    R(img, 24, 2, 104, 4, (150, 152, 158))
    R(img, 24, 3, 104, 4, (100, 102, 108))
    R(img, 22, 1, 24, 5, (120, 122, 128))
    R(img, 104, 1, 106, 5, (120, 122, 128))
    px(img, 112, 2, GREEN)
    px(img, 115, 2, AMBER if f < 2 else OFF)
    R(img, 8, 2, 14, 4, (90, 92, 96))
    return img


def _cable_manager(f):
    img = base(SHEET, 6, FACE_BLACK, 611)
    ears(img, FACE_BLACK)
    R(img, 6, 1, 122, 5, (20, 20, 22))
    for i in range(9):
        x = 9 + i * 13
        R(img, x, 0, x + 5, 6, (58, 60, 64))
        R(img, x, 0, x + 5, 1, (80, 82, 88))
        R(img, x + 1, 2, x + 4, 4, (28, 28, 30))
    return img


def _blanking(f):
    img = base(SHEET, 6, (30, 31, 34), 612)
    ears(img, (30, 31, 34))
    for x in (40, 84):
        R(img, x, 2, x + 4, 4, (20, 20, 22))
    return img


def _gateway(f):
    img = base(SHEET, 6, (214, 216, 220), 613)
    ears(img, (214, 216, 220))
    R(img, 7, 1, 21, 5, (14, 22, 40))
    rng = random.Random(633)
    hist = [rng.randint(1, 3) for _ in range(12 + FRAMES)]
    for i in range(12):
        hgt = hist[i + f]
        R(img, 8 + i, 5 - hgt, 9 + i, 4, (90, 200, 255) if i % 2 else (140, 230, 255))
    R(img, 23, 1, 27, 5, tc.MICAFI_TEAL)
    px(img, 25, 2, WHITE)
    act = activity(643, 9)
    for i in range(8):
        x = 31 + i * 4
        R(img, x, 2, x + 3, 5, tc.PORT_BLACK)
        px(img, x + 1, 2, tc.PIN_GOLD)
        px(img, x, 1, WHITE if i < 6 else OFF)
        px(img, x + 2, 1, (90, 200, 255) if i < 6 and act[i][f] else OFF)
    x = 66
    R(img, x, 2, x + 3, 5, tc.PORT_BLACK)
    px(img, x, 1, BLUE)
    px(img, x + 2, 1, BLUE if act[8][f] else OFF)
    for j in range(2):
        x = 73 + j * 5
        R(img, x, 1, x + 4, 5, (150, 152, 158))
        R(img, x + 1, 2, x + 3, 4, (30, 30, 34))
    R(img, 90, 1, 118, 5, (190, 192, 196))
    R(img, 92, 2, 112, 3, (160, 162, 168))
    px(img, 115, 2, (90, 200, 255) if f % 3 else WHITE)
    return img


def _gpu_2u(f):
    img = base(SHEET, 12, (26, 27, 30), 614)
    ears(img, (26, 27, 30))
    R(img, 6, 1, 8, 11, (118, 196, 40))
    act = activity(624, 4)
    for i in range(4):
        y = 2 + i * 2
        R(img, 10, y, 16, y + 1, TRAY)
        px(img, 17, y, GREEN if act[i][f] else OFF)
    # four fan grilles; the blades turn a step a frame
    for k in range(4):
        cx, cy = 30.5 + k * 22, 6.0
        tc.disc(img, cx, cy, 5.4, (70, 72, 78))
        tc.disc(img, cx, cy, 4.6, (12, 12, 14))
        for b in range(3):
            a = (f / float(FRAMES)) * (2 * math.pi / 3) + b * 2 * math.pi / 3 + k
            for r in (1.5, 2.5, 3.5):
                x = int(cx + r * math.cos(a))
                y = int(cy + r * math.sin(a))
                px(img, x, y, (54, 56, 60))
        tc.disc(img, cx, cy, 1.2, (60, 62, 68))
    px(img, 118, 3, GREEN)
    px(img, 118, 6, (118, 196, 40) if f % 2 else OFF)
    px(img, 118, 9, BLUE)
    return img


def N(en, de, es, sv):
    return (en, de, es, sv)


# The units, in sheet order. APPEND ONLY: the ordinal of each is saved in every rack, and its
# band's rows follow from the ones above it.
#   (enum name, registry, names, height U, depth px, side swatch, drawing)
UNITS = [
    ("SERVER_1U", "rack_unit_server_1u",
     N("1U Rack Server", "1-HE-Rack-Server", "Servidor de Rack 1U", "1U rackserver"),
     1, 11.0, SW_BLACK, _server_1u),
    ("SERVER_2U", "rack_unit_server_2u",
     N("2U Rack Server", "2-HE-Rack-Server", "Servidor de Rack 2U", "2U rackserver"),
     2, 11.0, SW_BLACK, _server_2u),
    ("STORAGE_4U", "rack_unit_storage_4u",
     N("4U Storage Array (SAN)", "4-HE-Speicher-Array (SAN)",
       "Cabina de Almacenamiento 4U (SAN)", "4U lagringsenhet (SAN)"),
     4, 12.0, SW_BLACK, _storage_4u),
    ("SWITCH_24_PORT", "rack_unit_switch_24_port",
     N("Micafi 24-Port Rack Switch", "Micafi 24-Port-Rack-Switch",
       "Switch de Rack Micafi de 24 Puertos", "Micafi 24-ports rackswitch"),
     1, 4.0, SW_SILVER, _switch_24),
    ("SWITCH_48_PORT", "rack_unit_switch_48_port",
     N("Micafi 48-Port Rack Switch", "Micafi 48-Port-Rack-Switch",
       "Switch de Rack Micafi de 48 Puertos", "Micafi 48-ports rackswitch"),
     1, 5.0, SW_SILVER, _switch_48),
    ("PATCH_PANEL", "rack_unit_patch_panel",
     N("24-Port Patch Panel", "24-Port-Patchpanel", "Panel de Parcheo de 24 Puertos",
       "24-ports patchpanel"),
     1, 2.0, SW_BLACK, _patch_panel),
    ("FIREWALL", "rack_unit_firewall",
     N("1U Firewall Appliance", "1-HE-Firewall", "Cortafuegos 1U", "1U brandvägg"),
     1, 5.0, SW_BLACK, _firewall),
    ("UPS_2U", "rack_unit_ups_2u",
     N("2U Rack UPS", "2-HE-Rack-USV", "SAI de Rack 2U", "2U rack-UPS"),
     2, 10.0, SW_BLACK, _ups_2u),
    ("PDU", "rack_unit_pdu",
     N("1U Rack PDU", "1-HE-Rack-Steckdosenleiste", "Regleta de Rack 1U (PDU)",
       "1U rackgrenuttag (PDU)"),
     1, 3.0, SW_GRAPHITE, _pdu),
    ("KVM_DRAWER", "rack_unit_kvm_drawer",
     N("1U KVM Console Drawer", "1-HE-KVM-Konsolenschublade", "Consola KVM Extraíble 1U",
       "1U KVM-konsollåda"),
     1, 10.0, SW_BLACK, _kvm),
    ("CABLE_MANAGER", "rack_unit_cable_manager",
     N("1U Cable Manager", "1-HE-Kabelführung", "Organizador de Cables 1U",
       "1U kabelhanterare"),
     1, 2.0, SW_BLACK, _cable_manager),
    ("BLANKING_PLATE", "rack_unit_blanking_plate",
     N("1U Blanking Plate", "1-HE-Blindplatte", "Panel Ciego 1U", "1U blindplåt"),
     1, 0.5, SW_BLACK, _blanking),
    ("MICAFI_GATEWAY", "rack_unit_micafi_gateway",
     N("Micafi Cloud Gateway (1U)", "Micafi Cloud-Gateway (1 HE)",
       "Pasarela en la Nube Micafi (1U)", "Micafi molngateway (1U)"),
     1, 4.0, SW_WHITE, _gateway),
    ("GPU_SERVER_2U", "rack_unit_gpu_server_2u",
     N("2U GPU Server", "2-HE-GPU-Server", "Servidor GPU 2U", "2U GPU-server"),
     2, 12.0, SW_BLACK, _gpu_2u),
]


def bands():
    """Each unit's first sheet row, in catalogue order."""
    out = []
    row = 0
    for u in UNITS:
        out.append(row)
        row += u[3] * TEX_PER_U
    assert row <= SWATCH_ROW, "the unit sheet is full: start a second sheet"
    return out


BANDS = bands()


def sheet_frame(f):
    img = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 255))
    for u, row in zip(UNITS, BANDS):
        band = u[6](f)
        assert band.size == (SHEET, u[3] * TEX_PER_U), u[0]
        img.paste(band, (0, row))
    for i, c in enumerate(SWATCHES):
        img.paste(tc.fill(c, 32, 2, 650 + i).crop((0, 0, 32, 8)), (i * 32, SWATCH_ROW))
    return img


tc.anim(C, "rack_units", FRAMES, sheet_frame, FRAMETIME)


# ------------------------------------------------------------------------------------------
# Frame textures
# ------------------------------------------------------------------------------------------
BODY = (30, 31, 34)


@C.texture("door_mesh")
def _door_mesh():
    """The perforated door: a 64 px sheet of square holes three texels across on a four texel
    pitch, every other row staggered, so more than half of it is open, as a real cabinet door's
    is. Drawn in the translucent pass (CsmGlassLayer.OTHER_GLASS), so at a distance the holes
    average into a dark screen the units still show through."""
    img = tc.fill(BODY, 64, 2, 701)
    p = img.load()
    for y in range(64):
        for x in range(64):
            sx = (x + (2 if (y // 4) % 2 else 0)) % 4
            if y % 4 != 3 and sx != 3:
                p[x, y] = (0, 0, 0, 0)
    return img


@C.texture("door_glass")
def _door_glass():
    """Smoked glass, a little lighter along one diagonal streak."""
    img = Image.new("RGBA", (16, 16))
    p = img.load()
    for y in range(16):
        for x in range(16):
            streak = abs((x + y) - 12) < 1
            p[x, y] = (54, 60, 66, 84) if streak else (18, 22, 26, 64)
    return img


@C.texture("rack_post")
def _rack_post():
    """A rail or upright: black steel with its square mounting holes, three to two pixels like
    the U."""
    img = tc.fill(BODY, 16, 2, 702)
    for y in range(0, 16, 2):
        R(img, 6, y, 10, y + 1, (8, 8, 9))
    return img


# ------------------------------------------------------------------------------------------
# Models
# ------------------------------------------------------------------------------------------
ALL = tc.ALL


def el(frm, to, t, faces=ALL, **kw):
    return {"frm": frm, "to": to, "t": t, "faces": faces, "kw": kw}


def build(specs, cell):
    """The elements of ``specs`` (rack coordinates, y 0..32) inside one cell, moved down to its
    own 0..16: a box crossing the cell line loses the face it would have there."""
    lo, hi = cell * 16, cell * 16 + 16
    out = []
    for s in specs:
        (x0, y0, z0), (x1, y1, z1) = s["frm"], s["to"]
        if y1 <= lo or y0 >= hi:
            continue
        faces = list(s["faces"])
        if y0 < lo and "down" in faces:
            faces.remove("down")
        if y1 > hi and "up" in faces:
            faces.remove("up")
        if not faces:
            continue
        out.append(E((x0, max(y0, lo) - lo, z0), (x1, min(y1, hi) - lo, z1), s["t"],
                     tuple(faces), **s["kw"]))
    return out


def whole(specs):
    return [E(s["frm"], s["to"], s["t"], tuple(s["faces"]), **s["kw"]) for s in specs]


# The 42U cabinet, front (the door) at z = 0, as every model faces north.
CAB_FIRST_Y, CAB_TOP_Y = 2.0, 30.0
CAB_FRONT_Z = 1.6       # the units' faces, flush with the rails
SIDE_T = 0.5            # a side panel's thickness


def cabinet_frame():
    s = [el((0, 0, 0), (16, 2, 16), "body"),
         el((0, 30, 0), (16, 32, 16), "body"),
         # front and back corner posts; their outer faces are left to the side panel
         el((0, 2, 0.6), (1, 30, 1.6), "body", ("north", "south", "east")),
         el((15, 2, 0.6), (16, 30, 1.6), "body", ("north", "south", "west")),
         el((0, 2, 14.4), (1, 30, 15.4), "body", ("north", "east")),
         el((15, 2, 14.4), (16, 30, 15.4), "body", ("north", "west")),
         # the rear door
         el((0, 2, 15.4), (16, 30, 16), "body", ("north", "south")),
         # the mounting rails either side of the 19-inch opening
         el((1.4, 2, 1.6), (2, 30, 2.3), "post", ("north", "east", "west")),
         el((14, 2, 1.6), (14.6, 30, 2.3), "post", ("north", "east", "west"))]
    return s


def cabinet_door(panel_tex):
    return [el((0, 2, 0), (1.25, 30, 0.6), "body", ("north", "south", "east", "west")),
            el((14.75, 2, 0), (16, 30, 0.6), "body", ("north", "south", "east", "west")),
            el((1.25, 28.5, 0), (14.75, 30, 0.6), "body", ("north", "south", "down")),
            el((1.25, 2, 0), (14.75, 3.5, 0.6), "body", ("north", "south", "up")),
            el((1.25, 3.5, 0.3), (14.75, 28.5, 0.3), panel_tex, ("north",)),
            # the handle, on the opening side (the viewer's right, model west)
            el((1.6, 13.0, -0.6), (2.2, 19.0, 0), "handle", ("north", "east", "west", "up",
                                                             "down"))]


def side_panel(side):
    x0, x1 = (0, SIDE_T) if side == "w" else (16 - SIDE_T, 16)
    outer = "west" if side == "w" else "east"
    inner = "east" if side == "w" else "west"
    return [el((x0, 2, 0.6), (x1, 30, 15.4), "body", (outer, inner))]


def open_frame():
    return [el((0, 0, 3), (2.5, 1, 13), "body"),
            el((13.5, 0, 3), (16, 1, 13), "body"),
            el((0.6, 1, 6), (2, 31, 8.5), "post", ("north", "south", "east", "west")),
            el((14, 1, 6), (15.4, 31, 8.5), "post", ("north", "south", "east", "west")),
            el((0.6, 31, 6), (15.4, 32, 8.5), "body"),
            el((2, 1, 6.5), (14, 2, 8), "body", ("north", "south", "up"))]


def wall_cabinet():
    return [el((0, 12, 8), (16, 13, 16), "body"),
            el((0, 3, 8), (16, 4, 16), "body"),
            el((0, 4, 8.6), (0.6, 12, 16), "body", ("west", "east", "north")),
            el((15.4, 4, 8.6), (16, 12, 16), "body", ("west", "east", "north")),
            el((0.6, 4, 15.4), (15.4, 12, 16), "body", ("north",)),
            el((0, 4, 8), (1, 12, 8.6), "body", ("north", "south", "east", "west")),
            el((15, 4, 8), (16, 12, 8.6), "body", ("north", "south", "east", "west")),
            el((1, 11.2, 8), (15, 12, 8.6), "body", ("north", "south", "down")),
            el((1, 4, 8), (15, 4.8, 8.6), "body", ("north", "south", "up")),
            el((1, 4.8, 8.3), (15, 11.2, 8.3), "glass", ("north",)),
            el((1.4, 4, 9.2), (2, 12, 9.8), "post", ("north", "east", "west")),
            el((14, 4, 9.2), (14.6, 12, 9.8), "post", ("north", "east", "west")),
            el((1.3, 7, 7.5), (1.8, 9, 8), "handle", ("north", "east", "west", "up", "down"))]


TEX = {"body": tc.T("black_anodised"), "post": C.T("rack_post"),
       "handle": tc.T("grey_abs"), "units": C.T("rack_units")}


def textures(extra):
    t = dict(TEX)
    t.update(extra)
    t["particle"] = TEX["body"]
    return t


ITEM_TALL = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0], "scale": [0.42] * 3},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2] * 3},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, -4, 0], "scale": [0.42] * 3},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1, 0],
                              "scale": [0.25] * 3},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0],
                              "scale": [0.3] * 3},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -1, 0],
                             "scale": [0.3] * 3},
}

FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def part(model, when, y):
    apply = {"model": model}
    if y:
        apply["y"] = y
    return {"when": when, "apply": apply}


def two_cell_state(base, sides=None):
    """A multipart blockstate for a two-cell rack: the cell's model by ``upper``, turned by
    ``facing``, and with ``sides`` (the side panel models by cell) a panel on each side where
    the row ends there (``panel_w`` / ``panel_e`` true)."""
    parts = []
    for cell, upper in ((0, "false"), (1, "true")):
        for facing, y in FACINGS:
            parts.append(part(base[cell], {"upper": upper, "facing": facing}, y))
    if sides:
        for cell, upper in ((0, "false"), (1, "true")):
            for key, side in (("panel_w", "w"), ("panel_e", "e")):
                for facing, y in FACINGS:
                    parts.append(part(sides[side][cell],
                                      {"upper": upper, "facing": facing, key: "true"}, y))
    return {"multipart": parts}


def rack_java(registry, frame, preset=False):
    if preset:
        return 'new BlockServerRack("%s", RackFrame.%s, true)' % (registry, frame)
    return 'new BlockServerRack("%s", RackFrame.%s)' % (registry, frame)


# ------------------------------------------------------------------------------------------
# Where a unit goes: the numbers RackFrame.java is written from, and the boxes a rack's units
# are drawn as. RackBakedModel builds the same boxes from the same numbers for a fillable rack;
# the pre-filled racks' models are written from them here, so the two look the same.
# ------------------------------------------------------------------------------------------
PITCH = 2.0 / 3.0       # a U, in model pixels
UNIT_X0, UNIT_X1 = 2.0, 14.0
OPEN_FRONT_MAX, OPEN_BACK = 5.75, 15.5

#   (enum name, cells, U, first unit's floor, open, the units' face (z), deepest unit, description)
FRAMES_DEF = [
    ("OPEN_42U", 2, 42, 2.0, True, OPEN_FRONT_MAX, 15.5,
     "The open two-post frame: a unit's face is at most 5.75 px in and its back at most 15.5,\n"
     "   * so a deep unit hangs centred on the posts as it does on a real relay rack."),
    ("CABINET_42U", 2, 42, 2.0, False, CAB_FRONT_Z, 13.8,
     "The enclosed cabinet: faces flush with the rails behind the door, the rear door 13.8 px\n"
     "   * behind them."),
    ("WALL_12U", 1, 12, 4.0, False, 9.2, 6.2,
     "The wall cabinet: 400 mm deep, so only the units 6.2 px deep or less fit."),
]
FRAME = {f[0]: f for f in FRAMES_DEF}


def unit_index(name):
    return [u[0] for u in UNITS].index(name)


def front_z(frame, depth):
    _, _, _, _, is_open, face, _, _ = FRAME[frame]
    return min(face, OPEN_BACK - depth) if is_open else face


def unit_boxes(frame, units, cell=None):
    """The boxes the units are drawn as, in one cell (moved down to its 0..16) or whole.

    ``units`` is a list of (unit index, top U). A face is left out where nothing can see it: the
    back inside a cabinet, a top or a bottom against the next unit when that one is at least as
    deep, and the face a box would have at the cell line. Returns (from, to, faces, uvs)."""
    _, cells, total, first, is_open, _, _, _ = FRAME[frame]
    k = 16.0 / SHEET
    spans = []
    for idx, top in units:
        u = UNITS[idx]
        h, d = u[3], u[4]
        z0 = front_z(frame, d)
        spans.append((idx, top, h, first + (top - h) * PITCH, first + top * PITCH, z0, z0 + d))
    lo, hi = (0.0, 16.0 * cells) if cell is None else (cell * 16.0, cell * 16.0 + 16.0)
    out = []
    for idx, top, h, y0, y1, z0, z1 in spans:
        above = any(t2 - h2 == top and a <= z0 and b >= z1
                    for _, t2, h2, _, _, a, b in spans)
        below = any(t2 == top - h and a <= z0 and b >= z1
                    for _, t2, h2, _, _, a, b in spans)
        if y1 <= lo or y0 >= hi:
            continue
        ya, yb = max(y0, lo), min(y1, hi)
        faces = ["north", "east", "west"]
        if is_open:
            faces.append("south")
        if not above and y1 <= hi:
            faces.append("up")
        if not below and y0 >= lo:
            faces.append("down")
        u = UNITS[idx]
        v0 = BANDS[idx] * k
        v1 = (BANDS[idx] + u[3] * TEX_PER_U) * k
        vt = v0 + (y1 - yb) / (y1 - y0) * (v1 - v0)
        vb = v0 + (y1 - ya) / (y1 - y0) * (v1 - v0)
        su = u[5] * 32 * k
        side = [su + 0.5, SWATCH_ROW * k + 0.25, su + 3.5, SWATCH_ROW * k + 0.75]
        uv = {f: side for f in faces}
        uv["north"] = [0, vt, 16, vb]
        shift = 0.0 if cell is None else lo
        out.append(((UNIT_X0, ya - shift, z0), (UNIT_X1, yb - shift, z1), tuple(faces), uv))
    return out


def unit_elements(frame, units, cell=None):
    return [E(frm, to, "units", faces, uv=uv) for frm, to, faces, uv in
            unit_boxes(frame, units, cell)]


# ---- the side panels, shared by every cabinet ------------------------------------------------
SIDE_MODELS = {}
for _side in ("w", "e"):
    SIDE_MODELS[_side] = (C.M("server_rack_side_%s_lower" % _side),
                          C.M("server_rack_side_%s_upper" % _side))

_side_files = {}
for _side in ("w", "e"):
    for _cell, _suffix in ((0, "lower"), (1, "upper")):
        _side_files["server_rack_side_%s_%s" % (_side, _suffix)] = tc.model(
            textures({}), build(side_panel(_side), _cell))


def two_cell_state_units(base, sides, units):
    """two_cell_state with each cell's units added as one more part."""
    state = two_cell_state(base, sides)
    for cell, upper in ((0, "false"), (1, "true")):
        for facing, y in FACINGS:
            state["multipart"].append(part(units[cell], {"upper": upper, "facing": facing}, y))
    return state


CABINET_DOORS = {"mesh": ("door", C.T("door_mesh")), "glass": ("glass", C.T("door_glass"))}


def add_cabinet(registry, names, door, side_files=None):
    door_key, door_tex = CABINET_DOORS[door]
    specs = cabinet_frame() + cabinet_door(door_key)
    tex = textures({door_key: door_tex})
    models = {}
    for cell, suffix in ((0, "lower"), (1, "upper")):
        models["%s_%s" % (registry, suffix)] = tc.model(tex, build(specs, cell))
    models.update(side_files or {})
    item_specs = specs + side_panel("w") + side_panel("e")
    item = tc.with_display(tc.model(tex, whole(item_specs)), ITEM_TALL)
    C.add(registry, rack_java(registry, "CABINET_42U"), names, models,
          two_cell_state((C.M(registry + "_lower"), C.M(registry + "_upper")), SIDE_MODELS),
          item=item, tab=TAB)


def add_open(registry, names):
    specs = open_frame()
    tex = textures({})
    models = {"%s_lower" % registry: tc.model(tex, build(specs, 0)),
              "%s_upper" % registry: tc.model(tex, build(specs, 1))}
    item = tc.with_display(tc.model(tex, whole(specs)), ITEM_TALL)
    C.add(registry, rack_java(registry, "OPEN_42U"), names, models,
          two_cell_state((C.M(registry + "_lower"), C.M(registry + "_upper"))),
          item=item, tab=TAB)


add_open("server_rack_42u_open",
         N("42U Server Rack (Open Frame)", "42-HE-Serverrack (offener Rahmen)",
           "Rack de Servidores 42U (Bastidor Abierto)", "42U serverrack (öppen ram)"))
add_cabinet("server_rack_42u_mesh",
            N("42U Server Cabinet (Perforated Door)", "42-HE-Serverschrank (Lochblechtür)",
              "Armario de Servidores 42U (Puerta Perforada)", "42U serverskåp (perforerad dörr)"),
            "mesh", _side_files)
add_cabinet("server_rack_42u_glass",
            N("42U Server Cabinet (Glass Door)", "42-HE-Serverschrank (Glastür)",
              "Armario de Servidores 42U (Puerta de Vidrio)", "42U serverskåp (glasdörr)"),
            "glass")


# ------------------------------------------------------------------------------------------
# The pre-filled racks: plain baked models, no tile entity, for a data centre of many racks.
# Each preset is its units from the top down; "-" is an empty U. They stand on the same frame
# models and side panels as the fillable racks and join rows with them.
# ------------------------------------------------------------------------------------------
def fill_from_top(seq, total=42):
    """(unit index, top U) for a list of unit names from the top down, "-" an empty U."""
    out = []
    top = total
    for name in seq:
        if name == "-":
            top -= 1
            continue
        idx = unit_index(name)
        out.append((idx, top))
        top -= UNITS[idx][3]
    assert top == 0, (top, seq)
    return out


TOP_OF_RACK = ["SWITCH_48_PORT", "PATCH_PANEL", "CABLE_MANAGER"]
PRESETS = [
    ("compute", "CABINET_42U", "mesh",
     N("42U Pre-Filled Server Cabinet (Compute Farm)", "Bestückter 42-HE-Serverschrank (Rechenfarm)",
       "Armario de Servidores 42U Equipado (Granja de Cómputo)",
       "Fyllt 42U serverskåp (beräkningsfarm)"),
     TOP_OF_RACK + ["SERVER_1U"] * 36 + ["-", "PDU", "PDU"]),
    ("mixed", "CABINET_42U", "mesh",
     N("42U Pre-Filled Server Cabinet (Mixed)", "Bestückter 42-HE-Serverschrank (Gemischt)",
       "Armario de Servidores 42U Equipado (Mixto)", "Fyllt 42U serverskåp (blandat)"),
     ["SWITCH_24_PORT", "PATCH_PANEL", "CABLE_MANAGER", "FIREWALL", "MICAFI_GATEWAY",
      "BLANKING_PLATE"] + ["SERVER_2U"] * 8
     + ["BLANKING_PLATE", "KVM_DRAWER", "BLANKING_PLATE", "SERVER_1U", "SERVER_1U", "SERVER_1U"]
     + ["-"] * 9 + ["PDU", "UPS_2U", "UPS_2U"]),
    ("storage", "CABINET_42U", "mesh",
     N("42U Pre-Filled Server Cabinet (Storage)", "Bestückter 42-HE-Serverschrank (Speicher)",
       "Armario de Servidores 42U Equipado (Almacenamiento)",
       "Fyllt 42U serverskåp (lagring)"),
     ["SWITCH_48_PORT", "SWITCH_48_PORT", "CABLE_MANAGER", "SERVER_1U", "SERVER_1U"]
     + ["STORAGE_4U"] * 8 + ["BLANKING_PLATE"] * 3 + ["UPS_2U"]),
    ("network", "CABINET_42U", "mesh",
     N("42U Pre-Filled Server Cabinet (Network Core)", "Bestückter 42-HE-Serverschrank (Netzwerkkern)",
       "Armario de Servidores 42U Equipado (Núcleo de Red)",
       "Fyllt 42U serverskåp (nätverkskärna)"),
     ["MICAFI_GATEWAY", "FIREWALL", "FIREWALL", "CABLE_MANAGER"]
     + ["PATCH_PANEL", "SWITCH_48_PORT", "CABLE_MANAGER"] * 9
     + ["-", "-"] + ["BLANKING_PLATE"] * 6 + ["PDU", "UPS_2U"]),
    ("gpu", "CABINET_42U", "mesh",
     N("42U Pre-Filled Server Cabinet (GPU Cluster)", "Bestückter 42-HE-Serverschrank (GPU-Cluster)",
       "Armario de Servidores 42U Equipado (Clúster de GPU)",
       "Fyllt 42U serverskåp (GPU-kluster)"),
     TOP_OF_RACK + ["GPU_SERVER_2U"] * 16
     + ["BLANKING_PLATE", "BLANKING_PLATE", "-", "PDU", "PDU", "UPS_2U"]),
    ("sparse", "CABINET_42U", "mesh",
     N("42U Pre-Filled Server Cabinet (Half Empty)", "Bestückter 42-HE-Serverschrank (Halb leer)",
       "Armario de Servidores 42U Equipado (Medio Vacío)",
       "Fyllt 42U serverskåp (halvtomt)"),
     ["SWITCH_24_PORT", "PATCH_PANEL"] + ["BLANKING_PLATE"] * 26
     + ["SERVER_2U", "SERVER_2U", "SERVER_1U", "SERVER_1U", "BLANKING_PLATE",
        "BLANKING_PLATE", "SERVER_1U", "BLANKING_PLATE", "BLANKING_PLATE", "PDU", "UPS_2U"]),
    ("network_open", "OPEN_42U", None,
     N("42U Pre-Filled Server Rack (Network Core, Open Frame)",
       "Bestücktes 42-HE-Serverrack (Netzwerkkern, offener Rahmen)",
       "Rack de Servidores 42U Equipado (Núcleo de Red, Bastidor Abierto)",
       "Fyllt 42U serverrack (nätverkskärna, öppen ram)"),
     ["CABLE_MANAGER"] + ["PATCH_PANEL", "SWITCH_48_PORT", "CABLE_MANAGER"] * 9
     + ["-", "FIREWALL", "MICAFI_GATEWAY"] + ["-"] * 10 + ["PDU"]),
]


def add_preset(key, frame, door, names, seq):
    registry = "server_rack_42u_filled_" + key
    units = fill_from_top(seq)
    tex = {"units": TEX["units"], "particle": TEX["body"]}
    models = {}
    for cell, suffix in ((0, "lower"), (1, "upper")):
        models["%s_units_%s" % (registry, suffix)] = tc.model(tex, unit_elements(frame, units,
                                                                                 cell))
    unit_models = (C.M(registry + "_units_lower"), C.M(registry + "_units_upper"))
    if frame == "OPEN_42U":
        specs = open_frame()
        frame_tex = textures({})
        base_models = (C.M("server_rack_42u_open_lower"), C.M("server_rack_42u_open_upper"))
        state = two_cell_state_units(base_models, None, unit_models)
    else:
        door_key, door_tex = CABINET_DOORS[door]
        specs = cabinet_frame() + cabinet_door(door_key) + side_panel("w") + side_panel("e")
        frame_tex = textures({door_key: door_tex})
        base_models = (C.M("server_rack_42u_%s_lower" % door),
                       C.M("server_rack_42u_%s_upper" % door))
        state = two_cell_state_units(base_models, SIDE_MODELS, unit_models)
    item_tex = dict(frame_tex)
    item_tex["units"] = TEX["units"]
    item = tc.with_display(tc.model(item_tex, whole(specs) + unit_elements(frame, units)),
                           ITEM_TALL)
    C.add(registry, rack_java(registry, frame, preset=True), names, models, state, item=item,
          tab=TAB)


for _p in PRESETS:
    add_preset(*_p)

# ---- the wall cabinet ----------------------------------------------------------------------
_wall_tex = textures({"glass": C.T("door_glass")})
_wall_els = whole(wall_cabinet())
C.add("network_cabinet_12u_wall",
      'new BlockNetworkCabinet("network_cabinet_12u_wall", RackFrame.WALL_12U)',
      N("12U Wall-Mount Network Cabinet", "12-HE-Wand-Netzwerkschrank",
        "Armario de Red Mural 12U", "12U väggmonterat nätverksskåp"),
      {"network_cabinet_12u_wall": tc.with_display(
          tc.model(_wall_tex, _wall_els),
          tc.desk_display(0.62, 0.0))},
      tc.facing_state(C.M("network_cabinet_12u_wall")), tab=TAB)


# ------------------------------------------------------------------------------------------
# The units' items: a small box with the unit's band on its face
# ------------------------------------------------------------------------------------------
def unit_item(i):
    name, registry, names, height, depth, swatch, _ = UNITS[i]
    row = BANDS[i]
    k = 16.0 / SHEET
    v0, v1 = row * k, (row + height * TEX_PER_U) * k
    su = swatch * 32 * k
    side_uv = [su + 0.5, SWATCH_ROW * k + 0.25, su + 3.5, SWATCH_ROW * k + 0.75]
    hh = 2.5 * height
    d = max(1.0, min(12.0, depth))
    y0, y1 = 8 - hh / 2, 8 + hh / 2
    z0 = 8 - d / 2
    box = E((0, y0, z0), (16, y1, z0 + d), "units", ALL,
            uv={"north": [0, v0, 16, v1], "south": side_uv, "east": side_uv,
                "west": side_uv, "up": side_uv, "down": side_uv})
    m = tc.model({"units": TEX["units"], "particle": TEX["units"]}, [box])
    return tc.with_display(m, tc.desk_display(0.9, 0.0))


for _i, _u in enumerate(UNITS):
    C.add_model_item(_u[1], "new ItemRackUnit(RackUnitType.%s)" % _u[0], _u[2], unit_item(_i),
                     tab=TAB)

# ------------------------------------------------------------------------------------------
# Messages and tooltips
# ------------------------------------------------------------------------------------------
C.add_lang("message.csm.rack.installed",
           N("Installed at U%s", "Eingebaut auf HE %s", "Instalado en la U%s",
             "Monterad på U%s"))
C.add_lang("message.csm.rack.removed",
           N("Removed from U%s", "Ausgebaut von HE %s", "Retirado de la U%s",
             "Borttagen från U%s"))
C.add_lang("message.csm.rack.full",
           N("No room left for a %sU unit", "Kein Platz mehr für ein %s-HE-Gerät",
             "No queda espacio para una unidad de %sU", "Inget utrymme kvar för en %sU-enhet"))
C.add_lang("message.csm.rack.too_deep",
           N("Too deep for this cabinet", "Zu tief für diesen Schrank",
             "Demasiado profundo para este armario", "För djup för det här skåpet"))
C.add_lang("message.csm.rack.empty_here",
           N("Nothing mounted at U%s", "Auf HE %s ist nichts eingebaut",
             "No hay nada montado en la U%s", "Inget monterat på U%s"))
C.add_lang("message.csm.rack.summary",
           N("%sU of %sU used, %s units (sneak-click a unit with an empty hand to remove it)",
             "%s von %s HE belegt, %s Geräte (mit leerer Hand schleichend klicken, um eines "
             "auszubauen)",
             "%sU de %sU en uso, %s unidades (clic agachado con la mano vacía para retirar una)",
             "%sU av %sU använda, %s enheter (smygklicka med tom hand för att ta bort en)"))
C.add_lang("tooltip.csm.rack_unit.size",
           N("%sU rack-mount unit", "%s-HE-Rackeinschub", "Unidad de rack de %sU",
             "%sU rackenhet"))
C.add_lang("tooltip.csm.rack_unit.use",
           N("Right-click a rack or cabinet to mount it", "Rechtsklick auf ein Rack oder einen "
             "Schrank zum Einbauen", "Clic derecho en un rack o armario para montarla",
             "Högerklicka på ett rack eller skåp för att montera den"))
C.add_lang("tooltip.csm.rack_unit.wall",
           N("Fits the 12U wall cabinet", "Passt in den 12-HE-Wandschrank",
             "Cabe en el armario mural 12U", "Passar i 12U-väggskåpet"))


# ------------------------------------------------------------------------------------------
# Outside the module's assets: the Java unit table and the recipes
# ------------------------------------------------------------------------------------------
def jfloat(v):
    s = "%g" % v
    return (s if "." in s else s + ".0") + "f"


def java_units():
    rows = []
    for i, u in enumerate(UNITS):
        name, registry, names, height, depth, swatch, _ = u
        sep = "," if i < len(UNITS) - 1 else ";"
        rows.append('  /** %s. */\n  %s("%s", %d, %s, %d, %d)%s'
                    % (names[0], name, registry, height, jfloat(depth), BANDS[i], swatch, sep))
    return """package com.micatechnologies.minecraft.csm.technology.rack;

/**
 * The rack-mount units: what each is called, how many U it takes, how deep it is and where its
 * face is on the unit sheet.
 *
 * <p><b>Written by {@code dev-env-utils/scripts/gen_technology_rack.py}; edit it there.</b> The
 * ordinals are saved in every rack ({@link RackContents}), so units are only ever appended, and
 * each unit's band on the sheet follows from the ones above it.</p>
 *
 * @since 2026.10
 */
public enum RackUnitType {

%s

  /** The unit sheet: every unit's face, and its sides' colour, on one animated sprite. */
  public static final String SHEET = "csm:blocks/technology/rack/rack_units";

  /** The sheet's size in texels, a frame. */
  public static final int SHEET_TEXELS = %d;

  /** Texels a U down the sheet. */
  public static final int TEXELS_PER_U = %d;

  /** The first row of the colour swatches, each 32 texels across and 8 down. */
  public static final int SWATCH_ROW = %d;

  private final String registryName;
  private final int heightU;
  private final float depth;
  private final int bandRow;
  private final int swatch;

  RackUnitType(String registryName, int heightU, float depth, int bandRow, int swatch) {
    this.registryName = registryName;
    this.heightU = heightU;
    this.depth = depth;
    this.bandRow = bandRow;
    this.swatch = swatch;
  }

  /** The unit's item's registry name. */
  public String getRegistryName() {
    return registryName;
  }

  /** How many U the unit takes. */
  public int getHeightU() {
    return heightU;
  }

  /** How deep the unit is, in model pixels (a sixteenth of a block). */
  public float getDepth() {
    return depth;
  }

  /** The sheet row its face starts on; it runs {@code heightU * TEXELS_PER_U} rows. */
  public int getBandRow() {
    return bandRow;
  }

  /** Which colour swatch its sides take: 0 black, 1 silver, 2 white, 3 graphite. */
  public int getSwatch() {
    return swatch;
  }

  /**
   * The unit with this ordinal, or {@code null} for one this version does not know (a rack
   * saved by a later version).
   *
   * @param ordinal the saved ordinal
   * @return the unit, or {@code null}
   */
  public static RackUnitType fromOrdinal(int ordinal) {
    RackUnitType[] all = values();
    return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
  }
}
""" % ("\n".join(rows), SHEET, TEX_PER_U, SWATCH_ROW)


RECIPES = {
    "SERVER_1U": (["control_board", "sheet_metal", "wiring_harness"], 1),
    "SERVER_2U": (["control_board", "control_board", "sheet_metal", "wiring_harness"], 1),
    "STORAGE_4U": (["control_board", "control_board", "sheet_metal", "sheet_metal",
                    "wiring_harness"], 1),
    "SWITCH_24_PORT": (["control_board", "sheet_metal", "wiring_harness", "led_module"], 1),
    "SWITCH_48_PORT": (["control_board", "control_board", "sheet_metal", "wiring_harness",
                        "led_module"], 1),
    "PATCH_PANEL": (["sheet_metal", "wiring_harness"], 1),
    "FIREWALL": (["control_board", "sheet_metal", "led_module"], 1),
    "UPS_2U": (["control_board", "sheet_metal", "sheet_metal", "wiring_harness"], 1),
    "PDU": (["sheet_metal", "wiring_harness", "wiring_harness"], 1),
    "KVM_DRAWER": (["control_board", "sheet_metal", "led_module"], 1),
    "CABLE_MANAGER": (["sheet_metal", "fastener_kit"], 2),
    "BLANKING_PLATE": (["sheet_metal", "fastener_kit"], 4),
    "MICAFI_GATEWAY": (["control_board", "sheet_metal", "wiring_harness", "led_module"], 1),
    "GPU_SERVER_2U": (["control_board", "control_board", "control_board", "sheet_metal",
                       "wiring_harness"], 1),
}


def recipe(name):
    parts, count = RECIPES[name]
    registry = [u[1] for u in UNITS if u[0] == name][0]
    return registry, json.dumps({
        "type": "forge:ore_shapeless",
        "conditions": [{"type": "forge:mod_loaded", "modid": "csm_technology"}],
        "ingredients": [{"item": "csm:part_" + p} for p in parts],
        "result": {"item": "csm:" + registry, "count": count},
    }, indent=2) + "\n"


def java_frames():
    rows = []
    for i, f in enumerate(FRAMES_DEF):
        name, cells, total, first, is_open, face, deepest, doc = f
        sep = "," if i < len(FRAMES_DEF) - 1 else ";"
        rows.append("  /**\n   * %s\n   */\n  %s(%d, %d, %s, %s, %s, %s)%s"
                    % (doc, name, cells, total, jfloat(first), "true" if is_open else "false",
                       jfloat(face), jfloat(deepest), sep))
    return """package com.micatechnologies.minecraft.csm.technology.rack;

/**
 * Where the units go in each kind of rack: how many cells and U it has, where its first U
 * starts, and how far in the units' faces are. A U is two thirds of a model pixel in every
 * frame, so a unit looks the same in each.
 *
 * <p><b>Written by {@code dev-env-utils/scripts/gen_technology_rack.py}; edit it there.</b> The
 * generator writes the pre-filled racks' models from the same numbers, so they and a filled
 * rack look the same. Not saved anywhere: the order may change.</p>
 *
 * @since 2026.10
 */
public enum RackFrame {

%s

  /** A U, in model pixels. */
  public static final float PITCH = 2.0f / 3.0f;

  /** The units' faces span x %s to %s. */
  public static final float UNIT_X0 = %s;

  /** See {@link #UNIT_X0}. */
  public static final float UNIT_X1 = %s;

  /** In an open frame, the furthest back a unit reaches. */
  public static final float OPEN_BACK = %s;

  private final int cells;
  private final int totalU;
  private final float firstY;
  private final boolean open;
  private final float face;
  private final float deepest;

  RackFrame(int cells, int totalU, float firstY, boolean open, float face, float deepest) {
    this.cells = cells;
    this.totalU = totalU;
    this.firstY = firstY;
    this.open = open;
    this.face = face;
    this.deepest = deepest;
  }

  /** How many block cells tall the rack is (1 or 2). */
  public int getCells() {
    return cells;
  }

  /** How many U it holds. */
  public int getTotalU() {
    return totalU;
  }

  /** The floor of U1, in model pixels from the bottom of the lowest cell. */
  public float getFirstY() {
    return firstY;
  }

  /** Whether it is an open frame, whose units show their backs. */
  public boolean isOpen() {
    return open;
  }

  /** The deepest unit it takes, in model pixels. */
  public float getDeepest() {
    return deepest;
  }

  /**
   * Whether a unit fits this rack's depth.
   *
   * @param type the unit
   * @return whether it fits
   */
  public boolean accepts(RackUnitType type) {
    return type.getDepth() <= deepest;
  }

  /**
   * How far in a unit's face is (z, model pixels from the front).
   *
   * @param depth the unit's depth
   * @return its face's z
   */
  public float frontZ(float depth) {
    return open ? Math.min(face, OPEN_BACK - depth) : face;
  }

  /**
   * The U at a height in the rack, or 0 below U1 and {@code totalU + 1} above the top U.
   *
   * @param y model pixels from the bottom of the lowest cell
   * @return the U
   */
  public int uAt(float y) {
    float k = (y - firstY) / PITCH;
    if (k < 0) {
      return 0;
    }
    return Math.min(totalU + 1, (int) Math.floor(k) + 1);
  }
}
""" % ("\n".join(rows), jfloat(UNIT_X0)[:-1], jfloat(UNIT_X1)[:-1], jfloat(UNIT_X0),
       jfloat(UNIT_X1), jfloat(OPEN_BACK))


def outside_files():
    out = {os.path.join(JAVA_DIR, "RackUnitType.java"): java_units(),
           os.path.join(JAVA_DIR, "RackFrame.java"): java_frames()}
    assert set(RECIPES) == set(u[0] for u in UNITS)
    for u in UNITS:
        registry, text = recipe(u[0])
        out[os.path.join(RECIPES_DIR, registry + ".json")] = text
    return out


def main():
    ap = argparse.ArgumentParser(description=C.name)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--fragments", action="store_true")
    args = ap.parse_args()
    rc = C.main()
    if args.fragments or rc:
        return rc
    stale = []
    for path, text in sorted(outside_files().items()):
        if args.check:
            try:
                with io.open(path, encoding="utf-8", newline="") as fh:
                    same = fh.read() == text
            except IOError:
                same = False
            if not same:
                stale.append(os.path.relpath(path, tc.REPO))
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(text)
    if stale:
        print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
