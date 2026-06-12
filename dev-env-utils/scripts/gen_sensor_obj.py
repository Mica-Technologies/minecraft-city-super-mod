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
                               OUT_DIR, _center, revolve, disc_down, lens_disc, join, straight)


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


def _flir_mount(m, stub_top, knuckle_y):
    """Shared FLIR mount: a mini stub extending ~1/2 block DOWN into the block below to meet the mast
    arm (like the Vantage Vector), plus a small knuckle. Camera is added by the caller on top."""
    box(m, -0.03, 0.03, -0.5, stub_top, -0.035, 0.035, "silver")  # slim mount stub into the arm
    box(m, -0.05, 0.05, knuckle_y - 0.05, knuckle_y + 0.05, -0.09, 0.02, "black")  # knuckle
    # small cable drip-loop beside the stub, dropping into the mast arm below
    lx, lcy, lcz, r = 0.095, -0.04, 0.0, 0.06

    def oc(deg):
        a = math.radians(deg)
        return (lx, lcy + r * math.sin(a), lcz + r * math.cos(a))

    sweep_tube(m, [oc(360.0 * i / 16) for i in range(17)], WIRE_R, "wire", ring=5, cap_ends=False)
    sweep_tube(m, [(0.03, knuckle_y - 0.05, 0.0), (lx, 0.04, lcz), oc(100.0)], WIRE_R, "wire", ring=5)
    sweep_tube(m, [oc(250.0), (lx, -0.34, lcz)], WIRE_R, "wire", ring=5)


def build_tiny_cam():
    """FLIR TrafiCam: a black sphere camera with a small lens brim, sitting ON TOP of the mast arm on
    a flat slotted bracket + short post + knuckle. Faces -Z, slightly down."""
    m = Mesh()
    a = math.radians(18)
    d = (0.0, -math.sin(a), -math.cos(a))
    up = (0.0, math.cos(a), -math.sin(a))
    _flir_mount(m, stub_top=0.12, knuckle_y=0.14)
    # spherical camera sitting low, close to the mast arm, facing the road
    cx, cy, cz = 0.0, 0.22, -0.05
    r = 0.13
    sphere(m, (cx, cy, cz), r, "black")
    lp = (cx + d[0] * r, cy + d[1] * r, cz + d[2] * r)
    lens_spot(m, lp, 0.055, d, "lens")
    # sun brim nestled on the sphere, extended a bit further down toward the lens, with a small
    # standoff so it isn't floating
    hc = (cx + d[0] * r * 0.26 + up[0] * r * 0.42, cy + d[1] * r * 0.26 + up[1] * r * 0.42,
          cz + d[2] * r * 0.26 + up[2] * r * 0.42)
    hood(m, hc, d, up, r * 1.18, r * 0.74, "black", n_arc=10, arc_deg=178, back_wall=True)
    sp = (cx + d[0] * r * 0.16 + up[0] * r * 0.80, cy + d[1] * r * 0.16 + up[1] * r * 0.80,
          cz + d[2] * r * 0.16 + up[2] * r * 0.80)   # sphere surface point under the brim
    oriented_box(m, ((sp[0] + hc[0]) / 2, (sp[1] + hc[1]) / 2, (sp[2] + hc[2]) / 2),
                 up, d, 0.022, 0.022, 0.02, "black")  # standoff bridging sphere -> brim
    return m


