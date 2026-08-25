#!/usr/bin/env python3
"""
Procedural generator for the CSM fire alarm appliances whose strobe lens is ROUND.

Four System Sensor L-Series LED devices and the four-colour beacon all wear textures photographed
from real appliances, and in every one of them the strobe lens is a circle. The hand-built JSON
models these blocks shipped with were adapted from rectangular-xenon siblings, so each lens was a
square box: the circle sat inscribed in a raised square, the enclosure's own colour showed on the
four corners that stuck out past it, and the box's 1-deep side faces each carried the whole 4.5-wide
lens UV smeared across them. This rebuilds them as OBJ, where a lens can simply be round.

Emits (into assets/csm/models/block/lifesafety/shared_models/):

  * systemsensor_lseries_led_hornstrobe.obj          -- wall horn strobe
  * systemsensor_lseries_led_speakerstrobe.obj       -- wall speaker strobe
  * systemsensor_lseries_led_speakerstrobe_ceiling.obj -- ceiling speaker strobe, a real dome
  * systemsensor_lseries_led_hornstrobe_outdoor.obj  -- weatherproof unit, faceplate on a backbox
  * firealarm_beacon.obj                             -- round Fresnel barrel on a mount plate

plus one shared MTL per model. Each model uses a single material so the blockstate can retexture it
per colour variant with `"textures": {"#body": "csm:blocks/lifesafety/..."}` -- Forge's OBJ loader
keys texture overrides on `#` + material name.

Two techniques do most of the work:

  * Silhouette tracing. The enclosure outline is read straight off the texture's alpha channel by
    ray-casting from the centroid, so the model's front cap is exactly the shape of the device in
    the photo -- rounded corners included. Because the front cap's UVs are the traced points
    themselves, the mapping is the identity and cannot bleed into the transparent margin. The side
    wall takes a flat patch of the enclosure's own surface (see find_flat_patch), which is what a
    plain moulded flank looks like.

  * Radial lens mapping. The lens is a shallow dome whose vertices are placed by angle and radius,
    and UV'd by that same angle and radius inside the circle the lens occupies in the texture. The
    texture is a head-on photograph of a round lens, so a radial map is the projection that made it.

Coordinate convention (matches gen_miovision_obj.py and Forge's block space):
  * 1 unit == 1 block; the file is written in 0..1 with the block's min corner at the origin, so a
    blockstate `x`/`y` rotation -- which pivots about (0.5, 0.5, 0.5) -- lands where it should.
  * Geometry here is authored in Minecraft's familiar 0..16 model units and divided at write time.
  * -Z is north, which is the front of every wall-mounted device in this mod. Lenses protrude
    toward smaller z.
  * Faces are triangles `f v/vt/vn`, wound so every normal points outward. Forge culls back faces,
    so inverted winding makes a model see-through.

UVs are written for `custom: {"flip-v": true}` in the blockstate, matching every other OBJ here:
Minecraft UV rows run top-down, OBJ v runs bottom-up, so vt_v = 1 - V/16.

Run:  python dev-env-utils/scripts/gen_firealarm_obj.py
"""

import math
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
# How much of the E7070 module's depth is clear lens cap. Imported rather than repeated so the
# geometry and the flash cannot drift from the art that gen_e7070_strobe_texture.py draws.
from gen_e7070_strobe_texture import GLASS_FRACTION as E7070_GLASS_FRACTION

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "csm")
OUT_DIR = os.path.join(ASSETS, "models", "block", "lifesafety", "shared_models")
TEX_DIR = os.path.join(ASSETS, "textures", "blocks", "lifesafety")

MATERIAL = "body"

#: The colour variants each model is retextured to. A flank's flat patch has to be plain in all of
#: them, since one model wears every colour in its row.
VARIANTS = {
    "system_sensor_l_series_led_red_horn_strobe": ["system_sensor_l_series_led_white_horn_strobe"],
    "system_sensor_l_series_led_red_speaker_strobe":
        ["system_sensor_l_series_led_white_speaker_strobe",
         "system_sensor_l_series_led_black_speaker_strobe"],
    "system_sensor_l_series_led_red_ceiling_speaker_strobe":
        ["system_sensor_l_series_led_white_ceiling_speaker_strobe",
         "system_sensor_l_series_led_black_ceiling_speaker_strobe"],
    "system_sensor_l_series_led_red_outdoor_horn_strobe": [],
    "system_sensor_spectralert_advance_lf_red_horn_strobe":
        ["system_sensor_spectralert_advance_lf_white_horn_strobe"],
    "system_sensor_spectralert_classic_red_horn_strobe":
        ["system_sensor_spectralert_classic_white_horn_strobe"],
}


def housing_patch(texture, size=0.8):
    """The flat patch of housing to give this device's flanks, valid across its colour variants."""
    return find_flat_patch([texture] + VARIANTS.get(texture, []), size)

#: Segments around a lens dome or a barrel. 24 is smooth at the size these render at.
RING_SEGMENTS = 24
#: Points traced around an enclosure silhouette.
CONTOUR_POINTS = 64
#: How far inside the alpha boundary the traced contour is pulled, in UV units. These textures have
#: a soft antialiased edge, and a vertex sitting exactly on it renders as a chewed silhouette under
#: CUTOUT_MIPPED. Geometry and UV shrink together, so the art still lands where it was traced from.
CONTOUR_SHRINK = 0.22

#: Shallow-dome profile as (depth fraction, radius fraction) from rim to face. The rim rolls over
#: quickly and the face stays broad and flat, which is the shape of an L-Series LED lens: a chrome
#: reflector cup under a barely-convex clear cover, not a hemisphere.
DOME_PROFILE = [(0.00, 1.000), (0.40, 0.975), (0.70, 0.925), (0.90, 0.820), (1.00, 0.620)]


# --------------------------------------------------------------------------- mesh
class Mesh:
    """Accumulates triangles and writes them out as an OBJ with one material."""

    def __init__(self):
        self.positions = []
        self.texcoords = []
        self.normals = []
        self.faces = []

    def _position(self, point):
        self.positions.append(point)
        return len(self.positions)

    def _texcoord(self, uv):
        # Clamp a hair inside the sprite. Minecraft's atlas bleeds neighbouring sprites into the
        # outermost texel row when mipmapping, and these are CUTOUT_MIPPED blocks.
        self.texcoords.append((min(15.99, max(0.01, uv[0])) / 16.0,
                               1.0 - min(15.99, max(0.01, uv[1])) / 16.0))
        return len(self.texcoords)

    def _normal(self, normal):
        self.normals.append(normal)
        return len(self.normals)

    def triangle(self, p1, p2, p3, uv1, uv2, uv3, outward, material=None):
        """Emit one triangle, reversing the winding if it faces away from `outward`."""
        normal = _face_normal(p1, p2, p3)
        if _dot(normal, outward) < 0.0:
            p2, p3 = p3, p2
            uv2, uv3 = uv3, uv2
            normal = _face_normal(p1, p2, p3)
        index = self._normal(normal)
        self.faces.append((material or MATERIAL,
                           [(self._position(p1), self._texcoord(uv1), index),
                            (self._position(p2), self._texcoord(uv2), index),
                            (self._position(p3), self._texcoord(uv3), index)]))

    def quad(self, p1, p2, p3, p4, uv1, uv2, uv3, uv4, outward, material=None):
        self.triangle(p1, p2, p3, uv1, uv2, uv3, outward, material)
        self.triangle(p1, p3, p4, uv1, uv3, uv4, outward, material)

    def write(self, path, name, texture, header, extra_materials=None):
        """Write the OBJ and its MTL.

        `texture` is the main material's. `extra_materials` maps any additional material name to
        its texture, for a model whose parts wear different art and must be retextured
        independently: the E7070's plate changes colour per variant while the strobe module
        clipped to it does not.
        """
        materials = [(MATERIAL, texture)] + sorted((extra_materials or {}).items())
        mtl_name = os.path.splitext(os.path.basename(path))[0] + ".mtl"
        with open(os.path.join(os.path.dirname(path), mtl_name), "w", newline="\n") as handle:
            handle.write("# Procedurally generated by gen_firealarm_obj.py\n")
            for material, material_texture in materials:
                handle.write("newmtl %s\nmap_Kd %s\n" % (material, material_texture))
        with open(path, "w", newline="\n") as handle:
            handle.write("# Procedurally generated by dev-env-utils/scripts/gen_firealarm_obj.py\n")
            handle.write("# %s\n" % header)
            handle.write("mtllib %s\no %s\n" % (mtl_name, name))
            for x, y, z in self.positions:
                handle.write("v %.6f %.6f %.6f\n" % (x / 16.0, y / 16.0, z / 16.0))
            for u, v in self.texcoords:
                handle.write("vt %.6f %.6f\n" % (u, v))
            for x, y, z in self.normals:
                handle.write("vn %.6f %.6f %.6f\n" % (x, y, z))
            for material, _ in materials:
                group = [corners for used, corners in self.faces if used == material]
                if not group:
                    continue
                handle.write("usemtl %s\n" % material)
                for corners in group:
                    handle.write("f " + " ".join("%d/%d/%d" % c for c in corners) + "\n")
        return len(self.faces)


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _face_normal(p1, p2, p3):
    ux, uy, uz = p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]
    vx, vy, vz = p3[0] - p1[0], p3[1] - p1[1], p3[2] - p1[2]
    nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
    length = math.sqrt(nx * nx + ny * ny + nz * nz)
    if length < 1e-9:
        return (0.0, 0.0, -1.0)
    return (nx / length, ny / length, nz / length)


# --------------------------------------------------------------------------- texture reading
def alpha_of(texture_name):
    path = os.path.join(TEX_DIR, texture_name + ".png")
    image = np.asarray(Image.open(path).convert("RGBA"))[..., 3]
    return image, image.shape[1] / 16.0


def opaque_bounds(texture_name, threshold=128):
    """The texture's opaque bounding box, in MC UV units."""
    alpha, scale = alpha_of(texture_name)
    rows, cols = np.nonzero(alpha > threshold)
    return (cols.min() / scale, rows.min() / scale,
            (cols.max() + 1) / scale, (rows.max() + 1) / scale)


def measure_lens(texture_name):
    """Find the round strobe lens in a texture and return its circle as a UV rect.

    Reading the lens off the art rather than hand-copying a rect is the point: these four devices
    look alike but do not put their lens in the same place -- the speaker strobe's sits a good three
    units higher on the face than the horn strobe's -- and a hand-copied rect silently inherits the
    wrong one.

    Two features carry the measurement. The LED die is a small saturated yellow square at the exact
    centre of the lens, which fixes the centre. The radius comes from walking outward until the
    enclosure's saturated red takes over; the chrome reflector cup, the clear cover and the die
    itself are all "not red", so the walk stops at the rim. It is measured on the red texture of
    each pair -- the white enclosure is too close to the chrome to separate, and both colours are
    the same appliance.
    """
    image = np.asarray(Image.open(os.path.join(TEX_DIR, texture_name + ".png")).convert("RGB"),
                       dtype=float)
    height, width, _ = image.shape
    scale = width / 16.0
    red, green, blue = image[..., 0], image[..., 1], image[..., 2]

    die = (red > 150) & (green > 140) & (blue < 120) & (np.abs(red - green) < 70)
    rows, cols = np.nonzero(die)
    if len(rows) == 0:
        raise ValueError("no LED die found in %s -- cannot locate its lens" % texture_name)
    centre_x, centre_y = cols.mean(), rows.mean()

    not_enclosure = ~(red - np.maximum(green, blue) > 45)
    radii = []
    for k in range(72):
        angle = 2.0 * math.pi * k / 72
        dx, dy = math.cos(angle), math.sin(angle)
        reach, gap = 0.0, 0
        for step in range(1, int(6 * scale)):
            x, y = int(round(centre_x + dx * step)), int(round(centre_y + dy * step))
            if not (0 <= x < width and 0 <= y < height):
                break
            if not_enclosure[y, x]:
                reach, gap = step, 0
            else:
                gap += 1
                if gap > 0.25 * scale:
                    break
        radii.append(reach / scale)
    radius_x = (radii[0] + radii[36]) / 2.0
    radius_y = (radii[18] + radii[54]) / 2.0
    if not (1.0 < radius_x < 4.0 and 1.0 < radius_y < 4.0):
        raise ValueError("implausible lens radius (%.2f, %.2f) in %s"
                         % (radius_x, radius_y, texture_name))
    return (centre_x / scale - radius_x, centre_y / scale - radius_y,
            centre_x / scale + radius_x, centre_y / scale + radius_y)


