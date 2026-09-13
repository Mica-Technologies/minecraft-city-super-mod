package com.micatechnologies.minecraft.csm.trafficaccessories;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The crossing's flasher mast head: the crossbuck over a crossarm carrying the twin red lamps,
 * with the bell beneath. Goes on top of any pole. The lamps alternate at the MUTCD's 35-65
 * flashes a minute while powered -- the wig-wag lives in an animated lens texture, exactly as
 * the RRFB's does, so every crossing in the world flashes in step -- and the tile entity rings
 * the bell once a second.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class BlockRailroadCrossingFlasher extends AbstractBlockRailroadCrossing {

  @Override
  public String getBlockRegistryName() {
    return "railroad_crossing_flasher";
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityRailroadCrossingFlasher.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityrailroadcrossingflasher";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityRailroadCrossingFlasher();
  }

  /**
   * The crossarm and lamps, the full width of the block and a fifth of it deep, centred on the
   * pole. The crossbuck above is left out of the box, as a sign's overhang is.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.0625D, 0.0D, 0.40625D, 0.9375D, 1.0D, 0.59375D);
  }
}
