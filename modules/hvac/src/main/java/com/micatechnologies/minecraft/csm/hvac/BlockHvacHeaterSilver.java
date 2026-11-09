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
 * The cabinet heater, silver finish: an outdoor condensing unit (heat pump) one block
 * square, the same model as the cabinet cooler; a {@link TileEntityHvacHeater}.
 *
 * @author Mica Technologies
 * @since 2026.4
 */
public class BlockHvacHeaterSilver extends AbstractBlockHvacUnit {

  public BlockHvacHeaterSilver() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "hvac_heater_silver";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacHeater.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvacheater";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacHeater();
  }
}
