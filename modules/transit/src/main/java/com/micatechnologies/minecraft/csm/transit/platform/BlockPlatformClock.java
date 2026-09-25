package com.micatechnologies.minecraft.csm.transit.platform;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * A double-faced platform clock hung from the ceiling, its hands telling the world's time on
 * both dials. The drum and dials are its baked model; {@link TileEntityPlatformClockRenderer}
 * draws the two hands on each dial, four quads a frame.
 *
 * @since 2026.9
 */
public class BlockPlatformClock extends BlockPlatformFixture implements ICsmTileEntityProvider {

  /**
   * Constructs a clock.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockPlatformClock(String registryName, double[] box) {
    super(registryName, box);
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityPlatformClock.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityplatformclock";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityPlatformClock();
  }
}
