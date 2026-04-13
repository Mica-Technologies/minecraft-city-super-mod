package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockHvacThermostat extends AbstractBlockRotatableNSEWUD
    implements ICsmTileEntityProvider {

  public BlockHvacThermostat() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "hvac_thermostat";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.1875, 0.1875, 0.9, 0.8125, 0.625, 1.0);
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
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return true;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public boolean canProvidePower(IBlockState state) {
    return true;
  }

  @Override
  public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos,
      EnumFacing side) {
    TileEntity te = blockAccess.getTileEntity(pos);
    if (te instanceof TileEntityHvacThermostat) {
      return ((TileEntityHvacThermostat) te).isCalling() ? 15 : 0;
    }
    return 0;
  }

  @Override
  public int getStrongPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos,
      EnumFacing side) {
    TileEntity te = blockAccess.getTileEntity(pos);
    if (te instanceof TileEntityHvacThermostat) {
      return ((TileEntityHvacThermostat) te).isCalling() ? 15 : 0;
    }
    return 0;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
      float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    // Don't open GUI when using the HVAC linker tool — let the item handle the click
    if (playerIn.getHeldItem(hand).getItem() instanceof ItemHvacLinker) {
      return false;
    }
    playerIn.openGui(Csm.instance, 6, worldIn, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacThermostat.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvacthermostat";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacThermostat();
  }
}
