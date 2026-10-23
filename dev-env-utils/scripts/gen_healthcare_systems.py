#!/usr/bin/env python3
"""
gen_healthcare_systems.py -- the Healthcare module's wayfinding and building systems.

Wayfinding: only the hospital wayfinding panel's name, tab line and messages. Its sheets, Java
layout, blockstate and models are gen_wayfinding.py's (one engine in Core draws all three
wayfinding panels, GitHub #307).

Building systems: the code blue button and annunciator (a blue strobe, an animated texture, with
the chime from gen_healthcare_sounds.py), the medical gas zone valve box and master alarm panel,
normal and emergency power outlets, and the infant security door panel.

Nothing here is a real product: no maker's name, no red cross (a protected emblem). Lamps and
strobes are animated textures (.mcmeta), so nothing ticks and nothing has a renderer.

Writes, under modules/healthcare/src/main/resources/assets/csm:

  * textures/blocks/healthcare/systems/*.png (+ .mcmeta)
  * models/block/healthcare/systems/*.json
  * blockstates/<registry>.json, and models/item/<registry>.json where needed
  * the tile names and messages in all four lang files

Usage:
    py gen_healthcare_systems.py              # write everything
    py gen_healthcare_systems.py --check      # fail if the tree has drifted
    py gen_healthcare_systems.py --fragments  # print the tab registration lines

Requires Pillow.
"""
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_healthcare_common as hc  # noqa: E402
import gen_healthcare_wards as hw  # noqa: E402
from gen_healthcare_common import (  # noqa: E402
    draw_text_centred, fill, frame, rect, shade)

C = hc.catalogue("gen_healthcare_systems.py", "systems")
T = C.T
M = C.M
W = hw.C.T    # the wards' shared swatches

el = hw.el
rot = hw.rot
model = hw.model
ITEM_SMALL = hw.ITEM_SMALL
B = hw.B
FULL = [0, 0, 16, 16]

PC = hc.PATIENT_CARE
CL = hc.CLINICAL

def tex(name, fn):
    C.textures[name] = fn


def lamp(img, cx, cy, size, colour):
    """A status lamp: a square, never a small disc (a disc a few texels across draws as a plus
    sign, and a red one as a cross)."""
    x0, y0 = int(round(cx - size / 2.0)), int(round(cy - size / 2.0))
    rect(img, x0, y0, x0 + size, y0 + size, colour)


def MI(name):
    return "csm:block/%s/%s" % (C.model_dir, name)


def front(frm, to, base, key, side="north", faces=hw.ALL):
    """A box whose front shows the whole of a texture."""
    return el(frm, to, base, faces=faces, per={side: key}, uv={side: FULL})


def wall_block(reg, java, names, textures, elements, tab, extra=None, display=None):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    C.add(reg, java, names, {reg: model(textures, elements, display=display)},
          hc.facing_state(M(reg), extra), tab=tab)


def java_for(cls, reg, box6, mat, collides=False):
    return 'new %s("%s", %s, HealthcareMaterial.%s, BlockHealthcare.Mount.WALL, %s)' % (
        cls, reg, B(*box6), mat, "true" if collides else "false")


# ==========================================================================================
# The wayfinding panel
# ==========================================================================================
# Its sheets, blockstate, models and item are gen_wayfinding.py's (Core's wayfinding engine draws
# all three wayfinding panels, #307); only its name, tab line and messages are here.
def wayfinding_panel():
    reg = "hospital_wayfinding_panel"
    C.add(reg, 'new BlockHospitalWayfindingPanel("%s")' % reg,
          ("Hospital Wayfinding Panel", "Krankenhaus-Wegweisertafel",
           "Panel de señalización hospitalaria", "Vägvisningsskylt (sjukhus)"),
          {}, None, tab=PC)


wayfinding_panel()


# ==========================================================================================
# Code blue
# ==========================================================================================
CODE_BLUE = (28, 84, 176)
CODE_BLUE_DARK = (18, 52, 116)


