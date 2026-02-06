package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockFireAlarmFireLiteBG8 extends AbstractBlockFireAlarmActivator {

  @Override
  public boolean onBlockActivated(World world,
      BlockPos blockPos,
      IBlockState blockState,
      EntityPlayer entityPlayer,
      EnumHand enumHand,
      EnumFacing enumFacing,
      float p_onBlockActivated_7_,
      float p_onBlockActivated_8_,
      float p_onBlockActivated_9_) {
    if (entityPlayer.inventory.getCurrentItem() != null &&
        (entityPlayer.inventory.getCurrentItem().getItem() instanceof ItemFireAlarmLinker)) {
      return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing,
          p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_);
    }
    boolean activated = activateLinkedPanel(world, blockPos, entityPlayer);
    if (!activated && !world.isRemote) {
      entityPlayer.sendMessage(
          new TextComponentString("WARNING: This pull station has lost connection, " +
              "has failed or is otherwise not functional."));
    }
    return true;
  }

  @Override
  public String getBlockRegistryName() {
    return "firealarmfirelitebg8";
  }

  @Override
  public int getBlockTickRate() {
    return 20;
  }

  @Override
  public void onTick(World world, BlockPos blockPos, IBlockState blockState) {
    // Do nothing
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return The bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
    return new AxisAlignedBB(0.125000, 0.062500, 0.937500, 0.875000, 0.937500, 1.000000);
  }
}
