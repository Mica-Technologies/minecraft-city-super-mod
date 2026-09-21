"""The parody ad catalogue and its illustrations, for ``gen_ads.py``.

Every brand here is invented. An illustration is a function of the ad's palette returning SVG
drawn in a 0..100 box; ``gen_ads.py`` places it. Keep the shapes bold and few: an ad is read
from across a street at a couple of dozen texture pixels a block.

Catalogue fields:
    id            registry-safe name, also the texture file prefix; never rename one that shipped
    brand         the invented brand
    brand_font    key into gen_ads.FONTS
    headline      the big line; *word* takes the accent colour, | forces a break
    headline_short  optional shorter headline for the 7:2 billboard
    head_font     key into gen_ads.FONTS
    sub           the supporting line (optional)
    cta           call to action: fictional 555-01xx numbers only, no real domains
    fine          the fine print joke (optional)
    art           name of the illustration function below
    art_side      "left" or "right" in the landscape shapes
    deco          background decoration: rays, circle, stripes, diag, dots, grid, or None
    palette       bg, bg2, fg, accent, brand, ink, band, band_ink, deco, fine
    category      food, drink, auto, legal, insurance, home, tech, health, travel, civic,
                  finance, services, entertainment; "house" is the fallback ad only
"""


# --------------------------------------------------------------------------------------------
# Illustrations (0..100 box)
# --------------------------------------------------------------------------------------------

def burger(p):
    return """
<ellipse cx="50" cy="88" rx="40" ry="5" fill="#000" opacity="0.18"/>
<path d="M12 70 H88 V76 Q88 86 76 86 H24 Q12 86 12 76 Z" fill="#d9913a"/>
<rect x="10" y="60" width="80" height="11" rx="3" fill="#5a2e1a"/>
<path d="M10 58 L20 64 L30 57 L40 64 L50 57 L60 64 L70 57 L80 64 L90 58 V62 H10 Z"
      fill="#f4c430"/>
<rect x="12" y="52" width="76" height="7" rx="3" fill="#e0402b"/>
<path d="M8 50 Q18 44 28 50 Q38 44 48 50 Q58 44 68 50 Q78 44 92 50 V54 H8 Z" fill="#5cb84a"/>
<path d="M12 48 Q12 20 50 18 Q88 20 88 48 Z" fill="#e8a03e"/>
<path d="M18 44 Q20 26 50 23" stroke="#f6c87a" stroke-width="3" fill="none"
      stroke-linecap="round"/>
<g fill="#fff4d6">
  <rect x="30" y="28" width="4" height="4" rx="1"/><rect x="46" y="24" width="4" height="4" rx="1"/>
  <rect x="62" y="29" width="4" height="4" rx="1"/><rect x="38" y="36" width="4" height="4" rx="1"/>
  <rect x="56" y="37" width="4" height="4" rx="1"/><rect x="72" y="38" width="4" height="4" rx="1"/>
</g>"""


def explosion_shield(p):
    return """
<path d="M50 4 L58 22 L76 10 L72 30 L94 30 L78 44 L96 58 L74 60 L82 80 L62 70 L56 92 L46 74
         L30 90 L30 68 L8 72 L22 56 L4 44 L24 38 L14 18 L34 26 Z" fill="#ffb300"/>
<path d="M50 18 L55 30 L68 24 L64 36 L78 38 L66 47 L76 58 L62 58 L64 72 L53 64 L48 78 L42 64
         L30 72 L34 58 L20 56 L32 47 L22 38 L37 36 L32 24 L45 30 Z" fill="#ff5a1f"/>
<path d="M50 30 L70 36 V52 Q70 68 50 78 Q30 68 30 52 V36 Z" fill="%s" stroke="#fff"
      stroke-width="3"/>
<path d="M41 53 L48 60 L60 45" stroke="#fff" stroke-width="5" fill="none"
      stroke-linecap="round" stroke-linejoin="round"/>""" % p["accent"]


def gavel(p):
    return """
<ellipse cx="50" cy="90" rx="36" ry="5" fill="#000" opacity="0.2"/>
<rect x="22" y="78" width="56" height="10" rx="2" fill="#7a4a24"/>
<rect x="28" y="72" width="44" height="8" rx="2" fill="#9c6232"/>
<g transform="rotate(-35 50 45)">
  <rect x="47" y="34" width="6" height="52" rx="2" fill="#b57a42"/>
  <rect x="28" y="16" width="44" height="22" rx="4" fill="#8a5429"/>
  <rect x="24" y="14" width="8" height="26" rx="2" fill="#c9924f"/>
  <rect x="68" y="14" width="8" height="26" rx="2" fill="#c9924f"/>
</g>
<g stroke="%s" stroke-width="3" stroke-linecap="round">
  <path d="M74 62 L84 56"/><path d="M76 70 L88 70"/><path d="M70 55 L74 45"/>
</g>""" % p["accent"]


def energy_can(p):
    return """
<ellipse cx="50" cy="93" rx="22" ry="4" fill="#000" opacity="0.2"/>
<path d="M32 14 Q32 8 38 8 H62 Q68 8 68 14 V86 Q68 92 62 92 H38 Q32 92 32 86 Z" fill="#1c1c24"/>
<rect x="32" y="16" width="36" height="68" fill="#2a2a36"/>
<path d="M32 30 L68 22 V34 L32 42 Z" fill="%s"/>
<path d="M32 62 L68 54 V66 L32 74 Z" fill="%s"/>
<path d="M53 36 L42 54 H50 L46 70 L60 48 H52 L56 36 Z" fill="#fff"/>
<rect x="36" y="16" width="4" height="68" fill="#fff" opacity="0.12"/>
<ellipse cx="50" cy="10" rx="14" ry="3" fill="#8d8d99"/>
<rect x="46" y="8" width="8" height="3" rx="1" fill="#c8c8d2"/>""" % (p["accent"], p["accent"])


def traffic_light(p):
    return """
<rect x="46" y="70" width="8" height="28" fill="#3b3f45"/>
<rect x="30" y="6" width="40" height="70" rx="8" fill="#23262b"/>
<rect x="30" y="6" width="40" height="70" rx="8" fill="none" stroke="#3d424a" stroke-width="2"/>
<circle cx="50" cy="20" r="8.5" fill="#4a1512"/>
<circle cx="50" cy="41" r="8.5" fill="#4a3a10"/>
<circle cx="50" cy="62" r="11" fill="#39ff7a" opacity="0.35"/>
<circle cx="50" cy="62" r="8.5" fill="#39e36f"/>
<circle cx="47" cy="59" r="2.5" fill="#d8ffe4"/>
<path d="M24 26 Q14 40 24 54" stroke="%s" stroke-width="3" fill="none" stroke-linecap="round"/>
<path d="M76 26 Q86 40 76 54" stroke="%s" stroke-width="3" fill="none" stroke-linecap="round"/>
""" % (p["accent"], p["accent"])


