package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The concession sign ({@code concession_sign}): the lit header band over a stand (HOT DOGS,
 * BEER, PIZZA, SNACKS, GRILL, SWEETS, DRINKS, each in its own colours) and, with four lines typed
 * in, a price list.
 *
 * <p>It is Core's wayfinding panel ({@code assets/docs/WAYFINDING_PANELS.md}): cells side by
 * side and stacked are one sign up to 16 x 6, baked into the chunk mesh with no renderer, set in
 * the editor (an empty-handed click; the presets are the header legends) or by NBT, so a generator
 * writes it with {@code /setblock} and {@code /blockdata}:
 * {@code {l1:"PIZZA",s:"laboratory",m:"wall",al:"c"}}. Metadata is the facing (S 0, W 1, N 2,
 * E 3), towards the reader.</p>
 *
 * @since 2026.10
 */
public class BlockConcessionSign extends AbstractBlockWayfindingPanel {

  /**
   * Constructs the sign.
   *
   * @param registryName its registry name
   */
  public BlockConcessionSign(String registryName) {
    super(registryName);
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return ConcourseSignPresets.CONCESSION;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityConcessionSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityconcessionsign";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityConcessionSign();
  }
}
