package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A ground storage tank, a three-block layer at a time: placed on the ground it is a whole
 * tank, and placed on top of a layer of the same size it carries that tank on up, centred on
 * it whichever of its cells was clicked. Each layer draws the shell plate; the lowest also the
 * concrete ringwall ({@link #BELOW} false) and the highest the roof, its vent and its handrail
 * ({@link #ABOVE} false), both actual state, so a tank re-forms as layers are added or taken
 * away. Breaking a cell takes its layer.
 *
 * <p>{@link #TILE} is the tile's place in its layer, {@code row * grid + column}.</p>
 *
 * @since 2026.9
 */
public class BlockGroundTank extends AbstractTankTile {

  /** A layer of the same tank is below this one. */
  public static final PropertyBool BELOW = PropertyBool.create("below");
  /** A layer of the same tank is above this one. */
  public static final PropertyBool ABOVE = PropertyBool.create("above");

  private PropertyInteger tile;

  public BlockGroundTank(String registryName, TankShape shape) {
    super(registryName, shape);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    int n = pendingShape().getGrid();
    tile = PropertyInteger.create("tile", 0, n * n - 1);
    return new CsmBlockStateContainer(this, tile, BELOW, ABOVE);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int[] g = gridPos(world, pos);
    return state.withProperty(tile, g[1] * shape.getGrid() + g[0])
        .withProperty(BELOW, sameShape(world.getBlockState(pos.down(3))))
        .withProperty(ABOVE, sameShape(world.getBlockState(pos.up(3))));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  /** Placed on a layer of the same tank: the new layer goes square on top of it. */
  @Override
  protected BlockPos axisFor(World world, BlockPos pos) {
    BlockPos below = pos.down();
    IBlockState under = world.getBlockState(below);
    BlockPos root = null;
    if (sameShape(under)) {
      root = below;
    } else if (under.getBlock() instanceof BlockTankPart) {
      root = TankUnits.findRoot(world, below);
    }
    if (root == null || !sameShape(world.getBlockState(root))) {
      return pos;
    }
    int half = shape.getHalf();
    return origin(world, root).add(half, 3, half);
  }
}
