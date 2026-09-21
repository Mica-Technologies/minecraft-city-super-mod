#!/usr/bin/env python3
"""The public-domain vintage advertisements the Signage & Advertising boards show.

Real print ads from 1900-1916, published in the United States before 1930 and so in the public
domain there, each checked one file at a time against the licence Wikimedia Commons records for
it -- never the collection's. They are fetched on first use into the gitignored _vintage_cache/,
trimmed of their scan margins, and set into the four ad shapes gen_ads.py draws (portrait 2:3,
square, poster 2:1, bulletin 7:2): the ad whole, on a flat backdrop of its own darkened colour,
so a portrait ad on a billboard is shown whole rather than cropped. Every source is written to
ads/vintage-sources.md beside the index.

    python dev-env-utils/scripts/fetch_vintage_ads.py
    python dev-env-utils/scripts/fetch_vintage_ads.py --check           # exit 1 on drift
    python dev-env-utils/scripts/fetch_vintage_ads.py --sheet out.png   # review contact sheet

What is not here, on purpose: patent medicines, alcohol, tobacco and firearms; brands still
trading, which on a board would read as a current endorsement; and the racist caricatures and
slurs that a great deal of advertising of the period carries. The candidate lists these were
chosen from had all of those; every ad below was looked at before it went in.

The Commons API asks for a descriptive User-Agent and polite pacing; this script sends one, pauses
between calls and backs off on HTTP 429.
"""

import argparse
import io
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

from PIL import Image, ImageDraw, ImageFilter

import gen_ads

REPO = gen_ads.REPO
ASSETS = gen_ads.ASSETS
TEX_DIR = os.path.join(ASSETS, "textures", "ads", "vintage")
INDEX_PATH = os.path.join(ASSETS, "ads", "vintage.json")
SOURCES_PATH = os.path.join(ASSETS, "ads", "vintage-sources.md")
CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_vintage_cache")

# The vintage textures' size against gen_ads' shapes.
SCALE = 0.75

API = "https://commons.wikimedia.org/w/api.php"
UA = {"User-Agent": "CSMVintageAdFetch/1.0 "
      "(https://github.com/Mica-Technologies/minecraft-city-super-mod; dev asset script)"}

# id, Commons file, what the picker calls it, year, trim as fractions (left, top, right, bottom)
CATALOGUE = [
    ("port_orchard_route", "File:Steamship Route to Port Orchard (1908) (ADVERT 205).jpeg",
     "Port Orchard Route Steamers", 1908, None),
    ("mobile_ohio_dining_cars", "File:Mobile and Ohio Railroad Dining Cars Advertisement 1902.jpg",
     "Mobile & Ohio Dining Cars", 1902, None),
    ("chicago_dog_show", "File:Chicago Kennel Club's Dog Show, advertising poster, 1902.jpg",
     "Chicago Kennel Club Dog Show", 1902, None),
    ("spaulding_printers",
     "File:George Spaulding and Company, Printers at 414 Clay Street San Francisco ad from 1902 "
     "Printers' Outing (page 3 crop).jpg",
     "Geo. Spaulding & Co. Printers", 1902, None),
    ("fritchle_electric", "File:100-Mile Fritchle Electric Automobile (1908) (ADVERT 104).jpeg",
     "Fritchle Electric", 1908, None),
    ("pierce_arrow", "File:American homes and gardens (1909) (17967343580).jpg",
     "The Pierce-Arrow", 1909, None),
    ("monarch_typewriter", "File:Monarch Visible Typewriter.jpg",
     "Monarch Visible Typewriter", 1910, None),
    ("parker_band_organs",
     "File:Parker's band organs - Famous for beauty and tone ... Grand $10,000 orchestro "
     "harmonium. Largest organ ever built LCCN2018647147.jpg",
     "Parker's Band Organs", 1900, (0.03, 0.02, 0.97, 0.985)),
    ("us_printing_labels",
     "File:FMIB 44410 United States Printing Co Labels, Advertising Cards, Folding Boxes, "
     "Embossed Work.jpeg",
     "United States Printing Co.", 1907, None),
    ("soapine", "File:Kendall Mfg. Co. (estab. 1827) (3092856567).jpg",
     "Soapine", 1900, None),
    ("queen_city_ink_parlour", "File:Augustus Jansson’s Queen City Ink Adverts "
     "48819868086 667f6163c6 b.jpg", "Queen City Printing Ink", 1905, None),
    ("queen_city_ink_bonnet", "File:Augustus Jansson’s Queen City Ink Adverts "
     "48819508653 6716a25135 c.jpg", "Queen City Printing Ink", 1905, None),
    ("chesapeake_york_river", "File:A - M (Page 81) BHL47093304.jpg",
     "Chesapeake & York River Lines", 1905, (0.03, 0.01, 0.97, 0.99)),
    ("mccray_refrigerators",
     "File:McCray Refrigerators. Tile, opal glass and odorless-wood lined. The Lakeside Press, "
     "R.R. Donnelley & Sons Company, Chicago.jpg",
     "McCray Refrigerators", 1903, (0.15, 0.07, 0.86, 0.93)),
    ("hart_kraft", "File:1908 Hart-Kraft Motor Company advertisement for 1909 truck.jpg",
     "Hart-Kraft Power Wagons", 1908, None),
    ("st_louis_car", "File:“ST. LOUIS CAR CO. ST. LOUIS MO.” “Builders of Electric "
     "Cars of every kind” - Electric railway review (IA electricrailwayr19amer) (page 33 "
     "crop).jpg", "St. Louis Car Co.", 1905, None),
    ("meadows_speedway", "File:Meadows Speedway (1909) (ADVERT 494).jpeg",
     "Meadows Speedway Race Meet", 1909, None),
    ("seeing_seattle_car", "File:Seeing Seattle Car Tour (1905) (ADVERT 339).jpeg",
     "Seeing Seattle Car", 1905, None),
]



