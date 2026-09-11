#!/usr/bin/env python3
"""
gen_guardrails.py -- the W-beam guardrail RAIL blocks for the Traffic Accessories tab.

Four sibling blocks -- steel or timber post, one rail or two -- that join into runs, take all eight
facings, and ramp with the grade. Everything a rail block draws is generated here: the OBJ models,
the swatch textures, the blockstates, and the lang and tab fragments. The END TREATMENTS are a
separate generator (``gen_guardrail_ends.py``); the two meet through ``guardrail_geometry.py``,
which is the only place any dimension is written down.

HOW A SEGMENT IS PUT TOGETHER
-----------------------------
Exactly the machinery the work zone barriers use, so the primitives are imported from
``gen_work_zone_devices`` rather than copied:

  * The rail is a CLOSED cross-section (the W face from ``W_PROFILE``, backed flat at
    ``RAIL_BACK_Z``) swept along X for the length of the cell -- ``swept_wall`` with a profile that
    runs up the rail's face instead of a half width.
  * The post and its BLOCK-OUT are a detachable piece, drawn only where ``post`` is true. The
    block-out is what holds the rail off the post and is plainly visible from behind; without it
    this is a plank on a stick.
  * The open ends are capped only where nothing connects (``connectleft`` / ``connectright``), and
    a diagonal joint grows a filler off the RIGHT-hand end only -- both for the reasons
    ``WORK_ZONE_ACCESSORIES.md`` gives: two caps in one plane at a joint, or two fillers in the
    same air, fight down the whole seam.

SLOPES, AND WHY THE FILLER IS LEVEL
-----------------------------------
``up`` rises a full block across the cell toward its right-hand end, and ``down`` falls a full block
across it from its left-hand end, so either way a run changing level by one block per cell is
continuous. Both are drawn in the LOWER of the two cells -- see ``guardrail_geometry``'s slope
section for why a ramp drawn in the higher one disappears into the block under it. The DIAGONAL
filler bridges the extra ``DIAGONAL_GAP`` between two cells that meet at a corner -- and across that
gap there is no rise left to make, because the core already made all of it inside its own cell. So
``fill_up`` is the flat filler carried at the top of the rise, and ``fill_down``, whose right-hand
end is back at the cell's own level, is the same shape as ``fill_flat``.

THE ONE BLOCKSTATE TRICK WORTH KNOWING
--------------------------------------
The post, the right-hand cap and the filler all have to be selected by TWO properties at once: a
boolean that says whether to draw them, and ``slope``, which says which of the three shapes.
Forge merges each property's variant independently, so this looks impossible -- but the merge has
a defined order and a defined blocker. ``ForgeBlockStateV1.getPermutations`` sorts the property
names ALPHABETICALLY and merges earlier into later with ``sync``, which only fills values that are
not already set, so the alphabetically FIRST property wins; and a submodel declared as ``null``
occupies its key (blocking anything later) and is then dropped before baking.

``connectright``, ``diagfill`` and ``post`` all sort before ``slope``. So ``slope`` carries all
three shapes, and each boolean nulls out the key it owns when its piece should not be drawn. See
the comment on ``blockstate_json``.

Usage:
    python gen_guardrails.py                 # writes into the repo tree
    python gen_guardrails.py --scratch       # writes into _guardrail_out/ instead

Requires Pillow.
"""
import argparse
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout              # noqa: E402
import guardrail_geometry as geo         # noqa: E402
from gen_work_zone_devices import (      # noqa: E402
    Mesh, box, check_uvs, draw_swatches, facing_variants, java_bbox, mesh_bounds, set_v_span,
    uv_swatch, SWATCH_ACCENT_V, SWATCH_BAND_V, SWATCH_BASE_V, TEX_SIZE,
)

TRAFFICACCESSORIES_OWNER = layout.owner_of_folder("trafficaccessories")
MODEL_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                       "models/block/" + geo.MODEL_SUBDIR)
TEXTURE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER,
                                         "textures/blocks/trafficaccessories/guardrail")
