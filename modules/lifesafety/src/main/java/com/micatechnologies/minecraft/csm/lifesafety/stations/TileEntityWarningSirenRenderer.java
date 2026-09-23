package com.micatechnologies.minecraft.csm.lifesafety.stations;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
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
 * Turns a rotating warning siren's horn while it sounds. Draws nothing otherwise: at rest the horn
 * is part of the siren's baked model, and while it sounds the baked model leaves it out
 * ({@link BlockWarningSiren}), so the horn drawn here is the very model it rests in, turned about
 * the siren's axis by the world clock ({@link TileEntityWarningSiren#hornAngle}).
 *
 * <p>The horn's quads are taken from the block's {@code head=true} state once per model and kept;
 * a siren is a few dozen quads, drawn only while it sounds.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityWarningSirenRenderer extends TileEntitySpecialRenderer<TileEntityWarningSiren> {

  private final Map<IBlockState, List<BakedQuad>> quadsByState =
      new IdentityHashMap<>();
  private final Map<IBlockState, IBakedModel> modelByState =
      new IdentityHashMap<>();

  @Override
  public void render(TileEntityWarningSiren te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    if (te.getSignal() == SirenSignal.NONE || !te.rotates()) {
      return;
    }
    IBlockState state = te.getWorld().getBlockState(te.getPos());
    if (!(state.getBlock() instanceof BlockWarningSiren)) {
      return;
    }
    IBlockState head = state.getBlock().getDefaultState()
        .withProperty(BlockWarningSiren.FACING, EnumFacing.NORTH)
        .withProperty(BlockWarningSiren.HEAD, true);
    // Keyed on the model as well as the state: a resource reload bakes new models, and the old
    // quads would point at the old texture atlas.
    IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher()
        .getModelForState(head);
    if (modelByState.get(head) != model) {
      modelByState.put(head, model);
      quadsByState.put(head, quads(model, head));
    }
    List<BakedQuad> cachedQuads = quadsByState.get(head);
    int light = te.getWorld().getCombinedLight(te.getPos(), 0);

    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.pushMatrix();
    RenderHelper.disableStandardItemLighting();
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >> 16);
    GlStateManager.translate(x + 0.5, y, z + 0.5);
    GlStateManager.rotate(te.hornAngle(partialTicks), 0F, 1F, 0F);
    GlStateManager.translate(-0.5, 0, -0.5);
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
    for (BakedQuad quad : cachedQuads) {
      LightUtil.renderQuadColor(buffer, quad, 0xFFFFFFFF);
      float s = quad.getFace() == EnumFacing.UP ? 1.0F
          : quad.getFace() == EnumFacing.DOWN ? 0.5F : 0.75F;
      buffer.putColorRGB_F4(s, s, s);
    }
    tessellator.draw();
    RenderHelper.enableStandardItemLighting();
    GlStateManager.popMatrix();
  }

  private static List<BakedQuad> quads(IBakedModel model, IBlockState state) {
    List<BakedQuad> out = new ArrayList<>(model.getQuads(state, null, 0L));
    for (EnumFacing side : EnumFacing.values()) {
      out.addAll(model.getQuads(state, side, 0L));
    }
    return out;
  }

  @Override
  public boolean isGlobalRenderer(TileEntityWarningSiren te) {
    return false;
  }
}
