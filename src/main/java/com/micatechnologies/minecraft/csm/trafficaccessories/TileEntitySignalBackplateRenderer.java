package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
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
 * seventy-odd backplate models across shapes, section counts and tilt variants, with ninety-odd
 * blocks pointing at them and their own textures; reproducing that here would put all of it at risk
 * in one step. The geometry and every texture stay exactly as authored, and only the transform is
 * new.
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
    final float rise = signalPos == null
        ? 0.0f
        : AbstractBlockSignalBackplate.spanRiseOf(te.getWorld(), signalPos);

    final long key = actual.hashCode() * 31L + Math.round(rise * 64.0f);

    GlStateManager.pushMatrix();
    GlStateManager.disableLighting();
    GlStateManager.translate(x, y, z);

    int displayList = DISPLAY_LISTS.get(pos, key);
    if (displayList == CsmDisplayListCache.NO_LIST) {
      displayList = DISPLAY_LISTS.allocate(pos, key);
      if (displayList != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        emit(te, pos, actual, rise);
        GL11.glEndList();
      }
    }
    // Bound outside the list, unconditionally, immediately before the call. A bind recorded inside
    // a display list can silently record nothing at all.
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    if (displayList == CsmDisplayListCache.NO_LIST) {
      // The cache is full and would not give us a list. Draw straight rather than vanish.
      emit(te, pos, actual, rise);
    } else {
      GL11.glCallList(displayList);
    }

    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
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
    dispatcher.getBlockModelRenderer()
        .renderModel(te.getWorld(), model, actual, pos, buffer, false);
    buffer.setTranslation(0.0, 0.0, 0.0);
    tessellator.draw();
  }
}
