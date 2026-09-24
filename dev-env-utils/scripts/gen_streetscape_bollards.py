#!/usr/bin/env python3
"""Every asset the Streetscape tab's bollards ship: cast-iron decorative bollards, steel security
bollards, flexible delineators, and the big painted concrete sphere (an OBJ lathe, since a JSON
model can only build a ball from stepped octagons).

    python dev-env-utils/scripts/gen_streetscape_bollards.py
    python dev-env-utils/scripts/gen_streetscape_bollards.py --check
    python dev-env-utils/scripts/gen_streetscape_bollards.py --fragments   # tab lines to paste

External road mods already ship the common bollards (plain, ringed and striped steel, retractable,
folding, portable, concrete, lit), so these are only the styles they lack. Each is a single-cell
BlockUtilityBox (settling onto sloped surfaces, turned four ways); the flexible delineators are
BlockBollardFlexible, which has no collision box, because a real one folds over when it is hit.

Round parts are the mod's usual exact octagons (life_safety_gen_common.post). The helpers that
measure a model into its tab line come from gen_streetscape_utility.py. No rusted twins: bollards
are painted or stainless and replaced when damaged (the user's choice, 2026-09-23).
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402
import gen_pedestal_pole as pole  # noqa: E402
import gen_streetscape_utility as gu  # noqa: E402

C = lc.Catalogue("gen_streetscape_bollards.py", "streetscape/bollards", "streetscape/bollards",
                 assets=gu.ASSETS)
TAB = "CsmTabStreetscape"
post = lc.post
slab = gu.slab

BLACK = (34, 36, 36)
GREEN = (30, 58, 42)
STAINLESS = (176, 180, 182)
STEEL = (70, 72, 74)
SAFETY_YELLOW = (226, 186, 30)
WHITE = (236, 236, 232)
REFLECTIVE = (246, 246, 240)
SPHERE_RED = (196, 30, 36)

# The concrete sphere: a ball about 0.85 of a block across whose bottom sits a little below the
# surface, so it stands on a small flat ring rather than balancing on a point.
SPHERE_RADIUS = 6.8
SPHERE_CENTRE_Y = 6.6
SPHERE_STEPS = 16
SPHERE_SIDES = 24


# ------------------------------------------------------------------------------------------
# Textures
# ------------------------------------------------------------------------------------------
def fluted(colour, seed):
    """Cast iron with vertical flutes: a dark groove every four texels, lit on one side."""
    img = lc.fill(colour, 32, 3, seed)
    for x in range(0, 32, 4):
        lc.rect(img, x, 0, x + 1, 32, lc.shade(colour, 0.6))
        lc.rect(img, x + 1, 0, x + 2, 32, lc.shade(colour, 1.25))
    return img


def brushed(colour, seed):
    """Brushed stainless: fine vertical streaks."""
    rng = random.Random(seed)
    img = lc.fill(colour, 32, 2, seed)
    px = img.load()
    for x in range(32):
        k = rng.uniform(0.9, 1.1)
        for y in range(32):
            r, g, b, a = px[x, y]
            px[x, y] = lc.shade((r, g, b), k) + (a,)
    return img


def reflective(seed):
    """Retroreflective sheeting: bright white with a sparkle of prismatic dots."""
    rng = random.Random(seed)
    img = lc.fill(REFLECTIVE, 32, 2, seed)
    px = img.load()
    for _ in range(60):
        x, y = rng.randrange(32), rng.randrange(32)
        px[x, y] = lc.shade(REFLECTIVE, rng.choice((0.85, 1.0))) + (255,)
    return img


def base_plate(seed):
    """A steel base plate with an anchor bolt near each corner."""
    img = lc.fill(STEEL, 32, 3, seed)
    for bx, by in ((4, 4), (26, 4), (4, 26), (26, 26)):
        lc.rect(img, bx, by, bx + 3, by + 3, lc.shade(STEEL, 1.5))
    return img


def painted_concrete(colour, seed):
    """Painted concrete: the paint's colour with a soft mottle. No chips of grey: the sphere
    wraps this one texture once round its whole girth, so a single odd texel is stretched into
    a streak down the side (the first version had them, and each showed as a grey line)."""
    return lc.fill(colour, 32, 3, seed)


def register_textures():
    tex = {
        "sphere_red": painted_concrete(SPHERE_RED, 51),
        "iron_black": fluted(BLACK, 41),
        "iron_green": fluted(GREEN, 42),
        "black": gu.paint(BLACK, 43, grain=2),
        "green": gu.paint(GREEN, 44, grain=2),
        "stainless": brushed(STAINLESS, 45),
        "steel": gu.paint(STEEL, 46),
        "yellow": gu.paint(SAFETY_YELLOW, 47, grain=3),
        "white": gu.paint(WHITE, 48, grain=2),
        "reflective": reflective(49),
        "plate": base_plate(50),
    }
    for name, img in tex.items():
        C.texture(name)(lambda img=img: img)


register_textures()


# ------------------------------------------------------------------------------------------
# Geometry
# ------------------------------------------------------------------------------------------
def ring(r, y0, y1, tex):
    """A band round a post: an octagon a little wider than the post, capped so its top and
    bottom read as a collar rather than a gap; the caps' middles are hidden inside the post."""
    return post(8, 8, r, y0, y1, tex)