def mattress(p):
    return """
<ellipse cx="50" cy="84" rx="44" ry="5" fill="#000" opacity="0.18"/>
<path d="M8 56 L30 40 H92 L70 56 Z" fill="#f4f0ff"/>
<path d="M8 56 H70 V78 H8 Z" fill="#dcd4f2"/>
<path d="M70 56 L92 40 V62 L70 78 Z" fill="#bfb3e0"/>
<g stroke="#a597d4" stroke-width="1.5" fill="none">
  <path d="M8 62 H70"/><path d="M8 72 H70"/><path d="M70 62 L92 46"/><path d="M70 72 L92 56"/>
</g>
<g fill="#b8abe3"><circle cx="30" cy="47" r="1.6"/><circle cx="50" cy="47" r="1.6"/>
  <circle cx="70" cy="47" r="1.6"/><circle cx="40" cy="52" r="1.6"/><circle cx="60" cy="52" r="1.6"/>
</g>
<g transform="rotate(-30 70 22)">
  <rect x="68" y="16" width="4" height="30" rx="1" fill="#8a5a2b"/>
  <path d="M56 14 Q70 4 84 14 L80 18 Q70 12 60 18 Z" fill="%s"/>
</g>""" % p["accent"]


def cone(p):
    return """
<ellipse cx="50" cy="90" rx="38" ry="6" fill="#000" opacity="0.18"/>
<path d="M14 84 H86 V92 H14 Z" fill="#e65c00"/>
<path d="M42 8 H58 L80 84 H20 Z" fill="#ff7a1a"/>
<path d="M36.5 30 H63.5 L67.5 44 H32.5 Z" fill="#ffffff"/>
<path d="M29 56 H71 L75 70 H25 Z" fill="#ffffff"/>
<path d="M42 8 H48 L32 84 H20 Z" fill="#ffffff" opacity="0.16"/>"""


def tooth(p):
    return """
<ellipse cx="50" cy="93" rx="26" ry="4" fill="#000" opacity="0.15"/>
<path d="M50 20 Q62 8 76 14 Q92 22 86 44 Q82 58 76 70 Q72 90 64 90 Q58 90 56 76 Q54 64 50 64
         Q46 64 44 76 Q42 90 36 90 Q28 90 24 70 Q18 58 14 44 Q8 22 24 14 Q38 8 50 20 Z"
      fill="#ffffff" stroke="#cfe6f5" stroke-width="2"/>
<path d="M26 22 Q20 30 22 42" stroke="#e6f3fb" stroke-width="5" fill="none"
      stroke-linecap="round"/>
<g fill="%s"><path d="M78 6 L80 12 L86 14 L80 16 L78 22 L76 16 L70 14 L76 12 Z"/>
  <path d="M16 62 L17.5 66 L21.5 67.5 L17.5 69 L16 73 L14.5 69 L10.5 67.5 L14.5 66 Z"/></g>
""" % p["accent"]


def house(p):
    return """
<ellipse cx="50" cy="90" rx="42" ry="5" fill="#000" opacity="0.18"/>
<rect x="64" y="18" width="9" height="20" fill="#7b3b2a"/>
<path d="M50 12 L90 46 H10 Z" fill="%s"/>
<rect x="18" y="44" width="64" height="44" fill="#fff6e8"/>
<rect x="42" y="60" width="16" height="28" rx="1" fill="#6b4226"/>
<circle cx="55" cy="75" r="1.4" fill="#f4c430"/>
<rect x="24" y="54" width="12" height="12" fill="#8fd3ff" stroke="#6b4226" stroke-width="2"/>
<rect x="64" y="54" width="12" height="12" fill="#8fd3ff" stroke="#6b4226" stroke-width="2"/>
<rect x="72" y="72" width="22" height="16" rx="2" fill="#ffffff" stroke="#333" stroke-width="1.5"/>
<rect x="81" y="66" width="3" height="8" fill="#333"/>
<text x="83" y="84" font-family="Poppins" font-weight="700" font-size="6.5" text-anchor="middle"
      fill="#c62828">SOLD</text>""" % p["accent"]


def television(p):
    return """
<ellipse cx="50" cy="92" rx="34" ry="4" fill="#000" opacity="0.2"/>
<path d="M38 76 L32 90 H68 L62 76 Z" fill="#2b2b33"/>
<rect x="6" y="14" width="88" height="62" rx="5" fill="#15151c"/>
<rect x="11" y="19" width="78" height="52" rx="2" fill="%s"/>
<path d="M44 34 L60 45 L44 56 Z" fill="#ffffff"/>
<g fill="#ffffff" opacity="0.5"><rect x="15" y="62" width="70" height="3" rx="1.5"/></g>
<rect x="15" y="62" width="22" height="3" rx="1.5" fill="#ffffff"/>
<g fill="#ffd166"><path d="M76 25 L77.5 29 L81.5 30 L77.5 31 L76 35 L74.5 31 L70.5 30 L74.5 29 Z"/>
</g>""" % p["accent"]


def phone(p):
    return """
<ellipse cx="50" cy="95" rx="22" ry="3" fill="#000" opacity="0.18"/>
<rect x="24" y="4" width="52" height="88" rx="4" fill="#25252b"/>
<rect x="28" y="12" width="44" height="66" rx="1" fill="%s"/>
<rect x="44" y="7" width="12" height="2" rx="1" fill="#55555f"/>
<rect x="44" y="82" width="12" height="6" rx="1" fill="#44444c"/>
<g fill="#ffffff" opacity="0.9">
  <rect x="32" y="18" width="10" height="10" rx="1"/><rect x="45" y="18" width="10" height="10" rx="1"/>
  <rect x="58" y="18" width="10" height="10" rx="1"/><rect x="32" y="32" width="10" height="10" rx="1"/>
  <rect x="45" y="32" width="10" height="10" rx="1"/><rect x="58" y="32" width="10" height="10" rx="1"/>
  <rect x="32" y="46" width="10" height="10" rx="1"/><rect x="45" y="46" width="10" height="10" rx="1"/>
</g>""" % p["accent"]


def hot_sauce(p):
    return """
<path d="M50 96 Q24 90 26 66 Q28 50 40 40 Q34 54 44 60 Q42 42 54 30 Q52 48 62 54 Q64 44 70 40
         Q80 56 76 72 Q72 92 50 96 Z" fill="#ff5a1f" opacity="0.85"/>
<ellipse cx="50" cy="95" rx="18" ry="3" fill="#000" opacity="0.2"/>
<rect x="44" y="6" width="12" height="10" rx="2" fill="#1a1a1a"/>
<path d="M45 16 H55 V28 Q66 36 66 50 V88 Q66 94 60 94 H40 Q34 94 34 88 V50 Q34 36 45 28 Z"
      fill="#b3120c"/>
<rect x="34" y="52" width="32" height="26" fill="#fff3d6"/>
<path d="M44 58 Q40 64 44 72 Q50 76 56 70 Q60 64 58 60 Q54 58 52 62 Q50 56 44 58 Z"
      fill="#d11d12"/>
<path d="M50 56 Q51 52 54 52" stroke="#2f8a2f" stroke-width="2" fill="none"/>
<rect x="38" y="30" width="3" height="54" rx="1.5" fill="#ffffff" opacity="0.25"/>"""


def minecart(p):
    return """
<rect x="4" y="86" width="92" height="4" fill="#6b6b6b"/>
<g fill="#8a5a2b"><rect x="8" y="90" width="8" height="4"/><rect x="30" y="90" width="8" height="4"/>
  <rect x="52" y="90" width="8" height="4"/><rect x="74" y="90" width="8" height="4"/></g>
<path d="M10 40 H90 L82 76 H18 Z" fill="#7d8791"/>
<path d="M10 40 H90 V46 H10 Z" fill="#9ea8b2"/>
<rect x="18" y="50" width="64" height="4" fill="#5d666e"/>
<rect x="20" y="62" width="60" height="4" fill="#5d666e"/>
<circle cx="30" cy="80" r="7" fill="#3a3f44"/><circle cx="30" cy="80" r="2.5" fill="#9ea8b2"/>
<circle cx="70" cy="80" r="7" fill="#3a3f44"/><circle cx="70" cy="80" r="2.5" fill="#9ea8b2"/>
<rect x="56" y="14" width="30" height="16" rx="2" fill="%s" transform="rotate(8 71 22)"/>
<text x="71" y="26" font-family="Poppins" font-weight="900" font-size="8" text-anchor="middle"
      fill="#1a1a1a" transform="rotate(8 71 22)">DEAL!</text>""" % p["accent"]


