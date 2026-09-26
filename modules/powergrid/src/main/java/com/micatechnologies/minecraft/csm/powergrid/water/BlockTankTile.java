package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A water tower's tank: a multi-leg tank with its balcony, or a pedestal tank. Its item places
 * the whole tank, centred on the block it is placed on -- the riser's top, or snapped onto a
 * pedestal column's middle whichever of its cells was clicked -- and every tile of it is this
 * block but the ones carrying the name band ({@link BlockTankBand}), which go on the two sides
 * facing toward and away from the player who placed it.
 *
 * <p>{@link #TILE} is the tile's place in the grid, {@code (layer * grid + row) * grid +
 * column}, actual state only: the tile counts its neighbours for it ({@link #gridPos}). A click
 * on the tank cycles the name on its band.</p>
 *
 * @since 2026.9
 */
public class BlockTankTile extends AbstractTankTile {

  private PropertyInteger tile;

  public BlockTankTile(String registryName, TankShape shape) {
    super(registryName, shape);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    tile = PropertyInteger.create("tile", 0, pendingShape().getTileCount() - 1);
    return new CsmBlockStateContainer(this, tile);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int[] g = gridPos(world, pos);
    int n = shape.getGrid();
    return state.withProperty(tile, (g[2] * n + g[1]) * n + g[0]);
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

  /** Set on a pedestal column: the tank centres on the column, whichever cell was clicked. */
  @Override
  protected BlockPos axisFor(World world, BlockPos pos) {
    BlockPos below = pos.down();
    IBlockState under = world.getBlockState(below);
    if (under.getBlock() instanceof BlockPedestalSection) {
      return below.up();
    }
    if (under.getBlock() instanceof BlockTankPart) {
      BlockPos root = TankUnits.findRoot(world, below);
      if (root != null && world.getBlockState(root).getBlock() instanceof BlockPedestalSection) {
        return root.up();
      }
    }
    return pos;
  }

  /** The band's tiles are the band block; they go on the rows facing the placer and behind. */
  @Override
  protected IBlockState rootStateFor(int gi, int gj, int gk, EnumFacing placerFacing) {
    if (!shape.hasBand() || gk != shape.getBandLayer()) {
      return getDefaultState();
    }
    int n = shape.getGrid();
    boolean onBand = placerFacing.getAxis() == EnumFacing.Axis.Z
        ? (gj == 0 || gj == n - 1) && shape.bandSlotOf(gi) >= 0
        : (gi == 0 || gi == n - 1) && shape.bandSlotOf(gj) >= 0;
    if (!onBand) {
      return getDefaultState();
    }
    return CsmRegistry.getBlock(getBlockRegistryName() + "_band").getDefaultState();
  }

  /** A click on the tank cycles the name on its band; sneaking, backwards. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    return cycleBand(this, world, pos, player, hand);
  }

  /**
   * Cycles the name on every band tile of the tank a tile belongs to, and tells the player the
   * name it now reads.
   */
  static boolean cycleBand(AbstractTankTile tile, World world, BlockPos pos, EntityPlayer player,
      EnumHand hand) {
    TankShape shape = tile.getShape();
    if (!shape.hasBand() || hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    BlockPos origin = tile.origin(world, pos);
    int n = shape.getGrid();
    int gk = shape.getBandLayer();
    int names = TankShapes.BAND_NAMES.length;
    int next = -1;
    for (int gj = 0; gj < n; gj++) {
      for (int gi = 0; gi < n; gi++) {
        BlockPos at = origin.add(3 * gi + 1, 3 * gk + 1, 3 * gj + 1);
        IBlockState st = world.getBlockState(at);
        if (!(st.getBlock() instanceof BlockTankBand) || !tile.sameShape(st)) {
          continue;
        }
        if (next < 0) {
          int step = player.isSneaking() ? names - 1 : 1;
          next = (st.getValue(BlockTankBand.NAME) + step) % names;
        }
        world.setBlockState(at, st.withProperty(BlockTankBand.NAME, next), 3);
      }
    }
    if (next < 0) {
      return false;
    }
    String name = TankShapes.BAND_NAMES[next];
    player.sendStatusMessage(name.isEmpty()
        ? new TextComponentTranslation("csm.utilities.water_tower.band_blank")
        : new TextComponentTranslation("csm.utilities.water_tower.band", name), true);
    return true;
  }
}
