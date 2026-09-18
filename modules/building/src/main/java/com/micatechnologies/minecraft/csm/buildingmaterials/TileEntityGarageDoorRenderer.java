package com.micatechnologies.minecraft.csm.buildingmaterials;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a garage door while it moves or stands stopped part-way: a roll-up's curtain or a grille rising into (or coming down
 * from) the top of its opening, or a sectional door's panels running up the track, round the bend
 * and back along the ceiling. Alive only for the seconds a move takes, so nothing is cached; at
 * rest the door is baked block models and this is never called.
 *
 * <p>Everything is drawn in the door's own frame -- the north-facing frame its models are drawn
 * in, with the anchor's cell at the origin and the door running east (clockwise from the facing)
 * -- and turned to the facing as the blockstate turns the models. A sectional panel's texture
 * mapping and path are the ones {@code gen_garage_doors.py} gives the open door's ceiling panels,
 * so the last frame of a move is the model that replaces it.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityGarageDoorRenderer extends TileEntitySpecialRenderer<TileEntityGarageDoor> {

  /** Half a sectional panel's thickness. */
  private static final double HALF = 1 / 16.0;
  /** The roll-up curtain's plane, z in the north-facing model: just behind the wall. */
  private static final double CURTAIN_Z0 = -1.0 / 16;
  private static final double CURTAIN_Z1 = -0.5 / 16;

  private BufferBuilder buf;
  private World world;
  private BlockPos anchor;
  private EnumFacing facing;

  @Override
  public void render(TileEntityGarageDoor te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!(state.getBlock() instanceof BlockGarageDoor)) {
      return;
    }
    BlockGarageDoor block = (BlockGarageDoor) state.getBlock();
    this.world = te.getWorld();
    this.anchor = te.getPos();
    this.facing = state.getValue(BlockGarageDoor.FACING);
    // Eased by position, not by time, so a door slows into both ends however often it was stopped
    // and started on the way.
    double p = te.position(partialTicks);
    double moved = p * p * (3 - 2 * p) * te.travel();

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.pushMatrix();
    GlStateManager.disableLighting();
    GlStateManager.enableAlpha();
    GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(-facing.getHorizontalAngle() + 180F, 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    Tessellator tessellator = Tessellator.getInstance();
    buf = tessellator.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
    String name = block.getBlockRegistryName();
    if (block.kind() == BlockGarageDoor.Kind.SECTIONAL) {
      String style = name.substring("garage_door_sectional_".length());
      Sectional s = Sectional.of(style);
      sectional(te, moved, atlas.getAtlasSprite(tex(s.face)), atlas.getAtlasSprite(tex(s.faceTop)),
          atlas.getAtlasSprite(tex(s.back)), atlas.getAtlasSprite(tex(s.backTop)));
    } else {
      String curtain = block.kind() == BlockGarageDoor.Kind.GRILLE ? "grille_mesh"
          : "rollup_galvanized";
      coiling(te, moved, atlas.getAtlasSprite(tex(curtain)),
          atlas.getAtlasSprite(tex("garage_bar")));
    }
    tessellator.draw();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
    buf = null;
  }

  private static String tex(String name) {
    return "csm:blocks/garage/" + name;
  }

  /** The textures a sectional style's panels wear: SHARED with gen_garage_doors.SECTIONAL_TEX. */
  private enum Sectional {
    WHITE("white_face", "white_face", "white_back", "white_back"),
    WINDOWED("white_face", "windowed_face_top", "white_back", "windowed_back_top"),
    COMMERCIAL("commercial_face", "commercial_face", "commercial_back", "commercial_back");

    final String face;
    final String faceTop;
    final String back;
    final String backTop;

    Sectional(String face, String faceTop, String back, String backTop) {
      this.face = face;
      this.faceTop = faceTop;
      this.back = back;
      this.backTop = backTop;
    }

    static Sectional of(String style) {
      for (Sectional s : values()) {
        if (s.name().equalsIgnoreCase(style)) {
          return s;
        }
      }
      return WHITE;
    }
  }

  // --- the roll-up and the grille ----------------------------------------------------------------

  /**
   * The curtain from its bottom edge, {@code moved} blocks up, to the top of the opening, where it
   * goes up into the hood; its slats move with it, so the texture is laid by distance up the
   * curtain rather than by height.
   */
  private void coiling(TileEntityGarageDoor te, double moved, TextureAtlasSprite curtain,
      TextureAtlasSprite bar) {
    int width = te.getWidth();
    int height = te.getHeight();
    double length = height - moved;
    for (int i = 0; i < width; i++) {
      for (int k = 0; k < Math.ceil(length - 1e-6); k++) {
        double m0 = k;
        double m1 = Math.min(k + 1, length);
        double y0 = moved + m0;
        double y1 = moved + m1;
        int lm = light(i + 0.5, (y0 + y1) / 2, CURTAIN_Z1);
        float v0 = (float) (1 - (m1 - k));
        // The street face, then the inside face (mirrored, as the models' north faces are).
        quad(curtain, i, y0, CURTAIN_Z1, i + 1, y1, CURTAIN_Z1, 0, 0, 1, 0F, 1F, v0, 1F, lm);
        quad(curtain, i + 1, y0, CURTAIN_Z0, i, y1, CURTAIN_Z0, 0, 0, -1, 0F, 1F, v0, 1F, lm);
      }
      if (length > 1e-6) {
        int lm = light(i + 0.5, moved + 0.5 / 16, CURTAIN_Z1);
        box(bar, i, moved, -1.75 / 16, i + 1, moved + 1 / 16.0, -0.25 / 16, lm);
      }
    }
  }

  // --- the sectional door ------------------------------------------------------------------------

  /**
   * A point {@code s} blocks along the track from the bottom of the opening, as {y, z}: up the
   * opening, round the bend, back along the ceiling. SHARED with gen_garage_doors.track_path.
   */
  private static double[] path(double s, int height) {
    double r = TileEntityGarageDoor.BEND;
    double plane = TileEntityGarageDoor.PLANE;
    double arc = Math.PI * r / 2;
    if (s <= height) {
      return new double[]{s, plane};
    }
    if (s <= height + arc) {
      double a = (s - height) / r;
      return new double[]{height + r * Math.sin(a), plane - r * (1 - Math.cos(a))};
    }
    return new double[]{height + r, plane - r - (s - height - arc)};
  }

  /**
   * Every section (two to a block) as a flat panel between its two edges on the path -- rigid
   * sections hinged at their joints, as a real door's are -- moved {@code moved} blocks along it.
   */
  private void sectional(TileEntityGarageDoor te, double moved, TextureAtlasSprite face,
      TextureAtlasSprite faceTop, TextureAtlasSprite back, TextureAtlasSprite backTop) {
    int width = te.getWidth();
    int height = te.getHeight();
    int sections = height * 2;
    for (int k = 0; k < sections; k++) {
      int row = k / 2;
      boolean top = row == height - 1;
      double[] a = path(moved + k * 0.5, height);
      double[] b = path(moved + (k + 1) * 0.5, height);
      double ty = b[0] - a[0];
      double tz = b[1] - a[1];
      double len = Math.sqrt(ty * ty + tz * tz);
      // The street side: N = (-tz, ty), SHARED with the generator's track normals.
      double ny = -tz / len * HALF;
      double nz = ty / len * HALF;
      // v: the lower section of a block wears the texture's bottom half.
      float vLow = k % 2 == 0 ? 1F : 0.5F;
      float vHigh = vLow - 0.5F;
      TextureAtlasSprite f = top ? faceTop : face;
      TextureAtlasSprite bk = top ? backTop : back;
      for (int i = 0; i < width; i++) {
        int lm = light(i + 0.5, (a[0] + b[0]) / 2, (a[1] + b[1]) / 2);
        // Street face: u = x across the door.
        panel(f, i, i + 1, a[0] + ny, a[1] + nz, b[0] + ny, b[1] + nz, ny, nz, 0F, 1F, vLow,
            vHigh, lm);
        // Inside face: mirrored.
        panel(bk, i + 1, i, a[0] - ny, a[1] - nz, b[0] - ny, b[1] - nz, -ny, -nz, 0F, 1F, vLow,
            vHigh, lm);
        if (k == 0) {
          edge(bk, i, a, ny, nz, -ty, -tz, lm);
        }
        if (k == sections - 1) {
          edge(bk, i, b, ny, nz, ty, tz, lm);
        }
      }
    }
  }

  /** One face of a section: x from {@code xa} to {@code xb} (u 0 to 1), a to b (vLow to vHigh). */
  private void panel(TextureAtlasSprite sp, double xa, double xb, double ay, double az,
      double by, double bz, double oy, double oz, float u0, float u1, float vLow, float vHigh,
      int lm) {
    double[] p0 = {xa, ay, az};
    double[] p1 = {xb, ay, az};
    double[] p2 = {xb, by, bz};
    double[] p3 = {xa, by, bz};
    face(p0, p1, p2, p3, new float[]{sp.getInterpolatedU(u0 * 16), sp.getInterpolatedU(u1 * 16),
            sp.getInterpolatedU(u1 * 16), sp.getInterpolatedU(u0 * 16)},
        new float[]{sp.getInterpolatedV(vLow * 16), sp.getInterpolatedV(vLow * 16),
            sp.getInterpolatedV(vHigh * 16), sp.getInterpolatedV(vHigh * 16)},
        0, oy, oz, lm);
  }

  /** A section's end edge at {@code p}, facing along the track ({@code oy}, {@code oz}). */
  private void edge(TextureAtlasSprite sp, int i, double[] p, double ny, double nz, double oy,
      double oz, int lm) {
    double[] p0 = {i, p[0] + ny, p[1] + nz};
    double[] p1 = {i + 1, p[0] + ny, p[1] + nz};
    double[] p2 = {i + 1, p[0] - ny, p[1] - nz};
    double[] p3 = {i, p[0] - ny, p[1] - nz};
    float u0 = sp.getMinU();
    float u1 = sp.getMaxU();
    float v0 = sp.getInterpolatedV(15);
    float v1 = sp.getMaxV();
    face(p0, p1, p2, p3, new float[]{u0, u1, u1, u0}, new float[]{v0, v0, v1, v1}, 0, oy, oz,
        lm);
  }

  // --- drawing -----------------------------------------------------------------------------------

  /** An axis-aligned plane quad facing (ox, oy, oz), textured with the whole sprite. */
  private void quad(TextureAtlasSprite sp, double x0, double y0, double z0, double x1, double y1,
      double z1, double ox, double oy, double oz, float u0, float u1, float v0, float v1,
      int lm) {
    double[] a = {x0, y0, z0};
    double[] b = {x1, y0, z1};
    double[] c = {x1, y1, z1};
    double[] d = {x0, y1, z0};
    float uu0 = sp.getInterpolatedU(u0 * 16);
    float uu1 = sp.getInterpolatedU(u1 * 16);
    float vv0 = sp.getInterpolatedV(v0 * 16);
    float vv1 = sp.getInterpolatedV(v1 * 16);
    face(a, b, c, d, new float[]{uu0, uu1, uu1, uu0}, new float[]{vv1, vv1, vv0, vv0}, ox, oy,
        oz, lm);
  }

  /** A small box, every face textured with the whole sprite. */
  private void box(TextureAtlasSprite sp, double x0, double y0, double z0, double x1, double y1,
      double z1, int lm) {
    quad(sp, x0, y0, z1, x1, y1, z1, 0, 0, 1, 0F, 1F, 0F, 1F, lm);
    quad(sp, x1, y0, z0, x0, y1, z0, 0, 0, -1, 0F, 1F, 0F, 1F, lm);
    double[][] top = {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
    double[][] bot = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}};
    float[] u = {sp.getMinU(), sp.getMaxU(), sp.getMaxU(), sp.getMinU()};
    float[] v = {sp.getMinV(), sp.getMinV(), sp.getMaxV(), sp.getMaxV()};
    face(top[0], top[1], top[2], top[3], u, v, 0, 1, 0, lm);
    face(bot[0], bot[1], bot[2], bot[3], u, v, 0, -1, 0, lm);
    quad(sp, x0, y0, z0, x0, y1, z1, -1, 0, 0, 0F, 1F, 0F, 1F, lm);
    quad(sp, x1, y0, z1, x1, y1, z0, 1, 0, 0, 0F, 1F, 0F, 1F, lm);
  }

  /**
   * One quad, wound so its front faces {@code (ox, oy, oz)}, and shaded by that direction the way
   * vanilla shades block faces -- the crane's geometry does the same.
   */
  private void face(double[] a, double[] b, double[] c, double[] d, float[] u, float[] v,
      double ox, double oy, double oz, int lm) {
    double e1x = b[0] - a[0];
    double e1y = b[1] - a[1];
    double e1z = b[2] - a[2];
    double e2x = c[0] - a[0];
    double e2y = c[1] - a[1];
    double e2z = c[2] - a[2];
    double nx = e1y * e2z - e1z * e2y;
    double ny = e1z * e2x - e1x * e2z;
    double nz = e1x * e2y - e1y * e2x;
    double[][] p = {a, b, c, d};
    int[] order = nx * ox + ny * oy + nz * oz < 0 ? new int[]{0, 3, 2, 1} : new int[]{0, 1, 2, 3};
    double ol = Math.sqrt(ox * ox + oy * oy + oz * oz);
    double fx = ox / ol;
    double fy = oy / ol;
    double fz = oz / ol;
    float shade = (float) (fx * fx * 0.6 + fy * fy * (fy > 0 ? 1.0 : 0.5) + fz * fz * 0.8);
    int grey = Math.min(255, (int) (shade * 255));
    int sky = (lm >> 16) & 0xFFFF;
    int block = lm & 0xFFFF;
    for (int i : order) {
      buf.pos(p[i][0], p[i][1], p[i][2]).color(grey, grey, grey, 255).tex(u[i], v[i])
          .lightmap(sky, block).endVertex();
    }
  }

  /**
   * The light at a point in the door's frame: the block it is in, turned to the world. A point in
   * a solid block (a panel sliding into the wall above) takes the light of the cell below it.
   */
  private int light(double mx, double my, double mz) {
    EnumFacing cw = facing.rotateY();
    // The door frame's x runs clockwise from the facing; its -z is the facing (the inside).
    double dx = mx - 0.5;
    double dz = mz - 0.5;
    double wx = anchor.getX() + 0.5 + cw.getXOffset() * dx - facing.getXOffset() * dz;
    double wz = anchor.getZ() + 0.5 + cw.getZOffset() * dx - facing.getZOffset() * dz;
    BlockPos p = new BlockPos(Math.floor(wx), anchor.getY() + Math.floor(my), Math.floor(wz));
    if (world.getBlockState(p).isOpaqueCube()) {
      p = p.down();
    }
    return world.getCombinedLight(p, 0);
  }
}