# --------------------------------------------------------------------------------------------
# Commons
# --------------------------------------------------------------------------------------------

def _api(**kw):
    kw["format"] = "json"
    data = urllib.parse.urlencode(kw).encode()
    for attempt in range(6):
        time.sleep(1.5)
        try:
            request = urllib.request.Request(API, data=data, headers=UA)
            return json.load(urllib.request.urlopen(request, timeout=60))
        except urllib.error.HTTPError as e:
            time.sleep(60 if e.code == 429 else 5)
        except OSError:
            time.sleep(5)
    raise RuntimeError("Commons API unreachable")


def _strip(html):
    import re
    return re.sub(r"<[^>]+>", "", html or "").strip()


def fetch(ad_id, title):
    """The ad's image and its rights record, from the cache or from Commons."""
    os.makedirs(CACHE, exist_ok=True)
    meta_path = os.path.join(CACHE, ad_id + ".json")
    img_path = os.path.join(CACHE, ad_id + ".jpg")
    if os.path.exists(meta_path) and os.path.exists(img_path):
        return Image.open(img_path).convert("RGB"), json.load(open(meta_path, encoding="utf-8"))
    r = _api(action="query", titles=title, prop="imageinfo",
             iiprop="url|size|extmetadata", iiurlwidth=1600)
    page = next(iter(r["query"]["pages"].values()))
    if "imageinfo" not in page:
        raise RuntimeError("missing on Commons: " + title)
    info = page["imageinfo"][0]
    md = info.get("extmetadata", {})
    meta = {
        "title": page["title"],
        "page": info["descriptionurl"],
        "licence": _strip(md.get("LicenseShortName", {}).get("value")),
        "terms": _strip(md.get("UsageTerms", {}).get("value")),
        "date": _strip(md.get("DateTimeOriginal", {}).get("value"))[:80],
        "credit": _strip(md.get("Credit", {}).get("value"))[:200],
    }
    time.sleep(1)
    request = urllib.request.Request(info.get("thumburl") or info["url"], headers=UA)
    data = urllib.request.urlopen(request, timeout=120).read()
    open(img_path, "wb").write(data)
    json.dump(meta, open(meta_path, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
    return Image.open(io.BytesIO(data)).convert("RGB"), meta


def check_rights(ad_id, meta):
    """Refuses anything Commons does not record as public domain."""
    text = (meta["licence"] + " " + meta["terms"]).lower()
    if "public domain" not in text and not meta["licence"].upper().startswith("PD"):
        sys.exit("%s is not recorded as public domain (%s); refusing it" % (ad_id,
                                                                            meta["licence"]))


# --------------------------------------------------------------------------------------------
# Shapes
# --------------------------------------------------------------------------------------------

def compose(ad, w, h):
    """The ad whole, centred on a flat backdrop of its own average colour, darkened: a poster
    pasted on a painted hoarding. A blurred enlargement of the ad was tried first and broke into
    blotches in a 256-colour PNG; a flat colour compresses to nothing and never bands."""
    mean = ad.resize((1, 1), Image.BOX).getpixel((0, 0))
    back = Image.new("RGB", (w, h), tuple(int(c * 0.32) for c in mean))
    margin = 0.94
    fit = min(w * margin / ad.width, h * margin / ad.height)
    fw, fh = max(1, round(ad.width * fit)), max(1, round(ad.height * fit))
    front = ad.resize((fw, fh), Image.LANCZOS)
    x, y = (w - fw) // 2, (h - fh) // 2
    shadow = Image.new("L", (w, h), 0)
    ImageDraw.Draw(shadow).rectangle((x + 3, y + 5, x + fw + 3, y + fh + 5), fill=140)
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=5))
    back.paste((0, 0, 0), (0, 0), shadow)
    back.paste(front, (x, y))
    return back


