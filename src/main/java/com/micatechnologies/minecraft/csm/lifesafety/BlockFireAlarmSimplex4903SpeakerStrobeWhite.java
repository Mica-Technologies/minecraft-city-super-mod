package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockFireAlarmSimplex4903SpeakerStrobeWhite extends
    AbstractBlockFireAlarmSounderVoiceEvac
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
    return new float[]{1.3f, 7.3f, 11f};
  }

  @Override
  public float[] getStrobeLensTo() {
    return new float[]{4.7f, 15.8f, 14f};
  }

  @Override
  public String getBlockRegistryName() {
    return "firealarmsimplex4903speakerstrobewhite";
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
        return new AxisAlignedBB(0.062500, 0.437500, 0.687500, 0.937500, 1.000000, 1.000000);
    }
}
