package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Security trays: a stack of them, or one with a traveller's things in it. On the floor they
 * stand on the floor; set on a roller conveyor or divesting table ({@link BlockSecurityLine}) they drop onto its
 * top, four sixteenths down into the lane's block ({@link #LOW}, actual state), which is where a
 * player puts them.
 *
 * @since 2026.9
 */
public class BlockSecurityTray extends BlockPlatformFixture {

  /** The block below is a security lane piece: drawn down on its top. */
  public static final PropertyBool LOW = PropertyBool.create("low");

  private static final AxisAlignedBB STANDING = new AxisAlignedBB(3 / 16.0, 0, 5 / 16.0,
      13 / 16.0, 4 / 16.0, 11 / 16.0);
  private static final AxisAlignedBB DROPPED = new AxisAlignedBB(3 / 16.0, -4 / 16.0, 5 / 16.0,
      13 / 16.0, 0, 11 / 16.0);

  /**
   * Constructs a tray block.
   *
   * @param registryName its registry name
   */
  public BlockSecurityTray(String registryName) {
    super(registryName, new double[]{3, 0, 5, 13, 4, 11});
    setDefaultState(getDefaultState().withProperty(LOW, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LOW);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    Block below = world.getBlockState(pos.down()).getBlock();
    return state.withProperty(LOW,
        below instanceof BlockSecurityLine && ((BlockSecurityLine) below).hasOpenTop());
  }

  /**
   * The tray's box, below its block when it has dropped onto a lane: a player looking down at it
   * aims through the tray's own block first, whose box the ray is tested against whole.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(LOW) ? DROPPED : STANDING;
  }
}
