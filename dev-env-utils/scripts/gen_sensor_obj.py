#!/usr/bin/env python3
"""
Generators / JSON-bakers for the *existing* traffic-signal sensor model realism pass (Workstream B).

Produces OBJ replacements for sensor heads that read wrong in the original Blockbench JSON models,
preserving proportions and the metal palette:

  * tiny_cam : the camera head was a faceted cylinder; rebuilt as a real sphere sitting on the stub.

Reuses the robust mesh helpers from gen_miovision_obj.py (outward-forced winding via quad_out, and
write() that offsets center-origin build coords into Minecraft's 0..1 block space). Like the
Miovision blocks, it also writes an origin-centred *_inv model for correct inventory icons.

Run from repo root:  python dev-env-utils/scripts/gen_sensor_obj.py
"""

import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_miovision_obj import (Mesh, box, sweep_tube, add_wire_loop, WIRE_R, _nrm,  # noqa: E402
                               OUT_DIR, _center)


# ---- JSON (Blockbench) -> OBJ baker, with an optional head tilt -----------------------------
_FACE_CORNERS = {
    "down": [0, 1, 2, 3], "up": [4, 5, 6, 7], "north": [0, 1, 5, 4],
    "south": [3, 2, 6, 7], "west": [0, 3, 7, 4], "east": [1, 2, 6, 5],
}


def _rot_about(p, axis, ang_deg, origin):
    a = math.radians(ang_deg)
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    elif axis == "z":
        x, y = x * c - y * s, x * s + y * c
    return (x + origin[0], y + origin[1], z + origin[2])


def bake_model(model_path, tex_fallback, tilt_pred=None, tilt=None):
    """Bake a Blockbench JSON block model into a Mesh (cuboids -> quads, element rotations baked in),
    converting 0..16 coords to centred build units. Faces keep their resolved texture as the material.
    Elements for which tilt_pred(i, element) is True get an extra `tilt` rotation (e.g. pitch the
    camera head down). Returns (mesh, {texture: material_name})."""
    model = json.load(open(model_path))
    mtex = dict(tex_fallback)
    mtex.update(model.get("textures", {}))

    def resolve(key):
        k = key.lstrip("#")
        for _ in range(8):
            v = mtex.get(k)
            if isinstance(v, str) and v.startswith("#"):
                k = v[1:]
            else:
                break
        return mtex.get(k, mtex.get("all", "csm:blocks/trafficsignals/shared_textures/metal_white"))

    mesh = Mesh()
    materials = {}

    def matname(tex):
        if tex not in materials:
            materials[tex] = "m%d" % len(materials)
        return materials[tex]

    for i, el in enumerate(model.get("elements", [])):
        f, t = el["from"], el["to"]
        corners = [(f[0], f[1], f[2]), (t[0], f[1], f[2]), (t[0], f[1], t[2]), (f[0], f[1], t[2]),
                   (f[0], t[1], f[2]), (t[0], t[1], f[2]), (t[0], t[1], t[2]), (f[0], t[1], t[2])]
        r = el.get("rotation")
        if r and r.get("angle"):
            corners = [_rot_about(c, r["axis"], r["angle"], r["origin"]) for c in corners]
        if tilt and tilt_pred and tilt_pred(i, el):
            corners = [_rot_about(c, tilt["axis"], tilt["angle"], tilt["origin"]) for c in corners]
        cb = [(c[0] / 16.0 - 0.5, c[1] / 16.0, c[2] / 16.0 - 0.5) for c in corners]
        centroid = tuple(sum(p[k] for p in cb) / 8 for k in range(3))
        for face, fc in el.get("faces", {}).items():
            if face not in _FACE_CORNERS:
                continue
            idx = _FACE_CORNERS[face]
            verts = [cb[idx[0]], cb[idx[1]], cb[idx[2]], cb[idx[3]]]
            mat = matname(resolve(fc.get("texture", "#0")))
            mesh.quad_out(mat, verts, [(0, 0), (1, 0), (1, 1), (0, 1)], centroid)
    return mesh, materials


def write_baked(mesh, materials, obj_name, offset=(0.5, 0.0, 0.5)):
    mtl_name = obj_name.replace(".obj", ".mtl")
    with open(os.path.join(OUT_DIR, mtl_name), "w", newline="\n") as f:
        f.write("# Procedurally baked by gen_sensor_obj.py\n")
        for tex, mat in materials.items():
            f.write("newmtl %s\nmap_Kd %s\n\n" % (mat, tex))
        f.write("newmtl none\n")
    mesh.write(os.path.join(OUT_DIR, obj_name), mtl_name, offset=offset)

