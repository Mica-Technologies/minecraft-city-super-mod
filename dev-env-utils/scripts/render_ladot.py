#!/usr/bin/env python3
"""Bespoke renderers for two LADOT street signs (vertical SIGNAL SYNC + NO STOPPING)."""
import os
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout

FONT = layout.resolve_asset("fonts/highway_gothic_wide.ttf")
SS = 4; S = 128 * SS
BLUE = (0, 0, 168, 255); YELLOW = (252, 209, 22, 255); WHITE = (248, 248, 248, 255)
BLACK = (16, 16, 16, 255); RED = (210, 0, 0, 255); GREEN = (0, 120, 60, 255)
TRAFFICSIGNS_OWNER = layout.owner_of_folder("trafficsigns")
def tex_path(name):
    return (layout.resolve_asset("textures/blocks/trafficsigns/" + name)
           or layout.asset_for_write(TRAFFICSIGNS_OWNER, "textures/blocks/trafficsigns/" + name))

def f(sz): return ImageFont.truetype(FONT, sz)
def fit(d, text, maxw, maxh, start=400):
    sz = start
    while sz > 6:
        fo = f(sz); bb = d.textbbox((0, 0), text, font=fo)
        if bb[2]-bb[0] <= maxw and bb[3]-bb[1] <= maxh: return fo
        sz -= 4
    return f(8)
def arrow_up(d, cx, cy, w, h, color):
    sw = w*0.34; head = 0.5
    d.polygon([(cx, cy-h/2), (cx-w/2, cy-h/2+h*head), (cx+w/2, cy-h/2+h*head)], fill=color)
    d.rectangle((cx-sw/2, cy-h/2+h*head, cx+sw/2, cy+h/2), fill=color)

def signal_sync():
    # portrait blue sign: small rotated SIGNAL + up-arrow (left column), big stacked SYNC (right)
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    m = 4*SS; r = 6*SS
    bw = S*0.56; x0 = (S-bw)/2
    box = (x0, m, x0+bw, S-m)
    d.rounded_rectangle(box, radius=r, fill=BLUE)
    d.rounded_rectangle((box[0]+3*SS, box[1]+3*SS, box[2]-3*SS, box[3]-3*SS), radius=r-2, outline=WHITE, width=3*SS)
    bh = box[3]-box[1]
    lcol_w = bw*0.36
    lcx = box[0] + lcol_w/2 + 4*SS
    # big SYNC stacked in the right column
    rcx = box[0] + lcol_w + (bw-lcol_w)/2
    letter_h = (bh - 16*SS)/4
    yfont = fit(d, "S", (bw-lcol_w)*0.92, letter_h*0.98)
    yy = box[1] + 8*SS
    for ch in "SYNC":
        bb = d.textbbox((0,0), ch, font=yfont)
        d.text((rcx-(bb[2]-bb[0])/2-bb[0], yy-bb[1]), ch, font=yfont, fill=WHITE)
        yy += letter_h
    # rotated SIGNAL (reads bottom->top) in upper-left
    sfont = fit(d, "SIGNAL", bh*0.52, lcol_w*0.7)
    ti = Image.new("RGBA", (int(bh*0.6), int(lcol_w*0.8)), (0,0,0,0))
    ImageDraw.Draw(ti).text((0,0), "SIGNAL", font=sfont, fill=WHITE)
    ti = ti.crop(ti.getbbox()).rotate(90, expand=True)
    img.alpha_composite(ti, (int(lcx-ti.width/2), int(box[1]+8*SS)))
    # up-arrow below SIGNAL in left column
    arrow_up(d, lcx, box[3]-bh*0.26, lcol_w*0.5, bh*0.30, WHITE)
    img.resize((128,128), Image.LANCZOS).save(tex_path("ladot_signal_sync.png"))

def no_stopping():
    # upscale the original (keeps the tow-truck symbols + layout) -- don't recreate
    g = Image.open("dev-env-utils/scripts/sign_originals_backup/ladot_no_stopping.png").convert("RGBA")
    g = g.resize((256,256), Image.LANCZOS).filter(ImageFilter.UnsharpMask(radius=1.5, percent=90, threshold=2))
    g.save(tex_path("ladot_no_stopping.png"))

if __name__ == "__main__":
    signal_sync(); no_stopping(); print("rendered ladot_signal_sync, ladot_no_stopping")