def build_trafiradar():
    """FLIR TrafiRadar: a rounded black rectangular housing (taller than wide) with a flat radar
    panel up front and a small camera/lens with a brim below it, sitting ON TOP of the mast arm on
    the same flat bracket + post + knuckle as the TrafiCam."""
    m = Mesh()
    a = math.radians(14)
    d = (0.0, -math.sin(a), -math.cos(a))
    up = (0.0, math.cos(a), -math.sin(a))
    _flir_mount(m, stub_top=0.14, knuckle_y=0.16)
    # rounded rectangular housing sitting low, close to the mast arm, tilted slightly down
    bc = (0.0, 0.32, -0.04)
    oriented_box(m, bc, d, up, 0.135, 0.115, 0.185, "black")
    front = (bc[0] + d[0] * 0.135, bc[1] + d[1] * 0.135, bc[2] + d[2] * 0.135)
    # flat radar panel inset on the upper front
    pc = (front[0] + up[0] * 0.065 + d[0] * 0.004, front[1] + up[1] * 0.065 + d[1] * 0.004,
          front[2] + up[2] * 0.065 + d[2] * 0.004)
    oriented_box(m, pc, d, up, 0.008, 0.09, 0.085, "lens")
    # small camera lens + brim, lower front
    lc = (front[0] - up[0] * 0.095, front[1] - up[1] * 0.095, front[2] - up[2] * 0.095)
    lens_spot(m, (lc[0] + d[0] * 0.01, lc[1] + d[1] * 0.01, lc[2] + d[2] * 0.01), 0.038, d, "lens")
    # squared-off mini visor over the lens (flat shroud, not a rounded hood; sits low over the lens)
    vc = (lc[0] + up[0] * 0.05 + d[0] * 0.03, lc[1] + up[1] * 0.05 + d[1] * 0.03,
          lc[2] + up[2] * 0.05 + d[2] * 0.03)
    oriented_box(m, vc, d, up, 0.055, 0.05, 0.008, "black")
    return m


def build_ac3(camera_y=1.90, tilt_deg=7.0, body="white", metal="silver"):
    """GridSmart AC3: a rectangular box camera with a rectangular sun shroud on a thin pole + mast-arm
    clamp. Like the Autoscope but boxy. `tilt_deg` is the downward aim (7deg for the AC3's long-range
    lens; 0 = level/straight-out for the overheight detector). `body`/`metal` pick the housing and
    hardware materials (white/silver for the AC3, black/silver for the overheight)."""
    m = Mesh()
    a = math.radians(tilt_deg)
    d = (0.0, -math.sin(a), -math.cos(a))
    up = (0.0, math.cos(a), -math.sin(a))
    pole_z, pole_r = 0.32, 0.045
    sweep_tube(m, [(0.0, 0.06, pole_z), (0.0, camera_y, pole_z)], pole_r, metal, ring=10,
               cap_ends=True)
    sweep_tube(m, [(0.0, camera_y - 0.06, pole_z), (0.0, camera_y + 0.02, pole_z)], 0.066, metal,
               ring=12, cap_ends=True)
    box(m, -0.05, 0.05, 0.32, 0.58, pole_z + pole_r - 0.01, 0.50, metal)
    box(m, -0.08, 0.08, 0.38, 0.52, pole_z - 0.055, pole_z + 0.055, metal)
    mount = (0.0, camera_y + 0.02, pole_z)
    oriented_box(m, mount, d, up, 0.05, 0.06, 0.045, metal)
    bl, bw, bh = 0.42, 0.17, 0.16
    axis = (mount[0] + up[0] * (bh * 0.5 + 0.02), mount[1] + up[1] * (bh * 0.5 + 0.02),
            mount[2] + up[2] * (bh * 0.5 + 0.02))
    bc = (axis[0] + d[0] * (bl * 0.5 - 0.12), axis[1] + d[1] * (bl * 0.5 - 0.12),
          axis[2] + d[2] * (bl * 0.5 - 0.12))
    oriented_box(m, bc, d, up, bl * 0.5, bw * 0.5, bh * 0.5, body)
    front = (bc[0] + d[0] * bl * 0.5, bc[1] + d[1] * bl * 0.5, bc[2] + d[2] * bl * 0.5)
    oriented_box(m, (front[0] + d[0] * 0.005, front[1] + d[1] * 0.005, front[2] + d[2] * 0.005),
                 d, up, 0.008, bw * 0.42, bh * 0.42, "lens")
    sc = (bc[0] + up[0] * (bh * 0.5 + 0.013) + d[0] * 0.06,
          bc[1] + up[1] * (bh * 0.5 + 0.013) + d[1] * 0.06,
          bc[2] + up[2] * (bh * 0.5 + 0.013) + d[2] * 0.06)
    oriented_box(m, sc, d, up, bl * 0.58, bw * 0.54, 0.012, body)
    side_wire_loop(m, (bc[0] - d[0] * bl * 0.5 + up[0] * bh * 0.4,
                       bc[1] - d[1] * bl * 0.5 + up[1] * bh * 0.4,
                       bc[2] - d[2] * bl * 0.5 + up[2] * bh * 0.4), pole_z, camera_y)
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


