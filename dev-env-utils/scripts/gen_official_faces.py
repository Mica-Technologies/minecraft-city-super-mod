#!/usr/bin/env python3
"""Replace existing road signs' faces with the official FHWA Standard Highway Signs drawings.

The catalogue below maps a registry name to the SHS page (or interim ZIP) its design is
drawn on. For each, the script reads the sign's own blockstate to find the plate model and
the texture it paints (slot ``1``; the file is often not named after the registry), measures
the plate's aspect off the model's elements, renders the drawing through
``shs_signs.official_face`` and writes the texture -- nothing else. A silhouette sign whose
blockstate names a ``_back`` texture on slot ``2`` gets that regenerated too. Registration,
lang and blockstates are never touched: this is a texture swap for signs that already exist,
which is what makes ``--check`` a byte comparison and a batch reversible with
``git checkout <commit> -- <texture>``.

    python dev-env-utils/scripts/gen_official_faces.py [--only a,b] [--sheet out.png] [--check]
    python dev-env-utils/scripts/gen_official_faces.py --verify-sheet <prefix>

``--sheet`` writes a before/after contact sheet at plate aspect (current texture left, new
face right) without touching the tree -- the batch review the user sees before anything is
written. ``--verify-sheet`` renders every ``?`` row of
``assets/docs/agent_progress/SHS_MATCH_TABLE.md`` as the current texture beside a thumbnail
of the cited book page, eight per sheet, so the token-matched guesses can be confirmed or
rejected by eye before they are catalogued.

Numbered signs (Speed Limit, CURVE xx MPH, ...) use ``replace=('50', '35')``: the book draws
one numeral, which is dropped and the mod's set in its place at the same cap height.
"""

import json
import os
import re
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import shs_signs as shs  # noqa: E402

MATCH_TABLE = os.path.join(layout.REPO_ROOT, 'assets', 'docs', 'agent_progress', 'SHS_MATCH_TABLE.md')


# ----------------------------------------------------------------------------- sources

def SHS(chapter, page, pick=0, mirror=False, palette=None, replace=None, inner=None,
        rotate_symbols=0, mirror_symbols=False):
    """A face from a 2004 SHS book page (0-based). ``pick`` for pages with more than one
    sign, ``mirror`` for the left-hand version of a symbol the book draws right-handed only
    (``mirror_symbols`` when it carries a legend: the arrow flips, the words do not),
    ``rotate_symbols`` to turn an arrow in place, ``replace=(old, new)`` to re-set the one
    numeral the book draws."""
    return lambda: (shs.book_sign(chapter, page, pick, inner=inner, replace=replace,
                                  rotate_symbols=rotate_symbols, mirror_symbols=mirror_symbols),
                    mirror, palette)


class SymbolFace(object):
    """A bare pictogram from the book plus the panel colour it goes on; composed by
    :func:`render` once the sign's plate aspect is known."""

    def __init__(self, symbol, colour):
        self.symbol, self.colour = symbol, colour


def SYM(chapter, page, picks, colour):
    """A recreational / services pictogram the book draws without a panel (``picks`` are the
    outline numbers, several for a symbol drawn in pieces), set in white on a rounded panel
    of ``colour`` ('brown', 'blue', 'green') with a white border."""
    return lambda: (SymbolFace(shs.symbol_face(chapter, page, picks), shs.MOD_COLOURS[colour]), False, None)


def SHSI(code, variant=None, palette=None):
    """A face from an interim SHS ZIP (a sign added or redrawn since the book)."""
    return lambda: (shs.interim_sign(code, variant), False, palette)


# ----------------------------------------------------------------------------- catalogue
# registry, source, SHS code (for the sheet caption)