def pedestrian(p):
    return """
<rect x="6" y="6" width="88" height="88" rx="10" fill="#1b1b1b"/>
<rect x="10" y="10" width="80" height="80" rx="7" fill="none" stroke="#333" stroke-width="2"/>
<g fill="#f5f5f5">
  <circle cx="52" cy="22" r="7"/>
  <path d="M46 32 Q52 29 58 32 L62 52 L70 60 L66 64 L57 55 L55 46 L52 60 L60 84 L54 86 L46 64
           L42 86 L36 84 L42 58 L44 44 L38 52 L32 50 Z"/>
</g>
<g stroke="%s" stroke-width="3" stroke-linecap="round" fill="none">
  <path d="M18 30 L26 30"/><path d="M16 40 L26 40"/><path d="M18 50 L26 50"/>
</g>""" % p["accent"]


def ballot_star(p):
    return """
<ellipse cx="50" cy="92" rx="36" ry="4" fill="#000" opacity="0.18"/>
<rect x="18" y="44" width="64" height="46" rx="3" fill="#ffffff" stroke="#1d3557"
      stroke-width="3"/>
<rect x="30" y="44" width="40" height="5" fill="#1d3557"/>
<g transform="rotate(-12 50 28)">
  <rect x="34" y="10" width="32" height="40" fill="#fdfdfd" stroke="#999" stroke-width="1"/>
  <path d="M40 26 L45 31 L56 18" stroke="%s" stroke-width="4" fill="none"
        stroke-linecap="round" stroke-linejoin="round"/>
  <rect x="40" y="36" width="20" height="2" fill="#bbb"/><rect x="40" y="41" width="14" height="2"
        fill="#bbb"/>
</g>
<path d="M50 56 L53.5 64 L62 64.5 L55.5 70 L57.5 78.5 L50 74 L42.5 78.5 L44.5 70 L38 64.5
         L46.5 64 Z" fill="%s"/>""" % (p["accent"], p["accent"])


def airplane(p):
    return """
<g fill="#ffffff" opacity="0.9"><ellipse cx="22" cy="74" rx="16" ry="6"/>
  <ellipse cx="32" cy="70" rx="10" ry="7"/><ellipse cx="78" cy="30" rx="14" ry="5"/>
  <ellipse cx="86" cy="27" rx="8" ry="5"/></g>
<g transform="rotate(-18 50 50)">
  <path d="M10 50 Q10 44 20 44 H80 Q94 46 96 50 Q94 54 80 56 H20 Q10 56 10 50 Z"
        fill="#ffffff"/>
  <path d="M44 46 L30 22 H38 L60 46 Z" fill="%s"/>
  <path d="M44 54 L30 78 H38 L60 54 Z" fill="%s"/>
  <path d="M14 46 L8 34 H14 L24 46 Z" fill="%s"/>
  <g fill="#8fd3ff"><circle cx="30" cy="49" r="1.8"/><circle cx="38" cy="49" r="1.8"/>
    <circle cx="46" cy="49" r="1.8"/><circle cx="54" cy="49" r="1.8"/><circle cx="62" cy="49" r="1.8"/>
    <circle cx="70" cy="49" r="1.8"/></g>
  <path d="M84 47 Q90 48 92 50 H84 Z" fill="#8fd3ff"/>
</g>""" % (p["accent"], p["accent"], p["accent"])


def donut(p):
    return """
<ellipse cx="50" cy="86" rx="38" ry="6" fill="#000" opacity="0.18"/>
<ellipse cx="50" cy="56" rx="42" ry="30" fill="#d9913a"/>
<path d="M12 50 Q10 30 50 24 Q90 30 88 50 Q84 58 76 54 Q70 62 62 56 Q54 64 46 56 Q38 64 30 56
         Q22 60 12 50 Z" fill="%s"/>
<ellipse cx="50" cy="46" rx="12" ry="7" fill="#d9913a"/>
<ellipse cx="50" cy="47" rx="9" ry="5" fill="#8a4f1c"/>
<g stroke-width="3" stroke-linecap="round">
  <path d="M26 38 L30 36" stroke="#ffffff"/><path d="M68 34 L72 37" stroke="#ffe066"/>
  <path d="M36 30 L39 33" stroke="#5ec8ff"/><path d="M60 29 L64 28" stroke="#ffffff"/>
  <path d="M74 45 L78 44" stroke="#5ec8ff"/><path d="M22 47 L25 50" stroke="#ffe066"/>
  <path d="M44 34 L48 33" stroke="#7bdc6a"/><path d="M56 38 L58 41" stroke="#7bdc6a"/>
</g>""" % p["accent"]


def ice_cream(p):
    return """
<path d="M32 50 L50 96 L68 50 Z" fill="#e0a45a"/>
<g stroke="#b97a35" stroke-width="1.5"><path d="M36 58 L60 50"/><path d="M40 68 L64 56"/>
  <path d="M44 78 L66 60"/><path d="M40 50 L60 70"/><path d="M52 50 L66 60"/>
  <path d="M34 54 L56 82"/></g>
<circle cx="50" cy="40" r="17" fill="#ffd9e6"/>
<circle cx="38" cy="50" r="10" fill="#ffd9e6"/><circle cx="62" cy="50" r="10" fill="#ffd9e6"/>
<circle cx="50" cy="22" r="13" fill="%s"/>
<path d="M40 30 Q44 36 42 42" stroke="%s" stroke-width="5" fill="none" stroke-linecap="round"/>
<circle cx="50" cy="7" r="5" fill="#e53935"/>
<path d="M50 3 Q54 -2 58 0" stroke="#2f7a2f" stroke-width="1.5" fill="none"/>""" % (
        p["accent"], p["accent"])


def car(p):
    return """
<ellipse cx="50" cy="84" rx="44" ry="5" fill="#000" opacity="0.2"/>
<path d="M6 70 Q6 56 18 54 L30 38 Q34 34 42 34 H62 Q70 34 74 40 L84 54 Q94 56 94 66 V74 H6 Z"
      fill="%s"/>
<path d="M33 52 L40 40 H52 V52 Z" fill="#bfe6ff"/>
<path d="M56 40 H62 Q66 40 68 44 L73 52 H56 Z" fill="#bfe6ff"/>
<rect x="84" y="60" width="8" height="5" rx="1" fill="#fff6b0"/>
<circle cx="26" cy="74" r="10" fill="#22252a"/><circle cx="26" cy="74" r="4" fill="#b9c0c8"/>
<circle cx="74" cy="74" r="10" fill="#22252a"/><circle cx="74" cy="74" r="4" fill="#b9c0c8"/>
<path d="M86 22 Q92 30 86 34 Q80 30 86 22 Z" fill="#1a1a1a"/>
<path d="M78 8 Q82 14 78 17 Q74 14 78 8 Z" fill="#1a1a1a"/>""" % p["accent"]


