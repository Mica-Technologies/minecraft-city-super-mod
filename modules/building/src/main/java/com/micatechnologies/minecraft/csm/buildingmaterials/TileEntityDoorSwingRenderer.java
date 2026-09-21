package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.CsmConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a door while it swings: both halves' own closed models, turned about the hinge, and a
 * fitted closer's arm solved for the angle the door is at.
 *
 * <p>Nothing is drawn by the models while a door swings (its {@code swing} state has none), so this
 * draws the leaf and whatever is fitted to it -- handles, push bar, the closer's body -- from the
 * very models the door rests in, turned about the pivot {@code gen_doors.py} makes its open models
 * with. A quarter turn about that pivot IS the open model, so the swing ends on exactly the picture
 * that replaces it. With {@code animateDoors} off, the door is drawn where it is going at once.</p>
 *
 * <p>Not all of a closer swings. The model marks its parts by tint index (no colour handler is
 * registered for the doors, so the index tints nothing): the shoe on the wall ({@code TINT_FIXED})
 * is drawn where it is, and the arm ({@code TINT_ARM}) is left out and drawn here instead, its elbow
 * solved by {@link DoorCloserArm} between the spindle the leaf has carried round and the shoe that
 * stayed put -- the same solution the models are baked from, shut and open.</p>
 *
 * <p>Each face is shaded as the world shades a block's faces, by which way it now faces, so a leaf
 * that ends its swing facing east is as dark as the east-facing open model that takes over.</p>
 *
 * @version 1.1
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityDoorSwingRenderer extends TileEntitySpecialRenderer<TileEntityDoorSwing> {

  /**
   * SHARED with gen_doors.PIVOT: where a left-hinged inswing leaf turns, with the inside to the
   * north. An outswing leaf turns about the depth mirror of it, the other way.
   */
  private static final double PIVOT_X = 0.875 / 16;
  private static final double PIVOT_Z = 15.125 / 16;

  /** SHARED with gen_doors.TINT_*: which part of a door model a quad belongs to. */
  private static final int TINT_FIXED = 1;
  private static final int TINT_ARM = 2;

  @Override
  public void render(TileEntityDoorSwing te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    BlockPos upperPos = te.getPos();
    IBlockState upper = te.getWorld().getBlockState(upperPos);
    if (!(upper.getBlock() instanceof BlockBuildingDoor)) {
      return;
    }
    BlockBuildingDoor block = (BlockBuildingDoor) upper.getBlock();
    IBlockState door = upper.getActualState(te.getWorld(), upperPos);
    EnumFacing facing = door.getValue(BlockBuildingDoor.FACING);
    boolean left = door.getValue(BlockBuildingDoor.HINGE) == BlockBuildingDoor.Hinge.LEFT;
    double p = CsmConfig.isDoorAnimationEnabled() ? te.progress(partialTicks) : 1.0;
    double eased = 1 - (1 - p) * (1 - p);
    double openness = te.isOpening() ? eased : 1 - eased;
    double turn = 90.0 * openness;
    // An outswing door turns the other way, about the depth mirror of the pivot.
    boolean out = block.outswing();
    float angle = (float) turn * (left ? 1 : -1) * (out ? -1 : 1);
    double pz = out ? 1 - PIVOT_Z : PIVOT_Z;
    float facingTurn = -(facing.getHorizontalAngle() + 180F);

    // The closed models, facing north, so the facing and the hinge turn are both applied here.
    IBlockState closed = block.getDefaultState().withProperty(BlockBuildingDoor.OPEN, false)
        .withProperty(BlockBuildingDoor.SWING, false)
        .withProperty(BlockBuildingDoor.HINGE, door.getValue(BlockBuildingDoor.HINGE))
        .withProperty(BlockBuildingDoor.CLOSER, door.getValue(BlockBuildingDoor.CLOSER));
    IBlockState lowerState = closed.withProperty(BlockBuildingDoor.HALF,
        BlockBuildingDoor.Half.LOWER);
    IBlockState upperState = closed.withProperty(BlockBuildingDoor.HALF,
        BlockBuildingDoor.Half.UPPER);
    // The TE is on the upper half; the lower is a block below it.
    List<BakedQuad> lowerQuads = quads(lowerState);
    List<BakedQuad> upperQuads = quads(upperState);
    int light = te.getWorld().getCombinedLight(upperPos.down(), 0);

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.pushMatrix();
    RenderHelper.disableStandardItemLighting();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >> 16);
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(facingTurn, 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    // What stays with the frame: a closer's shoe, and the arm reaching from it to the leaf.
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
    TextureAtlasSprite armSprite = null;
    for (BakedQuad quad : upperQuads) {
      if (quad.getTintIndex() == TINT_FIXED) {
        put(buffer, quad, facingTurn, 0);
      } else if (quad.getTintIndex() == TINT_ARM) {
        armSprite = quad.getSprite();
      }
    }
    if (armSprite != null) {
      drawArm(buffer, armSprite, turn, left, out, facingTurn);
    }
    tessellator.draw();

    // The leaf and everything fitted to it, turned about the hinge.
    double px = left ? PIVOT_X : 1 - PIVOT_X;
    GlStateManager.translate(px, 0, pz);
    GlStateManager.rotate(angle, 0F, 1F, 0F);
    GlStateManager.translate(-px, 0, -pz);
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
    for (BakedQuad quad : lowerQuads) {
      if (!quad.hasTintIndex()) {
        put(buffer, quad, facingTurn + angle, -1);
      }
    }
    for (BakedQuad quad : upperQuads) {
      if (!quad.hasTintIndex()) {
        put(buffer, quad, facingTurn + angle, 0);
      }
    }
    tessellator.draw();

    GlStateManager.disableBlend();
    RenderHelper.enableStandardItemLighting();
    GlStateManager.popMatrix();
  }

  /** Every quad of a state's model, from every side. */
  private static List<BakedQuad> quads(IBlockState state) {
    IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
        .getModelForState(state);
    List<BakedQuad> out = new ArrayList<>(model.getQuads(state, null, 0L));
    for (EnumFacing side : EnumFacing.values()) {
      out.addAll(model.getQuads(state, side, 0L));
    }
    return out;
  }

  /**
   * One baked quad, shaded for the way it faces once turned {@code yaw} degrees, and moved down a
   * block for the lower half.
   */
  private static void put(BufferBuilder buffer, BakedQuad quad, float yaw, double dy) {
    LightUtil.renderQuadColor(buffer, quad, 0xFFFFFFFF);
    Vec3i n = quad.getFace().getDirectionVec();
    float s = shade(n.getX(), n.getY(), n.getZ(), yaw);
    buffer.putColorRGB_F4(s, s, s);
    if (dy != 0) {
      buffer.putPosition(0, dy, 0);
    }
  }

  /**
   * How bright the world draws a face whose normal, turned {@code yaw} degrees about y as the
   * renderer turns it, points this way: the block face shading, 1 up, 0.5 down, 0.8 north and
   * south, 0.6 east and west, and in between for the faces of a door part way round.
   */
  private static float shade(double nx, double ny, double nz, float yaw) {
    double a = Math.toRadians(yaw);
    double x = nx * Math.cos(a) + nz * Math.sin(a);
    double z = -nx * Math.sin(a) + nz * Math.cos(a);
    return LightUtil.diffuseLight((float) x, (float) ny, (float) z);
  }

  /**
   * The closer's arm with the door {@code turn} degrees open: the main arm from the spindle to the
   * elbow, the forearm from the elbow to the shoe, and the elbow's pin, from {@link DoorCloserArm}
   * mirrored to this door's hinge side and swing.
   */
  private static void drawArm(BufferBuilder buffer, TextureAtlasSprite sprite, double turn,
      boolean left, boolean out, float yaw) {
    double[] j = DoorCloserArm.solve(turn);
    double sx = mirror(j[0], !left);
    double sz = mirror(j[1], out);
    double ex = mirror(j[2], !left);
    double ez = mirror(j[3], out);
    double px = mirror(DoorCloserArm.SHOE_X, !left);
    double pz = mirror(DoorCloserArm.SHOE_Z, out);
    link(buffer, sprite, sx, sz, ex, ez, DoorCloserArm.MAIN_Y0, DoorCloserArm.MAIN_Y1,
        DoorCloserArm.LINK_HALF, yaw);
    link(buffer, sprite, ex, ez, px, pz, DoorCloserArm.FORE_Y0, DoorCloserArm.FORE_Y1,
        DoorCloserArm.LINK_HALF, yaw);
    double h = DoorCloserArm.PIN_HALF;
    link(buffer, sprite, ex - h, ez, ex + h, ez, DoorCloserArm.PIN_Y0, DoorCloserArm.PIN_Y1, h,
        yaw);
  }

  private static double mirror(double v, boolean flip) {
    return flip ? 16 - v : v;
  }

  /**
   * A straight bar from (ax, az) to (bx, bz) at heights y0..y1, {@code half} either side of the
   * line, in pixels. Each face is wound to face outward whichever way the mirrors left it.
   */
  private static void link(BufferBuilder buffer, TextureAtlasSprite sprite, double ax, double az,
      double bx, double bz, double y0, double y1, double half, float yaw) {
    double len = Math.sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az));
    if (len < 1e-6) {
      return;
    }
    double dx = (bx - ax) / len;
    double dz = (bz - az) / len;
    double nx = -dz * half;
    double nz = dx * half;
    // Corners: 0-3 at y0, 4-7 at y1; a-side then b-side, each -n then +n.
    double[][] c = new double[8][];
    for (int k = 0; k < 8; k++) {
      boolean top = k >= 4;
      boolean far = (k & 2) != 0;
      boolean plus = (k & 1) != 0;
      double cx = (far ? bx : ax) + (plus ? nx : -nx);
      double cz = (far ? bz : az) + (plus ? nz : -nz);
      c[k] = new double[]{cx, top ? y1 : y0, cz};
    }
    double width = 2 * half;
    double height = y1 - y0;
    double run = Math.min(len, 16);
    face(buffer, sprite, c, 4, 5, 7, 6, run, width, yaw);
    face(buffer, sprite, c, 0, 2, 3, 1, run, width, yaw);
    face(buffer, sprite, c, 1, 3, 7, 5, run, height, yaw);
    face(buffer, sprite, c, 0, 4, 6, 2, run, height, yaw);
    face(buffer, sprite, c, 0, 1, 5, 4, width, height, yaw);
    face(buffer, sprite, c, 2, 6, 7, 3, width, height, yaw);
  }

  /**
   * One face of a bar: the corners {@code i0..i3} round it, and the size of the texture window it
   * takes. Which way is out is read off the corners -- from the bar's middle to the face's -- and
   * the face is wound and shaded for that, so a mirror cannot turn it inside out.
   */
  private static void face(BufferBuilder buffer, TextureAtlasSprite sprite, double[][] c, int i0,
      int i1, int i2, int i3, double u, double v, float yaw) {
    int[] order = {i0, i1, i2, i3};
    double[] a = c[i0];
    double[] b = c[i1];
    double[] d = c[i2];
    // The normal the corners wind about, and the way out from the bar's middle.
    double wx = (b[1] - a[1]) * (d[2] - a[2]) - (b[2] - a[2]) * (d[1] - a[1]);
    double wy = (b[2] - a[2]) * (d[0] - a[0]) - (b[0] - a[0]) * (d[2] - a[2]);
    double wz = (b[0] - a[0]) * (d[1] - a[1]) - (b[1] - a[1]) * (d[0] - a[0]);
    double ox = 0;
    double oy = 0;
    double oz = 0;
    for (int k = 0; k < 8; k++) {
      ox -= c[k][0] / 8;
      oy -= c[k][1] / 8;
      oz -= c[k][2] / 8;
    }
    for (int k : order) {
      ox += c[k][0] / 4;
      oy += c[k][1] / 4;
      oz += c[k][2] / 4;
    }
    if (wx * ox + wy * oy + wz * oz < 0) {
      order = new int[]{i3, i2, i1, i0};
    }
    double len = Math.sqrt(ox * ox + oy * oy + oz * oz);
    ox /= len;
    oy /= len;
    oz /= len;
    float s = shade(ox, oy, oz, yaw);
    double[][] uv = {{0, 0}, {u, 0}, {u, v}, {0, v}};
    for (int k = 0; k < 4; k++) {
      double[] q = c[order[k]];
      buffer.pos(q[0] / 16, q[1] / 16, q[2] / 16).color(s, s, s, 1F)
          .tex(sprite.getInterpolatedU(uv[k][0]), sprite.getInterpolatedV(uv[k][1]))
          .normal((float) ox, (float) oy, (float) oz).endVertex();
    }
  }
}
