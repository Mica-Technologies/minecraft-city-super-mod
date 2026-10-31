package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.SpanWireCableGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

/**
 * Draws every cable an anchor owns, from its {@link BridgeCableLayout}, through the span wire's
 * tube builder so the two kinds of cable are lit alike.
 *
 * <p>All of an anchor's cables, fittings, hangers and LED strips are one display list, keyed on
 * the layout and the light baked into its vertices, so a bridge's cables cost a matrix push and a
 * list call each per frame. <b>The white texture is bound outside the list, every frame,
 * immediately before the call</b>: see "Display lists: one texture, no cached state" in {@code
 * assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}. Nothing inside the list touches cached GL state.
 *
 * <p>A global renderer: drawn whenever its box is in view, not only when the anchor's own chunk
 * section is. A stay runs fifty blocks from its anchor, and culling it with the anchor's section
 * would drop it out of the sky as soon as the pylon top went behind something.
 */
public class TileEntityBridgeCableAnchorRenderer
    extends TileEntitySpecialRenderer<TileEntityBridgeCableAnchor> {

  private static final CsmDisplayListCache DISPLAY_LISTS =
      new CsmDisplayListCache("bridge_cable");

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  /** Lightmap coordinates for full brightness, the LED strip's. */
  private static final int FULL_BRIGHT = 240;

  /** Releases the cached geometry for a position. Called from the tile entity's lifecycle. */
  public static void cleanupDisplayList(BlockPos pos) {
    DISPLAY_LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntityBridgeCableAnchor te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (CsmRenderToggles.skipBridgeCable || te.getWorld() == null) {
      return;
    }
    final BridgeCableLayout layout = te.getLayout();
    if (layout == null || layout.getTubes().isEmpty()) {
      return;
    }

    final BlockPos pos = te.getPos();
    final int combinedLight = te.getWorld().getCombinedLight(pos, 0);
    final int skyLight = (combinedLight >> 16) & 0xFFFF;
    final int blockLight = combinedLight & 0xFFFF;

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GL11.glPushMatrix();
    GL11.glTranslated(x, y, z);

    // Vertices are relative to the anchor's corner, where the translate above put the origin; a
    // display list holds vertices, not the model-view matrix, so it stays right as the camera moves.
    final Vec3d origin = new Vec3d(pos.getX(), pos.getY(), pos.getZ());

    if (CsmRenderToggles.bridgeCablePerFrame) {
      Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
      draw(layout, origin, skyLight, blockLight);
    } else {
      final long stateKey = layout.getKey() * 31L + combinedLight;
      int displayList = DISPLAY_LISTS.get(pos, stateKey);
      if (displayList == CsmDisplayListCache.NO_LIST) {
        // A new layout replaces the old outright: free the old lists rather than keep up to four
        // shapes of a cable that no longer exists.
        DISPLAY_LISTS.invalidate(pos);
        displayList = DISPLAY_LISTS.allocate(pos, stateKey);
        if (displayList != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(displayList, GL11.GL_COMPILE);
          draw(layout, origin, skyLight, blockLight);
          GL11.glEndList();
        }
      }
      Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
      if (displayList == CsmDisplayListCache.NO_LIST) {
        draw(layout, origin, skyLight, blockLight);
      } else {
        GL11.glCallList(displayList);
      }
    }

    GL11.glPopMatrix();
    GlStateManager.resetColor();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
  }

  /** Emits the whole layout as one draw. */
  static void draw(BridgeCableLayout layout, Vec3d origin, int skyLight, int blockLight) {
    final Tessellator tessellator = Tessellator.getInstance();
    final BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    for (BridgeCableLayout.Tube tube : layout.getTubes()) {
      final float r = ((tube.rgb >> 16) & 0xFF) / 255.0f;
      final float g = ((tube.rgb >> 8) & 0xFF) / 255.0f;
      final float b = (tube.rgb & 0xFF) / 255.0f;
      final SpanWireCableGeometry.FacetColour colour;
      if (tube.stripeRgb < 0) {
        colour = (segment, side, out) -> {
          out[0] = r;
          out[1] = g;
          out[2] = b;
        };
      } else {
        // Two stripes on opposite sides, wound round the cable one facet a segment: the helical
        // fillet on a stay's sheath.
        final float sr = ((tube.stripeRgb >> 16) & 0xFF) / 255.0f;
        final float sg = ((tube.stripeRgb >> 8) & 0xFF) / 255.0f;
        final float sb = (tube.stripeRgb & 0xFF) / 255.0f;
        final int half = Math.max(1, tube.sides / 2);
        colour = (segment, side, out) -> {
          final boolean stripe = Math.floorMod(side - segment, half) == 0;
          out[0] = stripe ? sr : r;
          out[1] = stripe ? sg : g;
          out[2] = stripe ? sb : b;
        };
      }
      SpanWireCableGeometry.emitTubePath(buffer, tube.points, origin, tube.radius, tube.sides,
          colour, tube.glow ? FULL_BRIGHT : skyLight, tube.glow ? FULL_BRIGHT : blockLight);
    }
    tessellator.draw();
  }

  @Override
  public boolean isGlobalRenderer(TileEntityBridgeCableAnchor te) {
    return true;
  }
}
