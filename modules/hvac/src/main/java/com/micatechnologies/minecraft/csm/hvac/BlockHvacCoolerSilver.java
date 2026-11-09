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
 * The cabinet cooler, silver finish: an outdoor condensing unit (heat pump) one block
 * square, the same model as the cabinet heater; a {@link TileEntityHvacCooler}.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class BlockHvacCoolerSilver extends AbstractBlockHvacUnit {

  public BlockHvacCoolerSilver() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "hvac_cooler_silver";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacCooler.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvaccooler";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacCooler();
  }
}
