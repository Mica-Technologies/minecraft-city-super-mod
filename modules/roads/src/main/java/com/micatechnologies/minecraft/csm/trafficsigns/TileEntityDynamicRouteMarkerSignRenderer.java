package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
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

  /**
   * Minecraft's diffuse shade for a vertical block face: 0.8 looking along z, 0.6 along
   * x. It is baked into the plate's vertex colours when the chunk is meshed, and a tile
   * entity renderer gets none of it, so a legend drawn at full brightness sits visibly
   * brighter than the shield under it -- worst on an east or west facing, where the plate
   * is at 0.6 and the legend at 1.0.
   */
  private static final float SHADE_Z = 0.8f;
  private static final float SHADE_X = 0.6f;

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
        plateShade(actual.getValue(BlockDynamicRouteMarkerSign.FACING)),
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
      float shade, int sky, int blockLight) {
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
        cap, shaded(shield.getRouteTextColor(), shade), sky, blockLight);
  }

  /**
   * The shade the plate's own face is drawn at, so the legend on it matches.
   *
   * <p>The plate paints its art on the model's north face, which the blockstate turns onto
   * whichever side the sign reads toward. A diagonal facing leaves the quad's normal
   * halfway between two sides, and Minecraft resolves that tie in favour of north or
   * south, so only a due east or west sign is drawn at the darker multiplier.</p>
   */
  private static float plateShade(DirectionEight facing) {
    return facing == DirectionEight.E || facing == DirectionEight.W ? SHADE_X : SHADE_Z;
  }

  /** A legend colour multiplied by the plate's shade, channel by channel. */
  private static int shaded(int color, float shade) {
    final int r = Math.round(((color >> 16) & 0xFF) * shade);
    final int g = Math.round(((color >> 8) & 0xFF) * shade);
    final int b = Math.round((color & 0xFF) * shade);
    return (r << 16) | (g << 8) | b;
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
