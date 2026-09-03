#!/usr/bin/env python3
"""
Procedural generator for the solar ALPR (automatic license-plate reader) camera block models.

Emits two Wavefront OBJ models (+ a shared MTL) used by the CSM mod's traffic-accessory ALPR
camera blocks. Both wear the same matte-black "Flock"-style teardrop camera body, a tilted solar
panel, and a drooping power cable between the panel and the camera:

  * alpr_camera_solar.obj      -- STANDALONE: a tall black ground pole with the solar panel mounted
                                  over the top and the teardrop camera clamped to the side of the
                                  pole, aiming outward. Matches a free-standing roadside install.
  * alpr_camera_solar_wall.obj -- WALL / SIDE MOUNT: the same camera + solar panel on a short
                                  back-plate bracket that seats flush against a wall or existing
                                  pole (no ground pole of its own).

Coordinate convention (matches gen_miovision_obj.py and the mod's Blockbench-exported OBJs):
  * 1 unit == 1 block.
  * X and Z are CENTERED on the block (-0.5 .. 0.5); the model is shifted +0.5 on X/Z at write time
    so it lands in Forge's 0..1 corner-origin space. Centering keeps blockstate y-rotation pivoting
    about the block centre for clean 4-way (NSEW) facing.
  * Y runs from 0 at the block bottom upward (may exceed 1 for the pole / overhead panel).
  * The camera looks toward -Z ("north", the unrotated blockstate variant); the mount/back is +Z.
  * Faces are triangles `f v/vt/vn`, wound so every normal points OUTWARD (Forge culls back-faces).

Materials: housing (matte black body/pole/mounts), lens (dark camera glass), solar (panel cells),
frame (silver panel edge), wire (black cable).

Run:  python dev-env-utils/scripts/gen_alpr_obj.py
"""

import math
import os
import sys

# Reuse the proven mesh toolkit from the Miovision generator (same scripts dir, so a plain import
# works -- Python puts the running script's own directory on sys.path[0]). main() there is guarded
# by __main__, so importing has no side effects.
from gen_miovision_obj import (
    Mesh, box, sweep_tube, revolve, straight, _nrm, _cap_ring,
)

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import csm_layout as layout  # noqa: E402

REPO_ROOT = layout.REPO_ROOT
MODEL_REL_DIR = "models/block/trafficaccessories/shared_models"
OUT_OWNER = layout.owner_of_folder("trafficaccessories")


def _out_path(filename):
    """Where ``filename`` in the shared trafficaccessories models folder should be written."""
    return layout.asset_for_write(OUT_OWNER, MODEL_REL_DIR + "/" + filename)

HOUSING_TEX = "csm:blocks/trafficsignals/shared_textures/metal_black"
LENS_TEX = "csm:blocks/trafficsignals/shared_textures/camera_lens"
SOLAR_TEX = "csm:blocks/trafficaccessories/shared_textures/solar_panel"
FRAME_TEX = "csm:blocks/trafficsignals/shared_textures/metal_silver"
WIRE_TEX = "csm:blocks/trafficsignals/shared_textures/metal_black"

RING = 16          # cross-section facets for the camera body / lens (smooth teardrop)
POLE_RING = 12     # facets for the round pole
WIRE_R = 0.018     # cable radius


# ---- transforms ----------------------------------------------------------------------------
# The shared Mesh toolkit only rotates about Y. The camera needs a downward PITCH (rotation about
# X) so its lens aims at the road, so we add an X-axis rotation here. We build the camera into its
# own sub-mesh, pitch it about the mount knuckle, then merge it into the assembly -- that way the
# pole / panel / cable geometry already in the assembly is untouched.
def _rot_x_point(p, deg, py, pz):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = p
    ry, rz = y - py, z - pz
    return (x, py + ry * c - rz * s, pz + ry * s + rz * c)


def _rot_y_point(p, deg, px, pz):
    """Yaw a single point about the vertical axis through (px, pz). Matches Mesh.rotate_y: positive
    deg is clockwise viewed from above (front -Z swings toward +X)."""
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    x, y, z = p
    return (px + (x - px) * c - (z - pz) * s, y, pz + (x - px) * s + (z - pz) * c)