BLACK_TEX = "csm:blocks/trafficsignals/shared_textures/metal_black"
LENS_TEX = "csm:blocks/trafficsignals/shared_textures/solidoff"


def b(v):
    """Blockbench 0..16 coordinate -> centered build units (X,Z about 0, Y from 0)."""
    return v / 16.0


def sphere(mesh, center, radius, mat, lat=12, lon=16):
    cx, cy, cz = center

    def P(i, j):
        th = math.pi * i / lat
        ph = 2 * math.pi * j / lon
        return (cx + radius * math.sin(th) * math.cos(ph),
                cy + radius * math.cos(th),
                cz + radius * math.sin(th) * math.sin(ph))

    for i in range(lat):
        for j in range(lon):
            verts = [P(i, j), P(i, j + 1), P(i + 1, j + 1), P(i + 1, j)]
            u0, u1 = j / lon, (j + 1) / lon
            v0, v1 = i / lat, (i + 1) / lat
            mesh.quad_out(mat, verts, [(u0, v0), (u1, v0), (u1, v1), (u0, v1)], center)


def _unit(v):
    m = math.sqrt(sum(c * c for c in v)) or 1.0
    return (v[0] / m, v[1] / m, v[2] / m)


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def lens_spot(mesh, surface_pt, radius, normal, mat, ring=14):
    """A small slightly-raised dark lens disc facing `normal`, centred at `surface_pt`."""
    n = _unit(normal)
    ref = (0.0, 1.0, 0.0) if abs(n[1]) < 0.9 else (1.0, 0.0, 0.0)
    u = _unit(_cross(ref, n))
    w = _cross(n, u)
    c = (surface_pt[0] + n[0] * 0.005, surface_pt[1] + n[1] * 0.005, surface_pt[2] + n[2] * 0.005)
    for j in range(ring):
        a0, a1 = 2 * math.pi * j / ring, 2 * math.pi * (j + 1) / ring
        pa = (c[0] + radius * (math.cos(a0) * u[0] + math.sin(a0) * w[0]),
              c[1] + radius * (math.cos(a0) * u[1] + math.sin(a0) * w[1]),
              c[2] + radius * (math.cos(a0) * u[2] + math.sin(a0) * w[2]))
        pb = (c[0] + radius * (math.cos(a1) * u[0] + math.sin(a1) * w[0]),
              c[1] + radius * (math.cos(a1) * u[1] + math.sin(a1) * w[1]),
              c[2] + radius * (math.cos(a1) * u[2] + math.sin(a1) * w[2]))
        tri = (c, pa, pb)
        if sum(_nrm(*tri)[k] * n[k] for k in range(3)) < 0:   # ensure it faces `normal`
            pa, pb = pb, pa
        mesh.tri(mat, c, pa, pb, (0.5, 0.5),
                 (0.5 + 0.45 * math.cos(a0), 0.5 + 0.45 * math.sin(a0)),
                 (0.5 + 0.45 * math.cos(a1), 0.5 + 0.45 * math.sin(a1)))


def build_tiny_cam():
    """Small camera: keep the original thin mounting stub, replace the cylinder head with a sphere.

    Original Blockbench (0..16): stub box [7.5,0,7]->[8.5,9.25,8]; faceted-cylinder head centred
    ~ (8, 11.3, 7.1); the camera faces -Z (front). Palette: metal_black body, solidoff lens.
    """
    m = Mesh()
    # mounting stub / post (kept from the original proportions)
    box(m, b(7.5) - 0.5, b(8.5) - 0.5, b(0.0), b(9.25), b(7.0) - 0.5, b(8.0) - 0.5, "black")
    # spherical camera ball sitting on the stub
    cx, cy, cz = 0.0, b(11.3), b(7.1) - 0.5
    r = b(2.5)
    sphere(m, (cx, cy, cz), r, "black")
    # dark lens on the lower-front of the ball, aimed down-and-forward toward the road (-Z)
    nrm = _unit((0.0, -0.65, -0.76))
    surf = (cx + r * nrm[0], cy + r * nrm[1], cz + r * nrm[2])
    lens_spot(m, surf, b(1.4), nrm, "lens")
    return m


