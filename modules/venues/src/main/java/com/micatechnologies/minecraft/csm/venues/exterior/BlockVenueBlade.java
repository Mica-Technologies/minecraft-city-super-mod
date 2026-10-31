package com.micatechnologies.minecraft.csm.venues.exterior;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * An arena wayfinding blade: the gate blade ({@code venue_gate_blade}, GATE over a big letter)
 * or the section blade ({@code venue_section_blade}, SECTIONS 101 - 112 with an arrow). Lit,
 * double-sided and hung: square to a wall from a {@link BlockBladeBracket} above it (a projecting
 * blade), or from the ceiling over a concourse (an overhead one).
 *
 * <p>It is Core's wayfinding panel ({@code assets/docs/WAYFINDING_PANELS.md}): cells side by
 * side and stacked are one sign up to 16 x 6, baked into the chunk mesh with no renderer, its
 * rods reaching up to the first block above (the bracket's arm), set in the editor (an
 * empty-handed click) or by NBT, so a generator writes it as it writes every wayfinding panel:
 * {@code {l1:"GATE",l2:"C",ts1:0.5f}}. Metadata is the facing (S 0, W 1, N 2, E 3), towards the
 * reader of its front; a projecting blade faces along the wall.</p>
 *
 * @since 2026.10
 */
public class BlockVenueBlade extends AbstractBlockWayfindingPanel {

  private final boolean gate;

  /**
   * Constructs a blade.
   *
   * @param registryName its registry name
   * @param gate         the gate blade rather than the section blade
   */
  public BlockVenueBlade(String registryName, boolean gate) {
    super(registryName);
    this.gate = gate;
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return gate ? ExteriorSignPresets.GATE : ExteriorSignPresets.SECTION;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return gate ? TileEntityGateBlade.class : TileEntitySectionBlade.class;
  }

  @Override
  public String getTileEntityName() {
    return gate ? "tileentityvenuegateblade" : "tileentityvenuesectionblade";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return gate ? new TileEntityGateBlade() : new TileEntitySectionBlade();
  }
}
