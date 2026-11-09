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
 * A PTAC (packaged terminal air conditioner) with heat: the through-the-wall unit under a hotel
 * room's window, on the floor against the outside wall, its back to the wall and its front (the
 * intake grille, the discharge grille on top, the control door top right) to the room. Placed
 * against a wall, it turns its back to the wall the placer faces. A combination unit
 * ({@link TileEntityHvacPtac}): heats below 70°F and cools above 74°F on its own, or does what
 * its thermostat's system asks. Off-white or almond; one class, constructed by registry name.
 * Model from {@code dev-env-utils/scripts/gen_hvac_units.py}; the outside of the wall takes a
 * {@link BlockHvacPtacWallGrille}.
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class BlockHvacPtac extends AbstractBlockRotatableNSEW implements ICsmTileEntityProvider {

  /** The cabinet, 15.5 px wide, 8.25 tall with its control door, 6.25 deep, facing north. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.015625, 0.0, 0.609375, 0.984375,
      0.515625, 1.0);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  public BlockHvacPtac(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
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
    return TileEntityHvacPtac.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvacptac";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacPtac();
  }
}
