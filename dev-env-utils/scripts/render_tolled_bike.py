#!/usr/bin/env python3
"""Recreate tolled_bike_lane_sign: white tall sign w/ MUTCD bike symbol, SIGNAL, purple Alto MTA bar, $1.00.
Designed tall (1.31:1) then squished to square for the tall sign model."""
import os
import sys
from PIL import Image, ImageDraw, ImageFont
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)
import csm_layout as layout
FONT = layout.resolve_asset("fonts/highway_gothic_wide.ttf")
TRAFFICSIGNS_OWNER = layout.owner_of_folder("trafficsigns")
def tex_path(name):
    return (layout.resolve_asset("textures/blocks/trafficsigns/" + name)
           or layout.asset_for_write(TRAFFICSIGNS_OWNER, "textures/blocks/trafficsigns/" + name))
WHITE=(249,249,249,255); BLACK=(18,18,18,255); PURPLE=(99,25,150,255)
Wd,Hd = 256, 336

def f(sz): return ImageFont.truetype(FONT, sz)
def fit(d,t,mw,mh,s=90):
    while s>6:
        fo=f(s); bb=d.textbbox((0,0),t,font=fo)
        if bb[2]-bb[0]<=mw and bb[3]-bb[1]<=mh: return fo
        s-=2
    return f(8)
def ctext(d,t,cx,cy,fo,fill):
    bb=d.textbbox((0,0),t,font=fo); d.text((cx-(bb[2]-bb[0])/2-bb[0],cy-(bb[3]-bb[1])/2-bb[1]),t,font=fo,fill=fill)

def run():
    im=Image.new("RGBA",(Wd,Hd),(0,0,0,0)); d=ImageDraw.Draw(im)
    m=8; r=12
    box=(m,m,Wd-m,Hd-m)
    d.rounded_rectangle(box,radius=r,fill=WHITE)
    d.rounded_rectangle((box[0]+3,box[1]+3,box[2]-3,box[3]-3),radius=r-2,outline=BLACK,width=4)
    # bike symbol
    bike=Image.open(os.path.join(SCRIPT_DIR, "mutcd_symbols", "bike_symbol.png")).convert("RGBA")
    bw=Wd-70; bh=int(bike.height*bw/bike.width)
    if bh>110: bh=110; bw=int(bike.width*bh/bike.height)
    bike=bike.resize((bw,bh),Image.LANCZOS)
    im.alpha_composite(bike,(Wd//2-bw//2, 22))
    # SIGNAL
    ctext(d,"SIGNAL",Wd/2,168, fit(d,"SIGNAL",Wd-50,42), BLACK)
    # purple Alto MTA bar
    pb=(box[0]+10, 196, box[2]-10, 252); d.rounded_rectangle(pb,radius=8,fill=PURPLE)
    ctext(d,"Alto MTA",Wd/2,(pb[1]+pb[3])/2, fit(d,"Alto MTA",pb[2]-pb[0]-16,40), WHITE)
    # $1.00
    ctext(d,"$1.00",Wd/2,296, fit(d,"$1.00",Wd-70,46), BLACK)
    im.resize((256,256),Image.LANCZOS).save(tex_path("tolled_bike_lane_sign.png"))
    print("recreated tolled_bike_lane")

if __name__=="__main__": run()