def hood(mesh, start, d, up, length, radius, mat, n_along=4, n_arc=8, arc_deg=210.0,
         back_wall=False):
    """A contoured sun hood: the top portion of a cylinder (bottom cut off) swept along the camera
    axis `d`, sitting over the camera. Open underneath and at the ends, like a real shroud. If
    `back_wall`, the rear opening is filled in (double-sided) so you can't see through the hood."""
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
    if back_wall:
        # fill the rear opening (double-sided fan) so you can't see sky through the hood
        base, ring = rings[0]
        for j in range(n_arc):
            a, b = ring[j], ring[j + 1]
            mesh.tri(mat, base, a, b, (0.5, 0.5), (0.9, 0.5), (0.5, 0.9))
            mesh.tri(mat, base, b, a, (0.5, 0.5), (0.5, 0.9), (0.9, 0.5))


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


def _cap_fan(mesh, center, ringpts, inside_ref, mat):
    n = len(ringpts)
    for j in range(n):
        a, b = ringpts[j], ringpts[(j + 1) % n]
        if sum(_nrm(center, a, b)[k] * (center[k] - inside_ref[k]) for k in range(3)) < 0:
            a, b = b, a
        mesh.tri(mat, center, a, b, (0.5, 0.5), (0.9, 0.5), (0.5, 0.9))


def loft_along_axis(mesh, start, d, up, length, profile, mat, cap_back=True, cap_front=True,
                    ring=16):
    """Loft an elliptical cross-section along an axis to make a smooth rounded body. `profile` is a
    list of (t, half_width_along_side, half_height_along_up); t in 0..1 from start to start+d*length."""
    d = _unit(d)
    up = _unit(up)
    side = _unit(_cross(d, up))
    rings = []
    for (t, rx, ry) in profile:
        c = (start[0] + d[0] * length * t, start[1] + d[1] * length * t, start[2] + d[2] * length * t)
        rp = []
        for j in range(ring):
            a = 2 * math.pi * j / ring
            rp.append((c[0] + rx * math.cos(a) * side[0] + ry * math.sin(a) * up[0],
                       c[1] + rx * math.cos(a) * side[1] + ry * math.sin(a) * up[1],
                       c[2] + rx * math.cos(a) * side[2] + ry * math.sin(a) * up[2]))
        rings.append((c, rp))
    for i in range(len(profile) - 1):
        mid = tuple((rings[i][0][k] + rings[i + 1][0][k]) / 2 for k in range(3))
        r0, r1 = rings[i][1], rings[i + 1][1]
        for j in range(ring):
            j2 = (j + 1) % ring
            mesh.quad_out(mat, [r0[j], r0[j2], r1[j2], r1[j]],
                          [(0, 0), (1, 0), (1, 1), (0, 1)], mid)
    if cap_back:
        _cap_fan(mesh, rings[0][0], rings[0][1], rings[1][0], mat)
    if cap_front:
        _cap_fan(mesh, rings[-1][0], rings[-1][1], rings[-2][0], mat)
    return rings


