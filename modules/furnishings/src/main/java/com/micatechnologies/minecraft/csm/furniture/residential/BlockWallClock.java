package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;

/**
 * A round wall clock whose hands tell the world's time: the case and dial are its baked model
 * and {@code TileEntityResidentialClockRenderer} draws the two hands over the dial, two quads a
 * frame. It hangs on the wall behind it, in the middle of its block.
 *
 * <p>Where the dial is in the model is the generator's ({@code gen_furniture_living.py},
 * {@code WALL_CLOCK_DIAL}); the renderer's constants must match it.</p>
 *
 * @since 2026.9
 */
public class BlockWallClock extends BlockResidentialFurniture implements ICsmTileEntityProvider {

  /**
   * Constructs a wall clock.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockWallClock(String registryName, int[] box) {
    super(registryName, box, false);
  }

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return true;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityResidentialClock.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityresidentialclock";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityResidentialClock();
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