def cast_iron():
    """A fluted post on a plinth, two collars, and an acorn top."""
    return (post(8, 8, 2.6, 0, 1.2, "trim")
            + post(8, 8, 1.8, 1.2, 11, "body", top=False, bottom=False)
            + ring(2.1, 3.0, 3.6, "trim") + ring(2.1, 9.4, 10.0, "trim")
            + post(8, 8, 2.2, 11, 12.2, "trim", bottom=True)
            + post(8, 8, 1.9, 12.2, 13.4, "trim", bottom=False, top=False)
            + post(8, 8, 1.3, 13.4, 14.2, "trim", bottom=False, top=False)
            + post(8, 8, 0.6, 14.2, 14.8, "trim", bottom=False))


def stainless():
    """A brushed stainless sleeve with a shallow domed cap."""
    return (post(8, 8, 2.3, 0, 13.5, "body", top=False)
            + post(8, 8, 2.0, 13.5, 14.1, "body", bottom=False, top=False)
            + post(8, 8, 1.3, 14.1, 14.5, "body", bottom=False))


def crash_rated():
    """A thick crash-rated steel bollard on its anchor plate, in safety yellow with a
    reflective band."""
    return ([slab([3, 0, 3], [13, 0.5, 13], "plate")]
            + post(8, 8, 3.0, 0.5, 15, "body", top=False, bottom=False)
            + ring(3.05, 12, 13, "band")
            + post(8, 8, 2.6, 15, 15.5, "body", bottom=True))


def pipe_sleeve():
    """A steel pipe bollard under a yellow plastic cover sleeve with two reflective bands."""
    return (post(8, 8, 2.8, 0, 14, "body", top=False)
            + ring(2.85, 10.5, 11.3, "band") + ring(2.85, 12, 12.8, "band")
            + post(8, 8, 2.4, 14, 14.6, "body", bottom=False, top=False)
            + post(8, 8, 1.5, 14.6, 15, "body", bottom=False))


def flexible():
    """A tubular flexible delineator on a bolted base, with two reflective bands."""
    return (post(8, 8, 2.2, 0, 0.5, "base")
            + post(8, 8, 1.1, 0.5, 15, "body", top=False, bottom=False)
            + ring(1.15, 12, 13, "band") + ring(1.15, 13.8, 14.6, "band")
            + post(8, 8, 1.15, 15, 15.3, "body", bottom=False))


def sphere_profile():
    """The ball as lathe points (radius, y) from the ring where it meets the ground to its top."""
    r, cy = SPHERE_RADIUS, SPHERE_CENTRE_Y
    phi0 = math.acos(cy / r)            # polar angle from straight down where y = 0
    out = []
    for k in range(SPHERE_STEPS + 1):
        phi = phi0 + (math.pi - phi0) * k / SPHERE_STEPS
        out.append((0.0 if k == SPHERE_STEPS else r * math.sin(phi), cy - r * math.cos(phi)))
    return out


def sphere_obj(material):
    """The concrete sphere as an OBJ. A JSON model can only step a ball out of octagons, which at
    this size reads as a stack of discs; a lathe on SPHERE_SIDES sides with normals from the
    profile shades as a ball. Written in the pedestal pole's OBJ format, block units."""
    mesh = pole.Mesh()
    profile = sphere_profile()
    pole.lathe(mesh, profile, sides=SPHERE_SIDES, bottom_disc=True,
               v_scale=1.0 / (2 * SPHERE_RADIUS))
    lines = ["# Procedurally generated by dev-env-utils/scripts/gen_streetscape_bollards.py"
             " -- do not hand edit",
             "mtllib bollard_sphere.mtl",
             "o bollard_sphere"]
    for p in mesh.v:
        lines.append("v %.6f %.6f %.6f" % (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0))
    for t in mesh.vt:
        lines.append("vt %.6f %.6f" % t)
    for n in mesh.vn:
        lines.append("vn %.6f %.6f %.6f" % n)
    lines.append("usemtl %s" % material)
    for tri in mesh.f:
        lines.append("f " + " ".join("%d/%d/%d" % i for i in tri))
    return "\n".join(lines) + "\n"