def build_vantage_vector():
    """Iteris Vantage Vector: a smooth rounded white housing that is WIDER than it is deep (wide
    side-to-side, shallow front-to-back), with a recessed lens + overhanging brow at the front. Sits
    on top of a mast arm via a downward stub, with a cable drip-loop into the arm."""
    m = Mesh()
    cy, cz = 0.27, -0.02   # camera body sits low in the block, close to the mast arm
    half_w = 0.29
    # body lofted along +X (the WIDTH) so it is wide and shallow; cross-section is an ellipse in the
    # Z-Y (depth x height) plane, bulging in the middle and rounding off at the left/right ends.
    profile = [(0.00, 0.05, 0.05), (0.08, 0.135, 0.12), (0.22, 0.185, 0.155), (0.50, 0.20, 0.165),
               (0.78, 0.185, 0.155), (0.92, 0.135, 0.12), (1.00, 0.05, 0.05)]
    loft_along_axis(m, (-half_w, cy, cz), (1.0, 0.0, 0.0), (0.0, 1.0, 0.0), 2 * half_w, profile,
                    "white")
    # lens (toward -Z = road), aimed slightly down, RIGHT-aligned on the front of the housing
    d = _unit((0.0, -0.2, -1.0))
    fc = (-0.13, cy - 0.02, cz - 0.205)
    lr = 0.105
    sweep_tube(m, [(fc[0] - d[0] * 0.05, fc[1] - d[1] * 0.05, fc[2] - d[2] * 0.05),
                   (fc[0] + d[0] * 0.01, fc[1] + d[1] * 0.01, fc[2] + d[2] * 0.01)],
               lr, "white", ring=14, cap_ends=True)
    lens_spot(m, (fc[0] + d[0] * 0.02, fc[1] + d[1] * 0.02, fc[2] + d[2] * 0.02), lr * 0.62, d, "lens")
    # wide overhanging brow/hood over the lens (a shallow wide lip across the front-top)
    brow_d = _unit((0.0, -0.3, -0.95))
    brow_up = _unit((0.0, 0.95, -0.3))
    oriented_box(m, (0.0, cy + 0.10, cz - 0.20), brow_d, brow_up, 0.055, 0.23, 0.013, "white")
    # mounting stub extends ~1/2 block DOWN into the block below to meet the mast arm
    box(m, -0.06, 0.06, -0.5, cy - 0.10, cz - 0.05, cz + 0.05, "silver")
    # cable drip-loop, rotated 90deg (in the X-Y plane) and pushed out the back so it doesn't clip
    # down into the mast arm
    _vv_wire(m, cy, cz)
    return m


def _vv_wire(mesh, cy, cz):
    # Drip-loop in the X-Y plane (so it reads as a coil from behind), sitting low and offset to one
    # side, then dropping straight DOWN into the mast arm. up_seed=(0,0,1) keeps the X-Y-plane loop
    # from twisting; the vertical drop uses the default seed.
    lz = cz + 0.22                       # behind the housing
    lcx, lcy, r = 0.10, 0.07, 0.085      # low + offset to the +X side
    seed = (0.0, 0.0, 1.0)

    def oc(deg):
        a = math.radians(deg)
        return (lcx + r * math.cos(a), lcy + r * math.sin(a), lz)

    sweep_tube(mesh, [oc(360.0 * i / 20) for i in range(21)], WIRE_R, "wire", ring=6,
               cap_ends=False, up_seed=seed)
    # tail: out the housing back, down to the top of the loop
    sweep_tube(mesh, [(0.07, cy - 0.02, cz + 0.10), (0.10, cy - 0.14, lz - 0.02), oc(115.0)],
               WIRE_R, "wire", ring=6, up_seed=seed)
    # tail: from the loop straight down into the mast arm (block below)
    sweep_tube(mesh, [oc(245.0), (lcx, -0.32, lz)], WIRE_R, "wire", ring=6)


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


def add_bell(mesh, axis_xz, top_y):
    """GridSmart FE3 bell camera (per the product close-up): a ball-top mounting knob, a thin neck, a
    logo collar, a flaring bell body, and a wide stepped/flared rim skirt with a dark lens recessed
    underneath. Hangs from `top_y`."""
    ax, az = axis_xz
    sphere(mesh, (ax, top_y, az), 0.045, "white")            # ball-top mounting knob
    ny = top_y - 0.05                                        # neck starts just below the ball
    # neck (thin) -> logo collar -> flaring bell -> wider stepped rim skirt
    profile = [(ny, 0.028), (ny - 0.05, 0.052), (ny - 0.10, 0.075), (ny - 0.16, 0.085),
               (ny - 0.21, 0.09), (ny - 0.31, 0.135), (ny - 0.41, 0.165), (ny - 0.47, 0.175),
               (ny - 0.50, 0.20), (ny - 0.545, 0.218), (ny - 0.55, 0.218)]
    revolve(mesh, profile, axis_xz, "white")
    rim_y = ny - 0.55
    disc_down(mesh, axis_xz, 0.218, 0.12, rim_y, "white")    # flared rim underside (around the lens)
    lens_disc(mesh, axis_xz, 0.13, rim_y - 0.002, "lens")    # dark lens recessed in the rim


