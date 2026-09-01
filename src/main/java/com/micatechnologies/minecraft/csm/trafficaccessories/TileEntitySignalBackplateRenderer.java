package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHeadRenderer;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;


/**
 * Draws a signal backplate.
 *
 * <p><b>All</b> of them, not just the shifted ones. The block itself renders nothing, so this is
 * the only thing drawing a plate. That was a deliberate swap away from the obvious design -- keep
 * the cheap chunk geometry for a plate sitting still, and only take over when one has to move --
 * because the property that would have selected between them does not survive contact with the
 * blockstates: four families across two dialects, twenty of which enumerate all one hundred and
 * twenty property combinations by hand, and a fitted subclass building its own state container.
 * One path is less code and less to get wrong, and it is the path that can shift, tilt or rotate a
 * plate freely.
 *
 * <p>It re-renders the plate's own baked model rather than rebuilding the shape in code. There are
 * nineteen backplate models across shapes and section counts, with ninety-odd blocks pointing at
 * them and their own textures; reproducing that here would put all of it at risk in one step. The
 * geometry and every texture stay exactly as authored, and only the transform is new.
 *
 * <p>Nineteen rather than the eighty-three there once were, because the transform absorbed the
 * tilt: a plate is turned here instead of being drawn from a model authored pre-tilted, so the
 * sixty-four pre-tilted files went with the approximation they encoded.
 *
 * <p>The vertices are baked into a display list, keyed on the plate's state and how far it has been
 * lifted, so the common case costs one {@code glCallList} rather than a model re-walk every frame.
 * The texture is bound <b>outside</b> the list, unconditionally, immediately before the call -- see
 * "Display lists: one texture, no cached state" in {@code TRAFFIC_SIGNAL_SYSTEM.md}.
 */