def rotate_x(mesh, deg, py, pz):
    """Pitch a whole mesh about the X axis through (py, pz). Rotates stored normals too so winding
    and lighting stay correct."""
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    mesh.v = [(x, py + (y - py) * c - (z - pz) * s, pz + (y - py) * s + (z - pz) * c)
              for (x, y, z) in mesh.v]
    mesh.vn = [(nx, ny * c - nz * s, ny * s + nz * c) for (nx, ny, nz) in mesh.vn]


def merge(dst, src):
    """Append all geometry from sub-mesh `src` into `dst`, offsetting its (1-based) v/vt/vn indices."""
    ov, ot, on = len(dst.v), len(dst.vt), len(dst.vn)
    dst.v.extend(src.v)
    dst.vt.extend(src.vt)
    dst.vn.extend(src.vn)
    for mat, idx in src.faces:
        dst.faces.append((mat, [(vi + ov, ti + ot, ni + on) for (vi, ti, ni) in idx]))


# ---- generic builders ----------------------------------------------------------------------
def loft_z(mesh, sections, mat, ring=RING, cap_back=True, cap_front=True):
    """Loft a smooth tube along the Z axis through a list of elliptical cross-sections.

    sections: ordered list (back -> front) of (z, cx, cy, rx, ry). Each becomes an ellipse in the
    X-Y plane centred at (cx, cy, z) with half-width rx and half-height ry. Consecutive rings are
    bridged with quads wound outward (away from the centreline, which is correct for the convex
    body). Returns (rings, centers) so callers can build a custom front lens cap.
    """
    rings, centers = [], []
    for (z, cx, cy, rx, ry) in sections:
        pts = [(cx + rx * math.cos(2 * math.pi * j / ring),
                cy + ry * math.sin(2 * math.pi * j / ring), z) for j in range(ring)]
        rings.append(pts)
        centers.append((cx, cy, z))
    for i in range(len(rings) - 1):
        cmid = tuple((centers[i][k] + centers[i + 1][k]) / 2 for k in range(3))
        for j in range(ring):
            j2 = (j + 1) % ring
            verts = [rings[i][j], rings[i][j2], rings[i + 1][j2], rings[i + 1][j]]
            u0, u1 = j / ring, (j + 1) / ring
            v0, v1 = i / (len(rings) - 1), (i + 1) / (len(rings) - 1)
            mesh.quad_out(mat, verts, [(u0, v0), (u1, v0), (u1, v1), (u0, v1)], cmid)
    if cap_back:
        _cap_ring(mesh, rings[0], centers[0], centers[1], mat)
    if cap_front:
        _cap_ring(mesh, rings[-1], centers[-1], centers[-2], mat)
    return rings, centers


def disc_facing(mesh, center, radius, normal_sign_z, mat, ring=RING):
    """A flat circular disc in an X-Y plane at center=(cx,cy,cz), wound so its normal points along
    normal_sign_z (+1 => +Z, -1 => -Z). Used for the camera lens face."""
    cx, cy, cz = center
    inside = (cx, cy, cz + normal_sign_z * -1.0)  # a point behind the disc
    for j in range(ring):
        a0, a1 = 2 * math.pi * j / ring, 2 * math.pi * (j + 1) / ring
        pa = (cx + radius * math.cos(a0), cy + radius * math.sin(a0), cz)
        pb = (cx + radius * math.cos(a1), cy + radius * math.sin(a1), cz)
        ua = (0.5 + 0.45 * math.cos(a0), 0.5 + 0.45 * math.sin(a0))
        ub = (0.5 + 0.45 * math.cos(a1), 0.5 + 0.45 * math.sin(a1))
        n = _nrm((cx, cy, cz), pa, pb)
        if (n[2] >= 0) != (normal_sign_z > 0):
            pa, pb, ua, ub = pb, pa, ub, ua
        mesh.tri(mat, (cx, cy, cz), pa, pb, (0.5, 0.5), ua, ub)


