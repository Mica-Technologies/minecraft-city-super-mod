package com.micatechnologies.minecraft.csm.healthcare;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A piece with a screen that a click turns on and off: the bedside monitor, the infusion pump,
 * the patient-room TV and the vitals unit. The picture is an animated texture, so a screen that
 * is on costs no more than one that is off: nothing ticks and nothing renders it but the chunk.
 *
 * <p>{@link #ON} is the first flag: bit 2 on the floor, bit 3 on a wall.</p>
 *
 * @since 2026.10
 */
public class BlockHealthcareScreen extends BlockHealthcare {

  private static final PropertyBool[] FLAGS = {ON};

  /**
   * Constructs a screen.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param material     what it is made of
   * @param mount        floor or wall
   * @param collides     whether it can be walked into
   */
  public BlockHealthcareScreen(String registryName, int[] box, HealthcareMaterial material,
      Mount mount, boolean collides) {
    super(registryName, box, material, mount, collides);
  }

  @Override
  protected PropertyBool[] storedFlags() {
    return FLAGS;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    // A seat with a screen (the infusion recliner and its pump): a click sits, a sneak-click
    // switches the screen. Anything else: a click switches it.
    if (hasSeat() && !player.isSneaking()) {
      return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }
    if (player.isSneaking() && !hasSeat()) {
      return false;
    }
    if (!world.isRemote) {
      world.setBlockState(pos, state.cycleProperty(ON), 3);
    }
    return true;
  }
}