def dumbbell(p):
    return """
<ellipse cx="50" cy="84" rx="42" ry="5" fill="#000" opacity="0.2"/>
<g transform="rotate(-20 50 50)">
  <rect x="24" y="46" width="52" height="8" rx="2" fill="#b9c0c8"/>
  <rect x="10" y="30" width="12" height="40" rx="3" fill="#2b2b33"/>
  <rect x="20" y="34" width="8" height="32" rx="2" fill="#3c3c46"/>
  <rect x="78" y="30" width="12" height="40" rx="3" fill="#2b2b33"/>
  <rect x="72" y="34" width="8" height="32" rx="2" fill="#3c3c46"/>
  <rect x="12" y="32" width="3" height="36" rx="1" fill="#ffffff" opacity="0.2"/>
</g>
<path d="M70 6 L66 18 L74 18 L68 32 L84 14 L76 14 L80 6 Z" fill="%s"/>""" % p["accent"]


def wifi(p):
    return """
<g fill="none" stroke="%s" stroke-linecap="round">
  <path d="M14 42 Q50 10 86 42" stroke-width="9"/>
  <path d="M26 56 Q50 34 74 56" stroke-width="9"/>
  <path d="M38 70 Q50 60 62 70" stroke-width="9"/>
</g>
<circle cx="50" cy="82" r="7" fill="%s"/>
<g fill="#ffffff" opacity="0.8"><rect x="72" y="72" width="6" height="6"/>
  <rect x="80" y="64" width="6" height="6"/><rect x="88" y="56" width="6" height="6"/>
  <rect x="80" y="80" width="6" height="6" opacity="0.5"/></g>""" % (p["accent"], p["accent"])


def dog(p):
    return """
<ellipse cx="50" cy="92" rx="30" ry="4" fill="#000" opacity="0.18"/>
<path d="M30 92 Q28 64 50 60 Q72 64 70 92 Z" fill="#e8b27a"/>
<circle cx="50" cy="40" r="24" fill="#f0c592"/>
<path d="M26 26 Q14 30 16 54 Q24 58 30 46 Z" fill="#9c6232"/>
<path d="M74 26 Q86 30 84 54 Q76 58 70 46 Z" fill="#9c6232"/>
<ellipse cx="50" cy="52" rx="13" ry="10" fill="#fff3e2"/>
<ellipse cx="50" cy="47" rx="5" ry="3.5" fill="#2a1a10"/>
<path d="M50 50 V55 Q46 59 42 56 M50 55 Q54 59 58 56" stroke="#2a1a10" stroke-width="1.8"
      fill="none" stroke-linecap="round"/>
<circle cx="40" cy="36" r="3.2" fill="#2a1a10"/><circle cx="60" cy="36" r="3.2" fill="#2a1a10"/>
<circle cx="41" cy="35" r="1" fill="#fff"/><circle cx="61" cy="35" r="1" fill="#fff"/>
<path d="M34 72 Q50 80 66 72" stroke="%s" stroke-width="5" fill="none"/>
<circle cx="50" cy="80" r="4" fill="#f4c430"/>
<g fill="%s"><path d="M80 10 L82 16 L88 18 L82 20 L80 26 L78 20 L72 18 L78 16 Z"/></g>""" % (
        p["accent"], p["accent"])


def solar(p):
    return """
<circle cx="72" cy="26" r="14" fill="#ffc400"/>
<g stroke="#ffc400" stroke-width="3" stroke-linecap="round">
  <path d="M72 4 V8"/><path d="M72 44 V48"/><path d="M50 26 H54"/><path d="M90 26 H94"/>
  <path d="M56 10 L59 13"/><path d="M85 39 L88 42"/><path d="M88 10 L85 13"/><path d="M56 42 L59 39"/>
</g>
<path d="M8 86 L22 52 H74 L62 86 Z" fill="#1f3f8f"/>
<g stroke="#9ec3ff" stroke-width="1.5">
  <path d="M26.7 52 L13.3 86"/><path d="M40 52 L28 86"/><path d="M53 52 L43 86"/>
  <path d="M65 52 L55 86"/><path d="M18.5 63 H70"/><path d="M14 74.5 H66"/>
</g>
<path d="M8 86 L22 52 H74 L62 86 Z" fill="none" stroke="#c9d6e8" stroke-width="2.5"/>
<rect x="34" y="86" width="4" height="8" fill="#8a96a8"/>"""


def taco(p):
    return """
<ellipse cx="50" cy="86" rx="40" ry="5" fill="#000" opacity="0.18"/>
<path d="M10 76 Q10 30 50 30 Q90 30 90 76 Z" fill="#f2c14e"/>
<g fill="#6bbf3a"><circle cx="20" cy="56" r="7"/><circle cx="30" cy="46" r="8"/>
  <circle cx="44" cy="40" r="8"/><circle cx="58" cy="40" r="8"/><circle cx="72" cy="46" r="8"/>
  <circle cx="80" cy="56" r="7"/></g>
<g fill="#e0402b"><circle cx="30" cy="42" r="4"/><circle cx="52" cy="36" r="4"/>
  <circle cx="70" cy="42" r="4"/></g>
<g fill="#ffe28a"><rect x="38" y="38" width="3" height="7" rx="1" transform="rotate(20 40 42)"/>
  <rect x="60" y="36" width="3" height="7" rx="1" transform="rotate(-25 61 40)"/></g>
<path d="M14 76 Q16 42 50 40 Q84 42 86 76 Z" fill="#f7cf5e"/>
<g fill="#e3a93a"><circle cx="30" cy="62" r="2"/><circle cx="50" cy="56" r="2"/>
  <circle cx="68" cy="64" r="2"/><circle cx="42" cy="70" r="2"/><circle cx="60" cy="72" r="2"/></g>"""


def fish(p):
    return """
<g fill="#bfe9ff" opacity="0.8"><circle cx="82" cy="20" r="4"/><circle cx="90" cy="32" r="2.5"/>
  <circle cx="76" cy="10" r="2"/></g>
<path d="M14 50 L2 34 V66 Z" fill="%s"/>
<path d="M12 50 Q30 22 62 24 Q86 28 92 50 Q86 72 62 76 Q30 78 12 50 Z" fill="%s"/>
<path d="M46 26 Q52 12 64 14 Q62 20 60 25 Z" fill="%s"/>
<path d="M40 50 Q52 44 62 52 Q52 58 40 50 Z" fill="#ffffff" opacity="0.4"/>
<circle cx="76" cy="44" r="6" fill="#ffffff"/><circle cx="77" cy="44" r="3" fill="#10203a"/>
<path d="M86 56 Q90 58 88 60" stroke="#10203a" stroke-width="1.5" fill="none"/>
<g stroke="#ffffff" stroke-width="1.5" opacity="0.35" fill="none">
  <path d="M32 36 Q36 50 32 64"/><path d="M44 32 Q48 50 44 68"/></g>""" % (
        p["accent"], p["accent"], p["accent"])


def coffee(p):
    return """
<ellipse cx="46" cy="90" rx="36" ry="5" fill="#000" opacity="0.18"/>
<ellipse cx="46" cy="86" rx="34" ry="5" fill="#e8e0d4"/>
<path d="M18 40 H74 V70 Q74 86 58 86 H34 Q18 86 18 70 Z" fill="#ffffff"/>
<path d="M74 48 Q90 48 88 62 Q86 74 72 72" stroke="#ffffff" stroke-width="6" fill="none"/>
<ellipse cx="46" cy="40" rx="28" ry="5" fill="#6b3e1f"/>
<path d="M22 54 H70" stroke="%s" stroke-width="6"/>
<g stroke="#ffffff" stroke-width="3.5" fill="none" stroke-linecap="round" opacity="0.85">
  <path d="M34 30 Q30 22 36 16 Q40 10 36 4"/><path d="M48 30 Q44 22 50 16 Q54 10 50 4"/>
  <path d="M60 30 Q56 22 62 16"/></g>
<path d="M82 14 Q74 18 76 26 Q80 34 90 30 Q84 30 82 24 Q80 18 82 14 Z" fill="#fff6c9"/>
""" % p["accent"]


