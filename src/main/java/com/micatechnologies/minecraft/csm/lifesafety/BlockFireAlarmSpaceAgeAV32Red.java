package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockFireAlarmSpaceAgeAV32Red extends AbstractBlockFireAlarmSounder
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
    return new float[]{4.8f, 2.1f, 13.5f};
  }

  @Override
  public float[] getStrobeLensTo() {
    return new float[]{11.4f, 5.4f, 14f};
  }
  @Override
  public boolean isRedSlowToggleStrobe() {
    return true;
  }


  @Override
  public String getBlockRegistryName() {
    return "firealarmspaceageav32red";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return "csm:sae_marchtime";
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
        return new AxisAlignedBB(0.125000, 0.062500, 0.875000, 0.875000, 0.937500, 1.000000);
    }
}