public class TileEntitySignalBackplateRenderer
    extends TileEntitySpecialRenderer<TileEntitySignalBackplate> {

  /** Model units per block, matching the scale the signal head renderer works in. */
  private static final float MODEL_UNITS_PER_BLOCK = 16.0f;

  private static final CsmDisplayListCache DISPLAY_LISTS =
      new CsmDisplayListCache("signal_backplate");

  /**
   * How bright the band goes at its very best: dead ahead, on the darkest night.
   *
   * <p>Sheeting throws a lot of light back but it is not a lamp, and this is added on top of a
   * plate that is already lit. There is a ceiling somewhere above which it stops reading as paint
   * catching headlights and starts reading as a light source; this sits below it.
   */
  private static final float MAX_GLOW = 0.88f;

  /**
   * How sharply the effect falls away as you move off the plate's axis.
   *
   * <p>Retroreflective sheeting returns light along the line it arrived on, so it is bright to a
   * driver whose headlights are beside their eyes and almost invisible to anyone off to the side.
   * A fourth power is a narrow enough lobe to read as that rather than as a general sheen.
   */
  private static final double GLOW_FOCUS = 4.0;

  /** Below this the pass is skipped outright, which in daylight is always. */
  private static final float GLOW_CUTOFF = 0.012f;

  /** Releases the cached geometry for a position. Called from the tile entity's lifecycle. */
  public static void cleanupDisplayList(BlockPos pos) {
    DISPLAY_LISTS.invalidate(pos);
  }

  @Override
  public void render(TileEntitySignalBackplate te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te.getWorld() == null) {
      return;
    }
    final BlockPos pos = te.getPos();
    final IBlockState state = te.getWorld().getBlockState(pos);
    if (!(state.getBlock() instanceof AbstractBlockSignalBackplate)) {
      return;
    }
    final IBlockState actual = state.getActualState(te.getWorld(), pos);

    // How far the head next door has been lifted by a span wire, if there is one. This is the
    // whole reason a plate is drawn from here rather than baked into the chunk with everything
    // else: a plate is a block model and cannot follow a head a fraction of a block on its own.
    final BlockPos signalPos = AbstractBlockSignalBackplate.findSignalFor(te.getWorld(), pos,
        actual.getValue(AbstractBlockSignalBackplate.FACING));
    final Vec3d spanOffset = signalPos == null
        ? Vec3d.ZERO
        : AbstractBlockSignalBackplate.spanOffsetOf(te.getWorld(), signalPos);
    final float rise = (float) spanOffset.y;

    // The plate is drawn from the untilted model and turned here. The tilted variants stay in
    // getActualState -- they are how the tilt reaches this renderer -- but they no longer choose
    // geometry, so a tilt is now an exact transform rather than a model authored to approximate one.
    final EnumFacing facing = actual.getValue(AbstractBlockSignalBackplate.FACING);
    final IBlockState renderState = actual.withProperty(AbstractBlockSignalBackplate.MODEL_VARIANT,
        actual.getValue(AbstractBlockSignalBackplate.MODEL_VARIANT).untilted());

    final TrafficSignalBodyTilt tilt = signalPos == null
        ? TrafficSignalBodyTilt.NONE
        : AbstractBlockSignalBackplate.tiltOf(te.getWorld(), signalPos);

    // The light is baked into the compiled vertices, so a change in it has to compile a new list.
    // Day and night do not need this -- those move the lightmap texture under a fixed coordinate --
    // but a torch going up next door changes the coordinate itself.
    final int combinedLight = te.getWorld().getCombinedLight(pos, 0);

    // Keyed on the state actually compiled. The tilt is a matrix outside the list, so two tilts of
    // one plate share a list rather than evicting each other.
    long key = renderState.hashCode() * 31L + Math.round(rise * 64.0f);
    key = key * 31L + Math.round(spanOffset.x * 64.0);
    key = key * 31L + Math.round(spanOffset.z * 64.0);
    key = key * 31L + combinedLight;

    GlStateManager.pushMatrix();
    GlStateManager.disableLighting();
    GlStateManager.translate(x, y, z);
    // Sideways with the head. The rise goes into the compiled geometry below because it is baked
    // per position anyway; this is a plain world-axis shift and belongs in the matrix.
    if (spanOffset.x != 0.0 || spanOffset.z != 0.0) {
      GlStateManager.translate(spanOffset.x / MODEL_UNITS_PER_BLOCK, 0.0,
          spanOffset.z / MODEL_UNITS_PER_BLOCK);
    }
    applyTilt(pos, signalPos, facing, tilt);

    int displayList = DISPLAY_LISTS.get(pos, key);
    if (displayList == CsmDisplayListCache.NO_LIST) {
      displayList = DISPLAY_LISTS.allocate(pos, key);
      if (displayList != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        emit(te, pos, renderState, rise);
        GL11.glEndList();
      }
    }
    // Bound outside the list, unconditionally, immediately before the call. A bind recorded inside
    // a display list can silently record nothing at all.
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    if (displayList == CsmDisplayListCache.NO_LIST) {
      // The cache is full and would not give us a list. Draw straight rather than vanish.
      emit(te, pos, renderState, rise);
    } else {
      GL11.glCallList(displayList);
    }

    emitRetroreflection(te, pos, renderState, rise, partialTicks, facing);

    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /**
   * The band catching headlights.
   *
   * <p>Backplates carry a retroreflective border, and until now the only thing standing in for it
   * was an emissive texture that needs a shader pack to mean anything. This draws it directly: the
   * plate's own geometry again, added on top of itself, scaled by how squarely it is being looked
   * at and how dark it is.
   *
   * <p><b>Why adding the whole plate is right, and not a cheat.</b> Sheeting returns a fraction of
   * what lands on it, so how brightly a patch comes back is its own colour -- which is exactly what
   * addition does. The black body adds nothing because black is nothing, and the band brightens
   * because it is bright. Nothing has to know which quads are the band, so this works on all
   * nineteen models and every colour pairing without a list of which is which.
   *
   * <p><b>Cost.</b> Nothing at all in daylight or off-axis, which is nearly always: the factor
   * falls under the cutoff and the method returns before touching GL. When it does draw it is one
   * {@code glCallList} against a list that compiles once, because the brightness rides on
   * {@code glColor} rather than on the vertices.
   */
  private void emitRetroreflection(TileEntitySignalBackplate te, BlockPos pos,
      IBlockState renderState, float rise, float partialTicks, EnumFacing facing) {
    final float strength = glowStrength(te, pos, facing, partialTicks);
    if (strength < GLOW_CUTOFF) {
      return;
    }

    GlStateManager.enableBlend();
    // Additive: what comes back is added to what is already lit, never subtracted from it.
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    GlStateManager.depthMask(false);

    // The lightmap unit has to come off, and it is not optional. This pass draws in a format with
    // no lightmap coordinate, so the unit would sample whatever coordinate happened to be current
    // -- at night, near black -- and multiply the effect away to nothing. Switching the unit off
    // rather than forcing it to full bright leaves the shared lightmap state alone, which the
    // signal head renderer sets per vertex and relies on.
    OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
    GlStateManager.disableTexture2D();
    OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

    GlStateManager.pushMatrix();
    GlStateManager.translate(0.0, rise / MODEL_UNITS_PER_BLOCK, 0.0);
    emitGlow(Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(renderState),
        renderState, strength);
    GlStateManager.popMatrix();

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

    OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
    GlStateManager.enableTexture2D();
    OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

    GlStateManager.depthMask(true);
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.disableBlend();
  }

  /**
   * The plate again, in one buffer, with the strength written into the vertex colours.
   *
   * <p>This is vanilla's {@code renderModelBrightnessColor} with the batching it does not do.
   * That method begins and draws a buffer <em>per quad</em>, which for a plate is around fifty
   * draw calls a frame; there is no reason for that, since every quad here takes the same colour.
   * One begin, every quad, one draw.
   *
   * <p>The strength has to go into the vertex colours rather than {@code glColor}: a format with a
   * colour attribute ignores glColor, and a format without one drew nothing at all here. That is
   * also why the geometry cannot simply live in a display list -- the colour it is compiled with
   * would be frozen into it, and the whole point is that the colour changes as you move.
   */
  private void emitGlow(IBakedModel model, IBlockState renderState, float strength) {
    final Tessellator tessellator = Tessellator.getInstance();
    final BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
    for (EnumFacing side : EnumFacing.values()) {
      appendGlowQuads(buffer, model.getQuads(renderState, side, 0L), strength);
    }
    appendGlowQuads(buffer, model.getQuads(renderState, null, 0L), strength);
    tessellator.draw();
  }

  private static void appendGlowQuads(BufferBuilder buffer, List<BakedQuad> quads, float strength) {
    for (BakedQuad quad : quads) {
      buffer.addVertexData(quad.getVertexData());
      buffer.putColorRGB_F4(strength, strength, strength);
      final Vec3i normal = quad.getFace().getDirectionVec();
      buffer.putNormal(normal.getX(), normal.getY(), normal.getZ());
    }
  }

  /**
   * How hard the band is throwing light back, from zero to {@link #MAX_GLOW}.
   *
   * <p>Two terms. How squarely the plate is being looked at, because sheeting returns light along
   * the line it came in on. And how dark it is, because a retroreflector in daylight is just a
   * yellow stripe -- the effect people picture is the one they have seen at night.
   */
  private float glowStrength(TileEntitySignalBackplate te, BlockPos pos, EnumFacing facing,
      float partialTicks) {
    if (te.getWorld() == null) {
      return 0.0f;
    }
    final double toCameraX = TileEntityRendererDispatcher.staticPlayerX - (pos.getX() + 0.5);
    final double toCameraY = TileEntityRendererDispatcher.staticPlayerY - (pos.getY() + 0.5);
    final double toCameraZ = TileEntityRendererDispatcher.staticPlayerZ - (pos.getZ() + 0.5);
    final double distance =
        Math.sqrt(toCameraX * toCameraX + toCameraY * toCameraY + toCameraZ * toCameraZ);
    if (distance < 1.0e-4) {
      return 0.0f;
    }

    // The band is on the face the plate points at, so anyone behind it sees nothing come back.
    final double alignment = (toCameraX * facing.getXOffset()
        + toCameraY * facing.getYOffset()
        + toCameraZ * facing.getZOffset()) / distance;
    if (alignment <= 0.0) {
      return 0.0f;
    }

    final float darkness = 1.0f - te.getWorld().getSunBrightness(partialTicks);
    return (float) (Math.pow(alignment, GLOW_FOCUS) * darkness * MAX_GLOW);
  }

  /**
   * Turns the plate to match the head it is mounted to.
   *
   * <p>A plate bolts to the back of a head, so when the head swings the plate has to swing with it
   * -- about the <em>head's</em> centre, not its own. That is the whole bug this replaces: a
   * blockstate can only bake one rotation origin into a model, and the right origin sits a block
   * away in the facing direction, differently for every facing. The pre-tilted models were authored
   * for whichever facing looked best and the rest drifted, which is the gap between a tilted signal
   * and its plate.
   *
   * <p>Two things have to match the head exactly, and both are read from its renderer rather than
   * copied, so they cannot drift apart later:
   *
   * <ul>
   *   <li>the angle -- as a <em>delta</em> from the facing, because unlike the head's model the
   *       plate's already has its facing baked in by the blockstate;</li>
   *   <li>the sideways nudge a tilted head makes to stay visually centred. The head applies it in
   *       its own turned frame, so it is rotated into world axes here and applied outside the
   *       rotation, which comes to the same place.</li>
   * </ul>
   *
   * <p>Only horizontal facings tilt. A plate lying flat has no left or right for a tilt to mean,
   * and the head's own facing-angle table reads UP and DOWN as zero, so it is left alone.
   */
  private void applyTilt(BlockPos pos, BlockPos signalPos, EnumFacing facing,
      TrafficSignalBodyTilt tilt) {
    if (signalPos == null || tilt == TrafficSignalBodyTilt.NONE
        || facing.getAxis() == EnumFacing.Axis.Y) {
      return;
    }

    final float absoluteAngle =
        AbstractBlockControllableSignalHead.getTiltedFacing(tilt, facing).getRotation();
    final float delta =
        absoluteAngle - TileEntityTrafficSignalHeadRenderer.getBaseFacingAngle(facing);
    if (delta == 0.0f) {
      return;
    }

    // The head's centre, in blocks relative to this plate's own corner. Only X and Z matter to a
    // rotation about Y.
    final double pivotX = (signalPos.getX() - pos.getX()) + 0.5;
    final double pivotZ = (signalPos.getZ() - pos.getZ()) + 0.5;

    // The head's lateral nudge, turned from its frame into world axes. glRotate about +Y sends
    // (t, 0, 0) to (t*cos, 0, -t*sin).
    final double nudge =
        TileEntityTrafficSignalHeadRenderer.getLateralTiltOffset(tilt) / MODEL_UNITS_PER_BLOCK;
    final double radians = Math.toRadians(absoluteAngle);

    GlStateManager.translate(nudge * Math.cos(radians), 0.0, -nudge * Math.sin(radians));
    GlStateManager.translate(pivotX, 0.0, pivotZ);
    GlStateManager.rotate(delta, 0.0f, 1.0f, 0.0f);
    GlStateManager.translate(-pivotX, 0.0, -pivotZ);
  }

  /**
   * Builds the plate's geometry, lifted by the head's rise.
   *
   * <p>Emitted relative to the block's own corner, so the list stays valid wherever the camera is
   * and only the translate above has to change per frame.
   */
  private void emit(TileEntitySignalBackplate te, BlockPos pos, IBlockState actual, float rise) {
    final BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
    final IBakedModel model = dispatcher.getModelForState(actual);

    final Tessellator tessellator = Tessellator.getInstance();
    final BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    // renderModel works in world coordinates, so shift its output back to the block's own origin
    // and add the lift. Without the first part every plate would be drawn at its absolute world
    // position on top of the translate already applied, and end up a very long way away.
    buffer.setTranslation(-pos.getX(), -pos.getY() + rise / MODEL_UNITS_PER_BLOCK, -pos.getZ());
    // Flat, not the default smooth path, and this is the fix for the blotches on a plate's back.
    //
    // Smooth lighting computes ambient occlusion per vertex from the blocks around the face -- the
    // *block's* face. A plate's geometry is nowhere near its block: the model runs from -2 to 18
    // across and -16 to 28 up, so most of it hangs a block and a half outside the position whose
    // neighbours are being sampled. The head and the pole next door therefore cast occlusion onto
    // parts of the plate that are not next to them at all. On a plate mounted in the usual way, in
    // full daylight, that put patches of pure black and near-black across a face whose lit value is
    // 23 -- measured along one scanline as 0, 7, 10, 19 and 23 side by side.
    //
    // The flat path takes one brightness per quad instead, and since none of these quads lie on a
    // block boundary they all resolve to this block's own light -- a single uniform value across
    // the whole plate, which is what every other renderer in CSM already does by hand. The same
    // scanline now reads 23 with 25 on the side rails. Face shading still comes through: the
    // per-direction multiplier is baked into the quad colours at model bake time, so the plate
    // keeps its depth. (An isolated plate with no neighbours looked fine either way -- which is why
    // it is a useless control, and why this was checked on a mounted one.)
    dispatcher.getBlockModelRenderer()
        .renderModelFlat(te.getWorld(), model, actual, pos, buffer, false,
            MathHelper.getPositionRandom(pos));
    buffer.setTranslation(0.0, 0.0, 0.0);
    tessellator.draw();
  }
}
