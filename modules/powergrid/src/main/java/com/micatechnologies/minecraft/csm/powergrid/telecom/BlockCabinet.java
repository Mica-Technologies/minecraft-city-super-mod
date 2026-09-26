package com.micatechnologies.minecraft.csm.powergrid.telecom;

import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockSwitchedUnit;
import com.micatechnologies.minecraft.csm.streetscape.UtilityBoxSpec;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A pad-mounted telecom cabinet whose doors a click opens and shuts: Roads' utility box
 * multi-block (the root draws the whole unit, invisible parts fill the rest, placing is all or
 * nothing) with {@link #ON} meaning the doors stand open, swung out on their hinges with the
 * splice trays and patch panels inside showing. The fibre distribution cabinet is one.
 *
 * <p>Nothing ticks and there is no tile entity: eight states.</p>
 *
 * @since 2026.9
 */
public class BlockCabinet extends BlockSwitchedUnit {

  /**
   * @param registryName its registry name
   * @param spec         its size and shape
   */
  public BlockCabinet(String registryName, UtilityBoxSpec spec) {
    super(registryName, spec, true, 0);
  }

  @Override
  protected void toggled(World world, BlockPos pos, EntityPlayer player, boolean on) {
    world.playSound(null, pos,
        on ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE,
        SoundCategory.BLOCKS, 0.7F, 1.1F);
  }
}
