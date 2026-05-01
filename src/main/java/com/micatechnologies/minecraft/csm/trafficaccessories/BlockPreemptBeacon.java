package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractPoweredBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockPreemptBeacon extends AbstractPoweredBlockRotatableNSEWUD
    implements ICsmTileEntityProvider, ITrafficBeaconBlock, ICsmNoSnowAccumulation {

  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(0.000000, -0.421875, 0.406250, 0.562500, 0.156250, 0.593750);

  public BlockPreemptBeacon() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "tlpreemptbeacon";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficBeacon();
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficBeacon.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitytrafficbeaconpreempt";
  }

  @Override
  public float[] getBeaconLensFrom() {
    return new float[]{6.0f, -1.5f, 6.5f};
  }

  @Override
  public float[] getBeaconLensTo() {
    return new float[]{9.0f, 2.5f, 9.5f};
  }

  @Override
  public float getBeaconColorR() {
    return 1.0f;
  }

  @Override
  public float getBeaconColorG() {
    return 0.15f;
  }

  @Override
  public float getBeaconColorB() {
    return 0.1f;
  }
}