R = 'Regulatory'
W = 'Warning'
G = 'Guide'
E = 'EM'
# the R7-8 page draws the wheelchair panel in a slate blue; the mod's is the D9-6 blue
SLATE_TO_BLUE = {(74, 87, 120): shs.MOD_COLOURS['blue']}
FYG_FACE = {(255, 245, 0): shs.MOD_COLOURS['fyg']}   # the book's yellow onto fluorescent yellow-green
CATALOGUE = [
    # --- Phase 1: Regulatory, the confident exact matches. Not here, and why: signahead is
    # the up-arrow plaque, not the AHEAD legend; onewaytlsignright is the arrow-shaped R6-1;
    # signphotoenforced carries a signal head the R10-19 legend does not; signhovlaneends
    # was drawn portrait; its plate is now the wide one, so it is back in.
    ('signpoststopsign', SHS(R, 0), 'R1-1'),
    ('yieldsign', SHS(R, 1), 'R1-2'),
    ('signpost4way', SHS(R, 3), 'R1-3'),
    ('signpostallway', SHS(R, 4), 'R1-4'),
    ('signpostspeed50', SHS(R, 11), 'R2-1'),
    ('signpostmin40', SHS(R, 17), 'R2-4'),
    ('signnorightturn', SHS(R, 22), 'R3-1'),
    ('signnoleftturn', SHS(R, 24), 'R3-2'),
    ('signnoturns', SHS(R, 26), 'R3-3'),
    ('signnouturn', SHS(R, 27), 'R3-4'),
    ('signbuslane', SHS(R, 30, pick=3), 'R3-5b'),
    ('signleftmustturnleft', SHS(R, 31), 'R3-7L'),
    ('buslaneahead', SHS(R, 44), 'R3-10a'),
    ('signhovlaneends', SHS(R, 51), 'R3-14'),   # portrait plate: the R3-14 is 24 x 30
    ('signhovlaneahead', SHS(R, 58), 'R3-15'),
    ('signaheadplaque', SHS(R, 60, pick=1), 'R3-17a'),
    ('signendsplaque', SHS(R, 60, pick=2), 'R3-17b'),
    ('signdonotpass', SHS(R, 62), 'R4-1'),
    ('signpasswithcare', SHS(R, 63), 'R4-2'),
    ('signslowertraffickeepright', SHS(R, 64), 'R4-3'),
    ('signtrucksuserightlanes', SHS(R, 66), 'R4-5'),
    ('signtrucklane500ft', SHS(R, 67), 'R4-6'),
    ('signpostkeepright', SHS(R, 68), 'R4-7'),
    ('signdonotenter', SHS(R, 74), 'R5-1'),
    ('signwrongway', SHS(R, 75), 'R5-1a'),
    ('signnotrucks', SHS(R, 77), 'R5-2'),
    ('signnomotorvehicles', SHS(R, 79), 'R5-3'),
    ('signcommercialexclude', SHS(R, 80), 'R5-4'),   # portrait plate (2026-09-13)
    ('signvehiclelugsprohibit', SHS(R, 81), 'R5-5'),   # portrait plate (2026-09-13)
    ('signmotorcycleprohibit', SHS(R, 84), 'R5-8'),
    ('signpedestrianprohibit', SHS(R, 86, pick=1), 'R5-10b'),
    ('signloadzonenoparking', SHS(R, 93, pick=1), 'R7-6'),
    ('signbusstopnoparking', SHS(R, 93, pick=2), 'R7-7'),
    ('signnoparkingbikelane', SHS(R, 95), 'R7-9'),
    ('signtowawayzone', SHS(R, 100), 'R7-201'),
    ('signnoparkingonpave', SHS(R, 102), 'R8-1'),
    ('signnoparkingexceptshoulder', SHS(R, 103), 'R8-2'),
    ('signnoparkingtext', SHS(R, 104), 'R8-3'),
    ('signnoparking', SHS(R, 105), 'R8-3a'),
    ('signemergencyparkingonly', SHS(R, 109), 'R8-4'),
    ('signnostoppingpavement', SHS(R, 110), 'R8-5'),
    ('signnostoppingexceptshoulder', SHS(R, 111), 'R8-6'),
    ('signemergencystoppingonly', SHS(R, 112), 'R8-7'),
    ('signstophereflashing', SHS(R, 115), 'R8-10'),
    ('signwalkleft', SHS(R, 116), 'R9-1'),
    ('signcrossatcrosswalks', SHS(R, 116, pick=1), 'R9-2'),
    ('signnohitchhiking', SHS(R, 120), 'R9-4a'),
    ('signpostsidewalkclosed', SHS(R, 127), 'R9-11a'),   # left arrow; the right one is a gap sign
    ('signleftongreenarrow', SHS(R, 135), 'R10-5'),
    ('signpostdonotblock', SHS(R, 137), 'R10-7'),
    ('signuselanewithgreenarrow', SHS(R, 138), 'R10-8'),
    ('signleftturnsignal', SHS(R, 139), 'R10-10L'),
    ('signpostleftturnyieldgreen', SHS(R, 143), 'R10-12'),
    ('signesignal', SHS(R, 144), 'R10-13'),
    ('signr1016', SHS(R, 146), 'R10-16'),
    ('signkeepoffmedian', SHS(R, 153), 'R11-1'),
    ('signrdclosed', SHS(R, 154), 'R11-2'),
    ('signrdclosedthrutraffic', SHS(R, 156), 'R11-4'),
    ('signweightlimit10ton', SHS(R, 157), 'R12-1'),
    ('signaxle5tonlimit', SHS(R, 159), 'R12-2'),
    # --- Phase 2: Warning, the confident exact matches. Not here: signtruckhalf is a TRUCK
    # legend, not the W11-10 symbol; signleftrightarrow is the two-headed W1-7, not W16-5p.
    ('signpsotstopahead', SHS(W, 19), 'W3-1'),
    ('signyieldahead', SHS(W, 21), 'W3-2'),
    ('signsignalahead', SHS(W, 23), 'W3-3'),
    ('signpostspeed55', SHS(R, 11, replace=('50', '55')), 'R2-1 (55)'),
    ('signaddleft', SHS(W, 31, pick=1), 'W4-3L'),
    ('signaddright', SHS(W, 31), 'W4-3R'),
    ('signnarrowbridge', SHS(W, 36), 'W5-2'),
    ('signonelanebridge', SHS(W, 37), 'W5-3'),
    ('signdivhw', SHS(W, 41), 'W6-1a'),
    ('signdividedroad', SHS(W, 42), 'W6-1b'),
    ('signdivhwend', SHS(W, 44), 'W6-2a'),
    ('signdividedhwstart', SHS(W, 40), 'W6-1'),    # the symbol pair; the worded ones are above
    ('signdividedhwend', SHS(W, 43), 'W6-2'),
    ('signtwowaytraffic', SHS(W, 46), 'W6-3'),
    ('signtruckhill', SHS(W, 47), 'W7-1'),
    ('signhill', SHS(W, 48), 'W7-1a'),
    ('signtruck8grade', SHS(W, 49), 'W7-1b'),
    ('signbump', SHS(W, 58), 'W8-1'),
    ('signdip', SHS(W, 59), 'W8-2'),
    ('signpavementends', SHS(W, 60), 'W8-3'),
    ('signsoftshoulder', SHS(W, 61), 'W8-4'),
    ('signtruckcrossing', SHS(W, 63), 'W8-6'),
    ('signunevenlanes', SHS(W, 69), 'W8-11'),
    # the mod's pedestrian family is fluorescent yellow-green, as the current MUTCD allows
    ('signbicycle', SHS(W, 90, palette=FYG_FACE), 'W11-1'),
    ('signpedestrian', SHS(W, 91, palette=FYG_FACE), 'W11-2'),
    ('signdeer', SHS(W, 92), 'W11-3'),
    ('signexit25', SHS(W, 109), 'W13-2'),
    ('signcurve25', SHS(W, 113), 'W13-5'),
    ('signdeadend', SHS(W, 115), 'W14-1'),
    ('signnooutlet', SHS(W, 117), 'W14-2'),
    ('signplayground', SHS(W, 119), 'W15-1'),
    ('signshareroad', SHS(W, 120), 'W16-1'),
    ('signnosigns', SHS(W, 133), 'W18-1'),
    ('signoncominghasextendedgreen', SHS(W, 134), 'W25-1'),
    ('signoncomingmayextendedgreen', SHS(W, 135), 'W25-2'),
    ('signendroadwork', SHS(W, 168, pick=2), 'G20-2'),
    ('signexitclosed', SHS(W, 169, pick=2), 'E5-2a'),
    # --- Phase 3: Guide and EM panel signs. The recreational pictograms (camping, dog, kayak,
    # ...) are drawn in the book as bare symbols, some in several pieces, with no panel; they
    # wait for a symbol-on-panel mode. Fallout Shelter stays: the mod's is the classic yellow
    # sign, not the EM chapter's white directional one. Police is a badge, not the D9-14
    # legend. The D5-3 and M1-10 pages draw no white border, so those two keep their faces.
    ('signnorth', SHS(G, 13), 'M3-1'),
    ('signeast', SHS(G, 13, pick=1), 'M3-2'),
    ('signsouth', SHS(G, 14), 'M3-3'),
    ('signwest', SHS(G, 14, pick=1), 'M3-4'),
    ('signtemporary', SHS(G, 18, pick=1), 'M4-7'),
    ('signrestarea1mile', SHS(G, 35), 'D5-1'),
    ('signscenicoverlook2miles', SHS(G, 51), 'D6-1'),
    ('signweighstation1mile', SHS(G, 54), 'D8-1'),
    ('signhospital', SHS(G, 60), 'D9-2'),
    ('signgas', SHS(G, 65), 'D9-7'),
    ('signfreewayentrance', SHS(G, 92), 'D13-3'),
    ('signairport', SHS(G, 98), 'I-5'),
    ('signbusstation', SHS(G, 99), 'I-6'),
    ('signtrainstation', SHS(G, 100), 'I-7'),
    ('signlibrary', SHS(G, 101), 'I-8'),
    ('signtrafficctlpoint', SHS(E, 1, pick=2), 'EM-3'),
    # --- Phase 4: the near matches -- the book draws one numeral, replace= sets the mod's.
    # Not here: signkeepright1/2 (KEEP RIGHT with an arrow, not the R4-7 symbol), sign14_4
    # (a diamond, not the W12-2p plaque), the hurricane left/right (the EM-1 arrow points
    # up only), and the pictograms (Phase 3b).
    ('signspeed0', SHS(R, 11, replace=('50', '0')), 'R2-1 (0)'),
    ('signspeed5', SHS(R, 11, replace=('50', '5')), 'R2-1 (5)'),
    ('signspeed15', SHS(R, 11, replace=('50', '15')), 'R2-1 (15)'),
    ('signspeed20', SHS(R, 11, replace=('50', '20')), 'R2-1 (20)'),
    ('signspeed25', SHS(R, 11, replace=('50', '25')), 'R2-1 (25)'),
    ('signpostspeed30', SHS(R, 11, replace=('50', '30')), 'R2-1 (30)'),
    ('signspeed35', SHS(R, 11, replace=('50', '35')), 'R2-1 (35)'),
    ('signspeed40', SHS(R, 11, replace=('50', '40')), 'R2-1 (40)'),
    ('signspeed45', SHS(R, 11, replace=('50', '45')), 'R2-1 (45)'),
    ('signspeed65', SHS(R, 11, replace=('50', '65')), 'R2-1 (65)'),
    ('signspeed75', SHS(R, 11, replace=('50', '75')), 'R2-1 (75)'),
    ('signpost50min30', SHS(R, 19, replace=('55', '50')), 'R2-4a (50/30)'),   # 20 x 40 plate
    ('signhandicapreservedparking', SHS(R, 93, pick=3, palette=SLATE_TO_BLUE), 'R7-8'),
    ('signcurve15', SHS(W, 113, replace=('25', '15')), 'W13-5 (15)'),
    ('signcurve35', SHS(W, 113, replace=('25', '35')), 'W13-5 (35)'),
    ('signcurve45', SHS(W, 113, replace=('25', '45')), 'W13-5 (45)'),
    ('signramp15', SHS(W, 111, replace=('30', '15')), 'W13-3 (15)'),
    ('signramp25', SHS(W, 111, replace=('30', '25')), 'W13-3 (25)'),
    ('signramp35', SHS(W, 111, replace=('30', '35')), 'W13-3 (35)'),
    ('signtractor', SHS(W, 94), 'W11-5'),
    ('signhandicap', SHS(G, 64), 'D9-6'),
    ('signcrossoverquartermile', SHS(G, 91), 'D13-1'),
    # --- Phase 3b: the recreational pictograms, white on the mod's panel colours
    ('signcamping', SYM(G, 127, [0], 'blue'), 'RS-010'),
    ('signdog', SYM(G, 126, [0, 1], 'brown'), 'RS-030'),
    ('signseaplane', SYM(G, 126, [3], 'brown'), 'RS-080'),
    ('signhelicopter', SYM(G, 144, [0], 'brown'), 'RG-070'),
    ('signatv', SYM(G, 152, [2, 3, 4, 5, 6], 'brown'), 'RL-020'),
    ('signhangglider', SYM(G, 153, [2], 'brown'), 'RL-060'),
    ('signfishing', SYM(G, 156, [0, 1], 'brown'), 'RW-010'),
    ('signswimming', SYM(G, 160, [0], 'brown'), 'RW-080'),
    ('signkayak', SYM(G, 162, [0], 'brown'), 'RW-090'),
    ('signwindsurf', SYM(G, 162, [2], 'brown'), 'RW-120'),
    ('signskilift', SYM(G, 167, [2], 'brown'), 'RM-050'),
    ('signcamper', SYM(G, 135, [2, 3, 4], 'blue'), 'RA-130'),
    ('signboats', SYM(G, 156, [3], 'green'), 'RW-050'),   # the anchor (marina), as the mod draws it
    # signparkingnoarrow stays as drawn: it is one of a matching set with the arrow versions
    # --- Phase 5: the audit's unverified guesses, each checked against the page by eye and
    # kept only where the mod's sign IS that drawing (a custom number, lane or wording stays
    # as the mod drew it). Left/right twins of a legend sign use mirror_symbols. Not taken:
    # the arrow-shaped ONE WAY pair and the W16-5p arrow plaques (3:1 and 4:3 drawings on
    # 4.3:1 and 2:1 plates), the M6 arrows (the mod's are combined Y / T arrows), Weigh
    # Station Next Right (its page draws no white border).
    ('signbustaxionly', SHS(R, 57), 'R3-14b'),
    ('signcenterlaneturnsonly', SHS(R, 37), 'R3-9b'),
    ('signhov2ormorepervehicle', SHS(R, 53), 'R3-13'),
    ('signr105', SHS(R, 135), 'R10-5'),
    ('noparkingsundayholiday', SHS(R, 92), 'R7-3'),
    ('signnotrucksover7000', SHS(R, 161), 'R12-3'),
    ('signweightlimit2peraxle', SHS(R, 161, pick=1), 'R12-4'),
    ('signnonmotorprohibit', SHS(R, 83), 'R5-7'),
    ('signonewayright', SHS(R, 88), 'R6-2R'),
    ('signonewayleft', SHS(R, 88, mirror_symbols=True), 'R6-2L'),
    ('signstopherered', SHS(R, 136), 'R10-6'),   # straight arrow, as the mod drew both
    ('signstopherered2', SHS(R, 136, mirror_symbols=True), 'R10-6 (right)'),
    ('signstophereflashred2', SHS(R, 115, mirror_symbols=True), 'R8-10 (right)'),
    ('signnoparkinganytime', SHS(R, 91, mirror_symbols='both'), 'R7-1 (both ways)'),
    ('signnoturnred', SHS(R, 141), 'R10-11a'),
    ('signoturnonred', SHS(R, 142), 'R10-11b'),
    ('signonehrparking97', SHS(R, 93), 'R7-5 (1 hr)'),
    ('signrightturn', SHS(W, 0), 'W1-1R'),
    ('signleftturn', SHS(W, 0, pick=1), 'W1-1L'),
    ('signhairpinright', SHS(W, 10), 'W1-11R'),
    ('signhairpinleft', SHS(W, 10, mirror=True), 'W1-11L'),
    ('signloopright', SHS(W, 12), 'W1-15R'),
    ('signmergeleftlanends', SHS(W, 30), 'W4-2R'),
    ('signleftends', SHS(W, 30, pick=1), 'W4-2L'),
    ('signblastingzone', SHS(W, 159), 'W22-1'),
    ('rwrkbepreptostop', SHS(W, 25, palette={(255, 245, 0): shs.MOD_COLOURS['orange']}), 'W3-4 (TTC)'),
    ('rwrkflagger', SHS(W, 149), 'W20-7'),
    ('signworkdetourright', SHS(W, 172), 'M4-9R'),
    ('signworkdetouerleft', SHS(W, 172, mirror_symbols=True), 'M4-9L'),
    ('signturnoff2way', SHS(W, 160), 'W22-2'),
    ('signparkingarearight', SHS(G, 40), 'D5-4'),
    ('signrestarearight', SHS(G, 37, pick=1), 'D5-2a'),
    # --- Remaining-signs batch 1 (tab order from the top). Custom, left as drawn:
    # carelesspersonsign, signaheadleftright (three-headed), signbeginplaque (the book's BEGIN
    # is green), bearcrossingsign, beginfwysign / beginhwysign / beginpkwysign.
    ('signupright', SHS(G, 22, pick=2), 'M6-6R'),
    ('signupleft', SHS(G, 22, pick=2, mirror=True), 'M6-6L'),
    ('signaheadright', SHS(G, 22, pick=2), 'M6-6R'),
    ('signaheadleft', SHS(G, 22, pick=2, mirror=True), 'M6-6L'),
    ('signupslightright', SHS(G, 22, pick=4), 'M6-7R'),
    ('signupslightleft', SHS(G, 22, pick=4, mirror=True), 'M6-7L'),
    ('signahead', SHS(G, 21, pick=2), 'M6-3'),
    ('signaheadonly', SHS(R, 29), 'R3-5a'),
    ('signalternate', SHS(G, 15), 'M4-1a'),
    ('signalt', SHS(G, 15, pick=1), 'M4-1'),
    ('signbikelane', SHS(R, 60), 'R3-17'),
    ('signbikelanelarge', SHS(R, 60), 'R3-17'),
    ('signpostweightlimit', SHS(R, 163), 'R12-5'),
    # --- Remaining-signs batch 2. Left as drawn: the centre-lane hour panels
    # (signcenterlanebusonly69, signcenterhov6a9a, signcenterlanenouse79), signcityspeed35,
    # the danger / high-voltage set, signdividedhw1 / 2 (crossing arrows), signdontthinkparking,
    # signdownleftupright.
    ('signbypass', SHS(G, 15, pick=2), 'M4-2'),
    ('signbusiness', SHS(G, 15, pick=3), 'M4-3'),
    ('signend', SHS(G, 16, pick=1), 'M4-6'),
    ('signdoubleoneway', SHS(R, 87, pick=1), 'R6-1L'),
    ('signdoubleonewayb', SHS(R, 87), 'R6-1R'),
    ('signarrowdownright', SHS(G, 21, rotate_symbols=90), 'M6-2R (down)'),
    ('signarrowdownleft', SHS(G, 21, rotate_symbols=90, mirror=True), 'M6-2L (down)'),
    # --- Remaining-signs batch 3. Left as drawn: signexceptbus, signfdcstandpipe (NFPA),
    # forestryvehiclesonlysign, signhov6a9a, signhov2onlyoverhead, signhovahead / signhovends
    # (the book's R3-15a / R3-14 carry the "2+" the other way round), signhovrules,
    # kathieevanssign, signmetro, ladotantigridlockzone, ladotnostopping, signresidentlarge.
    ('signendplaque', SHS(G, 16, pick=1), 'M4-6'),
    ('signkeepright1', SHS(R, 69), 'R4-7b'),
    ('signkeepright2', SHS(R, 70), 'R4-7c'),
    ('signleftright', SHS(G, 21, pick=4), 'M6-4'),
    ('signaheadleftright', SHS(G, 23, pick=1), 'M6-5'),   # batch 1's leftover: it is on p23
    ('signleftahead', SHS(R, 28, pick=2), 'R3-6L'),
    ('signleftonly', SHS(R, 28), 'R3-5L'),
    ('signhurricane', SHS(E, 0), 'EM-1'),   # the three moved to the 24 x 24 square plate
    ('signhurricaneleft', SHS(E, 0, rotate_symbols=-90), 'EM-1 (left)'),
    ('signhurricaneright', SHS(E, 0, rotate_symbols=90), 'EM-1 (right)'),
    ('signbridgeice', SHS(W, 71), 'W8-13'),   # renamed Bridge Ices Before Road
]


