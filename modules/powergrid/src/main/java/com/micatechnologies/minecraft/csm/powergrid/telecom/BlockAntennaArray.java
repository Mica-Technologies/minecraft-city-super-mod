package com.micatechnologies.minecraft.csm.powergrid.telecom;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityFixture;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockTowerColumn;
import com.micatechnologies.minecraft.csm.powergrid.water.IColumnJoint;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A monopole's antenna array: a length of the pole with a three-sector frame round it, three panel
 * antennas and their radios a sector, the first sector facing the player who placed it. It is
 * placed in the pole's stack like a section (the {@link BlockTowerColumn} sections below and above
 * it carry on through it), so a tower is built to any height and can carry a second carrier's
 * array lower down; where nothing of the pole is above it ({@link #TOP}, actual state) the pole is
 * capped over the frame with its lightning rod.
 *
 * <p>The frame reaches a block past the array's cell each way, drawn by the array's own model; it
 * does not collide, and the array's box is the pole's. Eight states, no tile entity.</p>
 *
 * @since 2026.9
 */
public class BlockAntennaArray extends BlockUtilityFixture implements IColumnJoint {

  /** Nothing of the pole is above: the cap and the lightning rod. */
  public static final PropertyBool TOP = PropertyBool.create("top");

  /**
   * @param registryName its registry name
   * @param box          its box facing north (the pole's), in sixteenths
   */
  public BlockAntennaArray(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(TOP, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, TOP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    Block above = world.getBlockState(pos.up()).getBlock();
    return super.getActualState(state, world, pos).withProperty(TOP,
        !(above instanceof BlockTowerColumn) && !(above instanceof BlockAntennaArray));
  }

  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}