def oriented_slab(mesh, center, uvec, vvec, nvec, hu, hv, hn, top_mat, side_mat):
    """A flat rectangular slab (the solar panel). uvec/vvec/nvec are orthonormal axes; hu/hv/hn are
    half-extents along them. The +nvec face is the panel top (solar cells); all others use side_mat.
    """
    def corner(su, sv, sn):
        return tuple(center[k] + su * hu * uvec[k] + sv * hv * vvec[k] + sn * hn * nvec[k]
                     for k in range(3))
    c = {(su, sv, sn): corner(su, sv, sn)
         for su in (-1, 1) for sv in (-1, 1) for sn in (-1, 1)}
    uv = [(0, 0), (1, 0), (1, 1), (0, 1)]
    # top (+n) and bottom (-n)
    mesh.quad_out(top_mat, [c[(-1, -1, 1)], c[(1, -1, 1)], c[(1, 1, 1)], c[(-1, 1, 1)]], uv, center)
    mesh.quad_out(side_mat, [c[(-1, -1, -1)], c[(1, -1, -1)], c[(1, 1, -1)], c[(-1, 1, -1)]], uv,
                  center)
    # four edges
    mesh.quad_out(side_mat, [c[(-1, 1, -1)], c[(1, 1, -1)], c[(1, 1, 1)], c[(-1, 1, 1)]], uv, center)
    mesh.quad_out(side_mat, [c[(-1, -1, -1)], c[(1, -1, -1)], c[(1, -1, 1)], c[(-1, -1, 1)]], uv,
                  center)
    mesh.quad_out(side_mat, [c[(1, -1, -1)], c[(1, 1, -1)], c[(1, 1, 1)], c[(1, -1, 1)]], uv, center)
    mesh.quad_out(side_mat, [c[(-1, -1, -1)], c[(-1, 1, -1)], c[(-1, 1, 1)], c[(-1, -1, 1)]], uv,
                  center)


def cable(p0, p1, sag, n=14, bow_z=0.0):
    """A drooping power cable from p0 to p1 that sags `sag` blocks below the straight chord at its
    midpoint (half-sine), matching the slack loop seen between the solar panel and the camera.
    `bow_z` offsets the midpoint in Z (negative = forward, -Z) so the cable bellies gently outward
    without moving its endpoints -- used to keep the cable just off the pole rather than flat against
    its front face."""
    pts = []
    for i in range(n + 1):
        t = i / n
        s = math.sin(math.pi * t)
        pts.append((p0[0] + (p1[0] - p0[0]) * t,
                    p0[1] + (p1[1] - p0[1]) * t - sag * s,
                    p0[2] + (p1[2] - p0[2]) * t + bow_z * s))
    return pts


# ---- ALPR camera body (shared) -------------------------------------------------------------
# Side profile traced from the reference photos / STL: a rounded "teardrop" wedge whose bottom edge
# droops toward the front lens face (-Z) while the top stays roughly level. Built back (+Z) -> front
# (-Z) as elliptical rings; cy is the ring centre height, ry its half-height -> bottom = cy-ry.
CAM_SECTIONS = [
    # (z,     cx,  cy,    rx,    ry)
    (0.200, 0.0, 0.550, 0.055, 0.060),   # back nub (the mount knuckle plugs in here)
    (0.130, 0.0, 0.545, 0.120, 0.115),
    (0.030, 0.0, 0.520, 0.175, 0.160),
    (-0.070, 0.0, 0.490, 0.205, 0.185),
    (-0.170, 0.0, 0.470, 0.215, 0.200),
    (-0.260, 0.0, 0.455, 0.215, 0.205),
    (-0.300, 0.0, 0.450, 0.190, 0.190),  # front rim (lens face plane)
]
CAM_FRONT_Z = -0.300
CAM_BACK_Z = 0.200
LENS_R = 0.130


CAM_PITCH = 14.0   # degrees of downward tilt so the lens aims at the road (nose-down)
# Degrees of sideways cant toward the road. Negative yaw swings the lens (-Z) toward -X, which is
# the viewer's right when standing in front of the camera (forward=+Z, up=+Y => right=-X).
CAM_YAW = -15.0


