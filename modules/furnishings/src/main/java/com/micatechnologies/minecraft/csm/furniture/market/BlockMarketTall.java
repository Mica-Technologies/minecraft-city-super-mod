package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialTall;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A store machine two blocks tall, placed and broken as one ({@link BlockResidentialTall}): the
 * self-checkout kiosk and the bottle return machine. Its screen glows, so its upper half gives a
 * little light; a machine given a sound plays it on right-click (the self-checkout's scanner
 * beep).
 *
 * @since 2026.9
 */
public class BlockMarketTall extends BlockResidentialTall {

  private final int screenLight;
  @Nullable
  private final ICsmSound clickSound;

  /**
   * Constructs a machine.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths from the floor (up to 32)
   * @param screenLight  the light its upper half gives, 0 to 15
   * @param clickSound   what it plays on right-click, or null
   */
  public BlockMarketTall(String registryName, int[] box, int screenLight,
      @Nullable ICsmSound clickSound) {
    super(registryName, box, false, 0, null, null);
    this.screenLight = screenLight;
    this.clickSound = clickSound;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(UPPER) ? screenLight : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (clickSound == null || player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      SoundEvent event = clickSound.getSoundEvent();
      if (event != null) {
        world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.7F, 1.0F);
      }
    }
    return true;
  }
}
