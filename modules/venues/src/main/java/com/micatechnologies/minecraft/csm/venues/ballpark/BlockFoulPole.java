package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A foul pole ({@code foul_pole}): a yellow pole stacked as tall as wanted at the end of a foul
 * line, standing on a concrete footing where nothing of the pole is below ({@code base}) and
 * capped where nothing is above ({@code cap}), both actual state. A block with the {@code wing}
 * bit carries the yellow mesh screen, a block and a half long, on the fair side: set it on the
 * blocks above the wall.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the wing reaches out:
 * towards fair territory (west for a right field pole beyond a wall facing west); bit 2 the
 * wing. Placed by hand the wing reaches towards the placer, if placed while sneaking.</p>
 *
 * @since 2026.10
 */
public class BlockFoulPole extends BlockVenue {

  /** The mesh screen on the fair side. Stored, bit 2. */
  public static final PropertyBool WING = PropertyBool.create("wing");
  /** Nothing of the pole below: the footing. Actual. */
  public static final PropertyBool BASE = PropertyBool.create("base");
  /** Nothing of the pole above: the cap. Actual. */
  public static final PropertyBool CAP = PropertyBool.create("cap");

  private static final AxisAlignedBB POLE = new AxisAlignedBB(5 / 16.0, 0, 5 / 16.0,
      11 / 16.0, 1, 11 / 16.0);

  /**
   * Constructs the pole.
   *
   * @param registryName its registry name
   */
  public BlockFoulPole(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 3.0F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(WING, false).withProperty(BASE, true).withProperty(CAP, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, WING, BASE, CAP);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(WING, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(WING) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(WING, placer.isSneaking());
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(BASE, world.getBlockState(pos.down()).getBlock() != this)
        .withProperty(CAP, world.getBlockState(pos.up()).getBlock() != this);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return POLE;
  }
}