def pizza(p):
    return """
<ellipse cx="50" cy="92" rx="14" ry="3" fill="#000" opacity="0.18"/>
<path d="M50 94 L8 18 Q50 0 92 18 Z" fill="#f2c14e"/>
<path d="M8 18 Q50 0 92 18 L88 26 Q50 10 12 26 Z" fill="#c9832e"/>
<path d="M50 88 L14 24 Q50 12 86 24 Z" fill="#ffd966"/>
<g fill="#c62828"><circle cx="40" cy="36" r="6"/><circle cx="62" cy="34" r="6"/>
  <circle cx="50" cy="54" r="6"/><circle cx="36" cy="58" r="4"/><circle cx="60" cy="68" r="4"/>
</g>
<g fill="#3f8f3a"><path d="M52 40 Q56 36 58 42 Z"/><path d="M42 48 Q38 44 36 50 Z"/>
  <path d="M54 62 Q58 60 56 66 Z"/></g>
<path d="M66 70 Q68 80 66 84" stroke="#ffd966" stroke-width="4" fill="none"
      stroke-linecap="round"/>"""


def washer(p):
    return """
<ellipse cx="50" cy="94" rx="36" ry="3" fill="#000" opacity="0.18"/>
<rect x="14" y="6" width="72" height="86" rx="5" fill="#f4f7fa"/>
<rect x="14" y="6" width="72" height="16" rx="5" fill="#dfe6ee"/>
<circle cx="26" cy="14" r="3" fill="#8a96a8"/><rect x="58" y="11" width="20" height="6" rx="1"
      fill="#253045"/>
<circle cx="50" cy="56" r="27" fill="#c9d3de"/>
<circle cx="50" cy="56" r="21" fill="#7fc8f8"/>
<path d="M29 58 Q40 50 50 58 Q60 66 71 58 V62 Q71 77 50 77 Q29 77 29 62 Z" fill="#3f9ee0"/>
<path d="M40 60 Q44 54 50 58 Q46 66 40 60 Z" fill="%s"/>
<path d="M56 64 Q62 60 64 66 Q58 70 56 64 Z" fill="#ffffff"/>
<path d="M36 44 Q40 38 48 37" stroke="#ffffff" stroke-width="3" fill="none"
      stroke-linecap="round" opacity="0.8"/>""" % p["accent"]


def gems(p):
    gem = ('<g transform="translate(%d %d)"><path d="M0 8 L8 0 H24 L32 8 L16 28 Z" fill="%s"/>'
           '<path d="M0 8 H32 L16 28 Z" fill="%s"/><path d="M8 0 L12 8 L16 0 L20 8 L24 0" '
           'stroke="#ffffff" stroke-width="1" fill="none" opacity="0.6"/></g>')
    light, dark = "#4fe39a", "#17a35c"
    return ('<ellipse cx="50" cy="92" rx="40" ry="4" fill="#000" opacity="0.18"/>'
            + gem % (4, 62, light, dark) + gem % (36, 62, light, dark)
            + gem % (68, 62, light, dark) + gem % (20, 38, light, dark)
            + gem % (52, 38, light, dark) + gem % (36, 14, light, dark)
            + '<path d="M80 12 L82 18 L88 20 L82 22 L80 28 L78 22 L72 20 L78 18 Z" fill="%s"/>'
            % p["accent"])


def moving_box(p):
    return """
<ellipse cx="50" cy="90" rx="40" ry="5" fill="#000" opacity="0.18"/>
<path d="M14 40 L50 28 L86 40 V80 L50 92 L14 80 Z" fill="#c98e4f"/>
<path d="M50 52 L86 40 V80 L50 92 Z" fill="#b07a40"/>
<path d="M14 40 L50 52 V92 L14 80 Z" fill="#d9a262"/>
<path d="M14 40 L50 52 L86 40 L50 28 Z" fill="#e3b577"/>
<path d="M32 34 L68 46" stroke="#f1d3a0" stroke-width="5"/>
<path d="M20 58 L42 66 L42 78 L20 70 Z" fill="#ffffff" opacity="0.85"/>
<path d="M60 16 L68 4 L76 16 L72 16 L72 26 H64 V16 Z" fill="%s"/>""" % p["accent"]


def bus(p):
    return """
<ellipse cx="50" cy="90" rx="44" ry="4" fill="#000" opacity="0.2"/>
<rect x="6" y="24" width="88" height="56" rx="8" fill="%s"/>
<rect x="6" y="24" width="88" height="10" rx="5" fill="#ffffff" opacity="0.25"/>
<g fill="#cfeaff"><rect x="12" y="38" width="16" height="18" rx="2"/>
  <rect x="32" y="38" width="16" height="18" rx="2"/><rect x="52" y="38" width="16" height="18" rx="2"/>
  <rect x="72" y="38" width="16" height="28" rx="2"/></g>
<rect x="6" y="62" width="62" height="5" fill="#ffffff" opacity="0.6"/>
<rect x="72" y="28" width="16" height="6" rx="1" fill="#1a1a1a"/>
<text x="80" y="33" font-family="Poppins" font-weight="700" font-size="4.8" text-anchor="middle"
      fill="#ffb300">42</text>
<circle cx="24" cy="80" r="8" fill="#22252a"/><circle cx="24" cy="80" r="3" fill="#b9c0c8"/>
<circle cx="76" cy="80" r="8" fill="#22252a"/><circle cx="76" cy="80" r="3" fill="#b9c0c8"/>
""" % p["accent"]


def pigeon(p):
    return """
<ellipse cx="50" cy="92" rx="30" ry="4" fill="#000" opacity="0.18"/>
<path d="M40 88 L38 94 M46 88 L46 94 M56 88 L56 94 M62 88 L64 94" stroke="#e57a6a"
      stroke-width="2.5" stroke-linecap="round"/>
<path d="M18 64 Q20 40 46 38 Q52 20 64 18 Q78 18 80 32 L90 36 L80 40 Q80 52 74 62 Q70 88 44 88
         Q26 88 18 64 Z" fill="#8e98a6"/>
<path d="M64 36 Q74 44 70 58 Q66 48 56 44 Z" fill="#5fb89a"/>
<path d="M60 40 Q70 48 66 58 Q62 50 54 46 Z" fill="#8a5fb8" opacity="0.7"/>
<path d="M20 62 Q34 50 56 60 Q48 76 30 76 Q22 72 20 62 Z" fill="#6f7a88"/>
<circle cx="70" cy="28" r="3" fill="#ff8c1a"/><circle cx="70" cy="28" r="1.4" fill="#111"/>
<rect x="30" y="4" width="36" height="12" rx="2" fill="%s" transform="rotate(-6 48 10)"/>
<text x="48" y="13" font-family="Poppins" font-weight="900" font-size="6.5" text-anchor="middle"
      fill="#111" transform="rotate(-6 48 10)">UNION</text>""" % p["accent"]


