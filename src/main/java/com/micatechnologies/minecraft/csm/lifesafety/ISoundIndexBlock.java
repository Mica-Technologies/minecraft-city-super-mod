package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A sounder whose alarm tone is chosen per placed block rather than per block state.
 *
 * <p>A block's metadata carries the rotation of a fire alarm appliance and has four bits, so a
 * NSEWUD device can encode at most two selectable sounds alongside it. Appliances offering more
 * keep the choice in a {@link TileEntityFireAlarmSoundIndex} instead, which means resolving their
 * sound needs the world and the position -- neither of which
 * {@link AbstractBlockFireAlarmSounder#getSoundResourceName(IBlockState)} has.</p>
 *
 * <p>This interface is how the control panel asks. It used to ask by naming each such block class
 * in an {@code instanceof} chain, which quietly left any block added later playing its first
 * sound no matter what was selected on it.</p>
 */
public interface ISoundIndexBlock {

  /**
   * The sound resource this particular placed block should play, read from its tile entity.
   *
   * @param world the world the block is in
   * @param pos   the block's position
   * @param state the block's state
   *
   * @return the sound resource name, or null if this appliance is silent
   */
  String getSoundResourceName(World world, BlockPos pos, IBlockState state);
}
