package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import com.micatechnologies.minecraft.csm.trafficaccessories.GuideSignFontRenderer;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

/**
 * Draws the route number on a {@link BlockDynamicRouteMarkerSign}, and nothing else.
 *
 * <p>The marker itself -- the shield, its outline and its gray back -- is an ordinary block
 * model, picked by the {@code shield} block property, so it is baked into the chunk mesh like
 * any other sign and costs this renderer nothing. All that is left is the number, which is two
 * or three glyphs of the FHWA legend font set where the marker's own
 * {@link GuideSignShieldType} entry says its number goes. Those four measurements are the same
 * ones the dynamic guide sign uses, so a route drawn on a guide sign and the same route on a
 * post are lettered identically.</p>
 *
 * <h3>The frame</h3>
 *
 * <p>The blockstate turns the model by {@code y} degrees for the sign's facing, and Minecraft's
 * {@code ModelRotation} applies a blockstate {@code y} as a rotation of <b>minus</b> that angle,
 * so the renderer has to turn the same way. It then turns a further 180 degrees, which puts the
 * reader in front of the plate with +X to their right and +Y up -- the un-mirrored pixel space
 * {@link GuideSignFontRenderer} documents. In that frame a marker's {@code routeTextCenterX},
 * which is measured from the marker's left edge as it is read, is simply {@code 16 * fraction}.
 * The plate's own depth mirrors with it: a face the model puts at {@code z} sits at
 * {@code 16 - z} here, which is why the three shifts below are written as they are.</p>
 *
 * @version 1.0
 * @since 2026.9.20
 */
public class TileEntityDynamicRouteMarkerSignRenderer
    extends TileEntitySpecialRenderer<TileEntityDynamicRouteMarkerSign> {

  /** Where each shift puts the plate's painted face, in model units from the block's north. */
  private static final double PLATE_Z_NONE = 0.485;
  private static final double PLATE_Z_SETBACK = 12.985;
  private static final double PLATE_Z_BACKTOBACK = 28.49;

  /** How far in front of the plate the legend is drawn, in model units. */
  private static final double LEGEND_PROUD = 0.02;

  /** One block in model units, and the scale that gets there. */
  private static final double UNITS = 16.0;

  @Override
  public void render(TileEntityDynamicRouteMarkerSign te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }
    String route = te.getRouteNumber();
    if (route == null || route.isEmpty()) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!(state.getBlock() instanceof BlockDynamicRouteMarkerSign)) {
      return;
    }
    IBlockState actual = state.getActualState(te.getWorld(), te.getPos());
    final GuideSignShieldType shield = te.getShield();

    final int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    final int sky = (combinedLight >> 16) & 0xFFFF;
    final int blockLight = combinedLight & 0xFFFF;

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    // DirectionEight.getRotationDegrees is the angle the blockstate turns the model by, and
    // every other renderer in the mod applies it with this sign, so the legend and the plate
    // can never disagree about which way the sign faces. The extra half turn puts the reader
    // in front of the plate, which is the frame the font renderer draws in.
    GlStateManager.rotate(
        180.0f + actual.getValue(BlockDynamicRouteMarkerSign.FACING).getRotationDegrees(),
        0.0f, 1.0f, 0.0f);
    GlStateManager.scale(1.0 / UNITS, 1.0 / UNITS, 1.0 / UNITS);
    GlStateManager.translate(-UNITS / 2.0, 0.0, -UNITS / 2.0);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    drawRoute(route, shield, legendZ(actual.getValue(BlockDynamicRouteMarkerSign.SHIFT)),
        sky, blockLight);

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /**
   * Sets the number at the cap height its marker asks for, shrunk if it would run outside the
   * part of the face the marker leaves free -- which is what makes a three-digit route fit a
   * shield drawn around a two-digit one.
   */
  private static void drawRoute(String route, GuideSignShieldType shield, double zLegend,
      int sky, int blockLight) {
    float cap = (float) (UNITS * shield.getRouteTextCapFraction());
    final float maxWidth = (float) (UNITS * shield.getRouteTextMaxFraction());
    float width = GuideSignFontRenderer.getStringWidth(route, cap);
    if (width > maxWidth && width > 0.0f) {
      cap *= maxWidth / width;
      width = maxWidth;
    }
    final float centerX = (float) (UNITS * shield.getRouteTextCenterX());
    // The marker's centre Y is measured from the top of its face, as the atlas cell is laid
    // out; this space runs upwards from the plate's bottom edge.
    final float centerY = (float) (UNITS * (1.0 - shield.getRouteTextCenterY()));
    GuideSignFontRenderer.drawString(route, centerX - width / 2.0f, centerY, (float) zLegend,
        cap, shield.getRouteTextColor(), sky, blockLight);
  }

  /** Where the legend sits in the mirrored frame, for the shift the sign is drawn in. */
  private static double legendZ(SignShift shift) {
    switch (shift) {
      case SETBACK:
        return UNITS - PLATE_Z_SETBACK + LEGEND_PROUD;
      case BACKTOBACK:
        return UNITS - PLATE_Z_BACKTOBACK + LEGEND_PROUD;
      default:
        return UNITS - PLATE_Z_NONE + LEGEND_PROUD;
    }
  }
}