def megaphone(p):
    return """
<path d="M18 40 H34 L70 18 V82 L34 60 H18 Z" fill="%s"/>
<path d="M18 40 H34 V60 H18 Q12 60 12 50 Q12 40 18 40 Z" fill="#ffffff" opacity="0.35"/>
<path d="M70 18 Q78 50 70 82" stroke="#ffffff" stroke-width="4" fill="none" opacity="0.6"/>
<path d="M26 60 L32 84 H42 L38 62 Z" fill="%s"/>
<g stroke="%s" stroke-width="4" stroke-linecap="round" fill="none">
  <path d="M80 36 Q86 50 80 64"/><path d="M88 28 Q98 50 88 72"/></g>""" % (
        p["accent"], p["accent"], p["accent"])


# --------------------------------------------------------------------------------------------
# Catalogue
# --------------------------------------------------------------------------------------------

CATALOGUE = [
    dict(id="cube_burger", brand="Cube Burger", brand_font="bungee",
         headline="Now with 100% more *corners*",
         headline_short="Now with more *corners*",
         head_font="anton", sub="The only burger that stacks.",
         cta="Drive-thru open late", fine="Corners may be sharp. Eat responsibly.",
         art="burger", art_side="right", deco="rays", category="food",
         palette=dict(bg="#d7261e", bg2="#b01812", fg="#fff8e7", accent="#ffd23f",
                      brand="#ffd23f", ink="#fff8e7", band="#ffd23f", band_ink="#b01812",
                      deco="#ffd23f")),
    dict(id="boomguard_insurance", brand="BoomGuard Insurance", brand_font="archivo",
         headline="Because things *explode.*", head_font="archivo",
         sub="Creeper coverage from 99 cents a day.",
         cta="Call 555-0142", fine="Does not cover TNT you placed yourself.",
         art="explosion_shield", art_side="left", deco="circle", category="insurance",
         palette=dict(bg="#0f2a4a", bg2="#14365e", fg="#ffffff", accent="#2ea6ff",
                      brand="#ffffff", ink="#bcd6f0", band="#2ea6ff", band_ink="#0f2a4a",
                      deco="#1b4675")),
    dict(id="oof_ouch_law", brand="Oof & Ouch, Attorneys", brand_font="abril",
         headline="Hit by a *minecart?*", head_font="anton",
         sub="You may be entitled to emeralds.",
         cta="1-800-555-0137", fine="Past results do not guarantee emeralds.",
         art="gavel", art_side="right", deco="stripes", category="legal",
         palette=dict(bg="#101010", fg="#ffffff", accent="#ffcc00", brand="#ffcc00",
                      ink="#e8e8e8", band="#ffcc00", band_ink="#101010", deco="#ffcc00")),
    dict(id="gridlock_energy", brand="GRIDLOCK", brand_font="bangers", brand_upper=True,
         headline="For when you're going *nowhere fast*",
         headline_short="Going *nowhere fast?*", head_font="bangers",
         sub="Energy drink. 0% traffic relief.",
         cta="Now in Rush Hour Orange", fine="Do not drink while merging.",
         art="energy_can", art_side="left", deco="diag", category="drink",
         palette=dict(bg="#16161d", bg2="#232332", fg="#ffffff", accent="#ff7a00",
                      brand="#ff7a00", ink="#d0d0dc", band="#ff7a00", band_ink="#16161d",
                      deco="#ff7a00")),
    dict(id="green_wave_signals", brand="Green Wave Partners", brand_font="righteous",
         headline="We'll make every light *green.*|Eventually.", head_font="poppins-black",
         headline_short="Every light *green.* Eventually.",
         sub="Traffic signal timing consultants since last Tuesday.",
         cta="555-0163", fine="Results may vary by time of day, day of week, and mood.",
         art="traffic_light", art_side="right", deco="grid", category="services",
         palette=dict(bg="#0b3d2e", bg2="#0e5a42", fg="#ffffff", accent="#5dff9a",
                      brand="#5dff9a", ink="#cfeee0", band="#5dff9a", band_ink="#0b3d2e",
                      deco="#5dff9a")),
    dict(id="obsidian_mattress", brand="Uncle Obsidian's Mattresses", brand_font="lobster",
         headline="So firm you'll need a *diamond pickaxe*", head_font="archivo",
         headline_short="Need a *diamond pickaxe?*",
         sub="Everything must go. We just can't lift it.",
         cta="Route 9, next to the quarry", fine="Mattress weight: yes.",
         art="mattress", art_side="left", deco="circle", category="home",
         palette=dict(bg="#2b1a47", bg2="#3a2461", fg="#ffffff", accent="#c9a6ff",
                      brand="#e2ccff", ink="#d8cdea", band="#c9a6ff", band_ink="#2b1a47",
                      deco="#3d2a66")),
    dict(id="cone_zone", brand="Cone Zone Supply", brand_font="bungee",
         headline="Orange you glad you *called?*", head_font="anton",
         sub="Traffic cones, drums and barricades. Wholesale.",
         cta="555-0118", fine="Cones not responsible for being ignored.",
         art="cone", art_side="right", deco="stripes", category="services",
         palette=dict(bg="#ffffff", bg2="#f2f2f2", fg="#1a1a1a", accent="#ff6a00",
                      brand="#ff6a00", ink="#333333", band="#1a1a1a", band_ink="#ff8a1f",
                      deco="#ff6a00", fine="#555555")),
    dict(id="bright_smile_dental", brand="Bright Smile Dental", brand_font="righteous",
         headline="We fill *cavities.*|Not caves.", head_font="poppins-black",
         headline_short="We fill *cavities.* Not caves.",
         sub="New patients welcome. Sweet tooth optional.",
         cta="555-0176", fine="Please do not bring your pickaxe to your appointment.",
         art="tooth", art_side="right", deco="dots", category="health",
         palette=dict(bg="#e6f6ff", bg2="#cdeeff", fg="#0a3d62", accent="#00a8e8",
                      brand="#00a8e8", ink="#265a7a", band="#0a3d62", band_ink="#ffffff",
                      deco="#00a8e8", fine="#3d6f8f")),
    dict(id="redstone_realty", brand="Redstone Realty", brand_font="abril",
         headline="Location, location, *lava-free* location", head_font="abril",
         headline_short="*Lava-free* locations",
         sub="Homes with no mob spawners in the basement.",
         cta="Ask for Brenda  555-0129", fine="Lava-free status not guaranteed after purchase.",
         art="house", art_side="left", deco="circle", category="home",
         palette=dict(bg="#fbf3e4", bg2="#f3e6cc", fg="#3a1d12", accent="#c62828",
                      brand="#c62828", ink="#5a3a2a", band="#c62828", band_ink="#fff6e8",
                      deco="#f0dcc0", fine="#6b4a3a")),
    dict(id="couch_potato_plus", brand="Couch Potato+", brand_font="righteous",
         headline="40 new shows you'll *never finish*", head_font="poppins-black",
         headline_short="Shows you'll *never finish*",
         sub="Now streaming. Still buffering.", cta="Free trial for 3 minutes",
         fine="Are you still watching? We are.",
         art="television", art_side="right", deco="rays", category="entertainment",
         palette=dict(bg="#24103f", bg2="#3b1466", fg="#ffffff", accent="#ff4fa3",
                      brand="#ff4fa3", ink="#e0cff5", band="#ff4fa3", band_ink="#24103f",
                      deco="#ff4fa3")),
    dict(id="cubeline_brick", brand="Cubeline Brick 12", brand_font="poppins-bold",
         headline="Finally, a phone shaped like *everything else.*", head_font="poppins-bold",
         headline_short="Shaped like *everything else.*",
         sub="Now with more corners than ever.", cta="Available in Stone and Deepslate",
         fine="Case not required. Case is the phone.",
         art="phone", art_side="right", deco=None, category="tech",
         palette=dict(bg="#f5f5f7", bg2="#e8e8ed", fg="#111111", accent="#2f6bff",
                      brand="#111111", ink="#555555", band="#111111", band_ink="#ffffff",
                      deco="#2f6bff", fine="#777777")),
    dict(id="nether_nights_sauce", brand="Nether Nights Hot Sauce", brand_font="bangers",
         headline="Scoville rating: *yes.*", head_font="bangers",
         sub="Bottled at the source.", cta="Ask your server. Or don't.",
         fine="Keep away from children, pets and ghasts.",
         art="hot_sauce", art_side="left", deco="rays", category="food",
         palette=dict(bg="#1a0603", bg2="#3d0c05", fg="#ffffff", accent="#ff5a1f",
                      brand="#ff8a3d", ink="#f2cdbf", band="#ff5a1f", band_ink="#1a0603",
                      deco="#ff5a1f")),
    dict(id="honest_hals_minecarts", brand="Honest Hal's Used Minecarts", brand_font="marker",
         headline="Barely *derailed!*", head_font="marker",
         sub="Zero down. Zero brakes.", cta="Hal's Lot, end of the line",
         fine="As is. Tracks sold separately.",
         art="minecart", art_side="right", deco="rays", category="auto",
         palette=dict(bg="#ffd23f", bg2="#ffbf00", fg="#1a1a1a", accent="#d7261e",
                      brand="#d7261e", ink="#2a2a2a", band="#1a1a1a", band_ink="#ffd23f",
                      deco="#ffffff", fine="#3a3a3a")),
    dict(id="psa_look_both_ways", brand="Department of Streets", brand_font="poppins-bold",
         headline="Look both ways. *Twice.*", head_font="poppins-black",
         sub="The cars can't read the signs either.", cta="A public service message",
         fine="This message was approved by the crosswalk.",
         art="pedestrian", art_side="left", deco=None, category="civic",
         palette=dict(bg="#004d40", fg="#ffffff", accent="#ffd54f", brand="#ffffff",
                      ink="#cce5e1", band="#ffd54f", band_ink="#004d40", deco="#00695c")),
    dict(id="vote_blockley", brand="Pat Blockley for Mayor", brand_font="archivo",
         headline="*Fewer* potholes.|Probably.", head_font="archivo",
         headline_short="*Fewer* potholes. Probably.",
         sub="Experience you can count on. Most days.", cta="Vote Tuesday",
         fine="Paid for by the Committee to Elect Someone.",
         art="ballot_star", art_side="left", deco="stripes", category="civic",
         palette=dict(bg="#1d3557", bg2="#243f66", fg="#ffffff", accent="#e63946",
                      brand="#ffffff", ink="#d7e3f4", band="#e63946", band_ink="#ffffff",
                      deco="#ffffff")),
    dict(id="cloud_nine_air", brand="Cloud Nine Airlines", brand_font="pacifico",
         headline="Legroom sold *separately.*", head_font="poppins-black",
         sub="Fly anywhere. Sit nowhere.", cta="Fares from 49 emeralds",
         fine="Carry-on must fit under the seat in front of the seat in front of you.",
         art="airplane", art_side="right", deco=None, category="travel",
         palette=dict(bg="#4fb3ff", bg2="#1e7fd9", fg="#ffffff", accent="#ffe066",
                      brand="#ffffff", ink="#eaf6ff", band="#ffffff", band_ink="#1e7fd9",
                      deco="#ffffff")),
    dict(id="holey_moly_donuts", brand="Holey Moly Donuts", brand_font="lobster",
         headline="A dozen for the price of *twelve.*", head_font="bangers",
         headline_short="A dozen for *twelve.*",
         sub="Math is hard. Donuts are easy.", cta="Open at 5 a.m.",
         fine="Holes sold separately.",
         art="donut", art_side="left", deco="dots", category="food",
         palette=dict(bg="#ffe3ef", bg2="#ffd0e3", fg="#5a1636", accent="#ff4f8b",
                      brand="#e0336f", ink="#7a2a4c", band="#ff4f8b", band_ink="#ffffff",
                      deco="#ff4f8b", fine="#7a2a4c")),
    dict(id="tundra_creamery", brand="Tundra Creamery", brand_font="righteous",
         headline="Brain freeze *guaranteed.*", head_font="anton",
         sub="Made with real snow. Probably.", cta="Scoops from 2 emeralds",
         fine="Do not lick the sign.",
         art="ice_cream", art_side="right", deco="circle", category="food",
         palette=dict(bg="#b8f2e6", bg2="#9ee8d8", fg="#0e4a44", accent="#ff6f91",
                      brand="#0e4a44", ink="#1f5f58", band="#0e4a44", band_ink="#b8f2e6",
                      deco="#ffffff", fine="#1f5f58")),
    dict(id="quick_drip_oil", brand="Quick Drip Oil Change", brand_font="bungee",
         headline="In and out in 15 minutes. *Your car,* not you.", head_font="anton",
         headline_short="In and out in *15 minutes.*",
         sub="Oil changed. Wiper blades judged.", cta="No appointment needed",
         fine="15 minutes measured in dog minutes.",
         art="car", art_side="left", deco="stripes", category="auto",
         palette=dict(bg="#ffcc00", bg2="#ffb800", fg="#111111", accent="#d7261e",
                      brand="#111111", ink="#222222", band="#111111", band_ink="#ffcc00",
                      deco="#111111", fine="#333333")),
    dict(id="ironside_fitness", brand="Ironside Fitness", brand_font="bebas",
         headline="Punch trees *professionally.*", head_font="bebas",
         sub="Open 24 hours. Mostly by zombies after dark.", cta="First month free",
         fine="Gains not transferable between worlds.",
         art="dumbbell", art_side="right", deco="diag", category="health",
         palette=dict(bg="#121212", bg2="#1f1f1f", fg="#ffffff", accent="#ff2e2e",
                      brand="#ff2e2e", ink="#cccccc", band="#ff2e2e", band_ink="#121212",
                      deco="#ff2e2e")),
    dict(id="bytesize_internet", brand="ByteSize Internet", brand_font="righteous",
         headline="Up to 5 gig. *Probably.*", head_font="poppins-black",
         sub="Speeds you can almost believe.", cta="Switch today  555-0184",
         fine="Up to means up to. It does not mean at.",
         art="wifi", art_side="left", deco="grid", category="tech",
         palette=dict(bg="#062b35", bg2="#0a4250", fg="#ffffff", accent="#21e6c1",
                      brand="#21e6c1", ink="#bfe9e3", band="#21e6c1", band_ink="#062b35",
                      deco="#21e6c1")),
    dict(id="paws_and_claws", brand="Paws & Claws Grooming", brand_font="pacifico",
         headline="We groom wolves too. *Carefully.*", head_font="poppins-black",
         headline_short="We groom wolves. *Carefully.*",
         sub="Baths, trims and bone-free treats.", cta="Walk-ins welcome",
         fine="Collars in 16 colours.",
         art="dog", art_side="right", deco="circle", category="services",
         palette=dict(bg="#fff1dc", bg2="#ffe4bd", fg="#4a2a10", accent="#ff7b39",
                      brand="#e0612a", ink="#6b4526", band="#ff7b39", band_ink="#ffffff",
                      deco="#ffd9a8", fine="#6b4526")),
    dict(id="sunny_side_solar", brand="Sunny Side Solar", brand_font="archivo",
         headline="The sun's out. Your *bill* should be too.", head_font="archivo",
         headline_short="The sun's out. So is your *bill.*",
         sub="Rooftop solar with no daylight sensor required.", cta="Free quote  555-0150",
         fine="Does not work at night. Neither do we.",
         art="solar", art_side="left", deco="rays", category="home",
         palette=dict(bg="#0d2b6b", bg2="#163f94", fg="#ffffff", accent="#ffc400",
                      brand="#ffc400", ink="#d3def5", band="#ffc400", band_ink="#0d2b6b",
                      deco="#5b8cff")),
    dict(id="casa_cuadrada", brand="Casa Cuadrada", brand_font="lobster",
         headline="Taco Tuesday. *Every day.*", head_font="bangers",
         sub="Square plates. Round tacos. Balance.", cta="Two for three emeralds",
         fine="Every day is legally Tuesday inside the restaurant.",
         art="taco", art_side="right", deco="diag", category="food",
         palette=dict(bg="#00a6a6", bg2="#008b8b", fg="#ffffff", accent="#ffe066",
                      brand="#ffe066", ink="#e6ffff", band="#ff5d8f", band_ink="#ffffff",
                      deco="#ff5d8f")),
    dict(id="deep_blue_aquarium", brand="Deep Blue Aquarium", brand_font="abril",
         headline="Now with 30% fewer *squids.*", head_font="poppins-black",
         headline_short="30% fewer *squids.*",
         sub="The ink stains are part of the exhibit.", cta="Open daily 9 to 5",
         fine="Guardians not on display. Please stop asking.",
         art="fish", art_side="left", deco="dots", category="entertainment",
         palette=dict(bg="#03254c", bg2="#064f8a", fg="#ffffff", accent="#ffb347",
                      brand="#9fdcff", ink="#cfe8ff", band="#ffb347", band_ink="#03254c",
                      deco="#9fdcff")),
    dict(id="moonrise_coffee", brand="Moonrise Coffee", brand_font="pacifico",
         headline="For your commute. *All three hours of it.*", head_font="abril",
         headline_short="For your *three-hour* commute.",
         sub="Roasted daily. Like the drivers.", cta="Drive-thru on 5th",
         fine="Contains coffee. We checked.",
         art="coffee", art_side="right", deco="circle", category="drink",
         palette=dict(bg="#3b2416", bg2="#4d3020", fg="#fff4e6", accent="#f6b26b",
                      brand="#f6b26b", ink="#ead7c3", band="#f6b26b", band_ink="#3b2416",
                      deco="#5a3a26")),
    dict(id="mile_high_pizza", brand="Mile High Pizza", brand_font="lobster",
         headline="Delivered in 30 minutes or the *next 30.*", head_font="anton",
         headline_short="30 minutes or the *next 30.*",
         sub="Hot, fresh and roughly on time.", cta="Call 555-0105",
         fine="Minutes are estimates. So is the pizza.",
         art="pizza", art_side="left", deco="stripes", category="food",
         palette=dict(bg="#1f7a3a", bg2="#17652f", fg="#ffffff", accent="#ffd966",
                      brand="#ffffff", ink="#e0f2e5", band="#c62828", band_ink="#ffffff",
                      deco="#ffffff")),
    dict(id="spin_cycle_laundromat", brand="Spin Cycle Laundromat", brand_font="righteous",
         headline="We never lose a sock. *We lose two.*", head_font="poppins-black",
         headline_short="We lose *two* socks.",
         sub="Wash, dry, fold, ponder.", cta="Open 6 a.m. to midnight",
         fine="Lost socks are forwarded to the sock dimension.",
         art="washer", art_side="right", deco="dots", category="services",
         palette=dict(bg="#e3f2ff", bg2="#cbe6ff", fg="#12355b", accent="#ff6f59",
                      brand="#12355b", ink="#2d5580", band="#12355b", band_ink="#e3f2ff",
                      deco="#12355b", fine="#2d5580")),
    dict(id="stackwell_savings", brand="Stackwell Savings & Loan", brand_font="abril",
         headline="Your emeralds, *safely stacked.*", head_font="abril",
         sub="Up to 64 per slot, fully insured.", cta="Open an account today",
         fine="Member of the Villager Deposit Insurance Corporation. Hmm.",
         art="gems", art_side="left", deco="circle", category="finance",
         palette=dict(bg="#0f3b2e", bg2="#134d3c", fg="#ffffff", accent="#e8c15a",
                      brand="#e8c15a", ink="#cfe3d9", band="#e8c15a", band_ink="#0f3b2e",
                      deco="#1a5c48")),
    dict(id="rocket_movers", brand="Rocket Movers", brand_font="bungee",
         headline="We move you. *Emotionally* and physically.", head_font="archivo",
         headline_short="We move you. *Emotionally.*",
         sub="Local and long distance. Mostly local.", cta="555-0191",
         fine="Boxes labelled FRAGILE are handled with extra enthusiasm.",
         art="moving_box", art_side="right", deco="rays", category="services",
         palette=dict(bg="#ff6b1a", bg2="#e85a0c", fg="#ffffff", accent="#1d2b53",
                      brand="#1d2b53", ink="#fff1e6", band="#1d2b53", band_ink="#ffffff",
                      deco="#ffffff")),
    dict(id="citywide_transit", brand="Citywide Transit", brand_font="poppins-bold",
         headline="Ride the bus. *Somebody* has to.", head_font="poppins-black",
         sub="Every 12 minutes. Give or take a lot.", cta="Route 42 now serving downtown",
         fine="Please stand clear of the doors, and of Gary.",
         art="bus", art_side="left", deco=None, category="civic",
         palette=dict(bg="#f4f4f4", bg2="#e6e6e6", fg="#111111", accent="#0072ce",
                      brand="#0072ce", ink="#333333", band="#0072ce", band_ink="#ffffff",
                      deco="#0072ce", fine="#555555")),
    dict(id="psa_pigeons", brand="Department of Parks", brand_font="poppins-bold",
         headline="Don't feed the pigeons. *They're organizing.*", head_font="archivo",
         headline_short="The pigeons are *organizing.*",
         sub="They have a newsletter now.", cta="A public service message",
         fine="The pigeons have asked us to remove this sign.",
         art="pigeon", art_side="right", deco="stripes", category="civic",
         palette=dict(bg="#2e5e2e", bg2="#244d24", fg="#ffffff", accent="#ffd54f",
                      brand="#ffffff", ink="#dbeadb", band="#ffd54f", band_ink="#2e5e2e",
                      deco="#ffffff")),
    # The house ad: what a board shows for an ad id it does not know (a removed server image,
    # a renamed ad). Category "house" keeps it out of "all" and "random" playlists.
    dict(id="your_ad_here", brand="Your Ad Here", brand_font="archivo",
         headline="This space *available.*", head_font="anton",
         sub="Reach thousands of people stuck at this light.", cta="Call 555-0199",
         fine="Ask about our bulk rates for entire city blocks.",
         art="megaphone", art_side="left", deco="stripes", category="house",
         palette=dict(bg="#222222", bg2="#2e2e2e", fg="#ffffff", accent="#ffd400",
                      brand="#ffd400", ink="#dddddd", band="#ffd400", band_ink="#222222",
                      deco="#ffd400")),
]