def build_bell():
    """GridSmart FE3 bell camera (per the original JSON layout): a round vertical pole clamped behind
    to a mast arm, with only a SMALL bend at the top and then a LONG horizontal arm reaching the bell
    far out in front, where a tall thin white bell hangs."""
    m = Mesh()
    pole_z, pr = 0.55, 0.05
    a0 = 2.60                            # tall vertical pole (closer to the Miovision-tall height)
    # ONE continuous pole + gooseneck (uniform thickness, single piece), extends below into the arm
    centerline = [(0.0, -0.45, pole_z), (0.0, a0, pole_z), (0.0, a0 + 0.12, pole_z),
                  (0.0, a0 + 0.18, pole_z - 0.05), (0.0, a0 + 0.215, pole_z - 0.14),
                  (0.0, a0 + 0.22, pole_z - 0.24), (0.0, a0 + 0.22, pole_z - 0.90),
                  (0.0, a0 + 0.22, pole_z - 1.62)]
    sweep_tube(m, centerline, pr, "silver", ring=12, cap_ends=True)
    # banding clamps + a small mount block near the bottom where it meets the mast arm
    for yb in (0.18, 0.38):
        box(m, -0.075, 0.075, yb, yb + 0.05, pole_z - 0.065, pole_z + 0.065, "silver")
    box(m, -0.06, 0.06, -0.04, 0.14, pole_z - 0.055, pole_z + 0.055, "silver")
    # bell hangs straight down at 90deg from the far end of the level arm
    add_bell(m, axis_xz=(0.0, pole_z - 1.62), top_y=a0 + 0.19)
    # cable drip-loop OFF TO THE SIDE of the pole (so it doesn't clip the lower pole), into the mount
    _fe3_wire(m, pole_z)
    return m


def _fe3_wire(mesh, pole_z):
    # The cable runs from the mount down the FULL length of the pole and hangs in a drip loop at the
    # very BOTTOM of the camera pole (like the Miovision). Offset to the +X side so it never runs
    # through the pole.
    lx, cz = 0.13, pole_z + 0.02
    cy, r = -0.42, 0.085                    # drip loop at the bottom of the pole

    def oc(deg):
        a = math.radians(deg)
        return (lx, cy + r * math.sin(a), cz + r * math.cos(a))

    sweep_tube(mesh, [oc(360.0 * i / 18) for i in range(19)], WIRE_R, "wire", ring=6, cap_ends=False)
    # cable from the mount, down the side of the pole, into the bottom loop
    sweep_tube(mesh, [(0.05, 0.24, pole_z + 0.04), (lx, 0.12, cz), (lx, -0.26, cz), oc(110.0)],
               WIRE_R, "wire", ring=6)
    # the loop's far end tucks back into the pole bottom / mast arm
    sweep_tube(mesh, [oc(250.0), (0.03, -0.45, pole_z + 0.02)], WIRE_R, "wire", ring=6)


def build_box():
    """Generic box camera: bake the existing JSON model unchanged (it already faces down nicely),
    swap the lens to the clean camera-lens texture, and add a cable that exits the back of the
    housing, drip-loops, and runs up into the mast arm. Returns (mesh, {matname: texture})."""
    ms = "csm:blocks/trafficsignals/shared_textures/metal_silver"
    mw = "csm:blocks/trafficsignals/shared_textures/metal_white"
    so = "csm:blocks/trafficsignals/shared_textures/solidoff"
    mb = "csm:blocks/trafficsignals/shared_textures/metal_black"
    cl = "csm:blocks/trafficsignals/shared_textures/camera_lens"
    mesh, mats = bake_model(os.path.join(OUT_DIR, "trafficpolecamera_box.json"),
                            {"0": ms, "1": mw, "2": so, "all": mw})   # mats = {texture: matname}
    if so in mats:
        mats[cl] = mats.pop(so)        # clean lens texture
    mats[mb] = "wire"
    # cable drip-loop in the X-Y plane (sits flat on the back of the housing, not clipping into it),
    # then DOWN into the mast arm below. up_seed=(0,0,1) keeps the X-Y loop from twisting.
    lcx, lcy, lz, r = 0.0, -0.03, 0.10, 0.055
    seed = (0.0, 0.0, 1.0)

    def oc(deg):
        a = math.radians(deg)
        return (lcx + r * math.cos(a), lcy + r * math.sin(a), lz)

    sweep_tube(mesh, [oc(360.0 * i / 16) for i in range(17)], WIRE_R, "wire", ring=5,
               cap_ends=False, up_seed=seed)
    sweep_tube(mesh, [(0.0, 0.10, -0.01), (0.0, 0.02, lz - 0.02), oc(95.0)], WIRE_R, "wire", ring=5,
               up_seed=seed)
    sweep_tube(mesh, [oc(250.0), (0.0, -0.18, lz), (0.0, -0.33, 0.02)], WIRE_R, "wire", ring=5)
    return mesh, {v: k for k, v in mats.items()}   # invert -> {matname: texture} for emit()


