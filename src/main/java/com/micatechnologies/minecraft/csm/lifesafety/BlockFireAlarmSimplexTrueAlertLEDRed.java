package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockFireAlarmSimplexTrueAlertLEDRed extends AbstractBlockFireAlarmSounder
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
  public float[] getStrobeLensFrom() {
    return new float[]{6.5f, 6f, 14f};
  }

  @Override
  public float[] getStrobeLensTo() {
    return new float[]{9.5f, 8.5f, 15f};
  }


  @Override
  public String getBlockRegistryName() {
    return "firealarmsimplextruealertledred";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:stahorn";
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
        return new AxisAlignedBB(0.250000, 0.250000, 0.875000, 0.750000, 1.000000, 1.000000);
    }
}
