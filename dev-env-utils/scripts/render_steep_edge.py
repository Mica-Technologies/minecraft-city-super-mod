#!/usr/bin/env python3
"""Hybrid cleanup for steep_edge: keep the cleaned yellow diamond + embankment symbol,
but re-render the top CAUTION band and the bottom text crisply (binarized photo text was rough)."""
import os
from PIL import Image, ImageDraw, ImageFont
FONT = "src/main/resources/assets/csm/fonts/highway_gothic_wide.ttf"
T = "src/main/resources/assets/csm/textures/blocks/trafficsigns/"
WHITE=(249,249,249,255); BLACK=(18,18,18,255); YEL=(245,190,28,255)

def f(sz): return ImageFont.truetype(FONT, sz)
def fit(d, text, maxw, maxh, start=80):
    s=start
    while s>6:
        fo=f(s); bb=d.textbbox((0,0),text,font=fo)
        if bb[2]-bb[0]<=maxw and bb[3]-bb[1]<=maxh: return fo
        s-=2
    return f(8)
def ctext(d, text, cx, cy, fo, fill):
    bb=d.textbbox((0,0),text,font=fo); d.text((cx-(bb[2]-bb[0])/2-bb[0], cy-(bb[3]-bb[1])/2-bb[1]), text, font=fo, fill=fill)

def run():
    im = Image.open(T+"steep_edge_sign.png").convert("RGBA")   # current palette-cleaned
    d = ImageDraw.Draw(im); W,H = im.size
    # --- extract the FULL embankment symbol from the cleaned diamond interior ---
    crop = im.crop((90, 58, 170, 146))
    sym = Image.new("RGBA", crop.size, (0,0,0,0)); cp=crop.load(); sp=sym.load()
    for yy in range(crop.height):
        for xx in range(crop.width):
            r,g,b,a = cp[xx,yy]
            if (r+g+b)/3 < 80: sp[xx,yy]=(18,18,18,255)
    bb = sym.getbbox()
    if bb: sym = sym.crop(bb)
    # --- repaint middle white and draw a crisp diamond + the symbol ---
    d.rectangle((0,50,W,153), fill=WHITE)
    side = 72
    sq = Image.new("RGBA",(side,side),(0,0,0,0)); sd=ImageDraw.Draw(sq)
    sd.rounded_rectangle((0,0,side-1,side-1), radius=8, fill=YEL)
    sd.rounded_rectangle((4,4,side-5,side-5), radius=5, outline=BLACK, width=4)
    sq = sq.rotate(45, expand=True, resample=Image.BICUBIC)
    cx, cy = W//2, 101
    im.alpha_composite(sq, (cx-sq.width//2, cy-sq.height//2))
    if bb:
        maxh = 70; sw, sh = sym.size
        if sh > maxh:
            sw = max(1, int(sw*maxh/sh)); sh = maxh; sym = sym.resize((sw, sh), Image.LANCZOS)
        im.alpha_composite(sym, (cx-sw//2, cy-sh//2+2))
    d = ImageDraw.Draw(im)
    # --- repaint top band region white, then a crisp yellow CAUTION band ---
    d.rectangle((0,0,W,52), fill=WHITE)
    bx=(6,6,W-6,46); r=10
    d.rounded_rectangle(bx, radius=r, fill=YEL)
    d.rounded_rectangle((bx[0]+2,bx[1]+2,bx[2]-2,bx[3]-2), radius=r-2, outline=BLACK, width=3)
    # warning triangle (left)
    tx,ty,th = 30, 26, 24
    d.polygon([(tx,ty-th/2),(tx-th*0.6,ty+th/2),(tx+th*0.6,ty+th/2)], outline=BLACK, width=3)
    d.text((tx-3, ty-9), "!", font=f(16), fill=BLACK)
    cf = fit(d, "CAUTION", W-90, 34, start=40)
    ctext(d, "CAUTION", (W+40)/2, 26, cf, BLACK)
    # --- repaint bottom text region white, then crisp 4 lines ---
    d.rectangle((0,150,W,H), fill=WHITE)
    lines=["EDGE/STEEP","EMBANKMENT","KEEP BACK","25 FEET"]
    y0, y1 = 156, 250
    lh = (y1 - y0) / len(lines)
    lf = fit(d, "EMBANKMENT", W-22, lh*0.86, start=40)
    for i,l in enumerate(lines):
        ctext(d, l, W/2, y0 + i*lh + lh/2, lf, BLACK)
    # underline under 25 FEET (the original had one)
    bb=d.textbbox((0,0),"25 FEET",font=lf); uw=bb[2]-bb[0]
    uy=int(y0 + 4*lh - lh*0.12)
    d.rectangle((W/2-uw/2, uy, W/2+uw/2, uy+3), fill=BLACK)
    im.save(T+"steep_edge_sign.png"); print("steep_edge hybrid (crisp band+text, kept diamond)")

if __name__ == "__main__":
    run()
