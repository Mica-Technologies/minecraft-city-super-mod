#!/usr/bin/env python3
"""Every asset the Technology - Networking & IT tab ships: Micafi network gear.

    python dev-env-utils/scripts/gen_technology_networking.py
    python dev-env-utils/scripts/gen_technology_networking.py --check
    python dev-env-utils/scripts/gen_technology_networking.py --fragments   # tab lines to paste

Micafi is the house networking brand, an invented one (the Mica family of Micaplex and
Micarolla), with its own mark drawn in gen_technology_common.py. Nothing here copies a real
maker's name, mark or livery.

What is here, and the class that places each:

- **Micafi 8-Port Desktop Switch** (technology.BlockDeskDeviceFactory): a small white switch,
  its eight ports and their link and activity LEDs on the front, the mark and wordmark on top.
  The LEDs blink by an animated texture (8 frames), so nothing ticks.

Writes models under models/block/technology/networking, textures under
textures/blocks/technology/networking, and the shared swatches under
textures/blocks/technology/common (see gen_technology_common.py).
"""
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_technology_common as tc  # noqa: E402

C = tc.catalogue("gen_technology_networking.py", "networking")
TAB = tc.NETWORKING

C.add_lang("itemGroup.tabtechnetworking",
           ("CSM: Technology - Networking & IT", "CSM: Technik - Netzwerk & IT",
            "CSM: Tecnología - Redes y TI", "CSM: Teknik - Nätverk & IT"))

SIDES = ("east", "west", "south", "down")


# ------------------------------------------------------------------------------------------
# Desktop switches
# ------------------------------------------------------------------------------------------
# The 8-port switch: 8 x 2 x 6 units. Its face texture is 64 px square at 8 texels a unit: the
# front (8 x 2 units) is rows 0..15, the top (8 x 6) rows 16..63, so one animated texture
# carries both and the top's mark is drawn once with the LEDs.
SW8_BOX = (4, 0, 5, 12, 2, 11)
SW8_PORTS = 8
SW8_LINKED = 6          # ports 0..5 have a cable in; 6 and 7 are dark
SW8_FRAMES = 8


def switch_face_frame(frame_no, ports, linked, seed):
    """One frame of a desktop switch's face texture (front band and top)."""
    img = tc.fill(tc.WHITE_PLASTIC, 64, 2, 401)
    # the front: a slightly shaded band with a darker lip at the foot
    tc.rect(img, 0, 0, 64, 16, tc.shade(tc.WHITE_PLASTIC, 0.94))
    tc.rect(img, 0, 15, 64, 16, tc.shade(tc.WHITE_PLASTIC, 0.78))
    pitch = 7
    x0 = (64 - ports * pitch + 1) // 2
    rng = random.Random(seed)
    # every port's activity for every frame, from one seed, so the frames are a fixed loop
    activity = [[rng.random() < 0.55 for _ in range(SW8_FRAMES)] for _ in range(ports)]
    for p in range(ports):
        x = x0 + p * pitch
        # the RJ45 jack: a black opening, its contacts along the top, the latch notch below
        tc.rect(img, x, 5, x + 6, 12, tc.PORT_BLACK)
        tc.rect(img, x + 1, 6, x + 5, 7, tc.PIN_GOLD)
        tc.rect(img, x + 2, 12, x + 4, 13, tc.PORT_BLACK)
        on = p < linked
        tc.rect(img, x, 2, x + 2, 4, tc.LED_GREEN if on else tc.LED_OFF)
        blink = on and activity[p][frame_no]
        tc.rect(img, x + 4, 2, x + 6, 4, tc.LED_AMBER if blink else tc.LED_OFF)
    # the top, drawn as its user sees it (front at the foot): a shallow border, the mark and
    # wordmark, and the white status light by the front edge
    top = Image.new("RGBA", (64, 48), (0, 0, 0, 0))
    tc.frame(top, 1, 1, 63, 47, tc.shade(tc.WHITE_PLASTIC, 0.9))
    tc.micafi_mark(top, 26, 9, 12, tc.MICAFI_TEAL)
    tc.draw_text_centred(top, "MICAFI", 32, 25, tc.shade(tc.GREY_ABS, 0.9))
    tc.rect(top, 30, 40, 34, 42, (246, 250, 255))
    img.alpha_composite(top, (0, 16))
    return img


def switch_8():
    reg = "micafi_switch_8_port"
    tex = "switch_8_port_face"
    tc.animation(C, tex, [lambda f=f: switch_face_frame(f, SW8_PORTS, SW8_LINKED, 8008)
                          for f in range(SW8_FRAMES)], 3)
    x0, y0, z0, x1, y1, z1 = SW8_BOX
    els = [tc.B((x0, y0, z0), (x1, y1, z1), "face", ("north", "up"),
                uv={"north": [0, 0, 16, 4], "up": [0, 4, 16, 16]}, rot={"up": 180}),
           tc.B((x0, y0, z0), (x1, y1, z1), "shell", SIDES)]
    m = tc.model({"face": C.T(tex), "shell": tc.T("white_plastic"),
                  "particle": tc.T("white_plastic")}, els)
    C.add(reg, tc.desk_device_java(reg, SW8_BOX),
          tc.names("Micafi 8-Port Desktop Switch", "Micafi 8-Port-Desktop-Switch",
                   "Switch de Escritorio Micafi de 8 Puertos", "Micafi 8-ports skrivbordsswitch"),
          {reg: tc.with_display(m, tc.desk_display(1.2, 1.5))},
          tc.facing_state(C.M(reg)), tab=TAB)


switch_8()

if __name__ == "__main__":
    sys.exit(C.main())
