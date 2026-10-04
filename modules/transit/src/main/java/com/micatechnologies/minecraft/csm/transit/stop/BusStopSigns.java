package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.BlockUtils;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import com.micatechnologies.minecraft.csm.trafficsigns.AbstractBlockSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * What the bus stop's signs share. A stop is built on the road sign system: a column of Roads'
 * sign posts and road signs, with the flag, the arrival display and the poster cases among them,
 * each an {@link AbstractBlockSign} carrying its own length of post. So a stop's pieces find each
 * other by walking that column, and their renderers draw where the sign system's three shift
 * models put the sign.
 *
 * @since 2026.9
 */
public final class BusStopSigns {

  /** How far up or down the post a display looks for its stop's flag. */
  public static final int REACH = 8;

  /**
   * How far back each shift model draws the sign, in sixteenths, indexed by
   * {@link SignShift#ordinal()}: as authored, the setback's 12.5, and back to back 28.3 --
   * {@code SHIFTS} in {@code gen_transit_stops.py}. Back to back is 0.2 short of the other road
   * signs' 28.5 so the face stands clear of the partner's post end rather than a hundredth of a
   * unit in front of it (see {@code TRANSIT_SYSTEM.md}).
   */
  static final float[] SHIFT_Z = {0.0f, 12.5f, 28.3f};

  /**
   * Minecraft's diffuse shade for a vertical block face: 0.8 looking along z, 0.6 along x. A baked
   * face carries it in its vertex colours and a tile entity renderer gets none, so text printed
   * on a sign is multiplied by it to match the plate under it.
   */
  private static final float SHADE_Z = 0.8f;
  private static final float SHADE_X = 0.6f;

  private BusStopSigns() {
  }

  /**
   * How far back a sign in {@code shift} is drawn.
   *
   * @param shift the shift
   *
   * @return the offset in sixteenths
   */
  static float shiftZ(SignShift shift) {
    return SHIFT_Z[shift.ordinal()];
  }

  /**
   * The shade the sign's faces are drawn at. A diagonal facing leaves the face's normal halfway
   * between two sides and Minecraft resolves that tie toward north or south, so only a due east
   * or west sign takes the darker one.
   *
   * @param facing the sign's facing
   *
   * @return the multiplier
   */
  static float shade(DirectionEight facing) {
    return facing == DirectionEight.E || facing == DirectionEight.W ? SHADE_X : SHADE_Z;
  }

  /**
   * The flag of the stop {@code pos} is in: the nearest bus stop flag up or down the post, up
   * first, through the signs and posts of the column. Safe off the main thread: a chunk being
   * rendered is read without creating a tile entity in it.
   *
   * @param world the world
   * @param pos   a sign of the stop
   *
   * @return the flag's tile entity, or null if the post has none
   */
  public static TileEntityBusStopFlag flagNear(IBlockAccess world, BlockPos pos) {
    for (int dir = 1; dir >= -1; dir -= 2) {
      BlockPos p = pos;
      for (int i = 0; i < REACH; i++) {
        p = p.up(dir);
        IBlockState state = world.getBlockState(p);
        if (state.getBlock() instanceof BlockBusStopFlag) {
          TileEntity te = tileEntity(world, p);
          if (te instanceof TileEntityBusStopFlag) {
            return (TileEntityBusStopFlag) te;
          }
        }
        if (!(state.getBlock() instanceof AbstractBlockSign)) {
          break;
        }
      }
    }
    return null;
  }

  /**
   * The tile entity at {@code pos}, without creating one in a chunk being rendered.
   *
   * @param world the world
   * @param pos   the position
   *
   * @return the tile entity, or null
   */
  public static TileEntity tileEntity(IBlockAccess world, BlockPos pos) {
    return BlockUtils.getTileEntitySafe(world, pos);
  }
}
