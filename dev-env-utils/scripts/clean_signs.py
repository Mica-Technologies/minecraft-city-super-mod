#!/usr/bin/env python3
"""Precise per-image cleanup for photo-derived (upscaled) sign textures.
Snaps each pixel to the sign's true palette: dark->black, accent-hue->solid accent, else->bg.
This removes JPEG splotches / dirty backgrounds and gives flat solid color regions, while keeping
the symbol/text. Per-sign palette is defined in CONFIG. Light median denoise first.

Run AFTER upscale_signs.py (overwrites those textures). Verify with compare_signs.py.
"""
import os
from PIL import Image, ImageFilter
BK='dev-env-utils/scripts/sign_originals_backup/'
T='src/main/resources/assets/csm/textures/blocks/trafficsigns/'
WHITE=(249,249,249,255); BLACK=(18,18,18,255)
YEL=(245,190,28,255); RED=(200,12,20,255); GREEN=(0,120,60,255)
BLUE=(0,70,150,255); ORANGE=(245,120,0,255)

# hue predicates (on r,g,b)
def is_yellow(r,g,b): return r>120 and g>95 and (r-b)>32 and r>=g
def is_red(r,g,b):    return r>110 and r-g>45 and r-b>45
def is_green(r,g,b):  return g>75 and g-r>18 and g-b>12
def is_blue(r,g,b):   return b>90 and b-r>30 and b-g>8
def is_orange(r,g,b): return r>150 and 55<g<165 and b<95 and r-g>45
def is_white(r,g,b):  return min(r,g,b) > 150
BROWN=(83,53,20,255)

# per sign: (bg_color, dark_threshold, [(accent_color, predicate), ...])
CONFIG = {
  'steep_edge_sign':     (WHITE, 72, [(YEL, is_yellow)]),
  'pay_to_cross_sign':   (WHITE, 115, []),   # white/black only
  'hear_banjos_sign':    (BROWN, 45, [(WHITE, is_white)]),
}

def clean(tex, bg, dark_thr, accents):
    src = next((BK+tex+e for e in ('.jpg','.png') if os.path.exists(BK+tex+e)), None)
    if not src: print('  MISSING', tex); return False
    im = Image.open(src).convert('RGB').resize((256,256), Image.LANCZOS).filter(ImageFilter.MedianFilter(3)).convert('RGBA')
    px = im.load(); W,H = im.size
    for y in range(H):
        for x in range(W):
            r,g,b,a = px[x,y]; br=(r+g+b)/3
            if br < dark_thr: px[x,y]=BLACK; continue
            for col,pred in accents:
                if pred(r,g,b): px[x,y]=col; break
            else: px[x,y]=bg
    im.save(T+tex+'.png'); return True

def run(only=None):
    n=0
    for tex,(bg,dt,acc) in CONFIG.items():
        if only and tex not in only: continue
        if clean(tex,bg,dt,acc): n+=1; print('  cleaned',tex)
    print(f'cleaned {n} signs')

if __name__ == '__main__':
    run()
