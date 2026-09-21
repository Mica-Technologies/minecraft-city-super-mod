package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Interface for emergency light blocks that have light bulb elements which should render
 * a glow/halo effect via {@link TileEntityEmergencyLightRenderer} when active (no power).
 *
 * <p>Emergency lights have two directional bulbs that face forward. Each bulb's position
 * is specified in the 0-16 model coordinate system matching the Blockbench model.
 *
 * <p>The defaults describe the emergency light blocks: a {@link AbstractPoweredBlockRotatableNSEWUD}
 * with the two bulbs above. A block whose bulbs depend on its setup -- an exit sign with
 * emergency heads -- overrides the four methods that take a world and position, and the renderer
 * keeps one glow display list per {@link #getGlowVariant variant}.
 */
public interface IEmergencyLightBlock {

  /**
   * Returns the "from" corner of the left bulb element in model coordinates (0-16).
   */
  default float[] getLeftBulbFrom() {
    return new float[]{1.0f, 4.0f, 13.0f};
  }

  /**
   * Returns the "to" corner of the left bulb element in model coordinates (0-16).
   */
  default float[] getLeftBulbTo() {
    return new float[]{5.0f, 8.0f, 15.0f};
  }

  /**
   * Returns the "from" corner of the right bulb element in model coordinates (0-16).
   */
  default float[] getRightBulbFrom() {
    return new float[]{11.0f, 4.0f, 13.0f};
  }

  /**
   * Returns the "to" corner of the right bulb element in model coordinates (0-16).
   */
  default float[] getRightBulbTo() {
    return new float[]{15.0f, 8.0f, 15.0f};
  }

  /**
   * Whether the bulbs are lit: an emergency light lights when it loses mains power, which is
   * when its {@code POWERED} property is false.
   */
  default boolean isEmergencyLightActive(IBlockAccess world, BlockPos pos, IBlockState state) {
    return state.getPropertyKeys().contains(AbstractPoweredBlockRotatableNSEWUD.POWERED)
        && !state.getValue(AbstractPoweredBlockRotatableNSEWUD.POWERED)
        && state.getPropertyKeys().contains(AbstractBlockRotatableNSEWUD.FACING);
  }

  /** The way the bulbs face: the glow is drawn facing north and turned to this. */
  default EnumFacing getEmergencyLightFacing(IBlockState state) {
    return state.getValue(AbstractBlockRotatableNSEWUD.FACING);
  }

  /**
   * Which bulb layout this block shows at {@code pos}, from 0 to 255. Blocks of one class with
   * the same variant share one glow display list, so it must be the same whenever
   * {@link #getBulbs} is.
   */
  default int getGlowVariant(IBlockAccess world, BlockPos pos, IBlockState state) {
    return 0;
  }

  /**
   * Every bulb as {@code {fromX, fromY, fromZ, toX, toY, toZ}} in model coordinates, facing north.
   * The glow's bright face is drawn just in front of each bulb's low-z face.
   */
  default float[][] getBulbs(IBlockAccess world, BlockPos pos, IBlockState state) {
    float[] lf = getLeftBulbFrom();
    float[] lt = getLeftBulbTo();
    float[] rf = getRightBulbFrom();
    float[] rt = getRightBulbTo();
    return new float[][]{{lf[0], lf[1], lf[2], lt[0], lt[1], lt[2]},
        {rf[0], rf[1], rf[2], rt[0], rt[1], rt[2]}};
  }

  /**
   * Whether the light cone is narrow: less than half the spread, for bulbs beside a surface the
   * wide cone would wash out, such as an exit sign's face. Part of the glow list, so constant per
   * block class.
   */
  default boolean hasNarrowCone() {
    return false;
  }
}
