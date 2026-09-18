package com.micatechnologies.minecraft.csm.constructionsite;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a tower crane head's slewing unit (see {@link CraneGeometry}).
 *
 * <p>A crane is thousands of quads and changes almost never, so each head's geometry is compiled
 * once into a display list and replayed every frame, rebuilt only when its configuration or the
 * light at the head changes. The slew is a rotation applied outside the list, so turning the crane
 * never rebuilds it. The rules this follows are the ones in "Display lists: one texture, no cached
 * state" in {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}: the whole crane is drawn from the block
 * atlas, which is bound outside the list every frame; nothing inside the list touches cached GL
 * state; and the light is part of the cache key, since it is baked into the vertices.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityCraneHeadRenderer extends TileEntitySpecialRenderer<TileEntityCraneHead> {

  /** One compiled list per head, and the configuration it was compiled from. */
  private static final Map<BlockPos, int[]> LISTS = new HashMap<>();

  @Override
  public void render(TileEntityCraneHead te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    int light = te.getWorld().getCombinedLight(te.getPos(), 0);
    int key = te.renderKey() * 31 + light;
    BlockPos pos = te.getPos();
    int[] entry = LISTS.get(pos);
    if (entry == null) {
      entry = new int[]{GLAllocation.generateDisplayLists(1), key + 1};
      LISTS.put(pos.toImmutable(), entry);
    }
    if (entry[1] != key) {
      GlStateManager.glNewList(entry[0], GL11.GL_COMPILE);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buf = tessellator.getBuffer();
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      CraneGeometry.build(buf, te, light);
      tessellator.draw();
      GlStateManager.glEndList();
      entry[1] = key;
    }

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.pushMatrix();
    GlStateManager.disableLighting();
    GlStateManager.translate(x + 0.5 + te.getCentreX(), y, z + 0.5 + te.getCentreZ());
    GlStateManager.rotate(-te.getSlew(), 0F, 1F, 0F);
    GlStateManager.callList(entry[0]);
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /**
   * Frees a head's display list, when the head is broken or its chunk unloads.
   *
   * @param pos the head's position
   *
   * @since 1.0
   */
  public static void release(BlockPos pos) {
    int[] entry = LISTS.remove(pos);
    if (entry != null) {
      GLAllocation.deleteDisplayLists(entry[0]);
    }
  }

  @Override
  public boolean isGlobalRenderer(TileEntityCraneHead te) {
    return true;
  }
}