def trimmed(ad, trim):
    if not trim:
        return ad
    l, t, r, b = trim
    return ad.crop((round(ad.width * l), round(ad.height * t), round(ad.width * r),
                    round(ad.height * b)))


# --------------------------------------------------------------------------------------------
# Output
# --------------------------------------------------------------------------------------------

def outputs():
    files = {}
    index = []
    sources = ["# Vintage advertisement sources", "",
               "Every ad here was published in the United States before 1930 and is in the "
               "public domain there. Each file's licence was checked as Wikimedia Commons "
               "records it, one file at a time. Written by "
               "`dev-env-utils/scripts/fetch_vintage_ads.py`; do not edit by hand.", "",
               "| Ad | Year | Source | Licence |", "|---|---|---|---|"]
    for ad_id, title, brand, year, trim in CATALOGUE:
        image, meta = fetch(ad_id, title)
        check_rights(ad_id, meta)
        image = trimmed(image, trim)
        # A portrait ad set on a 7:2 billboard is a postage stamp in the middle of it; leave that
        # shape out, and a billboard falls back to the poster shape, where the ad is far larger.
        # No square: a square board takes the portrait. And three-quarter size: these are small
        # print ads, and 576 px is more than a kiosk face shows. Together they halve the jar.
        shapes = [s for s in gen_ads.SHAPE_ORDER if s != "square"
                  and (s != "bulletin" or image.width / image.height >= 1.2)]
        for shape in shapes:
            w, h = (round(v * SCALE) for v in gen_ads.SHAPES[shape])
            path = os.path.join(TEX_DIR, "%s_%s.png" % (ad_id, shape))
            files[path] = gen_ads.png_bytes(compose(image, w, h))
        mean = image.resize((1, 1), Image.BOX).getpixel((0, 0))
        index.append(dict(id=ad_id, brand="%s (%d)" % (brand, year), category="vintage",
                          shapes=shapes,
                          background="#%02x%02x%02x" % tuple(int(c * 0.32) for c in mean)))
        sources.append("| %s (%d) | %s | [%s](%s) | %s |" % (
            brand, year, meta["date"] or year, meta["title"][5:].replace("|", "/"), meta["page"],
            meta["licence"]))
    doc = {"source": "vintage", "shapes": {k: [round(v * SCALE) for v in gen_ads.SHAPES[k]]
                                           for k in gen_ads.SHAPE_ORDER}, "ads": index}
    files[INDEX_PATH] = (json.dumps(doc, indent=2) + "\n").encode("utf-8")
    files[SOURCES_PATH] = ("\n".join(sources) + "\n").encode("utf-8")
    return files


def contact_sheet(files, path):
    shots = [(p, Image.open(io.BytesIO(d))) for p, d in sorted(files.items())
             if p.endswith(".png")]
    scale = 0.3
    ids = [c[0] for c in CATALOGUE]
    row_h = int(768 * scale) + 30
    widths = [int(gen_ads.SHAPES[s][0] * scale) for s in gen_ads.SHAPE_ORDER]
    sheet = Image.new("RGB", (sum(widths) + 60, row_h * len(ids)), (40, 40, 44))
    draw = ImageDraw.Draw(sheet)
    font = gen_ads.pil_font("poppins-semi", 14)
    for r, ad_id in enumerate(ids):
        y = r * row_h
        draw.text((10, y + 4), ad_id, font=font, fill=(230, 230, 230))
        x = 10
        for shape, sw in zip(gen_ads.SHAPE_ORDER, widths):
            im = dict(shots).get(os.path.join(TEX_DIR, "%s_%s.png" % (ad_id, shape)))
            if im is None:
                x += sw + 12
                continue
            sheet.paste(im.convert("RGB").resize((sw, int(im.height * scale))), (x, y + 24))
            x += sw + 12
    sheet.save(path)
    print("wrote " + path)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--sheet", metavar="PNG")
    args = parser.parse_args()
    files = outputs()
    if args.sheet:
        contact_sheet(files, args.sheet)
        return 0
    stale = [p for p, d in files.items() if not os.path.exists(p) or open(p, "rb").read() != d]
    extra = []
    if os.path.isdir(TEX_DIR):
        extra = [os.path.join(TEX_DIR, n) for n in os.listdir(TEX_DIR)
                 if os.path.join(TEX_DIR, n) not in files]
    if args.check:
        for p in stale:
            print("stale: " + os.path.relpath(p, REPO))
        for p in extra:
            print("not generated: " + os.path.relpath(p, REPO))
        return 1 if stale or extra else 0
    os.makedirs(TEX_DIR, exist_ok=True)
    for p in extra:
        os.remove(p)
    for p in stale:
        with open(p, "wb") as out:
            out.write(files[p])
    size = sum(len(d) for p, d in files.items() if p.endswith(".png"))
    print("%d ads, %d written, %.1f MB" % (len(CATALOGUE), len(stale), size / 1e6))
    return 0


if __name__ == "__main__":
    sys.exit(main())
