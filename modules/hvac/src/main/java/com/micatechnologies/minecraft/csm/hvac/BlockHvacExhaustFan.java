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
 * A rooftop exhaust fan on its curb (#314): the upblast fan of a kitchen hood and the mushroom
 * (downblast) fan of a toilet exhaust, one block each, and the base of the
 * {@link BlockHvacLabExhaustFan}. One class, constructed by registry name.
 *
 * <p>A fan linked to a primary thermostat (the linker, as a heater is linked) and powered by
 * redstone or Forge Energy draws air out of the rooms its system's exhaust grilles
 * ({@link BlockHvacExhaustGrille}) are in, up to {@link #getCapacityCfm()} shared between them;
 * see {@link HvacAirBalance}. Unlinked it is decorative and costs nothing: it neither ticks nor
 * anchors a room. Models from {@code dev-env-utils/scripts/gen_hvac_exhaust.py}.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class BlockHvacExhaustFan extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  /** The upblast fan: curb, housing and damper, 14 px tall. */
  private static final AxisAlignedBB UPBLAST_BOX = new AxisAlignedBB(0.03125, 0.0, 0.0, 0.96875,
      0.875, 0.96875);

  private final String registryName;

  public BlockHvacExhaustFan(String registryName) {
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

  /**
   * Air the fan moves at full speed, cubic feet a minute: a high-plume lab fan 10,000, a kitchen
   * upblast fan 2,500, a toilet exhaust mushroom fan 1,200.
   */
  public int getCapacityCfm() {
    String name = getBlockRegistryName();
    if (name.contains("lab")) {
      return 10000;
    }
    return name.contains("upblast") ? 2500 : 1200;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return UPBLAST_BOX;
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
    return TileEntityHvacExhaustFan.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvacexhaustfan";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacExhaustFan();
  }
}
