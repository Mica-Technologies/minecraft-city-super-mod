package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockTrafficSign;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * A real-time arrival display clamped to a road sign post: a small amber LED panel listing the
 * next buses, three lines at a time ("12 DOWNTOWN 3 MIN"), turning to the next page every few
 * seconds. It lists the routes on the flag of its own stop -- the nearest bus stop flag up or
 * down the post -- or two stock routes on a post without one. The arrivals are made up but
 * steady: every player sees the same countdown, one minute a minute (see
 * {@link TileEntityBusArrivalDisplayRenderer}).
 *
 * <p>It is a road sign like the flag: a length of sign post with the display clamped to its
 * front, stacked in the post's column, facing the way the sign below it faces, and set back or
 * put back to back by the sign system like any other. The housing and the dark screen are
 * baked; the text is the renderer's. The screen gives a little light.</p>
 *
 * @since 2026.9
 */
public class BlockBusArrivalDisplay extends BlockTrafficSign implements ICsmTileEntityProvider {

  /**
   * The screen's middle and size in the unshifted model, in sixteenths: it runs x 1.6 to 14.4
   * and y 7.0 to 11.4 on the housing's front at z -0.9, as {@code gen_transit_stops.py} draws it
   * (texels 3 to 61 and 3 to 23 of the front's 4.57-a-unit window).
   */
  static final float SCREEN_MIDDLE_X = 8.0f;
  static final float SCREEN_MIDDLE_Y = 9.2f;
  static final float SCREEN_Z = -0.9f;
  static final float SCREEN_WIDTH = 12.8f;
  static final float SCREEN_HEIGHT = 4.4f;

  /**
   * Constructs an arrival display.
   *
   * @param registryName its registry name
   */
  public BlockBusArrivalDisplay(String registryName) {
    super(registryName);
    setLightLevel(0.25F);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityBusArrivalDisplay.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitybusarrivaldisplay";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityBusArrivalDisplay();
  }
}
