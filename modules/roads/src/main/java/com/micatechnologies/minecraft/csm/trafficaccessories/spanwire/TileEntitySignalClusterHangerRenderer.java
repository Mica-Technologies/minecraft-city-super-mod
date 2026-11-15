package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

/**
 * Draws a signal cluster hanger: the top frame from its hub out to each head, a drop onto each
 * head's top, a lower tie under the deepest head, and what holds the hub up.
 *
 * <p>What holds it up depends on whether the hanger is on a span. Linked, the span hanger's own
 * mast, saddle and coils come down onto the hub, exactly as on the span cluster mount. Unlinked --
 * under a mast arm, whose underside is on the block boundary above -- a clamp sits at the top of
 * the block and a pipe comes down from it, jogging over to the hub where the cluster's middle is not
 * under the hanger's own block.
 *
 * <p>The frame is one shape for every layout. A pair is a tee, a row is a bar with drops, a plus is
 * a spider, and an add-on beside a head, whose top is lower, gets a longer drop -- an L arm. None of
 * that needs classifying, because each arm simply runs from the hub to a head.
 *
 * <p>Geometry is baked into one display list keyed on the hardware state and the light, with the
 * white texture bound outside it every frame; see "Display lists: one texture, no cached state" in
 * {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}. The linked path goes through the span renderer's
 * own list; the unlinked one, which the span renderer does not draw at all, has its own.
 */
