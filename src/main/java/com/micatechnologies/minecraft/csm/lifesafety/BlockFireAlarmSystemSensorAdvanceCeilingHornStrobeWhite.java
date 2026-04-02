package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockFireAlarmSystemSensorAdvanceCeilingHornStrobeWhite extends
    AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider, IStrobeBlock {

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFireAlarmStrobe.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityfirealarmstrobe";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityFireAlarmStrobe();
  }

  @Override
  public String getBlockRegistryName() {
    return "firealarmsystemsensoradvanceceilinghornstrobewhite";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:spectralert";
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
        return new AxisAlignedBB(0.062500, 0.062500, 0.937500, 0.937500, 0.937500, 1.000000);
    }
}