def emit(mesh, base, mat_to_tex):
    """Write base.obj (0..1) + base_inv.obj (origin-centred for inventory) + base.mtl."""
    # Guard: every material the geometry references must have a texture mapping, otherwise the OBJ
    # emits `usemtl <name>` for a material the .mtl never defines and Forge logs
    # "material '<name>' referenced but was not found" at load.
    missing = {mat for mat, _ in mesh.faces} - set(mat_to_tex) - {"none"}
    if missing:
        raise ValueError("%s: mesh uses materials with no texture mapping: %s"
                         % (base, ", ".join(sorted(missing))))
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
    cl = "csm:blocks/trafficsignals/shared_textures/camera_lens"   # clean smoked-glass lens
    emit(build_tiny_cam(), "trafficpolecamera_tiny_cam",
         {"black": mb, "lens": cl, "wire": mb, "silver": ms})
    aut = {"silver": ms, "white": mw, "lens": cl, "wire": mb}
    emit(build_autoscope(2.30), "trafficpolecamera", aut)
    emit(build_autoscope(1.25, body_len=0.34, cam_r=0.085), "trafficpolecamera_short", aut)
    emit(build_vantage_vector(), "vantage_vector", {"white": mw, "silver": ms, "lens": cl,
                                                    "wire": mb})
    emit(build_trafiradar(), "trafiradar", {"black": mb, "silver": ms, "lens": cl, "wire": mb})
    emit(build_ac3(), "gridsmart_ac3", aut)
    # GridSmart AC3 mount-anchored diagonal variants (the block is now 8-way like the Miovision).
    # Pivot the camera +/-45deg about the AC3's own pole/clamp axis (build z=0.32) so the diagonals
    # keep the pole vertical and static while the camera swings, instead of rotating the whole unit
    # about the block centre. The blockstate places these at 90deg-step rotations (see the *.json).
    for sfx, deg in (("diagr", 45.0), ("diagl", -45.0)):
        dm = build_ac3()
        dm.rotate_y(deg, 0.0, 0.32)
        dm.write(os.path.join(OUT_DIR, "gridsmart_ac3_%s.obj" % sfx), "gridsmart_ac3.mtl")
    emit(build_bell(), "trafficpolecamera_bell", {"white": mw, "silver": ms, "lens": cl,
                                                  "wire": mb})
    # GridSmart FE3 (bell) mount-anchored aim-angle variants. The bell sensor keeps a cardinal
    # facing plus a configurable aim angle (none/left/right); LEFT/RIGHT render these models, the
    # bell arm swung +/-45deg about the pole/clamp axis (build z=pole_z=0.55) so the pole stays put.
    for sfx, deg in (("diagr", 45.0), ("diagl", -45.0)):
        dm = build_bell()
        dm.rotate_y(deg, 0.0, 0.55)
        dm.write(os.path.join(OUT_DIR, "trafficpolecamera_bell_%s.obj" % sfx),
                 "trafficpolecamera_bell.mtl")
    box_mesh, box_mats = build_box()
    emit(box_mesh, "trafficpolecamera_box", box_mats)
    # overheight detector: AC3 shape, black, facing straight out (level)
    emit(build_ac3(tilt_deg=0.0, body="black", metal="silver"), "overheight_detection_sensor",
         {"black": mb, "silver": ms, "lens": cl, "wire": mb})
    print("generated tiny_cam, autoscope, vantage_vector, trafiradar, ac3, overheight")


if __name__ == "__main__":
    main()
