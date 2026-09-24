package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Turns a ceiling fan's blades while it runs. At rest the blades are part of the fan's baked
 * model; while it runs the blockstate leaves them out ({@link BlockCeilingFan#FAN}), and this
 * draws the very same quads turning about the downrod. They are found as the quads the resting
 * fan has and the running one does not (the multipart shares its parts' quads between states),
 * once per model, and kept; a fan is a few dozen quads, drawn only while it runs. The block's
 * light is already set by the dispatcher.
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityCeilingFanRenderer extends TileEntitySpecialRenderer<TileEntityCeilingFan> {

  /** Degrees a millisecond: a little over one turn a second, slow enough not to strobe. */
  private static final double DEGREES_PER_MS = 0.42;

  private final Map<Block, IBakedModel> modelByBlock = new IdentityHashMap<>();
  private final Map<Block, List<BakedQuad>> bladesByBlock = new IdentityHashMap<>();

  @Override
  public void render(TileEntityCeilingFan te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if ((te.getBlockMetadata() & 8) == 0) {
      return;
    }
    Block block = te.getBlockType();
    if (!(block instanceof BlockCeilingFan)) {
      return;
    }
    IBlockState still = block.getDefaultState()
        .withProperty(BlockCeilingFan.FACING, EnumFacing.NORTH)
        .withProperty(BlockCeilingFan.LIGHT, false).withProperty(BlockCeilingFan.FAN, false);
    // Keyed on the model as well: a resource reload bakes new models, whose quads point at the
    // new texture atlas.
    IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
        .getModelForState(still);
    if (modelByBlock.get(block) != model) {
      modelByBlock.put(block, model);
      bladesByBlock.put(block, blades(model, still, still.withProperty(BlockCeilingFan.FAN, true)));
    }
    List<BakedQuad> blades = bladesByBlock.get(block);
    if (blades.isEmpty()) {
      return;
    }
    long phase = (te.getPos().hashCode() * 2654435761L) & 0xFFFF;
    double angle = ((CsmRenderUtils.gameMillis(te.getWorld(), partialTicks) + phase)
        * DEGREES_PER_MS) % 360.0;

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.pushMatrix();
    RenderHelper.disableStandardItemLighting();
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate((float) -angle, 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
    for (BakedQuad quad : blades) {
      LightUtil.renderQuadColor(buffer, quad, 0xFFFFFFFF);
      float s = quad.getFace() == EnumFacing.UP ? 1.0F
          : quad.getFace() == EnumFacing.DOWN ? 0.5F : 0.75F;
      buffer.putColorRGB_F4(s, s, s);
    }
    tessellator.draw();
    RenderHelper.enableStandardItemLighting();
    GlStateManager.popMatrix();
  }

  /** The quads {@code still} has and {@code running} does not: the blades. */
  private static List<BakedQuad> blades(IBakedModel model, IBlockState still,
      IBlockState running) {
    IBakedModel runningModel = Minecraft.getMinecraft().getBlockRendererDispatcher()
        .getModelForState(running);
    Set<BakedQuad> keep = Collections.newSetFromMap(new IdentityHashMap<>());
    keep.addAll(quads(runningModel, running));
    List<BakedQuad> out = new ArrayList<>();
    for (BakedQuad q : quads(model, still)) {
      if (!keep.contains(q)) {
        out.add(q);
      }
    }
    return out;
  }

  private static List<BakedQuad> quads(IBakedModel model, IBlockState state) {
    List<BakedQuad> out = new ArrayList<>(model.getQuads(state, null, 0L));
    for (EnumFacing side : EnumFacing.values()) {
      out.addAll(model.getQuads(state, side, 0L));
    }
    return out;
  }

  @Override
  public boolean isGlobalRenderer(TileEntityCeilingFan te) {
    return false;
  }
}
