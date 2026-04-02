package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkAccessory;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockControllableCrosswalkTweeter2
    extends AbstractBlockControllableCrosswalkAccessory {

  final int lenOfTweetSound = 40;

  public BlockControllableCrosswalkTweeter2() {
    super(Material.ROCK);
  }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return 0;
  }

  private static final float TWEETER_HEARING_RANGE = 16.0f;

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    int color = state.getValue(COLOR);
    if (color == 2 && !world.isRemote) {
      String channel = "tweeter_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
      CsmNetwork.sendToAll(APSSoundPacket.start(
          channel, "csm:crosswalk_cookoo_2", TWEETER_HEARING_RANGE, false, pos));
    }
    world.scheduleUpdate(pos, this, this.tickRate(world));
  }

  @Override
  public int tickRate(World p_tickRate_1_) {
    return lenOfTweetSound;
  }

  @Override
  public void onBlockAdded(World p_onBlockAdded_1_, BlockPos p_onBlockAdded_2_,
      IBlockState p_onBlockAdded_3_) {
    p_onBlockAdded_1_.scheduleUpdate(p_onBlockAdded_2_, this, this.tickRate(p_onBlockAdded_1_));
    super.onBlockAdded(p_onBlockAdded_1_, p_onBlockAdded_2_, p_onBlockAdded_3_);
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
        return new AxisAlignedBB(0.375000, 0.812500, 0.425000, 0.625000, 1.000000, 0.568750);
    }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "controllablecrosswalktweeter2";
  }

}
