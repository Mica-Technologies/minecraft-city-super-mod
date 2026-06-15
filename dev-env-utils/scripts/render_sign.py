#!/usr/bin/env python3
"""Clean road-sign texture renderer (Highway Gothic, supersampled for crisp edges).

Renders 128x128 RGBA sign textures matching the mod's good-quality signs (e.g. land_work_ahead).
Supersamples at 4x then Lanczos-downscales so text/edges are sharp, not blurry.

Shapes: diamond (warning), rect (regulatory landscape), square, plaque (short wide).
Colors are MUTCD-ish defaults but overridable per call.

CLI: python render_sign.py <out.png> <shape> "<LINE1|LINE2|...>" [--bg RGB --fg RGB --border RGB]
"""
import sys, os
from PIL import Image, ImageDraw, ImageFont

SS = 4
SIZE = 128 * SS
FONT_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                         "src/main/resources/assets/csm/fonts/highway_gothic_wide.ttf")

ORANGE = (255, 98, 0, 255)
WHITE = (245, 245, 245, 255)
BLACK = (20, 20, 20, 255)
BORDER = (24, 12, 0, 255)

def _rounded(draw, box, r, fill):
    draw.rounded_rectangle(box, radius=r, fill=fill)

def _fit_font(lines, max_w, max_h, bold=True):
    """Largest font size that fits all lines in max_w x max_h."""
    lo, hi, best = 8, 400, 8
    while lo <= hi:
        mid = (lo + hi) // 2
        f = ImageFont.truetype(FONT_PATH, mid)
        widths = [f.getbbox(l)[2] - f.getbbox(l)[0] for l in lines]
        line_h = (f.getbbox("AY")[3] - f.getbbox("AY")[1]) * 1.25
        total_h = line_h * len(lines)
        if max(widths) <= max_w and total_h <= max_h:
            best = mid; lo = mid + 1
        else:
            hi = mid - 1
    return ImageFont.truetype(FONT_PATH, best)

def _draw_text(img, lines, color, box):
    """Center multi-line text within box=(x0,y0,x1,y1)."""
    x0, y0, x1, y1 = box
    f = _fit_font(lines, x1 - x0, y1 - y0)
    d = ImageDraw.Draw(img)
    asc, desc = f.getmetrics()
    line_h = (asc + desc) * 1.05
    total_h = line_h * len(lines)
    cy = (y0 + y1) / 2 - total_h / 2
    for i, l in enumerate(lines):
        bb = f.getbbox(l)
        w = bb[2] - bb[0]
        x = (x0 + x1) / 2 - w / 2 - bb[0]
        y = cy + i * line_h
        d.text((x, y), l, font=f, fill=color)

def _draw_text_diamond(img, lines, color, cx, cy, R):
    """Center text inside a diamond (|dx|+|dy| <= R), sizing so each line fits the
    diamond's available width AT ITS OWN HEIGHT (widest at center, narrows outward)."""
    d = ImageDraw.Draw(img)
    n = len(lines)
    best = 8
    for sz in range(8, 400, 2):
        f = ImageFont.truetype(FONT_PATH, sz)
        asc, desc = f.getmetrics(); lh = (asc + desc) * 1.04
        if lh * n > 2 * R: break
        ok = True
        for i, l in enumerate(lines):
            yc = (i - (n - 1) / 2) * lh                 # line center offset from cy
            band = abs(yc) + lh / 2                     # farthest vertical extent of this line
            avail = R - band                            # diamond half-width at that extent
            bb = f.getbbox(l); w = bb[2] - bb[0]
            if avail <= 0 or w / 2 > avail:
                ok = False; break
        if ok: best = sz
        else: break
    f = ImageFont.truetype(FONT_PATH, best)
    asc, desc = f.getmetrics(); lh = (asc + desc) * 1.04
    for i, l in enumerate(lines):
        bb = f.getbbox(l); w = bb[2] - bb[0]
        x = cx - w / 2 - bb[0]
        y = cy + (i - (n - 1) / 2) * lh - (asc + desc) / 2
        d.text((x, y), l, font=f, fill=color)

