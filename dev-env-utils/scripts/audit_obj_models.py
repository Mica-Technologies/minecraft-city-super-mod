#!/usr/bin/env python3
"""
Audits Forge OBJ block models for the geometry faults that only show up once they are in game.

These are the ones that cost a round trip each to find by eye, and every one of them was a real bug
in the decorative lighting family before this existed:

  * COPLANAR OVERLAP -- two triangles in one plane, facing the same way, projections overlapping.
    Two surfaces at one depth is z-fighting, and it shimmers. A boss sitting exactly on the face of
    the plate behind it does this.
  * ON A BLOCK FACE -- a triangle lying on x/y/z = 0 or 16 whose normal points back into the block.
    It is then at exactly the depth of the NEIGHBOURING block's own inward face, which is also
    drawn, so the two fight. This is why a wall fixture's geometry must stand off z = 16 rather
    than sit on it. A face on the boundary pointing OUT of the block is culled and harmless.
  * INCONSISTENT WINDING -- two triangles share an edge and traverse it the same way, so one is
    inside-out and culls from the side you are looking at. This is what makes the top of a shade
    disappear when viewed from above. Reported as a warning rather than a failure: two DIFFERENT
    open surfaces meeting at a seam trip it legitimately, so check the render before acting.
  * SEE-THROUGH -- rays from fourteen directions whose nearest hit is a BACK face. Back-face
    culling draws nothing there, so you look straight through the model. This is the check that
    matters: a bowl modelled as one skin trips neither the winding nor the boundary count and is
    still invisible from above, which is exactly the bug it was added for.
  * BOUNDARY EDGES -- an edge used by one triangle only, so the surface is open there. Counted, not
    judged: a shade's neck and a canopy's top are open on purpose. A jump in the count after an
    edit is the signal worth reading.

Usage:
    python audit_obj_models.py                     # every lighting shared model
    python audit_obj_models.py <model.obj> [...]   # specific files

Exits non-zero if anything FAILs, so it can gate a commit.
"""

import glob
import itertools
import math
import os
import sys

import numpy as np

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DEFAULT_GLOB = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm", "models",
                            "block", "lighting", "shared_models", "*.obj")

QUANT = 1e-4


def load(path):
    positions, triangles = [], []
    for line in open(path, encoding="utf-8"):
        parts = line.split()
        if not parts:
            continue
        if parts[0] == "v":
            positions.append(tuple(float(v) for v in parts[1:4]))
        elif parts[0] == "f":
            idx = [int(p.split("/")[0]) - 1 for p in parts[1:4]]
            triangles.append(tuple(positions[i] for i in idx))
    return triangles


def key(point):
    return tuple(round(c / QUANT) for c in point)


