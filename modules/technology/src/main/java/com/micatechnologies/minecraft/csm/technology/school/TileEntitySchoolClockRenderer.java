package com.micatechnologies.minecraft.csm.technology.school;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws the hands of every school clock in the Technology tab, on every dial the block names
 * ({@link ISchoolClock}): the hour and minute hands from the world's time of day and, where the
 * dial has one, the red sweep hand (see {@link ClockTime} for why it sweeps once a real minute).
 * Everything else about a clock is its baked model.
 *
 * <p>Cheap by construction: one untextured batch per clock, two or three quads a dial (the
 * bracket and ceiling clocks have two dials), nothing read from the world but its time, and
 * nothing allocated per frame beyond the hand corners. The block and metadata come from the
 * tile entity's own cache. Hands take the block's light from the lightmap, as the model does,
 * and are darkened by the same face shading the model's dial gets (0.8 on a north or south face,
 * 0.6 on an east or west one), so they do not glow against it.</p>
 *
 * <p>A dial's numbers are in the model's frame, facing north; they are turned to the block's
 * facing as the blockstate turns the model ({@code y} 90 for east, 180 south, 270 west).</p>
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class TileEntitySchoolClockRenderer
    extends TileEntitySpecialRenderer<TileEntitySchoolClock> {

  /** Hand proportions, as fractions of the dial's radius: length, tail, half width, lift. */
  private static final double HOUR_LENGTH = 0.50;
  private static final double HOUR_TAIL = 0.12;
  private static final double HOUR_HALF_WIDTH = 0.06;
  private static final double MINUTE_LENGTH = 0.78;
  private static final double MINUTE_TAIL = 0.14;
  private static final double MINUTE_HALF_WIDTH = 0.042;
  private static final double SWEEP_LENGTH = 0.84;
  private static final double SWEEP_TAIL = 0.24;
  /** The sweep hand is never drawn thinner than this, in sixteenths, or it shimmers. */
  private static final double SWEEP_MIN_HALF_WIDTH = 0.12;
  private static final double SWEEP_HALF_WIDTH = 0.012;
  /** How far in front of the dial each hand lies, in sixteenths. */
  private static final double HOUR_LIFT = 0.04;
  private static final double MINUTE_LIFT = 0.08;
  private static final double SWEEP_LIFT = 0.12;

  private static final int HAND_R = 22;
  private static final int HAND_G = 22;
  private static final int HAND_B = 24;
  private static final int SWEEP_R = 196;
  private static final int SWEEP_G = 30;
  private static final int SWEEP_B = 28;

  @Override
  public void render(TileEntitySchoolClock te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    World world = te.getWorld();
    Block block = te.getBlockType();
    if (world == null || !(block instanceof ISchoolClock)) {
      return;
    }
    int meta = te.getBlockMetadata();
    SchoolClockDial[] dials = ((ISchoolClock) block).getClockDials(meta);
    if (dials.length == 0) {
      return;
    }
    EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
    long worldTime = world.getWorldTime();
    double hour = ClockTime.hourHandDegrees(worldTime);
    double minute = ClockTime.minuteHandDegrees(worldTime);
    double sweep = ClockTime.sweepHandDegrees(world.getTotalWorldTime(), partialTicks);

    GlStateManager.disableTexture2D();
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    for (SchoolClockDial dial : dials) {
      double r = dial.getRadius();
      EnumFacing out = turn(dial.getFace(), facing);
      float shade = out.getAxis() == EnumFacing.Axis.X ? 0.6F : 0.8F;
      hand(buffer, x, y, z, dial, facing, hour, HOUR_LENGTH * r, HOUR_TAIL * r,
          HOUR_HALF_WIDTH * r, HOUR_LIFT, HAND_R, HAND_G, HAND_B, shade);
      hand(buffer, x, y, z, dial, facing, minute, MINUTE_LENGTH * r, MINUTE_TAIL * r,
          MINUTE_HALF_WIDTH * r, MINUTE_LIFT, HAND_R, HAND_G, HAND_B, shade);
      if (dial.hasSweepHand()) {
        hand(buffer, x, y, z, dial, facing, sweep, SWEEP_LENGTH * r, SWEEP_TAIL * r,
            Math.max(SWEEP_MIN_HALF_WIDTH, SWEEP_HALF_WIDTH * r), SWEEP_LIFT, SWEEP_R, SWEEP_G,
            SWEEP_B, shade);
      }
    }
    tessellator.draw();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.enableTexture2D();
  }

  /**
   * One hand: its corners in the dial's plane ({@link ClockTime#handCorners}), placed on the
   * dial in the model's frame, then turned to the block's facing and moved to the block.
   */
  private static void hand(BufferBuilder buffer, double bx, double by, double bz,
      SchoolClockDial dial, EnumFacing facing, double degrees, double length, double tail,
      double halfWidth, double lift, int red, int green, int blue, float shade) {
    double[] c = ClockTime.handCorners(degrees, length, tail, halfWidth);
    EnumFacing face = dial.getFace();
    // The dial's outward normal, and the viewer's right as they look at it (up x normal).
    double nx = face.getXOffset();
    double nz = face.getZOffset();
    double rx = nz;
    double rz = -nx;
    int r = (int) (red * shade);
    int g = (int) (green * shade);
    int b = (int) (blue * shade);
    for (int i = 0; i < 4; i++) {
      double u = c[i * 2];
      double v = c[i * 2 + 1];
      double mx = dial.getX() + u * rx + lift * nx;
      double my = dial.getY() + v;
      double mz = dial.getZ() + u * rz + lift * nz;
      double wx;
      double wz;
      switch (facing) {
        case EAST:
          wx = 16 - mz;
          wz = mx;
          break;
        case SOUTH:
          wx = 16 - mx;
          wz = 16 - mz;
          break;
        case WEST:
          wx = mz;
          wz = 16 - mx;
          break;
        default:
          wx = mx;
          wz = mz;
          break;
      }
      buffer.pos(bx + wx / 16.0, by + my / 16.0, bz + wz / 16.0).color(r, g, b, 255)
          .endVertex();
    }
  }

  /** The way a face of the model looks once the block is turned to {@code facing}. */
  static EnumFacing turn(EnumFacing modelFace, EnumFacing facing) {
    EnumFacing out = modelFace;
    int steps = (facing.getHorizontalIndex() + 2) & 3;   // north 0, east 1, south 2, west 3
    for (int i = 0; i < steps; i++) {
      out = out.rotateY();
    }
    return out;
  }
}