def add_camera(mesh, dy, pitch_deg=CAM_PITCH, yaw_deg=CAM_YAW):
    """Add the teardrop camera body (+ lens + back mount knuckle) raised by `dy` in Y, pitched
    `pitch_deg` degrees nose-down and yawed `yaw_deg` degrees sideways so the lens aims down toward
    the road. The body is pitched/yawed about its mount knuckle, so the back stays put against the
    arm while the front swings. Returns key attachment points (in final coords): the back knuckle
    centre and the top-front cable anchor."""
    cam = Mesh()   # build the camera into its own sub-mesh so it can be pitched independently
    sections = [(z, cx, cy + dy, rx, ry) for (z, cx, cy, rx, ry) in CAM_SECTIONS]
    rings, centers = loft_z(cam, sections, "housing", cap_back=True, cap_front=False)

    # Front lens assembly: a black bezel FUNNEL recessing from the front rim back into the body to a
    # dark lens disc. The funnel must have real depth in Z -- a flat (single-Z) annulus gives every
    # face a +/-Z normal whose outward test against an in-plane reference is ~0, so the winding is
    # indeterminate and the ring renders see-through. Stepping the inner ring back (+Z) into the body
    # makes the bezel a cone with a well-defined outward normal, sealing the front.
    front_center = centers[-1]
    cyf = front_center[1]
    body_inside = (0.0, cyf, 0.10)              # reference point well inside the body (for winding)
    bezel_z = CAM_FRONT_Z + 0.03               # lens plane recessed ~0.03 behind the front rim
    bezel_inner = [(LENS_R * math.cos(2 * math.pi * j / RING),
                    cyf + LENS_R * math.sin(2 * math.pi * j / RING),
                    bezel_z) for j in range(RING)]
    rim = rings[-1]
    for j in range(RING):
        j2 = (j + 1) % RING
        cam.quad_out("housing", [rim[j], rim[j2], bezel_inner[j2], bezel_inner[j]],
                     [(0, 0), (1, 0), (1, 1), (0, 1)], body_inside)
    disc_facing(cam, (0.0, cyf, bezel_z), LENS_R, -1, "lens")
    # central lens barrel poking forward out of the recess (reads as the glass eye from any angle)
    barrel = straight((0.0, cyf, bezel_z + 0.01), (0.0, cyf, CAM_FRONT_Z - 0.01), 2)
    sweep_tube(cam, barrel, 0.05, "lens", ring=10, cap_ends=True, up_seed=(0.0, 1.0, 0.0))

    # Yaw the camera sideways then pitch it nose-down, both about the knuckle, then merge it in. The
    # knuckle sits on both rotation axes (x=0, z=CAM_BACK_Z), so it stays put -- the back keeps flush
    # against the arm while only the lens end swings.
    pivot_y, pivot_z = 0.545 + dy, CAM_BACK_Z
    cam.rotate_y(yaw_deg, 0.0, pivot_z)            # cant the lens sideways toward the road
    rotate_x(cam, -pitch_deg, pivot_y, pivot_z)    # negative => front (-Z) swings down toward the road
    merge(mesh, cam)

    knuckle = (0.0, pivot_y, CAM_BACK_Z)   # pivot point is unchanged by the rotations
    cable_anchor = _rot_y_point((0.0, 0.640 + dy, -0.080), yaw_deg, 0.0, pivot_z)
    cable_anchor = _rot_x_point(cable_anchor, -pitch_deg, pivot_y, pivot_z)
    return knuckle, cable_anchor


