package com.micatechnologies.minecraft.csm.hvac;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The rooftop cooler, black finish: a packaged rooftop unit drawn and collided over
 * 2 1/8 x 2 x 3 blocks from one, vents only, up to 100 blocks away; a
 * {@link TileEntityHvacRtuCooler}.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class BlockHvacRtuCoolerBlack extends AbstractBlockHvacUnit {

  public BlockHvacRtuCoolerBlack() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "hvac_rtu_cooler_black";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return RTU_BOX;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacRtuCooler.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvacrtucooler";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacRtuCooler();
  }
}
