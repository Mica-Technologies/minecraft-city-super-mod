#!/usr/bin/env python3
"""
gen_pole_finials.py -- the decorative finials that cap a pedestal traffic pole
(``trafficpolefinial``).

A finial is the cast ornament screwed onto the top of a decorative post: a ball, an acorn, an
urn, a spire or a plain flat cap. In CSM it is its OWN BLOCK, sitting in the air block directly
above a pedestal pole's top, rather than a state of the pole:

  * six ornaments is six models, and a pole that carried the choice would have to name the
    property in its blockstate, multiplying its 864 variants by seven for something only the
    top block of a stack ever draws;
  * a block of its own can be placed, broken and picked like anything else, and the pole only
    has to notice that something of ITS family sits above it and end its tube flush, which is a
    rule it already has for another pole (see ``PEDESTAL_POLE_SYSTEM.md``).

Each ornament is one lathe about the block's vertical axis, and each shares the same collar:
a sleeve that reaches DOWN 1.6 units into the pole's own block, over the tube's top, so the
joint reads as one casting rather than two blocks meeting. Nothing reaches above its own block.

The geometry helpers (mesh, lathe, uv, guards) are the pedestal pole's, imported rather than
copied, so both families shade and texture identically and there is one lathe in the tree.

Outputs:

  * ``polefinial_<style>.obj``  -- one lathe per ornament, standing, authored in model space
  * ``polefinial.mtl``          -- one material, ``body``; the blockstate retextures it per colour
  * ``blockstates/trafficpolefinial.json`` -- style picks the model, colour picks the texture
  * ``_finial_out/lang_fragment.txt`` and ``tab_fragment.txt``

Usage:
    python gen_pole_finials.py            # writes models, the blockstate, lang/tab fragments
    python gen_pole_finials.py --check    # regenerate into a temp dir and diff against the tree

Requires nothing beyond the standard library.
"""
import argparse
import json
import math
import os
import shutil
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402
import gen_pedestal_pole as pole  # noqa: E402

TRAFFICACCESSORIES_OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                       "models/block/trafficaccessories/shared_models")
BLOCKSTATE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER, "blockstates")
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_finial_out")

MODEL_PREFIX = "csm:trafficaccessories/shared_models"
MODEL_STEM = "polefinial"
MATERIAL = "body"
MTL_NAME = MODEL_STEM + ".mtl"
REGISTRY_NAME = "trafficpolefinial"

# The pole's five finishes, so a finial can wear the colour of the pole it stands on.
COLORS = pole.COLORS

SIDES = pole.SIDES          # 16, the pole family's silhouette -- every style is drawn on it, so
                            # every collar meets the tube the same way
FLUTES = 8                  # grooves round the urn's belly
FLUTE_DEPTH = 0.45           # how deep a groove cuts into the belly, in 1/16 block
AXIS = pole.AXIS
R_SHAFT = pole.R_SHAFT      # 3: the pedestal tube this all has to sit on

# --- the shapes, in 1/16 block units, y measured from the FLOOR of the finial's own block ------
# Collar: a sleeve over the tube top (which ends 0.01 below y = 0, in the pole's block) and the
# shoulder that closes in to whatever ornament follows. Every style starts with it, so every
# style meets the pole the same way. It reaches below its block on purpose: that is the join.
COLLAR = [(R_SHAFT + 0.05, -1.6), (3.5, -1.15), (3.5, 0.4), (3.3, 0.85), (2.6, 1.2), (1.75, 1.4)]

# A waisted neck with one bead, shared by both balls.
BALL_NECK = [(1.35, 1.7), (1.2, 2.2), (1.4, 2.55), (2.05, 2.7), (2.05, 3.0), (1.45, 3.15)]
BALL_NECK_TOP = 3.25        # where the neck enters a ball

BALL_SMALL_R = 3.0          # 6 across: the tube's own width, a modest post ball
BALL_LARGE_R = 3.9          # 7.8 across, the one that reads from across the street
BALL_STEPS = 12             # latitude steps from the neck ring to the pole of a ball