BLOCKSTATE_DIR = layout.asset_dir_for_write(TRAFFICACCESSORIES_OWNER, "blockstates")
SCRATCH_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_guardrail_out")

MODEL_PREFIX = "csm:%s" % geo.MODEL_SUBDIR
JAVA_CLASS = "BlockGuardrail"

# Where the post stands: the middle of the cell, across the run.
CX = geo.CELL * 0.5

# Wall thickness of the C-section post. Thin enough to read as rolled section, thick enough that
# the channel is still two texels of shading rather than a crack.
POST_WALL = 0.60

# Which flat swatch each part wears. Nothing on a guardrail carries a band, so every face reads a
# swatch and none reads the height strip -- which is also why no UV here can leave 0-1.
RAIL_SWATCH = SWATCH_BASE_V        # galvanised steel, on both broad faces of the rail
POST_SWATCH = SWATCH_BAND_V        # the post: galvanised, or timber on the wooden variants
BLOCKOUT_SWATCH = SWATCH_ACCENT_V  # the spacer between rail and post


# --- the catalogue --------------------------------------------------------------------------------
def _block(registry, stem, texture, wood, double, display):
    return (registry, {"stem": stem, "texture": texture, "wood": wood, "double": double,
                       "display": display})


BLOCKS = dict([
    _block(geo.RAIL_BLOCKS[0], "guardrail_w_beam", "guardrail_w_beam_steel",
           False, False, "W-Beam Guardrail"),
    _block(geo.RAIL_BLOCKS[1], "guardrail_w_beam_wood", "guardrail_w_beam_timber",
           True, False, "W-Beam Guardrail (Wood Post)"),
    _block(geo.RAIL_BLOCKS[2], "guardrail_w_beam_double", "guardrail_w_beam_steel",
           False, True, "W-Beam Guardrail (Double Sided)"),
    _block(geo.RAIL_BLOCKS[3], "guardrail_w_beam_wood_double", "guardrail_w_beam_timber",
           True, True, "W-Beam Guardrail (Wood Post, Double Sided)"),
])


# --- the rail -------------------------------------------------------------------------------------
def _ccw(section):
    """Wind a closed (z, y) section counter-clockwise, so one outward-normal rule serves it all."""
    area = 0.0
    for (z0, y0), (z1, y1) in zip(section, section[1:] + section[:1]):
        area += z0 * y1 - z1 * y0
    return section if area > 0.0 else list(reversed(section))


def rail_section(mirror=False):
    """The rail's closed cross-section in (z, y): the W face, backed flat at ``RAIL_BACK_Z``.

    ``W_PROFILE`` is a polyline up the FACE, so it is open at both ends; the two points added here
    close it against the rail's own back. A closed section sweeps into a solid, which is what makes
    the ends a plain cut rather than a hole into a shell.

    ``mirror`` reflects it about the post's centre line for the far rail of a double-sided run,
    which is why the near rail sits at the same z whichever variant is placed.
    """
    pts = [(geo.RAIL_FRONT_Z + depth, y) for (depth, y) in geo.W_PROFILE]
    pts.append((geo.RAIL_BACK_Z, geo.RAIL_TOP_Y))
    pts.append((geo.RAIL_BACK_Z, geo.RAIL_BOTTOM_Y))
    if mirror:
        pts = [(geo.CELL - z, y) for (z, y) in pts]
    return _ccw(pts)