def render(shape, lines, bg=None, fg=BLACK, border=None, out_size=128):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m = 3 * SS          # outer margin (bg fills almost to the edge, like real signs)
    INSET = 2 * SS      # border stroke sits ~2px inboard of the edge
    STROKE = 3 * SS     # thin ~3px border line (matches existing signs)
    bcol = border if border is not None else fg   # border matches font color by default

    if shape == "diamond":
        bg = bg or ORANGE
        side = int((SIZE - 2 * m) / (2 ** 0.5))
        sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
        sd = ImageDraw.Draw(sq)
        r = int(8 * SS)
        sd.rounded_rectangle((0, 0, side - 1, side - 1), radius=r, fill=bg)
        sd.rounded_rectangle((INSET, INSET, side - 1 - INSET, side - 1 - INSET),
                             radius=max(2, r - INSET), outline=bcol, width=STROKE)
        sq = sq.rotate(45, expand=True, resample=Image.BICUBIC)
        img.alpha_composite(sq, ((SIZE - sq.width) // 2, (SIZE - sq.height) // 2))
        R = (SIZE / 2 - m) - (INSET + STROKE) - 4 * SS   # text-safe half-extent inside the border
        _draw_text_diamond(img, lines, fg, SIZE / 2, SIZE / 2, R)
        return img.resize((out_size, out_size), Image.LANCZOS)
    else:  # rect / square / plaque
        bg = bg or WHITE
        if shape == "square":
            box = (m, m, SIZE - m, SIZE - m)
        elif shape == "plaque":  # short wide, centered vertically
            h = (SIZE - 2 * m) * 0.55
            box = (m, (SIZE - h) / 2, SIZE - m, (SIZE + h) / 2)
        else:  # rect landscape
            h = (SIZE - 2 * m) * 0.78
            box = (m, (SIZE - h) / 2, SIZE - m, (SIZE + h) / 2)
        r = int(7 * SS)
        d.rounded_rectangle(box, radius=r, fill=bg)
        d.rounded_rectangle((box[0] + INSET, box[1] + INSET, box[2] - INSET, box[3] - INSET),
                            radius=max(2, r - INSET), outline=bcol, width=STROKE)
        pad = INSET + STROKE + 5 * SS
        tb = (box[0] + pad, box[1] + pad, box[2] - pad, box[3] - pad)

    _draw_text(img, lines, fg, tb)
    return img.resize((out_size, out_size), Image.LANCZOS)

def _arrow(d, cx, cy, w, h, direction, color):
    """MUTCD-style arrow: triangular head (wider than shaft) + rectangular shaft."""
    head = 0.46          # head height fraction
    sw = w * 0.36        # shaft width
    if direction in ("up", "down"):
        sign = -1 if direction == "up" else 1
        apex = (cx, cy + sign * h / 2)
        base = cy + sign * h / 2 - sign * h * head
        d.polygon([apex, (cx - w/2, base), (cx + w/2, base)], fill=color)
        y0, y1 = (base, cy + h/2) if direction == "up" else (cy - h/2, base)
        d.rectangle((cx - sw/2, y0, cx + sw/2, y1), fill=color)
    else:  # left / right
        sign = -1 if direction == "left" else 1
        apex = (cx + sign * w / 2, cy)
        base = cx + sign * w / 2 - sign * w * head
        d.polygon([apex, (base, cy - h/2), (base, cy + h/2)], fill=color)
        x0, x1 = (base, cx + w/2) if direction == "left" else (cx - w/2, base)
        d.rectangle((x0, cy - sw/2, x1, cy + sw/2), fill=color)

def render_diamond_symbol(lines, bg=ORANGE, fg=BLACK, arrow="up",
                          text_box=(0.25, 0.27, 0.75, 0.50), arrow_y=0.64,
                          arrow_w=0.22, arrow_h=0.24, out_size=128):
    """Orange-diamond warning sign with text (upper) + a MUTCD arrow (lower)."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m, INSET, STROKE = 3 * SS, 2 * SS, 3 * SS
    side = int((SIZE - 2 * m) / (2 ** 0.5))
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sq)
    r = int(8 * SS)
    sd.rounded_rectangle((0, 0, side - 1, side - 1), radius=r, fill=bg)
    sd.rounded_rectangle((INSET, INSET, side - 1 - INSET, side - 1 - INSET),
                         radius=max(2, r - INSET), outline=fg, width=STROKE)
    sq = sq.rotate(45, expand=True, resample=Image.BICUBIC)
    img.alpha_composite(sq, ((SIZE - sq.width) // 2, (SIZE - sq.height) // 2))
    if lines:
        _draw_text(img, lines, fg, tuple(SIZE * v for v in text_box))
    _arrow(d, SIZE * 0.5, SIZE * arrow_y, SIZE * arrow_w, SIZE * arrow_h, arrow, fg)
    return img.resize((out_size, out_size), Image.LANCZOS)

def _star_pts(cx, cy, rO, rI, n, rot=-1.5707963):
    import math
    pts = []
    for i in range(2 * n):
        r = rO if i % 2 == 0 else rI
        a = rot + i * math.pi / n
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts

def render_badge(text, bg, star, fg, points=7, out_size=128):
    """Square badge sign: filled square, a multi-point star, and centered text."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m = 4 * SS
    d.rounded_rectangle((m, m, SIZE - m, SIZE - m), radius=7 * SS, fill=bg)
    d.rounded_rectangle((m + 3 * SS, m + 3 * SS, SIZE - m - 3 * SS, SIZE - m - 3 * SS),
                        radius=5 * SS, outline=star, width=2 * SS)
    cx = cy = SIZE / 2
    rO = SIZE * 0.40; rI = rO * 0.50
    d.polygon(_star_pts(cx, cy, rO, rI, points), fill=star)
    # keep text inside the star's central white area (narrower than the point span)
    _draw_text(img, [text], fg, (cx - rO * 0.52, cy - rO * 0.18, cx + rO * 0.52, cy + rO * 0.18))
    return img.resize((out_size, out_size), Image.LANCZOS)

def render_oneway(direction, out_size=128):
    """Black arrow-shaped ONE WAY sign (R6-1 style) with white text + white inset border."""
    import math
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m = 4 * SS
    bh = SIZE * 0.42; cy = SIZE / 2
    y0, y1 = cy - bh / 2, cy + bh / 2
    plen = (SIZE - 2 * m) * 0.26
    if direction == "right":
        bx0, bx1 = m, SIZE - m - plen
        poly = [(bx0, y0), (bx1, y0), (SIZE - m, cy), (bx1, y1), (bx0, y1)]
        tbx = (bx0 + 9 * SS, y0, bx1 - 2 * SS, y1)
    else:
        bx0, bx1 = m + plen, SIZE - m
        poly = [(bx0, y0), (bx1, y0), (bx1, y1), (bx0, y1), (m, cy)]
        tbx = (bx0 + 2 * SS, y0, bx1 - 9 * SS, y1)
    cxp = sum(p[0] for p in poly) / len(poly); cyp = sum(p[1] for p in poly) / len(poly)
    d.polygon(poly, fill=(245, 245, 245, 255))                     # white border base
    ins = 4 * SS
    def shrink(p):
        dx, dy = p[0] - cxp, p[1] - cyp; L = math.hypot(dx, dy) or 1
        return (p[0] - dx / L * ins, p[1] - dy / L * ins)
    d.polygon([shrink(p) for p in poly], fill=(20, 20, 20, 255))   # black face
    _draw_text(img, ["ONE WAY"], (245, 245, 245, 255), (tbx[0] + 6 * SS, y0 + 8 * SS, tbx[2] - 6 * SS, y1 - 8 * SS))
    return img.resize((out_size, out_size), Image.LANCZOS)

def render_wide(lines, bg=ORANGE, fg=BLACK, border=None, ratio=2.36, out_size=128):
    """Render a wide rectangular sign at its true aspect, then squish to a square texture.
    The medium_wide model stretches it back to `ratio` in-game (matches end_road_work)."""
    Wd, Hd = int(out_size * ratio * SS), out_size * SS
    img = Image.new("RGBA", (Wd, Hd), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bcol = border if border is not None else fg
    m, INSET, STROKE, r = 3 * SS, 2 * SS, 3 * SS, 7 * SS
    box = (m, m, Wd - m, Hd - m)
    d.rounded_rectangle(box, radius=r, fill=bg)
    d.rounded_rectangle((box[0]+INSET, box[1]+INSET, box[2]-INSET, box[3]-INSET),
                        radius=max(2, r-INSET), outline=bcol, width=STROKE)
    pad = INSET + STROKE + 6 * SS
    _draw_text(img, lines, fg, (box[0]+pad, box[1]+pad, box[2]-pad, box[3]-pad))
    return img.resize((out_size, out_size), Image.LANCZOS)

def render_composite(header_lines, body_lines, header_bg, header_fg,
                     body_bg, body_fg, border=None, split=0.32, out_size=128):
    """ANSI-style header band + body text on a rounded-rect sign (supersampled).
    Thin border stroke inset from the edge, matching the existing sign style."""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m = 3 * SS
    INSET = 2 * SS
    STROKE = 3 * SS
    bcol = border if border is not None else body_fg
    r = int(7 * SS)
    box = (m, m, SIZE - m, SIZE - m)
    d.rounded_rectangle(box, radius=r, fill=body_bg)              # body fills the sign
    split_y = box[1] + (box[3]-box[1]) * split
    # header band (clipped to rounded top via a temp mask is overkill; rounded corners are tiny)
    d.rectangle((box[0], box[1] + r, box[2], split_y), fill=header_bg)
    d.rounded_rectangle((box[0], box[1], box[2], box[1] + 2*r), radius=r, fill=header_bg)
    d.rectangle((box[0], split_y - SS, box[2], split_y), fill=bcol)   # divider
    # thin inset border stroke (matches font color)
    d.rounded_rectangle((box[0]+INSET, box[1]+INSET, box[2]-INSET, box[3]-INSET),
                        radius=max(2, r-INSET), outline=bcol, width=STROKE)
    pad = INSET + STROKE + 5 * SS
    _draw_text(img, header_lines, header_fg, (box[0]+pad, box[1]+pad, box[2]-pad, split_y-pad))
    _draw_text(img, body_lines, body_fg, (box[0]+pad, split_y+pad, box[2]-pad, box[3]-pad))
    return img.resize((out_size, out_size), Image.LANCZOS)

if __name__ == "__main__":
    out, shape, text = sys.argv[1], sys.argv[2], sys.argv[3]
    lines = text.split("|")
    kw = {}
    for i, a in enumerate(sys.argv):
        if a == "--bg": kw["bg"] = tuple(int(x) for x in sys.argv[i+1].split(",")) + (255,)
        if a == "--fg": kw["fg"] = tuple(int(x) for x in sys.argv[i+1].split(",")) + (255,)
        if a == "--border": kw["border"] = tuple(int(x) for x in sys.argv[i+1].split(",")) + (255,)
    render(shape, lines, **kw).save(out)
    print("wrote", out)