@C.texture("code_blue_body")
def _code_blue_body():
    return hc.swatch(CODE_BLUE, 9101, grain=2)


def code_blue_face(lit):
    """The code blue station's front: CODE BLUE over a white push button, PUSH on it, and a
    lamp that lights while the code is active."""
    def f():
        img = fill(CODE_BLUE, size=32, grain=1, seed=9102)
        frame(img, 0, 0, 32, 32, shade(CODE_BLUE, 0.8))
        hc.stamp_legend(img, "CODE", 16, 3, (250, 250, 250), 5)
        hc.stamp_legend(img, "BLUE", 16, 10, (250, 250, 250), 5)
        rect(img, 8, 17, 24, 28, (236, 236, 232))
        frame(img, 8, 17, 24, 28, (170, 172, 176))
        hc.stamp_legend(img, "PUSH", 16, 20, CODE_BLUE_DARK, 4)
        lamp(img, 27.5, 4.5, 3, (255, 230, 120) if lit else (90, 70, 40))
        return img
    return f


tex("code_blue_front", code_blue_face(False))
tex("code_blue_front_on", code_blue_face(True))

BUTTON = [el([4, 3, 14], [12, 13, 16], "body", per={"north": "front"}, uv={"north": FULL})]
wall_block("code_blue_button",
           'new BlockCodeBlueButton("code_blue_button", %s)' % B(4, 3, 13, 12, 13, 16),
           ("Code Blue Button", "Code-Blue-Taster", "Pulsador de código azul",
            "Larmknapp för hjärtstopp (Code Blue)"),
           {"body": T("code_blue_body"), "front": T("code_blue_front")},
           BUTTON, CL, extra={"active": {"false": {}, "true": {"textures": {
               "front": T("code_blue_front_on")}}}})


@C.texture("code_blue_lens_off")
def _code_blue_lens_off():
    img = fill((50, 96, 190), grain=1, seed=9110)
    for y in range(1, 16, 3):
        rect(img, 0, y, 16, y + 1, (70, 120, 210))
    return img


def strobe_frame(i):
    """The strobe's flash: dark but for two bright frames in twenty, once a second."""
    def f():
        if i in (0, 1, 2):
            img = fill((200, 226, 255), grain=1, seed=9111)
            for y in range(1, 16, 3):
                rect(img, 0, y, 16, y + 1, (240, 248, 255))
            if i == 2:
                img = fill((120, 170, 250), grain=1, seed=9112)
            return img
        return _code_blue_lens_off()
    return f


hc.animation(C, "code_blue_lens_flash", [strobe_frame(i) for i in range(20)], 1)


@C.texture("code_blue_label")
def _code_blue_label():
    img = fill((240, 240, 236), size=32, grain=1, seed=9113)
    rect(img, 0, 0, 32, 32, CODE_BLUE)
    hc.stamp_legend(img, "CODE BLUE", 16, 12, (250, 250, 250), 7, max_w=30)
    return img


@C.texture("speaker_grille")
def _speaker_grille():
    img = fill((226, 228, 226), grain=1, seed=9114)
    for y in range(1, 16, 2):
        for x in range(1, 16, 2):
            rect(img, x, y, x + 1, y + 1, (120, 122, 126))
    return img


ANNUNCIATOR = ([el([3, 1, 14], [13, 15, 16], "body"),
                front([4, 2, 13.6], [12, 6, 14], "body", "label", faces=("north", "east",
                                                                         "west", "up", "down")),
                front([4.5, 7, 13.6], [11.5, 9.5, 14], "body", "grille",
                      faces=("north", "east", "west", "up", "down"))]
               + hc.pipe_z(8, 12, 1.9, 12.2, 14, "lens"))