def oriented_box(mesh, center, d, up, hl, hw, ht, mat):
    """A box aligned to a frame: `d` = length axis, `up` = height axis, side = d x up."""
    d = _unit(d)
    up = _unit(up)
    side = _unit(_cross(d, up))
    cs = []
    for a in (-1, 1):
        for bb in (-1, 1):
            for c in (-1, 1):
                cs.append((center[0] + a * hl * d[0] + bb * hw * side[0] + c * ht * up[0],
                           center[1] + a * hl * d[1] + bb * hw * side[1] + c * ht * up[1],
                           center[2] + a * hl * d[2] + bb * hw * side[2] + c * ht * up[2]))
    for f in [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1), (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]:
        mesh.quad_out(mat, [cs[f[0]], cs[f[1]], cs[f[2]], cs[f[3]]],
                      [(0, 0), (1, 0), (1, 1), (0, 1)], center)


def hood(mesh, start, d, up, length, radius, mat, n_along=4, n_arc=8, arc_deg=210.0):
    """A contoured sun hood: the top portion of a cylinder (bottom cut off) swept along the camera
    axis `d`, sitting over the camera. Open underneath and at the ends, like a real shroud."""
    d = _unit(d)
    up = _unit(up)
    side = _unit(_cross(d, up))
    rings = []
    for i in range(n_along + 1):
        base = (start[0] + d[0] * length * i / n_along,
                start[1] + d[1] * length * i / n_along,
                start[2] + d[2] * length * i / n_along)
        ring = []
        for j in range(n_arc + 1):
            th = math.radians(-arc_deg / 2 + arc_deg * j / n_arc)   # 0 == straight up
            ring.append((base[0] + radius * (math.sin(th) * side[0] + math.cos(th) * up[0]),
                         base[1] + radius * (math.sin(th) * side[1] + math.cos(th) * up[1]),
                         base[2] + radius * (math.sin(th) * side[2] + math.cos(th) * up[2])))
        rings.append((base, ring))
    for i in range(n_along):
        ra, rb = rings[i][1], rings[i + 1][1]
        for j in range(n_arc):
            a, b, c, e = ra[j], ra[j + 1], rb[j + 1], rb[j]
            # double-sided: a hood is a thin open shell, so render both faces (otherwise the
            # underside is culled and the hood looks broken / half-missing from the side)
            mesh.quad(mat, a, b, c, e, (0, 0), (1, 0), (1, 1), (0, 1))
            mesh.quad(mat, a, e, c, b, (0, 0), (0, 1), (1, 1), (1, 0))


def side_wire_loop(mesh, back_top, pole_z, camera_y):
    """Black cable: straight out the back of the camera, then curving down into a drip-loop that
    hangs just off to the +X side of the pole and runs into the pole."""
    lx = 0.105                       # beside the pole (pole_r ~0.045 + cable)
    cz = pole_z + 0.02
    cy = camera_y - 0.15
    r = 0.085

    def oc(deg):
        a = math.radians(deg)
        return (lx, cy + r * math.sin(a), cz + r * math.cos(a))

    circle = [oc(360.0 * i / 22) for i in range(23)]
    sweep_tube(mesh, circle, WIRE_R, "wire", ring=6, cap_ends=False)
    # tail: straight out the back (+Z), then curve down and drift to the side, into the loop top
    bx, by, bz = back_top
    sweep_tube(mesh, [(bx, by, bz),
                      (bx, by + 0.015, bz + 0.10),    # straight back
                      (0.045, by - 0.05, bz + 0.07),  # begin dropping, drift to +X
                      oc(95.0)],                       # into the top of the loop
               WIRE_R, "wire", ring=6)
    # from the loop into the pole
    sweep_tube(mesh, [oc(45.0), (0.0, camera_y - 0.04, pole_z)], WIRE_R, "wire", ring=6)


