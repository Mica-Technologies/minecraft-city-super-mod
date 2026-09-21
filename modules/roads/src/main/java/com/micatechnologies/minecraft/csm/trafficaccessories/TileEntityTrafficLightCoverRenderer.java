package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * TESR for the dynamic traffic signal cover. Detects the adjacent signal head (via
 * {@link BlockTrafficLightCover#scanForSignal}) and renders a rain hood that wraps the
 * signal's merged envelope: a face plate in front plus top/bottom/left/right wrap panels
 * extending back over the signal body. The shell automatically adapts to the signal's
 * orientation (vertical or horizontal), section count and sizes, and any add-on signals
 * stacked above or below the primary head.
 *
 * <p>Body tilt is mirrored from the signal head renderer's two-stage transform: the tilt
 * component rotates around the <i>signal's</i> block center (one block away from the cover)
 * before the cover's own facing rotation is applied, plus the same ±2/±4 model-unit X shift
 * the signal applies for visual alignment. This keeps the cover clamped to the signal housing
 * at every tilt/angle setting.
 *
 * <p>The scan and the tilt it reads are kept on the tile entity
 * ({@link TileEntityTrafficLightCover#getRenderScan()}) rather than redone every frame: they
 * are dropped on a neighbour change and otherwise refreshed once a second. The shell's boxes
 * depend only on the envelope, the colour scheme and the light, so they are compiled into a list
 * shared by every cover that looks the same ({@link CsmSharedDisplayLists}); the tilt and facing
 * stay transforms applied around it. {@link CsmRenderToggles#sharedBakesPerFrame} draws them per
 * frame, for comparison.
 */
public class TileEntityTrafficLightCoverRenderer
    extends TileEntitySpecialRenderer<TileEntityTrafficLightCover> {

  private static final float PANEL_THICKNESS = BlockTrafficLightCover.PANEL_THICKNESS;
  private static final float FRONT_Z = BlockTrafficLightCover.FRONT_Z;
  private static final float BACK_Z = BlockTrafficLightCover.BACK_Z;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  /** Ids for the shell envelopes, which are too many floats to pack into a list key. */
  private static final RenderAppearanceIds APPEARANCES = new RenderAppearanceIds();

  /**
   * The shell's boxes, one list per look. Key layout (a {@code long}):
   * <ul>
   *   <li>bits 0-31: the combined light at the block, as {@code getCombinedLight} returns it
   *   (baked into the vertices)</li>
   *   <li>bits 32-39: the colour scheme ordinal</li>
   *   <li>bits 40 and up: the envelope's id from {@link #APPEARANCES}</li>
   * </ul>
   */
  private static final CsmSharedDisplayLists SHELL_LISTS =
      new CsmSharedDisplayLists("signal_cover_shell");

  @Override
  public void render(TileEntityTrafficLightCover te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {

    IBlockState blockState = te.getWorld().getBlockState(te.getPos());
    if (!(blockState.getBlock() instanceof BlockTrafficLightCover)) return;

    EnumFacing facing = blockState.getValue(AbstractBlockRotatableNSEWUD.FACING);
    CoverLayout layout = layoutFor(te, facing);
    BlockTrafficLightCover.CoverSignalScan scan = layout.scan;
    float tiltAngle = layout.tiltAngle;
    float pivotX = layout.pivotX;
    float pivotZ = layout.pivotZ;
    int tiltShift = layout.tiltShift;

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    MountKitColorScheme scheme = te.getColorScheme();

    GL11.glPushMatrix();
    GL11.glTranslated(x, y, z);
    GL11.glScaled(0.0625, 0.0625, 0.0625);

    // Step 1: Tilt rotation around the signal's center (world-aligned, matches the signal
    // head renderer's add-on pivot transform). No-op when the signal isn't tilted.
    if (tiltAngle != 0f) {
      GL11.glTranslated(pivotX, 8, pivotZ);
      GL11.glRotatef(tiltAngle, 0, 1, 0);
      GL11.glTranslated(-pivotX, -8, -pivotZ);
    }

    // Step 2: Facing rotation around the cover's own block center. Up/down placements
    // replicate the legacy blockstate X rotations (down: x=90, up: x=270 → GL -90/-270).
    GL11.glTranslated(8, 8, 8);
    if (facing == EnumFacing.DOWN) {
      GL11.glRotatef(270, 1, 0, 0);
    } else if (facing == EnumFacing.UP) {
      GL11.glRotatef(90, 1, 0, 0);
    } else {
      GL11.glRotatef(getRotationAngle(scan.renderFacing), 0, 1, 0);
    }
    GL11.glTranslated(-8, -8, -8);

    // Step 3: The same lateral alignment shift the signal head applies when tilted
    if (tiltShift != 0) {
      GL11.glTranslated(tiltShift, 0, 0);
    }

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawCover(scan, scheme, combinedLight);
    } else {
      long key = ((long) layout.appearance << 40) | ((long) (scheme.ordinal() & 0xFF) << 32)
          | (combinedLight & 0xFFFFFFFFL);
      int list = SHELL_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = SHELL_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawCover(scan, scheme, combinedLight);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawCover(scan, scheme, combinedLight);
      }
    }

    GL11.glPopMatrix();

    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.resetColor();
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
  }

  /**
   * The cover's scan and tilt: the tile entity's cached copy if it is still good and was made
   * for this facing, otherwise a fresh scan, which is then cached.
   */
  private static CoverLayout layoutFor(TileEntityTrafficLightCover te, EnumFacing facing) {
    Object cached = te.getRenderScan();
    if (cached instanceof CoverLayout && ((CoverLayout) cached).facing == facing) {
      return (CoverLayout) cached;
    }

    BlockTrafficLightCover.CoverSignalScan scan =
        BlockTrafficLightCover.scanForSignal(te.getWorld(), te.getPos(), facing);

    // --- Tilt sync with the adjacent signal head ---
    float tiltAngle = 0f;
    float pivotX = 8f;
    float pivotZ = 8f;
    int tiltShift = 0;
    if (scan.signalPos != null) {
      TrafficSignalBodyTilt tilt = readBodyTilt(te.getWorld(), scan.signalPos);
      IBlockState signalState = te.getWorld().getBlockState(scan.signalPos);
      EnumFacing signalFacing = readSignalFacing(signalState);
      if (tilt != null && tilt != TrafficSignalBodyTilt.NONE && signalFacing != null) {
        // Only sync tilt when the cover's render frame matches the signal's local frame;
        // otherwise the rotation/shift would be applied in mirrored coordinates.
        if (signalFacing == scan.renderFacing) {
          DirectionSixteen bodyDirection =
              AbstractBlockControllableSignalHead.getTiltedFacing(tilt, signalFacing);
          if (bodyDirection != null) {
            tiltAngle = bodyDirection.getRotation() - getBaseFacingAngle(signalFacing);
            // Signal center expressed in the cover's (world-aligned) model space
            pivotX = 8f + (scan.signalPos.getX() - te.getPos().getX()) * 16f;
            pivotZ = 8f + (scan.signalPos.getZ() - te.getPos().getZ()) * 16f;
            tiltShift = getTiltShift(tilt);
          }
        }
      }
    }

    int appearance = APPEARANCES.idOf(
        Float.floatToIntBits(scan.minX), Float.floatToIntBits(scan.maxX),
        Float.floatToIntBits(scan.minY), Float.floatToIntBits(scan.maxY));
    CoverLayout layout =
        new CoverLayout(facing, scan, tiltAngle, pivotX, pivotZ, tiltShift, appearance);
    te.setRenderScan(layout);
    return layout;
  }

  /**
   * Renders the cover shell: a face plate at the front plus four wrap panels (top, bottom,
   * left, right) extending back over the signal body. Panel layout matches the legacy static
   * cover models — side panels run the full shell height and the top/bottom panels sit
   * between them — so no coplanar faces z-fight against the signal housing.
   *
   * <p>Geometry only: the caller binds the texture and owns every GL state.</p>
   */
  private static void drawCover(BlockTrafficLightCover.CoverSignalScan scan,
      MountKitColorScheme scheme, int combinedLight) {
    int skyLight = (combinedLight >> 16) & 0xFFFF;
    int blockLight = combinedLight & 0xFFFF;

    List<RenderHelper.Box> panelBoxes = new ArrayList<>();
    List<RenderHelper.Box> plateBoxes = new ArrayList<>();

    float minX = scan.minX;
    float maxX = scan.maxX;
    float minY = scan.minY;
    float maxY = scan.maxY;

    // Face plate (front of the cover, spans the signal opening plus the top panel edge)
    plateBoxes.add(new RenderHelper.Box(
        new float[]{minX, minY, FRONT_Z},
        new float[]{maxX, maxY + PANEL_THICKNESS, FRONT_Z + PANEL_THICKNESS}));

    // Top wrap panel (behind the face plate)
    panelBoxes.add(new RenderHelper.Box(
        new float[]{minX, maxY, FRONT_Z + PANEL_THICKNESS},
        new float[]{maxX, maxY + PANEL_THICKNESS, BACK_Z}));

    // Bottom wrap panel (includes the front edge, below the face plate)
    panelBoxes.add(new RenderHelper.Box(
        new float[]{minX, minY - PANEL_THICKNESS, FRONT_Z},
        new float[]{maxX, minY, BACK_Z}));

    // Right wrap panel (full shell height)
    panelBoxes.add(new RenderHelper.Box(
        new float[]{maxX, minY - PANEL_THICKNESS, FRONT_Z},
        new float[]{maxX + PANEL_THICKNESS, maxY + PANEL_THICKNESS, BACK_Z}));

    // Left wrap panel (full shell height)
    panelBoxes.add(new RenderHelper.Box(
        new float[]{minX - PANEL_THICKNESS, minY - PANEL_THICKNESS, FRONT_Z},
        new float[]{minX, maxY + PANEL_THICKNESS, BACK_Z}));

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();

    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    RenderHelper.addBoxesToBufferLit(panelBoxes, buffer,
        scheme.aluR, scheme.aluG, scheme.aluB, 1.0f, 0, 0, 0, skyLight, blockLight);
    RenderHelper.addBoxesToBufferLit(plateBoxes, buffer,
        scheme.aluDarkR, scheme.aluDarkG, scheme.aluDarkB, 1.0f, 0, 0, 0, skyLight, blockLight);

    tessellator.draw();
  }

  /**
   * Resolves the facing of a signal-type block. Signal heads, blankout boxes, and lane
   * control signals extend {@code AbstractBlockRotatableNSEW} whose FACING is the 4-value
   * {@code BlockHorizontal.FACING} property — a different property instance than the
   * 6-value {@code BlockDirectional.FACING} used by NSEWUD blocks, so both must be checked
   * explicitly ({@code containsKey} compares allowed values, not just the property name).
   */
  @Nullable
  private static EnumFacing readSignalFacing(IBlockState signalState) {
    if (signalState.getProperties().containsKey(AbstractBlockRotatableNSEW.FACING)) {
      return signalState.getValue(AbstractBlockRotatableNSEW.FACING);
    }
    if (signalState.getProperties().containsKey(AbstractBlockRotatableNSEWUD.FACING)) {
      EnumFacing facing = signalState.getValue(AbstractBlockRotatableNSEWUD.FACING);
      return facing.getAxis() == EnumFacing.Axis.Y ? null : facing;
    }
    return null;
  }

  /**
   * Reads the body tilt setting from a signal-type tile entity, or null if the tile entity
   * doesn't support tilt.
   */
  @Nullable
  private static TrafficSignalBodyTilt readBodyTilt(net.minecraft.world.World world,
      BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (te instanceof TileEntityTrafficSignalHead) {
      return ((TileEntityTrafficSignalHead) te).getBodyTilt();
    }
    if (te instanceof TileEntityBlankoutBox) {
      return ((TileEntityBlankoutBox) te).getBodyTilt();
    }
    if (te instanceof TileEntityLaneControlSignal) {
      return ((TileEntityLaneControlSignal) te).getBodyTilt();
    }
    return null;
  }

  /**
   * Lateral alignment shift in model units, matching the signal head renderer's tilt
   * compensation (±2 for tilt, ±4 for angle).
   */
  private static int getTiltShift(TrafficSignalBodyTilt tilt) {
    switch (tilt) {
      case RIGHT_ANGLE: return -4;
      case RIGHT_TILT:  return -2;
      case LEFT_TILT:   return 2;
      case LEFT_ANGLE:  return 4;
      default:          return 0;
    }
  }

  private static float getRotationAngle(EnumFacing facing) {
    switch (facing) {
      case NORTH: return 0f;
      case WEST:  return 90f;
      case SOUTH: return 180f;
      case EAST:  return 270f;
      default:    return 0f;
    }
  }

  private static float getBaseFacingAngle(EnumFacing facing) {
    switch (facing) {
      case SOUTH: return 180.0f;
      case WEST:  return 90.0f;
      case NORTH: return 0.0f;
      case EAST:  return 270.0f;
      default:    return 0.0f;
    }
  }

  /**
   * A cached scan: the facing it was made for, the envelope, the tilt transform read from the
   * head, and the envelope's appearance id.
   */
  private static final class CoverLayout {

    final EnumFacing facing;
    final BlockTrafficLightCover.CoverSignalScan scan;
    final float tiltAngle;
    final float pivotX;
    final float pivotZ;
    final int tiltShift;
    final int appearance;

    CoverLayout(EnumFacing facing, BlockTrafficLightCover.CoverSignalScan scan, float tiltAngle,
        float pivotX, float pivotZ, int tiltShift, int appearance) {
      this.facing = facing;
      this.scan = scan;
      this.tiltAngle = tiltAngle;
      this.pivotX = pivotX;
      this.pivotZ = pivotZ;
      this.tiltShift = tiltShift;
      this.appearance = appearance;
    }
  }
}