def sweep_rail(mesh, mirror, x0, x1, lift, grade):
    """Sweep the rail section from ``x0`` to ``x1``, lifting by ``grade`` per unit of run.

    The section is translated in y as it advances, so every quad stays a parallelogram and stays
    planar. The face normal is worked out from the section edge and the grade rather than named:
    on a slope the top of the rail tilts back, and a normal that ignored the grade would light a
    ramp as though it were level.
    """
    section = rail_section(mirror)
    t = uv_swatch(RAIL_SWATCH)
    lift_a = lift
    lift_b = lift + grade * (x1 - x0)
    for (z0, y0), (z1, y1) in zip(section, section[1:] + section[:1]):
        dz, dy = z1 - z0, y1 - y0
        if abs(dz) < 1e-9 and abs(dy) < 1e-9:
            continue
        normal = (grade * dz, -dz, dy)
        mesh.quad_out([(x0, y0 + lift_a, z0), (x1, y0 + lift_b, z0),
                       (x1, y1 + lift_b, z1), (x0, y1 + lift_a, z1)],
                      normal, [t, t, t, t])


def rail_cap(mesh, mirror, x, lift, outward):
    """The plain cut over one open end of the rail.

    Drawn as a strip of trapezoids between the W face and the rail's back rather than as a fan:
    the section is not convex -- that is what the valley at mid height means -- and a fan across a
    concave section puts triangles outside the shape and on top of each other.
    """
    t = uv_swatch(RAIL_SWATCH)
    normal = (outward, 0.0, 0.0)
    back = geo.CELL - geo.RAIL_BACK_Z if mirror else geo.RAIL_BACK_Z

    def face_z(depth):
        z = geo.RAIL_FRONT_Z + depth
        return geo.CELL - z if mirror else z

    for (d0, y0), (d1, y1) in zip(geo.W_PROFILE, geo.W_PROFILE[1:]):
        if abs(y1 - y0) < 1e-9:
            continue
        mesh.quad_out([(x, y0 + lift, face_z(d0)), (x, y1 + lift, face_z(d1)),
                       (x, y1 + lift, back), (x, y0 + lift, back)],
                      normal, [t, t, t, t])


def rail_sides(double):
    """Which rails a variant carries: the near one, and on a double-sided run the far one too."""
    return (False, True) if double else (False,)


# --- the post and its block-out ---------------------------------------------------------------------
def build_post(mesh, wood, double, slope):
    """Post plus block-out, at the height the rail passes the middle of the cell.

    The post stands mid-cell, so on a sloped segment it meets the rail ``POST_LIFT`` of the way up
    the rise and has to be that much taller. A ramp is always drawn in the lower of its two cells,
    whichever way it runs, so the rail is above the cell there and the post always reaches UP to it
    from the cell floor.
    """
    lift = geo.slope_lift(slope, geo.POST_LIFT)
    foot = min(geo.POST_BOTTOM_Y, lift)
    half = geo.BLOCKOUT_HALF_X
    y0, y1 = geo.BLOCKOUT_Y0 + lift, geo.BLOCKOUT_Y1 + lift

    # The block-out's back is buried in the post's front, so that face is left off rather than
    # drawn inside it. Its front is only mostly covered by the rail -- the block-out stands a
    # little proud top and bottom -- so that one stays.
    box(mesh, CX - half, CX + half, y0, y1, geo.BLOCKOUT_Z0, geo.BLOCKOUT_Z1, BLOCKOUT_SWATCH,
        faces=("x-", "x+", "y-", "y+", "z-"))
    if double:
        back_faces = ["x-", "x+", "y-", "y+", "z+"]
        if not wood:
            # A C-section is open at the back, so nothing covers the far block-out's inner face.
            back_faces.append("z-")
        box(mesh, CX - half, CX + half, y0, y1,
            geo.BACK_BLOCKOUT_Z0, geo.BACK_BLOCKOUT_Z1, BLOCKOUT_SWATCH, faces=tuple(back_faces))

    top = geo.POST_TOP_Y + lift
    if wood:
        # Sawn timber: a plain square section, which is what a wooden post is.
        box(mesh, CX - geo.POST_HALF_X, CX + geo.POST_HALF_X, foot, top,
            geo.POST_Z0, geo.POST_Z1, POST_SWATCH)
        return

    # Rolled steel: a C-section, web toward the rail, flanges reaching back. Three solids that
    # TOUCH rather than overlap -- an overlap would put two faces of one plane in the same place
    # and z-fight -- and the flanges leave off the face buried in the web.
    web_z1 = geo.POST_Z0 + POST_WALL
    box(mesh, CX - geo.POST_HALF_X, CX + geo.POST_HALF_X, foot, top,
        geo.POST_Z0, web_z1, POST_SWATCH)
    for sign in (-1.0, 1.0):
        outer = CX + sign * geo.POST_HALF_X
        inner = outer - sign * POST_WALL
        box(mesh, min(outer, inner), max(outer, inner), foot, top,
            web_z1, geo.POST_Z1, POST_SWATCH, faces=("x-", "x+", "y-", "y+", "z+"))


