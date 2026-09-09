"""Generate the radar speed feedback sign's inventory texture.

The block is drawn entirely by its TESR, so the only texture it needs is the flat icon the item
model shows in the creative tab and the hotbar. Drawn rather than photographed so it matches the
colours the renderer actually uses.

Writes modules/roads/src/main/resources/assets/csm/textures/blocks/trafficaccessories/
radar_speed_sign.png

Run from the repo root:  python dev-env-utils/scripts/gen_radar_speed_sign_texture.py
"""

import os

from PIL import Image, ImageDraw

SIZE = 64

# Matching TileEntityRadarSpeedSignRenderer's palette, and TrafficSignalBodyColor's
# SCHOOL_BUS_YELLOW, which is the panel colour a freshly placed sign has.
PANEL = (255, 209, 33, 255)
BORDER = (13, 13, 13, 255)
WINDOW = (14, 13, 12, 255)
LED = (255, 170, 30, 255)
INK = (17, 17, 17, 255)

OUT_DIR = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm',
                       'textures', 'blocks', 'trafficaccessories')


def main():
    if not os.path.isdir(OUT_DIR):
        raise SystemExit('run from the repo root: %s not found' % OUT_DIR)

    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # The blank: a 3:4 board with the painted edge stripe every sign carries.
    d.rectangle([16, 4, 47, 59], fill=BORDER)
    d.rectangle([18, 6, 45, 57], fill=PANEL)

    # YOUR SPEED, suggested rather than lettered -- at 64 px real text is unreadable, and two
    # bars read as a legend where cramped glyphs read as noise.
    d.rectangle([22, 11, 41, 14], fill=INK)
    d.rectangle([21, 18, 42, 21], fill=INK)

    # The LED window, with two lit digits in it.
    d.rectangle([20, 27, 43, 54], fill=WINDOW)
    for x0 in (24, 34):
        d.rectangle([x0, 31, x0 + 6, 50], fill=LED)
        d.rectangle([x0 + 2, 34, x0 + 4, 47], fill=WINDOW)

    path = os.path.join(OUT_DIR, 'radar_speed_sign.png')
    img.save(path)
    print('wrote %s  %dx%d' % (path, img.width, img.height))


if __name__ == '__main__':
    main()