def ball(radius, neck_radius, neck_y, steps=BALL_STEPS):
    """A sphere sampled by latitude, from the ring where a neck of ``neck_radius`` enters it up
    to its pole. Returned as lathe profile points so it continues the neck with one shared ring
    and no seam."""
    rise = math.sqrt(radius * radius - neck_radius * neck_radius)
    centre_y = neck_y + rise
    phi0 = math.asin(neck_radius / radius)          # polar angle, measured from straight down
    out = []
    for k in range(steps + 1):
        phi = phi0 + (math.pi - phi0) * k / steps
        out.append((0.0 if k == steps else radius * math.sin(phi),
                    centre_y - radius * math.cos(phi)))
    return out


def ball_profile(radius):
    return COLLAR + BALL_NECK + ball(radius, BALL_NECK[-1][0], BALL_NECK_TOP)


# The acorn: the cup first, then the ovoid nut, closing to a blunt tip.
ACORN = COLLAR + [
    (1.5, 1.7), (2.6, 2.1), (3.25, 2.65), (3.45, 3.3),          # cup
    (3.5, 4.3), (3.3, 5.6), (2.85, 6.9), (2.05, 8.2),           # nut
    (1.15, 9.2), (0.5, 9.9), (0.0, 10.5),                       # tip
]

# The urn: a footed vase with a swelling belly, a narrow neck and a flared lip, closed by a low
# lid. Drawn on FLUTES facets rather than SIDES, which is what makes it read as fluted.
URN = COLLAR + [
    (1.6, 1.7), (2.25, 2.0), (2.25, 2.4), (1.75, 2.75),         # foot
    (2.6, 3.4), (3.3, 4.4), (3.45, 5.4), (3.15, 6.4), (2.5, 7.1),  # belly
    (1.75, 7.7), (1.65, 8.3),                                   # neck
    (2.6, 9.1), (2.8, 9.5), (2.5, 9.8),                         # lip
    (1.7, 10.0), (0.0, 10.4),                                   # lid
]

# The spire: a bead, then a long tapering spear point. Also faceted.
SPIRE = COLLAR + [
    (1.6, 1.7), (2.1, 1.95), (2.25, 2.25), (2.0, 2.55),         # bead
    (1.6, 3.1), (1.2, 5.6), (0.8, 8.4), (0.4, 11.0), (0.0, 13.0),  # point
]

# The flat cap: a wide, shallow disc with a bevelled rim. The one that suits a plain pedestrian
# post rather than a decorative one.
DISC = COLLAR[:3] + [
    (4.4, 0.9), (4.75, 1.35), (4.75, 2.3), (4.45, 2.75),        # rim
    (3.5, 3.05), (2.1, 3.3), (0.0, 3.5),                        # crown
]

# Where the urn's flutes run: the belly, fading out before the foot and the neck so the grooves
# die into plain castings rather than ending in a step.
URN_FLUTES = (3.2, 7.1)

# id, profile, the flute band (or None), the name the tool says in chat
STYLES = [
    ("ball_small", ball_profile(BALL_SMALL_R), None, "Small Ball"),
    ("ball_large", ball_profile(BALL_LARGE_R), None, "Large Ball"),
    ("acorn", ACORN, None, "Acorn"),
    ("urn", URN, URN_FLUTES, "Fluted Urn"),
    ("spire", SPIRE, None, "Spire"),
    ("disc", DISC, None, "Flat Cap"),
]

# The display name of the block itself, in the four locales the Roads module ships.
DISPLAY_NAMES = [
    ("en_us", "Pole Finial"),
    ("es_es", "Remate de Poste"),
    ("de_de", "Mast-Zierknauf"),
    ("sv_se", "Stolpknopp"),
]