public class TileEntitySignalClusterHangerRenderer
    extends SpanWireCableRenderer<TileEntitySignalClusterHanger> {

  private static final CsmDisplayListCache UNLINKED_LISTS =
      new CsmDisplayListCache("signal_cluster_hanger");

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  /** The top frame's arms: a pipe, stouter than the span's messenger. */
  private static final double ARM_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 1.8;

  /** The drops onto each head. */
  private static final double DROP_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 1.6;

  /** The fitting where the arms meet: a short, fat collar that covers the open tube ends. */
  private static final double HUB_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 2.6;
  private static final double HUB_HALF_HEIGHT = 0.05;

  /** The lower tie, thinner than the frame that carries the weight. */
  private static final double TIE_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 1.1;

  /** The pipe down from an arm's clamp to the frame, and the clamp itself. */
  private static final double PIPE_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 2.0;
  private static final double CLAMP_RADIUS = SpanWireCableGeometry.CABLE_RADIUS * 3.0;
  private static final double CLAMP_HALF_LENGTH = 0.14;

  /** How far below the frame a drop reaches when the head reports no top. */
  private static final double DROP_LENGTH = 0.18;

  /** The longest a drop will stretch, so a head reporting nonsense cannot pull one to the ground. */
  private static final double MAX_DROP_LENGTH = 1.5;

  /** Arms or legs shorter than this are left out: a tube inside a tube. */
  private static final double EPSILON = 0.02;

  /**
   * The span hanger's own mast, saddle and coils, borrowed so a linked hanger hangs exactly as a
   * span cluster mount does. Its tie is not wanted: the lower tie below draws the cluster's one.
   */
  private final TileEntitySpanWireHangerRenderer singleMount =
      new TileEntitySpanWireHangerRenderer() {
        @Override
        protected boolean drawsTetherTie() {
          return false;
        }
      };

  /** Releases the unlinked geometry for a position. Called from the tile entity's lifecycle. */
  public static void cleanupUnlinkedList(BlockPos pos) {
    UNLINKED_LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntitySignalClusterHanger te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te.getWorld() == null) {
      return;
    }
    if (te.getCable() != null) {
      super.render(te, x, y, z, partialTicks, destroyStage, alpha);
      return;
    }
    if (CsmRenderToggles.skipSpanWireCable) {
      return;
    }

    final BlockPos pos = te.getPos();
    final int combinedLight = te.getWorld().getCombinedLight(pos, 0);
    final int skyLight = (combinedLight >> 16) & 0xFFFF;
    final int blockLight = combinedLight & 0xFFFF;
    final Vec3d origin = new Vec3d(pos.getX(), pos.getY(), pos.getZ());

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GL11.glPushMatrix();
    GL11.glTranslated(x, y, z);

    final long stateKey = te.getHardwareStateKey() * 31L + combinedLight;
    int displayList = UNLINKED_LISTS.get(pos, stateKey);
    if (displayList == CsmDisplayListCache.NO_LIST) {
      displayList = UNLINKED_LISTS.allocate(pos, stateKey);
      if (displayList != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        drawUnlinked(te, origin, skyLight, blockLight);
        GL11.glEndList();
      }
    }
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    if (displayList == CsmDisplayListCache.NO_LIST) {
      drawUnlinked(te, origin, skyLight, blockLight);
    } else {
      GL11.glCallList(displayList);
    }

    GL11.glPopMatrix();
    GlStateManager.resetColor();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
  }

  private void drawUnlinked(TileEntitySignalClusterHanger te, Vec3d origin, int skyLight,
      int blockLight) {
    final Tessellator tessellator = Tessellator.getInstance();
    final BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    final TrafficSignalBodyColor color = te.getColor();
    final float red = color.getRed();
    final float green = color.getGreen();
    final float blue = color.getBlue();
    final Vec3d hub = te.getFrameHub();
    final Vec3d attach = te.getAttachPoint();

    // The clamp under the arm, on the block boundary above, and the pipe down from it to the
    // frame's height. Square to the block: an arm runs along an axis, and nothing here knows which.
    final double topY = te.getPos().getY() + 1.0 - CLAMP_RADIUS;
    SpanWireCableGeometry.emitStraightTube(buffer,
        new Vec3d(attach.x - CLAMP_HALF_LENGTH, topY, attach.z),
        new Vec3d(attach.x + CLAMP_HALF_LENGTH, topY, attach.z),
        origin, CLAMP_RADIUS, red, green, blue, skyLight, blockLight);
    final Vec3d pipeFoot = new Vec3d(attach.x, hub.y, attach.z);
    SpanWireCableGeometry.emitStraightTube(buffer, new Vec3d(attach.x, topY, attach.z), pipeFoot,
        origin, PIPE_RADIUS, red, green, blue, skyLight, blockLight);
    // A jog over to the hub when the cluster's middle is not under this block -- a pair across two
    // columns centres on their shared edge.
    if (horizontalDistance(pipeFoot, hub) > EPSILON) {
      SpanWireCableGeometry.emitStraightTube(buffer, pipeFoot, hub, origin, ARM_RADIUS,
          red, green, blue, skyLight, blockLight);
      emitHub(buffer, pipeFoot, origin, red, green, blue, skyLight, blockLight);
    }

    emitFrame(buffer, te, hub, origin, red, green, blue, skyLight, blockLight);
    tessellator.draw();
  }

  @Override
  protected void emitAttachmentHardware(TileEntitySignalClusterHanger te, BufferBuilder buffer,
      SpanWireCatenary cable, double atT, Vec3d origin, int skyLight, int blockLight) {
    // The mast stands on getDrawnFootPoint, which for this hanger is the frame's hub, so the clamp,
    // saddle and coils come straight down onto the middle of the frame.
    singleMount.emitAttachmentHardware(te, buffer, cable, atT, origin, skyLight, blockLight);
    final TrafficSignalBodyColor color = te.getColor();
    emitFrame(buffer, te, te.getFrameHub(), origin, color.getRed(), color.getGreen(),
        color.getBlue(), skyLight, blockLight);
  }

  /**
   * The top frame, the drops and the lower tie.
   *
   * @param hub the frame's middle at bracket height, computed once by the caller so the payload
   *            list is not re-read while it is being walked.
   */
  private void emitFrame(BufferBuilder buffer, TileEntitySignalClusterHanger te, Vec3d hub,
      Vec3d origin, float red, float green, float blue, int skyLight, int blockLight) {
    final List<TileEntitySpanWireClusterMount.ClusterPayload> payloads = te.getPayloads();
    emitHub(buffer, hub, origin, red, green, blue, skyLight, blockLight);

    if (payloads.isEmpty()) {
      // Nothing hangs yet: a stub below the hub, so an empty hanger still reads as one.
      SpanWireCableGeometry.emitStraightTube(buffer, hub, hub.subtract(0.0, DROP_LENGTH, 0.0),
          origin, DROP_RADIUS, red, green, blue, skyLight, blockLight);
      return;
    }

    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      final Vec3d point = TileEntitySignalClusterHanger.hardwarePoint(payload);
      final Vec3d above = new Vec3d(point.x, hub.y, point.z);
      if (horizontalDistance(hub, above) > EPSILON) {
        SpanWireCableGeometry.emitStraightTube(buffer, hub, above, origin, ARM_RADIUS,
            red, green, blue, skyLight, blockLight);
        emitElbow(buffer, above, origin, red, green, blue, skyLight, blockLight);
      }
      final Vec3d foot = new Vec3d(point.x, dropFootY(hub.y, payload.getTopY()), point.z);
      SpanWireCableGeometry.emitStraightTube(buffer, above, foot, origin, DROP_RADIUS,
          red, green, blue, skyLight, blockLight);
    }

    emitLowerTie(buffer, te, hub, payloads, origin, red, green, blue, skyLight, blockLight);
  }

  /**
   * Arms from a post under the hub to the underside of every head, at the deepest head's underside,
   * with a leg down to it from any head that ends higher. Nothing for a single head.
   */
  private void emitLowerTie(BufferBuilder buffer, TileEntitySignalClusterHanger te, Vec3d hub,
      List<TileEntitySpanWireClusterMount.ClusterPayload> payloads, Vec3d origin, float red,
      float green, float blue, int skyLight, int blockLight) {
    double tieY = Double.POSITIVE_INFINITY;
    int tied = 0;
    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      if (!Double.isNaN(payload.getTieY())) {
        tieY = Math.min(tieY, payload.getTieY());
        tied++;
      }
    }
    if (tied < 2) {
      return;
    }
    final Vec3d tieHub = new Vec3d(hub.x, tieY, hub.z);
    emitHub(buffer, tieHub, origin, red, green, blue, skyLight, blockLight);
    for (TileEntitySpanWireClusterMount.ClusterPayload payload : payloads) {
      if (Double.isNaN(payload.getTieY())) {
        continue;
      }
      final Vec3d point = TileEntitySignalClusterHanger.hardwarePoint(payload);
      final Vec3d end = new Vec3d(point.x, tieY, point.z);
      if (horizontalDistance(tieHub, end) > EPSILON) {
        SpanWireCableGeometry.emitStraightTube(buffer, tieHub, end, origin, TIE_RADIUS,
            red, green, blue, skyLight, blockLight);
      }
      if (payload.getTieY() - tieY > EPSILON) {
        SpanWireCableGeometry.emitStraightTube(buffer, new Vec3d(point.x, payload.getTieY(),
            point.z), end, origin, TIE_RADIUS, red, green, blue, skyLight, blockLight);
      }
    }

    // A box span's tether holds the assembly by one tie, from the middle of the lower frame.
    final SpanWireCatenary tether = te.getTether();
    if (tether != null) {
      TileEntitySpanWireHangerRenderer.emitTetherTieAt(buffer, tether, te.getSpan(), tieHub,
          tieY, origin, skyLight, blockLight);
    }
  }

  /** The collar where arms meet, a short vertical fitting fatter than any of them. */
  private static void emitHub(BufferBuilder buffer, Vec3d at, Vec3d origin, float red,
      float green, float blue, int skyLight, int blockLight) {
    SpanWireCableGeometry.emitStraightTube(buffer, at.add(0.0, HUB_HALF_HEIGHT, 0.0),
        at.subtract(0.0, HUB_HALF_HEIGHT, 0.0), origin, HUB_RADIUS, red, green, blue, skyLight,
        blockLight);
  }

  /** The fitting where an arm turns down into its drop. */
  private static void emitElbow(BufferBuilder buffer, Vec3d at, Vec3d origin, float red,
      float green, float blue, int skyLight, int blockLight) {
    SpanWireCableGeometry.emitStraightTube(buffer, at.add(0.0, HUB_HALF_HEIGHT * 0.7, 0.0),
        at.subtract(0.0, HUB_HALF_HEIGHT * 0.7, 0.0), origin, ARM_RADIUS * 1.25, red, green, blue,
        skyLight, blockLight);
  }

  /** Onto the head's top where it reported one, bounded either way; a fixed stub where not. */
  private static double dropFootY(double frameY, double payloadTopY) {
    if (Double.isNaN(payloadTopY)) {
      return frameY - DROP_LENGTH;
    }
    return Math.max(frameY - MAX_DROP_LENGTH, Math.min(frameY - DROP_LENGTH, payloadTopY));
  }

  private static double horizontalDistance(Vec3d a, Vec3d b) {
    final double dx = a.x - b.x;
    final double dz = a.z - b.z;
    return Math.sqrt(dx * dx + dz * dz);
  }
}