def add_solar_panel(mesh, center, half_w, half_len, tilt_deg, mount_base):
    """Add a tilted solar panel centred at `center` and a short mast box from `mount_base` up to the
    panel underside. The panel tilts `tilt_deg` from horizontal, rising toward +Z (back) so its face
    looks up and toward the camera side (-Z). Returns (front-bottom edge, underside-centre) cable
    anchors."""
    th = math.radians(tilt_deg)
    uvec = (1.0, 0.0, 0.0)
    vvec = (0.0, math.sin(th), math.cos(th))       # length axis, tilted up toward +Z
    nvec = (0.0, math.cos(th), -math.sin(th))      # panel normal: up & toward -Z
    oriented_slab(mesh, center, uvec, vvec, nvec, half_w, half_len, 0.03, "solar", "frame")
    # Mount arm from the mount base to the panel underside centre. Built as a tube along the actual
    # mount_base -> underside line so it reaches the panel even when the panel overhangs forward of
    # the base (a plain vertical box left the wall-mount panel floating ahead of its bracket).
    under = tuple(center[k] - nvec[k] * 0.03 for k in range(3))
    sweep_tube(mesh, straight(mount_base, under, 3), 0.045, "housing", ring=8, cap_ends=True)
    # cable leaves the panel's front-bottom edge
    front_edge = tuple(center[k] - vvec[k] * half_len - nvec[k] * 0.03 for k in range(3))
    return front_edge, under


def add_clamp(mesh, pole_z, y0, y1):
    """Two thin band clamps wrapping the pole front, plus a flat mount ear -- the bracket that holds
    the camera knuckle onto the pole."""
    box(mesh, -0.11, 0.11, y0, y0 + 0.04, pole_z - 0.12, pole_z + 0.12, "housing")
    box(mesh, -0.11, 0.11, y1 - 0.04, y1, pole_z - 0.12, pole_z + 0.12, "housing")
    box(mesh, -0.06, 0.06, y0, y1, pole_z - 0.12, pole_z - 0.02, "housing")  # mount ear (toward -Z)


# ---- assemblies ----------------------------------------------------------------------------
POLE_Z = 0.30
POLE_R = 0.09
POLE_TOP = 5.55


def build_standalone():
    m = Mesh()
    # Tall ground pole.
    revolve(m, [(0.0, POLE_R), (POLE_TOP, POLE_R)], (0.0, POLE_Z), "housing", ring=POLE_RING)
    box(m, -0.16, 0.16, 0.0, 0.06, POLE_Z - 0.16, POLE_Z + 0.16, "housing")  # base flange
    box(m, -POLE_R, POLE_R, POLE_TOP - 0.04, POLE_TOP + 0.02,
        POLE_Z - POLE_R, POLE_Z + POLE_R, "housing")                          # top cap

    # Camera clamped to the front (-Z side) of the pole near the top.
    cam_dy = 4.15
    knuckle, cam_anchor = add_camera(m, cam_dy)
    add_clamp(m, POLE_Z, knuckle[1] - 0.13, knuckle[1] + 0.13)
    # short arm from the clamp ear out to the camera knuckle
    box(m, -0.05, 0.05, knuckle[1] - 0.05, knuckle[1] + 0.05, POLE_Z - 0.16, knuckle[2], "housing")

    # Solar panel mounted over the top of the pole, tilted toward the camera side.
    panel_center = (0.0, POLE_TOP + 0.22, POLE_Z - 0.10)
    panel_front, panel_under = add_solar_panel(m, panel_center, half_w=0.52, half_len=0.46,
                                               tilt_deg=20.0,
                                               mount_base=(0.0, POLE_TOP - 0.02, POLE_Z - 0.02))

    # Drooping cable from the panel down to the camera. Starts at the panel's underside (so it
    # actually meets the panel) and descends in a vertical plane just off the pole's front face
    # (close to the pole, not flat against it) with a gentle forward belly -- rather than bowing way
    # out in front.
    wire_z = POLE_Z - POLE_R - 0.04   # ~0.17, a small gap in front of the pole's front surface
    c0 = panel_under                  # on the panel underside, above the pole top
    c1 = (0.0, cam_anchor[1] + 0.02, wire_z)
    sweep_tube(m, cable(c0, c1, sag=0.08, n=16, bow_z=-0.09), WIRE_R, "wire", ring=6)
    return m


