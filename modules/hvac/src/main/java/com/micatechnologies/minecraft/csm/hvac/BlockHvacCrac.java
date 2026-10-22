package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
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

/**
 * A floor-standing CRAC or CRAH unit for a data hall (#305): a cabinet two blocks tall drawn and
 * collided from one block, as the fare vending machine is, its front (the control panel and
 * filters) facing the placer. Downflow units take air in at the top and blow it under a raised
 * floor; upflow units the reverse, with a discharge plenum on top. The difference is the look:
 * both are a {@link TileEntityHvacCrac}. One class, constructed by registry name.
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class BlockHvacCrac extends AbstractBlockRotatableNSEW implements ICsmTileEntityProvider {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  /** The cabinet, 30 px tall and 15 deep, facing north. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.03125, 0.0, 0.0625, 0.96875,
      1.875, 1.0);

  private final String registryName;

  public BlockHvacCrac(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3F, 15F, 0F, 0);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  /** Needs the cell above clear for its upper half. */
  @Override
  public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
    return super.canPlaceBlockAt(worldIn, pos)
        && worldIn.getBlockState(pos.up()).getBlock().isReplaceable(worldIn, pos.up());
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

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacCrac.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvaccrac";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacCrac();
  }
}
