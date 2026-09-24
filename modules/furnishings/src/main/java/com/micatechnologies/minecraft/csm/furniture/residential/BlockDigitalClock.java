package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;

/**
 * A bedside digital clock showing the world's time, HH:MM on the 24-hour clock, in lit red
 * digits: {@code TileEntityResidentialClockRenderer} draws the time on its display, and the rest
 * is its baked model. It stands on whatever is under it, as the counter pieces do, so on a
 * nightstand it stands on the top.
 *
 * <p>Where the display is in the model is the generator's ({@code gen_furniture_living.py},
 * {@code DIGITAL_DISPLAY}); the renderer's constants must match it.</p>
 *
 * @since 2026.9
 */
public class BlockDigitalClock extends BlockCounterPiece implements ICsmTileEntityProvider {

  /**
   * Constructs a digital clock.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   */
  public BlockDigitalClock(String registryName, int[] box) {
    super(registryName, box, Material.WOOD, SoundType.STONE, BlockRenderLayer.SOLID);
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
}
