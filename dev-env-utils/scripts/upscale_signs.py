#!/usr/bin/env python3
"""Upscale good-but-low-res sign originals to 256x256 (+ mild sharpen) WITHOUT recreating.
Preserves all symbols/elements; just improves readability. Run AFTER recreate_signs.py.
Source = pristine backup; dest = mod texture dir."""
import os
import sys
from PIL import Image, ImageFilter
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_layout as layout
BK = os.path.join(SCRIPT_DIR, 'sign_originals_backup') + os.sep
TRAFFICSIGNS_OWNER = layout.owner_of_folder('trafficsigns')
def tex_path(tex):
    return (layout.resolve_asset('textures/blocks/trafficsigns/' + tex + '.png')
           or layout.asset_for_write(TRAFFICSIGNS_OWNER,
                                     'textures/blocks/trafficsigns/' + tex + '.png'))

# signs kept as their original art but upscaled (symbols can't be cleanly redrawn,
# or recreation would drop an element). Street signs only.
UPSCALE = [
    'generic_hv_danger_sign', 'ladot_no_stopping',
    'alto_dwp_hv_danger_sign', 'right_lane_bus_sign',
    'verizon_dig', 'extreme_heat_danger_sign',
    # non-regulatory signs kept as-is but upscaled (not the advertising ones the user skipped)
    'hg_blissgreen_hwy_sign', 'hear_banjos_sign', 'uia_welcomes_you_sign',
]
# Handled by dedicated renders/images (NOT upscaled, to avoid overwriting):
#   mi_vehicles -> user image; steep_edge -> render_steep_edge; tolled_bike -> render_tolled_bike;
#   safety_glasses -> to be recreated clean.

def run():
    n = 0
    for tex in UPSCALE:
        src = next((BK + tex + e for e in ('.png', '.jpg') if os.path.exists(BK + tex + e)), None)
        if not src:
            print('  MISSING backup', tex); continue
        im = Image.open(src).convert('RGBA').resize((256, 256), Image.LANCZOS)
        im = im.filter(ImageFilter.UnsharpMask(radius=1.5, percent=95, threshold=2))
        im.save(tex_path(tex)); n += 1
    print(f'upscaled {n} signs to 256 (+sharpen)')

if __name__ == '__main__':
    run()
