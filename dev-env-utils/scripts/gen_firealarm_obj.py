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

import numpy as np
from PIL import Image

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
        ["system_sensor_l_series_led_white_speaker_strobe"],
    "system_sensor_l_series_led_red_ceiling_speaker_strobe":
        ["system_sensor_l_series_led_white_ceiling_speaker_strobe"],
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

    def triangle(self, p1, p2, p3, uv1, uv2, uv3, outward):
        """Emit one triangle, reversing the winding if it faces away from `outward`."""
        normal = _face_normal(p1, p2, p3)
        if _dot(normal, outward) < 0.0:
            p2, p3 = p3, p2
            uv2, uv3 = uv3, uv2
            normal = _face_normal(p1, p2, p3)
        index = self._normal(normal)
        self.faces.append([(self._position(p1), self._texcoord(uv1), index),
                           (self._position(p2), self._texcoord(uv2), index),
                           (self._position(p3), self._texcoord(uv3), index)])

    def quad(self, p1, p2, p3, p4, uv1, uv2, uv3, uv4, outward):
        self.triangle(p1, p2, p3, uv1, uv2, uv3, outward)
        self.triangle(p1, p3, p4, uv1, uv3, uv4, outward)

    def write(self, path, name, texture, header):
        mtl_name = os.path.splitext(os.path.basename(path))[0] + ".mtl"
        with open(os.path.join(os.path.dirname(path), mtl_name), "w", newline="\n") as handle:
            handle.write("# Procedurally generated by gen_firealarm_obj.py\n")
            handle.write("newmtl %s\nmap_Kd %s\n" % (MATERIAL, texture))
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
            handle.write("usemtl %s\n" % MATERIAL)
            for face in self.faces:
                handle.write("f " + " ".join("%d/%d/%d" % c for c in face) + "\n")
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


def find_flat_patch(texture_names, size=0.8):
    """The smoothest fully-opaque square of enclosure surface shared by a model's colour variants.

    An enclosure's side wall is a plain moulded flank, so it wants a plain colour. Sampling it
    from a band inset inside the front outline -- the obvious trick, and what this generator did
    first -- only works when the art near the edge happens to be plain. On the L-Series LED units
    the speaker grille runs almost to the rim, so that band smeared perforations down the sides of
    the housing. Picking one flat patch instead gives the flank a single housing colour, and the
    face normals still shade it away from the front.

    The patch has to be flat in every texture the model wears, since one model serves red and white.
    """
    images = []
    for name in texture_names:
        data = np.asarray(Image.open(os.path.join(TEX_DIR, name + ".png")).convert("RGBA"),
                          dtype=float)
        images.append(data)
    scale = images[0].shape[1] / 16.0
    window = max(1, int(size * scale))
    best = None
    for v in np.arange(0.0, 16.0 - size, 0.25):
        for u in np.arange(0.0, 16.0 - size, 0.25):
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
                back_cap=True, side_strips=None):
    """An enclosure: traced front cap, extruded side wall, optional flat back.

    The front cap's UVs are the traced points themselves, so the art lands on the geometry it was
    traced from -- an identity mapping, which cannot bleed into the transparent margin. `wall_uv`
    is a flat patch of enclosure surface for the flanks.

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
                  front_i, front_j, back_j, back_i, outward=outward)

    if back_cap:
        back_centre = (centre_model[0], centre_model[1], z_back)
        for i in range(count):
            j = (i + 1) % count
            mesh.triangle(back_centre,
                          (points[i][0], points[i][1], z_back),
                          (points[j][0], points[j][1], z_back),
                          (wu0, wv0), (wu1, wv0), (wu1, wv1),
                          outward=(0.0, 0.0, 1.0))


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


def build_panel(mesh, front_map, contour, z_profile):
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
            mesh.quad((pa[0], pa[1], z_a), (pb[0], pb[1], z_a),
                      (pc[0], pc[1], z_b), (pd[0], pd[1], z_b),
                      front_map.to_uv(at(contour[i], uv_a)),
                      front_map.to_uv(at(contour[j], uv_a)),
                      front_map.to_uv(at(contour[j], uv_b)),
                      front_map.to_uv(at(contour[i], uv_b)), outward=outward)

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


def build_box(mesh, lo, hi, face_uv):
    """An axis-aligned box with an explicit UV rect per face. `face_uv` keys are the six face
    names; a face left out is not emitted."""
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
                  (u0, v0), (u1, v0), (u1, v1), (u0, v1), outward=outward)


# --------------------------------------------------------------------------- devices
def lens_from_uv(front_map, lens_uv):
    """Given the lens's rect in the texture, where does it land on the model, and how big is it?"""
    u0, v0, u1, v1 = lens_uv
    uv_centre = ((u0 + u1) / 2.0, (v0 + v1) / 2.0)
    uv_radius = ((u1 - u0) / 2.0, (v1 - v0) / 2.0)
    centre = front_map.to_model(uv_centre)
    radius = (uv_radius[0] * front_map.scale_x, uv_radius[1] * front_map.scale_y)
    return centre, radius, uv_centre, uv_radius


def wall_unit(texture, model_rect, z_front=14.0, z_back=16.0, lens_depth=1.0):
    """The two L-Series LED wall devices: traced enclosure, round lens standing off its face."""
    mesh = Mesh()
    uv_rect = opaque_bounds(texture)
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
    return wall_unit(texture, model_rect, z_front, z_back, lens_depth)


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