# --- the pieces -------------------------------------------------------------------------------------
def build_core(mesh, double, slope):
    lift, grade = geo.slope_lift(slope, 0.0), geo.slope_grade(slope)
    for mirror in rail_sides(double):
        sweep_rail(mesh, mirror, 0.0, geo.CELL, lift, grade)


def build_end(mesh, double, slope, left):
    """The cap over an open end, at whatever height the core reaches there.

    Only the right-hand cap needs one model per slope. The left-hand end is off the floor only on
    ``down``, and a cell only slopes down because its LEFT neighbour joins it a block up -- so a
    ``down`` cell's left end is never open and its cap is never drawn."""
    lift = geo.slope_lift(slope, 0.0 if left else 1.0)
    x = 0.0 if left else geo.CELL
    for mirror in rail_sides(double):
        rail_cap(mesh, mirror, x, lift, -1.0 if left else 1.0)


def build_fill(mesh, double, slope):
    """The length of rail that closes the gap to a DIAGONAL neighbour, off the right-hand end.

    Level, at whatever height the core's right-hand end reached: the neighbour's cell is a block up
    for ``up`` and level for ``down``, and its own core starts at its own ``RAIL_BOTTOM_Y``, which is
    exactly where this one stopped. All the rise happens inside a cell; none of it is left for the
    joint.
    """
    lift = geo.slope_lift(slope, 1.0)
    for mirror in rail_sides(double):
        sweep_rail(mesh, mirror, geo.CELL, geo.CELL + geo.DIAGONAL_GAP, lift, 0.0)


def build_inventory(mesh, wood, double):
    """Everything but the filler: what one segment looks like standing on its own.

    The filler is left out for the reason the work zone barriers leave it out -- it would put a
    stub of rail out of the icon, and the bounding box taken from these bounds would claim the
    cell beside the block.
    """
    build_core(mesh, double, "flat")
    build_post(mesh, wood, double, "flat")
    build_end(mesh, double, "flat", True)
    build_end(mesh, double, "flat", False)


def model_pieces(spec):
    """suffix -> builder, for every model one rail block ships."""
    wood, double = spec["wood"], spec["double"]
    pieces = {
        "core": lambda m: build_core(m, double, "flat"),
        "end_left": lambda m: build_end(m, double, "flat", True),
    }
    for slope in geo.SLOPES:
        if slope != "flat":
            pieces["core_%s" % slope] = (lambda s: lambda m: build_core(m, double, s))(slope)
        pieces["post_%s" % slope] = (lambda s: lambda m: build_post(m, wood, double, s))(slope)
        pieces["fill_%s" % slope] = (lambda s: lambda m: build_fill(m, double, s))(slope)
        pieces["end_right_%s" % slope] = (
            (lambda s: lambda m: build_end(m, double, s, False))(slope))
    return pieces


# --- textures ---------------------------------------------------------------------------------------
def guardrail_texture(post_color, blockout_color):
    """The swatch sheet one rail block wears.

    Every face on a guardrail reads a flat swatch: there is no band to place, and Minecraft's own
    per-face shading is what gives the corrugation its relief. The body of the image is the rail's
    galvanised grey so the break particles come out steel-coloured.

    ``draw_swatches`` paints the column at the FLIPPED rows ``flip-v`` needs. Drawing it the
    natural way up hands every face its neighbour's colour, silently.
    """
    img = Image.new("RGBA", (TEX_SIZE, TEX_SIZE), geo.GALVANISED + (255,))
    draw_swatches(img, geo.GALVANISED, post_color, blockout_color)
    return img


