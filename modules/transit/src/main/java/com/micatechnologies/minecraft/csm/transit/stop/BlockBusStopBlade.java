package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The stop-name blade: a bus stop's name on a sign in its agency's livery, set on top of the
 * full-size stop pole ({@link BlockBusStopPoleFlag} places one), against a wall, or hung from a
 * canopy or a shelter. Lit and double-sided.
 *
 * <p>It is Core's wayfinding panel ({@code assets/docs/WAYFINDING_PANELS.md}): cells side by
 * side and stacked are one sign up to 16 x 6, baked into the chunk mesh with no renderer, edited
 * with an empty-handed click (or by NBT, {@code {l1:"RIVERSIDE"}}). The presets are the agencies'
 * liveries ({@link BusStopSignPresets#BLADE}); the editor's custom colours are a custom agency.
 * Metadata is the facing (S 0, W 1, N 2, E 3), towards the reader of its front.</p>
 *
 * @since 2026.11
 */
public class BlockBusStopBlade extends AbstractBlockWayfindingPanel {

  /**
   * Constructs the blade.
   *
   * @param registryName its registry name
   */
  public BlockBusStopBlade(String registryName) {
    super(registryName);
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return BusStopSignPresets.BLADE;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBusStopBlade.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitybusstopblade";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityBusStopBlade();
  }
}
