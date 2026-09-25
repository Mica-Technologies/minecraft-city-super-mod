package com.micatechnologies.minecraft.csm.transit.board;

import com.micatechnologies.minecraft.csm.transit.stop.BusDepartures;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusArrivalDisplayRenderer;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusStopFlag;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a bay display's text: the arrival display's panel ({@link
 * TileEntityBusArrivalDisplayRenderer#drawPanel}), three amber lines a page, on a screen of the
 * same size, so the two share every display list and read alike.
 *
 * <p>The screen's place is {@code gen_transit_boards.py}'s ({@code BAY_SCREEN_*}): change one,
 * change both.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityBusBayDisplayRenderer
    extends TileEntitySpecialRenderer<TileEntityBusBayDisplay> {

  /** The screen's middle, facing north, in sixteenths. */
  private static final float SCREEN_MIDDLE_X = 8.0f;
  private static final float SCREEN_MIDDLE_Y = 7.6f;
  private static final float SCREEN_Z = 14.6f;

  /** The arrivals being drawn: scratch, render thread only. */
  private static final int[] ROUTES =
      new int[TileEntityBusStopFlag.PLATES * BusDepartures.PER_ROUTE];
  private static final int[] MINUTES =
      new int[TileEntityBusStopFlag.PLATES * BusDepartures.PER_ROUTE];

  @Override
  public void render(TileEntityBusBayDisplay te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    te.refreshView();
    int count = te.arrivals(ROUTES, MINUTES);
    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // the model's front (north) turned to face +z: its point (mx, my, mz) is
    // (8 - mx, my, 8 - mz) / 16 here, with the reader's right at +x
    GlStateManager.rotate(-te.getFacing().getHorizontalAngle(), 0F, 1F, 0F);
    GlStateManager.translate((8 - SCREEN_MIDDLE_X) / 16.0, SCREEN_MIDDLE_Y / 16.0,
        (8 - SCREEN_Z) / 16.0);
    GlStateManager.scale(1 / 16.0, 1 / 16.0, 1 / 16.0);
    TileEntityBusArrivalDisplayRenderer.drawPanel(ROUTES, MINUTES, count,
        te.getWorld().getTotalWorldTime());
    GlStateManager.popMatrix();
  }
}
