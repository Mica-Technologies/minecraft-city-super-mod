#!/usr/bin/env python3
"""Re-render all recreated sign textures from specs (single source of truth).

Text-only specs come from the triage (recreate + text_only), plus a few hand-tuned
ones (road_ends, positively, doorsunlocked). Composite specs are inline. Border always
matches the font color, thin and inset (see render_sign.render / render_composite).
Run from repo root: python dev-env-utils/scripts/recreate_signs.py
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from render_sign import render, render_composite, render_wide, render_diamond_symbol, render_oneway, render_badge

T = 'src/main/resources/assets/csm/textures/blocks/trafficsigns/'
C = {'white':(245,245,245,255),'black':(20,20,20,255),'orange':(255,98,0,255),
     'yellow':(252,209,22,255),'green':(0,108,56,255),'red':(196,30,38,255),
     'blue':(0,67,123,255),'brown':(74,52,38,255),'pink':(232,62,140,255)}
def col(w, d):
    if isinstance(w, tuple): return w if len(w) == 4 else w + (255,)
    return C.get((w or '').strip().lower(), d)

cls = {os.path.splitext(r['file'])[0]: r['shape']
       for r in json.load(open('dev-env-utils/scripts/sign_classification.json'))['results']}
cls['positively_no_smoking_sign'] = 'plaque'; cls['base_station_radio_sign'] = 'plaque'
RSHAPE = {'diamond':'diamond','square':'square','landscape_rect':'rect','portrait_rect':'rect',
          'plaque':'plaque','strip':'plaque','octagon':'square'}
FLUOR = {'campground_crossing_sign':(186,255,41,255), 'fawe_incident_sign':(255,0,255,255)}  # exact magenta from original

# wide signs: design at true aspect then squish to square (paired with the medium_wide model)
WIDE = {
    'end_land_work_sign': (['END','LAND WORK'], (255,98,0,255), (20,20,20,255)),
}

# orange-diamond signs with a MUTCD arrow symbol: (lines, arrow_dir, bg, fg)
ARROW = {
    'freeway_detour_sign': (['FWY','DETOUR'], 'up', (255,98,0,255), (20,20,20,255)),
    'castraightdetoursign': (['DETOUR'], 'up', (255,98,0,255), (20,20,20,255)),
}

# hand overrides for text-only line breaks / specials
OVERRIDE = {
    'doorsunlockedbiz': ['THIS DOOR TO','REMAIN UNLOCKED','DURING','BUSINESS HOURS'],
}
EXTRA_TEXT = {  # signs graded 'good' after earlier recreation; keep them in sync here
    'road_ends_in_water_sign': ('diamond', ['ROAD','ENDS IN','WATER'], 'orange', 'black'),
    'positively_no_smoking_sign': ('rect', ['POSITIVELY NO','SMOKING OR OPEN','LIGHTS PERMITTED'], 'white', 'black'),
    'rgr_ahead_sign': ('diamond', ['RGR','AHEAD'], 'orange', 'black'),
    'rgr_baby_sign': ('diamond', ['RGR','BABY','AHEAD'], 'orange', 'black'),
    'rgr_chicken_sign': ('diamond', ['RGR','CHICKEN','AHEAD'], 'orange', 'black'),
    'massdot_heavy_merge_sign': ('diamond', ['HEAVY','MERGE','AHEAD'], 'yellow', 'black'),
    'massdot_heavy_merge_sign_rw': ('diamond', ['HEAVY','MERGE','AHEAD'], 'orange', 'black'),
    'alwaysroadworksign': ('diamond', ['ALWAYS','ROAD WORK','AHEAD'], 'orange', 'black'),
    'snownotremovedsign': ('square', ['SNOW NOT','REMOVED','BEYOND HERE'], 'white', 'black'),
    'hwy_entrance': ('square', ['HIGHWAY','ENTRANCE'], (47,146,135), 'white'),
    'no_parking_eairs_sign': ('square', ['NO PARKING','IN THIS AREA','FOR 1 MILE','EMERGENCY','AIRSTRIP'], 'white', (247,0,0)),
    'respect_uia_nat_parks_sign': ('rect', ['PLEASE','RESPECT ALL UIA','NATIONAL PARKS','AND PROPERTY','FOR EVERYONE','UIA CODES APPLY'], 'white', (10,105,78)),
    'thickly_settled_sign': ('diamond', ['THICKLY','SETTLED'], 'yellow', 'black'),  # NEW (MassDOT)
    # tall regulatory sign -> render square so all 5 lines fit; blockstate uses the tall-rect model
    'shoulder_travel_on_green_arrow_sign': ('square', ['TRAVEL ON','SHOULDER','ON GREEN','ARROW','ONLY'], 'white', 'black'),
    'specialeventsign': ('diamond', ['SPECIAL','EVENT','AHEAD'], 'orange', 'black'),
    'no_guardrails_sign': ('diamond', ['NO','GUARDRAILS'], 'yellow', 'black'),
    'no_guardrails_sign_rr': ('diamond', ['NO','GUARDRAILS'], 'orange', 'black'),
    'watchdownhillspeedsign': ('diamond', ['WATCH','DOWNHILL','SPEED'], 'yellow', 'black'),
}

def text_only():
    tri = json.load(open('dev-env-utils/scripts/sign_triage.json'))['results']
    done = 0
    for r in tri:
        if not (r['quality'] == 'recreate' and r['type'] == 'text_only' and r['lines']): continue
        tex = r['tex']
        if tex in WIDE: continue   # rendered separately as a wide-then-squished sign
        shp = r['shapeSuggest'] if (not r['shapeOk'] and r.get('shapeSuggest') != 'keep') else cls.get(tex, 'square')
        rshape = RSHAPE.get(shp, 'square')
        bg = FLUOR.get(tex) or col(r['bg'], (245,245,245,255))
        fg = col(r['fg'], (20,20,20,255))
        lines = OVERRIDE.get(tex, [l.strip() for l in r['lines']])
        render(rshape, lines, bg=bg, fg=fg).save(T + tex + '.png')   # border defaults to fg
        done += 1
    for tex, (rshape, lines, bg, fg) in EXTRA_TEXT.items():
        render(rshape, lines, bg=col(bg,(245,245,245,255)), fg=col(fg,(20,20,20,255))).save(T + tex + '.png')
        done += 1
    return done

RED, WHITE, BLACK, YELLOW = C['red'], C['white'], C['black'], C['yellow']
COMPOSITES = {
 'danger_falling_material_sign': (['DANGER'], ['FALLING','MATERIAL'], RED, WHITE, WHITE, BLACK, 0.34),
 'danger_bad_water_sign':        (['DANGER'], ['DO NOT DRINK','THIS WATER'], RED, WHITE, WHITE, BLACK, 0.34),
 'deadly_force_sign':            (['WARNING'], ['BEYOND THIS POINT','DEADLY FORCE','IS AUTHORIZED'], WHITE, BLACK, WHITE, BLACK, 0.30),
 'fall_hazzard_sign':            (['WARNING'], ['FALL HAZARD AREA','DO NOT ENTER'], WHITE, BLACK, WHITE, BLACK, 0.32),
 'ear_protection_sign':          (['CAUTION'], ['EAR PROTECTION','REQUIRED BEYOND','THIS POINT'], YELLOW, BLACK, YELLOW, BLACK, 0.30),
 'switch_equipment_warning_sign':(['CAUTION'], ['REMOTE CONTROLLED','EQUIPMENT MAY OPERATE','AT ANY TIME','KEEP CLEAR OF','MOVING PARTS'], YELLOW, BLACK, YELLOW, BLACK, 0.26),
 'restricted_area_sign':         (['RESTRICTED AREA'], ['NO TRESPASSING','BEYOND THIS POINT','PHOTOGRAPHY','IS PROHIBITED'], BLACK, WHITE, WHITE, BLACK, 0.22),
 'base_station_radio_sign':      (['USE BASE STATION','RADIOS ONLY IN','CONTROL ROOM'], ['NO RADIOS ALLOWED','BEHIND CONTROL','CABINETS'], WHITE, BLACK, YELLOW, BLACK, 0.50),
}
def composites():
    for tex, (h, b, hb, hf, bb, bf, sp) in COMPOSITES.items():
        render_composite(h, b, hb, hf, bb, bf, split=sp).save(T + tex + '.png')   # border defaults to body_fg
    return len(COMPOSITES)

def wide():
    for tex, (lines, bg, fg) in WIDE.items():
        render_wide(lines, bg=bg, fg=fg).save(T + tex + '.png')
    return len(WIDE)

def arrows():
    for tex, (lines, ad, bg, fg) in ARROW.items():
        render_diamond_symbol(lines, bg=bg, fg=fg, arrow=ad).save(T + tex + '.png')
    return len(ARROW)

ONEWAY = {'oneway_tl_sign_left': 'left', 'oneway_tl_sign_right': 'right'}
def oneways():
    for tex, dirn in ONEWAY.items():
        render_oneway(dirn).save(T + tex + '.png')
    return len(ONEWAY)

# badge signs: (text, bg, star, fg, points)
BADGE = {'sheriffstation': ('SHERIFF', (9,1,139,255), (245,245,245,255), (9,1,139,255), 7)}
def badges():
    for tex, (txt, bg, st, fg, pts) in BADGE.items():
        render_badge(txt, bg=bg, star=st, fg=fg, points=pts).save(T + tex + '.png')
    return len(BADGE)

if __name__ == '__main__':
    n1 = text_only(); n2 = composites(); n3 = wide(); n4 = arrows(); n5 = oneways(); n6 = badges()
    print(f"re-rendered {n1} text + {n2} composite + {n3} wide + {n4} arrow + {n5} oneway + {n6} badge signs")