def fluted_lathe(mesh, profile, band, flutes=FLUTES, depth=FLUTE_DEPTH, sides=SIDES):
    """The pole's lathe with vertical grooves cut into one band of it.

    The grooves are a radius that varies with the angle as well as the height, which the plain
    lathe cannot express: its rings are circles and its normals come from the profile slope
    alone. So this walks the same rings and takes the normal from the surface itself, as the
    cross product of the two partial derivatives, measured numerically. Getting that from the
    surface rather than from the profile is the whole point -- a groove that is only a
    silhouette, shaded as though it were round, does not read as a groove at all.

    ``band`` is the (low, high) height over which the flutes run; the depth eases to nothing at
    each end of it with a smoothstep, so a groove dies into the plain casting instead of ending
    in a step.
    """
    lo_y, hi_y = band
    radii = [(y, r) for (r, y) in profile]

    def base_radius(y):
        """The profile's radius at any height, straight-line between its points."""
        if y <= radii[0][0]:
            return radii[0][1]
        for (ya, ra), (yb, rb) in zip(radii, radii[1:]):
            if ya <= y <= yb:
                return ra if yb <= ya else ra + (rb - ra) * (y - ya) / (yb - ya)
        return radii[-1][1]

    def window(y):
        if y <= lo_y or y >= hi_y:
            return 0.0
        t = (y - lo_y) / (hi_y - lo_y)
        t = min(1.0, 2.0 * min(t, 1.0 - t))       # ramp up over the first tenth, down the last
        return t * t * (3.0 - 2.0 * t)

    def radius(y, angle):
        groove = 0.5 * (1.0 + math.cos(flutes * angle))   # 1 in a groove, 0 on a rib
        return base_radius(y) - depth * window(y) * groove

    def point(y, angle):
        r = radius(y, angle)
        return (AXIS + r * math.cos(angle), y, AXIS + r * math.sin(angle))

    def normal(y, angle):
        eps = 1e-3
        da = [(a - b) / (2 * eps) for a, b in zip(point(y, angle + eps), point(y, angle - eps))]
        dy = [(a - b) / (2 * eps) for a, b in zip(point(y + eps, angle), point(y - eps, angle))]
        n = pole.vcross(tuple(da), tuple(dy))
        radial = (math.cos(angle), 0.0, math.sin(angle))
        return pole.vnorm(n if pole.vdot(n, radial) > 0 else (-n[0], -n[1], -n[2]))

    angles = pole.ring_angles(sides)
    for k in range(len(profile) - 1):
        (ra, ya), (rb, yb) = profile[k], profile[k + 1]
        if abs(ra) < 1e-9 and abs(rb) < 1e-9:
            continue
        va, vb = ya / 16.0, yb / 16.0
        for i in range(sides):
            j = (i + 1) % sides
            ua, ub = i / sides, (i + 1) / sides
            ai, aj = angles[i], angles[j]
            if rb < 1e-9:        # cone to a point at the top
                mesh.tri([(point(ya, ai), pole.uv(ua, va), normal(ya, ai)),
                          (point(ya, aj), pole.uv(ub, va), normal(ya, aj)),
                          ((AXIS, yb, AXIS), pole.uv(0.5 * (ua + ub), vb), normal(ya, ai))])
            elif ra < 1e-9:      # cone from a point at the bottom
                mesh.tri([((AXIS, ya, AXIS), pole.uv(0.5 * (ua + ub), va), normal(yb, ai)),
                          (point(yb, aj), pole.uv(ub, vb), normal(yb, aj)),
                          (point(yb, ai), pole.uv(ua, vb), normal(yb, ai))])
            else:
                mesh.quad((point(ya, ai), pole.uv(ua, va), normal(ya, ai)),
                          (point(ya, aj), pole.uv(ub, va), normal(ya, aj)),
                          (point(yb, aj), pole.uv(ub, vb), normal(yb, aj)),
                          (point(yb, ai), pole.uv(ua, vb), normal(yb, ai)))


def build(profile, band):
    mesh = pole.Mesh()
    if band is None:
        pole.lathe(mesh, profile, sides=SIDES)
    else:
        fluted_lathe(mesh, profile, band)
    return mesh


def write_obj(mesh, path, name, header):
    """The pedestal pole's OBJ format, written here so the file names its OWN material library
    rather than the pole's. Coordinates go out in block units, as the pole's writer does."""
    lines = ["# Procedurally generated by dev-env-utils/scripts/gen_pole_finials.py"
             " -- do not hand edit",
             "# " + header,
             "mtllib %s" % MTL_NAME,
             "o %s" % name]
    for p in mesh.v:
        lines.append("v %.6f %.6f %.6f" % (p[0] / 16.0, p[1] / 16.0, p[2] / 16.0))
    for t in mesh.vt:
        lines.append("vt %.6f %.6f" % t)
    for n in mesh.vn:
        lines.append("vn %.6f %.6f %.6f" % n)
    lines.append("usemtl %s" % MATERIAL)
    for tri in mesh.f:
        lines.append("f " + " ".join("%d/%d/%d" % i for i in tri))
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")


def model_ref(style_id):
    return "%s/%s_%s.obj" % (MODEL_PREFIX, MODEL_STEM, style_id)