# ----------------------------------------------------------------------------- the sign's own plate

def _asset_json(relative_path):
    path = layout.resolve_asset(relative_path)
    if path is None:
        raise SystemExit('missing asset %s' % relative_path)
    with open(path, encoding='utf-8') as fh:
        return json.load(fh)


def plate_aspect(model_ref):
    """Width / height of the plate face the sign texture (slot ``1``) is painted on, read off
    the model's elements: the largest vertical face textured ``#1``."""
    name = model_ref.split(':', 1)[1]
    model = _asset_json('models/block/%s.json' % name)
    best = None
    for el in model.get('elements', []):
        f, t = el['from'], el['to']
        dims = [abs(t[i] - f[i]) for i in range(3)]
        for side, face in el.get('faces', {}).items():
            if face.get('texture') != '#1' or side in ('up', 'down'):
                continue
            w = dims[0] if side in ('north', 'south') else dims[2]
            h = dims[1]
            if best is None or w * h > best[0] * best[1]:
                best = (w, h)
    if best is None:
        raise SystemExit('%s has no face textured #1' % model_ref)
    return best[0] / float(best[1])


def sign_info(registry):
    """Where the sign's face texture lives and the plate it is stretched onto."""
    bs_path = layout.blockstate_file(registry)
    if bs_path is None:
        raise SystemExit('no blockstate for %s' % registry)
    with open(bs_path, encoding='utf-8') as fh:
        bs = json.load(fh)
    defaults = bs['defaults']
    textures = defaults['textures']
    face_ref = textures['1']
    back_ref = textures.get('2')

    def tex_path(ref):
        rel = 'textures/' + ref.split(':', 1)[1] + '.png'
        path = layout.resolve_asset(rel)
        return path or layout.asset_for_write(layout.owner_of(registry), rel)

    return {
        'model': defaults['model'],
        'aspect': plate_aspect(defaults['model']),
        'texture': tex_path(face_ref),
        'back': tex_path(back_ref) if back_ref and back_ref.endswith('_back') else None,
    }


