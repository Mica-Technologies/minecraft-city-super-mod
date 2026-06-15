#!/usr/bin/env python3
"""Render the NYC-style BUS LANE sign (SR-1874) to match the real proportions.

Designed at the true wide aspect (~2.36:1) then squished to 128x128, because the
medium_wide model stretches a square texture horizontally (same as end_road_work).
3 bands: blue BUS LANE + bus icons / black 7AM-7PM·MON-FRI / white down-arrow + BUSES ONLY & RIGHT TURNS.
"""
import os
from PIL import Image, ImageDraw, ImageFont

FONT = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                    "src/main/resources/assets/csm/fonts/highway_gothic_wide.ttf")
BLUE = (0, 86, 184, 255); BLACK = (15, 15, 15, 255); WHITE = (248, 248, 248, 255)
Wd, Hd = 1180, 500   # working canvas, ratio 2.36

def font(sz): return ImageFont.truetype(FONT, sz)

def fit(d, text, box, fill, max_sz, anchor="mm"):
    x0, y0, x1, y1 = box
    sz = max_sz
    while sz > 6:
        f = font(sz); bb = d.textbbox((0, 0), text, font=f)
        if bb[2]-bb[0] <= x1-x0 and bb[3]-bb[1] <= y1-y0: break
        sz -= 2
    f = font(sz)
    d.text(((x0+x1)//2, (y0+y1)//2), text, font=f, fill=fill, anchor=anchor)

def bus_icon(d, cx, cy, w, h, color):
    """Simple front-view bus silhouette in `color`."""
    bw, bh = w, h
    x0, y0 = cx-bw//2, cy-bh//2
    d.rounded_rectangle((x0, y0, x0+bw, y0+bh), radius=bh//6, fill=color)
    # windows (cut to BLUE band color) -- two panes near top
    pad = bw//8; wy0 = y0+bh//6; wy1 = y0+bh//2
    d.rounded_rectangle((x0+pad, wy0, cx-bw//16, wy1), radius=bh//14, fill=BLUE)
    d.rounded_rectangle((cx+bw//16, wy0, x0+bw-pad, wy1), radius=bh//14, fill=BLUE)
    # headlights (blue dots) bottom corners
    r = bw//12
    d.ellipse((x0+pad, y0+bh-2*r-pad//2, x0+pad+2*r, y0+bh-pad//2), fill=BLUE)
    d.ellipse((x0+bw-pad-2*r, y0+bh-2*r-pad//2, x0+bw-pad, y0+bh-pad//2), fill=BLUE)

def down_arrow(d, cx, cy, w, h, color):
    sw = w*0.34
    d.rectangle((cx-sw/2, cy-h/2, cx+sw/2, cy+h*0.05), fill=color)
    d.polygon([(cx-w/2, cy+h*0.0), (cx+w/2, cy+h*0.0), (cx, cy+h/2)], fill=color)

def render():
    img = Image.new("RGBA", (Wd, Hd), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m = 10; r = 22
    box = (m, m, Wd-m, Hd-m)
    d.rounded_rectangle(box, radius=r, fill=WHITE)
    b1 = m + (Hd-2*m)*0.30      # blue/black divide
    b2 = m + (Hd-2*m)*0.50      # black/white divide
    # blue band
    d.rounded_rectangle((box[0], box[1], box[2], b1+r), radius=r, fill=BLUE)
    d.rectangle((box[0], box[1]+r, box[2], b1), fill=BLUE)
    # black band
    d.rectangle((box[0], b1, box[2], b2), fill=BLACK)
    # --- band 1: bus icons + BUS LANE ---
    ih = (b1-box[1])*0.62; iw = ih*1.15
    bus_icon(d, box[0]+iw*0.9, (box[1]+b1)//2, int(iw), int(ih), WHITE)
    bus_icon(d, box[2]-iw*0.9, (box[1]+b1)//2, int(iw), int(ih), WHITE)
    fit(d, "BUS  LANE", (box[0]+iw*1.7, box[1]+8, box[2]-iw*1.7, b1-8), WHITE, 200)
    # --- band 2: times ---
    midx = (box[0]+box[2])//2
    fit(d, "7AM - 7PM", (box[0]+30, b1+6, midx-10, b2-6), WHITE, 130)
    fit(d, "MON - FRI", (midx+10, b1+6, box[2]-30, b2-6), WHITE, 130)
    # --- band 3: arrow + BUSES ONLY + & RIGHT TURNS ---
    ax = box[0]+(box[2]-box[0])*0.18
    down_arrow(d, ax, (b2+box[3])//2, (box[2]-box[0])*0.16, (box[3]-b2)*0.62, BLACK)
    tx0 = box[0]+(box[2]-box[0])*0.34
    fit(d, "BUSES ONLY", (tx0, b2+10, box[2]-20, b2+(box[3]-b2)*0.62), BLACK, 200)
    fit(d, "& RIGHT TURNS", (tx0, b2+(box[3]-b2)*0.60, box[2]-20, box[3]-10), BLACK, 110)
    # thin outer border (matches text=black)
    d.rounded_rectangle((box[0]+6, box[1]+6, box[2]-6, box[3]-6), radius=r-4, outline=BLACK, width=7)
    return img.resize((128, 128), Image.LANCZOS)

if __name__ == "__main__":
    out = "src/main/resources/assets/csm/textures/blocks/trafficsigns/bus_lane_sign.png"
    render().save(out); print("wrote", out)
    # wide preview (un-squished) for visual check
    render().resize((128,128)).save("/tmp/buslane_sq.png")