wall_block("code_blue_annunciator",
           'new BlockCodeBlueAnnunciator("code_blue_annunciator", %s)' % B(3, 1, 12, 13, 15, 16),
           ("Code Blue Annunciator (Strobe and Chime)", "Code-Blue-Melder (Blitz und Gong)",
            "Anunciador de código azul (estroboscópico y carillón)",
            "Code Blue-larmdon (blixt och klocka)"),
           {"body": W("white"), "label": T("code_blue_label"), "grille": T("speaker_grille"),
            "lens": T("code_blue_lens_off")},
           ANNUNCIATOR, CL, extra={"active": {"false": {}, "true": {"textures": {
               "lens": T("code_blue_lens_flash")}}}})


def cb_lang(key, en, de, es, sv):
    C.add_lang("csm.healthcare.codeblue." + key, (en, de, es, sv))


cb_lang("activated", "[Code Blue] Activated at %s.", "[Code Blue] Ausgelöst bei %s.",
        "[Código azul] Activado en %s.", "[Code Blue] Utlöst vid %s.")
cb_lang("reset", "[Code Blue] Reset (%s station(s) cleared).",
        "[Code Blue] Zurückgesetzt (%s Taster gelöscht).",
        "[Código azul] Restablecido (%s pulsador(es) borrado(s)).",
        "[Code Blue] Återställt (%s knapp(ar) kvitterade).")
cb_lang("powered", "[Code Blue] Still held on by redstone.",
        "[Code Blue] Wird noch durch Redstone gehalten.",
        "[Código azul] Sigue activo por redstone.", "[Code Blue] Hålls fortfarande av redstone.")

# ==========================================================================================
# Medical gas: the zone valve box and the master alarm panel
# ==========================================================================================
GAS = (("O2", hc.O2_GREEN, (250, 250, 250)), ("AIR", hc.AIR_YELLOW, (30, 30, 30)),
       ("VAC", hc.VAC_WHITE, (30, 30, 30)))


@C.texture("valve_box_back")
def _valve_box_back():
    """The box's back panel: each gas's label band over its riser."""
    img = fill((214, 216, 214), size=32, grain=1, seed=9201)
    for i, (name, bg, fg) in enumerate(GAS):
        x0 = 1 + i * 10
        rect(img, x0, 2, x0 + 10, 7, bg)
        frame(img, x0, 2, x0 + 10, 7, shade(bg, 0.7))
        draw_text_centred(img, name, x0 + 5, 2, fg)
    return img


