package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The section number sign ({@code stadium_section_sign}): the big painted section number
 * over a vomitory, 101 by default, navy, flat on the wall.
 *
 * <p>It is Core's wayfinding panel ({@code assets/docs/WAYFINDING_PANELS.md}): cells side by
 * side and stacked are one sign up to 16 x 6, baked into the chunk mesh with no renderer, its
 * text and look set in the editor (an empty-handed click) or by NBT, so a generator writes it with
 * {@code /setblock} and {@code /blockdata} as it writes the airport, hospital and generic panels:
 * {@code {l1:"114",s:"neutral",m:"wall"}}. Metadata is the facing (S 0, W 1, N 2, E 3), towards
 * the reader.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumSectionSign extends AbstractBlockWayfindingPanel {

  /**
   * Constructs the sign.
   *
   * @param registryName its registry name
   */
  public BlockStadiumSectionSign(String registryName) {
    super(registryName);
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return VenueSignPresets.SECTION;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityStadiumSectionSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitystadiumsectionsign";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityStadiumSectionSign();
  }
}