def sphere_bounds():
    r = SPHERE_RADIUS
    top = SPHERE_CENTRE_Y + r
    return "new AxisAlignedBB(%.6f, 0.000000, %.6f, %.6f, %.6f, %.6f)" % (
        (8 - r) / 16, (8 - r) / 16, (8 + r) / 16, top / 16, (8 + r) / 16)


# ------------------------------------------------------------------------------------------
# Catalogue
# ------------------------------------------------------------------------------------------
def add_sphere(registry, names, texture):
    """The concrete sphere bollard: an OBJ, retextured per colour through the blockstate."""
    material = "sphere"
    C.extra["models/block/%s/bollard_sphere.obj" % C.model_dir] = sphere_obj(material)
    C.extra["models/block/%s/bollard_sphere.mtl" % C.model_dir] = (
        "# Procedurally generated by gen_streetscape_bollards.py -- do not hand edit\n"
        "newmtl %s\nmap_Kd %s\n" % (material, C.T(texture)))
    state = {"forge_marker": 1,
             "defaults": {"model": "csm:%s/bollard_sphere.obj" % C.model_dir,
                          "custom": {"flip-v": True},
                          "textures": {"#" + material: C.T(texture), "particle": C.T(texture)}},
             "variants": {"facing": {"north": {}, "east": {"y": 90}, "south": {"y": 180},
                                     "west": {"y": 270}},
                          "inventory": [{"transform": "forge:default-block"}]}}
    java = ('new BlockUtilityBox("%s", new UtilityBoxSpec(1, 1, 1,\n        %s,\n        null))'
            % (registry, sphere_bounds()))
    C.add(registry, java, names, {}, state, tab=TAB)


def add_bollard(registry, names, els, textures, flexible_post=False):
    model = gu.model_for_catalogue(C, textures, els)
    state = lc.facing_state(C.M(registry))
    cls = "BlockBollardFlexible" if flexible_post else "BlockUtilityBox"
    java = 'new %s("%s", %s)' % (cls, registry, gu.spec_java(els, (1, 1, 1), None))
    C.add(registry, java, names, {registry: model}, state, tab=TAB)


def bollards():
    add_bollard("bollard_cast_iron_black",
                ("Cast-Iron Bollard (Black)", "Gusseisenpoller (Schwarz)",
                 "Bolardo de Hierro Fundido (Negro)", "Gjutjärnspollare (Svart)"),
                cast_iron(), {"body": "iron_black", "trim": "black"})
    add_bollard("bollard_cast_iron_green",
                ("Cast-Iron Bollard (Dark Green)", "Gusseisenpoller (Dunkelgrün)",
                 "Bolardo de Hierro Fundido (Verde Oscuro)", "Gjutjärnspollare (Mörkgrön)"),
                cast_iron(), {"body": "iron_green", "trim": "green"})
    add_bollard("bollard_stainless",
                ("Stainless Steel Bollard", "Edelstahlpoller", "Bolardo de Acero Inoxidable",
                 "Rostfri Pollare"), stainless(), {"body": "stainless"})
    add_bollard("bollard_crash_rated",
                ("Crash-Rated Bollard", "Anprallsicherer Poller", "Bolardo Antichoque",
                 "Påkörningsskyddad Pollare"), crash_rated(),
                {"body": "yellow", "plate": "plate", "band": "reflective"})
    add_bollard("bollard_pipe_sleeve",
                ("Pipe Bollard (Yellow Sleeve)", "Rohrpoller (Gelbe Hülle)",
                 "Bolardo de Tubo (Funda Amarilla)", "Rörpollare (Gul Hylsa)"), pipe_sleeve(),
                {"body": "yellow", "band": "reflective"})
    add_bollard("bollard_flexible_white",
                ("Flexible Delineator (White)", "Flexibler Leitpfosten (Weiß)",
                 "Delineador Flexible (Blanco)", "Flexibel Markeringsstolpe (Vit)"), flexible(),
                {"body": "white", "base": "black", "band": "reflective"}, flexible_post=True)
    add_bollard("bollard_flexible_yellow",
                ("Flexible Delineator (Yellow)", "Flexibler Leitpfosten (Gelb)",
                 "Delineador Flexible (Amarillo)", "Flexibel Markeringsstolpe (Gul)"), flexible(),
                {"body": "yellow", "base": "black", "band": "reflective"}, flexible_post=True)
    # The big painted ball outside a store's doors (the user's reference, 2026-09-23).
    add_sphere("bollard_sphere_red",
               ("Concrete Sphere Bollard (Red)", "Betonkugelpoller (Rot)",
                "Bolardo Esférico de Hormigón (Rojo)", "Betongklotpollare (Röd)"), "sphere_red")


bollards()

if __name__ == "__main__":
    sys.exit(C.main())