def blockstate():
    """Forge per-property blockstate: ``style`` picks the ornament's model, ``color`` paints it
    with the finish of the pole below (an actual-state property the block resolves per frame).
    One property picking the model while another overrides textures is the RRFB's arrangement;
    two properties both setting ``model`` would fight, which is why colour is a texture swap."""
    silver = COLORS[0][1]
    return {
        "forge_marker": 1,
        "defaults": {
            "model": model_ref(STYLES[0][0]),
            "custom": {"flip-v": True},
            "textures": {"#" + MATERIAL: silver, "particle": silver},
        },
        "variants": {
            "inventory": [{"transform": "forge:default-block"}],
            "style": {s[0]: {"model": model_ref(s[0])} for s in STYLES},
            "color": {c[0]: {"textures": {"#" + MATERIAL: c[1], "particle": c[1]}}
                      for c in COLORS},
        },
    }


def generate(model_dir, blockstate_dir, scratch_dir):
    os.makedirs(model_dir, exist_ok=True)
    os.makedirs(blockstate_dir, exist_ok=True)
    os.makedirs(scratch_dir, exist_ok=True)

    with open(os.path.join(model_dir, MTL_NAME), "w", newline="\n") as fh:
        fh.write("# Procedurally generated by gen_pole_finials.py -- do not hand edit\n"
                 "# One material; the blockstate retextures #%s per pole colour.\n"
                 "newmtl %s\nmap_Kd %s\n" % (MATERIAL, MATERIAL, COLORS[0][1]))

    for style_id, profile, band, label in STYLES:
        mesh = build(profile, band)
        lo, hi = pole.mesh_bounds(mesh)
        # x and z stay inside the block; y may reach 1.6 BELOW it, into the pole's own block,
        # which is where the collar sleeves over the tube top.
        assert all(-0.02 <= c <= 16.02 for c in (lo[0], lo[2], hi[0], hi[2])), (style_id, lo, hi)
        assert -1.65 <= lo[1] and hi[1] <= 16.02, (style_id, lo, hi)
        write_obj(mesh, os.path.join(model_dir, "%s_%s.obj" % (MODEL_STEM, style_id)),
                  "%s_%s" % (MODEL_STEM, style_id),
                  "%s finial: collar sleeved over the pole top, ornament above" % label)

    with open(os.path.join(blockstate_dir, REGISTRY_NAME + ".json"), "w", newline="\n") as fh:
        json.dump(blockstate(), fh, indent=2)
        fh.write("\n")

    with open(os.path.join(scratch_dir, "lang_fragment.txt"), "w", newline="\n") as fh:
        for locale, name in DISPLAY_NAMES:
            fh.write("%s: tile.%s.name=%s\n" % (locale, REGISTRY_NAME, name))
    with open(os.path.join(scratch_dir, "tab_fragment.txt"), "w", newline="\n") as fh:
        fh.write("    initTabBlock(BlockTrafficPoleFinial.class, fmlPreInitializationEvent);"
                 " // Pole Finial\n")
    return [s[0] for s in STYLES]


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true",
                    help="regenerate into a temp dir and fail if the tree differs")
    args = ap.parse_args()

    if not args.check:
        styles = generate(MODEL_DIR, BLOCKSTATE_DIR, SCRATCH_DIR)
        print("wrote %d finial models + %s to %s" % (len(styles), MTL_NAME, MODEL_DIR))
        print("wrote %s.json to %s" % (REGISTRY_NAME, BLOCKSTATE_DIR))
        print("lang/tab fragments in %s" % SCRATCH_DIR)
        return 0

    tmp = tempfile.mkdtemp(prefix="finial_")
    try:
        styles = generate(os.path.join(tmp, "m"), os.path.join(tmp, "b"), os.path.join(tmp, "s"))
        stale = []
        for name in ["%s_%s.obj" % (MODEL_STEM, s) for s in styles] + [MTL_NAME]:
            if not layout.same_generated_text(os.path.join(tmp, "m", name),
                                              os.path.join(MODEL_DIR, name)):
                stale.append(name)
        fn = REGISTRY_NAME + ".json"
        if not layout.same_generated_text(os.path.join(tmp, "b", fn),
                                          os.path.join(BLOCKSTATE_DIR, fn)):
            stale.append(fn)
        if stale:
            print("out of date (re-run without --check):\n  " + "\n  ".join(stale))
            return 1
        print("pole finial assets are up to date")
        return 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
