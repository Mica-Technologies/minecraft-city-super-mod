package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.lifesafety.LifeSafetySounds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A blue-light emergency call box: its blue lamp always lit, so it can be found at night. Press
 * its button (right-click) and it rings through to a dispatcher, as the real ones on campuses
 * and in car parks do: a click, the ring-back tone, the line picking up.
 *
 * @since 2026.9
 */
public class BlockCallBox extends BlockLitProp {

  public BlockCallBox(String registryName, int[] box, int light) {
    super(registryName, box, light);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand == EnumHand.MAIN_HAND && !world.isRemote) {
      SoundEvent ring = LifeSafetySounds.CALL_BOX_RING.getSoundEvent();
      if (ring != null) {
        world.playSound(null, pos, ring, SoundCategory.BLOCKS, 1.0F, 1.0F);
      }
    }
    return true;
  }
}
