package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockFireAlarmWheelockRSSStrobe extends AbstractBlockFireAlarmSounder
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
    return new float[]{3.4f, 9.7f, 12f};
  }

  @Override
  public float[] getStrobeLensTo() {
    return new float[]{12.9f, 12.2f, 14f};
  }

  @Override
  public String getBlockRegistryName() {
    return "rssstrobe";
  }

  @Override
  public String getSoundResourceName(IBlockState blockState) {
    return null;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.187500, 0.375000, 0.750000, 0.812500, 1.000000, 1.000000);
  }
}