def trace_silhouette(texture_name, points=CONTOUR_POINTS, threshold=128, uv_box=None):
    """Trace the outline of the opaque region as `points` (u, v) pairs in MC UV units.

    The boundary is walked pixel by pixel (Moore-neighbour tracing) and then resampled at even
    arc length, rather than sampled along rays from the centre. A radial sample is fine on a shape
    that is nearly a circle but chops the corners off a tall rounded rectangle, because the corners
    are where the outline moves fastest per degree and get the fewest samples -- which showed up as
    a visibly octagonal SpectrAlert backplate.
    """
    alpha, scale = alpha_of(texture_name)
    height, width = alpha.shape
    if uv_box:
        mask = np.zeros_like(alpha, dtype=bool)
        u0, v0, u1, v1 = [int(round(value * scale)) for value in uv_box]
        mask[v0:v1, u0:u1] = True
        alpha = np.where(mask, alpha, 0)
    solid = alpha > threshold
    if not solid.any():
        raise ValueError("%s has no opaque pixels to trace" % texture_name)

    rows, cols = np.nonzero(solid)
    centre_uv = (cols.mean() / scale, rows.mean() / scale)

    # Moore-neighbour boundary trace, clockwise from the first opaque pixel in raster order.
    start = (int(rows[0]), int(cols[0]))
    neighbours = [(-1, 0), (-1, 1), (0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1)]

    def opaque(y, x):
        return 0 <= y < height and 0 <= x < width and solid[y, x]

    boundary = [start]
    current, backtrack = start, 6           # came from the west
    guard = 8 * (width + height)
    for _ in range(guard):
        found = False
        for k in range(8):
            index = (backtrack + 1 + k) % 8
            dy, dx = neighbours[index]
            candidate = (current[0] + dy, current[1] + dx)
            if opaque(*candidate):
                backtrack = (index + 5) % 8
                current = candidate
                boundary.append(current)
                found = True
                break
        if not found or (len(boundary) > 3 and current == start):
            break
    if len(boundary) < points:
        raise ValueError("boundary trace of %s collapsed (%d points)"
                         % (texture_name, len(boundary)))

    # Resample at even arc length, averaging every dense boundary pixel that falls in each bin
    # rather than picking one of them. The walk follows an antialiased alpha edge pixel by pixel,
    # so the raw outline is a staircase; a bin holds roughly ten pixels, and their mean cancels
    # that +/-1px jitter. Doing it here rather than smoothing the decimated outline afterwards is
    # what keeps the corners: jitter is high frequency and averages away inside one bin, while a
    # corner's real curvature spans many bins and survives. Smoothing after decimation instead
    # visibly softened corners that these mouldings do not actually have -- the housings lost
    # several percent of their area to it.
    lengths = [0.0]
    for a, b in zip(boundary, boundary[1:]):
        lengths.append(lengths[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    total = lengths[-1]
    if total <= 0.0:
        raise ValueError("degenerate boundary for %s" % texture_name)

    bins = [[] for _ in range(points)]
    for index, (y, x) in enumerate(boundary):
        bins[min(points - 1, int(points * lengths[index] / total))].append(
            (x / scale, y / scale))
    contour, previous = [], None
    for bucket in bins:
        if bucket:
            previous = (sum(b[0] for b in bucket) / len(bucket),
                        sum(b[1] for b in bucket) / len(bucket))
        if previous is not None:
            contour.append(previous)
    if len(contour) < points // 2:
        raise ValueError("boundary of %s resampled to only %d points"
                         % (texture_name, len(contour)))

    contour = [shrink_toward(point, centre_uv, CONTOUR_SHRINK) for point in contour]
    return contour, centre_uv


def find_flat_patch(texture_names, size=0.8, within=None):
    """The smoothest fully-opaque square of enclosure surface shared by a model's colour variants.

    An enclosure's side wall is a plain moulded flank, so it wants a plain colour. Sampling it
    from a band inset inside the front outline -- the obvious trick, and what this generator did
    first -- only works when the art near the edge happens to be plain. On the L-Series LED units
    the speaker grille runs almost to the rim, so that band smeared perforations down the sides of
    the housing. Picking one flat patch instead gives the flank a single housing colour, and the
    face normals still shade it away from the front.

    The patch has to be flat in every texture the model wears, since one model serves red and white.
    `within` restricts the search to one region, for when a particular part's own surface is wanted
    rather than the enclosure's -- a strobe lens flank should be frosted white, not housing red.
    """
    images = []
    for name in texture_names:
        data = np.asarray(Image.open(os.path.join(TEX_DIR, name + ".png")).convert("RGBA"),
                          dtype=float)
        images.append(data)
    scale = images[0].shape[1] / 16.0
    window = max(1, int(size * scale))
    u_lo, v_lo, u_hi, v_hi = within or (0.0, 0.0, 16.0, 16.0)
    # A whole-texture sweep steps coarsely; a restricted region can be only a couple of units
    # across, so it needs a finer one to have any candidates at all.
    step = 0.25 if within is None else 0.05
    best = None
    for v in np.arange(v_lo, v_hi - size, step):
        for u in np.arange(u_lo, u_hi - size, step):
            y0, x0 = int(v * scale), int(u * scale)
            roughness, usable = 0.0, True
            for data in images:
                block = data[y0:y0 + window, x0:x0 + window]
                if block.size == 0 or block[..., 3].min() < 250:
                    usable = False
                    break
                roughness = max(roughness, float(block[..., :3].reshape(-1, 3).std(0).mean()))
            if usable and (best is None or roughness < best[0]):
                best = (roughness, float(u), float(v))
    if best is None:
        raise ValueError("no opaque flat patch found in %s" % (texture_names,))
    _, u, v = best
    # Pull in a touch so the patch cannot catch a neighbouring feature at the texel level.
    return (u + 0.1, v + 0.1, u + size - 0.1, v + size - 0.1)


def shrink_toward(point, centre, amount):
    """Move a UV point `amount` units toward `centre`, for sampling just inside the silhouette."""
    dx, dy = point[0] - centre[0], point[1] - centre[1]
    length = math.hypot(dx, dy)
    if length < 1e-6:
        return point
    factor = max(0.0, (length - amount)) / length
    return (centre[0] + dx * factor, centre[1] + dy * factor)


# --------------------------------------------------------------------------- mapping
class FrontMap:
    """Maps texture UV to model X/Y across a device's north-facing front.

    Both axes flip. A texture's v axis runs downward while a model's y axis runs upward, so v0 (the
    top of the art) lands on y1 (the top of the model). And u runs opposite to x: the front of a
    wall device faces north, and looking at a north face from outside puts world +X (east) on the
    viewer's left, which is where u0 goes. Getting this backwards mirrors the art -- which is
    invisible on a symmetrical grille and glaring on the FIRE lettering beside it.
    """

    def __init__(self, uv_rect, model_rect):
        self.u0, self.v0, self.u1, self.v1 = uv_rect
        self.x0, self.y0, self.x1, self.y1 = model_rect

    def to_model(self, uv):
        u, v = uv
        x = self.x1 - (u - self.u0) * (self.x1 - self.x0) / (self.u1 - self.u0)
        y = self.y1 - (v - self.v0) * (self.y1 - self.y0) / (self.v1 - self.v0)
        return x, y

    def to_uv(self, point):
        """Inverse of to_model: where on the texture does this point of the front face sit?"""
        x, y = point
        u = self.u0 + (self.x1 - x) * (self.u1 - self.u0) / (self.x1 - self.x0)
        v = self.v0 + (self.y1 - y) * (self.v1 - self.v0) / (self.y1 - self.y0)
        return u, v

    @property
    def scale_x(self):
        return (self.x1 - self.x0) / (self.u1 - self.u0)

    @property
    def scale_y(self):
        return (self.y1 - self.y0) / (self.v1 - self.v0)


# --------------------------------------------------------------------------- builders
def build_shell(mesh, contour_uv, centre_uv, front_map, z_front, z_back, wall_uv,
                back_cap=True, side_strips=None, wall_material=None):
    """An enclosure: traced front cap, extruded side wall, optional flat back.

    The front cap's UVs are the traced points themselves, so the art lands on the geometry it was
    traced from -- an identity mapping, which cannot bleed into the transparent margin. `wall_uv`
    is a flat patch of enclosure surface for the flanks. `wall_material` puts the flanks on a
    material of their own, for a device whose sides carry art the front-on photograph cannot hold
    -- the E50's FIRE legend runs down its flanks and appears nowhere on its plate.

    `side_strips` is for a texture that photographs the device's side walls separately, as the
    weatherproof unit's does: {"east": (u_at_front, u_at_back), "west": (...)}. Note that the two
    ends are not interchangeable: texture u runs opposite to model x on a north-facing front, so
    the LEFT strip of the art is the device's EAST flank and its edge adjacent to the faceplate is
    the one that meets the front. Wall segments
    facing mostly left or right then take their colour from the matching strip -- so the FIRE
    lettering down the real unit's flank lands on the flank -- while the top and bottom segments
    fall back to sampling inside the outline.
    """
    centre_model = front_map.to_model(centre_uv)
    points = [front_map.to_model(uv) for uv in contour_uv]
    count = len(points)

    front_centre = (centre_model[0], centre_model[1], z_front)
    for i in range(count):
        j = (i + 1) % count
        mesh.triangle(front_centre,
                      (points[i][0], points[i][1], z_front),
                      (points[j][0], points[j][1], z_front),
                      centre_uv, contour_uv[i], contour_uv[j],
                      outward=(0.0, 0.0, -1.0))

    wu0, wv0, wu1, wv1 = wall_uv
    for i in range(count):
        j = (i + 1) % count
        outward = (points[i][0] + points[j][0] - 2 * centre_model[0],
                   points[i][1] + points[j][1] - 2 * centre_model[1], 0.0)
        front_i, front_j = (wu0, wv0), (wu1, wv0)
        back_i, back_j = (wu1, wv1), (wu0, wv1)
        if side_strips and abs(outward[0]) > abs(outward[1]):
            strip = side_strips.get("east" if outward[0] > 0 else "west")
            if strip:
                u_front, u_back = strip
                front_i, back_i = (u_front, contour_uv[i][1]), (u_back, contour_uv[i][1])
                front_j, back_j = (u_front, contour_uv[j][1]), (u_back, contour_uv[j][1])
        mesh.quad((points[i][0], points[i][1], z_front),
                  (points[j][0], points[j][1], z_front),
                  (points[j][0], points[j][1], z_back),
                  (points[i][0], points[i][1], z_back),
                  front_i, front_j, back_j, back_i, outward=outward, material=wall_material)

    if back_cap:
        back_centre = (centre_model[0], centre_model[1], z_back)
        for i in range(count):
            j = (i + 1) % count
            mesh.triangle(back_centre,
                          (points[i][0], points[i][1], z_back),
                          (points[j][0], points[j][1], z_back),
                          (wu0, wv0), (wu1, wv0), (wu1, wv1),
                          outward=(0.0, 0.0, 1.0), material=wall_material)


def rounded_rect(x0, y0, x1, y1, radius, per_corner=6):
    """A rounded-rectangle outline in model space, counter-clockwise from the bottom-right."""
    radius = min(radius, (x1 - x0) / 2.0, (y1 - y0) / 2.0)
    corners = [(x1 - radius, y0 + radius, -math.pi / 2.0),
               (x1 - radius, y1 - radius, 0.0),
               (x0 + radius, y1 - radius, math.pi / 2.0),
               (x0 + radius, y0 + radius, math.pi)]
    points = []
    for cx, cy, start in corners:
        for k in range(per_corner + 1):
            angle = start + (math.pi / 2.0) * k / per_corner
            points.append((cx + radius * math.cos(angle), cy + radius * math.sin(angle)))
    return points


def build_panel(mesh, front_map, contour, z_profile, rim_uv=None):
    """A raised moulding standing off an enclosure face -- the SpectrAlert Classic's louvred horn
    module and its xenon lens are both one of these.

    `contour` is the outline in MODEL space and every UV is derived from it through `front_map`,
    so the art on a raised part lands exactly where the same art sits on the plate behind it.
    Deriving rather than hand-copying the rect is the point: the JSON model this replaces had both
    parts offset a quarter of a texture unit sideways, which drew every edge twice.

    `z_profile` is [(z, inset, uv_inset), ...] running from the plate outward; the last entry is
    capped. `inset` shrinks the geometry, putting a chamfer on the front lip. `uv_inset` shrinks
    where the side wall SAMPLES, independently of the geometry, which is what wraps the part's own
    art around its sides: the lens is a clear rounded cube whose flanks are as glassy as its face,
    and the horn's ribs carry on around the moulding, so neither wants a flat slab of enclosure
    colour there. Sampling deepest at the plate and closest to the outline at the front lip also
    puts the bright edge highlight where the real part catches light.

    Pass `rim_uv` instead when a part's flanks are NOT a continuation of its face: the ET24's
    strobe lens carries its FIRE legend on the front only, so wrapping would run the lettering
    round the sides. A flat patch of the lens's own frosted surface is what those flanks are.
    """
    centre = (sum(p[0] for p in contour) / len(contour),
              sum(p[1] for p in contour) / len(contour))

    def at(point, inset):
        dx, dy = point[0] - centre[0], point[1] - centre[1]
        length = math.hypot(dx, dy)
        if length < 1e-6 or inset <= 0.0:
            return point
        factor = max(0.0, length - inset) / length
        return (centre[0] + dx * factor, centre[1] + dy * factor)

    count = len(contour)
    for k in range(len(z_profile) - 1):
        (z_a, inset_a, uv_a), (z_b, inset_b, uv_b) = z_profile[k], z_profile[k + 1]
        for i in range(count):
            j = (i + 1) % count
            pa, pb = at(contour[i], inset_a), at(contour[j], inset_a)
            pc, pd = at(contour[j], inset_b), at(contour[i], inset_b)
            outward = (pa[0] + pb[0] - 2 * centre[0], pa[1] + pb[1] - 2 * centre[1],
                       -(inset_b - inset_a))
            if rim_uv:
                ru0, rv0, ru1, rv1 = rim_uv
                uvs = ((ru0, rv0), (ru1, rv0), (ru1, rv1), (ru0, rv1))
            else:
                uvs = (front_map.to_uv(at(contour[i], uv_a)),
                       front_map.to_uv(at(contour[j], uv_a)),
                       front_map.to_uv(at(contour[j], uv_b)),
                       front_map.to_uv(at(contour[i], uv_b)))
            mesh.quad((pa[0], pa[1], z_a), (pb[0], pb[1], z_a),
                      (pc[0], pc[1], z_b), (pd[0], pd[1], z_b),
                      uvs[0], uvs[1], uvs[2], uvs[3], outward=outward)

    z_front, inset_front, _ = z_profile[-1]
    # The cap shows the part's own art at full size: it is a hair smaller than the outline because
    # of the chamfer, but the art belongs to the whole part, not to the inset rectangle.
    for i in range(count):
        j = (i + 1) % count
        pa, pb = at(contour[i], inset_front), at(contour[j], inset_front)
        mesh.triangle((centre[0], centre[1], z_front), (pa[0], pa[1], z_front),
                      (pb[0], pb[1], z_front),
                      front_map.to_uv(centre), front_map.to_uv(contour[i]),
                      front_map.to_uv(contour[j]), outward=(0.0, 0.0, -1.0))


def build_dish(mesh, front_map, contour, z_profile, back_uv=None):
    """A housing that TAPERS toward its face -- a truncated dome, wide at the wall and narrower
    where its own front sits.

    build_shell extrudes an outline straight back, which is what a flat-flanked moulding is. A
    Gentex Commander 5 is not one: it is a rounded pillow whose flanks slope in from the mounting
    surface to a recessed face, and its FIRE legends live on that slope. Nothing here needs a flat
    patch for the sides, because a straight-on photograph of the appliance already contains the
    slope -- foreshortened into a ring around the perimeter. So every ring takes its UVs from the
    very contour its geometry comes from, which makes the mapping the identity and lands the
    legends back on the slope they were photographed on.

    `contour` is in MODEL space and `z_profile` is [(z, inset), ...] running from the mounting
    surface forward, the last entry capped. `back_uv` is a flat patch for the hidden back; without
    it the back is left open, which is right when something else caps it.
    """
    centre = (sum(p[0] for p in contour) / len(contour),
              sum(p[1] for p in contour) / len(contour))

    def at(point, inset):
        dx, dy = point[0] - centre[0], point[1] - centre[1]
        length = math.hypot(dx, dy)
        if length < 1e-6 or inset <= 0.0:
            return point
        factor = max(0.0, length - inset) / length
        return (centre[0] + dx * factor, centre[1] + dy * factor)

    count = len(contour)
    for k in range(len(z_profile) - 1):
        (z_a, inset_a), (z_b, inset_b) = z_profile[k], z_profile[k + 1]
        for i in range(count):
            j = (i + 1) % count
            pa, pb = at(contour[i], inset_a), at(contour[j], inset_a)
            pc, pd = at(contour[j], inset_b), at(contour[i], inset_b)
            outward = (pa[0] + pb[0] - 2 * centre[0], pa[1] + pb[1] - 2 * centre[1],
                       -(inset_b - inset_a))
            mesh.quad((pa[0], pa[1], z_a), (pb[0], pb[1], z_a),
                      (pc[0], pc[1], z_b), (pd[0], pd[1], z_b),
                      front_map.to_uv(pa), front_map.to_uv(pb),
                      front_map.to_uv(pc), front_map.to_uv(pd), outward=outward)

    z_front, inset_front = z_profile[-1]
    for i in range(count):
        j = (i + 1) % count
        pa, pb = at(contour[i], inset_front), at(contour[j], inset_front)
        mesh.triangle((centre[0], centre[1], z_front), (pa[0], pa[1], z_front),
                      (pb[0], pb[1], z_front),
                      front_map.to_uv((centre[0], centre[1])), front_map.to_uv(pa),
                      front_map.to_uv(pb), outward=(0.0, 0.0, -1.0))

    if back_uv:
        z_back, inset_back = z_profile[0]
        bu0, bv0, bu1, bv1 = back_uv
        for i in range(count):
            j = (i + 1) % count
            pa, pb = at(contour[i], inset_back), at(contour[j], inset_back)
            mesh.triangle((centre[0], centre[1], z_back), (pa[0], pa[1], z_back),
                          (pb[0], pb[1], z_back), (bu0, bv0), (bu1, bv0), (bu1, bv1),
                          outward=(0.0, 0.0, 1.0))


def build_dome(mesh, centre, radius, z_base, z_tip, uv_centre, uv_radius,
               segments=RING_SEGMENTS, profile=None):
    """A shallow round lens dome, UV'd radially from the circle the lens occupies in the texture.

    `centre`/`radius` and `uv_centre`/`uv_radius` are (x, y) pairs so a device whose front face is
    mapped at slightly different horizontal and vertical scales still gets a lens that matches its
    own art exactly.
    """
    profile = profile or DOME_PROFILE
    depth = z_base - z_tip
    angles = [2.0 * math.pi * i / segments for i in range(segments)]

    def vertex(angle, radius_fraction, depth_fraction):
        point = (centre[0] + radius[0] * radius_fraction * math.cos(angle),
                 centre[1] + radius[1] * radius_fraction * math.sin(angle),
                 z_base - depth * depth_fraction)
        # u runs opposite to model x on a north-facing surface; v runs opposite to model y.
        uv = (uv_centre[0] - uv_radius[0] * radius_fraction * math.cos(angle),
              uv_centre[1] - uv_radius[1] * radius_fraction * math.sin(angle))
        return point, uv

    mean_radius = (radius[0] + radius[1]) / 2.0
    for k in range(len(profile) - 1):
        depth_a, radius_a = profile[k]
        depth_b, radius_b = profile[k + 1]
        # Outward normal of a surface of revolution: perpendicular to the profile tangent, in the
        # (radial, z) plane. The band recedes toward the tip, so it leans forward as well as out.
        outward_radial = depth * (depth_b - depth_a)
        outward_z = mean_radius * (radius_b - radius_a)
        for i in range(segments):
            angle_i, angle_j = angles[i], angles[(i + 1) % segments]
            mid = angle_i + math.pi / segments
            pa, ua = vertex(angle_i, radius_a, depth_a)
            pb, ub = vertex(angle_j, radius_a, depth_a)
            pc, uc = vertex(angle_j, radius_b, depth_b)
            pd, ud = vertex(angle_i, radius_b, depth_b)
            outward = (outward_radial * math.cos(mid), outward_radial * math.sin(mid), outward_z)
            mesh.quad(pa, pb, pc, pd, ua, ub, uc, ud, outward=outward)

    tip_depth, tip_radius = profile[-1]
    tip_centre = (centre[0], centre[1], z_base - depth * tip_depth)
    for i in range(segments):
        pa, ua = vertex(angles[i], tip_radius, tip_depth)
        pb, ub = vertex(angles[(i + 1) % segments], tip_radius, tip_depth)
        mesh.triangle(tip_centre, pa, pb, uv_centre, ua, ub, outward=(0.0, 0.0, -1.0))


def build_barrel(mesh, centre, radius_base, radius_tip, z_base, z_tip, side_uv,
                 cap_uv_centre=None, cap_uv_radius=0.0, segments=RING_SEGMENTS):
    """A round fluted barrel, optionally capped with a lens disc -- the beacon's Fresnel lens.

    `side_uv` is (u0, v0, u1, v1) and wraps once around: its u axis runs around the circumference
    and its v axis runs from base to tip along the barrel. The beacon's fluted strip has its ribs
    running along u and its shading falling off across it, which is exactly a cylinder unwrapped,
    so one traverse puts the ribs concentric about the axis and the highlight on one side.
    """
    u0, v0, u1, v1 = side_uv
    outward_radial = z_base - z_tip
    outward_z = radius_tip - radius_base
    for i in range(segments):
        angle_i = 2.0 * math.pi * i / segments
        angle_j = 2.0 * math.pi * (i + 1) / segments
        mid = angle_i + math.pi / segments
        ui = u0 + (u1 - u0) * i / segments
        uj = u0 + (u1 - u0) * (i + 1) / segments
        base_i = (centre[0] + radius_base * math.cos(angle_i),
                  centre[1] + radius_base * math.sin(angle_i), z_base)
        base_j = (centre[0] + radius_base * math.cos(angle_j),
                  centre[1] + radius_base * math.sin(angle_j), z_base)
        tip_j = (centre[0] + radius_tip * math.cos(angle_j),
                 centre[1] + radius_tip * math.sin(angle_j), z_tip)
        tip_i = (centre[0] + radius_tip * math.cos(angle_i),
                 centre[1] + radius_tip * math.sin(angle_i), z_tip)
        outward = (outward_radial * math.cos(mid), outward_radial * math.sin(mid), outward_z)
        mesh.quad(base_i, base_j, tip_j, tip_i,
                  (ui, v1), (uj, v1), (uj, v0), (ui, v0), outward=outward)

    if cap_uv_centre is None:
        return
    tip_centre = (centre[0], centre[1], z_tip)
    for i in range(segments):
        angle_i = 2.0 * math.pi * i / segments
        angle_j = 2.0 * math.pi * (i + 1) / segments
        pa = (centre[0] + radius_tip * math.cos(angle_i),
              centre[1] + radius_tip * math.sin(angle_i), z_tip)
        pb = (centre[0] + radius_tip * math.cos(angle_j),
              centre[1] + radius_tip * math.sin(angle_j), z_tip)
        ua = (cap_uv_centre[0] - cap_uv_radius * math.cos(angle_i),
              cap_uv_centre[1] - cap_uv_radius * math.sin(angle_i))
        ub = (cap_uv_centre[0] - cap_uv_radius * math.cos(angle_j),
              cap_uv_centre[1] - cap_uv_radius * math.sin(angle_j))
        mesh.triangle(tip_centre, pa, pb, cap_uv_centre, ua, ub, outward=(0.0, 0.0, -1.0))


def build_box(mesh, lo, hi, face_uv, material=None):
    """An axis-aligned box with an explicit UV rect per face. `face_uv` keys are the six face
    names; a face left out is not emitted. `material` puts the whole box on a material other than
    the default one, for a part that is retextured separately from the body it sits on."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    corners = {
        "north": ([(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], (0, 0, -1)),
        "south": ([(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)], (0, 0, 1)),
        "west": ([(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], (-1, 0, 0)),
        "east": ([(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)], (1, 0, 0)),
        "up": ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], (0, 1, 0)),
        "down": ([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], (0, -1, 0)),
    }
    for name, uv_rect in face_uv.items():
        points, outward = corners[name]
        u0, v0, u1, v1 = uv_rect
        mesh.quad(points[0], points[1], points[2], points[3],
                  (u0, v0), (u1, v0), (u1, v1), (u0, v1), outward=outward, material=material)


# --------------------------------------------------------------------------- devices
def lens_from_uv(front_map, lens_uv):
    """Given the lens's rect in the texture, where does it land on the model, and how big is it?"""
    u0, v0, u1, v1 = lens_uv
    uv_centre = ((u0 + u1) / 2.0, (v0 + v1) / 2.0)
    uv_radius = ((u1 - u0) / 2.0, (v1 - v0) / 2.0)
    centre = front_map.to_model(uv_centre)
    radius = (uv_radius[0] * front_map.scale_x, uv_radius[1] * front_map.scale_y)
    return centre, radius, uv_centre, uv_radius


def wall_unit(texture, model_rect, z_front=14.0, z_back=16.0, lens_depth=1.0, outline="fitted"):
    """The two L-Series LED wall devices: round lens standing off a flat-sided enclosure.

    `outline` picks how the shell is shaped. "fitted" builds a rounded rectangle, which is what
    these mouldings are -- flat sides, round corners -- and gives each straight run one quad with
    one normal. "traced" follows the alpha edge and is for the ceiling unit, whose outline really
    is a curve all the way round.
    """
    mesh = Mesh()
    uv_rect = opaque_bounds(texture)
    if outline == "fitted":
        contour, centre_uv, _ = fit_rounded_rect_uv([texture] + VARIANTS.get(texture, []))
    else:
        contour, centre_uv = trace_silhouette(texture)
    front_map = FrontMap(uv_rect, model_rect)
    build_shell(mesh, contour, centre_uv, front_map, z_front, z_back, housing_patch(texture))
    centre, radius, uv_centre, uv_radius = lens_from_uv(front_map, measure_lens(texture))
    build_dome(mesh, centre, radius, z_front, z_front - lens_depth, uv_centre, uv_radius)
    return mesh, centre, radius, front_map


def ceiling_unit(texture, model_rect, z_front, z_back, lens_depth):
    """The ceiling speaker strobe. Its enclosure is already a disc in the texture, so the same
    trace that squares up a wall unit's corners gives this one a genuinely round rim -- and the
    shell gains the depth a real ceiling appliance has, instead of the 0.01-unit pancake the JSON
    model inherited from its predecessor."""
    return wall_unit(texture, model_rect, z_front, z_back, lens_depth, outline="traced")


def outdoor_unit(texture):
    """The weatherproof horn strobe: a faceplate standing off its own backbox.

    Its texture is a composite -- a straight-on faceplate flanked by two strips lifted from a
    three-quarter shot that supply the FIRE side walls -- so the faceplate keeps those authored
    side UVs rather than a traced inset. Only the top and bottom caps change, off the transparent
    margin they were sampling and onto a strip inside the art.
    """
    mesh = Mesh()
    face_uv = (3.875, 1.625, 12.25, 14.5)
    # Plain enclosure surface for the hidden backbox.
    plain = housing_patch(texture)

    # The backbox is capped on all six sides. Its north face is mostly hidden behind the faceplate,
    # but the faceplate is traced from the art and so has rounded corners -- leaving that face off
    # opens a hole at each corner that you see straight through the enclosure into.
    build_box(mesh, (4.0, 1.5, 13.0), (12.0, 14.5, 16.0),
              {"north": plain, "east": plain, "west": plain, "up": plain, "down": plain,
               "south": plain})

    model_rect = (3.5, 0.75, 12.5, 14.75)
    front_map = FrontMap(face_uv, model_rect)
    contour, centre_uv = trace_silhouette(texture, uv_box=face_uv)
    build_shell(mesh, contour, centre_uv, front_map, 11.5, 13.0, housing_patch(texture),
                back_cap=False,
                side_strips={"east": (3.875, 1.75), "west": (12.25, 14.375)})

    centre, radius, uv_centre, uv_radius = lens_from_uv(front_map, measure_lens(texture))
    build_dome(mesh, centre, radius, 11.5, 10.5, uv_centre, uv_radius)
    return mesh, centre, radius, front_map


def panel_from_uv(front_map, uv_rect, radius, per_corner=6):
    """The model-space rounded outline of a moulding described by the rect it occupies in the
    texture. Going through `front_map` means the part is positioned by where its art actually is,
    rather than by numbers typed in alongside it that can drift apart from the picture."""
    u0, v0, u1, v1 = uv_rect
    (x1, y1) = front_map.to_model((u0, v0))
    (x0, y0) = front_map.to_model((u1, v1))
    return rounded_rect(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), radius, per_corner)


def lf_unit(texture):
    """The SpectrAlert Advance LF low frequency sounder.

    Its enclosure is a barrel: the top and bottom edges bow outward and the corners are cut away,
    none of which a flat box front face can express -- it sampled straight past the art into the
    transparent margin, which is why the silhouette came out chewed. Tracing the outline gives the
    real curve for free, and the lens becomes a wrapped rounded moulding like the Classic's.
    """
    mesh = Mesh()
    model_rect = (3.0, 2.0, 13.0, 16.0)
    front_map = FrontMap(opaque_bounds(texture), model_rect)
    contour, centre_uv = trace_silhouette(texture)
    build_shell(mesh, contour, centre_uv, front_map, 14.0, 16.0, housing_patch(texture))

    lens = panel_from_uv(front_map, (5.15, 6.77, 11.47, 13.10), radius=1.50)
    build_panel(mesh, front_map, lens,
                [(14.0, 0.0, 0.85), (13.15, 0.0, 0.30), (12.9, 0.30, 0.05)])
    xs = [p[0] for p in lens]
    ys = [p[1] for p in lens]
    return mesh, (min(xs), min(ys), 12.9), (max(xs), max(ys), 14.0)


def classic_unit(texture):
    """The SpectrAlert Classic: a traced backplate carrying a raised horn module and a xenon lens.

    The plate is the reference for the whole device -- its front face carries the photograph at
    the scale the photograph was taken -- so the two raised parts take their outlines in model
    space and let `FrontMap` say which piece of that photograph belongs on each. That is what
    keeps the louvres, the FIRE legend and the lens bezel single-edged.

    Both raised parts are rounded rectangles, because that is what the real mouldings are; the
    box elements they replace showed square corners and a flat slab bottom under the lens.
    """
    mesh = Mesh()
    plate_rect = (1.25, 0.2, 14.75, 15.9)
    front_map = FrontMap(opaque_bounds(texture), plate_rect)
    contour, centre_uv = trace_silhouette(texture)
    build_shell(mesh, contour, centre_uv, front_map, 14.5, 16.0, housing_patch(texture))

    # Horn module: the louvred upper moulding carrying the FIRE legend. Its ribs wrap the sides.
    horn = rounded_rect(3.34, 7.44, 12.91, 15.29, radius=1.15)
    build_panel(mesh, front_map, horn,
                [(14.5, 0.0, 0.85), (13.35, 0.0, 0.30), (13.0, 0.30, 0.05)])

    # Xenon lens: a clear rounded cube standing well off the plate. Its flanks are as glassy as
    # its face, so they sample the lens's own bezel rather than enclosure plastic.
    lens = rounded_rect(3.83, 0.69, 12.42, 7.44, radius=1.90)
    build_panel(mesh, front_map, lens,
                [(14.5, 0.0, 1.10), (11.95, 0.0, 0.40), (11.6, 0.45, 0.05)])
    return mesh


#: The material the E7070's strobe module wears, kept off the body material so the plate can be
#: retextured red or white while the module -- white on both -- stays as it is.
STROBE_MATERIAL = "strobe"


def fit_rounded_rect_uv(textures, per_corner=5, shrink=0.20, radius=None):
    """Fit a straight-sided, round-cornered outline to a silhouette, in UV units.

    Tracing the alpha edge is right for a shape whose outline really is a curve -- the LF's bowed
    top and bottom, a ceiling unit's disc. It is wrong for a moulding with FLAT sides. The traced
    outline of one is straight to within a pixel, but it arrives as sixty-odd short segments, and
    the extruded flank then gets sixty-odd slightly different normals: Minecraft shades each one
    separately and a flat side reads as a bevelled curve. Fitting instead gives each straight run a
    single quad with a single normal, and leaves the corners as real arcs.

    The corner radius comes from how much area the silhouette is missing against its own bounding
    box -- a rounded rectangle loses exactly (4 - pi) r^2 to its four corners -- so it is measured
    off the art rather than guessed. Averaged across the colour variants a model serves, since
    antialiasing puts their edges a fraction of a pixel apart.
    """
    boxes, radii = [], []
    for name in textures:
        alpha, scale = alpha_of(name)
        solid = alpha > 128
        rows, cols = np.nonzero(solid)
        u0, u1 = cols.min() / scale, (cols.max() + 1) / scale
        v0, v1 = rows.min() / scale, (rows.max() + 1) / scale
        area = solid.sum() / (scale * scale)
        radii.append(math.sqrt(max(0.0, ((u1 - u0) * (v1 - v0) - area) / (4.0 - math.pi))))
        boxes.append((u0, v0, u1, v1))
    u0 = sum(b[0] for b in boxes) / len(boxes) + shrink
    v0 = sum(b[1] for b in boxes) / len(boxes) + shrink
    u1 = sum(b[2] for b in boxes) / len(boxes) - shrink
    v1 = sum(b[3] for b in boxes) / len(boxes) - shrink
    # The area estimate assumes the ONLY thing missing from the bounding box is four corner arcs.
    # That holds for the L-Series shells; it over-reads on a photograph carrying a soft edge or a
    # drop shadow, where the deficit has another source. Pass `radius` to override it when walking
    # the outline in from a corner says the real corners are tighter than the area implies.
    radius = sum(radii) / len(radii) if radius is None else radius
    return (rounded_rect(u0, v0, u1, v1, radius, per_corner),
            ((u0 + u1) / 2.0, (v0 + v1) / 2.0), radius)


def straighten_sides(contour, centre, band=0.5, tolerance=0.40):
    """Snap the near-vertical runs of a traced outline onto straight lines, leaving the rest.

    For a shell whose outline is a rounded rectangle all the way round, fit_rounded_rect_uv is the
    tool. This is for the ones that are only PARTLY straight -- the TrueAlert has flat sides but a
    bottom edge that genuinely bows outward, and fitting a rectangle to it would throw that away.

    A line is fitted to each side over the middle of the height, where the outline is certainly on
    the flat run, then every point on that side lying within `tolerance` of the line is pulled onto
    it. Points further off -- the corner arcs, the curved bottom -- are left exactly as traced. The
    fit allows a slope, so a moulding that tapers slightly stays tapered instead of being forced
    parallel.
    """
    if len(contour) < 8:
        return contour
    vs = [point[1] for point in contour]
    low, high = min(vs), max(vs)
    span = high - low
    if span <= 0:
        return contour
    inner_low = low + span * (1.0 - band) / 2.0
    inner_high = high - span * (1.0 - band) / 2.0

    result = list(contour)
    for side in (-1, 1):
        seed = [i for i, p in enumerate(contour)
                if (p[0] - centre[0]) * side > 0 and inner_low <= p[1] <= inner_high]
        if len(seed) < 3:
            continue
        us = np.array([contour[i][0] for i in seed])
        vv = np.array([contour[i][1] for i in seed])
        if vv.max() - vv.min() < 1e-6:
            continue
        slope, intercept = np.polyfit(vv, us, 1)
        # Snap only within the straight run's own height, grown a little. Without this bound a
        # corner point that happens to sit near the line gets pulled onto it, squaring off the
        # corner and kinking whatever curve follows -- on the TrueAlert that ate two points out of
        # its bowed bottom.
        margin = 0.10 * span
        reach_low, reach_high = vv.min() - margin, vv.max() + margin
        for i, point in enumerate(contour):
            if (point[0] - centre[0]) * side <= 0:
                continue
            if not (reach_low <= point[1] <= reach_high):
                continue
            fitted = slope * point[1] + intercept
            if abs(point[0] - fitted) <= tolerance:
                result[i] = (fitted, point[1])
    return result


def measure_xenon_lens(texture, window=(3.5, 7.0, 12.5, 14.5), pad=0.25):
    """Find a rectangular xenon lens in a RED enclosure texture and return its UV rect.

    The L-Series LED devices are located by their LED die, a saturated yellow square at the exact
    centre of a round lens. A xenon unit has no die and no circle: it has a wide, low chrome
    reflector box, and its lens sits noticeably lower on the face than the LED lens does -- which
    is why the two families cannot share a lens position.

    So this looks for what is NOT the enclosure's red, bright enough to be the reflector, inside a
    window that excludes the FIRE legend down each flank (also not red, also bright). Occupancy per
    column and per row then gives the span where the feature is actually dense, rather than a
    bounding box that any stray speck could widen. Measured on the red texture of a pair; the white
    enclosure is too close to the chrome to separate, and both are the same appliance.
    """
    rgba = np.asarray(Image.open(os.path.join(TEX_DIR, texture + ".png")).convert("RGBA"),
                      dtype=float)
    colour, alpha = rgba[..., :3], rgba[..., 3]
    height, width, _ = colour.shape
    scale = width / 16.0
    red, green, blue = colour[..., 0], colour[..., 1], colour[..., 2]
    mask = (~((red - np.maximum(green, blue)) > 38)) & (alpha > 200) & (colour.max(2) > 95)
    keep = np.zeros_like(mask)
    u0, v0, u1, v1 = [int(round(value * scale)) for value in window]
    keep[v0:v1, u0:u1] = True
    mask &= keep
    if mask.sum() < 150:
        raise ValueError("no xenon lens found in %s" % texture)
    columns = mask.sum(0)
    rows = mask.sum(1)
    dense_u = [x for x in range(width) if columns[x] > 0.18 * columns.max()]
    dense_v = [y for y in range(height) if rows[y] > 0.18 * rows.max()]
    return (min(dense_u) / scale - pad, min(dense_v) / scale - pad,
            (max(dense_u) + 1) / scale + pad, (max(dense_v) + 1) / scale + pad)


def measure_chrome_lens(texture, window=(5.0, 5.0, 11.0, 11.0), pad=0.15):
    """Find a round CHROME lens and return its UV rect.

    A ceiling unit's strobe is a bare chrome dome: no LED die to sit on, and on the white variants
    no colour contrast to find it by either. What it does have is a reflector that is not the
    enclosure's red, near the middle of a disc, so the same not-red-and-bright test the xenon wall
    lenses use works here with the window pulled in to the centre.
    """
    rgba = np.asarray(Image.open(os.path.join(TEX_DIR, texture + ".png")).convert("RGBA"),
                      dtype=float)
    colour, alpha = rgba[..., :3], rgba[..., 3]
    height, width, _ = colour.shape
    scale = width / 16.0
    red, green, blue = colour[..., 0], colour[..., 1], colour[..., 2]
    mask = (~((red - np.maximum(green, blue)) > 38)) & (alpha > 200) & (colour.max(2) > 95)
    keep = np.zeros_like(mask)
    u0, v0, u1, v1 = [int(round(value * scale)) for value in window]
    keep[v0:v1, u0:u1] = True
    mask &= keep
    if mask.sum() < 150:
        raise ValueError("no chrome lens found in %s" % texture)
    columns, rows = mask.sum(0), mask.sum(1)
    dense_u = [x for x in range(width) if columns[x] > 0.18 * columns.max()]
    dense_v = [y for y in range(height) if rows[y] > 0.18 * rows.max()]
    return (min(dense_u) / scale - pad, min(dense_v) / scale - pad,
            (max(dense_u) + 1) / scale + pad, (max(dense_v) + 1) / scale + pad)


def plain_shell_unit(texture, variants, model_rect, z_front, z_back, outline="fitted"):
    """An appliance with no strobe: just the enclosure. The horn and speaker versions of a device
    are the same moulding as their strobe siblings with the lens left off.

    `outline` is "fitted" for a rounded rectangle, "traced" to follow the alpha edge, or
    "straightened" to follow it with the near-vertical runs snapped flat -- which is what a shape
    with straight sides and a genuinely curved top or bottom needs.
    """
    mesh = Mesh()
    front_map = FrontMap(opaque_bounds(texture), model_rect)
    if outline == "fitted":
        contour, centre_uv, _ = fit_rounded_rect_uv(variants)
    else:
        contour, centre_uv = trace_silhouette(texture)
        if outline == "straightened":
            contour = straighten_sides(contour, centre_uv)
    build_shell(mesh, contour, centre_uv, front_map, z_front, z_back, find_flat_patch(variants, 0.8))
    return mesh


def ceiling_strobe_unit(texture, variants, model_rect=(1.0, 1.0, 15.0, 15.0),
                        z_front=14.4, z_back=16.0, lens_depth=0.9):
    """A ceiling appliance: a traced disc with a round chrome dome at its centre.

    Traced, not fitted -- a disc's outline really is a curve the whole way round, which is the one
    case where following the alpha edge is the right tool.
    """
    mesh = Mesh()
    front_map = FrontMap(opaque_bounds(texture), model_rect)
    contour, centre_uv = trace_silhouette(texture)
    build_shell(mesh, contour, centre_uv, front_map, z_front, z_back, find_flat_patch(variants, 0.8))
    centre, radius, uv_centre, uv_radius = lens_from_uv(front_map, measure_chrome_lens(texture))
    build_dome(mesh, centre, radius, z_front, z_front - lens_depth, uv_centre, uv_radius)
    return mesh, centre, radius


def lseries_xenon_unit(texture, variants, model_rect=(3.0, 3.0, 13.0, 16.0),
                       z_front=14.0, z_back=16.0, lens_depth=1.0):
    """A System Sensor L-Series wall appliance with a rectangular xenon lens.

    Same enclosure as the L-Series LED units -- same footprint, so the two sit together on a wall
    without one looking a size apart from the other -- but the lens is a wide low reflector box
    lower on the face rather than a round dome in the middle.
    """
    mesh = Mesh()
    front_map = FrontMap(opaque_bounds(texture), model_rect)
    # A fitted outline rather than a traced one: this shell's sides are flat, and only its corners
    # are round. See fit_rounded_rect_uv for why tracing a flat side is the wrong tool.
    contour, centre_uv, _ = fit_rounded_rect_uv(variants)
    build_shell(mesh, contour, centre_uv, front_map, z_front, z_back, find_flat_patch(variants, 0.8))

    # Shallow UV insets, as on the TrueAlert: this lens is wide and short, so deeper ones would
    # reach past its bright rim and wrap the dark reflector interior around its sides.
    lens = panel_from_uv(front_map, measure_xenon_lens(texture), radius=0.45)
    build_panel(mesh, front_map, lens,
                [(z_front, 0.0, 0.30), (z_front - lens_depth * 0.75, 0.0, 0.12),
                 (z_front - lens_depth, 0.22, 0.03)])
    xs = [p[0] for p in lens]
    ys = [p[1] for p in lens]
    return (mesh, (min(xs), min(ys), z_front - lens_depth), (max(xs), max(ys), z_front))


FLANK_MATERIAL = "flank"


def e50_unit(texture, variants):
    """The Wheelock E50 speaker strobe: a boxy enclosure with a wide lens bar across its top.

    The flanks are the interesting part. A real E50 carries FIRE down each side -- white on the red
    unit, red on the white one -- and its plate photograph, being straight on, contains none of it.
    So the shell's wall goes on a second material that gen_e50_flank_texture.py draws per colour.
    It cannot be shared the way the E7070's strobe module is, because that module is white on both
    variants while this legend inverts between them.
    """
    mesh = Mesh()
    model_rect = (3.0, 2.6, 13.0, 16.0)     # 10 x 13.4, the enclosure's own 0.745 aspect
    front_map = FrontMap(opaque_bounds(texture), model_rect)
    # Radius measured off the outline rather than off the area. Walking in from the top edge, the
    # E50 reaches near-full width within two pixels, so its corners are tight; the area estimate
    # reads 1.63 here because this photograph loses box area to something other than its corners.
    contour, centre_uv, _ = fit_rounded_rect_uv(variants, radius=0.75)
    # Left and right take the legend panel, which runs the full height of the flank texture so the
    # letters land at the height they do on the appliance. Top and bottom take plain housing --
    # routing every wall segment to the panel would run FIRE around the top and bottom edges too.
    build_shell(mesh, contour, centre_uv, front_map, 14.0, 16.0,
                (3.4, 0.4, 6.6, 3.6), wall_material=FLANK_MATERIAL,
                # One flank takes the panel reversed. East and west run their u in opposite senses
                # and are viewed from opposite sides, so an identical rect comes out mirrored on
                # one of them. Reversing costs nothing here: unlike the E7070's flank, this panel
                # has no cap at one end, so flipping it only un-mirrors the legend.
                side_strips={"east": (2.3, 0.1), "west": (0.1, 2.3)})

    # Wide, short lens bar. The rect is the lens's own measured extent, not a hair outside it: the
    # photograph puts a dark drop shadow under the lens and the red plate immediately beside it, and
    # a rect that reached those by even one pixel handed the side walls a slab of shadow instead of
    # glass -- the strobe's ends and underside came out charcoal grey.
    # The default window looks lower down the face, where a SpectrAlert's xenon lens sits; the
    # E50 wears its bar across the top, and no padding, so the rect ends on the lens.
    lens = panel_from_uv(front_map,
                         measure_xenon_lens(texture, window=(3.0, 1.5, 13.0, 5.5), pad=0.0),
                         radius=0.40)
    # Shallow insets, as on the TrueAlert and the xenon L-Series -- but every ring must stay inside
    # the lens, so the front lip samples a sixth of a unit in rather than sitting on the boundary.
    build_panel(mesh, front_map, lens,
                [(14.0, 0.0, 0.34), (13.05, 0.0, 0.24), (12.8, 0.20, 0.16)])
    xs = [p[0] for p in lens]
    ys = [p[1] for p in lens]
    return mesh, (min(xs), min(ys), 12.8), (max(xs), max(ys), 14.0)


#: Where a Commander 5's strobe lens sits on its face, as a UV rect to search inside. The face
#: carries a perforated horn grille as well, which is every bit as rough as the lens, and the FIRE
#: legends down the outer edges are rough too -- so the window has to fence off both.
GENTEX_C5_LENS_WINDOW = (2.8, 7.0, 13.2, 13.8)

#: How the housing's slope runs, as (depth from the mounting surface, inset) pairs. Read off the
#: ceiling-mount profiles: the pillow rolls over slowly at the wall and straightens as it goes,
#: reaching a face about two thirds the width of the plate.
GENTEX_C5_TAPER = [(0.00, 0.00), (0.22, 0.18), (0.50, 0.75), (0.78, 1.45), (1.00, 2.05)]


def gentex_commander5_unit(texture, variants, model_rect=(2.5, 2.5, 13.5, 13.5),
                           z_back=16.0, z_face=13.1, lens_depth=0.7, plate_radius=2.8,
                           shrink=0.50):
    """The Gentex Commander 5 horn strobe: a rounded pillow with a recessed face.

    Universal mount -- the same appliance goes on a wall or a ceiling -- which costs nothing here,
    since NSEWUD rotation already turns the whole model to face out of whatever it is stuck to.

    It is pulled shallower than the ceiling-mount profiles measure. At the real appliance's ratio
    an 11-wide unit wants four and a half units of depth, and on a wall in game that reads as a
    box bolted to it rather than an appliance; a third of its width sits better beside the flat
    plates it shares a corridor with, and the taper carries the bulk it loses.

    The shape is a taper rather than an extrusion, so it needs build_dish rather than build_shell:
    the housing is widest where it meets the mounting surface and narrows to a face about two
    thirds of that, and the FIRE legends live on the slope between the two. Its front cap lands on
    the recessed grey module -- horn grille above, LED lens below -- because the identity mapping
    puts the middle of the photograph on the middle of the model.
    """
    mesh = Mesh()
    front_map = FrontMap(opaque_bounds(texture), model_rect)
    # A deeper shrink than the default. The outline's job here is also to say where the art
    # STARTS, and a photograph's cut edge carries a couple of texels of shadow that the
    # default 0.2 leaves the outermost ring sitting on -- a dark fringe all round the housing
    # where it meets the wall, and Minecraft's mipmapping widens it.
    contour_uv, centre_uv, _ = fit_rounded_rect_uv(variants, radius=plate_radius,
                                                   shrink=shrink)
    contour = [front_map.to_model(uv) for uv in contour_uv]
    depth = z_back - z_face
    build_dish(mesh, front_map, contour,
               [(z_back - depth * where, inset) for where, inset in GENTEX_C5_TAPER],
               back_uv=find_flat_patch(variants, 0.8))

    # The lens stands proud of the face. It is shallow enough that wrapping its own art round its
    # sides is right -- what shows there is the clear cover's edge, which is what the art has just
    # inside its outline.
    lens = panel_from_uv(front_map,
                         measure_moulding(texture, window=GENTEX_C5_LENS_WINDOW, occupancy=0.4),
                         radius=0.35)
    build_panel(mesh, front_map, lens,
                [(z_face, 0.0, 0.28), (z_face - lens_depth * 0.7, 0.0, 0.12),
                 (z_face - lens_depth, 0.18, 0.03)])
    xs = [p[0] for p in lens]
    ys = [p[1] for p in lens]
    return mesh, (min(xs), min(ys), z_face - lens_depth), (max(xs), max(ys), z_face)


def measure_moulding(texture, inset=(0.06, 0.05), contrast=45, occupancy=0.25, shrink=0.0,
                     window=None):
    """Find a raised moulding on an enclosure face by CONTRAST, and return its UV rect.

    measure_xenon_lens and measure_chrome_lens both separate the part from the plate by colour --
    "not red, and bright" -- which only works on the red half of a pair. The Edwards 202-8A is
    sold in a red version and a grey one, and on the grey one the plate, the moulding's bezel and
    the chrome reflector behind the lens are all the same lightness; nothing in colour space tells
    them apart. What does is surface: a moulded plate is smooth, and a prismatic chrome reflector
    under a clear cover is emphatically not. So this looks for local contrast -- the spread between
    the lightest and darkest pixel in a small window -- and takes the bounding box of the region
    that has it, which finds the same moulding on both colours.

    `inset` skips a margin inside the silhouette, where a photographed plate's own bevel catches a
    highlight against a shadow and reads as contrast. `occupancy` requires a row or column to be
    mostly inside the moulding before it counts, so a screw head or a stamped notice off to one
    side cannot stretch the box. `shrink` pulls the result in afterwards: the detector stops on the
    drop shadow the moulding casts on the plate, which sits a little outside the moulding itself.
    `window` is a UV rect to search inside, for a face carrying more than one rough thing: a
    Commander 5 has a perforated horn grille above its lens, and both are as rough as each other.
    """
    image = Image.open(os.path.join(TEX_DIR, texture + ".png")).convert("RGBA")
    alpha = np.asarray(image)[..., 3]
    scale = alpha.shape[1] / 16.0
    grey = image.convert("L")
    spread = (np.asarray(grey.filter(ImageFilter.MaxFilter(5)), dtype=float)
              - np.asarray(grey.filter(ImageFilter.MinFilter(5)), dtype=float))

    rows, cols = np.nonzero(alpha > 128)
    x0, x1, y0, y1 = cols.min(), cols.max(), rows.min(), rows.max()
    width, height = x1 - x0 + 1, y1 - y0 + 1
    region = np.zeros(alpha.shape, dtype=bool)
    region[y0 + int(inset[1] * height):y1 - int(inset[1] * height),
           x0 + int(inset[0] * width):x1 - int(inset[0] * width)] = True
    if window:
        fence = np.zeros(alpha.shape, dtype=bool)
        fence[int(window[1] * scale):int(window[3] * scale),
              int(window[0] * scale):int(window[2] * scale)] = True
        region &= fence
    rough = region & (alpha > 128) & (spread > contrast)

    # Occupancy is measured against the SEARCH region, not the whole silhouette: inside a window
    # only a few units tall, a row of the lens covers all of it and a fraction of the plate's
    # height would never be reached.
    span_y, span_x = max(1, region.sum(0).max()), max(1, region.sum(1).max())
    columns = np.nonzero(rough.sum(0) > occupancy * span_y)[0]
    lines = np.nonzero(rough.sum(1) > occupancy * span_x)[0]
    if len(columns) == 0 or len(lines) == 0:
        raise ValueError("no raised moulding found in %s" % texture)
    return (columns.min() / scale + shrink, lines.min() / scale + shrink,
            (columns.max() + 1) / scale - shrink, (lines.max() + 1) / scale - shrink)


def find_glassy_patch(textures, lens_uv, size=0.5, brightest=0.75):
    """A flat and BRIGHT patch of a clear lens cover, for the flanks of a deep one.

    build_panel's default wrap -- sampling the part's own art a little inside its own outline --
    is right for a shallow lens, where the flank is barely a lip and what it should show is the
    bright bezel the art already has there. This cover is nearly two units deep, and the wrap hands
    that much flank a slab of the reflector behind the glass: on the -TW's photograph that is very
    nearly black, and it renders as the lens smeared back over the housing behind it.

    What a real clear cover's side actually looks like is in the side profiles -- bright
    translucent plastic, the reflector only dimly behind it. So the flanks take a flat patch of the
    lens's own art, like the ET24's frosted one, except that flattest alone would happily settle on
    the dark chrome, which is every bit as smooth as the glass. Candidates are ranked by roughness
    only after discarding all but the brightest `brightest` fraction of them. Scored across every
    texture the model wears, taking each candidate's worst case, since one model serves both
    colours and a patch has to be glass in each.
    """
    images = [np.asarray(Image.open(os.path.join(TEX_DIR, name + ".png")).convert("RGBA"),
                         dtype=float) for name in textures]
    scale = images[0].shape[1] / 16.0
    window = max(1, int(size * scale))
    candidates = []
    for v in np.arange(lens_uv[1], lens_uv[3] - size, 0.1):
        for u in np.arange(lens_uv[0], lens_uv[2] - size, 0.1):
            brightness, roughness, usable = 255.0, 0.0, True
            for image in images:
                block = image[int(v * scale):int(v * scale) + window,
                              int(u * scale):int(u * scale) + window]
                if block.size == 0 or block[..., 3].min() < 250:
                    usable = False
                    break
                pixels = block[..., :3].reshape(-1, 3)
                brightness = min(brightness, float(pixels.mean()))
                roughness = max(roughness, float(pixels.std(0).mean()))
            if usable:
                candidates.append((brightness, roughness, float(u), float(v)))
    if not candidates:
        raise ValueError("no patch of lens found in %s" % (textures,))
    cut = sorted(c[0] for c in candidates)[int((1.0 - brightest) * (len(candidates) - 1))]
    bright = [c for c in candidates if c[0] >= cut] or candidates
    _, _, u, v = min(bright, key=lambda c: c[1])
    return (u + 0.05, v + 0.05, u + size - 0.05, v + size - 0.05)


#: Width of the flank strip in edwards_est_202_8a_flank.png, in UV units. crop_device_flank.py
#: wrote it at that width; the model has to sample exactly it or the FIRE legend stretches.
EDWARDS_FLANK_STRIP = 2.9
#: The plain-moulding patch in the same atlas, pulled a hair inside its rect.
EDWARDS_FLANK_PATCH = (3.6, 0.1, 7.4, 3.9)


def edwards_202_unit(texture, variants, model_rect=(3.0, 1.4, 13.0, 16.0), z_plate=15.4,
                     z_body=12.6,
                     z_lens=10.8, moulding_shrink=0.30, bezel=0.34, plate_radius=0.55):
    """The Edwards EST 202-8A wall strobe: a thin plate carrying a deep body under a clear lens.

    This one is mostly about depth. Its siblings in this file are two-unit-thick appliances whose
    lens stands a unit proud; a 202-8A is a stack -- mounting plate, then a grey moulded body a
    third of the plate's width deep, then a big clear prismatic cover on the front of that. The
    side profile puts the whole protrusion at a shade under half the plate's width, so it is built
    as three parts rather than a shell with a lens on it.

One model serves both colours, because the -TW's texture is the -T's with its plate repainted
    (recolor_housing.py --from-saturated) rather than a second photograph. Its own product shot
    exists, but it is a different plate -- narrower against its height, and pierced by a lens window
    that runs nearly to the top edge where the -T leaves a margin. Built to its own art the -TW came
    out with the strobe stretched to the plate's edge and no bezel above it, and no single set of
    baked UVs can sit on both. Repainting the good photograph gets one geometry, one model, and a
    pair that is the same size on a wall the way the real pair is.

    The body's flanks are the reason it needs a second material. A real 202-8A carries FIRE down
    each side of that grey body in rotated red capitals, and the plate photograph -- taken straight
    on -- contains none of it. crop_device_flank.py lifts the band out of a side profile instead.
    The -T and the -TW share the moulding exactly, only the plate behind it changing colour, so
    unlike the E50's inverting legend one flank texture serves both blocks.
    """
    mesh = Mesh()
    front_map = FrontMap(opaque_bounds(texture), model_rect)

    # The plate: flat sides, rounded corners, and thin. Its own radius is passed rather than
    # measured -- these photographs carry a soft cut edge, and the area estimate reads that
    # missing sliver all the way round as though it were corner.
    contour, centre_uv, _ = fit_rounded_rect_uv(variants, radius=plate_radius)
    build_shell(mesh, contour, centre_uv, front_map, z_plate, 16.0, find_flat_patch(variants, 0.8))

    # The body, extruded forward off the plate. Its front cap wears the plate photograph's own
    # lens region, so the grey bezel ring around the clear cover is real art rather than a guess.
    body_uv = measure_moulding(texture, shrink=moulding_shrink)
    body_centre = ((body_uv[0] + body_uv[2]) / 2.0, (body_uv[1] + body_uv[3]) / 2.0)
    build_shell(mesh, rounded_rect(*body_uv, radius=0.45, per_corner=5), body_centre, front_map,
                z_body, z_plate, EDWARDS_FLANK_PATCH, back_cap=False,
                wall_material=FLANK_MATERIAL,
                # East takes the strip with its front edge at the strip's far end. Texture u runs
                # opposite to model x on a north-facing front, so handing both flanks the same
                # sense mirrors the legend on one of them -- see the E50, which has the same
                # problem for the same reason.
                side_strips={"east": (EDWARDS_FLANK_STRIP, 0.0),
                             "west": (0.0, EDWARDS_FLANK_STRIP)})

    # The clear cover, inset from the body by the width of the bezel holding it. Its flanks get a
    # flat patch of its own bright glass rather than the usual wrap -- see find_glassy_patch.
    lens_uv = (body_uv[0] + bezel, body_uv[1] + bezel, body_uv[2] - bezel, body_uv[3] - bezel)
    lens = panel_from_uv(front_map, lens_uv, radius=0.40)
    depth = z_body - z_lens
    build_panel(mesh, front_map, lens,
                [(z_body, 0.0, 0.0), (z_body - depth * 0.65, 0.0, 0.0), (z_lens, 0.30, 0.0)],
                rim_uv=find_glassy_patch(variants, lens_uv))
    xs = [p[0] for p in lens]
    ys = [p[1] for p in lens]
    return mesh, (min(xs), min(ys), z_lens), (max(xs), max(ys), z_body)


def truealert_unit(texture, variants):
    """The Simplex TrueAlert horn strobe and speaker strobe, which share one enclosure.

    The shape is the point here. A TrueAlert is 0.66 wide-to-tall with a bottom edge that bows
    downward in a broad arc under the FIRE band, and the model it replaces was a plain box wearing
    a squashed 1.12-aspect photograph, so the curve simply did not exist. Tracing the silhouette
    off a properly proportioned cutout gets the arc, the rounded shoulders and the tapered flanks
    for free -- there is no radius to tune, because none of it is a radius.
    """
    mesh = Mesh()
    # A TrueAlert is a big appliance, so it hangs about a fifth of a block below its own, keeping
    # its top edge on the block's. The rect is scaled about that top edge rather than stretched:
    # 12.8 x 19.2 holds the same 0.667 aspect the cutout has, so nothing in the art elongates.
    model_rect = (1.6, -3.2, 14.4, 16.0)
    front_map = FrontMap(opaque_bounds(texture), model_rect)
    contour, centre_uv = trace_silhouette(texture)
    contour = straighten_sides(contour, centre_uv)
    # Two units of body: the side profile puts the enclosure's depth at roughly a sixth of its
    # width, and it is a moulded shell rather than the thin plate the previous model implied.
    build_shell(mesh, contour, centre_uv, front_map, 14.0, 16.0, find_flat_patch(variants, 0.8))

    # The lens is a clear wraparound standing about half the body's depth proud of its face, so
    # its flanks show its own glass rather than housing.
    # Shallow UV insets: this lens is wide and short, only about three units tall in the texture,
    # so the deeper insets the taller lenses use would reach past its bright bezel and wrap the
    # dark reflector interior around its sides. A third of a unit stays on the bezel, which is
    # what the real clear edge looks like.
    lens = panel_from_uv(front_map, (3.4, 9.95, 12.6, 12.9), radius=0.55)
    build_panel(mesh, front_map, lens,
                [(14.0, 0.0, 0.30), (13.15, 0.0, 0.12), (12.9, 0.25, 0.03)])
    xs = [p[0] for p in lens]
    ys = [p[1] for p in lens]
    return mesh, (min(xs), min(ys), 12.9), (max(xs), max(ys), 14.0)


def e7070_unit(texture, variants):
    """The Wheelock E7070 speaker strobe: a square plate with a strobe module clipped to its face.

    The module is the part that needed the work. Its texture is a straight-on photograph, so the
    only strobe art in it is the lens seen head-on, and the shipped model smeared that one rect
    over all six faces of a box -- while the plate's own sides carried degenerate zero-height UVs
    that rendered as glitched slivers. On the real appliance the module is a white plastic body
    with a vertical red FIRE legend down each flank and a clear lens cap wrapping its front, which
    is what gen_e7070_strobe_texture.py draws and what this maps: the head-on lens on the front,
    the flank art on each side with its glass end forward, plain body white top and bottom.
    """
    mesh = Mesh()
    plate_lo, plate_hi = (2.0, 4.0, 15.0), (14.0, 16.0, 16.0)
    plate_rim = find_flat_patch(variants, 0.8)
    build_box(mesh, plate_lo, plate_hi,
              {"north": (0.0, 0.0, 16.0, 16.0), "south": plate_rim, "east": plate_rim,
               "west": plate_rim, "up": plate_rim, "down": plate_rim})

    # The module sits where the lens sits in the photograph, so it covers its own art exactly.
    front_map = FrontMap((0.0, 0.0, 16.0, 16.0), (plate_lo[0], plate_lo[1], plate_hi[0],
                                                  plate_hi[1]))
    (x1, y1) = front_map.to_model((6.0, 1.6))
    (x0, y0) = front_map.to_model((9.2, 14.4))
    z_front = 11.5                      # about 1.3x its own width proud, as the real one stands

    # Each flank takes the layout whose cap sits at the end its own u calls "front": west runs u
    # front-to-back, east back-to-front. Both rects stay ASCENDING -- reversing one to move the cap
    # would mirror its legend, which is why the atlas holds the reversed layout drawn out rather
    # than a mirror of the first.
    flank_west = (0.0, 0.0, 6.0, 16.0)
    flank_east = (6.0, 0.0, 12.0, 16.0)
    lens_front = (12.0, 0.0, 15.4, 16.0)
    body_white = (15.5, 1.0, 15.9, 5.0)
    build_box(mesh, (min(x0, x1), min(y0, y1), z_front), (max(x0, x1), max(y0, y1), plate_lo[2]),
              {"north": lens_front, "east": flank_east, "west": flank_west,
               "up": body_white, "down": body_white},
              material=STROBE_MATERIAL)

    # The flash is bounded to the clear cap, not the whole module: the renderer runs its side band
    # from the front face back through this depth, and past the cap it would be lighting up the
    # opaque white body the FIRE legend is printed on.
    cap_depth = (plate_lo[2] - z_front) * E7070_GLASS_FRACTION
    return (mesh,
            (min(x0, x1), min(y0, y1), z_front),
            (max(x0, x1), max(y0, y1), z_front + cap_depth))


def et24_unit(texture):
    """The Wheelock ET24 sounder strobe: a square plate with a tall frosted strobe lens on it.

    Unlike the other appliances here the plate's texture is fully opaque with no silhouette to
    trace, so the plate is a plain box wearing the whole photograph. The lens stands off it by the
    two units the Wheelock E70 and ET70 strobes use, and takes a flat patch of its own frosted
    surface on the flanks -- the FIRE legend is printed on the lens face only, so wrapping it would
    run the lettering around the sides of a part that is really just a translucent block.
    """
    mesh = Mesh()
    plate_rect = (1.5, 1.5, 14.5, 14.5)
    front_map = FrontMap((0.0, 0.0, 16.0, 16.0), plate_rect)
    plate_rim = find_flat_patch([texture], 0.8, within=(4.5, 13.9, 9.0, 15.9))
    build_box(mesh, (plate_rect[0], plate_rect[1], 14.0), (plate_rect[2], plate_rect[3], 16.0),
              {"north": (0.0, 0.0, 16.0, 16.0), "south": plate_rim, "east": plate_rim,
               "west": plate_rim, "up": plate_rim, "down": plate_rim})

    lens_uv = (4.38, 1.0, 8.56, 13.94)
    lens = panel_from_uv(front_map, lens_uv, radius=0.5)
    frosted = find_flat_patch([texture], 0.6, within=lens_uv)
    build_panel(mesh, front_map, lens,
                [(14.0, 0.0, 0.0), (12.3, 0.0, 0.0), (12.0, 0.22, 0.0)], rim_uv=frosted)

    xs = [p[0] for p in lens]
    ys = [p[1] for p in lens]
    return mesh, (min(xs), min(ys), 12.0), (max(xs), max(ys), 14.0)


def beacon():
    """The colour-lens beacon: a square mount plate carrying a round, fluted Fresnel barrel.

    The texture is an atlas the previous model never used as one -- a plate with four screws in
    u[0,8] v[0,8], plain metal beside it, a fluted strip in u[0,8] v[8,16] whose ribs run along u,
    and a round lens face in u[8,12] v[8,12]. Wrapping the strip's u around the barrel puts those
    ribs where a Fresnel lens actually has them, concentric about the axis.
    """
    mesh = Mesh()
    plate_lo, plate_hi = (3.0, 3.0, 13.5), (13.0, 13.0, 16.0)
    metal = (8.4, 0.4, 15.6, 7.6)
    build_box(mesh, plate_lo, plate_hi,
              {"north": (0.2, 0.2, 7.8, 7.8), "south": metal,
               "east": (8.4, 0.4, 15.6, 2.2), "west": (8.4, 0.4, 15.6, 2.2),
               "up": (8.4, 0.4, 15.6, 2.2), "down": (8.4, 0.4, 15.6, 2.2)})

    # Barrel profile as (z, radius), plate outward to nose: a narrow neck off the plate, a
    # full-width barrel, then a taper into the nose. The previous stepped-box model reached nearly
    # 10 units off the wall, which on a 10-wide plate read as a megaphone rather than an appliance.
    # A real Fresnel beacon stands off its base by a little less than the barrel is wide, which is
    # what these sections describe.
    sections = [(13.5, 3.10), (12.7, 3.10), (12.6, 3.55), (10.0, 3.55), (9.0, 2.60)]
    flute_v0, flute_v1 = 8.3, 15.7
    total = sections[0][0] - sections[-1][0]
    for k in range(len(sections) - 1):
        (z_a, r_a), (z_b, r_b) = sections[k], sections[k + 1]
        v_a = flute_v0 + (flute_v1 - flute_v0) * (sections[0][0] - z_a) / total
        v_b = flute_v0 + (flute_v1 - flute_v0) * (sections[0][0] - z_b) / total
        build_barrel(mesh, (8.0, 8.0), r_a, r_b, z_a, z_b, (0.2, v_b, 7.8, v_a))

    # Nose: the round lens itself, domed over the end of the barrel.
    z_nose, radius_nose = sections[-1]
    build_dome(mesh, (8.0, 8.0), (radius_nose, radius_nose), z_nose, 8.0,
               (10.0, 10.0), (1.85, 1.85))
    return mesh, (8.0, 8.0), (radius_nose, radius_nose), z_nose


# --------------------------------------------------------------------------- driver
def main():
    reports = []

    def emit(mesh, filename, name, texture, header, lens=None):
        faces = mesh.write(os.path.join(OUT_DIR, filename), name,
                           "csm:blocks/lifesafety/" + texture, header)
        reports.append((filename, faces, lens))

    # Wall horn strobe and wall speaker strobe. Both enclosures occupy the footprint their JSON
    # models did, so the blocks' bounding boxes are unchanged; only the lens becomes round.
    mesh, centre, radius, front_map = wall_unit(
        "system_sensor_l_series_led_red_horn_strobe",
        model_rect=(3.0, 3.0, 13.0, 16.0))
    emit(mesh, "systemsensor_lseries_led_hornstrobe.obj", "lseries_led_hornstrobe",
         "system_sensor_l_series_led_red_horn_strobe",
         "System Sensor L-Series LED wall horn strobe", (centre, radius, 14.0, 13.0))

    mesh, centre, radius, front_map = wall_unit(
        "system_sensor_l_series_led_red_speaker_strobe",
        model_rect=(3.0, 3.0, 13.0, 16.0))
    emit(mesh, "systemsensor_lseries_led_speakerstrobe.obj", "lseries_led_speakerstrobe",
         "system_sensor_l_series_led_red_speaker_strobe",
         "System Sensor L-Series LED wall speaker strobe", (centre, radius, 14.0, 13.0))

    mesh, centre, radius, front_map = ceiling_unit(
        "system_sensor_l_series_led_red_ceiling_speaker_strobe",
        model_rect=(1.0, 1.0, 15.0, 15.0), z_front=14.4, z_back=16.0, lens_depth=0.9)
    emit(mesh, "systemsensor_lseries_led_speakerstrobe_ceiling.obj",
         "lseries_led_speakerstrobe_ceiling",
         "system_sensor_l_series_led_red_ceiling_speaker_strobe",
         "System Sensor L-Series LED ceiling speaker strobe",
         (centre, radius, 14.4, 13.5))

    mesh, centre, radius, front_map = outdoor_unit(
        "system_sensor_l_series_led_red_outdoor_horn_strobe")
    emit(mesh, "systemsensor_lseries_led_hornstrobe_outdoor.obj",
         "lseries_led_hornstrobe_outdoor",
         "system_sensor_l_series_led_red_outdoor_horn_strobe",
         "System Sensor L-Series LED weatherproof horn strobe",
         (centre, radius, 11.5, 10.5))

    # Ceiling strobes: traced disc, chrome dome.
    for stem, colours in (("ceiling_hornstrobe",
                           ("system_sensor_l_series_red_ceiling_horn_strobe",
                            "system_sensor_l_series_white_ceiling_horn_strobe")),
                          ("ceiling_speakerstrobe",
                           ("system_sensor_l_series_red_ceiling_speaker_strobe",
                            "system_sensor_l_series_white_ceiling_speaker_strobe"))):
        mesh, centre, radius = ceiling_strobe_unit(colours[0], list(colours))
        reports.append(("systemsensor_lseries_%s.obj" % stem,
                        mesh.write(os.path.join(OUT_DIR, "systemsensor_lseries_%s.obj" % stem),
                                   "lseries_%s" % stem, "csm:blocks/lifesafety/" + colours[0],
                                   "System Sensor L-Series ceiling %s" % stem),
                        (centre, radius, 14.4, 13.5)))

    # No-strobe enclosures: the same mouldings with the lens left off.
    for stem, colours, rect, outline, depth in (
            ("ceiling_speaker", ("system_sensor_l_series_red_ceiling_speaker",
                                 "system_sensor_l_series_white_ceiling_speaker"),
             (1.0, 1.0, 15.0, 15.0), "traced", (14.4, 16.0)),
            ("horn", ("system_sensor_l_series_red_horn",
                      "system_sensor_l_series_white_horn"),
             (3.0, 4.0, 13.0, 16.0), "fitted", (14.0, 16.0)),
            ("speaker", ("system_sensor_l_series_red_speaker",
                         "system_sensor_l_series_white_speaker"),
             (3.0, 4.0, 13.0, 16.0), "fitted", (14.0, 16.0))):
        mesh = plain_shell_unit(colours[0], list(colours), rect, depth[0], depth[1], outline)
        reports.append(("systemsensor_lseries_%s.obj" % stem,
                        mesh.write(os.path.join(OUT_DIR, "systemsensor_lseries_%s.obj" % stem),
                                   "lseries_%s" % stem, "csm:blocks/lifesafety/" + colours[0],
                                   "System Sensor L-Series %s" % stem),
                        None))

    # TrueAlert plain horn / speaker: the strobe's enclosure without the lens. Straightened
    # rather than fitted, so its bowed bottom survives the way the strobe version's does.
    mesh = plain_shell_unit("shared_textures/simplex_truealert_red_horn",
                            ["shared_textures/simplex_truealert_red_horn",
                             "shared_textures/simplex_truealert_white_horn"],
                            (2.0, 3.5, 14.0, 16.0), 14.0, 16.0, outline="straightened")
    reports.append(("simplex_truealert_sounder.obj",
                    mesh.write(os.path.join(OUT_DIR, "simplex_truealert_sounder.obj"),
                               "simplex_truealert_sounder",
                               "csm:blocks/lifesafety/shared_textures/simplex_truealert_red_horn",
                               "Simplex TrueAlert horn / speaker"),
                    None))

    # The standalone strobe: same shell, single colour, no sounder.
    mesh, lens_from, lens_to = lseries_xenon_unit("system_sensor_l_series_strobe_red",
                                                 ["system_sensor_l_series_strobe_red"])
    reports.append(("systemsensor_lseries_strobe.obj",
                    mesh.write(os.path.join(OUT_DIR, "systemsensor_lseries_strobe.obj"),
                               "lseries_strobe",
                               "csm:blocks/lifesafety/system_sensor_l_series_strobe_red",
                               "System Sensor L-Series standalone strobe"),
                    (((lens_from[0] + lens_to[0]) / 2, (lens_from[1] + lens_to[1]) / 2),
                     ((lens_to[0] - lens_from[0]) / 2, (lens_to[1] - lens_from[1]) / 2),
                     lens_to[2], lens_from[2])))

    for stem, colours in (("hornstrobe", ("system_sensor_l_series_red_horn_strobe",
                                         "system_sensor_l_series_white_horn_strobe")),
                          ("speakerstrobe", ("system_sensor_l_series_red_speaker_strobe",
                                             "system_sensor_l_series_white_speaker_strobe"))):
        mesh, lens_from, lens_to = lseries_xenon_unit(colours[0], list(colours))
        reports.append(("systemsensor_lseries_%s.obj" % stem,
                        mesh.write(os.path.join(OUT_DIR, "systemsensor_lseries_%s.obj" % stem),
                                   "lseries_%s" % stem,
                                   "csm:blocks/lifesafety/" + colours[0],
                                   "System Sensor L-Series xenon %s" % stem),
                        (((lens_from[0] + lens_to[0]) / 2, (lens_from[1] + lens_to[1]) / 2),
                         ((lens_to[0] - lens_from[0]) / 2, (lens_to[1] - lens_from[1]) / 2),
                         lens_to[2], lens_from[2])))

    mesh, lens_from, lens_to = e50_unit(
        "wheelock_e50_red_speaker_strobe",
        ["wheelock_e50_red_speaker_strobe", "wheelock_e50_white_speaker_strobe"])
    reports.append(("wheelock_e50_speakerstrobe.obj",
                    mesh.write(os.path.join(OUT_DIR, "wheelock_e50_speakerstrobe.obj"),
                               "wheelock_e50_speakerstrobe",
                               "csm:blocks/lifesafety/wheelock_e50_red_speaker_strobe",
                               "Wheelock E50 speaker strobe",
                               extra_materials={FLANK_MATERIAL:
                                                "csm:blocks/lifesafety/wheelock_e50_flank_red"}),
                    (((lens_from[0] + lens_to[0]) / 2, (lens_from[1] + lens_to[1]) / 2),
                     ((lens_to[0] - lens_from[0]) / 2, (lens_to[1] - lens_from[1]) / 2),
                     lens_to[2], lens_from[2])))

    # The black edition is the red plate repainted (recolor_housing.py --from-saturated), so it
    # keeps the red unit's light FIRE legends where the white unit's dark ones would vanish.
    gentex5 = ["gentex_commander_5_red_horn_strobe", "gentex_commander_5_white_horn_strobe",
               "gentex_commander_5_black_horn_strobe"]
    mesh, lens_from, lens_to = gentex_commander5_unit(gentex5[0], gentex5)
    reports.append(("gentex_commander5_hornstrobe.obj",
                    mesh.write(os.path.join(OUT_DIR, "gentex_commander5_hornstrobe.obj"),
                               "gentex_commander5_hornstrobe",
                               "csm:blocks/lifesafety/" + gentex5[0],
                               "Gentex Commander 5 horn strobe"),
                    (((lens_from[0] + lens_to[0]) / 2, (lens_from[1] + lens_to[1]) / 2),
                     ((lens_to[0] - lens_from[0]) / 2, (lens_to[1] - lens_from[1]) / 2),
                     lens_to[2], lens_from[2])))

    # The Edwards 202-8A pair, on one model: see edwards_202_unit for why the -TW is a repaint of
    # the -T's plate rather than its own photograph.
    edwards = ["edwards_est_202_8a_t_red", "edwards_est_202_8a_tw_white"]
    mesh, lens_from, lens_to = edwards_202_unit(edwards[0], edwards)
    reports.append(("edwards_est_202_8a_strobe.obj",
                    mesh.write(os.path.join(OUT_DIR, "edwards_est_202_8a_strobe.obj"),
                               "edwards_est_202_8a_strobe",
                               "csm:blocks/lifesafety/" + edwards[0],
                               "Edwards EST 202-8A wall strobe",
                               extra_materials={FLANK_MATERIAL:
                                                "csm:blocks/lifesafety/"
                                                "edwards_est_202_8a_flank"}),
                    (((lens_from[0] + lens_to[0]) / 2, (lens_from[1] + lens_to[1]) / 2),
                     ((lens_to[0] - lens_from[0]) / 2, (lens_to[1] - lens_from[1]) / 2),
                     lens_to[2], lens_from[2])))

    mesh, lens_from, lens_to = truealert_unit(
        "shared_textures/simplex_truealert_red_horn_strobe",
        ["shared_textures/simplex_truealert_red_horn_strobe",
         "shared_textures/simplex_truealert_white_horn_strobe"])
    reports.append(("simplex_truealert_hornstrobe.obj",
                    mesh.write(os.path.join(OUT_DIR, "simplex_truealert_hornstrobe.obj"),
                               "simplex_truealert_hornstrobe",
                               "csm:blocks/lifesafety/shared_textures/"
                               "simplex_truealert_red_horn_strobe",
                               "Simplex TrueAlert horn strobe / speaker strobe"),
                    (((lens_from[0] + lens_to[0]) / 2, (lens_from[1] + lens_to[1]) / 2),
                     ((lens_to[0] - lens_from[0]) / 2, (lens_to[1] - lens_from[1]) / 2),
                     lens_to[2], lens_from[2])))

    mesh, lens_from, lens_to = e7070_unit(
        "wheelock_et70_red_speaker_strobe",
        ["wheelock_et70_red_speaker_strobe", "wheelock_et70_white_speaker_strobe"])
    reports.append(("wheelock_e7070_speakerstrobe.obj",
                    mesh.write(os.path.join(OUT_DIR, "wheelock_e7070_speakerstrobe.obj"),
                               "wheelock_e7070_speakerstrobe",
                               "csm:blocks/lifesafety/wheelock_et70_red_speaker_strobe",
                               "Wheelock E7070 speaker strobe",
                               extra_materials={STROBE_MATERIAL:
                                                "csm:blocks/lifesafety/"
                                                "wheelock_e7070_strobe_module"}),
                    (((lens_from[0] + lens_to[0]) / 2, (lens_from[1] + lens_to[1]) / 2),
                     ((lens_to[0] - lens_from[0]) / 2, (lens_to[1] - lens_from[1]) / 2),
                     lens_to[2], lens_from[2])))

    mesh, lens_from, lens_to = et24_unit("wheelock_et24_red_sounder_strobe")
    reports.append(("wheelock_et24_sounderstrobe.obj",
                    mesh.write(os.path.join(OUT_DIR, "wheelock_et24_sounderstrobe.obj"),
                               "wheelock_et24_sounderstrobe",
                               "csm:blocks/lifesafety/wheelock_et24_red_sounder_strobe",
                               "Wheelock ET24 sounder strobe"),
                    (((lens_from[0] + lens_to[0]) / 2, (lens_from[1] + lens_to[1]) / 2),
                     ((lens_to[0] - lens_from[0]) / 2, (lens_to[1] - lens_from[1]) / 2),
                     lens_to[2], lens_from[2])))

    mesh, lens_from, lens_to = lf_unit("system_sensor_spectralert_advance_lf_red_horn_strobe")
    reports.append(("systemsensor_sa_lf_hornstrobe.obj",
                    mesh.write(os.path.join(OUT_DIR, "systemsensor_sa_lf_hornstrobe.obj"),
                               "sa_lf_hornstrobe",
                               "csm:blocks/lifesafety/"
                               "system_sensor_spectralert_advance_lf_red_horn_strobe",
                               "System Sensor SpectrAlert Advance LF low frequency sounder"),
                    ((( (lens_from[0]+lens_to[0])/2, (lens_from[1]+lens_to[1])/2 ),
                      ( (lens_to[0]-lens_from[0])/2, (lens_to[1]-lens_from[1])/2 ),
                      lens_to[2], lens_from[2]))))

    mesh = classic_unit("system_sensor_spectralert_classic_red_horn_strobe")
    emit(mesh, "systemsensor_spectralert_classic_hornstrobe.obj", "spectralert_classic_hornstrobe",
         "system_sensor_spectralert_classic_red_horn_strobe",
         "System Sensor SpectrAlert Classic horn strobe")

    mesh, centre, radius, z_nose = beacon()
    emit(mesh, "firealarm_beacon.obj", "firealarm_beacon", "fire_alarm_beacon_red",
         "Colour-lens fire alarm beacon, round Fresnel barrel",
         # The flash sits on the nose dome, which runs from the barrel's last ring forward to z=8.
         (centre, radius, z_nose, 8.0))

    print("Wrote %d models to %s\n" % (len(reports), OUT_DIR))
    print("%-52s %6s  %s" % ("model", "tris", "strobe lens from/to for CsmTabLifeSafety"))
    for filename, faces, lens in reports:
        if lens:
            (cx, cy), (rx, ry), z_base, z_tip = lens
            bounds = "{%.2ff, %.2ff, %.2ff} -> {%.2ff, %.2ff, %.2ff}" % (
                cx - rx, cy - ry, z_tip, cx + rx, cy + ry, z_base)
        else:
            bounds = ""
        print("%-52s %6d  %s" % (filename, faces, bounds))


if __name__ == "__main__":
    main()
