package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A stadium light tower's shaft, stacked as tall as wanted with a {@link BlockLightHead} on top:
 * the galvanized monopole ({@code stadium_light_pole}, a round shaft with step bolts up its back)
 * or the lattice mast ({@code stadium_light_lattice}, four corner angles laced on every face).
 * The bottom cell (nothing of the same shaft under it, {@code base}, actual state) stands on a
 * concrete pier with a base plate.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the head's: the monopole's step bolts are
 * on its back.</p>
 *
 * @since 2026.10
 */
public class BlockLightPole extends BlockVenue {

  /** Nothing of the same shaft below: the pier. Actual. */
  public static final PropertyBool BASE = PropertyBool.create("base");

  private static final AxisAlignedBB MONOPOLE = new AxisAlignedBB(5 / 16.0, 0, 5 / 16.0,
      11 / 16.0, 1, 11 / 16.0);
  private static final AxisAlignedBB LATTICE = new AxisAlignedBB(1 / 16.0, 0, 1 / 16.0,
      15 / 16.0, 1, 15 / 16.0);
  private static final AxisAlignedBB PIER = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

  private final boolean lattice;

  /**
   * Constructs a shaft.
   *
   * @param registryName its registry name
   * @param lattice      the lattice mast rather than the monopole
   */
  public BlockLightPole(String registryName, boolean lattice) {
    super(registryName, Material.IRON, SoundType.METAL, 3.0F);
    this.lattice = lattice;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(BASE, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, BASE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(BASE, world.getBlockState(pos.down()).getBlock() != this);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (state.getValue(BASE) && !lattice) {
      return PIER;
    }
    return lattice ? LATTICE : MONOPOLE;
  }
}
