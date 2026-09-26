package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A water tower tank's tile that carries the town's name: its share of the tank with the band
 * zone cut out, and the band in that zone reading {@link #NAME} (stored, the metadata). The
 * placement puts these on the two sides facing toward and away from the player; a click on any
 * cell of the tank cycles every band tile's name together ({@link BlockTankTile#cycleBand}).
 *
 * <p>{@link #SLOT} is actual state: which side of the tank the tile is on and which of the
 * band's tiles along that side, {@code side * slots + slot}, sides north, east, south, west. The
 * blockstate draws the north side's models turned to the tile's side. Hidden: the tank's own
 * item places these.</p>
 *
 * @since 2026.9
 */
public class BlockTankBand extends AbstractTankTile {

  /** The name the band reads, an index into {@link TankShapes#BAND_NAMES}. */
  public static final PropertyInteger NAME = PropertyInteger.create("name", 0,
      TankShapes.BAND_NAMES.length - 1);

  private PropertyInteger slot;

  public BlockTankBand(String registryName, TankShape shape) {
    super(registryName, shape);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    slot = PropertyInteger.create("slot", 0, 4 * pendingShape().getBandSlotCount() - 1);
    return new CsmBlockStateContainer(this, slot, NAME);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int[] g = gridPos(world, pos);
    int n = shape.getGrid();
    int side;
    int column;
    if (g[1] == 0) {
      side = 0;
      column = g[0];
    } else if (g[0] == n - 1) {
      side = 1;
      column = g[1];
    } else if (g[1] == n - 1) {
      side = 2;
      column = n - 1 - g[0];
    } else if (g[0] == 0) {
      side = 3;
      column = n - 1 - g[1];
    } else {
      side = 0;
      column = g[0];
    }
    int s = Math.max(0, shape.bandSlotOf(column));
    return state.withProperty(slot, side * shape.getBandSlotCount() + s);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(NAME, Math.min(meta, TankShapes.BAND_NAMES.length - 1));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(NAME);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    return BlockTankTile.cycleBand(this, world, pos, player, hand);
  }
}
