package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
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
 * Draws the messenger cable.
 *
 * <p>Each attachment draws <em>only</em> the piece of cable running from itself to the next
 * attachment along the span (the plan's D6). That keeps each tile entity's geometry to the gap
 * between two mounts rather than the whole span, so frustum culling and chunk unloading degrade
 * a long span gracefully instead of taking all of it away with one chunk. Every piece is a slice
 * of the same solved curve, so the joins are exact and the cable reads as one continuous
 * catenary — which is what a real messenger is (the plan's D2).
 *
 * <p>The far anchor owns no piece of cable, but it still draws its own hardware.
 *
 * <p>Geometry is baked into a display list keyed on the span's shape and the block light, and
 * <b>the texture is bound outside the list, unconditionally, immediately before the call.</b>
 * That is not optional — see "Display lists: one texture, no cached state" in
 * {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}. Binding inside the list can silently record no
 * bind at all, because {@code TextureManager} skips a redundant bind, and the list then samples
 * whatever the block atlas happens to have at the white pixel's UV.
 */
public abstract class SpanWireCableRenderer<T extends AbstractTileEntitySpanWireAttachment>
    extends TileEntitySpecialRenderer<T> {

  /**
   * Cached cable geometry, keyed by the position of the attachment that owns the piece. Released
   * from the tile entity's lifecycle callbacks and bounded as a backstop; see
   * {@link CsmDisplayListCache}.
   */
  private static final CsmDisplayListCache DISPLAY_LISTS =
      new CsmDisplayListCache("span_wire_cable");

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  /**
   * Galvanized steel messenger, weathered. Dark enough to read against a bright sky, which is
   * the background a span is almost always seen against, without going to flat black.
   */
  private static final float CABLE_RED = 0.20f;
  private static final float CABLE_GREEN = 0.21f;
  private static final float CABLE_BLUE = 0.23f;

  /**
   * The lashed conductor bundle running under the messenger: black insulation, and slightly
   * slimmer than the steel it is tied to.
   */
  private static final double CONDUCTOR_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 0.8;
  private static final double CONDUCTOR_DROP =
      SpanWireCableGeometry.CABLE_RADIUS + SpanWireCableGeometry.CABLE_RADIUS * 0.8;
  private static final float CONDUCTOR_RED = 0.09f;
  private static final float CONDUCTOR_GREEN = 0.09f;
  private static final float CONDUCTOR_BLUE = 0.10f;

  /** The lower tether of a box span: same steel as the messenger, a touch slimmer. */
  private static final double TETHER_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 0.85;

  /** Releases the cached geometry for a position. Called from the tile entity's lifecycle. */
  public static void cleanupDisplayList(BlockPos pos) {
    DISPLAY_LISTS.invalidate(pos);
  }

  @Override
  public void render(T te, double x, double y, double z, float partialTicks, int destroyStage,
      float alpha) {
    if (CsmRenderToggles.skipSpanWireCable) {
      return;
    }
    if (te.getWorld() == null) {
      return;
    }

    final SpanWireCatenary cable = te.getCable();
    if (cable == null) {
      return;
    }

    // The far anchor owns no cable but still wears its own hardware -- its dead-end thimble and
    // its guy. Only the segment is skipped for it, not the whole draw; an early return here is
    // what left the far end of every span bare.
    final BlockPos next = te.getNextAttachment();
    final double fromT = te.getSpanParameter();
    final double toT = te.getNextSpanParameter();
    final boolean drawsSegment = next != null && Math.abs(toT - fromT) > 1.0e-9;

    final BlockPos pos = te.getPos();
    final int combinedLight = te.getWorld().getCombinedLight(pos, 0);
    final int skyLight = (combinedLight >> 16) & 0xFFFF;
    final int blockLight = combinedLight & 0xFFFF;

    GlStateManager.disableLighting();
    GlStateManager.disableCull();

    GL11.glPushMatrix();
    GL11.glTranslated(x, y, z);

    // Vertices are emitted relative to this block's own corner, because glTranslate above has
    // put the origin there. The list holds those relative vertices, so it stays valid wherever
    // the camera is; a display list does not capture the model-view matrix.
    final Vec3d origin = new Vec3d(pos.getX(), pos.getY(), pos.getZ());

    if (CsmRenderToggles.spanWireCablePerFrame) {
      Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
      drawCable(te, cable, fromT, toT, drawsSegment, origin, skyLight, blockLight);
    } else {
      final long stateKey = stateKey(te, combinedLight);
      int displayList = DISPLAY_LISTS.get(pos, stateKey);
      if (displayList == CsmDisplayListCache.NO_LIST) {
        displayList = DISPLAY_LISTS.allocate(pos, stateKey);
        if (displayList != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(displayList, GL11.GL_COMPILE);
          drawCable(te, cable, fromT, toT, drawsSegment, origin, skyLight, blockLight);
          GL11.glEndList();
        }
      }
      Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
      if (displayList == CsmDisplayListCache.NO_LIST) {
        // The cache is full and would not give us a list. Draw straight rather than vanish.
        drawCable(te, cable, fromT, toT, drawsSegment, origin, skyLight, blockLight);
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

  private void drawCable(T te, SpanWireCatenary cable, double fromT, double toT,
      boolean drawsSegment, Vec3d origin, int skyLight, int blockLight) {
    final Tessellator tessellator = Tessellator.getInstance();
    final BufferBuilder buffer = tessellator.getBuffer();

    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    if (!drawsSegment) {
      // Hardware only: the far anchor of a span.
      emitAttachmentHardware(te, buffer, cable, fromT, origin, skyLight, blockLight);
      tessellator.draw();
      return;
    }
    SpanWireCableGeometry.emitSegment(buffer, cable, fromT, toT, origin,
        SpanWireCableGeometry.CABLE_RADIUS, CABLE_RED, CABLE_GREEN, CABLE_BLUE,
        skyLight, blockLight);
    // The conductors lashed under the messenger. A span wire carries the signals' power and
    // control along the same cable that holds them up, and that bundle is easily as visible as
    // the steel it is tied to -- without it a span reads as a bare guy wire with lights on it.
    SpanWireCableGeometry.emitTubePath(buffer,
        SpanWireCableGeometry.sampleSegment(cable, fromT, toT, CONDUCTOR_DROP), false, origin,
        CONDUCTOR_RADIUS, CONDUCTOR_RED, CONDUCTOR_GREEN, CONDUCTOR_BLUE, skyLight, blockLight);

    // The lower tether of a box span. Solved separately rather than offset from the messenger,
    // because it is strung tighter and therefore is not the same curve moved down: the gap
    // between the two closes toward midspan, which is what a box span looks like.
    final SpanWireCatenary tether = te.getTether();
    if (tether != null) {
      SpanWireCableGeometry.emitSegment(buffer, tether, fromT, toT, origin, TETHER_RADIUS,
          CABLE_RED, CABLE_GREEN, CABLE_BLUE, skyLight, blockLight);
    }
    // Same buffer, same draw call, same display list: hardware and cable are one assembly, and
    // splitting them would buy nothing but a second draw.
    emitAttachmentHardware(te, buffer, cable, fromT, origin, skyLight, blockLight);
    tessellator.draw();
  }

  /**
   * Hook for whatever this kind of attachment hangs off the cable. Anchors hang nothing; hangers
   * override this to drop to the mount they carry.
   *
   * @param atT where this attachment sits along the span
   */
  protected void emitAttachmentHardware(T te, BufferBuilder buffer, SpanWireCatenary cable,
      double atT, Vec3d origin, int skyLight, int blockLight) {
    // Nothing by default.
  }

  /**
   * The inputs the compiled geometry depends on: the shape of the span, and the light baked into
   * the vertices. A change in either has to force a recompile, or the cable would keep drawing
   * its old curve, or keep its old lighting through a sunset.
   */
  private static long stateKey(AbstractTileEntitySpanWireAttachment te, int combinedLight) {
    final SpanWireDefinition span = te.getSpan();
    long key = span == null ? 0L : span.hashCode();
    key = key * 31L + Double.doubleToLongBits(te.getSpanParameter());
    key = key * 31L + Double.doubleToLongBits(te.getNextSpanParameter());
    key = key * 31L + combinedLight;
    key = key * 31L + te.getHardwareStateKey();
    return key;
  }
}
