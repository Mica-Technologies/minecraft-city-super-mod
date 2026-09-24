#!/usr/bin/env python3
"""Every asset the connectable dry standpipe ships, in the Fire Protection tab: free-standing and
wall-run pipe in two sizes and two finishes, and four fittings that sit in a wall run.

    python dev-env-utils/scripts/gen_standpipe_system.py
    python dev-env-utils/scripts/gen_standpipe_system.py --check
    python dev-env-utils/scripts/gen_standpipe_system.py --fragments   # tab lines to paste

BlockStandpipePipe is centred in its block and joins on all six sides; BlockStandpipeWallPipe
runs along the wall behind it, its axis at z = 11 facing north, joining up, down and sideways
and, through its front, a free-standing pipe in front of it. Both are multipart blockstates:
one arm model per side, picked by the actual-state property for that side, and the cast joint
where the run is not straight. A fitting is a wall pipe whose body is drawn in place of the
joint. Every round part is an exact octagon (life_safety_gen_common._octagon), and each arm is
capped at both ends, so where a main meets a branch the step between them is closed.

Above or below, a solid face counts as a join: the arm runs into it and a steel collar plate is
drawn on the face, so a riser reads as going through the floor to the next storey (the user's
request, 2026-09-23).

A plain wall pipe with a wall pipe on the wall at right angles behind it (an outside corner) or
in front of it (an inside corner) turns the corner: see wall_corner.

The main is 8 px across, the branch 6 px. The Standpipe Riser keeps its registry name as the red
wall branch pipe (the user's choice, 2026-09-23), so risers already placed join runs.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import life_safety_gen_common as lc  # noqa: E402

C = lc.Catalogue("gen_standpipe_system.py", "lifesafety/standpipe", "lifesafety/standpipe")
TAB = "CsmTabFireProtection"
octagon = lc._octagon


def FP(name):
    """A Fire Protection texture, shared rather than copied."""
    return "csm:blocks/lifesafety/fireprotection/%s" % name


GALVANISED = (168, 172, 170)
AIR_GREEN = (46, 104, 92)


@C.texture("galvanised")
def galvanised():
    # No baked shading: an arm's faces take a half-block of texture each, so shading drawn
    # across the texture repeats along the pipe as bands. The octagon's faces shade themselves.
    return lc.fill(GALVANISED, 16, 5, 71)


@C.texture("air_green")
def air_green():
    return lc.fill(AIR_GREEN, 16, 4, 72)


FINISHES = {
    # key: (pipe texture, names en/de/es-fem/es-masc/sv)
    "red": (FP("red"), ("Red", "Rot", "Roja", "Rojo", "Röd")),
    "silver": (C.T("galvanised"), ("Silver", "Silber", "Plateada", "Plateado", "Silverfärgad")),
}
SIZES = {"main": 4.0, "branch": 3.0}
AXIS_Z = 11.0
JOINT_GROW = 0.8


# ------------------------------------------------------------------------------------------
# Arms and joints
# ------------------------------------------------------------------------------------------
def centred_arm(side, r):
    return {
        "north": octagon("z", 8, 8, r, 0, 8, "p", True),
        "south": octagon("z", 8, 8, r, 8, 16, "p", True),
        "west": octagon("x", 8, 8, r, 0, 8, "p", True),
        "east": octagon("x", 8, 8, r, 8, 16, "p", True),
        "down": octagon("y", 8, 8, r, 0, 8, "p", True),
        "up": octagon("y", 8, 8, r, 8, 16, "p", True),
    }[side]


def centred_joint(r):
    j = r + JOINT_GROW
    return octagon("y", 8, 8, j, 8 - j, 8 + j, "p", True)


def wall_arm(side, r):
    """Facing north: left is east (+x), the front north (-z)."""
    return {
        "up": octagon("y", 8, AXIS_Z, r, 8, 16, "p", True),
        "down": octagon("y", 8, AXIS_Z, r, 0, 8, "p", True),
        "left": octagon("x", 8, AXIS_Z, r, 8, 16, "p", True),
        "right": octagon("x", 8, AXIS_Z, r, 0, 8, "p", True),
        "front": octagon("z", 8, 8, r, 0, AXIS_Z, "p", True),
    }[side]


def wall_joint(r):
    j = r + JOINT_GROW
    return octagon("y", 8, AXIS_Z, j, 8 - j, 8 + j, "p", True)


# Round a building corner the other face's run crosses this pipe's axis at x = 5 or 11 (facing
# north), where its own axis is set back to its own wall, so a corner piece draws both arms to an
# elbow there. Outer: the side arm on the elbow's side and an arm to the back; inner: the side
# arm from the other side and an arm to the front. The pipe on the other face uses its usual arm.
CORNERS = ("outer_right", "outer_left", "inner_right", "inner_left")


def wall_corner(kind, r):
    e = 16 - AXIS_Z if kind.endswith("right") else AXIS_Z
    outer = kind.startswith("outer")
    to_right = outer == kind.endswith("right")
    x0, x1 = (0, e) if to_right else (e, 16)
    j = r + JOINT_GROW
    els = octagon("x", 8, AXIS_Z, r, x0, x1, "p", True)
    els += (octagon("z", e, 8, r, AXIS_Z, 16, "p", True) if outer
            else octagon("z", e, 8, r, 0, AXIS_Z, "p", True))
    els += octagon("y", e, AXIS_Z, j, 8 - j, 8 + j, "p", True)
    return els


# Where a pipe goes through a floor or ceiling: a steel collar plate flat on the face, so the
# run reads as passing through rather than stopping against it. On a wall pipe the plate's back
# edge runs half a pixel into the wall, out of sight.
PLATE_GROW = 1.6
PLATE_THICK = 0.6


def plate(cz, r, ceiling):
    y0, y1 = (16 - PLATE_THICK, 16) if ceiling else (0, PLATE_THICK)
    return octagon("y", 8, cz, r + PLATE_GROW, y0, y1, "steel", True)


# ------------------------------------------------------------------------------------------
# Fittings (main size), facing north
# ------------------------------------------------------------------------------------------
R_MAIN = SIZES["main"]


def valve_body():
    """An in-line valve: a barrel body on the run, bonnet and handwheel to the front."""
    return (octagon("y", 8, AXIS_Z, 5.2, 3.5, 12.5, "p", True)
            + [lc.box([7.3, 7.3, 3.0], [8.7, 8.7, 5.9], "steel")]
            + octagon("z", 8, 8, 3.2, 2.2, 3.0, "wheel", True))


def hose_outlet_body():
    """A tee with a hose valve on its outlet: stub to the front, the valve body, its handwheel
    on top and the capped outlet."""
    return (wall_joint(R_MAIN)
            + octagon("z", 8, 8, 2.4, 6.5, AXIS_Z, "p", True)
            + [lc.box([5.6, 5.6, 3.2], [10.4, 10.4, 6.5], "brass"),
               lc.box([7.5, 10.4, 4.3], [8.5, 12.6, 5.4], "steel")]
            + octagon("y", 8, 4.85, 2.2, 12.6, 13.3, "wheel", True)
            + octagon("z", 8, 8, 2.6, 1.4, 3.2, "cap", True))


def air_valve_body():
    """An air release valve: a short stub up from the run and the tank on it."""
    return (wall_joint(R_MAIN)
            + octagon("y", 8, AXIS_Z, 1.4, 12.8, 13.6, "steel", True)
            + octagon("y", 8, AXIS_Z, 3.4, 13.6, 19.4, "green", True)
            + octagon("y", 8, AXIS_Z, 1.8, 19.4, 20.2, "green", True))


def inlet_body():
    """The inlet manifold at the foot of a run: a check valve body under the riser and a
    header in front of it with three capped inlets facing the street."""
    els = wall_joint(R_MAIN)
    els.append(lc.box([5.5, 3.0, 8.4], [10.5, 8.0, 14.4], "p"))
    els += octagon("x", 5, 8.4, 2.6, 1.5, 14.5, "p", True)
    for x in (3.5, 8.0, 12.5):
        els += octagon("z", x, 5, 1.6, 4.4, 8.4, "p", True)
        els += octagon("z", x, 5, 2.0, 3.2, 4.4, "cap", True)
    return els


FITTINGS = [
    # key, Java enum, body, arms drawn on the item, names (en, de, es, sv)
    ("inlet_manifold", "INLET", inlet_body, ("up",),
     ("Standpipe Inlet Manifold", "Steigleitungs-Einspeisung",
      "Colector de entrada de columna seca", "Stigarledningens inmatningsgrenrör"), "m"),
    ("hose_outlet", "HOSE_OUTLET", hose_outlet_body, ("up", "down"),
     ("Standpipe Hose Outlet", "Steigleitungs-Schlauchanschluss",
      "Salida de manguera de columna seca", "Stigarledningens slanguttag"), "f"),
    ("air_valve", "AIR_VALVE", air_valve_body, ("left", "right"),
     ("Standpipe Air Release Valve", "Steigleitungs-Entlüftungsventil",
      "Válvula de purga de aire de columna seca", "Stigarledningens avluftningsventil"), "f"),
    ("drain_valve", "VALVE", valve_body, ("up", "down"),
     ("Standpipe Drain Valve", "Steigleitungs-Entleerungsventil",
      "Válvula de drenaje de columna seca", "Stigarledningens tömningsventil"), "f"),
]


# ------------------------------------------------------------------------------------------
# Blockstates and catalogue
# ------------------------------------------------------------------------------------------
def tex(finish):
    t = {"p": FINISHES[finish][0], "steel": FP("steel"), "brass": FP("brass"),
         "cap": FP("cap_brass"), "wheel": FP("handwheel"), "green": C.T("air_green")}
    t["particle"] = t["p"]
    return t


def model(finish, elements):
    return lc.model(tex(finish), elements)


ROT = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def wall_state(prefix, fitting_model=None):
    parts = []
    for facing, y in ROT:
        rot = {"y": y} if y else {}
        for side in ("up", "down", "left", "right", "front"):
            parts.append({"when": {"facing": facing, side: "true"},
                          "apply": dict(model=C.M(prefix + "_arm_" + side), **rot)})
        for where in ("floor", "ceiling"):
            parts.append({"when": {"facing": facing, where: "true"},
                          "apply": dict(model=C.M(prefix + "_" + where), **rot)})
        if not fitting_model:
            for kind in CORNERS:
                parts.append({"when": {"facing": facing, "corner": kind},
                              "apply": dict(model=C.M(prefix + "_corner_" + kind), **rot)})
        parts.append({"when": {"facing": facing, "joint": "true"},
                      "apply": dict(model=C.M(prefix + "_joint"), **rot)})
        if fitting_model:
            parts.append({"when": {"facing": facing}, "apply": dict(model=fitting_model, **rot)})
    return {"multipart": parts}


def centred_state(prefix):
    parts = [{"when": {side: "true"}, "apply": {"model": C.M(prefix + "_arm_" + side)}}
             for side in ("north", "south", "east", "west", "up", "down")]
    parts.append({"when": {"joint": "true"}, "apply": {"model": C.M(prefix + "_joint")}})
    for where in ("floor", "ceiling"):
        parts.append({"when": {where: "true"}, "apply": {"model": C.M(prefix + "_" + where)}})
    return {"multipart": parts}


def item(reg):
    return {"parent": "csm:block/%s/%s_item" % (C.model_dir, reg)}


def f1(v):
    return "%.1fF" % v


def box_java(els):
    pts = [p for el in els for p in _corners(el)]
    lo = [max(0.0, min(p[i] for p in pts)) for i in range(3)]
    hi = [min(16.0, max(p[i] for p in pts)) for i in range(3)]
    return "new AxisAlignedBB(%s)" % ", ".join("%.4fD" % (v / 16.0) for v in lo + hi)


def _corners(el):
    import gen_streetscape_utility as gu
    return gu.element_corners(el)


def pipes():
    # Shared arm and joint models, one set per size and finish.
    for size, r in SIZES.items():
        for finish in FINISHES:
            prefix = "standpipe_%s_%s" % (size, finish)
            models = {}
            for side in ("north", "south", "east", "west", "up", "down"):
                models[prefix + "_arm_" + side] = model(finish, centred_arm(side, r))
            models[prefix + "_joint"] = model(finish, centred_joint(r))
            models[prefix + "_floor"] = model(finish, plate(8, r, False))
            models[prefix + "_ceiling"] = model(finish, plate(8, r, True))
            wp = prefix + "_wall"
            for side in ("up", "down", "left", "right", "front"):
                models[wp + "_arm_" + side] = model(finish, wall_arm(side, r))
            models[wp + "_joint"] = model(finish, wall_joint(r))
            models[wp + "_floor"] = model(finish, plate(AXIS_Z, r, False))
            models[wp + "_ceiling"] = model(finish, plate(AXIS_Z, r, True))
            for kind in CORNERS:
                models[wp + "_corner_" + kind] = model(finish, wall_corner(kind, r))
            C.extra_models = getattr(C, "extra_models", {})
            C.extra_models.update(models)

    for size, r in SIZES.items():
        en_size, de_size, es_size, sv_size = {
            "main": ("Main", "Haupt", "principal", "huvud"),
            "branch": ("Branch", "Abzweig", "ramal", "gren")}[size]
        for finish, (_, (en, de, es_f, es_m, sv)) in FINISHES.items():
            prefix = "standpipe_%s_%s" % (size, finish)
            # Free-standing.
            reg = "standpipe_pipe_%s_%s" % (size, finish)
            models = {reg + "_item": model(finish, centred_arm("up", r) + centred_arm("down", r))}
            C.add(reg, 'new BlockStandpipePipe("%s", %s)' % (reg, f1(r)),
                  ("Standpipe %s Pipe (%s)" % (en_size, en),
                   "Steigleitungsrohr, %s (%s)" % (de_size, de),
                   "Tubería de columna seca, %s (%s)" % (es_size, es_f),
                   "Stigarledningsrör, %s (%s)" % (sv_size, sv)),
                  models, centred_state(prefix), item=item(reg), tab=TAB)
            # Wall run; the red branch is the old Standpipe Riser.
            riser = size == "branch" and finish == "red"
            reg = "standpipe_riser" if riser else "standpipe_wall_pipe_%s_%s" % (size, finish)
            names = (("Standpipe Riser", "Steigleitung", "Tubería vertical de columna seca",
                      "Stigarledning") if riser else
                     ("Standpipe %s Pipe, Wall (%s)" % (en_size, en),
                      "Steigleitungsrohr, %s, Wand (%s)" % (de_size, de),
                      "Tubería de columna seca, %s, de pared (%s)" % (es_size, es_f),
                      "Stigarledningsrör, %s, vägg (%s)" % (sv_size, sv)))
            models = {reg + "_item": model(finish, wall_arm("up", r) + wall_arm("down", r))}
            C.add(reg, 'new BlockStandpipeWallPipe("%s", %s,\n'
                       '        BlockStandpipeWallPipe.Fitting.NONE, null)' % (reg, f1(r)),
                  names, models, wall_state(prefix + "_wall"), item=item(reg), tab=TAB)


def fittings():
    for key, enum, body, item_arms, (en, de, es, sv), gender in FITTINGS:
        for finish, (_, (cen, cde, ces_f, ces_m, csv)) in FINISHES.items():
            reg = "standpipe_%s_%s" % (key, finish)
            els = body()
            models = {
                reg + "_body": model(finish, els),
                reg + "_item": model(finish, els + [e for side in item_arms
                                                    for e in wall_arm(side, R_MAIN)]),
            }
            state = wall_state("standpipe_main_%s_wall" % finish, C.M(reg + "_body"))
            java = ('new BlockStandpipeWallPipe("%s", %s,\n'
                    '        BlockStandpipeWallPipe.Fitting.%s,\n'
                    '        %s)' % (reg, f1(R_MAIN), enum, box_java(els)))
            C.add(reg, java, ("%s (%s)" % (en, cen), "%s (%s)" % (de, cde),
                              "%s (%s)" % (es, ces_f if gender == "f" else ces_m),
                              "%s (%s)" % (sv, csv)),
                  models, state, item=item(reg), tab=TAB)


pipes()
fittings()

# The shared arm and joint models belong to no one block; hang them on the first block so the
# catalogue writes (and --check compares) them.
C.blocks[0]["models"].update(C.extra_models)

if __name__ == "__main__":
    sys.exit(C.main())
