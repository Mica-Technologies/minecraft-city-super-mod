package com.micatechnologies.minecraft.csm.venues.lines;

import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A court or track cell's model: its blockstate's body (the sides and, for an item, a picture of
 * a top) with the top replaced by the pieces {@link LinePainter} cuts the cell into, each a
 * convex polygon fanned into quads in its material's sprite and colour (written into the vertex
 * colour, which both lighting pipelines multiply their shade into). A raised material (the kerb)
 * is drawn proud of the top with walls along the edges that are not the cell's own.
 *
 * <p>The quads are cached by {@link LineArtKey} in {@link LineArtModels}: a cell is painted
 * once, when its key is first seen, and nothing here runs per frame.</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class LineArtBakedModel extends BakedModelWrapper<IBakedModel> {

  /** A cell's quads: its top (culled with the face) and its walls (never culled). */
  static final class Quads {

    final List<BakedQuad> top;
    final List<BakedQuad> walls;

    Quads(List<BakedQuad> top, List<BakedQuad> walls) {
      this.top = top;
      this.walls = walls;
    }
  }

  public LineArtBakedModel(IBakedModel original) {
    super(original);
  }

  @Override
  @Nonnull
  public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side,
      long rand) {
    LineArtKey key = null;
    if (state instanceof IExtendedBlockState) {
      key = ((IExtendedBlockState) state).getValue(BlockLineCell.KEY);
    }
    if (key == null) {
      return originalModel.getQuads(state, side, rand);
    }
    Quads q = LineArtModels.quads(key);
    if (q == null) {
      return originalModel.getQuads(state, side, rand);
    }
    if (side == EnumFacing.UP) {
      return q.top;
    }
    if (side == null && !q.walls.isEmpty()) {
      List<BakedQuad> base = originalModel.getQuads(state, null, rand);
      if (base.isEmpty()) {
        return q.walls;
      }
      List<BakedQuad> out = new ArrayList<>(base.size() + q.walls.size());
      out.addAll(base);
      out.addAll(q.walls);
      return out;
    }
    return originalModel.getQuads(state, side, rand);
  }

  /** Keeps this model the one drawn for an item (the wrapped model names itself). */
  @Override
  @Nonnull
  public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
      @Nonnull ItemCameraTransforms.TransformType type) {
    Pair<? extends IBakedModel, Matrix4f> pair = originalModel.handlePerspective(type);
    return Pair.of(this, pair.getRight());
  }

  // --- baking -------------------------------------------------------------------------------

  /** Paints a key's cell into quads. */
  static Quads bake(LineArtKey key,
      java.util.function.Function<String, TextureAtlasSprite> sprites) {
    LineArtFamily fam = key.family();
    LineArtLayout layout = fam.layout(key.layout);
    List<LinePainter.Piece> pieces = LinePainter.paint(layout, key.i, key.j);
    List<BakedQuad> top = new ArrayList<>();
    List<BakedQuad> walls = new ArrayList<>();
    List<double[]> raised = new ArrayList<>();
    for (LinePainter.Piece p : pieces) {
      LineArtMaterial m = LineArtMaterials.ALL[Math.max(0,
          Math.min(LineArtMaterials.ALL.length - 1, p.mat))];
      TextureAtlasSprite sprite = sprites.apply(fam.sprite(m.sprite));
      int rgb = colour(m, key);
      double[] local = toBlock(p.pts, key);
      float y = 1.0f + m.raise / 16.0f;
      fanUp(top, local, y, sprite, rgb);
      if (m.raise > 0) {
        raised.add(local);
      }
    }
    for (int r = 0; r < raised.size(); r++) {
      double[] poly = raised.get(r);
      int n = poly.length / 2;
      for (int k = 0; k < n; k++) {
        int m2 = (k + 1) % n;
        double ax = poly[2 * k];
        double az = poly[2 * k + 1];
        double bx = poly[2 * m2];
        double bz = poly[2 * m2 + 1];
        if (onCellEdge(ax, az, bx, bz) || shared(raised, r, ax, az, bx, bz)) {
          continue;
        }
        LineArtMaterial m = LineArtMaterials.ALL[LineArtMaterials.KERB];
        wall(walls, poly, ax, az, bx, bz, 1.0f + m.raise / 16.0f,
            sprites.apply(fam.sprite(m.sprite)), colour(m, key));
      }
    }
    return new Quads(Collections.unmodifiableList(top), Collections.unmodifiableList(walls));
  }

  private static int colour(LineArtMaterial m, LineArtKey key) {
    switch (m.colour) {
      case CC:
        return SurfaceColours.rgb(key.colour(0));
      case SC:
        return SurfaceColours.rgb(key.colour(1));
      case LC:
        return VenueColours.rgb(key.colour(2));
      case OC:
        return VenueColours.rgb(key.colour(3));
      default:
        return m.rgb;
    }
  }

  /** A polygon in layout metres to block-local 0..1, turned with the key. */
  private static double[] toBlock(double[] pts, LineArtKey key) {
    double[] out = new double[pts.length];
    for (int k = 0; k < pts.length; k += 2) {
      double lx = pts[k] - key.i;
      double lz = pts[k + 1] - key.j;
      double x;
      double z;
      switch (key.rot) {
        case 1:
          x = 1 - lz;
          z = lx;
          break;
        case 2:
          x = 1 - lx;
          z = 1 - lz;
          break;
        case 3:
          x = lz;
          z = 1 - lx;
          break;
        default:
          x = lx;
          z = lz;
          break;
      }
      out[k] = x;
      out[k + 1] = z;
    }
    return out;
  }

  private static boolean onCellEdge(double ax, double az, double bx, double bz) {
    double e = 1e-6;
    return (Math.abs(ax) < e && Math.abs(bx) < e) || (Math.abs(ax - 1) < e && Math.abs(bx - 1) < e)
        || (Math.abs(az) < e && Math.abs(bz) < e) || (Math.abs(az - 1) < e && Math.abs(bz - 1) < e);
  }

  /** Whether another raised polygon has the same edge (a kerb cut in two inside its cell). */
  private static boolean shared(List<double[]> raised, int self, double ax, double az, double bx,
      double bz) {
    double e = 1e-6;
    for (int r = 0; r < raised.size(); r++) {
      if (r == self) {
        continue;
      }
      double[] p = raised.get(r);
      int n = p.length / 2;
      for (int k = 0; k < n; k++) {
        int m = (k + 1) % n;
        if (Math.abs(p[2 * k] - bx) < e && Math.abs(p[2 * k + 1] - bz) < e
            && Math.abs(p[2 * m] - ax) < e && Math.abs(p[2 * m + 1] - az) < e) {
          return true;
        }
      }
    }
    return false;
  }

  /** Fans a convex polygon facing up into quads, wound as the game winds an up face. */
  private static void fanUp(List<BakedQuad> out, double[] poly, float y, TextureAtlasSprite sprite,
      int rgb) {
    int n = poly.length / 2;
    if (n < 3) {
      return;
    }
    // an up face's corners run NW, SW, SE, NE: clockwise in (x, z), a negative area there
    boolean flip = LinePainter.area2(poly) > 0;
    double[] p = poly;
    if (flip) {
      p = new double[poly.length];
      for (int k = 0; k < n; k++) {
        p[2 * k] = poly[2 * (n - 1 - k)];
        p[2 * k + 1] = poly[2 * (n - 1 - k) + 1];
      }
    }
    for (int k = 1; k < n - 1; k += 2) {
      int a = k;
      int b = k + 1;
      int c = Math.min(k + 2, n - 1);
      int[] data = new int[28];
      vertex(data, 0, p[0], y, p[1], sprite, rgb, p[0], p[1], 0, 1, 0);
      vertex(data, 1, p[2 * a], y, p[2 * a + 1], sprite, rgb, p[2 * a], p[2 * a + 1], 0, 1, 0);
      vertex(data, 2, p[2 * b], y, p[2 * b + 1], sprite, rgb, p[2 * b], p[2 * b + 1], 0, 1, 0);
      vertex(data, 3, p[2 * c], y, p[2 * c + 1], sprite, rgb, p[2 * c], p[2 * c + 1], 0, 1, 0);
      out.add(new BakedQuad(data, -1, EnumFacing.UP, sprite, true, DefaultVertexFormats.ITEM));
    }
  }

  /** A kerb's wall from the top down to the block's top, facing away from its polygon. */
  private static void wall(List<BakedQuad> out, double[] poly, double ax, double az, double bx,
      double bz, float top, TextureAtlasSprite sprite, int rgb) {
    double cx = 0;
    double cz = 0;
    int n = poly.length / 2;
    for (int k = 0; k < n; k++) {
      cx += poly[2 * k];
      cz += poly[2 * k + 1];
    }
    cx /= n;
    cz /= n;
    double ex = bx - ax;
    double ez = bz - az;
    // outward: the edge's normal pointing away from the centre
    double nx = ez;
    double nz = -ex;
    if (nx * ((ax + bx) / 2 - cx) + nz * ((az + bz) / 2 - cz) < 0) {
      nx = -nx;
      nz = -nz;
    }
    double len = Math.sqrt(nx * nx + nz * nz);
    if (len < 1e-9) {
      return;
    }
    nx /= len;
    nz /= len;
    // wound anticlockwise seen from outside: top-left, bottom-left, bottom-right, top-right,
    // where right, for one looking at the wall from outside, is (nz, -nx)
    double sx = nz;
    double sz = -nx;
    double[] p0 = {ax, az};
    double[] p1 = {bx, bz};
    if (ex * sx + ez * sz < 0) {
      p0 = new double[]{bx, bz};
      p1 = new double[]{ax, az};
    }
    double span = Math.sqrt(ex * ex + ez * ez);
    int[] data = new int[28];
    float u1 = (float) Math.min(1.0, span);
    vertex(data, 0, p0[0], top, p0[1], sprite, rgb, 0, 0, (float) nx, 0, (float) nz);
    vertex(data, 1, p0[0], 1.0, p0[1], sprite, rgb, 0, (top - 1.0), (float) nx, 0,
        (float) nz);
    vertex(data, 2, p1[0], 1.0, p1[1], sprite, rgb, u1, (top - 1.0), (float) nx, 0,
        (float) nz);
    vertex(data, 3, p1[0], top, p1[1], sprite, rgb, u1, 0, (float) nx, 0, (float) nz);
    EnumFacing face = EnumFacing.getFacingFromVector((float) nx, 0, (float) nz);
    out.add(new BakedQuad(data, -1, face, sprite, true, DefaultVertexFormats.ITEM));
  }

  /**
   * One vertex in the ITEM format: position, colour (ABGR), uv, normal.
   *
   * @param u the texture's u, 0..1 across the block
   * @param v the texture's v, 0..1
   */
  private static void vertex(int[] data, int i, double x, double y, double z,
      TextureAtlasSprite sprite, int rgb, double u, double v, float nx, float ny, float nz) {
    int o = i * 7;
    data[o] = Float.floatToRawIntBits((float) x);
    data[o + 1] = Float.floatToRawIntBits((float) y);
    data[o + 2] = Float.floatToRawIntBits((float) z);
    data[o + 3] = 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    data[o + 4] = Float.floatToRawIntBits(sprite.getInterpolatedU(u * 16.0));
    data[o + 5] = Float.floatToRawIntBits(sprite.getInterpolatedV(v * 16.0));
    data[o + 6] = ((int) (nx * 127) & 0xFF) | (((int) (ny * 127) & 0xFF) << 8)
        | (((int) (nz * 127) & 0xFF) << 16);
  }
}
