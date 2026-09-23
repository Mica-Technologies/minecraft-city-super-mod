package com.micatechnologies.minecraft.csm.lifesafety.fireprotection;

import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A magnetic door holder: while its panel is quiet it holds the door beside it open, and when
 * the panel alarms it lets go and the door shuts, as a fire door in a corridor must.
 *
 * <p>It holds a door the way a player would: with redstone. It gives weak power on every side
 * while holding, and a door is held open by power (CSM's doors and vanilla ones alike, see
 * DOORS.md), so it needs to know nothing about doors and works with any of them. Place it next
 * to the door, on the wall or floor the door swings against.</p>
 *
 * @since 2026.9
 */
public class BlockMagneticDoorHolder extends AbstractBlockPanelFollower {

  public BlockMagneticDoorHolder(String registryName, int[] box) {
    super(registryName, box);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(@Nonnull IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return state.getValue(ALARM) ? 0 : 15;
  }
}
