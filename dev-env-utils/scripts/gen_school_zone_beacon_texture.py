"""Generate the school zone beacon assembly's inventory texture.

The block is drawn entirely by its TESR, so the only texture it needs is the flat icon the
item model shows in the creative tab and the hotbar. Drawn rather than photographed so it
matches the colours the renderer actually uses.

Writes modules/roads/src/main/resources/assets/csm/textures/blocks/trafficaccessories/
school_zone_beacon.png

Run from the repo root:  python dev-env-utils/scripts/gen_school_zone_beacon_texture.py
"""

import os

from PIL import Image, ImageDraw

SIZE = 64

# Matching TileEntitySchoolZoneBeaconRenderer's palette so the icon and the block agree.
PANEL = (184, 237, 51, 255)
BORDER = (13, 13, 13, 255)
HOUSING = (33, 33, 36, 255)
LAMP_LIT = (255, 184, 26, 255)
LAMP_DARK = (51, 38, 13, 255)
INK = (17, 17, 17, 255)

OUT_DIR = os.path.join('modules', 'roads', 'src', 'main', 'resources', 'assets', 'csm',
                       'textures', 'blocks', 'trafficaccessories')


def main():
    if not os.path.isdir(OUT_DIR):
        raise SystemExit('run from the repo root: %s not found' % OUT_DIR)

    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Beacon bar across the top, one lamp lit and one dark so the icon reads as flashing
    # rather than as a dead assembly.
    d.rectangle([14, 3, 49, 14], fill=HOUSING, outline=BORDER)
    d.ellipse([19, 5, 27, 13], fill=LAMP_LIT, outline=BORDER)
    d.ellipse([36, 5, 44, 13], fill=LAMP_DARK, outline=BORDER)

    # Sign panel.
    d.rectangle([16, 17, 47, 60], fill=PANEL, outline=BORDER)

    # Legend, suggested rather than lettered -- at 64 px real text is unreadable, and a row of
    # bars reads as a sign where cramped glyphs read as noise.
    for y in (21, 26, 31):
        d.rectangle([21, y, 42, y + 2], fill=INK)
    # The posted number, drawn as a chunky block pair.
    d.rectangle([23, 37, 30, 49], fill=INK)
    d.rectangle([33, 37, 40, 49], fill=INK)
    d.rectangle([25, 39, 28, 47], fill=PANEL)
    d.rectangle([35, 39, 38, 47], fill=PANEL)
    # "WHEN FLASHING" plaque line.
    d.rectangle([21, 53, 42, 55], fill=INK)

    path = os.path.join(OUT_DIR, 'school_zone_beacon.png')
    img.save(path)
    print('wrote %s  %dx%d' % (path, img.width, img.height))


if __name__ == '__main__':
    main()