@C.texture("valve_box_glass")
def _valve_box_glass():
    """The cover's window: clear, a couple of glints, and the instruction along the bottom."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for k in range(5):
        for t in range(10):
            x, y = 3 + t + k * 0, 3 + t + k
            if 0 <= x < 32 and 0 <= y < 32 and k in (0, 3):
                px[x + 14, y] = (250, 252, 255, 110)
    rect(img, 0, 25, 32, 32, (200, 30, 30))
    draw_text_centred(img, "SHUTOFF", 16, 26, (250, 250, 250))
    frame(img, 0, 0, 32, 32, (190, 192, 196))
    return img


@C.texture("gas_gauge")
def _gas_gauge():
    """A pressure gauge's round dial, clear outside its steel rim (a cutout on a flat face):
    the scale's ticks, a red band at the low end and the needle at the working pressure."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            d = math.hypot(dx, dy)
            if d >= 8:
                continue
            a = math.degrees(math.atan2(-dy, dx))
            c = (246, 246, 242)
            if d >= 6.8:
                c = (150, 154, 158)
            elif 5.2 <= d < 6.6 and (a > -45 or a < -135) and int(a + 360) % 30 < 9:
                c = (40, 40, 44)
            elif 5.2 <= d < 6.6 and -135 <= a < -110:
                c = (200, 40, 40)
            px[x, y] = c + (255,)
    for t in range(6):
        px[8 + t // 2, 8 - t] = (20, 20, 24, 255)   # the needle, at the working pressure
    rect(img, 7, 7, 9, 9, (40, 40, 44))
    return img


tex("copper", lambda: hc.swatch((184, 112, 72), 9203, grain=3))

VALVE = [el([1, 1, 14.5], [15, 15, 16], "frame", faces=("east", "west", "up", "down")),
         front([1.5, 1.5, 15.8], [14.5, 14.5, 16], "frame", "back", faces=("north",)),
         # the cover's frame and its window
         el([0.5, 0.5, 13], [15.5, 1.5, 14.5], "frame"), el([0.5, 14.5, 13], [15.5, 15.5, 14.5],
                                                            "frame"),
         el([0.5, 1.5, 13], [1.5, 14.5, 14.5], "frame"), el([14.5, 1.5, 13], [15.5, 14.5, 14.5],
                                                             "frame"),
         el([1.5, 1.5, 13.2], [14.5, 14.5, 13.2], "glass", faces=("north", "south"),
            uv={"north": FULL, "south": FULL})]
for i, (name, colour, _fg) in enumerate(GAS):
    cx = 13.25 - i * 5.0     # north face mirrors x: the first gas on the reader's left
    VALVE += hc.pipe_z(cx, 6.5, 0.9, 14.6, 15.8, "copper", front=False)
    VALVE += [el([cx - 0.6, 3, 15], [cx + 0.6, 12, 15.8], "copper")]
    VALVE += hc.pipe_z(cx, 10.4, 1.4, 14.6, 15.4, "steel", front=False)
    VALVE += [el([cx - 1.6, 8.8, 14.5], [cx + 1.6, 12, 14.5], "gauge", faces=("north",),
                 uv={"north": FULL})]
    VALVE += [el([cx - 0.5, 5.6, 13.9], [cx + 0.5, 7.4, 15], "steel"),
              el([cx - 2.2, 6.1, 13.7], [cx + 0.4, 6.9, 14.3], "h%d" % i)]
VALVE_TEX = {"frame": W("stainless"), "back": T("valve_box_back"), "glass": T("valve_box_glass"),
             "copper": T("copper"), "gauge": T("gas_gauge"), "steel": W("stainless"),
             "h0": W("gas_o2"), "h1": W("yellow"), "h2": W("white"), "particle": W("stainless")}
wall_block("medical_gas_zone_valve_box",
           java_for("BlockHealthcare", "medical_gas_zone_valve_box", (0, 0, 13, 16, 16, 16),
                    "METAL"),
           ("Medical Gas Zone Valve Box", "Medizingas-Zonenabsperrkasten",
            "Caja de válvulas de zona de gases medicinales", "Medicinsk gas, zonventilskåp"),
           VALVE_TEX, VALVE, CL)


def gas_alarm_face(alarm, muted, flash):
    """The master alarm's front: the title, and per gas a NORMAL lamp and a LOW/HIGH lamp; in
    alarm the oxygen line's LOW lamp is lit (flashing) and its NORMAL lamp dark; the amber lamp
    by the silence key shows the buzzer is silenced."""
    def f():
        img = fill((232, 232, 228), size=32, grain=1, seed=9210)
        rect(img, 0, 0, 32, 7, (60, 64, 70))
        hc.stamp_legend(img, "MED GAS", 16, 1, (250, 250, 250), 5, series="D", max_w=30)
        for i, (name, colour, fg) in enumerate(GAS):
            y = 8 + i * 6
            rect(img, 1, y, 13, y + 5, colour)
            frame(img, 1, y, 13, y + 5, shade(colour, 0.6))
            draw_text_centred(img, name, 7, y, fg)
            fault = alarm and i == 0
            lamp(img, 18, y + 2.5, 3, (40, 200, 70) if not fault else (30, 70, 40))
            lamp(img, 25, y + 2.5, 3, (250, 40, 30) if fault and flash else (110, 40, 36))
        rect(img, 2, 27, 12, 31, (70, 74, 80))
        lamp(img, 28.5, 29, 3, (250, 190, 40) if muted else (110, 80, 30))
        return img
    return f


tex("gas_alarm_normal", gas_alarm_face(False, False, False))
tex("gas_alarm_normal_muted", gas_alarm_face(False, True, False))
hc.animation(C, "gas_alarm_alarm", [gas_alarm_face(True, False, i == 0) for i in range(2)], 8)
hc.animation(C, "gas_alarm_alarm_muted", [gas_alarm_face(True, True, i == 0) for i in range(2)],
             8)

ALARM_PANEL = [el([2, 2, 14.5], [14, 14, 16], "body", per={"north": "face"},
                  uv={"north": FULL})]
_alarm_states = {"0": {}, "1": {"textures": {"face": T("gas_alarm_alarm")}},
                 "2": {"textures": {"face": T("gas_alarm_normal_muted")}},
                 "3": {"textures": {"face": T("gas_alarm_alarm_muted")}}}
wall_block("medical_gas_master_alarm",
           'new BlockMedicalGasAlarm("medical_gas_master_alarm", %s)' % B(2, 2, 14, 14, 14, 16),
           ("Medical Gas Master Alarm Panel", "Medizingas-Zentralalarmtafel",
            "Panel de alarma maestra de gases medicinales", "Medicinsk gas, huvudlarmtablå"),
           {"body": W("frame_grey"), "face": T("gas_alarm_normal")}, ALARM_PANEL, CL,
           extra={"state": _alarm_states})
C.add_lang("csm.healthcare.gasalarm.muted",
           ("[Medical Gas] Buzzer silenced.", "[Medizingas] Summer stummgeschaltet.",
            "[Gases medicinales] Zumbador silenciado.", "[Medicinsk gas] Summern tystad."))
C.add_lang("csm.healthcare.gasalarm.unmuted",
           ("[Medical Gas] Buzzer on.", "[Medizingas] Summer an.",
            "[Gases medicinales] Zumbador activado.", "[Medicinsk gas] Summern på."))

# ==========================================================================================
# Power outlets: normal (white) and emergency power (red)
# ==========================================================================================


def outlet_face(plate, body):
    """A duplex receptacle on its plate: two faces, each with two slots and a ground hole."""
    def f():
        img = fill(plate, grain=1, seed=9301 + plate[0])
        frame(img, 0, 0, 16, 16, shade(plate, 0.82))
        for y0 in (2, 9):
            rect(img, 4, y0, 12, y0 + 5, body)
            frame(img, 4, y0, 12, y0 + 5, shade(body, 0.85))
            rect(img, 5, y0 + 1, 6, y0 + 3, (30, 30, 30))
            rect(img, 10, y0 + 1, 11, y0 + 3, (30, 30, 30))
            rect(img, 7, y0 + 3, 9, y0 + 4, (30, 30, 30))
        rect(img, 7, 7, 9, 8, shade(plate, 0.6))   # the plate screw
        return img
    return f


tex("outlet_white", outlet_face((236, 234, 226), (242, 240, 232)))
tex("outlet_red", outlet_face((196, 36, 32), (214, 44, 38)))


for reg, key, names in (
        ("hospital_outlet_normal", "white",
         ("Power Outlet (Normal Power)", "Steckdose (Normalnetz)",
          "Toma de corriente (red normal)", "Eluttag (normalnät)")),
        ("hospital_outlet_emergency", "red",
         ("Power Outlet (Emergency Power, Red)", "Steckdose (Notstrom, rot)",
          "Toma de corriente (red de emergencia, roja)", "Eluttag (reservkraft, rött)"))):
    wall_block(reg, java_for("BlockHealthcare", reg, (5, 4, 15, 11, 12, 16), "PLASTIC"), names,
               {key: W("white") if key == "white" else W("red"),
                key + "_face": T("outlet_" + key)},
               [el([5, 4, 15.4], [11, 12, 16], key, per={"north": key + "_face"},
                   uv={"north": FULL})], CL)

wall_block("hospital_outlet_pair",
           java_for("BlockHealthcare", "hospital_outlet_pair", (2, 4, 15, 14, 12, 16), "PLASTIC"),
           ("Power Outlets (Normal and Emergency)", "Steckdosen (Normal- und Notstrom)",
            "Tomas de corriente (normal y de emergencia)", "Eluttag (normal- och reservkraft)"),
           {"white": W("white"), "white_face": T("outlet_white"), "red": W("red"),
            "red_face": T("outlet_red")},
           [el([8.3, 4, 15.4], [14, 12, 16], "white", per={"north": "white_face"},
               uv={"north": FULL}),
            el([2, 4, 15.4], [7.7, 12, 16], "red", per={"north": "red_face"},
               uv={"north": FULL})], CL)

# ==========================================================================================
# The infant security door panel
# ==========================================================================================


def infant_face(alarm, lit):
    """The panel's front: a badge reader, a keypad and a status band, SECURE in green, or ALARM
    in red flashing."""
    def f():
        img = fill((52, 56, 62), size=32, grain=1, seed=9401)
        frame(img, 0, 0, 32, 32, (36, 38, 42))
        band = (200, 30, 30) if alarm and lit else (90, 24, 24) if alarm else (30, 150, 70)
        rect(img, 3, 3, 29, 10, band)
        hc.stamp_legend(img, "ALARM" if alarm else "SECURE", 16, 4, (250, 250, 250), 5,
                        max_w=24)
        rect(img, 4, 13, 14, 23, (34, 36, 40))
        hc.emblem(img, 9, 18, 3.4, (120, 170, 220))
        for r in range(3):
            for c in range(3):
                rect(img, 17 + c * 4, 13 + r * 4, 20 + c * 4, 16 + r * 4, (200, 202, 206))
        lamp(img, 9, 27, 3, (250, 60, 40) if alarm and lit else (60, 200, 90) if not alarm
             else (90, 40, 30))
        return img
    return f


tex("infant_panel_secure", infant_face(False, False))
hc.animation(C, "infant_panel_alarm", [infant_face(True, i == 0) for i in range(2)], 6)
wall_block("infant_security_panel",
           'new BlockInfantSecurityPanel("infant_security_panel", %s)' % B(4, 3, 14, 12, 13, 16),
           ("Infant Security Door Panel", "Säuglingsschutz-Türterminal",
            "Panel de seguridad infantil de puerta", "Spädbarnslarm, dörrpanel"),
           {"body": W("dark_grey"), "face": T("infant_panel_secure")},
           [el([4, 3, 14.5], [12, 13, 16], "body", per={"north": "face"}, uv={"north": FULL})],
           CL, extra={"alarm": {"false": {}, "true": {"textures": {
               "face": T("infant_panel_alarm")}}}})
C.add_lang("csm.healthcare.infant.reset",
           ("[Infant Security] Alarm reset.", "[Säuglingsschutz] Alarm zurückgesetzt.",
            "[Seguridad infantil] Alarma restablecida.", "[Spädbarnslarm] Larmet återställt."))
C.add_lang("csm.healthcare.infant.held",
           ("[Infant Security] The tag alarm is still active.",
            "[Säuglingsschutz] Der Tag-Alarm ist noch aktiv.",
            "[Seguridad infantil] La alarma de la etiqueta sigue activa.",
            "[Spädbarnslarm] Brickans larm är fortfarande aktivt."))
C.add_lang("csm.healthcare.infant.secure",
           ("[Infant Security] Secure; no alarm.", "[Säuglingsschutz] Gesichert, kein Alarm.",
            "[Seguridad infantil] Seguro; sin alarma.", "[Spädbarnslarm] Säkrat; inget larm."))


# ==========================================================================================
# Writing
# ==========================================================================================
def main():
    frag = "--fragments" in sys.argv
    check = "--check" in sys.argv
    rc = C.main()
    if rc == 0 and not frag and not check:
        written = []
        for root, _dirs, files in os.walk(hc.ASSETS):
            written += [os.path.join(root, f) for f in files]
        hc.check_json(written)
    return rc


if __name__ == "__main__":
    sys.exit(main())