TEXTURES = {
    "guardrail_w_beam_steel": lambda: guardrail_texture(geo.GALVANISED_DARK, geo.GALVANISED_DARK),
    "guardrail_w_beam_timber": lambda: guardrail_texture(geo.TIMBER, geo.TIMBER_DARK),
}


# --- output -----------------------------------------------------------------------------------------
def write_model(mesh, path, name, mtl_file):
    """Write one OBJ, with the attribution pointing at THIS generator.

    ``Mesh.write`` stamps the work zone script's name into the header, which is where the class
    lives; a model that says it came from a generator that does not build it sends the next person
    to the wrong file.
    """
    check_uvs(mesh, name)
    mesh.write(path, name, mtl_file)
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    text = text.replace("gen_work_zone_devices.py", "gen_guardrails.py", 1)
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        fh.write(text)


def write_mtl(path, texture_name):
    """The one material every variant retextures. Not ``gen_work_zone_devices.write_mtl``: that
    one bakes the work zone texture folder into the path, and the guardrails have their own."""
    with open(path, "w", newline="\n") as fh:
        fh.write("# Procedurally generated by dev-env-utils/scripts/gen_guardrails.py"
                 " -- do not hand edit\n")
        fh.write("# One material; the steel and timber variants retexture #%s.\n" % geo.MATERIAL)
        fh.write("newmtl %s\n" % geo.MATERIAL)
        fh.write("map_Kd %s/%s\n" % (geo.TEXTURE_PREFIX, texture_name))


def blockstate_json(spec):
    """The Forge blockstate for one rail block.

    Forge merges one property's variant at a time, alphabetically, and the first one to set a
    value keeps it (``ForgeBlockStateV1.getPermutations`` -> ``Variant.sync``). Three of the pieces
    have to answer to two properties at once, and this is how:

      * ``slope`` -- which sorts LAST of the six -- carries the post, the right-hand cap and the
        filler, each in the shape that slope needs, and overrides the core model for ``up`` and
        ``down``.
      * ``post``, ``connectright`` and ``diagfill`` -- all of which sort before it -- declare their
        submodel key as ``null`` on the side where the piece must NOT be drawn. A null submodel
        occupies the key, so ``slope``'s own entry for it is never merged in, and is then dropped
        before anything is baked (``ForgeBlockStateV1`` line ``submodels.values().removeIf``).

    ``connectleft`` needs none of that: the left-hand end of a segment is only off the floor on
    ``down``, and a ``down`` segment is always joined on its left, so one cap serves all three.
    """
    stem = spec["stem"]
    model = "%s/%s" % (MODEL_PREFIX, stem)
    texture = "%s/%s" % (geo.TEXTURE_PREFIX, spec["texture"])

    def piece(suffix):
        return {"model": "%s_%s.obj" % (model, suffix),
                "custom": {"flip-v": True},
                "textures": {"#%s" % geo.MATERIAL: texture}}

    def submodel(**pieces):
        return {"submodel": {key: piece(suffix) for key, suffix in pieces.items()}}

    slope_variants = {}
    for slope in geo.SLOPES:
        # `flat` inherits the core model from the defaults; the other two replace it.
        variant = {} if slope == "flat" else {"model": "%s_core_%s.obj" % (model, slope)}
        variant.update(submodel(post="post_%s" % slope, fill="fill_%s" % slope,
                                end_right="end_right_%s" % slope))
        slope_variants[slope] = variant

    variants = {"facing": facing_variants({"diagonal": True})}
    # The end caps are on the FALSE side: a piece is drawn where nothing connects.
    variants["connectleft"] = {"false": submodel(end_left="end_left"), "true": {}}
    variants["connectright"] = {"false": {}, "true": {"submodel": {"end_right": None}}}
    # The post and the diagonal filler are the other way round: drawn where the property is true.
    variants["post"] = {"false": {"submodel": {"post": None}}, "true": {}}
    variants["diagfill"] = {"false": {"submodel": {"fill": None}}, "true": {}}
    variants["slope"] = slope_variants
    variants["normal"] = [{}]
    variants["inventory"] = [{"model": "%s_inv.obj" % model,
                              "custom": {"flip-v": True},
                              "textures": {"#%s" % geo.MATERIAL: texture},
                              "transform": "forge:default-block"}]
    return {
        "forge_marker": 1,
        "defaults": {
            "model": "%s_core.obj" % model,
            "custom": {"flip-v": True},
            "textures": {"#%s" % geo.MATERIAL: texture},
        },
        "variants": variants,
    }


