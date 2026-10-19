package com.micatechnologies.minecraft.csm.healthcare.diagnostics;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcarePair;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The refrigerated morgue cabinet ({@code morgue_cabinet}): two tiers, one door and one tray to a
 * cell, two blocks tall as a {@link BlockHealthcarePair tall pair}. A click on a cell opens or
 * shuts that cell's door; an open door stands square to the cabinet with the tray slid half out
 * in front.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), bit 2 the upper cell, bit 3 this
 * cell's door open.</p>
 *
 * @since 2026.10
 */
public class BlockMorgueCabinet extends BlockHealthcarePair {

  /** This cell's door is open. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  private static final PropertyBool[] FLAGS = {SECOND, OPEN};

  /** Constructs the cabinet. */
  public BlockMorgueCabinet() {
    super("morgue_cabinet", new int[]{0, 0, 0, 16, 16, 16}, HealthcareMaterial.METAL,
        BlockHealthcare.Mount.FLOOR, Shape.TALL);
  }

  @Override
  protected PropertyBool[] storedFlags() {
    return FLAGS;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      world.playSound(null, pos, open ? SoundEvents.BLOCK_IRON_DOOR_OPEN
          : SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.8F, 1.0F);
    }
    return true;
  }
}
