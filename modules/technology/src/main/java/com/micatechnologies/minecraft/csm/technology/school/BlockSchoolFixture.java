package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A piece of the school set that hangs on a wall (or from a ceiling) facing one of four ways:
 * the base of the clocks, the clock/speaker panels, the bell controller and the hallway bell. It
 * faces the player who places it, so its wall is behind it, and its box is given facing north,
 * in sixteenths, and turned with it. Its models come from
 * {@code dev-env-utils/scripts/gen_technology_school.py}.
 *
 * @since 2026.10
 */
public class BlockSchoolFixture extends AbstractBlockRotatableNSEW {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * Constructs a fixture.
   *
   * @param registryName its registry name
   * @param box          its box facing north: x0, y0, z0, x1, y1, z1 in sixteenths
   */
  public BlockSchoolFixture(String registryName, double[] box) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.0F, 6.0F, 0F, 0);
    this.registryName = registryName;
    this.box = toBox(box);
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  /**
   * A box given in sixteenths as {x0, y0, z0, x1, y1, z1}.
   *
   * @param b the box in sixteenths
   *
   * @return the box in blocks
   */
  protected static AxisAlignedBB toBox(double[] b) {
    return new AxisAlignedBB(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0, b[4] / 16.0,
        b[5] / 16.0);
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
    return false;
  }

  /** Mipped cutout: a dial's numerals and a grille's holes stay steady at a distance. */
  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