def render(source, info):
    face, mirror, palette = source()
    if isinstance(face, SymbolFace):
        return shs.symbol_on_panel(face.symbol, face.colour, info['aspect'], size=256)
    # a silhouette sign (one with a _back texture) IS its outline: never squash it to fill
    # a 2:1 plaque squishes its legend to 8 px per plate unit across at 128; 256 keeps it
    # readable a few blocks away
    return shs.official_face(face, info['aspect'], mirror, palette,
                             size=256 if info['aspect'] >= 1.8 else shs.DEFAULT_TEX,
                             stretch_tol=0.0 if info['back'] else None)


# ----------------------------------------------------------------------------- sheets

def _at_aspect(img, aspect, h=150, max_w=200):
    """The square texture stretched back to its plate, fitted in ``max_w`` x ``h``."""
    w = int(round(h * aspect))
    if w > max_w:
        w, h = max_w, int(round(max_w / aspect))
    return img.convert('RGBA').resize((w, h), Image.LANCZOS)


def contact_sheet(entries, out, cols=3):
    """Before / after per entry: the current texture and the new face, both stretched to
    the plate, captioned with the registry and the SHS code."""
    cell_w, cell_h = 420, 200
    rows = (len(entries) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * cell_w, rows * cell_h), (60, 60, 70, 255))
    d = ImageDraw.Draw(sheet)
    for i, (registry, source, code) in enumerate(entries):
        info = sign_info(registry)
        x0, y0 = (i % cols) * cell_w, (i // cols) * cell_h
        before = _at_aspect(Image.open(info['texture']), info['aspect']) \
            if os.path.exists(info['texture']) else None
        after = _at_aspect(render(source, info), info['aspect'])
        if before is not None:
            sheet.alpha_composite(before, (x0 + 10 + (200 - before.width) // 2, y0 + 10))
        sheet.alpha_composite(after, (x0 + 210 + (200 - after.width) // 2, y0 + 10))
        d.text((x0 + 10, y0 + 170), '%s  <-  %s' % (registry, code), fill=(235, 235, 235, 255))
        d.text((x0 + 10, y0 + 184), os.path.basename(info['texture']), fill=(170, 170, 170, 255))
    sheet.save(out)
    return sheet.size


_ROW_RE = re.compile(r'^\| (\S+) \| (.*?) \| (EXACT|NEAR|NONE)\? \| (\w+) p(\d+)\s*([^|]*)\| (.*?) \|$')


def verify_sheets(prefix, per_sheet=8, categories=('EXACT', 'NEAR')):
    """Every ``?`` row of the match table in ``categories`` that cites a page and is not yet
    catalogued: current texture beside the page thumbnail, so a human can confirm or reject
    the guess."""
    rows = []
    done = {e[0] for e in CATALOGUE}
    with open(MATCH_TABLE, encoding='utf-8') as fh:
        for line in fh:
            m = _ROW_RE.match(line.rstrip('\n'))
            if m and m.group(3) in categories and m.group(1) not in done:
                rows.append(m.groups())
    print('%d ? rows with a cited page' % len(rows))
    cell_w, cell_h = 640, 330
    for s in range(0, len(rows), per_sheet):
        chunk = rows[s:s + per_sheet]
        sheet = Image.new('RGBA', (2 * cell_w, ((len(chunk) + 1) // 2) * cell_h), (60, 60, 70, 255))
        d = ImageDraw.Draw(sheet)
        for i, (registry, display, cat, chapter, page, code, note) in enumerate(chunk):
            x0, y0 = (i % 2) * cell_w, (i // 2) * cell_h
            try:
                info = sign_info(registry)
                cur = _at_aspect(Image.open(info['texture']), info['aspect'], h=200)
                sheet.alpha_composite(cur, (x0 + 10 + (230 - cur.width) // 2, y0 + 10))
            except SystemExit as e:
                d.text((x0 + 10, y0 + 100), str(e), fill=(255, 120, 120, 255))
            pix = shs.book_page(chapter, int(page)).get_pixmap(dpi=28)
            thumb = Image.frombytes('RGB', (pix.width, pix.height), pix.samples).convert('RGBA')
            sheet.alpha_composite(thumb, (x0 + 250, y0 + 10))
            d.text((x0 + 10, y0 + 230), '%s (%s?)' % (registry, cat), fill=(235, 235, 235, 255))
            d.text((x0 + 10, y0 + 246), display[:60], fill=(200, 200, 200, 255))
            d.text((x0 + 10, y0 + 262), '%s p%s %s' % (chapter, page, code.strip()), fill=(200, 200, 200, 255))
            d.text((x0 + 10, y0 + 278), note[:90], fill=(160, 160, 160, 255))
        out = '%s_%02d.png' % (prefix, s // per_sheet)
        sheet.save(out)
        print('wrote', out)


# ----------------------------------------------------------------------------- main

def main():
    args = sys.argv[1:]

    def opt(name):
        if name in args:
            i = args.index(name)
            return args[i + 1]
        return None

    if '--verify-sheet' in args:
        verify_sheets(opt('--verify-sheet'))
        return

    only = opt('--only')
    entries = CATALOGUE if not only else [e for e in CATALOGUE if e[0] in only.split(',')]
    if not entries:
        raise SystemExit('nothing selected')

    sheet = opt('--sheet')
    if sheet:
        print('sheet %s %s' % (sheet, contact_sheet(entries, sheet)))
        return

    check = '--check' in args
    drift = []
    for registry, source, code in entries:
        info = sign_info(registry)
        face = render(source, info)
        targets = [(info['texture'], face)]
        if info['back']:
            targets.append((info['back'], shs.back_texture(face)))
        for path, img in targets:
            if check:
                cur = Image.open(path).convert('RGBA') if os.path.exists(path) else None
                if cur is None or cur.size != img.size or cur.tobytes() != img.tobytes():
                    drift.append(path)
            else:
                img.save(path)
        if not check:
            print('wrote %s <- %s (%s, plate %.2f)' % (
                os.path.basename(info['texture']), code, registry, info['aspect']))
    if check:
        if drift:
            print('DRIFT: %d texture(s) differ from the generator:' % len(drift))
            for p in drift:
                print('  ' + p)
            sys.exit(1)
        print('all %d official faces match the generator' % len(entries))


if __name__ == '__main__':
    main()
