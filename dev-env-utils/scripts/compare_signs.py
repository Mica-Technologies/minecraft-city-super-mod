#!/usr/bin/env python3
"""Build an ORIGINAL-vs-NEW contact sheet for recreated signs (verification).
Usage: python compare_signs.py tex1 tex2 ...   (tex = basename, no extension)
Writes dev-env-utils/scripts/_compare.png : each row = [original | new] at 128px.
"""
import os
import sys
from PIL import Image, ImageDraw
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_layout as layout  # noqa: E402
BK=os.path.join(SCRIPT_DIR, 'sign_originals_backup') + os.sep
CELL=128; GAP=10; LBL=14
def load(p):
    if p.startswith(BK) and not __import__('os').path.exists(p):
        alt = p[:-4]+'.jpg'
        if __import__('os').path.exists(alt): p = alt
    try: return Image.open(p).convert('RGBA').resize((CELL,CELL), Image.LANCZOS)
    except Exception:
        im=Image.new('RGBA',(CELL,CELL),(40,40,40,255)); ImageDraw.Draw(im).text((6,56),'(missing)',fill=(255,255,255,255)); return im
def tex_path(tex):
    return (layout.resolve_asset('textures/blocks/trafficsigns/' + tex + '.png')
           or os.path.join(layout.asset_dir_for_write(layout.owner_of_folder('trafficsigns'),
                                                        'textures/blocks/trafficsigns'),
                           tex + '.png'))
texs=sys.argv[1:]
rowh=CELL+LBL+GAP
sheet=Image.new('RGBA',(CELL*2+GAP*3, rowh*len(texs)+GAP), (70,70,70,255))
d=ImageDraw.Draw(sheet)
# checker bg so transparency is visible
for yy in range(0,sheet.height,16):
    for xx in range(0,sheet.width,16):
        if (xx//16+yy//16)%2==0: d.rectangle((xx,yy,xx+16,yy+16), fill=(90,90,90,255))
for i,tex in enumerate(texs):
    y=GAP+i*rowh
    sheet.alpha_composite(load(BK+tex+'.png'), (GAP, y+LBL))
    sheet.alpha_composite(load(tex_path(tex)), (GAP*2+CELL, y+LBL))
    d.text((GAP, y), f'{tex}   ORIGINAL  |  NEW', fill=(255,255,255,255))
out=os.path.join(SCRIPT_DIR, '_compare.png')
sheet.convert('RGBA').save(out); print('wrote', out, 'rows:', len(texs))