def normal(tri):
    (ax, ay, az), (bx, by, bz), (cx, cy, cz) = tri
    ux, uy, uz = bx - ax, by - ay, bz - az
    vx, vy, vz = cx - ax, cy - ay, cz - az
    n = (uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
    length = math.sqrt(sum(c * c for c in n))
    return tuple(c / length for c in n) if length > 1e-12 else (0.0, 0.0, 0.0)


def winding_and_boundary(triangles):
    directed = {}
    for index, tri in enumerate(triangles):
        keys = [key(p) for p in tri]
        for a, b in ((keys[0], keys[1]), (keys[1], keys[2]), (keys[2], keys[0])):
            directed.setdefault(frozenset((a, b)), []).append((a, b, index))

    inconsistent, boundary = [], []
    for edge, uses in directed.items():
        if len(uses) == 1:
            boundary.append(uses[0][2])
        elif len(uses) == 2:
            (a0, b0, i0), (a1, b1, i1) = uses
            # Consistent orientation traverses a shared edge in opposite directions.
            if (a0, b0) == (a1, b1):
                inconsistent.append((i0, i1))
    return inconsistent, boundary


def plane_key(tri):
    n = normal(tri)
    d = sum(n[i] * tri[0][i] for i in range(3))
    return tuple(round(c, 3) for c in n) + (round(d, 3),)


def project(tri, n):
    axis = min(range(3), key=lambda i: abs(n[i]))
    basis = [0.0, 0.0, 0.0]
    basis[axis] = 1.0
    u = _norm(_cross(n, basis))
    v = _cross(n, u)
    return [(sum(p[i] * u[i] for i in range(3)), sum(p[i] * v[i] for i in range(3))) for p in tri]


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _norm(a):
    length = math.sqrt(sum(c * c for c in a)) or 1.0
    return tuple(c / length for c in a)


def tris_overlap(p, q):
    """Separating-axis test on two 2D triangles. Touching edges do not count as overlap."""
    for poly in (p, q):
        for i in range(3):
            (x0, y0), (x1, y1) = poly[i], poly[(i + 1) % 3]
            ax, ay = -(y1 - y0), x1 - x0
            pv = [ax * x + ay * y for x, y in p]
            qv = [ax * x + ay * y for x, y in q]
            if min(pv) >= max(qv) - 1e-6 or min(qv) >= max(pv) - 1e-6:
                return False
    return True


def coplanar_overlaps(triangles):
    planes = {}
    for index, tri in enumerate(triangles):
        planes.setdefault(plane_key(tri), []).append(index)
    hits = []
    for group in planes.values():
        if len(group) < 2:
            continue
        for i, j in itertools.combinations(group, 2):
            a, b = triangles[i], triangles[j]
            n = normal(a)
            # Same plane AND same facing. Opposite-facing coplanar pairs are back-to-back surfaces,
            # which the renderer separates by culling and which never fight.
            if sum(n[k] * normal(b)[k] for k in range(3)) < 0.9:
                continue
            if tris_overlap(project(a, n), project(b, n)):
                hits.append((i, j))
    return hits


def on_block_boundary(triangles):
    """Faces lying on a block face AND visible from inside this block.

    A triangle on z = 16 whose normal points back into the block is at exactly the depth of the
    neighbouring block's own inward face, which is also visible. The depth buffer cannot separate
    them, so the surface shimmers. The same face pointing OUT of the block is culled and harmless.
    """
    hits = []
    for index, tri in enumerate(triangles):
        n = normal(tri)
        for axis in range(3):
            values = [round(p[axis], 4) for p in tri]
            if len(set(values)) != 1:
                continue
            plane = values[0]
            if plane not in (0.0, 1.0):
                continue
            # Inward means pointing away from the boundary, into this block's interior.
            inward = 1.0 if plane == 0.0 else -1.0
            if n[axis] * inward > 0.9:
                hits.append((index, "xyz"[axis], plane))
    return hits



#: View directions for the see-through test: the six axes plus the eight corners.
VIEW_DIRECTIONS = ([(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
                   + [(x, y, z) for x in (-1, 1) for y in (-1, 1) for z in (-1, 1)])
RAY_GRID = 44


def see_through(triangles, samples=RAY_GRID):
    """Rays whose NEAREST hit is a back face -- i.e. you look straight through the model.

    This is the check that matters, because it asks the question the eye asks. Winding and boundary
    counts are proxies: a seam between two open surfaces trips them harmlessly, while a bowl
    modelled as a single skin trips neither and is still invisible from above. Back-face culling
    means the first thing a ray meets must be facing the ray; if it is facing away, nothing is
    drawn there and you see whatever is behind the model.
    """
    if not triangles:
        return []
    tris = np.array(triangles, dtype=np.float64)
    v0, v1, v2 = tris[:, 0], tris[:, 1], tris[:, 2]
    edge1, edge2 = v1 - v0, v2 - v0
    normals = np.cross(edge1, edge2)
    lengths = np.linalg.norm(normals, axis=1)
    keep = lengths > 1e-12
    v0, edge1, edge2 = v0[keep], edge1[keep], edge2[keep]
    normals = normals[keep] / lengths[keep, None]

    lo, hi = tris.reshape(-1, 3).min(axis=0), tris.reshape(-1, 3).max(axis=0)
    centre, radius = (lo + hi) / 2.0, float(np.linalg.norm(hi - lo)) / 2.0 + 1e-3

    failures = []
    for direction in VIEW_DIRECTIONS:
        d = np.array(direction, dtype=np.float64)
        d /= np.linalg.norm(d)
        # Two axes across the view, so the rays tile the model's silhouette.
        helper = np.array([0.0, 1.0, 0.0]) if abs(d[1]) < 0.9 else np.array([1.0, 0.0, 0.0])
        u = np.cross(d, helper)
        u /= np.linalg.norm(u)
        w = np.cross(d, u)
        steps = np.linspace(-radius, radius, samples)
        grid_u, grid_w = np.meshgrid(steps, steps)
        origins = (centre - d * radius * 2.0
                   + grid_u.reshape(-1, 1) * u + grid_w.reshape(-1, 1) * w)

        best = np.full(len(origins), np.inf)
        best_facing = np.zeros(len(origins), dtype=bool)
        for i in range(len(v0)):
            # Moller-Trumbore, vectorised over rays for one triangle.
            pvec = np.cross(d, edge2[i])
            det = edge1[i] @ pvec
            if abs(det) < 1e-12:
                continue
            tvec = origins - v0[i]
            uu = (tvec @ pvec) / det
            qvec = np.cross(tvec, edge1[i])
            vv = (qvec @ d) / det
            tt = (edge2[i] @ qvec.T) / det
            hit = (uu >= 0) & (vv >= 0) & (uu + vv <= 1) & (tt > 1e-6) & (tt < best)
            if not hit.any():
                continue
            best[hit] = tt[hit]
            best_facing[hit] = (normals[i] @ d) < 0.0

        seen = np.isfinite(best)
        through = seen & ~best_facing
        if through.any():
            failures.append((direction, int(through.sum()), int(seen.sum())))
    return failures


def main():
    worst = 0
    paths = sys.argv[1:] or sorted(glob.glob(DEFAULT_GLOB))
    for path in paths:
        triangles = load(path)
        inconsistent, boundary = winding_and_boundary(triangles)
        overlaps = coplanar_overlaps(triangles)
        boundary_faces = on_block_boundary(triangles)
        holes = see_through(triangles)
        name = path.replace("\\", "/").rsplit("/", 1)[-1]
        flag = "FAIL" if (overlaps or boundary_faces or holes) else ("warn" if inconsistent else "ok  ")
        print("%s %-32s tris=%4d  winding=%-3d coplanar=%-3d onface=%-3d seethru=%-2d boundary=%d"
              % (flag, name, len(triangles), len(inconsistent), len(overlaps),
                 len(boundary_faces), len(holes), len(boundary)))
        for direction, count, seen in holes[:4]:
            print("        see-through looking along %s: %d of %d rays hit a back face first"
                  % (direction, count, seen))
        for i, j in overlaps[:4]:
            centre = [sum(t[k] * 16 for t in triangles[i]) / 3 for k in range(3)]
            print("        coplanar overlap near (%.2f, %.2f, %.2f)" % tuple(centre))
        for index, axis, plane in boundary_faces[:3]:
            centre = [sum(t[k] * 16 for t in triangles[index]) / 3 for k in range(3)]
            print("        on the %s=%g block face, facing inward, at (%.2f, %.2f, %.2f)"
                  % (axis, plane * 16, *centre))
        for i, j in inconsistent[:4]:
            centre = [sum(t[k] * 16 for t in triangles[i]) / 3 for k in range(3)]
            print("        winding flip near (%.2f, %.2f, %.2f)" % tuple(centre))
        worst = max(worst, len(overlaps) + len(boundary_faces) + len(holes))
    return 1 if worst else 0


if __name__ == "__main__":
    sys.exit(main())