def build_wall():
    m = Mesh()
    # Back plate / bracket that seats flush against the wall (+Z, z=0.50) and rises above the block
    # to carry the overhead solar panel.
    plate_z1 = 0.50
    box(m, -0.20, 0.20, 0.30, 1.32, plate_z1 - 0.06, plate_z1, "housing")

    # Camera on a short arm off the plate.
    cam_dy = 0.05
    knuckle, cam_anchor = add_camera(m, cam_dy)
    box(m, -0.06, 0.06, knuckle[1] - 0.06, knuckle[1] + 0.06, knuckle[2], plate_z1 - 0.04, "housing")

    # Solar panel on a short mast above, same tilt as the standalone. Pushed forward (-Z) so the
    # tilted panel's back corner clears the wall behind it (the back face is at z=0.50): the back
    # edge sits at center_z + half_len*cos(tilt) ~= 0.06 + 0.41 = 0.47, just shy of the wall.
    panel_center = (0.0, 1.42, 0.06)
    panel_front, panel_under = add_solar_panel(m, panel_center, half_w=0.48, half_len=0.44,
                                               tilt_deg=20.0,
                                               mount_base=(0.0, 1.28, plate_z1 - 0.06))

    # Cable from the panel underside (so it meets the panel) down to the camera, tucked back toward
    # the mount with a gentle forward belly rather than bowing out front.
    c0 = panel_under
    c1 = (0.0, cam_anchor[1] + 0.03, 0.13)
    sweep_tube(m, cable(c0, c1, sag=0.06, n=16, bow_z=-0.08), WIRE_R, "wire", ring=6)
    return m


# ---- output --------------------------------------------------------------------------------
def write_mtl(path):
    with open(path, "w", newline="\n") as f:
        f.write("# Procedurally generated by gen_alpr_obj.py\n")
        for name, tex in (("housing", HOUSING_TEX), ("lens", LENS_TEX), ("solar", SOLAR_TEX),
                          ("frame", FRAME_TEX), ("wire", WIRE_TEX)):
            f.write("newmtl %s\nmap_Kd %s\n\n" % (name, tex))
        f.write("newmtl none\n")


def _center(mesh):
    xs = [p[0] for p in mesh.v]
    ys = [p[1] for p in mesh.v]
    zs = [p[2] for p in mesh.v]
    return ((min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2, (min(zs) + max(zs)) / 2)


def _report(name, mesh):
    xs = [p[0] for p in mesh.v]
    ys = [p[1] for p in mesh.v]
    zs = [p[2] for p in mesh.v]
    cen = (sum(xs) / len(xs), sum(ys) / len(ys), sum(zs) / len(zs))
    out = 0
    for _, idx in mesh.faces:
        vs = [mesh.v[i - 1] for i, _, _ in idx]
        n = _nrm(*vs[:3])
        fc = tuple(sum(p[k] for p in vs) / 3 for k in range(3))
        if sum(n[k] * (fc[k] - cen[k]) for k in range(3)) >= 0:
            out += 1
    print("%-26s verts=%4d tris=%4d  x[%.2f,%.2f] y[%.2f,%.2f] z[%.2f,%.2f]  outward~%d%%" % (
        name, len(mesh.v), len(mesh.faces),
        min(xs) + 0.5, max(xs) + 0.5, min(ys), max(ys), min(zs) + 0.5, max(zs) + 0.5,
        round(100 * out / len(mesh.faces))))


def main():
    mtl_name = "alpr_camera_solar.mtl"
    mtl_path = _out_path(mtl_name)
    os.makedirs(os.path.dirname(mtl_path), exist_ok=True)
    write_mtl(mtl_path)

    builders = (("alpr_camera_solar", build_standalone),
                ("alpr_camera_solar_wall", build_wall))
    for base, builder in builders:
        m = builder()
        m.write(_out_path(base + ".obj"), mtl_name)
        _report(base, m)
        # Inventory copy centred on the origin so item-frame/GUI rotation (which pivots about the
        # model origin) keeps it in view -- same trick the furniture/Miovision OBJs use.
        inv = builder()
        cx, cy, cz = _center(inv)
        inv.write(_out_path(base + "_inv.obj"), mtl_name, offset=(-cx, -cy, -cz))


if __name__ == "__main__":
    main()
