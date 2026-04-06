package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Factory for creating {@link AbstractBlockFireAlarmSounderVoiceEvac} blocks with strobe that only
 * differ in registry name, bounding box, and strobe lens coordinates.
 *
 * @since 2026.4
 */
public class BlockFireAlarmVoiceEvacStrobeFactory extends AbstractBlockFireAlarmSounderVoiceEvac
    implements ICsmTileEntityProvider, IStrobeBlock {

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final float[] strobeLensFrom;
  private final float[] strobeLensTo;

  public BlockFireAlarmVoiceEvacStrobeFactory(String registryName, AxisAlignedBB boundingBox,
      float[] strobeLensFrom, float[] strobeLensTo) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.strobeLensFrom = strobeLensFrom;
    this.strobeLensTo = strobeLensTo;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }

  @Override
  public float[] getStrobeLensFrom() {
    return strobeLensFrom;
  }

  @Override
  public float[] getStrobeLensTo() {
    return strobeLensTo;
  }

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
}