def build_autoscope(camera_y, pole_bottom=0.06, body_len=0.40, cam_r=0.095, tilt_deg=20.0):
    """Autoscope Solo Terra: a thin vertical camera pole clamped to a mast arm, with a cylindrical
    camera sitting ON TOP of the pole (via a collar + mount), angled down at the road under a
    contoured sun hood, and a black cable drip-loop resting against the side of the pole."""
    m = Mesh()
    pole_z, pole_r = 0.32, 0.045
    a = math.radians(tilt_deg)
    d = (0.0, -math.sin(a), -math.cos(a))     # camera aim: down-and-forward
    up = (0.0, math.cos(a), -math.sin(a))     # camera "up"

    # thin vertical camera pole
    sweep_tube(m, [(0.0, pole_bottom, pole_z), (0.0, camera_y, pole_z)], pole_r, "silver",
               ring=10, cap_ends=True)
    # solid pipe collar at the pole top (capped, so no see-through at the top)
    sweep_tube(m, [(0.0, camera_y - 0.06, pole_z), (0.0, camera_y + 0.02, pole_z)], 0.066, "silver",
               ring=12, cap_ends=True)
    # minimal clamp to the mast arm (flush to the back block edge), lower on the pole
    box(m, -0.05, 0.05, 0.32, 0.58, pole_z + pole_r - 0.01, 0.50, "silver")
    box(m, -0.08, 0.08, 0.38, 0.52, pole_z - 0.055, pole_z + 0.055, "silver")

    # camera sits ON TOP of the pole: a small mount, then the cylinder angled down-forward
    mount = (0.0, camera_y + 0.02, pole_z)
    oriented_box(m, mount, d, up, 0.05, 0.06, 0.045, "silver")
    # cylinder centre just above the mount; extends forward to the lens and a bit BACKWARD past the
    # pole so the cable can drape off the back over air before looping into the pole.
    axis = (mount[0] + up[0] * (cam_r + 0.015), mount[1] + up[1] * (cam_r + 0.015),
            mount[2] + up[2] * (cam_r + 0.015))
    back_ext = 0.16
    rear = (axis[0] - d[0] * back_ext, axis[1] - d[1] * back_ext, axis[2] - d[2] * back_ext)
    front = (axis[0] + d[0] * body_len, axis[1] + d[1] * body_len, axis[2] + d[2] * body_len)
    sweep_tube(m, [rear, front], cam_r, "white", ring=14, cap_ends=True)
    # contoured (double-sided) sun hood over the top of the cylinder, extending past the front
    hood(m, (rear[0] + d[0] * 0.04, rear[1] + d[1] * 0.04, rear[2] + d[2] * 0.04),
         d, up, (body_len + back_ext) * 1.0, cam_r + 0.02, "white")
    # small metal standoffs bridging the camera body to the hood (so the hood isn't floating).
    # Sized to just span the ~0.02 gap (camera surface -> hood) without poking through the hood.
    for t in (0.55, 0.9):
        ac = (rear[0] + (front[0] - rear[0]) * t, rear[1] + (front[1] - rear[1]) * t,
              rear[2] + (front[2] - rear[2]) * t)
        so_c = (ac[0] + up[0] * (cam_r + 0.007), ac[1] + up[1] * (cam_r + 0.007),
                ac[2] + up[2] * (cam_r + 0.007))
        # narrow (small width along `side`) so the box corners stay near the top of the hood's
        # curve and don't poke through where the hood curves away.
        oriented_box(m, so_c, d, up, 0.015, 0.008, 0.010, "silver")
    # dark lens at the front
    lens_spot(m, (front[0] + d[0] * 0.01, front[1] + d[1] * 0.01, front[2] + d[2] * 0.01),
              cam_r * 0.85, d, "lens")
    # black cable: out the back of the camera, into a side drip-loop, into the pole
    side_wire_loop(m, (rear[0] + up[0] * cam_r * 0.4, rear[1] + up[1] * cam_r * 0.4,
                       rear[2] + up[2] * cam_r * 0.4), pole_z, camera_y)
    return m


def emit(mesh, base, mat_to_tex):
    """Write base.obj (0..1) + base_inv.obj (origin-centred for inventory) + base.mtl."""
    mtl = base + ".mtl"
    with open(os.path.join(OUT_DIR, mtl), "w", newline="\n") as f:
        f.write("# Procedurally generated by gen_sensor_obj.py\n")
        for mat, tex in mat_to_tex.items():
            f.write("newmtl %s\nmap_Kd %s\n\n" % (mat, tex))
        f.write("newmtl none\n")
    mesh.write(os.path.join(OUT_DIR, base + ".obj"), mtl)
    cx, cy, cz = _center(mesh)
    mesh.write(os.path.join(OUT_DIR, base + "_inv.obj"), mtl, offset=(-cx, -cy, -cz))


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    ms = "csm:blocks/trafficsignals/shared_textures/metal_silver"
    mw = "csm:blocks/trafficsignals/shared_textures/metal_white"
    mb = "csm:blocks/trafficsignals/shared_textures/metal_black"
    so = "csm:blocks/trafficsignals/shared_textures/solidoff"
    emit(build_tiny_cam(), "trafficpolecamera_tiny_cam", {"black": mb, "lens": so})
    aut = {"silver": ms, "white": mw, "lens": so, "wire": mb}
    emit(build_autoscope(2.30), "trafficpolecamera", aut)
    emit(build_autoscope(1.25, body_len=0.34, cam_r=0.085), "trafficpolecamera_short", aut)
    print("generated tiny_cam, autoscope (regular + short)")


if __name__ == "__main__":
    main()
