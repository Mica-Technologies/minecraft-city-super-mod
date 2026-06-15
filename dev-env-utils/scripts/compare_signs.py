#!/usr/bin/env python3
"""Build an ORIGINAL-vs-NEW contact sheet for recreated signs (verification).
Usage: python compare_signs.py tex1 tex2 ...   (tex = basename, no extension)
Writes dev-env-utils/scripts/_compare.png : each row = [original | new] at 128px.
"""
import sys
from PIL import Image, ImageDraw
T='src/main/resources/assets/csm/textures/blocks/trafficsigns/'
BK='dev-env-utils/scripts/sign_originals_backup/'
CELL=128; GAP=10; LBL=14
def load(p):
    if p.startswith(BK) and not __import__('os').path.exists(p):
        alt = p[:-4]+'.jpg'
        if __import__('os').path.exists(alt): p = alt
    try: return Image.open(p).convert('RGBA').resize((CELL,CELL), Image.LANCZOS)
    except Exception:
        im=Image.new('RGBA',(CELL,CELL),(40,40,40,255)); ImageDraw.Draw(im).text((6,56),'(missing)',fill=(255,255,255,255)); return im
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
    sheet.alpha_composite(load(T+tex+'.png'), (GAP*2+CELL, y+LBL))
    d.text((GAP, y), f'{tex}   ORIGINAL  |  NEW', fill=(255,255,255,255))
out='dev-env-utils/scripts/_compare.png'
sheet.convert('RGBA').save(out); print('wrote', out, 'rows:', len(texs))