def generate(model_dir, texture_dir, blockstate_dir, fragment_dir):
    for d in (model_dir, texture_dir, blockstate_dir, fragment_dir):
        os.makedirs(d, exist_ok=True)
    set_v_span(geo.CELL)
    written = []
    bounds = {}

    for texture, build in TEXTURES.items():
        path = os.path.join(texture_dir, texture + ".png")
        build().save(path)
        written.append(path)

    for registry, spec in BLOCKS.items():
        stem = spec["stem"]
        mtl_file = stem + ".mtl"
        for suffix, builder in model_pieces(spec).items():
            mesh = Mesh()
            builder(mesh)
            name = "%s_%s" % (stem, suffix)
            path = os.path.join(model_dir, name + ".obj")
            write_model(mesh, path, name, mtl_file)
            written.append(path)

        inv = Mesh()
        build_inventory(inv, spec["wood"], spec["double"])
        inv_path = os.path.join(model_dir, stem + "_inv.obj")
        write_model(inv, inv_path, stem + "_inv", mtl_file)
        written.append(inv_path)
        bounds[registry] = mesh_bounds(inv)

        mtl_path = os.path.join(model_dir, mtl_file)
        write_mtl(mtl_path, spec["texture"])
        written.append(mtl_path)

        bs_path = os.path.join(blockstate_dir, registry + ".json")
        with open(bs_path, "w", newline="\n") as fh:
            json.dump(blockstate_json(spec), fh, indent=2)
            fh.write("\n")
        written.append(bs_path)

    # Fragments, for pasting into the lang file and the tab: this generator never rewrites a file
    # it does not own.
    lang_path = os.path.join(fragment_dir, "rails_lang.txt")
    with open(lang_path, "w", newline="\n") as fh:
        for registry, spec in BLOCKS.items():
            fh.write("tile.%s.name=%s\n" % (registry, spec["display"]))
    written.append(lang_path)

    tab_path = os.path.join(fragment_dir, "rails_tab.java")
    with open(tab_path, "w", newline="\n") as fh:
        fh.write("// Generated by gen_guardrails.py -- the bounding box is the INVENTORY model's\n")
        fh.write("// own extent, so it follows the geometry and leaves the diagonal filler out:\n")
        fh.write("// a lone rail must not claim the cell beside it.\n")
        for registry in BLOCKS:
            fh.write('initTabBlock(new %s("%s",\n    %s));\n'
                     % (JAVA_CLASS, registry, java_bbox(bounds[registry])))
    written.append(tab_path)

    return written


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--scratch", action="store_true",
                        help="write models, textures and blockstates into _guardrail_out/ too")
    args = parser.parse_args()

    if args.scratch:
        model_dir = os.path.join(SCRATCH_DIR, "models")
        texture_dir = os.path.join(SCRATCH_DIR, "textures")
        blockstate_dir = os.path.join(SCRATCH_DIR, "blockstates")
    else:
        model_dir, texture_dir, blockstate_dir = MODEL_DIR, TEXTURE_DIR, BLOCKSTATE_DIR

    for path in generate(model_dir, texture_dir, blockstate_dir, SCRATCH_DIR):
        print(os.path.relpath(path, layout.REPO_ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
