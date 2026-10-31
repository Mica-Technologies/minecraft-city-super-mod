package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.wayfinding.AbstractBlockWayfindingPanel;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingPreset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The vomitory portal sign ({@code stadium_portal_sign}): the lit sign over a vomitory saying
 * which sections it leads to, SECTIONS 101 - 104 with an arrow up by default, on the wall
 * or hung.
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
public class BlockStadiumPortalSign extends AbstractBlockWayfindingPanel {

  /**
   * Constructs the sign.
   *
   * @param registryName its registry name
   */
  public BlockStadiumPortalSign(String registryName) {
    super(registryName);
  }

  @Override
  public WayfindingPreset[] getPresets() {
    return VenueSignPresets.PORTAL;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityStadiumPortalSign.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitystadiumportalsign";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityStadiumPortalSign();
  }
}
